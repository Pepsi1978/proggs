package de.frank.entropyreducer.domain.tts

import android.util.Base64
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.remote.tts.GoogleTtsVoices
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** HTTP-Fehler von Gemini- oder Azure-Stimme (Netzfehler laufen als IOException durch). */
class CloudVoiceException(message: String) : Exception(message)

/** Einstellungen der Gemini-Stimme, gelesen vom [TtsPlayer]. */
data class GeminiTtsConfig(
    val freeKey: String,
    val paidKey: String,
    /** frei_dann_bezahlt, nur_frei oder nur_bezahlt. */
    val keyChoice: String,
    val chirpFallback: Boolean,
)

/**
 * Gemini 3.8 Flash / Flash-Lite TTS (liefert WAV) und Azure Neural (liefert MP3).
 * Beides spielt der MediaPlayer des [TtsPlayer] ab; er erkennt das Format am Inhalt.
 */
class CloudVoiceSynth {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Reihenfolge laut [GeminiTtsConfig.keyChoice]: erst der kostenlose, dann der bezahlte Schlüssel. Ist
     * [GeminiTtsConfig.chirpFallback] an, geht es nach einem Fehler des kostenlosen Schlüssels nicht zum bezahlten,
     * sondern mit derselben Stimme über [chirp] zu Chirp 3 HD. Netzfehler laufen unverändert durch.
     */
    suspend fun gemini(
        text: String,
        voiceId: String,
        config: GeminiTtsConfig,
        chirp: suspend (chirpVoice: String) -> ByteArray,
    ): ByteArray = withContext(Dispatchers.IO) {
        val model = if (voiceId.startsWith(GoogleTtsVoices.GEMINI_LITE_PREFIX)) LITE_MODEL else FLASH_MODEL
        val free = config.freeKey.filterNot(Char::isWhitespace)
        val paid = config.paidKey.filterNot(Char::isWhitespace)
        var freeError: CloudVoiceException? = null
        if (config.keyChoice != NUR_BEZAHLT) {
            val blocked = System.currentTimeMillis() < freeBlockedUntil
            if (free.isNotBlank() && !blocked) {
                // Mit Ausweichweg nicht erst bei 429 warten — der nächste Weg ist schneller als jede Wartezeit.
                val escape = config.chirpFallback || (config.keyChoice == FREI_DANN_BEZAHLT && paid.isNotBlank())
                try {
                    return@withContext geminiRequest(text, model, voiceId, free, waitOn429 = !escape)
                } catch (e: CloudVoiceException) {
                    freeError = e
                    if (escape) freeBlockedUntil = System.currentTimeMillis() + FREE_PAUSE_MS
                    Diag.e(DiagnosticArea.GOOGLE_TTS, TAG, "Gemini-Stimme: kostenloser Schlüssel gescheitert (model=$model): ${e.message?.take(120)}", e)
                }
            }
            if (config.chirpFallback) {
                Diag.d(DiagnosticArea.GOOGLE_TTS, TAG, "Gemini-Stimme: Rückfall auf Chirp 3 HD (gesperrt=$blocked)")
                return@withContext chirp(GoogleTtsVoices.chirpFor(voiceId))
            }
            if (config.keyChoice == NUR_FREI) {
                throw freeError ?: CloudVoiceException("Es ist kein kostenloser Gemini-Schlüssel für die Stimme hinterlegt. Trag ihn in den Einstellungen ein.")
            }
        }
        if (paid.isBlank()) {
            throw freeError ?: CloudVoiceException("Es ist kein Gemini-Schlüssel für die Stimme hinterlegt. Trag ihn in den Einstellungen ein.")
        }
        geminiRequest(text, model, voiceId, paid, waitOn429 = true)
    }

    private suspend fun geminiRequest(text: String, model: String, voiceId: String, key: String, waitOn429: Boolean): ByteArray {
        // Gemini liest den Text wörtlich vor; eine Anweisung im Text würde mitgesprochen.
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", text)))))
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseModalities", JSONArray().put("AUDIO"))
                    .put(
                        "speechConfig",
                        JSONObject().put(
                            "voiceConfig",
                            JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", GoogleTtsVoices.geminiName(voiceId))),
                        ),
                    ),
            )
        val request = Request.Builder()
            .url("$GEMINI_URL$model:generateContent")
            .header("x-goog-api-key", key)
            .post(body.toString().toRequestBody(JSON_TYPE))
            .build()
        val payload = post(request, waitOn429)
        val candidate = runCatching { JSONObject(payload).optJSONArray("candidates")?.optJSONObject(0) }.getOrNull()
        val finish = candidate?.optString("finishReason").orEmpty()
        val part = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optJSONObject("inlineData")
        val data = part?.optString("data").orEmpty()
        if (data.isBlank()) throw CloudVoiceException("Gemini hat kein Audio geliefert ($finish).")
        if (finish.isNotBlank() && finish != "STOP") throw CloudVoiceException("Gemini hat die Stimme abgebrochen ($finish).")
        val raw = Base64.decode(data, Base64.DEFAULT)
        val rate = Regex("rate=(\\d+)").find(part?.optString("mimeType").orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 24_000
        val wav = if (raw.size > 44 && String(raw, 0, 4, Charsets.US_ASCII) == "RIFF") raw else withWavHeader(raw, rate)
        // Abgeschnittenes Audio meldet Gemini teils trotzdem als fertig: Unter 25 Zeichen je Sekunde spricht niemand.
        val seconds = (wav.size - 44) / (wavRate(wav) * 2.0)
        if (text.length >= 60 && seconds < text.length / 25.0) {
            throw CloudVoiceException("Gemini-Audio zu kurz (${"%.1f".format(seconds)} s für ${text.length} Zeichen).")
        }
        return wav
    }

    private suspend fun post(request: Request, waitOn429: Boolean): String {
        var attempt = 0
        while (true) {
            val (payload, code) = client.newCall(request).execute().use { it.body?.string().orEmpty() to it.code }
            when {
                code in 200..299 -> return payload
                code == 429 && waitOn429 && attempt < WAIT_MS.size -> { delay(WAIT_MS[attempt]); attempt++ }
                code == 401 || code == 403 -> throw CloudVoiceException("Gemini hat den Schlüssel abgelehnt (Fehler $code). Prüf ihn in den Einstellungen.")
                code == 429 -> throw CloudVoiceException("Das Kontingent der Gemini-Stimme ist erschöpft (Fehler 429).")
                else -> throw CloudVoiceException("Gemini antwortete mit Fehler $code: ${payload.take(200)}")
            }
        }
    }

    /** Rohes PCM (16 Bit, mono) bekommt einen WAV-Kopf, damit der MediaPlayer es abspielt. */
    private fun withWavHeader(pcm: ByteArray, rate: Int): ByteArray {
        val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + pcm.size).put("WAVE".toByteArray())
            .put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(pcm.size)
        return header.array() + pcm
    }

    private fun wavRate(wav: ByteArray): Int =
        java.nio.ByteBuffer.wrap(wav, 24, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int.takeIf { it > 0 } ?: 24_000

    /**
     * Azure AI Speech über REST: Schlüssel und Region der eigenen Speech-Ressource (F0 kostenlos oder S0).
     * HD-Stimmen (DragonHD) bekommen keinen <lang>-Rahmen: Sie bestimmen Betonung und Tempo selbst.
     */
    suspend fun azure(text: String, voiceId: String, key: String, regionInput: String): ByteArray = withContext(Dispatchers.IO) {
        val apiKey = key.filterNot(Char::isWhitespace)
        if (apiKey.isBlank()) throw CloudVoiceException("Es ist kein Azure-Schlüssel hinterlegt. Trag ihn in den Einstellungen ein.")
        val region = azureRegion(regionInput)
            ?: throw CloudVoiceException("Die Azure-Region „${regionInput.take(40)}“ verstehe ich nicht. Trag den Regionsnamen ein, etwa westeurope.")
        val voice = GoogleTtsVoices.azureName(voiceId)
        val hd = voiceId.contains("DragonHD")
        // Steuerzeichen sind in XML verboten; Azure lehnte das ganze SSML sonst mit 400 ab.
        val escaped = text.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), " ")
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        // <lang> kennt Azure nur bei den mehrsprachigen Stimmen; die übrigen sprechen ohnehin nur Deutsch.
        val content = if (!hd && voice.contains("Multilingual")) "<lang xml:lang='de-DE'>$escaped</lang>" else escaped
        val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
            "<voice name='$voice'>$content</voice></speak>"
        val request = Request.Builder()
            .url("https://$region.tts.speech.microsoft.com/cognitiveservices/v1")
            .header("Ocp-Apim-Subscription-Key", apiKey)
            .header("X-Microsoft-OutputFormat", "audio-24khz-96kbitrate-mono-mp3")
            .header("User-Agent", "EntropieReductor")
            .post(ssml.toRequestBody(SSML_TYPE))
            .build()
        var attempt = 0
        while (true) {
            val response = client.newCall(request).execute()
            val retryAfter = response.header("Retry-After")?.trim()?.toLongOrNull()
            val (bytes, code) = response.use {
                (if (it.isSuccessful) it.body?.bytes() else it.body?.string()?.toByteArray()) to it.code
            }
            when {
                code in 200..299 -> {
                    if (bytes == null || bytes.isEmpty()) throw CloudVoiceException("Azure hat kein Audio geliefert.")
                    return@withContext bytes
                }
                // F0 erlaubt nur rund 20 Anfragen je Minute: ein 429 ist meist dieses Ratenlimit, nicht das Monatskontingent.
                code == 429 && attempt < AZURE_WAIT_MS.size -> {
                    delay(retryAfter?.let { (it * 1000).coerceIn(1_000L, 60_000L) } ?: AZURE_WAIT_MS[attempt])
                    attempt++
                }
                code == 401 || code == 403 -> throw CloudVoiceException("Azure hat Schlüssel oder Region abgelehnt (Fehler $code). Prüf beides in den Einstellungen.")
                code == 429 -> throw CloudVoiceException("Azure lässt gerade keine Anfragen zu (Fehler 429): Ratenlimit oder Monatskontingent erschöpft.")
                code == 400 && hd -> throw CloudVoiceException("Azure lehnt die HD-Stimme ab (Fehler 400). HD-Stimmen laufen nur mit der Stufe S0 und nicht in jeder Region.")
                else -> throw CloudVoiceException("Azure antwortete mit Fehler $code: ${bytes?.toString(Charsets.UTF_8)?.take(200).orEmpty()}")
            }
        }
        @Suppress("UNREACHABLE_CODE")
        ByteArray(0)
    }

    /**
     * Der Regionsname für die Adresse („westeurope“). Nimmt auch den eingefügten Endpunkt aus dem Portal
     * („https://westeurope.api.cognitive.microsoft.com/“); null, wenn nichts Brauchbares darin steht.
     */
    private fun azureRegion(input: String): String? {
        val raw = input.trim().lowercase().ifBlank { "westeurope" }
        val fromEndpoint = Regex("^(?:https?://)?([a-z0-9]+)\\.(?:api\\.cognitive|tts\\.speech)\\.").find(raw)?.groupValues?.get(1)
        return (fromEndpoint ?: raw).takeIf { Regex("^[a-z0-9]{2,40}$").matches(it) }
    }

    companion object {
        private const val TAG = "CloudVoiceSynth"
        const val FLASH_MODEL = "gemini-3.8-flash-tts"
        const val LITE_MODEL = "gemini-3.8-flash-lite-tts"
        const val FREI_DANN_BEZAHLT = "frei_dann_bezahlt"
        const val NUR_FREI = "nur_frei"
        const val NUR_BEZAHLT = "nur_bezahlt"
        val KEY_CHOICES = listOf(
            FREI_DANN_BEZAHLT to "Erst kostenlos, dann bezahlt",
            NUR_FREI to "Nur kostenlos",
            NUR_BEZAHLT to "Nur bezahlt",
        )
        private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

        /** So lange bleibt der kostenlose Gemini-Schlüssel nach einem Fehler übersprungen, wenn es einen Ausweichweg gibt. */
        private const val FREE_PAUSE_MS = 10 * 60_000L

        /** Gilt für alle Instanzen: sonst probierte jede neue Vorlese-Sequenz den gescheiterten Schlüssel erneut. */
        @Volatile private var freeBlockedUntil = 0L
        private val WAIT_MS = longArrayOf(1_000L, 3_000L, 8_000L)

        /** Reicht zusammen über das Minutenfenster von F0. */
        private val AZURE_WAIT_MS = longArrayOf(3_000L, 10_000L, 20_000L, 30_000L)
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
        private val SSML_TYPE = "application/ssml+xml".toMediaType()
    }
}

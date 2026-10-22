package de.frank.cortex.tts

import de.frank.cortex.data.SettingsStore
import de.frank.cortex.network.ApiClient
import de.frank.cortex.observability.CortexLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fehler von Gemini- oder Azure-Stimme. [fatal] = gilt fuer die ganze Vorlese-Session (Schluessel fehlt,
 * abgelehnt, Kontingent erschoepft); sonst betrifft er nur diesen Absatz (zu kurzes Audio, Abbruch).
 */
class CloudVoiceException(message: String, val fatal: Boolean = true) : Exception(message)

/** Die festen Stimmlisten von Azure Neural (inkl. HD/DragonHD). */
object AzureVoices {
    data class Entry(val id: String, val short: String, val female: Boolean, val description: String)

    val all = listOf(
        Entry("de-DE-Seraphina:DragonHDLatestNeural", "Seraphina HD", true, "weiblich · HD (nur S0)"),
        Entry("de-DE-Florian:DragonHDLatestNeural", "Florian HD", false, "männlich · HD (nur S0)"),
        Entry("de-DE-SeraphinaMultilingualNeural", "Seraphina", true, "weiblich · Standard"),
        Entry("de-DE-KatjaNeural", "Katja", true, "weiblich"),
        Entry("de-DE-AmalaNeural", "Amala", true, "weiblich"),
        Entry("de-DE-ElkeNeural", "Elke", true, "weiblich"),
        Entry("de-DE-GiselaNeural", "Gisela (Kind)", true, "weiblich · Kind"),
        Entry("de-DE-KlarissaNeural", "Klarissa", true, "weiblich"),
        Entry("de-DE-LouisaNeural", "Louisa", true, "weiblich"),
        Entry("de-DE-MajaNeural", "Maja", true, "weiblich"),
        Entry("de-DE-TanjaNeural", "Tanja", true, "weiblich"),
        Entry("de-DE-FlorianMultilingualNeural", "Florian", false, "männlich"),
        Entry("de-DE-ConradNeural", "Conrad", false, "männlich"),
        Entry("de-DE-KillianNeural", "Killian", false, "männlich"),
        Entry("de-DE-BerndNeural", "Bernd", false, "männlich"),
        Entry("de-DE-ChristophNeural", "Christoph", false, "männlich"),
        Entry("de-DE-KasperNeural", "Kasper", false, "männlich"),
        Entry("de-DE-KlausNeural", "Klaus", false, "männlich"),
        Entry("de-DE-RalfNeural", "Ralf", false, "männlich"),
    )

    fun isHd(id: String): Boolean = id.contains("DragonHD")

    fun labelFor(id: String): String = all.firstOrNull { it.id == id }?.short ?: id

    /**
     * Der Regionsname fuer die Adresse ("westeurope"). Nimmt auch den eingefuegten Endpunkt aus dem Portal
     * ("https://westeurope.api.cognitive.microsoft.com/"); null, wenn nichts Brauchbares darin steht.
     */
    fun region(input: String): String? {
        val raw = input.trim().lowercase().ifBlank { SettingsStore.DEFAULT_AZURE_REGION }
        val fromEndpoint = Regex("^(?:https?://)?([a-z0-9]+)\\.(?:api\\.cognitive|tts\\.speech)\\.").find(raw)?.groupValues?.get(1)
        return (fromEndpoint ?: raw).takeIf { Regex("^[a-z0-9]{2,40}$").matches(it) }
    }
}

/**
 * Gemini 3.8 Flash / Flash-Lite TTS (liefert rohes PCM 16 bit mono 24 kHz wie Chirp, laeuft also ueber den
 * PcmPlayer) und Azure Neural (liefert MP3, laeuft ueber den Mp3Player). Das Tempo setzt jeweils der Player.
 */
class CloudVoiceSynthesizer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Reihenfolge laut [SettingsStore.geminiTtsSchluesselWahl]: erst der kostenlose, dann der bezahlte Schluessel.
     * Ist [SettingsStore.geminiTtsChirpRueckfall] an, geht es nach einem Fehler des kostenlosen Schluessels nicht
     * zum bezahlten, sondern mit derselben Stimme zu Chirp 3 HD. Netzfehler laufen unveraendert durch.
     * Gibt PCM zurueck.
     */
    suspend fun gemini(text: String, model: String, voiceName: String): ByteArray = withContext(Dispatchers.IO) {
        val choice = SettingsStore.geminiTtsSchluesselWahl
        val free = SettingsStore.geminiTtsKeyFrei.filterNot(Char::isWhitespace)
        val paid = SettingsStore.geminiTtsKeyBezahlt.filterNot(Char::isWhitespace)
        val chirp = SettingsStore.geminiTtsChirpRueckfall
        var freeError: CloudVoiceException? = null
        if (choice != SettingsStore.GEMINI_TTS_NUR_BEZAHLT) {
            val blocked = System.currentTimeMillis() < freeBlockedUntil
            if (free.isNotBlank() && !blocked) {
                // Mit Ausweichweg nicht erst bei 429 warten — der naechste Weg ist schneller als jede Wartezeit.
                val escape = chirp || (choice == SettingsStore.GEMINI_TTS_FREI_DANN_BEZAHLT && paid.isNotBlank())
                try {
                    return@withContext geminiRequest(text, model, voiceName, free, "kostenlos", waitOn429 = !escape)
                } catch (e: CloudVoiceException) {
                    freeError = e
                    if (escape) freeBlockedUntil = System.currentTimeMillis() + FREE_PAUSE_MS
                    CortexLog.warn("GeminiTts", "gemini", "Kostenloser Schlüssel gescheitert: ${e.message?.take(120)}",
                        mapOf("model" to model))
                }
            }
            if (chirp) {
                CortexLog.info("GeminiTts", "gemini", "Rückfall auf Chirp 3 HD", mapOf("voice" to voiceName, "blocked" to blocked))
                return@withContext ApiClient.geminiTts(text, "de-DE-Chirp3-HD-$voiceName")
            }
            if (choice == SettingsStore.GEMINI_TTS_NUR_FREI) {
                throw freeError ?: CloudVoiceException("Es ist kein kostenloser Gemini-Schlüssel für die Stimme hinterlegt. Trag ihn in den Einstellungen ein.")
            }
        }
        if (paid.isBlank()) {
            throw freeError ?: CloudVoiceException("Es ist kein Gemini-Schlüssel für die Stimme hinterlegt. Trag ihn in den Einstellungen ein.")
        }
        geminiRequest(text, model, voiceName, paid, "bezahlt", waitOn429 = true)
    }

    private suspend fun geminiRequest(text: String, model: String, voiceName: String, key: String, kind: String, waitOn429: Boolean): ByteArray {
        val startedAt = System.currentTimeMillis()
        // Gemini liest den Text woertlich vor; eine Anweisung im Text wuerde mitgesprochen.
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
                            JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", voiceName)),
                        ),
                    ),
            )
        val request = Request.Builder()
            .url("$GEMINI_URL$model:generateContent")
            .header("x-goog-api-key", key)
            .post(body.toString().toRequestBody(JSON_TYPE))
            .build()
        var attempt = 0
        var payload = ""
        while (true) {
            val (text0, code) = client.newCall(request).execute().use { it.body?.string().orEmpty() to it.code }
            payload = text0
            if (code in 200..299) break
            if (code == 429 && waitOn429 && attempt < WAIT_MS.size) {
                delay(WAIT_MS[attempt]); attempt++; continue
            }
            throw when (code) {
                401, 403 -> CloudVoiceException("Gemini hat den Schlüssel abgelehnt (Fehler $code). Prüf ihn in den Einstellungen.")
                429 -> CloudVoiceException("Das Kontingent der Gemini-Stimme ist erschöpft (Fehler 429).")
                else -> CloudVoiceException("Gemini antwortete mit Fehler $code: ${payload.take(200)}", fatal = false)
            }
        }
        val candidate = runCatching { JSONObject(payload).optJSONArray("candidates")?.optJSONObject(0) }.getOrNull()
        val finish = candidate?.optString("finishReason").orEmpty()
        val part = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optJSONObject("inlineData")
        val data = part?.optString("data").orEmpty()
        if (data.isBlank()) throw CloudVoiceException("Gemini hat kein Audio geliefert ($finish).", fatal = false)
        if (finish.isNotBlank() && finish != "STOP") throw CloudVoiceException("Gemini hat die Stimme abgebrochen ($finish).", fatal = false)
        val raw = android.util.Base64.decode(data, android.util.Base64.DEFAULT)
        val pcm = if (raw.size > 44 && String(raw, 0, 4, Charsets.US_ASCII) == "RIFF") raw.copyOfRange(44, raw.size) else raw
        val rate = Regex("rate=(\\d+)").find(part?.optString("mimeType").orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 24_000
        // Abgeschnittenes Audio meldet Gemini teils trotzdem als fertig: Unter 25 Zeichen je Sekunde spricht niemand.
        val seconds = pcm.size / (rate * 2.0)
        if (text.length >= 60 && seconds < text.length / 25.0) {
            throw CloudVoiceException("Gemini-Audio zu kurz (${"%.1f".format(seconds)} s für ${text.length} Zeichen).", fatal = false)
        }
        CortexLog.info("GeminiTts", "gemini", "Gemini-Absatz erzeugt", mapOf(
            "model" to model, "key" to kind, "text_len" to text.length, "rate" to rate,
            "elapsed_ms" to (System.currentTimeMillis() - startedAt)))
        return pcm
    }

    /**
     * Azure AI Speech ueber REST: Schluessel und Region der eigenen Speech-Ressource (F0 kostenlos oder S0).
     * Gibt MP3 zurueck. Kein prosody-Tempo — das setzt der Mp3Player beim Abspielen.
     */
    suspend fun azure(text: String, voice: String): ByteArray = withContext(Dispatchers.IO) {
        val key = SettingsStore.azureTtsKey.filterNot(Char::isWhitespace)
        if (key.isBlank()) throw CloudVoiceException("Es ist kein Azure-Schlüssel hinterlegt. Trag ihn in den Einstellungen ein.")
        val regionInput = SettingsStore.azureTtsRegion
        val region = AzureVoices.region(regionInput)
            ?: throw CloudVoiceException("Die Azure-Region „${regionInput.take(40)}“ verstehe ich nicht. Trag den Regionsnamen ein, etwa westeurope.")
        val hd = AzureVoices.isHd(voice)
        // Steuerzeichen sind in XML verboten; Azure lehnte das ganze SSML sonst mit 400 ab.
        val escaped = text.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), " ")
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;")
        // <lang> kennt Azure nur bei den mehrsprachigen Stimmen; die uebrigen sprechen ohnehin nur Deutsch.
        val content = if (voice.contains("Multilingual")) "<lang xml:lang='de-DE'>$escaped</lang>" else escaped
        val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
            "<voice name='$voice'>$content</voice></speak>"
        val request = Request.Builder()
            .url("https://$region.tts.speech.microsoft.com/cognitiveservices/v1")
            .header("Ocp-Apim-Subscription-Key", key)
            .header("X-Microsoft-OutputFormat", "audio-24khz-96kbitrate-mono-mp3")
            .header("User-Agent", "Cortex")
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
                    if (bytes == null || bytes.isEmpty()) throw CloudVoiceException("Azure hat kein Audio geliefert.", fatal = false)
                    return@withContext bytes
                }
                // F0 erlaubt nur rund 20 Anfragen je Minute: ein 429 ist meist dieses Ratenlimit.
                code == 429 && attempt < AZURE_WAIT_MS.size -> {
                    delay(retryAfter?.let { (it * 1000).coerceIn(1_000L, 60_000L) } ?: AZURE_WAIT_MS[attempt])
                    attempt++
                }
                code == 401 || code == 403 -> throw CloudVoiceException("Azure hat Schlüssel oder Region abgelehnt (Fehler $code). Prüf beides in den Einstellungen.")
                code == 429 -> throw CloudVoiceException("Azure lässt gerade keine Anfragen zu (Fehler 429): Ratenlimit oder Monatskontingent erschöpft.")
                code == 400 && hd -> throw CloudVoiceException("Azure lehnt die HD-Stimme ab (Fehler 400). HD-Stimmen laufen nur mit der Stufe S0 und nicht in jeder Region.")
                else -> throw CloudVoiceException("Azure antwortete mit Fehler $code: ${bytes?.toString(Charsets.UTF_8)?.take(200).orEmpty()}", fatal = false)
            }
        }
        @Suppress("UNREACHABLE_CODE")
        ByteArray(0)
    }

    companion object {
        const val GEMINI_FLASH_MODEL = "gemini-3.8-flash-tts"
        const val GEMINI_LITE_MODEL = "gemini-3.8-flash-lite-tts"
        private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

        /** So lange bleibt der kostenlose Gemini-Schluessel nach einem Fehler uebersprungen, wenn es einen Ausweichweg gibt. */
        private const val FREE_PAUSE_MS = 10 * 60_000L

        /** Gilt fuer alle Instanzen: sonst probierte jede neue Vorlese-Session den gescheiterten Schluessel erneut. */
        @Volatile private var freeBlockedUntil = 0L
        private val WAIT_MS = longArrayOf(1_000L, 3_000L, 8_000L)

        /** Reicht zusammen ueber das Minutenfenster von F0. */
        private val AZURE_WAIT_MS = longArrayOf(3_000L, 10_000L, 20_000L, 30_000L)
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
        private val SSML_TYPE = "application/ssml+xml".toMediaType()
    }
}

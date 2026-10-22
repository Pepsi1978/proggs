package de.frank.experimente.tts

import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini 3.8 Flash / Flash-Lite TTS und Azure Neural — derselbe Weg wie in Jarvis (Synthese.kt).
 *
 * Erzeugt je Absatz eine Datei (WAV bei Gemini, MP3 bei Azure); abgespielt wird sie vom
 * Absatzabspieler wie bei allen anderen Netz-Stimmen.
 */
class GeminiAzureTts(context: Context) {
    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Reihenfolge laut [wahl]: erst der kostenlose, dann der bezahlte Schlüssel. Ist [chirpRueckfall] an,
     * geht es nach einem Fehler des kostenlosen Schlüssels nicht zum bezahlten, sondern mit derselben
     * Stimme über [chirp] (Google Chirp 3 HD) weiter. Netzfehler laufen unverändert durch.
     */
    suspend fun gemini(
        text: String,
        lite: Boolean,
        stimmeId: String,
        freiRoh: String,
        bezahltRoh: String,
        wahl: String,
        chirpRueckfall: Boolean,
        chirp: suspend (stimme: String) -> File,
    ): File {
        val modell = if (lite) GEMINI_LITE_MODELL else GEMINI_FLASH_MODELL
        val frei = freiRoh.filterNot(Char::isWhitespace)
        val bezahlt = bezahltRoh.filterNot(Char::isWhitespace)
        var fehlerFrei: Exception? = null
        if (wahl != TtsCatalog.GEMINI_TTS_NUR_BEZAHLT) {
            val gesperrt = System.currentTimeMillis() < freiGesperrtBis
            if (frei.isNotBlank() && !gesperrt) {
                // Mit Ausweichweg nicht erst bei 429 warten — der nächste Weg ist schneller als jede Wartezeit.
                val ausweg = chirpRueckfall || (wahl == TtsCatalog.GEMINI_TTS_FREI_DANN_BEZAHLT && bezahlt.isNotBlank())
                try {
                    return geminiAnfrage(text, modell, stimmeId, frei, "kostenlos", warteBei429 = !ausweg)
                } catch (fehler: TtsPlaybackException) {
                    fehlerFrei = fehler
                    if (ausweg) freiGesperrtBis = System.currentTimeMillis() + FREI_PAUSE_MS
                    Log.w(TAG, "Gemini-Stimme: kostenloser Schlüssel gescheitert ($modell): ${fehler.message?.take(120)}")
                }
            }
            if (chirpRueckfall) {
                Log.i(TAG, "Gemini-Stimme: Rückfall auf Chirp 3 HD (${TtsCatalog.geminiName(stimmeId)}, gesperrt=$gesperrt)")
                return chirp(TtsCatalog.chirpZu(stimmeId))
            }
            if (wahl == TtsCatalog.GEMINI_TTS_NUR_FREI) {
                throw fehlerFrei ?: TtsPlaybackException("Es ist kein kostenloser Gemini-Schlüssel für die Stimme hinterlegt.")
            }
        }
        if (bezahlt.isBlank()) {
            throw fehlerFrei ?: TtsPlaybackException("Es ist kein Gemini-Schlüssel für die Stimme hinterlegt.")
        }
        return geminiAnfrage(text, modell, stimmeId, bezahlt, "bezahlt", warteBei429 = true)
    }

    private suspend fun geminiAnfrage(text: String, modell: String, stimmeId: String, schluessel: String, art: String, warteBei429: Boolean): File {
        // Gemini liest den Text wörtlich vor; eine Anweisung im Text würde mitgesprochen.
        val koerper = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", text)))))
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseModalities", JSONArray().put("AUDIO"))
                    .put(
                        "speechConfig",
                        JSONObject().put(
                            "voiceConfig",
                            JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", TtsCatalog.geminiName(stimmeId))),
                        ),
                    ),
            )
        val anfrage = Request.Builder()
            .url("$GEMINI_URL$modell:generateContent")
            .addHeader("x-goog-api-key", schluessel)
            .post(koerper.toString().toRequestBody(JSON_TYP))
            .build()
        val kandidat = JSONObject(String(fuehreAus(anfrage, warteBei429, "Gemini"), Charsets.UTF_8))
            .optJSONArray("candidates")?.optJSONObject(0)
        val ende = kandidat?.optString("finishReason").orEmpty()
        val teil = kandidat?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optJSONObject("inlineData")
        val daten = teil?.optString("data").orEmpty()
        if (daten.isBlank()) throw TtsPlaybackException("Gemini hat kein Audio geliefert ($ende).")
        if (ende.isNotBlank() && ende != "STOP") throw TtsPlaybackException("Gemini hat die Stimme abgebrochen ($ende).")
        val roh = Base64.decode(daten, Base64.DEFAULT)
        val rate = Regex("rate=(\\d+)").find(teil?.optString("mimeType").orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 24_000
        val wav = if (roh.size > 44 && String(roh, 0, 4, Charsets.US_ASCII) == "RIFF") roh else mitWavKopf(roh, rate)
        // Abgeschnittenes Audio meldet Gemini teils trotzdem als fertig: Unter 25 Zeichen je Sekunde spricht niemand.
        val sekunden = (wav.size - 44) / (wavRate(wav) * 2.0)
        if (text.length >= 60 && sekunden < text.length / 25.0) {
            throw TtsPlaybackException("Gemini-Audio zu kurz (${"%.1f".format(sekunden)} s für ${text.length} Zeichen).")
        }
        Log.i(TAG, "Gemini-Absatz erzeugt: modell=$modell schluessel=$art zeichen=${text.length}")
        return schreibeDatei(wav, "wav")
    }

    /**
     * Azure AI Speech über REST: Schlüssel und Region der eigenen Speech-Ressource (F0 kostenlos oder S0).
     * HD-Stimmen (DragonHD) bekommen weder Tempo noch <lang>-Rahmen: Sie bestimmen Betonung und Tempo selbst.
     */
    suspend fun azure(text: String, stimmeId: String, tempo: Float, schluesselRoh: String, regionRoh: String): File {
        val schluessel = schluesselRoh.filterNot(Char::isWhitespace)
        if (schluessel.isBlank()) throw TtsPlaybackException("Es ist kein Azure-Schlüssel hinterlegt.")
        val region = TtsCatalog.azureRegion(regionRoh)
            ?: throw TtsPlaybackException("Die Azure-Region „${regionRoh.take(40)}“ ist unbekannt. Trag den Regionsnamen ein, etwa westeurope.")
        val stimme = TtsCatalog.azureName(stimmeId)
        val hd = TtsCatalog.istAzureHd(stimmeId)
        // Steuerzeichen sind in XML verboten; Azure lehnte das ganze SSML sonst mit 400 ab.
        val escaped = ssmlEscape(text.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), " "))
        // <lang> kennt Azure nur bei den mehrsprachigen Stimmen; die übrigen sprechen ohnehin nur Deutsch.
        val inhalt = if (stimme.contains("Multilingual")) "<lang xml:lang='de-DE'>$escaped</lang>" else escaped
        val prozent = ((tempo.coerceIn(0.7f, 1.3f) - 1f) * 100).roundToInt()
        val mitTempo = if (hd || prozent == 0) inhalt else "<prosody rate='${if (prozent > 0) "+" else ""}$prozent%'>$inhalt</prosody>"
        val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
            "<voice name='$stimme'>$mitTempo</voice></speak>"
        val anfrage = Request.Builder()
            .url("https://$region.tts.speech.microsoft.com/cognitiveservices/v1")
            .addHeader("Ocp-Apim-Subscription-Key", schluessel)
            .addHeader("X-Microsoft-OutputFormat", "audio-24khz-96kbitrate-mono-mp3")
            .addHeader("User-Agent", "Vorleser")
            .post(ssml.toRequestBody(SSML_TYP))
            .build()
        val bytes = azureAnfrage(anfrage, hd)
        if (bytes.isEmpty()) throw TtsPlaybackException("Azure hat kein Audio geliefert.")
        Log.i(TAG, "Azure-Absatz erzeugt: stimme=$stimme zeichen=${text.length}")
        return schreibeDatei(bytes, "mp3")
    }

    /**
     * F0 erlaubt nur rund 20 Anfragen je Minute: Ein 429 ist meist dieses Ratenlimit, nicht das
     * Monatskontingent — darum nach Retry-After (höchstens eine Minute) warten.
     */
    private suspend fun azureAnfrage(anfrage: Request, hd: Boolean): ByteArray {
        var versuch = 0
        while (true) {
            val antwort = client.newCall(anfrage).warteAufAntwort()
            val warteSek = antwort.header("Retry-After")?.trim()?.toLongOrNull()
            val (koerper, code) = antwort.use { (if (it.isSuccessful) it.body?.bytes() else it.body?.string()?.toByteArray()) to it.code }
            when {
                code in 200..299 -> return koerper ?: ByteArray(0)
                code == 429 && versuch < AZURE_WARTEZEITEN_MS.size -> {
                    delay(warteSek?.let { (it * 1000).coerceIn(1_000L, 60_000L) } ?: AZURE_WARTEZEITEN_MS[versuch])
                    versuch++
                }
                code == 401 || code == 403 -> throw TtsPlaybackException("Azure hat Schlüssel oder Region abgelehnt (Fehler $code).")
                code == 429 -> throw TtsPlaybackException("Azure lässt gerade keine Anfragen zu (Fehler 429): Ratenlimit oder Monatskontingent erschöpft.")
                code == 400 && hd -> throw TtsPlaybackException("Azure lehnt die HD-Stimme ab (Fehler 400). HD-Stimmen laufen nur mit der Stufe S0 und nicht in jeder Region.")
                else -> throw TtsPlaybackException("Azure antwortete mit Fehler $code: ${koerper?.toString(Charsets.UTF_8)?.take(200).orEmpty()}")
            }
        }
    }

    private suspend fun fuehreAus(anfrage: Request, warteBei429: Boolean, wer: String): ByteArray {
        var versuch = 0
        while (true) {
            val (koerper, code) = client.newCall(anfrage).warteAufAntwort().use { it.body?.bytes() to it.code }
            when {
                code in 200..299 -> return koerper ?: ByteArray(0)
                code == 429 && warteBei429 && versuch < WARTEZEITEN_MS.size -> {
                    delay(WARTEZEITEN_MS[versuch])
                    versuch++
                }
                code == 401 || code == 403 -> throw TtsPlaybackException("$wer hat den Schlüssel abgelehnt (Fehler $code).")
                code == 429 -> throw TtsPlaybackException("Das Kontingent der $wer-Stimme ist erschöpft (Fehler 429).")
                else -> throw TtsPlaybackException("$wer antwortete mit Fehler $code: ${koerper?.toString(Charsets.UTF_8)?.take(200).orEmpty()}")
            }
        }
    }

    /** Rohes PCM (16 Bit, mono) bekommt einen WAV-Kopf, damit der MediaPlayer es abspielt. */
    private fun mitWavKopf(pcm: ByteArray, rate: Int): ByteArray {
        val kopf = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + pcm.size).put("WAVE".toByteArray())
            .put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(pcm.size)
        return kopf.array() + pcm
    }

    private fun wavRate(wav: ByteArray): Int =
        java.nio.ByteBuffer.wrap(wav, 24, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int.takeIf { it > 0 } ?: 24_000

    private fun ssmlEscape(text: String): String = text
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")

    private fun schreibeDatei(bytes: ByteArray, endung: String): File =
        File(appContext.cacheDir, "zusatz_tts_${UUID.randomUUID()}.$endung").apply { writeBytes(bytes) }

    fun shutdown() {
        runCatching {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }

    private companion object {
        const val TAG = "GeminiAzureTts"
        const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        const val GEMINI_FLASH_MODELL = "gemini-3.8-flash-tts"
        const val GEMINI_LITE_MODELL = "gemini-3.8-flash-lite-tts"

        /** So lange bleibt der kostenlose Gemini-Schlüssel nach einem Fehler übersprungen, wenn es einen Ausweichweg gibt. */
        const val FREI_PAUSE_MS = 10 * 60_000L

        /** Gilt für alle Instanzen: Sonst probierte jeder neue Vorleser den gescheiterten Schlüssel erneut. */
        @Volatile var freiGesperrtBis = 0L

        val JSON_TYP = "application/json; charset=utf-8".toMediaType()
        val SSML_TYP = "application/ssml+xml".toMediaType()
        val WARTEZEITEN_MS = longArrayOf(1_000L, 3_000L, 8_000L)

        /** Reicht zusammen über das Minutenfenster von F0. */
        val AZURE_WARTEZEITEN_MS = longArrayOf(3_000L, 10_000L, 20_000L, 30_000L)
    }
}

package de.frank.genialeideen.speech

import android.content.Context
import android.util.Base64
import de.frank.genialeideen.data.settings.SecureSettings
import de.frank.genialeideen.observability.IdeenLog
import de.frank.genialeideen.tts.TtsCatalog
import de.frank.genialeideen.tts.TtsProvider
import de.frank.genialeideen.tts.deutschesSsml
import de.frank.genialeideen.tts.ssmlEscape
import kotlin.math.roundToInt
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

/** Ein Fehler, der die ganze Sitzung betrifft — die Pipeline hält dann sofort an. */
class SyntheseAbbruch(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Ein Fehler an genau diesem Absatz — der Absatz wird halbiert und erneut versucht. */
class SyntheseFehler(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Erzeugt Audio, ohne es abzuspielen — die Voraussetzung für die Vorausschau der
 * Absatz-Pipeline (Baustein D 4.2, Schritt 3).
 */
class Synthese(context: Context, private val settings: SecureSettings) {
    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Begrenzt die Gleichzeitigkeit, sonst laufen die Dienste ins Rate-Limit (Baustein D 4.4). */
    private val gleichzeitig = Semaphore(2)

    /** Ob die gewählte Engine vorab synthetisieren kann. Edge spricht über den WebSocket-Player. */
    fun kannVorausschauen(): Boolean = when (settings.ttsProvider) {
        TtsProvider.GOOGLE_CLOUD.id, TtsProvider.QWEN_CLONE.id, TtsProvider.QWEN.id,
        TtsProvider.GEMINI_FLASH.id, TtsProvider.GEMINI_LITE.id, TtsProvider.AZURE.id -> true
        else -> false
    }

    /**
     * Prüft den Google-Schlüssel unabhängig von der gerade gewählten Stimme. Ohne diesen Weg
     * würde die Prüfung über [synthetisiere] laufen und bei einer Edge- oder Alibaba-Stimme
     * mit "kann nicht vorab erzeugt werden" abbrechen, obwohl der Schlüssel in Ordnung ist.
     */
    suspend fun pruefeGoogle(text: String = "Probe."): File = gleichzeitig.withPermit { google(text) }

    suspend fun synthetisiere(text: String): File = gleichzeitig.withPermit {
        when (settings.ttsProvider) {
            TtsProvider.GOOGLE_CLOUD.id -> google(text)
            TtsProvider.GEMINI_FLASH.id -> gemini(text, GEMINI_FLASH_MODELL, settings.geminiFlashTtsVoice)
            TtsProvider.GEMINI_LITE.id -> gemini(text, GEMINI_LITE_MODELL, settings.geminiLiteTtsVoice)
            TtsProvider.AZURE.id -> azure(text, settings.azureTtsVoice, settings.ttsSpeechRate)
            TtsProvider.QWEN_CLONE.id -> qwen(text, geklont = true)
            TtsProvider.QWEN.id -> qwen(text, geklont = false)
            else -> throw SyntheseAbbruch("Diese Stimme kann nicht vorab erzeugt werden.")
        }
    }

    private suspend fun google(text: String, sonderStimme: String? = null): File {
        val schluessel = settings.googleTtsApiKey.trim()
        if (schluessel.isBlank()) {
            throw SyntheseAbbruch(
                "Vorlesen fehlgeschlagen: Es ist kein Google-Schlüssel hinterlegt. " +
                    "Trag ihn in den Einstellungen ein.",
            )
        }
        val stimme = sonderStimme ?: settings.googleTtsVoice
        // Chirp 3 HD versteht kein SSML — dort bleibt es beim reinen Text, die Sprache steuert
        // allein der languageCode. Alle uebrigen Google-Stimmen werden per <lang> festgenagelt.
        val alsSsml = settings.immerDeutschVorlesen && !stimme.contains("Chirp")
        val koerper = JSONObject()
            .put(
                "input",
                if (alsSsml) {
                    JSONObject().put("ssml", deutschesSsml(text))
                } else {
                    JSONObject().put("text", text)
                },
            )
            .put("voice", JSONObject().put("languageCode", "de-DE").put("name", stimme))
            .put(
                "audioConfig",
                JSONObject()
                    .put("audioEncoding", "MP3")
                    .put("speakingRate", settings.ttsSpeechRate.coerceIn(0.25f, 4f).toDouble()),
                // Chirp-3-HD-Stimmen kennen keinen pitch-Parameter — er wird gar nicht erst
                // gesendet, sonst antwortet Google mit Fehler 400.
            )
        val adresse = GOOGLE_URL.toHttpUrl().newBuilder().addQueryParameter("key", schluessel).build()
        val anfrage = Request.Builder()
            .url(adresse)
            .post(koerper.toString().toRequestBody(JSON_TYP))
            .build()
        val antwort = fuehreAus(anfrage)
        val json = JSONObject(antwort)
        val audio = json.optString("audioContent")
        if (audio.isBlank()) throw SyntheseFehler("Google hat kein Audio geliefert.")
        return schreibeDatei(Base64.decode(audio, Base64.DEFAULT), "mp3")
    }

    /**
     * Azure AI Speech über REST: Schlüssel und Region der eigenen Speech-Ressource (F0 kostenlos oder S0).
     * HD-Stimmen (DragonHD) bekommen weder Tempo noch <lang>-Rahmen: Sie bestimmen Betonung und Tempo selbst.
     */
    private suspend fun azure(text: String, stimmeId: String, tempo: Float): File {
        val schluessel = settings.azureTtsKey.filterNot(Char::isWhitespace)
        if (schluessel.isBlank()) {
            throw SyntheseAbbruch("Vorlesen fehlgeschlagen: Es ist kein Azure-Schlüssel hinterlegt. Trag ihn in den Einstellungen ein.")
        }
        val region = TtsCatalog.azureRegion(settings.azureTtsRegion) ?: throw SyntheseAbbruch(
            "Vorlesen fehlgeschlagen: Die Azure-Region „${settings.azureTtsRegion.take(40)}“ verstehe ich nicht. Trag den Regionsnamen ein, etwa westeurope.",
        )
        val stimme = TtsCatalog.azureName(stimmeId)
        val hd = TtsCatalog.istAzureHd(stimmeId)
        // Steuerzeichen sind in XML verboten; Azure lehnte das ganze SSML sonst mit 400 ab.
        val escaped = ssmlEscape(text.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), " "))
        // <lang> kennt Azure nur bei den mehrsprachigen Stimmen; die übrigen sprechen ohnehin nur Deutsch.
        val inhalt = if (settings.immerDeutschVorlesen && stimme.contains("Multilingual")) "<lang xml:lang='de-DE'>$escaped</lang>" else escaped
        val prozent = ((tempo.coerceIn(0.7f, 1.3f) - 1f) * 100).roundToInt()
        val mitTempo = if (hd || prozent == 0) inhalt else "<prosody rate='${if (prozent > 0) "+" else ""}$prozent%'>$inhalt</prosody>"
        val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
            "<voice name='$stimme'>$mitTempo</voice></speak>"
        val beginn = System.currentTimeMillis()
        val anfrage = Request.Builder()
            .url("https://$region.tts.speech.microsoft.com/cognitiveservices/v1")
            .addHeader("Ocp-Apim-Subscription-Key", schluessel)
            .addHeader("X-Microsoft-OutputFormat", "audio-24khz-96kbitrate-mono-mp3")
            .addHeader("User-Agent", "GenialeIdeen")
            .post(ssml.toRequestBody(SSML_TYP))
            .build()
        val bytes = azureAnfrage(anfrage, hd)
        if (bytes.isEmpty()) throw SyntheseFehler("Azure hat kein Audio geliefert.")
        IdeenLog.info("Synthese", "azure", "Azure-Absatz erzeugt", mapOf("stimme" to stimme, "zeichen" to text.length, "ms" to System.currentTimeMillis() - beginn))
        return schreibeDatei(bytes, "mp3")
    }

    /**
     * Schickt die Azure-Anfrage; die Antwort ist Ton statt Text. F0 erlaubt nur rund 20 Anfragen je Minute: Ein 429 ist
     * meist dieses Ratenlimit, nicht das Monatskontingent — darum nach Retry-After (höchstens eine Minute) warten.
     */
    private suspend fun azureAnfrage(anfrage: Request, hd: Boolean): ByteArray {
        var versuch = 0
        while (true) {
            val antwort = client.newCall(anfrage).warte()
            val warteSek = antwort.header("Retry-After")?.trim()?.toLongOrNull()
            val (koerper, code) = antwort.use { (if (it.isSuccessful) it.body?.bytes() else it.body?.string()?.toByteArray()) to it.code }
            when {
                code in 200..299 -> return koerper ?: ByteArray(0)
                code == 429 && versuch < AZURE_WARTEZEITEN_MS.size -> {
                    val ms = warteSek?.let { (it * 1000).coerceIn(1_000L, 60_000L) } ?: AZURE_WARTEZEITEN_MS[versuch]
                    IdeenLog.warn("Synthese", "azureAnfrage", "Rate-Limit, warte erneut", mapOf("ms" to ms, "versuch" to versuch))
                    kotlinx.coroutines.delay(ms)
                    versuch++
                }
                code == 401 || code == 403 -> throw SyntheseAbbruch(
                    "Vorlesen fehlgeschlagen: Azure hat Schlüssel oder Region abgelehnt (Fehler $code). Prüf beides in den Einstellungen.",
                )
                code == 429 -> throw SyntheseAbbruch(
                    "Vorlesen fehlgeschlagen: Azure lässt gerade keine Anfragen zu (Fehler 429): Ratenlimit oder Monatskontingent erschöpft.",
                )
                code == 400 && hd -> throw SyntheseAbbruch(
                    "Vorlesen fehlgeschlagen: Azure lehnt die HD-Stimme ab (Fehler 400). HD-Stimmen laufen nur mit der Stufe S0 und nicht in jeder Region.",
                )
                else -> throw SyntheseFehler("Azure antwortete mit Fehler $code: ${koerper?.toString(Charsets.UTF_8)?.take(200).orEmpty()}")
            }
        }
    }

    /**
     * Gemini 3.8 Flash bzw. Flash-Lite TTS. Reihenfolge laut [SecureSettings.geminiTtsSchluesselWahl]: erst der
     * kostenlose, dann der bezahlte Schlüssel. Ist [SecureSettings.geminiTtsChirpRueckfall] an, geht es nach einem
     * Fehler des kostenlosen Schlüssels nicht zum bezahlten, sondern mit derselben Stimme zu Chirp 3 HD.
     * Netzfehler laufen unverändert durch — die wiederholt der Vorleser selbst.
     */
    private suspend fun gemini(text: String, modell: String, stimmeId: String): File {
        val wahl = settings.geminiTtsSchluesselWahl
        val frei = settings.geminiTtsKeyFrei.filterNot(Char::isWhitespace)
        val bezahlt = settings.geminiTtsKeyBezahlt.filterNot(Char::isWhitespace)
        val chirp = settings.geminiTtsChirpRueckfall
        var fehlerFrei: Exception? = null
        if (wahl != SecureSettings.GEMINI_TTS_NUR_BEZAHLT) {
            val gesperrt = System.currentTimeMillis() < freiGesperrtBis
            if (frei.isNotBlank() && !gesperrt) {
                // Mit Ausweichweg nicht erst bei 429 warten — der nächste Weg ist schneller als jede Wartezeit.
                val ausweg = chirp || (wahl == SecureSettings.GEMINI_TTS_FREI_DANN_BEZAHLT && bezahlt.isNotBlank())
                try {
                    return geminiAnfrage(text, modell, stimmeId, frei, "kostenlos", warteBei429 = !ausweg)
                } catch (fehler: Exception) {
                    if (fehler !is SyntheseAbbruch && fehler !is SyntheseFehler) throw fehler
                    fehlerFrei = fehler
                    if (ausweg) freiGesperrtBis = System.currentTimeMillis() + FREI_PAUSE_MS
                    IdeenLog.warn("Synthese", "gemini", "Gemini-Stimme: kostenloser Schlüssel gescheitert", mapOf("modell" to modell, "grund" to fehler.message?.take(120)))
                }
            }
            if (chirp) {
                IdeenLog.info("Synthese", "gemini", "Gemini-Stimme: Rückfall auf Chirp 3 HD", mapOf("stimme" to TtsCatalog.geminiName(stimmeId), "gesperrt" to gesperrt))
                return google(text, TtsCatalog.chirpZu(stimmeId))
            }
            if (wahl == SecureSettings.GEMINI_TTS_NUR_FREI) {
                throw fehlerFrei ?: SyntheseAbbruch(
                    "Vorlesen fehlgeschlagen: Es ist kein kostenloser Gemini-Schlüssel für die Stimme hinterlegt. Trag ihn in den Einstellungen ein.",
                )
            }
        }
        if (bezahlt.isBlank()) {
            throw fehlerFrei ?: SyntheseAbbruch(
                "Vorlesen fehlgeschlagen: Es ist kein Gemini-Schlüssel für die Stimme hinterlegt. Trag ihn in den Einstellungen ein.",
            )
        }
        return geminiAnfrage(text, modell, stimmeId, bezahlt, "bezahlt", warteBei429 = true)
    }

    private suspend fun geminiAnfrage(text: String, modell: String, stimmeId: String, schluessel: String, art: String, warteBei429: Boolean): File {
        val beginn = System.currentTimeMillis()
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
        val kandidat = JSONObject(fuehreAus(anfrage, warteBei429)).optJSONArray("candidates")?.optJSONObject(0)
        val ende = kandidat?.optString("finishReason").orEmpty()
        val teil = kandidat?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optJSONObject("inlineData")
        val daten = teil?.optString("data").orEmpty()
        if (daten.isBlank()) throw SyntheseFehler("Gemini hat kein Audio geliefert ($ende).")
        if (ende.isNotBlank() && ende != "STOP") throw SyntheseFehler("Gemini hat die Stimme abgebrochen ($ende).")
        val roh = Base64.decode(daten, Base64.DEFAULT)
        val rate = Regex("rate=(\\d+)").find(teil?.optString("mimeType").orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 24_000
        val wav = if (roh.size > 44 && String(roh, 0, 4, Charsets.US_ASCII) == "RIFF") roh else mitWavKopf(roh, rate)
        // Abgeschnittenes Audio meldet Gemini teils trotzdem als fertig: Unter 25 Zeichen je Sekunde spricht niemand.
        val sekunden = (wav.size - 44) / (wavRate(wav) * 2.0)
        if (text.length >= 60 && sekunden < text.length / 25.0) {
            throw SyntheseFehler("Gemini-Audio zu kurz (${"%.1f".format(sekunden)} s für ${text.length} Zeichen).")
        }
        IdeenLog.info("Synthese", "geminiAnfrage", "Gemini-Absatz erzeugt", mapOf("modell" to modell, "schluessel" to art, "zeichen" to text.length, "ms" to System.currentTimeMillis() - beginn))
        return schreibeDatei(wav, "wav")
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

    /**
     * @param geklont true für meine eigene Stimme (Voice-Clone-Modell), false für die fertigen
     *   Alibaba-Standardstimmen. Klonen und Sprechen müssen dasselbe Modell benutzen, sonst wird
     *   die Stimm-Kennung abgelehnt — darum zwei Modellnamen.
     */
    private suspend fun qwen(text: String, geklont: Boolean): File {
        val schluessel = settings.qwenTtsApiKey.filterNot(Char::isWhitespace)
        val stimme = if (geklont) {
            settings.qwenTtsVoiceId.filterNot(Char::isWhitespace)
        } else {
            settings.qwenStandardVoice
        }
        if (schluessel.isBlank()) {
            throw SyntheseAbbruch(
                "Vorlesen fehlgeschlagen: Der Alibaba-Schlüssel fehlt. Trag ihn in den Einstellungen ein.",
            )
        }
        if (stimme.isBlank()) {
            throw SyntheseAbbruch(
                "Vorlesen fehlgeschlagen: Die eigene Stimme ist nicht eingerichtet. " +
                    "Nimm sie in den Einstellungen auf.",
            )
        }
        val koerper = JSONObject()
            .put("model", if (geklont) QWEN_KLON_MODELL else QWEN_MODELL)
            .put(
                "input",
                JSONObject().put("text", text).put("voice", stimme).put("language_type", "German"),
            )
        val anfrage = Request.Builder()
            .url(QWEN_URL)
            .addHeader("Authorization", "Bearer $schluessel")
            .post(koerper.toString().toRequestBody(JSON_TYP))
            .build()
        val antwort = fuehreAus(anfrage)
        val rohAdresse = JSONObject(antwort)
            .optJSONObject("output")?.optJSONObject("audio")?.optString("url").orEmpty()
        if (rohAdresse.isBlank()) throw SyntheseFehler("Die eigene Stimme hat kein Audio geliefert.")
        // Die Adresse kommt teils als http — Android blockt das, also anheben.
        val adresse = if (rohAdresse.startsWith("http://")) {
            "https://" + rohAdresse.removePrefix("http://")
        } else {
            rohAdresse
        }
        val bytes = ladeHerunter(adresse)
        return schreibeDatei(bytes, "wav")
    }

    private suspend fun fuehreAus(anfrage: Request, warteBei429: Boolean = true): String {
        var versuch = 0
        while (true) {
            val antwort = client.newCall(anfrage).warte()
            val text = antwort.use { it.body?.string().orEmpty() to it.code }
            val (koerper, code) = text
            when {
                code in 200..299 -> return koerper
                // 429: exponentiell warten und erneut versuchen (Baustein D 4.4).
                code == 429 && warteBei429 && versuch < WARTEZEITEN_MS.size -> {
                    kotlinx.coroutines.delay(WARTEZEITEN_MS[versuch])
                    de.frank.genialeideen.observability.IdeenLog.warn(
                        "Synthese",
                        "fuehreAus",
                        "Rate-Limit, warte erneut",
                        mapOf("ms" to WARTEZEITEN_MS[versuch], "versuch" to versuch),
                    )
                    versuch++
                }
                code == 401 || code == 403 -> throw SyntheseAbbruch(
                    "Vorlesen fehlgeschlagen: Der Schlüssel wurde abgelehnt (Fehler $code). " +
                        "Prüf ihn in den Einstellungen.",
                )
                code == 429 -> throw SyntheseAbbruch(
                    "Vorlesen fehlgeschlagen: Das Kontingent der Stimme ist erschöpft (Fehler 429).",
                )
                else -> throw SyntheseFehler("Die Stimme antwortete mit Fehler $code: ${koerper.take(200)}")
            }
        }
    }

    private suspend fun ladeHerunter(adresse: String): ByteArray {
        val antwort = client.newCall(Request.Builder().url(adresse).build()).warte()
        return antwort.use {
            if (!it.isSuccessful) throw SyntheseFehler("Das Audio konnte nicht geladen werden (${it.code}).")
            it.body?.bytes() ?: throw SyntheseFehler("Das geladene Audio war leer.")
        }
    }

    private fun schreibeDatei(bytes: ByteArray, endung: String): File {
        val verzeichnis = File(appContext.cacheDir, "vorlesen").apply { mkdirs() }
        return File(verzeichnis, "absatz_${UUID.randomUUID()}.$endung").apply { writeBytes(bytes) }
    }

    /** Räumt alle Zwischendateien weg, die eine beendete Wiedergabe hinterlassen hat. */
    fun raeumeAuf() {
        runCatching { File(appContext.cacheDir, "vorlesen").listFiles()?.forEach(File::delete) }
    }

    private suspend fun Call.warte(): Response = suspendCancellableCoroutine { fortsetzung ->
        fortsetzung.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (fortsetzung.isActive) fortsetzung.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                fortsetzung.resume(response) { _, abgebrochen, _ -> abgebrochen.close() }
            }
        })
    }

    private companion object {
        const val GOOGLE_URL = "https://texttospeech.googleapis.com/v1/text:synthesize"
        const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        const val GEMINI_FLASH_MODELL = "gemini-3.8-flash-tts"
        const val GEMINI_LITE_MODELL = "gemini-3.8-flash-lite-tts"

        /** So lange bleibt der kostenlose Gemini-Schlüssel nach einem Fehler übersprungen, wenn es einen Ausweichweg gibt. */
        const val FREI_PAUSE_MS = 10 * 60_000L

        /** Gilt für alle Instanzen: Sonst probierte jeder neue Vorleser den gescheiterten Schlüssel erneut. */
        @Volatile var freiGesperrtBis = 0L
        const val QWEN_URL =
            "https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"
        /** Die fertigen Standardstimmen. */
        const val QWEN_MODELL = "qwen3-tts-flash"

        /** Die geklonten Stimmen — dasselbe Modell wie beim Anlegen (Baustein E). */
        const val QWEN_KLON_MODELL = "qwen3-tts-vc-2026-01-22"
        val JSON_TYP = "application/json; charset=utf-8".toMediaType()
        val WARTEZEITEN_MS = longArrayOf(1_000L, 3_000L, 8_000L)
        val SSML_TYP = "application/ssml+xml".toMediaType()

        /** Reicht zusammen über das Minutenfenster von F0. */
        val AZURE_WARTEZEITEN_MS = longArrayOf(3_000L, 10_000L, 20_000L, 30_000L)
    }
}

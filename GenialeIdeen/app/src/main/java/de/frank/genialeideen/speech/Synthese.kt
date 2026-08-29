package de.frank.genialeideen.speech

import android.content.Context
import android.util.Base64
import de.frank.genialeideen.data.settings.SecureSettings
import de.frank.genialeideen.tts.TtsProvider
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
        TtsProvider.GOOGLE_CLOUD.id, TtsProvider.QWEN_CLONE.id, TtsProvider.QWEN.id -> true
        else -> false
    }

    suspend fun synthetisiere(text: String): File = gleichzeitig.withPermit {
        when (settings.ttsProvider) {
            TtsProvider.GOOGLE_CLOUD.id -> google(text)
            TtsProvider.QWEN_CLONE.id -> qwen(text, geklont = true)
            TtsProvider.QWEN.id -> qwen(text, geklont = false)
            else -> throw SyntheseAbbruch("Diese Stimme kann nicht vorab erzeugt werden.")
        }
    }

    private suspend fun google(text: String): File {
        val schluessel = settings.googleTtsApiKey.trim()
        if (schluessel.isBlank()) {
            throw SyntheseAbbruch(
                "Vorlesen fehlgeschlagen: Es ist kein Google-Schlüssel hinterlegt. " +
                    "Trag ihn in den Einstellungen ein.",
            )
        }
        val stimme = settings.googleTtsVoice
        val koerper = JSONObject()
            .put("input", JSONObject().put("text", text))
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

    private suspend fun fuehreAus(anfrage: Request): String {
        var versuch = 0
        while (true) {
            val antwort = client.newCall(anfrage).warte()
            val text = antwort.use { it.body?.string().orEmpty() to it.code }
            val (koerper, code) = text
            when {
                code in 200..299 -> return koerper
                // 429: exponentiell warten und erneut versuchen (Baustein D 4.4).
                code == 429 && versuch < WARTEZEITEN_MS.size -> {
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
        const val QWEN_URL =
            "https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"
        /** Die fertigen Standardstimmen. */
        const val QWEN_MODELL = "qwen3-tts-flash"

        /** Die geklonten Stimmen — dasselbe Modell wie beim Anlegen (Baustein E). */
        const val QWEN_KLON_MODELL = "qwen3-tts-vc-2026-01-22"
        val JSON_TYP = "application/json; charset=utf-8".toMediaType()
        val WARTEZEITEN_MS = longArrayOf(1_000L, 3_000L, 8_000L)
    }
}

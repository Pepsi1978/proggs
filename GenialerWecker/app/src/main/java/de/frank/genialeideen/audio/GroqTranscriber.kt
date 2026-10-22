package de.frank.genialeideen.audio

import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import de.frank.genialeideen.network.shutdownSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

class GroqTranscriber(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val filterStille: Boolean = true,
    private val filterMetriken: Boolean = true,
    private val filterZeitstempel: Boolean = true,
    private val filterFloskeln: Boolean = true,
    /** Wörterbuch-Prompt aus [vokabularPrompt]; null = ohne persönliches Wörterbuch. */
    private val prompt: String? = null,
) {
    private val analyzer = SpeechAnalyzer()
    private val filter = WhisperHallucinationFilter()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    suspend fun transcribe(wav: ByteArray): String = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            throw GroqTranscriptionException("Groq ist auf diesem Geraet nicht konfiguriert.")
        }
        if (wav.size > MAX_FILE_BYTES) {
            throw GroqTranscriptionException("Die Aufnahme ist fuer die Groq-Transkription zu gross.")
        }

        val analysis = analyzer.analyze(wav)
        if (filterStille && analysis != null && analysis.voicedMs < SpeechAnalyzer.MIN_SPEECH_MS) {
            logger.info("Layer 1: recording rejected before upload (${analysis.voicedMs} ms voiced)")
            return@withContext ""
        }

        // large-v3 ist nach einer Groq-Sperre 30 Minuten lang durch Turbo ersetzt.
        var modell = if (model == MODEL_LARGE_V3 && System.currentTimeMillis() < turboErzwungenBis) MODEL_TURBO else model
        var parsed = try {
            sende(wav, modell, prompt)
        } catch (gesperrt: ModellGesperrtException) {
            // large-v3 ist für die Groq-Organisation nicht freigeschaltet: Diktat nicht verlieren, einmal mit Turbo.
            logger.warning("model_blocked_fallback_turbo ($modell)")
            turboErzwungenBis = System.currentTimeMillis() + SPERRE_MS
            modell = MODEL_TURBO
            sende(wav, modell, prompt)
        }
        // Whisper gibt bei Stille/Gemurmel gern den Prompt selbst aus: dann einmal ohne Wörterbuch wiederholen.
        if (istPromptEcho(parsed.text, prompt)) {
            logger.warning("prompt_echo_retry_without_prompt (${parsed.text.length} chars)")
            parsed = sende(wav, modell, null)
        }
        filter.filter(parsed, analysis, filterMetriken, filterZeitstempel, filterFloskeln)
    }

    private suspend fun sende(wav: ByteArray, modell: String, prompt: String?): GroqTranscriptionResponse {
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "audio.wav", wav.toRequestBody(WAV_MEDIA_TYPE))
            .addFormDataPart("model", modell)
            .addFormDataPart("language", "de")
            .addFormDataPart("response_format", "verbose_json")
            .addFormDataPart("temperature", "0")
        if (prompt != null) builder.addFormDataPart("prompt", prompt)
        val request = Request.Builder()
            .url(TRANSCRIPTIONS_URL)
            .header("Authorization", "Bearer $apiKey")
            .post(builder.build())
            .build()

        return client.newCall(request).await().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                if (modell == MODEL_LARGE_V3 && istModellGesperrt(responseBody)) throw ModellGesperrtException()
                throw GroqTranscriptionException(httpError(response.code, responseBody))
            }
            runCatching { parseResponse(responseBody) }.getOrElse { error ->
                throw GroqTranscriptionException("Groq hat eine ungueltige Antwort geliefert.", error)
            }
        }
    }

    private class ModellGesperrtException : Exception("model_permission_blocked_org")

    internal fun parseResponse(body: String): GroqTranscriptionResponse {
        val json = JSONObject(body)
        val segments = json.optJSONArray("segments")?.let { array ->
            (0 until array.length()).map { index ->
                val segment = array.getJSONObject(index)
                GroqSegment(
                    start = segment.optDoubleOrNull("start"),
                    end = segment.optDoubleOrNull("end"),
                    text = segment.optString("text"),
                    noSpeechProbability = segment.optDoubleOrNull("no_speech_prob"),
                    averageLogProbability = segment.optDoubleOrNull("avg_logprob"),
                    compressionRatio = segment.optDoubleOrNull("compression_ratio"),
                )
            }
        }
        return GroqTranscriptionResponse(json.optString("text").trim(), segments)
    }

    fun shutdown() {
        client.shutdownSafely(logger)
    }

    private fun JSONObject.optDoubleOrNull(name: String): Double? =
        optDouble(name).takeUnless(Double::isNaN)

    private fun httpError(status: Int, body: String): String {
        val detail = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()
        return when (status) {
            401, 403 -> "Der konfigurierte Groq-Schluessel wurde abgelehnt."
            413 -> "Die Aufnahme ist fuer die Groq-Transkription zu gross."
            429 -> "Groq hat zu viele Anfragen erhalten. Bitte versuche es spaeter erneut."
            in 500..599 -> "Groq ist derzeit nicht erreichbar. Bitte versuche es spaeter erneut."
            else -> detail?.takeIf(String::isNotBlank)?.let { "Groq-Fehler $status: $it" }
                ?: "Groq-Fehler $status."
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        GroqTranscriptionException(
                            "Groq ist ueber die aktuelle Verbindung nicht erreichbar.",
                            e,
                        ),
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) {
                    continuation.resume(response) { _, value, _ -> value.close() }
                } else {
                    response.close()
                }
            }
        })
    }

    companion object {
        internal const val TRANSCRIPTIONS_URL =
            "https://api.groq.com/openai/v1/audio/transcriptions"
        internal const val MAX_FILE_BYTES = 25 * 1024 * 1024
        const val MODEL_TURBO = "whisper-large-v3-turbo"
        const val MODEL_LARGE_V3 = "whisper-large-v3"
        const val DEFAULT_MODEL = MODEL_TURBO

        /** Auswahl in den Einstellungen: Modell-ID zu Anzeige. */
        val MODELLE = listOf(MODEL_TURBO to "Large V3 Turbo (schnell)", MODEL_LARGE_V3 to "Large V3 (genauer)")

        fun modellName(modell: String): String = MODELLE.firstOrNull { it.first == modell }?.second ?: MODELLE.first().second

        /** Voreinstellung des persönlichen Wörterbuchs (Stand TerminalVoiceOverlay). */
        const val STANDARD_WOERTERBUCH = "Bug-Almanach, Sonnet-Schwarm, Groq, Whisper, APK-Update, UpdateStation, OpenLauncher, Jarvis, debugging, Logcat, Harness, Best Practices, VCO, CVO, ChatGPT, Claude, Claude Code, OpenAI, Anthropic, bug almanach, Anthrophic"

        private const val ECHO_MARKER = "Begriffe"
        private const val MAX_PROMPT_CHARS = 350
        private const val MAX_BEGRIFFE = 100
        private const val SPERRE_MS = 30 * 60 * 1000L

        /** Nach einer Groq-Sperre für large-v3 bis hierhin direkt Turbo nehmen. */
        @Volatile private var turboErzwungenBis = 0L

        /** Neue Modellwahl in den Einstellungen hebt die 30-Minuten-Sperre sofort auf. */
        fun sperreAufheben() { turboErzwungenBis = 0L }

        fun begriffe(text: String): List<String> = text
            .split('\n', '\r', ',', ';')
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .distinctBy { it.lowercase() }
            .take(MAX_BEGRIFFE)

        /** Wörterbuch als natürlicher Satz; Whisper übernimmt daraus Schreibweisen. Null bei Schalter aus oder ohne Begriffe. */
        fun vokabularPrompt(an: Boolean, text: String): String? {
            if (!an) return null
            val sb = StringBuilder("$ECHO_MARKER: ")
            var added = 0
            for (w in begriffe(text)) {
                // Zu lange Einträge überspringen, nicht abbrechen: die übrigen Begriffe zählen.
                if (sb.length + w.length + 2 > MAX_PROMPT_CHARS) continue
                if (added++ > 0) sb.append(", ")
                sb.append(w)
            }
            return if (added == 0) null else sb.append('.').toString()
        }

        internal fun istPromptEcho(text: String, prompt: String?): Boolean {
            if (prompt.isNullOrEmpty() || text.isBlank()) return false
            fun norm(s: String) = s.lowercase().filter(Char::isLetterOrDigit)
            val t = norm(text)
            val marker = norm(ECHO_MARKER)
            val p = norm(prompt)
            if (t.startsWith(marker) && p.contains(t.substring(marker.length))) return true
            // Teil-Echo: bei Stille gibt Whisper oft nur Ende oder Mitte des Prompts aus.
            return t.length >= 12 && p.contains(t)
        }

        internal fun istModellGesperrt(body: String): Boolean =
            body.contains("model_permission_blocked_org") || body.contains("blocked at the organization level")

        private val WAV_MEDIA_TYPE = "audio/wav".toMediaType()
        private val logger = Logger.getLogger(GroqTranscriber::class.java.name)
    }
}

class GroqTranscriptionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

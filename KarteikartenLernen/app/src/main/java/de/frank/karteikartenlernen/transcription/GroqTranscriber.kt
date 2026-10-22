package de.frank.karteikartenlernen.transcription

import android.util.Log
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONObject

/**
 * [model] ist das Standardmodell; [modelProvider] liefert die Wahl aus den Einstellungen und
 * [promptProvider] den Wörterbuch-Satz aus [vokabularPrompt] (null = ohne Wörterbuch), beide je Aufnahme.
 */
class GroqTranscriber(
    private val apiKeyProvider: () -> String,
    private val model: String,
    private val modelProvider: () -> String = { model },
    private val promptProvider: () -> String? = { null },
) {
    private val apiKey: String get() = apiKeyProvider().trim()

    private val speechAnalyzer = SpeechAnalyzer()
    private val hallucinationFilter = WhisperHallucinationFilter()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    suspend fun transcribe(file: File): String = withContext(Dispatchers.IO) {
        if (!isConfigured) throw GroqTranscriptionException("Bitte in den Einstellungen den Groq-API-Schlüssel eintragen.")
        if (file.length() > MAX_FILE_BYTES) throw GroqTranscriptionException("Die Aufnahme ist für die Groq-Transkription zu groß.")
        val analysis = speechAnalyzer.analyze(file.readBytes())
        if (analysis != null && analysis.voicedMs < SpeechAnalyzer.MIN_SPEECH_MS) return@withContext ""
        val prompt = promptProvider()
        // large-v3 ist nach einer Groq-Sperre 30 Minuten lang durch Turbo ersetzt.
        val gewaehlt = modelProvider()
        var modell = if (gewaehlt == MODEL_LARGE_V3 && System.currentTimeMillis() < turboErzwungenBis) MODEL_TURBO else gewaehlt
        var result = try {
            sende(file, modell, prompt)
        } catch (gesperrt: ModellGesperrtException) {
            // large-v3 ist für die Groq-Organisation nicht freigeschaltet: Diktat nicht verlieren, einmal mit Turbo.
            Log.w(TAG, "model_blocked_fallback_turbo ($modell)")
            turboErzwungenBis = System.currentTimeMillis() + SPERRE_MS
            modell = MODEL_TURBO
            sende(file, modell, prompt)
        }
        // Whisper gibt bei Stille/Gemurmel gern den Prompt selbst aus: dann einmal ohne Wörterbuch wiederholen.
        if (istPromptEcho(result.text, prompt)) {
            Log.w(TAG, "prompt_echo_retry_without_prompt (${result.text.length} chars)")
            result = sende(file, modell, null)
        }
        return@withContext hallucinationFilter.filter(result, analysis)
    }

    private suspend fun sende(file: File, modell: String, prompt: String?): GroqTranscriptionResponse {
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("audio/wav".toMediaType()))
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
        val response = client.newCall(request).await()
        response.use {
            val responseBody = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                if (modell == MODEL_LARGE_V3 && istModellGesperrt(responseBody)) throw ModellGesperrtException()
                throw GroqTranscriptionException(httpError(it.code, responseBody))
            }
            return runCatching { parseResponse(responseBody) }
                .getOrElse { throw GroqTranscriptionException("Groq hat eine ungültige Antwort geliefert.") }
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
        return GroqTranscriptionResponse(text = json.optString("text").trim(), segments = segments)
    }

    private fun JSONObject.optDoubleOrNull(name: String): Double? = optDouble(name).takeUnless(Double::isNaN)

    fun shutdown() {
        client.dispatcher.cancelAll()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private fun httpError(status: Int, body: String): String {
        val detail = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
        return when (status) {
            401, 403 -> "Der lokal konfigurierte Groq-Schlüssel wurde abgelehnt."
            413 -> "Die Aufnahme ist für die Groq-Transkription zu groß."
            429 -> "Groq hat zu viele Anfragen erhalten. Bitte versuche es später erneut."
            in 500..599 -> "Groq ist derzeit nicht erreichbar. Bitte versuche es später erneut."
            else -> detail?.takeIf { it.isNotBlank() }?.let { "Groq-Fehler $status: $it" } ?: "Groq-Fehler $status."
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (continuation.isActive) continuation.resumeWithException(
                    GroqTranscriptionException("Groq ist über die aktuelle Verbindung nicht erreichbar.", error),
                )
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) continuation.resume(response) { _, value, _ -> value.close() }
                else response.close()
            }
        })
    }

    companion object {
        internal const val TRANSCRIPTIONS_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
        internal const val MAX_FILE_BYTES = 25L * 1024 * 1024
        private const val TAG = "GroqTranscriber"
        const val MODEL_TURBO = "whisper-large-v3-turbo"
        const val MODEL_LARGE_V3 = "whisper-large-v3"

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
            return t.startsWith(marker) && norm(prompt).contains(t.substring(marker.length))
        }

        internal fun istModellGesperrt(body: String): Boolean =
            body.contains("model_permission_blocked_org") || body.contains("blocked at the organization level")
    }
}

class GroqTranscriptionException(message: String, cause: Throwable? = null) : Exception(message, cause)

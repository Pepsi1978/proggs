package de.frank.stacklabor.werftstudio.service.speech

import android.util.Log
import de.frank.stacklabor.werftstudio.service.network.awaitResponse
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class GroqTranscriber(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build(),
) {
    private val analyzer = SpeechAnalyzer()
    private val filter = WhisperHallucinationFilter()

    /**
     * [modell] ist die gewählte Whisper-Variante, [prompt] der Wörterbuch-Satz aus [vokabularPrompt]
     * (null = ohne persönliches Wörterbuch).
     */
    suspend fun transcribe(apiKey: String, wav: ByteArray, modell: String = MODEL_TURBO, prompt: String? = null): String {
        if (apiKey.isBlank()) throw GroqTranscriptionException("Bitte zuerst einen Groq-API-Schlüssel hinterlegen.")
        if (wav.size > MAX_FILE_BYTES) throw GroqTranscriptionException("Die Aufnahme ist für die Groq-Transkription zu groß.")
        val analysis = analyzer.analyze(wav)
        if (analysis != null && analysis.voicedMs < SpeechAnalyzer.MIN_SPEECH_MS) return ""
        // large-v3 ist nach einer Groq-Sperre 30 Minuten lang durch Turbo ersetzt.
        var aktuell = if (modell == MODEL_LARGE_V3 && System.currentTimeMillis() < turboErzwungenBis) MODEL_TURBO else modell
        var parsed = try {
            sende(apiKey, wav, aktuell, prompt)
        } catch (gesperrt: ModellGesperrtException) {
            // large-v3 ist für die Groq-Organisation nicht freigeschaltet: Diktat nicht verlieren, einmal mit Turbo.
            Log.w(TAG, "model_blocked_fallback_turbo ($aktuell)")
            turboErzwungenBis = System.currentTimeMillis() + SPERRE_MS
            aktuell = MODEL_TURBO
            sende(apiKey, wav, aktuell, prompt)
        }
        // Whisper gibt bei Stille/Gemurmel gern den Prompt selbst aus: dann einmal ohne Wörterbuch wiederholen.
        if (istPromptEcho(parsed.text, prompt)) {
            Log.w(TAG, "prompt_echo_retry_without_prompt (${parsed.text.length} chars)")
            parsed = sende(apiKey, wav, aktuell, null)
        }
        return filter.filter(parsed, analysis)
    }

    private suspend fun sende(apiKey: String, wav: ByteArray, modell: String, prompt: String?): GroqTranscriptionResponse {
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
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .post(builder.build())
            .build()
        try {
            return client.newCall(request).awaitResponse().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    if (modell == MODEL_LARGE_V3 && istModellGesperrt(responseBody)) throw ModellGesperrtException()
                    throw GroqTranscriptionException(httpError(response.code, responseBody))
                }
                runCatching { parseResponse(responseBody) }.getOrElse {
                    throw GroqTranscriptionException("Groq hat eine ungültige Antwort geliefert.", it)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: GroqTranscriptionException) {
            throw error
        } catch (error: ModellGesperrtException) {
            throw error
        } catch (error: IOException) {
            throw GroqTranscriptionException("Groq ist über die aktuelle Verbindung nicht erreichbar.", error)
        }
    }

    private class ModellGesperrtException : Exception("model_permission_blocked_org")

    private fun parseResponse(body: String): GroqTranscriptionResponse {
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

    private fun JSONObject.optDoubleOrNull(name: String): Double? = optDouble(name).takeUnless(Double::isNaN)

    private fun httpError(status: Int, body: String): String {
        val detail = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
        return when (status) {
            401, 403 -> "Der konfigurierte Groq-Schlüssel wurde abgelehnt."
            413 -> "Die Aufnahme ist für die Groq-Transkription zu groß."
            429 -> "Groq hat zu viele Anfragen erhalten. Bitte später erneut versuchen."
            in 500..599 -> "Groq ist derzeit nicht erreichbar. Bitte später erneut versuchen."
            else -> detail?.takeIf(String::isNotBlank)?.let { "Groq-Fehler $status: $it" } ?: "Groq-Fehler $status."
        }
    }

    companion object {
        private const val TAG = "GroqTranscriber"
        private const val TRANSCRIPTIONS_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
        const val MODEL_TURBO = "whisper-large-v3-turbo"
        const val MODEL_LARGE_V3 = "whisper-large-v3"
        private const val MAX_FILE_BYTES = 25 * 1024 * 1024
        private val WAV_MEDIA_TYPE = "audio/wav".toMediaType()

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

data class GroqTranscriptionResponse(val text: String, val segments: List<GroqSegment>? = null)

data class GroqSegment(
    val start: Double?,
    val end: Double?,
    val text: String?,
    val noSpeechProbability: Double?,
    val averageLogProbability: Double?,
    val compressionRatio: Double?,
)

class GroqTranscriptionException(message: String, cause: Throwable? = null) : Exception(message, cause)

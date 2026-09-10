package de.frank.stacklabor.werftstudio.service.speech

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

    suspend fun transcribe(apiKey: String, wav: ByteArray): String {
        if (apiKey.isBlank()) throw GroqTranscriptionException("Bitte zuerst einen Groq-API-Schlüssel hinterlegen.")
        if (wav.size > MAX_FILE_BYTES) throw GroqTranscriptionException("Die Aufnahme ist für die Groq-Transkription zu groß.")
        val analysis = analyzer.analyze(wav)
        if (analysis != null && analysis.voicedMs < SpeechAnalyzer.MIN_SPEECH_MS) return ""
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "audio.wav", wav.toRequestBody(WAV_MEDIA_TYPE))
            .addFormDataPart("model", MODEL)
            .addFormDataPart("language", "de")
            .addFormDataPart("response_format", "verbose_json")
            .addFormDataPart("temperature", "0")
            .build()
        val request = Request.Builder()
            .url(TRANSCRIPTIONS_URL)
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .post(multipart)
            .build()
        try {
            return client.newCall(request).awaitResponse().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw GroqTranscriptionException(httpError(response.code, responseBody))
                val parsed = runCatching { parseResponse(responseBody) }.getOrElse {
                    throw GroqTranscriptionException("Groq hat eine ungültige Antwort geliefert.", it)
                }
                filter.filter(parsed, analysis)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: GroqTranscriptionException) {
            throw error
        } catch (error: IOException) {
            throw GroqTranscriptionException("Groq ist über die aktuelle Verbindung nicht erreichbar.", error)
        }
    }

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

    private companion object {
        const val TRANSCRIPTIONS_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
        const val MODEL = "whisper-large-v3-turbo"
        const val MAX_FILE_BYTES = 25 * 1024 * 1024
        val WAV_MEDIA_TYPE = "audio/wav".toMediaType()
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

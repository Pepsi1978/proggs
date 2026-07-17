package de.frank.karteikartenlernen.transcription

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONObject

class GroqTranscriber(
    private val apiKey: String,
    private val model: String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    suspend fun transcribe(file: File): String {
        if (!isConfigured) throw GroqTranscriptionException("Groq ist auf diesem Gerät nicht konfiguriert.")
        if (file.length() > MAX_FILE_BYTES) throw GroqTranscriptionException("Die Aufnahme ist für die Groq-Transkription zu groß.")
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("audio/wav".toMediaType()))
            .addFormDataPart("model", model)
            .addFormDataPart("language", "de")
            .addFormDataPart("response_format", "verbose_json")
            .addFormDataPart("temperature", "0")
            .build()
        val request = Request.Builder()
            .url(TRANSCRIPTIONS_URL)
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
        val response = client.newCall(request).await()
        response.use {
            val responseBody = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw GroqTranscriptionException(httpError(it.code, responseBody))
            val text = runCatching { JSONObject(responseBody).optString("text").trim() }
                .getOrElse { throw GroqTranscriptionException("Groq hat eine ungültige Antwort geliefert.") }
            if (text.isEmpty()) throw GroqTranscriptionException("In der Aufnahme wurde kein Text erkannt.")
            return text
        }
    }

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
    }
}

class GroqTranscriptionException(message: String, cause: Throwable? = null) : Exception(message, cause)

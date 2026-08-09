package de.frank.experimente.tts

import android.util.Base64
import de.frank.experimente.network.shutdownSafely
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class QwenEnrollmentException(message: String) : Exception(message)

/**
 * Registers a recording as a cloned voice and removes voices again.
 *
 * The recording travels inline as a base64 data URI, so nothing has to be uploaded to a public
 * address first — the phone talks to Model Studio directly.
 */
class QwenVoiceEnrollment {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    /** Registers [wav] and returns the new voice id. */
    suspend fun create(apiKey: String, name: String, wav: ByteArray): String = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.filterNot(Char::isWhitespace)
        if (cleanKey.isBlank()) throw QwenEnrollmentException("Es fehlt der Alibaba-Schlüssel.")
        if (wav.isEmpty()) throw QwenEnrollmentException("Die Aufnahme ist leer.")

        val encoded = Base64.encodeToString(wav, Base64.NO_WRAP)
        val body = JSONObject()
            .put("model", ENROLLMENT_MODEL)
            .put(
                "input",
                JSONObject()
                    .put("action", "create")
                    .put("target_model", TARGET_MODEL)
                    .put("preferred_name", preferredName(name))
                    .put("audio", JSONObject().put("data", "data:audio/wav;base64,$encoded")),
            )
        val answer = call(body, cleanKey)
        answer.optJSONObject("output")?.optString("voice").orEmpty().ifBlank {
            throw QwenEnrollmentException("Alibaba hat keine Stimm-Kennung zurückgegeben.")
        }
    }

    suspend fun delete(apiKey: String, voiceId: String): Unit = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.filterNot(Char::isWhitespace)
        if (cleanKey.isBlank()) throw QwenEnrollmentException("Es fehlt der Alibaba-Schlüssel.")
        val body = JSONObject()
            .put("model", ENROLLMENT_MODEL)
            .put(
                "input",
                JSONObject()
                    .put("action", "delete")
                    .put("voice", voiceId.filterNot(Char::isWhitespace)),
            )
        call(body, cleanKey)
    }

    fun shutdown() = client.shutdownSafely(logger)

    private fun call(body: JSONObject, apiKey: String): JSONObject {
        val request = Request.Builder()
            .url(CUSTOMIZATION_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val text = try {
            client.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    android.util.Log.e(TAG, "enrollment ${response.code}: ${payload.take(300)}")
                    throw QwenEnrollmentException(readableError(payload, response.code))
                }
                payload
            }
        } catch (error: QwenEnrollmentException) {
            throw error
        } catch (error: Exception) {
            android.util.Log.e(TAG, "enrollment failed: ${error.message}", error)
            throw QwenEnrollmentException("Die Verbindung zu Alibaba ist fehlgeschlagen.")
        }
        return try {
            JSONObject(text)
        } catch (error: Exception) {
            throw QwenEnrollmentException("Alibaba hat eine unlesbare Antwort geliefert.")
        }
    }

    private companion object {
        const val CUSTOMIZATION_URL =
            "https://dashscope-intl.aliyuncs.com/api/v1/services/audio/tts/customization"
        const val ENROLLMENT_MODEL = "qwen-voice-enrollment"

        /** Cloning and speaking must name the same model, otherwise synthesis fails later. */
        const val TARGET_MODEL = "qwen3-tts-vc-2026-01-22"
        const val TAG = "QwenTts"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val logger: Logger = Logger.getLogger(QwenVoiceEnrollment::class.java.name)

        /** Alibaba bakes the name into the voice id and accepts letters and digits only. */
        fun preferredName(raw: String): String =
            raw.filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }
                .take(16)
                .ifBlank { "Stimme" }

        fun readableError(payload: String, code: Int): String {
            val message = runCatching { JSONObject(payload).optString("message") }.getOrNull()
            return when {
                !message.isNullOrBlank() -> message
                code == 401 || code == 403 -> "Der Alibaba-Schlüssel wird abgewiesen."
                else -> "Alibaba hat mit Fehler $code geantwortet."
            }
        }
    }
}

package de.frank.cortex.tts

import android.util.Base64
import de.frank.cortex.observability.CortexLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class QwenEnrollmentException(message: String) : Exception(message)

/**
 * Meldet eine Aufnahme als geklonte Stimme an und entfernt Stimmen wieder.
 *
 * Die Aufnahme reist als base64-Daten-URI mit, es muss also nichts vorher an eine oeffentliche
 * Adresse hochgeladen werden — das Telefon spricht direkt mit Model Studio.
 */
class QwenVoiceEnrollment {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    /** Meldet [wav] an und gibt die neue Stimm-Kennung zurueck. */
    suspend fun create(apiKey: String, name: String, wav: ByteArray): String = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.filterNot(Char::isWhitespace)
        if (cleanKey.isBlank()) throw QwenEnrollmentException("Es fehlt der Alibaba-Schlüssel.")
        if (wav.isEmpty()) throw QwenEnrollmentException("Die Aufnahme ist leer.")

        val encoded = Base64.encodeToString(wav, Base64.NO_WRAP)
        val body = JSONObject()
            .put("model", QwenVoiceDirectory.ENROLLMENT_MODEL)
            .put(
                "input",
                JSONObject()
                    .put("action", "create")
                    .put("target_model", QwenTtsSynthesizer.MODEL)
                    .put("preferred_name", preferredName(name))
                    .put("audio", JSONObject().put("data", "data:audio/wav;base64,$encoded"))
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
            .put("model", QwenVoiceDirectory.ENROLLMENT_MODEL)
            .put(
                "input",
                JSONObject()
                    .put("action", "delete")
                    .put("voice", voiceId.filterNot(Char::isWhitespace))
            )
        call(body, cleanKey)
    }

    private fun call(body: JSONObject, apiKey: String): JSONObject {
        val request = Request.Builder()
            .url(QwenVoiceDirectory.CUSTOMIZATION_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody(QwenVoiceDirectory.JSON_MEDIA_TYPE))
            .build()
        val text = try {
            client.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    CortexLog.error("QwenTts", "enrollment", "Antwort ${response.code}: ${payload.take(300)}")
                    throw QwenEnrollmentException(readableError(payload, response.code))
                }
                payload
            }
        } catch (e: QwenEnrollmentException) {
            throw e
        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
            CortexLog.error("QwenTts", "enrollment", "Verbindung fehlgeschlagen: ${e.message}")
            throw QwenEnrollmentException("Die Verbindung zu Alibaba ist fehlgeschlagen.")
        }
        return try {
            JSONObject(text)
        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
            throw QwenEnrollmentException("Alibaba hat eine unlesbare Antwort geliefert.")
        }
    }

    companion object {
        /** Alibaba baeckt den Namen in die Stimm-Kennung ein und nimmt nur Buchstaben und Ziffern. */
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

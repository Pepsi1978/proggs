package de.frank.cortex.tts

import de.frank.cortex.observability.CortexLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Eine Stimme, die aus einer Aufnahme geklont wurde und im Model-Studio-Konto liegt. */
data class ClonedVoice(
    val id: String,
    val name: String,
    val createdAt: String
)

/**
 * Listet die geklonten Stimmen des Kontos auf, damit sie in der App waehlbar sind.
 *
 * Ohne diese Liste muesste die 46 Zeichen lange Stimm-Kennung am Telefon abgetippt werden.
 */
class QwenVoiceDirectory {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun list(apiKey: String): List<ClonedVoice> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.filterNot(Char::isWhitespace)
        if (cleanKey.isBlank()) return@withContext emptyList()

        val body = JSONObject()
            .put("model", ENROLLMENT_MODEL)
            .put("input", JSONObject().put("action", "list"))
        val request = Request.Builder()
            .url(CUSTOMIZATION_URL)
            .addHeader("Authorization", "Bearer $cleanKey")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    CortexLog.error("QwenTts", "list", "Stimmen-Liste fehlgeschlagen (${response.code}): ${text.take(300)}")
                    return@withContext emptyList()
                }
                val list = JSONObject(text)
                    .optJSONObject("output")
                    ?.optJSONArray("voice_list")
                    ?: return@withContext emptyList()
                (0 until list.length()).mapNotNull { index ->
                    val entry = list.optJSONObject(index) ?: return@mapNotNull null
                    val id = entry.optString("voice")
                    if (id.isBlank()) return@mapNotNull null
                    ClonedVoice(
                        id = id,
                        name = displayName(id),
                        createdAt = germanDate(entry.optString("gmt_create"))
                    )
                }
            }
        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
            CortexLog.error("QwenTts", "list", "Stimmen-Liste fehlgeschlagen: ${e.message}")
            emptyList()
        }
    }

    companion object {
        const val CUSTOMIZATION_URL =
            "https://dashscope-intl.aliyuncs.com/api/v1/services/audio/tts/customization"
        const val ENROLLMENT_MODEL = "qwen-voice-enrollment"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Holt den gewaehlten Namen aus Kennungen wie `qwen-tts-vc-FrankHD-voice-2026…-4d0a`. */
        fun displayName(id: String): String {
            val name = Regex("^qwen-tts-vc-(.+?)-voice-").find(id)?.groupValues?.get(1) ?: return id
            // Alibaba erlaubt keine Leerzeichen im Stimm-Namen — ein Grossbuchstabe nach einem
            // Kleinbuchstaben ist also die Wortgrenze: FrankHD wird zu Frank HD.
            return name.replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ")
        }

        /** Macht aus `2026-08-03 23:21:42` ein `03.08.2026, 23:21`. */
        fun germanDate(raw: String): String {
            val match = Regex("""(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})""").find(raw) ?: return raw
            val (year, month, day, hour, minute) = match.destructured
            return "$day.$month.$year, $hour:$minute"
        }
    }
}

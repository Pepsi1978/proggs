package de.frank.stacklabor.werftstudio.service.tts

import de.frank.stacklabor.werftstudio.service.network.awaitResponse
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** A voice that was cloned from a recording and lives in the Alibaba Model Studio account. */
data class ClonedVoice(
    val id: String,
    val name: String,
    val createdAt: String,
)

/**
 * Lists the cloned voices of the account so they can be picked in the settings.
 *
 * Without this the 46 character voice id would have to be typed by hand on the phone.
 * Taken over from PerfectMoment, where the same account is used.
 */
class QwenVoiceDirectory(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun list(apiKey: String): List<ClonedVoice> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.filterNot(Char::isWhitespace)
        if (cleanKey.isBlank()) throw TtsException("Es fehlt der Alibaba-Schlüssel.")

        val body = JSONObject()
            .put("model", "qwen-voice-enrollment")
            .put("input", JSONObject().put("action", "list"))
        val request = Request.Builder()
            .url(LIST_URL)
            .addHeader("Authorization", "Bearer $cleanKey")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).awaitResponse().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw TtsException(
                    when (response.code) {
                        401, 403 -> "Der Alibaba-Schlüssel wird abgewiesen."
                        else -> "Die Stimmenliste meldet HTTP ${response.code}."
                    },
                )
            }
            val list = JSONObject(text).optJSONObject("output")?.optJSONArray("voice_list")
                ?: return@use emptyList()
            (0 until list.length()).mapNotNull { index ->
                val entry = list.optJSONObject(index) ?: return@mapNotNull null
                val id = entry.optString("voice")
                if (id.isBlank()) return@mapNotNull null
                ClonedVoice(
                    id = id,
                    name = displayName(id),
                    createdAt = germanDate(entry.optString("gmt_create")),
                )
            }
        }
    }

    private companion object {
        const val LIST_URL = "https://dashscope-intl.aliyuncs.com/api/v1/services/audio/tts/customization"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Pulls the chosen name out of ids like `qwen-tts-vc-FrankHD-voice-2026…-4d0a`. */
        fun displayName(id: String): String {
            val name = Regex("^qwen-tts-vc-(.+?)-voice-").find(id)?.groupValues?.get(1) ?: return id
            // Alibaba allows no spaces in a voice name, so a capital after a small letter is
            // where the word break belongs: FrankHD becomes Frank HD.
            return name.replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ")
        }

        /** Turns `2026-08-03 23:21:42` into `03.08.2026, 23:21`. */
        fun germanDate(raw: String): String {
            val match = Regex("""(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})""").find(raw) ?: return raw
            val (year, month, day, hour, minute) = match.destructured
            return "$day.$month.$year, $hour:$minute"
        }
    }
}

package de.frank.denknotiz.tts

import android.util.Base64
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class QwenVoice(val id: String, val name: String, val createdAt: String)

class QwenVoiceManager {
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS).build()

    suspend fun list(rawKey: String): List<QwenVoice> = withContext(Dispatchers.IO) {
        val key = rawKey.filterNot(Char::isWhitespace)
        require(key.isNotBlank()) { "Qwen-Schlüssel fehlt." }
        val result = call(JSONObject().put("model", ENROLLMENT_MODEL).put("input", JSONObject().put("action", "list")), key)
        val voices = result.optJSONObject("output")?.optJSONArray("voice_list") ?: return@withContext emptyList()
        (0 until voices.length()).mapNotNull { index ->
            val item = voices.optJSONObject(index) ?: return@mapNotNull null
            item.optString("voice").takeIf(String::isNotBlank)?.let { id ->
                QwenVoice(id, displayName(id), germanDate(item.optString("gmt_create")))
            }
        }
    }

    suspend fun enroll(rawKey: String, name: String, wav: ByteArray): String = withContext(Dispatchers.IO) {
        val key = rawKey.filterNot(Char::isWhitespace)
        require(key.isNotBlank()) { "Qwen-Schlüssel fehlt." }
        require(wav.isNotEmpty()) { "Die Stimmaufnahme ist leer." }
        val cleanName = name.filter(Char::isLetterOrDigit).take(16).ifBlank { "Stimme" }
        val body = JSONObject().put("model", ENROLLMENT_MODEL).put("input", JSONObject().put("action", "create")
            .put("target_model", QWEN_MODEL).put("preferred_name", cleanName)
            .put("audio", JSONObject().put("data", "data:audio/wav;base64,${Base64.encodeToString(wav, Base64.NO_WRAP)}")))
        call(body, key).optJSONObject("output")?.optString("voice").orEmpty()
            .ifBlank { throw IOException("Alibaba hat keine Stimm-Kennung geliefert.") }
    }

    suspend fun delete(rawKey: String, voiceId: String) = withContext(Dispatchers.IO) {
        val key = rawKey.filterNot(Char::isWhitespace)
        call(JSONObject().put("model", ENROLLMENT_MODEL).put("input", JSONObject().put("action", "delete").put("voice", voiceId)), key)
        Unit
    }

    private fun call(body: JSONObject, key: String): JSONObject {
        val request = Request.Builder().url(URL).header("Authorization", "Bearer $key")
            .post(body.toString().toRequestBody(JSON)).build()
        return client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Alibaba ${response.code}: ${JSONObject(raw).optString("message").ifBlank { raw.take(240) }}")
            JSONObject(raw)
        }
    }

    companion object {
        private const val URL = "https://dashscope-intl.aliyuncs.com/api/v1/services/audio/tts/customization"
        private const val ENROLLMENT_MODEL = "qwen-voice-enrollment"
        fun displayName(id: String): String = Regex("^qwen-tts-vc-(.+?)-voice-").find(id)?.groupValues?.get(1)
            ?.replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ") ?: id
        fun germanDate(raw: String): String = Regex("""(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})""").find(raw)?.destructured
            ?.let { (year, month, day, hour, minute) -> "$day.$month.$year, $hour:$minute" } ?: raw
    }
}

private val JSON = "application/json; charset=utf-8".toMediaType()

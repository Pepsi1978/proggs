package de.frank.perfectmoment.backup

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Talks to Google Drive's appDataFolder — a hidden per-app folder in the user's own Drive. Nothing
 * here can see or touch the rest of their files.
 *
 * Deliberately plain HTTP over the OkHttp client the app already carries, instead of pulling in the
 * whole Drive SDK for three calls.
 */
class DriveClient(private val http: OkHttpClient) {

    class DriveException(message: String, val code: Int = 0) : IOException(message)

    data class RemoteFile(val id: String, val modifiedTime: String?)

    /**
     * Finds the existing backup. `spaces=appDataFolder` is required — without it Drive searches the
     * visible drive and returns nothing. Fields must be listed explicitly or they come back null.
     */
    suspend fun findBackup(token: String): RemoteFile? = withContext(Dispatchers.IO) {
        val url = "https://www.googleapis.com/drive/v3/files" +
            "?spaces=appDataFolder" +
            "&q=" + encode("name = '${BackupPayload.FILE_NAME}' and trashed = false") +
            "&fields=" + encode("files(id,modifiedTime)") +
            "&pageSize=10"
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token").get().build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw DriveException(describe(response.code, body), response.code)
            val files = JSONObject(body).optJSONArray("files")
            if (files == null || files.length() == 0) {
                null
            } else {
                val first = files.getJSONObject(0)
                RemoteFile(first.getString("id"), first.optString("modifiedTime", null))
            }
        }
    }

    suspend fun download(token: String, fileId: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw DriveException(describe(response.code, body), response.code)
            body
        }
    }

    /**
     * Writes the backup. Updating a known file id rather than always creating avoids the duplicate
     * files two devices would otherwise pile up.
     */
    suspend fun upload(token: String, existingId: String?, json: String): String =
        withContext(Dispatchers.IO) {
            val content = json.toRequestBody(JSON)
            val request = if (existingId == null) {
                val metadata = JSONObject().apply {
                    put("name", BackupPayload.FILE_NAME)
                    put("parents", org.json.JSONArray().put("appDataFolder"))
                }.toString()
                val multipart = MultipartBody.Builder().setType("multipart/related".toMediaType())
                    .addPart(metadata.toRequestBody(JSON))
                    .addPart(content)
                    .build()
                Request.Builder()
                    .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id")
                    .header("Authorization", "Bearer $token")
                    .post(multipart)
                    .build()
            } else {
                Request.Builder()
                    .url("https://www.googleapis.com/upload/drive/v3/files/$existingId?uploadType=media&fields=id")
                    .header("Authorization", "Bearer $token")
                    .patch(content)
                    .build()
            }
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw DriveException(describe(response.code, body), response.code)
                JSONObject(body).optString("id", existingId.orEmpty())
            }
        }

    /** Turns Drive's error bodies into something a person can act on. */
    private fun describe(code: Int, body: String): String {
        val reason = runCatching {
            JSONObject(body).getJSONObject("error").optString("message")
        }.getOrNull().orEmpty()
        return when {
            code == 401 -> "Zugang abgelaufen — bitte erneut anmelden."
            code == 403 && reason.contains("storageQuota", ignoreCase = true) ->
                "Dein Google-Speicher ist voll. Die Sicherung zählt gegen die 15 GB deines Kontos."
            code == 403 -> "Kein Zugriff auf Google Drive: $reason"
            else -> "Google Drive antwortet mit $code: $reason"
        }
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

package de.frank.karteikartenlernen.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit

data class AuthResult(val email: String?)
data class GeneratedCard(val question: String, val answer: String, val explanation: String)
data class GeneratedResearch(val title: String, val answer: String, val cards: List<GeneratedCard>)

enum class AuthErrorKind { REAUTH, QUOTA, NETWORK }
class CodexAuthException(val kind: AuthErrorKind, message: String) : Exception(message)

class CodexAuthManager(context: Context) {
    private val appContext = context.applicationContext
    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val store = EncryptedSharedPreferences.create(
        appContext,
        "codex_oauth",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    @Volatile private var activeServer: ServerSocket? = null

    val email: String? get() = store.getString(KEY_EMAIL, null)
    val isConnected: Boolean get() = store.contains(KEY_ACCESS_TOKEN)

    suspend fun login(activity: Activity): AuthResult = withContext(Dispatchers.IO) {
        val verifier = randomUrlSafe(64)
        val challenge = urlSafe(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
        val state = randomUrlSafe(32)
        val callbackServer = ServerSocket().apply { bind(InetSocketAddress(InetAddress.getLoopbackAddress(), CALLBACK_PORT)) }
        activeServer = callbackServer
        try {
            callbackServer.use { server ->
            activeServer = server
            server.soTimeout = TimeUnit.MINUTES.toMillis(5).toInt()
            val authUri = Uri.parse(AUTH_URL).buildUpon()
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("scope", "openid profile email offline_access")
                .appendQueryParameter("code_challenge", challenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("state", state)
                .appendQueryParameter("codex_cli_simplified_flow", "true")
                .build()
            withContext(Dispatchers.Main) {
                activity.startActivity(Intent(Intent.ACTION_VIEW, authUri))
            }
            val socket = server.accept()
            val requestLine = BufferedReader(InputStreamReader(socket.getInputStream())).readLine().orEmpty()
            val path = requestLine.split(' ').getOrNull(1).orEmpty()
            if (!path.startsWith("/auth/callback?")) throw CodexAuthException(AuthErrorKind.REAUTH, "Ungültiger OAuth-Callback-Pfad.")
            val query = Uri.parse("http://localhost$path")
            val code = query.getQueryParameter("code")
            val returnedState = query.getQueryParameter("state")
            val error = query.getQueryParameter("error_description") ?: query.getQueryParameter("error")
            val success = code != null && returnedState == state
            val body = if (success) {
                "<html><body style='font-family:sans-serif;background:#0c0e14;color:#f4f5fb;padding:40px'><h2>Anmeldung erfolgreich</h2><p>Du kannst zu Karteikarten Lernen zurückkehren.</p></body></html>"
            } else {
                "<html><body style='font-family:sans-serif;padding:40px'><h2>Anmeldung fehlgeschlagen</h2><p>${htmlEscape(error ?: "Ungültige OAuth-Antwort")}</p></body></html>"
            }
            val response = "HTTP/1.1 ${if (success) "200 OK" else "400 Bad Request"}\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.toByteArray().size}\r\nConnection: close\r\n\r\n$body"
            socket.getOutputStream().use { it.write(response.toByteArray()) }
            socket.close()
            if (error != null) throw CodexAuthException(AuthErrorKind.REAUTH, error)
            if (returnedState != state) throw CodexAuthException(AuthErrorKind.REAUTH, "OAuth-State stimmt nicht überein.")
            if (code == null) throw CodexAuthException(AuthErrorKind.REAUTH, "OpenAI hat keinen Anmeldecode geliefert.")
                exchangeCode(code, verifier)
            }
        } finally {
            activeServer = null
        }
    }

    fun cancelLogin() {
        activeServer?.runCatching { close() }
        activeServer = null
    }

    suspend fun generateResearch(model: String, reasoning: String, question: String, cardLimit: Int): GeneratedResearch =
        withContext(Dispatchers.IO) {
            val token = validAccessToken()
            val accountId = jwtClaim(token, "chatgpt_account_id")
                ?: store.getString(KEY_ACCOUNT_ID, null)
                ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Im Codex-Token fehlt die ChatGPT-Account-ID.")
            val requestedCards = if (cardLimit == 0) 12 else cardLimit
            val payload = JSONObject().apply {
                put("model", model)
                put("stream", false)
                put("instructions", "Antworte auf Deutsch, fachlich korrekt und gut verständlich. Erzeuge einen kurzen Sessiontitel, eine Lernantwort und genau $requestedCards eigenständige Karteikarten. Keine Markdown-Syntax.")
                put("input", question)
                put("reasoning", JSONObject().put("effort", reasoning.lowercase().replace("mittel", "medium").replace("niedrig", "low").replace("hoch", "high")))
                put("text", structuredOutputFormat())
            }
            val connection = (URL(RESPONSES_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 120_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("originator", "codex_cli_rs")
                setRequestProperty("User-Agent", "codex_cli_rs/0.0.0 (Karteikarten Lernen)")
                setRequestProperty("ChatGPT-Account-ID", accountId)
            }
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            val responseText = connection.readBody()
            classifyHttpError(connection.responseCode, responseText)
            val output = extractOutputText(JSONObject(responseText))
                ?: throw CodexAuthException(AuthErrorKind.NETWORK, "Das gewählte GPT-Modell hat keine strukturierten Lerndaten geliefert.")
            val result = JSONObject(output)
            val cards = result.getJSONArray("cards")
            GeneratedResearch(
                title = result.getString("title"),
                answer = result.getString("answer"),
                cards = (0 until cards.length()).map { index ->
                    val card = cards.getJSONObject(index)
                    GeneratedCard(card.getString("question"), card.getString("answer"), card.getString("explanation"))
                },
            )
        }

    fun logout() {
        store.edit().clear().apply()
    }

    private fun exchangeCode(code: String, verifier: String): AuthResult {
        val form = formBody(
            "grant_type" to "authorization_code",
            "client_id" to CLIENT_ID,
            "code" to code,
            "redirect_uri" to REDIRECT_URI,
            "code_verifier" to verifier,
        )
        val connection = postForm(TOKEN_URL, form)
        val response = connection.readBody()
        classifyHttpError(connection.responseCode, response)
        val json = JSONObject(response)
        val accessToken = json.getString("access_token")
        val refreshToken = json.optString("refresh_token").takeIf(String::isNotBlank)
        val idToken = json.optString("id_token").takeIf(String::isNotBlank)
        val foundEmail = idToken?.let { jwtValue(it, "email") } ?: jwtValue(accessToken, "email")
        val accountId = jwtClaim(accessToken, "chatgpt_account_id")
        store.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply {
                if (refreshToken != null) putString(KEY_REFRESH_TOKEN, refreshToken)
                if (foundEmail != null) putString(KEY_EMAIL, foundEmail)
                if (accountId != null) putString(KEY_ACCOUNT_ID, accountId)
            }
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + json.optLong("expires_in", 3600) * 1000)
            .apply()
        return AuthResult(foundEmail)
    }

    private fun validAccessToken(): String {
        val token = store.getString(KEY_ACCESS_TOKEN, null)
            ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Bitte zuerst bei OpenAI anmelden.")
        if (System.currentTimeMillis() < store.getLong(KEY_EXPIRES_AT, 0) - REFRESH_SKEW_MS) return token
        val refreshToken = store.getString(KEY_REFRESH_TOKEN, null)
            ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Die Anmeldung ist abgelaufen. Bitte erneut anmelden.")
        val connection = postForm(
            TOKEN_URL,
            formBody(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "client_id" to CLIENT_ID,
            ),
        )
        val response = connection.readBody()
        classifyHttpError(connection.responseCode, response)
        val json = JSONObject(response)
        val newAccessToken = json.getString("access_token")
        val edit = store.edit()
            .putString(KEY_ACCESS_TOKEN, newAccessToken)
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + json.optLong("expires_in", 3600) * 1000)
        json.optString("refresh_token").takeIf(String::isNotBlank)?.let { edit.putString(KEY_REFRESH_TOKEN, it) }
        jwtClaim(newAccessToken, "chatgpt_account_id")?.let { edit.putString(KEY_ACCOUNT_ID, it) }
        edit.apply()
        return newAccessToken
    }

    private fun postForm(url: String, form: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20_000
        readTimeout = 20_000
        doOutput = true
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        outputStream.use { it.write(form.toByteArray()) }
    }

    private fun HttpURLConnection.readBody(): String {
        val stream = if (responseCode in 200..299) inputStream else errorStream
        return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    }

    private fun classifyHttpError(code: Int, body: String) {
        if (code in 200..299) return
        val message = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
            ?: body.take(240)
        when {
            code == 429 -> throw CodexAuthException(AuthErrorKind.QUOTA, "Dein ChatGPT-/Codex-Kontingent ist aktuell ausgeschöpft. Bitte später erneut versuchen.")
            body.contains("refresh_token_reused", true) -> throw CodexAuthException(AuthErrorKind.REAUTH, "Der Refresh-Token wurde bereits verwendet. Bitte erneut im Browser anmelden.")
            code == 401 || code == 403 || body.contains("invalid_grant", true) -> throw CodexAuthException(AuthErrorKind.REAUTH, "OpenAI-Anmeldung ungültig oder nicht mehr zugelassen. Bitte erneut anmelden. $message")
            else -> throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI-Fehler $code: $message")
        }
    }

    private fun structuredOutputFormat(): JSONObject {
        val card = JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put("properties", JSONObject()
                .put("question", JSONObject().put("type", "string"))
                .put("answer", JSONObject().put("type", "string"))
                .put("explanation", JSONObject().put("type", "string")))
            .put("required", JSONArray(listOf("question", "answer", "explanation")))
        val schema = JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put("properties", JSONObject()
                .put("title", JSONObject().put("type", "string"))
                .put("answer", JSONObject().put("type", "string"))
                .put("cards", JSONObject().put("type", "array").put("items", card)))
            .put("required", JSONArray(listOf("title", "answer", "cards")))
        return JSONObject().put("format", JSONObject()
            .put("type", "json_schema")
            .put("name", "karteikarten_research")
            .put("strict", true)
            .put("schema", schema))
    }

    private fun extractOutputText(json: JSONObject): String? {
        json.optString("output_text").takeIf(String::isNotBlank)?.let { return it }
        val output = json.optJSONArray("output") ?: return null
        for (i in 0 until output.length()) {
            val content = output.optJSONObject(i)?.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                content.optJSONObject(j)?.optString("text")?.takeIf(String::isNotBlank)?.let { return it }
            }
        }
        return null
    }

    private fun jwtClaim(token: String, name: String): String? {
        val payload = jwtPayload(token) ?: return null
        return payload.optJSONObject("https://api.openai.com/auth")?.optString(name)?.takeIf(String::isNotBlank)
    }

    private fun jwtValue(token: String, name: String): String? =
        jwtPayload(token)?.optString(name)?.takeIf(String::isNotBlank)

    private fun jwtPayload(token: String): JSONObject? = runCatching {
        val part = token.split('.')[1]
        JSONObject(String(Base64.getUrlDecoder().decode(part.padEnd((part.length + 3) / 4 * 4, '='))))
    }.getOrNull()

    private fun randomUrlSafe(bytes: Int): String = ByteArray(bytes).also(SecureRandom()::nextBytes).let(::urlSafe)
    private fun urlSafe(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    private fun formBody(vararg values: Pair<String, String>): String = values.joinToString("&") {
        "${URLEncoder.encode(it.first, "UTF-8")}=${URLEncoder.encode(it.second, "UTF-8")}"
    }
    private fun htmlEscape(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    companion object {
        private const val CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
        private const val AUTH_URL = "https://auth.openai.com/oauth/authorize"
        private const val TOKEN_URL = "https://auth.openai.com/oauth/token"
        private const val RESPONSES_URL = "https://chatgpt.com/backend-api/codex/responses"
        private const val CALLBACK_PORT = 1455
        private const val REDIRECT_URI = "http://localhost:1455/auth/callback"
        private const val REFRESH_SKEW_MS = 120_000L
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_ACCOUNT_ID = "account_id"
        private const val KEY_EMAIL = "email"
    }
}

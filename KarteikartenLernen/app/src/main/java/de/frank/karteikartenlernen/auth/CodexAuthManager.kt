package de.frank.karteikartenlernen.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.UnknownHostException
import java.net.URL
import java.net.URLEncoder
import java.util.Base64

data class AuthResult(val email: String?)
data class DeviceAuthInfo(val userCode: String, val verificationUri: String)
data class GeneratedCard(val question: String, val answer: String, val explanation: String)
data class GeneratedResearch(val title: String, val answer: String, val cards: List<GeneratedCard>)

enum class AuthErrorKind { REAUTH, QUOTA, NETWORK }
class CodexAuthException(val kind: AuthErrorKind, message: String) : Exception(message)
internal enum class DevicePollAction { PROCESS, PENDING, FAIL }

internal fun devicePollAction(statusCode: Int): DevicePollAction = when {
    statusCode == 200 -> DevicePollAction.PROCESS
    statusCode == 403 || statusCode == 404 || statusCode == 429 || statusCode >= 500 -> DevicePollAction.PENDING
    else -> DevicePollAction.FAIL
}

internal fun devicePollInterval(value: Any?): Int =
    (value?.toString()?.toIntOrNull() ?: CodexAuthManager.DEFAULT_DEVICE_POLL_SECONDS)
        .coerceAtLeast(CodexAuthManager.MIN_DEVICE_POLL_SECONDS)

internal fun codexInput(question: String): JSONArray = JSONArray().put(
    JSONObject()
        .put("role", "user")
        .put("content", question),
)

internal fun parseCodexSse(body: String): String {
    val deltas = StringBuilder()
    var completedText: String? = null
    body.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (!line.startsWith("data:")) return@forEach
        val data = line.removePrefix("data:").trim()
        if (data.isEmpty() || data == "[DONE]") return@forEach
        val event = runCatching { JSONObject(data) }.getOrElse { return@forEach }
        when (event.optString("type")) {
            "response.output_text.delta" -> deltas.append(event.optString("delta"))
            "response.completed" -> completedText = event.optJSONObject("response")?.let(::extractOutputText)
            "response.failed" -> {
                val message = event.optJSONObject("response")?.optJSONObject("error")?.optString("message")
                    ?.takeIf(String::isNotBlank) ?: "OpenAI hat die Antwort abgebrochen."
                throw CodexAuthException(AuthErrorKind.NETWORK, message)
            }
            "error" -> {
                val message = event.optString("message").takeIf(String::isNotBlank)
                    ?: event.optJSONObject("error")?.optString("message")?.takeIf(String::isNotBlank)
                    ?: "OpenAI hat einen unbekannten Streaming-Fehler gemeldet."
                throw CodexAuthException(AuthErrorKind.NETWORK, message)
            }
        }
    }
    return deltas.toString().takeIf(String::isNotBlank)
        ?: completedText?.takeIf(String::isNotBlank)
        ?: throw CodexAuthException(AuthErrorKind.NETWORK, "OpenAI hat keinen Antworttext geliefert.")
}

internal fun extractOutputText(json: JSONObject): String? {
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

class CodexAuthManager(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
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
    val email: String? get() = store.getString(KEY_EMAIL, null)
    val isConnected: Boolean get() = store.contains(KEY_ACCESS_TOKEN)

    suspend fun login(activity: ComponentActivity, onDeviceCode: (DeviceAuthInfo) -> Unit): AuthResult = withContext(Dispatchers.IO) {
        val startConnection = postJsonWithDnsRetry(
            DEVICE_USER_CODE_URL,
            JSONObject().put("client_id", CLIENT_ID).toString(),
        )
        val startBody = startConnection.readBody()
        classifyHttpError(startConnection.responseCode, startBody)
        val start = JSONObject(startBody)
        val userCode = start.optString("user_code").trim()
        val deviceAuthId = start.optString("device_auth_id").trim()
        if (userCode.isEmpty() || deviceAuthId.isEmpty()) {
            throw CodexAuthException(AuthErrorKind.REAUTH, "OpenAI hat keinen vollständigen Gerätecode geliefert.")
        }
        var intervalSeconds = devicePollInterval(start.opt("interval"))
        val info = DeviceAuthInfo(userCode, DEVICE_VERIFICATION_URL)
        withContext(Dispatchers.Main) {
            onDeviceCode(info)
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.verificationUri)))
        }
        val expiresAt = System.currentTimeMillis() + DEVICE_CODE_LIFETIME_MS
        while (System.currentTimeMillis() < expiresAt) {
            delay(intervalSeconds * 1_000L)
            val pollConnection = try {
                postJsonWithDnsRetry(
                    DEVICE_TOKEN_URL,
                    JSONObject().put("device_auth_id", deviceAuthId).put("user_code", userCode).toString(),
                )
            } catch (_: CodexAuthException) {
                intervalSeconds = (intervalSeconds + NETWORK_POLL_BACKOFF_SECONDS).coerceAtMost(MAX_DEVICE_POLL_SECONDS)
                continue
            }
            val pollBody = pollConnection.readBody()
            when (devicePollAction(pollConnection.responseCode)) {
                DevicePollAction.PENDING -> {
                    if (pollConnection.responseCode == 429 || pollBody.contains("slow_down", true)) {
                        intervalSeconds = (intervalSeconds + SLOW_DOWN_SECONDS).coerceAtMost(MAX_DEVICE_POLL_SECONDS)
                    }
                }
                DevicePollAction.FAIL -> classifyHttpError(pollConnection.responseCode, pollBody)
                DevicePollAction.PROCESS -> {
                    val authorization = JSONObject(pollBody)
                    val code = authorization.optString("authorization_code").trim()
                    val verifier = authorization.optString("code_verifier").trim()
                    if (code.isEmpty() || verifier.isEmpty()) {
                        throw CodexAuthException(AuthErrorKind.REAUTH, "OpenAI hat den Gerätecode bestätigt, aber keinen vollständigen Anmeldecode geliefert.")
                    }
                    awaitForegroundAndNetwork(activity)
                    return@withContext exchangeCode(code, verifier, DEVICE_REDIRECT_URI)
                }
            }
        }
        throw CodexAuthException(AuthErrorKind.REAUTH, "Der OpenAI-Gerätecode ist abgelaufen. Bitte die Anmeldung neu starten.")
    }

    fun cancelLogin() = Unit

    suspend fun generateResearch(model: String, reasoning: String, question: String, cardLimit: Int): GeneratedResearch =
        withContext(Dispatchers.IO) {
            val token = validAccessToken()
            val accountId = jwtClaim(token, "chatgpt_account_id")
                ?: store.getString(KEY_ACCOUNT_ID, null)
                ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Im Codex-Token fehlt die ChatGPT-Account-ID.")
            val requestedCards = if (cardLimit == 0) 12 else cardLimit
            val payload = JSONObject().apply {
                put("model", model)
                put("stream", true)
                put("store", false)
                put("instructions", "Antworte auf Deutsch, fachlich korrekt und gut verständlich. Erzeuge einen kurzen Sessiontitel, eine Lernantwort und genau $requestedCards eigenständige Karteikarten. Keine Markdown-Syntax.")
                put("input", codexInput(question))
                put("reasoning", JSONObject().put("effort", reasoning.lowercase().replace("mittel", "medium").replace("niedrig", "low").replace("hoch", "high")))
                put("text", structuredOutputFormat())
            }
            val connection = (URL(RESPONSES_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 120_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("originator", "codex_cli_rs")
                setRequestProperty("User-Agent", "codex_cli_rs/0.0.0 (Karteikarten Lernen)")
                setRequestProperty("ChatGPT-Account-ID", accountId)
            }
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            val responseText = connection.readBody()
            classifyHttpError(connection.responseCode, responseText)
            val output = parseCodexSse(responseText).trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val result = runCatching { JSONObject(output) }.getOrElse {
                throw CodexAuthException(AuthErrorKind.NETWORK, "Das gewählte GPT-Modell hat keine gültigen strukturierten Lerndaten geliefert.")
            }
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

    private suspend fun exchangeCode(code: String, verifier: String, redirectUri: String): AuthResult {
        val form = formBody(
            "grant_type" to "authorization_code",
            "client_id" to CLIENT_ID,
            "code" to code,
            "redirect_uri" to redirectUri,
            "code_verifier" to verifier,
        )
        val connection = postFormWithDnsRetry(TOKEN_URL, form)
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

    private suspend fun validAccessToken(): String {
        val token = store.getString(KEY_ACCESS_TOKEN, null)
            ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Bitte zuerst bei OpenAI anmelden.")
        if (System.currentTimeMillis() < store.getLong(KEY_EXPIRES_AT, 0) - REFRESH_SKEW_MS) return token
        val refreshToken = store.getString(KEY_REFRESH_TOKEN, null)
            ?: throw CodexAuthException(AuthErrorKind.REAUTH, "Die Anmeldung ist abgelaufen. Bitte erneut anmelden.")
        val connection = postFormWithDnsRetry(
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

    private fun postJson(url: String, json: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20_000
        readTimeout = 20_000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Accept", "application/json")
        outputStream.use { it.write(json.toByteArray()) }
    }

    private suspend fun postFormWithDnsRetry(url: String, form: String): HttpURLConnection {
        return withDnsRetry { postForm(url, form) }
    }

    private suspend fun postJsonWithDnsRetry(url: String, json: String): HttpURLConnection {
        return withDnsRetry { postJson(url, json) }
    }

    private suspend fun withDnsRetry(block: () -> HttpURLConnection): HttpURLConnection {
        var lastError: UnknownHostException? = null
        repeat(DNS_RETRY_DELAYS_MS.size + 1) { attempt ->
            try {
                return block()
            } catch (error: UnknownHostException) {
                lastError = error
                if (attempt < DNS_RETRY_DELAYS_MS.size) {
                    awaitValidatedNetwork()
                    delay(DNS_RETRY_DELAYS_MS[attempt])
                }
            }
        }
        throw CodexAuthException(
            AuthErrorKind.NETWORK,
            "OpenAI ist über die aktuelle Netzwerkverbindung noch nicht erreichbar. Bitte prüfe WLAN oder Mobilfunk und versuche die Anmeldung erneut. (${lastError?.message})",
        )
    }

    private suspend fun awaitForegroundAndNetwork(activity: ComponentActivity) {
        val ready = withTimeoutOrNull(FOREGROUND_NETWORK_TIMEOUT_MS) {
            while (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || !hasValidatedNetwork()) {
                delay(NETWORK_POLL_MS)
            }
            true
        } ?: false
        if (!ready) {
            throw CodexAuthException(
                AuthErrorKind.NETWORK,
                "Die Anmeldung wurde autorisiert, aber die App hat noch keine aktive Internetverbindung. Kehre zur App zurück und versuche es erneut.",
            )
        }
    }

    private suspend fun awaitValidatedNetwork() {
        withTimeoutOrNull(DNS_NETWORK_WAIT_MS) {
            while (!hasValidatedNetwork()) delay(NETWORK_POLL_MS)
        }
    }

    private fun hasValidatedNetwork(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (error: SecurityException) {
            // The following HTTP request remains the authoritative connectivity check.
            Log.e("CodexAuth", "Network-state permission unavailable; falling back to HTTP", error)
            true
        }
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

    private fun formBody(vararg values: Pair<String, String>): String = values.joinToString("&") {
        "${URLEncoder.encode(it.first, "UTF-8")}=${URLEncoder.encode(it.second, "UTF-8")}"
    }

    companion object {
        private const val CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
        private const val TOKEN_URL = "https://auth.openai.com/oauth/token"
        private const val DEVICE_USER_CODE_URL = "https://auth.openai.com/api/accounts/deviceauth/usercode"
        private const val DEVICE_TOKEN_URL = "https://auth.openai.com/api/accounts/deviceauth/token"
        private const val DEVICE_VERIFICATION_URL = "https://auth.openai.com/codex/device"
        private const val DEVICE_REDIRECT_URI = "https://auth.openai.com/deviceauth/callback"
        private const val RESPONSES_URL = "https://chatgpt.com/backend-api/codex/responses"
        private const val REFRESH_SKEW_MS = 120_000L
        private const val FOREGROUND_NETWORK_TIMEOUT_MS = 5 * 60_000L
        private const val DEVICE_CODE_LIFETIME_MS = 15 * 60_000L
        private const val DNS_NETWORK_WAIT_MS = 10_000L
        private const val NETWORK_POLL_MS = 200L
        internal const val DEFAULT_DEVICE_POLL_SECONDS = 5
        internal const val MIN_DEVICE_POLL_SECONDS = 3
        private const val MAX_DEVICE_POLL_SECONDS = 30
        private const val NETWORK_POLL_BACKOFF_SECONDS = 2
        private const val SLOW_DOWN_SECONDS = 5
        internal val DNS_RETRY_DELAYS_MS = longArrayOf(500L, 1_500L, 3_000L)
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_ACCOUNT_ID = "account_id"
        private const val KEY_EMAIL = "email"
    }
}

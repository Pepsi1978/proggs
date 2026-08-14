package de.frank.stacklabor.werftstudio.service.auth

import de.frank.stacklabor.werftstudio.service.codex.CodexErrorClassifier
import de.frank.stacklabor.werftstudio.service.codex.CodexErrorKind
import de.frank.stacklabor.werftstudio.service.codex.CodexException
import de.frank.stacklabor.werftstudio.service.network.awaitResponse
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class CodexOAuthClient(
    private val tokenStore: CodexTokenStore,
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    private val refreshMutex = Mutex()
    private val loginMutex = Mutex()
    private val _state = MutableStateFlow(tokenStore.read().toAuthState())
    val state: StateFlow<CodexAuthState> = _state.asStateFlow()

    fun currentAccount(): CodexAccount? = tokenStore.read()?.let { CodexAccount(it.accountId, it.email) }

    suspend fun startDeviceAuthorization(): DeviceAuthorizationSession = loginMutex.withLock {
        val response = postJson(
            DEVICE_USER_CODE_URL,
            JSONObject().put("client_id", CLIENT_ID).toString(),
        )
        val json = response.requireJson("OpenAI hat keinen gültigen Gerätecode geliefert.")
        val userCode = json.optString("user_code").trim()
        val deviceAuthId = json.optString("device_auth_id").trim()
        if (userCode.isBlank() || deviceAuthId.isBlank()) {
            throw CodexException(CodexErrorKind.REAUTH, "OpenAI hat keinen vollständigen Gerätecode geliefert.")
        }
        DeviceAuthorizationSession(
            userCode = userCode,
            verificationUri = DEVICE_VERIFICATION_URL,
            deviceAuthId = deviceAuthId,
            expiresAtEpochMillis = clockMillis() + DEVICE_CODE_LIFETIME_MS,
            initialPollSeconds = json.optInt("interval", DEFAULT_POLL_SECONDS)
                .coerceIn(MIN_POLL_SECONDS, MAX_POLL_SECONDS),
        )
    }

    suspend fun finishDeviceAuthorization(
        session: DeviceAuthorizationSession,
        onPollingState: (DevicePollingState) -> Unit = {},
    ): CodexAccount = loginMutex.withLock {
        var pollSeconds = session.initialPollSeconds
        var networkBackoffSeconds = pollSeconds
        while (clockMillis() < session.expiresAtEpochMillis) {
            delay(pollSeconds * 1_000L)
            val poll = try {
                postJsonRaw(
                    DEVICE_TOKEN_URL,
                    JSONObject()
                        .put("device_auth_id", session.deviceAuthId)
                        .put("user_code", session.userCode)
                        .toString(),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                networkBackoffSeconds = (networkBackoffSeconds * 2).coerceAtMost(MAX_POLL_SECONDS)
                pollSeconds = networkBackoffSeconds
                continue
            }

            when {
                poll.code == 200 -> {
                    val authorization = poll.json("OpenAI hat die Geräteanmeldung ungültig bestätigt.")
                    val code = authorization.optString("authorization_code").trim()
                    val verifier = authorization.optString("code_verifier").trim()
                    if (code.isBlank() || verifier.isBlank()) {
                        throw CodexException(
                            CodexErrorKind.REAUTH,
                            "OpenAI hat keinen vollständigen Anmeldecode geliefert.",
                        )
                    }
                    return@withLock exchangeCode(code, verifier)
                }
                poll.body.contains("access_denied", ignoreCase = true) -> throw CodexException(
                    CodexErrorKind.REAUTH,
                    "Die Codex-Anmeldung wurde verweigert.",
                )
                poll.body.contains("expired_token", ignoreCase = true) -> throw CodexException(
                    CodexErrorKind.REAUTH,
                    "Der OpenAI-Gerätecode ist abgelaufen. Bitte die Anmeldung neu starten.",
                )
                poll.code == 429 || poll.body.contains("slow_down", ignoreCase = true) -> {
                    pollSeconds = (pollSeconds + SLOW_DOWN_SECONDS).coerceAtMost(MAX_POLL_SECONDS)
                    onPollingState(DevicePollingState.SLOWED_DOWN)
                }
                poll.code == 403 || poll.code == 404 || poll.code >= 500 ||
                    poll.body.contains("authorization_pending", ignoreCase = true) ->
                    onPollingState(DevicePollingState.WAITING)
                else -> throw CodexErrorClassifier.fromHttp(
                    poll.code,
                    poll.body,
                    poll.retryAfter,
                )
            }
        }
        throw CodexException(
            CodexErrorKind.REAUTH,
            "Der OpenAI-Gerätecode ist abgelaufen. Bitte die Anmeldung neu starten.",
        )
    }

    suspend fun validAccessToken(): String {
        val current = tokenStore.read()
            ?: throw CodexException(CodexErrorKind.REAUTH, "Bitte zuerst bei Codex anmelden.")
        if (!needsRefresh(current)) return current.accessToken
        return refreshMutex.withLock {
            val afterLock = tokenStore.read()
                ?: throw CodexException(CodexErrorKind.REAUTH, "Bitte zuerst bei Codex anmelden.")
            if (!needsRefresh(afterLock)) return@withLock afterLock.accessToken
            refresh(afterLock)
        }
    }

    fun logout() {
        tokenStore.clear()
        _state.value = CodexAuthState.SignedOut
    }

    private suspend fun exchangeCode(code: String, verifier: String): CodexAccount {
        val response = postForm(
            FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", CLIENT_ID)
                .add("code", code)
                .add("redirect_uri", DEVICE_REDIRECT_URI)
                .add("code_verifier", verifier)
                .build(),
        )
        val json = response.requireJson("OpenAI hat keine gültigen Anmeldetokens geliefert.")
        return saveTokens(json, previousRefreshToken = null)
    }

    private suspend fun refresh(current: CodexTokens): String {
        val refreshToken = current.refreshToken ?: run {
            logout()
            throw CodexException(CodexErrorKind.REAUTH, "Die Anmeldung ist abgelaufen. Bitte erneut anmelden.")
        }
        try {
            val response = postForm(
                FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .add("client_id", CLIENT_ID)
                    .build(),
            )
            val json = response.requireJson("OpenAI hat beim Aktualisieren keine gültigen Tokens geliefert.")
            saveTokens(json, previousRefreshToken = refreshToken)
            return tokenStore.read()?.accessToken
                ?: throw CodexException(CodexErrorKind.REAUTH, "Der neue Zugriffstoken konnte nicht gespeichert werden.")
        } catch (error: CodexException) {
            if (error.kind == CodexErrorKind.REAUTH) logout()
            throw error
        }
    }

    private fun saveTokens(json: JSONObject, previousRefreshToken: String?): CodexAccount {
        val accessToken = json.optString("access_token").takeIf(String::isNotBlank)
            ?: throw CodexException(CodexErrorKind.REAUTH, "OpenAI hat keinen Zugriffstoken geliefert.")
        val idToken = json.optString("id_token").takeIf(String::isNotBlank)
        val accountId = jwtClaim(accessToken, "chatgpt_account_id")
            ?: jwtClaim(accessToken, "account_id")
            ?: idToken?.let { jwtClaim(it, "chatgpt_account_id") }
        val email = idToken?.let { jwtEmail(it) } ?: jwtEmail(accessToken)
        val tokens = CodexTokens(
            accessToken = accessToken,
            refreshToken = json.optString("refresh_token").takeIf(String::isNotBlank) ?: previousRefreshToken,
            expiresAtEpochMillis = clockMillis() +
                json.optLong("expires_in", DEFAULT_TOKEN_LIFETIME_SECONDS) * 1_000L,
            accountId = accountId,
            email = email,
        )
        tokenStore.write(tokens)
        return CodexAccount(accountId, email).also { _state.value = CodexAuthState.SignedIn(it) }
    }

    private fun needsRefresh(tokens: CodexTokens): Boolean =
        clockMillis() >= tokens.expiresAtEpochMillis - REFRESH_SKEW_MS

    private suspend fun postJson(url: String, body: String): HttpResult {
        val result = postJsonRaw(url, body)
        if (result.code !in 200..299) {
            throw CodexErrorClassifier.fromHttp(result.code, result.body, result.retryAfter)
        }
        return result
    }

    private suspend fun postJsonRaw(url: String, body: String): HttpResult {
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .build()
        return execute(request)
    }

    private suspend fun postForm(body: FormBody): HttpResult {
        val result = execute(Request.Builder().url(TOKEN_URL).post(body).header("Accept", "application/json").build())
        if (result.code !in 200..299) {
            throw CodexErrorClassifier.fromHttp(result.code, result.body, result.retryAfter)
        }
        return result
    }

    private suspend fun execute(request: Request): HttpResult = withContext(Dispatchers.IO) {
        httpClient.newCall(request).awaitResponse().use {
            HttpResult(it.code, it.body?.string().orEmpty(), it.header("Retry-After"))
        }
    }

    private fun HttpResult.requireJson(message: String): JSONObject {
        if (code !in 200..299) throw CodexErrorClassifier.fromHttp(code, body, retryAfter)
        return json(message)
    }

    private fun HttpResult.json(message: String): JSONObject = runCatching { JSONObject(body) }
        .getOrElse { throw CodexException(CodexErrorKind.NETWORK, message, it) }

    private fun jwtEmail(token: String): String? = jwtPayload(token)?.optString("email")?.takeIf(String::isNotBlank)
        ?: jwtClaim(token, "email")

    private fun jwtClaim(token: String, name: String): String? = jwtPayload(token)
        ?.optJSONObject("https://api.openai.com/auth")
        ?.optString(name)
        ?.takeIf(String::isNotBlank)

    private fun jwtPayload(token: String): JSONObject? = runCatching {
        val part = token.split('.')[1]
        val padded = part.padEnd((part.length + 3) / 4 * 4, '=')
        JSONObject(String(Base64.getUrlDecoder().decode(padded), Charsets.UTF_8))
    }.getOrNull()

    private data class HttpResult(val code: Int, val body: String, val retryAfter: String?)

    private companion object {
        const val CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
        const val TOKEN_URL = "https://auth.openai.com/oauth/token"
        const val DEVICE_USER_CODE_URL = "https://auth.openai.com/api/accounts/deviceauth/usercode"
        const val DEVICE_TOKEN_URL = "https://auth.openai.com/api/accounts/deviceauth/token"
        const val DEVICE_VERIFICATION_URL = "https://auth.openai.com/codex/device"
        const val DEVICE_REDIRECT_URI = "https://auth.openai.com/deviceauth/callback"
        const val DEVICE_CODE_LIFETIME_MS = 15 * 60_000L
        const val REFRESH_SKEW_MS = 120_000L
        const val DEFAULT_TOKEN_LIFETIME_SECONDS = 3_600L
        const val DEFAULT_POLL_SECONDS = 5
        const val MIN_POLL_SECONDS = 3
        const val MAX_POLL_SECONDS = 30
        const val SLOW_DOWN_SECONDS = 5
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }
}

private fun CodexTokens?.toAuthState(): CodexAuthState = if (this == null) {
    CodexAuthState.SignedOut
} else {
    CodexAuthState.SignedIn(CodexAccount(accountId, email))
}

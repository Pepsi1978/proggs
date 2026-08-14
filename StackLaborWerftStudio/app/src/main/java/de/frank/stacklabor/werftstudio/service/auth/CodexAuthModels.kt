package de.frank.stacklabor.werftstudio.service.auth

data class CodexTokens(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtEpochMillis: Long,
    val accountId: String?,
    val email: String?,
)

data class CodexAccount(val accountId: String?, val email: String?)

sealed interface CodexAuthState {
    data object SignedOut : CodexAuthState
    data class SignedIn(val account: CodexAccount) : CodexAuthState
}

data class DeviceAuthorizationSession(
    val userCode: String,
    val verificationUri: String,
    internal val deviceAuthId: String,
    internal val expiresAtEpochMillis: Long,
    internal val initialPollSeconds: Int,
)

enum class DevicePollingState { WAITING, SLOWED_DOWN }

interface CodexTokenStore {
    fun read(): CodexTokens?
    fun write(tokens: CodexTokens)
    fun clear()
}

package de.frank.modellkompass.auth

enum class CodexModel(val label: String, val apiId: String) {
    ASTRA("GPT-6 Astra", "gpt-6-astra"),
    SOL("GPT-6.1 Sol", "gpt-6.1-sol"),
    LUNA("GPT-6 Luna", "gpt-6-luna"),
    TERRA("GPT-5.6 Terra", "gpt-5.6-terra"),
    ;

    val supportedEfforts: List<ReasoningEffort>
        get() = when (this) {
            ASTRA, SOL, TERRA -> ReasoningEffort.entries.toList()
            LUNA -> ReasoningEffort.entries.filter { it != ReasoningEffort.ULTRA }
        }

    fun normalizeEffort(effort: ReasoningEffort): ReasoningEffort =
        effort.takeIf { it in supportedEfforts } ?: ReasoningEffort.MEDIUM
}

enum class ReasoningEffort(val label: String, val apiValue: String) {
    LOW("Niedrig", "low"),
    MEDIUM("Mittel", "medium"),
    HIGH("Hoch", "high"),
    XHIGH("Sehr hoch", "xhigh"),
    MAX("Maximal", "max"),
    ULTRA("Ultra", "ultra"),
}

data class AuthResult(val email: String?)

data class DeviceAuthInfo(
    val userCode: String,
    val verificationUri: String,
)

enum class AuthErrorKind { REAUTH, QUOTA, NETWORK }

/**
 * @param retryable true bei Störungen, die sich von allein erledigen können (Serverfehler 5xx,
 *        Zeitüberschreitungen). Solche Anfragen werden automatisch wiederholt.
 */
class CodexAuthException(
    val kind: AuthErrorKind,
    message: String,
    cause: Throwable? = null,
    val retryable: Boolean = false,
) : Exception(message, cause)

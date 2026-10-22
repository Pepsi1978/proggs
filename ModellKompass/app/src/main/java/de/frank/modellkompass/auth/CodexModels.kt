package de.frank.modellkompass.auth

enum class CodexModel(val label: String, val apiId: String, val anbieter: KiAnbieter = KiAnbieter.OPENAI) {
    ASTRA("GPT-6 Astra", "gpt-6-astra"),
    SOL("GPT-6.1 Sol", "gpt-6.1-sol"),
    LUNA("GPT-6 Luna", "gpt-6-luna"),
    TERRA("GPT-5.6 Terra", "gpt-5.6-terra"),
    OPUS("Claude Opus 5.5", "claude-opus-5-5", KiAnbieter.ANTHROPIC),
    FABLE("Claude Fable 5.1", "claude-fable-5-1", KiAnbieter.ANTHROPIC),
    SONNET("Claude Sonnet 5.5", "claude-sonnet-5-5", KiAnbieter.ANTHROPIC),
    HAIKU("Claude Haiku 5.5", "claude-haiku-5-5", KiAnbieter.ANTHROPIC),
    ;

    val istAnthropic: Boolean get() = anbieter == KiAnbieter.ANTHROPIC

    val supportedEfforts: List<ReasoningEffort>
        get() = when (this) {
            ASTRA, SOL, TERRA -> ReasoningEffort.entries.toList()
            LUNA, OPUS, FABLE, SONNET, HAIKU -> ReasoningEffort.entries.filter { it != ReasoningEffort.ULTRA }
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

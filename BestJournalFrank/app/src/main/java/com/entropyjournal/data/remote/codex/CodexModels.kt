package com.entropyjournal.data.remote.codex

/** Die auswaehlbaren ChatGPT-/Codex-Modelle. */
enum class CodexModel(val label: String, val apiId: String, val anbieter: KiAnbieter = KiAnbieter.OPENAI) {
    ASTRA("GPT-6 Astra", "gpt-6-astra"),
    SOL("GPT-6.1 Sol", "gpt-6.1-sol"),
    TERRA("GPT-5.6 Terra", "gpt-5.6-terra"),
    LUNA("GPT-6 Luna", "gpt-6-luna"),
    OPUS("Claude Opus 5.5", "claude-opus-5-5", KiAnbieter.ANTHROPIC),
    FABLE("Claude Fable 5.1", "claude-fable-5-1", KiAnbieter.ANTHROPIC),
    SONNET("Claude Sonnet 5.5", "claude-sonnet-5-5", KiAnbieter.ANTHROPIC),
    HAIKU("Claude Haiku 5.5", "claude-haiku-5-5", KiAnbieter.ANTHROPIC),
    ;

    val istAnthropic: Boolean get() = anbieter == KiAnbieter.ANTHROPIC

    val supportedEfforts: List<ReasoningEffort>
        get() = when (this) {
            ASTRA, SOL, TERRA -> ReasoningEffort.entries
            LUNA, OPUS, FABLE, SONNET, HAIKU -> ReasoningEffort.entries.filter { it != ReasoningEffort.ULTRA }
        }

    fun normalizeEffort(effort: ReasoningEffort): ReasoningEffort =
        effort.takeIf { it in supportedEfforts } ?: ReasoningEffort.DEFAULT

    companion object {
        val DEFAULT = TERRA

        fun fromId(value: String?): CodexModel = entries.firstOrNull {
            it.apiId.equals(value?.trim(), ignoreCase = true) ||
                it.label.equals(value?.trim(), ignoreCase = true)
        } ?: when {
            value?.contains("sol", ignoreCase = true) == true -> SOL
            value?.contains("luna", ignoreCase = true) == true -> LUNA
            value?.contains("astra", ignoreCase = true) == true -> ASTRA
            else -> DEFAULT
        }

        fun von(anbieter: KiAnbieter): List<CodexModel> = entries.filter { it.anbieter == anbieter }
    }
}

/** Wie lange das Modell vor der Antwort nachdenkt ("Thinking"). */
enum class ReasoningEffort(val label: String, val apiValue: String) {
    LOW("Niedrig", "low"),
    MEDIUM("Mittel", "medium"),
    HIGH("Hoch", "high"),
    XHIGH("Sehr hoch", "xhigh"),
    MAX("Maximal", "max"),
    ULTRA("Ultra", "ultra"),
    ;

    companion object {
        val DEFAULT = MEDIUM

        fun fromValue(value: String?): ReasoningEffort = entries.firstOrNull {
            it.apiValue.equals(value?.trim(), ignoreCase = true) ||
                it.label.equals(value?.trim(), ignoreCase = true)
        } ?: DEFAULT
    }
}

data class AuthResult(val email: String?)

data class DeviceAuthInfo(
    val userCode: String,
    val verificationUri: String,
)

enum class AuthErrorKind { REAUTH, QUOTA, NETWORK }

/**
 * @param retryable true bei Stoerungen, die sich von allein erledigen koennen (Serverfehler 5xx,
 *        Zeitueberschreitungen). Solche Anfragen werden automatisch wiederholt.
 */
class CodexAuthException(
    val kind: AuthErrorKind,
    message: String,
    cause: Throwable? = null,
    val retryable: Boolean = false,
) : Exception(message, cause)

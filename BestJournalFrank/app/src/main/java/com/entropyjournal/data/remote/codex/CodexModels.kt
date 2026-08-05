package com.entropyjournal.data.remote.codex

/** Die auswaehlbaren ChatGPT-/Codex-Modelle. */
enum class CodexModel(val label: String, val apiId: String) {
    SOL("GPT 5.6 Sol", "gpt-5.6-sol"),
    TERRA("GPT 5.6 Terra", "gpt-5.6-terra"),
    LUNA("GPT 5.6 Luna", "gpt-5.6-luna"),
    ;

    companion object {
        val DEFAULT = TERRA

        fun fromId(value: String?): CodexModel = entries.firstOrNull {
            it.apiId.equals(value?.trim(), ignoreCase = true) ||
                it.label.equals(value?.trim(), ignoreCase = true)
        } ?: DEFAULT
    }
}

/** Wie lange das Modell vor der Antwort nachdenkt ("Thinking"). */
enum class ReasoningEffort(val label: String, val apiValue: String) {
    LOW("Niedrig", "low"),
    MEDIUM("Mittel", "medium"),
    HIGH("Hoch", "high"),
    XHIGH("Sehr hoch", "xhigh"),
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

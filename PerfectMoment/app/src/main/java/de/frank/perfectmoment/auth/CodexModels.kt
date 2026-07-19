package de.frank.perfectmoment.auth

enum class CodexModel(val label: String, val apiId: String) {
    SOL("GPT 5.6 Sol", "gpt-5.6-sol"),
    TERRA("GPT 5.6 Terra", "gpt-5.6-terra"),
    LUNA("GPT 5.6 Luna", "gpt-5.6-luna"),
    ;

    companion object {
        fun fromLabel(value: String): CodexModel = entries.firstOrNull {
            it.label.equals(value.trim(), ignoreCase = true) || it.apiId.equals(value.trim(), ignoreCase = true)
        } ?: TERRA
    }
}

enum class ReasoningEffort(val label: String, val apiValue: String) {
    LOW("Niedrig", "low"),
    MEDIUM("Mittel", "medium"),
    HIGH("Hoch", "high"),
    XHIGH("Sehr hoch", "xhigh"),
    ;

    companion object {
        fun fromLabel(value: String): ReasoningEffort = entries.firstOrNull {
            it.label.equals(value.trim(), ignoreCase = true) || it.apiValue.equals(value.trim(), ignoreCase = true)
        } ?: MEDIUM
    }
}

data class CodexQuestionRequest(
    val topic: String,
    val introContext: String = "",
    val previousQuestions: List<String> = emptyList(),
    val skillText: String,
    val operatingModeText: String,
    val model: CodexModel = CodexModel.TERRA,
    val reasoningEffort: ReasoningEffort = ReasoningEffort.MEDIUM,
)

data class CodexQuestion(
    val emoji: String,
    val text: String,
)

data class AuthResult(val email: String?)

data class DeviceAuthInfo(
    val userCode: String,
    val verificationUri: String,
)

enum class AuthErrorKind { REAUTH, QUOTA, NETWORK }

class CodexAuthException(
    val kind: AuthErrorKind,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class QuestionValidationException(message: String) : IllegalArgumentException(message)

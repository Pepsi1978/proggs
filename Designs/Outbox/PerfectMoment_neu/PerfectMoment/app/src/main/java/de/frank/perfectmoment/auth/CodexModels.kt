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

enum class QuestionPerspective(val id: String, val label: String) {
    FIRST_PERSON("first_person", "Ich-Person"),
    SECOND_PERSON("second_person", "Du-Person"),
    ;

    companion object {
        fun fromId(value: String): QuestionPerspective = entries.firstOrNull { it.id == value } ?: SECOND_PERSON
    }
}

data class CodexQuestionRequest(
    val topic: String,
    val introContext: String = "",
    val entranceQuestion: String = "",
    val previousQuestions: List<String> = emptyList(),
    val skillText: String,
    val operatingModeText: String,
    val perspective: QuestionPerspective = QuestionPerspective.SECOND_PERSON,
    val model: CodexModel = CodexModel.TERRA,
    val reasoningEffort: ReasoningEffort = ReasoningEffort.MEDIUM,
)

data class SessionPromptDecision(
    val requiresAnswer: Boolean,
    val question: String,
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

class QuestionValidationException(message: String) : IllegalArgumentException(message)

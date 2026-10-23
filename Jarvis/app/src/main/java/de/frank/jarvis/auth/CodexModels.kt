package de.frank.jarvis.auth

/** Wer das Modell rechnet: OpenAI über das ChatGPT-Konto oder Anthropic über einen eigenen API-Schlüssel. */
enum class KiAnbieter(val label: String) { OPENAI("OpenAI"), ANTHROPIC("Anthropic") }

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

    /** Anthropic kennt die Stufen low bis max, aber kein Ultra. */
    val supportedEfforts: List<ReasoningEffort>
        get() = when (this) {
            ASTRA, SOL, TERRA -> ReasoningEffort.entries.toList()
            LUNA, OPUS, FABLE, SONNET, HAIKU -> ReasoningEffort.entries.filter { it != ReasoningEffort.ULTRA }
        }

    fun normalizeEffort(effort: ReasoningEffort): ReasoningEffort =
        effort.takeIf { it in supportedEfforts } ?: ReasoningEffort.MEDIUM

    companion object {
        fun fromLabel(value: String): CodexModel = entries.firstOrNull {
            it.label.equals(value.trim(), ignoreCase = true) || it.apiId.equals(value.trim(), ignoreCase = true)
        } ?: when {
            value.contains("sol", ignoreCase = true) -> SOL
            value.contains("luna", ignoreCase = true) -> LUNA
            value.contains("astra", ignoreCase = true) -> ASTRA
            else -> TERRA
        }

        fun von(anbieter: KiAnbieter): List<CodexModel> = entries.filter { it.anbieter == anbieter }
    }
}

enum class ReasoningEffort(val label: String, val apiValue: String) {
    LOW("Niedrig", "low"),
    MEDIUM("Mittel", "medium"),
    HIGH("Hoch", "high"),
    XHIGH("Sehr hoch", "xhigh"),
    MAX("Maximal", "max"),
    ULTRA("Ultra", "ultra"),
    ;

    /** Anzeigename je Modell: Anthropic nennt die Stufen wie in der Claude-App (Niedrig, Mittel, Hoch, Extra hoch, Maximal). */
    fun titel(modell: CodexModel?): String = if (this == XHIGH && modell?.istAnthropic == true) "Extra hoch" else label

    companion object {
        fun fromLabel(value: String): ReasoningEffort = entries.firstOrNull {
            it.label.equals(value.trim(), ignoreCase = true) || it.apiValue.equals(value.trim(), ignoreCase = true)
        } ?: if (value.trim().equals("Extra hoch", ignoreCase = true)) XHIGH else MEDIUM
    }
}

enum class QuestionPerspective(val id: String, val label: String) {
    FIRST_PERSON("first_person", "Ich-Person"),
    SECOND_PERSON("second_person", "Du-Person"),
    NEUTRAL_PERSON("neutral_person", "Man-Person"),
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

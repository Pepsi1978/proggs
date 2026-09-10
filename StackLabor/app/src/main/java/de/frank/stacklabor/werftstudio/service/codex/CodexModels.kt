package de.frank.stacklabor.werftstudio.service.codex

enum class CodexModel(val apiId: String) {
    ASTRA("gpt-6-astra"),
    SOL("gpt-5.6-sol"),
    TERRA("gpt-5.6-terra"),
    LUNA("gpt-5.6-luna");

    val label: String get() = apiId.substringAfterLast('-').replaceFirstChar(Char::uppercase)
    val reasoningEfforts: List<ReasoningEffort>
        get() = if (this == ASTRA) ReasoningEffort.entries else ReasoningEffort.entries.filter { it != ReasoningEffort.ULTRA }

    fun normalizeReasoning(value: String?): ReasoningEffort =
        reasoningEfforts.firstOrNull { it.apiValue == value }
            ?: if (this == ASTRA) ReasoningEffort.MEDIUM else ReasoningEffort.HIGH

    companion object {
        fun fromApiId(value: String?): CodexModel = entries.firstOrNull { it.apiId == value } ?: TERRA
    }
}

enum class ReasoningEffort(val apiValue: String, val label: String) {
    LOW("low", "Niedrig"),
    MEDIUM("medium", "Mittel"),
    HIGH("high", "Hoch"),
    XHIGH("xhigh", "Sehr hoch"),
    MAX("max", "Maximal"),
    ULTRA("ultra", "Ultra"),
}

enum class CodexErrorKind { REAUTH, QUOTA, NETWORK }

class CodexException(
    val kind: CodexErrorKind,
    message: String,
    cause: Throwable? = null,
    val retryable: Boolean = false,
    val retryAfterSeconds: Long? = null,
) : Exception(message, cause)

data class CodexGoal(val id: String, val rank: Int, val text: String)

enum class CodexEvaluationScope { STACK, DAY }

data class CodexEvaluationRequest(
    val evaluationId: String,
    val contextJson: String,
    val goals: List<CodexGoal>,
    val supplementIds: Set<String>,
    val questionIds: Set<String>,
    val scope: CodexEvaluationScope,
    val model: CodexModel = CodexModel.TERRA,
    val reasoningEffort: ReasoningEffort = ReasoningEffort.HIGH,
)

enum class CellEffect(val wireValue: String) { SUPPORTS("stuetzt"), DISRUPTS("stoert") }

data class EvaluationCell(
    val supplementId: String,
    val goalId: String,
    val effect: CellEffect,
    val strength: Int,
    val reason: String,
)

enum class CompetitionType(val wireValue: String) { ABSORPTION("aufnahme"), EFFECT("wirkung"), TIMING("zeitpunkt") }

data class Competition(
    val supplementAId: String,
    val supplementBId: String,
    val type: CompetitionType,
    val severity: Int,
    val reason: String,
)

data class QuestionAnswer(val questionId: String, val text: String)

data class CodexEvaluation(
    val cells: List<EvaluationCell>,
    val competitions: List<Competition>,
    val answers: List<QuestionAnswer>,
    val narrative: String,
    val notices: List<String>,
)

sealed interface CodexEvaluationOutcome {
    data class Success(val evaluation: CodexEvaluation) : CodexEvaluationOutcome
    data class InvalidJson(
        val preservedNarrative: String,
        val rawOutput: String,
        val reason: String,
    ) : CodexEvaluationOutcome
}

sealed interface CodexStreamEvent {
    data class Stage(val text: String) : CodexStreamEvent
    data class RawDelta(val text: String) : CodexStreamEvent
    data class NarrativeDelta(val text: String) : CodexStreamEvent
    data object NetworkRetry : CodexStreamEvent
}

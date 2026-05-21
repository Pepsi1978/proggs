package de.frank.entropyreducer.presentation.agentic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.domain.agentic.WorkflowEvent
import de.frank.entropyreducer.domain.agentic.WorkflowRunner
import de.frank.entropyreducer.domain.agentic.gates.UiConfirmationGate
import de.frank.entropyreducer.domain.model.ConfirmDecision
import de.frank.entropyreducer.domain.model.ExecutionStatus
import de.frank.entropyreducer.domain.model.StepType
import de.frank.entropyreducer.domain.model.TriggerSource
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel fuer die agentic-Prompt-Ausfuehrung (Frank-Wunsch 2026-05-21).
 *
 * Workflow:
 *  - Compose ruft `runPrompt(promptId, userInputContext)` auf
 *  - ViewModel startet den WorkflowRunner und konsumiert den Flow<WorkflowEvent>
 *  - Jedes Event wird in den UiState gemerged — UI beobachtet uiState via collectAsState
 *  - Nach Finished ist isRunning=false, finalAnswer und ggf. errorMessage gesetzt
 *
 * Pattern: ein einzelner aktiver Run pro ViewModel-Instanz. Wenn der Nutzer
 * waehrend eines laufenden Runs einen weiteren Prompt ausfuehrt, wird der erste
 * abgebrochen (cancelCurrentRun).
 */
@HiltViewModel
class AgenticPromptViewModel
@Inject
constructor(
    private val workflowRunner: WorkflowRunner,
    private val uiConfirmationGate: UiConfirmationGate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgenticRunUiState())
    val uiState: StateFlow<AgenticRunUiState> = _uiState.asStateFlow()

    /** Lebender Stream der ausstehenden Confirmations — von der UI direkt beobachtet. */
    val pendingConfirmationFlow: StateFlow<de.frank.entropyreducer.domain.agentic.gates.ConfirmationRequest?> =
        uiConfirmationGate.pendingRequest

    private var currentRunJob: Job? = null

    fun approveConfirmation() {
        uiConfirmationGate.respond(ConfirmDecision.APPROVED)
        _uiState.update { it.copy(pendingConfirmation = null) }
    }

    fun rejectConfirmation(reason: String? = null) {
        uiConfirmationGate.respond(ConfirmDecision.REJECTED, reason)
        _uiState.update { it.copy(pendingConfirmation = null) }
    }

    fun runPrompt(
        promptId: String,
        userInputContext: String? = null,
        triggerSource: TriggerSource = TriggerSource.MANUAL,
    ) {
        // Falls bereits ein Run laeuft, abbrechen
        cancelCurrentRun()

        _uiState.value =
            AgenticRunUiState(
                isRunning = true,
                isDialogOpen = true,
                promptId = promptId,
                userInputContext = userInputContext,
                startedAt = System.currentTimeMillis(),
            )

        currentRunJob =
            viewModelScope.launch {
                workflowRunner.run(promptId, userInputContext, triggerSource).collect { event ->
                    _uiState.update { state -> reduce(state, event) }
                }
            }
    }

    fun cancelCurrentRun() {
        // Direktive 3 Loop-1-Fix (war MED-5-Bug): wenn ein Confirm-Dialog
        // gerade offen ist (Continuation suspended), MUSS die Continuation
        // sauber aufgeloest werden — sonst haengt sie bis zum 60s-Timeout
        // und blockiert den naechsten Run. respond(REJECTED) loest sie sofort.
        if (uiConfirmationGate.pendingRequest.value != null) {
            uiConfirmationGate.respond(
                decision = ConfirmDecision.REJECTED,
                rejectReason = "Run vom Nutzer abgebrochen",
            )
        }
        currentRunJob?.cancel()
        currentRunJob = null
        _uiState.update {
            if (it.isRunning) {
                it.copy(
                    isRunning = false,
                    errorMessage = it.errorMessage ?: "Vom Nutzer abgebrochen",
                )
            } else it
        }
    }

    fun dismissDialog() {
        // Dialog schliessen, Run lassen wenn er noch laeuft (Background)
        _uiState.update { it.copy(isDialogOpen = false) }
    }

    fun reopenLastResult() {
        // Frank kann den Dialog wieder oeffnen falls er ihn versehentlich geschlossen hat
        _uiState.update { it.copy(isDialogOpen = true) }
    }

    private fun reduce(state: AgenticRunUiState, event: WorkflowEvent): AgenticRunUiState =
        when (event) {
            is WorkflowEvent.Started ->
                state.copy(
                    executionId = event.executionId,
                    promptName = event.promptName,
                    modelUsed = event.modelUsed,
                    statusLine = "Lauf gestartet…",
                )

            is WorkflowEvent.StepStarted ->
                state.copy(
                    statusLine =
                        when (event.stepType) {
                            StepType.LLM_CALL -> "Gemini denkt nach…"
                            StepType.TOOL_CALL ->
                                "Werkzeug \"${event.toolName ?: ""}\" wird ausgefuehrt…"
                            StepType.USER_CONFIRM -> "Warte auf Bestaetigung…"
                            StepType.BLOCKED -> "Schritt blockiert"
                        },
                    steps = state.steps + StepDisplay(
                        index = event.stepIndex,
                        type = event.stepType,
                        toolName = event.toolName,
                        message = "(laeuft…)",
                    ),
                )

            is WorkflowEvent.LlmStepCompleted ->
                state.copy(
                    statusLine = if (event.text != null) "Gemini hat geantwortet." else null,
                    tokensInputTotal = state.tokensInputTotal + event.tokensInput,
                    tokensOutputTotal = state.tokensOutputTotal + event.tokensOutput,
                    steps =
                        state.steps.map { step ->
                            if (step.index == event.stepIndex && step.type == StepType.LLM_CALL) {
                                step.copy(message = event.text?.take(400) ?: "(nur Tool-Aufrufe)")
                            } else step
                        },
                )

            is WorkflowEvent.ToolStepCompleted ->
                state.copy(
                    statusLine =
                        if (event.success) "${event.toolName}: ok"
                        else "${event.toolName}: Fehler",
                    steps =
                        state.steps.map { step ->
                            if (step.index == event.stepIndex && step.type == StepType.TOOL_CALL) {
                                step.copy(
                                    message =
                                        if (event.success)
                                            event.resultPreview?.take(300) ?: "Erfolgreich"
                                        else "Fehler: ${event.error ?: "unbekannt"}",
                                )
                            } else step
                        },
                )

            is WorkflowEvent.ConfirmationRequested ->
                state.copy(
                    statusLine =
                        "Bestaetigung erforderlich fuer ${event.request.tool.name}",
                    pendingConfirmation = event.request,
                )

            is WorkflowEvent.Blocked ->
                state.copy(
                    statusLine = "Blockiert: ${event.reason}",
                    steps = state.steps + StepDisplay(
                        index = event.stepIndex,
                        type = StepType.BLOCKED,
                        toolName = event.toolName,
                        message = event.reason,
                    ),
                )

            is WorkflowEvent.Finished ->
                state.copy(
                    isRunning = false,
                    finalStatus = event.status,
                    finalAnswer = event.finalAnswer,
                    errorMessage = event.errorMessage,
                    toolCallCount = event.toolCallCount,
                    durationMillis = event.durationMillis,
                    statusLine =
                        when (event.status) {
                            ExecutionStatus.SUCCESS -> "Lauf erfolgreich beendet."
                            ExecutionStatus.FAILED ->
                                "Lauf fehlgeschlagen: ${event.errorMessage ?: "unbekannt"}"
                            ExecutionStatus.BLOCKED_BY_TOKEN_LIMIT ->
                                "Lauf gestoppt: Token-Limit ueberschritten."
                            ExecutionStatus.BLOCKED_BY_PERMISSION ->
                                "Lauf gestoppt: Berechtigung fehlt."
                            ExecutionStatus.BLOCKED_BY_TIMEOUT ->
                                "Lauf gestoppt: Zeitlimit ueberschritten."
                            ExecutionStatus.BLOCKED_BY_USER_REJECT ->
                                "Lauf gestoppt: vom Nutzer abgelehnt."
                            ExecutionStatus.CANCELLED -> "Lauf abgebrochen."
                            ExecutionStatus.INTERRUPTED -> "Lauf unterbrochen."
                            ExecutionStatus.RUNNING -> "Laeuft noch?"
                        },
                )
        }

    override fun onCleared() {
        // Auch bei ViewModel-Cleanup haengende Continuation aufloesen
        if (uiConfirmationGate.pendingRequest.value != null) {
            uiConfirmationGate.respond(
                decision = ConfirmDecision.REJECTED,
                rejectReason = "ViewModel beendet",
            )
        }
        currentRunJob?.cancel()
        super.onCleared()
    }
}

/**
 * UI-State fuer den agentic-Run-Dialog. Wird vom ExecutionDialog konsumiert.
 */
data class AgenticRunUiState(
    val isDialogOpen: Boolean = false,
    val isRunning: Boolean = false,
    val promptId: String? = null,
    val promptName: String? = null,
    val modelUsed: String? = null,
    val executionId: String? = null,
    val userInputContext: String? = null,
    val startedAt: Long = 0L,
    val statusLine: String? = null,
    val steps: List<StepDisplay> = emptyList(),
    val finalStatus: ExecutionStatus? = null,
    val finalAnswer: String? = null,
    val errorMessage: String? = null,
    val toolCallCount: Int = 0,
    val tokensInputTotal: Int = 0,
    val tokensOutputTotal: Int = 0,
    val durationMillis: Long = 0L,
    val pendingConfirmation: de.frank.entropyreducer.domain.agentic.gates.ConfirmationRequest? = null,
)

/**
 * Ein Step im UI-Verlauf. Wird im Dialog als Liste angezeigt.
 */
data class StepDisplay(
    val index: Int,
    val type: StepType,
    val toolName: String? = null,
    val message: String = "",
)

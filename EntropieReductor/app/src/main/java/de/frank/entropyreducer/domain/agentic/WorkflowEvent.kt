package de.frank.entropyreducer.domain.agentic

import de.frank.entropyreducer.domain.agentic.gates.ConfirmationRequest
import de.frank.entropyreducer.domain.model.ExecutionStatus
import de.frank.entropyreducer.domain.model.StepType

/**
 * Stream von Ereignissen waehrend einer Prompt-Ausfuehrung. Der WorkflowRunner
 * gibt einen Flow<WorkflowEvent> zurueck — UI und Trigger-System abonnieren das
 * und reagieren auf Started, einzelne Steps, Confirmation-Requests und Finished.
 *
 * Lebenszyklus pro Run:
 *   Started -> (LlmCallStep | ToolCallStep | ConfirmationRequested | Blocked)* -> Finished
 *
 * Ein Confirmation-Request bricht den Run NICHT ab — das UI bestaetigt
 * asynchron ueber den ConfirmationGate-Mechanismus. Der Workflow-Runner
 * wartet auf die ConfirmationResult, der Flow geht waehrenddessen nicht
 * weiter.
 */
sealed class WorkflowEvent {
    abstract val executionId: String

    data class Started(
        override val executionId: String,
        val promptId: String,
        val promptName: String,
        val modelUsed: String,
    ) : WorkflowEvent()

    /** Wird gerade ein LLM_CALL ausgefuehrt (vor Gemini-Aufruf). */
    data class StepStarted(
        override val executionId: String,
        val stepIndex: Int,
        val stepType: StepType,
        val toolName: String? = null,
    ) : WorkflowEvent()

    /** LLM_CALL ist fertig, mit Text-Output (oder null wenn Gemini nur Tool-Call schickt). */
    data class LlmStepCompleted(
        override val executionId: String,
        val stepIndex: Int,
        val text: String? = null,
        val tokensInput: Int = 0,
        val tokensOutput: Int = 0,
    ) : WorkflowEvent()

    /** TOOL_CALL ist abgeschlossen. */
    data class ToolStepCompleted(
        override val executionId: String,
        val stepIndex: Int,
        val toolName: String,
        val success: Boolean,
        val resultPreview: String? = null,
        val error: String? = null,
    ) : WorkflowEvent()

    /**
     * UI-Bridge: ein Write-Tool wartet auf User-Bestaetigung. UI zeigt den
     * Confirm-Dialog. Beachtung: der ConfirmationGate.request()-Aufruf laeuft
     * parallel und wartet auf die Antwort — das Event ist nur fuer das UI
     * informativ.
     */
    data class ConfirmationRequested(
        override val executionId: String,
        val request: ConfirmationRequest,
    ) : WorkflowEvent()

    /** Ein Gate hat den Aufruf blockiert. */
    data class Blocked(
        override val executionId: String,
        val stepIndex: Int,
        val toolName: String? = null,
        val reason: String,
    ) : WorkflowEvent()

    /** Run ist beendet. status sagt warum: SUCCESS, FAILED, BLOCKED_BY_*, etc. */
    data class Finished(
        override val executionId: String,
        val status: ExecutionStatus,
        val finalAnswer: String? = null,
        val errorMessage: String? = null,
        val tokensTotal: Int = 0,
        val toolCallCount: Int = 0,
        val durationMillis: Long = 0L,
    ) : WorkflowEvent()
}

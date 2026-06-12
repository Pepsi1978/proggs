package de.frank.entropyreducer.domain.agentic

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.entities.PromptExecutionEntity
import de.frank.entropyreducer.data.local.entities.PromptExecutionStepEntity
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.data.repository.PromptExecutionRepository
import de.frank.entropyreducer.domain.model.ExecutionStatus
import de.frank.entropyreducer.domain.model.TriggerSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schmaler Helper fuer das Audit-Log. Kapselt die DB-Schreibungen die der
 * WorkflowRunner waehrend eines Runs macht. Trennt die DB-Mechanik von der
 * ReAct-Loop-Logik im Runner.
 *
 * Lebenszyklus-Aufrufe:
 *   start(...) -> N x logStep(...) -> complete(...) ODER fail(...)
 *
 * Bei App-Crash bleibt der prompt_executions-Eintrag auf RUNNING — der
 * markRunningAsInterrupted-Aufruf im PromptExecutionRepository (vom App-
 * Startup-Code) faengt das.
 */
@Singleton
class ExecutionLogger
@Inject
constructor(private val executionRepo: PromptExecutionRepository) {

    /**
     * Logging-Start: schreibt einen neuen prompt_executions-Eintrag mit
     * Status RUNNING und Snapshot des Prompts (damit Audit eine
     * Loeschung ueberlebt).
     */
    suspend fun start(
        executionId: String,
        prompt: SavedPromptEntity,
        userInputContext: String?,
        startedAt: Long,
        modelUsed: String,
        triggerSource: TriggerSource,
    ) {
        executionRepo.upsertExecution(
            PromptExecutionEntity(
                id = executionId,
                promptId = prompt.id,
                snapshotName = prompt.name,
                snapshotContent = prompt.content,
                snapshotCategory = prompt.category,
                snapshotModel = prompt.model,
                userInputContext = userInputContext,
                startedAt = startedAt,
                status = ExecutionStatus.RUNNING,
                modelUsed = modelUsed,
                triggerSource = triggerSource,
            )
        )
    }

    /** Einzelnen Schritt loggen. */
    suspend fun logStep(step: PromptExecutionStepEntity) {
        executionRepo.upsertStep(step)
    }

    /**
     * Run erfolgreich abgeschlossen — Status auf SUCCESS setzen mit
     * finalAnswer + Bilanz (Tokens, Tool-Calls, Dauer).
     */
    suspend fun complete(
        executionId: String,
        finalAnswer: String?,
        tokensInput: Int,
        tokensOutput: Int,
        toolCallCount: Int,
        finishedAt: Long,
    ) {
        val existing = executionRepo.getById(executionId)
        if (existing == null) {
            // Direktive 3 Loop-1-Fix (war MED-4-Bug): nicht silent verwerfen.
            Diag.e(DiagnosticArea.AGENTIC, 
                "ExecutionLogger",
                "complete() fuer unbekannte executionId=$executionId — " +
                    "Tokens und finalAnswer werden NICHT persistiert. " +
                    "Das deutet auf einen Race oder einen geloeschten Audit-Eintrag hin.",
            )
            return
        }
        executionRepo.updateExecution(
            existing.copy(
                status = ExecutionStatus.SUCCESS,
                finalAnswer = finalAnswer,
                tokensInput = tokensInput,
                tokensOutput = tokensOutput,
                tokensTotal = tokensInput + tokensOutput,
                toolCallCount = toolCallCount,
                finishedAt = finishedAt,
            )
        )
    }

    /**
     * Run fehlgeschlagen ODER blockiert. status sollte FAILED oder BLOCKED_BY_*
     * sein. errorMessage ist Pflicht.
     */
    suspend fun fail(
        executionId: String,
        status: ExecutionStatus,
        errorMessage: String,
        tokensInput: Int = 0,
        tokensOutput: Int = 0,
        toolCallCount: Int = 0,
        finishedAt: Long = System.currentTimeMillis(),
    ) {
        val existing = executionRepo.getById(executionId)
        if (existing == null) {
            // Direktive 3 Loop-1-Fix (war MED-4-Bug): nicht silent verwerfen.
            Diag.e(DiagnosticArea.AGENTIC, 
                "ExecutionLogger",
                "fail() fuer unbekannte executionId=$executionId — " +
                    "Fehler-Begruendung wird NICHT persistiert: $errorMessage",
            )
            return
        }
        executionRepo.updateExecution(
            existing.copy(
                status = status,
                errorMessage = errorMessage,
                tokensInput = tokensInput,
                tokensOutput = tokensOutput,
                tokensTotal = tokensInput + tokensOutput,
                toolCallCount = toolCallCount,
                finishedAt = finishedAt,
            )
        )
    }
}

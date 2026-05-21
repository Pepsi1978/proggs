package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.PromptExecutionDao
import de.frank.entropyreducer.data.local.dao.PromptExecutionStepDao
import de.frank.entropyreducer.data.local.entities.PromptExecutionEntity
import de.frank.entropyreducer.data.local.entities.PromptExecutionStepEntity
import de.frank.entropyreducer.domain.model.ExecutionStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Repository fuer das Audit-Log der Prompt-Ausfuehrungen. Kapselt sowohl die Top-Level-
 * Ausfuehrungseintraege (prompt_executions) als auch die feinkoernigen Schritte
 * (prompt_execution_steps) — die zwei Tabellen werden vom ExecutionLogger immer
 * zusammen geschrieben.
 *
 * Lese-Schnittstelle:
 * - getRecent(): juengste N Ausfuehrungen ueber alle Prompts
 * - getByPrompt(promptId): Verlauf eines bestimmten Prompts
 * - getStepsForExecution(id): einzelne Schritte zur Detailansicht
 *
 * Schreib-Schnittstelle (nur vom WorkflowRunner / ExecutionLogger genutzt):
 * - upsertExecution / updateExecution
 * - upsertStep
 * - markRunningAsInterrupted: App-Crash-Erkennung beim App-Start
 */
@Singleton
class PromptExecutionRepository
@Inject
constructor(
    private val executionDao: PromptExecutionDao,
    private val stepDao: PromptExecutionStepDao,
) {
    // Lese-Operationen

    fun getRecent(limit: Int = 50): Flow<List<PromptExecutionEntity>> =
        executionDao.getRecent(limit)

    fun getByPrompt(promptId: String, limit: Int = 30): Flow<List<PromptExecutionEntity>> =
        executionDao.getByPrompt(promptId, limit)

    suspend fun getById(id: String): PromptExecutionEntity? = executionDao.getById(id)

    fun getStepsForExecution(executionId: String): Flow<List<PromptExecutionStepEntity>> =
        stepDao.getByExecution(executionId)

    suspend fun getStepsSnapshot(executionId: String): List<PromptExecutionStepEntity> =
        stepDao.getByExecutionSnapshot(executionId)

    fun getByStatus(status: ExecutionStatus): Flow<List<PromptExecutionEntity>> =
        executionDao.getByStatus(status)

    suspend fun count(): Int = executionDao.count()

    // Schreib-Operationen (nur vom WorkflowRunner / ExecutionLogger)

    suspend fun upsertExecution(execution: PromptExecutionEntity) {
        executionDao.upsert(execution)
    }

    suspend fun updateExecution(execution: PromptExecutionEntity) {
        executionDao.update(execution)
    }

    suspend fun upsertStep(step: PromptExecutionStepEntity) {
        stepDao.upsert(step)
    }

    suspend fun deleteExecution(execution: PromptExecutionEntity) {
        executionDao.delete(execution)
    }

    /**
     * Beim App-Start aufrufen: alle Ausfuehrungen die noch als RUNNING markiert
     * sind werden auf INTERRUPTED gesetzt. Schuetzt vor "Geister-Runs" nach
     * App-Crash oder erzwungenem Beenden.
     */
    suspend fun markRunningAsInterrupted(now: Long) {
        executionDao.getRunning().forEach { exec ->
            executionDao.update(
                exec.copy(
                    status = ExecutionStatus.INTERRUPTED,
                    finishedAt = now,
                    errorMessage = "App wurde beendet bevor der Lauf abgeschlossen war",
                )
            )
        }
    }

    /** Aelteste N Eintraege loeschen — fuer manuelles Aufraeumen aus den Einstellungen. */
    suspend fun pruneOldest(count: Int) {
        executionDao.pruneOldest(count)
    }
}

package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import de.frank.entropyreducer.data.local.entities.PromptExecutionEntity
import de.frank.entropyreducer.data.local.entities.PromptExecutionStepEntity
import de.frank.entropyreducer.data.local.entities.PromptToolPermissionEntity
import de.frank.entropyreducer.data.local.entities.PromptTriggerEntity
import de.frank.entropyreducer.data.local.entities.TokenUsageDailyEntity
import de.frank.entropyreducer.domain.model.ExecutionStatus
import de.frank.entropyreducer.domain.model.TriggerType
import kotlinx.coroutines.flow.Flow

/**
 * Audit-Log aller Prompt-Ausfuehrungen. Wird vom ExecutionLogger im WorkflowRunner
 * gefuettert, nicht direkt vom UI.
 */
@Dao
interface PromptExecutionDao {
    @Query("SELECT * FROM prompt_executions ORDER BY startedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<PromptExecutionEntity>>

    @Query(
        "SELECT * FROM prompt_executions WHERE promptId = :promptId ORDER BY startedAt DESC LIMIT :limit"
    )
    fun getByPrompt(promptId: String, limit: Int = 30): Flow<List<PromptExecutionEntity>>

    @Query("SELECT * FROM prompt_executions WHERE id = :id")
    suspend fun getById(id: String): PromptExecutionEntity?

    @Query("SELECT * FROM prompt_executions WHERE status = :status ORDER BY startedAt DESC")
    fun getByStatus(status: ExecutionStatus): Flow<List<PromptExecutionEntity>>

    /**
     * Alle Ausfuehrungen die noch als RUNNING markiert sind. Beim App-Start vom
     * ExecutionLogger gepueft und als INTERRUPTED markiert (App-Crash-Erkennung).
     */
    @Query("SELECT * FROM prompt_executions WHERE status = 'RUNNING'")
    suspend fun getRunning(): List<PromptExecutionEntity>

    // Bugfix 2026-07-03 (Room-Almanach K1): @Upsert — REPLACE loeschte via CASCADE die
    // Execution-Steps bei jedem Status-Update der laufenden Ausfuehrung.
    @Upsert
    suspend fun upsert(execution: PromptExecutionEntity)

    @Update suspend fun update(execution: PromptExecutionEntity)

    @Delete suspend fun delete(execution: PromptExecutionEntity)

    /** Aelteste Audit-Eintraege loeschen wenn Tabelle zu gross wird. */
    @Query(
        "DELETE FROM prompt_executions WHERE id IN (SELECT id FROM prompt_executions ORDER BY startedAt ASC LIMIT :count)"
    )
    suspend fun pruneOldest(count: Int)

    @Query("SELECT COUNT(*) FROM prompt_executions") suspend fun count(): Int
}

/**
 * Feinkoernige Schritte innerhalb einer Ausfuehrung. Ein Step-Eintrag pro LLM_CALL,
 * TOOL_CALL, USER_CONFIRM und BLOCKED.
 */
@Dao
interface PromptExecutionStepDao {
    @Query(
        "SELECT * FROM prompt_execution_steps WHERE executionId = :executionId ORDER BY stepIndex ASC"
    )
    fun getByExecution(executionId: String): Flow<List<PromptExecutionStepEntity>>

    @Query(
        "SELECT * FROM prompt_execution_steps WHERE executionId = :executionId ORDER BY stepIndex ASC"
    )
    suspend fun getByExecutionSnapshot(executionId: String): List<PromptExecutionStepEntity>

    @Query("SELECT * FROM prompt_execution_steps WHERE id = :id")
    suspend fun getById(id: String): PromptExecutionStepEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(step: PromptExecutionStepEntity)

    @Update suspend fun update(step: PromptExecutionStepEntity)

    @Query("DELETE FROM prompt_execution_steps WHERE executionId = :executionId")
    suspend fun deleteByExecution(executionId: String)
}

/**
 * Tool-Freischaltungen pro Prompt. NUR Write-Tools brauchen Eintraege — Read-Tools
 * sind per Default fuer alle Prompts erlaubt.
 */
@Dao
interface PromptToolPermissionDao {
    @Query("SELECT * FROM prompt_tool_permissions WHERE promptId = :promptId")
    fun getByPrompt(promptId: String): Flow<List<PromptToolPermissionEntity>>

    @Query("SELECT * FROM prompt_tool_permissions WHERE promptId = :promptId")
    suspend fun getByPromptSnapshot(promptId: String): List<PromptToolPermissionEntity>

    @Query(
        "SELECT * FROM prompt_tool_permissions WHERE promptId = :promptId AND toolName = :toolName"
    )
    suspend fun getOne(promptId: String, toolName: String): PromptToolPermissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(permission: PromptToolPermissionEntity)

    @Update suspend fun update(permission: PromptToolPermissionEntity)

    @Delete suspend fun delete(permission: PromptToolPermissionEntity)

    @Query("DELETE FROM prompt_tool_permissions WHERE promptId = :promptId")
    suspend fun deleteByPrompt(promptId: String)
}

/**
 * Tages-Token-Statistik pro Prompt. Wird inkrementell vom ExecutionLogger gefuettert,
 * kann ueber den "Token-Stats neu berechnen"-Knopf in den Einstellungen aus
 * prompt_executions neu aufgebaut werden.
 */
@Dao
interface TokenUsageDailyDao {
    /** Alle Tage eines Prompts in chronologischer Reihenfolge (fuer Balkendiagramm). */
    @Query(
        "SELECT * FROM token_usage_daily WHERE promptId = :promptId AND day >= :sinceDay ORDER BY day ASC"
    )
    fun getHistoryForPrompt(promptId: String, sinceDay: String): Flow<List<TokenUsageDailyEntity>>

    /** Alle Prompts an einem bestimmten Tag (z.B. fuer "heute"-Uebersicht). */
    @Query("SELECT * FROM token_usage_daily WHERE day = :day ORDER BY tokensTotal DESC")
    fun getAllForDay(day: String): Flow<List<TokenUsageDailyEntity>>

    @Query(
        "SELECT * FROM token_usage_daily WHERE promptId = :promptId AND day = :day"
    )
    suspend fun getOne(promptId: String, day: String): TokenUsageDailyEntity?

    /** Heutige Tokens eines Prompts — vom TokenMeter vor jedem Run abgefragt. */
    @Query(
        "SELECT COALESCE(SUM(tokensTotal), 0) FROM token_usage_daily WHERE promptId = :promptId AND day = :day"
    )
    suspend fun getTotalForPromptOnDay(promptId: String, day: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(usage: TokenUsageDailyEntity)

    /**
     * Atomare Inkrement-Operation (Direktive 3 Loop-1-Fix, war MED-1-Bug):
     * Statt read-modify-write in 2 separaten Queries (race condition bei
     * parallelen Runs) wird der gesamte Upsert in EINEM SQLite-Statement
     * ausgefuehrt. Bei Konflikt auf der ID (= promptId_day) werden die Werte
     * der bestehenden Zeile inkrementiert.
     *
     * Vorsicht: erfordert SQLite >= 3.24 (Android API 29+ haben SQLite 3.28,
     * API 28 hat nur 3.22 → CRASH bei ON CONFLICT DO UPDATE). Loop-3-Fix:
     * TokenUsageRepository.addTokens faengt die SQLiteException und faellt
     * auf Mutex-geschuetztes Read-Modify-Write zurueck.
     */
    @Query(
        """
        INSERT INTO token_usage_daily (id, promptId, day, tokensInput, tokensOutput, tokensTotal, runCount)
        VALUES (:id, :promptId, :day, :tokensInput, :tokensOutput, :tokensInput + :tokensOutput, 1)
        ON CONFLICT(id) DO UPDATE SET
            tokensInput = tokensInput + :tokensInput,
            tokensOutput = tokensOutput + :tokensOutput,
            tokensTotal = tokensTotal + :tokensInput + :tokensOutput,
            runCount = runCount + 1
        """
    )
    suspend fun incrementUsage(
        id: String,
        promptId: String,
        day: String,
        tokensInput: Int,
        tokensOutput: Int,
    )

    @Query("DELETE FROM token_usage_daily") suspend fun deleteAll()

    @Query("DELETE FROM token_usage_daily WHERE promptId = :promptId")
    suspend fun deleteByPrompt(promptId: String)

    /** Alte Tage prunen (z.B. aelter als 365 Tage). */
    @Query("DELETE FROM token_usage_daily WHERE day < :beforeDay")
    suspend fun pruneOlderThan(beforeDay: String)
}

/**
 * Auto-Trigger-Konfiguration pro Prompt (Stufe 3). Vom TriggerScheduler gelesen
 * und gepflegt (nextScheduledAt, lastRunAt).
 */
@Dao
interface PromptTriggerDao {
    @Query("SELECT * FROM prompt_triggers WHERE promptId = :promptId")
    fun getByPrompt(promptId: String): Flow<List<PromptTriggerEntity>>

    @Query("SELECT * FROM prompt_triggers WHERE isActive = 1")
    suspend fun getAllActive(): List<PromptTriggerEntity>

    @Query("SELECT * FROM prompt_triggers WHERE id = :id")
    suspend fun getById(id: String): PromptTriggerEntity?

    /** Faellige Cron-Trigger fuer den TriggerScheduler. */
    @Query(
        "SELECT * FROM prompt_triggers WHERE isActive = 1 AND triggerType = 'CRON' AND nextScheduledAt IS NOT NULL AND nextScheduledAt <= :nowMillis"
    )
    suspend fun getDueCronTriggers(nowMillis: Long): List<PromptTriggerEntity>

    /** Event-Trigger fuer den EventDispatcher. */
    @Query(
        "SELECT * FROM prompt_triggers WHERE isActive = 1 AND triggerType = 'EVENT'"
    )
    suspend fun getAllEventTriggers(): List<PromptTriggerEntity>

    /** Ketten-Trigger fuer Trigger-Chaining nach Erfolg eines Prompts. */
    @Query(
        "SELECT * FROM prompt_triggers WHERE isActive = 1 AND triggerType = 'CHAIN' AND chainAfterPromptId = :sourcePromptId"
    )
    suspend fun getChainTriggersAfter(sourcePromptId: String): List<PromptTriggerEntity>

    @Query("SELECT * FROM prompt_triggers WHERE triggerType = :type AND isActive = 1")
    suspend fun getActiveByType(type: TriggerType): List<PromptTriggerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(trigger: PromptTriggerEntity)

    @Update suspend fun update(trigger: PromptTriggerEntity)

    @Delete suspend fun delete(trigger: PromptTriggerEntity)

    @Query("DELETE FROM prompt_triggers WHERE promptId = :promptId")
    suspend fun deleteByPrompt(promptId: String)

    /**
     * Direktive 3 Loop-2-Fix (war LOOP-2-2-Bug): chainAfterPromptId hat keinen
     * FK weil das logisch eine WEAK reference ist (Ziel-Prompt kann existieren,
     * muss aber nicht). Wenn der Source-Prompt geloescht wird, sind Chain-Trigger
     * orphaned. Dieser Cleanup loescht solche Trigger explizit nach Prompt-Delete.
     */
    @Query("DELETE FROM prompt_triggers WHERE chainAfterPromptId = :sourcePromptId")
    suspend fun deleteOrphanedChainTriggers(sourcePromptId: String)
}

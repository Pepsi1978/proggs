package de.frank.entropyreducer.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import de.frank.entropyreducer.domain.model.ConfirmDecision
import de.frank.entropyreducer.domain.model.ExecutionStatus
import de.frank.entropyreducer.domain.model.PromptCategory
import de.frank.entropyreducer.domain.model.StepType
import de.frank.entropyreducer.domain.model.TriggerSource
import de.frank.entropyreducer.domain.model.TriggerType

/**
 * Eine Zeile pro Prompt-Ausfuehrung. Enthaelt einen Snapshot des Prompts zum Zeitpunkt der
 * Ausfuehrung, damit der Audit-Eintrag eigenstaendig lesbar bleibt wenn der Prompt spaeter
 * geloescht oder veraendert wird.
 *
 * Bewusst KEIN ForeignKey auf saved_prompts(id) — Audit ueberlebt Prompt-Loeschung.
 * stattdessen INDEX auf promptId fuer Verlauf-Queries.
 */
@Entity(
    tableName = "prompt_executions",
    indices = [
        Index(value = ["promptId"]),
        Index(value = ["startedAt"]),
        Index(value = ["status"]),
    ],
)
data class PromptExecutionEntity(
    @PrimaryKey val id: String,
    val promptId: String,
    // Snapshot des Prompts zum Ausfuehrungs-Zeitpunkt (damit Audit nach Loeschung lesbar bleibt)
    val snapshotName: String,
    val snapshotContent: String,
    val snapshotCategory: PromptCategory,
    val snapshotModel: String,
    /** Optionaler zusaetzlicher Kontext den der Nutzer beim Ausfuehren tippt. */
    val userInputContext: String? = null,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val status: ExecutionStatus,
    /** Finale Text-Antwort von Gemini (oder null bei Fehler/Block). */
    val finalAnswer: String? = null,
    /** Fehler-Begruendung wenn status FAILED oder BLOCKED_BY_*. */
    val errorMessage: String? = null,
    val tokensInput: Int = 0,
    val tokensOutput: Int = 0,
    val tokensTotal: Int = 0,
    val toolCallCount: Int = 0,
    /**
     * Konkret verwendetes Modell (z.B. "gemini-2.5-pro"). Identisch mit snapshotModel
     * solange Frank's Modell-Wahl beim Run beachtet wurde.
     */
    val modelUsed: String,
    val triggerSource: TriggerSource = TriggerSource.MANUAL,
)

/**
 * Feinkoerniger Schritt innerhalb einer Prompt-Ausfuehrung (ReAct-Loop).
 *
 * Ein typischer Run hat die Sequenz:
 *   step 0: LLM_CALL (Gemini schlaegt ersten Tool-Call vor)
 *   step 1: TOOL_CALL (read_entropie_eintraege)
 *   step 2: LLM_CALL (Gemini formuliert naechsten Schritt)
 *   step 3: USER_CONFIRM (Nutzer bestaetigt Write-Tool)
 *   step 4: TOOL_CALL (create_aufgabe)
 *   step 5: LLM_CALL (Gemini gibt finale Antwort)
 *
 * Rollback wird ueber createdEntityIds/updatedEntityIds/deletedEntityIds ermoeglicht —
 * pro Step ist nachvollziehbar welche Daten erzeugt/geaendert wurden.
 */
@Entity(
    tableName = "prompt_execution_steps",
    foreignKeys = [
        ForeignKey(
            entity = PromptExecutionEntity::class,
            parentColumns = ["id"],
            childColumns = ["executionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["executionId"]),
        Index(value = ["executionId", "stepIndex"]),
    ],
)
data class PromptExecutionStepEntity(
    @PrimaryKey val id: String,
    val executionId: String,
    val stepIndex: Int,
    val stepType: StepType,
    val timestamp: Long,
    val toolName: String? = null,
    /** Serialisierte JSON-Args wie Gemini sie geliefert hat. */
    val toolArgsJson: String? = null,
    /** Serialisiertes Result oder Fehler. */
    val toolResultJson: String? = null,
    /** Bei LLM_CALL: Text-Ausgabe von Gemini fuer diesen Step. */
    val llmTextOutput: String? = null,
    val confirmDecision: ConfirmDecision? = null,
    val createdEntityIds: List<String> = emptyList(),
    val updatedEntityIds: List<String> = emptyList(),
    val deletedEntityIds: List<String> = emptyList(),
    val error: String? = null,
)

/**
 * Tool-Freischaltung pro Prompt. NUR Write-Tools brauchen einen Eintrag — Read-Tools sind
 * per Default fuer alle Prompts erlaubt (kategorie-uebergreifend, Frank-Entscheidung
 * 2026-05-21).
 *
 * trustMode = true bedeutet: dieser Prompt darf das Write-Tool ohne Confirm-Dialog
 * ausfuehren. Standardmaessig false — Sicherheit > Bequemlichkeit.
 */
@Entity(
    tableName = "prompt_tool_permissions",
    foreignKeys = [
        ForeignKey(
            entity = SavedPromptEntity::class,
            parentColumns = ["id"],
            childColumns = ["promptId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["promptId", "toolName"], unique = true),
        Index(value = ["promptId"]),
    ],
)
data class PromptToolPermissionEntity(
    @PrimaryKey val id: String,
    val promptId: String,
    val toolName: String,
    val granted: Boolean = false,
    val trustMode: Boolean = false,
)

/**
 * Tagesaggregierte Token-Statistik pro Prompt. Vom ExecutionLogger inkrementell
 * gefuettert — jeder Run-Abschluss macht einen Upsert. Existiert separat von
 * prompt_executions weil Charts mit GROUP BY ueber tausende Eintraege zu langsam
 * waeren.
 *
 * Drift-Schutz: in den Einstellungen gibt es einen "Token-Stats neu berechnen"-Knopf
 * der die Tabelle aus prompt_executions neu aufbaut. Falls die Aggregation jemals
 * von der Quelle abweicht.
 *
 * id-Format: "{promptId}_{day}" — kompositer Schluessel als String fuer einfaches Upsert.
 */
@Entity(
    tableName = "token_usage_daily",
    foreignKeys = [
        ForeignKey(
            entity = SavedPromptEntity::class,
            parentColumns = ["id"],
            childColumns = ["promptId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["promptId", "day"], unique = true),
        Index(value = ["day"]),
        Index(value = ["promptId"]),
    ],
)
data class TokenUsageDailyEntity(
    @PrimaryKey val id: String,
    val promptId: String,
    /** ISO-Datum "2026-05-21" in lokaler Zeitzone. */
    val day: String,
    val tokensInput: Int = 0,
    val tokensOutput: Int = 0,
    val tokensTotal: Int = 0,
    val runCount: Int = 0,
)

/**
 * Konfiguration fuer automatische Ausfuehrung eines Prompts (Stufe 3).
 *
 * Verwendung je nach triggerType:
 * - MANUAL: keine zusaetzlichen Felder noetig (Eintrag existiert nicht, oder isActive=false)
 * - CRON: `cronExpression` gesetzt, `nextScheduledAt` vom TriggerScheduler gepflegt
 * - EVENT: `eventCondition` als DSL-String (z.B. "new_entropie_entries:24h:>3")
 * - CHAIN: `chainAfterPromptId` zeigt auf den Trigger-Prompt
 */
@Entity(
    tableName = "prompt_triggers",
    foreignKeys = [
        ForeignKey(
            entity = SavedPromptEntity::class,
            parentColumns = ["id"],
            childColumns = ["promptId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["promptId"]),
        Index(value = ["nextScheduledAt"]),
        Index(value = ["chainAfterPromptId"]),
        Index(value = ["isActive"]),
    ],
)
data class PromptTriggerEntity(
    @PrimaryKey val id: String,
    val promptId: String,
    val triggerType: TriggerType,
    val cronExpression: String? = null,
    val eventCondition: String? = null,
    val chainAfterPromptId: String? = null,
    val isActive: Boolean = true,
    val lastRunAt: Long? = null,
    val nextScheduledAt: Long? = null,
)

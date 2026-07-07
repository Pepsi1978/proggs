package de.frank.entropyreducer.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.frank.entropyreducer.domain.model.MemorySource
import de.frank.entropyreducer.domain.model.PromptCategory

@Entity(tableName = "saved_prompts")
data class SavedPromptEntity(
    @PrimaryKey val id: String,
    val name: String,
    val content: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    /**
     * Kategorie des Prompts (Frank-Wunsch 2026-05-20). Prompts wirken nur in ihrem Bereich —
     * "Aufgaben"-Prompts beeinflussen z.B. nur ProcessEntryUseCase. Default = AUFGABEN, weil das
     * vor der Kategorisierung der Standardfall war.
     *
     * Ab Agentic-AI (Frank-Wunsch 2026-05-21) wird die Kategorie NUR noch fuers UI-Sortieren
     * verwendet, nicht mehr fuer Tool-Permissions — die laufen per prompt_tool_permissions
     * fein-granular pro Prompt.
     */
    val category: PromptCategory = PromptCategory.AUFGABEN,
    /**
     * Vom Nutzer gewaehltes Gemini-Modell fuer agentic-AI-Ausfuehrungen.
     * Beispiele: "gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.5-flash-lite".
     * Default = Gemini 3.1 Flash Lite (Frank-Wunsch 2026-05-25: app-weites
     * Standard-Modell, schnell und guenstig).
     * Frank-Entscheidung 2026-05-21: kein Auto-Selector, immer manuell pro Prompt.
     */
    val model: String = "gemini-3.1-flash-lite",
    /**
     * Optionales Tages-Token-Limit fuer diesen Prompt. null = kein Limit. Bei
     * Ueberschreitung blockiert der WorkflowRunner weitere Aufrufe mit Status
     * BLOCKED_BY_TOKEN_LIMIT. Frank-Wunsch 2026-05-21: Sichtbarkeit ueber alle
     * Prompts in den Einstellungen + optionales hartes Limit pro Prompt.
     */
    val tokenLimitPerDay: Int? = null,
    /**
     * Trust-Modus: wenn true, fuehrt der Prompt seine freigeschalteten Write-Tools
     * OHNE Confirm-Dialog aus. Default = false (Sicherheit). Wird vom
     * ConfirmationGate ueber prompt_tool_permissions.trustMode UND dieses
     * Default-Flag entschieden — beides true = kein Dialog.
     */
    val trustModeDefault: Boolean = false,
)

@Entity(tableName = "memory_entries")
data class MemoryEntryEntity(
    @PrimaryKey val id: String,
    val content: String,
    val source: MemorySource,
    val isActive: Boolean,
    val confidence: Int, // 0..100
    val createdAt: Long,
    val updatedAt: Long,
)

/** KI-synthetisierte Codex-Version (Sonntag 19:00 + manuell). */
@Entity(tableName = "genie_codex_versions")
data class GenieCodexVersionEntity(
    @PrimaryKey val id: String,
    val content: String,
    val createdAt: Long,
    val basedOnEntryIds: List<String>,
    val basedOnInsightIds: List<String>,
    val basedOnMemoryIds: List<String>,
)

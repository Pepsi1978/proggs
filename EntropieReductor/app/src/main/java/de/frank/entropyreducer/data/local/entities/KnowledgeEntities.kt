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
     */
    val category: PromptCategory = PromptCategory.AUFGABEN,
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

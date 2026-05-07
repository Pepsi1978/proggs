package de.frank.entropyreducer.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.frank.entropyreducer.domain.model.MemorySource

@Entity(tableName = "saved_prompts")
data class SavedPromptEntity(
    @PrimaryKey val id: String,
    val name: String,
    val content: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "memory_entries")
data class MemoryEntryEntity(
    @PrimaryKey val id: String,
    val content: String,
    val source: MemorySource,
    val isActive: Boolean,
    val confidence: Int,                     // 0..100
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

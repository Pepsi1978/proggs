package de.frank.entropyreducer.data.local.journalmirror

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Gespiegelter Tagebucheintrag aus BestJournal Frank (Frank-Wunsch 2026-05-24).
 * sourceId = id der Quell-DB (journal_entries.id). Read-only-Kopie.
 */
@Entity(tableName = "journal_mirror_entries")
data class JournalMirrorEntryEntity(
    @PrimaryKey val sourceId: Long,
    val timestamp: Long,
    val title: String?,
    val displayText: String,
    val rawText: String,
    val improvedText: String?,
    val isImproved: Boolean,
    val summary: String?,
)

/**
 * Gespiegelter Nachtrag. sourceId = id aus entry_follow_ups, entryId = zugehoeriger
 * Eintrag (journal_entries.id). Read-only-Kopie.
 */
@Entity(
    tableName = "journal_mirror_followups",
    indices = [Index("entryId")],
)
data class JournalMirrorFollowupEntity(
    @PrimaryKey val sourceId: Long,
    val entryId: Long,
    val createdAt: Long,
    val text: String,
    val rawText: String,
    val improvedText: String?,
    val isImproved: Boolean,
)

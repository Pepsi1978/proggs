package de.frank.entropyreducer.data.local.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Nachtrag zu einem [EntropyEntryEntity] (Frank-Wunsch 2026-05-20): Im Vollbild-Detail-Screen
 * sollen Nachträge wie in BestJournalFrank als eigene Karten erscheinen — mit Datum, Original-Text
 * und optional KI-verbessertem Text. Vorher wurden Nachträge bloss als Text an `description`
 * angehängt; das verlor die Struktur (Zeitstempel, Verbessert/Original-Tab pro Nachtrag).
 *
 * Foreign Key auf entropy_entries.id mit CASCADE — wenn der Eintrag gelöscht wird, fliegen seine
 * Nachträge automatisch mit.
 */
@Immutable
@Entity(
    tableName = "entropy_entry_followups",
    foreignKeys =
        [
            ForeignKey(
                entity = EntropyEntryEntity::class,
                parentColumns = ["id"],
                childColumns = ["entryId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("entryId"), Index(value = ["entryId", "createdAt"])],
)
data class EntropyEntryFollowupEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    /** Originaltext wie er eingesprochen wurde (Whisper-Transkript). */
    val rawText: String,
    /** Optional KI-verbesserte Fassung. Wenn null → es gibt nur das Original. */
    val improvedText: String?,
    /** True wenn der Benutzer auf "Mit KI verbessern" gedrückt hat. */
    val isImproved: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

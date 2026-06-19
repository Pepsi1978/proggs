package de.frank.entropyreducer.data.local.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Nachtrag zu einer [IdeaEntity] (ID-Architektur Etappe 2, Frank-Wunsch 2026-06-19).
 *
 * Entspricht der bisherigen `IdeenFollowup` (vorher im JSON der Idee verschachtelt). Die Herkunft
 * ist `ideaId` (FK auf ideas.id, CASCADE) — ein Nachtrag haengt an seiner Idee, deshalb braucht er
 * KEINE eigenen origin/root-Felder (analog entropy_entry_followups.entryId).
 */
@Immutable
@Entity(
    tableName = "idea_followups",
    foreignKeys =
        [
            ForeignKey(
                entity = IdeaEntity::class,
                parentColumns = ["id"],
                childColumns = ["ideaId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("ideaId"), Index(value = ["ideaId", "createdAtMs"])],
)
data class IdeaFollowupEntity(
    @PrimaryKey val id: String,
    val ideaId: String,
    val createdAtMs: Long,
    val text: String,
    val improvedText: String? = null,
    val isImproved: Boolean = false,
)

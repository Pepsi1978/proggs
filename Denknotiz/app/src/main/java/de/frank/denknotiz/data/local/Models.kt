package de.frank.denknotiz.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class EntryType { NOTE, AI_RESPONSE }
enum class SnapshotStatus { RUNNING, FAILED, COMPLETED }

@Entity(tableName = "sessions", indices = [Index("updatedAt")])
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val titleManual: Boolean = false,
    val titleGenerated: Boolean = false,
)

@Entity(
    tableName = "entries",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId"), Index(value = ["sessionId", "ordinal"], unique = true), Index("snapshotId")],
)
data class EntryEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val ordinal: Long,
    val type: EntryType,
    val title: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
    val snapshotId: String? = null,
    val historical: Boolean = false,
    val citationsJson: String = "[]",
    val titleManual: Boolean = false,
    val titleGenerated: Boolean = false,
    val originalText: String? = null,
)

@Entity(
    tableName = "evaluation_snapshots",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId")],
)
data class EvaluationSnapshotEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val lowerOrdinalExclusive: Long,
    val upperOrdinalInclusive: Long,
    val sourceNoteIdsJson: String,
    val focusQuestion: String,
    val profileId: String,
    val webEnabled: Boolean,
    val model: String,
    val reasoning: String,
    val chunkCount: Int,
    val status: SnapshotStatus,
    val error: String = "",
    val createdAt: Long,
    val completedAt: Long? = null,
    val sourceNotesJson: String = "[]",
    val profileInstruction: String = "",
)

@Entity(
    tableName = "context_boundaries",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class ContextBoundaryEntity(
    @PrimaryKey val sessionId: String,
    val lastIncludedOrdinal: Long = 0,
    val lastResponseId: String? = null,
    val updatedAt: Long,
)

data class SessionBundle(
    val session: SessionEntity,
    val entries: List<EntryEntity>,
    val snapshots: List<EvaluationSnapshotEntity>,
    val boundary: ContextBoundaryEntity?,
)

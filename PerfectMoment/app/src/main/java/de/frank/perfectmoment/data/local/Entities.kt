package de.frank.perfectmoment.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "sessions",
    indices = [Index(value = ["startedAt"])],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,
    val startedAt: Long,
    val durationMin: Int,
    val voiceName: String,
    val providerId: String,
    val pauseRep: Int,
    val pauseNext: Int,
    val reps: Int,
    val questionCount: Int = 0,
    val introContext: String = "",
    val entranceQuestion: String = "",
    val resumeQuestionIndex: Int? = null,
    val resumeRepetition: Int? = null,
    val resumeRemainingMs: Long? = null,
    @ColumnInfo(defaultValue = "1") val playCount: Int = 1,
    @ColumnInfo(defaultValue = "0") val lastPlayedAt: Long = startedAt,
    /** The short title the history list shows instead of the whole wish. Empty until written. */
    @ColumnInfo(defaultValue = "") val summary: String = "",
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "orderIndex"], unique = true),
    ],
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val orderIndex: Int,
    val emoji: String,
    val text: String,
)

@Entity(
    tableName = "skills",
    indices = [Index(value = ["createdAt"])],
)
data class SkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val text: String,
    val createdAt: Long,
)

@Entity(
    tableName = "hooks",
    indices = [Index(value = ["sortIndex"], unique = true)],
)
data class HookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emoji: String,
    val text: String,
    val sortIndex: Int,
)

data class SessionWithQuestions(
    @Embedded val session: SessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val questions: List<QuestionEntity>,
) {
    fun sorted(): SessionWithQuestions = copy(questions = questions.sortedBy(QuestionEntity::orderIndex))
}

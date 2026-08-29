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
    /** Frage-IDs in der laufenden Abspielreihenfolge; hält zufällige eigene Sessions fortsetzbar. */
    @ColumnInfo(defaultValue = "") val resumeQuestionOrder: String = "",
    @ColumnInfo(defaultValue = "1") val playCount: Int = 1,
    @ColumnInfo(defaultValue = "0") val lastPlayedAt: Long = startedAt,
    /** The short title the history list shows instead of the whole wish. Empty until written. */
    @ColumnInfo(defaultValue = "") val summary: String = "",
    /** True once the title was typed by hand. The AI then never renames the entry again. */
    @ColumnInfo(defaultValue = "0") val summaryManual: Boolean = false,
    /** The voice this entry always plays with. Empty means the one from the settings. */
    @ColumnInfo(defaultValue = "") val voiceProviderOverride: String = "",
    @ColumnInfo(defaultValue = "") val voiceOverride: String = "",
    /**
     * True for a reading flow whose questions were typed by hand instead of written by the AI.
     * Such an entry lives in its own section of the history and never asks OpenAI for refills.
     */
    @ColumnInfo(defaultValue = "0") val custom: Boolean = false,
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

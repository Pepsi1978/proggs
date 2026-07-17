package de.frank.karteikartenlernen.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "study_sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val count: Int,
    val known: Int,
    val dateLabel: String,
    val accent: Int,
)

@Entity(tableName = "researches")
data class ResearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val question: String,
    val answer: String,
    val createdAt: Long,
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val researchId: Long,
    val question: String,
    val answer: String,
    val explanation: String,
    val status: String,
)

@Entity(tableName = "learning_results")
data class LearningResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val flashcardId: Long,
    val known: Boolean,
    val reviewedAt: Long,
)

data class SessionContextRow(
    val id: String,
    val title: String,
    val question: String,
    val answerExcerpt: String,
)

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_sessions ORDER BY createdAt DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM flashcards WHERE sessionId = :sessionId ORDER BY id")
    fun observeCards(sessionId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM researches WHERE sessionId = :sessionId ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestResearch(sessionId: String): ResearchEntity?

    @Query("SELECT COUNT(*) FROM study_sessions")
    suspend fun sessionCount(): Int

    @Query("""
        SELECT s.id, s.title,
            COALESCE((SELECT r.question FROM researches r WHERE r.sessionId = s.id ORDER BY r.createdAt DESC LIMIT 1), '') AS question,
            COALESCE((SELECT SUBSTR(r.answer, 1, 600) FROM researches r WHERE r.sessionId = s.id ORDER BY r.createdAt DESC LIMIT 1), '') AS answerExcerpt
        FROM study_sessions s
        ORDER BY s.createdAt DESC
    """)
    suspend fun sessionContexts(): List<SessionContextRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SessionEntity)

    @Insert
    suspend fun insertResearch(research: ResearchEntity): Long

    @Insert
    suspend fun insertCards(cards: List<FlashcardEntity>): List<Long>

    @Query("SELECT * FROM flashcards WHERE id IN (:ids) ORDER BY id")
    suspend fun cardsByIds(ids: List<Long>): List<FlashcardEntity>

    @Transaction
    suspend fun copyCardsToSession(sourceCardIds: List<Long>, targetSessionId: String): Int {
        val sourceCards = cardsByIds(sourceCardIds.distinct())
        if (sourceCards.isEmpty()) return 0
        insertCards(sourceCards.map { card ->
            card.copy(id = 0, sessionId = targetSessionId, status = "NEW")
        })
        addCardCount(targetSessionId, sourceCards.size)
        return sourceCards.size
    }

    @Transaction
    suspend fun insertResearchBundle(
        session: SessionEntity,
        research: ResearchEntity,
        cards: List<FlashcardEntity>,
    ): List<Long> {
        upsertSession(session)
        val researchId = insertResearch(research)
        return insertCards(cards.map { it.copy(researchId = researchId) })
    }

    @Insert
    suspend fun insertLearningResult(result: LearningResultEntity)

    @Query("UPDATE flashcards SET status = :status WHERE id = :id")
    suspend fun updateCardStatus(id: Long, status: String)

    @Query("UPDATE study_sessions SET known = known + 1 WHERE id = :sessionId AND known < count")
    suspend fun incrementKnown(sessionId: String)

    @Query("UPDATE study_sessions SET known = 0 WHERE id = :sessionId")
    suspend fun clearKnown(sessionId: String)

    @Query("UPDATE study_sessions SET count = count + :amount WHERE id = :sessionId")
    suspend fun addCardCount(sessionId: String, amount: Int)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteCard(id: Long)

    @Query("UPDATE flashcards SET status = 'NEW' WHERE sessionId = :sessionId")
    suspend fun resetSession(sessionId: String)
}

@Database(
    entities = [SessionEntity::class, ResearchEntity::class, FlashcardEntity::class, LearningResultEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "karteikarten.db",
            ).build().also { instance = it }
        }
    }
}

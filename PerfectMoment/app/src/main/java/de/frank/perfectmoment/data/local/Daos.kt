package de.frank.perfectmoment.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    abstract fun observeSessions(): Flow<List<SessionEntity>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    abstract fun observeSession(sessionId: Long): Flow<SessionWithQuestions?>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    abstract suspend fun getSession(sessionId: Long): SessionWithQuestions?

    @Insert
    abstract suspend fun insertSession(session: SessionEntity): Long

    @Update
    abstract suspend fun updateSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    abstract suspend fun deleteSession(sessionId: Long): Int

    @Query(
        "UPDATE sessions SET playCount = playCount + 1, lastPlayedAt = :playedAt " +
            "WHERE id = :sessionId",
    )
    abstract suspend fun markPlayed(sessionId: Long, playedAt: Long)

    @Insert
    abstract suspend fun insertQuestions(questions: List<QuestionEntity>): List<Long>

    @Query("SELECT COALESCE(MAX(orderIndex) + 1, 0) FROM questions WHERE sessionId = :sessionId")
    abstract suspend fun nextQuestionIndex(sessionId: Long): Int

    @Query("SELECT COUNT(*) FROM questions WHERE sessionId = :sessionId")
    abstract suspend fun questionCount(sessionId: Long): Int

    @Query("UPDATE sessions SET questionCount = :questionCount WHERE id = :sessionId")
    abstract suspend fun updateQuestionCount(sessionId: Long, questionCount: Int)

    @Query("DELETE FROM questions WHERE sessionId = :sessionId")
    abstract suspend fun deleteQuestions(sessionId: Long)

    @Query(
        """UPDATE sessions SET
            durationMin = :durationMin,
            pauseRep = :pauseRep,
            pauseNext = :pauseNext,
            reps = :reps,
            questionCount = :questionCount,
            resumeQuestionIndex = :questionIndex,
            resumeRepetition = :repetition,
            resumeRemainingMs = :remainingMs
            WHERE id = :sessionId""",
    )
    abstract suspend fun saveProgress(
        sessionId: Long,
        durationMin: Int,
        pauseRep: Int,
        pauseNext: Int,
        reps: Int,
        questionCount: Int,
        questionIndex: Int,
        repetition: Int,
        remainingMs: Long,
    )

    @Query(
        """UPDATE sessions SET
            resumeQuestionIndex = NULL,
            resumeRepetition = NULL,
            resumeRemainingMs = NULL
            WHERE id = :sessionId""",
    )
    abstract suspend fun clearProgress(sessionId: Long)
}

@Dao
abstract class SkillDao {
    @Query("SELECT * FROM skills ORDER BY createdAt ASC, id ASC")
    abstract fun observeSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE id = :skillId")
    abstract fun observeSkill(skillId: Long): Flow<SkillEntity?>

    @Query("SELECT * FROM skills WHERE id = :skillId")
    abstract suspend fun getSkill(skillId: Long): SkillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSkill(skill: SkillEntity): Long

    @Delete
    abstract suspend fun deleteSkill(skill: SkillEntity)
}

@Dao
abstract class HookDao {
    @Query("SELECT * FROM hooks ORDER BY sortIndex ASC, id ASC")
    abstract fun observeHooks(): Flow<List<HookEntity>>

    @Query("SELECT * FROM hooks WHERE id = :hookId")
    abstract suspend fun getHook(hookId: Long): HookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertHook(hook: HookEntity): Long

    @Delete
    abstract suspend fun deleteHook(hook: HookEntity)

    @Query("UPDATE hooks SET sortIndex = :sortIndex WHERE id = :hookId")
    protected abstract suspend fun setSortIndex(hookId: Long, sortIndex: Int)

    @Query("SELECT id FROM hooks")
    protected abstract suspend fun getHookIds(): List<Long>

    @Transaction
    open suspend fun reorder(hookIds: List<Long>) {
        require(hookIds.size == hookIds.distinct().size && hookIds.toSet() == getHookIds().toSet()) {
            "Hook order must contain every hook exactly once"
        }
        hookIds.forEachIndexed { index, hookId -> setSortIndex(hookId, -index - 1) }
        hookIds.forEachIndexed { index, hookId -> setSortIndex(hookId, index) }
    }
}

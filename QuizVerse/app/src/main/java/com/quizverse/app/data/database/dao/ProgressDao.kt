package com.quizverse.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quizverse.app.data.database.entities.UserProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    // --- Read operations ---

    /** Observe the single UserProgress row; emits null when the table is empty. */
    @Query("SELECT * FROM user_progress LIMIT 1")
    fun getProgress(): Flow<UserProgress?>

    // --- Write operations ---

    /** Insert or replace the progress row (table is designed for exactly one row). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: UserProgress)

    /** Update XP and derived level after a completed game session. */
    @Query(
        "UPDATE user_progress " +
        "SET totalXp = :totalXp, currentLevel = :currentLevel " +
        "WHERE id = (SELECT id FROM user_progress LIMIT 1)"
    )
    suspend fun updateXp(totalXp: Int, currentLevel: Int)

    /** Update both the current and all-time-best streak values. */
    @Query(
        "UPDATE user_progress " +
        "SET currentStreak = :currentStreak, longestStreak = :longestStreak " +
        "WHERE id = (SELECT id FROM user_progress LIMIT 1)"
    )
    suspend fun updateStreak(currentStreak: Int, longestStreak: Int)

    /** Update cumulative answer stats and total play time. */
    @Query(
        "UPDATE user_progress " +
        "SET totalQuestionsAnswered = :totalAnswered, " +
        "    totalCorrectAnswers    = :totalCorrect, " +
        "    totalPlayTime          = :totalPlayTime " +
        "WHERE id = (SELECT id FROM user_progress LIMIT 1)"
    )
    suspend fun updateStats(totalAnswered: Int, totalCorrect: Int, totalPlayTime: Long)

    /** Record the daily challenge streak and the date it was last completed. */
    @Query(
        "UPDATE user_progress " +
        "SET dailyChallengeStreak    = :streak, " +
        "    lastDailyChallengeDate  = :date " +
        "WHERE id = (SELECT id FROM user_progress LIMIT 1)"
    )
    suspend fun updateDailyChallenge(streak: Int, date: String)
}

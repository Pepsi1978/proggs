package com.quizverse.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quizverse.app.data.database.entities.HighScore
import kotlinx.coroutines.flow.Flow

@Dao
interface HighScoreDao {

    // --- Read operations ---

    /** Observe all recorded high scores ordered from highest to lowest score. */
    @Query("SELECT * FROM high_scores ORDER BY score DESC")
    fun getHighScores(): Flow<List<HighScore>>

    /** Observe high scores filtered by a specific game mode. */
    @Query("SELECT * FROM high_scores WHERE gameMode = :gameMode ORDER BY score DESC")
    fun getHighScoresByMode(gameMode: String): Flow<List<HighScore>>

    /** Observe the single best score entry for a given game mode; null if none exists. */
    @Query("SELECT * FROM high_scores WHERE gameMode = :gameMode ORDER BY score DESC LIMIT 1")
    fun getTopScore(gameMode: String): Flow<HighScore?>

    // --- Write operations ---

    /** Persist a new high score entry. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(highScore: HighScore)

    /** Remove every high score record (e.g. on user data reset). */
    @Query("DELETE FROM high_scores")
    suspend fun deleteAll()
}

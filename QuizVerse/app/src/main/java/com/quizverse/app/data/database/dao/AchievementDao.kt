package com.quizverse.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quizverse.app.data.database.entities.Achievement
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    // --- Read operations ---

    /** Observe the full achievement list ordered by tier (Gold first) then by id. */
    @Query("SELECT * FROM achievements ORDER BY tier DESC, id ASC")
    fun getAllAchievements(): Flow<List<Achievement>>

    /** Observe only achievements that have been unlocked, newest first. */
    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedDate DESC")
    fun getUnlockedAchievements(): Flow<List<Achievement>>

    /** Observe a single achievement by its stable string id; emits null if not found. */
    @Query("SELECT * FROM achievements WHERE id = :id")
    fun getAchievementById(id: String): Flow<Achievement?>

    // --- Write operations ---

    /** Bulk-insert the initial achievement definitions; existing rows are replaced. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<Achievement>)

    /** Update the player's progress toward a specific achievement. */
    @Query("UPDATE achievements SET currentValue = :currentValue WHERE id = :id")
    suspend fun updateProgress(id: String, currentValue: Int)

    /** Mark an achievement as unlocked and record the unlock date. */
    @Query(
        "UPDATE achievements " +
        "SET isUnlocked = 1, unlockedDate = :date " +
        "WHERE id = :id"
    )
    suspend fun unlockAchievement(id: String, date: String)
}

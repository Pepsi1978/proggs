package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.frank.entropyreducer.data.local.entities.HabitSuggestionEntity
import kotlinx.coroutines.flow.Flow

/** DAO fuer Gewohnheits-Vorschlaege (Tabelle habit_suggestions). ID-Architektur Etappe 3. */
@Dao
interface HabitSuggestionDao {
    @Query("SELECT * FROM habit_suggestions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<HabitSuggestionEntity>>

    @Query("SELECT * FROM habit_suggestions")
    suspend fun getAllForBackup(): List<HabitSuggestionEntity>

    @Query("SELECT * FROM habit_suggestions WHERE id = :id")
    suspend fun getById(id: String): HabitSuggestionEntity?

    @Query("SELECT COUNT(*) FROM habit_suggestions")
    suspend fun count(): Int

    /** Existiert bereits ein Vorschlag, der aus dieser Quell-Idee stammt? (Dedup ueber die Kette.) */
    @Query("SELECT COUNT(*) FROM habit_suggestions WHERE originId = :originId")
    suspend fun countByOriginId(originId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(suggestion: HabitSuggestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(suggestions: List<HabitSuggestionEntity>)

    @Update suspend fun update(suggestion: HabitSuggestionEntity)

    @Query("DELETE FROM habit_suggestions WHERE id = :id")
    suspend fun deleteById(id: String)
}

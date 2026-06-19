package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.frank.entropyreducer.data.local.entities.TaskSuggestionEntity
import kotlinx.coroutines.flow.Flow

/** DAO fuer Aufgaben-Vorschlaege (Tabelle task_suggestions). ID-Architektur Etappe 2. */
@Dao
interface TaskSuggestionDao {
    @Query("SELECT * FROM task_suggestions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TaskSuggestionEntity>>

    @Query("SELECT * FROM task_suggestions")
    suspend fun getAllForBackup(): List<TaskSuggestionEntity>

    @Query("SELECT * FROM task_suggestions WHERE id = :id")
    suspend fun getById(id: String): TaskSuggestionEntity?

    @Query("SELECT COUNT(*) FROM task_suggestions")
    suspend fun count(): Int

    /** Existiert bereits ein Vorschlag, der aus dieser Quell-Idee stammt? (Dedup ueber die Kette.) */
    @Query("SELECT COUNT(*) FROM task_suggestions WHERE originId = :originId")
    suspend fun countByOriginId(originId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(suggestion: TaskSuggestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(suggestions: List<TaskSuggestionEntity>)

    @Update suspend fun update(suggestion: TaskSuggestionEntity)

    @Query("DELETE FROM task_suggestions WHERE id = :id")
    suspend fun deleteById(id: String)
}

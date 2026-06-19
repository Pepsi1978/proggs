package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.frank.entropyreducer.data.local.entities.HabitEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO fuer Gewohnheiten (Tabelle habits). ID-Architektur Etappe 3.
 *
 * Die manuelle Reihenfolge liegt in `position` (Drag-and-Drop) — alle Lese-Queries sortieren danach.
 */
@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY position ASC")
    fun getAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY position ASC")
    suspend fun getAllForBackup(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: String): HabitEntity?

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun count(): Int

    /** Hoechste vergebene Position (oder -1, wenn leer) — fuer "neue Gewohnheit ans Ende". */
    @Query("SELECT COALESCE(MAX(position), -1) FROM habits")
    suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(habits: List<HabitEntity>)

    @Update suspend fun update(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteById(id: String)
}

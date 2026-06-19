package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity
import kotlinx.coroutines.flow.Flow

/** DAO fuer das Prioritaets-Gedaechtnis (Tabelle priority_memory). */
@Dao
interface PriorityMemoryDao {
    @Query("SELECT * FROM priority_memory ORDER BY priority DESC, updatedAt DESC")
    fun getAll(): Flow<List<PriorityMemoryEntity>>

    /** Neueste N Eintraege fuer den Gemini-Kontext (einstellbares Limit). */
    @Query("SELECT * FROM priority_memory ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getNewest(limit: Int): List<PriorityMemoryEntity>

    /** Gesamtzahl der Eintraege — fuer die Limit-Warnung in der UI. */
    @Query("SELECT COUNT(*) FROM priority_memory")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM priority_memory WHERE id = :id")
    suspend fun getById(id: String): PriorityMemoryEntity?

    @Query("SELECT * FROM priority_memory WHERE sourceEntryId = :sourceEntryId LIMIT 1")
    suspend fun getBySourceEntryId(sourceEntryId: String): PriorityMemoryEntity?

    @Query("SELECT * FROM priority_memory")
    suspend fun getAllForBackup(): List<PriorityMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: PriorityMemoryEntity)

    @Update suspend fun update(memory: PriorityMemoryEntity)

    @Query("DELETE FROM priority_memory WHERE id = :id")
    suspend fun deleteById(id: String)
}

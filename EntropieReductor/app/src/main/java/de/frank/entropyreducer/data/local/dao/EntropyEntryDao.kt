package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import kotlinx.coroutines.flow.Flow

@Dao
interface EntropyEntryDao {

    @Query("SELECT * FROM entropy_entries WHERE id = :id")
    suspend fun getById(id: String): EntropyEntryEntity?

    @Query("SELECT * FROM entropy_entries WHERE status != :archived ORDER BY priorityScore DESC, createdAt DESC")
    fun getActive(archived: EntryStatus = EntryStatus.ARCHIVIERT): Flow<List<EntropyEntryEntity>>

    @Query("SELECT * FROM entropy_entries WHERE timeBucket = :bucket AND status != :archived ORDER BY priorityScore DESC")
    fun getByTimeBucket(bucket: TimeBucket, archived: EntryStatus = EntryStatus.ARCHIVIERT): Flow<List<EntropyEntryEntity>>

    @Query("SELECT * FROM entropy_entries WHERE category = :cat AND status != :archived ORDER BY priorityScore DESC")
    fun getByCategory(cat: EntropyCategory, archived: EntryStatus = EntryStatus.ARCHIVIERT): Flow<List<EntropyEntryEntity>>

    @Query("SELECT * FROM entropy_entries WHERE status = :resolved AND resolvedAt >= :sinceMillis ORDER BY resolvedAt DESC")
    fun getRecentlyResolved(resolved: EntryStatus = EntryStatus.REDUZIERT, sinceMillis: Long): Flow<List<EntropyEntryEntity>>

    @Query("SELECT * FROM entropy_entries WHERE status = :archived ORDER BY resolvedAt DESC, updatedAt DESC")
    fun getArchived(archived: EntryStatus = EntryStatus.ARCHIVIERT): Flow<List<EntropyEntryEntity>>

    @Query("SELECT * FROM entropy_entries WHERE status = :resolved AND resolvedAt < :beforeMillis")
    suspend fun getResolvedBefore(resolved: EntryStatus = EntryStatus.REDUZIERT, beforeMillis: Long): List<EntropyEntryEntity>

    @Query("SELECT * FROM entropy_entries WHERE status != :archived ORDER BY priorityScore DESC")
    fun getByPriorityDesc(archived: EntryStatus = EntryStatus.ARCHIVIERT): Flow<List<EntropyEntryEntity>>

    @Query("SELECT COUNT(*) FROM entropy_entries WHERE status = :status")
    fun countByStatus(status: EntryStatus): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: EntropyEntryEntity)

    @Update
    suspend fun update(entry: EntropyEntryEntity)

    @Delete
    suspend fun delete(entry: EntropyEntryEntity)

    @Query("DELETE FROM entropy_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM entropy_entries")
    suspend fun deleteAll()
}

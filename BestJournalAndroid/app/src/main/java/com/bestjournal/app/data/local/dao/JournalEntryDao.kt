package com.bestjournal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bestjournal.app.data.local.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: Long): JournalEntryEntity?

    @Query(
        "SELECT * FROM journal_entries WHERE displayText LIKE '%' || :query || '%' OR rawText LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' ORDER BY timestamp DESC"
    )
    fun search(query: String): Flow<List<JournalEntryEntity>>

    @Query(
        "SELECT * FROM journal_entries WHERE adviceCategoryTags LIKE '%' || :category || '%' ORDER BY timestamp DESC"
    )
    fun filterByCategory(category: String): Flow<List<JournalEntryEntity>>

    @Query(
        "SELECT * FROM journal_entries WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC"
    )
    fun filterByTimeRange(startTime: Long, endTime: Long): Flow<List<JournalEntryEntity>>

    @Query("SELECT COUNT(*) FROM journal_entries") suspend fun getEntryCount(): Int

    @Query("SELECT * FROM journal_entries WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<JournalEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntryEntity): Long

    @Update suspend fun update(entry: JournalEntryEntity)

    @Delete suspend fun delete(entry: JournalEntryEntity)

    @Query("UPDATE journal_entries SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
}

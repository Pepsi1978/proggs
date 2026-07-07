package com.bestjournal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.bestjournal.app.data.local.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: Long): JournalEntryEntity?

    @Query(
        "SELECT * FROM journal_entries " +
            "WHERE displayText LIKE '%' || :query || '%' " +
            "OR rawText LIKE '%' || :query || '%' " +
            "OR title LIKE '%' || :query || '%' " +
            "OR summary LIKE '%' || :query || '%' " +
            "OR followUpText LIKE '%' || :query || '%' " +
            "OR EXISTS (" +
            "SELECT 1 FROM entry_follow_ups " +
            "WHERE entry_follow_ups.entryId = journal_entries.id " +
            "AND entry_follow_ups.text LIKE '%' || :query || '%'" +
            ") " +
            "ORDER BY timestamp DESC"
    )
    fun search(query: String): Flow<List<JournalEntryEntity>>

    @Query(
        """
        SELECT * FROM journal_entries
        WHERE ',' || COALESCE(adviceCategoryTags, '') || ',' LIKE '%,' || :category || ',%'
        ORDER BY timestamp DESC
        """
    )
    fun filterByCategory(category: String): Flow<List<JournalEntryEntity>>

    @Query(
        "SELECT * FROM journal_entries WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC"
    )
    fun filterByTimeRange(startTime: Long, endTime: Long): Flow<List<JournalEntryEntity>>

    @Query("SELECT COUNT(*) FROM journal_entries") suspend fun getEntryCount(): Int

    @Query("SELECT MIN(timestamp) FROM journal_entries") suspend fun getEarliestTimestamp(): Long?

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    suspend fun getAllEntriesOnce(): List<JournalEntryEntity>

    @Query(
        "SELECT * FROM journal_entries WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC"
    )
    suspend fun getEntriesBetween(startTime: Long, endTime: Long): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entries WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<JournalEntryEntity>

    // Bugfix 2026-07-03 (Room-Almanach K1, gefunden vom android-bug-guard): @Upsert statt
    // @Insert(REPLACE) — REPLACE ist DELETE+INSERT und wuerde bei einem Insert mit bestehender
    // id via CASCADE alle entry_follow_ups + entry_photos des Eintrags still loeschen.
    // Verhaltensneutral: saveEntry() legt immer neue Eintraege an (id=0 -> Insert + rowId),
    // Edits laufen separat ueber update().
    @Upsert
    suspend fun insert(entry: JournalEntryEntity): Long

    @Update suspend fun update(entry: JournalEntryEntity)

    @Delete suspend fun delete(entry: JournalEntryEntity)

    // Debug/Test-Feature: loescht ALLE Eintraege robust, gibt Anzahl geloeschter Zeilen zurueck.
    @Query("DELETE FROM journal_entries")
    suspend fun deleteAll(): Int

    @Query("UPDATE journal_entries SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("UPDATE journal_entries SET followUpText = :followUpText WHERE id = :entryId")
    suspend fun updateFollowUpSummary(entryId: Long, followUpText: String?)
}

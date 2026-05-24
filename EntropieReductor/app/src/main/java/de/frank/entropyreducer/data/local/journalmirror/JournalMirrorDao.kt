package de.frank.entropyreducer.data.local.journalmirror

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalMirrorDao {

    @Query("SELECT * FROM journal_mirror_entries ORDER BY timestamp DESC")
    fun observeEntries(): Flow<List<JournalMirrorEntryEntity>>

    @Query("SELECT * FROM journal_mirror_entries WHERE sourceId = :id")
    suspend fun getEntry(id: Long): JournalMirrorEntryEntity?

    @Query("SELECT * FROM journal_mirror_followups WHERE entryId = :entryId ORDER BY createdAt ASC")
    suspend fun followupsFor(entryId: Long): List<JournalMirrorFollowupEntity>

    @Query("SELECT sourceId FROM journal_mirror_entries")
    suspend fun existingEntryIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<JournalMirrorEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFollowups(followups: List<JournalMirrorFollowupEntity>)

    // Loeschen nach explizit uebergebener ID-Liste (Repository ruft bei leerer Quelle
    // stattdessen deleteAll* auf — "NOT IN ()" ist in SQLite ungueltig).
    @Query("DELETE FROM journal_mirror_entries WHERE sourceId NOT IN (:keepIds)")
    suspend fun deleteEntriesNotIn(keepIds: List<Long>)

    @Query("DELETE FROM journal_mirror_followups WHERE sourceId NOT IN (:keepIds)")
    suspend fun deleteFollowupsNotIn(keepIds: List<Long>)

    @Query("DELETE FROM journal_mirror_entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM journal_mirror_followups")
    suspend fun deleteAllFollowups()
}

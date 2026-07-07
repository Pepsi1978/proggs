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

    @Query("SELECT sourceId FROM journal_mirror_followups")
    suspend fun existingFollowupIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<JournalMirrorEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFollowups(followups: List<JournalMirrorFollowupEntity>)

    // Loeschen nach expliziter ID-Liste. Das Repository berechnet die Loeschmenge in
    // Kotlin und ruft dies in Bloecken (<= 900 IDs) auf — vermeidet das
    // SQLite-Variablenlimit (999 auf aelteren Geraeten). Niemals mit leerer Liste
    // aufrufen ("IN ()" ist ungueltig); das Repository ruft pro Chunk auf, leere
    // Loeschmengen erzeugen gar keinen Aufruf.
    @Query("DELETE FROM journal_mirror_entries WHERE sourceId IN (:ids)")
    suspend fun deleteEntriesByIds(ids: List<Long>)

    @Query("DELETE FROM journal_mirror_followups WHERE sourceId IN (:ids)")
    suspend fun deleteFollowupsByIds(ids: List<Long>)
}

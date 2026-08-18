package de.frank.denknotiz.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observe(id: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions")
    suspend fun all(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE sessionId = :sessionId ORDER BY ordinal")
    fun observeForSession(sessionId: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries ORDER BY createdAt, ordinal")
    suspend fun all(): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun get(id: String): EntryEntity?

    @Query("SELECT COALESCE(MAX(ordinal), 0) FROM entries WHERE sessionId = :sessionId")
    suspend fun maxOrdinal(sessionId: String): Long

    @Query("SELECT * FROM entries WHERE sessionId = :sessionId AND type = 'NOTE' AND ordinal > :lower AND ordinal <= :upper ORDER BY ordinal")
    suspend fun notesInRange(sessionId: String, lower: Long, upper: Long): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE snapshotId = :snapshotId AND type = 'AI_RESPONSE' LIMIT 1")
    suspend fun responseForSnapshot(snapshotId: String): EntryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE entries SET historical = 1 WHERE snapshotId IN (SELECT id FROM evaluation_snapshots WHERE sourceNoteIdsJson LIKE '%' || :noteId || '%')")
    suspend fun markResponsesHistorical(noteId: String)
}

@Dao
interface EvaluationDao {
    @Query("SELECT * FROM evaluation_snapshots WHERE sessionId = :sessionId ORDER BY createdAt")
    fun observeForSession(sessionId: String): Flow<List<EvaluationSnapshotEntity>>

    @Query("SELECT * FROM evaluation_snapshots")
    suspend fun all(): List<EvaluationSnapshotEntity>

    @Query("SELECT * FROM evaluation_snapshots WHERE id = :id")
    suspend fun get(id: String): EvaluationSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(snapshot: EvaluationSnapshotEntity): Long

    @Update
    suspend fun update(snapshot: EvaluationSnapshotEntity)
}

@Dao
interface BoundaryDao {
    @Query("SELECT * FROM context_boundaries WHERE sessionId = :sessionId")
    fun observe(sessionId: String): Flow<ContextBoundaryEntity?>

    @Query("SELECT * FROM context_boundaries")
    suspend fun all(): List<ContextBoundaryEntity>

    @Query("SELECT * FROM context_boundaries WHERE sessionId = :sessionId")
    suspend fun get(sessionId: String): ContextBoundaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(boundary: ContextBoundaryEntity)
}

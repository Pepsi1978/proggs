package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.HypothesisMessageEntity
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.data.local.entities.ScientistMessageEntity
import de.frank.entropyreducer.data.local.entities.ScientistSessionEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.HypothesisStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ScientistSessionDao {
    @Query("SELECT * FROM scientist_sessions WHERE isArchived = 0 ORDER BY lastActiveAt DESC")
    fun getActive(): Flow<List<ScientistSessionEntity>>

    @Query("SELECT * FROM scientist_sessions ORDER BY lastActiveAt DESC")
    fun getAll(): Flow<List<ScientistSessionEntity>>

    @Query("SELECT * FROM scientist_sessions WHERE id = :id")
    suspend fun getById(id: String): ScientistSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ScientistSessionEntity)

    @Update
    suspend fun update(session: ScientistSessionEntity)

    @Delete
    suspend fun delete(session: ScientistSessionEntity)
}

@Dao
interface ScientistMessageDao {
    @Query("SELECT * FROM scientist_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun getForSession(sessionId: String): Flow<List<ScientistMessageEntity>>

    @Query("SELECT * FROM scientist_messages ORDER BY createdAt ASC")
    suspend fun getAll(): List<ScientistMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ScientistMessageEntity)

    @Update
    suspend fun update(message: ScientistMessageEntity)

    @Query("DELETE FROM scientist_messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM scientist_messages WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
}

@Dao
interface HypothesisDao {
    @Query("SELECT * FROM hypotheses ORDER BY createdAt DESC")
    fun getAll(): Flow<List<HypothesisEntity>>

    @Query("SELECT * FROM hypotheses WHERE status = :status ORDER BY plannedStartDate ASC")
    fun getByStatus(status: HypothesisStatus): Flow<List<HypothesisEntity>>

    @Query("SELECT * FROM hypotheses WHERE status = 'AKTIV' AND plannedStartDate <= :date AND plannedEndDate >= :date")
    fun getActiveOnDate(date: Long): Flow<List<HypothesisEntity>>

    @Query("SELECT * FROM hypotheses WHERE id = :id")
    suspend fun getById(id: String): HypothesisEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(hypothesis: HypothesisEntity)

    @Update
    suspend fun update(hypothesis: HypothesisEntity)

    @Delete
    suspend fun delete(hypothesis: HypothesisEntity)
}

@Dao
interface HypothesisMessageDao {
    @Query("SELECT * FROM hypothesis_messages WHERE hypothesisId = :hypothesisId ORDER BY createdAt ASC")
    fun getForHypothesis(hypothesisId: String): Flow<List<HypothesisMessageEntity>>

    @Query("SELECT * FROM hypothesis_messages WHERE hypothesisId = :hypothesisId ORDER BY createdAt ASC")
    suspend fun getForHypothesisOnce(hypothesisId: String): List<HypothesisMessageEntity>

    /** Frank-Wunsch 2026-05-09: Vollstaendiges Drive-Backup braucht alle Messages auf einmal. */
    @Query("SELECT * FROM hypothesis_messages ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<HypothesisMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: HypothesisMessageEntity)

    @Query("DELETE FROM hypothesis_messages WHERE hypothesisId = :hypothesisId")
    suspend fun deleteForHypothesis(hypothesisId: String)
}

@Dao
interface InsightDao {
    @Query("SELECT * FROM insights ORDER BY confidence DESC, successCount DESC")
    fun getByConfidenceDesc(): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insights WHERE targetCategory = :cat ORDER BY confidence DESC")
    fun getByCategory(cat: EntropyCategory): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insights WHERE confidence >= 70 AND attemptCount >= 3 ORDER BY confidence DESC")
    fun getConfirmed(): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insights WHERE confidence < 70 OR attemptCount < 3 ORDER BY updatedAt DESC")
    fun getInObservation(): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insights WHERE confidence <= 20 ORDER BY updatedAt DESC")
    fun getDiscarded(): Flow<List<InsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(insight: InsightEntity)

    @Update
    suspend fun update(insight: InsightEntity)

    @Delete
    suspend fun delete(insight: InsightEntity)
}

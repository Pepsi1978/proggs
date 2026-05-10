package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.frank.entropyreducer.data.local.entities.OuraActivityEntity
import de.frank.entropyreducer.data.local.entities.OuraDailySleepEntity
import de.frank.entropyreducer.data.local.entities.OuraPersonalInfoEntity
import de.frank.entropyreducer.data.local.entities.OuraReadinessEntity
import de.frank.entropyreducer.data.local.entities.OuraResilienceEntity
import de.frank.entropyreducer.data.local.entities.OuraSleepDetailEntity
import kotlinx.coroutines.flow.Flow

/** Tages-Readiness-Score aus /daily_readiness. */
@Dao
interface OuraReadinessDao {
    @Query("SELECT * FROM oura_daily_readiness ORDER BY day DESC LIMIT 1")
    fun getLatest(): Flow<OuraReadinessEntity?>

    @Query("SELECT * FROM oura_daily_readiness ORDER BY day ASC")
    fun getAll(): Flow<List<OuraReadinessEntity>>

    @Query("SELECT * FROM oura_daily_readiness WHERE day = :day")
    suspend fun getByDay(day: String): OuraReadinessEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<OuraReadinessEntity>)

    @Query("DELETE FROM oura_daily_readiness WHERE capturedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("DELETE FROM oura_daily_readiness")
    suspend fun deleteAll()
}

/** Tages-Sleep-Score aus /daily_sleep. */
@Dao
interface OuraDailySleepDao {
    @Query("SELECT * FROM oura_daily_sleep ORDER BY day DESC LIMIT 1")
    fun getLatest(): Flow<OuraDailySleepEntity?>

    @Query("SELECT * FROM oura_daily_sleep ORDER BY day ASC")
    fun getAll(): Flow<List<OuraDailySleepEntity>>

    @Query("SELECT * FROM oura_daily_sleep WHERE day = :day")
    suspend fun getByDay(day: String): OuraDailySleepEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<OuraDailySleepEntity>)

    @Query("DELETE FROM oura_daily_sleep WHERE capturedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("DELETE FROM oura_daily_sleep")
    suspend fun deleteAll()
}

/** Tages-Activity-Score aus /daily_activity. */
@Dao
interface OuraActivityDao {
    @Query("SELECT * FROM oura_daily_activity ORDER BY day DESC LIMIT 1")
    fun getLatest(): Flow<OuraActivityEntity?>

    @Query("SELECT * FROM oura_daily_activity ORDER BY day ASC")
    fun getAll(): Flow<List<OuraActivityEntity>>

    @Query("SELECT * FROM oura_daily_activity WHERE day = :day")
    suspend fun getByDay(day: String): OuraActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<OuraActivityEntity>)

    @Query("DELETE FROM oura_daily_activity WHERE capturedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("DELETE FROM oura_daily_activity")
    suspend fun deleteAll()
}

/** Tages-Resilience-Score aus /daily_resilience. */
@Dao
interface OuraResilienceDao {
    @Query("SELECT * FROM oura_daily_resilience ORDER BY day DESC LIMIT 1")
    fun getLatest(): Flow<OuraResilienceEntity?>

    @Query("SELECT * FROM oura_daily_resilience ORDER BY day ASC")
    fun getAll(): Flow<List<OuraResilienceEntity>>

    @Query("SELECT * FROM oura_daily_resilience WHERE day = :day")
    suspend fun getByDay(day: String): OuraResilienceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<OuraResilienceEntity>)

    @Query("DELETE FROM oura_daily_resilience WHERE capturedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("DELETE FROM oura_daily_resilience")
    suspend fun deleteAll()
}

/** Detaillierte Schlaf-Sessions aus /sleep. */
@Dao
interface OuraSleepDetailDao {
    @Query("SELECT * FROM oura_sleep_detail ORDER BY day DESC, bedtimeStart DESC LIMIT 1")
    fun getLatest(): Flow<OuraSleepDetailEntity?>

    @Query("SELECT * FROM oura_sleep_detail ORDER BY day ASC, bedtimeStart ASC")
    fun getAll(): Flow<List<OuraSleepDetailEntity>>

    @Query("SELECT * FROM oura_sleep_detail WHERE day = :day ORDER BY bedtimeStart ASC")
    fun getByDay(day: String): Flow<List<OuraSleepDetailEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<OuraSleepDetailEntity>)

    @Query("DELETE FROM oura_sleep_detail WHERE capturedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("DELETE FROM oura_sleep_detail")
    suspend fun deleteAll()
}

/** Stammdaten aus /personal_info. Single-Row mit fester ID = 1L. */
@Dao
interface OuraPersonalInfoDao {
    @Query("SELECT * FROM oura_personal_info WHERE id = 1")
    fun observe(): Flow<OuraPersonalInfoEntity?>

    @Query("SELECT * FROM oura_personal_info WHERE id = 1")
    suspend fun get(): OuraPersonalInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OuraPersonalInfoEntity)

    @Query("DELETE FROM oura_personal_info")
    suspend fun deleteAll()
}

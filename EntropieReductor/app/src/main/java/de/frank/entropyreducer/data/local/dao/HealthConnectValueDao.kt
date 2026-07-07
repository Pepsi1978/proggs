package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.frank.entropyreducer.data.local.entities.HealthConnectValueEntity

/**
 * DAO fuer den Cross-Device-Cache der Health-Connect-Werte (Frank-Wunsch
 * 2026-05-10 abend). REPLACE-onConflict macht alle Insert-Aufrufe idempotent —
 * ein erneutes Schreiben desselben (metric, timestampMs) ueberschreibt den Wert
 * sauber.
 */
@Dao
interface HealthConnectValueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(values: List<HealthConnectValueEntity>)

    @Query("SELECT * FROM hc_value_cache WHERE metric = :metric ORDER BY timestampMs ASC")
    suspend fun getByMetric(metric: String): List<HealthConnectValueEntity>

    @Query("SELECT * FROM hc_value_cache ORDER BY timestampMs ASC")
    suspend fun getAll(): List<HealthConnectValueEntity>

    @Query("DELETE FROM hc_value_cache")
    suspend fun deleteAll()
}

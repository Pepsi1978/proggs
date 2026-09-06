package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import de.frank.entropyreducer.data.local.entities.HealthConnectValueEntity
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM hc_value_cache ORDER BY timestampMs ASC")
    fun observeAll(): Flow<List<HealthConnectValueEntity>>

    /** A backup must not roll a corrected Zepp daily measurement back to an older read. */
    @Transaction
    suspend fun restoreValues(values: List<HealthConnectValueEntity>): Int {
        val existing = getAll().associateBy { it.metric to it.timestampMs }
        val accepted = values.filter { incoming ->
            val local = existing[incoming.metric to incoming.timestampMs]
            !incoming.metric.startsWith("zepp_") || local == null || incoming.createdAt > local.createdAt
        }
        upsertAll(accepted)
        return accepted.size
    }

    @Query("DELETE FROM hc_value_cache")
    suspend fun deleteAll()
}

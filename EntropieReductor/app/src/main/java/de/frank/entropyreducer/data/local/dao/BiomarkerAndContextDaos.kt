package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.CalendarEventEntity
import de.frank.entropyreducer.data.local.entities.KiTriggerEntity
import de.frank.entropyreducer.data.local.entities.SupplementLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BiomarkerSnapshotDao {
    @Query("SELECT * FROM biomarker_snapshots ORDER BY capturedAt DESC LIMIT 1")
    fun getLatest(): Flow<BiomarkerSnapshotEntity?>

    @Query("SELECT * FROM biomarker_snapshots WHERE capturedAt BETWEEN :from AND :to ORDER BY capturedAt ASC")
    fun getRange(from: Long, to: Long): Flow<List<BiomarkerSnapshotEntity>>

    @Query("SELECT * FROM biomarker_snapshots WHERE id = :id")
    suspend fun getById(id: String): BiomarkerSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: BiomarkerSnapshotEntity)

    @Query("DELETE FROM biomarker_snapshots")
    suspend fun deleteAll()
}

@Dao
interface SupplementLogDao {
    @Query("SELECT * FROM supplement_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SupplementLogEntity>>

    @Query("SELECT * FROM supplement_logs WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    fun getRange(from: Long, to: Long): Flow<List<SupplementLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SupplementLogEntity)

    @Query("DELETE FROM supplement_logs WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CalendarDayDao {
    @Query("SELECT * FROM calendar_cache WHERE date = :date")
    fun getDay(date: String): Flow<CalendarDayEntity?>

    @Query("SELECT * FROM calendar_cache WHERE date BETWEEN :fromDate AND :toDate ORDER BY date ASC")
    fun getRange(fromDate: String, toDate: String): Flow<List<CalendarDayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: CalendarDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(days: List<CalendarDayEntity>)
}

@Dao
interface KiTriggerDao {
    @Query("SELECT * FROM ki_triggers WHERE isActive = 1 AND approvedAt IS NOT NULL ORDER BY createdAt DESC")
    fun getActive(): Flow<List<KiTriggerEntity>>

    @Query("SELECT * FROM ki_triggers WHERE approvedAt IS NULL ORDER BY proposedAt DESC")
    fun getPendingApproval(): Flow<List<KiTriggerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(trigger: KiTriggerEntity)

    @Update
    suspend fun update(trigger: KiTriggerEntity)

    @Query("DELETE FROM ki_triggers WHERE id = :id")
    suspend fun deleteById(id: String)
}

/**
 * Google-Calendar-Events (Stufe 4 Erweiterung). Wird bei jedem Sync neu befuellt
 * — alte Events im Sync-Fenster werden zuerst geloescht, dann die aktuellen
 * geschrieben, damit geloeschte Google-Events nicht haengen bleiben.
 */
@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events WHERE date = :date ORDER BY allDay DESC, startMs ASC")
    fun getByDate(date: String): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE date BETWEEN :fromDate AND :toDate ORDER BY date ASC, allDay DESC, startMs ASC")
    fun getRange(fromDate: String, toDate: String): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<CalendarEventEntity>)

    @Query("DELETE FROM calendar_events WHERE date BETWEEN :fromDate AND :toDate")
    suspend fun deleteRange(fromDate: String, toDate: String)

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAll()
}

package de.frank.jarvis.bio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.frank.jarvis.bio.data.local.entities.BiomarkerSnapshotEntity
import de.frank.jarvis.bio.data.local.entities.WhoopWorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BiomarkerSnapshotDao {
    @Query("SELECT * FROM biomarker_snapshots ORDER BY capturedAt DESC LIMIT 1")
    fun getLatest(): Flow<BiomarkerSnapshotEntity?>

    @Query("SELECT * FROM biomarker_snapshots WHERE capturedAt BETWEEN :from AND :to ORDER BY capturedAt ASC")
    fun getRange(from: Long, to: Long): Flow<List<BiomarkerSnapshotEntity>>

    /** Frank-Wunsch 2026-05-08: vollstaendige Historie aller Whoop-Snapshots,
     *  nicht nur die letzten 30 Tage. Wird im BiomarkerDetailScreen + Charts genutzt. */
    @Query("SELECT * FROM biomarker_snapshots ORDER BY capturedAt ASC")
    fun getAll(): Flow<List<BiomarkerSnapshotEntity>>

    @Query("SELECT * FROM biomarker_snapshots WHERE id = :id")
    suspend fun getById(id: String): BiomarkerSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: BiomarkerSnapshotEntity)

    /** Performance-Fix 2026-07-03 (#47449): Batch-Variante fuer den Start-Restore —
     *  EINE Transaktion statt N Einzeltransaktionen (vorher ~300 pro App-Start). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(snapshots: List<BiomarkerSnapshotEntity>)

    @Query("DELETE FROM biomarker_snapshots")
    suspend fun deleteAll()

    /** Frank-Wunsch 2026-05-09: Whoop-Daten erst ab 25.02.2026 (Geraete-Kaufdatum)
     *  zeigen — alles davor sind Whoop-API-Phantomwerte (z.B. Avg-HR=0). Wird bei
     *  jedem Sync aufgerufen damit alte Daten konsistent verschwinden. */
    @Query("DELETE FROM biomarker_snapshots WHERE capturedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}

/**
 * Whoop-Workouts (1:N pro Tag). Liefert eine flache Liste — Aggregation pro Tag
 * geschieht im ViewModel via groupBy(dateKey). Frank-Wunsch 2026-05-09: alle
 * Trainings mit Sportart, Strain, HR-Zonen sichtbar machen.
 */
@Dao
interface WhoopWorkoutDao {
    @Query("SELECT * FROM whoop_workouts ORDER BY startMs DESC")
    fun observeAll(): Flow<List<WhoopWorkoutEntity>>

    @Query("SELECT * FROM whoop_workouts WHERE startMs BETWEEN :from AND :to ORDER BY startMs DESC")
    fun observeRange(from: Long, to: Long): Flow<List<WhoopWorkoutEntity>>

    @Query("SELECT * FROM whoop_workouts WHERE dateKey = :dateKey ORDER BY startMs ASC")
    fun observeByDateKey(dateKey: String): Flow<List<WhoopWorkoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(workout: WhoopWorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(workouts: List<WhoopWorkoutEntity>)

    @Query("DELETE FROM whoop_workouts WHERE startMs < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("DELETE FROM whoop_workouts")
    suspend fun deleteAll()
}


package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.frank.entropyreducer.data.local.entities.AmazfitDailyEntity
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import kotlinx.coroutines.flow.Flow

/**
 * Tagliche Amazfit-Werte (PAI, BioCharge, Hauttemperatur, etc.).
 * Pattern wie BiomarkerSnapshotDao — getLatest, getRange, getAll, upsert,
 * deleteOlderThan. Frank-Wunsch 2026-05-09: alle Werte mit Quellen-Markierung.
 */
@Dao
interface AmazfitDailyDao {
    @Query("SELECT * FROM amazfit_daily ORDER BY capturedAt DESC LIMIT 1")
    fun getLatest(): Flow<AmazfitDailyEntity?>

    @Query("SELECT * FROM amazfit_daily ORDER BY capturedAt ASC")
    fun getAll(): Flow<List<AmazfitDailyEntity>>

    @Query("SELECT * FROM amazfit_daily WHERE capturedAt BETWEEN :from AND :to ORDER BY capturedAt ASC")
    fun getRange(from: Long, to: Long): Flow<List<AmazfitDailyEntity>>

    @Query("SELECT * FROM amazfit_daily WHERE date = :date")
    suspend fun getByDate(date: String): AmazfitDailyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AmazfitDailyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AmazfitDailyEntity>)

    @Query("DELETE FROM amazfit_daily WHERE capturedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("DELETE FROM amazfit_daily")
    suspend fun deleteAll()
}

/**
 * Amazfit-Workouts (Sport-Sessions mit GPS-Track + Pulsverlauf + Splits).
 * Frank-Wunsch 2026-05-09: kompletter Sport-Bereich als Unterbereich des
 * Biomarker-Screens.
 */
@Dao
interface AmazfitWorkoutDao {
    @Query("SELECT * FROM amazfit_workouts ORDER BY startMs DESC")
    fun observeAll(): Flow<List<AmazfitWorkoutEntity>>

    @Query("SELECT * FROM amazfit_workouts WHERE startMs BETWEEN :from AND :to ORDER BY startMs DESC")
    fun observeRange(from: Long, to: Long): Flow<List<AmazfitWorkoutEntity>>

    @Query("SELECT * FROM amazfit_workouts WHERE dateKey = :dateKey ORDER BY startMs ASC")
    fun observeByDateKey(dateKey: String): Flow<List<AmazfitWorkoutEntity>>

    @Query("SELECT * FROM amazfit_workouts WHERE trackId = :trackId LIMIT 1")
    suspend fun getById(trackId: String): AmazfitWorkoutEntity?

    @Query("SELECT * FROM amazfit_workouts WHERE trackId = :trackId LIMIT 1")
    fun observeById(trackId: String): Flow<AmazfitWorkoutEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(workout: AmazfitWorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(workouts: List<AmazfitWorkoutEntity>)

    @Query("DELETE FROM amazfit_workouts WHERE startMs < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("DELETE FROM amazfit_workouts")
    suspend fun deleteAll()

    /**
     * Loescht alle Workouts die NICHT von Polar stammen.
     *
     * Frank-Wunsch 2026-05-16: Nach dem Strava-Revert und der Umstellung auf
     * Polar als alleinige Trainings-Quelle sollen die alten Zepp/Health-Connect-
     * Eintraege weg — sie haben kein Update mehr, die "T-Rex 3"-Labels nerven
     * und sie verwirren die Trainings-Liste.
     *
     * Erfasst:
     *  - source IS NULL (Legacy-Eintraege ohne Quellen-Tag)
     *  - source NOT LIKE 'polar%' (alles ausser 'polar' und 'polar-bulk')
     *
     * Wird vom PolarBulkImportWorker direkt vor dem Schreiben der frischen
     * Polar-Trainings aufgerufen — Frank sieht danach nur noch Polar-Daten.
     *
     * @return Anzahl der geloeschten Zeilen
     */
    @Query("DELETE FROM amazfit_workouts WHERE source IS NULL OR source NOT LIKE 'polar%'")
    suspend fun deleteNonPolarWorkouts(): Int

    /**
     * Frank-Wunsch 2026-05-16: Health-Connect-Workout-Merge braucht die Liste
     * existierender Start-Zeitstempel um Duplikate zu erkennen. Wir vergleichen
     * neue HC-Sessions per +/- 5 Minuten Toleranz gegen diese Liste.
     */
    @Query("SELECT startMs FROM amazfit_workouts WHERE startMs BETWEEN :from AND :to ORDER BY startMs DESC")
    suspend fun getStartMsInRange(from: Long, to: Long): List<Long>

    /**
     * Setzt sportName fuer alle Workouts mit gegebenem sportType — aber nur dort
     * wo der Name aktuell abweicht (idempotent, kein No-Op-UPDATE wenn schon korrekt).
     * Frank-Wunsch 2026-05-10: Migration fuer T-Rex-3-Codes 12 (Crosstrainer) und
     * 52 (Krafttraining) — beide wurden frueher faelschlich als "Laufen" gespeichert
     * weil der source-Prefix "run.huami.com" auf der T-Rex 3 generisch fuer alle
     * Sportarten ist. Gibt die Anzahl geaenderter Zeilen zurueck.
     */
    @Query(
        "UPDATE amazfit_workouts SET sportName = :sportName " +
            "WHERE sportType = :sportType AND sportName != :sportName",
    )
    suspend fun updateSportNameByType(sportType: Int, sportName: String): Int
}

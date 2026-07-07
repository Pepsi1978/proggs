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
     * Frank-Bugfix 2026-05-22: Cleanup-Migration darf nicht auf einem frisch
     * installierten Geraet vor dem Drive-Restore laufen — sie wuerde sonst
     * den restaurierten Stand sofort wieder loeschen. count() liefert die
     * Anzahl Workouts in der lokalen DB; bei 0 + nicht gesetztem Cleanup-
     * Flag wartet die Migration auf den Restore.
     */
    @Query("SELECT COUNT(*) FROM amazfit_workouts")
    suspend fun count(): Int

    /**
     * Frank-Wunsch 2026-05-18: Loescht ein einzelnes Workout per trackId.
     * Wird aus dem Training-Detail-Screen ueber das 3-Punkte-Menue
     * "Aktivitaet loeschen" aufgerufen.
     *
     * @return 1 wenn das Workout existierte und geloescht wurde, sonst 0
     */
    @Query("DELETE FROM amazfit_workouts WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: String): Int

    /**
     * Frank-Wunsch 2026-05-17: Sandwich-Delete fuer den V2-Cleanup. Loescht alle
     * Workouts deren startMs ausserhalb des Fensters [olderThanMs, newerThanMs]
     * liegt. Beide Grenzen INKLUSIV — Eintraege mit startMs == olderThanMs oder
     * startMs == newerThanMs bleiben erhalten.
     *
     * Anwendung: 967 Polar-Trainings reduzieren auf "nur die letzten 2 Jahre bis
     * 30.03.2026 17:25" — also Uralt-Daten UND die neuen Duplikate (17.05., 14.05.,
     * 09.05., 08.05., 01.05.) in EINEM atomaren Schritt entfernen.
     *
     * @return Anzahl der geloeschten Zeilen
     */
    @Query("DELETE FROM amazfit_workouts WHERE startMs < :olderThanMs OR startMs > :newerThanMs")
    suspend fun deleteOutsideRange(olderThanMs: Long, newerThanMs: Long): Int

    /**
     * Loescht alle Workouts die NICHT von Polar stammen.
     *
     * Frank-Wunsch 2026-05-16: Nach der Umstellung auf
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
     * Frank-Bugfix 2026-05-16: SELECT ohne die grossen Stream-Felder
     * (gpsTrackJson, heartRateSeriesJson, paceSeriesJson, paceStreamJson,
     * splitsJson) damit beim Backup nicht 100+ MB Daten in den RAM kommen
     * die danach eh weggeworfen werden.
     *
     * Bei 956 Polar-Bulk-Trainings reduziert das den Backup-Build-RAM
     * von ~135 MB auf ~3 MB — beide Allokationsspitzen weg.
     */
    @Query(
        "SELECT trackId, dateKey, startMs, endMs, durationSeconds, sportType, " +
            "sportName, distanceMeters, avgPaceSecPerKm, maxPaceSecPerKm, " +
            "avgSpeedKmh, maxSpeedKmh, calories, avgHeartRate, maxHeartRate, " +
            "altitudeGainMeters, altitudeLossMeters, trainingEffectAerobic, " +
            "trainingEffectAnaerobic, vo2Max, cadence, strideLengthCm, " +
            "recoveryTimeHours, skinTempCelsius, swolf, poolLaps, " +
            "poolLengthMeters, source, city, " +
            "weatherTempCelsius, weatherCondition, weatherFetchedMs, createdAt " +
            "FROM amazfit_workouts ORDER BY startMs DESC"
    )
    suspend fun getAllForBackupSlim(): List<AmazfitWorkoutBackupRow>

    /**
     * Frank-Wunsch 2026-05-16: Health-Connect-Workout-Merge braucht die Liste
     * existierender Start-Zeitstempel um Duplikate zu erkennen.
     */
    @Query("SELECT startMs FROM amazfit_workouts WHERE startMs BETWEEN :from AND :to ORDER BY startMs DESC")
    suspend fun getStartMsInRange(from: Long, to: Long): List<Long>

    /**
     * Performance 2026-05-23 (Loop 1, Direktive #3): Schlanke 3-Spalten-Projektion NUR der
     * Felder die der Workouts-Backup-Fingerprint im [SyncCoordinator] braucht — trackId,
     * createdAt und manualOverridesMs. OHNE die grossen Stream-Felder (gpsTrackJson,
     * heartRateSeriesJson, paceSeriesJson, paceStreamJson, splitsJson).
     *
     * Hintergrund: Der Fingerprint entscheidet beim App-Start ob sich seit dem letzten
     * Workouts-Backup etwas geaendert hat. Frueher wurden dafuer ueber observeAll() ALLE
     * Workouts inkl. ~6 MB (bei viel Polar-Historie bis ~130 MB) Stream-Daten in den RAM
     * geladen und nach dem Hash sofort verworfen. Diese Projektion liefert nur die ~30 Byte
     * pro Workout die der Hash wirklich braucht.
     *
     * WICHTIG: Reihenfolge IDENTISCH zu [observeAll] (ORDER BY startMs DESC), damit der
     * joinToString-Hash bit-genau derselbe ist und die Skip-/Upload-Entscheidung sich nicht
     * aendert.
     */
    @Query(
        "SELECT trackId, createdAt, manualOverridesMs, weatherFetchedMs FROM amazfit_workouts ORDER BY startMs DESC"
    )
    suspend fun getFingerprintRows(): List<AmazfitWorkoutFingerprintRow>

    /**
     * Setzt sportName fuer alle Workouts mit gegebenem sportType — aber nur
     * wo der Name aktuell abweicht (idempotent).
     */
    @Query(
        "UPDATE amazfit_workouts SET sportName = :sportName " +
            "WHERE sportType = :sportType AND sportName != :sportName",
    )
    suspend fun updateSportNameByType(sportType: Int, sportName: String): Int

    /**
     * Frank-Wunsch 2026-05-17: einmalige Umbenennung. Alle Workouts deren
     * sportName auf einen alten Begriff matched, bekommen den neuen Namen.
     * Idempotent — wenn niemand mehr den alten Namen hat, 0 Zeilen geaendert.
     */
    @Query(
        "UPDATE amazfit_workouts SET sportName = :newName WHERE sportName = :oldName",
    )
    suspend fun renameSportName(oldName: String, newName: String): Int

    /**
     * Frank-Wunsch 2026-05-24: Dauer-Korrektur per gezieltem UPDATE. Setzt NUR durationSeconds
     * und endMs eines Trainings (per trackId) auf die verstrichene Zeit (elapsed_time) —
     * alle anderen Felder (GPS, Puls, Splits, Pace) bleiben unangetastet. Die WHERE-Klausel
     * `durationSeconds != :durationSeconds` macht das idempotent (kein Schreibzugriff wenn
     * der Wert schon stimmt).
     *
     * @return 1 wenn aktualisiert, sonst 0
     */
    @Query(
        "UPDATE amazfit_workouts SET durationSeconds = :durationSeconds, endMs = :endMs " +
            "WHERE trackId = :trackId AND durationSeconds != :durationSeconds",
    )
    suspend fun updateDurationByTrackId(trackId: String, durationSeconds: Long, endMs: Long): Int

    /**
     * Wetter pro Training (Open-Meteo) setzen: Temperatur (°C), Wetterlage-Wort und den
     * Abruf-Zeitstempel per trackId. [fetchedMs] wird IMMER gesetzt — auch wenn temp/condition
     * null sind (API-Fehler/kein Ergebnis) — damit dasselbe Training nicht bei jedem Sync
     * erneut abgefragt wird.
     *
     * @return 1 wenn aktualisiert, sonst 0
     */
    @Query(
        "UPDATE amazfit_workouts SET weatherTempCelsius = :temp, weatherCondition = :condition, " +
            "weatherFetchedMs = :fetchedMs WHERE trackId = :trackId",
    )
    suspend fun updateWeather(trackId: String, temp: Int?, condition: String?, fetchedMs: Long): Int

    /**
     * Kandidaten fuer den Wetter-Backfill: Trainings, die noch nie einen Wetter-Abruf hatten
     * (weatherFetchedMs IS NULL) und einen GPS-Track besitzen (ohne Koordinaten kein Wetter).
     * Neueste zuerst — so bekommen die aktuellen Trainings zuerst ihr Wetter.
     */
    @Query(
        "SELECT * FROM amazfit_workouts WHERE weatherFetchedMs IS NULL AND gpsTrackJson IS NOT NULL " +
            "ORDER BY startMs DESC",
    )
    suspend fun getWorkoutsMissingWeather(): List<AmazfitWorkoutEntity>
}

/**
 * Schlanke Projektion fuer das Cross-Device-Backup — ohne die teuren
 * Stream-Felder (gpsTrackJson, heartRateSeriesJson, paceSeriesJson,
 * paceStreamJson, splitsJson). Bei 956 Workouts spart das ~130 MB RAM.
 */
data class AmazfitWorkoutBackupRow(
    val trackId: String,
    val dateKey: String,
    val startMs: Long,
    val endMs: Long,
    val durationSeconds: Long?,
    val sportType: Int?,
    val sportName: String?,
    val distanceMeters: Double?,
    val avgPaceSecPerKm: Double?,
    val maxPaceSecPerKm: Double?,
    val avgSpeedKmh: Double?,
    val maxSpeedKmh: Double?,
    val calories: Double?,
    val avgHeartRate: Int?,
    val maxHeartRate: Int?,
    val altitudeGainMeters: Double?,
    val altitudeLossMeters: Double?,
    val trainingEffectAerobic: Double?,
    val trainingEffectAnaerobic: Double?,
    val vo2Max: Double?,
    val cadence: Int?,
    val strideLengthCm: Int?,
    val recoveryTimeHours: Int?,
    val skinTempCelsius: Double?,
    val swolf: Int?,
    val poolLaps: Int?,
    val poolLengthMeters: Double?,
    val source: String?,
    val city: String?,
    val weatherTempCelsius: Int?,
    val weatherCondition: String?,
    val weatherFetchedMs: Long?,
    val createdAt: Long,
)

/**
 * Performance 2026-05-23 (Loop 1): Minimal-Projektion fuer den Workouts-Backup-Fingerprint.
 * Enthaelt nur die drei Felder die in den Hash einfliessen — KEINE Stream-Felder. Wird von
 * [AmazfitWorkoutDao.getFingerprintRows] geliefert. Feld-Reihenfolge und -Typen sind exakt
 * die der bisherigen Fingerprint-Berechnung im SyncCoordinator, damit der Hash identisch bleibt.
 */
data class AmazfitWorkoutFingerprintRow(
    val trackId: String,
    val createdAt: Long,
    val manualOverridesMs: Long?,
    /** Wetter-Abruf-Zeitstempel im Fingerprint: so triggert ein Wetter-Backfill ein neues
     *  Drive-Backup (sonst bliebe der Hash gleich und das Wetter kaeme nie in die Cloud). */
    val weatherFetchedMs: Long?,
)

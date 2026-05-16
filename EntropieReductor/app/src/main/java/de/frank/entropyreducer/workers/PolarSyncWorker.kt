package de.frank.entropyreducer.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.room.withTransaction
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.local.AppDatabase
import de.frank.entropyreducer.data.local.dao.AmazfitWorkoutDao
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.repository.PolarRepository

/**
 * Synchronisiert Polar-Workouts im Hintergrund (Frank-Wunsch 2026-05-16).
 *
 * Pollt die naechste Polar-Transaction, mappt alle enthaltenen Trainings auf
 * AmazfitWorkoutEntities und schreibt sie in `amazfit_workouts`. Bei Erfolg
 * wird ein Drive-Backup angestossen damit das andere Geraet beim naechsten
 * Restore sofort die neuen Trainings sieht.
 *
 * Direktive 3 (Resilient Bugfixing):
 *  - Bei Auth-Fehlern (Token abgelaufen, User nicht registriert): success
 *    ohne Retry — der User muss in der App neu autorisieren. Endloses
 *    Retry wuerde Quota verbrauchen ohne Effekt.
 *  - Bei Netzwerk-Fehlern: retry
 *  - Transaction-Commit ist Pflicht — wenn der naechste Sync wieder die
 *    gleichen Eintraege sieht, schreiben wir mit upsertAll mit der gleichen
 *    trackId, also kein Datenverlust (REPLACE-Insert).
 */
@HiltWorker
class PolarSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val polarRepo: PolarRepository,
    private val workoutDao: AmazfitWorkoutDao,
    private val appDatabase: AppDatabase,
    private val syncCoordinator: SyncCoordinator,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!polarRepo.isAuthenticated()) {
            Log.d(TAG, "Polar nicht verbunden — Sync skippen")
            return Result.success()
        }
        val outcome = polarRepo.fetchWorkoutsAsEntities()
        return outcome.fold(
            onSuccess = { entities ->
                if (entities.isNotEmpty()) {
                    // Frank-Bug 2026-05-16 (Iteration 2): Frueher wurde ein
                    // bestehendes Workout komplett SKIPPED — auch wenn der
                    // frische Polar-Pull jetzt Streams (HR, GPS, Pace) hatte
                    // die vorher fehlten. Folge: Polar's typische 5-30-Min-
                    // Latenz fuer Samples konnte NIE in die DB nachfliessen.
                    //
                    // Neuer Workflow:
                    //  - source == "polar-bulk": SKIPPEN (Frank-Wunsch:
                    //    Bulk-Historie nie ueberschreiben, sie ist die
                    //    autoritative Lang-Historie aus dem ZIP-Export).
                    //  - source == "polar" + Entity neu in DB: INSERT.
                    //  - source == "polar" + Entity schon da: MERGE mit
                    //    fresh-wins-if-not-null. So bekommen frische Streams
                    //    eine zweite Chance ohne dass leere Refresh-Versuche
                    //    bereits vorhandene Daten ausnullen.
                    val newOnly = mutableListOf<de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity>()
                    val updated = mutableListOf<de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity>()
                    var skippedBulk = 0
                    var unchanged = 0
                    for (e in entities) {
                        val exists = workoutDao.getById(e.trackId)
                        when {
                            exists == null -> newOnly += e
                            exists.source == "polar-bulk" -> {
                                skippedBulk++
                                Log.d(TAG, "Polar-Sync: ${e.trackId} ist Bulk-Eintrag — Live-Daten werden NICHT geschrieben")
                            }
                            else -> {
                                val merged = exists.copy(
                                    durationSeconds = e.durationSeconds ?: exists.durationSeconds,
                                    sportType = e.sportType ?: exists.sportType,
                                    sportName = e.sportName ?: exists.sportName,
                                    distanceMeters = e.distanceMeters ?: exists.distanceMeters,
                                    avgPaceSecPerKm = e.avgPaceSecPerKm ?: exists.avgPaceSecPerKm,
                                    maxPaceSecPerKm = e.maxPaceSecPerKm ?: exists.maxPaceSecPerKm,
                                    avgSpeedKmh = e.avgSpeedKmh ?: exists.avgSpeedKmh,
                                    maxSpeedKmh = e.maxSpeedKmh ?: exists.maxSpeedKmh,
                                    calories = e.calories ?: exists.calories,
                                    avgHeartRate = e.avgHeartRate ?: exists.avgHeartRate,
                                    maxHeartRate = e.maxHeartRate ?: exists.maxHeartRate,
                                    gpsTrackJson = e.gpsTrackJson ?: exists.gpsTrackJson,
                                    heartRateSeriesJson = e.heartRateSeriesJson ?: exists.heartRateSeriesJson,
                                    paceSeriesJson = e.paceSeriesJson ?: exists.paceSeriesJson,
                                    paceStreamJson = e.paceStreamJson ?: exists.paceStreamJson,
                                    splitsJson = e.splitsJson ?: exists.splitsJson,
                                    altitudeGainMeters = e.altitudeGainMeters ?: exists.altitudeGainMeters,
                                    altitudeLossMeters = e.altitudeLossMeters ?: exists.altitudeLossMeters,
                                    trainingEffectAerobic = e.trainingEffectAerobic ?: exists.trainingEffectAerobic,
                                    trainingEffectAnaerobic = e.trainingEffectAnaerobic ?: exists.trainingEffectAnaerobic,
                                    vo2Max = e.vo2Max ?: exists.vo2Max,
                                    cadence = e.cadence ?: exists.cadence,
                                    strideLengthCm = e.strideLengthCm ?: exists.strideLengthCm,
                                    createdAt = System.currentTimeMillis(),
                                )
                                if (merged != exists) {
                                    updated += merged
                                    Log.i(TAG, "Polar-Sync: ${e.trackId} aktualisiert — streams jetzt: hr=${merged.heartRateSeriesJson != null} pace=${merged.paceStreamJson != null} gps=${merged.gpsTrackJson != null} splits=${merged.paceSeriesJson != null} cadence=${merged.cadence != null} stride=${merged.strideLengthCm != null} altGain=${merged.altitudeGainMeters != null} altLoss=${merged.altitudeLossMeters != null} maxPace=${merged.maxPaceSecPerKm != null}")
                                } else {
                                    unchanged++
                                }
                            }
                        }
                    }
                    if (newOnly.isNotEmpty() || updated.isNotEmpty()) {
                        appDatabase.withTransaction {
                            if (newOnly.isNotEmpty()) workoutDao.upsertAll(newOnly)
                            for (m in updated) workoutDao.upsert(m)
                        }
                        Log.i(TAG, "Polar-Sync: ${newOnly.size} neu, ${updated.size} aktualisiert, $skippedBulk Bulk skipped, $unchanged unveraendert")
                        syncCoordinator.requestSync()
                    } else {
                        Log.d(TAG, "Polar-Sync: ${entities.size} geliefert — nichts neu (Bulk: $skippedBulk, unveraendert: $unchanged)")
                    }
                }
                Result.success()
            },
            onFailure = { ex ->
                Log.w(TAG, "Polar-Sync fehlgeschlagen: ${ex.message}")
                Result.retry()
            },
        )
    }

    companion object {
        private const val TAG = "PolarSyncWorker"
        const val UNIQUE_NAME_PERIODIC = "polar-sync-periodic"
        const val UNIQUE_NAME_ONESHOT = "polar-sync-oneshot"
    }
}

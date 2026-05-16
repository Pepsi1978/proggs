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
                    appDatabase.withTransaction {
                        workoutDao.upsertAll(entities)
                    }
                    Log.i(TAG, "Polar-Sync: ${entities.size} Workouts geschrieben")
                    // Frische Trainings sofort ins Drive-Backup — gleicher Pattern
                    // wie bei den anderen Datenquellen.
                    syncCoordinator.requestSync()
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

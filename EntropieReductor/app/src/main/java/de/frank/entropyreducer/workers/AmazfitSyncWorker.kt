package de.frank.entropyreducer.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.repository.AmazfitRepository

/**
 * Synchronisiert taeglich die Amazfit-T-Rex-3-Daten aus der Zepp-Cloud.
 * Pattern wie WhoopSyncWorker — analog naechtlich (04:30) oder on-demand
 * via BackgroundScheduler.runAmazfitSyncNow().
 *
 * Frank-Wunsch 2026-05-09: Erst-Sync zieht 365 Tage Historie. Bei dauerhaftem
 * Lauf reicht das ebenfalls (Zepp speichert weiter zurueck, aber 365 Tage sind
 * fuer Trends mehr als genug).
 */
@HiltWorker
class AmazfitSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: AmazfitRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val outcome = repo.syncLastDays(days = 365)
        return outcome.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }

    companion object {
        const val UNIQUE_NAME_PERIODIC = "amazfit-sync-periodic"
        const val UNIQUE_NAME_ONESHOT = "amazfit-sync-oneshot"
    }
}

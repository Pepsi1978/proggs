package de.frank.entropyreducer.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.repository.WhoopRepository

/**
 * Synchronisiert Whoop-Biomarker im Hintergrund.
 * Spec §15.4 — bei App-Start (wenn letzter Sync > 30 Min.) + nightly 04:30.
 */
@HiltWorker
class WhoopSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: WhoopRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val outcome = repo.syncLastDays(days = 30)
        return outcome.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }

    companion object {
        const val UNIQUE_NAME_PERIODIC = "whoop-sync-periodic"
        const val UNIQUE_NAME_ONESHOT = "whoop-sync-oneshot"
    }
}

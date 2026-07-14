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
        // Frank-Wunsch 2026-05-08: vollstaendige Historie (letztes Jahr) statt
        // nur 30 Tage — die KI kann mit mehr Daten besser Trends erkennen und
        // die Korrelations-Card profitiert direkt von der laengeren Historie.
        val outcome = repo.syncLastDays(days = 365)
        return outcome.fold(
            onSuccess = { Result.success() },
            onFailure = { retryOrFailure() },
        )
    }

    companion object {
        const val UNIQUE_NAME_PERIODIC = "whoop-sync-periodic"
        const val UNIQUE_NAME_ONESHOT = "whoop-sync-oneshot"
    }
}

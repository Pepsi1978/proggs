package de.frank.entropyreducer.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.repository.CalendarRepository
import de.frank.entropyreducer.domain.usecase.RebucketEntriesUseCase

/**
 * Synchronisiert Google Calendar (Schichtcodes) im Hintergrund. Nach erfolgreichem
 * Sync werden alle aktiven Eintraege gegen den neuen Kalender re-gebucketed (§22 Stufe 2).
 */
@HiltWorker
class CalendarSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: CalendarRepository,
    private val rebucket: RebucketEntriesUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val outcome = repo.syncDefaultWindow()
        return outcome.fold(
            onSuccess = {
                runCatching { rebucket() }
                Result.success()
            },
            onFailure = { Result.retry() },
        )
    }

    companion object {
        const val UNIQUE_NAME_PERIODIC = "calendar-sync-periodic"
        const val UNIQUE_NAME_ONESHOT = "calendar-sync-oneshot"
    }
}

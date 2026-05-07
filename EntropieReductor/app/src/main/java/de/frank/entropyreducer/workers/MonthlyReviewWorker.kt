package de.frank.entropyreducer.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.usecase.GenerateReviewUseCase
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * Monatsrueckblick — am 1. des Folgemonats 19:00 (Spec §16.3).
 * Worker laeuft taeglich, prueft selbst ob heute der 1. ist und ob diesen Monat
 * schon ein Rueckblick erstellt wurde (via lastMonthlyReviewAt). So uberlebt der
 * Job auch, wenn das Geraet zwischen 1. und 2. ausgeschaltet war.
 */
@HiltWorker
class MonthlyReviewWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settings: AppSettings,
    private val generator: GenerateReviewUseCase,
    private val notifier: ShiftAwareNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now(ZoneId.systemDefault())
            // Erst zwischen 1. und 7. eines Monats erlauben — danach nicht mehr nachholen.
            if (today.dayOfMonth > 7) {
                Log.i(TAG, "Heute ist der ${today.dayOfMonth}., zu spaet fuer den Monatsrueckblick.")
                return Result.success()
            }
            val lastAt = settings.lastMonthlyReviewAtMsFlow.first()
            val lastDate = if (lastAt > 0) {
                java.time.Instant.ofEpochMilli(lastAt).atZone(ZoneId.systemDefault()).toLocalDate()
            } else null
            if (lastDate?.year == today.year && lastDate.monthValue == today.monthValue) {
                Log.i(TAG, "Monatsrueckblick fuer ${today.month} existiert bereits.")
                return Result.success()
            }

            val result = generator(GenerateReviewUseCase.Range.MONTH)
            val text = result.getOrNull()
            if (text.isNullOrBlank()) {
                Log.w(TAG, "Monatsrueckblick leer: ${result.exceptionOrNull()?.message}")
                Result.retry()
            } else {
                settings.setMonthlyReview(text, System.currentTimeMillis())
                notifier.postOrDelay(
                    notificationId = NOTIFICATION_ID,
                    title = "Dein Monatsrueckblick ist fertig",
                    body = "Tippen, um ihn vom Genie vorlesen zu lassen.",
                )
                Result.success()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "MonthlyReviewWorker fehlgeschlagen", t)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "MonthlyReviewWorker"
        private const val NOTIFICATION_ID = 4714
        const val UNIQUE_NAME_PERIODIC = "monthly-review-periodic"
        const val UNIQUE_NAME_ONESHOT = "monthly-review-oneshot"
    }
}

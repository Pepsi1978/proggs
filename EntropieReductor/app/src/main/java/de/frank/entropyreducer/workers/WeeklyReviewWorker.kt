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

/**
 * Wochenrueckblick — Sonntag 19:00 (Spec §16.3).
 * Erzeugt narrativen Markdown-Text der letzten 7 Tage und cached ihn in AppSettings.
 */
@HiltWorker
class WeeklyReviewWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settings: AppSettings,
    private val generator: GenerateReviewUseCase,
    private val notifier: ShiftAwareNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val result = generator(GenerateReviewUseCase.Range.WEEK)
        val text = result.getOrNull()
        if (text.isNullOrBlank()) {
            Log.w(TAG, "Wochenrueckblick leer: ${result.exceptionOrNull()?.message}")
            Result.retry()
        } else {
            settings.setWeeklyReview(text, System.currentTimeMillis())
            notifier.postOrDelay(
                notificationId = NOTIFICATION_ID,
                title = "Dein Wochenrueckblick ist fertig",
                body = "Tippen, um ihn vom Genie vorlesen zu lassen.",
            )
            Result.success()
        }
    } catch (t: Throwable) {
        Log.e(TAG, "WeeklyReviewWorker fehlgeschlagen", t)
        Result.retry()
    }

    companion object {
        private const val TAG = "WeeklyReviewWorker"
        private const val NOTIFICATION_ID = 4713
        const val UNIQUE_NAME_PERIODIC = "weekly-review-periodic"
        const val UNIQUE_NAME_ONESHOT = "weekly-review-oneshot"
    }
}

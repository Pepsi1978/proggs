package de.frank.entropyreducer.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.notifications.AppNotification
import de.frank.entropyreducer.domain.usecase.GenerateReviewUseCase
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * Monatsrueckblick — am 1. des Folgemonats 19:00 (Spec §16.3).
 * Worker laeuft täglich, prueft selbst ob heute der 1. ist und ob diesen Monat
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
            val force = inputData.getBoolean(KEY_FORCE, false)
            val today = LocalDate.now(ZoneId.systemDefault())
            // Erst zwischen 1. und 7. eines Monats erlauben — danach nicht mehr nachholen.
            if (!force && today.dayOfMonth > 7) {
                Diag.i(DiagnosticArea.BRIEFING, TAG, "Heute ist der ${today.dayOfMonth}., zu spät für den Monatsrueckblick (force=false).")
                return Result.success()
            }
            val lastAt = settings.lastMonthlyReviewAtMsFlow.first()
            val lastDate = if (lastAt > 0) {
                java.time.Instant.ofEpochMilli(lastAt).atZone(ZoneId.systemDefault()).toLocalDate()
            } else null
            if (!force && lastDate?.year == today.year && lastDate.monthValue == today.monthValue) {
                Diag.i(DiagnosticArea.BRIEFING, TAG, "Monatsrueckblick für ${today.month} existiert bereits (force=false).")
                return Result.success()
            }

            val result = generator(GenerateReviewUseCase.Range.MONTH)
            val text = result.getOrNull()
            if (text.isNullOrBlank()) {
                val ex = result.exceptionOrNull()
                Diag.w(DiagnosticArea.BRIEFING, TAG, "Monatsrueckblick leer: ${ex?.message}")
                // Bei fehlendem API-Key (IllegalStateException) NICHT retry-en —
                // sonst Endlos-Loop. Bei echten Netz-/Server-Fehlern: retry.
                if (ex?.message == "Kein Gemini-Key hinterlegt") Result.success()
                else retryOrFailure()
            } else {
                settings.setMonthlyReview(text, System.currentTimeMillis())
                try {
                    notifier.postOrDelay(
                        kind = AppNotification.MONATSRUECKBLICK,
                        notificationId = NOTIFICATION_ID,
                        title = "Dein Monatsrueckblick ist fertig",
                        body = "Tippen, um ihn vom Genie vorlesen zu lassen.",
                    )
                } catch (cancellation: kotlinx.coroutines.CancellationException) {
                    throw cancellation
                } catch (t: Throwable) {
                    Diag.w(DiagnosticArea.BRIEFING, TAG, "Monatsrueckblick gespeichert, Benachrichtigung fehlgeschlagen", t)
                }
                Result.success()
            }
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.BRIEFING, TAG, "MonthlyReviewWorker fehlgeschlagen", t)
            retryOrFailure()
        }
    }

    companion object {
        private const val TAG = "MonthlyReviewWorker"
        private const val NOTIFICATION_ID = 4714
        const val UNIQUE_NAME_PERIODIC = "monthly-review-periodic"
        const val UNIQUE_NAME_ONESHOT = "monthly-review-oneshot"
        const val KEY_FORCE = "force"
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    private fun retryOrFailure(): Result =
        if (runAttemptCount + 1 < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
}

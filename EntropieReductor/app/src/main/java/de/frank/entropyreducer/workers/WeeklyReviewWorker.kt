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
import de.frank.entropyreducer.domain.usecase.GenerateReviewUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.first

/**
 * Wochenrueckblick — Sonntag 19:00 (Spec §16.3).
 * Erzeugt narrativen Markdown-Text der letzten 7 Tage und cached ihn in AppSettings.
 *
 * Duplikat-Schutz: Wenn diese Kalenderwoche schon ein Rueckblick gespeichert ist,
 * exitet der Worker sofort. Schuetzt vor doppelten Gemini-Aufrufen bei
 * WorkManager-Backoff oder manuellem Refresh.
 */
@HiltWorker
class WeeklyReviewWorker @AssistedInject constructor(
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
            val lastAt = settings.lastWeeklyReviewAtMsFlow.first()
            if (!force && lastAt > 0 && isSameWeek(today, lastAt)) {
                Diag.i(DiagnosticArea.BRIEFING, TAG, "Wochenrueckblick für KW ${weekOf(today)} existiert bereits (force=false).")
                return Result.success()
            }

            val result = generator(GenerateReviewUseCase.Range.WEEK)
            val text = result.getOrNull()
            if (text.isNullOrBlank()) {
                val ex = result.exceptionOrNull()
                Diag.w(DiagnosticArea.BRIEFING, TAG, "Wochenrueckblick leer: ${ex?.message}")
                // Bei fehlendem API-Key (IllegalStateException) NICHT retry-en —
                // sonst Endlos-Loop. Bei echten Netz-/Server-Fehlern: retry.
                if (ex is IllegalStateException) Result.success() else Result.retry()
            } else {
                settings.setWeeklyReview(text, System.currentTimeMillis())
                notifier.postOrDelay(
                    notificationId = NOTIFICATION_ID,
                    title = "Dein Wochenrueckblick ist fertig",
                    body = "Tippen, um ihn vom Genie vorlesen zu lassen.",
                )
                Result.success()
            }
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.BRIEFING, TAG, "WeeklyReviewWorker fehlgeschlagen", t)
            Result.retry()
        }
    }

    private fun isSameWeek(today: LocalDate, lastAtMs: Long): Boolean {
        val lastDate = Instant.ofEpochMilli(lastAtMs)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        return today.year == lastDate.year && weekOf(today) == weekOf(lastDate)
    }

    private fun weekOf(date: LocalDate): Int {
        val field = WeekFields.of(Locale.GERMANY).weekOfWeekBasedYear()
        return date.get(field)
    }

    companion object {
        private const val TAG = "WeeklyReviewWorker"
        private const val NOTIFICATION_ID = 4713
        const val UNIQUE_NAME_PERIODIC = "weekly-review-periodic"
        const val UNIQUE_NAME_ONESHOT = "weekly-review-oneshot"
        const val KEY_FORCE = "force"
    }
}

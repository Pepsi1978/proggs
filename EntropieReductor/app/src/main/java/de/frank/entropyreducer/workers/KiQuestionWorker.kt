package de.frank.entropyreducer.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.repository.KiQuestionRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.kiquestion.KiQuestionGenerator
import de.frank.entropyreducer.domain.notifications.AppNotification
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * Prueft alle 30 Min die fuenf Trigger aus §10.4 und schreibt die naechste Frage in
 * den KiQuestionRepository. Die UI beobachtet den Repository-Flow und zeigt die
 * Frage in der KI-Frage-Card auf Dashboard 1.
 */
@HiltWorker
class KiQuestionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val entryDao: EntropyEntryDao,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val calendarDao: CalendarDayDao,
    private val repo: KiQuestionRepository,
    private val settings: AppSettings,
    private val generator: KiQuestionGenerator,
    private val notifier: ShiftAwareNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            val dismissedUntil = repo.dismissedUntilMs.first()
            if (dismissedUntil > now) {
                Diag.i(DiagnosticArea.AGENTIC, TAG, "Frage ist noch snoozed bis ${java.util.Date(dismissedUntil)}")
                return Result.success()
            }

            val today = LocalDate.now(ZoneId.systemDefault())
            val tomorrow = today.plusDays(1)
            val entries = entryDao.getActive().first()
            val latest = biomarkerDao.getLatest().first()
            val todayDay = calendarDao.getDay(today.toString()).first()
            val tomorrowDay = calendarDao.getDay(tomorrow.toString()).first()

            val previous = repo.currentQuestion.first()
            val question = generator.generate(entries, latest, todayDay, tomorrowDay, now)
            repo.setCurrent(question)
            settings.setLastKiQuestionCheck(now)
            Diag.i(DiagnosticArea.AGENTIC, TAG, "KI-Frage-Check: ${question?.triggerKey ?: "keine Frage"}")

            // Schichtbewusste Notification, nur bei neuer Frage.
            if (question != null && previous?.triggerKey != question.triggerKey) {
                try {
                    notifier.postOrDelay(
                        kind = AppNotification.KI_FRAGE,
                        notificationId = NOTIFICATION_ID,
                        title = "Frage des Moments",
                        body = question.text,
                    )
                } catch (cancellation: kotlinx.coroutines.CancellationException) {
                    throw cancellation
                } catch (t: Throwable) {
                    Diag.w(DiagnosticArea.AGENTIC, TAG, "KI-Frage gespeichert, Benachrichtigung fehlgeschlagen", t)
                }
            }
            Result.success()
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (e: Throwable) {
            Diag.e(DiagnosticArea.AGENTIC, TAG, "KI-Frage-Worker fehlgeschlagen", e)
            retryOrFailure()
        }
    }

    companion object {
        private const val TAG = "KiQuestionWorker"
        private const val NOTIFICATION_ID = 4711
        const val UNIQUE_NAME_PERIODIC = "ki-question-periodic"
        const val UNIQUE_NAME_ONESHOT = "ki-question-oneshot"
    }
}

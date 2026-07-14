package de.frank.entropyreducer.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.domain.usecase.GenerateKiTriggersUseCase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * KI-Trigger-Engine Worker (Spec §16.2). Worker laeuft täglich 11:00,
 * arbeitet aber nur an Mittwoch + Sonntag — sonst sofortiger Erfolg.
 */
@HiltWorker
class KiTriggerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val generator: GenerateKiTriggersUseCase,
    private val notifier: ShiftAwareNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val force = inputData.getBoolean(KEY_FORCE, false)
            val today = LocalDate.now(ZoneId.systemDefault()).dayOfWeek
            if (!force && today != DayOfWeek.WEDNESDAY && today != DayOfWeek.SUNDAY) {
                Diag.i(DiagnosticArea.AGENTIC, TAG, "Heute ist $today, KI-Trigger-Engine pausiert (force=false).")
                return Result.success()
            }

            val result = generator()
            val failure = result.exceptionOrNull()
            if (failure != null) {
                Diag.w(DiagnosticArea.AGENTIC, TAG, "KI-Trigger-Engine fehlgeschlagen: ${failure.message}")
                return if (failure.message == "Kein Gemini-Key hinterlegt") {
                    Result.success()
                } else {
                    retryOrFailure()
                }
            }
            val count = result.getOrThrow()
            Diag.i(DiagnosticArea.AGENTIC, TAG, "KI-Trigger-Engine: $count neue Vorschlaege erzeugt (force=$force).")
            if (count > 0) {
                try {
                    notifier.postOrDelay(
                        notificationId = NOTIFICATION_ID,
                        title = "Die KI schlaegt $count neue Trigger vor",
                        body = "Tippen, um anzusehen — annehmen oder ablehnen.",
                    )
                } catch (cancellation: kotlinx.coroutines.CancellationException) {
                    throw cancellation
                } catch (t: Throwable) {
                    Diag.w(DiagnosticArea.AGENTIC, TAG, "KI-Trigger gespeichert, Benachrichtigung fehlgeschlagen", t)
                }
            }
            Result.success()
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.AGENTIC, TAG, "KiTriggerWorker fehlgeschlagen mit harter Exception", t)
            retryOrFailure()
        }
    }

    companion object {
        private const val TAG = "KiTriggerWorker"
        private const val NOTIFICATION_ID = 4715
        const val UNIQUE_NAME_PERIODIC = "ki-trigger-periodic"
        const val UNIQUE_NAME_ONESHOT = "ki-trigger-oneshot"
        const val KEY_FORCE = "force"
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    private fun retryOrFailure(): Result =
        if (runAttemptCount + 1 < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
}

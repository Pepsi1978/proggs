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
import de.frank.entropyreducer.domain.usecase.GenerateDailyBriefingUseCase
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * Erstellt das Tagesbriefing — täglich kurz nach dem Aufwachen (Spec Stufe 4 #2).
 *
 * Schreibt Ergebnis in AppSettings (text + Datum + Zeitstempel). Dashboard 1
 * beobachtet den Flow und zeigt es als Card mit "Anhoeren"-Button (TtsPlayer).
 *
 * Schichtaware: Wenn der Benutzer nachts arbeitet, weicht die Wachzeit ab —
 * deshalb laeuft der Worker als kurzer Polling-Job (alle 90 Min) und prueft
 * selbst, ob heute schon ein Briefing gemacht wurde. So entfaellt komplexes
 * Schichtfenster-Mapping im Scheduler.
 */
@HiltWorker
class DailyBriefingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settings: AppSettings,
    private val generator: GenerateDailyBriefingUseCase,
    private val notifier: ShiftAwareNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val force = inputData.getBoolean(KEY_FORCE, false)
            val today = LocalDate.now(ZoneId.systemDefault()).toString()
            val storedDate = settings.dailyBriefingDateFlow.first()
            if (!force && storedDate == today) {
                Diag.i(DiagnosticArea.BRIEFING, TAG, "Tagesbriefing für $today existiert bereits — skip (force=false).")
                return Result.success()
            }

            val result = generator()
            val text = result.getOrNull()
            if (text.isNullOrBlank()) {
                val ex = result.exceptionOrNull()
                Diag.w(DiagnosticArea.BRIEFING, TAG, "Tagesbriefing-Generierung fehlgeschlagen: ${ex?.message}")
                // Wenn API-Key fehlt: Worker NICHT retry-en — sonst lauft er endlos
                // bis der Benutzer den Key hinterlegt. Erst beim naechsten Polling-
                // Lauf (90 Min) wird neu versucht.
                return if (ex?.message == "Kein Gemini-Key hinterlegt") {
                    Result.success()
                } else {
                    retryOrFailure()
                }
            }

            settings.setDailyBriefing(text, today, System.currentTimeMillis())
            Diag.i(DiagnosticArea.BRIEFING, TAG, "Tagesbriefing gespeichert (${text.length} Zeichen)")

            try {
                notifier.postOrDelay(
                    notificationId = NOTIFICATION_ID,
                    title = "Dein Tagesbriefing ist bereit",
                    body = "Tippen, um es vom Genie vorlesen zu lassen.",
                )
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                Diag.w(DiagnosticArea.BRIEFING, TAG, "Tagesbriefing gespeichert, Benachrichtigung fehlgeschlagen", t)
            }
            Result.success()
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.BRIEFING, TAG, "DailyBriefingWorker fehlgeschlagen", t)
            retryOrFailure()
        }
    }

    companion object {
        private const val TAG = "DailyBriefingWorker"
        private const val NOTIFICATION_ID = 4712
        const val UNIQUE_NAME_PERIODIC = "daily-briefing-periodic"
        const val UNIQUE_NAME_ONESHOT = "daily-briefing-oneshot"
        const val KEY_FORCE = "force"
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    private fun retryOrFailure(): Result =
        if (runAttemptCount + 1 < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()

}

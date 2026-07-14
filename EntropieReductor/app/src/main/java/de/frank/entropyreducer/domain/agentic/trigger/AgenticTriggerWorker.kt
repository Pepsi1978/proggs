package de.frank.entropyreducer.domain.agentic.trigger

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.repository.PromptTriggerRepository
import de.frank.entropyreducer.domain.agentic.WorkflowEvent
import de.frank.entropyreducer.domain.agentic.WorkflowRunner
import de.frank.entropyreducer.domain.model.TriggerSource
import de.frank.entropyreducer.workers.retryOrFailure
import kotlinx.coroutines.flow.collect

/**
 * Periodischer Worker fuer agentic-Auto-Trigger (Frank-Wunsch 2026-05-21).
 *
 * Laeuft alle 15 Minuten (kuerzeres Intervall ist von Android nicht erlaubt
 * fuer PeriodicWorkRequests). Bei jedem Aufruf:
 *  1. faellige CRON-Trigger holen (nextScheduledAt <= now)
 *  2. fuer jeden: WorkflowRunner.run(triggerSource=SCHEDULED) collect bis
 *     Finished
 *  3. nextScheduledAt neu berechnen via SimpleCronParser und in DB schreiben
 *
 * Notification wird hier NICHT gezeigt — der WorkflowRunner loggt alles in
 * prompt_executions. Frank sieht das im HistoryScreen. Falls spaeter
 * Notifications gewuenscht: Hook hier einbauen.
 *
 * Achtung: dieser Worker laeuft ohne UI-Confirm-Dialog. Write-Tools die
 * Frank nicht in den Trust-Modus gesetzt hat werden BLOCKIERT (Status
 * BLOCKED_BY_USER_REJECT nach 60s Timeout im UiConfirmationGate). Das ist
 * die richtige Sicherheits-Voreinstellung — Frank sieht den Block im
 * Verlauf und kann den Prompt anpassen.
 */
@HiltWorker
class AgenticTriggerWorker
@AssistedInject
constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val triggerRepo: PromptTriggerRepository,
    private val promptRepo: PromptRepository,
    private val workflowRunner: WorkflowRunner,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            val due = triggerRepo.getDueCronTriggers(now)
            if (due.isEmpty()) {
                Diag.d(DiagnosticArea.AGENTIC, TAG, "Keine faelligen Trigger.")
                return Result.success()
            }

            Diag.i(DiagnosticArea.AGENTIC, TAG, "Faellige Trigger: ${due.size}")

            for (trigger in due) {
                // Direktive 3 Loop-2-Fix (war LOOP-2-3-Bug): Prompt.isActive
                // pruefen. Wenn Frank den Prompt deaktiviert hat, soll der
                // Auto-Trigger NICHT mehr feuern — bisher feuerte er trotzdem
                // weil nur prompt_triggers.isActive geprueft wurde, nicht
                // saved_prompts.isActive.
                val prompt = promptRepo.getById(trigger.promptId)
                if (prompt == null) {
                    Diag.w(DiagnosticArea.AGENTIC, TAG, "Trigger ${trigger.id} zeigt auf nicht-existenten Prompt — skip + reschedule")
                    rescheduleNext(trigger, now)
                    continue
                }
                if (!prompt.isActive) {
                    Diag.i(DiagnosticArea.AGENTIC, 
                        TAG,
                        "Trigger ${trigger.id} fuer deaktivierten Prompt '${prompt.name}' " +
                            "uebersprungen. Trigger bleibt aktiv — feuert wieder sobald " +
                            "der Prompt aktiviert wird.",
                    )
                    rescheduleNext(trigger, now)
                    continue
                }
                Diag.i(DiagnosticArea.AGENTIC, TAG, "Starte Auto-Run fuer Prompt ${trigger.promptId} via Trigger ${trigger.id}")
                runOne(trigger)
                rescheduleNext(trigger, now)
            }
            Result.success()
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.AGENTIC, TAG, "AgenticTriggerWorker fehlgeschlagen", t)
            retryOrFailure()
        }
    }

    private suspend fun runOne(
        trigger: de.frank.entropyreducer.data.local.entities.PromptTriggerEntity
    ) {
        try {
            workflowRunner.run(
                promptId = trigger.promptId,
                userInputContext = null,
                triggerSource = TriggerSource.SCHEDULED,
            ).collect { event ->
                if (event is WorkflowEvent.Finished) {
                    Diag.i(DiagnosticArea.AGENTIC, 
                        TAG,
                        "Auto-Run beendet: status=${event.status}, " +
                            "tokens=${event.tokensTotal}, tools=${event.toolCallCount}",
                    )
                }
            }
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.AGENTIC, TAG, "Auto-Run fuer ${trigger.id} fehlgeschlagen", t)
        }
    }

    private suspend fun rescheduleNext(
        trigger: de.frank.entropyreducer.data.local.entities.PromptTriggerEntity,
        firedAt: Long,
    ) {
        val cron = trigger.cronExpression
        if (cron.isNullOrBlank()) {
            triggerRepo.markFired(trigger.id, firedAt, null)
            return
        }
        val nextAt = SimpleCronParser.nextFireAt(cron, firedAt)
        triggerRepo.markFired(trigger.id, firedAt, nextAt)
    }

    companion object {
        const val TAG = "AgenticTriggerWorker"
        const val UNIQUE_NAME_PERIODIC = "agentic-trigger-periodic"
    }
}

package de.frank.entropyreducer.domain.agentic.trigger

import android.util.Log
import dagger.Lazy
import de.frank.entropyreducer.data.repository.PromptTriggerRepository
import de.frank.entropyreducer.domain.agentic.WorkflowEvent
import de.frank.entropyreducer.domain.agentic.WorkflowRunner
import de.frank.entropyreducer.domain.model.TriggerSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Loest Chain-Trigger nach erfolgreichem Lauf eines Prompts aus (Frank-Wunsch
 * 2026-05-21).
 *
 * Mechanismus:
 *  - Nach SUCCESS-Status eines Runs ruft der WorkflowRunner
 *    `notifySuccess(promptId)` auf
 *  - Dieser Notifier sucht alle aktiven CHAIN-Trigger mit
 *    chainAfterPromptId = promptId
 *  - Fuer jeden gefundenen Chain-Trigger: WorkflowRunner.run() im
 *    eigenen ApplicationScope (fire-and-forget)
 *
 * Wichtig (Hilt-Zyklus): WorkflowRunner injiziert ChainTriggerNotifier,
 * ChainTriggerNotifier braucht WorkflowRunner zum Triggern. Deshalb wird
 * `WorkflowRunner` als `Lazy<WorkflowRunner>` injiziert um den Zyklus
 * aufzuloesen.
 *
 * Zyklenschutz: Chain-Runs sind als TriggerSource.CHAINED markiert. Wenn
 * ein CHAINED-Run wiederum erfolgreich ist, koennte er weitere Chains
 * ausloesen — das ist gewollt (Chain-of-Chains). Frank ist selber dafuer
 * verantwortlich keine Endlosschleife zu bauen (z.B. wenn Prompt A nach
 * Prompt B startet UND umgekehrt).
 */
@Singleton
class ChainTriggerNotifier
@Inject
constructor(
    private val triggerRepo: PromptTriggerRepository,
    private val workflowRunnerLazy: Lazy<WorkflowRunner>,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Vom WorkflowRunner aufgerufen nach erfolgreichem Lauf. Sucht
     * Chain-Trigger und startet sie fire-and-forget.
     */
    fun notifySuccess(promptId: String) {
        scope.launch {
            try {
                val chainTriggers = triggerRepo.getChainTriggersAfter(promptId)
                if (chainTriggers.isEmpty()) return@launch
                Log.i(TAG, "Chain-Trigger nach Erfolg von $promptId: ${chainTriggers.size}")
                for (trigger in chainTriggers) {
                    launch { runChainOne(trigger) }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "ChainTriggerNotifier fehlgeschlagen", t)
            }
        }
    }

    private suspend fun runChainOne(
        trigger: de.frank.entropyreducer.data.local.entities.PromptTriggerEntity
    ) {
        try {
            Log.i(TAG, "Starte Chain-Run fuer Prompt ${trigger.promptId} via Trigger ${trigger.id}")
            workflowRunnerLazy
                .get()
                .run(
                    promptId = trigger.promptId,
                    userInputContext = null,
                    triggerSource = TriggerSource.CHAINED,
                )
                .collect { event ->
                    if (event is WorkflowEvent.Finished) {
                        Log.i(
                            TAG,
                            "Chain-Run beendet: status=${event.status}, " +
                                "tokens=${event.tokensTotal}",
                        )
                    }
                }
            triggerRepo.markFired(trigger.id, System.currentTimeMillis(), null)
        } catch (t: Throwable) {
            Log.e(TAG, "Chain-Run fuer ${trigger.id} fehlgeschlagen", t)
        }
    }

    companion object {
        private const val TAG = "ChainTriggerNotifier"
    }
}

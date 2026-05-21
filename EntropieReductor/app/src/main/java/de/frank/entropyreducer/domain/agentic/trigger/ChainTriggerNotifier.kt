package de.frank.entropyreducer.domain.agentic.trigger

import android.util.Log
import dagger.Lazy
import de.frank.entropyreducer.data.repository.PromptTriggerRepository
import de.frank.entropyreducer.domain.agentic.WorkflowEvent
import de.frank.entropyreducer.domain.agentic.WorkflowRunner
import de.frank.entropyreducer.domain.model.TriggerSource
import java.util.concurrent.ConcurrentHashMap
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
     * Zyklus-Schutz (Direktive 3 Loop-1-Fix, war HIGH-2-Bug):
     * Verhindert dass A→B→A in unter [CHAIN_COOLDOWN_MS] eine Endlosschleife
     * ausloest. Jede promptId die als Chain-Quelle gefeuert hat landet hier
     * mit ihrem Timestamp. Beim naechsten Aufruf wird gepueft ob die gleiche
     * Quelle in den letzten 30 Sekunden schon Chains gestartet hat. Wenn ja:
     * Skip und Log-Warning.
     *
     * Plus: max-depth via TARGET-PromptId-Tracking. Wenn die gleiche
     * Ziel-PromptId in der letzten Cooldown-Periode mehr als 3x als
     * Chain-Ziel war, wird sie blockiert (defense against fan-out-amplification).
     */
    private val recentlyFiredSources = ConcurrentHashMap<String, Long>()
    private val targetTriggerCount = ConcurrentHashMap<String, MutableList<Long>>()

    /**
     * Vom WorkflowRunner aufgerufen nach erfolgreichem Lauf. Sucht
     * Chain-Trigger und startet sie fire-and-forget.
     */
    fun notifySuccess(promptId: String) {
        val now = System.currentTimeMillis()

        // Cleanup: alte Eintraege rausnehmen (>= CHAIN_COOLDOWN_MS alt)
        recentlyFiredSources.entries.removeIf { now - it.value > CHAIN_COOLDOWN_MS }
        targetTriggerCount.values.forEach { list ->
            list.removeAll { now - it > CHAIN_COOLDOWN_MS }
        }

        // Zyklus-Erkennung Schicht 1: gleiche Quelle in Cooldown blockiert
        val lastFire = recentlyFiredSources[promptId]
        if (lastFire != null && now - lastFire < CHAIN_COOLDOWN_MS) {
            Log.w(
                TAG,
                "Chain-Notify fuer $promptId uebersprungen — schon in den letzten " +
                    "${CHAIN_COOLDOWN_MS / 1000}s gefeuert (Zyklus-Schutz)",
            )
            return
        }
        recentlyFiredSources[promptId] = now

        scope.launch {
            try {
                val chainTriggers = triggerRepo.getChainTriggersAfter(promptId)
                if (chainTriggers.isEmpty()) return@launch
                Log.i(TAG, "Chain-Trigger nach Erfolg von $promptId: ${chainTriggers.size}")
                for (trigger in chainTriggers) {
                    // Zyklus-Erkennung Schicht 2: target fan-out limit
                    val targetCounts =
                        targetTriggerCount.getOrPut(trigger.promptId) { mutableListOf() }
                    if (targetCounts.size >= MAX_CHAIN_FIRES_PER_TARGET) {
                        Log.w(
                            TAG,
                            "Chain-Ziel ${trigger.promptId} hat schon " +
                                "$MAX_CHAIN_FIRES_PER_TARGET Trigger in der Cooldown-Periode — " +
                                "weiterer Aufruf blockiert (Fan-Out-Schutz)",
                        )
                        continue
                    }
                    targetCounts.add(now)
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
        /** Zeitfenster fuer Zyklus-Erkennung. */
        private const val CHAIN_COOLDOWN_MS = 30_000L
        /** Maximale Chain-Trigger fuer die gleiche Ziel-PromptId pro Cooldown-Fenster. */
        private const val MAX_CHAIN_FIRES_PER_TARGET = 3
    }
}

package de.frank.entropyreducer.domain.agentic.trigger

import dagger.Lazy
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.repository.PromptRepository
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Collections

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
    private val promptRepo: PromptRepository,
    private val workflowRunnerLazy: Lazy<WorkflowRunner>,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Loop-5-Fix (L5-5): Globales Concurrency-Limit fuer Chain-Runs.
     * Verhindert dass eine Erfolgskaskade (Prompt A loest 10 Chains aus)
     * 10 parallele Gemini-Calls feuert die alle das Token-Budget parallel
     * anzapfen. 2 parallel ist ein guter Kompromiss zwischen Speed und
     * Schutz vor Token-Quota-Explosion.
     */
    private val concurrencyLimiter = Semaphore(permits = 2)

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
    /**
     * Direktive 3 Loop-4-Fix (war L4-1-Bug): ConcurrentHashMap-Wert war eine
     * MutableList die NICHT thread-safe ist. Bei parallelen Chain-Triggern auf
     * dasselbe Ziel wuerde add()/removeAll() eine ConcurrentModificationException
     * werfen. Loesung: synchronizedList() wrap — die outer-Map bleibt
     * ConcurrentHashMap, der Wert wird intern synchronisiert. Plus: alle
     * Zugriffe auf die List werden in synchronized(list) gehuellt um composite-
     * Aktionen (iterieren + adden) atomar zu machen.
     */
    private val targetTriggerCount =
        ConcurrentHashMap<String, MutableList<Long>>()

    /**
     * Vom WorkflowRunner aufgerufen nach erfolgreichem Lauf. Sucht
     * Chain-Trigger und startet sie fire-and-forget.
     */
    fun notifySuccess(promptId: String) {
        val now = System.currentTimeMillis()

        // Cleanup: alte Eintraege rausnehmen (>= CHAIN_COOLDOWN_MS alt)
        recentlyFiredSources.entries.removeIf { now - it.value > CHAIN_COOLDOWN_MS }
        // Loop-4-Fix L4-1: synchronized um composite-Aktion (iterieren + remove)
        targetTriggerCount.values.forEach { list ->
            synchronized(list) { list.removeAll { now - it > CHAIN_COOLDOWN_MS } }
        }

        // Zyklus-Erkennung Schicht 1: gleiche Quelle in Cooldown blockiert
        var sourceReserved = false
        recentlyFiredSources.compute(promptId) { _, lastFire ->
            if (lastFire != null && now - lastFire < CHAIN_COOLDOWN_MS) {
                lastFire
            } else {
                sourceReserved = true
                now
            }
        }
        if (!sourceReserved) {
            Diag.w(DiagnosticArea.AGENTIC, 
                TAG,
                "Chain-Notify fuer $promptId uebersprungen — schon in den letzten " +
                    "${CHAIN_COOLDOWN_MS / 1000}s gefeuert (Zyklus-Schutz)",
            )
            return
        }
        scope.launch {
            try {
                val chainTriggers = triggerRepo.getChainTriggersAfter(promptId)
                if (chainTriggers.isEmpty()) return@launch
                Diag.i(DiagnosticArea.AGENTIC, TAG, "Chain-Trigger nach Erfolg von $promptId: ${chainTriggers.size}")
                for (trigger in chainTriggers) {
                    // Zyklus-Erkennung Schicht 2: target fan-out limit.
                    // Loop-4-Fix L4-1: check-then-add MUSS atomar sein, sonst
                    // koennen zwei parallel Chains denselben Slot doppelt
                    // nehmen. synchronized(list) macht die composite-Aktion
                    // atomar.
                    if (wouldCreateCycle(promptId, trigger.promptId)) {
                        Diag.w(
                            DiagnosticArea.AGENTIC,
                            TAG,
                            "Chain ${promptId} -> ${trigger.promptId} blockiert: " +
                                "der konfigurierte Triggergraph enthaelt einen Zyklus",
                        )
                        continue
                    }
                    val targetCounts = targetTriggerCount.computeIfAbsent(trigger.promptId) {
                        Collections.synchronizedList(mutableListOf())
                    }
                    val accepted =
                        synchronized(targetCounts) {
                            if (targetCounts.size >= MAX_CHAIN_FIRES_PER_TARGET) {
                                false
                            } else {
                                targetCounts.add(now)
                                true
                            }
                        }
                    if (!accepted) {
                        Diag.w(DiagnosticArea.AGENTIC, 
                            TAG,
                            "Chain-Ziel ${trigger.promptId} hat schon " +
                                "$MAX_CHAIN_FIRES_PER_TARGET Trigger in der Cooldown-Periode — " +
                                "weiterer Aufruf blockiert (Fan-Out-Schutz)",
                        )
                        continue
                    }
                    // L5-5: Semaphore beschraenkt globale Chain-Concurrency
                    launch { concurrencyLimiter.withPermit { runChainOne(trigger) } }
                }
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                Diag.e(DiagnosticArea.AGENTIC, TAG, "ChainTriggerNotifier fehlgeschlagen", t)
            }
        }
    }

    /**
     * Prueft den persistenten Triggergraphen vor dem Start. Der bisherige
     * Zeitfenster-Schutz allein konnte A -> B -> A nicht stoppen, wenn einzelne
     * Workflows laenger als das Cooldown-Fenster liefen.
     */
    private suspend fun wouldCreateCycle(sourcePromptId: String, targetPromptId: String): Boolean {
        if (sourcePromptId == targetPromptId) return true
        val pending = ArrayDeque<String>()
        val visited = mutableSetOf<String>()
        pending.add(targetPromptId)

        while (pending.isNotEmpty() && visited.size < MAX_CHAIN_GRAPH_NODES) {
            val current = pending.removeFirst()
            if (!visited.add(current)) continue
            if (current == sourcePromptId) return true
            triggerRepo.getChainTriggersAfter(current).forEach { next ->
                if (next.promptId !in visited) pending.add(next.promptId)
            }
        }
        return pending.isNotEmpty()
    }

    private suspend fun runChainOne(
        trigger: de.frank.entropyreducer.data.local.entities.PromptTriggerEntity
    ) {
        try {
            // Direktive 3 Loop-2-Fix (war LOOP-2-3-Bug Schicht 2): auch
            // Chain-Trigger pruefen Prompt.isActive vor dem Start.
            val targetPrompt = promptRepo.getById(trigger.promptId)
            if (targetPrompt == null || !targetPrompt.isActive) {
                Diag.i(DiagnosticArea.AGENTIC, 
                    TAG,
                    "Chain-Ziel ${trigger.promptId} existiert nicht oder ist deaktiviert — skip",
                )
                return
            }
            Diag.i(DiagnosticArea.AGENTIC, TAG, "Starte Chain-Run fuer Prompt ${trigger.promptId} via Trigger ${trigger.id}")
            workflowRunnerLazy
                .get()
                .run(
                    promptId = trigger.promptId,
                    userInputContext = null,
                    triggerSource = TriggerSource.CHAINED,
                )
                .collect { event ->
                    if (event is WorkflowEvent.Finished) {
                        Diag.i(DiagnosticArea.AGENTIC, 
                            TAG,
                            "Chain-Run beendet: status=${event.status}, " +
                                "tokens=${event.tokensTotal}",
                        )
                    }
                }
            triggerRepo.markFired(trigger.id, System.currentTimeMillis(), null)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.AGENTIC, TAG, "Chain-Run fuer ${trigger.id} fehlgeschlagen", t)
        }
    }

    companion object {
        private const val TAG = "ChainTriggerNotifier"
        /** Zeitfenster fuer Zyklus-Erkennung. */
        private const val CHAIN_COOLDOWN_MS = 30_000L
        /** Maximale Chain-Trigger fuer die gleiche Ziel-PromptId pro Cooldown-Fenster. */
        private const val MAX_CHAIN_FIRES_PER_TARGET = 3
        /** Defensives Suchlimit fuer versehentlich extrem grosse Triggergraphen. */
        private const val MAX_CHAIN_GRAPH_NODES = 1_000
    }
}

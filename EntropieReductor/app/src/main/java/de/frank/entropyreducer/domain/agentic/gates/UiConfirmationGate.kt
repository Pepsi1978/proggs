package de.frank.entropyreducer.domain.agentic.gates

import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.repository.PromptToolPermissionRepository
import de.frank.entropyreducer.domain.model.ConfirmDecision
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * UI-Variante des ConfirmationGate (Frank-Wunsch 2026-05-21).
 *
 * Anders als AutoApproveConfirmationGate wartet diese Implementation auf eine
 * UI-Entscheidung. Mechanismus ueber StateFlow + Continuation:
 *
 *  1. WorkflowRunner ruft `request(req)` auf
 *  2. Pre-Check Trust-Modus: wenn promptTrust ODER toolTrust → AUTO_APPROVED
 *     (kein Dialog)
 *  3. Sonst: pendingRequest.value = req, suspendiere Coroutine
 *  4. UI beobachtet pendingRequest, zeigt Confirm-Dialog
 *  5. Frank tippt "Genehmigen" oder "Ablehnen" → ViewModel ruft `respond(...)`
 *  6. respond() resumes Coroutine mit ConfirmDecision
 *  7. WorkflowRunner faehrt mit der Entscheidung fort
 *
 * Timeout: 60 Sekunden ohne Antwort → TIMED_OUT (wird als REJECTED behandelt).
 *
 * Nur EIN ausstehender Request gleichzeitig — wenn ein neuer Request rein
 * kommt waehrend ein alter offen ist, wird der alte als TIMED_OUT abgelehnt.
 */
@Singleton
class UiConfirmationGate
@Inject
constructor(
    private val permissionRepo: PromptToolPermissionRepository,
    private val promptRepo: PromptRepository,
) : ConfirmationGate {

    private val _pendingRequest = MutableStateFlow<ConfirmationRequest?>(null)
    val pendingRequest: StateFlow<ConfirmationRequest?> = _pendingRequest.asStateFlow()

    /**
     * Thread-sicher (Direktive 3 Loop-1-Fix, war HIGH-3-Bug): vorher war
     * pendingContinuation ein einfaches var — read+write von verschiedenen
     * Dispatchers konnte TOCTOU verursachen. AtomicReference + zusaetzliches
     * Mutex fuer den request-Pfad garantieren dass nur EINE Continuation
     * gleichzeitig aktiv ist und respond() immer die richtige aufweckt.
     */
    private val pendingContinuationRef =
        AtomicReference<CancellableContinuation<ConfirmationResult>?>(null)
    private val requestMutex = Mutex()

    override suspend fun request(req: ConfirmationRequest): ConfirmationResult {
        // 1. Trust-Modus pruefen
        val toolPerm = permissionRepo.getOne(req.promptId, req.tool.name)
        val promptTrustDefault = promptRepo.getById(req.promptId)?.trustModeDefault ?: false
        val effectiveTrust = (toolPerm?.trustMode == true) || promptTrustDefault
        if (effectiveTrust) {
            return ConfirmationResult(
                decision = ConfirmDecision.AUTO_APPROVED,
                rejectReason = null,
            )
        }

        // 2. Background-Schutz (Direktive 3 Loop-1-Fix, war MED-2-Bug):
        // Wenn der Run im Hintergrund laeuft (TriggerSource SCHEDULED/CHAINED/EVENT),
        // gibt es keinen User der den Dialog beantworten kann. Statt 60s zu warten
        // sofort als REJECTED zurueckgeben — sauber und schnell. Frank's intentionaler
        // Schutz: Background-Runs muessen Trust-Modus pro Tool haben.
        if (req.isBackground) {
            return ConfirmationResult(
                decision = ConfirmDecision.REJECTED,
                rejectReason =
                    "Background-Run ohne Trust-Modus — Schreib-Tools brauchen Trust " +
                        "wenn sie ohne UI-Dialog laufen sollen.",
            )
        }

        // 3. Mutex-geschuetzte Pre-Verdraengung: alten Pending-Request beenden
        requestMutex.withLock {
            val previous = pendingContinuationRef.getAndSet(null)
            if (previous != null && previous.isActive) {
                previous.resume(
                    ConfirmationResult(
                        decision = ConfirmDecision.TIMED_OUT,
                        rejectReason = "Vom naechsten Confirm-Request verdraengt",
                    )
                )
            }
        }

        // 4. Neuer Request → Coroutine suspendieren, auf UI warten.
        // Loop-5-Fix (L5-4): wir merken uns die LOKALE Continuation und
        // nutzen sie fuer compareAndSet im Cleanup — sonst wuerde ein
        // frischer Request der zwischen withTimeoutOrNull und der
        // Cleanup-Zeile gestartet wird hier fuelschlich auf null gesetzt
        // werden (Race-Window).
        var ourContinuation: CancellableContinuation<ConfirmationResult>? = null
        val timeoutResult: ConfirmationResult? =
            withTimeoutOrNull(60_000L) {
                suspendCancellableCoroutine<ConfirmationResult> { cont ->
                    ourContinuation = cont
                    pendingContinuationRef.set(cont)
                    _pendingRequest.value = req
                    cont.invokeOnCancellation {
                        _pendingRequest.value = null
                        // CAS: nur loeschen wenn wir noch die Aktive sind
                        pendingContinuationRef.compareAndSet(cont, null)
                    }
                }
            }

        // Cleanup nur unserer eigenen Continuation (L5-4 Fix):
        // compareAndSet statt getAndSet — falls schon ein neuer Request
        // ourContinuation verdraengt hat, wird sein Slot nicht beruehrt.
        ourContinuation?.let { pendingContinuationRef.compareAndSet(it, null) }
        _pendingRequest.compareAndSet(req, null)

        return timeoutResult
            ?: ConfirmationResult(
                decision = ConfirmDecision.TIMED_OUT,
                rejectReason = "Keine Antwort innerhalb 60 Sekunden",
            )
    }

    /**
     * Von der UI aufgerufen wenn Frank den Confirm-Dialog beantwortet.
     * Thread-sicher via AtomicReference.getAndSet — selbst bei parallelem
     * Aufruf gewinnt der erste, der zweite faengt null und kehrt ohne Wirkung
     * zurueck.
     */
    fun respond(decision: ConfirmDecision, rejectReason: String? = null) {
        val cont = pendingContinuationRef.getAndSet(null) ?: return
        if (cont.isActive) {
            cont.resume(ConfirmationResult(decision = decision, rejectReason = rejectReason))
        }
        _pendingRequest.value = null
    }
}

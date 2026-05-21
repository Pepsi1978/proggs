package de.frank.entropyreducer.domain.agentic.gates

import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.repository.PromptToolPermissionRepository
import de.frank.entropyreducer.domain.model.ConfirmDecision
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
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

    private var pendingContinuation:
        kotlinx.coroutines.CancellableContinuation<ConfirmationResult>? =
        null

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

        // 2. Alten Pending-Request (falls vorhanden) als TIMED_OUT ablehnen
        val previous = pendingContinuation
        if (previous != null && previous.isActive) {
            previous.resume(
                ConfirmationResult(
                    decision = ConfirmDecision.TIMED_OUT,
                    rejectReason = "Vom naechsten Confirm-Request verdraengt",
                )
            )
        }

        // 3. Neuer Request → Coroutine suspendieren, auf UI warten
        val timeoutResult: ConfirmationResult? =
            withTimeoutOrNull(60_000L) {
                suspendCancellableCoroutine<ConfirmationResult> { cont ->
                    pendingContinuation = cont
                    _pendingRequest.value = req
                    cont.invokeOnCancellation {
                        _pendingRequest.value = null
                        pendingContinuation = null
                    }
                }
            }

        // Cleanup falls nicht schon durch respond() passiert
        _pendingRequest.value = null
        pendingContinuation = null

        return timeoutResult
            ?: ConfirmationResult(
                decision = ConfirmDecision.TIMED_OUT,
                rejectReason = "Keine Antwort innerhalb 60 Sekunden",
            )
    }

    /**
     * Von der UI aufgerufen wenn Frank den Confirm-Dialog beantwortet.
     */
    fun respond(decision: ConfirmDecision, rejectReason: String? = null) {
        val cont = pendingContinuation ?: return
        if (cont.isActive) {
            cont.resume(ConfirmationResult(decision = decision, rejectReason = rejectReason))
        }
        _pendingRequest.value = null
        pendingContinuation = null
    }
}

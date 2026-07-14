package de.frank.entropyreducer.domain.agentic.gates

import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.model.ConfirmDecision
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID
import kotlinx.serialization.json.JsonElement

/**
 * Dritter Gate in der Pipeline. Fuer Write-Tools steht zwischen
 * "Permission OK" und "Tool wird ausgefuehrt" ein User-Confirm. Read-Tools
 * gehen ohne Confirm durch.
 *
 * Trust-Modus (Frank-Wunsch 2026-05-21):
 * - Pro Prompt: trustModeDefault (in saved_prompts)
 * - Pro Tool fuer einen Prompt: prompt_tool_permissions.trustMode
 * - WENN entweder das Prompt-Default ODER das Tool-Permission auf trust ist,
 *   wird der Dialog uebersprungen (AUTO_APPROVED)
 *
 * Implementierung: dieses Interface trennt die Logik ("wann Dialog noetig?")
 * von der UI-Mechanik ("zeige Dialog, warte auf Antwort"). Etappe 4 liefert
 * eine AutoApprove-Default-Impl die fuer Background-Runs (Auto-Trigger) und
 * Trust-Modus genutzt wird. Die UI-Variante mit echtem Dialog kommt in
 * Etappe 9.
 */
interface ConfirmationGate {
    /**
     * Confirm fuer einen geplanten Write-Tool-Aufruf einholen. Wenn Trust
     * aktiviert ist, returnt sofort AUTO_APPROVED ohne UI.
     *
     * Bei TIMED_OUT (Default 60s, vom WorkflowRunner gesteuert) gilt das als
     * REJECTED — Sicherheits-Default.
     */
    suspend fun request(req: ConfirmationRequest): ConfirmationResult
}

/**
 * Anfrage an den User: "Soll dieses Tool wirklich ausgefuehrt werden?"
 * Enthaelt alles was die UI fuer eine Vorschau braucht.
 */
data class ConfirmationRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val promptId: String,
    val executionId: String,
    val tool: AgenticTool,
    /** Args die Gemini fuer den Aufruf vorschlaegt. */
    val args: JsonElement,
    /**
     * Optional: Kurzbeschreibung was der Aufruf bewirken wird, z.B.
     * "3 neue Aufgaben werden angelegt". Wenn null, baut das UI aus
     * tool.name + args eine Default-Beschreibung.
     */
    val previewText: String? = null,
    /**
     * Direktive 3 Loop-1-Fix (war MED-2-Bug): true wenn der Run im Hintergrund
     * laeuft (TriggerSource SCHEDULED/CHAINED/EVENT). Der UiConfirmationGate
     * lehnt Background-Confirms sofort ab statt 60s zu warten — sonst wuerde
     * jeder Background-Run mit Schreib-Tools ohne Trust hartnaeckig haengen.
     */
    val isBackground: Boolean = false,
)

data class ConfirmationResult(
    val decision: ConfirmDecision,
    /** Wenn REJECTED: optionale Begruendung vom User. */
    val rejectReason: String? = null,
)

/**
 * Default-Implementation fuer Headless-Runs (Background-Trigger, Test-Runs).
 * Approved IMMER automatisch — wenn der WorkflowRunner sicherstellen will
 * dass nichts ohne Dialog passiert, muss er entweder eine UI-Variante des
 * ConfirmationGate injecten oder den Trust-Modus pro Tool ablehnen.
 *
 * Verhalten:
 * - Wenn Tool im Trust-Modus (prompt-default ODER tool-permission): AUTO_APPROVED
 * - Sonst: AUTO_APPROVED (weil Default-Impl) — das ist OK fuer Read-Tools weil
 *   PermissionGate sie bereits blockt wenn nicht erlaubt
 *
 * Achtung: Fuer Write-Tools ohne Trust-Modus muss in der UI-Variante (Etappe 9)
 * ein echter Dialog gezeigt werden. Die AutoApprove-Variante darf nicht in
 * Production-UI-Runs aktiv sein wenn echte Confirms erwartet werden.
 */
@Singleton
class AutoApproveConfirmationGate @Inject constructor() : ConfirmationGate {
    /**
     * Headless-Default: immer AUTO_APPROVED. Das ist sicher fuer
     * Background-Trigger und Test-Runs solange der PermissionGate vorher den
     * Aufruf gepueft hat. Production-UI sollte diese Implementierung
     * ueberschreiben (Etappe 9) damit Write-Tools ohne Trust-Modus einen
     * echten Dialog bekommen.
     */
    override suspend fun request(req: ConfirmationRequest): ConfirmationResult =
        ConfirmationResult(decision = ConfirmDecision.AUTO_APPROVED, rejectReason = null)
}

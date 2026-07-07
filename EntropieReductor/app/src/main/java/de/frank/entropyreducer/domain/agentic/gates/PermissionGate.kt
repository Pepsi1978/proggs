package de.frank.entropyreducer.domain.agentic.gates

import de.frank.entropyreducer.data.repository.PromptToolPermissionRepository
import de.frank.entropyreducer.domain.agentic.AgenticTool
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Erster Gate in der WorkflowRunner-Pipeline. Prueft VOR jedem Tool-Aufruf ob
 * dieser Prompt das Tool aufrufen darf.
 *
 * Regeln (Frank-Entscheidung 2026-05-21):
 * - Read-Tools (isWriteTool=false): IMMER erlaubt, kategorie-uebergreifend
 * - Write-Tools (isWriteTool=true): nur wenn prompt_tool_permissions.granted=true
 *
 * Wird vor dem Confirmation-Gate und vor dem TokenMeter ausgefuehrt. Wenn der
 * Aufruf hier blockiert wird, kommt der Workflow-Status auf BLOCKED_BY_PERMISSION
 * und der Run endet ohne dass das Tool ausgefuehrt wird.
 */
@Singleton
class PermissionGate
@Inject
constructor(private val permissionRepo: PromptToolPermissionRepository) {

    /**
     * Prueft ob das Tool fuer diesen Prompt erlaubt ist.
     *
     * Wichtig: Read-Tools werden hier nicht weiter eingeschraenkt — Frank kann
     * Read-Tools NICHT per UI sperren (sind immer aktiv). Falls das in Zukunft
     * gewuenscht sein sollte, ist hier die Stelle.
     */
    suspend fun check(promptId: String, tool: AgenticTool): GateDecision {
        if (!tool.isWriteTool) {
            return GateDecision.Allowed("Read-Tool — kategorie-uebergreifend erlaubt")
        }

        val granted = permissionRepo.isWriteToolGranted(promptId, tool.name)
        return if (granted) {
            GateDecision.Allowed("Write-Tool freigeschaltet fuer diesen Prompt")
        } else {
            GateDecision.Denied(
                reason =
                    "Write-Tool '${tool.name}' ist fuer diesen Prompt nicht freigeschaltet. " +
                        "Aktiviere es in den Prompt-Einstellungen unter Tool-Berechtigungen."
            )
        }
    }
}

/**
 * Generische Gate-Entscheidung. Wird von allen 3 Gates (Permission, Token,
 * Confirmation) returned. Allowed -> weiter, Denied -> Run abbrechen mit der
 * reason als errorMessage.
 */
sealed class GateDecision {
    abstract val reason: String

    data class Allowed(override val reason: String) : GateDecision()

    data class Denied(override val reason: String) : GateDecision()
}

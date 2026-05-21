package de.frank.entropyreducer.domain.agentic

import de.frank.entropyreducer.data.remote.FunctionDeclaration
import de.frank.entropyreducer.data.repository.PromptToolPermissionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zentraler Hub fuer alle agentic-Tools. Wird vom WorkflowRunner befragt um
 * die FunctionDeclaration-Liste fuer einen konkreten Prompt zu bekommen.
 *
 * Tools werden ueber Hilt-Multibindings registriert: jedes AgenticTool wird
 * in AgenticModule per @Binds @IntoSet eingehaengt. Beim App-Start sammelt
 * Hilt alle registrierten Tools in das Set<AgenticTool>, der ToolRegistry
 * bekommt es per @Inject.
 *
 * Diese Klasse weiss NICHTS ueber Permissions-Logik — sie weiss nur welche
 * Tools existieren und welche Read/Write-Klassifikation sie haben. Die
 * eigentliche Permission-Pruefung macht der PermissionGate (Etappe 4). Diese
 * Klasse bietet aber eine Convenience-Methode `functionDeclarationsForPrompt`
 * die das Filter-Pattern kapselt.
 */
@Singleton
class ToolRegistry
@Inject
constructor(private val tools: Set<@JvmSuppressWildcards AgenticTool>) {

    /** Alle Tools, sortiert nach Name. */
    val all: List<AgenticTool> by lazy { tools.toList().sortedBy { it.name } }

    val readTools: List<AgenticTool> by lazy { all.filter { !it.isWriteTool } }

    val writeTools: List<AgenticTool> by lazy { all.filter { it.isWriteTool } }

    /** Tool nach Name suchen — null wenn nicht registriert. */
    fun byName(name: String): AgenticTool? = tools.firstOrNull { it.name == name }

    /** Alle Tools einer Kategorie. */
    fun byCategory(category: ToolCategory): List<AgenticTool> =
        all.filter { it.category == category }

    /**
     * Baut die Liste von FunctionDeclarations fuer einen konkreten Prompt.
     * Read-Tools sind immer dabei (kategorie-uebergreifend frei, Frank-Entscheidung
     * 2026-05-21). Write-Tools nur wenn der Prompt sie explizit freigeschaltet hat
     * (prompt_tool_permissions.granted=true).
     *
     * Diese Methode wird vom WorkflowRunner VOR dem Gemini-Aufruf gerufen und
     * in das GeminiRequest.tools-Feld eingefuegt.
     */
    suspend fun functionDeclarationsForPrompt(
        promptId: String,
        permissionRepo: PromptToolPermissionRepository,
    ): List<FunctionDeclaration> {
        return tools.mapNotNull { tool ->
            val allowed =
                if (tool.isWriteTool) {
                    permissionRepo.isWriteToolGranted(promptId, tool.name)
                } else {
                    true
                }
            if (!allowed) null
            else
                FunctionDeclaration(
                    name = tool.name,
                    description = tool.description,
                    parameters = tool.parameterSchema,
                )
        }
    }

    /**
     * Liste der Tool-Namen die fuer diesen Prompt erlaubt sind — zum schnellen
     * Cross-Check im PermissionGate ohne FunctionDeclaration-Aufbau.
     */
    suspend fun allowedToolNamesForPrompt(
        promptId: String,
        permissionRepo: PromptToolPermissionRepository,
    ): Set<String> {
        return tools
            .filter { tool ->
                if (tool.isWriteTool) permissionRepo.isWriteToolGranted(promptId, tool.name)
                else true
            }
            .mapTo(mutableSetOf()) { it.name }
    }
}

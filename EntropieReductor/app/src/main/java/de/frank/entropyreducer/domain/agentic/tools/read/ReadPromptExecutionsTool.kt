package de.frank.entropyreducer.domain.agentic.tools.read

import de.frank.entropyreducer.data.local.entities.PromptExecutionEntity
import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.repository.PromptExecutionRepository
import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Cross-Prompt-Read-Tool (Frank-Wunsch 2026-05-21).
 *
 * Liest die Ergebnisse frueherer Prompt-Ausfuehrungen — entweder ueber alle
 * Prompts oder gefiltert auf einen bestimmten Prompt-Namen. Damit kann ein
 * Prompt auf den Output eines anderen Prompts referenzieren — das ist die
 * Grundlage fuer kettenbasierte Workflows ("nach der Wochenanalyse wird der
 * Aufgaben-Generator getriggert und liest die Wochenanalyse").
 *
 * Parameter:
 *  - prompt_name (string, optional) — wenn gesetzt: nur Runs dieses Prompts
 *  - limit (int, optional, default 10, max 50) — wie viele juengste Runs
 *  - nur_erfolgreiche (boolean, optional, default true) — nur SUCCESS
 */
@Singleton
class ReadPromptExecutionsTool
@Inject
constructor(
    private val executionRepo: PromptExecutionRepository,
    private val promptRepo: PromptRepository,
) : AgenticTool {

    override val name: String = "read_prompt_ergebnisse"

    override val category: ToolCategory = ToolCategory.READ_PROMPT_HISTORY

    override val description: String =
        "Liest die Ergebnisse frueherer Prompt-Ausfuehrungen — entweder ueber alle " +
            "Prompts oder gefiltert auf einen Prompt-Namen. Nutze das wenn du auf die " +
            "Ausgabe eines anderen Prompts referenzieren willst (z.B. \"lies die letzte " +
            "Wochenanalyse und baue darauf auf\"). Standard: die juengsten 10 erfolgreichen " +
            "Runs."

    override val isWriteTool: Boolean = false

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Filter fuer die Prompt-Ergebnis-Abfrage.",
            properties =
                mapOf(
                    "prompt_name" to
                        Schema(
                            type = SchemaType.STRING,
                            description =
                                "Optional: Name des Prompts dessen Runs zurueckgegeben werden. " +
                                    "Wenn weggelassen: Runs ueber alle Prompts.",
                        ),
                    "limit" to
                        Schema(
                            type = SchemaType.INTEGER,
                            description = "Maximale Anzahl Runs (Default 10, Max 50).",
                        ),
                    "nur_erfolgreiche" to
                        Schema(
                            type = SchemaType.BOOLEAN,
                            description =
                                "Wenn true: nur Runs mit Status SUCCESS. " +
                                    "Wenn false: alle Status. Default true.",
                        ),
                ),
            required = emptyList(),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: JsonObject(emptyMap())
            val promptNameFilter = obj["prompt_name"]?.jsonPrimitive?.content
            val limit =
                (obj["limit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 10).coerceIn(1, 50)
            val nurErfolgreiche =
                obj["nur_erfolgreiche"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true

            val promptIdFilter: String? =
                if (promptNameFilter != null) {
                    val allPrompts = promptRepo.getAll().first()
                    val match = allPrompts.firstOrNull { it.name.equals(promptNameFilter, ignoreCase = true) }
                    if (match == null) {
                        return ToolResult.Failure(
                            "Kein Prompt mit Name '${promptNameFilter}' gefunden."
                        )
                    }
                    match.id
                } else null

            val executions: List<PromptExecutionEntity> =
                if (promptIdFilter != null) {
                    executionRepo.getByPrompt(promptIdFilter, limit = limit).first()
                } else {
                    executionRepo.getRecent(limit = limit).first()
                }

            val filtered =
                if (nurErfolgreiche)
                    executions.filter {
                        it.status == de.frank.entropyreducer.domain.model.ExecutionStatus.SUCCESS
                    }
                else executions

            val resultJson = buildJsonObject {
                put("count", filtered.size)
                put("filter_prompt_name", promptNameFilter ?: "")
                put("nur_erfolgreiche", nurErfolgreiche)
                put(
                    "executions",
                    buildJsonArray {
                        filtered.forEach { exec ->
                            add(
                                buildJsonObject {
                                    put("id", exec.id)
                                    put("promptName", exec.snapshotName)
                                    put("status", exec.status.name)
                                    put("startedAt", exec.startedAt)
                                    put("finishedAt", exec.finishedAt ?: 0L)
                                    put("modelUsed", exec.modelUsed)
                                    put("tokensTotal", exec.tokensTotal)
                                    put("toolCallCount", exec.toolCallCount)
                                    put("triggerSource", exec.triggerSource.name)
                                    put(
                                        "finalAnswer",
                                        exec.finalAnswer?.take(1500) ?: "",
                                    )
                                    if (exec.errorMessage != null) {
                                        put("errorMessage", exec.errorMessage)
                                    }
                                }
                            )
                        }
                    },
                )
            }

            ToolResult.Success(data = resultJson)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            ToolResult.Failure(
                "Fehler beim Lesen der Prompt-Ergebnisse: ${t.message ?: t::class.simpleName}"
            )
        }
    }
}

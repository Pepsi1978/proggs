package de.frank.entropyreducer.domain.agentic.tools.read

import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.repository.ScientistRepository
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
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Read-Tool: liest die Forscher-Sessions (Wissenschaftler-Diskussionen) aus dem lokalen Cache.
 *
 * Parameter:
 *  - limit (integer, optional, default=20, max=100):
 *    Wie viele der jüngsten Sessions zurückgegeben werden sollen.
 *    Sortierung: jüngste zuerst (nach lastActiveAt absteigend).
 *
 * Liefert ein JSON-Objekt mit `count`, `totalAvailable` und `sessions` (Array).
 * Jede Session enthält id, title, createdAt, lastActiveAt und isArchived.
 *
 * Archivierte Sessions werden standardmäßig eingeschlossen — Gemini kann das
 * selbst filtern wenn nötig (isArchived-Feld ist im JSON enthalten).
 */
@Singleton
class ReadForscherSessionsTool
@Inject
constructor(private val scientistRepository: ScientistRepository) : AgenticTool {

    override val name: String = "read_forscher_sessions"

    override val category: ToolCategory = ToolCategory.READ_FORSCHER

    override val description: String =
        "Liest die Forscher-Sessions des Nutzers — das sind Diskussions-Runden mit dem " +
            "KI-Wissenschaftler zur Hypothesenbildung und Selbstexperiment-Planung. " +
            "Nutze dieses Tool wenn du wissen möchtest welche Themen der Nutzer mit dem " +
            "Wissenschaftler besprochen hat, welche Hypothesen diskutiert wurden oder " +
            "wie aktiv der Nutzer den Forscher-Bereich nutzt."

    override val isWriteTool: Boolean = false

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Filter-Parameter für die Forscher-Sessions-Abfrage.",
            properties =
                mapOf(
                    "limit" to
                        Schema(
                            type = SchemaType.INTEGER,
                            description =
                                "Maximale Anzahl der zurückgegebenen Sessions (jüngste zuerst). " +
                                    "Default: 20, Max: 100.",
                        ),
                ),
            required = emptyList(),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: JsonObject(emptyMap())
            val limit =
                (obj["limit"]?.jsonPrimitive?.content?.toIntOrNull() ?: DEFAULT_LIMIT)
                    .coerceIn(1, MAX_LIMIT)

            // Alle Sessions holen, jüngste (nach lastActiveAt) zuerst, dann auf limit kürzen
            val allSessions = scientistRepository.observeAllSessions().first()
            val totalAvailable = allSessions.size
            val sessions =
                allSessions
                    .sortedByDescending { it.lastActiveAt }
                    .take(limit)

            val resultJson =
                buildJsonObject {
                    put("count", sessions.size)
                    put("totalAvailable", totalAvailable)
                    put("sessions", buildJsonArray {
                        sessions.forEach { session ->
                            add(buildJsonObject {
                                put("id", session.id)
                                put("title", session.title)
                                put("createdAt", session.createdAt)
                                put("lastActiveAt", session.lastActiveAt)
                                put("isArchived", session.isArchived)
                            })
                        }
                    })
                }

            ToolResult.Success(data = resultJson)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Lesen der Forscher-Sessions: ${t.message ?: t::class.simpleName}"
            )
        }
    }

    companion object {
        private const val DEFAULT_LIMIT = 20
        private const val MAX_LIMIT = 100
    }
}

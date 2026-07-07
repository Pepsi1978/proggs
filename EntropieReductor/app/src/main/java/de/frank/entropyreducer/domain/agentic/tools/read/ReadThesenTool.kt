package de.frank.entropyreducer.domain.agentic.tools.read

import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Liest Thesen aus dem EntryRepository.
 *
 * Als "Thesen" gelten Einträge mit timeBucket=SPAETER die nicht archiviert sind —
 * also Langzeit-Gedanken, Ideen und Überlegungen ohne konkretes Datum.
 *
 * Parameter:
 *  - limit (integer, optional, default=50): maximale Anzahl Einträge (max 200)
 *
 * Ergebnis-JSON: count, totalAvailable, entries[] mit
 *  id, createdAt, category, status, title, description (max 500 Zeichen),
 *  aiNotes, priorityScore.
 */
@Singleton
class ReadThesenTool
@Inject
constructor(private val entryRepository: EntryRepository) : AgenticTool {

    override val name: String = "read_thesen"

    override val category: ToolCategory = ToolCategory.READ_THESEN

    override val description: String =
        "Liest Thesen des Nutzers — das sind langfristige Gedanken, Ideen und Überlegungen " +
            "ohne konkretes Datum (timeBucket SPAETER, nicht archiviert). " +
            "Nutze dieses Tool wenn du wissen möchtest welche offenen Langzeit-Themen und " +
            "Ideen der Nutzer gesammelt hat, z.B. um daraus Muster oder Prioritäten abzuleiten."

    override val isWriteTool: Boolean = false

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Filter-Parameter für die Thesen-Abfrage.",
            properties =
                mapOf(
                    "limit" to
                        Schema(
                            type = SchemaType.INTEGER,
                            description =
                                "Maximale Anzahl der zurückgegebenen Thesen. Default 50, Max 200.",
                        ),
                ),
            required = emptyList(),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: JsonObject(emptyMap())
            val limit =
                (obj["limit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 50).coerceIn(1, 200)

            val allEntries: List<EntropyEntryEntity> = entryRepository.getAllForBackup()

            // Thesen = SPAETER-Bucket, nicht archiviert
            val thesen =
                allEntries
                    .filter { entry ->
                        entry.timeBucket == TimeBucket.SPAETER &&
                            entry.status != EntryStatus.ARCHIVIERT
                    }
                    .sortedByDescending { it.createdAt }
                    .take(limit)

            val resultJson =
                buildJsonObject {
                    put("count", thesen.size)
                    put("totalAvailable", allEntries.count {
                        it.timeBucket == TimeBucket.SPAETER &&
                            it.status != EntryStatus.ARCHIVIERT
                    })
                    put("entries", buildJsonArray {
                        thesen.forEach { entry ->
                            add(buildJsonObject {
                                put("id", entry.id)
                                put("createdAt", entry.createdAt)
                                put("category", entry.category.name)
                                put("status", entry.status.name)
                                put("title", entry.title)
                                put(
                                    "description",
                                    entry.description.take(500) +
                                        if (entry.description.length > 500) "..." else "",
                                )
                                put("aiNotes", entry.aiNotes ?: "")
                                put("priorityScore", entry.priorityScore)
                            })
                        }
                    })
                }

            ToolResult.Success(data = resultJson)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Lesen der Thesen: ${t.message ?: t::class.simpleName}"
            )
        }
    }
}

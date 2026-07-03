package de.frank.entropyreducer.domain.agentic.tools.read

import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.repository.MemoryRepository
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
import de.frank.entropyreducer.domain.model.MemorySource
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
 * Liest Memory-Einträge (langfristiges Wissen der KI über den Nutzer).
 *
 * Memories sind vom Nutzer oder der KI gespeicherte Fakten über den Nutzer —
 * z.B. Präferenzen, Erkenntnisse aus früheren Sessions, Profilwissen.
 * Über die Parameter kann nach Quelle, Aktivität und Konfidenz gefiltert werden.
 *
 * Parameter:
 *  - quelle (string, optional, default="alle"):
 *    "manuell" | "ki_vorschlag" | "aus_profil" | "alle"
 *  - nur_aktiv (boolean, optional, default=true): nur aktive Memories zurückgeben
 *  - min_confidence (integer, optional, default=0): Mindest-Konfidenzwert 0..100
 *
 * Ergebnis-JSON: count, totalAvailable, entries[] mit
 *  id, content, source, confidence, isActive, createdAt.
 */
@Singleton
class ReadMemoryTool
@Inject
constructor(private val memoryRepository: MemoryRepository) : AgenticTool {

    override val name: String = "read_memory"

    override val category: ToolCategory = ToolCategory.READ_MEMORY

    override val description: String =
        "Liest das langfristige Gedächtnis über den Nutzer — gespeicherte Fakten, Präferenzen " +
            "und Erkenntnisse aus früheren Interaktionen. " +
            "Nutze dieses Tool wenn du personalisierte Antworten geben möchtest oder prüfen willst " +
            "was über den Nutzer bereits bekannt ist, z.B. Schlafgewohnheiten, Ziele oder " +
            "bevorzugte Arbeitszeiten."

    override val isWriteTool: Boolean = false

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Filter-Parameter für die Memory-Abfrage.",
            properties =
                mapOf(
                    "quelle" to
                        Schema(
                            type = SchemaType.STRING,
                            description =
                                "Quelle der Memories. Default: alle.",
                            enum = listOf("manuell", "ki_vorschlag", "aus_profil", "alle"),
                        ),
                    "nur_aktiv" to
                        Schema(
                            type = SchemaType.BOOLEAN,
                            description =
                                "Nur aktive Memories zurückgeben. Default: true.",
                        ),
                    "min_confidence" to
                        Schema(
                            type = SchemaType.INTEGER,
                            description =
                                "Mindest-Konfidenzwert 0..100. Default: 0 (kein Filter).",
                        ),
                ),
            required = emptyList(),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: JsonObject(emptyMap())
            val quelleArg = obj["quelle"]?.jsonPrimitive?.content ?: "alle"
            val nurAktiv =
                obj["nur_aktiv"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true
            val minConfidence =
                (obj["min_confidence"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0)
                    .coerceIn(0, 100)

            // Quelle-Argument auf MemorySource-Enum mappen — validieren bevor DB-Call
            val targetSource: MemorySource? =
                when (quelleArg) {
                    "manuell" -> MemorySource.MANUELL
                    "ki_vorschlag" -> MemorySource.KI_VORSCHLAG
                    "aus_profil" -> MemorySource.AUS_PROFIL
                    "alle" -> null
                    else ->
                        return ToolResult.Failure(
                            "Unbekannte Quelle: '$quelleArg'. " +
                                "Erlaubt: manuell, ki_vorschlag, aus_profil, alle."
                        )
                }

            // Passendes Flow wählen: getActive() wenn nur aktive gewünscht, sonst getAll()
            val allMemories =
                if (nurAktiv) {
                    memoryRepository.getActive().first()
                } else {
                    memoryRepository.getAll().first()
                }

            val filtered =
                allMemories.filter { memory ->
                    val quelleOk = targetSource == null || memory.source == targetSource
                    val confidenceOk = memory.confidence >= minConfidence
                    quelleOk && confidenceOk
                }

            val resultJson =
                buildJsonObject {
                    put("count", filtered.size)
                    put("totalAvailable", allMemories.size)
                    put("entries", buildJsonArray {
                        filtered.forEach { memory ->
                            add(buildJsonObject {
                                put("id", memory.id)
                                put("content", memory.content)
                                put("source", memory.source.name)
                                put("confidence", memory.confidence)
                                put("isActive", memory.isActive)
                                put("createdAt", memory.createdAt)
                            })
                        }
                    })
                }

            ToolResult.Success(data = resultJson)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Lesen der Memories: ${t.message ?: t::class.simpleName}"
            )
        }
    }
}

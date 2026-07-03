package de.frank.entropyreducer.domain.agentic.tools.read

import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
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
 * Erstes Referenz-Read-Tool: liest Entropie-Eintraege aus dem EntryRepository.
 *
 * Parameter:
 *  - zeitraum (string, optional, default="letzte_7_tage"):
 *    "letzte_7_tage" | "letzte_30_tage" | "letzte_90_tage" | "alle"
 *  - limit (integer, optional, default=50): maximale Anzahl Eintraege
 *
 * Liefert ein JSON-Objekt mit `count` und `entries` (Array). Jeder Eintrag hat:
 *  id, createdAt (ISO), category, status, originalText (Anfang),
 *  summary, followupCount.
 *
 * Diese Klasse dient als Vorlage fuer die anderen 7 Read-Tools — sie zeigt das
 * AgenticTool-Pattern: Schema-Definition, Args-Parsing, Repository-Aufruf,
 * JSON-Serialisierung des Ergebnisses, defensive Exception-Behandlung.
 */
@Singleton
class ReadEntropieEintraegeTool
@Inject
constructor(private val entryRepository: EntryRepository) : AgenticTool {

    override val name: String = "read_entropie_eintraege"

    override val category: ToolCategory = ToolCategory.READ_ENTROPIE

    override val description: String =
        "Liest Entropie-Tagebuch-Eintraege des Nutzers aus einem gewaehlten Zeitraum. " +
            "Nutze dieses Tool wenn du den Inhalt von Tagebuch-Eintraegen brauchst um eine " +
            "Frage zu beantworten, ein Muster zu erkennen oder eine Aufgabe daraus abzuleiten. " +
            "Du kannst den Zeitraum einschraenken und die Anzahl begrenzen."

    override val isWriteTool: Boolean = false

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Filter-Parameter fuer die Eintrag-Abfrage.",
            properties =
                mapOf(
                    "zeitraum" to
                        Schema(
                            type = SchemaType.STRING,
                            description =
                                "Zeitraum der zurueckgegebenen Eintraege. Default: letzte_7_tage.",
                            enum =
                                listOf(
                                    "letzte_7_tage",
                                    "letzte_30_tage",
                                    "letzte_90_tage",
                                    "alle",
                                ),
                        ),
                    "limit" to
                        Schema(
                            type = SchemaType.INTEGER,
                            description =
                                "Maximale Anzahl der zurueckgegebenen Eintraege. Default 50, Max 200.",
                        ),
                ),
            required = emptyList(),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: JsonObject(emptyMap())
            val zeitraum =
                obj["zeitraum"]?.jsonPrimitive?.content ?: "letzte_7_tage"
            val limit =
                (obj["limit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 50).coerceIn(1, 200)

            val sinceMillis: Long? =
                when (zeitraum) {
                    "letzte_7_tage" -> ctx.startedAt - 7L * MILLIS_PER_DAY
                    "letzte_30_tage" -> ctx.startedAt - 30L * MILLIS_PER_DAY
                    "letzte_90_tage" -> ctx.startedAt - 90L * MILLIS_PER_DAY
                    "alle" -> null
                    else ->
                        return ToolResult.Failure(
                            "Unbekannter Zeitraum: '$zeitraum'. Erlaubt: letzte_7_tage, letzte_30_tage, letzte_90_tage, alle."
                        )
                }

            val allEntries: List<EntropyEntryEntity> = entryRepository.getAllForBackup()
            val filtered =
                allEntries
                    .filter { sinceMillis == null || it.createdAt >= sinceMillis }
                    .sortedByDescending { it.createdAt }
                    .take(limit)

            val resultJson =
                buildJsonObject {
                    put("count", filtered.size)
                    put("totalAvailable", allEntries.size)
                    put("zeitraum", zeitraum)
                    put("entries", buildJsonArray {
                        filtered.forEach { entry ->
                            add(buildJsonObject {
                                put("id", entry.id)
                                put("createdAt", entry.createdAt)
                                put("category", entry.category.name)
                                put("status", entry.status.name)
                                put("timeBucket", entry.timeBucket.name)
                                put("title", entry.title)
                                put(
                                    "description",
                                    entry.description.take(500) +
                                        if (entry.description.length > 500) "..." else "",
                                )
                                put(
                                    "rawTranscript",
                                    entry.rawTranscript.take(800) +
                                        if (entry.rawTranscript.length > 800) "..." else "",
                                )
                                put("aiNotes", entry.aiNotes ?: "")
                                put("priorityScore", entry.priorityScore)
                                put("severity", entry.severity)
                                put("tags", buildJsonArray { entry.tags.forEach { add(it) } })
                            })
                        }
                    })
                }

            ToolResult.Success(data = resultJson)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Lesen der Eintraege: ${t.message ?: t::class.simpleName}"
            )
        }
    }

    companion object {
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}

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
import de.frank.entropyreducer.domain.model.priorityBucketForScore
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
 * Liest aktive Aufgaben aus dem EntryRepository.
 *
 * "Aktive Aufgaben" sind Einträge mit Status OFFEN oder IN_ARBEIT. Über die optionalen
 * Parameter kann nach Prioritätsbereich und Status gefiltert werden.
 *
 * Parameter:
 *  - bucket (string, optional, default="alle"):
 *    "sehr_hoch" | "hoch" | "mittel" | "gering" | "spaeter" | "alle"
 *  - status (string, optional, default="offen"):
 *    "offen" | "in_arbeit" | "alle"
 *    Hinweis: "offen" liefert auch IN_ARBEIT-Einträge (= alle aktiven)
 *  - limit (integer, optional, default=50): maximale Anzahl Einträge (max 200)
 *
 * Sortierung: priorityScore absteigend, dann createdAt absteigend.
 *
 * Ergebnis-JSON: count, totalAvailable, filter, entries[] mit
 *  id, createdAt, category, status, timeBucket, title,
 *  description (max 500 Zeichen), aiNotes, priorityScore,
 *  severity, estimatedDurationMinutes.
 */
@Singleton
class ReadAufgabenTool
@Inject
constructor(private val entryRepository: EntryRepository) : AgenticTool {

    override val name: String = "read_aufgaben"

    override val category: ToolCategory = ToolCategory.READ_AUFGABEN

    override val description: String =
        "Liest aktive Aufgaben des Nutzers nach Prioritätsbereichen. " +
            "Nutze dieses Tool wenn du wissen möchtest was der Nutzer aktuell zu erledigen hat, " +
            "z.B. um Prioritäten einzuschätzen, offene Arbeit zu bündeln oder eine Tagesplanung zu erstellen."

    override val isWriteTool: Boolean = false

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Filter-Parameter für die Aufgaben-Abfrage.",
            properties =
                mapOf(
                    "bucket" to
                        Schema(
                            type = SchemaType.STRING,
                            description =
                                "Prioritätsbereich-Filter. Default: alle. " +
                                    "'offen' liefert OFFEN + IN_ARBEIT gemeinsam.",
                            enum =
                                listOf(
                                    "sehr_hoch",
                                    "hoch",
                                    "mittel",
                                    "gering",
                                    "spaeter",
                                    "heute",
                                    "morgen",
                                    "freiblock",
                                    "alle",
                                ),
                        ),
                    "status" to
                        Schema(
                            type = SchemaType.STRING,
                            description =
                                "Status-Filter. Default: offen (= OFFEN + IN_ARBEIT). " +
                                    "'alle' liefert alle Status-Werte.",
                            enum = listOf("offen", "in_arbeit", "alle"),
                        ),
                    "limit" to
                        Schema(
                            type = SchemaType.INTEGER,
                            description =
                                "Maximale Anzahl der zurückgegebenen Aufgaben. Default 50, Max 200.",
                        ),
                ),
            required = emptyList(),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: JsonObject(emptyMap())
            val bucketArg = obj["bucket"]?.jsonPrimitive?.content ?: "alle"
            val statusArg = obj["status"]?.jsonPrimitive?.content ?: "offen"
            val limit =
                (obj["limit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 50).coerceIn(1, 200)

            // Prioritätsbereich-Argument auf TimeBucket-Enum mappen. Alte Bucket-Namen bleiben Alias.
            val targetBucket: TimeBucket? =
                when (bucketArg) {
                    "sehr_hoch", "heute" -> TimeBucket.HEUTE
                    "hoch", "morgen" -> TimeBucket.MORGEN
                    "mittel", "freiblock" -> TimeBucket.FREIBLOCK
                    "gering" -> TimeBucket.GERING
                    "spaeter" -> TimeBucket.SPAETER
                    "alle" -> null
                    else ->
                        return ToolResult.Failure(
                            "Unbekannter Bucket: '$bucketArg'. " +
                                "Erlaubt: sehr_hoch, hoch, mittel, gering, spaeter, alle."
                        )
                }

            val allEntries: List<EntropyEntryEntity> = entryRepository.getAllForBackup()

            val filtered =
                allEntries
                    .filter { entry ->
                        // Bucket-Filter
                        val effectiveBucket = priorityBucketForScore(entry.manualPriorityScore ?: entry.priorityScore)
                        val bucketOk = targetBucket == null || effectiveBucket == targetBucket

                        // Status-Filter: "offen" = OFFEN + IN_ARBEIT (alle aktiven),
                        // "in_arbeit" = nur IN_ARBEIT, "alle" = kein Filter
                        val statusOk =
                            when (statusArg) {
                                "offen" ->
                                    entry.status == EntryStatus.OFFEN ||
                                        entry.status == EntryStatus.IN_ARBEIT
                                "in_arbeit" -> entry.status == EntryStatus.IN_ARBEIT
                                "alle" -> true
                                else ->
                                    entry.status == EntryStatus.OFFEN ||
                                        entry.status == EntryStatus.IN_ARBEIT
                            }

                        bucketOk && statusOk
                    }
                    .sortedWith(
                        compareByDescending<EntropyEntryEntity> { it.priorityScore }
                            .thenByDescending { it.createdAt }
                    )
                    .take(limit)

            val resultJson =
                buildJsonObject {
                    put("count", filtered.size)
                    put("totalAvailable", allEntries.size)
                    put("filter", buildJsonObject {
                        put("bucket", bucketArg)
                        put("status", statusArg)
                    })
                    put("entries", buildJsonArray {
                        filtered.forEach { entry ->
                            val effectiveBucket =
                                priorityBucketForScore(entry.manualPriorityScore ?: entry.priorityScore)
                            add(buildJsonObject {
                                put("id", entry.id)
                                put("createdAt", entry.createdAt)
                                put("category", entry.category.name)
                                put("status", entry.status.name)
                                put("timeBucket", entry.timeBucket.name)
                                put("priorityBucket", effectiveBucket.name)
                                put("title", entry.title)
                                put(
                                    "description",
                                    entry.description.take(500) +
                                        if (entry.description.length > 500) "..." else "",
                                )
                                put("aiNotes", entry.aiNotes ?: "")
                                put("priorityScore", entry.priorityScore)
                                put("severity", entry.severity)
                                put("estimatedDurationMinutes", entry.estimatedDurationMinutes)
                            })
                        }
                    })
                }

            ToolResult.Success(data = resultJson)
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Lesen der Aufgaben: ${t.message ?: t::class.simpleName}"
            )
        }
    }
}

package de.frank.entropyreducer.domain.agentic.tools.read

import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.repository.InsightRepository
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
import de.frank.entropyreducer.domain.model.EntropyCategory
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
 * Read-Tool: liest KI-generierte Insights (Methoden & Erkenntnisse) aus dem Repertoire.
 *
 * Nutze dieses Tool wenn du wissen möchtest welche Methoden oder Erkenntnisse der Nutzer
 * bereits bestätigt, beobachtet oder verworfen hat. Du kannst nach Status (bestätigt /
 * in Beobachtung / verworfen) und nach Entropie-Kategorie filtern, um gezielt passende
 * Erkenntnisse für eine Empfehlung oder Analyse zu finden.
 *
 * Parameter:
 *  - status (string, optional, default="alle"):
 *    "confirmed" | "in_observation" | "discarded" | "alle"
 *  - kategorie (string, optional):
 *    "KOERPERLICH" | "MENTAL" | "ZEITLICH" | "EMOTIONAL" |
 *    "GESUNDHEITLICH" | "UMGEBUNG" | "SONSTIGES"
 *  - limit (integer, optional, default=30, max=200)
 */
@Singleton
class ReadInsightsTool
@Inject
constructor(private val insightRepository: InsightRepository) : AgenticTool {

    override val name: String = "read_insights"

    override val category: ToolCategory = ToolCategory.READ_INSIGHTS

    override val description: String =
        "Liest KI-generierte Insights und Methoden aus dem Repertoire des Nutzers. " +
            "Nutze dieses Tool wenn du bestätigte, beobachtete oder verworfene Erkenntnisse " +
            "abrufen möchtest — z. B. um eine passende Methode zu empfehlen oder den " +
            "Fortschritt eines Nutzers in einem bestimmten Lebensbereich zu verstehen."

    override val isWriteTool: Boolean = false

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Filter-Parameter für die Insights-Abfrage.",
            properties =
                mapOf(
                    "status" to
                        Schema(
                            type = SchemaType.STRING,
                            description =
                                "Filtert nach dem Bestätigungs-Status des Insights. Default: alle.",
                            enum =
                                listOf(
                                    "confirmed",
                                    "in_observation",
                                    "discarded",
                                    "alle",
                                ),
                        ),
                    "kategorie" to
                        Schema(
                            type = SchemaType.STRING,
                            description =
                                "Filtert nach Entropie-Kategorie des Insights. Wenn weggelassen, " +
                                    "werden alle Kategorien zurückgegeben.",
                            enum =
                                listOf(
                                    "KOERPERLICH",
                                    "MENTAL",
                                    "ZEITLICH",
                                    "EMOTIONAL",
                                    "GESUNDHEITLICH",
                                    "UMGEBUNG",
                                    "SONSTIGES",
                                ),
                        ),
                    "limit" to
                        Schema(
                            type = SchemaType.INTEGER,
                            description =
                                "Maximale Anzahl der zurückgegebenen Insights. Default 30, Max 200.",
                        ),
                ),
            required = emptyList(),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: JsonObject(emptyMap())
            val status = obj["status"]?.jsonPrimitive?.content ?: "alle"
            val kategorieRaw = obj["kategorie"]?.jsonPrimitive?.content
            val limit =
                (obj["limit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 30).coerceIn(1, 200)

            // Kategorie parsen — ungültiger Wert liefert sofort Failure
            val kategorieFilter: EntropyCategory? =
                if (kategorieRaw != null) {
                    runCatching { EntropyCategory.valueOf(kategorieRaw) }.getOrElse {
                        return ToolResult.Failure(
                            "Ungültige Kategorie: '$kategorieRaw'. Erlaubt: " +
                                EntropyCategory.entries.joinToString(", ") { it.name }
                        )
                    }
                } else {
                    null
                }

            // Passenden Flow anhand Status auswählen
            val allByStatus: List<InsightEntity> =
                when (status) {
                    "confirmed" -> insightRepository.observeConfirmed().first()
                    "in_observation" -> insightRepository.observeInObservation().first()
                    "discarded" -> insightRepository.observeDiscarded().first()
                    "alle" -> insightRepository.observeAll().first()
                    else ->
                        return ToolResult.Failure(
                            "Ungültiger Status: '$status'. Erlaubt: confirmed, in_observation, discarded, alle."
                        )
                }

            // Optionaler Kategorie-Filter auf Client-Seite
            val filtered =
                (if (kategorieFilter != null) {
                    allByStatus.filter {
                        it.targetCategory == kategorieFilter ||
                            it.additionalCategories.contains(kategorieFilter)
                    }
                } else {
                    allByStatus
                }).take(limit)

            val resultJson =
                buildJsonObject {
                    put("count", filtered.size)
                    put("totalAvailable", allByStatus.size)
                    put("statusFilter", status)
                    put("kategorieFilter", kategorieRaw ?: "alle")
                    put(
                        "insights",
                        buildJsonArray {
                            filtered.forEach { insight ->
                                add(
                                    buildJsonObject {
                                        put("id", insight.id)
                                        put("title", insight.title)
                                        put("description", insight.description)
                                        put("targetCategory", insight.targetCategory.name)
                                        put(
                                            "additionalCategories",
                                            buildJsonArray {
                                                insight.additionalCategories.forEach { c ->
                                                    add(c.name)
                                                }
                                            },
                                        )
                                        put("confidence", insight.confidence)
                                        put("successCount", insight.successCount)
                                        put("attemptCount", insight.attemptCount)
                                        put("avgBiomarkerImpact", insight.avgBiomarkerImpact ?: "")
                                        put("avgFeltImpact", insight.avgFeltImpact ?: 0.0)
                                        put("manualSource", insight.manualSource)
                                        put("createdAt", insight.createdAt)
                                        put("updatedAt", insight.updatedAt)
                                        put(
                                            "sourceHypothesisIds",
                                            buildJsonArray {
                                                insight.sourceHypothesisIds.forEach { id ->
                                                    add(id)
                                                }
                                            },
                                        )
                                    }
                                )
                            }
                        },
                    )
                }

            ToolResult.Success(data = resultJson)
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Lesen der Insights: ${t.message ?: t::class.simpleName}"
            )
        }
    }
}

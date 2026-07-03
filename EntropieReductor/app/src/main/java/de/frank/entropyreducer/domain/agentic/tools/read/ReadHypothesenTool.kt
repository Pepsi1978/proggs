package de.frank.entropyreducer.domain.agentic.tools.read

import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.repository.HypothesisRepository
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
import de.frank.entropyreducer.domain.model.HypothesisStatus
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
 * Read-Tool: liest Hypothesen / Experimente aus dem Forscher-Bereich.
 *
 * Nutze dieses Tool wenn du laufende oder vergangene Experimente des Nutzers analysieren
 * möchtest — z. B. um den Fortschritt eines aktiven Experiments zu bewerten, abgeschlossene
 * Experimente für eine Empfehlung heranzuziehen, oder zu prüfen welche Hypothesen noch
 * vorgeschlagen aber nicht gestartet wurden. Du kannst nach Status filtern.
 *
 * Parameter:
 *  - status (string, optional, default="alle"):
 *    "vorgeschlagen" | "aktiv" | "abgebrochen" | "abgeschlossen" | "alle"
 *  - limit (integer, optional, default=30, max=200)
 */
@Singleton
class ReadHypothesenTool
@Inject
constructor(private val hypothesisRepository: HypothesisRepository) : AgenticTool {

    override val name: String = "read_hypothesen"

    override val category: ToolCategory = ToolCategory.READ_HYPOTHESEN

    override val description: String =
        "Liest Hypothesen und Experimente des Nutzers aus dem Forscher-Bereich. " +
            "Nutze dieses Tool wenn du wissen möchtest welche Experimente der Nutzer " +
            "gestartet hat, gerade aktiv durchführt oder bereits abgeschlossen hat — " +
            "z. B. um Fortschritte zu bewerten oder neue Empfehlungen auf bisherigen " +
            "Experimenten aufzubauen."

    override val isWriteTool: Boolean = false

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Filter-Parameter für die Hypothesen-Abfrage.",
            properties =
                mapOf(
                    "status" to
                        Schema(
                            type = SchemaType.STRING,
                            description =
                                "Filtert Hypothesen nach ihrem aktuellen Status. Default: alle.",
                            enum =
                                listOf(
                                    "vorgeschlagen",
                                    "aktiv",
                                    "abgebrochen",
                                    "abgeschlossen",
                                    "alle",
                                ),
                        ),
                    "limit" to
                        Schema(
                            type = SchemaType.INTEGER,
                            description =
                                "Maximale Anzahl der zurückgegebenen Hypothesen. Default 30, Max 200.",
                        ),
                ),
            required = emptyList(),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: JsonObject(emptyMap())
            val status = obj["status"]?.jsonPrimitive?.content ?: "alle"
            val limit =
                (obj["limit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 30).coerceIn(1, 200)

            // Passenden Flow anhand Status auswählen
            val allByStatus: List<HypothesisEntity> =
                when (status) {
                    "vorgeschlagen" ->
                        hypothesisRepository.observeByStatus(HypothesisStatus.VORGESCHLAGEN).first()
                    "aktiv" ->
                        hypothesisRepository.observeByStatus(HypothesisStatus.AKTIV).first()
                    "abgebrochen" ->
                        hypothesisRepository.observeByStatus(HypothesisStatus.ABGEBROCHEN).first()
                    "abgeschlossen" ->
                        hypothesisRepository.observeByStatus(HypothesisStatus.ABGESCHLOSSEN).first()
                    "alle" ->
                        hypothesisRepository.observeAll().first()
                    else ->
                        return ToolResult.Failure(
                            "Ungültiger Status: '$status'. Erlaubt: vorgeschlagen, aktiv, abgebrochen, abgeschlossen, alle."
                        )
                }

            val filtered = allByStatus.take(limit)

            val resultJson =
                buildJsonObject {
                    put("count", filtered.size)
                    put("totalAvailable", allByStatus.size)
                    put("statusFilter", status)
                    put(
                        "hypothesen",
                        buildJsonArray {
                            filtered.forEach { h ->
                                add(
                                    buildJsonObject {
                                        put("id", h.id)
                                        put("title", h.title)
                                        put("description", h.description)
                                        put("rationale", h.rationale)
                                        put("status", h.status.name)
                                        put("outcome", h.outcome?.name ?: "")
                                        put("outcomeNotes", h.outcomeNotes ?: "")
                                        put("createdAt", h.createdAt)
                                        put("plannedStartDate", h.plannedStartDate)
                                        put("plannedEndDate", h.plannedEndDate)
                                        put("actualStartDate", h.actualStartDate ?: 0L)
                                        put("actualEndDate", h.actualEndDate ?: 0L)
                                        put("felltEntropyChange", h.felltEntropyChange ?: 0)
                                        put("biomarkerBeforeId", h.biomarkerBeforeId ?: "")
                                        put("biomarkerAfterId", h.biomarkerAfterId ?: "")
                                        put(
                                            "relatedEntryIds",
                                            buildJsonArray {
                                                h.relatedEntryIds.forEach { id -> add(id) }
                                            },
                                        )
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
                message = "Fehler beim Lesen der Hypothesen: ${t.message ?: t::class.simpleName}"
            )
        }
    }
}

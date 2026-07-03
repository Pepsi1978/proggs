package de.frank.entropyreducer.domain.agentic.tools.write

import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.repository.HypothesisRepository
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
import de.frank.entropyreducer.domain.model.HypothesisStatus
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Write-Tool: legt eine neue Hypothese / ein neues Experiment an.
 *
 * Status wird auf VORGESCHLAGEN gesetzt — der Nutzer aktiviert sie manuell
 * oder über ein weiteres Tool. plannedEndDate default = now + 14 Tage.
 */
@Singleton
class CreateHypotheseTool
@Inject
constructor(private val hypothesisRepository: HypothesisRepository) : AgenticTool {

    override val name: String = "create_hypothese"

    override val category: ToolCategory = ToolCategory.WRITE_HYPOTHESEN

    override val isWriteTool: Boolean = true

    override val description: String =
        "Legt eine neue Hypothese / ein neues Experiment an. Hypothesen sind strukturierte Tests " +
            "im Forscher-Bereich, mit Hypothese-Text, Begruendung und einem geplanten Start/End-Datum. " +
            "Nutze das wenn der Nutzer einen kontrollierten Selbstversuch starten will."

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Parameter fuer die neue Hypothese.",
            properties =
                mapOf(
                    "titel" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Kurzbezeichnung der Hypothese, z.B. 'Koffein-Reduktion verbessert Schlaf'.",
                        ),
                    "beschreibung" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Detaillierte Beschreibung des Experiments und der erwarteten Wirkung.",
                        ),
                    "rationale" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Optionale Begruendung — warum diese Hypothese sinnvoll ist.",
                        ),
                    "geplantes_start_datum" to
                        Schema(
                            type = SchemaType.INTEGER,
                            description = "Geplanter Starttermin als Unix-Millisekunden. Default: jetzt.",
                        ),
                    "geplantes_end_datum" to
                        Schema(
                            type = SchemaType.INTEGER,
                            description = "Geplantes Enddatum als Unix-Millisekunden. Default: jetzt + 14 Tage.",
                        ),
                ),
            required = listOf("titel", "beschreibung"),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: JsonObject(emptyMap())

            val titel = obj["titel"]?.jsonPrimitive?.content?.trim()
                ?: return ToolResult.Failure("Parameter 'titel' ist erforderlich.")
            val beschreibung = obj["beschreibung"]?.jsonPrimitive?.content?.trim()
                ?: return ToolResult.Failure("Parameter 'beschreibung' ist erforderlich.")
            val rationale = obj["rationale"]?.jsonPrimitive?.content?.trim() ?: ""

            val now = System.currentTimeMillis()
            val plannedStart = obj["geplantes_start_datum"]?.jsonPrimitive?.content
                ?.toLongOrNull() ?: now
            val plannedEnd = obj["geplantes_end_datum"]?.jsonPrimitive?.content
                ?.toLongOrNull() ?: (now + DEFAULT_DURATION_MS)

            val id = UUID.randomUUID().toString()
            val entity = HypothesisEntity(
                id = id,
                title = titel,
                description = beschreibung,
                rationale = rationale,
                createdAt = now,
                plannedStartDate = plannedStart,
                plannedEndDate = plannedEnd,
                actualStartDate = null,
                actualEndDate = null,
                status = HypothesisStatus.VORGESCHLAGEN,
                outcome = null,
                outcomeNotes = null,
                biomarkerBeforeId = null,
                biomarkerAfterId = null,
                felltEntropyChange = null,
                relatedEntryIds = emptyList(),
            )

            hypothesisRepository.upsert(entity)

            val resultJson = buildJsonObject {
                put("id", id)
                put("titel", titel)
                put("status", HypothesisStatus.VORGESCHLAGEN.name)
                put("plannedStartDate", plannedStart)
                put("plannedEndDate", plannedEnd)
                put("message", "Hypothese erfolgreich angelegt.")
            }

            ToolResult.Success(
                data = resultJson,
                createdEntityIds = listOf(id),
            )
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Anlegen der Hypothese: ${t.message ?: t::class.simpleName}"
            )
        }
    }

    companion object {
        /** 14 Tage in Millisekunden als Standard-Versuchsdauer. */
        private const val DEFAULT_DURATION_MS = 14L * 24L * 60L * 60L * 1000L
    }
}

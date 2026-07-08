package de.frank.entropyreducer.domain.agentic.tools.write

import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.safety.PhoneContentGuard
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Write-Tool: Legt eine neue These im Thesen-Bereich an.
 *
 * Thesen sind Langzeit-Hypothesen oder Grundsatz-Gedanken die der Nutzer über
 * sich selbst, seinen Körper oder seine Lebensumstände formuliert. Sie sind
 * keine Aufgaben und haben deshalb die niedrigste Priorität (priorityScore=20.0,
 * severity=2) und landen im SPÄTER-Bucket.
 *
 * Parameter:
 *  - text (string, required, max 3000 Zeichen): der These-Text
 *  - kategorie (string, optional, default "MENTAL"): EntropyCategory-Enum-Wert
 *  - titel (string, optional): Überschrift — wird aus dem Text generiert wenn leer
 *
 * Liefert bei Erfolg: JSON mit {id, titel, kategorie, message}
 * und createdEntityIds = [id] für Audit + Rollback.
 */
@Singleton
class CreateTheseTool
@Inject
constructor(private val entryRepository: EntryRepository) : AgenticTool {

    override val name: String = "create_these"

    override val category: ToolCategory = ToolCategory.WRITE_THESEN

    override val isWriteTool: Boolean = true

    override val description: String =
        "Legt eine neue These an. Thesen sind Langzeit-Hypothesen oder Grundsatz-Gedanken " +
            "die der Nutzer ueber sich selbst, seinen Koerper oder seine Lebensumstaende " +
            "formuliert. Im UI sind Thesen die Eintraege im Bereich Thesen."

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Parameter für das Anlegen einer neuen These.",
            properties =
                mapOf(
                    "text" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Der These-Text. Maximal 3000 Zeichen.",
                        ),
                    "kategorie" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Kategorie der These. Default: MENTAL.",
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
                    "titel" to
                        Schema(
                            type = SchemaType.STRING,
                            description =
                                "Optionale Überschrift. Wird aus den ersten 80 Zeichen " +
                                    "des Textes generiert wenn nicht angegeben.",
                        ),
                ),
            required = listOf("text"),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: return ToolResult.Failure(
                "Ungültige Argumente: JsonObject erwartet."
            )

            // --- Pflicht-Parameter ---
            val text = obj["text"]?.jsonPrimitive?.content?.trim()
                ?: return ToolResult.Failure("Parameter 'text' fehlt oder ist leer.")
            if (PhoneContentGuard.isSecondBrainWorkArtifact(obj["titel"]?.jsonPrimitive?.content, text)) {
                return ToolResult.Failure("Interner Second-Brain-Arbeitsinhalt wird nicht als These gespeichert.")
            }

            // --- Längen-Validierung ---
            if (text.length > 3000) {
                return ToolResult.Failure(
                    "Parameter 'text' ist zu lang (${text.length} Zeichen, Max 3000)."
                )
            }

            // --- Optionale Parameter ---
            val kategorieRaw = obj["kategorie"]?.jsonPrimitive?.content?.trim() ?: "MENTAL"
            val titelRaw = obj["titel"]?.jsonPrimitive?.content?.trim()

            // Auto-Titel aus den ersten 80 Zeichen generieren wenn kein Titel angegeben
            val titel = if (!titelRaw.isNullOrBlank()) {
                titelRaw
            } else {
                text.take(80) + if (text.length > 80) "..." else ""
            }

            // --- Enum-Konvertierung ---
            val kategorie = try {
                EntropyCategory.valueOf(kategorieRaw)
            } catch (e: IllegalArgumentException) {
                return ToolResult.Failure(
                    "Unbekannte Kategorie: '$kategorieRaw'. " +
                        "Erlaubt: KOERPERLICH, MENTAL, ZEITLICH, EMOTIONAL, " +
                        "GESUNDHEITLICH, UMGEBUNG, SONSTIGES."
                )
            }

            // --- Entity aufbauen ---
            // Thesen: niedrigste Priorität (20.0), niedrige Schwere (2),
            // immer im SPÄTER-Bucket — langfristige Hypothesen ohne Zeitdruck.
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val entity = EntropyEntryEntity(
                id = id,
                rawTranscript = text,
                title = titel,
                description = text,
                category = kategorie,
                severity = 2,
                priorityScore = 20.0,
                priorityReason = "These — kein zeitkritisches Action-Item",
                status = EntryStatus.OFFEN,
                timeBucket = TimeBucket.SPAETER,
                estimatedDurationMinutes = null,
                createdAt = now,
                updatedAt = now,
                resolvedAt = null,
                tags = emptyList(),
                aiNotes = null,
                source = EntrySource.KI_ERKANNT,
                biomarkerSnapshotId = null,
                manualBucket = null,
                manualBucketSetAt = null,
            )

            entryRepository.upsert(entity)

            val resultJson = buildJsonObject {
                put("id", id)
                put("titel", titel)
                put("kategorie", kategorie.name)
                put("bucket", TimeBucket.SPAETER.name)
                put("message", "These angelegt")
            }

            ToolResult.Success(
                data = resultJson,
                createdEntityIds = listOf(id),
            )
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Anlegen der These: ${t.message ?: t::class.simpleName}"
            )
        }
    }
}

package de.frank.entropyreducer.domain.agentic.tools.write

import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.repository.EntryRepository
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
 * Write-Tool: Legt einen neuen Tagebuch-Eintrag im Entropie-Tagebuch an.
 *
 * Tagebuch-Einträge sind reflektierende Notizen — keine Aufgaben. Sie landen
 * automatisch im SPÄTER-Bucket (kein Zeitdruck) und bekommen eine niedrigere
 * Priorität als Aufgaben (priorityScore=30.0, severity=3).
 *
 * Der Titel wird automatisch aus den ersten 80 Zeichen des Textes generiert,
 * wenn kein expliziter Titel übergeben wird.
 *
 * Parameter:
 *  - text (string, required, max 5000 Zeichen): der eigentliche Eintrag-Text
 *  - kategorie (string, optional, default "SONSTIGES"): EntropyCategory-Enum-Wert
 *  - titel (string, optional): Überschrift — wird aus dem Text generiert wenn leer
 *
 * Liefert bei Erfolg: JSON mit {id, titel, kategorie, message}
 * und createdEntityIds = [id] für Audit + Rollback.
 */
@Singleton
class CreateTagebuchEintragTool
@Inject
constructor(private val entryRepository: EntryRepository) : AgenticTool {

    override val name: String = "create_tagebuch_eintrag"

    override val category: ToolCategory = ToolCategory.WRITE_ENTROPIE

    override val isWriteTool: Boolean = true

    override val description: String =
        "Legt einen neuen Tagebuch-Eintrag im Entropie-Tagebuch an. Tagebuch-Eintraege " +
            "sind reflektierende Notizen, keine Aufgaben. Nutze das wenn der Nutzer eine " +
            "Beobachtung, eine Erkenntnis oder eine Stimmung festhalten will."

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Parameter für das Anlegen eines neuen Tagebuch-Eintrags.",
            properties =
                mapOf(
                    "text" to
                        Schema(
                            type = SchemaType.STRING,
                            description =
                                "Der eigentliche Tagebuch-Text. Maximal 5000 Zeichen.",
                        ),
                    "kategorie" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Kategorie des Eintrags. Default: SONSTIGES.",
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

            // --- Längen-Validierung ---
            if (text.length > 5000) {
                return ToolResult.Failure(
                    "Parameter 'text' ist zu lang (${text.length} Zeichen, Max 5000)."
                )
            }

            // --- Optionale Parameter ---
            val kategorieRaw = obj["kategorie"]?.jsonPrimitive?.content?.trim() ?: "SONSTIGES"
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
            // Tagebuch-Einträge: niedrige Priorität (30.0), mittlere Schwere (3),
            // immer im SPÄTER-Bucket — sie sind keine zeitkritischen Aufgaben.
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val entity = EntropyEntryEntity(
                id = id,
                rawTranscript = text,
                title = titel,
                description = text,
                category = kategorie,
                severity = 3,
                priorityScore = 30.0,
                priorityReason = "Tagebuch-Eintrag, kein Action-Item",
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
                put("message", "Tagebuch-Eintrag angelegt")
            }

            ToolResult.Success(
                data = resultJson,
                createdEntityIds = listOf(id),
            )
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Anlegen des Tagebuch-Eintrags: ${t.message ?: t::class.simpleName}"
            )
        }
    }
}

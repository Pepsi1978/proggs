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
 * Write-Tool: Legt eine neue Aufgabe im Entropie-Stream an.
 *
 * Aufgaben sind konkrete, handlungsorientierte Einträge, die im
 * HEUTE/MORGEN/FREIBLOCK/SPÄTER-Bucket nach Priority sortiert erscheinen.
 *
 * Parameter:
 *  - titel (string, required, max 200 Zeichen): Kurzbezeichnung der Aufgabe
 *  - beschreibung (string, required, max 2000 Zeichen): Ausführliche Beschreibung
 *  - kategorie (string, required): EntropyCategory-Enum-Wert
 *  - bucket (string, optional, default "HEUTE"): TimeBucket-Enum-Wert
 *  - severity (integer, optional, default 5): Schwere 1..10
 *  - priorityScore (number, optional, default 50.0): Priorität 0.0..100.0
 *
 * Liefert bei Erfolg: JSON mit {id, titel, kategorie, bucket, message}
 * und createdEntityIds = [id] für Audit + Rollback.
 */
@Singleton
class CreateAufgabeTool
@Inject
constructor(private val entryRepository: EntryRepository) : AgenticTool {

    override val name: String = "create_aufgabe"

    override val category: ToolCategory = ToolCategory.WRITE_AUFGABEN

    override val isWriteTool: Boolean = true

    override val description: String =
        "Legt eine neue Aufgabe an. Nutze dieses Tool wenn der Nutzer einen To-Do-Eintrag " +
            "braucht, eine konkrete Handlung. Aufgaben werden im HEUTE/MORGEN/FREIBLOCK/" +
            "SPAETER-Bucket sortiert nach Priority gezeigt."

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Parameter für das Anlegen einer neuen Aufgabe.",
            properties =
                mapOf(
                    "titel" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Kurzbezeichnung der Aufgabe. Maximal 200 Zeichen.",
                        ),
                    "beschreibung" to
                        Schema(
                            type = SchemaType.STRING,
                            description =
                                "Ausführliche Beschreibung der Aufgabe. Maximal 2000 Zeichen.",
                        ),
                    "kategorie" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Kategorie der Aufgabe.",
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
                    "bucket" to
                        Schema(
                            type = SchemaType.STRING,
                            description =
                                "Zeitlicher Bucket. Default: HEUTE. " +
                                    "HEUTE = sofort sichtbar, MORGEN = morgen, " +
                                    "FREIBLOCK = nächster freier Schichtblock, " +
                                    "SPAETER = kein konkretes Datum.",
                            enum = listOf("HEUTE", "MORGEN", "FREIBLOCK", "SPAETER"),
                        ),
                    "severity" to
                        Schema(
                            type = SchemaType.INTEGER,
                            description =
                                "Schwere der Aufgabe von 1 (minimal) bis 10 (maximal). Default: 5.",
                        ),
                    "priorityScore" to
                        Schema(
                            type = SchemaType.NUMBER,
                            description =
                                "Prioritäts-Score von 0.0 bis 100.0. Default: 50.0. " +
                                    "Höherer Wert = weiter oben in der Liste.",
                        ),
                ),
            required = listOf("titel", "beschreibung", "kategorie"),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: return ToolResult.Failure(
                "Ungültige Argumente: JsonObject erwartet."
            )

            // --- Pflicht-Parameter auslesen ---
            val titel = obj["titel"]?.jsonPrimitive?.content?.trim()
                ?: return ToolResult.Failure("Parameter 'titel' fehlt oder ist leer.")

            val beschreibung = obj["beschreibung"]?.jsonPrimitive?.content?.trim()
                ?: return ToolResult.Failure("Parameter 'beschreibung' fehlt oder ist leer.")

            val kategorieRaw = obj["kategorie"]?.jsonPrimitive?.content?.trim()
                ?: return ToolResult.Failure("Parameter 'kategorie' fehlt oder ist leer.")

            // --- Längen-Validierung ---
            if (titel.length > 200) {
                return ToolResult.Failure(
                    "Parameter 'titel' ist zu lang (${titel.length} Zeichen, Max 200)."
                )
            }
            if (beschreibung.length > 2000) {
                return ToolResult.Failure(
                    "Parameter 'beschreibung' ist zu lang (${beschreibung.length} Zeichen, Max 2000)."
                )
            }

            // --- Optionale Parameter ---
            val bucketRaw = obj["bucket"]?.jsonPrimitive?.content?.trim() ?: "HEUTE"
            val severity = (obj["severity"]?.jsonPrimitive?.content?.toIntOrNull() ?: 5)
                .coerceIn(1, 10)
            val priorityScore = (obj["priorityScore"]?.jsonPrimitive?.content?.toDoubleOrNull()
                ?: 50.0).coerceIn(0.0, 100.0)

            // --- Enum-Konvertierung mit verständlicher Fehlermeldung ---
            val kategorie = try {
                EntropyCategory.valueOf(kategorieRaw)
            } catch (e: IllegalArgumentException) {
                return ToolResult.Failure(
                    "Unbekannte Kategorie: '$kategorieRaw'. " +
                        "Erlaubt: KOERPERLICH, MENTAL, ZEITLICH, EMOTIONAL, " +
                        "GESUNDHEITLICH, UMGEBUNG, SONSTIGES."
                )
            }

            val bucket = try {
                TimeBucket.valueOf(bucketRaw)
            } catch (e: IllegalArgumentException) {
                return ToolResult.Failure(
                    "Unbekannter Bucket: '$bucketRaw'. " +
                        "Erlaubt: HEUTE, MORGEN, FREIBLOCK, SPAETER."
                )
            }

            // --- Entity aufbauen ---
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val entity = EntropyEntryEntity(
                id = id,
                rawTranscript = beschreibung,
                title = titel,
                description = beschreibung,
                category = kategorie,
                severity = severity,
                priorityScore = priorityScore,
                priorityReason = "Vom Agentic-AI angelegt",
                status = EntryStatus.OFFEN,
                timeBucket = bucket,
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
                put("bucket", bucket.name)
                put("severity", severity)
                put("priorityScore", priorityScore)
                put("message", "Aufgabe angelegt")
            }

            ToolResult.Success(
                data = resultJson,
                createdEntityIds = listOf(id),
            )
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Anlegen der Aufgabe: ${t.message ?: t::class.simpleName}"
            )
        }
    }
}

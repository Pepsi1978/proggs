package de.frank.entropyreducer.domain.agentic.tools.write

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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Write-Tool (kritisch): Löscht eine Aufgabe irreversibel aus der Datenbank.
 *
 * ACHTUNG: Die Foreign-Key-CASCADE-Regel in EntropyEntryFollowupEntity sorgt dafür,
 * dass alle zugehörigen Followups automatisch mitgelöscht werden.
 *
 * Sicherheits-Mechanismus: Der Parameter `confirm=true` muss explizit übergeben werden.
 * Fehlt er oder ist er false, bricht das Tool mit einer Erklärung ab — Gemini muss
 * den Nutzer explizit um Bestätigung bitten, bevor es den Aufruf wiederholt.
 *
 * Für "Aufgabe als erledigt markieren" ist update_aufgabe_status mit REDUZIERT
 * das richtige Tool — DELETE sollte wirklich nur bei echtem Lösch-Wunsch genutzt werden.
 */
@Singleton
class DeleteAufgabeTool
@Inject
constructor(private val entryRepository: EntryRepository) : AgenticTool {

    override val name: String = "delete_aufgabe"

    override val category: ToolCategory = ToolCategory.DELETE_ANY

    override val isWriteTool: Boolean = true

    override val description: String =
        "Löscht eine Aufgabe komplett. ACHTUNG: irreversibel. " +
            "Nutze NUR wenn der Nutzer ausdrücklich löschen will (nicht abhaken — " +
            "dafür ist update_aufgabe_status mit REDUZIERT). " +
            "Erfordert eine bewusste Bestätigung via confirm=true."

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Parameter für die irreversible Löschung einer Aufgabe.",
            properties =
                mapOf(
                    "id" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Die eindeutige ID der zu löschenden Aufgabe.",
                        ),
                    "confirm" to
                        Schema(
                            type = SchemaType.BOOLEAN,
                            description =
                                "Muss explizit true sein. Sicherheits-Doppelbestätigung — " +
                                    "frage den Nutzer, bevor du diesen Wert auf true setzt.",
                        ),
                ),
            required = listOf("id", "confirm"),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: return ToolResult.Failure("Ungültige Argumente.")

            val id = obj["id"]?.jsonPrimitive?.content?.trim()
                ?: return ToolResult.Failure("Parameter 'id' fehlt.")

            // Sicherheitscheck: confirm muss explizit true sein
            val confirmRaw = obj["confirm"]?.jsonPrimitive?.content
            val confirmed = confirmRaw?.toBooleanStrictOrNull() ?: false

            if (!confirmed) {
                return ToolResult.Failure(
                    "Löschung erfordert explizite Bestätigung. " +
                        "Setze confirm=true wenn du sicher bist. " +
                        "Hinweis: Zum Abhaken einer Aufgabe nutze stattdessen " +
                        "update_aufgabe_status mit status=REDUZIERT."
                )
            }

            // Aufgabe aus der Datenbank holen (für den Titel im Rückgabe-JSON)
            val existing = entryRepository.get(id)
                ?: return ToolResult.Failure("Aufgabe mit id='$id' nicht gefunden.")

            entryRepository.delete(existing)

            val resultJson = buildJsonObject {
                put("id", id)
                put("titel", existing.title)
                put(
                    "message",
                    "Aufgabe '${existing.title}' wurde gelöscht. " +
                        "Alle zugehörigen Nachträge wurden ebenfalls entfernt.",
                )
            }

            ToolResult.Success(
                data = resultJson,
                deletedEntityIds = listOf(id),
            )
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Löschen der Aufgabe: ${t.message ?: t::class.simpleName}"
            )
        }
    }
}

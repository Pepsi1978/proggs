package de.frank.entropyreducer.domain.agentic.tools.write

import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
import de.frank.entropyreducer.domain.model.EntryStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Write-Tool: Setzt den Status eines Aufgaben-Eintrags.
 *
 * Erlaubte Status-Werte: OFFEN, IN_ARBEIT, REDUZIERT, ARCHIVIERT.
 * Bei REDUZIERT wird zusätzlich resolvedAt auf den aktuellen Zeitstempel gesetzt.
 *
 * Voraussetzung: Die Aufgaben-ID muss bekannt sein — ggf. vorher read_aufgaben aufrufen.
 */
@Singleton
class UpdateAufgabeStatusTool
@Inject
constructor(private val entryRepository: EntryRepository) : AgenticTool {

    override val name: String = "update_aufgabe_status"

    override val category: ToolCategory = ToolCategory.WRITE_AUFGABEN

    override val isWriteTool: Boolean = true

    override val description: String =
        "Setzt den Status eines existierenden Aufgaben-Eintrags auf OFFEN, IN_ARBEIT, " +
            "REDUZIERT (erledigt) oder ARCHIVIERT. Nutze das wenn der Nutzer eine Aufgabe " +
            "abhakt, in Arbeit nimmt oder als erledigt markiert. Erforderlich ist die ID — " +
            "verwende vorher read_aufgaben um die ID zu finden."

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Parameter für die Status-Aktualisierung.",
            properties =
                mapOf(
                    "id" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Die eindeutige ID der Aufgabe (UUID-Format).",
                        ),
                    "status" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Neuer Status der Aufgabe.",
                            enum = listOf("OFFEN", "IN_ARBEIT", "REDUZIERT", "ARCHIVIERT"),
                        ),
                ),
            required = listOf("id", "status"),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: return ToolResult.Failure("Ungültige Argumente.")

            val id = obj["id"]?.jsonPrimitive?.content?.trim()
                ?: return ToolResult.Failure("Parameter 'id' fehlt.")

            val statusRaw = obj["status"]?.jsonPrimitive?.content?.trim()
                ?: return ToolResult.Failure("Parameter 'status' fehlt.")

            // Prüfe ob der Status-Wert gültig ist
            val newStatus = try {
                EntryStatus.valueOf(statusRaw)
            } catch (e: IllegalArgumentException) {
                return ToolResult.Failure(
                    "Ungültiger Status: '$statusRaw'. Erlaubt: OFFEN, IN_ARBEIT, REDUZIERT, ARCHIVIERT."
                )
            }

            // Eintrag aus der Datenbank holen
            val existing = entryRepository.get(id)
                ?: return ToolResult.Failure("Aufgabe mit id='$id' nicht gefunden.")

            val oldStatus = existing.status
            val now = System.currentTimeMillis()

            // resolvedAt nur setzen wenn Status auf REDUZIERT wechselt;
            // bei Rückstufung (z.B. REDUZIERT → OFFEN) bleibt der alte Wert erhalten
            val resolvedAt = when (newStatus) {
                EntryStatus.REDUZIERT -> now
                else -> existing.resolvedAt
            }

            val updated = existing.copy(
                status = newStatus,
                updatedAt = now,
                resolvedAt = resolvedAt,
            )

            entryRepository.update(updated)

            val resultJson = buildJsonObject {
                put("id", id)
                put("oldStatus", oldStatus.name)
                put("newStatus", newStatus.name)
                put("message", "Status von '${oldStatus.name}' auf '${newStatus.name}' geändert.")
            }

            ToolResult.Success(
                data = resultJson,
                updatedEntityIds = listOf(id),
            )
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Aktualisieren des Status: ${t.message ?: t::class.simpleName}"
            )
        }
    }
}

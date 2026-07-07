package de.frank.entropyreducer.domain.agentic.tools.write

import dagger.Lazy
import de.frank.entropyreducer.data.local.dao.EntropyEntryFollowupDao
import de.frank.entropyreducer.data.local.entities.EntropyEntryFollowupEntity
import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Write-Tool: Hängt einen Followup-Nachtrag an eine bestehende Aufgabe.
 *
 * Folgenachrichten erscheinen im Detail-Screen als eigene Karten mit Zeitstempel —
 * sie ersetzen das frühere Muster "Text an description anhängen" (Frank-Wunsch 2026-05-20).
 *
 * Das DAO wird direkt injiziert, da kein eigenes FollowupRepository existiert.
 * Hilt registriert DAOs über DatabaseModule und stellt sie per @Provides bereit.
 *
 * Der übergebene Text wird als rawText gespeichert; improvedText und isImproved
 * bleiben false — KI-Verbesserung läuft nur interaktiv über den UI-Button.
 */
@Singleton
class AddFollowupToAufgabeTool
@Inject
constructor(
    private val entryRepository: EntryRepository,
    private val followupDao: EntropyEntryFollowupDao,
    // Frank-Bugfix 2026-05-22: Nach jedem Followup sofort Drive-Sync triggern
    // damit der KI-erstellte Nachtrag auf dem anderen Geraet sofort ankommt.
    private val syncCoordinator: Lazy<SyncCoordinator>,
) : AgenticTool {

    override val name: String = "add_followup_to_aufgabe"

    override val category: ToolCategory = ToolCategory.WRITE_AUFGABEN

    override val isWriteTool: Boolean = true

    override val description: String =
        "Fügt einer existierenden Aufgabe eine Folgemeldung (Update/Notiz) hinzu. " +
            "Nutze das wenn der Nutzer Fortschritt oder Beobachtungen zu einer bereits " +
            "angelegten Aufgabe ergänzen will, ohne ein neues Action-Item zu erstellen."

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Parameter für den neuen Followup-Eintrag.",
            properties =
                mapOf(
                    "aufgabe_id" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "ID der Aufgabe, zu der der Nachtrag gehört.",
                        ),
                    "text" to
                        Schema(
                            type = SchemaType.STRING,
                            description =
                                "Text des Nachtrags (max. 2000 Zeichen). " +
                                    "Wird als Originaltext gespeichert — keine KI-Verbesserung.",
                        ),
                ),
            required = listOf("aufgabe_id", "text"),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: return ToolResult.Failure("Ungültige Argumente.")

            val aufgabeId = obj["aufgabe_id"]?.jsonPrimitive?.content?.trim()
                ?: return ToolResult.Failure("Parameter 'aufgabe_id' fehlt.")

            val text = obj["text"]?.jsonPrimitive?.content
                ?: return ToolResult.Failure("Parameter 'text' fehlt.")

            if (text.isBlank()) {
                return ToolResult.Failure("Der Nachtrag-Text darf nicht leer sein.")
            }

            // Text auf 2000 Zeichen begrenzen
            val safeText = text.take(2000)

            // Prüfen ob die übergeordnete Aufgabe existiert
            entryRepository.get(aufgabeId)
                ?: return ToolResult.Failure("Aufgabe mit id='$aufgabeId' nicht gefunden.")

            val now = System.currentTimeMillis()
            val followupId = UUID.randomUUID().toString()

            val followup = EntropyEntryFollowupEntity(
                id = followupId,
                entryId = aufgabeId,
                rawText = safeText,
                improvedText = null,
                isImproved = false,
                createdAt = now,
                updatedAt = now,
            )

            followupDao.upsert(followup)
            syncCoordinator.get().requestSync("Agentic-AI: Aufgaben-Nachtrag hinzugefuegt")

            val resultJson = buildJsonObject {
                put("followupId", followupId)
                put("aufgabeId", aufgabeId)
                put("textLaenge", safeText.length)
                put("message", "Nachtrag erfolgreich hinzugefügt.")
            }

            ToolResult.Success(
                data = resultJson,
                createdEntityIds = listOf(followupId),
                updatedEntityIds = listOf(aufgabeId),
            )
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Hinzufügen des Nachtrags: ${t.message ?: t::class.simpleName}"
            )
        }
    }
}

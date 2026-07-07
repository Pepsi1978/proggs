package de.frank.entropyreducer.domain.agentic.tools.write

import dagger.Lazy
import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Write-Tool: überschreibt das Profil-Freitextfeld in AppSettings.
 *
 * SICHERHEITS-DESIGN:
 * - Dieses Tool ist per Default NICHT freigeschaltet (isWriteTool = true +
 *   WRITE_PROFIL-Kategorie erfordern explizite Aktivierung im Prompt-Editor).
 * - Zusätzlich verlangt das Tool den Parameter confirm=true als
 *   Doppelt-Bestätigung, damit ein versehentlicher Aufruf das Profil
 *   nicht überschreibt.
 * - Die KI soll confirm ERST setzen wenn der Nutzer ausdrücklich zugestimmt hat.
 */
@Singleton
class UpdateProfilTool
@Inject
constructor(
    private val appSettings: AppSettings,
    // Frank-Bugfix 2026-05-22: Profil-Update triggert sofort Drive-Sync.
    private val syncCoordinator: Lazy<SyncCoordinator>,
) : AgenticTool {

    override val name: String = "update_profil"

    override val category: ToolCategory = ToolCategory.WRITE_PROFIL

    override val isWriteTool: Boolean = true

    override val description: String =
        "ACHTUNG: überschreibt das gesamte Profil-Feld. Standardmäßig NICHT freigeschaltet — " +
            "Frank muss diesen Tool-Eintrag in den Prompt-Einstellungen explizit aktivieren. " +
            "Nutze NUR wenn der Nutzer ausdrücklich seine Profil-Daten ändern will."

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Parameter fuer die Profil-Aktualisierung.",
            properties =
                mapOf(
                    "neuer_text" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Der vollständige neue Profil-Text (max. 10.000 Zeichen).",
                        ),
                    "confirm" to
                        Schema(
                            type = SchemaType.BOOLEAN,
                            description = "Muss explizit true sein — Sicherheits-Bestätigung. Ohne true wird die Änderung NICHT gespeichert.",
                        ),
                ),
            required = listOf("neuer_text", "confirm"),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: JsonObject(emptyMap())

            val neuerText = obj["neuer_text"]?.jsonPrimitive?.content
                ?: return ToolResult.Failure("Parameter 'neuer_text' ist erforderlich.")

            // Doppelt-Bestätigung prüfen — Fehler mit klarer Anweisung an Gemini
            val confirm = obj["confirm"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
            if (!confirm) {
                return ToolResult.Failure(
                    "Profil-Änderung erfordert explizite Bestätigung. Setze confirm=true — aber " +
                        "frage den Nutzer ZUERST ob er sein Profil wirklich überschreiben möchte."
                )
            }

            // Länge begrenzen
            if (neuerText.length > MAX_PROFILE_LENGTH) {
                return ToolResult.Failure(
                    "Profil-Text zu lang: ${neuerText.length} Zeichen (Maximum: $MAX_PROFILE_LENGTH)."
                )
            }

            // Alten Wert für die Rückmeldung lesen
            val oldText = appSettings.profileTextFlow.first()
            val oldLength = oldText.length

            appSettings.setProfileText(neuerText)
            syncCoordinator.get().requestSync("Agentic-AI: Profiltext aktualisiert")

            val resultJson = buildJsonObject {
                put("oldLength", oldLength)
                put("newLength", neuerText.length)
                put("message", "Profil erfolgreich aktualisiert ($oldLength → ${neuerText.length} Zeichen).")
            }

            ToolResult.Success(
                data = resultJson,
                updatedEntityIds = listOf("profile_text"),
            )
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Aktualisieren des Profils: ${t.message ?: t::class.simpleName}"
            )
        }
    }

    companion object {
        private const val MAX_PROFILE_LENGTH = 10_000
    }
}

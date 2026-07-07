package de.frank.entropyreducer.domain.agentic.tools.write

import de.frank.entropyreducer.data.local.entities.ScientistSessionEntity
import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.repository.ScientistRepository
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
import de.frank.entropyreducer.domain.model.ScientistRole
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Write-Tool: startet eine neue Forscher-Diskussions-Session.
 *
 * Legt eine ScientistSessionEntity an und fügt optional eine erste
 * Kontext-Message (Rolle: NUTZER) ein, wenn initial_kontext angegeben wurde.
 * Die Session ist sofort aktiv (isArchived = false).
 */
@Singleton
class CreateForscherSessionTool
@Inject
constructor(private val scientistRepository: ScientistRepository) : AgenticTool {

    override val name: String = "create_forscher_session"

    override val category: ToolCategory = ToolCategory.WRITE_FORSCHER

    override val isWriteTool: Boolean = true

    override val description: String =
        "Startet eine neue Forscher-Diskussions-Session. Der Forscher ist die KI-Persoenlichkeit " +
            "fuer hypothesengetriebene Erkundungen. Nutze das wenn der Nutzer ein neues Thema " +
            "mit dem Forscher diskutieren will."

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Parameter fuer die neue Forscher-Session.",
            properties =
                mapOf(
                    "thema" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Titel / Thema der neuen Session, z.B. 'Schlafoptimierung durch Abend-Routine'.",
                        ),
                    "initial_kontext" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Optionaler Einstiegstext der als erste Nutzer-Message in die Session eingefügt wird.",
                        ),
                ),
            required = listOf("thema"),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: JsonObject(emptyMap())

            val thema = obj["thema"]?.jsonPrimitive?.content?.trim()
                ?: return ToolResult.Failure("Parameter 'thema' ist erforderlich.")
            val initialKontext = obj["initial_kontext"]?.jsonPrimitive?.content?.trim()

            val now = System.currentTimeMillis()
            val sessionId = UUID.randomUUID().toString()

            val session = ScientistSessionEntity(
                id = sessionId,
                title = thema,
                createdAt = now,
                lastActiveAt = now,
                isArchived = false,
            )
            scientistRepository.upsertSession(session)

            // Optionalen Einstiegs-Kontext als erste Nutzer-Message speichern
            if (!initialKontext.isNullOrBlank()) {
                val message = de.frank.entropyreducer.data.local.entities.ScientistMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = ScientistRole.NUTZER,
                    content = initialKontext,
                    createdAt = now,
                    attachedHypothesisIds = emptyList(),
                )
                scientistRepository.insertMessage(message)
            }

            val resultJson = buildJsonObject {
                put("sessionId", sessionId)
                put("thema", thema)
                put("hasInitialKontext", initialKontext != null)
                put("message", "Forscher-Session erfolgreich gestartet.")
            }

            ToolResult.Success(
                data = resultJson,
                createdEntityIds = listOf(sessionId),
            )
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Starten der Forscher-Session: ${t.message ?: t::class.simpleName}"
            )
        }
    }
}

package de.frank.entropyreducer.domain.agentic.tools.write

import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.repository.MemoryRepository
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
import de.frank.entropyreducer.domain.model.MemorySource
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Write-Tool: fügt einen neuen Memory-Eintrag ins Langzeitgedächtnis ein.
 *
 * Source wird immer als KI_VORSCHLAG gesetzt, da dieses Tool von der
 * agentic KI aufgerufen wird — nicht vom Nutzer direkt.
 * Der Nutzer kann den Eintrag anschließend im Memory-Manager bestätigen
 * oder löschen.
 */
@Singleton
class AddMemoryTool
@Inject
constructor(private val memoryRepository: MemoryRepository) : AgenticTool {

    override val name: String = "add_memory"

    override val category: ToolCategory = ToolCategory.WRITE_MEMORY

    override val isWriteTool: Boolean = true

    override val description: String =
        "Fügt einen neuen Memory-Eintrag hinzu — das ist das Langzeitgedächtnis über den Nutzer " +
            "das die KI bei späteren Antworten berücksichtigt. Nutze das wenn etwas Wichtiges " +
            "über den Nutzer erinnerungswürdig ist (Präferenzen, Lebenssituation, Gesundheitsfakten)."

    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Parameter fuer den neuen Memory-Eintrag.",
            properties =
                mapOf(
                    "content" to
                        Schema(
                            type = SchemaType.STRING,
                            description = "Der zu merkende Inhalt, z.B. 'Frank schläft schlecht bei Vollmond'.",
                        ),
                    "confidence" to
                        Schema(
                            type = SchemaType.INTEGER,
                            description = "Konfidenz 0..100 wie sicher dieser Fakt ist. Default: 70.",
                        ),
                ),
            required = listOf("content"),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val obj = args as? JsonObject ?: JsonObject(emptyMap())

            val content = obj["content"]?.jsonPrimitive?.content?.trim()
                ?: return ToolResult.Failure("Parameter 'content' ist erforderlich.")

            if (content.isBlank()) {
                return ToolResult.Failure("Parameter 'content' darf nicht leer sein.")
            }

            val confidence = (obj["confidence"]?.jsonPrimitive?.content?.toIntOrNull() ?: DEFAULT_CONFIDENCE)
                .coerceIn(0, 100)

            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()

            val entity = MemoryEntryEntity(
                id = id,
                content = content,
                source = MemorySource.KI_VORSCHLAG,
                isActive = true,
                confidence = confidence,
                createdAt = now,
                updatedAt = now,
            )

            memoryRepository.upsert(entity)

            val resultJson = buildJsonObject {
                put("id", id)
                put("content", content)
                put("confidence", confidence)
                put("source", MemorySource.KI_VORSCHLAG.name)
                put("message", "Memory-Eintrag erfolgreich gespeichert.")
            }

            ToolResult.Success(
                data = resultJson,
                createdEntityIds = listOf(id),
            )
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Speichern des Memory-Eintrags: ${t.message ?: t::class.simpleName}"
            )
        }
    }

    companion object {
        private const val DEFAULT_CONFIDENCE = 70
    }
}

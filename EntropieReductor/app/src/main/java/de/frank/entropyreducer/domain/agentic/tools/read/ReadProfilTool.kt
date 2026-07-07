package de.frank.entropyreducer.domain.agentic.tools.read

import de.frank.entropyreducer.data.remote.Schema
import de.frank.entropyreducer.data.remote.SchemaType
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.agentic.AgenticTool
import de.frank.entropyreducer.domain.agentic.ToolCategory
import de.frank.entropyreducer.domain.agentic.ToolContext
import de.frank.entropyreducer.domain.agentic.ToolResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Read-Tool: liest den persönlichen Profil-Text des Nutzers.
 *
 * Nutze dieses Tool am Anfang einer Analyse oder Empfehlung wenn du die persönliche
 * Lebenssituation, Ziele und Präferenzen des Nutzers kennen möchtest. Der Profil-Text
 * enthält Stammdaten und Kontext den der Nutzer manuell hinterlegt hat und der dir
 * hilft, Antworten besser auf seine Situation zuzuschneiden.
 *
 * Keine Parameter — gibt immer den vollständigen Profil-Text zurück.
 */
@Singleton
class ReadProfilTool
@Inject
constructor(private val appSettings: AppSettings) : AgenticTool {

    override val name: String = "read_profil"

    override val category: ToolCategory = ToolCategory.READ_PROFIL

    override val description: String =
        "Liest den persönlichen Profil-Text des Nutzers (Stammdaten, Lebenssituation, Ziele, " +
            "Präferenzen). Nutze dieses Tool wenn du den Nutzerprofil-Kontext benötigst um " +
            "Empfehlungen oder Analysen besser auf seine individuelle Situation abzustimmen."

    override val isWriteTool: Boolean = false

    // Kein Parameter-Schema nötig — das Tool hat keine Eingabe-Parameter
    override val parameterSchema: Schema =
        Schema(
            type = SchemaType.OBJECT,
            description = "Dieses Tool benötigt keine Parameter.",
            properties = emptyMap(),
            required = emptyList(),
        )

    override suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult {
        return try {
            val profilText = appSettings.profileTextFlow.first()
            val isEmpty = profilText.isBlank()

            val resultJson =
                buildJsonObject {
                    put(
                        "profil",
                        if (isEmpty) "Kein Profil hinterlegt." else profilText,
                    )
                    put("isEmpty", isEmpty)
                    put("length", profilText.length)
                }

            ToolResult.Success(data = resultJson)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            ToolResult.Failure(
                message = "Fehler beim Lesen des Profils: ${t.message ?: t::class.simpleName}"
            )
        }
    }
}

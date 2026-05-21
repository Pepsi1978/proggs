package de.frank.entropyreducer.domain.agentic

import de.frank.entropyreducer.data.remote.Schema
import kotlinx.serialization.json.JsonElement

/**
 * Interface fuer ein einzelnes Tool das ein agentic-Prompt aufrufen kann.
 *
 * Jedes Tool ist eine eigenstaendige Klasse mit @Inject constructor und wird
 * im AgenticModule per @Binds @IntoSet registriert. Der WorkflowRunner liest
 * die ToolRegistry, baut die FunctionDeclarations fuer Gemini, und ruft
 * `execute()` wenn Gemini einen FunctionCall mit `name == this.name` schickt.
 *
 * Read- vs Write-Tools:
 * - Read-Tools (isWriteTool=false) sind per Default fuer alle Prompts erlaubt
 * - Write-Tools (isWriteTool=true) brauchen pro-Prompt-Freischaltung in
 *   prompt_tool_permissions + ggf. Confirm-Dialog
 */
interface AgenticTool {

    /**
     * Eindeutige Tool-ID, wird Gemini als FunctionDeclaration.name geliefert
     * und matched mit FunctionCall.name beim Aufruf. Convention: snake_case,
     * z.B. "read_entropie_eintraege" oder "create_aufgabe".
     */
    val name: String

    /**
     * Kategorie zur Sortierung im Permission-Editor und Tools-Uebersicht.
     */
    val category: ToolCategory

    /**
     * Beschreibung fuer Gemini — was tut das Tool, wann ist es sinnvoll?
     * Wird in das FunctionDeclaration.description-Feld kopiert. Gemini
     * liest das beim Reasoning ob das Tool aufgerufen werden soll.
     */
    val description: String

    /**
     * true wenn das Tool Daten aendert (schreibend). Liest der PermissionGate
     * und der ConfirmationGate.
     *
     * WARNUNG (Loop-5-Fix L5-6): Diese Flag wird vom Tool SELBER deklariert —
     * es gibt KEINE strukturelle Runtime-Validierung dass ein isWriteTool=false-
     * Tool wirklich nichts schreibt. Wenn ein neues Tool faelschlich false
     * deklariert aber repo.upsert/delete/update aufruft, umgeht es den
     * Permission-/Confirm-Gate. Konsequenz: persoenliche Daten koennten ohne
     * Frank's Zustimmung geaendert werden.
     *
     * Code-Review-Pflicht fuer neue Tools:
     *  - isWriteTool=true wenn execute() IRGENDEINE write-Operation macht
     *    (upsert, delete, update, insert)
     *  - isWriteTool=false NUR bei reinen Read-Queries
     */
    val isWriteTool: Boolean

    /**
     * JSON-Schema der Argumente. null wenn das Tool keine Parameter hat.
     */
    val parameterSchema: Schema?

    /**
     * Tool ausfuehren. Args sind direkt das was Gemini im FunctionCall.args
     * geschickt hat — kann JsonObject oder andere JSON-Strukturen sein.
     * Return-Wert wird vom WorkflowRunner serialisiert und als
     * FunctionResponse an Gemini zurueckgegeben.
     *
     * Bei Erfolg: ToolResult.Success mit dem Ergebnis-JsonElement.
     * Bei Fehler: ToolResult.Failure mit menschenlesbarer Begruendung.
     *
     * Implementierungen sollten Exceptions abfangen und als Failure zurueckgeben,
     * statt zu werfen — sonst stirbt der ganze ReAct-Loop.
     */
    suspend fun execute(args: JsonElement, ctx: ToolContext): ToolResult
}

/**
 * Kategorien fuer das Permission-Editor-UI und die Tools-Uebersicht in den
 * Einstellungen. Jeder Wert hat ein Anzeige-Label auf Deutsch.
 */
enum class ToolCategory(val label: String, val isReadCategory: Boolean) {
    // Read-Kategorien
    READ_ENTROPIE("Entropie lesen", true),
    READ_THESEN("Thesen lesen", true),
    READ_AUFGABEN("Aufgaben lesen", true),
    READ_MEMORY("Gedaechtnis lesen", true),
    READ_PROFIL("Profil lesen", true),
    READ_INSIGHTS("Insights lesen", true),
    READ_BIOMARKER("Biomarker lesen", true),
    READ_HYPOTHESEN("Hypothesen lesen", true),
    READ_FORSCHER("Forscher-Sessions lesen", true),
    READ_PROMPT_HISTORY("Prompt-Verlauf lesen", true),
    READ_CODEX("Codex lesen", true),
    READ_KALENDER("Kalender lesen", true),

    // Write-Kategorien
    WRITE_ENTROPIE("Entropie schreiben", false),
    WRITE_THESEN("Thesen schreiben", false),
    WRITE_AUFGABEN("Aufgaben schreiben", false),
    WRITE_MEMORY("Gedaechtnis schreiben", false),
    WRITE_PROFIL("Profil aendern", false),
    WRITE_HYPOTHESEN("Hypothesen schreiben", false),
    WRITE_FORSCHER("Forscher-Sessions starten", false),
    WRITE_CODEX("Codex schreiben", false),
    DELETE_ANY("Loeschen erlaubt", false),
}

/**
 * Kontext der bei jedem Tool-Aufruf mitgegeben wird. Erlaubt dem Tool zu
 * wissen IN WELCHEM Lauf es ausgefuehrt wird (fuer Logging, Cross-Prompt-Read,
 * Token-Tracking).
 */
data class ToolContext(
    val promptId: String,
    val executionId: String,
    val stepIndex: Int,
    val startedAt: Long,
    /**
     * Optional: Kontext den der Nutzer beim Ausfuehren mitgegeben hat
     * (z.B. "fokussiere dich auf den Bereich Schlaf").
     */
    val userInputContext: String? = null,
)

/**
 * Ergebnis eines Tool-Aufrufs. Wird vom WorkflowRunner serialisiert und an
 * Gemini als FunctionResponse zurueckgeschickt.
 *
 * Bei Success: data ist das JSON-Result. createdEntityIds/updatedEntityIds/
 * deletedEntityIds sind fuer Rollback und Audit-Log relevant — pro Step ist
 * nachvollziehbar welche DB-Eintraege beruehrt wurden.
 *
 * Bei Failure: message ist menschenlesbar, data kann zusaetzliche Details
 * fuer Gemini enthalten (z.B. Validation-Fehler-Liste).
 */
sealed class ToolResult {
    abstract val data: JsonElement?

    data class Success(
        override val data: JsonElement,
        val createdEntityIds: List<String> = emptyList(),
        val updatedEntityIds: List<String> = emptyList(),
        val deletedEntityIds: List<String> = emptyList(),
    ) : ToolResult()

    data class Failure(
        val message: String,
        override val data: JsonElement? = null,
    ) : ToolResult()
}

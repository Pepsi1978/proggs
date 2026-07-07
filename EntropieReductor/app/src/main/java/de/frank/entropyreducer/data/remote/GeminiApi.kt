package de.frank.entropyreducer.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Google Gemini API.
 *
 * Endpoint: POST /v1beta/models/{model}:generateContent
 *
 * Function Calling (Frank-Wunsch 2026-05-21): Die API kann Tools bekommen — Gemini
 * entscheidet selber wann es ein Tool aufruft (ReAct-Pattern). Tool-Aufrufe kommen
 * als `functionCall` in den Antwort-Parts zurueck, die App fuehrt das Tool aus und
 * schickt das Ergebnis als `functionResponse` zurueck. Der Loop laeuft bis Gemini
 * eine reine Text-Antwort schickt.
 *
 * Doku: https://ai.google.dev/gemini-api/docs/function-calling
 */
interface GeminiApi {

    @POST("models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest,
    ): GeminiResponse
}

// ============================================================================
// Request-Seite
// ============================================================================

@Serializable
data class GeminiRequest(
    val systemInstruction: GeminiContent? = null,
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    /**
     * Function Calling: Liste von Tool-Gruppen die Gemini aufrufen darf. Jede Tool-Gruppe
     * enthaelt eine Liste von Function Declarations. Wir nutzen typischerweise EINE
     * Tool-Gruppe mit allen Funktions-Deklarationen.
     */
    val tools: List<Tool>? = null,
    /**
     * Steuert wie Gemini mit den Tools umgeht. Default: AUTO (Gemini entscheidet selbst).
     * Optional: ANY (Gemini MUSS irgendein Tool aufrufen) oder NONE (Tools sind verfuegbar
     * aber Gemini soll nur Text antworten).
     */
    val toolConfig: ToolConfig? = null,
)

@Serializable
data class GeminiContent(
    /** "user" oder "model" oder "function". Bei systemInstruction null. */
    val role: String? = null,
    val parts: List<GeminiPart>,
)

/**
 * Ein einzelner Teil einer GeminiContent. Genau EINES der folgenden Felder ist
 * gesetzt — entweder text, oder functionCall, oder functionResponse, oder
 * inlineData. Wir behalten den positional-Constructor mit text als erstem Param
 * fuer Rueckwaerts-Kompatibilitaet.
 *
 * Achtung: `text` ist seit der Function-Calling-Erweiterung nullable. Bestehende
 * Aufrufer die `?.text` ueber eine `?.firstOrNull()`-Kette machen sind nicht
 * betroffen, da der Type schon vorher als String? aufgeloest wurde.
 */
@Serializable
data class GeminiPart(
    val text: String? = null,
    val functionCall: FunctionCall? = null,
    val functionResponse: FunctionResponse? = null,
    val inlineData: InlineData? = null,
) {
    companion object {
        /** Convenience: nur Text. */
        fun text(value: String): GeminiPart = GeminiPart(text = value)

        /** Convenience: Function-Call von der App zurueck an Gemini (selten direkt). */
        fun call(name: String, args: JsonElement): GeminiPart =
            GeminiPart(functionCall = FunctionCall(name = name, args = args))

        /** Convenience: Tool-Ergebnis zurueck an Gemini im naechsten Turn. */
        fun response(name: String, response: JsonElement): GeminiPart =
            GeminiPart(
                functionResponse = FunctionResponse(name = name, response = response)
            )
    }
}

/**
 * Gemini-Generierungs-Optionen. Die meisten Defaults sind sinnvoll — typischerweise
 * setzen wir nur temperature und maxOutputTokens. Function-Calling profitiert von
 * niedriger temperature (deterministischer in Tool-Wahl).
 */
@Serializable
data class GeminiGenerationConfig(
    val temperature: Double? = null,
    val responseMimeType: String? = null,
    val maxOutputTokens: Int? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val stopSequences: List<String>? = null,
)

// ============================================================================
// Function Calling — Schema
// ============================================================================

/**
 * Eine Tool-Gruppe. In aller Regel reicht ein einziges Tool-Objekt mit allen
 * functionDeclarations. Mehrere Tool-Objekte sind moeglich aber selten noetig.
 */
@Serializable
data class Tool(
    val functionDeclarations: List<FunctionDeclaration>,
)

/**
 * Beschreibung einer einzelnen Funktion die Gemini aufrufen kann.
 * - name: muss eindeutig sein, z.B. "read_entropie_eintraege"
 * - description: Gemini liest das und entscheidet WANN das Tool sinnvoll ist
 * - parameters: JSON-Schema-Beschreibung der Argumente
 */
@Serializable
data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Schema? = null,
)

/**
 * JSON-Schema fuer Funktions-Parameter. Rekursiv: bei type=OBJECT enthaelt
 * properties weitere Schemas, bei type=ARRAY enthaelt items ein Schema.
 *
 * Unterstuetzte Types: STRING, NUMBER, INTEGER, BOOLEAN, ARRAY, OBJECT.
 *
 * "required" ist nur bei type=OBJECT relevant.
 * "enum" ist nur bei type=STRING relevant und beschraenkt die erlaubten Werte.
 */
@Serializable
data class Schema(
    val type: SchemaType,
    val description: String? = null,
    val format: String? = null,
    val nullable: Boolean? = null,
    val enum: List<String>? = null,
    val properties: Map<String, Schema>? = null,
    val required: List<String>? = null,
    val items: Schema? = null,
)

@Serializable
enum class SchemaType {
    @SerialName("STRING") STRING,
    @SerialName("NUMBER") NUMBER,
    @SerialName("INTEGER") INTEGER,
    @SerialName("BOOLEAN") BOOLEAN,
    @SerialName("ARRAY") ARRAY,
    @SerialName("OBJECT") OBJECT,
}

/**
 * Wenn Gemini entscheidet ein Tool aufzurufen, kommt der Aufruf als FunctionCall
 * in einem Antwort-Part zurueck.
 * - name: matched einer FunctionDeclaration.name
 * - args: JSON-Object mit den Parameter-Werten (kann JsonObject sein)
 */
@Serializable
data class FunctionCall(
    val name: String,
    val args: JsonElement,
)

/**
 * Die Antwort der App nach einem FunctionCall — wird im naechsten Request als
 * Content mit role="function" eingefuegt.
 * - name: muss zum aufrufenden FunctionCall.name passen
 * - response: beliebiges JSON-Objekt mit dem Tool-Ergebnis
 */
@Serializable
data class FunctionResponse(
    val name: String,
    val response: JsonElement,
)

/**
 * Inline-Bild-/Audio-Daten. Wir nutzen das jetzt noch nicht, aber die Felder
 * sind im Schema damit Gemini's Antwort-Parsing nicht bricht falls Gemini mal
 * einen InlineData-Part sendet.
 */
@Serializable
data class InlineData(
    val mimeType: String,
    /** Base64-kodierte Daten. */
    val data: String,
)

/**
 * Steuert wie Gemini mit Tools umgeht.
 */
@Serializable
data class ToolConfig(
    val functionCallingConfig: FunctionCallingConfig,
)

@Serializable
data class FunctionCallingConfig(
    val mode: FunctionCallingMode,
    /** Wenn mode=ANY: optional auf bestimmte Funktionen einschraenken. */
    val allowedFunctionNames: List<String>? = null,
)

@Serializable
enum class FunctionCallingMode {
    /** Default: Gemini entscheidet selber ob ein Tool oder reiner Text. */
    @SerialName("AUTO") AUTO,
    /** Gemini MUSS irgendein Tool aufrufen — Text-Antwort wird abgelehnt. */
    @SerialName("ANY") ANY,
    /** Tools sind im Request aber Gemini soll sie ignorieren (reine Text-Antwort). */
    @SerialName("NONE") NONE,
}

// ============================================================================
// Response-Seite
// ============================================================================

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val usageMetadata: UsageMetadata? = null,
    /** Block-Reason wenn Gemini den Prompt abgelehnt hat (Safety, Recitation, ...). */
    val promptFeedback: PromptFeedback? = null,
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null,
    val safetyRatings: List<SafetyRating>? = null,
)

/**
 * Token-Verbrauch eines Aufrufs. Vom TokenMeter benoetigt um Tagesaggregation zu
 * fuehren.
 */
@Serializable
data class UsageMetadata(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null,
    val cachedContentTokenCount: Int? = null,
)

@Serializable
data class PromptFeedback(
    val blockReason: String? = null,
    val safetyRatings: List<SafetyRating>? = null,
)

@Serializable
data class SafetyRating(
    val category: String? = null,
    val probability: String? = null,
    val blocked: Boolean? = null,
)

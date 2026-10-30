package de.frank.jarvis.auth

import org.json.JSONArray
import org.json.JSONObject

/**
 * Anfrage an die Anthropic Messages API (`POST /v1/messages`, Streaming) für ein Claude-Modell.
 * Gleiche Eingabe wie [codexChatPayload]: Anweisung, Gesprächszüge, Modell und Denkstufe.
 * Denken ist bei allen vier Claude-Modellen von selbst an (adaptiv); die Tiefe steuert `output_config.effort`.
 */
internal fun anthropicChatPayload(
    instructions: String,
    turns: List<ChatTurn>,
    model: CodexModel,
    reasoningEffort: ReasoningEffort,
    mitFallback: Boolean,
): JSONObject {
    val messages = JSONArray()
    var rolle: String? = null
    var inhalt = JSONArray()
    fun abschliessen() {
        val r = rolle ?: return
        if (inhalt.length() == 0) inhalt.put(textBlock("…"))
        messages.put(JSONObject().put("role", r).put("content", inhalt))
    }
    // Anthropic erwartet abwechselnd user und assistant, beginnend mit user; aufeinanderfolgende Züge derselben Rolle werden zusammengelegt.
    turns.forEach { turn ->
        val r = if (turn.role == "assistant") "assistant" else "user"
        if (rolle == null && r == "assistant") { rolle = "user"; inhalt = JSONArray().put(textBlock("(Gesprächsbeginn)")) }
        if (r != rolle) { abschliessen(); rolle = r; inhalt = JSONArray() }
        // Bilder vor dem Text, so liest Claude sie am besten; Bilder gibt es nur in eigenen Zügen.
        if (r == "user") turn.bilder.forEach { bild -> anthropicBild(bild)?.let(inhalt::put) }
        if (turn.text.isNotBlank()) inhalt.put(textBlock(turn.text))
    }
    // Ein letzter Modell-Zug wäre ein Vorbefüllen, das die neuen Claude-Modelle ablehnen.
    if (rolle == "assistant") { abschliessen(); rolle = "user"; inhalt = JSONArray().put(textBlock("Fahre fort.")) }
    abschliessen()
    val payload = JSONObject()
        .put("model", model.apiId)
        // Immer das Maximum je Antwort: Franks Tokenlimit gilt für die ganze Aufgabe und fragt beim Erreichen nach (JarvisAgent).
        .put("max_tokens", ANTHROPIC_MAX_TOKENS)
        .put("stream", true)
        // Die große Anweisung bleibt über alle Schritte einer Antwort gleich: zwischenspeichern spart Kosten und Wartezeit.
        .put("system", anthropicSystem(instructions))
        .put("cache_control", JSONObject().put("type", "ephemeral"))
        .put("messages", messages)
        .put("output_config", JSONObject().put("effort", model.normalizeEffort(reasoningEffort).apiValue))
    // Lehnt die Sicherheitsprüfung eine Anfrage ab, rechnet Anthropic sie selbst mit einem passenden Ersatzmodell weiter.
    if (mitFallback && model.mitFallback) payload.put("fallbacks", "default")
    return payload
}

/**
 * Die Anweisung als Blöcke, getrennt an [CACHE_TRENNER]: Der Zwischenspeicher trifft nur an Blockgrenzen. In einem einzigen Block
 * änderte die Uhrzeit am Ende jede Minute den ganzen Text, und das Modell läse bei jeder neuen Frage alles neu.
 * Der erste Block (Werkzeuge, Grundsätze) bleibt eine Stunde, spätere Blöcke die üblichen 5 Minuten; längere müssen vorn stehen.
 */
internal fun anthropicSystem(instructions: String): JSONArray {
    val teile = instructions.split(CACHE_TRENNER).filter { it.isNotBlank() }
    val bloecke = JSONArray()
    teile.forEachIndexed { i, teil ->
        val cache = JSONObject().put("type", "ephemeral")
        if (i == 0 && teile.size > 1) cache.put("ttl", "1h")
        val text = if (i == teile.lastIndex) teil.trim() + "\n\n" + UMLAUT_HINWEIS else teil
        bloecke.put(textBlock(text).put("cache_control", cache))
    }
    return bloecke
}

/** Ersatzmodell bei Ablehnung gibt es für Opus, Fable und Sonnet, nicht für Haiku. */
internal val CodexModel.mitFallback: Boolean get() = istAnthropic && this != CodexModel.HAIKU

internal const val ANTHROPIC_FALLBACK_BETA = "server-side-fallback-2026-07-01"
/** Höchstwert aller vier Claude-Modelle. Bei Effort „Maximal“ verbraucht schon das Denken mehr als 64 000 Token. */
internal const val ANTHROPIC_MAX_TOKENS = 128_000

private fun textBlock(text: String) = JSONObject().put("type", "text").put("text", text)

/** data-URL (Foto, PDF-Seite) oder Adresse in einen Bild-Block. */
private fun anthropicBild(bild: String): JSONObject? {
    if (bild.startsWith("http://") || bild.startsWith("https://")) {
        return JSONObject().put("type", "image").put("source", JSONObject().put("type", "url").put("url", bild))
    }
    if (!bild.startsWith("data:")) return null
    val komma = bild.indexOf(',').takeIf { it > 0 } ?: return null
    val kopf = bild.substring(5, komma)
    if (!kopf.endsWith(";base64")) return null
    val mime = kopf.removeSuffix(";base64").ifBlank { "image/jpeg" }
    return JSONObject().put("type", "image").put("source", JSONObject()
        .put("type", "base64").put("media_type", mime).put("data", bild.substring(komma + 1)))
}

/**
 * Ordnet einen HTTP-Fehler von Anthropic zu. Bewusst nie [AuthErrorKind.REAUTH]: Das hieße „bei ChatGPT neu anmelden“,
 * ein falscher Anthropic-Schlüssel hat mit dem ChatGPT-Konto nichts zu tun.
 */
internal fun classifyAnthropicError(code: Int, body: String) {
    if (code in 200..299) return
    val message = runCatching { JSONObject(body).optJSONObject("error")?.optString("message")?.takeIf(String::isNotBlank) }.getOrNull() ?: body.take(240)
    throw when {
        code == 401 || code == 403 -> CodexAuthException(AuthErrorKind.NETWORK, "Der Anthropic-API-Schlüssel ist ungültig oder nicht freigeschaltet. Bitte unter Einstellungen → Jarvis' Kopf prüfen. $message")
        code == 429 -> CodexAuthException(AuthErrorKind.NETWORK, "Anthropic bremst gerade (Ratenlimit oder Guthaben). $message", retryable = true)
        code == 408 || code == 529 || code in 500..599 -> CodexAuthException(AuthErrorKind.NETWORK, "Anthropic ist gerade überlastet oder nicht erreichbar (Fehler $code). Bitte kurz erneut versuchen.", retryable = true)
        else -> CodexAuthException(AuthErrorKind.NETWORK, "$ANTHROPIC_FEHLER_PREFIX$code: $message")
    }
}

/** Anfang jeder abgelehnten Anfrage (4xx außer Schlüssel und Ratenlimit); bei 400 versucht Jarvis es einmal ohne Ersatzmodell-Beta. */
internal const val ANTHROPIC_FEHLER_PREFIX = "Anthropic-Fehler "

/** Sammelt den Text aus dem Anthropic-Stream (`content_block_delta` mit `text_delta`). */
internal class AnthropicSseAccumulator {
    private val text = StringBuilder()
    private var stopReason: String? = null
    var isCompleted = false
        private set
    private var eingabe = 0L
    private var ausgabe = 0L
    /** Aus dem Zwischenspeicher gelesen / neu hineingeschrieben / ungespeichert, für die Messung im Logbuch. */
    var cacheText = ""
        private set
    /** Verbrauchte Ausgabe-Token dieser Antwort, Denken eingeschlossen (aus `message_delta.usage`). */
    val ausgabeToken: Int get() = ausgabe.toInt()
    /** Verbrauchte Tokens dieser Anfrage: Eingabe (auch aus dem Zwischenspeicher) plus Ausgabe samt Denken. */
    val tokens: Long get() = eingabe + ausgabe

    fun accept(data: String): String? {
        if (data.isBlank()) return null
        val event = runCatching { JSONObject(data) }.getOrElse {
            throw CodexAuthException(AuthErrorKind.NETWORK, "Anthropic hat ein ungültiges Streaming-Ereignis geliefert.", it)
        }
        return when (event.optString("type")) {
            "content_block_delta" -> event.optJSONObject("delta")?.takeIf { it.optString("type") == "text_delta" }
                ?.optString("text")?.takeIf(String::isNotEmpty)?.also(text::append)
            "message_start" -> {
                event.optJSONObject("message")?.optJSONObject("usage")?.let { u ->
                    eingabe = u.optLong("input_tokens") + u.optLong("cache_creation_input_tokens") + u.optLong("cache_read_input_tokens")
                    ausgabe = u.optLong("output_tokens")
                    cacheText = "gelesen=${u.optLong("cache_read_input_tokens")} geschrieben=${u.optLong("cache_creation_input_tokens")} neu=${u.optLong("input_tokens")}"
                }
                null
            }
            "message_delta" -> {
                event.optJSONObject("delta")?.optString("stop_reason")?.takeIf(String::isNotBlank)?.let { stopReason = it }
                // Die Ausgabe-Zahl hier ist schon die Summe der ganzen Antwort.
                event.optJSONObject("usage")?.optLong("output_tokens")?.takeIf { it > 0 }?.let { ausgabe = it }
                null
            }
            "message_stop" -> { isCompleted = true; null }
            "error" -> {
                val fehler = event.optJSONObject("error")
                val art = fehler?.optString("type").orEmpty()
                throw CodexAuthException(AuthErrorKind.NETWORK, "Anthropic hat die Antwort abgebrochen: " + (fehler?.optString("message")?.takeIf(String::isNotBlank) ?: art),
                    retryable = art == "overloaded_error" || art == "api_error" || art == "rate_limit_error")
            }
            else -> null
        }
    }

    fun result(): String {
        if (!isCompleted) throw CodexAuthException(AuthErrorKind.NETWORK, "Die Anthropic-Verbindung endete vor dem Abschluss der Antwort.")
        // Abgelehnt oder abgeschnitten ist keine fertige Antwort: Ein halber Werkzeugaufruf würde sonst als Antwort gedeutet.
        if (stopReason == "refusal") throw CodexAuthException(AuthErrorKind.NETWORK, "Claude hat die Anfrage aus Sicherheitsgründen abgelehnt.")
        if (stopReason == "max_tokens") throw CodexAuthException(AuthErrorKind.NETWORK, "Die Claude-Antwort war zu lang und wurde abgeschnitten.")
        return text.toString().takeIf(String::isNotBlank)
            ?: throw CodexAuthException(AuthErrorKind.NETWORK, "Anthropic hat keinen Antworttext geliefert.")
    }
}

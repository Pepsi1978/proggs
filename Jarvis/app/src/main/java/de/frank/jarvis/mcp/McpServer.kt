package de.frank.jarvis.mcp

import android.content.Context
import de.frank.jarvis.BuildConfig
import de.frank.jarvis.agent.JarvisAgent
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.Ergebnis
import de.frank.jarvis.faehigkeit.KalenderFaehigkeit
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.faehigkeit.Werkzeug
import de.frank.jarvis.faehigkeit.schema
import de.frank.jarvis.faehigkeit.text
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Der MCP-Server von Jarvis (Model Context Protocol, Streamable HTTP, zustandslos, nur JSON-Antworten).
 * ChatGPT ruft ihn über den Tunnel auf. Er kennt genau vier Dinge: initialize, ping, tools/list, tools/call.
 * Es gibt bewusst keine Sitzungen — ChatGPT beginnt oft bei jedem Aufruf neu.
 */
class McpServer(context: Context) {
    private val app = context.applicationContext
    private val agent by lazy { JarvisAgent(app) }

    /** Werkzeuge von Jarvis selbst, zusätzlich zu denen der angebundenen Apps. */
    private val eigene: List<Werkzeug> = listOf(
        Werkzeug(
            name = "jarvis_status",
            titel = "Jarvis-Status",
            beschreibung = "Jarvis: sagt, ob Jarvis auf Franks Handy erreichbar ist, welche Apps angebunden sind und welches Datum und welche Uhrzeit dort gerade gelten. " +
                "Nutze es bei „Jarvis, bist du da?“ oder wenn du das heutige Datum auf dem Handy brauchst.",
            schema = schema(),
            nurLesen = true,
        ) {
            val jetzt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN))
            val apps = Register.alle(app).joinToString("; ") { f -> f.name + ": " + (f.stoerung()?.let { "gestört ($it)" } ?: "bereit") }
            val dienst = Register.alle(app).filterIsInstance<KalenderFaehigkeit>().firstOrNull()?.heuteKurz().orEmpty()
            Ergebnis("Jarvis ist bereit. Auf dem Handy ist es $jetzt. " + (if (dienst.isEmpty()) "" else "Dienst heute: $dienst. ") + "Angebundene Apps: $apps.")
        },
        Werkzeug(
            name = "jarvis_auftrag",
            titel = "Auftrag an Jarvis",
            beschreibung = "Jarvis: übergibt einen frei formulierten Auftrag an Jarvis auf Franks Handy, der ihn selbstständig mit allen angebundenen Apps erledigt " +
                "und in einem Satz antwortet. Nutze dieses Werkzeug NUR, wenn keines der anderen Werkzeuge direkt passt — etwa für Aufträge über mehrere Schritte " +
                "(„verschiebe alles von heute Nachmittag auf morgen“, „räum meine überfälligen Aufgaben auf“). Für eine einzelne Aufgabe nimm das direkte Werkzeug, das ist schneller.",
            schema = schema("auftrag" to text("Der vollständige Auftrag in Franks Worten, mit allen Angaben aus dem Gespräch."), pflicht = listOf("auftrag")),
            nurLesen = false,
        ) { a ->
            val auftrag = a.optString("auftrag").trim()
            if (auftrag.isEmpty()) Ergebnis("Der Auftrag ist leer.", fehler = true)
            // Knappes Zeitfenster: ChatGPT wartet nicht beliebig lange auf ein Werkzeug.
            else Ergebnis(agent.frage(auftrag, zeitlimitMs = 45_000, maxSchritte = 6))
        },
    )

    private fun alleWerkzeuge(): List<Werkzeug> = Register.werkzeuge(app) + eigene

    /**
     * Verarbeitet einen HTTP-Rumpf. Rückgabe: Antwort-JSON oder null, wenn nichts zu antworten ist
     * (reine Benachrichtigung → HTTP 202).
     */
    fun verarbeite(rumpf: String): String? {
        val anfang = rumpf.trimStart()
        if (anfang.startsWith("[")) {
            val stapel = runCatching { JSONArray(anfang) }.getOrElse { return fehler(JSONObject.NULL, -32700, "Ungültiges JSON").toString() }
            val antworten = (0 until stapel.length()).mapNotNull { i -> stapel.optJSONObject(i)?.let(::eine) }
            return if (antworten.isEmpty()) null else JSONArray(antworten).toString()
        }
        val anfrage = runCatching { JSONObject(anfang) }.getOrElse { return fehler(JSONObject.NULL, -32700, "Ungültiges JSON").toString() }
        return eine(anfrage)?.toString()
    }

    private fun eine(anfrage: JSONObject): JSONObject? {
        val methode = anfrage.optString("method")
        // Ohne id ist es eine Benachrichtigung (z. B. notifications/initialized): keine Antwort.
        if (!anfrage.has("id") || anfrage.isNull("id")) return null
        val id = anfrage.get("id")
        val parameter = anfrage.optJSONObject("params") ?: JSONObject()
        return when (methode) {
            "initialize" -> ergebnis(id, JSONObject()
                .put("protocolVersion", parameter.optString("protocolVersion").takeIf { it in VERSIONEN } ?: VERSIONEN.first())
                .put("capabilities", JSONObject().put("tools", JSONObject().put("listChanged", false)))
                .put("serverInfo", JSONObject().put("name", "Jarvis").put("title", "Jarvis").put("version", BuildConfig.VERSION_NAME))
                .put("instructions", anleitung()))
            "ping" -> ergebnis(id, JSONObject())
            "tools/list" -> ergebnis(id, JSONObject().put("tools", JSONArray(alleWerkzeuge().map(::beschreibe))))
            "tools/call" -> ergebnis(id, rufe(parameter.optString("name"), parameter.optJSONObject("arguments") ?: JSONObject()))
            "resources/list" -> ergebnis(id, JSONObject().put("resources", JSONArray()))
            "prompts/list" -> ergebnis(id, JSONObject().put("prompts", JSONArray()))
            else -> fehler(id, -32601, "Unbekannte Methode: $methode")
        }
    }

    private fun rufe(name: String, argumente: JSONObject): JSONObject {
        val werkzeug = alleWerkzeuge().firstOrNull { it.name == name }
            ?: return inhalt("Das Werkzeug $name gibt es nicht.", fehler = true)
        val r = runCatching { runBlocking { werkzeug.ausfuehren(argumente) } }
            .getOrElse { Ergebnis("Werkzeug fehlgeschlagen: ${it.message ?: it.javaClass.simpleName}", fehler = true) }
        Protokoll.melde(Quelle.CHATGPT, werkzeug.titel, r.text, !r.fehler)
        return inhalt(r.text, r.fehler)
    }

    private fun beschreibe(w: Werkzeug): JSONObject = JSONObject()
        .put("name", w.name)
        .put("title", w.titel)
        .put("description", w.beschreibung)
        .put("inputSchema", w.schema)
        .put("annotations", JSONObject()
            .put("title", w.titel)
            .put("readOnlyHint", w.nurLesen)
            .put("destructiveHint", w.loeschend)
            .put("idempotentHint", w.nurLesen)
            .put("openWorldHint", false))

    private fun anleitung(): String = buildString {
        append("Dies ist Jarvis, Franks persönlicher Assistent auf seinem Handy. Sagt Frank „Jarvis“ oder geht es um seine Aufgaben, Termine ")
        append("oder Erinnerungen, nutze diese Werkzeuge. Antworte danach kurz in einem Satz, was erledigt wurde, ohne ids vorzulesen. ")
        append("Fehlt eine nötige Angabe oder ist sie mehrdeutig, frage kurz nach, statt zu raten.\n")
        Register.alle(app).forEach { append(it.name).append(": ").append(it.hinweise).append('\n') }
    }

    private fun inhalt(text: String, fehler: Boolean) = JSONObject()
        .put("content", JSONArray().put(JSONObject().put("type", "text").put("text", text)))
        .put("isError", fehler)

    private fun ergebnis(id: Any, ergebnis: JSONObject) = JSONObject().put("jsonrpc", "2.0").put("id", id).put("result", ergebnis)
    private fun fehler(id: Any, code: Int, text: String) = JSONObject().put("jsonrpc", "2.0").put("id", id)
        .put("error", JSONObject().put("code", code).put("message", text))

    companion object {
        /** Unterstützte Protokollstände, neuester zuerst. Wünscht der Client einen davon, bekommt er ihn. */
        val VERSIONEN = listOf("2025-06-18", "2025-11-25", "2025-03-26", "2024-11-05")
    }
}

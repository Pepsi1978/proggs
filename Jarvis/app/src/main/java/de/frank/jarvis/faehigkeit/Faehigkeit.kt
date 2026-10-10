package de.frank.jarvis.faehigkeit

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Was ein Werkzeug zurückgibt. [struktur] (für Modell und Karte) und [meta] (nur für die Karte, das Modell sieht es
 * nicht) braucht nur ein Werkzeug, das in ChatGPT etwas anzeigt (siehe [AblageKarte]).
 */
data class Ergebnis(val text: String, val fehler: Boolean = false, val struktur: JSONObject? = null, val meta: JSONObject? = null)

/**
 * Ein Werkzeug, das ChatGPT (über das Plugin) und das eigene Modell von Jarvis gleichermaßen aufrufen können.
 * [schema] ist das JSON-Schema der Eingabe. [nurLesen] und [loeschend] gehen als Hinweise an ChatGPT.
 */
class Werkzeug(
    val name: String,
    val titel: String,
    val beschreibung: String,
    val schema: JSONObject,
    val nurLesen: Boolean,
    val loeschend: Boolean = false,
    /** Zusätzliche Angaben für ChatGPT im Werkzeug-Verzeichnis (`_meta`), etwa `openai/fileParams` für Dateieingaben. */
    val meta: JSONObject? = null,
    val ausfuehren: suspend (JSONObject) -> Ergebnis,
)

/**
 * Eine angebundene App. Jede weitere App (Ideen, Journal, Entropie-Reduktor …) wird eine eigene Klasse
 * und trägt sich in [Register.alle] ein — Plugin, Jarvis-Chat und Oberfläche übernehmen sie von selbst.
 */
interface Faehigkeit {
    val id: String
    val name: String
    val beschreibung: String
    /** Kurzer Hinweis für die Modelle, wie mit dieser App umzugehen ist. */
    val hinweise: String
    val werkzeuge: List<Werkzeug>
    /** null = bereit, sonst der Grund in einem Satz. */
    fun stoerung(): String?
    /** false = nur für Jarvis selbst und seine Agenten, nicht als Werkzeug in ChatGPT. */
    val imPlugin: Boolean get() = true
}

object Register {
    @Volatile private var liste: List<Faehigkeit>? = null

    fun alle(context: Context): List<Faehigkeit> = liste ?: synchronized(this) {
        liste ?: listOf(
            // Die Regeln stehen vorn: Manche Programme kürzen die Anleitung des Plugins, der Anfang kommt immer an.
            RegelnFaehigkeit(context.applicationContext),
            AufgabenFaehigkeit(context.applicationContext), BiomarkerFaehigkeit(context.applicationContext), KalenderFaehigkeit(context.applicationContext), WetterFaehigkeit(context.applicationContext), FahrtFaehigkeit(context.applicationContext),
            WeckerFaehigkeit(context.applicationContext), IdeenFaehigkeit(context.applicationContext), AblageFaehigkeit(context.applicationContext), MailFaehigkeit(context.applicationContext), WissenFaehigkeit(context.applicationContext), TagebuchFaehigkeit(context.applicationContext),
            WebFaehigkeit(context.applicationContext), RepoFaehigkeit(context.applicationContext),
        ).also { liste = it }
    }

    fun werkzeuge(context: Context): List<Werkzeug> = alle(context).flatMap { it.werkzeuge }

    /** Nur was ChatGPT sehen soll. */
    fun pluginWerkzeuge(context: Context): List<Werkzeug> = alle(context).filter { it.imPlugin }.flatMap { it.werkzeuge }
}

// ---- Zusammenfassen ----
// ChatGPT wählt sicherer, wenn es wenige klar getrennte Werkzeuge sieht. Verwandte Aufgaben einer App teilen
// sich deshalb ein Werkzeug; welches Einzelwerkzeug arbeitet, entscheidet die Eingabe.

/** Dasselbe Werkzeug mit anderem Namen, Text, Schema oder anderer Ausführung. */
fun Werkzeug.als(
    name: String = this.name, titel: String = this.titel, beschreibung: String = this.beschreibung, schema: JSONObject = this.schema,
    nurLesen: Boolean = this.nurLesen, loeschend: Boolean = this.loeschend, ausfuehren: (suspend (JSONObject) -> Ergebnis)? = null,
): Werkzeug = Werkzeug(name, titel, beschreibung, schema, nurLesen, loeschend, meta, ausfuehren ?: this.ausfuehren)

/** Kopie eines Schemas mit zusätzlichen Feldern; [pflicht] ersetzt die Pflichtfelder, wenn angegeben. */
fun JSONObject.mit(vararg felder: Pair<String, JSONObject>, pflicht: List<String>? = null): JSONObject = JSONObject(toString()).also { kopie ->
    felder.forEach { kopie.getJSONObject("properties").put(it.first, it.second) }
    pflicht?.let { kopie.put("required", JSONArray(it)) }
}

/** Hat die Eingabe dieses Feld mit einem echten Wert? */
fun JSONObject.gesetzt(name: String): Boolean = has(name) && !isNull(name) && optString(name).isNotBlank()

// ---- Kleine Helfer für JSON-Schemas ----

fun schema(vararg felder: Pair<String, JSONObject>, pflicht: List<String> = emptyList()): JSONObject = JSONObject()
    .put("type", "object")
    .put("properties", JSONObject().apply { felder.forEach { put(it.first, it.second) } })
    .put("required", JSONArray(pflicht))
    .put("additionalProperties", false)

fun text(beschreibung: String, werte: List<String>? = null): JSONObject = JSONObject().put("type", "string").put("description", beschreibung)
    .apply { werte?.let { put("enum", JSONArray(it)) } }

fun zahl(beschreibung: String): JSONObject = JSONObject().put("type", "integer").put("description", beschreibung)
fun schalter(beschreibung: String): JSONObject = JSONObject().put("type", "boolean").put("description", beschreibung)
fun textListe(beschreibung: String): JSONObject = JSONObject().put("type", "array").put("description", beschreibung).put("items", JSONObject().put("type", "string"))

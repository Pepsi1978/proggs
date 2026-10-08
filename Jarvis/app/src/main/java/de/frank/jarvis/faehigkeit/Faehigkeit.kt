package de.frank.jarvis.faehigkeit

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Ergebnis(val text: String, val fehler: Boolean = false)

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
}

object Register {
    @Volatile private var liste: List<Faehigkeit>? = null

    fun alle(context: Context): List<Faehigkeit> = liste ?: synchronized(this) {
        liste ?: listOf(AufgabenFaehigkeit(context.applicationContext), BiomarkerFaehigkeit(context.applicationContext), KalenderFaehigkeit(context.applicationContext)).also { liste = it }
    }

    fun werkzeuge(context: Context): List<Werkzeug> = alle(context).flatMap { it.werkzeuge }
}

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

package de.frank.jarvis.data

import android.content.Context
import android.util.Log
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Ein gespeichertes Gespräch, wie es die Seitenleiste zeigt. */
data class Sitzung(val id: String, val titel: String, val erstellt: Long, val geaendert: Long, val anzahl: Int)

/** Eine Nachricht im Speicher: von Frank oder von Jarvis. */
data class GespeicherteNachricht(val vonMir: Boolean, val text: String)

/**
 * Jedes Gespräch mit Jarvis als eigene Datei unter `files/gespraeche/<id>.json`, damit Frank alte Gespräche in der
 * Seitenleiste wiederfindet und weiterführen kann. Klein und lesbar: Titel, Zeitpunkte und die Nachrichten.
 */
object Gespraeche {
    private const val TAG = "Gespraeche"

    private fun ordner(context: Context) = File(context.applicationContext.filesDir, "gespraeche").apply { mkdirs() }
    private fun datei(context: Context, id: String) = File(ordner(context), "$id.json")

    fun neueId(): String = System.currentTimeMillis().toString(36) + (1000..9999).random()

    /** Alle Gespräche, das zuletzt geführte zuerst. */
    fun alle(context: Context): List<Sitzung> =
        ordner(context).listFiles { f -> f.name.endsWith(".json") }.orEmpty().mapNotNull { f ->
            runCatching {
                val j = JSONObject(f.readText())
                Sitzung(j.getString("id"), j.optString("titel"), j.optLong("erstellt"), j.optLong("geaendert"), j.optJSONArray("nachrichten")?.length() ?: 0)
            }.onFailure { Log.w(TAG, "Gespräch ${f.name} nicht lesbar", it) }.getOrNull()
        }.sortedByDescending { it.geaendert }

    fun laden(context: Context, id: String): Pair<Sitzung, List<GespeicherteNachricht>>? = runCatching {
        val j = JSONObject(datei(context, id).readText())
        val a = j.optJSONArray("nachrichten") ?: JSONArray()
        val nachrichten = (0 until a.length()).map { a.getJSONObject(it).let { n -> GespeicherteNachricht(n.optBoolean("mir"), n.optString("text")) } }
        Sitzung(id, j.optString("titel"), j.optLong("erstellt"), j.optLong("geaendert"), nachrichten.size) to nachrichten
    }.getOrNull()

    /** Speichert das ganze Gespräch. Ein von Hand vergebener Titel bleibt erhalten. */
    fun speichern(context: Context, id: String, erstellt: Long, nachrichten: List<GespeicherteNachricht>) {
        if (nachrichten.isEmpty()) return
        runCatching {
            val d = datei(context, id)
            val alt = if (d.exists()) runCatching { JSONObject(d.readText()) }.getOrNull() else null
            val eigenerTitel = alt?.optBoolean("eigenerTitel") == true
            val titel = if (eigenerTitel) alt!!.optString("titel") else titelAus(nachrichten)
            val json = JSONObject()
                .put("id", id)
                .put("titel", titel)
                .put("eigenerTitel", eigenerTitel)
                .put("erstellt", erstellt)
                .put("geaendert", System.currentTimeMillis())
                .put("nachrichten", JSONArray().apply { nachrichten.forEach { put(JSONObject().put("mir", it.vonMir).put("text", it.text)) } })
            schreiben(d, json)
        }.onFailure { Log.w(TAG, "Gespräch nicht gespeichert", it) }
    }

    fun umbenennen(context: Context, id: String, titel: String) {
        runCatching {
            val d = datei(context, id)
            val j = JSONObject(d.readText()).put("titel", titel.trim()).put("eigenerTitel", true)
            schreiben(d, j)
        }.onFailure { Log.w(TAG, "Gespräch nicht umbenannt", it) }
    }

    fun loeschen(context: Context, id: String) { datei(context, id).delete() }

    private fun schreiben(d: File, json: JSONObject) {
        val tmp = File(d.parentFile, d.name + ".tmp")
        tmp.writeText(json.toString())
        if (!tmp.renameTo(d)) { d.delete(); tmp.renameTo(d) }
    }

    /** Titel aus Franks erster Frage: eine Zeile, höchstens 60 Zeichen. */
    private fun titelAus(nachrichten: List<GespeicherteNachricht>): String {
        val erste = nachrichten.firstOrNull { it.vonMir }?.text ?: nachrichten.first().text
        val zeile = erste.replace(Regex("\\s+"), " ").trim()
        return if (zeile.length <= 60) zeile else zeile.take(57).substringBeforeLast(' ', zeile.take(57)) + " …"
    }
}

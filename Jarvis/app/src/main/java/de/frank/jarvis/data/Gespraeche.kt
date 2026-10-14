package de.frank.jarvis.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Ein gespeichertes Gespräch, wie es die Seitenleiste zeigt. */
data class Sitzung(val id: String, val titel: String, val erstellt: Long, val geaendert: Long, val anzahl: Int)

/**
 * Eine Nachricht im Speicher: von Frank oder von Jarvis. [art]: „antwort“ (normal), „fehler“ (Fehlermeldung statt
 * Antwort) oder „abgebrochen“ (Franks Frage blieb unbeantwortet). [kontext] sieht nur das Modell im Verlauf,
 * etwa was in einer abgebrochenen Runde schon erledigt war.
 */
data class GespeicherteNachricht(val vonMir: Boolean, val text: String, val art: String = "antwort", val kontext: String = "")

/**
 * Jedes Gespräch mit Jarvis als eigene Datei unter `files/gespraeche/<id>.json`, damit Frank alte Gespräche in der
 * Seitenleiste wiederfindet und weiterführen kann. Klein und lesbar: Titel, Zeitpunkte und die Nachrichten.
 */
object Gespraeche {
    private const val TAG = "Gespraeche"

    /** Speichern, Umbenennen und Löschen nacheinander: Zwei Sicherungen desselben Gesprächs dürfen sich nie überholen. */
    private val sperre = Any()

    private fun ordner(context: Context) = File(context.applicationContext.filesDir, "gespraeche").apply { mkdirs() }
    private fun datei(context: Context, id: String) = File(ordner(context), "$id.json")

    fun neueId(): String = System.currentTimeMillis().toString(36) + (1000..9999).random()

    /** Liest eine Gesprächsdatei; eine nach Absturz übrig gebliebene Sicherung (.bak) greift dabei von selbst. */
    private fun lies(d: File): JSONObject = JSONObject(String(AtomicFile(d).readFully(), Charsets.UTF_8))

    /** Alle Gespräche, das zuletzt geführte zuerst. */
    fun alle(context: Context): List<Sitzung> =
        ordner(context).listFiles { f -> f.name.endsWith(".json") }.orEmpty().mapNotNull { f ->
            runCatching {
                val j = lies(f)
                Sitzung(j.getString("id"), j.optString("titel"), j.optLong("erstellt"), j.optLong("geaendert"), j.optJSONArray("nachrichten")?.length() ?: 0)
            }.onFailure { Log.w(TAG, "Gespräch ${f.name} nicht lesbar", it) }.getOrNull()
        }.sortedByDescending { it.geaendert }

    fun laden(context: Context, id: String): Pair<Sitzung, List<GespeicherteNachricht>>? = runCatching {
        val j = lies(datei(context, id))
        val a = j.optJSONArray("nachrichten") ?: JSONArray()
        val nachrichten = (0 until a.length()).map {
            a.getJSONObject(it).let { n -> GespeicherteNachricht(n.optBoolean("mir"), n.optString("text"), n.optString("art", "antwort"), n.optString("kontext")) }
        }
        Sitzung(id, j.optString("titel"), j.optLong("erstellt"), j.optLong("geaendert"), nachrichten.size) to nachrichten
    }.getOrNull()

    /**
     * Speichert das ganze Gespräch. Ein von Hand vergebener Titel bleibt erhalten. Liefert den neuen Eintrag für die
     * Seitenleiste (null = nichts geschrieben). Ein älterer Stand mit weniger Nachrichten überschreibt nie einen neueren.
     */
    fun speichern(context: Context, id: String, erstellt: Long, nachrichten: List<GespeicherteNachricht>): Sitzung? {
        if (nachrichten.isEmpty()) return null
        return synchronized(sperre) {
            runCatching {
                val d = datei(context, id)
                val alt = if (d.exists() || File(d.path + ".bak").exists()) runCatching { lies(d) }.getOrNull() else null
                val bisher = alt?.optJSONArray("nachrichten")?.length() ?: 0
                if (nachrichten.size < bisher) {
                    Log.w(TAG, "Älterer Stand von $id verworfen ($bisher > ${nachrichten.size})")
                    return@runCatching null
                }
                val eigenerTitel = alt?.optBoolean("eigenerTitel") == true
                val titel = if (eigenerTitel) alt!!.optString("titel") else titelAus(nachrichten)
                val jetzt = System.currentTimeMillis()
                val json = JSONObject()
                    .put("id", id)
                    .put("titel", titel)
                    .put("eigenerTitel", eigenerTitel)
                    .put("erstellt", erstellt)
                    .put("geaendert", jetzt)
                    .put("nachrichten", JSONArray().apply {
                        nachrichten.forEach { n ->
                            put(JSONObject().put("mir", n.vonMir).put("text", n.text).apply {
                                // Nur Abweichungen speichern: Alte Dateien bleiben gültig, neue bleiben klein.
                                if (n.art != "antwort") put("art", n.art)
                                if (n.kontext.isNotEmpty()) put("kontext", n.kontext)
                            })
                        }
                    })
                schreiben(d, json)
                Sitzung(id, titel, erstellt, jetzt, nachrichten.size)
            }.onFailure { Log.w(TAG, "Gespräch nicht gespeichert", it) }.getOrNull()
        }
    }

    fun umbenennen(context: Context, id: String, titel: String) {
        synchronized(sperre) {
            runCatching {
                val d = datei(context, id)
                val j = lies(d).put("titel", titel.trim()).put("eigenerTitel", true)
                schreiben(d, j)
            }.onFailure { Log.w(TAG, "Gespräch nicht umbenannt", it) }
        }
    }

    fun loeschen(context: Context, id: String) {
        synchronized(sperre) { AtomicFile(datei(context, id)).delete() }
    }

    /** Atomar mit fsync: Nach Absturz oder leerem Akku bleibt der alte oder der neue Stand, nie eine leere Datei. */
    private fun schreiben(d: File, json: JSONObject) {
        val af = AtomicFile(d)
        val aus = af.startWrite()
        try {
            aus.write(json.toString().toByteArray(Charsets.UTF_8))
            af.finishWrite(aus)
        } catch (e: Exception) {
            af.failWrite(aus)
            throw e
        }
    }

    /** Titel aus Franks erster Frage: eine Zeile, höchstens 60 Zeichen. */
    private fun titelAus(nachrichten: List<GespeicherteNachricht>): String {
        val erste = nachrichten.firstOrNull { it.vonMir }?.text ?: nachrichten.first().text
        val zeile = erste.replace(Regex("\\s+"), " ").trim()
        return if (zeile.length <= 60) zeile else zeile.take(57).substringBeforeLast(' ', zeile.take(57)) + " …"
    }
}

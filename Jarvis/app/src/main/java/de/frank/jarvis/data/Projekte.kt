package de.frank.jarvis.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Ein Projekt in der Seitenleiste: ein Name und seine eigenen Gespräche (diese tragen die [id] im Feld „projekt“). */
data class Projekt(val id: String, val name: String, val erstellt: Long)

/** Alle Projekte in einer Datei `files/projekte.json`, atomar geschrieben wie die Gespräche. */
object Projekte {
    private const val TAG = "Projekte"
    private val sperre = Any()

    private fun datei(context: Context) = File(context.applicationContext.filesDir, "projekte.json")

    /** Alle Projekte, das neueste zuerst. */
    fun alle(context: Context): List<Projekt> = synchronized(sperre) { runCatching { lies(context) }.getOrDefault(emptyList()) }.sortedByDescending { it.erstellt }

    /**
     * Legt ein Projekt an und liefert es; null = nicht gespeichert. Eine unlesbare Datei wird vorher als
     * `projekte.kaputt.json` beiseitegelegt, statt das Anlegen dauerhaft zu blockieren.
     */
    fun anlegen(context: Context, name: String): Projekt? = synchronized(sperre) {
        runCatching {
            val p = Projekt(Gespraeche.neueId(), name.replace(Regex("\\s+"), " ").trim(), System.currentTimeMillis())
            val bisher = runCatching { lies(context) }.getOrElse {
                runCatching { datei(context).copyTo(File(datei(context).parentFile, "projekte.kaputt.json"), overwrite = true) }
                emptyList()
            }
            val a = JSONArray().apply { (bisher + p).forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("erstellt", it.erstellt)) } }
            Gespraeche.schreiben(datei(context), JSONObject().put("projekte", a))
            p
        }.onFailure { Log.w(TAG, "Projekt nicht angelegt", it) }.getOrNull()
    }

    private fun lies(context: Context): List<Projekt> {
        val d = datei(context)
        if (!d.exists() && !File(d.path + ".bak").exists()) return emptyList()
        return runCatching {
            val a = JSONObject(String(AtomicFile(d).readFully(), Charsets.UTF_8)).optJSONArray("projekte") ?: JSONArray()
            (0 until a.length()).map { i -> a.getJSONObject(i).let { Projekt(it.getString("id"), it.optString("name"), it.optLong("erstellt")) } }
        }.onFailure { Log.w(TAG, "Projekte nicht lesbar", it) }.getOrThrow()
    }
}

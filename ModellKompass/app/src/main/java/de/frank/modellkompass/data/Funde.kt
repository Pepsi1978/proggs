package de.frank.modellkompass.data

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Ein empfohlenes Modell. [hfRepo] ist die genaue Kennung auf Hugging Face (herausgeber/name). */
data class Modellfund(
    val name: String,
    val herausgeber: String,
    val hfRepo: String,
    val datei: String,
    val parameter: String,
    val vramGb: Double?,
    val laeuftIn: String,
    val lmStudio: String,
    val staerken: String,
    val hinweis: String,
    val quelle: String,
    /** true = Repo auf Hugging Face gefunden, false = dort nicht vorhanden, null = nicht prüfbar. */
    val geprueft: Boolean? = null,
)

/** Das Rechercheergebnis eines Bereichs. [rohtext] steht nur da, wenn sich die Antwort nicht auswerten ließ. */
data class Ergebnis(
    val bereich: String,
    val stand: Long,
    val modell: String,
    val zusammenfassung: String,
    val modelle: List<Modellfund>,
    val rohtext: String? = null,
)

/** Legt jedes Ergebnis als eigene JSON-Datei ab. */
object Speicher {
    private fun ordner(context: Context) = File(context.filesDir, "funde").apply { mkdirs() }

    fun lade(context: Context): Map<String, Ergebnis> = Bereich.entries.mapNotNull { b ->
        runCatching { lies(JSONObject(File(ordner(context), "${b.id}.json").readText())) }.getOrNull()
    }.associateBy { it.bereich }

    fun sichere(context: Context, e: Ergebnis) {
        val ziel = File(ordner(context), "${e.bereich}.json")
        val neu = File(ordner(context), "${e.bereich}.json.neu")
        neu.writeText(schreibe(e).toString())
        if (!neu.renameTo(ziel)) { ziel.delete(); neu.renameTo(ziel) }
    }

    private fun schreibe(e: Ergebnis) = JSONObject()
        .put("bereich", e.bereich).put("stand", e.stand).put("modell", e.modell)
        .put("zusammenfassung", e.zusammenfassung).put("rohtext", e.rohtext ?: JSONObject.NULL)
        .put("modelle", JSONArray().apply {
            e.modelle.forEach { m ->
                put(
                    JSONObject().put("name", m.name).put("herausgeber", m.herausgeber).put("hfRepo", m.hfRepo)
                        .put("datei", m.datei).put("parameter", m.parameter).put("vramGb", m.vramGb ?: JSONObject.NULL)
                        .put("laeuftIn", m.laeuftIn).put("lmStudio", m.lmStudio).put("staerken", m.staerken)
                        .put("hinweis", m.hinweis).put("quelle", m.quelle).put("geprueft", m.geprueft ?: JSONObject.NULL),
                )
            }
        })

    private fun lies(j: JSONObject): Ergebnis {
        val liste = j.optJSONArray("modelle") ?: JSONArray()
        return Ergebnis(
            bereich = j.getString("bereich"),
            stand = j.optLong("stand"),
            modell = j.optString("modell"),
            zusammenfassung = j.optString("zusammenfassung"),
            rohtext = if (j.isNull("rohtext")) null else j.optString("rohtext"),
            modelle = (0 until liste.length()).mapNotNull { liste.optJSONObject(it) }.map { m ->
                Modellfund(
                    m.optString("name"), m.optString("herausgeber"), m.optString("hfRepo"), m.optString("datei"),
                    m.optString("parameter"), if (m.isNull("vramGb")) null else m.optDouble("vramGb"),
                    m.optString("laeuftIn"), m.optString("lmStudio"), m.optString("staerken"), m.optString("hinweis"),
                    m.optString("quelle"), if (m.isNull("geprueft")) null else m.optBoolean("geprueft"),
                )
            },
        )
    }
}

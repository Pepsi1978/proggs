package de.frank.longevity.ki

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Zwischenstand des großen Aktualisierungslaufs: Jeder fertige Schritt (Neuheiten, Bewertungs-Block,
 * neu geschriebener Text, neuer Faktor) wird sofort gesichert. Bricht der Lauf ab, setzt der
 * nächste Lauf dort fort, statt alles neu zu erarbeiten. Der Stand gilt nur für dieselbe Liste (gleiche ids);
 * nach einem erfolgreichen Lauf wird er gelöscht.
 */
class LaufSpeicher(private val datei: File) {

    class Stand(
        val tiefe: String,
        val ids: List<Long>,
        val beginn: Long,
        val schritte: MutableMap<String, String>,
    )

    private var stand: Stand? = null

    @Synchronized
    fun laden(): Stand? {
        stand?.let { return it }
        if (!datei.exists()) return null
        return runCatching {
            val o = JSONObject(datei.readText())
            val ids = o.getJSONArray("ids").let { a -> List(a.length()) { a.getLong(it) } }
            val s = o.getJSONObject("schritte")
            val schritte = linkedMapOf<String, String>()
            s.keys().forEach { k -> schritte[k] = s.getString(k) }
            Stand(o.optString("tiefe"), ids, o.optLong("beginn"), schritte)
        }.onFailure { KiLog.fehler("Zwischenstand unlesbar – wird verworfen", it) }.getOrNull().also { stand = it }
    }

    /** Beginnt einen Lauf: passt der gesicherte Stand (gleiche Liste, gleiche Tiefe), wird er fortgesetzt, sonst neu angelegt. */
    @Synchronized
    fun beginnen(tiefe: String, ids: List<Long>): Stand {
        val alt = laden()
        val sortiert = ids.sorted()
        // Die Stufe darf wechseln: Blöcke anderer Größe haben andere Schlüssel und werden einfach neu bewertet.
        if (alt != null && alt.ids == sortiert) {
            KiLog.info("Zwischenstand wird fortgesetzt (Tiefe ${alt.tiefe}→$tiefe): ${alt.schritte.size} Schritte gesichert (${alt.schritte.keys.joinToString()})")
            return alt
        }
        if (alt != null) KiLog.info("Zwischenstand passt nicht zur Liste (${alt.ids.size}→${sortiert.size} Faktoren) – neuer Lauf")
        return Stand(tiefe, sortiert, System.currentTimeMillis(), linkedMapOf()).also { stand = it; sichern() }
    }

    @Synchronized
    fun hole(schluessel: String): String? = stand?.schritte?.get(schluessel)

    @Synchronized
    fun lege(schluessel: String, text: String) {
        val s = stand ?: return
        s.schritte[schluessel] = text
        sichern()
        KiLog.info("Zwischenstand gesichert: $schluessel (${text.length} Zeichen, insgesamt ${s.schritte.size} Schritte)")
    }

    @Synchronized
    fun entferne(schluessel: String) {
        if (stand?.schritte?.remove(schluessel) != null) sichern()
    }

    @Synchronized
    fun loeschen() {
        stand = null
        datei.delete()
        KiLog.info("Zwischenstand gelöscht")
    }

    /** Kurzbeschreibung für den Dialog, null wenn nichts gesichert ist oder der Stand nicht zur Liste [ids] passt. */
    @Synchronized
    fun beschreibung(ids: List<Long>): String? {
        val s = laden() ?: return null
        if (s.schritte.isEmpty() || s.ids != ids.sorted()) return null
        val zeit = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY).format(Date(s.beginn))
        return "Ein abgebrochener Lauf vom $zeit ist gesichert (${s.schritte.size} fertige Schritte). Er wird dort fortgesetzt, wo er stehen geblieben ist."
    }

    private fun sichern() {
        val s = stand ?: return
        val o = JSONObject()
            .put("tiefe", s.tiefe)
            .put("ids", JSONArray(s.ids))
            .put("beginn", s.beginn)
            .put("schritte", JSONObject(s.schritte as Map<*, *>))
        val text = o.toString()
        runCatching {
            val tmp = File(datei.parentFile, datei.name + ".tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(datei)) { datei.delete(); tmp.renameTo(datei) }
        }.onFailure { KiLog.fehler("Zwischenstand konnte nicht gesichert werden", it) }
        KiLog.kopie("lauf-stand.json", text)
    }
}

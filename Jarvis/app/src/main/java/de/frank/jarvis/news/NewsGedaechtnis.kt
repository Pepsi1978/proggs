package de.frank.jarvis.news

import android.content.Context
import de.frank.jarvis.abgleich.GedaechtnisAbgleich
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

/**
 * Merkt sich je Kalendertag, welche News-Kompass-Meldungen Frank tatsächlich genannt oder vorgelesen wurden.
 *
 * - Je Gerät und Tag eine Datei `news_genannt/<Datum>_<Gerät>.json`: Jedes Gerät schreibt nur seine eigene, gelesen wird die
 *   Vereinigung aller. Der Gedächtnis-Abgleich bringt sie aufs andere Gerät (je Datei gewinnt die jüngste Fassung, und
 *   weil jedes Gerät nur seine eigene schreibt, geht nichts verloren). Ältere als 7 Tage werden gelöscht.
 * - „Genannt“ heißt: Die fertige Antwort im eigenen Chat oder Sprachgespräch nennt die Meldung ([antwortGegeben]), Frank hat
 *   eine Meldung gezielt nachgefragt, oder ChatGPT meldet sie über `news_lesen` mit `mitgeteilt`. Ein Abruf allein zählt nicht.
 */
object NewsGedaechtnis {
    const val ORDNER = "news_genannt"
    private const val HALTEN_TAGE = 7L

    /** Zuletzt im Gespräch angebotene Meldungen (Abruf) mit Zeitpunkt; erst die Antwort entscheidet, was davon genannt wurde. */
    private val angeboten = ArrayList<Pair<Long, NewsMeldung>>()

    private fun ordner(context: Context) = File(context.filesDir, ORDNER).apply { mkdirs() }
    private fun eigeneDatei(context: Context, tag: LocalDate) =
        File(ordner(context), "${tag}_" + Geraet.ablageName.replace(Regex("[^A-Za-z0-9]"), "_") + ".json")

    @Synchronized
    fun genannt(context: Context, tag: LocalDate = LocalDate.now()): List<Genannt> =
        (ordner(context).listFiles() ?: emptyArray()).filter { it.name.startsWith("${tag}_") && it.name.endsWith(".json") }.flatMap { datei ->
            runCatching {
                val arr = JSONObject(datei.readText()).optJSONArray("eintraege") ?: JSONArray()
                (0 until arr.length()).map { arr.getJSONObject(it) }.map { Genannt(it.optString("titel"), it.optString("quelle"), it.optLong("um")) }
            }.getOrElse { emptyList() }
        }

    @Synchronized
    fun merke(context: Context, meldungen: Collection<NewsMeldung>) {
        if (meldungen.isEmpty()) return
        val tag = LocalDate.now()
        val vorhanden = genannt(context, tag)
        val neu = meldungen.filter { m -> vorhanden.none { NewsRegeln.gleich(m, it) } }
        if (neu.isEmpty()) return
        val datei = eigeneDatei(context, tag)
        val alt = runCatching { JSONObject(datei.readText()).optJSONArray("eintraege") }.getOrNull() ?: JSONArray()
        val jetzt = System.currentTimeMillis()
        neu.forEach { alt.put(JSONObject().put("titel", it.titel).put("quelle", it.quelle).put("rubrik", it.rubrik).put("um", jetzt)) }
        val teil = File(datei.path + ".teil")
        teil.writeText(JSONObject().put("tag", tag.toString()).put("geraet", Geraet.ablageName).put("eintraege", alt).toString())
        if (!teil.renameTo(datei)) { datei.delete(); teil.renameTo(datei) }
        raeumeAuf(context, tag)
        Logbuch.aktion("News-Kompass", "Als genannt gemerkt: " + neu.joinToString(" | ") { it.titel.take(60) })
        GedaechtnisAbgleich.anstossen()
    }

    private fun raeumeAuf(context: Context, heute: LocalDate) {
        (ordner(context).listFiles() ?: emptyArray()).forEach { f ->
            val tag = runCatching { LocalDate.parse(f.name.substringBefore('_')) }.getOrNull() ?: return@forEach
            if (tag.isBefore(heute.minusDays(HALTEN_TAGE))) f.delete()
        }
    }

    /** Ein Abruf im Gespräch hat diese Meldungen geliefert (noch nicht genannt). */
    @Synchronized
    fun angeboten(meldungen: Collection<NewsMeldung>) {
        val jetzt = System.currentTimeMillis()
        angeboten.removeAll { jetzt - it.first > 3_600_000L }
        meldungen.forEach { m -> angeboten.removeAll { it.second.id == m.id }; angeboten += jetzt to m }
    }

    /**
     * Die fertige Antwort an Frank (eigener Chat oder Sprachgespräch): Was davon eine seit [seit] angebotene Meldung nennt, gilt als
     * genannt. Läuft nach jeder Antwort, kostet ohne vorherigen News-Abruf nichts.
     */
    fun antwortGegeben(context: Context, antwort: String, seit: Long) {
        val kandidaten = synchronized(this) { angeboten.filter { it.first >= seit }.map { it.second } }
        if (kandidaten.isEmpty() || antwort.isBlank()) return
        val genannt = kandidaten.filter { NewsRegeln.genanntIn(antwort, it) }
        runCatching { merke(context, genannt) }.onFailure { Logbuch.fehler("News-Kompass genannt", it) }
        synchronized(this) { angeboten.removeAll { a -> genannt.any { it.id == a.second.id } } }
    }
}

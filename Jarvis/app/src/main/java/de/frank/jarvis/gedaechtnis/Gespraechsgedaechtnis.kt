package de.frank.jarvis.gedaechtnis

import android.content.Context
import de.frank.jarvis.data.Gespraeche
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.faehigkeit.Regeln
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** Etwas, das Jarvis aus einem Gespräch mitnimmt, ohne dass es eine dauerhafte Tatsache ist: ein Vorhaben, eine Zusage, ein offenes Thema. */
data class Gespraechsnotiz(val id: Int, val text: String, val erstellt: Long, val bis: LocalDate?)

/**
 * Das Gedächtnis aus Franks Interaktionen mit Jarvis.
 *
 * Kurzzeit ([kurzzeit]): die Gespräche der letzten 10 Tage (eigener Chat und „Hey Jarvis“; die letzten zwei Tage mit
 * dem Wortlaut der letzten Wechsel, davor nur Franks Fragen), die Aktionen über ChatGPT und Jarvis (was angelegt,
 * geändert, gelöscht wurde) und die Gesprächsnotizen.
 *
 * Lernen ([Lernlauf]): Ist ein Gespräch zehn Minuten ruhig, liest Jarvis die neuen Wechsel und übernimmt daraus
 * dauerhafte Ziele und Tatsachen in die Merkdateien, ausdrücklich verlangte Regeln und kurzlebige Gesprächsnotizen
 * (Vorhaben, Zusagen, offene Themen, mit Ablaufdatum). Grundlage sind nur Franks eigene Aussagen. Derselbe Lauf prüft
 * das früher Gelernte auf Aktualität und räumt Erledigtes auf; hier liegen dafür die Bausteine.
 */
object Gespraechsgedaechtnis {
    private const val TAGE = 10L
    private const val NOTIZEN_HOECHSTENS = 40
    /** Ohne eigenes Ablaufdatum gilt eine Notiz so lange. */
    private const val NOTIZ_TAGE = 14L
    private const val RUHE_MS = 10 * 60_000L
    private const val HOECHSTENS = 9_000

    private fun notizDatei(context: Context) = File(context.applicationContext.filesDir, "gespraechsnotizen.json")
    private fun standDatei(context: Context) = File(context.applicationContext.filesDir, "gespraeche_gelernt.json")

    // ---------------------------------------------------------------- Notizen

    @Synchronized
    fun alleNotizen(context: Context): List<Gespraechsnotiz> {
        val d = notizDatei(context)
        if (!d.isFile) return emptyList()
        return runCatching {
            val a = JSONArray(d.readText())
            (0 until a.length()).map { i -> a.getJSONObject(i).let { n -> Gespraechsnotiz(n.getInt("id"), n.getString("text"), n.optLong("erstellt"), n.optString("bis").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }) } }
        }.getOrElse {
            // Unlesbar: einmal beiseitelegen, bevor das nächste Merken die Datei mit nur einer Notiz überschreibt.
            beiseitelegen(d)
            emptyList()
        }
    }

    /** Je Datei höchstens eine `.defekt-<Zeit>`-Kopie pro Prozess, sonst entstünde bei jedem Lesen eine neue. */
    private val beiseitegelegt = HashSet<String>()

    private fun beiseitelegen(d: File) {
        if (d.length() == 0L || !synchronized(beiseitegelegt) { beiseitegelegt.add(d.name) }) return
        runCatching { d.copyTo(File(d.path + ".defekt-" + System.currentTimeMillis()), overwrite = true) }
        Logbuch.fehler("Gesprächsgedächtnis: ${d.name} unlesbar, beiseitegelegt")
    }

    /** Die gültigen Notizen: nicht abgelaufen. */
    fun notizen(context: Context): List<Gespraechsnotiz> {
        val heute = LocalDate.now()
        return alleNotizen(context).filter { n -> !(n.bis ?: datum(n.erstellt).plusDays(NOTIZ_TAGE)).isBefore(heute) }
    }

    @Synchronized
    private fun schreibeNotizen(context: Context, liste: List<Gespraechsnotiz>) {
        val inhalt = JSONArray(liste.map { JSONObject().put("id", it.id).put("text", it.text).put("erstellt", it.erstellt).put("bis", it.bis?.toString() ?: "") }).toString()
        // Atomar (Zwischendatei, fsync, umbenennen): Ein Abbruch hinterlässt nie eine halbe Notizdatei.
        de.frank.jarvis.ablage.AblageSpeicher.schreibeAtomar(notizDatei(context), inhalt.toByteArray())
    }

    @Synchronized
    fun merkeNotiz(context: Context, text: String, bis: LocalDate?): Gespraechsnotiz? {
        val sauber = text.replace(Regex("\\s+"), " ").trim().take(300)
        if (sauber.length < 8) return null
        // Abgelaufene fallen beim Schreiben weg; bleibt es zu voll, geht die älteste.
        val gueltig = notizen(context)
        // Wiederholte Läufe dürfen keine Dubletten anlegen: auch fast gleiche Fassungen desselben Vorhabens nicht.
        if (gueltig.any { it.text.equals(sauber, ignoreCase = true) || de.frank.jarvis.faehigkeit.Merkdatei.aehnlichkeit(it.text, sauber) >= 0.75 }) return null
        val notiz = Gespraechsnotiz((alleNotizen(context).maxOfOrNull { it.id } ?: 0) + 1, sauber, System.currentTimeMillis(), bis)
        schreibeNotizen(context, (gueltig + notiz).takeLast(NOTIZEN_HOECHSTENS))
        return notiz
    }

    /** Holt eine vom Lernlauf entfernte Notiz mit ihrer alten id zurück (Wiederherstellen im Lernbericht). */
    @Synchronized
    fun stelleNotizWiederHer(context: Context, notiz: Gespraechsnotiz): Boolean {
        val alle = alleNotizen(context)
        if (alle.any { it.id == notiz.id }) return true
        schreibeNotizen(context, (alle + notiz).sortedBy { it.id }.takeLast(NOTIZEN_HOECHSTENS))
        return true
    }

    /** Abgelaufene Notizen (Ablaufdatum vorbei) endgültig aus der Datei nehmen; liefert die entfernten. */
    @Synchronized
    fun entferneAbgelaufene(context: Context): List<Gespraechsnotiz> {
        val alle = alleNotizen(context)
        val gueltig = notizen(context)
        val weg = alle.filter { a -> gueltig.none { it.id == a.id } }
        if (weg.isNotEmpty()) schreibeNotizen(context, gueltig)
        return weg
    }

    /** Bis wann eine Notiz gilt (eigenes Datum oder 14 Tage nach dem Anlegen). */
    fun gueltigBis(n: Gespraechsnotiz): LocalDate = n.bis ?: datum(n.erstellt).plusDays(NOTIZ_TAGE)

    @Synchronized
    fun loescheNotiz(context: Context, id: Int): Boolean {
        val alle = alleNotizen(context)
        if (alle.none { it.id == id }) return false
        schreibeNotizen(context, alle.filter { it.id != id })
        return true
    }

    // ---------------------------------------------------------------- Kurzzeitgedächtnis

    /** Gespräche, die in den letzten [tage] Tagen geändert wurden, neueste zuerst. Nur diese Dateien werden überhaupt gelesen. */
    private fun neuereGespraeche(context: Context, tage: Long): List<Pair<de.frank.jarvis.data.Sitzung, List<de.frank.jarvis.data.GespeicherteNachricht>>> {
        val grenze = System.currentTimeMillis() - tage * 24 * 3_600_000L
        return File(context.applicationContext.filesDir, "gespraeche").listFiles { f -> f.name.endsWith(".json") && f.lastModified() >= grenze }.orEmpty()
            .sortedByDescending { it.lastModified() }
            .mapNotNull { Gespraeche.laden(context, it.name.removeSuffix(".json")) }
            // Fehlermeldungen sind keine Antworten von Jarvis; eine unbeantwortete Frage wird als solche markiert.
            .map { (s, n) -> s to n.filter { it.art != "fehler" }.map { if (it.art == "abgebrochen") it.copy(text = it.text + " (unbeantwortet)") else it } }
            .filter { it.first.geaendert >= grenze }
            .sortedByDescending { it.first.geaendert }
    }

    /** Die Titel der schreibenden Werkzeuge: Nur sie gelten als Aktion (Lesen ist keine Neuigkeit). */
    private fun schreibTitel(context: Context): Set<String> = Register.werkzeuge(context).filter { !it.nurLesen }.map { it.titel }.toSet() +
        setOf("Idee speichern oder ändern", "Agent beauftragt")

    fun kurzzeit(context: Context): String {
        val app = context.applicationContext
        Protokoll.lade(app)
        val jetzt = System.currentTimeMillis()
        val teile = mutableListOf<String>()

        val notizen = notizen(app)
        if (notizen.isNotEmpty()) teile += "Aus Gesprächen gemerkt (offene Vorhaben, Zusagen, Themen; gilt bis zum genannten Tag):\n" +
            notizen.joinToString("\n") { "- [Notiz ${it.id}] " + it.text + (it.bis?.let { b -> " (bis ${kurz(b)})" } ?: "") + " – notiert ${kurz(datum(it.erstellt))}" }

        val gespraeche = neuereGespraeche(app, TAGE)
        if (gespraeche.isNotEmpty()) teile += buildString {
            append("Gespräche mit Jarvis in den letzten 10 Tagen (neueste zuerst):\n")
            gespraeche.take(14).forEach { (s, nachrichten) ->
                append("- „").append(s.titel).append("“, ").append(zeit(s.geaendert)).append(":\n")
                if (jetzt - s.geaendert < 48 * 3_600_000L) {
                    // Die letzten zwei Tage im Wortlaut der letzten Wechsel: Darauf bezieht sich Frank am ehesten („wie vorhin gesagt …“).
                    nachrichten.takeLast(8).forEach { n -> append("    ").append(if (n.vonMir) "Frank: " else "Jarvis: ").append(flach(n.text, if (n.vonMir) 400 else 300)).append('\n') }
                } else {
                    append("    Frank fragte: ").append(nachrichten.filter { it.vonMir }.takeLast(4).joinToString(" | ") { flach(it.text, 140) }).append('\n')
                }
            }
        }.trim()

        val titel = schreibTitel(app)
        val grenze = jetzt - TAGE * 24 * 3_600_000L
        val aktionen = Protokoll.eintraege.value.filter { it.zeit >= grenze && it.ok && it.quelle != Quelle.SYSTEM && it.werkzeug in titel }
        if (aktionen.isNotEmpty()) teile += "Was über ChatGPT und Jarvis zuletzt erledigt wurde (neueste zuerst):\n" +
            aktionen.take(20).joinToString("\n") { "- " + zeit(it.zeit) + " (" + it.quelle.anzeige + ") " + it.werkzeug + ": " + flach(it.text, 160) }
        val abrufe = Protokoll.eintraege.value.filter { it.zeit >= jetzt - 48 * 3_600_000L && it.quelle == Quelle.CHATGPT && it.werkzeug !in titel }
        if (abrufe.isNotEmpty()) teile += "Worüber Frank in den letzten 48 Stunden über ChatGPT nachgefragt hat (abgerufene Bereiche): " +
            abrufe.groupingBy { it.werkzeug }.eachCount().entries.sortedByDescending { it.value }.joinToString(", ") { "${it.key} (${it.value}×)" }

        if (teile.isEmpty()) return "In den letzten 10 Tagen gab es keine Gespräche oder Aktionen mit Jarvis."
        return teile.joinToString("\n\n").let { if (it.length > HOECHSTENS) it.take(HOECHSTENS).substringBeforeLast('\n') + "\n… (ältere Gespräche mit gedaechtnis_suchen)" else it }
    }

    // ---------------------------------------------------------------- Lernen aus Gesprächen (Bausteine für den Lernlauf)

    internal fun ladeStand(context: Context): JSONObject {
        val d = standDatei(context)
        if (!d.isFile) return JSONObject()
        return runCatching { JSONObject(d.readText()) }.getOrElse { beiseitelegen(d); JSONObject() }
    }
    // Atomar: Ein halb geschriebener Stand ließe Jarvis die Gespräche der letzten drei Tage noch einmal lernen.
    internal fun speichereStand(context: Context, stand: JSONObject) =
        runCatching { de.frank.jarvis.ablage.AblageSpeicher.schreibeAtomar(standDatei(context), stand.toString().toByteArray()) }

    /** Gespräche der letzten drei Tage mit neuen Worten von Frank: (Gespräch, bisher gelernte Anzahl, alle Nachrichten, ruht schon zehn Minuten). */
    private fun mitNeuem(context: Context, stand: JSONObject): List<Pair<Triple<de.frank.jarvis.data.Sitzung, Int, List<de.frank.jarvis.data.GespeicherteNachricht>>, Boolean>> {
        val jetzt = System.currentTimeMillis()
        return neuereGespraeche(context, 3).mapNotNull { (s, nachrichten) ->
            val gelernt = stand.optInt(s.id, 0).coerceAtMost(nachrichten.size)
            if (nachrichten.drop(gelernt).none { it.vonMir }) null else Triple(s, gelernt, nachrichten) to (jetzt - s.geaendert >= RUHE_MS)
        }
    }

    /** Neue Wechsel aus Gesprächen, die seit mindestens zehn Minuten ruhen: (Gespräch, bisher gelernte Anzahl, alle Nachrichten). */
    internal fun offen(context: Context, stand: JSONObject): List<Triple<de.frank.jarvis.data.Sitzung, Int, List<de.frank.jarvis.data.GespeicherteNachricht>>> =
        mitNeuem(context, stand).filter { it.second }.map { it.first }

    /** Titel der Gespräche mit neuen Worten von Frank, die noch keine zehn Minuten ruhen (sie liest ein späterer Lauf). */
    internal fun nochNichtRuhend(context: Context, stand: JSONObject): List<String> = mitNeuem(context, stand).filter { !it.second }.map { it.first.first.titel }

    /**
     * Gibt es Neues zu lernen? Billig: liest nur die Gespräche der letzten drei Tage. Neue Aktionen über ChatGPT allein
     * lösen keinen Lauf aus: Sie sind Werkzeugergebnisse, nicht Franks Worte, also keine Lernquelle.
     */
    fun neueGespraecheFaellig(context: Context): Boolean = runCatching { offen(context, ladeStand(context)).isNotEmpty() }.getOrDefault(false)

    /** true = gerade läuft ein Lernlauf (von Hand oder automatisch). */
    val lerntGerade: Boolean get() = Lernlauf.laeuft

    internal fun neueAktionen(context: Context, stand: JSONObject): List<de.frank.jarvis.data.Eintrag> {
        Protokoll.lade(context.applicationContext)
        val seit = stand.optLong("_chatgpt", System.currentTimeMillis() - 24 * 3_600_000L)
        val titel = schreibTitel(context)
        return Protokoll.eintraege.value.filter { it.zeit > seit && it.ok && it.quelle == Quelle.CHATGPT && it.werkzeug in titel }
    }

    /** Alte Einträge im Stand aufräumen: Gespräche, die es nicht mehr gibt, braucht er nicht. */
    internal fun standAufraeumen(app: Context, stand: JSONObject) {
        val vorhanden = File(app.filesDir, "gespraeche").list().orEmpty().map { it.removeSuffix(".json") }.toSet()
        stand.keys().asSequence().toList().filter { !it.startsWith("_") && it !in vorhanden }.forEach { stand.remove(it) }
    }

    /** true = Frank legt in diesen Worten ausdrücklich etwas für die Zukunft fest; nur dann darf ein Lernlauf eine Regel anlegen. */
    internal fun ausdruecklich(worte: List<String>): Boolean = worte.any { AUSDRUECKLICH.containsMatchIn(it.lowercase(Locale.GERMAN)) }

    /**
     * Übernimmt eine Regel aus einem Gespräch. Eine id gilt nur, wenn es die Regel gibt (sonst neu anlegen); die alte
     * Fassung landet in `merk_verlauf.jsonl`. Scheitert das Speichern, steht der Grund im Protokoll statt still verloren.
     * Ergebnis: (alte Fassung oder null, neue Fassung).
     */
    internal fun lerneRegel(app: Context, satz: String, id: Int?): Pair<String?, String>? {
        if (satz.length !in 12..400) return null
        val alle = Regeln.alle(app)
        if (alle.any { it.text.equals(satz, ignoreCase = true) }) return null
        val alt = id?.let { i -> alle.firstOrNull { it.id == i } }
        val neu = Regeln.speichere(app, satz, alt?.id, "Gespräch")
        if (neu == null) {
            Protokoll.melde(Quelle.JARVIS, if (Regeln.defekt(app) != null) "Gedächtnis defekt" else "Gedächtnis voll", "Nicht gelernt (Gespräch): $satz", ok = false)
            return null
        }
        if (alt != null) runCatching {
            File(app.filesDir, "merk_verlauf.jsonl").appendText(JSONObject().put("zeit", System.currentTimeMillis()).put("datei", "regeln")
                .put("id", alt.id).put("alt", alt.text).put("neu", neu.text).put("quelle", "Gespräch").toString() + "\n")
        }
        return alt?.text to neu.text
    }

    // ---------------------------------------------------------------- Helfer

    internal fun datum(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    private fun kurz(tag: LocalDate): String = tag.format(DateTimeFormatter.ofPattern("EEEE d.M.", Locale.GERMAN))
    internal fun zeit(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEEE d.M., HH:mm", Locale.GERMAN))
    internal fun flach(text: String, laenge: Int): String = text.replace(Regex("\\s+"), " ").trim().let { if (it.length > laenge) it.take(laenge) + " …" else it }

    /** Echte Festlegungen für die Zukunft; ein bloßes „immer“ oder „nie“ („immer noch müde“, „wie immer“) reicht nicht. */
    private val AUSDRUECKLICH = Regex("\\b(ab jetzt|ab sofort|von nun an|in zukunft|künftig|zukünftig|merk dir|merke dir|grundsätzlich|immer wenn|jedes mal wenn|nie wieder|nie mehr|du sollst (immer|nie)|sollst du (immer|nie))\\b")
}

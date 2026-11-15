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
    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences("gespraechsgedaechtnis", Context.MODE_PRIVATE)

    /**
     * Die nächste freie Notiz-id: über der höchsten in der Datei und über jeder je vergebenen. Eine gelöschte id wird nie
     * wieder vergeben, sonst träfe ein „wiederherstellen“ oder ein späteres „ERLEDIGT N…“ eine fremde Notiz.
     */
    @Synchronized
    private fun naechsteId(context: Context, alle: List<Gespraechsnotiz>): Int {
        val id = maxOf(alle.maxOfOrNull { it.id } ?: 0, prefs(context).getInt("hoechsteId", 0)) + 1
        // commit statt apply: Endet der Prozess gleich danach, darf die id nicht ein zweites Mal vergeben werden.
        prefs(context).edit().putInt("hoechsteId", id).commit()
        return id
    }

    /** Eine gemeinsame Sperre für das Anhängen an die Verlaufsdateien (`merk_verlauf.jsonl`, `lern_aenderungen.jsonl`). */
    private val anhaengeSperre = Any()

    /** Hängt eine Zeile an eine Verlaufsdatei; nie zwei Schreiber gleichzeitig, sonst vermischen sich die Zeilen. */
    internal fun haengeAn(datei: File, zeile: String) = synchronized(anhaengeSperre) { runCatching { datei.appendText(zeile.trimEnd('\n') + "\n") } }
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
        val notiz = Gespraechsnotiz(naechsteId(context, alleNotizen(context)), sauber, System.currentTimeMillis(), bis)
        schreibeMitAbgelaufenen(context, gueltig + notiz)
        return notiz
    }

    /**
     * Holt eine vom Lernlauf entfernte Notiz zurück (Wiederherstellen im Lernbericht): mit ihrer alten id, wenn die frei ist,
     * sonst unter einer neuen. War sie abgelaufen, gilt sie ab jetzt wieder 14 Tage, sonst wäre sie sofort unsichtbar und
     * der nächste Lauf entfernte sie erneut.
     */
    @Synchronized
    fun stelleNotizWiederHer(context: Context, notiz: Gespraechsnotiz): Boolean {
        val alle = alleNotizen(context)
        if (alle.any { it.id == notiz.id && it.text == notiz.text }) return true
        val abgelaufen = gueltigBis(notiz).isBefore(LocalDate.now())
        val frisch = if (abgelaufen) notiz.copy(erstellt = System.currentTimeMillis(), bis = null) else notiz
        val zurueck = if (alle.any { it.id == notiz.id }) frisch.copy(id = naechsteId(context, alle)) else frisch
        // Wie beim Merken: Die zurückgeholte Notiz steht als jüngste hinten; ist es zu voll, geht die älteste gültige, nachvollziehbar.
        schreibeMitAbgelaufenen(context, notizen(context) + zurueck)
        return alleNotizen(context).any { it.id == zurueck.id && it.text == zurueck.text }
    }

    /**
     * Schreibt die gültigen Notizen [gueltig] (jüngste hinten) und lässt abgelaufene in der Datei: Sie verschwinden erst über
     * [entferneAbgelaufene] im Lernlauf, mit Posten im Bericht und „Wiederherstellen“, nie still nebenbei. Ist es zu voll, geht die
     * älteste gültige Notiz; das steht im Protokoll.
     */
    @Synchronized
    private fun schreibeMitAbgelaufenen(context: Context, gueltig: List<Gespraechsnotiz>) {
        val ids = gueltig.mapTo(HashSet()) { it.id }
        val heute = LocalDate.now()
        val abgelaufen = alleNotizen(context).filter { it.id !in ids && gueltigBis(it).isBefore(heute) }
        gueltig.dropLast(NOTIZEN_HOECHSTENS).forEach { weg ->
            Logbuch.gedaechtnis("Gesprächsnotiz verdrängt (mehr als $NOTIZEN_HOECHSTENS)", weg.text.take(200))
            Protokoll.melde(Quelle.JARVIS, "Gesprächsnotiz verdrängt", "Mehr als $NOTIZEN_HOECHSTENS Notizen, die älteste ging: ${weg.text}")
        }
        schreibeNotizen(context, abgelaufen + gueltig.takeLast(NOTIZEN_HOECHSTENS))
    }

    /**
     * Abgelaufene Notizen (Ablaufdatum vorbei) endgültig aus der Datei nehmen; liefert die entfernten. Was Frank im Lernbericht
     * behalten oder wiederhergestellt hat, wird nie automatisch entfernt: Es gilt dann einfach weitere 14 Tage.
     */
    @Synchronized
    fun entferneAbgelaufene(context: Context): List<Gespraechsnotiz> {
        val alle = alleNotizen(context)
        val gueltig = notizen(context)
        val abgelaufen = alle.filter { a -> gueltig.none { it.id == a.id } }
        if (abgelaufen.isEmpty()) return emptyList()
        val (behalten, weg) = abgelaufen.partition { Lernlauf.istBehalten(context, "Notiz", it.text) }
        val erneuert = behalten.map { it.copy(erstellt = System.currentTimeMillis(), bis = null) }
        schreibeNotizen(context, gueltig + erneuert)
        return weg
    }

    /** Bis wann eine Notiz gilt (eigenes Datum oder 14 Tage nach dem Anlegen). */
    fun gueltigBis(n: Gespraechsnotiz): LocalDate = n.bis ?: datum(n.erstellt).plusDays(NOTIZ_TAGE)

    /** [text] gesetzt: nur löschen, wenn die Notiz mit dieser id noch genau diesen Text hat (ein langer Lauf sah eine ältere Fassung). */
    @Synchronized
    fun loescheNotiz(context: Context, id: Int, text: String? = null): Boolean {
        val alle = alleNotizen(context)
        if (alle.none { it.id == id && (text == null || it.text == text) }) return false
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

    /** Letztes Ergebnis von [kurzzeit] mit dem Fingerabdruck seiner Quellen. */
    @Volatile private var kurzzeitZwischen: Triple<String, Long, String>? = null
    private const val KURZZEIT_HOECHSTENS_MS = 5 * 60_000L

    /**
     * Das Kurzzeitgedächtnis. Zwischengespeichert: Neu gebaut wird nur, wenn sich eine Gesprächsdatei, die Notizen oder das
     * Protokoll geändert haben oder der Stand älter als fünf Minuten ist (die 48-Stunden-Fenster rücken sonst nicht weiter).
     */
    fun kurzzeit(context: Context): String {
        val app = context.applicationContext
        Protokoll.lade(app)
        val jetzt = System.currentTimeMillis()
        val grenzeDateien = jetzt - TAGE * 24 * 3_600_000L
        val dateien = File(app.filesDir, "gespraeche").listFiles { f -> f.name.endsWith(".json") && f.lastModified() >= grenzeDateien }.orEmpty()
        val eintraege = Protokoll.eintraege.value
        val fingerabdruck = dateien.sortedBy { it.name }.joinToString(",") { it.name + ":" + it.lastModified() + ":" + it.length() } +
            "|" + notizDatei(app).lastModified() + "|" + eintraege.size + ":" + (eintraege.firstOrNull()?.zeit ?: 0L)
        kurzzeitZwischen?.let { (abdruck, zeit, text) -> if (abdruck == fingerabdruck && jetzt - zeit < KURZZEIT_HOECHSTENS_MS) return text }
        return baueKurzzeit(app, jetzt).also { kurzzeitZwischen = Triple(fingerabdruck, jetzt, it) }
    }

    private fun baueKurzzeit(app: Context, jetzt: Long): String {
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
    /**
     * [vorruecken] = alles Offene wurde gelesen: Erst dann rückt der Zeitpunkt des letzten Laufs vor. Blieb etwas übrig, bliebe es
     * sonst außerhalb des nächsten (dann wieder kürzeren) Lesefensters und ginge still verloren.
     */
    internal fun speichereStand(context: Context, stand: JSONObject, vorruecken: Boolean = true) = runCatching {
        if (vorruecken) stand.put("_zuletzt", System.currentTimeMillis())
        de.frank.jarvis.ablage.AblageSpeicher.schreibeAtomar(standDatei(context), stand.toString().toByteArray())
    }

    /** Wiedererkennung einer Nachricht: Wer sie schrieb und ihr Text. */
    private fun anker(n: de.frank.jarvis.data.GespeicherteNachricht): String = (if (n.vonMir) "F:" else "J:") + n.text.trim().hashCode()

    /**
     * Wie viele Nachrichten eines Gesprächs schon gelernt sind. Maßgeblich ist die zuletzt gelernte Nachricht (Anker), nicht die
     * reine Anzahl: Fällt davor eine weg (Erneut versuchen, Kürzen) oder ändert sich ihr Fehlerstatus, verrutschte die Anzahl.
     * Steht der Anker mehrfach im Gespräch, gilt die Stelle, die der alten Anzahl am nächsten liegt; fehlt er, die Anzahl.
     */
    internal fun gelerntBis(stand: JSONObject, id: String, nachrichten: List<de.frank.jarvis.data.GespeicherteNachricht>): Int {
        val anzahl = stand.optInt(id, 0).coerceAtMost(nachrichten.size)
        val a = stand.optJSONObject("_anker")?.optString(id).orEmpty()
        if (a.isEmpty() || anzahl == 0) return anzahl
        val stellen = nachrichten.indices.filter { anker(nachrichten[it]) == a }
        return stellen.minByOrNull { kotlin.math.abs(it + 1 - anzahl) }?.let { it + 1 } ?: anzahl
    }

    /** Hält fest, dass [bis] Nachrichten des Gesprächs gelernt sind, mit dem Anker der letzten. */
    internal fun merkeGelernt(stand: JSONObject, id: String, nachrichten: List<de.frank.jarvis.data.GespeicherteNachricht>, bis: Int) {
        stand.put(id, bis)
        val anker = stand.optJSONObject("_anker") ?: JSONObject().also { stand.put("_anker", it) }
        if (bis in 1..nachrichten.size) anker.put(id, anker(nachrichten[bis - 1])) else anker.remove(id)
    }

    /**
     * Wie weit der Lernlauf zurückliest: drei Tage, und nach einer längeren Pause (nicht angemeldet, Lernen aus, kein Netz)
     * bis zum letzten Lauf zurück, höchstens 14 Tage. Sonst gingen Franks Worte aus der Pause still verloren.
     */
    private fun lernTage(stand: JSONObject): Long {
        val zuletzt = stand.optLong("_zuletzt", 0L)
        if (zuletzt <= 0L) return 3L
        return ((System.currentTimeMillis() - zuletzt) / (24 * 3_600_000L) + 2).coerceIn(3L, 14L)
    }

    /**
     * Gespräche seit dem letzten Lauf (mindestens drei Tage, siehe [lernTage]) mit neuen Worten von Frank: (Gespräch, bisher
     * gelernte Anzahl, alle Nachrichten, ruht schon zehn Minuten). Programmier-Gespräche nie: Dort spricht Frank mit dem
     * Programmierer über eine App („ab jetzt immer die Version anzeigen“), nicht mit Jarvis über sich.
     */
    private fun mitNeuem(context: Context, stand: JSONObject): List<Pair<Triple<de.frank.jarvis.data.Sitzung, Int, List<de.frank.jarvis.data.GespeicherteNachricht>>, Boolean>> {
        val jetzt = System.currentTimeMillis()
        return neuereGespraeche(context, lernTage(stand)).filter { !it.first.programmierung }.mapNotNull { (s, nachrichten) ->
            val gelernt = gelerntBis(stand, s.id, nachrichten)
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
    fun neueGespraecheFaellig(context: Context): Boolean = runCatching {
        // Billig bei jeder Wächter-Minute: Nur wenn sich eine Gesprächsdatei oder der Stand geändert hat oder fünf Minuten vergangen sind
        // (ein Gespräch kann inzwischen zehn Minuten ruhen), werden die Dateien wirklich gelesen.
        val app = context.applicationContext
        val jetzt = System.currentTimeMillis()
        val stand = ladeStand(app)
        val grenze = jetzt - lernTage(stand) * 24 * 3_600_000L
        val abdruck = File(app.filesDir, "gespraeche").listFiles { f -> f.name.endsWith(".json") && f.lastModified() >= grenze }.orEmpty()
            .sortedBy { it.name }.joinToString(",") { it.name + ":" + it.lastModified() + ":" + it.length() } + "|" + standDatei(app).lastModified()
        faelligZwischen?.let { (a, zeit, ergebnis) -> if (a == abdruck && jetzt - zeit < FAELLIG_HOECHSTENS_MS) return@runCatching ergebnis }
        offen(app, stand).isNotEmpty().also { faelligZwischen = Triple(abdruck, jetzt, it) }
    }.getOrDefault(false)

    @Volatile private var faelligZwischen: Triple<String, Long, Boolean>? = null
    private const val FAELLIG_HOECHSTENS_MS = 5 * 60_000L

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
        stand.optJSONObject("_anker")?.let { a -> a.keys().asSequence().toList().filter { it !in vorhanden }.forEach { a.remove(it) } }
    }

    /** true = Frank legt in diesen Worten ausdrücklich etwas für die Zukunft fest; nur dann darf ein Lernlauf eine Regel anlegen. */
    internal fun ausdruecklich(worte: List<String>): Boolean = worte.any { AUSDRUECKLICH.containsMatchIn(it.lowercase(Locale.GERMAN)) }

    /**
     * Stammt die Regel [satz] aus einer ausdrücklichen Festlegung von Frank? Ja nur, wenn eine seiner Nachrichten mit „ab jetzt“,
     * „merk dir“ … mindestens zwei inhaltliche Wörter mit der Regel teilt. Ein „grundsätzlich“ in einem anderen Gespräch reicht nicht.
     */
    internal fun regelBelegt(satz: String, frankJeGespraech: Collection<List<String>>): Boolean {
        val regel = inhaltsWoerter(satz)
        return frankJeGespraech.any { worte -> worte.any { w -> AUSDRUECKLICH.containsMatchIn(w.lowercase(Locale.GERMAN)) && (inhaltsWoerter(w) intersect regel).size >= 2 } }
    }

    /** Wörter ab fünf Buchstaben, klein, ohne Endung „-en/-e/-n/-s“: grob genug, dass „Schlaffragen“ und „Schlaffrage“ zusammenpassen. */
    private fun inhaltsWoerter(text: String): Set<String> = Regex("[\\p{L}]{5,}").findAll(text.lowercase(Locale.GERMAN))
        .map { it.value.removeSuffix("en").removeSuffix("e").removeSuffix("n").removeSuffix("s") }.filter { it.length >= 4 }.toSet()

    /**
     * Eine Antwortzeile eines Lern-Modells, bereinigt: Aufzählungszeichen, Nummern, Fettdruck und Anführungszeichen am Rand weg.
     * Gemeinsam für Lernlauf und Tagebuch-Lernen, damit beide dieselben Zeilen erkennen.
     */
    internal fun bereinigeZeile(zeile: String): String = zeile.trim().replace("**", "")
        .replace(Regex("^(?:[-*•–]\\s*|\\d+[.)]\\s*)"), "").trim()
        .trimStart('„', '"', '“', '»', '\'').trimEnd('“', '”', '"', '«', '\'').trim()

    /**
     * Übernimmt eine Regel aus einem Gespräch. Eine id gilt nur für eine selbst gelernte Regel (sonst neu anlegen); die alte
     * Fassung landet in `merk_verlauf.jsonl`. Scheitert das Speichern, steht der Grund im Protokoll statt still verloren.
     * Ergebnis: (alte Fassung oder null, neue Fassung).
     */
    internal fun lerneRegel(app: Context, satz: String, id: Int?): Pair<String?, String>? {
        if (satz.length !in 12..400) return null
        // Ein Lernlauf erzeugt keine Rechte und macht nichts Situatives dauerhaft: Erlaubnisse (mit belegtem Geltungsbereich)
        // legt nur Frank selbst über jarvis_merken oder die App an.
        if (de.frank.jarvis.faehigkeit.Merkdatei.istErlaubnis(satz) || de.frank.jarvis.faehigkeit.Merkdatei.situativ(satz) != null) {
            // Kein Fehler, nur ein bewusst verworfener Vorschlag: ins Logbuch, nicht ins Protokoll (dort stehen echte Speicherfehler).
            de.frank.jarvis.observability.Logbuch.gedaechtnis("Nicht als Regel gelernt (Erlaubnis oder situativ)", satz.take(200))
            return null
        }
        val alle = Regeln.alle(app)
        // Nur eine selbst gelernte Regel darf ein Lernlauf ersetzen; Franks eigene Regeln bleiben, die neue kommt daneben.
        val alt = id?.let { i -> alle.firstOrNull { it.id == i && it.gelernt } }
        // Dubletten auch in leicht anderer Fassung (wie bei Infos und Zielen), nur nicht gegen die Regel, die ersetzt wird.
        if (alle.any { it.id != alt?.id && (it.text.equals(satz, ignoreCase = true) || de.frank.jarvis.faehigkeit.Merkdatei.aehnlichkeit(it.text, satz) >= 0.6) }) return null
        val neu = Regeln.speichere(app, satz, alt?.id, "Gespräch")
        if (neu == null) {
            Protokoll.melde(Quelle.JARVIS, if (Regeln.defekt(app) != null) "Gedächtnis defekt" else "Gedächtnis voll", "Nicht gelernt (Gespräch): $satz", ok = false)
            return null
        }
        if (alt != null) haengeAn(File(app.filesDir, "merk_verlauf.jsonl"), JSONObject().put("zeit", System.currentTimeMillis()).put("datei", "regeln")
            .put("id", alt.id).put("alt", alt.text).put("neu", neu.text).put("quelle", "Gespräch").toString())
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

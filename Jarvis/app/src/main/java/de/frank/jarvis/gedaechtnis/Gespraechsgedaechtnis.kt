package de.frank.jarvis.gedaechtnis

import android.content.Context
import android.util.Log
import de.frank.jarvis.agent.JarvisAgent
import de.frank.jarvis.agent.Lernen
import de.frank.jarvis.auth.ChatTurn
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Gespraeche
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.faehigkeit.Regeln
import de.frank.jarvis.faehigkeit.UeberFrank
import de.frank.jarvis.faehigkeit.Ziele
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
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
 * Lernen ([lerne]): Ist ein Gespräch zehn Minuten ruhig, liest Jarvis die neuen Wechsel und übernimmt daraus
 * dauerhafte Ziele und Tatsachen in die Merkdateien, ausdrücklich verlangte Regeln und kurzlebige Gesprächsnotizen
 * (Vorhaben, Zusagen, offene Themen, mit Ablaufdatum). Grundlage sind nur Franks eigene Aussagen.
 */
object Gespraechsgedaechtnis {
    private const val TAG = "JarvisGespraeche"
    private const val TAGE = 10L
    private const val NOTIZEN_HOECHSTENS = 40
    /** Ohne eigenes Ablaufdatum gilt eine Notiz so lange. */
    private const val NOTIZ_TAGE = 14L
    private const val RUHE_MS = 10 * 60_000L
    private const val HOECHSTENS = 9_000
    /** So groß wird die Eingabe eines Lernlaufs höchstens (wächst nur, wenn schon das Gemerkte größer ist). */
    private const val EINGABE_HOECHSTENS = 18_000

    private fun notizDatei(context: Context) = File(context.applicationContext.filesDir, "gespraechsnotizen.json")
    private fun standDatei(context: Context) = File(context.applicationContext.filesDir, "gespraeche_gelernt.json")

    // ---------------------------------------------------------------- Notizen

    @Synchronized
    fun alleNotizen(context: Context): List<Gespraechsnotiz> = runCatching {
        val a = JSONArray(notizDatei(context).readText())
        (0 until a.length()).map { i -> a.getJSONObject(i).let { n -> Gespraechsnotiz(n.getInt("id"), n.getString("text"), n.optLong("erstellt"), n.optString("bis").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }) } }
    }.getOrDefault(emptyList())

    /** Die gültigen Notizen: nicht abgelaufen. */
    fun notizen(context: Context): List<Gespraechsnotiz> {
        val heute = LocalDate.now()
        return alleNotizen(context).filter { n -> !(n.bis ?: datum(n.erstellt).plusDays(NOTIZ_TAGE)).isBefore(heute) }
    }

    @Synchronized
    private fun schreibeNotizen(context: Context, liste: List<Gespraechsnotiz>) {
        val inhalt = JSONArray(liste.map { JSONObject().put("id", it.id).put("text", it.text).put("erstellt", it.erstellt).put("bis", it.bis?.toString() ?: "") }).toString()
        val d = notizDatei(context)
        val neu = File(d.path + ".neu")
        neu.writeText(inhalt)
        if (!neu.renameTo(d)) { d.writeText(inhalt); neu.delete() }
    }

    @Synchronized
    fun merkeNotiz(context: Context, text: String, bis: LocalDate?): Gespraechsnotiz? {
        val sauber = text.replace(Regex("\\s+"), " ").trim().take(300)
        if (sauber.length < 8) return null
        // Abgelaufene fallen beim Schreiben weg; bleibt es zu voll, geht die älteste.
        val gueltig = notizen(context)
        if (gueltig.any { it.text.equals(sauber, ignoreCase = true) }) return null
        val notiz = Gespraechsnotiz((alleNotizen(context).maxOfOrNull { it.id } ?: 0) + 1, sauber, System.currentTimeMillis(), bis)
        schreibeNotizen(context, (gueltig + notiz).takeLast(NOTIZEN_HOECHSTENS))
        return notiz
    }

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

    // ---------------------------------------------------------------- Lernen aus Gesprächen

    private fun ladeStand(context: Context): JSONObject = runCatching { JSONObject(standDatei(context).readText()) }.getOrDefault(JSONObject())
    private fun speichereStand(context: Context, stand: JSONObject) = runCatching { standDatei(context).writeText(stand.toString()) }

    /** Neue Wechsel aus Gesprächen, die seit mindestens zehn Minuten ruhen: (Gespräch, bisher gelernte Anzahl, alle Nachrichten). */
    private fun offen(context: Context, stand: JSONObject): List<Triple<de.frank.jarvis.data.Sitzung, Int, List<de.frank.jarvis.data.GespeicherteNachricht>>> {
        val jetzt = System.currentTimeMillis()
        return neuereGespraeche(context, 3).mapNotNull { (s, nachrichten) ->
            val gelernt = stand.optInt(s.id, 0).coerceAtMost(nachrichten.size)
            if (jetzt - s.geaendert < RUHE_MS || nachrichten.drop(gelernt).none { it.vonMir }) null else Triple(s, gelernt, nachrichten)
        }
    }

    /**
     * Gibt es etwas zu lernen? Billig: liest nur die Gespräche der letzten drei Tage. Neue Aktionen über ChatGPT allein
     * lösen keinen Lauf aus: Sie sind Werkzeugergebnisse, nicht Franks Worte, also keine Lernquelle.
     */
    fun lernenFaellig(context: Context): Boolean = runCatching {
        Einstellungen.get(context).auswertungLernen && !lernSperre.isLocked && offen(context, ladeStand(context)).isNotEmpty()
    }.getOrDefault(false)

    /** Nie zwei Lernläufe gleichzeitig (Knopf in der Oberfläche und Wächter): Beide würden dasselbe lernen. */
    private val lernSperre = Mutex()

    /** true = gerade läuft ein Lernlauf aus Gesprächen. */
    val lerntGerade: Boolean get() = lernSperre.isLocked

    private fun neueAktionen(context: Context, stand: JSONObject): List<de.frank.jarvis.data.Eintrag> {
        Protokoll.lade(context.applicationContext)
        val seit = stand.optLong("_chatgpt", System.currentTimeMillis() - 24 * 3_600_000L)
        val titel = schreibTitel(context)
        return Protokoll.eintraege.value.filter { it.zeit > seit && it.ok && it.quelle == Quelle.CHATGPT && it.werkzeug in titel }
    }

    /**
     * Ein Lernlauf über alle ruhenden Gespräche mit neuen Wechseln und die neuen Aktionen über ChatGPT.
     * true = es wurde etwas übernommen. Ohne Modellantwort bleibt alles offen und der nächste Lauf versucht es erneut.
     */
    suspend fun lerne(context: Context): Boolean {
        // Läuft schon ein Lernlauf, gibt es nichts zu tun: Er liest dieselben Wechsel.
        if (!lernSperre.tryLock()) return false
        try {
            return lerneGesperrt(context)
        } finally {
            lernSperre.unlock()
        }
    }

    private suspend fun lerneGesperrt(context: Context): Boolean {
        val app = context.applicationContext
        val e = Einstellungen.get(app)
        if (!e.auswertungLernen) return false
        val agent = JarvisAgent(app)
        if (!agent.verbunden) return false
        val stand = ladeStand(app)
        val gespraeche = offen(app, stand)
        val aktionen = neueAktionen(app, stand).sortedBy { it.zeit }
        if (gespraeche.isEmpty()) {
            // Aktionen allein sind keine Lernquelle (Werkzeugergebnisse, nicht Franks Worte): nur den Stand vorziehen, ohne Modellaufruf.
            if (aktionen.isNotEmpty()) { stand.put("_chatgpt", aktionen.last().zeit); speichereStand(app, stand) }
            return false
        }

        val kopf = buildString {
            append("HEUTE: ").append(LocalDate.now()).append("\n\n")
            append("SCHON GEMERKT:\nREGELN:\n").append(Regeln.liste(Regeln.alle(app)).ifEmpty { "(keine)" })
            append("\n\nZIELE:\n").append(Ziele.liste(Ziele.alle(app)).ifEmpty { "(keine)" })
            append("\n\nINFOS:\n").append(UeberFrank.liste(UeberFrank.alle(app)).ifEmpty { "(keine)" })
            append("\n\nNOTIZEN:\n").append(notizen(app).joinToString("\n") { "[${it.id}] ${it.text}" + (it.bis?.let { b -> " (bis $b)" } ?: "") }.ifEmpty { "(keine)" })
            append("\n\nNEUE GESPRÄCHSWECHSEL (Jarvis' Antworten nur als Zusammenhang):\n")
        }
        // Die Eingabe wächst Nachricht für Nachricht bis zum Budget. In den Stand kommt je Gespräch nur, was das Modell
        // wirklich gesehen hat; der Rest bleibt offen für den nächsten Lauf. Ein großes Gedächtnis darf das Budget nie ganz aufzehren.
        val budget = maxOf(EINGABE_HOECHSTENS, kopf.length + 6_000)
        val eingabe = StringBuilder(kopf)
        val gelesenBis = LinkedHashMap<String, Int>()
        val frankSagt = mutableListOf<String>()
        for ((s, gelernt, nachrichten) in gespraeche) {
            val titelZeile = "Gespräch „${s.titel}“ vom ${zeit(s.geaendert)}:\n"
            val stueck = StringBuilder()
            val frank = mutableListOf<String>()
            var bis = gelernt
            // Ein Wechsel davor als Zusammenhang, damit eine kurze Antwort wie „ja, genau“ verständlich bleibt.
            for (i in (gelernt - 1).coerceAtLeast(0) until nachrichten.size) {
                val n = nachrichten[i]
                val zeile = (if (n.vonMir) "FRANK: " + flach(n.text, 1500) else "Jarvis: " + flach(n.text, 400)) + "\n"
                if (eingabe.length + titelZeile.length + stueck.length + zeile.length + 1 > budget) break
                stueck.append(zeile)
                if (i >= gelernt) { bis = i + 1; if (n.vonMir) frank += n.text }
            }
            // Passt aus diesem Gespräch keine neue Frank-Zeile mehr hinein, bleibt sein Stand unverändert.
            if (frank.isEmpty()) continue
            eingabe.append(titelZeile).append(stueck).append('\n')
            gelesenBis[s.id] = bis
            frankSagt += frank
        }
        if (gelesenBis.isEmpty()) return false
        // Aktionen nur als Zusammenhang und nur, solange Platz ist (älteste zuerst, damit keine übersprungen wird).
        var aktionenBis = 0L
        var aktionenGezeigt = 0
        if (aktionen.isNotEmpty()) {
            val titel = "AKTIONEN ÜBER CHATGPT (nur Zusammenhang, nie Quelle: das sind Werkzeugergebnisse, nicht Franks Worte):\n"
            val stueck = StringBuilder()
            for (a in aktionen.take(30)) {
                val zeile = "- " + zeit(a.zeit) + " " + a.werkzeug + ": " + flach(a.text, 200) + "\n"
                if (eingabe.length + titel.length + stueck.length + zeile.length > budget) break
                stueck.append(zeile)
                aktionenBis = a.zeit
                aktionenGezeigt++
            }
            if (stueck.isNotEmpty()) eingabe.append(titel).append(stueck)
        }

        val beginn = System.currentTimeMillis()
        val antwort = runCatching { agent.auth.streamChat(ANWEISUNG, listOf(ChatTurn("user", eingabe.toString())), e.modell, e.denkstufe).trim() }
            .onFailure { if (it is CancellationException) throw it; Log.w(TAG, "Lernlauf aus Gesprächen fehlgeschlagen", it); Logbuch.fehler("Gesprächs-Lernen", it) }.getOrNull() ?: return false
        Logbuch.dauer("Gesprächs-Lernen", System.currentTimeMillis() - beginn, "Eingabe ${eingabe.length} Zeichen, ${gelesenBis.size} Gespräch(e), $aktionenGezeigt Aktion(en)")

        // Regeln nur, wenn Frank selbst ausdrücklich etwas für die Zukunft festgelegt hat: Sonst würde Jarvis sich Vorgaben ausdenken.
        val regelErlaubt = frankSagt.any { AUSDRUECKLICH.containsMatchIn(it.lowercase(Locale.GERMAN)) }
        val gelernt = mutableListOf<String>()
        val zieleUndInfos = mutableListOf<String>()
        antwort.lines().map { it.trim().replace("**", "").replace(Regex("^(?:[-*•–]\\s*|\\d+[.)]\\s*)"), "").trim() }.forEach { zeile ->
            ZEILE.find(zeile)?.let { t ->
                val art = t.groupValues[1].uppercase(Locale.GERMAN)
                val id = t.groupValues[2].toIntOrNull()
                val satz = t.groupValues[3].trim()
                when (art) {
                    // Ziele und Infos nach denselben Regeln wie beim Tagebuch-Lernen: Ziele nie ersetzen, ungültige id = neu,
                    // alte Fassung in den Verlauf, „Gedächtnis voll“ ins Protokoll.
                    "ZIEL", "INFO" -> zieleUndInfos += art + (id?.let { " $it" } ?: "") + ": " + satz
                    "REGEL" -> if (regelErlaubt) lerneRegel(app, satz, id)?.let { gelernt += "Regel: $it" }
                    "NOTIZ" -> {
                        val bis = BIS.find(satz)?.groupValues?.get(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        merkeNotiz(app, satz.replace(BIS, "").trim(), bis)?.let { gelernt += "Notiz: " + it.text }
                    }
                    "ERLEDIGT" -> id?.let { if (loescheNotiz(app, it)) gelernt += "Notiz $it erledigt" }
                }
            }
        }
        if (zieleUndInfos.isNotEmpty()) gelernt += Lernen.uebernimm(app, zieleUndInfos.joinToString("\n"), "Gespräch", hoechstens = 6).map { "Gemerkt: $it" }
        gelesenBis.forEach { (id, anzahl) -> stand.put(id, anzahl) }
        if (aktionenBis > 0L) stand.put("_chatgpt", aktionenBis)
        // Alte Einträge im Stand aufräumen: Gespräche, die es nicht mehr gibt, braucht er nicht.
        val vorhanden = File(app.filesDir, "gespraeche").list().orEmpty().map { it.removeSuffix(".json") }.toSet()
        stand.keys().asSequence().toList().filter { !it.startsWith("_") && it !in vorhanden }.forEach { stand.remove(it) }
        speichereStand(app, stand)
        if (gelernt.isNotEmpty()) {
            Protokoll.melde(Quelle.JARVIS, "Aus Gesprächen gelernt", gelernt.joinToString(" · "))
            Logbuch.gedaechtnis("Aus Gesprächen gelernt", gelernt.joinToString(" · "))
            Lagebild.anstossen(app, listOf(Bereich.GEMERKT, Bereich.GESPRAECHE), "aus Gesprächen gelernt")
        }
        return gelernt.isNotEmpty()
    }

    /**
     * Übernimmt eine Regel aus einem Gespräch. Eine id gilt nur, wenn es die Regel gibt (sonst neu anlegen); die alte
     * Fassung landet in `merk_verlauf.jsonl`. Scheitert das Speichern, steht der Grund im Protokoll statt still verloren.
     */
    private fun lerneRegel(app: Context, satz: String, id: Int?): String? {
        if (satz.length !in 12..400) return null
        val alle = Regeln.alle(app)
        if (alle.any { it.text.equals(satz, ignoreCase = true) }) return null
        val alt = id?.let { i -> alle.firstOrNull { it.id == i } }
        val neu = Regeln.speichere(app, satz, alt?.id)
        if (neu == null) {
            Protokoll.melde(Quelle.JARVIS, if (Regeln.defekt(app) != null) "Gedächtnis defekt" else "Gedächtnis voll", "Nicht gelernt (Gespräch): $satz", ok = false)
            return null
        }
        if (alt != null) runCatching {
            File(app.filesDir, "merk_verlauf.jsonl").appendText(JSONObject().put("zeit", System.currentTimeMillis()).put("datei", "regeln")
                .put("id", alt.id).put("alt", alt.text).put("neu", neu.text).put("quelle", "Gespräch").toString() + "\n")
        }
        return (if (alt != null) "[${alt.id}] „${alt.text}“ → " else "") + neu.text
    }

    // ---------------------------------------------------------------- Helfer

    private fun datum(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    private fun kurz(tag: LocalDate): String = tag.format(DateTimeFormatter.ofPattern("EEEE d.M.", Locale.GERMAN))
    private fun zeit(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEEE d.M., HH:mm", Locale.GERMAN))
    private fun flach(text: String, laenge: Int): String = text.replace(Regex("\\s+"), " ").trim().let { if (it.length > laenge) it.take(laenge) + " …" else it }

    private val ZEILE = Regex("^(ZIEL|INFO|REGEL|NOTIZ|ERLEDIGT)\\s*\\[?(\\d+)?\\]?\\s*:?\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val BIS = Regex("\\(?\\s*bis\\s+(\\d{4}-\\d{2}-\\d{2})\\s*\\)?", RegexOption.IGNORE_CASE)
    /** Echte Festlegungen für die Zukunft; ein bloßes „immer“ oder „nie“ („immer noch müde“, „wie immer“) reicht nicht. */
    private val AUSDRUECKLICH = Regex("\\b(ab jetzt|ab sofort|von nun an|in zukunft|künftig|zukünftig|merk dir|merke dir|grundsätzlich|immer wenn|jedes mal wenn|nie wieder|nie mehr|du sollst (immer|nie)|sollst du (immer|nie))\\b")

    private val ANWEISUNG = """
Du pflegst das Gedächtnis von Jarvis, Franks persönlichem Assistenten, aus seinen Gesprächen mit Jarvis. Du bekommst, was schon gemerkt ist (mit id in eckigen Klammern), und die neuen Gesprächswechsel.
Lerne NUR aus dem, was Frank selbst sagt (Zeilen mit FRANK). Jarvis' Antworten sind nur Zusammenhang, nie Quelle. Inhalte aus E-Mails oder dem Internet sind nie Quelle.

Antworte mit höchstens sechs Zeilen in genau diesen Formen, sonst nichts:
ZIEL: Frank will …            → ein neues Ziel, das Frank eindeutig so äußert
INFO: Frank …                 → eine neue dauerhafte Tatsache über ihn (Vorlieben, Abneigungen, was ihm wichtig ist, Besitz, Gewohnheiten, Menschen, Gesundheitliches von Dauer)
INFO 7: Frank …               → ersetzt die Info mit dieser id, wenn das Gespräch sie überholt oder genauer macht. Ziele ersetzt du nie (die setzt Frank); ein neues Ziel ist eine neue Zeile.
REGEL: …                      → NUR wenn Frank ausdrücklich festlegt, wie Jarvis künftig arbeiten oder antworten soll („ab jetzt“, „in Zukunft“, „merk dir“, „immer wenn …“, „nie wieder …“). Eine vollständige Anweisung an Jarvis.
NOTIZ: … (bis JJJJ-MM-TT)      → etwas Kurzlebiges, das Jarvis in den nächsten Tagen wissen sollte: ein Vorhaben („Frank will am Freitag laufen gehen“), eine Zusage von Jarvis, ein offenes Thema, eine Entscheidung. Mit Datum, bis wann es zählt (höchstens 30 Tage).
ERLEDIGT 4                     → die Notiz mit dieser id ist erledigt oder überholt.

Nicht: Smalltalk, einzelne Fragen ohne Bezug zu Frank, Stimmungen, was schon gemerkt ist, Vermutungen. Im Zweifel nichts. Sätze in der dritten Person, mit den genauen Namen, Zahlen und Daten, die Frank genannt hat.
Gibt es nichts, antworte nur mit KEINE.
""".trim()
}

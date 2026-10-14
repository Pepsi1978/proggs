package de.frank.jarvis.faehigkeit

import android.content.Context
import android.util.Log
import de.frank.jarvis.data.Einstellungen
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Franks Tagebuch: Markdown-Dateien `JJJJ-MM-TT_Tagebucheintrag.md` in einem Google-Drive-Ordner.
 * Der eigene Server holt sie mit einem Nur-Lese-Zugang aus Drive (siehe `Jarvis/server`), Jarvis holt sie
 * von dort bei jeder Synchronisierung aufs Handy und liest dann lokal. Nur lesend.
 */
class TagebuchFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "tagebuch"
    override val name = "Tagebuch"
    override val beschreibung = "Franks Tagebucheinträge nach Datum lesen und durchsuchen."
    override val hinweise =
        "Das Tagebuch enthält je Tag einen Eintrag, in dem Frank festhält, was er gemacht, erlebt und gedacht hat. Nutze es für „Was habe ich gestern gemacht?“, " +
            "„Was war letzte Woche los?“, „Wann habe ich zuletzt …?“. Die Einträge sind persönlich: gib sie sachlich wieder, ohne zu werten. Das Tagebuch ist nur lesbar; " +
            "neue Einträge entstehen in ChatGPT, nicht über Jarvis. Für manche Tage gibt es keinen Eintrag."

    private val ordner get() = File(context.filesDir, "tagebuch").apply { mkdirs() }
    private val e get() = Einstellungen.get(context)
    @Volatile private var zuletztGeholt = 0L
    /** Bündelt gleichzeitige Abrufe (Wächter, Werkzeuge, Auswertung): Es läuft nie mehr als ein Vollabruf. */
    private val abrufSperre = kotlinx.coroutines.sync.Mutex()
    /** Was der Server zuletzt über den Drive-Zugang gemeldet hat; leer = alles in Ordnung. */
    @Volatile var driveFehler = ""
        private set

    override fun stoerung(): String? = when {
        e.serverHost.isBlank() || e.serverToken.isBlank() -> "Der Server-Schlüssel fehlt in dieser Jarvis-Fassung. In Jarvis unter Einstellungen den Server-Schlüssel eintragen " +
            "(steht am PC in ~/SK/Jarvis/relay.properties unter token) oder Jarvis einmal am PC bauen; danach bleibt er gespeichert."
        driveFehler.isNotEmpty() -> "Der Server kann Google Drive nicht lesen. In Jarvis unter Einstellungen → Tagebuch auf „Berechtigung erneuern“ tippen."
        else -> null
    }

    /** Datum → Datei, neueste zuerst. Das Datum steht am Anfang des Dateinamens. */
    private fun eintraege(): List<Pair<LocalDate, File>> = ordner.listFiles { f -> f.isFile && f.name.endsWith(".md") }.orEmpty()
        .mapNotNull { f -> runCatching { LocalDate.parse(f.name.take(10)) to f }.getOrNull() }.sortedByDescending { it.first }

    /**
     * Holt die Einträge frisch vom Server. Bricht der Abruf ab, bleibt der bisherige Bestand lesbar.
     * Rückgabe: Anzahl der Einträge oder null bei Fehler.
     */
    suspend fun synchronisiere(tage: Int = 4000): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/tagebuch?tage=$tage").header("X-Jarvis-Token", e.serverToken).build()
            val antwort = CLIENT.newCall(anfrage).execute().use { a ->
                check(a.isSuccessful) { "Server antwortet mit ${a.code}" }
                JSONObject(a.body?.string().orEmpty())
            }
            val dateien = antwort.getJSONArray("dateien")
            val namen = HashSet<String>()
            for (i in 0 until dateien.length()) {
                val d = dateien.getJSONObject(i)
                val name = d.getString("name")
                if (!NAME.matches(name)) continue
                namen += name
                File(ordner, name).let { ziel ->
                    if (!ziel.exists() || ziel.readText() != d.getString("text")) {
                        // Erst daneben schreiben, dann umbenennen: Ein Abbruch hinterlässt nie einen halben Eintrag.
                        val neu = File(ordner, ".$name.neu")
                        neu.writeText(d.getString("text"))
                        if (!neu.renameTo(ziel)) { ziel.writeText(d.getString("text")); neu.delete() }
                    }
                }
            }
            // Was im Drive-Ordner gelöscht wurde, verschwindet auch hier — aber nur, wenn der Server sauber gelesen hat.
            if (antwort.optString("fehler").isEmpty() && tage >= 4000 && namen.isNotEmpty()) ordner.listFiles()?.filter { it.name.endsWith(".md") && it.name !in namen }?.forEach { it.delete() }
            zuletztGeholt = System.currentTimeMillis()
            driveFehler = antwort.optString("fehler")
            if (driveFehler.isNotEmpty()) Log.w("JarvisTagebuch", driveFehler)
            eintraege().size
        }.onFailure { Log.w("JarvisTagebuch", "Synchronisierung fehlgeschlagen", it) }.getOrNull()
    }

    /** Vor dem Lesen kurz nachholen, wenn der Bestand leer oder älter als 20 Minuten ist. */
    private suspend fun frisch() = abrufSperre.withLock { if (eintraege().isEmpty() || System.currentTimeMillis() - zuletztGeholt > 20 * 60_000L) synchronisiere() }

    /** Die letzten Tage im Volltext (begrenzt), für Tagesdatenbank und Tagesauswertung. */
    fun rueckblick(tage: Int = 7, zeichenJeTag: Int = 2500): String {
        val ab = LocalDate.now().minusDays(tage.toLong())
        val liste = eintraege().filter { !it.first.isBefore(ab) }
        if (liste.isEmpty()) return "Kein Tagebucheintrag in den letzten $tage Tagen."
        return liste.joinToString("\n\n") { (tag, datei) -> "### " + lang(tag) + "\n" + datei.readText().trim().let { if (it.length > zeichenJeTag) it.take(zeichenJeTag) + " …" else it } }
    }

    private val verdichtet get() = File(context.filesDir, "tagebuch_verdichtet").apply { mkdirs() }
    private val kurz get() = File(context.filesDir, "tagebuch_kurz").apply { mkdirs() }

    /** Die Einträge ab [ab] (eingeschlossen), älteste zuerst, je Tag höchstens [zeichenJeTag] Zeichen; leer, wenn es keine gibt. */
    fun eintraegeAb(ab: LocalDate, zeichenJeTag: Int): String = eintraege().filter { !it.first.isBefore(ab) }.sortedBy { it.first }
        .joinToString("\n\n") { (tag, datei) -> "### " + lang(tag) + "\n" + datei.readText().trim().let { if (it.length > zeichenJeTag) it.take(zeichenJeTag) + " …" else it } }

    /** Das Datum des neuesten Eintrags. */
    fun neuesterEintrag(): LocalDate? = eintraege().firstOrNull()?.first

    /** Fürs Live-Gedächtnis: höchstens alle 20 Minuten frisch vom Server, dann [gedaechtnis]. */
    suspend fun gedaechtnisFrisch(): String { runCatching { frisch() }; return gedaechtnis() }

    /** Prüfsumme eines Textes: Ändert Frank einen alten Eintrag, passt die Kurzfassung nicht mehr und wird neu geschrieben. */
    private fun pruefsumme(text: String): String = java.security.MessageDigest.getInstance("SHA-256").digest(text.trim().toByteArray()).take(8).joinToString("") { "%02x".format(it) }

    /** Ab hier zählt ein Tag als älter: Die letzten [VOLL_TAGE] Tage (heute eingeschlossen) stehen vollständig im Gedächtnis. */
    private fun vollAb(heute: LocalDate = LocalDate.now()): LocalDate = heute.minusDays(VOLL_TAGE - 1L)

    /** Monate, die ganz vor dieser Grenze liegen, werden als Monat verdichtet; die Tage dazwischen stehen je Tag in Stichpunkten. */
    private fun monatsGrenze(heute: LocalDate = LocalDate.now()): YearMonth = YearMonth.from(heute.minusDays(MONAT_NACH_TAGEN))

    /**
     * Das Tagebuch als Gedächtnis fürs Gesamtbild, nach einer festen Kontextregel, damit es über die Jahre nicht anwächst:
     * die letzten 10 Tage vollständig (je Tag bis [VOLL_ZEICHEN]), davor je Tag eine Kurzfassung in Stichpunkten
     * (von Jarvis geschrieben, siehe `agent/Lernen.kt`; bis dahin der Anfang des Eintrags), und Monate, die ganz
     * vor den letzten fünf Wochen liegen, als Monats-Kurzfassung. Wird es trotzdem zu lang, fällt das Älteste weg.
     */
    fun gedaechtnis(): String {
        val alle = eintraege()
        if (alle.isEmpty()) return "Es liegen keine Tagebucheinträge vor."
        val heute = LocalDate.now()
        val ab = vollAb(heute)
        val monate = verdichtet.listFiles { f -> f.name.endsWith(".txt") }.orEmpty().sortedBy { it.name }
        val fertig = monate.map { it.name.take(7) }.toSet()
        val tage = alle.filter { YearMonth.from(it.first).toString() !in fertig || !it.first.isBefore(ab) }.sortedBy { it.first }
        // Feste Teilbudgets statt eines Schnitts von vorn: Das Langzeitgedächtnis (Monate) und die letzten 10 Tage
        // behalten immer ihren Platz. Reicht es nicht, fallen die ältesten Monate bzw. Stichpunkt-Tage weg, nie mitten im Text.
        val voll = tage.filter { !it.first.isBefore(ab) }.map { (tag, datei) ->
            "### " + lang(tag) + " (vollständig)\n" + datei.readText().trim().let { if (it.length > VOLL_ZEICHEN) it.take(VOLL_ZEICHEN) + " … (Rest mit tagebuch_lesen)" else it }
        }
        val rest = (GEDAECHTNIS_ZEICHEN - voll.sumOf { it.length + 2 }).coerceAtLeast(6_000)
        val monatsZeilen = neuesteZuerst(monate.map { it.name.take(7) + ": " + it.readText().trim().take(900) }, rest * 2 / 5)
        val stichpunkte = neuesteZuerst(tage.filter { it.first.isBefore(ab) }.map { (tag, datei) ->
            "### " + lang(tag) + ": " + (kurzfassung(tag, datei) ?: datei.readText().replace(Regex("\\s+"), " ").trim().let { if (it.length > 300) it.take(300) + " …" else it })
        }, rest - monatsZeilen.sumOf { it.length + 1 })
        return buildList {
            if (monatsZeilen.isNotEmpty()) add("Frühere Monate, von Jarvis verdichtet:\n" + monatsZeilen.joinToString("\n"))
            if (stichpunkte.isNotEmpty()) add("Ältere Tage in Stichpunkten:\n" + stichpunkte.joinToString("\n"))
            addAll(voll)
        }.joinToString("\n\n")
    }

    /** Von hinten (neueste) so viele Zeilen, wie in [budget] Zeichen passen, in der ursprünglichen Reihenfolge. */
    private fun neuesteZuerst(zeilen: List<String>, budget: Int): List<String> {
        var summe = 0
        return zeilen.asReversed().takeWhile { summe += it.length + 1; summe <= budget }.reversed()
    }

    /** Die gespeicherte Kurzfassung eines Tages, wenn sie zum aktuellen Text des Eintrags passt. */
    private fun kurzfassung(tag: LocalDate, datei: File): String? = runCatching {
        val j = JSONObject(File(kurz, "$tag.json").readText())
        j.optString("text").takeIf { it.isNotBlank() && j.optString("pruefsumme") == pruefsumme(datei.readText()) }
    }.getOrNull()

    /** Ältere Tage (vor den letzten 10), deren Kurzfassung fehlt oder nicht mehr zum Eintrag passt: Datum → Text. Neueste zuerst, höchstens [hoechstens]. */
    fun tageOhneKurzfassung(hoechstens: Int = 14): List<Pair<LocalDate, String>> {
        val ab = vollAb()
        val fertig = verdichtet.list().orEmpty().filter { it.endsWith(".txt") }.map { it.take(7) }.toSet()
        return eintraege().filter { it.first.isBefore(ab) && YearMonth.from(it.first).toString() !in fertig }
            .filter { (tag, datei) -> kurzfassung(tag, datei) == null }.sortedByDescending { it.first }.take(hoechstens)
            .map { (tag, datei) -> tag to datei.readText().trim().take(4000) }
    }

    fun speichereKurzfassung(tag: LocalDate, text: String) {
        val datei = eintraege().firstOrNull { it.first == tag }?.second ?: return
        val inhalt = JSONObject().put("text", text.trim().take(400)).put("pruefsumme", pruefsumme(datei.readText())).toString()
        File(kurz, "$tag.json").writeText(inhalt)
    }

    /**
     * Der älteste Monat, der ganz vor der Monatsgrenze liegt und noch keine (oder eine veraltete) Kurzfassung hat:
     * Monat (JJJJ-MM) und seine Einträge. Veraltet heißt: Ein Eintrag des Monats kam später dazu oder wurde geändert.
     */
    fun unverdichteterMonat(): Pair<String, String>? {
        val grenze = monatsGrenze()
        val monate = eintraege().groupBy { YearMonth.from(it.first) }.filterKeys { it.isBefore(grenze) }
        val treffer = monate.entries.sortedBy { it.key }.firstOrNull { (m, l) ->
            val summe = monatsSumme(l)
            val gespeichert = gespeicherteSumme(m.toString())
            // Übergang: Ältere Fassungen schrieben nur die Kurzfassung, ohne Prüfsumme. Dann die Summe des heutigen Inhalts
            // nachtragen und den Monat als fertig werten, statt die ganze Historie Stunde für Stunde neu zu verdichten.
            if (gespeichert == null && File(verdichtet, "$m.txt").exists()) {
                runCatching { File(verdichtet, "$m.pruefsumme").writeText(summe) }
                false
            } else summe != gespeichert
        } ?: return null
        return treffer.key.toString() to treffer.value.sortedBy { it.first }.joinToString("\n\n") { (tag, datei) -> "### " + lang(tag) + "\n" + datei.readText().trim().take(3000) }
    }

    /**
     * Die Prüfsumme eines Monats. Zwischengespeichert nach Anzahl, Änderungszeiten und Größen der Dateien: Solange sich
     * keine Datei ändert, wird nichts gelesen (sonst läse jeder Lernlauf das ganze Tagebuch).
     */
    private fun monatsSumme(liste: List<Pair<LocalDate, File>>): String {
        val sortiert = liste.sortedBy { it.first }
        val monat = sortiert.firstOrNull()?.let { YearMonth.from(it.first).toString() } ?: return pruefsumme("")
        val fingerabdruck = sortiert.joinToString("|") { it.first.toString() + ":" + it.second.lastModified() + ":" + it.second.length() }
        synchronized(SUMMEN) { SUMMEN[monat]?.takeIf { it.first == fingerabdruck }?.let { return it.second } }
        val summe = pruefsumme(sortiert.joinToString("\n") { it.first.toString() + it.second.readText().trim() })
        synchronized(SUMMEN) { SUMMEN[monat] = fingerabdruck to summe }
        return summe
    }
    private fun gespeicherteSumme(monat: String): String? = File(verdichtet, "$monat.pruefsumme").takeIf { it.exists() && File(verdichtet, "$monat.txt").exists() }?.readText()?.trim()

    /** Speichert die Kurzfassung eines Monats mit der Prüfsumme der Einträge, aus denen sie entstand. */
    fun speichereVerdichtung(monat: String, text: String, eintraegeText: String) {
        File(verdichtet, "$monat.txt").writeText(text.trim())
        val liste = eintraege().filter { YearMonth.from(it.first).toString() == monat }
        // Hat sich der Monat während des Schreibens geändert, bleibt die Summe der alten Fassung: Der nächste Lauf verdichtet ihn erneut.
        val jetzt = liste.sortedBy { it.first }.joinToString("\n\n") { (tag, datei) -> "### " + lang(tag) + "\n" + datei.readText().trim().take(3000) }
        if (jetzt == eintraegeText) File(verdichtet, "$monat.pruefsumme").writeText(monatsSumme(liste))
    }

    /**
     * Für gedaechtnis_suchen: Einträge, in denen alle [woerter] vorkommen, neueste zuerst, je Treffer das Datum und ein
     * Ausschnitt um die erste Fundstelle. Kein Netz, durchsucht den lokalen Bestand.
     */
    fun suche(woerter: List<String>, von: LocalDate?, bis: LocalDate?, hoechstens: Int): List<Pair<LocalDate, String>> =
        eintraege().asSequence()
            .filter { (tag, _) -> (von == null || !tag.isBefore(von)) && (bis == null || !tag.isAfter(bis)) }
            .mapNotNull { (tag, datei) -> datei.readText().let { t -> if (woerter.all { w -> t.contains(w, ignoreCase = true) }) tag to ausschnitt(t, woerter.firstOrNull().orEmpty()) else null } }
            .take(hoechstens).toList()

    /** Die verdichteten Monate, in denen alle Wörter vorkommen. */
    fun sucheMonate(woerter: List<String>): List<Pair<String, String>> = verdichtet.listFiles { f -> f.name.endsWith(".txt") }.orEmpty().sortedByDescending { it.name }
        .mapNotNull { f -> f.readText().let { t -> if (woerter.all { w -> t.contains(w, ignoreCase = true) }) f.name.take(7) to ausschnitt(t, woerter.firstOrNull().orEmpty()) else null } }

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "tagebuch_lesen",
            titel = "Tagebuch lesen",
            beschreibung = "Jarvis: liest Franks Tagebucheinträge im Volltext, für einen Tag oder einen Zeitraum. Nutze es für „Was habe ich gestern gemacht?“, " +
                "„Was war in den letzten Tagen los?“, „Was stand am 6. Oktober im Tagebuch?“. Ohne Angaben kommen die letzten 7 Tage. Mit suche nur Einträge, die das Wort enthalten.",
            schema = schema(
                "datum" to text("Ein einzelner Tag: heute, gestern, vorgestern oder JJJJ-MM-TT."),
                "von" to text("Erster Tag eines Zeitraums JJJJ-MM-TT."),
                "bis" to text("Letzter Tag eines Zeitraums JJJJ-MM-TT (Vorgabe: heute)."),
                "suche" to text("Wort oder Wortgruppe; durchsucht dann alle Einträge, wenn kein Zeitraum angegeben ist."),
            ),
            nurLesen = true,
        ) { a ->
            stoerung()?.let { return@Werkzeug Ergebnis(it, fehler = true) }
            frisch()
            val alle = eintraege()
            if (alle.isEmpty()) return@Werkzeug Ergebnis("Es liegen keine Tagebucheinträge vor. " + (stoerung() ?: "Server oder Drive sind gerade nicht erreichbar."), fehler = true)
            val heute = LocalDate.now()
            val suche = a.optString("suche").trim().lowercase(Locale.GERMAN)
            val einzeln = datum(a.optString("datum"))
            val bis = einzeln ?: datum(a.optString("bis")) ?: heute
            val von = einzeln ?: datum(a.optString("von")) ?: if (suche.isEmpty()) bis.minusDays(6) else LocalDate.MIN
            var treffer = alle.filter { !it.first.isBefore(von) && !it.first.isAfter(bis) }
            if (suche.isNotEmpty()) treffer = treffer.filter { suche in it.second.readText().lowercase(Locale.GERMAN) }
            if (treffer.isEmpty()) return@Werkzeug Ergebnis(
                "Kein Tagebucheintrag " + (if (einzeln != null) "für ${lang(einzeln)}" else "im Zeitraum") + (if (suche.isEmpty()) "" else " mit „$suche“") +
                    ". Vorhanden sind Einträge von ${lang(alle.last().first)} bis ${lang(alle.first().first)} (${alle.size} Tage).",
            )
            // Viele Tage auf einmal: je Eintrag kürzen, damit die Antwort handhabbar bleibt.
            val gezeigt = treffer.take(40)
            val jeTag = (60_000 / gezeigt.size).coerceIn(1500, 20_000)
            val kopf = if (treffer.size > gezeigt.size) "${treffer.size} Treffer, gezeigt die ${gezeigt.size} neuesten. Ältere reichen zurück bis ${lang(treffer.last().first)}; " +
                "für Älteres von/bis setzen. Alle Trefferdaten: ${treffer.joinToString(", ") { it.first.toString() }}.\n\n"
            else "${treffer.size} Tagebucheintrag/-einträge:\n\n"
            Ergebnis("Heute ist ${lang(heute)}. " + kopf + gezeigt.joinToString("\n\n") { (tag, datei) ->
                "### " + lang(tag) + "\n" + datei.readText().trim().let { if (it.length > jeTag) it.take(jeTag) + " … (gekürzt)" else it }
            })
        },
    )

    private fun datum(text: String): LocalDate? = when (text.trim().lowercase(Locale.GERMAN)) {
        "" -> null
        "heute" -> LocalDate.now()
        "gestern" -> LocalDate.now().minusDays(1)
        "vorgestern" -> LocalDate.now().minusDays(2)
        else -> runCatching { LocalDate.parse(text.trim().take(10)) }.getOrNull()
    }

    fun lang(tag: LocalDate): String = tag.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN))

    companion object {
        private const val GEDAECHTNIS_ZEICHEN = 34_000
        /** So viele Tage (heute eingeschlossen) stehen vollständig im Gedächtnis. */
        const val VOLL_TAGE = 10
        private const val VOLL_ZEICHEN = 2_500
        /** Ein Monat wird verdichtet, wenn er ganz vor diesem Abstand liegt; bis dahin steht jeder Tag als Kurzfassung da. */
        private const val MONAT_NACH_TAGEN = 35L

        /** Ein Ausschnitt um die erste Fundstelle, für Suchergebnisse. */
        fun ausschnitt(text: String, wort: String, breite: Int = 220): String {
            val flach = text.replace(Regex("\\s+"), " ").trim()
            val i = if (wort.isEmpty()) 0 else flach.indexOf(wort, ignoreCase = true).coerceAtLeast(0)
            val von = (i - breite).coerceAtLeast(0)
            val bis = (i + wort.length + breite).coerceAtMost(flach.length)
            return (if (von > 0) "… " else "") + flach.substring(von, bis) + (if (bis < flach.length) " …" else "")
        }
        /** Nur einfache Dateinamen: kein Pfad, keine Sonderzeichen, Endung .md. */
        private val NAME = Regex("[\\w.\\- äöüÄÖÜß]{1,120}\\.md")
        /** Monat → (Fingerabdruck der Dateien, Prüfsumme); gilt für alle Instanzen. */
        private val SUMMEN = HashMap<String, Pair<String, String>>()
        private val CLIENT = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(110, TimeUnit.SECONDS).build()
    }
}

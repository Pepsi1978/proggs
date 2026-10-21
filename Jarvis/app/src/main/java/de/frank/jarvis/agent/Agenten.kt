package de.frank.jarvis.agent

import android.content.Context
import android.util.Log
import de.frank.jarvis.ablage.AblageZentrale
import de.frank.jarvis.ablage.Art
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Ein Agent: ein Name und eine Rolle, also die Anweisung, wie er an Aufträge herangeht. Kein eigenes Schrittlimit: Jeder Agent
 * nutzt Franks Einstellung („Schrittlimit pro Anfrage“), das Zeitlimit ist [Agenten.ZEITLIMIT_MINUTEN] je Zeitfenster.
 */
data class AgentenBauplan(val name: String, val rolle: String, val vorgegeben: Boolean = false)

/**
 * Ergebnis eines Agentenlaufs: der Titel seines Eintrags (in der Ablage oder, ohne Ablage-Wunsch, vorläufig) oder null, wenn kein
 * Bericht entstand, dazu der Grund, wenn er nicht (ganz) fertig wurde. [bericht] = der Berichtstext, [dateien] = absolute Pfade
 * der Bilder und PDFs zum Bericht (für die Anzeige im Gespräch).
 */
data class AgentenErgebnis(val titel: String?, val grund: String?, val bericht: String? = null, val dateien: List<String> = emptyList(), val vorlaeufig: Boolean = false,
    /** Nach Zeitablauf auf Franks Wunsch (oder ohne Antwort) beendet; [bericht] ist dann ein Zwischenstand. */
    val abgebrochen: Boolean = false)

/** [wartet] = Kennung, unter der der Lauf nach Zeitablauf auf Franks Entscheidung wartet ([Agenten.entscheide]); null = er arbeitet. */
data class AgentenLauf(val agent: String, val auftrag: String, val seit: Long, val schritt: String = "", val id: Long = 0L, val wartet: String? = null)

/**
 * Die Agenten von Jarvis. Ein Agent ist das eigene Modell von Jarvis mit einer festen Rolle; er arbeitet einen
 * Auftrag selbstständig in mehreren Schritten ab (Werkzeuge der angebundenen Apps, Internet-Suche, Bilder) und liefert
 * seinen Bericht ins Gespräch zurück. In die Ablage legt er nur, wenn Frank das ausdrücklich verlangt hat (Vorgang.ablage);
 * sonst liegen Bericht, Bilder und PDF vorläufig ([de.frank.jarvis.ablage.Vorlaeufig]). Jarvis kann neue Agenten anlegen: Es genügt eine Rolle in Worten.
 *
 * Ein Lauf dauert Minuten und läuft deshalb im Hintergrund über den Dienst, nie innerhalb eines Plugin-Aufrufs.
 */
object Agenten {
    private const val TAG = "JarvisAgenten"
    private val _laeufe = MutableStateFlow<List<AgentenLauf>>(emptyList())
    val laeufe: StateFlow<List<AgentenLauf>> = _laeufe.asStateFlow()
    /** Eindeutige Nummer je Lauf: Zwei Läufe aus derselben Millisekunde dürfen sich nicht gegenseitig austragen. */
    private val naechsteId = java.util.concurrent.atomic.AtomicLong()

    /** So viele Agenten arbeiten höchstens gleichzeitig. Ein Lauf, der nach Zeitablauf auf Franks Entscheidung wartet, behält seinen Platz. */
    const val HOECHSTENS = 2
    /** Internes Zeitlimit je Zeitfenster. Danach pausiert der Agent und fragt Frank: abbrechen oder weitere so viele Minuten? */
    const val ZEITLIMIT_MINUTEN = 30
    /** So lange wartet ein pausierter Agent auf Franks Antwort; danach schreibt er seinen Zwischenstand und gibt den Platz frei. */
    const val ANTWORT_MS = 8 * 3_600_000L
    /** Offene Zeitablauf-Fragen je Kennung (Vorgang oder Lauf): true = weiterarbeiten, false = abbrechen. */
    private val entscheidungen = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.CompletableDeferred<Boolean>>()

    /** Franks Antwort auf „Abbrechen oder weiterarbeiten?“. false = unter dieser Kennung wartet gerade kein Lauf. */
    fun entscheide(kennung: String, weiter: Boolean): Boolean = entscheidungen[kennung.trim().uppercase(Locale.ROOT)]?.complete(weiter) ?: false

    /** Wartet unter [kennung] gerade ein Lauf nach Zeitablauf auf Franks Entscheidung? */
    fun wartet(kennung: String): Boolean = entscheidungen.containsKey(kennung.trim().uppercase(Locale.ROOT))
    /** Ein vorgemerkter Start: Agent, Auftrag und Zeitpunkt. */
    private class Vormerkung(val agent: String, val auftrag: String, val seit: Long)
    /** Gestartet, aber noch nicht in [laeufe]: Der Dienst beginnt den Lauf erst asynchron. Geschützt durch [platz]. */
    private val vorgemerkt = ArrayList<Vormerkung>()
    private val platz = Any()
    /** Kommt ein vorgemerkter Lauf so lange nicht an (Dienst startete nicht, Agent inzwischen gelöscht), verfällt der Platz. */
    private const val VORMERKUNG_MS = 2 * 60_000L

    /**
     * Reserviert synchron einen Platz für einen Lauf, bevor er asynchron über den Dienst startet. Sonst sähen zwei
     * gleichzeitige Aufrufe beide noch einen freien Platz und es liefen mehr als [HOECHSTENS] Agenten.
     * false = kein Platz frei. Der Platz geht an den Lauf über ([fuehreAus]), mit [gibFrei] zurück oder verfällt.
     */
    fun reserviere(agent: String, auftrag: String): Boolean = synchronized(platz) {
        val jetzt = System.currentTimeMillis()
        vorgemerkt.removeAll { jetzt - it.seit > VORMERKUNG_MS }
        if (_laeufe.value.size + vorgemerkt.size >= HOECHSTENS) return@synchronized false
        vorgemerkt += Vormerkung(agent, auftrag, jetzt)
        true
    }

    /** Gibt einen reservierten Platz zurück, dessen Start nicht zustande kam. */
    fun gibFrei(agent: String, auftrag: String) {
        synchronized(platz) { nimmVormerkung(agent, auftrag) }
    }

    /** Nur unter [platz] aufrufen. */
    private fun nimmVormerkung(agent: String, auftrag: String) {
        vorgemerkt.indexOfFirst { it.agent.equals(agent, ignoreCase = true) && it.auftrag.trim() == auftrag.trim() }.takeIf { it >= 0 }?.let { vorgemerkt.removeAt(it) }
    }

    private val VORGABEN = listOf(
        AgentenBauplan(
            "Recherche",
            "Du bist ein gründlicher Recherche-Agent. Kläre zuerst in Gedanken, welche Teilfragen der Auftrag enthält. Suche dann mit mehreren unterschiedlichen Anfragen im Internet, " +
                "lies die wichtigsten Seiten selbst nach und prüfe Angaben gegeneinander. Schreibe am Ende einen gegliederten Bericht: Kurzfassung in drei bis fünf Sätzen, " +
                "dann die Ergebnisse nach Teilfragen, dann eine klare Empfehlung, dann offene Punkte und die Quellen mit Adresse. Trenne Belegtes von Vermutetem.",
            vorgegeben = true,
        ),
        AgentenBauplan(
            "Machbarkeit",
            "Du prüfst, wie sich eine Idee umsetzen lässt. Lies zuerst die Idee (falls sie in Geniale Ideen steht) und kläre Ziel und Nutzen. Recherchiere dann, welche Wege es gibt, " +
                "was sie kosten, was schon existiert und wo die Risiken liegen. Schreibe einen Bericht: Kurzfassung, Umsetzungswege im Vergleich, empfohlener Weg mit den ersten drei Schritten, " +
                "Aufwand grob geschätzt, Risiken, Quellen.",
            vorgegeben = true,
        ),
        AgentenBauplan(
            "Code-Analyse",
            "Du wertest den Code in Franks Repo auf GitHub aus; du änderst dort nichts. Vorgehen: Finde mit repo_lesen das Projekt (projekte, dateien), lies README.md, CLAUDE.md oder AGENTS.md und " +
                "versionslog.json, dann die Dateien, um die es im Auftrag geht, samt der Stellen, die sie benutzen (suche). Behaupte nur, was du im Code gelesen hast, und nenne Datei und Stelle. " +
                "Dein Bericht: Kurzfassung in drei bis fünf Sätzen; der Stand der App oder des Bereichs (was gebaut ist, wie es zusammenhängt, was fehlt oder auffällt); danach unter der Überschrift " +
                "„Auftrag für das Programmier-Werkzeug“ ein fertiger, eigenständiger Auftrag, den Frank unverändert an Claude Code oder Codex geben kann: Projekt, Ziel, betroffene Dateien mit Pfad und " +
                "jetziger Stelle, was genau anders werden soll, Randfälle, woran man erkennt, dass es fertig ist. Den Auftrag nur schreiben, wenn Frank eine Änderung will oder dir etwas Behebenswertes auffällt.",
            vorgegeben = true,
        ),
    )

    private fun ordner(context: Context) = File(context.filesDir, "agenten").apply { mkdirs() }
    private fun datei(context: Context, name: String) = File(ordner(context), name.lowercase(Locale.GERMAN).replace(Regex("[^a-z0-9äöüß]+"), "-").trim('-') + ".json")

    fun alle(context: Context): List<AgentenBauplan> {
        val eigene = ordner(context).listFiles { f -> f.name.endsWith(".json") }?.mapNotNull { f ->
            runCatching { JSONObject(f.readText()).let { AgentenBauplan(it.getString("name"), it.getString("rolle")) } }.getOrNull()
        }.orEmpty()
        // Ein eigener Agent mit dem Namen einer Vorgabe ersetzt sie.
        return VORGABEN.filter { v -> eigene.none { it.name.equals(v.name, ignoreCase = true) } } + eigene.sortedBy { it.name.lowercase(Locale.GERMAN) }
    }

    fun finde(context: Context, name: String): AgentenBauplan? {
        val gesucht = name.trim().lowercase(Locale.GERMAN)
        val liste = alle(context)
        return liste.firstOrNull { it.name.lowercase(Locale.GERMAN) == gesucht } ?: liste.filter { gesucht in it.name.lowercase(Locale.GERMAN) }.singleOrNull()
    }

    fun speichere(context: Context, name: String, rolle: String): AgentenBauplan {
        val plan = AgentenBauplan(name.trim().take(40), rolle.trim())
        datei(context, plan.name).writeText(JSONObject().put("name", plan.name).put("rolle", plan.rolle).toString())
        return plan
    }

    /** Löscht einen selbst angelegten Agenten. Vorgaben bleiben. */
    fun loesche(context: Context, name: String): Boolean = datei(context, name).let { it.exists() && it.delete() }

    /**
     * Nur bei ausdrücklichem PDF-Wunsch: Jarvis setzt aus dem Bericht ein gestaltetes Dokument (Titelseite, Inhaltsübersicht, Typografie,
     * Merkkästen, Tabellen, eingebettete Abbildungen). Der Agent liefert dafür Gliederung und passende Bilder.
     */
    private const val PDF_ANWEISUNG =
        "Frank hat ausdrücklich ein PDF verlangt. Jarvis setzt aus deinem Bericht ein professionell gestaltetes Dokument: Titelseite mit Titelbild und Inhaltsübersicht, " +
            "klare Typografie, farbige Merkkästen, Tabellen und eingebettete Abbildungen. Rufe dafür NICHT ablage_schreiben auf und setze bei bild_erzeugen kein auch_pdf. So lieferst du zu:\n" +
            "1. Beginne den Bericht mit einer Zeile „# Dokumenttitel“ (kurz, ohne Datum) und danach „## Kurzfassung“.\n" +
            "2. Erzeuge mit bild_erzeugen (Titel wie oben, je Bild ein Aufruf; kommt „läuft noch“, rufe es mit denselben Angaben noch einmal auf) zwei bis vier Abbildungen, " +
            "die den Inhalt verständlicher machen: ein Titelbild (format quer, ruhige, hochwertige Illustration zum Thema, ohne Text) und je nach Inhalt Infografiken, " +
            "Diagramme, Ablauf- oder Vergleichsgrafiken mit deutscher Beschriftung. Zahlen und Aussagen in Grafiken müssen mit deinem Bericht übereinstimmen; nichts nur zur Dekoration. " +
            "Beschreibe jede Grafik bei bild_erzeugen genau: Inhalt, Beschriftungen, Werte, Stil (sauber, modern, viel Weißraum, Farben Dunkelblau und Petrol).\n" +
            "3. Binde das Titelbild als eigene Zeile ![Titelbild](dateiname.png) direkt nach dem Dokumenttitel ein und jede weitere Abbildung an der inhaltlich passenden Stelle " +
            "als eigene Zeile ![Was die Abbildung zeigt](dateiname.png), mit genau dem Dateinamen aus dem Werkzeug-Ergebnis.\n" +
            "4. Gestaltungsmittel: ## für Kapitel, ### für Unterkapitel, Stichpunkte und nummerierte Listen, Tabellen in Markdown für Vergleiche, " +
            "Merkkästen als Zeilen mit „> “ davor (zum Beispiel „> **Empfehlung:** …“), **fett** für Schlüsselbegriffe.\n" +
            "5. Prüfe vor deiner Antwort mit pdf_entwurf_pruefen (inhalt = dein fertiger Bericht), ob alle Abbildungen gefunden werden und das Layout stimmt; behebe gemeldete Probleme."

    /** Laufende Agenten je Vorgangskennung, damit vorgaenge (aktion abbrechen) einen Lauf beenden kann. */
    private val jobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    fun merkeJob(vorgang: String, job: kotlinx.coroutines.Job) {
        jobs[vorgang] = job
        job.invokeOnCompletion { jobs.remove(vorgang, job) }
    }

    /**
     * Bricht den Lauf zu [vorgang] ab. false = es läuft gerade keiner dazu. Wartet er nach Zeitablauf auf Franks Entscheidung,
     * gilt das als „Abbrechen“: Er schreibt noch seinen Zwischenstand, statt alles zu verwerfen.
     */
    fun brichAb(vorgang: String): Boolean = entscheide(vorgang, weiter = false) ||
        (jobs[vorgang]?.let { it.cancel(kotlinx.coroutines.CancellationException("von Frank abgebrochen")); true } ?: false)

    /**
     * Führt einen Auftrag aus. Der Bericht kommt als Text ins Gespräch; ein gestaltetes PDF nur, wenn der Vorgang es ausdrücklich
     * verlangt ([Vorgang.pdf], nie aus einer Wortsuche im Auftrag). Das Ergebnis kommt nur in die Ablage, wenn der
     * Vorgang es verlangt ([Vorgang.ablage]), sonst vorläufig. Beides ist unabhängig voneinander. [vorgang] = Kennung im [Vorgaenge]-Buch; Start, Schritte und Ende
     * werden dort festgehalten. [beiFrage] meldet Frank nach Zeitablauf Kennung und Statusbericht (Benachrichtigung, Gespräch),
     * [nachEntscheidung] bekommt seine Antwort (true = weiterarbeiten), bevor der Lauf weitergeht.
     */
    suspend fun fuehreAus(
        context: Context, plan: AgentenBauplan, auftrag: String, vorgang: String? = null,
        beiFrage: (kennung: String, bericht: String) -> Unit = { _, _ -> }, nachEntscheidung: (kennung: String, weiter: Boolean) -> Unit = { _, _ -> },
    ): AgentenErgebnis {
        val app = context.applicationContext
        val lauf = AgentenLauf(plan.name, auftrag, System.currentTimeMillis(), id = naechsteId.incrementAndGet())
        val buch = vorgang?.let { Vorgaenge.buch(app) }
        // Atomar ändern: Zwei Agenten laufen parallel, sonst schreibt einer den alten Stand des anderen zurück.
        // Zusammen mit der Vormerkung: Der Platz wechselt vom vorgemerkten zum laufenden Agenten, ohne dass reserviere() ihn doppelt zählt oder übersieht.
        synchronized(platz) {
            nimmVormerkung(plan.name, auftrag)
            _laeufe.update { it + lauf }
        }
        // Das Ende steht im Buch, bevor der Lauf aus laeufe verschwindet.
        var ende: Pair<String, AgentenErgebnis>? = null
        try {
            if (vorgang != null) {
                // Wurde der Vorgang abgebrochen, während der Dienst ihn startete, gar nicht erst anfangen.
                if (buch?.gestartet(vorgang)?.offen == false) throw kotlinx.coroutines.CancellationException("vor dem Anlauf abgebrochen")
            }
            val eintrag = vorgang?.let { buch?.finde(it) }
            val ablegen = eintrag?.ablage ?: false
            val pdf = eintrag?.pdf ?: false
            val kennung = (vorgang ?: "L${lauf.id}").uppercase(Locale.ROOT)
            val beiZeitablauf: suspend (Zeitablauf) -> String? = { stand -> frageNachZeitablauf(plan, lauf, kennung, vorgang?.let { v -> buch?.let { v to it } }, stand, beiFrage, nachEntscheidung) }
            val r = fuehreAusIntern(app, plan, auftrag, lauf, ablegen, pdf, beiZeitablauf) { schritt -> if (vorgang != null) runCatching { buch?.schritt(vorgang, schritt) } }
            // Ein Abbruch während des Abschluss-Schritts wird dort verschluckt; dann zählt er trotzdem als Abbruch.
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            return r.also { ende = (when { r.abgebrochen -> Vorgang.ABGEBROCHEN; r.titel == null -> Vorgang.FEHLGESCHLAGEN; else -> Vorgang.FERTIG }) to r }
        } catch (e: kotlinx.coroutines.CancellationException) {
            ende = Vorgang.ABGEBROCHEN to AgentenErgebnis(null, e.message ?: "abgebrochen")
            Protokoll.melde(Quelle.JARVIS, "Agent ${plan.name}", "Abgebrochen: ${auftrag.take(120)}", ok = false)
            throw e
        } finally {
            if (vorgang != null) {
                val (stand, r) = ende ?: (Vorgang.FEHLGESCHLAGEN to AgentenErgebnis(null, "unerwartet beendet"))
                runCatching { buch?.beende(vorgang, stand, r.titel?.takeIf { !r.vorlaeufig }, r.grund, r.titel?.takeIf { r.vorlaeufig }) }
            }
            _laeufe.update { l -> l.filter { it.id != lauf.id } }
        }
    }

    /**
     * Zeitfenster abgelaufen: Zustand anzeigen (Startseite, Vorgangsbuch, über [beiFrage] Benachrichtigung und Gespräch) und auf Franks
     * Entscheidung warten. null = weiterarbeiten, sonst der Grund fürs Ende (dann schreibt der Agent seinen Zwischenstand).
     */
    private suspend fun frageNachZeitablauf(
        plan: AgentenBauplan, lauf: AgentenLauf, kennung: String, imBuch: Pair<String, VorgangsBuch>?, stand: Zeitablauf,
        beiFrage: (String, String) -> Unit, nachEntscheidung: (String, Boolean) -> Unit,
    ): String? {
        val antwort = kotlinx.coroutines.CompletableDeferred<Boolean>()
        entscheidungen[kennung] = antwort
        var weiter: Boolean? = null
        try {
            _laeufe.update { l -> l.map { if (it.id == lauf.id) it.copy(schritt = stand.kurz(), wartet = kennung) else it } }
            imBuch?.let { (v, buch) -> runCatching { buch.rueckfrage(v, stand.kurz()) } }
            Protokoll.melde(Quelle.JARVIS, "Agent ${plan.name}", "Zeitlimit erreicht, wartet auf Franks Entscheidung: ${stand.kurz()}", ok = true)
            runCatching { beiFrage(kennung, stand.bericht(plan.name)) }.onFailure { Log.w(TAG, "Zeitablauf-Frage nicht zugestellt", it) }
            weiter = kotlinx.coroutines.withTimeoutOrNull(ANTWORT_MS) { antwort.await() }
            runCatching { nachEntscheidung(kennung, weiter == true) }
            return when (weiter) {
                true -> {
                    imBuch?.let { (v, buch) -> runCatching { buch.weiter(v) } }
                    Protokoll.melde(Quelle.JARVIS, "Agent ${plan.name}", "Frank: weiterarbeiten, weitere $ZEITLIMIT_MINUTEN Minuten", ok = true)
                    null
                }
                false -> "Nach Ablauf des internen Zeitlimits (${stand.minuten} Minuten) von Frank abgebrochen"
                null -> "Nach Ablauf des internen Zeitlimits (${stand.minuten} Minuten) kam ${ANTWORT_MS / 3_600_000} Stunden lang keine Entscheidung; beendet"
            }
        } finally {
            entscheidungen.remove(kennung, antwort)
            _laeufe.update { l -> l.map { if (it.id == lauf.id) it.copy(wartet = null, schritt = if (weiter == true) "arbeitet weiter" else "schreibt den Zwischenstand") else it } }
        }
    }

    private suspend fun fuehreAusIntern(
        app: Context, plan: AgentenBauplan, auftrag: String, lauf: AgentenLauf, ablegen: Boolean, pdf: Boolean,
        beiZeitablauf: suspend (Zeitablauf) -> String?, beiSchritt: (String) -> Unit,
    ): AgentenErgebnis {
        // Der Titel steht vorab fest: Dateien, die der Agent unterwegs erzeugt (Bilder, PDFs, Downloads), landen im selben Eintrag wie sein Bericht.
        val titel = "${plan.name} – ${auftrag.replace(Regex("\\s+"), " ").take(50).trim()} – ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("d.M. HH.mm"))}"
        // Ohne ausdrücklichen Ablage-Wunsch liegen Bericht, Bilder und PDF vorläufig: Sie kommen ins Gespräch, nicht in Franks Ablage.
        val speicher = if (ablegen) AblageZentrale.speicher(app) else de.frank.jarvis.ablage.Vorlaeufig.speicher(app)
        try {
            val rolle = "DEINE ROLLE ALS AGENT „${plan.name}“: ${plan.rolle}\n" +
                "Arbeite den Auftrag selbstständig ab, ohne Rückfragen. Dein letzter Schritt ist die Antwort mit dem vollständigen Bericht als Text; " +
                "der Bericht geht an Frank ins Gespräch und erscheint dort wie eine normale Chatantwort. Er darf und soll so ausführlich sein, wie der Auftrag es verlangt: " +
                "alle Ergebnisse vollständig, keine knappe Zusammenfassung mit Verweis auf eine Datei. Schreibe ihn in gutem Deutsch, übersichtlich gegliedert mit " +
                "Überschriften (##), kurzen Absätzen, Stichpunkten, **fett** für Schlüsselbegriffe und, wo es hilft, Tabellen in Markdown.\n" +
                (if (ablegen) "Frank will das Ergebnis in der Ablage: Dein Ablage-Eintrag heißt „$titel“. Dateien (bild_erzeugen, ablage_datei_speichern) legst du mit genau diesem Titel ab, " +
                    "dann gehören sie zu deinem Bericht. Nenne im Bericht nur Dateien, deren Speicherung ein Werkzeug-Ergebnis bestätigt hat.\n"
                else "Frank hat KEINE Ablage verlangt: Lege nichts in die Ablage (ablage_schreiben und ablage_datei_speichern sind gesperrt) und schreib nicht, etwas liege in der Ablage. " +
                    "Dein Ergebnis heißt „$titel“; Bilder aus bild_erzeugen mit genau diesem Titel gehören zu deinem Bericht und erscheinen mit ihm im Gespräch. " +
                    "Nenne im Bericht nur Bilder, deren Erzeugung ein Werkzeug-Ergebnis bestätigt hat; Dateien aus dem Internet nennst du mit ihrer Adresse.\n") +
                (if (pdf) PDF_ANWEISUNG
                else "Frank hat KEIN PDF verlangt: Das Ergebnis ist allein dein Bericht als Chattext. Erzeuge kein PDF und keine andere Datei (bei bild_erzeugen nie auch_pdf, " +
                    "kein ablage_schreiben mit als_pdf) und schreib nicht, es gebe ein PDF. Steht im Auftrag „kein PDF“ oder „nur Text“, gilt das erst recht. " +
                    "Bilder nur, wenn der Auftrag ausdrücklich Bilder oder Illustrationen will: dann mit bild_erzeugen (je Bild ein Aufruf, Titel wie oben; kommt „läuft noch“, " +
                    "rufe es mit denselben Angaben noch einmal auf), höchstens vier, und setze jedes als eigene Zeile ![Bildunterschrift](dateiname.png) in den Bericht.")
            val denker = JarvisAgent(app)
            var grund: String? = null
            var nachZeitablaufBeendet = false
            // Franks Schrittlimit (Einstellungen → Jarvis) gilt auch hier, 0 = unbegrenzt; kein eigenes festes Agenten-Limit.
            val schrittlimit = de.frank.jarvis.data.Einstellungen.get(app).schrittlimit
            // Ohne PDF-Wunsch sperrt PdfWunsch jedes PDF in bild_erzeugen und ablage_schreiben, auch wenn das Modell es versucht.
            val bericht = withContext(de.frank.jarvis.ablage.PdfWunsch(pdf)) { denker.versuche(
                auftrag = auftrag, zeitlimitMs = ZEITLIMIT_MINUTEN * 60_000L, maxSchritte = schrittlimit, rolle = rolle, mitInternet = true, ablageTitel = titel, ablageDauerhaft = ablegen, abschluss = true,
                beiSchritt = { schritt -> _laeufe.update { l -> l.map { if (it.id == lauf.id) it.copy(schritt = schritt) else it } }; beiSchritt(schritt) },
                beiAbbruch = { grund = it },
                beiZeitablauf = { stand -> beiZeitablauf(stand).also { if (it != null) nachZeitablaufBeendet = true } },
            ) }
            if (bericht.isNullOrBlank()) {
                Protokoll.melde(Quelle.JARVIS, "Agent ${plan.name}", "Kein Ergebnis (${grund ?: "leere Antwort"}) für: ${auftrag.take(120)}", ok = false)
                return AgentenErgebnis(null, grund ?: "Das Modell hat keinen Bericht geliefert", abgebrochen = nachZeitablaufBeendet)
            }
            val datei = speicher.schreibeText(titel, "# ${plan.name}: ${auftrag.trim()}\n\nErstellt am ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm 'Uhr'"))} von Jarvis.\n\n$bericht").second
            var pdfMeldung = ""
            var pdfFehler: String? = null
            // Hat jedes Bild des Eintrags schon sein eigenes PDF (bild_erzeugen mit auch_pdf), ist der PDF-Wunsch erfüllt:
            // Ein zweites PDF aus dem Bericht zeigte dasselbe Motiv noch einmal (doppelte Infografik). Bei mehreren Bildern,
            // von denen nur manche ein eigenes PDF haben, ist das Bericht-PDF gemeint und kommt wie bisher.
            fun bildPdfGenuegt(): Boolean {
                val anhaenge = speicher.findeGenau(titel)?.anhaenge.orEmpty()
                val bilder = anhaenge.filter { it.art == Art.BILD }
                return bilder.isNotEmpty() && bilder.all { b -> b.kennung != null && anhaenge.any { it.kennung == "${b.kennung}-pdf" && it.herkunft == AblageZentrale.HERKUNFT_BILD_PDF } }
            }
            if (pdf && !bildPdfGenuegt()) {
                pdfMeldung = runCatching { " " + AblageZentrale.textAlsPdfAblegen(app, titel, bericht, speicher, "${plan.name} · erstellt von Jarvis") }
                    .getOrElse { pdfFehler = "das PDF ließ sich nicht erzeugen: ${AblageZentrale.fehlerText(it)}"; " Das PDF fehlt ($pdfFehler)." }
            }
            // Fehlt das gewünschte PDF, gilt der Lauf nicht als fertig: Grund geht in Protokoll und Benachrichtigung.
            val gesamt = listOfNotNull(grund, pdfFehler).joinToString("; ").ifEmpty { null }
            Protokoll.melde(Quelle.JARVIS, "Agent ${plan.name}", (if (gesamt == null) "Fertig" else "Nicht vollständig ($gesamt)") +
                (if (ablegen) ", abgelegt als „${datei.nameWithoutExtension}“." else ", Ergebnis „${datei.nameWithoutExtension}“ vorläufig (keine Ablage gewünscht).") + pdfMeldung, ok = gesamt == null)
            val eintrag = speicher.findeGenau(titel)
            val dateien = eintrag?.anhaenge.orEmpty().filter { it.art == Art.BILD || it.art == Art.PDF }.map { speicher.datei(it).absolutePath }
            return AgentenErgebnis(eintrag?.titel ?: datei.nameWithoutExtension, gesamt, bericht, dateien, vorlaeufig = !ablegen, abgebrochen = nachZeitablaufBeendet)
        } catch (e: Exception) {
            // Ein Abbruch ist kein Fehler des Agenten: weiterreichen, fuehreAus trägt ihn als abgebrochen ein.
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Agent fehlgeschlagen", e)
            val text = AblageZentrale.fehlerText(e)
            Protokoll.melde(Quelle.JARVIS, "Agent ${plan.name}", "Fehlgeschlagen: $text", ok = false)
            return AgentenErgebnis(null, text)
        }
    }
}

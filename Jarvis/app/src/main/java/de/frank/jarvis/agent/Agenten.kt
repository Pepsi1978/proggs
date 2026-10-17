package de.frank.jarvis.agent

import android.content.Context
import android.util.Log
import de.frank.jarvis.ablage.AblageZentrale
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.Ablage
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

/** Ein Agent: ein Name und eine Rolle, also die Anweisung, wie er an Aufträge herangeht. */
// Recherche mit Bildern und PDF braucht viele Schritte: Suchen, Seiten lesen, je Bild ein bis zwei Aufrufe. 14 Schritte und 12 Minuten reichten dafür nicht.
data class AgentenBauplan(val name: String, val rolle: String, val vorgegeben: Boolean = false, val schritte: Int = 30, val minuten: Int = 25)

/** Ergebnis eines Agentenlaufs: der Titel des Ablage-Eintrags oder null, dazu der Grund, wenn er nicht (ganz) fertig wurde. */
data class AgentenErgebnis(val titel: String?, val grund: String?)

data class AgentenLauf(val agent: String, val auftrag: String, val seit: Long, val schritt: String = "", val id: Long = 0L)

/**
 * Die Agenten von Jarvis. Ein Agent ist das eigene Modell von Jarvis mit einer festen Rolle; er arbeitet einen
 * Auftrag selbstständig in mehreren Schritten ab (Werkzeuge der angebundenen Apps, Internet-Suche, Ablage) und
 * legt sein Ergebnis als Datei in die Ablage. Jarvis kann neue Agenten anlegen: Es genügt eine Rolle in Worten.
 *
 * Ein Lauf dauert Minuten und läuft deshalb im Hintergrund über den Dienst, nie innerhalb eines Plugin-Aufrufs.
 */
object Agenten {
    private const val TAG = "JarvisAgenten"
    private val _laeufe = MutableStateFlow<List<AgentenLauf>>(emptyList())
    val laeufe: StateFlow<List<AgentenLauf>> = _laeufe.asStateFlow()
    /** Eindeutige Nummer je Lauf: Zwei Läufe aus derselben Millisekunde dürfen sich nicht gegenseitig austragen. */
    private val naechsteId = java.util.concurrent.atomic.AtomicLong()

    /** So viele Agenten arbeiten höchstens gleichzeitig. */
    const val HOECHSTENS = 2
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

    private val PDF_WUNSCH = Regex("\\bpdf", RegexOption.IGNORE_CASE)

    /** Laufende Agenten je Vorgangskennung, damit vorgaenge (aktion abbrechen) einen Lauf beenden kann. */
    private val jobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()

    fun merkeJob(vorgang: String, job: kotlinx.coroutines.Job) {
        jobs[vorgang] = job
        job.invokeOnCompletion { jobs.remove(vorgang, job) }
    }

    /** Bricht den Lauf zu [vorgang] ab. false = es läuft gerade keiner dazu. */
    fun brichAb(vorgang: String): Boolean = jobs[vorgang]?.let { it.cancel(kotlinx.coroutines.CancellationException("von Frank abgebrochen")); true } ?: false

    /**
     * Führt einen Auftrag aus und legt das Ergebnis in die Ablage (auf Wunsch auch als PDF mit den erzeugten Bildern).
     * [vorgang] = Kennung im [Vorgaenge]-Buch; Start, Schritte und Ende werden dort festgehalten.
     */
    suspend fun fuehreAus(context: Context, plan: AgentenBauplan, auftrag: String, vorgang: String? = null): AgentenErgebnis {
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
            val r = fuehreAusIntern(app, plan, auftrag, lauf) { schritt -> if (vorgang != null) runCatching { buch?.schritt(vorgang, schritt) } }
            // Ein Abbruch während des Abschluss-Schritts wird dort verschluckt; dann zählt er trotzdem als Abbruch.
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            return r.also { ende = (if (r.titel == null) Vorgang.FEHLGESCHLAGEN else Vorgang.FERTIG) to r }
        } catch (e: kotlinx.coroutines.CancellationException) {
            ende = Vorgang.ABGEBROCHEN to AgentenErgebnis(null, e.message ?: "abgebrochen")
            Protokoll.melde(Quelle.JARVIS, "Agent ${plan.name}", "Abgebrochen: ${auftrag.take(120)}", ok = false)
            throw e
        } finally {
            if (vorgang != null) {
                val (stand, r) = ende ?: (Vorgang.FEHLGESCHLAGEN to AgentenErgebnis(null, "unerwartet beendet"))
                runCatching { buch?.beende(vorgang, stand, r.titel, r.grund) }
            }
            _laeufe.update { l -> l.filter { it.id != lauf.id } }
        }
    }

    private suspend fun fuehreAusIntern(app: Context, plan: AgentenBauplan, auftrag: String, lauf: AgentenLauf, beiSchritt: (String) -> Unit): AgentenErgebnis {
        // Der Titel steht vorab fest: Dateien, die der Agent unterwegs erzeugt (Bilder, PDFs, Downloads), landen im selben Eintrag wie sein Bericht.
        val titel = "${plan.name} – ${auftrag.replace(Regex("\\s+"), " ").take(50).trim()} – ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("d.M. HH.mm"))}"
        try {
            val rolle = "DEINE ROLLE ALS AGENT „${plan.name}“: ${plan.rolle}\n" +
                "Arbeite den Auftrag selbstständig ab, ohne Rückfragen. Dein letzter Schritt ist die Antwort mit dem vollständigen Bericht als Text; " +
                "der Bericht wird als Datei gespeichert und kann länger sein. Schreibe ihn in gutem Deutsch mit Überschriften.\n" +
                "Dein Ablage-Eintrag heißt „$titel“. Dateien (bild_erzeugen, ablage_datei_speichern) legst du mit genau diesem Titel ab, " +
                "dann gehören sie zu deinem Bericht. Nenne im Bericht nur Dateien, deren Speicherung ein Werkzeug-Ergebnis bestätigt hat.\n" +
                "Sollen Bilder oder Illustrationen dazu, erzeuge sie mit bild_erzeugen (je Bild ein Aufruf, Titel wie oben; kommt „läuft noch“, rufe es mit denselben Angaben noch einmal auf). " +
                "Plane höchstens vier Bilder, damit Zeit für den Bericht bleibt. Ein PDF baut Jarvis nach deinem Bericht selbst, wenn der Auftrag eines verlangt: " +
                "Rufe dafür NICHT ablage_schreiben auf. Setze jedes gespeicherte Bild an passender Stelle als eigene Zeile ![Bildunterschrift](dateiname.png) in den Bericht, " +
                "mit genau dem Dateinamen aus dem Werkzeug-Ergebnis; nicht eingebundene Bilder kommen ans Ende des PDFs."
            val denker = JarvisAgent(app)
            val bericht = denker.versuche(
                auftrag = auftrag, zeitlimitMs = plan.minuten * 60_000L, maxSchritte = plan.schritte, rolle = rolle, mitInternet = true, ablageTitel = titel, abschluss = true,
                beiSchritt = { schritt -> _laeufe.update { l -> l.map { if (it.id == lauf.id) it.copy(schritt = schritt) else it } }; beiSchritt(schritt) },
            )
            val grund = denker.abbruchGrund
            if (bericht.isNullOrBlank()) {
                Protokoll.melde(Quelle.JARVIS, "Agent ${plan.name}", "Kein Ergebnis (${grund ?: "leere Antwort"}) für: ${auftrag.take(120)}", ok = false)
                return AgentenErgebnis(null, grund ?: "Das Modell hat keinen Bericht geliefert")
            }
            val datei = Ablage.schreibe(app, titel, "# ${plan.name}: ${auftrag.trim()}\n\nErstellt am ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm 'Uhr'"))} von Jarvis.\n\n$bericht")
            var pdf = ""
            var pdfFehler: String? = null
            if (PDF_WUNSCH.containsMatchIn(auftrag)) {
                pdf = runCatching { " " + AblageZentrale.textAlsPdfAblegen(app, titel, bericht) }
                    .getOrElse { pdfFehler = "das PDF ließ sich nicht erzeugen: ${AblageZentrale.fehlerText(it)}"; " Das PDF fehlt ($pdfFehler)." }
            }
            // Fehlt das gewünschte PDF, gilt der Lauf nicht als fertig: Grund geht in Protokoll und Benachrichtigung.
            val gesamt = listOfNotNull(grund, pdfFehler).joinToString("; ").ifEmpty { null }
            Protokoll.melde(Quelle.JARVIS, "Agent ${plan.name}", (if (gesamt == null) "Fertig" else "Nicht vollständig ($gesamt)") + ", abgelegt als „${datei.nameWithoutExtension}“.$pdf", ok = gesamt == null)
            return AgentenErgebnis(datei.nameWithoutExtension, gesamt)
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

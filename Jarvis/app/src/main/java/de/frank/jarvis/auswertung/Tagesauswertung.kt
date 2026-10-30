package de.frank.jarvis.auswertung

import android.content.Context
import android.util.Log
import de.frank.jarvis.ablage.AblageSpeicher
import de.frank.jarvis.agent.JarvisAgent
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.BiomarkerFaehigkeit
import de.frank.jarvis.faehigkeit.IdeenFaehigkeit
import de.frank.jarvis.faehigkeit.TagebuchFaehigkeit
import de.frank.jarvis.faehigkeit.WeckerFaehigkeit
import de.frank.jarvis.faehigkeit.WetterFaehigkeit
import de.frank.jarvis.faehigkeit.WissenFaehigkeit
import de.frank.jarvis.faehigkeit.KalenderFaehigkeit
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Eine gespeicherte Tagesauswertung. [mitKi] = vom Modell geschrieben; sonst der reine Datenbericht.
 * [vorlaeufig] = Sofort-Fassung beim Abruf: Sie zeigt frische Daten, zählt aber nicht als erfüllter Lauf.
 */
// [profil]: Ausführlichkeit, mit der das Modell geschrieben hat (leer bei Datenbericht und älteren Fassungen).
data class Auswertung(val zeit: Long, val anlass: String, val text: String, val daten: String, val mitKi: Boolean, val modell: String, val vorlaeufig: Boolean = false,
                      val profil: String = "") {
    val zeitpunkt: LocalDateTime get() = Instant.ofEpochMilli(zeit).atZone(ZoneId.systemDefault()).toLocalDateTime()
}

data class AuswertungsStand(val laeuft: Boolean = false, val schritt: String = "", val neueste: Auswertung? = null)

/**
 * Die Tagesauswertung: Jarvis trägt zu festen Uhrzeiten zusammen, was für ein Tag heute ist, wie die Biodaten
 * im Vergleich stehen und was die nächsten Tage bringen, und legt das Ergebnis fertig ab. ChatGPT holt es mit
 * einem einzigen Aufruf, statt alle Apps neu abzufragen.
 *
 * Zwei Schritte: Erst sammeln feste Regeln die Daten (Dienst, Schlafzeiten, Termine, Kennzahlen). Dann schreibt
 * das eigene Modell von Jarvis daraus die Auswertung. Ohne Modell bleibt der Datenbericht — er ist auch allein
 * brauchbar. Die Aufgaben gehören nicht in die gespeicherte Fassung: Sie ändern sich laufend und werden beim
 * Abruf frisch gelesen.
 */
object Tagesauswertung {
    private const val TAG = "JarvisAuswertung"
    private const val BEHALTEN = 100  // bei stündlicher Synchronisation gut vier Tage
    private val sperre = Mutex()
    private val _stand = MutableStateFlow(AuswertungsStand())
    val stand: StateFlow<AuswertungsStand> = _stand.asStateFlow()

    /** Die neueste endgültige Fassung (kein vorläufiger Datenbericht), damit veraltet() nicht jedes Mal den Ordner liest. */
    @Volatile private var endgueltig: Auswertung? = null
    /** Für Arbeit, die einen Abruf überdauern darf (Nachziehen von Wetter und Tagebuch beim Datenbericht). */
    private val hintergrund = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun ordner(context: Context) = File(context.filesDir, "tagesauswertung").apply { mkdirs() }

    /** Alle gespeicherten Fassungen, neueste zuerst. Neue Namen beginnen mit „z“ und sortieren hinter die alten in Ortszeit. */
    private fun dateien(context: Context): List<File> =
        ordner(context).listFiles { f -> f.name.endsWith(".json") }.orEmpty().sortedByDescending { it.name }

    fun lade(context: Context) {
        neueste(context)
    }

    /**
     * Die neueste Fassung (auch eine vorläufige). Aus dem Speicher, sonst von der Platte: Eine beschädigte oder halb
     * geschriebene Datei wird übersprungen, dann gilt die nächstältere.
     */
    fun neueste(context: Context): Auswertung? = _stand.value.neueste
        ?: dateien(context).asSequence().mapNotNull(::lies).firstOrNull()?.also { a -> _stand.update { if (it.neueste == null) it.copy(neueste = a) else it } }

    /** Die neueste endgültige Fassung: ohne vorläufige Datenberichte, die beim Abruf entstehen. */
    fun neuesteEndgueltige(context: Context): Auswertung? {
        _stand.value.neueste?.takeIf { !it.vorlaeufig }?.let { return it }
        endgueltig?.let { return it }
        return dateien(context).asSequence().mapNotNull(::lies).firstOrNull { !it.vorlaeufig }?.also { endgueltig = it }
    }

    private fun lies(datei: File): Auswertung? = runCatching {
        JSONObject(datei.readText()).let { Auswertung(it.getLong("zeit"), it.optString("anlass"), it.optString("text"), it.optString("daten"), it.optBoolean("ki"), it.optString("modell"), it.optBoolean("vorlaeufig"), it.optString("profil")) }
    }.getOrNull()

    /** Wie runCatching, reicht aber einen Abbruch weiter: Ein beendeter Dienst soll keine halbe Auswertung speichern. */
    private inline fun <T> ohneAbbruch(block: () -> T): T? = try { block() } catch (e: CancellationException) { throw e } catch (e: Throwable) { null }

    /** Die letzte Deutung von heute als Anhang für einen reinen Datenbericht, damit sie nicht aus dem Gedächtnis fällt. */
    private fun letzteDeutung(context: Context): String = neuesteGedeutete(context)?.takeIf { it.mitKi && it.zeitpunkt.toLocalDate() == LocalDate.now() }
        ?.let { "\n\nLetzte Deutung von Jarvis von ${it.zeitpunkt.format(DateTimeFormatter.ofPattern("HH:mm"))} Uhr (Werte können sich seitdem geändert haben):\n" + it.text }
        .orEmpty()

    private fun archivOrdner(context: Context) = File(context.filesDir, "tagesauswertung_archiv").apply { mkdirs() }

    /**
     * Die neueste gedeutete Auswertung der letzten 24 Stunden, sonst die neueste überhaupt. Ein reiner Datenbericht
     * (Modell kurz nicht erreichbar) soll eine gute Deutung von vor einer Stunde nicht aus dem Gedächtnis verdrängen.
     */
    fun neuesteGedeutete(context: Context): Auswertung? {
        val grenze = System.currentTimeMillis() - 24 * 3_600_000L
        _stand.value.neueste?.takeIf { it.mitKi && it.zeit >= grenze }?.let { return it }
        // Nur beim ersten Zugriff von der Platte suchen; danach hält speichere() die neueste gedeutete Fassung im Speicher.
        // Ein fehlender Treffer wird ebenfalls gemerkt, sonst läse jede Gedächtnis-Auffrischung bis zu 30 Dateien.
        if (!gedeutetGeprueft) {
            val gefunden = dateien(context).asSequence().take(30).mapNotNull(::lies).firstOrNull { it.mitKi }
            synchronized(this) {
                if (!gedeutetGeprueft) {
                    if (gefunden != null && (gedeutet?.zeit ?: 0L) < gefunden.zeit) gedeutet = gefunden
                    gedeutetGeprueft = true
                }
            }
        }
        return gedeutet?.takeIf { it.zeit >= grenze } ?: neueste(context)
    }

    /** Die neueste gedeutete Fassung im Speicher, siehe neuesteGedeutete(). */
    @Volatile private var gedeutet: Auswertung? = null
    @Volatile private var gedeutetGeprueft = false

    /**
     * Das Fazit (erster Absatz) der letzten gedeuteten Auswertung je Tag, für die letzten [tage] Tage vor heute.
     * Quelle ist das Tagesarchiv, das anders als die Einzelfassungen nicht nach gut vier Tagen gelöscht wird.
     */
    fun fazits(context: Context, tage: Int): List<Pair<LocalDate, String>> {
        val heute = LocalDate.now()
        return (tage downTo 1).mapNotNull { n ->
            val tag = heute.minusDays(n.toLong())
            File(archivOrdner(context), "$tag.txt").takeIf { it.exists() }?.readText()?.trim()?.substringBefore("\n\n")?.substringBefore("\nRÜCKBLICK")?.trim()
                ?.let { tag to (if (it.length > 400) it.take(400) + " …" else it) }
        }
    }

    /** Das Tagesarchiv für gedaechtnis_suchen: Datum → Text der letzten gedeuteten Auswertung des Tages. */
    fun archiv(context: Context): List<Pair<LocalDate, File>> = archivOrdner(context).listFiles { f -> f.name.endsWith(".txt") }.orEmpty()
        .mapNotNull { f -> runCatching { LocalDate.parse(f.name.take(10)) to f }.getOrNull() }.sortedByDescending { it.first }

    private fun speichere(context: Context, a: Auswertung) {
        // Je Tag bleibt die letzte gedeutete Fassung dauerhaft: das Langzeitgedächtnis der Tagesauswertungen.
        if (a.mitKi) runCatching { AblageSpeicher.schreibeAtomar(File(archivOrdner(context), "${a.zeitpunkt.toLocalDate()}.txt"), a.text.toByteArray()) }
            .onFailure { Logbuch.fehler("Tagesauswertung: Archiv nicht geschrieben", it) }
        // Atomar (Zwischendatei, dann umbenennen): Ein Abbruch hinterlässt nie eine halbe neueste Fassung.
        // Der Name kommt aus der Epoch-Zeit, damit Zeitumstellung und Zeitzonenwechsel die Reihenfolge nicht verdrehen.
        val name = "z%013d.json".format(Locale.ROOT, a.zeit)
        AblageSpeicher.schreibeAtomar(
            File(ordner(context), name),
            JSONObject().put("zeit", a.zeit).put("anlass", a.anlass).put("text", a.text).put("daten", a.daten).put("ki", a.mitKi).put("modell", a.modell)
                .put("vorlaeufig", a.vorlaeufig).put("profil", a.profil).toString().toByteArray(),
        )
        if (!a.vorlaeufig) endgueltig = a
        if (a.mitKi) gedeutet = a
        dateien(context).drop(BEHALTEN).forEach { it.delete() }
        // Reste abgebrochener Schreibvorgänge wegräumen.
        val alt = System.currentTimeMillis() - 3_600_000L
        ordner(context).listFiles { f -> f.name.endsWith(".tmp") && f.lastModified() < alt }?.forEach { it.delete() }
    }

    /**
     * Sofort-Fassung ohne Abgleich und ohne Modell, in wenigen Sekunden fertig. Für den Fall, dass beim Abruf
     * nur eine veraltete Auswertung vorliegt: lieber frische Daten ohne Deutung als eine alte Deutung.
     */
    suspend fun datenbericht(context: Context, anlass: String): Auswertung {
        val app = context.applicationContext
        val beginn = System.currentTimeMillis()
        // Nur Wetter und die letzten Tagebuchtage kurz nachziehen, höchstens 2,5 Sekunden: Mehr nutzt der Bericht nicht.
        // Das Wissen und das ganze Tagebuch holt erstelle() gleich danach ohnehin (ein zweiter, gleichzeitiger
        // Wissens-Abgleich würde dessen Zwischenverzeichnis zerschießen). Was länger braucht, läuft im Hintergrund zu
        // Ende (ein laufender Netzaufruf lässt sich nicht sauber abbrechen); begrenzt wird nur das Warten.
        val alle = Register.alle(app)
        val nachziehen = hintergrund.launch {
            launch { ohneAbbruch { alle.filterIsInstance<WetterFaehigkeit>().firstOrNull()?.synchronisiere() } }
            // Mit kleinem Zeitraum: Der Bericht nutzt nur den Rückblick der letzten Woche, und es wird nichts gelöscht.
            launch { ohneAbbruch { alle.filterIsInstance<TagebuchFaehigkeit>().firstOrNull()?.synchronisiere(tage = 8) } }
        }
        withTimeoutOrNull(2_500L) { nachziehen.join() }
        val fertig = Auswertung(
            System.currentTimeMillis(), anlass,
            "Diese Fassung ist der reine Datenbericht, ohne die Deutung von Jarvis. Die vollständige Auswertung wird gerade im Hintergrund erstellt." + letzteDeutung(app),
            sammle(app), false, "", vorlaeufig = true,
        )
        speichere(app, fertig)
        _stand.value = _stand.value.copy(neueste = fertig)
        Logbuch.dauer("Tagesauswertung: Datenbericht", System.currentTimeMillis() - beginn, anlass)
        return fertig
    }

    /**
     * Ist die neueste endgültige Auswertung älter als der letzte Lauf, der laut Plan schon hätte stattfinden sollen?
     * Ein vorläufiger Datenbericht zählt nicht: Bis ein echter Lauf gelingt, bleibt die Auswertung veraltet.
     */
    fun veraltet(context: Context, aktuell: Auswertung? = neueste(context)): Boolean {
        val neu = (if (aktuell == null || aktuell.vorlaeufig) neuesteEndgueltige(context) else aktuell)?.zeitpunkt ?: return true
        // Ohne Synchronisation zählt nur der Tag. Mit ihr zählt der letzte fällige Lauf: Über die Schlafpause hinweg
        // (auch über Mitternacht) bleibt die Fassung von davor die richtige.
        val faellig = Zeitplan.letzterFaelliger(context) ?: return neu.toLocalDate() != LocalDate.now()
        // Fünf Minuten Luft: Ein Lauf, der gerade erst gestartet ist, zählt noch nicht als verpasst.
        return neu.isBefore(faellig) && faellig.isBefore(LocalDateTime.now().minusMinutes(5))
    }

    /** Erstellt eine neue Auswertung. Läuft schon eine, kehrt der Aufruf sofort mit null zurück. */
    suspend fun erstelle(context: Context, anlass: String): Auswertung? {
        val app = context.applicationContext
        if (!sperre.tryLock()) return null
        val beginn = System.currentTimeMillis()
        try {
            fun schritt(text: String) { _stand.value = _stand.value.copy(laeuft = true, schritt = text) }
            val einstellungen = Einstellungen.get(app)
            val alle = Register.alle(app)
            // Wetter, Tagebuch und Wissen hängen nicht voneinander ab: gleichzeitig holen, gewartet wird nur auf die langsamste Quelle.
            // Biodaten werden NICHT abgeglichen (Frank-Wunsch 2026-10-15): Entropie Reductor holt sie selbst, Jarvis liest
            // nur den dort gespeicherten Stand. Jeder Whoop-Abruf kostet einen Token-Tausch und damit ein Risiko für die Anmeldung.
            schritt("Wetter, Tagebuch und Wissen holen")
            coroutineScope {
                listOf(
                    async(Dispatchers.IO) { ohneAbbruch { alle.filterIsInstance<WetterFaehigkeit>().firstOrNull()?.synchronisiere() }; Unit },
                    async(Dispatchers.IO) { ohneAbbruch { alle.filterIsInstance<TagebuchFaehigkeit>().firstOrNull()?.synchronisiere() }; Unit },
                    async(Dispatchers.IO) { ohneAbbruch { alle.filterIsInstance<WissenFaehigkeit>().firstOrNull()?.synchronisiere() }; Unit },
                ).awaitAll()
            }
            Logbuch.dauer("Tagesauswertung: Quellen geholt", System.currentTimeMillis() - beginn, "Biodaten aus dem gespeicherten Stand")

            schritt("Daten zusammentragen")
            val daten = sammle(app)

            val deuten = einstellungen.auswertungDeutung
            val profil = einstellungen.auswertungProfil
            if (deuten) schritt("Auswertung schreiben")
            // Zählt die Tokens aller Modellanfragen dieser Auswertung; sie stehen danach unter dem Text.
            val zaehler = de.frank.jarvis.auth.TokenZaehler()
            val antwort: String? = if (!deuten) null else try {
                // „Maximal“ denkt mehrere Minuten pro Schritt: dann mehr Zeit, sonst endet der Lauf als reiner Datenbericht.
                val zeitlimit = if (einstellungen.denkstufe == de.frank.jarvis.auth.ReasoningEffort.MAX) 12 * 60_000L else 5 * 60_000L
                kotlinx.coroutines.withContext(zaehler) {
                    JarvisAgent(app).versuche(auftrag(daten, profil), zeitlimitMs = zeitlimit, maxSchritte = 3, beiSchritt = { schritt("Jarvis prüft nach: $it") })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.w(TAG, "Modell fehlgeschlagen", e)
                Logbuch.fehler("Tagesauswertung: Modell fehlgeschlagen", e, anlass)
                null
            }
            val text = antwort?.trim()?.takeIf { it.length > 200 }?.let { t ->
                if (zaehler.tokens <= 0) t
                else t + "\n\nVerbrauch dieser Auswertung: " + String.format(Locale.GERMAN, "%,d", zaehler.tokens) + " Tokens (${einstellungen.modell.label})."
            }

            val fertig = Auswertung(
                zeit = System.currentTimeMillis(),
                anlass = anlass,
                // Ohne neue Deutung bleibt die letzte von heute sichtbar, statt vom reinen Datenbericht verdrängt zu werden.
                text = text ?: ((if (deuten) "Diese Fassung ist der reine Datenbericht: Das Modell von Jarvis war nicht erreichbar oder nicht verbunden."
                else "Diese Fassung ist der reine Datenbericht: Die Deutung durch das Modell ist in Jarvis unter Einstellungen ausgeschaltet.") + letzteDeutung(app)),
                daten = daten,
                mitKi = text != null,
                modell = if (text != null) einstellungen.modell.label else "",
                profil = if (text != null) profil else "",
            )
            // Wurde der Dienst inzwischen beendet, keine halb gesammelte Fassung als neueste ablegen.
            currentCoroutineContext().ensureActive()
            speichere(app, fertig)
            _stand.value = AuswertungsStand(laeuft = false, neueste = fertig)
            // Hat ein Claude-Modell geschrieben, zeigt die Anzeige unten danach den neuen Guthabenstand.
            if (text != null && einstellungen.modell.istAnthropic) de.frank.jarvis.faehigkeit.AnthropicGuthabenFaehigkeit.nachAufgabeHolen(app)
            else de.frank.jarvis.faehigkeit.AnthropicGuthabenFaehigkeit.frischHalten(app)
            // Modell ausgefallen: höchstens zweimal nach 15 Minuten neu versuchen; nach einer gelungenen Deutung ist das erledigt.
            if (deuten) runCatching { if (text != null) Zeitplan.deutungGelungen(app) else Zeitplan.nachModellausfall(app, anlass) }
            // Das Live-Gedächtnis übernimmt die neue Deutung und die frisch geholten Daten sofort.
            de.frank.jarvis.gedaechtnis.Lagebild.anstossen(app, listOf(
                de.frank.jarvis.gedaechtnis.Bereich.AUSWERTUNG, de.frank.jarvis.gedaechtnis.Bereich.BIODATEN, de.frank.jarvis.gedaechtnis.Bereich.TRAININGS,
                de.frank.jarvis.gedaechtnis.Bereich.WETTER, de.frank.jarvis.gedaechtnis.Bereich.TAGEBUCH, de.frank.jarvis.gedaechtnis.Bereich.RUECKBLICK, de.frank.jarvis.gedaechtnis.Bereich.WISSEN,
            ), "neue Tagesauswertung")
            Protokoll.melde(Quelle.JARVIS, "Tagesauswertung", "$anlass: " + (if (fertig.mitKi) "erstellt mit ${fertig.modell}" else if (deuten) "nur Datenbericht, Modell nicht erreichbar" else "nur Datenbericht, Deutung ausgeschaltet"), fertig.mitKi || !deuten)
            Logbuch.dauer("Tagesauswertung", System.currentTimeMillis() - beginn, "$anlass, " + if (fertig.mitKi) "mit Deutung" else "nur Datenbericht")
            return fertig
        } catch (e: CancellationException) {
            // Abbruch (Dienst beendet) ist kein Fehler: Zustand zurücksetzen und den Abbruch weiterreichen.
            _stand.value = _stand.value.copy(laeuft = false, schritt = "")
            Logbuch.system("Tagesauswertung abgebrochen", anlass)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Auswertung fehlgeschlagen", e)
            Logbuch.fehler("Tagesauswertung fehlgeschlagen", e, anlass)
            Protokoll.melde(Quelle.JARVIS, "Tagesauswertung", "Fehlgeschlagen: ${e.message ?: e.javaClass.simpleName}", ok = false)
            _stand.value = _stand.value.copy(laeuft = false, schritt = "")
            return null
        } finally {
            sperre.unlock()
        }
    }

    /**
     * Schritt 1: alle Daten nach festen Regeln, als ein Text mit klaren Abschnitten. Alle Quellen werden gleichzeitig
     * abgefragt und danach in fester Reihenfolge zusammengesetzt: Der Text bleibt gleich, die Wartezeit ist die der
     * langsamsten Quelle statt der Summe.
     */
    private suspend fun sammle(app: Context): String = coroutineScope {
        val alle = Register.alle(app)
        val kalender = alle.filterIsInstance<KalenderFaehigkeit>().firstOrNull()
        val biomarker = alle.filterIsInstance<BiomarkerFaehigkeit>().firstOrNull()
        val werkzeuge = alle.flatMap { it.werkzeuge }.associateBy { it.name }
        suspend fun rufe(name: String, argumente: JSONObject = JSONObject()): String =
            ohneAbbruch { werkzeuge[name]?.ausfuehren(argumente)?.let { (if (it.fehler) "NICHT VERFÜGBAR: " else "") + it.text } } ?: "NICHT VERFÜGBAR"

        val heute = LocalDate.now()
        val jetzt = LocalDateTime.now()
        val rahmen = async(Dispatchers.IO) { ohneAbbruch { kalender?.rahmen(6) }?.ifEmpty { null } ?: "NICHT VERFÜGBAR: Kalender nicht lesbar" }
        val termine = async(Dispatchers.IO) { rufe("kalender_lesen", JSONObject().put("von", heute.toString()).put("bis", heute.plusDays(4).toString())) }
        val wetter = async(Dispatchers.IO) { ohneAbbruch { alle.filterIsInstance<WetterFaehigkeit>().firstOrNull()?.vorschau(3) } ?: "NICHT VERFÜGBAR" }
        val wecker = async(Dispatchers.IO) { ohneAbbruch { alle.filterIsInstance<WeckerFaehigkeit>().firstOrNull()?.ueberblick() } ?: "NICHT VERFÜGBAR" }
        val bioTag = async(Dispatchers.IO) { rufe("biomarker_tag", JSONObject().put("datum", heute.toString())) }
        val bioVergleich = async(Dispatchers.IO) { rufe("biomarker_auswertung", JSONObject().put("tage", 30).put("metriken", JSONArray(biomarker?.alleMetriken ?: emptyList<String>()))) }
        val bioVerlauf = async(Dispatchers.IO) { rufe("biomarker_verlauf", JSONObject().put("von", heute.minusDays(7).toString()).put("aufloesung", "tag").put("metriken", JSONArray(listOf("schlafdauer", "wach_min", "erholung", "hrv", "ruhepuls")))) }
        val rueckblickTermine = async(Dispatchers.IO) { rufe("kalender_lesen", JSONObject().put("von", heute.minusDays(4).toString()).put("bis", heute.minusDays(1).toString())) }
        val trainings = async(Dispatchers.IO) { rufe("trainings_lesen", JSONObject().put("von", heute.minusDays(14).toString()).put("limit", 20)) }
        val tagebuch = async(Dispatchers.IO) { ohneAbbruch { alle.filterIsInstance<TagebuchFaehigkeit>().firstOrNull()?.rueckblick(7) } ?: "NICHT VERFÜGBAR" }
        val ideen = async(Dispatchers.IO) { ohneAbbruch { alle.filterIsInstance<IdeenFaehigkeit>().firstOrNull()?.ueberblick() } ?: "NICHT VERFÜGBAR" }
        val wissen = async(Dispatchers.IO) { ohneAbbruch { alle.filterIsInstance<WissenFaehigkeit>().firstOrNull()?.ueberblick() } ?: "NICHT VERFÜGBAR" }
        val aufgabenHeute = async(Dispatchers.IO) { rufe("aufgaben_lesen", JSONObject().put("bereich", "heute")) }
        val aufgabenMorgen = async(Dispatchers.IO) { rufe("aufgaben_lesen", JSONObject().put("bereich", "morgen")) }

        buildString {
            append("STAND: ").append(jetzt.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN))).append('\n')
            append("Biodaten: gespeicherter Stand aus Entropie Reductor, das selbst abgleicht. Wie alt die Whoop-Daten sind, steht bei den Biomarkern (Zeile „Whoop-Stand“); Belastung und Energie von heute sind Zwischenstände mit dieser Uhrzeit. Steht dort „ACHTUNG“, ist der Abruf einer Quelle gestört: das gehört an den Anfang der Auswertung.").append("\n\n")

            append("== RAHMEN DER NÄCHSTEN TAGE (fest gerechnet, verbindlich) ==\n")
            append(rahmen.await()).append("\n\n")

            append("== TERMINE HEUTE UND DIE NÄCHSTEN 4 TAGE ==\n")
            append(termine.await()).append("\n\n")

            append("== WETTER HEUTE, MORGEN, ÜBERMORGEN ==\n")
            append(wetter.await()).append("\n\n")

            append("== GESTELLTE WECKER ==\n")
            append(wecker.await()).append("\n\n")

            append("== BIODATEN: WERTE DES AKTUELLEN TAGES ==\n")
            append(bioTag.await()).append("\n\n")

            append("== BIODATEN: AKTUELLER STAND GEGEN DEN LETZTEN MONAT, 7 TAGE UND ALLE BISHERIGEN TAGE ==\n")
            append(bioVergleich.await()).append("\n\n")

            append("== BIODATEN: VERLAUF DER LETZTEN 7 TAGE (Schlaf und Erholung, zum Abgleich mit den Diensten) ==\n")
            append(bioVerlauf.await()).append("\n\n")

            append("== DIENSTE UND TERMINE DER LETZTEN 4 TAGE ==\n")
            append(rueckblickTermine.await()).append("\n\n")

            append("== TRAININGS DER LETZTEN 14 TAGE ==\n")
            append(trainings.await()).append("\n\n")

            append("== TAGEBUCH DER LETZTEN 7 TAGE ==\n")
            append(tagebuch.await()).append("\n\n")

            append("== GENIALE IDEEN (offen, gekürzt) ==\n")
            append(ideen.await()).append("\n\n")

            append("== WISSENS-DATENBANK (Inhaltsverzeichnis; Volltexte über wissen_lesen) ==\n")
            append(wissen.await()).append("\n\n")

            append("== OFFENE AUFGABEN (nur Stand jetzt, ändert sich laufend) ==\n")
            append(aufgabenHeute.await()).append('\n')
            append(aufgabenMorgen.await())
        }
    }

    /**
     * Schritt 2: der Auftrag an das Modell. Gründlich ist der bisherige Auftrag Wort für Wort; Normal und Kurz bekommen
     * denselben Auftrag und davor ihren Umfang-Block, der nur Länge und Verdichtung regelt.
     */
    private fun auftrag(daten: String, profil: String): String {
        val text = auftrag(daten)
        val block = umfang(profil) ?: return text
        // Die erste Fundstelle ist die der Vorlage, die Daten stehen erst dahinter.
        val stelle = text.indexOf("\nDATEN:\n").takeIf { it >= 0 } ?: return text
        return text.substring(0, stelle) + "\n\n" + block + text.substring(stelle)
    }

    /** Länge und Verdichtung je Profil; null = gründlich, unverändert. Die Wortzahlen beziehen sich auf rund 1.000 Wörter bei Gründlich. */
    private fun umfang(profil: String): String? = when (profil) {
        Einstellungen.AUSWERTUNG_NORMAL -> """
UMFANG DIESER AUSWERTUNG: NORMAL. Diese Vorgaben gehen allen Längen- und Umfangsangaben oben vor; alles andere gilt unverändert.
Frank hat das Profil Normal gewählt: dieselben Informationen wie in der ausführlichen Fassung, sprachlich verdichtet, insgesamt etwa 450 bis 550 Wörter, also rund die Hälfte. Lass keine Information und keine Warnung weg, sag sie knapper: weniger Füllwörter, keine Wiederholungen, zusammengehörige Werte in einem Satz.
- Fazit: ein Satz, danach eine Leerzeile.
- RÜCKBLICK: zwei bis drei Sätze.
- AKTUELL: drei bis vier kurze Absätze. Schlaf, HRV, Ruhepuls, Erholung und VO2max wie bisher, aber je Wert nur der aussagekräftigste Vergleich (meist der Monatsschnitt) statt Woche, Monat und Gesamt nacheinander; einen weiteren Vergleich nur, wenn er etwas anderes aussagt. Danach Wetter, Aufgaben, die Einschätzung und zwei bis drei konkrete Empfehlungen.
- AUSBLICK: zwei bis drei Sätze.
Die Überschriften RÜCKBLICK, AKTUELL und AUSBLICK stehen weiter in Großbuchstaben auf eigener Zeile. Schreib für das Ohr: kurze bis mittellange Sätze mit je einer Aussage, natürliche Übergänge, gerundete Zahlen, keine Klammern, keine Abkürzungen.
""".trim()
        Einstellungen.AUSWERTUNG_KURZ -> """
UMFANG DIESER AUSWERTUNG: KURZ. Diese Vorgaben gehen allen Längen- und Umfangsangaben oben vor; alles andere gilt unverändert.
Frank hat das Profil Kurz gewählt: ein schneller, stimmiger Überblick zum Zuhören, insgesamt etwa 220 bis 280 Wörter, also rund ein Viertel der ausführlichen Fassung. Das ist keine abgeschnittene Fassung: Wähle das Wesentliche aus und sag es knapp. Immer enthalten sind: die Art des Tages mit Dienst, Abfahrt und Schlafenszeit, feste Termine, jede Warnung (zum Beispiel ein Wecker, der zum Dienst fehlt, ein gestörter Abruf mit ACHTUNG, überfällige Aufgaben), die Kernaussage zu Schlaf und Erholung mit Franks wichtigsten Werten und eine klare Empfehlung.
- Fazit: ein Satz, danach eine Leerzeile.
- RÜCKBLICK: ein bis zwei Sätze.
- AKTUELL: vier bis acht Stichpunkte. Jeder steht auf eigener Zeile, beginnt mit einem Bindestrich und ist ein ganzer, kurzer Satz mit Punkt, damit er vorgelesen natürlich klingt. Höchstens ein Vergleich je Messgröße, gerundete Zahlen.
- AUSBLICK: ein bis zwei Sätze.
Die Überschriften RÜCKBLICK, AKTUELL und AUSBLICK stehen weiter in Großbuchstaben auf eigener Zeile. Keine Klammern, keine Abkürzungen, keine Reihen bloßer Zahlen.
""".trim()
        else -> null
    }

    /** Der gründliche Auftrag, unverändert seit vor den Profilen. */
    private fun auftrag(daten: String): String = """
Schreibe Franks Tagesauswertung. Sie wird gespeichert und ihm später vorgelesen: ganze, gut sprechbare Sätze, du-Anrede, keine Tabellen, kein Markdown.

Dein Ziel: Frank weiß danach, wie es ihm geht, was für ein Tag heute ist und was jetzt sinnvoll ist. Der Schwerpunkt liegt auf dem heutigen Tag: AKTUELL ist der ausführliche Hauptteil und macht etwa zwei Drittel der Auswertung aus. RÜCKBLICK und AUSBLICK sind kurz und dienen nur dazu, das Heute einzuordnen: woher der heutige Zustand kommt und worauf er hinausläuft. Verbinde die Daten miteinander, statt sie nacheinander aufzuzählen. Sage jeden Punkt nur einmal: Was im RÜCKBLICK erklärt ist, greift AKTUELL nur in einem Halbsatz auf, und ein Hinweis aus AKTUELL steht nicht noch einmal im AUSBLICK.

So gehst du vor:
1. Lies zuerst den RAHMEN. Er legt verbindlich fest, ob ein Tag Arbeitstag oder frei ist, welcher Dienst ansteht, wann Frank schläft und wann er Zeit hat. Rechne das nicht neu.
2. Deute die Biodaten immer vor diesem Hintergrund. Nach einem Nachtdienst schläft Frank am Tag, etwa von 6 bis 15 Uhr. Tagschlaf ist oft kürzer und unruhiger als Nachtschlaf. Sind dann Schlafdauer, Tiefschlaf oder REM niedriger oder die Wachzeit höher als sonst, nenne den Tagschlaf als wahrscheinlichen Grund und rate zu Erholung und Entspannung statt zu Belastung. Ein Schlafwert gehört zu dem Tag, an dem der Schlaf endet. Fehlt der Schlafwert von heute, weil Frank laut Rahmen noch schläft oder gerade aufgestanden ist, ist das normal.
3. Verbinde alles miteinander: Erholung mit Dienst und Training, Wetter mit den freien Zeitfenstern, Aufgaben und Ideen mit der Zeit und der Kraft, die Frank heute wirklich hat.

Feste Regeln:
- Verwende nur Zahlen, die unten stehen, und schätze nichts. Steht bei etwas NICHT VERFÜGBAR, sage das in einem Satz und deute es nicht.
- Nenne die Zahlen, die etwas aussagen, gerundet und mit Einordnung (zum Beispiel „HRV 49, über deinem Monatsschnitt“). Sage Wochentage statt Kalenderdaten.
- Tagesbelastung, Energieumsatz und Schritte des laufenden Tages sind Zwischenstände.
- Stehen in deiner Anweisung FRANKS REGELN, halte sie ein. Sie gehen diesen Vorgaben vor. Steht dort WAS JARVIS ÜBER FRANK WEISS, beziehe es in Deutung und Empfehlungen ein (zum Beispiel was ihm wichtig ist oder was er bei Regen nicht mag). Stehen dort FRANKS ZIELE, stelle sie den aktuellen Werten gegenüber und nenne, was heute darauf einzahlt.
- Keine medizinischen Diagnosen.
- Meist reichen die Daten unten. Du darfst höchstens zwei Werkzeuge zusätzlich aufrufen, wenn ein auffälliger Wert einen Blick in den Verlauf braucht.

Aufbau: Beginne mit einem Satz Fazit. Danach folgen genau diese drei Teile, jeder mit seiner Überschrift in Großbuchstaben auf eigener Zeile.

RÜCKBLICK: Kurz, höchstens ein Absatz mit drei bis fünf Sätzen. Nur was den heutigen Zustand erklärt: welche Dienste oder freien Tage direkt hinter Frank liegen, die wichtigste Entwicklung bei Schlaf, Erholung und Training (Tagschlaf nach Nachtdiensten gesondert betrachten) und höchstens ein Tagebuch-Thema, das ihn noch beschäftigt. Keine Tag-für-Tag-Aufzählung, keine Wiederholung einzelner Werte, die in AKTUELL ohnehin vorkommen.

AKTUELL: Der heutige Tag, der ausführliche Hauptteil. Stelle hier den Zusammenhang zur Vergangenheit her (warum die Werte heute so sind) und zum Ausblick (was das Heute für die nächsten Tage bedeutet). Was für ein Tag es ist (Dienst oder frei, Abfahrt, Schlaf- und freie Zeitfenster) und welche Termine anstehen. Wie es Frank heute geht: der letzte Schlaf mit Dauer, Tiefschlaf, REM und Wachzeit, dazu Erholung, HRV und Ruhepuls, eingeordnet gegen 7 Tage, den letzten Monat und alle bisherigen Tage und bezogen auf die Art des Tages. Körperwerte nur, wenn sie neu oder auffällig sind. Das Wetter jetzt und über den Tag. Welche Aufgaben heute anliegen oder überfällig sind und welche in die freien Zeitfenster passen. Dann eine klare Einschätzung, wie belastbar Frank heute ist, und zwei bis vier konkrete Empfehlungen, was als Nächstes sinnvoll ist. Passt eine der genialen Ideen zu Zeit und Kraft von heute, nenne sie; zähle die Ideen nicht auf.

AUSBLICK: Kurz, höchstens ein Absatz mit drei bis fünf Sätzen. Nur das Wichtigste der nächsten Tage: der nächste Dienst oder freie Tag und jeder Wechsel in den nächsten Tagen (zum Beispiel der Beginn eines Nachtdienst-Blocks), feste Termine und die Aufgaben von morgen knapp in einem Satz, das beste Zeitfenster draußen für morgen und woran Frank rechtzeitig denken muss (zum Beispiel Vorschlafen vor einem Nachtdienst-Block, früh schlafen vor einem Tagdienst, ein Wecker, der zum Dienst fehlt, Abholung der Tonnen). Keine Tag-für-Tag-Beschreibung des ganzen Dienstblocks; eine Idee für den nächsten freien Tag nur, wenn sie wirklich passt.

DATEN:
$daten
""".trim()

    /** Die Bereiche der Tagesdatenbank: Kurzname → Anfang der Abschnittsüberschrift. */
    val BEREICHE = linkedMapOf(
        "rahmen" to "RAHMEN", "termine" to "TERMINE", "wetter" to "WETTER", "wecker" to "GESTELLTE WECKER", "biodaten_heute" to "BIODATEN: WERTE", "biodaten_vergleich" to "BIODATEN: AKTUELLER",
        "biodaten_verlauf" to "BIODATEN: VERLAUF", "rueckblick_termine" to "DIENSTE UND TERMINE DER LETZTEN", "trainings" to "TRAININGS", "tagebuch" to "TAGEBUCH", "ideen" to "GENIALE IDEEN", "wissen" to "WISSENS-DATENBANK",
    )

    /** Ein oder alle Bereiche der zuletzt gespeicherten Tagesdaten. Aufgaben fehlen bewusst: Sie werden immer frisch gelesen. */
    /** Der Datenanhang ohne die genannten Abschnitte (Anfang der Überschrift ohne „== “), statt ab einer Stelle alles abzuschneiden. */
    fun abschnitte(daten: String, ohne: Set<String>): String =
        daten.split(Regex("(?m)^(?=== )")).filterNot { teil -> ohne.any { teil.startsWith("== $it") } }.joinToString("").trim()

    fun tagesdaten(a: Auswertung, bereich: String): String {
        val abschnitte = a.daten.substringBefore("== OFFENE AUFGABEN").split(Regex("(?m)^== ")).drop(1).map { "== $it".trim() }
        val gesucht = BEREICHE[bereich.trim().lowercase(Locale.GERMAN)]
        val teile = if (gesucht == null) abschnitte else abschnitte.filter { it.startsWith("== $gesucht") }
        val minuten = java.time.Duration.between(a.zeitpunkt, LocalDateTime.now()).toMinutes()
        return "TAGESDATENBANK, Stand " + a.zeitpunkt.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM, HH:mm 'Uhr'", Locale.GERMAN)) +
            " (vor " + (if (minuten < 90) "$minuten Minuten" else "${minuten / 60} Stunden") + ").\n\n" + teile.joinToString("\n\n").ifEmpty { "Dieser Bereich ist nicht vorhanden." }
    }

    /** Der Text, den ein Abruf bekommt: Auswertung, Datenanhang und Alter. Aufgaben hängt der Aufrufer frisch an. */
    fun alsText(a: Auswertung, mitDaten: Boolean): String = buildString {
        val jetzt = LocalDateTime.now()
        val minuten = java.time.Duration.between(a.zeitpunkt, jetzt).toMinutes()
        append("TAGESAUSWERTUNG, erstellt ").append(a.zeitpunkt.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN)))
        append(" (").append(if (minuten < 90) "vor $minuten Minuten" else "vor ${minuten / 60} Stunden").append(", ").append(a.anlass).append(")")
        if (a.mitKi) append(", geschrieben von Jarvis mit ").append(a.modell)
        append(".\n")
        if (a.zeitpunkt.toLocalDate() != jetzt.toLocalDate()) append("ACHTUNG: Diese Auswertung ist nicht von heute. Für heute liegt noch keine vor.\n")
        append('\n').append(a.text).append('\n')
        if (mitDaten || !a.mitKi) append("\n--- DATENANHANG (Stand der Erstellung, zum Nachschlagen einzelner Werte) ---\n").append(a.daten).append('\n')
    }
}

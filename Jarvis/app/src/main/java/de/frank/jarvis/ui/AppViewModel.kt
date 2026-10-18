package de.frank.jarvis.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.jarvis.agent.JarvisAgent
import de.frank.jarvis.auth.ChatTurn
import de.frank.jarvis.auth.CodexModel
import de.frank.jarvis.auth.DeviceAuthInfo
import de.frank.jarvis.auth.ReasoningEffort
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.dienst.JarvisDienst
import de.frank.jarvis.faehigkeit.Merkdatei
import de.frank.jarvis.faehigkeit.Regel
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** So lange vor einem Update darf Frank Jarvis verlassen haben (etwa für UpdateStation), damit der Neustart dort weitermacht. */
private const val UPDATE_VORLAUF = 15 * 60_000L
/** So lange nach dem Update gilt das erste Öffnen noch als Neustart durch das Update, später als normaler Start. */
private const val UPDATE_NACHLAUF = 60 * 60_000L

enum class Reiter(val anzeige: String) { JARVIS("Jarvis"), ABLAGE("Ablage"), AKTIVITAET("Aktivität"), EINSTELLUNGEN("Einstellungen") }

/**
 * Eine Nachricht im Gespräch. [art]: „antwort“, „fehler“ (Fehlermeldung statt Antwort, geht nicht ans Modell) oder
 * „abgebrochen“ (Franks Frage blieb unbeantwortet). [kontext] sieht nur das Modell, etwa was in der Runde erledigt wurde.
 */
data class Nachricht(val vonMir: Boolean, val text: String, val art: String = "antwort", val kontext: String = "", val anhaenge: List<String> = emptyList(), val bewertung: Int = 0)

/** Ein Schritt für „Rückgängig“ in einer Merkliste: wie sich die eigene Änderung umkehren lässt, ohne fremde zu berühren. */
private sealed class MerkSchritt {
    class Angelegt(val regel: Regel) : MerkSchritt()
    class Geaendert(val id: Int, val alterText: String, val neuerText: String) : MerkSchritt()
    class Geloescht(val regel: Regel) : MerkSchritt()
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    companion object {
        /** Das eine ViewModel des Prozesses, für die Activity wie für die Sprachblase im Hintergrund. Nur auf dem Hauptfaden. */
        fun hole(context: Context): AppViewModel {
            val app = context.applicationContext as de.frank.jarvis.JarvisApplication
            return androidx.lifecycle.ViewModelProvider(app, androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app))[AppViewModel::class.java]
        }

        /** Zustand in [ablageUmzug]: Die Übernahme in die Ablage läuft gerade. */
        const val UMZUG_LAEUFT = "läuft"
    }

    val einstellungen = Einstellungen.get(app)
    private val agent = JarvisAgent(app)
    private val auth get() = agent.auth

    private var aktuellerReiter by mutableStateOf(Reiter.JARVIS)
    /** Zuvor geöffnete Reiter, neuester zuletzt. Der Jarvis-Reiter ist die Wurzel: Wer dort ankommt, beginnt neu. */
    private val reiterVerlauf = mutableListOf<Reiter>()

    var reiter: Reiter
        get() = aktuellerReiter
        set(neu) {
            if (neu == aktuellerReiter) return
            if (neu == Reiter.JARVIS) reiterVerlauf.clear() else { reiterVerlauf.remove(neu); reiterVerlauf += aktuellerReiter }
            aktuellerReiter = neu
        }

    /** Offener Bereich der Einstellungen und die zuvor geöffneten, hier statt im Bildschirm: Ein Update stellt ihn wieder her. */
    var einstellungsReiter by mutableStateOf("jarvis")
    var einstellungsVerlauf by mutableStateOf(listOf<String>())

    /** Zurück-Geste auf einem Unterreiter: zum vorher offenen Reiter, ohne Verlauf zum Jarvis-Startbildschirm. Beendet nie die App. */
    fun reiterZurueck() {
        aktuellerReiter = reiterVerlauf.removeLastOrNull() ?: Reiter.JARVIS
        if (aktuellerReiter == Reiter.JARVIS) reiterVerlauf.clear()
    }

    // ---- Sperre ----
    /** true, solange die App auf den Fingerabdruck wartet. Der Dienst im Hintergrund läuft davon unberührt. */
    var gesperrt by mutableStateOf(false)
    /** Seit wann die App nicht mehr auf dem Bildschirm ist; 0 = sichtbar oder eben erst gestartet. */
    var hintergrundSeit = 0L
    /** Wann die App zuletzt sichtbar war; 0 = seit dem Start noch nie entsperrt. */
    var zuletztSichtbar = 0L
    /** Bis wann die Sperre nach einem bewusst gestarteten Teilen, Öffnen oder „Speichern unter“ wartet (sonst ginge das Ergebnis verloren). */
    var sperrAufschubBis = 0L
    /** Jarvis hat selbst eine andere App geöffnet, ohne über die Activity zu gehen (Anmeldeseite von ChatGPT); MainActivity verbraucht es in onStart. */
    var eigenerAbsprung = false
    var entsperrenAnfragen: () -> Unit = {}
    /** Jarvis läuft per „Hey Jarvis“ über dem Sperrbildschirm; das Gespräch bleibt dabei verborgen. */
    var ueberSperre = false
    /** Beendet das Zeigen über der Sperre in der gerade lebenden Activity. */
    var ueberSperreBeenden: () -> Unit = {}

    // ---- Sprache ----
    val sprache = de.frank.jarvis.data.settings.SecureSettings(app)
    val vorleser: de.frank.jarvis.speech.Vorleser = de.frank.jarvis.speech.Vorleser.hole(app, sprache)
    private val mikro = de.frank.module.transkriptionsmodul.TranskriptionAnbindung.mikrofon(app)
    val pegel get() = mikro.pegel
    var nimmtAuf by mutableStateOf(false); private set
    var schreibtMit by mutableStateOf(false); private set
    var mikrofonAnfragen: () -> Unit = {}
    /** Die laufende Frage kam über das Mikrofon: Dann wird die Antwort vorgelesen. */
    private var gesprochen = false

    // ---- Gespräch mit Jarvis ----
    val gespraech = mutableStateListOf<Nachricht>()

    /**
     * Eine laufende Antwort, gebunden an ihr Gespräch: Sie läuft weiter, wenn Frank in ein anderes Gespräch wechselt oder ein
     * neues beginnt, und mehrere Gespräche dürfen gleichzeitig arbeiten. [frage] ist die Stelle der Frage im Gespräch,
     * [erledigt], was schreibende Werkzeuge in dieser Runde schon geändert haben.
     */
    inner class Runde(val sitzung: String, val frage: Int) {
        var schritt by mutableStateOf("")
        lateinit var job: Job
        val erledigt: MutableList<String> = java.util.Collections.synchronizedList(mutableListOf())
    }

    /** Die laufenden Antworten je Gespräch (Kennung → Runde). */
    val laufende = androidx.compose.runtime.mutableStateMapOf<String, Runde>()
    /** Jarvis arbeitet gerade im offenen Gespräch. */
    val denkt: Boolean get() = sitzungId?.let { laufende.containsKey(it) } == true
    /** Was Jarvis im offenen Gespräch gerade tut („Aufgaben lesen“), solange er arbeitet. */
    val schritt: String get() = sitzungId?.let { laufende[it]?.schritt }.orEmpty()

    // ---- ChatGPT-Anmeldung ----
    var kiVerbunden by mutableStateOf(auth.isConnected); private set
    var kiVerbindet by mutableStateOf(false); private set
    var kiKonto by mutableStateOf(auth.email.orEmpty()); private set
    var geraeteCode by mutableStateOf<DeviceAuthInfo?>(null); private set

    // ---- Lage ----
    /** null = Geniale Aufgaben ist angebunden, sonst der Grund. */
    var aufgabenStoerung by mutableStateOf<String?>(null); private set
    var aufgabenGeprueft by mutableStateOf(false); private set
    /** Je angebundener App: null = bereit, sonst der Grund. Fehlt der Eintrag, läuft die Prüfung noch. */
    var stoerungen by mutableStateOf<Map<String, String?>>(emptyMap()); private set
    var akkuFrei by mutableStateOf(false); private set
    /** „Hey Jarvis“: Mikrofon erlaubt und „Über anderen Apps einblenden“ erlaubt. */
    var weckwortMikro by mutableStateOf(false); private set
    var ueberlagernErlaubt by mutableStateOf(false); private set
    var weckwortMikroAnfragen: () -> Unit = {}
    var hinweiseAn by mutableStateOf(true); private set

    var meldung by mutableStateOf<String?>(null)
    var hinweiseAnfragen: () -> Unit = {}
    var kalenderAnfragen: () -> Unit = {}

    /** Die laufende Prüfung der angebundenen Apps und wann sie zuletzt lief (für die Drosselung bei onResume). */
    private var lageJob: Job? = null
    private var lageZuletzt = 0L

    init { lagePruefen() }

    // ---- Stimmprofil für „Hey Jarvis“ ----

    fun stimmeAnlernen() {
        if (!einstellungen.weckwortAn || !weckwortMikro) { meldung = "Zum Anlernen bitte zuerst „Hey Jarvis“ einschalten und das Mikrofon erlauben."; return }
        de.frank.jarvis.weckwort.Stimmprofil.anlernenStarten()
    }

    fun stimmeAnlernenAbbrechen() = de.frank.jarvis.weckwort.Stimmprofil.anlernenAbbrechen()

    fun stimmeLoeschen() {
        de.frank.jarvis.weckwort.Stimmprofil.loeschen()
        meldung = "Stimmprofil gelöscht. Jarvis reagiert wieder auf jede Stimme."
    }

    /**
     * Prüft Anmeldung, Erlaubnisse und die angebundenen Apps. [sofort] = false (beim Zurückkehren in die App): Die Apps
     * werden höchstens alle fünf Minuten gefragt, denn jede Frage weckt die Partner-App. Knöpfe und Erlaubnisse prüfen sofort.
     */
    fun lagePruefen(sofort: Boolean = true) {
        val app = getApplication<Application>()
        kiVerbunden = auth.isConnected
        kiKonto = auth.email.orEmpty()
        akkuFrei = app.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(app.packageName)
        hinweiseAn = NotificationManagerCompat.from(app).areNotificationsEnabled()
        weckwortMikro = de.frank.jarvis.weckwort.WeckwortDienst.mikrofonErlaubt(app)
        ueberlagernErlaubt = android.os.Build.VERSION.SDK_INT < 29 || android.provider.Settings.canDrawOverlays(app)
        val jetzt = System.currentTimeMillis()
        if (!sofort && (lageJob?.isActive == true || jetzt - lageZuletzt < LAGE_ABSTAND_MS)) return
        lageZuletzt = jetzt
        lageJob?.cancel()
        lageJob = viewModelScope.launch {
            // Alle Apps gleichzeitig fragen statt nacheinander: Die Wartezeit ist die der langsamsten, nicht die Summe.
            val lage = withContext(Dispatchers.IO) {
                coroutineScope { Register.alle(app).map { f -> async { f.id to f.stoerung() } }.awaitAll().toMap() }
            }
            stoerungen = lage
            aufgabenStoerung = lage["aufgaben"]
            aufgabenGeprueft = true
            Logbuch.dauer("Lage geprüft", System.currentTimeMillis() - jetzt, "${lage.count { it.value != null }} Störung(en)")
        }
    }

    // ---- Gespräch ----

    /** Gibt false zurück, wenn die Frage nicht angenommen wurde (leer, Jarvis denkt noch, nicht verbunden). */
    fun sende(eingabe: String, anhaenge: List<de.frank.jarvis.data.Anhang> = emptyList(), diktiert: Boolean = false): Boolean {
        // Vorgelesen wird nur die Antwort auf genau diese gesprochene Frage. Endet sie vorzeitig (Abbruch, leer, nicht
        // verbunden), darf die nächste getippte Antwort nicht laut werden.
        val vorlesen = gesprochen
        gesprochen = false
        // Ob die Antwort laut wird: gesprochene Frage (Hey Jarvis, Gesprächsmodus) oder eingesprochen und von Hand gesendet.
        // Nie im Live-Gespräch, das spricht selbst.
        val antwortLaut = !liveAktiv && ((vorlesen && einstellungen.antwortenVorlesen) || (diktiert && einstellungen.diktatAntwortVorlesen))
        val text = eingabe.trim().ifEmpty { if (anhaenge.isNotEmpty()) ANHANG_PLATZHALTER else "" }
        if (text.isEmpty()) return false
        // Wechsel zwischen Gesprächen erledigt die App selbst, auch während Jarvis im offenen Gespräch noch arbeitet.
        // Der Befehl geht nie ans Modell und kann deshalb keinen Programmierauftrag auslösen.
        if (anhaenge.isEmpty() && navigieren(text, vorlesen)) return true
        // Die Antwort, die bei „Hey Jarvis“ lief, weicht jetzt der neuen Frage (nur genau sie und nur, wenn sie noch läuft).
        weichendeRunde?.let { w -> weichendeRunde = null; if (vorlesen && sitzungId?.let { laufende[it] } === w) abbrechen() }
        if (denkt) return false
        if (!kiVerbunden) {
            meldung = "Bitte zuerst mit ChatGPT verbinden."
            reiter = Reiter.EINSTELLUNGEN
            return false
        }
        val verlauf = verlaufFuersModell()
        // Aus „In neuer Session weiterarbeiten“: Was die ursprüngliche Runde schon erledigt hatte, geht einmal mit (nur in diesem Gespräch).
        val erledigt = entwurfErledigt?.takeIf { it.sitzung == sitzungId && it.text == eingabe.trim() }?.erledigt.orEmpty()
        entwurfErledigt = null
        gespraech += Nachricht(true, text, anhaenge = anhaenge.map { it.pfad }, kontext = erledigt)
        sitzungSichern()
        rundeStarten(text, anhaenge, verlauf, gespraech.size - 1, antwortLaut,
            if (erledigt.isEmpty()) "" else "\n\n(Diese Frage lief schon einmal. Schon erledigt und NICHT noch einmal ausführen: $erledigt)")
        return true
    }

    /**
     * Startet die Antwort auf die Frage an Stelle [frage] im offenen Gespräch. [hinweis] geht zusätzlich ans Modell
     * (beim erneuten Versuch: was die verworfene Antwort schon erledigt hatte).
     */
    private fun rundeStarten(text: String, anhaenge: List<de.frank.jarvis.data.Anhang>, verlauf: List<ChatTurn>, frage: Int, vorlesen: Boolean, hinweis: String) {
        val plugins = aktivePlugins.toList()
        val id = sitzungId ?: return
        val runde = Runde(id, frage)
        laufende[id] = runde
        // Die Kennung des Gesprächs läuft im Koroutinen-Kontext mit: Startet Jarvis in dieser Runde einen Programmierauftrag,
        // kommen dessen Meldungen in genau dieses Gespräch, auch wenn inzwischen andere Gespräche arbeiten.
        // Franks eigene Worte (diese Frage und seine vorige, getippt oder eingesprochen): Nur daran entscheidet programmier_nachtrag,
        // ob er einen Nachtrag ausdrücklich verlangt hat. Die erste Nachricht eines Auftrags-Gesprächs ist der Auftrag selbst, nicht Franks Wort.
        val worte = gespraech.take(frage + 1).filter { it.vonMir && !de.frank.jarvis.faehigkeit.ProgrammierChat.istAuftragsVorspann(it.text) }
            .takeLast(2).joinToString("\n") { it.text }
        runde.job = viewModelScope.launch(de.frank.jarvis.faehigkeit.ProgrammierChat.GespraechKennung(id) + de.frank.jarvis.faehigkeit.ProgrammierChat.FranksWorte(worte, streng = true)) {
            var art = "antwort"
            val antwort = try {
                val beilage = if (anhaenge.isEmpty()) null else withContext(Dispatchers.IO) { de.frank.jarvis.data.Anhaenge.fuersModell(anhaenge) }
                val auftrag = text + beilage?.text.orEmpty() + (if (plugins.isEmpty()) "" else "\n\n(Gewählte Plugins: " + plugins.joinToString(", ") + ")") + hinweis
                // Franks Schrittlimit gilt hier; das Zeitlimit wächst mit (je Schritt 12 s, mindestens 2 Minuten; unbegrenzt = 30 Minuten, also mehr als die 20 Minuten bei 100 Schritten).
                val schritte = einstellungen.schrittlimit
                val zeitlimit = if (schritte <= 0) 30 * 60_000L else maxOf(120_000L, schritte * 12_000L)
                var grund: String? = null
                agent.versuche(auftrag, verlauf, zeitlimitMs = zeitlimit, maxSchritte = schritte, beiSchritt = { runde.schritt = it }, lernen = true, abschluss = true,
                    beiAenderung = { runde.erledigt += it }, bilder = beilage?.bilder.orEmpty(), beiAbbruch = { grund = it })
                    ?: (if (!agent.verbunden) JarvisAgent.NICHT_VERBUNDEN else agent.ersatzText(grund)).also { art = "fehler" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logbuch.fehler("Gespräch: Antwort", e)
                art = "fehler"
                e.message ?: "Das hat nicht geklappt."
            }
            rundeFertig(runde, antwort, art, vorlesen)
        }
    }

    // ---- Aktionen unter einer Antwort ----

    /** Ein Daumen für die Antwort an Stelle [i]: +1, -1 oder 0 (zurücknehmen). [grund] beim Daumen runter (Kennung aus Stil.GRUENDE). */
    fun bewerten(i: Int, urteil: Int, grund: String = "") {
        val n = gespraech.getOrNull(i) ?: return
        if (n.vonMir) return
        gespraech[i] = n.copy(bewertung = urteil)
        sitzungSichern()
        // Der Abschlussbericht stammt von der Programmier-Sitzung, nicht von Jarvis: Er lehrt Jarvis keinen Stil.
        if (urteil == 0 || n.art == de.frank.jarvis.faehigkeit.ProgrammierChat.ART_BERICHT || n.art == de.frank.jarvis.faehigkeit.ProgrammierChat.ART_AGENT) return
        val frage = (i - 1 downTo 0).firstOrNull { gespraech[it].vonMir }?.let { gespraech[it].text }.orEmpty()
        val modell = einstellungen.modell
        val stufe = einstellungen.denkstufe
        viewModelScope.launch {
            withContext(Dispatchers.IO) { de.frank.jarvis.data.Stil.bewerten(getApplication(), urteil, grund, frage, n.text, modell, stufe) }
            meldung = if (urteil > 0) "Danke! Gemerkt, dass dir diese Antwort gefällt." else "Danke, gemerkt. Daraus lerne ich, wie du Antworten lieber hättest."
            if (de.frank.jarvis.data.Stil.neueSeitAuswertung(getApplication()) >= de.frank.jarvis.data.Stil.AB) stilAuswerten(still = true)
        }
    }

    var stilWertetAus by mutableStateOf(false); private set

    /** Leitet aus den Bewertungen Stilregeln ab (Gedächtnis → Stil). [still] = von selbst, dann nur bei Änderungen melden. */
    fun stilAuswerten(still: Boolean = false) {
        if (stilWertetAus) return
        if (!kiVerbunden) { if (!still) meldung = "Für die Stil-Auswertung bitte zuerst mit ChatGPT verbinden."; return }
        if (de.frank.jarvis.data.Stil.anzahl(getApplication()) < de.frank.jarvis.data.Stil.AB) {
            if (!still) meldung = "Noch keine Bewertungen: Tippe unter einer Antwort auf Daumen hoch oder runter."
            return
        }
        stilWertetAus = true
        viewModelScope.launch {
            val ergebnis = runCatching { de.frank.jarvis.data.Stil.auswerten(getApplication(), auth, einstellungen.modell) }.getOrNull()
            stilWertetAus = false
            stilListe.laden()
            if (ergebnis != null && (!still || !ergebnis.contains("nichts Neues"))) meldung = ergebnis
            else if (ergebnis == null && !still) meldung = "Die Stil-Auswertung hat gerade nicht geklappt."
        }
    }

    /**
     * In neuem Chat fortsetzen: ein neues Gespräch mit den Nachrichten bis einschließlich [bis]. Anhänge werden kopiert,
     * damit das Löschen des einen Gesprächs die Bilder des anderen nicht mitnimmt. Der Verlauf gilt im Moment des Aufrufs;
     * [danach] läuft, sobald das neue Gespräch offen ist (die Kopien entstehen im Hintergrund).
     */
    fun abzweigen(bis: Int, meldenAls: String = "Neuer Chat ab dieser Stelle.", danach: suspend () -> Unit = {}) {
        // bis = -1: ein leeres neues Gespräch.
        if (bis < -1 || bis >= gespraech.size) return
        val stand = gespraech.take(bis + 1)
        viewModelScope.launch {
            // Die Anhänge (oft mehrere MB große Fotos) abseits des Hauptfadens kopieren: Sonst stockt die Oberfläche.
            val kopie = withContext(Dispatchers.IO) { stand.map { n -> n.copy(bewertung = 0, anhaenge = n.anhaenge.mapNotNull { anhangKopieren(it) }) } }
            neuesGespraech()
            gespraech.addAll(kopie)
            // Ein leeres Gespräch bekommt erst mit der ersten Frage eine Kennung und eine Datei.
            if (kopie.isNotEmpty()) sitzungSichern()
            meldung = meldenAls
            danach()
        }
    }

    /**
     * Mit einer eigenen Frage in einer neuen, getrennten Session weiterarbeiten: Das neue Gespräch bekommt den Verlauf vor
     * dieser Frage (nichts danach), die Frage selbst samt Anhängen liegt fertig in der Eingabeblase. So läuft nichts doppelt,
     * und Frank kann sie abschicken oder vorher ändern. Die alte Session und laufende Aufträge bleiben unberührt.
     */
    fun mitFrageWeiterarbeiten(i: Int) {
        val n = gespraech.getOrNull(i)?.takeIf { it.vonMir } ?: return
        // Arbeitet Jarvis gerade an genau dieser Frage, liefe sie nach dem Abschicken doppelt. Nach der Stelle gefragt, nicht nach
        // „letzte Nachricht“: Statusmeldungen eines Programmierauftrags können während der Runde schon dahinter stehen.
        if (sitzungId?.let { laufende[it] }?.frage == i) { meldung = "Jarvis arbeitet noch an dieser Frage."; return }
        // Was eine abgebrochene Runde zu dieser Frage schon erledigt hat, geht beim Abschicken mit ans Modell (nicht doppelt ausführen).
        val erledigt = n.kontext.takeIf { it.contains("Bereits erledigt") }.orEmpty()
        abzweigen(i - 1, "Neue Session: Deine Frage steht bereit.") {
            // Kopieren abseits des Hauptfadens, wie in [abzweigen].
            val bilder = withContext(Dispatchers.IO) { n.anhaenge.mapNotNull { anhangKopieren(it) } }
            val fehlen = n.anhaenge.size - bilder.size
            // Ein noch nicht abgeschickter Entwurf weicht der Frage; seine Anhangdateien werden mit entfernt.
            anhaenge.toList().forEach { anhangWeg(it) }
            entwurfDiktiert = false
            entwurf = if (n.anhaenge.isNotEmpty() && n.text == ANHANG_PLATZHALTER) "" else n.text
            bilder.forEach { anhaenge.add(de.frank.jarvis.data.Anhang(it)) }
            entwurfErledigt = if (erledigt.isEmpty()) null else EntwurfNotiz(sitzungId, entwurf.trim(), erledigt)
            meldung = "Neue Session: Deine Frage steht bereit" + (when (fehlen) { 0 -> "."; 1 -> ", 1 Anhang fehlt."; else -> ", $fehlen Anhänge fehlen." }) +
                (if (erledigt.isNotEmpty()) " Teile davon waren schon erledigt, Jarvis weiß Bescheid." else "")
        }
    }

    /** Erledigt-Notiz der Frage, die [mitFrageWeiterarbeiten] in die Eingabe gelegt hat. */
    private class EntwurfNotiz(val sitzung: String?, val text: String, val erledigt: String)

    /** Gilt nur, wenn genau diese Frage unverändert in genau diesem Gespräch abgeschickt wird. */
    private var entwurfErledigt: EntwurfNotiz? = null

    private fun anhangKopieren(pfad: String): String? = runCatching {
        val alt = java.io.File(pfad)
        // Bilder und PDFs von Jarvis (vorläufig oder in der Ablage) gehören ihrem Speicher, nicht dem Gespräch: Verweis übernehmen.
        // Eine Kopie daneben kennte der Speicher nicht, und sein Aufräumen löschte sie.
        if (!alt.absolutePath.startsWith(de.frank.jarvis.data.Anhaenge.ordner(getApplication()).absolutePath)) return@runCatching pfad
        if (!alt.exists()) return@runCatching null
        // Freien Namen suchen: Viele Kopien in derselben Millisekunde dürfen nicht kollidieren (copyTo überschreibt nicht).
        val basis = System.currentTimeMillis()
        val neu = generateSequence(0) { it + 1 }.map { java.io.File(alt.parentFile, "${basis + it}_" + alt.name.substringAfter('_')) }.first { !it.exists() }
        alt.copyTo(neu)
        neu.path
    }.getOrNull()

    /**
     * Erneut versuchen: Die letzte Antwort wird ersetzt. Bei einer älteren Antwort geht es in einem neuen Chat ab der
     * Frage weiter, damit nichts verloren geht. Was die verworfene Antwort schon erledigt hat, wird nicht doppelt ausgeführt.
     */
    fun erneutVersuchen(i: Int) {
        if (denkt) { meldung = "Jarvis arbeitet noch."; return }
        if (!kiVerbunden) { meldung = "Bitte zuerst mit ChatGPT verbinden."; return }
        val alt = gespraech.getOrNull(i) ?: return
        if (alt.vonMir) return
        // Der Abschlussbericht und Statusmeldungen einer Programmier-Sitzung sind keine Antwort von Jarvis: Ein neuer Versuch
        // schickte die Frage erneut ab, die den Programmierauftrag gestartet hat, und startete ihn ein zweites Mal.
        if (alt.art == de.frank.jarvis.faehigkeit.ProgrammierChat.ART_BERICHT || alt.art == de.frank.jarvis.faehigkeit.ProgrammierChat.ART_STATUS || alt.art == "rueckfrage") {
            meldung = "Das ist eine Meldung der Programmier-Sitzung, keine Antwort von Jarvis. Für einen neuen Auftrag frag Jarvis direkt."
            return
        }
        // Ergebnis eines Hintergrundauftrags oder ein fertiges Bild: Ein neuer Versuch startete Auftrag bzw. Bild ein zweites Mal.
        if (alt.art == de.frank.jarvis.faehigkeit.ProgrammierChat.ART_AGENT || alt.art == de.frank.jarvis.faehigkeit.ProgrammierChat.ART_BILD) {
            meldung = "Das ist ein fertiges Ergebnis, keine Antwort zum Wiederholen. Für etwas Neues frag Jarvis direkt."
            return
        }
        val frage = (i - 1 downTo 0).firstOrNull { gespraech[it].vonMir } ?: run { meldung = "Zu dieser Antwort gibt es keine Frage."; return }
        // Erledigtes steht an der Antwort („Erledigt in dieser Antwort“) oder, nach einem Fehler oder Abbruch, an der Frage.
        val erledigt = listOfNotNull(alt.kontext.takeIf { it.contains("Erledigt") }, gespraech[frage].kontext.takeIf { it.contains("Bereits erledigt") })
            .joinToString(" ")
        val hinweis = "\n\n(Frank wollte eine neue Antwort auf diese Frage; die vorige wurde verworfen." +
            (if (erledigt.isNotEmpty()) " Schon erledigt und NICHT noch einmal ausführen: $erledigt" else "") + ")"
        // Die Frage selbst, nicht „die letzte Nachricht“: Statusmeldungen eines Programmierauftrags können dazwischen stehen.
        // Nach dem Abzweigen endet das neue Gespräch genau mit ihr, sonst steht sie unverändert an ihrer Stelle.
        val neuStarten = {
            val n = gespraech[frage]
            rundeStarten(n.text, n.anhaenge.map { de.frank.jarvis.data.Anhang(it) }, verlaufFuersModell(bis = frage), frage, vorlesen = false, hinweis = hinweis)
        }
        if (i != gespraech.lastIndex) {
            // Das Abzweigen kopiert die Anhänge im Hintergrund: Die neue Runde startet erst, wenn das neue Gespräch steht.
            abzweigen(frage, "Neuer Chat: Jarvis antwortet hier noch einmal neu.") { neuStarten() }
        } else {
            gespraech.removeAt(i)
            // Die Entfernung gleich speichern, ausdrücklich als Kürzung um diese eine Antwort: Sonst lehnte der Schutz gegen
            // ältere Stände jedes Speichern bis zur neuen Antwort ab (Abbruch, Daumen), und die verworfene Antwort käme zurück.
            sitzungSichern(kuerzenUm = 1)
            neuStarten()
        }
    }

    /** Die Antwort einer Runde ins Gespräch: sichtbar, wenn es gerade offen ist, sonst in seine gespeicherte Datei. */
    private fun rundeFertig(runde: Runde, antwort: String, art: String, vorlesen: Boolean) {
        // Schon abgebrochen (oder das Gespräch gelöscht): nichts mehr anhängen.
        if (laufende[runde.sitzung] !== runde) return
        laufende.remove(runde.sitzung)
        // Fehlermeldungen gehen nicht ans Modell: Was in der Runde schon erledigt wurde, hängt deshalb an der Frage
        // (wie bei einem Abbruch), sonst führte „versuch es nochmal“ alles ein zweites Mal aus.
        val frageNotiz = if (art == "fehler") erledigtNotiz("Bereits erledigt, bevor die Runde abbrach", runde.erledigt) else ""
        val neu = Nachricht(false, antwort, art, if (art == "fehler") "" else erledigtNotiz("Erledigt in dieser Antwort", runde.erledigt))
        if (runde.sitzung == sitzungId) {
            rundeInsOffene(runde, frageNotiz, neu, vorlesen)
            return
        }
        // Frank ist inzwischen in einem anderen Gespräch: Die Antwort kommt in die gespeicherte Datei und steht beim nächsten Öffnen da.
        viewModelScope.launch {
            val offen = sicherung.withLock {
                // Unter der Sicherung erneut prüfen: Wurde genau dieses Gespräch inzwischen geöffnet (das Laden hält dieselbe
                // Sicherung), gehört die Antwort in die Ansicht. In die Datei geschrieben, überschriebe sie das nächste Speichern.
                if (runde.sitzung == sitzungId) true else {
                    withContext(Dispatchers.IO) {
                        if (frageNotiz.isNotEmpty()) de.frank.jarvis.data.Gespraeche.letzteFrageKontext(getApplication(), runde.sitzung, frageNotiz)
                        de.frank.jarvis.data.Gespraeche.anhaengen(getApplication(), runde.sitzung, de.frank.jarvis.data.GespeicherteNachricht(false, neu.text, neu.art, neu.kontext))
                    }
                    false
                }
            }
            if (offen) { rundeInsOffene(runde, frageNotiz, neu, vorlesen); return@launch }
            sitzungenLaden()
            de.frank.jarvis.gedaechtnis.Lagebild.anstossen(getApplication(), listOf(de.frank.jarvis.gedaechtnis.Bereich.GESPRAECHE), "Gespräch mit Jarvis")
        }
    }

    /** Hängt die Antwort einer Runde an das gerade offene Gespräch (das ihre ist) und liest sie bei Bedarf vor. */
    private fun rundeInsOffene(runde: Runde, frageNotiz: String, neu: Nachricht, vorlesen: Boolean) {
        val i = runde.frage
        val n = gespraech.getOrNull(i)
        if (frageNotiz.isNotEmpty() && n != null && n.vonMir) gespraech[i] = n.copy(kontext = frageNotiz)
        gespraech += neu
        sitzungSichern()
        if (vorlesen) vorleser.sprich("gespraech:" + (gespraech.size - 1), "Jarvis", neu.text)
    }

    private fun erledigtNotiz(kopf: String, erledigt: List<String>): String =
        synchronized(erledigt) { erledigt.toList() }.takeIf { it.isNotEmpty() }?.let { "($kopf: " + it.joinToString("; ") + ")" }.orEmpty()

    /**
     * Der Verlauf fürs Modell: Fehlermeldungen fallen weg (sie sind keine Antworten), unbeantwortete Fragen sind als solche
     * markiert, und der unsichtbare Kontext (was erledigt wurde) geht mit. Gekürzt wird nach Zeichen im Agenten.
     */
    private fun verlaufFuersModell(bis: Int = gespraech.size): List<ChatTurn> {
        val sicht = gespraech.take(bis).takeLast(MAX_VERLAUF).filter { it.art != "fehler" }
        return sicht.mapIndexed { i, n ->
            // Anhänge früherer Fragen gehen nur als Name mit, sonst wüchse jede Anfrage um alle alten Bilder.
            val mitAnhang = if (n.anhaenge.isEmpty()) n.text else n.text + "\n(Angehängt: " + n.anhaenge.joinToString(", ") { de.frank.jarvis.data.Anhang(it).name } + ")"
            val text = if (n.kontext.isEmpty()) mitAnhang else mitAnhang + "\n" + n.kontext
            if (n.vonMir) {
                val unbeantwortet = n.art == "abgebrochen" || i == sicht.lastIndex || sicht[i + 1].vonMir
                ChatTurn("user", if (unbeantwortet) "$text\n(Diese Frage blieb unbeantwortet. Geh nur darauf ein, wenn Frank sich darauf bezieht.)" else text)
            } else {
                ChatTurn("assistant", text)
            }
        }
    }

    // ---- Mikrofon und Vorlesen ----

    // ---- Jarvis' Gedächtnis: Regeln, Ziele, Wissen über Frank ----

    /** Eine der beiden Merkdateien samt dem, was ihr Bildschirm braucht. */
    inner class Merkliste(
        private val datei: de.frank.jarvis.faehigkeit.Merkdatei,
        val titel: String,
        /** „Regel“ oder „Notiz“, für Meldungen und Beschriftungen. */
        val wort: String,
        val erklaerung: String,
        val platzhalter: String,
        private val korrekturAuftrag: String,
        /** true = älteste id zuerst (Regelwerk nach Priorität), sonst das Neueste zuerst. */
        private val aufsteigend: Boolean = false,
    ) {
        var eintraege by mutableStateOf(emptyList<de.frank.jarvis.faehigkeit.Regel>()); private set
        private fun geordnet(l: List<de.frank.jarvis.faehigkeit.Regel>) = if (aufsteigend) l.sortedBy { it.id } else l.sortedByDescending { it.id }
        /** Der in diesem Bildschirm zuletzt hinzugefügte Eintrag: nur er bekommt die KI-Korrektur. */
        var letzter by mutableStateOf<Int?>(null); private set
        var korrigiert by mutableStateOf(false); private set
        /** Die letzten eigenen Änderungen als Umkehr-Schritte für „Rückgängig“. Was Plugin oder Lernlauf inzwischen geändert haben, bleibt dabei stehen. */
        private val verlauf = mutableStateListOf<MerkSchritt>()
        val rueckgaengigMoeglich: Boolean get() = verlauf.isNotEmpty()

        init {
            // Schreibt irgendwer eine Merkdatei (Plugin, Lernlauf, Jarvis im Gespräch), zeigt die Liste sofort den neuen Stand.
            viewModelScope.launch {
                Merkdatei.stand.collect {
                    val neu = geordnet(withContext(Dispatchers.IO) { datei.alle(getApplication()) })
                    eintraege = neu
                    if (neu.none { it.id == letzter }) letzter = null
                }
            }
        }

        fun laden() { eintraege = geordnet(datei.alle(getApplication())) }

        private fun merke(schritt: MerkSchritt) {
            verlauf += schritt
            if (verlauf.size > 20) verlauf.removeAt(0)
        }

        fun neu(text: String) {
            if (text.isBlank()) return
            val eintrag = datei.speichere(getApplication(), text)
            if (eintrag == null) {
                meldung = when {
                    datei.defekt(getApplication()) != null -> "Die Datei ist beschädigt und wird nicht überschrieben (gesichert als .defekt). Details unter Aktivität."
                    datei.voll(getApplication()) -> "Die Datei ist voll. Lösche zuerst einen überholten Eintrag."
                    else -> "Der Eintrag wurde nicht gespeichert."
                }
                return
            }
            letzter = eintrag.id
            merke(MerkSchritt.Angelegt(eintrag))
            laden()
        }

        /**
         * Speichert eine Bearbeitung nur, wenn der Eintrag noch [ursprung] lautet (der Text beim Öffnen des Editors).
         * Hat ihn inzwischen jemand anders geändert, gewinnt die neuere Fassung und Frank bekommt Bescheid.
         */
        fun aendern(id: Int, text: String, ursprung: String? = null): Boolean {
            if (text.isBlank()) { meldung = "Ein leerer Eintrag geht nicht. Zum Entfernen bitte löschen."; return false }
            val alt = ursprung ?: eintraege.firstOrNull { it.id == id }?.text
            val neu = alt?.let { datei.ersetzeWenn(getApplication(), id, it, text) }
            if (alt == null || neu == null) {
                laden()
                meldung = if (eintraege.any { it.id == id }) "Der Eintrag wurde inzwischen geändert. Dein Entwurf ist noch da; nochmal Speichern überschreibt."
                    else "Der Eintrag wurde inzwischen gelöscht. Du siehst jetzt den neuen Stand."
                return false
            }
            if (neu.text != alt) merke(MerkSchritt.Geaendert(id, alt, neu.text))
            laden()
            return true
        }

        fun loeschen(id: Int) {
            val weg = datei.loesche(getApplication(), id)
            if (weg == null) { laden(); meldung = "Diesen Eintrag gibt es schon nicht mehr."; return }
            merke(MerkSchritt.Geloescht(weg))
            if (letzter == id) letzter = null
            laden()
            meldung = "$wort gelöscht. „Rückgängig“ holt den Eintrag zurück."
        }

        /** Nimmt die letzte eigene Änderung zurück, und nur sie: Einträge, die seither dazukamen, bleiben. */
        fun rueckgaengig() {
            val schritt = verlauf.removeLastOrNull() ?: return
            val app = getApplication<Application>()
            val geklappt = when (schritt) {
                is MerkSchritt.Angelegt ->
                    datei.alle(app).firstOrNull { it.id == schritt.regel.id }?.takeIf { it.text == schritt.regel.text }?.let { datei.loesche(app, it.id) } != null
                is MerkSchritt.Geaendert -> datei.ersetzeWenn(app, schritt.id, schritt.neuerText, schritt.alterText) != null
                is MerkSchritt.Geloescht -> datei.stelleWiederHer(app, schritt.regel) != null
            }
            laden()
            if (eintraege.none { it.id == letzter }) letzter = null
            meldung = if (geklappt) "Letzte Änderung zurückgenommen." else "Das ließ sich nicht zurücknehmen: Der Eintrag wurde inzwischen anderweitig geändert."
        }

        /** Formuliert den zuletzt hinzugefügten Eintrag um, und nur ihn. */
        fun korrektur() {
            val eintrag = eintraege.firstOrNull { it.id == letzter } ?: return
            if (korrigiert) return
            if (!kiVerbunden) { meldung = "Für die KI-Korrektur bitte zuerst mit ChatGPT verbinden."; return }
            korrigiert = true
            viewModelScope.launch {
                try {
                    val neu = auth.streamChat(korrekturAuftrag, listOf(ChatTurn("user", eintrag.text)), einstellungen.modell, einstellungen.denkstufe).trim().trim('"', '„', '“')
                    when {
                        neu.length < 10 -> meldung = "Die KI-Korrektur hat nichts Brauchbares geliefert. Der Text bleibt, wie er ist."
                        neu == eintrag.text -> meldung = "Das ist schon klar formuliert."
                        else -> {
                            // Nur schreiben, wenn der Eintrag noch der ist, den die KI gelesen hat: sonst wäre eine neuere Fassung weg.
                            val ersetzt = datei.ersetzeWenn(getApplication(), eintrag.id, eintrag.text, neu)
                            if (ersetzt == null) {
                                meldung = "Der Eintrag wurde inzwischen geändert oder gelöscht. Die Korrektur ist verworfen."
                            } else {
                                merke(MerkSchritt.Geaendert(eintrag.id, eintrag.text, ersetzt.text))
                                meldung = "$wort neu formuliert."
                            }
                            laden()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logbuch.fehler("KI-Korrektur", e)
                    meldung = e.message ?: "Die KI-Korrektur hat nicht geklappt."
                } finally {
                    korrigiert = false
                }
            }
        }
    }

    val regelListe = Merkliste(
        de.frank.jarvis.faehigkeit.Regeln, "Franks Regeln", "Regel",
        "Diese Regeln beachtet Jarvis bei jeder Antwort, bei seinen Agenten und in der Tagesauswertung. Er lernt sie auch selbst, wenn du ihm sagst, wie du etwas künftig haben möchtest.",
        "Neue Regel eintippen oder einsprechen", REGEL_KORREKTUR,
    )
    val zielListe = Merkliste(
        de.frank.jarvis.faehigkeit.Ziele, "Franks Ziele", "Ziel",
        "Das willst du erreichen. Jarvis misst seine Deutungen und Vorschläge daran, auch in der Tagesauswertung. Er merkt sich Ziele auch selbst, wenn du sie nennst.",
        "Neues Ziel eintippen oder einsprechen", ZIEL_KORREKTUR,
    )
    val infoListe = Merkliste(
        de.frank.jarvis.faehigkeit.UeberFrank, "Über Frank", "Notiz",
        "Das weiß Jarvis über dich und hat es bei jeder Antwort, bei seinen Agenten und in der Tagesauswertung vor Augen. Er notiert es auch selbst, wenn du etwas über dich erzählst.",
        "Neue Notiz über dich eintippen oder einsprechen", INFO_KORREKTUR,
    )
    val programmierListe = Merkliste(
        de.frank.jarvis.faehigkeit.Programmierregeln, "Programmierregeln", "Programmierregel",
        "Diese Regeln sind nur für Jarvis selbst: Er hat sie bei jeder Antwort vor Augen und richtet Programmieraufträge danach aus. An den Programmierer (PC, Mac oder Cloud) gehen sie nie mit, weder im Auftrag noch über das Live-Gedächtnis.",
        "Neue Programmierregel eintippen oder einsprechen", PROGRAMMIERREGEL_KORREKTUR,
    )
    val stilListe = Merkliste(
        de.frank.jarvis.faehigkeit.StilRegeln, "Stil", "Stilregel",
        "So sollen Jarvis' Antworten aussehen: Länge, Gliederung, Ton und Symbole. Jarvis lernt das aus deinen Daumen hoch und runter unter den Antworten; du kannst alles ändern, löschen oder selbst eintragen.",
        "Neue Stilregel eintippen oder einsprechen", STIL_KORREKTUR,
    )
    val symbioseListe = Merkliste(
        de.frank.jarvis.faehigkeit.Symbiose, "Zusammenarbeit mit ChatGPT", "Regel",
        "Die Regeln, nach denen ChatGPT Live und Jarvis zusammenarbeiten, nach Wichtigkeit geordnet (oben das Wichtigste). Derselbe Text gehört 1:1 in ChatGPT unter Einstellungen → Personalisierung → " +
            "Benutzerdefinierte Anweisungen; „Text für ChatGPT kopieren“ legt ihn in die Zwischenablage. Nach jeder Änderung neu kopieren: ChatGPT erkennt eine alte Kopie an der Fassung.",
        "Neue Regel für die Zusammenarbeit eintippen oder einsprechen", SYMBIOSE_KORREKTUR, aufsteigend = true,
    )
    val entscheidungsListe = Merkliste(
        de.frank.jarvis.faehigkeit.Entscheidungen, "Entscheidungen", "Entscheidung",
        "Was du ausdrücklich entschieden hast, mit Datum. Jarvis und ChatGPT halten das fest, damit ein neues Gespräch nicht wieder bei null anfängt. Nur deine eigenen Beschlüsse, keine Vorschläge.",
        "Neue Entscheidung eintippen oder einsprechen", ENTSCHEIDUNG_KORREKTUR,
    )
    /** Die Merkliste, die gerade im Vollbild offen ist. */
    var merkOffen by mutableStateOf<Merkliste?>(null); private set
    /** Wohin der Text der laufenden Aufnahme geht, wenn nicht ins Gespräch. */
    private var diktatZiel: ((String) -> Unit)? = null
    /** Die laufende Aufnahme kam vom Diktier-Mikrofon der Eingabeblase: Der Text geht ins Feld statt direkt an Jarvis. */
    private var diktatInsFeld = false

    // ---- Eingabeblase: Entwurf, Anhänge, Diktat ----

    /** Der Text in der Eingabeblase. Im ViewModel, damit jedes Diktat ihn hinten ergänzen kann. */
    var entwurf by mutableStateOf("")
    /** Fotos und Dateien, die mit der nächsten Frage an Jarvis gehen. */
    val anhaenge = mutableStateListOf<de.frank.jarvis.data.Anhang>()
    var kameraAnfragen: () -> Unit = {}
    /** Wohin die Kamera das Foto schreibt; im ViewModel, damit es ein Auf- oder Zuklappen während der Aufnahme übersteht. */
    var kameraZiel: android.net.Uri? = null
    var fotosAnfragen: () -> Unit = {}
    var dateienAnfragen: () -> Unit = {}
    /** Gewählte Plugins (vorbereitet: Kennungen aus [de.frank.jarvis.ui.Plugins.alle]). */
    val aktivePlugins = mutableStateListOf<String>()

    /** Übernimmt Fotos oder Dateien als Anhang der nächsten Frage. */
    fun anhangHinzu(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val neu = withContext(Dispatchers.IO) { uris.take(10).mapNotNull { de.frank.jarvis.data.Anhaenge.uebernehmen(getApplication(), it) } }
            if (neu.size < uris.size.coerceAtMost(10)) meldung = "Nicht jede Datei ließ sich übernehmen."
            anhaenge += neu
        }
    }

    fun anhangWeg(a: de.frank.jarvis.data.Anhang) { anhaenge.remove(a); runCatching { a.datei.delete() } }

    /** Senden aus der Eingabeblase: Text und Anhänge zusammen. */
    fun entwurfSenden() {
        val text = entwurf
        val anh = anhaenge.toList()
        if (text.isBlank() && anh.isEmpty()) return
        if (denkt) return
        // Kam Text ins Feld über das Diktier-Mikrofon, wird die erste Antwort darauf vorgelesen (Einstellung).
        val diktiert = entwurfDiktiert
        entwurf = ""
        anhaenge.clear()
        // Nicht angenommen (etwa nicht verbunden): Text und Anhänge bleiben in der Blase, statt verloren zu gehen.
        if (!sende(text, anh, diktiert)) {
            if (entwurf.isEmpty()) entwurf = text
            anhaenge.addAll(0, anh)
        } else entwurfDiktiert = false
    }

    /** Der aktuelle Entwurf enthält Text vom Diktier-Mikrofon; gilt nur für das nächste Absenden. */
    private var entwurfDiktiert = false

    /**
     * Diktier-Mikrofon der Eingabeblase: Tippen startet die Aufnahme, nochmal Tippen schreibt den Text ins Feld.
     * Jedes weitere Diktat kommt mit einem Leerzeichen hinter den vorhandenen Text; gesendet wird erst mit dem Pfeil.
     */
    fun diktatTippen() {
        when {
            schreibtMit -> Unit
            nimmtAuf -> aufnahmeBeenden()
            sprache.groqApiKey.isBlank() -> { meldung = "Für das Mikrofon bitte zuerst den Groq-Schlüssel eintragen."; reiter = Reiter.EINSTELLUNGEN }
            else -> {
                diktatZiel = { t -> entwurf = if (entwurf.isBlank()) t else entwurf.trimEnd() + " " + t; entwurfDiktiert = true }
                diktatInsFeld = true
                mikrofonAnfragen()
            }
        }
    }

    // ---- Live-Gespräch (OpenAI Realtime) ----

    var liveAktiv by mutableStateOf(false); private set
    var liveStatus by mutableStateOf(""); private set
    /** Tonweg, Ausgabegerät und Mikrofon, wie das System sie im Live-Gespräch tatsächlich nutzt. */
    var liveAudioweg by mutableStateOf(""); private set
    var livePegel by androidx.compose.runtime.mutableFloatStateOf(0f); private set
    var liveStumm by mutableStateOf(false); private set
    /** Was das laufende (oder zuletzt beendete) Live-Gespräch bisher gekostet hat, in USD. */
    var liveKosten by mutableStateOf(0.0); private set
    /** Summe aller Live-Gespräche seit dem letzten Zurücksetzen (Einstellungen → Live-Gespräch). */
    var liveKostenBuch by mutableStateOf(de.frank.jarvis.live.LiveKostenBuch.lesen(getApplication<android.app.Application>())); private set
    /**
     * Kosten je Gespräch (Start-Kennung), die noch nicht im [liveKostenBuch] stecken. Je Gespräch ein Eintrag: Endet eins
     * und das nächste startet, bevor das erste verbucht ist, fällt das erste nicht aus der Summe.
     */
    private val liveUnverbucht = androidx.compose.runtime.mutableStateMapOf<Int, Double>()
    /** Gesamtkosten aller Live-Gespräche seit dem Zurücksetzen, laufende genau einmal eingerechnet (Live-Leiste). */
    val liveKostenGesamt get() = liveKostenBuch.gesamtUsd + liveUnverbucht.values.sum()
    fun liveKostenZuruecksetzen() {
        liveKostenBuch = de.frank.jarvis.live.LiveKostenBuch.zuruecksetzen(getApplication<android.app.Application>())
        meldung = "Kostenzählung des Live-Gesprächs zurückgesetzt."
    }
    var liveMikroAnfragen: () -> Unit = {}
    private var live: de.frank.jarvis.live.LiveGespraech? = null
    /** Zählt Live-Starts: Ein verspäteter Start oder das Ende einer alten Verbindung darf eine neuere nicht anfassen. */
    private var liveStand = 0

    /** Tipp auf die orange Blase: Live-Gespräch starten oder beenden. */
    fun liveTippen() {
        when {
            liveAktiv -> liveBeenden()
            liveSchluessel().first.isBlank() -> {
                meldung = if (de.frank.jarvis.live.LiveOptionen.Stand(sprache).istGemini)
                    "Für Gemini Live bitte zuerst einen Gemini-Schlüssel aus AI Studio unter Einstellungen → Stimme eintragen."
                else "Für das Live-Gespräch bitte zuerst den OpenAI-API-Schlüssel unter Einstellungen → Stimme eintragen."
                einstellungsReiter = "stimme"
                reiter = Reiter.EINSTELLUNGEN
            }
            else -> liveMikroAnfragen()
        }
    }

    /**
     * Schlüssel für das eingestellte Live-Modell und (nur Gemini) der bezahlte Ersatz, falls das kostenlose Kontingent ausgeht.
     * „Erst kostenlos, dann bezahlt“ nimmt den bezahlten allein, wenn kein kostenloser eingetragen ist.
     */
    fun liveSchluessel(st: de.frank.jarvis.live.LiveOptionen.Stand = de.frank.jarvis.live.LiveOptionen.Stand(sprache)): Pair<String, String> {
        if (!st.istGemini) return sprache.openAiApiKey to ""
        val frei = sprache.geminiApiKey
        val bezahlt = sprache.geminiApiKeyBezahlt
        return when (st["g_schluessel"]) {
            "frei" -> frei to ""
            "bezahlt" -> bezahlt to ""
            else -> if (frei.isNotBlank()) frei to bezahlt else bezahlt to ""
        }
    }

    fun liveErlaubt(erlaubt: Boolean) {
        if (!erlaubt) { meldung = "Ohne Mikrofon-Erlaubnis kann ich dich nicht hören."; return }
        if (liveAktiv) return
        // Eine laufende Aufnahme oder der Gesprächsmodus endet zuerst ganz (Dienst stoppen, Mikrofon freigeben), erst dann
        // holt das Live-Gespräch das Mikrofon; sonst stoppte das verspätete Aufräumen den Dienst des Live-Gesprächs.
        val aufraeumen = if (nimmtAuf || holtMikro || weckwortLauf?.isActive == true) aufnahmeVerwerfen() else null
        vorleser.stopp()
        liveAktiv = true
        liveStumm = false
        liveStatus = "Verbinde …"
        liveAudioweg = ""
        val stand = ++liveStand
        // Sofort, nicht erst nach dem Mikrofon: Sonst zeigt die Leiste kurz die Zahlen des letzten Gesprächs.
        liveKosten = 0.0
        liveUnverbucht[stand] = 0.0
        viewModelScope.launch {
            aufraeumen?.join()
            de.frank.jarvis.weckwort.Weckwort.mikrofonHolen()
            // Ein neuerer Start gehört das Mikrofon schon: nichts freigeben, nichts starten. Wurde das Gespräch dagegen beendet,
            // während das Aufräumen noch lief (liveStand zählt auch das Ende), hält niemand mehr das Mikrofon: freigeben,
            // sonst bliebe „Hey Jarvis“ taub.
            if (stand != liveStand) { liveUnverbucht.remove(stand); if (!liveAktiv && !nimmtAuf && !holtMikro && weckwortLauf?.isActive != true) de.frank.jarvis.weckwort.Weckwort.mikrofonFreigeben(); return@launch }
            if (!liveAktiv) { liveUnverbucht.remove(stand); de.frank.jarvis.weckwort.Weckwort.mikrofonFreigeben(); return@launch }
            de.frank.jarvis.audio.AufnahmeDienst.starten(getApplication())
            // Wie im Gesprächsmodus: Andere Apps bleiben leise, sonst hört das Mikrofon die Musik und das Live-Gespräch antwortet darauf.
            de.frank.jarvis.speech.GespraechsFokus.halten(getApplication())
            val haupt: (() -> Unit) -> Unit = { block -> viewModelScope.launch(Dispatchers.Main) { if (stand == liveStand) block() } }
            val optionen = de.frank.jarvis.live.LiveOptionen.Stand(sprache)
            val mitgeben = optionen["verlauf"].toIntOrNull() ?: 0
            val bisher = gespraech.filter { it.art == "antwort" && it.text.isNotBlank() }.takeLast(mitgeben).map { it.vonMir to it.text }
            live = de.frank.jarvis.live.LiveGespraech(
                getApplication(), liveSchluessel(optionen).first, optionen, liveAnweisung(optionen.istLive, optionen.istGemini && optionen.jarvisDirekt), bisher,
                beiStatus = { s -> haupt { liveStatus = s } },
                beiPegel = { p -> livePegel = p },
                beiAudioweg = { a -> haupt { liveAudioweg = a } },
                beiNutzerText = { t -> haupt { liveNachricht(Nachricht(true, t)) } },
                beiJarvisText = { t -> haupt { liveNachricht(Nachricht(false, t)) } },
                beiWerkzeug = { id, auftrag -> haupt { liveWerkzeug(id, auftrag) } },
                // Die abgerechnete Dauer kommt bei GPT-Live erst nach dem Ende: auch dann noch übernehmen.
                beiKosten = { k -> viewModelScope.launch(Dispatchers.Main) {
                    if (stand == liveStand || !liveAktiv) liveKosten = k
                    // Nach dem Verbuchen nicht wieder aufnehmen: Dann steckt das Gespräch schon im Buch.
                    if (liveUnverbucht.containsKey(stand)) liveUnverbucht[stand] = k
                } },
                // Jedes Gespräch genau einmal ins dauerhafte Kostenbuch, auch wenn inzwischen ein neues läuft.
                beiAbrechnung = { a ->
                    de.frank.jarvis.live.LiveKostenBuch.buchen(getApplication<android.app.Application>(), a)
                    // Frisch lesen: Ein Zurücksetzen dazwischen darf nicht vom alten Stand überschrieben werden.
                    // Im selben Schritt als verbucht markieren, damit die Live-Leiste das Gespräch nie doppelt zählt.
                    viewModelScope.launch(Dispatchers.Main) {
                        liveKostenBuch = de.frank.jarvis.live.LiveKostenBuch.lesen(getApplication<android.app.Application>())
                        liveUnverbucht.remove(stand)
                    }
                },
                beiFehler = { m -> haupt { meldung = m } },
                beiEnde = { haupt { liveAufraeumen() } },
                ersatzSchluessel = liveSchluessel(optionen).second,
                schluesselGratis = optionen.istGemini && optionen["g_schluessel"] != "bezahlt" && liveSchluessel(optionen).first == sprache.geminiApiKey,
            ).also { it.start() }
        }
    }

    /** Welche Stimme gerade ihre Hörprobe holt (für die Sanduhr am Knopf). */
    var stimmProbeLaedt by mutableStateOf<String?>(null); private set

    /** Favoriten-Stimmen als `art_stimme`: mit Stern ganz oben in der Liste. */
    var stimmFavoriten by mutableStateOf(de.frank.jarvis.live.StimmProbe.favoriten(app)); private set

    /** Langes Drücken auf eine Stimme: Stern an oder wieder aus. */
    fun stimmFavoritUmschalten(art: String, stimme: String, titel: String) {
        stimmFavoriten = de.frank.jarvis.live.StimmProbe.favoritUmschalten(getApplication(), art, stimme)
        meldung = if (de.frank.jarvis.live.StimmProbe.favoritSchluessel(art, stimme) in stimmFavoriten) "★ $titel ist jetzt Favorit." else "$titel ist kein Favorit mehr."
    }

    /** Spielt die deutsche Hörprobe einer Live-Stimme; die erste je Stimme kommt einmalig von OpenAI. */
    fun stimmProbe(art: String, stimme: String) {
        if (liveAktiv) { meldung = "Während des Live-Gesprächs gibt es keine Hörprobe."; return }
        if (stimmProbeLaedt != null) return
        val neu = !de.frank.jarvis.live.StimmProbe.vorhanden(getApplication(), art, stimme)
        if (neu) { stimmProbeLaedt = stimme; meldung = "Hörprobe für $stimme wird einmalig erzeugt …" }
        viewModelScope.launch {
            // Wirft das Abspielen (etwa AudioTrack nicht verfügbar), darf weder die App abstürzen noch die Sanduhr hängen bleiben.
            val fehler = try {
                de.frank.jarvis.live.StimmProbe.abspielen(getApplication(), liveSchluessel().first, art, stimme, liveSchluessel().second)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logbuch.fehler("Hörprobe", e)
                "Die Hörprobe ließ sich nicht abspielen."
            } finally {
                stimmProbeLaedt = null
            }
            if (fehler != null) meldung = fehler
        }
    }

    fun liveStummSchalten() { liveStumm = !liveStumm; live?.stumm = liveStumm }

    /** Antippen der Live-Blase: Jarvis verstummt sofort und hört wieder zu. */
    fun liveUnterbrechen() { live?.unterbrechen() }

    fun liveBeenden() { live?.beenden() ?: liveAufraeumen() }

    private fun liveAufraeumen() {
        if (!liveAktiv) return
        liveStand++
        liveAktiv = false
        live = null
        livePegel = 0f
        liveStatus = ""
        liveAudioweg = ""
        de.frank.jarvis.audio.AufnahmeDienst.stoppen()
        de.frank.jarvis.weckwort.Weckwort.mikrofonFreigeben()
        de.frank.jarvis.speech.GespraechsFokus.freigeben()
        viewModelScope.launch {
            // Kurz warten: GPT-Live meldet die endgültige Dauer erst nach dem Schließen.
            kotlinx.coroutines.delay(2_500)
            if (!liveAktiv && liveKosten > 0) meldung = "Live-Gespräch beendet · ca. " + de.frank.jarvis.live.LiveOptionen.usdText(liveKosten)
        }
    }

    /** Franks letzte Äußerung im Live-Gespräch, im Wortlaut: Ersatz, falls das Modell einen Sessionbefehl beim Delegieren umformuliert. */
    private var liveAeusserung: String? = null

    private fun liveNachricht(n: Nachricht) {
        if (n.vonMir) liveAeusserung = n.text
        gespraech += n
        sitzungSichern()
        // „Bitte das Gespräch beenden“: in der App erkannt, ohne Modell. Das Live-Modell bestätigt sonst oft nur, ohne zu delegieren.
        if (n.vonMir && liveAktiv && SprachBefehle.istLiveEnde(n.text)) liveSprachEnde("gesagt: " + n.text)
    }

    /**
     * Frank will das Live-Gespräch beenden: Verbindung, Mikrofon und Hörer sofort zu, danach eine kurze Bestätigung vom
     * Vorleser. Das Mikrofon öffnet danach nicht von selbst; Gespräch, Agenten und Programmieraufträge laufen weiter.
     */
    private fun liveSprachEnde(anlass: String) {
        if (!liveAktiv) return
        sprachsitzungBeendet = true
        Logbuch.sprache("Live-Gespräch beendet", anlass)
        liveBeenden()
        // Eigene Quelle je Meldung: Dieselbe Quelle ein zweites Mal hielte das Vorlesen an, statt zu sprechen.
        vorleser.sprich("live-ende:" + System.currentTimeMillis(), "Jarvis", "Okay, Live-Gespräch beendet.")
    }

    /** Das Live-Modell fragt Jarvis mit allen Werkzeugen; die Antwort geht als Werkzeug-Ergebnis zurück und wird gesprochen. */
    private fun liveWerkzeug(callId: String, auftrag: String) {
        val sitzung = live
        // Genau einmal verbrauchen: Eine alte Äußerung darf nie an einem späteren Werkzeugaufruf hängen.
        val aeusserung = liveAeusserung
        liveAeusserung = null
        // Gesprächsende vor allem anderen und ohne Modell: Die Antwort des Agenten ginge sonst nur als Bestätigung zurück.
        if (listOfNotNull(auftrag, aeusserung).any { SprachBefehle.istLiveEnde(it) }) { liveSprachEnde("Werkzeugaufruf: $auftrag"); return }
        // Sessionwechsel laufen wie im Mikrofonpfad über [navigieren]: in der App, ohne Modell, nie als Programmierauftrag.
        val befehl = GespraechsNavigation.liveBefehl(auftrag, aeusserung, navigationsAuswahl)
        if (befehl != null) {
            if (navigiert) { if (live === sitzung) sitzung?.ergebnis(callId, "Moment, ich wechsle gerade die Session.", navigation = true); return }
            if (navigieren(befehl, vorlesen = false) { t -> if (live === sitzung) sitzung?.ergebnis(callId, t, navigation = true) }) return
        }
        // Wie im Mikrofonpfad: Die Rückfrage „Welche Session meinst du?“ gilt nur für die nächste Äußerung.
        navigationsAuswahl = null
        val verlauf = verlaufFuersModell()
        // „Jarvis direkt“: Modell und Denkstufe aus den Live-Einstellungen; hohe Stufen bekommen mehr Zeit.
        val optionen = de.frank.jarvis.live.LiveOptionen.Stand(sprache)
        val modell = de.frank.jarvis.live.LiveOptionen.jarvisModell(optionen)
        val denkstufe = de.frank.jarvis.live.LiveOptionen.jarvisDenkstufe(optionen)
        // Die Stufe, die wirklich ans Modell geht: „Wie im Chat“ zählt mit, und ein Modell ohne die Stufe fällt wie in der Anfrage zurück.
        val wirksameStufe = if (!optionen.jarvisDirekt) null
            else (modell ?: einstellungen.modell).normalizeEffort(denkstufe ?: einstellungen.denkstufe)
        val zeitlimit = if (wirksameStufe != null && wirksameStufe >= de.frank.jarvis.auth.ReasoningEffort.HIGH) 120_000L else 60_000L
        // Wie in [sende]: Startet Jarvis hier einen Programmierauftrag, kommen dessen Meldungen in dieses Gespräch.
        val kennung = sitzungId?.let { de.frank.jarvis.faehigkeit.ProgrammierChat.GespraechKennung(it) } ?: kotlin.coroutines.EmptyCoroutineContext
        // Live: die Mitschrift von Franks Äußerung für die Nachtrags-Prüfung (unsicher, daher nicht streng).
        val worte = aeusserung?.let { de.frank.jarvis.faehigkeit.ProgrammierChat.FranksWorte(it, streng = false) } ?: kotlin.coroutines.EmptyCoroutineContext
        viewModelScope.launch(kennung + worte) {
            val antwort = runCatching {
                // Live (jarvis_fragen): fest 8 Schritte, unabhängig von Franks Chat-Schrittlimit; das Zeitlimit oben bleibt.
                agent.versuche(auftrag.ifBlank { "Antworte auf Franks letzte Äußerung im Gespräch." }, verlauf, lernen = true, abschluss = true, zeitlimitMs = zeitlimit,
                    maxSchritte = JarvisAgent.STANDARD_SCHRITTE, modell = modell, denkstufe = denkstufe, schnell = optionen.jarvisDirekt)
            }.getOrNull() ?: "Das konnte ich gerade nicht nachschlagen."
            // Nur an dieselbe Verbindung: Eine neue kennt diese Kennung nicht.
            if (live === sitzung) sitzung?.ergebnis(callId, antwort)
        }
    }

    /**
     * Anweisung für das Sprachmodell. GPT-Live delegiert, Realtime und Gemini rufen das Werkzeug selbst.
     * [nurStimme]: Gemini mit „Jarvis direkt“ gibt jede Äußerung an Jarvis weiter und spricht nur dessen Antwort.
     */
    private fun liveAnweisung(gptLive: Boolean, nurStimme: Boolean = false): String =
        if (nurStimme) "# Rolle\nDu bist die Stimme von Jarvis, Franks persönlichem Assistenten auf seinem Handy, im Live-Sprachgespräch. Denken und Antworten übernimmt Jarvis.\n" +
            "# Sprache\nSprich immer Deutsch. Natürlich, warm und knapp: kurze Sätze, keine Aufzählungszeichen, keine Formatierung, keine Links und keine Kennungen vorlesen.\n" +
            "# Weitergeben\nZu JEDER Äußerung von Frank, die eine Antwort braucht (Fragen, Bitten, Aufträge, Sessionwechsel, Gesprächsende, auch Smalltalk), rufst du sofort " +
            "jarvis_fragen auf, mit seinem vollständigen Anliegen in seinen eigenen Worten und dem nötigen Zusammenhang aus dem Gespräch. Antworte nie selbst inhaltlich, " +
            "auch nicht, wenn du die Antwort zu wissen glaubst. Sag beim Aufruf höchstens ein kurzes „Moment“.\n" +
            "# Vorlesen\nKommt das Ergebnis von Jarvis, sprich es natürlich und sinngemäß vollständig vor. Füge nichts hinzu, lass nichts Wichtiges weg und bewerte es nicht. " +
            "Ist das Ergebnis eine Rückfrage, stell sie Frank genau so.\n" +
            "# Ausnahmen\nDie kurze Begrüßung zu Beginn und ein „Okay“ oder „Gern“ als Antwort auf ein bloßes Danke darfst du selbst sprechen.\n" +
            "# Unterbrechen\nFrank kann dich jederzeit unterbrechen; dann hörst du sofort auf und gehst auf ihn ein.\n" +
            "# Regeln\nErfinde nie Daten. Behaupte nie selbst, etwas erledigt, eine Session gewechselt oder das Gespräch beendet zu haben.\n" +
            "Jetzt ist " + java.text.SimpleDateFormat("EEEE, d. MMMM yyyy, HH:mm", java.util.Locale.GERMAN).format(java.util.Date()) + " Uhr."
        else
        "# Rolle\nDu bist Jarvis, Franks persönlicher Assistent auf seinem Handy, im Live-Sprachgespräch.\n" +
            "# Sprache\nSprich immer Deutsch, auch wenn Frank kurz Englisch spricht. Natürlich, warm und knapp wie ein guter Gesprächspartner: kurze Sätze, " +
            "keine Aufzählungszeichen, keine Formatierung, keine Links und keine Kennungen vorlesen.\n" +
            "# Unterbrechen\nFrank kann dich jederzeit unterbrechen; dann hörst du sofort auf und gehst auf ihn ein. Lass ihm Denkpausen.\n" +
            (if (gptLive) "# Nachschlagen\nFür alles, was Franks Daten, Apps oder Aktionen betrifft (Termine, Dienstplan, Aufgaben, Wecker, Timer, Biodaten, Schlaf, Ideen, Tagebuch, " +
                "Mails, Wetter, Fahrzeit, Gedächtnis, Programmieraufträge, Recherchen), delegierst du, bevor du antwortest. Sag dabei genau einen kurzen Satz wie „Moment, ich schaue nach“.\n" +
                "# Sessions\nWill Frank in eine andere Session (ein anderes Gespräch) wechseln, zurück in die letzte oder eine neue öffnen, etwa „Wechsle in die Whoop-Session“, " +
                "delegierst du das ebenfalls mit seinen genauen Worten; auch seine Antwort, wenn Jarvis fragt, welche Session er meint. Sag dann nur kurz, was Jarvis zurückmeldet. " +
                "Behaupte nie selbst, gewechselt zu haben.\n"
            else "# Werkzeug\nFür alles, was Franks Daten, Apps oder Aktionen betrifft (Termine, Dienstplan, Aufgaben, Wecker, Timer, Biodaten, Schlaf, Ideen, Tagebuch, Mails, Wetter, " +
                "Fahrzeit, Gedächtnis, Programmieraufträge, Recherchen), rufst du jarvis_fragen mit dem vollständigen Auftrag auf, bevor du antwortest. " +
                "Sag dabei genau einen kurzen Satz wie „Moment, ich schaue nach“.\n" +
                "# Sessions\nWill Frank in eine andere Session (ein anderes Gespräch) wechseln, zurück in die letzte oder eine neue öffnen, etwa „Wechsle in die Whoop-Session“, " +
                "rufst du jarvis_fragen mit seinen genauen Worten auf; auch mit seiner Antwort, wenn Jarvis fragt, welche Session er meint. Sag dann nur kurz, was Jarvis zurückmeldet. " +
                "Behaupte nie selbst, gewechselt zu haben.\n") +
            "# Beenden\nWill Frank das Gespräch beenden, etwa „Gespräch beenden“, „Beende das Live-Gespräch“ oder „Leg auf“, " +
                (if (gptLive) "delegierst du das sofort mit seinen genauen Worten" else "rufst du sofort jarvis_fragen mit seinen genauen Worten auf") +
                ". Jarvis schließt dann die Verbindung selbst. Sag vorher höchstens „Okay“ und behaupte nie selbst, das Gespräch sei beendet.\n" +
            "# Regeln\nErfinde nie Daten. Melde etwas erst als erledigt, wenn das Ergebnis da ist.\n" +
            // Franks Antwort-Stil (aus seinen Bewertungen) gilt auch im Gespräch, soweit er zum Sprechen passt.
            de.frank.jarvis.faehigkeit.StilRegeln.alle(getApplication()).takeIf { it.isNotEmpty() }
                ?.joinToString("\n", "# Franks Stil (gilt, soweit es zum Sprechen passt)\n", "\n") { "- " + it.text }.orEmpty() +
            "Jetzt ist " + java.text.SimpleDateFormat("EEEE, d. MMMM yyyy, HH:mm", java.util.Locale.GERMAN).format(java.util.Date()) + " Uhr."

    /** Der Symbiose-Text genau so, wie er in ChatGPT eingetragen wird (Überschrift mit Stand und Fassung, dann die Regeln). */
    fun symbioseText(): String = de.frank.jarvis.faehigkeit.Symbiose.fuerChatGpt(getApplication())

    /** Legt den Symbiose-Text in die Zwischenablage, damit Frank ihn in ChatGPT einfügen kann. */
    fun symbioseKopieren() {
        val text = symbioseText()
        val ablage = getApplication<Application>().getSystemService(android.content.ClipboardManager::class.java)
        ablage?.setPrimaryClip(android.content.ClipData.newPlainText("Jarvis-Symbiose für ChatGPT", text))
        val grenze = de.frank.jarvis.faehigkeit.Symbiose.GRENZE
        meldung = if (text.length > grenze) "Kopiert, aber ${text.length} von höchstens $grenze Zeichen: Für ChatGPT bitte erst Regeln kürzen."
            else "Kopiert (${text.length} von $grenze Zeichen). In ChatGPT unter Einstellungen → Personalisierung → Benutzerdefinierte Anweisungen einfügen und den alten Jarvis-Text ersetzen."
    }

    fun merkLaden() { regelListe.laden(); zielListe.laden(); infoListe.laden(); programmierListe.laden(); stilListe.laden(); entscheidungsListe.laden(); symbioseListe.laden() }
    fun merkOeffnen(liste: Merkliste) { liste.laden(); merkOffen = liste }
    fun merkSchliessen() {
        // Läuft noch ein Diktat für diese Liste, geht das Gesprochene wie erwartet hinein und die Aufnahme endet.
        if (nimmtAuf && diktatZiel != null) aufnahmeBeenden()
        merkOffen = null
    }

    /** Mikrofon im Vollbild einer Merkliste: Die Aufnahme wird als neuer Eintrag gespeichert, nicht an Jarvis geschickt. */
    fun merkMikrofonTippen(liste: Merkliste) {
        when {
            schreibtMit -> Unit
            nimmtAuf -> aufnahmeBeenden()
            sprache.groqApiKey.isBlank() -> meldung = "Für das Mikrofon bitte zuerst den Groq-Schlüssel in den Einstellungen eintragen."
            else -> { diktatZiel = liste::neu; mikrofonAnfragen() }
        }
    }

    fun mikrofonErlaubt(erlaubt: Boolean) {
        if (!erlaubt) { diktatZiel = null; diktatInsFeld = false; meldung = "Ohne Mikrofon-Erlaubnis kann ich dich nicht hören."; return }
        if (holtMikro) return
        vorleser.stopp()
        holtMikro = true
        viewModelScope.launch {
            try {
                // Von Hand gestartet: Die Aufnahme läuft weiter, auch wenn Frank in eine andere App wechselt, bis er
                // wieder auf das Mikrofon tippt. Dafür braucht Android einen Mikrofon-Dienst im Vordergrund.
                if (!aufnahmeStarten()) { diktatZiel = null; meldung = "Die Aufnahme ließ sich nicht starten." }
                else {
                    de.frank.jarvis.audio.AufnahmeDienst.starten(getApplication())
                    // Laufende Musik pausiert fürs Diktat, sonst landet sie in der Mitschrift.
                    de.frank.jarvis.speech.GespraechsFokus.halten(getApplication())
                }
            } finally {
                holtMikro = false
            }
        }
    }

    /** Wartet, bis „Hey Jarvis“ das Mikrofon losgelassen hat, und startet dann die Aufnahme. */
    private suspend fun aufnahmeStarten(): Boolean {
        val stand = wechselStand
        de.frank.jarvis.weckwort.Weckwort.mikrofonHolen()
        // Während des Wartens von Hand die Session gewechselt: Diese Aufnahme gehört zur alten und startet nicht mehr.
        if (stand != wechselStand) { de.frank.jarvis.weckwort.Weckwort.mikrofonFreigeben(); return false }
        if (mikro.start(viewModelScope)) { nimmtAuf = true; aufnahmeStand = stand; return true }
        de.frank.jarvis.weckwort.Weckwort.mikrofonFreigeben()
        return false
    }
    private var holtMikro = false
    /** Zählt Sessionwechsel von Hand. Jede Aufnahme merkt sich den Stand beim Start ([aufnahmeStand]) und gehört nur zu dieser Session. */
    private var wechselStand = 0
    private var aufnahmeStand = 0
    /** Die Mitschrift der zuletzt beendeten Aufnahme; ein Wechsel von Hand bricht sie ab. */
    private var mitschriftJob: Job? = null

    /**
     * Frank wechselt von Hand die Session (Seitenleiste, „Neues Gespräch“, Löschen): Aufnahme, Mitschrift und Gesprächsmodus
     * der alten Session enden sofort. Nichts davon darf in der neuen Session landen oder dort das Mikrofon wieder öffnen.
     * Laufende Antworten und Programmieraufträge der alten Session bleiben unberührt und schreiben in deren Datei.
     */
    private fun aufnahmeVerwerfen(): Job? {
        wechselStand++
        val aktiv = nimmtAuf || holtMikro || schreibtMit || weckwortLauf?.isActive == true
        weckwortLauf?.cancel()
        mitschriftJob?.cancel()
        mitschriftJob = null
        weichendeRunde = null
        if (!aktiv) return null
        nimmtAuf = false
        freihaendig = false
        schreibtMit = false
        diktatZiel = null
        Logbuch.sprache("Aufnahme verworfen", "Session von Hand gewechselt")
        return viewModelScope.launch {
            mikro.stop()
            de.frank.jarvis.audio.AufnahmeDienst.stoppen()
            de.frank.jarvis.weckwort.Weckwort.mikrofonFreigeben()
            de.frank.jarvis.speech.GespraechsFokus.freigeben()
        }
    }

    // ---- Hey Jarvis ----

    /** Fehlende Erlaubnis für „Hey Jarvis“ holen: erst das Mikrofon, dann „Über anderen Apps einblenden“. */
    fun weckwortEinrichten(activity: ComponentActivity) {
        when {
            !weckwortMikro -> weckwortMikroAnfragen()
            !ueberlagernErlaubt -> runCatching {
                activity.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${activity.packageName}")))
            }
        }
    }

    fun weckwortSchalten(an: Boolean, activity: ComponentActivity) {
        einstellungen.weckwortAn = an
        if (an && !weckwortMikro) weckwortMikroAnfragen()
        de.frank.jarvis.weckwort.WeckwortDienst.abgleichen(activity)
    }

    fun weckwortEmpfindlichkeit(stufe: String) {
        einstellungen.weckwortEmpfindlichkeit = stufe
        de.frank.jarvis.weckwort.WeckwortDienst.empfindlichkeitUebernehmen(getApplication())
    }

    /** „jarvis“ = „Hey Jarvis“, „phrase“ = eigene Phrase über sherpa-onnx. */
    fun weckwortArtSetzen(art: String) {
        if (einstellungen.weckwortArt == art) return
        einstellungen.weckwortArt = art
        de.frank.jarvis.weckwort.WeckwortDienst.neuLaden(getApplication())
    }

    /** Neue eigene Phrase: nur übernehmen, wenn das (englische) Modell sie zerlegen kann. */
    fun weckwortPhraseSetzen(text: String): Boolean {
        val app = getApplication<Application>()
        val normiert = de.frank.jarvis.weckwort.PhrasenTokens.normiert(text)
        if (normiert.split(' ').size < 2) { meldung = "Bitte mindestens zwei Wörter, besser drei."; return false }
        if (de.frank.jarvis.weckwort.PhrasenTokens.zerlege(app, normiert) == null) {
            meldung = "Das Modell kennt nur englische Buchstaben A bis Z, ohne Umlaute und Ziffern."
            return false
        }
        einstellungen.weckwortPhrase = normiert
        einstellungen.weckwortArt = "phrase"
        de.frank.jarvis.weckwort.WeckwortDienst.neuLaden(app)
        meldung = "Weckwort: „$normiert“"
        return true
    }

    /** Die laufende Aufnahme kam über „Hey Jarvis“ und endet von selbst, sobald Frank fertig gesprochen hat. */
    var freihaendig by mutableStateOf(false); private set
    private var weckwortLauf: Job? = null
    /** „Sprachsession beenden“: Die laufende Sprachschleife öffnet das Mikrofon nicht mehr von selbst. Erst eine ausdrückliche Aktivierung hebt das auf. */
    @Volatile private var sprachsitzungBeendet = false

    /** Das Werkzeug `sprachsession_beenden` (vom Modell, auf einem Hintergrundfaden): Ende vormerken. true = eine Sprachschleife lief. */
    private val sprachsessionBeenden: () -> Boolean = {
        sprachsitzungBeendet = true
        val lief = weckwortLauf?.isActive == true
        // Im Live-Gespräch beendet dasselbe Werkzeug die Live-Verbindung: Sonst bestätigte das Modell nur und der Hörer bliebe offen.
        val liveLief = liveAktiv
        if (liveLief) viewModelScope.launch(Dispatchers.Main) { liveSprachEnde("per Werkzeug") }
        else Logbuch.sprache("Sprachsitzung beendet", if (lief) "per Werkzeug" else "per Werkzeug, keine Sprachschleife aktiv")
        lief || liveLief
    }

    /** Was gerade passiert, für die Anzeige auch im gesperrten Zustand ([liestVor]: der Vorleser spricht). */
    fun sprachStatus(liestVor: Boolean): String? = when {
        nimmtAuf -> if (freihaendig) "Ich höre zu …" else "Ich höre zu … tippe zum Senden"
        schreibtMit -> "Schreibe mit …"
        denkt -> "Jarvis denkt nach …"
        liestVor -> "Jarvis antwortet …"
        else -> null
    }

    /**
     * „Hey Jarvis“ wurde erkannt: aufnehmen, piepen (ab jetzt sprechen), nach der Frage von selbst abschicken und
     * die Antwort vorlesen. [fertig] läuft, wenn alles vorbei ist (die Antwort ist zu Ende gesprochen).
     */
    fun weckwortStart(fortsetzen: Boolean, fertig: () -> Unit) {
        de.frank.jarvis.weckwort.Weckwort.uebernommen()
        val grund = when {
            nimmtAuf || schreibtMit || holtMikro -> ""
            sprache.groqApiKey.isBlank() -> "Für „Hey Jarvis“ bitte zuerst den Groq-Schlüssel eintragen."
            !kiVerbunden -> "Für „Hey Jarvis“ bitte zuerst mit ChatGPT verbinden."
            else -> null
        }
        if (grund != null) {
            if (grund.isNotEmpty()) meldung = grund
            if (!nimmtAuf && !holtMikro) de.frank.jarvis.weckwort.Weckwort.mikrofonFreigeben()
            fertig()
            return
        }
        // War Jarvis beim Weckwort schon sichtbar vorn, geht das offene Gespräch weiter. Eine dort noch laufende Antwort
        // weicht der neuen Frage (sonst verwirft [sende] sie); ein Programmierauftrag läuft davon unabhängig weiter.
        // Abgebrochen wird erst in [sende], wenn feststeht, dass der Satz kein Wechsel der Session ist: Sonst endete die
        // Arbeit im alten Gespräch, obwohl Frank nur nebenher eine neue Session öffnen will.
        weichendeRunde = if (fortsetzen) sitzungId?.let { laufende[it] } else null
        reiter = Reiter.JARVIS
        merkOffen = null
        // Auch die Gedächtnis-Ansicht schließen: Sie verdeckt sonst Gespräch und Reiter-Leiste.
        GedaechtnisAnsicht.offen = false
        diktatZiel = null
        vorleser.stopp()
        // Kam „Hey Jarvis“ aus einer anderen App, aus dem Hintergrund oder per Kaltstart, beginnt ein neues Gespräch (beim
        // Kaltstart ist womöglich schon das Gespräch eines laufenden Programmierauftrags offen). Das bisherige bleibt
        // gespeichert; arbeitet Jarvis dort noch, landet die Antwort in dessen Datei, der Programmierauftrag läuft weiter.
        if (!fortsetzen && (gespraech.isNotEmpty() || sitzungId != null)) gespraechLeeren()
        // Ausdrücklich aktiviert: Eine früher beendete Sprachsession hindert die neue nicht mehr.
        sprachsitzungBeendet = false
        val treffer = de.frank.jarvis.weckwort.Weckwort.trefferMerkmale
        de.frank.jarvis.weckwort.Weckwort.trefferMerkmale = null
        weckwortLauf?.cancel()
        val nr = ++sprachmodusNr
        sprachmodusAktiv = true
        weckwortLauf = viewModelScope.launch {
            // Für das ganze Gespräch den Audiofokus halten: Andere Apps bleiben leise, bis Jarvis fertig ist.
            de.frank.jarvis.speech.GespraechsFokus.halten(getApplication())
            try {
                // Gesprächsmodus: Nach jeder vorgelesenen Antwort piept es wieder und Jarvis hört weiter zu, bis
                // nach dem Piepton nichts mehr kommt. Das Mikrofon bleibt dabei für Jarvis reserviert, sonst
                // schnappt es sich zwischen zwei Runden der Weckwort-Dienst.
                var runde = 0
                while (true) {
                    // Vorgemerkte Runde nach „Sprachsession beenden“: nicht mehr piepen und nicht mehr zuhören.
                    if (runde > 0 && sprachsitzungBeendet) break
                    val antworten = gespraech.count { !it.vonMir }
                    val navi = navigationen
                    val ergebnis = runde(erste = runde == 0)
                    if (ergebnis == de.frank.module.transkriptionsmodul.SprechEnde.Lage.NICHTS_GEHOERT) {
                        // Gleich nach „Hey Jarvis“ nichts gesagt: Das war wohl ein Fehlalarm, das Stimmprofil lernt ihn.
                        // Nicht, wenn nur das Mikrofon nicht starten wollte: Das sagt nichts über den Klang.
                        if (runde == 0 && !mikroFehlte) {
                            de.frank.jarvis.weckwort.Stimmprofil.fehlalarm(treffer)
                            de.frank.jarvis.weckwort.WeckwortProtokoll.ausgang(getApplication(), "nichts")
                        }
                        break
                    }
                    if (runde == 0) de.frank.jarvis.weckwort.WeckwortProtokoll.ausgang(getApplication(), "frage")
                    // Mitschrift und Antwort abwarten, dann bis die Antwort zu Ende vorgelesen ist. Ohne feste Pausen:
                    // Der Vorleser steht schon auf „lädt“, wenn „denkt“ endet (beides im selben Schritt).
                    snapshotFlow { schreibtMit || denkt || navigiert }.first { !it }
                    vorleser.stand.first { it.zustand == de.frank.jarvis.speech.VorleseZustand.AUS }
                    // Ausdrücklich beendet: Schluss, auch wenn inzwischen eine Antwort aus dem Hintergrund (Programmierauftrag) ankam.
                    if (sprachsitzungBeendet) break
                    // Keine neue Antwort (abgebrochen, nichts verstanden) oder Frank hat selbst übernommen: Schluss.
                    // Nach einem Wechsel der Session hört Jarvis weiter zu, dann im neuen Gespräch.
                    if ((gespraech.count { !it.vonMir } <= antworten && navigationen == navi) || nimmtAuf || holtMikro) break
                    kotlinx.coroutines.delay(300)
                    runde++
                }
            } finally {
                de.frank.jarvis.speech.GespraechsFokus.freigeben()
                freihaendig = false
                if (!nimmtAuf && !holtMikro) de.frank.jarvis.weckwort.Weckwort.mikrofonFreigeben()
                // Ein inzwischen neu gestarteter Sprachmodus bleibt aktiv: Nur der eigene Lauf setzt das Zeichen zurück.
                if (sprachmodusNr == nr) sprachmodusAktiv = false
                fertig()
                SprachBlase.abgleichen(getApplication())
            }
        }
    }

    /** Läuft der Sprachmodus von „Hey Jarvis“ (Gesprächsmodus)? Danach richtet sich die Sprachblase. */
    var sprachmodusAktiv by mutableStateOf(false); private set
    private var sprachmodusNr = 0

    /** Für die Sprachblase: Wer spricht gerade? */
    val sprichtFrank: Boolean get() = nimmtAuf
    val sprichtJarvis: Boolean get() = vorleser.stand.value.zustand == de.frank.jarvis.speech.VorleseZustand.SPRICHT

    /**
     * Langes Drücken auf die Sprachblase: Sprachmodus sofort beenden. Aufnahme, offene Antwort und Vorlesen enden, das
     * Mikrofon öffnet nicht mehr von selbst. Das Gespräch bleibt gespeichert.
     */
    fun sprachmodusBeenden() {
        sprachsitzungBeendet = true
        Logbuch.sprache("Sprachsitzung beendet", "Sprachblase lang gedrückt")
        aufnahmeVerwerfen()
        abbrechen()
        vorleser.stopp()
        SprachBlase.abgleichen(getApplication())
    }

    /**
     * Eine Runde im Gesprächsmodus: Mikrofon holen, piepen (ab jetzt sprechen), aufnehmen, bis Frank 4,5 Sekunden
     * schweigt, und abschicken. Liefert [SprechEnde.Lage.NICHTS_GEHOERT], wenn nach dem Piepton nichts kam.
     */
    private var mikroFehlte = false

    private suspend fun runde(erste: Boolean): de.frank.module.transkriptionsmodul.SprechEnde.Lage {
        holtMikro = true
        val gestartet = try { aufnahmeStarten() } finally { holtMikro = false }
        mikroFehlte = !gestartet
        if (!gestartet) { meldung = "Die Aufnahme ließ sich nicht starten."; return de.frank.module.transkriptionsmodul.SprechEnde.Lage.NICHTS_GEHOERT }
        freihaendig = true
        // Nach einer Antwort darf Frank etwas länger überlegen, bevor er anfängt.
        val ende = de.frank.module.transkriptionsmodul.SprechEnde(wartenMs = if (erste) 7_000 else 9_000)
        // Die Uhr läuft ab Beginn des Pieptons; SprechEnde überspringt genau dessen Dauer.
        val start = System.currentTimeMillis()
        piep()
        var lage = de.frank.module.transkriptionsmodul.SprechEnde.Lage.WEITER
        while (nimmtAuf && lage == de.frank.module.transkriptionsmodul.SprechEnde.Lage.WEITER) {
            kotlinx.coroutines.delay(50)
            lage = ende.neu(pegel.value, System.currentTimeMillis() - start, mikro.sprachRahmen)
        }
        freihaendig = false
        when {
            !nimmtAuf -> return de.frank.module.transkriptionsmodul.SprechEnde.Lage.FERTIG // Frank hat selbst auf Stopp getippt: Das Abschicken läuft schon.
            lage == de.frank.module.transkriptionsmodul.SprechEnde.Lage.NICHTS_GEHOERT -> {
                nimmtAuf = false
                mikro.stop()
                return lage
            }
            else -> aufnahmeBeenden(freigeben = false)
        }
        return de.frank.module.transkriptionsmodul.SprechEnde.Lage.FERTIG
    }

    /** Kurzer Piepton: Ab jetzt hört Jarvis zu. Über die Medienlautstärke, wie die vorgelesene Antwort. */
    private suspend fun piep() {
        val ton = runCatching { android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100) }.getOrNull() ?: return
        try {
            ton.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 160)
            kotlinx.coroutines.delay(220)
        } finally {
            ton.release()
        }
    }

    /** [freigeben] = false im Gesprächsmodus: Das Mikrofon bleibt für die nächste Runde reserviert. */
    private fun aufnahmeBeenden(freigeben: Boolean = true) {
        nimmtAuf = false
        // Die Mitschrift gehört zur Session, in der die Aufnahme begann.
        val stand = aufnahmeStand
        schreibtMit = true
        // Ein Diktat geht nur in eine Merkliste, solange sie offen ist; sonst ist der Text eine Frage an Jarvis.
        // Das Diktier-Mikrofon der Eingabeblase schreibt ins Feld, Hey Jarvis und der Gesprächsmodus senden weiter direkt.
        val ziel = diktatZiel?.takeIf { merkOffen != null || diktatInsFeld }
        diktatZiel = null
        diktatInsFeld = false
        mitschriftJob = viewModelScope.launch {
            try {
                val wav = mikro.stop()
                val stimmhaft = mikro.letzteStimmhaft
                de.frank.jarvis.audio.AufnahmeDienst.stoppen()
                if (freigeben && weckwortLauf?.isActive != true) {
                    de.frank.jarvis.weckwort.Weckwort.mikrofonFreigeben()
                    de.frank.jarvis.speech.GespraechsFokus.freigeben()
                }
                if (wav == null || wav.size < 2000) { meldung = "Die Aufnahme war zu kurz."; return@launch }
                val beginn = System.currentTimeMillis()
                val groq = transkribierer()
                val text = withContext(Dispatchers.IO) { de.frank.module.transkriptionsmodul.TranskriptionAnbindung.diktat(groq).transkribiere(wav, stimmhaft).text.trim() }
                Logbuch.sprache("Mitschrift", "${wav.size / 1024} KB Aufnahme, ${text.length} Zeichen", System.currentTimeMillis() - beginn)
                // Inzwischen von Hand die Session gewechselt: Das verspätete Ergebnis verfällt, nie in der neuen Session.
                if (stand != wechselStand) { Logbuch.sprache("Mitschrift verworfen", "Session von Hand gewechselt"); return@launch }
                if (text.isBlank()) meldung = "Ich habe nichts verstanden."
                else if (ziel != null) ziel(text)
                else if (SprachBefehle.istSessionEnde(text, imGespraechsmodus = weckwortLauf?.isActive == true)) {
                    // Ausbefehl im Gesprächsmodus oder ausdrücklich „Sprachsession beenden“: kurz bestätigen, danach kein Piepton
                    // und kein offenes Mikrofon mehr. Geht nie ans Modell: kein neues Gespräch, kein Auftrag. Das Gespräch und
                    // laufende Antworten, Agenten und Programmieraufträge bleiben unberührt.
                    sprachsitzungBeendet = true
                    Logbuch.sprache("Sprachsitzung beendet", text)
                    // Eigene Quelle je Meldung: Dieselbe Quelle ein zweites Mal hielte das Vorlesen an, statt zu sprechen.
                    vorleser.sprich("sitzung-ende:" + System.currentTimeMillis(), "Jarvis", "Okay, Sprachsession beendet.")
                }
                else { schreibtMit = false; gesprochen = true; sende(text) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logbuch.fehler("Mitschrift", e)
                meldung = e.message ?: "Die Aufnahme konnte nicht in Text umgewandelt werden."
            } finally {
                schreibtMit = false
            }
        }
    }

    /** Ein Groq-Client für alle Aufnahmen: Die Verbindung bleibt offen, statt bei jeder Frage neu aufgebaut zu werden. */
    private var groq: de.frank.module.transkriptionsmodul.GroqTranscriber? = null
    private var groqStand = 0

    /** Der Transkribierer zu den aktuellen Einstellungen; neu gebaut nur, wenn sich Schlüssel oder Filter geändert haben. */
    private fun transkribierer(): de.frank.module.transkriptionsmodul.GroqTranscriber {
        val stand = listOf(sprache.groqApiKey, sprache.filterStilleVorabAn, sprache.filterSegmentmetrikenAn, sprache.filterZeitstempelAn, sprache.filterFloskelnAn).hashCode()
        groq?.takeIf { groqStand == stand }?.let { return it }
        groq?.shutdown()
        return de.frank.module.transkriptionsmodul.GroqTranscriber(
            apiKey = sprache.groqApiKey,
            filterStille = sprache.filterStilleVorabAn, filterMetriken = sprache.filterSegmentmetrikenAn,
            filterZeitstempel = sprache.filterZeitstempelAn, filterFloskeln = sprache.filterFloskelnAn,
        ).also { groq = it; groqStand = stand }
    }

    /** Lautsprecher-Knopf: vorlesen, oder anhalten, wenn genau das schon läuft. */
    fun lies(quelle: String, titel: String, text: String) {
        val stand = vorleser.stand.value
        if (stand.quelle == quelle && stand.zustand != de.frank.jarvis.speech.VorleseZustand.AUS) vorleser.stopp() else vorleser.sprich(quelle, titel, text)
    }

    // ---- Bilder und PDFs im Chat: bewusst in die Ablage übernehmen ----

    /** Laufende oder gescheiterte Übernahmen je Chat-Verweis: [UMZUG_LAEUFT] oder der Fehlertext. Erfolg entfernt den Eintrag. */
    val ablageUmzug = androidx.compose.runtime.mutableStateMapOf<String, String>()

    /**
     * „In Ablage verschieben“ an einem Bild oder PDF im Chat: Datei und Zuordnung (Titel, Beschreibung, Kennung) kommen in die
     * Ablage. Erst wenn sie dort liegt, zeigt der Chat den Erfolg und die Nachricht verweist auf die Datei in der Ablage; schlägt
     * es fehl, bleibt die vorläufige Datei und der Fehler steht am Bild.
     */
    fun chatDateiInAblage(pfad: String) {
        if (ablageUmzug[pfad] == UMZUG_LAEUFT) return
        ablageUmzug[pfad] = UMZUG_LAEUFT
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { de.frank.jarvis.ablage.Vorlaeufig.dateiInAblage(getApplication(), pfad) } }
            r.onSuccess { u ->
                ablageUmzug.remove(pfad)
                sicherung.withLock {
                    var geaendert = false
                    gespraech.indices.forEach { i ->
                        val n = gespraech[i]
                        if (pfad !in n.anhaenge) return@forEach
                        val kontext = if (n.art == de.frank.jarvis.faehigkeit.ProgrammierChat.ART_BILD) "(Dieses Bild liegt in der Ablage im Eintrag „${u.titel}“.)" else n.kontext
                        gespraech[i] = n.copy(anhaenge = n.anhaenge.map { if (it == pfad) u.pfad else it }, kontext = kontext)
                        geaendert = true
                    }
                    if (geaendert) sitzungSichern()
                }
                meldung = if (u.schonDa) "Lag schon in der Ablage: „${u.titel}“." else "In der Ablage gespeichert: „${u.titel}“."
            }.onFailure { e ->
                if (e is CancellationException) throw e
                ablageUmzug[pfad] = de.frank.jarvis.ablage.AblageZentrale.fehlerText(e)
            }
        }
    }

    // ---- Programmieraufträge im Gespräch ----

    /**
     * Das Gespräch, das die App beim Start wegen eines laufenden Programmierauftrags öffnet. Gesetzt, bevor die Datei gelesen
     * wird: Meldungen, die der Server genau in diesem Moment nachliefert (nach einem Update von Jarvis), gehen dann in das
     * geladene Gespräch statt in die Datei, die gerade gelesen wird.
     */
    @Volatile private var programmierGespraech: String? = null

    /**
     * Das Gespräch, das [sitzungLaden] gerade liest. Meldungen dafür übernimmt die App (statt sie selbst an die Datei zu hängen):
     * Sie warten auf die Sicherung, die das Laden hält, und kommen danach in die Ansicht. Sonst sähe die Ansicht den Stand
     * vor der Meldung, und das nächste Speichern überschriebe sie.
     */
    @Volatile private var ladeZiel: String? = null

    private val programmierEmpfaenger: (String, de.frank.jarvis.data.GespeicherteNachricht) -> Boolean = { id, n ->
        if (id != sitzungId && id != programmierGespraech && id != ladeZiel) false else {
            viewModelScope.launch(Dispatchers.Main) {
                sicherung.withLock {
                    if (id == sitzungId) {
                        // Dieselbe Rückfrage nur einmal (Wiederholung des Hooks, Abgleich mit dem Server).
                        val frageId = if (n.art == "rueckfrage") de.frank.jarvis.data.Gespraeche.rueckfrageId(n.kontext) else ""
                        if (frageId.isNotEmpty() && gespraech.any { it.art == "rueckfrage" && de.frank.jarvis.data.Gespraeche.rueckfrageId(it.kontext) == frageId }) return@withLock
                        gespraech += Nachricht(n.vonMir, n.text, n.art, n.kontext, n.anhaenge)
                        sitzungSichern()
                    } else withContext(Dispatchers.IO) { de.frank.jarvis.data.Gespraeche.anhaengen(getApplication(), id, n) }
                }
            }
            true
        }
    }

    private val programmierAendern: (String, String, (org.json.JSONObject) -> Boolean) -> Boolean? = { id, frageId, aenderung ->
        val istOffen = id == sitzungId
        if (!istOffen && id != programmierGespraech && id != ladeZiel) null else {
            val gefunden = if (istOffen) gespraech.toList().any { it.art == "rueckfrage" && de.frank.jarvis.data.Gespraeche.rueckfrageId(it.kontext) == frageId }
            else de.frank.jarvis.data.Gespraeche.rueckfrageKontext(getApplication(), id, frageId) != null
            if (gefunden) viewModelScope.launch(Dispatchers.Main) {
                sicherung.withLock {
                    val i = gespraech.indexOfLast { it.art == "rueckfrage" && de.frank.jarvis.data.Gespraeche.rueckfrageId(it.kontext) == frageId }
                    if (id == sitzungId && i >= 0) {
                        val k = runCatching { org.json.JSONObject(gespraech[i].kontext) }.getOrNull() ?: return@withLock
                        if (!aenderung(k)) return@withLock
                        gespraech[i] = gespraech[i].copy(kontext = k.toString())
                        sitzungSichern()
                    } else withContext(Dispatchers.IO) { de.frank.jarvis.data.Gespraeche.rueckfrageAendern(getApplication(), id, frageId, aenderung) }
                }
            }
            gefunden
        }
    }

    /** Die Rückfrage (frage_id), die gerade im Vollbild offen ist, oder null. */
    var rueckfrageDialog by mutableStateOf<String?>(null)
        private set

    /** Rückfragen, die Frank in diesem Lauf geschlossen hat: Sie öffnen sich nicht von selbst wieder (der Chat zeigt „Beantworten“). */
    private val rueckfrageGeschlossen = mutableSetOf<String>()

    fun rueckfrageOeffnen(frageId: String) { rueckfrageDialog = frageId }

    fun rueckfrageSchliessen() {
        rueckfrageDialog?.let { rueckfrageGeschlossen += it }
        rueckfrageDialog = null
    }

    /**
     * Öffnet die älteste noch offene Rückfrage des Gesprächs im Vollbild, wenn gerade keine offen ist und Frank sie nicht
     * selbst geschlossen hat: beim Eintreffen und beim Öffnen des Gesprächs.
     */
    fun rueckfrageVonSelbst() {
        if (rueckfrageDialog != null) return
        val jetzt = System.currentTimeMillis() / 1000
        gespraech.firstOrNull { n ->
            if (n.art != "rueckfrage") return@firstOrNull false
            val k = runCatching { org.json.JSONObject(n.kontext) }.getOrNull() ?: return@firstOrNull false
            val stand = de.frank.jarvis.faehigkeit.RueckfrageStand.von(k)
            stand == de.frank.jarvis.faehigkeit.RueckfrageStand.OFFEN && k.optString("frage_id") !in rueckfrageGeschlossen &&
                (k.optLong("bis") == 0L || k.optLong("bis") > jetzt)
        }?.let { rueckfrageDialog = de.frank.jarvis.data.Gespraeche.rueckfrageId(it.kontext) }
    }

    /** Merkt sich die angefangene Auswahl im Gespräch: Nach dem Schließen oder einem Neustart der App ist sie wieder da. */
    fun rueckfrageEntwurf(frageId: String, entwurf: org.json.JSONObject) {
        val gespraechId = sitzungId ?: return
        de.frank.jarvis.faehigkeit.ProgrammierChat.rueckfrageAendern(getApplication(), gespraechId, frageId) { k ->
            if (k.optJSONObject("entwurf")?.toString() == entwurf.toString()) false else { k.put("entwurf", entwurf); true }
        }
    }

    init {
        de.frank.jarvis.faehigkeit.ProgrammierChat.empfaenger = programmierEmpfaenger
        de.frank.jarvis.faehigkeit.ProgrammierChat.aendern = programmierAendern
        de.frank.jarvis.faehigkeit.Sprachsession.beenden = sprachsessionBeenden
    }

    /**
     * Knopf in einer Rückfrage: Franks Antwort an die wartende Programmier-Sitzung, oder mit [freigeben] die Frage an den
     * Rechner und die Claude-App weitergeben. Der Stand steht danach an der Rückfrage (sendet, beantwortet, Fehler mit Grund).
     */
    fun programmierAntwort(frageId: String, antworten: List<String>, text: String, freigeben: Boolean = false) {
        viewModelScope.launch {
            val faehigkeit = de.frank.jarvis.faehigkeit.Register.alle(getApplication()).filterIsInstance<de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit>().firstOrNull()
            val ergebnis = runCatching { faehigkeit?.beantworte(frageId, antworten, text, freigeben) ?: throw IllegalStateException("Programmieren ist nicht verfügbar.") }
            meldung = ergebnis.getOrElse { it.message ?: "Die Antwort ging nicht raus." }
            // Angekommen: weiter zur nächsten offenen Rückfrage dieses Gesprächs. Bei einem Fehler bleibt das Fenster offen
            // und zeigt den Grund samt „Erneut senden“.
            if (ergebnis.isSuccess) {
                if (rueckfrageDialog == frageId) { rueckfrageDialog = null; rueckfrageGeschlossen += frageId }
                rueckfrageVonSelbst()
            }
        }
    }

    /** Gleicht offene Rückfragen mit dem Jarvis-Server ab (verlorene Meldungen, Ausgang am Rechner). */
    fun rueckfragenAbgleichen() {
        viewModelScope.launch {
            de.frank.jarvis.faehigkeit.Register.alle(getApplication()).filterIsInstance<de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit>().firstOrNull()
                ?.let { runCatching { it.fragenAbgleichen() } }
        }
    }

    override fun onCleared() {
        if (de.frank.jarvis.faehigkeit.ProgrammierChat.empfaenger === programmierEmpfaenger) de.frank.jarvis.faehigkeit.ProgrammierChat.empfaenger = null
        if (de.frank.jarvis.faehigkeit.ProgrammierChat.aendern === programmierAendern) de.frank.jarvis.faehigkeit.ProgrammierChat.aendern = null
        if (de.frank.jarvis.faehigkeit.Sprachsession.beenden === sprachsessionBeenden) de.frank.jarvis.faehigkeit.Sprachsession.beenden = null
        live?.beenden()
        // Das Aufräumen des Live-Gesprächs liefe über den schon beendeten viewModelScope: Mikrofon hier direkt freigeben.
        if (nimmtAuf || holtMikro || liveAktiv) de.frank.jarvis.weckwort.Weckwort.mikrofonFreigeben()
        // Ebenso den Audiofokus von Diktat und Live-Gespräch: Sonst blieben andere Apps pausiert.
        if (nimmtAuf || holtMikro || liveAktiv || weckwortLauf?.isActive == true) de.frank.jarvis.speech.GespraechsFokus.freigeben()
        mikro.release()
        de.frank.jarvis.audio.AufnahmeDienst.stoppen()
        groq?.shutdown()
        super.onCleared()
    }

    /** Bricht die Antwort im offenen Gespräch ab. Antworten in anderen Gesprächen laufen weiter. */
    fun abbrechen() {
        val id = sitzungId ?: return
        val runde = laufende.remove(id) ?: return
        // Die Frage bleibt als unbeantwortet im Verlauf, samt dem, was in dieser Runde schon erledigt war.
        frageAbgebrochen(runde)
        // Der Wächter in der Anfrage schließt die Verbindung, sobald die Koroutine abgebrochen wird: nur diese eine Antwort.
        runde.job.cancel()
    }

    /** Bricht die Antwort eines beliebigen Gesprächs ab (beim Löschen), ohne den Verlauf anzufassen. */
    private fun rundeAbbrechen(id: String) {
        laufende.remove(id)?.job?.cancel()
    }

    /** Markiert die Frage der Runde als abgebrochen, sofort und auf dem Hauptfaden (danach kann schon ein neues Gespräch beginnen). */
    private fun frageAbgebrochen(runde: Runde) {
        val i = runde.frage
        val n = gespraech.getOrNull(i) ?: return
        if (!n.vonMir || i != gespraech.lastIndex) return
        gespraech[i] = n.copy(art = "abgebrochen", kontext = erledigtNotiz("Bereits erledigt, bevor die Runde abbrach", runde.erledigt))
        sitzungSichern()
    }

    // ---- Gespeicherte Gespräche (Seitenleiste) ----

    /** Das offene Gespräch; null = noch nicht gespeichert (es entsteht mit der ersten Nachricht). */
    var sitzungId by mutableStateOf<String?>(null); private set
    private var sitzungErstellt = 0L
    /** Wann zuletzt etwas im Gespräch passiert ist, für „Hey Jarvis“: weiterführen oder neu anfangen. */
    private var letzteAktivitaet = 0L
    var sitzungen by mutableStateOf<List<de.frank.jarvis.data.Sitzung>>(emptyList()); private set

    /** Speichern, Umbenennen, Löschen und Neueinlesen der Gespräche der Reihe nach: Kein älterer Stand überholt einen neueren. */
    private val sicherung = Mutex()

    init {
        // Normaler Start: Startbildschirm mit neuem Gespräch. Nur nach einem Update von Jarvis selbst geht es dort weiter, wo Frank war.
        nachUpdateWiederherstellen()
        bildschirmBeobachten()
        runCatching { de.frank.jarvis.faehigkeit.Register.alle(getApplication()).filterIsInstance<de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit>().firstOrNull()?.aktivitaetLaden() }
        sitzungenLaden()
        // Das Stimmprofil braucht erst „Hey Jarvis“ (und der Weckwort-Dienst lädt es selbst): nicht den ersten Frame aufhalten.
        viewModelScope.launch(Dispatchers.IO) { de.frank.jarvis.weckwort.Stimmprofil.laden(app) }
    }

    fun sitzungenLaden() {
        viewModelScope.launch {
            sitzungen = sicherung.withLock { withContext(Dispatchers.IO) { de.frank.jarvis.data.Gespraeche.alle(getApplication()) } }
            // Ältere Gespräche ohne KI-Überschrift nachholen: die zehn neuesten, eins nach dem anderen.
            if (!kiVerbunden) return@launch
            sitzungen.take(10).forEach { s ->
                val stand = withContext(Dispatchers.IO) {
                    if (!de.frank.jarvis.data.Gespraeche.brauchtKiTitel(getApplication(), s.id, s.anzahl)) null
                    else de.frank.jarvis.data.Gespraeche.laden(getApplication(), s.id)?.second
                } ?: return@forEach
                kiTitelBilden(s.id, stand)
            }
        }
    }

    /** [kuerzenUm]: so viele Nachrichten hat Frank eben bewusst entfernt; sonst überschreibt ein kürzerer Stand die Datei nie. */
    private fun sitzungSichern(kuerzenUm: Int = 0) {
        letzteAktivitaet = System.currentTimeMillis()
        val id = sitzungId ?: de.frank.jarvis.data.Gespraeche.neueId().also { sitzungId = it; sitzungErstellt = System.currentTimeMillis() }
        val erstellt = sitzungErstellt
        val stand = gespraech.map { de.frank.jarvis.data.GespeicherteNachricht(it.vonMir, it.text, it.art, it.kontext, it.anhaenge, it.bewertung) }
        offenesGespraechMerken(id, letzteAktivitaet)
        viewModelScope.launch {
            // Nur dieses eine Gespräch schreiben und in der Liste ersetzen, statt alle Dateien neu zu lesen.
            val neu = sicherung.withLock { withContext(Dispatchers.IO) { de.frank.jarvis.data.Gespraeche.speichern(getApplication(), id, erstellt, stand, kuerzenUm) } }
            if (neu != null) sitzungen = listOf(neu) + sitzungen.filter { it.id != neu.id }
            if (neu != null) kiTitelBilden(id, stand)
            // Das Gespräch gehört sofort ins Kurzzeitgedächtnis (für das nächste Gespräch und für ChatGPT).
            de.frank.jarvis.gedaechtnis.Lagebild.anstossen(getApplication(), listOf(de.frank.jarvis.gedaechtnis.Bereich.GESPRAECHE), "Gespräch mit Jarvis")
        }
    }

    /** Gespräche, für die gerade eine Überschrift entsteht: höchstens ein Auftrag je Gespräch gleichzeitig. */
    private val titelInArbeit = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    /**
     * Überschrift für die Seitenleiste vom Modell: höchstens sieben Wörter, aus dem, worum es im Gespräch geht
     * (meist Franks erste Frage). Läuft still im Hintergrund; ohne Verbindung bleibt die erste Frage als Titel.
     */
    private fun kiTitelBilden(id: String, stand: List<de.frank.jarvis.data.GespeicherteNachricht>) {
        if (!kiVerbunden || stand.size < 2 || !titelInArbeit.add(id)) return
        viewModelScope.launch {
            try {
                val noetig = withContext(Dispatchers.IO) { de.frank.jarvis.data.Gespraeche.brauchtKiTitel(getApplication(), id, stand.size) }
                if (!noetig) return@launch
                val auszug = buildString {
                    stand.take(8).forEach { n -> append(if (n.vonMir) "Frank: " else "Jarvis: ").append(n.text.replace(Regex("\\s+"), " ").take(600)).append('\n') }
                }.take(4_000)
                val roh = kotlinx.coroutines.withTimeoutOrNull(45_000) {
                    runCatching { auth.streamChat(TITEL_AUFTRAG, listOf(ChatTurn("user", auszug)), einstellungen.modell, ReasoningEffort.LOW) }.getOrNull()
                } ?: return@launch
                val titel = roh.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
                    .trim('"', '„', '“', '\'', '*', '#', ' ').removeSuffix(".").split(Regex("\\s+")).take(7).joinToString(" ")
                if (titel.length < 3) return@launch
                val neu = withContext(Dispatchers.IO) { sicherung.withLock { de.frank.jarvis.data.Gespraeche.setzeKiTitel(getApplication(), id, titel, stand.size) } }
                if (neu != null) sitzungen = sitzungen.map { if (it.id == id) it.copy(titel = neu.titel) else it }
            } finally { titelInArbeit.remove(id) }
        }
    }

    /** Neues, leeres Gespräch. Das bisherige bleibt gespeichert und steht in der Seitenleiste; arbeitet Jarvis dort noch, macht er weiter. */
    fun neuesGespraech() {
        // Von Hand (Knopf, Seitenleiste, Löschen): Aufnahme und Gesprächsmodus der alten Session enden.
        aufnahmeVerwerfen()
        gespraechLeeren()
    }

    /** Leert die Ansicht ohne die Aufnahme anzufassen: für „Hey Jarvis“ und den Wechsel per Sprache, die im neuen Gespräch weiterhören. */
    private fun gespraechLeeren() {
        sitzungId?.let { vorherigeSitzung = it }
        vorleser.stopp()
        entwurfErledigt = null
        gespraech.clear()
        sitzungId = null
        sitzungErstellt = 0L
        offenesGespraechVergessen()
    }

    // Zuletzt offenes Gespräch; „Hey Jarvis“ führt nur das Gespräch einer schon sichtbaren App fort, nie ein gespeichertes.
    private fun offenePrefs() = getApplication<Application>().getSharedPreferences("jarvis_offenes_gespraech", Context.MODE_PRIVATE)

    private fun offenesGespraechMerken(id: String, zeit: Long) {
        offenePrefs().edit().putString("id", id).putLong("zeit", zeit).apply()
    }

    private fun offenesGespraechVergessen() {
        offenePrefs().edit().clear().apply()
    }

    // ---- Start, Rückkehr und Neustart nach einem Update ----

    /** Welcher Bildschirm offen ist und ob die App gerade sichtbar ist: Daraus stellt der Neustart nach einem Update alles wieder her. */
    private fun bildPrefs() = getApplication<Application>().getSharedPreferences("jarvis_bildschirm", Context.MODE_PRIVATE)

    /** Hält jeden Wechsel von Reiter, Merkliste, Gedächtnis-Ansicht und Einstellungsbereich sofort fest (ein Update beendet die App ohne Vorwarnung). */
    private fun bildschirmBeobachten() {
        viewModelScope.launch {
            snapshotFlow { listOf(reiter.name, merkOffen?.titel.orEmpty(), GedaechtnisAnsicht.offen.toString(), einstellungsReiter, einstellungsVerlauf.joinToString(",")) }
                .collect { (r, merk, ged, einst, verlauf) ->
                    bildPrefs().edit().putString("reiter", r).putString("merk", merk).putBoolean("gedaechtnis", ged == "true")
                        .putString("einstellungen", einst).putString("einstellungsVerlauf", verlauf).apply()
                }
        }
    }

    /** Sichtbar (onStart) oder verlassen (onStop). Beendet ein Update die App, bleibt „sichtbar“ stehen, weil onStop nie kommt. */
    fun sichtbarMerken(sichtbar: Boolean) {
        val e = bildPrefs().edit().putBoolean("sichtbar", sichtbar)
        if (!sichtbar) e.putLong("verlassen", System.currentTimeMillis())
        e.apply()
    }

    /**
     * Neustart durch ein Update von Jarvis: dieselbe Session und derselbe Bildschirm wie davor. Erkannt am Installationszeitpunkt
     * der App, der sich seit dem letzten Start geändert hat, und daran, dass Frank Jarvis bis kurz vor dem Update benutzt hat
     * (sichtbar oder höchstens [UPDATE_VORLAUF] vorher verlassen, etwa für UpdateStation). Ein altes gespeichertes Gespräch allein
     * reicht nie. Nicht über MY_PACKAGE_REPLACED: Das überspringt Samsung, wenn Jarvis beim Update vorn war.
     */
    private fun nachUpdateWiederherstellen() {
        val app = getApplication<Application>()
        val p = bildPrefs()
        val installiert = runCatching { app.packageManager.getPackageInfo(app.packageName, 0).lastUpdateTime }.getOrDefault(0L)
        val bekannt = p.getLong("installiert", 0L)
        val warSichtbar = p.getBoolean("sichtbar", false)
        val verlassen = p.getLong("verlassen", 0L)
        p.edit().putLong("installiert", installiert).apply()
        if (installiert == 0L || bekannt == 0L || installiert == bekannt) return
        val kurzVorher = warSichtbar || (verlassen > 0L && installiert - verlassen in 0..UPDATE_VORLAUF)
        val baldDanach = System.currentTimeMillis() - installiert in 0..UPDATE_NACHLAUF
        if (!kurzVorher || !baldDanach) {
            Logbuch.gedaechtnis("Start nach Update ohne Wiederherstellung", if (!kurzVorher) "Jarvis war vor dem Update nicht in Gebrauch" else "erst lange nach dem Update geöffnet")
            return
        }
        // Dieselbe Session: das zuletzt offene Gespräch (keins = Frank war in einem neuen, noch leeren Gespräch).
        val id = offenePrefs().getString("id", null)
        if (id != null) {
            // Vor dem Lesen setzen: Meldungen, die der Server genau jetzt nachliefert, gehen dann in das geladene Gespräch.
            programmierGespraech = id
            val geladen = de.frank.jarvis.data.Gespraeche.laden(app, id)
            if (geladen != null) {
                gespraech.addAll(geladen.second.map { Nachricht(it.vonMir, it.text, it.art, it.kontext, it.anhaenge, it.bewertung) })
                sitzungId = id
                sitzungErstellt = geladen.first.erstellt
                letzteAktivitaet = System.currentTimeMillis()
            } else programmierGespraech = null
        }
        // Derselbe Bildschirm.
        val r = runCatching { Reiter.valueOf(p.getString("reiter", null) ?: "") }.getOrNull() ?: Reiter.JARVIS
        if (r != Reiter.JARVIS) { reiterVerlauf.clear(); aktuellerReiter = r }
        einstellungsReiter = p.getString("einstellungen", null)?.takeIf { it.isNotBlank() } ?: "jarvis"
        einstellungsVerlauf = p.getString("einstellungsVerlauf", null).orEmpty().split(',').filter { it.isNotBlank() }
        GedaechtnisAnsicht.offen = p.getBoolean("gedaechtnis", false)
        val merk = p.getString("merk", null).orEmpty()
        listOf(symbioseListe, regelListe, zielListe, infoListe, programmierListe, stilListe, entscheidungsListe).firstOrNull { it.titel == merk }?.let { merkOeffnen(it) }
        Logbuch.gedaechtnis("Zustand nach Update wiederhergestellt", "Reiter ${r.anzeige}, ${gespraech.size} Nachrichten")
    }

    /**
     * Frank öffnet Jarvis wieder, nachdem es nicht auf dem Bildschirm war: Startbildschirm mit neuem, leerem Gespräch. Das bisherige
     * bleibt gespeichert; laufende Antworten und Programmieraufträge arbeiten in ihrem Gespräch weiter. Ein laufender Gesprächsmodus
     * oder eine Aufnahme bleibt, wo sie ist: Sonst landete die nächste gesprochene Frage mitten im Gespräch in einer neuen Session.
     */
    fun startbildschirmNeu() {
        // Auch während Jarvis im Gesprächsmodus nachdenkt oder vorliest und während eines Live-Gesprächs: Sonst endete die
        // Sprachschleife mit dem leeren Gespräch, und die Antwort käme nur noch in die Datei, ohne vorgelesen zu werden.
        if (freihaendig || nimmtAuf || schreibtMit || holtMikro || sprachmodusAktiv || weckwortLauf?.isActive == true || liveAktiv) return
        reiter = Reiter.JARVIS
        merkOffen = null
        diktatZiel = null
        GedaechtnisAnsicht.offen = false
        einstellungsReiter = "jarvis"
        einstellungsVerlauf = emptyList()
        navigationsAuswahl = null
        // Ohne vorleser.stopp(): Ein Vorlesen im Hintergrund (Tagesauswertung, Ablage) läuft weiter.
        if (gespraech.isNotEmpty() || sitzungId != null) {
            sitzungId?.let { vorherigeSitzung = it }
            gespraech.clear()
            sitzungId = null
            sitzungErstellt = 0L
            offenesGespraechVergessen()
        }
    }


    /** Ein altes Gespräch öffnen und dort weitermachen. Läuft im bisherigen Gespräch noch eine Antwort, arbeitet sie im Hintergrund weiter. */
    fun sitzungOeffnen(id: String) {
        if (id == sitzungId) return
        // Von Hand gewechselt: Aufnahme, Mitschrift und Gesprächsmodus der alten Session enden sofort, nicht erst nach dem Laden.
        aufnahmeVerwerfen()
        viewModelScope.launch { sitzungLaden(id) }
    }

    /** Lädt ein gespeichertes Gespräch in die Ansicht. false = ließ sich nicht öffnen. */
    private suspend fun sitzungLaden(id: String): Boolean {
        // Unter der Sicherung: Eine Antwort, die genau jetzt in diese Datei geschrieben wird, ist dann mit im geladenen Stand.
        // Die Ansicht wird noch unter der Sicherung umgestellt: Was hinter ihr wartet (Antwort einer Runde, Meldung eines
        // Programmierauftrags), sieht danach das geöffnete Gespräch und kommt in die Ansicht statt nur in die Datei.
        val geoeffnet = sicherung.withLock {
            ladeZiel = id
            try {
                val geladen = withContext(Dispatchers.IO) { de.frank.jarvis.data.Gespraeche.laden(getApplication(), id) } ?: return@withLock false
                vorleser.stopp()
                sitzungId?.takeIf { it != id }?.let { vorherigeSitzung = it }
                gespraech.clear()
                gespraech.addAll(geladen.second.map { Nachricht(it.vonMir, it.text, it.art, it.kontext, it.anhaenge, it.bewertung) })
                sitzungId = id
                sitzungErstellt = geladen.first.erstellt
                true
            } finally {
                if (ladeZiel == id) ladeZiel = null
            }
        }
        if (!geoeffnet) { meldung = "Dieses Gespräch ließ sich nicht öffnen."; sitzungenLaden(); return false }
        // Wer ein altes Gespräch öffnet, will dort weitersprechen, auch mit „Hey Jarvis“.
        letzteAktivitaet = System.currentTimeMillis()
        offenesGespraechMerken(id, letzteAktivitaet)
        return true
    }

    // ---- Wechsel zwischen Gesprächen per Sprache ----

    /** Das Gespräch vor dem jetzigen, für „Gehe in die letzte Session zurück“. */
    private var vorherigeSitzung: String? = null
    /** Offene Rückfrage „Welche Session meinst du?“ mit ihren Kandidaten. */
    private var navigationsAuswahl: List<SessionLage>? = null
    /** Ein Wechsel sucht oder lädt gerade; der Gesprächsmodus wartet darauf wie auf eine Antwort. */
    var navigiert by mutableStateOf(false); private set
    /** „Hey Jarvis“ bei sichtbarer App, während Jarvis noch antwortet: Die laufende Antwort weicht der neuen Frage, nicht einem Wechsel. */
    private var weichendeRunde: Runde? = null
    /** Zählt erledigte Wechsel: Der Gesprächsmodus hört danach weiter zu, obwohl im Gespräch keine neue Antwort steht. */
    private var navigationen = 0

    /** true = der Text war ein Navigationsbefehl (oder die Antwort auf die Rückfrage) und ist erledigt. */
    private fun navigieren(text: String, vorlesen: Boolean, antwort: ((String) -> Unit)? = null): Boolean {
        if (navigiert) return false
        navigationsAuswahl?.let { kandidaten ->
            navigationsAuswahl = null
            when (val wahl = GespraechsNavigation.waehle(text, kandidaten)) {
                is NaviWahl.Treffer -> { wechsleZu(wahl.sitzung.id, vorlesen, antwort); return true }
                NaviWahl.Abbruch -> { navigiertMelden("Okay, wir bleiben hier.", vorlesen, antwort); return true }
                NaviWahl.Keine -> Unit
            }
        }
        val befehl = GespraechsNavigation.erkenne(text) ?: return false
        Logbuch.gedaechtnis("Wechsel zwischen Gesprächen", text)
        reiter = Reiter.JARVIS
        merkOffen = null
        when (befehl) {
            NaviBefehl.Neu -> { gespraechLeeren(); navigiertMelden("Neue Session ist offen.", vorlesen, antwort) }
            else -> sessionSuchen(befehl, vorlesen, gespraech.takeLast(6).joinToString(" ") { it.text }, antwort)
        }
        return true
    }

    /**
     * Was Jarvis über seine Sessions weiß, bei jedem Wechsel frisch von der Platte: gespeicherte Gespräche (Titel, letzte
     * Änderung) und je Gespräch Stand, App und Inhalt seiner Programmieraufträge. So zählen Statusänderungen sofort, nicht ein
     * alter Titel in der Seitenleiste.
     */
    private fun sessionLagen(app: Application): List<SessionLage> {
        val auftraege = runCatching {
            Register.alle(app).filterIsInstance<de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit>().firstOrNull()?.auftragsLagen()
        }.getOrNull().orEmpty()
        return de.frank.jarvis.data.Gespraeche.alle(app).take(MAX_SUCHE).map { s ->
            val a = auftraege[s.id]
            if (a == null) SessionLage(s) else SessionLage(s, a.app, a.stand, a.aktiv, a.inhalt)
        }
    }

    private fun wechsleZu(id: String, vorlesen: Boolean, antwort: ((String) -> Unit)? = null) {
        navigiert = true
        viewModelScope.launch { try { oeffnenUndMelden(id, vorlesen, antwort) } finally { navigiert = false } }
    }

    private suspend fun oeffnenUndMelden(id: String, vorlesen: Boolean, antwort: ((String) -> Unit)? = null) {
        if (id == sitzungId) { navigiertMelden("Wir sind schon in „${titelVon(id)}“.", vorlesen, antwort); return }
        val geoeffnet = sitzungLaden(id)
        navigiertMelden(if (geoeffnet) "Session „${titelVon(id)}“ ist offen." else "Diese Session ließ sich nicht öffnen.", vorlesen, antwort)
    }

    /**
     * Sucht das Ziel von „zurück“, „die andere Programmier-Session“ und „die Whoop-Session“ im aktuellen Session-Bestand.
     * Ein klarer Treffer wird geöffnet, mehrere plausible führen zur kurzen Rückfrage statt zu raten. Nur Navigation: Hier
     * entsteht nie ein Programmierauftrag oder eine Ersatz-Session, und die Arbeit in der bisherigen Session läuft weiter.
     */
    private fun sessionSuchen(befehl: NaviBefehl, vorlesen: Boolean, kontext: String, antwort: ((String) -> Unit)? = null) {
        navigiert = true
        val aktuell = sitzungId
        val vorher = vorherigeSitzung
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val jetzt = System.currentTimeMillis()
                val inhalte = HashMap<String, String>()
                val inhalt: (SessionLage) -> String = { l ->
                    inhalte.getOrPut(l.sitzung.id) { de.frank.jarvis.data.Gespraeche.laden(app, l.sitzung.id)?.second.orEmpty().joinToString(" ") { it.text } }
                }
                val (ziel, mitDatum) = withContext(Dispatchers.IO) {
                    val lagen = sessionLagen(app)
                    fun zurueck(b: NaviBefehl.Zurueck): NaviZiel =
                        GespraechsNavigation.zurueckZiel(b, lagen, aktuell, vorher, jetzt)?.let { NaviZiel.Oeffnen(it) }
                            ?: NaviZiel.Keins(if (b.programmiert) "Es gibt noch keine andere Programmier-Session." else "Es gibt noch keine andere Session.")
                    fun thema(b: NaviBefehl.Thema): NaviZiel {
                        val k = GespraechsNavigation.themaZiel(b, lagen, aktuell, inhalt, jetzt)
                        return when (k.size) {
                            0 -> NaviZiel.Keins("Zu „${b.stichworte.joinToString(" ")}“ finde ich keine Session.")
                            1 -> NaviZiel.Oeffnen(k.first())
                            else -> NaviZiel.Rueckfrage(k)
                        }
                    }
                    when (befehl) {
                        is NaviBefehl.Zurueck -> zurueck(befehl) to false
                        is NaviBefehl.Thema -> thema(befehl) to true
                        is NaviBefehl.Andere -> (GespraechsNavigation.andereZiel(befehl, lagen, aktuell, kontext, inhalt, jetzt) ?: run {
                            // „Die andere Session“ ohne Programmier-Bezug: mit Stichwörtern wie „die Whoop-Session“, sonst wie „zurück“.
                            if (befehl.stichworte.isNotEmpty()) thema(NaviBefehl.Thema(befehl.stichworte, false)) else zurueck(NaviBefehl.Zurueck())
                        }) to befehl.stichworte.isNotEmpty()
                        NaviBefehl.Neu -> NaviZiel.Keins("") to false
                    }
                }
                Logbuch.gedaechtnis("Session-Suche", "${befehl.javaClass.simpleName}: " + when (ziel) {
                    is NaviZiel.Oeffnen -> "öffne ${ziel.lage.sitzung.id} (${ziel.lage.stand.ifEmpty { "ohne Auftrag" }})"
                    is NaviZiel.Rueckfrage -> "${ziel.kandidaten.size} Kandidaten"
                    is NaviZiel.Keins -> ziel.grund
                })
                when (ziel) {
                    is NaviZiel.Oeffnen -> oeffnenUndMelden(ziel.lage.sitzung.id, vorlesen, antwort)
                    is NaviZiel.Keins -> navigiertMelden(ziel.grund, vorlesen, antwort)
                    is NaviZiel.Rueckfrage -> {
                        navigationsAuswahl = ziel.kandidaten
                        val datum = java.text.SimpleDateFormat("d. MMMM", java.util.Locale.GERMAN)
                        val liste = ziel.kandidaten.mapIndexed { i, k ->
                            "${ORDINALE[i]} ${k.bezeichnung()}" + if (mitDatum && !k.offen(System.currentTimeMillis())) " vom ${datum.format(java.util.Date(k.sitzung.geaendert))}" else ""
                        }
                        val einleitung = if (ziel.kandidaten.all { it.offen(System.currentTimeMillis()) }) "Es laufen mehrere Programmier-Sessions" else "Dazu passen mehrere Sessions"
                        navigiertMelden("$einleitung: ${liste.joinToString(", ")}. Welche meinst du?", vorlesen, antwort)
                    }
                }
            } finally {
                navigiert = false
            }
        }
    }

    private fun titelVon(id: String): String = sitzungen.firstOrNull { it.id == id }?.titel?.takeIf { it.isNotBlank() } ?: "ohne Titel"

    /**
     * Bestätigung eines Wechsels: oben eingeblendet und, wenn gesprochen gefragt, vorgelesen. Nie ins Gespräch geschrieben.
     * Aus dem Live-Gespräch geht sie über [antwort] an das Live-Modell, das sie selbst spricht.
     */
    private fun navigiertMelden(text: String, vorlesen: Boolean, antwort: ((String) -> Unit)? = null) {
        meldung = text
        navigationen++
        antwort?.invoke(text)
        if (vorlesen && einstellungen.antwortenVorlesen) vorleser.sprich("navigation:" + System.currentTimeMillis(), "Jarvis", text)
    }

    fun sitzungUmbenennen(id: String, titel: String) {
        if (titel.isBlank()) return
        viewModelScope.launch {
            sitzungen = sicherung.withLock {
                withContext(Dispatchers.IO) {
                    de.frank.jarvis.data.Gespraeche.umbenennen(getApplication(), id, titel)
                    de.frank.jarvis.data.Gespraeche.alle(getApplication())
                }
            }
        }
    }

    fun sitzungLoeschen(id: String) {
        rundeAbbrechen(id)
        if (id == sitzungId) neuesGespraech()
        if (vorherigeSitzung == id) vorherigeSitzung = null
        if (navigationsAuswahl?.any { it.sitzung.id == id } == true) navigationsAuswahl = null
        viewModelScope.launch {
            sitzungen = sicherung.withLock {
                withContext(Dispatchers.IO) {
                    de.frank.jarvis.data.Gespraeche.loeschen(getApplication(), id)
                    de.frank.jarvis.data.Gespraeche.alle(getApplication())
                }
            }
            meldung = "Gespräch gelöscht."
        }
    }

    // ---- ChatGPT-Anmeldung ----

    fun kiVerbinden() {
        if (kiVerbindet) return
        kiVerbindet = true
        viewModelScope.launch {
            try {
                // Mit dem Application-Context: Die Anmeldung wartet lange und darf keine Activity festhalten (Fold auf- oder zuklappen).
                // Die Anmeldeseite öffnet Jarvis selbst: Die Rückkehr von dort ist kein neuer Start (wie startActivityForResult in MainActivity).
                // Erst unmittelbar vor dem Öffnen gesetzt: Scheitert die Anmeldung vorher, schluckte es sonst die nächste echte Rückkehr.
                auth.login(getApplication(), { de.frank.jarvis.MainActivity.sichtbar }, vorDemOeffnen = { eigenerAbsprung = true }) { info -> geraeteCode = info }
                kiVerbunden = true
                kiKonto = auth.email.orEmpty()
                meldung = "Mit ChatGPT verbunden."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                meldung = e.message ?: "Die Anmeldung ist fehlgeschlagen."
            } finally {
                geraeteCode = null
                kiVerbindet = false
                // Ist Jarvis jetzt sichtbar, war Frank schon zurück (oder nie weg): Das Zeichen hat ausgedient, auch nach Fehler
                // oder Abbruch. Ist er noch auf der Anmeldeseite, bleibt es für seine Rückkehr von dort stehen.
                if (de.frank.jarvis.MainActivity.sichtbar) eigenerAbsprung = false
            }
        }
    }

    fun kiAbbrechen() { auth.cancelLogin(); geraeteCode = null; kiVerbindet = false }

    fun kiTrennen() {
        auth.cancelLogin(); auth.logout()
        kiVerbunden = false; kiKonto = ""; geraeteCode = null
    }

    fun setzeModell(modell: CodexModel) {
        einstellungen.modell = modell
        einstellungen.denkstufe = modell.normalizeEffort(einstellungen.denkstufe)
    }

    fun setzeDenkstufe(stufe: ReasoningEffort) { einstellungen.denkstufe = stufe }
    fun setzeSchrittlimit(limit: Int) { einstellungen.schrittlimit = limit }

    // ---- Plugin und Tunnel ----

    fun serverSpeichern(host: String, token: String) {
        einstellungen.serverHost = host
        einstellungen.serverToken = token
        JarvisDienst.abgleichen(getApplication())
        meldung = "Gespeichert. Jarvis verbindet sich neu."
    }

    fun dienstSchalten(an: Boolean) {
        einstellungen.dienstAn = an
        JarvisDienst.abgleichen(getApplication())
    }

    fun adresseErneuern() {
        einstellungen.neuesGeheimnis()
        // Neu verbinden: Der Server lernt die neue Adresse erst beim Anmelden des Handys.
        JarvisDienst.abgleichen(getApplication())
        meldung = "Neue Plugin-Adresse erzeugt. Bitte in ChatGPT neu eintragen."
    }

    val pluginAdresse: String
        get() = einstellungen.serverHost.takeIf { it.isNotEmpty() }?.let { "https://$it/j/${einstellungen.geheimnis}/mcp" }.orEmpty()

    /** [geheim]: Zugangsdaten wie die Plugin-Adresse; Android zeigt sie dann nicht in der Vorschau der Zwischenablage. */
    fun kopiere(text: String, was: String, geheim: Boolean = false) {
        val app = getApplication<Application>()
        val clip = ClipData.newPlainText(was, text)
        if (geheim) clip.description.extras = android.os.PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
        app.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
        meldung = "$was kopiert."
    }

    fun ausZwischenablage(): String {
        val app = getApplication<Application>()
        return (app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
            ?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(app)?.toString()?.trim().orEmpty()
    }

    fun protokollLeeren() = Protokoll.leere()

    // ---- Wetter ----
    var wetterSuchtOrt by mutableStateOf(false); private set

    /** Sucht die Koordinaten zum Ortsnamen (Open-Meteo) und holt die Vorhersage für den neuen Ort. */
    fun wetterOrtSetzen(name: String) {
        if (name.isBlank() || wetterSuchtOrt) return
        wetterSuchtOrt = true
        viewModelScope.launch {
            val gefunden = withContext(Dispatchers.IO) {
                runCatching {
                    val adresse = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=de&name=" + java.net.URLEncoder.encode(name.trim(), "UTF-8")
                    okhttp3.OkHttpClient().newCall(okhttp3.Request.Builder().url(adresse).build()).execute().use { a ->
                        org.json.JSONObject(a.body?.string().orEmpty()).optJSONArray("results")?.optJSONObject(0)
                    }
                }.getOrNull()
            }
            if (gefunden == null) meldung = "Diesen Ort habe ich nicht gefunden."
            else {
                einstellungen.wetterBreite = gefunden.optDouble("latitude")
                einstellungen.wetterLaenge = gefunden.optDouble("longitude")
                einstellungen.wetterOrt = listOf(gefunden.optString("name"), gefunden.optString("admin1")).filter { it.isNotBlank() }.distinct().joinToString(", ")
                withContext(Dispatchers.IO) { Register.alle(getApplication()).filterIsInstance<de.frank.jarvis.faehigkeit.WetterFaehigkeit>().firstOrNull()?.synchronisiere() }
                meldung = "Wetter-Ort: ${einstellungen.wetterOrt}."
            }
            wetterSuchtOrt = false
        }
    }

    val kalenderSchreibenErlaubt: Boolean
        get() = androidx.core.content.ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.WRITE_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED

    // ---- Tagebuch: Drive-Berechtigung ----
    var driveErneuertGerade by mutableStateOf(false); private set

    /** Öffnet die Zustimmungsseite von Google und gibt den neuen Nur-Lese-Zugang an den Server weiter. */
    fun driveBerechtigungErneuern() {
        if (driveErneuertGerade) return
        driveErneuertGerade = true
        viewModelScope.launch {
            val (erfolg, text) = de.frank.jarvis.tunnel.DriveFreigabe.erneuere(getApplication())
            if (erfolg) withContext(Dispatchers.IO) { Register.alle(getApplication()).filterIsInstance<de.frank.jarvis.faehigkeit.TagebuchFaehigkeit>().firstOrNull()?.synchronisiere() }
            meldung = text
            driveErneuertGerade = false
            lagePruefen()
        }
    }

    // ---- E-Mail ----

    fun mailSpeichern(adresse: String, passwort: String, empfaenger: String) {
        einstellungen.mailAdresse = adresse
        // Leeres Feld heißt: das gespeicherte Passwort behalten.
        if (passwort.isNotBlank()) einstellungen.mailPasswort = passwort
        einstellungen.mailEmpfaenger = empfaenger
        lagePruefen()
        meldung = "E-Mail-Einstellungen gespeichert."
    }

    fun mailEntfernen() {
        einstellungen.mailPasswort = ""
        lagePruefen()
        meldung = "App-Passwort entfernt."
    }

    // ---- Pünktlich losfahren ----
    var standortAnfragen: () -> Unit = {}
    var standortErlaubt by mutableStateOf(false); private set
    var standortImmer by mutableStateOf(false); private set
    var abfahrtStatus by mutableStateOf(""); private set
    var fahrzeitText by mutableStateOf<String?>(null); private set
    var fahrzeitLaeuft by mutableStateOf(false); private set

    fun abfahrtLaden() {
        val app = getApplication<Application>()
        standortErlaubt = de.frank.jarvis.fahrt.Standort.erlaubt(app)
        standortImmer = de.frank.jarvis.fahrt.Standort.immerErlaubt(app)
        viewModelScope.launch { abfahrtStatus = withContext(Dispatchers.IO) { de.frank.jarvis.fahrt.Abfahrt.status(app) } }
    }

    /** Ankunftszeit, Adresse oder Schalter geändert: Die laufende Planung gilt nicht mehr, neu rechnen lassen. */
    fun abfahrtGeaendert() {
        einstellungen.abfahrtZustand = ""
        de.frank.jarvis.fahrt.Abfahrt.stelle(getApplication())
        abfahrtLaden()
    }

    /** Spielt eine Beispiel-Meldung so ab, wie die echte kommt: Benachrichtigung, Stimme, eingestellte Lautstärke. */
    fun abfahrtProbe() = de.frank.jarvis.fahrt.Abfahrt.probe(getApplication())

    /** Fahrzeit vom Standort (ersatzweise von zu Hause) zu „arbeit“, „zuhause“ oder einer Adresse. */
    fun fahrzeitPruefen(ziel: String) {
        if (ziel.isBlank() || fahrzeitLaeuft) return
        fahrzeitLaeuft = true
        viewModelScope.launch {
            val werkzeug = Register.werkzeuge(getApplication()).firstOrNull { it.name == "fahrzeit_lesen" }
            fahrzeitText = withContext(Dispatchers.IO) { runCatching { werkzeug?.ausfuehren(org.json.JSONObject().put("ziel", ziel.trim()))?.text }.getOrNull() }
                ?.substringBefore("\nLosfahr-Meldung:") ?: "Die Fahrzeit ließ sich nicht abrufen."
            fahrzeitLaeuft = false
            abfahrtLaden()
        }
    }

    // ---- Tagesauswertung ----

    fun auswertungJetzt() {
        JarvisDienst.auswerten(getApplication(), "von Hand in der App gestartet")
        meldung = "Jarvis erstellt die Tagesauswertung. Das dauert ein bis drei Minuten."
    }

    fun auswertungSchalten(an: Boolean) {
        einstellungen.auswertungAn = an
        de.frank.jarvis.auswertung.Zeitplan.stelle(getApplication())
    }

    fun auswertungAbstand(stunden: Int) { einstellungen.auswertungAbstand = stunden; de.frank.jarvis.auswertung.Zeitplan.stelle(getApplication()) }
    fun auswertungSchlafpause(an: Boolean) { einstellungen.auswertungSchlafpause = an; de.frank.jarvis.auswertung.Zeitplan.stelle(getApplication()) }
    fun auswertungDeutung(an: Boolean) { einstellungen.auswertungDeutung = an }

    val naechsteAuswertung: String
        get() = de.frank.jarvis.auswertung.Zeitplan.naechster(getApplication())?.let {
            (if (it.toLocalDate() == java.time.LocalDate.now()) "heute" else "morgen") + " um " + it.toLocalTime().toString().take(5) + " Uhr"
        } ?: "ausgeschaltet"
}


/** Text einer Frage, die nur aus Anhängen besteht. */
private const val ANHANG_PLATZHALTER = "Schau dir den Anhang an."

/** So viele der zuletzt geführten Gespräche durchsucht „Gehe in die Whoop-Session zurück“. */
private const val MAX_SUCHE = 150

private val ORDINALE = listOf("erstens", "zweitens", "drittens")

/** Höchstens so viele frühere Nachrichten gehen als Verlauf an das Modell (der Agent kürzt zusätzlich nach Zeichen). */
private const val MAX_VERLAUF = 40

/** So oft höchstens fragt das Zurückkehren in die App die angebundenen Apps ab. */
private const val LAGE_ABSTAND_MS = 5 * 60_000L

/** Auftrag an das Modell für die KI-Korrektur einer einzelnen Regel. */
private const val REGEL_KORREKTUR =
    "Du formulierst eine Vorgabe von Frank zu einer klaren Regel für seinen Assistenten Jarvis um. Frank hat sie eingesprochen oder eingetippt; " +
        "Jarvis bekommt die Regel danach bei jeder Antwort als Anweisung beigelegt und muss sie ohne weiteres Wissen verstehen. " +
        "Schreibe sie in gutem Deutsch als vollständige, für sich allein verständliche Anweisung an Jarvis: wann sie gilt und was dann zu tun ist. " +
        "Behalte jeden inhaltlichen Punkt und jeden genannten Wert, füge nichts hinzu und deute nichts um. Lass Füllwörter, Wiederholungen und Selbstkorrekturen weg " +
        "und berichtige Hörfehler der Spracherkennung (zum Beispiel „Javis“ für Jarvis, „WUPP“ für Whoop). So kurz wie möglich, höchstens 400 Zeichen. " +
        "Antworte nur mit der Regel, ohne Anführungszeichen, ohne Einleitung und ohne Erklärung."

/** Auftrag an das Modell für die KI-Korrektur einer einzelnen Regel der Zusammenarbeit mit ChatGPT. */
private const val SYMBIOSE_KORREKTUR =
    "Du formulierst eine Regel für die Zusammenarbeit zwischen ChatGPT Live (der Sprachmodus der ChatGPT-App) und Franks Assistenten-App Jarvis um. Sie steht in ChatGPTs benutzerdefinierten Anweisungen " +
        "und spricht ChatGPT mit „du“ an, Frank spricht als „ich“. Beginne mit einem Stichwort und Doppelpunkt (zum Beispiel „Termine:“). Behalte jeden inhaltlichen Punkt und genannte Werkzeugnamen genau, " +
        "füge nichts hinzu. Lass Füllwörter weg und berichtige Hörfehler der Spracherkennung. So kurz wie möglich, höchstens 400 Zeichen. Antworte nur mit der Regel, ohne Anführungszeichen und ohne Erklärung."

/** Auftrag an das Modell für die KI-Korrektur einer einzelnen Entscheidung. */
private const val ENTSCHEIDUNG_KORREKTUR =
    "Du formulierst eine Entscheidung, die Frank getroffen hat, zu einem klaren, für sich allein verständlichen Satz in der dritten Person um („Frank hat entschieden, …“), mit Datum, wenn er eines nennt. " +
        "Behalte jeden inhaltlichen Punkt, füge nichts hinzu und deute nichts um; mach aus einer Überlegung keinen Beschluss. " +
        "Lass Füllwörter und Selbstkorrekturen weg und berichtige Hörfehler der Spracherkennung. So kurz wie möglich, höchstens 400 Zeichen. " +
        "Antworte nur mit dem Satz, ohne Anführungszeichen, ohne Einleitung und ohne Erklärung."

/** Auftrag an das Modell für die KI-Korrektur einer einzelnen Stilregel. */
private const val STIL_KORREKTUR =
    "Du formulierst eine Vorgabe von Frank, wie die Antworten seines Assistenten Jarvis aussehen sollen (Länge, Gliederung, Ton, Symbole), zu einer klaren Stilregel um. " +
        "Jarvis bekommt sie bei jeder Antwort beigelegt und muss sie ohne weiteres Wissen verstehen. Behalte jeden inhaltlichen Punkt, füge nichts hinzu und deute nichts um. " +
        "Lass Füllwörter und Selbstkorrekturen weg und berichtige Hörfehler der Spracherkennung. So kurz wie möglich, höchstens 400 Zeichen. " +
        "Antworte nur mit der Regel, ohne Anführungszeichen, ohne Einleitung und ohne Erklärung."

/** Auftrag an das Modell für die KI-Korrektur einer einzelnen Programmierregel. */
private const val PROGRAMMIERREGEL_KORREKTUR =
    "Du formulierst eine Vorgabe von Frank zu einer klaren Programmierregel um. Frank hat sie eingesprochen oder eingetippt; " +
        "Jarvis nutzt die Regel danach als eigenen Kontext, um Programmieraufträge zu steuern und den Programmierer passend zu instruieren, und muss sie ohne weiteres Wissen verstehen. " +
        "Schreibe sie in gutem Deutsch als vollständige, für sich allein verständliche Anweisung: wann sie gilt und was dann zu tun ist. " +
        "Behalte jeden inhaltlichen Punkt, jeden Namen, jeden Pfad und jeden genannten Wert genau so, füge nichts hinzu und deute nichts um. Lass Füllwörter, Wiederholungen und Selbstkorrekturen weg " +
        "und berichtige Hörfehler der Spracherkennung. So kurz wie möglich, höchstens 400 Zeichen. " +
        "Antworte nur mit der Regel, ohne Anführungszeichen, ohne Einleitung und ohne Erklärung."

/** Auftrag an das Modell für die KI-Korrektur eines einzelnen Ziels. */
private const val ZIEL_KORREKTUR =
    "Du formulierst ein Ziel, das Frank eingesprochen oder eingetippt hat, zu einem klaren Eintrag für seinen Assistenten Jarvis um. " +
        "Jarvis misst seine Vorschläge künftig daran und muss das Ziel ohne weiteres Wissen verstehen. " +
        "Schreibe es in gutem Deutsch als kurzen Satz in der dritten Person, der mit „Frank will“ oder „Frank möchte“ beginnt (bei mehreren Zielen höchstens drei Sätze). " +
        "Behalte jede genannte Zahl und Frist genau so, füge nichts hinzu und deute nichts um. Lass Füllwörter und Wiederholungen weg " +
        "und berichtige offensichtliche Hörfehler der Spracherkennung. Höchstens 300 Zeichen. " +
        "Antworte nur mit dem Ziel, ohne Anführungszeichen, ohne Einleitung und ohne Erklärung."

/** Auftrag an das Modell für die KI-Korrektur einer einzelnen Notiz über Frank. */
private const val INFO_KORREKTUR =
    "Du formulierst eine Angabe, die Frank über sich selbst eingesprochen oder eingetippt hat, zu einer klaren Notiz für seinen Assistenten Jarvis um. " +
        "Jarvis hat die Notiz danach bei jeder Antwort vor Augen und muss sie ohne weiteres Wissen verstehen. " +
        "Schreibe sie in gutem Deutsch als kurzen, für sich allein verständlichen Satz über Frank in der dritten Person (bei mehreren Angaben höchstens drei Sätze). " +
        "Behalte jede Tatsache, jeden Namen und jede Zahl genau so, füge nichts hinzu und deute nichts um. Lass Füllwörter, Wiederholungen und Selbstkorrekturen weg " +
        "und berichtige offensichtliche Hörfehler der Spracherkennung. Höchstens 300 Zeichen. " +
        "Antworte nur mit der Notiz, ohne Anführungszeichen, ohne Einleitung und ohne Erklärung."

/** Auftrag für die Überschrift eines Gesprächs in der Seitenleiste. */
private const val TITEL_AUFTRAG =
    "Du gibst einem Gespräch zwischen Frank und seinem Assistenten Jarvis eine Überschrift für die Gesprächsliste. " +
        "Fasse in höchstens sieben Wörtern auf Deutsch zusammen, worum es geht; meist trifft es Franks erstes Anliegen, " +
        "hat sich das Gespräch verlagert, das Hauptthema. Klar und konkret wie ein Betreff, keine Anführungszeichen, " +
        "kein Punkt am Ende, keine Einleitung. Antworte nur mit der Überschrift."

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

enum class Reiter(val anzeige: String) { JARVIS("Jarvis"), ABLAGE("Ablage"), AKTIVITAET("Aktivität"), EINSTELLUNGEN("Einstellungen") }

/**
 * Eine Nachricht im Gespräch. [art]: „antwort“, „fehler“ (Fehlermeldung statt Antwort, geht nicht ans Modell) oder
 * „abgebrochen“ (Franks Frage blieb unbeantwortet). [kontext] sieht nur das Modell, etwa was in der Runde erledigt wurde.
 */
data class Nachricht(val vonMir: Boolean, val text: String, val art: String = "antwort", val kontext: String = "")

/** Ein Schritt für „Rückgängig“ in einer Merkliste: wie sich die eigene Änderung umkehren lässt, ohne fremde zu berühren. */
private sealed class MerkSchritt {
    class Angelegt(val regel: Regel) : MerkSchritt()
    class Geaendert(val id: Int, val alterText: String, val neuerText: String) : MerkSchritt()
    class Geloescht(val regel: Regel) : MerkSchritt()
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val einstellungen = Einstellungen.get(app)
    private val agent = JarvisAgent(app)
    private val auth get() = agent.auth

    var reiter by mutableStateOf(Reiter.JARVIS)

    // ---- Sperre ----
    /** true, solange die App auf den Fingerabdruck wartet. Der Dienst im Hintergrund läuft davon unberührt. */
    var gesperrt by mutableStateOf(false)
    /** Wann die App zuletzt sichtbar war; 0 = seit dem Start noch nie entsperrt. */
    var zuletztSichtbar = 0L
    /** Bis wann die Sperre nach einem bewusst gestarteten Teilen, Öffnen oder „Speichern unter“ wartet (sonst ginge das Ergebnis verloren). */
    var sperrAufschubBis = 0L
    var entsperrenAnfragen: () -> Unit = {}
    /** Jarvis läuft per „Hey Jarvis“ über dem Sperrbildschirm; das Gespräch bleibt dabei verborgen. */
    var ueberSperre = false
    /** Beendet das Zeigen über der Sperre in der gerade lebenden Activity. */
    var ueberSperreBeenden: () -> Unit = {}

    // ---- Sprache ----
    val sprache = de.frank.jarvis.data.settings.SecureSettings(app)
    val vorleser: de.frank.jarvis.speech.Vorleser = de.frank.jarvis.speech.Vorleser.hole(app, sprache)
    private val mikro = de.frank.jarvis.audio.MicRecorder(app)
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

    fun sende(eingabe: String) {
        // Vorgelesen wird nur die Antwort auf genau diese gesprochene Frage. Endet sie vorzeitig (Abbruch, leer, nicht
        // verbunden), darf die nächste getippte Antwort nicht laut werden.
        val vorlesen = gesprochen
        gesprochen = false
        val text = eingabe.trim()
        if (text.isEmpty() || denkt) return
        if (!kiVerbunden) {
            meldung = "Bitte zuerst mit ChatGPT verbinden."
            reiter = Reiter.EINSTELLUNGEN
            return
        }
        val verlauf = verlaufFuersModell()
        gespraech += Nachricht(true, text)
        sitzungSichern()
        val id = sitzungId ?: return
        val runde = Runde(id, gespraech.size - 1)
        laufende[id] = runde
        // Die Kennung des Gesprächs läuft im Koroutinen-Kontext mit: Startet Jarvis in dieser Runde einen Programmierauftrag,
        // kommen dessen Meldungen in genau dieses Gespräch, auch wenn inzwischen andere Gespräche arbeiten.
        runde.job = viewModelScope.launch(de.frank.jarvis.faehigkeit.ProgrammierChat.GespraechKennung(id)) {
            var art = "antwort"
            val antwort = try {
                agent.versuche(text, verlauf, beiSchritt = { runde.schritt = it }, lernen = true, abschluss = true, beiAenderung = { runde.erledigt += it })
                    ?: (if (!agent.verbunden) JarvisAgent.NICHT_VERBUNDEN else agent.ersatzText()).also { art = "fehler" }
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
            val i = runde.frage
            val n = gespraech.getOrNull(i)
            if (frageNotiz.isNotEmpty() && n != null && n.vonMir && i == gespraech.lastIndex) gespraech[i] = n.copy(kontext = frageNotiz)
            gespraech += neu
            sitzungSichern()
            if (vorlesen && einstellungen.antwortenVorlesen) vorleser.sprich("gespraech:" + (gespraech.size - 1), "Jarvis", antwort)
            return
        }
        // Frank ist inzwischen in einem anderen Gespräch: Die Antwort kommt in die gespeicherte Datei und steht beim nächsten Öffnen da.
        viewModelScope.launch {
            sicherung.withLock {
                withContext(Dispatchers.IO) {
                    if (frageNotiz.isNotEmpty()) de.frank.jarvis.data.Gespraeche.letzteFrageKontext(getApplication(), runde.sitzung, frageNotiz)
                    de.frank.jarvis.data.Gespraeche.anhaengen(getApplication(), runde.sitzung, de.frank.jarvis.data.GespeicherteNachricht(false, neu.text, neu.art, neu.kontext))
                }
            }
            sitzungenLaden()
            de.frank.jarvis.gedaechtnis.Lagebild.anstossen(getApplication(), listOf(de.frank.jarvis.gedaechtnis.Bereich.GESPRAECHE), "Gespräch mit Jarvis")
        }
    }

    private fun erledigtNotiz(kopf: String, erledigt: List<String>): String =
        synchronized(erledigt) { erledigt.toList() }.takeIf { it.isNotEmpty() }?.let { "($kopf: " + it.joinToString("; ") + ")" }.orEmpty()

    /**
     * Der Verlauf fürs Modell: Fehlermeldungen fallen weg (sie sind keine Antworten), unbeantwortete Fragen sind als solche
     * markiert, und der unsichtbare Kontext (was erledigt wurde) geht mit. Gekürzt wird nach Zeichen im Agenten.
     */
    private fun verlaufFuersModell(): List<ChatTurn> {
        val sicht = gespraech.takeLast(MAX_VERLAUF).filter { it.art != "fehler" }
        return sicht.mapIndexed { i, n ->
            val text = if (n.kontext.isEmpty()) n.text else n.text + "\n" + n.kontext
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
    ) {
        var eintraege by mutableStateOf(emptyList<de.frank.jarvis.faehigkeit.Regel>()); private set
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
                    val neu = withContext(Dispatchers.IO) { datei.alle(getApplication()) }.sortedByDescending { it.id }
                    eintraege = neu
                    if (neu.none { it.id == letzter }) letzter = null
                }
            }
        }

        fun laden() { eintraege = datei.alle(getApplication()).sortedByDescending { it.id } }

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
    /** Die Merkliste, die gerade im Vollbild offen ist. */
    var merkOffen by mutableStateOf<Merkliste?>(null); private set
    /** Wohin der Text der laufenden Aufnahme geht, wenn nicht ins Gespräch. */
    private var diktatZiel: ((String) -> Unit)? = null

    fun merkLaden() { regelListe.laden(); zielListe.laden(); infoListe.laden() }
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

    /** Tipp auf den Mikrofon-Knopf: Aufnahme starten oder beenden und abschicken. */
    fun mikrofonTippen() {
        // Der Knopf im Gespräch schickt immer an Jarvis, nie in eine geschlossene Merkliste.
        if (merkOffen == null) diktatZiel = null
        when {
            schreibtMit || denkt -> Unit
            nimmtAuf -> aufnahmeBeenden()
            sprache.groqApiKey.isBlank() -> { meldung = "Für das Mikrofon bitte zuerst den Groq-Schlüssel eintragen."; reiter = Reiter.EINSTELLUNGEN }
            else -> mikrofonAnfragen()
        }
    }

    fun mikrofonErlaubt(erlaubt: Boolean) {
        if (!erlaubt) { diktatZiel = null; meldung = "Ohne Mikrofon-Erlaubnis kann ich dich nicht hören."; return }
        if (holtMikro) return
        vorleser.stopp()
        holtMikro = true
        viewModelScope.launch {
            try {
                // Von Hand gestartet: Die Aufnahme läuft weiter, auch wenn Frank in eine andere App wechselt, bis er
                // wieder auf das Mikrofon tippt. Dafür braucht Android einen Mikrofon-Dienst im Vordergrund.
                if (!aufnahmeStarten()) { diktatZiel = null; meldung = "Die Aufnahme ließ sich nicht starten." }
                else de.frank.jarvis.audio.AufnahmeDienst.starten(getApplication())
            } finally {
                holtMikro = false
            }
        }
    }

    /** Wartet, bis „Hey Jarvis“ das Mikrofon losgelassen hat, und startet dann die Aufnahme. */
    private suspend fun aufnahmeStarten(): Boolean {
        de.frank.jarvis.weckwort.Weckwort.mikrofonHolen()
        if (mikro.start(viewModelScope)) { nimmtAuf = true; return true }
        de.frank.jarvis.weckwort.Weckwort.mikrofonFreigeben()
        return false
    }
    private var holtMikro = false

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
        de.frank.jarvis.weckwort.Weckwort.schwelle = einstellungen.weckwortSchwelle
        // Die eigene Phrase bekommt ihre Schwelle beim Anlegen: Dienst neu einlesen lassen.
        if (einstellungen.weckwortArt == "phrase") de.frank.jarvis.weckwort.WeckwortDienst.neuLaden(getApplication())
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
    fun weckwortStart(fertig: () -> Unit) {
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
        if (denkt) abbrechen()
        reiter = Reiter.JARVIS
        merkOffen = null
        // Auch die Gedächtnis-Ansicht schließen: Sie verdeckt sonst Gespräch und Reiter-Leiste.
        GedaechtnisAnsicht.offen = false
        diktatZiel = null
        vorleser.stopp()
        // Activity oder ViewModel wurden inzwischen neu aufgebaut: das offene Gespräch der letzten zehn Minuten zurückholen.
        if (gespraech.isEmpty() && sitzungId == null) offenesGespraechWiederherstellen()
        // Lange nichts mehr gesagt: „Hey Jarvis“ beginnt ein neues Gespräch. Sonst geht das offene weiter.
        if (gespraech.isNotEmpty() && System.currentTimeMillis() - letzteAktivitaet > NEUES_GESPRAECH_NACH_MS) neuesGespraech()
        val treffer = de.frank.jarvis.weckwort.Weckwort.trefferMerkmale
        de.frank.jarvis.weckwort.Weckwort.trefferMerkmale = null
        weckwortLauf?.cancel()
        weckwortLauf = viewModelScope.launch {
            // Für das ganze Gespräch den Audiofokus halten: Andere Apps bleiben leise, bis Jarvis fertig ist.
            de.frank.jarvis.speech.GespraechsFokus.halten(getApplication())
            try {
                // Gesprächsmodus: Nach jeder vorgelesenen Antwort piept es wieder und Jarvis hört weiter zu, bis
                // nach dem Piepton nichts mehr kommt. Das Mikrofon bleibt dabei für Jarvis reserviert, sonst
                // schnappt es sich zwischen zwei Runden der Weckwort-Dienst.
                var runde = 0
                while (true) {
                    val antworten = gespraech.count { !it.vonMir }
                    val ergebnis = runde(erste = runde == 0)
                    if (ergebnis == de.frank.jarvis.weckwort.SprechEnde.Lage.NICHTS_GEHOERT) {
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
                    snapshotFlow { schreibtMit || denkt }.first { !it }
                    vorleser.stand.first { it.zustand == de.frank.jarvis.speech.VorleseZustand.AUS }
                    // Keine neue Antwort (abgebrochen, nichts verstanden) oder Frank hat selbst übernommen: Schluss.
                    if (gespraech.count { !it.vonMir } <= antworten || nimmtAuf || holtMikro) break
                    kotlinx.coroutines.delay(300)
                    runde++
                }
            } finally {
                de.frank.jarvis.speech.GespraechsFokus.freigeben()
                freihaendig = false
                if (!nimmtAuf && !holtMikro) de.frank.jarvis.weckwort.Weckwort.mikrofonFreigeben()
                fertig()
            }
        }
    }

    /**
     * Eine Runde im Gesprächsmodus: Mikrofon holen, piepen (ab jetzt sprechen), aufnehmen, bis Frank 4,5 Sekunden
     * schweigt, und abschicken. Liefert [SprechEnde.Lage.NICHTS_GEHOERT], wenn nach dem Piepton nichts kam.
     */
    private var mikroFehlte = false

    private suspend fun runde(erste: Boolean): de.frank.jarvis.weckwort.SprechEnde.Lage {
        holtMikro = true
        val gestartet = try { aufnahmeStarten() } finally { holtMikro = false }
        mikroFehlte = !gestartet
        if (!gestartet) { meldung = "Die Aufnahme ließ sich nicht starten."; return de.frank.jarvis.weckwort.SprechEnde.Lage.NICHTS_GEHOERT }
        freihaendig = true
        // Nach einer Antwort darf Frank etwas länger überlegen, bevor er anfängt.
        val ende = de.frank.jarvis.weckwort.SprechEnde(wartenMs = if (erste) 7_000 else 9_000)
        // Die Uhr läuft ab Beginn des Pieptons; SprechEnde überspringt genau dessen Dauer.
        val start = System.currentTimeMillis()
        piep()
        var lage = de.frank.jarvis.weckwort.SprechEnde.Lage.WEITER
        while (nimmtAuf && lage == de.frank.jarvis.weckwort.SprechEnde.Lage.WEITER) {
            kotlinx.coroutines.delay(50)
            lage = ende.neu(pegel.value, System.currentTimeMillis() - start)
        }
        freihaendig = false
        when {
            !nimmtAuf -> return de.frank.jarvis.weckwort.SprechEnde.Lage.FERTIG // Frank hat selbst auf Stopp getippt: Das Abschicken läuft schon.
            lage == de.frank.jarvis.weckwort.SprechEnde.Lage.NICHTS_GEHOERT -> {
                nimmtAuf = false
                mikro.stop()
                return lage
            }
            else -> aufnahmeBeenden(freigeben = false)
        }
        return de.frank.jarvis.weckwort.SprechEnde.Lage.FERTIG
    }

    /** „Ende“, „Stopp“, „Danke, das war's“ …: beendet den Gesprächsmodus. Nur wenn der ganze Satz ein solcher Befehl ist. */
    private fun istAusbefehl(text: String): Boolean {
        val worte = text.lowercase(java.util.Locale.GERMAN).replace("’", "'").replace(Regex("[^a-zäöüß' ]"), " ")
            .split(' ').filter { it.isNotBlank() && it !in FUELLWORTE }
        val satz = worte.joinToString(" ").replace("'", "")
        return satz in AUSBEFEHLE
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
        schreibtMit = true
        // Ein Diktat geht nur in eine Merkliste, solange sie offen ist; sonst ist der Text eine Frage an Jarvis.
        val ziel = diktatZiel?.takeIf { merkOffen != null }
        diktatZiel = null
        viewModelScope.launch {
            try {
                val wav = mikro.stop()
                de.frank.jarvis.audio.AufnahmeDienst.stoppen()
                if (freigeben && weckwortLauf?.isActive != true) de.frank.jarvis.weckwort.Weckwort.mikrofonFreigeben()
                if (wav == null || wav.size < 2000) { meldung = "Die Aufnahme war zu kurz."; return@launch }
                val beginn = System.currentTimeMillis()
                val groq = transkribierer()
                val text = withContext(Dispatchers.IO) { de.frank.jarvis.audio.Diktat(groq).transkribiere(wav).text.trim() }
                Logbuch.sprache("Mitschrift", "${wav.size / 1024} KB Aufnahme, ${text.length} Zeichen", System.currentTimeMillis() - beginn)
                if (text.isBlank()) meldung = "Ich habe nichts verstanden."
                else if (ziel != null) ziel(text)
                else if (weckwortLauf?.isActive == true && istAusbefehl(text)) {
                    // Ausbefehl im Gesprächsmodus: kurz bestätigen, danach kein Piepton und kein offenes Mikrofon mehr.
                    // Ohne neue Antwort im Gespräch endet die Schleife in weckwortStart von selbst.
                    Logbuch.sprache("Sprachsitzung beendet", text)
                    vorleser.sprich("sitzung-ende", "Jarvis", "Okay, Sprachsitzung beendet.")
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
    private var groq: de.frank.jarvis.audio.GroqTranscriber? = null
    private var groqStand = 0

    /** Der Transkribierer zu den aktuellen Einstellungen; neu gebaut nur, wenn sich Schlüssel oder Filter geändert haben. */
    private fun transkribierer(): de.frank.jarvis.audio.GroqTranscriber {
        val stand = listOf(sprache.groqApiKey, sprache.filterStilleVorabAn, sprache.filterSegmentmetrikenAn, sprache.filterZeitstempelAn, sprache.filterFloskelnAn).hashCode()
        groq?.takeIf { groqStand == stand }?.let { return it }
        groq?.shutdown()
        return de.frank.jarvis.audio.GroqTranscriber(
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

    // ---- Programmieraufträge im Gespräch ----

    /**
     * Das Gespräch, das die App beim Start wegen eines laufenden Programmierauftrags öffnet. Gesetzt, bevor die Datei gelesen
     * wird: Meldungen, die der Server genau in diesem Moment nachliefert (nach einem Update von Jarvis), gehen dann in das
     * geladene Gespräch statt in die Datei, die gerade gelesen wird.
     */
    @Volatile private var programmierGespraech: String? = null

    private val programmierEmpfaenger: (String, de.frank.jarvis.data.GespeicherteNachricht) -> Boolean = { id, n ->
        if (id != sitzungId && id != programmierGespraech) false else {
            viewModelScope.launch(Dispatchers.Main) {
                if (id == sitzungId) {
                    gespraech += Nachricht(n.vonMir, n.text, n.art, n.kontext)
                    sitzungSichern()
                } else withContext(Dispatchers.IO) { de.frank.jarvis.data.Gespraeche.anhaengen(getApplication(), id, n) }
            }
            true
        }
    }

    private val programmierBeantwortet: (String, String, String) -> Boolean = { id, frageId, antwort ->
        if (id != sitzungId && id != programmierGespraech) false else {
            viewModelScope.launch(Dispatchers.Main) {
                val i = gespraech.indexOfLast { it.art == "rueckfrage" && it.kontext.contains(frageId) }
                if (id == sitzungId && i >= 0) {
                    val k = runCatching { org.json.JSONObject(gespraech[i].kontext) }.getOrNull() ?: return@launch
                    gespraech[i] = gespraech[i].copy(kontext = k.put("beantwortet", true).put("antwort", antwort).toString())
                    sitzungSichern()
                } else withContext(Dispatchers.IO) { de.frank.jarvis.data.Gespraeche.rueckfrageBeantwortet(getApplication(), id, frageId, antwort) }
            }
            true
        }
    }

    init {
        de.frank.jarvis.faehigkeit.ProgrammierChat.empfaenger = programmierEmpfaenger
        de.frank.jarvis.faehigkeit.ProgrammierChat.beantwortet = programmierBeantwortet
    }

    /** Knopf in einer Rückfrage: Franks Antwort an die wartende Programmier-Sitzung. */
    fun programmierAntwort(frageId: String, antworten: List<String>, text: String) {
        viewModelScope.launch {
            val faehigkeit = de.frank.jarvis.faehigkeit.Register.alle(getApplication()).filterIsInstance<de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit>().firstOrNull()
            meldung = runCatching { faehigkeit?.beantworte(frageId, antworten, text) ?: "Programmieren ist nicht verfügbar." }
                .getOrElse { it.message ?: "Die Antwort ging nicht raus." }
        }
    }

    override fun onCleared() {
        if (de.frank.jarvis.faehigkeit.ProgrammierChat.empfaenger === programmierEmpfaenger) de.frank.jarvis.faehigkeit.ProgrammierChat.empfaenger = null
        if (de.frank.jarvis.faehigkeit.ProgrammierChat.beantwortet === programmierBeantwortet) de.frank.jarvis.faehigkeit.ProgrammierChat.beantwortet = null
        if (nimmtAuf || holtMikro) de.frank.jarvis.weckwort.Weckwort.mikrofonFreigeben()
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
        laufendenAuftragOeffnen()
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

    private fun sitzungSichern() {
        letzteAktivitaet = System.currentTimeMillis()
        val id = sitzungId ?: de.frank.jarvis.data.Gespraeche.neueId().also { sitzungId = it; sitzungErstellt = System.currentTimeMillis() }
        val erstellt = sitzungErstellt
        val stand = gespraech.map { de.frank.jarvis.data.GespeicherteNachricht(it.vonMir, it.text, it.art, it.kontext) }
        offenesGespraechMerken(id, letzteAktivitaet)
        viewModelScope.launch {
            // Nur dieses eine Gespräch schreiben und in der Liste ersetzen, statt alle Dateien neu zu lesen.
            val neu = sicherung.withLock { withContext(Dispatchers.IO) { de.frank.jarvis.data.Gespraeche.speichern(getApplication(), id, erstellt, stand) } }
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
        vorleser.stopp()
        gespraech.clear()
        sitzungId = null
        sitzungErstellt = 0L
        offenesGespraechVergessen()
    }

    // Das offene Gespräch überlebt den Neuaufbau von Activity und ViewModel (Wischen aus „Zuletzt verwendet“, Prozessende):
    // „Hey Jarvis“ führt es zehn Minuten lang weiter, wie im laufenden Betrieb.
    private fun offenePrefs() = getApplication<Application>().getSharedPreferences("jarvis_offenes_gespraech", Context.MODE_PRIVATE)

    private fun offenesGespraechMerken(id: String, zeit: Long) {
        offenePrefs().edit().putString("id", id).putLong("zeit", zeit).apply()
    }

    private fun offenesGespraechVergessen() {
        offenePrefs().edit().clear().apply()
    }

    /**
     * Läuft ein Programmierauftrag aus einem Gespräch noch, öffnet die App beim Start genau dieses Gespräch (zum Beispiel nach
     * einem Update von Jarvis selbst, bei dem Android die App beendet). Klein, deshalb direkt.
     */
    private fun laufendenAuftragOeffnen() {
        if (gespraech.isNotEmpty() || sitzungId != null) return
        val id = runCatching {
            de.frank.jarvis.faehigkeit.Register.alle(getApplication()).filterIsInstance<de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit>().firstOrNull()?.laufendesGespraech()
        }.getOrNull() ?: return
        programmierGespraech = id
        val geladen = de.frank.jarvis.data.Gespraeche.laden(getApplication(), id) ?: run { programmierGespraech = null; return }
        gespraech.addAll(geladen.second.map { Nachricht(it.vonMir, it.text, it.art, it.kontext) })
        sitzungId = id
        sitzungErstellt = geladen.first.erstellt
        letzteAktivitaet = System.currentTimeMillis()
        offenesGespraechMerken(id, letzteAktivitaet)
        Logbuch.gedaechtnis("Gespräch mit laufendem Programmierauftrag geöffnet", "${gespraech.size} Nachrichten")
    }

    /** Holt das zuletzt geführte Gespräch zurück, wenn es jünger als [NEUES_GESPRAECH_NACH_MS] ist. Klein, deshalb direkt. */
    private fun offenesGespraechWiederherstellen() {
        val p = offenePrefs()
        val id = p.getString("id", null) ?: return
        val zeit = p.getLong("zeit", 0L)
        if (System.currentTimeMillis() - zeit > NEUES_GESPRAECH_NACH_MS) return
        val geladen = de.frank.jarvis.data.Gespraeche.laden(getApplication(), id) ?: return
        gespraech.addAll(geladen.second.map { Nachricht(it.vonMir, it.text, it.art, it.kontext) })
        sitzungId = id
        sitzungErstellt = geladen.first.erstellt
        // Der gespeicherte Zeitpunkt, nicht jetzt: Die Zehn-Minuten-Regel rechnet von der letzten echten Nachricht.
        letzteAktivitaet = zeit
        Logbuch.gedaechtnis("Offenes Gespräch fortgesetzt", "${gespraech.size} Nachrichten")
    }

    /** Ein altes Gespräch öffnen und dort weitermachen. Läuft im bisherigen Gespräch noch eine Antwort, arbeitet sie im Hintergrund weiter. */
    fun sitzungOeffnen(id: String) {
        if (id == sitzungId) return
        viewModelScope.launch {
            // Unter der Sicherung: Eine Antwort, die genau jetzt in diese Datei geschrieben wird, ist dann mit im geladenen Stand.
            val geladen = sicherung.withLock { withContext(Dispatchers.IO) { de.frank.jarvis.data.Gespraeche.laden(getApplication(), id) } }
            if (geladen == null) { meldung = "Dieses Gespräch ließ sich nicht öffnen."; sitzungenLaden(); return@launch }
            vorleser.stopp()
            gespraech.clear()
            gespraech.addAll(geladen.second.map { Nachricht(it.vonMir, it.text, it.art, it.kontext) })
            sitzungId = id
            sitzungErstellt = geladen.first.erstellt
            // Wer ein altes Gespräch öffnet, will dort weitersprechen, auch mit „Hey Jarvis“.
            letzteAktivitaet = System.currentTimeMillis()
            offenesGespraechMerken(id, letzteAktivitaet)
        }
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

    fun kiVerbinden(activity: ComponentActivity) {
        if (kiVerbindet) return
        kiVerbindet = true
        viewModelScope.launch {
            try {
                auth.login(activity) { info -> geraeteCode = info }
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

    fun kopiere(text: String, was: String) {
        val app = getApplication<Application>()
        app.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(was, text))
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
    fun auswertungAbgleich(an: Boolean) { einstellungen.auswertungAbgleich = an }
    fun auswertungDeutung(an: Boolean) { einstellungen.auswertungDeutung = an }

    val naechsteAuswertung: String
        get() = de.frank.jarvis.auswertung.Zeitplan.naechster(getApplication())?.let {
            (if (it.toLocalDate() == java.time.LocalDate.now()) "heute" else "morgen") + " um " + it.toLocalTime().toString().take(5) + " Uhr"
        } ?: "ausgeschaltet"
}

/** So lange nach der letzten Nachricht führt „Hey Jarvis“ das offene Gespräch weiter, danach beginnt ein neues. */
private const val NEUES_GESPRAECH_NACH_MS = 10 * 60_000L

/** Wörter, die vor oder nach dem Ausbefehl nicht zählen („Okay Jarvis, Ende bitte“). */
private val FUELLWORTE = setOf("jarvis", "okay", "ok", "bitte", "hey", "also", "gut", "dann", "ja", "nein", "nee", "so")

/** Ganze Sätze, die den Gesprächsmodus beenden (nach Kleinschreibung, ohne Satzzeichen und Füllwörter). */
private val AUSBEFEHLE = setOf(
    "ende", "stopp", "stop", "schluss", "aus", "fertig", "tschüss", "tschüs", "ciao", "danke", "danke schön", "dankeschön", "vielen dank",
    "das wars", "danke das wars", "das wars danke", "das ist alles", "danke das ist alles", "das reicht", "mehr nicht", "nichts mehr",
    "sprachsitzung beenden", "sprachsitzung beendet", "sitzung beenden", "gespräch beenden", "beenden", "beende", "aufhören", "hör auf",
    "ende der sitzung", "schluss für jetzt", "erstmal nicht", "erst mal nicht", "nichts weiter", "sonst nichts", "danke sonst nichts",
)

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

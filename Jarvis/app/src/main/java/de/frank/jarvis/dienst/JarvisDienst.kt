package de.frank.jarvis.dienst

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import de.frank.jarvis.MainActivity
import de.frank.jarvis.R
import de.frank.jarvis.agent.Agenten
import de.frank.jarvis.agent.Vorgaenge
import de.frank.jarvis.agent.Vorgang
import de.frank.jarvis.faehigkeit.Register
import org.json.JSONObject
import de.frank.jarvis.auswertung.Tagesauswertung
import de.frank.jarvis.gedaechtnis.Waechter
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.auswertung.Zeitplan
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.fahrt.Abfahrt
import de.frank.jarvis.mcp.McpServer
import de.frank.jarvis.tunnel.Tunnel
import de.frank.jarvis.tunnel.TunnelStufe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import android.os.PowerManager
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

/**
 * Hält Jarvis erreichbar: MCP-Server auf dem Handy plus Verbindung zum eigenen Server, als Vordergrunddienst
 * mit sichtbarer Statuszeile. So nimmt Jarvis Aufrufe aus ChatGPT auch bei gesperrtem Bildschirm entgegen.
 */
class JarvisDienst : Service() {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mcp by lazy { McpServer(this) }
    /** Lange Arbeit im Hintergrund (Tagesauswertung) läuft hier, nicht auf dem Hauptfaden. */
    private val arbeit = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** false = Android hat den Vordergrund verweigert; der Dienst beendet sich und nimmt keine Aufträge mehr an. */
    @Volatile private var vordergrund = false
    /** Die neueste Start-ID: stopSelf(id) greift nur mit ihr; ältere IDs ignoriert Android. */
    @Volatile private var letzteStartId = 0

    /**
     * Netzwechsel (WLAN ↔ Mobilfunk) oder Funkloch vorbei: sofort über das neue Netz neu verbinden, statt
     * abzuwarten, bis die alte Verbindung am toten Netz von selbst auffällt.
     */
    private val netz = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = Tunnel.netzGewechselt(network.networkHandle)
        override fun onLost(network: Network) = Tunnel.netzVerloren(network.networkHandle)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        kanalAnlegen(this)
        // Sofort in den Vordergrund: Android gibt dafür nur wenige Sekunden.
        val typ = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        try {
            ServiceCompat.startForeground(this, HINWEIS_ID, hinweis("Jarvis startet …"), typ)
        } catch (fehler: Exception) {
            // Aus dem Hintergrund (etwa nach einem Neustart durch Android) verweigert: nicht ohne Vordergrund weiterlaufen.
            Log.w("JarvisDienst", "Vordergrund aus dem Hintergrund nicht erlaubt", fehler)
            Logbuch.fehler("Dienst: Vordergrund nicht erlaubt", fehler)
            stopSelf()
            return
        }
        vordergrund = true
        laeuft = true
        Protokoll.lade(this)
        Logbuch.starte(this)
        // Offene Vorgänge eines beendeten Prozesses gleich als abgebrochen eintragen, nicht erst beim nächsten Abruf.
        arbeit.launch { runCatching { Vorgaenge.buch(this@JarvisDienst).lade() } }
        Logbuch.system("Dienst gestartet", "Version ${de.frank.jarvis.BuildConfig.VERSION_NAME}")
        // Das Live-Gedächtnis: Der Wächter lädt es aus dem Speicher und hält es danach rund um die Uhr aktuell.
        // Abseits des Hauptfadens: Das Laden liest und parst die ganze Gesamtbild-Datei.
        arbeit.launch { runCatching { Waechter.starte(this@JarvisDienst) }.onFailure { Logbuch.fehler("Dienst: Wächter starten", it) } }
        // Nach einem Update: neu installierte Versionen in die Funktionsübersicht einarbeiten (wartet bei Live-Gespräch oder ohne Netz).
        de.frank.jarvis.faehigkeit.Funktionen.anstossen(this, "nach dem Start")
        // Meldet der Server einen neuen Tagebucheintrag, liest Jarvis ihn sofort. Nur den App-Kontext festhalten, nicht den Dienst.
        val app = applicationContext
        Tunnel.beiSignal = { art -> Waechter.serverSignal(app, art) }
        // Nach jedem Verbinden: Rückfragen der Programmier-Sitzungen mit dem Server abgleichen. Eine Meldung, die über eine
        // halb tote Verbindung ging, ist sonst verloren, und Frank sähe die Frage nie.
        Tunnel.beiVerbunden = {
            arbeit.launch {
                runCatching { de.frank.jarvis.faehigkeit.Register.alle(app).filterIsInstance<de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit>().firstOrNull()?.fragenAbgleichen() }
                    .onFailure { Logbuch.fehler("Rückfragen abgleichen", it) }
            }
        }
        // Ablage: Reste abgebrochener Übertragungen aufräumen, unterbrochene Downloads fortsetzen.
        de.frank.jarvis.ablage.AblageZentrale.beimStart(this)
        // Die erste Meldung des Rückrufs ist nur die Bestandsaufnahme: kein Abriss wegen eines Netzes aus einem früheren Dienstlauf.
        Tunnel.netzVergessen()
        runCatching { getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(netz) }
        bereich.launch {
            var vorher: TunnelStufe? = null
            Tunnel.zustand.collect { zustand ->
                if (zustand.stufe != vorher) { Logbuch.netz("Verbindung: ${zustand.stufe}", zustand.meldung); vorher = zustand.stufe }
                val text = if (zustand.stufe == TunnelStufe.ONLINE) "Bereit für ChatGPT" else zustand.meldung
                // Weggewischt soll weg bleiben: dann nur den Text merken, nicht bei jedem Netzwechsel neu zeigen. Noch sichtbar wird sie aktualisiert.
                val nm = getSystemService(NotificationManager::class.java)
                val weggewischt = de.frank.jarvis.meldung.Meldungen.profil(this@JarvisDienst, de.frank.jarvis.meldung.Meldungsarten.BEREITSCHAFT).minimiert &&
                    runCatching { nm.activeNotifications.none { it.id == HINWEIS_ID } }.getOrDefault(true)
                if (weggewischt) letzterText = text else nm.notify(HINWEIS_ID, hinweis(text))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Vordergrund verweigert (siehe onCreate): Der Dienst beendet sich gerade, nichts mehr anfangen.
        if (!vordergrund) { stopSelf(startId); return START_NOT_STICKY }
        letzteStartId = startId
        val e = Einstellungen.get(this)
        // Bereitschaft aus: Der Dienst läuft nur für den Auftrag (Abfahrt, Agent, Auswertung), ohne Verbindung für ChatGPT,
        // und beendet sich danach selbst (unten im finally).
        if (e.dienstAn) Tunnel.starte(e.serverHost, e.serverToken, e.geheimnis) { rumpf -> mcp.verarbeite(rumpf) }
        val anlass = intent?.getStringExtra(EXTRA_AUSWERTEN)
        val agent = intent?.getStringExtra(EXTRA_AGENT)
        val auftrag = intent?.getStringExtra(EXTRA_AUFTRAG).orEmpty()
        val perMail = intent?.getBooleanExtra(EXTRA_MAIL, false) == true
        val vorlesen = intent?.getBooleanExtra(EXTRA_VORLESEN, false) == true
        val vorgang = intent?.getStringExtra(EXTRA_VORGANG)
        val abfahrt = intent?.getBooleanExtra(EXTRA_ABFAHRT, false) == true
        val lernen = intent?.getBooleanExtra(EXTRA_LERNEN, false) == true
        offen.incrementAndGet()
        // Alles Weitere liest Dateien und den Kalender: nicht auf dem Hauptfaden, sonst ruckelt jedes Öffnen der App.
        arbeit.launch {
            try {
                Tagesauswertung.lade(this@JarvisDienst)
                val auftraege = mutableListOf<Job?>()
                auftraege += when {
                    abfahrt -> pruefeAbfahrt()
                    lernen -> lerneAutomatisch()
                    agent != null -> lasseArbeiten(agent, auftrag, perMail, vorlesen, vorgang)
                    anlass != null -> werteAus(anlass)
                    else -> null
                }
                // Verpasst (Handy war aus, App wurde beendet): nachholen, sobald der Dienst wieder läuft, auch wenn er gerade
                // für etwas anderes (Abfahrt, Agent) gestartet wurde. Die Sperre in der Auswertung verhindert Doppelläufe.
                if (anlass == null && verpasst()) auftraege += werteAus("nachgeholt, der geplante Lauf wurde verpasst")
                Zeitplan.stelle(this@JarvisDienst)
                runCatching { de.frank.jarvis.gedaechtnis.Lernlauf.stelleWecker(this@JarvisDienst) }
                // Die Prüfung stellt ihren nächsten Wecker am Ende selbst.
                if (!abfahrt) Abfahrt.stelle(this@JarvisDienst)
                // Erst wenn die Arbeit fertig ist, darf sich der Dienst bei ausgeschalteter Bereitschaft beenden.
                auftraege.forEach { it?.join() }
            } catch (fehler: Exception) {
                if (fehler is kotlinx.coroutines.CancellationException) throw fehler
                Logbuch.fehler("Dienst: Start-Auftrag", fehler)
            } finally {
                // stopSelf(startId) greift nicht, falls inzwischen ein neuerer Auftrag kam; der beendet den Dienst dann selbst.
                // Mit der neuesten ID: Endet ein älterer Auftrag zuletzt, beendete stopSelf(startId) den Dienst nie. Kommt
                // inzwischen ein neuer Start, hat er eine höhere ID und der Stopp wird ignoriert.
                if (offen.decrementAndGet() == 0 && !Einstellungen.get(this@JarvisDienst).dienstAn) stopSelf(letzteStartId)
            }
        }
        return if (e.dienstAn) START_STICKY else START_NOT_STICKY
    }

    /** Fahrzeit zur Arbeit prüfen und melden, wenn es Zeit zum Losfahren ist. */
    private fun pruefeAbfahrt(): Job =
        arbeit.launch {
            val wach = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:abfahrt")
            runCatching { wach.acquire(90_000L) }
            try {
                Abfahrt.pruefe(this@JarvisDienst)
            } finally {
                runCatching { if (wach.isHeld) wach.release() }
            }
        }

    /** Ein fälliger automatischer Lernlauf (Lern-Wecker, nach der Tagesauswertung); der Dienst wartet, bis er fertig ist. */
    private fun lerneAutomatisch(): Job =
        arbeit.launch {
            runCatching { de.frank.jarvis.gedaechtnis.Lernlauf.automatisch(this@JarvisDienst) }
                .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it; Logbuch.fehler("Lernlauf automatisch", it) }
        }

    private fun verpasst(): Boolean {
        val faellig = Zeitplan.letzterFaelliger(this) ?: return false
        // Die neueste liegt nach lade() schon im Speicher: kein zweites Lesen von der Platte.
        // Eine vorläufige Fassung (Datenbericht beim Abruf) zählt nicht als erledigter Lauf.
        val letzte = Tagesauswertung.neuesteEndgueltige(this)?.zeitpunkt
        // Nur nachholen, wenn der Lauf noch nicht lange her ist; sonst übernimmt der nächste geplante.
        return (letzte == null || letzte.isBefore(faellig)) && faellig.isAfter(LocalDateTime.now().minusHours(6))
    }

    /**
     * Ein Agent arbeitet im Hintergrund und meldet sich mit einer Benachrichtigung. Sein Bericht kommt samt Bildern und PDF in das
     * Jarvis-Gespräch, aus dem er beauftragt wurde (Aufträge aus ChatGPT holen ihn über vorgaenge); in die Ablage nur auf Wunsch.
     */
    private fun lasseArbeiten(name: String, auftrag: String, perMail: Boolean, vorlesen: Boolean, vorgang: String?): Job? {
        // Agent inzwischen gelöscht: Den reservierten Platz sofort zurückgeben, statt ihn zwei Minuten zu blockieren.
        val plan = Agenten.finde(this, name) ?: run {
            Agenten.gibFrei(name, auftrag)
            vorgang?.let { Vorgaenge.buch(this).beende(it, Vorgang.FEHLGESCHLAGEN, null, "Den Agenten $name gibt es nicht mehr") }
            return null
        }
        // Inzwischen abgebrochen (vorgaenge) oder als gescheitert eingetragen: nicht mehr anlaufen lassen.
        if (vorgang != null && Vorgaenge.buch(this).finde(vorgang)?.offen == false) { Agenten.gibFrei(name, auftrag); return null }
        return arbeit.launch {
            val wach = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:agent")
            // Je Zeitfenster gehalten; während der Lauf nach Zeitablauf auf Frank wartet, schläft das Gerät ruhig weiter.
            val fensterMs = (Agenten.ZEITLIMIT_MINUTEN + 5) * 60_000L
            runCatching { wach.acquire(fensterMs) }
            try {
                val lauf = Agenten.fuehreAus(this@JarvisDienst, plan, auftrag, vorgang,
                    beiFrage = { kennung, bericht ->
                        runCatching { if (wach.isHeld) wach.release() }
                        frageNachZeitablauf(plan.name, kennung, vorgang, bericht)
                    },
                    nachEntscheidung = { kennung, weiter ->
                        runCatching { getSystemService(NotificationManager::class.java).cancel(zeitablaufNummer(kennung)) }
                        if (weiter) runCatching { wach.acquire(fensterMs) }
                    },
                )
                val titel = lauf.titel
                val v = vorgang?.let { Vorgaenge.buch(this@JarvisDienst).finde(it) }
                var zusatz = ""
                if (titel != null && perMail) {
                    val senden = Register.werkzeuge(this@JarvisDienst).firstOrNull { it.name == "mail_senden" }
                    val ergebnis = runCatching { senden?.ausfuehren(JSONObject().put("betreff", "Jarvis: $titel").put("ablage_datei", titel)) }.getOrNull()
                    zusatz = if (ergebnis != null && !ergebnis.fehler) " Per E-Mail verschickt." else " E-Mail nicht möglich: ${ergebnis?.text ?: "nicht eingerichtet"}"
                }
                val wo = when {
                    !lauf.vorlaeufig -> "liegt in der Ablage"
                    !v?.gespraech.isNullOrEmpty() -> "steht im Jarvis-Gespräch (nicht in der Ablage gespeichert)"
                    else -> "ist fertig (nicht in der Ablage gespeichert; ChatGPT holt es mit vorgaenge)"
                }
                val text = when {
                    titel != null && lauf.grund != null -> "„$titel“ $wo, aber nicht vollständig: ${lauf.grund}.$zusatz"
                    titel != null -> "„$titel“ $wo.$zusatz"
                    else -> "Der Agent ${plan.name} ist nicht fertig geworden: ${lauf.grund ?: "Grund unbekannt"}. Details unter Aktivität."
                }
                // Ins Gespräch, aus dem der Auftrag kam: Bericht mit Bildern und PDF, oder ehrlich der Grund, warum keiner kam.
                v?.gespraech?.takeIf { it.isNotEmpty() }?.let { g ->
                    val kopf = "**${plan.name} fertig** · Auftrag ${v.id}" + (if (lauf.vorlaeufig) "" else " · in der Ablage als „$titel“") + "\n\n"
                    val inhalt = when {
                        lauf.bericht != null -> kopf + lauf.bericht.trim() + (lauf.grund?.let { "\n\n_Nicht vollständig: ${it}._" } ?: "")
                        else -> "**${plan.name} nicht fertig** · Auftrag ${v.id}\n\n${lauf.grund ?: "Grund unbekannt"}. Details unter Aktivität."
                    }
                    val notiz = if (lauf.bericht == null) "" else if (lauf.vorlaeufig)
                        "(Ergebnis von Hintergrundauftrag ${v.id}, VORLÄUFIG, NICHT in der Ablage. Will Frank es behalten: in_ablage_uebernehmen mit vorgang ${v.id}.)"
                    else "(Ergebnis von Hintergrundauftrag ${v.id}, in der Ablage als „$titel“.)"
                    runCatching { de.frank.jarvis.faehigkeit.ProgrammierChat.poste(this@JarvisDienst, g, inhalt, de.frank.jarvis.faehigkeit.ProgrammierChat.ART_AGENT, notiz, lauf.dateien) }
                        .onSuccess { Vorgaenge.buch(this@JarvisDienst).mitgeteilt(listOf(v.id)) }
                }
                val agentTitel = "Jarvis: ${plan.name} fertig" + (vorgang?.let { " (Vorgang $it)" } ?: "")
                de.frank.jarvis.meldung.Meldungen.zeige(
                    this@JarvisDienst, de.frank.jarvis.meldung.Meldungsarten.AGENT,
                    // Eigener Bereich: trifft keine feste Meldung (1, 4200, 4300, 4301, 4310, 4400–4499, 4711, 7000–7999) und keine der Ablage (ab 200_000).
                    20_000 + (System.currentTimeMillis() % 100_000).toInt(),
                    NotificationCompat.Builder(this@JarvisDienst, KANAL_ERGEBNIS).setSmallIcon(R.drawable.ic_stat_jarvis).setContentTitle(agentTitel)
                        .setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true)
                        .setContentIntent(PendingIntent.getActivity(this@JarvisDienst, 0, Intent(this@JarvisDienst, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)),
                    // Vorgelesen wird unten die eigene Zusammenfassung, nicht der Meldungstext.
                    agentTitel, sprechtext = null,
                )
                // Im Sprachgespräch begonnen: Das Ergebnis kommt vollständig zu Gehör (auch nach Stille-Ende, aus dem Hintergrund, bei
                // gesperrtem Handy), danach ist der passende Sprachdialog wieder offen. Dann keine zweite, kurze Fassung unten.
                val sprachlich = v?.takeIf { it.gespraech.isNotEmpty() }?.let { de.frank.jarvis.speech.SprachModus.aus(it.sprache) }
                // Vor der teuren Vorlesefassung entscheiden; bewusst beendet oder Vorlesen aus: der bisherige Weg (Meldungs-Einstellung, vorlesen=true).
                val sprechen = sprachlich != null && de.frank.jarvis.speech.SprachZustellung.ausgang(this@JarvisDienst, sprachlich, v.erstellt, de.frank.jarvis.meldung.Meldungsarten.AGENT)
                    .also { if (it != de.frank.jarvis.speech.SprachZustellung.Ausgang.SPRECHEN) Logbuch.sprache("Sprachzustellung", "vorgang:${v.id}: ${it.text}") } ==
                    de.frank.jarvis.speech.SprachZustellung.Ausgang.SPRECHEN
                if (v != null && sprachlich != null && sprechen) {
                    val gesprochen = if (lauf.bericht != null)
                        de.frank.jarvis.agent.Vorlesefassung.vollstaendig(this@JarvisDienst, plan.name, lauf.bericht, lauf.grund)
                    else "Der Agent ${plan.name} ist leider nicht fertig geworden: ${lauf.grund ?: "Grund unbekannt"}. Details stehen im Chat."
                    val ansage = if (lauf.bericht != null) "Frank, die Ergebnisse sind jetzt da." else "Frank, zu deinem Auftrag an den Agenten ${plan.name}:"
                    de.frank.jarvis.ui.AppViewModel.zustellen(this@JarvisDienst,
                        de.frank.jarvis.speech.Zustellung("vorgang:${v.id}", sprachlich, v.gespraech, v.erstellt, ansage, gesprochen,
                            ruhe = de.frank.jarvis.meldung.Meldungsarten.AGENT))
                }
                // Nach der Benachrichtigung: Die Zusammenfassung braucht bis zu 75 Sekunden und soll die Meldung nicht aufhalten.
                else if (titel != null && (vorlesen || de.frank.jarvis.meldung.Meldungen.vorlesenAn(this@JarvisDienst, de.frank.jarvis.meldung.Meldungsarten.AGENT))) {
                    lauf.bericht?.let { bericht ->
                        // Vorgelesen wird eine eigene kurze Zusammenfassung fürs Ohr, nicht Auftrag und Bericht; der Bericht bleibt unverändert.
                        val fassung = de.frank.jarvis.agent.Vorlesefassung.erstelle(this@JarvisDienst, plan.name, titel, bericht, lauf.grund, inAblage = !lauf.vorlaeufig)
                        // Mit Stimme und Lautstärke aus Einstellungen → Benachrichtigungen → Agent fertig; ein laufendes Gespräch wartet sie ab,
                        // und ein Abschalten der Meldung in der Zwischenzeit verwirft sie.
                        de.frank.jarvis.meldung.Meldungen.vorlesenWartend(this@JarvisDienst, de.frank.jarvis.meldung.Meldungsarten.AGENT, plan.name, fassung,
                            schluessel = titel, ausdruecklich = vorlesen)
                    }
                }
            } finally {
                runCatching { if (wach.isHeld) wach.release() }
            }
        }.also { job -> vorgang?.let { Agenten.merkeJob(it, job) } }
    }

    /**
     * Zeitlimit eines Agenten erreicht: Statusbericht und Frage „Abbrechen oder weiterarbeiten?“ ins Gespräch, aus dem der Auftrag kam,
     * und als Benachrichtigung mit beiden Knöpfen. Im Gespräch still (Meldungen.zeige): Das Live-Gespräch hat Vorrang.
     */
    private fun frageNachZeitablauf(agent: String, kennung: String, vorgang: String?, bericht: String) {
        val v = vorgang?.let { Vorgaenge.buch(this).finde(it) }
        v?.gespraech?.takeIf { it.isNotEmpty() }?.let { g ->
            runCatching {
                de.frank.jarvis.faehigkeit.ProgrammierChat.poste(this, g, "**$agent: Zeitlimit erreicht** · Auftrag ${v.id}\n\n$bericht",
                    de.frank.jarvis.faehigkeit.ProgrammierChat.ART_AGENT,
                    "(Hintergrundauftrag ${v.id} pausiert nach Ablauf des internen Zeitlimits und wartet auf Franks Entscheidung. Will Frank weiterarbeiten oder weiterrecherchieren: " +
                        "vorgang_fortsetzen mit id ${v.id}. Will er abbrechen: vorgang_abbrechen mit id ${v.id}, dann schreibt der Agent einen Zwischenstand.)")
            }
        }
        val titel = "Jarvis: $agent – Zeitlimit erreicht" + (vorgang?.let { " (Vorgang $it)" } ?: "")
        val kurz = "Das interne Zeitlimit ist erreicht. Abbrechen oder weiterarbeiten?"
        fun knopf(aktion: String, code: Int) = PendingIntent.getBroadcast(this, zeitablaufNummer(kennung) * 2 + code,
            Intent(this, AgentenEntscheidungEmpfaenger::class.java).setAction(aktion).putExtra(AgentenEntscheidungEmpfaenger.EXTRA_KENNUNG, kennung),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        de.frank.jarvis.meldung.Meldungen.zeige(
            this, de.frank.jarvis.meldung.Meldungsarten.AGENT, zeitablaufNummer(kennung),
            NotificationCompat.Builder(this, KANAL_ERGEBNIS).setSmallIcon(R.drawable.ic_stat_jarvis).setContentTitle(titel)
                .setContentText(kurz).setStyle(NotificationCompat.BigTextStyle().bigText(bericht.replace("**", ""))).setAutoCancel(false)
                .addAction(R.drawable.ic_stat_jarvis, "Weiterarbeiten", knopf(AgentenEntscheidungEmpfaenger.AKTION_WEITER, 0))
                .addAction(R.drawable.ic_stat_jarvis, "Abbrechen", knopf(AgentenEntscheidungEmpfaenger.AKTION_ABBRECHEN, 1))
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)),
            titel, sprechtext = null,
        )
    }

    private fun werteAus(anlass: String): Job =
        arbeit.launch {
            // Das Gerät darf währenddessen nicht einschlafen; nach spätestens 12 Minuten gibt das System die Sperre frei (Auswertung und danach das Lernen aus dem Tagebuch).
            val wach = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:tagesauswertung")
            runCatching { wach.acquire(12 * 60_000L) }
            try {
                val beginn = System.currentTimeMillis()
                val fertig = Tagesauswertung.erstelle(this@JarvisDienst, anlass)
                Logbuch.dauer("Tagesauswertung", System.currentTimeMillis() - beginn, "$anlass: " + when { fertig == null -> "lief schon oder scheiterte"; fertig.mitKi -> "mit Deutung"; else -> "nur Datenbericht" })
                // Danach, ohne die Auswertung aufzuhalten: aus dem Tagebuch lernen (jeder Schritt hat sein eigenes Zeitlimit).
                if (fertig?.mitKi == true) runCatching { de.frank.jarvis.agent.Lernen.laufe(this@JarvisDienst) }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it; Logbuch.fehler("Lernen aus dem Tagebuch", it) }
                // Die Auswertung weckt den Dienst ohnehin regelmäßig: Ist ein Lernlauf fällig, läuft er gleich mit (auch ohne Bereitschaft).
                runCatching { de.frank.jarvis.gedaechtnis.Lernlauf.automatisch(this@JarvisDienst) }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it; Logbuch.fehler("Lernlauf nach der Auswertung", it) }
                // Verschobene Pflege der Funktionsübersicht (Live-Gespräch, kein Netz) holt die stündliche Auswertung nach.
                de.frank.jarvis.faehigkeit.Funktionen.anstossen(this@JarvisDienst, "nach der Tagesauswertung")
            } finally {
                runCatching { if (wach.isHeld) wach.release() }
                Zeitplan.stelle(this@JarvisDienst)
            }
        }

    override fun onDestroy() {
        laeuft = false
        Logbuch.system("Dienst beendet")
        Waechter.stoppe(this)
        runCatching { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(netz) }
        Tunnel.beiSignal = null
        Tunnel.beiVerbunden = null
        Tunnel.stoppe()
        bereich.cancel()
        arbeit.cancel()
        super.onDestroy()
    }

    private fun hinweis(text: String): Notification = baueHinweis(this, text)

    companion object {
        /** Läuft der Dienst im Vordergrund, und was steht gerade in seiner Meldung (für [hinweisErneuern]). */
        @Volatile private var laeuft = false
        @Volatile private var letzterText = "Jarvis startet …"

        /**
         * Wegwischbar (Einstellungen → Benachrichtigungen): nicht fest, ab Android 14 lässt sich die Anzeige wegwischen und Jarvis
         * läuft weiter. Ein eigener Kanal mit kleinster Wichtigkeit hülfe nicht: Android hebt ihn für Vordergrund-Dienste an.
         */
        private fun baueHinweis(context: Context, text: String): Notification {
            letzterText = text
            val minimiert = de.frank.jarvis.meldung.Meldungen.profil(context, de.frank.jarvis.meldung.Meldungsarten.BEREITSCHAFT).minimiert
            return NotificationCompat.Builder(context, KANAL)
                .setSmallIcon(R.drawable.ic_stat_jarvis)
                .setContentTitle("Jarvis")
                .setContentText(text)
                .setOngoing(!minimiert)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                .build()
        }

        /** Nach „Minimiert“ an oder aus: die laufende Meldung sofort auf den passenden Kanal umstellen. */
        fun hinweisErneuern(context: Context) {
            if (!laeuft) return
            val app = context.applicationContext
            kanalAnlegen(app)
            runCatching { app.getSystemService(NotificationManager::class.java).notify(HINWEIS_ID, baueHinweis(app, letzterText)) }
        }

        /** Laufende Start-Aufträge: Bei ausgeschalteter Bereitschaft beendet sich der Dienst erst, wenn keiner mehr läuft. */
        private val offen = AtomicInteger(0)
        /** Wie viele Aufträge der Dienst gerade abarbeitet (für [abgleichen]). */
        val arbeitet: Int get() = offen.get()

        private const val KANAL = "jarvis_dienst"
        private const val HINWEIS_ID = 1
        private const val EXTRA_AUSWERTEN = "auswerten"
        private const val EXTRA_AGENT = "agent"
        private const val EXTRA_AUFTRAG = "auftrag"
        private const val EXTRA_MAIL = "mail"
        private const val EXTRA_VORLESEN = "vorlesen"
        private const val EXTRA_VORGANG = "vorgang"
        private const val KANAL_ERGEBNIS = "jarvis_ergebnisse"
        private const val EXTRA_ABFAHRT = "abfahrt"
        private const val EXTRA_LERNEN = "lernen"

        /** Beauftragt den Dienst mit einem fälligen automatischen Lernlauf (vom Lern-Wecker). */
        fun lernen(context: Context) {
            val app = context.applicationContext
            runCatching { ContextCompat.startForegroundService(app, Intent(app, JarvisDienst::class.java).putExtra(EXTRA_LERNEN, true)) }
                .onFailure { Log.w("JarvisDienst", "Lernlauf ließ sich nicht starten", it); Logbuch.fehler("Lernlauf: Dienst ließ sich nicht starten", it) }
        }

        /** Beauftragt den Dienst mit der Prüfung der Fahrzeit zur Arbeit (vom Wecker oder aus der App). */
        fun abfahrtPruefen(context: Context) {
            val app = context.applicationContext
            runCatching { ContextCompat.startForegroundService(app, Intent(app, JarvisDienst::class.java).putExtra(EXTRA_ABFAHRT, true)) }
                .onFailure { Log.w("JarvisDienst", "Abfahrtsprüfung ließ sich nicht starten", it) }
        }

        /** Beauftragt einen Agenten; die Arbeit läuft im Dienst weiter, auch wenn der Aufrufer längst fertig ist. */
        /** false = der Dienst ließ sich nicht starten (dann läuft nichts). */
        fun agentStarten(context: Context, name: String, auftrag: String, perMail: Boolean, vorlesen: Boolean = false, vorgang: String? = null): Boolean {
            val app = context.applicationContext
            return runCatching {
                ContextCompat.startForegroundService(app, Intent(app, JarvisDienst::class.java).putExtra(EXTRA_AGENT, name).putExtra(EXTRA_AUFTRAG, auftrag).putExtra(EXTRA_MAIL, perMail).putExtra(EXTRA_VORLESEN, vorlesen).putExtra(EXTRA_VORGANG, vorgang))
            }.onFailure { Log.w("JarvisDienst", "Agent ließ sich nicht starten", it) }.isSuccess
        }

        /** Beauftragt den Dienst mit einer Tagesauswertung (vom Wecker, aus der App oder aus dem Plugin). */
        fun auswerten(context: Context, anlass: String) {
            val app = context.applicationContext
            runCatching { ContextCompat.startForegroundService(app, Intent(app, JarvisDienst::class.java).putExtra(EXTRA_AUSWERTEN, anlass)) }
                .onFailure { Log.w("JarvisDienst", "Auswertung ließ sich nicht starten", it) }
        }

        fun kanalAnlegen(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(KANAL, "Jarvis-Bereitschaft", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Zeigt, dass Jarvis für ChatGPT erreichbar ist."
                    setShowBadge(false)
                },
            )
            // Der frühere Kanal „minimiert“ (1.8.151) wirkte bei Vordergrund-Diensten nicht.
            runCatching { context.getSystemService(NotificationManager::class.java).deleteNotificationChannel("jarvis_dienst_min") }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(KANAL_ERGEBNIS, "Jarvis-Ergebnisse", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Meldet, wenn ein Agent von Jarvis fertig ist."
                },
            )
        }

        /** Startet den Dienst oder übernimmt geänderte Einstellungen; bei abgeschaltetem Dienst stoppt er. */
        fun abgleichen(context: Context) {
            val app = context.applicationContext
            val absicht = Intent(app, JarvisDienst::class.java)
            if (Einstellungen.get(app).dienstAn) {
                runCatching { ContextCompat.startForegroundService(app, absicht) }
                    .onFailure { Log.w("JarvisDienst", "Start nicht möglich", it) }
            } else if (arbeitet > 0) {
                // Bereitschaft aus, aber der Dienst arbeitet gerade für einen Auftrag (Agent, Auswertung, Abfahrt): nur die
                // Verbindung trennen. Der Dienst beendet sich nach dem Auftrag selbst; stopService bräche ihn mittendrin ab
                // (etwa bei jedem Öffnen der App oder Auf- und Zuklappen).
                Tunnel.stoppe()
                Thread { runCatching { Zeitplan.stelle(app) } }.start()
            } else {
                app.stopService(absicht)
                // Ohne Dienst muss der nächste Auswertungslauf trotzdem geplant sein (der Wecker startet ihn dann selbst).
                // Abseits des Hauptfadens: Das Planen liest den Kalender.
                Thread { runCatching { Zeitplan.stelle(app) } }.start()
            }
        }
    }
}

/** Nach dem Einschalten des Handys und nach einem App-Update ist Jarvis von selbst wieder erreichbar. */
class StartEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val app = context.applicationContext
                if (Einstellungen.get(app).dienstAn) {
                    // Der Dienst plant beim Start Auswertungslauf und Abfahrtsprüfung selbst.
                    JarvisDienst.abgleichen(app)
                } else {
                    // Bereitschaft aus: Ohne Dienst beide Wecker hier planen (sie starten den Dienst dann selbst).
                    // Abseits des Hauptfadens: Das Planen liest den Kalender.
                    val ergebnis = goAsync()
                    Thread {
                        try {
                            runCatching { Zeitplan.stelle(app) }
                            runCatching { Abfahrt.stelle(app) }
                            runCatching { de.frank.jarvis.gedaechtnis.Lernlauf.stelleWecker(app) }
                        } finally {
                            ergebnis.finish()
                        }
                    }.start()
                }
                de.frank.jarvis.weckwort.WeckwortDienst.nachNeustart(context)
            }
            // Uhr oder Zeitzone verstellt: Der gestellte Wecker passt nicht mehr.
            // Abseits des Hauptfadens: Das Planen liest den Kalender.
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                val app = context.applicationContext
                val ergebnis = goAsync()
                Thread {
                    try {
                        runCatching { Zeitplan.stelle(app) }
                        runCatching { Abfahrt.stelle(app) }
                    } finally {
                        ergebnis.finish()
                    }
                }.start()
            }
        }
    }
}

/** Feste Nummer der Zeitablauf-Benachrichtigung je Kennung; eigener Bereich zwischen den Agenten-Ergebnissen (20 000–119 999) und der Ablage (ab 200 000). */
internal fun zeitablaufNummer(kennung: String): Int = 140_000 + (kennung.uppercase(java.util.Locale.ROOT).hashCode() and 0xFFF)

/** Die Knöpfe „Weiterarbeiten“ und „Abbrechen“ der Zeitablauf-Benachrichtigung eines Agenten. */
class AgentenEntscheidungEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kennung = intent.getStringExtra(EXTRA_KENNUNG) ?: return
        val weiter = intent.action == AKTION_WEITER
        context.getSystemService(NotificationManager::class.java)?.cancel(zeitablaufNummer(kennung))
        // Lief der Auftrag inzwischen nicht mehr (schon entschieden, Jarvis neu gestartet), ehrlich sagen statt still nichts zu tun.
        if (!Agenten.entscheide(kennung, weiter))
            android.widget.Toast.makeText(context, "Dieser Auftrag wartet nicht mehr auf eine Entscheidung (Stand unter Aktivität).", android.widget.Toast.LENGTH_LONG).show()
    }

    companion object {
        const val AKTION_WEITER = "de.frank.jarvis.AGENT_WEITER"
        const val AKTION_ABBRECHEN = "de.frank.jarvis.AGENT_ABBRECHEN"
        const val EXTRA_KENNUNG = "kennung"
    }
}

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
        Protokoll.lade(this)
        Logbuch.starte(this)
        // Offene Vorgänge eines beendeten Prozesses gleich als abgebrochen eintragen, nicht erst beim nächsten Abruf.
        arbeit.launch { runCatching { Vorgaenge.buch(this@JarvisDienst).lade() } }
        Logbuch.system("Dienst gestartet", "Version ${de.frank.jarvis.BuildConfig.VERSION_NAME}")
        // Das Live-Gedächtnis: Der Wächter lädt es aus dem Speicher und hält es danach rund um die Uhr aktuell.
        // Abseits des Hauptfadens: Das Laden liest und parst die ganze Gesamtbild-Datei.
        arbeit.launch { runCatching { Waechter.starte(this@JarvisDienst) }.onFailure { Logbuch.fehler("Dienst: Wächter starten", it) } }
        // Meldet der Server einen neuen Tagebucheintrag, liest Jarvis ihn sofort. Nur den App-Kontext festhalten, nicht den Dienst.
        val app = applicationContext
        Tunnel.beiSignal = { art -> Waechter.serverSignal(app, art) }
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
                getSystemService(NotificationManager::class.java).notify(HINWEIS_ID, hinweis(text))
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
        offen.incrementAndGet()
        // Alles Weitere liest Dateien und den Kalender: nicht auf dem Hauptfaden, sonst ruckelt jedes Öffnen der App.
        arbeit.launch {
            try {
                Tagesauswertung.lade(this@JarvisDienst)
                val auftraege = mutableListOf<Job?>()
                auftraege += when {
                    abfahrt -> pruefeAbfahrt()
                    agent != null -> lasseArbeiten(agent, auftrag, perMail, vorlesen, vorgang)
                    anlass != null -> werteAus(anlass)
                    else -> null
                }
                // Verpasst (Handy war aus, App wurde beendet): nachholen, sobald der Dienst wieder läuft, auch wenn er gerade
                // für etwas anderes (Abfahrt, Agent) gestartet wurde. Die Sperre in der Auswertung verhindert Doppelläufe.
                if (anlass == null && verpasst()) auftraege += werteAus("nachgeholt, der geplante Lauf wurde verpasst")
                Zeitplan.stelle(this@JarvisDienst)
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

    private fun verpasst(): Boolean {
        val faellig = Zeitplan.letzterFaelliger(this) ?: return false
        // Die neueste liegt nach lade() schon im Speicher: kein zweites Lesen von der Platte.
        // Eine vorläufige Fassung (Datenbericht beim Abruf) zählt nicht als erledigter Lauf.
        val letzte = Tagesauswertung.neuesteEndgueltige(this)?.zeitpunkt
        // Nur nachholen, wenn der Lauf noch nicht lange her ist; sonst übernimmt der nächste geplante.
        return (letzte == null || letzte.isBefore(faellig)) && faellig.isAfter(LocalDateTime.now().minusHours(6))
    }

    /** Ein Agent arbeitet im Hintergrund, legt sein Ergebnis in die Ablage und meldet sich mit einer Benachrichtigung. */
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
            runCatching { wach.acquire((plan.minuten + 5) * 60_000L) }
            try {
                val lauf = Agenten.fuehreAus(this@JarvisDienst, plan, auftrag, vorgang)
                val titel = lauf.titel
                var zusatz = ""
                if (titel != null && perMail) {
                    val senden = Register.werkzeuge(this@JarvisDienst).firstOrNull { it.name == "mail_senden" }
                    val ergebnis = runCatching { senden?.ausfuehren(JSONObject().put("betreff", "Jarvis: $titel").put("ablage_datei", titel)) }.getOrNull()
                    zusatz = if (ergebnis != null && !ergebnis.fehler) " Per E-Mail verschickt." else " E-Mail nicht möglich: ${ergebnis?.text ?: "nicht eingerichtet"}"
                }
                val text = when {
                    titel != null && lauf.grund != null -> "„$titel“ liegt in der Ablage, aber nicht vollständig: ${lauf.grund}.$zusatz"
                    titel != null -> "„$titel“ liegt in der Ablage.$zusatz"
                    else -> "Der Agent ${plan.name} ist nicht fertig geworden: ${lauf.grund ?: "Grund unbekannt"}. Details unter Aktivität."
                }
                getSystemService(NotificationManager::class.java).notify(
                    // Eigener Bereich: trifft keine feste Meldung (1, 4200, 4300, 4301, 4310, 4711, 7000–7999) und keine der Ablage (ab 200_000).
                    20_000 + (System.currentTimeMillis() % 100_000).toInt(),
                    NotificationCompat.Builder(this@JarvisDienst, KANAL_ERGEBNIS).setSmallIcon(R.drawable.ic_stat_jarvis).setContentTitle("Jarvis: ${plan.name} fertig" + (vorgang?.let { " (Vorgang $it)" } ?: ""))
                        .setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true)
                        .setSilent(de.frank.jarvis.speech.GespraechsFokus.stilleHinweise)
                        .setContentIntent(PendingIntent.getActivity(this@JarvisDienst, 0, Intent(this@JarvisDienst, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                        .build(),
                )
                // Nach der Benachrichtigung: Die Zusammenfassung braucht bis zu 75 Sekunden und soll die Meldung nicht aufhalten.
                if (titel != null && (vorlesen || Einstellungen.get(this@JarvisDienst).ergebnisseVorlesen)) {
                    de.frank.jarvis.faehigkeit.Ablage.finde(this@JarvisDienst, titel)?.let { datei ->
                        // Vorgelesen wird eine eigene kurze Zusammenfassung fürs Ohr, nicht Auftrag und Bericht; der Bericht bleibt unverändert.
                        val fassung = de.frank.jarvis.agent.Vorlesefassung.erstelle(this@JarvisDienst, plan.name, titel, datei.readText(), lauf.grund)
                        // Der Live-Sprachmodus hat Vorrang: Läuft ein Gespräch, wartet die Zusammenfassung, bis es vorbei ist (höchstens 15 Minuten).
                        val frei = kotlinx.coroutines.withTimeoutOrNull(15 * 60_000L) {
                            while (de.frank.jarvis.speech.GespraechsFokus.stilleHinweise) kotlinx.coroutines.delay(2_000L)
                            true
                        }
                        // Eigene Quelle „agent:“: kurzer Tonfokus, kurze Pausen und kein Zusammenstoß mit dem Vorleseknopf der Ablage.
                        if (frei == true) kotlinx.coroutines.withContext(Dispatchers.Main) {
                            de.frank.jarvis.speech.Vorleser.hole(this@JarvisDienst, de.frank.jarvis.data.settings.SecureSettings(this@JarvisDienst)).sprich("agent:" + datei.name, plan.name, fassung)
                        }
                    }
                }
            } finally {
                runCatching { if (wach.isHeld) wach.release() }
            }
        }.also { job -> vorgang?.let { Agenten.merkeJob(it, job) } }
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
            } finally {
                runCatching { if (wach.isHeld) wach.release() }
                Zeitplan.stelle(this@JarvisDienst)
            }
        }

    override fun onDestroy() {
        Logbuch.system("Dienst beendet")
        Waechter.stoppe(this)
        runCatching { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(netz) }
        Tunnel.beiSignal = null
        Tunnel.stoppe()
        bereich.cancel()
        arbeit.cancel()
        super.onDestroy()
    }

    private fun hinweis(text: String): Notification = NotificationCompat.Builder(this, KANAL)
        .setSmallIcon(R.drawable.ic_stat_jarvis)
        .setContentTitle("Jarvis")
        .setContentText(text)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        .build()

    companion object {
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

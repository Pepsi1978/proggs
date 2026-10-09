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
import de.frank.jarvis.faehigkeit.Register
import org.json.JSONObject
import de.frank.jarvis.auswertung.Tagesauswertung
import de.frank.jarvis.auswertung.Zeitplan
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.mcp.McpServer
import de.frank.jarvis.tunnel.Tunnel
import de.frank.jarvis.tunnel.TunnelStufe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import android.os.PowerManager
import java.time.LocalDateTime

/**
 * Hält Jarvis erreichbar: MCP-Server auf dem Handy plus Verbindung zum eigenen Server, als Vordergrunddienst
 * mit sichtbarer Statuszeile. So nimmt Jarvis Aufrufe aus ChatGPT auch bei gesperrtem Bildschirm entgegen.
 */
class JarvisDienst : Service() {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mcp by lazy { McpServer(this) }
    /** Lange Arbeit im Hintergrund (Tagesauswertung) läuft hier, nicht auf dem Hauptfaden. */
    private val arbeit = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Ist das Netz wieder da (WLAN ↔ Mobilfunk, Funkloch vorbei), sofort neu verbinden statt abzuwarten. */
    private val netz = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = Tunnel.jetztVerbinden()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        kanalAnlegen(this)
        // Sofort in den Vordergrund: Android gibt dafür nur wenige Sekunden.
        val typ = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, HINWEIS_ID, hinweis("Jarvis startet …"), typ)
        Protokoll.lade(this)
        // Ablage: Reste abgebrochener Übertragungen aufräumen, unterbrochene Downloads fortsetzen.
        de.frank.jarvis.ablage.AblageZentrale.beimStart(this)
        runCatching { getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(netz) }
        bereich.launch {
            Tunnel.zustand.collect { zustand ->
                val text = if (zustand.stufe == TunnelStufe.ONLINE) "Bereit für ChatGPT" else zustand.meldung
                getSystemService(NotificationManager::class.java).notify(HINWEIS_ID, hinweis(text))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val e = Einstellungen.get(this)
        Tunnel.starte(e.serverHost, e.serverToken, e.geheimnis) { rumpf -> mcp.verarbeite(rumpf) }
        Tagesauswertung.lade(this)
        val anlass = intent?.getStringExtra(EXTRA_AUSWERTEN)
        val agent = intent?.getStringExtra(EXTRA_AGENT)
        when {
            agent != null -> lasseArbeiten(agent, intent.getStringExtra(EXTRA_AUFTRAG).orEmpty(), intent.getBooleanExtra(EXTRA_MAIL, false), intent.getBooleanExtra(EXTRA_VORLESEN, false))
            anlass != null -> werteAus(anlass)
            // Verpasst (Handy war aus, App wurde beendet): nachholen, sobald der Dienst wieder läuft.
            verpasst() -> werteAus("nachgeholt, der geplante Lauf wurde verpasst")
        }
        Zeitplan.stelle(this)
        return START_STICKY
    }

    private fun verpasst(): Boolean {
        val faellig = Zeitplan.letzterFaelliger(this) ?: return false
        val letzte = Tagesauswertung.neueste(this)?.zeitpunkt
        // Nur nachholen, wenn der Lauf noch nicht lange her ist; sonst übernimmt der nächste geplante.
        return (letzte == null || letzte.isBefore(faellig)) && faellig.isAfter(LocalDateTime.now().minusHours(6))
    }

    /** Ein Agent arbeitet im Hintergrund, legt sein Ergebnis in die Ablage und meldet sich mit einer Benachrichtigung. */
    private fun lasseArbeiten(name: String, auftrag: String, perMail: Boolean, vorlesen: Boolean) {
        val plan = Agenten.finde(this, name) ?: return
        arbeit.launch {
            val wach = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:agent")
            runCatching { wach.acquire(15 * 60_000L) }
            try {
                val titel = Agenten.fuehreAus(this@JarvisDienst, plan, auftrag)
                var zusatz = ""
                if (titel != null && perMail) {
                    val senden = Register.werkzeuge(this@JarvisDienst).firstOrNull { it.name == "mail_senden" }
                    val ergebnis = runCatching { senden?.ausfuehren(JSONObject().put("betreff", "Jarvis: $titel").put("ablage_datei", titel)) }.getOrNull()
                    zusatz = if (ergebnis != null && !ergebnis.fehler) " Per E-Mail verschickt." else " E-Mail nicht möglich: ${ergebnis?.text ?: "nicht eingerichtet"}"
                }
                val text = if (titel != null) "„$titel“ liegt in der Ablage.$zusatz" else "Der Agent ${plan.name} ist nicht fertig geworden."
                if (titel != null && (vorlesen || Einstellungen.get(this@JarvisDienst).ergebnisseVorlesen)) {
                    de.frank.jarvis.faehigkeit.Ablage.finde(this@JarvisDienst, titel)?.let { datei ->
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            de.frank.jarvis.speech.Vorleser.hole(this@JarvisDienst, de.frank.jarvis.data.settings.SecureSettings(this@JarvisDienst)).sprich("ablage:" + datei.name, plan.name, datei.readText())
                        }
                    }
                }
                getSystemService(NotificationManager::class.java).notify(
                    (System.currentTimeMillis() % 100_000).toInt() + 10,
                    NotificationCompat.Builder(this@JarvisDienst, KANAL_ERGEBNIS).setSmallIcon(R.drawable.ic_stat_jarvis).setContentTitle("Jarvis: ${plan.name} fertig")
                        .setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true)
                        .setContentIntent(PendingIntent.getActivity(this@JarvisDienst, 0, Intent(this@JarvisDienst, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                        .build(),
                )
            } finally {
                runCatching { if (wach.isHeld) wach.release() }
            }
        }
    }

    private fun werteAus(anlass: String) {
        arbeit.launch {
            // Das Gerät darf währenddessen nicht einschlafen; nach spätestens 8 Minuten gibt das System die Sperre frei.
            val wach = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:tagesauswertung")
            runCatching { wach.acquire(8 * 60_000L) }
            try {
                val fertig = Tagesauswertung.erstelle(this@JarvisDienst, anlass)
                // Danach, ohne die Auswertung aufzuhalten: aus dem Tagebuch lernen (höchstens zweieinhalb Minuten).
                if (fertig?.mitKi == true) runCatching { kotlinx.coroutines.withTimeoutOrNull(150_000L) { de.frank.jarvis.agent.Lernen.laufe(this@JarvisDienst) } }
            } finally {
                runCatching { if (wach.isHeld) wach.release() }
                Zeitplan.stelle(this@JarvisDienst)
            }
        }
    }

    override fun onDestroy() {
        runCatching { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(netz) }
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
        private const val KANAL = "jarvis_dienst"
        private const val HINWEIS_ID = 1
        private const val EXTRA_AUSWERTEN = "auswerten"
        private const val EXTRA_AGENT = "agent"
        private const val EXTRA_AUFTRAG = "auftrag"
        private const val EXTRA_MAIL = "mail"
        private const val EXTRA_VORLESEN = "vorlesen"
        private const val KANAL_ERGEBNIS = "jarvis_ergebnisse"

        /** Beauftragt einen Agenten; die Arbeit läuft im Dienst weiter, auch wenn der Aufrufer längst fertig ist. */
        fun agentStarten(context: Context, name: String, auftrag: String, perMail: Boolean, vorlesen: Boolean = false) {
            val app = context.applicationContext
            runCatching {
                ContextCompat.startForegroundService(app, Intent(app, JarvisDienst::class.java).putExtra(EXTRA_AGENT, name).putExtra(EXTRA_AUFTRAG, auftrag).putExtra(EXTRA_MAIL, perMail).putExtra(EXTRA_VORLESEN, vorlesen))
            }.onFailure { Log.w("JarvisDienst", "Agent ließ sich nicht starten", it) }
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
            } else {
                app.stopService(absicht)
            }
        }
    }
}

/** Nach dem Einschalten des Handys und nach einem App-Update ist Jarvis von selbst wieder erreichbar. */
class StartEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> JarvisDienst.abgleichen(context)
            // Uhr oder Zeitzone verstellt: Der gestellte Wecker passt nicht mehr.
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> Zeitplan.stelle(context)
        }
    }
}

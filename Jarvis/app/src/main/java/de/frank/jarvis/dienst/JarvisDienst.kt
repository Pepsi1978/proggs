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

/**
 * Hält Jarvis erreichbar: MCP-Server auf dem Handy plus Verbindung zum eigenen Server, als Vordergrunddienst
 * mit sichtbarer Statuszeile. So nimmt Jarvis Aufrufe aus ChatGPT auch bei gesperrtem Bildschirm entgegen.
 */
class JarvisDienst : Service() {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mcp by lazy { McpServer(this) }

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
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(netz) }
        Tunnel.stoppe()
        bereich.cancel()
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

        fun kanalAnlegen(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(KANAL, "Jarvis-Bereitschaft", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Zeigt, dass Jarvis für ChatGPT erreichbar ist."
                    setShowBadge(false)
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
        }
    }
}

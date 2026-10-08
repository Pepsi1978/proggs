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
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import de.frank.jarvis.MainActivity
import de.frank.jarvis.R
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.mcp.HttpServer
import de.frank.jarvis.tunnel.Tunnel
import de.frank.jarvis.tunnel.TunnelStufe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Hält Jarvis erreichbar: MCP-Server auf dem Handy plus Tunnel nach außen, als Vordergrunddienst mit
 * sichtbarer Statuszeile. So nimmt Jarvis Aufrufe aus ChatGPT auch bei gesperrtem Bildschirm entgegen.
 */
class JarvisDienst : Service() {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var server: HttpServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        kanalAnlegen(this)
        // Sofort in den Vordergrund: Android gibt dafür nur wenige Sekunden.
        ServiceCompat.startForeground(this, HINWEIS_ID, hinweis("Jarvis startet …"), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        Protokoll.lade(this)
        starteServer()
        // Früh laden: Passt die Tunnel-Bibliothek nicht zum Gerät, steht das sofort im Log und nicht erst beim Verbinden.
        runCatching { com.ngrok.NgrokStart.lade() }
            .onSuccess { Log.i("JarvisDienst", "Tunnel-Bibliothek geladen") }
            .onFailure { Log.e("JarvisDienst", "Tunnel-Bibliothek lädt nicht", it) }
        bereich.launch {
            Tunnel.zustand.map { it.stufe to it.meldung }.distinctUntilChanged().collect { (stufe, meldung) ->
                val text = when (stufe) {
                    TunnelStufe.ONLINE -> "Bereit für ChatGPT"
                    TunnelStufe.KEIN_TOKEN -> "Tunnel noch nicht eingerichtet"
                    else -> meldung
                }
                getSystemService(NotificationManager::class.java).notify(HINWEIS_ID, hinweis(text))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val e = Einstellungen.get(this)
        if (server == null) starteServer()
        Tunnel.starte(e.tunnelToken, e.tunnelDomain, Einstellungen.PORT)
        return START_STICKY
    }

    private fun starteServer() {
        server = runCatching { HttpServer(this, Einstellungen.PORT).apply { start(30_000, false) } }
            .onFailure { Log.e("JarvisDienst", "MCP-Server startet nicht", it) }
            .getOrNull()
    }

    override fun onDestroy() {
        Tunnel.stoppe()
        runCatching { server?.stop() }
        server = null
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

        /** Startet den Dienst oder übernimmt geänderte Tunnel-Einstellungen; bei abgeschaltetem Dienst stoppt er. */
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

package de.frank.newskompass.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import de.frank.newskompass.MainActivity
import de.frank.newskompass.NewsApplication
import de.frank.newskompass.observability.KompassLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Hält das Vorlesen am Leben, wenn die App im Hintergrund ist (Home-Taste, andere App, Bildschirm aus).
 *
 * Ohne Vordergrund-Dienst friert Android die App kurz nach dem Verlassen ein: Der laufende Absatz klingt
 * noch aus, die nächsten werden aber nicht mehr geholt. Der Dienst läuft, solange der [VorleseManager]
 * nicht AUS ist (auch pausiert), zeigt den Stand in einer stillen Benachrichtigung mit „Pause/Weiter“ und
 * „Stopp“ und beendet sich selbst, sobald das Vorlesen endet. Ein Teil-Wecksperre hält die CPU wach, damit
 * auch bei ausgeschaltetem Bildschirm die nächsten Absätze rechtzeitig aus dem Netz kommen.
 */
class VorleseDienst : Service() {

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wach: PowerManager.WakeLock? = null
    private var beobachtet = false
    private val vorleser get() = (application as NewsApplication).vorleser

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        legeKanalAn(this)
        wach = getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:vorlesen")
            ?.apply { setReferenceCounted(false) }
        wachHalten(true)
    }

    /** Wach nur, solange gelesen oder geladen wird; pausiert darf das Handy schlafen. */
    private fun wachHalten(an: Boolean) {
        val sperre = wach ?: return
        if (an && !sperre.isHeld) sperre.acquire(WACH_MAX_MS)
        if (!an && sperre.isHeld) sperre.release()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Jeder startForegroundService() verlangt startForeground(), sonst beendet Android die App.
        val gestartet = try {
            val typ = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
            ServiceCompat.startForeground(this, NID, benachrichtigung(vorleser.zustand.value), typ)
            true
        } catch (fehler: Exception) {
            KompassLog.warn("VorleseDienst", "start", "Vordergrund nicht erlaubt", mapOf("grund" to fehler.message))
            false
        }
        if (!gestartet) {
            beende()
            return START_NOT_STICKY
        }
        // Erst nach startForeground beobachten: Ein Beenden davor wertet Android als Absturz.
        if (!beobachtet) {
            beobachtet = true
            bereich.launch {
                vorleser.zustand.collect { z ->
                    if (z.stufe == VorleseStufe.AUS) {
                        beende()
                    } else {
                        wachHalten(z.stufe != VorleseStufe.PAUSIERT)
                        runCatching { NotificationManagerCompat.from(this@VorleseDienst).notify(NID, benachrichtigung(z)) }
                    }
                }
            }
        }
        when (intent?.action) {
            AKTION_UMSCHALTEN -> {
                if (vorleser.zustand.value.stufe == VorleseStufe.PAUSIERT) vorleser.fortsetzen() else vorleser.pausiere()
            }
            AKTION_STOPP -> vorleser.stoppe()
        }
        if (vorleser.zustand.value.stufe == VorleseStufe.AUS) beende()
        return START_NOT_STICKY
    }

    private fun benachrichtigung(z: VorleseZustand): Notification {
        val pausiert = z.stufe == VorleseStufe.PAUSIERT
        val stand = if (z.absatzAnzahl > 0 && z.absatzNummer > 0) "Absatz ${z.absatzNummer} von ${z.absatzAnzahl}" else "Wird vorbereitet …"
        val oeffnen = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, KANAL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(if (pausiert) "Vorlesen pausiert" else "NewsKompass liest vor")
            .setContentText(stand)
            .setContentIntent(oeffnen)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                if (pausiert) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (pausiert) "Weiter" else "Pause",
                aktion(AKTION_UMSCHALTEN, 1),
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stopp", aktion(AKTION_STOPP, 2))
            .build()
    }

    private fun aktion(aktion: String, code: Int): PendingIntent = PendingIntent.getService(
        this, code, Intent(this, VorleseDienst::class.java).setAction(aktion),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun beende() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        runCatching { NotificationManagerCompat.from(this).cancel(NID) }
        wach?.let { if (it.isHeld) it.release() }
        wach = null
        stopSelf()
    }

    override fun onDestroy() {
        wach?.let { if (it.isHeld) it.release() }
        wach = null
        bereich.cancel()
        super.onDestroy()
    }

    companion object {
        private const val KANAL = "vorlesen_v1"
        private const val NID = 4_711
        private const val AKTION_UMSCHALTEN = "de.frank.newskompass.VORLESEN_UMSCHALTEN"
        private const val AKTION_STOPP = "de.frank.newskompass.VORLESEN_STOPP"
        /** Obergrenze für die Wecksperre; ein langer Nachrichtenblock dauert deutlich kürzer. */
        private const val WACH_MAX_MS = 3 * 60 * 60_000L

        private fun legeKanalAn(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(KANAL) != null) return
            nm.createNotificationChannel(
                NotificationChannel(KANAL, "Vorlesen", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Zeigt das laufende Vorlesen mit Pause und Stopp"
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                },
            )
        }

        /** Startet den Dienst, damit das Vorlesen im Hintergrund weiterläuft. Scheitert still, wenn Android es nicht erlaubt. */
        fun starte(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, VorleseDienst::class.java))
            } catch (fehler: Exception) {
                KompassLog.warn("VorleseDienst", "starte", "Dienst nicht gestartet", mapOf("grund" to fehler.message))
            }
        }
    }
}

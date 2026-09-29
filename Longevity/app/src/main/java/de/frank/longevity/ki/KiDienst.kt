package de.frank.longevity.ki

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
import androidx.core.app.ServiceCompat
import de.frank.longevity.MainActivity
import de.frank.longevity.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Vordergrund-Dienst für lange KI-Arbeiten, mit Fortschritt in der Benachrichtigung. */
class KiDienst : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var laeuft = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val typ = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        ServiceCompat.startForeground(this, ID_LAUFEND, meldung(laufend = true), typ)
        if (laeuft) return START_NOT_STICKY
        val auftrag = KiArbeit.auftrag
        if (auftrag == null) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        laeuft = true
        val sperre = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "longevity:ki").apply { acquire(30 * 60_000L) }
        val fortschritt = Fortschritt()
        val job = scope.launch {
            try {
                auftrag(fortschritt)
                fortschritt.fertig()
            } catch (c: CancellationException) {
                KiArbeit.fehler = "Abgebrochen."
            } catch (e: Throwable) {
                KiArbeit.fehler = e.message ?: "Die KI-Arbeit ist fehlgeschlagen."
            } finally {
                KiArbeit.beendet()
                if (sperre.isHeld) sperre.release()
                getSystemService(NotificationManager::class.java).notify(ID_FERTIG, meldung(laufend = false))
                ServiceCompat.stopForeground(this@KiDienst, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        KiArbeit.abbruch = { job.cancel() }
        scope.launch {
            while (isActive && KiArbeit.laeuft) {
                fortschritt.tick()
                getSystemService(NotificationManager::class.java).notify(ID_LAUFEND, meldung(laufend = true))
                delay(1000)
            }
        }
        return START_NOT_STICKY
    }

    private fun meldung(laufend: Boolean): Notification {
        val oeffnen = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val titel = KiArbeit.art?.anzeige ?: "KI"
        val b = NotificationCompat.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ic_stat_longevity)
            .setContentIntent(oeffnen)
            .setOnlyAlertOnce(true)
        return if (laufend) {
            val p = (KiArbeit.prozent * 100).toInt()
            b.setContentTitle("$titel läuft · $p %").setContentText(KiArbeit.schritt).setProgress(100, p, false).setOngoing(true).build()
        } else {
            val text = KiArbeit.fehler ?: KiArbeit.ergebnis ?: "Fertig."
            b.setContentTitle(if (KiArbeit.fehler == null) "$titel fertig" else "$titel nicht fertig").setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true).build()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val KANAL = "ki_arbeit"
        private const val ID_LAUFEND = 41
        private const val ID_FERTIG = 42

        fun kanalAnlegen(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(KANAL, "KI-Arbeiten", NotificationManager.IMPORTANCE_LOW))
        }
    }
}

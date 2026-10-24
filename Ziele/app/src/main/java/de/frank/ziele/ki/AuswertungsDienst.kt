package de.frank.ziele.ki

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import de.frank.ziele.MainActivity
import de.frank.ziele.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Hält den Prozess wach, solange eine Auswertung läuft: Vordergrund-Dienst mit laufender Benachrichtigung plus
 * Wach-Sperre. Ohne ihn friert Android (vor allem Samsung) den Prozess ein oder beendet ihn, sobald Frank die App
 * minimiert, und die halb fertige Auswertung geht verloren. Beendet sich selbst, wenn kein Lauf mehr aktiv ist.
 */
class AuswertungsDienst : Service() {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var beobachter: Job? = null
    private var wach: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    /** Letzter Start: stopSelf(startId) verwirft keinen Start, der gerade noch unterwegs ist (sonst stürzt Android die App ab). */
    private var letzterStart = 0
    private var beendet = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        letzterStart = startId
        beendet = false
        val meldung = laufend(this, Auswertung.stand.value.values.firstOrNull() ?: "Bereite vor …", Auswertung.stand.value.size)
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID_LAUFEND, meldung, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else startForeground(ID_LAUFEND, meldung)
        } catch (e: Exception) {
            Log.w("ZieleDienst", "Vordergrund nicht erlaubt", e)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        // Bei jedem Start neu: Die Zeitgrenze beginnt von vorn, falls der Dienst über mehrere Läufe hinweg lebt.
        (wach ?: getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Ziele:Auswertung")
            .apply { setReferenceCounted(false) }.also { wach = it })
            .acquire(2 * 60 * 60 * 1000L)
        if (beobachter == null) beobachter = bereich.launch {
            Auswertung.stand.collect { laeufe ->
                if (beendet) return@collect
                if (laeufe.isEmpty()) beenden(letzterStart)
                else getSystemService(NotificationManager::class.java).notify(ID_LAUFEND, laufend(this@AuswertungsDienst, laeufe.values.first(), laeufe.size))
            }
        }
        return START_NOT_STICKY
    }

    private fun beenden(startId: Int) {
        beendet = true
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    /** Android 15+: Zeitlimit für Datenabgleich erreicht, sauber beenden (die Läufe rechnen trotzdem weiter, solange der Prozess lebt). */
    override fun onTimeout(startId: Int, fgsType: Int) = beenden(startId)

    override fun onDestroy() {
        bereich.cancel()
        getSystemService(NotificationManager::class.java).cancel(ID_LAUFEND)
        wach?.takeIf { it.isHeld }?.release()
        wach = null
        super.onDestroy()
    }

    companion object {
        private const val KANAL_LAUFEND = "auswertung_laeuft"
        private const val KANAL_FERTIG = "auswertung_fertig"
        private const val ID_LAUFEND = 4711

        /** Startet den Dienst; aus dem Hintergrund (z. B. über die Jarvis-Brücke) kann Android das verbieten, dann läuft es wie bisher. */
        fun starte(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, AuswertungsDienst::class.java)) }
                .onFailure { Log.w("ZieleDienst", "Dienst nicht gestartet", it) }
        }

        @Volatile private var kanaeleDa = false

        private fun kanaele(context: Context) {
            if (kanaeleDa) return
            kanaeleDa = true
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(KANAL_LAUFEND, "Laufende Auswertung", NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel(KANAL_FERTIG, "Auswertung fertig", NotificationManager.IMPORTANCE_DEFAULT))
        }

        private fun oeffnen(context: Context): PendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        private fun laufend(context: Context, schritt: String, anzahl: Int): Notification {
            kanaele(context)
            return NotificationCompat.Builder(context, KANAL_LAUFEND)
                .setSmallIcon(R.drawable.ic_stat_ziel)
                .setContentTitle(if (anzahl > 1) "$anzahl Auswertungen laufen" else "Auswertung läuft")
                .setContentText(schritt)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(0, 0, true)
                .setContentIntent(oeffnen(context))
                .build()
        }

        /** Meldet das Ergebnis, wenn Frank gerade nicht in der App ist (in der App erscheint es als Hinweis). */
        fun fertig(context: Context, zielId: Long, titel: String, text: String, fehler: Boolean) {
            if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            kanaele(context)
            val n = NotificationCompat.Builder(context, KANAL_FERTIG)
                .setSmallIcon(R.drawable.ic_stat_ziel)
                .setContentTitle(if (fehler) "„$titel“: Auswertung fehlgeschlagen" else "„$titel“")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(oeffnen(context))
                .build()
            context.getSystemService(NotificationManager::class.java).notify(10_000 + zielId.toInt(), n)
        }
    }
}

package de.frank.stacklabor.vorlesen

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
import android.util.Log
import androidx.core.app.NotificationCompat
import de.frank.stacklabor.R

/**
 * Haelt das Vorlesen am Leben, wenn Frank den Bildschirm verlaesst (F-16).
 *
 * Der Dienst spielt selbst nichts ab — das bleibt in [VorleseSteuerung]. Er ist die Huelle, die
 * Android davon abhaelt, die vom Nutzer gestartete Wiedergabe im Hintergrund abzuwuergen, und er
 * traegt den Stopp-Knopf in der Benachrichtigung.
 *
 * Im Manifest eingetragen als `de.frank.stacklabor.vorlesen.VorleseDienst` mit
 * `foregroundServiceType="mediaPlayback"`.
 */
class VorleseDienst : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        legeKanalAn()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == AKTION_STOPP) {
            VorleseSteuerung.stoppeLaufendes()
            stopSelf()
            return START_NOT_STICKY
        }
        val meldung = baueMeldung()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                MELDUNGS_NUMMER,
                meldung,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(MELDUNGS_NUMMER, meldung)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun legeKanalAn() {
        val verwaltung = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (verwaltung.getNotificationChannel(KANAL) != null) return
        val kanal = NotificationChannel(
            KANAL,
            getString(R.string.vorlesen_kanal),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }
        verwaltung.createNotificationChannel(kanal)
    }

    private fun baueMeldung(): Notification {
        // Kein direkter Bezug auf die Startseite: Der Startintent der App fuehrt immer dorthin,
        // egal wie die Oberflaeche spaeter heisst.
        val oeffnen = packageManager.getLaunchIntentForPackage(packageName)?.let { start ->
            PendingIntent.getActivity(
                this,
                0,
                start,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        val stoppen = PendingIntent.getService(
            this,
            1,
            Intent(this, VorleseDienst::class.java).setAction(AKTION_STOPP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, KANAL)
            .setContentTitle(getString(R.string.vorlesen_laeuft))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(oeffnen)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.vorlesen_stopp),
                stoppen,
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .build()
    }

    companion object {
        private const val KANAL = "vorlesen"
        private const val MELDUNGS_NUMMER = 4721
        private const val AKTION_STOPP = "de.frank.stacklabor.vorlesen.STOPP"
        private const val MARKE = "VorleseDienst"

        /** Startet die Huelle. Scheitert der Start, laeuft das Vorlesen trotzdem weiter. */
        fun starte(context: Context) {
            val absicht = Intent(context, VorleseDienst::class.java)
            try {
                context.startForegroundService(absicht)
            } catch (fehler: Exception) {
                Log.w(MARKE, "Vordergrunddienst liess sich nicht starten: ${fehler.message}")
            }
        }

        fun stoppe(context: Context) {
            try {
                context.stopService(Intent(context, VorleseDienst::class.java))
            } catch (fehler: Exception) {
                Log.w(MARKE, "Vordergrunddienst liess sich nicht stoppen: ${fehler.message}")
            }
        }
    }
}

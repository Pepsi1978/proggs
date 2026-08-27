package de.frank.gedankenspeicher.hintergrund

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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import de.frank.gedankenspeicher.MainActivity
import de.frank.gedankenspeicher.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Der Abbruch-Knopf der Auswertungs-Benachrichtigung. */
object AuswertungsFernbedienung {
    private val _abbruch = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val abbruch: SharedFlow<Unit> = _abbruch.asSharedFlow()

    fun forderAbbruchAn() {
        _abbruch.tryEmit(Unit)
    }
}

/**
 * Hält die laufende KI-Auswertung am Leben, während Frank die App verlässt.
 *
 * Die Auswertung hängt am ViewModel und nicht am Bildschirm — formal lief sie also schon
 * immer weiter. In der Praxis nicht: Android friert den Prozess einer App, die niemand mehr
 * sieht, nach kurzer Zeit ein und schneidet ihr die offenen Verbindungen ab. Eine gründliche
 * Auswertung braucht aber Minuten, und genau die verlor sie damit — man kam zurück und es
 * war nichts da.
 *
 * Ein Vordergrunddienst vom Typ `dataSync` ist der Weg, den Android dafür vorsieht: eine
 * begrenzte, sichtbare Arbeit, die zu Ende laufen darf. Die Benachrichtigung verschwindet
 * von selbst, sobald die Antwort im Verlauf steht.
 */
class AuswertungsDienst : Service() {

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(KANAL, "Auswertung", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Zeigt an, dass der Gedankenspeicher gerade auswertet."
                setShowBadge(false)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_ABBRUCH) {
            AuswertungsFernbedienung.forderAbbruchAn()
            stoppeDienst()
            return START_NOT_STICKY
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    BENACHRICHTIGUNG_ID,
                    baueBenachrichtigung(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(BENACHRICHTIGUNG_ID, baueBenachrichtigung())
            }
        }
        return START_NOT_STICKY
    }

    private fun baueBenachrichtigung(): Notification {
        val oeffnen = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val abbrechen = PendingIntent.getService(
            this,
            1,
            Intent(this, AuswertungsDienst::class.java).setAction(ACTION_ABBRUCH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ic_auswertung)
            .setContentTitle("Der Gedankenspeicher wertet aus")
            .setContentText("Das dauert einen Moment — du kannst weiterarbeiten.")
            .setProgress(0, 0, true)
            .setContentIntent(oeffnen)
            .addAction(R.drawable.ic_auswertung, "Abbrechen", abbrechen)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun stoppeDienst() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val KANAL = "auswertung"
        private const val BENACHRICHTIGUNG_ID = 7303
        private const val ACTION_ABBRUCH = "de.frank.gedankenspeicher.AUSWERTUNG_ABBRUCH"

        /** Wird beim Druck auf „Auswerten" gerufen — also aus dem Vordergrund heraus. */
        fun starte(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context.applicationContext, AuswertungsDienst::class.java),
                )
            }
        }

        fun beende(context: Context) {
            runCatching {
                context.applicationContext.stopService(
                    Intent(context.applicationContext, AuswertungsDienst::class.java),
                )
            }
        }
    }
}

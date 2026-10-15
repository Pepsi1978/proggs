package de.frank.jarvis.audio

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
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import de.frank.jarvis.MainActivity
import de.frank.jarvis.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Hält die von Hand gestartete Aufnahme am Leben, während Frank in eine andere App wechselt.
 *
 * Ohne Vordergrunddienst vom Typ Mikrofon liefert Android einer App im Hintergrund nur Stille. Der Dienst läuft genau so
 * lange wie die Aufnahme: [starten] beim Tipp auf das Mikrofon (Jarvis ist da sichtbar, nur dann darf er starten),
 * [stoppen], sobald die Aufnahme endet. Er stoppt sich selbst erst, nachdem er [startForeground] aufgerufen hat, sonst
 * bricht Android die App ab.
 */
class AufnahmeDienst : Service() {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var gestartet = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val typ = if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
            ServiceCompat.startForeground(this, HINWEIS_ID, hinweis(), typ)
        } catch (fehler: Exception) {
            Log.w(TAG, "Aufnahme-Dienst nicht erlaubt", fehler)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!gestartet) {
            gestartet = true
            bereich.launch {
                aktiv.first { !it }
                ServiceCompat.stopForeground(this@AufnahmeDienst, ServiceCompat.STOP_FOREGROUND_REMOVE)
                gestartet = false
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun hinweis(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(KANAL, "Aufnahme", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Zeigt, dass Jarvis gerade zuhört, auch wenn du in einer anderen App bist."
                setShowBadge(false)
            },
        )
        val oeffnen = PendingIntent.getActivity(
            this, 33, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ic_stat_jarvis)
            .setContentTitle("Jarvis hört zu")
            .setContentText("Zurück zu Jarvis und auf das Mikrofon tippen, um zu senden.")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(oeffnen)
            .build()
    }

    override fun onDestroy() {
        bereich.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Aufnahme"
        private const val KANAL = "jarvis_aufnahme"
        private const val HINWEIS_ID = 4310

        /** Läuft gerade eine von Hand gestartete Aufnahme? Der Dienst beendet sich, sobald das false wird. */
        private val aktiv = MutableStateFlow(false)

        /** Nur aufrufen, während Jarvis sichtbar ist. */
        fun starten(context: Context) {
            aktiv.value = true
            val app = context.applicationContext
            runCatching { ContextCompat.startForegroundService(app, Intent(app, AufnahmeDienst::class.java)) }
                .onFailure { Log.w(TAG, "Start nicht möglich", it) }
        }

        fun stoppen() {
            aktiv.value = false
        }
    }
}

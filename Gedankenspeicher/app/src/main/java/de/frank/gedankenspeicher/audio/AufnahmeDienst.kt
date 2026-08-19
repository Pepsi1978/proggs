package de.frank.gedankenspeicher.audio

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

/**
 * Die Fernbedienung der laufenden Aufnahme.
 *
 * Der Dienst kennt das ViewModel nicht und darf es auch nicht kennen — beide leben im selben
 * Prozess, aber an verschiedenen Lebensläufen. Ein Stopp aus der Benachrichtigung wird deshalb
 * hier eingeworfen und vom ViewModel aufgesammelt, wo die Aufnahme tatsächlich zu Hause ist.
 */
object AufnahmeFernbedienung {
    private val _stopp = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val stopp: SharedFlow<Unit> = _stopp.asSharedFlow()

    fun forderStoppAn() {
        _stopp.tryEmit(Unit)
    }
}

/**
 * Hält das Mikrofon am Leben, während Frank die App verlässt.
 *
 * Ohne einen Vordergrunddienst nimmt Android einer App im Hintergrund das Mikrofon weg: die
 * Aufnahme liefe formal weiter, käme aber als Stille zurück. Der Dienst mit dem Typ
 * `microphone` ist der einzige Weg, weiter aufnehmen zu dürfen, während oben etwas anderes
 * auf dem Bildschirm steht — und die dauerhafte Benachrichtigung ist der Preis dafür, den
 * Android bewusst verlangt: es soll niemandem verborgen bleiben, dass mitgehört wird.
 */
class AufnahmeDienst : Service() {

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(KANAL, "Sprachaufnahme", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Zeigt an, dass der Gedankenspeicher gerade zuhört."
                setShowBadge(false)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOPP) {
            AufnahmeFernbedienung.forderStoppAn()
            stoppeDienst()
            return START_NOT_STICKY
        }
        val typ = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(BENACHRICHTIGUNG_ID, baueBenachrichtigung(), typ)
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
        val beenden = PendingIntent.getService(
            this,
            1,
            Intent(this, AufnahmeDienst::class.java).setAction(ACTION_STOPP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ic_mikrofon)
            .setContentTitle("Der Gedankenspeicher hört zu")
            .setContentText("Sprich weiter — zum Beenden hier tippen.")
            .setContentIntent(oeffnen)
            .addAction(R.drawable.ic_mikrofon, "Beenden", beenden)
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
        private const val KANAL = "aufnahme"
        private const val BENACHRICHTIGUNG_ID = 7301
        private const val ACTION_STOPP = "de.frank.gedankenspeicher.AUFNAHME_STOPP"

        /**
         * Startet den Dienst. Schlägt der Start fehl — auf Android 14+ etwa, wenn die App
         * gerade nicht vorn ist —, läuft die Aufnahme trotzdem an; sie überlebt dann nur den
         * Wechsel in eine andere App nicht.
         */
        fun starte(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context.applicationContext, AufnahmeDienst::class.java),
                )
            }
        }

        fun beende(context: Context) {
            runCatching {
                context.applicationContext.stopService(
                    Intent(context.applicationContext, AufnahmeDienst::class.java),
                )
            }
        }
    }
}

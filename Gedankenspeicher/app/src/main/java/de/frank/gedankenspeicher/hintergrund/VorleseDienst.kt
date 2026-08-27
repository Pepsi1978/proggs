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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Die Fernbedienung des laufenden Vorlesens.
 *
 * Wie bei der Aufnahme kennt der Dienst den [de.frank.gedankenspeicher.tts.Vorleser] nicht.
 * Ein Druck in der Benachrichtigung wird hier eingeworfen und vom ViewModel aufgesammelt,
 * wo das Vorlesen zu Hause ist.
 */
object VorleseFernbedienung {

    enum class Befehl { UMSCHALTEN, STOPP }

    private val _befehle = MutableSharedFlow<Befehl>(extraBufferCapacity = 4)
    val befehle: SharedFlow<Befehl> = _befehle.asSharedFlow()

    fun sende(befehl: Befehl) {
        _befehle.tryEmit(befehl)
    }
}

/**
 * Was in der Benachrichtigung steht, während vorgelesen wird.
 *
 * Bewusst ein Fluss und **kein** weiterer `startForegroundService`-Aufruf: Android 14+
 * verbietet das Starten eines Vordergrunddienstes aus dem Hintergrund, und genau dort
 * wechselt der Pause-Zustand ja — nämlich wenn Frank in einer anderen App auf „Anhalten"
 * drückt. Der Dienst lauscht deshalb selbst und zeichnet seine Benachrichtigung neu.
 */
object VorleseAnzeige {
    val pausiert = MutableStateFlow(false)
}

/**
 * Hält das Vorlesen am Leben, während Frank die App verlässt.
 *
 * Vorher hörte die Sprachausgabe in `onPause` auf — wer während einer langen Auswertung
 * etwas nachschlagen ging, verlor den Faden mitten im Satz. Android lässt eine App im
 * Hintergrund aber nur dann dauerhaft Ton ausgeben und Netz benutzen, wenn ein
 * Vordergrunddienst läuft: ohne ihn friert das System den Prozess nach kurzer Zeit ein,
 * die nächste Absatz-Synthese kommt nicht mehr durch und es wird still.
 *
 * Der Typ `mediaPlayback` ist der passende — es ist Wiedergabe, keine Datensicherung —,
 * und die Benachrichtigung ist zugleich die Fernbedienung: anhalten, fortsetzen, beenden,
 * ohne in die App zurückzumüssen.
 */
class VorleseDienst : Service() {

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(KANAL, "Vorlesen", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Zeigt an, dass der Gedankenspeicher gerade vorliest."
                setShowBadge(false)
            },
        )
        // Der Pause-Zustand kommt als Fluss herein, nicht als neuer Dienststart (s.o.).
        bereich.launch {
            VorleseAnzeige.pausiert.drop(1).collect { pausiert ->
                runCatching {
                    getSystemService(NotificationManager::class.java)
                        ?.notify(BENACHRICHTIGUNG_ID, baueBenachrichtigung(pausiert))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOPP -> {
                VorleseFernbedienung.sende(VorleseFernbedienung.Befehl.STOPP)
                stoppeDienst()
                return START_NOT_STICKY
            }

            ACTION_UMSCHALTEN -> {
                VorleseFernbedienung.sende(VorleseFernbedienung.Befehl.UMSCHALTEN)
                // Die Anzeige zieht über [VorleseAnzeige] nach, sobald der Vorleser
                // wirklich angehalten hat — nicht schon beim Druck.
                return START_NOT_STICKY
            }
        }
        runCatching {
            val benachrichtigung = baueBenachrichtigung(VorleseAnzeige.pausiert.value)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    BENACHRICHTIGUNG_ID,
                    benachrichtigung,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(BENACHRICHTIGUNG_ID, benachrichtigung)
            }
        }
        return START_NOT_STICKY
    }

    private fun baueBenachrichtigung(pausiert: Boolean): Notification {
        val oeffnen = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val umschalten = PendingIntent.getService(
            this,
            1,
            Intent(this, VorleseDienst::class.java).setAction(ACTION_UMSCHALTEN),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val beenden = PendingIntent.getService(
            this,
            2,
            Intent(this, VorleseDienst::class.java).setAction(ACTION_STOPP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ic_vorlesen)
            .setContentTitle(if (pausiert) "Vorlesen angehalten" else "Der Gedankenspeicher liest vor")
            .setContentText(if (pausiert) "Zum Weiterhören hier tippen." else "Läuft weiter, auch in anderen Apps.")
            .setContentIntent(oeffnen)
            .addAction(R.drawable.ic_vorlesen, if (pausiert) "Weiter" else "Anhalten", umschalten)
            .addAction(R.drawable.ic_vorlesen, "Beenden", beenden)
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

    override fun onDestroy() {
        bereich.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val KANAL = "vorlesen"
        private const val BENACHRICHTIGUNG_ID = 7302
        private const val ACTION_STOPP = "de.frank.gedankenspeicher.VORLESEN_STOPP"
        private const val ACTION_UMSCHALTEN = "de.frank.gedankenspeicher.VORLESEN_UMSCHALTEN"

        /**
         * Startet den Dienst. Wird immer aus dem Vordergrund heraus gerufen (Druck auf den
         * Vorlese-Knopf) — nur dort erlaubt Android 14+ den Start. Schlägt er dennoch fehl,
         * wird trotzdem vorgelesen; es überlebt dann nur den Wechsel in eine andere App nicht.
         */
        fun starte(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context.applicationContext, VorleseDienst::class.java),
                )
            }
        }

        fun beende(context: Context) {
            VorleseAnzeige.pausiert.value = false
            runCatching {
                context.applicationContext.stopService(
                    Intent(context.applicationContext, VorleseDienst::class.java),
                )
            }
        }
    }
}

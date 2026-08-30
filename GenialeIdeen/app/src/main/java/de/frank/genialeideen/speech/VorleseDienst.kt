package de.frank.genialeideen.speech

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
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import de.frank.genialeideen.MainActivity
import de.frank.genialeideen.R
import de.frank.genialeideen.observability.IdeenLog

/**
 * Hält das Vorlesen am Leben, auch wenn der Bildschirm ausgeht oder die App verlassen wird
 * (Baustein D 4.3). Der Dienst beendet sich selbst, sobald der letzte Absatz durch ist —
 * er läuft nie still weiter.
 */
class VorleseDienst : Service() {

    private var sitzung: MediaSessionCompat? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        laeuft = true
        legeKanalAn()
        sitzung = MediaSessionCompat(this, "GenialeIdeenVorlesen").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPause() {
                    Vorleser.aktuell()?.pause()
                }

                override fun onPlay() {
                    Vorleser.aktuell()?.weiter()
                }

                override fun onStop() {
                    Vorleser.aktuell()?.stopp()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Der Vordergrund kommt IMMER zuerst — vor jeder Fallunterscheidung und vor jedem
        // vorzeitigen return. Wer mit startForegroundService gerufen wird, hat fünf Sekunden,
        // um startForeground nachzuliefern; sonst beendet Android die ganze App mit
        // ForegroundServiceDidNotStartInTimeException. Genau daran stürzte die App ab, wenn
        // man von einer vorgelesenen Idee zur nächsten sprang.
        val titel = intent?.getStringExtra(EXTRA_TITEL) ?: letzterTitel
        val nummer = intent?.getIntExtra(EXTRA_NUMMER, 0) ?: 0
        val gesamt = intent?.getIntExtra(EXTRA_GESAMT, 0) ?: 0
        val pausiert = intent?.getBooleanExtra(EXTRA_PAUSIERT, false) ?: false
        letzterTitel = titel
        starteImVordergrund(baueMeldung(titel, nummer, gesamt, pausiert))

        when (intent?.action) {
            VorleseAktion.PAUSE -> Vorleser.aktuell()?.pause()
            VorleseAktion.WEITER -> Vorleser.aktuell()?.weiter()
            VorleseAktion.STOPP -> {
                Vorleser.aktuell()?.stopp()
                halteAn()
                return START_NOT_STICKY
            }
            VorleseAktion.SCHLIESSEN -> {
                // Nur den Dienst abräumen; das Vorlesen hat sich bereits selbst beendet.
                halteAn()
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    /** Verlässt den Vordergrund ordentlich, bevor der Dienst stirbt. */
    private fun halteAn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        laeuft = false
        letzterTitel = "Geniale Ideen"
        sitzung?.isActive = false
        sitzung?.release()
        sitzung = null
        super.onDestroy()
    }

    private fun starteImVordergrund(meldung: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(MELDUNG_ID, meldung, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(MELDUNG_ID, meldung)
        }
    }

    private fun baueMeldung(
        titel: String,
        nummer: Int,
        gesamt: Int,
        pausiert: Boolean,
    ): Notification {
        val oeffnen = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val fortschritt = if (gesamt > 0) "Absatz $nummer von $gesamt" else "Wird vorbereitet"
        val bauer = NotificationCompat.Builder(this, KANAL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titel.ifBlank { "Geniale Ideen" })
            .setContentText(fortschritt)
            .setContentIntent(oeffnen)
            .setOngoing(!pausiert)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(MediaStyle().setMediaSession(sitzung?.sessionToken).setShowActionsInCompactView(0, 1))

        if (pausiert) {
            bauer.addAction(
                R.drawable.ic_notification,
                "Weiter",
                dienstIntent(VorleseAktion.WEITER),
            )
        } else {
            bauer.addAction(
                R.drawable.ic_notification,
                "Pause",
                dienstIntent(VorleseAktion.PAUSE),
            )
        }
        bauer.addAction(R.drawable.ic_notification, "Stopp", dienstIntent(VorleseAktion.STOPP))

        sitzung?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP,
                )
                .setState(
                    if (pausiert) PlaybackStateCompat.STATE_PAUSED else PlaybackStateCompat.STATE_PLAYING,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1f,
                )
                .build(),
        )
        return bauer.build()
    }

    private fun dienstIntent(aktion: String): PendingIntent = PendingIntent.getService(
        this,
        aktion.hashCode(),
        VorleseAktion.intent(this, aktion),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun legeKanalAn() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val kanal = NotificationChannel(
            KANAL_ID,
            "Vorlesen",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Zeigt an, was gerade vorgelesen wird, mit Pause, Weiter und Stopp."
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(kanal)
    }

    companion object {
        /** Ob der Dienst gerade lebt — ohne das würde [beenden] ihn erst starten. */
        @Volatile private var laeuft = false

        /** Der zuletzt gezeigte Titel, damit ein Neustart ohne Extras nicht leer dasteht. */
        @Volatile private var letzterTitel = "Geniale Ideen"

        private const val KANAL_ID = "vorlesen"
        private const val MELDUNG_ID = 4711
        private const val EXTRA_TITEL = "titel"
        private const val EXTRA_NUMMER = "nummer"
        private const val EXTRA_GESAMT = "gesamt"
        private const val EXTRA_PAUSIERT = "pausiert"

        fun starten(context: Context) {
            aktualisiere(context, "Geniale Ideen", 0, 0, pausiert = false)
        }

        fun aktualisiere(
            context: Context,
            titel: String,
            nummer: Int,
            gesamt: Int,
            pausiert: Boolean,
        ) {
            // Ohne Benachrichtigungsrecht gibt es keinen Vordergrunddienst — das Vorlesen läuft
            // dann nur im Vordergrund weiter, statt die App abstürzen zu lassen.
            if (!darfMelden(context)) {
                IdeenLog.warn("VorleseDienst", "aktualisiere", "Ohne Benachrichtigungsrecht kein Dienst")
                return
            }
            val intent = Intent(context, VorleseDienst::class.java)
                .putExtra(EXTRA_TITEL, titel)
                .putExtra(EXTRA_NUMMER, nummer)
                .putExtra(EXTRA_GESAMT, gesamt)
                .putExtra(EXTRA_PAUSIERT, pausiert)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }.onFailure {
                IdeenLog.warn("VorleseDienst", "aktualisiere", "Dienst nicht startbar", mapOf("art" to it.javaClass.simpleName))
            }
        }

        /**
         * Beendet den Dienst über seinen eigenen Weg, damit er vorher noch [startForeground]
         * ruft. Ein blosses `stopService` von aussen schiesst ihn ab, während ein gerade
         * abgesetztes `startForegroundService` noch auf sein `startForeground` wartet — und
         * genau dieses Rennen kostete die App das Leben.
         */
        fun beenden(context: Context) {
            if (!laeuft) return
            val intent = Intent(context, VorleseDienst::class.java).setAction(VorleseAktion.SCHLIESSEN)
            runCatching { context.startService(intent) }
                .onFailure {
                    // Aus dem Hintergrund darf kein Dienst mehr angestossen werden; dann bleibt
                    // nur der harte Weg — dort wartet aber auch niemand mehr auf startForeground.
                    runCatching { context.stopService(Intent(context, VorleseDienst::class.java)) }
                }
        }

        private fun darfMelden(context: Context): Boolean =
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

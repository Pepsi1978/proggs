package de.frank.jarvis.ablage

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import de.frank.jarvis.R
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Eine Erinnerung in der Form Wecker: Weckton in der Schleife, dazwischen liest Jarvis die Kurzfassung vor, bis Frank ausdrücklich ausschaltet.
 * Bewusst ohne Höchstdauer und ohne Wegwischen: Weggewischt kommt die Meldung sofort wieder. Aus ist nur „Aus“, in der App oder per Sprache,
 * oder eine Bestätigung von einem anderen Gerät.
 */
class ErinnerungKlingelDienst : Service() {
    private var spieler: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var ablauf: Job? = null
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() { super.onCreate(); instanz = this }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Wer per startForegroundService gerufen wird, muss startForeground liefern, auch wenn schon alles aus ist.
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(NUMMER, baueMeldung(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            else startForeground(NUMMER, baueMeldung())
        }.onFailure { Logbuch.fehler("Erinnerung Vordergrund", it) }
        if (ErinnerungPlaner.klingelt.value.isEmpty()) { beenden(); return START_NOT_STICKY }
        if (spieler == null) klingeln()
        else aktualisieren()
        return START_NOT_STICKY
    }

    private fun klingeln() {
        val attribute = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        for (ton in listOfNotNull(RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))) {
            val p = MediaPlayer()
            val ok = runCatching {
                p.setAudioAttributes(attribute); p.setDataSource(this, ton); p.isLooping = true
                p.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK); p.prepare(); p.start()
            }.isSuccess
            if (ok) { spieler = p; break } else runCatching { p.release() }
        }
        runCatching {
            val v = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator else @Suppress("DEPRECATION") (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0), attribute)
            vibrator = v
        }
        // Erst der Ton, dann die Kurzfassung bei leisem Weckton, danach alle zwei Minuten erneut.
        ablauf = bereich.launch {
            delay(6_000L)
            while (isActive) {
                ErinnerungPlaner.klingelt.value.forEach { f ->
                    runCatching { spieler?.setVolume(0.12f, 0.12f) }
                    ErinnerungStimme.sprich(applicationContext, f.erinnerung, alsWecker = true)
                    runCatching { spieler?.setVolume(1f, 1f) }
                }
                delay(120_000L)
            }
        }
    }

    private fun aktualisieren() {
        if (ErinnerungPlaner.klingelt.value.isEmpty()) beenden()
        else runCatching { getSystemService(NotificationManager::class.java).notify(NUMMER, baueMeldung()) }
    }

    private fun beenden() {
        ablauf?.cancel(); ablauf = null
        runCatching { spieler?.stop() }; runCatching { spieler?.release() }; spieler = null
        runCatching { vibrator?.cancel() }; vibrator = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        instanz = null
        ablauf?.cancel()
        runCatching { spieler?.release() }; spieler = null
        runCatching { vibrator?.cancel() }
        super.onDestroy()
    }

    private fun baueMeldung(): Notification {
        val liste = ErinnerungPlaner.klingelt.value
        val flaggen = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        fun rundfunk(code: Int, aktion: String) = PendingIntent.getBroadcast(this, code, Intent(this, ErinnerungEmpfaenger::class.java).setAction(aktion), flaggen)
        val text = liste.firstOrNull()?.erinnerung?.kurz ?: "Erinnerung"
        val titel = if (liste.size > 1) "${liste.size} Erinnerungen" else liste.firstOrNull()?.erinnerung?.zeile ?: "Erinnerung"
        return NotificationCompat.Builder(this, de.frank.jarvis.timer.Timer.KANAL_STILL).setSmallIcon(R.drawable.ic_stat_jarvis)
            .setContentTitle(titel).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true).setAutoCancel(false).setOnlyAlertOnce(true)
            .apply { liste.firstOrNull()?.let { setContentIntent(ErinnerungPlaner.oeffnenAbsicht(this@ErinnerungKlingelDienst, it.erinnerung.id, 19_997)) } }
            .addAction(R.drawable.ic_stat_jarvis, "Aus", rundfunk(19_996, ErinnerungPlaner.AKTION_BESTAETIGEN))
            .setDeleteIntent(rundfunk(19_995, ErinnerungPlaner.AKTION_ZEIGEN))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val NUMMER = 19_994
        @Volatile private var instanz: ErinnerungKlingelDienst? = null
        fun abgleichen() { Handler(Looper.getMainLooper()).post { instanz?.aktualisieren() } }
    }
}

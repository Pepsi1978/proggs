package de.frank.jarvis.timer

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
import android.util.Log
import androidx.core.app.NotificationCompat
import de.frank.jarvis.MainActivity
import de.frank.jarvis.R
import de.frank.jarvis.data.Einstellungen

/**
 * Lässt einen abgelaufenen Timer klingeln wie einen Wecker: Weckton in der Schleife (Wecker-Lautstärke, auch bei
 * „Nicht stören“), dazu Vibration, bis Frank in der Meldung, in der App oder per Sprache ausschaltet. Wischt er die
 * Meldung weg, kommt sie wieder: Aus ist nur „Aus“ oder „Schlummern“. Die Liste der klingelnden Timer führt [Timer.klingelt].
 */
class TimerKlingelDienst : Service() {
    private var spieler: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val hoechstdauer = Runnable {
        // Niemand hat ausgeschaltet: still melden, damit der Timer nicht unbemerkt bleibt.
        val verpasst = Timer.klingelt.value
        Timer.klingelnAus(this)
        verpasst.forEach { Timer.meldung(applicationContext, it.id, it, laut = false) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instanz = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Wer per startForegroundService gerufen wird, muss startForeground liefern, auch wenn schon alles aus ist.
        vordergrund(baueMeldung())
        if (Timer.klingelt.value.isEmpty()) { beenden(); return START_NOT_STICKY }
        if (spieler == null) klingeln()
        else {
            // Ein weiterer Timer ist abgelaufen: Er bekommt die volle Klingeldauer, nicht nur den Rest des ersten.
            val max = Einstellungen.get(this).timerKlingelMax
            handler.removeCallbacks(hoechstdauer)
            if (max > 0) handler.postDelayed(hoechstdauer, max * 60_000L)
        }
        return START_NOT_STICKY
    }

    private fun vordergrund(meldung: Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(MELDUNG_ID, meldung, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            else startForeground(MELDUNG_ID, meldung)
        }.onFailure { Log.w(TAG, "Vordergrund nicht möglich", it) }
    }

    private fun klingeln() {
        val e = Einstellungen.get(this)
        val attribute = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val toene = listOfNotNull(
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        )
        for (ton in toene) {
            val p = MediaPlayer()
            val ok = runCatching {
                p.setAudioAttributes(attribute)
                p.setDataSource(this, ton)
                p.isLooping = true
                p.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
                val v = e.timerLautstaerke / 100f
                p.setVolume(v, v)
                p.prepare()
                p.start()
            }.onFailure { Log.w(TAG, "Weckton $ton nicht abspielbar", it) }.isSuccess
            if (ok) { spieler = p; break } else runCatching { p.release() }
        }
        if (e.timerVibration) runCatching {
            val v = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator
            else @Suppress("DEPRECATION") (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0), attribute)
            vibrator = v
        }.onFailure { Log.w(TAG, "Vibration nicht möglich", it) }
        val max = e.timerKlingelMax
        handler.removeCallbacks(hoechstdauer)
        if (max > 0) handler.postDelayed(hoechstdauer, max * 60_000L)
    }

    /** Nach jeder Änderung an [Timer.klingelt]: Meldung erneuern oder, wenn nichts mehr klingelt, aufhören. */
    private fun aktualisieren() {
        if (Timer.klingelt.value.isEmpty()) beenden()
        else runCatching { getSystemService(NotificationManager::class.java).notify(MELDUNG_ID, baueMeldung()) }
    }

    private fun beenden() {
        handler.removeCallbacks(hoechstdauer)
        runCatching { spieler?.stop() }
        runCatching { spieler?.release() }
        spieler = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        instanz = null
        handler.removeCallbacks(hoechstdauer)
        runCatching { spieler?.release() }
        spieler = null
        runCatching { vibrator?.cancel() }
        super.onDestroy()
    }

    private fun baueMeldung(): Notification {
        Timer.kanalAnlegen(this)
        val liste = Timer.klingelt.value
        val e = Einstellungen.get(this)
        val flaggen = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        fun rundfunk(code: Int, aktion: String) = PendingIntent.getBroadcast(this, code, Intent(this, TimerEmpfaenger::class.java).setAction(aktion).putExtra(Timer.EXTRA_ID, -1), flaggen)
        val oeffnen = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), flaggen)
        val titel = when (liste.size) { 0 -> "Timer"; 1 -> "${liste[0].name} ist abgelaufen"; else -> "${liste.size} Timer sind abgelaufen" }
        val text = if (liste.size == 1) "Nach " + Timer.dauerText(liste[0].dauerMs) + " · Tippe „Aus“ zum Ausschalten" else liste.joinToString(", ") { it.name }
        return NotificationCompat.Builder(this, Timer.KANAL_STILL).setSmallIcon(R.drawable.ic_stat_jarvis)
            .setContentTitle(titel).setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true).setAutoCancel(false).setOnlyAlertOnce(true)
            .setContentIntent(oeffnen)
            .addAction(R.drawable.ic_stat_jarvis, "Aus", rundfunk(9_998, Timer.AKTION_STOPPEN))
            .addAction(R.drawable.ic_stat_jarvis, "Schlummern ${e.timerSchlummern} min", rundfunk(9_997, Timer.AKTION_SCHLUMMERN))
            // Weggewischt (geht ab Android 14 auch bei Vordergrund-Diensten): gleich wieder zeigen.
            .setDeleteIntent(rundfunk(9_996, Timer.AKTION_ZEIGEN))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val TAG = "JarvisTimerKlingeln"
        private const val MELDUNG_ID = 7_999
        @Volatile private var instanz: TimerKlingelDienst? = null

        fun abgleichen() {
            Handler(Looper.getMainLooper()).post { instanz?.aktualisieren() }
        }
    }
}

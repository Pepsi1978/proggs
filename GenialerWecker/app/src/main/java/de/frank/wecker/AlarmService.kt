package de.frank.wecker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import de.frank.genialeideen.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.math.roundToInt

data class RingState(val alarm: Alarm? = null, val step: String = "", val message: String = "", val test: Boolean = false, val playing: Boolean = false)

/** Beim Wecken ausschließlich lokale Wiedergabe. Der Dienst hält auch ohne Activity den Alarm. */
class AlarmService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var store: AlarmStore
    private lateinit var audio: AudioManager
    private var player: MediaPlayer? = null
    private var wake: PowerManager.WakeLock? = null
    private var focus: AudioFocusRequest? = null
    private var current: Alarm? = null
    private var originalVolume = -1
    private var loop: Job? = null
    private var generation = 0
    private var test = false
    private var fallbackFailures = 0

    override fun onCreate() {
        super.onCreate()
        store = AlarmStore.get(this)
        audio = getSystemService(AudioManager::class.java)
        startForeground(NOTIFICATION, notification(null))
        originalVolume = store.prefs.getInt("volumeBeforeRing", -1).takeIf { it >= 0 }
            ?: audio.getStreamVolume(AudioManager.STREAM_ALARM)
        store.prefs.edit().putInt("volumeBeforeRing", originalVolume).commit()
        wake = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:alarm").apply { acquire(12 * 60 * 60_000L) }
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(attributes).setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_GAIN) current?.let { setVolume(it.volume) }
            }.build().also { audio.requestAudioFocus(it) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "TEST" -> {
                // Eine Vorschau darf einen bereits klingelnden echten Alarm niemals ersetzen.
                if (store.ringing().isEmpty()) {
                    test = true
                    store.get(intent.getStringExtra("id").orEmpty())?.let(::begin) ?: stopSelf()
                }
            }
            "STOP" -> if (current?.photoRequired != true || test) finishCurrent(false)
            "PHOTO_OK" -> {
                // Nur die nicht exportierte AlarmActivity liefert dieses interne Kommando.
                if (intent.getStringExtra("id") == current?.id) finishCurrent(false)
            }
            "SNOOZE" -> finishCurrent(true)
            else -> {
                if (test && store.ringing().isNotEmpty()) { test = false; current = null }
                if (current == null) nextAlarm()
                else getSystemService(NotificationManager::class.java).notify(NOTIFICATION, notification(current))
            }
        }
        if (current == null && store.ringing().isEmpty()) stopSelf()
        return START_STICKY
    }

    private fun nextAlarm() {
        val ids = store.ringing().filter { store.get(it) != null }
        store.ringing(ids)
        val alarm = ids.firstOrNull()?.let(store::get)
        if (alarm == null) stopSelf() else begin(alarm)
    }

    private fun begin(alarm: Alarm) {
        current = alarm
        fallbackFailures = 0
        generation++
        val token = generation
        loop?.cancel()
        player?.release(); player = null
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION, notification(alarm))
        _state.value = RingState(alarm, "Wecken", test = test)
        if (alarm.vibrate) getSystemService(Vibrator::class.java).vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 400, 300, 400, 1200), 0))
        loop = scope.launch {
            val start = System.currentTimeMillis()
            while (isActive) {
                val fraction = if (alarm.fadeSeconds == 0) 1f else
                    ((System.currentTimeMillis() - start).toFloat() / (alarm.fadeSeconds * 1000)).coerceIn(.05f, 1f)
                setVolume((alarm.volume * fraction).roundToInt().coerceAtLeast(1))
                delay(500)
            }
        }
        val clips = buildList<Pair<String, String>> {
            alarm.steps.forEach { step ->
                when (step) {
                    Step.TONE -> add(step.title to Tones.file(store.files, alarm.cue).absolutePath)
                    Step.MUSIC -> add(step.title to alarm.music.ifBlank { Tones.file(store.files, alarm.tone).absolutePath })
                    Step.IDEAS, Step.TEXT -> {
                        val prepared = alarm.prepared[step.name].orEmpty().filter { File(it).isFile && File(it).length() > 44 }
                        if (prepared.isNotEmpty()) prepared.forEach { add(step.title to it) }
                        else {
                            _state.value = _state.value.copy(message = "${step.title}: noch kein Offline-Audio. Der Ersatzweckton läuft.")
                            add(step.title to Tones.file(store.files, alarm.tone).absolutePath)
                        }
                    }
                }
            }
        }.ifEmpty { listOf("Weckton" to Tones.file(store.files, "classic").absolutePath) }
        play(clips, 0, token)
    }

    private fun setVolume(percent: Int) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val value = (max * percent / 100f).roundToInt().coerceIn(1, max)
        runCatching { if (audio.getStreamVolume(AudioManager.STREAM_ALARM) != value) audio.setStreamVolume(AudioManager.STREAM_ALARM, value, 0) }
            .onFailure { _state.value = _state.value.copy(message = "Android blockiert die Lautstärke. Bitte Wecker unter Nicht stören zulassen.") }
    }

    private fun play(clips: List<Pair<String, String>>, index: Int, token: Int) {
        if (token != generation || current == null) return
        player?.release()
        val clip = clips[index % clips.size]
        _state.value = _state.value.copy(step = clip.first, playing = false)
        val next = MediaPlayer()
        player = next
        fun failed() {
            if (token != generation) return
            fallbackFailures++
            _state.value = _state.value.copy(message = "Audio nicht abspielbar. Der lokale Ersatzweckton wird verwendet.")
            scope.launch {
                delay(if (fallbackFailures > 2) 2000 else 100)
                if (token == generation) play(listOf("Ersatzweckton" to Tones.file(store.files, "classic").absolutePath), 0, token)
            }
        }
        try {
            next.setAudioAttributes(attributes)
            next.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            if (clip.second.startsWith("content:")) next.setDataSource(this, Uri.parse(clip.second))
            else next.setDataSource(clip.second)
            next.setOnPreparedListener {
                if (token == generation) {
                    if (clip.first == Step.IDEAS.title || clip.first == Step.TEXT.title) {
                        runCatching { it.playbackParams = android.media.PlaybackParams().setSpeed(current?.preparedSpeed ?: 1f) }
                    }
                    it.start()
                    _state.value = _state.value.copy(playing = true)
                }
            }
            next.setOnCompletionListener { if (token == generation) play(clips, index + 1, token) }
            next.setOnErrorListener { _, _, _ -> failed(); true }
            next.prepareAsync()
        } catch (_: Exception) { failed() }
    }

    private fun finishCurrent(snooze: Boolean) {
        val alarm = current ?: return
        if (snooze && !test) {
            val latest = store.get(alarm.id) ?: alarm
            if (latest.snoozes >= latest.snoozeLimit) {
                _state.value = _state.value.copy(message = "Alle Schlummerpausen sind aufgebraucht.")
                return
            }
            val updated = latest.copy(snoozeUntil = System.currentTimeMillis() + latest.snoozeMinutes * 60_000, snoozes = latest.snoozes + 1)
            try { AlarmScheduler(this).schedule(updated); store.put(updated) }
            catch (_: Exception) { _state.value = _state.value.copy(message = "Schlummern konnte nicht geplant werden. Der Wecker läuft weiter."); return }
        }
        generation++
        loop?.cancel()
        player?.release(); player = null
        getSystemService(Vibrator::class.java).cancel()
        if (!test) store.ringing(store.ringing().filterNot { it == alarm.id })
        current = null
        test = false
        nextAlarm()
    }

    private fun notification(alarm: Alarm?): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Aktiver Wecker", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null); enableVibration(false); lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        })
        val open = PendingIntent.getActivity(this, 1, Intent(this, AlarmActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_wecker)
            .setContentTitle(alarm?.name ?: "Genialer Wecker").setContentText("Zum Wecker öffnen")
            .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setContentIntent(open)
            .setFullScreenIntent(open, true).setOnlyAlertOnce(true)
        if (alarm != null && alarm.snoozeLimit > alarm.snoozes) builder.addAction(0, "Schlummern", action("SNOOZE", 2))
        if (alarm != null && !alarm.photoRequired) builder.addAction(0, "Stoppen", action("STOP", 3))
        return builder.build()
    }
    private fun action(name: String, code: Int) = PendingIntent.getService(this, code, Intent(this, AlarmService::class.java).setAction(name), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        generation++; current = null
        scope.cancel(); player?.release(); player = null
        getSystemService(Vibrator::class.java).cancel()
        focus?.let(audio::abandonAudioFocusRequest)
        runCatching { if (originalVolume >= 0) audio.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0) }
        store.prefs.edit().remove("volumeBeforeRing").commit()
        wake?.let { if (it.isHeld) it.release() }
        _state.value = RingState()
        super.onDestroy()
    }
    companion object {
        const val CHANNEL = "alarm_ring_v1"
        const val NOTIFICATION = 1001
        val attributes: AudioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        private val _state = MutableStateFlow(RingState())
        val state = _state.asStateFlow()
    }
}

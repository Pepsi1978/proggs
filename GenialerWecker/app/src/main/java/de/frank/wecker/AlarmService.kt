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

/**
 * Bestätigtes Ergebnis einer Bedienaktion. Der Weckbildschirm zeigt Erfolg nur nach diesem Signal,
 * nie schon nach dem bloßen Tippen. [seq] steigt mit jedem Ergebnis, auch bei gleicher Aktion.
 */
data class RingResult(val seq: Long, val id: String, val ringId: Long, val action: String, val ok: Boolean, val snoozeUntil: Long = 0)

/** [ringId] identifies one ringing instance; the same alarm id rings again with a new ringId. */
data class RingState(val ringId: Long = 0, val alarm: Alarm? = null, val step: String = "", val message: String = "", val test: Boolean = false, val playing: Boolean = false, val variation: Int = 1)

/** Beim Wecken ausschließlich lokale Wiedergabe. Der Dienst hält auch ohne Activity den Alarm. */
class AlarmService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var store: AlarmStore
    private lateinit var audio: AudioManager
    private var playback: AlarmAudioQueue? = null
    private var wake: PowerManager.WakeLock? = null
    private var focus: AudioFocusRequest? = null
    private var current: Alarm? = null
    private var originalVolume = -1
    private var loop: Job? = null
    private var generation = 0
    private var test = false
    private var fallbackFailures = 0
    /** Klingelaufträge, deren Speicherung fehlschlug. Sie klingeln trotzdem, solange dieser Prozess lebt. */
    private val volatileRinging = linkedSetOf<String>()
    private fun pending(): List<String> = (store.ringingEntries().filterNot(::wasStopped).map { it.id } + volatileRinging).distinct()
    /** Commands from notifications carry no id; an explicit id must match the current alarm. */
    /** A test started from the app button: silent notification, the app opens the alarm screen itself. */
    private var quiet = false
    private var foregroundId = 0
    private fun Intent.targetsCurrent() = getStringExtra("id")?.let { it == current?.id } ?: true
    /** An explicit ring id must match the ringing instance, so stale screens or notifications never act on a newer ring. */
    private fun Intent.targetsRing() = targetsCurrent() && (!hasExtra("ring") || getLongExtra("ring", 0) == ringId)
    private var ringId = 0L
    /** Failures are attributed to the ring the request was meant for. */
    private fun Intent.reject(action: String) {
        val id = getStringExtra("id") ?: return
        report(id, action, false, getLongExtra("ring", 0))
    }

    override fun onCreate() {
        super.onCreate()
        trace("create")
        store = AlarmStore.get(this)
        audio = getSystemService(AudioManager::class.java)
        originalVolume = store.prefs.getInt("volumeBeforeRing", -1).takeIf { it >= 0 }
            ?: audio.getStreamVolume(AudioManager.STREAM_ALARM)
        store.prefs.edit().putInt("volumeBeforeRing", originalVolume).commit()
        wake = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:alarm").apply { acquire(12 * 60 * 60_000L) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        trace("command=${intent?.action} current=${current != null} test=$test")
        // Decided per first command, so the button test never flashes the loud full-screen notification.
        if (foregroundId == 0) {
            showForeground(null, intent?.action == "TEST" && intent.getBooleanExtra("quiet", false) && pending().isEmpty())
            // Request audio focus only once the service is in the foreground (required for background starts on Android 15+).
            focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attributes).setOnAudioFocusChangeListener { change ->
                    if (change == AudioManager.AUDIOFOCUS_GAIN) current?.let { setVolume(it.volume) }
                }.build().also { audio.requestAudioFocus(it) }
        }
        when (intent?.action) {
            "TEST" -> {
                // Eine Vorschau darf einen bereits klingelnden echten Alarm niemals ersetzen.
                if (pending().isEmpty()) {
                    // Same UI and audio path as a real alarm; the snooze counter lives only in memory.
                    TestSnooze.cancel(this)
                    test = true
                    quiet = intent.getBooleanExtra("quiet", false)
                    val snoozes = intent.getIntExtra("testSnoozes", 0)
                    store.get(intent.getStringExtra("id").orEmpty())?.let { begin(it.copy(snoozes = snoozes, snoozeUntil = 0)) } ?: stopSelf()
                }
            }
            "STOP" -> if (intent.targetsRing() && current?.photoRequired != true) finishCurrent(false) else intent.reject("STOP")
            // Only a test may be ended without its photo task (back gesture), so nobody gets stuck in a test.
            "TEST_END" -> if (test && intent.targetsRing()) finishCurrent(false) else intent.reject("STOP")
            "PHOTO_OK" -> {
                // Nur die nicht exportierte AlarmActivity liefert dieses interne Kommando.
                if (intent.getStringExtra("id") == current?.id && intent.targetsRing()) finishCurrent(false) else intent.reject("STOP")
            }
            "SNOOZE" -> if (intent.targetsRing()) finishCurrent(true) else intent.reject("SNOOZE")
            else -> {
                // A real alarm always wins over a test, including a test that is currently snoozing.
                TestSnooze.cancel(this)
                intent?.getStringExtra("id")?.let { id -> if (id !in store.ringing() && store.get(id) != null) volatileRinging += id }
                if (test && pending().isNotEmpty()) { test = false; quiet = false; current = null }
                if (current == null) nextAlarm()
                else showForeground(current, quiet)
            }
        }
        if (current == null && pending().isEmpty()) stopSelf()
        return START_STICKY
    }

    private fun nextAlarm() {
        val entries = store.ringingEntries()
        // Stopped occurrences stay stopped in this process even if removing them from storage failed; retry the removal.
        val live = entries.filter { store.get(it.id) != null && !wasStopped(it) }
        if (live.size != entries.size) runCatching { store.writeRinging(live) }
        volatileRinging.retainAll { store.get(it) != null }
        val alarm = (live.map { it.id } + volatileRinging).distinct().firstOrNull()?.let(store::get)
        if (alarm == null) stopSelf() else begin(alarm)
    }

    private fun begin(alarm: Alarm) {
        AlarmRinging.cancelFallback(this)
        current = alarm
        ringId = android.os.SystemClock.elapsedRealtimeNanos()
        fallbackFailures = 0
        generation++
        val token = generation
        loop?.cancel()
        playback?.close(); playback = null
        showForeground(alarm, quiet)
        _state.value = RingState(ringId, alarm, "Wecken", test = test)
        if (alarm.vibrate) vibrateAsAlarm()
        loop = scope.launch {
            val start = System.currentTimeMillis()
            while (isActive) {
                val fraction = if (alarm.fadeSeconds == 0) 1f else
                    ((System.currentTimeMillis() - start).toFloat() / (alarm.fadeSeconds * 1000)).coerceIn(.05f, 1f)
                setVolume((alarm.volume * fraction).roundToInt().coerceAtLeast(1))
                delay(500)
            }
        }
        val tones = Tones.names.keys.associateWith { Tones.file(store.files, it).absolutePath }
        play(AlarmPlaylist.build(alarm, tones), token)
    }

    /** Alarm usage lets the vibration pass silent mode and Do Not Disturb like the sound does. */
    private fun vibrateAsAlarm() = runCatching {
        val vibrator = getSystemService(Vibrator::class.java)
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 400, 300, 400, 1200), 0)
        if (android.os.Build.VERSION.SDK_INT >= 33) vibrator.vibrate(effect, android.os.VibrationAttributes.createForUsage(android.os.VibrationAttributes.USAGE_ALARM))
        else @Suppress("DEPRECATION") vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
    }

    private fun setVolume(percent: Int) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val value = (max * percent / 100f).roundToInt().coerceIn(1, max)
        runCatching { if (audio.getStreamVolume(AudioManager.STREAM_ALARM) != value) audio.setStreamVolume(AudioManager.STREAM_ALARM, value, 0) }
            .onFailure { _state.value = _state.value.copy(message = "Android blockiert die Lautstärke. Bitte Wecker unter Nicht stören zulassen.") }
    }

    private fun play(clips: List<AlarmClip>, token: Int) {
        if (token != generation || current == null) return
        playback?.close()
        playback = AlarmAudioQueue(this, clips, onPlaying = { clip ->
            trace("play variant=${clip.variation} fallback=${clip.audio.fallback}")
            if (token == generation) _state.value = _state.value.copy(step = clip.step, playing = true, variation = clip.variation,
                message = when {
                    clip.audio.fallback && clip.audio.provider == "local" -> "Kein gültiges Sprach-Audio verfügbar. Der lokale Ersatzweckton läuft."
                    clip.audio.fallback -> "Die vorbereitete Edge-Notfallstimme wird verwendet."
                    else -> ""
                })
        }, onFailure = { error ->
            if (token == generation) {
                android.util.Log.w("WeckerPlayback", "Wiedergabe wird durch lokalen Ersatzweckton abgesichert", error)
                trace("failure=${error.javaClass.simpleName}:${error.message}")
                fallbackFailures++
                _state.value = _state.value.copy(message = "Audio nicht abspielbar. Der lokale Ersatzweckton wird verwendet.", playing = false)
                scope.launch {
                    delay(if (fallbackFailures > 2) 2000 else 100)
                    if (token == generation) play(listOf(AlarmClip("Ersatzweckton", PreparedAudio(Tones.file(store.files, "classic").absolutePath, fallback = true, provider = "local"))), token)
                }
            }
        }).also { it.start() }
    }

    private fun finishCurrent(snooze: Boolean) {
        val alarm = current ?: return
        val ring = ringId
        var snoozeUntil = 0L
        if (snooze && !test) {
            val latest = store.get(alarm.id) ?: alarm
            if (latest.snoozes >= latest.snoozeLimit) {
                _state.value = _state.value.copy(message = "Alle Schlummerpausen sind aufgebraucht.")
                report(alarm.id, "SNOOZE", false, ring)
                return
            }
            val updated = latest.copy(snoozeUntil = System.currentTimeMillis() + latest.snoozeMinutes * 60_000, snoozes = latest.snoozes + 1)
            // Persist first: a snooze alarm that fires must find its matching snoozeUntil. Roll back if planning fails.
            try { store.put(updated) }
            catch (_: Exception) { _state.value = _state.value.copy(message = "Schlummern konnte nicht gespeichert werden. Der Wecker läuft weiter."); report(alarm.id, "SNOOZE", false, ring); return }
            try { AlarmScheduler(this).schedule(updated) }
            catch (_: Exception) {
                runCatching { store.put(latest) }
                _state.value = _state.value.copy(message = "Schlummern konnte nicht geplant werden. Der Wecker läuft weiter.")
                report(alarm.id, "SNOOZE", false, ring)
                return
            }
            SnoozeNotice.show(this, updated)
            snoozeUntil = updated.snoozeUntil
        }
        if (test) {
            if (!snooze) TestSnooze.cancel(this)
            else if (alarm.snoozes >= alarm.snoozeLimit) {
                _state.value = _state.value.copy(message = "Alle Schlummerpausen sind aufgebraucht.")
                report(alarm.id, "SNOOZE", false, ring)
                return
            } else try {
                snoozeUntil = System.currentTimeMillis() + alarm.snoozeMinutes * 60_000L
                TestSnooze.start(this, alarm.id, alarm.snoozes + 1, snoozeUntil)
            } catch (_: Exception) {
                _state.value = _state.value.copy(message = "Schlummern konnte nicht geplant werden. Der Wecker läuft weiter.")
                report(alarm.id, "SNOOZE", false, ring)
                return
            }
        }
        generation++
        loop?.cancel()
        playback?.close(); playback = null
        getSystemService(Vibrator::class.java).cancel()
        AlarmRinging.cancelFallback(this)
        if (!test) {
            store.ringingEntries().find { it.id == alarm.id }?.let(::markStopped)
            runCatching { store.ringing(store.ringing().filterNot { it == alarm.id }) }
            volatileRinging -= alarm.id
            // Advance an occurrence that could not be advanced at claim time and retry failed planning once.
            AlarmScheduler(this).settleAfterRing(alarm.id)
        }
        current = null
        test = false
        quiet = false
        // Confirmed before the next pending alarm begins, so a new alarm always replaces this feedback.
        report(alarm.id, if (snooze) "SNOOZE" else "STOP", true, ring, snoozeUntil)
        nextAlarm()
    }

    private fun report(id: String, action: String, ok: Boolean, ring: Long, snoozeUntil: Long = 0) {
        _result.value = RingResult((_result.value?.seq ?: 0) + 1, id, ring, action, ok, snoozeUntil)
    }

    /**
     * Real alarms and the silent button test use different notification ids: a real alarm arriving during a
     * test gets a fresh notification and therefore its full-screen alert despite setOnlyAlertOnce.
     */
    private fun showForeground(alarm: Alarm?, quietMode: Boolean) {
        val id = if (quietMode) TEST_NOTIFICATION else NOTIFICATION
        val manager = getSystemService(NotificationManager::class.java)
        if (id == foregroundId) { manager.notify(id, notification(alarm, quietMode)); return }
        androidx.core.app.ServiceCompat.startForeground(this, id, notification(alarm, quietMode),
            if (android.os.Build.VERSION.SDK_INT >= 29) android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0)
        if (foregroundId != 0) manager.cancel(foregroundId)
        foregroundId = id
    }

    private fun notification(alarm: Alarm?, quietMode: Boolean): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        val open = PendingIntent.getActivity(this, 1, Intent(this, AlarmActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = if (quietMode) {
            manager.createNotificationChannel(NotificationChannel(TEST_CHANNEL, "Testwecken", NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null); enableVibration(false); setShowBadge(false)
            })
            NotificationCompat.Builder(this, TEST_CHANNEL).setPriority(NotificationCompat.PRIORITY_LOW).setSilent(true)
        } else {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Aktiver Wecker", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null); enableVibration(false); lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            })
            NotificationCompat.Builder(this, CHANNEL).setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setFullScreenIntent(open, true)
        }
        builder.setSmallIcon(R.drawable.ic_wecker)
            .setContentTitle(alarm?.name ?: "Genialer Wecker").setContentText("Zum Wecker öffnen")
            .setOngoing(true).setContentIntent(open).setOnlyAlertOnce(true)
        if (alarm != null && alarm.snoozeLimit > alarm.snoozes) builder.addAction(0, "Schlummern", action("SNOOZE", 2, alarm))
        if (alarm != null && !alarm.photoRequired) builder.addAction(0, "Beenden", action("STOP", 3, alarm))
        return builder.build()
    }
    /** The ring id is part of the intent identity (data URI), so an old notification can never be redirected to a newer ring. */
    private fun action(name: String, code: Int, alarm: Alarm) = PendingIntent.getService(this, code, Intent(this, AlarmService::class.java).setAction(name)
        .setData(android.net.Uri.parse("wecker://ring/$ringId/$name")).putExtra("id", alarm.id).putExtra("ring", ringId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        trace("destroy")
        generation++; current = null
        scope.cancel(); playback?.close(); playback = null
        getSystemService(Vibrator::class.java).cancel()
        focus?.let(audio::abandonAudioFocusRequest)
        runCatching { if (originalVolume >= 0) audio.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0) }
        store.prefs.edit().remove("volumeBeforeRing").commit()
        wake?.let { if (it.isHeld) it.release() }
        _state.value = RingState()
        super.onDestroy()
    }
    companion object {
        private val events = ArrayDeque<String>()
        @Synchronized private fun trace(event: String) {
            if (events.size >= 40) events.removeFirst()
            events.addLast("${android.os.SystemClock.elapsedRealtime()} $event")
            android.util.Log.d("WeckerLifecycle", event)
        }
        @Synchronized internal fun diagnosticEvents() = events.joinToString("\n")
        private val stopped = mutableMapOf<String, Long>()
        @Synchronized private fun markStopped(entry: RingEntry) { stopped[entry.id] = entry.at }
        /** True for an occurrence already stopped in this process. A newly claimed occurrence has another `at`. */
        @Synchronized internal fun wasStopped(entry: RingEntry) = stopped[entry.id] == entry.at
        const val CHANNEL = "alarm_ring_v1"
        const val NOTIFICATION = 1001
        const val TEST_CHANNEL = "alarm_test_v1"
        const val TEST_NOTIFICATION = 1003
        val attributes: AudioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        private val _state = MutableStateFlow(RingState())
        private val _result = MutableStateFlow<RingResult?>(null)
        val result = _result.asStateFlow()
        val state = _state.asStateFlow()
    }
}

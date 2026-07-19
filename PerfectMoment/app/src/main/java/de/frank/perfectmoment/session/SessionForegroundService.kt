package de.frank.perfectmoment.session

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import android.os.PowerManager
import de.frank.perfectmoment.PerfectMomentApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SessionForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controller: SessionController
        get() = (application as PerfectMomentApplication).container.sessionController
    private lateinit var audioManager: AudioManager
    private lateinit var mediaSession: MediaSession
    private lateinit var focusRequest: AudioFocusRequest
    private var stateJob: Job? = null
    private var resumeAfterFocusGain = false
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        SessionNotification.createChannel(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setOnAudioFocusChangeListener(::onAudioFocusChanged)
            .build()
        mediaSession = MediaSession(this, "PerfectMomentSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = play()
                override fun onPause() = pause()
                override fun onStop() = stopSession()
            })
            isActive = true
        }
        startForeground(
            SessionNotification.NOTIFICATION_ID,
            SessionNotification.build(this, mediaSession, controller.runtime.value, controller.state.value),
        )
        acquireWakeLock()
        stateJob = serviceScope.launch {
            combine(controller.runtime, controller.state) { runtime, state -> runtime to state }
                .collect { (runtime, state) -> updateNotification(runtime, state) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> if (controller.state.value?.speakerOn == true) pause() else play()
            ACTION_STOP -> stopSession()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            stateJob?.cancel()
            audioManager.abandonAudioFocusRequest(focusRequest)
            mediaSession.isActive = false
            mediaSession.release()
            serviceScope.cancel()
        } finally {
            try {
                wakeLock?.takeIf { it.isHeld }?.release()
                wakeLock = null
            } finally {
                super.onDestroy()
            }
        }
    }

    private fun play() {
        if (audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            resumeAfterFocusGain = false
            controller.setSpeakerOn(true)
        }
    }

    private fun pause() {
        resumeAfterFocusGain = false
        controller.setSpeakerOn(false)
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    private fun stopSession() {
        resumeAfterFocusGain = false
        controller.stopAndClear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onAudioFocusChanged(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            -> {
                resumeAfterFocusGain = controller.state.value?.speakerOn == true
                controller.setSpeakerOn(false)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeAfterFocusGain = false
                controller.setSpeakerOn(false)
            }
            AudioManager.AUDIOFOCUS_GAIN -> if (resumeAfterFocusGain) {
                resumeAfterFocusGain = false
                controller.setSpeakerOn(true)
            }
        }
    }

    private fun updateNotification(runtime: SessionRuntime?, state: SessionState?) {
        val playbackState = when {
            state?.phase == Phase.ENDED -> PlaybackState.STATE_STOPPED
            state?.speakerOn == true -> PlaybackState.STATE_PLAYING
            else -> PlaybackState.STATE_PAUSED
        }
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP,
                )
                .setState(playbackState, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build(),
        )
        getSystemService(NotificationManager::class.java).notify(
            SessionNotification.NOTIFICATION_ID,
            SessionNotification.build(this, mediaSession, runtime, state),
        )
    }

    private fun acquireWakeLock() {
        val sessionDurationMs = controller.runtime.value?.config?.durationMs
            ?.coerceAtMost(MAX_SESSION_DURATION_MS)
            ?: MAX_SESSION_DURATION_MS
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:session")
            .apply {
                setReferenceCounted(false)
                acquire(sessionDurationMs + WAKE_LOCK_RESERVE_MS)
            }
    }

    companion object {
        const val ACTION_START = "de.frank.perfectmoment.session.START"
        const val ACTION_TOGGLE = "de.frank.perfectmoment.session.TOGGLE"
        const val ACTION_STOP = "de.frank.perfectmoment.session.STOP"
        private const val MAX_SESSION_DURATION_MS = 120 * 60_000L
        private const val WAKE_LOCK_RESERVE_MS = 5 * 60_000L
    }
}

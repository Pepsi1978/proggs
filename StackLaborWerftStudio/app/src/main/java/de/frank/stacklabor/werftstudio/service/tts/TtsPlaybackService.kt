package de.frank.stacklabor.werftstudio.service.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerNotificationManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TtsPlaybackStateBus {
    private val mutableState = MutableStateFlow<TtsPlaybackState>(TtsPlaybackState.Idle)
    val state: StateFlow<TtsPlaybackState> = mutableState.asStateFlow()
    internal fun update(value: TtsPlaybackState) { mutableState.value = value }
}

object TtsServiceIntegration {
    @Volatile
    var engine: TtsEngine? = null

    @Volatile
    var contentIntentFactory: ((Context) -> PendingIntent?)? = null

    internal fun requireEngine(): TtsEngine = checkNotNull(engine) {
        "TtsServiceIntegration.engine muss in Application.onCreate gesetzt werden."
    }
}

@androidx.media3.common.util.UnstableApi
class TtsPlaybackService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private lateinit var playerNotificationManager: PlayerNotificationManager
    private var playbackJob: Job? = null
    private var paragraphIndex = 0
    private var paragraphCount = 0

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true,
            )
        }
        playerNotificationManager = PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
            .setSmallIconResourceId(android.R.drawable.ic_lock_silent_mode_off)
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence = "Auswertung wird vorgelesen"
                override fun createCurrentContentIntent(player: Player): PendingIntent? =
                    TtsServiceIntegration.contentIntentFactory?.invoke(this@TtsPlaybackService)
                override fun getCurrentContentText(player: Player): CharSequence =
                    if (paragraphCount == 0) "Audio wird vorbereitet" else "Absatz ${paragraphIndex + 1} von $paragraphCount"
                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback,
                ): android.graphics.Bitmap? = null
            })
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopPlaybackAndService()
                }

                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean,
                ) {
                    if (ongoing) startForegroundCompat(notification) else stopForeground(STOP_FOREGROUND_DETACH)
                }
            })
            .build()
            .apply {
                setUsePlayPauseActions(true)
                setUseStopAction(true)
                setPlayer(player)
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat(preparingNotification())
        when (intent?.action) {
            ACTION_STOP -> stopPlaybackAndService()
            ACTION_PAUSE -> pausePlayback()
            ACTION_RESUME -> resumePlayback()
            ACTION_PLAY -> startPlayback(intent)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        playbackJob?.cancel()
        playerNotificationManager.setPlayer(null)
        player.release()
        serviceScope.cancel()
        if (TtsPlaybackStateBus.state.value !is TtsPlaybackState.Error) {
            TtsPlaybackStateBus.update(TtsPlaybackState.Idle)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startPlayback(intent: Intent) {
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        val provider = intent.getStringExtra(EXTRA_PROVIDER)
            ?.let { runCatching { TtsProviderId.valueOf(it) }.getOrNull() }
            ?: TtsProviderId.EDGE
        val configuration = TtsConfiguration(
            provider = provider,
            voiceId = intent.getStringExtra(EXTRA_VOICE).orEmpty(),
            speechRate = intent.getFloatExtra(EXTRA_RATE, 1f).coerceIn(0.7f, 1.3f),
            paragraphPauseMillis = intent.getLongExtra(EXTRA_PAUSE, 450L).coerceIn(0L, 10_000L),
            autoStopMillis = intent.getLongExtra(EXTRA_AUTO_STOP, 30 * 60_000L).coerceAtLeast(0L),
        )
        playbackJob?.cancel()
        playbackJob = serviceScope.launch {
            try {
                val paragraphs = TtsText.paragraphs(text)
                if (paragraphs.isEmpty()) throw TtsException("Die Auswertung enthält keinen vorlesbaren Fließtext.")
                paragraphCount = paragraphs.size
                val start = intent.getIntExtra(EXTRA_START_PARAGRAPH, 0).coerceIn(0, paragraphs.lastIndex)
                val engine = TtsServiceIntegration.requireEngine()
                val playAll: suspend () -> Unit = {
                    // Der naechste Absatz wird schon erzeugt, waehrend der aktuelle laeuft. Ohne das
                    // entsteht vor jedem Absatz eine hoerbare Pause, weil erst dann synthetisiert wird.
                    var naechster: Deferred<SynthesizedAudio>? = null
                    for (index in start..paragraphs.lastIndex) {
                        paragraphIndex = index
                        TtsPlaybackStateBus.update(TtsPlaybackState.Preparing(index, paragraphCount))
                        val audio = (naechster ?: async { engine.synthesizeParagraph(paragraphs[index], configuration) }).await()
                        naechster = if (index < paragraphs.lastIndex) {
                            async { engine.synthesizeParagraph(paragraphs[index + 1], configuration) }
                        } else null
                        playAndAwait(Uri.fromFile(audio.file), index, paragraphCount)
                        if (index < paragraphs.lastIndex && configuration.paragraphPauseMillis > 0) {
                            delay(configuration.paragraphPauseMillis)
                        }
                    }
                }
                if (configuration.autoStopMillis > 0L) {
                    withTimeoutOrNull(configuration.autoStopMillis) {
                        playAll()
                    }
                } else {
                    playAll()
                }
                stopPlaybackAndService()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                TtsPlaybackStateBus.update(
                    TtsPlaybackState.Error(error.message ?: "Vorlesen ist fehlgeschlagen."),
                )
                player.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private suspend fun playAndAwait(uri: Uri, index: Int, count: Int) {
        suspendCancellableCoroutine { continuation ->
            val terminal = AtomicBoolean(false)
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY && player.playWhenReady) {
                        TtsPlaybackStateBus.update(TtsPlaybackState.Playing(index, count))
                    }
                    if (playbackState == Player.STATE_ENDED && terminal.compareAndSet(false, true)) {
                        player.removeListener(this)
                        continuation.resume(Unit) { _, _, _ -> }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (terminal.compareAndSet(false, true)) {
                        player.removeListener(this)
                        continuation.resumeWithException(TtsException("Media3 konnte die Audiodatei nicht abspielen.", error))
                    }
                }
            }
            player.addListener(listener)
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.play()
            continuation.invokeOnCancellation {
                if (terminal.compareAndSet(false, true)) player.removeListener(listener)
                player.stop()
            }
        }
    }

    private fun pausePlayback() {
        if (!player.isPlaying) return
        player.pause()
        TtsPlaybackStateBus.update(TtsPlaybackState.Paused(paragraphIndex, paragraphCount))
    }

    private fun resumePlayback() {
        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) return
        player.play()
        TtsPlaybackStateBus.update(TtsPlaybackState.Playing(paragraphIndex, paragraphCount))
    }

    private fun stopPlaybackAndService() {
        playbackJob?.cancel()
        playbackJob = null
        player.stop()
        if (TtsPlaybackStateBus.state.value !is TtsPlaybackState.Error) {
            TtsPlaybackStateBus.update(TtsPlaybackState.Idle)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Vorlesen", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Wiedergabesteuerung für vorgelesene Auswertungen"
                setShowBadge(false)
            },
        )
    }

    private fun preparingNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TtsPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentTitle("Auswertung wird vorgelesen")
            .setContentText("Audio wird vorbereitet")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, "Stopp", stopIntent)
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "tts_playback"
        const val NOTIFICATION_ID = 7412
        const val ACTION_PLAY = "de.frank.stacklabor.werftstudio.tts.PLAY"
        const val ACTION_PAUSE = "de.frank.stacklabor.werftstudio.tts.PAUSE"
        const val ACTION_RESUME = "de.frank.stacklabor.werftstudio.tts.RESUME"
        const val ACTION_STOP = "de.frank.stacklabor.werftstudio.tts.STOP"
        private const val EXTRA_TEXT = "text"
        private const val EXTRA_PROVIDER = "provider"
        private const val EXTRA_VOICE = "voice"
        private const val EXTRA_RATE = "rate"
        private const val EXTRA_PAUSE = "pause"
        private const val EXTRA_AUTO_STOP = "auto_stop"
        private const val EXTRA_START_PARAGRAPH = "start_paragraph"

        fun play(context: Context, narrative: String, configuration: TtsConfiguration, startParagraph: Int = 0) {
            val intent = Intent(context, TtsPlaybackService::class.java)
                .setAction(ACTION_PLAY)
                .putExtra(EXTRA_TEXT, narrative)
                .putExtra(EXTRA_PROVIDER, configuration.provider.name)
                .putExtra(EXTRA_VOICE, configuration.voiceId)
                .putExtra(EXTRA_RATE, configuration.speechRate)
                .putExtra(EXTRA_PAUSE, configuration.paragraphPauseMillis)
                .putExtra(EXTRA_AUTO_STOP, configuration.autoStopMillis)
                .putExtra(EXTRA_START_PARAGRAPH, startParagraph)
            ContextCompat.startForegroundService(context.applicationContext, intent)
        }

        fun pause(context: Context) = send(context, ACTION_PAUSE)
        fun resume(context: Context) = send(context, ACTION_RESUME)
        fun stop(context: Context) = send(context, ACTION_STOP)

        private fun send(context: Context, action: String) {
            context.startService(Intent(context, TtsPlaybackService::class.java).setAction(action))
        }
    }
}

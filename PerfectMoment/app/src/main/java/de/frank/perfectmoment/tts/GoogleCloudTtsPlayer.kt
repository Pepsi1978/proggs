package de.frank.perfectmoment.tts

import de.frank.perfectmoment.network.shutdownSafely
import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class GoogleCloudTtsPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val lock = Any()
    private val generation = AtomicLong(0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var activeCall: Call? = null
    private var activeJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var activeFile: File? = null

    fun speak(
        text: String,
        apiKey: String,
        voiceName: String,
        onPlaybackStart: () -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        stop()
        val requestGeneration = generation.incrementAndGet()
        val callbacks = PlaybackCallbacks(onPlaybackStart, onComplete, onError)
        if (text.isBlank()) {
            finishComplete(requestGeneration, callbacks)
            return
        }
        if (apiKey.isBlank()) {
            finishError(
                requestGeneration,
                callbacks,
                IllegalStateException("Google Cloud TTS ist nicht konfiguriert."),
            )
            return
        }

        val job = scope.launch(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("input", JSONObject().put("text", text))
                    put(
                        "voice",
                        JSONObject()
                            .put("languageCode", "de-DE")
                            .put("name", voiceName),
                    )
                    put("audioConfig", JSONObject().put("audioEncoding", "MP3"))
                }
                val url = GOOGLE_TTS_URL.toHttpUrl().newBuilder()
                    .addQueryParameter("key", apiKey)
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                val call = client.newCall(request)
                synchronized(lock) {
                    if (requestGeneration != generation.get() || callbacks.terminal.get()) {
                        call.cancel()
                        return@launch
                    }
                    activeCall = call
                }

                val audioBytes = call.execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw TtsPlaybackException(
                            "Google TTS error ${response.code}: ${responseBody.take(500)}",
                        )
                    }
                    val audioContent = JSONObject(responseBody).optString("audioContent")
                    if (audioContent.isBlank()) {
                        throw TtsPlaybackException("Google TTS returned no audio data.")
                    }
                    Base64.decode(audioContent, Base64.DEFAULT)
                }
                if (requestGeneration != generation.get()) return@launch

                val file = File(
                    appContext.cacheDir,
                    "google_tts_${requestGeneration}_${UUID.randomUUID()}.mp3",
                )
                file.writeBytes(audioBytes)
                synchronized(lock) {
                    if (requestGeneration != generation.get()) {
                        file.delete()
                        return@launch
                    }
                    activeCall = null
                    activeFile = file
                }
                scope.launch { playFile(file, requestGeneration, callbacks) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (requestGeneration == generation.get()) {
                    logger.warning("Google TTS failed: ${error.message}")
                    finishError(requestGeneration, callbacks, error)
                }
            }
        }
        synchronized(lock) {
            if (requestGeneration == generation.get() && !callbacks.terminal.get()) {
                activeJob = job
            } else {
                job.cancel()
            }
        }
    }

    private fun playFile(
        file: File,
        requestGeneration: Long,
        callbacks: PlaybackCallbacks,
    ) {
        if (requestGeneration != generation.get() || callbacks.terminal.get()) {
            file.delete()
            return
        }

        val player = MediaPlayer()
        try {
            synchronized(lock) {
                if (requestGeneration != generation.get()) {
                    player.release()
                    file.delete()
                    return
                }
                mediaPlayer = player
                activeJob = null
                player.setDataSource(file.absolutePath)
                player.setOnCompletionListener {
                    finishComplete(requestGeneration, callbacks)
                }
                player.setOnErrorListener { _, what, extra ->
                    finishError(
                        requestGeneration,
                        callbacks,
                        TtsPlaybackException("Google MediaPlayer error $what/$extra"),
                    )
                    true
                }
                player.prepare()
                player.start()
            }
            signalStart(requestGeneration, callbacks)
        } catch (error: Exception) {
            finishError(requestGeneration, callbacks, error)
        }
    }

    fun stop() {
        generation.incrementAndGet()
        cleanupCurrent()
    }

    fun shutdown() {
        stop()
        scope.cancel()
        client.shutdownSafely(logger)
    }

    private fun signalStart(requestGeneration: Long, callbacks: PlaybackCallbacks) {
        if (requestGeneration != generation.get() || callbacks.terminal.get()) return
        if (callbacks.started.compareAndSet(false, true)) callbacks.onStart()
    }

    private fun finishComplete(requestGeneration: Long, callbacks: PlaybackCallbacks) {
        finish(requestGeneration, callbacks) { callbacks.onComplete() }
    }

    private fun finishError(
        requestGeneration: Long,
        callbacks: PlaybackCallbacks,
        error: Exception,
    ) {
        finish(requestGeneration, callbacks) { callbacks.onError(error) }
    }

    private fun finish(
        requestGeneration: Long,
        callbacks: PlaybackCallbacks,
        terminalCallback: () -> Unit,
    ) {
        if (requestGeneration != generation.get() ||
            !callbacks.terminal.compareAndSet(false, true)
        ) {
            return
        }
        cleanupCurrent()
        scope.launch {
            if (requestGeneration == generation.get()) terminalCallback()
        }
    }

    private fun cleanupCurrent() {
        val call: Call?
        val job: Job?
        val player: MediaPlayer?
        val file: File?
        synchronized(lock) {
            call = activeCall
            job = activeJob
            player = mediaPlayer
            file = activeFile
            activeCall = null
            activeJob = null
            mediaPlayer = null
            activeFile = null
        }
        call?.cancel()
        job?.cancel()
        try {
            player?.stop()
        } catch (_: Exception) {
            // MediaPlayer can already be completed or in an error state.
        }
        player?.release()
        file?.delete()
    }

    private class PlaybackCallbacks(
        val onStart: () -> Unit,
        val onComplete: () -> Unit,
        val onError: (Exception) -> Unit,
    ) {
        val started = AtomicBoolean(false)
        val terminal = AtomicBoolean(false)
    }

    companion object {
        private const val GOOGLE_TTS_URL =
            "https://texttospeech.googleapis.com/v1/text:synthesize"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val logger = Logger.getLogger(GoogleCloudTtsPlayer::class.java.name)
    }
}

class TtsPlaybackException(message: String, cause: Throwable? = null) : Exception(message, cause)

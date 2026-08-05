package com.entropyjournal.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Speaks through the user's own cloned voice, hosted by Alibaba Model Studio.
 *
 * Unlike the other providers this one needs two calls: the synthesis request answers with a
 * link to the finished audio, which is then downloaded. Speaking rate and pitch are absent on
 * purpose — the model varies emphasis by itself, so every repetition sounds newly spoken.
 */
class QwenTtsPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val lock = Any()
    private val generation = AtomicLong(0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private var activeCall: Call? = null
    private var activeJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var activeFile: File? = null

    fun speak(
        text: String,
        rawApiKey: String,
        rawVoiceId: String,
        onPlaybackStart: (() -> Unit)? = null,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        // Second line of defence: values stored before the input was sanitised would still
        // carry blanks and fail with 401, so they are cleaned here as well.
        val apiKey = rawApiKey.filterNot(Char::isWhitespace)
        val voiceId = rawVoiceId.filterNot(Char::isWhitespace)
        stop()
        val requestGeneration = generation.incrementAndGet()
        val callbacks = PlaybackCallbacks(onPlaybackStart, onComplete, onError)
        if (text.isBlank()) {
            finishComplete(requestGeneration, callbacks)
            return
        }
        if (apiKey.isBlank() || voiceId.isBlank()) {
            Log.e(TAG, "abort: key or voice id missing")
            finishError(
                requestGeneration,
                callbacks,
                IllegalStateException("Die eigene Stimme ist nicht konfiguriert."),
            )
            return
        }

        val job = scope.launch(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("model", MODEL)
                    put(
                        "input",
                        JSONObject()
                            .put("text", text)
                            .put("voice", voiceId)
                            .put("language_type", "German"),
                    )
                }
                val request = Request.Builder()
                    .url(SYNTHESIS_URL)
                    .addHeader("Authorization", "Bearer $apiKey")
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

                val audioUrl = call.execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw Exception("Qwen TTS error ${response.code}: ${responseBody.take(500)}")
                    }
                    val url = JSONObject(responseBody)
                        .optJSONObject("output")
                        ?.optJSONObject("audio")
                        ?.optString("url")
                        .orEmpty()
                    if (url.isBlank()) {
                        throw Exception("Qwen TTS returned no audio link.")
                    }
                    // The link arrives as plain http, which Android blocks by default.
                    if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url
                }
                if (requestGeneration != generation.get()) return@launch

                val downloadCall = client.newCall(Request.Builder().url(audioUrl).build())
                synchronized(lock) {
                    if (requestGeneration != generation.get() || callbacks.terminal.get()) {
                        downloadCall.cancel()
                        return@launch
                    }
                    activeCall = downloadCall
                }
                val audioBytes = downloadCall.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Qwen TTS download failed: ${response.code}")
                    }
                    response.body?.bytes() ?: throw Exception("Qwen TTS audio was empty.")
                }
                if (requestGeneration != generation.get()) return@launch

                val file = File(
                    appContext.cacheDir,
                    "qwen_tts_${requestGeneration}_${UUID.randomUUID()}.wav",
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
                Log.e(TAG, "failed: ${error.javaClass.simpleName}: ${error.message}", error)
                if (requestGeneration == generation.get()) {
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
                        Exception("Qwen MediaPlayer error $what/$extra"),
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
    }

    private fun signalStart(requestGeneration: Long, callbacks: PlaybackCallbacks) {
        if (requestGeneration != generation.get() || callbacks.terminal.get()) return
        if (callbacks.started.compareAndSet(false, true)) callbacks.onStart?.invoke()
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
        val onStart: (() -> Unit)?,
        val onComplete: () -> Unit,
        val onError: (Exception) -> Unit,
    ) {
        val started = AtomicBoolean(false)
        val terminal = AtomicBoolean(false)
    }

    companion object {
        private const val SYNTHESIS_URL =
            "https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"

        /** Cloning and synthesis must use the same model, otherwise the voice id is rejected. */
        const val MODEL = "qwen3-tts-vc-2026-01-22"

        private const val TAG = "QwenTts"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

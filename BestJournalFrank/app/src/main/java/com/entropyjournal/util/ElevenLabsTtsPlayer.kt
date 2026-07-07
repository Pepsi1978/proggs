package com.entropyjournal.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Text-to-Speech player using ElevenLabs REST API.
 * Falls back via onError callback if the API call fails (quota, network, auth).
 */
class ElevenLabsTtsPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "ElevenLabsTTS"
    }

    /**
     * Speaks the given text using ElevenLabs API.
     * @param text Text to speak
     * @param apiKey ElevenLabs API key
     * @param voiceId ElevenLabs voice ID
     * @param onPlaybackStart Called when audio playback begins
     * @param onComplete Called when playback finishes
     * @param onError Called if the API call fails — caller should fallback to Edge TTS
     */
    fun speak(
        text: String,
        apiKey: String,
        voiceId: String,
        onPlaybackStart: (() -> Unit)? = null,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        stop()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val audioFile = fetchAudio(text, apiKey, voiceId)
                withContext(Dispatchers.Main) {
                    playFile(audioFile, onPlaybackStart, onComplete)
                }
            } catch (e: Exception) {
                Log.e(TAG, "ElevenLabs TTS failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    private fun fetchAudio(text: String, apiKey: String, voiceId: String): File {
        val url = "${Constants.ELEVENLABS_BASE_URL}/text-to-speech/$voiceId"

        // Use Flash v2.5 model for lower latency and half credit cost
        val jsonBody = """
            {
                "text": ${org.json.JSONObject.quote(text)},
                "model_id": "eleven_flash_v2_5",
                "voice_settings": {
                    "stability": 0.5,
                    "similarity_boost": 0.75
                }
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("xi-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "audio/mpeg")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            val code = response.code
            response.close()
            throw Exception("ElevenLabs API error $code: $errorBody")
        }

        val audioFile = File(context.cacheDir, "elevenlabs_tts.mp3")
        FileOutputStream(audioFile).use { out ->
            response.body?.byteStream()?.copyTo(out)
        }
        response.close()

        if (audioFile.length() == 0L) {
            audioFile.delete()
            throw Exception("ElevenLabs returned empty audio")
        }

        return audioFile
    }

    private fun playFile(
        file: File,
        onPlaybackStart: (() -> Unit)?,
        onComplete: () -> Unit,
    ) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                onComplete()
                file.delete()
            }
            setOnErrorListener { _, _, _ ->
                onComplete()
                file.delete()
                true
            }
            prepare()
            start()
            onPlaybackStart?.invoke()
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun shutdown() {
        stop()
    }
}

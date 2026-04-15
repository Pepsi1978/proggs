package com.entropyjournal.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Google Cloud Text-to-Speech player using Chirp 3 HD voices.
 * 1 million characters/month free, then $30/million characters.
 */
class GoogleCloudTtsPlayer(private val context: Context) {

    companion object {
        private const val TAG = "GoogleCloudTts"
    }

    private var mediaPlayer: MediaPlayer? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun speak(
        text: String,
        apiKey: String,
        voiceName: String,
        onPlaybackStart: (() -> Unit)? = null,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        stop()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = JSONObject().apply {
                    put("input", JSONObject().put("text", text))
                    put(
                        "voice",
                        JSONObject().apply {
                            put("languageCode", "de-DE")
                            put("name", voiceName)
                        },
                    )
                    put("audioConfig", JSONObject().put("audioEncoding", "MP3"))
                }

                val request = Request.Builder()
                    .url("${Constants.GOOGLE_TTS_BASE_URL}?key=$apiKey")
                    .post(
                        body.toString()
                            .toRequestBody("application/json; charset=utf-8".toMediaType()),
                    )
                    .build()

                Log.d(TAG, "Requesting TTS for ${text.length} chars, voice: $voiceName")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    throw Exception("Google TTS error ${response.code}: $responseBody")
                }

                val json = JSONObject(responseBody)
                val audioContent = json.getString("audioContent")
                val audioBytes = Base64.decode(audioContent, Base64.DEFAULT)

                val audioFile = File(context.cacheDir, "google_tts.mp3")
                audioFile.writeBytes(audioBytes)

                CoroutineScope(Dispatchers.Main).launch {
                    playFile(audioFile, onPlaybackStart, onComplete)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google Cloud TTS failed: ${e.message}", e)
                CoroutineScope(Dispatchers.Main).launch { onError(e) }
            }
        }
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

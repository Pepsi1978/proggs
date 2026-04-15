package com.entropyjournal.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Offline TTS using sherpa-onnx with the Piper Thorsten High German voice model.
 * Generates audio entirely on-device — no internet required.
 */
class PiperTtsPlayer(private val context: Context) {

    companion object {
        private const val TAG = "PiperTtsPlayer"
    }

    private var tts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null
    private var currentJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private fun getModelDir(): File =
        File(context.filesDir, Constants.PIPER_MODEL_DIR)

    private fun isModelExtracted(): Boolean {
        val dir = getModelDir()
        return dir.exists() &&
            File(dir, Constants.PIPER_MODEL_FILE).exists() &&
            File(dir, Constants.PIPER_TOKENS_FILE).exists() &&
            File(dir, Constants.PIPER_DATA_DIR).isDirectory
    }

    private fun extractModelFromAssets() {
        val destDir = getModelDir()
        if (isModelExtracted()) return

        Log.d(TAG, "Extracting Piper TTS model from assets...")
        destDir.mkdirs()

        val assetDir = Constants.PIPER_MODEL_DIR
        copyAssetDir(assetDir, destDir)
        Log.d(TAG, "Piper TTS model extracted to ${destDir.absolutePath}")
    }

    private fun copyAssetDir(assetPath: String, destDir: File) {
        val assets = context.assets
        val entries = assets.list(assetPath) ?: return

        if (entries.isEmpty()) {
            // It's a file
            assets.open(assetPath).use { input ->
                File(destDir, "").outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            destDir.mkdirs()
            for (entry in entries) {
                val childAsset = "$assetPath/$entry"
                val childDest = File(destDir, entry)
                val childEntries = assets.list(childAsset)
                if (childEntries != null && childEntries.isNotEmpty()) {
                    copyAssetDir(childAsset, childDest)
                } else {
                    assets.open(childAsset).use { input ->
                        childDest.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    private fun ensureTtsInitialized(): OfflineTts {
        tts?.let { return it }

        extractModelFromAssets()
        val modelDir = getModelDir().absolutePath

        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = "$modelDir/${Constants.PIPER_MODEL_FILE}",
                    tokens = "$modelDir/${Constants.PIPER_TOKENS_FILE}",
                    dataDir = "$modelDir/${Constants.PIPER_DATA_DIR}",
                    lengthScale = 1.0f,
                ),
                numThreads = 2,
                provider = "cpu",
            ),
            maxNumSentences = 2,
        )

        return OfflineTts(config = config).also {
            tts = it
            Log.d(TAG, "Piper TTS initialized (sample rate: ${it.sampleRate()})")
        }
    }

    fun speak(
        text: String,
        onPlaybackStart: (() -> Unit)? = null,
        onComplete: () -> Unit,
    ) {
        stop()
        currentJob = scope.launch {
            try {
                val engine = ensureTtsInitialized()
                Log.d(TAG, "Generating audio for ${text.length} chars...")

                val audio = engine.generate(text = text, sid = 0, speed = 1.0f)
                val samples = audio.samples
                val sampleRate = audio.sampleRate

                if (samples.isEmpty()) {
                    Log.w(TAG, "Generated empty audio")
                    withContext(Dispatchers.Main) { onComplete() }
                    return@launch
                }

                Log.d(TAG, "Audio generated: ${samples.size} samples at ${sampleRate}Hz")

                // Convert float samples to 16-bit PCM
                val pcm = ShortArray(samples.size)
                for (i in samples.indices) {
                    val clamped = samples[i].coerceIn(-1.0f, 1.0f)
                    pcm[i] = (clamped * 32767).toInt().toShort()
                }

                withContext(Dispatchers.Main) {
                    playPcm(pcm, sampleRate, onPlaybackStart, onComplete)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Piper TTS failed: ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }

    private fun playPcm(
        pcm: ShortArray,
        sampleRate: Int,
        onPlaybackStart: (() -> Unit)?,
        onComplete: () -> Unit,
    ) {
        stop()
        val bufferSize = pcm.size * 2 // 2 bytes per short

        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            bufferSize,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        )

        track.write(pcm, 0, pcm.size)
        track.setNotificationMarkerPosition(pcm.size)
        track.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) {
                    t?.release()
                    audioTrack = null
                    onComplete()
                }
                override fun onPeriodicNotification(t: AudioTrack?) {}
            },
        )

        audioTrack = track
        track.play()
        onPlaybackStart?.invoke()
    }

    fun stop() {
        currentJob?.cancel()
        currentJob = null
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun shutdown() {
        stop()
        try { tts?.release() } catch (_: Exception) {}
        tts = null
    }
}

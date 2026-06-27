package de.frank.cortex.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import de.frank.cortex.observability.CortexLog
import kotlinx.coroutines.*

class PcmPlayer {

    private var player: AudioTrack? = null
    private var playJob: Job? = null
    private var isPlaying = false

    companion object {
        const val SAMPLE_RATE = 24000
        const val CHANNEL = AudioFormat.CHANNEL_OUT_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    fun play(scope: CoroutineScope, pcmData: ByteArray) {
        stop()

        val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)

        player = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL)
                    .setEncoding(ENCODING)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        isPlaying = true
        player?.play()

        playJob = scope.launch(Dispatchers.IO) {
            try {
                var offset = 0
                val chunkSize = bufferSize.coerceAtLeast(4096)
                while (isActive && offset < pcmData.size && isPlaying) {
                    val remaining = pcmData.size - offset
                    val writeSize = minOf(chunkSize, remaining)
                    player?.write(pcmData, offset, writeSize)
                    offset += writeSize
                }
            } catch (e: Exception) {
                CortexLog.error("PcmPlayer", "play", "Abspiel-Fehler: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    stop()
                }
            }
        }

        CortexLog.info("PcmPlayer", "play", "Wiedergabe gestartet", mapOf("pcm_bytes" to pcmData.size))
    }

    fun stop() {
        isPlaying = false
        playJob?.cancel()
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {}
        player = null
    }

    fun isPlaying(): Boolean = isPlaying
}

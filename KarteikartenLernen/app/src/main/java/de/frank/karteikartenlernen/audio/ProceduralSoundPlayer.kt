package de.frank.karteikartenlernen.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import de.frank.karteikartenlernen.model.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

enum class SoundEffect { FLIP, KNOWN, UNKNOWN, TRANSITION, DONE }

class ProceduralSoundPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun play(effect: SoundEffect, settings: AppSettings) {
        if (!settings.sounds || !enabled(effect, settings)) return
        scope.launch {
            val duration = when (effect) {
                SoundEffect.FLIP -> 0.19
                SoundEffect.KNOWN -> 0.5
                SoundEffect.UNKNOWN -> 0.33
                SoundEffect.TRANSITION -> 0.3
                SoundEffect.DONE -> 0.95
            }
            val data = synthesize(effect, duration, settings.volume)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(data.size * 2)
                .build()
            track.write(data, 0, data.size)
            track.play()
            delay((duration * 1000).toLong() + 80)
            track.release()
        }
    }

    private fun enabled(effect: SoundEffect, s: AppSettings) = when (effect) {
        SoundEffect.FLIP -> s.soundFlip
        SoundEffect.KNOWN -> s.soundKnown
        SoundEffect.UNKNOWN -> s.soundUnknown
        SoundEffect.TRANSITION -> s.soundTransition
        SoundEffect.DONE -> s.soundDone
    }

    private fun synthesize(effect: SoundEffect, duration: Double, volume: Float): ShortArray {
        val count = (SAMPLE_RATE * duration).toInt()
        return ShortArray(count) { index ->
            val t = index.toDouble() / SAMPLE_RATE
            val raw = when (effect) {
                SoundEffect.FLIP -> toneSweep(t, 0.13, 150.0, 90.0, 0.16) + noise(t, 0.14, 0.09)
                SoundEffect.KNOWN -> tone(t, 0.0, 0.14, 587.0, 0.16) + tone(t, 0.09, 0.22, 880.0, 0.16) + tone(t, 0.17, 0.3, 1175.0, 0.09)
                SoundEffect.UNKNOWN -> toneSweep(t, 0.28, 320.0, 190.0, 0.18) + noise(t, 0.12, 0.05)
                SoundEffect.TRANSITION -> toneSweep(t, 0.2, 420.0, 700.0, 0.05) + noise(t, 0.26, 0.035)
                SoundEffect.DONE -> listOf(523.0, 659.0, 784.0, 1046.0, 1319.0).mapIndexed { i, f -> tone(t, i * 0.1, 0.5, f, 0.13) }.sum()
            }
            (raw.coerceIn(-1.0, 1.0) * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun tone(t: Double, delay: Double, duration: Double, frequency: Double, gain: Double): Double {
        val local = t - delay
        if (local !in 0.0..duration) return 0.0
        return sin(2 * PI * frequency * local) * envelope(local, duration) * gain
    }

    private fun toneSweep(t: Double, duration: Double, start: Double, end: Double, gain: Double): Double {
        if (t !in 0.0..duration) return 0.0
        val frequency = start * (end / start).pow(t / duration)
        return sin(2 * PI * frequency * t) * envelope(t, duration) * gain
    }

    private fun noise(t: Double, duration: Double, gain: Double): Double =
        if (t in 0.0..duration) (Random.nextDouble() * 2 - 1) * envelope(t, duration) * gain else 0.0

    private fun envelope(t: Double, duration: Double): Double {
        val attack = (t / 0.012).coerceAtMost(1.0)
        return attack * exp(-4.7 * t / duration)
    }

    companion object { private const val SAMPLE_RATE = 44_100 }
}

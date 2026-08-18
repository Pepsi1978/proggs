package de.frank.gedankenspeicher.tts

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import java.util.WeakHashMap
import java.util.logging.Logger

/**
 * Makes every spoken line come out at full strength.
 *
 * Three things were missing, and each one cost volume:
 *  - no [AudioAttributes], so playback never declared itself as speech on the media
 *    stream the volume keys control,
 *  - no explicit channel volume,
 *  - no gain at all - the raw voice files from Edge, Google and Qwen sit far below
 *    what a phone speaker needs in a quiet room.
 *
 * Every player in this package MUST route its [MediaPlayer] through [boost] and
 * [release]. A new voice provider that skips this is quiet by omission again.
 */
object SpeechLoudness {
    /**
     * +12 dB. The enhancer compresses before it amplifies, so speech gets loud
     * without the crackle that raw amplification would produce.
     */
    const val TARGET_GAIN_MILLIBEL = 1200

    private val logger = Logger.getLogger("SpeechLoudness")
    private val enhancers = WeakHashMap<MediaPlayer, LoudnessEnhancer>()

    /** Speech on the media stream: volume keys control it and ducking behaves. */
    val attributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    /** Call directly after [MediaPlayer.prepare] and before start(). */
    fun boost(player: MediaPlayer) {
        try {
            player.setVolume(1f, 1f)
        } catch (error: Exception) {
            logger.warning("Playback volume could not be set: ${error.message}")
        }
        try {
            val enhancer = LoudnessEnhancer(player.audioSessionId)
            enhancer.setTargetGain(TARGET_GAIN_MILLIBEL)
            enhancer.enabled = true
            synchronized(enhancers) { enhancers[player] = enhancer }
        } catch (error: Exception) {
            // A few devices and emulator images ship without the effect. Speech must
            // still play: quieter is bad, silent would be worse.
            logger.warning("Loudness enhancer unavailable, playing unboosted: ${error.message}")
        }
    }

    /** Call before [MediaPlayer.release] - the effect outlives the player otherwise. */
    fun release(player: MediaPlayer?) {
        val enhancer = synchronized(enhancers) { enhancers.remove(player) } ?: return
        try {
            enhancer.enabled = false
            enhancer.release()
        } catch (error: Exception) {
            logger.warning("Loudness enhancer could not be released: ${error.message}")
        }
    }
}

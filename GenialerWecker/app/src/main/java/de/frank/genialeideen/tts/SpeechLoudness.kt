package de.frank.genialeideen.tts

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.util.logging.Logger

/**
 * Plays every spoken line unaltered at full channel volume.
 *
 * Deliberately without audio effects: the former LoudnessEnhancer compressed and amplified the voice, and
 * Frank wants speech without any sound effect. Loudness comes from the stream volume alone.
 *
 * Every player in this package routes its [MediaPlayer] through [boost] and [release].
 */
object SpeechLoudness {
    private val logger = Logger.getLogger("SpeechLoudness")

    /** Speech on the media stream: volume keys control it and ducking behaves. */
    val attributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    /** Call directly after [MediaPlayer.prepare] and before start(): full channel volume, no effect. */
    fun boost(player: MediaPlayer) {
        try {
            player.setVolume(1f, 1f)
        } catch (error: Exception) {
            logger.warning("Playback volume could not be set: ${error.message}")
        }
    }

    /** Kept for the players' release path; there is no effect left to free. */
    @Suppress("UNUSED_PARAMETER")
    fun release(player: MediaPlayer?) = Unit
}

package de.frank.wecker

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.PowerManager
import android.os.Handler
import android.os.Looper

/** Ein aktiver und ein vorgeladener Absatz; bewusste Pausen gelten unabhängig vom Sprechtempo. */
class AlarmAudioQueue(
    private val context: Context,
    private val clips: List<AlarmClip>,
    private val onPlaying: (AlarmClip) -> Unit,
    private val onFailure: (Exception) -> Unit,
) : AutoCloseable {
    private class Slot(val index: Int, val player: MediaPlayer, var ready: Boolean = false)
    private var current: Slot? = null
    private var queued: Slot? = null
    private var closed = false
    private val handler = Handler(Looper.getMainLooper())

    fun start() {
        check(clips.isNotEmpty())
        current = create(0)
    }
    private fun create(index: Int): Slot {
        val player = MediaPlayer()
        val slot = Slot(index, player)
        try {
            player.setAudioAttributes(AlarmService.attributes)
            player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            player.setDataSource(clips[index].audio.path)
            player.setOnPreparedListener {
                if (!closed) try {
                    slot.ready = true
                    if (current === slot) play(slot)
                    else if (queued === slot && current?.ready == true && clips[current!!.index].pauseAfterMillis == 0L)
                        current?.player?.setNextMediaPlayer(player)
                } catch (e: Exception) { fail(e) }
            }
            player.setOnCompletionListener {
                if (!closed && current === slot) {
                    val next = queued
                    current = null
                    player.release()
                    // Der nächste Absatz bleibt während der Pause vorbereitet, startet aber noch nicht.
                    // close() entfernt diesen Übergang auch beim Stoppen/Schlummern in der Pause.
                    handler.postDelayed({
                        if (!closed) {
                            queued = null; current = next
                            if (next == null) current = create((index + 1) % clips.size)
                            else if (next.ready) try { play(next) } catch (e: Exception) { fail(e) }
                        }
                    }, clips[index].pauseAfterMillis)
                }
            }
            player.setOnErrorListener { _, what, extra -> fail(IllegalStateException("Audio-Wiedergabe fehlgeschlagen ($what/$extra)")); true }
            player.prepareAsync()
        } catch (e: Exception) {
            player.release()
            fail(e)
        }
        return slot
    }
    private fun play(slot: Slot) {
        if (closed) return
        val clip = clips[slot.index]
        // Erst beim Übergang setzen: setPlaybackParams darf keinen vorgeladenen Player starten.
        slot.player.playbackParams = PlaybackParams().setSpeed(clip.audio.speed)
        slot.player.start()
        onPlaying(clip)
        queued = create((slot.index + 1) % clips.size)
    }
    private fun fail(error: Exception) {
        if (closed) return
        close()
        onFailure(error)
    }
    override fun close() {
        if (closed) return
        closed = true
        handler.removeCallbacksAndMessages(null)
        current?.player?.release(); queued?.player?.release()
        current = null; queued = null
    }
}

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
    clips: List<AlarmClip>,
    private val onPlaying: (AlarmClip) -> Unit,
    private val onFailure: (Exception) -> Unit,
) : AutoCloseable {
    private class Slot(val index: Int, val player: MediaPlayer, var ready: Boolean = false)
    private var current: Slot? = null
    private var queued: Slot? = null
    private var closed = false
    private val handler = Handler(Looper.getMainLooper())
    /**
     * Spielt während des ganzen Weckens leises Pausenrauschen in Dauerschleife (zweite Schicht neben den gefüllten
     * Pausen). Ohne es ging der Lautsprecher-Verstärker in den Pausen zwischen Aufgaben und Absätzen aus und
     * blendete den nächsten Satz leise und hallig ein.
     */
    private var wachhalter: MediaPlayer? = null
    /**
     * Die Clips mit ausgefüllten Pausen: Jede Pause wird ein eigener Clip aus leisem Rauschen ([Tones.stille]), der
     * lückenlos per setNextMediaPlayer in den nächsten Satz übergeht. Früher lief in der Pause nichts, der nächste
     * Satz wurde neu gestartet und begann leise und hallig (nur die erste Aufgabe, lückenlos nach dem Vorlauf, klang
     * gleichmäßig). Lässt sich das Rauschen nicht anlegen, bleibt es bei der alten, stummen Pause.
     */
    private val folge: List<AlarmClip>
    /** Indizes der eingefügten Pausen-Clips in [folge]; sie melden kein [onPlaying]. */
    private val pausen = HashSet<Int>()

    init {
        val liste = mutableListOf<AlarmClip>()
        clips.forEach { clip ->
            val pause = clip.pauseAfterMillis
            val rauschen = if (pause <= 0L) null else runCatching { Tones.stille(AlarmStore.get(context).files, pause) }
                .onFailure { android.util.Log.w("WeckerAudio", "Pausenrauschen fehlt; stumme Pause als Ersatz", it) }.getOrNull()
            if (rauschen == null) liste += clip
            else {
                liste += clip.copy(pauseAfterMillis = 0)
                pausen += liste.size
                liste += AlarmClip(clip.step, PreparedAudio(rauschen.absolutePath, provider = "local"), clip.variation)
            }
        }
        folge = liste
    }

    fun start() {
        check(folge.isNotEmpty())
        wachhalter = Tones.wachhalter(context, AlarmService.attributes)
        current = create(0)
    }
    private fun create(index: Int): Slot {
        val player = MediaPlayer()
        val slot = Slot(index, player)
        try {
            player.setAudioAttributes(AlarmService.attributes)
            player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            player.setDataSource(folge[index].audio.path)
            player.setOnPreparedListener {
                if (!closed) try {
                    slot.ready = true
                    if (current === slot) play(slot)
                    else if (queued === slot && current?.ready == true && folge[current!!.index].pauseAfterMillis == 0L)
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
                            if (next == null) current = create((index + 1) % folge.size)
                            else if (next.ready) try { play(next) } catch (e: Exception) { fail(e) }
                        }
                    }, folge[index].pauseAfterMillis)
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
        val clip = folge[slot.index]
        // Erst beim Übergang setzen: setPlaybackParams darf keinen vorgeladenen Player starten.
        // Nur bei abweichendem Tempo (Qwen): Bei 1,0 ist nichts zu tun, und ein laufender, lückenlos gestarteter
        // Player soll nicht unnötig neu eingestellt werden.
        if (kotlin.math.abs(clip.audio.speed - 1f) > .001f) slot.player.playbackParams = PlaybackParams().setSpeed(clip.audio.speed)
        slot.player.start()
        if (slot.index !in pausen) onPlaying(clip)
        queued = create((slot.index + 1) % folge.size)
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
        runCatching { wachhalter?.release() }; wachhalter = null
    }
}

package de.frank.gedankenspeicher.tts

import android.content.Context
import android.media.MediaPlayer
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * **Spielt genau einen Absatz und kommt erst zurück, wenn er zu Ende ist.**
 *
 * Das ist das Gegenstück zur Absatz-Pipeline im [Vorleser]: weil das Warten hier
 * `suspend` ist, kann daneben schon der nächste Absatz synthetisiert werden, ohne dass
 * sich zwei Stimmen überlagern.
 *
 * Anhalten und Fortsetzen gehen über denselben Spieler — sie treffen damit immer den
 * Absatz, der gerade läuft.
 */
class Absatzabspieler(private val kontext: Context) {

    private val sperre = Any()
    private var spieler: MediaPlayer? = null

    /** Spielt die Datei und kehrt erst nach dem letzten Ton zurück. */
    suspend fun spieleUndWarte(datei: File) = suspendCancellableCoroutine { fortsetzung ->
        val fertig = AtomicBoolean(false)
        val neuer = MediaPlayer()
        try {
            synchronized(sperre) {
                raeumeAuf()
                spieler = neuer
                neuer.setAudioAttributes(SpeechLoudness.attributes)
                // Hält die CPU wach, solange ein Absatz klingt. Ohne das schläft das Gerät
                // bei ausgeschaltetem Bildschirm mitten im Satz ein — der Vordergrunddienst
                // allein verhindert das nicht, er verhindert nur das Einfrieren der App.
                runCatching {
                    neuer.setWakeMode(kontext, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                }
                neuer.setDataSource(datei.absolutePath)
                neuer.setOnCompletionListener {
                    if (fertig.compareAndSet(false, true)) {
                        loese(neuer)
                        fortsetzung.resume(Unit)
                    }
                }
                neuer.setOnErrorListener { _, was, extra ->
                    if (fertig.compareAndSet(false, true)) {
                        loese(neuer)
                        fortsetzung.resumeWithException(
                            TtsPlaybackException("Wiedergabe brach ab ($was/$extra)."),
                        )
                    }
                    true
                }
                neuer.prepare()
                SpeechLoudness.boost(neuer)
                neuer.start()
            }
        } catch (fehler: Exception) {
            if (fertig.compareAndSet(false, true)) {
                loese(neuer)
                fortsetzung.resumeWithException(fehler)
            }
        }

        fortsetzung.invokeOnCancellation {
            if (fertig.compareAndSet(false, true)) loese(neuer)
        }
    }

    fun pause(): Boolean = synchronized(sperre) {
        val laufender = spieler ?: return@synchronized false
        try {
            if (laufender.isPlaying) laufender.pause()
            true
        } catch (fehler: Exception) {
            logger.warning("Anhalten misslang: ${fehler.message}")
            false
        }
    }

    fun fortsetzen(): Boolean = synchronized(sperre) {
        val laufender = spieler ?: return@synchronized false
        try {
            laufender.start()
            true
        } catch (fehler: Exception) {
            logger.warning("Fortsetzen misslang: ${fehler.message}")
            false
        }
    }

    fun stop() = synchronized(sperre) { raeumeAuf() }

    private fun loese(welcher: MediaPlayer) = synchronized(sperre) {
        if (spieler === welcher) raeumeAuf() else beende(welcher)
    }

    private fun raeumeAuf() {
        val alter = spieler ?: return
        spieler = null
        beende(alter)
    }

    private fun beende(welcher: MediaPlayer) {
        try {
            welcher.stop()
        } catch (_: Exception) {
            // Schon beendet oder im Fehlerzustand — beides ist hier in Ordnung.
        }
        SpeechLoudness.release(welcher)
        welcher.release()
    }

    private companion object {
        val logger: Logger = Logger.getLogger(Absatzabspieler::class.java.name)
    }
}

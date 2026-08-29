package de.frank.claudekompass.tts

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import de.frank.claudekompass.observability.KompassLog
import java.util.WeakHashMap

/**
 * Sorgt dafür, dass gesprochener Text mit voller Kraft herauskommt.
 *
 * Drei Dinge fehlen sonst, und jedes kostet Lautstärke:
 *  - keine [AudioAttributes], also meldet sich die Wiedergabe nie als Sprache auf dem
 *    Medienkanal an, den die Lautstärketasten steuern,
 *  - keine ausdrückliche Kanallautstärke,
 *  - überhaupt keine Verstärkung — die rohen Sprachdateien von Google, Edge und Alibaba
 *    liegen deutlich unter dem, was ein Handylautsprecher in einem ruhigen Raum braucht.
 *
 * Jeder Abspieler in diesem Paket MUSS seinen [MediaPlayer] durch [verstaerke] und
 * [gibFrei] schicken. Ein neuer Anbieter, der das überspringt, ist wieder zu leise.
 */
object SpeechLoudness {

    /**
     * Plus 12 Dezibel. Der Verstärker komprimiert vor dem Anheben, deshalb wird Sprache laut,
     * ohne das Knistern zu erzeugen, das rohes Verstärken machen würde.
     */
    const val ZIEL_VERSTAERKUNG_MILLIBEL = 1200

    private val verstaerker = WeakHashMap<MediaPlayer, LoudnessEnhancer>()

    /** Sprache auf dem Medienkanal: Die Lautstärketasten greifen, das Absenken passt. */
    val attribute: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    /** Direkt nach [MediaPlayer.prepare] und vor `start()` aufrufen. */
    fun verstaerke(spieler: MediaPlayer) {
        try {
            spieler.setVolume(1f, 1f)
        } catch (fehler: Exception) {
            KompassLog.warn("SpeechLoudness", "verstaerke", "Lautstärke nicht setzbar", mapOf("grund" to fehler.message))
        }
        try {
            val geraet = LoudnessEnhancer(spieler.audioSessionId)
            geraet.setTargetGain(ZIEL_VERSTAERKUNG_MILLIBEL)
            geraet.enabled = true
            synchronized(verstaerker) { verstaerker[spieler] = geraet }
        } catch (fehler: Exception) {
            // Ein paar Geräte und Emulator-Abbilder haben den Effekt nicht. Sprache muss
            // trotzdem laufen: leiser ist schlecht, still wäre schlimmer.
            KompassLog.warn(
                "SpeechLoudness",
                "verstaerke",
                "Verstärker nicht verfügbar, spiele unverstärkt",
                mapOf("grund" to fehler.message),
            )
        }
    }

    /** Vor [MediaPlayer.release] aufrufen — der Effekt überlebt den Abspieler sonst. */
    fun gibFrei(spieler: MediaPlayer?) {
        val geraet = synchronized(verstaerker) { verstaerker.remove(spieler) } ?: return
        try {
            geraet.enabled = false
            geraet.release()
        } catch (fehler: Exception) {
            KompassLog.warn("SpeechLoudness", "gibFrei", "Verstärker nicht freigebbar", mapOf("grund" to fehler.message))
        }
    }
}

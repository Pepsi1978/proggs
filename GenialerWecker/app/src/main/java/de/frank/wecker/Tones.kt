package de.frank.wecker

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/** Eigene, lizenzfreie Signale; keine Downloads und keine Netzabhängigkeit beim Wecken. */
object Tones {
    /** So lange läuft Stille, bevor der erste Klang startet: Zeit, in der der Verstärker sicher angeht. */
    const val VORLAUF_MS = 900L
    val names = linkedMapOf("classic" to "Klassischer Wecker", "bells" to "Sanfte Glocken", "pulse" to "Digitaler Puls", "chime" to "Erinnerungszeichen")
    fun file(directory: File, name: String): File {
        val kind = name.takeIf { it in names } ?: "classic"
        val file = File(directory, "tone_$kind.wav")
        if (file.exists() && file.length() > 44) return file
        synchronized(this) {
            val rate = 22050
            val duration = if (kind == "chime") 2 else 6
            val samples = rate * duration
            val buffer = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
            buffer.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray())
                .putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(samples * 2)
            repeat(samples) { i ->
                val t = i.toDouble() / rate
                val phase = t % (if (kind == "bells") 1.5 else 1.0)
                val frequency = when (kind) { "bells" -> 523.25; "pulse" -> if (phase < .5) 880.0 else 1108.73; "chime" -> if (t < .7) 659.25 else 880.0; else -> 880.0 }
                val envelope = when (kind) {
                    "bells" -> kotlin.math.exp(-3.0 * phase)
                    "chime" -> kotlin.math.exp(-3.0 * (if (t < .7) t else t - .7))
                    else -> if (phase % .25 < .16) 0.8 else 0.0
                }
                val fade = minOf(1.0, t * 100, (duration - t) * 100)
                val value = (sin(2 * PI * frequency * t) + .2 * sin(2 * PI * frequency * 2 * t)) * envelope * fade
                buffer.putShort((value * 18000).toInt().coerceIn(-32767, 32767).toShort())
            }
            val temporary = File(directory, "tone_$kind.tmp")
            temporary.writeBytes(buffer.array())
            check(temporary.renameTo(file)) { "Weckton konnte nicht gespeichert werden." }
        }
        return file
    }

    /**
     * Unhörbar leises Rauschen (etwa −70 dBFS) von [millis] Länge: für den Vorlauf vor dem ersten Klang, für die
     * Pausen zwischen Aufgaben und Absätzen und für den Wachhalter.
     *
     * Bewusst KEINE reine Stille: Der Lautsprecher-Verstärker (Samsung Fold) erkennt digitale Nullen nach etwa ein bis
     * zwei Sekunden als „aus“ und blendet den nächsten Satz dann leise und hallig ein. Das Rauschen liegt unter dem
     * Eigenrauschen des Lautsprechers und hält ihn trotzdem an. Neue Erzeugungsart = neuer Dateiname (_v2), sonst
     * würde die alte Nullen-Datei aus dem Cache weiterbenutzt.
     */
    fun stille(directory: File, millis: Long = VORLAUF_MS): File {
        val file = File(directory, "stille_${millis}_v2.wav")
        if (file.exists() && file.length() > 44) return file
        synchronized(this) {
            if (file.exists() && file.length() > 44) return file
            val rate = 22050
            val samples = (rate * millis / 1000).toInt().coerceAtLeast(1)
            val buffer = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
            buffer.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray())
                .putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(samples * 2)
            val zufall = java.util.Random(millis)
            repeat(samples) { buffer.putShort((zufall.nextInt(2 * RAUSCHEN + 1) - RAUSCHEN).toShort()) }
            val temporary = File(directory, "stille_${millis}_v2.tmp")
            temporary.writeBytes(buffer.array())
            check(temporary.renameTo(file)) { "Vorlauf konnte nicht gespeichert werden." }
        }
        return file
    }

    /** Spitzenwert des Pausenrauschens in 16-Bit-Stufen: ±12 ≈ −69 dBFS Spitze, −73 dBFS im Mittel. */
    private const val RAUSCHEN = 12

    /**
     * Pausenrauschen ([stille]) in Dauerschleife auf demselben Audioweg wie [attributes]. Solange es läuft, bleibt der
     * Lautsprecher-Verstärker an, und jeder Satz kommt ab der ersten Silbe in voller Lautstärke statt leise und
     * hallig eingeblendet. Scheitert es, gibt es null: Wecken und Vorschau laufen dann trotzdem.
     */
    fun wachhalter(context: Context, attributes: AudioAttributes): MediaPlayer? {
        val player = runCatching { MediaPlayer() }
            .onFailure { android.util.Log.w("WeckerAudio", "Wachhalter: MediaPlayer nicht verfügbar", it) }.getOrNull() ?: return null
        return runCatching {
            player.setAudioAttributes(attributes)
            player.setDataSource(stille(AlarmStore.get(context).files).absolutePath)
            player.isLooping = true
            player.prepare()
            player.start()
            player
        }.getOrElse {
            android.util.Log.w("WeckerAudio", "Wachhalter nicht gestartet; Wecken läuft ohne ihn weiter", it)
            runCatching { player.release() }; null
        }
    }
}

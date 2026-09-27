package de.frank.wecker

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/** Eigene, lizenzfreie Signale; keine Downloads und keine Netzabhängigkeit beim Wecken. */
object Tones {
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
}

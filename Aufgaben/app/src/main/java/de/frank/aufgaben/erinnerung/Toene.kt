package de.frank.aufgaben.erinnerung

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Eigene, lizenzfreie Erinnerungstöne (wie im Genialen Wecker), dazu jeder Systemton.
 * Gespielt wird mit eigener Lautstärke — unabhängig davon, was der Kanal sonst täte.
 */
object Toene {
    val eigene = linkedMapOf(
        "chime" to "Erinnerungszeichen",
        "bells" to "Sanfte Glocken",
        "pulse" to "Digitaler Puls",
        "classic" to "Klassisch",
        "kristall" to "Kristall",
        "tropfen" to "Wassertropfen",
    )

    private fun datei(context: Context, name: String): File {
        val art = name.takeIf { it in eigene } ?: "chime"
        val file = File(context.filesDir, "ton_$art.wav")
        if (file.exists() && file.length() > 44) return file
        synchronized(this) {
            val rate = 22050
            val dauer = when (art) { "chime", "tropfen" -> 2.0; "kristall" -> 2.5; else -> 4.0 }
            val samples = (rate * dauer).toInt()
            val buffer = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
            buffer.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray())
                .putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(samples * 2)
            repeat(samples) { i ->
                val t = i.toDouble() / rate
                val wert = when (art) {
                    "bells" -> { val p = t % 1.5; sin(2 * PI * 523.25 * t) * exp(-3.0 * p) + .3 * sin(2 * PI * 1046.5 * t) * exp(-4.0 * p) }
                    "pulse" -> { val p = t % 1.0; (if (p % .25 < .16) .8 else 0.0) * sin(2 * PI * (if (p < .5) 880.0 else 1108.73) * t) }
                    "classic" -> { val p = t % 1.0; (if (p % .25 < .16) .8 else 0.0) * sin(2 * PI * 880.0 * t) }
                    "kristall" -> listOf(0.0 to 1318.5, 0.18 to 1568.0, 0.36 to 2093.0).sumOf { (s, f) ->
                        if (t < s) 0.0 else sin(2 * PI * f * t) * exp(-4.0 * (t - s)) * .5
                    }
                    "tropfen" -> { val p = t % 0.5; sin(2 * PI * (600.0 + 900.0 * exp(-18.0 * p)) * t) * exp(-9.0 * p) }
                    else -> { val s = if (t < .45) t else t - .45; sin(2 * PI * (if (t < .45) 659.25 else 987.77) * t) * exp(-3.0 * s) }
                }
                val weich = minOf(1.0, t * 200, (dauer - t) * 60)
                buffer.putShort((wert * weich * 17000).toInt().coerceIn(-32767, 32767).toShort())
            }
            val tmp = File(context.filesDir, "ton_$art.tmp")
            tmp.writeBytes(buffer.array())
            tmp.renameTo(file)
        }
        return file
    }

    /** Spielt den Ton einmal. [fertig] kommt nach dem Ende oder spätestens nach [maxMs]. */
    fun spiele(context: Context, ton: String, lautstaerke: Float, maxMs: Long = 9_000, fertig: () -> Unit = {}): MediaPlayer? {
        return try {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            if (ton.startsWith("uri:")) player.setDataSource(context, Uri.parse(ton.removePrefix("uri:")))
            else player.setDataSource(datei(context, ton).absolutePath)
            player.setVolume(lautstaerke, lautstaerke)
            var beendet = false
            val ende = {
                if (!beendet) {
                    beendet = true
                    runCatching { player.stop() }
                    runCatching { player.release() }
                    fertig()
                }
            }
            player.setOnCompletionListener { ende() }
            player.setOnErrorListener { _, _, _ -> ende(); true }
            player.prepare()
            player.start()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ ende() }, maxMs)
            player
        } catch (_: Exception) {
            fertig()
            null
        }
    }
}

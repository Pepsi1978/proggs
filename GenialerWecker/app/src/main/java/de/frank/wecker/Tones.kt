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
    /** So lange läuft das Füllsignal ([stille]), bevor der erste Klang startet: Zeit, in der der Verstärker sicher angeht. */
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
     * Füllsignal von [millis] Länge mit der Abtastrate [rate]: für den Vorlauf vor dem ersten Klang, für die Pausen
     * zwischen Aufgaben und Absätzen und für den Wachhalter.
     *
     * Bewusst KEINE reine Stille: Der Lautsprecher-Verstärker (Samsung Fold) wertet digitale Nullen und sehr leise
     * Signale nach etwa ein bis zwei Sekunden als „aus“ und blendet den nächsten Satz dann leise und hallig ein. Das
     * Füllsignal ist ein tiefer Ton ([BRUMM_HZ], −40 dBFS), den ein Handylautsprecher nicht wiedergeben kann, plus
     * leises Rauschen. Für den Verstärker ist das ein deutliches Signal, zu hören ist es nicht. Ganze Schwingungen je
     * Pause, Beginn und Ende im Nulldurchgang: kein Knacken, auch nicht in der Dauerschleife.
     *
     * [rate] sollte der Abtastrate des Nachbarclips entsprechen (siehe [abtastrate]), sonst baut Android beim
     * Übergang einen neuen Tonweg auf, und der Übergang ist nicht wirklich lückenlos.
     * Neue Erzeugungsart = neuer Dateiname (_v3), sonst liefe eine alte Datei aus dem Cache weiter.
     */
    fun stille(directory: File, millis: Long = VORLAUF_MS, rate: Int = STANDARD_RATE): File {
        val file = File(directory, "stille_${millis}_${rate}_v3.wav")
        if (file.exists() && file.length() > 44) return file
        synchronized(this) {
            if (file.exists() && file.length() > 44) return file
            val samples = (rate.toLong() * millis / 1000).toInt().coerceAtLeast(1)
            val buffer = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
            buffer.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray())
                .putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(samples * 2)
            // Ganze Schwingungen: Die Frequenz wird so gerundet, dass die Pause genau auf einem Nulldurchgang endet.
            val schwingungen = maxOf(1L, Math.round(BRUMM_HZ * millis / 1000.0))
            val frequenz = schwingungen * 1000.0 / millis
            val zufall = java.util.Random(millis * 31 + rate)
            repeat(samples) { i ->
                val brumm = BRUMM * sin(2 * PI * frequenz * i / rate)
                val rauschen = zufall.nextInt(2 * RAUSCHEN + 1) - RAUSCHEN
                buffer.putShort((brumm + rauschen).toInt().coerceIn(-32767, 32767).toShort())
            }
            val temporary = File(directory, "${file.nameWithoutExtension}.tmp")
            temporary.writeBytes(buffer.array())
            check(temporary.renameTo(file)) { "Vorlauf konnte nicht gespeichert werden." }
        }
        return file
    }

    /**
     * Abtastrate der ersten Tonspur von [path] (Google/Edge-MP3 24000, Supertonic 44100 …), damit Pausen dasselbe
     * Format haben wie die Sprache davor. Unlesbar → [STANDARD_RATE] (Übergang dann evtl. nicht ganz lückenlos).
     */
    fun abtastrate(path: String): Int = runCatching {
        val extractor = android.media.MediaExtractor()
        try {
            extractor.setDataSource(path)
            (0 until extractor.trackCount).map { extractor.getTrackFormat(it) }
                .firstOrNull { it.getString(android.media.MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE)
        } finally { extractor.release() }
    }.onFailure { android.util.Log.w("WeckerAudio", "Abtastrate von $path unlesbar", it) }
        .getOrNull()?.takeIf { it in 8000..96000 } ?: STANDARD_RATE

    const val STANDARD_RATE = 22050
    /** Tiefer Füllton: unter dem, was ein Handylautsprecher abstrahlt, und unter der Hörschwelle bei −40 dBFS. */
    const val BRUMM_HZ = 30.0
    /** Amplitude des Füllton in 16-Bit-Stufen: 328 ≈ −40 dBFS, für den Verstärker klar „Signal“. */
    private const val BRUMM = 328.0
    /** Spitzenwert des zusätzlichen Rauschens: ±24 ≈ −63 dBFS. */
    private const val RAUSCHEN = 24

    /**
     * Füllsignal ([stille]) in Dauerschleife auf demselben Audioweg wie [attributes]. Solange es läuft, bleibt der
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

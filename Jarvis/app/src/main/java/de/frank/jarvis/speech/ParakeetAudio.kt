package de.frank.jarvis.speech

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Macht aus der Aufnahme einer anderen App die Proben, die Parakeet braucht: 16 kHz, mono, Float.
 * Die Apps nehmen unterschiedlich auf (16 kHz, 44,1 kHz, WAV mit weiteren Blöcken, MP4): WAV wird über seinen Kopf gelesen,
 * alles andere über MediaCodec dekodiert.
 */
internal object ParakeetAudio {
    private const val ZIEL_HZ = 16_000

    fun lies(datei: File): FloatArray {
        val kopf = ByteArray(12)
        datei.inputStream().use { it.read(kopf) }
        val riff = String(kopf, 0, 4, Charsets.US_ASCII) == "RIFF" && String(kopf, 8, 4, Charsets.US_ASCII) == "WAVE"
        return if (riff) ausWav(datei.readBytes()) else ausCodec(datei)
    }

    private fun ausWav(wav: ByteArray): FloatArray {
        val b = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        var kanaele = 1
        var hz = ZIEL_HZ
        var bits = 16
        var format = 1
        var p = 12
        while (p + 8 <= wav.size) {
            val name = String(wav, p, 4, Charsets.US_ASCII)
            val laenge = b.getInt(p + 4)
            val inhalt = p + 8
            if (name == "fmt ") {
                format = b.getShort(inhalt).toInt() and 0xFFFF
                kanaele = b.getShort(inhalt + 2).toInt().coerceAtLeast(1)
                hz = b.getInt(inhalt + 4).coerceAtLeast(1)
                bits = b.getShort(inhalt + 14).toInt()
            } else if (name == "data") {
                val ende = if (laenge in 0..(wav.size - inhalt)) inhalt + laenge else wav.size
                if (format != 1 && format != 3 && format != 0xFFFE) throw ParakeetFehler("Dieses WAV-Format wird nicht unterstützt.")
                val proben = when (bits) {
                    16 -> FloatArray((ende - inhalt) / 2) { b.getShort(inhalt + it * 2) / 32_768f }
                    32 -> if (format == 3) FloatArray((ende - inhalt) / 4) { b.getFloat(inhalt + it * 4) }
                    else FloatArray((ende - inhalt) / 4) { b.getInt(inhalt + it * 4) / 2_147_483_648f }
                    else -> throw ParakeetFehler("WAV mit $bits Bit wird nicht unterstützt.")
                }
                return aufZiel(proben, kanaele, hz)
            }
            p = inhalt + laenge.coerceAtLeast(0) + (laenge and 1)
        }
        throw ParakeetFehler("Die Aufnahme enthält keine Audiodaten.")
    }

    private fun ausCodec(datei: File): FloatArray {
        val quelle = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            quelle.setDataSource(datei.absolutePath)
            val spur = (0 until quelle.trackCount).firstOrNull { quelle.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?: throw ParakeetFehler("Die Aufnahme enthält keine Audiospur.")
            quelle.selectTrack(spur)
            val eingang = quelle.getTrackFormat(spur)
            val c = MediaCodec.createDecoderByType(eingang.getString(MediaFormat.KEY_MIME)!!).also { codec = it }
            c.configure(eingang, null, null, 0)
            c.start()
            var hz = eingang.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var kanaele = eingang.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var proben = FloatArray(1 shl 18)
            var n = 0
            val info = MediaCodec.BufferInfo()
            var eingabeFertig = false
            while (true) {
                if (!eingabeFertig) {
                    val i = c.dequeueInputBuffer(10_000)
                    if (i >= 0) {
                        val gelesen = quelle.readSampleData(c.getInputBuffer(i)!!, 0)
                        if (gelesen < 0) {
                            c.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            eingabeFertig = true
                        } else {
                            c.queueInputBuffer(i, 0, gelesen, quelle.sampleTime, 0)
                            quelle.advance()
                        }
                    }
                }
                val o = c.dequeueOutputBuffer(info, 10_000)
                if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    hz = c.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    kanaele = c.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                } else if (o >= 0) {
                    val puffer = c.getOutputBuffer(o)!!.order(ByteOrder.nativeOrder()).asShortBuffer()
                    val anzahl = info.size / 2
                    if (n + anzahl > proben.size) proben = proben.copyOf(maxOf(proben.size * 2, n + anzahl))
                    for (k in 0 until anzahl) proben[n++] = puffer.get(k) / 32_768f
                    c.releaseOutputBuffer(o, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
            return aufZiel(proben.copyOf(n), kanaele, hz)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            quelle.release()
        }
    }

    /** Kanäle mitteln, dann linear auf 16 kHz umrechnen; für Sprache genügt das. */
    private fun aufZiel(proben: FloatArray, kanaele: Int, hz: Int): FloatArray {
        val mono = if (kanaele <= 1) proben else FloatArray(proben.size / kanaele) { i ->
            var summe = 0f
            for (k in 0 until kanaele) summe += proben[i * kanaele + k]
            summe / kanaele
        }
        if (hz == ZIEL_HZ || mono.isEmpty()) return mono
        val schritt = hz.toDouble() / ZIEL_HZ
        return FloatArray((mono.size / schritt).toInt()) { i ->
            val pos = i * schritt
            val links = pos.toInt().coerceAtMost(mono.size - 1)
            val rechts = (links + 1).coerceAtMost(mono.size - 1)
            val anteil = (pos - links).toFloat()
            mono[links] * (1 - anteil) + mono[rechts] * anteil
        }
    }
}

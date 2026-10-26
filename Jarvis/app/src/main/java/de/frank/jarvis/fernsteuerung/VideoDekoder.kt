package de.frank.jarvis.fernsteuerung

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** H.264-Bilder (Annex B, SPS/PPS vor jedem Schlüsselbild) direkt auf eine Surface dekodieren, mit wenig Verzögerung. */
class VideoDekoder(private val surface: Surface, private val breite: Int, private val hoehe: Int, private val schluesselbildHolen: () -> Unit) {
    private val schlange = LinkedBlockingQueue<ByteArray>(90)
    @Volatile private var laeuft = true
    private var codec: MediaCodec? = null
    private val faden = Thread({ schleife() }, "Fernsteuerung-Video")

    fun start() = faden.start()

    fun bild(daten: ByteArray) {
        if (!schlange.offer(daten)) {
            // Rückstau: alles verwerfen und auf ein frisches Schlüsselbild warten.
            schlange.clear(); wartetAufSchluessel = true; schluesselbildHolen()
        }
    }

    @Volatile private var wartetAufSchluessel = true

    private fun schleife() {
        val info = MediaCodec.BufferInfo()
        while (laeuft) {
            try {
                val d = schlange.poll(20, TimeUnit.MILLISECONDS)
                if (d != null) {
                    val nal = nalTypen(d)
                    if (wartetAufSchluessel && 5 !in nal) {
                        ausgeben(info); continue
                    }
                    if (codec == null) anlegen(d) ?: continue
                    // Nur ein Schlüsselbild beendet das Warten (ein gleichzeitiger Rückstau setzt es wieder).
                    if (5 in nal) wartetAufSchluessel = false else if (wartetAufSchluessel) { ausgeben(info); continue }
                    val c = codec ?: continue
                    val idx = c.dequeueInputBuffer(30_000)
                    if (idx >= 0) {
                        val buf = c.getInputBuffer(idx)!!
                        buf.clear()
                        if (d.size > buf.capacity()) { c.queueInputBuffer(idx, 0, 0, 0, 0); throw IllegalStateException("Bild zu groß: ${d.size}") }
                        buf.put(d)
                        c.queueInputBuffer(idx, 0, d.size, System.nanoTime() / 1000, 0)
                    }
                }
                ausgeben(info)
            } catch (e: InterruptedException) {
                break
            } catch (e: Exception) {
                // Dekoderfehler: frisch anlegen und auf ein neues Schlüsselbild warten, statt schwarz zu bleiben.
                Log.w(FernVerbindung.TAG, "Dekoder", e)
                freigeben()
                schlange.clear(); wartetAufSchluessel = true; schluesselbildHolen()
                Thread.sleep(200)
            }
        }
        freigeben()
    }

    private fun freigeben() {
        runCatching { codec?.stop() }; runCatching { codec?.release() }
        codec = null
    }

    private fun ausgeben(info: MediaCodec.BufferInfo) {
        val c = codec ?: return
        while (true) {
            val o = c.dequeueOutputBuffer(info, 0)
            if (o >= 0) c.releaseOutputBuffer(o, true) else break
        }
    }

    private fun anlegen(erstes: ByteArray): Unit? {
        val teile = nalEinheiten(erstes)
        val sps = teile.firstOrNull { it.second == 7 } ?: return null
        val pps = teile.firstOrNull { it.second == 8 } ?: return null
        val f = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, breite, hoehe)
        f.setByteBuffer("csd-0", ByteBuffer.wrap(erstes, sps.first, sps.third))
        f.setByteBuffer("csd-1", ByteBuffer.wrap(erstes, pps.first, pps.third))
        if (Build.VERSION.SDK_INT >= 30) f.setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
        f.setInteger(MediaFormat.KEY_PRIORITY, 0)
        f.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, breite * hoehe * 2)
        val c = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        c.configure(f, surface, null, 0)
        c.start()
        codec = c
        return Unit
    }

    /** Wartet kurz, bis der Codec die Surface freigegeben hat, damit ein Nachfolger sie verbinden kann. */
    fun stop() {
        laeuft = false
        if (Thread.currentThread() != faden) runCatching { faden.join(1000) }
    }

    companion object {
        /** (Anfang inkl. Startcode, NAL-Typ, Länge inkl. Startcode) jeder NAL-Einheit. */
        fun nalEinheiten(d: ByteArray): List<Triple<Int, Int, Int>> {
            val starts = ArrayList<Int>()
            var i = 0
            while (i + 3 < d.size) {
                if (d[i].toInt() == 0 && d[i + 1].toInt() == 0 && d[i + 2].toInt() == 0 && d[i + 3].toInt() == 1) { starts += i; i += 4 } else i++
            }
            return starts.mapIndexed { k, s ->
                val ende = if (k + 1 < starts.size) starts[k + 1] else d.size
                Triple(s, if (s + 4 < d.size) d[s + 4].toInt() and 0x1f else 0, ende - s)
            }
        }

        fun nalTypen(d: ByteArray): Set<Int> {
            // Nur den Anfang durchsuchen: SPS/PPS/IDR stehen vorn, und große P-Bilder sollen nicht ganz gescannt werden.
            val kopf = if (d.size > 512) d.copyOf(512) else d
            return nalEinheiten(kopf).map { it.second }.toSet()
        }
    }
}

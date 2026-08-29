package de.frank.claudekompass.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import de.frank.claudekompass.observability.KompassLog
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Nimmt über das Mikrofon auf und liefert eine fertige WAV-Datei im Speicher.
 *
 * 16 000 Hz, mono, 16 Bit — genau das, was die Spracherkennung erwartet (Referenz, Baustein F).
 * Fürs Klonen der eigenen Stimme sind 24 000 Hz gefragt; die lassen sich über
 * [starte] anfordern, mit Rückfall auf 16 kHz, falls das Gerät sie ablehnt.
 *
 * Ein Punkt, der schon Aufnahmen gekostet hat: [stoppe] wartet begrenzt auf das Lese-Ende.
 * Manche Geräte wecken einen blockierenden `read` nicht zuverlässig auf — ohne diese Grenze
 * bliebe der Aufrufer für immer hängen.
 */
class Mikrofon(context: Context) {

    private val appContext = context.applicationContext
    private val lebenszyklusSperre = Any()
    private val pufferSperre = Any()
    private val pcmPuffer = ByteArrayOutputStream()
    private val nimmtAuf = AtomicBoolean(false)

    @Volatile private var aufnehmer: AudioRecord? = null
    private var leseJob: Job? = null
    private var begonnenUm = 0L

    /** Die Rate, die das Gerät tatsächlich hergegeben hat — der WAV-Kopf muss dazu passen. */
    private var aktiveRate = ABTASTRATE

    fun starte(bereich: CoroutineScope, gewuenschteRate: Int = ABTASTRATE): Boolean =
        synchronized(lebenszyklusSperre) {
            if (!nimmtAuf.compareAndSet(false, true)) return true
            if (!bereich.isActive) {
                nimmtAuf.set(false)
                KompassLog.warn("Mikrofon", "starte", "Bereich ist nicht aktiv")
                return false
            }
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                nimmtAuf.set(false)
                KompassLog.warn("Mikrofon", "starte", "Erlaubnis für das Mikrofon fehlt")
                return false
            }

            val rate = listOf(gewuenschteRate, ABTASTRATE)
                .distinct()
                .firstOrNull { AudioRecord.getMinBufferSize(it, KANAL, FORMAT) > 0 }
            val minPuffer = rate?.let { AudioRecord.getMinBufferSize(it, KANAL, FORMAT) } ?: 0
            if (rate == null || minPuffer <= 0) {
                nimmtAuf.set(false)
                KompassLog.warn("Mikrofon", "starte", "Keine nutzbare Abtastrate", mapOf("gewuenscht" to gewuenschteRate))
                return false
            }
            if (rate != gewuenschteRate) {
                KompassLog.warn(
                    "Mikrofon",
                    "starte",
                    "Gewünschte Rate nicht verfügbar, nehme Ersatz",
                    mapOf("gewuenscht" to gewuenschteRate, "genommen" to rate),
                )
            }
            aktiveRate = rate

            val neuerAufnehmer = try {
                AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, KANAL, FORMAT, minPuffer * 2)
            } catch (fehler: Exception) {
                nimmtAuf.set(false)
                KompassLog.error("Mikrofon", "starte", "AudioRecord nicht erzeugbar", mapOf("grund" to fehler.message))
                return false
            }
            if (neuerAufnehmer.state != AudioRecord.STATE_INITIALIZED) {
                nimmtAuf.set(false)
                neuerAufnehmer.release()
                KompassLog.error("Mikrofon", "starte", "AudioRecord nicht bereit")
                return false
            }
            try {
                neuerAufnehmer.startRecording()
            } catch (fehler: Exception) {
                nimmtAuf.set(false)
                neuerAufnehmer.release()
                KompassLog.error("Mikrofon", "starte", "Aufnahme nicht startbar", mapOf("grund" to fehler.message))
                return false
            }

            synchronized(pufferSperre) { pcmPuffer.reset() }
            aufnehmer = neuerAufnehmer
            begonnenUm = System.currentTimeMillis()
            leseJob = bereich.launch(Dispatchers.IO) {
                val lesePuffer = ByteArray(minPuffer)
                try {
                    while (isActive && nimmtAuf.get()) {
                        val gelesen = neuerAufnehmer.read(lesePuffer, 0, lesePuffer.size, AudioRecord.READ_BLOCKING)
                        if (!nimmtAuf.get()) break
                        when {
                            gelesen > 0 -> synchronized(pufferSperre) {
                                if (pcmPuffer.size() + gelesen <= MAX_PUFFER_BYTES) {
                                    pcmPuffer.write(lesePuffer, 0, gelesen)
                                } else {
                                    KompassLog.warn("Mikrofon", "starte", "Aufnahmepuffer voll, beende Aufnahme")
                                    return@launch
                                }
                            }
                            gelesen < 0 -> {
                                KompassLog.error("Mikrofon", "starte", "Lesefehler", mapOf("code" to gelesen))
                                return@launch
                            }
                        }
                    }
                } finally {
                    // Endet die Schleife von selbst (voller Puffer, Lesefehler), muss der
                    // Aufnehmer hier freigegeben werden. stoppe() liefert die bis dahin
                    // gesammelten Bytes trotzdem noch aus — nichts geht verloren.
                    if (nimmtAuf.get()) {
                        synchronized(lebenszyklusSperre) {
                            if (aufnehmer === neuerAufnehmer) aufnehmer = null
                        }
                        stoppeUndGibFrei(neuerAufnehmer)
                    }
                }
            }
            true
        }

    suspend fun stoppe(): ByteArray? {
        val aktiverAufnehmer: AudioRecord?
        val aktiverJob: Job?
        synchronized(lebenszyklusSperre) {
            if (!nimmtAuf.compareAndSet(true, false)) return null
            aktiverAufnehmer = aufnehmer
            aktiverJob = leseJob
            aufnehmer = null
            leseJob = null
        }

        stoppeUndGibFrei(aktiverAufnehmer)
        aktiverJob?.cancel()
        val fertig = withTimeoutOrNull(STOPP_WARTEN_MS) {
            aktiverJob?.let { joinAll(it) }
            true
        } ?: false
        if (!fertig) KompassLog.warn("Mikrofon", "stoppe", "Lese-Job kam nicht rechtzeitig zum Ende")

        val pcm = synchronized(pufferSperre) { pcmPuffer.toByteArray().also { pcmPuffer.reset() } }
        KompassLog.info(
            "Mikrofon",
            "stoppe",
            "Aufnahme beendet",
            mapOf("bytes" to pcm.size, "dauerMs" to (System.currentTimeMillis() - begonnenUm)),
        )
        return pcm.takeIf { it.isNotEmpty() }?.let(::pcmZuWav)
    }

    fun nimmtGeradeAuf(): Boolean = nimmtAuf.get()

    fun gibFrei() {
        val aktiverAufnehmer: AudioRecord?
        val aktiverJob: Job?
        synchronized(lebenszyklusSperre) {
            nimmtAuf.set(false)
            aktiverAufnehmer = aufnehmer
            aktiverJob = leseJob
            aufnehmer = null
            leseJob = null
        }
        aktiverJob?.cancel()
        stoppeUndGibFrei(aktiverAufnehmer)
    }

    private fun stoppeUndGibFrei(aufnehmer: AudioRecord?) {
        // Beides nur bestmöglich: Was vor einem Herstellerfehler aufgenommen wurde, bleibt
        // brauchbar — deshalb darf hier nichts weitergeworfen werden.
        runCatching { aufnehmer?.stop() }
        runCatching { aufnehmer?.release() }
    }

    private fun pcmZuWav(pcm: ByteArray): ByteArray {
        val rate = aktiveRate
        val byteRate = rate * KANAELE * BITS_PRO_WERT / 8
        val kopf = ByteBuffer.allocate(WAV_KOPF_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(pcm.size + 36)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(KANAELE.toShort())
            putInt(rate)
            putInt(byteRate)
            putShort((KANAELE * BITS_PRO_WERT / 8).toShort())
            putShort(BITS_PRO_WERT.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcm.size)
        }.array()
        return kopf + pcm
    }

    companion object {
        const val ABTASTRATE = 16_000

        /** Womit die Referenzaufnahme fürs Klonen gemacht wird (Referenz, Baustein E). */
        const val KLON_ABTASTRATE = 24_000
        const val KANAL = AudioFormat.CHANNEL_IN_MONO
        const val FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val WAV_KOPF_BYTES = 44

        private const val KANAELE = 1
        private const val BITS_PRO_WERT = 16

        /** Rund zehn Minuten bei 16 kHz. Danach wird nicht mehr gepuffert. */
        private const val MAX_PUFFER_BYTES = ABTASTRATE * 2 * 60 * 10
        private const val STOPP_WARTEN_MS = 1_500L
    }
}

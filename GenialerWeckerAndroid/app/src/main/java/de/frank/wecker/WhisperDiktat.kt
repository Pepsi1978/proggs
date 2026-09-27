package de.frank.wecker

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import kotlinx.coroutines.*
import kotlin.math.sqrt

/**
 * Das Whisper-Sprachmodell (small, int8) liegt fest in der APK unter assets/whisper – das Diktat funktioniert
 * vom ersten Start an ohne Internet. sherpa-onnx liest es direkt aus der APK (unkomprimiert abgelegt).
 */
object WhisperModell {
    private val DATEIEN = listOf("whisper/small-encoder.int8.onnx", "whisper/small-decoder.int8.onnx", "whisper/small-tokens.txt")
    val encoder get() = DATEIEN[0]
    val decoder get() = DATEIEN[1]
    val tokens get() = DATEIEN[2]
    @Volatile private var vorhanden: Boolean? = null
    /** Liegt das Modell in dieser APK? (Ein Build ohne Modell fällt auf das Android-Diktat zurück.) */
    fun bereit(context: Context): Boolean = vorhanden ?: runCatching {
        context.assets.list("whisper").orEmpty().toSet().containsAll(DATEIEN.map { it.substringAfter('/') })
    }.getOrDefault(false).also { vorhanden = it }
}

/** Der geladene Erkenner; teuer im Aufbau, darum eine Instanz, die nach einer Minute Ruhe freigegeben wird. */
object WhisperErkenner {
    private var erkenner: OfflineRecognizer? = null
    private var sprache = ""
    private var freigabe: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    /** Vorab laden, während noch gesprochen wird – dann steht das Ergebnis gleich nach „Fertig“. */
    fun vorwaermen(context: Context, code: String) { scope.launch { runCatching { holen(context, code) } } }

    private fun holen(context: Context, code: String): OfflineRecognizer = synchronized(lock) {
        freigabe?.cancel()
        erkenner?.takeIf { sprache == code }?.let { return it }
        erkenner?.release(); erkenner = null
        val neu = OfflineRecognizer(context.applicationContext.assets, OfflineRecognizerConfig(modelConfig = OfflineModelConfig(
            whisper = OfflineWhisperModelConfig(encoder = WhisperModell.encoder, decoder = WhisperModell.decoder, language = code, task = "transcribe"),
            tokens = WhisperModell.tokens, numThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4))))
        erkenner = neu; sprache = code
        neu
    }

    /** Erkennt 16-kHz-Samples; lange Aufnahmen in Stücken von höchstens 30 s, an der leisesten Stelle geteilt. */
    fun erkenne(context: Context, samples: FloatArray, code: String): String {
        val rec = holen(context, code)
        val text = synchronized(lock) {
            DiktatAudio.teile(samples).joinToString(" ") { stueck ->
                val strom = rec.createStream()
                try { strom.acceptWaveform(stueck, DiktatAudio.RATE); rec.decode(strom); rec.getResult(strom).text.trim() }
                finally { strom.release() }
            }.trim()
        }
        spaeterFreigeben()
        return DiktatAudio.saeubern(text)
    }

    private fun spaeterFreigeben() {
        freigabe?.cancel()
        freigabe = scope.launch { delay(60_000); freigeben() }
    }

    fun freigeben() = synchronized(lock) { erkenner?.release(); erkenner = null }
}
/** Reine Audio-Hilfen: Stille erkennen, Ränder kürzen, teilen, typische Whisper-Floskeln verwerfen. */
object DiktatAudio {
    const val RATE = 16_000
    private const val FENSTER = RATE * 30 / 1000 // 30 ms

    /** Wie viele Millisekunden deutlich über dem Grundrauschen liegen (Sprache). */
    fun sprachMs(samples: FloatArray): Int {
        val pegel = fensterPegel(samples)
        if (pegel.isEmpty()) return 0
        val boden = pegel.sorted()[pegel.size / 10].coerceAtLeast(0.002f)
        return pegel.count { it > maxOf(boden * 3f, 0.012f) } * 30
    }

    /** Stille am Anfang und Ende weg (mit 300 ms Rand): weniger Rechenzeit, weniger Halluzinationen. */
    fun kuerzen(samples: FloatArray): FloatArray {
        val pegel = fensterPegel(samples)
        if (pegel.isEmpty()) return samples
        val boden = pegel.sorted()[pegel.size / 10].coerceAtLeast(0.002f)
        val schwelle = maxOf(boden * 3f, 0.012f)
        val erstes = pegel.indexOfFirst { it > schwelle }
        val letztes = pegel.indexOfLast { it > schwelle }
        if (erstes < 0) return samples
        val rand = RATE * 3 / 10
        return samples.copyOfRange((erstes * FENSTER - rand).coerceAtLeast(0), ((letztes + 1) * FENSTER + rand).coerceAtMost(samples.size))
    }

    private fun fensterPegel(samples: FloatArray): FloatArray = FloatArray(samples.size / FENSTER) { i ->
        var summe = 0f
        for (j in i * FENSTER until (i + 1) * FENSTER) summe += samples[j] * samples[j]
        sqrt(summe / FENSTER)
    }

    fun teile(samples: FloatArray): List<FloatArray> {
        val max = RATE * 30
        if (samples.size <= max) return listOf(samples)
        val stuecke = mutableListOf<FloatArray>()
        var start = 0
        while (samples.size - start > max) {
            // Leiseste 300-ms-Stelle zwischen 25 und 30 s.
            val von = start + RATE * 25; val bis = start + max - FENSTER * 10
            var besteStelle = start + max; var bestePegel = Float.MAX_VALUE
            var p = von
            while (p < bis) {
                var s = 0f
                for (j in p until p + FENSTER * 10) s += samples[j] * samples[j]
                if (s < bestePegel) { bestePegel = s; besteStelle = p + FENSTER * 5 }
                p += FENSTER
            }
            stuecke += samples.copyOfRange(start, besteStelle)
            start = besteStelle
        }
        stuecke += samples.copyOfRange(start, samples.size)
        return stuecke
    }

    private val FLOSKELN = setOf(
        "vielen dank", "vielen dank fürs zuschauen", "danke fürs zuschauen", "bis zum nächsten mal", "bis zum nächsten video",
        "untertitel", "untertitel des zdf", "untertitelung des zdf für funk", "untertitel im auftrag des zdf für funk",
        "untertitel im auftrag des zdf", "untertitel der amara org community", "thank you", "thank you for watching",
        "thanks for watching", "please subscribe", "subtitles by the amara org community", "sous titrage société radio canada",
        "merci d avoir regardé", "subtítulos realizados por la comunidad de amara org", "gracias por ver el video",
    )
    private fun normal(text: String) = text.lowercase().map { if (it.isLetter()) it else ' ' }.joinToString("").split(' ').filter(String::isNotEmpty).joinToString(" ")

    /** Verwirft reine Floskeln und hängt sie auch nicht am Ende an („… Untertitel im Auftrag des ZDF“). */
    fun saeubern(text: String): String {
        var t = text.trim()
        if (normal(t) in FLOSKELN) return ""
        FLOSKELN.filter { it.split(' ').size >= 3 }.forEach { f ->
            val satzEnde = Regex("[.!?]\\s*")
            val saetze = t.split(satzEnde).filter(String::isNotBlank)
            if (saetze.size > 1 && normal(saetze.last()).startsWith(f)) t = t.substring(0, t.lastIndexOf(saetze.last())).trim()
        }
        return t
    }
}

/** Aufnahme mit Pegelanzeige, danach Erkennung mit Whisper. Zustände wie beim Android-Diktat. */
class WhisperDiktat(private val context: Context) {
    var zustand by mutableStateOf<DiktatZustand>(DiktatZustand.Bereit)
        private set
    var pegel by mutableFloatStateOf(0f)
        private set
    var sekunden by mutableIntStateOf(0)
        private set
    val hoertZu: Boolean get() = zustand is DiktatZustand.Hoert
    val erkennt: Boolean get() = zustand == DiktatZustand.Pruefe

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    @Volatile private var stopp = false
    @Volatile private var verwerfen = false

    @SuppressLint("MissingPermission") // Die Freigabe prüft der Aufrufer vor jedem Start.
    fun starten(code: String) {
        if (job?.isActive == true) return
        stopp = false; verwerfen = false; sekunden = 0; pegel = 0f
        zustand = DiktatZustand.Hoert("")
        WhisperErkenner.vorwaermen(context, code)
        job = scope.launch {
            val samples = withContext(Dispatchers.IO) { aufnehmen() }
            if (verwerfen) { zustand = DiktatZustand.Bereit; return@launch }
            if (samples == null) { zustand = DiktatZustand.Hinweis("Das Mikrofon ließ sich nicht öffnen. Prüfe, ob eine andere App es gerade benutzt."); return@launch }
            if (DiktatAudio.sprachMs(samples) < 400) { zustand = DiktatZustand.Hinweis("Ich habe nichts verstanden. Sprich etwas näher am Handy und tippe erneut auf „Diktieren“."); return@launch }
            zustand = DiktatZustand.Pruefe
            val text = try { withContext(Dispatchers.Default) { WhisperErkenner.erkenne(context, DiktatAudio.kuerzen(samples), code) } }
                catch (e: CancellationException) { throw e }
                catch (e: Throwable) {
                    android.util.Log.w("WeckerDiktat", "Whisper-Erkennung fehlgeschlagen", e)
                    zustand = DiktatZustand.Hinweis("Die Spracherkennung ist fehlgeschlagen. Bitte versuch es noch einmal."); return@launch
                }
            zustand = if (text.isBlank()) DiktatZustand.Hinweis("Ich habe nichts verstanden. Bitte noch einmal.") else DiktatZustand.Ergebnis(text)
        }
    }

    @SuppressLint("MissingPermission")
    private fun aufnehmen(): FloatArray? {
        val min = AudioRecord.getMinBufferSize(DiktatAudio.RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = runCatching { AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, DiktatAudio.RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, maxOf(min, DiktatAudio.RATE)) }.getOrNull() ?: return null
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return null }
        val gesammelt = java.io.ByteArrayOutputStream()
        val puffer = ShortArray(DiktatAudio.RATE / 10)
        val bytes = java.nio.ByteBuffer.allocate(puffer.size * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        var gesamt = 0
        try {
            rec.startRecording()
            while (!stopp && gesamt < DiktatAudio.RATE * 180) {
                val n = rec.read(puffer, 0, puffer.size)
                if (n <= 0) continue
                var summe = 0.0
                bytes.clear()
                for (i in 0 until n) { summe += puffer[i] * puffer[i].toDouble(); bytes.putShort(puffer[i]) }
                gesammelt.write(bytes.array(), 0, n * 2)
                gesamt += n
                pegel = (sqrt(summe / n) / 6000.0).toFloat().coerceIn(0f, 1f)
                sekunden = gesamt / DiktatAudio.RATE
            }
        } finally { runCatching { rec.stop() }; rec.release(); pegel = 0f }
        val roh = java.nio.ByteBuffer.wrap(gesammelt.toByteArray()).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        return FloatArray(roh.remaining()) { roh.get(it) / 32768f }
    }

    fun beenden() { stopp = true }
    fun abbrechen() { if (hoertZu) { verwerfen = true; stopp = true } }
    fun zeigeHinweis(meldung: String) { zustand = DiktatZustand.Hinweis(meldung) }
    fun zuruecksetzen() { abbrechen(); zustand = DiktatZustand.Bereit }
    fun freigeben() { abbrechen(); scope.cancel() }
}

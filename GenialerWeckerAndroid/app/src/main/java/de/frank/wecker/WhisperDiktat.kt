package de.frank.wecker

import android.annotation.SuppressLint
import android.app.ActivityManager
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * Das Whisper-Sprachmodell für das Offline-Diktat (sherpa-onnx, nur Erkennung, ohne GPL-Anteile).
 * Es wird einmalig geladen und liegt danach im App-Speicher; das Diktat braucht dann kein Internet.
 * „small“ erkennt deutlich besser, „base“ ist die kompakte Fassung für Handys mit wenig Arbeitsspeicher.
 */
object WhisperModell {
    enum class Art(val id: String, val anzeige: String, val groessen: List<Long>) {
        SMALL("small", "Genau", listOf(112_442_483L, 262_226_114L, 816_730L)),
        BASE("base", "Kompakt", listOf(29_120_534L, 130_672_026L, 816_730L));
        val dateien: List<String> get() = listOf("$id-encoder.int8.onnx", "$id-decoder.int8.onnx", "$id-tokens.txt")
        val megabyte: Int get() = (groessen.sum() / 1_048_576L).toInt()
    }
    enum class Status { FEHLT, LAEDT, BEREIT, FEHLER }
    data class Zustand(val status: Status, val fortschritt: Float = 0f, val meldung: String = "")

    private val _zustand = MutableStateFlow(Zustand(Status.FEHLT))
    val zustand = _zustand.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ladeJob: Job? = null
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()

    /** Handys unter 6 GB Arbeitsspeicher bekommen das kompakte Modell, alle anderen das genaue. */
    fun art(context: Context): Art {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(info)
        return if (info.totalMem in 1 until 5_500_000_000L) Art.BASE else Art.SMALL
    }
    fun ordner(context: Context) = File(context.filesDir, "whisper")
    fun datei(context: Context, name: String) = File(ordner(context), name)
    fun bereit(context: Context): Boolean = art(context).let { a -> a.dateien.zip(a.groessen).all { (n, g) -> datei(context, n).length() == g } }

    fun pruefe(context: Context) {
        if (_zustand.value.status == Status.LAEDT) return
        _zustand.value = Zustand(if (bereit(context)) Status.BEREIT else Status.FEHLT)
    }

    /** Lädt fehlende Dateien, setzt abgebrochene Downloads fort (Range) und prüft jede Größe. */
    fun laden(context: Context) {
        if (ladeJob?.isActive == true) return
        val app = context.applicationContext
        val art = art(app)
        ladeJob = scope.launch {
            try {
                ordner(app).mkdirs()
                val gesamt = art.groessen.sum().toFloat()
                var fertig = 0L
                _zustand.value = Zustand(Status.LAEDT, 0f)
                art.dateien.zip(art.groessen).forEach { (name, groesse) ->
                    val ziel = datei(app, name)
                    if (ziel.length() == groesse) { fertig += groesse; return@forEach }
                    val teil = File(ziel.path + ".part")
                    val vorhanden = teil.length().takeIf { it < groesse } ?: 0L.also { teil.delete() }
                    val anfrage = Request.Builder().url("https://huggingface.co/csukuangfj/sherpa-onnx-whisper-${art.id}/resolve/main/$name")
                        .apply { if (vorhanden > 0) header("Range", "bytes=$vorhanden-") }.build()
                    client.newCall(anfrage).execute().use { antwort ->
                        check(antwort.isSuccessful) { "Der Download ist fehlgeschlagen (${antwort.code})." }
                        val anhaengen = vorhanden > 0 && antwort.code == 206
                        RandomAccessFile(teil, "rw").use { raf ->
                            if (anhaengen) raf.seek(vorhanden) else raf.setLength(0)
                            var geschrieben = if (anhaengen) vorhanden else 0L
                            val puffer = ByteArray(256 * 1024)
                            val eingang = antwort.body!!.byteStream()
                            while (true) {
                                ensureActive()
                                val n = eingang.read(puffer)
                                if (n < 0) break
                                raf.write(puffer, 0, n)
                                geschrieben += n
                                _zustand.value = Zustand(Status.LAEDT, ((fertig + geschrieben) / gesamt).coerceIn(0f, 1f))
                            }
                        }
                    }
                    check(teil.length() == groesse) { "Die Datei $name ist unvollständig angekommen. Bitte erneut laden." }
                    check(teil.renameTo(ziel)) { "Das Sprachmodell konnte nicht gespeichert werden." }
                    fertig += groesse
                }
                _zustand.value = Zustand(Status.BEREIT, 1f)
            } catch (e: CancellationException) {
                _zustand.value = Zustand(Status.FEHLT); throw e
            } catch (e: Exception) {
                android.util.Log.w("WeckerDiktat", "Modell-Download fehlgeschlagen", e)
                _zustand.value = Zustand(Status.FEHLER, meldung = if (e is java.io.IOException) "Keine Verbindung. Der Download setzt beim nächsten Versuch dort fort, wo er stand." else e.message ?: "Download fehlgeschlagen.")
            }
        }
    }

    fun abbrechen() { ladeJob?.cancel() }

    fun loeschen(context: Context) {
        abbrechen()
        WhisperErkenner.freigeben()
        ordner(context).listFiles()?.forEach { it.delete() }
        _zustand.value = Zustand(Status.FEHLT)
    }
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
        val art = WhisperModell.art(context)
        val (enc, dec, tok) = art.dateien.map { WhisperModell.datei(context, it).absolutePath }
        val neu = OfflineRecognizer(config = OfflineRecognizerConfig(modelConfig = OfflineModelConfig(
            whisper = OfflineWhisperModelConfig(encoder = enc, decoder = dec, language = code, task = "transcribe"),
            tokens = tok, numThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4))))
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

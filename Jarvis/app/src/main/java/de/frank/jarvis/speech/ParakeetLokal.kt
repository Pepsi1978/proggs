package de.frank.jarvis.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.weckwort.WeckwortErkenner
import de.frank.module.transkriptionsmodul.SileroVad
import de.frank.module.transkriptionsmodul.SpeechAnalyzer
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Offline-Diktat mit parakeet-primeline: deutscher Fine-Tune von nvidia/parakeet-tdt-0.6b-v3, int8-ONNX über
 * sherpa-onnx, nur CPU. Genau das Modell aus github.com/winidi/dictate (Hugging Face flozen1981/parakeet-primeline-onnx,
 * festgelegte Revision). Die Dateien (rund 670 MB) liegen nicht in der APK, sondern in [ordner]: einmal
 * herunterladen ([herunterladen]) oder per adb hineinlegen. Danach kein Netz, kein Schlüssel.
 *
 * Fehlt das Modell oder scheitert es, gibt es einen Fehler, nie einen Rückfall auf Groq.
 * Lizenz CC-BY-4.0: primeline (Fine-Tune), NVIDIA (Basismodell), k2-fsa/sherpa-onnx (Laufzeit).
 */
object ParakeetLokal {
    private const val REPO = "flozen1981/parakeet-primeline-onnx"
    private const val REVISION = "d548e25b9bfe559aa274f361892dc4ed5d64743a"

    /** Dateien mit ihrer Größe in genau dieser Revision; die Größe dient als Vollständigkeitsprüfung. */
    val DATEIEN = linkedMapOf(
        "tokens.txt" to 102_132L,
        "decoder.int8.onnx" to 11_845_275L,
        "joiner.int8.onnx" to 6_355_277L,
        "encoder.int8.onnx" to 1_548_009L,
        // Die Gewichte des Encoders; sie müssen neben encoder.int8.onnx liegen.
        "encoder.int8.onnx.data" to 650_776_320L,
    )
    val GESAMT_BYTES = DATEIEN.values.sum()

    private const val ABTASTRATE = 16_000
    private const val THREADS = 4

    /** Der Encoder scheitert über 400 s und braucht davor quadratisch mehr Speicher: längere Diktate in Stücken. */
    private const val MAX_STUECK_S = 90
    private const val SUCH_S = 15
    private const val FENSTER_S = 0.4

    /** Ein langes Stück, das leer zurückkommt, noch einmal in kurzen Teilen (wie in dictate). */
    private const val ZWEITER_VERSUCH_S = 20

    /** Leise Aufnahmen kommen sonst leer zurück: auf diese Spitze anheben, höchstens um [MAX_VERSTAERKUNG]. */
    private const val ZIEL_SPITZE = 0.5f
    private const val MAX_VERSTAERKUNG = 30f

    fun ordner(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "parakeet-primeline-onnx")

    /** Alle Dateien da und vollständig. */
    fun bereit(context: Context): Boolean {
        val o = ordner(context)
        return DATEIEN.all { (name, groesse) -> File(o, name).let { it.isFile && it.length() == groesse } }
    }

    fun belegteBytes(context: Context): Long =
        ordner(context).listFiles()?.sumOf { it.length() } ?: 0L

    /** Messwerte für die Einstellungen; null, solange nichts gemessen ist. */
    data class Messung(val ladeMs: Long, val ramMb: Long, val audioMs: Long = 0, val rechenMs: Long = 0)

    @Volatile
    var messung: Messung? = null
        private set

    private val sperre = Mutex()
    private var erkenner: OfflineRecognizer? = null

    /** Lädt das Modell im Hintergrund vor (beim Wählen der Option oder beim Aufnahmestart), damit das Diktat nicht wartet. */
    suspend fun vorwaermen(context: Context) {
        if (!bereit(context)) return
        runCatching { sperre.withLock { laden(context.applicationContext) } }
            .onFailure { Logbuch.fehler("Parakeet laden", it) }
    }

    /** Gibt das Modell aus dem Arbeitsspeicher frei (Option abgewählt oder Modell gelöscht). */
    suspend fun freigeben() = sperre.withLock {
        erkenner?.release()
        erkenner = null
    }

    private suspend fun laden(context: Context): OfflineRecognizer = erkenner ?: withContext(Dispatchers.Default) {
        if (!bereit(context)) throw ParakeetFehler("Das Offline-Modell ist noch nicht eingerichtet. Lade es unter Einstellungen → Sprache → Spracheingabe herunter.")
        // Auf dem Fold 8 (SME ohne SME2) stürzen die KleidiAI-Kerne von ORT ab. Der Schalter gilt für die ganze ONNX Runtime,
        // auch für sherpa-onnx, aber erst, wenn eine Java-Sitzung ihn gesetzt hat: darum vorher sicher eine anlegen.
        if (WeckwortErkenner.KLEIDIAI_ABSCHALTEN) SileroVad(context, true).close()
        val o = ordner(context)
        val vorher = android.os.Debug.getNativeHeapAllocatedSize() + rssBytes()
        val beginn = System.currentTimeMillis()
        val config = OfflineRecognizerConfig().apply {
            featConfig = FeatureConfig().apply { sampleRate = ABTASTRATE; featureDim = 128 }
            modelConfig = OfflineModelConfig().apply {
                transducer = OfflineTransducerModelConfig().apply {
                    encoder = File(o, "encoder.int8.onnx").absolutePath
                    decoder = File(o, "decoder.int8.onnx").absolutePath
                    joiner = File(o, "joiner.int8.onnx").absolutePath
                }
                tokens = File(o, "tokens.txt").absolutePath
                numThreads = THREADS
                provider = "cpu"
                modelType = "nemo_transducer"
            }
            decodingMethod = "greedy_search"
        }
        // Ohne AssetManager liest sherpa-onnx die Dateien vom Pfad; nur so findet ORT die externen Gewichte (.data).
        val neu = OfflineRecognizer(null, config)
        // Aufwärmen: Die erste echte Mitschrift wäre sonst etwa doppelt so langsam.
        repeat(2) { dekodiere(neu, FloatArray(ABTASTRATE)) }
        val ladeMs = System.currentTimeMillis() - beginn
        val ramMb = ((android.os.Debug.getNativeHeapAllocatedSize() + rssBytes() - vorher) / 2 / 1_048_576).coerceAtLeast(0)
        messung = Messung(ladeMs, ramMb)
        Logbuch.sprache("Parakeet geladen", "threads=$THREADS ram≈${ramMb} MB", ladeMs)
        erkenner = neu
        neu
    }

    /** Resident Set Size des Prozesses (aus /proc), ergänzt den Native-Heap, weil ORT die Gewichte auch mappt. */
    private fun rssBytes(): Long = runCatching {
        File("/proc/self/statm").readText().trim().split(" ")[1].toLong() * 4096
    }.getOrDefault(0L)

    /**
     * Mitschrift einer WAV aus dem MicRecorder (16 kHz, mono, 16 Bit, 44 Byte Kopf), ganz auf dem Handy.
     * [stilleFilter]: ohne erkannte Stimme gar nicht erst rechnen (gleicher Schalter wie „Stille vorab“).
     */
    suspend fun transkribiere(context: Context, wav: ByteArray, stimmhaft: BooleanArray?, stilleFilter: Boolean): String {
        if (stilleFilter) {
            val analyse = SpeechAnalyzer().analyze(wav, stimmhaft)
            if (analyse != null && analyse.voicedMs < SpeechAnalyzer.MIN_SPEECH_MS) return ""
        }
        val proben = alsFloat(wav)
        return sperre.withLock {
            val r = laden(context.applicationContext)
            withContext(Dispatchers.Default) {
                val beginn = System.currentTimeMillis()
                val text = schneide(proben, MAX_STUECK_S).map { stueck ->
                    dekodiere(r, stueck).ifEmpty {
                        if (stueck.size > ZWEITER_VERSUCH_S * ABTASTRATE) schneide(stueck, ZWEITER_VERSUCH_S).map { dekodiere(r, it) }.filter { it.isNotEmpty() }.joinToString(" ")
                        else ""
                    }
                }.filter { it.isNotEmpty() }.joinToString(" ")
                val rechenMs = System.currentTimeMillis() - beginn
                val audioMs = proben.size * 1000L / ABTASTRATE
                messung = (messung ?: Messung(0, 0)).copy(audioMs = audioMs, rechenMs = rechenMs)
                Logbuch.sprache("Parakeet-Mitschrift", "audio=${audioMs} ms zeichen=${text.length}", rechenMs)
                text
            }
        }
    }

    private fun dekodiere(r: OfflineRecognizer, stueck: FloatArray): String {
        var spitze = 0f
        for (w in stueck) spitze = maxOf(spitze, abs(w))
        val eingabe = if (spitze > 0f && spitze < ZIEL_SPITZE) {
            val faktor = minOf(ZIEL_SPITZE / spitze, MAX_VERSTAERKUNG)
            FloatArray(stueck.size) { stueck[it] * faktor }
        } else stueck
        val strom = r.createStream()
        try {
            strom.acceptWaveform(eingabe, ABTASTRATE)
            r.decode(strom)
            return r.getResult(strom).text.trim()
        } finally {
            strom.release()
        }
    }

    private fun alsFloat(wav: ByteArray): FloatArray {
        val kopf = 44
        val n = ((wav.size - kopf).coerceAtLeast(0)) / 2
        return FloatArray(n) { i ->
            val o = kopf + 2 * i
            ((wav[o + 1].toInt() shl 8) or (wav[o].toInt() and 0xFF)).toShort() / 32_768f
        }
    }

    /** Stücke von höchstens [maxS] Sekunden, geschnitten an der leisesten Stelle kurz vor dem Ende. */
    private fun schneide(proben: FloatArray, maxS: Int): List<FloatArray> {
        val maxN = maxS * ABTASTRATE
        val suchN = minOf(SUCH_S, maxS / 2) * ABTASTRATE
        val fensterN = (FENSTER_S * ABTASTRATE).toInt()
        val teile = mutableListOf<FloatArray>()
        var anfang = 0
        while (proben.size - anfang > maxN) {
            val suchAnfang = anfang + maxN - suchN
            var bestes = suchAnfang
            var bestesMass = Double.MAX_VALUE
            var p = suchAnfang
            while (p + fensterN <= anfang + maxN) {
                var e = 0.0
                for (i in p until p + fensterN) e += proben[i] * proben[i]
                if (e < bestesMass) { bestesMass = e; bestes = p }
                p += fensterN
            }
            val schnitt = bestes + fensterN / 2
            teile += proben.copyOfRange(anfang, schnitt)
            anfang = schnitt
        }
        teile += proben.copyOfRange(anfang, proben.size)
        return teile
    }

    /**
     * Lädt die Modelldateien der festgelegten Revision von Hugging Face (einmalig, rund 670 MB). Fertige Dateien
     * werden übersprungen; unterbrochen geht es mit der Teildatei weiter.
     */
    suspend fun herunterladen(context: Context, fortschritt: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        val o = ordner(context).apply { mkdirs() }
        val client = OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build()
        var fertig = 0L
        for ((name, groesse) in DATEIEN) {
            val ziel = File(o, name)
            if (ziel.isFile && ziel.length() == groesse) { fertig += groesse; fortschritt(fertig, GESAMT_BYTES); continue }
            val teil = File(o, "$name.part")
            val schon = if (teil.isFile && teil.length() < groesse) teil.length() else { teil.delete(); 0L }
            val anfrage = Request.Builder().url("https://huggingface.co/$REPO/resolve/$REVISION/$name")
                .apply { if (schon > 0) header("Range", "bytes=$schon-") }.build()
            client.newCall(anfrage).execute().use { antwort ->
                if (!antwort.isSuccessful) throw ParakeetFehler("Download von $name fehlgeschlagen (HTTP ${antwort.code}).")
                val anhaengen = schon > 0 && antwort.code == 206
                var geladen = if (anhaengen) schon else 0L
                java.io.FileOutputStream(teil, anhaengen).use { aus ->
                    val ein = antwort.body!!.byteStream()
                    val puffer = ByteArray(256 * 1024)
                    var letzte = 0L
                    while (true) {
                        ensureActive()
                        val n = ein.read(puffer)
                        if (n < 0) break
                        aus.write(puffer, 0, n)
                        geladen += n
                        if (geladen - letzte > 4 * 1_048_576) { letzte = geladen; fortschritt(fertig + geladen, GESAMT_BYTES) }
                    }
                }
                if (teil.length() != groesse) throw ParakeetFehler("$name ist unvollständig (${teil.length()} von $groesse Byte).")
                ziel.delete()
                if (!teil.renameTo(ziel)) throw ParakeetFehler("$name ließ sich nicht ablegen.")
            }
            fertig += groesse
            fortschritt(fertig, GESAMT_BYTES)
        }
        Logbuch.sprache("Parakeet-Modell geladen", "${GESAMT_BYTES / 1_048_576} MB")
    }

    /** Löscht die Modelldateien vom Handy. */
    suspend fun loeschen(context: Context) {
        freigeben()
        ordner(context).deleteRecursively()
        messung = null
    }
}

class ParakeetFehler(message: String) : Exception(message)

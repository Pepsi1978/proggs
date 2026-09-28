package de.frank.wecker

import android.content.Context
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsPocketModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Eine lokale Neural-Stimme aus einem mitgelieferten Modell (Test). [sprachen] null = alle App-Sprachen.
 * [referenz] ist bei Pocket die Stimmprobe (Asset-Pfad), aus der das Modell die Stimme übernimmt.
 */
data class ModellStimme(val id: String, val modell: String, val name: String, val art: String,
    val sid: Int = 0, val referenz: String? = null, val sprachen: Set<String>? = null)

/**
 * Test-Katalog der Modellstimmen: Supertonic 3 (31 Sprachen, OpenRAIL-M) und Pocket TTS (nur Englisch, Januar-Modell;
 * die neueren Sprachmodelle versteht sherpa-onnx 1.13.8 noch nicht, Issue #3755). Beide laufen ohne espeak (GPL).
 */
object ModellKatalog {
    const val PROVIDER = "modell_tts"
    val STIMMEN: List<ModellStimme> =
        (0 until 10).map { ModellStimme("modell:supertonic:$it", "supertonic", "Supertonic ${it + 1}", "Supertonic 3 · offline", sid = it) } +
        listOf("bria" to "Bria", "loona" to "Loona").map { (datei, name) ->
            ModellStimme("modell:pocket:$datei", "pocket", "Pocket $name", "Pocket TTS · offline · nur Englisch",
                referenz = "tts/pocket/test_wavs/$datei.wav", sprachen = setOf("en"))
        }

    fun istModell(id: String): Boolean = STIMMEN.any { it.id == id }
    fun finde(id: String): ModellStimme? = STIMMEN.firstOrNull { it.id == id }
    fun fuer(sprache: String): List<ModellStimme> = STIMMEN.filter { it.sprachen == null || sprache in it.sprachen }
    /** Spricht die Stimme [id] die Sprache [sprache]? */
    fun passt(id: String, sprache: String): Boolean = finde(id)?.let { it.sprachen == null || Sprachen.gueltig(sprache) in it.sprachen } == true
}

/**
 * Synthese mit den Modellstimmen über sherpa-onnx (eigener Bau ohne espeak, siehe docs/sherpa-onnx-asr-only).
 * Die Modelle liegen als Assets in der APK. Immer nur ein Modell im Speicher; nach kurzer Ruhe wird es freigegeben.
 */
object ModellStimmen {
    private const val RUHE_MS = 60_000L
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var geladen: Pair<String, OfflineTts>? = null
    private var freigabe: Job? = null
    private val referenzen = HashMap<String, Pair<FloatArray, Int>>()

    /** Erzeugt [text] als WAV-Datei [ziel]. */
    suspend fun synthetisiere(context: Context, text: String, stimme: SyntheseStimme, ziel: File) = mutex.withLock {
        val s = ModellKatalog.finde(stimme.stimme) ?: throw SyntheseAbbruch("Unbekannte Modellstimme „${stimme.stimme}“.")
        val sprache = Sprachen.gueltig(stimme.sprache)
        if (s.sprachen != null && sprache !in s.sprachen) throw SyntheseAbbruch("${s.name} spricht kein ${Sprachen.name(sprache)}.")
        freigabe?.cancel(); freigabe = null
        try {
            withContext(Dispatchers.IO) {
                val tts = instanz(context, s.modell)
                val tempo = stimme.ttsSpeechRate.coerceIn(.5f, 2f)
                val gen = if (s.modell == "supertonic") {
                    GenerationConfig(sid = s.sid, speed = tempo, numSteps = 8, extra = mapOf("lang" to sprache))
                } else {
                    val (samples, rate) = referenz(context, s.referenz!!)
                    GenerationConfig(speed = tempo, referenceAudio = samples, referenceSampleRate = rate, numSteps = 5,
                        extra = mapOf("temperature" to "0.7", "chunk_size" to "15"))
                }
                val audio = tts.generateWithConfig(text, gen)
                if (audio.samples.isEmpty()) throw SyntheseAbbruch("${s.name} hat kein Audio erzeugt.")
                if (!audio.save(ziel.absolutePath)) throw SyntheseAbbruch("Das Audio von ${s.name} ließ sich nicht speichern.")
            }
        } finally { spaeterFreigeben() }
    }

    private fun instanz(context: Context, modell: String): OfflineTts {
        geladen?.let { (name, tts) -> if (name == modell) return tts else { tts.release(); geladen = null } }
        val config = if (modell == "supertonic") OfflineTtsModelConfig(numThreads = 4, supertonic = OfflineTtsSupertonicModelConfig(
            durationPredictor = "tts/supertonic/duration_predictor.int8.onnx",
            textEncoder = "tts/supertonic/text_encoder.int8.onnx",
            vectorEstimator = "tts/supertonic/vector_estimator.int8.onnx",
            vocoder = "tts/supertonic/vocoder.int8.onnx",
            ttsJson = "tts/supertonic/tts.json",
            unicodeIndexer = "tts/supertonic/unicode_indexer.bin",
            voiceStyle = "tts/supertonic/voice.bin",
        )) else OfflineTtsModelConfig(numThreads = 4, pocket = OfflineTtsPocketModelConfig(
            lmFlow = "tts/pocket/lm_flow.int8.onnx",
            lmMain = "tts/pocket/lm_main.int8.onnx",
            encoder = "tts/pocket/encoder.onnx",
            decoder = "tts/pocket/decoder.int8.onnx",
            textConditioner = "tts/pocket/text_conditioner.onnx",
            vocabJson = "tts/pocket/vocab.json",
            tokenScoresJson = "tts/pocket/token_scores.json",
        ))
        val tts = OfflineTts(context.assets, OfflineTtsConfig(model = config))
        geladen = modell to tts
        return tts
    }

    /** Stimmprobe als Mono-Float-Samples; WAV mit 16-Bit-PCM oder 32-Bit-Float. */
    private fun referenz(context: Context, pfad: String): Pair<FloatArray, Int> = referenzen.getOrPut(pfad) {
        val b = ByteBuffer.wrap(context.assets.open(pfad).use { it.readBytes() }).order(ByteOrder.LITTLE_ENDIAN)
        var kanaele = 1; var rate = 24_000; var bits = 16; var format = 1
        var pos = 12
        while (pos + 8 <= b.limit()) {
            val id = String(ByteArray(4) { b.get(pos + it) }, Charsets.US_ASCII)
            val laenge = b.getInt(pos + 4)
            val start = pos + 8
            if (id == "fmt ") {
                format = b.getShort(start).toInt(); kanaele = b.getShort(start + 2).toInt()
                rate = b.getInt(start + 4); bits = b.getShort(start + 14).toInt()
            } else if (id == "data") {
                val bytesJe = bits / 8
                val frames = minOf(laenge, b.limit() - start) / (bytesJe * kanaele)
                val samples = FloatArray(frames) { f ->
                    var summe = 0f
                    for (k in 0 until kanaele) {
                        val o = start + (f * kanaele + k) * bytesJe
                        summe += if (format == 3 && bits == 32) b.getFloat(o) else b.getShort(o) / 32768f
                    }
                    summe / kanaele
                }
                return@getOrPut samples to rate
            }
            pos = start + laenge + (laenge and 1)
        }
        throw SyntheseAbbruch("Die Stimmprobe $pfad ist keine lesbare WAV-Datei.")
    }

    private fun spaeterFreigeben() {
        freigabe?.cancel()
        freigabe = scope.launch {
            delay(RUHE_MS)
            mutex.withLock { geladen?.second?.release(); geladen = null }
        }
    }
}

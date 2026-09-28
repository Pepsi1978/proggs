package de.frank.wecker

import android.content.Context
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsPocketModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Eine lokale Neural-Stimme aus einem mitgelieferten Modell. [sprachen] null = alle App-Sprachen.
 * [referenz] ist bei Pocket die Stimmprobe (Asset-Pfad), aus der das Modell die Stimme übernimmt.
 * Der angezeigte Name hängt von Sprache und Region ab (siehe [ModellKatalog.anzeige]); die Kennung nie.
 */
data class ModellStimme(val id: String, val modell: String, val weiblich: Boolean, val nummer: Int,
    val sid: Int = 0, val referenz: String? = null, val sprachen: Set<String>? = null, val eigenname: String? = null)

/**
 * Die Vorlesestimmen der App, alle lokal: Supertonic 3 (ein Modell für alle Sprachen, fp32, 5 weibliche + 5 männliche
 * Stimmen: sid 0–4 = F1–F5, sid 5–9 = M1–M5, alphabetisch im voice.bin) und Pocket TTS (nur Englisch, zwei weibliche
 * Stimmen; die neueren Sprachmodelle versteht sherpa-onnx 1.13.8 noch nicht, Issue #3755). Beide ohne espeak (GPL).
 */
object ModellKatalog {
    const val PROVIDER = "modell_tts"
    /** Vorgabe jeder Sprache: Supertonic F4. */
    const val VORGABE = "modell:supertonic:3"
    val STIMMEN: List<ModellStimme> =
        (0 until 10).map { ModellStimme("modell:supertonic:$it", "supertonic", weiblich = it < 5, nummer = it % 5, sid = it) } +
        listOf("bria" to "Bria", "loona" to "Loona").mapIndexed { i, (datei, name) ->
            ModellStimme("modell:pocket:$datei", "pocket", weiblich = true, nummer = i,
                referenz = "tts/pocket/test_wavs/$datei.wav", sprachen = setOf("en"), eigenname = name)
        }

    /** Landestypische Vornamen je Sprache/Region: je fünf weibliche und fünf männliche, in Stimmreihenfolge. */
    private val NAMEN: Map<String, Pair<List<String>, List<String>>> = mapOf(
        "de" to (listOf("Lea", "Nina", "Hannah", "Sophie", "Marie") to listOf("Lukas", "Jonas", "Felix", "Paul", "Max")),
        "en-US" to (listOf("Emma", "Olivia", "Ava", "Mia", "Grace") to listOf("Liam", "Noah", "Ethan", "James", "Mason")),
        "en-GB" to (listOf("Amelia", "Isla", "Poppy", "Freya", "Charlotte") to listOf("Oliver", "Harry", "George", "Alfie", "Arthur")),
        "fr" to (listOf("Chloé", "Camille", "Manon", "Inès", "Juliette") to listOf("Louis", "Hugo", "Gabriel", "Jules", "Théo")),
        "es" to (listOf("Lucía", "Sofía", "Carmen", "Valeria", "Elena") to listOf("Mateo", "Pablo", "Javier", "Diego", "Álvaro")),
        "pt-PT" to (listOf("Beatriz", "Leonor", "Inês", "Matilde", "Carolina") to listOf("Tiago", "Duarte", "Rodrigo", "Afonso", "Tomás")),
        "pt-BR" to (listOf("Ana", "Júlia", "Larissa", "Camila", "Fernanda") to listOf("João", "Pedro", "Lucas", "Rafael", "Gabriel")),
    )

    /** Namensvariante für Sprache + Geräteregion (Englisch GB/US, Portugiesisch PT/BR). */
    private fun variante(sprache: String, region: String): String = when (Sprachen.gueltig(sprache)) {
        "en" -> if (region.uppercase() in setOf("GB", "IE", "AU", "NZ")) "en-GB" else "en-US"
        "pt" -> if (region.uppercase() in setOf("PT", "AO", "MZ", "CV")) "pt-PT" else "pt-BR"
        else -> Sprachen.gueltig(sprache)
    }

    fun name(s: ModellStimme, sprache: String, region: String = ""): String {
        s.eigenname?.let { return it }
        val (w, m) = NAMEN.getValue(variante(sprache, region))
        return (if (s.weiblich) w else m)[s.nummer]
    }
    /** „Lea · weiblich“ */
    fun anzeige(s: ModellStimme, sprache: String, region: String = ""): String = "${name(s, sprache, region)} · ${if (s.weiblich) "weiblich" else "männlich"}"

    fun istModell(id: String): Boolean = STIMMEN.any { it.id == id }
    fun finde(id: String): ModellStimme? = STIMMEN.firstOrNull { it.id == id }
    fun fuer(sprache: String): List<ModellStimme> = STIMMEN.filter { it.sprachen == null || Sprachen.gueltig(sprache) in it.sprachen }
    /** Spricht die Stimme [id] die Sprache [sprache]? */
    fun passt(id: String, sprache: String): Boolean = finde(id)?.let { it.sprachen == null || Sprachen.gueltig(sprache) in it.sprachen } == true
}

/**
 * Synthese mit den Modellstimmen über sherpa-onnx (eigener Bau ohne espeak, siehe docs/sherpa-onnx-asr-only).
 * Die Modelle liegen als Assets in der APK. Immer nur ein Modell im Speicher; nach kurzer Ruhe wird es freigegeben.
 */
object ModellStimmen {
    /** Rechenschritte von Supertonic: 40 klingt deutlich sauberer als 10/20 (Hörvergleich 28.09.2026), kostet etwa 4× Zeit. */
    const val SCHRITTE = 40
    private const val RUHE_MS = 60_000L
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var geladen: Pair<String, OfflineTts>? = null
    private var freigabe: Job? = null
    private val referenzen = HashMap<String, Pair<FloatArray, Int>>()

    /** Fortschritt der gerade laufenden Synthese, 0..1; wächst gleichmäßig aus der geschätzten Rechenzeit. */
    private val _fortschritt = MutableStateFlow(0f)
    val fortschritt: StateFlow<Float> = _fortschritt
    /** Gemessene Rechenzeit je Zeichen (s), gleitend nachkalibriert – Startwert für 40 Schritte auf einem Mittelklasse-Handy. */
    @Volatile private var sekundenJeZeichen = 0.12

    /** Erzeugt [text] als WAV-Datei [ziel]. */
    suspend fun synthetisiere(context: Context, text: String, stimme: SyntheseStimme, ziel: File) = mutex.withLock {
        val s = ModellKatalog.finde(stimme.stimme) ?: throw SyntheseAbbruch("Unbekannte Modellstimme „${stimme.stimme}“.")
        val sprache = Sprachen.gueltig(stimme.sprache)
        if (s.sprachen != null && sprache !in s.sprachen) throw SyntheseAbbruch("${ModellKatalog.name(s, sprache)} spricht kein ${Sprachen.name(sprache)}.")
        freigabe?.cancel(); freigabe = null
        try {
            withContext(Dispatchers.IO) {
                val tts = instanz(context, s.modell)
                val tempo = stimme.ttsSpeechRate.coerceIn(.5f, 2f)
                val gen = if (s.modell == "supertonic") {
                    GenerationConfig(sid = s.sid, speed = tempo, numSteps = SCHRITTE, extra = mapOf("lang" to sprache))
                } else {
                    val (samples, rate) = referenz(context, s.referenz!!)
                    GenerationConfig(speed = tempo, referenceAudio = samples, referenceSampleRate = rate, numSteps = 5,
                        extra = mapOf("temperature" to "0.7", "chunk_size" to "15"))
                }
                val zeichen = text.length.coerceAtLeast(1)
                val erwartet = (sekundenJeZeichen * zeichen).coerceAtLeast(1.0)
                val start = System.nanoTime()
                _fortschritt.value = 0f
                val uhr = scope.launch {
                    while (isActive) {
                        val vergangen = (System.nanoTime() - start) / 1e9
                        _fortschritt.value = (vergangen / erwartet).toFloat().coerceAtMost(.97f)
                        delay(200)
                    }
                }
                val audio = try { tts.generateWithConfig(text, gen) } finally { uhr.cancel() }
                val dauer = (System.nanoTime() - start) / 1e9
                sekundenJeZeichen = sekundenJeZeichen * .6 + dauer / zeichen * .4
                _fortschritt.value = 1f
                if (audio.samples.isEmpty()) throw SyntheseAbbruch("Die Stimme hat kein Audio erzeugt.")
                if (!audio.save(ziel.absolutePath)) throw SyntheseAbbruch("Das Audio ließ sich nicht speichern.")
            }
        } finally { spaeterFreigeben() }
    }

    private fun instanz(context: Context, modell: String): OfflineTts {
        geladen?.let { (name, tts) -> if (name == modell) return tts else { tts.release(); geladen = null } }
        val config = if (modell == "supertonic") OfflineTtsModelConfig(numThreads = 4, supertonic = OfflineTtsSupertonicModelConfig(
            durationPredictor = "tts/supertonic/duration_predictor.onnx",
            textEncoder = "tts/supertonic/text_encoder.onnx",
            vectorEstimator = "tts/supertonic/vector_estimator.onnx",
            vocoder = "tts/supertonic/vocoder.onnx",
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

    /**
     * Vorab erzeugte Hörprobe (40 Schritte, Probesatz der Sprache) aus assets/stimmproben, einmal in den Cache kopiert;
     * null, wenn es für diese Stimme/Sprache keine gibt. Erzeugt von docs/stimmproben/erzeuge_stimmproben.py.
     */
    fun probeDatei(context: Context, stimme: SyntheseStimme): File? {
        val s = ModellKatalog.finde(stimme.stimme)?.takeIf { stimme.istModell } ?: return null
        val sprache = Sprachen.gueltig(stimme.sprache)
        val datei = if (s.modell == "supertonic") "supertonic-${s.sid}.ogg" else "pocket-${s.referenz!!.substringAfterLast('/').removeSuffix(".wav")}.ogg"
        val ziel = File(context.cacheDir, "stimmproben/$sprache/$datei")
        if (ziel.length() > 0) return ziel
        return runCatching {
            ziel.parentFile?.mkdirs()
            val tmp = File(ziel.path + ".tmp")
            context.assets.open("stimmproben/$sprache/$datei").use { ein -> tmp.outputStream().use { ein.copyTo(it) } }
            check(tmp.renameTo(ziel)); ziel
        }.getOrNull()
    }

    private fun spaeterFreigeben() {
        freigabe?.cancel()
        freigabe = scope.launch {
            delay(RUHE_MS)
            mutex.withLock { geladen?.second?.release(); geladen = null }
        }
    }
}

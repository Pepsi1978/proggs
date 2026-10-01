package de.frank.wecker

import android.content.Context
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import de.frank.genialeideen.speech.SyntheseAbbruch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/** Eine lokale Supertonic-Stimme; [nummer] zählt innerhalb eines Geschlechts (0–4). */
data class ModellStimme(val id: String, val weiblich: Boolean, val nummer: Int, val sid: Int)

/**
 * Die lokalen Vorlesestimmen aus GenialerWeckerAndroid: Supertonic 3 (ein Modell für alle Sprachen, fp32,
 * 5 weibliche + 5 männliche Stimmen: sid 0–4 = F1–F5, sid 5–9 = M1–M5). Rechnet komplett auf dem Handy,
 * ohne Internet und ohne Schlüssel. Die Namen sind dieselben deutschen Vornamen wie in der Verkaufs-App.
 * Pocket TTS (nur Englisch) ist bewusst nicht dabei: Diese App liest Deutsch.
 */
object ModellKatalog {
    /** Vorgabe: Supertonic F4 („Sophie“). */
    const val VORGABE = "modell:supertonic:3"
    val STIMMEN: List<ModellStimme> = (0 until 10).map { ModellStimme("modell:supertonic:$it", weiblich = it < 5, nummer = it % 5, sid = it) }
    private val WEIBLICH = listOf("Lea", "Nina", "Hannah", "Sophie", "Marie")
    private val MAENNLICH = listOf("Lukas", "Jonas", "Felix", "Paul", "Max")

    fun name(s: ModellStimme): String = (if (s.weiblich) WEIBLICH else MAENNLICH)[s.nummer]
    /** „Lea · weiblich“ */
    fun anzeige(s: ModellStimme): String = "${name(s)} · ${if (s.weiblich) "weiblich" else "männlich"}"
    fun finde(id: String): ModellStimme? = STIMMEN.firstOrNull { it.id == id }
}

/**
 * Synthese mit den Modellstimmen über sherpa-onnx (eigener Bau ohne espeak, siehe GenialerWeckerAndroid/docs/sherpa-onnx-asr-only).
 * Die Modelle lädt die App einmal herunter (SupertonicModell). Immer nur ein Modell im Speicher; nach kurzer Ruhe wird es freigegeben.
 */
object ModellStimmen {
    /** Rechenschritte von Supertonic: 40 klingt deutlich sauberer als 10/20 (Hörvergleich 28.09.2026), kostet etwa 4× Zeit. */
    const val SCHRITTE = 40
    private const val RUHE_MS = 60_000L
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var geladen: Pair<String, OfflineTts>? = null
    private var freigabe: Job? = null

    /** Fortschritt der gerade laufenden Synthese, 0..1; wächst gleichmäßig aus der geschätzten Rechenzeit. */
    private val _fortschritt = MutableStateFlow(0f)
    val fortschritt: StateFlow<Float> = _fortschritt
    /** Gemessene Rechenzeit je Zeichen (s), gleitend nachkalibriert – Startwert für 40 Schritte auf einem Mittelklasse-Handy. */
    @Volatile private var sekundenJeZeichen = 0.12

    /** Erzeugt [text] mit der Stimme [stimmeId] und dem Tempo [tempoWunsch] als WAV-Datei [ziel]. */
    suspend fun synthetisiere(context: Context, text: String, stimmeId: String, tempoWunsch: Float, ziel: File, sprache: String = "de") = mutex.withLock {
        val s = ModellKatalog.finde(stimmeId) ?: ModellKatalog.finde(ModellKatalog.VORGABE)!!
        freigabe?.cancel(); freigabe = null
        try {
            withContext(Dispatchers.IO) {
                val tts = instanz(context, "supertonic")
                val tempo = tempoWunsch.coerceIn(.5f, 2f)
                val gen = GenerationConfig(sid = s.sid, speed = tempo, numSteps = SCHRITTE, extra = mapOf("lang" to sprache))
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
        if (!SupertonicModell.geladen(context))
            throw SyntheseAbbruch("Die Supertonic-Stimmen sind noch nicht geladen. Einstellungen → Vorlesen → „Stimmen laden“ (${SupertonicModell.mb}).")
        SupertonicModell.steuerdateienBereitstellen(context)
        fun pfad(name: String) = SupertonicModell.datei(context, name).absolutePath
        val config = OfflineTtsModelConfig(numThreads = 4, supertonic = OfflineTtsSupertonicModelConfig(
            durationPredictor = pfad("duration_predictor.onnx"),
            textEncoder = pfad("text_encoder.onnx"),
            vectorEstimator = pfad("vector_estimator.onnx"),
            vocoder = pfad("vocoder.onnx"),
            ttsJson = pfad("tts.json"),
            unicodeIndexer = pfad("unicode_indexer.bin"),
            voiceStyle = pfad("voice.bin"),
        ))
        val tts = OfflineTts(null, OfflineTtsConfig(model = config))
        geladen = modell to tts
        return tts
    }

    /**
     * Vorab erzeugte Hörprobe (40 Schritte, deutscher Probesatz) aus assets/stimmproben, einmal in den Cache kopiert;
     * null, wenn es für diese Stimme keine gibt. Übernommen aus GenialerWeckerAndroid.
     */
    fun probeDatei(context: Context, stimmeId: String): File? {
        val s = ModellKatalog.finde(stimmeId) ?: return null
        val ziel = File(context.cacheDir, "stimmproben/de/supertonic-${s.sid}.ogg")
        if (ziel.length() > 0) return ziel
        return runCatching {
            ziel.parentFile?.mkdirs()
            val tmp = File(ziel.path + ".tmp")
            context.assets.open("stimmproben/de/supertonic-${s.sid}.ogg").use { ein -> tmp.outputStream().use { ein.copyTo(it) } }
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

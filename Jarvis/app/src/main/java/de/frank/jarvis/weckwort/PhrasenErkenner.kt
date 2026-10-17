package de.frank.jarvis.weckwort

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig

/**
 * Erkennt eine frei gewählte Weckwort-Phrase mit dem KeywordSpotter von sherpa-onnx (Modell in assets/kws/:
 * `sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01`, int8, englisch, wie im Windows-VoiceAgent).
 *
 * Das Modell kennt nur englische Laute und erkennt Tokens, keinen Klartext: Die Phrase zerlegt [PhrasenTokens].
 * Bekannte Schwäche (Bug-Almanach android/voice-assistant-trigger §7): Das Gigaspeech-Modell weckt bei manchen
 * Wörtern schwach auf. Deshalb Boost und Schwelle je nach Empfindlichkeit, und „Hey Jarvis“ bleibt wählbar.
 */
class PhrasenErkenner(
    context: Context,
    /** Tokens der Phrase, genau wie eingestellt. */
    private val tokens: String,
    schwelle: Float,
    /** Aussprache-Varianten (Tokens), nur im [breit]en Modus: wenn die Stimmerkennung Fremde aussiebt. */
    private val varianten: List<String> = emptyList(),
) : AutoCloseable {
    private val spotter = KeywordSpotter(
        context.assets,
        KeywordSpotterConfig().apply {
            featConfig = FeatureConfig().apply { sampleRate = WeckwortErkenner.ABTASTRATE; featureDim = 80 }
            modelConfig = OnlineModelConfig().apply {
                transducer = OnlineTransducerModelConfig().apply {
                    encoder = "kws/encoder.onnx"
                    decoder = "kws/decoder.onnx"
                    joiner = "kws/joiner.onnx"
                }
                this.tokens = "kws/tokens.txt"
                numThreads = 1
                provider = "cpu"
                modelType = "zipformer2"
            }
            // Pflichtangabe; die eigentliche Phrase kommt unten mit dem Strom.
            keywordsFile = "kws/keywords.txt"
            // Die Kotlin-Vorgaben passen nicht zum Weckwort (Almanach android/voice-assistant-trigger §15): Mit 2
            // Leer-Rahmen meldet der Spotter die Phrase erst nach einer Pause, schnell weitergesprochen nie. Die
            // Entprellung übernimmt spotter.reset() nach jedem Treffer. Mehr Pfade halten unsichere Aussprache im Beam.
            numTrailingBlanks = 0
            maxActivePaths = 8
            // Gilt nur für Zeilen ohne eigenes „#“/„:“; unsere tragen beides, aber nie auf 0,25 zurückfallen.
            keywordsThreshold = schwelle
            keywordsScore = BOOST
        },
    )
    var schwelle: Float = schwelle
        private set
    /** Breit: Phrase samt Aussprache-Varianten und stärkerem Boost (nur mit Stimmerkennung). */
    var breit = false
        private set
    private var zeile = zeilen()
    private var strom: OnlineStream = spotter.createStream(zeile)
    private val puffer = FloatArray(WeckwortErkenner.BLOCK)

    /** Welche Variante zuletzt gegriffen hat (Tokens), fürs Protokoll. */
    var letzterTreffer: String = ""
        private set

    /**
     * Je Zeile: Tokens, Boost (:), Schwelle (#); mehrere Zeilen mit „/“ getrennt. Ohne „@Original“, das parst
     * sherpa-onnx als Tokens (Almanach #32).
     */
    private fun zeilen(): String {
        val boost = if (breit) BOOST_BREIT else BOOST
        return (listOf(tokens) + if (breit) varianten else emptyList()).distinct().joinToString("/") { "$it :$boost #$schwelle" }
    }

    /** Schwelle und Breite einstellen; eine Änderung gilt ab sofort (neuer Strom). */
    fun einstellen(schwelle: Float, breit: Boolean) {
        if (schwelle == this.schwelle && breit == this.breit) return
        this.schwelle = schwelle
        this.breit = breit
        zeile = zeilen()
        zuruecksetzen()
    }

    /** Nach jeder Pause neu anfangen, damit kein Rest der letzten Aufnahme mitzählt. */
    fun zuruecksetzen() {
        runCatching { strom.release() }
        strom = spotter.createStream(zeile)
    }

    /** Verstärkung vor dem Spotter: leise und entfernte Rufe in den Pegelbereich bringen, den das Modell kennt. */
    private var verstaerkung = 1f

    /**
     * Nimmt einen Block (16 kHz, 16 Bit) und meldet, ob die Phrase gerade zu Ende gesprochen wurde.
     * [rauschboden]: Effektivwert der Stille (16 Bit) aus dem Dienst. Im ruhigen Raum wird bis zu [MAX_VERSTAERKUNG]fach
     * verstärkt, im Lärm nicht (das hebt nur den Lärm). Laute Rufe senkt der Begrenzer sofort, danach steigt die
     * Verstärkung langsam wieder an, so bleibt sie innerhalb einer Phrase gleich.
     */
    fun erkannt(block: ShortArray, rauschboden: Float = -1f): Boolean {
        val ziel = if (rauschboden > 0f) (BODEN_ZIEL / rauschboden).coerceIn(1f, MAX_VERSTAERKUNG) else 1f
        verstaerkung = if (ziel < verstaerkung) ziel else verstaerkung + (ziel - verstaerkung) * ANSTIEG
        var spitze = 0
        for (w in block) { val a = kotlin.math.abs(w.toInt()); if (a > spitze) spitze = a }
        val spitzeFloat = spitze / 32768f
        if (spitzeFloat * verstaerkung > GRENZE) verstaerkung = maxOf(1f, GRENZE / spitzeFloat)
        for (i in block.indices) puffer[i] = (block[i] / 32768f * verstaerkung).coerceIn(-1f, 1f)
        strom.acceptWaveform(if (block.size == puffer.size) puffer else puffer.copyOf(block.size), WeckwortErkenner.ABTASTRATE)
        // Pflicht: so lange dekodieren, wie Daten bereitliegen, nicht nur einmal (Almanach #21).
        while (spotter.isReady(strom)) spotter.decode(strom)
        val treffer = spotter.getResult(strom).keyword
        if (treffer.isEmpty()) return false
        letzterTreffer = treffer
        // Pflicht direkt nach dem Treffer, sonst meldet der Strom ihn immer wieder (Almanach #9).
        spotter.reset(strom)
        return true
    }

    override fun close() {
        runCatching { strom.release() }
        runCatching { spotter.release() }
    }

    companion object {
        private const val BOOST = 2.0f
        /** Mit Stimmerkennung: stärkerer Boost, damit auch schnell und verschliffen Gesprochenes greift. */
        private const val BOOST_BREIT = 3.0f

        /** Schwelle des KeywordSpotters je Empfindlichkeit (Vorgabe des Modells 0,25; kleiner = springt leichter an). */
        fun schwelleFuer(empfindlichkeit: String): Float =
            when (empfindlichkeit) { "niedrig" -> 0.35f; "hoch" -> 0.15f; "sehr_hoch" -> 0.10f; else -> 0.25f }

        /** Rauschboden (16 Bit, etwa -47 dBFS), auf den die Verstärkung zielt; Spitzen begrenzt auf [GRENZE]. */
        private const val BODEN_ZIEL = 150f
        private const val MAX_VERSTAERKUNG = 6f
        private const val GRENZE = 0.9f
        /** Je Block (80 ms) so viel des Wegs zurück zum Ziel: rund 8 s nach einem lauten Ruf wieder voll. */
        private const val ANSTIEG = 0.01f

        /**
         * Mit Stimmerkennung deutlich empfindlicher: Sie siebt Fremde aus, die Phrase soll auch schnell gesprochen und
         * von weitem greifen. (14.10.2026: schnell gesprochenes „Hey Jarvis One“ griff bei 0,18 kaum, langsam schon.)
         */
        const val SPRECHER_BONUS = 0.13f
        const val SCHWELLE_MIN = 0.06f
        /**
         * Beim Anlernen: leicht, mit allen Varianten, damit auch schnelle und leise Proben ankommen, aber nicht so leicht,
         * dass Fernseher oder andere Stimmen nebenbei als Probe ins Profil rutschen.
         */
        const val SCHWELLE_ANLERNEN = 0.10f
    }
}

/**
 * Zerlegt eine Phrase in die Tokens des Weckwort-Modells (SentencePiece UNIGRAM, 500 Stück), Viterbi über die
 * Log-Werte wie sentencepiece selbst. Kotlin-Fassung von `VoiceAgent/Core/SentencePieceUnigram.cs`, dort gegen
 * Python-sentencepiece geprüft. Das Modell kennt nur GROSSBUCHSTABEN (A–Z, Apostroph); Umlaute gehen nicht.
 */
object PhrasenTokens {
    private const val TRENNER = '▁'
    @Volatile private var werte: Map<String, Float>? = null
    private var laengstes = 1

    private fun laden(context: Context): Map<String, Float> = werte ?: synchronized(this) {
        werte ?: context.assets.open("kws/unigram-vocab.txt").bufferedReader(Charsets.UTF_8).useLines { zeilen ->
            val m = HashMap<String, Float>()
            zeilen.forEach { z ->
                if (z.isEmpty() || z[0] == '#') return@forEach
                val teile = z.split('\t')
                // Nur normale Stücke (Typ 1) zum Zerlegen.
                if (teile.size < 3 || teile[2] != "1" || teile[0].isEmpty()) return@forEach
                teile[1].toFloatOrNull()?.let { m[teile[0]] = it; if (teile[0].length > laengstes) laengstes = teile[0].length }
            }
            m
        }.also { werte = it }
    }

    /**
     * Wie Frank die Phrase schnell oder mit deutschem Akzent tatsächlich ausspricht, in englischer Schreibung: Das
     * Modell hört Laute, und „One“ klingt oft wie „Wan“, „Hey“ wie „Hi“ oder verschmilzt ganz mit dem nächsten Wort.
     * Je Wort die Schreibweisen, die gleich klingen.
     */
    private val gleichKlingend = mapOf(
        "HEY" to listOf("HI", "HAY", "A"),
        "HI" to listOf("HEY"),
        "ONE" to listOf("WON", "WAN", "WHAN"),
        "JARVIS" to listOf("JARVES", "JAVIS"),
        // Deutsches „Handy“ klingt wie „Hendi“.
        "HANDY" to listOf("HENDY", "HENDI", "HANDI"),
        "OKAY" to listOf("OK", "OKEY"),
        "TWO" to listOf("TOO", "TU"),
        "WAKE" to listOf("WEAK"),
    )
    private val auftakt = setOf("HEY", "HI", "HELLO", "OKAY", "OK")

    /**
     * Aussprache-Varianten der Phrase (als Tokens, ohne die Phrase selbst): je Wort die gleich klingenden
     * Schreibweisen, dazu die Phrase ohne Auftakt („Hey“ verschluckt). Höchstens 16, damit der Spotter schnell bleibt.
     */
    fun varianten(context: Context, text: String): List<String> {
        val woerter = normiert(text).split(' ').filter { it.isNotEmpty() }
        if (woerter.isEmpty()) return emptyList()
        var formen = listOf(emptyList<String>())
        for (w in woerter) formen = formen.flatMap { f -> (listOf(w) + gleichKlingend[w].orEmpty()).map { f + it } }
        val alle = formen.map { it.joinToString(" ") }.toMutableList()
        if (woerter.size >= 3 && woerter.first() in auftakt) alle += woerter.drop(1).joinToString(" ")
        val ohneSelbst = alle.distinct().filter { it != woerter.joinToString(" ") }
        // Erst die ohne Auftakt und die mit nur einer Abweichung, dann der Rest.
        val sortiert = ohneSelbst.sortedBy { v ->
            val teile = v.split(' ')
            if (teile.size != woerter.size) 0 else teile.zip(woerter).count { (a, b) -> a != b }
        }
        return sortiert.take(16).mapNotNull { zerlege(context, it) }
    }

    /** Die Phrase so, wie das Modell sie erwartet: Großbuchstaben, einfache Leerzeichen. */
    fun normiert(text: String): String = text.trim().uppercase().replace(Regex("\\s+"), " ")

    /** Tokens, durch Leerzeichen getrennt („▁OKAY ▁COMP U TER“), oder null, wenn ein Zeichen nicht geht. */
    fun zerlege(context: Context, text: String): String? {
        val woerter = normiert(text)
        if (woerter.isEmpty() || woerter.length > 60) return null
        val s = TRENNER + woerter.replace(' ', TRENNER)
        val m = laden(context)
        val n = s.length
        val besten = DoubleArray(n + 1) { Double.NEGATIVE_INFINITY }
        val von = IntArray(n + 1) { -1 }
        besten[0] = 0.0
        for (i in 0 until n) {
            if (besten[i] == Double.NEGATIVE_INFINITY) continue
            for (len in 1..minOf(laengstes, n - i)) {
                val wert = m[s.substring(i, i + len)] ?: continue
                if (besten[i] + wert > besten[i + len]) { besten[i + len] = besten[i] + wert; von[i + len] = i }
            }
        }
        if (besten[n] == Double.NEGATIVE_INFINITY) return null
        val stuecke = ArrayList<String>()
        var i = n
        while (i > 0) {
            val j = von[i]
            if (j < 0) return null
            stuecke += s.substring(j, i)
            i = j
        }
        return stuecke.asReversed().joinToString(" ")
    }
}

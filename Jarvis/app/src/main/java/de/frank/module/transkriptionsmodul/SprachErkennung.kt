// ──────────────────────────────────────────────────────────────────────
// Modul M1.4 — Transkriptionsmodul · Stand v1
// Quelle: Module/Android/M1.4-Transkriptionsmodul/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.4 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.transkriptionsmodul

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.nio.FloatBuffer

/**
 * Silero VAD (v5.1.2, MIT, assets/vad/): Wahrscheinlichkeit, dass ein 32-ms-Rahmen (512 Werte bei 16 kHz)
 * menschliche Sprache enthält. Anders als der Pegel unterscheidet das Modell Stimme von Motor-, Reifen- und
 * Windgeräusch, die im Auto oft lauter sind als leise Sprache.
 *
 * Eingang wie in der Vorlage (OnnxWrapper von Silero): die letzten 64 Werte des vorigen Rahmens vorn, dann der
 * Rahmen, zusammen [1, 576]; dazu der Zustand [2, 1, 128] und die Abtastrate als Skalar.
 */
class SileroVad(context: Context, kleidiaiAbschalten: Boolean) : AutoCloseable {
    private val umgebung = OrtEnvironment.getEnvironment().also { u -> runCatching { u.setTelemetry(false) } }
    private val optionen = OrtSession.SessionOptions().apply {
        setIntraOpNumThreads(1)
        setInterOpNumThreads(1)
        // Prozessoren mit SME, aber ohne SME2 (Galaxy Fold 8) stürzen an den KleidiAI-Kernen ab; die App weiß, ob das
        // Gerät dazugehört (BEISPIEL.md), und muss es für alle ONNX-Sitzungen gleich halten.
        if (kleidiaiAbschalten) addConfigEntry("mlas.disable_kleidiai", "1")
    }
    private val sitzung = umgebung.createSession(context.assets.open("vad/silero_vad.onnx").use { it.readBytes() }, optionen)
    private val rate = OnnxTensor.createTensor(umgebung, ABTASTRATE.toLong())
    private val zustand = FloatArray(2 * 128)
    private val kontext = FloatArray(KONTEXT)
    private val eingabe = FloatArray(KONTEXT + RAHMEN)

    /** Vor jeder Aufnahme: Nichts aus der letzten soll nachwirken. */
    fun zuruecksetzen() {
        zustand.fill(0f)
        kontext.fill(0f)
    }

    /** [rahmen]: genau [RAHMEN] Werte von -1 bis 1. */
    fun wahrscheinlichkeit(rahmen: FloatArray): Float {
        System.arraycopy(kontext, 0, eingabe, 0, KONTEXT)
        System.arraycopy(rahmen, 0, eingabe, KONTEXT, RAHMEN)
        OnnxTensor.createTensor(umgebung, FloatBuffer.wrap(eingabe), longArrayOf(1, (KONTEXT + RAHMEN).toLong())).use { x ->
            OnnxTensor.createTensor(umgebung, FloatBuffer.wrap(zustand), longArrayOf(2, 1, 128)).use { s ->
                sitzung.run(mapOf("input" to x, "state" to s, "sr" to rate)).use { ergebnis ->
                    val p = (ergebnis.get("output").get() as OnnxTensor).floatBuffer.get(0)
                    (ergebnis.get("stateN").get() as OnnxTensor).floatBuffer.get(zustand)
                    System.arraycopy(rahmen, RAHMEN - KONTEXT, kontext, 0, KONTEXT)
                    return p
                }
            }
        }
    }

    override fun close() {
        runCatching { rate.close() }
        runCatching { sitzung.close() }
        runCatching { optionen.close() }
    }

    companion object {
        const val ABTASTRATE = 16_000
        /** 32 ms bei 16 kHz. */
        const val RAHMEN = 512
        private const val KONTEXT = 64
    }
}

/**
 * Führt während der Aufnahme Buch, wann gesprochen wurde — je 32-ms-Rahmen aus den Werten des Modells.
 * Ohne Android und ohne Modell ([modell] wird hineingereicht), damit es sich mit echten Modellwerten testen lässt.
 *
 * Zwei Sichten:
 * - [sprachRahmen] für das Aufnahmeende: Sprache beginnt erst nach [ANLAUF_RAHMEN] Rahmen in Folge ab [EIN]
 *   (ein Schlagloch oder ein Blinkerklick setzt so nichts in Gang) und hält, solange die Werte über [AUS] bleiben.
 *   Ein Zähler, der nur wächst: Wer alle 50 ms nachsieht, verpasst keinen Rahmen dazwischen.
 * - [stimmhaft] für Zuschnitt und Halluzinationsfilter: großzügiger ([STIMMHAFT]), damit leise Wortanfänge
 *   und Satzenden nicht als Stille gelten.
 */
class SprachZeitachse(private val modell: (FloatArray) -> Float) {
    private val rahmen = FloatArray(SileroVad.RAHMEN)
    private var fuellung = 0
    private val werte = ArrayList<Float>()
    private var folge = 0
    private var imSprechen = false

    @Volatile
    var sprachRahmen = 0L
        private set

    /** Nimmt rohe 16-Bit-PCM-Bytes (little endian), wie sie aus AudioRecord kommen. */
    fun pcm(puffer: ByteArray, laenge: Int) {
        var i = 0
        while (i + 1 < laenge) {
            val wert = ((puffer[i + 1].toInt() shl 8) or (puffer[i].toInt() and 0xFF)).toShort()
            rahmen[fuellung++] = wert / 32_768f
            if (fuellung == rahmen.size) {
                rahmenFertig(modell(rahmen))
                fuellung = 0
            }
            i += 2
        }
    }

    private fun rahmenFertig(p: Float) {
        werte += p
        if (imSprechen) {
            if (p >= AUS) sprachRahmen++ else { imSprechen = false; folge = 0 }
            return
        }
        folge = if (p >= EIN) folge + 1 else 0
        if (folge >= ANLAUF_RAHMEN) {
            imSprechen = true
            sprachRahmen += folge
        }
    }

    /** Je Rahmen (32 ms): Klingt das nach Stimme? */
    fun stimmhaft(): BooleanArray = BooleanArray(werte.size) { werte[it] >= STIMMHAFT }

    companion object {
        const val RAHMEN_MS = 32
        const val EIN = 0.5f
        const val AUS = 0.35f
        const val STIMMHAFT = 0.3f
        /** Drei Rahmen ≈ 100 ms: kürzer ist keine Silbe. */
        const val ANLAUF_RAHMEN = 3
    }
}

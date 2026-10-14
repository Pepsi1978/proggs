package de.frank.jarvis.weckwort

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.nio.FloatBuffer

/**
 * Erkennt „Hey Jarvis“ mit den vortrainierten Modellen von openWakeWord (assets/weckwort/, Lizenz dort).
 *
 * Drei Stufen je Block von 80 ms (1280 Werte bei 16 kHz), genau wie die Python-Vorlage `openwakeword.Model`:
 * 1. Mel-Spektrogramm aus den letzten 1760 Werten (Block plus 480 Werte Vorlauf). Eingabe sind die rohen
 *    16-Bit-Werte als Kommazahl, NICHT auf ±1 gebracht, sonst stimmt nichts mehr. Ausgabe je Zeile `x / 10 + 2`.
 * 2. Einbettung der letzten 76 Mel-Zeilen zu 96 Merkmalen.
 * 3. Urteil über die letzten 16 Einbettungen (rund 1,3 Sekunden): Wert von 0 bis 1.
 *
 * Gegen die Vorlage geprüft (gleiche Werte auf drei Stellen, auch mit deutschen Stimmen):
 * `best-practices/android/voice-assistant-trigger.md` Abschnitt openWakeWord.
 */
class WeckwortErkenner(context: Context) : AutoCloseable {
    private val umgebung = OrtEnvironment.getEnvironment()
    private val optionen = OrtSession.SessionOptions().apply {
        // Ein Kern genügt für 80-ms-Blöcke und hält das Handy kühl.
        setIntraOpNumThreads(1)
        setInterOpNumThreads(1)
    }
    private val mel = sitzung(context, "melspectrogram.onnx")
    private val einbettung = sitzung(context, "embedding_model.onnx")
    private val urteil = sitzung(context, "hey_jarvis.onnx")

    private val roh = FloatArray(ROH_WERTE)
    private var rohLaenge = 0
    private val melZeilen = ArrayDeque<FloatArray>()
    private val merkmale = ArrayDeque<FloatArray>()

    /** Die 16 Einbettungen (flach, 1536 Werte), über die zuletzt geurteilt wurde: Grundlage für das [Stimmprofil]. */
    var letzteMerkmale: FloatArray? = null
        private set

    init { zuruecksetzen() }

    private fun sitzung(context: Context, datei: String): OrtSession =
        umgebung.createSession(context.assets.open("weckwort/$datei").use { it.readBytes() }, optionen)

    /** Nach jeder Pause neu anfangen, damit kein Rest der letzten Aufnahme mitzählt. */
    fun zuruecksetzen() {
        rohLaenge = 0
        melZeilen.clear()
        // Wie die Vorlage: 76 Zeilen aus Einsen, dann gibt es ab dem ersten Block Einbettungen.
        repeat(MEL_FENSTER) { melZeilen.addLast(FloatArray(MEL_BREITE) { 1f }) }
        merkmale.clear()
        letzteMerkmale = null
    }

    /** Nimmt genau einen Block von [BLOCK] Werten und liefert den Wert für „Hey Jarvis“ (0 im Anlauf). */
    fun bewerte(block: ShortArray): Float {
        require(block.size == BLOCK) { "Block muss $BLOCK Werte haben" }
        val neu = minOf(rohLaenge + BLOCK, ROH_WERTE)
        val behalten = neu - BLOCK
        System.arraycopy(roh, rohLaenge - behalten, roh, 0, behalten)
        for (i in 0 until BLOCK) roh[behalten + i] = block[i].toFloat()
        rohLaenge = neu

        val spektrum = rechne(mel, roh.copyOf(rohLaenge), longArrayOf(1, rohLaenge.toLong()))
        for (zeile in 0 until spektrum.size / MEL_BREITE) {
            melZeilen.addLast(FloatArray(MEL_BREITE) { spektrum[zeile * MEL_BREITE + it] / 10f + 2f })
        }
        while (melZeilen.size > MEL_FENSTER) melZeilen.removeFirst()

        val fenster = FloatArray(MEL_FENSTER * MEL_BREITE)
        melZeilen.forEachIndexed { i, zeile -> System.arraycopy(zeile, 0, fenster, i * MEL_BREITE, MEL_BREITE) }
        merkmale.addLast(rechne(einbettung, fenster, longArrayOf(1, MEL_FENSTER.toLong(), MEL_BREITE.toLong(), 1)).copyOf(MERKMALE))
        while (merkmale.size > MERKMAL_FENSTER) merkmale.removeFirst()
        if (merkmale.size < MERKMAL_FENSTER) return 0f

        val eingabe = FloatArray(MERKMAL_FENSTER * MERKMALE)
        merkmale.forEachIndexed { i, m -> System.arraycopy(m, 0, eingabe, i * MERKMALE, MERKMALE) }
        letzteMerkmale = eingabe
        return rechne(urteil, eingabe, longArrayOf(1, MERKMAL_FENSTER.toLong(), MERKMALE.toLong())).firstOrNull() ?: 0f
    }

    /** Ein Modell rechnen lassen; Ein- und Ausgabe flach, so braucht es keine verschachtelten Felder. */
    private fun rechne(sitzung: OrtSession, daten: FloatArray, form: LongArray): FloatArray =
        OnnxTensor.createTensor(umgebung, FloatBuffer.wrap(daten), form).use { tensor ->
            sitzung.run(mapOf(sitzung.inputNames.first() to tensor)).use { ergebnis ->
                val puffer = (ergebnis.get(0) as OnnxTensor).floatBuffer
                FloatArray(puffer.remaining()).also { puffer.get(it) }
            }
        }

    override fun close() {
        runCatching { mel.close() }
        runCatching { einbettung.close() }
        runCatching { urteil.close() }
        runCatching { optionen.close() }
    }

    companion object {
        const val ABTASTRATE = 16_000
        /** 80 ms bei 16 kHz. */
        const val BLOCK = 1280
        private const val ROH_WERTE = BLOCK + 480
        private const val MEL_BREITE = 32
        private const val MEL_FENSTER = 76
        private const val MERKMALE = 96
        private const val MERKMAL_FENSTER = 16
    }
}

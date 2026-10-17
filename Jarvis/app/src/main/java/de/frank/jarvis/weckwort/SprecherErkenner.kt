package de.frank.jarvis.weckwort

import android.content.Context
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import kotlin.math.sqrt

/**
 * Stimm-Fingerabdruck: Wer spricht, unabhängig davon, was gesagt wird. Modell in assets/sprecher/campplus.onnx:
 * `3dspeaker_speech_campplus_sv_zh_en_16k-common_advanced` (CAM++, 3D-Speaker, Apache-2.0), trainiert auf über
 * 200.000 Sprechern in mehreren Sprachen, über sherpa-onnx (dieselbe ONNX Runtime wie der Rest).
 *
 * Anders als das [Stimmprofil] (Lautmerkmale des Weckwort-Modells, unterscheidet Wörter) ist dieses Modell darauf
 * trainiert, Menschen zu unterscheiden: Andere sagen dieselbe Phrase, der Fingerabdruck bleibt ein anderer.
 */
class SprecherErkenner(context: Context) : AutoCloseable {
    private val extraktor = SpeakerEmbeddingExtractor(
        context.assets,
        SpeakerEmbeddingExtractorConfig("sprecher/campplus.onnx", 1, false, "cpu"),
    )

    /**
     * Fingerabdruck der Aufnahme (16 kHz, ±1), auf Länge 1 gebracht; null, wenn zu wenig Sprache darin ist.
     * [stimmhaft]: je 32-ms-Rahmen, ob Silero dort eine Stimme hört. Dann zählt nur die Stimme, nicht Lärm drumherum.
     */
    fun fingerabdruck(ton: FloatArray, stimmhaft: BooleanArray? = null): FloatArray? {
        // Hört Silero zu wenig Stimme (sehr schnell, sehr leise), bleibt der Zuschnitt nach Lautstärke.
        val sprache = (stimmhaft?.let { nurStimme(ton, it) } ?: nurSprache(ton)) ?: return null
        val strom = extraktor.createStream()
        return try {
            strom.acceptWaveform(sprache, WeckwortErkenner.ABTASTRATE)
            strom.inputFinished()
            if (!extraktor.isReady(strom)) return null
            normiert(extraktor.compute(strom))
        } finally {
            strom.release()
        }
    }

    override fun close() { runCatching { extraktor.release() } }

    companion object {
        private const val RAHMEN = 320 // 20 ms

        /**
         * Schneidet Stille vorn und hinten ab (aus fünf Metern ist der Großteil der Aufnahme Raumrauschen) und bringt
         * die Sprache auf eine feste Lautstärke. So zählt im Fingerabdruck die Stimme, nicht der Abstand.
         */
        internal fun nurSprache(ton: FloatArray): FloatArray? {
            val n = ton.size / RAHMEN
            if (n < 10) return null
            val energie = FloatArray(n) { r -> var s = 0f; for (i in r * RAHMEN until (r + 1) * RAHMEN) s += ton[i] * ton[i]; sqrt(s / RAHMEN) }
            val sortiert = energie.sorted()
            val boden = sortiert[n / 10]
            val spitze = sortiert[n - 1 - n / 20]
            if (spitze <= boden * 2f) return null
            val schwelle = boden + (spitze - boden) * 0.15f
            val erster = energie.indexOfFirst { it > schwelle }
            val letzter = energie.indexOfLast { it > schwelle }
            if (erster < 0 || letzter - erster < 15) return null // unter 0,3 s Sprache
            val von = maxOf(0, erster - 5) * RAHMEN
            val bis = minOf(n, letzter + 6) * RAHMEN
            val stueck = ton.copyOfRange(von, bis)
            val faktor = (0.1f / spitze).coerceIn(0.5f, 30f)
            for (i in stueck.indices) stueck[i] = (stueck[i] * faktor).coerceIn(-1f, 1f)
            return stueck
        }

        /**
         * Wie [nurSprache], aber nach Silero statt nach Lautstärke: Bei Lärm (Auto, Küche, Straße) ist die Stimme kaum
         * lauter als der Hintergrund, die Lautstärke findet sie dann nicht. Behält nur die Rahmen mit Stimme (je einen
         * Rahmen Rand) und bringt sie auf feste Lautstärke; null unter 0,3 s Stimme.
         */
        internal fun nurStimme(ton: FloatArray, stimmhaft: BooleanArray): FloatArray? {
            val r = VAD_RAHMEN
            val n = minOf(stimmhaft.size, ton.size / r)
            val behalten = BooleanArray(n) { i -> (maxOf(0, i - 1)..minOf(n - 1, i + 1)).any { stimmhaft[it] } }
            if (stimmhaft.take(n).count { it } < 10) return null
            val stueck = FloatArray(behalten.count { it } * r)
            var pos = 0
            for (i in 0 until n) if (behalten[i]) { System.arraycopy(ton, i * r, stueck, pos, r); pos += r }
            val rahmen = stueck.size / RAHMEN
            if (rahmen < 1) return null
            val energie = FloatArray(rahmen) { k -> var s = 0f; for (i in k * RAHMEN until (k + 1) * RAHMEN) s += stueck[i] * stueck[i]; sqrt(s / RAHMEN) }
            val spitze = energie.sorted()[rahmen - 1 - rahmen / 20]
            if (spitze <= 0f) return null
            val faktor = (0.1f / spitze).coerceIn(0.5f, 30f)
            for (i in stueck.indices) stueck[i] = (stueck[i] * faktor).coerceIn(-1f, 1f)
            return stueck
        }

        /** Rahmenlänge der Sprachaktivitätserkennung (Silero, 32 ms). */
        const val VAD_RAHMEN = 512

        fun normiert(v: FloatArray): FloatArray {
            var s = 0.0
            for (x in v) s += x * x
            val l = sqrt(s).toFloat()
            return if (l == 0f) v else FloatArray(v.size) { v[it] / l }
        }

        fun kosinus(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size) return -1f
            var s = 0f
            for (i in a.indices) s += a[i] * b[i]
            return s
        }
    }
}

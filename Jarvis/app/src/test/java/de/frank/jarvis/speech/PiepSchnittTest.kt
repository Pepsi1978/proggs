package de.frank.jarvis.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PiepSchnittTest {
    private val rate = 16_000

    private fun wav(proben: DoubleArray): ByteArray {
        val pcm = ByteArray(proben.size * 2)
        proben.forEachIndexed { i, v ->
            val s = (v.coerceIn(-1.0, 1.0) * 32_767).toInt()
            pcm[i * 2] = (s and 0xFF).toByte(); pcm[i * 2 + 1] = (s shr 8).toByte()
        }
        val kopf = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(pcm.size + 36); put("WAVE".toByteArray()); put("fmt ".toByteArray())
            putInt(16); putShort(1); putShort(1); putInt(rate); putInt(rate * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(pcm.size)
        }.array()
        return kopf + pcm
    }

    private fun ms(n: Int) = n * rate / 1_000
    private val zufall = java.util.Random(7)
    private fun stille(n: Int) = DoubleArray(ms(n)) { zufall.nextGaussian() * 0.001 }
    private fun piep(n: Int) = DoubleArray(ms(n)) { val t = it.toDouble() / rate; 0.3 * Math.sin(2 * Math.PI * 400 * t) + 0.3 * Math.sin(2 * Math.PI * 1_200 * t) }
    /** Stimmähnlich: Grundton 140 Hz mit Obertönen und Rauschen. */
    private fun stimme(n: Int) = DoubleArray(ms(n)) { val t = it.toDouble() / rate; (1..12).sumOf { h -> 0.05 / h * Math.sin(2 * Math.PI * 140 * h * t) } + zufall.nextGaussian() * 0.02 }

    @Test
    fun piepUndNachklangFallenWegSpracheBleibt() {
        // Nachklang der Antwort (Stimme), Ruhe, Piepton (per Bluetooth 200 ms verspätet), dann sofort Sprache.
        val vor = stimme(300) + stille(400)
        val ab = vor.size * 2
        val proben = vor + stille(200) + piep(35) + stille(60) + stimme(800)
        val r = PiepSchnitt.schneiden(wav(proben), ab, null)
        assertTrue(r.piepGefunden)
        val rest = (r.wav.size - 44) / 2
        // Von der Stimme fehlt nichts, vom Piepton bleibt höchstens ein Rest Nachhall.
        assertTrue("Rest $rest", rest >= ms(800) && rest <= ms(800) + ms(70))
    }

    @Test
    fun ohnePiepWirdAbDerMarkeGeschnitten() {
        val proben = stimme(300) + stille(500) + stimme(500)
        val ab = ms(800) * 2
        val r = PiepSchnitt.schneiden(wav(proben), ab, BooleanArray(proben.size / 512) { true })
        assertEquals(false, r.piepGefunden)
        val schnitt = ab / 1024 * 1024
        assertEquals(proben.size - schnitt / 2, (r.wav.size - 44) / 2)
        assertEquals(proben.size / 512 - ab / 1024, r.stimmhaft!!.size)
    }

    @Test
    fun vokalMit400HzOhnePiepWirdNichtAngeschnitten() {
        // Kein Piepton in der Aufnahme (etwa Echounterdrückung), Frank spricht sofort einen Vokal mit Obertönen bei 400 und 1200 Hz.
        val vokal = DoubleArray(ms(900)) { val t = it.toDouble() / rate; (1..8).sumOf { h -> 0.1 / h * Math.sin(2 * Math.PI * 200 * h * t) } }
        val vor = stimme(300)
        val ab = vor.size * 2
        val r = PiepSchnitt.schneiden(wav(vor + vokal), ab, null)
        assertEquals(false, r.piepGefunden)
        val schnitt = ab / 1024 * 1024
        assertEquals(vor.size + vokal.size - schnitt / 2, (r.wav.size - 44) / 2)
    }

    @Test
    fun tieferVokalMitFormantenBei400Und1200IstKeinPiep() {
        // Grundton 100 Hz, Formanten genau auf 400 und 1200 Hz: jedes Fenster sieht wie Piepton aus, hält aber zu lange an.
        val vokal = DoubleArray(ms(900)) { val t = it.toDouble() / rate; (1..30).sumOf { h -> (if (h == 4 || h == 12) 0.2 else 0.01 / h) * Math.sin(2 * Math.PI * 100 * h * t) } }
        val vor = stimme(300)
        val ab = vor.size * 2
        val r = PiepSchnitt.schneiden(wav(vor + vokal), ab, null)
        assertEquals(false, r.piepGefunden)
        assertEquals(vor.size + vokal.size - ab / 1024 * 1024 / 2, (r.wav.size - 44) / 2)
    }

    @Test
    fun ohneSucheNurAnDerMarke() {
        val proben = stille(300) + piep(35) + stimme(500)
        val ab = ms(300) * 2
        val r = PiepSchnitt.schneiden(wav(proben), ab, null, suchen = false)
        assertEquals(false, r.piepGefunden)
        assertEquals(proben.size - ab / 1024 * 1024 / 2, (r.wav.size - 44) / 2)
    }
}

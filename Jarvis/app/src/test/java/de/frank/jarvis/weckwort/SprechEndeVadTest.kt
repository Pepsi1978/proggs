package de.frank.jarvis.weckwort

import de.frank.jarvis.audio.SprachZeitachse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aufnahmeende mit echten Werten von Silero VAD v5.1.2 auf Audio-Testfällen (TTS-Sprache, synthetisches Motor-,
 * Abroll- und Windgeräusch mit Schlaglöchern; erzeugt mit werkzeuge/vad_testfaelle.py). Je Fall liegen die
 * Modellwerte und der Pegel wie in MicRecorder je 32-ms-Rahmen unter test/resources/vad/.
 */
class SprechEndeVadTest {
    private class Fall(val werte: List<Float>, val pegel: List<Float>, val sprache: List<Pair<Long, Long>>)

    private fun lade(name: String): Fall {
        val zeilen = javaClass.classLoader!!.getResourceAsStream("vad/$name.txt")!!.bufferedReader().readLines()
        val sprache = zeilen.first().substringAfter("Sprache (s):").trim().split(' ').filter { it.isNotBlank() }.map {
            val (a, b) = it.split('-').map { s -> (s.toDouble() * 1000).toLong() }
            a to b
        }
        val daten = zeilen.drop(1).filter { it.isNotBlank() }.map { it.split(' ') }
        return Fall(daten.map { it[0].toFloat() }, daten.map { it[1].toFloat() }, sprache)
    }

    /**
     * Spielt die Aufnahme ab wie auf dem Handy: Die Zeitachse bekommt je 32 ms einen Rahmen, die Schleife in
     * `runde()` sieht alle 50 ms nach.
     */
    private fun abspielen(fall: Fall, ende: SprechEnde): Triple<SprechEnde.Lage, Long, SprachZeitachse> {
        var naechster = 0
        val achse = SprachZeitachse { fall.werte[naechster - 1] }
        val leer = ByteArray(1024)
        var ms = 50L
        val bis = fall.werte.size * 32L
        while (ms <= bis) {
            while ((naechster + 1) * 32L <= ms && naechster < fall.werte.size) {
                naechster++
                achse.pcm(leer, leer.size)
            }
            val pegel = fall.pegel[(naechster - 1).coerceAtLeast(0)]
            val lage = ende.neu(pegel, ms, achse.sprachRahmen)
            if (lage != SprechEnde.Lage.WEITER) return Triple(lage, ms, achse)
            ms += 50
        }
        return Triple(SprechEnde.Lage.WEITER, ms, achse)
    }

    @Test
    fun spracheDannFahrgeraeuschEndetNachViereinhalbSekunden() {
        val fall = lade("auto_sprache_dann_fahrt")
        val (lage, ms, _) = abspielen(fall, SprechEnde())
        assertEquals(SprechEnde.Lage.FERTIG, lage)
        val nachSprache = ms - fall.sprache.last().second
        assertTrue("Ende $nachSprache ms nach dem letzten Wort", nachSprache in 4_500L..5_000L)
    }

    @Test
    fun reinesFahrgeraeuschHaeltNichtsOffen() {
        val (lage, ms, achse) = abspielen(lade("auto_nur_fahrt"), SprechEnde())
        assertEquals(SprechEnde.Lage.NICHTS_GEHOERT, lage)
        assertEquals(7_000L, ms)
        assertEquals(0L, achse.sprachRahmen)
    }

    @Test
    fun nachdenkpauseImAutoSchneidetNichtAb() {
        val fall = lade("auto_pause")
        val (lage, ms, _) = abspielen(fall, SprechEnde())
        assertEquals(SprechEnde.Lage.FERTIG, lage)
        // Nicht in der 3-s-Pause beendet, sondern erst nach dem zweiten Satz.
        val nachSprache = ms - fall.sprache.last().second
        assertTrue("Ende $nachSprache ms nach dem zweiten Satz", nachSprache in 4_500L..5_000L)
    }

    @Test
    fun leiseSpracheImRuhigenRaumWirdErkannt() {
        val fall = lade("ruhig_leise")
        val ende = SprechEnde()
        val (lage, ms, _) = abspielen(fall, ende)
        assertEquals(SprechEnde.Lage.FERTIG, lage)
        assertTrue(ende.gesprochen)
        val nachSprache = ms - fall.sprache.last().second
        assertTrue("Ende $nachSprache ms nach dem letzten Wort", nachSprache in 4_500L..5_000L)
    }

    @Test
    fun leiseSpracheImLautenAutoWirdErkannt() {
        // Schwierigster Fall: Sprache 10 dB leiser als das Fahrgeräusch. Sie muss erkannt werden und darf nicht
        // vor ihrem Ende abgeschnitten werden.
        val fall = lade("auto_leise")
        val ende = SprechEnde()
        val (lage, ms, _) = abspielen(fall, ende)
        assertEquals(SprechEnde.Lage.FERTIG, lage)
        assertTrue(ende.gesprochen)
        assertTrue("Ende bei $ms ms, Sprache endet ${fall.sprache.last().second}", ms >= fall.sprache.last().second)
    }

    @Test
    fun stimmhaftDecktDieSpracheUndNichtDasRauschen() {
        val fall = lade("auto_sprache_dann_fahrt")
        val (_, _, achse) = abspielen(fall, SprechEnde(wartenMs = 60_000, stilleMs = 60_000))
        val stimmhaft = achse.stimmhaft()
        val letzter = stimmhaft.indexOfLast { it } * 32L
        // Letzter stimmhafter Rahmen kurz nach dem letzten Wort, nicht irgendwo im Fahrgeräusch danach.
        assertTrue("letzter stimmhafter Rahmen bei $letzter ms", letzter in fall.sprache.last().second - 500..fall.sprache.last().second + 500)
    }
}

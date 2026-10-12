package de.frank.jarvis.weckwort

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SprechEndeTest {
    /** Spielt eine Pegelfolge im 50-ms-Takt ab und liefert Lage und Zeitpunkt des Endes. */
    private fun abspielen(ende: SprechEnde, pegel: (Long) -> Float, bis: Long = 70_000): Pair<SprechEnde.Lage, Long> {
        var ms = 0L
        while (ms <= bis) {
            val lage = ende.neu(pegel(ms), ms)
            if (lage != SprechEnde.Lage.WEITER) return lage to ms
            ms += 50
        }
        return SprechEnde.Lage.WEITER to ms
    }

    @Test
    fun frageInRuhigemRaumEndetNachDerPause() {
        // Piepton bis 300 ms, Stille, Sprache von 1 s bis 3 s (mit kurzer Atempause), danach Stille.
        val (lage, ms) = abspielen(SprechEnde(), { t ->
            when {
                t < 300 -> 0.9f
                t in 1_000..1_800 || t in 2_200..3_000 -> 0.45f
                else -> 0.06f
            }
        })
        assertEquals(SprechEnde.Lage.FERTIG, lage)
        assertTrue("Ende nach 1,6 s Stille, war $ms", ms in 4_550..4_800)
    }

    @Test
    fun ohneSpracheNichtsGehoert() {
        val (lage, ms) = abspielen(SprechEnde(), { t -> if (t < 300) 0.9f else 0.07f })
        assertEquals(SprechEnde.Lage.NICHTS_GEHOERT, lage)
        assertEquals(7_000L, ms)
    }

    @Test
    fun pieptonAlleinZaehltNichtAlsSprache() {
        val ende = SprechEnde()
        abspielen(ende, { t -> if (t < 250) 1f else 0.05f }, bis = 2_000)
        assertTrue(!ende.gesprochen)
    }

    @Test
    fun lautesAutoHebtDieSchwelle() {
        // Grundrauschen 0,25 (Fahrgeräusch) liegt über der Ruheschwelle, darf aber nicht als Sprache gelten.
        val ende = SprechEnde()
        val (lage, _) = abspielen(ende, { t -> if (t < 300) 0.9f else 0.25f }, bis = 8_000)
        assertEquals(SprechEnde.Lage.NICHTS_GEHOERT, lage)
        assertTrue(ende.schwelle() > 0.35f)
    }

    @Test
    fun sofortigesSprechenWirdErkannt() {
        // Frank spricht direkt nach dem Piepton, nur kurze Wortpausen.
        val (lage, ms) = abspielen(SprechEnde(), { t ->
            when {
                t < 250 -> 0.9f
                t < 3_000 -> if ((t / 50) % 8 == 7L) 0.06f else 0.45f
                else -> 0.06f
            }
        })
        assertEquals(SprechEnde.Lage.FERTIG, lage)
        assertTrue("Ende nach 1,6 s Stille, war $ms", ms in 4_500..4_800)
    }

    @Test
    fun sprachePlusRauschenWirdErkannt() {
        val (lage, _) = abspielen(SprechEnde(), { t ->
            when {
                t < 300 -> 0.9f
                t in 1_000..2_500 -> 0.6f
                else -> 0.25f
            }
        })
        assertEquals(SprechEnde.Lage.FERTIG, lage)
    }
}

package de.frank.newskompass.news

import java.time.Instant
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NachrichtenAlterTest {
    private val jetzt = Instant.parse("2026-10-06T12:00:00Z").toEpochMilli()

    @Test
    fun dreiTageAltesEreignisFaelltBeiZweiTagenHeraus() {
        val ereignis = NachrichtenAlter.ereignisZeit("2026-10-03T12:00:00Z")
        assertFalse(NachrichtenAlter.istAktuell(ereignis, jetzt, 2))
        assertTrue(NachrichtenAlter.istAktuell(ereignis, jetzt, 3))
    }

    @Test
    fun altersgrenzeGiltFuerJedeReglerstufeEinschliesslichDerGrenze() {
        for (tage in 1..10) {
            val grenze = jetzt - TimeUnit.DAYS.toMillis(tage.toLong())
            assertEquals(grenze, NachrichtenAlter.fruehestens(jetzt, tage))
            assertTrue(NachrichtenAlter.istAktuell(grenze, jetzt, tage))
            assertTrue(NachrichtenAlter.istAktuell(grenze + 1, jetzt, tage))
            assertFalse(NachrichtenAlter.istAktuell(grenze - 1, jetzt, tage))
        }
    }

    @Test
    fun zukunftUndFehlendesDatumWerdenNichtAlsAktuellGewertet() {
        assertFalse(NachrichtenAlter.istAktuell(jetzt + 1, jetzt, 10))
        assertFalse(NachrichtenAlter.istAktuell(null, jetzt, 10))
        assertTrue(NachrichtenAlter.istAktuell(jetzt, jetzt, 1))
    }

    @Test
    fun datumMussEindeutigUndGueltigSein() {
        for (angabe in listOf("", "null", "gestern", "2026-02-30", "2026-10-05T12:00:00", "06.10.2026")) {
            assertNull(angabe, NachrichtenAlter.ereignisZeit(angabe))
        }
    }

    @Test
    fun zeitpunktMitZeitzoneWirdKorrektVerglichen() {
        assertEquals(jetzt, NachrichtenAlter.ereignisZeit(" 2026-10-06T14:00:00+02:00 "))
        assertFalse(NachrichtenAlter.istAktuell(NachrichtenAlter.ereignisZeit("2026-10-04T13:59:59+02:00"), jetzt, 2))
        assertTrue(NachrichtenAlter.istAktuell(NachrichtenAlter.ereignisZeit("2026-10-04T14:00:00+02:00"), jetzt, 2))
    }

    @Test
    fun reinesDatumZaehltVorsichtigAbDeutschemTagesbeginn() {
        assertEquals(Instant.parse("2026-10-04T22:00:00Z").toEpochMilli(), NachrichtenAlter.ereignisZeit("2026-10-05"))
        assertFalse(NachrichtenAlter.istAktuell(NachrichtenAlter.ereignisZeit("2026-10-04"), jetzt, 2))
        assertTrue(NachrichtenAlter.istAktuell(NachrichtenAlter.ereignisZeit("2026-10-05"), jetzt, 2))
    }

    @Test
    fun tagesbeginnBeruecksichtigtSommerzeitUndWinterzeit() {
        assertEquals(Instant.parse("2026-10-24T22:00:00Z").toEpochMilli(), NachrichtenAlter.ereignisZeit("2026-10-25"))
        assertEquals(Instant.parse("2026-10-25T23:00:00Z").toEpochMilli(), NachrichtenAlter.ereignisZeit("2026-10-26"))
    }
}

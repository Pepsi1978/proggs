package de.frank.jarvis.ui

import de.frank.jarvis.data.Sitzung
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeistenReiterTest {

    private fun s(id: String, projekt: String = "", programmierung: Boolean = false) = Sitzung(id, id, 1L, 2L, 3, projekt, programmierung)

    @Test fun programmiergespraecheNurUnterProgrammierung() {
        val alle = listOf(s("chat"), s("code", programmierung = true), s("projektchat", "p1"), s("projektcode", "p1", true), s("verwaist", "weg"))
        val l = aufteilen(alle, setOf("p1"))
        assertEquals(listOf("chat", "verwaist"), l.gespraeche.map { it.id })
        assertEquals(setOf("code", "projektcode"), l.programmierung.map { it.id }.toSet())
        // Keine Duplikate zwischen „Gespräche“ und „Programmierung“.
        assertTrue(l.gespraeche.intersect(l.programmierung.toSet()).isEmpty())
    }

    @Test fun programmierungFestNachStartzeit() {
        // Die älteste Session hat die jüngste Änderung: Sie bleibt trotzdem unten.
        val alt = Sitzung("alt", "alt", 100L, 9_000L, 5, programmierung = true, auftragStart = 110L)
        val mitte = Sitzung("mitte", "mitte", 200L, 300L, 5, programmierung = true)
        val neu = Sitzung("neu", "neu", 250L, 400L, 5, programmierung = true, auftragStart = 500L)
        val l = aufteilen(listOf(alt, mitte, neu), emptySet())
        assertEquals(listOf("neu", "mitte", "alt"), l.programmierung.map { it.id })
    }

    @Test fun reiterDesOffenenGespraechs() {
        assertEquals(LeistenReiter.PROGRAMMIERUNG, reiterFuer(programmierung = true, projekt = ""))
        assertEquals(LeistenReiter.PROGRAMMIERUNG, reiterFuer(programmierung = true, projekt = "p1"))
        assertEquals(LeistenReiter.PROJEKTE, reiterFuer(programmierung = false, projekt = "p1"))
        assertEquals(LeistenReiter.GESPRAECHE, reiterFuer(programmierung = false, projekt = ""))
    }
}

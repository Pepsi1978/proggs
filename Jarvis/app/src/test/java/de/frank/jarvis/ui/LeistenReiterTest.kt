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
        assertEquals(listOf("code", "projektcode"), l.programmierung.map { it.id })
        // Keine Duplikate zwischen „Gespräche“ und „Programmierung“.
        assertTrue(l.gespraeche.intersect(l.programmierung.toSet()).isEmpty())
    }

    @Test fun reiterDesOffenenGespraechs() {
        assertEquals(LeistenReiter.PROGRAMMIERUNG, reiterFuer(programmierung = true, projekt = ""))
        assertEquals(LeistenReiter.PROGRAMMIERUNG, reiterFuer(programmierung = true, projekt = "p1"))
        assertEquals(LeistenReiter.PROJEKTE, reiterFuer(programmierung = false, projekt = "p1"))
        assertEquals(LeistenReiter.GESPRAECHE, reiterFuer(programmierung = false, projekt = ""))
    }
}

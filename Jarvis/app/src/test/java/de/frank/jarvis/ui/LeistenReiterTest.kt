package de.frank.jarvis.ui

import de.frank.jarvis.data.Sitzung
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeistenReiterTest {

    /** Alle Testgespräche sind jünger: Archiviert wird hier nichts. */
    private val JETZT = 1_000L

    private fun s(id: String, projekt: String = "", programmierung: Boolean = false) = Sitzung(id, id, 1L, 2L, 3, projekt, programmierung)

    @Test fun programmiergespraecheNurUnterProgrammierung() {
        val alle = listOf(s("chat"), s("code", programmierung = true), s("projektchat", "p1"), s("projektcode", "p1", true), s("verwaist", "weg"))
        val l = aufteilen(alle, setOf("p1"), JETZT)
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
        val l = aufteilen(listOf(alt, mitte, neu), emptySet(), JETZT)
        assertEquals(listOf("neu", "mitte", "alt"), l.programmierung.map { it.id })
    }

    @Test fun archivierteFehlenFavoritenUndOffenesBleiben() {
        val tag = 24L * 60 * 60 * 1000
        val jetzt = 100L * tag
        val altChat = Sitzung("altChat", "a", 1L, jetzt - 31 * tag, 3)
        val jungChat = Sitzung("jungChat", "j", 1L, jetzt - 29 * tag, 3)
        val altCode = Sitzung("altCode", "c", 1L, jetzt - 11 * tag, 3, programmierung = true)
        val stern = Sitzung("stern", "s", 1L, jetzt - 90 * tag, 3, programmierung = true, favorit = true)
        val offen = Sitzung("offen", "o", 1L, jetzt - 90 * tag, 3)
        val l = aufteilen(listOf(altChat, jungChat, altCode, stern, offen), emptySet(), jetzt, offen = "offen")
        assertEquals(listOf("jungChat", "offen"), l.gespraeche.map { it.id })
        assertEquals(listOf("stern"), l.programmierung.map { it.id })
    }

    @Test fun reiterDesOffenenGespraechs() {
        assertEquals(LeistenReiter.PROGRAMMIERUNG, reiterFuer(programmierung = true, projekt = ""))
        assertEquals(LeistenReiter.PROGRAMMIERUNG, reiterFuer(programmierung = true, projekt = "p1"))
        assertEquals(LeistenReiter.PROJEKTE, reiterFuer(programmierung = false, projekt = "p1"))
        assertEquals(LeistenReiter.GESPRAECHE, reiterFuer(programmierung = false, projekt = ""))
    }
}

package de.frank.gedankenspeicher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NachtraegeTest {

    private val zeit1 = 1_780_000_000_000L
    private val zeit2 = 1_780_000_600_000L

    @Test
    fun zweiNachtraegeBekommenBeideIhreZeileOhneAbsturz() {
        val text = "Hallo\n\nWelt\n\nFoo"
        val (neu, zeiten) = Nachtraege.setzeZeilenEin(text, listOf(7 to zeit1, 13 to zeit2))
        val erwartet = "Hallo\n\n" + Nachtraege.zeile(zeit1) + "\nWelt\n\n" +
            Nachtraege.zeile(zeit2) + "\nFoo"
        assertEquals(erwartet, neu)
        assertEquals(2, zeiten.size)
        assertTrue(zeiten.containsAll(listOf(zeit1, zeit2)))
    }

    @Test
    fun leererNachtragBekommtKeineZeile() {
        val text = "Hallo\n\n"
        val (neu, zeiten) = Nachtraege.setzeZeilenEin(text, listOf(7 to zeit1))
        assertEquals(text, neu)
        assertTrue(zeiten.isEmpty())
    }

    @Test
    fun tippenVorDerStelleVerschiebtSie() {
        val alt = "Hallo\n\nWelt"
        val neu = "XXHallo\n\nWelt"
        val stellen = Nachtraege.verschiebeStellen(listOf(7 to zeit1), alt, neu)
        assertEquals(listOf(9 to zeit1), stellen)
    }

    @Test
    fun tippenHinterDerStelleVerschiebtSieNicht() {
        val alt = "Hallo\n\nWelt"
        val neu = "Hallo\n\nWelt und mehr"
        val stellen = Nachtraege.verschiebeStellen(listOf(7 to zeit1), alt, neu)
        assertEquals(listOf(7 to zeit1), stellen)
    }

    @Test
    fun loeschenVorDerStelleZiehtSieZurueck() {
        val alt = "Hallo\n\nWelt"
        val neu = "Hao\n\nWelt"
        val stellen = Nachtraege.verschiebeStellen(listOf(7 to zeit1), alt, neu)
        assertEquals(listOf(5 to zeit1), stellen)
    }

    @Test
    fun zeileUndAbschnittstextErgebenDenOriginaltext() {
        val nachtrag = "— Nachtrag vom 01.09.2026, 10:00 —\n\nB"
        val abschnitte = Nachtraege.abschnitte("A\n\n$nachtrag")
        assertEquals(2, abschnitte.size)
        val abschnitt = abschnitte[1]
        assertEquals("01.09.2026, 10:00", abschnitt.nachtragVom)
        assertEquals(nachtrag, Nachtraege.zeileVon(abschnitt.nachtragVom!!) + abschnitt.text)
    }
}

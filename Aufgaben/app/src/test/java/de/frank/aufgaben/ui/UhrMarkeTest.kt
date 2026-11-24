package de.frank.aufgaben.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UhrMarkeTest {
    // Maßstab wie in der App bei Dichte 1: 42 px pro Stunde, Leiste beginnt bei y = 0 um 8 Uhr.
    private val stunde = 42f
    private fun y(min: Int) = (min - 8 * 60) / 60f * stunde
    private fun knopf(min: Int) = Rechteck(150f, y(min) - 9f, 230f, y(min) + 9f)
    private val stunden = (8..14).map { y(it * 60) }
    private fun lage(min: Int, karten: List<Rechteck>) = uhrLage(knopf(min), karten, 2f, y(min), 18f, stunden, 14f, 2f)

    // Karte für 11:00, eine halbe Stunde lang: 39 px hoch, mittig auf der Startzeit, ab Spalte 54.
    private val karte11 = Rechteck(54f, y(660) - 19.5f, 360f, y(660) + 19.5f)

    @Test fun kurzVorElfWechseltNachLinksUndErsetztNurElfUhr() {
        val l = lage(651, listOf(karte11)) // 10:51
        assertTrue(l.links)
        assertEquals(setOf(3), l.verdeckt) // Index 3 = 11 Uhr; 10 Uhr bleibt sichtbar
    }

    @Test fun mitPlatzBleibtDieUhrzeitInDerMitte() {
        val l = lage(570, listOf(karte11)) // 09:30
        assertFalse(l.links)
        assertTrue(l.verdeckt.isEmpty())
    }

    @Test fun nachDerKarteKehrtSieInDieMitteZurueck() {
        assertTrue(lage(675, listOf(karte11)).links) // 11:15 liegt noch auf der Karte
        val l = lage(705, listOf(karte11)) // 11:45, Karte ist vorbei
        assertFalse(l.links)
        assertTrue(l.verdeckt.isEmpty())
    }

    @Test fun zwischenZweiStundenBleibenBeideSichtbar() {
        val l = lage(630, listOf(Rechteck(54f, y(630) - 19.5f, 360f, y(630) + 19.5f))) // 10:30 auf einer Karte
        assertTrue(l.links)
        assertTrue(l.verdeckt.isEmpty())
    }
}

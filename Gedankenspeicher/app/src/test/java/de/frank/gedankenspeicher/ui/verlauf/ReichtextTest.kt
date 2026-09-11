package de.frank.gedankenspeicher.ui.verlauf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReichtextTest {

    @Test
    fun adresseInKlammerVerschwindetSamtLeererKlammer() {
        assertEquals("Mehr hier.", Reichtext.ohneQuellen("Mehr (https://x.de) hier."))
    }

    @Test
    fun adresseFrisstDieSchliessendeKlammerNicht() {
        val ergebnis = Reichtext.ohneQuellen("Mehr (siehe https://x.de) hier.")
        assertFalse(ergebnis.contains("x.de"))
        assertTrue(ergebnis.endsWith(") hier."))
        assertEquals(ergebnis.count { it == '(' }, ergebnis.count { it == ')' })
    }

    @Test
    fun adresseAmSatzendeBehaeltDenPunkt() {
        assertEquals("unter.", Reichtext.ohneQuellen("unter https://x.de."))
    }

    @Test
    fun einrueckungAmZeilenanfangBleibt() {
        assertEquals("1. Punkt\n   weiter", Reichtext.ohneQuellen("1. Punkt\n   weiter"))
    }

    @Test
    fun svgCodeblockLaesstQuellenImFolgetextNichtStehen() {
        val ergebnis = Reichtext.ohneQuellen("```svg\n<svg></svg>\n```\nMehr (https://x.de) hier.")
        assertFalse(ergebnis.contains("x.de"))
        assertEquals("```svg\n<svg></svg>\n```\nMehr hier.", ergebnis)
    }

    @Test
    fun adresseMitKlammergruppeVerschwindetGanz() {
        assertEquals("Siehe dazu.", Reichtext.ohneQuellen("Siehe https://de.wikipedia.org/wiki/Merkur_(Planet) dazu."))
        assertEquals("Mehr hier.", Reichtext.ohneQuellen("Mehr (https://de.wikipedia.org/wiki/Merkur_(Planet)) hier."))
    }

    @Test
    fun codeblockBleibtUnveraendert() {
        val text = "Code:\n```\nprint()\n```"
        assertEquals(text, Reichtext.ohneQuellen(text))
    }
}

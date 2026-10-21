package de.frank.jarvis.faehigkeit

import de.frank.jarvis.faehigkeit.DiktatTrenner.TRENNER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiktatTrennerTest {
    @Test fun einDiktatBekommtGenauEinenTrenner() = assertEquals("Hallo Jarvis$TRENNER", DiktatTrenner.anhaengen("", " Hallo Jarvis "))

    @Test fun mehrereDiktateBleibenGetrennt() {
        val zwei = DiktatTrenner.anhaengen(DiktatTrenner.anhaengen("", "Erstens"), "Zweitens")
        assertEquals("Erstens${TRENNER}Zweitens$TRENNER", zwei)
    }

    @Test fun leereBeitraegeAendernNichts() {
        val e = "Erstens$TRENNER"
        assertEquals(e, DiktatTrenner.anhaengen(e, ""))
        assertEquals(e, DiktatTrenner.anhaengen(e, "  "))
        assertEquals(e, DiktatTrenner.anhaengen(e, ";"))
    }

    @Test fun getipptesBleibtUndKeinDoppelterTrenner() {
        val vorspann = ProgrammiererVorlage.einsetzen("")
        assertEquals("${vorspann}Filter einbauen$TRENNER", DiktatTrenner.anhaengen(vorspann, "Filter einbauen"))
        assertEquals("Text ;\n\nNeu$TRENNER", DiktatTrenner.anhaengen("Text ; ", "Neu"))
        // Ein Semikolon direkt am Wort ist kein Trenner: Der nächste Beitrag bekommt seinen eigenen.
        assertEquals("val x = 1; Neu$TRENNER", DiktatTrenner.anhaengen("val x = 1;", "Neu"))
    }

    @Test fun abschliessenderTrennerGehtNichtMit() {
        assertEquals("Eins", DiktatTrenner.ohneSchluss("Eins$TRENNER"))
        assertEquals("Eins${TRENNER}Zwei", DiktatTrenner.ohneSchluss("Eins${TRENNER}Zwei$TRENNER"))
        assertEquals("for (;;) x", DiktatTrenner.ohneSchluss("for (;;) x"))
        assertEquals("", DiktatTrenner.ohneSchluss(";\n\n"))
    }

    @Test fun einzelneAufgabeMitSchlusstrennerBleibtEine() {
        assertFalse(DiktatTrenner.mehrteilig("Knopf grün machen$TRENNER"))
        assertEquals("Knopf grün machen", DiktatTrenner.auftrag("Knopf grün machen$TRENNER"))
    }

    @Test fun dreiAufgabenEinAuftragMitZeilentrennern() {
        val roh = "Aufgabe eins\n;\nAufgabe zwei\n\n  ;  \n\nAufgabe drei$TRENNER;\n"
        assertTrue(DiktatTrenner.mehrteilig(roh))
        assertEquals(listOf("Aufgabe eins", "Aufgabe zwei", "Aufgabe drei"), DiktatTrenner.abschnitte(roh))
        assertEquals("Aufgabe eins${TRENNER}Aufgabe zwei${TRENNER}Aufgabe drei", DiktatTrenner.auftrag(roh))
    }

    @Test fun tippformUndCodeBleibenUnangetastet() {
        // „ ; “ erkennt die CLI selbst; Semikola in Code dürfen nicht zerlegt werden.
        val text = "Aufgabe eins ; Aufgabe zwei mit for (i = 0 ; i < n ; i++)"
        assertEquals(text, DiktatTrenner.auftrag(text))
    }

    @Test fun ohneTrennerUnveraendert() {
        val text = "Baue einen Filter ein.\nDer Knopf soll grün sein; danach bauen."
        assertFalse(DiktatTrenner.mehrteilig(text))
        assertEquals(text, DiktatTrenner.auftrag("  $text  "))
    }

    @Test fun leereAbschnitteFallenWeg() {
        assertEquals("Eins${TRENNER}Zwei", DiktatTrenner.auftrag(";\n\nEins$TRENNER;$TRENNER  \n;\nZwei"))
        assertEquals("Knopf grün", DiktatTrenner.auftrag(";\n\nKnopf grün\n;\n"))
        assertEquals("Eins$TRENNER})", DiktatTrenner.auftrag("Eins\n;\n})"))
    }
}

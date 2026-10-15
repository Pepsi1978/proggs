package de.frank.aufgaben.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression 15.10.2026: Jarvis legte „21:00“ an, gespeichert wurde 02:00 — die alte Regex las nur die „2“.
 * anlegen und aendern der Jarvis-Brücke lesen die Uhrzeit beide über [Tage.minutenAus] und geben sie über [Tage.zeit] zurück.
 */
class TageUhrzeitTest {
    private fun rundreise(eingabe: String): String = Tage.zeit(Tage.minutenAus(eingabe)!!)

    @Test fun anlegen_21_00_bleibt_21_00() {
        assertEquals(21 * 60, Tage.minutenAus("21:00"))
        assertEquals("21:00", rundreise("21:00"))
    }

    @Test fun anlegen_02_00_bleibt_02_00() {
        assertEquals(2 * 60, Tage.minutenAus("02:00"))
        assertEquals("02:00", rundreise("02:00"))
        assertEquals("02:00", rundreise("2:00"))
    }

    @Test fun aendern_20_00_bleibt_20_00() {
        assertEquals(20 * 60, Tage.minutenAus("20:00"))
        assertEquals("20:00", rundreise("20:00"))
    }

    @Test fun aendern_23_30_bleibt_23_30() {
        assertEquals(23 * 60 + 30, Tage.minutenAus("23:30"))
        assertEquals("23:30", rundreise("23:30"))
    }

    @Test fun alle_stunden_und_viertel_unveraendert() {
        for (h in 0..23) for (m in listOf(0, 15, 30, 45)) {
            val text = "%02d:%02d".format(h, m)
            assertEquals(text, rundreise(text))
        }
    }

    @Test fun nur_stunde_und_varianten() {
        assertEquals(20 * 60, Tage.minutenAus("20"))
        assertEquals(21 * 60, Tage.minutenAus("21 Uhr"))
        assertEquals(21 * 60 + 15, Tage.minutenAus("21.15"))
        assertEquals(9 * 60 + 5, Tage.minutenAus(" 9:05"))
    }

    @Test fun ungueltig_ergibt_null_statt_falscher_stunde() {
        assertNull(Tage.minutenAus("24:00"))
        assertNull(Tage.minutenAus("25:00"))
        assertNull(Tage.minutenAus("2100"))
        assertNull(Tage.minutenAus("abends"))
    }
}

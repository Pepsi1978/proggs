package de.frank.jarvis.speech

import de.frank.jarvis.live.LiveGespraech
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SprachZustellungTest {
    private val beginn = 1_000_000L

    @Test fun stilleEndeVerwirftNichts() {
        // Kein bewusstes Ende (0) oder eines vor der Anfrage: Das Ergebnis wird gesprochen.
        assertEquals(SprachZustellung.Ausgang.SPRECHEN, SprachZustellung.entscheide(beginn, 0L, vorlesenAn = true, ruhe = false))
        assertEquals(SprachZustellung.Ausgang.SPRECHEN, SprachZustellung.entscheide(beginn, beginn - 1, vorlesenAn = true, ruhe = false))
    }

    @Test fun bewusstesEndeGehtAllemVor() {
        assertEquals(SprachZustellung.Ausgang.BEWUSST_BEENDET, SprachZustellung.entscheide(beginn, beginn + 60_000, vorlesenAn = true, ruhe = false))
        assertEquals(SprachZustellung.Ausgang.BEWUSST_BEENDET, SprachZustellung.entscheide(beginn, beginn, vorlesenAn = false, ruhe = true))
    }

    @Test fun vorlesenAusUndRuhezeitWerdenRespektiert() {
        assertEquals(SprachZustellung.Ausgang.VORLESEN_AUS, SprachZustellung.entscheide(beginn, 0L, vorlesenAn = false, ruhe = false))
        assertEquals(SprachZustellung.Ausgang.RUHEZEIT, SprachZustellung.entscheide(beginn, 0L, vorlesenAn = true, ruhe = true))
    }

    @Test fun jedesErgebnisNurEinmal() {
        val schluessel = "vorgang:TEST" + System.nanoTime()
        assertTrue(SprachZustellung.einmal(schluessel))
        // Doppeltes Fertig-Ereignis (Dienst und Live gleichzeitig, Wiederholung): nichts zweimal vorlesen.
        assertFalse(SprachZustellung.einmal(schluessel))
        assertTrue(SprachZustellung.einmal(schluessel + "-anders"))
    }

    @Test fun modusKennungRundUndUnbekanntLeer() {
        SprachModus.entries.forEach { assertEquals(it, SprachModus.aus(it.kennung)) }
        assertNull(SprachModus.aus(""))
        assertNull(SprachModus.aus("chatgpt"))
    }

    @Test fun liveStilleEndeNurNachEingestellterZeit() {
        val t = 10_000_000L
        assertFalse(LiveGespraech.stilleAbgelaufen(0, 0L, t))
        assertFalse(LiveGespraech.stilleAbgelaufen(5, t - 5 * 60_000L, t))
        assertTrue(LiveGespraech.stilleAbgelaufen(5, t - 5 * 60_000L - 1, t))
    }
}

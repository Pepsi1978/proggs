package de.frank.jarvis.live

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiFehlerTest {
    /** Wortlaut aus dem Logcat vom 21.10.2026, 12:53 und 12:54 Uhr (nach „Connection reset“ beim Fortsetzen). */
    private val schonVerbunden = "BidiGenerateContent session is already connected to an existing client"

    @Test fun fortsetzungAbgelehntErkannt() = assertTrue(GeminiFehler.schonVerbunden(schonVerbunden))
    // modell() trifft 1008 weiter; deshalb prüft onClosing schonVerbunden() vorher.
    @Test fun bleibtFuerModellPruefungEin1008() = assertTrue(GeminiFehler.modell(1008, schonVerbunden))
    @Test fun keinKontingentOderSchluessel() {
        assertFalse(GeminiFehler.kontingent(schonVerbunden))
        assertFalse(GeminiFehler.schluessel(schonVerbunden))
    }
    @Test fun echteAblehnungNichtSchonVerbunden() {
        assertFalse(GeminiFehler.schonVerbunden("Request contains an invalid argument."))
        assertFalse(GeminiFehler.schonVerbunden("API key not valid. Please pass a valid API key."))
        assertFalse(GeminiFehler.schonVerbunden(""))
    }
}

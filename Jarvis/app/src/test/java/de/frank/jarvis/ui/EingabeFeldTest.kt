package de.frank.jarvis.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EingabeFeldTest {
    @Test fun tippenGilt() = assertTrue(EingabeFeld.meldungGilt(false, "ab", "ab", "abc"))

    @Test fun cursorBewegungGilt() = assertTrue(EingabeFeld.meldungGilt(false, "ab", "ab", "ab"))

    @Test fun alterStandNachDemSendenVerfaellt() {
        // Blase schon eingeklappt, Feld meldet beim Verschwinden den gesendeten Text.
        assertFalse(EingabeFeld.meldungGilt(true, "gesendet", "", "gesendet"))
        assertFalse(EingabeFeld.meldungGilt(true, "", "", "gesendet"))
    }

    @Test fun alterStandBeiOffenerBlaseVerfaellt() = assertFalse(EingabeFeld.meldungGilt(false, "gesendet", "", "gesendet"))

    @Test fun tasteAufAltemStandUeberschreibtKeinDiktat() = assertFalse(EingabeFeld.meldungGilt(false, "a", "a ; b ; ", "ax"))
}

package de.frank.stacklabor.werftstudio.bruecke

import de.frank.stacklabor.werftstudio.domain.model.Einheit
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DosisEingabeTest {
    private fun gut(menge: String? = null, jeStueck: String? = null, stueck: String? = null, einheit: String? = null): Dosis =
        assertIs<DosisEingabe.Ergebnis.Gut>(DosisEingabe.lies(menge, jeStueck, stueck, einheit)).dosis

    @Test
    fun zweiGrammAusZweiKapselnSindJeEinGramm() {
        // „Füge Ashwagandha in Sport 2 hinzu, 2 Gramm, zwei Kapseln“
        val d = gut(menge = "2", stueck = "zwei", einheit = "Gramm")
        assertEquals(0, d.stueckzahl.compareTo(BigDecimal(2)))
        assertEquals(0, d.mengeJeStueck!!.compareTo(BigDecimal.ONE))
        assertEquals(Einheit.G, d.einheit)
    }

    @Test
    fun sechsGrammOhneStueckzahlIstEineEinnahme() {
        // „Füge Glycin in Sport 2 hinzu, 6 Gramm“
        val d = gut(menge = "6", einheit = "g")
        assertEquals(0, d.stueckzahl.compareTo(BigDecimal.ONE))
        assertEquals(0, d.mengeJeStueck!!.compareTo(BigDecimal(6)))
        assertEquals(Einheit.G, d.einheit)
    }

    @Test
    fun grammUndMilligrammWerdenNieVerwechseltOderUmgerechnet() {
        assertEquals(Einheit.MG, gut(menge = "500", einheit = "Milligramm").einheit)
        assertEquals(Einheit.G, gut(menge = "0,5", einheit = "gramm").einheit)
        assertEquals(Einheit.UG, gut(menge = "100", einheit = "mcg").einheit)
        assertEquals(0, gut(menge = "1,5", einheit = "g").mengeJeStueck!!.compareTo(BigDecimal("1.5")))
        assertEquals(0, DosisEingabe.inMilligramm(BigDecimal(2), Einheit.G)!!.compareTo(BigDecimal(2000)))
        assertEquals(0, DosisEingabe.inMilligramm(BigDecimal(2000), Einheit.MG)!!.compareTo(BigDecimal(2000)))
    }

    @Test
    fun nurKapselnOhneMenge() {
        val d = gut(stueck = "2")
        assertNull(d.mengeJeStueck)
        assertNull(d.einheit)
    }

    @Test
    fun fehlendeAngabenWerdenErfragtNichtErfunden() {
        assertTrue(assertIs<DosisEingabe.Ergebnis.Fehlt>(DosisEingabe.lies("2", null, null, null)).frage.contains("Einheit"))
        assertTrue(assertIs<DosisEingabe.Ergebnis.Fehlt>(DosisEingabe.lies(null, null, null, "g")).frage.contains("Dosis"))
        assertTrue(assertIs<DosisEingabe.Ergebnis.Fehlt>(DosisEingabe.lies("1", null, "3", "g")).frage.contains("je Stück"))
        assertIs<DosisEingabe.Ergebnis.Fehlt>(DosisEingabe.lies("2", null, null, "Esslöffelchen"))
        assertIs<DosisEingabe.Ergebnis.Fehlt>(DosisEingabe.lies("0", null, null, "g"))
    }
}

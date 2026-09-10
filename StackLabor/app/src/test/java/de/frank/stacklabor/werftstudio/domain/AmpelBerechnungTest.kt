package de.frank.stacklabor.werftstudio.domain

import de.frank.stacklabor.werftstudio.domain.model.Ampel
import de.frank.stacklabor.werftstudio.domain.model.Bewertungszelle
import de.frank.stacklabor.werftstudio.domain.model.StackZiel
import de.frank.stacklabor.werftstudio.domain.model.Wirkung
import kotlin.test.Test
import kotlin.test.assertEquals

class AmpelBerechnungTest {
    @Test
    fun `Zielgewicht wechselt exakt an den Ranggrenzen`() {
        assertEquals(3, AmpelBerechnung.zielgewicht(1))
        assertEquals(3, AmpelBerechnung.zielgewicht(3))
        assertEquals(2, AmpelBerechnung.zielgewicht(4))
        assertEquals(2, AmpelBerechnung.zielgewicht(7))
        assertEquals(1, AmpelBerechnung.zielgewicht(8))
    }

    @Test
    fun `Mittel wird ab Produkt sechs rot und ab zwei gelb`() {
        val zelle = zelle("m", "z", Wirkung.STOERT, 2)
        assertEquals(
            Ampel.ROT,
            AmpelBerechnung.mittelAmpel("m", true, listOf(StackZiel("s", "z", 3)), listOf(zelle)),
        )
        assertEquals(
            Ampel.GELB,
            AmpelBerechnung.mittelAmpel("m", true, listOf(StackZiel("s", "z", 8)), listOf(zelle)),
        )
        assertEquals(
            Ampel.GRUEN,
            AmpelBerechnung.mittelAmpel("m", true, listOf(StackZiel("s", "z", 8)), emptyList()),
        )
        assertEquals(
            Ampel.GRAU,
            AmpelBerechnung.mittelAmpel("m", false, listOf(StackZiel("s", "z", 1)), listOf(zelle)),
        )
    }

    @Test
    fun `Zielgrenzen und starke Stoerung werden exakt ausgewertet`() {
        val aktive = setOf("a", "b", "c")
        assertEquals(Ampel.GRAU, AmpelBerechnung.zielAmpel("z", aktive, emptyList()))
        assertEquals(
            Ampel.GELB,
            AmpelBerechnung.zielAmpel("z", aktive, listOf(zelle("a", "z", Wirkung.STUETZT, 2))),
        )
        assertEquals(
            Ampel.GRUEN,
            AmpelBerechnung.zielAmpel("z", aktive, listOf(zelle("a", "z", Wirkung.STUETZT, 3))),
        )
        assertEquals(
            Ampel.ROT,
            AmpelBerechnung.zielAmpel(
                "z",
                aktive,
                listOf(zelle("a", "z", Wirkung.STUETZT, 1), zelle("b", "z", Wirkung.STOERT, 2)),
            ),
        )
        assertEquals(
            Ampel.ROT,
            AmpelBerechnung.zielAmpel(
                "z",
                aktive,
                listOf(zelle("a", "z", Wirkung.STUETZT, 3), zelle("b", "z", Wirkung.STOERT, 3)),
            ),
        )
        assertEquals(
            Ampel.GELB,
            AmpelBerechnung.zielAmpel(
                "z",
                aktive,
                listOf(
                    zelle("a", "z", Wirkung.STUETZT, 3),
                    zelle("b", "z", Wirkung.STUETZT, 2),
                    zelle("c", "z", Wirkung.STOERT, 2),
                ),
            ),
        )
    }

    @Test
    fun `Graue Zielampel zaehlt in der Sammelampel wie gelb`() {
        assertEquals(Ampel.GRAU, AmpelBerechnung.sammelAmpel(listOf(Ampel.GRUEN), false))
        assertEquals(Ampel.GELB, AmpelBerechnung.sammelAmpel(listOf(Ampel.GRUEN, Ampel.GRAU), true))
        assertEquals(Ampel.ROT, AmpelBerechnung.sammelAmpel(listOf(Ampel.GRAU, Ampel.ROT), true))
        assertEquals(Ampel.GRUEN, AmpelBerechnung.sammelAmpel(listOf(Ampel.GRUEN), true))
    }

    private fun zelle(mittelId: String, zielId: String, wirkung: Wirkung, staerke: Int): Bewertungszelle =
        Bewertungszelle("b", mittelId, zielId, wirkung, staerke, "Grund")
}

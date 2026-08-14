package de.frank.stacklabor.werftstudio.domain

import de.frank.stacklabor.werftstudio.domain.model.Darreichungsform
import de.frank.stacklabor.werftstudio.domain.model.DosisVariante
import de.frank.stacklabor.werftstudio.domain.model.Einheit
import de.frank.stacklabor.werftstudio.domain.model.FrequenzTyp
import de.frank.stacklabor.werftstudio.domain.model.Loeslichkeit
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PruefsummeTest {
    @Test
    fun `Sortierreihenfolge beeinflusst die Pruefsumme nicht`() {
        val original = inhalt()
        val umsortiert = original.copy(
            eintraege = original.eintraege.reversed(),
            aktiveZielIds = original.aktiveZielIds.reversed(),
            fragen = original.fragen.reversed(),
        )
        assertEquals(Pruefsumme.berechnen(original), Pruefsumme.berechnen(umsortiert))
    }

    @Test
    fun `Bewertungsrelevante Inhaltsaenderungen aendern die Pruefsumme`() {
        val original = inhalt()
        assertNotEquals(
            Pruefsumme.berechnen(original),
            Pruefsumme.berechnen(original.copy(einnahmeHinweis = "mit Wasser")),
        )
        assertNotEquals(
            Pruefsumme.berechnen(original),
            Pruefsumme.berechnen(original.copy(aktiveZielIds = listOf("ziel-1", "ziel-3"))),
        )
        assertNotEquals(
            Pruefsumme.berechnen(original),
            Pruefsumme.berechnen(original.copy(dosisVariante = DosisVariante.DIENST)),
        )
    }

    @Test
    fun `Numerisch gleiche Dezimalwerte ergeben dieselbe Pruefsumme`() {
        val original = inhalt()
        val normalisiert = original.copy(
            eintraege = original.eintraege.map { eintrag ->
                eintrag.copy(
                    dosen = eintrag.dosen.map { dosis ->
                        dosis.copy(mengeJeStueck = dosis.mengeJeStueck?.setScale(3))
                    },
                )
            },
        )
        assertEquals(Pruefsumme.berechnen(original), Pruefsumme.berechnen(normalisiert))
    }

    private fun inhalt(): StackPruefsummenInhalt {
        val mittel = PruefsummenMittel(
            id = "mittel-1",
            loeslichkeit = Loeslichkeit.WASSER,
            darreichungsform = Darreichungsform.KAPSEL,
            komponenten = listOf(
                PruefsummenKomponente("k-1", "Vitamin C", BigDecimal("80"), Einheit.MG),
            ),
        )
        return StackPruefsummenInhalt(
            zeitpunkt = "Morgens",
            einnahmeHinweis = "nuechtern",
            dosisVariante = DosisVariante.FREI,
            eintraege = listOf(
                PruefsummenEintrag(
                    id = "eintrag-1",
                    mittel = mittel,
                    frequenzTyp = FrequenzTyp.TAEGLICH,
                    alleNTage = null,
                    gruppeId = null,
                    zusatztext = "Text",
                    dosen = listOf(
                        PruefsummenDosis(DosisVariante.FREI, BigDecimal.ONE, BigDecimal("50"), Einheit.MG),
                        PruefsummenDosis(DosisVariante.DIENST, BigDecimal.ONE, BigDecimal("75"), Einheit.MG),
                    ),
                    alternationsPartnerMittelIds = listOf("partner-b", "partner-a"),
                ),
                PruefsummenEintrag(
                    id = "eintrag-2",
                    mittel = mittel.copy(id = "mittel-2", komponenten = emptyList()),
                    frequenzTyp = FrequenzTyp.ALLE_N_TAGE,
                    alleNTage = 3,
                    gruppeId = "gruppe",
                    zusatztext = "",
                    dosen = listOf(
                        PruefsummenDosis(DosisVariante.FREI, BigDecimal("2"), BigDecimal("80"), Einheit.MG),
                    ),
                    alternationsPartnerMittelIds = emptyList(),
                ),
            ),
            aktiveZielIds = listOf("ziel-2", "ziel-1"),
            fragen = listOf(PruefsummenFrage("frage-2", "Zwei"), PruefsummenFrage("frage-1", "Eins")),
        )
    }
}

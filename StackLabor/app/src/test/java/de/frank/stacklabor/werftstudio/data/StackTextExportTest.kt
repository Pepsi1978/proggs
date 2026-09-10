package de.frank.stacklabor.werftstudio.data

import de.frank.stacklabor.werftstudio.data.transfer.StackExportDaten
import de.frank.stacklabor.werftstudio.data.transfer.StackTextExport
import de.frank.stacklabor.werftstudio.data.transfer.StackZielText
import de.frank.stacklabor.werftstudio.domain.model.Darreichungsform
import de.frank.stacklabor.werftstudio.domain.model.DosisVariante
import de.frank.stacklabor.werftstudio.domain.model.Einheit
import de.frank.stacklabor.werftstudio.domain.model.FrequenzTyp
import de.frank.stacklabor.werftstudio.domain.model.Loeslichkeit
import de.frank.stacklabor.werftstudio.domain.model.Mittel
import de.frank.stacklabor.werftstudio.domain.model.Stack
import de.frank.stacklabor.werftstudio.domain.model.StackEintrag
import de.frank.stacklabor.werftstudio.domain.model.StackEintragDosis
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StackTextExportTest {
    private val zeitpunkt = 1_755_000_000_000L

    private fun daten(name: String = "Morgen-Stack"): StackExportDaten {
        val mittel = Mittel(
            id = "pea",
            name = "PEA (Palmitoylethanolamid)",
            loeslichkeit = Loeslichkeit.WASSER,
            darreichungsform = Darreichungsform.KAPSEL,
            hersteller = "Thorne",
            durchfallrisiko = true,
            beistoffe = "Reismehl",
        )
        val eintrag = StackEintrag(
            id = "eintrag-1",
            stackId = "stack-1",
            mittelId = "pea",
            frequenzTyp = FrequenzTyp.ALLE_N_TAGE,
            alleNTage = 3,
            reihenfolge = 1,
            zusatztext = "Nüchtern nehmen",
            offenerHinweis = "stützt Ziel 1",
            dosen = listOf(
                StackEintragDosis("eintrag-1", DosisVariante.FREI, BigDecimal("2"), BigDecimal("80"), Einheit.MG),
                StackEintragDosis("eintrag-1", DosisVariante.DIENST, BigDecimal("1"), BigDecimal("80"), Einheit.MG),
            ),
        )
        return StackExportDaten(
            stack = Stack("stack-1", name, "06:00", "nüchtern", 1),
            eintraege = listOf(eintrag),
            mittelNachId = mapOf("pea" to mittel),
            ziele = listOf(StackZielText(1, "Entzündungen senken")),
        )
    }

    @Test
    fun `einzelner Stack enthaelt alle Angaben zum Mittel`() {
        val text = StackTextExport.einzelnerStack(daten(), DosisVariante.FREI, "0.1.6", zeitpunkt)

        assertTrue("MORGEN-STACK" in text, "Die Überschrift fehlt")
        assertTrue("06:00 · nüchtern" in text)
        assertTrue("1. Entzündungen senken" in text, "Das Ziel fehlt")
        assertTrue("PEA (Palmitoylethanolamid)" in text)
        assertTrue("2 × 80 mg = 160 mg" in text, "Die freie Dosis fehlt")
        assertTrue("1 × 80 mg = 80 mg" in text, "Die Dienst-Dosis fehlt")
        assertTrue("alle 3 Tage" in text)
        assertTrue("wasserlöslich" in text)
        assertTrue("Kapsel" in text)
        assertTrue("Thorne" in text)
        assertTrue("Reismehl" in text)
        assertTrue("Nüchtern nehmen" in text)
        assertTrue("stützt Ziel 1" in text)
        assertTrue("App-Version 0.1.6" in text)
    }

    @Test
    fun `alle Stacks stehen nacheinander mit Trennung`() {
        val text = StackTextExport.alleStacks(
            listOf(daten("Morgen-Stack"), daten("Abend-Stack")),
            DosisVariante.FREI,
            "0.1.6",
            zeitpunkt,
        )

        val morgen = text.indexOf("MORGEN-STACK")
        val abend = text.indexOf("ABEND-STACK")
        assertTrue(morgen in 1..<abend, "Die Reihenfolge der Stacks stimmt nicht")
        assertTrue("ÜBERSICHT" in text, "Die Inhaltsübersicht fehlt")
        assertTrue("═" in text, "Die Trennlinie zwischen den Stacks fehlt")
        assertTrue("2 Stacks, 2 Einträge" in text)
    }

    @Test
    fun `Dateiname enthaelt keine verbotenen Zeichen`() {
        val name = StackTextExport.dateiname("Morgen/Stack: Teil 1", zeitpunkt)

        assertTrue(name.endsWith(".txt"))
        assertTrue(name.none { it in "\\/:*?\"<>| " }, "Der Dateiname enthält ein verbotenes Zeichen: $name")
        assertTrue(name.startsWith("Morgen-Stack--Teil-1"), "Unerwarteter Dateiname: $name")
    }

    @Test
    fun `leerer Stack meldet das ausdruecklich`() {
        val leer = daten().copy(eintraege = emptyList(), ziele = emptyList())

        val text = StackTextExport.einzelnerStack(leer, DosisVariante.DIENST, "0.1.6", zeitpunkt)

        assertTrue("keine Ziele festgelegt" in text)
        assertTrue("noch kein Mittel eingetragen" in text)
        assertEquals(true, "Dosis-Variante: Dienst" in text)
    }
}

package de.frank.stacklabor.werftstudio.bruecke

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NamensaufloesungTest {
    /** Namen wie auf Franks Handy (Beispiel aus dem Auftrag), keine echten Daten. */
    private val franks = listOf("Nach Aufstehen 1", "Nach Aufstehen 2", "Sport 1", "Sport 2", "Sport 3", "Vor dem Schlafen 1", "Vor dem Schlafen 2")
        .mapIndexed { i, n -> Kandidat("s$i", n) }

    /** Namen wie im Startbestand der App. */
    private val startbestand = listOf("Morgen-Stack Teil 1", "Morgen-Stack Teil 2", "Pre-Sport-Stack", "Abend-Stack Teil 1", "Abend-Stack Teil 2", "Abend-Stack Teil 3")
        .mapIndexed { i, n -> Kandidat("b$i", n) }

    private fun stack(anfrage: String, liste: List<Kandidat> = franks): String =
        assertIs<Aufloesung.Eindeutig>(Namensaufloesung.stack(anfrage, liste), "„$anfrage“ sollte eindeutig sein").kandidat.name

    @Test
    fun sprachlicheVariantenTreffenDenRichtigenStack() {
        assertEquals("Sport 2", stack("Sport 2"))
        assertEquals("Sport 2", stack("Sportmodus 2"))
        assertEquals("Sport 2", stack("Sport-Stacks 2"))
        assertEquals("Sport 2", stack("sport-stack zwei"))
        assertEquals("Sport 2", stack("zweiter Sport-Stack"))
        assertEquals("Sport 2", stack("Sport2"))
        assertEquals("Nach Aufstehen 1", stack("nach dem Aufstehen 1"))
        assertEquals("Nach Aufstehen 2", stack("Aufstehen zwei"))
        assertEquals("Vor dem Schlafen 1", stack("vor schlafen eins"))
        assertEquals("Vor dem Schlafen 2", stack("Schlafen 2"))
    }

    @Test
    fun zahlenGeltenStreng() {
        // „Sport 2“ darf nie bei „Sport 3“ oder einem Stack ohne Zahl landen.
        assertIs<Aufloesung.Keiner>(Namensaufloesung.stack("Sport 4", franks))
        assertEquals("Sport 3", stack("Sport drei"))
    }

    @Test
    fun mehrdeutigeAnfragenFuehrenZurRueckfrage() {
        val sport = assertIs<Aufloesung.Mehrdeutig>(Namensaufloesung.stack("Sport", franks))
        assertEquals(listOf("Sport 1", "Sport 2", "Sport 3"), sport.kandidaten.map { it.name })
        assertIs<Aufloesung.Mehrdeutig>(Namensaufloesung.stack("Schlafen", franks))
        assertIs<Aufloesung.Mehrdeutig>(Namensaufloesung.stack("Stack 1", franks))
    }

    @Test
    fun startbestandsNamenFunktionierenEbenso() {
        assertEquals("Morgen-Stack Teil 1", stack("Morgen 1", startbestand))
        assertEquals("Abend-Stack Teil 3", stack("Abendstack drei", startbestand))
        assertEquals("Pre-Sport-Stack", stack("Pre-Sport", startbestand))
        assertIs<Aufloesung.Mehrdeutig>(Namensaufloesung.stack("Abend", startbestand))
    }

    @Test
    fun mittelMitTippfehlernUndZusaetzen() {
        val mittel = listOf("Ashwagandha KSM-66", "Glycin", "Magnesiumglycinat", "Vitamin D3 + K2", "Vitamin C").mapIndexed { i, n -> Kandidat("m$i", n) }
        assertEquals("Ashwagandha KSM-66", assertIs<Aufloesung.Eindeutig>(Namensaufloesung.mittel("Ashwagandha", mittel)).kandidat.name)
        assertEquals("Glycin", assertIs<Aufloesung.Eindeutig>(Namensaufloesung.mittel("glycin", mittel)).kandidat.name)
        assertEquals("Vitamin D3 + K2", assertIs<Aufloesung.Eindeutig>(Namensaufloesung.mittel("Vitamin D3", mittel)).kandidat.name)
        assertIs<Aufloesung.Mehrdeutig>(Namensaufloesung.mittel("Vitamin", mittel))
        assertEquals("Glycin", assertIs<Aufloesung.Eindeutig>(Namensaufloesung.mittel("Glyzin", mittel)).kandidat.name)
        assertTrue(Namensaufloesung.aehnlich("Ashwaganda KSM-66", mittel).any { it.name == "Ashwagandha KSM-66" })
        assertTrue(Namensaufloesung.gleich("Vitamin-C", "vitamin c"))
    }
}

package de.frank.jarvis.faehigkeit

import de.frank.jarvis.faehigkeit.ProgrammiererVorlage.VORSPANN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgrammiererVorlageTest {
    @Test fun leererEntwurfBekommtGenauDenVorspann() = assertEquals("$VORSPANN ", ProgrammiererVorlage.einsetzen(""))

    @Test fun getipptesBleibtAlsAufgabeDahinter() =
        assertEquals("$VORSPANN Knopf in Jarvis grün machen", ProgrammiererVorlage.einsetzen(" Knopf in Jarvis grün machen "))

    @Test fun vorspannKommtNichtDoppelt() {
        val schon = "$VORSPANN Filter in Geniale Aufgaben"
        assertEquals(schon, ProgrammiererVorlage.einsetzen(schon))
    }

    @Test fun nurVorspannStartetKeinenAuftrag() {
        assertTrue(ProgrammiererVorlage.ohneAufgabe("$VORSPANN "))
        assertTrue(ProgrammiererVorlage.ohneAufgabe("$VORSPANN …"))
        assertFalse(ProgrammiererVorlage.ohneAufgabe("$VORSPANN baue in Jarvis einen Filter ein"))
    }

    @Test fun ohneVorspannIstEsKeinProgrammiererText() {
        assertNull(ProgrammiererVorlage.aufgabe("Wie wird das Wetter?"))
        assertFalse(ProgrammiererVorlage.ohneAufgabe("Wie wird das Wetter?"))
        assertEquals("Filter einbauen", ProgrammiererVorlage.aufgabe("$VORSPANN Filter einbauen"))
    }
}

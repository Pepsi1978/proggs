package de.frank.jarvis.faehigkeit

import de.frank.jarvis.faehigkeit.ProgrammiererVorlage.VORSPANN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgrammiererVorlageTest {
    @Test fun einstiegMitModellGiltVerbindlich() {
        assertEquals(ProgrammiererVorlage.MODELL_OPUS, ProgrammiererVorlage.modell("${ProgrammiererVorlage.VORSPANN_OPUS} A ; B"))
        assertEquals(ProgrammiererVorlage.MODELL_SOL, ProgrammiererVorlage.modell("  ${ProgrammiererVorlage.VORSPANN_SOL} A"))
        assertNull(ProgrammiererVorlage.modell("$VORSPANN A"))
        assertNull(ProgrammiererVorlage.modell("Nimm bitte Opus 5.5"))
        assertTrue(ProgrammiererVorlage.hinweis("${ProgrammiererVorlage.VORSPANN_SOL} A").contains("modell=sol"))
        assertEquals(ProgrammiererVorlage.HINWEIS, ProgrammiererVorlage.hinweis("$VORSPANN A"))
    }

    @Test fun anderesModellErsetztDenEinstiegUndGetipptesBleibt() {
        val opus = ProgrammiererVorlage.einsetzen("Filter", ProgrammiererVorlage.VORSPANN_OPUS)
        assertEquals("${ProgrammiererVorlage.VORSPANN_OPUS} Filter", opus)
        assertEquals("${ProgrammiererVorlage.VORSPANN_SOL} Filter", ProgrammiererVorlage.einsetzen(opus, ProgrammiererVorlage.VORSPANN_SOL))
        assertEquals("Filter", ProgrammiererVorlage.entfernen(opus))
        assertEquals("Filter", ProgrammiererVorlage.aufgabe(opus))
        assertTrue(ProgrammiererVorlage.aktiv(opus))
        assertFalse(ProgrammiererVorlage.aktiv("Filter"))
    }

    @Test fun vorspannPasstZumModell() {
        assertEquals(ProgrammiererVorlage.VORSPANN_OPUS, ProgrammiererVorlage.vorspannFuer(ProgrammiererVorlage.MODELL_OPUS))
        assertEquals(ProgrammiererVorlage.VORSPANN_SOL, ProgrammiererVorlage.vorspannFuer(ProgrammiererVorlage.MODELL_SOL))
    }

    @Test(expected = IllegalArgumentException::class) fun unbekanntesModellWirdNieZuOpus() {
        ProgrammiererVorlage.vorspannFuer("fable")
    }

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

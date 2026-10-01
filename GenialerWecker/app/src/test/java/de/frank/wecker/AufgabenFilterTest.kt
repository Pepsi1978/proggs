package de.frank.wecker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AufgabenFilterTest {
    private val tag = 20_000L
    private val neun = 9 * 60

    @Test fun aufgabeVorDerWeckzeitFaelltWeg() = assertFalse(TasksBridge.nachWeckzeit(8 * 60, tag, tag, neun))
    @Test fun aufgabeZurWeckzeitBleibt() = assertTrue(TasksBridge.nachWeckzeit(neun, tag, tag, neun))
    @Test fun aufgabeNachDerWeckzeitBleibt() = assertTrue(TasksBridge.nachWeckzeit(14 * 60, tag, tag, neun))
    @Test fun aufgabeOhneUhrzeitBleibt() = assertTrue(TasksBridge.nachWeckzeit(-1, tag, tag, neun))
    @Test fun ueberfaelligeAufgabeBleibt() = assertTrue(TasksBridge.nachWeckzeit(8 * 60, tag - 1, tag, neun))
    @Test fun ohneGrenzeBleibtAlles() = assertTrue(TasksBridge.nachWeckzeit(8 * 60, tag, tag, null))
    @Test fun weckMinuteAusStundeUndMinute() =
        assertTrue(TasksBridge.weckMinute(Alarm(hour = 9, minute = 15)) == 9 * 60 + 15)
}

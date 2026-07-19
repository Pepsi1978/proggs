package de.frank.perfectmoment.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DictationTextTest {
    @Test
    fun firstDictationEndsWithSpace() {
        assertEquals("Erster Satz. ", appendDictation("", "Erster Satz."))
    }

    @Test
    fun laterDictationContinuesExistingText() {
        assertEquals(
            "Erster Satz. Zweiter Satz. ",
            appendDictation("Erster Satz. ", "Zweiter Satz."),
        )
    }

    @Test
    fun whitespaceIsNormalizedAtJoin() {
        assertEquals("Vorhanden Neu ", appendDictation("Vorhanden   ", "  Neu  "))
    }

    @Test
    fun blankTranscriptDoesNotChangeText() {
        assertEquals("Vorhanden ", appendDictation("Vorhanden ", "   "))
    }
}

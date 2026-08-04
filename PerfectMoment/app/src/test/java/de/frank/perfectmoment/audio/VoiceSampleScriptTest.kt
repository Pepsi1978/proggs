package de.frank.perfectmoment.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSampleScriptTest {
    @Test
    fun `falls back to the reference questions when nothing was generated`() {
        assertEquals(VoiceSampleScript.fallback, VoiceSampleScript.script(emptyList()))
        assertEquals(VoiceSampleScript.fallback, VoiceSampleScript.script(listOf("", "   ")))
    }

    @Test
    fun `cuts a long list down to about the reading budget`() {
        val questions = List(30) { "Warum fühlt sich mein Leben gerade so leicht und klar an?" }

        val script = VoiceSampleScript.script(questions)

        val words = script.sumOf { it.split(' ').size }
        assertTrue("zu wenig Text: $words Wörter", words >= VoiceSampleScript.TARGET_WORDS)
        // One question may push past the budget, none may follow it.
        assertTrue("zu viel Text: $words Wörter", words < VoiceSampleScript.TARGET_WORDS + 20)
        assertTrue(script.size < questions.size)
    }

    @Test
    fun `keeps every question when the generated ones are short`() {
        val questions = listOf("Warum jetzt?", "Wieso hier?", "Was noch?")

        assertEquals(questions, VoiceSampleScript.script(questions))
    }
}

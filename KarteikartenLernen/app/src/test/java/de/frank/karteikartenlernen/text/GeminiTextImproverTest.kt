package de.frank.karteikartenlernen.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiTextImproverTest {
    @Test
    fun payloadSeparatesEditorialInstructionFromSourceText() {
        val source = "warum ist hrv nachts höher und alkohol"
        val payload = geminiImprovementPayload(source)
        val instruction = payload.getJSONObject("system_instruction").getJSONArray("parts").getJSONObject(0).getString("text")
        val userText = payload.getJSONArray("contents").getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text")

        assertTrue(instruction.contains("Intention"))
        assertTrue(instruction.contains("Satzbau"))
        assertTrue(instruction.contains("füge keine neuen Fakten"))
        assertEquals(source, userText)
        assertFalse(instruction.contains(source))
    }

    @Test
    fun parserCombinesAllGeminiTextParts() {
        val body = """{"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"Warum ist "},{"text":"die HRV höher?"}]}}]}"""

        assertEquals("Warum ist die HRV höher?", parseGeminiImprovement(body))
    }
}

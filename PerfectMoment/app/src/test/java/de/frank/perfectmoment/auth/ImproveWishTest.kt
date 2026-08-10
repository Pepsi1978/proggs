package de.frank.perfectmoment.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImproveWishTest {
    private fun instructions(previous: List<String>) = codexImproveWishPayload(
        wish = "also ähm ich möchte ruhiger werden abends",
        previousVersions = previous,
        model = CodexModel.TERRA,
        reasoningEffort = ReasoningEffort.MEDIUM,
    ).getString("instructions")

    @Test
    fun `asks for a different wording once versions exist`() {
        val first = instructions(emptyList())
        val second = instructions(listOf("Ich möchte abends zur Ruhe kommen."))

        assertFalse(first.contains("bereits vorgeschlagen"))
        assertTrue(second.contains("bereits vorgeschlagen"))
        assertTrue(second.contains("Ich möchte abends zur Ruhe kommen."))
    }

    @Test
    fun `only polishes the wording and keeps the routine out of it`() {
        val instructions = codexImproveWishPayload(
            wish = "also ähm ich möchte ruhiger werden abends",
            previousVersions = emptyList(),
            model = CodexModel.TERRA,
            reasoningEffort = ReasoningEffort.MEDIUM,
        ).getString("instructions")

        assertFalse(instructions.contains("Routine"))
        assertFalse(instructions.contains("Anweisung"))
        assertTrue(instructions.contains("Füge NICHTS hinzu"))
        assertTrue(instructions.contains("Lass NICHTS weg"))
    }

    @Test
    fun `reads the rewritten version out of the answer`() {
        assertEquals(
            "Ich möchte abends zur Ruhe kommen.",
            parseImprovedWish("""{"fassung":"„Ich möchte abends zur Ruhe kommen.“"}"""),
        )
        assertEquals(
            "Ich möchte abends zur Ruhe kommen.",
            parseImprovedWish("```json\n{\"fassung\":\"Ich möchte abends zur Ruhe kommen.\"}\n```"),
        )
    }

    @Test
    fun `returns nothing when the answer is unusable`() {
        assertEquals("", parseImprovedWish("kein json"))
        assertEquals("", parseImprovedWish("""{"etwas":"anderes"}"""))
    }
}

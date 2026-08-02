package de.frank.perfectmoment.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreinstalledContentTest {
    @Test
    fun assumptionQuestionsIsAnAdditionalSkill() {
        assertNotEquals(
            PreinstalledContent.RESEARCH_TEAM_SKILL_NAME,
            PreinstalledContent.ASSUMPTION_QUESTIONS_SKILL_NAME,
        )
        assertTrue(PreinstalledContent.assumptionQuestionsSkillText.isNotBlank())
    }

    @Test
    fun consciousnessImageIsAThirdSkillOfItsOwn() {
        val names = listOf(
            PreinstalledContent.RESEARCH_TEAM_SKILL_NAME,
            PreinstalledContent.ASSUMPTION_QUESTIONS_SKILL_NAME,
            PreinstalledContent.CONSCIOUSNESS_IMAGE_SKILL_NAME,
        )

        assertEquals(names.size, names.distinct().size)
        assertTrue(PreinstalledContent.consciousnessImageSkillText.isNotBlank())
        assertNotEquals(
            PreinstalledContent.assumptionQuestionsSkillText,
            PreinstalledContent.consciousnessImageSkillText,
        )
    }

    @Test
    fun consciousnessImageDefinesTheRequiredOutput() {
        val text = PreinstalledContent.consciousnessImageSkillText

        assertTrue(text.startsWith("AUFGABE"))
        assertTrue(text.contains("Gib ausschließlich die erzeugten Fragen aus."))
        assertTrue(text.contains("Keine Nummerierung."))
        assertTrue(
            text.endsWith(
                "Schreibe jede Frage in eine eigene Zeile und beginne sie mit einem " +
                    "inhaltlich passenden Emoji.",
            ),
        )
    }

    @Test
    fun assumptionQuestionsDefinesTheRequiredOutput() {
        val text = PreinstalledContent.assumptionQuestionsSkillText

        assertTrue(text.contains("Genau dreißig Fragen."))
        assertTrue(text.contains("Vor jeder Frage ein Emoji"))
        assertTrue(text.contains("Keine Nummerierung."))
        assertTrue(text.contains("Keine Einleitung. Keine Ausleitung."))
        assertTrue(text.contains("Ich-Form als Standard."))
    }
}

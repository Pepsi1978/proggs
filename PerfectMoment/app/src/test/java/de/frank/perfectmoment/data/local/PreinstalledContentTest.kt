package de.frank.perfectmoment.data.local

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
    fun assumptionQuestionsDefinesTheRequiredOutput() {
        val text = PreinstalledContent.assumptionQuestionsSkillText

        assertTrue(text.contains("Genau dreißig Fragen."))
        assertTrue(text.contains("Vor jeder Frage ein Emoji"))
        assertTrue(text.contains("Keine Nummerierung."))
        assertTrue(text.contains("Keine Einleitung. Keine Ausleitung."))
        assertTrue(text.contains("Ich-Form als Standard."))
    }
}

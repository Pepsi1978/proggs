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
    fun assumptionReinforcementIsAFourthSkillOfItsOwn() {
        val names = listOf(
            PreinstalledContent.RESEARCH_TEAM_SKILL_NAME,
            PreinstalledContent.ASSUMPTION_QUESTIONS_SKILL_NAME,
            PreinstalledContent.CONSCIOUSNESS_IMAGE_SKILL_NAME,
            PreinstalledContent.ASSUMPTION_REINFORCEMENT_SKILL_NAME,
        )

        assertEquals(names.size, names.distinct().size)
        assertEquals("Annahmeverstärkung", PreinstalledContent.ASSUMPTION_REINFORCEMENT_SKILL_NAME)
        assertTrue(PreinstalledContent.assumptionReinforcementSkillText.isNotBlank())
        assertNotEquals(
            PreinstalledContent.consciousnessImageSkillText,
            PreinstalledContent.assumptionReinforcementSkillText,
        )
    }

    @Test
    fun assumptionReinforcementCarriesItsTwoCoreRules() {
        val text = PreinstalledContent.assumptionReinforcementSkillText

        // The whole point of this skill: a question may never ask for the target state
        // itself, and no word after "und" may sound like its own negation.
        assertTrue(text.contains("Das Grundgesetz: voraussetzen statt erfragen"))
        assertTrue(text.contains("Die Klangprüfung: kein"))
        assertTrue(text.contains("höchstens etwa jede fünfte Frage"))
    }

    @Test
    fun assumptionReinforcementDefinesTheRequiredOutput() {
        val text = PreinstalledContent.assumptionReinforcementSkillText

        // The front matter of the Claude Code skill file has no meaning inside the app.
        assertTrue(text.startsWith("# Annahmenverstärkung"))
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

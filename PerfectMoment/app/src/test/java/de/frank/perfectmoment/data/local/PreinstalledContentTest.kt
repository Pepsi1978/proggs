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

    @Test
    fun assumptionBoostIsAFourthSkillOfItsOwn() {
        val names = listOf(
            PreinstalledContent.RESEARCH_TEAM_SKILL_NAME,
            PreinstalledContent.ASSUMPTION_QUESTIONS_SKILL_NAME,
            PreinstalledContent.CONSCIOUSNESS_IMAGE_SKILL_NAME,
            PreinstalledContent.ASSUMPTION_BOOST_SKILL_NAME,
        )

        assertEquals(names.size, names.distinct().size)
        assertNotEquals(
            PreinstalledContent.consciousnessImageSkillText,
            PreinstalledContent.assumptionBoostSkillText,
        )
    }

    @Test
    fun assumptionBoostCarriesItsOwnRules() {
        val text = PreinstalledContent.assumptionBoostSkillText

        assertTrue(text.startsWith("AUFGABE"))
        assertTrue(text.contains("SPRACHREGEL: EINFACHE FRAGEN"))
        assertTrue(text.contains("GRUNDGESETZ DER ANNAHMENVERSTÄRKUNG"))
        assertTrue(text.contains("FRAGEFORMEN UND IHRE RANGFOLGE"))
        assertTrue(text.contains("KLANGREGEL: NACH „UND“ NIEMALS EIN WORT MIT „UN“-GEGENTEIL"))
        assertTrue(text.contains("PRÜFUNG VOR DER AUSGABE"))
        assertTrue(text.contains("Länge: 5 bis 12 Wörter. Absolute Obergrenze sind 14 Wörter."))
        assertTrue(
            text.endsWith(
                "Schreibe jede Frage in eine eigene Zeile und beginne sie mit einem " +
                    "inhaltlich passenden Emoji.",
            ),
        )
    }

    @Test
    fun assumptionBoost2IsAFifthSkillOfItsOwn() {
        val names = listOf(
            PreinstalledContent.RESEARCH_TEAM_SKILL_NAME,
            PreinstalledContent.ASSUMPTION_QUESTIONS_SKILL_NAME,
            PreinstalledContent.CONSCIOUSNESS_IMAGE_SKILL_NAME,
            PreinstalledContent.ASSUMPTION_BOOST_SKILL_NAME,
            PreinstalledContent.ASSUMPTION_BOOST_2_SKILL_NAME,
        )

        assertEquals(names.size, names.distinct().size)
        assertNotEquals(
            PreinstalledContent.assumptionBoostSkillText,
            PreinstalledContent.assumptionBoost2SkillText,
        )
    }

    @Test
    fun assumptionBoost2CarriesItsOwnRules() {
        val text = PreinstalledContent.assumptionBoost2SkillText

        assertTrue(text.startsWith("AUFGABE"))
        assertTrue(text.contains("DAS WIRKPRINZIP"))
        assertTrue(text.contains("EINFACH HEISST NICHT KURZ"))
        assertTrue(text.contains("DIE ACHT FRAGETYPEN"))
        assertTrue(text.contains("KLANGREGEL: NACH „UND“ NIE EIN WORT MIT „UN“-GEGENTEIL"))
        assertTrue(text.contains("VOLLSTÄNDIGES BEISPIEL"))
        assertTrue(
            text.endsWith("Das Problem aus der Eingabe erscheint in keiner einzigen Frage."),
        )
    }
}

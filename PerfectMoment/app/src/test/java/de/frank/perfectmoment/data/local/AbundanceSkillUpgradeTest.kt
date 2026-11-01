package de.frank.perfectmoment.data.local

import de.frank.perfectmoment.auth.IncrementalQuestionValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AbundanceSkillUpgradeTest {
    private val skills = mapOf(
        PreinstalledContent.RESEARCH_TEAM_SKILL_NAME to PreinstalledContent.researchTeamSkillText,
        PreinstalledContent.ASSUMPTION_QUESTIONS_SKILL_NAME to PreinstalledContent.assumptionQuestionsSkillText,
        PreinstalledContent.CONSCIOUSNESS_IMAGE_SKILL_NAME to PreinstalledContent.consciousnessImageSkillText,
        PreinstalledContent.ASSUMPTION_BOOST_SKILL_NAME to PreinstalledContent.assumptionBoostSkillText,
        PreinstalledContent.ASSUMPTION_BOOST_2_SKILL_NAME to PreinstalledContent.assumptionBoost2SkillText,
    )

    @Test
    fun everyPreinstalledSkillCarriesTheRuleOnce() {
        assertEquals(AbundanceSkillUpgrade.skillNames, skills.keys)
        skills.values.forEach { text ->
            assertEquals(2, text.split(AbundanceSkillUpgrade.RULE_HEADING).size)
        }
    }

    @Test
    fun everyContradictingPassageWasReplaced() {
        skills.forEach { (name, text) ->
            val passages = AbundanceSkillUpgrade.replacedPassages(name)
            assertTrue(passages.isNotEmpty())
            passages.forEach { assertFalse("$name: $it", text.contains(it)) }
        }
    }

    @Test
    fun upgradeIsRepeatableAndKeepsOwnSkillsUntouched() {
        skills.forEach { (name, text) -> assertEquals(text, AbundanceSkillUpgrade.apply(name, text)) }
        assertEquals("Mein Skill", AbundanceSkillUpgrade.apply("Eigener Skill", "Mein Skill"))
    }

    @Test
    fun editedSkillKeepsItsOwnTextAndGetsTheRule() {
        val upgraded = AbundanceSkillUpgrade.apply(
            PreinstalledContent.ASSUMPTION_BOOST_SKILL_NAME,
            "Meine eigene Fassung",
        )
        assertTrue(upgraded.startsWith("Meine eigene Fassung"))
        assertTrue(upgraded.contains(AbundanceSkillUpgrade.RULE_HEADING))
    }

    @Test
    fun seekingGoalQuestionsAreDroppedAndAbundantOnesStay() {
        val validator = IncrementalQuestionValidator()

        assertTrue(validator.accept("🌙 Wie erreiche ich, dass ich einen guten Schlaf habe?").isEmpty())
        assertTrue(validator.accept("🌙 Wie kannst du besser schlafen?").isEmpty())
        assertEquals(1, validator.accept("🌙 Wie habe ich es geschafft, dass es mir sehr gut geht?").size)
        assertEquals(1, validator.accept("🌙 Weshalb fühle ich mich voller Frieden?").size)
    }
}

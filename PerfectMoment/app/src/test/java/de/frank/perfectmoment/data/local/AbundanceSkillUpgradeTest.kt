package de.frank.perfectmoment.data.local

import de.frank.perfectmoment.auth.IncrementalQuestionValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
        // Die Rohtexte stehen in der Quelldatei: Nur so fällt eine Passage auf, die nie gepasst hat.
        val source = File("src/main/java/de/frank/perfectmoment/data/local/PreinstalledContent.kt")
            .readText()
            .replace("\r\n", "\n")
        skills.forEach { (name, text) ->
            val passages = AbundanceSkillUpgrade.replacedPassages(name)
            assertTrue(passages.isNotEmpty())
            passages.forEach {
                assertTrue("$name passt nicht: $it", source.contains(it))
                assertFalse("$name: $it", text.contains(it))
            }
        }
    }

    @Test
    fun renamedPreinstalledSkillIsStillUpgraded() {
        val raw = AbundanceSkillUpgrade.replacedPassages(PreinstalledContent.ASSUMPTION_BOOST_2_SKILL_NAME)
            .take(2)
            .joinToString("\n")

        assertTrue(AbundanceSkillUpgrade.carriesRule(AbundanceSkillUpgrade.apply("Annahme 2", raw)))
    }

    @Test
    fun firstRuleWordingIsLifted() {
        val text = PreinstalledContent.assumptionBoostSkillText
            .replace("Jede einzelne erzeugte Frage", "Jede einzelne Frage")

        assertEquals(
            PreinstalledContent.assumptionBoostSkillText,
            AbundanceSkillUpgrade.apply(PreinstalledContent.ASSUMPTION_BOOST_SKILL_NAME, text),
        )
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
        val validator = IncrementalQuestionValidator(enforceAbundance = true)

        listOf(
            "Wie erreiche ich, dass ich einen guten Schlaf habe?",
            "Wie kannst du besser schlafen?",
            "Und wie kann ich ruhiger werden?",
            "1. Wie kann ich ruhiger werden?",
            "Das Ziel ist klar. Wie erreiche ich es?",
            "Wie fühlt sich mein Körper an?",
            "Wie tief ist meine Ruhe?",
            "Was kann ich für besseren Schlaf tun?",
            "Warum werde ich bald fit sein?",
        ).forEach { assertTrue(it, validator.accept("🌙 $it").isEmpty()) }

        listOf(
            "Wie habe ich es geschafft, dass es mir sehr gut geht?",
            "Wie habe ich genug Geld zusammenbekommen, um mir das neue Auto zu kaufen?",
            "Weshalb fühle ich mich voller Frieden?",
            "Wodurch ist meine Fitness so stark gestiegen?",
        ).forEach { assertEquals(it, 1, validator.accept("🌙 $it").size) }
    }

    @Test
    fun ownSkillsWithoutRuleAreNotFiltered() {
        assertEquals(1, IncrementalQuestionValidator().accept("🌙 Wie kann ich ruhiger werden?").size)
    }
}

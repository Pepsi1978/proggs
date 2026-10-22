package de.frank.genialeideen.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class CodexModelsTest {
    @Test
    fun openAiAuswahlEnthaeltGenauDieAktuellenModelle() {
        assertEquals(
            listOf("GPT-6 Astra" to "gpt-6-astra", "GPT-6.1 Sol" to "gpt-6.1-sol",
                "GPT-5.6 Terra" to "gpt-5.6-terra", "GPT-6 Luna" to "gpt-6-luna"),
            CodexModel.von(KiAnbieter.OPENAI).map { it.label to it.apiId },
        )
    }

    @Test
    fun gespeicherteAlteAuswahlenBehaltenIhreModellfamilie() {
        listOf("gpt-5.6-sol", "GPT 5.6 Sol", "GPT-5.6 Sol", "gpt-6-sol", "GPT-6 Sol").forEach {
            assertEquals(CodexModel.SOL, CodexModel.fromLabel(" $it "))
        }
        listOf("gpt-5.6-luna", "GPT 5.6 Luna", "GPT-5.6 Luna").forEach {
            assertEquals(CodexModel.LUNA, CodexModel.fromLabel(it))
        }
        assertEquals(CodexModel.TERRA, CodexModel.fromLabel("GPT 5.6 Terra"))
        CodexModel.entries.forEach {
            assertEquals(it, CodexModel.fromLabel(it.apiId))
            assertEquals(it, CodexModel.fromLabel(it.label))
        }
    }

    @Test
    fun denkstufenPassenZumModell() {
        val mitUltra = listOf("low", "medium", "high", "xhigh", "max", "ultra")
        val ohneUltra = listOf("low", "medium", "high", "xhigh", "max")
        listOf(CodexModel.ASTRA, CodexModel.SOL, CodexModel.TERRA).forEach { modell ->
            assertEquals(mitUltra, modell.supportedEfforts.map { it.apiValue })
            assertEquals(ReasoningEffort.ULTRA, modell.normalizeEffort(ReasoningEffort.ULTRA))
        }
        (listOf(CodexModel.LUNA) + CodexModel.von(KiAnbieter.ANTHROPIC)).forEach { modell ->
            assertEquals(ohneUltra, modell.supportedEfforts.map { it.apiValue })
            assertEquals(ReasoningEffort.MEDIUM, modell.normalizeEffort(ReasoningEffort.ULTRA))
        }
        CodexModel.entries.forEach { modell ->
            modell.supportedEfforts.forEach { assertEquals(it, modell.normalizeEffort(it)) }
        }
    }
}

package de.frank.perfectmoment.auth

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Hält Modell-Kennungen und Denkstärken auf den Werten fest, die der Codex-Endpunkt
 * (https://chatgpt.com/backend-api/codex/responses) für gpt-5.6 tatsächlich annimmt.
 */
class CodexModelsTest {
    @Test
    fun `every model uses its official api id`() {
        assertEquals(
            listOf("gpt-5.6-sol", "gpt-5.6-terra", "gpt-5.6-luna"),
            CodexModel.entries.map { it.apiId },
        )
    }

    @Test
    fun `every reasoning level the api accepts is selectable`() {
        assertEquals(
            listOf("low", "medium", "high", "xhigh", "max"),
            ReasoningEffort.entries.map { it.apiValue },
        )
    }

    @Test
    fun `stored settings values map back to their entry`() {
        CodexModel.entries.forEach { model ->
            assertEquals(model, CodexModel.fromLabel(model.apiId))
            assertEquals(model, CodexModel.fromLabel(model.label))
        }
        ReasoningEffort.entries.forEach { effort ->
            assertEquals(effort, ReasoningEffort.fromLabel(effort.apiValue))
            assertEquals(effort, ReasoningEffort.fromLabel(effort.label))
        }
    }

    @Test
    fun `the request carries the chosen model and reasoning level`() {
        val payload = codexSummaryPayload("Thema", CodexModel.SOL, ReasoningEffort.MAX)

        assertEquals("gpt-5.6-sol", payload.getString("model"))
        assertEquals("max", payload.getJSONObject("reasoning").getString("effort"))
    }
}

package de.frank.karteikartenlernen.auth

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CodexAuthRetryTest {
    @Test
    fun dnsRetriesAreBoundedAndBackOff() {
        val delays = CodexAuthManager.DNS_RETRY_DELAYS_MS

        assertArrayEquals(longArrayOf(500L, 1_500L, 3_000L), delays)
        assertTrue(delays.asList().zipWithNext().all { (first, second) -> second > first })
    }

    @Test
    fun devicePollingKeepsTransientResponsesPending() {
        assertEquals(DevicePollAction.PROCESS, devicePollAction(200))
        assertEquals(DevicePollAction.PENDING, devicePollAction(403))
        assertEquals(DevicePollAction.PENDING, devicePollAction(404))
        assertEquals(DevicePollAction.PENDING, devicePollAction(429))
        assertEquals(DevicePollAction.PENDING, devicePollAction(503))
        assertEquals(DevicePollAction.FAIL, devicePollAction(400))
    }

    @Test
    fun devicePollingAcceptsStringIntervalFromOpenAi() {
        assertEquals(5, devicePollInterval("5"))
        assertEquals(3, devicePollInterval("0"))
        assertEquals(5, devicePollInterval(null))
    }

    @Test
    fun codexInputUsesRequiredUserItemList() {
        val input = codexInput("Warum ist die HRV nachts höher?")

        assertEquals(1, input.length())
        assertEquals("user", input.getJSONObject(0).getString("role"))
        assertEquals("Warum ist die HRV nachts höher?", input.getJSONObject(0).getString("content"))
    }

    @Test
    fun codexSseCombinesOutputDeltas() {
        val stream = """
            event: response.output_text.delta
            data: {"type":"response.output_text.delta","delta":"{\"title\":\"Test\","}

            data: {"type":"response.output_text.delta","delta":"\"answer\":\"Text\",\"cards\":[]}"}

            data: {"type":"response.completed","response":{}}

            data: [DONE]
        """.trimIndent()

        assertEquals("{\"title\":\"Test\",\"answer\":\"Text\",\"cards\":[]}", parseCodexSse(stream))
    }

    @Test
    fun codexSseFallsBackToCompletedResponse() {
        val stream = """
            data: {"type":"response.completed","response":{"output_text":"fertig"}}

            data: [DONE]
        """.trimIndent()

        assertEquals("fertig", parseCodexSse(stream))
    }

    @Test
    fun codexSsePrefersCompleteResponseOverPartialDeltas() {
        val stream = """
            data: {"type":"response.output_text.delta","delta":"teilweise"}

            data: {"type":"response.completed","response":{"output_text":"vollständig"}}
        """.trimIndent()

        assertEquals("vollständig", parseCodexSse(stream))
    }

    @Test
    fun researchPayloadRequiresWebSearchAndDynamicCardRange() {
        val payload = codexResearchPayload("gpt-test", "Hoch", "Erkläre Photosynthese")
        val schema = payload.getJSONObject("text").getJSONObject("format").getJSONObject("schema")
        val cards = schema.getJSONObject("properties").getJSONObject("cards")

        assertEquals("web_search", payload.getJSONArray("tools").getJSONObject(0).getString("type"))
        assertEquals("auto", payload.getString("tool_choice"))
        assertEquals("priority", payload.getString("service_tier"))
        assertEquals("high", payload.getJSONObject("reasoning").getString("effort"))
        assertEquals(30, cards.getInt("minItems"))
        assertEquals(70, cards.getInt("maxItems"))
        assertTrue(cards.getJSONObject("items").getJSONObject("properties").has("targetSessionIds"))
    }

    @Test
    fun researchInputIncludesEveryExistingSession() {
        val sessions = listOf(
            ExistingSessionContext("bio", "Biologie", "Was ist eine Zelle?", "Eine Zelle ist ..."),
            ExistingSessionContext("chemie", "Chemie", "Was ist ein Atom?", "Ein Atom ist ..."),
        )

        val input = researchInput("Wie arbeitet der Körper?", sessions)

        assertTrue(input.contains("\"id\":\"bio\""))
        assertTrue(input.contains("\"id\":\"chemie\""))
        assertTrue(input.contains("Was ist eine Zelle?"))
        assertTrue(input.contains("Was ist ein Atom?"))
    }

    @Test
    fun researchInstructionsRequireSimpleGermanAndForeignWordExplanations() {
        val instructions = researchInstructions()

        assertTrue(instructions.contains("Niveau der 10. Klasse"))
        assertTrue(instructions.contains("Fachwort oder Fremdwort"))
        assertTrue(instructions.contains("1.500 und 5.000 Wörtern"))
        assertTrue(instructions.contains("30 bis 70"))
    }

    @Test
    fun wordCountHandlesWhitespaceAndParagraphs() {
        assertEquals(5, researchWordCount(" Eins  zwei\n\ndrei\tvier fünf "))
        assertEquals(0, researchWordCount("   \n "))
    }

    @Test
    fun codexSseRejectsStreamsWithoutCompletion() {
        val stream = """data: {"type":"response.output_text.delta","delta":"teilweise"}"""

        assertThrows(CodexAuthException::class.java) { parseCodexSse(stream) }
    }

    @Test
    fun streamingAnswerDecoderHandlesSplitKeysEscapesAndUnicode() {
        val decoder = StreamingAnswerDecoder()
        val chunks = listOf(
            "{\"ans",
            "wer\":\"## Überschrift\\n\\nEin ",
            "Zitat: \\\"klar\\\" und Umlaut \\u00",
            "E4 sowie Emoji \\uD83D\\uDE00\",\"title\":\"Test\"}",
        )

        val answer = chunks.joinToString("") { decoder.accept(it) }

        assertEquals("## Überschrift\n\nEin Zitat: \"klar\" und Umlaut ä sowie Emoji 😀", answer)
        assertTrue(decoder.isComplete)
    }

    @Test
    fun streamingAnswerDecoderIgnoresNestedAndStringOccurrences() {
        val decoder = StreamingAnswerDecoder()
        val raw = "{\"meta\":{\"answer\":\"falsch\"},\"title\":\"answer\",\"answer\":\"richtig\",\"cards\":[]}"

        assertEquals("richtig", decoder.accept(raw))
        assertTrue(decoder.isComplete)
    }

    @Test
    fun answerEmitterPublishesFirstTextImmediatelyAndBatchesFollowingText() {
        var now = 1_000L
        val published = mutableListOf<String>()
        var completed = 0
        val emitter = ThrottledAnswerEmitter(published::add, { completed++ }) { now }

        emitter.accept("{\"answer\":\"Erstes")
        emitter.accept(" zweites")
        now += 100
        emitter.accept(" drittes\",\"title\":\"T\"}")

        assertEquals(listOf("Erstes", " zweites drittes"), published)
        assertEquals(1, completed)
    }

    @Test
    fun everyVisibleModelAndEffortBuildsTheExpectedPayload() {
        val efforts = mapOf("Niedrig" to "low", "Mittel" to "medium", "Hoch" to "high")

        val expectedModels = mapOf(
            "GPT 5.6 Sol" to "gpt-5.6-sol",
            "GPT 5.6 Terra" to "gpt-5.6-terra",
            "GPT 5.6 Luna" to "gpt-5.6-luna",
        )
        assertEquals(expectedModels, RESEARCH_MODELS)
        expectedModels.forEach { (label, modelId) ->
            efforts.forEach { (labelEffort, apiEffort) ->
                val payload = codexResearchPayload(codexModelId(label), labelEffort, "Test")
                assertEquals(modelId, payload.getString("model"))
                assertEquals(apiEffort, payload.getJSONObject("reasoning").getString("effort"))
                assertEquals("priority", payload.getString("service_tier"))
            }
        }
    }

    @Test
    fun completedOutputConcatenatesAllTextParts() {
        val response = org.json.JSONObject("""{"output":[{"content":[{"text":"Teil 1"},{"output_text":" und Teil 2"}]}]}""")

        assertEquals("Teil 1 und Teil 2", extractOutputText(response))
    }
}

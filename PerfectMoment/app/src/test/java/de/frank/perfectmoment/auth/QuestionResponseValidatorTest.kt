package de.frank.perfectmoment.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionResponseValidatorTest {
    @Test
    fun acceptsExactlyThirtyUniqueQuestions() {
        val raw = (1..30).map { "🌟 Wie fühlt sich Fortschritt $it an?" }

        val result = QuestionResponseValidator.validate(raw)

        assertEquals(30, result.size)
        assertEquals(CodexQuestion("🌟", "Wie fühlt sich Fortschritt 1 an?"), result.first())
    }

    @Test
    fun removesNormalizedDuplicatesAgainstPreviousAndCurrentQuestions() {
        val raw = (1..28).map { "🌟 Wie fühlt sich Fortschritt $it an?" } + listOf(
            "✨ BEREITS gestellt!!!",
            "🌟 Wie fühlt sich Fortschritt 1 an",
        )

        val result = QuestionResponseValidator.validate(raw, previousQuestions = listOf("Bereits gestellt?"))

        assertEquals(28, result.size)
    }

    @Test
    fun rejectsFewerThanTwentyValidQuestions() {
        val raw = (1..19).map { "🌟 Eigenständige Frage $it?" } + List(11) { "✨" }

        assertThrows(QuestionValidationException::class.java) {
            QuestionResponseValidator.validate(raw)
        }
    }

    @Test
    fun separatesEmojiSequencesAndUsesFallbackWithoutEmoji() {
        val raw = listOf(
            "👨‍👩‍👧‍👦 Was trägt deine Familie bei?",
            "👩🏽‍⚕️ Was stärkt heute deine Gesundheit?",
            "🇩🇪 Was bedeutet Heimat für dich?",
            "Welche Ruhe spürst du bereits?",
        ) + (5..30).map { "🌲 Welche neue Perspektive zeigt Frage $it?" }

        val result = QuestionResponseValidator.validate(raw)

        assertEquals(CodexQuestion("👨‍👩‍👧‍👦", "Was trägt deine Familie bei?"), result[0])
        assertEquals(CodexQuestion("👩🏽‍⚕️", "Was stärkt heute deine Gesundheit?"), result[1])
        assertEquals(CodexQuestion("🇩🇪", "Was bedeutet Heimat für dich?"), result[2])
        assertEquals(CodexQuestion("✨", "Welche Ruhe spürst du bereits?"), result[3])
    }

    @Test
    fun removesEntranceQuestionFromGeneratedQuestions() {
        val raw = listOf("🎯 ${IntroQuestionPolicy.QUESTION}") +
            (1..29).map { "🌟 Eigenständige Frage $it?" }

        val result = QuestionResponseValidator.validate(raw)

        assertEquals(29, result.size)
        assertEquals(false, result.any { IntroQuestionPolicy.isEntranceQuestion(it.text) })
    }

    @Test
    fun entrancePromptUsesAnswerAsSessionTopic() {
        val prompt = IntroQuestionPolicy.resolve(
            topic = "Frage mich immer zuerst: „${IntroQuestionPolicy.QUESTION}“ Danach beginne.",
            introContext = "Wie finde ich mehr innere Ruhe? ",
            answerRequired = true,
        )

        assertEquals("Wie finde ich mehr innere Ruhe?", prompt.topic)
        assertEquals("", prompt.introContext)
    }

    @Test
    fun sessionPromptPayloadRequiresSemanticClassification() {
        val payload = codexSessionPromptPayload(
            "Welches Gefühl möchtest du stärker wahrnehmen? Danach beginnt die Routine.",
            CodexModel.TERRA,
            ReasoningEffort.MEDIUM,
        )

        assertTrue(payload.getString("instructions").contains("semantisch"))
        assertTrue(payload.getJSONArray("input").getJSONObject(0).getString("content").contains("Gefühl"))
        val properties = payload.getJSONObject("text").getJSONObject("format")
            .getJSONObject("schema").getJSONObject("properties")
        assertEquals("boolean", properties.getJSONObject("requires_answer").getString("type"))
        assertEquals("string", properties.getJSONObject("question").getString("type"))
    }

    @Test
    fun parsesDynamicSessionPromptDecision() {
        val decision = parseSessionPromptDecision(
            """{"requires_answer":true,"question":"„Welchen Wunsch oder welches Ziel hast du?“"}""",
        )

        assertEquals(true, decision.requiresAnswer)
        assertEquals("Welchen Wunsch oder welches Ziel hast du?", decision.question)
    }

    @Test
    fun regularTopicKeepsIntroAsAdditionalContext() {
        val prompt = IntroQuestionPolicy.resolve(
            topic = "Wie fühlt sich ein schönes Leben an?",
            introContext = "Mit mehr Gelassenheit",
            answerRequired = false,
        )

        assertEquals("Wie fühlt sich ein schönes Leben an?", prompt.topic)
        assertEquals("Mit mehr Gelassenheit", prompt.introContext)
    }

    @Test
    fun payloadExplicitlyForbidsEntranceQuestion() {
        val payload = codexQuestionsPayload(
            CodexQuestionRequest(
                topic = "Mein Thema",
                skillText = "Skill",
                operatingModeText = "Erzeuge Fragen",
            ),
        )

        val instructions = payload.getString("instructions")
        assertTrue(instructions.contains(IntroQuestionPolicy.QUESTION))
        assertTrue(instructions.contains("niemals als erzeugte Frage erscheinen"))
        assertTrue(instructions.contains("Du-Person"))
    }

    @Test
    fun firstPersonOverridesConflictingOperatingMode() {
        val payload = codexQuestionsPayload(
            CodexQuestionRequest(
                topic = "Mein Thema",
                skillText = "Skill",
                operatingModeText = "Sprich den Hörer mit du an.",
                perspective = QuestionPerspective.FIRST_PERSON,
            ),
        )

        val instructions = payload.getString("instructions")
        assertTrue(instructions.contains("Vorrang"))
        assertTrue(instructions.contains("Ich-Person"))
        assertTrue(instructions.contains("Warum genieße ich"))
    }

    @Test
    fun perspectiveRewriteKeepsQuestionCountAndOrder() {
        val raw = """{"fragen":["Warum genieße ich den Moment?","Was stärkt mich?"]}"""

        val result = parsePerspectiveQuestions(raw, expectedCount = 2)

        assertEquals(listOf("Warum genieße ich den Moment?", "Was stärkt mich?"), result)
    }

    @Test
    fun removesDynamicallyDetectedEntranceQuestion() {
        val entranceQuestion = "Welches Gefühl möchtest du stärker wahrnehmen?"
        val raw = listOf("🎯 $entranceQuestion") +
            (1..29).map { "🌟 Eigenständige Frage $it?" }

        val result = QuestionResponseValidator.validate(raw, excludedQuestions = listOf(entranceQuestion))

        assertEquals(29, result.size)
        assertEquals(false, result.any { it.text == entranceQuestion })
    }
}

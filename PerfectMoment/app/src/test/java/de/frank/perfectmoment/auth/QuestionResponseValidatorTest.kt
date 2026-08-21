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
    fun neutralPersonUsesManForm() {
        val payload = codexQuestionsPayload(
            CodexQuestionRequest(
                topic = "Mein Thema",
                skillText = "Skill",
                operatingModeText = "Sprich den Hörer mit du an.",
                perspective = QuestionPerspective.NEUTRAL_PERSON,
            ),
        )

        val instructions = payload.getString("instructions")
        assertTrue(instructions.contains("Vorrang"))
        assertTrue(instructions.contains("Man-Person"))
        assertTrue(instructions.contains("Warum genießt man"))
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

    /** Der Klumpen stammt wortgetreu aus einer echten Sitzung (07.08.2026). */
    @Test
    fun zusammengeklumpterBlockWirdInEinzelneFragenZerlegt() {
        val block = "🌟 Welchen Wert hat die Ruhe, mit der ich meine Geschichte betrachte? " +
            "Hi, I'm here to help with your questions. What would you like to know? " +
            "👣 Wieso bewegt sich mein Körper frei durch den ganzen Raum? " +
            "🪟 Was fällt mir gerade auf, weil ich diesen Moment wach erlebe?"

        val result = QuestionResponseValidator.splitIntoQuestions(block)

        assertEquals(
            listOf(
                "Welchen Wert hat die Ruhe, mit der ich meine Geschichte betrachte?",
                "Wieso bewegt sich mein Körper frei durch den ganzen Raum?",
                "Was fällt mir gerade auf, weil ich diesen Moment wach erlebe?",
            ),
            result.map { it.text },
        )
        assertEquals(listOf("🌟", "👣", "🪟"), result.map { it.emoji })
    }

    @Test
    fun einzelneFrageBleibtUnveraendert() {
        val result = QuestionResponseValidator.splitIntoQuestions("✨ Warum genieße ich diesen Moment?")

        assertEquals(1, result.size)
        assertEquals("Warum genieße ich diesen Moment?", result.single().text)
        assertEquals("✨", result.single().emoji)
    }

    @Test
    fun eintragOhneFrageWirdVerworfen() {
        assertTrue(QuestionResponseValidator.splitIntoQuestions("✨ Hier fehlt das Fragezeichen").isEmpty())
        assertTrue(QuestionResponseValidator.splitIntoQuestions("   ").isEmpty())
        assertTrue(QuestionResponseValidator.splitIntoQuestions("✨ Kurz?").isEmpty())
    }

    @Test
    fun ueberlangerEintragOhneTrennstelleWirdVerworfen() {
        val tooLong = "✨ " + "Warum ist dieser Satz so unendlich lang, ".repeat(20) + "warum nur?"

        assertTrue(QuestionResponseValidator.splitIntoQuestions(tooLong).isEmpty())
    }

    @Test
    fun zerlegterBlockZaehltAlsMehrereGueltigeFragen() {
        val raw = (1..29).map { "🌟 Eigenständige Frage $it?" } +
            "✨ Erste Klumpenfrage hier? 🔥 Zweite Klumpenfrage hier?"

        val result = QuestionResponseValidator.validate(raw)

        assertEquals(31, result.size)
        assertTrue(result.any { it.text == "Erste Klumpenfrage hier?" })
        assertTrue(result.any { it.text == "Zweite Klumpenfrage hier?" })
    }
}

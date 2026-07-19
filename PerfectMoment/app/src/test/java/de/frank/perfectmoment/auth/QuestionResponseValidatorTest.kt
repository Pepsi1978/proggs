package de.frank.perfectmoment.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
}

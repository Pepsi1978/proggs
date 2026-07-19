package de.frank.perfectmoment.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamingQuestionDecoderTest {
    @Test
    fun emitsQuestionAsSoonAsItsJsonStringIsComplete() {
        val decoder = StreamingQuestionDecoder()

        assertEquals(emptyList<String>(), decoder.append("{\"fra"))
        assertEquals(emptyList<String>(), decoder.append("gen\":[\"🌟 Erste Fra"))
        assertEquals(listOf("🌟 Erste Frage?"), decoder.append("ge?\","))
        assertEquals(listOf("✨ Zweite Frage?"), decoder.append("\"✨ Zweite Frage?\"]}"))
    }

    @Test
    fun decodesEscapedQuotesAndUnicode() {
        val decoder = StreamingQuestionDecoder()

        val questions = decoder.append("{\"fragen\":[\"✨ Was bedeutet \\\"Ruhe\\\"?\",\"🌲 Wie klingt \\u00dcbermut?\"]}")

        assertEquals(listOf("✨ Was bedeutet \"Ruhe\"?", "🌲 Wie klingt Übermut?"), questions)
    }

    @Test
    fun doesNotEmitCompletedQuestionTwice() {
        val decoder = StreamingQuestionDecoder()

        assertEquals(listOf("✨ Erste?"), decoder.append("{\"fragen\":[\"✨ Erste?\""))
        assertEquals(emptyList<String>(), decoder.append(","))
        assertEquals(listOf("🌲 Zweite?"), decoder.append("\"🌲 Zweite?\"]}"))
    }
}

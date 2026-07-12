package de.frank.cortex.ui.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ChatSpeechSanitizerTest {
    @Test
    fun removesUrlsCitationsAndTrailingSourcesWithoutDroppingAnswer() {
        val input = """
            Archer wird in Strange New Worlds erwähnt [1]. Mehr steht bei https://example.com/a/b?x=1.

            Quellen:
            - [Memory Alpha](https://memory-alpha.example/archer)
            - www.example.org/source
        """.trimIndent()

        val cleaned = ChatSpeechSanitizer.clean(input)

        assertEquals("Archer wird in Strange New Worlds erwähnt. Mehr steht bei.", cleaned)
        assertFalse(cleaned.contains("http", ignoreCase = true))
        assertFalse(cleaned.contains("Quelle", ignoreCase = true))
    }

    @Test
    fun preservesNormalAnswerTextAndRemovesOnlyMarkdownFormatting() {
        assertEquals(
            "Jonathan Archer kehrt nicht als gespielte Figur zurück.",
            ChatSpeechSanitizer.clean("**Jonathan Archer** kehrt nicht als gespielte Figur zurück."),
        )
    }

    @Test
    fun removesTrailingSourceLinksWithoutSourcesHeading() {
        val input = """
            Die gezielte Pflege weniger Hinweise ist entscheidender als ihre bloße Menge.

            [OpenAI Codex](https://openai.com/codex)
            [Terminal Bench](https://terminal-bench.example/results)
        """.trimIndent()

        assertEquals(
            "Die gezielte Pflege weniger Hinweise ist entscheidender als ihre bloße Menge.",
            ChatSpeechSanitizer.clean(input),
        )
    }
}

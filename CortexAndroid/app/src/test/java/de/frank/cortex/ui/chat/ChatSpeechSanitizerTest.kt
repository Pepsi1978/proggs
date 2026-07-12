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

    @Test
    fun removesNaturalLanguageSourceAttributionsFromProductionReplyPattern() {
        val input = """
            OpenAI nennt ungefähr hundert Zeilen als grobe Obergrenze. Quelle OpenAI Harness Engineering.

            Die Schlussfolgerung ist, Kontextdateien minimal zu halten. Quelle arXiv Studie 2602 Punkt 11988.

            Ein kurzer Hinweis kann teure Irrwege verhindern. Quelle: Anthropic Dokumentation.

            CLAUDE.md kann AGENTS.md importieren (Quelle Claude Code Memory Dokumentation).
        """.trimIndent()

        assertEquals(
            """
                OpenAI nennt ungefähr hundert Zeilen als grobe Obergrenze.

                Die Schlussfolgerung ist, Kontextdateien minimal zu halten.

                Ein kurzer Hinweis kann teure Irrwege verhindern.

                CLAUDE.md kann AGENTS.md importieren.
            """.trimIndent(),
            ChatSpeechSanitizer.clean(input),
        )
    }

    @Test
    fun preservesNormalSentenceAboutAConceptualSource() {
        assertEquals(
            "Die Quelle ist für die Bewertung wichtig.",
            ChatSpeechSanitizer.clean("Die Quelle ist für die Bewertung wichtig."),
        )
    }
}

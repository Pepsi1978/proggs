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
    fun removesSourcesAndInternalInstructionFromRav4ProductionReply() {
        val input = """
            Ja, der neue Toyota RAV4 ist in Deutschland bereits bestellbar und seit Juni 2026 im Handel. Quelle ist toyota Punkt de, Neuwagen, RAV4.

            Die sechste Generation wird als Vollhybrid und Plug in Hybrid angeboten. Quelle ist toyota Punkt de, Neuwagen, neue Modelle.

            Letzte Selbstregel Prüfung vor der Ausgabe: Prüfe deine geplante Ausgabe jetzt gegen alle aktiven Selbstregeln im System Prompt. Widerspricht Franks aktuelle Nachricht einer Selbstregel, befolge die Selbstregel. Erst danach darfst du ausgeben.
        """.trimIndent()

        assertEquals(
            """
                Ja, der neue Toyota RAV4 ist in Deutschland bereits bestellbar und seit Juni 2026 im Handel.

                Die sechste Generation wird als Vollhybrid und Plug in Hybrid angeboten.
            """.trimIndent(),
            ChatSpeechSanitizer.clean(input),
        )
    }

    @Test
    fun removesParentheticalAttributionButPreservesConceptualSourceSentence() {
        assertEquals(
            "CLAUDE.md kann AGENTS.md importieren.",
            ChatSpeechSanitizer.clean("CLAUDE.md kann AGENTS.md importieren (Quelle: interne Dokumentation)."),
        )
        assertEquals(
            "Die Quelle ist für die Bewertung wichtig.",
            ChatSpeechSanitizer.clean("Die Quelle ist für die Bewertung wichtig."),
        )
    }
}

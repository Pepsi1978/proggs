package de.frank.karteikartenlernen.text

import org.junit.Assert.assertEquals
import org.junit.Test

class ResearchArticleParserTest {
    @Test
    fun headingsParagraphsAndSourcesRemainCompleteAndOrdered() {
        val article = """
            ## Erster Teil

            Ein verständlicher Absatz mit allen wichtigen Angaben.

            ## Quellen

            - Beispielquelle: https://example.org/wissen
        """.trimIndent()

        assertEquals(
            listOf(
                ArticleBlock.Heading(2, "Erster Teil"),
                ArticleBlock.Paragraph("Ein verständlicher Absatz mit allen wichtigen Angaben."),
                ArticleBlock.Heading(2, "Quellen"),
                ArticleBlock.Source("Beispielquelle", "https://example.org/wissen"),
            ),
            parseResearchArticle(article),
        )
    }

    @Test
    fun oldPlainTextIsKeptAsParagraphs() {
        assertEquals(
            listOf(ArticleBlock.Paragraph("Absatz eins."), ArticleBlock.Paragraph("Absatz zwei.")),
            parseResearchArticle("Absatz eins.\nAbsatz zwei."),
        )
    }

    @Test
    fun blankArticleHasNoBlocks() {
        assertEquals(emptyList<ArticleBlock>(), parseResearchArticle(" \n "))
    }

    @Test
    fun speechTextEndsBeforeSources() {
        val answer = """
            ## Erklärung

            Nur dieser Inhalt soll vorgelesen werden.

            ## Quellenangaben

            - Beispiel: https://example.org
        """.trimIndent()

        assertEquals(
            "## Erklärung\n\nNur dieser Inhalt soll vorgelesen werden.",
            researchAnswerForSpeech(answer),
        )
    }

    @Test
    fun speechTextWithoutSourcesRemainsUnchanged() {
        assertEquals("Die vollständige Antwort.", researchAnswerForSpeech("  Die vollständige Antwort.  "))
    }

    @Test
    fun inlineSourcesAreRemovedAndOnlyAnswerTextRemains() {
        val answer = """
            ## Erklärung

            Pflanzen wandeln Licht in Energie um.【turn0search0†L1-L5】 Das nennt man [Photosynthese](https://example.org/photosynthese). [1]

            Ein weiterer Satz (Quelle: Beispiel-Institut, 2026) mit Link https://example.org/studie.

            ## Quellen

            - Beispiel: https://example.org
        """.trimIndent()

        assertEquals(
            "## Erklärung\n\nPflanzen wandeln Licht in Energie um. Das nennt man Photosynthese.\n\nEin weiterer Satz mit Link",
            sanitizeResearchAnswer(answer),
        )
        assertEquals(sanitizeResearchAnswer(answer), researchAnswerForSpeech(answer))
    }

    @Test
    fun streamingSpeechUsesOnlyCompletedParagraphs() {
        val streaming = "## Eins\n\nErster Absatz ist fertig.\n\n## Zwei\n\nZweiter Absatz ist noch nicht fertig"

        assertEquals(
            listOf("Eins. Erster Absatz ist fertig."),
            completedResearchSpeechSegments(streaming, final = false),
        )
        assertEquals(
            listOf("Eins. Erster Absatz ist fertig.", "Zwei. Zweiter Absatz ist noch nicht fertig"),
            completedResearchSpeechSegments(streaming, final = true),
        )
    }

    @Test
    fun streamingSpeechRepeatsHeadingForEveryCompletedParagraph() {
        val answer = "## Thema\n\nErster Absatz.\n\nZweiter Absatz.\n\nDritter Absatz läuft"

        assertEquals(
            listOf("Thema. Erster Absatz.", "Thema. Zweiter Absatz."),
            completedResearchSpeechSegments(answer, final = false),
        )
    }
}

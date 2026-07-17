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
}

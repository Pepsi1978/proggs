package de.frank.karteikartenlernen.text

sealed interface ArticleBlock {
    data class Heading(val level: Int, val text: String) : ArticleBlock
    data class Paragraph(val text: String) : ArticleBlock
    data class Source(val title: String, val url: String) : ArticleBlock
}

fun parseResearchArticle(text: String): List<ArticleBlock> {
    if (text.isBlank()) return emptyList()
    val blocks = mutableListOf<ArticleBlock>()
    text.replace("\r\n", "\n")
        .replace('\r', '\n')
        .trim()
        .split(Regex("\n\\s*\n"))
        .forEach { rawChunk ->
            val lines = rawChunk.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
            if (lines.isEmpty()) return@forEach
            val heading = HEADING.matchEntire(lines.first())
            if (heading != null) {
                blocks += ArticleBlock.Heading(
                    level = heading.groupValues[1].length.coerceIn(1, 3),
                    text = heading.groupValues[2].trim(),
                )
                appendArticleLines(blocks, lines.drop(1))
            } else {
                appendArticleLines(blocks, lines)
            }
        }
    return blocks
}

fun researchAnswerForSpeech(answer: String): String {
    val sourceHeading = SOURCE_HEADING.find(answer) ?: return answer.trim()
    return answer.substring(0, sourceHeading.range.first).trim()
}

private fun appendArticleLines(blocks: MutableList<ArticleBlock>, lines: List<String>) {
    lines.forEach { line ->
        val source = SOURCE.matchEntire(line)
        if (source != null) {
            blocks += ArticleBlock.Source(source.groupValues[1].trim(), source.groupValues[2])
        } else {
            blocks += ArticleBlock.Paragraph(line)
        }
    }
}

private val HEADING = Regex("^(#{1,3})\\s+(.+)$")
private val SOURCE = Regex("^-\\s+(.+?):\\s+(https?://\\S+)$")
private val SOURCE_HEADING = Regex(
    pattern = "^(?:#{1,3}\\s*)?(?:Quellen|Quellenangaben|Sources)\\s*:?\\s*$",
    options = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
)

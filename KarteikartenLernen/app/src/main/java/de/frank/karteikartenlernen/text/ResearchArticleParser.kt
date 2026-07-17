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
    return sanitizeResearchAnswer(answer)
}

fun sanitizeResearchAnswer(answer: String): String {
    val sourceHeading = SOURCE_HEADING.find(answer)
    val answerOnly = if (sourceHeading == null) answer else answer.substring(0, sourceHeading.range.first)
    return answerOnly
        .replace(OPENAI_CITATION, "")
        .replace(CHATGPT_CITATION, "")
        .replace(MARKDOWN_LINK) { match -> match.groupValues[1] }
        .replace(PARENTHETICAL_SOURCE, "")
        .replace(BRACKETED_SOURCE, "")
        .replace(NUMERIC_CITATION, "")
        .replace(BARE_URL, "")
        .lineSequence()
        .map { line -> line.replace(MULTIPLE_SPACES, " ").trimEnd() }
        .filterNot { line -> SOURCE_ONLY_LINE.matches(line.trim()) }
        .joinToString("\n")
        .replace(SPACE_BEFORE_PUNCTUATION, "$1")
        .replace(EXCESS_BLANK_LINES, "\n\n")
        .trim()
}

fun completedResearchSpeechSegments(answer: String, final: Boolean): List<String> {
    val availableText = if (final) {
        answer
    } else {
        val normalized = answer.replace("\r\n", "\n").replace('\r', '\n')
        val boundary = normalized.lastIndexOf("\n\n")
        if (boundary < 0) return emptyList() else normalized.substring(0, boundary + 2)
    }
    val segments = mutableListOf<String>()
    var heading: String? = null
    parseResearchArticle(sanitizeResearchAnswer(availableText)).forEach { block ->
        when (block) {
            is ArticleBlock.Heading -> heading = block.text
            is ArticleBlock.Paragraph -> {
                segments += heading?.let { "$it. ${block.text}" } ?: block.text
                heading = null
            }
            is ArticleBlock.Source -> Unit
        }
    }
    return segments
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
private val OPENAI_CITATION = Regex("【[^】]*】")
private val CHATGPT_CITATION = Regex("cite[^]*")
private val MARKDOWN_LINK = Regex("\\[([^]]+)]\\(https?://[^)]+\\)", RegexOption.IGNORE_CASE)
private val PARENTHETICAL_SOURCE = Regex("\\((?:Quelle|Quellen|Source|Sources|vgl\\.)\\s*:?[^)]*\\)", RegexOption.IGNORE_CASE)
private val BRACKETED_SOURCE = Regex("\\[(?:Quelle|Quellen|Source|Sources)\\s*:?[^]]*]", RegexOption.IGNORE_CASE)
private val NUMERIC_CITATION = Regex("\\[(?:\\d+[a-z]?)(?:\\s*[,;–-]\\s*\\d+[a-z]?)*]")
private val BARE_URL = Regex("https?://\\S+|www\\.\\S+", RegexOption.IGNORE_CASE)
private val SOURCE_ONLY_LINE = Regex("^(?:Quelle|Quellen|Source|Sources|vgl\\.)\\s*:?\\s*.*$", RegexOption.IGNORE_CASE)
private val SPACE_BEFORE_PUNCTUATION = Regex("[ \\t]+([,.;:!?])")
private val MULTIPLE_SPACES = Regex("[ \\t]{2,}")
private val EXCESS_BLANK_LINES = Regex("\\n{3,}")

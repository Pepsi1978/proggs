package de.frank.cortex.ui.chat

internal object ChatSpeechSanitizer {
    private val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
    private val headingRegex = Regex("(?m)^#{1,6}\\s*")
    private val bulletRegex = Regex("(?m)^[-*•]\\s+")
    private val markdownWebLinkRegex = Regex(
        "\\[([^]]+)]\\((?:https?://|www\\.)[^\\s)]+(?:\\s+\"[^\"]*\")?\\)",
        RegexOption.IGNORE_CASE,
    )
    private val bareWebUrlRegex = Regex("\\b(?:https?://|www\\.)[^\\s<>()]+", RegexOption.IGNORE_CASE)
    private val trailingSourcesRegex = Regex(
        "^\\s*(?:quellen?|sources?|references?|weiterf(?:ü|ue)hrende\\s+links?)\\s*:?.*$",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL),
    )
    private val trailingMarkdownLinksRegex = Regex(
        "(?:\\n\\s*)+(?:(?:[-*•]\\s*)?\\[[^]\\n]+]\\((?:https?://|www\\.)[^)\\n]+\\)\\s*)+$",
        RegexOption.IGNORE_CASE,
    )
    private val numericCitationRegex = Regex("\\[(?:\\d+(?:\\s*[-,]\\s*\\d+)*)]")

    fun clean(text: String): String = text
        .replace(trailingSourcesRegex, "")
        .replace(trailingMarkdownLinksRegex, "")
        .replace(markdownWebLinkRegex, "$1")
        .replace(bareWebUrlRegex) { match ->
            match.value.lastOrNull()?.takeIf { it in ".,;:!?" }?.toString().orEmpty()
        }
        .replace(numericCitationRegex, "")
        .replace(boldRegex, "$1")
        .replace("**", "")
        .replace(headingRegex, "")
        .replace(bulletRegex, "")
        .replace(Regex("[ \\t]+([,.;:!?])"), "$1")
        .replace(Regex("\\n[ \\t]+"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

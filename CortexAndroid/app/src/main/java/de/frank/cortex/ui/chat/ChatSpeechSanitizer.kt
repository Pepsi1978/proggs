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
    private val bareDomainRegex = Regex(
        "\\b(?:[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])?\\.)+(?:de|com|org|net|eu|info|io|ai|co\\.uk)\\b[^\\s<>()]*",
        RegexOption.IGNORE_CASE,
    )
    private val trailingSourcesRegex = Regex(
        "^\\s*(?:quellen?|sources?|references?|weiterf(?:ü|ue)hrende\\s+links?)\\s*:?.*$",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL),
    )
    private val sourceAttributionTailRegex = Regex(
        "(?:^|(?<=[.!?]))[ \\t]*(?:[-–—]\\s*)?" +
            "(?:quelle(?:n)?|sources?|references?)\\s*(?::|ist|sind|war|waren)?\\s+[^\\n]*",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )
    private val articleSourceAttributionRegex = Regex(
        "(?:^|(?<=[.!?]))[ \\t]*(?:die\\s+)?quelle\\s*(?::|ist|war)\\s+" +
            "(?:https?://|www\\.)?" +
            "(?:[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])?\\.)+(?:de|com|org|net|eu|info|io|ai|co\\.uk)\\b[^\\n]*",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )
    private val parentheticalSourceRegex = Regex(
        "\\s*\\((?:quelle(?:n)?|sources?|references?)\\s*(?::|ist|sind|war|waren)?\\s+[^)\\n]+\\)",
        RegexOption.IGNORE_CASE,
    )
    private val internalOutputInstructionRegex = Regex(
        "(?:^|\\n)\\s*(?:letzte|abschlie(?:ß|ss)ende|finale)\\s+" +
            "(?:selbstregel|regel)[ -]?(?:prüfung|pruefung|check)\\b[^\\n]*" +
            "|(?:^|\\n)\\s*prüfe deine geplante ausgabe jetzt gegen alle aktiven selbstregeln[^\\n]*",
        RegexOption.IGNORE_CASE,
    )
    private val numericCitationRegex = Regex("\\[(?:\\d+(?:\\s*[-,]\\s*\\d+)*)]")

    fun clean(text: String): String = text
        .replace(internalOutputInstructionRegex, "")
        .replace(trailingSourcesRegex, "")
        .replace(articleSourceAttributionRegex, "")
        .replace(markdownWebLinkRegex, "$1")
        .replace(bareWebUrlRegex) { match ->
            match.value.lastOrNull()?.takeIf { it in ".,;:!?" }?.toString().orEmpty()
        }
        .replace(parentheticalSourceRegex, "")
        .replace(sourceAttributionTailRegex, "")
        .replace(bareDomainRegex) { match ->
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

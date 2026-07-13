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
    // Quellen-BLOCK (Überschrift-Zeile wie "Quellen:" ohne weiteren Inhalt) bis zum Textende
    // entfernen. Frueher matchte hier auch "Quellensteuer..." (keine Wortgrenze!) und loeschte
    // per DOT_MATCHES_ALL die KOMPLETTE Antwort — jetzt greift der Block-Loescher nur bei einer
    // reinen Ueberschrift-Zeile, eine Inline-Zeile ("Quellen: a, b") loescht nur sich selbst.
    private val trailingSourcesBlockRegex = Regex(
        "(?:^|\\n)\\s*(?:quellen?|sources?|references?|weiterf(?:ü|ue)hrende\\s+links?)\\s*:?\\s*(?:\\n|$)(?s).*\\z",
        RegexOption.IGNORE_CASE,
    )
    private val inlineSourcesLineRegex = Regex(
        "^[ \\t]*(?:quellen?|sources?|references?|weiterf(?:ü|ue)hrende\\s+links?)\\s*:[^\\n]*$",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )
    private val sourceAttributionTailRegex = Regex(
        // Nur ":"-Formen ODER die SINGULAR-Kopula "Quelle ist/war ..." (typische gesprochene
        // Attribution wie "Quelle ist toyota Punkt de"). Die Plural-Kopula ist bewusst raus:
        // "Quellen sind Materialien, aus denen ..." ist ein normaler Inhaltssatz und wurde
        // sonst mitsamt Kernaussage aus Anzeige, Speicherung und Vorlesen geloescht.
        "(?:^|(?<=[.!?]))[ \\t]*(?:[-–—]\\s*)?" +
            "(?:(?:quelle(?:n)?|sources?|references?)\\s*:|(?:quelle|source)\\s+(?:ist|war|is|was))\\s+[^\\n]*",
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
        .replace(trailingSourcesBlockRegex, "")
        .replace(inlineSourcesLineRegex, "")
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

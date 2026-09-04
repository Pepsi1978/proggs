package de.frank.codexkompass.update

data class GelesenerEintrag(
    val name: String,
    val beschreibung: String,
    val art: String,
    val kategorie: String = "",
)

/** Liest nur die offizielle CLI-Tabelle. Namen und Aliasse kommen niemals vom Modell. */
object DokuParser {
    private val slash = Regex("`(/[a-z][a-z0-9-]*)`")
    val changelogNamen = setOf("/cd", "/pwd", "/cwd", "/export", "/recap")

    fun leseSlashBefehle(markdown: String): List<GelesenerEintrag> {
        val abschnitt = markdown.substringAfter("## Built-in slash commands", "")
            .substringBefore("\n## ")
        if (abschnitt.isBlank()) return emptyList()
        val ergebnis = linkedMapOf<String, GelesenerEintrag>()
        var nameSpalte = -1
        var textSpalte = -1
        for (zeile in abschnitt.lineSequence()) {
            if (!zeile.trim().startsWith("|")) continue
            val spalten = zeile.trim().trim('|').split(Regex("(?<!\\\\)\\|"))
                .map { it.trim() }
            if ("Command" in spalten && "Purpose" in spalten) {
                nameSpalte = spalten.indexOf("Command")
                textSpalte = spalten.indexOf("Purpose")
                continue
            }
            if (nameSpalte < 0 || spalten.size <= maxOf(nameSpalte, textSpalte)) continue
            val namen = slash.findAll(spalten[nameSpalte]).map { it.groupValues[1] }.toList()
            val beschreibung = spalten[textSpalte]
            if (beschreibung.isBlank()) continue
            for ((index, name) in namen.withIndex()) {
                ergebnis.putIfAbsent(name, GelesenerEintrag(name, beschreibung,
                    if (index == 0) "Eingebaut" else "Alias von ${namen.first()}"))
            }
        }
        // Dieser Alias steht im Erklärungsteil statt in der Tabelle.
        if (markdown.contains("`/clean`") && ergebnis.containsKey("/stop")) {
            ergebnis["/clean"] = ergebnis.getValue("/stop").copy(name = "/clean", art = "Alias von /stop")
        }
        return ergebnis.values.toList()
    }

    /** Die Release-Seite liefert HTML; die CLI-Version bleibt unabhängig von App-Releases. */
    fun leseNeuesteVersion(changelog: String): String =
        Regex("Codex CLI\\s*(?:<[^>]+>\\s*)*([0-9]+\\.[0-9]+\\.[0-9]+)")
            .find(changelog)?.groupValues?.get(1).orEmpty()

    /** Dokumentierte Ergänzungen, solange sie noch nicht in der Übersicht stehen. */
    fun ergaenzeAusChangelog(changelog: String): List<GelesenerEintrag> {
        val beschreibungen = mapOf(
            "/cd" to "Change the working directory in the TUI session.",
            "/pwd" to "Show the working directory in the TUI session.",
            "/cwd" to "Manage the working directory in the TUI session.",
            "/export" to "Export the conversation as Markdown to the clipboard or a file.",
            "/recap" to "Request a manual recap of the current conversation.",
        )
        return beschreibungen.filterKeys { name ->
            Regex("<code[^>]*>" + Regex.escape(name) + "(?:\\s|<)").containsMatchIn(changelog)
        }.map { (name, text) -> GelesenerEintrag(name, text, "Eingebaut", "Sitzung") }
    }

    /** Keine erfundene Einführungsfassung aus einer beliebigen Erwähnung ableiten. */
    @Suppress("UNUSED_PARAMETER")
    fun findeEinzug(changelog: String, name: String, istSlash: Boolean): Pair<String, String> =
        "" to ""
}

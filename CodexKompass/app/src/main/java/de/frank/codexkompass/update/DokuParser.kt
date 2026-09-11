package de.frank.codexkompass.update

import org.json.JSONArray

data class GelesenerEintrag(
    val name: String,
    val beschreibung: String,
    val art: String,
    val kategorie: String = "",
)

/** Eine stabile Codex-CLI-Fassung aus den GitHub-Releases, neueste zuerst. */
data class ReleaseNotiz(val version: String, val text: String)

/** Liest nur die offizielle CLI-Tabelle. Namen und Aliasse kommen niemals vom Modell. */
object DokuParser {
    private val slash = Regex("`(/[a-z][a-z0-9-]*)`")
    private val stabileFassung = Regex("rust-v([0-9]+\\.[0-9]+\\.[0-9]+)")

    /** Pfade, die in den Release-Notes in Backticks stehen, aber keine Befehle sind. */
    private val keineBefehle = setOf(
        "/dev", "/tmp", "/etc", "/usr", "/bin", "/sbin", "/home", "/var", "/proc", "/sys",
        "/opt", "/mnt", "/root", "/lib", "/private", "/workspace", "/users", "/volumes",
    )

    /** Befehle, die nur im Änderungsprotokoll stehen und nie als entfernt gelten sollen. */
    val changelogNamen = setOf("/cd", "/pwd", "/cwd", "/export", "/recap", "/worktree")

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

    /**
     * Liest die stabilen CLI-Fassungen aus der GitHub-Releases-API.
     *
     * Die Changelog-Seite auf learn.chatgpt.com führt seit Herbst 2026 keine CLI-Versionen mehr;
     * neue Slash-Befehle stehen zuerst in den Release-Notes und oft erst Wochen später in der
     * Befehlstabelle der Doku. Alpha-Fassungen werden übergangen.
     */
    fun leseReleases(vararg seiten: String): List<ReleaseNotiz> = seiten.flatMap { json ->
        val feld = runCatching { JSONArray(json) }.getOrNull() ?: return@flatMap emptyList()
        (0 until feld.length()).mapNotNull { index ->
            val release = feld.optJSONObject(index) ?: return@mapNotNull null
            if (release.optBoolean("prerelease") || release.optBoolean("draft")) return@mapNotNull null
            val version = stabileFassung.matchEntire(release.optString("tag_name"))
                ?.groupValues?.get(1) ?: return@mapNotNull null
            ReleaseNotiz(version, release.optString("body"))
        }
    }.distinctBy { it.version }

    fun leseNeuesteVersion(releases: List<ReleaseNotiz>): String =
        releases.firstOrNull()?.version.orEmpty()

    /**
     * Slash-Befehle, die in den Release-Notes vorkommen, aber (noch) nicht in der Doku-Tabelle.
     *
     * Beschreibung ist die Zeile der neuesten Fassung, die den Befehl nennt — bevorzugt eine
     * Hervorhebung, nicht der nackte PR-Titel.
     */
    fun ergaenzeAusReleases(releases: List<ReleaseNotiz>): List<GelesenerEintrag> {
        // Handgeschriebene Texte haben Vorrang vor einer rohen Release-Zeile.
        val festeTexte = mapOf(
            "/cd" to "Change the working directory in the TUI session.",
            "/pwd" to "Show the working directory in the TUI session.",
            "/cwd" to "Manage the working directory in the TUI session.",
            "/export" to "Export the conversation as Markdown to the clipboard or a file.",
            "/recap" to "Request a manual recap of the current conversation.",
            "/worktree" to "Create an isolated Git worktree checkout for a new or forked session, then browse and resume worktree sessions.",
        )
        val gefunden = linkedMapOf<String, Pair<String, Boolean>>()
        festeTexte.forEach { (name, text) -> gefunden[name] = text to false }
        for (release in releases) {
            for (zeile in release.text.lineSequence()) {
                val sauber = zeile.trim()
                if (!sauber.startsWith("- ") && !sauber.startsWith("* ")) continue
                val istPrTitel = sauber.drop(2).startsWith("#")
                for (name in slash.findAll(sauber).map { it.groupValues[1] }) {
                    if (name in keineBefehle) continue
                    val bisher = gefunden[name]
                    if (bisher == null || (bisher.second && !istPrTitel)) {
                        gefunden[name] = bereinige(sauber) to istPrTitel
                    }
                }
            }
        }
        return gefunden.map { (name, wert) ->
            GelesenerEintrag(name, wert.first, "Eingebaut", "Neu dazugekommen")
        }
    }

    /** Die älteste Fassung im Fenster, die den Befehl nennt, samt Belegzeile. */
    fun findeEinzug(releases: List<ReleaseNotiz>, name: String): Pair<String, String> {
        val muster = "`$name`"
        val release = releases.lastOrNull { muster in it.text } ?: return "" to ""
        val zeile = release.text.lineSequence().firstOrNull { muster in it }?.let(::bereinige).orEmpty()
        return release.version to zeile
    }

    private fun bereinige(zeile: String): String = zeile.trim()
        .removePrefix("- ").removePrefix("* ")
        .replace(Regex("\\[(#[0-9]+)]\\([^)]*\\)"), "$1")
        .replace(Regex("\\s*\\((?:#[0-9]+(?:,\\s*)?)+\\)"), "")
        .replace(Regex("\\s+@[A-Za-z0-9-]+$"), "")
        .trim()
}

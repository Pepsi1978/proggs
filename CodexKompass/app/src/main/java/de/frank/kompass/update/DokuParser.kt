package de.frank.kompass.update

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

    private val wegfallWorte = Regex("\\b(remove[sd]?|deprecat\\w*|drop(ped|s)?|no longer)\\b", RegexOption.IGNORE_CASE)

    /** Eine Zeichenkette im Objektliteral — in doppelten oder in einfachen Anführungszeichen. */
    private const val ZEICHENKETTE = "(?:\"((?:[^\"\\\\]|\\\\.)*)\"|'((?:[^'\\\\]|\\\\.)*)')"

    /**
     * Ein Eintrag der Konfigurations-Referenz: `{ key: …, type: …, description: … }`.
     * `DOT_MATCHES_ALL`, weil die Beschreibung oft erst in der nächsten Zeile beginnt.
     */
    private val configEintrag = Regex(
        "\\{\\s*key:\\s*$ZEICHENKETTE\\s*,\\s*type:\\s*$ZEICHENKETTE\\s*,\\s*description:\\s*$ZEICHENKETTE",
        RegexOption.DOT_MATCHES_ALL,
    )

    private const val SCHLUESSEL_GRUPPE = 1
    private const val TYP_GRUPPE = 3
    private const val TEXT_GRUPPE = 5

    /** Ab hier beschreibt die Seite die zweite Datei. */
    private const val REQUIREMENTS_UEBERSCHRIFT = "## `requirements.toml`"

    /**
     * Art eines Befehls, den bisher nur die Release-Notes nennen. Solche Einträge gelten nie als
     * entfernt, nur weil sie aus dem Release-Fenster rutschen; nimmt die Doku sie auf, bekommen
     * sie deren Art.
     */
    const val ART_RELEASE = "Eingebaut (Release-Notes)"

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
     * Liest die Schlüssel aus der Konfigurations-Referenz.
     *
     * Die Seite trägt ihre Schlüssel nicht in einer Markdown-Tabelle, sondern in einer
     * Oberflächen-Komponente: `<ConfigTable options={[ { key, type, description }, … ]} />`.
     * Das ist ein JavaScript-Objektliteral, und daraus folgen zwei Dinge, die eine naive
     * Auswertung übersieht:
     *
     *  1. **Beide Anführungszeichen kommen vor.** Steht in einem Typ selbst ein `"`, weicht
     *     die Seite auf `'…'` aus — `type: 'boolean | { context_size = "low|medium|high" }'`.
     *     Wer nur doppelte Anführungszeichen liest, verliert genau die Schlüssel mit den
     *     interessantesten Typen, darunter `tools.web_search` und `web_search`.
     *  2. **Der Text steht oft in der nächsten Zeile.** Nach `description:` bricht die Seite
     *     um, wenn der Satz lang ist. Deshalb wird über Zeilengrenzen hinweg gelesen.
     *
     * Die Seite führt zwei Dateien hintereinander auf: erst `config.toml`, dann
     * `requirements.toml`. Welcher Abschnitt gerade läuft, wird mitgeführt und landet als Art
     * am Eintrag — das beantwortet beim Nachschlagen die erste Frage: „In welche Datei
     * schreibe ich das?"
     */
    fun leseEinstellungen(markdown: String): List<GelesenerEintrag> {
        val grenze = markdown.indexOf(REQUIREMENTS_UEBERSCHRIFT)
        val gefunden = linkedMapOf<String, GelesenerEintrag>()
        for (treffer in configEintrag.findAll(markdown)) {
            val name = feld(treffer, SCHLUESSEL_GRUPPE)
            if (name.isBlank()) continue
            val typ = feld(treffer, TYP_GRUPPE)
            val text = feld(treffer, TEXT_GRUPPE)
            if (text.isBlank()) continue
            val ausRequirements = grenze >= 0 && treffer.range.first > grenze
            gefunden.putIfAbsent(
                name,
                GelesenerEintrag(
                    name = name,
                    // Der Typ gehört mit in den Text: Ohne ihn weiss man beim Nachschlagen
                    // nicht, ob dort eine Zahl, ein Wort oder eine Liste hingehört.
                    beschreibung = if (typ.isBlank()) saeubereText(text) else "${saeubereText(text)} (Typ: $typ)",
                    art = if (ausRequirements) "requirements.toml" else "config.toml",
                ),
            )
        }
        return gefunden.values.toList()
    }

    /** Holt eine der beiden Fassungen einer Zeichenkette — mit `"` oder mit `'` geschrieben. */
    private fun feld(treffer: MatchResult, gruppe: Int): String {
        val doppelt = treffer.groupValues.getOrNull(gruppe).orEmpty()
        val einfach = treffer.groupValues.getOrNull(gruppe + 1).orEmpty()
        return doppelt.ifBlank { einfach }
    }

    /** Nimmt Maskierungen und Zeilenumbrüche aus einem Beschreibungstext. */
    private fun saeubereText(text: String): String = text
        .replace("\\\"", "\"")
        .replace("\\'", "'")
        .replace("\\n", " ")
        .replace(Regex("\\s{2,}"), " ")
        .trim()

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
                // „Removed `/foo`“ belegt gerade nicht, dass es den Befehl gibt.
                if (wegfallWorte.containsMatchIn(sauber)) continue
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
            GelesenerEintrag(
                name,
                wert.first,
                if (name in festeTexte) "Eingebaut" else ART_RELEASE,
                "Neu dazugekommen",
            )
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

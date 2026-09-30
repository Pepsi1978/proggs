package de.frank.kompass.update

import de.frank.kompass.observability.KompassLog

/** Ein aus der Unterlage gelesener Name samt englischer Beschreibung. */
data class GelesenerEintrag(
    val name: String,
    val beschreibung: String,
    val art: String,
    val kategorie: String = "",
)

/**
 * Liest Namen und Beschreibungen aus den Markdown-Seiten der Kimi-Code-Doku
 * (github.com/MoonshotAI/kimi-code, docs/en).
 *
 * Bewusst ohne Beteiligung eines Sprachmodells: Eine Tabelle ist eindeutig auswertbar, und ein
 * Modell könnte einen Namen erfinden oder einen echten weglassen. Erklärt wird später, gelesen
 * wird jetzt.
 *
 * Streng genommen wird dabei:
 *  1. **Nur die richtige Tabelle** — erkannt an ihrer Kopfzeile.
 *  2. **Die richtige Spalte** — über ihren Namen, nie über ihre Position.
 *  3. **Maskierte Trennstriche** — in `` `/plan [on\|off]` `` trennt `\|` keine Spalte.
 */
object DokuParser {

    private val tabellenZeile = Regex("^\\s*\\|(.+)\\|\\s*$")
    private val trennZeile = Regex("^\\s*\\|?[\\s:|-]{4,}\\|?\\s*$")

    /** Teilt an Trennstrichen, die nicht mit `\` maskiert sind. */
    private val spaltenTrenner = Regex("(?<!\\\\)\\|")

    /**
     * Ein Slash-Befehl steht in Rückstrichen, oft mit Argumenten dahinter. Der Name endet am
     * ersten Leerzeichen oder an `[`/`<` — so wird `/title [<text>]` zu `/title` und
     * `/plan clear` zu `/plan`. Auch `/?` ist ein gültiger Name.
     */
    private val slashName = Regex("`(/[^`\\s\\[<]+)")

    /** Eine Umgebungsvariable ist durchgehend gross geschrieben. */
    private val variablenName = Regex("`([A-Z][A-Z0-9_]{2,60})`")

    /** Ein Versionskopf im Änderungsprotokoll: `## 2.1.1 (2026-09-24)`. */
    val versionsKopf = Regex("^##\\s+([0-9]+\\.[0-9]+\\.[0-9]+)(?:\\s+\\(.*\\))?\\s*$")

    /** Eine Tabelle mit ihrer Kopfzeile — erst damit lassen sich Spalten benennen. */
    private data class Tabelle(val kopf: List<String>, val zeilen: List<List<String>>)

    /**
     * Liest alle Befehlstabellen (`| Command | Alias | Description | Always available |`).
     *
     * Die Alias-Spalte kann mehrere Namen tragen (`/h`, `/?`); jeder wird ein eigener Eintrag.
     * Die Tabelle der mitgelieferten Skills hat weder Alias- noch Verfügbarkeitsspalte.
     */
    fun leseSlashBefehle(markdown: String): List<GelesenerEintrag> {
        val gefunden = LinkedHashMap<String, GelesenerEintrag>()
        for (tabelle in leseTabellen(markdown)) {
            val nameIndex = findeSpalte(tabelle.kopf, listOf("Command"))
            val textIndex = findeSpalte(tabelle.kopf, listOf("Description"))
            if (nameIndex < 0 || textIndex < 0) continue
            val aliasIndex = findeSpalte(tabelle.kopf, listOf("Alias"))
            val istSkill = aliasIndex < 0 && findeSpalte(tabelle.kopf, listOf("Always available")) < 0
            for (zeile in tabelle.zeilen) {
                if (zeile.size <= maxOf(nameIndex, textIndex)) continue
                val name = slashName.find(zeile[nameIndex])?.groupValues?.get(1) ?: continue
                val beschreibung = zeile[textIndex]
                if (beschreibung.isBlank()) continue
                gefunden.putIfAbsent(
                    name,
                    GelesenerEintrag(name, beschreibung, if (istSkill) "Mitgelieferter Skill" else "Eingebaut"),
                )
                if (aliasIndex in zeile.indices) {
                    for (alias in slashName.findAll(zeile[aliasIndex])) {
                        val aliasName = alias.groupValues[1]
                        gefunden.putIfAbsent(
                            aliasName,
                            GelesenerEintrag(aliasName, "Alias for $name: $beschreibung", "Alias"),
                        )
                    }
                }
            }
        }
        return gefunden.values.toList().also { melde("Slash-Befehle", it.size) }
    }

    /**
     * Liest die Feld-Tabellen der Konfigurationsseite (`| Field | Type | Default | Description |`).
     *
     * Der Abschnitt steht in der Überschrift darüber (`` ## `loop_control` ``) und wird zum
     * Präfix: `loop_control.max_steps_per_turn`. Anbieter und Modelle sind benannte Tabellen
     * und bekommen den Platzhalter wie im Bestand (`providers.<name>.type`). Zeilen vom Typ
     * `table` sind nur Verweise auf einen Abschnitt und werden übersprungen. Tabellen ohne
     * Type-Spalte (etwa die der veralteten Felder) ebenso.
     */
    fun leseEinstellungen(markdown: String): List<GelesenerEintrag> {
        val gefunden = LinkedHashMap<String, GelesenerEintrag>()
        val h2 = Regex("^##\\s+(.+)$")
        val h3 = Regex("^###\\s+`\\[?([a-z_]+)]?`")
        val abschnittsName = Regex("^`([a-z_.]+)`$")
        val feldName = Regex("`\\[?([a-z_]+)]?(?:\\.([a-z_]+))?`")
        var praefix: String? = null
        var art = "config.toml"
        val zeilen = markdown.lines()
        var index = 0
        while (index < zeilen.size - 1) {
            val zeile = zeilen[index]
            val ueber2 = h2.find(zeile)
            val ueber3 = h3.find(zeile)
            if (ueber2 != null) {
                val titel = ueber2.groupValues[1].trim()
                val abschnitt = abschnittsName.find(titel)?.groupValues?.get(1)
                praefix = when {
                    titel.equals("Top-level fields", ignoreCase = true) -> ""
                    abschnitt == "tui.toml" -> "".also { art = "tui.toml" }
                    abschnitt == "providers" -> "providers.<name>"
                    abschnitt == "models" -> "models.<alias>"
                    abschnitt == "services" -> "services.<name>"
                    abschnitt == "permission" -> "permission.rules"
                    // Das Programm selbst liest [task]; die Seite schreibt teils [background].
                    abschnitt == "background" -> "task"
                    else -> abschnitt
                }
            } else if (ueber3 != null) {
                praefix = ueber3.groupValues[1]
                art = "local.toml"
            }
            val istKopf = tabellenZeile.matches(zeile) && !trennZeile.matches(zeile) &&
                trennZeile.matches(zeilen[index + 1])
            if (!istKopf) {
                index += 1
                continue
            }
            val kopf = zerlege(zeile)
            index += 2
            val nameIndex = findeSpalte(kopf, listOf("Field"))
            val typIndex = findeSpalte(kopf, listOf("Type"))
            val textIndex = findeSpalte(kopf, listOf("Description"))
            while (index < zeilen.size && tabellenZeile.matches(zeilen[index])) {
                val zellen = zerlege(zeilen[index])
                index += 1
                val vorsilbe = praefix ?: continue
                if (nameIndex < 0 || typIndex < 0 || textIndex < 0) continue
                if (zellen.size <= maxOf(nameIndex, typIndex, textIndex)) continue
                if (zellen[typIndex] == "`table`") continue
                val roh = feldName.find(zellen[nameIndex]) ?: continue
                val feld = listOf(roh.groupValues[1], roh.groupValues[2])
                    .filter { it.isNotEmpty() }
                    .joinToString(".")
                val name = if (vorsilbe.isEmpty()) feld else "$vorsilbe.$feld"
                val beschreibung = zellen[textIndex]
                if (beschreibung.isBlank()) continue
                gefunden.putIfAbsent(name, GelesenerEintrag(name, beschreibung, art))
            }
        }
        return gefunden.values.toList().also { melde("Einstellungen", it.size) }
    }

    /**
     * Liest die Variablenseite. Sie hat zwei Formen: eigene Überschriften
     * (`` ### `KIMI_CODE_HOME` ``, Beschreibung im Absatz darunter) und Tabellen mit einer
     * Spalte `Variable` oder `Key`.
     */
    fun leseVariablen(markdown: String): List<GelesenerEintrag> {
        val gefunden = LinkedHashMap<String, GelesenerEintrag>()
        val ueberschrift = Regex("^###\\s+`([A-Z][A-Z0-9_]{2,60})`\\s*$")
        val zeilen = markdown.lines()
        for ((index, zeile) in zeilen.withIndex()) {
            val name = ueberschrift.find(zeile)?.groupValues?.get(1) ?: continue
            val absatz = zeilen.drop(index + 1)
                .dropWhile { it.isBlank() }
                .takeWhile { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("```") }
                .joinToString(" ")
            if (absatz.isNotBlank()) {
                gefunden.putIfAbsent(name, GelesenerEintrag(name, saeubere(absatz), "Umgebungsvariable"))
            }
        }
        for (tabelle in leseTabellen(markdown)) {
            val nameIndex = findeSpalte(tabelle.kopf, listOf("Variable", "Key"))
            val zweckIndex = findeSpalte(tabelle.kopf, listOf("Purpose", "Description"))
            val anbieterIndex = findeSpalte(tabelle.kopf, listOf("Applicable provider"))
            val vorgabeIndex = findeSpalte(tabelle.kopf, listOf("Default"))
            if (nameIndex < 0 || (zweckIndex < 0 && anbieterIndex < 0)) continue
            for (zeile in tabelle.zeilen) {
                if (zeile.size <= nameIndex) continue
                val name = variablenName.find(zeile[nameIndex])?.groupValues?.get(1) ?: continue
                val beschreibung = if (zweckIndex in zeile.indices) {
                    zeile[zweckIndex]
                } else {
                    "Credential key name for ${zeile.getOrElse(anbieterIndex) { "" }}; written in " +
                        "config.toml under [providers.<name>.env], not read from the shell. " +
                        "Default: ${zeile.getOrElse(vorgabeIndex) { "none" }}"
                }
                if (beschreibung.isBlank()) continue
                gefunden.putIfAbsent(name, GelesenerEintrag(name, beschreibung, "Umgebungsvariable"))
            }
        }
        return gefunden.values.toList().also { melde("Umgebungsvariablen", it.size) }
    }

    /** Schneidet das Dokument in Tabellen: Kopfzeile, Trennzeile, Datenzeilen. */
    private fun leseTabellen(markdown: String): List<Tabelle> {
        val zeilen = markdown.lines()
        val tabellen = mutableListOf<Tabelle>()
        var index = 0
        while (index < zeilen.size - 1) {
            val kopfZeile = zeilen[index]
            val istKopf = tabellenZeile.matches(kopfZeile) &&
                !trennZeile.matches(kopfZeile) &&
                trennZeile.matches(zeilen[index + 1])
            if (!istKopf) {
                index += 1
                continue
            }
            val kopf = zerlege(kopfZeile)
            index += 2
            val datenZeilen = mutableListOf<List<String>>()
            while (index < zeilen.size && tabellenZeile.matches(zeilen[index])) {
                if (!trennZeile.matches(zeilen[index])) datenZeilen += zerlege(zeilen[index])
                index += 1
            }
            tabellen += Tabelle(kopf, datenZeilen)
        }
        return tabellen
    }

    /** Sucht eine Spalte über ihren Kopfnamen. -1, wenn die Tabelle sie nicht hat. */
    private fun findeSpalte(kopf: List<String>, namen: List<String>): Int =
        kopf.indexOfFirst { zelle -> namen.any { zelle.equals(it, ignoreCase = true) } }

    private fun zerlege(zeile: String): List<String> {
        val inhalt = tabellenZeile.find(zeile)?.groupValues?.get(1) ?: return emptyList()
        return inhalt.split(spaltenTrenner).map { saeubere(it) }
    }

    /** Nimmt Verweise, maskierte Striche und doppelte Leerzeichen aus einer Tabellenzelle. */
    private fun saeubere(zelle: String): String = zelle
        .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")
        .replace("\\|", "|")
        .replace(Regex("\\s{2,}"), " ")
        .trim()

    /**
     * Findet die neueste Version im Änderungsprotokoll. Es ist absteigend sortiert — die
     * erste Überschrift ist die aktuelle Version.
     */
    fun leseNeuesteVersion(changelog: String): String =
        changelog.lineSequence().firstNotNullOfOrNull { versionsKopf.find(it)?.groupValues?.get(1) }.orEmpty()

    /**
     * Sucht die älteste Version, in der ein Name im Protokoll vorkommt, samt Belegzeile.
     */
    fun findeEinzug(changelog: String, name: String, istSlash: Boolean): Pair<String, String> {
        val muster = if (istSlash) {
            Regex("(?<![A-Za-z0-9._~/-])`?/" + Regex.escape(name.removePrefix("/")) + "(?![a-zA-Z0-9-])")
        } else {
            Regex("`" + Regex.escape(name) + "`")
        }
        val neuMuster = Regex("\\b(Add|Added|New|Introduce|Introduced|Rename|Renamed)\\b")

        var besteVersion = ""
        var besterBeleg = ""
        var besteOhneNeu = ""
        var belegOhneNeu = ""
        var laufendeVersion = ""

        for (zeile in changelog.lineSequence()) {
            val ueberschrift = versionsKopf.find(zeile)
            if (ueberschrift != null) {
                laufendeVersion = ueberschrift.groupValues[1]
                continue
            }
            val gestutzt = zeile.trim()
            if (!gestutzt.startsWith("-") || laufendeVersion.isEmpty()) continue
            if (!muster.containsMatchIn(gestutzt)) continue
            // Das Protokoll läuft von neu nach alt; der jeweils letzte Fund ist der älteste.
            if (neuMuster.containsMatchIn(gestutzt)) {
                besteVersion = laufendeVersion
                besterBeleg = gestutzt.take(220)
            }
            besteOhneNeu = laufendeVersion
            belegOhneNeu = gestutzt.take(220)
        }
        return if (besteVersion.isNotEmpty()) besteVersion to besterBeleg else besteOhneNeu to belegOhneNeu
    }

    private fun melde(was: String, anzahl: Int) {
        KompassLog.info("DokuParser", "lese", "Aus der Unterlage gelesen", mapOf("was" to was, "anzahl" to anzahl))
    }
}

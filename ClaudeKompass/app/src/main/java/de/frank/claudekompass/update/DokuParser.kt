package de.frank.claudekompass.update

import de.frank.claudekompass.observability.KompassLog

/** Ein aus der Unterlage gelesener Name samt englischer Beschreibung. */
data class GelesenerEintrag(
    val name: String,
    val beschreibung: String,
    val art: String,
    val kategorie: String = "",
)

/**
 * Liest Namen und Beschreibungen aus den Markdown-Tabellen der offiziellen Unterlagen.
 *
 * Bewusst ohne Beteiligung eines Sprachmodells: Eine Tabelle ist eindeutig auswertbar, und ein
 * Modell könnte einen Namen erfinden oder einen echten weglassen. Beim Aktualisieren einer
 * Nachschlage-App wäre beides schlimm — hier zählt Genauigkeit, nicht Sprachgefühl. Erklärt
 * wird später, gelesen wird jetzt.
 *
 * Drei Dinge werden dabei streng genommen, weil jedes einzelne schon still danebengegangen ist:
 *
 *  1. **Nur die richtige Tabelle.** Die Einstellungsseite enthält dreizehn Tabellen — die
 *     Übersicht und daneben lauter Unterfeld-Tabellen (`Field | Type | What it does`). Wer
 *     alle liest, holt sich Namen wie `Bash` als angebliche Einstellung. Deshalb wird die
 *     Kopfzeile geprüft und nur eine Tabelle mit den erwarteten Spalten ausgewertet.
 *  2. **Die richtige Spalte.** Die Übersicht hat vier Spalten: `Key | Description | Topic |
 *     Scope`. Wer einfach die letzte nimmt, bekommt „Any file" als Beschreibung — und schickt
 *     genau das als Erklärgrundlage an das Modell.
 *  3. **Maskierte Trennstriche.** In `` `/voice [hold\|tap\|off]` `` steht ein `\|`, das keine
 *     Spalte trennt. Wer stumpf an `|` teilt, verschiebt alle Spalten dieser Zeile.
 */
object DokuParser {

    /** Findet Zeilen wie `| `/compact` | Built-in | Free up context … |`. */
    private val tabellenZeile = Regex("^\\s*\\|(.+)\\|\\s*$")
    private val trennZeile = Regex("^\\s*\\|?[\\s:|-]{4,}\\|?\\s*$")

    /** Teilt an Trennstrichen, die nicht mit `\` maskiert sind. */
    private val spaltenTrenner = Regex("(?<!\\\\)\\|")

    /** Ein Slash-Befehl steht in Rückstrichen, oft mit Argumenten dahinter. */
    private val slashName = Regex("`/([a-z][a-z0-9-]*)")

    /** Ein Einstellungsname steht in Rückstrichen und kann Punkte enthalten. */
    private val einstellungsName = Regex("`([a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z][a-zA-Z0-9_]*)*)`")

    /** Eine Umgebungsvariable ist durchgehend gross geschrieben. */
    private val variablenName = Regex("`([A-Z][A-Z0-9_]{2,60})`")

    /** Eine Tabelle mit ihrer Kopfzeile — erst damit lassen sich Spalten benennen. */
    private data class Tabelle(val kopf: List<String>, val zeilen: List<List<String>>)

    fun leseSlashBefehle(markdown: String): List<GelesenerEintrag> =
        leseSpalten(
            markdown = markdown,
            nameSpalten = listOf("Command", "Befehl"),
            textSpalten = listOf("Purpose", "Description"),
            zusatzSpalten = listOf("Type", "Kind"),
        ) { rohName, beschreibung, zusatz ->
            val treffer = slashName.find(rohName) ?: return@leseSpalten null
            GelesenerEintrag(
                name = "/" + treffer.groupValues[1],
                beschreibung = beschreibung,
                art = (zusatz["Type"] ?: zusatz["Kind"]).orEmpty().ifBlank { "Eingebaut" },
            )
        }.also { melde("Slash-Befehle", it.size) }

    fun leseEinstellungen(markdown: String): List<GelesenerEintrag> =
        leseSpalten(
            markdown = markdown,
            nameSpalten = listOf("Key", "Setting"),
            textSpalten = listOf("Description"),
            zusatzSpalten = listOf("Topic", "Scope"),
        ) { rohName, beschreibung, zusatz ->
            val treffer = einstellungsName.find(rohName) ?: return@leseSpalten null
            val name = treffer.groupValues[1]
            // Durchgehend gross geschriebene Namen sind Umgebungsvariablen und gehören in die
            // andere Liste. Ohne diese Trennung stünden sie doppelt in der App.
            if (name == name.uppercase()) return@leseSpalten null
            GelesenerEintrag(
                name = name,
                beschreibung = beschreibung,
                art = artAusGeltungsbereich(zusatz["Scope"].orEmpty()),
                kategorie = zusatz["Topic"].orEmpty(),
            )
        }.also { melde("Einstellungen", it.size) }

    fun leseVariablen(markdown: String): List<GelesenerEintrag> =
        leseSpalten(
            markdown = markdown,
            nameSpalten = listOf("Variable"),
            textSpalten = listOf("Purpose", "Description"),
        ) { rohName, beschreibung, _ ->
            val treffer = variablenName.find(rohName) ?: return@leseSpalten null
            GelesenerEintrag(
                name = treffer.groupValues[1],
                beschreibung = beschreibung,
                art = "Umgebungsvariable",
            )
        }.also { melde("Umgebungsvariablen", it.size) }

    /**
     * Wo eine Einstellung stehen darf, sagt die Spalte `Scope`. Daraus wird die Angabe, die in
     * der App unter dem Namen steht — sie beantwortet die erste Frage beim Nachschlagen:
     * „In welche Datei schreibe ich das?"
     */
    private fun artAusGeltungsbereich(geltung: String): String = when {
        geltung.contains("managed", ignoreCase = true) -> "managed-settings.json"
        geltung.contains("global config", ignoreCase = true) -> "~/.claude.json"
        else -> "settings.json"
    }

    /**
     * Wertet alle Tabellen aus, deren Kopfzeile die gesuchten Spalten hat.
     *
     * Findet sich keine solche Tabelle, kommt eine leere Liste zurück — der Aufrufer erkennt
     * das an der Untergrenze und bricht ab, statt den Bestand zu leeren.
     */
    private fun leseSpalten(
        markdown: String,
        nameSpalten: List<String>,
        textSpalten: List<String>,
        zusatzSpalten: List<String> = emptyList(),
        deute: (rohName: String, beschreibung: String, zusatz: Map<String, String>) -> GelesenerEintrag?,
    ): List<GelesenerEintrag> {
        val gefunden = LinkedHashMap<String, GelesenerEintrag>()
        for (tabelle in leseTabellen(markdown)) {
            val nameIndex = findeSpalte(tabelle.kopf, nameSpalten)
            val textIndex = findeSpalte(tabelle.kopf, textSpalten)
            if (nameIndex < 0 || textIndex < 0) continue

            val zusatzIndex = zusatzSpalten.associateWith { findeSpalte(tabelle.kopf, listOf(it)) }
            for (zeile in tabelle.zeilen) {
                if (zeile.size <= maxOf(nameIndex, textIndex)) continue
                val beschreibung = zeile[textIndex]
                if (beschreibung.isBlank()) continue
                val zusatz = zusatzIndex
                    .filterValues { it >= 0 && it < zeile.size }
                    .mapValues { (_, index) -> zeile[index] }
                val eintrag = deute(zeile[nameIndex], beschreibung, zusatz) ?: continue
                // Der erste Fund gewinnt: Die Übersichtstabelle steht vor den Wiederholungen
                // weiter unten und trägt die knappere, brauchbarere Beschreibung.
                gefunden.putIfAbsent(eintrag.name, eintrag)
            }
        }
        return gefunden.values.toList()
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
     * Findet die neueste Version im Änderungsprotokoll.
     *
     * Das Protokoll ist absteigend sortiert — die erste Überschrift ist damit die aktuelle
     * Version.
     */
    fun leseNeuesteVersion(changelog: String): String =
        Regex("(?m)^##\\s+([0-9]+\\.[0-9]+\\.[0-9]+)\\s*$")
            .find(changelog)?.groupValues?.get(1).orEmpty()

    /**
     * Sucht die älteste Version, in der ein Name im Protokoll vorkommt.
     *
     * Das ist die belastbarste Auskunft auf die Frage „seit wann gibt es das?", die sich ohne
     * Raten geben lässt. Zurück kommt zusätzlich die Belegzeile, damit in der App sichtbar
     * bleibt, worauf sich die Angabe stützt.
     */
    fun findeEinzug(changelog: String, name: String, istSlash: Boolean): Pair<String, String> {
        val muster = if (istSlash) {
            Regex("`?/" + Regex.escape(name.removePrefix("/")) + "(?![a-zA-Z0-9-])")
        } else {
            Regex("`" + Regex.escape(name) + "`")
        }
        val neuMuster = Regex("\\b(Added|New|Introduced|Renamed)\\b")

        var besteVersion = ""
        var besterBeleg = ""
        var besteOhneNeu = ""
        var belegOhneNeu = ""
        var laufendeVersion = ""

        for (zeile in changelog.lineSequence()) {
            val ueberschrift = Regex("^##\\s+([0-9]+\\.[0-9]+\\.[0-9]+)\\s*$").find(zeile)
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
        return if (besteVersion.isNotEmpty()) {
            besteVersion to besterBeleg
        } else {
            besteOhneNeu to belegOhneNeu
        }
    }

    private fun melde(was: String, anzahl: Int) {
        KompassLog.info("DokuParser", "lese", "Aus der Unterlage gelesen", mapOf("was" to was, "anzahl" to anzahl))
    }
}

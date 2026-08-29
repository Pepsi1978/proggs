package de.frank.claudekompass.update

import de.frank.claudekompass.observability.KompassLog

/** Ein aus der Unterlage gelesener Name samt englischer Beschreibung. */
data class GelesenerEintrag(
    val name: String,
    val beschreibung: String,
    val art: String,
)

/**
 * Liest Namen und Beschreibungen aus den Markdown-Tabellen der offiziellen Unterlagen.
 *
 * Bewusst ohne Beteiligung eines Sprachmodells: Eine Tabelle ist eindeutig auswertbar, und ein
 * Modell könnte einen Namen erfinden oder einen echten weglassen. Beim Aktualisieren einer
 * Nachschlage-App wäre beides schlimm — hier zählt Genauigkeit, nicht Sprachgefühl. Erklärt
 * wird später, gelesen wird jetzt.
 */
object DokuParser {

    /** Findet Zeilen wie `| `/compact` | Built-in | Free up context … |`. */
    private val tabellenZeile = Regex("^\\s*\\|(.+)\\|\\s*$")
    private val trennZeile = Regex("^\\s*\\|?[\\s:|-]{4,}\\|?\\s*$")

    /** Ein Slash-Befehl steht in Rückstrichen, oft mit Argumenten dahinter. */
    private val slashName = Regex("`/([a-z][a-z0-9-]*)")

    /** Ein Einstellungsname steht in Rückstrichen und kann Punkte enthalten. */
    private val einstellungsName = Regex("`([a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z][a-zA-Z0-9_]*)*)`")

    /** Eine Umgebungsvariable ist durchgehend gross geschrieben. */
    private val variablenName = Regex("`([A-Z][A-Z0-9_]{2,60})`")

    fun leseSlashBefehle(markdown: String): List<GelesenerEintrag> =
        leseTabellen(markdown) { zellen ->
            val treffer = slashName.find(zellen.first()) ?: return@leseTabellen null
            GelesenerEintrag(
                name = "/" + treffer.groupValues[1],
                beschreibung = zellen.last().trim(),
                art = if (zellen.size >= 3) zellen[1].trim() else "",
            )
        }.also { melde("Slash-Befehle", it.size) }

    fun leseEinstellungen(markdown: String): List<GelesenerEintrag> =
        leseTabellen(markdown) { zellen ->
            val treffer = einstellungsName.find(zellen.first()) ?: return@leseTabellen null
            val name = treffer.groupValues[1]
            // Durchgehend gross geschriebene Namen sind Umgebungsvariablen und gehören in die
            // andere Liste. Ohne diese Trennung stünden sie doppelt in der App.
            if (name == name.uppercase()) return@leseTabellen null
            GelesenerEintrag(
                name = name,
                beschreibung = zellen.last().trim(),
                art = "settings.json",
            )
        }.also { melde("Einstellungen", it.size) }

    fun leseVariablen(markdown: String): List<GelesenerEintrag> =
        leseTabellen(markdown) { zellen ->
            val treffer = variablenName.find(zellen.first()) ?: return@leseTabellen null
            GelesenerEintrag(
                name = treffer.groupValues[1],
                beschreibung = zellen.last().trim(),
                art = "Umgebungsvariable",
            )
        }.also { melde("Umgebungsvariablen", it.size) }

    private fun leseTabellen(
        markdown: String,
        deute: (List<String>) -> GelesenerEintrag?,
    ): List<GelesenerEintrag> {
        val gefunden = LinkedHashMap<String, GelesenerEintrag>()
        for (zeile in markdown.lineSequence()) {
            if (trennZeile.matches(zeile)) continue
            val treffer = tabellenZeile.find(zeile) ?: continue
            val zellen = treffer.groupValues[1].split('|').map { saeubere(it) }
            if (zellen.size < 2) continue
            val eintrag = deute(zellen) ?: continue
            if (eintrag.beschreibung.isBlank()) continue
            // Der erste Fund gewinnt: Die Übersichtstabelle steht vor den Wiederholungen
            // weiter unten und trägt die knappere, brauchbarere Beschreibung.
            gefunden.putIfAbsent(eintrag.name, eintrag)
        }
        return gefunden.values.toList()
    }

    /** Nimmt Verweise, Rückstriche und doppelte Leerzeichen aus einer Tabellenzelle. */
    private fun saeubere(zelle: String): String = zelle
        .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")
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

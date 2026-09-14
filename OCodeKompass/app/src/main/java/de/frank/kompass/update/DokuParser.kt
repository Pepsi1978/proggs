package de.frank.kompass.update

import org.json.JSONArray
import org.json.JSONObject

data class GelesenerEintrag(
    val name: String,
    val beschreibung: String,
    val art: String,
    val kategorie: String = "",
)

/** Eine veröffentlichte OpenCode-Fassung aus den GitHub-Releases, neueste zuerst. */
data class ReleaseNotiz(val version: String, val text: String)

/**
 * Liest die offizielle Befehlsliste aus der OpenCode-Doku. Namen und Aliasse kommen niemals
 * vom Modell.
 *
 * Die OpenCode-Doku führt die Befehle nicht als Tabelle, sondern als Abschnitte:
 *
 * ```
 * ### compact
 *
 * Compact the current session. _Alias_: `/summarize`
 * ```
 *
 * Gelesen wird ausschliesslich der Bereich zwischen „## Commands" und der nächsten Überschrift
 * derselben Ebene. Alles davor ist Fliesstext über `@`-Verweise und `!`-Befehle; alles danach
 * gehört zur Editor-Einrichtung. Ohne diese Abgrenzung landete jedes `/pfad/im/text` im Bestand.
 */
object DokuParser {
    private val slash = Regex("`(/[a-z][a-z0-9-]*)`")
    private val ueberschrift = Regex("^###\\s+([a-z][a-z0-9-]*)\\s*$")
    /**
     * Die Alias-Angabe eines Abschnitts.
     *
     * `Alias(?:es)?` statt `Aliases?`: Die Doku schreibt bei einem einzigen Alias "_Alias_",
     * bei mehreren "_Aliases_" — ein blosses Frage-s hätte die Singular-Form verfehlt und
     * /summarize wie /clear stillschweigend verschluckt. MULTILINE, weil die Angabe mitten im
     * Abschnitt steht und nicht an dessen Ende.
     */
    private val aliasZeile = Regex("_Alias(?:es)?_:\\s*(.+)$", RegexOption.MULTILINE)
    /** Fassungen heissen `v1.18.30`. Vorabfassungen tragen zusätzlich einen Bindestrich-Teil. */
    private val stabileFassung = Regex("v([0-9]+\\.[0-9]+\\.[0-9]+)")

    /** Pfade, die in den Release-Notes in Backticks stehen, aber keine Befehle sind. */
    private val keineBefehle = setOf(
        "/dev", "/tmp", "/etc", "/usr", "/bin", "/sbin", "/home", "/var", "/proc", "/sys",
        "/opt", "/mnt", "/root", "/lib", "/private", "/workspace", "/users", "/volumes",
        "/docs", "/api", "/session", "/config", "/global",
    )

    private val wegfallWorte = Regex("\\b(remove[sd]?|deprecat\\w*|drop(ped|s)?|no longer)\\b", RegexOption.IGNORE_CASE)

    /**
     * Art eines Befehls, den bisher nur die Release-Notes nennen. Solche Einträge gelten nie als
     * entfernt, nur weil sie aus dem Release-Fenster rutschen; nimmt die Doku sie auf, bekommen
     * sie deren Art.
     */
    const val ART_RELEASE = "Eingebaut (Release-Notes)"

    /**
     * Befehle, die nur ausserhalb der Befehlsliste belegt sind und nie als entfernt gelten.
     *
     * Bei OpenCode steht alles Eingebaute in der Liste selbst — deshalb ist die Menge leer.
     * Sie bleibt stehen, damit ein später nachgetragener Sonderfall hier seinen Platz hat.
     */
    val changelogNamen = emptySet<String>()

    /** So verweist ein JSON-Schema auf einen seiner eigenen Bausteine. */
    private const val VERWEIS_VORSILBE = "#/\$defs/"

    /**
     * Wie tief die Schlüsselpfade gelesen werden.
     *
     * Zwei Ebenen sind die Grenze, an der die Angaben aufhören zu helfen: `server.port` sucht
     * man, das Bauteil der dritten Ebene darunter nicht. Ausserdem verweisen die Bausteine des
     * Schemas teilweise auf sich selbst — ohne Grenze liefe die Auflösung im Kreis.
     */
    private const val MAX_TIEFE = 2

    /**
     * Liest die Einstellungen aus dem offiziellen JSON-Schema von `opencode.json`.
     *
     * **Warum das Schema und nicht die Doku-Seite.** `config.mdx` erklärt die Einstellungen an
     * Beispielblöcken: ein Stück JSON, darunter ein Absatz Fliesstext. Daraus lässt sich keine
     * verlässliche Schlüsselliste gewinnen — ein Beispiel zeigt, was eine Einstellung kann,
     * nicht welche es gibt. Das Schema dagegen ist die Datei, gegen die OpenCode selbst prüft.
     * Es ist die Wahrheit, es trägt die Beschreibungen mit, und es ist mit 39 KB klein.
     *
     * Bis Fassung 0.6.8 war der Config-Bereich an gar keine Quelle angeschlossen: Der Abgleich
     * kannte nur `Bereich.SLASH`. `URL_CONFIG` stand zwar in der Abruf-Klasse, wurde aber nie
     * aufgerufen. Die Einstellungsliste blieb deshalb auf ihrem Auslieferungsstand stehen.
     *
     * Gelesen wird zwei Ebenen tief. Das ist keine Willkür, sondern die Grenze, an der die
     * Angaben aufhören, beim Nachschlagen zu helfen: `server.port` ist eine Einstellung, die
     * man sucht; die dritte Ebene darunter ist Bauteil einer Struktur. Ausserdem verweisen die
     * Bausteine des Schemas teilweise auf sich selbst — ohne Grenze liefe die Auflösung im
     * Kreis.
     *
     * Wo das Schema einen freien Namen erlaubt (`additionalProperties`), steht `<name>` im
     * Pfad — so wie es auch die Referenz von Codex schreibt: `command.<name>.template`.
     */
    fun leseEinstellungen(schemaJson: String): List<GelesenerEintrag> {
        val wurzel = runCatching { JSONObject(schemaJson) }.getOrNull() ?: return emptyList()
        val bausteine = wurzel.optJSONObject("\$defs") ?: JSONObject()
        val gefunden = linkedMapOf<String, GelesenerEintrag>()

        fun loese(knoten: JSONObject?): JSONObject? {
            if (knoten == null) return null
            val verweis = knoten.optString("\$ref")
            if (!verweis.startsWith(VERWEIS_VORSILBE)) return knoten
            return bausteine.optJSONObject(verweis.removePrefix(VERWEIS_VORSILBE))
        }

        fun gehe(knoten: JSONObject?, praefix: String, tiefe: Int) {
            if (tiefe > MAX_TIEFE) return
            val ziel = loese(knoten) ?: return
            val eigenschaften = ziel.optJSONObject("properties")
            if (eigenschaften != null) {
                for (name in eigenschaften.keys()) {
                    // Der Schema-Verweis ist eine Angabe für den Editor, keine Einstellung.
                    if (name == "\$schema") continue
                    val kind = eigenschaften.optJSONObject(name) ?: continue
                    val pfad = praefix + name
                    val aufgeloest = loese(kind)
                    val text = kind.optString("description")
                        .ifBlank { aufgeloest?.optString("description").orEmpty() }
                    gefunden.putIfAbsent(
                        pfad,
                        GelesenerEintrag(
                            name = pfad,
                            beschreibung = beschreibungOderHinweis(pfad, text, typVon(aufgeloest ?: kind)),
                            art = "opencode.json",
                        ),
                    )
                    gehe(kind, "$pfad.", tiefe + 1)
                }
            }
            val freieNamen = ziel.optJSONObject("additionalProperties")
            if (freieNamen != null && tiefe < MAX_TIEFE) {
                gehe(freieNamen, praefix + "<name>.", tiefe + 1)
            }
        }

        gehe(wurzel, "", 0)
        return gefunden.values.toList()
    }

    /**
     * Macht aus einem Schlüssel ohne Beschreibung einen ehrlichen Text.
     *
     * Rund die Hälfte der Unterfelder trägt im Schema keinen erklärenden Satz. Sie deshalb
     * wegzulassen wäre falsch — es sind gültige Einstellungen, und wer sie sucht, soll sie
     * finden. Sie mit einem erfundenen Satz zu füllen wäre schlimmer. Also steht dort, was
     * sicher stimmt: wohin der Schlüssel gehört, welchen Typ er hat, und dass die offizielle
     * Beschreibung an dieser Stelle fehlt.
     */
    private fun beschreibungOderHinweis(pfad: String, text: String, typ: String): String {
        val typteil = if (typ.isBlank()) "" else " (Typ: $typ)"
        if (text.isNotBlank()) return text.replace(Regex("\\s{2,}"), " ").trim() + typteil
        val eltern = pfad.substringBeforeLast('.', "")
        val herkunft = if (eltern.isBlank()) "Schlüssel in `opencode.json`" else "Teil der Einstellung `$eltern`"
        return "$herkunft$typteil. Das offizielle Schema führt zu diesem Schlüssel keinen " +
            "erklärenden Satz."
    }

    /** Der Typ, so wie ihn das Schema angibt. Mehrere erlaubte Formen werden benannt, nicht geraten. */
    private fun typVon(knoten: JSONObject): String {
        knoten.opt("type")?.let { wert ->
            if (wert is String) return wert
            if (wert is JSONArray) {
                return (0 until wert.length()).joinToString(" | ") { wert.optString(it) }
            }
        }
        knoten.optJSONArray("enum")?.let { werte ->
            return (0 until minOf(werte.length(), 6)).joinToString(" | ") { werte.optString(it) }
        }
        if (knoten.has("anyOf") || knoten.has("oneOf")) return "mehrere Formen"
        return ""
    }

    fun leseSlashBefehle(markdown: String): List<GelesenerEintrag> {
        val abschnitt = markdown.substringAfter("\n## Commands", "").substringBefore("\n## ")
        if (abschnitt.isBlank()) return emptyList()

        val ergebnis = linkedMapOf<String, GelesenerEintrag>()
        var name = ""
        val text = StringBuilder()
        var inCodeBlock = false

        fun uebernimm() {
            if (name.isBlank()) return
            val roh = text.toString().trim()
            val aliasse = aliasZeile.find(roh)
                ?.groupValues?.get(1)
                ?.let { teil -> slash.findAll(teil).map { it.groupValues[1] }.toList() }
                .orEmpty()
            val beschreibung = bereinigeBeschreibung(roh)
            if (beschreibung.isNotBlank()) {
                ergebnis.putIfAbsent(name, GelesenerEintrag(name, beschreibung, "Eingebaut"))
                aliasse.forEach { alias ->
                    ergebnis.putIfAbsent(alias, GelesenerEintrag(alias, beschreibung, "Alias von $name"))
                }
            }
            name = ""
            text.clear()
        }

        for (zeile in abschnitt.lineSequence()) {
            if (zeile.trimStart().startsWith("```")) {
                // Die Beispielblöcke enthalten nur den Befehl selbst; sie tragen nichts bei.
                inCodeBlock = !inCodeBlock
                continue
            }
            if (inCodeBlock) continue
            val treffer = ueberschrift.find(zeile.trim())
            if (treffer != null) {
                uebernimm()
                name = "/" + treffer.groupValues[1]
                continue
            }
            if (name.isNotBlank()) text.append(zeile).append('\n')
        }
        uebernimm()
        return ergebnis.values.toList()
    }

    /**
     * Macht aus dem Abschnittstext einen Satz, der als offizielle Beschreibung taugt.
     *
     * Genommen wird NUR der erste Absatz. Danach folgen Hinweiskästen, die Tastenkürzel-Zeile
     * und Nachbemerkungen — sie beschreiben den Befehl nicht mehr und blähten den Eintrag auf.
     * Weg müssen ausserdem die Verweise auf andere Doku-Seiten (ihr Linkziel gibt es hier
     * nicht) und die Alias-Angabe, die schon als eigener Eintrag steht.
     */
    private fun bereinigeBeschreibung(roh: String): String {
        val ersterAbsatz = roh.split("\n\n")
            .map { absatz ->
                absatz.lineSequence()
                    .map { it.trim() }
                    .filter { zeile ->
                        zeile.isNotBlank() &&
                            !zeile.startsWith(":::") &&
                            !zeile.startsWith("---") &&
                            !zeile.startsWith("**Keybind:**")
                    }
                    .joinToString(" ")
            }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        return ersterAbsatz
            .replace(aliasZeile, "")
            .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
            .replace(Regex("`([^`]*)`"), "$1")
            .replace("**", "")
            // Der Linktext bliebe sonst als nackter Satz "Learn more." stehen und sagt nichts.
            .replace(Regex("\\s*\\bLearn more\\b\\s*\\.?"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('.', ' ')
            .let { if (it.isBlank()) "" else "$it." }
    }

    /**
     * Liest die Fassungen aus der GitHub-Releases-API.
     *
     * OpenCode veröffentlicht sehr häufig; die Versionsnummer der neuesten Ausgabe ist der
     * Stand, gegen den abgeglichen wird. Vorabfassungen und Entwürfe werden übergangen.
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
     * Slash-Befehle, die in den Release-Notes vorkommen, aber (noch) nicht in der Befehlsliste.
     *
     * Beschreibung ist die Zeile der neuesten Fassung, die den Befehl nennt — bevorzugt eine
     * Hervorhebung, nicht der nackte PR-Titel.
     */
    fun ergaenzeAusReleases(releases: List<ReleaseNotiz>): List<GelesenerEintrag> {
        val gefunden = linkedMapOf<String, Pair<String, Boolean>>()
        for (release in releases) {
            for (zeile in release.text.lineSequence()) {
                val sauber = zeile.trim()
                if (!sauber.startsWith("- ") && !sauber.startsWith("* ")) continue
                // „Removed `/foo`" belegt gerade nicht, dass es den Befehl gibt.
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
            GelesenerEintrag(name, wert.first, ART_RELEASE, "Neu dazugekommen")
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

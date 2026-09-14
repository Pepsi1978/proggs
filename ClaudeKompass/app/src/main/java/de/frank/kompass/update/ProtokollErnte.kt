package de.frank.kompass.update

import de.frank.kompass.observability.KompassLog

/**
 * Liest Namen aus dem Änderungsprotokoll — als zweite Quelle neben den Doku-Tabellen.
 *
 * **Warum es diese Datei gibt.** Die offiziellen Übersichtsseiten sind nicht vollständig.
 * `/output-style` kam in 2.1.269 zurück und steht bis heute in keiner Zeile der
 * Befehlstabelle; `bashEditDiffEnabled`, `CLAUDE_CODE_WORKFLOW_MAX_CONCURRENT_AGENTS` und
 * `CLAUDE_CODE_GATEWAY_MODEL_DISCOVERY_TIMEOUT_MS` fehlen genauso in der Einstellungs- und
 * der Variablenliste. Solange die Tabellen die einzige Quelle waren, konnte der
 * Aktualisieren-Knopf diese Einträge nicht finden — egal wie oft man ihn drückte. Das war
 * kein Fehler im Abgleich, sondern eine Lücke in der Quelle.
 *
 * Das Protokoll schliesst die Lücke, weil dort jede Neuerung mit Version steht. Es ist aber
 * Fliesstext, kein Datensatz — deshalb wird hier strenger gelesen als in den Tabellen:
 *
 *  1. **Nur die Einführungszeile zählt.** Ein Slash-Befehl gilt nur, wenn er unmittelbar
 *     hinter `Added`/`Introduced` steht. Eine Zeile wie „Fixed deny rules on symlinked
 *     directories (`/etc`, `/tmp`, `/var`)" nennt drei Pfade, keine Befehle — bei loser
 *     Suche stünden sie als Befehle in einer Nachschlage-App.
 *  2. **Der Name muss an sein Signalwort grenzen.** „`skillOverrides` setting now works:
 *     `off` hides …" nennt zwei Namen; nur der erste ist eine Einstellung. Steht nur
 *     `option` dabei, muss der Name ausserdem wie ein Schlüssel aussehen (Binnenversalie
 *     oder Punkt) — sonst wird aus der „`ultra` option" des Bericht-Knopfs eine Einstellung.
 *  3. **Umgebungsvariablen nur mit Claude-Code-Vorsilbe.** Das Protokoll nennt auch
 *     `GIT_DIR`, `GH_TOKEN` oder `XDG_DATA_HOME`. Das sind fremde Variablen; sie gehören
 *     nicht in eine Liste der Einstellmöglichkeiten von Claude Code.
 *  4. **Die neueste Nennung entscheidet über Leben und Tod.** Das Protokoll läuft von neu
 *     nach alt. Wird ein Name in seiner jüngsten Zeile als `Removed`/`Deprecated` geführt
 *     oder dort weg-umbenannt, kommt er nicht in die App. `/output-style` war in 2.0.96
 *     abgekündigt und ist seit 2.1.269 wieder da — und genau deshalb gehört er hinein.
 *
 * Bewusst ohne Versionsfenster: Gelesen wird immer die ganze Historie. Ein Fenster „seit dem
 * letzten Lauf" würde `/output-style` beim übernächsten Lauf wieder herausfallen lassen —
 * er stünde weiterhin in keiner Tabelle und wäre plötzlich „verschwunden". Was diese Ernte
 * findet, hängt nur vom Protokoll ab, nicht davon, wann zuletzt aktualisiert wurde.
 */
object ProtokollErnte {

    /** Ein Eintrag aus dem Protokoll samt der Zeile, die ihn belegt. */
    private data class Fund(val version: String, val zeile: String)

    private val versionsZeile = Regex("^##\\s+([0-9]+\\.[0-9]+\\.[0-9]+)\\s*$")

    /** `Added `/output-style [name]`` — der Name steht direkt hinter dem Verb. */
    private val einfuehrungSlash =
        Regex("^-\\s+(?:Added|Introduced)\\s+(?:the\\s+|a\\s+new\\s+)?`/([a-z][a-z0-9-]*)(?![a-zA-Z0-9_/-])")

    /** Jede Nennung eines Befehls — für die Frage, ob es ihn noch gibt. */
    private val nennungSlash = Regex("`/([a-z][a-z0-9-]*)(?![a-zA-Z0-9_/-])")

    /** `CLAUDE_CODE_…` — auch in der Form `NAME=0`, wie sie im Protokoll oft steht. */
    private val nennungVariable = Regex("`([A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+)[`=]")

    /**
     * Nur Variablen, die zu Claude Code gehören. Das Protokoll nennt auch fremde.
     */
    private val eigeneVorsilbe =
        Regex("^(CLAUDE|ANTHROPIC|OTEL|DISABLE|ENABLE|USE_|MAX_|BASH_|MCP_|VERTEX|CLOUD_ML|AWS_BEARER|SLACK_|GATEWAY_)")

    private val schluesselMuster = "`([a-z][a-zA-Z0-9]*(?:\\.[a-zA-Z][a-zA-Z0-9]*)*)`"

    /** „setting `x`", „option `x`", „config `x`" — Signalwort links vom Namen. */
    private val einstellungLinks = Regex("\\b(?:setting|option|config)\\s+$schluesselMuster")

    /** „`x` setting", „`x` managed setting" — Signalwort rechts. */
    private val einstellungRechts = Regex("$schluesselMuster\\s+(?:managed\\s+)?setting\\b")

    /** „`x` option" — schwächeres Signal, deshalb zusätzlich die Form des Namens prüfen. */
    private val optionRechts = Regex("$schluesselMuster\\s+(?:config\\s+)?option\\b")

    /** Ein Schlüssel sieht aus wie ein Schlüssel: Binnenversalie oder Punkt. */
    private val wieEinSchluessel = Regex("[A-Z.]")

    /**
     * Zeilen der Editor-Erweiterungen. Deren Einstellungen stehen in der Oberfläche von
     * VS Code, nicht in `settings.json` — sie hätten in dieser App nur Verwirrung gestiftet.
     */
    private val fremdeOberflaeche = Regex("^-\\s+\\[(VSCode|VS Code|JetBrains|IntelliJ)]")

    /** Eine Zeile, die etwas einführt — ihr Text erklärt einen Eintrag besser als eine Fehlerbehebung. */
    private val einfuehrungsZeile = Regex("^-\\s+(?:Added|Introduced)\\b")

    private val weggefallen = Regex("\\b(Removed|Deprecated)\\b")
    private val umbenannt = Regex("\\bRenamed\\b")

    fun leseSlashBefehle(changelog: String): List<GelesenerEintrag> =
        lese(
            changelog = changelog,
            nurEigeneZeilen = false,
            nennungen = { zeile -> nennungSlash.findAll(zeile).map { "/" + it.groupValues[1] }.toList() },
            einfuehrungen = { zeile ->
                einfuehrungSlash.find(zeile)?.let { listOf("/" + it.groupValues[1]) }.orEmpty()
            },
        ).map { (name, fund) ->
            GelesenerEintrag(
                name = name,
                beschreibung = saeubere(fund.zeile),
                art = "Eingebaut",
                kategorie = HERKUNFT,
            )
        }.also { melde("Slash-Befehle", it.size) }

    fun leseEinstellungen(changelog: String): List<GelesenerEintrag> =
        lese(
            changelog = changelog,
            nurEigeneZeilen = true,
            nennungen = ::einstellungsNamen,
            einfuehrungen = ::einstellungsNamen,
        ).map { (name, fund) ->
            GelesenerEintrag(
                name = name,
                beschreibung = saeubere(fund.zeile),
                art = if (fund.zeile.contains("managed", ignoreCase = true)) {
                    "managed-settings.json"
                } else {
                    "settings.json"
                },
                kategorie = HERKUNFT,
            )
        }.also { melde("Einstellungen", it.size) }

    fun leseVariablen(changelog: String): List<GelesenerEintrag> =
        lese(
            changelog = changelog,
            nurEigeneZeilen = true,
            nennungen = ::variablenNamen,
            einfuehrungen = ::variablenNamen,
        ).map { (name, fund) ->
            GelesenerEintrag(
                name = name,
                beschreibung = saeubere(fund.zeile),
                art = "Umgebungsvariable",
                kategorie = HERKUNFT,
            )
        }.also { melde("Umgebungsvariablen", it.size) }

    private fun einstellungsNamen(zeile: String): List<String> = buildList {
        einstellungLinks.findAll(zeile).forEach { add(it.groupValues[1]) }
        einstellungRechts.findAll(zeile).forEach { add(it.groupValues[1]) }
        optionRechts.findAll(zeile)
            .map { it.groupValues[1] }
            .filter { wieEinSchluessel.containsMatchIn(it) }
            .forEach { add(it) }
    }

    private fun variablenNamen(zeile: String): List<String> =
        nennungVariable.findAll(zeile)
            .map { it.groupValues[1] }
            .filter { eigeneVorsilbe.containsMatchIn(it) }
            .toList()

    /**
     * Geht das Protokoll einmal von oben nach unten durch.
     *
     * Oben steht die neueste Fassung. Deshalb entscheidet die erste Begegnung mit einem Namen
     * darüber, ob es ihn noch gibt — und die letzte darüber, seit wann. Ein Eintrag kommt nur
     * zurück, wenn er irgendwo eingeführt wurde und in seiner jüngsten Zeile nicht als
     * weggefallen dasteht.
     */
    private fun lese(
        changelog: String,
        nurEigeneZeilen: Boolean,
        nennungen: (String) -> List<String>,
        einfuehrungen: (String) -> List<String>,
    ): List<Pair<String, Fund>> {
        // Zwei Töpfe: Eine Zeile mit „Added" ist die bessere Grundlage für die spätere
        // Erklärung als eine Fehlerbehebung, die den Namen nur nebenbei nennt. Gibt es beides,
        // gewinnt die Einführungszeile.
        val mitVerb = LinkedHashMap<String, Fund>()
        val ohneVerb = LinkedHashMap<String, Fund>()
        val tot = mutableSetOf<String>()
        val gesehen = mutableSetOf<String>()
        var version = ""

        for (rohZeile in changelog.lineSequence()) {
            val kopf = versionsZeile.find(rohZeile)
            if (kopf != null) {
                version = kopf.groupValues[1]
                continue
            }
            val zeile = rohZeile.trim()
            if (!zeile.startsWith("-") || version.isEmpty()) continue
            if (nurEigeneZeilen && fremdeOberflaeche.containsMatchIn(zeile)) continue

            // Erste Begegnung = jüngste Zeile: nur sie entscheidet über den Lebendstatus.
            for (name in nennungen(zeile)) {
                if (!gesehen.add(name)) continue
                if (weggefallen.containsMatchIn(zeile)) {
                    tot += name
                } else if (umbenannt.containsMatchIn(zeile) && stehtVorDemNeuenNamen(zeile, name)) {
                    tot += name
                }
            }
            // Überschrieben wird bewusst: Der älteste Fund trägt die richtige „seit"-Angabe,
            // und der kommt beim Durchlauf von oben zuletzt.
            val topf = if (einfuehrungsZeile.containsMatchIn(zeile)) mitVerb else ohneVerb
            for (name in einfuehrungen(zeile)) topf[name] = Fund(version, zeile)
        }
        val zusammen = LinkedHashMap<String, Fund>(ohneVerb)
        zusammen.putAll(mitVerb)
        return zusammen.entries
            .filter { it.key !in tot }
            .map { it.key to it.value }
    }

    /**
     * Bei „Renamed `/alt` to `/neu`" ist nur der alte Name weg. Steht der Name links von
     * „ to ", ist er der alte.
     */
    private fun stehtVorDemNeuenNamen(zeile: String, name: String): Boolean {
        val trennung = zeile.indexOf(" to ")
        if (trennung < 0) return false
        val blank = name.removePrefix("/")
        return zeile.take(trennung).contains(blank)
    }

    /** Macht aus der Protokollzeile einen lesbaren englischen Satz. */
    private fun saeubere(zeile: String): String = zeile
        .removePrefix("-")
        .trim()
        .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")
        .replace(Regex("\\s{2,}"), " ")
        .take(400)

    private fun melde(was: String, anzahl: Int) {
        KompassLog.info(
            "ProtokollErnte",
            "lese",
            "Aus dem Änderungsprotokoll geerntet",
            mapOf("was" to was, "anzahl" to anzahl),
        )
    }

    /**
     * Steht als Kategorie an jedem Eintrag, den nur das Protokoll kennt. In der App ist damit
     * sichtbar, worauf die Angabe beruht — die Doku-Tabelle führt ihn ja nicht.
     */
    const val HERKUNFT = "Aus dem Änderungsprotokoll"
}

package de.frank.claudekompass

import de.frank.claudekompass.update.DokuParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft das Auswerten der offiziellen Unterlagen.
 *
 * Das ist der empfindlichste Teil des Aktualisierens: Liest der Parser nichts mehr, gälte
 * jeder vorhandene Eintrag als verschwunden. Die Untergrenze im Aktualisierer fängt das ab —
 * diese Tests halten fest, dass die Auswertung überhaupt das Richtige liest.
 */
class DokuParserTest {

    private val befehleMarkdown = """
        # Commands

        | Command | Type | Description |
        | :------ | :--- | :---------- |
        | `/clear` | Built-in | Start a new conversation with an empty context |
        | `/compact [instructions]` | Built-in | Free up context by summarizing the conversation |
        | `/code-review [level]` | Skill | Review the current diff for correctness bugs |
        | `/effort [level]` | Built-in | Set the effort level |
    """.trimIndent()

    /**
     * Nachgebaut nach der echten Einstellungsseite: vier Spalten, die Beschreibung in der
     * zweiten — und darunter eine der vielen Unterfeld-Tabellen, die NICHT ausgewertet werden
     * darf. Genau die hat der Parser früher mitgelesen und daraus Einstellungen wie `Bash`
     * erfunden.
     */
    private val einstellungenMarkdown = """
        | Key | Description | Topic | Scope |
        | :-- | :---------- | :---- | :---- |
        | [`autoCompactEnabled`](#a) | Turn automatic compaction off or on | Memory and context | Any file |
        | `permissions.deny` | Block listed tool uses | Permission settings | Managed |
        | `ANTHROPIC_API_KEY` | API key sent as header | Authentication | Any file |

        ## Unterfelder

        | Field | Type | What it does |
        | :---- | :--- | :----------- |
        | `Bash` | string | Matches shell commands |
        | `commit` | string | Change the trailer |
    """.trimIndent()

    private val variablenMarkdown = """
        | Variable | Purpose |
        | :------- | :------ |
        | `ANTHROPIC_API_KEY` | API key sent as header |
        | `NO_COLOR` | Turn off colored output |
    """.trimIndent()

    private val changelog = """
        ## 2.1.251

        - Added `PreModelSwitch` hook events

        ## 2.1.76

        - Added `/effort` slash command to set model effort level

        ## 2.0.31

        - Fixed issue causing `/compact` to fail
    """.trimIndent()

    @Test
    fun liestSlashBefehleAusDerTabelle() {
        val gelesen = DokuParser.leseSlashBefehle(befehleMarkdown)
        assertEquals(4, gelesen.size)
        assertTrue(gelesen.any { it.name == "/clear" })
        assertTrue(gelesen.any { it.name == "/code-review" })
        // Die Argumente hinter dem Namen gehören nicht zum Namen.
        assertTrue(gelesen.any { it.name == "/compact" })
        assertFalse(gelesen.any { it.name.contains("[") })
    }

    @Test
    fun uebernimmtDieBeschreibung() {
        val gelesen = DokuParser.leseSlashBefehle(befehleMarkdown)
        val clear = gelesen.first { it.name == "/clear" }
        assertEquals("Start a new conversation with an empty context", clear.beschreibung)
        assertEquals("Built-in", clear.art)
    }

    @Test
    fun trenntEinstellungenVonVariablen() {
        val einstellungen = DokuParser.leseEinstellungen(einstellungenMarkdown)
        // Durchgehend gross geschriebene Namen sind Variablen und gehören nicht in diese Liste,
        // sonst stünden sie doppelt in der App.
        assertFalse(einstellungen.any { it.name == "ANTHROPIC_API_KEY" })
        assertTrue(einstellungen.any { it.name == "autoCompactEnabled" })
        assertTrue(einstellungen.any { it.name == "permissions.deny" })

        val variablen = DokuParser.leseVariablen(variablenMarkdown)
        assertTrue(variablen.any { it.name == "ANTHROPIC_API_KEY" })
        assertTrue(variablen.any { it.name == "NO_COLOR" })
    }

    @Test
    fun nimmtDieBeschreibungsSpalteUndNichtDieLetzte() {
        val einstellungen = DokuParser.leseEinstellungen(einstellungenMarkdown)
        val eintrag = einstellungen.first { it.name == "autoCompactEnabled" }
        // Früher stand hier „Any file" — die letzte Spalte. Genau dieser Text ging danach als
        // Erklärgrundlage ans Modell und galt zugleich als geänderte offizielle Beschreibung.
        assertEquals("Turn automatic compaction off or on", eintrag.beschreibung)
        assertEquals("Memory and context", eintrag.kategorie)
        assertEquals("settings.json", eintrag.art)
    }

    @Test
    fun liestNurDieUebersichtsTabelleUndNichtDieUnterfelder() {
        val einstellungen = DokuParser.leseEinstellungen(einstellungenMarkdown)
        // `Bash` und `commit` stehen in einer Unterfeld-Tabelle. Sie sind keine Einstellungen.
        assertFalse(einstellungen.any { it.name == "Bash" })
        assertFalse(einstellungen.any { it.name == "commit" })
        assertEquals(2, einstellungen.size)
    }

    @Test
    fun leitetDenAblageOrtAusDemGeltungsbereichAb() {
        val einstellungen = DokuParser.leseEinstellungen(einstellungenMarkdown)
        assertEquals(
            "managed-settings.json",
            einstellungen.first { it.name == "permissions.deny" }.art,
        )
    }

    @Test
    fun maskierteTrennstricheVerschiebenKeineSpalten() {
        val markdown = """
            | Command | Purpose |
            | :------ | :------ |
            | `/voice [hold\|tap\|off]` | Toggle voice dictation, or enable a specific mode |
        """.trimIndent()
        val gelesen = DokuParser.leseSlashBefehle(markdown)
        assertEquals(1, gelesen.size)
        // Ohne Rücksicht auf das maskierte `\|` stand hier früher „off]" als Beschreibung.
        assertEquals("/voice", gelesen.first().name)
        assertEquals("Toggle voice dictation, or enable a specific mode", gelesen.first().beschreibung)
    }

    @Test
    fun tabelleOhnePassendenKopfLiefertNichts() {
        val markdown = """
            | Rule | What it matches |
            | :--- | :-------------- |
            | `Bash(git:*)` | Any git command |
        """.trimIndent()
        assertTrue(DokuParser.leseSlashBefehle(markdown).isEmpty())
        assertTrue(DokuParser.leseEinstellungen(markdown).isEmpty())
        assertTrue(DokuParser.leseVariablen(markdown).isEmpty())
    }

    @Test
    fun findetDieNeuesteVersion() {
        assertEquals("2.1.251", DokuParser.leseNeuesteVersion(changelog))
    }

    @Test
    fun findetDenEinzugMitBeleg() {
        val (version, beleg) = DokuParser.findeEinzug(changelog, "/effort", istSlash = true)
        assertEquals("2.1.76", version)
        assertTrue(beleg.contains("Added"))
    }

    @Test
    fun nimmtAeltesteErwaehnungWennKeinAddedDasteht() {
        val (version, _) = DokuParser.findeEinzug(changelog, "/compact", istSlash = true)
        assertEquals("2.0.31", version)
    }

    @Test
    fun unbekannterNameLiefertLeer() {
        val (version, beleg) = DokuParser.findeEinzug(changelog, "/gibtesnicht", istSlash = true)
        assertEquals("", version)
        assertEquals("", beleg)
    }

    @Test
    fun leereUnterlageLiefertNichts() {
        assertTrue(DokuParser.leseSlashBefehle("").isEmpty())
        assertTrue(DokuParser.leseEinstellungen("Kein Text mit Tabelle.").isEmpty())
    }
}

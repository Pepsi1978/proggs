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

    private val einstellungenMarkdown = """
        | Key | Description |
        | :-- | :---------- |
        | `autoCompactEnabled` | Turn automatic compaction off or on |
        | `permissions.deny` | Block listed tool uses |
        | `ANTHROPIC_API_KEY` | API key sent as header |
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

        val variablen = DokuParser.leseVariablen(einstellungenMarkdown)
        assertTrue(variablen.any { it.name == "ANTHROPIC_API_KEY" })
        assertFalse(variablen.any { it.name == "autoCompactEnabled" })
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

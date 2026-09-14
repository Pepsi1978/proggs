package de.frank.ocodekompass

import de.frank.kompass.update.DokuParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft das Lesen der Einstellungen aus dem JSON-Schema von `opencode.json`.
 *
 * Der Ausschnitt ist dem echten Schema nachgebaut: eine Wurzel, die auf einen Baustein
 * verweist, Bausteine, die aufeinander zeigen, ein Baustein, der auf sich selbst zeigt, und
 * Felder mit freiem Namen. Jede dieser Formen kommt im Original vor.
 */
class SchemaLeserTest {

    private val schema = """
        {
          "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
          "${'$'}ref": "#/${'$'}defs/Config",
          "${'$'}defs": {
            "ServerConfig": {
              "type": "object",
              "properties": {
                "port": { "type": "integer", "description": "Port to listen on" },
                "hostname": { "type": "string" }
              }
            },
            "AgentConfig": {
              "type": "object",
              "properties": {
                "model": { "type": "string", "description": "Model for this agent" },
                "sub": { "${'$'}ref": "#/${'$'}defs/AgentConfig" }
              }
            },
            "Config": {
              "type": "object",
              "properties": {
                "${'$'}schema": { "type": "string", "description": "Schema reference" },
                "shell": { "type": "string", "description": "Default shell" },
                "logLevel": { "type": "string", "enum": ["debug", "info", "warn"] },
                "server": {
                  "${'$'}ref": "#/${'$'}defs/ServerConfig",
                  "description": "Server configuration"
                },
                "agent": {
                  "type": "object",
                  "additionalProperties": { "${'$'}ref": "#/${'$'}defs/AgentConfig" }
                }
              }
            }
          }
        }
    """.trimIndent()

    private val namen get() = DokuParser.leseEinstellungen(schema).map { it.name }

    @Test
    fun `verschachtelte Schluessel bekommen ihren Pfad`() {
        assertTrue("server fehlt", "server" in namen)
        assertTrue("server.port fehlt", "server.port" in namen)
        assertTrue("shell fehlt", "shell" in namen)
    }

    @Test
    fun `ein freier Name steht als Platzhalter im Pfad`() {
        // `agent` erlaubt beliebige Namen; im Pfad steht dafuer <name>, wie es auch die
        // Referenz von Codex schreibt.
        assertTrue("agent.<name>.model fehlt: $namen", "agent.<name>.model" in namen)
    }

    @Test
    fun `der Schema-Verweis ist keine Einstellung`() {
        assertFalse("\$schema gehoert nicht in die Liste", namen.any { it.endsWith("schema") })
    }

    @Test
    fun `ein Baustein der auf sich selbst zeigt bringt die Auswertung nicht zum Kreisen`() {
        // Ohne Tiefengrenze liefe AgentConfig -> sub -> AgentConfig endlos.
        assertTrue("Auswertung lieferte nichts", namen.isNotEmpty())
        assertFalse("zu tief gelesen", namen.any { it.count { z -> z == '.' } > 2 })
    }

    @Test
    fun `ein Schluessel ohne Beschreibung bekommt einen ehrlichen Hinweis`() {
        val eintrag = DokuParser.leseEinstellungen(schema).first { it.name == "server.hostname" }
        // Nichts erfinden: Dort steht, wohin der Schluessel gehoert und dass die Quelle
        // an dieser Stelle schweigt.
        assertTrue(
            "Der Hinweis fehlt: ${eintrag.beschreibung}",
            eintrag.beschreibung.contains("`server`") &&
                eintrag.beschreibung.contains("keinen erklärenden Satz"),
        )
    }

    @Test
    fun `der Typ steht mit im Text`() {
        val eintrag = DokuParser.leseEinstellungen(schema).first { it.name == "server.port" }
        assertTrue("Typ fehlt: ${eintrag.beschreibung}", eintrag.beschreibung.contains("integer"))
    }

    @Test
    fun `die Datei steht als Art am Eintrag`() {
        assertEquals("opencode.json", DokuParser.leseEinstellungen(schema).first().art)
    }

    @Test
    fun `kaputtes JSON liefert nichts statt Unsinn`() {
        // Der Aufrufer erkennt das an der Untergrenze und bricht ab, statt den Bestand zu leeren.
        assertTrue(DokuParser.leseEinstellungen("kein JSON").isEmpty())
    }
}

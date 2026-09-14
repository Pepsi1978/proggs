package de.frank.codexkompass

import de.frank.kompass.update.DokuParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft das Lesen der Konfigurations-Referenz.
 *
 * Die Ausschnitte stammen wörtlich von der Seite. Sie stehen hier, weil die Referenz ihre
 * Schlüssel nicht in einer Tabelle führt, sondern in einem JavaScript-Objektliteral — und
 * dort zwei Eigenheiten hat, an denen eine naive Auswertung scheitert: gemischte
 * Anführungszeichen und Beschreibungen, die erst in der nächsten Zeile beginnen.
 */
class ConfigReferenzTest {

    private val referenz = """
        # Configuration Reference

        ## `config.toml`

        <ConfigTable
          options={[
            {
              key: "model",
              type: "string",
              description: "Model to use (e.g., `gpt-5.5`).",
            },
            {
              key: "model_auto_compact_token_limit",
              type: "number",
              description:
                "Token threshold that triggers automatic history compaction (unset uses model defaults).",
            },
            {
              key: "tools.web_search",
              type: 'boolean | { context_size = "low|medium|high" }',
              description:
                "Optional web search tool configuration.",
            },
          ]}
        />

        ## `requirements.toml`

        <ConfigTable
          options={[
            {
              key: "rules.prefix_rules[].decision",
              type: "prompt | forbidden",
              description: "Required. Requirements rules can only prompt or forbid.",
            },
          ]}
        />
    """.trimIndent()

    @Test
    fun `alle Schluessel werden gelesen`() {
        val gelesen = DokuParser.leseEinstellungen(referenz)
        assertEquals(4, gelesen.size)
    }

    @Test
    fun `ein Typ mit Anfuehrungszeichen darin geht nicht verloren`() {
        val gelesen = DokuParser.leseEinstellungen(referenz)
        // Steht ein " im Typ, weicht die Seite auf einfache Anfuehrungszeichen aus. Wer nur
        // doppelte liest, verliert genau die Schluessel mit den interessantesten Typen.
        val treffer = gelesen.firstOrNull { it.name == "tools.web_search" }
        assertTrue("tools.web_search fehlt", treffer != null)
        assertTrue(
            "Der Typ gehoert mit in den Text",
            treffer!!.beschreibung.contains("context_size"),
        )
    }

    @Test
    fun `eine Beschreibung in der naechsten Zeile wird mitgenommen`() {
        val gelesen = DokuParser.leseEinstellungen(referenz)
        val treffer = gelesen.first { it.name == "model_auto_compact_token_limit" }
        assertTrue(
            "Der umgebrochene Text fehlt",
            treffer.beschreibung.startsWith("Token threshold"),
        )
    }

    @Test
    fun `die Datei steht als Art am Eintrag`() {
        val gelesen = DokuParser.leseEinstellungen(referenz).associateBy { it.name }
        assertEquals("config.toml", gelesen.getValue("model").art)
        assertEquals(
            "requirements.toml",
            gelesen.getValue("rules.prefix_rules[].decision").art,
        )
    }

    @Test
    fun `eine umgebaute Seite liefert nichts statt Unsinn`() {
        // Der Aufrufer erkennt das an der Untergrenze und bricht ab, statt den Bestand zu leeren.
        assertTrue(DokuParser.leseEinstellungen("# Nur Fliesstext, keine Tabelle").isEmpty())
    }
}

package de.frank.stacklabor.werftstudio.service.codex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CodexJsonTest {
    private val supplements = setOf("nem-1", "nem-2")
    private val goals = setOf("ziel-1")
    private val questions = setOf("frage-1")

    @Test
    fun `parst gueltige duenn besetzte Antwort`() {
        val result = CodexJson.parse(validJson(), supplements, goals, questions)

        assertEquals(1, result.cells.size)
        assertEquals(CellEffect.SUPPORTS, result.cells.single().effect)
        assertEquals(1, result.competitions.size)
        assertEquals("Auswertungstext", result.narrative)
        assertEquals("Antwort", result.answers.single().text)
    }

    @Test
    fun `nimmt eine zielfreie Auswertung ohne Zellen an`() {
        // So sieht die Antwort aus, wenn fuer den Stack keine Ziele festgelegt sind.
        val raw = """
            {"zellen":[],
             "konkurrenzen":[{"nem_a":"nem-1","nem_b":"nem-2","art":"aufnahme","schwere":2,"grund":"Beide binden Eisen."}],
             "antworten":[{"frage":"frage-1","text":"Antwort"}],
             "gesamt":"Die Zusammenstellung wurde ohne Ziele geprueft.",
             "hinweise":[]}
        """.trimIndent()

        val result = CodexJson.parse(raw, supplements, goalIds = emptySet(), questionIds = questions)

        assertTrue(result.cells.isEmpty())
        assertEquals(1, result.competitions.size)
        assertEquals("Die Zusammenstellung wurde ohne Ziele geprueft.", result.narrative)
    }

    @Test
    fun `weist Zellen ab wenn gar keine Ziele festgelegt sind`() {
        assertFailsWith<InvalidCodexJsonException> {
            CodexJson.parse(validJson(), supplements, goalIds = emptySet(), questionIds = questions)
        }
    }

    @Test
    fun `weist unbekannte Felder strikt ab`() {
        val raw = validJson().replace("\"hinweise\":[]", "\"hinweise\":[],\"extra\":true")

        val error = assertFailsWith<InvalidCodexJsonException> {
            CodexJson.parse(raw, supplements, goals, questions)
        }

        assertTrue(error.message.orEmpty().contains("falsche Felder"))
    }

    @Test
    fun `weist unbekannte IDs und ungueltige Staerken ab`() {
        assertFailsWith<InvalidCodexJsonException> {
            CodexJson.parse(validJson().replace("ziel-1", "ziel-fremd"), supplements, goals, questions)
        }
        assertFailsWith<InvalidCodexJsonException> {
            CodexJson.parse(validJson().replace("\"staerke\":2", "\"staerke\":4"), supplements, goals, questions)
        }
    }

    @Test
    fun `weist doppelte Bewertungszellen ab`() {
        val cell = "{\"nem\":\"nem-1\",\"ziel\":\"ziel-1\",\"wirkung\":\"stuetzt\"," +
            "\"staerke\":2,\"grund\":\"Grund\"}"
        val raw = validJson().replace("\"zellen\":[$cell]", "\"zellen\":[$cell,$cell]")

        assertFailsWith<InvalidCodexJsonException> {
            CodexJson.parse(raw, supplements, goals, questions)
        }
    }

    @Test
    fun `dekodiert wachsenden gesamt String mit Escapes`() {
        assertEquals("Erste\nZe", CodexJson.bestEffortNarrative("{\"gesamt\":\"Erste\\nZe"))
        assertEquals("Erste\nZeile", CodexJson.bestEffortNarrative("{\"gesamt\":\"Erste\\nZeile\"}"))
        assertEquals("Grün", CodexJson.bestEffortNarrative("{\"gesamt\":\"Gr\\u00fcn\"}"))
    }

    private fun validJson(): String {
        val cell = "{\"nem\":\"nem-1\",\"ziel\":\"ziel-1\",\"wirkung\":\"stuetzt\"," +
            "\"staerke\":2,\"grund\":\"Grund\"}"
        return "{\"zellen\":[$cell]," +
            "\"konkurrenzen\":[{\"nem_a\":\"nem-1\",\"nem_b\":\"nem-2\"," +
            "\"art\":\"aufnahme\",\"schwere\":2,\"grund\":\"Abstand halten\"}]," +
            "\"antworten\":[{\"frage\":\"frage-1\",\"text\":\"Antwort\"}]," +
            "\"gesamt\":\"Auswertungstext\",\"hinweise\":[]}"
    }
}

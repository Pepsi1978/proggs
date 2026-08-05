package de.frank.cortex.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regressionstest zu Franks Fund 2026-08-05: Im Editier-Fenster stand die Ueberschrift
 * "Vorgesehener Eintrag" statt des vorgesehenen Eintrags.
 */
class MemoryEntryTextTest {

    private val reply = """
        Vorgesehener Eintrag

        Kategorie: „Geräte“
        Titel: „Galaxy Fold 8 als Haupthandy“

        Eintrag:
        Ab sofort benutze ich das Galaxy Fold 8 als mein Haupthandy.

        Soll ich ihn so ablegen?
    """.trimIndent()

    @Test
    fun `serverfeld hat vorrang`() {
        assertEquals(
            "Ab sofort benutze ich das Galaxy Fold 8 als mein Haupthandy.",
            editableEntryText("Ab sofort benutze ich das Galaxy Fold 8 als mein Haupthandy.", reply)
        )
    }

    @Test
    fun `ohne serverfeld wird der eintrag aus der antwort geschnitten`() {
        assertEquals(
            "Ab sofort benutze ich das Galaxy Fold 8 als mein Haupthandy.",
            editableEntryText(null, reply)
        )
    }

    @Test
    fun `blosse ueberschrift landet nie im editor`() {
        assertEquals(
            "Ab sofort benutze ich das Galaxy Fold 8 als mein Haupthandy.",
            editableEntryText("Vorgesehener Eintrag", reply)
        )
    }

    @Test
    fun `wortwoertliche uebernahme wird ebenso erkannt`() {
        val verbatim = """
            Vorgesehener Eintrag

            Kategorie: „Notizen“
            Titel: „Einkauf“

            Wortwörtlicher Text:
            Milch, Brot und Butter kaufen.

            Soll ich ihn so ablegen?
        """.trimIndent()
        assertEquals("Milch, Brot und Butter kaufen.", editableEntryText("", verbatim))
    }

    @Test
    fun `dublettenrueckfrage schneidet nur den eintrag heraus`() {
        val withDuplicates = """
            Vorgesehener Eintrag

            Kategorie: „Geräte“
            Titel: „Galaxy Fold 8 als Haupthandy“

            Eintrag:
            Ab sofort benutze ich das Galaxy Fold 8 als mein Haupthandy.

            Es gibt schon ähnliche Einträge in deinem Gedächtnis:

            1. „Galaxy Fold 8 als Haupthandy“ (Geräte, Ähnlichkeit 83 %)
        """.trimIndent()
        assertEquals(
            "Ab sofort benutze ich das Galaxy Fold 8 als mein Haupthandy.",
            editableEntryText(null, withDuplicates)
        )
    }

    @Test
    fun `ohne erkennbaren eintrag bleibt die antwort erhalten`() {
        assertEquals("Alles klar.", editableEntryText(null, "Alles klar."))
    }
}

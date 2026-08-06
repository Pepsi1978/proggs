package de.frank.perfectmoment.backup

import de.frank.perfectmoment.data.local.SkillEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillTextExportTest {

    private fun skill(id: Long, name: String, text: String) =
        SkillEntity(id = id, name = name, text = text, createdAt = 0L)

    @Test
    fun jederSkillBekommtEineNummerierteUeberschrift() {
        val text = SkillTextExport.buildText(
            listOf(
                skill(1, "Forscherteam", "Erste Zeile"),
                skill(2, "Annahmen", "Zweite Zeile"),
            ),
        )

        assertTrue(text.contains("SKILL 1: Forscherteam"))
        assertTrue(text.contains("SKILL 2: Annahmen"))
    }

    @Test
    fun derOriginaltextBleibtUnveraendert() {
        val original = "Zeile eins\n\n  eingerückt\nZeile drei"

        val text = SkillTextExport.buildText(listOf(skill(1, "Test", original)))

        assertTrue(text.contains(original))
    }

    @Test
    fun ohneSkillsBleibtDerTextLeer() {
        assertEquals("", SkillTextExport.buildText(emptyList()))
    }
}

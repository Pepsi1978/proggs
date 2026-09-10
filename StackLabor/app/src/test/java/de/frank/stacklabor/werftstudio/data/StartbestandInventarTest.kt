package de.frank.stacklabor.werftstudio.data

import de.frank.stacklabor.werftstudio.data.transfer.StackLaborJson
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StartbestandInventarTest {
    private val dokument by lazy {
        val asset = listOf(
            File("src/main/assets/startbestand.json"),
            File("app/src/main/assets/startbestand.json"),
        ).firstOrNull(File::isFile)
        assertNotNull(asset, "startbestand.json wurde nicht gefunden.")
        StackLaborJson().dekodieren(asset.readText(Charsets.UTF_8))
    }

    @Test
    fun `Startbestand hat exakt das spezifizierte Inventar`() {
        assertEquals(StackLaborJson.SEED_FORMAT, dokument.format)
        assertEquals(6, dokument.stacks.size)
        assertEquals(72, dokument.eintraege.size)
        assertEquals(63, dokument.mittel.size)
        assertEquals(
            mapOf("morning1" to 16, "morning2" to 19, "presport" to 10, "evening1" to 8, "evening2" to 18, "evening3" to 1),
            dokument.eintraege.groupingBy { it.stackId }.eachCount(),
        )
    }

    @Test
    fun `Jeder Eintrag hat eine freie Dosis und nur Venlafaxin eine Dienst-Dosis`() {
        val freieDosen = dokument.dosen.filter { it.variante == "FREI" }
        val dienstDosen = dokument.dosen.filter { it.variante == "DIENST" }
        assertEquals(dokument.eintraege.map { it.id }.toSet(), freieDosen.map { it.stackEintragId }.toSet())
        assertEquals(listOf("morning1-04"), dienstDosen.map { it.stackEintragId })
        assertEquals("75", dienstDosen.single().mengeJeStueck)
    }

    @Test
    fun `Mehrfachvorkommen verwenden stabile Mittel-IDs`() {
        val eintraegeNachMittel = dokument.eintraege.groupBy { it.mittelId }
        assertEquals(2, eintraegeNachMittel.getValue("magnesium-bisglycinat").size)
        assertEquals(2, eintraegeNachMittel.getValue("kollagen").size)
        assertEquals(2, eintraegeNachMittel.getValue("vitamin-c").size)
        assertTrue(dokument.wirkstoffkomponenten.any { it.mittelId == "eisen-bisglycinat" && it.name == "Vitamin C" })
        assertTrue(dokument.wirkstoffkomponenten.count { it.mittelId == "vitamin-d3-k2" } == 2)
    }
}

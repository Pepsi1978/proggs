package de.frank.entropyreducer.domain.usecase

import com.google.common.truth.Truth.assertThat
import de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity
import org.junit.Test

class PriorityMemoryLogicTest {

    private fun mem(id: String, title: String, prio: Double = 50.0, src: String? = null) =
        PriorityMemoryEntity(
            id = id,
            title = title,
            description = "Beschreibung $title",
            priority = prio,
            createdAt = 0L,
            updatedAt = 0L,
            sourceEntryId = src,
        )

    @Test
    fun `Format enthaelt Titel Beschreibung und Prioritaet`() {
        val out = formatPriorityMemoriesForPrompt(listOf(mem("1", "Laufen", 80.0)))
        assertThat(out).contains("Laufen")
        assertThat(out).contains("Beschreibung Laufen")
        assertThat(out).contains("80")
    }

    @Test
    fun `Leere Liste ergibt leeren String`() {
        assertThat(formatPriorityMemoriesForPrompt(emptyList())).isEmpty()
    }

    @Test
    fun `Dedup findet Eintrag per sourceEntryId zuerst`() {
        val list = listOf(mem("a", "X", src = "entry-1"), mem("b", "X", src = "entry-2"))
        val hit = selectMemoryToUpdate(list, sourceEntryId = "entry-2", title = "X")
        assertThat(hit?.id).isEqualTo("b")
    }

    @Test
    fun `Dedup faellt auf gleichen Titel zurueck wenn keine sourceId passt`() {
        val list = listOf(mem("a", "  Laufen  ", src = "entry-1"))
        val hit = selectMemoryToUpdate(list, sourceEntryId = "entry-99", title = "laufen")
        assertThat(hit?.id).isEqualTo("a")
    }

    @Test
    fun `Dedup gibt null wenn nichts passt`() {
        val list = listOf(mem("a", "Laufen", src = "entry-1"))
        assertThat(selectMemoryToUpdate(list, "entry-99", "Voellig anderes")).isNull()
    }
}

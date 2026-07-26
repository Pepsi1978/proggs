package de.frank.perfectmoment.ui

import de.frank.perfectmoment.data.local.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HistorySortingTest {
    @Test
    fun `zuletzt abgespielte Sitzung bleibt bei jeder Sortierung oben`() {
        HistorySort.entries.forEach { sort ->
            assertEquals(3L, sortHistorySessions(sessions(), sort).first().id)
        }
    }

    @Test
    fun `Standard sortiert Rest nach Nutzungshaeufigkeit`() {
        assertEquals(
            listOf(3L, 2L, 1L, 4L),
            sortHistorySessions(sessions(), HistorySort.MOST_USED).map(SessionEntity::id),
        )
    }

    @Test
    fun `weitere Kriterien sortieren den nicht angehefteten Rest`() {
        assertEquals(listOf(3L, 4L, 2L, 1L), sortedIds(HistorySort.NEWEST))
        assertEquals(listOf(3L, 1L, 2L, 4L), sortedIds(HistorySort.OLDEST))
        assertEquals(listOf(3L, 2L, 1L, 4L), sortedIds(HistorySort.A_TO_Z))
    }

    private fun sortedIds(sort: HistorySort) =
        sortHistorySessions(sessions(), sort).map(SessionEntity::id)

    private fun sessions() = listOf(
        session(id = 1, topic = "Balance", startedAt = 100, playCount = 2, lastPlayedAt = 100),
        session(id = 2, topic = "Ankommen", startedAt = 200, playCount = 5, lastPlayedAt = 300),
        session(id = 3, topic = "Ruhe", startedAt = 300, playCount = 1, lastPlayedAt = 900),
        session(id = 4, topic = "Zuversicht", startedAt = 400, playCount = 1, lastPlayedAt = 400),
    )

    private fun session(
        id: Long,
        topic: String,
        startedAt: Long,
        playCount: Int,
        lastPlayedAt: Long,
    ) = SessionEntity(
        id = id,
        topic = topic,
        startedAt = startedAt,
        durationMin = 10,
        voiceName = "voice",
        providerId = "edge",
        pauseRep = 1,
        pauseNext = 1,
        reps = 1,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
    )
}

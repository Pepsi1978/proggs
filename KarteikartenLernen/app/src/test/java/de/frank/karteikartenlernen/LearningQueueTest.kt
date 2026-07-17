package de.frank.karteikartenlernen

import de.frank.karteikartenlernen.model.advanceLearningQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningQueueTest {
    @Test
    fun unknownCardIsAppendedForRepetition() {
        val result = advanceLearningQueue(listOf(0, 1), position = 0, known = false)

        assertEquals(listOf(0, 1, 0), result.queue)
        assertEquals(1, result.nextPosition)
        assertFalse(result.done)
    }

    @Test
    fun finalKnownCardCompletesRound() {
        val result = advanceLearningQueue(listOf(0, 1), position = 1, known = true)

        assertEquals(2, result.nextPosition)
        assertTrue(result.done)
    }
}

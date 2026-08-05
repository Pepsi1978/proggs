package de.frank.perfectmoment.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShuffleUpcomingTest {

    @Test
    fun `bereits vorgelesene Fragen bleiben vor dem Fortsetzungspunkt liegen`() {
        val questions = List(20) { question(it + 1) }

        val mixed = shuffleUpcoming(questions, fromIndex = 8)

        assertEquals(questions.take(8), mixed.take(8))
    }

    @Test
    fun `jede offene Frage kommt genau einmal vor`() {
        val questions = List(20) { question(it + 1) }

        val upcoming = shuffleUpcoming(questions, fromIndex = 8).drop(8)

        assertEquals(questions.drop(8).toSet(), upcoming.toSet())
        assertEquals(upcoming.size, upcoming.distinct().size)
    }

    @Test
    fun `keine vorgelesene Frage rutscht in die Zukunft`() {
        val questions = List(20) { question(it + 1) }

        val upcoming = shuffleUpcoming(questions, fromIndex = 8).drop(8)

        assertTrue(upcoming.none { it in questions.take(8) })
    }

    @Test
    fun `mischt bei mehreren Laeufen tatsaechlich`() {
        val questions = List(30) { question(it + 1) }

        val orders = List(20) { shuffleUpcoming(questions, fromIndex = 5).map(Question::text) }

        assertTrue(orders.distinct().size > 1)
    }

    @Test
    fun `ein Punkt hinter der letzten Frage laesst die Liste unveraendert`() {
        val questions = List(5) { question(it + 1) }

        assertEquals(questions, shuffleUpcoming(questions, fromIndex = questions.size))
        assertEquals(questions, shuffleUpcoming(questions, fromIndex = 99))
    }

    @Test
    fun `ohne Fortschritt wird die ganze Liste gemischt`() {
        val questions = List(20) { question(it + 1) }

        val mixed = shuffleUpcoming(questions, fromIndex = 0)

        assertEquals(questions.toSet(), mixed.toSet())
        assertEquals(questions.size, mixed.size)
    }

    private fun question(number: Int) = Question(emoji = "✨", text = "Frage $number")
}

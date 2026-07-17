package de.frank.karteikartenlernen

import de.frank.karteikartenlernen.auth.ExistingSessionContext
import de.frank.karteikartenlernen.auth.GeneratedCard
import org.junit.Assert.assertEquals
import org.junit.Test

class CrossSuggestionTest {
    @Test
    fun suggestionsContainOnlyCardsAssignedByTheModel() {
        val sessions = listOf(
            ExistingSessionContext("bio", "Biologie", "", ""),
            ExistingSessionContext("chemie", "Chemie", "", ""),
            ExistingSessionContext("geschichte", "Geschichte", "", ""),
        )
        val cards = listOf(
            GeneratedCard("Zelle?", "Baustein", "Erklärung", listOf("bio")),
            GeneratedCard("Atom?", "Teilchen", "Erklärung", listOf("chemie")),
            GeneratedCard("DNA?", "Erbgut", "Erklärung", listOf("bio", "chemie")),
            GeneratedCard("Datum?", "Heute", "Erklärung"),
        )

        val suggestions = buildCrossSuggestions(cards, listOf(11L, 12L, 13L, 14L), sessions)

        assertEquals(listOf("bio", "chemie"), suggestions.map { it.sessionId })
        assertEquals(listOf(11L, 13L), suggestions[0].cardIds)
        assertEquals(listOf(12L, 13L), suggestions[1].cardIds)
    }

    @Test
    fun noSemanticMatchesCreateNoSuggestion() {
        val sessions = listOf(ExistingSessionContext("bio", "Biologie", "", ""))
        val cards = listOf(GeneratedCard("Geschichte?", "Antwort", "Erklärung"))

        assertEquals(emptyList<Any>(), buildCrossSuggestions(cards, listOf(1L), sessions))
    }
}

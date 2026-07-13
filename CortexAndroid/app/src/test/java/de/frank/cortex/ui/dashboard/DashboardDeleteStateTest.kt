package de.frank.cortex.ui.dashboard

import de.frank.cortex.data.model.BrainEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardDeleteStateTest {
    @Test
    fun `deleted entry is removed from every visible dashboard state`() {
        val deleted = entry("deleted")
        val retained = entry("retained")
        val state = DashboardUiState(
            selectedEntry = deleted,
            isEditing = true,
            browseResults = listOf(deleted, retained),
            searchResults = listOf(retained, deleted),
            error = "old error"
        )

        val result = state.withoutEntry(deleted.doc_id)

        assertNull(result.selectedEntry)
        assertFalse(result.isEditing)
        assertNull(result.error)
        assertEquals(listOf(retained), result.browseResults)
        assertEquals(listOf(retained), result.searchResults)
    }

    private fun entry(id: String) = BrainEntry(
        doc_id = id,
        title = id,
        category = "Programmierung/Sessions",
        text = "text",
        created_at = null,
        updated_at = null
    )
}

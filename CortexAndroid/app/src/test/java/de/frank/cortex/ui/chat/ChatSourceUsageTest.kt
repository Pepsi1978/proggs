package de.frank.cortex.ui.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatSourceUsageTest {
    @Test
    fun formatsEverySupportedSourceCombination() {
        assertEquals("Gedächtnis", formatSourceUsage(listOf("memory")))
        assertEquals("Internet", formatSourceUsage(listOf("internet")))
        assertEquals("Gedächtnis + Internet", formatSourceUsage(listOf("internet", "memory")))
        assertEquals("Ohne Suche", formatSourceUsage(emptyList()))
    }

    @Test
    fun doesNotGuessMissingOrUnknownMetadata() {
        assertNull(formatSourceUsage(null))
        assertNull(formatSourceUsage(listOf("future-source")))
    }
}

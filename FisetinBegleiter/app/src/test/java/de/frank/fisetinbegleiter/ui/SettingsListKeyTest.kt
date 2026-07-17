package de.frank.fisetinbegleiter.ui

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsListKeyTest {
    @Test
    fun `same database id remains unique across settings lists`() {
        assertNotEquals(ingredientListKey(7), stackItemListKey(7))
    }

    @Test
    fun `keys remain stable for recomposition`() {
        assertEquals("ingredient:7", ingredientListKey(7))
        assertEquals("stack:7", stackItemListKey(7))
    }
}

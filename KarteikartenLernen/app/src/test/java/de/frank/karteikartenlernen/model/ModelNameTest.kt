package de.frank.karteikartenlernen.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelNameTest {
    @Test
    fun legacySoulNameIsMigratedToOfficialSolName() {
        assertEquals("GPT 5.6 Sol", normalizeModelName("GPT 5.6 Soul"))
        assertEquals("GPT 5.6 Terra", normalizeModelName("GPT 5.6 Terra"))
    }
}

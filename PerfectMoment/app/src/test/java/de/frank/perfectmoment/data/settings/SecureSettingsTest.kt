package de.frank.perfectmoment.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SecureSettingsTest {
    @Test
    fun `Pausen werden auf gerade Werte zwischen zwei und sechzig normalisiert`() {
        mapOf(
            -1 to 2,
            1 to 2,
            2 to 2,
            3 to 4,
            4 to 4,
            59 to 60,
            60 to 60,
            61 to 60,
        ).forEach { (input, expected) ->
            assertEquals(expected, normalizePauseSeconds(input))
        }
    }
}

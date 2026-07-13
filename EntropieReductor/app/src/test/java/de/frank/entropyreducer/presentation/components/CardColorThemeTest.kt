package de.frank.entropyreducer.presentation.components

import com.google.common.truth.Truth.assertThat
import de.frank.entropyreducer.data.repository.cardColorIdForTheme
import de.frank.entropyreducer.data.repository.cardColorIndexForTheme
import org.junit.Test

class CardColorThemeTest {

    @Test
    fun `light and dark colors use independent stored keys`() {
        val colors = mapOf("hrv" to 3, "dark/hrv" to 7)

        assertThat(cardColorIdForTheme("hrv", isDark = false)).isEqualTo("hrv")
        assertThat(cardColorIdForTheme("hrv", isDark = true)).isEqualTo("dark/hrv")
        assertThat(cardColorIndexForTheme(colors, "hrv", isDark = false)).isEqualTo(3)
        assertThat(cardColorIndexForTheme(colors, "hrv", isDark = true)).isEqualTo(7)
    }

    @Test
    fun `both themes offer the same number of colors and index zero remains reset`() {
        assertThat(LIGHT_CARD_COLORS).hasSize(30)
        assertThat(DARK_CARD_COLORS).hasSize(30)
        assertThat(cardColorOverrideForIndex(0, isDark = false)).isNull()
        assertThat(cardColorOverrideForIndex(0, isDark = true)).isNull()
    }
}

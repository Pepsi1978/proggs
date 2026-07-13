package de.frank.entropyreducer.presentation.widget

import com.google.common.truth.Truth.assertThat
import de.frank.entropyreducer.data.settings.ThemeMode
import org.junit.Test

class WidgetThemeRefreshPolicyTest {

    @Test
    fun `system theme refresh applies only to system-themed widgets`() {
        assertThat(shouldRefreshWidgetForSystemTheme(ThemeMode.SYSTEM)).isTrue()
        assertThat(shouldRefreshWidgetForSystemTheme(ThemeMode.LIGHT)).isFalse()
        assertThat(shouldRefreshWidgetForSystemTheme(ThemeMode.DARK)).isFalse()
    }

    @Test
    fun `system theme refresh skips an already rendered night mode`() {
        assertThat(shouldRefreshWidgetForNightMode(ThemeMode.SYSTEM, null, 1)).isTrue()
        assertThat(shouldRefreshWidgetForNightMode(ThemeMode.SYSTEM, 1, 1)).isFalse()
        assertThat(shouldRefreshWidgetForNightMode(ThemeMode.SYSTEM, 1, 2)).isTrue()
        assertThat(shouldRefreshWidgetForNightMode(ThemeMode.LIGHT, null, 1)).isFalse()
    }
}

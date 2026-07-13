package de.frank.entropyreducer.presentation.widget

import com.google.common.truth.Truth.assertThat
import de.frank.entropyreducer.data.settings.ThemeMode
import org.junit.Test

class WidgetThemeRefreshPolicyTest {

    @Test
    fun `configuration changes refresh only system-themed widgets`() {
        assertThat(shouldRefreshWidgetForConfigurationChange(ThemeMode.SYSTEM)).isTrue()
        assertThat(shouldRefreshWidgetForConfigurationChange(ThemeMode.LIGHT)).isFalse()
        assertThat(shouldRefreshWidgetForConfigurationChange(ThemeMode.DARK)).isFalse()
    }
}

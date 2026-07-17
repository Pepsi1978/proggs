package de.frank.fisetinbegleiter.ui

import de.frank.fisetinbegleiter.data.ProtocolTemplateEntity
import de.frank.fisetinbegleiter.ui.theme.AppThemeMode
import de.frank.fisetinbegleiter.ui.theme.AppThemeVariant
import org.junit.Assert.assertEquals
import org.junit.Test

class DesignStateTest {
    @Test
    fun unknownThemeValuesUseDesignDefaults() {
        assertEquals(AppThemeVariant.EMBER, AppThemeVariant.fromPersistenceValue("unknown"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromPersistenceValue(null))
    }

    @Test
    fun persistedThemeValuesAreCaseInsensitiveAndTrimmed() {
        assertEquals(AppThemeVariant.VITAL, AppThemeVariant.fromPersistenceValue(" Vital "))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromPersistenceValue("DARK"))
    }

    @Test
    fun protocolValuesAreClampedToDesignBounds() {
        val validated = ProtocolTemplateEntity(
            standardDurationDays = 9,
            antioxidantBlockMinutes = 30,
            mealDeadlineMinutes = 500,
            mealWarningMinutes = -5,
            spermidinStartMinutes = 10,
            spermidinReminderMinutes = 500,
        ).validatedForDesign()

        assertEquals(3, validated.standardDurationDays)
        assertEquals(60, validated.antioxidantBlockMinutes)
        assertEquals(120, validated.mealDeadlineMinutes)
        assertEquals(0, validated.mealWarningMinutes)
        assertEquals(30, validated.spermidinStartMinutes)
        assertEquals(240, validated.spermidinReminderMinutes)
    }
}

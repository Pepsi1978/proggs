package de.frank.fisetinbegleiter.ui

import de.frank.fisetinbegleiter.data.ProtocolTemplateEntity
import de.frank.fisetinbegleiter.data.CureEntity
import de.frank.fisetinbegleiter.data.CureStatus
import de.frank.fisetinbegleiter.ui.theme.AppThemeMode
import de.frank.fisetinbegleiter.ui.theme.AppThemeVariant
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

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

    @Test
    fun oneDayProtocolRemainsValid() {
        val validated = ProtocolTemplateEntity(standardDurationDays = 1).validatedForDesign()

        assertEquals(1, validated.standardDurationDays)
    }

    @Test
    fun plannedFutureCureShowsDateAndTimeInsteadOfToday() {
        val today = LocalDate.of(2026, 7, 19)
        val cure = plannedCure(today.plusDays(90), 9 * 60 + 30)

        assertEquals("Geplante Kur am 17.10.2026 um 09:30 Uhr", todaySectionTitle(cure, today))
    }

    @Test
    fun curePlannedForTodayKeepsTodayHeading() {
        val today = LocalDate.of(2026, 7, 19)

        assertEquals("Heute", todaySectionTitle(plannedCure(today, 480), today))
    }

    @Test
    fun activeCureKeepsTodayHeadingRegardlessOfOriginalStartDate() {
        val today = LocalDate.of(2026, 7, 19)
        val activeCure = plannedCure(today.minusDays(1), 480).copy(status = CureStatus.AKTIV)

        assertEquals("Heute", todaySectionTitle(activeCure, today))
    }

    private fun plannedCure(date: LocalDate, startMinute: Int) = CureEntity(
        startEpochDay = date.toEpochDay(),
        plannedDurationDays = 2,
        status = CureStatus.GEPLANT,
        carrierLiquid = "Wasser",
        fatCarrier = "MCT-Öl",
        preferredStartMinuteOfDay = startMinute,
    )
}

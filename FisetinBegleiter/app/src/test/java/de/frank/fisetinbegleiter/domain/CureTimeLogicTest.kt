package de.frank.fisetinbegleiter.domain

import de.frank.fisetinbegleiter.data.AppCureState
import de.frank.fisetinbegleiter.data.CureDayEntity
import de.frank.fisetinbegleiter.data.CureEntity
import de.frank.fisetinbegleiter.data.CureStatus
import de.frank.fisetinbegleiter.data.CureWithDays
import de.frank.fisetinbegleiter.data.ProtocolTemplateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CureTimeLogicTest {
    private val protocol = ProtocolTemplateEntity()
    private val t0 = 1_000_000L

    @Test
    fun `timeline uses exact protocol offsets`() {
        val result = timeline(t0, protocol)

        assertEquals(t0 + 30 * 60_000L, result.mealDeadline)
        assertEquals(t0 + 180 * 60_000L, result.spermidinStart)
        assertEquals(t0 + 240 * 60_000L, result.spermidinEnd)
        assertEquals(t0 + 240 * 60_000L, result.blockEnd)
    }

    @Test
    fun `state changes exactly at antioxidant window end`() {
        val cure = activeCure(day(t0 = t0))
        val end = timeline(t0, protocol).blockEnd

        assertEquals(AppCureState.SPERRFENSTER_AKTIV, cureState(cure, protocol, end - 1))
        assertEquals(AppCureState.SPERRFENSTER_VORBEI, cureState(cure, protocol, end))
    }

    @Test
    fun `day only completes after manual steps and time window`() {
        val completeSteps = day(t0 = t0).copy(mealDoneAt = t0 + 1, spermidinDoneAt = t0 + 2)
        val end = timeline(t0, protocol).blockEnd

        assertFalse(canComplete(completeSteps, end - 1, protocol))
        assertTrue(canComplete(completeSteps, end, protocol))
        assertFalse(canComplete(completeSteps.copy(spermidinDoneAt = null), end, protocol))
    }

    @Test
    fun `next unfinished day is selected`() {
        val first = day(dayNumber = 1).copy(completedAt = t0)
        val second = day(dayNumber = 2)

        assertEquals(2, currentDay(activeCure(first, second))?.dayNumber)
    }

    private fun day(dayNumber: Int = 1, t0: Long? = null) = CureDayEntity(
        id = dayNumber.toLong(),
        cureId = 1,
        dayNumber = dayNumber,
        plannedEpochDay = dayNumber.toLong(),
        t0 = t0,
        drinkDoneAt = t0,
    )

    private fun activeCure(vararg days: CureDayEntity) = CureWithDays(
        CureEntity(
            id = 1,
            startEpochDay = 1,
            plannedDurationDays = 3,
            status = CureStatus.AKTIV,
            carrierLiquid = "Wasser",
            fatCarrier = "MCT-Öl",
            preferredStartMinuteOfDay = 480,
        ),
        days.toList(),
    )
}

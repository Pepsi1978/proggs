package de.frank.wecker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneId

class ClockChangeTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private val newYork = ZoneId.of("America/New_York")
    private fun at(text: String) = OffsetDateTime.parse(text).toInstant().toEpochMilli()

    private val daily = Alarm(id = "d", hour = 7, minute = 0, days = (1..7).toSet(), enabled = true,
        nextAt = at("2026-09-19T07:00:00+02:00"), sleepMinutes = 480)

    @Test fun zoneChangeMapsWallClockToTheNewZone() {
        val now = at("2026-09-18T20:00:00-04:00")
        val updated = AlarmClaim.recomputeNextAt(daily, now, newYork)
        assertEquals(at("2026-09-19T07:00:00-04:00"), updated.nextAt)
        // The sleep reminder follows the corrected wake time: 07:00 − 8 h − 15 min New York time.
        assertEquals(at("2026-09-18T22:45:00-04:00"), SchlafPlan.next(updated, now, true, newYork)!!.trigger(SchlafPlan.leadMs(SchlafPlan.LEAD_DEFAULT_MINUTES)))
    }

    @Test fun missingNextAtIsStillRecomputedWithoutClockChange() {
        // Regression guard: the helper has no nextAt guard, so restoreOne's "nextAt == 0" path keeps working.
        val updated = AlarmClaim.recomputeNextAt(daily.copy(nextAt = 0), at("2026-09-18T20:00:00+02:00"), berlin)
        assertEquals(at("2026-09-19T07:00:00+02:00"), updated.nextAt)
    }

    @Test fun disabledAlarmAndSnoozeSlotStayUntouched() {
        val off = daily.copy(enabled = false)
        assertSame(off, AlarmClaim.recomputeNextAt(off, at("2026-09-18T20:00:00-04:00"), newYork))
        val snoozing = daily.copy(snoozeUntil = at("2026-09-18T20:05:00+02:00"))
        assertEquals(snoozing.snoozeUntil, AlarmClaim.recomputeNextAt(snoozing, at("2026-09-18T20:00:00-04:00"), newYork).snoozeUntil)
    }

    @Test fun passedOneOffDateIsDisabledAsBefore() {
        val once = Alarm(id = "o", hour = 7, minute = 0, startDate = "2026-09-18", enabled = true, nextAt = at("2026-09-18T07:00:00+02:00"))
        val updated = AlarmClaim.recomputeNextAt(once, at("2026-09-18T08:00:00-04:00"), newYork)
        assertFalse(updated.enabled)
        assertEquals(0L, updated.nextAt)
    }
}

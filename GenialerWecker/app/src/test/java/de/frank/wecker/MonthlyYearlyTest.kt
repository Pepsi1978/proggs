package de.frank.wecker

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class MonthlyYearlyTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private fun at(text: String) = OffsetDateTime.parse(text).toInstant().toEpochMilli()
    private fun instant(text: String) = OffsetDateTime.parse(text).toInstant()
    private fun monthly(start: String, every: Int = 1, hour: Int = 7, minute: Int = 0) =
        Alarm(id = "m", hour = hour, minute = minute, startDate = start, repeatUnit = Alarm.MONTHLY, repeatEvery = every, enabled = true)
    private fun yearly(start: String, every: Int = 1) =
        Alarm(id = "y", hour = 7, minute = 0, startDate = start, repeatUnit = Alarm.YEARLY, repeatEvery = every, enabled = true)

    @Test fun monthlyFromTheTwentyThirdKeepsTheDay() {
        val alarm = monthly("2026-09-23")
        assertEquals(at("2026-09-23T07:00:00+02:00"), AlarmTime.next(alarm, instant("2026-09-17T20:00:00+02:00"), berlin))
        assertEquals(at("2026-10-23T07:00:00+02:00"), AlarmTime.next(alarm, instant("2026-09-23T07:00:01+02:00"), berlin))
        assertTrue(alarm.repeats)
        assertFalse(alarm.isExpiredOnce(instant("2027-01-01T00:00:00+01:00")))
    }

    @Test fun shortMonthsUseTheLastDayAndTheAnchorReturns() {
        val alarm = monthly("2027-01-31")
        assertEquals(at("2027-02-28T07:00:00+01:00"), AlarmTime.next(alarm, instant("2027-02-01T00:00:00+01:00"), berlin))
        assertEquals(at("2027-03-31T07:00:00+02:00"), AlarmTime.next(alarm, instant("2027-03-01T00:00:00+01:00"), berlin))
        // Leap year: the 29th instead of the 28th.
        assertEquals(at("2028-02-29T07:00:00+01:00"), AlarmTime.next(monthly("2028-01-31"), instant("2028-02-01T00:00:00+01:00"), berlin))
    }

    @Test fun everyThreeMonthsStaysAnchoredWithoutDrift() {
        val alarm = monthly("2026-11-30", every = 3)
        assertEquals(at("2027-02-28T07:00:00+01:00"), AlarmTime.next(alarm, instant("2026-12-01T00:00:00+01:00"), berlin))
        assertEquals(at("2027-05-30T07:00:00+02:00"), AlarmTime.next(alarm, instant("2027-03-01T00:00:00+01:00"), berlin))
    }

    @Test fun monthEndAndYearEndRollOver() {
        assertEquals(at("2027-01-31T07:00:00+01:00"), AlarmTime.next(monthly("2026-12-31"), instant("2027-01-01T12:00:00+01:00"), berlin))
    }

    @Test fun yearlyOnTheTwentyNinthOfFebruary() {
        val alarm = yearly("2028-02-29")
        assertEquals(at("2029-02-28T07:00:00+01:00"), AlarmTime.next(alarm, instant("2028-03-01T00:00:00+01:00"), berlin))
        assertEquals(at("2032-02-29T07:00:00+01:00"), AlarmTime.next(alarm, instant("2031-03-01T00:00:00+01:00"), berlin))
        assertEquals(at("2030-02-28T07:00:00+01:00"), AlarmTime.next(yearly("2026-02-28", every = 2), instant("2028-03-01T00:00:00+01:00"), berlin))
    }

    @Test fun aStartDateInTheFutureIsNeverUndercut() {
        val alarm = monthly("2027-06-15")
        assertEquals(at("2027-06-15T07:00:00+02:00"), AlarmTime.next(alarm, instant("2026-09-17T20:00:00+02:00"), berlin))
        assertEquals(at("2027-06-15T07:00:00+02:00"), AlarmTime.next(yearly("2027-06-15"), instant("2026-09-17T20:00:00+02:00"), berlin))
    }

    @Test fun claimAndSkipWorkWithMonthly() {
        val alarm = monthly("2026-09-23").copy(nextAt = at("2026-09-23T07:00:00+02:00"))
        val accepted = AlarmClaim.decide(alarm, false, alarm.nextAt, emptyList(), alarm.nextAt + 500, berlin) as AlarmClaim.Accepted
        assertEquals(at("2026-10-23T07:00:00+02:00"), accepted.next!!.nextAt)
        assertTrue(accepted.next!!.enabled)
        // Skip and undo.
        val marked = alarm.copy(skippedThrough = "2026-09-23")
        assertEquals(at("2026-10-23T07:00:00+02:00"), AlarmTime.nextRespectingSkip(marked, instant("2026-09-20T12:00:00+02:00"), berlin))
        assertTrue(AlarmTime.isSkipping(marked.copy(nextAt = at("2026-10-23T07:00:00+02:00")), instant("2026-09-20T12:00:00+02:00"), berlin))
        assertEquals(at("2026-09-23T07:00:00+02:00"), AlarmTime.next(marked.copy(skippedThrough = ""), instant("2026-09-20T12:00:00+02:00"), berlin))
    }

    @Test fun claimPlansTheNextYearlyOccurrence() {
        val alarm = yearly("2028-02-29").copy(nextAt = at("2028-02-29T07:00:00+01:00"))
        val accepted = AlarmClaim.decide(alarm, false, alarm.nextAt, emptyList(), alarm.nextAt + 500, berlin) as AlarmClaim.Accepted
        assertEquals(at("2029-02-28T07:00:00+01:00"), accepted.next!!.nextAt)
        assertTrue(accepted.next!!.enabled)
        val everyTwo = yearly("2026-06-15", every = 2).copy(nextAt = at("2026-06-15T07:00:00+02:00"))
        val next = AlarmClaim.decide(everyTwo, false, everyTwo.nextAt, emptyList(), everyTwo.nextAt + 500, berlin) as AlarmClaim.Accepted
        assertEquals(at("2028-06-15T07:00:00+02:00"), next.next!!.nextAt)
    }

    @Test fun sleepReminderWorksForMonthly() {
        val alarm = monthly("2026-09-23").copy(nextAt = at("2026-09-23T07:00:00+02:00"), sleepMinutes = 480)
        val plan = SchlafPlan.next(alarm, at("2026-09-22T12:00:00+02:00"), true, berlin)!!
        assertEquals(at("2026-09-22T23:00:00+02:00"), plan.bedtime)
        assertEquals(at("2026-09-22T22:45:00+02:00"), plan.trigger(SchlafPlan.leadMs(SchlafPlan.LEAD_DEFAULT_MINUTES)))
    }

    @Test fun jsonStaysCompatibleAndConflictsAreRejected() {
        val alarm = monthly("2026-09-23", every = 3)
        assertEquals(alarm, Alarm.from(JSONObject(alarm.json().toString())))
        // Older entry without the new fields keeps its former meaning.
        val legacy = Alarm(id = "l", hour = 7, days = setOf(1, 2), enabled = true)
        val loaded = Alarm.from(legacy.json().apply { remove("repeatUnit"); remove("repeatEvery") })
        assertEquals("", loaded.repeatUnit)
        assertEquals(0, loaded.repeatEvery)
        assertEquals(legacy, loaded)
        // Invalid entries are rejected instead of being reinterpreted; AlarmStore then keeps the raw backup and skips them.
        assertThrowsIllegal { Alarm.from(alarm.json().put("repeatUnit", "week")) }
        assertThrowsIllegal { Alarm.from(alarm.json().put("startDate", "")) }
        assertThrowsIllegal { Alarm.from(alarm.json().put("startDate", "kaputt")) }
        assertThrowsIllegal { Alarm.from(alarm.json().put("repeatEvery", 13)) }
        assertThrowsIllegal { Alarm.from(alarm.json().put("intervalDays", 5)) }
        assertThrowsIllegal { Alarm.from(legacy.json().put("repeatEvery", 2)) }
        alarm.validate()
        // Conflicting combinations cannot be saved.
        assertThrowsIllegal { alarm.copy(days = setOf(1)).validate() }
        assertThrowsIllegal { alarm.copy(intervalDays = 5).validate() }
        assertThrowsIllegal { alarm.copy(startDate = "").validate() }
        assertThrowsIllegal { alarm.copy(repeatEvery = 13).validate() }
        assertThrowsIllegal { yearly("2026-09-23", every = 6).validate() }
        assertThrowsIllegal { Alarm(id = "x", repeatEvery = 2).validate() }
    }

    private fun assertThrowsIllegal(block: () -> Unit) {
        val failed = runCatching { block() }.isFailure
        assertTrue("Erwartete Ablehnung", failed)
    }
}

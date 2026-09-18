package de.frank.wecker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Deutsche Sommer-/Winterzeit: 02:30 in der Frühjahrslücke → 03:30 MESZ, doppelte Herbst-02:30 → nur die erste (MESZ). */
class DstBerlinTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private val reference = OffsetDateTime.parse("2026-09-17T00:00:00+02:00").toInstant()
    private val autumn = berlin.rules.nextTransition(reference)
    private val spring = berlin.rules.nextTransition(autumn.instant)
    private val autumnDay: LocalDate = autumn.dateTimeBefore.toLocalDate()
    private val springDay: LocalDate = spring.dateTimeBefore.toLocalDate()
    private val cest = ZoneOffset.ofHours(2)
    private val cet = ZoneOffset.ofHours(1)

    private fun ms(day: LocalDate, time: String, offset: ZoneOffset) = OffsetDateTime.of(day, LocalTime.parse(time), offset).toInstant().toEpochMilli()
    private fun daily(hour: Int, minute: Int, nextAt: Long = 0) = Alarm(id = "d", hour = hour, minute = minute, days = (1..7).toSet(), enabled = true, nextAt = nextAt)

    @Test fun transitionsComeFromZoneRules() {
        assertTrue(autumn.isOverlap)
        assertTrue(spring.isGap)
        assertEquals(LocalDate.parse("2026-10-25"), autumnDay)
        assertEquals(LocalDate.parse("2027-03-28"), springDay)
    }

    @Test fun springDailyAtTwoThirtyRingsAtThreeThirtySummerTimeAndThenNormally() {
        val alarm = daily(2, 30)
        val first = AlarmTime.next(alarm, Instant.ofEpochMilli(ms(springDay.minusDays(1), "20:00", cet)), berlin)
        assertEquals(ms(springDay, "03:30", cest), first)
        val accepted = AlarmClaim.decide(alarm.copy(nextAt = first), false, first, emptyList(), first + 500, berlin) as AlarmClaim.Accepted
        assertEquals(ms(springDay.plusDays(1), "02:30", cest), accepted.next!!.nextAt)
    }

    @Test fun autumnDailyAtTwoThirtyRingsOnceAtTheFirstOccurrence() {
        val alarm = daily(2, 30)
        val first = AlarmTime.next(alarm, Instant.ofEpochMilli(ms(autumnDay.minusDays(1), "20:00", cest)), berlin)
        assertEquals(ms(autumnDay, "02:30", cest), first)
        // Claim: next occurrence is the following day, never the second 02:30 (CET) of the same day.
        val accepted = AlarmClaim.decide(alarm.copy(nextAt = first), false, first, emptyList(), first + 500, berlin) as AlarmClaim.Accepted
        assertEquals(ms(autumnDay.plusDays(1), "02:30", cet), accepted.next!!.nextAt)
        // Duplicate broadcast of the same occurrence is rejected.
        assertSame(AlarmClaim.Rejected, AlarmClaim.decide(accepted.next, false, first, listOf(accepted.entry), first + 900, berlin))
        // During the repeated hour (02:10 CET) no new ringing for this day: planning and late settling both go to the next day.
        val duringSecondHour = ms(autumnDay, "02:10", cet)
        assertEquals(ms(autumnDay.plusDays(1), "02:30", cet), AlarmTime.next(alarm, Instant.ofEpochMilli(duringSecondHour), berlin))
        val (settled, _) = AlarmClaim.settle(alarm.copy(nextAt = first), duringSecondHour, berlin)
        assertEquals(ms(autumnDay.plusDays(1), "02:30", cet), settled.nextAt)
    }

    @Test fun weekdayAndDatedOneOffFollowTheSameRule() {
        val sunday = Alarm(id = "w", hour = 2, minute = 30, days = setOf(7), enabled = true)
        assertEquals(ms(autumnDay, "02:30", cest), AlarmTime.next(sunday, Instant.ofEpochMilli(ms(autumnDay.minusDays(1), "20:00", cest)), berlin))
        assertEquals(ms(springDay, "03:30", cest), AlarmTime.next(sunday, Instant.ofEpochMilli(ms(springDay.minusDays(1), "20:00", cet)), berlin))
        val onceAutumn = Alarm(id = "o", hour = 2, minute = 30, startDate = autumnDay.toString(), enabled = true)
        assertEquals(ms(autumnDay, "02:30", cest), AlarmTime.next(onceAutumn, reference, berlin))
        val onceSpring = Alarm(id = "p", hour = 2, minute = 30, startDate = springDay.toString(), enabled = true)
        assertEquals(ms(springDay, "03:30", cest), AlarmTime.next(onceSpring, reference, berlin))
    }

    @Test fun intervalRhythmKeepsLocalTimeAcrossBothTransitions() {
        // Every 7 days from 18.10.2026: both transition Sundays are regular occurrences.
        val seven = Alarm(id = "s", hour = 7, minute = 0, startDate = "2026-10-18", intervalDays = 7, enabled = true)
        // 07:00 winter time = 06:00 UTC, 07:00 summer time = 05:00 UTC.
        assertEquals(Instant.parse("2026-10-25T06:00:00Z").toEpochMilli(), AlarmTime.next(seven, Instant.ofEpochMilli(ms(autumnDay.minusDays(1), "12:00", cest)), berlin))
        assertEquals(Instant.parse("2027-03-28T05:00:00Z").toEpochMilli(), AlarmTime.next(seven, Instant.ofEpochMilli(ms(springDay.minusDays(1), "12:00", cet)), berlin))
        val night = seven.copy(hour = 2, minute = 30)
        assertEquals(ms(autumnDay, "02:30", cest), AlarmTime.next(night, Instant.ofEpochMilli(ms(autumnDay.minusDays(1), "12:00", cest)), berlin))
        assertEquals(ms(springDay, "03:30", cest), AlarmTime.next(night, Instant.ofEpochMilli(ms(springDay.minusDays(1), "12:00", cet)), berlin))
    }

    @Test fun sleepDurationIsRealTimeAndReminderFifteenMinutesBefore() {
        // Spring: 07:00 CEST (05:00 UTC) − 8 h = 22:00 CET the evening before, reminder 21:45 CET.
        val springWake = ms(springDay, "07:00", cest)
        assertEquals(Instant.parse("2027-03-28T05:00:00Z").toEpochMilli(), springWake)
        val springPlan = SchlafPlan.next(daily(7, 0, springWake).copy(sleepMinutes = 480), ms(springDay.minusDays(1), "12:00", cet), true, berlin)!!
        assertEquals(ms(springDay.minusDays(1), "22:00", cet), springPlan.bedtime)
        assertEquals(ms(springDay.minusDays(1), "21:45", cet), springPlan.trigger(SchlafPlan.leadMs(SchlafPlan.LEAD_DEFAULT_MINUTES)))
        // Autumn: 07:00 CET (06:00 UTC) − 8 h = 00:00 CEST, reminder 23:45 CEST the evening before.
        val autumnWake = ms(autumnDay, "07:00", cet)
        assertEquals(Instant.parse("2026-10-25T06:00:00Z").toEpochMilli(), autumnWake)
        val autumnPlan = SchlafPlan.next(daily(7, 0, autumnWake).copy(sleepMinutes = 480), ms(autumnDay.minusDays(1), "12:00", cest), true, berlin)!!
        assertEquals(ms(autumnDay, "00:00", cest), autumnPlan.bedtime)
        assertEquals(ms(autumnDay.minusDays(1), "23:45", cest), autumnPlan.trigger(SchlafPlan.leadMs(SchlafPlan.LEAD_DEFAULT_MINUTES)))
    }
}

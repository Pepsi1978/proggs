package de.frank.wecker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneId

class SchlafErinnerungTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private fun at(text: String) = OffsetDateTime.parse(text).toInstant().toEpochMilli()
    private fun daily(nextAt: String, sleep: Int, id: String = "a") =
        Alarm(id = id, hour = 7, minute = 0, days = (1..7).toSet(), enabled = true, nextAt = at(nextAt), sleepMinutes = sleep)

    @Test fun reminderIsFifteenMinutesBeforeBedtime() {
        val plan = SchlafPlan.next(daily("2026-09-19T07:00:00+02:00", 480), at("2026-09-18T20:00:00+02:00"), true, berlin)!!
        assertEquals(at("2026-09-18T22:45:00+02:00"), plan.trigger)
    }

    @Test fun missedOneOffReminderIsSkipped() {
        val once = Alarm(id = "o", hour = 7, minute = 0, enabled = true, nextAt = at("2026-09-19T07:00:00+02:00"), sleepMinutes = 480)
        assertNull(SchlafPlan.next(once, at("2026-09-18T23:00:00+02:00"), true, berlin))
    }

    @Test fun offDisabledOrWithoutSleepHasNoPlan() {
        val now = at("2026-09-18T12:00:00+02:00")
        assertNull(SchlafPlan.next(daily("2026-09-19T07:00:00+02:00", 480), now, false, berlin))
        assertNull(SchlafPlan.next(daily("2026-09-19T07:00:00+02:00", 480).copy(enabled = false), now, true, berlin))
        assertNull(SchlafPlan.next(daily("2026-09-19T07:00:00+02:00", 0), now, true, berlin))
    }

    @Test fun twentyFourHourSleepPlansTheFollowingOccurrenceBeforeTodaysRinging() {
        // Daily 07:00, 24 h sleep: tomorrow's reminder (today 06:45) lies before today's ringing at 07:00.
        val plan = SchlafPlan.next(daily("2026-09-18T07:00:00+02:00", 1440), at("2026-09-18T05:00:00+02:00"), true, berlin)!!
        assertEquals(at("2026-09-19T07:00:00+02:00"), plan.wakeAt)
        assertEquals(at("2026-09-18T06:45:00+02:00"), plan.trigger)
        // And the delivery of that occurrence is valid although it is not nextAt.
        assertNotNull(SchlafPlan.gueltig(daily("2026-09-18T07:00:00+02:00", 1440), plan.wakeAt, 1440, at("2026-09-18T06:46:00+02:00"), true, berlin))
    }

    @Test fun staleOrLateDeliveriesAreDropped() {
        val alarm = daily("2026-09-19T07:00:00+02:00", 480)
        val wake = at("2026-09-19T07:00:00+02:00")
        assertNotNull(SchlafPlan.gueltig(alarm, wake, 480, at("2026-09-18T22:45:00+02:00"), true, berlin))
        // Sleep duration changed meanwhile.
        assertNull(SchlafPlan.gueltig(alarm.copy(sleepMinutes = 540), wake, 480, at("2026-09-18T22:45:00+02:00"), true, berlin))
        // Late after bedtime, switch off, alarm disabled or deleted.
        assertNull(SchlafPlan.gueltig(alarm, wake, 480, at("2026-09-18T23:01:00+02:00"), true, berlin))
        assertNull(SchlafPlan.gueltig(alarm, wake, 480, at("2026-09-18T22:50:00+02:00"), false, berlin))
        assertNull(SchlafPlan.gueltig(alarm.copy(enabled = false), wake, 480, at("2026-09-18T22:50:00+02:00"), true, berlin))
        assertNull(SchlafPlan.gueltig(null, wake, 480, at("2026-09-18T22:50:00+02:00"), true, berlin))
        // Wake time no longer part of the plan (e.g. time changed).
        assertNull(SchlafPlan.gueltig(alarm, at("2026-09-19T06:00:00+02:00"), 480, at("2026-09-18T21:50:00+02:00"), true, berlin))
    }

    @Test fun deliveryWindowStartsExactlyAtTheReminder() {
        val alarm = daily("2026-09-19T07:00:00+02:00", 480)
        val wake = at("2026-09-19T07:00:00+02:00")
        assertNull(SchlafPlan.gueltig(alarm, wake, 480, at("2026-09-18T22:44:59+02:00"), true, berlin))
        assertNotNull(SchlafPlan.gueltig(alarm, wake, 480, at("2026-09-18T22:45:00+02:00"), true, berlin))
        assertNotNull(SchlafPlan.gueltig(alarm, wake, 480, at("2026-09-18T23:00:00+02:00"), true, berlin))
    }

    @Test fun groupTextKeepsEachAlarmsOwnSleepDuration() {
        val now = at("2026-09-18T22:45:00+02:00")
        val a = daily("2026-09-19T07:00:00+02:00", 480, id = "a").copy(name = "Früh")
        val b = Alarm(id = "b", name = "Spät", hour = 8, minute = 0, days = (1..7).toSet(), enabled = true, nextAt = at("2026-09-19T08:00:00+02:00"), sleepMinutes = 540)
        val va = SchlafPlan.Vorkommen("a", a.nextAt, 480)
        val vb = SchlafPlan.Vorkommen("b", b.nextAt, 540)
        assertEquals(va.bedtime, vb.bedtime)
        val text = SchlafPlan.text(va.bedtime, now, listOf(SchlafPlan.Eintrag(b.name, vb.wakeAt, 540), SchlafPlan.Eintrag(a.name, va.wakeAt, 480)), berlin)
        assertEquals("Schlafenszeit ≈ heute 23:00 · „Früh“ Sa. 07:00 (8 Std.) und „Spät“ Sa. 08:00 (9 Std.)", text)
        assertEquals("Schlafenszeit ≈ heute 23:00 · 8 Std. Schlaf bis „Früh“ Sa. 07:00",
            SchlafPlan.text(va.bedtime, now, listOf(SchlafPlan.Eintrag(a.name, va.wakeAt, 480)), berlin))
        // Membership for the visible group: both alarms; a disabled one drops out.
        assertNotNull(SchlafPlan.inGruppe(a, va.group, now, true, berlin))
        assertNotNull(SchlafPlan.inGruppe(b, va.group, now, true, berlin))
        assertNull(SchlafPlan.inGruppe(b.copy(enabled = false), va.group, now, true, berlin))
        assertNull(SchlafPlan.inGruppe(a, va.group, now, false, berlin))
    }

    @Test fun sameBedtimeSharesOneGroupAndDoubleDeliveryIsMarked() {
        val a = SchlafPlan.Vorkommen("a", at("2026-09-19T07:00:00+02:00"), 480)
        val b = SchlafPlan.Vorkommen("b", at("2026-09-19T08:00:00+02:00"), 540)
        assertEquals(a.group, b.group)
        var marks = SchlafPlan.Marken(emptyMap())
        assertFalse(marks.gruppeGemeldet(a))
        marks = SchlafPlan.Marken.parse(marks.mit(a).json())
        assertTrue(marks.enthaelt(a))
        assertTrue(marks.gruppeGemeldet(b))
        assertFalse(marks.enthaelt(b))
        assertTrue(marks.bereinigt(at("2026-09-25T00:00:00+02:00")).gruppen.isEmpty())
    }

    @Test fun clockChangeKeepsRealDuration() {
        // 25.10.2026 fall back: 07:00 CET minus 8 h minus 15 min = 23:45 CEST the evening before.
        val alarm = daily("2026-10-25T07:00:00+01:00", 480)
        val plan = SchlafPlan.next(alarm, at("2026-10-24T20:00:00+02:00"), true, berlin)!!
        assertEquals(at("2026-10-24T23:45:00+02:00"), plan.trigger)
    }
}

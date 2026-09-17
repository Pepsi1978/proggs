package de.frank.wecker

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class SkipMarkTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private val newYork = ZoneId.of("America/New_York")
    private fun at(text: String) = OffsetDateTime.parse(text).toInstant().toEpochMilli()
    private fun instant(text: String) = OffsetDateTime.parse(text).toInstant()
    private val daily = Alarm(id = "d", hour = 7, minute = 0, days = (1..7).toSet(), enabled = true)

    /** Same steps as AlarmScheduler.skipNext, on a pure alarm. */
    private fun skip(alarm: Alarm, now: Long, zone: ZoneId): Alarm {
        val marked = alarm.copy(skippedThrough = AlarmTime.localDate(alarm.nextAt, zone).toString())
        return marked.copy(nextAt = AlarmTime.nextRespectingSkip(marked, Instant.ofEpochMilli(maxOf(alarm.nextAt, now)), zone))
    }

    @Test fun skipSurvivesZoneChange() {
        val now = at("2026-09-18T20:00:00+02:00")
        val skipped = skip(daily.copy(nextAt = at("2026-09-19T07:00:00+02:00")), now, berlin)
        assertEquals("2026-09-19", skipped.skippedThrough)
        assertEquals(at("2026-09-20T07:00:00+02:00"), skipped.nextAt)
        // Zone change to New York: still the day after the skipped date, now 07:00 local.
        val moved = AlarmClaim.recomputeNextAt(skipped, at("2026-09-18T14:00:00-04:00"), newYork)
        assertEquals(at("2026-09-20T07:00:00-04:00"), moved.nextAt)
        assertTrue(AlarmTime.isSkipping(moved, Instant.ofEpochMilli(at("2026-09-18T14:00:00-04:00")), newYork))
    }

    @Test fun midnightAlarmOnTheDayAfterTheMarkIsKept() {
        val midnight = Alarm(id = "m", hour = 0, minute = 0, days = (1..7).toSet(), enabled = true, skippedThrough = "2026-09-19")
        assertEquals(at("2026-09-20T00:00:00+02:00"), AlarmTime.nextRespectingSkip(midnight, instant("2026-09-18T20:00:00+02:00"), berlin))
    }

    @Test fun autumnMarkAlsoExcludesTheSecondTwoThirty() {
        val night = Alarm(id = "n", hour = 2, minute = 30, days = (1..7).toSet(), enabled = true, skippedThrough = "2026-10-25")
        // Searching from inside the repeated hour must not pick the second 02:30 (CET) of the skipped day.
        assertEquals(at("2026-10-26T02:30:00+01:00"), AlarmTime.nextRespectingSkip(night, instant("2026-10-25T02:10:00+01:00"), berlin))
    }

    @Test fun shiftKeepsItsAnchorWhenAnOccurrenceIsSkipped() {
        val shift = Alarm(id = "s", hour = 20, minute = 0, startDate = "2026-09-13", intervalDays = 35, enabled = true, nextAt = at("2026-10-18T20:00:00+02:00"))
        val skipped = skip(shift, at("2026-10-01T12:00:00+02:00"), berlin)
        assertEquals(at("2026-11-22T20:00:00+01:00"), skipped.nextAt)
    }

    @Test fun repeatedSkipAndUndo() {
        val now = at("2026-09-18T20:00:00+02:00")
        val once = skip(daily.copy(nextAt = at("2026-09-19T07:00:00+02:00")), now, berlin)
        val twice = skip(once, now, berlin)
        assertEquals("2026-09-20", twice.skippedThrough)
        assertEquals(at("2026-09-21T07:00:00+02:00"), twice.nextAt)
        // Undo removes all skips.
        val undone = twice.copy(skippedThrough = "").let { it.copy(nextAt = AlarmTime.next(it, Instant.ofEpochMilli(now), berlin)) }
        assertEquals(at("2026-09-19T07:00:00+02:00"), undone.nextAt)
        assertFalse(AlarmTime.isSkipping(undone, Instant.ofEpochMilli(now), berlin))
    }

    @Test fun clockPastTheMarkAndRingingClearIt() {
        val marked = daily.copy(skippedThrough = "2026-09-19", nextAt = at("2026-09-20T07:00:00+02:00"))
        val later = instant("2026-09-25T12:00:00+02:00")
        assertEquals(at("2026-09-26T07:00:00+02:00"), AlarmTime.nextRespectingSkip(marked, later, berlin))
        assertFalse(AlarmTime.isSkipping(marked.copy(nextAt = at("2026-09-26T07:00:00+02:00")), later, berlin))
        val accepted = AlarmClaim.decide(marked, false, marked.nextAt, emptyList(), marked.nextAt + 500, berlin) as AlarmClaim.Accepted
        assertEquals("", accepted.next!!.skippedThrough)
        assertEquals(at("2026-09-21T07:00:00+02:00"), accepted.next!!.nextAt)
    }

    @Test fun jsonIsBackwardCompatibleAndLegacySkipStaysVisible() {
        val marked = daily.copy(nextAt = at("2026-09-20T07:00:00+02:00"), skippedThrough = "2026-09-19")
        assertEquals(marked, Alarm.from(JSONObject(marked.json().toString())))
        val legacy = marked.json().apply { remove("skippedThrough") }
        val loaded = Alarm.from(legacy)
        assertEquals("", loaded.skippedThrough)
        // Without a mark the former nextAt comparison still reports the old skip.
        assertTrue(AlarmTime.isSkipping(loaded, instant("2026-09-18T20:00:00+02:00"), berlin))
        // An unreadable mark counts as none; a non-repeating alarm ignores a mark.
        assertEquals("", Alarm.from(marked.json().put("skippedThrough", "kaputt")).skippedThrough)
        assertEquals(null, Alarm(id = "o", startDate = "2026-09-19", skippedThrough = "2026-09-19").skipDate)
    }
}

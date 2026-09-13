package de.frank.wecker

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class AlarmTimeTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private fun next(alarm: Alarm, now: String) = Instant.ofEpochMilli(AlarmTime.next(alarm, Instant.parse(now), berlin)).toString()

    @Test fun onceMovesToTomorrowWhenTimePassed() {
        assertEquals("2026-09-14T05:00:00Z", next(Alarm(hour = 7), "2026-09-13T06:00:00Z"))
    }
    @Test fun selectedWeekdaysSkipWeekend() {
        assertEquals("2026-09-14T05:00:00Z", next(Alarm(hour = 7, days = setOf(1, 2, 3, 4, 5)), "2026-09-11T06:00:00Z"))
    }
    @Test fun shiftIntervalStaysAnchoredAcrossClockChange() {
        val shift = Alarm(hour = 20, startDate = "2026-09-13", intervalDays = 35)
        assertEquals("2026-10-18T18:00:00Z", next(shift, "2026-09-13T18:00:01Z"))
        assertEquals("2026-11-22T19:00:00Z", next(shift, "2026-10-18T18:00:01Z"))
        assertEquals("2026-12-27T19:00:00Z", next(shift, "2026-11-23T00:00:00Z"))
    }
    @Test fun missedCyclesDoNotDrift() {
        val shift = Alarm(hour = 7, startDate = "2026-09-01", intervalDays = 5)
        assertEquals("2026-09-16T05:00:00Z", next(shift, "2026-09-13T12:00:00Z"))
    }
    @Test fun futureAnchorDoesNotScheduleEarlier() {
        assertEquals("2026-11-12T06:00:00Z", next(Alarm(hour = 7, startDate = "2026-11-12", intervalDays = 35), "2026-09-13T12:00:00Z"))
    }
    @Test fun explicitDateCanBeSixtyDaysAway() {
        assertEquals("2026-11-12T06:00:00Z", next(Alarm(hour = 7, startDate = "2026-11-12"), "2026-09-13T12:00:00Z"))
    }
    @Test(expected = IllegalArgumentException::class) fun pastDateCannotBeSilentlyRescheduled() {
        next(Alarm(startDate = "2026-09-12"), "2026-09-13T12:00:00Z")
    }
    @Test fun springGapMovesToFirstValidLocalHour() {
        assertEquals("2026-03-29T01:30:00Z", next(Alarm(hour = 2, minute = 30, days = setOf(7)), "2026-03-28T12:00:00Z"))
    }
    @Test fun autumnOverlapDoesNotRingTwice() {
        assertEquals("2026-11-01T01:30:00Z", next(Alarm(hour = 2, minute = 30, days = setOf(7)), "2026-10-25T00:30:01Z"))
    }
    @Test fun alarmRoundTripPreservesShiftPhotoAudioAndSnooze() {
        val alarm = Alarm(intervalDays = 35, startDate = "2026-09-13", steps = listOf(Step.IDEAS, Step.TEXT, Step.MUSIC),
            text = "Äpfel, Öl und Grüße", snoozeUntil = 123456L, photoRequired = true, color = "blue", colorPercent = 40,
            prepared = mapOf("TEXT" to listOf("eins", "zwei")), preparedSpeed = .75f)
        assertEquals(alarm, Alarm.from(alarm.json()))
        alarm.validate()
    }
    @Test(expected = IllegalArgumentException::class) fun photoCannotBeEnabledWithoutACondition() {
        Alarm(photoRequired = true).validate()
    }
    @Test fun longSpeechRetainsUnicodeAndAllWords() {
        val text = (1..400).joinToString(" ") { "Grüße😀 Nummer $it." }
        val chunks = SpeechPreparation.chunks(text)
        assertTrue(chunks.all { it.length <= 900 })
        assertEquals(text, chunks.joinToString(" "))
        assertTrue(chunks.none { it.last().isHighSurrogate() || it.first().isLowSurrogate() })
    }
}

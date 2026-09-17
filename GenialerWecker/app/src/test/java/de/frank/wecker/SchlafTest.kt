package de.frank.wecker

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneId

class SchlafTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private fun at(text: String) = OffsetDateTime.parse(text).toInstant().toEpochMilli()

    @Test fun nineMinusNineHoursIsMidnight() {
        val wake = at("2026-09-19T09:00:00+02:00")
        assertEquals(at("2026-09-19T00:00:00+02:00"), Schlaf.bedtime(wake, 9 * 60))
        assertEquals("Schlafenszeit ≈ morgen 00:00 · 9 Std. Schlaf", Schlaf.hinweis(wake, 540, at("2026-09-18T23:59:00+02:00"), berlin))
    }

    @Test fun acrossMidnightIsLabelledRelativeToToday() {
        val now = at("2026-09-18T20:00:00+02:00")
        assertEquals("Schlafenszeit ≈ heute 22:30 · 8 Std. Schlaf", Schlaf.hinweis(at("2026-09-19T06:30:00+02:00"), 480, now, berlin))
        assertEquals("Schlafenszeit war ≈ heute 19:00 · 8,5 Std. Schlaf", Schlaf.hinweis(at("2026-09-19T03:30:00+02:00"), 510, now, berlin))
        // Several days away: weekday and date, never a bare clock time.
        assertTrue(Schlaf.tagLabel(at("2026-09-25T22:00:00+02:00"), now, berlin).contains("25.09. · 22:00"))
    }

    @Test fun springForwardKeepsRealSleepDuration() {
        // 29.03.2026: 02:00 CET becomes 03:00 CEST. 07:00 CEST minus 8 real hours is 22:00 CET on the evening before.
        val bed = Schlaf.bedtime(at("2026-03-29T07:00:00+02:00"), 480)
        assertEquals(at("2026-03-28T22:00:00+01:00"), bed)
        assertEquals("gestern 22:00", Schlaf.tagLabel(bed, at("2026-03-29T01:00:00+01:00"), berlin))
    }

    @Test fun fallBackKeepsRealSleepDuration() {
        // 25.10.2026: 03:00 CEST becomes 02:00 CET. 07:00 CET minus 8 real hours is 00:00 CEST.
        assertEquals(at("2026-10-25T00:00:00+02:00"), Schlaf.bedtime(at("2026-10-25T07:00:00+01:00"), 480))
    }

    @Test fun durationsLimitsAndPersistence() {
        assertEquals("30 Min.", Schlaf.dauer(30)); assertEquals("1 Std.", Schlaf.dauer(60))
        assertEquals("8,5 Std.", Schlaf.dauer(510)); assertEquals("24 Std.", Schlaf.dauer(1440))
        assertTrue(Schlaf.valid(0)); assertTrue(Schlaf.valid(30)); assertTrue(Schlaf.valid(1440))
        assertFalse(Schlaf.valid(15)); assertFalse(Schlaf.valid(1470)); assertFalse(Schlaf.valid(45))
        val alarm = Alarm(id = "a", sleepMinutes = 510)
        assertEquals(alarm, Alarm.from(JSONObject(alarm.json().toString())))
        // Older stored alarms without the field keep "no sleep duration".
        val old = alarm.json().apply { remove("sleepMinutes") }
        assertEquals(0, Alarm.from(old).sleepMinutes)
        assertEquals(510, alarm.copy(id = "b", name = "Kopie").sleepMinutes)
    }
}

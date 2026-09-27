package de.frank.wecker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneId

class ZeitRingTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private fun at(text: String) = OffsetDateTime.parse(text).toInstant().toEpochMilli()

    @Test fun midnightToNineIsNineHoursWithArcToNineOClock() {
        val g = ZeitRing.berechne(at("2026-09-18T00:00:00.020+02:00"), at("2026-09-18T09:00:00+02:00"), berlin)
        assertEquals(540, g.remainingMinutes)
        assertEquals(-90f, g.nowAngle, 0.01f)
        assertEquals(180f, g.targetAngle, 0.01f)
        assertEquals(270f, g.sweep!!, 0.01f)
        assertNull(g.centerLabel)
    }

    @Test fun elevenPmToOneAmIsTwoHours() {
        val g = ZeitRing.berechne(at("2026-09-18T23:00:00+02:00"), at("2026-09-19T01:00:00+02:00"), berlin)
        assertEquals(120, g.remainingMinutes)
        assertEquals(60f, g.sweep!!, 0.01f)
    }

    @Test fun fivePmToNineTomorrowHasNoArc() {
        val g = ZeitRing.berechne(at("2026-09-18T17:00:00+02:00"), at("2026-09-19T09:00:00+02:00"), berlin)
        assertEquals(960, g.remainingMinutes)
        assertNull(g.sweep)
        assertEquals("> 12 Std.", g.centerLabel)
    }

    @Test fun clockChangeInBetweenHasNoArcButRealDuration() {
        // 25.10.2026: 03:00 CEST becomes 02:00 CET, so 01:00 -> 06:00 local lasts six real hours.
        val g = ZeitRing.berechne(at("2026-10-25T01:00:00+02:00"), at("2026-10-25T06:00:00+01:00"), berlin)
        assertEquals(360, g.remainingMinutes)
        assertNull(g.sweep)
        assertEquals("Umstellung", g.centerLabel)
    }

    @Test fun moreThanOneDayShowsWeekday() {
        val g = ZeitRing.berechne(at("2026-09-18T09:00:00+02:00"), at("2026-09-20T09:00:00+02:00"), berlin)
        assertNull(g.sweep)
        assertNotNull(g.centerLabel)
        assertEquals("So", g.centerLabel)
    }
}

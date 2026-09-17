package de.frank.wecker

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class AlarmClaimTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private fun ms(text: String) = Instant.parse(text).toEpochMilli()
    private val shiftAt = ms("2026-10-18T18:00:00Z")
    private val shift = Alarm(id = "shift", hour = 20, startDate = "2026-09-13", intervalDays = 35, nextAt = shiftAt, snoozes = 2)

    @Test fun acceptedOccurrenceAdvancesIntervalRhythmOnce() {
        val decision = AlarmClaim.decide(shift, false, shiftAt, emptyList(), shiftAt + 500, berlin) as AlarmClaim.Accepted
        assertEquals(ms("2026-11-22T19:00:00Z"), decision.next!!.nextAt)
        assertEquals(0, decision.next!!.snoozes)
        assertEquals(RingEntry("shift", shiftAt, shiftAt + 500), decision.entry)
        // The same broadcast delivered again after the claim is rejected: no double ring, no second advance.
        assertSame(AlarmClaim.Rejected, AlarmClaim.decide(decision.next, false, shiftAt, listOf(decision.entry), shiftAt + 900, berlin))
    }

    @Test fun lateDeliveryKeepsAnchorInsteadOfDrifting() {
        val decision = AlarmClaim.decide(shift, false, shiftAt, emptyList(), shiftAt + 3 * 60 * 60_000L, berlin) as AlarmClaim.Accepted
        assertEquals(ms("2026-11-22T19:00:00Z"), decision.next!!.nextAt)
    }

    @Test fun followUpComputationFailureStillRingsAndDuplicateIsRejected() {
        val broken = shift.copy(startDate = "kaputt")
        val decision = AlarmClaim.decide(broken, false, shiftAt, emptyList(), shiftAt, berlin) as AlarmClaim.Accepted
        assertNull(decision.next)
        assertTrue(decision.error.isNotBlank())
        assertSame(AlarmClaim.Rejected, AlarmClaim.decide(broken, false, shiftAt, listOf(decision.entry), shiftAt + 1, berlin))
    }

    @Test fun onceAlarmIsConsumed() {
        val once = Alarm(id = "once", nextAt = shiftAt)
        val next = (AlarmClaim.decide(once, false, shiftAt, emptyList(), shiftAt, berlin) as AlarmClaim.Accepted).next!!
        assertFalse(next.enabled); assertEquals(0L, next.nextAt)
    }

    @Test fun staleOrDisabledOccurrencesAreRejected() {
        assertSame(AlarmClaim.Rejected, AlarmClaim.decide(shift, false, shiftAt - 1, emptyList(), shiftAt, berlin))
        assertSame(AlarmClaim.Rejected, AlarmClaim.decide(shift.copy(enabled = false), false, shiftAt, emptyList(), shiftAt, berlin))
        assertSame(AlarmClaim.Rejected, AlarmClaim.decide(null, false, shiftAt, emptyList(), shiftAt, berlin))
    }

    @Test fun snoozeOnlyMatchesItsOwnTimeAndKeepsCounterAndRhythm() {
        val snoozeAt = shiftAt + 5 * 60_000L
        val snoozed = shift.copy(nextAt = ms("2026-11-22T19:00:00Z"), snoozeUntil = snoozeAt, snoozes = 1)
        val decision = AlarmClaim.decide(snoozed, true, snoozeAt, emptyList(), snoozeAt, berlin) as AlarmClaim.Accepted
        assertEquals(0L, decision.next!!.snoozeUntil)
        assertEquals(1, decision.next!!.snoozes)
        assertEquals(snoozed.nextAt, decision.next!!.nextAt)
        assertSame(AlarmClaim.Rejected, AlarmClaim.decide(snoozed, true, snoozeAt + 1, emptyList(), snoozeAt, berlin))
        assertSame(AlarmClaim.Rejected, AlarmClaim.decide(snoozed.copy(snoozeUntil = 0), true, snoozeAt, emptyList(), snoozeAt, berlin))
    }

    @Test fun reclaimKeepsOriginalSinceSoAgeIsNeverReset() {
        val refireAt = shiftAt + 60_000
        val ringing = listOf(RingEntry("shift", shiftAt, shiftAt))
        val decision = AlarmClaim.decide(shift.copy(snoozeUntil = refireAt), true, refireAt, ringing, refireAt, berlin) as AlarmClaim.Accepted
        assertEquals(RingEntry("shift", refireAt, shiftAt), decision.entry)
    }

    @Test fun twoAlarmsInTheSameMinuteAreClaimedIndependently() {
        val a = Alarm(id = "a", nextAt = shiftAt); val b = Alarm(id = "b", nextAt = shiftAt)
        val first = AlarmClaim.decide(a, false, shiftAt, emptyList(), shiftAt, berlin) as AlarmClaim.Accepted
        assertTrue(AlarmClaim.decide(b, false, shiftAt, listOf(first.entry), shiftAt, berlin) is AlarmClaim.Accepted)
    }

    @Test fun settleAdvancesUnadvancedOccurrenceWithoutResettingSnoozes() {
        val now = shiftAt + 10 * 60_000L
        val (settled, error) = AlarmClaim.settle(shift, now, berlin)
        assertEquals(ms("2026-11-22T19:00:00Z"), settled.nextAt)
        assertEquals(2, settled.snoozes)
        assertEquals("", error)
        assertEquals(settled, AlarmClaim.settle(settled, now, berlin).first)
        assertFalse(AlarmClaim.settle(Alarm(nextAt = shiftAt), now, berlin).first.enabled)
        val (broken, message) = AlarmClaim.settle(shift.copy(startDate = "kaputt"), now, berlin)
        assertFalse(broken.enabled); assertTrue(message.isNotBlank())
    }

    @Test fun recoveryRefiresRecentAndLegacyButDropsOnlyKnownStale() {
        val now = shiftAt + AlarmClaim.STALE_MS + 1
        assertEquals(AlarmClaim.Recovery.DROP_STALE, AlarmClaim.recover(RingEntry("x", shiftAt, shiftAt), now))
        assertEquals(AlarmClaim.Recovery.REFIRE, AlarmClaim.recover(RingEntry("x", shiftAt, now - 1000), now))
        assertEquals(AlarmClaim.Recovery.REFIRE, AlarmClaim.recover(RingEntry("x"), now))
    }

    @Test fun ringingFormatStaysBackwardCompatible() {
        assertEquals(listOf(RingEntry("a"), RingEntry("b")), RingEntry.parse("a,b,,a", null))
        assertEquals(listOf(RingEntry("a")), RingEntry.parse("a", "kein json"))
        val entries = listOf(RingEntry("a", 10, 20), RingEntry("legacy"))
        assertEquals("a,legacy", RingEntry.ids(entries))
        assertEquals(entries, RingEntry.parse(RingEntry.ids(entries), RingEntry.meta(entries)))
        // Metadata of an id that is no longer ringing is ignored.
        assertEquals(listOf(RingEntry("legacy")), RingEntry.parse("legacy", RingEntry.meta(entries)))
    }
}

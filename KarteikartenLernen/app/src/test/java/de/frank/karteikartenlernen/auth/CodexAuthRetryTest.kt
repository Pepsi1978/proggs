package de.frank.karteikartenlernen.auth

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class CodexAuthRetryTest {
    @Test
    fun dnsRetriesAreBoundedAndBackOff() {
        val delays = CodexAuthManager.DNS_RETRY_DELAYS_MS

        assertArrayEquals(longArrayOf(500L, 1_500L, 3_000L), delays)
        assertTrue(delays.asList().zipWithNext().all { (first, second) -> second > first })
    }

    @Test
    fun devicePollingKeepsTransientResponsesPending() {
        assertEquals(DevicePollAction.PROCESS, devicePollAction(200))
        assertEquals(DevicePollAction.PENDING, devicePollAction(403))
        assertEquals(DevicePollAction.PENDING, devicePollAction(404))
        assertEquals(DevicePollAction.PENDING, devicePollAction(429))
        assertEquals(DevicePollAction.PENDING, devicePollAction(503))
        assertEquals(DevicePollAction.FAIL, devicePollAction(400))
    }

    @Test
    fun devicePollingAcceptsStringIntervalFromOpenAi() {
        assertEquals(5, devicePollInterval("5"))
        assertEquals(3, devicePollInterval("0"))
        assertEquals(5, devicePollInterval(null))
    }
}

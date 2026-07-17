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

    @Test
    fun codexInputUsesRequiredUserItemList() {
        val input = codexInput("Warum ist die HRV nachts höher?")

        assertEquals(1, input.length())
        assertEquals("user", input.getJSONObject(0).getString("role"))
        assertEquals("Warum ist die HRV nachts höher?", input.getJSONObject(0).getString("content"))
    }

    @Test
    fun codexSseCombinesOutputDeltas() {
        val stream = """
            event: response.output_text.delta
            data: {"type":"response.output_text.delta","delta":"{\"title\":\"Test\","}

            data: {"type":"response.output_text.delta","delta":"\"answer\":\"Text\",\"cards\":[]}"}
            data: {"type":"response.completed","response":{}}
            data: [DONE]
        """.trimIndent()

        assertEquals("{\"title\":\"Test\",\"answer\":\"Text\",\"cards\":[]}", parseCodexSse(stream))
    }

    @Test
    fun codexSseFallsBackToCompletedResponse() {
        val stream = """
            data: {"type":"response.completed","response":{"output_text":"fertig"}}
            data: [DONE]
        """.trimIndent()

        assertEquals("fertig", parseCodexSse(stream))
    }
}

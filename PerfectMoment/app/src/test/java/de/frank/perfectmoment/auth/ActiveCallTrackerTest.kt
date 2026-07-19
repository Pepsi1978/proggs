package de.frank.perfectmoment.auth

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveCallTrackerTest {
    private val client = OkHttpClient()

    @Test
    fun cancelStopsTrackedCall() {
        val tracker = ActiveCallTracker()
        val call = newCall()

        tracker.track(call)
        tracker.cancel()

        assertTrue(call.isCanceled())
    }

    @Test
    fun replacementCancelsPreviousCallOnly() {
        val tracker = ActiveCallTracker()
        val previous = newCall()
        val current = newCall()

        tracker.track(previous)
        tracker.track(current)

        assertTrue(previous.isCanceled())
        assertFalse(current.isCanceled())
    }

    @Test
    fun clearingPreviousCallKeepsCurrentCallCancelable() {
        val tracker = ActiveCallTracker()
        val previous = newCall()
        val current = newCall()

        tracker.track(previous)
        tracker.track(current)
        tracker.clear(previous)
        tracker.cancel()

        assertTrue(current.isCanceled())
    }

    private fun newCall() = client.newCall(
        Request.Builder().url("https://example.com/").build(),
    )
}

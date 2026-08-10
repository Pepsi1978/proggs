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
    fun newCallLetsRunningCallContinue() {
        val tracker = ActiveCallTracker()
        val running = newCall()
        val added = newCall()

        tracker.track(running)
        tracker.track(added)

        assertFalse(running.isCanceled())
        assertFalse(added.isCanceled())
    }

    @Test
    fun cancelStopsEveryTrackedCall() {
        val tracker = ActiveCallTracker()
        val first = newCall()
        val second = newCall()

        tracker.track(first)
        tracker.track(second)
        tracker.cancel()

        assertTrue(first.isCanceled())
        assertTrue(second.isCanceled())
    }

    @Test
    fun clearedCallIsNoLongerCancelled() {
        val tracker = ActiveCallTracker()
        val finished = newCall()
        val running = newCall()

        tracker.track(finished)
        tracker.track(running)
        tracker.clear(finished)
        tracker.cancel()

        assertFalse(finished.isCanceled())
        assertTrue(running.isCanceled())
    }

    private fun newCall() = client.newCall(
        Request.Builder().url("https://example.com/").build(),
    )
}

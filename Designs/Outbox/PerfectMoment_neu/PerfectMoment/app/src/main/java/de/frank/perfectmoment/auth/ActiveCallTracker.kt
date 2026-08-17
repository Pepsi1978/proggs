package de.frank.perfectmoment.auth

import java.util.concurrent.atomic.AtomicReference
import okhttp3.Call

internal class ActiveCallTracker {
    private val activeCall = AtomicReference<Call?>()

    fun track(call: Call) {
        activeCall.getAndSet(call)?.cancel()
    }

    fun clear(call: Call) {
        activeCall.compareAndSet(call, null)
    }

    fun cancel() {
        activeCall.getAndSet(null)?.cancel()
    }
}

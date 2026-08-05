package com.entropyjournal.data.remote.codex

import java.util.concurrent.atomic.AtomicReference
import okhttp3.Call

/** Haelt den laufenden Codex-Aufruf fest, damit ein neuer den alten ersetzen kann. */
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

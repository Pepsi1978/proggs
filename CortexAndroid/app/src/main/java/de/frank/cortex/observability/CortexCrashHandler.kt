package de.frank.cortex.observability

import android.util.Log

class CortexCrashHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            CortexLog.error(
                module = "CRASH",
                fn = "uncaughtException",
                msg = "Unbehandelter Absturz in Thread ${thread.name}: ${throwable.message}",
                ctx = mapOf(
                    "thread" to thread.name,
                    "exception" to throwable.javaClass.simpleName
                ),
                trace = throwable.stackTraceToString()
            )
            Log.e(CortexLog.TAG, "ABSTURZ", throwable)
            // Synchron flushen: der Default-Handler killt den Prozess gleich — der Crash-Eintrag
            // laege sonst noch in der asynchronen Schreib-Queue und ginge verloren.
            CortexLog.flushBlocking(2000)
        } catch (_: Exception) {
            // Logging darf den Crash-Handler nie selbst crashen
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }
}

package de.frank.codexkompass.observability

import kotlin.system.exitProcess

/**
 * Globaler Fehlerfänger: nichts stirbt still.
 *
 * Ohne ihn verschwindet ein Absturz im Systemlog und ist nach einem Neustart des Geräts weg.
 * Der Handler schreibt den vollen Kontext ins Logbuch und gibt danach an den ursprünglichen
 * Handler ab, damit sich das System weiterhin normal verhält.
 */
object KompassCrashHandler {

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                KompassLog.error(
                    "CrashHandler",
                    "uncaught",
                    "Unbehandelter Fehler: ${error.javaClass.name}: ${error.message}",
                    mapOf(
                        "thread" to thread.name,
                        "stack" to error.stackTraceToString().take(4000),
                    ),
                )
            }
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                exitProcess(2)
            }
        }
    }
}

package de.frank.entropyreducer.data.diagnostics

import android.util.Log

/**
 * Statische Logging-Fassade (Timber-Muster) — der einheitliche Einstieg fuer ALLE Log-Ausgaben der
 * App (Frank-Wunsch 2026-06-12: kein Log laeuft mehr an der Diagnose vorbei).
 *
 * Warum eine Fassade statt DiagnosticLogger ueberall zu injizieren:
 * - Funktioniert auch in Klassen ausserhalb des Hilt-Graphen und in fruehen Lifecycle-Phasen (vor
 *   der Hilt-Injection) — dann Fallback auf pures Logcat, kein Log geht je verloren.
 * - Kein Konstruktor-Umbau in ~40 Dateien noetig (weniger Fehlerquellen, kein Graph-Umbau).
 *
 * Initialisierung: [EntropyReducerApp.onCreate] ruft [init] direkt nach super.onCreate(). Davor
 * verhalten sich alle Aufrufe exakt wie frueher (android.util.Log mit Original-Tag).
 *
 * Der [tag] (Klassenname) bleibt als "[Tag] "-Praefix in der Message erhalten — verlustfrei
 * gegenueber dem frueheren Log.x(TAG, msg); die Quelle ist in DB/JSONL/Logcat sichtbar.
 */
object Diag {
    @Volatile private var logger: DiagnosticLogger? = null

    /** Wird einmalig von EntropyReducerApp.onCreate() aufgerufen. */
    fun init(diagnosticLogger: DiagnosticLogger) {
        logger = diagnosticLogger
    }

    /** Fehler — etwas ist fehlgeschlagen. */
    fun e(area: DiagnosticArea, tag: String, message: String, throwable: Throwable? = null) {
        val l = logger
        if (l != null) l.error(area, prefixed(tag, message), throwable)
        else Log.e(tag, message, throwable)
    }

    /** Warnung — funktioniert, aber mit Einschraenkung. */
    fun w(area: DiagnosticArea, tag: String, message: String, throwable: Throwable? = null) {
        val l = logger
        if (l != null) l.warn(area, prefixed(tag, message), throwable)
        else Log.w(tag, message, throwable)
    }

    /** Reine Information. */
    fun i(area: DiagnosticArea, tag: String, message: String) {
        val l = logger
        if (l != null) l.info(area, prefixed(tag, message)) else Log.i(tag, message)
    }

    /** Entwickler-Detail (frueher Log.d) — laeuft ebenfalls durch alle Diagnose-Schichten. */
    fun d(area: DiagnosticArea, tag: String, message: String) {
        val l = logger
        if (l != null) l.debug(area, prefixed(tag, message)) else Log.d(tag, message)
    }

    private fun prefixed(tag: String, message: String) = "[$tag] $message"
}

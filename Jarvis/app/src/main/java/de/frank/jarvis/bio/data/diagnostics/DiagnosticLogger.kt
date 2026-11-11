package de.frank.jarvis.bio.data.diagnostics

import android.util.Log
import de.frank.jarvis.observability.Logbuch

/**
 * Diagnose des Biomarker-Bereichs: Statt der eigenen Diagnose-Datenbank von Entropie Reductor schreibt sie in Jarvis' Logbuch
 * (Fehler und Warnungen, Erfolge der Abrufe als Systemeintrag) und nach Logcat. Dieselbe Schnittstelle wie in Entropie Reductor.
 */
class DiagnosticLogger {
    fun error(area: DiagnosticArea, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "${area.displayName}: $message", throwable)
        Logbuch.fehler("Biomarker ${area.displayName}", throwable, message)
    }

    fun warn(area: DiagnosticArea, message: String, throwable: Throwable? = null) {
        Log.w(TAG, "${area.displayName}: $message", throwable)
        Logbuch.fehler("Biomarker ${area.displayName}", throwable, message)
    }

    fun success(area: DiagnosticArea, message: String) {
        Log.i(TAG, "${area.displayName}: $message")
        Logbuch.system("Biomarker ${area.displayName}", message)
    }

    fun info(area: DiagnosticArea, message: String) { Log.i(TAG, "${area.displayName}: $message") }

    fun debug(area: DiagnosticArea, message: String) { Log.d(TAG, "${area.displayName}: $message") }

    companion object { private const val TAG = "JarvisBio" }
}
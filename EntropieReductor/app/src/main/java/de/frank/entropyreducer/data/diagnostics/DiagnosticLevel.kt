package de.frank.entropyreducer.data.diagnostics

/**
 * Schweregrad eines Diagnose-Eintrags. Bestimmt Farbe und Symbol im Diagnose-Screen.
 *
 * Frank-Wunsch 2026-05-23: Nicht nur Fehler, sondern auch wichtige Erfolge loggen — damit
 * sichtbar ist OB und WANN ein Bereich zuletzt funktioniert hat, nicht nur wenn er kaputt ist.
 */
enum class DiagnosticLevel {
    /** Etwas ist fehlgeschlagen (z.B. 429 Rate-Limit, Token abgelaufen, Backup-Fehler). */
    ERROR,

    /** Funktioniert, aber mit Einschraenkung (z.B. Sync uebersprungen wegen Cooldown). */
    WARN,

    /** Wichtiger Erfolg (z.B. "Strava-Sync OK, 12 Workouts"). */
    SUCCESS,

    /** Reine Information ohne Wertung. */
    INFO,
    ;

    companion object {
        fun fromNameOrNull(raw: String?): DiagnosticLevel? =
            entries.firstOrNull { it.name == raw }
    }
}

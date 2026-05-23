package de.frank.entropyreducer.data.diagnostics

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ein einzelner Diagnose-Log-Eintrag. Liegt in einer EIGENEN Room-DB
 * ([DiagnosticLogDatabase]) — getrennt von AppDatabase und ScientistDatabase, damit:
 *  - keine Migration der Haupt-DB noetig ist (kein Risiko fuer echte Daten),
 *  - die Logs NICHT ins Drive-Backup wandern (geraetelokale Diagnose-Daten).
 *
 * [area] und [level] werden als String (Enum-Name) gespeichert — robust ohne TypeConverter,
 * und erlaubt einfache WHERE-Filter pro Bereich. Das Zurueck-Mapping passiert via
 * [DiagnosticArea.fromNameOrNull] / [DiagnosticLevel.fromNameOrNull].
 */
@Entity(tableName = "diagnostic_log")
data class DiagnosticLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Unix-Millis, Berliner Zeit (App laeuft global in Europe/Berlin, siehe EntropyReducerApp). */
    val timestampMs: Long,
    /** [DiagnosticArea.name] */
    val area: String,
    /** [DiagnosticLevel.name] */
    val level: String,
    /** Kurze, fuer Frank verstaendliche Meldung (Deutsch). */
    val message: String,
    /** Optionale Zusatzdetails: Stacktrace, HTTP-Code, Response-Body etc. */
    val details: String? = null,
)

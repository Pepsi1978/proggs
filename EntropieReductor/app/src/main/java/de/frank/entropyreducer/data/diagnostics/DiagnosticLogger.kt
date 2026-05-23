package de.frank.entropyreducer.data.diagnostics

import android.util.Log
import de.frank.entropyreducer.di.ApplicationScope
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Zentrales internes Diagnose-Logging (Frank-Wunsch 2026-05-23).
 *
 * Idee: Alle API-Bereiche (Strava, Whoop, Oura, Drive-Backup, Kalender, Groq, Gemini, Claude,
 * TTS, Health Connect) melden Fehler UND wichtige Erfolge hierher. Frank sieht das im
 * Diagnose-Screen — ohne Logcat, ohne PC. So ist in Zukunft sofort sichtbar WARUM etwas nicht
 * aktualisiert (z.B. "Strava 14:03 — HTTP 429 Rate-Limit" statt nur "vor 6 h synchronisiert").
 *
 * Robustheit (Direktive #3):
 *  - Fire-and-forget: Methoden sind NICHT suspend, blockieren den Aufrufer nie.
 *  - Jeder DB-Zugriff in runCatching — ein Logging-Fehler darf NIE die App stoeren oder die
 *    eigentliche Funktion (Sync, Backup) beeintraechtigen.
 *  - Schreibt IMMER parallel ins Logcat (Schicht 2), falls die DB mal nicht erreichbar ist.
 *  - Auto-Pruning: alte Eintraege (>14 Tage) und harte Obergrenze (2000) verhindern
 *    unbegrenztes Wachstum (Poka-Yoke).
 */
@Singleton
class DiagnosticLogger
@Inject
constructor(
    private val dao: DiagnosticLogDao,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val insertsSincePrune = AtomicInteger(0)

    /** Fehler — etwas ist fehlgeschlagen. */
    fun error(area: DiagnosticArea, message: String, throwable: Throwable? = null) {
        write(area, DiagnosticLevel.ERROR, message, throwable)
    }

    /** Warnung — funktioniert, aber mit Einschraenkung (z.B. Sync uebersprungen). */
    fun warn(area: DiagnosticArea, message: String, throwable: Throwable? = null) {
        write(area, DiagnosticLevel.WARN, message, throwable)
    }

    /** Wichtiger Erfolg — z.B. "Sync OK, 12 Workouts". */
    fun success(area: DiagnosticArea, message: String) {
        write(area, DiagnosticLevel.SUCCESS, message, null)
    }

    /** Reine Information. */
    fun info(area: DiagnosticArea, message: String) {
        write(area, DiagnosticLevel.INFO, message, null)
    }

    private fun write(
        area: DiagnosticArea,
        level: DiagnosticLevel,
        message: String,
        throwable: Throwable?,
    ) {
        // Schicht 2: immer auch ins Logcat — ueberlebt selbst wenn die DB-Coroutine scheitert.
        val tag = "Diag/${area.name}"
        when (level) {
            DiagnosticLevel.ERROR -> Log.e(tag, message, throwable)
            DiagnosticLevel.WARN -> Log.w(tag, message, throwable)
            DiagnosticLevel.SUCCESS -> Log.i(tag, message)
            DiagnosticLevel.INFO -> Log.d(tag, message)
        }

        val details =
            throwable?.let { t ->
                buildString {
                    append(t::class.java.simpleName)
                    t.message?.let { append(": ").append(it) }
                    append('\n')
                    append(Log.getStackTraceString(t).take(MAX_DETAIL_CHARS))
                }
            }

        scope.launch {
            runCatching {
                dao.insert(
                    DiagnosticLogEntry(
                        timestampMs = System.currentTimeMillis(),
                        area = area.name,
                        level = level.name,
                        message = message,
                        details = details,
                    )
                )
                maybePrune()
            }
                .onFailure {
                    // Bewusst NUR Logcat — kein erneuter DB-Zugriff, sonst Endlos-Schleife.
                    Log.w("DiagnosticLogger", "Log-Insert fehlgeschlagen (ignoriert)", it)
                }
        }
    }

    /** Pruning nur gelegentlich, um nicht bei jedem Insert 2 zusaetzliche Queries zu fahren. */
    private suspend fun maybePrune() {
        if (insertsSincePrune.incrementAndGet() < PRUNE_EVERY_N_INSERTS) return
        insertsSincePrune.set(0)
        runCatching {
            val cutoff = System.currentTimeMillis() - MAX_AGE_MS
            dao.deleteOlderThan(cutoff)
            dao.trimToMaxCount(MAX_ENTRIES)
        }
    }

    companion object {
        private const val MAX_DETAIL_CHARS = 4000
        private const val PRUNE_EVERY_N_INSERTS = 25
        private const val MAX_ENTRIES = 2000
        private const val MAX_AGE_MS = 14L * 24L * 60L * 60L * 1000L // 14 Tage
    }
}

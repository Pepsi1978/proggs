package de.frank.entropyreducer.data.diagnostics

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.di.ApplicationScope
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/**
 * Zentrales internes Diagnose-Logging (Frank-Wunsch 2026-05-23).
 *
 * Zweck (Frank-Klarstellung 2026-05-23): Das Log dient VOR ALLEM dem spaeteren Debuggen durch
 * Claude. Wenn Tage spaeter ein Sync-/API-Fehler untersucht werden soll, muss nachvollziehbar sein
 * WAS WANN in welchem Bereich (Whoop, Oura, Drive, Kalender, Groq, Gemini, Claude, TTS,
 * Health Connect) schiefging — inklusive HTTP-Codes und Stacktraces. Sekundaer dient der
 * In-App-Screen Frank zum schnellen Reinschauen.
 *
 * DREI Persistenz-Schichten (Defense in Depth):
 * 1. Room-DB → speist den In-App-Diagnose-Screen, 14 Tage, gefiltert/farbig.
 * 2. Logcat → sofortiges Live-Debugging (`adb logcat -s Diag/HEALTH_CONNECT`), rotiert aber schnell.
 * 3. JSONL-Datei pro Tag im externen App-Ordner
 *    (`<externalFilesDir>/diagnostics/diag-YYYY-MM-DD.jsonl`) → von Claude per `adb pull` auch Tage
 *    spaeter auslesbar und grep-/parsebar. DAS ist die Schicht fuer das Nachhinein-Debuggen. Pfad
 *    auf dem Geraet: /sdcard/Android/data/<package>/files/diagnostics/
 *
 * Robustheit (Direktive #3):
 * - Fire-and-forget: Methoden sind NICHT suspend, blockieren den Aufrufer nie.
 * - Jede Schicht in eigenem runCatching — ein Fehler in einer Schicht stoppt die anderen nicht und
 *   beeintraechtigt NIE die eigentliche Funktion (Sync, Backup).
 * - Auto-Pruning: DB (14 Tage / max 4000) und alte JSONL-Dateien (>14 Tage) werden geraeumt.
 */
@Singleton
class DiagnosticLogger
@Inject
constructor(
    private val dao: DiagnosticLogDao,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val insertsSincePrune = AtomicInteger(0)

    // Frank-Debugging 2026-05-23: serialisiert die JSONL-Datei-Schreibzugriffe. Mehrere Syncs
    // loggen parallel (applicationScope/Dispatchers.IO) — ohne diesen Mutex koennten zwei
    // gleichzeitige appendText-Aufrufe Zeilen verschraenken und die JSONL-Datei beschaedigen.
    private val fileMutex = Mutex()

    // java.time-Formatter sind thread-safe (im Gegensatz zu SimpleDateFormat) — wichtig, weil
    // mehrere Log-Coroutinen parallel im ApplicationScope (Dispatchers.IO) laufen koennen.
    private val isoFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    private val fileDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Performance 2026-05-24 (vom Benutzer freigegeben): Das app-private Diagnose-Verzeichnis
    // aendert sich waehrend der Prozesslaufzeit nie. getExternalFilesDir() ist aber ein
    // Framework-Aufruf, der bisher bei JEDER Log-Zeile neu lief. Wir cachen das Ergebnis — aber
    // NUR wenn es erfolgreich aufgeloest wurde (non-null + Verzeichnis existiert/angelegt).
    // Schlaegt die Aufloesung einmal fehl (z.B. Speicher kurz nicht verfuegbar), wird NICHT
    // gecacht und beim naechsten Mal neu versucht. Zugriff laeuft immer unter fileMutex -> kein
    // Race auf cachedDir.
    @Volatile private var cachedDir: File? = null

    /** Fehler — etwas ist fehlgeschlagen. */
    fun error(area: DiagnosticArea, message: String, throwable: Throwable? = null) {
        write(area, DiagnosticLevel.ERROR, message, throwable)
    }

    /** Warnung — funktioniert, aber mit Einschraenkung (z.B. Sync uebersprungen, Rate-Limit). */
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

    /** Entwickler-Detail (frueher Log.d) — gleiche Schichten, eigenes Level zum Filtern. */
    fun debug(area: DiagnosticArea, message: String) {
        write(area, DiagnosticLevel.DEBUG, message, null)
    }

    private fun write(
        area: DiagnosticArea,
        level: DiagnosticLevel,
        message: String,
        throwable: Throwable?,
    ) {
        val timestampMs = System.currentTimeMillis()

        // Schicht 2: Logcat — ueberlebt selbst wenn DB- und Datei-Coroutine scheitern.
        val tag = "Diag/${area.name}"
        when (level) {
            DiagnosticLevel.ERROR -> Log.e(tag, message, throwable)
            DiagnosticLevel.WARN -> Log.w(tag, message, throwable)
            DiagnosticLevel.SUCCESS -> Log.i(tag, message)
            // 2026-06-12: INFO jetzt Log.i (vorher Log.d) — Standard-Logcat zeigt Infos wieder an.
            DiagnosticLevel.INFO -> Log.i(tag, message)
            DiagnosticLevel.DEBUG -> Log.d(tag, message)
        }

        val details = throwable?.let { t ->
            buildString {
                append(t::class.java.simpleName)
                t.message?.let { append(": ").append(it) }
                append('\n')
                append(Log.getStackTraceString(t).take(MAX_DETAIL_CHARS))
            }
        }

        scope.launch {
            // Schicht 1: Room-DB (In-App-Screen).
            runCatching {
                    dao.insert(
                        DiagnosticLogEntry(
                            timestampMs = timestampMs,
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
                    Log.w("DiagnosticLogger", "DB-Insert fehlgeschlagen (ignoriert)", it)
                }

            // Schicht 3: JSONL-Tagesdatei (von Claude per adb pull auslesbar). Datei-Zugriffe
            // ueber fileMutex serialisiert, damit parallele Logs keine Zeilen verschraenken.
            runCatching {
                    fileMutex.withLock {
                        appendToDailyFile(area, level, message, details, timestampMs)
                    }
                }
                .onFailure {
                    Log.w("DiagnosticLogger", "Datei-Append fehlgeschlagen (ignoriert)", it)
                }
        }
    }

    /** Haengt eine JSON-Zeile an die Tagesdatei. JSONObject sorgt fuer korrektes Escaping. */
    private fun appendToDailyFile(
        area: DiagnosticArea,
        level: DiagnosticLevel,
        message: String,
        details: String?,
        timestampMs: Long,
    ) {
        val dir = diagnosticsDir() ?: return
        val zdt = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault())
        val file = File(dir, "diag-${fileDateFormat.format(zdt)}.jsonl")
        val line =
            JSONObject()
                .put("ts", isoFormat.format(zdt))
                .put("area", area.name)
                .put("level", level.name)
                .put("message", message)
                .apply { if (!details.isNullOrBlank()) put("details", details) }
                .toString()
        file.appendText(line + "\n")
    }

    /**
     * Pruning nur gelegentlich (alle 25 Inserts), um nicht jedes Mal zusaetzliche Arbeit zu fahren.
     */
    private suspend fun maybePrune() {
        if (insertsSincePrune.incrementAndGet() < PRUNE_EVERY_N_INSERTS) return
        insertsSincePrune.set(0)
        runCatching {
            val cutoff = System.currentTimeMillis() - MAX_AGE_MS
            dao.deleteOlderThan(cutoff)
            dao.trimToMaxCount(MAX_ENTRIES)
            fileMutex.withLock { pruneOldFiles(cutoff) }
        }
    }

    /**
     * Loest das app-private Diagnose-Verzeichnis auf und cacht es nach dem ersten Erfolg. Gibt null
     * zurueck wenn der externe Speicher (gerade) nicht verfuegbar ist — dann wird NICHT gecacht,
     * sondern beim naechsten Aufruf erneut versucht.
     */
    private fun diagnosticsDir(): File? {
        cachedDir?.let {
            return it
        }
        val base = context.getExternalFilesDir(null) ?: return null
        val dir = File(base, DIR_NAME)
        if (!dir.exists() && !dir.mkdirs()) return null
        cachedDir = dir
        return dir
    }

    /** Loescht JSONL-Tagesdateien die aelter als 14 Tage sind. */
    private fun pruneOldFiles(cutoffMs: Long) {
        val dir = diagnosticsDir() ?: return
        val files = dir.listFiles() ?: return
        files.forEach { f ->
            if (f.isFile && f.name.startsWith("diag-") && f.lastModified() < cutoffMs) {
                runCatching { f.delete() }
            }
        }
    }

    companion object {
        private const val DIR_NAME = "diagnostics"
        private const val MAX_DETAIL_CHARS = 4000
        private const val PRUNE_EVERY_N_INSERTS = 25
        // 2026-06-12: 2000 → 4000. Logging-Vereinheitlichung: ~40 neue Quellen (inkl.
        // INFO/DEBUG) schreiben in die DB — ERROR darf nicht zu schnell rausrotieren.
        // Die JSONL-Schicht haelt unabhaengig davon volle 14 Tage.
        private const val MAX_ENTRIES = 4000
        private const val MAX_AGE_MS = 14L * 24L * 60L * 60L * 1000L // 14 Tage
    }
}

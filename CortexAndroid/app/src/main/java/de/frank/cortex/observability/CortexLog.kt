package de.frank.cortex.observability

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

object CortexLog {

    const val TAG = "FRANK_CORTEX"
    const val LOGIC_TAG = "LOGIC"

    private lateinit var logFile: File

    // DateTimeFormatter statt SimpleDateFormat: SimpleDateFormat ist NICHT thread-safe — das
    // geteilte Objekt wurde von Main-, IO- und OkHttp-Threads gleichzeitig benutzt und konnte
    // korrupte Timestamps liefern. DateTimeFormatter ist immutable/thread-safe (gleiches Format).
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

    // Datei-Schreiben laeuft auf EINEM dedizierten Hintergrund-Thread: vorher blockierte jede
    // Log-Zeile den Aufrufer (oft den Main-Thread) mit open+write+close — Jank-Quelle unter Last
    // (jeder HTTP-Request loggt mehrfach). Single-Thread = FIFO, die Log-Reihenfolge in der Datei
    // bleibt exakt die Aufruf-Reihenfolge. Logcat (Log.i/w/e) bleibt synchron auf dem Aufrufer,
    // damit `adb logcat` weiterhin LIVE mitlesbar ist (Observability-Direktive).
    private val writeExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "cortex-log-writer").apply { isDaemon = true }
    }

    fun init(context: Context) {
        logFile = File(context.filesDir, "cortex.log.jsonl")
        Log.i(TAG, "Log-Pfad: ${logFile.absolutePath}")
        info("CortexLog", "init", "Logging initialisiert", mapOf("path" to logFile.absolutePath))
    }

    fun info(module: String, fn: String, msg: String, ctx: Map<String, Any?> = emptyMap()) {
        write("INFO", module, fn, msg, ctx)
    }

    fun warn(module: String, fn: String, msg: String, ctx: Map<String, Any?> = emptyMap()) {
        write("WARN", module, fn, msg, ctx)
    }

    fun error(module: String, fn: String, msg: String, ctx: Map<String, Any?> = emptyMap(), trace: String? = null) {
        val fullCtx = if (trace != null) ctx + ("trace" to trace) else ctx
        write("ERROR", module, fn, msg, fullCtx)
    }

    fun probe(condition: Boolean, message: String, ctx: Map<String, Any?> = emptyMap()) {
        if (!condition) {
            warn("PROBE", "probe", message, ctx)
        }
    }

    fun checkpoint(step: String, intent: String, expected: String, actual: String, ok: Boolean, ctx: Map<String, Any?> = emptyMap()) {
        val entry = JSONObject().apply {
            put("ts", dateFormat.format(LocalDateTime.now()))
            put("kind", "CHECKPOINT")
            put("step", step)
            put("intent", intent)
            put("expected", expected)
            put("actual", actual)
            put("ok", ok)
            ctx.forEach { (k, v) -> put(k, v?.toString() ?: JSONObject.NULL) }
        }
        Log.i(LOGIC_TAG, entry.toString())
        writeToFile(entry.toString())
    }

    private fun write(level: String, module: String, fn: String, msg: String, ctx: Map<String, Any?>) {
        val entry = JSONObject().apply {
            put("ts", dateFormat.format(LocalDateTime.now()))
            put("level", level)
            put("module", module)
            put("fn", fn)
            put("msg", msg)
            if (ctx.isNotEmpty()) {
                val ctxObj = JSONObject()
                ctx.forEach { (k, v) -> ctxObj.put(k, v?.toString() ?: JSONObject.NULL) }
                put("ctx", ctxObj)
            }
        }
        val line = entry.toString()
        when (level) {
            "ERROR" -> Log.e(TAG, line)
            "WARN" -> Log.w(TAG, line)
            else -> Log.i(TAG, line)
        }
        writeToFile(line)
    }

    // Rotation: Log darf nicht unbegrenzt wachsen (Observability-Standard). Bei Ueberschreiten
    // wird die Datei zu .1 rotiert (eine Generation reicht — die aeltere wird ersetzt).
    private const val MAX_LOG_BYTES = 5L * 1024 * 1024

    /** Blockiert, bis alle bisher eingereihten Log-Zeilen auf Platte sind (max. [timeoutMs]).
     *  Nur fuer den Crash-Pfad: der Default-Handler killt den Prozess direkt danach — ohne
     *  Flush laege genau der wichtigste Eintrag (der Crash) noch in der Executor-Queue. */
    fun flushBlocking(timeoutMs: Long = 2000) {
        try {
            writeExecutor.submit {}.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "Log-Flush fehlgeschlagen: ${e.message}")
        }
    }

    private fun writeToFile(line: String) {
        if (!::logFile.isInitialized) return
        try {
            writeExecutor.execute {
                try {
                    if (logFile.length() > MAX_LOG_BYTES) {
                        val rotated = File(logFile.parentFile, logFile.name + ".1")
                        if (rotated.exists()) rotated.delete()
                        logFile.renameTo(rotated)
                    }
                    FileWriter(logFile, true).use { it.appendLine(line) }
                } catch (e: Exception) {
                    Log.e(TAG, "Log-Schreibfehler: ${e.message}")
                }
            }
        } catch (e: Exception) {
            // Executor abgelehnt (Shutdown) — Logging darf nie crashen.
            Log.e(TAG, "Log-Queue-Fehler: ${e.message}")
        }
    }
}

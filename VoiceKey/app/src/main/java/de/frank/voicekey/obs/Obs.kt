package de.frank.voicekey.obs

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Observability-Schicht (observability-first):
 * - Strukturiertes JSON-Lines-Log in filesDir/logs/voicekey.jsonl (+ Rotation).
 * - Spiegelung nach logcat (TAG VOICEKEY) fuer Live-Tailing.
 * - probe(): Logik-Sonden — Annahme-Verletzungen landen als WARN im Log, crashen aber nie.
 * - checkpoint(): Live-Logik-Sonden (Intent-Verifikation) im eigenen Kanal (TAG LOGIC),
 *   Format "erwartet vs. tatsaechlich" — live lesbar via `adb logcat -s LOGIC`.
 */
object Obs {

    const val TAG = "VOICEKEY"
    const val TAG_LOGIC = "LOGIC"

    private const val MAX_LOG_BYTES = 2L * 1024 * 1024

    private val lock = Any()
    private var logFile: File? = null
    @Volatile private var appVersion: String = "?"

    fun init(context: Context, versionName: String, versionCode: Int) {
        appVersion = "$versionName ($versionCode)"
        val dir = File(context.filesDir, "logs").apply { mkdirs() }
        logFile = File(dir, "voicekey.jsonl")
        // Fester Log-Pfad wird beim Start EINMAL ausgegeben (Direktive).
        Log.i(TAG, "Log: ${logFile?.absolutePath}")
        i("Obs", "init", "App-Start", mapOf("version" to appVersion))
    }

    fun d(module: String, fn: String, msg: String, ctx: Map<String, Any?> = emptyMap()) =
        write("DEBUG", module, fn, msg, ctx, null)

    fun i(module: String, fn: String, msg: String, ctx: Map<String, Any?> = emptyMap()) =
        write("INFO", module, fn, msg, ctx, null)

    fun w(module: String, fn: String, msg: String, ctx: Map<String, Any?> = emptyMap(), tr: Throwable? = null) =
        write("WARN", module, fn, msg, ctx, tr)

    fun e(module: String, fn: String, msg: String, ctx: Map<String, Any?> = emptyMap(), tr: Throwable? = null) =
        write("ERROR", module, fn, msg, ctx, tr)

    fun fatal(module: String, fn: String, msg: String, tr: Throwable) =
        write("FATAL", module, fn, msg, emptyMap(), tr)

    /**
     * Logik-Sonde: prueft eine Annahme. Verletzung wird geloggt, die App laeuft weiter.
     * Rueckgabe = Bedingung, damit sie inline in if() nutzbar bleibt.
     */
    fun probe(condition: Boolean, msg: String, module: String, fn: String, ctx: Map<String, Any?> = emptyMap()): Boolean {
        if (!condition) write("WARN", module, fn, "PROBE VERLETZT: $msg", ctx, null, kind = "PROBE")
        return condition
    }

    /**
     * Intent-Checkpoint (bestaetigende Live-Logik-Sonde): meldet "erwartet vs. tatsaechlich"
     * fuer einen fachlichen Schritt aus dem Bau-Prompt. Eigener logcat-Kanal LOGIC.
     */
    fun checkpoint(step: String, intent: String, expected: String, actual: String, ctx: Map<String, Any?> = emptyMap()) {
        val ok = expected == actual
        val json = JSONObject().apply {
            put("ts", now())
            put("kind", "CHECKPOINT")
            put("step", step)
            put("intent", intent)
            put("expected", expected)
            put("actual", actual)
            put("ok", ok)
            put("ctx", JSONObject(ctx.mapValues { it.value?.toString() ?: "null" }))
        }
        val line = json.toString()
        if (ok) Log.i(TAG_LOGIC, line) else Log.e(TAG_LOGIC, line)
        appendLine(line)
    }

    private fun write(
        level: String,
        module: String,
        fn: String,
        msg: String,
        ctx: Map<String, Any?>,
        tr: Throwable?,
        kind: String = "LOG",
    ) {
        val json = JSONObject().apply {
            put("ts", now())
            put("level", level)
            put("kind", kind)
            put("module", module)
            put("fn", fn)
            put("msg", msg)
            put("version", appVersion)
            if (ctx.isNotEmpty()) put("ctx", JSONObject(ctx.mapValues { it.value?.toString() ?: "null" }))
            if (tr != null) put("trace", Log.getStackTraceString(tr))
        }
        val line = json.toString()
        when (level) {
            "DEBUG" -> Log.d(TAG, line)
            "INFO" -> Log.i(TAG, line)
            "WARN" -> Log.w(TAG, line)
            else -> Log.e(TAG, line)
        }
        appendLine(line)
    }

    private fun appendLine(line: String) {
        val file = logFile ?: return
        try {
            synchronized(lock) {
                if (file.length() > MAX_LOG_BYTES) {
                    val old = File(file.parentFile, "voicekey.1.jsonl")
                    if (old.exists()) old.delete()
                    file.renameTo(old)
                }
                file.appendText(line + "\n")
            }
        } catch (e: Exception) {
            // Logging darf die App nie zum Absturz bringen — letzter Ausweg: nur logcat.
            Log.e(TAG, "Log-Datei nicht schreibbar: ${e.message}")
        }
    }

    private fun now(): String = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

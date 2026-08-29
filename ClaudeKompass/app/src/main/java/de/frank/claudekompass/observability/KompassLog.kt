package de.frank.claudekompass.observability

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

/**
 * Strukturiertes Logbuch der App — eine JSON-Zeile je Ereignis.
 *
 * Regel „Observability-First": Ohne diese Schicht wird kein Feature gebaut. Jede Zeile trägt
 * Zeitstempel, Stufe, Modul, Funktion, Meldung und einen frei befüllbaren Kontext, damit sich
 * ein Fehlverhalten später aus der Datei rekonstruieren lässt, ohne die App neu zu starten.
 *
 * Der Pfad wird beim Start EINMAL ausgegeben (siehe [start]), damit er auffindbar ist.
 */
object KompassLog {

    private const val TAG = "Kompass"
    private const val FILE_NAME = "kompass-log.jsonl"
    private const val MAX_BYTES = 2L * 1024 * 1024
    private const val KEPT_ROTATIONS = 3

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.GERMANY)
    private val writeLock = Any()

    @Volatile
    private var logFile: File? = null

    @Volatile
    private var sessionId: String = "-"

    /** Pfad der Logdatei, sobald [start] gelaufen ist — sonst null. */
    val path: String? get() = logFile?.absolutePath

    fun start(context: Context, session: String) {
        val directory = File(context.filesDir, "logs").apply { mkdirs() }
        logFile = File(directory, FILE_NAME)
        sessionId = session
        // Der Pfad steht bewusst auch im Logcat: ohne ihn findet niemand die Datei.
        Log.i(TAG, "Logbuch: ${logFile?.absolutePath}")
        info("KompassLog", "start", "Logbuch geoeffnet", mapOf("pfad" to (path ?: "-")))
    }

    fun debug(module: String, function: String, message: String, context: Map<String, Any?> = emptyMap()) =
        write("debug", module, function, message, context)

    fun info(module: String, function: String, message: String, context: Map<String, Any?> = emptyMap()) =
        write("info", module, function, message, context)

    fun warn(module: String, function: String, message: String, context: Map<String, Any?> = emptyMap()) =
        write("warn", module, function, message, context)

    fun error(module: String, function: String, message: String, context: Map<String, Any?> = emptyMap()) =
        write("error", module, function, message, context)

    private fun write(
        level: String,
        module: String,
        function: String,
        message: String,
        context: Map<String, Any?>,
    ) {
        val line = runCatching {
            JSONObject()
                .put("ts", timestampFormat.format(Date()))
                .put("level", level)
                .put("session", sessionId)
                .put("module", module)
                .put("fn", function)
                .put("msg", message)
                .put("ctx", JSONObject(context.mapValues { it.value?.toString() ?: "null" }))
                .toString()
        }.getOrElse { return }

        // Spiegelung nach Logcat: während der Entwicklung ist das der schnellste Weg.
        when (level) {
            "error" -> Log.e(TAG, line)
            "warn" -> Log.w(TAG, line)
            "debug" -> Log.d(TAG, line)
            else -> Log.i(TAG, line)
        }

        val file = logFile ?: return
        synchronized(writeLock) {
            runCatching {
                if (file.length() > MAX_BYTES) rotate(file)
                file.appendText(line + "\n")
            }
        }
    }

    /** Hält die Datei klein, ohne ältere Einträge sofort wegzuwerfen. */
    private fun rotate(file: File) {
        for (index in KEPT_ROTATIONS downTo 1) {
            val older = File(file.parentFile, "$FILE_NAME.$index")
            val newer = if (index == 1) file else File(file.parentFile, "$FILE_NAME.${index - 1}")
            if (newer.exists()) {
                if (older.exists()) older.delete()
                newer.renameTo(older)
            }
        }
    }
}

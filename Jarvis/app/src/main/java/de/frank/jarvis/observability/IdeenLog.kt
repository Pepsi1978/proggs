package de.frank.jarvis.observability

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

enum class LogStufe { DEBUG, INFO, WARN, ERROR }

data class LogZeile(
    val zeitpunkt: Long,
    val stufe: LogStufe,
    val modul: String,
    val funktion: String,
    val nachricht: String,
    val kontext: Map<String, Any?> = emptyMap(),
) {
    fun alsJson(): String = JSONObject()
        // SimpleDateFormat ist nicht threadsicher, protokolliert wird aber aus mehreren Threads.
        .put("ts", synchronized(ZEIT_FORMAT) { ZEIT_FORMAT.format(Date(zeitpunkt)) })
        .put("level", stufe.name)
        .put("module", modul)
        .put("fn", funktion)
        .put("msg", nachricht)
        .put("ctx", JSONObject(kontext))
        .toString()

    private companion object {
        val ZEIT_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.GERMANY)
    }
}

/**
 * Strukturiertes Protokoll als JSON-Zeilen (Kapitel 17 der Referenz).
 *
 * Niemals hinein: Schlüssel, Token, Passwörter, vollständige Diktate oder Ideentexte —
 * statt des Inhalts wird dessen Länge protokolliert.
 *
 * Die Datei wird auf einem eigenen Faden geschrieben, nie auf dem des Aufrufers (oft der
 * Hauptfaden mitten im Vorlesen). Jede Zeile ab INFO landet zusätzlich im [Logbuch] als Art
 * „sprache“, damit Vorlesen und Diktat dort mit ausgewertet werden.
 */
object IdeenLog {
    private const val MAX_ZEILEN_IM_SPEICHER = 500
    private const val MAX_DATEI_BYTES = 1_000_000L
    private const val DATEI_NAME = "geniale-ideen.log"

    private val puffer = ConcurrentLinkedDeque<LogZeile>()
    private val _zeilen = MutableStateFlow<List<LogZeile>>(emptyList())
    val zeilen: StateFlow<List<LogZeile>> = _zeilen.asStateFlow()

    @Volatile private var datei: File? = null

    /** Ein Faden für alle Dateizugriffe — so bleibt auch die Reihenfolge von Leeren und Anhängen erhalten. */
    private val schreiber = Executors.newSingleThreadExecutor { r ->
        Thread(r, "jarvis-ideenlog").apply { isDaemon = true }
    }

    fun start(context: Context) {
        val verzeichnis = File(context.filesDir, "logs").apply { mkdirs() }
        datei = File(verzeichnis, DATEI_NAME)
        info("IdeenLog", "start", "Protokoll geöffnet", mapOf("pfad" to DATEI_NAME))
    }

    fun logDatei(): File? = datei

    fun debug(modul: String, fn: String, msg: String, ctx: Map<String, Any?> = emptyMap()) =
        schreibe(LogStufe.DEBUG, modul, fn, msg, ctx)

    fun info(modul: String, fn: String, msg: String, ctx: Map<String, Any?> = emptyMap()) =
        schreibe(LogStufe.INFO, modul, fn, msg, ctx)

    fun warn(modul: String, fn: String, msg: String, ctx: Map<String, Any?> = emptyMap()) =
        schreibe(LogStufe.WARN, modul, fn, msg, ctx)

    fun error(modul: String, fn: String, msg: String, ctx: Map<String, Any?> = emptyMap()) =
        schreibe(LogStufe.ERROR, modul, fn, msg, ctx)

    fun leeren() {
        puffer.clear()
        _zeilen.value = emptyList()
        val ziel = datei ?: return
        runCatching { schreiber.execute { runCatching { ziel.writeText("") } } }
    }

    private fun schreibe(
        stufe: LogStufe,
        modul: String,
        fn: String,
        msg: String,
        ctx: Map<String, Any?>,
    ) {
        val zeile = LogZeile(System.currentTimeMillis(), stufe, modul, fn, msg, ctx)
        puffer.addFirst(zeile)
        while (puffer.size > MAX_ZEILEN_IM_SPEICHER) puffer.pollLast()
        _zeilen.value = puffer.toList()
        when (stufe) {
            LogStufe.DEBUG -> Log.d(modul, "$fn: $msg")
            LogStufe.INFO -> Log.i(modul, "$fn: $msg")
            LogStufe.WARN -> Log.w(modul, "$fn: $msg")
            LogStufe.ERROR -> Log.e(modul, "$fn: $msg")
        }
        // DEBUG bleibt draussen: Dort stehen einzelne korrigierte Wörter aus Diktaten.
        if (stufe != LogStufe.DEBUG) {
            Logbuch.sprache(
                "$modul.$fn: $msg",
                buildString {
                    append(stufe.name)
                    if (ctx.isNotEmpty()) append(' ').append(ctx.entries.joinToString(" ") { "${it.key}=${it.value}" })
                },
            )
        }
        val ziel = datei ?: return
        runCatching {
            schreiber.execute {
                runCatching {
                    if (ziel.length() > MAX_DATEI_BYTES) {
                        val behalten = ziel.readLines().takeLast(1_000)
                        ziel.writeText(behalten.joinToString("\n") + "\n")
                    }
                    ziel.appendText(zeile.alsJson() + "\n")
                }
            }
        }
    }
}

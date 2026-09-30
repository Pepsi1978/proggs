package de.frank.longevity.ki

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnose-Log aller KI-Arbeiten. Schreibt in den app-eigenen Ordner auf dem gemeinsamen Speicher
 * (`/sdcard/Android/data/de.frank.longevity/files/logs/ki-log.txt`), damit man ihn per adb auslesen kann –
 * auch beim Release-Build, der keinen Zugriff auf die privaten App-Daten erlaubt. Ab 3 MB wird die Datei
 * nach `ki-log.alt.txt` verschoben, es bleiben also immer die letzten ~6 MB erhalten.
 */
object KiLog {
    private const val TAG = "LongevityKi"
    private const val MAX_BYTES = 3L * 1024 * 1024
    private var datei: File? = null
    private val zeit = SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS", Locale.GERMANY)

    /** Ordner für Diagnosedateien (Log, Kopie des Protokolls, Stand des Laufs). */
    var ordner: File? = null
        private set

    fun init(context: Context) {
        if (datei != null) return
        val o = File(context.getExternalFilesDir(null) ?: context.filesDir, "logs").apply { mkdirs() }
        ordner = o
        datei = File(o, "ki-log.txt")
    }

    fun info(text: String) = schreibe("I", text, null)

    fun warn(text: String, t: Throwable? = null) = schreibe("W", text, t)

    fun fehler(text: String, t: Throwable? = null) = schreibe("E", text, t)

    /** Legt eine Diagnose-Kopie einer Datei in den Log-Ordner (z. B. das Diskussionsprotokoll). */
    fun kopie(name: String, inhalt: String) {
        val o = ordner ?: return
        runCatching { File(o, name).writeText(inhalt) }
    }

    @Synchronized
    private fun schreibe(stufe: String, text: String, t: Throwable?) {
        when (stufe) {
            "E" -> Log.e(TAG, text, t)
            "W" -> Log.w(TAG, text, t)
            else -> Log.i(TAG, text)
        }
        val d = datei ?: return
        runCatching {
            if (d.length() > MAX_BYTES) {
                val alt = File(d.parentFile, "ki-log.alt.txt")
                alt.delete()
                d.renameTo(alt)
            }
            val zeile = buildString {
                append(zeit.format(Date())).append(' ').append(stufe).append(" [").append(Thread.currentThread().name).append("] ").append(text)
                if (t != null) {
                    append('\n').append(t.javaClass.name).append(": ").append(t.message)
                    val sw = StringWriter()
                    t.printStackTrace(PrintWriter(sw))
                    append('\n').append(sw.toString().lineSequence().take(40).joinToString("\n"))
                }
                append('\n')
            }
            d.appendText(zeile)
        }
    }
}

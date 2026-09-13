package de.frank.genialeideen.observability

import android.content.Context
import de.frank.genialeideen.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Schreibt den Bericht **vor** dem Absturz und reicht danach an den ursprünglichen Handler
 * weiter — der Absturz wird nicht verschluckt (Baustein P.1).
 */
class IdeenCrashHandler private constructor(
    context: Context,
    private val vorgaenger: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    private val verzeichnis = File(context.filesDir, "crashes").apply { mkdirs() }

    override fun uncaughtException(thread: Thread, fehler: Throwable) {
        runCatching {
            val stapel = StringWriter().also { fehler.printStackTrace(PrintWriter(it)) }.toString()
            val bericht = buildString {
                appendLine("Zeitpunkt: ${ZEIT.format(Date())}")
                appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_BUMPED_AT})")
                appendLine("Gerät: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                appendLine("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
                appendLine("Thread: ${thread.name}")
                appendLine("Letzte Aktion: ${letzteAktion ?: "unbekannt"}")
                appendLine()
                append(stapel)
            }
            File(verzeichnis, "absturz-${System.currentTimeMillis()}.txt").writeText(bericht)
            IdeenLog.error("Absturz", "uncaughtException", fehler.javaClass.simpleName, mapOf("thread" to thread.name))
        }
        vorgaenger?.uncaughtException(thread, fehler)
    }

    companion object {
        private val ZEIT = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY)

        /** Was der Nutzer zuletzt getan hat — steht im Bericht, nie mit Inhalten. */
        @Volatile var letzteAktion: String? = null

        fun installiere(context: Context) {
            val vorher = Thread.getDefaultUncaughtExceptionHandler()
            if (vorher is IdeenCrashHandler) return
            Thread.setDefaultUncaughtExceptionHandler(IdeenCrashHandler(context.applicationContext, vorher))
        }

        fun berichte(context: Context): List<File> =
            File(context.filesDir, "crashes").listFiles()
                ?.sortedByDescending(File::lastModified)
                .orEmpty()

        fun verwerfen(context: Context) {
            berichte(context).forEach { runCatching { it.delete() } }
        }
    }
}

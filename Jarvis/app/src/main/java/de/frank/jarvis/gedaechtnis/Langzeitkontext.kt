package de.frank.jarvis.gedaechtnis

import android.content.Context
import android.util.AtomicFile
import de.frank.jarvis.faehigkeit.Merkdatei
import java.io.File
import java.io.FileNotFoundException

/**
 * Franks Langzeitkontext: eine frei bearbeitbare Textdatei (etwa was ChatGPT sich über ihn gemerkt hat), die Jarvis bei
 * jeder Frage mitliest. Sie liegt nur hier auf dem Handy; der Mac liest und schreibt sie über das Werkzeug
 * jarvis_langzeitkontext, dadurch gibt es genau einen Stand. [stand] schützt vor dem Überschreiben einer neueren Fassung.
 */
object Langzeitkontext {
    const val HOECHSTENS = 200_000
    /** So viel davon steht in jedem Kontext; den Rest liest ein Modell bei Bedarf mit dem Werkzeug. */
    private const val IM_KONTEXT = 12_000

    private fun datei(context: Context) = File(context.applicationContext.filesDir, "langzeitkontext.txt")

    fun text(context: Context): String = synchronized(this) {
        try { String(AtomicFile(datei(context)).readFully(), Charsets.UTF_8) } catch (_: FileNotFoundException) { "" }
    }

    fun stand(context: Context): Long = datei(context).lastModified()

    /** Schreibt atomar und gibt den neuen Stand zurück. */
    fun ersetze(context: Context, text: String): Long = synchronized(this) {
        val af = AtomicFile(datei(context))
        val aus = af.startWrite()
        try {
            aus.write(text.toByteArray(Charsets.UTF_8))
            af.finishWrite(aus)
        } catch (e: Exception) {
            af.failWrite(aus)
            throw e
        }
        Merkdatei.zuletztGeaendert = System.currentTimeMillis()
        stand(context)
    }

    fun alsKontext(context: Context): String {
        val t = text(context).trim()
        if (t.isEmpty()) return ""
        val gekuerzt = t.length > IM_KONTEXT
        return "FRANKS LANGZEITKONTEXT (von Frank selbst gepflegte Textdatei, gilt dauerhaft als Hintergrundwissen über ihn; nicht ungefragt vorlesen" +
            (if (gekuerzt) "; hier gekürzt, vollständig mit jarvis_langzeitkontext" else "") + "):\n" + t.take(IM_KONTEXT) + "\n"
    }
}

package de.frank.jarvis.gedaechtnis

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import java.io.File
import java.io.FileNotFoundException

/**
 * Franks Langzeitkontext: eine frei bearbeitbare Textdatei (etwa was ChatGPT sich über ihn gemerkt hat), die Jarvis bei
 * jeder Frage mitliest. Sie liegt nur hier auf dem Handy; der Mac liest und schreibt sie über die Werkzeuge
 * jarvis_langzeitkontext_lesen und _ersetzen, dadurch gibt es genau einen Stand. [stand] schützt vor dem Überschreiben
 * einer neueren Fassung, die vorige Fassung bleibt als Sicherung liegen.
 */
object Langzeitkontext {
    const val HOECHSTENS = 200_000
    /** So viel davon steht in jedem Kontext; den Rest liest ein Modell bei Bedarf mit dem Werkzeug. */
    private const val IM_KONTEXT = 12_000

    private fun datei(context: Context) = File(context.applicationContext.filesDir, "langzeitkontext.txt")
    private fun sicherung(context: Context) = File(context.applicationContext.filesDir, "langzeitkontext-vorfassung.txt")

    private fun lies(f: File): String = try {
        String(AtomicFile(f).readFully(), Charsets.UTF_8)
    } catch (_: FileNotFoundException) {
        ""
    }

    /** Wirft bei einem Lesefehler: Wer speichern will, darf eine unlesbare Datei nicht für leer halten. */
    fun text(context: Context): String = synchronized(this) { lies(datei(context)) }

    fun vorfassung(context: Context): String = synchronized(this) { lies(sicherung(context)) }

    fun stand(context: Context): Long = datei(context).lastModified()

    /**
     * Schreibt atomar, aber nur, wenn die Datei noch auf dem Stand [basis] ist; sonst null. Prüfen und Schreiben stehen unter
     * derselben Sperre, damit Mac und Handy sich nicht gegenseitig überschreiben. Gibt den neuen Stand zurück.
     */
    fun ersetzeWennStand(context: Context, basis: Long, text: String): Long? = synchronized(this) {
        if (basis != stand(context)) return null
        val f = datei(context)
        if (f.exists()) schreibe(sicherung(context), lies(f))
        schreibe(f, text)
        // Dateizeiten lösen grob auf: Der neue Stand muss sich trotzdem vom alten unterscheiden.
        if (stand(context) <= basis) f.setLastModified(basis + 1)
        stand(context)
    }

    private fun schreibe(ziel: File, text: String) {
        val af = AtomicFile(ziel)
        val aus = af.startWrite()
        try {
            aus.write(text.toByteArray(Charsets.UTF_8))
            af.finishWrite(aus)
        } catch (e: Exception) {
            af.failWrite(aus)
            throw e
        }
    }

    /** Für jeden Kontextaufbau: Eine unlesbare Datei darf keine Antwort von Jarvis verhindern. */
    fun alsKontext(context: Context): String {
        val t = runCatching { text(context) }.onFailure { Log.w("Langzeitkontext", "nicht lesbar", it) }.getOrDefault("").trim()
        if (t.isEmpty()) return ""
        val gekuerzt = t.length > IM_KONTEXT
        return "FRANKS LANGZEITKONTEXT (von Frank selbst gepflegte Textdatei, gilt dauerhaft als Hintergrundwissen über ihn; nicht ungefragt vorlesen" +
            (if (gekuerzt) "; hier gekürzt, vollständig mit jarvis_langzeitkontext_lesen" else "") + "):\n" + t.take(IM_KONTEXT) + "\n"
    }
}

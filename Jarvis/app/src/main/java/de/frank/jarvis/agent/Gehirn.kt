package de.frank.jarvis.agent

import android.content.Context
import de.frank.jarvis.gedaechtnis.Lagebild

/**
 * Das Gesamtbild, mit dem Jarvis jede Frage betrachtet. Es liegt fertig im Live-Gedächtnis ([Lagebild]) bereit:
 * Ein Text für alle Wege. Der eigene Chat und die Agenten bekommen ihn in die Anweisung, das Plugin holt ihn mit
 * einem Aufruf (jarvis_kontext).
 */
object Gehirn {
    /** Wann das Plugin das Gesamtbild zuletzt geholt hat; danach richtet sich die Erinnerung in den Werkzeug-Ergebnissen. */
    @Volatile var zuletztGeholt = 0L
    /** Wann das Plugin zuletzt Gedächtnis oder Neuigkeiten mit einem Werkzeug-Ergebnis nachgereicht bekam. */
    @Volatile var zuletztNachgereicht = 0L

    suspend fun kontext(context: Context): String = Lagebild.kontext(context)
}

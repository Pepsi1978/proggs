package de.frank.ziele.data

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Die Kontextdatei auf dem Gerät (`files/kontext.txt`): was die KI über Frank wissen soll, eingesprochen oder getippt.
 * Sie geht bei jeder Auswertung mit. Jarvis kann sie über die Brücke lesen und ergänzen.
 */
object Kontext {
    private val _text = MutableStateFlow<String?>(null)

    private fun datei(context: Context) = File(context.applicationContext.filesDir, "kontext.txt")

    fun fluss(context: Context): StateFlow<String?> { if (_text.value == null) _text.value = lies(context); return _text.asStateFlow() }

    @Synchronized
    fun lies(context: Context): String = runCatching { datei(context).takeIf { it.exists() }?.readText(Charsets.UTF_8) }.getOrNull().orEmpty()

    /**
     * Speichert Getipptes, das auf dem Stand [basis] begonnen wurde. Kam inzwischen etwas hinten dazu (Diktat, Jarvis),
     * bleibt es erhalten und wird angehängt, statt überschrieben zu werden.
     */
    @Synchronized
    fun schreibeGetippt(context: Context, basis: String, text: String) {
        val jetzt = lies(context)
        val dazu = if (jetzt != basis && jetzt.startsWith(basis)) jetzt.substring(basis.length) else ""
        schreibe(context, if (dazu.isEmpty()) text else text.trimEnd() + dazu)
    }

    @Synchronized
    fun schreibe(context: Context, text: String) {
        val d = datei(context)
        val tmp = File(d.parentFile, "kontext.tmp")
        tmp.writeText(text, Charsets.UTF_8)
        if (!tmp.renameTo(d)) { d.writeText(text, Charsets.UTF_8); tmp.delete() }
        _text.value = text
    }

    /** Hängt einen neuen Absatz mit Datum an, so bleibt nachvollziehbar, wann etwas dazukam. Als Ganzes gesperrt: Diktat und Jarvis gleichzeitig verlieren nichts. */
    @Synchronized
    fun ergaenze(context: Context, absatz: String): String {
        val sauber = absatz.trim()
        if (sauber.isEmpty()) return lies(context)
        val stempel = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val alt = lies(context).trimEnd()
        val neu = (if (alt.isEmpty()) "" else "$alt\n\n") + "[$stempel] $sauber"
        schreibe(context, neu)
        return neu
    }
}

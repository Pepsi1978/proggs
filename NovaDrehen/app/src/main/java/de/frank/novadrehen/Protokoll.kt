package de.frank.novadrehen

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Merkt sich die letzten Entscheidungen des Dienstes, damit sie in der App sichtbar sind.
// So lässt sich ein Fehlverhalten am Handy per Bildschirmfoto belegen, ganz ohne Computer.
object Protokoll {
    private const val MAX = 60
    private val eintraege = ArrayDeque<String>()
    private val zeit = SimpleDateFormat("HH:mm:ss.SSS", Locale.GERMANY)

    @Synchronized
    fun neu(text: String) {
        Log.i("NovaDrehen", text)
        eintraege.addLast("${zeit.format(Date())}  $text")
        while (eintraege.size > MAX) eintraege.removeFirst()
    }

    // Neueste Zeile oben.
    @Synchronized
    fun alle(): String = if (eintraege.isEmpty()) "(noch leer)" else eintraege.reversed().joinToString("\n")
}

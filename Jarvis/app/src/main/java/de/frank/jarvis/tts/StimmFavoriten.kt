package de.frank.jarvis.tts

import android.content.Context

/**
 * Favoriten-Stimmen (langes Drücken in jeder Stimmenauswahl: Vorlesen, Benachrichtigungen) als `art_stimme`; sie stehen
 * in der Liste oben mit Stern. Gespeichert wie bisher unter `stimmproben`/`favoriten`, damit gesetzte Sterne bleiben.
 */
object StimmFavoriten {
    private const val DATEI = "stimmproben"
    private const val SCHLUESSEL = "favoriten"

    fun favoriten(context: Context): Set<String> =
        context.getSharedPreferences(DATEI, Context.MODE_PRIVATE).getStringSet(SCHLUESSEL, emptySet())!!.toSet()

    /** Schlüssel einer Stimme in [favoriten]. */
    fun favoritSchluessel(art: String, stimme: String) = art + "_" + stimme

    fun favoritUmschalten(context: Context, art: String, stimme: String): Set<String> {
        val schluessel = favoritSchluessel(art, stimme)
        val neu = favoriten(context).let { if (schluessel in it) it - schluessel else it + schluessel }
        context.getSharedPreferences(DATEI, Context.MODE_PRIVATE).edit().putStringSet(SCHLUESSEL, neu).apply()
        return neu
    }

    /** Hörproben des entfernten alten Live-Gesprächs (PCM-Dateien im Ordner `files/stimmproben`): niemand liest sie mehr. */
    fun altlastenEntfernen(context: Context) {
        runCatching { java.io.File(context.filesDir, "stimmproben").takeIf { it.isDirectory }?.deleteRecursively() }
    }
}

package de.frank.jarvis.data

import android.os.Build

/**
 * Auf welchem von Franks Geräten diese Installation läuft. Dieselbe APK läuft auf dem Handy (Galaxy Fold 8, „Jarvis“) und
 * auf dem Tablet (Galaxy Tab S9, „Jarvis Tablet“); am Server und im Abgleich sind beide eigene Geräte:
 * eigene Verbindung, eigene Plugin-Adresse, eigener Name in Ablage und Erinnerungen.
 *
 * Entschieden wird nach dem Modell, nie nach der Bildschirmbreite: Das aufgeklappte Fold ist so breit wie ein Tablet,
 * bliebe aber das Handy. Ein Gerät, das sich fälschlich als Handy meldete, würfe das echte Handy vom Server.
 */
object Geraet {
    /** Samsung-Tablets (Galaxy Tab: SM-X…, SM-T…, SM-P…). */
    val istTablet: Boolean = Build.MODEL.orEmpty().uppercase().let { it.startsWith("SM-X") || it.startsWith("SM-T") || it.startsWith("SM-P") }

    /** Kennung am Server (Kopfzeile X-Jarvis-Geraet): „handy“ oder „tablet“. */
    val kennung: String = if (istTablet) "tablet" else "handy"

    /** Name dieser Jarvis-Version in Ablage, Erinnerungen und Abgleich; muss zum Namen passen, den der Server aus der Kennung bildet. */
    val ablageName: String = if (istTablet) "Jarvis Tablet" else "Jarvis Handy"

    /** So nennt sich Jarvis hier gegenüber Frank und in der Plugin-Anmeldung. */
    val name: String = if (istTablet) "Jarvis Tablet" else "Jarvis"

    /** „Handy“ oder „Tablet“, für Sätze wie „auf dem Tablet“. */
    val wort: String = if (istTablet) "Tablet" else "Handy"

    const val KOPFZEILE = "X-Jarvis-Geraet"
}

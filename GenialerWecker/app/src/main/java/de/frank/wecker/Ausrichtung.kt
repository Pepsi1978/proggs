package de.frank.wecker

import android.app.Activity
import android.content.pm.ActivityInfo

/**
 * Die gewählte Bildschirmausrichtung. Die Werte stammen aus den vorhandenen Einstellungen
 * (`SecureSettings.ausrichtung`, erlaubt sind „hochformat", „querformat", „automatisch");
 * „automatisch" ist der Vorgabewert und entspricht dem bisherigen Verhalten.
 *
 * Android entscheidet mit: Ab einer kleinsten Fensterbreite von 600 dp ignorieren aktuelle
 * Versionen `setRequestedOrientation()` — auf dem aufgeklappten Foldable, im geteilten Fenster
 * und auf Tablets kann die Auswahl deshalb wirkungslos bleiben. Das App-seitige Opt-out steht
 * als `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` im Manifest und läuft mit API 37 aus.
 */
object Ausrichtung {
    const val HOCHFORMAT = "hochformat"
    const val QUERFORMAT = "querformat"
    const val AUTOMATISCH = "automatisch"

    val optionen = listOf(
        AUTOMATISCH to "Automatisch",
        HOCHFORMAT to "Hochformat",
        QUERFORMAT to "Querformat",
    )

    private fun angefordert(wert: String): Int = when (wert) {
        HOCHFORMAT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        QUERFORMAT -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    /**
     * Setzt die Ausrichtung nur bei einer tatsächlichen Abweichung. Ein Schreibzugriff auf
     * `requestedOrientation` kann eine neue Konfiguration auslösen; ein unbedingtes Setzen bei jedem
     * Durchlauf würde daraus eine Schleife machen.
     */
    fun anwenden(activity: Activity, wert: String) {
        val ziel = angefordert(wert)
        if (activity.requestedOrientation != ziel) activity.requestedOrientation = ziel
    }
}

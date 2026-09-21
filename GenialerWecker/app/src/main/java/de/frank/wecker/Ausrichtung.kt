package de.frank.wecker

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.layout

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

/**
 * Rückfallebene für große Displays: Auf dem aufgeklappten Foldable übergeht Android die angeforderte
 * Ausrichtung teils trotz Opt-out — das Fenster dreht sich mit, obwohl Hoch- oder Querformat fest
 * gewählt ist. Passt die Fensterform dann nicht zur Wahl, dreht diese Hülle den Inhalt um genau den
 * Winkel zurück, um den das System gedreht hat. Der Inhalt bleibt damit fest zum Gerät stehen.
 *
 * Solange Android die Anforderung einhält, gibt es nie eine Abweichung und die Hülle tut nichts.
 * Bei „automatisch“ ist sie ebenfalls wirkungslos.
 */
@androidx.compose.runtime.Composable
fun AusrichtungsSperre(wert: String, inhalt: @androidx.compose.runtime.Composable () -> Unit) {
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val view = androidx.compose.ui.platform.LocalView.current
    val quer = config.screenWidthDp > config.screenHeightDp
    val drehung = androidx.compose.runtime.remember(config) {
        @Suppress("DEPRECATION")
        val anzeige = if (android.os.Build.VERSION.SDK_INT >= 30) view.display
            else (view.context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay
        anzeige?.rotation ?: 0
    }
    // Die letzte Drehlage, in der Fenster und Wahl zusammenpassten — sie ist der Bezugspunkt.
    var passendeDrehung by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(-1) }
    val abweichung = (wert == Ausrichtung.HOCHFORMAT && quer) || (wert == Ausrichtung.QUERFORMAT && !quer)
    if (!abweichung) {
        androidx.compose.runtime.SideEffect { passendeDrehung = drehung }
        inhalt()
        return
    }
    // Android dreht die Darstellung um (aktuell − passend) × 90° im Uhrzeigersinn; zurück geht es gegen ihn.
    val bezug = if (passendeDrehung >= 0) passendeDrehung else (drehung + 1) % 4
    var grad = ((bezug - drehung) * 90) % 360
    if (grad > 180) grad -= 360
    if (grad <= -180) grad += 360
    if (grad == 0) grad = 90
    androidx.compose.foundation.layout.Box(
        androidx.compose.ui.Modifier.fillMaxSize().layout { messbar, grenzen ->
            val b = grenzen.maxWidth
            val h = grenzen.maxHeight
            val platz = messbar.measure(androidx.compose.ui.unit.Constraints.fixed(h, b))
            layout(b, h) {
                platz.placeWithLayer((b - platz.width) / 2, (h - platz.height) / 2) { rotationZ = grad.toFloat() }
            }
        },
    ) { inhalt() }
}


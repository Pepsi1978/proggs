package de.frank.stacklabor.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Das Theme der App.
 *
 * Der Wechsel zwischen Hell und Dunkel ueberblendet alle Farbrollen in 420 ms
 * (M-19, gemessen als `transition` auf `background-color`, `color`, `border-color`).
 * Ein harter Sprung waere bei Glasflaechen und Auren ein sichtbarer Bruch.
 */
@Composable
fun StackLaborTheme(
    dunkel: Boolean,
    bewegungReduziert: Boolean,
    inhalt: @Composable () -> Unit,
) {
    val ziel = if (dunkel) FarbenDunkel else FarbenHell
    val spec = if (bewegungReduziert) Bewegungen.sofort<androidx.compose.ui.graphics.Color>()
    else Bewegungen.erscheinung<androidx.compose.ui.graphics.Color>()

    // Jede Rolle blendet einzeln um — so wandert die ganze Oberflaeche gemeinsam.
    val farben = StackFarben(
        grund = animateColorAsState(ziel.grund, spec, label = "grund").value,
        flaeche = animateColorAsState(ziel.flaeche, spec, label = "flaeche").value,
        erhoeht = animateColorAsState(ziel.erhoeht, spec, label = "erhoeht").value,
        rand = animateColorAsState(ziel.rand, spec, label = "rand").value,
        text = animateColorAsState(ziel.text, spec, label = "text").value,
        textSchwach = animateColorAsState(ziel.textSchwach, spec, label = "textSchwach").value,
        akzent = animateColorAsState(ziel.akzent, spec, label = "akzent").value,
        akzent2 = animateColorAsState(ziel.akzent2, spec, label = "akzent2").value,
        gruen = animateColorAsState(ziel.gruen, spec, label = "gruen").value,
        gelb = animateColorAsState(ziel.gelb, spec, label = "gelb").value,
        gelbText = animateColorAsState(ziel.gelbText, spec, label = "gelbText").value,
        rot = animateColorAsState(ziel.rot, spec, label = "rot").value,
        rotKraeftig = animateColorAsState(ziel.rotKraeftig, spec, label = "rotKraeftig").value,
        grau = animateColorAsState(ziel.grau, spec, label = "grau").value,
        loeslichWasser = animateColorAsState(ziel.loeslichWasser, spec, label = "lwas").value,
        loeslichFettFuellung = animateColorAsState(ziel.loeslichFettFuellung, spec, label = "lfetf").value,
        loeslichFettRand = animateColorAsState(ziel.loeslichFettRand, spec, label = "lfetr").value,
        deaktiviert = animateColorAsState(ziel.deaktiviert, spec, label = "deakt").value,
        schatten = ziel.schatten,
        schatten2 = ziel.schatten2,
        glas = animateColorAsState(ziel.glas, spec, label = "glas").value,
        istHell = ziel.istHell,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val fenster = (view.context as Activity).window
            fenster.statusBarColor = ziel.erhoeht.value.toInt()
            fenster.navigationBarColor = ziel.grund.value.toInt()
            WindowCompat.getInsetsController(fenster, view).apply {
                isAppearanceLightStatusBars = ziel.istHell
                isAppearanceLightNavigationBars = ziel.istHell
            }
        }
    }

    // Material 3 bekommt dieselben Farben, damit eingebaute Bausteine nicht ausscheren.
    val m3 = if (dunkel) {
        darkColorScheme(
            primary = farben.akzent, onPrimary = farben.grund,
            background = farben.grund, onBackground = farben.text,
            surface = farben.flaeche, onSurface = farben.text,
            surfaceVariant = farben.erhoeht, onSurfaceVariant = farben.textSchwach,
            outline = farben.rand, error = farben.rot,
        )
    } else {
        lightColorScheme(
            primary = farben.akzent, onPrimary = androidx.compose.ui.graphics.Color.White,
            background = farben.grund, onBackground = farben.text,
            surface = farben.flaeche, onSurface = farben.text,
            surfaceVariant = farben.erhoeht, onSurfaceVariant = farben.textSchwach,
            outline = farben.rand, error = farben.rot,
        )
    }

    CompositionLocalProvider(
        LocalStackFarben provides farben,
        LocalBewegungReduziert provides bewegungReduziert,
    ) {
        MaterialTheme(colorScheme = m3, typography = StackTypografie, content = inhalt)
    }
}

/** Kurzzugriff auf die Farbrollen: `Farben.akzent` statt des langen Pfades. */
val Farben: StackFarben
    @Composable get() = LocalStackFarben.current

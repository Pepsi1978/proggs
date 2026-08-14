package de.frank.experimente.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.ui.components.UntereLeiste
import de.frank.experimente.ui.screens.Auswertung
import de.frank.experimente.ui.screens.Einstellungen
import de.frank.experimente.ui.screens.Erkenntnisse
import de.frank.experimente.ui.screens.Gespraech
import de.frank.experimente.ui.screens.Heute
import de.frank.experimente.ui.screens.Logbuch
import de.frank.experimente.ui.screens.Merkliste
import de.frank.experimente.ui.screens.Monitor
import de.frank.experimente.ui.screens.Selbstbild
import de.frank.experimente.ui.screens.WuenscheUndZiele
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.LocalEffektstufe
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.dauer
import kotlin.math.abs

/**
 * Der Wechsel zwischen den Bildschirmen.
 *
 * **E-12 · M-91** — der abgehende Bildschirm wird auf 96 % verkleinert, der neue kommt auf
 * 104 % beginnend herein; 260 ms mit `cubic-bezier(.2, 0, 0, 1)`. Das ist der `hoch`-Verlauf
 * des Entwurfs (`opacity 0 → 1`, `translateY(14px) scale(.98) → 0/1`).
 *
 * Bewusst **kein** Pager: der würde das Schieben erzwingen. Das Wischen (F-27) läuft deshalb
 * als eigene Geste, die den Bildschirm wechselt statt ihn mitzuziehen — über alle **sechs**
 * Hauptbildschirme in der Reihenfolge der unteren Leiste.
 */
@Composable
fun Navigation(modell: AppViewModel) {
    val ziel by modell.ziel.collectAsStateWithLifecycle()
    val kannZurueck by modell.kannZurueck.collectAsStateWithLifecycle()
    val farben = LocalFarben.current
    val stufe = LocalEffektstufe.current
    val wechsel = dauer(Bewegung.WECHSEL, stufe)

    // Die Zurück-Taste und die Wischgeste des Geräts führen denselben Weg zurück wie der
    // Pfeil in der Kopfleiste. Auf dem Monitor bleibt sie unangetastet und verlässt die App.
    BackHandler(enabled = kannZurueck) {
        // Ein offener Selbstbild-Text wird auch auf diesem Weg gesichert.
        if (ziel == Ziel.SELBSTBILD) modell.speichereSelbstbild()
        modell.zurueck()
    }

    val wischbar = ziel in Ziel.hauptreihe

    Box(
        Modifier
            .fillMaxSize()
            .background(farben.grund)
            .then(
                if (!wischbar) Modifier else Modifier.pointerInput(ziel) {
                    var strecke = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { strecke = 0f },
                        onDragEnd = {
                            if (abs(strecke) > SCHWELLE) modell.wische(nachLinks = strecke < 0)
                        },
                    ) { _, weite -> strecke += weite }
                }
            ),
    ) {
        AnimatedContent(
            targetState = ziel,
            transitionSpec = {
                (
                    fadeIn(tween(wechsel, easing = Bewegung.ruhig)) +
                        scaleIn(tween(wechsel, easing = Bewegung.ruhig), initialScale = 1.04f)
                    ) togetherWith (
                    fadeOut(tween(wechsel, easing = Bewegung.ruhig)) +
                        scaleOut(tween(wechsel, easing = Bewegung.ruhig), targetScale = 0.96f)
                    )
            },
            label = "bildschirmwechsel",
        ) { welcher ->
            when (welcher) {
                Ziel.MONITOR -> Monitor(modell)
                Ziel.HEUTE -> Heute(modell)
                Ziel.GESPRAECH -> Gespraech(modell)
                Ziel.AUSWERTUNG -> Auswertung(modell)
                Ziel.ZIELE -> WuenscheUndZiele(modell)
                Ziel.MERKLISTE -> Merkliste(modell)
                Ziel.ERKENNTNISSE -> Erkenntnisse(modell)
                Ziel.LOGBUCH -> Logbuch(modell)
                Ziel.EINSTELLUNGEN -> Einstellungen(modell)
                Ziel.SELBSTBILD -> Selbstbild(modell)
            }
        }

        // Die untere Leiste liegt **über** dem Bildschirmwechsel und wechselt deshalb nicht
        // mit: sie steht fest, während der Bildschirm darunter tauscht. Lag sie im
        // Bildschirm, wurde sie bei jedem Wisch mitskaliert und mitgeblendet — sie sprang.
        // Sichtbar ist sie auf den sechs Hauptbildschirmen; die Bildschirme lassen ihren
        // Platz mit einem gleich hohen Abstandhalter frei (`Bildschirmgeruest`).
        AnimatedVisibility(
            visible = wischbar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(wechsel, easing = Bewegung.ruhig)),
            exit = fadeOut(tween(wechsel, easing = Bewegung.ruhig)),
        ) {
            UntereLeiste(jetzt = ziel, beiWahl = modell::gehe)
        }
    }
}

/** Ab dieser Wischweite wird gewechselt — darunter war es ein Antippen. */
private const val SCHWELLE = 90f

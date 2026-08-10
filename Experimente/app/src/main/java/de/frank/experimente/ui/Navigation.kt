package de.frank.experimente.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.experimente.ui.screens.Auswertung
import de.frank.experimente.ui.screens.Einstellungen
import de.frank.experimente.ui.screens.Erkenntnisse
import de.frank.experimente.ui.screens.Gespraech
import de.frank.experimente.ui.screens.Heute
import de.frank.experimente.ui.screens.Logbuch
import de.frank.experimente.ui.screens.Merkliste
import de.frank.experimente.ui.screens.Selbstbild
import de.frank.experimente.ui.screens.WuenscheUndZiele
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.dauer
import de.frank.experimente.ui.theme.grundAuren
import kotlin.math.abs

/**
 * Der Wechsel zwischen den Bildschirmen.
 *
 * M-63: zwischen den Hauptbildschirmen reines Überblenden, 200 ms, `cubic-bezier(.4,0,.6,1)`.
 * Bewusst **kein** Pager: der würde das Schieben erzwingen und M-63 widersprechen. Das
 * Wischen (F-27) läuft deshalb als eigene Geste, die den Bildschirm wechselt statt ihn
 * mitzuziehen.
 */
@Composable
fun Navigation(modell: AppViewModel) {
    val ziel by modell.ziel.collectAsStateWithLifecycle()
    val farben = LocalFarben.current
    val blenden = dauer(Bewegung.MITTEL)

    // Nur die fünf Hauptbildschirme reagieren auf Wischen (F-27 Regeln).
    val wischbar = ziel in Ziel.hauptreihe

    Box(
        Modifier
            .fillMaxSize()
            .background(farben.grund)
            .grundAuren(farben)
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
                fadeIn(tween(blenden, easing = Bewegung.blenden)) togetherWith
                    fadeOut(tween(blenden, easing = Bewegung.blenden))
            },
            label = "bildschirmwechsel",
        ) { welcher ->
            when (welcher) {
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
    }
}

/** Ab dieser Wischweite wird gewechselt — darunter war es ein Antippen. */
private const val SCHWELLE = 90f

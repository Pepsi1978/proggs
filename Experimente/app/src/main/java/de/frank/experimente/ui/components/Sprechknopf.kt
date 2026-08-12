package de.frank.experimente.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import de.frank.experimente.ui.theme.LocalEffektstufe
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.Symbole

/** Die vollrunde Form, die der Entwurf als `border-radius: 9999px` schreibt. */
private val Vollrund = RoundedCornerShape(percent = 50)

/**
 * Der große Sprechknopf: **88 × 88 dp**, vollrund, Fläche *Aktion*, 1 dp Rand
 * `color-mix(Text 28%, transparent)`, darunter der Schein
 * `0 10px 30px color-mix(Aktion 36%)`. Das Mikrofon misst 30 dp und steht in *Auf Aktion*.
 *
 * Während der Aufnahme wird es zum Stopp-Quadrat — der Zustand steht also als **Form** da,
 * nicht nur als Farbe.
 *
 * **E-19 · Pulsringe:** drei Ringe steigen gestaffelt aus dem Knopf auf, je 2 dp Rand in
 * *Aktion*, `puls` über 1800 ms `ease-out` endlos mit 0 / 400 / 800 ms Versatz:
 * `scale(1) opacity .5` → `scale(1.9) opacity 0`.
 * Rückfallebene: ein einzelner, stehender Ring — die Aufnahme bleibt erkennbar.
 */
@Composable
fun Sprechknopf(
    nimmtAuf: Boolean,
    beiKlick: () -> Unit,
    modifier: Modifier = Modifier,
    beschriftung: String = "Lage einsprechen",
) {
    val farben = LocalFarben.current
    val stufe = LocalEffektstufe.current
    val quelle = merkeDruck()

    Box(modifier.size(168.dp), contentAlignment = Alignment.Center) {
        if (nimmtAuf) {
            if (stufe.dauerbewegung) {
                Pulsring(0)
                Pulsring(400)
                Pulsring(800)
            } else if (stufe.dauerbewegungSichtbar) {
                Box(
                    Modifier
                        .size(88.dp)
                        .alpha(0.45f)
                        .clip(Vollrund)
                        .border(2.dp, farben.aktion, Vollrund),
                )
            }
        }
        Box(
            Modifier
                .size(88.dp)
                .federdruck(quelle)
                .shadow(
                    elevation = 10.dp,
                    shape = Vollrund,
                    clip = false,
                    ambientColor = farben.aktion.copy(alpha = 0.36f),
                    spotColor = farben.aktion.copy(alpha = 0.36f),
                )
                .clip(Vollrund)
                .background(farben.aktion)
                .border(1.dp, farben.text.copy(alpha = 0.28f), Vollrund)
                .clickable(interactionSource = quelle, indication = null, onClick = beiKlick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (nimmtAuf) Symbole.Stopp else Symbole.Mikrofon,
                contentDescription = if (nimmtAuf) "Aufnahme beenden" else beschriftung,
                tint = farben.aufAktion,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

/** Ein Ring von `E-19`, mit seinem Versatz. */
@Composable
private fun Pulsring(versatz: Int) {
    val farben = LocalFarben.current
    val takt = rememberInfiniteTransition(label = "puls$versatz")
    val fortschritt by takt.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800, delayMillis = versatz, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulsweg$versatz",
    )
    Box(
        Modifier
            .size(88.dp)
            .scale(1f + 0.9f * fortschritt)
            .alpha(0.5f * (1f - fortschritt))
            .clip(Vollrund)
            .border(2.dp, farben.aktion, Vollrund),
    )
}

/**
 * **E-18 — die Aufnahme-Wellenform.** Ein Kranz aus 24 Balken zeigt den Mikrofonpegel:
 * je 3 dp breit, 36 dp hoch, 3 dp Abstand, Radius 2 dp, Fläche *Aktion*, von unten
 * skalierend (`transform-origin: bottom`).
 *
 * Der Entwurf gibt jedem Balken seine eigene Dauer `320 + (i·97) mod 420` ms und seinen
 * Versatz `(i·63) mod 380` ms; die Bewegung läuft `ease-in-out` endlos wechselnd zwischen
 * `scaleY(.22)` und `scaleY(1)`.
 *
 * Rückfallebene: der Kranz steht still — der atmende Ring (`M-02`) trägt den Zustand.
 */
@Composable
fun Wellenform(modifier: Modifier = Modifier) {
    val farben = LocalFarben.current
    val stufe = LocalEffektstufe.current
    val takt = rememberInfiniteTransition(label = "welle")
    Row(
        modifier.height(40.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(24) { nr ->
            val dauerMs = 320 + (nr * 97) % 420
            val versatz = (nr * 63) % 380
            val hoehe by takt.animateFloat(
                initialValue = 0.22f,
                targetValue = if (stufe.dauerbewegung) 1f else 0.22f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = dauerMs,
                        delayMillis = versatz,
                        easing = de.frank.experimente.ui.theme.Bewegung.atmen,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "balken$nr",
            )
            Box(
                Modifier
                    .width(3.dp)
                    .height(36.dp * hoehe)
                    .clip(RoundedCornerShape(2.dp))
                    .background(farben.aktion)
                    .alpha(stufe.dauerbewegungDeckkraft),
            )
        }
    }
}

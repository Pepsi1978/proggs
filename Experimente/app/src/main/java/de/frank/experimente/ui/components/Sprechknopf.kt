package de.frank.experimente.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.Formen
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.Symbole
import de.frank.experimente.ui.theme.bewegungReduziert
import de.frank.experimente.ui.theme.sprechknopfVerlauf

/**
 * Der große Sprechknopf: 88 × 88 dp, vollrund, Verlauf 145°, farbiger Schein, Lichtsaum oben.
 * Darum der Aufnahme-Ring, 104 × 104 mit 2 dp Rand.
 *
 * **M-02** — der Ring atmet: 3200 ms, `cubic-bezier(0.42, 0, 0.58, 1)`, endlos, wechselnd.
 * Er läuft nur während der Aufnahme; bei reduzierter Bewegung steht er still (dann bleibt der
 * Ring sichtbar, damit die Aufnahme trotzdem erkennbar ist).
 */
@Composable
fun Sprechknopf(
    nimmtAuf: Boolean,
    beiKlick: () -> Unit,
    modifier: Modifier = Modifier,
    beschriftung: String = "Lage einsprechen",
) {
    val farben = LocalFarben.current
    val ruhig = bewegungReduziert()

    val atem = rememberInfiniteTransition(label = "atem")
    val weite by atem.animateFloat(
        initialValue = 1f,
        targetValue = if (nimmtAuf && !ruhig) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(Bewegung.ATEM, easing = Bewegung.atmen),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "atemweite",
    )
    val schimmer by atem.animateFloat(
        initialValue = 0.9f,
        targetValue = if (nimmtAuf && !ruhig) 0.35f else 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(Bewegung.ATEM, easing = Bewegung.atmen),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "atemschimmer",
    )

    Box(modifier.size(104.dp), contentAlignment = Alignment.Center) {
        if (nimmtAuf) {
            Box(
                Modifier
                    .size(104.dp)
                    .scale(weite)
                    .alpha(schimmer)
                    .clip(Formen.vollrund)
                    .border(2.dp, farben.aktion, Formen.vollrund),
            )
        }
        Box(
            Modifier
                .size(88.dp)
                // `0 16px 32px` Schlagschatten und `0 0 24px` farbiger Schein aus dem Entwurf.
                .shadow(
                    elevation = 16.dp,
                    shape = Formen.vollrund,
                    clip = false,
                    ambientColor = farben.aktion.copy(alpha = 0.30f),
                    spotColor = farben.aktion.copy(alpha = 0.30f),
                )
                .clip(Formen.vollrund)
                .background(sprechknopfVerlauf(farben))
                .border(1.dp, farben.text.copy(alpha = 0.28f), Formen.vollrund)
                .clickable(onClick = beiKlick),
            contentAlignment = Alignment.Center,
        ) {
            // Der innere Ring, 6 dp von der Kante: im Entwurf hebt er die Wölbung heraus.
            // Ein `inset`-Schatten hat in Compose kein Gegenstück, also nachgezeichnet.
            Box(
                Modifier
                    .size(76.dp)
                    .clip(Formen.vollrund)
                    .border(1.dp, farben.text.copy(alpha = 0.20f), Formen.vollrund),
            )
            Icon(
                imageVector = Symbole.LageEinsprechen,
                contentDescription = beschriftung,
                tint = farben.grund,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

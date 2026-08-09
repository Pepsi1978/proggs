package de.frank.experimente.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.LocalAppColors
import de.frank.experimente.ui.theme.LocalReduzierteBewegung
import de.frank.experimente.ui.theme.Mass
import de.frank.experimente.ui.theme.aktionsVerlauf
import de.frank.experimente.ui.theme.schattenAktion

/**
 * Der Sprechknopf — das am häufigsten benutzte Bauteil der App.
 *
 * **M-02** Läuft die Aufnahme, atmet ein Ring um den Knopf: 100 % → 108 % → 100 %,
 * Deckkraft 40 % → 15 % → 40 %, 3200 ms `atmen`, endlos, alternierend.
 * Das ist die **einzige** Dauerbewegung der ganzen App.
 *
 * **M-03** Beim Start **und** beim Ende der Aufnahme vibriert das Gerät kurz — damit Frank
 * auch ohne Hinsehen weiß, dass der Knopf läuft.
 *
 * Bei reduzierter Bewegung entfällt das Atmen; der laufende Zustand wird dann über die
 * Farbe gezeigt. Die Vibration bleibt — sie ist keine Bewegung.
 */
@Composable
fun Sprechknopf(
    laeuftAufnahme: Boolean,
    onKlick: () -> Unit,
    modifier: Modifier = Modifier,
    gross: Boolean = true,
    aktiv: Boolean = true,
) {
    val farben = LocalAppColors.current
    val reduziert = LocalReduzierteBewegung.current
    val view = LocalView.current
    val durchmesser: Dp = if (gross) Mass.sprechknopfGross else Mass.sprechknopfKlein

    val ringSkalierung: Float
    val ringDeckkraft: Float
    if (laeuftAufnahme && !reduziert) {
        val uebergang = rememberInfiniteTransition(label = "M-02")
        val puls by uebergang.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(Bewegung.ATMEN_MS, easing = Bewegung.atmen),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "M-02-puls",
        )
        ringSkalierung = 1f + 0.08f * puls
        ringDeckkraft = 0.40f - 0.25f * puls
    } else {
        ringSkalierung = 1f
        ringDeckkraft = if (laeuftAufnahme) 0.40f else 0f
    }

    Box(modifier = modifier.size(durchmesser * 1.16f), contentAlignment = Alignment.Center) {
        if (ringDeckkraft > 0f) {
            Box(
                Modifier
                    .size(durchmesser)
                    .scale(ringSkalierung)
                    .border(2.dp, farben.aktion.copy(alpha = ringDeckkraft), CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(durchmesser)
                .then(if (aktiv) Modifier.schattenAktion(farben, CircleShape) else Modifier)
                .clip(CircleShape)
                .then(
                    if (aktiv) {
                        Modifier.background(aktionsVerlauf(farben))
                    } else {
                        Modifier.background(farben.erhoeht)
                    }
                )
                .border(1.dp, farben.text.copy(alpha = 0.28f), CircleShape)
                .clickable(
                    enabled = aktiv,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    // M-03 — kurze Rückmeldung bei Start UND Ende
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onKlick()
                },
            contentAlignment = Alignment.Center,
        ) {
            // Der innere Ring, 6 dp eingerückt (`::before` im Design), Deckkraft 48 %.
            Box(
                Modifier
                    .size(durchmesser - 12.dp)
                    .border(1.dp, farben.grund.copy(alpha = 0.36f * 0.48f), CircleShape),
            )
            Icon(
                imageVector = if (laeuftAufnahme) Icons.Filled.Stop else Icons.Outlined.Mic,
                contentDescription = if (laeuftAufnahme) "Aufnahme beenden" else "Sprechen",
                tint = if (aktiv) farben.grund else farben.blass,
                modifier = Modifier.size(if (gross) 28.dp else 24.dp),
            )
        }
    }
}

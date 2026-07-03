package de.frank.entropyreducer.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

enum class MicState {
    IDLE,
    RECORDING,
    PROCESSING,
}

/**
 * Mic-Button gemäß Spec §4.2 + §8.1 und Referenzbildern.
 * - Idle: Atmen-Effect (Scale 1.0 ↔ 1.04, 2 Sek)
 * - Recording: pulsierender konzentrischer Ring + roter Mic-Icon
 * - Processing: Spinner
 *
 * Tap und Long-Press werden vom Aufrufer gehandhabt.
 */
@Composable
fun MicButton(
    state: MicState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    accentColor: Color? = null,
) {
    val infinite = rememberInfiniteTransition(label = "micPulse")

    val idleScale by
        infinite.animateFloat(
            initialValue = 1f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
            label = "idleBreath",
        )
    val recordingPulse by
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
            label = "recordingPulse",
        )

    val targetScale =
        when (state) {
            MicState.IDLE -> idleScale
            MicState.RECORDING -> 1.05f
            MicState.PROCESSING -> 1f
        }

    // Mic-Tint pro Tab (Frank-Wunsch 2026-05-17): Bottom-Bar gibt ueber
    // [accentColor] die aktive Tab-Farbe vor. Ohne Override fallen wir auf den
    // Cosmos-Primaer-Akzent zurueck.
    val baseAccent = accentColor ?: LocalCosmos.current.accent
    val accentBrush =
        remember(baseAccent) {
            Brush.radialGradient(0f to baseAccent, 1f to baseAccent.copy(alpha = 0.65f))
        }
    Box(modifier = modifier.size(size + 32.dp), contentAlignment = Alignment.Center) {
        // Pulsierende Ringe bei Aufnahme
        if (state == MicState.RECORDING) {
            val ringScale = 1f + recordingPulse * 0.6f
            val ringAlpha = (1f - recordingPulse).coerceIn(0f, 1f)
            Box(
                modifier =
                    Modifier.size(size)
                        .graphicsLayer {
                            scaleX = ringScale
                            scaleY = ringScale
                            alpha = ringAlpha * 0.5f
                        }
                        .clip(CircleShape)
                        .background(baseAccent.copy(alpha = 0.3f))
            )
        }

        // Hauptbutton
        Box(
            modifier =
                Modifier.size(size)
                    .scale(targetScale)
                    .clip(CircleShape)
                    .background(accentBrush)
                    .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                MicState.IDLE ->
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Aufnahme starten",
                        tint = CosmosColors.BgDark,
                        modifier = Modifier.size(size * 0.42f),
                    )
                MicState.RECORDING ->
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Aufnahme beenden",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.40f),
                    )
                MicState.PROCESSING ->
                    CircularProgressIndicator(
                        color = CosmosColors.BgDark,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(size * 0.5f).padding(4.dp),
                    )
            }
        }
    }
}

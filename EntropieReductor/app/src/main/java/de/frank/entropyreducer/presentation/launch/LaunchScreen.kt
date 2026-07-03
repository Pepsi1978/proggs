package de.frank.entropyreducer.presentation.launch

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.entropyreducer.BuildConfig
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Startbildschirm der App (Frank-Wunsch 2026-05-09): zeigt prominent den Variant-Label
 * (Debugversion / Performance Version / Release Version) damit Frank die parallel installierten
 * Builds sofort unterscheiden kann.
 *
 * Layout: zentriertes Logo + Titel + Variant-Label + Start-Button, ueber einem dezenten
 * Verlaufs-Hintergrund passend zum Cosmos-Theme. Tap auf "Start" ruft onStart() auf — der
 * ContainerComposable wechselt dann zur eigentlichen App.
 */
@Composable
fun LaunchScreen(onStart: () -> Unit) {
    val cosmos = LocalCosmos.current
    val variantLabel = BuildConfig.VARIANT_LABEL
    val versionName = BuildConfig.VERSION_NAME
    val versionUpdatedAt = BuildConfig.VERSION_UPDATED_AT

    // Sanfter Atemeffekt am Logo, damit der Bildschirm lebendig wirkt.
    val infinite = rememberInfiniteTransition(label = "launchPulse")
    val logoScale by
        infinite.animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
            label = "logoBreath",
        )

    val backgroundBrush =
        remember(cosmos.isDark) {
            if (cosmos.isDark) {
                Brush.verticalGradient(
                    listOf(CosmosColors.BgDark, CosmosColors.BgDarkAccent, CosmosColors.BgDark)
                )
            } else {
                Brush.verticalGradient(
                    listOf(CosmosColors.BgLight, CosmosColors.BgLightAccent, CosmosColors.BgLight)
                )
            }
        }

    val systemPadding: PaddingValues = WindowInsets.systemBars.asPaddingValues()

    Box(
        modifier = Modifier.fillMaxSize().background(backgroundBrush),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(systemPadding).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier.size(112.dp)
                        .graphicsLayer {
                            scaleX = logoScale
                            scaleY = logoScale
                        }
                        .background(
                            brush =
                                Brush.radialGradient(
                                    0f to LocalCosmos.current.accent,
                                    1f to LocalCosmos.current.accent.copy(alpha = 0.55f),
                                ),
                            shape = RoundedCornerShape(56.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Entropie Reduktor",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = cosmos.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))

            // Variant-Label als prominentes Pill — damit Frank Debug und
            // Performance auf den ersten Blick unterscheiden kann.
            Box(
                modifier =
                    Modifier.background(
                            color = LocalCosmos.current.accent.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50),
                        )
                        .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = variantLabel,
                    color = LocalCosmos.current.accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Version $versionName ($versionUpdatedAt)",
                fontSize = 13.sp,
                color = cosmos.textSecondary,
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(28.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = LocalCosmos.current.accent,
                        contentColor = Color.Black,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(10.dp))
                Text(text = "Start", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

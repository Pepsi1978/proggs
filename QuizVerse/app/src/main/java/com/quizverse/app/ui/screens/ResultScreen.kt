package com.quizverse.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.quizverse.app.ui.components.ParticleEffect
import com.quizverse.app.ui.navigation.Screen
import com.quizverse.app.ui.theme.Gold
import com.quizverse.app.ui.theme.GoldDark
import com.quizverse.app.ui.theme.GoldLight
import com.quizverse.app.ui.theme.GlassBorder
import com.quizverse.app.ui.theme.GlassWhite
import com.quizverse.app.ui.theme.Primary
import com.quizverse.app.ui.theme.Secondary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

// Confetti particle data for legacy inline confetti
private data class ConfettiParticle(
    val x: Float,
    val color: Color,
    val size: Float,
    val speedFactor: Float,
)

private val confettiColors = listOf(
    Gold, GoldLight, GoldDark,
    Color(0xFF6C63FF), Color(0xFFFF6584),
    Color(0xFF43E97B), Color(0xFF3B82F6),
    Color(0xFFEC4899), Color(0xFF14B8A6),
)

private val confettiParticles = List(50) {
    ConfettiParticle(
        x           = Random.nextFloat(),
        color       = confettiColors[it % confettiColors.size],
        size        = Random.nextFloat() * 12f + 5f,
        speedFactor = Random.nextFloat() * 0.5f + 0.75f,
    )
}

/** Returns the rank tier (0=bronze, 1=silver, 2=gold) and emoji based on percent. */
private fun rankTier(percent: Int): Triple<Int, String, String> = when {
    percent >= 80 -> Triple(2, "Hervorragend!", "🏆")
    percent >= 60 -> Triple(1, "Sehr gut!", "🎉")
    percent >= 30 -> Triple(0, "Gut gemacht!", "👍")
    else          -> Triple(-1, "Weiter üben!", "😅")
}

/** Gradient colors for each rank tier. */
private fun tierGradient(tier: Int): List<Color> = when (tier) {
    2    -> listOf(Color(0xFFB8860B), Gold, GoldLight, Color(0xFFFFF9C4))
    1    -> listOf(Color(0xFF9E9E9E), Color(0xFFE0E0E0), Color(0xFFF5F5F5))
    0    -> listOf(Color(0xFF8D4E15), Color(0xFFCD7F32), Color(0xFFE8A96B))
    else -> listOf(Primary, Color(0xFFA855F7))
}

/** Medal/emblem accent color for each tier. */
private fun tierColor(tier: Int): Color = when (tier) {
    2    -> Gold
    1    -> Color(0xFFC0C0C0)
    0    -> Color(0xFFCD7F32)
    else -> Primary
}

@Composable
fun ResultScreen(
    score: Int,
    total: Int,
    correct: Int,
    categoryId: Int,
    navController: NavHostController,
) {
    val percent = if (total > 0) (correct * 100) / total else 0
    val (tier, performanceLabel, performanceEmoji) = rankTier(percent)

    // Count-up animation for score
    var displayedScore by remember { mutableIntStateOf(0) }
    val cardScale  = remember { Animatable(0.5f) }
    val cardAlpha  = remember { Animatable(0f) }
    val buttonsAlpha = remember { Animatable(0f) }
    val statsAlpha = remember { Animatable(0f) }

    LaunchedEffect(score) {
        val steps = 50
        val stepDelay = 800L / steps
        val step = score / steps.toFloat()
        repeat(steps) { i ->
            delay(stepDelay)
            displayedScore = ((i + 1) * step).roundToInt().coerceAtMost(score)
        }
        displayedScore = score
    }

    LaunchedEffect(Unit) {
        delay(80)
        cardScale.animateTo(
            targetValue  = 1f,
            animationSpec = tween(600, easing = EaseOutBack),
        )
    }
    LaunchedEffect(Unit) {
        cardAlpha.animateTo(1f, animationSpec = tween(350))
        delay(300)
        statsAlpha.animateTo(1f, animationSpec = tween(400))
        delay(150)
        buttonsAlpha.animateTo(1f, animationSpec = tween(400))
    }

    // Infinite glow pulse for score
    val infiniteTransition = rememberInfiniteTransition(label = "resultGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 0.9f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )
    val confettiFall by infiniteTransition.animateFloat(
        initialValue  = -0.1f,
        targetValue   = 1.1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(3200, easing = LinearEasing),
        ),
        label = "confettiFall",
    )

    val accentColor  = tierColor(tier)
    val gradient     = tierGradient(tier)
    val showConfetti = percent >= 60

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val boxWidthPx  = constraints.maxWidth.toFloat()
            val boxHeightPx = constraints.maxHeight.toFloat()

            // Background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                accentColor.copy(alpha = 0.08f),
                                MaterialTheme.colorScheme.background,
                            ),
                        ),
                    ),
            )

            // Confetti particles
            if (showConfetti) {
                ParticleEffect(isActive = true, count = 120)

                // Inline confetti fallback with different timing
                confettiParticles.forEach { particle ->
                    val fallY = ((confettiFall + particle.speedFactor * 0.25f) % 1.2f - 0.05f)
                    Box(
                        modifier = Modifier.offset {
                            IntOffset(
                                x = (particle.x * boxWidthPx).roundToInt(),
                                y = (fallY * boxHeightPx).roundToInt(),
                            )
                        },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(particle.size.dp)
                                .clip(if (particle.size > 10f) CircleShape else RoundedCornerShape(2.dp))
                                .background(particle.color.copy(alpha = 0.8f)),
                        )
                    }
                }
            }

            // Main scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Animated emblem (Gold/Silver/Bronze based on score)
                Box(
                    modifier = Modifier
                        .scale(cardScale.value)
                        .alpha(cardAlpha.value),
                    contentAlignment = Alignment.Center,
                ) {
                    // Glow halo behind the emblem
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = glowAlpha * 0.25f))
                            .blur(20.dp),
                    )
                    // Outer ring
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                brush = Brush.sweepGradient(gradient),
                                shape = CircleShape,
                            )
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.22f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = performanceEmoji, fontSize = 64.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Performance label
                Text(
                    text       = performanceLabel,
                    style      = MaterialTheme.typography.headlineSmall.copy(
                        shadow = Shadow(
                            color       = accentColor.copy(alpha = 0.6f),
                            blurRadius  = 12f,
                        ),
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color      = accentColor,
                    modifier   = Modifier
                        .scale(cardScale.value)
                        .alpha(cardAlpha.value),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Main result card with Glassmorphism
                Card(
                    modifier  = Modifier
                        .fillMaxWidth()
                        .scale(cardScale.value)
                        .alpha(cardAlpha.value),
                    shape     = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .border(
                                width = 1.5.dp,
                                color = GlassBorder,
                                shape = RoundedCornerShape(28.dp),
                            )
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        GlassWhite,
                                        accentColor.copy(alpha = 0.06f),
                                    ),
                                ),
                            )
                            .padding(28.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier            = Modifier.fillMaxWidth(),
                        ) {
                            // Score count-up with glow effect
                            Box(contentAlignment = Alignment.Center) {
                                // Glow behind score
                                Text(
                                    text     = "$displayedScore",
                                    style    = MaterialTheme.typography.displayLarge.copy(
                                        fontSize   = 72.sp,
                                        fontWeight = FontWeight.Black,
                                    ),
                                    color    = accentColor.copy(alpha = glowAlpha * 0.4f),
                                    modifier = Modifier.blur(12.dp),
                                )
                                Text(
                                    text  = "$displayedScore",
                                    style = TextStyle(
                                        fontSize   = 72.sp,
                                        fontWeight = FontWeight.Black,
                                        brush      = Brush.verticalGradient(gradient),
                                    ),
                                )
                            }

                            Text(
                                text  = "Punkte",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(1.dp)
                                    .background(GlassBorder),
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Stats row: Richtig / Falsch / Treffer
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                ResultStatItem(
                                    value = "$correct",
                                    label = "Richtig",
                                    color = Color(0xFF43E97B),
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(48.dp)
                                        .background(GlassBorder),
                                )
                                ResultStatItem(
                                    value = "${total - correct}",
                                    label = "Falsch",
                                    color = Secondary,
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(48.dp)
                                        .background(GlassBorder),
                                )
                                ResultStatItem(
                                    value = "$percent%",
                                    label = "Treffer",
                                    color = accentColor,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stat detail cards (Glassmorphism)
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .alpha(statsAlpha.value),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GlassStatCard(
                        modifier = Modifier.weight(1f),
                        emoji    = "📊",
                        value    = "$total",
                        label    = "Fragen",
                        color    = Primary,
                    )
                    GlassStatCard(
                        modifier = Modifier.weight(1f),
                        emoji    = "🎯",
                        value    = "$percent%",
                        label    = "Trefferquote",
                        color    = accentColor,
                    )
                    GlassStatCard(
                        modifier = Modifier.weight(1f),
                        emoji    = if (tier == 2) "🥇" else if (tier == 1) "🥈" else "🥉",
                        value    = if (tier >= 0) listOf("Bronze", "Silber", "Gold")[tier] else "—",
                        label    = "Rang",
                        color    = accentColor,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action buttons
                Column(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .alpha(buttonsAlpha.value),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                ) {
                    // 3D-style Play Again button
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Shadow layer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .offset(y = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentColor.copy(alpha = 0.35f))
                                .blur(4.dp),
                        )
                        Button(
                            onClick = {
                                navController.navigate(Screen.Difficulty.createRoute(categoryId)) {
                                    popUpTo(Screen.Category.route)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape  = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                            ),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.horizontalGradient(gradient)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.Replay,
                                        contentDescription = null,
                                        tint               = Color.White,
                                    )
                                    Text(
                                        text       = "Nochmal spielen",
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.White,
                                    )
                                }
                            }
                        }
                    }

                    // Home button with glass style
                    OutlinedButton(
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape  = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.5.dp,
                            color = GlassBorder,
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = GlassWhite,
                        ),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Home,
                            contentDescription = null,
                            modifier           = Modifier.padding(end = 8.dp),
                            tint               = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text       = "Startseite",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStatItem(
    value: String,
    label: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color      = color,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun GlassStatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    value: String,
    label: String,
    color: Color,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(16.dp),
            )
            .background(GlassWhite)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = color,
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultScreenPreview() {
    ResultScreen(
        score        = 850,
        total        = 10,
        correct      = 8,
        categoryId   = 1,
        navController = rememberNavController(),
    )
}

package com.quizverse.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.quizverse.app.QuizVerseApp
import com.quizverse.app.ui.navigation.Screen
import com.quizverse.app.ui.theme.Accent
import com.quizverse.app.ui.theme.Primary
import com.quizverse.app.ui.theme.Secondary
import com.quizverse.app.util.DailyQuotes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlin.math.sin

// Data model for a home screen navigation card
private data class HomeCard(
    val emoji: String,
    val title: String,
    val description: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val route: String
)

// Navigation cards displayed on the home screen
private val homeCards = listOf(
    HomeCard(
        emoji = "🎯",
        title = "Quiz starten",
        description = "Wähle eine Kategorie und teste dein Wissen",
        gradientStart = Primary,
        gradientEnd = Color(0xFFA855F7),
        route = Screen.Category.route
    ),
    HomeCard(
        emoji = "⚡",
        title = "Tägliche Herausforderung",
        description = "Neues Quiz jeden Tag – bleib am Ball!",
        gradientStart = Secondary,
        gradientEnd = Color(0xFFF43F5E),
        route = Screen.DailyChallenge.route
    ),
    HomeCard(
        emoji = "🏆",
        title = "Bestenliste",
        description = "Sieh wie du gegen andere abschneidest",
        gradientStart = Color(0xFFF59E0B),
        gradientEnd = Color(0xFFEF4444),
        route = Screen.Leaderboard.route
    ),
    HomeCard(
        emoji = "👤",
        title = "Profil",
        description = "Deine Statistiken und Erfolge im Überblick",
        gradientStart = Accent,
        gradientEnd = Color(0xFF06B6D4),
        route = Screen.Profile.route
    ),
    HomeCard(
        emoji = "⚙️",
        title = "Einstellungen",
        description = "App nach deinen Wünschen anpassen",
        gradientStart = Color(0xFF6366F1),
        gradientEnd = Color(0xFF8B5CF6),
        route = Screen.Settings.route
    )
)

// Daily quotes are in DailyQuotes.kt (365 quotes, one per day of year)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val app = LocalContext.current.applicationContext as QuizVerseApp

    // Entrance animation for the logo/title
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val quoteAlpha = remember { Animatable(0f) }

    // Pulsing glow + brain scale animation
    val infiniteTransition = rememberInfiniteTransition(label = "brain_pulse")
    val brainGlow = infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brain_glow_scale"
    )
    val glowAlpha = infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    // Brain emoji itself pulses slightly
    val brainScale = infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brain_scale"
    )
    // Subtle vertical float for the brain
    val brainFloat = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brain_float"
    )

    // Floating shapes animation
    // Vertical float offsets — stronger movement
    val floatOffset1 = infiniteTransition.animateFloat(
        initialValue = -25f, targetValue = 25f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float1"
    )
    val floatOffset2 = infiniteTransition.animateFloat(
        initialValue = 20f, targetValue = -20f,
        animationSpec = infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float2"
    )
    val floatOffset3 = infiniteTransition.animateFloat(
        initialValue = -18f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float3"
    )
    // Horizontal drift offsets
    val driftX1 = infiniteTransition.animateFloat(
        initialValue = -15f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(5500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "driftX1"
    )
    val driftX2 = infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "driftX2"
    )
    val driftX3 = infiniteTransition.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "driftX3"
    )
    val starRotation = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "star_rotation"
    )

    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, animationSpec = tween(500))
        delay(200)
        subtitleAlpha.animateTo(1f, animationSpec = tween(600))
        delay(400)
        quoteAlpha.animateTo(1f, animationSpec = tween(800))
    }

    // Random quote — changes on tap, never repeats the same one twice in a row
    var currentQuote by remember { mutableStateOf(DailyQuotes.randomQuote()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background gradient — richer than before
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.10f),
                            Color(0xFFA855F7).copy(alpha = 0.05f),
                            Color.Transparent,
                            Secondary.copy(alpha = 0.03f)
                        )
                    )
                )
        )

        // Floating decorative shapes
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val w = size.width
            val h = size.height
            val d1x = driftX1.value.dp.toPx()
            val d2x = driftX2.value.dp.toPx()
            val d3x = driftX3.value.dp.toPx()
            val f1y = floatOffset1.value.dp.toPx()
            val f2y = floatOffset2.value.dp.toPx()
            val f3y = floatOffset3.value.dp.toPx()

            // Circle 1 — top right, large
            drawCircle(
                color = Primary.copy(alpha = 0.08f),
                radius = 90.dp.toPx(),
                center = Offset(w * 0.85f + d1x, 80.dp.toPx() + f1y)
            )

            // Circle 2 — left side, medium
            drawCircle(
                color = Secondary.copy(alpha = 0.07f),
                radius = 55.dp.toPx(),
                center = Offset(w * 0.08f + d2x, h * 0.35f + f2y)
            )

            // Circle 3 — bottom right
            drawCircle(
                color = Accent.copy(alpha = 0.09f),
                radius = 40.dp.toPx(),
                center = Offset(w * 0.9f + d3x, h * 0.65f + f3y)
            )

            // Circle 4 — center-left
            drawCircle(
                color = Color(0xFFF59E0B).copy(alpha = 0.07f),
                radius = 25.dp.toPx(),
                center = Offset(w * 0.15f + d1x, h * 0.7f + f1y)
            )

            // Circle 5 — top center
            drawCircle(
                color = Color(0xFFA855F7).copy(alpha = 0.06f),
                radius = 30.dp.toPx(),
                center = Offset(w * 0.55f + d2x, h * 0.12f + f3y)
            )

            // Star 1 — top left
            val s1 = Offset(w * 0.18f + d2x, 160.dp.toPx() + f2y)
            rotate(starRotation.value, pivot = s1) {
                drawPath(createStarPath(s1, 14.dp.toPx(), 7.dp.toPx(), 4), color = Color(0xFFF59E0B).copy(alpha = 0.15f))
            }

            // Star 2 — right side
            val s2 = Offset(w * 0.88f + d3x, h * 0.45f + f3y)
            rotate(-starRotation.value * 0.7f, pivot = s2) {
                drawPath(createStarPath(s2, 12.dp.toPx(), 6.dp.toPx(), 5), color = Primary.copy(alpha = 0.12f))
            }

            // Star 3 — bottom left
            val s3 = Offset(w * 0.22f + d1x, h * 0.85f + f1y)
            rotate(starRotation.value * 0.5f, pivot = s3) {
                drawPath(createStarPath(s3, 10.dp.toPx(), 5.dp.toPx(), 6), color = Accent.copy(alpha = 0.12f))
            }

            // Star 4 — top center-right
            val s4 = Offset(w * 0.72f + d2x, h * 0.18f + f2y)
            rotate(starRotation.value * 0.8f, pivot = s4) {
                drawPath(createStarPath(s4, 9.dp.toPx(), 4.dp.toPx(), 4), color = Secondary.copy(alpha = 0.13f))
            }

            // Star 5 — center left
            val s5 = Offset(w * 0.05f + d3x, h * 0.52f + f3y)
            rotate(-starRotation.value * 0.6f, pivot = s5) {
                drawPath(createStarPath(s5, 8.dp.toPx(), 4.dp.toPx(), 5), color = Color(0xFFF59E0B).copy(alpha = 0.10f))
            }

            // Star 6 — bottom right
            val s6 = Offset(w * 0.82f + d1x, h * 0.78f + f1y)
            rotate(starRotation.value * 0.4f, pivot = s6) {
                drawPath(createStarPath(s6, 7.dp.toPx(), 3.dp.toPx(), 4), color = Color(0xFFA855F7).copy(alpha = 0.11f))
            }

            // Star 7 — mid-center
            val s7 = Offset(w * 0.45f + d2x, h * 0.42f + f2y)
            rotate(-starRotation.value * 0.3f, pivot = s7) {
                drawPath(createStarPath(s7, 6.dp.toPx(), 3.dp.toPx(), 6), color = Primary.copy(alpha = 0.08f))
            }

            // Star 8 — top far-left
            val s8 = Offset(w * 0.08f + d3x, h * 0.08f + f3y)
            rotate(starRotation.value * 0.9f, pivot = s8) {
                drawPath(createStarPath(s8, 11.dp.toPx(), 5.dp.toPx(), 5), color = Color(0xFFA855F7).copy(alpha = 0.14f))
            }

            // Star 9 — right mid-upper
            val s9 = Offset(w * 0.95f + d1x, h * 0.28f + f1y)
            rotate(-starRotation.value * 0.55f, pivot = s9) {
                drawPath(createStarPath(s9, 8.dp.toPx(), 4.dp.toPx(), 4), color = Accent.copy(alpha = 0.13f))
            }

            // Star 10 — bottom center
            val s10 = Offset(w * 0.55f + d2x, h * 0.92f + f2y)
            rotate(starRotation.value * 0.65f, pivot = s10) {
                drawPath(createStarPath(s10, 13.dp.toPx(), 6.dp.toPx(), 5), color = Color(0xFFF59E0B).copy(alpha = 0.12f))
            }

            // Star 11 — left mid-lower
            val s11 = Offset(w * 0.12f + d3x, h * 0.58f + f3y)
            rotate(-starRotation.value * 0.45f, pivot = s11) {
                drawPath(createStarPath(s11, 7.dp.toPx(), 3.dp.toPx(), 6), color = Secondary.copy(alpha = 0.11f))
            }

            // Star 12 — center upper
            val s12 = Offset(w * 0.38f + d1x, h * 0.25f + f1y)
            rotate(starRotation.value * 0.35f, pivot = s12) {
                drawPath(createStarPath(s12, 10.dp.toPx(), 5.dp.toPx(), 4), color = Primary.copy(alpha = 0.10f))
            }

            // Floating dots
            drawCircle(color = Primary.copy(alpha = 0.12f), radius = 4.dp.toPx(),
                center = Offset(w * 0.4f + d3x, 120.dp.toPx() + f3y))
            drawCircle(color = Secondary.copy(alpha = 0.12f), radius = 5.dp.toPx(),
                center = Offset(w * 0.7f + d1x, h * 0.3f + f1y))
            drawCircle(color = Color(0xFFA855F7).copy(alpha = 0.10f), radius = 3.5f.dp.toPx(),
                center = Offset(w * 0.5f + d2x, h * 0.55f + f2y))
            drawCircle(color = Accent.copy(alpha = 0.12f), radius = 3.dp.toPx(),
                center = Offset(w * 0.75f + d3x, h * 0.8f + f3y))
            drawCircle(color = Color(0xFFF59E0B).copy(alpha = 0.10f), radius = 3.dp.toPx(),
                center = Offset(w * 0.3f + d1x, h * 0.22f + f1y))
            drawCircle(color = Secondary.copy(alpha = 0.08f), radius = 2.5f.dp.toPx(),
                center = Offset(w * 0.92f + d2x, h * 0.55f + f2y))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // Animated brain emoji with pulsing glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.offset(y = brainFloat.value.dp)
            ) {
                // Outer glow ring — large, soft
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(brainGlow.value)
                        .alpha(glowAlpha.value * 0.5f)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Primary.copy(alpha = 0.3f),
                                    Color(0xFFA855F7).copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(50)
                        )
                )
                // Inner glow — brighter, tighter
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(brainGlow.value * 0.85f)
                        .alpha(glowAlpha.value)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Primary.copy(alpha = 0.5f),
                                    Color(0xFFA855F7).copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(50)
                        )
                )
                // Brain emoji — pulses and floats
                Text(
                    text = "🧠",
                    fontSize = 96.sp,
                    modifier = Modifier
                        .scale(logoScale.value * brainScale.value)
                        .alpha(logoAlpha.value)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // App title — animated wave with color gradient per letter
            Row(
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                horizontalArrangement = Arrangement.Center
            ) {
                val titleColors = listOf(
                    Primary,
                    Color(0xFF6366F1),
                    Secondary,
                    Color(0xFFA855F7),
                    Accent,
                    Color(0xFFF59E0B),
                    Primary,
                    Color(0xFF6366F1),
                    Secondary
                )
                "QuizVerse".forEachIndexed { i, char ->
                    val wavePhase = infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 2f * Math.PI.toFloat(),
                        animationSpec = infiniteRepeatable(
                            tween(2000 + i * 100, easing = LinearEasing),
                            RepeatMode.Restart
                        ),
                        label = "title_wave_$i"
                    )
                    val yOffset = sin(wavePhase.value.toDouble()).toFloat() * 6f
                    Text(
                        text = char.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = titleColors[i % titleColors.size],
                        modifier = Modifier.graphicsLayer {
                            translationY = yOffset.dp.toPx()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle with fade-in
            Text(
                text = "Wissen ist Abenteuer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Navigation cards with staggered entrance animations
            homeCards.forEachIndexed { index, card ->
                AnimatedHomeCard(
                    card = card,
                    delayMillis = 300 + index * 80,
                    onClick = {
                        if (card.route == Screen.Category.route) {
                            app.soundManager.playCheerStart()
                        }
                        navController.navigate(card.route)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Random motivational quote — tap for animated transition to a new one
            AnimatedContent(
                targetState = currentQuote,
                transitionSpec = {
                    (scaleIn(
                        initialScale = 0.85f,
                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(400)
                    )).togetherWith(
                        scaleOut(
                            targetScale = 1.1f,
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(300)
                        )
                    )
                },
                label = "quote_transition",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .alpha(quoteAlpha.value)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { currentQuote = DailyQuotes.randomQuote(exclude = currentQuote) }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) { quote ->
                Text(
                    text = "\u201E$quote\u201C",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Creates a star-shaped path with the given number of points
private fun createStarPath(center: Offset, outerRadius: Float, innerRadius: Float, points: Int): Path {
    val path = Path()
    val angleStep = Math.PI / points
    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = i * angleStep - Math.PI / 2
        val x = center.x + (radius * Math.cos(angle)).toFloat()
        val y = center.y + (radius * Math.sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

// Single navigation card with slide-up + fade entrance animation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimatedHomeCard(
    card: HomeCard,
    delayMillis: Int,
    onClick: () -> Unit
) {
    val offsetY = remember { Animatable(40f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        alpha.animateTo(1f, animationSpec = tween(400))
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha.value),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(card.gradientStart, card.gradientEnd)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category emoji in a semi-transparent circle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.emoji,
                        fontSize = 26.sp
                    )
                }

                // Title and description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = card.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // Arrow indicator
                Text(
                    text = "›",
                    fontSize = 24.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController())
}

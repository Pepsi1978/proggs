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
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

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
    val floatOffset1 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float1"
    )
    val floatOffset2 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -15f,
        animationSpec = infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float2"
    )
    val floatOffset3 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float3"
    )
    val starRotation = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
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

            // Circle 1 — top right, large, very subtle
            drawCircle(
                color = Primary.copy(alpha = 0.07f),
                radius = 90.dp.toPx(),
                center = Offset(w * 0.85f, 80.dp.toPx() + floatOffset1.value.dp.toPx())
            )

            // Circle 2 — left side, medium
            drawCircle(
                color = Secondary.copy(alpha = 0.06f),
                radius = 50.dp.toPx(),
                center = Offset(w * 0.1f, h * 0.35f + floatOffset2.value.dp.toPx())
            )

            // Circle 3 — bottom right, small
            drawCircle(
                color = Accent.copy(alpha = 0.08f),
                radius = 35.dp.toPx(),
                center = Offset(w * 0.9f, h * 0.65f + floatOffset3.value.dp.toPx())
            )

            // Circle 4 — center-left, tiny accent
            drawCircle(
                color = Color(0xFFF59E0B).copy(alpha = 0.06f),
                radius = 20.dp.toPx(),
                center = Offset(w * 0.15f, h * 0.7f + floatOffset1.value.dp.toPx())
            )

            // Star 1 — top left, rotating
            rotate(starRotation.value, pivot = Offset(w * 0.18f, 160.dp.toPx() + floatOffset2.value.dp.toPx())) {
                val starCenter = Offset(w * 0.18f, 160.dp.toPx() + floatOffset2.value.dp.toPx())
                val starPath = createStarPath(starCenter, 12.dp.toPx(), 6.dp.toPx(), 4)
                drawPath(starPath, color = Color(0xFFF59E0B).copy(alpha = 0.12f))
            }

            // Star 2 — right side, rotating opposite
            rotate(-starRotation.value * 0.7f, pivot = Offset(w * 0.88f, h * 0.45f + floatOffset3.value.dp.toPx())) {
                val starCenter2 = Offset(w * 0.88f, h * 0.45f + floatOffset3.value.dp.toPx())
                val starPath2 = createStarPath(starCenter2, 10.dp.toPx(), 5.dp.toPx(), 5)
                drawPath(starPath2, color = Primary.copy(alpha = 0.10f))
            }

            // Star 3 — bottom left
            rotate(starRotation.value * 0.5f, pivot = Offset(w * 0.25f, h * 0.85f + floatOffset1.value.dp.toPx())) {
                val starCenter3 = Offset(w * 0.25f, h * 0.85f + floatOffset1.value.dp.toPx())
                val starPath3 = createStarPath(starCenter3, 8.dp.toPx(), 4.dp.toPx(), 6)
                drawPath(starPath3, color = Accent.copy(alpha = 0.10f))
            }

            // Tiny dots scattered
            drawCircle(color = Primary.copy(alpha = 0.10f), radius = 3.dp.toPx(),
                center = Offset(w * 0.4f, 120.dp.toPx() + floatOffset3.value.dp.toPx()))
            drawCircle(color = Secondary.copy(alpha = 0.10f), radius = 4.dp.toPx(),
                center = Offset(w * 0.7f, h * 0.3f + floatOffset1.value.dp.toPx()))
            drawCircle(color = Color(0xFFA855F7).copy(alpha = 0.08f), radius = 3.dp.toPx(),
                center = Offset(w * 0.5f, h * 0.55f + floatOffset2.value.dp.toPx()))
            drawCircle(color = Accent.copy(alpha = 0.10f), radius = 2.5f.dp.toPx(),
                center = Offset(w * 0.75f, h * 0.8f + floatOffset3.value.dp.toPx()))
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
                    fontSize = 64.sp,
                    modifier = Modifier
                        .scale(logoScale.value * brainScale.value)
                        .alpha(logoAlpha.value)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // App title with entrance animation
            Text(
                text = "QuizVerse",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Primary,
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

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

            // Random motivational quote — tap for a new one
            Text(
                text = "\u201E$currentQuote\u201C",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .alpha(quoteAlpha.value)
                    .clickable { currentQuote = DailyQuotes.randomQuote(exclude = currentQuote) }
            )

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

package com.quizverse.app.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.quizverse.app.QuizVerseApp
import com.quizverse.app.ui.navigation.Screen
import com.quizverse.app.ui.theme.Accent
import com.quizverse.app.ui.theme.Primary
import com.quizverse.app.ui.theme.Secondary
import com.quizverse.app.util.DailyQuotes
import kotlinx.coroutines.delay
import kotlin.math.sin

// Data model for a home screen navigation card — now with SVG asset path
private data class HomeCard(
    val iconAsset: String,
    val title: String,
    val description: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val route: String
)

// Navigation cards with Fluent Emoji 3D SVG icons
private val homeCards = listOf(
    HomeCard(
        iconAsset = "icons/quiz_bullseye.svg",
        title = "Quiz starten",
        description = "Wähle eine Kategorie und teste dein Wissen",
        gradientStart = Primary,
        gradientEnd = Color(0xFFA855F7),
        route = Screen.Category.route
    ),
    HomeCard(
        iconAsset = "icons/challenge_lightning.svg",
        title = "Tägliche Herausforderung",
        description = "Neues Quiz jeden Tag – bleib am Ball!",
        gradientStart = Secondary,
        gradientEnd = Color(0xFFF43F5E),
        route = Screen.DailyChallenge.route
    ),
    HomeCard(
        iconAsset = "icons/leaderboard_medal.svg",
        title = "Bestenliste",
        description = "Sieh wie du gegen andere abschneidest",
        gradientStart = Color(0xFFF59E0B),
        gradientEnd = Color(0xFFEF4444),
        route = Screen.Leaderboard.route
    ),
    HomeCard(
        iconAsset = "icons/profile_nerd.svg",
        title = "Profil",
        description = "Deine Statistiken und Erfolge im Überblick",
        gradientStart = Accent,
        gradientEnd = Color(0xFF06B6D4),
        route = Screen.Profile.route
    ),
    HomeCard(
        iconAsset = "icons/settings_gear.svg",
        title = "Einstellungen",
        description = "App nach deinen Wünschen anpassen",
        gradientStart = Color(0xFF6366F1),
        gradientEnd = Color(0xFF8B5CF6),
        route = Screen.Settings.route
    )
)

// Shared SVG-capable ImageLoader (created once per context)
@Composable
private fun rememberSvgImageLoader(context: Context): ImageLoader {
    return remember(context) {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    val app = LocalContext.current.applicationContext as QuizVerseApp
    val context = LocalContext.current
    val svgLoader = rememberSvgImageLoader(context)

    // Entrance animation for the logo/title
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val quoteAlpha = remember { Animatable(0f) }

    // Pulsing glow + brain scale animation
    val infiniteTransition = rememberInfiniteTransition(label = "brain_pulse")
    val brainGlow = infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "brain_glow_scale"
    )
    val glowAlpha = infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_alpha"
    )
    val brainScale = infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "brain_scale"
    )
    val brainFloat = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "brain_float"
    )

    // Floating shapes
    val floatOffset1 = infiniteTransition.animateFloat(
        initialValue = -25f, targetValue = 25f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float1"
    )
    val floatOffset2 = infiniteTransition.animateFloat(
        initialValue = 20f, targetValue = -20f,
        animationSpec = infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float2"
    )
    val floatOffset3 = infiniteTransition.animateFloat(
        initialValue = -18f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float3"
    )
    val driftX1 = infiniteTransition.animateFloat(
        initialValue = -15f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(5500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "driftX1"
    )
    val driftX2 = infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "driftX2"
    )
    val driftX3 = infiniteTransition.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "driftX3"
    )
    val starRotation = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart), label = "star_rotation"
    )

    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, animationSpec = tween(500))
        delay(200)
        subtitleAlpha.animateTo(1f, animationSpec = tween(600))
        delay(400)
        quoteAlpha.animateTo(1f, animationSpec = tween(800))
    }

    var currentQuote by remember { mutableStateOf(DailyQuotes.randomQuote()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Primary.copy(alpha = 0.10f), Color(0xFFA855F7).copy(alpha = 0.05f),
                            Color.Transparent, Secondary.copy(alpha = 0.03f))
                    )
                )
        )

        // Floating decorative shapes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val d1x = driftX1.value.dp.toPx(); val d2x = driftX2.value.dp.toPx(); val d3x = driftX3.value.dp.toPx()
            val f1y = floatOffset1.value.dp.toPx(); val f2y = floatOffset2.value.dp.toPx(); val f3y = floatOffset3.value.dp.toPx()

            drawCircle(Primary.copy(alpha = 0.08f), 90.dp.toPx(), Offset(w * 0.85f + d1x, 80.dp.toPx() + f1y))
            drawCircle(Secondary.copy(alpha = 0.07f), 55.dp.toPx(), Offset(w * 0.08f + d2x, h * 0.35f + f2y))
            drawCircle(Accent.copy(alpha = 0.09f), 40.dp.toPx(), Offset(w * 0.9f + d3x, h * 0.65f + f3y))
            drawCircle(Color(0xFFF59E0B).copy(alpha = 0.07f), 25.dp.toPx(), Offset(w * 0.15f + d1x, h * 0.7f + f1y))

            val s1 = Offset(w * 0.18f + d2x, 160.dp.toPx() + f2y)
            rotate(starRotation.value, pivot = s1) {
                drawPath(createStarPath(s1, 14.dp.toPx(), 7.dp.toPx(), 4), color = Color(0xFFF59E0B).copy(alpha = 0.15f))
            }
            val s2 = Offset(w * 0.88f + d3x, h * 0.45f + f3y)
            rotate(-starRotation.value * 0.7f, pivot = s2) {
                drawPath(createStarPath(s2, 12.dp.toPx(), 6.dp.toPx(), 5), color = Primary.copy(alpha = 0.12f))
            }

            drawCircle(Primary.copy(alpha = 0.12f), 4.dp.toPx(), Offset(w * 0.4f + d3x, 120.dp.toPx() + f3y))
            drawCircle(Secondary.copy(alpha = 0.12f), 5.dp.toPx(), Offset(w * 0.7f + d1x, h * 0.3f + f1y))
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
            Box(contentAlignment = Alignment.Center, modifier = Modifier.offset(y = brainFloat.value.dp)) {
                Box(modifier = Modifier.size(140.dp).scale(brainGlow.value).alpha(glowAlpha.value * 0.5f)
                    .background(Brush.radialGradient(listOf(Primary.copy(alpha = 0.3f), Color(0xFFA855F7).copy(alpha = 0.15f), Color.Transparent)), RoundedCornerShape(50)))
                Box(modifier = Modifier.size(100.dp).scale(brainGlow.value * 0.85f).alpha(glowAlpha.value)
                    .background(Brush.radialGradient(listOf(Primary.copy(alpha = 0.5f), Color(0xFFA855F7).copy(alpha = 0.3f), Color.Transparent)), RoundedCornerShape(50)))
                Text("🧠", fontSize = 96.sp, modifier = Modifier.scale(logoScale.value * brainScale.value).alpha(logoAlpha.value))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 3D Title "QuizVerse" with shadow depth ──────────────────────
            Row(
                modifier = Modifier.scale(logoScale.value).alpha(logoAlpha.value),
                horizontalArrangement = Arrangement.Center
            ) {
                val titleColors = listOf(
                    Primary, Color(0xFF6366F1), Secondary, Color(0xFFA855F7),
                    Accent, Color(0xFFF59E0B), Primary, Color(0xFF6366F1), Secondary
                )
                "QuizVerse".forEachIndexed { i, char ->
                    val wavePhase = infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = 2f * Math.PI.toFloat(),
                        animationSpec = infiniteRepeatable(tween(2000 + i * 100, easing = LinearEasing), RepeatMode.Restart),
                        label = "title_wave_$i"
                    )
                    val yOffset = sin(wavePhase.value.toDouble()).toFloat() * 6f
                    val baseColor = titleColors[i % titleColors.size]
                    // 3D text: dark shadow behind + bright color on top
                    Text(
                        text = char.toString(),
                        style = TextStyle(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = baseColor,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = Offset(2f, 4f),
                                blurRadius = 6f
                            )
                        ),
                        modifier = Modifier.graphicsLayer {
                            translationY = yOffset.dp.toPx()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Wissen ist Abenteuer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Navigation cards (vertical list, 3D style, SVG icons) ────
            homeCards.forEachIndexed { index, card ->
                Animated3DCard(
                    card = card,
                    delayMillis = 300 + index * 80,
                    svgLoader = svgLoader,
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

            // Random motivational quote
            AnimatedContent(
                targetState = currentQuote,
                transitionSpec = {
                    (scaleIn(initialScale = 0.85f, animationSpec = tween(400, easing = FastOutSlowInEasing))
                        + fadeIn(animationSpec = tween(400)))
                        .togetherWith(scaleOut(targetScale = 1.1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                            + fadeOut(animationSpec = tween(300)))
                },
                label = "quote_transition",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).alpha(quoteAlpha.value)
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

// Creates a star-shaped path
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

// ── 3D Card with SVG icon, neumorphic press effect, shimmer ──────────────────
@Composable
private fun Animated3DCard(
    card: HomeCard,
    delayMillis: Int,
    svgLoader: ImageLoader,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(30f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Press animations
    val pressTranslateY by animateFloatAsState(if (isPressed) 5f else 0f, spring(stiffness = Spring.StiffnessHigh), label = "neu_ty")
    val pressScale by animateFloatAsState(if (isPressed) 0.975f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "neu_sc")
    val outerShadowAlpha by animateFloatAsState(if (isPressed) 0f else 1f, tween(100), label = "neu_os")
    val highlightAlpha by animateFloatAsState(if (isPressed) 0.08f else 0.5f, tween(100), label = "neu_hl")
    val edgeShadowAlpha by animateFloatAsState(if (isPressed) 0.5f else 0.25f, tween(100), label = "neu_es")
    val innerShadowAlpha by animateFloatAsState(if (isPressed) 0.6f else 0f, tween(100), label = "neu_is")

    // Continuous animations
    val ct = rememberInfiniteTransition(label = "card_${card.title}")
    val breathScale = ct.animateFloat(1f, 1.015f,
        infiniteRepeatable(tween(2500 + delayMillis % 500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath_${card.title}")
    val shimmer = ct.animateFloat(-0.3f, 1.3f,
        infiniteRepeatable(tween(3000 + delayMillis % 700, easing = LinearEasing), RepeatMode.Restart), label = "shimmer_${card.title}")
    val iconFloat = ct.animateFloat(0f, -4f,
        infiniteRepeatable(tween(1200 + delayMillis % 400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ifl_${card.title}")

    // Entrance animation
    LaunchedEffect(Unit) { delay(delayMillis.toLong()); offsetY.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
    LaunchedEffect(Unit) { delay(delayMillis.toLong()); alpha.animateTo(1f, tween(400)) }

    val shape = RoundedCornerShape(20.dp)

    // SVG icon painter
    val iconPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context).data("file:///android_asset/${card.iconAsset}").build(),
        imageLoader = svgLoader
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha.value)
            .graphicsLayer {
                scaleX = breathScale.value * pressScale; scaleY = breathScale.value * pressScale
                translationY = (offsetY.value + pressTranslateY).dp.toPx()
                this.shape = RoundedCornerShape(20.dp); clip = false
                shadowElevation = 12.dp.toPx() * outerShadowAlpha
                ambientShadowColor = Color.Black.copy(alpha = 0.3f * outerShadowAlpha)
                spotShadowColor = Color.Black.copy(alpha = 0.4f * outerShadowAlpha)
            }
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(card.gradientStart, card.gradientEnd)))
            .drawWithContent {
                drawContent()
                val w = size.width; val h = size.height
                val cr = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                // Top highlight
                drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = highlightAlpha), Color.Transparent), startY = 0f, endY = 6.dp.toPx()), size = Size(w, 6.dp.toPx()), cornerRadius = cr)
                // Left highlight
                drawRoundRect(Brush.horizontalGradient(listOf(Color.White.copy(alpha = highlightAlpha * 0.7f), Color.Transparent), startX = 0f, endX = 5.dp.toPx()), size = Size(5.dp.toPx(), h), cornerRadius = cr)
                // Bottom shadow
                drawRoundRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = edgeShadowAlpha)), startY = h - 6.dp.toPx(), endY = h), topLeft = Offset(0f, h - 6.dp.toPx()), size = Size(w, 6.dp.toPx()), cornerRadius = cr)
                // Right shadow
                drawRoundRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = edgeShadowAlpha * 0.6f)), startX = w - 5.dp.toPx(), endX = w), topLeft = Offset(w - 5.dp.toPx(), 0f), size = Size(5.dp.toPx(), h), cornerRadius = cr)
                // Inner press shadow
                if (innerShadowAlpha > 0.01f) {
                    drawRoundRect(Brush.linearGradient(listOf(Color.Black.copy(alpha = innerShadowAlpha * 0.5f), Color.Transparent), start = Offset.Zero, end = Offset(w * 0.3f, h * 0.3f)), size = size, cornerRadius = cr)
                }
                // Border highlight
                drawRoundRect(Color.White.copy(alpha = highlightAlpha * 0.4f), size = size, cornerRadius = cr, style = Stroke(width = 1.5.dp.toPx()))
            }
            // Shimmer
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent), startX = shimmer.value * 1000f - 300f, endX = shimmer.value * 1000f + 100f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 3D SVG icon in a frosted glass container
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(
                            Brush.linearGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Transparent), start = Offset.Zero, end = Offset(size.width * 0.5f, size.height * 0.5f)),
                            cornerRadius = CornerRadius(14.dp.toPx())
                        )
                        drawRoundRect(
                            Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.2f)), start = Offset(size.width * 0.5f, size.height * 0.5f), end = Offset(size.width, size.height)),
                            cornerRadius = CornerRadius(14.dp.toPx())
                        )
                    }
                    .graphicsLayer { translationY = iconFloat.value.dp.toPx() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = iconPainter,
                    contentDescription = card.title,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Title and description
            Column(modifier = Modifier.weight(1f)) {
                Text(card.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text(card.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            }

            // Arrow
            Text("›", fontSize = 24.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Light)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController())
}

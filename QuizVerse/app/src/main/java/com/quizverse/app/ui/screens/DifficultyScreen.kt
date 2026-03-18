package com.quizverse.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.quizverse.app.ui.components.CategoryBackground
import com.quizverse.app.ui.navigation.Screen
import com.quizverse.app.ui.theme.Gold
import com.quizverse.app.ui.theme.GoldLight
import com.quizverse.app.ui.theme.Primary

// Difficulty levels: value, German label, color indicator, emoji, description
private data class DifficultyLevel(
    val value: Int,
    val label: String,
    val color: Color,
    val emoji: String,
    val description: String
)

private val difficultyLevels = listOf(
    DifficultyLevel(1, "Leicht",  Color(0xFF22C55E), "🟢", "Ideal für Einsteiger"),
    DifficultyLevel(2, "Mittel",  Color(0xFF3B82F6), "🔵", "Für Wissbegierige"),
    DifficultyLevel(3, "Schwer",  Color(0xFFF59E0B), "🟡", "Knifflige Fragen"),
    DifficultyLevel(4, "Experte", Color(0xFFEF4444), "🔴", "Nur für Profis"),
    DifficultyLevel(5, "Meister", Color(0xFF8B5CF6), "💜", "Die ultimative Challenge")
)

// Available question counts for a quiz session
private val questionCounts = listOf(10, 20, 30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultyScreen(categoryId: Int, navController: NavHostController) {
    // Currently selected difficulty level (default: Mittel = 2)
    var selectedDifficulty by remember { mutableIntStateOf(2) }
    // Currently selected number of questions (default: 10)
    var selectedCount by remember { mutableIntStateOf(10) }

    // Entrance animation for screen content
    val contentAlpha = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(28f) }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, animationSpec = tween(420))
    }
    LaunchedEffect(Unit) {
        contentOffsetY.animateTo(0f, animationSpec = tween(420, easing = FastOutSlowInEasing))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Schwierigkeitsgrad",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category-specific animated background
            CategoryBackground(categoryId)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .alpha(contentAlpha.value)
                    .graphicsLayer { translationY = contentOffsetY.value.dp.toPx() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                // Header section
                Text(
                    text = "Wähle deinen Schwierigkeitsgrad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Je höher, desto kniffliger die Fragen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Large elegant difficulty cards with staggered entrance
                difficultyLevels.forEachIndexed { index, level ->
                    DifficultyCard(
                        level = level,
                        isSelected = selectedDifficulty == level.value,
                        entranceDelay = index * 60,
                        onClick = { selectedDifficulty = level.value }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Question count section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Gold star icon
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Gold.copy(alpha = 0.3f), GoldLight.copy(alpha = 0.1f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("★", fontSize = 14.sp, color = Gold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Anzahl der Fragen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Question count chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    questionCounts.forEach { count ->
                        QuestionCountChip(
                            count = count,
                            isSelected = selectedCount == count,
                            onClick = { selectedCount = count },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Big pulsing start button with glow
                PulsingStartButton(
                    selectedDifficulty = selectedDifficulty,
                    onClick = {
                        navController.navigate(
                            Screen.Quiz.createRoute(
                                categoryId = categoryId,
                                difficulty = selectedDifficulty,
                                questionCount = selectedCount
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

// ── Large elegant difficulty card with glassmorphism and 3D press ─────────────
@Composable
private fun DifficultyCard(
    level: DifficultyLevel,
    isSelected: Boolean,
    entranceDelay: Int,
    onClick: () -> Unit
) {
    val cardAlpha = remember { Animatable(0f) }
    val cardOffsetX = remember { Animatable(-30f) }

    // Staggered slide-in from left
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(entranceDelay.toLong())
        cardAlpha.animateTo(1f, animationSpec = tween(350, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(entranceDelay.toLong())
        cardOffsetX.animateTo(0f, animationSpec = tween(350, easing = FastOutSlowInEasing))
    }

    // Selection scale pop
    val selectionScale = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            selectionScale.animateTo(1.035f, animationSpec = tween(120))
            selectionScale.animateTo(1f, animationSpec = tween(120))
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "diff_pressScale_${level.value}"
    )
    val pressTranslateY by animateFloatAsState(
        targetValue = if (isPressed) 4f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "diff_ty_${level.value}"
    )
    val outerShadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0f else 1f,
        animationSpec = tween(100),
        label = "diff_shadow_${level.value}"
    )
    val highlightAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.04f else if (isSelected) 0.3f else 0.12f,
        animationSpec = tween(150),
        label = "diff_hl_${level.value}"
    )
    val innerShadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.5f else 0f,
        animationSpec = tween(100),
        label = "diff_is_${level.value}"
    )

    // Animated selection border alpha
    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "diff_border_${level.value}"
    )

    // Animated background fill for selected state
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.22f else 0.08f,
        animationSpec = tween(200),
        label = "diff_bg_${level.value}"
    )

    val shape = RoundedCornerShape(18.dp)
    val cr = CornerRadius(18.dp.value, 18.dp.value)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha.value)
            .graphicsLayer { translationX = cardOffsetX.value.dp.toPx() }
    ) {
        // Colored glow behind selected card
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(12.dp)
                    .alpha(0.4f * outerShadowAlpha)
                    .background(
                        Brush.horizontalGradient(
                            listOf(level.color.copy(alpha = 0.7f), level.color.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(50)
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = pressScale * selectionScale.value
                    scaleY = pressScale * selectionScale.value
                    translationY = pressTranslateY.dp.toPx()
                    this.shape = shape
                    clip = false
                    shadowElevation = if (isSelected) 16.dp.toPx() * outerShadowAlpha
                                      else 8.dp.toPx() * outerShadowAlpha
                    ambientShadowColor = level.color.copy(alpha = if (isSelected) 0.4f else 0.2f * outerShadowAlpha)
                    spotShadowColor = level.color.copy(alpha = if (isSelected) 0.5f else 0.25f * outerShadowAlpha)
                }
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            level.color.copy(alpha = bgAlpha),
                            level.color.copy(alpha = bgAlpha * 0.5f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .drawWithContent {
                    drawContent()

                    val w = size.width
                    val h = size.height

                    // Glass overlay
                    drawRoundRect(color = Color.White.copy(alpha = 0.04f), size = size, cornerRadius = cr)

                    // Top highlight edge
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = highlightAlpha), Color.Transparent),
                            startY = 0f, endY = 6.dp.toPx()
                        ),
                        size = Size(w, 6.dp.toPx()), cornerRadius = cr
                    )

                    // Inner press shadow
                    if (innerShadowAlpha > 0.01f) {
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Black.copy(alpha = innerShadowAlpha * 0.4f), Color.Transparent),
                                start = Offset.Zero, end = Offset(w * 0.3f, h * 0.3f)
                            ),
                            size = size, cornerRadius = cr
                        )
                    }

                    // Selected: colored gradient border with glow effect
                    if (borderAlpha > 0.01f) {
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    level.color.copy(alpha = borderAlpha),
                                    level.color.copy(alpha = borderAlpha * 0.6f),
                                    level.color.copy(alpha = borderAlpha * 0.8f)
                                )
                            ),
                            size = size, cornerRadius = cr,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    } else {
                        // Unselected: subtle glass border
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.12f),
                            size = size, cornerRadius = cr,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Large colored indicator circle with inner glow
                Box(contentAlignment = Alignment.Center) {
                    // Outer glow ring (visible when selected)
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(level.color.copy(alpha = 0.3f), Color.Transparent)
                                    )
                                )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.4f),
                                        level.color
                                    )
                                )
                            )
                            .drawWithContent {
                                drawContent()
                                // Inner highlight on circle
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.4f),
                                    radius = size.width * 0.2f,
                                    center = Offset(size.width * 0.35f, size.height * 0.3f)
                                )
                            }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = level.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isSelected) level.color
                                else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = level.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) level.color.copy(alpha = 0.75f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Check mark or emoji indicator
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(level.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            fontSize = 16.sp,
                            color = level.color,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    Text(
                        text = level.emoji,
                        fontSize = 22.sp,
                        modifier = Modifier.alpha(0.5f)
                    )
                }
            }
        }
    }
}

// ── Glassmorphism question count chip (equal width) ────────────────────────────
@Composable
private fun QuestionCountChip(
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(1f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isSelected) {
        if (isSelected) {
            scaleAnim.animateTo(1.08f, animationSpec = tween(110))
            scaleAnim.animateTo(1f, animationSpec = tween(110))
        }
    }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chip_press_$count"
    )

    val shape = RoundedCornerShape(14.dp)
    val cr = CornerRadius(14.dp.value, 14.dp.value)

    Box(
        modifier = modifier
            .scale(scaleAnim.value * pressScale)
            .clip(shape)
            .background(
                if (isSelected) Brush.linearGradient(
                    colors = listOf(Primary, Color(0xFFA855F7))
                ) else Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            )
            .drawWithContent {
                drawContent()
                val w = size.width
                val h = size.height

                // Top highlight
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = if (isSelected) 0.35f else 0.15f), Color.Transparent),
                        startY = 0f, endY = 5.dp.toPx()
                    ),
                    size = Size(w, 5.dp.toPx()), cornerRadius = cr
                )
                // Border
                drawRoundRect(
                    color = if (isSelected) Color.White.copy(alpha = 0.3f)
                            else Color.White.copy(alpha = 0.1f),
                    size = size, cornerRadius = cr,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (count == 1) "Frage" else "Fragen",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Large pulsing start button with gradient, glow, and shimmer ───────────────
@Composable
private fun PulsingStartButton(
    selectedDifficulty: Int,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "start_pulse")

    // Outer glow pulse
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    // Button scale pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )
    // Shimmer
    val shimmer by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "btn_press"
    )
    val pressShadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0f else 1f,
        animationSpec = tween(100),
        label = "btn_shadow"
    )

    val shape = RoundedCornerShape(20.dp)
    val cr = CornerRadius(20.dp.value, 20.dp.value)

    Box(contentAlignment = Alignment.Center) {
        // Outer glow halo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .scale(glowScale)
                .alpha(glowAlpha * pressShadowAlpha * 0.5f)
                .background(
                    Brush.horizontalGradient(
                        listOf(Primary.copy(alpha = 0.5f), Color(0xFFA855F7).copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(pulseScale * pressScale)
                .graphicsLayer {
                    this.shape = shape
                    clip = false
                    shadowElevation = 18.dp.toPx() * pressShadowAlpha
                    ambientShadowColor = Primary.copy(alpha = 0.4f * pressShadowAlpha)
                    spotShadowColor = Color(0xFFA855F7).copy(alpha = 0.5f * pressShadowAlpha)
                }
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Primary, Color(0xFF8B5CF6), Color(0xFFA855F7))
                    )
                )
                .drawWithContent {
                    drawContent()

                    val w = size.width
                    val h = size.height

                    // Shimmer sweep
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.2f), Color.Transparent),
                            startX = shimmer * w * 1.5f - w * 0.3f,
                            endX = shimmer * w * 1.5f + w * 0.2f
                        ),
                        size = size, cornerRadius = cr
                    )

                    // Top highlight
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
                            startY = 0f, endY = 8.dp.toPx()
                        ),
                        size = Size(w, 8.dp.toPx()), cornerRadius = cr
                    )
                    // Bottom shadow edge
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)),
                            startY = h - 8.dp.toPx(), endY = h
                        ),
                        topLeft = Offset(0f, h - 8.dp.toPx()),
                        size = Size(w, 8.dp.toPx()), cornerRadius = cr
                    )
                    // Glass border
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.35f),
                        size = size, cornerRadius = cr,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
                .padding(vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "🚀", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Quiz starten",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                // Difficulty level pill on the button
                val currentLevel = difficultyLevels.find { it.value == selectedDifficulty }
                if (currentLevel != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = currentLevel.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DifficultyScreenPreview() {
    DifficultyScreen(categoryId = 1, navController = rememberNavController())
}

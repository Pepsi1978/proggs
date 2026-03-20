package com.quizverse.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.quizverse.app.QuizVerseApp
import com.quizverse.app.ui.navigation.Screen
import com.quizverse.app.ui.theme.Accent
import com.quizverse.app.ui.theme.GlassBorder
import com.quizverse.app.ui.theme.GlassWhite
import com.quizverse.app.ui.theme.Gold
import com.quizverse.app.ui.theme.Primary
import com.quizverse.app.ui.theme.Secondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val GreenSuccess = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyChallengeScreen(navController: NavHostController) {
    val app = LocalContext.current.applicationContext as QuizVerseApp
    val progress by app.database.progressDao().getProgress().collectAsState(initial = null)
    val today = java.time.LocalDate.now().toString()
    val alreadyCompleted = progress?.lastDailyChallengeDate == today

    val todayFormatted = LocalDate.now().format(
        DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN)
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Animated streak number for dramatic effect
    val streakGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "streakGlow"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (alreadyCompleted) 0.97f else 1f,
        animationSpec = tween(300),
        label = "buttonScale"
    )

    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(Primary, Secondary)
    )
    val completedGradient = Brush.horizontalGradient(
        colors = listOf(GreenSuccess, Color(0xFF81C784))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tägliche Herausforderung",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Decorative background orbs using theme colors
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.TopEnd)
                    .graphicsLayer {
                        translationX = 80.dp.toPx()
                        translationY = (-40).dp.toPx()
                    }
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.07f))
            )
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.BottomStart)
                    .graphicsLayer {
                        translationX = (-60).dp.toPx()
                        translationY = 60.dp.toPx()
                    }
                    .clip(CircleShape)
                    .background(Secondary.copy(alpha = 0.06f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Dramatic hero emoji with layered glow animation
                AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(140.dp)
                            .scale(if (alreadyCompleted) 1f else pulseScale)
                    ) {
                        // Outer glow ring
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            (if (alreadyCompleted) GreenSuccess else Gold)
                                                .copy(alpha = glowAlpha * 0.15f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        // Inner glow
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            (if (alreadyCompleted) GreenSuccess else Secondary)
                                                .copy(alpha = glowAlpha * 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Text(
                            text = if (alreadyCompleted) "✅" else "🔥",
                            fontSize = 72.sp
                        )
                    }
                }

                // Title card with glassmorphism
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(500, delayMillis = 150)
                    ) + fadeIn(animationSpec = tween(500, delayMillis = 150))
                ) {
                    GlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Tages-Quiz",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Primary.copy(alpha = 0.25f),
                                                Secondary.copy(alpha = 0.25f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        GlassBorder,
                                        RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = todayFormatted,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Streak card — prominent with fire animation
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(500, delayMillis = 250)
                    ) + fadeIn(animationSpec = tween(500, delayMillis = 250))
                ) {
                    GlassCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Animated fire emoji scale
                                Text(
                                    text = "🔥",
                                    fontSize = 36.sp,
                                    modifier = Modifier.scale(pulseScale)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${progress?.dailyChallengeStreak ?: 0}",
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Gold.copy(alpha = streakGlow)
                                )
                                Text(
                                    text = "Tage-Serie",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(width = 1.dp, height = 70.dp)
                                    .background(GlassBorder)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "📅", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (alreadyCompleted) "Heute ✓" else "Heute",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alreadyCompleted) GreenSuccess
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (alreadyCompleted) "Abgeschlossen" else "Noch offen",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Description card with glassmorphism
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(500, delayMillis = 350)
                    ) + fadeIn(animationSpec = tween(500, delayMillis = 350))
                ) {
                    GlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Über die Herausforderung",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoRow(icon = "🎯", text = "15 zufällige Fragen aus allen Kategorien")
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow(icon = "⚡", text = "Mittlere Schwierigkeit")
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow(icon = "🏆", text = "Täglich neue Fragen — einmal pro Tag spielbar")
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow(icon = "📈", text = "Baut deine Tages-Serie auf")
                        }
                    }
                }

                // Premium play button section
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(500, delayMillis = 450)
                    ) + fadeIn(animationSpec = tween(500, delayMillis = 450))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (alreadyCompleted) {
                            // Completed status card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GreenSuccess.copy(alpha = 0.12f))
                                    .border(1.dp, GreenSuccess.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GreenSuccess,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Bereits abgeschlossen!",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenSuccess
                                    )
                                }
                            }
                        }

                        // Premium 3D play button with gradient + shadow border
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .scale(buttonScale)
                                .clip(RoundedCornerShape(32.dp))
                                .background(
                                    if (alreadyCompleted) completedGradient else buttonGradient
                                )
                                .border(
                                    width = 1.dp,
                                    brush = if (alreadyCompleted) Brush.horizontalGradient(
                                        listOf(GreenSuccess.copy(alpha = 0.6f), Color(0xFF81C784).copy(alpha = 0.4f))
                                    ) else Brush.horizontalGradient(
                                        listOf(Primary.copy(alpha = 0.8f), Secondary.copy(alpha = 0.6f))
                                    ),
                                    shape = RoundedCornerShape(32.dp)
                                )
                        ) {
                            Button(
                                onClick = {
                                    if (!alreadyCompleted) {
                                        navController.navigate(
                                            Screen.Quiz.createRoute(
                                                categoryId = 11,
                                                difficulty = 2,
                                                questionCount = 15
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent
                                ),
                                enabled = !alreadyCompleted,
                                shape = RoundedCornerShape(32.dp),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Text(
                                    text = if (alreadyCompleted) "✓  Heute gespielt" else "▶  Jetzt spielen",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        if (alreadyCompleted) {
                            Text(
                                text = "Komm morgen wieder! 👋",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ── Glassmorphism card container ───────────────────────────────────────────────

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        GlassWhite,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )
                )
            )
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
    ) {
        content()
    }
}

@Composable
private fun InfoRow(icon: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = icon, fontSize = 20.sp)
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            lineHeight = 20.sp
        )
    }
}

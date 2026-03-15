package com.quizverse.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PrimaryColor = Color(0xFF6C63FF)
private val SecondaryColor = Color(0xFFFF6584)
private val BackgroundStart = Color(0xFF1A1A2E)
private val BackgroundEnd = Color(0xFF16213E)
private val CardBackground = Color(0xFF0F3460)
private val GoldColor = Color(0xFFFFD700)
private val GreenSuccess = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyChallengeScreen(navController: NavHostController) {
    val app = LocalContext.current.applicationContext as QuizVerseApp
    val db = app.database
    val progress by db.progressDao().getProgress().collectAsState(initial = null)
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

    val buttonScale by animateFloatAsState(
        targetValue = if (alreadyCompleted) 0.97f else 1f,
        animationSpec = tween(300),
        label = "buttonScale"
    )

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(BackgroundStart, BackgroundEnd)
    )
    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(PrimaryColor, SecondaryColor)
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
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Zurück",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    Brush.horizontalGradient(
                        listOf(
                            PrimaryColor.copy(alpha = 0.9f),
                            SecondaryColor.copy(alpha = 0.7f)
                        )
                    )
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(paddingValues)
        ) {
            // Decorative background circles
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.TopEnd)
                    .graphicsLayer {
                        translationX = 80.dp.toPx()
                        translationY = (-40).dp.toPx()
                    }
                    .clip(CircleShape)
                    .background(PrimaryColor.copy(alpha = 0.08f))
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
                    .background(SecondaryColor.copy(alpha = 0.07f))
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

                // Hero emoji with pulse animation
                AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .scale(if (alreadyCompleted) 1f else pulseScale)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            (if (alreadyCompleted) GreenSuccess else SecondaryColor)
                                                .copy(alpha = glowAlpha * 0.3f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Text(
                            text = if (alreadyCompleted) "✅" else "🔥",
                            fontSize = 64.sp
                        )
                    }
                }

                // Title card
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(500, delayMillis = 150)
                    ) + fadeIn(animationSpec = tween(500, delayMillis = 150))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CardBackground.copy(alpha = 0.8f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Tages-Quiz",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                PrimaryColor.copy(alpha = 0.3f),
                                                SecondaryColor.copy(alpha = 0.3f)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = todayFormatted,
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Streak card
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(500, delayMillis = 250)
                    ) + fadeIn(animationSpec = tween(500, delayMillis = 250))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CardBackground.copy(alpha = 0.8f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "🔥", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${progress?.dailyChallengeStreak ?: 0}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldColor
                                )
                                Text(
                                    text = "Tage-Serie",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(width = 1.dp, height = 60.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "📅", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (alreadyCompleted) "Heute ✓" else "Heute",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alreadyCompleted) GreenSuccess else Color.White
                                )
                                Text(
                                    text = if (alreadyCompleted) "Abgeschlossen" else "Noch offen",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Description card
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(500, delayMillis = 350)
                    ) + fadeIn(animationSpec = tween(500, delayMillis = 350))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CardBackground.copy(alpha = 0.8f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Über die Herausforderung",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoRow(icon = "🎯", text = "10 zufällige Fragen aus allen Kategorien")
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow(icon = "⚡", text = "Mittlere Schwierigkeit")
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow(icon = "🏆", text = "Täglich neue Fragen — einmal pro Tag spielbar")
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow(icon = "📈", text = "Baut deine Tages-Serie auf")
                        }
                    }
                }

                // Play / completed section
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
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = GreenSuccess.copy(alpha = 0.15f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GreenSuccess,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.size(10.dp))
                                    Text(
                                        text = "Bereits abgeschlossen!",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenSuccess
                                    )
                                }
                            }
                        }

                        // Gradient play button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .scale(buttonScale)
                                .clip(RoundedCornerShape(30.dp))
                                .background(
                                    if (alreadyCompleted) completedGradient else buttonGradient
                                )
                        ) {
                            Button(
                                onClick = {
                                    if (!alreadyCompleted) {
                                        navController.navigate(
                                            Screen.Quiz.createRoute(
                                                categoryId = -1,
                                                difficulty = 2,
                                                questionCount = 10
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
                                shape = RoundedCornerShape(30.dp),
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
                                color = Color.White.copy(alpha = 0.7f),
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
            color = Color.White.copy(alpha = 0.85f),
            lineHeight = 20.sp
        )
    }
}

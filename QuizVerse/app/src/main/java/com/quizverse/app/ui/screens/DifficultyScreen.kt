package com.quizverse.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.quizverse.app.ui.components.CategoryBackground
import com.quizverse.app.ui.navigation.Screen

// Difficulty levels: value, German label, color indicator, emoji
private data class DifficultyLevel(
    val value: Int,
    val label: String,
    val color: Color,
    val emoji: String
)

private val difficultyLevels = listOf(
    DifficultyLevel(1, "Leicht",  Color(0xFF22C55E), "🟢"),
    DifficultyLevel(2, "Mittel",  Color(0xFF3B82F6), "🔵"),
    DifficultyLevel(3, "Schwer",  Color(0xFFF59E0B), "🟡"),
    DifficultyLevel(4, "Experte", Color(0xFFEF4444), "🔴"),
    DifficultyLevel(5, "Meister", Color(0xFF8B5CF6), "💜")
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

    // Entrance animation for the whole screen content
    val contentAlpha = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, animationSpec = tween(400))
    }
    LaunchedEffect(Unit) {
        contentOffsetY.animateTo(0f, animationSpec = tween(400, easing = FastOutSlowInEasing))
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
                    .alpha(contentAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Wähle deinen Schwierigkeitsgrad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Je höher, desto kniffliger die Fragen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Difficulty selection buttons with color indicator dots
                difficultyLevels.forEach { level ->
                    DifficultyButton(
                        level = level,
                        isSelected = selectedDifficulty == level.value,
                        onClick = { selectedDifficulty = level.value }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Anzahl der Fragen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Question count chips with animated selection
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    questionCounts.forEach { count ->
                        QuestionCountChip(
                            count = count,
                            isSelected = selectedCount == count,
                            onClick = { selectedCount = count }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Start button with gradient background and pulse animation
                PulsingStartButton(
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

                Spacer(modifier = Modifier.height(32.dp))
            }
        } // end Box
    }
}

// Difficulty selection button with colored indicator dot
@Composable
private fun DifficultyButton(
    level: DifficultyLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scaleAnim = remember { Animatable(1f) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) level.color.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = if (isSelected) 0.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isSelected) Modifier.border(
                        width = 2.dp,
                        color = level.color,
                        shape = RoundedCornerShape(14.dp)
                    ) else Modifier
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Color indicator dot
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(level.color)
                )

                Text(
                    text = level.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) level.color
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                if (isSelected) {
                    Text(
                        text = "✓",
                        fontSize = 18.sp,
                        color = level.color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Animated question count selection chip
@Composable
private fun QuestionCountChip(
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            scaleAnim.animateTo(1.1f, animationSpec = tween(100))
            scaleAnim.animateTo(1f, animationSpec = tween(100))
        }
    }

    Box(
        modifier = Modifier
            .scale(scaleAnim.value)
            .clip(RoundedCornerShape(50.dp))
            .background(
                if (isSelected) Brush.horizontalGradient(
                    colors = listOf(Color(0xFF6C63FF), Color(0xFFA855F7))
                ) else Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .then(
                if (!isSelected) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(50.dp)
                ) else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Start button with gradient background and subtle pulse animation
@Composable
private fun PulsingStartButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulseScale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF6C63FF), Color(0xFFA855F7))
                )
            )
            .clickable { onClick() }
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "🚀", fontSize = 22.sp)
            Text(
                text = "Quiz starten",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DifficultyScreenPreview() {
    DifficultyScreen(categoryId = 1, navController = rememberNavController())
}

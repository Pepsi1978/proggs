package com.quizverse.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizverse.app.ui.theme.GlassBorder
import com.quizverse.app.ui.theme.GlassWhite
import com.quizverse.app.ui.theme.Gold

/**
 * Displays a quiz question inside an animated card with a glassmorphism effect.
 *
 * The card uses a semi-transparent frosted-glass look layered over category gradients.
 * The category badge is styled elegantly, and difficulty uses filled/empty stars.
 *
 * @param questionText    The question to display.
 * @param questionNumber  Current question index (1-based).
 * @param totalQuestions  Total number of questions in the session.
 * @param categoryEmoji   Emoji icon of the active category.
 * @param categoryName    Display name of the active category.
 * @param difficulty      Difficulty level 1-5 used to render star indicators.
 * @param gradientStart   Start colour of the card gradient.
 * @param gradientEnd     End colour of the card gradient.
 * @param visible         Controls the entrance AnimatedVisibility.
 * @param modifier        Optional layout modifier.
 */
@Composable
fun QuestionCard(
    questionText: String,
    questionNumber: Int,
    totalQuestions: Int,
    categoryEmoji: String,
    categoryName: String,
    difficulty: Int,
    gradientStart: Color,
    gradientEnd: Color,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    initialOffsetY = { -it / 3 },
                ),
        modifier = modifier,
    ) {
        // Outer card with gradient background
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                gradientStart.copy(alpha = 0.85f),
                                gradientEnd.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            ) {
                // Glassmorphism overlay — frosted semi-transparent layer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(GlassWhite)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.White.copy(alpha = 0.1f),
                                ),
                            ),
                            shape = RoundedCornerShape(24.dp),
                        )
                        .padding(24.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Category header row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            // Category badge (glassmorphism pill)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.25f),
                                                Color.White.copy(alpha = 0.10f),
                                            ),
                                        ),
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = Color.White.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(50),
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Text(text = categoryEmoji, fontSize = 16.sp)
                                    Text(
                                        text = categoryName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            // Question counter badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.20f))
                                    .border(
                                        width = 0.5.dp,
                                        color = GlassBorder,
                                        shape = RoundedCornerShape(50),
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = "$questionNumber / $totalQuestions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        // Subtle glass divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.4f),
                                            Color.Transparent,
                                        ),
                                    ),
                                ),
                        )

                        // Question text
                        Text(
                            text = questionText,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        )

                        // Difficulty stars row with gold filled stars
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Schwierigkeit ",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.75f),
                            )
                            repeat(5) { index ->
                                Text(
                                    text = if (index < difficulty) "★" else "☆",
                                    color = if (index < difficulty) Gold else Color.White.copy(alpha = 0.35f),
                                    fontSize = 17.sp,
                                    fontWeight = if (index < difficulty) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

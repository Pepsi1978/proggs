package com.quizverse.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Brand colors
private val PrimaryPurple = Color(0xFF6C63FF)
private val AccentPurple = Color(0xFFA855F7)
private val CardBackground = Color(0xFF1E1B2E)
private val SurfaceBackground = Color(0xFF13111E)
private val TextSecondary = Color(0xFF9E9AB8)

private data class ShopItemPreview(
    val emoji: String,
    val title: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(navController: NavHostController) {
    // --- Animatable values ---
    val bagScale = remember { Animatable(0f) }

    val headingAlpha = remember { Animatable(0f) }
    val headingOffsetY = remember { Animatable(30f) }

    val descAlpha = remember { Animatable(0f) }

    // Each card gets its own alpha + vertical offset for staggered entrance
    val card1Alpha = remember { Animatable(0f) }
    val card2Alpha = remember { Animatable(0f) }
    val card3Alpha = remember { Animatable(0f) }
    val card1OffsetY = remember { Animatable(40f) }
    val card2OffsetY = remember { Animatable(40f) }
    val card3OffsetY = remember { Animatable(40f) }

    val noteAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Bag pops in
        launch {
            bagScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
            )
        }
        delay(220)

        // 2. Heading slides up + fades in
        launch {
            headingAlpha.animateTo(1f, animationSpec = tween(380))
        }
        launch {
            headingOffsetY.animateTo(0f, animationSpec = tween(380, easing = FastOutSlowInEasing))
        }
        delay(160)

        // 3. Description fades in
        launch {
            descAlpha.animateTo(1f, animationSpec = tween(380))
        }
        delay(200)

        // 4. Cards staggered (120 ms apart)
        launch {
            card1Alpha.animateTo(1f, animationSpec = tween(340))
        }
        launch {
            card1OffsetY.animateTo(0f, animationSpec = tween(340, easing = FastOutSlowInEasing))
        }
        delay(120)

        launch {
            card2Alpha.animateTo(1f, animationSpec = tween(340))
        }
        launch {
            card2OffsetY.animateTo(0f, animationSpec = tween(340, easing = FastOutSlowInEasing))
        }
        delay(120)

        launch {
            card3Alpha.animateTo(1f, animationSpec = tween(340))
        }
        launch {
            card3OffsetY.animateTo(0f, animationSpec = tween(340, easing = FastOutSlowInEasing))
        }
        delay(200)

        // 5. Bottom note
        launch {
            noteAlpha.animateTo(1f, animationSpec = tween(400))
        }
    }

    val shopItems = listOf(
        ShopItemPreview("💡", "Extra-Hinweis", "Erhalte einen zusätzlichen Hinweis pro Frage"),
        ShopItemPreview("⏱️", "Zeitbonus", "30 Sekunden extra Zeit pro Quiz"),
        ShopItemPreview("🎯", "50:50 Joker", "Eliminiere zwei falsche Antworten")
    )

    val cardAlphas = listOf(card1Alpha, card2Alpha, card3Alpha)
    val cardOffsets = listOf(card1OffsetY, card2OffsetY, card3OffsetY)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Shop",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceBackground
                )
            )
        },
        containerColor = SurfaceBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SurfaceBackground,
                            Color(0xFF1A1530),
                            SurfaceBackground
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Large shopping bag with scale-in animation
                Text(
                    text = "🛍️",
                    fontSize = 88.sp,
                    modifier = Modifier.scale(bagScale.value)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Heading: "Demnächst verfügbar!"
                Text(
                    text = "Demnächst verfügbar!",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(headingAlpha.value)
                        .offset(y = headingOffsetY.value.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Description
                Text(
                    text = "Hier kannst du bald Joker, Hinweise und Extras freischalten",
                    fontSize = 15.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 23.sp,
                    modifier = Modifier
                        .alpha(descAlpha.value)
                        .padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Gradient divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    PrimaryPurple.copy(alpha = 0.55f),
                                    AccentPurple.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Staggered preview cards
                shopItems.forEachIndexed { index, item ->
                    ShopPreviewCard(
                        item = item,
                        modifier = Modifier
                            .alpha(cardAlphas[index].value)
                            .offset(y = cardOffsets[index].value.dp)
                    )
                    if (index < shopItems.lastIndex) {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Bottom note
                Text(
                    text = "Wir arbeiten hart daran, dir tolle Inhalte zu bringen 💜",
                    fontSize = 13.sp,
                    color = TextSecondary.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(noteAlpha.value)
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun ShopPreviewCard(
    item: ShopItemPreview,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        CardBackground,
                        Color(0xFF231F38)
                    )
                )
            )
    ) {
        // Left accent stripe with gradient
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(PrimaryPurple, AccentPurple)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Emoji + text block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Emoji container with subtle radial gradient
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    PrimaryPurple.copy(alpha = 0.28f),
                                    AccentPurple.copy(alpha = 0.10f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.emoji,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Greyed-out "Bald!" badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF2E2A45))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Bald!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopScreenPreview() {
    ShopScreen(navController = rememberNavController())
}

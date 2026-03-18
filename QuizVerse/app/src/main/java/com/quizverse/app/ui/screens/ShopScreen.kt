package com.quizverse.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.quizverse.app.ui.theme.Accent
import com.quizverse.app.ui.theme.GlassBorder
import com.quizverse.app.ui.theme.GlassWhite
import com.quizverse.app.ui.theme.Gold
import com.quizverse.app.ui.theme.GoldDark
import com.quizverse.app.ui.theme.GoldLight
import com.quizverse.app.ui.theme.Primary
import com.quizverse.app.ui.theme.Secondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ShopItemPreview(
    val emoji: String,
    val title: String,
    val description: String,
    val glowColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(navController: NavHostController) {
    val bagScale = remember { Animatable(0f) }

    val headingAlpha = remember { Animatable(0f) }
    val headingOffsetY = remember { Animatable(30f) }

    val descAlpha = remember { Animatable(0f) }

    val card1Alpha = remember { Animatable(0f) }
    val card2Alpha = remember { Animatable(0f) }
    val card3Alpha = remember { Animatable(0f) }
    val card1OffsetY = remember { Animatable(40f) }
    val card2OffsetY = remember { Animatable(40f) }
    val card3OffsetY = remember { Animatable(40f) }

    val noteAlpha = remember { Animatable(0f) }

    // Animated glow for the banner
    val infiniteTransition = rememberInfiniteTransition(label = "shopGlow")
    val bannerGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bannerGlow"
    )

    LaunchedEffect(Unit) {
        launch {
            bagScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
            )
        }
        delay(220)

        launch { headingAlpha.animateTo(1f, animationSpec = tween(380)) }
        launch { headingOffsetY.animateTo(0f, animationSpec = tween(380, easing = FastOutSlowInEasing)) }
        delay(160)

        launch { descAlpha.animateTo(1f, animationSpec = tween(380)) }
        delay(200)

        launch { card1Alpha.animateTo(1f, animationSpec = tween(340)) }
        launch { card1OffsetY.animateTo(0f, animationSpec = tween(340, easing = FastOutSlowInEasing)) }
        delay(120)

        launch { card2Alpha.animateTo(1f, animationSpec = tween(340)) }
        launch { card2OffsetY.animateTo(0f, animationSpec = tween(340, easing = FastOutSlowInEasing)) }
        delay(120)

        launch { card3Alpha.animateTo(1f, animationSpec = tween(340)) }
        launch { card3OffsetY.animateTo(0f, animationSpec = tween(340, easing = FastOutSlowInEasing)) }
        delay(200)

        launch { noteAlpha.animateTo(1f, animationSpec = tween(400)) }
    }

    val shopItems = listOf(
        ShopItemPreview("💡", "Extra-Hinweis", "Erhalte einen zusätzlichen Hinweis pro Frage", Primary),
        ShopItemPreview("⏱️", "Zeitbonus", "30 Sekunden extra Zeit pro Quiz", Accent),
        ShopItemPreview("🎯", "50:50 Joker", "Eliminiere zwei falsche Antworten", Gold)
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
                        color = MaterialTheme.colorScheme.onSurface
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
            // Subtle premium gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Gold.copy(alpha = 0.05f),
                                Primary.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Large shopping bag with scale-in + glow ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .scale(bagScale.value)
                ) {
                    // Glow ring behind bag
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Gold.copy(alpha = bannerGlow * 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Text(text = "🛍️", fontSize = 88.sp)
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Animated "Demnächst verfügbar!" banner with Gold glow
                Box(
                    modifier = Modifier
                        .alpha(headingAlpha.value)
                        .offset(y = headingOffsetY.value.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    GoldDark.copy(alpha = 0.2f * bannerGlow),
                                    Gold.copy(alpha = 0.15f * bannerGlow),
                                    GoldLight.copy(alpha = 0.2f * bannerGlow)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    GoldDark.copy(alpha = 0.5f * bannerGlow),
                                    GoldLight.copy(alpha = 0.4f * bannerGlow)
                                )
                            ),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "✨ Bald verfügbar!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Demnächst verfügbar!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(headingAlpha.value)
                        .offset(y = headingOffsetY.value.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Hier kannst du bald Joker, Hinweise und Extras freischalten",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 23.sp,
                    modifier = Modifier
                        .alpha(descAlpha.value)
                        .padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Premium gradient divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Gold.copy(alpha = 0.4f),
                                    Primary.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Staggered preview cards with per-item glow colors
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

                Text(
                    text = "Wir arbeiten hart daran, dir tolle Inhalte zu bringen 💜",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
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
                        GlassWhite,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        item.glowColor.copy(alpha = 0.35f),
                        GlassBorder
                    )
                ),
                RoundedCornerShape(16.dp)
            )
    ) {
        // Top glow streak for premium feel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            item.glowColor.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Left accent stripe with per-item gradient color
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(item.glowColor, item.glowColor.copy(alpha = 0.5f))
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 3D icon container with radial glow
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    item.glowColor.copy(alpha = 0.25f),
                                    item.glowColor.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(1.dp, item.glowColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = item.emoji, fontSize = 26.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // "Bald!" badge with item-color glow
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(item.glowColor.copy(alpha = 0.12f))
                    .border(1.dp, item.glowColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Bald!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = item.glowColor
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

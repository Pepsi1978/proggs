package com.entropyjournal.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entropyjournal.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SplashBackground = Color(0xFF16110D)
private val SplashText = Color(0xFFF2E3C8)
private val SplashMuted = Color(0xFFB3A489)
private val SplashGold = Color(0xFFE8B547)
private val SplashCopper = Color(0xFFDF741E)
private val SplashOnGold = Color(0xFF241505)
@Composable
fun SplashScreen(onSplashFinished: () -> Unit, viewModel: SplashViewModel) {
    val context = LocalContext.current
    val isExiting = remember { mutableStateOf(false) }
    val exitAlpha = remember { Animatable(1f) }
    val heroAlpha = remember { Animatable(0f) }
    val heroScale = remember { Animatable(1.08f) }
    val contentAlpha = remember { Animatable(0f) }
    val contentOffset = remember { Animatable(18f) }

    val infiniteTransition = rememberInfiniteTransition(label = "goldenThreadSplash")
    val breathScale by
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.03f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1300, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "loginBreathe",
        )
    val glowAlpha by
        infiniteTransition.animateFloat(
            initialValue = 0.18f,
            targetValue = 0.35f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1300, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "loginGlow",
        )
    val shimmerProgress by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)),
            label = "goldHairline",
        )

    LaunchedEffect(Unit) {
        launch { heroAlpha.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch { heroScale.animateTo(1f, tween(3000, easing = FastOutSlowInEasing)) }
        delay(420)
        launch { contentAlpha.animateTo(1f, tween(700)) }
        launch { contentOffset.animateTo(0f, tween(800, easing = FastOutSlowInEasing)) }
    }

    LaunchedEffect(isExiting.value) {
        if (isExiting.value) {
            exitAlpha.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
            delay(50)
            onSplashFinished()
        }
    }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(SplashBackground)
                .graphicsLayer { alpha = exitAlpha.value }
    ) {
        Image(
            painter = painterResource(R.drawable.splash_hero_book),
            contentDescription = "Aufgeschlagenes Buch im warmen Licht",
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.fillMaxWidth()
                    .fillMaxHeight(0.44f)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        alpha = heroAlpha.value
                        scaleX = heroScale.value
                        scaleY = heroScale.value
                    },
        )

        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colorStops =
                                arrayOf(
                                    0f to SplashBackground.copy(alpha = 0.15f),
                                    0.4f to Color.Transparent,
                                    0.96f to SplashBackground,
                                )
                        )
                    )
        )

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .fillMaxHeight(0.57f)
                    .align(Alignment.BottomCenter)
                    .offset(y = (-8).dp)
                    .padding(horizontal = 32.dp)
                    .graphicsLayer {
                        alpha = contentAlpha.value
                        translationY = contentOffset.value * density
                    },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Entropy Journal",
                color = SplashText,
                style =
                    MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        lineHeight = 44.sp,
                    ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(14.dp))
            Box(
                modifier =
                    Modifier.width(72.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        Color.Transparent,
                                        SplashGold,
                                        SplashCopper,
                                        SplashGold,
                                        Color.Transparent,
                                    ),
                                start = Offset((shimmerProgress * 280f) - 140f, 0f),
                                end = Offset((shimmerProgress * 280f) + 140f, 0f),
                            )
                        )
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Dein persönliches KI-Tagebuch\nfür Klarheit und Veränderung",
                color = SplashMuted,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 25.6.sp),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(38.dp))
            Box(
                modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier.matchParentSize()
                            .graphicsLayer { alpha = glowAlpha }
                            .blur(24.dp, BlurredEdgeTreatment.Unbounded)
                            .clip(RoundedCornerShape(50))
                            .background(SplashGold)
                )
                Row(
                    modifier =
                        Modifier.graphicsLayer {
                                scaleX = breathScale
                                scaleY = breathScale
                            }
                            .shadow(12.dp, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.linearGradient(listOf(SplashGold, SplashCopper))
                            )
                            .clickable(enabled = !isExiting.value && !viewModel.isSigningIn) {
                                if (viewModel.isSignedIn()) {
                                    isExiting.value = true
                                } else {
                                    viewModel.signIn(context) { isExiting.value = true }
                                }
                            }
                            .padding(horizontal = 34.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier.size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFFDF6)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "G",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (viewModel.isSigningIn) "Anmeldung läuft …" else "Mit Google anmelden",
                        color = SplashOnGold,
                        style =
                            MaterialTheme.typography.labelLarge.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                    )
                }
            }

            viewModel.signInError?.let { error ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = error,
                    color = Color(0xFFFF9B8F),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.weight(1f))
            Text(
                text = "v0.21.1 - 18.07.2026 22:21 Uhr · © Frank Barwandt",
                color = Color(0xFF8A7A5C),
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 18.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

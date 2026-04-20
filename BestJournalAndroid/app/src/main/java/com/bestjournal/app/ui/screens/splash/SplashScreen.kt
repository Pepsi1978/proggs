package com.bestjournal.app.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestjournal.app.R
import kotlin.math.cos
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════
// Splash Screen — "The Gilded Sanctum" (design by Google Stitch)
// Cinematic hero image + warm copper/gold entrance animations
// ═══════════════════════════════════════════════════════════════════

enum class SplashDestination { Consent, Onboarding, Main }

// Splash-specific colors from the Stitch "Gilded Sanctum" design system
private val SplashBg = Color(0xFF131313)
private val CopperLight = Color(0xFFFFB689)
private val CopperDeep = Color(0xFFDF741E)
private val GoldAccent = Color(0xFFECC165)
private val OnSurface = Color(0xFFE5E2E1)
private val OnPrimaryDark = Color(0xFF512400)
private val WarmSandVariant = Color(0xFFDDC1B2)

private val ParticleColors =
    listOf(Color(0xFFFFB689), Color(0xFFDF741E), Color(0xFFECC165), Color(0xFFC4875A))

private const val PARTICLE_COUNT = 200

@Composable
fun SplashScreen(
    onSplashFinished: (destination: SplashDestination) -> Unit,
    viewModel: SplashViewModel,
) {
    // ── Exit state: when Start is pressed, fade everything out first ──
    val isExiting = remember { mutableStateOf(false) }
    val exitAlpha = remember { Animatable(1f) }

    LaunchedEffect(isExiting.value) {
        if (isExiting.value) {
            exitAlpha.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
            delay(50)
            val destination =
                when {
                    !viewModel.isConsentShown() -> SplashDestination.Consent
                    !viewModel.isOnboardingCompleted() -> SplashDestination.Onboarding
                    else -> SplashDestination.Main
                }
            onSplashFinished(destination)
        }
    }

    // ── Entrance animation states ──
    val heroAlpha = remember { Animatable(0f) }
    val heroScale = remember { Animatable(1.12f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(24f) }
    val accentAlpha = remember { Animatable(0f) }
    val btnAlpha = remember { Animatable(0f) }
    val btnOffsetY = remember { Animatable(50f) }
    val subAlpha = remember { Animatable(0f) }

    // ── Continuous breathing for Start button ──
    val infinite = rememberInfiniteTransition(label = "splash")
    val glowAlpha by
        infinite.animateFloat(
            initialValue = 0.12f,
            targetValue = 0.32f,
            animationSpec =
                infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "btnGlow",
        )
    val btnBreathScale by
        infinite.animateFloat(
            initialValue = 1f,
            targetValue = 1.04f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1800, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "btnBreathing",
        )

    // ── Floating copper dust particles (truly random positions) ──
    val particleSpeeds = remember {
        FloatArray(PARTICLE_COUNT) { Random.nextFloat() * 0.0015f + 0.0008f }
    }
    val particlePhases = remember { FloatArray(PARTICLE_COUNT) { Random.nextFloat() * 6.2832f } }
    val particleRadii = remember {
        FloatArray(PARTICLE_COUNT) { i ->
            when {
                i < 30 -> Random.nextFloat() * 1.5f + 0.5f // 0.5-2.0 dp (nahe Sterne)
                i < 60 -> Random.nextFloat() * 0.6f + 0.3f // 0.3-0.9 dp (mittlere)
                else -> Random.nextFloat() * 0.35f + 0.15f // 0.15-0.5 dp (ferne Sterne)
            }
        }
    }
    val particleColorIdx = remember {
        IntArray(PARTICLE_COUNT) { Random.nextInt(ParticleColors.size) }
    }
    val particleX = remember { FloatArray(PARTICLE_COUNT) { Random.nextFloat() } }
    val particleY = remember { FloatArray(PARTICLE_COUNT) { Random.nextFloat() } }
    val particleCycles = remember { IntArray(PARTICLE_COUNT) { 0 } }
    val particleTime = remember { mutableStateOf(0f) }

    LaunchedEffect("particles") {
        val start = System.nanoTime()
        while (true) {
            delay(16L)
            particleTime.value = (System.nanoTime() - start) / 1_000_000f
        }
    }

    // ── Cinematic entrance sequence ──
    LaunchedEffect("entrance") {
        // Phase 1: Hero image — fade in + slow zoom settle (Ken Burns feel)
        launch { heroAlpha.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
        launch { heroScale.animateTo(1f, tween(3000, easing = FastOutSlowInEasing)) }

        delay(600)

        // Phase 2: Title slides up
        launch { titleAlpha.animateTo(1f, tween(700)) }
        launch { titleOffsetY.animateTo(0f, tween(800, easing = FastOutSlowInEasing)) }

        delay(400)

        // Phase 3: Accent divider
        launch { accentAlpha.animateTo(1f, tween(500)) }

        delay(300)

        // Phase 4: Button flies in from below
        launch { btnAlpha.animateTo(1f, tween(500)) }
        launch { btnOffsetY.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }

        delay(400)

        // Phase 5: Subtitle fades in last
        subAlpha.animateTo(1f, tween(600))
    }

    // ── Layout ──
    Box(modifier = Modifier.fillMaxSize().background(SplashBg)) {
        // Exit wrapper: content fades out, background stays solid black
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = exitAlpha.value }) {
            // Layer 1: Ambient radial glows (warm atmosphere behind everything)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush =
                        Brush.radialGradient(
                            colors = listOf(CopperLight.copy(alpha = 0.07f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.25f),
                            radius = size.width * 0.9f,
                        ),
                    radius = size.width * 0.9f,
                    center = Offset(size.width * 0.5f, size.height * 0.25f),
                )
                drawCircle(
                    brush =
                        Brush.radialGradient(
                            colors = listOf(GoldAccent.copy(alpha = 0.04f), Color.Transparent),
                            center = Offset(size.width * 0.7f, size.height * 0.55f),
                            radius = size.width * 0.7f,
                        ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.7f, size.height * 0.55f),
                )
            }

            // Layer 2: Hero image (golden book — top ~65% of screen)
            Image(
                painter = painterResource(R.drawable.splash_hero_book),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier.fillMaxWidth()
                        .fillMaxHeight(0.65f)
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = heroAlpha.value
                            scaleX = heroScale.value
                            scaleY = heroScale.value
                            clip = true
                        },
            )

            // Layer 3: Gradient overlay — covers full screen to hide ambient glows in gap
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colorStops =
                                        arrayOf(
                                            0.0f to Color.Transparent,
                                            0.29f to Color.Transparent,
                                            0.46f to SplashBg.copy(alpha = 0.7f),
                                            0.57f to SplashBg.copy(alpha = 0.95f),
                                            0.65f to SplashBg,
                                            1.0f to SplashBg,
                                        )
                                )
                        )
            )

            // Layer 3.5: Floating copper dust — cos wave ensures position change at alpha=0
            Canvas(modifier = Modifier.fillMaxSize()) {
                val t = particleTime.value
                for (i in 0 until PARTICLE_COUNT) {
                    val wave = t * particleSpeeds[i] + particlePhases[i]
                    val cycle = (wave / 6.2832f).toInt()
                    // cos(k*2π)=1 → alpha=0 at cycle boundary = invisible when position changes
                    val alpha = (1f - cos(wave)) / 2f

                    if (cycle != particleCycles[i]) {
                        particleCycles[i] = cycle
                        particleX[i] = Random.nextFloat()
                        particleY[i] = Random.nextFloat()
                    }

                    if (alpha > 0.02f) {
                        drawCircle(
                            color = ParticleColors[particleColorIdx[i]].copy(alpha = alpha * 0.35f),
                            radius = particleRadii[i] * density,
                            center = Offset(particleX[i] * size.width, particleY[i] * size.height),
                        )
                    }
                }
            }

            // Layer 4: Content — title, accent line, button, subtitle
            Column(
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .widthIn(max = 420.dp)
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // "BEST JOURNAL" — cinematic editorial headline
                Text(
                    "BEST\nJOURNAL",
                    style =
                        MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 52.sp,
                            lineHeight = 54.sp,
                            letterSpacing = (-1.5).sp,
                        ),
                    color = OnSurface.copy(alpha = titleAlpha.value),
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.graphicsLayer { translationY = titleOffsetY.value * density },
                )

                Spacer(Modifier.height(16.dp))

                // Copper accent divider
                Box(
                    modifier =
                        Modifier.width(48.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(CopperLight.copy(alpha = 0.4f * accentAlpha.value))
                )

                Spacer(Modifier.height(36.dp))

                // Start button — gradient fill + breathing glow
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.fillMaxWidth().graphicsLayer {
                            alpha = btnAlpha.value
                            translationY = btnOffsetY.value * density
                            scaleX = btnBreathScale
                            scaleY = btnBreathScale
                        },
                ) {
                    // Breathing glow behind the button (API 31+ visual; no-op on older)
                    Box(
                        modifier =
                            Modifier.matchParentSize()
                                .graphicsLayer { alpha = glowAlpha }
                                .blur(20.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CopperDeep)
                    )

                    Button(
                        onClick = { if (!isExiting.value) isExiting.value = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        elevation =
                            ButtonDefaults.buttonElevation(
                                defaultElevation = 10.dp,
                                pressedElevation = 2.dp,
                            ),
                    ) {
                        Box(
                            modifier =
                                Modifier.fillMaxSize()
                                    .background(
                                        brush =
                                            Brush.horizontalGradient(
                                                colors = listOf(CopperLight, CopperDeep)
                                            ),
                                        shape = RoundedCornerShape(16.dp),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(R.string.splash_start),
                                style =
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                    ),
                                color = OnPrimaryDark,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Subtle brand subtitle
                Text(
                    stringResource(R.string.splash_subtitle),
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 3.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                    color = WarmSandVariant.copy(alpha = 0.6f * subAlpha.value),
                    textAlign = TextAlign.Center,
                )
            }
        } // end exit-wrapper
    }
}

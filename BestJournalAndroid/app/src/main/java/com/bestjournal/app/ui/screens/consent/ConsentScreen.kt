package com.bestjournal.app.ui.screens.consent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.InsertChart
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
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
// Consent Screen — "Privacy Welcome"
// Matches the Splash/Onboarding "Gilded Sanctum" palette
// Cinematic entrance, info cards, two-button footer with animated glow
// ═══════════════════════════════════════════════════════════════════

private val ConsentBg = Color(0xFF131313)
private val CopperLight = Color(0xFFFFB689)
private val CopperDeep = Color(0xFFDF741E)
private val GoldAccent = Color(0xFFECC165)
private val OnSurface = Color(0xFFE5E2E1)
private val OnSurfaceMuted = Color(0xFFB8B2AE)
private val OnPrimaryDark = Color(0xFF512400)
private val CardBg = Color(0x1FFFB689)
private val CardBorder = Color(0x33FFB689)

private val ParticleColors =
    listOf(Color(0xFFFFB689), Color(0xFFDF741E), Color(0xFFECC165), Color(0xFFC4875A))

private const val PARTICLE_COUNT = 120

@Composable
fun ConsentScreen(
    viewModel: ConsentViewModel,
    onOpenDocument: (LegalDocument) -> Unit,
    onContinue: () -> Unit,
) {
    // Exit animation shared by both buttons
    val isExiting = remember { mutableStateOf<ExitMode?>(null) }
    val exitAlpha = remember { Animatable(1f) }

    LaunchedEffect(isExiting.value) {
        val mode = isExiting.value ?: return@LaunchedEffect
        when (mode) {
            ExitMode.AcceptAll -> viewModel.acceptAll()
            ExitMode.OnlyRequired -> viewModel.acceptWithoutAnalytics()
        }
        exitAlpha.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        delay(50)
        onContinue()
    }

    // Entrance animations
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(24f) }
    val accentAlpha = remember { Animatable(0f) }
    val card1Alpha = remember { Animatable(0f) }
    val card2Alpha = remember { Animatable(0f) }
    val card3Alpha = remember { Animatable(0f) }
    val linksAlpha = remember { Animatable(0f) }
    val btnAlpha = remember { Animatable(0f) }
    val btnOffsetY = remember { Animatable(40f) }

    // Breathing glow for primary button
    val infinite = rememberInfiniteTransition(label = "consent")
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
            targetValue = 1.03f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1800, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "btnBreathing",
        )

    // Floating copper dust particles
    val particleSpeeds = remember {
        FloatArray(PARTICLE_COUNT) { Random.nextFloat() * 0.0015f + 0.0008f }
    }
    val particlePhases = remember { FloatArray(PARTICLE_COUNT) { Random.nextFloat() * 6.2832f } }
    val particleRadii = remember {
        FloatArray(PARTICLE_COUNT) { i ->
            when {
                i < 20 -> Random.nextFloat() * 1.4f + 0.5f
                i < 50 -> Random.nextFloat() * 0.6f + 0.3f
                else -> Random.nextFloat() * 0.35f + 0.15f
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

    // Cinematic entrance sequence
    LaunchedEffect("entrance") {
        launch { titleAlpha.animateTo(1f, tween(700)) }
        launch { titleOffsetY.animateTo(0f, tween(800, easing = FastOutSlowInEasing)) }
        delay(300)
        launch { accentAlpha.animateTo(1f, tween(500)) }
        delay(200)
        launch { card1Alpha.animateTo(1f, tween(500)) }
        delay(150)
        launch { card2Alpha.animateTo(1f, tween(500)) }
        delay(150)
        launch { card3Alpha.animateTo(1f, tween(500)) }
        delay(200)
        launch { linksAlpha.animateTo(1f, tween(500)) }
        delay(150)
        launch { btnAlpha.animateTo(1f, tween(500)) }
        launch { btnOffsetY.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
    }

    Box(modifier = Modifier.fillMaxSize().background(ConsentBg)) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = exitAlpha.value }) {
            // Ambient radial glows
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush =
                        Brush.radialGradient(
                            colors = listOf(CopperLight.copy(alpha = 0.08f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.18f),
                            radius = size.width * 0.9f,
                        ),
                    radius = size.width * 0.9f,
                    center = Offset(size.width * 0.5f, size.height * 0.18f),
                )
                drawCircle(
                    brush =
                        Brush.radialGradient(
                            colors = listOf(GoldAccent.copy(alpha = 0.04f), Color.Transparent),
                            center = Offset(size.width * 0.2f, size.height * 0.78f),
                            radius = size.width * 0.7f,
                        ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.2f, size.height * 0.78f),
                )
            }

            // Floating dust particles
            Canvas(modifier = Modifier.fillMaxSize()) {
                val t = particleTime.value
                for (i in 0 until PARTICLE_COUNT) {
                    val wave = t * particleSpeeds[i] + particlePhases[i]
                    val cycle = (wave / 6.2832f).toInt()
                    val alpha = (1f - cos(wave)) / 2f
                    if (cycle != particleCycles[i]) {
                        particleCycles[i] = cycle
                        particleX[i] = Random.nextFloat()
                        particleY[i] = Random.nextFloat()
                    }
                    if (alpha > 0.02f) {
                        drawCircle(
                            color =
                                ParticleColors[particleColorIdx[i]].copy(alpha = alpha * 0.35f),
                            radius = particleRadii[i] * density,
                            center = Offset(particleX[i] * size.width, particleY[i] * size.height),
                        )
                    }
                }
            }

            // Content — compact layout, no scrolling needed on typical phones
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 22.dp)
                        .padding(top = 18.dp, bottom = 18.dp)
                        .widthIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Title
                Text(
                    text = stringResource(R.string.consent_title),
                    style =
                        MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            lineHeight = 32.sp,
                            letterSpacing = (-0.4).sp,
                        ),
                    color = OnSurface.copy(alpha = titleAlpha.value),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { translationY = titleOffsetY.value * density },
                )

                Spacer(Modifier.height(10.dp))

                // Copper accent divider
                Box(
                    modifier =
                        Modifier.width(48.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(CopperLight.copy(alpha = 0.45f * accentAlpha.value))
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.consent_intro),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = OnSurfaceMuted.copy(alpha = accentAlpha.value),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))

                // Info card 1 — local storage (compact)
                InfoCard(
                    icon = Icons.Rounded.Lock,
                    title = stringResource(R.string.consent_card1_title),
                    body = stringResource(R.string.consent_card1_body),
                    alpha = card1Alpha.value,
                )

                Spacer(Modifier.height(8.dp))

                // Info card 2 — AI processing (compact)
                InfoCard(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.consent_card2_title),
                    body = stringResource(R.string.consent_card2_body),
                    alpha = card2Alpha.value,
                )

                Spacer(Modifier.height(8.dp))

                // Info card 3 — analytics (compact)
                InfoCard(
                    icon = Icons.Rounded.InsertChart,
                    title = stringResource(R.string.consent_card3_title),
                    body = stringResource(R.string.consent_card3_body),
                    alpha = card3Alpha.value,
                )

                Spacer(Modifier.height(14.dp))

                // Document links: Datenschutz + Nutzungsbedingungen side by side, Impressum centered below
                Column(
                    modifier = Modifier.graphicsLayer { alpha = linksAlpha.value }.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DocLinkChip(label = stringResource(R.string.legal_title_datenschutz)) {
                            onOpenDocument(LegalDocument.Datenschutz)
                        }
                        DocLinkChip(
                            label = stringResource(R.string.legal_title_nutzungsbedingungen)
                        ) { onOpenDocument(LegalDocument.Nutzungsbedingungen) }
                    }
                    DocLinkChip(label = stringResource(R.string.legal_title_impressum)) {
                        onOpenDocument(LegalDocument.Impressum)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Confirmation / agreement text
                Text(
                    text = stringResource(R.string.consent_confirmation),
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                        ),
                    color = OnSurfaceMuted.copy(alpha = 0.9f * linksAlpha.value),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))

                // Primary button (narrower than full width for visual balance)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.width(240.dp).graphicsLayer {
                            alpha = btnAlpha.value
                            translationY = btnOffsetY.value * density
                            scaleX = btnBreathScale
                            scaleY = btnBreathScale
                        },
                ) {
                    Box(
                        modifier =
                            Modifier.matchParentSize()
                                .graphicsLayer { alpha = glowAlpha }
                                .blur(20.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CopperDeep)
                    )
                    Button(
                        onClick = { if (isExiting.value == null) isExiting.value = ExitMode.AcceptAll },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
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
                                        brush = Brush.horizontalGradient(listOf(CopperLight, CopperDeep)),
                                        shape = RoundedCornerShape(16.dp),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.consent_accept_all),
                                style =
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp,
                                    ),
                                color = OnPrimaryDark,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        if (isExiting.value == null) isExiting.value = ExitMode.OnlyRequired
                    },
                    modifier =
                        Modifier.graphicsLayer { alpha = btnAlpha.value }
                            .width(240.dp)
                            .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border =
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            CopperLight.copy(alpha = 0.55f),
                        ),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = OnSurface,
                        ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.consent_disable_stats),
                        fontSize = 13.sp,
                        letterSpacing = 0.4.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private enum class ExitMode { AcceptAll, OnlyRequired }

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String, alpha: Float) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .graphicsLayer { this.alpha = alpha }
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(16.dp))
                .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier =
                    Modifier.size(40.dp).clip(CircleShape).background(CopperDeep.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CopperLight,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        ),
                    color = OnSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
                    color = OnSurfaceMuted,
                )
            }
        }
    }
}

@Composable
private fun DocLinkChip(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            color = GoldAccent,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

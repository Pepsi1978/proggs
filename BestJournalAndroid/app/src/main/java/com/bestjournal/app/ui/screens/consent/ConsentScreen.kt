package com.bestjournal.app.ui.screens.consent

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestjournal.app.R
import com.bestjournal.app.ui.components.PrivacyPreferences
import com.bestjournal.app.ui.components.PrivacyPreferencesSheet
import com.bestjournal.app.util.Constants
import kotlin.math.cos
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════
// Consent Screen v4 — Layered consent (EDSA 03/2023, Day-One-style)
// Friendly welcome + three equally prominent buttons + optional
// "Manuelle Auswahl" opens the PrivacyPreferencesSheet on demand.
// ═══════════════════════════════════════════════════════════════════

private val ConsentBg = Color(0xFF131313)
private val CopperLight = Color(0xFFFFB689)
private val CopperDeep = Color(0xFFDF741E)
private val GoldAccent = Color(0xFFECC165)
private val OnSurface = Color(0xFFE5E2E1)
private val OnSurfaceMuted = Color(0xFFB8B2AE)
private val OnPrimaryDark = Color(0xFF512400)
private val BulletBg = Color(0x14ECC165)
private val BulletBorder = Color(0x33ECC165)

private val ParticleColors =
    listOf(Color(0xFFFFB689), Color(0xFFDF741E), Color(0xFFECC165), Color(0xFFC4875A))

private const val PARTICLE_COUNT = 80

@Composable
fun ConsentScreen(
    viewModel: ConsentViewModel,
    onOpenDocument: (LegalDocument) -> Unit,
    onContinue: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isCaliforniaLikely = remember(configuration) {
        val lang = configuration.locales[0].language
        val country = configuration.locales[0].country
        lang.equals("en", ignoreCase = true) &&
            (country.equals("US", ignoreCase = true) || country.isEmpty())
    }

    // Current toggle state (starts all off — EDSA 03/2023 default).
    val analyticsOn by viewModel.analyticsEnabled.collectAsStateWithLifecycle()
    val groqOn by viewModel.groqEnabled.collectAsStateWithLifecycle()
    val geminiOn by viewModel.geminiEnabled.collectAsStateWithLifecycle()
    val ttsOn by viewModel.ttsEnabled.collectAsStateWithLifecycle()
    val driveOn by viewModel.driveBackupEnabled.collectAsStateWithLifecycle()
    val doNotSellOn by viewModel.doNotSellEnabled.collectAsStateWithLifecycle()

    var showSheet by remember { mutableStateOf(false) }
    val isExiting = remember { mutableStateOf<ExitMode?>(null) }
    val exitAlpha = remember { Animatable(1f) }

    LaunchedEffect(isExiting.value) {
        val mode = isExiting.value ?: return@LaunchedEffect
        when (mode) {
            ExitMode.AcceptAll -> viewModel.acceptAll()
            ExitMode.MinimumOnly -> viewModel.acceptMinimumOnly()
            ExitMode.SaveSelection -> viewModel.saveSelection()
        }
        exitAlpha.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        delay(50)
        onContinue()
    }

    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(24f) }
    val introAlpha = remember { Animatable(0f) }
    val bulletsAlpha = remember { Animatable(0f) }
    val btnAlpha = remember { Animatable(0f) }
    val btnOffsetY = remember { Animatable(40f) }

    val infinite = rememberInfiniteTransition(label = "consent")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "btnGlow",
    )
    val btnBreathScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "btnBreath",
    )

    // Particles (lighter than v3 — keeps screen calm, not overwhelming)
    val particleSpeeds = remember { FloatArray(PARTICLE_COUNT) { Random.nextFloat() * 0.0015f + 0.0008f } }
    val particlePhases = remember { FloatArray(PARTICLE_COUNT) { Random.nextFloat() * 6.2832f } }
    val particleRadii = remember {
        FloatArray(PARTICLE_COUNT) { i ->
            when {
                i < 10 -> Random.nextFloat() * 1.4f + 0.5f
                i < 35 -> Random.nextFloat() * 0.6f + 0.3f
                else -> Random.nextFloat() * 0.35f + 0.15f
            }
        }
    }
    val particleColorIdx = remember { IntArray(PARTICLE_COUNT) { Random.nextInt(ParticleColors.size) } }
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

    LaunchedEffect("entrance") {
        launch { titleAlpha.animateTo(1f, tween(600)) }
        launch { titleOffsetY.animateTo(0f, tween(700, easing = FastOutSlowInEasing)) }
        delay(250)
        launch { introAlpha.animateTo(1f, tween(500)) }
        delay(200)
        launch { bulletsAlpha.animateTo(1f, tween(500)) }
        delay(250)
        launch { btnAlpha.animateTo(1f, tween(500)) }
        launch { btnOffsetY.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
    }

    Box(modifier = Modifier.fillMaxSize().background(ConsentBg)) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = exitAlpha.value }) {
            // Ambient glow
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CopperLight.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.15f),
                        radius = size.width * 0.9f,
                    ),
                    radius = size.width * 0.9f,
                    center = Offset(size.width * 0.5f, size.height * 0.15f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GoldAccent.copy(alpha = 0.04f), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.8f),
                        radius = size.width * 0.7f,
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.2f, size.height * 0.8f),
                )
            }

            // Floating dust
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
                            color = ParticleColors[particleColorIdx[i]].copy(alpha = alpha * 0.30f),
                            radius = particleRadii[i] * density,
                            center = Offset(particleX[i] * size.width, particleY[i] * size.height),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 18.dp)
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Title — warm welcome
                Text(
                    text = stringResource(R.string.consent_welcome_title),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        letterSpacing = (-0.4).sp,
                    ),
                    color = OnSurface.copy(alpha = titleAlpha.value),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { translationY = titleOffsetY.value * density },
                )

                Spacer(Modifier.height(10.dp))

                // Copper accent divider
                Box(
                    modifier = Modifier.width(48.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CopperLight.copy(alpha = 0.45f * titleAlpha.value))
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.consent_welcome_intro),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                    color = OnSurfaceMuted.copy(alpha = introAlpha.value),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(22.dp))

                // Promise bullets — reassuring, short
                Column(
                    modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = bulletsAlpha.value },
                ) {
                    PromiseBullet(
                        icon = Icons.Rounded.Gavel,
                        text = stringResource(R.string.consent_promise_local),
                    )
                    Spacer(Modifier.height(10.dp))
                    PromiseBullet(
                        icon = Icons.Rounded.Tune,
                        text = stringResource(R.string.consent_promise_no_training),
                    )
                    Spacer(Modifier.height(10.dp))
                    PromiseBullet(
                        icon = Icons.Rounded.Favorite,
                        text = stringResource(R.string.consent_promise_optional_ai),
                    )
                }

                Spacer(Modifier.height(28.dp))

                // Three equally prominent buttons (EDSA 03/2023 + UWG EmpCo-RL 27.09.2026 — no dark patterns).
                // Visually identical COPPER-FILLED buttons — same gradient, same height/width, same text style,
                // no glow, no breathing animation, no elevation highlight for a single option.
                // "Equally prominent" per EDSA means visually interchangeable, not necessarily monochrome.
                Column(
                    modifier = Modifier.fillMaxWidth().graphicsLayer {
                        alpha = btnAlpha.value
                        translationY = btnOffsetY.value * density
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Accept all
                    ConsentFilledButton(
                        label = stringResource(R.string.consent_btn_accept_all),
                        onClick = {
                            if (isExiting.value == null) isExiting.value = ExitMode.AcceptAll
                        },
                        glowAlpha = glowAlpha,
                        breathScale = btnBreathScale,
                    )

                    // Required only
                    ConsentFilledButton(
                        label = stringResource(R.string.consent_btn_minimum_only),
                        onClick = {
                            if (isExiting.value == null) isExiting.value = ExitMode.MinimumOnly
                        },
                        glowAlpha = glowAlpha,
                        breathScale = btnBreathScale,
                    )

                    // Manual selection (opens bottom sheet)
                    ConsentFilledButton(
                        label = stringResource(R.string.consent_btn_manual_selection),
                        leadingIcon = Icons.Rounded.Tune,
                        onClick = { showSheet = true },
                        glowAlpha = glowAlpha,
                        breathScale = btnBreathScale,
                    )
                }

                Spacer(Modifier.height(22.dp))

                // Legal document links (compact chips)
                Column(
                    modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = btnAlpha.value },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DocLinkChip(label = stringResource(R.string.legal_title_datenschutz)) {
                            onOpenDocument(LegalDocument.Datenschutz)
                        }
                        DocLinkChip(label = stringResource(R.string.legal_title_nutzungsbedingungen)) {
                            onOpenDocument(LegalDocument.Nutzungsbedingungen)
                        }
                    }
                    DocLinkChip(label = stringResource(R.string.legal_title_impressum)) {
                        onOpenDocument(LegalDocument.Impressum)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Inline URL footer — opens legal docs directly in browser
                ConsentLegalFooter()

                Spacer(Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.consent_footer_version),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                    ),
                    color = OnSurfaceMuted.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    // Privacy preferences sheet (opens via "Manuelle Auswahl")
    PrivacyPreferencesSheet(
        visible = showSheet,
        initial = PrivacyPreferences(
            analytics = analyticsOn,
            groq = groqOn,
            gemini = geminiOn,
            tts = ttsOn,
            driveBackup = driveOn,
            doNotSell = doNotSellOn,
        ),
        onDismiss = { showSheet = false },
        onSave = { prefs ->
            viewModel.setAnalytics(prefs.analytics)
            viewModel.setGroq(prefs.groq)
            viewModel.setGemini(prefs.gemini)
            viewModel.setTts(prefs.tts)
            viewModel.setDriveBackup(prefs.driveBackup)
            if (prefs.doNotSell) viewModel.setDoNotSell(true) else viewModel.setDoNotSell(false)
            // User saved their selection — exit consent flow.
            if (isExiting.value == null) isExiting.value = ExitMode.SaveSelection
        },
        showDoNotSell = isCaliforniaLikely,
    )
}

private enum class ExitMode { AcceptAll, MinimumOnly, SaveSelection }

/**
 * Visually-identical copper-filled button used for all three consent options.
 *
 * All three buttons share the SAME gradient fill, the SAME breathing scale animation,
 * the SAME glow halo, the SAME elevation, the SAME size and text style. Because the
 * styling is literally the same for every button, none is visually more prominent than
 * the others — EDSA Guideline 03/2023 §3.1.2 and UWG EmpCo-RL compliant.
 *
 * [glowAlpha] and [breathScale] are driven by a shared InfiniteTransition at the call
 * site, so all buttons pulse and glow in sync.
 */
@Composable
private fun ConsentFilledButton(
    label: String,
    onClick: () -> Unit,
    glowAlpha: Float,
    breathScale: Float,
    leadingIcon: ImageVector? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.width(280.dp).graphicsLayer {
            scaleX = breathScale
            scaleY = breathScale
        },
    ) {
        // Glow halo (same alpha animation for every button)
        Box(
            modifier = Modifier.fillMaxWidth()
                .height(54.dp)
                .graphicsLayer { alpha = glowAlpha }
                .clip(RoundedCornerShape(16.dp))
                .background(CopperDeep)
        )
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 10.dp,
                pressedElevation = 2.dp,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(listOf(CopperLight, CopperDeep)),
                        shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = OnPrimaryDark,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            fontSize = 15.sp,
                        ),
                        color = OnPrimaryDark,
                    )
                }
            }
        }
    }
}

@Composable
private fun PromiseBullet(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BulletBg)
            .border(BorderStroke(1.dp, BulletBorder), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(30.dp)
                .clip(CircleShape)
                .background(GoldAccent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = OnSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 17.sp,
        )
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
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Inline footer row with three browser-links to legal documents.
 * Locale-aware: picks de/en/ko URL strings depending on device language,
 * with German as fallback. Opens the URL via ACTION_VIEW intent.
 */
@Composable
private fun ConsentLegalFooter() {
    val context = LocalContext.current
    val locale = java.util.Locale.getDefault().language

    val datenschutzUrl = stringResource(
        when (locale) {
            "en" -> R.string.legal_url_datenschutz_en
            "ko" -> R.string.legal_url_datenschutz_ko
            else -> R.string.legal_url_datenschutz_de
        }
    )
    val impressumUrl = stringResource(
        when (locale) {
            "en" -> R.string.legal_url_impressum_en
            "ko" -> R.string.legal_url_impressum_ko
            else -> R.string.legal_url_impressum_de
        }
    )
    val agbUrl = stringResource(
        when (locale) {
            "en" -> R.string.legal_url_agb_en
            "ko" -> R.string.legal_url_agb_ko
            else -> R.string.legal_url_agb_de
        }
    )

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        TextButton(
            onClick = { openUrl(datenschutzUrl) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = stringResource(R.string.consent_footer_link_datenschutz),
                color = OnSurfaceMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
            )
        }
        Text(
            text = "·",
            color = OnSurfaceMuted.copy(alpha = 0.5f),
            fontSize = 11.sp,
        )
        TextButton(
            onClick = { openUrl(impressumUrl) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = stringResource(R.string.consent_footer_link_impressum),
                color = OnSurfaceMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
            )
        }
        Text(
            text = "·",
            color = OnSurfaceMuted.copy(alpha = 0.5f),
            fontSize = 11.sp,
        )
        TextButton(
            onClick = { openUrl(agbUrl) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = stringResource(R.string.consent_footer_link_agb),
                color = OnSurfaceMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

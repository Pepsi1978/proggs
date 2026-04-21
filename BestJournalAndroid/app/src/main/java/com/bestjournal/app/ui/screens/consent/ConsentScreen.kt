package com.bestjournal.app.ui.screens.consent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.InsertChart
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NoAccounts
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.bestjournal.app.util.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════
// Consent Screen v3 — Granular toggles (GDPR Art. 7 + EDSA 03/2023)
// One screen, all privacy decisions, three equally prominent footer buttons.
// Gilded Sanctum palette kept; content column scrollable.
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
private val EssentialBg = Color(0x14ECC165)
private val EssentialBorder = Color(0x33ECC165)

private val ParticleColors =
    listOf(Color(0xFFFFB689), Color(0xFFDF741E), Color(0xFFECC165), Color(0xFFC4875A))

private const val PARTICLE_COUNT = 80

@Composable
fun ConsentScreen(
    viewModel: ConsentViewModel,
    onOpenDocument: (LegalDocument) -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isCaliforniaLikely =
        remember(configuration) {
            // CCPA Do-Not-Sell toggle shown for en-US / en-CA (North American English locales).
            val lang = configuration.locales[0].language
            val country = configuration.locales[0].country
            lang.equals("en", ignoreCase = true) &&
                (country.equals("US", ignoreCase = true) || country.isEmpty())
        }

    val analyticsOn by viewModel.analyticsEnabled.collectAsState()
    val crashlyticsOn by viewModel.crashlyticsEnabled.collectAsState()
    val groqOn by viewModel.groqEnabled.collectAsState()
    val geminiOn by viewModel.geminiEnabled.collectAsState()
    val ttsOn by viewModel.ttsEnabled.collectAsState()
    val driveOn by viewModel.driveBackupEnabled.collectAsState()
    val doNotSellOn by viewModel.doNotSellEnabled.collectAsState()

    // Exit animation shared by all three footer buttons
    val isExiting = remember { mutableStateOf<ExitMode?>(null) }
    val exitAlpha = remember { Animatable(1f) }

    LaunchedEffect(isExiting.value) {
        val mode = isExiting.value ?: return@LaunchedEffect
        when (mode) {
            ExitMode.AcceptAll -> viewModel.acceptAll()
            ExitMode.SaveSelection -> viewModel.saveSelection()
            ExitMode.MinimumOnly -> viewModel.acceptMinimumOnly()
        }
        exitAlpha.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        delay(50)
        onContinue()
    }

    // Entrance animations
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(24f) }
    val contentAlpha = remember { Animatable(0f) }
    val btnAlpha = remember { Animatable(0f) }
    val btnOffsetY = remember { Animatable(40f) }

    // Breathing glow for primary button (Accept All)
    val infinite = rememberInfiniteTransition(label = "consent")
    val glowAlpha by
        infinite.animateFloat(
            initialValue = 0.12f,
            targetValue = 0.32f,
            animationSpec =
                infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "btnGlow",
        )

    // Floating copper dust particles
    val particleSpeeds = remember {
        FloatArray(PARTICLE_COUNT) { Random.nextFloat() * 0.0015f + 0.0008f }
    }
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

    LaunchedEffect("entrance") {
        launch { titleAlpha.animateTo(1f, tween(600)) }
        launch { titleOffsetY.animateTo(0f, tween(700, easing = FastOutSlowInEasing)) }
        delay(200)
        launch { contentAlpha.animateTo(1f, tween(500)) }
        delay(300)
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
                            colors = listOf(CopperLight.copy(alpha = 0.06f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.12f),
                            radius = size.width * 0.9f,
                        ),
                    radius = size.width * 0.9f,
                    center = Offset(size.width * 0.5f, size.height * 0.12f),
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
                            color = ParticleColors[particleColorIdx[i]].copy(alpha = alpha * 0.28f),
                            radius = particleRadii[i] * density,
                            center = Offset(particleX[i] * size.width, particleY[i] * size.height),
                        )
                    }
                }
            }

            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp)
                        .padding(top = 14.dp, bottom = 12.dp)
                        .widthIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.consent_title),
                    style =
                        MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            lineHeight = 30.sp,
                            letterSpacing = (-0.4).sp,
                        ),
                    color = OnSurface.copy(alpha = titleAlpha.value),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { translationY = titleOffsetY.value * density },
                )

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier =
                        Modifier.width(48.dp)
                            .height(2.dp)
                            .background(CopperLight.copy(alpha = 0.45f * titleAlpha.value))
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.consent_intro),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                    color = OnSurfaceMuted.copy(alpha = titleAlpha.value),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(12.dp))

                Column(modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }) {
                    // ── Essential section (no toggle) ─────────────────────────
                    SectionLabel(stringResource(R.string.consent_section_essential))
                    Spacer(Modifier.height(6.dp))
                    EssentialCard(
                        icon = Icons.Rounded.Lock,
                        title = stringResource(R.string.consent_toggle_local_title),
                        body = stringResource(R.string.consent_toggle_local_body),
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Optional section with granular toggles ────────────────
                    SectionLabel(stringResource(R.string.consent_section_optional))
                    Spacer(Modifier.height(6.dp))

                    // "No training" badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    color = Color(0x2AECC165),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.consent_no_training_badge),
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    ConsentToggleCard(
                        icon = Icons.Rounded.InsertChart,
                        title = stringResource(R.string.consent_toggle_analytics_title),
                        body = stringResource(R.string.consent_toggle_analytics_body),
                        checked = analyticsOn,
                        enabled = !doNotSellOn,
                        onCheckedChange = viewModel::setAnalytics,
                    )
                    Spacer(Modifier.height(8.dp))
                    ConsentToggleCard(
                        icon = Icons.Rounded.BugReport,
                        title = stringResource(R.string.consent_toggle_crashlytics_title),
                        body = stringResource(R.string.consent_toggle_crashlytics_body),
                        checked = crashlyticsOn,
                        enabled = !doNotSellOn,
                        onCheckedChange = viewModel::setCrashlytics,
                    )
                    Spacer(Modifier.height(8.dp))
                    ConsentToggleCard(
                        icon = Icons.Rounded.Mic,
                        title = stringResource(R.string.consent_toggle_groq_title),
                        body = stringResource(R.string.consent_toggle_groq_body),
                        checked = groqOn,
                        enabled = !doNotSellOn,
                        onCheckedChange = viewModel::setGroq,
                    )
                    Spacer(Modifier.height(8.dp))
                    ConsentToggleCard(
                        icon = Icons.Rounded.AutoAwesome,
                        title = stringResource(R.string.consent_toggle_gemini_title),
                        body = stringResource(R.string.consent_toggle_gemini_body),
                        checked = geminiOn,
                        enabled = !doNotSellOn,
                        onCheckedChange = viewModel::setGemini,
                    )
                    Spacer(Modifier.height(8.dp))
                    ConsentToggleCard(
                        icon = Icons.Rounded.VolumeUp,
                        title = stringResource(R.string.consent_toggle_tts_title),
                        body = stringResource(R.string.consent_toggle_tts_body),
                        checked = ttsOn,
                        enabled = !doNotSellOn,
                        onCheckedChange = viewModel::setTts,
                    )
                    Spacer(Modifier.height(8.dp))
                    ConsentToggleCard(
                        icon = Icons.Rounded.Backup,
                        title = stringResource(R.string.consent_toggle_drive_title),
                        body = stringResource(R.string.consent_toggle_drive_body),
                        checked = driveOn,
                        enabled = !doNotSellOn,
                        onCheckedChange = viewModel::setDriveBackup,
                    )

                    if (isCaliforniaLikely) {
                        Spacer(Modifier.height(12.dp))
                        ConsentToggleCard(
                            icon = Icons.Rounded.NoAccounts,
                            title = stringResource(R.string.consent_toggle_do_not_sell_title),
                            body = stringResource(R.string.consent_toggle_do_not_sell_body),
                            checked = doNotSellOn,
                            enabled = true,
                            onCheckedChange = viewModel::setDoNotSell,
                            emphasized = true,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.consent_sensitive_note),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp,
                            ),
                        color = OnSurfaceMuted.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )

                    Spacer(Modifier.height(12.dp))

                    // Document links
                    Column(
                        modifier = Modifier.fillMaxWidth(),
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
                }

                Spacer(Modifier.height(14.dp))

                // Three equally-prominent buttons (EDSA 03/2023 Dark Patterns)
                Column(
                    modifier =
                        Modifier.fillMaxWidth().graphicsLayer {
                            alpha = btnAlpha.value
                            translationY = btnOffsetY.value * density
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Primary: Accept All (glow)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.width(260.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier.matchParentSize()
                                    .graphicsLayer { alpha = glowAlpha }
                                    .background(
                                        brush =
                                            Brush.horizontalGradient(
                                                listOf(CopperLight, CopperDeep)
                                            ),
                                        shape = RoundedCornerShape(16.dp),
                                    )
                        )
                        Button(
                            onClick = {
                                if (isExiting.value == null) isExiting.value = ExitMode.AcceptAll
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            elevation =
                                ButtonDefaults.buttonElevation(
                                    defaultElevation = 8.dp,
                                    pressedElevation = 2.dp,
                                ),
                        ) {
                            Box(
                                modifier =
                                    Modifier.fillMaxSize()
                                        .background(
                                            brush =
                                                Brush.horizontalGradient(
                                                    listOf(CopperLight, CopperDeep)
                                                ),
                                            shape = RoundedCornerShape(16.dp),
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.consent_btn_accept_all),
                                    style =
                                        MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            letterSpacing = 0.4.sp,
                                        ),
                                    color = OnPrimaryDark,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Secondary: Save Selection (same prominence)
                    OutlinedButton(
                        onClick = {
                            if (isExiting.value == null) isExiting.value = ExitMode.SaveSelection
                        },
                        modifier = Modifier.width(260.dp).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, CopperLight.copy(alpha = 0.7f)),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = OnSurface,
                            ),
                    ) {
                        Text(
                            text = stringResource(R.string.consent_btn_save_selection),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp,
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Tertiary: Minimum Only (also full-width, same button style)
                    OutlinedButton(
                        onClick = {
                            if (isExiting.value == null) isExiting.value = ExitMode.MinimumOnly
                        },
                        modifier = Modifier.width(260.dp).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, OnSurfaceMuted.copy(alpha = 0.55f)),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = OnSurface,
                            ),
                    ) {
                        Text(
                            text = stringResource(R.string.consent_btn_minimum_only),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.3.sp,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text =
                        stringResource(
                            R.string.consent_footer_version,
                            Constants.CURRENT_POLICY_VERSION,
                        ),
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                        ),
                    color = OnSurfaceMuted.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private enum class ExitMode { AcceptAll, SaveSelection, MinimumOnly }

@Composable
private fun SectionLabel(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier.width(12.dp).height(1.dp).background(CopperLight.copy(alpha = 0.5f))
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = CopperLight,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier =
                Modifier.height(1.dp)
                    .weight(1f)
                    .background(CopperLight.copy(alpha = 0.25f))
        )
    }
}

@Composable
private fun EssentialCard(icon: ImageVector, title: String, body: String) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .background(EssentialBg, RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, EssentialBorder), RoundedCornerShape(14.dp))
                .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier =
                    Modifier.size(32.dp)
                        .background(GoldAccent.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = OnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = body,
                    color = OnSurfaceMuted,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                )
            }
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ConsentToggleCard(
    icon: ImageVector,
    title: String,
    body: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    emphasized: Boolean = false,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    val bodyAlpha = if (enabled) 1f else 0.45f
    val cardBg = if (emphasized) Color(0x22FFB689) else CardBg
    val cardBorder = if (emphasized) CopperLight.copy(alpha = 0.5f) else CardBorder

    Box(
        modifier =
            Modifier.fillMaxWidth()
                .background(cardBg, RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(14.dp))
                .graphicsLayer { alpha = bodyAlpha }
                .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(32.dp)
                            .background(CopperDeep.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CopperLight,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = OnSurface,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { expanded = !expanded },
                    ) {
                        Text(
                            text = stringResource(R.string.consent_details_expand),
                            color = GoldAccent,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Icon(
                            imageVector =
                                if (expanded) Icons.Rounded.ExpandLess
                                else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Switch(
                    checked = checked,
                    onCheckedChange = if (enabled) onCheckedChange else null,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = OnPrimaryDark,
                            checkedTrackColor = CopperLight,
                            checkedBorderColor = CopperDeep,
                            uncheckedThumbColor = OnSurfaceMuted,
                            uncheckedTrackColor = Color(0x22FFFFFF),
                            uncheckedBorderColor = OnSurfaceMuted.copy(alpha = 0.4f),
                        ),
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(120)),
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = body,
                        color = OnSurfaceMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DocLinkChip(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(
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

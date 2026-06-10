package com.bestjournal.app.ui.screens.dashboard

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestjournal.app.R
import com.bestjournal.app.domain.model.Advice
import com.bestjournal.app.domain.model.AdvicePriority
import com.bestjournal.app.domain.model.TopAction
import com.bestjournal.app.ui.components.AdviceCategoryCard
import com.bestjournal.app.ui.components.AiInfoBanner
import com.bestjournal.app.ui.components.FreeLimitIndicator
import com.bestjournal.app.ui.components.GlassCard
import com.bestjournal.app.ui.components.NeonDivider
import com.bestjournal.app.ui.components.ParticleBackground
import com.bestjournal.app.ui.components.ShimmerLoadingEffect
import com.bestjournal.app.ui.components.TwinklingStars
import com.bestjournal.app.ui.theme.CustomPalette
import com.bestjournal.app.ui.theme.FeatureAccentOrange
import com.bestjournal.app.ui.theme.GoalPalette
import com.bestjournal.app.ui.theme.InsightPalette
import com.bestjournal.app.ui.theme.LocalIsDarkTheme
import com.bestjournal.app.ui.theme.NeonAmber
import com.bestjournal.app.ui.theme.NeonCyan
import com.bestjournal.app.ui.theme.NeonEmerald
import com.bestjournal.app.ui.theme.NeonRed
import com.bestjournal.app.ui.theme.SummaryPalette
import com.bestjournal.app.ui.components.PrivacyGateHost
import com.bestjournal.app.ui.components.rememberPrivacyGateState
import com.bestjournal.app.util.EdgeTtsPlayer
import com.bestjournal.app.util.PrivacyGateHelper
import com.bestjournal.app.util.rememberHapticAction

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onNavigateToPaywall: (String) -> Unit = {}) {
    val blocks by viewModel.adviceBlocks.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboardUsed by viewModel.weeklyDashboardUsed.collectAsStateWithLifecycle()
    val dashboardMax by viewModel.weeklyDashboardMax.collectAsStateWithLifecycle()
    val isFreemiumUser by viewModel.isFreemiumUser.collectAsStateWithLifecycle()
    val isDark = LocalIsDarkTheme.current
    val doHaptic = rememberHapticAction()
    var showLegendDialog by remember { mutableStateOf(false) }
    var selectedAdvice by remember { mutableStateOf<Pair<Advice, String>?>(null) }
    var selectedCategoryBlock by remember {
        mutableStateOf<com.bestjournal.app.domain.model.AdviceBlock?>(null)
    }
    val context = LocalContext.current
    var isDashboardSpeaking by remember { mutableStateOf(false) }
    var isDashboardTtsLoading by remember { mutableStateOf(false) }
    val dashboardTts = remember { EdgeTtsPlayer(context) }
    // NK1: Per-service pre-usage consent gates (EDSA 03/2023, BGH Planet49).
    // Groq-Gate is in JournalScreen (voice recording). Gemini + Edge-TTS gates here.
    val geminiGate = rememberPrivacyGateState(PrivacyGateHelper.CloudService.Gemini)
    val edgeTtsGate = rememberPrivacyGateState(PrivacyGateHelper.CloudService.EdgeTts)
    PrivacyGateHost(
        state = geminiGate,
        titleRes = R.string.privacy_gate_gemini_title,
        bodyRes = R.string.privacy_gate_gemini_body,
        acceptRes = R.string.privacy_gate_gemini_accept,
        declineRes = R.string.privacy_gate_gemini_cancel,
    )
    PrivacyGateHost(
        state = edgeTtsGate,
        titleRes = R.string.privacy_gate_tts_title,
        bodyRes = R.string.privacy_gate_tts_body,
        acceptRes = R.string.privacy_gate_tts_accept,
        declineRes = R.string.privacy_gate_tts_cancel,
    )
    val dashboardTtsPrefs = remember {
        try {
            com.bestjournal.app.util.EncryptedPrefsProvider.get(context)
        } catch (_: Exception) {
            null
        }
    }
    // Frank-Wunsch 2026-05-15: Buendelt alle TTS-Dependencies fuer die Top-Aktionen-
    // Detail-Dialoge in einem einzigen Objekt, das durch die 5 Block-Composables
    // weitergereicht wird. Bewusst KEIN remember, weil doHaptic ein nicht-hashbares
    // Lambda ist; das Objekt ist klein und der Overhead pro Recomposition zu klein
    // um den Wartungsaufwand einer @Stable-Markierung zu rechtfertigen.
    val topActionTtsCtx =
        DashboardTtsContext(
            tts = dashboardTts,
            edgeTtsGate = edgeTtsGate,
            ttsPrefs = dashboardTtsPrefs,
            doHaptic = doHaptic,
            context = context,
        )

    DisposableEffect(Unit) {
        onDispose {
            dashboardTts.stop()
            dashboardTts.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isDark) {
            ParticleBackground()
            TwinklingStars()
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed title bar (does not scroll)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(R.string.dashboard_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        com.bestjournal.app.ui.components.SunMoonToggle()
                    }
                    Row {
                        IconButton(
                            onClick = {
                                doHaptic(HapticFeedbackType.LongPress)
                                showLegendDialog = true
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                stringResource(R.string.dashboard_legend),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (uiState.canUndo) {
                            IconButton(
                                onClick = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    viewModel.undoDashboard()
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.Undo,
                                    stringResource(R.string.action_undo),
                                    tint = NeonAmber,
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                doHaptic(HapticFeedbackType.LongPress)
                                geminiGate.run { viewModel.refreshDashboard() }
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Refresh,
                                stringResource(R.string.dashboard_refresh),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                val lastUpdated = remember(uiState.isLoading) { viewModel.getLastUpdatedText() }
                if (lastUpdated != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = lastUpdated,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // AI Act Art. 50 inline marker — right-aligned, with 8.dp (~2mm) offset
                        // from the edge; longer translations grow leftwards while the trailing
                        // edge stays fixed.
                        com.bestjournal.app.ui.components.AiGeneratedBadgeInline(
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
            }

            // Scrollable content
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // C1 — Profil-Header: zeigt das aktive Profil + Kurzfokus.
                // Bei Custom-Profilen mit gesetztem fokus_kern wird dieser bevorzugt
                // angezeigt — das ist die KI-eigene Zusammenfassung des Auftrags-Kerns.
                // Faellt fokus_kern leer aus, wird der erste Teil des Profil-Prompts gezeigt.
                if (uiState.activeProfileLabel.isNotBlank()) {
                    item(key = "profile_header") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text =
                                        stringResource(
                                            R.string.dashboard_active_profile_label,
                                            uiState.activeProfileLabel,
                                        ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                                // Bug-Fix 2026-05-15: customFokusKern darf NUR bei
                                // Custom-Profilen verwendet werden. Sonst ueberschreibt der
                                // aus dem Prefs gelesene Wert (vom letzten Custom-Profil-Refresh
                                // uebriggeblieben) die statischen Beschreibungen aller
                                // eingebauten Profile — alle Profile haetten denselben Text.
                                val isCustomProfile =
                                    uiState.currentScenario >=
                                        com.bestjournal.app.util.Constants.FIRST_CUSTOM_SCENARIO_INDEX
                                val focusText =
                                    if (isCustomProfile && uiState.customFokusKern.isNotBlank())
                                        uiState.customFokusKern
                                    else uiState.activeProfileFocus
                                if (focusText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = focusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        // Frank-Vorgabe 2026-05-15: max 3 Zeilen, bei Ueberlauf
                                        // mit Ellipsis abkuerzen — der Header soll kompakt bleiben.
                                        maxLines = 3,
                                        overflow =
                                            androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
                // B3 — Hinweis-Karte bei leerem Custom-Profil-Prompt. Wird statt
                // des normalen Dashboards gerendert, damit der Benutzer den fehlenden
                // Schritt nicht uebersieht.
                if (uiState.emptyCustomPromptWarning) {
                    item(key = "empty_custom_warning") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.dashboard_empty_custom_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.dashboard_empty_custom_message),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                    }
                    return@LazyColumn
                }

                if (uiState.showAiInfoBanner) {
                    item(key = "ai_banner") {
                        AiInfoBanner(onDismiss = { viewModel.dismissAiInfoBanner() })
                    }
                }

                // Weekly review upsell — shown for free users arriving from Sunday notification
                if (uiState.showWeeklyReviewBanner) {
                    item(key = "weekly_review_upsell") {
                        GlassCard(
                            glowColor = InsightPalette.primary,
                            glowIntensity = 0.3f,
                            cornerRadius = 20.dp,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier =
                                        Modifier.size(52.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        InsightPalette.primary,
                                                        InsightPalette.secondary,
                                                    )
                                                )
                                            ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.dashboard_weekly_review_waiting),
                                    style =
                                        MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                    color = InsightPalette.primary,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text =
                                        stringResource(
                                            R.string.dashboard_weekly_review_upsell_body
                                        ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.onWeeklyReviewUpsellClicked()
                                        onNavigateToPaywall("weekly_review")
                                    },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = InsightPalette.primary,
                                            contentColor = Color.White,
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 14.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.dashboard_discover_premium),
                                        style =
                                            MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = { viewModel.dismissWeeklyReviewBanner() }) {
                                    Text(
                                        stringResource(R.string.action_later),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.isLoading) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            ShimmerLoadingEffect(height = 80.dp, cornerRadius = 16.dp)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) {
                                Text(
                                    stringResource(R.string.dashboard_please_wait),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (uiState.isScenarioSwitch)
                                        stringResource(R.string.dashboard_loading_profile_switch)
                                    else if (uiState.isDeleteUpdate)
                                        stringResource(R.string.dashboard_loading_delete_update)
                                    else if (uiState.isAutoUpdate)
                                        stringResource(R.string.dashboard_loading_auto_update)
                                    else stringResource(R.string.dashboard_loading_default),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                // Dashboard limit message
                if (uiState.dashboardLimitMessage != null) {
                    item {
                        GlassCard(glowIntensity = 0.1f) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier =
                                        Modifier.size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.08f
                                                )
                                            ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.HourglassEmpty,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.dashboardLimitMessage ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { viewModel.dismissLimitMessage() }) {
                                    Text(
                                        stringResource(R.string.action_understood),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                // Analysis error message
                if (uiState.errorMessage != null) {
                    item {
                        GlassCard(glowColor = NeonRed, glowIntensity = 0.15f) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier =
                                        Modifier.size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.12f
                                                )
                                            ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Coffee,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text =
                                        uiState.errorMessage
                                            ?: stringResource(
                                                R.string.dashboard_gemini_unavailable
                                            ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        viewModel.clearError()
                                        geminiGate.run { viewModel.refreshDashboard() }
                                    },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                ) {
                                    Text(stringResource(R.string.action_retry))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = { viewModel.clearError() }) {
                                    Text(
                                        stringResource(R.string.action_later),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                if (blocks.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (uiState.currentScenario >= com.bestjournal.app.util.Constants.FIRST_CUSTOM_SCENARIO_INDEX) {
                                    val customPrompt = viewModel.getCustomPrompt()
                                    if (customPrompt.isBlank()) {
                                        Text(
                                            stringResource(R.string.dashboard_no_custom_focus),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            stringResource(R.string.dashboard_custom_focus_hint),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = TextAlign.Center,
                                        )
                                    } else {
                                        Text(
                                            stringResource(R.string.dashboard_no_analysis_yet),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            stringResource(
                                                R.string.dashboard_create_entries_custom
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                } else {
                                    Text(
                                        stringResource(R.string.dashboard_no_analysis_yet),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        stringResource(R.string.dashboard_create_entries_default),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }

                if (blocks.isNotEmpty()) {
                    // Contextual upsell banner — shown once after first analysis for free users
                    // (all
                    // scenarios)
                    if (uiState.showAnalysisUpsellBanner) {
                        item(key = "analysis_upsell") {
                            GlassCard(
                                glowColor = NeonAmber,
                                glowIntensity = 0.25f,
                                cornerRadius = 16.dp,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            NeonAmber,
                                                            NeonAmber.copy(alpha = 0.6f),
                                                        )
                                                    )
                                                ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.dashboard_like_analysis),
                                            style =
                                                MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                            color = NeonAmber,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text =
                                                stringResource(
                                                    R.string.dashboard_premium_upsell_body
                                                ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    doHaptic(HapticFeedbackType.LongPress)
                                                    viewModel.dismissAnalysisUpsellBanner()
                                                }
                                            ) {
                                                Text(
                                                    stringResource(R.string.action_later),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline,
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Button(
                                                onClick = {
                                                    doHaptic(HapticFeedbackType.LongPress)
                                                    viewModel.onAnalysisUpsellClicked()
                                                    onNavigateToPaywall("first_analysis")
                                                },
                                                colors =
                                                    ButtonDefaults.buttonColors(
                                                        containerColor = NeonAmber,
                                                        contentColor = Color.Black,
                                                    ),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding =
                                                    PaddingValues(
                                                        horizontal = 16.dp,
                                                        vertical = 6.dp,
                                                    ),
                                            ) {
                                                Text(
                                                    stringResource(R.string.dashboard_learn_more),
                                                    style =
                                                        MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Free-limit progress indicator for freemium users
                    if (isFreemiumUser) {
                        item(key = "free_limit_indicator") {
                            val remaining = (dashboardMax - dashboardUsed).coerceAtLeast(0)
                            LaunchedEffect(dashboardUsed) {
                                viewModel.trackFreeLimitShown(remaining)
                            }
                            FreeLimitIndicator(
                                usedCount = dashboardUsed,
                                maxCount = dashboardMax,
                                featureLabel = stringResource(R.string.dashboard_feature_analyses),
                                onUpgradeClick = {
                                    viewModel.onFreeLimitUpgradeClicked()
                                    onNavigateToPaywall("free_limit")
                                },
                                visible = true,
                            )
                        }
                    }

                    if (uiState.currentScenario == 0) {
                        // ═══════ ZUSAMMENFASSUNG DASHBOARD ═══════
                        // Cool blue/indigo theme — informative, not urgent

                        // Key Insights block (replaces Top 5 Maßnahmen)
                        val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                        if (topActions.isNotEmpty()) {
                            item {
                                SummaryKeyInsightsBlock(
                                    actions = topActions,
                                    ttsCtx = topActionTtsCtx,
                                )
                            }
                        }

                        // Overview (replaces Gesamtanalyse)
                        item {
                            val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                            GlassCard(glowColor = SummaryPalette.primary, glowIntensity = 0.2f) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        stringResource(
                                        R.string.dashboard_summary_overview_title
                                        ),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = SummaryPalette.primary,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = overallAnalysis,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    AnalysisTtsShareRow(
                                        text = overallAnalysis,
                                        tts = dashboardTts,
                                        edgeTtsGate = edgeTtsGate,
                                        isSpeaking = isDashboardSpeaking,
                                        isTtsLoading = isDashboardTtsLoading,
                                        onSpeakingChange = { isDashboardSpeaking = it },
                                        onTtsLoadingChange = { isDashboardTtsLoading = it },
                                        doHaptic = doHaptic,
                                        context = context,
                                        ttsPrefs = dashboardTtsPrefs,
                                    )
                                }
                            }
                        }

                        // Theme cards — same LazyRow but with summary styling
                        item {
                            val categoryScrollIsolation = remember {
                                object : NestedScrollConnection {
                                    override fun onPostScroll(
                                        consumed: Offset,
                                        available: Offset,
                                        source: NestedScrollSource,
                                    ): Offset = Offset(available.x, 0f)

                                    override suspend fun onPostFling(
                                        consumed: Velocity,
                                        available: Velocity,
                                    ): Velocity = Velocity(available.x, 0f)
                                }
                            }

                            Box(modifier = Modifier.nestedScroll(categoryScrollIsolation)) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    itemsIndexed(blocks, key = { _, block -> block.id }) {
                                        index,
                                        block ->
                                        AdviceCategoryCard(
                                            block = block,
                                            isSelected = index == uiState.selectedCategoryIndex,
                                            onClick = {
                                                viewModel.selectCategory(index)
                                                selectedCategoryBlock = block
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        // All observations header + relevance legend below it
                        item(key = "all_observations") {
                            Spacer(modifier = Modifier.height(20.dp))
                            NeonDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.dashboard_all_observations),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SummaryPalette.secondary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        item(key = "relevance_legend") { SummaryRelevanceLegend() }

                        // All observations sorted by relevance (remembered to skip re-computation on every recompose)
                        val allObservations =
                            blocks
                                .flatMap { block ->
                                    block.advices.map { advice ->
                                        Triple(advice, block.categoryName, block.entropyLevel)
                                    }
                                }
                                .sortedBy { (advice, _, _) ->
                                    when (advice.priority) {
                                        AdvicePriority.HIGH -> 0
                                        AdvicePriority.MEDIUM -> 1
                                        AdvicePriority.LOW -> 2
                                    }
                                }

                        itemsIndexed(
                            allObservations,
                            key = { _, (advice, catName, _) -> "obs::$catName::${advice.title}" },
                        ) { _, (advice, catName, _) ->
                            SummaryObservationCard(
                                advice = advice,
                                categoryName = catName,
                                onClick = { selectedAdvice = Pair(advice, catName) },
                            )
                        }
                    } else if (uiState.currentScenario == 2) {
                        // ═══════ SELBSTERKENNTNIS DASHBOARD ═══════
                        // Warm violet/rose theme — introspective, personal

                        val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                        if (topActions.isNotEmpty()) {
                            item {
                                InsightKeyBlock(
                                    actions = topActions,
                                    ttsCtx = topActionTtsCtx,
                                )
                            }
                        }

                        item {
                            val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                            GlassCard(glowColor = InsightPalette.primary, glowIntensity = 0.2f) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        stringResource(R.string.dashboard_inner_mirror),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = InsightPalette.primary,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = overallAnalysis,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    AnalysisTtsShareRow(
                                        text = overallAnalysis,
                                        tts = dashboardTts,
                                        edgeTtsGate = edgeTtsGate,
                                        isSpeaking = isDashboardSpeaking,
                                        isTtsLoading = isDashboardTtsLoading,
                                        onSpeakingChange = { isDashboardSpeaking = it },
                                        onTtsLoadingChange = { isDashboardTtsLoading = it },
                                        doHaptic = doHaptic,
                                        context = context,
                                        ttsPrefs = dashboardTtsPrefs,
                                    )
                                }
                            }
                        }

                        item {
                            val categoryScrollIsolation = remember {
                                object : NestedScrollConnection {
                                    override fun onPostScroll(
                                        consumed: Offset,
                                        available: Offset,
                                        source: NestedScrollSource,
                                    ): Offset = Offset(available.x, 0f)

                                    override suspend fun onPostFling(
                                        consumed: Velocity,
                                        available: Velocity,
                                    ): Velocity = Velocity(available.x, 0f)
                                }
                            }
                            Box(modifier = Modifier.nestedScroll(categoryScrollIsolation)) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    itemsIndexed(blocks, key = { _, block -> block.id }) {
                                        index,
                                        block ->
                                        AdviceCategoryCard(
                                            block = block,
                                            isSelected = index == uiState.selectedCategoryIndex,
                                            onClick = {
                                                viewModel.selectCategory(index)
                                                selectedCategoryBlock = block
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "all_insights") {
                            Spacer(modifier = Modifier.height(20.dp))
                            NeonDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.dashboard_all_insights),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = InsightPalette.primary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        item(key = "insight_depth_legend") { InsightDepthLegend() }

                        val allInsights =
                            blocks
                                .flatMap { block ->
                                    block.advices.map { advice ->
                                        Triple(advice, block.categoryName, block.entropyLevel)
                                    }
                                }
                                .sortedBy { (advice, _, _) ->
                                    when (advice.priority) {
                                        AdvicePriority.HIGH -> 0
                                        AdvicePriority.MEDIUM -> 1
                                        AdvicePriority.LOW -> 2
                                    }
                                }

                        itemsIndexed(
                            allInsights,
                            key = { _, (advice, catName, _) -> "ins::$catName::${advice.title}" },
                        ) { _, (advice, catName, _) ->
                            InsightCard(
                                advice = advice,
                                categoryName = catName,
                                onClick = { selectedAdvice = Pair(advice, catName) },
                            )
                        }
                    } else if (uiState.currentScenario == 3) {
                        // ═══════ PERSÖNLICHE ZIELE DASHBOARD ═══════
                        // Green/gold theme — motivating, progress-oriented

                        val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                        if (topActions.isNotEmpty()) {
                            item {
                                GoalNextStepsBlock(
                                    actions = topActions,
                                    ttsCtx = topActionTtsCtx,
                                )
                            }
                        }

                        item {
                            val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                            GlassCard(glowColor = GoalPalette.primary, glowIntensity = 0.2f) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        stringResource(R.string.dashboard_goal_overview),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = GoalPalette.primary,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        overallAnalysis,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    AnalysisTtsShareRow(
                                        text = overallAnalysis,
                                        tts = dashboardTts,
                                        edgeTtsGate = edgeTtsGate,
                                        isSpeaking = isDashboardSpeaking,
                                        isTtsLoading = isDashboardTtsLoading,
                                        onSpeakingChange = { isDashboardSpeaking = it },
                                        onTtsLoadingChange = { isDashboardTtsLoading = it },
                                        doHaptic = doHaptic,
                                        context = context,
                                        ttsPrefs = dashboardTtsPrefs,
                                    )
                                }
                            }
                        }

                        item {
                            val categoryScrollIsolation = remember {
                                object : NestedScrollConnection {
                                    override fun onPostScroll(
                                        consumed: Offset,
                                        available: Offset,
                                        source: NestedScrollSource,
                                    ): Offset = Offset(available.x, 0f)

                                    override suspend fun onPostFling(
                                        consumed: Velocity,
                                        available: Velocity,
                                    ): Velocity = Velocity(available.x, 0f)
                                }
                            }
                            Box(modifier = Modifier.nestedScroll(categoryScrollIsolation)) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    itemsIndexed(blocks, key = { _, block -> block.id }) {
                                        index,
                                        block ->
                                        AdviceCategoryCard(
                                            block = block,
                                            isSelected = index == uiState.selectedCategoryIndex,
                                            onClick = {
                                                viewModel.selectCategory(index)
                                                selectedCategoryBlock = block
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "all_goals") {
                            Spacer(modifier = Modifier.height(20.dp))
                            NeonDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.dashboard_all_goals),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = GoalPalette.primary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        item(key = "goal_status_legend") { GoalStatusLegend() }

                        val allGoals =
                            blocks
                                .flatMap { block ->
                                    block.advices.map { advice ->
                                        Triple(advice, block.categoryName, block.entropyLevel)
                                    }
                                }
                                .sortedBy { (advice, _, _) ->
                                    when (advice.priority) {
                                        AdvicePriority.HIGH -> 0
                                        AdvicePriority.MEDIUM -> 1
                                        AdvicePriority.LOW -> 2
                                    }
                                }

                        itemsIndexed(
                            allGoals,
                            key = { _, (advice, catName, _) -> "goal::$catName::${advice.title}" },
                        ) { _, (advice, catName, _) ->
                            GoalCard(
                                advice = advice,
                                categoryName = catName,
                                onClick = { selectedAdvice = Pair(advice, catName) },
                            )
                        }
                    } else if (uiState.currentScenario >= com.bestjournal.app.util.Constants.FIRST_CUSTOM_SCENARIO_INDEX) {
                        // ═══════ INDIVIDUELLE ANALYSE DASHBOARD ═══════
                        val fallbackTop5 =
                            context.getString(R.string.dashboard_top_results_fallback)
                        val fallbackAnalyse =
                            context.getString(R.string.dashboard_analysis_fallback)
                        val fallbackErgebnisse =
                            context.getString(R.string.dashboard_all_results_fallback)
                        val customTop5 = uiState.customHeaderTop5.ifBlank { fallbackTop5 }
                        val customAnalyse = uiState.customHeaderAnalyse.ifBlank { fallbackAnalyse }
                        val customErgebnisse =
                            uiState.customHeaderErgebnisse.ifBlank { fallbackErgebnisse }

                        val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                        if (topActions.isNotEmpty()) {
                            item {
                                CustomInsightsBlock(
                                    actions = topActions,
                                    ttsCtx = topActionTtsCtx,
                                    title = customTop5,
                                )
                            }
                        }

                        item {
                            val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                            GlassCard(glowColor = CustomPalette.primary, glowIntensity = 0.2f) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        customAnalyse,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = CustomPalette.primary,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        overallAnalysis,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    AnalysisTtsShareRow(
                                        text = overallAnalysis,
                                        tts = dashboardTts,
                                        edgeTtsGate = edgeTtsGate,
                                        isSpeaking = isDashboardSpeaking,
                                        isTtsLoading = isDashboardTtsLoading,
                                        onSpeakingChange = { isDashboardSpeaking = it },
                                        onTtsLoadingChange = { isDashboardTtsLoading = it },
                                        doHaptic = doHaptic,
                                        context = context,
                                        ttsPrefs = dashboardTtsPrefs,
                                    )
                                }
                            }
                        }

                        item {
                            val categoryScrollIsolation = remember {
                                object : NestedScrollConnection {
                                    override fun onPostScroll(
                                        consumed: Offset,
                                        available: Offset,
                                        source: NestedScrollSource,
                                    ): Offset = Offset(available.x, 0f)

                                    override suspend fun onPostFling(
                                        consumed: Velocity,
                                        available: Velocity,
                                    ): Velocity = Velocity(available.x, 0f)
                                }
                            }
                            Box(modifier = Modifier.nestedScroll(categoryScrollIsolation)) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    itemsIndexed(blocks, key = { _, block -> block.id }) {
                                        index,
                                        block ->
                                        AdviceCategoryCard(
                                            block = block,
                                            isSelected = index == uiState.selectedCategoryIndex,
                                            onClick = {
                                                viewModel.selectCategory(index)
                                                selectedCategoryBlock = block
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "all_custom") {
                            Spacer(modifier = Modifier.height(20.dp))
                            NeonDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                customErgebnisse,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = CustomPalette.primary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        item(key = "custom_legend") { CustomRelevanceLegend() }

                        val allCustom =
                            blocks
                                .flatMap { block ->
                                    block.advices.map { advice ->
                                        Triple(advice, block.categoryName, block.entropyLevel)
                                    }
                                }
                                .sortedBy { (advice, _, _) ->
                                    when (advice.priority) {
                                        AdvicePriority.HIGH -> 0
                                        AdvicePriority.MEDIUM -> 1
                                        AdvicePriority.LOW -> 2
                                    }
                                }

                        itemsIndexed(
                            allCustom,
                            key = { _, (advice, catName, _) -> "cust::$catName::${advice.title}" },
                        ) { _, (advice, catName, _) ->
                            CustomResultCard(
                                advice = advice,
                                categoryName = catName,
                                onClick = { selectedAdvice = Pair(advice, catName) },
                            )
                        }
                    } else {
                        // ═══════ DEFAULT DASHBOARD (Entropy) ═══════

                        // Top-5 Entropy-Reducing Actions — visually prominent block
                        val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                        if (topActions.isNotEmpty()) {
                            item {
                                TopActionsBlock(
                                    actions = topActions,
                                    ttsCtx = topActionTtsCtx,
                                )
                            }
                        }

                        // Overall analysis
                        item {
                            val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""

                            GlassCard(glowIntensity = 0.2f) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        stringResource(R.string.dashboard_overall_analysis),
                                        style =
                                            MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                textDecoration = TextDecoration.Underline,
                                            ),
                                        color = NeonAmber,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = overallAnalysis,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    AnalysisTtsShareRow(
                                        text = overallAnalysis,
                                        tts = dashboardTts,
                                        edgeTtsGate = edgeTtsGate,
                                        isSpeaking = isDashboardSpeaking,
                                        isTtsLoading = isDashboardTtsLoading,
                                        onSpeakingChange = { isDashboardSpeaking = it },
                                        onTtsLoadingChange = { isDashboardTtsLoading = it },
                                        doHaptic = doHaptic,
                                        context = context,
                                        ttsPrefs = dashboardTtsPrefs,
                                    )
                                }
                            }
                        }

                        // Category cards — LazyRow with scroll isolation from pager
                        item {
                            val categoryScrollIsolation = remember {
                                object : NestedScrollConnection {
                                    override fun onPostScroll(
                                        consumed: Offset,
                                        available: Offset,
                                        source: NestedScrollSource,
                                    ): Offset = Offset(available.x, 0f)

                                    override suspend fun onPostFling(
                                        consumed: Velocity,
                                        available: Velocity,
                                    ): Velocity = Velocity(available.x, 0f)
                                }
                            }

                            Box(modifier = Modifier.nestedScroll(categoryScrollIsolation)) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    itemsIndexed(blocks, key = { _, block -> block.id }) {
                                        index,
                                        block ->
                                        AdviceCategoryCard(
                                            block = block,
                                            isSelected = index == uiState.selectedCategoryIndex,
                                            onClick = {
                                                viewModel.selectCategory(index)
                                                selectedCategoryBlock = block
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        // Priority legend
                        item(key = "priority_legend") { PriorityLegend() }

                        // ALL recommendations from ALL categories, sorted by priority
                        item(key = "all_recommendations") {
                            Spacer(modifier = Modifier.height(20.dp))
                            NeonDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.dashboard_all_recommendations),
                                style =
                                    MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                color = NeonAmber,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Collect all advices from all blocks with their category name
                        val allAdvicesWithCategory =
                            blocks
                                .flatMap { block ->
                                    block.advices.map { advice ->
                                        Triple(advice, block.categoryName, block.entropyLevel)
                                    }
                                }
                                .sortedBy { (advice, _, _) ->
                                    when (advice.priority) {
                                        AdvicePriority.HIGH -> 0
                                        AdvicePriority.MEDIUM -> 1
                                        AdvicePriority.LOW -> 2
                                    }
                                }

                        itemsIndexed(
                            allAdvicesWithCategory,
                            key = { _, (advice, catName, _) -> "adv::$catName::${advice.title}" },
                        ) { _, (advice, catName, _) ->
                            AdviceCard(
                                advice = advice,
                                categoryName = catName,
                                onClick = { selectedAdvice = Pair(advice, catName) },
                            )
                        }

                        item(key = "ai_output_disclaimer") {
                            com.bestjournal.app.ui.components.AiOutputDisclaimer()
                        }
                    }
                }
            }
        }
    }

    if (showLegendDialog) {
        LegendDialog(scenario = uiState.currentScenario, onDismiss = { showLegendDialog = false })
    }

    selectedAdvice?.let { (advice, catName) ->
        AdviceDerivationDialog(
            advice = advice,
            categoryName = catName,
            onDismiss = { selectedAdvice = null },
        )
    }

    selectedCategoryBlock?.let { block ->
        CategoryDetailDialog(block = block, onDismiss = { selectedCategoryBlock = null })
    }
}

@Composable
private fun PriorityLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        LegendDot(color = NeonRed, label = stringResource(R.string.legend_urgent))
        LegendDot(color = NeonAmber, label = stringResource(R.string.legend_attention))
        LegendDot(color = NeonCyan, label = stringResource(R.string.legend_observe))
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegendDialog(scenario: Int, onDismiss: () -> Unit) {
    val title =
        when {
            scenario == 0 -> stringResource(R.string.profile_summary)
            scenario == 2 -> stringResource(R.string.profile_insight)
            scenario == 3 -> stringResource(R.string.profile_goals)
            scenario >= com.bestjournal.app.util.Constants.FIRST_CUSTOM_SCENARIO_INDEX ->
                stringResource(R.string.profile_custom)
            else -> stringResource(R.string.profile_entropy)
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (scenario) {
                    0 -> {
                        // ── Zusammenfassung ──
                        Text(
                            stringResource(R.string.legend_activity_level),
                            style = MaterialTheme.typography.titleMedium,
                            color = SummaryPalette.primary,
                        )
                        Text(
                            stringResource(R.string.legend_activity_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(
                                color = NeonRed,
                                label = stringResource(R.string.legend_activity_high),
                            )
                            LegendDot(
                                color = NeonAmber,
                                label = stringResource(R.string.legend_activity_medium),
                            )
                            LegendDot(
                                color = NeonEmerald,
                                label = stringResource(R.string.legend_activity_low),
                            )
                        }
                        NeonDivider()
                        Text(
                            stringResource(R.string.legend_observation_relevance),
                            style = MaterialTheme.typography.titleMedium,
                            color = SummaryPalette.primary,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                SummaryPalette.secondary.copy(alpha = 0.12f)
                                            ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = SummaryPalette.secondary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_relevance_high),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(SummaryPalette.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.PushPin,
                                        contentDescription = null,
                                        tint = SummaryPalette.primary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_relevance_medium),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(SummaryPalette.accent.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.BookmarkBorder,
                                        contentDescription = null,
                                        tint = SummaryPalette.accent,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_relevance_low),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    2 -> {
                        // ── Selbsterkenntnis ──
                        Text(
                            stringResource(R.string.legend_reflection_depth),
                            style = MaterialTheme.typography.titleMedium,
                            color = InsightPalette.primary,
                        )
                        Text(
                            stringResource(R.string.legend_reflection_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(
                                color = InsightPalette.primary,
                                label = stringResource(R.string.legend_reflection_deep),
                            )
                            LegendDot(
                                color = InsightPalette.muted,
                                label = stringResource(R.string.legend_reflection_aware),
                            )
                            LegendDot(
                                color = InsightPalette.accent,
                                label = stringResource(R.string.legend_reflection_surface),
                            )
                        }
                        NeonDivider()
                        Text(
                            stringResource(R.string.legend_insight_depth),
                            style = MaterialTheme.typography.titleMedium,
                            color = InsightPalette.primary,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(InsightPalette.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Psychology,
                                        contentDescription = null,
                                        tint = InsightPalette.primary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_insight_deep),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(InsightPalette.muted.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.FavoriteBorder,
                                        contentDescription = null,
                                        tint = InsightPalette.muted,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_insight_aware),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(InsightPalette.accent.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Eco,
                                        contentDescription = null,
                                        tint = InsightPalette.accent,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_insight_surface),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    3 -> {
                        // ── Persönliche Ziele ──
                        Text(
                            stringResource(R.string.legend_progress),
                            style = MaterialTheme.typography.titleMedium,
                            color = GoalPalette.primary,
                        )
                        Text(
                            stringResource(R.string.legend_goal_progress_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(
                                color = GoalPalette.muted,
                                label = stringResource(R.string.legend_blocked_range),
                            )
                            LegendDot(
                                color = GoalPalette.accent,
                                label = stringResource(R.string.legend_in_progress_range),
                            )
                            LegendDot(
                                color = GoalPalette.primary,
                                label = stringResource(R.string.legend_progress_range),
                            )
                        }
                        NeonDivider()
                        Text(
                            stringResource(R.string.legend_goal_status),
                            style = MaterialTheme.typography.titleMedium,
                            color = GoalPalette.primary,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(GoalPalette.muted.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Block,
                                        contentDescription = null,
                                        tint = GoalPalette.muted,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_blocked_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(GoalPalette.accent.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.LockOpen,
                                        contentDescription = null,
                                        tint = GoalPalette.accent,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_open_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(GoalPalette.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = GoalPalette.primary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_progress_success_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    4 -> {
                        // ── Individuelle Analyse ──
                        Text(
                            stringResource(R.string.legend_analysis_value),
                            style = MaterialTheme.typography.titleMedium,
                            color = CustomPalette.primary,
                        )
                        Text(
                            stringResource(R.string.legend_analysis_relevance_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(
                                color = CustomPalette.primary,
                                label = stringResource(R.string.legend_high_focus),
                            )
                            LegendDot(
                                color = CustomPalette.secondary,
                                label = stringResource(R.string.legend_medium_focus),
                            )
                            LegendDot(
                                color = CustomPalette.accent,
                                label = stringResource(R.string.legend_low_focus),
                            )
                        }
                        NeonDivider()
                        Text(
                            stringResource(R.string.legend_result_relevance),
                            style = MaterialTheme.typography.titleMedium,
                            color = CustomPalette.primary,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(CustomPalette.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Whatshot,
                                        contentDescription = null,
                                        tint = CustomPalette.primary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_important_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                CustomPalette.secondary.copy(alpha = 0.12f)
                                            ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.TipsAndUpdates,
                                        contentDescription = null,
                                        tint = CustomPalette.secondary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_relevant_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(CustomPalette.accent.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.EditNote,
                                        contentDescription = null,
                                        tint = CustomPalette.accent,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_note_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    else -> {
                        // ── Räume dein Leben auf (Belastung) ──
                        Text(
                            stringResource(R.string.legend_burden_scale),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            stringResource(R.string.legend_burden_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(
                                color = NeonRed,
                                label = stringResource(R.string.legend_high_burden),
                            )
                            LegendDot(
                                color = NeonAmber,
                                label = stringResource(R.string.legend_medium_burden),
                            )
                            LegendDot(
                                color = NeonEmerald,
                                label = stringResource(R.string.legend_low_burden),
                            )
                        }
                        NeonDivider()
                        Text(
                            stringResource(R.string.legend_priority_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(NeonRed.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Error,
                                        contentDescription = null,
                                        tint = NeonRed,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_priority_high),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(NeonAmber.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = NeonAmber,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_priority_medium),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(NeonCyan.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.legend_priority_low),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
                NeonDivider()
                Text(
                    stringResource(R.string.legend_categories),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.legend_categories_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.action_understood),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

@Composable
private fun CategoryDetailDialog(
    block: com.bestjournal.app.domain.model.AdviceBlock,
    onDismiss: () -> Unit,
) {
    val levelLabel =
        when {
            block.entropyLevel < 0.33f -> stringResource(R.string.level_low)
            block.entropyLevel < 0.66f -> stringResource(R.string.level_medium)
            else -> stringResource(R.string.level_high)
        }
    val levelColor =
        when {
            block.entropyLevel < 0.33f -> NeonEmerald
            block.entropyLevel < 0.66f -> NeonAmber
            else -> NeonRed
        }
    val urgency =
        when {
            block.entropyLevel >= 0.67f -> stringResource(R.string.urgency_high)
            block.entropyLevel >= 0.34f -> stringResource(R.string.urgency_medium)
            else -> stringResource(R.string.urgency_low)
        }

    val sortedAdvices =
        block.advices.sortedBy {
            when (it.priority) {
                AdvicePriority.HIGH -> 0
                AdvicePriority.MEDIUM -> 1
                AdvicePriority.LOW -> 2
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier.size(40.dp)
                                .clip(CircleShape)
                                .background(levelColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector =
                                com.bestjournal.app.ui.components.getIconForCategory(
                                    block.categoryIcon
                                ),
                            contentDescription = null,
                            tint = levelColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        block.categoryName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = levelColor.copy(alpha = 0.15f)) {
                    Text(
                        "$levelLabel (${(block.entropyLevel * 100).toInt()}%) \u2014 $urgency",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = levelColor,
                    )
                }
            }
        },
        text = {
            Column(
                modifier =
                    Modifier.fillMaxWidth().height(400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    block.categorySummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.dashboard_all_recommendations),
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            textDecoration = TextDecoration.Underline
                        ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                sortedAdvices.forEach { advice ->
                    val dotColor =
                        when (advice.priority) {
                            AdvicePriority.HIGH -> NeonRed
                            AdvicePriority.MEDIUM -> NeonAmber
                            AdvicePriority.LOW -> NeonCyan
                        }
                    val dotIcon =
                        when (advice.priority) {
                            AdvicePriority.HIGH -> Icons.Rounded.Whatshot
                            AdvicePriority.MEDIUM -> Icons.Rounded.TipsAndUpdates
                            AdvicePriority.LOW -> Icons.Rounded.AutoAwesome
                        }
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier =
                                    Modifier.size(24.dp)
                                        .clip(CircleShape)
                                        .background(dotColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = dotIcon,
                                    contentDescription = null,
                                    tint = dotColor,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                advice.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            advice.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (advice.connection.isNotBlank()) {
                            Text(
                                "\u2197 ${advice.connection}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.action_close),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

@Composable
private fun AdviceDerivationDialog(advice: Advice, categoryName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    advice.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        categoryName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
        text = {
            Column(
                modifier =
                    Modifier.fillMaxWidth().height(350.dp).verticalScroll(rememberScrollState())
            ) {
                Text(
                    advice.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (advice.derivation.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.dashboard_derived_from),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    advice.derivation.forEach { entry ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(24.dp),
                            ) {
                                Box(
                                    modifier =
                                        Modifier.size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                )
                                Box(
                                    modifier =
                                        Modifier.width(2.dp)
                                            .height(40.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    entry.date,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    entry.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.dashboard_update_for_derivation),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                if (advice.connection.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "\u2197 ${stringResource(R.string.dashboard_connection)}: ${advice.connection}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                com.bestjournal.app.ui.components.AiOutputDisclaimer()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.action_close),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

/**
 * Frank-Wunsch 2026-05-15: Vorlese-Button (oranger Lautsprecher) in jedem
 * Top-Aktionen-Detail-Dialog. Buendelt alle TTS-Dependencies, damit sie durch
 * alle 5 Block-Composables in einem einzigen Parameter durchgereicht werden.
 */
class DashboardTtsContext(
    val tts: com.bestjournal.app.util.EdgeTtsPlayer,
    val edgeTtsGate: com.bestjournal.app.ui.components.PrivacyGateState,
    val ttsPrefs: android.content.SharedPreferences?,
    val doHaptic: (HapticFeedbackType) -> Unit,
    val context: android.content.Context,
)

/**
 * Frank-Wunsch 2026-05-15: gleicher oranger Lautsprecher wie in den
 * Tagebucheintraegen und Rueckblicken, jetzt auch in den Top-Aktionen-Dialogen.
 * Vorgelesen wird die UEBERSCHRIFT (ohne Zahl davor) und der Rest des Texts.
 */
@Composable
private fun TopActionTtsButton(action: TopAction, ttsCtx: DashboardTtsContext) {
    var isSpeaking by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val ttsText =
        buildString {
            append(action.title)
            if (action.description.isNotBlank()) {
                append(". ")
                append(action.description)
            }
            if (action.detailedDescription.isNotBlank()) {
                append(". ")
                append(action.detailedDescription)
            }
        }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            // Sicherheit: Dialog wird geschlossen waehrend gesprochen wird —
            // sofort stoppen, sonst spielt der Audio-Player im Hintergrund weiter.
            if (isSpeaking || isLoading) {
                ttsCtx.tts.stop()
            }
        }
    }
    IconButton(
        onClick = {
            ttsCtx.doHaptic(HapticFeedbackType.LongPress)
            if (isSpeaking || isLoading) {
                ttsCtx.tts.stop()
                isSpeaking = false
                isLoading = false
            } else {
                val ttsOn =
                    ttsCtx.ttsPrefs?.getBoolean(
                        com.bestjournal.app.util.Constants.PREF_TTS_ENABLED,
                        false,
                    ) ?: false
                if (!ttsOn) {
                    android.widget.Toast.makeText(
                            ttsCtx.context,
                            ttsCtx.context.getString(R.string.dashboard_enable_voices),
                            android.widget.Toast.LENGTH_SHORT,
                        )
                        .show()
                } else {
                    ttsCtx.edgeTtsGate.run {
                        isLoading = true
                        isSpeaking = true
                        val voice =
                            ttsCtx.ttsPrefs?.getString(
                                com.bestjournal.app.util.Constants.PREF_EDGE_TTS_VOICE,
                                com.bestjournal.app.util.TtsVoiceRegistry.getLocaleVoices()
                                    .defaultVoiceId,
                            )
                                ?: com.bestjournal.app.util.TtsVoiceRegistry.getLocaleVoices()
                                    .defaultVoiceId
                        ttsCtx.tts.speak(
                            ttsText,
                            voice = voice,
                            onPlaybackStart = { isLoading = false },
                        ) {
                            isSpeaking = false
                            isLoading = false
                        }
                    }
                }
            }
        },
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
            contentDescription =
                if (isSpeaking) stringResource(R.string.dashboard_tts_stop)
                else stringResource(R.string.dashboard_tts_read),
            tint = FeatureAccentOrange,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun TopActionsBlock(actions: List<TopAction>, ttsCtx: DashboardTtsContext) {
    var selectedAction by remember { mutableStateOf<Pair<Int, TopAction>?>(null) }

    GlassCard(glowColor = NeonAmber, glowIntensity = 0.3f) {
        Column {
            Text(
                stringResource(R.string.dashboard_top_5_actions),
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                    ),
                color = NeonAmber,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            actions.forEachIndexed { index, action ->
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedAction = index to action }
                            .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier =
                            Modifier.size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (index) {
                                        0 -> NeonRed
                                        1 -> NeonAmber
                                        2 -> NeonAmber.copy(alpha = 0.8f)
                                        else -> NeonCyan
                                    }
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = action.title,
                            style =
                                MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = action.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < actions.lastIndex) {
                    NeonDivider()
                }
            }
        }
    }

    selectedAction?.let { (index, action) ->
        TopActionDetailDialog(
            action = action,
            index = index,
            onDismiss = { selectedAction = null },
            ttsCtx = ttsCtx,
        )
    }
}

@Composable
private fun TopActionDetailDialog(
    action: TopAction,
    index: Int,
    onDismiss: () -> Unit,
    ttsCtx: DashboardTtsContext,
) {
    val dotColor =
        when (index) {
            0 -> NeonRed
            1 -> NeonAmber
            2 -> NeonAmber.copy(alpha = 0.8f)
            else -> NeonCyan
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(dotColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        style =
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.surface,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        text = {
            Column {
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (action.detailedDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    NeonDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = action.detailedDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopActionTtsButton(action = action, ttsCtx = ttsCtx)
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(
                        stringResource(R.string.action_close),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    )
}

@Composable
private fun AdviceCard(advice: Advice, categoryName: String = "", onClick: () -> Unit = {}) {
    val priorityColor =
        when (advice.priority) {
            AdvicePriority.HIGH -> NeonRed
            AdvicePriority.MEDIUM -> NeonAmber
            AdvicePriority.LOW -> NeonCyan
        }
    val priorityIcon =
        when (advice.priority) {
            AdvicePriority.HIGH -> Icons.Rounded.Whatshot
            AdvicePriority.MEDIUM -> Icons.Rounded.TipsAndUpdates
            AdvicePriority.LOW -> Icons.Rounded.AutoAwesome
        }

    GlassCard(
        modifier = Modifier.clickable { onClick() },
        glowColor = priorityColor,
        glowIntensity = 0.1f,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier.size(32.dp)
                            .clip(CircleShape)
                            .background(priorityColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = priorityIcon,
                        contentDescription = null,
                        tint = priorityColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = advice.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (categoryName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            categoryName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = advice.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (advice.connection.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\u2197 ${advice.connection}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// ZUSAMMENFASSUNG DASHBOARD — Cool blue/indigo theme
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SummaryKeyInsightsBlock(actions: List<TopAction>, ttsCtx: DashboardTtsContext) {
    var selectedAction by remember { mutableStateOf<Pair<Int, TopAction>?>(null) }

    GlassCard(glowColor = SummaryPalette.secondary, glowIntensity = 0.25f) {
        Column {
            Text(
                stringResource(R.string.dashboard_core_insights),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SummaryPalette.secondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.dashboard_summary_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = SummaryPalette.muted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            actions.forEachIndexed { index, action ->
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedAction = index to action }
                            .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Blue gradient numbered badge
                    Box(
                        modifier =
                            Modifier.size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(SummaryPalette.primary, SummaryPalette.secondary)
                                    )
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = action.title,
                            style =
                                MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = action.description,
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < actions.lastIndex) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(1.dp)
                                .background(SummaryPalette.primary.copy(alpha = 0.15f))
                    )
                }
            }
        }
    }

    selectedAction?.let { (index, action) ->
        SummaryInsightDetailDialog(
            action = action,
            index = index,
            onDismiss = { selectedAction = null },
            ttsCtx = ttsCtx,
        )
    }
}

@Composable
private fun SummaryInsightDetailDialog(
    action: TopAction,
    index: Int,
    onDismiss: () -> Unit,
    ttsCtx: DashboardTtsContext,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(SummaryPalette.primary, SummaryPalette.secondary)
                                )
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        style =
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.surface,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        text = {
            Column {
                Text(
                    text = action.description,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                        ),
                    color = SummaryPalette.primary,
                )
                if (action.detailedDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(1.dp)
                                .background(SummaryPalette.primary.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = action.detailedDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopActionTtsButton(action = action, ttsCtx = ttsCtx)
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close), color = SummaryPalette.primary)
                }
            }
        },
    )
}

@Composable
private fun SummaryRelevanceLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        SummaryLegendItem(
            icon = Icons.Rounded.Star,
            label = stringResource(R.string.summary_central),
            color = SummaryPalette.secondary,
        )
        SummaryLegendItem(
            icon = Icons.Rounded.PushPin,
            label = stringResource(R.string.summary_relevant),
            color = SummaryPalette.primary,
        )
        SummaryLegendItem(
            icon = Icons.Rounded.BookmarkBorder,
            label = stringResource(R.string.summary_side_note),
            color = SummaryPalette.accent,
        )
    }
}

@Composable
private fun SummaryLegendItem(
    icon: ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun SummaryObservationCard(
    advice: Advice,
    categoryName: String = "",
    onClick: () -> Unit = {},
) {
    val (icon, glowColor) =
        when (advice.priority) {
            AdvicePriority.HIGH -> Icons.Rounded.Star to SummaryPalette.secondary
            AdvicePriority.MEDIUM -> Icons.Rounded.PushPin to SummaryPalette.primary
            AdvicePriority.LOW -> Icons.Rounded.BookmarkBorder to SummaryPalette.accent
        }

    GlassCard(
        modifier = Modifier.clickable { onClick() },
        glowColor = glowColor,
        glowIntensity = 0.08f,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier.size(24.dp)
                            .clip(CircleShape)
                            .background(glowColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = glowColor,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = advice.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (categoryName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SummaryPalette.primary.copy(alpha = 0.12f),
                    ) {
                        Text(
                            categoryName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = SummaryPalette.primary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = advice.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (advice.connection.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\u2194 ${advice.connection}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SummaryPalette.accent.copy(alpha = 0.8f),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SELBSTERKENNTNIS DASHBOARD — Warm violet/rose theme
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun InsightKeyBlock(actions: List<TopAction>, ttsCtx: DashboardTtsContext) {
    var selectedAction by remember { mutableStateOf<Pair<Int, TopAction>?>(null) }

    GlassCard(glowColor = InsightPalette.primary, glowIntensity = 0.25f) {
        Column {
            Text(
                stringResource(R.string.dashboard_deepest_insights),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = InsightPalette.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.dashboard_insight_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = InsightPalette.muted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            actions.forEachIndexed { index, action ->
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedAction = index to action }
                            .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier =
                            Modifier.size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(InsightPalette.primary, InsightPalette.secondary)
                                    )
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = action.title,
                            style =
                                MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = action.description,
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < actions.lastIndex) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(1.dp)
                                .background(InsightPalette.primary.copy(alpha = 0.15f))
                    )
                }
            }
        }
    }

    selectedAction?.let { (index, action) ->
        InsightDetailDialog(
            action = action,
            index = index,
            onDismiss = { selectedAction = null },
            ttsCtx = ttsCtx,
        )
    }
}

@Composable
private fun InsightDetailDialog(
    action: TopAction,
    index: Int,
    onDismiss: () -> Unit,
    ttsCtx: DashboardTtsContext,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(InsightPalette.primary, InsightPalette.secondary)
                                )
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        style =
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.surface,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    action.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        text = {
            Column {
                Text(
                    action.description,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                        ),
                    color = InsightPalette.primary,
                )
                if (action.detailedDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(1.dp)
                                .background(InsightPalette.primary.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        action.detailedDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopActionTtsButton(action = action, ttsCtx = ttsCtx)
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close), color = InsightPalette.primary)
                }
            }
        },
    )
}

@Composable
private fun InsightDepthLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        InsightLegendItem(
            icon = Icons.Rounded.Visibility,
            label = stringResource(R.string.insight_deep_label),
            color = InsightPalette.primary,
        )
        InsightLegendItem(
            icon = Icons.Rounded.FavoriteBorder,
            label = stringResource(R.string.insight_aware_label),
            color = InsightPalette.muted,
        )
        InsightLegendItem(
            icon = Icons.Rounded.Eco,
            label = stringResource(R.string.insight_surface_label),
            color = InsightPalette.accent,
        )
    }
}

@Composable
private fun InsightLegendItem(
    icon: ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun InsightCard(advice: Advice, categoryName: String = "", onClick: () -> Unit = {}) {
    val (icon, glowColor) =
        when (advice.priority) {
            AdvicePriority.HIGH -> Icons.Rounded.Visibility to InsightPalette.primary
            AdvicePriority.MEDIUM -> Icons.Rounded.FavoriteBorder to InsightPalette.muted
            AdvicePriority.LOW -> Icons.Rounded.Eco to InsightPalette.accent
        }

    GlassCard(
        modifier = Modifier.clickable { onClick() },
        glowColor = glowColor,
        glowIntensity = 0.08f,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier.size(24.dp)
                            .clip(CircleShape)
                            .background(glowColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = glowColor,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    advice.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (categoryName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = InsightPalette.primary.copy(alpha = 0.12f),
                    ) {
                        Text(
                            categoryName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = InsightPalette.primary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                advice.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (advice.connection.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "\uD83D\uDD17 ${advice.connection}",
                    style = MaterialTheme.typography.labelMedium,
                    color = InsightPalette.secondary.copy(alpha = 0.8f),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PERSÖNLICHE ZIELE DASHBOARD — Green/gold theme
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun GoalNextStepsBlock(actions: List<TopAction>, ttsCtx: DashboardTtsContext) {
    var selectedAction by remember { mutableStateOf<Pair<Int, TopAction>?>(null) }

    GlassCard(glowColor = GoalPalette.primary, glowIntensity = 0.25f) {
        Column {
            Text(
                stringResource(R.string.dashboard_goals_next_steps_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GoalPalette.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.dashboard_goal_steps),
                style = MaterialTheme.typography.labelMedium,
                color = GoalPalette.accent,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            actions.forEachIndexed { index, action ->
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedAction = index to action }
                            .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier =
                            Modifier.size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(GoalPalette.primary, GoalPalette.secondary)
                                    )
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${index + 1}",
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            action.title,
                            style =
                                MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            action.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < actions.lastIndex) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(1.dp)
                                .background(GoalPalette.primary.copy(alpha = 0.15f))
                    )
                }
            }
        }
    }

    selectedAction?.let { (index, action) ->
        GoalStepDetailDialog(
            action = action,
            index = index,
            onDismiss = { selectedAction = null },
            ttsCtx = ttsCtx,
        )
    }
}

@Composable
private fun GoalStepDetailDialog(
    action: TopAction,
    index: Int,
    onDismiss: () -> Unit,
    ttsCtx: DashboardTtsContext,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(GoalPalette.primary, GoalPalette.secondary)
                                )
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        style =
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.surface,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    action.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        text = {
            Column {
                Text(
                    action.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = GoalPalette.primary,
                )
                if (action.detailedDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(1.dp)
                                .background(GoalPalette.primary.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        action.detailedDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopActionTtsButton(action = action, ttsCtx = ttsCtx)
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close), color = GoalPalette.primary)
                }
            }
        },
    )
}

@Composable
private fun GoalStatusLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        GoalLegendItem(
            icon = Icons.Rounded.Block,
            label = stringResource(R.string.goal_blocked_label),
            color = GoalPalette.muted,
        )
        GoalLegendItem(
            icon = Icons.Rounded.LockOpen,
            label = stringResource(R.string.goal_open_label),
            color = GoalPalette.accent,
        )
        GoalLegendItem(
            icon = Icons.Rounded.CheckCircle,
            label = stringResource(R.string.goal_progress_label),
            color = GoalPalette.primary,
        )
    }
}

@Composable
private fun GoalLegendItem(
    icon: ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun GoalCard(advice: Advice, categoryName: String = "", onClick: () -> Unit = {}) {
    val (icon, glowColor) =
        when (advice.priority) {
            AdvicePriority.HIGH -> Icons.Rounded.Block to GoalPalette.muted
            AdvicePriority.MEDIUM -> Icons.Rounded.LockOpen to GoalPalette.accent
            AdvicePriority.LOW -> Icons.Rounded.CheckCircle to GoalPalette.primary
        }

    GlassCard(
        modifier = Modifier.clickable { onClick() },
        glowColor = glowColor,
        glowIntensity = 0.08f,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier.size(24.dp)
                            .clip(CircleShape)
                            .background(glowColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = glowColor,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    advice.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (categoryName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GoalPalette.primary.copy(alpha = 0.12f),
                    ) {
                        Text(
                            categoryName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = GoalPalette.primary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                advice.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (advice.connection.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "\u27A1 ${advice.connection}",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoalPalette.secondary.copy(alpha = 0.8f),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// INDIVIDUELLE ANALYSE DASHBOARD — Warm amber/sand theme
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun CustomInsightsBlock(
    actions: List<TopAction>,
    ttsCtx: DashboardTtsContext,
    title: String = stringResource(R.string.dashboard_top_results_fallback),
) {
    var selectedAction by remember { mutableStateOf<Pair<Int, TopAction>?>(null) }

    GlassCard(glowColor = CustomPalette.primary, glowIntensity = 0.25f) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CustomPalette.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.dashboard_custom_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = CustomPalette.muted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            actions.forEachIndexed { index, action ->
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedAction = index to action }
                            .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier =
                            Modifier.size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(CustomPalette.primary, CustomPalette.secondary)
                                    )
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${index + 1}",
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            action.title,
                            style =
                                MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            action.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < actions.lastIndex) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(1.dp)
                                .background(CustomPalette.primary.copy(alpha = 0.15f))
                    )
                }
            }
        }
    }

    selectedAction?.let { (index, action) ->
        CustomDetailDialog(
            action = action,
            index = index,
            onDismiss = { selectedAction = null },
            ttsCtx = ttsCtx,
        )
    }
}

@Composable
private fun CustomDetailDialog(
    action: TopAction,
    index: Int,
    onDismiss: () -> Unit,
    ttsCtx: DashboardTtsContext,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(CustomPalette.primary, CustomPalette.secondary)
                                )
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        style =
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.surface,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    action.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        text = {
            Column {
                Text(
                    action.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = CustomPalette.primary,
                )
                if (action.detailedDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(1.dp)
                                .background(CustomPalette.primary.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        action.detailedDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopActionTtsButton(action = action, ttsCtx = ttsCtx)
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close), color = CustomPalette.primary)
                }
            }
        },
    )
}

@Composable
private fun CustomRelevanceLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        CustomLegendItem(
            icon = Icons.Rounded.Whatshot,
            label = stringResource(R.string.custom_important_label),
            color = CustomPalette.primary,
        )
        CustomLegendItem(
            icon = Icons.Rounded.TipsAndUpdates,
            label = stringResource(R.string.custom_relevant_label),
            color = CustomPalette.secondary,
        )
        CustomLegendItem(
            icon = Icons.Rounded.EditNote,
            label = stringResource(R.string.custom_note_label),
            color = CustomPalette.accent,
        )
    }
}

@Composable
private fun CustomLegendItem(
    icon: ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun CustomResultCard(advice: Advice, categoryName: String = "", onClick: () -> Unit = {}) {
    val (icon, glowColor) =
        when (advice.priority) {
            AdvicePriority.HIGH -> Icons.Rounded.Whatshot to CustomPalette.primary
            AdvicePriority.MEDIUM -> Icons.Rounded.TipsAndUpdates to CustomPalette.secondary
            AdvicePriority.LOW -> Icons.Rounded.EditNote to CustomPalette.accent
        }

    GlassCard(
        modifier = Modifier.clickable { onClick() },
        glowColor = glowColor,
        glowIntensity = 0.08f,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier.size(24.dp)
                            .clip(CircleShape)
                            .background(glowColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = glowColor,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    advice.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (categoryName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CustomPalette.primary.copy(alpha = 0.12f),
                    ) {
                        Text(
                            categoryName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = CustomPalette.primary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                advice.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (advice.connection.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "\u2194 ${advice.connection}",
                    style = MaterialTheme.typography.labelMedium,
                    color = CustomPalette.accent.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun AnalysisTtsShareRow(
    text: String,
    tts: EdgeTtsPlayer,
    edgeTtsGate: com.bestjournal.app.ui.components.PrivacyGateState,
    isSpeaking: Boolean,
    isTtsLoading: Boolean,
    onSpeakingChange: (Boolean) -> Unit,
    onTtsLoadingChange: (Boolean) -> Unit,
    doHaptic: (HapticFeedbackType) -> Unit,
    context: android.content.Context,
    ttsPrefs: android.content.SharedPreferences? = null,
) {
    Spacer(modifier = Modifier.height(12.dp))
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(
            onClick = {
                doHaptic(HapticFeedbackType.LongPress)
                if (isSpeaking || isTtsLoading) {
                    tts.stop()
                    onSpeakingChange(false)
                    onTtsLoadingChange(false)
                } else {
                    val ttsOn =
                        ttsPrefs?.getBoolean(
                            com.bestjournal.app.util.Constants.PREF_TTS_ENABLED,
                            false,
                        ) ?: false
                    if (!ttsOn) {
                        android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.dashboard_enable_voices),
                                android.widget.Toast.LENGTH_SHORT,
                            )
                            .show()
                    } else {
                        edgeTtsGate.run {
                            onTtsLoadingChange(true)
                            onSpeakingChange(true)
                            val voice =
                                ttsPrefs?.getString(
                                    com.bestjournal.app.util.Constants.PREF_EDGE_TTS_VOICE,
                                    com.bestjournal.app.util.TtsVoiceRegistry.getLocaleVoices()
                                        .defaultVoiceId,
                                )
                                    ?: com.bestjournal.app.util.TtsVoiceRegistry.getLocaleVoices()
                                        .defaultVoiceId
                            tts.speak(
                                text,
                                voice = voice,
                                onPlaybackStart = { onTtsLoadingChange(false) },
                            ) {
                                onSpeakingChange(false)
                                onTtsLoadingChange(false)
                            }
                        }
                    }
                }
            },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                contentDescription =
                    if (isSpeaking) stringResource(R.string.dashboard_tts_stop)
                    else stringResource(R.string.dashboard_tts_read),
                tint = FeatureAccentOrange,
                modifier = Modifier.size(24.dp),
            )
        }
        IconButton(
            onClick = {
                doHaptic(HapticFeedbackType.LongPress)
                val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                context.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        context.getString(R.string.dashboard_share_analysis),
                    )
                )
            },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.Rounded.Share,
                contentDescription = stringResource(R.string.dashboard_share),
                tint = FeatureAccentOrange,
                modifier = Modifier.size(24.dp),
            )
        }
    }
    if (isTtsLoading) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = if (LocalIsDarkTheme.current) Color(0xFF5C7AA3) else Color(0xFF1976D2),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.dashboard_tts_wait),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

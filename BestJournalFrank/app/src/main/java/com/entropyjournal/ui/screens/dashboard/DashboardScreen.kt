package com.entropyjournal.ui.screens.dashboard

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.entropyjournal.util.Constants
import com.entropyjournal.util.rememberHapticAction
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
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.entropyjournal.domain.model.Advice
import com.entropyjournal.domain.model.AdvicePriority
import com.entropyjournal.domain.model.TopAction
import com.entropyjournal.ui.components.AdviceCategoryCard
import com.entropyjournal.ui.components.GlassCard
import com.entropyjournal.ui.components.NeonDivider
import com.entropyjournal.ui.components.ParticleBackground
import com.entropyjournal.ui.components.ShimmerLoadingEffect
import com.entropyjournal.ui.components.TwinklingStars
import com.entropyjournal.ui.theme.CustomPalette
import com.entropyjournal.ui.theme.GoalPalette
import com.entropyjournal.ui.theme.InsightPalette
import com.entropyjournal.ui.theme.FeatureAccentOrange
import com.entropyjournal.ui.theme.SummaryPalette
import com.entropyjournal.util.TtsManager
import android.content.Intent
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.entropyjournal.ui.theme.LocalIsDarkTheme
import com.entropyjournal.ui.theme.NeonAmber
import com.entropyjournal.ui.theme.NeonCyan
import com.entropyjournal.ui.theme.NeonEmerald
import com.entropyjournal.ui.theme.NeonRed

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val blocks by viewModel.adviceBlocks.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = LocalIsDarkTheme.current
    val doHaptic = rememberHapticAction()
    var showLegendDialog by remember { mutableStateOf(false) }
    var selectedAdvice by remember { mutableStateOf<Pair<Advice, String>?>(null) }
    var selectedCategoryBlock by remember {
        mutableStateOf<com.entropyjournal.domain.model.AdviceBlock?>(null)
    }
    val context = LocalContext.current
    var isDashboardSpeaking by remember { mutableStateOf(false) }
    var isDashboardTtsLoading by remember { mutableStateOf(false) }
    val dashboardTts = remember { TtsManager(context) }
    // Frank-Wunsch 2026-05-15: TTS-Dependencies fuer Top-Aktionen-Detail-Dialoge.
    val topActionTtsCtx =
        DashboardTtsContext(tts = dashboardTts, doHaptic = doHaptic, context = context)

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        com.entropyjournal.ui.components.SunMoonToggle()
                    }
                    Row {
                        IconButton(onClick = { doHaptic(HapticFeedbackType.LongPress); showLegendDialog = true }) {
                            Icon(
                                Icons.Rounded.Info,
                                "Legende",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (uiState.canUndo) {
                            IconButton(onClick = { doHaptic(HapticFeedbackType.LongPress); viewModel.undoDashboard() }) {
                                Icon(Icons.Rounded.Undo, "R\u00fcckg\u00e4ngig", tint = NeonAmber)
                            }
                        }
                        IconButton(onClick = { doHaptic(HapticFeedbackType.LongPress); viewModel.refreshDashboard() }) {
                            Icon(
                                Icons.Rounded.Refresh,
                                "Aktualisieren",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                val lastUpdated = remember(uiState.isLoading) { viewModel.getLastUpdatedText() }
                if (lastUpdated != null) {
                    Text(
                        text = lastUpdated,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            }

            // Scrollable content
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // C1 — Profil-Header: zeigt das aktive Profil + Kurzfokus.
                // Frank-Wunsch 2026-05-08: scrollt mit dem restlichen Dashboard-Inhalt mit
                // (vorher war das Surface in der fixed Column oben — bleibt jetzt also nicht
                // mehr stehen wenn man nach unten scrollt).
                if (uiState.activeProfileLabel.isNotBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Aktives Profil: ${uiState.activeProfileLabel}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                                // Bug-Fix 2026-05-15: customFokusKern stammt aus dem letzten
                                // Custom-Profil-Refresh und ueberlebt in Prefs auch wenn der User
                                // auf ein eingebautes Profil wechselt. Folge: alle Profile zeigen
                                // denselben Fokus-Kern. Daher: customFokusKern NUR bei
                                // Custom-Profilen verwenden, sonst IMMER activeProfileFocus.
                                val isCustomProfile =
                                    uiState.currentScenario >=
                                        com.entropyjournal.util.Constants.FIRST_CUSTOM_SCENARIO_INDEX
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
                                        // Frank-Vorgabe 2026-05-15: max 3 Zeilen.
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
                    item {
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
                                        text = "Profil ohne Fokus-Text",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text =
                                        "Dieses Profil hat noch keinen Fokus-Text. " +
                                            "Öffne die Einstellungen, wähle dieses Profil " +
                                            "und ergänze einen Fokus-Prompt — dann erscheint " +
                                            "hier dein persönliches Dashboard.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                    }
                    return@LazyColumn
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
                                    "Bitte warten",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (uiState.isScenarioSwitch)
                                        "KI-Dashboard wird nach jedem Profilwechsel automatisch aktualisiert"
                                    else if (uiState.isDeleteUpdate)
                                        "KI-Dashboard wird nach jedem gelöschten Tagebucheintrag automatisch aktualisiert"
                                    else if (uiState.isAutoUpdate)
                                        "KI-Dashboard wird nach jedem neuen Tagebucheintrag automatisch aktualisiert"
                                    else "KI-Dashboard wird aktualisiert",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
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
                                if (uiState.errorMessage != null) {
                                    Text(
                                        "Analyse fehlgeschlagen",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = NeonRed,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        uiState.errorMessage!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                } else if (uiState.currentScenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX) {
                                    val customPrompt = viewModel.getCustomPrompt()
                                    if (customPrompt.isBlank()) {
                                        Text(
                                            "Kein Analyse-Fokus eingegeben",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Gib in den Einstellungen unter\n\u201eIndividuelle Analyse\u201c einen Fokus ein,\noder w\u00e4hle ein anderes Profil.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = TextAlign.Center,
                                        )
                                    } else {
                                        Text(
                                            "Noch keine Analyse",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Erstelle Tagebucheintr\u00e4ge,\ndann analysiert die KI deinen Fokus.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                } else {
                                    Text(
                                        "Noch keine Analyse",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Erstelle Tagebucheintr\u00e4ge,\ndann analysiert die KI deine Muster.",
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
                    if (uiState.currentScenario == 0) {
                        // ═══════ ZUSAMMENFASSUNG DASHBOARD ═══════
                        val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                        if (topActions.isNotEmpty()) {
                            item {
                                SummaryKeyInsightsBlock(
                                    actions = topActions,
                                    ttsCtx = topActionTtsCtx,
                                )
                            }
                        }

                        item {
                            val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                            GlassCard(glowColor = SummaryPalette.primary, glowIntensity = 0.2f) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "\u00dcberblick",
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
                                        isSpeaking = isDashboardSpeaking,
                                        isTtsLoading = isDashboardTtsLoading,
                                        onSpeakingChange = { isDashboardSpeaking = it },
                                        onTtsLoadingChange = { isDashboardTtsLoading = it },
                                        doHaptic = doHaptic,
                                        context = context,
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
                                    itemsIndexed(blocks) { index, block ->
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

                        item(key = "all_observations") {
                            Spacer(modifier = Modifier.height(20.dp))
                            NeonDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Alle Beobachtungen",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SummaryPalette.secondary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        item(key = "relevance_legend") { SummaryRelevanceLegend() }

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
                        itemsIndexed(allObservations) { _, (advice, catName, _) ->
                            SummaryObservationCard(
                                advice = advice,
                                categoryName = catName,
                                onClick = { selectedAdvice = Pair(advice, catName) },
                            )
                        }
                    } else if (uiState.currentScenario == 2) {
                        // ═══════ SELBSTERKENNTNIS DASHBOARD ═══════
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
                                        "Innerer Spiegel",
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
                                        isSpeaking = isDashboardSpeaking,
                                        isTtsLoading = isDashboardTtsLoading,
                                        onSpeakingChange = { isDashboardSpeaking = it },
                                        onTtsLoadingChange = { isDashboardTtsLoading = it },
                                        doHaptic = doHaptic,
                                        context = context,
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
                                    itemsIndexed(blocks) { index, block ->
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
                                "Alle Einsichten",
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
                        itemsIndexed(allInsights) { _, (advice, catName, _) ->
                            InsightCard(
                                advice = advice,
                                categoryName = catName,
                                onClick = { selectedAdvice = Pair(advice, catName) },
                            )
                        }
                    } else if (uiState.currentScenario == 3) {
                        // ═══════ PERSÖNLICHE ZIELE DASHBOARD ═══════
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
                                        "Ziel-\u00dcberblick",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = GoalPalette.primary,
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
                                        isSpeaking = isDashboardSpeaking,
                                        isTtsLoading = isDashboardTtsLoading,
                                        onSpeakingChange = { isDashboardSpeaking = it },
                                        onTtsLoadingChange = { isDashboardTtsLoading = it },
                                        doHaptic = doHaptic,
                                        context = context,
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
                                    itemsIndexed(blocks) { index, block ->
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
                                "Alle Ziele",
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
                        itemsIndexed(allGoals) { _, (advice, catName, _) ->
                            GoalCard(
                                advice = advice,
                                categoryName = catName,
                                onClick = { selectedAdvice = Pair(advice, catName) },
                            )
                        }
                    } else if (uiState.currentScenario == 4) {
                        // ═══════ INDIVIDUELLE ANALYSE DASHBOARD ═══════
                        val customTop5 =
                            uiState.customHeaderTop5.ifBlank { "Wichtigste Ergebnisse" }
                        val customAnalyse = uiState.customHeaderAnalyse.ifBlank { "Analyse" }
                        val customErgebnisse =
                            uiState.customHeaderErgebnisse.ifBlank { "Alle Ergebnisse" }

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
                                        isSpeaking = isDashboardSpeaking,
                                        isTtsLoading = isDashboardTtsLoading,
                                        onSpeakingChange = { isDashboardSpeaking = it },
                                        onTtsLoadingChange = { isDashboardTtsLoading = it },
                                        doHaptic = doHaptic,
                                        context = context,
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
                                    itemsIndexed(blocks) { index, block ->
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
                        itemsIndexed(allCustom) { _, (advice, catName, _) ->
                            CustomResultCard(
                                advice = advice,
                                categoryName = catName,
                                onClick = { selectedAdvice = Pair(advice, catName) },
                            )
                        }
                    } else {
                        // ═══════ DEFAULT DASHBOARD ═══════
                        val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                        if (topActions.isNotEmpty()) {
                            item {
                                TopActionsBlock(
                                    actions = topActions,
                                    ttsCtx = topActionTtsCtx,
                                )
                            }
                        }

                        item {
                            val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                            val entryCount = blocks.firstOrNull()?.basedOnEntryCount ?: 0
                            GlassCard(glowIntensity = 0.2f) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        "Gesamtanalyse",
                                        style =
                                            MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                textDecoration = TextDecoration.Underline,
                                            ),
                                        color = NeonAmber,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                    if (entryCount > 0) {
                                        Text(
                                            "Basierend auf $entryCount Eintr\u00e4gen",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = overallAnalysis,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    AnalysisTtsShareRow(
                                        text = overallAnalysis,
                                        tts = dashboardTts,
                                        isSpeaking = isDashboardSpeaking,
                                        isTtsLoading = isDashboardTtsLoading,
                                        onSpeakingChange = { isDashboardSpeaking = it },
                                        onTtsLoadingChange = { isDashboardTtsLoading = it },
                                        doHaptic = doHaptic,
                                        context = context,
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
                                    itemsIndexed(blocks) { index, block ->
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

                        item { PriorityLegend() }

                        item(key = "all_recommendations") {
                            Spacer(modifier = Modifier.height(20.dp))
                            NeonDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Alle Empfehlungen",
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
                        itemsIndexed(allAdvicesWithCategory) { _, (advice, catName, _) ->
                            AdviceCard(
                                advice = advice,
                                categoryName = catName,
                                onClick = { selectedAdvice = Pair(advice, catName) },
                            )
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
        LegendDot(color = NeonRed, label = "Dringend")
        LegendDot(color = NeonAmber, label = "Aufmerksamkeit")
        LegendDot(color = NeonCyan, label = "Beobachten")
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
        when (scenario) {
            0 -> "Zusammenfassung"
            2 -> "Selbsterkenntnis"
            3 -> "Pers\u00f6nliche Ziele"
            4 -> "Individuelle Analyse"
            else -> "R\u00e4ume dein Leben auf"
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
                        Text(
                            "Aktivit\u00e4tslevel",
                            style = MaterialTheme.typography.titleMedium,
                            color = SummaryPalette.primary,
                        )
                        Text(
                            "Der Halbkreis zeigt wie viel in diesem Bereich passiert:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(color = NeonRed, label = "Viel Aktivit\u00e4t (67\u2013100%)")
                            LegendDot(
                                color = NeonAmber,
                                label = "Moderate Aktivit\u00e4t (34\u201366%)",
                            )
                            LegendDot(
                                color = NeonEmerald,
                                label = "Wenig Aktivit\u00e4t (0\u201333%)",
                            )
                        }
                        NeonDivider()
                        Text(
                            "Relevanz der Beobachtungen",
                            style = MaterialTheme.typography.titleMedium,
                            color = SummaryPalette.primary,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(SummaryPalette.secondary.copy(alpha = 0.12f)),
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
                                    "Zentral \u2014 Kern-Themen deiner Eintr\u00e4ge",
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
                                    "Relevant \u2014 Wiederkehrende Beobachtungen",
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
                                    "Randnotiz \u2014 Einmalige Erw\u00e4hnungen",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    2 -> {
                        Text(
                            "Reflexionstiefe",
                            style = MaterialTheme.typography.titleMedium,
                            color = InsightPalette.primary,
                        )
                        Text(
                            "Der Halbkreis zeigt die Tiefe deiner Selbstreflexion:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(color = InsightPalette.primary, label = "Tiefgehend (67\u2013100%)")
                            LegendDot(color = InsightPalette.muted, label = "Bewusst (34\u201366%)")
                            LegendDot(color = InsightPalette.accent, label = "Oberfl\u00e4che (0\u201333%)")
                        }
                        NeonDivider()
                        Text(
                            "Tiefe der Einsichten",
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
                                    "Tiefgehend \u2014 Verborgene Muster und \u00dcberzeugungen",
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
                                    "Bewusst \u2014 Erkannte Denk- und Gef\u00fchlsmuster",
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
                                    "Oberfl\u00e4che \u2014 Erste Beobachtungen",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    3 -> {
                        Text(
                            "Fortschritt",
                            style = MaterialTheme.typography.titleMedium,
                            color = GoalPalette.primary,
                        )
                        Text(
                            "Der Halbkreis zeigt den Fortschritt deiner Ziele:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(color = GoalPalette.muted, label = "Blockiert (0\u201333%)")
                            LegendDot(color = GoalPalette.accent, label = "In Arbeit (34\u201366%)")
                            LegendDot(color = GoalPalette.primary, label = "Fortschritt (67\u2013100%)")
                        }
                        NeonDivider()
                        Text(
                            "Zielstatus",
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
                                    "Blockiert \u2014 Ziele mit Hindernissen",
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
                                    "Offen \u2014 Ziele noch ohne Fortschritt",
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
                                    "Fortschritt \u2014 Ziele mit sichtbarem Erfolg",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    4 -> {
                        Text(
                            "Analysewert",
                            style = MaterialTheme.typography.titleMedium,
                            color = CustomPalette.primary,
                        )
                        Text(
                            "Der Halbkreis zeigt die Relevanz f\u00fcr deinen Fokus:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(
                                color = CustomPalette.primary,
                                label = "Hoch (67\u2013100%) \u2014 Kern deines Fokus",
                            )
                            LegendDot(
                                color = CustomPalette.secondary,
                                label = "Mittel (34\u201366%) \u2014 Verbindung erkannt",
                            )
                            LegendDot(
                                color = CustomPalette.accent,
                                label = "Niedrig (0\u201333%) \u2014 Am Rand",
                            )
                        }
                        NeonDivider()
                        Text(
                            "Relevanz der Ergebnisse",
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
                                    "Wichtig \u2014 Direkt relevant f\u00fcr deinen Fokus",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(CustomPalette.secondary.copy(alpha = 0.12f)),
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
                                    "Relevant \u2014 Verbindung zu deinem Fokus",
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
                                    "Notiz \u2014 Randbemerkung",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    else -> {
                        Text(
                            "Entropie-Skala",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Der Halbkreis unter jeder Kategorie zeigt die Belastungsintensit\u00e4t:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(
                                color = NeonRed,
                                label = "Hoch (67\u2013100%) \u2014 Sofort handeln",
                            )
                            LegendDot(
                                color = NeonAmber,
                                label = "Mittel (34\u201366%) \u2014 Aufmerksamkeit n\u00f6tig",
                            )
                            LegendDot(
                                color = NeonEmerald,
                                label = "Niedrig (0\u201333%) \u2014 Guter Zustand",
                            )
                        }
                        NeonDivider()
                        Text(
                            "Priorit\u00e4t der Ratschl\u00e4ge",
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
                                    "Rot = Dringend \u2014 Sofort handeln",
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
                                    "Orange = Mittel \u2014 Bald angehen",
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
                                    "Blau = Niedrig \u2014 Beobachten",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
                NeonDivider()
                Text(
                    "Kategorien",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Kategorien werden dynamisch erstellt \u2014 die KI erkennt Themen in deinen Eintr\u00e4gen und erstellt passende Kategorien. Neue Themen f\u00fchren automatisch zu neuen Symbolen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Verstanden", color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

@Composable
private fun CategoryDetailDialog(
    block: com.entropyjournal.domain.model.AdviceBlock,
    onDismiss: () -> Unit,
) {
    val levelLabel =
        when {
            block.entropyLevel < 0.33f -> "Niedrig"
            block.entropyLevel < 0.66f -> "Mittel"
            else -> "Hoch"
        }
    val levelColor =
        when {
            block.entropyLevel < 0.33f -> NeonEmerald
            block.entropyLevel < 0.66f -> NeonAmber
            else -> NeonRed
        }
    val urgency =
        when {
            block.entropyLevel >= 0.67f -> "Dringend \u2014 Sofort handeln"
            block.entropyLevel >= 0.34f -> "Aufmerksamkeit n\u00f6tig"
            else -> "Guter Zustand \u2014 Beobachten"
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
                    Icon(
                        imageVector =
                            com.entropyjournal.ui.components.getIconForCategory(block.categoryIcon),
                        contentDescription = null,
                        tint = levelColor,
                        modifier = Modifier.size(28.dp),
                    )
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
                    "Alle Empfehlungen",
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline,
                        ),
                    color = NeonAmber,
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
                            AdvicePriority.LOW -> Icons.Rounded.Eco
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
                Text("Schlie\u00dfen", color = MaterialTheme.colorScheme.primary)
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
                        "Hergeleitet aus:",
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
                        "Aktualisiere das Dashboard f\u00fcr eine detaillierte Herleitung.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                if (advice.connection.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "\u2197 Verbindung: ${advice.connection}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schlie\u00dfen", color = MaterialTheme.colorScheme.primary)
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
            AdvicePriority.LOW -> Icons.Rounded.Eco
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

/**
 * Frank-Wunsch 2026-05-15: TTS-Dependencies fuer Top-Aktionen-Dialoge buendeln.
 */
class DashboardTtsContext(
    val tts: TtsManager,
    val doHaptic: (HapticFeedbackType) -> Unit,
    val context: android.content.Context,
)

/**
 * Frank-Wunsch 2026-05-15: gleicher oranger Lautsprecher wie in Tagebucheintraegen
 * und Rueckblicken, jetzt auch in den Top-Aktionen-Detail-Dialogen. Vorgelesen wird
 * UEBERSCHRIFT (ohne Zahl davor) + Beschreibung + ggf. ausfuehrliche Beschreibung.
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
                isLoading = true
                isSpeaking = true
                val started =
                    ttsCtx.tts.speak(
                        ttsText,
                        onPlaybackStart = { isLoading = false },
                    ) {
                        isSpeaking = false
                        isLoading = false
                    }
                if (!started) {
                    isSpeaking = false
                    isLoading = false
                    android.widget.Toast.makeText(
                            ttsCtx.context,
                            "Stimmen in den Einstellungen einschalten",
                            android.widget.Toast.LENGTH_SHORT,
                        )
                        .show()
                }
            }
        },
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
            contentDescription = if (isSpeaking) "Stoppen" else "Vorlesen",
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
                "Top ${actions.size} Massnahmen",
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
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(dotColor),
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
                    Text("Schlie\u00dfen", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
    )
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
                "Kernerkenntnisse",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SummaryPalette.secondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Die wichtigsten Punkte aus deinen Eintr\u00e4gen",
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
                    Box(
                        modifier =
                            Modifier.size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(listOf(SummaryPalette.primary, SummaryPalette.secondary))
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
                            .background(Brush.linearGradient(listOf(SummaryPalette.primary, SummaryPalette.secondary))),
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
                    Text("Schlie\u00dfen", color = SummaryPalette.primary)
                }
            }
        },
    )
}

@Composable
private fun SummaryRelevanceLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        SummaryLegendItem(icon = Icons.Rounded.Star, label = "Zentral", color = SummaryPalette.secondary)
        SummaryLegendItem(icon = Icons.Rounded.PushPin, label = "Relevant", color = SummaryPalette.primary)
        SummaryLegendItem(
            icon = Icons.Rounded.BookmarkBorder,
            label = "Randnotiz",
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
                "Tiefste Erkenntnisse",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = InsightPalette.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Was deine Eintr\u00e4ge \u00fcber dich verraten",
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
                                    Brush.linearGradient(listOf(InsightPalette.primary, InsightPalette.secondary))
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
                            .background(Brush.linearGradient(listOf(InsightPalette.primary, InsightPalette.secondary))),
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
                    Text("Schlie\u00dfen", color = InsightPalette.primary)
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
            label = "Tiefgehend",
            color = InsightPalette.primary,
        )
        InsightLegendItem(
            icon = Icons.Rounded.FavoriteBorder,
            label = "Bewusst",
            color = InsightPalette.muted,
        )
        InsightLegendItem(icon = Icons.Rounded.Eco, label = "Oberfläche", color = InsightPalette.accent)
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
                "N\u00e4chste Schritte",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GoalPalette.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Die wichtigsten Schritte f\u00fcr deine Ziele",
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
                                .background(Brush.linearGradient(listOf(GoalPalette.primary, GoalPalette.secondary))),
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
                            .background(Brush.linearGradient(listOf(GoalPalette.primary, GoalPalette.secondary))),
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
                    Text("Schlie\u00dfen", color = GoalPalette.primary)
                }
            }
        },
    )
}

@Composable
private fun GoalStatusLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        GoalLegendItem(icon = Icons.Rounded.Block, label = "Blockiert", color = GoalPalette.muted)
        GoalLegendItem(icon = Icons.Rounded.LockOpen, label = "Offen", color = GoalPalette.accent)
        GoalLegendItem(icon = Icons.Rounded.CheckCircle, label = "Fortschritt", color = GoalPalette.primary)
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
    title: String = "Wichtigste Ergebnisse",
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
                "Basierend auf deiner individuellen Analyse",
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
                                .background(Brush.linearGradient(listOf(CustomPalette.primary, CustomPalette.secondary))),
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
                            .background(Brush.linearGradient(listOf(CustomPalette.primary, CustomPalette.secondary))),
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
                    Text("Schlie\u00dfen", color = CustomPalette.primary)
                }
            }
        },
    )
}

@Composable
private fun CustomRelevanceLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        CustomLegendItem(icon = Icons.Rounded.Whatshot, label = "Wichtig", color = CustomPalette.primary)
        CustomLegendItem(
            icon = Icons.Rounded.TipsAndUpdates,
            label = "Relevant",
            color = CustomPalette.secondary,
        )
        CustomLegendItem(icon = Icons.Rounded.EditNote, label = "Notiz", color = CustomPalette.accent)
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
    tts: TtsManager,
    isSpeaking: Boolean,
    isTtsLoading: Boolean,
    onSpeakingChange: (Boolean) -> Unit,
    onTtsLoadingChange: (Boolean) -> Unit,
    doHaptic: (HapticFeedbackType) -> Unit,
    context: android.content.Context,
) {
    Spacer(modifier = Modifier.height(12.dp))
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
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
                    onTtsLoadingChange(true)
                    onSpeakingChange(true)
                    val started = tts.speak(
                        text,
                        onPlaybackStart = { onTtsLoadingChange(false) },
                    ) {
                        onSpeakingChange(false)
                        onTtsLoadingChange(false)
                    }
                    if (!started) {
                        onSpeakingChange(false)
                        onTtsLoadingChange(false)
                        android.widget.Toast.makeText(
                            context,
                            "Stimmen in den Einstellungen einschalten",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                contentDescription = if (isSpeaking) "Stoppen" else "Vorlesen",
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
                context.startActivity(Intent.createChooser(shareIntent, "Analyse teilen"))
            },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.Rounded.Share,
                contentDescription = "Teilen",
                tint = FeatureAccentOrange,
                modifier = Modifier.size(24.dp),
            )
        }
    }
    if (isTtsLoading) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = if (LocalIsDarkTheme.current) Color(0xFF5C7AA3) else Color(0xFF1976D2),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Bitte warten, Text-to-Speech wird erzeugt…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


package com.bestjournal.app.ui.screens.dashboard

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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.bestjournal.app.domain.model.Advice
import com.bestjournal.app.domain.model.AdvicePriority
import com.bestjournal.app.ui.components.AdviceCategoryCard
import com.bestjournal.app.ui.components.AiInfoBanner
import com.bestjournal.app.ui.components.GlassCard
import com.bestjournal.app.ui.components.NeonDivider
import com.bestjournal.app.ui.components.ParticleBackground
import com.bestjournal.app.ui.components.TwinklingStars
import com.bestjournal.app.ui.components.PulsingOrb
import com.bestjournal.app.ui.components.ShimmerLoadingEffect
import com.bestjournal.app.ui.theme.LocalIsDarkTheme
import com.bestjournal.app.ui.theme.NeonAmber
import com.bestjournal.app.ui.theme.NeonCyan
import com.bestjournal.app.ui.theme.NeonEmerald
import com.bestjournal.app.ui.theme.NeonRed
import com.bestjournal.app.ui.theme.SummaryBlue
import com.bestjournal.app.ui.theme.SummaryIndigo
import com.bestjournal.app.ui.theme.SummaryTeal
import com.bestjournal.app.ui.theme.SummarySlate
import com.bestjournal.app.ui.theme.InsightViolet
import com.bestjournal.app.ui.theme.InsightRose
import com.bestjournal.app.ui.theme.InsightWarm
import com.bestjournal.app.ui.theme.InsightMauve
import com.bestjournal.app.ui.theme.GoalEmerald
import com.bestjournal.app.ui.theme.GoalGold
import com.bestjournal.app.ui.theme.GoalSky
import com.bestjournal.app.ui.theme.GoalCoral
import com.bestjournal.app.ui.theme.CustomAmber
import com.bestjournal.app.ui.theme.CustomSand
import com.bestjournal.app.ui.theme.CustomSage
import com.bestjournal.app.ui.theme.CustomStone
import com.bestjournal.app.domain.model.TopAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.Brush

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToPaywall: (String) -> Unit = {},
) {
    val blocks by viewModel.adviceBlocks.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isDark = LocalIsDarkTheme.current
    var showLegendDialog by remember { mutableStateOf(false) }
    var selectedAdvice by remember { mutableStateOf<Pair<Advice, String>?>(null) }
    var selectedCategoryBlock by remember {
        mutableStateOf<com.bestjournal.app.domain.model.AdviceBlock?>(null)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isDark) {
            ParticleBackground()
            TwinklingStars()
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title bar
            item(key = "title_bar") {
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
                            "Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        com.bestjournal.app.ui.components.SunMoonToggle()
                    }
                    Row {
                        IconButton(onClick = { showLegendDialog = true }) {
                            Icon(
                                Icons.Rounded.Info,
                                "Legende",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (uiState.canUndo) {
                            IconButton(onClick = { viewModel.undoDashboard() }) {
                                Icon(Icons.Rounded.Undo, "R\u00fcckg\u00e4ngig", tint = NeonAmber)
                            }
                        }
                        IconButton(onClick = { viewModel.refreshDashboard() }) {
                            Icon(
                                Icons.Rounded.Refresh,
                                "Aktualisieren",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                val lastUpdated =
                    remember(uiState.isLoading) { viewModel.getLastUpdatedText() }
                if (lastUpdated != null) {
                    Text(
                        text = lastUpdated,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (uiState.showAiInfoBanner) {
                item(key = "ai_banner") { AiInfoBanner(onDismiss = { viewModel.dismissAiInfoBanner() }) }
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
                                if (uiState.isScenarioSwitch) "KI-Dashboard wird nach jedem Profilwechsel automatisch aktualisiert"
                                else if (uiState.isAutoUpdate) "KI-Dashboard wird nach jedem neuen Tagebucheintrag automatisch aktualisiert"
                                else "KI-Dashboard wird aktualisiert",
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
                            Text(text = "\u23F3", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.dashboardLimitMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.dismissLimitMessage() }) {
                                Text("Verstanden", color = MaterialTheme.colorScheme.primary)
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
                            Text(text = "\u2615", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text =
                                    "Gemini ist gerade nicht erreichbar \u2014 bitte versuch es gleich nochmal.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.clearError()
                                    viewModel.refreshDashboard()
                                },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                            ) {
                                Text("Nochmal versuchen")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text(
                                    "Sp\u00e4ter",
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
                            if (uiState.currentScenario == 4) {
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
                    // Cool blue/indigo theme — informative, not urgent

                    // Key Insights block (replaces Top 5 Maßnahmen)
                    val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                    if (topActions.isNotEmpty()) {
                        item {
                            SummaryKeyInsightsBlock(actions = topActions)
                        }
                    }

                    // Overview (replaces Gesamtanalyse)
                    item {
                        val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                        GlassCard(glowColor = SummaryBlue, glowIntensity = 0.2f) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "\uD83D\uDCD6  \u00dcberblick",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = SummaryBlue,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = overallAnalysis,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                    // All observations header + relevance legend below it
                    item(key = "all_observations") {
                        Spacer(modifier = Modifier.height(20.dp))
                        NeonDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "\uD83D\uDD0D  Alle Beobachtungen",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = SummaryIndigo,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }

                    item(key = "relevance_legend") { SummaryRelevanceLegend() }

                    // All observations sorted by relevance
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
                    // Warm violet/rose theme — introspective, personal

                    val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                    if (topActions.isNotEmpty()) {
                        item {
                            InsightKeyBlock(actions = topActions)
                        }
                    }

                    item {
                        val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                        GlassCard(glowColor = InsightViolet, glowIntensity = 0.2f) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "\uD83D\uDC9C  Innerer Spiegel",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = InsightViolet,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = overallAnalysis,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    item {
                        val categoryScrollIsolation = remember {
                            object : NestedScrollConnection {
                                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = Offset(available.x, 0f)
                                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = Velocity(available.x, 0f)
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
                                        onClick = { viewModel.selectCategory(index); selectedCategoryBlock = block },
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
                            "\uD83D\uDD2E  Alle Einsichten",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = InsightMauve,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }

                    item(key = "insight_depth_legend") { InsightDepthLegend() }

                    val allInsights = blocks
                        .flatMap { block -> block.advices.map { advice -> Triple(advice, block.categoryName, block.entropyLevel) } }
                        .sortedBy { (advice, _, _) -> when (advice.priority) { AdvicePriority.HIGH -> 0; AdvicePriority.MEDIUM -> 1; AdvicePriority.LOW -> 2 } }

                    itemsIndexed(allInsights) { _, (advice, catName, _) ->
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
                        item { GoalNextStepsBlock(actions = topActions) }
                    }

                    item {
                        val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                        GlassCard(glowColor = GoalEmerald, glowIntensity = 0.2f) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "\uD83D\uDDFA\uFE0F  Ziel-\u00dcberblick",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = GoalEmerald,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(overallAnalysis, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    item {
                        val categoryScrollIsolation = remember {
                            object : NestedScrollConnection {
                                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = Offset(available.x, 0f)
                                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = Velocity(available.x, 0f)
                            }
                        }
                        Box(modifier = Modifier.nestedScroll(categoryScrollIsolation)) {
                            LazyRow(contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                itemsIndexed(blocks) { index, block ->
                                    AdviceCategoryCard(block = block, isSelected = index == uiState.selectedCategoryIndex, onClick = { viewModel.selectCategory(index); selectedCategoryBlock = block })
                                }
                            }
                        }
                    }

                    item(key = "all_goals") {
                        Spacer(modifier = Modifier.height(20.dp))
                        NeonDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "\uD83C\uDFAF  Alle Ziele",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = GoalEmerald,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }

                    item(key = "goal_status_legend") { GoalStatusLegend() }

                    val allGoals = blocks
                        .flatMap { block -> block.advices.map { advice -> Triple(advice, block.categoryName, block.entropyLevel) } }
                        .sortedBy { (advice, _, _) -> when (advice.priority) { AdvicePriority.HIGH -> 0; AdvicePriority.MEDIUM -> 1; AdvicePriority.LOW -> 2 } }

                    itemsIndexed(allGoals) { _, (advice, catName, _) ->
                        GoalCard(advice = advice, categoryName = catName, onClick = { selectedAdvice = Pair(advice, catName) })
                    }
                } else if (uiState.currentScenario == 4) {
                    // ═══════ INDIVIDUELLE ANALYSE DASHBOARD ═══════
                    val customTop5 = uiState.customHeaderTop5.ifBlank { "Wichtigste Ergebnisse" }
                    val customAnalyse = uiState.customHeaderAnalyse.ifBlank { "Analyse" }
                    val customErgebnisse = uiState.customHeaderErgebnisse.ifBlank { "Alle Ergebnisse" }

                    val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                    if (topActions.isNotEmpty()) {
                        item { CustomInsightsBlock(actions = topActions, title = customTop5) }
                    }

                    item {
                        val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                        GlassCard(glowColor = CustomAmber, glowIntensity = 0.2f) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "\uD83D\uDD2C  $customAnalyse",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = CustomAmber,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(overallAnalysis, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    item {
                        val categoryScrollIsolation = remember {
                            object : NestedScrollConnection {
                                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = Offset(available.x, 0f)
                                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = Velocity(available.x, 0f)
                            }
                        }
                        Box(modifier = Modifier.nestedScroll(categoryScrollIsolation)) {
                            LazyRow(contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                itemsIndexed(blocks) { index, block ->
                                    AdviceCategoryCard(block = block, isSelected = index == uiState.selectedCategoryIndex, onClick = { viewModel.selectCategory(index); selectedCategoryBlock = block })
                                }
                            }
                        }
                    }

                    item(key = "all_custom") {
                        Spacer(modifier = Modifier.height(20.dp))
                        NeonDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "\uD83D\uDCCB  $customErgebnisse",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CustomAmber,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }

                    item(key = "custom_legend") { CustomRelevanceLegend() }

                    val allCustom = blocks
                        .flatMap { block -> block.advices.map { advice -> Triple(advice, block.categoryName, block.entropyLevel) } }
                        .sortedBy { (advice, _, _) -> when (advice.priority) { AdvicePriority.HIGH -> 0; AdvicePriority.MEDIUM -> 1; AdvicePriority.LOW -> 2 } }

                    itemsIndexed(allCustom) { _, (advice, catName, _) ->
                        CustomResultCard(advice = advice, categoryName = catName, onClick = { selectedAdvice = Pair(advice, catName) })
                    }
                } else {
                    // ═══════ DEFAULT DASHBOARD (Entropy) ═══════

                    // Top-5 Entropy-Reducing Actions — visually prominent block
                    val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                    if (topActions.isNotEmpty()) {
                        item {
                            TopActionsBlock(actions = topActions)
                        }
                    }

                    // Contextual upsell banner — shown once after first analysis for free users
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
                                    // Star icon in golden circle
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
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
                                            text = "Gef\u00e4llt dir die Analyse?",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = NeonAmber,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Mit Premium bekommst du unbegrenzte Analysen aus 5 verschiedenen Perspektiven.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.onAnalysisUpsellClicked()
                                                    onNavigateToPaywall("first_analysis")
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = NeonAmber,
                                                    contentColor = Color.Black,
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                            ) {
                                                Text(
                                                    "Mehr erfahren",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                )
                                            }
                                            TextButton(
                                                onClick = { viewModel.dismissAnalysisUpsellBanner() },
                                            ) {
                                                Text(
                                                    "Sp\u00e4ter",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Overall analysis
                    item {
                        val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""

                        GlassCard(glowIntensity = 0.2f) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Gesamtanalyse",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
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

                    // Priority legend
                    item(key = "priority_legend") { PriorityLegend() }

                    // ALL recommendations from ALL categories, sorted by priority
                    item(key = "all_recommendations") {
                        Spacer(modifier = Modifier.height(20.dp))
                        NeonDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Alle Empfehlungen",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
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
    val title = when (scenario) {
        0 -> "Legende \u2014 Zusammenfassung"
        2 -> "Legende \u2014 Selbsterkenntnis"
        3 -> "Legende \u2014 Pers\u00f6nliche Ziele"
        4 -> "Legende \u2014 Individuelle Analyse"
        else -> "Legende \u2014 R\u00e4ume dein Leben auf"
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
                        Text("Aktivit\u00e4tslevel", style = MaterialTheme.typography.titleMedium, color = SummaryBlue)
                        Text("Der Halbkreis zeigt wie viel in diesem Bereich passiert:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(color = NeonRed, label = "Viel Aktivit\u00e4t (67\u2013100%)")
                            LegendDot(color = NeonAmber, label = "Moderate Aktivit\u00e4t (34\u201366%)")
                            LegendDot(color = NeonEmerald, label = "Wenig Aktivit\u00e4t (0\u201333%)")
                        }
                        NeonDivider()
                        Text("Relevanz der Beobachtungen", style = MaterialTheme.typography.titleMedium, color = SummaryBlue)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("\u2B50 Zentral \u2014 Kern-Themen deiner Eintr\u00e4ge", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("\uD83D\uDCCC Relevant \u2014 Wiederkehrende Beobachtungen", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("\uD83D\uDCCE Randnotiz \u2014 Einmalige Erw\u00e4hnungen", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    2 -> {
                        // ── Selbsterkenntnis ──
                        Text("Reflexionstiefe", style = MaterialTheme.typography.titleMedium, color = InsightViolet)
                        Text("Der Halbkreis zeigt die Tiefe deiner Selbstreflexion:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(color = InsightViolet, label = "Tiefgehend (67\u2013100%)")
                            LegendDot(color = InsightMauve, label = "Bewusst (34\u201366%)")
                            LegendDot(color = InsightWarm, label = "Oberfl\u00e4che (0\u201333%)")
                        }
                        NeonDivider()
                        Text("Tiefe der Einsichten", style = MaterialTheme.typography.titleMedium, color = InsightViolet)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("\uD83E\uDE9E Tiefgehend \u2014 Verborgene Muster und \u00dcberzeugungen", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("\uD83D\uDC9C Bewusst \u2014 Erkannte Denk- und Gef\u00fchlsmuster", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("\uD83C\uDF31 Oberfl\u00e4che \u2014 Erste Beobachtungen", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    3 -> {
                        // ── Persönliche Ziele ──
                        Text("Fortschritt", style = MaterialTheme.typography.titleMedium, color = GoalEmerald)
                        Text("Der Halbkreis zeigt den Fortschritt deiner Ziele:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(color = GoalCoral, label = "Blockiert (0\u201333%)")
                            LegendDot(color = GoalGold, label = "In Arbeit (34\u201366%)")
                            LegendDot(color = GoalEmerald, label = "Fortschritt (67\u2013100%)")
                        }
                        NeonDivider()
                        Text("Zielstatus", style = MaterialTheme.typography.titleMedium, color = GoalEmerald)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("\uD83D\uDEA7 Blockiert \u2014 Ziele mit Hindernissen", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("\uD83D\uDD13 Offen \u2014 Ziele noch ohne Fortschritt", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("\u2705 Fortschritt \u2014 Ziele mit sichtbarem Erfolg", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    4 -> {
                        // ── Individuelle Analyse ──
                        Text("Analysewert", style = MaterialTheme.typography.titleMedium, color = CustomAmber)
                        Text("Der Halbkreis zeigt die Relevanz f\u00fcr deinen Fokus:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(color = CustomAmber, label = "Hoch (67\u2013100%) \u2014 Kern deines Fokus")
                            LegendDot(color = CustomSand, label = "Mittel (34\u201366%) \u2014 Verbindung erkannt")
                            LegendDot(color = CustomSage, label = "Niedrig (0\u201333%) \u2014 Am Rand")
                        }
                        NeonDivider()
                        Text("Relevanz der Ergebnisse", style = MaterialTheme.typography.titleMedium, color = CustomAmber)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("\uD83D\uDD25 Wichtig \u2014 Direkt relevant f\u00fcr deinen Fokus", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("\uD83D\uDCA1 Relevant \u2014 Verbindung zu deinem Fokus", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("\uD83D\uDCDD Notiz \u2014 Randbemerkung", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    else -> {
                        // ── Räume dein Leben auf (Belastung) ──
                        Text("Belastungs-Skala", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Der Halbkreis zeigt wie stark dich dieses Thema belastet:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LegendDot(color = NeonRed, label = "Hoch (67\u2013100%) \u2014 Sofort handeln")
                            LegendDot(color = NeonAmber, label = "Mittel (34\u201366%) \u2014 Aufmerksamkeit n\u00f6tig")
                            LegendDot(color = NeonEmerald, label = "Niedrig (0\u201333%) \u2014 Guter Zustand")
                        }
                        NeonDivider()
                        Text("Priorit\u00e4t der Ratschl\u00e4ge", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("\uD83D\uDD34 Rot = Dringend \u2014 Sofort handeln", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("\uD83D\uDFE0 Orange = Mittel \u2014 Bald angehen", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("\uD83D\uDD35 Blau = Niedrig \u2014 Beobachten", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                NeonDivider()
                Text("Kategorien", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
    block: com.bestjournal.app.domain.model.AdviceBlock,
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
                            com.bestjournal.app.ui.components.getIconForCategory(
                                block.categoryIcon
                            ),
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
                    style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.Underline),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                sortedAdvices.forEach { advice ->
                    val dot =
                        when (advice.priority) {
                            AdvicePriority.HIGH -> "\uD83D\uDD34"
                            AdvicePriority.MEDIUM -> "\uD83D\uDFE0"
                            AdvicePriority.LOW -> "\uD83D\uDD35"
                        }
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            "$dot ${advice.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
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
private fun TopActionsBlock(actions: List<TopAction>) {
    var selectedAction by remember { mutableStateOf<Pair<Int, TopAction>?>(null) }

    GlassCard(
        glowColor = NeonAmber,
        glowIntensity = 0.3f,
    ) {
        Column {
            Text(
                "Top 5 Massnahmen",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
                color = NeonAmber,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            actions.forEachIndexed { index, action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedAction = index to action }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = action.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
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
        )
    }
}

@Composable
private fun TopActionDetailDialog(action: TopAction, index: Int, onDismiss: () -> Unit) {
    val dotColor = when (index) {
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
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
            TextButton(onClick = onDismiss) {
                Text("Schlie\u00dfen", color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

@Composable
private fun AdviceCard(advice: Advice, categoryName: String = "", onClick: () -> Unit = {}) {
    GlassCard(
        modifier = Modifier.clickable { onClick() },
        glowColor =
            when (advice.priority) {
                AdvicePriority.HIGH -> NeonRed
                AdvicePriority.MEDIUM -> NeonAmber
                AdvicePriority.LOW -> NeonCyan
            },
        glowIntensity = 0.1f,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text =
                        when (advice.priority) {
                            AdvicePriority.HIGH -> "\uD83D\uDD34"
                            AdvicePriority.MEDIUM -> "\uD83D\uDFE0"
                            AdvicePriority.LOW -> "\uD83D\uDD35"
                        }
                )
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
private fun SummaryKeyInsightsBlock(actions: List<TopAction>) {
    var selectedAction by remember { mutableStateOf<Pair<Int, TopAction>?>(null) }

    GlassCard(
        glowColor = SummaryIndigo,
        glowIntensity = 0.25f,
    ) {
        Column {
            Text(
                "\uD83D\uDCA1  Kernerkenntnisse",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SummaryIndigo,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Die wichtigsten Punkte aus deinen Eintr\u00e4gen",
                style = MaterialTheme.typography.labelMedium,
                color = SummarySlate,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            actions.forEachIndexed { index, action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedAction = index to action }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Blue gradient numbered badge
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        SummaryBlue,
                                        SummaryIndigo,
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = action.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = action.description,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < actions.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(1.dp)
                            .background(SummaryBlue.copy(alpha = 0.15f))
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
        )
    }
}

@Composable
private fun SummaryInsightDetailDialog(action: TopAction, index: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(SummaryBlue, SummaryIndigo)
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                    color = SummaryBlue,
                )
                if (action.detailedDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(SummaryBlue.copy(alpha = 0.2f))
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
            TextButton(onClick = onDismiss) {
                Text("Schlie\u00dfen", color = SummaryBlue)
            }
        },
    )
}

@Composable
private fun SummaryRelevanceLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        SummaryLegendItem(emoji = "\u2B50", label = "Zentral", color = SummaryIndigo)
        SummaryLegendItem(emoji = "\uD83D\uDCCC", label = "Relevant", color = SummaryBlue)
        SummaryLegendItem(emoji = "\uD83D\uDCCE", label = "Randnotiz", color = SummaryTeal)
    }
}

@Composable
private fun SummaryLegendItem(emoji: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun SummaryObservationCard(advice: Advice, categoryName: String = "", onClick: () -> Unit = {}) {
    val (emoji, glowColor) = when (advice.priority) {
        AdvicePriority.HIGH -> "\u2B50" to SummaryIndigo
        AdvicePriority.MEDIUM -> "\uD83D\uDCCC" to SummaryBlue
        AdvicePriority.LOW -> "\uD83D\uDCCE" to SummaryTeal
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
                Text(text = emoji)
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
                        color = SummaryBlue.copy(alpha = 0.12f),
                    ) {
                        Text(
                            categoryName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = SummaryBlue,
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
                    color = SummaryTeal.copy(alpha = 0.8f),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SELBSTERKENNTNIS DASHBOARD — Warm violet/rose theme
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun InsightKeyBlock(actions: List<TopAction>) {
    var selectedAction by remember { mutableStateOf<Pair<Int, TopAction>?>(null) }

    GlassCard(glowColor = InsightViolet, glowIntensity = 0.25f) {
        Column {
            Text(
                "\uD83E\uDE9E  Tiefste Erkenntnisse",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = InsightViolet,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Was deine Eintr\u00e4ge \u00fcber dich verraten",
                style = MaterialTheme.typography.labelMedium,
                color = InsightMauve,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            actions.forEachIndexed { index, action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedAction = index to action }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(listOf(InsightViolet, InsightRose))
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = action.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = action.description,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < actions.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(1.dp)
                            .background(InsightViolet.copy(alpha = 0.15f))
                    )
                }
            }
        }
    }

    selectedAction?.let { (index, action) ->
        InsightDetailDialog(action = action, index = index, onDismiss = { selectedAction = null })
    }
}

@Composable
private fun InsightDetailDialog(action: TopAction, index: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(InsightViolet, InsightRose))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.surface)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(action.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column {
                Text(action.description, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), color = InsightViolet)
                if (action.detailedDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(InsightViolet.copy(alpha = 0.2f)))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(action.detailedDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schlie\u00dfen", color = InsightViolet) } },
    )
}

@Composable
private fun InsightDepthLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        InsightLegendItem(emoji = "\uD83E\uDE9E", label = "Tiefgehend", color = InsightViolet)
        InsightLegendItem(emoji = "\uD83D\uDC9C", label = "Bewusst", color = InsightMauve)
        InsightLegendItem(emoji = "\uD83C\uDF31", label = "Oberfl\u00e4che", color = InsightWarm)
    }
}

@Composable
private fun InsightLegendItem(emoji: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun InsightCard(advice: Advice, categoryName: String = "", onClick: () -> Unit = {}) {
    val (emoji, glowColor) = when (advice.priority) {
        AdvicePriority.HIGH -> "\uD83E\uDE9E" to InsightViolet
        AdvicePriority.MEDIUM -> "\uD83D\uDC9C" to InsightMauve
        AdvicePriority.LOW -> "\uD83C\uDF31" to InsightWarm
    }

    GlassCard(
        modifier = Modifier.clickable { onClick() },
        glowColor = glowColor,
        glowIntensity = 0.08f,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = emoji)
                Spacer(modifier = Modifier.width(8.dp))
                Text(advice.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                if (categoryName.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(6.dp), color = InsightViolet.copy(alpha = 0.12f)) {
                        Text(categoryName, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = InsightViolet)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(advice.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (advice.connection.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("\uD83D\uDD17 ${advice.connection}", style = MaterialTheme.typography.labelMedium, color = InsightRose.copy(alpha = 0.8f))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// PERSÖNLICHE ZIELE DASHBOARD — Green/gold theme
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun GoalNextStepsBlock(actions: List<TopAction>) {
    var selectedAction by remember { mutableStateOf<Pair<Int, TopAction>?>(null) }

    GlassCard(glowColor = GoalEmerald, glowIntensity = 0.25f) {
        Column {
            Text(
                "\uD83C\uDFAF  N\u00e4chste Schritte",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = GoalEmerald,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Die wichtigsten Schritte f\u00fcr deine Ziele",
                style = MaterialTheme.typography.labelMedium,
                color = GoalGold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            actions.forEachIndexed { index, action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedAction = index to action }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(GoalEmerald, GoalSky))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.surface)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(action.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(action.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (index < actions.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(GoalEmerald.copy(alpha = 0.15f)))
                }
            }
        }
    }

    selectedAction?.let { (index, action) ->
        GoalStepDetailDialog(action = action, index = index, onDismiss = { selectedAction = null })
    }
}

@Composable
private fun GoalStepDetailDialog(action: TopAction, index: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(GoalEmerald, GoalSky))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.surface)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(action.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column {
                Text(action.description, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = GoalEmerald)
                if (action.detailedDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GoalEmerald.copy(alpha = 0.2f)))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(action.detailedDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schlie\u00dfen", color = GoalEmerald) } },
    )
}

@Composable
private fun GoalStatusLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        GoalLegendItem(emoji = "\uD83D\uDEA7", label = "Blockiert", color = GoalCoral)
        GoalLegendItem(emoji = "\uD83D\uDD13", label = "Offen", color = GoalGold)
        GoalLegendItem(emoji = "\u2705", label = "Fortschritt", color = GoalEmerald)
    }
}

@Composable
private fun GoalLegendItem(emoji: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun GoalCard(advice: Advice, categoryName: String = "", onClick: () -> Unit = {}) {
    val (emoji, glowColor) = when (advice.priority) {
        AdvicePriority.HIGH -> "\uD83D\uDEA7" to GoalCoral
        AdvicePriority.MEDIUM -> "\uD83D\uDD13" to GoalGold
        AdvicePriority.LOW -> "\u2705" to GoalEmerald
    }

    GlassCard(modifier = Modifier.clickable { onClick() }, glowColor = glowColor, glowIntensity = 0.08f) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = emoji)
                Spacer(modifier = Modifier.width(8.dp))
                Text(advice.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                if (categoryName.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(6.dp), color = GoalEmerald.copy(alpha = 0.12f)) {
                        Text(categoryName, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = GoalEmerald)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(advice.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (advice.connection.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("\u27A1 ${advice.connection}", style = MaterialTheme.typography.labelMedium, color = GoalSky.copy(alpha = 0.8f))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// INDIVIDUELLE ANALYSE DASHBOARD — Warm amber/sand theme
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun CustomInsightsBlock(actions: List<TopAction>, title: String = "Wichtigste Ergebnisse") {
    var selectedAction by remember { mutableStateOf<Pair<Int, TopAction>?>(null) }

    GlassCard(glowColor = CustomAmber, glowIntensity = 0.25f) {
        Column {
            Text(
                "\uD83D\uDD0D  $title",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = CustomAmber,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Basierend auf deiner individuellen Analyse",
                style = MaterialTheme.typography.labelMedium,
                color = CustomStone,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            actions.forEachIndexed { index, action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedAction = index to action }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(CustomAmber, CustomSand))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.surface)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(action.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(action.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (index < actions.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(CustomAmber.copy(alpha = 0.15f)))
                }
            }
        }
    }

    selectedAction?.let { (index, action) ->
        CustomDetailDialog(action = action, index = index, onDismiss = { selectedAction = null })
    }
}

@Composable
private fun CustomDetailDialog(action: TopAction, index: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(CustomAmber, CustomSand))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.surface)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(action.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column {
                Text(action.description, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = CustomAmber)
                if (action.detailedDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CustomAmber.copy(alpha = 0.2f)))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(action.detailedDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schlie\u00dfen", color = CustomAmber) } },
    )
}

@Composable
private fun CustomRelevanceLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        CustomLegendItem(emoji = "\uD83D\uDD25", label = "Wichtig", color = CustomAmber)
        CustomLegendItem(emoji = "\uD83D\uDCA1", label = "Relevant", color = CustomSand)
        CustomLegendItem(emoji = "\uD83D\uDCDD", label = "Notiz", color = CustomSage)
    }
}

@Composable
private fun CustomLegendItem(emoji: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun CustomResultCard(advice: Advice, categoryName: String = "", onClick: () -> Unit = {}) {
    val (emoji, glowColor) = when (advice.priority) {
        AdvicePriority.HIGH -> "\uD83D\uDD25" to CustomAmber
        AdvicePriority.MEDIUM -> "\uD83D\uDCA1" to CustomSand
        AdvicePriority.LOW -> "\uD83D\uDCDD" to CustomSage
    }

    GlassCard(modifier = Modifier.clickable { onClick() }, glowColor = glowColor, glowIntensity = 0.08f) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = emoji)
                Spacer(modifier = Modifier.width(8.dp))
                Text(advice.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                if (categoryName.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(6.dp), color = CustomAmber.copy(alpha = 0.12f)) {
                        Text(categoryName, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = CustomAmber)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(advice.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (advice.connection.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("\u2194 ${advice.connection}", style = MaterialTheme.typography.labelMedium, color = CustomSage.copy(alpha = 0.8f))
            }
        }
    }
}

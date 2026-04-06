package com.entropyjournal.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
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
import com.entropyjournal.ui.components.PulsingOrb
import com.entropyjournal.ui.components.ShimmerLoadingEffect
import com.entropyjournal.ui.theme.LocalIsDarkTheme
import com.entropyjournal.ui.theme.NeonAmber
import com.entropyjournal.ui.theme.NeonCyan
import com.entropyjournal.ui.theme.NeonEmerald
import com.entropyjournal.ui.theme.NeonRed
import com.entropyjournal.ui.theme.SummaryBlue
import com.entropyjournal.ui.theme.SummaryIndigo
import com.entropyjournal.ui.theme.SummaryTeal
import com.entropyjournal.ui.theme.SummarySlate
import com.entropyjournal.ui.theme.InsightViolet
import com.entropyjournal.ui.theme.InsightRose
import com.entropyjournal.ui.theme.InsightWarm
import com.entropyjournal.ui.theme.InsightMauve
import com.entropyjournal.ui.theme.GoalEmerald
import com.entropyjournal.ui.theme.GoalGold
import com.entropyjournal.ui.theme.GoalSky
import com.entropyjournal.ui.theme.GoalCoral
import com.entropyjournal.ui.theme.CustomAmber
import com.entropyjournal.ui.theme.CustomSand
import com.entropyjournal.ui.theme.CustomSage
import com.entropyjournal.ui.theme.CustomStone
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.Brush

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val blocks by viewModel.adviceBlocks.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isDark = LocalIsDarkTheme.current
    var showLegendDialog by remember { mutableStateOf(false) }
    var selectedAdvice by remember { mutableStateOf<Pair<Advice, String>?>(null) }
    var selectedCategoryBlock by remember { mutableStateOf<com.entropyjournal.domain.model.AdviceBlock?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isDark) {
            ParticleBackground()
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title bar
            item(key = "title_bar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dashboard", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        com.entropyjournal.ui.components.SunMoonToggle()
                    }
                    Row {
                        IconButton(onClick = { showLegendDialog = true }) {
                            Icon(Icons.Rounded.Info, "Legende", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (uiState.canUndo) {
                            IconButton(onClick = { viewModel.undoDashboard() }) {
                                Icon(Icons.Rounded.Undo, "R\u00fcckg\u00e4ngig", tint = NeonAmber)
                            }
                        }
                        IconButton(onClick = { viewModel.refreshDashboard() }) {
                            Icon(Icons.Rounded.Refresh, "Aktualisieren", tint = MaterialTheme.colorScheme.primary)
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

            if (blocks.isEmpty() && !uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (uiState.errorMessage != null) {
                                Text("Analyse fehlgeschlagen", style = MaterialTheme.typography.titleLarge, color = NeonRed)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(uiState.errorMessage!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            } else if (uiState.currentScenario == 4) {
                                val customPrompt = viewModel.getCustomPrompt()
                                if (customPrompt.isBlank()) {
                                    Text("Kein Analyse-Fokus eingegeben", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Gib in den Einstellungen unter\n\u201eIndividuelle Analyse\u201c einen Fokus ein,\noder w\u00e4hle ein anderes Profil.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                                } else {
                                    Text("Noch keine Analyse", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Erstelle Tagebucheintr\u00e4ge,\ndann analysiert die KI deinen Fokus.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                                }
                            } else {
                                Text("Noch keine Analyse", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Erstelle Tagebucheintr\u00e4ge,\ndann analysiert die KI deine Muster.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
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
                        item { SummaryKeyInsightsBlock(actions = topActions) }
                    }

                    item {
                        val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                        GlassCard(glowColor = SummaryBlue, glowIntensity = 0.2f) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("\uD83D\uDCD6  \u00dcberblick", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = SummaryBlue, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = overallAnalysis, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                    item(key = "all_observations") {
                        Spacer(modifier = Modifier.height(20.dp))
                        NeonDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("\uD83D\uDD0D  Alle Beobachtungen", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = SummaryIndigo, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }

                    item(key = "relevance_legend") { SummaryRelevanceLegend() }

                    val allObservations = blocks.flatMap { block -> block.advices.map { advice -> Triple(advice, block.categoryName, block.entropyLevel) } }.sortedBy { (advice, _, _) -> when (advice.priority) { AdvicePriority.HIGH -> 0; AdvicePriority.MEDIUM -> 1; AdvicePriority.LOW -> 2 } }
                    itemsIndexed(allObservations) { _, (advice, catName, _) -> SummaryObservationCard(advice = advice, categoryName = catName, onClick = { selectedAdvice = Pair(advice, catName) }) }
                } else if (uiState.currentScenario == 2) {
                    // ═══════ SELBSTERKENNTNIS DASHBOARD ═══════
                    val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                    if (topActions.isNotEmpty()) {
                        item { InsightKeyBlock(actions = topActions) }
                    }

                    item {
                        val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                        GlassCard(glowColor = InsightViolet, glowIntensity = 0.2f) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("\uD83D\uDC9C  Innerer Spiegel", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = InsightViolet, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = overallAnalysis, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                    item(key = "all_insights") {
                        Spacer(modifier = Modifier.height(20.dp))
                        NeonDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("\uD83D\uDD2E  Alle Einsichten", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = InsightMauve, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }

                    item(key = "insight_depth_legend") { InsightDepthLegend() }

                    val allInsights = blocks.flatMap { block -> block.advices.map { advice -> Triple(advice, block.categoryName, block.entropyLevel) } }.sortedBy { (advice, _, _) -> when (advice.priority) { AdvicePriority.HIGH -> 0; AdvicePriority.MEDIUM -> 1; AdvicePriority.LOW -> 2 } }
                    itemsIndexed(allInsights) { _, (advice, catName, _) -> InsightCard(advice = advice, categoryName = catName, onClick = { selectedAdvice = Pair(advice, catName) }) }
                } else if (uiState.currentScenario == 3) {
                    // ═══════ PERSÖNLICHE ZIELE DASHBOARD ═══════
                    val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                    if (topActions.isNotEmpty()) {
                        item { GoalNextStepsBlock(actions = topActions) }
                    }

                    item {
                        val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                        GlassCard(glowColor = GoalEmerald, glowIntensity = 0.2f) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("\uD83D\uDDFA\uFE0F  Ziel-\u00dcberblick", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = GoalEmerald, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = overallAnalysis, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("\uD83C\uDFAF  Alle Ziele", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = GoalEmerald, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }

                    item(key = "goal_status_legend") { GoalStatusLegend() }

                    val allGoals = blocks.flatMap { block -> block.advices.map { advice -> Triple(advice, block.categoryName, block.entropyLevel) } }.sortedBy { (advice, _, _) -> when (advice.priority) { AdvicePriority.HIGH -> 0; AdvicePriority.MEDIUM -> 1; AdvicePriority.LOW -> 2 } }
                    itemsIndexed(allGoals) { _, (advice, catName, _) -> GoalCard(advice = advice, categoryName = catName, onClick = { selectedAdvice = Pair(advice, catName) }) }
                } else if (uiState.currentScenario == 4) {
                    // ═══════ INDIVIDUELLE ANALYSE DASHBOARD ═══════
                    val customTop5 = uiState.customHeaderTop5.ifBlank { "Wichtigste Ergebnisse" }
                    val customAnalyse = uiState.customHeaderAnalyse.ifBlank { "Analyse" }
                    val customErgebnisse = uiState.customHeaderErgebnisse.ifBlank { "Alle Ergebnisse" }

                    val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                    if (topActions.isNotEmpty()) { item { CustomInsightsBlock(actions = topActions, title = customTop5) } }

                    item {
                        val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                        GlassCard(glowColor = CustomAmber, glowIntensity = 0.2f) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("\uD83D\uDD2C  $customAnalyse", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = CustomAmber, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(overallAnalysis, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    item {
                        val categoryScrollIsolation = remember { object : NestedScrollConnection { override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = Offset(available.x, 0f); override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = Velocity(available.x, 0f) } }
                        Box(modifier = Modifier.nestedScroll(categoryScrollIsolation)) {
                            LazyRow(contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                itemsIndexed(blocks) { index, block -> AdviceCategoryCard(block = block, isSelected = index == uiState.selectedCategoryIndex, onClick = { viewModel.selectCategory(index); selectedCategoryBlock = block }) }
                            }
                        }
                    }

                    item(key = "all_custom") { Spacer(modifier = Modifier.height(20.dp)); NeonDivider(); Spacer(modifier = Modifier.height(16.dp)); Text("\uD83D\uDCCB  $customErgebnisse", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = CustomAmber, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    item(key = "custom_legend") { CustomRelevanceLegend() }

                    val allCustom = blocks.flatMap { block -> block.advices.map { advice -> Triple(advice, block.categoryName, block.entropyLevel) } }.sortedBy { (advice, _, _) -> when (advice.priority) { AdvicePriority.HIGH -> 0; AdvicePriority.MEDIUM -> 1; AdvicePriority.LOW -> 2 } }
                    itemsIndexed(allCustom) { _, (advice, catName, _) -> CustomResultCard(advice = advice, categoryName = catName, onClick = { selectedAdvice = Pair(advice, catName) }) }
                } else {
                    // ═══════ DEFAULT DASHBOARD ═══════
                    val topActions = blocks.firstOrNull()?.topActions ?: emptyList()
                    if (topActions.isNotEmpty()) {
                        item { TopActionsBlock(actions = topActions) }
                    }

                    item {
                        val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                        val entryCount = blocks.firstOrNull()?.basedOnEntryCount ?: 0
                        GlassCard(glowIntensity = 0.2f) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("Gesamtanalyse", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline), color = NeonAmber, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                if (entryCount > 0) { Text("Basierend auf $entryCount Eintr\u00e4gen", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline) }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = overallAnalysis, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                    item { PriorityLegend() }

                    item(key = "all_recommendations") {
                        Spacer(modifier = Modifier.height(20.dp))
                        NeonDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Alle Empfehlungen", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline), color = NeonAmber, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }

                    val allAdvicesWithCategory = blocks.flatMap { block -> block.advices.map { advice -> Triple(advice, block.categoryName, block.entropyLevel) } }.sortedBy { (advice, _, _) -> when (advice.priority) { AdvicePriority.HIGH -> 0; AdvicePriority.MEDIUM -> 1; AdvicePriority.LOW -> 2 } }
                    itemsIndexed(allAdvicesWithCategory) { _, (advice, catName, _) -> AdviceCard(advice = advice, categoryName = catName, onClick = { selectedAdvice = Pair(advice, catName) }) }
                }
            }
        }
    }

    if (showLegendDialog) { LegendDialog(onDismiss = { showLegendDialog = false }) }

    selectedAdvice?.let { (advice, catName) ->
        AdviceDerivationDialog(advice = advice, categoryName = catName, onDismiss = { selectedAdvice = null })
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
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Legende", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Entropie-Skala", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("Der Halbkreis unter jeder Kategorie zeigt die Belastungsintensit\u00e4t:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                NeonDivider()
                Text("Kategorien", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("Kategorien werden dynamisch erstellt \u2014 die KI erkennt Themen in deinen Eintr\u00e4gen und erstellt passende Kategorien. Neue Themen f\u00fchren automatisch zu neuen Symbolen.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Verstanden", color = MaterialTheme.colorScheme.primary) } }
    )
}

@Composable
private fun CategoryDetailDialog(block: com.entropyjournal.domain.model.AdviceBlock, onDismiss: () -> Unit) {
    val levelLabel = when {
        block.entropyLevel < 0.33f -> "Niedrig"
        block.entropyLevel < 0.66f -> "Mittel"
        else -> "Hoch"
    }
    val levelColor = when {
        block.entropyLevel < 0.33f -> NeonEmerald
        block.entropyLevel < 0.66f -> NeonAmber
        else -> NeonRed
    }
    val urgency = when {
        block.entropyLevel >= 0.67f -> "Dringend \u2014 Sofort handeln"
        block.entropyLevel >= 0.34f -> "Aufmerksamkeit n\u00f6tig"
        else -> "Guter Zustand \u2014 Beobachten"
    }

    val sortedAdvices = block.advices.sortedBy {
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
                        imageVector = com.entropyjournal.ui.components.getIconForCategory(block.categoryIcon),
                        contentDescription = null,
                        tint = levelColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(block.categoryName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = levelColor.copy(alpha = 0.15f)) {
                    Text(
                        "$levelLabel (${(block.entropyLevel * 100).toInt()}%) \u2014 $urgency",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = levelColor
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().height(400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(block.categorySummary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(8.dp))
                Text("Alle Empfehlungen", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline), color = NeonAmber, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                sortedAdvices.forEach { advice ->
                    val dot = when (advice.priority) {
                        AdvicePriority.HIGH -> "\uD83D\uDD34"
                        AdvicePriority.MEDIUM -> "\uD83D\uDFE0"
                        AdvicePriority.LOW -> "\uD83D\uDD35"
                    }
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("$dot ${advice.title}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(advice.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (advice.connection.isNotBlank()) {
                            Text("\u2197 ${advice.connection}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schlie\u00dfen", color = MaterialTheme.colorScheme.primary) } }
    )
}

@Composable
private fun AdviceDerivationDialog(advice: Advice, categoryName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(advice.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(categoryName, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(350.dp).verticalScroll(rememberScrollState())) {
                Text(advice.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (advice.derivation.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Hergeleitet aus:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    advice.derivation.forEach { entry ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                Box(modifier = Modifier.width(2.dp).height(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(entry.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(entry.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Aktualisiere das Dashboard f\u00fcr eine detaillierte Herleitung.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }

                if (advice.connection.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("\u2197 Verbindung: ${advice.connection}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schlie\u00dfen", color = MaterialTheme.colorScheme.primary) } }
    )
}

@Composable
private fun AdviceCard(advice: Advice, categoryName: String = "", onClick: () -> Unit = {}) {
    GlassCard(
        modifier = Modifier.clickable { onClick() },
        glowColor = when (advice.priority) {
            AdvicePriority.HIGH -> NeonRed
            AdvicePriority.MEDIUM -> NeonAmber
            AdvicePriority.LOW -> NeonCyan
        },
        glowIntensity = 0.1f
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when (advice.priority) {
                        AdvicePriority.HIGH -> "\uD83D\uDD34"
                        AdvicePriority.MEDIUM -> "\uD83D\uDFE0"
                        AdvicePriority.LOW -> "\uD83D\uDD35"
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = advice.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                if (categoryName.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(categoryName, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = advice.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (advice.connection.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "\u2197 ${advice.connection}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            }
        }
    }
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

// ═══════════════════════════════════════════════════════════════════════
// ZUSAMMENFASSUNG DASHBOARD — Cool blue/indigo theme
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun SummaryKeyInsightsBlock(actions: List<TopAction>) {
    var selectedAction by remember { mutableStateOf<Pair<Int, TopAction>?>(null) }

    GlassCard(glowColor = SummaryIndigo, glowIntensity = 0.25f) {
        Column {
            Text("\uD83D\uDCA1  Kernerkenntnisse", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = SummaryIndigo, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Die wichtigsten Punkte aus deinen Eintr\u00e4gen", style = MaterialTheme.typography.labelMedium, color = SummarySlate, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            actions.forEachIndexed { index, action ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { selectedAction = index to action }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(SummaryBlue, SummaryIndigo))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "${index + 1}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.surface)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = action.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = action.description, style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (index < actions.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(SummaryBlue.copy(alpha = 0.15f)))
                }
            }
        }
    }

    selectedAction?.let { (index, action) ->
        SummaryInsightDetailDialog(action = action, index = index, onDismiss = { selectedAction = null })
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
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(SummaryBlue, SummaryIndigo))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "${index + 1}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.surface)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = action.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column {
                Text(text = action.description, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), color = SummaryBlue)
                if (action.detailedDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SummaryBlue.copy(alpha = 0.2f)))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = action.detailedDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schlie\u00dfen", color = SummaryBlue) } },
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
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun SummaryObservationCard(advice: Advice, categoryName: String = "", onClick: () -> Unit = {}) {
    val (emoji, glowColor) = when (advice.priority) {
        AdvicePriority.HIGH -> "\u2B50" to SummaryIndigo
        AdvicePriority.MEDIUM -> "\uD83D\uDCCC" to SummaryBlue
        AdvicePriority.LOW -> "\uD83D\uDCCE" to SummaryTeal
    }

    GlassCard(modifier = Modifier.clickable { onClick() }, glowColor = glowColor, glowIntensity = 0.08f) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = emoji)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = advice.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                if (categoryName.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(6.dp), color = SummaryBlue.copy(alpha = 0.12f)) {
                        Text(categoryName, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = SummaryBlue)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = advice.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (advice.connection.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "\u2194 ${advice.connection}", style = MaterialTheme.typography.labelMedium, color = SummaryTeal.copy(alpha = 0.8f))
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
            Text("\uD83E\uDE9E  Tiefste Erkenntnisse", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = InsightViolet, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Was deine Eintr\u00e4ge \u00fcber dich verraten", style = MaterialTheme.typography.labelMedium, color = InsightMauve, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            actions.forEachIndexed { index, action ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { selectedAction = index to action }.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(InsightViolet, InsightRose))), contentAlignment = Alignment.Center) {
                        Text("${index + 1}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.surface)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(action.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(action.description, style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (index < actions.lastIndex) { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(InsightViolet.copy(alpha = 0.15f))) }
            }
        }
    }
    selectedAction?.let { (index, action) -> InsightDetailDialog(action = action, index = index, onDismiss = { selectedAction = null }) }
}

@Composable
private fun InsightDetailDialog(action: TopAction, index: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(InsightViolet, InsightRose))), contentAlignment = Alignment.Center) {
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

    GlassCard(modifier = Modifier.clickable { onClick() }, glowColor = glowColor, glowIntensity = 0.08f) {
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
            Text("\uD83C\uDFAF  N\u00e4chste Schritte", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = GoalEmerald, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Die wichtigsten Schritte f\u00fcr deine Ziele", style = MaterialTheme.typography.labelMedium, color = GoalGold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            actions.forEachIndexed { index, action ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { selectedAction = index to action }.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(GoalEmerald, GoalSky))), contentAlignment = Alignment.Center) {
                        Text("${index + 1}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.surface)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(action.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(action.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (index < actions.lastIndex) { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(GoalEmerald.copy(alpha = 0.15f))) }
            }
        }
    }
    selectedAction?.let { (index, action) -> GoalStepDetailDialog(action = action, index = index, onDismiss = { selectedAction = null }) }
}

@Composable
private fun GoalStepDetailDialog(action: TopAction, index: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(GoalEmerald, GoalSky))), contentAlignment = Alignment.Center) {
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
            Text("\uD83D\uDD0D  $title", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = CustomAmber, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Basierend auf deiner individuellen Analyse", style = MaterialTheme.typography.labelMedium, color = CustomStone, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            actions.forEachIndexed { index, action ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { selectedAction = index to action }.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(CustomAmber, CustomSand))), contentAlignment = Alignment.Center) {
                        Text("${index + 1}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.surface)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(action.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(action.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (index < actions.lastIndex) { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(CustomAmber.copy(alpha = 0.15f))) }
            }
        }
    }
    selectedAction?.let { (index, action) -> CustomDetailDialog(action = action, index = index, onDismiss = { selectedAction = null }) }
}

@Composable
private fun CustomDetailDialog(action: TopAction, index: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface,
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(CustomAmber, CustomSand))), contentAlignment = Alignment.Center) { Text("${index + 1}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.surface) }; Spacer(modifier = Modifier.width(12.dp)); Text(action.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface) } },
        text = { Column { Text(action.description, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = CustomAmber); if (action.detailedDescription.isNotBlank()) { Spacer(modifier = Modifier.height(12.dp)); Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CustomAmber.copy(alpha = 0.2f))); Spacer(modifier = Modifier.height(12.dp)); Text(action.detailedDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } },
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
    Row(verticalAlignment = Alignment.CenterVertically) { Text(emoji, style = MaterialTheme.typography.labelLarge); Spacer(modifier = Modifier.width(4.dp)); Text(label, style = MaterialTheme.typography.labelMedium, color = color) }
}

@Composable
private fun CustomResultCard(advice: Advice, categoryName: String = "", onClick: () -> Unit = {}) {
    val (emoji, glowColor) = when (advice.priority) { AdvicePriority.HIGH -> "\uD83D\uDD25" to CustomAmber; AdvicePriority.MEDIUM -> "\uD83D\uDCA1" to CustomSand; AdvicePriority.LOW -> "\uD83D\uDCDD" to CustomSage }
    GlassCard(modifier = Modifier.clickable { onClick() }, glowColor = glowColor, glowIntensity = 0.08f) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = emoji); Spacer(modifier = Modifier.width(8.dp))
                Text(advice.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                if (categoryName.isNotBlank()) { Surface(shape = RoundedCornerShape(6.dp), color = CustomAmber.copy(alpha = 0.12f)) { Text(categoryName, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = CustomAmber) } }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(advice.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (advice.connection.isNotBlank()) { Spacer(modifier = Modifier.height(6.dp)); Text("\u2194 ${advice.connection}", style = MaterialTheme.typography.labelMedium, color = CustomSage.copy(alpha = 0.8f)) }
        }
    }
}

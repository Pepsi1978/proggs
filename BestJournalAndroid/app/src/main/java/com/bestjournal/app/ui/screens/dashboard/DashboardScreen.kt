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
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.bestjournal.app.domain.model.Advice
import com.bestjournal.app.domain.model.AdvicePriority
import com.bestjournal.app.ui.components.AdviceCategoryCard
import com.bestjournal.app.ui.components.AiInfoBanner
import com.bestjournal.app.ui.components.GlassCard
import com.bestjournal.app.ui.components.NeonDivider
import com.bestjournal.app.ui.components.ParticleBackground
import com.bestjournal.app.ui.components.PulsingOrb
import com.bestjournal.app.ui.components.ShimmerLoadingEffect
import com.bestjournal.app.ui.theme.LocalIsDarkTheme
import com.bestjournal.app.ui.theme.NeonAmber
import com.bestjournal.app.ui.theme.NeonCyan
import com.bestjournal.app.ui.theme.NeonEmerald
import com.bestjournal.app.ui.theme.NeonRed

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
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
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title bar
            item {
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
            }

            if (uiState.showAiInfoBanner) {
                item { AiInfoBanner(onDismiss = { viewModel.dismissAiInfoBanner() }) }
            }

            if (uiState.isLoading) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerLoadingEffect(height = 60.dp, cornerRadius = 16.dp)
                        Text(
                            "Dashboard wird aktualisiert...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
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
                            Text(text = "⚠️", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Schließen", color = MaterialTheme.colorScheme.primary)
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

            if (blocks.isNotEmpty()) {
                // Overall analysis
                item {
                    val overallAnalysis = blocks.firstOrNull()?.overallAnalysis ?: ""
                    val avgEntropy = blocks.map { it.entropyLevel }.average().toFloat()
                    val entryCount = blocks.firstOrNull()?.basedOnEntryCount ?: 0

                    GlassCard(glowIntensity = 0.2f) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PulsingOrb(entropyLevel = avgEntropy, size = 48.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Gesamtanalyse",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (entryCount > 0) {
                                        Text(
                                            "Basierend auf $entryCount Eintr\u00e4gen",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }
                            }
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
                // Consumes leftover horizontal scroll/fling so the HorizontalPager
                // doesn't steal the gesture when swiping through categories
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
                item { PriorityLegend() }

                // ALL recommendations from ALL categories, sorted by priority
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    NeonDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Empfehlungen",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
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

    if (showLegendDialog) {
        LegendDialog(onDismiss = { showLegendDialog = false })
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
private fun LegendDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Legende", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    LegendDot(color = NeonRed, label = "Hoch (67\u2013100%) \u2014 Sofort handeln")
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
                    Text(
                        "\uD83D\uDD34 Rot = Dringend \u2014 Sofort handeln",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "\uD83D\uDFE0 Orange = Mittel \u2014 Bald angehen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "\uD83D\uDD35 Blau = Niedrig \u2014 Beobachten",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
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
                    "Empfehlungen",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
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

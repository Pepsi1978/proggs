package de.frank.entropyreducer.presentation.dashboard1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.presentation.ThemeViewModel
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.EntropyCategoryPill
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.StatusBar
import de.frank.entropyreducer.presentation.components.ThemeToggleIcon
import de.frank.entropyreducer.presentation.components.rememberMicPermissionState
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.color
import de.frank.entropyreducer.presentation.theme.label

/**
 * Dashboard 1 — Aufgaben (Spec §10, Referenzbild 11/21).
 */
@Composable
fun TasksScreen(
    onOpenSettings: () -> Unit,
    onSwitchTab: (String) -> Unit,
    currentTab: String,
    vm: TasksViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val cosmos = LocalCosmos.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val micPerm = rememberMicPermissionState(
        onAllGranted = { vm.onMicClick() },
        onDenied = { denied ->
            val msg = if (Manifest.permission.RECORD_AUDIO in denied) {
                "Mikrofon-Zugriff wurde abgelehnt. Aktiviere ihn in den System-Einstellungen, damit du Eintraege per Sprache erfassen kannst."
            } else {
                "Benachrichtigungs-Zugriff fehlt — die Aufnahme braucht ihn fuer die Foreground-Notification."
            }
            scope.launch { snackbar.showSnackbar(msg) }
        },
    )

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            vm.dismissError()
        }
    }

    val themeVm: ThemeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val themeMode by themeVm.themeMode.collectAsState()

    CosmosScaffold(
        title = "Entropie Reduktor",
        actions = {
            ThemeToggleIcon(current = themeMode, onCycle = themeVm::cycle)
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Einstellungen",
                    tint = cosmos.textPrimary,
                )
            }
        },
        bottomBar = {
            CosmosBottomBar(
                currentTab = currentTab,
                micState = state.micState,
                onTabSelected = onSwitchTab,
                onMicClick = {
                    if (micPerm.check()) vm.onMicClick() else micPerm.request()
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                StatusBar(percent = state.statusPercent)
                Spacer(Modifier.height(8.dp))

                state.processingMessage?.let {
                    GlassCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cosmos.textPrimary,
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        CategoryFilterRow(
                            active = state.activeCategories,
                            onToggle = vm::toggleCategory,
                            onClearAll = vm::clearCategoryFilter,
                        )
                    }

                    if (state.entriesByBucket.values.all { it.isEmpty() }) {
                        item { EmptyState() }
                    } else {
                        TimeBucket.values().forEach { bucket ->
                            val list = state.entriesByBucket[bucket].orEmpty()
                            if (list.isNotEmpty()) {
                                item { BucketHeader(bucket, list.size, list.sumOf { it.severity }) }
                                items(list, key = { it.id }) { entry ->
                                    EntropyEntryCard(entry)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(120.dp)) }  // Platz fuer Bottom-Nav
                }
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp),
            ) { Snackbar(it) }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    active: Set<EntropyCategory>,
    onToggle: (EntropyCategory) -> Unit,
    onClearAll: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            val isAll = active.isEmpty()
            AssistChip(
                onClick = onClearAll,
                label = { Text("Alle", fontWeight = if (isAll) FontWeight.Bold else FontWeight.Normal) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isAll) CosmosColors.AccentPrimary.copy(alpha = 0.2f) else Color.Transparent,
                    labelColor = if (isAll) CosmosColors.AccentPrimary else LocalCosmos.current.textSecondary,
                ),
            )
        }
        items(EntropyCategory.values().toList()) { cat ->
            val on = cat in active
            AssistChip(
                onClick = { onToggle(cat) },
                label = { Text(cat.label()) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (on) cat.color().copy(alpha = 0.2f) else Color.Transparent,
                    labelColor = if (on) cat.color() else LocalCosmos.current.textSecondary,
                ),
            )
        }
    }
}

@Composable
private fun BucketHeader(bucket: TimeBucket, count: Int, sumSeverity: Int) {
    val cosmos = LocalCosmos.current
    val label = when (bucket) {
        TimeBucket.HEUTE -> "HEUTE"
        TimeBucket.MORGEN -> "MORGEN"
        TimeBucket.DIESE_WOCHE -> "DIESE WOCHE"
        TimeBucket.DIESEN_MONAT -> "DIESEN MONAT"
        TimeBucket.SPAETER -> "SPAETER"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 6.dp, height = 18.dp)
                .clip(RoundedCornerShape(50))
                .background(CosmosColors.AccentPrimary),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = cosmos.textPrimary,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = cosmos.textSecondary,
        )
    }
}

@Composable
private fun EntropyEntryCard(entry: EntropyEntryEntity) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EntropyCategoryPill(entry.category)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${entry.priorityScore.toInt()}",
                    color = CosmosColors.AccentPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = cosmos.textPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyMedium,
                color = cosmos.textSecondary,
                maxLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            // Severity-Balken (1-10)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(cosmos.glassBg),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(entry.severity / 10f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(entry.category.color()),
                )
            }
            if (entry.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.tags.take(3).forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = cosmos.textSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(cosmos.glassBg)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val cosmos = LocalCosmos.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Mic,
            contentDescription = null,
            tint = CosmosColors.AccentPrimary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tippe auf das Mikrofon und sprich aus, was deine Energie kostet.",
            style = MaterialTheme.typography.titleMedium,
            color = cosmos.textPrimary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Die KI ordnet es ein, priorisiert es und plant es in deinen Schichtkalender ein.",
            style = MaterialTheme.typography.bodyMedium,
            color = cosmos.textSecondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

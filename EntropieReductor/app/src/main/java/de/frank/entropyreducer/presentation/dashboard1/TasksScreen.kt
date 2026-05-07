package de.frank.entropyreducer.presentation.dashboard1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.draw.alpha
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
import de.frank.entropyreducer.domain.model.EntryStatus
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
                "Mikrofon-Zugriff wurde abgelehnt. Aktiviere ihn in den System-Einstellungen, damit du Einträge per Sprache erfassen kannst."
            } else {
                "Benachrichtigungs-Zugriff fehlt — die Aufnahme braucht ihn für die Foreground-Notification."
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
                StatusBar(percent = state.statusPercent, breakdown = state.statusBreakdown)
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
                        de.frank.entropyreducer.presentation.briefing.BriefingPanel()
                    }
                    state.kiQuestion?.let { q ->
                        item {
                            de.frank.entropyreducer.presentation.components.KiQuestionCard(
                                question = q,
                                onMicAnswer = {
                                    if (micPerm.check()) vm.onMicClick() else micPerm.request()
                                },
                                onSnooze = vm::snoozeKiQuestion,
                            )
                        }
                    }
                    item {
                        CategoryFilterRow(
                            active = state.activeCategories,
                            onToggle = vm::toggleCategory,
                            onClearAll = vm::clearCategoryFilter,
                        )
                    }

                    if (state.entriesByBucket.values.all { it.isEmpty() } && state.resolvedEntries.isEmpty()) {
                        item { EmptyState() }
                    } else {
                        // Aktive Eintraege gruppiert nach Time-Bucket
                        TimeBucket.values().forEach { bucket ->
                            val list = state.entriesByBucket[bucket].orEmpty()
                                .filter { it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT }
                            if (list.isNotEmpty()) {
                                item { BucketHeader(bucket, list.size, list.sumOf { it.severity }) }
                                items(list, key = { it.id }) { entry ->
                                    EntropyEntryCard(
                                        entry = entry,
                                        onClick = { vm.openEntryDetail(entry.id) },
                                        onResolve = {
                                            vm.markEntryResolved(entry.id)
                                            scope.launch {
                                                val result = snackbar.showSnackbar(
                                                    message = "Eintrag erledigt: ${entry.title}",
                                                    actionLabel = "Rückgängig",
                                                    duration = androidx.compose.material3.SnackbarDuration.Short,
                                                )
                                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                    vm.reopenEntry(entry.id)
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        // Erledigt-Sektion am Ende
                        if (state.resolvedEntries.isNotEmpty()) {
                            item { ResolvedHeader(state.resolvedEntries.size) }
                            items(state.resolvedEntries, key = { "resolved-${it.id}" }) { entry ->
                                EntropyEntryCard(
                                    entry = entry,
                                    onClick = { vm.openEntryDetail(entry.id) },
                                    onResolve = {
                                        // Tap auf Haken bei erledigtem Eintrag → wieder offen
                                        vm.reopenEntry(entry.id)
                                    },
                                )
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

    // Detail-Bottom-Sheet — wird durch Tap auf eine Eintrag-Card geoeffnet.
    state.detailEntry?.let { entry ->
        EntryDetailSheet(
            entry = entry,
            onClose = { vm.closeEntryDetail() },
            onSetStatus = { st -> vm.setEntryStatus(entry.id, st) },
            onDelete = { vm.deleteEntry(entry.id) },
        )
    }
}

/**
 * Detail-Bottom-Sheet (Bild 12/22). Zeigt Eintrag im Detail mit Icon-Kreis,
 * Title, Beschreibung, Schweregrad-Hinweis, 4 Status-Buttons (Offen / In Arbeit /
 * Reduziert / Archiviert), Tags, KI-Begruendung + KI-Notizen, sowie ein
 * "Löschen"-Button. Aus dem Sheet kann der Status direkt umgestellt werden.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun EntryDetailSheet(
    entry: EntropyEntryEntity,
    onClose: () -> Unit,
    onSetStatus: (EntryStatus) -> Unit,
    onDelete: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header: Title + X
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Eintrag im Detail",
                    style = MaterialTheme.typography.titleLarge,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.Close,
                        contentDescription = "Schließen",
                        tint = cosmos.textSecondary,
                    )
                }
            }
            // Hero-Card mit Icon-Kreis + Title + Score
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        CategoryIconCircle(category = entry.category, tint = entry.category.color())
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            EntropyCategoryPill(entry.category)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = cosmos.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = entry.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cosmos.textSecondary,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${entry.priorityScore.toInt()}",
                                color = CosmosColors.AccentPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("Prio", color = cosmos.textSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Schweregrad: ${severityLabel(entry.severity)} (${entry.severity}/10)",
                        style = MaterialTheme.typography.labelMedium,
                        color = cosmos.textSecondary,
                    )
                    Spacer(Modifier.height(6.dp))
                    SeverityRainbowBar(severity = entry.severity)
                }
            }
            // Status-Section: 4 Buttons
            Text(
                "Status",
                style = MaterialTheme.typography.titleSmall,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusButton("Offen", EntryStatus.OFFEN, entry.status, onSetStatus, modifier = Modifier.weight(1f))
                StatusButton("In Arbeit", EntryStatus.IN_ARBEIT, entry.status, onSetStatus, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusButton("Reduziert", EntryStatus.REDUZIERT, entry.status, onSetStatus, modifier = Modifier.weight(1f))
                StatusButton("Archiviert", EntryStatus.ARCHIVIERT, entry.status, onSetStatus, modifier = Modifier.weight(1f))
            }
            // Tags
            if (entry.tags.isNotEmpty()) {
                Text("Tags", style = MaterialTheme.typography.titleSmall, color = cosmos.textPrimary, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.tags.forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = cosmos.textSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(cosmos.glassBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            // KI-Begruendung
            if (entry.priorityReason.isNotBlank() || !entry.aiNotes.isNullOrBlank()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            "KI-Begruendung",
                            style = MaterialTheme.typography.titleSmall,
                            color = CosmosColors.AccentSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = entry.priorityReason.ifBlank { "(keine Begruendung)" },
                            style = MaterialTheme.typography.bodySmall,
                            color = cosmos.textPrimary,
                        )
                        if (!entry.aiNotes.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "KI-Notizen",
                                style = MaterialTheme.typography.titleSmall,
                                color = CosmosColors.AccentSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = entry.aiNotes!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = cosmos.textPrimary,
                            )
                        }
                    }
                }
            }
            // Loeschen-Button — gefuellt rot, klar sichtbar.
            androidx.compose.material3.Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = CosmosColors.Critical,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Löschen",
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatusButton(
    label: String,
    status: EntryStatus,
    current: EntryStatus,
    onClick: (EntryStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    val selected = status == current
    val accent = if (selected) CosmosColors.AccentPrimary else cosmos.textSecondary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) CosmosColors.AccentPrimary.copy(alpha = 0.18f) else cosmos.glassBg)
            .clickable { onClick(status) }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun severityLabel(severity: Int): String = when {
    severity <= 3 -> "Niedrig"
    severity <= 6 -> "Mittel"
    severity <= 8 -> "Hoch"
    else -> "Sehr hoch"
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
            CategoryFilterChip(
                label = "Alle",
                icon = Icons.Outlined.GridView,
                tint = CosmosColors.AccentPrimary,
                selected = isAll,
                onClick = onClearAll,
            )
        }
        items(EntropyCategory.values().toList()) { cat ->
            val on = cat in active
            CategoryFilterChip(
                label = cat.label(),
                icon = iconForCategory(cat),
                tint = cat.color(),
                selected = on,
                onClick = { onToggle(cat) },
            )
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) tint.copy(alpha = 0.20f) else cosmos.glassBg,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) tint else cosmos.textSecondary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) tint else cosmos.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun iconForCategory(category: EntropyCategory): androidx.compose.ui.graphics.vector.ImageVector =
    when (category) {
        EntropyCategory.KOERPERLICH -> Icons.Outlined.Bolt
        EntropyCategory.MENTAL -> Icons.Outlined.Psychology
        EntropyCategory.ZEITLICH -> Icons.Outlined.AccessTime
        EntropyCategory.EMOTIONAL -> Icons.Outlined.FavoriteBorder
        EntropyCategory.GESUNDHEITLICH -> Icons.Outlined.MedicalServices
        EntropyCategory.UMGEBUNG -> Icons.Outlined.Home
        EntropyCategory.SONSTIGES -> Icons.Outlined.MoreHoriz
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
    val accent = bucketAccent(bucket)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon-Pille mit Calendar-Icon (Soll-Design)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = bucketIcon(bucket),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        // Count-Pill rechts
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.18f))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ResolvedHeader(count: Int) {
    val cosmos = LocalCosmos.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CosmosColors.Success.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = CosmosColors.Success,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "ERLEDIGT",
            style = MaterialTheme.typography.labelLarge,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(CosmosColors.Success.copy(alpha = 0.18f))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = CosmosColors.Success,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun bucketIcon(bucket: TimeBucket): androidx.compose.ui.graphics.vector.ImageVector = when (bucket) {
    TimeBucket.HEUTE -> Icons.Outlined.Today
    TimeBucket.MORGEN -> Icons.Outlined.Event
    TimeBucket.DIESE_WOCHE -> Icons.Outlined.DateRange
    TimeBucket.DIESEN_MONAT -> Icons.Outlined.CalendarMonth
    TimeBucket.SPAETER -> Icons.Outlined.HourglassEmpty
}

@Composable
private fun bucketAccent(bucket: TimeBucket): Color = when (bucket) {
    TimeBucket.HEUTE -> CosmosColors.AccentPrimary
    TimeBucket.MORGEN -> CosmosColors.AccentSecondary
    TimeBucket.DIESE_WOCHE -> CosmosColors.CatHealth
    TimeBucket.DIESEN_MONAT -> CosmosColors.CatMental
    TimeBucket.SPAETER -> LocalCosmos.current.textSecondary
}

@Composable
private fun EntropyEntryCard(
    entry: EntropyEntryEntity,
    onClick: () -> Unit = {},
    onResolve: () -> Unit = {},
    onSeverityHint: () -> Unit = {},
) {
    val cosmos = LocalCosmos.current
    val catColor = entry.category.color()
    val isResolved = entry.status == EntryStatus.REDUZIERT || entry.status == EntryStatus.ARCHIVIERT
    // Erledigte Eintraege werden ausgegraut und durchgestrichen — visueller Status.
    val cardAlpha = if (isResolved) 0.55f else 1f
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .alpha(cardAlpha),
    ) {
        Column {
            // Top-Row: Icon-Kreis links | Title+Beschreibung | Score+"Prio"+Haken
            Row(verticalAlignment = Alignment.Top) {
                CategoryIconCircle(category = entry.category, tint = catColor)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EntropyCategoryPill(entry.category)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = entry.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = cosmos.textSecondary,
                        maxLines = 2,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${entry.priorityScore.toInt()}",
                        color = CosmosColors.AccentPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Prio",
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    // Checkbox-Stil: leeres Quadrat wenn offen, ausgefuelltes Haekchen wenn erledigt.
                    // Tap toggelt — offen → erledigt (REDUZIERT), erledigt → wieder OFFEN.
                    val checkBg = if (isResolved) CosmosColors.Success.copy(alpha = 0.85f) else androidx.compose.ui.graphics.Color.Transparent
                    val checkBorder = if (isResolved) CosmosColors.Success else cosmos.glassBorder
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(checkBg)
                            .border(
                                androidx.compose.foundation.BorderStroke(2.dp, checkBorder),
                                RoundedCornerShape(8.dp),
                            )
                            .clickable(onClick = onResolve),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isResolved) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Erledigt",
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Severity-Bar im Regenbogen-Stil (Soll-Design): 5 Segmente in Status-Farben,
            // gefuellt nach severity (1-10 -> 0..1)
            SeverityRainbowBar(severity = entry.severity)

            // Tag-Pills
            if (entry.tags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.tags.take(3).forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = cosmos.textSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(cosmos.glassBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // Meta-Row unten: Bucket-Hinweis | Empfohlen-Badge | Wearable-Badge
            Spacer(Modifier.height(10.dp))
            EntryMetaRow(entry = entry)
        }
    }
}

/** Farbiger Kreis mit Material-Icon basierend auf der Entropie-Kategorie. */
@Composable
private fun CategoryIconCircle(
    category: de.frank.entropyreducer.domain.model.EntropyCategory,
    tint: androidx.compose.ui.graphics.Color,
) {
    val icon = when (category) {
        de.frank.entropyreducer.domain.model.EntropyCategory.KOERPERLICH -> Icons.Outlined.Bolt
        de.frank.entropyreducer.domain.model.EntropyCategory.MENTAL -> Icons.Outlined.Psychology
        de.frank.entropyreducer.domain.model.EntropyCategory.ZEITLICH -> Icons.Outlined.AccessTime
        de.frank.entropyreducer.domain.model.EntropyCategory.EMOTIONAL -> Icons.Outlined.FavoriteBorder
        de.frank.entropyreducer.domain.model.EntropyCategory.GESUNDHEITLICH -> Icons.Outlined.MedicalServices
        de.frank.entropyreducer.domain.model.EntropyCategory.UMGEBUNG -> Icons.Outlined.Home
        de.frank.entropyreducer.domain.model.EntropyCategory.SONSTIGES -> Icons.Outlined.MoreHoriz
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * 5-Segment-Severity-Bar im Regenbogen-Stil (Soll-Design). Die Segmente sind
 * gleich gross, der "ausgefuellte" Anteil ergibt sich aus severity/10. Nicht
 * gefuellte Segmente sind ausgegraut, gefuellte zeigen ihre Status-Farbe.
 */
@Composable
private fun SeverityRainbowBar(severity: Int) {
    val cosmos = LocalCosmos.current
    val sev = severity.coerceIn(1, 10)
    val palette = listOf(
        CosmosColors.StatusGreen,
        CosmosColors.StatusLightGreen,
        CosmosColors.StatusYellow,
        CosmosColors.StatusOrange,
        CosmosColors.StatusRed,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (i in 0 until 5) {
            // Segment i ist gefuellt wenn severity >= (i+1)*2
            val filled = sev >= (i + 1) * 2
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (filled) palette[i] else cosmos.glassBg),
            )
        }
    }
}

@Composable
private fun EntryMetaRow(entry: EntropyEntryEntity) {
    val cosmos = LocalCosmos.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Bucket-Time-Label (TimeBucket)
        val bucketLabel = when (entry.timeBucket) {
            de.frank.entropyreducer.domain.model.TimeBucket.HEUTE -> "heute"
            de.frank.entropyreducer.domain.model.TimeBucket.MORGEN -> "morgen"
            de.frank.entropyreducer.domain.model.TimeBucket.DIESE_WOCHE -> "diese Woche"
            de.frank.entropyreducer.domain.model.TimeBucket.DIESEN_MONAT -> "diesen Monat"
            de.frank.entropyreducer.domain.model.TimeBucket.SPAETER -> "später"
        }
        val durationHint = entry.estimatedDurationMinutes?.let {
            when {
                it < 60 -> "$it min"
                it < 24 * 60 -> "${it / 60} h"
                else -> "${it / (24 * 60)} d"
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = cosmos.textSecondary,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = if (durationHint != null) "$bucketLabel, $durationHint" else bucketLabel,
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        // Empfohlen-Badge (immer wenn priorityScore > 70)
        if (entry.priorityScore > 70) {
            Text(
                text = "Empfohlen",
                color = CosmosColors.Success,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CosmosColors.Success.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        // Wearable-Indikator (wenn ein Biomarker-Snapshot verlinkt ist)
        if (entry.biomarkerSnapshotId != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.MonitorHeart,
                    contentDescription = "Wearable-Bezug",
                    tint = CosmosColors.AccentSecondary,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "Wearable",
                    color = CosmosColors.AccentSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
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

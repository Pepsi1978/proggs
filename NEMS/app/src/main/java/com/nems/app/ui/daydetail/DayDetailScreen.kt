package com.nems.app.ui.daydetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nems.app.ui.theme.SectionEvening1
import com.nems.app.ui.theme.SectionEvening2
import com.nems.app.ui.theme.SectionEvening3
import com.nems.app.ui.theme.SectionMorning1
import com.nems.app.ui.theme.SectionMorning2
import com.nems.app.ui.theme.SectionPreSport
import com.nems.app.ui.theme.StatusGreen
import com.nems.app.ui.theme.StatusYellow
import com.nems.app.util.DateUtils

// Stable shape constants — allocated once, never recreated during recomposition
private val SectionCardShape = RoundedCornerShape(12.dp)
private val EntryCardShape = RoundedCornerShape(8.dp)
private val ToggleCardShape = RoundedCornerShape(12.dp)
private val ProgressCardShape = RoundedCornerShape(12.dp)
private val ButtonShape = RoundedCornerShape(12.dp)

@Composable
private fun sectionAccentColor(sectionId: String): Color = when (sectionId) {
    "morning1" -> SectionMorning1
    "morning2" -> SectionMorning2
    "presport" -> SectionPreSport
    "evening1" -> SectionEvening1
    "evening2" -> SectionEvening2
    "evening3" -> SectionEvening3
    else -> MaterialTheme.colorScheme.primary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    dateString: String,
    onNavigateBack: () -> Unit,
    viewModel: DayDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Track collapsed state per section
    val collapsedSections = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val state = uiState) {
                        is DayDetailUiState.Success -> DateUtils.formatDayHeader(state.date)
                        else -> dateString
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurueck",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is DayDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is DayDetailUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Dienst/Frei toggle
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        DienstToggleCard(
                            isDienstTag = state.isDienstTag,
                            onToggle = { viewModel.toggleDienstTag() },
                        )
                    }

                    // Overall progress bar
                    item {
                        OverallProgressCard(progress = state.overallProgress)
                    }

                    // Sections
                    items(state.sections, key = { it.section.id }) { sectionWithEntries ->
                        val sectionId = sectionWithEntries.section.id
                        val isCollapsed = collapsedSections[sectionId] ?: true
                        val onCollapse = remember(sectionId) { { collapsedSections[sectionId] = !isCollapsed } }
                        val onMarkComplete = remember(sectionId) { { viewModel.toggleSectionComplete(sectionId) } }
                        SectionCard(
                            sectionWithEntries = sectionWithEntries,
                            isCollapsed = isCollapsed,
                            onToggleCollapse = onCollapse,
                            onToggleEntry = { entryId, taken ->
                                viewModel.toggleEntry(entryId, taken)
                            },
                            onMarkSectionComplete = onMarkComplete,
                        )
                    }

                    // Complete day button
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.toggleDayComplete() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusGreen,
                                contentColor = Color.Black,
                            ),
                            shape = ButtonShape,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tag als erledigt markieren",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DienstToggleCard(
    isDienstTag: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ToggleCardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ToggleCardShape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = if (isDienstTag) "Diensttag" else "Freier Tag",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (isDienstTag) "Sport-Protokoll aktiv" else "Normales Protokoll",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = isDienstTag,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

@Composable
private fun OverallProgressCard(progress: Float) {
    val percentage = (progress * 100).toInt()
    val progressColor = when {
        progress >= 1.0f -> StatusGreen
        progress >= 0.5f -> StatusYellow
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ProgressCardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ProgressCardShape)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Gesamtfortschritt",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = progressColor,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun SectionCard(
    sectionWithEntries: SectionWithEntries,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onToggleEntry: (String, Boolean) -> Unit,
    onMarkSectionComplete: () -> Unit,
) {
    val section = sectionWithEntries.section
    val accentColor = sectionAccentColor(section.id)
    val sectionBorder = remember(accentColor) { androidx.compose.foundation.BorderStroke(1.dp, accentColor) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SectionCardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, SectionCardShape),
    ) {
        // Section header with accent stripe
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleCollapse),
        ) {
            // Accent color stripe on the left
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .background(accentColor),
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = section.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = section.timeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                    )
                    Text(
                        text = section.instruction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Progress count badge
                Box(
                    modifier = Modifier
                        .clip(EntryCardShape)
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${sectionWithEntries.takenCount}/${sectionWithEntries.totalCount}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    )
                }

                // Collapse/expand icon
                Icon(
                    imageVector = if (isCollapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                    contentDescription = if (isCollapsed) "Aufklappen" else "Einklappen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(20.dp),
                )
            }
        }

        // Collapsible entries
        AnimatedVisibility(
            visible = !isCollapsed,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                sectionWithEntries.entries.forEach { entryWithSupplement ->
                    key(entryWithSupplement.entry.id) {
                        SupplementEntryRow(
                            entryWithSupplement = entryWithSupplement,
                            onToggle = { taken ->
                                onToggleEntry(entryWithSupplement.entry.id, taken)
                            },
                        )
                    }
                }

                // "Mark all" button
                OutlinedButton(
                    onClick = onMarkSectionComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    border = sectionBorder,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = accentColor,
                    ),
                    shape = EntryCardShape,
                ) {
                    Text(
                        text = "Alle abhaken",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplementEntryRow(
    entryWithSupplement: EntryWithSupplement,
    onToggle: (Boolean) -> Unit,
) {
    val entry = entryWithSupplement.entry
    val supplement = entryWithSupplement.supplement

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!entry.taken) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = entry.taken,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = StatusGreen,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkmarkColor = Color.Black,
            ),
            modifier = Modifier.size(24.dp),
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = supplement?.name ?: entry.supplementId,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (entry.taken) FontWeight.Normal else FontWeight.Medium,
                color = if (entry.taken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
            )
            if (supplement != null) {
                Text(
                    text = "${supplement.dosage}  •  ${supplement.capsuleCount}x ${supplement.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
        }

        // Indicator dots for diarrhoea risk and powder
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (supplement?.diarrhoeaRisk == true) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusYellow),
                )
            }
            if (supplement?.isPowder == true) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusGreen),
                )
            }
        }
    }
}

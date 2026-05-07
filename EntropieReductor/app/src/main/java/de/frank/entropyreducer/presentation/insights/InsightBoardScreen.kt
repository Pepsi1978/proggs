package de.frank.entropyreducer.presentation.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.EntropyCategoryPill
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.color
import de.frank.entropyreducer.presentation.theme.label

/**
 * Insight Board (Spec §14.2): drei kollabierbare Sektionen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightBoardScreen(
    onBack: () -> Unit,
    vm: InsightBoardViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val cosmos = LocalCosmos.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    CosmosScaffold(
        title = "Insight Board",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Zurueck", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            section("Bestaetigte Methoden", state.confirmed, Section.CONFIRMED, state.expandedSection, vm)
            section("In Beobachtung", state.observation, Section.OBSERVATION, state.expandedSection, vm)
            section("Verworfen", state.discarded, Section.DISCARDED, state.expandedSection, vm)
        }
    }

    state.selected?.let { insight ->
        ModalBottomSheet(
            onDismissRequest = vm::close,
            sheetState = sheetState,
            containerColor = if (cosmos.isDark) CosmosColors.BgDarkAccent else CosmosColors.BgLight,
        ) {
            InsightDetailContent(
                insight = insight,
                onAdjustConfidence = { vm.adjustConfidence(insight, it) },
                onChangeCategory = { vm.setCategory(insight, it) },
                onSaveText = { title, desc -> vm.setTitleAndDescription(insight, title, desc) },
                onDelete = { vm.delete(insight) },
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    items: List<InsightEntity>,
    sectionKey: Section,
    expanded: Section,
    vm: InsightBoardViewModel,
) {
    val isExpanded = expanded == sectionKey
    item("header-${sectionKey.name}") {
        SectionHeader(
            title = title,
            count = items.size,
            isExpanded = isExpanded,
            onClick = { vm.toggleSection(sectionKey) },
        )
    }
    if (isExpanded) {
        if (items.isEmpty()) {
            item("empty-${sectionKey.name}") { EmptySection() }
        } else {
            items(items, key = { it.id }) { insight ->
                InsightCard(insight, onClick = { vm.open(insight) })
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, isExpanded: Boolean, onClick: () -> Unit) {
    val cosmos = LocalCosmos.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cosmos.glassBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = cosmos.textPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            color = cosmos.textSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (isExpanded) "Einklappen" else "Aufklappen",
            tint = cosmos.textSecondary,
        )
    }
}

@Composable
private fun EmptySection() {
    val cosmos = LocalCosmos.current
    Text(
        text = "Noch nichts hier — schliesse Hypothesen erfolgreich ab, dann sammeln sich Insights.",
        color = cosmos.textSecondary,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
internal fun InsightCard(insight: InsightEntity, onClick: () -> Unit) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EntropyCategoryPill(category = insight.targetCategory)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${insight.confidence}%",
                    color = cosmos.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = insight.title,
                color = cosmos.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (insight.description.isNotBlank()) {
                Text(
                    text = insight.description,
                    color = cosmos.textPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                )
            }
            ConfidenceBar(insight.confidence)
            Text(
                text = "Versuche: ${insight.attemptCount} · Erfolge: ${insight.successCount}" +
                    (insight.avgBiomarkerImpact?.let { " · $it" } ?: ""),
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
internal fun ConfidenceBar(percent: Int) {
    val cosmos = LocalCosmos.current
    val pct = percent.coerceIn(0, 100) / 100f
    val color = when {
        percent >= 70 -> CosmosColors.Success
        percent >= 40 -> CosmosColors.Warning
        else -> CosmosColors.Critical
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(cosmos.glassBorder),
    ) {
        Box(
            Modifier
                .fillMaxWidth(pct)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
    }
}

@Composable
private fun InsightDetailContent(
    insight: InsightEntity,
    onAdjustConfidence: (Int) -> Unit,
    onChangeCategory: (EntropyCategory) -> Unit,
    onSaveText: (String, String) -> Unit,
    onDelete: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    // rememberSaveable: ungespeicherte Edits ueberleben Foldable-Klappung + Drehung
    // (Galaxy Fold 6 Hauptszenario). Vorher gingen Titel/Description-Aenderungen
    // verloren wenn der Nutzer das Geraet wechselte zwischen Falten und Tippen.
    var title by rememberSaveable(insight.id) { mutableStateOf(insight.title) }
    var description by rememberSaveable(insight.id) { mutableStateOf(insight.description) }
    var confidence by rememberSaveable(insight.id) { mutableStateOf(insight.confidence.toFloat()) }
    var categoryMenu by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titel") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors(cosmos),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Beschreibung") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors(cosmos),
        )
        TextButton(onClick = { onSaveText(title, description) }) {
            Text("Text speichern", color = CosmosColors.AccentPrimary)
        }

        Text(
            "Confidence: ${confidence.toInt()}%",
            color = cosmos.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = confidence,
            onValueChange = { confidence = it },
            onValueChangeFinished = { onAdjustConfidence(confidence.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = CosmosColors.AccentPrimary,
                activeTrackColor = CosmosColors.AccentPrimary,
                inactiveTrackColor = cosmos.glassBorder,
            ),
        )

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, cosmos.glassBorder), RoundedCornerShape(12.dp))
                    .clickable { categoryMenu = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Zielkategorie:",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                EntropyCategoryPill(category = insight.targetCategory)
            }
            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                EntropyCategory.values().forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.label()) },
                        onClick = {
                            onChangeCategory(c)
                            categoryMenu = false
                        },
                    )
                }
            }
        }

        Text(
            text = "Versuche: ${insight.attemptCount} · Erfolge: ${insight.successCount}" +
                (insight.avgBiomarkerImpact?.let { " · $it" } ?: ""),
            color = cosmos.textSecondary,
            style = MaterialTheme.typography.bodySmall,
        )

        Button(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CosmosColors.Critical.copy(alpha = 0.20f),
                contentColor = CosmosColors.Critical,
            ),
        ) {
            Icon(Icons.Outlined.Delete, null)
            Spacer(Modifier.width(8.dp))
            Text("Insight loeschen")
        }
    }
}

@Composable
private fun textFieldColors(cosmos: de.frank.entropyreducer.presentation.theme.CosmosThemeExt) =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = cosmos.textPrimary,
        unfocusedTextColor = cosmos.textPrimary,
        focusedBorderColor = CosmosColors.AccentPrimary,
        unfocusedBorderColor = cosmos.glassBorder,
    )

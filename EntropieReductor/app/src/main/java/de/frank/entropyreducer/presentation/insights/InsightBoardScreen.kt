package de.frank.entropyreducer.presentation.insights

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.EntropyCategoryPill
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.WhisperMicButton
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.color
import de.frank.entropyreducer.presentation.theme.label

/**
 * Insight Board (Spec §14.2): drei kollabierbare Sektionen. Frank-Wunsch 2026-05-09: Plus-Button
 * rechts oben oeffnet ein Sheet zum manuellen Hinzufuegen einer bestaetigten Methode per Mic +
 * KI-Politur.
 */
// Performance-Audit Loop 8 (2026-05-10): Top-level Liste statt .values()-Array.
private val ALL_ENTROPY_CATEGORIES: List<EntropyCategory> = EntropyCategory.entries.toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightBoardScreen(onBack: () -> Unit, vm: InsightBoardViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    CosmosScaffold(
        title = "Insight Board",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
        actions = {
            IconButton(onClick = vm::openAddDialog) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Bestätigte Methode manuell hinzufügen",
                    tint = cosmos.textPrimary,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            section(
                "Bestätigte Methoden",
                state.confirmed,
                Section.CONFIRMED,
                state.expandedSection,
                vm,
            )
            section(
                "In Beobachtung",
                state.observation,
                Section.OBSERVATION,
                state.expandedSection,
                vm,
            )
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
                onToggleAdditional = { vm.toggleAdditionalCategory(insight, it) },
                onSaveText = { title, desc -> vm.setTitleAndDescription(insight, title, desc) },
                onDelete = { vm.delete(insight) },
            )
        }
    }

    val add = state.addDialog
    if (add !is AddMethodState.Closed) {
        ModalBottomSheet(
            onDismissRequest = vm::closeAddDialog,
            sheetState = addSheetState,
            containerColor = if (cosmos.isDark) CosmosColors.BgDarkAccent else CosmosColors.BgLight,
        ) {
            AddMethodSheet(
                state = add,
                onTranscript = vm::onTranscriptReady,
                onTitleChange = vm::editAddTitle,
                onDescriptionChange = vm::editAddDescription,
                onPrimaryChange = vm::setAddPrimaryCategory,
                onToggleAdditional = vm::toggleAddAdditional,
                onSave = vm::saveManualMethod,
                onCancel = vm::closeAddDialog,
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
            // Performance-Audit Loop 2 (2026-05-10): contentType ergaenzt —
            // alle Insight-Karten haben aktuell denselben Typ; bei kuenftigen
            // Sub-Typen sind die Slots schon korrekt klassifiziert.
            items(items, key = { it.id }, contentType = { "insight" }) { insight ->
                InsightCard(insight, onClick = { vm.open(insight) })
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, isExpanded: Boolean, onClick: () -> Unit) {
    val cosmos = LocalCosmos.current
    Row(
        modifier =
            Modifier.fillMaxWidth()
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
        text =
            "Noch nichts hier — schließe Hypothesen erfolgreich ab oder tippe auf das Plus-Symbol oben rechts, um eine Methode manuell hinzuzufügen.",
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
            // Frank-Wunsch 2026-05-09: zusaetzliche Kategorien als kleinere
            // Pillen unter der primary anzeigen — sichtbar machen welche Lebens-
            // bereiche eine Methode mit-adressiert (Laufen wirkt MENTAL +
            // KOERPERLICH + EMOTIONAL gleichzeitig).
            if (insight.additionalCategories.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    insight.additionalCategories.take(4).forEach { c -> AdditionalCategoryChip(c) }
                }
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
                text =
                    buildString {
                        append(
                            "Versuche: ${insight.attemptCount} · Erfolge: ${insight.successCount}"
                        )
                        insight.avgBiomarkerImpact?.let { append(" · $it") }
                        if (insight.manualSource) append(" · manuell")
                    },
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun AdditionalCategoryChip(category: EntropyCategory) {
    val cosmos = LocalCosmos.current
    val color = category.color()
    Box(
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.18f))
                .border(BorderStroke(1.dp, color.copy(alpha = 0.45f)), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "+ ${category.label()}",
            color = cosmos.textSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
internal fun ConfidenceBar(percent: Int) {
    val cosmos = LocalCosmos.current
    val pct = percent.coerceIn(0, 100) / 100f
    val color =
        when {
            percent >= 70 -> LocalCosmos.current.ok
            percent >= 40 -> LocalCosmos.current.warn
            else -> LocalCosmos.current.crit
        }
    Box(
        Modifier.fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(cosmos.glassBorder)
    ) {
        Box(
            Modifier.fillMaxWidth(pct).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color)
        )
    }
}

@Composable
private fun InsightDetailContent(
    insight: InsightEntity,
    onAdjustConfidence: (Int) -> Unit,
    onChangeCategory: (EntropyCategory) -> Unit,
    onToggleAdditional: (EntropyCategory) -> Unit,
    onSaveText: (String, String) -> Unit,
    onDelete: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    var title by rememberSaveable(insight.id) { mutableStateOf(insight.title) }
    var description by rememberSaveable(insight.id) { mutableStateOf(insight.description) }
    var confidence by rememberSaveable(insight.id) { mutableStateOf(insight.confidence.toFloat()) }
    var categoryMenu by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
            Text("Text speichern", color = LocalCosmos.current.accent)
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
            colors =
                SliderDefaults.colors(
                    thumbColor = LocalCosmos.current.accent,
                    activeTrackColor = LocalCosmos.current.accent,
                    inactiveTrackColor = cosmos.glassBorder,
                ),
        )

        Box {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, cosmos.glassBorder), RoundedCornerShape(12.dp))
                        .clickable { categoryMenu = true }
                        .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Hauptkategorie:",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                EntropyCategoryPill(category = insight.targetCategory)
            }
            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                ALL_ENTROPY_CATEGORIES.forEach { c ->
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

        // Multi-Kategorien-Auswahl als toggable Chips. Frank-Wunsch 2026-05-09:
        // Methoden duerfen so viele Kategorien tragen, wie zutreffen.
        Text(
            "Wirkt zusätzlich auf:",
            color = cosmos.textSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
        CategoryToggleRow(
            primary = insight.targetCategory,
            selected = insight.additionalCategories,
            onToggle = onToggleAdditional,
        )

        Text(
            text =
                "Versuche: ${insight.attemptCount} · Erfolge: ${insight.successCount}" +
                    (insight.avgBiomarkerImpact?.let { " · $it" } ?: ""),
            color = cosmos.textSecondary,
            style = MaterialTheme.typography.bodySmall,
        )

        Button(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = LocalCosmos.current.crit.copy(alpha = 0.20f),
                    contentColor = LocalCosmos.current.crit,
                ),
        ) {
            Icon(Icons.Outlined.Delete, null)
            Spacer(Modifier.width(8.dp))
            Text("Insight löschen")
        }
    }
}

/**
 * Sheet zum manuellen Hinzufuegen einer bestaetigten Methode (Frank-Wunsch 2026-05-09). Der
 * Mic-Button nimmt den Sprachtext per Whisper Large V3 Turbo auf, das Transkript geht durch
 * PolishMethodTextUseCase, dann sind Title/ Description/Categories editierbar — Frank kann alles
 * nochmal anpassen vor dem Speichern.
 */
@Composable
private fun AddMethodSheet(
    state: AddMethodState,
    onTranscript: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPrimaryChange: (EntropyCategory) -> Unit,
    onToggleAdditional: (EntropyCategory) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    var primaryMenu by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier.size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(LocalCosmos.current.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = LocalCosmos.current.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Bestätigte Methode hinzufügen",
                    color = cosmos.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Sprich die Maßnahme ein — die KI verbessert den Text und erkennt alle passenden Kategorien.",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Outlined.Close, "Schließen", tint = cosmos.textSecondary)
            }
        }

        // Aufnahme-Bereich
        when (state) {
            AddMethodState.Closed -> Unit
            AddMethodState.Polishing -> {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cosmos.glassBg)
                            .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = LocalCosmos.current.accent,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "KI verbessert die Methode…",
                            color = cosmos.textPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            AddMethodState.Saving -> {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cosmos.glassBg)
                            .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = LocalCosmos.current.ok,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Speichere…",
                            color = cosmos.textPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            is AddMethodState.Ready -> {
                // Mic-Button + Hinweis
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cosmos.glassBg)
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WhisperMicButton(onTranscript = onTranscript, size = 56.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val hint =
                            if (state.title.isBlank() && state.description.isBlank()) {
                                "Tippe und sprich die Methode frei ein."
                            } else if (state.usedAi) {
                                "KI-Politur fertig. Du kannst noch anpassen."
                            } else {
                                "Roher Text übernommen — KI war nicht erreichbar."
                            }
                        Text(
                            hint,
                            color = cosmos.textPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Whisper Large V3 Turbo · echte Umlaute · Du-Anrede",
                            color = cosmos.textSecondary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    label = { Text("Titel") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(cosmos),
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Beschreibung — wie und warum wirkt die Methode") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(cosmos),
                )

                // Hauptkategorie als Dropdown
                Box {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    BorderStroke(1.dp, cosmos.glassBorder),
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable { primaryMenu = true }
                                .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Hauptkategorie:",
                            color = cosmos.textSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                        )
                        EntropyCategoryPill(category = state.primaryCategory)
                    }
                    DropdownMenu(
                        expanded = primaryMenu,
                        onDismissRequest = { primaryMenu = false },
                    ) {
                        ALL_ENTROPY_CATEGORIES.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.label()) },
                                onClick = {
                                    onPrimaryChange(c)
                                    primaryMenu = false
                                },
                            )
                        }
                    }
                }

                Text(
                    "Wirkt zusätzlich auf (mehrere möglich):",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
                CategoryToggleRow(
                    primary = state.primaryCategory,
                    selected = state.additionalCategories,
                    onToggle = onToggleAdditional,
                )

                Button(
                    onClick = onSave,
                    enabled = state.title.isNotBlank() || state.description.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = LocalCosmos.current.accent,
                            contentColor = androidx.compose.ui.graphics.Color.White,
                        ),
                ) {
                    Text("In bestätigte Methoden speichern")
                }
            }
        }
    }
}

/**
 * Toggle-Reihe für Zusatzkategorien — alle Kategorien außer der Primary werden als auswählbare
 * Chips angezeigt. Tap toggelt drin/draußen.
 */
@Composable
private fun CategoryToggleRow(
    primary: EntropyCategory,
    selected: List<EntropyCategory>,
    onToggle: (EntropyCategory) -> Unit,
) {
    val cosmos = LocalCosmos.current
    val all = remember { EntropyCategory.values().toList() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        all.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { c ->
                    val isPrimary = c == primary
                    val isSelected = selected.contains(c)
                    val color = c.color()
                    val bg =
                        when {
                            isPrimary -> color.copy(alpha = 0.10f)
                            isSelected -> color.copy(alpha = 0.30f)
                            else -> cosmos.glassBg
                        }
                    val borderColor =
                        when {
                            isPrimary -> color.copy(alpha = 0.30f)
                            isSelected -> color
                            else -> cosmos.glassBorder
                        }
                    val textAlpha = if (isPrimary) 0.45f else 1f
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
                                .let { if (!isPrimary) it.clickable { onToggle(c) } else it }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .wrapContentHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = c.label(),
                            color = cosmos.textPrimary.copy(alpha = textAlpha),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
                // Auffuellen damit die Reihe immer 3 Slots breit ist
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun textFieldColors(cosmos: de.frank.entropyreducer.presentation.theme.CosmosThemeExt) =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = cosmos.textPrimary,
        unfocusedTextColor = cosmos.textPrimary,
        focusedBorderColor = LocalCosmos.current.accent,
        unfocusedBorderColor = cosmos.glassBorder,
    )

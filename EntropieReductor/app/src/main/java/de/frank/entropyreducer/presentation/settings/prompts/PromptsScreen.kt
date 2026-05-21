package de.frank.entropyreducer.presentation.settings.prompts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import de.frank.entropyreducer.presentation.agentic.PromptToolPermissionsDialog
import de.frank.entropyreducer.presentation.agentic.AgenticExecutionDialog
import de.frank.entropyreducer.presentation.agentic.TokenStatsScreen
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.domain.model.PromptCategory
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.settings.PromptsViewModel
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Eigene Prompts mit Kategorien (Frank-Wunsch 2026-05-20). Pro App-Bereich (Aufgaben, Entropie,
 * Thesen, Analyse, Forscher, Codex) ein eigenes Akkordeon mit eigenem "+"-Knopf — neue Prompts
 * landen direkt in der jeweiligen Kategorie, ohne dass der User die Kategorie nochmal in einem
 * Dropdown wählen muss.
 */
@Composable
fun PromptsScreen(onBack: () -> Unit, vm: PromptsViewModel = hiltViewModel()) {
    val prompts by vm.prompts.collectAsState()
    val cosmos = LocalCosmos.current
    var editing by remember { mutableStateOf<SavedPromptEntity?>(null) }
    var creatingInCategory by remember { mutableStateOf<PromptCategory?>(null) }
    var executePromptId by remember { mutableStateOf<String?>(null) }
    var showTokenStats by remember { mutableStateOf(false) }
    var permissionEditFor by remember { mutableStateOf<SavedPromptEntity?>(null) }
    val expandedMap = remember { mutableStateOf(mutableMapOf<PromptCategory, Boolean>()) }

    if (showTokenStats) {
        TokenStatsScreen(onBack = { showTokenStats = false })
        return
    }

    val byCategory: Map<PromptCategory, List<SavedPromptEntity>> =
        remember(prompts) {
            PromptCategory.entries.associateWith { cat -> prompts.filter { it.category == cat } }
        }

    CosmosScaffold(
        title = "Eigene Prompts",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
        actions = {
            IconButton(onClick = { showTokenStats = true }) {
                Icon(
                    Icons.Outlined.BarChart,
                    contentDescription = "Token-Verbrauch",
                    tint = cosmos.textPrimary,
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text =
                            "Prompts wirken nur in ihrer Kategorie — jeder Bereich der App bekommt seine eigenen Verhaltensregeln.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cosmos.textSecondary,
                    )
                }
                PromptCategory.entries.forEach { cat ->
                    item(key = "cat-${cat.name}") {
                        val expanded = expandedMap.value[cat] ?: false
                        val items = byCategory[cat].orEmpty()
                        CategoryAccordion(
                            category = cat,
                            count = items.size,
                            expanded = expanded,
                            onToggle = {
                                val m = expandedMap.value.toMutableMap()
                                m[cat] = !expanded
                                expandedMap.value = m
                            },
                            onAdd = { creatingInCategory = cat },
                            content = {
                                if (items.isEmpty()) {
                                    Text(
                                        text =
                                            "Noch keine Prompts in dieser Kategorie. Tippe auf das + im Kopf, um einen neuen anzulegen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cosmos.textSecondary,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items.forEach { p ->
                                            PromptRow(
                                                prompt = p,
                                                onToggle = { vm.toggle(p) },
                                                onEdit = { editing = p },
                                                onDelete = { vm.delete(p) },
                                                onExecute = { executePromptId = p.id },
                                                onPermissions = { permissionEditFor = p },
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    editing?.let { p ->
        EditDialog(
            initialName = p.name,
            initialContent = p.content,
            category = p.category,
            initialModel = p.model,
            initialTrust = p.trustModeDefault,
            onSave = { name, content, model, trust ->
                vm.save(
                    p.copy(
                        name = name,
                        content = content,
                        model = model,
                        trustModeDefault = trust,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
                editing = null
            },
            onCancel = { editing = null },
        )
    }
    creatingInCategory?.let { cat ->
        EditDialog(
            initialName = "",
            initialContent = "",
            category = cat,
            initialModel = "gemini-2.5-flash",
            initialTrust = false,
            onSave = { n, c, model, trust ->
                vm.create(n, c, cat, model, trust)
                creatingInCategory = null
            },
            onCancel = { creatingInCategory = null },
        )
    }
    executePromptId?.let { id ->
        AgenticExecutionDialog(
            promptId = id,
            onDismiss = { executePromptId = null },
        )
    }
    permissionEditFor?.let { p ->
        PromptToolPermissionsDialog(
            promptName = p.name,
            writeTools = vm.writeTools,
            permissionsFlow = vm.permissionsForPrompt(p.id),
            onSet = { toolName, granted, trust ->
                vm.setToolPermission(p.id, toolName, granted, trust)
            },
            onDismiss = { permissionEditFor = null },
        )
    }
}

@Composable
private fun CategoryAccordion(
    category: PromptCategory,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    content: @Composable () -> Unit,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.size(8.dp))
                Box(
                    modifier =
                        Modifier.clip(RoundedCornerShape(50))
                            .background(CosmosColors.AccentPrimary.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "$count",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmosColors.AccentPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onAdd) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Neuer Prompt in ${category.label}",
                        tint = CosmosColors.AccentPrimary,
                    )
                }
                Icon(
                    imageVector =
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Zuklappen" else "Aufklappen",
                    tint = cosmos.textSecondary,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) { content() }
            }
        }
    }
}

@Composable
private fun PromptRow(
    prompt: SavedPromptEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExecute: () -> Unit,
    onPermissions: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(cosmos.glassBg)
                .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    prompt.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = prompt.isActive,
                    onCheckedChange = { onToggle() },
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CosmosColors.AccentPrimary,
                            checkedBorderColor = CosmosColors.AccentPrimary,
                        ),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = prompt.content.take(140) + if (prompt.content.length > 140) "…" else "",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onExecute,
                    modifier = Modifier.size(32.dp),
                    enabled = prompt.isActive,
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = "Ausführen",
                        tint =
                            if (prompt.isActive) CosmosColors.AccentPrimary
                            else CosmosColors.AccentPrimary.copy(alpha = 0.35f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Bearbeiten",
                        tint = CosmosColors.AccentPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onPermissions, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = "Tool-Berechtigungen",
                        tint = CosmosColors.AccentPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Löschen",
                        tint = CosmosColors.Critical,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EditDialog(
    initialName: String,
    initialContent: String,
    category: PromptCategory,
    initialModel: String,
    initialTrust: Boolean,
    onSave: (name: String, content: String, model: String, trust: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var content by remember { mutableStateOf(initialContent) }
    var model by remember { mutableStateOf(initialModel) }
    var trustMode by remember { mutableStateOf(initialTrust) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && content.isNotBlank())
                        onSave(name.trim(), content.trim(), model, trustMode)
                },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = CosmosColors.AccentPrimary,
                        contentColor = Color.Black,
                    ),
            ) {
                Text("Speichern")
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Abbrechen") } },
        title = { Text("Prompt: ${category.label}") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Inhalt") },
                    modifier = Modifier.height(180.dp).fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                // Modell-Wahl (Frank-Wunsch 2026-05-21)
                Box {
                    OutlinedTextField(
                        value = modelLabel(model),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Modell") },
                        trailingIcon = {
                            IconButton(onClick = { modelDropdownExpanded = !modelDropdownExpanded }) {
                                Icon(
                                    Icons.Outlined.ArrowDropDown,
                                    contentDescription = "Modell waehlen",
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(
                        expanded = modelDropdownExpanded,
                        onDismissRequest = { modelDropdownExpanded = false },
                    ) {
                        MODEL_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(modelLabel(option)) },
                                onClick = {
                                    model = option
                                    modelDropdownExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Trust-Modus (mit Warnhinweis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Trust-Modus",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text =
                                "Wenn aktiv: Write-Tools laufen ohne Bestaetigungs-Dialog. " +
                                    "Bitte NUR fuer Prompts setzen denen du voll vertraust " +
                                    "(z.B. Background-Auto-Trigger).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = trustMode,
                        onCheckedChange = { trustMode = it },
                    )
                }
            }
        },
    )
}

private val MODEL_OPTIONS =
    listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.5-pro")

private fun modelLabel(model: String): String =
    when (model) {
        "gemini-2.5-pro" -> "Gemini 2.5 Pro (gruendlich, teurer)"
        "gemini-2.5-flash" -> "Gemini 2.5 Flash (schnell, ausreichend)"
        "gemini-2.5-flash-lite" -> "Gemini 2.5 Flash Lite (sehr schnell, einfache Aufgaben)"
        else -> model
    }

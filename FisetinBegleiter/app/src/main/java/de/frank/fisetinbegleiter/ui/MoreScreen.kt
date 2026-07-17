package de.frank.fisetinbegleiter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.frank.fisetinbegleiter.BuildConfig
import de.frank.fisetinbegleiter.data.IngredientEntity
import de.frank.fisetinbegleiter.data.IngredientPhase
import de.frank.fisetinbegleiter.data.ProtocolTemplateEntity
import de.frank.fisetinbegleiter.data.StackCategory
import de.frank.fisetinbegleiter.data.StackItemEntity
import de.frank.fisetinbegleiter.ui.theme.AllowedGreen
import de.frank.fisetinbegleiter.ui.theme.AllowedGreenContainer
import de.frank.fisetinbegleiter.ui.theme.WarningYellow
import de.frank.fisetinbegleiter.ui.theme.WarningYellowContainer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class MoreTab(val label: String) { PLANER("Schicht-Planer"), EINSTELLUNGEN("Einstellungen"), SICHERHEIT("Sicherheit") }
private enum class ShiftBlock(val label: String) {
    DAY("Tagdienst"), FREE_AFTER_DAY("Frei nach Tagdienst"), NIGHT("Nachtdienst"), FREE_AFTER_NIGHT("Frei nach Nachtdienst")
}

@Composable
fun MoreScreen(
    state: MainUiState,
    onSaveProtocol: (ProtocolTemplateEntity) -> Unit,
    onSaveIngredient: (IngredientEntity) -> Unit,
    onDeleteIngredient: (IngredientEntity) -> Unit,
    onSaveStack: (StackItemEntity) -> Unit,
    onDeleteStack: (StackItemEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(MoreTab.PLANER) }
    Column(modifier.padding(horizontal = 20.dp)) {
        LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MoreTab.entries) { destination ->
                FilterChip(selected = tab == destination, onClick = { tab = destination }, label = { Text(destination.label) })
            }
        }
        when (tab) {
            MoreTab.PLANER -> ShiftPlanner(Modifier.weight(1f))
            MoreTab.EINSTELLUNGEN -> SettingsContent(
                state,
                onSaveProtocol,
                onSaveIngredient,
                onDeleteIngredient,
                onSaveStack,
                onDeleteStack,
                Modifier.weight(1f),
            )
            MoreTab.SICHERHEIT -> SafetyContent(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ShiftPlanner(modifier: Modifier) {
    var block by remember { mutableStateOf(ShiftBlock.DAY) }
    var dayInBlock by remember { mutableIntStateOf(1) }
    val daysUntilFree = when (block) {
        ShiftBlock.DAY, ShiftBlock.NIGHT -> (5 - dayInBlock).coerceAtLeast(1)
        ShiftBlock.FREE_AFTER_DAY, ShiftBlock.FREE_AFTER_NIGHT -> 0
    }
    val suggested = LocalDate.now().plusDays(daysUntilFree.toLong())
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("Wann Kur legen?", "Das 4-4-4-System wird als Entscheidungshilfe abgebildet.") }
        item {
            Text("Aktueller Block", style = MaterialTheme.typography.titleLarge)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ShiftBlock.entries.forEach { option ->
                    FilterChip(selected = block == option, onClick = { block = option }, label = { Text(option.label) })
                }
            }
        }
        if (block == ShiftBlock.DAY || block == ShiftBlock.NIGHT) {
            item {
                Text("Tag $dayInBlock von 4", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { dayInBlock = (dayInBlock - 1).coerceAtLeast(1) }) { Text("−") }
                    OutlinedButton(onClick = { dayInBlock = (dayInBlock + 1).coerceAtMost(4) }) { Text("+") }
                }
            }
        }
        item {
            SignalCard(
                "Empfohlenes Kur-Fenster",
                if (daysUntilFree == 0) {
                    "Der Freiblock läuft. Frühester sinnvoller Start: heute, ${suggested.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}."
                } else {
                    "Nächster Freiblock ab ${suggested.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}. Dort sind nüchterner Morgen, Mahlzeiten und 4-Stunden-Fenster realistisch."
                },
                AllowedGreenContainer,
                AllowedGreen,
            )
        }
        item {
            SignalCard(
                "Tagdienst ist ungeeignet",
                "An Tagdienst-Tagen (04:00 auf, 04:30 los) ist die Kur nicht durchführbar.",
                WarningYellowContainer,
                WarningYellow,
            )
        }
        item { Text("Die Planung ist eine organisatorische Hilfe und keine medizinische Empfehlung.") }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SettingsContent(
    state: MainUiState,
    onSaveProtocol: (ProtocolTemplateEntity) -> Unit,
    onSaveIngredient: (IngredientEntity) -> Unit,
    onDeleteIngredient: (IngredientEntity) -> Unit,
    onSaveStack: (StackItemEntity) -> Unit,
    onDeleteStack: (StackItemEntity) -> Unit,
    modifier: Modifier,
) {
    val locked = state.activeCure != null
    var protocol by remember(state.protocol) { mutableStateOf(state.protocol) }
    var editedIngredient by remember { mutableStateOf<IngredientEntity?>(null) }
    var ingredientDialog by remember { mutableStateOf(false) }
    var editedStack by remember { mutableStateOf<StackItemEntity?>(null) }
    var stackDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Standard-Protokoll", "Änderungen gelten für neue Abläufe.") }
        if (locked) {
            item {
                SignalCard(
                    "Bearbeitung während der Kur gesperrt",
                    "So bleiben bereits geplante Alarme und die angezeigten Zeitfenster konsistent. Nach Kurabschluss kannst du die Vorlage für die nächste Kur ändern.",
                    WarningYellowContainer,
                    WarningYellow,
                )
            }
        }
        item {
            NumberSetting("Standarddauer (2–3 Tage)", protocol.standardDurationDays, !locked) {
                protocol = protocol.copy(standardDurationDays = it.coerceIn(2, 3))
            }
        }
        item {
            NumberSetting("Antioxidantien-Sperre (Minuten)", protocol.antioxidantBlockMinutes, !locked) {
                protocol = protocol.copy(antioxidantBlockMinutes = it.coerceAtLeast(1))
            }
        }
        item {
            NumberSetting("Mahlzeit bis Minute", protocol.mealDeadlineMinutes, !locked) {
                protocol = protocol.copy(mealDeadlineMinutes = it.coerceAtLeast(1))
            }
        }
        item {
            NumberSetting("Mahlzeit-Vorwarnung bei Minute", protocol.mealWarningMinutes, !locked) {
                protocol = protocol.copy(mealWarningMinutes = it.coerceAtLeast(0))
            }
        }
        item {
            NumberSetting("Spermidin-Fenster ab Minute", protocol.spermidinStartMinutes, !locked) {
                protocol = protocol.copy(spermidinStartMinutes = it.coerceAtLeast(1))
            }
        }
        item {
            NumberSetting("Spermidin-Nachfassung bei Minute", protocol.spermidinReminderMinutes, !locked) {
                protocol = protocol.copy(spermidinReminderMinutes = it.coerceAtLeast(1))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Erinnerungen", style = MaterialTheme.typography.titleMedium)
                    Text("Exakte Alarme für die Kur-Schritte", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = protocol.remindersEnabled, onCheckedChange = { protocol = protocol.copy(remindersEnabled = it) }, enabled = !locked)
            }
        }
        item { Button(onClick = { onSaveProtocol(protocol) }, enabled = !locked, modifier = Modifier.fillMaxWidth()) { Text("Protokollwerte speichern") } }
        item { Text("Zutaten", style = MaterialTheme.typography.headlineMedium) }
        items(state.ingredients, key = { ingredientListKey(it.id) }) { item ->
            EditableRow(
                title = "${item.name} · ${item.amount}",
                subtitle = "${if (item.phase == IngredientPhase.DRINK) "Drink" else "Spermidin"}${if (item.optional) " · optional" else ""}",
                onEdit = if (locked) null else ({ editedIngredient = item; ingredientDialog = true }),
                onDelete = if (locked) null else ({ onDeleteIngredient(item) }),
            )
        }
        item {
            OutlinedButton(onClick = { editedIngredient = null; ingredientDialog = true }, enabled = !locked, modifier = Modifier.fillMaxWidth()) { Text("Zutat hinzufügen") }
        }
        item { Text("Konflikt-Tabelle", style = MaterialTheme.typography.headlineMedium) }
        items(state.stackItems, key = { stackItemListKey(it.id) }) { item ->
            EditableRow(
                title = item.name,
                subtitle = "${categoryLabel(item.category)} · ${item.usualDose}",
                onEdit = if (locked) null else ({ editedStack = item; stackDialog = true }),
                onDelete = if (locked || item.isMedication) null else ({ onDeleteStack(item) }),
            )
        }
        item {
            OutlinedButton(onClick = { editedStack = null; stackDialog = true }, enabled = !locked, modifier = Modifier.fillMaxWidth()) { Text("Substanz hinzufügen") }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }

    if (ingredientDialog) {
        IngredientEditor(
            item = editedIngredient,
            nextSort = state.ingredients.size + 1,
            onDismiss = { ingredientDialog = false },
            onSave = { onSaveIngredient(it); ingredientDialog = false },
        )
    }
    if (stackDialog) {
        StackEditor(
            item = editedStack,
            nextSort = state.stackItems.size + 1,
            onDismiss = { stackDialog = false },
            onSave = { onSaveStack(it); stackDialog = false },
        )
    }
}

@Composable
private fun NumberSetting(label: String, value: Int, enabled: Boolean, onValue: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { it.toIntOrNull()?.let(onValue) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EditableRow(title: String, subtitle: String, onEdit: (() -> Unit)?, onDelete: (() -> Unit)?) {
    androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth().then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            onEdit?.let { TextButton(onClick = it) { Text("Ändern") } }
            onDelete?.let { TextButton(onClick = it) { Text("Löschen") } }
        }
    }
}

@Composable
private fun IngredientEditor(item: IngredientEntity?, nextSort: Int, onDismiss: () -> Unit, onSave: (IngredientEntity) -> Unit) {
    var name by remember(item) { mutableStateOf(item?.name.orEmpty()) }
    var amount by remember(item) { mutableStateOf(item?.amount.orEmpty()) }
    var note by remember(item) { mutableStateOf(item?.note.orEmpty()) }
    var phase by remember(item) { mutableStateOf(item?.phase ?: IngredientPhase.DRINK) }
    var optional by remember(item) { mutableStateOf(item?.optional ?: false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Zutat hinzufügen" else "Zutat bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amount, { amount = it }, label = { Text("Menge") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Hinweis") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IngredientPhase.entries.forEach { value -> FilterChip(selected = phase == value, onClick = { phase = value }, label = { Text(if (value == IngredientPhase.DRINK) "Drink" else "Spermidin") }) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(optional, { optional = it })
                    Text("Optional")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && amount.isNotBlank(),
                onClick = { onSave(IngredientEntity(item?.id ?: 0, phase, name.trim(), amount.trim(), note.trim(), optional, item?.sortOrder ?: nextSort)) },
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun StackEditor(item: StackItemEntity?, nextSort: Int, onDismiss: () -> Unit, onSave: (StackItemEntity) -> Unit) {
    var name by remember(item) { mutableStateOf(item?.name.orEmpty()) }
    var dose by remember(item) { mutableStateOf(item?.usualDose.orEmpty()) }
    var note by remember(item) { mutableStateOf(item?.note.orEmpty()) }
    var category by remember(item) { mutableStateOf(item?.category ?: StackCategory.UNPROBLEMATISCH) }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Substanz hinzufügen" else "Substanz bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), enabled = item?.isMedication != true)
                OutlinedTextField(dose, { dose = it }, label = { Text("Übliche Dosis") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Hinweis") }, modifier = Modifier.fillMaxWidth())
                Column {
                    OutlinedButton(onClick = { expanded = true }, enabled = item?.isMedication != true) { Text(categoryLabel(category)) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        StackCategory.entries.forEach { value ->
                            DropdownMenuItem(text = { Text(categoryLabel(value)) }, onClick = { category = value; expanded = false })
                        }
                    }
                }
                if (item?.isMedication == true) Text("Medikamente bleiben immer als unproblematisch gekennzeichnet.", color = AllowedGreen)
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && dose.isNotBlank(),
                onClick = {
                    onSave(
                        StackItemEntity(
                            id = item?.id ?: 0,
                            name = name.trim(),
                            usualDose = dose.trim(),
                            category = if (item?.isMedication == true) StackCategory.UNPROBLEMATISCH else category,
                            isMedication = item?.isMedication ?: false,
                            note = note.trim(),
                            sortOrder = item?.sortOrder ?: nextSort,
                        ),
                    )
                },
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

private fun categoryLabel(category: StackCategory): String = when (category) {
    StackCategory.HARTE_SPERRE -> "Harte Sperre"
    StackCategory.GRENZFALL -> "Grenzfall"
    StackCategory.UNPROBLEMATISCH -> "Unproblematisch"
}

internal fun ingredientListKey(id: Long): String = "ingredient:$id"

internal fun stackItemListKey(id: Long): String = "stack:$id"

@Composable
private fun SafetyContent(modifier: Modifier) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("Sicherheit & Haftung") }
        item {
            SignalCard(
                "Keine medizinische Beratung",
                "Diese App bildet nur dein persönliches, selbst festgelegtes Protokoll ab. Bei Unsicherheiten oder Wechselwirkungen ärztlichen Rat einholen.",
                WarningYellowContainer,
                WarningYellow,
            )
        }
        item {
            SignalCard(
                "Venlafaxin nicht aussetzen",
                "Dein Medikament wird durch die Kur nicht verändert und nie ausgesetzt.",
                AllowedGreenContainer,
                AllowedGreen,
            )
        }
        item { Text("Keine Cloud, kein Nutzerkonto, keine Internetverbindung. Protokoll- und Verlaufsdaten bleiben lokal auf dem Gerät.") }
        item { Text("App-Version ${BuildConfig.VERSION_NAME} · Stand ${BuildConfig.VERSION_BUMPED_AT}", style = MaterialTheme.typography.bodySmall) }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

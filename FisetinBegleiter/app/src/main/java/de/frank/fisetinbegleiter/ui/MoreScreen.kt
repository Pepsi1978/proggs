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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import de.frank.fisetinbegleiter.ui.theme.WarningYellow
import de.frank.fisetinbegleiter.ui.theme.WarningYellowContainer

@Composable
fun MoreScreen(
    state: MainUiState,
    onSaveProtocol: (ProtocolTemplateEntity) -> Unit,
    onSaveIngredient: (IngredientEntity) -> Unit,
    onDeleteIngredient: (IngredientEntity) -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsContent(
        state,
        onSaveProtocol,
        onSaveIngredient,
        onDeleteIngredient,
        onOpenBatterySettings,
        modifier.padding(horizontal = 20.dp),
    )
}

@Composable
private fun SettingsContent(
    state: MainUiState,
    onSaveProtocol: (ProtocolTemplateEntity) -> Unit,
    onSaveIngredient: (IngredientEntity) -> Unit,
    onDeleteIngredient: (IngredientEntity) -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier,
) {
    val locked = state.activeCure != null
    var protocol by remember(state.protocol) { mutableStateOf(state.protocol) }
    var editedIngredient by remember { mutableStateOf<IngredientEntity?>(null) }
    var ingredientDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("System", "Berechtigungen und Zuverlässigkeit der App.") }
        item {
            OutlinedButton(onClick = onOpenBatterySettings, modifier = Modifier.fillMaxWidth()) {
                Text("Akku-Optimierung für zuverlässige Erinnerungen prüfen")
            }
        }
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
        item { Text("App-Version ${BuildConfig.VERSION_NAME} · Stand ${BuildConfig.VERSION_BUMPED_AT}", style = MaterialTheme.typography.bodySmall) }
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

internal fun ingredientListKey(id: Long): String = "ingredient:$id"

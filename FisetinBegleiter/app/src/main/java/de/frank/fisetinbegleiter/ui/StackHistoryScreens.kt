package de.frank.fisetinbegleiter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.frank.fisetinbegleiter.data.CureStatus
import de.frank.fisetinbegleiter.data.CureWithDays
import de.frank.fisetinbegleiter.data.StackCategory
import de.frank.fisetinbegleiter.data.StackItemEntity
import de.frank.fisetinbegleiter.ui.theme.AllowedGreen
import de.frank.fisetinbegleiter.ui.theme.AllowedGreenContainer
import de.frank.fisetinbegleiter.ui.theme.BlockedRed
import de.frank.fisetinbegleiter.ui.theme.BlockedRedContainer
import de.frank.fisetinbegleiter.ui.theme.WarningYellow
import de.frank.fisetinbegleiter.ui.theme.WarningYellowContainer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StackScreen(state: MainUiState, modifier: Modifier = Modifier) {
    val hardBlocked = state.blockActive
    LazyColumn(modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionTitle(
                "Angepasster Morgen-Stack",
                if (hardBlocked) "Für heute während des aktiven Sperrfensters." else "Aktuell besteht keine Antioxidantien-Sperre.",
            )
        }
        if (hardBlocked) {
            item {
                SignalCard(
                    "Freigabe um ${formatTime(state.blockEnd!!)}",
                    "Bis dahin rote Substanzen weglassen. Gelbe Grenzfälle im Zweifel ebenfalls verschieben.",
                    WarningYellowContainer,
                    WarningYellow,
                )
            }
        }
        StackCategory.entries.forEach { category ->
            val categoryItems = state.stackItems.filter { it.category == category }
            if (categoryItems.isNotEmpty()) {
                item {
                    Text(
                        when (category) {
                            StackCategory.HARTE_SPERRE -> if (hardBlocked) "Harte Sperre" else "Jetzt wieder erlaubt"
                            StackCategory.GRENZFALL -> "Grenzfälle"
                            StackCategory.UNPROBLEMATISCH -> "Unproblematisch"
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                items(categoryItems, key = { it.id }) { item -> StackItemCard(item, hardBlocked) }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun StackItemCard(item: StackItemEntity, hardBlocked: Boolean) {
    val blocked = item.category == StackCategory.HARTE_SPERRE && hardBlocked
    val caution = item.category == StackCategory.GRENZFALL && hardBlocked
    val container = when {
        blocked -> BlockedRedContainer
        caution -> WarningYellowContainer
        else -> AllowedGreenContainer
    }
    val content = when {
        blocked -> BlockedRed
        caution -> WarningYellow
        else -> AllowedGreen
    }
    Card(colors = CardDefaults.cardColors(containerColor = container, contentColor = content)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textDecoration = if (blocked) TextDecoration.LineThrough else null,
            )
            Text(item.usualDose)
            val status = when {
                item.isMedication -> "Medikament – immer einnehmen, nie wegen der Kur weglassen."
                blocked -> "Während des Sperrfensters weglassen."
                caution -> "Im Zweifel hinter das 4-Stunden-Fenster verschieben."
                item.category == StackCategory.HARTE_SPERRE -> "Sperrfenster vorbei – jetzt nachholbar."
                else -> "Normal einnehmen."
            }
            Text(if (blocked) "Während des Sperrfensters weglassen; Freigabezeit siehe oben." else status, fontWeight = FontWeight.SemiBold)
            if (item.note.isNotBlank()) Text(item.note, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun HistoryScreen(
    state: MainUiState,
    onUpdateNote: (CureWithDays, String) -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("Protokoll", "Alle Daten bleiben lokal auf diesem Gerät.") }
        item {
            OutlinedButton(onClick = onExport, enabled = state.history.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("Als Textdatei exportieren")
            }
        }
        if (state.history.isEmpty()) {
            item { Text("Noch keine Kur gespeichert.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(state.history, key = { it.cure.id }) { cure ->
            HistoryCard(cure, onUpdateNote)
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun HistoryCard(cure: CureWithDays, onUpdateNote: (CureWithDays, String) -> Unit) {
    var note by remember(cure.cure.id, cure.cure.note) { mutableStateOf(cure.cure.note) }
    val doneSteps = cure.days.sumOf { day ->
        listOf(day.drinkDoneAt, day.mealDoneAt, day.spermidinDoneAt, day.blockEndedAt).count { it != null }
    }
    val allSteps = cure.days.size * 4
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    LocalDate.ofEpochDay(cure.cure.startEpochDay).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(if (cure.cure.status == CureStatus.ABGESCHLOSSEN) "Abgeschlossen" else "Aktiv", color = if (cure.cure.status == CureStatus.ABGESCHLOSSEN) AllowedGreen else WarningYellow)
            }
            Text("${cure.cure.plannedDurationDays} Tage · $doneSteps von $allSteps Schritten dokumentiert")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Notiz, Verträglichkeit, Beobachtungen") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { onUpdateNote(cure, note) }, enabled = note != cure.cure.note) { Text("Notiz speichern") }
        }
    }
}

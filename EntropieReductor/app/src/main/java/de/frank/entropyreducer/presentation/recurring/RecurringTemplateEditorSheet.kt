package de.frank.entropyreducer.presentation.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.label

/**
 * Bottom-Sheet-Editor fuer Vorlagen wiederkehrender Aufgaben (Sprint 2.6, Frank-Wunsch 2026-05-22).
 * Wird sowohl fuer Anlegen (initial = null) als auch Bearbeiten (initial != null) verwendet.
 *
 * Loop-Vorlagen werden nicht mehr automatisch geplant. Der Editor speichert nur noch den Inhalt;
 * die eigentliche Aufgabe entsteht spaeter manuell per "Hinzufügen".
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)
@Composable
fun RecurringTemplateEditorSheet(
    initial: RecurringTemplateEntity?,
    onDismiss: () -> Unit,
    onSave: (RecurringTemplateEntity) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cosmos = LocalCosmos.current

    // Initialwerte aus existierendem Eintrag oder Defaults fuer neuen Eintrag.
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: EntropyCategory.SONSTIGES) }
    var severity by remember { mutableStateOf((initial?.severity ?: 5).toFloat()) }
    var durationText by remember {
        mutableStateOf(initial?.estimatedDurationMinutes?.toString().orEmpty())
    }

    val canSave = title.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (cosmos.isDark) Color(0xFF26211B) else Color.White,
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (initial == null) "Neue Vorlage" else "Vorlage bearbeiten",
                    style = MaterialTheme.typography.titleLarge,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Schließen",
                        tint = cosmos.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Titel
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titel") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            // Beschreibung
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Beschreibung (optional)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            // Kategorie
            SectionTitle("Kategorie")
            CategoryDropdown(selected = category, onSelect = { category = it })
            Spacer(Modifier.height(16.dp))

            // Schweregrad-Slider (1..10)
            SectionTitle("Schweregrad: ${severity.toInt()}")
            Slider(
                value = severity,
                onValueChange = { severity = it },
                valueRange = 1f..10f,
                steps = 8,
            )
            Spacer(Modifier.height(16.dp))

            // Dauer (optional)
            OutlinedTextField(
                value = durationText,
                onValueChange = { new -> durationText = new.filter { it.isDigit() }.take(4) },
                label = { Text("Geschätzte Dauer in Minuten (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            // Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        val template =
                            (initial
                                    ?: RecurringTemplateEntity(
                                        id = "",
                                        title = title,
                                        rrule = "FREQ=DAILY",
                                    ))
                                .copy(
                                    title = title.trim(),
                                    description = description.trim().ifBlank { null },
                                    category = category,
                                    priorityScore = initial?.priorityScore ?: 50,
                                    severity = severity.toInt(),
                                    estimatedDurationMinutes = durationText.toIntOrNull(),
                                    rrule = initial?.rrule ?: "FREQ=DAILY",
                                    timeOfDayMinutes = initial?.timeOfDayMinutes ?: 480,
                                    updatedAt = now,
                                )
                        onSave(template)
                    },
                    enabled = canSave,
                ) {
                    Text("Speichern")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val cosmos = LocalCosmos.current
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = cosmos.textPrimary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CategoryDropdown(selected: EntropyCategory, onSelect: (EntropyCategory) -> Unit) {
    // Kompakter Chip-Wrap statt Dropdown — alle Kategorien direkt sichtbar fuer schnellere Auswahl.
    val cosmos = LocalCosmos.current
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EntropyCategory.values().forEach { cat ->
            val isSel = cat == selected
            Box(
                modifier =
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (isSel) LocalCosmos.current.accent else cosmos.glassBg)
                        .clickable { onSelect(cat) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = cat.label(),
                    color = if (isSel) Color.White else cosmos.textPrimary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun FreqTabs(selected: String, onSelect: (String) -> Unit) {
    val cosmos = LocalCosmos.current
    val tabs = listOf("DAILY" to "Täglich", "WEEKLY" to "Wöchentlich", "MONTHLY" to "Monatlich")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEach { (key, label) ->
            val isSel = selected == key
            Box(
                modifier =
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) LocalCosmos.current.accent else cosmos.glassBg)
                        .clickable { onSelect(key) }
                        .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isSel) Color.White else cosmos.textPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun IntervalStepper(interval: Int, unitLabel: String, onChange: (Int) -> Unit) {
    val cosmos = LocalCosmos.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Jede(s)", style = MaterialTheme.typography.bodyMedium, color = cosmos.textSecondary)
        Spacer(Modifier.width(12.dp))
        StepperButton(label = "–", onClick = { onChange(interval - 1) })
        Spacer(Modifier.width(12.dp))
        Text(
            text = interval.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.width(12.dp))
        StepperButton(label = "+", onClick = { onChange(interval + 1) })
        Spacer(Modifier.width(12.dp))
        Text(unitLabel, style = MaterialTheme.typography.bodyMedium, color = cosmos.textSecondary)
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        modifier =
            Modifier.size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LocalCosmos.current.accent.copy(alpha = 0.18f))
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = LocalCosmos.current.accent, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WeekdayPicker(selected: Set<String>, onChange: (Set<String>) -> Unit) {
    val cosmos = LocalCosmos.current
    val days =
        listOf(
            "MO" to "Mo",
            "TU" to "Di",
            "WE" to "Mi",
            "TH" to "Do",
            "FR" to "Fr",
            "SA" to "Sa",
            "SU" to "So",
        )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { (key, label) ->
            val isSel = key in selected
            Box(
                modifier =
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) LocalCosmos.current.accent else cosmos.glassBg)
                        .clickable { onChange(if (isSel) selected - key else selected + key) }
                        .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isSel) Color.White else cosmos.textPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun MonthDayPicker(day: Int, onChange: (Int) -> Unit) {
    val cosmos = LocalCosmos.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton(label = "–", onClick = { onChange(day - 1) })
        Spacer(Modifier.width(12.dp))
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.width(12.dp))
        StepperButton(label = "+", onClick = { onChange(day + 1) })
        Spacer(Modifier.width(12.dp))
        Text(".", style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary)
    }
}

@Composable
private fun TimePicker(minutes: Int, onChange: (Int) -> Unit) {
    val cosmos = LocalCosmos.current
    val hours = minutes / 60
    val mins = minutes % 60
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton(label = "–", onClick = { onChange(((hours - 1).mod(24)) * 60 + mins) })
        Spacer(Modifier.width(8.dp))
        Text(
            text = "%02d".format(hours),
            style = MaterialTheme.typography.titleMedium,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.width(4.dp))
        StepperButton(label = "+", onClick = { onChange(((hours + 1).mod(24)) * 60 + mins) })
        Spacer(Modifier.width(16.dp))
        Text(":", style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary)
        Spacer(Modifier.width(16.dp))
        StepperButton(label = "–", onClick = { onChange(hours * 60 + ((mins - 5).mod(60))) })
        Spacer(Modifier.width(8.dp))
        Text(
            text = "%02d".format(mins),
            style = MaterialTheme.typography.titleMedium,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.width(4.dp))
        StepperButton(label = "+", onClick = { onChange(hours * 60 + ((mins + 5).mod(60))) })
    }
}

// =====================================================================
//  RRULE-Helpers (rein lokal — keine lib-recur-Abhaengigkeit nur fuer Edit)
// =====================================================================

private data class ParsedRule(
    val freq: String,
    val interval: Int,
    val weekdays: Set<String>,
    val monthDay: Int,
)

private fun parseRule(rrule: String): ParsedRule {
    val parts =
        rrule
            .split(";")
            .mapNotNull { it.split("=", limit = 2).takeIf { kv -> kv.size == 2 } }
            .associate { it[0].uppercase() to it[1] }
    return ParsedRule(
        freq = (parts["FREQ"] ?: "DAILY").uppercase(),
        interval = parts["INTERVAL"]?.toIntOrNull() ?: 1,
        weekdays =
            parts["BYDAY"]
                ?.split(",")
                ?.mapNotNull { it.takeLast(2).uppercase().takeIf(::isWeekday) }
                ?.toSet() ?: emptySet(),
        monthDay = parts["BYMONTHDAY"]?.toIntOrNull() ?: 1,
    )
}

private fun isWeekday(s: String): Boolean = s in setOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")

private fun buildRule(freq: String, interval: Int, weekdays: Set<String>, monthDay: Int): String {
    val sb = StringBuilder("FREQ=").append(freq)
    if (interval > 1) sb.append(";INTERVAL=").append(interval)
    if (freq == "WEEKLY" && weekdays.isNotEmpty()) {
        // Kanonische Reihenfolge MO..SU
        val order = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
        sb.append(";BYDAY=").append(order.filter { it in weekdays }.joinToString(","))
    }
    if (freq == "MONTHLY") {
        sb.append(";BYMONTHDAY=").append(monthDay)
    }
    return sb.toString()
}

private fun formatTime(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

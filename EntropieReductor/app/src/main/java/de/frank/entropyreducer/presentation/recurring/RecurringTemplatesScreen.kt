package de.frank.entropyreducer.presentation.recurring

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Verwaltung wiederkehrender Aufgaben-Vorlagen (Sprint 2, Frank-Wunsch 2026-05-22).
 *
 * Responsive Layout:
 *  - screenWidthDp < 600dp (Smartphone): LazyColumn mit Karten 1-spaltig
 *  - screenWidthDp >= 600dp (Tablet/Fold ausgeklappt): LazyVerticalGrid 2-spaltig
 */
@Composable
fun RecurringTemplatesScreen(
    viewModel: RecurringTemplatesViewModel = hiltViewModel(),
) {
    val templates by viewModel.templates.collectAsState()
    val cosmos = LocalCosmos.current

    var editingTemplate: RecurringTemplateEntity? by remember { mutableStateOf(null) }
    var showCreator by remember { mutableStateOf(false) }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val useGrid = screenWidthDp >= 600

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Wiederkehrende Aufgaben",
                style = MaterialTheme.typography.headlineMedium,
                color = cosmos.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Vorlagen die automatisch zu Aufgaben werden — täglich, wöchentlich oder nach RRULE-Regel.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(16.dp))

            if (templates.isEmpty()) {
                EmptyState(onCreate = { showCreator = true })
            } else if (useGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(templates) { t ->
                        TemplateCard(
                            template = t,
                            onToggle = { viewModel.toggleActive(t) },
                            onDelete = { viewModel.delete(t) },
                            onClick = { editingTemplate = t },
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(templates) { t ->
                        TemplateCard(
                            template = t,
                            onToggle = { viewModel.toggleActive(t) },
                            onDelete = { viewModel.delete(t) },
                            onClick = { editingTemplate = t },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreator = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = CosmosColors.AccentPrimary,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Neue Vorlage")
        }
    }

    if (showCreator || editingTemplate != null) {
        RecurringTemplateEditorSheet(
            initial = editingTemplate,
            onDismiss = {
                showCreator = false
                editingTemplate = null
            },
            onSave = { newTemplate ->
                viewModel.save(newTemplate)
                showCreator = false
                editingTemplate = null
            },
        )
    }
}

// Inline-Extension fuer LazyColumn-/LazyVerticalGrid-Items
private inline fun <T> androidx.compose.foundation.lazy.LazyListScope.items(
    list: List<T>,
    crossinline content: @Composable (T) -> Unit,
) {
    items(list.size) { index -> content(list[index]) }
}

private inline fun <T> androidx.compose.foundation.lazy.grid.LazyGridScope.items(
    list: List<T>,
    crossinline content: @Composable (T) -> Unit,
) {
    items(list.size) { index -> content(list[index]) }
}

@Composable
private fun TemplateCard(
    template: RecurringTemplateEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        contentPadding = 16.dp,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Repeat,
                    contentDescription = null,
                    tint = if (template.isActive) CosmosColors.AccentPrimary else cosmos.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = template.isActive,
                    onCheckedChange = { onToggle() },
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = humanReadable(template.rrule),
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )

            if (!template.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (template.nextOccurrenceAt != null) {
                    val df = remember {
                        java.text.SimpleDateFormat("EEE, dd.MM. HH:mm", java.util.Locale.GERMAN)
                    }
                    Text(
                        text = "Nächste: ${df.format(java.util.Date(template.nextOccurrenceAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${template.occurrenceCount}×",
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Löschen",
                        tint = CosmosColors.Critical,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    val cosmos = LocalCosmos.current
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Repeat,
                    contentDescription = null,
                    tint = CosmosColors.AccentSecondary,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Noch keine wiederkehrenden Aufgaben",
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tippe auf das Plus um eine Vorlage anzulegen — täglich, wöchentlich, monatlich oder mit eigener Regel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            }
        }
    }
}

/**
 * Wandelt eine RRULE in einen lesbaren deutschen Text um.
 * Beispiele:
 *  - "FREQ=DAILY"                              → "Täglich"
 *  - "FREQ=WEEKLY;BYDAY=MO,WE,FR"              → "Wöchentlich: Mo, Mi, Fr"
 *  - "FREQ=MONTHLY;BYMONTHDAY=1"               → "Monatlich am 1."
 *  - "FREQ=WEEKLY;INTERVAL=2"                  → "Alle 2 Wochen"
 *  - unbekannte Regel                          → die Original-Regel
 */
internal fun humanReadable(rrule: String): String {
    val parts = rrule.split(";")
        .mapNotNull { part ->
            val kv = part.split("=", limit = 2)
            if (kv.size == 2) kv[0].uppercase() to kv[1] else null
        }
        .toMap()

    val freq = parts["FREQ"] ?: return rrule
    val interval = parts["INTERVAL"]?.toIntOrNull() ?: 1
    val byDay = parts["BYDAY"]?.split(",")
    val byMonthDay = parts["BYMONTHDAY"]

    val dayNamesShort = mapOf(
        "MO" to "Mo", "TU" to "Di", "WE" to "Mi",
        "TH" to "Do", "FR" to "Fr", "SA" to "Sa", "SU" to "So",
    )

    return when (freq) {
        "DAILY" -> if (interval == 1) "Täglich" else "Alle $interval Tage"
        "WEEKLY" -> {
            val days = byDay?.mapNotNull { dayNamesShort[it.takeLast(2)] }?.joinToString(", ")
            val base = if (interval == 1) "Wöchentlich" else "Alle $interval Wochen"
            if (days.isNullOrBlank()) base else "$base: $days"
        }
        "MONTHLY" -> {
            val base = if (interval == 1) "Monatlich" else "Alle $interval Monate"
            if (byMonthDay != null) "$base am $byMonthDay." else base
        }
        "YEARLY" -> if (interval == 1) "Jährlich" else "Alle $interval Jahre"
        else -> rrule
    }
}

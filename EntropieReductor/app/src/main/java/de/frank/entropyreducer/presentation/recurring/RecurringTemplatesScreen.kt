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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.EntropyCategoryPill
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.MicCaptureActions
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.dashboard1.CategoryIconCircle
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.navigation.Routes
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.color

/**
 * Loop-Reiter (Frank-Wunsch 2026-05-22 Phase 2):
 *  - Aufgaben sehen 1:1 wie im Aufgaben-Reiter aus (GlassCard mit
 *    CategoryIconCircle + Kategorie-Pill + Titel + Beschreibung + Prio-Zahl).
 *  - Checkbox LINKS vor jeder Karte (statt Switch rechts).
 *  - Beim Aktivieren der Checkbox wird sofort eine Aufgabe in den Aufgaben-
 *    Reiter uebernommen (ueber GenerateRecurringInstancesUseCase im
 *    ViewModel — lastGeneratedAt=0 erzwingt die naechste Generierung).
 *  - Solange die Checkbox aktiv ist, wird nach Abschluss der Aufgabe die
 *    naechste Instanz nach RRULE erzeugt.
 *  - Mic ueber die BottomBar oeffnet die einheitlichen MicCaptureActions
 *    in Orange (Aufgaben-Sub-Akzent).
 *  - BottomBar bleibt sichtbar (forcedSubMode=Routes.TASKS).
 */
@Composable
fun RecurringTemplatesScreen(
    onSwitchTab: (String) -> Unit = {},
    onSwitchSub: (parentTab: String, index: Int) -> Unit = { _, _ -> },
    viewModel: RecurringTemplatesViewModel = hiltViewModel(),
) {
    val templates by viewModel.templates.collectAsState()
    val cosmos = LocalCosmos.current

    var micActionsOpen by remember { mutableStateOf(false) }

    CosmosScaffold(
        title = "Loop",
        bottomBar = {
            CosmosBottomBar(
                currentTab = Routes.TASKS,
                micState = MicState.IDLE,
                onTabSelected = onSwitchTab,
                onMicClick = { micActionsOpen = !micActionsOpen },
                onSubAreaSelected = onSwitchSub,
                forcedSubMode = Routes.TASKS,
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (templates.isEmpty()) {
                EmptyHint(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 120.dp,
                    ),
                ) {
                    items(templates) { t ->
                        TemplateAsTaskCard(
                            template = t,
                            onToggleActive = { viewModel.toggleActive(t) },
                            onDelete = { viewModel.delete(t) },
                        )
                    }
                }
            }

            // Frank-Wunsch 2026-05-22 Phase 2: Mic in Orange (Aufgaben-Sub).
            MicCaptureActions(
                visible = micActionsOpen,
                accent = Color(0xFFEA580C),
                onTextCommit = { text, source ->
                    viewModel.createFromText(text = text, source = source)
                },
                onClose = { micActionsOpen = false },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun EmptyHint(modifier: Modifier = Modifier) {
    val cosmos = LocalCosmos.current
    Column(
        modifier = modifier.padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Repeat,
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
        Spacer(Modifier.height(6.dp))
        Text(
            "Tippe auf das Mikrofon und waehle „Schreiben“ oder „Aufnehmen“.",
            style = MaterialTheme.typography.bodySmall,
            color = cosmos.textSecondary,
        )
    }
}

/**
 * Frank-Wunsch 2026-05-22 Phase 2 (Aufgabe 3 + 5): Loop-Pattern-Karte sieht
 * 1:1 wie die EntropyEntryCard im Aufgaben-Reiter aus — gleicher GlassCard,
 * CategoryIconCircle links, Kategorie-Pill, Titel, Beschreibung, grosse Prio-
 * Zahl rechts. Davor eine Checkbox; aktiv heisst "wird als Aufgabe uebernommen
 * und solange aktiv nach jedem Abschluss neu erstellt".
 *
 * Die RRULE-Beschreibung (Taeglich, Woechentlich …) erscheint als zweite Zeile
 * unter der Beschreibung, damit Frank auf einen Blick sieht wann der Eintrag
 * fällig wird.
 */
// Frank-Wunsch 2026-05-24: internal statt private, damit der Aufgaben-Reiter
// (TasksScreen) die Loop-Karten 1:1 als Akkordeon-Dropdown wiederverwenden kann.
@Composable
internal fun TemplateAsTaskCard(
    template: RecurringTemplateEntity,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val catColor = template.category.color()
    val tint = priorityCardTint(template.priorityScore.toDouble(), cosmos.isDark)

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleActive),
        tintColor = tint,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // Checkbox links (Frank-Wunsch 2026-05-22: statt Switch rechts).
            Checkbox(
                checked = template.isActive,
                onCheckedChange = { onToggleActive() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFFEA580C),
                    uncheckedColor = cosmos.textSecondary,
                ),
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.width(4.dp))
            CategoryIconCircle(category = template.category, tint = catColor)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EntropyCategoryPill(template.category)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                )
                if (!template.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = template.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = cosmos.textSecondary,
                        maxLines = 2,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = humanReadable(template.rrule),
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${template.priorityScore}",
                    color = priorityColor(template.priorityScore.toDouble()),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Prio",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.height(4.dp))
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

/** Identisch zur Skala in TasksScreen damit die grosse Prio-Zahl die gleiche Farbe trifft. */
private fun priorityColor(score: Double): Color =
    when {
        score >= 80.0 -> CosmosColors.PriorityRed
        score >= 60.0 -> CosmosColors.PriorityOrange
        score >= 40.0 -> CosmosColors.PriorityYellow
        score >= 20.0 -> CosmosColors.PriorityGreen
        else -> CosmosColors.PriorityBlue
    }

/** Karten-Hintergrund-Tint identisch zur EntropyEntryCard-Logik. */
private fun priorityCardTint(score: Double, isDark: Boolean): Color {
    val base = when {
        score >= 80.0 -> CosmosColors.PriorityRed
        score >= 60.0 -> CosmosColors.PriorityOrange
        score >= 40.0 -> CosmosColors.PriorityYellow
        score >= 20.0 -> CosmosColors.PriorityGreen
        else -> CosmosColors.PriorityBlue
    }
    return base.copy(alpha = if (isDark) 0.14f else 0.18f)
}

/** Inline-Items-Helper fuer LazyColumn. */
private inline fun <T> androidx.compose.foundation.lazy.LazyListScope.items(
    list: List<T>,
    crossinline content: @Composable (T) -> Unit,
) {
    items(list.size) { index -> content(list[index]) }
}

/**
 * RRULE → deutscher Klartext. Beispiele:
 * - "FREQ=DAILY"                   → "Täglich"
 * - "FREQ=WEEKLY;BYDAY=MO,WE,FR"   → "Wöchentlich: Mo, Mi, Fr"
 * - "FREQ=MONTHLY;BYMONTHDAY=1"    → "Monatlich am 1."
 * - unbekannte Regel               → die Original-Regel
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

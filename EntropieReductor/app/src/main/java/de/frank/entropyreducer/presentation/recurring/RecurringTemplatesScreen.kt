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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Repeat
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.MicCaptureActions
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.navigation.Routes
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Loop-Reiter (Frank-Wunsch 2026-05-22 abend): einfache Liste der wiederkehrenden
 * Aufgaben. Der Mic-Button in der BottomBar oeffnet jetzt — wie ueberall in der
 * App — die einheitlichen MicCaptureActions (zwei runde Buttons "Schreiben"
 * und "Aufnehmen" in Orange), genau wie der Entropie-Reiter (Frank-Wunsch
 * 2026-05-22, dritte Iteration).
 *
 * BottomBar ist jetzt explizit eingebettet (Frank-Wunsch 2026-05-22): vorher
 * hatte der Screen kein eigenes Scaffold und die Bar verschwand beim Wechsel
 * nach Loop. Jetzt rendert er CosmosScaffold + CosmosBottomBar selbst mit
 * forcedSubMode=Routes.TASKS, damit die Bar in Orange leuchtet.
 *
 * Tap auf Karte: aktiv/pausiert toggeln (Switch).
 * Mülltonne pro Karte: loescht die Vorlage.
 */
@Composable
fun RecurringTemplatesScreen(
    onSwitchTab: (String) -> Unit = {},
    onSwitchSub: (parentTab: String, index: Int) -> Unit = { _, _ -> },
    viewModel: RecurringTemplatesViewModel = hiltViewModel(),
) {
    val templates by viewModel.templates.collectAsState()
    val cosmos = LocalCosmos.current

    // Frank-Wunsch 2026-05-22 (dritte Iteration): kein eckiges Sheet mehr.
    // Mic-Tap oeffnet die einheitlichen MicCaptureActions.
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 120.dp,
                    ),
                ) {
                    items(templates) { t ->
                        TemplateCard(
                            template = t,
                            onToggle = { viewModel.toggleActive(t) },
                            onDelete = { viewModel.delete(t) },
                        )
                    }
                }
            }

            // Frank-Wunsch 2026-05-22: einheitliche Mic-Aktion in Orange
            // (Aufgaben-Akzent — Loop ist Sub-Bereich des Aufgaben-Tabs).
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

@Composable
private fun TemplateCard(
    template: RecurringTemplateEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = if (template.isActive) CosmosColors.AccentPrimary else cosmos.textSecondary

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        contentPadding = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Repeat,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = humanReadable(template.rrule),
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            }
            Switch(checked = template.isActive, onCheckedChange = { onToggle() })
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

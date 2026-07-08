package de.frank.entropyreducer.presentation.recurring

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.MicCaptureActions
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.navigation.Routes
import de.frank.entropyreducer.presentation.priorityRampColor
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.color

/**
 * Loop-Reiter (Frank-Wunsch 2026-05-22 Phase 2):
 * - Aufgaben sehen 1:1 wie im Aufgaben-Reiter aus (GlassCard mit CategoryIconCircle +
 *   Kategorie-Pill + Titel + Beschreibung + Prio-Zahl).
 * - Loop-Vorlagen werden nur noch manuell per Button in die Aufgabenliste uebernommen.
 * - Mic ueber die BottomBar oeffnet die einheitlichen MicCaptureActions in Orange
 *   (Aufgaben-Sub-Akzent).
 * - BottomBar bleibt sichtbar (forcedSubMode=Routes.TASKS).
 */
@Composable
fun RecurringTemplatesScreen(
    onSwitchTab: (String) -> Unit = {},
    onSwitchSub: (parentTab: String, index: Int) -> Unit = { _, _ -> },
    viewModel: RecurringTemplatesViewModel = hiltViewModel(),
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
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
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 120.dp,
                        ),
                ) {
                    items(templates) { t ->
                        TemplateAsTaskCard(
                            template = t,
                            onDelete = { viewModel.delete(t) },
                            onAddToTasks = { bucket -> viewModel.addToTasks(t, bucket) },
                        )
                    }
                }
            }

            // Frank-Wunsch 2026-05-22 Phase 2: Mic in Orange (Aufgaben-Sub).
            MicCaptureActions(
                visible = micActionsOpen,
                accent = LocalCosmos.current.accentTasks,
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
            tint = LocalCosmos.current.accentForscher,
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
 * Frank-Wunsch 2026-05-22 Phase 2 (Aufgabe 3 + 5): Loop-Pattern-Karte sieht 1:1 wie die
 * EntropyEntryCard im Aufgaben-Reiter aus — gleicher GlassCard, CategoryIconCircle links,
 * Kategorie-Pill, Titel, Beschreibung, grosse Prio-Zahl rechts. Eine Vorlage wird nicht mehr
 * automatisch erzeugt, sondern nur noch ueber den Button "Hinzufügen" manuell kopiert.
 */
// Frank-Wunsch 2026-05-24: internal statt private, damit der Aufgaben-Reiter
// (TasksScreen) die Loop-Karten 1:1 als Akkordeon-Dropdown wiederverwenden kann.
// Aktuell: kein Aktiv-Schalter, kein Slider, kein Intervall. Die Vorlage wird nur ueber
// "Hinzufügen" manuell als Aufgabe kopiert.
@Composable
internal fun TemplateAsTaskCard(
    template: RecurringTemplateEntity,
    onDelete: () -> Unit,
    onAddToTasks: (TimeBucket?) -> Unit = {},
    // Klick auf die Karte oeffnet den Loop-Detail-Screen fuer Titel/Beschreibung.
    onOpenDetail: () -> Unit = {},
) {
    val cosmos = LocalCosmos.current
    val context = LocalContext.current

    var addMenuOpen by remember(template.id) { mutableStateOf(false) }

    val effectivePriority = template.priorityScore.toDouble()
    val ramp = priorityRampColor(effectivePriority)
    val priorityBrush =
        remember(ramp) { Brush.horizontalGradient(colors = listOf(ramp.copy(alpha = 0.20f), ramp)) }

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDetail),
        tintBrush = priorityBrush,
    ) {
        Column {
            // ZEILE 1: Titel + Loeschen. Kein Aktiv-/Pausiert-Schalter mehr.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Frank-Wunsch 2026-06-01: Tap auf den Titel oeffnet den Loop-Detail-Screen.
                    modifier = Modifier.weight(1f).clickable(onClick = onOpenDetail),
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Löschen",
                        tint = LocalCosmos.current.crit,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            // ZEILE 2: Nur noch manuelles Hinzufügen.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Box {
                    Button(
                        onClick = { addMenuOpen = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text("Hinzufügen")
                    }
                    DropdownMenu(
                        expanded = addMenuOpen,
                        onDismissRequest = { addMenuOpen = false },
                    ) {
                        loopAddChoices.forEach { (label, bucket) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onAddToTasks(bucket)
                                    android.widget.Toast.makeText(
                                        context,
                                        "Aufgabe hinzugefügt",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                    addMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Die fünf wählbaren Prioritätsbereiche für eine Loop-Vorlage. */
internal val loopBucketOptions =
    listOf(TimeBucket.HEUTE, TimeBucket.MORGEN, TimeBucket.FREIBLOCK, TimeBucket.GERING, TimeBucket.SPAETER)

internal val loopAddChoices: List<Pair<String, TimeBucket?>> =
    listOf("KI nach Priorität" to null) + loopBucketOptions.map { loopBucketLabel(it) to it }

internal fun loopBucketLabel(b: TimeBucket): String =
    when (b) {
        TimeBucket.HEUTE -> "Sehr hoch"
        TimeBucket.MORGEN -> "Hoch"
        TimeBucket.FREIBLOCK -> "Mittel"
        TimeBucket.GERING -> "Gering"
        TimeBucket.SPAETER -> "Später"
    }

/** Inline-Items-Helper fuer LazyColumn. */
private inline fun <T> androidx.compose.foundation.lazy.LazyListScope.items(
    list: List<T>,
    crossinline content: @Composable (T) -> Unit,
) {
    items(list.size) { index -> content(list[index]) }
}

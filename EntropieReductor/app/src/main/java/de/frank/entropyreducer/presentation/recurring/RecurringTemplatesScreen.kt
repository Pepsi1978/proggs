package de.frank.entropyreducer.presentation.recurring

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.color

/**
 * Loop-Reiter (Frank-Wunsch 2026-05-22 Phase 2):
 * - Aufgaben sehen 1:1 wie im Aufgaben-Reiter aus (GlassCard mit CategoryIconCircle +
 *   Kategorie-Pill + Titel + Beschreibung + Prio-Zahl).
 * - Checkbox LINKS vor jeder Karte (statt Switch rechts).
 * - Beim Aktivieren der Checkbox wird sofort eine Aufgabe in den Aufgaben- Reiter uebernommen
 *   (ueber GenerateRecurringInstancesUseCase im ViewModel — lastGeneratedAt=0 erzwingt die naechste
 *   Generierung).
 * - Solange die Checkbox aktiv ist, wird nach Abschluss der Aufgabe die naechste Instanz nach RRULE
 *   erzeugt.
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
                            onToggleActive = { viewModel.toggleActive(t) },
                            onDelete = { viewModel.delete(t) },
                            onSetPriority = { score -> viewModel.setPriority(t, score) },
                            onSetTargetBucket = { bucket -> viewModel.setTargetBucket(t, bucket) },
                            onSetInterval = { days -> viewModel.setIntervalDays(t, days) },
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
 * Kategorie-Pill, Titel, Beschreibung, grosse Prio- Zahl rechts. Davor eine Checkbox; aktiv heisst
 * "wird als Aufgabe uebernommen und solange aktiv nach jedem Abschluss neu erstellt".
 *
 * Die RRULE-Beschreibung (Taeglich, Woechentlich …) erscheint als zweite Zeile unter der
 * Beschreibung, damit Frank auf einen Blick sieht wann der Eintrag fällig wird.
 */
// Frank-Wunsch 2026-05-24: internal statt private, damit der Aufgaben-Reiter
// (TasksScreen) die Loop-Karten 1:1 als Akkordeon-Dropdown wiederverwenden kann.
// Frank-Wunsch 2026-05-31: Loop-Karten sehen jetzt 1:1 wie die normalen Aufgaben-
// Karten aus (EntropyEntryCard): Farbverlauf-Hintergrund nach Prioritaet, Titel in
// EINER Zeile, darunter Prio-Perle (oeffnet Schieberegler) + Bucket-Perle (Tag-Wahl).
// Statt des Erledigungs-Haekchens sitzt vorne ein AKTIV-Schalter (gefuellt orange =
// aktiv/wird erstellt, leer = pausiert). Loeschen bleibt als dezentes Icon erreichbar.
@Composable
internal fun TemplateAsTaskCard(
    template: RecurringTemplateEntity,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    onSetPriority: (Int) -> Unit = {},
    onSetTargetBucket: (TimeBucket?) -> Unit = {},
    onSetInterval: (Int?) -> Unit = {},
    // Frank-Wunsch 2026-06-01: Klick auf die Karte oeffnet den Loop-Detail-Screen,
    // wo Titel/Beschreibung/Intervall/Prio/Bucket/aktiv bearbeitet werden.
    onOpenDetail: () -> Unit = {},
    autoOpenPrioSlider: Boolean = false,
    onPrioSliderConsumed: () -> Unit = {},
    autoOpenBucketMenu: Boolean = false,
    onBucketMenuConsumed: () -> Unit = {},
) {
    val cosmos = LocalCosmos.current
    val loopAccent = LocalCosmos.current.accentTasks

    var sliderActive by remember(template.id) { mutableStateOf(false) }
    var liveSlider by remember(template.id) { mutableStateOf<Float?>(null) }
    var bucketMenuOpen by remember(template.id) { mutableStateOf(false) }
    // Frank-Wunsch 2026-06-01: Intervall-Perle (alle N Tage / KI entscheidet).
    var intervalMenuOpen by remember(template.id) { mutableStateOf(false) }

    // Frank-Wunsch 2026-05-31: Vom Widget aus (ACTION_SET_LOOP_PRIORITY / _BUCKET) wird
    // genau diese Vorlage gemeint — dann den Schieberegler bzw. das Tag-Menue direkt
    // aufklappen und die Markierung sofort wieder freigeben (Einmal-Konsum).
    LaunchedEffect(autoOpenPrioSlider) {
        if (autoOpenPrioSlider) {
            sliderActive = true
            onPrioSliderConsumed()
        }
    }
    LaunchedEffect(autoOpenBucketMenu) {
        if (autoOpenBucketMenu) {
            bucketMenuOpen = true
            onBucketMenuConsumed()
        }
    }

    // Effektive Prioritaet = Live-Regler ?: gespeicherter Wert. Faerbt die Karte live.
    val effectivePriority = liveSlider?.toDouble() ?: template.priorityScore.toDouble()
    val ramp = priorityRampColor(effectivePriority)
    val priorityBrush =
        remember(ramp) { Brush.horizontalGradient(colors = listOf(ramp.copy(alpha = 0.20f), ramp)) }
    // Pausierte Vorlagen dezent ausgegraut.
    val cardAlpha = if (template.isActive) 1f else 0.5f

    GlassCard(
        modifier = Modifier.fillMaxWidth().alpha(cardAlpha).clickable(onClick = onOpenDetail),
        tintBrush = priorityBrush,
    ) {
        Column {
            // ZEILE 1: Aktiv-Schalter (vorderes Feld) + Titel + Loeschen.
            Row(verticalAlignment = Alignment.CenterVertically) {
                val boxBg = if (template.isActive) loopAccent else cosmos.glassBg
                val boxBorder = if (template.isActive) loopAccent else cosmos.textSecondary
                Box(
                    modifier =
                        Modifier.size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(boxBg)
                            .border(BorderStroke(2.dp, boxBorder), RoundedCornerShape(8.dp))
                            .clickable(onClick = onToggleActive),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Repeat,
                        contentDescription =
                            if (template.isActive) "Aktiv – tippen zum Pausieren"
                            else "Pausiert – tippen zum Aktivieren",
                        tint = if (template.isActive) Color.White else cosmos.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
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
            // ZEILE 2: Wiederkehr-Rhythmus links + Prio-Perle + Bucket-Perle rechts.
            val prioLabel =
                if (liveSlider != null) "Priorität ${Math.round(liveSlider!!)}"
                else "Priorität ${template.priorityScore}"
            val bucketLabel = template.targetBucket?.let { loopBucketLabel(it) } ?: "KI"
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Frank-Wunsch 2026-06-01: Intervall-Perle. Zeigt "KI" (KI entscheidet),
                // "Täglich" oder "Alle N Tage" und oeffnet bei Tap die Intervall-Auswahl.
                val intervalLabel =
                    when (val d = template.intervalDays) {
                        null -> "KI"
                        1 -> "Täglich"
                        else -> "Alle $d Tage"
                    }
                Box {
                    LoopPearl(label = intervalLabel) { intervalMenuOpen = true }
                    DropdownMenu(
                        expanded = intervalMenuOpen,
                        onDismissRequest = { intervalMenuOpen = false },
                    ) {
                        loopIntervalChoices.forEach { (label, days) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onSetInterval(days)
                                    intervalMenuOpen = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                // Prio-Perle: oeffnet den Schieberegler (setzt priorityScore der Vorlage).
                LoopPearl(label = prioLabel) { sliderActive = !sliderActive }
                Spacer(Modifier.width(8.dp))
                // Bereich-Perle: "KI" oder Prioritätsbereich. Tap öffnet die Bereich-Auswahl.
                Box {
                    LoopPearl(label = bucketLabel) { bucketMenuOpen = true }
                    DropdownMenu(
                        expanded = bucketMenuOpen,
                        onDismissRequest = { bucketMenuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("KI · nach Priorität") },
                            onClick = {
                                onSetTargetBucket(null)
                                bucketMenuOpen = false
                            },
                        )
                        loopBucketOptions.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(loopBucketLabel(b)) },
                                onClick = {
                                    onSetTargetBucket(b)
                                    bucketMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }

            // Schieberegler — nur sichtbar wenn die Prio-Perle angetippt wurde.
            // 5%-Schritte (steps=19). Beim Loslassen wird gespeichert, der Regler
            // klappt zu, und die Kachelfarbe bleibt auf dem neuen Wert.
            if (sliderActive) {
                Spacer(Modifier.height(8.dp))
                val sliderPos = liveSlider ?: template.priorityScore.toFloat()
                Slider(
                    value = sliderPos.coerceIn(0f, 100f),
                    onValueChange = { liveSlider = it },
                    onValueChangeFinished = {
                        liveSlider?.let { onSetPriority(Math.round(it)) }
                        sliderActive = false
                    },
                    valueRange = 0f..100f,
                    steps = 19,
                    colors = SliderDefaults.colors(thumbColor = ramp, activeTrackColor = ramp),
                )
            }
        }
    }
}

/** Waehlbare Wiederkehr-Intervalle in Tagen (Frank-Wunsch 2026-06-01). 1 = taeglich. */
internal val loopIntervalOptions = listOf(1, 2, 3, 5, 7, 10, 14, 30)

/**
 * Frank-Wunsch 2026-06-01: deutlich groessere Bandbreite an Wiederkehr-Intervallen fuer das
 * Dropdown im Loop-Detail (vorher fehlten u.a. "alle 4 Tage" und "alle 6 Tage"). Liste aus (Label,
 * intervalDays): null = "KI entscheidet". Tage lueckenlos 1..31, danach Wochen- und Monats-Schritte
 * (als Tage gerechnet, da das Cooldown-Modell in Tagen ab letzter Erledigung arbeitet). Deckt die
 * gaengige Bandbreite (taeglich bis jaehrlich) ab.
 */
internal val loopIntervalChoices: List<Pair<String, Int?>> = buildList {
    add("KI entscheidet" to null)
    add("Täglich" to 1)
    for (n in 2..31) add("Alle $n Tage" to n)
    add("Alle 6 Wochen (42 Tage)" to 42)
    add("Alle 8 Wochen (56 Tage)" to 56)
    add("Alle 2 Monate (60 Tage)" to 60)
    add("Alle 3 Monate (90 Tage)" to 90)
    add("Alle 4 Monate (120 Tage)" to 120)
    add("Alle 6 Monate (180 Tage)" to 180)
    add("Jährlich (365 Tage)" to 365)
}

/** Menschenlesbares Label fuer ein Intervall (intervalDays). null = KI entscheidet. */
internal fun loopIntervalLabel(days: Int?): String =
    loopIntervalChoices.firstOrNull { it.second == days }?.first
        ?: days?.let { "Alle $it Tage" }
        ?: "KI entscheidet"

/** Die fünf wählbaren Prioritätsbereiche für eine Loop-Vorlage. */
internal val loopBucketOptions =
    listOf(TimeBucket.HEUTE, TimeBucket.MORGEN, TimeBucket.FREIBLOCK, TimeBucket.GERING, TimeBucket.SPAETER)

internal fun loopBucketLabel(b: TimeBucket): String =
    when (b) {
        TimeBucket.HEUTE -> "Priorität sehr hoch"
        TimeBucket.MORGEN -> "Priorität hoch"
        TimeBucket.FREIBLOCK -> "Priorität mittel"
        TimeBucket.GERING -> "Priorität gering"
        TimeBucket.SPAETER -> "Später"
    }

/**
 * Kleine Perle im gleichen Stil wie die Prio-/Bucket-Perle der normalen Aufgaben- Karte
 * (glassBg-Hintergrund + textSecondary-Text). Frank-Wunsch 2026-05-31.
 */
@Composable
private fun LoopPearl(label: String, onClick: () -> Unit) {
    val cosmos = LocalCosmos.current
    val pillShape = remember { RoundedCornerShape(50) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.background(cosmos.glassBg, pillShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = cosmos.textSecondary,
        )
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
    val base =
        when {
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
 * - "FREQ=DAILY" → "Täglich"
 * - "FREQ=WEEKLY;BYDAY=MO,WE,FR" → "Wöchentlich: Mo, Mi, Fr"
 * - "FREQ=MONTHLY;BYMONTHDAY=1" → "Monatlich am 1."
 * - unbekannte Regel → die Original-Regel
 */
internal fun humanReadable(rrule: String): String {
    val parts =
        rrule
            .split(";")
            .mapNotNull { part ->
                val kv = part.split("=", limit = 2)
                if (kv.size == 2) kv[0].uppercase() to kv[1] else null
            }
            .toMap()
    val freq = parts["FREQ"] ?: return rrule
    val interval = parts["INTERVAL"]?.toIntOrNull() ?: 1
    val byDay = parts["BYDAY"]?.split(",")
    val byMonthDay = parts["BYMONTHDAY"]
    val dayNamesShort =
        mapOf(
            "MO" to "Mo",
            "TU" to "Di",
            "WE" to "Mi",
            "TH" to "Do",
            "FR" to "Fr",
            "SA" to "Sa",
            "SU" to "So",
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

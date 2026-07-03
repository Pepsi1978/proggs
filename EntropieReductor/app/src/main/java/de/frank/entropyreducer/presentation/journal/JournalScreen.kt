package de.frank.entropyreducer.presentation.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorEntryEntity
import de.frank.entropyreducer.data.prefs.JournalSyncStatus
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.navigation.Routes
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Journal-Reiter (Frank-Wunsch 2026-05-24) — Sub-Bereich Slot 1 des Aufgaben-Tabs.
 *
 * Zeigt die aus BestJournal Frank gespiegelten Tagebucheintraege read-only an. Optik 1:1 wie der
 * Entropie-Reiter (Timeline mit Buch-Badge + Zeit-Sektionen). Ganz oben ein Sync-Status-Kopf mit
 * Zeitpunkt der letzten Synchronisierung + Anzahl neuer Eintraege. Tippen auf einen Eintrag oeffnet
 * den read-only Detail-Screen (mit Vorlesen).
 */
@Composable
fun JournalScreen(
    onSwitchSub: (parentTab: String, index: Int) -> Unit,
    onSwitchTab: (route: String) -> Unit,
    onOpenEntry: (sourceId: Long) -> Unit,
    showBottomBar: Boolean = true,
    vm: JournalViewModel = hiltViewModel(),
) {
    val cosmos = LocalCosmos.current
    val entries by vm.entries.collectAsStateWithLifecycle()
    val status by vm.syncStatus.collectAsStateWithLifecycle()

    CosmosScaffold(
        title = "Journal",
        showBottomBar = showBottomBar,
        // Frank-Wunsch 2026-05-24: kompakter Header zieht Titel + Sync-Kopf + Liste
        // dichter zusammen (weniger Luft unter "Journal").
        compactHeader = true,
        bottomBar = {
            CosmosBottomBar(
                currentTab = Routes.SCIENTIST,
                micState = MicState.IDLE,
                onTabSelected = { route -> onSwitchTab(route) },
                // Journal ist read-only — der Mic-Knopf hat hier keine Funktion.
                onMicClick = {},
                onSubAreaSelected = { parent, index -> onSwitchSub(parent, index) },
                forcedSubMode = Routes.SCIENTIST,
                // Frank-Wunsch 2026-06-10: Journal ist Sub-Bereich 3 unter Forscher
                // (vom Aufgaben-Bereich umgezogen) → dauerhaft hervorheben.
                selectedSubIndex = 3,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SyncStatusHeader(status)
            if (entries.isEmpty()) {
                Column(
                    // weight(1f): fuellt nur den Rest UNTER dem Sync-Kopf (kein Ueberlauf).
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Book,
                        contentDescription = null,
                        tint = JournalAccent,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Dein Journal",
                        style = MaterialTheme.typography.titleLarge,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text =
                            "Noch keine Tagebucheinträge aus BestJournal Frank. Beim nächsten " +
                                "App-Start werden neue Einträge automatisch übernommen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cosmos.textSecondary,
                    )
                }
            } else {
                val grouped = remember(entries) { groupEntriesBySection(entries) }
                LazyColumn(
                    // weight(1f): fuellt nur den Rest UNTER dem Sync-Kopf (kein Ueberlauf).
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding =
                        PaddingValues(start = 12.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    grouped.forEach { section ->
                        item(key = "header-${section.label}") {
                            SectionHeader(label = section.label)
                        }
                        val lastIndex = section.entries.lastIndex
                        section.entries.forEachIndexed { index, e ->
                            val position =
                                when {
                                    section.entries.size == 1 -> TimelinePosition.ONLY
                                    index == 0 -> TimelinePosition.FIRST
                                    index == lastIndex -> TimelinePosition.LAST
                                    else -> TimelinePosition.MIDDLE
                                }
                            item(key = e.sourceId) {
                                TimelineEntryRow(
                                    entry = e,
                                    position = position,
                                    onClick = { onOpenEntry(e.sourceId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusHeader(status: JournalSyncStatus) {
    val cosmos = LocalCosmos.current
    val text =
        if (status.lastSyncMs == 0L) {
            "Noch nicht synchronisiert · 0 Einträge neu"
        } else {
            val ts =
                SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.GERMANY)
                    .format(Date(status.lastSyncMs))
            val n = status.lastNewCount
            val neu = if (n == 1) "1 neuer Eintrag" else "$n Einträge neu"
            "Zuletzt synchronisiert: $ts  ·  $neu"
        }
    // Frank-Wunsch 2026-05-24: dezenter Grauton statt Orange (nicht so stark sichtbar)
    // und dicht unter dem Titel — nur ganz wenig Abstand oben/unten.
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = cosmos.textSecondary,
        modifier =
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 4.dp),
    )
}

/** Section-Header in der Zeit-gruppierten Liste — wie im Entropie-Reiter. */
@Composable
private fun SectionHeader(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(52.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = JournalAccent,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.size(8.dp))
        Box(
            modifier =
                Modifier.weight(1f).height(1.dp).background(JournalAccent.copy(alpha = 0.25f))
        )
    }
}

/** Eintrag plus Timeline-Rail links — 52dp Spalte mit Linie und Buch-Badge. */
@Composable
private fun TimelineEntryRow(
    entry: JournalMirrorEntryEntity,
    position: TimelinePosition,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val lineColor = JournalAccent.copy(alpha = 0.35f)
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 4.dp)) {
        Box(modifier = Modifier.width(52.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (position == TimelinePosition.ONLY) return@Canvas
                val cx = size.width / 2f
                val midY = size.height / 2f
                val gap = 18.dp.toPx() + 2.dp.toPx()
                val strokePx = 2.dp.toPx()
                val drawAbove =
                    position == TimelinePosition.MIDDLE || position == TimelinePosition.LAST
                val drawBelow =
                    position == TimelinePosition.MIDDLE || position == TimelinePosition.FIRST
                if (drawAbove && midY - gap > 0f) {
                    drawLine(lineColor, Offset(cx, 0f), Offset(cx, midY - gap), strokePx)
                }
                if (drawBelow && midY + gap < size.height) {
                    drawLine(lineColor, Offset(cx, midY + gap), Offset(cx, size.height), strokePx)
                }
            }
            Box(
                modifier =
                    Modifier.size(36.dp)
                        .clip(CircleShape)
                        .background(JournalAccent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Book,
                    contentDescription = null,
                    tint = JournalAccent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        GlassCard(modifier = Modifier.weight(1f).clickable { onClick() }) {
            Column {
                Text(
                    text = entry.title?.takeIf { it.isNotBlank() } ?: "Tagebucheintrag",
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatJournalTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = entry.displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textPrimary,
                    maxLines = 5,
                )
            }
        }
    }
}

private enum class TimelinePosition {
    FIRST,
    MIDDLE,
    LAST,
    ONLY,
}

private data class JournalSection(val label: String, val entries: List<JournalMirrorEntryEntity>)

private fun groupEntriesBySection(entries: List<JournalMirrorEntryEntity>): List<JournalSection> {
    if (entries.isEmpty()) return emptyList()
    val sorted = entries.sortedByDescending { it.timestamp }
    val buckets = linkedMapOf<String, MutableList<JournalMirrorEntryEntity>>()
    sorted.forEach { e ->
        val label = sectionLabelFor(e.timestamp)
        buckets.getOrPut(label) { mutableListOf() }.add(e)
    }
    return buckets.map { (label, list) -> JournalSection(label, list) }
}

/** Zeit-Sektion-Label — adaptiert aus dem Entropie-Reiter (Heute/Gestern/Diese Woche/...). */
private fun sectionLabelFor(timestamp: Long): String {
    val now = Calendar.getInstance()
    val entry = Calendar.getInstance().apply { timeInMillis = timestamp }
    val todayStart =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    val yesterdayStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val dow = todayStart.get(Calendar.DAY_OF_WEEK)
    val daysSinceMonday = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
    val thisWeekMonday =
        (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysSinceMonday) }
    val lastWeekMonday =
        (thisWeekMonday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
    val twoWeeksAgo = (thisWeekMonday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -14) }
    val threeWeeksAgo =
        (thisWeekMonday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -21) }
    val fourWeeksAgo = (thisWeekMonday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -28) }
    val sameMonth =
        entry.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
            entry.get(Calendar.YEAR) == now.get(Calendar.YEAR)

    return when {
        timestamp >= todayStart.timeInMillis -> "Heute"
        timestamp >= yesterdayStart.timeInMillis -> "Gestern"
        timestamp >= thisWeekMonday.timeInMillis && sameMonth -> "Diese Woche"
        timestamp >= lastWeekMonday.timeInMillis && sameMonth -> "Letzte Woche"
        timestamp >= twoWeeksAgo.timeInMillis && sameMonth -> "Vor 2 Wochen"
        timestamp >= threeWeeksAgo.timeInMillis && sameMonth -> "Vor 3 Wochen"
        timestamp >= fourWeeksAgo.timeInMillis && sameMonth -> "Vor 4 Wochen"
        entry.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> {
            val fmt = SimpleDateFormat("MMMM yyyy", Locale.GERMAN)
            fmt.format(Date(timestamp)).replaceFirstChar { it.uppercase() }
        }
        else -> {
            val fmt = SimpleDateFormat("MMMM", Locale.GERMAN)
            val month = fmt.format(Date(timestamp)).replaceFirstChar { it.uppercase() }
            "${entry.get(Calendar.YEAR)} — $month"
        }
    }
}

// Lila (Frank-Wunsch 2026-06-18): Forscher-Sub-Screens sind lila.
// TTS-Buttons bleiben Orange (cosmos.accent).
internal val JournalAccent: Color
    @Composable get() = LocalCosmos.current.accentForscher

internal fun formatJournalTimestamp(ts: Long): String =
    SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.GERMANY).format(Date(ts))

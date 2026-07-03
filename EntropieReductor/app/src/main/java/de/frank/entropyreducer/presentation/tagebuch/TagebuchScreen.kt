package de.frank.entropyreducer.presentation.tagebuch

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.components.VoiceCaptureState
import de.frank.entropyreducer.presentation.components.VoiceCaptureViewModel
import de.frank.entropyreducer.presentation.components.rememberMicPermissionState
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.navigation.Routes
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tagebuch-Bereich (Frank-Wunsch 2026-05-18) — Sub-Bereich 1 des Aufgaben-Tabs.
 *
 * Erste Iteration (MVP): Texteingabe ueber Stift-Button, Persistierung im DataStore als JSON-Liste,
 * Liste der Eintraege nach Datum sortiert, Detail-Sheet beim Tap auf einen Eintrag. Mikrofon-Button
 * ist als Platzhalter vorhanden — Whisper-Aufnahme, Gemini-Text-Verbesserung, TTS-Vorlesen und
 * Symbol-Kategorien folgen in einer naechsten Iteration (1:1-Portierung der
 * BestJournalFrank-Tagebuchfunktion ist umfangreich).
 *
 * Die App-Farbe folgt der Forscher-Akzentfarbe (violett) damit der Tab visuell in den
 * Forscher-Bereich passt. CosmosBottomBar laeuft im Sub-Mode mit forcedSubMode=SCIENTIST.
 */
@Composable
fun TagebuchScreen(
    onBack: () -> Unit,
    onSwitchSub: (parentTab: String, index: Int) -> Unit,
    onSwitchTab: (route: String) -> Unit,
    onOpenEntry: (entryId: String) -> Unit = {},
    showBottomBar: Boolean = true,
) {
    val cosmos = LocalCosmos.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Stabiler Flow (Bug-Almanach jetpack-compose.md Kurzcheck #16 / §2.14): den rohen cold
    // Flow NICHT pro Recomposition neu bauen, sonst verpasst collectAsStateWithLifecycle
    // Emissionen (gespeicherte Aenderung erscheint erst beim naechsten Tap). remember stabilisiert
    // ihn.
    val entriesStream = remember(context) { tagebuchEntriesFlow(context) }
    val entries by entriesStream.collectAsStateWithLifecycle(initialValue = emptyList())

    var inputDialogOpen by remember { mutableStateOf(false) }
    // Frank-Wunsch 2026-05-18 Folgeauftrag: Erst nur Mic-Button anzeigen.
    // Klick auf Mic legt rechts "Aufnehmen" und links "Schreiben" frei.
    var actionsExpanded by remember { mutableStateOf(false) }
    // A12: echte Whisper-Aufnahme via Groq + Gemini-Text-Verbesserung.
    val voiceVm: VoiceCaptureViewModel = hiltViewModel()
    val voiceState by voiceVm.state.collectAsStateWithLifecycle()
    val voiceError by voiceVm.error.collectAsStateWithLifecycle()
    LaunchedEffect(voiceError) {
        voiceError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            voiceVm.clearError()
        }
    }
    val improveVm: TagebuchImproveViewModel = hiltViewModel()
    val improveState by improveVm.state.collectAsStateWithLifecycle()
    val improvedText by improveVm.improvedText.collectAsStateWithLifecycle()
    val improveError by improveVm.error.collectAsStateWithLifecycle()
    // Frank-Wunsch 2026-05-19: Auto-Ueberschrift via Gemini (max 3 Woerter).
    // Wird nach jedem neuen Eintrag und nach jedem Edit asynchron gesetzt.
    val titleVm: TagebuchTitleViewModel = hiltViewModel()
    // Frank-Wunsch 2026-05-20: Auto-Zusammenfassung (3-5 Bullet-Points) via Gemini.
    // Wird nach jedem neuen Eintrag asynchron erzeugt — Detail-Screen zeigt sie als
    // Bullet-Points statt eines leeren Platzhalters.
    val summaryVm: TagebuchSummaryViewModel = hiltViewModel()
    var pendingTranscript by remember { mutableStateOf<String?>(null) }
    val micPermission =
        rememberMicPermissionState(
            onAllGranted = {
                voiceVm.toggle { transcript ->
                    pendingTranscript = transcript
                    actionsExpanded = false
                }
            }
        )

    // Helper: Eintrag speichern und parallel den Auto-Titel via Gemini erzeugen.
    // Fallback-Titel bleibt bei Fehler (kein API-Key, kein Netz) bestehen.
    //
    // Frank-Wunsch 2026-06-01: Auch beim SOFORT-Verbessern direkt nach der Aufnahme
    // muss das Original-Transkript erhalten bleiben. rawText = immer das Original,
    // improved = optionaler Gemini-Text. Beide werden gespeichert (text + improvedText),
    // damit der Detail-Screen zwischen Original und Verbessert umschalten kann —
    // genau wie bei der nachtraeglichen Verbesserung. preferImproved spiegelt Franks
    // Anzeigewahl im Transkript-Dialog (welcher Text als primaer angezeigt wird).
    val saveNewEntry: (String, String?, Boolean) -> Unit = { rawText, improved, preferImproved ->
        val base = TagebuchEntry.create(rawText)
        val entry =
            if (!improved.isNullOrBlank()) {
                base.copy(improvedText = improved, isImproved = preferImproved)
            } else {
                base
            }
        scope.launch { addTagebuchEntry(context, entry) }
        // Titel/Zusammenfassung aus dem primaer angezeigten Text ableiten (bessere Qualitaet).
        val primaryText = if (preferImproved && !improved.isNullOrBlank()) improved else rawText
        titleVm.generateTitle(primaryText) { newTitle ->
            scope.launch { updateTagebuchEntry(context, entry.id, title = newTitle) }
        }
        summaryVm.generateSummary(primaryText) { bullets ->
            scope.launch { updateTagebuchEntry(context, entry.id, summary = bullets) }
        }
    }

    if (inputDialogOpen) {
        TextInputDialog(
            onDismiss = { inputDialogOpen = false },
            onSave = { text ->
                // Manuell getippter Eintrag — keine KI-Verbesserung, nur Original.
                saveNewEntry(text, null, false)
                inputDialogOpen = false
            },
        )
    }
    // A12: Transkript-Dialog nach Whisper-Aufnahme — Frank kann den Text
    // editieren, optional via Gemini verbessern lassen und speichern.
    val pending = pendingTranscript
    if (pending != null) {
        TranscriptDialog(
            initialText = pending,
            improvedText = improvedText,
            isImproving = improveState == ImproveState.RUNNING,
            errorMessage = improveError ?: voiceError,
            onImprove = { text -> improveVm.improve(text) },
            onSave = { original, improved, preferImproved ->
                saveNewEntry(original, improved, preferImproved)
                pendingTranscript = null
                improveVm.clearImproved()
            },
            onDismiss = {
                pendingTranscript = null
                improveVm.clearImproved()
            },
        )
    }
    // Detail wird jetzt als Vollbild-Screen ueber den NavGraph aufgerufen
    // (Frank-Wunsch 2026-05-20). Tap auf eine Eintrag-Karte navigiert direkt
    // dorthin — kein AlertDialog mehr.

    CosmosScaffold(
        title = "Tagebuch",
        showBottomBar = showBottomBar,
        bottomBar = {
            CosmosBottomBar(
                currentTab = Routes.SCIENTIST,
                micState =
                    when (voiceState) {
                        VoiceCaptureState.RECORDING -> MicState.RECORDING
                        VoiceCaptureState.PROCESSING -> MicState.PROCESSING
                        VoiceCaptureState.IDLE -> MicState.IDLE
                    },
                onTabSelected = { route -> onSwitchTab(route) },
                // Frank-Wunsch 2026-05-18 (Folgeauftrag 3): Der untere Mic-
                // Button in der BottomBar ist der EINZIGE Mic-Knopf. Sein
                // Klick faltet die zwei Aktions-Buttons (Stift + Aufnehmen-Mic)
                // direkt darueber auf — vorher ist im Tagebuch-Bereich
                // KEIN Mikrofon zu sehen.
                onMicClick = { actionsExpanded = !actionsExpanded },
                onSubAreaSelected = { parent, index -> onSwitchSub(parent, index) },
                forcedSubMode = Routes.SCIENTIST,
                // Frank-Wunsch 2026-06-09: Tagebuch ("Entropie") ist jetzt Sub-Bereich 1
                // unter Forscher → dauerhaft hervorheben.
                selectedSubIndex = 1,
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Book,
                        contentDescription = null,
                        tint = TagebuchAccent,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Dein Tagebuch",
                        style = MaterialTheme.typography.titleLarge,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text =
                            "Hier kannst du innere, mentale und emotionale Entropie aufschreiben. " +
                                "Tippe auf den Stift um deinen ersten Eintrag zu erstellen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cosmos.textSecondary,
                    )
                }
            } else {
                // Frank-Wunsch 2026-05-23: Gruppierung nach Zeit-Sektionen wie
                // BestJournalFrank's Tagebuch — Heute / Gestern / Diese Woche /
                // Letzte Woche / Vor 2/3/4 Wochen / Monatsname / Jahr — Monat.
                // Jeder Eintrag bekommt links eine durchgehende Timeline-Rail
                // mit Buch-Badge und rechts die Karte. Sortierung von neu nach alt.
                val grouped = remember(entries) { groupEntriesBySection(entries) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            start = 12.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 120.dp,
                        ),
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
                            item(key = e.id) {
                                TimelineEntryRow(
                                    entry = e,
                                    position = position,
                                    onClick = { onOpenEntry(e.id) },
                                )
                            }
                        }
                    }
                }
            }

            // Frank-Wunsch 2026-05-18 (Folgeauftrag 3):
            // KEIN eigener Mic-FAB im Tagebuch-Bereich. Der BottomBar-Mic-
            // Button (in der Statusleiste unten) ist der einzige Mic-Trigger.
            // Sein Klick setzt actionsExpanded=true → es erscheinen direkt
            // ueber der BottomBar (rechts: Aufnehmen-Mic, links: Schreiben).
            // Standardmaessig ist im Tagebuch-Bereich NICHTS zu sehen.
            if (actionsExpanded) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    FabIconButton(
                        icon = Icons.Outlined.Edit,
                        label = "Schreiben",
                        backgroundColor = TagebuchAccent.copy(alpha = 0.7f),
                        // Frank-Wunsch 2026-05-22 (dritte Iteration): Icon schwarz
                        // wie der zentrale BottomBar-Mic.
                        iconTint = Color.Black,
                        onClick = {
                            inputDialogOpen = true
                            actionsExpanded = false
                        },
                    )
                    // A12: echte Whisper-Aufnahme via Groq Large V3 Turbo.
                    val (recordIcon, recordLabel) =
                        when (voiceState) {
                            VoiceCaptureState.IDLE -> Icons.Outlined.Mic to "Aufnehmen"
                            VoiceCaptureState.RECORDING -> Icons.Outlined.Stop to "Stop"
                            VoiceCaptureState.PROCESSING -> Icons.Outlined.Mic to "Transkribiere…"
                        }
                    FabIconButton(
                        icon = recordIcon,
                        label = recordLabel,
                        backgroundColor =
                            if (voiceState == VoiceCaptureState.RECORDING)
                                Color(0xFFE53935).copy(alpha = 0.7f)
                            else TagebuchAccent.copy(alpha = 0.7f),
                        iconTint = Color.Black,
                        onClick = {
                            when (voiceState) {
                                VoiceCaptureState.IDLE -> {
                                    if (micPermission.check()) {
                                        voiceVm.toggle { transcript ->
                                            pendingTranscript = transcript
                                            actionsExpanded = false
                                        }
                                    } else {
                                        micPermission.request()
                                    }
                                }
                                VoiceCaptureState.RECORDING -> {
                                    voiceVm.toggle { transcript ->
                                        pendingTranscript = transcript
                                        actionsExpanded = false
                                    }
                                }
                                VoiceCaptureState.PROCESSING -> Unit
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Section-Header in der Zeit-gruppierten Liste — z.B. "Heute", "Diese Woche", "Vor 2 Wochen", "Mai"
 * oder "2024 — Dezember". Frank-Wunsch 2026-05-23.
 */
@Composable
private fun SectionHeader(label: String) {
    val cosmos = LocalCosmos.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Linker Block hat 52dp Breite — passt genau zur Timeline-Rail darunter.
        Spacer(Modifier.width(52.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = TagebuchAccent,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.size(8.dp))
        Box(
            modifier =
                Modifier.weight(1f).height(1.dp).background(TagebuchAccent.copy(alpha = 0.25f))
        )
    }
}

/**
 * Eintrag plus Timeline-Rail links — 52dp Spalte mit durchgehender Linie und zentriertem
 * Buch-Badge. Frank-Wunsch 2026-05-23 (Vorbild BestJournalFrank).
 */
@Composable
private fun TimelineEntryRow(
    entry: TagebuchEntry,
    position: TimelinePosition,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val lineColor = TagebuchAccent.copy(alpha = 0.35f)
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
                    drawLine(
                        color = lineColor,
                        start = Offset(cx, 0f),
                        end = Offset(cx, midY - gap),
                        strokeWidth = strokePx,
                    )
                }
                if (drawBelow && midY + gap < size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(cx, midY + gap),
                        end = Offset(cx, size.height),
                        strokeWidth = strokePx,
                    )
                }
            }
            Box(
                modifier =
                    Modifier.size(36.dp)
                        .clip(CircleShape)
                        .background(TagebuchAccent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Book,
                    contentDescription = null,
                    tint = TagebuchAccent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        GlassCard(modifier = Modifier.weight(1f).clickable { onClick() }) {
            Column {
                Text(
                    text = entry.title.ifBlank { "Tagebucheintrag" },
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatTimestamp(entry.timestampMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
                Spacer(Modifier.height(6.dp))
                // Frank-Wunsch 2026-05-23: 5 statt 4 Zeilen Vorschau.
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textPrimary,
                    maxLines = 5,
                )
            }
        }
    }
}

/** Position eines Eintrags innerhalb seiner Zeit-Sektion — steuert die Timeline-Linie. */
private enum class TimelinePosition {
    FIRST,
    MIDDLE,
    LAST,
    ONLY,
}

/** Eine Zeit-Sektion mit ihrem Label und allen darunter gruppierten Eintraegen. */
private data class TagebuchSection(val label: String, val entries: List<TagebuchEntry>)

/**
 * Gruppiert Eintraege nach Zeit-Sektionen (Frank-Wunsch 2026-05-23). Reihenfolge: Heute, Gestern,
 * Diese Woche, Letzte Woche, Vor 2/3/4 Wochen, Monatsname, Jahr — Monat. Innerhalb der Sektion
 * bleiben die Eintraege nach Timestamp absteigend sortiert. Erwartet eine bereits nach Timestamp
 * absteigend sortierte Eingabe.
 */
private fun groupEntriesBySection(entries: List<TagebuchEntry>): List<TagebuchSection> {
    if (entries.isEmpty()) return emptyList()
    val sorted = entries.sortedByDescending { it.timestampMs }
    val buckets = linkedMapOf<String, MutableList<TagebuchEntry>>()
    sorted.forEach { e ->
        val label = sectionLabelFor(e.timestampMs)
        buckets.getOrPut(label) { mutableListOf() }.add(e)
    }
    return buckets.map { (label, list) -> TagebuchSection(label, list) }
}

/**
 * Berechnet das Section-Label fuer einen Timestamp. Adaptiert aus
 * BestJournalFrank/DateTimeFormatter.getSectionLabel — erweitert um "Heute" und "Gestern" am Anfang
 * (Frank-Wunsch 2026-05-23, exakter Wortlaut "heute und gestern").
 */
private fun sectionLabelFor(timestamp: Long): String {
    val now = java.util.Calendar.getInstance()
    val entry = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val todayStart =
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
    val yesterdayStart =
        (todayStart.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    val dow = todayStart.get(java.util.Calendar.DAY_OF_WEEK)
    val daysSinceMonday =
        if (dow == java.util.Calendar.SUNDAY) 6 else dow - java.util.Calendar.MONDAY
    val thisWeekMonday =
        (todayStart.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.DAY_OF_YEAR, -daysSinceMonday)
        }
    val lastWeekMonday =
        (thisWeekMonday.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.DAY_OF_YEAR, -7)
        }
    val twoWeeksAgo =
        (thisWeekMonday.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.DAY_OF_YEAR, -14)
        }
    val threeWeeksAgo =
        (thisWeekMonday.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.DAY_OF_YEAR, -21)
        }
    val fourWeeksAgo =
        (thisWeekMonday.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.DAY_OF_YEAR, -28)
        }
    val sameMonth =
        entry.get(java.util.Calendar.MONTH) == now.get(java.util.Calendar.MONTH) &&
            entry.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR)

    return when {
        timestamp >= todayStart.timeInMillis -> "Heute"
        timestamp >= yesterdayStart.timeInMillis -> "Gestern"
        timestamp >= thisWeekMonday.timeInMillis && sameMonth -> "Diese Woche"
        timestamp >= lastWeekMonday.timeInMillis && sameMonth -> "Letzte Woche"
        timestamp >= twoWeeksAgo.timeInMillis && sameMonth -> "Vor 2 Wochen"
        timestamp >= threeWeeksAgo.timeInMillis && sameMonth -> "Vor 3 Wochen"
        timestamp >= fourWeeksAgo.timeInMillis && sameMonth -> "Vor 4 Wochen"
        entry.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) -> {
            val fmt = java.text.SimpleDateFormat("MMMM yyyy", Locale.GERMAN)
            fmt.format(java.util.Date(timestamp)).replaceFirstChar { it.uppercase() }
        }
        else -> {
            val fmt = java.text.SimpleDateFormat("MMMM", Locale.GERMAN)
            val month = fmt.format(java.util.Date(timestamp)).replaceFirstChar { it.uppercase() }
            "${entry.get(java.util.Calendar.YEAR)} — $month"
        }
    }
}

@Composable
private fun FabIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier.size(56.dp).clip(CircleShape).background(backgroundColor).clickable {
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalCosmos.current.textSecondary,
        )
    }
}

@Composable
private fun TextInputDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuer Tagebucheintrag") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                placeholder = {
                    Text("Beschreibe deine innere, mentale oder emotionale Entropie ...")
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onSave(text.trim()) },
                enabled = text.isNotBlank(),
            ) {
                Text("Speichern", color = TagebuchAccent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun TranscriptDialog(
    initialText: String,
    improvedText: String?,
    isImproving: Boolean,
    errorMessage: String?,
    onImprove: (String) -> Unit,
    // Frank-Wunsch 2026-06-01: Original UND verbesserter Text werden zurueckgegeben,
    // damit das Original beim Sofort-Verbessern nicht verloren geht. preferImproved =
    // welcher Text gerade angezeigt wird (wird als primaere Anzeige im Detail uebernommen).
    onSave: (original: String, improved: String?, preferImproved: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var editableText by remember(initialText) { mutableStateOf(initialText) }
    var useImproved by remember { mutableStateOf(false) }
    val activeText = if (useImproved && improvedText != null) improvedText else editableText
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag von der Aufnahme") },
        text = {
            Column {
                if (improvedText != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (useImproved) "Verbessert" else "Original",
                            style = MaterialTheme.typography.labelMedium,
                            color = TagebuchAccent,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.size(8.dp))
                        TextButton(onClick = { useImproved = !useImproved }) {
                            Text(if (useImproved) "Original zeigen" else "Verbessert zeigen")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                OutlinedTextField(
                    value = activeText,
                    onValueChange = {
                        if (useImproved) {
                            // Verbesserter Text war bisher read-only — bei Tippen
                            // schalten wir zurueck auf Original-Edit.
                            useImproved = false
                            editableText = it
                        } else {
                            editableText = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE53935),
                    )
                }
                if (improvedText == null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { onImprove(editableText) },
                            enabled = !isImproving && editableText.isNotBlank(),
                        ) {
                            if (isImproving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("Wird verbessert…")
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(6.dp))
                                Text("Text verbessern (Gemini)")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (activeText.isNotBlank()) {
                        // Original = editierter Roh-Text, Verbessert = Gemini-Resultat (falls
                        // vorhanden).
                        // useImproved = welche Variante Frank gerade ansieht → wird als primaer
                        // uebernommen.
                        val original = editableText.trim().ifBlank { activeText.trim() }
                        onSave(original, improvedText?.trim(), useImproved && improvedText != null)
                    }
                },
                enabled = activeText.isNotBlank(),
            ) {
                Text("Speichern", color = TagebuchAccent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

/**
 * Detail-Dialog fuer einen Tagebucheintrag.
 *
 * Frank-Wunsch 2026-05-19:
 * - Text ist bearbeitbar (OutlinedTextField) — Fehler im Eintrag koennen nachtraeglich korrigiert
 *   werden.
 * - Rechts oben ein 3-Punkte-Menue mit "Eintrag loeschen".
 * - Speichern-Button erscheint nur wenn der Text geaendert wurde.
 */
@Composable
private fun EntryDetailDialog(
    entry: TagebuchEntry,
    onDismiss: () -> Unit,
    onSave: (updatedText: String) -> Unit,
    onDelete: () -> Unit,
) {
    var editableText by remember(entry.id) { mutableStateOf(entry.text) }
    var menuOpen by remember { mutableStateOf(false) }
    val hasChanges = editableText.trim() != entry.text.trim() && editableText.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.title.ifBlank { "Tagebucheintrag" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "Mehr")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Eintrag löschen") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        },
        text = {
            Column {
                Text(
                    text = formatTimestamp(entry.timestampMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalCosmos.current.textSecondary,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editableText,
                    onValueChange = { editableText = it },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }
        },
        confirmButton = {
            if (hasChanges) {
                TextButton(
                    onClick = { onSave(editableText.trim()) },
                    enabled = editableText.isNotBlank(),
                ) {
                    Text("Speichern", color = TagebuchAccent)
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Schließen") }
            }
        },
        dismissButton = {
            if (hasChanges) {
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
            }
        },
    )
}

/* ============================== Datenmodell ============================== */

data class TagebuchEntry(
    val id: String,
    val timestampMs: Long,
    val title: String,
    val text: String,
    /**
     * Nachträge zum Eintrag (Frank-Wunsch 2026-05-20). Werden im Vollbild- Detail-Screen als eigene
     * Karten angezeigt. Bei alten Einträgen ohne dieses Feld liefert der JSON-Parser eine leere
     * Liste.
     */
    val followups: List<TagebuchFollowup> = emptyList(),
    /**
     * KI-generierte Zusammenfassung (Frank-Wunsch 2026-05-20, 2026-05-23 auf Fliesstext). `null` =
     * noch keine Zusammenfassung erstellt — der Detail-Screen zeigt dann einen Knopf "Mit KI
     * zusammenfassen".
     */
    val summary: String? = null,
    /**
     * Nachträgliche KI-Verbesserung des Eintrags-Texts (Frank-Wunsch 2026-05-23). `null` = noch
     * nicht via KI verbessert; in der UI gibt es dann den Knopf "Mit KI nachträglich verbessern".
     * Original-Text bleibt in [text].
     */
    val improvedText: String? = null,
    val isImproved: Boolean = false,
    /**
     * Zeitpunkt der letzten Bearbeitung (Edit-Sync, Phase B 2026-06-20). Wird bei jeder Aenderung
     * am Eintrag (Text/Titel/Summary/Improved + Followup-Aenderungen) auf `now` gesetzt und treibt
     * den geraeteuebergreifenden Last-Write-Wins im Restore. Bestand (alte Backups ohne Feld)
     * faellt im Parser auf `timestampMs` als Baseline zurueck.
     */
    val updatedAt: Long = 0L,
) {
    companion object {
        fun create(text: String): TagebuchEntry {
            // Title = erste 30-40 Zeichen oder erste Zeile, ohne Punkt-Suffix.
            val firstLine = text.lineSequence().firstOrNull().orEmpty()
            val title =
                if (firstLine.length <= 40) firstLine.trim() else firstLine.take(40).trim() + "…"
            val now = System.currentTimeMillis()
            return TagebuchEntry(
                id = UUID.randomUUID().toString(),
                timestampMs = now,
                title = title,
                text = text,
                followups = emptyList(),
                updatedAt = now,
            )
        }
    }
}

/**
 * Einzelner Nachtrag zu einem [TagebuchEntry]. Hat ebenfalls einen eigenen KI-verbesserten Text
 * (Frank-Wunsch 2026-05-23) damit jeder Nachtrag separat nachgeschliffen werden kann.
 */
data class TagebuchFollowup(
    val id: String,
    val createdAtMs: Long,
    val text: String,
    val improvedText: String? = null,
    val isImproved: Boolean = false,
)

private val Context.tagebuchStore by preferencesDataStore(name = "tagebuch_entries")
private val KEY_ENTRIES = stringPreferencesKey("entries_json")

internal fun tagebuchEntriesFlow(context: Context): Flow<List<TagebuchEntry>> =
    context.tagebuchStore.data.map { prefs ->
        val raw = prefs[KEY_ENTRIES] ?: return@map emptyList()
        runCatching {
                val arr = JSONArray(raw)
                buildList(arr.length()) {
                    for (i in 0 until arr.length()) add(jsonToEntry(arr.getJSONObject(i)))
                }
            }
            .getOrDefault(emptyList())
            .sortedByDescending { it.timestampMs }
    }

/** Beobachtbarer Flow eines einzelnen Eintrags — für den Vollbild-Detail-Screen. */
internal fun tagebuchEntryFlow(context: Context, id: String): Flow<TagebuchEntry?> =
    tagebuchEntriesFlow(context).map { list -> list.firstOrNull { it.id == id } }

internal suspend fun addTagebuchEntry(context: Context, entry: TagebuchEntry) {
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing + entry
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Tagebuch/Journal: Aenderung")
}

internal suspend fun deleteTagebuchEntry(context: Context, id: String, propagate: Boolean = true) {
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing.filterNot { it.id == id }
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
    // Frank-Wunsch 2026-06-20: Loeschung propagieren (Tombstone). propagate=false: Restore-Cleanup.
    if (propagate) {
        de.frank.entropyreducer.data.markDeleted(
            context, de.frank.entropyreducer.data.TombstoneType.TAGEBUCH, id)
        de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(
            context, "Tagebuch/Journal: Aenderung")
    }
}

/**
 * Aktualisiert Text und/oder Titel eines bestehenden Eintrags. Felder die als `null` uebergeben
 * werden, bleiben unveraendert. Wird sowohl vom Edit-Dialog (Text-Aenderung) als auch vom
 * Gemini-Auto-Titel (Title-Aenderung) genutzt — daher die optionalen Parameter.
 *
 * Frank-Wunsch 2026-05-23: Auch improvedText + isImproved werden hier durchgereicht damit die
 * KI-Nachbearbeitungs-Funktion einen einheitlichen Update-Pfad hat.
 */
internal suspend fun updateTagebuchEntry(
    context: Context,
    id: String,
    text: String? = null,
    title: String? = null,
    summary: String? = null,
    improvedText: String? = null,
    isImproved: Boolean? = null,
) {
    if (
        text == null &&
            title == null &&
            summary == null &&
            improvedText == null &&
            isImproved == null
    )
        return
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing.map { e ->
            if (e.id == id) {
                e.copy(
                    text = text ?: e.text,
                    title = title ?: e.title,
                    summary = summary ?: e.summary,
                    improvedText = improvedText ?: e.improvedText,
                    isImproved = isImproved ?: e.isImproved,
                    updatedAt = System.currentTimeMillis(),
                )
            } else {
                e
            }
        }
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Tagebuch/Journal: Aenderung")
}

/**
 * Speichert die KI-Verbesserung eines Followups (Frank-Wunsch 2026-05-23). Setzt improvedText +
 * isImproved=true; rawText bleibt unveraendert.
 */
internal suspend fun setTagebuchFollowupImproved(
    context: Context,
    entryId: String,
    followupId: String,
    improvedText: String,
) {
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing.map { e ->
            if (e.id != entryId) return@map e
            e.copy(
                followups =
                    e.followups.map { f ->
                        if (f.id == followupId)
                            f.copy(improvedText = improvedText, isImproved = true)
                        else f
                    },
                updatedAt = System.currentTimeMillis(),
            )
        }
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Tagebuch/Journal: Aenderung")
}

/**
 * Hängt einen Nachtrag an einen Tagebucheintrag an (Frank-Wunsch 2026-05-20). Der Nachtrag wird im
 * Detail-Screen als eigene Karte angezeigt.
 */
internal suspend fun addTagebuchFollowup(
    context: Context,
    entryId: String,
    followup: TagebuchFollowup,
) {
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing.map { e ->
            if (e.id == entryId)
                e.copy(followups = e.followups + followup, updatedAt = System.currentTimeMillis())
            else e
        }
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Tagebuch/Journal: Aenderung")
}

internal suspend fun updateTagebuchFollowup(
    context: Context,
    entryId: String,
    followupId: String,
    newText: String,
) {
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing.map { e ->
            if (e.id != entryId) return@map e
            e.copy(
                followups =
                    e.followups.map { f -> if (f.id == followupId) f.copy(text = newText) else f },
                updatedAt = System.currentTimeMillis(),
            )
        }
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Tagebuch/Journal: Aenderung")
}

internal suspend fun deleteTagebuchFollowup(context: Context, entryId: String, followupId: String) {
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing.map { e ->
            if (e.id != entryId) e
            else
                e.copy(
                    followups = e.followups.filterNot { it.id == followupId },
                    updatedAt = System.currentTimeMillis(),
                )
        }
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Tagebuch/Journal: Aenderung")
}

private fun parseEntries(raw: String?): List<TagebuchEntry> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) add(jsonToEntry(arr.getJSONObject(i)))
            }
        }
        .getOrDefault(emptyList())
}

private fun jsonToEntry(o: JSONObject): TagebuchEntry =
    TagebuchEntry(
        id = o.optString("id"),
        timestampMs = o.optLong("ts"),
        title = o.optString("title"),
        text = o.optString("text"),
        followups = jsonToFollowups(o.optJSONArray("followups")),
        summary = o.optString("summary").takeIf { it.isNotBlank() },
        improvedText = o.optString("improvedText").takeIf { it.isNotBlank() },
        isImproved = o.optBoolean("isImproved", false),
        // Edit-Sync: Bestand ohne updatedAt faellt auf ts (Erstellzeit) als Baseline zurueck.
        updatedAt = o.optLong("updatedAt", o.optLong("ts")),
    )

private fun jsonToFollowups(arr: JSONArray?): List<TagebuchFollowup> {
    if (arr == null) return emptyList()
    return buildList(arr.length()) {
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            add(
                TagebuchFollowup(
                    id = o.optString("id"),
                    createdAtMs = o.optLong("ts"),
                    text = o.optString("text"),
                    improvedText = o.optString("improvedText").takeIf { it.isNotBlank() },
                    isImproved = o.optBoolean("isImproved", false),
                )
            )
        }
    }
}

private fun serializeEntries(entries: List<TagebuchEntry>): String {
    val arr = JSONArray()
    for (e in entries) {
        val o = JSONObject()
        o.put("id", e.id)
        o.put("ts", e.timestampMs)
        o.put("updatedAt", e.updatedAt)
        o.put("title", e.title)
        o.put("text", e.text)
        if (e.followups.isNotEmpty()) {
            val fArr = JSONArray()
            for (f in e.followups) {
                val fo = JSONObject()
                fo.put("id", f.id)
                fo.put("ts", f.createdAtMs)
                fo.put("text", f.text)
                if (!f.improvedText.isNullOrBlank()) {
                    fo.put("improvedText", f.improvedText)
                }
                if (f.isImproved) fo.put("isImproved", true)
                fArr.put(fo)
            }
            o.put("followups", fArr)
        }
        if (!e.summary.isNullOrBlank()) {
            o.put("summary", e.summary)
        }
        if (!e.improvedText.isNullOrBlank()) {
            o.put("improvedText", e.improvedText)
        }
        if (e.isImproved) o.put("isImproved", true)
        arr.put(o)
    }
    return arr.toString()
}

internal fun formatTagebuchTimestamp(ts: Long): String {
    val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())
    return dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm", Locale.GERMANY))
}

private fun formatTimestamp(ts: Long): String = formatTagebuchTimestamp(ts)

/**
 * Akzentfarbe — Frank-Wunsch 2026-05-22 (zweite Iteration): exakt gleiche Frank-Wunsch 2026-06-09:
 * Farbe wie der Forscher-Tab-Sub-Modus in der BottomBar (Violett #A78BFA) — nach dem Umzug von
 * Aufgaben (orange) in den Forscher-Bereich.
 */
internal val TagebuchAccent: Color
    @Composable get() = LocalCosmos.current.accentForscher

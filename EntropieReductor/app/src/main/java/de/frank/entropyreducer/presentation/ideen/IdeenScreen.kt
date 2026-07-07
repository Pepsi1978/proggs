package de.frank.entropyreducer.presentation.ideen

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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
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
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.IdeaWithFollowups
import de.frank.entropyreducer.data.local.entities.IdeaEntity
import de.frank.entropyreducer.data.local.entities.IdeaFollowupEntity
import de.frank.entropyreducer.data.local.ideaDaoFrom
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.LocalMicActionsOpen
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Ideen-Bereich (Frank-Wunsch 2026-06-10) — Sub-Bereich 3 des Aufgaben-Tabs.
 *
 * 1:1-Klon des Entropie-/Tagebuch-Bereichs (Forscher-Slot 1): voller Funktionsumfang — Texteingabe
 * ueber Stift-Button, Whisper-Sprachaufnahme, Gemini-Text-Verbesserung, Auto-Ueberschrift,
 * KI-Zusammenfassung, TTS-Vorlesen, Detail-Screen mit Nachtraegen, Zeit-Timeline. Eigener DataStore
 * "ideen_entries" — voellig getrennt vom Tagebuch.
 *
 * Die App-Farbe folgt der Aufgaben-Akzentfarbe (Orange #EA580C), weil der Reiter dort liegt.
 * CosmosBottomBar laeuft im Sub-Mode mit forcedSubMode=TASKS, selectedSubIndex=3.
 */
@Composable
fun IdeenScreen(
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
    val entriesStream = remember(context) { ideenEntriesFlow(context) }
    val entries by entriesStream.collectAsStateWithLifecycle(initialValue = emptyList())

    var inputDialogOpen by remember { mutableStateOf(false) }
    // Frank-Wunsch 2026-05-18 Folgeauftrag: Erst nur Mic-Button anzeigen.
    // Klick auf Mic legt rechts "Aufnehmen" und links "Schreiben" frei.
    val micActions = LocalMicActionsOpen.current
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
    val improveVm: IdeenImproveViewModel = hiltViewModel()
    // Agentic Auto-Suggestion: Bei jeder neuen Idee automatisch Aufgaben + Gewohnheiten generieren
    val autoSuggestionVm: AutoSuggestionViewModel = hiltViewModel()
    val improveState by improveVm.state.collectAsStateWithLifecycle()
    val improvedText by improveVm.improvedText.collectAsStateWithLifecycle()
    val improveError by improveVm.error.collectAsStateWithLifecycle()
    // Frank-Wunsch 2026-05-19: Auto-Ueberschrift via Gemini (max 3 Woerter).
    // Wird nach jedem neuen Eintrag und nach jedem Edit asynchron gesetzt.
    val titleVm: IdeenTitleViewModel = hiltViewModel()
    // Frank-Wunsch 2026-05-20: Auto-Zusammenfassung (3-5 Bullet-Points) via Gemini.
    // Wird nach jedem neuen Eintrag asynchron erzeugt — Detail-Screen zeigt sie als
    // Bullet-Points statt eines leeren Platzhalters.
    val summaryVm: IdeenSummaryViewModel = hiltViewModel()
    // Frank-Wunsch 2026-07-04: Beim Betreten des Ideen-Reiters SOFORT neue Ideen aus dem Second
    // Brain holen (Brain->App) — ohne auf den naechsten App-Start oder einen Timer zu warten.
    val brainSyncVm: IdeenBrainSyncViewModel = hiltViewModel()
    LaunchedEffect(Unit) { brainSyncVm.pullNow() }
    var pendingTranscript by remember { mutableStateOf<String?>(null) }
    val micPermission =
        rememberMicPermissionState(
            onAllGranted = {
                voiceVm.toggle { transcript ->
                    pendingTranscript = transcript
                    micActions.close()
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
        val base = IdeenEntry.create(rawText)
        val entry =
            if (!improved.isNullOrBlank()) {
                base.copy(improvedText = improved, isImproved = preferImproved)
            } else {
                base
            }
        scope.launch {
            addIdeenEntry(context, entry)
            // Agentic Auto-Suggestion: Erst NACH dem Speichern der Idee Vorschläge generieren,
            // damit die neue Idee im DataStore verfügbar ist (Frank-Wunsch 2026-06-18).
            autoSuggestionVm.triggerSuggestions()
        }
        // Titel/Zusammenfassung aus dem primaer angezeigten Text ableiten (bessere Qualitaet).
        val primaryText = if (preferImproved && !improved.isNullOrBlank()) improved else rawText
        titleVm.generateTitle(primaryText) { newTitle ->
            scope.launch { updateIdeenEntry(context, entry.id, title = newTitle) }
        }
        summaryVm.generateSummary(primaryText) { bullets ->
            scope.launch { updateIdeenEntry(context, entry.id, summary = bullets) }
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
        title = "Ideen",
        showBottomBar = showBottomBar,
        bottomBar = {
            CosmosBottomBar(
                currentTab = Routes.TASKS,
                micState =
                    when (voiceState) {
                        VoiceCaptureState.RECORDING -> MicState.RECORDING
                        VoiceCaptureState.PROCESSING -> MicState.PROCESSING
                        VoiceCaptureState.IDLE -> MicState.IDLE
                    },
                onTabSelected = { route -> onSwitchTab(route) },
                // Der untere Mic-Button in der BottomBar ist der EINZIGE Mic-Knopf. Sein
                // Klick faltet die zwei Aktions-Buttons (Stift + Aufnehmen-Mic)
                // direkt darueber auf — vorher ist im Ideen-Bereich KEIN Mikrofon zu sehen.
                onMicClick = { micActions.toggle() },
                onSubAreaSelected = { parent, index -> onSwitchSub(parent, index) },
                forcedSubMode = Routes.TASKS,
                // Frank-Wunsch 2026-06-10: Ideen ist Sub-Bereich 3 unter Aufgaben → dauerhaft
                // hervorheben.
                selectedSubIndex = 3,
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
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = IdeenAccent,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Deine Ideen",
                        style = MaterialTheme.typography.titleLarge,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text =
                            "Hier kannst du deine Ideen, Einfälle und Gedanken festhalten. " +
                                "Tippe auf den Stift um deine erste Idee zu erstellen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cosmos.textSecondary,
                    )
                }
            } else {
                // Frank-Wunsch 2026-05-23: Gruppierung nach Zeit-Sektionen wie
                // BestJournalFrank's Ideen — Heute / Gestern / Diese Woche /
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
            // KEIN eigener Mic-FAB im Ideen-Bereich. Der BottomBar-Mic-
            // Button (in der Statusleiste unten) ist der einzige Mic-Trigger.
            // Sein Klick setzt actionsExpanded=true → es erscheinen direkt
            // ueber der BottomBar (rechts: Aufnehmen-Mic, links: Schreiben).
            // Standardmaessig ist im Ideen-Bereich NICHTS zu sehen.
            if (micActions.isOpen) {
                val actionAccent = LocalCosmos.current.accent
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    FabIconButton(
                        icon = Icons.Outlined.Edit,
                        label = "Schreiben",
                        backgroundColor = actionAccent.copy(alpha = 0.7f),
                        // Frank-Wunsch 2026-05-22 (dritte Iteration): Icon schwarz
                        // wie der zentrale BottomBar-Mic.
                        iconTint = Color.Black,
                        onClick = {
                            inputDialogOpen = true
                            micActions.close()
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
                        backgroundColor = actionAccent.copy(alpha = 0.7f),
                        iconTint = Color.Black,
                        onClick = {
                            when (voiceState) {
                                VoiceCaptureState.IDLE -> {
                                    if (micPermission.check()) {
                                        voiceVm.toggle { transcript ->
                                            pendingTranscript = transcript
                                            micActions.close()
                                        }
                                    } else {
                                        micPermission.request()
                                    }
                                }
                                VoiceCaptureState.RECORDING -> {
                                    voiceVm.toggle { transcript ->
                                        pendingTranscript = transcript
                                        micActions.close()
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
            color = IdeenAccent,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.size(8.dp))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(IdeenAccent.copy(alpha = 0.25f)))
    }
}

/**
 * Eintrag plus Timeline-Rail links — 52dp Spalte mit durchgehender Linie und zentriertem
 * Buch-Badge. Frank-Wunsch 2026-05-23 (Vorbild BestJournalFrank).
 */
@Composable
private fun TimelineEntryRow(entry: IdeenEntry, position: TimelinePosition, onClick: () -> Unit) {
    val cosmos = LocalCosmos.current
    val lineColor = IdeenAccent.copy(alpha = 0.35f)
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
                        .background(IdeenAccent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = IdeenAccent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        GlassCard(modifier = Modifier.weight(1f).clickable { onClick() }) {
            Column {
                Text(
                    text = entry.title.ifBlank { "Idee" },
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
private data class IdeenSection(val label: String, val entries: List<IdeenEntry>)

/**
 * Gruppiert Eintraege nach Zeit-Sektionen (Frank-Wunsch 2026-05-23). Reihenfolge: Heute, Gestern,
 * Diese Woche, Letzte Woche, Vor 2/3/4 Wochen, Monatsname, Jahr — Monat. Innerhalb der Sektion
 * bleiben die Eintraege nach Timestamp absteigend sortiert. Erwartet eine bereits nach Timestamp
 * absteigend sortierte Eingabe.
 */
private fun groupEntriesBySection(entries: List<IdeenEntry>): List<IdeenSection> {
    if (entries.isEmpty()) return emptyList()
    val sorted = entries.sortedByDescending { it.timestampMs }
    val buckets = linkedMapOf<String, MutableList<IdeenEntry>>()
    sorted.forEach { e ->
        val label = sectionLabelFor(e.timestampMs)
        buckets.getOrPut(label) { mutableListOf() }.add(e)
    }
    return buckets.map { (label, list) -> IdeenSection(label, list) }
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
        title = { Text("Neue Idee") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                placeholder = { Text("Schreibe deine Idee, deinen Einfall oder Gedanken auf ...") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onSave(text.trim()) },
                enabled = text.isNotBlank(),
            ) {
                Text("Speichern", color = IdeenAccent)
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
                            color = IdeenAccent,
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
                Text("Speichern", color = IdeenAccent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

/**
 * Detail-Dialog fuer einen Ideeneintrag.
 *
 * Frank-Wunsch 2026-05-19:
 * - Text ist bearbeitbar (OutlinedTextField) — Fehler im Eintrag koennen nachtraeglich korrigiert
 *   werden.
 * - Rechts oben ein 3-Punkte-Menue mit "Eintrag loeschen".
 * - Speichern-Button erscheint nur wenn der Text geaendert wurde.
 */
@Composable
private fun EntryDetailDialog(
    entry: IdeenEntry,
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
                    text = entry.title.ifBlank { "Idee" },
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
                    Text("Speichern", color = IdeenAccent)
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

data class IdeenEntry(
    val id: String,
    val timestampMs: Long,
    val title: String,
    val text: String,
    /**
     * Nachträge zum Eintrag (Frank-Wunsch 2026-05-20). Werden im Vollbild- Detail-Screen als eigene
     * Karten angezeigt. Bei alten Einträgen ohne dieses Feld liefert der JSON-Parser eine leere
     * Liste.
     */
    val followups: List<IdeenFollowup> = emptyList(),
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
     * Zeitpunkt der letzten Bearbeitung (Edit-Sync, Phase B 2026-06-20). Nullable, weil die
     * Room-Spalte `ideas.updatedAt` in Phase A additiv-nullable kam (Bestand = null). Wird bei jeder
     * Aenderung (Text/Titel/Summary/Improved + Followup-Aenderungen am Parent) auf `now` gesetzt und
     * treibt den geraeteuebergreifenden Last-Write-Wins. Baseline bei null = `timestampMs`.
     */
    val updatedAt: Long? = null,
) {
    companion object {
        fun create(text: String): IdeenEntry {
            // Title = erste 30-40 Zeichen oder erste Zeile, ohne Punkt-Suffix.
            val firstLine = text.lineSequence().firstOrNull().orEmpty()
            val title =
                if (firstLine.length <= 40) firstLine.trim() else firstLine.take(40).trim() + "…"
            val now = System.currentTimeMillis()
            return IdeenEntry(
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
 * Einzelner Nachtrag zu einem [IdeenEntry]. Hat ebenfalls einen eigenen KI-verbesserten Text
 * (Frank-Wunsch 2026-05-23) damit jeder Nachtrag separat nachgeschliffen werden kann.
 */
data class IdeenFollowup(
    val id: String,
    val createdAtMs: Long,
    val text: String,
    val improvedText: String? = null,
    val isImproved: Boolean = false,
)

// -------------------------------------------------------------------------------------------------
// Persistenz: Ideen liegen ab ID-Architektur Etappe 2c (Frank-Wunsch 2026-06-19) in Room
// (Tabellen `ideas` + `idea_followups`) statt im DataStore-JSON. Die oeffentlichen Signaturen
// bleiben 1:1 — ALLE Aufrufer (Compose-UI, Migrator, AutoSuggestion/KiTaskSuggest-ViewModels,
// SyncCoordinator, SyncEntriesUseCase) laufen unveraendert weiter. Der DAO kommt per Hilt-
// @EntryPoint (ideaDaoFrom). triggerDriveBackup-Aufrufe bleiben 1:1 erhalten -> Drive-Backup und
// Sync ziehen automatisch nach (Etappe 2e), weil sie ueber dieselbe ideenEntriesFlow/addIdeenEntry
// laufen.
//
// Der DataStore "ideen_entries" + die JSON-Lesekette (ideenEntriesFromJsonFlow) bleiben als
// Quelle fuer den einmaligen Room-Migrator (IdeaTaskRoomMigrator, Etappe 2b) erhalten.
// -------------------------------------------------------------------------------------------------

private val Context.ideenStore by preferencesDataStore(name = "ideen_entries")
private val KEY_ENTRIES = stringPreferencesKey("entries_json")
private const val IDEEN_TAG = "EREIdeenPersist"

/** Room-Zeile (Idee + Nachtraege) -> UI-Modell. Followups nach Zeit (Room sortiert @Relation nicht). */
private fun IdeaWithFollowups.toIdeenEntry(): IdeenEntry =
    IdeenEntry(
        id = idea.id,
        timestampMs = idea.timestampMs,
        title = idea.title,
        text = idea.text,
        followups =
            followups
                .sortedBy { it.createdAtMs }
                .map {
                    IdeenFollowup(
                        id = it.id,
                        createdAtMs = it.createdAtMs,
                        text = it.text,
                        improvedText = it.improvedText,
                        isImproved = it.isImproved,
                    )
                },
        summary = idea.summary,
        improvedText = idea.improvedText,
        isImproved = idea.isImproved,
        updatedAt = idea.updatedAt,
    )

/**
 * UI-Modell -> Room-Idee. Herkunft (originId/originType/rootId) wird hier NICHT gesetzt — sie kommt
 * in Etappe 2d und wird dort gezielt durchgereicht; ein bestehender Wert darf nicht ueberschrieben
 * werden. Beim Anlegen einer frisch eingesprochenen Idee ist null korrekt (= Ursprung der Kette).
 */
private fun IdeenEntry.toEntity(): IdeaEntity =
    IdeaEntity(
        id = id,
        timestampMs = timestampMs,
        title = title,
        text = text,
        summary = summary,
        improvedText = improvedText,
        isImproved = isImproved,
        // Edit-Sync: vorhandenen updatedAt durchtragen; bei null (Bestand/Migrator) Erstellzeit als
        // Baseline (NICHT now — sonst wuerden migrierte Bestands-Ideen faelschlich als frisch geaendert gelten).
        updatedAt = updatedAt ?: timestampMs,
    )

private fun IdeenFollowup.toEntity(ideaId: String): IdeaFollowupEntity =
    IdeaFollowupEntity(
        id = id,
        ideaId = ideaId,
        createdAtMs = createdAtMs,
        text = text,
        improvedText = improvedText,
        isImproved = isImproved,
    )

/** Lesequelle des Ideen-Reiters: reaktiver Flow aus Room (Etappe 2c). */
internal fun ideenEntriesFlow(context: Context): Flow<List<IdeenEntry>> =
    ideaDaoFrom(context)
        .getAllIdeasWithFollowups()
        .map { rows -> rows.map { it.toIdeenEntry() } }
        .distinctUntilChanged()

/** Beobachtbarer Flow eines einzelnen Eintrags — für den Vollbild-Detail-Screen. */
internal fun ideenEntryFlow(context: Context, id: String): Flow<IdeenEntry?> =
    ideenEntriesFlow(context).map { list -> list.firstOrNull { it.id == id } }

internal suspend fun addIdeenEntry(context: Context, entry: IdeenEntry) {
    ideaDaoFrom(context)
        .upsertIdeaWithFollowups(entry.toEntity(), entry.followups.map { it.toEntity(entry.id) })
    Diag.d(
        DiagnosticArea.DATABASE,
        IDEEN_TAG,
        "CHECKPOINT step=addIdee id=${entry.id} followups=${entry.followups.size}",
    )
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Ideen-Reiter: Aenderung")
}

internal suspend fun deleteIdeenEntry(context: Context, id: String, propagate: Boolean = true) {
    // CASCADE (ForeignKey in IdeaFollowupEntity) loescht zugehoerige idea_followups automatisch.
    ideaDaoFrom(context).deleteIdeaById(id)
    Diag.d(DiagnosticArea.DATABASE, IDEEN_TAG, "CHECKPOINT step=deleteIdee id=$id")
    // Frank-Wunsch 2026-06-20: Loeschung propagieren (Tombstone). propagate=false: Restore-Cleanup.
    if (propagate) {
        // Bugfix 2026-06-20: abgeleitete OFFENE Vorschlaege dieser Idee sofort wegraeumen — sonst
        // bleiben sie im UI sichtbar und der verwaiste Vorschlag taucht nach dem naechsten Restore
        // wieder auf (Root Cause: Vorschlags-Heal kannte nur "Idee angenommen", nicht "Idee geloescht").
        // Der IDEE-Tombstone + der Verwaist-Filter (isRootIdeaDeleted) verhindern das Wiederkommen
        // geraeteuebergreifend; dieses lokale Wegraeumen ist die sofortige UI-Seite.
        val habitSugDao = de.frank.entropyreducer.data.local.habitSuggestionDaoFrom(context)
        for (s in habitSugDao.getAllForBackup()) if (s.rootId == id) habitSugDao.deleteById(s.id)
        val taskSugDao = de.frank.entropyreducer.data.local.taskSuggestionDaoFrom(context)
        for (s in taskSugDao.getAllForBackup()) if (s.rootId == id) taskSugDao.deleteById(s.id)
        de.frank.entropyreducer.data.markDeleted(
            context, de.frank.entropyreducer.data.TombstoneType.IDEE, id)
        de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Ideen-Reiter: Aenderung")
    }
}

/**
 * Aktualisiert Text und/oder Titel eines bestehenden Eintrags. Felder die als `null` uebergeben
 * werden, bleiben unveraendert. Wird sowohl vom Edit-Dialog (Text-Aenderung) als auch vom
 * Gemini-Auto-Titel (Title-Aenderung) genutzt — daher die optionalen Parameter.
 *
 * Frank-Wunsch 2026-05-23: Auch improvedText + isImproved werden hier durchgereicht damit die
 * KI-Nachbearbeitungs-Funktion einen einheitlichen Update-Pfad hat. Herkunfts-Felder bleiben
 * unangetastet (copy auf die geladene Entity).
 */
internal suspend fun updateIdeenEntry(
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
    val dao = ideaDaoFrom(context)
    val current = dao.getIdeaById(id) ?: return
    dao.updateIdea(
        current.copy(
            text = text ?: current.text,
            title = title ?: current.title,
            summary = summary ?: current.summary,
            improvedText = improvedText ?: current.improvedText,
            isImproved = isImproved ?: current.isImproved,
            updatedAt = System.currentTimeMillis(),
        )
    )
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Ideen-Reiter: Aenderung")
}

/**
 * Speichert die KI-Verbesserung eines Followups (Frank-Wunsch 2026-05-23). Setzt improvedText +
 * isImproved=true; rawText bleibt unveraendert.
 */
internal suspend fun setIdeenFollowupImproved(
    context: Context,
    entryId: String,
    followupId: String,
    improvedText: String,
) {
    val dao = ideaDaoFrom(context)
    val followup = dao.getFollowupsForIdea(entryId).firstOrNull { it.id == followupId } ?: return
    dao.upsertFollowups(listOf(followup.copy(improvedText = improvedText, isImproved = true)))
    // Edit-Sync: Parent-Idee als geaendert markieren, sonst propagiert die Followup-Aenderung nicht.
    dao.getIdeaById(entryId)?.let { dao.updateIdea(it.copy(updatedAt = System.currentTimeMillis())) }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Ideen-Reiter: Aenderung")
}

/**
 * Hängt einen Nachtrag an einen Ideeneintrag an (Frank-Wunsch 2026-05-20). Der Nachtrag wird im
 * Detail-Screen als eigene Karte angezeigt.
 */
internal suspend fun addIdeenFollowup(context: Context, entryId: String, followup: IdeenFollowup) {
    val dao = ideaDaoFrom(context)
    dao.upsertFollowups(listOf(followup.toEntity(entryId)))
    // Edit-Sync: Parent-Idee als geaendert markieren, sonst propagiert der neue Nachtrag nicht.
    dao.getIdeaById(entryId)?.let { dao.updateIdea(it.copy(updatedAt = System.currentTimeMillis())) }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Ideen-Reiter: Aenderung")
}

internal suspend fun updateIdeenFollowup(
    context: Context,
    entryId: String,
    followupId: String,
    newText: String,
) {
    val dao = ideaDaoFrom(context)
    val followup = dao.getFollowupsForIdea(entryId).firstOrNull { it.id == followupId } ?: return
    dao.upsertFollowups(listOf(followup.copy(text = newText)))
    // Edit-Sync: Parent-Idee als geaendert markieren, sonst propagiert die Nachtrag-Aenderung nicht.
    dao.getIdeaById(entryId)?.let { dao.updateIdea(it.copy(updatedAt = System.currentTimeMillis())) }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Ideen-Reiter: Aenderung")
}

internal suspend fun deleteIdeenFollowup(context: Context, entryId: String, followupId: String) {
    val dao = ideaDaoFrom(context)
    dao.deleteFollowupById(followupId)
    // Edit-Sync: Parent-Idee als geaendert markieren, sonst propagiert die Nachtrag-Loeschung nicht.
    dao.getIdeaById(entryId)?.let { dao.updateIdea(it.copy(updatedAt = System.currentTimeMillis())) }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Ideen-Reiter: Aenderung")
}

/**
 * Edit-Sync (Phase B 2026-06-20): ersetzt eine BEREITS LOKAL vorhandene Idee per Last-Write-Wins
 * mit dem eingehenden Stand aus dem Drive-Backup — OHNE die Herkunft (originId/originType/rootId)
 * zu verlieren. Die Herkunft lebt nur in Room (IdeaEntity), NICHT im Backup-DTO/UI-Modell; ein
 * naives addIdeenEntry()/upsert wuerde sie auf null setzen (gleiche Falle wie der v19-Gewohnheits-Fix).
 * Daher: geladene Entity per copy() aktualisieren (Herkunft bleibt) + Nachtraege komplett ersetzen.
 * Setzt KEINEN neuen updatedAt (uebernimmt den eingehenden), damit der LWW-Vergleich stabil bleibt.
 */
internal suspend fun replaceIdeenEntryFromSync(context: Context, incoming: IdeenEntry) {
    val dao = ideaDaoFrom(context)
    val current = dao.getIdeaById(incoming.id) ?: return
    dao.updateIdea(
        current.copy(
            timestampMs = incoming.timestampMs,
            title = incoming.title,
            text = incoming.text,
            summary = incoming.summary,
            improvedText = incoming.improvedText,
            isImproved = incoming.isImproved,
            updatedAt = incoming.updatedAt ?: incoming.timestampMs,
        )
    )
    // Nachtraege komplett ersetzen (Edit-Sync deckt auch Nachtrag-Aenderungen ab). Nur vorhandene
    // DAO-Methoden — kein Schema-Eingriff.
    for (f in dao.getFollowupsForIdea(incoming.id)) dao.deleteFollowupById(f.id)
    if (incoming.followups.isNotEmpty()) {
        dao.upsertFollowups(incoming.followups.map { it.toEntity(incoming.id) })
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Ideen-Reiter: Aenderung")
}

// --- JSON-Lesekette: NUR noch fuer den einmaligen Room-Migrator (IdeaTaskRoomMigrator, Etappe 2b) ---

/**
 * Liest die Bestands-Ideen aus dem alten DataStore-JSON ("ideen_entries"). NUR fuer den einmaligen
 * Umzug nach Room. Der Ideen-Reiter selbst liest ab Etappe 2c aus Room (ideenEntriesFlow). Diese
 * Funktion NICHT fuer UI/Backup/Sync verwenden — sie spiegelt den eingefrorenen Alt-Stand.
 */
internal fun ideenEntriesFromJsonFlow(context: Context): Flow<List<IdeenEntry>> =
    context.ideenStore.data.map { prefs ->
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

private fun jsonToEntry(o: JSONObject): IdeenEntry =
    IdeenEntry(
        id = o.optString("id"),
        timestampMs = o.optLong("ts"),
        title = o.optString("title"),
        text = o.optString("text"),
        followups = jsonToFollowups(o.optJSONArray("followups")),
        summary = o.optString("summary").takeIf { it.isNotBlank() },
        improvedText = o.optString("improvedText").takeIf { it.isNotBlank() },
        isImproved = o.optBoolean("isImproved", false),
    )

private fun jsonToFollowups(arr: JSONArray?): List<IdeenFollowup> {
    if (arr == null) return emptyList()
    return buildList(arr.length()) {
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            add(
                IdeenFollowup(
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

internal fun formatIdeenTimestamp(ts: Long): String {
    val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())
    return dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm", Locale.GERMANY))
}

private fun formatTimestamp(ts: Long): String = formatIdeenTimestamp(ts)

/**
 * Akzentfarbe — Blau (Frank-Wunsch 2026-06-18): Sub-Tabs unter Aufgaben sind blau.
 * TTS-Buttons bleiben Orange (cosmos.accent).
 */
internal val IdeenAccent: Color
    @Composable get() = LocalCosmos.current.accentTasksSub

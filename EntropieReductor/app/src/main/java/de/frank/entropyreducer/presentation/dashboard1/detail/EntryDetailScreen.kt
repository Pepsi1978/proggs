package de.frank.entropyreducer.presentation.dashboard1.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.data.local.entities.EntropyEntryFollowupEntity
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.WhisperMicButton
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Vollbild-Detail-Screen einer Entropie-Aufgabe (Frank-Wunsch 2026-05-20).
 *
 * Aufbau-Reduktion 2026-05-22 (Frank-Wunsch): Zusammenfassung-Karte, Status-Buttons und
 * Vorlese-Zeile sind raus — sie reduzierten nicht die Entropie, sie erhoehten sie.
 *
 * Aktueller Aufbau:
 * 1. TopAppBar mit Zurück-Pfeil
 * 2. Hero-Karte (Kategorie, Title, Schweregrad, Prio-Score, Severity-Bar)
 * 3. Tags
 * 4. KI-Begründung + KI-Notizen
 * 5. Nachträge als eigene Karten (mit Lösch-Button pro Nachtrag)
 * 6. Nachtrag-Aufnahme via Mic (nur Label "Nachtrag einsprechen", kein Untertitel)
 * 7. Löschen-Button
 */
@Composable
fun EntryDetailScreen(onBack: () -> Unit, viewModel: EntryDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onBack() }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }
    // Frank-Wunsch 2026-05-22: Snackbar nach "Zu Loop hinzufügen"
    LaunchedEffect(Unit) {
        viewModel.templateCreated.collect { success ->
            snackbar.showSnackbar(
                if (success) "Findest du jetzt im Reiter Loop."
                else "Konnte keine wiederkehrende Aufgabe anlegen."
            )
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = topInset)) {
            // TopAppBar — bewusst handgemacht, damit der Stil 1:1 zu Journal
            // passt (kein Material3-TopAppBar wegen der Cosmos-Farben).
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Zurück",
                        tint = cosmos.textPrimary,
                    )
                }
                Text(
                    text = "Eintrag",
                    style = MaterialTheme.typography.titleLarge,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            val entry = state.entry
            if (entry == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LocalCosmos.current.accent)
                }
                return@Column
            }

            // Frank-Wunsch 2026-05-31: Titel oben ist antippbar und inline editierbar.
            // Der gespeicherte Titel erscheint dank gemeinsamer DB auch in der Liste.
            var editingTitle by remember(entry.id) { mutableStateOf(false) }
            var titleDraft by remember(entry.id) { mutableStateOf(entry.title) }
            var editingText by remember(entry.id) { mutableStateOf(false) }
            var textDraft by
                remember(entry.id) { mutableStateOf(entry.rawTranscript.ifBlank { entry.description }) }

            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = bottomInset + 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Der ursprünglich eingegebene Aufgabentext bleibt unter dem Titel sichtbar.
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        if (editingTitle) {
                            androidx.compose.material3.OutlinedTextField(
                                value = titleDraft,
                                onValueChange = { titleDraft = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle =
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                colors =
                                    androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = cosmos.textPrimary,
                                        unfocusedTextColor = cosmos.textPrimary,
                                        focusedBorderColor = LocalCosmos.current.accent,
                                        unfocusedBorderColor = cosmos.glassBorder,
                                        cursorColor = LocalCosmos.current.accent,
                                    ),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            viewModel.updateTitle(titleDraft)
                                            editingTitle = false
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Titel speichern",
                                            tint = LocalCosmos.current.accent,
                                        )
                                    }
                                },
                            )
                        } else {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = cosmos.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier =
                                    Modifier.clickable {
                                        titleDraft = entry.title
                                        editingTitle = true
                                    },
                            )
                        }
                        val taskText = entry.rawTranscript.ifBlank { entry.description }
                        if (editingText) {
                            Spacer(Modifier.height(6.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = textDraft,
                                onValueChange = { textDraft = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                colors =
                                    androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = cosmos.textPrimary,
                                        unfocusedTextColor = cosmos.textPrimary,
                                        focusedBorderColor = LocalCosmos.current.accent,
                                        unfocusedBorderColor = cosmos.glassBorder,
                                        cursorColor = LocalCosmos.current.accent,
                                    ),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            viewModel.updateTaskText(textDraft)
                                            editingText = false
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Aufgabentext speichern",
                                            tint = LocalCosmos.current.accent,
                                        )
                                    }
                                },
                            )
                        } else {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text =
                                    taskText.takeIf { it.isNotBlank() && it != entry.title }
                                        ?: "Aufgabentext hinzufügen",
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                    if (taskText.isNotBlank() && taskText != entry.title) {
                                        cosmos.textPrimary
                                    } else {
                                        cosmos.textSecondary
                                    },
                                modifier =
                                    Modifier.clickable {
                                        textDraft = taskText
                                        editingText = true
                                    },
                            )
                        }
                    }
                }

                // ── 6. Nachträge als eigene Karten ──
                state.followups.forEachIndexed { index, followup ->
                    FollowupCard(
                        followup = followup,
                        index = index + 1,
                        onTextChange = { newText ->
                            viewModel.updateFollowupText(followup.id, newText)
                        },
                        onDelete = { viewModel.deleteFollowup(followup.id) },
                    )
                }

                // ── 5. Nachtrag hinzufügen via Whisper ──
                // Untertitel "Whisper Large V3 Turbo" entfernt 2026-05-22 (Frank-Wunsch) —
                // nur noch der schlichte Label "Nachtrag einsprechen".
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            tint = LocalCosmos.current.accent,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Nachtrag einsprechen",
                            color = cosmos.textPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        WhisperMicButton(
                            onTranscript = { transcript -> viewModel.addFollowup(transcript) },
                            size = 44.dp,
                        )
                    }
                }

                // Vorlese-Zeile "Eintrag + Nachträge vorlesen" entfernt 2026-05-22
                // (Frank-Wunsch). TTS-Funktion bleibt im ViewModel falls spaeter
                // wieder gebraucht, wird aktuell aber nicht mehr aus dem UI getriggert.

                // ── 6a. "Zu Loop hinzufügen"-Button (Frank-Wunsch 2026-05-22) ──
                // Vor dem Löschen-Button, damit Frank versehentlich nicht
                // statt "Löschen" "Hinzufügen" trifft. Akzent-Farbe (orange)
                // damit der visuelle Kontrast zum roten Löschen-Button klar
                // ist.
                androidx.compose.material3.OutlinedButton(
                    onClick = { viewModel.convertToRecurringTemplate() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors =
                        androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = LocalCosmos.current.accent
                        ),
                    border =
                        androidx.compose.foundation.BorderStroke(
                            width = 1.5.dp,
                            color = LocalCosmos.current.accent,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Repeat,
                        contentDescription = null,
                        tint = LocalCosmos.current.accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Aufgabe zu wiederkehrenden hinzufügen",
                        color = LocalCosmos.current.accent,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── 6b. Löschen-Button ──
                Button(
                    onClick = { viewModel.deleteEntry() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = LocalCosmos.current.crit,
                            contentColor = Color.White,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Eintrag löschen",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        ) {
            Snackbar(it)
        }

        if (state.isAddingFollowup) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f))
                        .clickable(enabled = false, onClick = {}),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = LocalCosmos.current.accent)
            }
        }
    }
}

/**
 * Diskrete, exponentiell wachsende Stop-Liste fuer den Zeitaufwand-Regler (Frank-Wunsch
 * 2026-05-22): am Anfang feine Minuten-Schritte, dann Stunden, Tage, Wochen.
 *
 * 24 Stops decken alles von 5 min bis 4 Wochen ab.
 */
private val DURATION_STOPS: IntArray =
    intArrayOf(
        5,
        10,
        15,
        20,
        30,
        45, // Minuten (6)
        60,
        90,
        120,
        180,
        240,
        300,
        420,
        600,
        720, // Stunden 1-12h (9)
        1440,
        2160,
        2880,
        4320,
        5760,
        7200, // Tage 1-5 (6)
        10080,
        20160,
        40320, // Wochen 1, 2, 4 (3)
    )

private fun durationStopIndexFor(minutes: Int?): Int {
    if (minutes == null) return -1
    val idx = DURATION_STOPS.indexOfFirst { it >= minutes }
    return if (idx < 0) DURATION_STOPS.lastIndex else idx
}

private fun formatDuration(minutes: Int): String =
    when {
        minutes < 60 -> "$minutes min"
        minutes < 1440 -> {
            val h = minutes / 60
            val rest = minutes % 60
            if (rest == 0) "$h h" else "$h h $rest min"
        }
        minutes < 10080 -> {
            val d = minutes / 1440
            val restH = (minutes % 1440) / 60
            if (restH == 0) "$d Tag${if (d > 1) "e" else ""}"
            else "$d Tag${if (d > 1) "e" else ""} $restH h"
        }
        else -> {
            val w = minutes / 10080
            val restD = (minutes % 10080) / 1440
            if (restD == 0) "$w Woche${if (w > 1) "n" else ""}"
            else "$w Woche${if (w > 1) "n" else ""} $restD Tag${if (restD > 1) "e" else ""}"
        }
    }

@Composable
private fun DurationEstimateCard(
    currentMinutes: Int?,
    isAiEstimate: Boolean,
    onChange: (Int?) -> Unit,
) {
    val cosmos = LocalCosmos.current
    val initialStop = remember(currentMinutes) { durationStopIndexFor(currentMinutes) }
    // Slider-Position in float Stop-Index. -1 = unbestimmt.
    var sliderPos by
        remember(initialStop) { mutableStateOf(if (initialStop < 0) 0f else initialStop.toFloat()) }
    var manualText by remember(currentMinutes) { mutableStateOf(currentMinutes?.toString() ?: "") }
    // Frank-Wunsch 2026-05-22 (zweite Iteration): WAEHREND des Schiebens soll die
    // Anzeige live aktualisieren. Wir tracken den Drag-Status: solange Frank den
    // Finger auf dem Slider hat, zeigen wir sliderMinutes. Erst beim Loslassen
    // wird onChange aufgerufen und currentMinutes uebernommen.
    var isDragging by remember { mutableStateOf(false) }
    val sliderMinutes =
        remember(sliderPos) {
            DURATION_STOPS[sliderPos.toInt().coerceIn(0, DURATION_STOPS.lastIndex)]
        }
    // Live-Wert prioritisieren: solange gerade gezogen wird, IMMER sliderMinutes.
    // Sonst: currentMinutes (manuell gesetzt) > sliderMinutes (gerundeter Stop).
    val displayMinutes = if (isDragging) sliderMinutes else (currentMinutes ?: sliderMinutes)
    val isUnset = currentMinutes == null && !isDragging

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Zeitaufwand",
                    style = MaterialTheme.typography.titleSmall,
                    color = LocalCosmos.current.accent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    when {
                        isUnset -> "ohne Schaetzung"
                        isAiEstimate && !isDragging -> "≈ ${formatDuration(displayMinutes)} · KI"
                        else -> "≈ ${formatDuration(displayMinutes)}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isUnset) cosmos.textSecondary else cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            androidx.compose.material3.Slider(
                value = sliderPos,
                onValueChange = { v ->
                    sliderPos = v
                    isDragging = true
                },
                onValueChangeFinished = {
                    val snapped = sliderPos.toInt().coerceIn(0, DURATION_STOPS.lastIndex)
                    sliderPos = snapped.toFloat()
                    val m = DURATION_STOPS[snapped]
                    manualText = m.toString()
                    isDragging = false
                    onChange(m)
                },
                valueRange = 0f..(DURATION_STOPS.lastIndex.toFloat()),
                steps = DURATION_STOPS.size - 2,
                colors =
                    androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = LocalCosmos.current.accent,
                        activeTrackColor = LocalCosmos.current.accent,
                        inactiveTrackColor = cosmos.glassBorder,
                    ),
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "5 min",
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "4 Wochen",
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.OutlinedTextField(
                    value = manualText,
                    onValueChange = { manualText = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Manuell in Minuten") },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                    modifier = Modifier.weight(1f),
                    colors =
                        androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = cosmos.textPrimary,
                            unfocusedTextColor = cosmos.textPrimary,
                            focusedBorderColor = LocalCosmos.current.accent,
                            unfocusedBorderColor = cosmos.glassBorder,
                        ),
                )
                Spacer(Modifier.width(8.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        val parsed = manualText.toIntOrNull()
                        if (parsed != null && parsed > 0) {
                            onChange(parsed)
                            sliderPos = durationStopIndexFor(parsed).toFloat().coerceAtLeast(0f)
                        }
                    }
                ) {
                    Text("Setzen")
                }
                Spacer(Modifier.width(6.dp))
                androidx.compose.material3.TextButton(
                    onClick = {
                        onChange(null)
                        manualText = ""
                        sliderPos = 0f
                    }
                ) {
                    Text("Loeschen", color = cosmos.textSecondary)
                }
            }
        }
    }
}

/**
 * Frank-Wunsch 2026-05-22 (dritte Iteration): Frist-Karte mit Kalender-Symbol. Tap auf das Symbol
 * oeffnet einen Material-DatePicker. Anzeige der Restzeit direkt unter dem Datum — z.B. "in 3
 * Tagen" oder "ueberfaellig seit 2 h".
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DueDateCard(dueAtMs: Long?, onChange: (Long?) -> Unit) {
    val cosmos = LocalCosmos.current
    var showPicker by remember { mutableStateOf(false) }
    val nowMs = remember { System.currentTimeMillis() }
    val labelDate =
        remember(dueAtMs) {
            dueAtMs?.let {
                val df = java.text.SimpleDateFormat("d. MMM yyyy", java.util.Locale.GERMAN)
                df.format(java.util.Date(it))
            }
        }
    val labelRelative =
        remember(dueAtMs, nowMs) { dueAtMs?.let { formatRelativeDeadline(it - nowMs) } }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Frist",
                    style = MaterialTheme.typography.titleSmall,
                    color = LocalCosmos.current.accent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showPicker = true }) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "Frist setzen",
                        tint = LocalCosmos.current.accent,
                    )
                }
            }
            if (dueAtMs == null) {
                Text(
                    "Keine Frist gesetzt — tippe auf den Kalender um eine Deadline festzulegen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    "$labelDate · $labelRelative",
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if ((dueAtMs - nowMs) < 24L * 60 * 60 * 1000) LocalCosmos.current.crit
                        else cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.TextButton(onClick = { onChange(null) }) {
                    Text("Frist entfernen", color = cosmos.textSecondary)
                }
            }
        }
    }

    if (showPicker) {
        val datePickerState =
            androidx.compose.material3.rememberDatePickerState(
                initialSelectedDateMillis = dueAtMs ?: System.currentTimeMillis()
            )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        // Auf Ende des gewaehlten Tages setzen (23:59:59 Lokalzeit),
                        // damit Frank den ganzen Tag noch Zeit hat. Sonst wuerde
                        // 00:00 als Frist gelten und die Aufgabe waere ab Mitternacht
                        // schon "abgelaufen".
                        val picked = datePickerState.selectedDateMillis
                        if (picked != null) {
                            val cal = java.util.Calendar.getInstance()
                            cal.timeInMillis = picked
                            // DatePicker liefert UTC-Mitternacht → in Lokalzeit umrechnen.
                            val localCal =
                                java.util.Calendar.getInstance().apply {
                                    set(
                                        cal.get(java.util.Calendar.YEAR),
                                        cal.get(java.util.Calendar.MONTH),
                                        cal.get(java.util.Calendar.DAY_OF_MONTH),
                                        23,
                                        59,
                                        59,
                                    )
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }
                            onChange(localCal.timeInMillis)
                        }
                        showPicker = false
                    }
                ) {
                    Text("Setzen", color = LocalCosmos.current.accent)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showPicker = false }) {
                    Text("Abbrechen", color = cosmos.textSecondary)
                }
            },
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }
}

private fun formatRelativeDeadline(diffMs: Long): String {
    val absDiff = kotlin.math.abs(diffMs)
    val past = diffMs < 0
    val minutes = absDiff / (60 * 1000)
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    return when {
        past && days >= 1 -> "ueberfaellig seit $days Tag${if (days != 1L) "en" else ""}"
        past && hours >= 1 -> "ueberfaellig seit $hours h"
        past -> "ueberfaellig"
        days >= 14 -> "in $weeks Wochen"
        days >= 2 -> "in $days Tagen"
        days >= 1 -> "morgen"
        hours >= 1 -> "in $hours h"
        else -> "in $minutes min"
    }
}

@Composable
private fun FollowupCard(
    followup: EntropyEntryFollowupEntity,
    index: Int,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    var draft by remember(followup.id, followup.rawText) { mutableStateOf(followup.rawText) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Book,
                        contentDescription = null,
                        tint = LocalCosmos.current.accentForscher,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Nachtrag ${germanNumberWord(index)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = LocalCosmos.current.accentForscher,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Nachtrag löschen",
                        tint = LocalCosmos.current.crit,
                    )
                }
            }
            Text(
                text = formatTimestamp(followup.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
                modifier = Modifier.padding(start = 26.dp),
            )
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = draft,
                onValueChange = {
                    draft = it
                    onTextChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle =
                    TextStyle(color = cosmos.textPrimary, fontSize = 15.sp, lineHeight = 22.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(LocalCosmos.current.accent),
                keyboardOptions = KeyboardOptions.Default,
            )
        }
    }
}

private fun severityLabel(severity: Int): String =
    when {
        severity <= 3 -> "Niedrig"
        severity <= 6 -> "Mittel"
        severity <= 8 -> "Hoch"
        else -> "Sehr hoch"
    }

private fun priorityColor(score: Double): Color =
    when {
        score >= 80.0 -> CosmosColors.PriorityRed
        score >= 60.0 -> CosmosColors.PriorityOrange
        score >= 40.0 -> CosmosColors.PriorityYellow
        score >= 20.0 -> CosmosColors.PriorityGreen
        else -> CosmosColors.PriorityBlue
    }

private fun germanNumberWord(n: Int): String =
    when (n) {
        1 -> "eins"
        2 -> "zwei"
        3 -> "drei"
        4 -> "vier"
        5 -> "fünf"
        6 -> "sechs"
        7 -> "sieben"
        8 -> "acht"
        9 -> "neun"
        10 -> "zehn"
        else -> "$n"
    }

private fun formatTimestamp(ms: Long): String {
    val fmt = SimpleDateFormat("d. MMM yyyy, HH:mm", Locale.GERMAN)
    return fmt.format(Date(ms))
}

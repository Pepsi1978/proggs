package de.frank.entropyreducer.presentation.thesen

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.WhisperMicButton
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Vollbild-Detail-Screen eines Thesen-/Entropie-Eintrags (Frank-Wunsch 2026-05-20).
 *
 * Aufbau 1:1 wie BestJournalFrank's EntryDetailScreen — angepasst auf das DataStore-basierte
 * ThesenEntry-Modell:
 * 1. TopAppBar mit Zurück-Pfeil
 * 2. Zusammenfassung-Karte (Title fett + Text editierbar)
 * 3. Nachträge als eigene Karten (mit Inline-Edit + Löschen)
 * 4. Mic-Karte zum Einsprechen neuer Nachträge (Whisper)
 * 5. Vorlesen-Knopf für Title + Text + alle Nachträge (Google TTS)
 * 6. Löschen-Button (entfernt Eintrag samt Nachträgen)
 */
@Composable
fun ThesenEntryDetailScreen(
    onBack: () -> Unit,
    viewModel: ThesenEntryDetailViewModel = hiltViewModel(),
) {
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

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // showImproved wird fuer den Lautsprecher in der Top-Bar benoetigt.
    var showImproved by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = topInset)) {
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
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { viewModel.speakAll(showImproved) },
                    modifier = Modifier.size(40.dp),
                ) {
                    when (state.ttsState) {
                        ThesenTtsState.LOADING ->
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = LocalCosmos.current.accent,
                            )
                        ThesenTtsState.SPEAKING ->
                            Icon(
                                imageVector = Icons.Outlined.Stop,
                                contentDescription = "Vorlesen stoppen",
                                tint = LocalCosmos.current.accent,
                                modifier = Modifier.size(22.dp),
                            )
                        ThesenTtsState.IDLE ->
                            Icon(
                                    imageVector = Icons.Outlined.VolumeUp,
                                contentDescription = "Vorlesen",
                                tint = LocalCosmos.current.accent,
                                modifier = Modifier.size(22.dp),
                            )
                    }
                }
            }

            val entry = state.entry
            if (entry == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LocalCosmos.current.accent)
                }
                return@Column
            }

            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = bottomInset + 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ── KI-Zusammenfassung-Karte (Bullet-Points oder Knopf "Mit KI erstellen") ──
                val summaryVm: ThesenSummaryViewModel = hiltViewModel()
                val summaryState by summaryVm.state.collectAsStateWithLifecycle()
                val summaryError by summaryVm.error.collectAsStateWithLifecycle()
                LaunchedEffect(summaryError) {
                    summaryError?.let {
                        snackbar.showSnackbar(it)
                        summaryVm.clearError()
                    }
                }
                val onGenerateSummary: () -> Unit = {
                    val baseText = buildString {
                        append(entry.text)
                        entry.followups.forEach { f ->
                            append("\n\nNachtrag: ")
                            append(f.text)
                        }
                    }
                    summaryVm.generateSummary(baseText) { bullets ->
                        viewModel.updateSummary(bullets)
                    }
                }
                SummaryCard(
                    summary = entry.summary,
                    isRunning = summaryState == SummaryState.RUNNING,
                    onGenerate = onGenerateSummary,
                )

                // ── Eintrag-Karte (Title + Text, inline editierbar, optional KI-Verbessert) ──
                // Frank-Wunsch 2026-05-23: Wie BestJournalFrank — bei vorhandener
                // KI-Verbesserung erscheint oben ein Tab "Verbessert"/"Original".
                // Andernfalls kann der Eintrag per Knopf "Mit KI nachträglich
                // verbessern" verschliffen werden.
                val improveVm: ThesenImproveViewModel = hiltViewModel()
                val improveError by improveVm.error.collectAsStateWithLifecycle()
                LaunchedEffect(improveError) {
                    improveError?.let {
                        snackbar.showSnackbar(it)
                        improveVm.clearImproved()
                    }
                }

                var titleDraft by remember(entry.id, entry.title) { mutableStateOf(entry.title) }
                var textDraft by remember(entry.id, entry.text) { mutableStateOf(entry.text) }
                // showImproved wird oben in der Column fuer den Lautsprecher benoetigt.
                LaunchedEffect(entry.id, entry.improvedText) {
                    showImproved = entry.improvedText != null
                }
                var entryImproving by remember(entry.id) { mutableStateOf(false) }

                ImprovableEntryCard(
                    headerLabel = "Eintrag",
                    timestampMs = entry.timestampMs,
                    titleDraft = titleDraft,
                    onTitleChange = {
                        titleDraft = it
                        viewModel.updateTitle(it)
                    },
                    originalText = textDraft,
                    onOriginalChange = {
                        textDraft = it
                        viewModel.updateText(it)
                    },
                    improvedText = entry.improvedText,
                    showImproved = showImproved,
                    onToggleVariant = { showImproved = !showImproved },
                    isImproving = entryImproving,
                    onImprove = {
                        entryImproving = true
                        improveVm.improveOnce(textDraft) { improved ->
                            entryImproving = false
                            if (improved != null) {
                                viewModel.setImprovedText(improved)
                                showImproved = true
                            }
                        }
                    },
                )

                // ── Nachträge ──
                entry.followups.forEachIndexed { index, followup ->
                    FollowupCard(
                        followup = followup,
                        index = index + 1,
                        onTextChange = { newText ->
                            viewModel.updateFollowup(followup.id, newText)
                        },
                        onDelete = { viewModel.deleteFollowup(followup.id) },
                        improveVm = improveVm,
                        onImproved = { improved ->
                            viewModel.setFollowupImproved(followup.id, improved)
                        },
                    )
                }

                // ── Nachtrag einsprechen ──
                // Frank-Wunsch 2026-05-23: Nur der Titel, kein "Whisper Large V3 Turbo"-Subtext.
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            tint = ThesenAccent,
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


                // ── Löschen ──
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
    }
}

/**
 * KI-Zusammenfassung als Mini-Fliesstext (Frank-Wunsch 2026-05-23 — max 6 Zeilen,
 * keine Bullet-Points mehr). Migriert alte Bullet-Daten automatisch zu Fliesstext.
 */
@Composable
private fun SummaryCard(summary: String?, isRunning: Boolean, onGenerate: () -> Unit) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Zusammenfassung",
                style = MaterialTheme.typography.titleMedium,
                color = ThesenAccent,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (!summary.isNullOrBlank()) {
                val prose = summaryAsProse(summary)
                Text(
                    text = prose,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textPrimary,
                    maxLines = 6,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onGenerate, enabled = !isRunning) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = ThesenAccent,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Wird erstellt …", style = MaterialTheme.typography.labelMedium)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ThesenAccent,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Neu erstellen",
                            style = MaterialTheme.typography.labelMedium,
                            color = ThesenAccent,
                        )
                    }
                }
            } else {
                Text(
                    text =
                        "Noch keine Zusammenfassung. Gemini erstellt einen kurzen Mini-Fliesstext aus dem Eintrag und allen Nachträgen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onGenerate,
                    enabled = !isRunning,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Wird erstellt …",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Mit KI Zusammenfassung erstellen",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Migrations-Helper (Frank-Wunsch 2026-05-23): alte Bullet-Point-Daten werden zu
 * Fliesstext gejoined damit die UI keine Spruenge zwischen alten/neuen Eintraegen zeigt.
 * Neue Eintraege liefert das ViewModel bereits als Fliesstext — dann passiert hier nichts.
 */
private fun summaryAsProse(raw: String): String {
    val trimmed = raw.trim()
    if (!trimmed.contains("•") && !trimmed.contains("\n- ") && !trimmed.startsWith("- ")) {
        return trimmed
    }
    return trimmed
        .lines()
        .map { it.trim().removePrefix("•").removePrefix("-").removePrefix("*").trim() }
        .filter { it.isNotBlank() }
        .joinToString(separator = " ")
}

/**
 * Eintrag-Karte mit optionalem Verbessert/Original-Tab (Frank-Wunsch 2026-05-23).
 * Wenn keine KI-Version vorhanden ist, wird unter dem editierbaren Originaltext ein
 * "Mit KI nachträglich verbessern"-Knopf gezeigt. Liegt eine improvedText-Variante
 * vor, erscheint oben ein Tab und der Knopf wird durch "Neu verbessern" ersetzt.
 */
@Composable
private fun ImprovableEntryCard(
    headerLabel: String,
    timestampMs: Long,
    titleDraft: String,
    onTitleChange: (String) -> Unit,
    originalText: String,
    onOriginalChange: (String) -> Unit,
    improvedText: String?,
    showImproved: Boolean,
    onToggleVariant: () -> Unit,
    isImproving: Boolean,
    onImprove: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val hasImproved = !improvedText.isNullOrBlank()

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                headerLabel,
                style = MaterialTheme.typography.titleMedium,
                color = ThesenAccent,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatThesenTimestamp(timestampMs),
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            BasicTextField(
                value = titleDraft,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle =
                    TextStyle(
                        color = cosmos.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                    ),
                cursorBrush = SolidColor(ThesenAccent),
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (hasImproved) {
                VariantTabRow(
                    showImproved = showImproved,
                    onToggle = onToggleVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (hasImproved && showImproved) {
                // Verbesserter Text — read-only, joinable per Klick auf Tab.
                Text(
                    text = improvedText!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textPrimary,
                    lineHeight = 22.sp,
                )
            } else {
                BasicTextField(
                    value = originalText,
                    onValueChange = onOriginalChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle =
                        TextStyle(
                            color = cosmos.textSecondary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        ),
                    cursorBrush = SolidColor(ThesenAccent),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ImproveButton(
                isImproving = isImproving,
                hasImproved = hasImproved,
                onClick = onImprove,
            )
        }
    }
}

/**
 * Wiederverwendbarer Tab "Verbessert" / "Original" — orange Highlight unter dem
 * aktiven Tab, anderer Tab in Sekundärfarbe.
 */
@Composable
private fun VariantTabRow(showImproved: Boolean, onToggle: () -> Unit) {
    val cosmos = LocalCosmos.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TabChip(label = "Verbessert", active = showImproved, onClick = { if (!showImproved) onToggle() })
        Spacer(Modifier.width(6.dp))
        TabChip(label = "Original", active = !showImproved, onClick = { if (showImproved) onToggle() })
        Spacer(Modifier.weight(1f))
        Text(
            text = if (showImproved) "von Gemini" else "Roh-Transkript",
            style = MaterialTheme.typography.labelSmall,
            color = cosmos.textSecondary,
        )
    }
}

@Composable
private fun TabChip(label: String, active: Boolean, onClick: () -> Unit) {
    val cosmos = LocalCosmos.current
    val bg = if (active) ThesenAccent.copy(alpha = 0.18f) else Color.Transparent
    val fg = if (active) ThesenAccent else cosmos.textSecondary
    Box(
        modifier =
            Modifier
                .background(bg, RoundedCornerShape(50))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ImproveButton(isImproving: Boolean, hasImproved: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = !isImproving) {
        if (isImproving) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = ThesenAccent,
            )
            Spacer(Modifier.width(8.dp))
            Text("Wird verbessert …", style = MaterialTheme.typography.labelMedium)
        } else {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = ThesenAccent,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text =
                    if (hasImproved) "Neu verbessern"
                    else "Mit KI nachträglich verbessern",
                style = MaterialTheme.typography.labelMedium,
                color = ThesenAccent,
            )
        }
    }
}

@Composable
private fun FollowupCard(
    followup: ThesenFollowup,
    index: Int,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
    improveVm: ThesenImproveViewModel,
    onImproved: (String) -> Unit,
) {
    val cosmos = LocalCosmos.current
    var draft by remember(followup.id, followup.text) { mutableStateOf(followup.text) }
    val hasImproved = !followup.improvedText.isNullOrBlank()
    var showImproved by
        remember(followup.id, followup.isImproved) {
            mutableStateOf(followup.isImproved && followup.improvedText != null)
        }
    var improving by remember(followup.id) { mutableStateOf(false) }

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
                        tint = ThesenAccent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Nachtrag ${germanNumberWord(index)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = ThesenAccent,
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
                text = formatThesenTimestamp(followup.createdAtMs),
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
                modifier = Modifier.padding(start = 26.dp),
            )
            Spacer(Modifier.height(8.dp))

            if (hasImproved) {
                VariantTabRow(showImproved = showImproved, onToggle = { showImproved = !showImproved })
                Spacer(Modifier.height(8.dp))
            }

            if (hasImproved && showImproved) {
                Text(
                    text = followup.improvedText!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textPrimary,
                    lineHeight = 22.sp,
                )
            } else {
                BasicTextField(
                    value = draft,
                    onValueChange = {
                        draft = it
                        onTextChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle =
                        TextStyle(color = cosmos.textPrimary, fontSize = 15.sp, lineHeight = 22.sp),
                    cursorBrush = SolidColor(ThesenAccent),
                )
            }

            Spacer(Modifier.height(10.dp))
            ImproveButton(
                isImproving = improving,
                hasImproved = hasImproved,
                onClick = {
                    improving = true
                    improveVm.improveOnce(draft) { improved ->
                        improving = false
                        if (improved != null) {
                            onImproved(improved)
                            showImproved = true
                        }
                    }
                },
            )
        }
    }
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

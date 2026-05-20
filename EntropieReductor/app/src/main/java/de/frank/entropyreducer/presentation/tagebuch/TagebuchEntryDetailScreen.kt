package de.frank.entropyreducer.presentation.tagebuch

import androidx.compose.foundation.background
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
 * Vollbild-Detail-Screen eines Tagebuch-/Entropie-Eintrags (Frank-Wunsch 2026-05-20).
 *
 * Aufbau 1:1 wie BestJournalFrank's EntryDetailScreen — angepasst auf das DataStore-basierte
 * TagebuchEntry-Modell:
 * 1. TopAppBar mit Zurück-Pfeil
 * 2. Zusammenfassung-Karte (Title fett + Text editierbar)
 * 3. Nachträge als eigene Karten (mit Inline-Edit + Löschen)
 * 4. Mic-Karte zum Einsprechen neuer Nachträge (Whisper)
 * 5. Vorlesen-Knopf für Title + Text + alle Nachträge (Google TTS)
 * 6. Löschen-Button (entfernt Eintrag samt Nachträgen)
 */
@Composable
fun TagebuchEntryDetailScreen(
    onBack: () -> Unit,
    viewModel: TagebuchEntryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
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
                )
            }

            val entry = state.entry
            if (entry == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CosmosColors.AccentPrimary)
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
                val summaryVm: TagebuchSummaryViewModel = hiltViewModel()
                val summaryState by summaryVm.state.collectAsState()
                val summaryError by summaryVm.error.collectAsState()
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

                // ── Eintrag-Karte (Title + Text, inline editierbar) ──
                var titleDraft by remember(entry.id, entry.title) { mutableStateOf(entry.title) }
                var textDraft by remember(entry.id, entry.text) { mutableStateOf(entry.text) }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Eintrag",
                            style = MaterialTheme.typography.titleMedium,
                            color = CosmosColors.AccentSecondary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatTagebuchTimestamp(entry.timestampMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = cosmos.textSecondary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        BasicTextField(
                            value = titleDraft,
                            onValueChange = {
                                titleDraft = it
                                viewModel.updateTitle(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle =
                                TextStyle(
                                    color = cosmos.textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 24.sp,
                                ),
                            cursorBrush = SolidColor(CosmosColors.AccentPrimary),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicTextField(
                            value = textDraft,
                            onValueChange = {
                                textDraft = it
                                viewModel.updateText(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle =
                                TextStyle(
                                    color = cosmos.textSecondary,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                ),
                            cursorBrush = SolidColor(CosmosColors.AccentPrimary),
                        )
                    }
                }

                // ── Nachträge ──
                entry.followups.forEachIndexed { index, followup ->
                    FollowupCard(
                        followup = followup,
                        index = index + 1,
                        onTextChange = { newText ->
                            viewModel.updateFollowup(followup.id, newText)
                        },
                        onDelete = { viewModel.deleteFollowup(followup.id) },
                    )
                }

                // ── Nachtrag einsprechen ──
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            tint = CosmosColors.AccentPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Nachtrag einsprechen",
                                color = cosmos.textPrimary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Whisper Large V3 Turbo — wird als eigene Karte gespeichert.",
                                color = cosmos.textSecondary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        WhisperMicButton(
                            onTranscript = { transcript -> viewModel.addFollowup(transcript) },
                            size = 44.dp,
                        )
                    }
                }

                // ── Vorlesen-Zeile ──
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(1.dp)
                            .background(cosmos.glassBorder.copy(alpha = 0.3f))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { viewModel.speakAll() },
                        modifier = Modifier.size(44.dp),
                    ) {
                        when (state.ttsState) {
                            TagebuchTtsState.LOADING ->
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = CosmosColors.AccentPrimary,
                                )
                            TagebuchTtsState.SPEAKING ->
                                Icon(
                                    imageVector = Icons.Outlined.Stop,
                                    contentDescription = "Vorlesen stoppen",
                                    tint = CosmosColors.AccentPrimary,
                                    modifier = Modifier.size(26.dp),
                                )
                            TagebuchTtsState.IDLE ->
                                Icon(
                                    imageVector = Icons.Outlined.VolumeUp,
                                    contentDescription = "Vorlesen",
                                    tint = CosmosColors.AccentPrimary,
                                    modifier = Modifier.size(26.dp),
                                )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text =
                            when (state.ttsState) {
                                TagebuchTtsState.LOADING -> "TTS wird erzeugt …"
                                TagebuchTtsState.SPEAKING -> "Spricht — tippen zum Stoppen"
                                TagebuchTtsState.IDLE -> "Eintrag + Nachträge vorlesen"
                            },
                        style = MaterialTheme.typography.labelMedium,
                        color = cosmos.textSecondary,
                    )
                }

                // ── Löschen ──
                Button(
                    onClick = { viewModel.deleteEntry() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = CosmosColors.Critical,
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
 * KI-Zusammenfassung als Bullet-Points (analog BestJournalFrank). Bei leerer Summary wird
 * stattdessen ein Knopf "Mit KI Zusammenfassung erstellen" gezeigt.
 */
@Composable
private fun SummaryCard(summary: String?, isRunning: Boolean, onGenerate: () -> Unit) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Zusammenfassung",
                style = MaterialTheme.typography.titleMedium,
                color = CosmosColors.AccentPrimary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (!summary.isNullOrBlank()) {
                summary
                    .lines()
                    .filter { it.trimStart().startsWith("•") }
                    .forEach { line ->
                        val bulletText = line.trimStart().removePrefix("•").trim()
                        Row(modifier = Modifier.padding(bottom = 4.dp)) {
                            Text(
                                "• ",
                                color = cosmos.textPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                bulletText,
                                color = cosmos.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onGenerate, enabled = !isRunning) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = CosmosColors.AccentPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Wird erstellt …", style = MaterialTheme.typography.labelMedium)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = CosmosColors.AccentPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Neu erstellen",
                            style = MaterialTheme.typography.labelMedium,
                            color = CosmosColors.AccentPrimary,
                        )
                    }
                }
            } else {
                Text(
                    text =
                        "Noch keine Zusammenfassung. Gemini erstellt 3–5 Bullet-Points aus dem Eintrag und allen Nachträgen.",
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

@Composable
private fun FollowupCard(
    followup: TagebuchFollowup,
    index: Int,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    var draft by remember(followup.id, followup.text) { mutableStateOf(followup.text) }

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
                        tint = CosmosColors.AccentSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Nachtrag ${germanNumberWord(index)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = CosmosColors.AccentSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Nachtrag löschen",
                        tint = CosmosColors.Critical,
                    )
                }
            }
            Text(
                text = formatTagebuchTimestamp(followup.createdAtMs),
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
                cursorBrush = SolidColor(CosmosColors.AccentPrimary),
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

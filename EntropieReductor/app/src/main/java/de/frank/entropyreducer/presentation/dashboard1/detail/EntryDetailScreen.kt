package de.frank.entropyreducer.presentation.dashboard1.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.local.entities.EntropyEntryFollowupEntity
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.presentation.components.EntropyCategoryPill
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.WhisperMicButton
import de.frank.entropyreducer.presentation.dashboard1.CategoryIconCircle
import de.frank.entropyreducer.presentation.dashboard1.SeverityRainbowBar
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Vollbild-Detail-Screen einer Entropie-Aufgabe (Frank-Wunsch 2026-05-20).
 *
 * Aufbau 1:1 wie BestJournalFrank's EntryDetailScreen, angepasst auf das Aufgaben-Datenmodell:
 * 1. TopAppBar mit Zurück-Pfeil
 * 2. Zusammenfassung-Karte (Title fett + Description)
 * 3. Hero-Karte (Kategorie, Schweregrad, Prio-Score, Severity-Bar)
 * 4. Status-Buttons (4 Stück)
 * 5. Tags
 * 6. KI-Begründung + KI-Notizen
 * 7. Nachträge als eigene Karten (mit Lösch-Button pro Nachtrag)
 * 8. Nachtrag-Aufnahme via Mic
 * 9. Aktions-Zeile mit Vorlesen + Aufnahmedauer (analog Journal)
 * 10. Löschen-Button
 */
@Composable
fun EntryDetailScreen(onBack: () -> Unit, viewModel: EntryDetailViewModel = hiltViewModel()) {
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
                // ── 1. Zusammenfassung-Karte (Title + Description) ──
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Zusammenfassung",
                            style = MaterialTheme.typography.titleMedium,
                            color = CosmosColors.AccentPrimary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = cosmos.textPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        if (entry.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = entry.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cosmos.textSecondary,
                            )
                        }
                    }
                }

                // ── 2. Hero-Karte mit Kategorie + Prio + Severity ──
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.Top) {
                            CategoryIconCircle(
                                category = entry.category,
                                tint = entry.category.color(),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                EntropyCategoryPill(entry.category)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Aufgabe",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cosmos.textSecondary,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${entry.priorityScore.toInt()}",
                                    color = priorityColor(entry.priorityScore),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "Prio",
                                    color = cosmos.textSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Schweregrad: ${severityLabel(entry.severity)} (${entry.severity}/10)",
                            style = MaterialTheme.typography.labelMedium,
                            color = cosmos.textSecondary,
                        )
                        Spacer(Modifier.height(6.dp))
                        SeverityRainbowBar(severity = entry.severity)
                    }
                }

                // ── 3. Status-Buttons (4 Stück) ──
                Text(
                    "Status",
                    style = MaterialTheme.typography.titleSmall,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(
                        label = "Offen",
                        status = EntryStatus.OFFEN,
                        current = entry.status,
                        onClick = viewModel::setStatus,
                        modifier = Modifier.weight(1f),
                    )
                    StatusChip(
                        label = "In Arbeit",
                        status = EntryStatus.IN_ARBEIT,
                        current = entry.status,
                        onClick = viewModel::setStatus,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(
                        label = "Reduziert",
                        status = EntryStatus.REDUZIERT,
                        current = entry.status,
                        onClick = viewModel::setStatus,
                        modifier = Modifier.weight(1f),
                    )
                    StatusChip(
                        label = "Archiviert",
                        status = EntryStatus.ARCHIVIERT,
                        current = entry.status,
                        onClick = viewModel::setStatus,
                        modifier = Modifier.weight(1f),
                    )
                }

                // ── 4. Tags ──
                if (entry.tags.isNotEmpty()) {
                    Text(
                        "Tags",
                        style = MaterialTheme.typography.titleSmall,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        entry.tags.forEach { tag ->
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = cosmos.textSecondary,
                                modifier =
                                    Modifier.clip(RoundedCornerShape(50))
                                        .background(cosmos.glassBg)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // ── 5. KI-Begründung + KI-Notizen ──
                if (entry.priorityReason.isNotBlank() || !entry.aiNotes.isNullOrBlank()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                "KI-Begründung",
                                style = MaterialTheme.typography.titleSmall,
                                color = CosmosColors.AccentSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = entry.priorityReason.ifBlank { "(keine Begründung)" },
                                style = MaterialTheme.typography.bodySmall,
                                color = cosmos.textPrimary,
                            )
                            if (!entry.aiNotes.isNullOrBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "KI-Notizen",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CosmosColors.AccentSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = entry.aiNotes!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cosmos.textPrimary,
                                )
                            }
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

                // ── 7. Nachtrag hinzufügen via Whisper ──
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

                // ── 8. Aktions-Zeile: Vorlesen + Status ──
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
                            TtsState.LOADING ->
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = CosmosColors.AccentPrimary,
                                )
                            TtsState.SPEAKING ->
                                Icon(
                                    imageVector = Icons.Outlined.Stop,
                                    contentDescription = "Vorlesen stoppen",
                                    tint = CosmosColors.AccentPrimary,
                                    modifier = Modifier.size(26.dp),
                                )
                            TtsState.IDLE ->
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
                                TtsState.LOADING -> "TTS wird erzeugt …"
                                TtsState.SPEAKING -> "Spricht — tippen zum Stoppen"
                                TtsState.IDLE -> "Eintrag + Nachträge vorlesen"
                            },
                        style = MaterialTheme.typography.labelMedium,
                        color = cosmos.textSecondary,
                    )
                }

                // ── 9. Löschen-Button ──
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

        if (state.isAddingFollowup) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f))
                        .clickable(enabled = false, onClick = {}),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = CosmosColors.AccentPrimary)
            }
        }
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
                cursorBrush = androidx.compose.ui.graphics.SolidColor(CosmosColors.AccentPrimary),
                keyboardOptions = KeyboardOptions.Default,
            )
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    status: EntryStatus,
    current: EntryStatus,
    onClick: (EntryStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    val selected = status == current
    val accent = if (selected) CosmosColors.AccentPrimary else cosmos.textSecondary
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selected) CosmosColors.AccentPrimary.copy(alpha = 0.18f) else cosmos.glassBg
                )
                .border(
                    width = if (selected) 1.dp else 0.dp,
                    color = if (selected) CosmosColors.AccentPrimary else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable { onClick(status) }
                .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
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

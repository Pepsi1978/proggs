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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
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
 * Aufbau-Reduktion 2026-05-22 (Frank-Wunsch): Zusammenfassung-Karte, Status-Buttons
 * und Vorlese-Zeile sind raus — sie reduzierten nicht die Entropie, sie erhoehten sie.
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
                // ── 1. Hero-Karte mit Kategorie + Title + Beschreibung + Prio + Severity ──
                // Zusammenfassung-Karte oben raus (Frank-Wunsch 2026-05-22) — Title +
                // Beschreibung sind jetzt direkt in der Hero-Karte, keine doppelte Anzeige.
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
                                    text = entry.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = cosmos.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (entry.description.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = entry.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = cosmos.textSecondary,
                                    )
                                }
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

                // ── 2. Tags ──
                // Status-Sektion (Offen/In Arbeit/Reduziert/Archiviert) raus
                // (Frank-Wunsch 2026-05-22) — zu viele Infos in der Aufgabe.
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

                // ── 5. Nachtrag hinzufügen via Whisper ──
                // Untertitel "Whisper Large V3 Turbo" entfernt 2026-05-22 (Frank-Wunsch) —
                // nur noch der schlichte Label "Nachtrag einsprechen".
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            tint = CosmosColors.AccentPrimary,
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

                // ── 6. Löschen-Button ──
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

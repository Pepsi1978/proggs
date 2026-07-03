package de.frank.entropyreducer.presentation.settings.diagnostics

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.diagnostics.DiagnosticLevel
import de.frank.entropyreducer.data.diagnostics.DiagnosticLogEntry
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import kotlinx.coroutines.launch

/**
 * Diagnose-Protokoll (Frank-Wunsch 2026-05-23): Zeigt die internen Fehler- und Erfolgs-Meldungen
 * aller API-Bereiche (Whoop, Oura, Drive-Backup, Kalender, Groq, Gemini, Claude, TTS,
 * Health Connect). Farbig nach Schweregrad, filterbar pro Bereich, mit Export (Teilen) und
 * Loeschen. So sieht Frank ohne Logcat sofort WARUM etwas nicht aktualisiert.
 */
@Composable
fun DiagnosticLogScreen(onBack: () -> Unit, viewModel: DiagnosticLogViewModel = hiltViewModel()) {
    val cosmos = LocalCosmos.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val selectedArea by viewModel.selectedArea.collectAsStateWithLifecycle()

    CosmosScaffold(
        title = "Diagnose-Protokoll",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
        actions = {
            IconButton(
                onClick = {
                    scope.launch {
                        val text = viewModel.buildExportText()
                        val send =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    "Entropie Reductor — Diagnose-Protokoll",
                                )
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                        context.startActivity(
                            Intent.createChooser(send, "Diagnose-Protokoll teilen")
                        )
                    }
                }
            ) {
                Icon(Icons.Outlined.IosShare, "Teilen", tint = cosmos.textPrimary)
            }
            IconButton(onClick = { viewModel.clearLog() }) {
                Icon(Icons.Outlined.DeleteSweep, "Protokoll leeren", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter-Chips: Alle + pro Bereich (LazyRow statt Row+horizontalScroll).
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedArea == null,
                        onClick = { viewModel.setFilter(null) },
                        label = { Text("Alle") },
                        colors = chipColors(),
                    )
                }
                items(DiagnosticArea.entries) { area ->
                    FilterChip(
                        selected = selectedArea == area,
                        onClick = { viewModel.setFilter(area) },
                        label = { Text(area.displayName) },
                        colors = chipColors(),
                    )
                }
            }

            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Noch keine Eintraege.\nSobald ein API-Bereich etwas meldet, erscheint es hier.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cosmos.textSecondary,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(entries, key = { it.id }) { entry -> LogEntryCard(entry) }
                }
            }
        }
    }
}

@Composable
private fun chipColors() =
    FilterChipDefaults.filterChipColors(
        selectedContainerColor = LocalCosmos.current.accent.copy(alpha = 0.22f),
        selectedLabelColor = LocalCosmos.current.textPrimary,
        labelColor = LocalCosmos.current.textSecondary,
    )

@Composable
private fun LogEntryCard(entry: DiagnosticLogEntry) {
    val cosmos = LocalCosmos.current
    val level = DiagnosticLevel.fromNameOrNull(entry.level) ?: DiagnosticLevel.INFO
    val area = DiagnosticArea.fromNameOrNull(entry.area)?.displayName ?: entry.area
    val accent = levelColor(level)
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier =
            Modifier.fillMaxWidth().clickable(enabled = entry.details != null) {
                expanded = !expanded
            }
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier =
                    Modifier.size(10.dp).clip(CircleShape).background(accent).padding(top = 4.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(area, style = MaterialTheme.typography.labelLarge, color = accent)
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatTime(entry.timestampMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
                Spacer(Modifier.size(4.dp))
                Text(
                    entry.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textPrimary,
                )
                entry.details
                    ?.takeIf { it.isNotBlank() }
                    ?.let { details ->
                        Spacer(Modifier.size(6.dp))
                        Text(
                            details,
                            style = MaterialTheme.typography.bodySmall,
                            color = cosmos.textSecondary,
                            maxLines = if (expanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
            }
        }
    }
}

private fun levelColor(level: DiagnosticLevel): Color =
    when (level) {
        DiagnosticLevel.ERROR -> CosmosColors.Critical
        DiagnosticLevel.WARN -> CosmosColors.Warning
        DiagnosticLevel.SUCCESS -> CosmosColors.Success
        DiagnosticLevel.INFO -> CosmosColors.AccentPrimary
        // DEBUG gedimmt — Entwickler-Details treten optisch hinter ERROR/WARN zurueck.
        DiagnosticLevel.DEBUG -> CosmosColors.AccentPrimary.copy(alpha = 0.45f)
    }

private fun formatTime(ms: Long): String {
    val df = java.text.SimpleDateFormat("dd.MM. HH:mm:ss", java.util.Locale.GERMANY)
    return df.format(java.util.Date(ms))
}

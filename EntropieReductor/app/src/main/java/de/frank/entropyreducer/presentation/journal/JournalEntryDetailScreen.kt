package de.frank.entropyreducer.presentation.journal

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorFollowupEntity
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Read-only Detail-Screen eines gespiegelten Tagebucheintrags (Frank-Wunsch 2026-05-24).
 *
 * Aufbau wie der Entropie-Reiter-Detail, aber NUR LESEN:
 * 1. Top-Bar mit Zurück-Pfeil + Vorlesen-Knopf (liest Eintrag + alle Nachträge)
 * 2. KI-Zusammenfassung (falls vorhanden)
 * 3. Umschalter Original / KI-verbessert (nur wenn improvedText existiert)
 * 4. Nachträge als eigene Karten (read-only)
 */
@Composable
fun JournalEntryDetailScreen(
    onBack: () -> Unit,
    viewModel: JournalEntryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Original/KI-Umschalter hier hochgezogen, damit der Vorlese-Knopf in der Top-Bar
    // dieselbe Variante liest, die unten angezeigt wird (Frank-Wunsch 2026-06-01).
    // Standard ist "KI-verbessert", sobald eine verbesserte Variante existiert.
    val variantEntry = state.entry
    val improvedAvailable = !variantEntry?.improvedText.isNullOrBlank()
    var showImproved by
        remember(variantEntry?.sourceId, improvedAvailable) { mutableStateOf(improvedAvailable) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = topInset)) {
            // ── Top-Bar: Zurück + Titel + Vorlesen ──
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
                    text = "Tagebucheintrag",
                    style = MaterialTheme.typography.titleLarge,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { viewModel.speak(showImproved) },
                    modifier = Modifier.size(44.dp),
                ) {
                    when (state.ttsState) {
                        JournalTtsState.LOADING ->
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = LocalCosmos.current.accent,
                            )
                        JournalTtsState.SPEAKING ->
                            Icon(
                                imageVector = Icons.Outlined.Stop,
                                contentDescription = "Vorlesen stoppen",
                                tint = LocalCosmos.current.accent,
                                modifier = Modifier.size(24.dp),
                            )
                        JournalTtsState.IDLE ->
                            Icon(
                                imageVector = Icons.Outlined.VolumeUp,
                                contentDescription = "Vorlesen",
                                tint = LocalCosmos.current.accent,
                                modifier = Modifier.size(24.dp),
                            )
                    }
                }
            }

            val entry = state.entry
            if (entry == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (state.loaded) {
                        Text("Eintrag nicht gefunden", color = cosmos.textSecondary)
                    } else {
                        CircularProgressIndicator(color = JournalAccent)
                    }
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
                Text(
                    text = formatJournalTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )

                // ── KI-Zusammenfassung (read-only) ──
                if (!entry.summary.isNullOrBlank()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                "Zusammenfassung",
                                style = MaterialTheme.typography.titleMedium,
                                color = JournalAccent,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = entry.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cosmos.textPrimary,
                            )
                        }
                    }
                }

                // ── Eintrag-Karte (Titel + Text, optional Original/KI-Umschalter) ──
                // showImproved ist oben hochgezogen — damit Vorlesen == Anzeige.
                val hasImproved = !entry.improvedText.isNullOrBlank()
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = entry.title?.takeIf { it.isNotBlank() } ?: "Eintrag",
                            style = MaterialTheme.typography.titleMedium,
                            color = cosmos.textPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        if (hasImproved) {
                            VariantTabRow(
                                showImproved = showImproved,
                                onToggle = { showImproved = !showImproved },
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        val body =
                            if (showImproved && !entry.improvedText.isNullOrBlank())
                                entry.improvedText
                            else entry.displayText
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cosmos.textPrimary,
                            lineHeight = 22.sp,
                        )
                    }
                }

                // ── Nachträge (read-only) ──
                if (state.followups.isNotEmpty()) {
                    state.followups.forEachIndexed { index, f -> FollowupCard(f, index + 1) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FollowupCard(followup: JournalMirrorFollowupEntity, index: Int) {
    val cosmos = LocalCosmos.current
    var showImproved by
        remember(followup.sourceId, followup.isImproved) {
            mutableStateOf(followup.isImproved && !followup.improvedText.isNullOrBlank())
        }
    val hasImproved = !followup.improvedText.isNullOrBlank()
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Book,
                    contentDescription = null,
                    tint = JournalAccent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Nachtrag $index",
                    style = MaterialTheme.typography.titleSmall,
                    color = JournalAccent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = formatJournalTimestamp(followup.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
                modifier = Modifier.padding(start = 26.dp),
            )
            Spacer(Modifier.height(8.dp))
            if (hasImproved) {
                VariantTabRow(
                    showImproved = showImproved,
                    onToggle = { showImproved = !showImproved },
                )
                Spacer(Modifier.height(8.dp))
            }
            val body =
                if (showImproved && !followup.improvedText.isNullOrBlank()) followup.improvedText
                else followup.text
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = cosmos.textPrimary,
                lineHeight = 22.sp,
            )
        }
    }
}

/** Umschalter "KI-verbessert" / "Original" — read-only Variante. */
@Composable
private fun VariantTabRow(showImproved: Boolean, onToggle: () -> Unit) {
    val cosmos = LocalCosmos.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TabChip(
            label = "KI-verbessert",
            active = showImproved,
            onClick = { if (!showImproved) onToggle() },
        )
        Spacer(Modifier.width(6.dp))
        TabChip(
            label = "Original",
            active = !showImproved,
            onClick = { if (showImproved) onToggle() },
        )
    }
}

@Composable
private fun TabChip(label: String, active: Boolean, onClick: () -> Unit) {
    val cosmos = LocalCosmos.current
    val bg = if (active) JournalAccent.copy(alpha = 0.18f) else Color.Transparent
    val fg = if (active) JournalAccent else cosmos.textSecondary
    Box(
        modifier =
            Modifier.background(bg, RoundedCornerShape(50))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

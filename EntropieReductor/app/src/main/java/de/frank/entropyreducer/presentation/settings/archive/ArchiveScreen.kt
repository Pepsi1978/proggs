package de.frank.entropyreducer.presentation.settings.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.EntropyCategoryPill
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.color
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Archiv-Bildschirm (Frank-Wunsch 2026-05-09): zeigt alle archivierten Aufgaben
 * gruppiert nach Monat. Pro Eintrag sichtbar: Titel, Kategorie, Erledigt-Datum,
 * und besonders die eingesprochene Loesungsmethode aus aiNotes — denn das ist
 * der Kern: wie wurde die Entropie gesenkt?
 */
// Performance-Audit Loop 9 (2026-05-10): Top-level Formatter, thread-safe.
private val ARCHIVE_DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())

@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    vm: ArchiveViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    CosmosScaffold(
        title = "Archiv",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Such-Feld + Header
            ArchiveHeader(totalCount = state.totalCount)
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = vm::setQuery,
                placeholder = { Text("Suchen in Titel, Beschreibung oder Methode", color = cosmos.textSecondary) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, null, tint = cosmos.textSecondary)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = cosmos.textPrimary,
                    unfocusedTextColor = cosmos.textPrimary,
                    focusedBorderColor = LocalCosmos.current.accent,
                    unfocusedBorderColor = cosmos.glassBorder,
                ),
            )
            Spacer(Modifier.height(8.dp))
            if (state.sections.isEmpty()) {
                EmptyArchiveState(hasFilter = state.searchQuery.isNotBlank())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.sections.forEach { section ->
                        item {
                            MonthHeader(label = section.monthLabel, count = section.entries.size)
                        }
                        items(section.entries, key = { it.id }) { entry ->
                            ArchiveEntryCard(
                                entry = entry,
                                onUnarchive = { vm.unarchive(entry.id) },
                                onDelete = { vm.deletePermanently(entry.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveHeader(totalCount: Int) {
    val cosmos = LocalCosmos.current
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LocalCosmos.current.accentForscher.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Inventory2, null,
                    tint = LocalCosmos.current.accentForscher,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "$totalCount archivierte Aufgaben",
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Erledigte Aufgaben mit eingesprochener Lösungs-Methode. Der Forscher nutzt das Archiv um Muster und neue Hypothesen zu erkennen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(label: String, count: Int) {
    val cosmos = LocalCosmos.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(LocalCosmos.current.accentForscher.copy(alpha = 0.18f))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = LocalCosmos.current.accentForscher,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ArchiveEntryCard(
    entry: EntropyEntryEntity,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val catColor = entry.category.color()
    // Performance-Audit Loop 9 (2026-05-10): Top-level Formatter statt
    // Allokation pro Item beim Scrollen durch das Archiv.
    val resolvedDate = (entry.resolvedAt ?: entry.updatedAt).let {
        ARCHIVE_DATE_FMT.format(Instant.ofEpochMilli(it))
    }
    // Methode aus aiNotes extrahieren — Format ist "Methode: <text>" oder
    // mehrzeilig mit "Methode:" als Marker. Wenn nichts gefunden, zeige aiNotes
    // komplett (oder leer).
    val methodText = entry.aiNotes?.let { notes ->
        val marker = "Methode:"
        val idx = notes.indexOf(marker)
        if (idx >= 0) notes.substring(idx + marker.length).trim()
        else notes.trim()
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Inventory2, null,
                        tint = catColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    EntropyCategoryPill(entry.category)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Erledigt am $resolvedDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
                IconButton(onClick = onUnarchive) {
                    Icon(
                        Icons.Outlined.Unarchive, "Aus Archiv holen",
                        tint = LocalCosmos.current.accent,
                    )
                }
            }
            if (entry.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            }
            if (!methodText.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LocalCosmos.current.ok.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Column {
                        Text(
                            "So habe ich die Entropie gesenkt:",
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalCosmos.current.ok,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            methodText,
                            style = MaterialTheme.typography.bodySmall,
                            color = cosmos.textPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyArchiveState(hasFilter: Boolean) {
    val cosmos = LocalCosmos.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.Inventory2, null,
            tint = LocalCosmos.current.accentForscher,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (hasFilter) "Keine Treffer für deine Suche."
            else "Noch keine archivierten Aufgaben.",
            style = MaterialTheme.typography.titleMedium,
            color = cosmos.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (hasFilter) "Probier eine andere Suchanfrage."
            else "Aufgaben werden 14 Tage nach Erledigung automatisch hier archiviert.",
            style = MaterialTheme.typography.bodySmall,
            color = cosmos.textSecondary,
        )
    }
}

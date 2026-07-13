package de.frank.entropyreducer.presentation.amazfit

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.presentation.components.ColorPaletteBar
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.rememberCardColors
import de.frank.entropyreducer.presentation.dashboard4.AmazfitWorkoutRow
import de.frank.entropyreducer.presentation.dashboard4.BiomarkerCardId
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Volle Liste aller Trainings von der Amazfit T-Rex 3, mit Filtern fuer Zeitraum und Sportart.
 * Erreichbar aus dem Biomarker-Screen ueber "Alle anzeigen ▸".
 *
 * Frank-Wunsch 2026-05-09: ein eigener Sport-Bereich der nur fuer Sport da ist — mit
 * T-Rex-3-Vermerk damit klar ist woher die Werte kommen.
 */
// Performance-Audit Loop 3 (2026-05-10): Range-Liste als top-level val statt
// listOf(...)-Allokation pro Recomposition.
private val ALL_RANGES: List<Range> =
    listOf(Range.LAST_30, Range.LAST_90, Range.LAST_365, Range.ALL)

@Composable
fun AmazfitTrainingsScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    vm: AmazfitTrainingsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current

    CosmosScaffold(
        title = "Sport (Polar)",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Zurück",
                    tint = cosmos.textPrimary,
                )
            }
        },
        compactHeader = true,
    ) { padding ->
        // Frank-Wunsch 2026-05-18 Folgeauftrag: Farbpalette oben in der
        // Trainings-Liste. Auswahl persistiert auf AMAZFIT_TRAININGS.
        val cardColorAccess = rememberCardColors()
        val trainingsCardId = BiomarkerCardId.AMAZFIT_TRAININGS

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding =
                androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 0.dp,
                    bottom = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ColorPaletteBar(
                    selectedIndex = cardColorAccess.colorFor(trainingsCardId, cosmos.isDark),
                    onPick = { idx -> cardColorAccess.setColor(trainingsCardId, idx, cosmos.isDark) },
                )
            }
            // Header mit T-Rex-3-Marker + Anzahl Trainings
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier.size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(LocalCosmos.current.warn.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DirectionsRun,
                            contentDescription = null,
                            tint = LocalCosmos.current.warn,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Column {
                        Text(
                            "Alle Trainings",
                            style = MaterialTheme.typography.titleMedium,
                            color = cosmos.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${state.filtered.size} von ${state.workouts.size} Trainings (Polar)",
                            style = MaterialTheme.typography.labelSmall,
                            color = cosmos.textSecondary,
                        )
                    }
                }
            }

            // Zeitraum-Filter
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
                ) {
                    items(items = ALL_RANGES, key = { it.name }) { range ->
                        val selected = state.rangeFilter == range
                        AssistChip(
                            onClick = { vm.setRange(range) },
                            label = {
                                Text(
                                    when (range) {
                                        Range.LAST_30 -> "30 Tage"
                                        Range.LAST_90 -> "3 Monate"
                                        Range.LAST_365 -> "1 Jahr"
                                        Range.ALL -> "Alles"
                                    }
                                )
                            },
                            colors =
                                AssistChipDefaults.assistChipColors(
                                    containerColor =
                                        if (selected) LocalCosmos.current.warn.copy(alpha = 0.18f)
                                        else Color.Transparent,
                                    labelColor =
                                        if (selected) LocalCosmos.current.warn else cosmos.textPrimary,
                                ),
                        )
                    }
                }
            }

            // Sportart-Filter (Chips, scrollbar)
            if (state.availableSports.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
                    ) {
                        item {
                            val selected = state.sportFilter == null
                            AssistChip(
                                onClick = { vm.setSport(null) },
                                label = { Text("Alle Sportarten") },
                                colors =
                                    AssistChipDefaults.assistChipColors(
                                        containerColor =
                                            if (selected) LocalCosmos.current.warn.copy(alpha = 0.18f)
                                            else Color.Transparent,
                                        labelColor =
                                            if (selected) LocalCosmos.current.warn
                                            else cosmos.textPrimary,
                                    ),
                            )
                        }
                        items(state.availableSports, key = { it.first }) { (type, name) ->
                            val selected = state.sportFilter == type
                            AssistChip(
                                onClick = { vm.setSport(type) },
                                label = { Text(name) },
                                colors =
                                    AssistChipDefaults.assistChipColors(
                                        containerColor =
                                            if (selected) LocalCosmos.current.warn.copy(alpha = 0.18f)
                                            else Color.Transparent,
                                        labelColor =
                                            if (selected) LocalCosmos.current.warn
                                            else cosmos.textPrimary,
                                    ),
                            )
                        }
                    }
                }
            }

            // Trainings-Liste
            if (state.filtered.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                "Noch keine Trainings",
                                style = MaterialTheme.typography.titleMedium,
                                color = cosmos.textPrimary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Verbinde dein Polar-Konto in den Einstellungen unter API. " +
                                    "Nach dem Login werden alle Trainings aus Polar Flow " +
                                    "(inkl. externer Brustgurt-Daten vom H10) hier erscheinen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = cosmos.textSecondary,
                            )
                        }
                    }
                }
            } else {
                items(state.filtered, key = { it.trackId }) { w ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        // Performance (Tiefen-Debugging 2026-07-04, Almanach compose §10.5):
                        // RestingHr-Suche memoiziert — lief vorher pro sichtbarem Item bei
                        // jeder Recomposition ueber die GANZE Whoop-Historie (O(n) + 2
                        // ZonedDateTime-Allokationen pro Snapshot). Ergebnis identisch.
                        val restingHr = remember(state.snapshots, w.startMs) {
                            findRestingHrForWorkoutDay(state.snapshots, w.startMs)
                        }
                        AmazfitWorkoutRow(
                            workout = w,
                            onClick = { onOpenDetail(w.trackId) },
                            restingHrForDay = restingHr,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.dashboard4.formatDistance
import de.frank.entropyreducer.presentation.dashboard4.formatDuration
import de.frank.entropyreducer.presentation.dashboard4.formatPace
import de.frank.entropyreducer.presentation.dashboard4.formatStartLabel
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Detail-Ansicht eines einzelnen Workouts. Frank-Wunsch 2026-05-09: alle Werte
 * sichtbar machen — er sortiert spaeter aus was er nicht braucht.
 *
 * Aktuell zeigen wir alle Felder die in der Tabelle gespeichert sind. GPS-Track,
 * Pulsverlauf und Splits werden als JSON-Strings persistiert; Visualisierung
 * (Karte, Linien-Chart) folgt sobald die genauen Detail-Endpoints der Zepp-API
 * via Live-Sync verifiziert sind.
 */
@Composable
fun AmazfitTrainingDetailScreen(
    onBack: () -> Unit,
    vm: AmazfitTrainingDetailViewModel = hiltViewModel(),
) {
    val workout by vm.workout.collectAsState()
    val cosmos = LocalCosmos.current

    CosmosScaffold(
        title = workout?.sportName ?: "Training (T-Rex 3)",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Zurueck",
                    tint = cosmos.textPrimary,
                )
            }
        },
        compactHeader = true,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val w = workout
            if (w == null) {
                item { Text("Wird geladen …", color = cosmos.textSecondary) }
                return@LazyColumn
            }
            item { HeaderCard(w) }
            item { KennzahlenCard(w) }
            item { LaufKennzahlenCard(w) }
            item { HoehenmeterCard(w) }
            item { TrainingseffektCard(w) }
            item { TracksCard(w) }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun HeaderCard(w: AmazfitWorkoutEntity) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmosColors.Warning.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsRun,
                    contentDescription = null,
                    tint = CosmosColors.Warning,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    w.sportName ?: "Sport",
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    formatStartLabel(w.startMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = cosmos.textSecondary,
                )
                Text(
                    "Quelle: T-Rex 3",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmosColors.Warning,
                )
            }
        }
    }
}

@Composable
private fun KennzahlenCard(w: AmazfitWorkoutEntity) {
    SectionCard(title = "Kennzahlen") {
        ValueRow("Distanz", w.distanceMeters?.let { formatDistance(it) })
        ValueRow("Dauer", w.durationSeconds?.let { formatDuration(it) })
        ValueRow("Kalorien", w.calories?.let { "%.0f kcal".format(it) })
        ValueRow("Durchschnittspuls", w.avgHeartRate?.let { "$it bpm" })
        ValueRow("Maximalpuls", w.maxHeartRate?.let { "$it bpm" })
    }
}

@Composable
private fun LaufKennzahlenCard(w: AmazfitWorkoutEntity) {
    SectionCard(title = "Pace & Geschwindigkeit") {
        ValueRow("Durchschnitts-Pace", w.avgPaceSecPerKm?.let { formatPace(it) })
        ValueRow("Maximale Pace", w.maxPaceSecPerKm?.let { formatPace(it) })
        ValueRow("Durchschnittsgeschwindigkeit", w.avgSpeedKmh?.let { "%.2f km/h".format(it) })
        ValueRow("Maximale Geschwindigkeit", w.maxSpeedKmh?.let { "%.2f km/h".format(it) })
        ValueRow("Schrittfrequenz", w.cadence?.let { "$it spm" })
        ValueRow("Schrittlaenge", w.strideLengthCm?.let { "$it cm" })
    }
}

@Composable
private fun HoehenmeterCard(w: AmazfitWorkoutEntity) {
    SectionCard(title = "Hoehenmeter") {
        ValueRow("Aufwaerts", w.altitudeGainMeters?.let { "%.0f m".format(it) })
        ValueRow("Abwaerts", w.altitudeLossMeters?.let { "%.0f m".format(it) })
    }
}

@Composable
private fun TrainingseffektCard(w: AmazfitWorkoutEntity) {
    SectionCard(title = "Trainingseffekt & Erholung") {
        ValueRow("Aerob", w.trainingEffectAerobic?.let { "%.1f / 5.0".format(it) })
        ValueRow("Anaerob", w.trainingEffectAnaerobic?.let { "%.1f / 5.0".format(it) })
        ValueRow("VO2Max", w.vo2Max?.let { "%.1f ml/kg/min".format(it) })
        ValueRow("Erholungszeit", w.recoveryTimeHours?.let { "$it h" })
        ValueRow("Hauttemperatur", w.skinTempCelsius?.let { "%.2f °C".format(it) })
        ValueRow("SWOLF", w.swolf?.let { it.toString() })
        ValueRow("Bahnen", w.poolLaps?.let { it.toString() })
        ValueRow("Pool-Laenge", w.poolLengthMeters?.let { "%.0f m".format(it) })
    }
}

@Composable
private fun TracksCard(w: AmazfitWorkoutEntity) {
    val cosmos = LocalCosmos.current
    val hasGps = !w.gpsTrackJson.isNullOrBlank()
    val hasHr = !w.heartRateSeriesJson.isNullOrBlank()
    val hasPace = !w.paceSeriesJson.isNullOrBlank()
    val hasSplits = !w.splitsJson.isNullOrBlank()
    SectionCard(title = "Verlaeufe") {
        Text(
            buildString {
                appendLine(if (hasGps) "GPS-Track gespeichert" else "Kein GPS-Track verfuegbar")
                appendLine(if (hasHr) "Pulsverlauf gespeichert" else "Kein Pulsverlauf verfuegbar")
                appendLine(if (hasPace) "Pace-Serie gespeichert" else "Keine Pace-Serie verfuegbar")
                appendLine(if (hasSplits) "Splits gespeichert" else "Keine Splits verfuegbar")
            }.trim(),
            style = MaterialTheme.typography.bodySmall,
            color = cosmos.textSecondary,
        )
        if (hasGps || hasHr || hasPace || hasSplits) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Karte und Verlaufs-Charts folgen sobald die Detail-Endpoints der " +
                    "Zepp-Cloud final verifiziert sind. Die Rohdaten sind bereits gespeichert.",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun ValueRow(label: String, value: String?) {
    if (value == null) return
    val cosmos = LocalCosmos.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope

package de.frank.entropyreducer.presentation.dashboard4

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Header-Trenner zwischen Whoop-Werten und Amazfit-T-Rex-3-Werten.
 * Frank-Wunsch 2026-05-09: deutliche Quellen-Trennung.
 */
@Composable
internal fun AmazfitSectionHeader() {
    val cosmos = LocalCosmos.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CosmosColors.Warning.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.MonitorHeart,
                contentDescription = null,
                tint = CosmosColors.Warning,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.size(10.dp))
        Column {
            Text(
                "Amazfit T-Rex 3",
                style = MaterialTheme.typography.titleMedium,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Werte direkt von der Uhr",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
        }
    }
}

/**
 * Sport-Card im Biomarker-Screen. Zeigt die letzten ~5 Trainings als kompakte
 * Liste mit Sportart-Symbol + Distanz + Zeit + Pace + Puls. Tap auf eine Zeile
 * fuehrt zum Detail-Screen, "Alle anzeigen" oeffnet die volle Trainings-Liste.
 */
@Composable
internal fun AmazfitTrainingsCard(
    workouts: List<AmazfitWorkoutEntity>,
    onOpenAll: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val cosmos = LocalCosmos.current
    val recent = workouts.take(5)
    val latest = workouts.firstOrNull()
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CosmosColors.Warning.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsRun,
                        contentDescription = null,
                        tint = CosmosColors.Warning,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    text = "Sport (T-Rex 3)",
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (workouts.isNotEmpty()) {
                    Text(
                        text = "Alle anzeigen ▸",
                        color = CosmosColors.Warning,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onOpenAll() },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (workouts.isEmpty()) {
                Text(
                    text = "Noch keine Trainings — verbinde dich in den Einstellungen unter API mit deinem Zepp-Konto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            } else {
                // HERO: letztes Training prominent mit Pattern-Layout.
                if (latest != null) {
                    LetzterLaufHero(
                        w = latest,
                        onOpenDetail = { onOpenDetail(latest.trackId) },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Frühere Trainings",
                        style = MaterialTheme.typography.labelMedium,
                        color = cosmos.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                // Restliche kleinere Zeilen — letzten überspringen weil schon im Hero.
                recent.drop(1).forEachIndexed { i, w ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    AmazfitWorkoutRow(workout = w, onClick = { onOpenDetail(w.trackId) })
                }
                if (workouts.size > recent.size) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "+ ${workouts.size - recent.size} weitere Trainings",
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAll() }
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * Hero-Pattern für den letzten Lauf — Frank-Wunsch 2026-05-09:
 * "schoenes Fenster mit allen Werten in Patterns".
 * Sportart-Icon + Datum + T-Rex-3-Badge oben, dann Distanz und Dauer GROSS,
 * darunter ein 2x2-Pattern mit Pace, Puls, Kalorien, Höhenmeter.
 */
@Composable
private fun LetzterLaufHero(
    w: AmazfitWorkoutEntity,
    onOpenDetail: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = CosmosColors.Warning
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.10f))
            .clickable { onOpenDetail() }
            .padding(14.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsRun,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = w.sportName ?: "Sport",
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = formatStartLabel(w.startMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.22f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "T-Rex 3",
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HeroBigStat(
                    label = "Distanz",
                    value = w.distanceMeters?.let { formatDistance(it) } ?: "—",
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
                HeroBigStat(
                    label = "Dauer",
                    value = w.durationSeconds?.let { formatDuration(it) } ?: "—",
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))
            // 2x2-Pattern mit den wichtigsten weiteren Werten.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat(
                    label = "Ø Pace",
                    value = w.avgPaceSecPerKm?.let { formatPace(it) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
                MiniStat(
                    label = "Ø Puls",
                    value = w.avgHeartRate?.let { "$it bpm" } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat(
                    label = "Kalorien",
                    value = w.calories?.let { "%.0f kcal".format(it) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
                MiniStat(
                    label = "Höhe ↑",
                    value = w.altitudeGainMeters?.let { "%.0f m".format(it) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeroBigStat(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = cosmos.textSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    val cosmos = LocalCosmos.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(cosmos.glassBorder.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Eine Zeile in der Trainings-Liste. Sportart links, Datum/Zeit Mitte oben,
 * Distanz und Pace darunter, Pulswerte rechts.
 */
@Composable
internal fun AmazfitWorkoutRow(
    workout: AmazfitWorkoutEntity,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = workout.sportName ?: "Sport (Code ${workout.sportType ?: "?"})",
                style = MaterialTheme.typography.titleSmall,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatStartLabel(workout.startMs),
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                workout.distanceMeters?.let {
                    Text(
                        text = formatDistance(it),
                        color = cosmos.textPrimary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "  ·  ",
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                workout.durationSeconds?.let {
                    Text(
                        text = formatDuration(it),
                        color = cosmos.textPrimary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                workout.avgPaceSecPerKm?.let {
                    Text(
                        text = "  ·  ${formatPace(it)}",
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (workout.avgHeartRate != null || workout.maxHeartRate != null) {
            Column(horizontalAlignment = Alignment.End) {
                workout.avgHeartRate?.let {
                    Text(
                        text = "Ø $it",
                        color = CosmosColors.Critical,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                workout.maxHeartRate?.let {
                    Text(
                        text = "Max $it",
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    text = "bpm",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

internal fun formatStartLabel(startMs: Long): String {
    val zone = ZoneId.systemDefault()
    val zdt = Instant.ofEpochMilli(startMs).atZone(zone)
    val today = java.time.LocalDate.now(zone)
    val date = zdt.toLocalDate()
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    val time = zdt.format(timeFmt)
    return when (date) {
        today -> "Heute · $time"
        today.minusDays(1) -> "Gestern · $time"
        today.minusDays(2) -> "Vorgestern · $time"
        else -> zdt.format(DateTimeFormatter.ofPattern("EEE dd.MM. · HH:mm", Locale.GERMANY))
    }
}

internal fun formatDistance(meters: Double): String =
    if (meters >= 1000.0) "%.2f km".format(Locale.GERMANY, meters / 1000.0)
    else "${meters.toInt()} m"

internal fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}

internal fun formatPace(secPerKm: Double): String {
    if (secPerKm <= 0) return ""
    val total = secPerKm.toInt()
    val m = total / 60
    val s = total % 60
    return "%d:%02d min/km".format(m, s)
}

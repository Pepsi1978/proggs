package de.frank.entropyreducer.presentation.dashboard4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.DownhillSkiing
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Rowing
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Skateboarding
import androidx.compose.material.icons.outlined.SportsKabaddi
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.data.local.entities.WhoopWorkoutEntity
import de.frank.entropyreducer.data.remote.whoop.SportIcon
import de.frank.entropyreducer.data.remote.whoop.WhoopSportNames
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import de.frank.entropyreducer.presentation.theme.CosmosColors

/**
 * Container-Card fuer ALLE Workouts eines Tages. Zeigt entweder eine Empty-Card oder eine Liste von
 * Workout-Items mit Trennlinien dazwischen.
 *
 * Frank-Wunsch 2026-05-09: Workout-Bereich komplett mit Strain, Kalorien, Durchschnitts-/Max-HR,
 * Sportart, Dauer, Start-/End-Zeit und HR-Zonen.
 */
@Composable
fun WorkoutsForDayCard(workouts: List<WhoopWorkoutEntity>) {
    val cosmos = LocalCosmos.current
    if (workouts.isEmpty()) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = cosmos.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Workouts an diesem Tag",
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Keine Trainings — Whoop hat für diesen Tag keine Workout-Aktivität erfasst.",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        return
    }

    val totalStrain = workouts.sumOf { it.strain ?: 0.0 }
    val totalKcal = workouts.sumOf { (it.kilojoule ?: 0.0) / 4.184 }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header — Anzahl Workouts + Summe Strain + Summe Kalorien
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(LocalCosmos.current.warn.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = LocalCosmos.current.warn,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Workouts (${workouts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text =
                            buildString {
                                append("Summe ")
                                append("%.1f".format(totalStrain))
                                append(" Belastung · ")
                                append("%.0f".format(totalKcal))
                                append(" kcal")
                            },
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            workouts.forEachIndexed { idx, workout ->
                WorkoutItem(workout)
                if (idx < workouts.lastIndex) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(
                        color = cosmos.textSecondary.copy(alpha = 0.15f),
                        thickness = 0.5.dp,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * Ein einzelnes Workout — gross und detailliert. Layout:
 * 1) Header: Sport-Icon + Sportname + Zeitfenster + Strain-Badge
 * 2) Stats-Pillen: Kalorien, Ø-HR, Max-HR, Distanz/Hoehenmeter
 * 3) HR-Zonen-Stapelbalken (wenn vorhanden) mit Legende
 */
@Composable
private fun WorkoutItem(workout: WhoopWorkoutEntity) {
    val cosmos = LocalCosmos.current
    val sportName = workout.sportName ?: WhoopSportNames.nameOf(workout.sportId)
    val strain = workout.strain ?: 0.0
    val strainColor = strainColorFor(strain)
    val durationMin = ((workout.endMs - workout.startMs) / 60_000).toInt().coerceAtLeast(0)
    val durationLabel =
        if (durationMin >= 60) {
            "${durationMin / 60} h ${(durationMin % 60).toString().padStart(2, '0')} min"
        } else {
            "$durationMin min"
        }
    // Performance-Audit Loop 8 (2026-05-10): Top-level WORKOUT_CARDS_TIME_FMT
    // statt Allokation pro Recomposition.
    val zone = ZoneId.systemDefault()
    val startStr = Instant.ofEpochMilli(workout.startMs).atZone(zone).format(WORKOUT_CARDS_TIME_FMT)
    val endStr = Instant.ofEpochMilli(workout.endMs).atZone(zone).format(WORKOUT_CARDS_TIME_FMT)

    Column {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier.size(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(strainColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconFor(WhoopSportNames.iconKeyOf(workout.sportId)),
                    contentDescription = sportName,
                    tint = strainColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sportName,
                    style = MaterialTheme.typography.titleSmall,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "$startStr – $endStr · $durationLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
            }
            Box(
                modifier =
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .background(strainColor.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Belastung",
                        color = strainColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "%.1f".format(strain),
                        color = strainColor,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Stats-Pillen
        Spacer(Modifier.height(10.dp))
        val distanceText =
            workout.distanceMeter?.takeIf { it > 0.0 }?.let { "%.2f km".format(it / 1000.0) }
        val kcal = workout.kilojoule?.let { "%.0f".format(it / 4.184) } ?: "—"

        // Frank-Wunsch 2026-05-09: "Aufzeichnung %"-Pill komplett raus. Wenn keine
        // Distanz: nur 3 Pills (Kalorien, Ø Puls, Max Puls) — mittig statt linksbündig.
        if (distanceText != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatPill(label = "Kalorien", value = "$kcal kcal", modifier = Modifier.weight(1f))
                StatPill(
                    label = "Ø Puls",
                    value = workout.averageHeartRate?.let { "$it bpm" } ?: "—",
                    modifier = Modifier.weight(1f),
                )
                StatPill(
                    label = "Max Puls",
                    value = workout.maxHeartRate?.let { "$it bpm" } ?: "—",
                    modifier = Modifier.weight(1f),
                )
                StatPill(
                    label = "Strecke",
                    value = distanceText,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            // 3 Pills mittig (10% padding links und rechts → wirkt zentriert)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatPill(label = "Kalorien", value = "$kcal kcal", modifier = Modifier.weight(1f))
                StatPill(
                    label = "Ø Puls",
                    value = workout.averageHeartRate?.let { "$it bpm" } ?: "—",
                    modifier = Modifier.weight(1f),
                )
                StatPill(
                    label = "Max Puls",
                    value = workout.maxHeartRate?.let { "$it bpm" } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // HR-Zonen-Bar (nur wenn mindestens eine Zone Daten hat)
        if (hasZoneData(workout)) {
            Spacer(Modifier.height(12.dp))
            HrZonesSection(workout)
        }
    }
}

/**
 * Horizontaler Stapelbalken der die Aufenthaltsdauer pro HR-Zone visualisiert. Whoop teilt Workouts
 * in 6 Zonen (0 = Ruhe, 5 = Maximum). Farben spiegeln die Intensitaet wider —
 * gruen-blau-gelb-orange-rot Verlauf.
 */
@Composable
private fun HrZonesSection(workout: WhoopWorkoutEntity) {
    val cosmos = LocalCosmos.current
    // Performance-Audit Loop 4 (2026-05-10): Zones-Liste in remember(workout) statt
    // pro Recomposition (vorher 6 ZoneInfo-Allokationen + chunked-Sub-Listen).
    val zones =
        remember(workout) {
            listOf(
                ZoneInfo("Z0", "Ruhe", workout.zoneZeroMilli ?: 0L, ZONE0_COLOR),
                ZoneInfo("Z1", "Sehr leicht", workout.zoneOneMilli ?: 0L, ZONE1_COLOR),
                ZoneInfo("Z2", "Leicht", workout.zoneTwoMilli ?: 0L, ZONE2_COLOR),
                ZoneInfo("Z3", "Mittel", workout.zoneThreeMilli ?: 0L, ZONE3_COLOR),
                ZoneInfo("Z4", "Hart", workout.zoneFourMilli ?: 0L, ZONE4_COLOR),
                ZoneInfo("Z5", "Maximum", workout.zoneFiveMilli ?: 0L, ZONE5_COLOR),
            )
        }
    val total = zones.sumOf { it.millis }.coerceAtLeast(1)

    Column {
        Text(
            text = "Herzfrequenz-Zonen",
            style = MaterialTheme.typography.labelMedium,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        // Stapelbalken
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(cosmos.glassBg)
        ) {
            zones.forEach { z ->
                val frac = z.millis.toFloat() / total.toFloat()
                if (frac > 0f) {
                    Box(
                        modifier =
                            Modifier.fillMaxHeight()
                                .background(z.color)
                                .weight(frac.coerceAtLeast(0.0001f))
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Legende — pro Zeile zwei Zonen damit es schoen kompakt bleibt
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            zones.chunked(2).forEach { rowZones ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowZones.forEach { z ->
                        ZoneLegendChip(z, total, modifier = Modifier.weight(1f))
                    }
                    if (rowZones.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ZoneLegendChip(zone: ZoneInfo, total: Long, modifier: Modifier = Modifier) {
    val cosmos = LocalCosmos.current
    val pct = if (total > 0) zone.millis.toDouble() / total * 100.0 else 0.0
    val mins = (zone.millis / 60_000L).toInt()
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(zone.color))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                "${zone.label} · ${zone.name}",
                color = cosmos.textPrimary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "${"%.0f".format(pct)}% · ${mins} min",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    val cosmos = LocalCosmos.current
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(cosmos.glassBg)
                .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = cosmos.textSecondary, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = cosmos.textPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/* ----------------------------- Helpers ----------------------------- */

private data class ZoneInfo(val label: String, val name: String, val millis: Long, val color: Color)

private fun hasZoneData(workout: WhoopWorkoutEntity): Boolean {
    val total =
        (workout.zoneZeroMilli ?: 0L) +
            (workout.zoneOneMilli ?: 0L) +
            (workout.zoneTwoMilli ?: 0L) +
            (workout.zoneThreeMilli ?: 0L) +
            (workout.zoneFourMilli ?: 0L) +
            (workout.zoneFiveMilli ?: 0L)
    return total > 0L
}

private fun strainColorFor(strain: Double): Color =
    when {
        strain >= 18.0 -> CosmosColors.Critical
        strain >= 14.0 -> CosmosColors.Warning
        strain >= 10.0 -> CosmosColors.AccentPrimary
        strain >= 6.0 -> CosmosColors.AccentSecondary
        else -> CosmosColors.Success
    }

private fun iconFor(key: SportIcon): ImageVector =
    when (key) {
        SportIcon.BIKE -> Icons.Outlined.DirectionsBike
        SportIcon.ROW -> Icons.Outlined.Rowing
        SportIcon.SKI -> Icons.Outlined.DownhillSkiing
        SportIcon.POOL -> Icons.Outlined.Pool
        SportIcon.SELF_IMPROVEMENT -> Icons.Outlined.SelfImprovement
        SportIcon.RUN -> Icons.Outlined.DirectionsRun
        SportIcon.FITNESS -> Icons.Outlined.FitnessCenter
        SportIcon.SPORTS -> Icons.Outlined.SportsSoccer
        SportIcon.MARTIAL -> Icons.Outlined.SportsKabaddi
        SportIcon.HIKING -> Icons.Outlined.Hiking
        SportIcon.SKATE -> Icons.Outlined.Skateboarding
    }

// Whoop-typische Zonen-Farben — Intensitaet von ruhig (gruen) zu maximal (rot).
// Performance-Audit Loop 8 (2026-05-10): Top-level Formatter — DateTimeFormatter
// ist thread-safe per java.time-API-Garantie.
private val WORKOUT_CARDS_TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private val ZONE0_COLOR = Color(0xFF8E9AA8) // grau (Recovery)
private val ZONE1_COLOR = Color(0xFF34D399) // gruen
private val ZONE2_COLOR = Color(0xFF60A5FA) // blau
private val ZONE3_COLOR = Color(0xFFFBBF24) // gelb
private val ZONE4_COLOR = Color(0xFFFB923C) // orange
private val ZONE5_COLOR = Color(0xFFEF4444) // rot

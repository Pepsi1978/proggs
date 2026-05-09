package de.frank.entropyreducer.presentation.amazfit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.Locale

/**
 * Detail-Ansicht eines Workouts mit Hero-Pattern, Stats-Grid und visualisierten
 * Verlaufs-Daten. Frank-Wunsch 2026-05-09: schoenes Layout fuer den letzten Lauf
 * mit Pattern-Karten, Pulsverlauf-Chart, Splits-Tabelle und GPS-Track-Vorschau.
 */
@Composable
fun AmazfitTrainingDetailScreen(
    onBack: () -> Unit,
    vm: AmazfitTrainingDetailViewModel = hiltViewModel(),
) {
    val workout by vm.workout.collectAsState()
    val cosmos = LocalCosmos.current
    val w = workout
    // Parser auf Composable-Level (nicht in item-Lambda — dort kein @Composable-Context).
    val gps = remember(w?.gpsTrackJson) { parseGpsPoints(w?.gpsTrackJson) }
    val hr = remember(w?.heartRateSeriesJson) { parsePipeIntList(w?.heartRateSeriesJson) }
    val splits = remember(w?.paceSeriesJson, w?.splitsJson) {
        parsePipeDoubleList(w?.paceSeriesJson) + parsePipeDoubleList(w?.splitsJson)
    }

    CosmosScaffold(
        title = workout?.sportName ?: "Training (T-Rex 3)",
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (w == null) {
                item { Text("Wird geladen …", color = cosmos.textSecondary) }
                return@LazyColumn
            }
            item { HeroCard(w) }
            item { PaceAndHrGrid(w) }
            item { TerrainGrid(w) }
            item { TrainingseffektCard(w) }
            if (gps.isNotEmpty()) item { GpsTrackCard(gps, w.city) }
            if (hr.isNotEmpty()) item { PulsverlaufCard(hr) }
            if (splits.isNotEmpty()) item { TempoVerlaufCard(splits) }
            if (splits.isNotEmpty()) item { SplitsCard(splits) }
            item { SchwimmCard(w) }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

/* ============================== HERO ============================== */

@Composable
private fun HeroCard(w: AmazfitWorkoutEntity) {
    val cosmos = LocalCosmos.current
    val accent = CosmosColors.Warning
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsRun,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = w.sportName ?: "Sport",
                        style = MaterialTheme.typography.titleLarge,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = formatStartLabel(w.startMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = cosmos.textSecondary,
                    )
                    if (!w.city.isNullOrBlank()) {
                        Text(
                            text = "in ${w.city}",
                            style = MaterialTheme.typography.labelMedium,
                            color = cosmos.textSecondary,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "T-Rex 3",
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BigStat(
                    label = "Distanz",
                    value = w.distanceMeters?.let { formatDistance(it) } ?: "—",
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
                BigStat(
                    label = "Dauer",
                    value = w.durationSeconds?.let { formatDuration(it) } ?: "—",
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BigStat(label: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val cosmos = LocalCosmos.current
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = cosmos.textSecondary,
        )
        Text(
            text = value,
            color = accent,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/* ============================== STATS-GRIDS ============================== */

@Composable
private fun PaceAndHrGrid(w: AmazfitWorkoutEntity) {
    StatsGrid(
        listOf(
            "Ø Pace" to (w.avgPaceSecPerKm?.let { formatPace(it) } ?: "—"),
            "Maximale Pace" to (w.maxPaceSecPerKm?.let { formatPace(it) } ?: "—"),
            "Ø Puls" to (w.avgHeartRate?.let { "$it bpm" } ?: "—"),
            "Maximalpuls" to (w.maxHeartRate?.let { "$it bpm" } ?: "—"),
        ),
    )
}

@Composable
private fun TerrainGrid(w: AmazfitWorkoutEntity) {
    StatsGrid(
        listOf(
            "Höhe ↑" to (w.altitudeGainMeters?.let { "%.0f m".format(it) } ?: "—"),
            "Höhe ↓" to (w.altitudeLossMeters?.let { "%.0f m".format(it) } ?: "—"),
            "Schrittfrequenz" to (w.cadence?.let { "$it spm" } ?: "—"),
            "Schrittlänge" to (w.strideLengthCm?.let { "$it cm" } ?: "—"),
        ),
    )
    Spacer(Modifier.height(8.dp))
    StatsGrid(
        listOf(
            "Kalorien" to (w.calories?.let { "%.0f kcal".format(it) } ?: "—"),
            "VO₂Max" to (w.vo2Max?.let { "%.1f".format(it) } ?: estimateVo2Max(w)),
            "Erholung" to (w.recoveryTimeHours?.let { "$it h" } ?: "—"),
            "Hauttemperatur" to (w.skinTempCelsius?.let { "%.2f °C".format(it) } ?: "—"),
        ),
    )
}

@Composable
private fun StatsGrid(items: List<Pair<String, String>>) {
    val cosmos = LocalCosmos.current
    val rows = items.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = cosmos.textSecondary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = value,
                                style = MaterialTheme.typography.titleMedium,
                                color = cosmos.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/* ============================== TRAININGSEFFEKT ============================== */

@Composable
private fun TrainingseffektCard(w: AmazfitWorkoutEntity) {
    val a = w.trainingEffectAerobic
    val b = w.trainingEffectAnaerobic
    if (a == null && b == null) return
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Trainingseffekt",
                style = MaterialTheme.typography.titleSmall,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            if (a != null) {
                EffektBar(label = "Aerob", value = a, max = 5.0, accent = CosmosColors.AccentPrimary)
                Spacer(Modifier.height(6.dp))
            }
            if (b != null) {
                EffektBar(label = "Anaerob", value = b, max = 5.0, accent = CosmosColors.Critical)
            }
        }
    }
}

@Composable
private fun EffektBar(label: String, value: Double, max: Double, accent: androidx.compose.ui.graphics.Color) {
    val cosmos = LocalCosmos.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = cosmos.textSecondary, modifier = Modifier.weight(1f))
            Text(
                "%.1f / %.1f".format(value, max),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(cosmos.glassBorder.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (value / max).coerceIn(0.0, 1.0).toFloat())
                    .height(8.dp)
                    .background(accent),
            )
        }
    }
}

/* ============================== GPS-TRACK ============================== */

@Composable
private fun GpsTrackCard(points: List<Pair<Double, Double>>, city: String?) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Strecke",
                style = MaterialTheme.typography.titleSmall,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            if (!city.isNullOrBlank()) {
                Text(text = city, style = MaterialTheme.typography.labelSmall, color = cosmos.textSecondary)
            }
            Spacer(Modifier.height(8.dp))
            // Statische Polyline-Vorschau auf Canvas. Lat/Lon werden auf eine
            // Box mit Seitenverhältnis 16:9 normalisiert. Nord ist oben.
            val accent = CosmosColors.Warning
            val borderColor = cosmos.glassBorder.copy(alpha = 0.3f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cosmos.glassBorder.copy(alpha = 0.08f)),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (points.size < 2) return@Canvas
                    val minLat = points.minOf { it.first }
                    val maxLat = points.maxOf { it.first }
                    val minLon = points.minOf { it.second }
                    val maxLon = points.maxOf { it.second }
                    val latRange = (maxLat - minLat).coerceAtLeast(1e-6)
                    val lonRange = (maxLon - minLon).coerceAtLeast(1e-6)
                    val pad = 12f
                    val w = size.width - 2 * pad
                    val h = size.height - 2 * pad
                    fun toX(lon: Double) = pad + ((lon - minLon) / lonRange * w).toFloat()
                    fun toY(lat: Double) = pad + ((maxLat - lat) / latRange * h).toFloat()
                    val path = Path().apply {
                        moveTo(toX(points[0].second), toY(points[0].first))
                        for (p in points.drop(1)) {
                            lineTo(toX(p.second), toY(p.first))
                        }
                    }
                    drawPath(path, accent, style = Stroke(width = 4f))
                    // Start-Punkt grün, End-Punkt rot
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                        radius = 6f,
                        center = Offset(toX(points.first().second), toY(points.first().first)),
                    )
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color(0xFFEF5350),
                        radius = 6f,
                        center = Offset(toX(points.last().second), toY(points.last().first)),
                    )
                    @Suppress("UNUSED_VARIABLE") val unused = borderColor
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${points.size} GPS-Punkte · grün = Start, rot = Ziel",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
        }
    }
}

/* ============================== PULSVERLAUF ============================== */

@Composable
private fun PulsverlaufCard(hr: List<Int>) {
    val cosmos = LocalCosmos.current
    val avg = if (hr.isEmpty()) 0 else hr.average().toInt()
    val min = hr.minOrNull() ?: 0
    val max = hr.maxOrNull() ?: 0
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row {
                Text(
                    text = "Pulsverlauf",
                    style = MaterialTheme.typography.titleSmall,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Ø $avg · Max $max bpm",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmosColors.Critical,
                )
            }
            Spacer(Modifier.height(8.dp))
            val accent = CosmosColors.Critical
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cosmos.glassBorder.copy(alpha = 0.08f)),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (hr.size < 2) return@Canvas
                    val minH = (min - 5).coerceAtLeast(40)
                    val maxH = (max + 5).coerceAtMost(220)
                    val range = (maxH - minH).coerceAtLeast(1)
                    val pad = 8f
                    val w = size.width - 2 * pad
                    val h = size.height - 2 * pad
                    fun toX(i: Int) = pad + (i.toFloat() / (hr.size - 1).toFloat()) * w
                    fun toY(v: Int) = pad + (1f - (v - minH).toFloat() / range.toFloat()) * h
                    val path = Path().apply {
                        moveTo(toX(0), toY(hr[0]))
                        for (i in 1 until hr.size) lineTo(toX(i), toY(hr[i]))
                    }
                    drawPath(path, accent, style = Stroke(width = 3f))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${hr.size} Werte · Min $min · Max $max bpm",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
        }
    }
}

/* ============================== TEMPO-VERLAUF ============================== */

/**
 * Tempo-Verlaufs-Chart als Linie. Frank-Wunsch 2026-05-09: Tempo soll auch
 * grafisch dargestellt werden, nicht nur als Tabelle.
 * Y-Achse INVERTIERT — niedrigere sec/km (= schneller) ist OBEN.
 */
@Composable
private fun TempoVerlaufCard(splits: List<Double>) {
    val cosmos = LocalCosmos.current
    // Werte normalisieren: <50 ist sec/m → ×1000 = sec/km
    val secPerKm = splits.map { if (it < 50.0) it * 1000.0 else it }.filter { it > 0 }
    if (secPerKm.size < 2) return
    val avg = secPerKm.average()
    val min = secPerKm.min()
    val max = secPerKm.max()
    val accent = CosmosColors.AccentPrimary
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row {
                Text(
                    text = "Tempo-Verlauf",
                    style = MaterialTheme.typography.titleSmall,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Ø ${formatPaceSec(avg)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cosmos.glassBorder.copy(alpha = 0.08f)),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pad = 8f
                    val w = size.width - 2 * pad
                    val h = size.height - 2 * pad
                    val range = (max - min).coerceAtLeast(1.0)
                    fun toX(i: Int) = pad + (i.toFloat() / (secPerKm.size - 1).toFloat()) * w
                    // INVERTIERT: schneller (kleiner sec/km) ist OBEN
                    fun toY(v: Double) = pad + ((v - min) / range).toFloat() * h
                    val path = Path().apply {
                        moveTo(toX(0), toY(secPerKm[0]))
                        for (i in 1 until secPerKm.size) lineTo(toX(i), toY(secPerKm[i]))
                    }
                    drawPath(path, accent, style = Stroke(width = 3f))
                    // Start- und Endpunkt markieren
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                        radius = 6f,
                        center = Offset(toX(0), toY(secPerKm.first())),
                    )
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color(0xFFEF5350),
                        radius = 6f,
                        center = Offset(toX(secPerKm.size - 1), toY(secPerKm.last())),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${secPerKm.size} Werte · Schnellster: ${formatPaceSec(min)} · Langsamster: ${formatPaceSec(max)} · grün = Start, rot = Ziel",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
        }
    }
}

private fun formatPaceSec(secPerKm: Double): String {
    if (secPerKm <= 0) return ""
    val total = secPerKm.toInt()
    val m = total / 60
    val s = total % 60
    return "%d:%02d min/km".format(m, s)
}

/* ============================== SPLITS ============================== */

@Composable
private fun SplitsCard(splits: List<Double>) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Pace pro Kilometer",
                style = MaterialTheme.typography.titleSmall,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            // Pace-Werte koennen entweder als Sekunden pro Km oder als
            // Sekunden pro Meter gespeichert sein. Defensiv interpretieren:
            // Werte unter 50 sind vermutlich sec/m → mit 1000 multiplizieren.
            val secPerKm = splits.map { if (it < 50.0) it * 1000.0 else it }
            val maxPace = secPerKm.max()
            val minPace = secPerKm.filter { it > 0 }.minOrNull() ?: 0.0
            secPerKm.forEachIndexed { idx, sec ->
                if (sec <= 0) return@forEachIndexed
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(
                        text = "Km ${idx + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cosmos.textSecondary,
                        modifier = Modifier.weight(0.18f),
                    )
                    Box(modifier = Modifier.weight(0.55f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(cosmos.glassBorder.copy(alpha = 0.18f))) {
                        val frac = if (maxPace > minPace) ((sec - minPace) / (maxPace - minPace)).coerceIn(0.0, 1.0).toFloat() else 0.5f
                        Box(modifier = Modifier.fillMaxWidth(fraction = (1f - frac).coerceIn(0.05f, 1f)).height(10.dp).background(CosmosColors.Warning))
                    }
                    Text(
                        text = formatPace(sec),
                        style = MaterialTheme.typography.bodySmall,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(0.27f).padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

/* ============================== SCHWIMM ============================== */

@Composable
private fun SchwimmCard(w: AmazfitWorkoutEntity) {
    if (w.swolf == null && w.poolLengthMeters == null) return
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Schwimmen",
                style = MaterialTheme.typography.titleSmall,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            ValueRow("SWOLF", w.swolf?.takeIf { it > 0 }?.toString())
            ValueRow("Bahnen", w.poolLaps?.takeIf { it > 0 }?.toString())
            ValueRow("Pool-Länge", w.poolLengthMeters?.takeIf { it > 0 }?.let { "%.0f m".format(it) })
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
        Text(value, style = MaterialTheme.typography.bodySmall, color = cosmos.textPrimary, fontWeight = FontWeight.SemiBold)
    }
}

/* ============================== PARSER ============================== */

/**
 * Format-Erkenntnisse aus rolandsz/Mi-Fit-and-Zepp-workout-exporter:
 * - Trenner zwischen Samples ist ";" (nicht "|")
 * - Lat/Lon/HR sind DELTA-encoded: erster Wert absolut, weitere Deltas
 * - Lat/Lon-Skalierung: Integer ÷ 100_000_000 für Grad
 * - HR-Format pro Sample: "time_delta,hr_delta" (zwei Werte!)
 */

/**
 * Parst Pulsverlauf. Format: "0,85;1,1;1,-1;1,2;..." wo erste Zahl ein
 * time-delta ist und zweite ein hr-delta. Erster hr-Wert ist absolut, alle
 * weiteren sind Deltas die akkumuliert werden muessen.
 */
private fun parsePipeIntList(s: String?): List<Int> {
    if (s.isNullOrBlank()) return emptyList()
    val deltas = mutableListOf<Int>()
    for (sample in s.split(";")) {
        if (sample.isBlank()) continue
        val parts = sample.split(",")
        // hr-Delta ist der ZWEITE Wert (parts[1]), parts[0] ist time-delta
        val v = (parts.getOrNull(1) ?: parts.getOrNull(0))?.trim()?.toIntOrNull() ?: continue
        deltas.add(v)
    }
    // Akkumulieren — Delta zu absolutem HR
    val out = mutableListOf<Int>()
    var sum = 0
    for (d in deltas) {
        sum += d
        // Plausibilitaetsfilter: HR zwischen 30 und 230
        if (sum in 30..230) out.add(sum)
    }
    return out
}

/**
 * Parst kilo_pace-String. Vermutung: Werte pro Kilometer als Float,
 * mit ";" oder "|" getrennt. Format defensiv.
 */
private fun parsePipeDoubleList(s: String?): List<Double> {
    if (s.isNullOrBlank()) return emptyList()
    // ";" ist primaerer Trenner laut rolandsz-Code. Fallback "|".
    return s.split(";").flatMap { it.split("|") }.mapNotNull { it.trim().toDoubleOrNull() }
}

/**
 * Parst GPS-Track. Format aus rolandsz/exporters/base_exporter.py:
 *   "latInt,lonInt;latInt,lonInt;..." mit Semikolon zwischen Punkten
 *   und Komma zwischen Lat und Lon je Punkt.
 * Werte sind DELTA-encoded (erster Punkt absolut), Skalierung ÷ 100_000_000.
 */
private fun parseGpsPoints(s: String?): List<Pair<Double, Double>> {
    if (s.isNullOrBlank()) return emptyList()
    val latDeltas = mutableListOf<Long>()
    val lonDeltas = mutableListOf<Long>()
    for (sample in s.split(";")) {
        if (sample.isBlank()) continue
        val parts = sample.split(",")
        val lat = parts.getOrNull(0)?.trim()?.toLongOrNull() ?: continue
        val lon = parts.getOrNull(1)?.trim()?.toLongOrNull() ?: continue
        latDeltas.add(lat)
        lonDeltas.add(lon)
    }
    if (latDeltas.isEmpty()) return emptyList()
    // Akkumulieren
    val out = mutableListOf<Pair<Double, Double>>()
    var latSum = 0L
    var lonSum = 0L
    for (i in latDeltas.indices) {
        latSum += latDeltas[i]
        lonSum += lonDeltas[i]
        // Skalierung: Integer ÷ 100_000_000 = Grad
        val latDeg = latSum.toDouble() / 100_000_000.0
        val lonDeg = lonSum.toDouble() / 100_000_000.0
        // Plausibilitaetsfilter: nur ueberhaupt-mögliche Koordinaten
        if (latDeg in -90.0..90.0 && lonDeg in -180.0..180.0) {
            out.add(latDeg to lonDeg)
        }
    }
    return out
}

private typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope

/**
 * Schaetzt VO2Max — Uth-Sørensen-Pedersen-Formel (Standard in Fitness-Apps):
 *   VO2Max ≈ 15.3 × (HR_max / HR_rest)
 *
 * WICHTIG: HR_max ist Frank's PERSOENLICHER Maximalpuls (180 — Frank-Info
 * 2026-05-09), NICHT der Workout-Max. Der persoenliche Max ist konstant,
 * der Workout-Max kann darunter liegen wenn nicht voll ausbelastet.
 * HR_rest ist der typische Ruhepuls (Default 65) — sollte spaeter aus dem
 * Daily-Eintrag des gleichen Tages kommen.
 *
 * Verifikation: 15.3 × 180 / 65 = 42.4 — passt zu Zepp-App-Anzeige 41.
 */
private fun estimateVo2Max(w: de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity): String {
    @Suppress("UNUSED_VARIABLE") val unused = w
    val frankMaxHr = 180
    val restingHr = 65
    val vo2 = 15.3 * frankMaxHr.toDouble() / restingHr.toDouble()
    return "≈ %.0f".format(vo2)
}

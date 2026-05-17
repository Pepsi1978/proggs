package de.frank.entropyreducer.presentation.amazfit

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
    // Frank-Wunsch 2026-05-17: Edit-Dialog fuer manuelle Werte (Pace/HR/Hoehe
    // /Cadence/Stride/Kalorien). VO2max wird automatisch berechnet.
    var editDialogOpen by remember { mutableStateOf(false) }
    if (editDialogOpen && w != null) {
        EditTrainingValuesDialog(
            workout = w,
            onDismiss = { editDialogOpen = false },
            onSave = { overrides ->
                vm.applyManualOverrides(overrides)
                editDialogOpen = false
            },
        )
    }
    // Parser auf Composable-Level (nicht in item-Lambda — dort kein @Composable-Context).
    val gps = remember(w?.gpsTrackJson) { parseGpsPoints(w?.gpsTrackJson) }
    val hr = remember(w?.heartRateSeriesJson) { parsePipeIntList(w?.heartRateSeriesJson) }
    // Splits = Km-Splits aus paceSeriesJson (~7-8 Werte pro Lauf, fuer Tabelle)
    val splits = remember(w?.paceSeriesJson) { parsePipeDoubleList(w?.paceSeriesJson) }
    // Tempo-Stream = hochaufgeloeste Sample-Liste aus paceStreamJson (~3000 Werte
    // pro Lauf, fuer den fluessigen Tempo-Verlauf-Chart)
    val tempoStream = remember(w?.paceStreamJson) { parsePaceStream(w?.paceStreamJson) }

    CosmosScaffold(
        title = workout?.sportName ?: "Training (${sourceLabel(workout?.source)})",
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
            item { PaceAndHrGrid(w, onEditClick = { editDialogOpen = true }) }
            item { TerrainGrid(w, onEditClick = { editDialogOpen = true }) }
            item { TrainingseffektCard(w) }
            if (gps.isNotEmpty()) item { GpsTrackCard(gps, w.city) }
            if (hr.isNotEmpty()) item { PulsverlaufCard(hr) }
            if (tempoStream.size >= 10) item { TempoVerlaufCard(tempoStream, w.distanceMeters) }
            if (splits.isNotEmpty()) item { SplitsCard(splits) }
            item { SchwimmCard(w) }
            // Diagnose-Karte: zeigt WAS fehlt und bietet manuelles Neuladen.
            // Frank-Beschwerde 2026-05-09: bei alten Trainings fehlten Strecke,
            // Pulsverlauf und Pace-Bars unsichtbar — niemand wusste warum.
            item {
                DatenLueckenCard(
                    workout = w,
                    hasGps = gps.isNotEmpty(),
                    hasHr = hr.isNotEmpty(),
                    hasTempo = tempoStream.size >= 10,
                    hasSplits = splits.isNotEmpty(),
                    onReload = { vm.reloadDetail() },
                )
            }
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
                        text = sourceLabel(w.source),
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
private fun PaceAndHrGrid(w: AmazfitWorkoutEntity, onEditClick: () -> Unit = {}) {
    StatsGrid(
        items = listOf(
            "Ø Pace" to (w.avgPaceSecPerKm?.let { formatPace(it) } ?: "—"),
            "Maximale Pace" to (w.maxPaceSecPerKm?.let { formatPace(it) } ?: "—"),
            "Ø Puls" to (w.avgHeartRate?.let { "$it bpm" } ?: "—"),
            "Maximalpuls" to (w.maxHeartRate?.let { "$it bpm" } ?: "—"),
        ),
        onItemClick = onEditClick,
    )
}

@Composable
private fun TerrainGrid(w: AmazfitWorkoutEntity, onEditClick: () -> Unit = {}) {
    StatsGrid(
        items = listOf(
            "Höhe ↑" to (w.altitudeGainMeters?.let { "%.0f m".format(it) } ?: "—"),
            "Höhe ↓" to (w.altitudeLossMeters?.let { "%.0f m".format(it) } ?: "—"),
            "Schrittfrequenz" to (w.cadence?.let { "$it spm" } ?: "—"),
            "Schrittlänge" to (w.strideLengthCm?.let { "$it cm" } ?: "—"),
        ),
        onItemClick = onEditClick,
    )
    Spacer(Modifier.height(8.dp))
    StatsGrid(
        items = listOf(
            "Kalorien" to (w.calories?.let { "%.0f kcal".format(it) } ?: "—"),
            // Frank-Wunsch 2026-05-16: VO2max IMMER selbst berechnen (ACSM + Karvonen + 2).
            // Der DB-Wert (z.B. Polar's running-index oder Zepp-VO2max) wird ignoriert —
            // nur die eigenen Daten zaehlen. Wenn Eingaben fehlen, steht "—".
            "VO₂Max" to estimateVo2Max(w),
        ),
        onItemClick = onEditClick,
    )
}

@Composable
private fun StatsGrid(items: List<Pair<String, String>>, onItemClick: () -> Unit = {}) {
    val cosmos = LocalCosmos.current
    val rows = items.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    // Frank-Wunsch 2026-05-17: Jedes Stat-Feld ist klickbar
                    // und oeffnet den Edit-Dialog. Stift-Icons sind raus —
                    // direkt auf die Wert-Box tippen.
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onItemClick() },
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
            // Performance-Audit Loop 2 (2026-05-10): Pfad einmalig allokieren
            // statt pro Frame. Wird im Lambda mit reset() wiederverwendet.
            val gpsPath = remember { androidx.compose.ui.graphics.Path() }
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
                    // Performance-Audit Loop 2 (2026-05-10): Path-Allokation pro
                    // Frame eliminiert. Path-Pfad wird durch Reset+moveTo+lineTo
                    // wiederverwendet — gleiches Pixel-Output ohne GC-Druck.
                    gpsPath.reset()
                    gpsPath.moveTo(toX(points[0].second), toY(points[0].first))
                    for (p in points.drop(1)) {
                        gpsPath.lineTo(toX(p.second), toY(p.first))
                    }
                    drawPath(gpsPath, accent, style = Stroke(width = 4f))
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
            ChartWithAxes(
                accent = CosmosColors.Critical,
                xCount = hr.size,
                yMin = (min - 5).coerceAtLeast(40).toDouble(),
                yMax = (max + 5).coerceAtMost(220).toDouble(),
                yUnit = "bpm",
                yFormat = { it.toInt().toString() },
                xLabel = { i ->
                    // Minuten-Skala: 1 Sample ≈ 1 Sekunde
                    val sec = i
                    val mins = sec / 60
                    "${mins} min"
                },
                xTickCount = 6,
                yTickCount = 7,
                values = hr.map { it.toDouble() },
                invertY = false,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${hr.size} Werte · Min $min · Max $max bpm",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
        }
    }
}

/**
 * Linien-Chart mit Achsen-Skalen (Y links, X unten), Hintergrund-Gridlines,
 * Start/Ende-Markierungen. Frank-Wunsch 2026-05-09: Skalen sichtbar fuer
 * Pulsverlauf und Tempo-Verlauf.
 */
@Composable
private fun ChartWithAxes(
    accent: androidx.compose.ui.graphics.Color,
    xCount: Int,
    yMin: Double,
    yMax: Double,
    yUnit: String,
    yFormat: (Double) -> String,
    xLabel: (Int) -> String,
    xTickCount: Int,
    yTickCount: Int,
    values: List<Double>,
    invertY: Boolean,
) {
    val cosmos = LocalCosmos.current
    val gridColor = cosmos.glassBorder.copy(alpha = 0.5f)
    val labelArgb = cosmos.textPrimary.toArgb()
    if (values.size < 2 || yMax <= yMin) return
    // Performance-Audit Loop 2 (2026-05-10): Pfad einmalig allokieren statt
    // pro Frame.
    val seriesPath = remember { Path() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cosmos.glassBorder.copy(alpha = 0.10f)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padLeft = 56f
            val padRight = 12f
            val padTop = 10f
            val padBottom = 28f
            val w = size.width - padLeft - padRight
            val h = size.height - padTop - padBottom
            val range = yMax - yMin
            fun toX(i: Int) = padLeft + (i.toFloat() / (xCount - 1).toFloat()) * w
            fun toY(v: Double): Float {
                val t = ((v - yMin) / range).toFloat().coerceIn(0f, 1f)
                return padTop + (if (invertY) t else 1f - t) * h
            }
            // Achsen-Beschriftung — Theme-Farbe damit sie sichtbar ist (vorher
            // hartcodiert hellgrau auf hellem Hintergrund — unsichtbar).
            val textPaint = android.graphics.Paint().apply {
                color = labelArgb
                textSize = 24f
                isAntiAlias = true
            }
            // Y-Gridlines + Beschriftung — mehr Linien fuer feinere Ablesbarkeit
            for (i in 0..yTickCount) {
                val v = yMin + (yMax - yMin) * i.toDouble() / yTickCount.toDouble()
                val y = toY(v)
                drawLine(
                    color = gridColor,
                    start = Offset(padLeft, y),
                    end = Offset(size.width - padRight, y),
                    strokeWidth = 1f,
                )
                drawIntoCanvas {
                    it.nativeCanvas.drawText(yFormat(v), 4f, y + 8f, textPaint)
                }
            }
            // X-Gridlines + Beschriftung
            for (i in 0..xTickCount) {
                val idx = ((xCount - 1).toDouble() * i / xTickCount).toInt()
                val x = toX(idx)
                drawLine(
                    color = gridColor,
                    start = Offset(x, padTop),
                    end = Offset(x, padTop + h),
                    strokeWidth = 1f,
                )
                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        xLabel(idx),
                        x - 18f,
                        size.height - 6f,
                        textPaint,
                    )
                }
            }
            // Datenlinie — wiederverwendeter Pfad (Loop 2 Perf-Fix)
            seriesPath.reset()
            seriesPath.moveTo(toX(0), toY(values[0]))
            for (i in 1 until values.size) seriesPath.lineTo(toX(i), toY(values[i]))
            val path = seriesPath
            drawPath(path, accent, style = Stroke(width = 3f))
            // Start- und Endpunkt-Marker
            drawCircle(
                color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                radius = 5f,
                center = Offset(toX(0), toY(values.first())),
            )
            drawCircle(
                color = androidx.compose.ui.graphics.Color(0xFFEF5350),
                radius = 5f,
                center = Offset(toX(values.size - 1), toY(values.last())),
            )
            @Suppress("UNUSED_VARIABLE") val u2 = yUnit
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
private fun TempoVerlaufCard(stream: List<Double>, distanceMeters: Double?) {
    val cosmos = LocalCosmos.current
    // stream-Werte sind bereits in sec/km vom Parser. Stark schwankende Werte
    // (z.B. an Pause-Phasen) ueber 1200 sec/km filtern damit der Chart nicht
    // durch Outliers verzerrt wird.
    val secPerKm = stream.filter { it in 150.0..1200.0 }
    if (secPerKm.size < 10) return
    val avg = secPerKm.average()
    val min = secPerKm.min()
    val max = secPerKm.max()
    val accent = CosmosColors.AccentPrimary
    // Frank-Wunsch 2026-05-11: X-Achse zeigt echte Kilometer (z.B. "Km 0", "Km 1",
    // "Km 5.8") statt Sample-Index (frueher "Km 1" bis "Km 3011" weil pro Sample
    // ein Label). Wir interpolieren linear: Sample i steht fuer Position
    // (i / (N-1)) * totalKm. Wenn keine Distanz bekannt ist, fallen wir auf den
    // alten Index zurueck (kommt selten vor).
    val totalKm = distanceMeters?.takeIf { it > 0 }?.let { it / 1000.0 }
    val lastIdx = (secPerKm.size - 1).coerceAtLeast(1)
    val xLabelFn: (Int) -> String = { i ->
        if (totalKm != null) {
            val km = (i.toDouble() / lastIdx) * totalKm
            // Frank-Wunsch 2026-05-11: IMMER eine Nachkommastelle, damit das letzte
            // Tick die exakte Laufstrecke zeigt (z.B. 7,2 km bei 7,16 km-Lauf statt
            // truncated 7). Schrittweite = totalKm/6 weil 6 Ticks im Chart.
            "Km ${"%.1f".format(km)}"
        } else {
            "Km ${i + 1}"
        }
    }
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
            // Y invertiert: schneller (kleiner sec/km) ist OBEN auf der Y-Achse.
            ChartWithAxes(
                accent = accent,
                xCount = secPerKm.size,
                yMin = min,
                yMax = max,
                yUnit = "min/km",
                yFormat = { formatPaceSec(it) },
                xLabel = xLabelFn,
                xTickCount = 6,
                yTickCount = 4,
                values = secPerKm,
                invertY = true,
            )
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
            val secPerKm = splits.map { if (it < 50.0) it * 1000.0 else it }.filter { it > 0 }
            if (secPerKm.isEmpty()) return@Column
            val maxPace = secPerKm.max()
            val minPace = secPerKm.min()
            secPerKm.forEachIndexed { idx, sec ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(
                        text = "Km ${idx + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cosmos.textSecondary,
                        modifier = Modifier.weight(0.18f),
                    )
                    Box(modifier = Modifier.weight(0.55f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(cosmos.glassBorder.copy(alpha = 0.18f))) {
                        // Frank-Wunsch 2026-05-09 (zweite Iteration): laengster Bar
                        // = 100% (langsamster Km), andere proportional davon.
                        // Beispiel: maxPace 9:25 (565 sec/km) = 100% Bar,
                        // 7:11 (431 sec/km) = 431/565 = 76% Bar.
                        val frac = (sec / maxPace).toFloat().coerceIn(0.0f, 1.0f)
                        // Farbverlauf relativ: gruen (schnellster) → rot (langsamster)
                        val colorFrac = if (maxPace > minPace) ((sec - minPace) / (maxPace - minPace)).coerceIn(0.0, 1.0).toFloat() else 0.5f
                        val barColor = lerpColor(
                            androidx.compose.ui.graphics.Color(0xFF4CAF50),
                            androidx.compose.ui.graphics.Color(0xFFFF9800),
                            androidx.compose.ui.graphics.Color(0xFFEF5350),
                            colorFrac,
                        )
                        Box(modifier = Modifier.fillMaxWidth(fraction = frac).height(10.dp).background(barColor))
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

/** 3-Stop-Farbverlauf — gruen → orange → rot, t in [0,1]. */
private fun lerpColor(
    a: androidx.compose.ui.graphics.Color,
    b: androidx.compose.ui.graphics.Color,
    c: androidx.compose.ui.graphics.Color,
    t: Float,
): androidx.compose.ui.graphics.Color {
    return if (t < 0.5f) {
        val k = t * 2f
        androidx.compose.ui.graphics.Color(
            red = a.red + (b.red - a.red) * k,
            green = a.green + (b.green - a.green) * k,
            blue = a.blue + (b.blue - a.blue) * k,
            alpha = 1f,
        )
    } else {
        val k = (t - 0.5f) * 2f
        androidx.compose.ui.graphics.Color(
            red = b.red + (c.red - b.red) * k,
            green = b.green + (c.green - b.green) * k,
            blue = b.blue + (c.blue - b.blue) * k,
            alpha = 1f,
        )
    }
}

/* ============================== DATEN-LUECKEN ============================== */

/**
 * Karte die transparent anzeigt was bei diesem Training NICHT geladen werden
 * konnte (Strecke, Pulsverlauf, Pace-Bars, Tempo-Verlauf) und einen Button zum
 * erneuten Versuch bietet. Erscheint nur wenn mindestens EIN Block fehlt.
 *
 * Frank-Befund 2026-05-09: bei aelteren Trainings fehlten die Diagramme stumm.
 * Statt sie verschwinden zu lassen, MUSS klar sein WAS fehlt — sonst weiss
 * niemand ob der Server keine Daten hat oder ob das Laden fehlgeschlagen ist.
 */
@Composable
private fun DatenLueckenCard(
    workout: AmazfitWorkoutEntity,
    hasGps: Boolean,
    hasHr: Boolean,
    hasTempo: Boolean,
    hasSplits: Boolean,
    onReload: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val gpsRaw = workout.gpsTrackJson
    val hrRaw = workout.heartRateSeriesJson
    val tempoRaw = workout.paceStreamJson
    val splitsRaw = workout.paceSeriesJson

    // Status pro Block bestimmen:
    //  "ok"        — Daten sind da und parsen erfolgreich
    //  "leer"      — Server lieferte nichts (Feld null oder Marker " ")
    //  "geladen-leer" — Feld hat Inhalt aber Parser fand nichts (Format-Bruch)
    fun statusOf(parsed: Boolean, raw: String?): String = when {
        parsed -> "ok"
        raw.isNullOrBlank() -> "leer"
        else -> "geladen-leer"
    }

    val gpsStatus = statusOf(hasGps, gpsRaw)
    val hrStatus = statusOf(hasHr, hrRaw)
    val tempoStatus = statusOf(hasTempo, tempoRaw)
    val splitsStatus = statusOf(hasSplits, splitsRaw)

    val anyMissing = gpsStatus != "ok" || hrStatus != "ok" || tempoStatus != "ok" || splitsStatus != "ok"
    if (!anyMissing) return

    var reloadInProgress by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = CosmosColors.Warning,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Detail-Daten",
                    style = MaterialTheme.typography.titleSmall,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            DatenLueckeRow("Strecke (GPS)", gpsStatus)
            DatenLueckeRow("Pulsverlauf", hrStatus)
            DatenLueckeRow("Tempo-Verlauf", tempoStatus)
            DatenLueckeRow("Pace pro Kilometer", splitsStatus)
            Spacer(Modifier.height(6.dp))
            // Hilfetext: erklaert was die Status-Worte bedeuten.
            Text(
                text = when {
                    gpsStatus == "leer" && hrStatus == "leer" && tempoStatus == "leer" && splitsStatus == "leer" ->
                        "Der Server hat fuer dieses Training keine Detail-Daten geliefert. Bei aelteren Trainings ist das normal — die Zepp-Cloud loescht Detail-Daten nach einigen Wochen."
                    gpsStatus == "geladen-leer" || hrStatus == "geladen-leer" || tempoStatus == "geladen-leer" || splitsStatus == "geladen-leer" ->
                        "Daten kamen vom Server, aber das Format konnte nicht gelesen werden. Bitte erneut laden."
                    else ->
                        "Einige Detail-Daten konnten noch nicht geladen werden. Tippe auf 'Erneut laden' um es nochmal zu versuchen."
                },
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = {
                    reloadInProgress = true
                    onReload()
                },
                enabled = !reloadInProgress,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(if (reloadInProgress) "Wird geladen …" else "Erneut laden")
            }
        }
    }
}

@Composable
private fun DatenLueckeRow(label: String, status: String) {
    val cosmos = LocalCosmos.current
    val (statusText, statusColor) = when (status) {
        "ok" -> "verfuegbar" to androidx.compose.ui.graphics.Color(0xFF4CAF50)
        "leer" -> "vom Server nicht geliefert" to cosmos.textSecondary
        "geladen-leer" -> "Format-Fehler" to CosmosColors.Critical
        else -> "?" to cosmos.textSecondary
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = cosmos.textPrimary)
        Text(statusText, style = MaterialTheme.typography.labelSmall, color = statusColor)
    }
}

/* ============================== SCHWIMM ============================== */

@Composable
private fun SchwimmCard(w: AmazfitWorkoutEntity) {
    // Server liefert swolf=-1 und poolLengthMeters=0 fuer Nicht-Schwimm-Workouts
    // (statt null). Nur anzeigen wenn ECHTE Werte da sind. Frank-Bug 2026-05-09:
    // Schwimm-Card erschien faelschlich bei jedem Trailrunning-Workout.
    val hasSwolf = (w.swolf ?: 0) > 0
    val hasPool = (w.poolLengthMeters ?: 0.0) > 0.0
    val hasLaps = (w.poolLaps ?: 0) > 0
    if (!hasSwolf && !hasPool && !hasLaps) return
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
    val trimmed = s.trim()
    // Frank-Wunsch 2026-05-16: Polar-Bulk-Import liefert HR als JSON-Array
    // `[[ts, hr], [ts, hr], ...]` — Zepp liefert es als Delta-encoded
    // "timeDelta,hrDelta;..." Format. Parser erkennt beide automatisch.
    if (trimmed.startsWith("[")) {
        return parseJsonTimestampValueAsIntList(trimmed, valueIndex = 1, minVal = 30, maxVal = 230)
    }
    val deltas = mutableListOf<Int>()
    for (sample in s.split(";")) {
        if (sample.isBlank()) continue
        val parts = sample.split(",")
        val v = (parts.getOrNull(1) ?: parts.getOrNull(0))?.trim()?.toIntOrNull() ?: continue
        deltas.add(v)
    }
    val out = mutableListOf<Int>()
    var sum = 0
    for (d in deltas) {
        sum += d
        if (sum in 30..230) out.add(sum)
    }
    return out
}

/**
 * Frank-Wunsch 2026-05-16: Parst JSON-Array-Streams `[[ts, value], ...]`.
 * Liefert eine Liste der Values als Int. Defensiv: Plausibilitaets-Range.
 */
private fun parseJsonTimestampValueAsIntList(json: String, valueIndex: Int, minVal: Int, maxVal: Int): List<Int> {
    val out = mutableListOf<Int>()
    runCatching {
        val arr = Json.parseToJsonElement(json) as? JsonArray
            ?: return emptyList()
        for (item in arr) {
            val pair = item as? JsonArray ?: continue
            val v = pair.getOrNull(valueIndex)?.let { el ->
                runCatching {
                    (el as? JsonPrimitive)?.intOrNull
                        ?: (el as? JsonPrimitive)?.content?.toDoubleOrNull()?.toInt()
                }.getOrNull()
            } ?: continue
            if (v in minVal..maxVal) out.add(v)
        }
    }
    return out
}

/**
 * Wie oben, aber liefert Double-Werte (fuer Pace).
 */
private fun parseJsonTimestampValueAsDoubleList(json: String, valueIndex: Int, minVal: Double, maxVal: Double): List<Double> {
    val out = mutableListOf<Double>()
    runCatching {
        val arr = Json.parseToJsonElement(json) as? JsonArray
            ?: return emptyList()
        for (item in arr) {
            val pair = item as? JsonArray ?: continue
            val v = pair.getOrNull(valueIndex)?.let { el ->
                runCatching {
                    (el as? JsonPrimitive)?.doubleOrNull
                }.getOrNull()
            } ?: continue
            if (v in minVal..maxVal) out.add(v)
        }
    }
    return out
}

/**
 * Parst den hochaufgeloesten Pace-Stream aus dem Detail-Endpoint
 * `data.pace`. Format vermutlich "time_delta,pace_value;..." mit Werten in
 * Sekunden pro Meter (gleiche Einheit wie avg_pace) — konvertiert auf
 * Sekunden pro Kilometer durch ×1000. Defensiv: falls Werte gross genug
 * sind (>50), schon sec/km, kein zusaetzliches Skalieren.
 */
private fun parsePaceStream(s: String?): List<Double> {
    if (s.isNullOrBlank()) return emptyList()
    val trimmed = s.trim()
    // Frank-Wunsch 2026-05-16: Polar-Bulk-Import liefert Pace als JSON-Array
    // `[[ts, paceSecPerKm], ...]` direkt — bereits in sec/km, kein Skalieren noetig.
    if (trimmed.startsWith("[")) {
        return parseJsonTimestampValueAsDoubleList(trimmed, valueIndex = 1, minVal = 150.0, maxVal = 1500.0)
    }
    val out = mutableListOf<Double>()
    for (sample in s.split(";")) {
        if (sample.isBlank()) continue
        val parts = sample.split(",")
        val v = parts.lastOrNull()?.trim()?.toDoubleOrNull() ?: continue
        // Plausibilitaet: nur positive Werte, kein -1 / -2000000 (NO_VALUE-Marker
        // aus rolandsz/exporters/base_exporter.py).
        if (v <= 0 || v < -1000000) continue
        // Skalierung: <50 → sec/m × 1000, >=50 → schon sec/km
        val secPerKm = if (v < 50.0) v * 1000.0 else v
        // Pace-Plausibilitaet: 150-1500 sec/km (2:30 bis 25:00 min/km)
        if (secPerKm in 150.0..1500.0) out.add(secPerKm)
    }
    return out
}

/**
 * Parst kilo_pace / lap String. Format ist je nach Zepp-Version:
 *   "503.5;504.2;505.1"           — nur Pace-Werte
 *   "1000,503.5;2000,504.2"        — Distanz,Pace pro Kilometer-Marker
 *   "503.5|504.2"                  — alte Pipe-Variante
 * Wir nehmen pro Sample die LETZTE Zahl (= Pace bei Distanz-Marker-Format).
 */
private fun parsePipeDoubleList(s: String?): List<Double> {
    if (s.isNullOrBlank()) return emptyList()
    val out = mutableListOf<Double>()
    for (token in s.split(";", "|")) {
        if (token.isBlank()) continue
        val parts = token.trim().split(",")
        val v = parts.lastOrNull()?.trim()?.toDoubleOrNull() ?: continue
        out.add(v)
    }
    return out
}

/**
 * Parst GPS-Track. Format aus rolandsz/exporters/base_exporter.py:
 *   "latInt,lonInt;latInt,lonInt;..." mit Semikolon zwischen Punkten
 *   und Komma zwischen Lat und Lon je Punkt.
 * Werte sind DELTA-encoded (erster Punkt absolut), Skalierung ÷ 100_000_000.
 */
private fun parseGpsPoints(s: String?): List<Pair<Double, Double>> {
    if (s.isNullOrBlank()) return emptyList()
    val trimmed = s.trim()
    // Frank-Wunsch 2026-05-16: Polar-Bulk-Import liefert GPS als JSON-Array
    // `[[lat, lon, alt, ts], ...]` mit Werten direkt als Dezimalgrad —
    // KEIN Delta, KEIN Skalieren noetig.
    if (trimmed.startsWith("[")) {
        val out = mutableListOf<Pair<Double, Double>>()
        runCatching {
            val arr = Json.parseToJsonElement(trimmed)
                as? JsonArray ?: return@runCatching
            for (item in arr) {
                val pt = item as? JsonArray ?: continue
                val lat = (pt.getOrNull(0) as? JsonPrimitive)?.doubleOrNull ?: continue
                val lon = (pt.getOrNull(1) as? JsonPrimitive)?.doubleOrNull ?: continue
                if (lat in -90.0..90.0 && lon in -180.0..180.0) out.add(lat to lon)
            }
        }
        return out
    }
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
 * Schaetzt VO2Max aus den ECHTEN Workout-Daten — kombiniert ACSM-Lauf-Formel
 * mit HR-Reserve nach Karvonen. Frank-Wunsch 2026-05-09: pro Lauf einen
 * eigenen Wert, nicht eine Konstante.
 *
 * Schritt 1: VO2 bei diesem Workout (ml/kg/min) aus Lauf-Geschwindigkeit:
 *   VO2_workout = 0.2 × Speed (m/min) + 3.5    [ACSM-Formel fuer Laufen]
 *
 * Schritt 2: HR-Reserve-Anteil — wieviel % der HR-Reserve nutzt Frank waehrend
 * dieses Workouts? Wenn er bei moderater Belastung gleichen Speed schafft,
 * ist seine VO2Max hoeher als wenn er voll ausbelastet sein muesste:
 *   HR_reserve_anteil = (avg_HR − rest_HR) / (max_HR − rest_HR)
 *
 * Schritt 3: Hochrechnung auf Maximum:
 *   VO2Max ≈ VO2_workout / HR_reserve_anteil
 *
 * HR_max = 180 (Frank's persoenlicher Maximalpuls — Frank-Info 2026-05-09)
 * HR_rest = 65 (Default — sollte spaeter aus AmazfitDaily kommen)
 *
 * Verifikation Frank's Trailrunning 7.16km/3612s: speed=119 m/min,
 * VO2_workout=27.3, avg_HR=146, HR_reserve=0.70, VO2Max ≈ 39 — passt zu
 * Zepp-App-Anzeige 41 (kleine Abweichung wegen Hoehenmeter beim Trail).
 */
/**
 * Schaetzt VO2Max nach der ACSM-Formel mit HR-Reserve-Hochrechnung.
 * Dies ist die wissenschaftlich anerkannte Methode (American College of
 * Sports Medicine — Quelle ACSM Guidelines for Exercise Testing 11. Auflage)
 * und wird auch von Garmin Firstbeat als Grundlage genutzt.
 *
 * Schritt 1 — VO2 bei diesem Workout (ml/kg/min):
 *   VO2_workout = 0.2 × Speed (m/min) + 3.5    [ACSM-Lauf-Formel]
 *
 * Schritt 2 — HR-Reserve-Anteil (Karvonen):
 *   HR_reserve_anteil = (avg_HR − rest_HR) / (max_HR − rest_HR)
 *
 * Schritt 3 — Hochrechnung auf Maximum:
 *   VO2Max ≈ VO2_workout / HR_reserve_anteil
 *
 * Korrektur +2 (Frank-Empirie 2026-05-09): Frank's Zepp-App zeigt fuer den
 * heutigen Lauf 41, reine ACSM gibt 38-39 — also +2 Konstante damit die
 * absoluten Werte zu seinem Geraet passen. Die Variation zwischen Workouts
 * bleibt korrekt, nur der Offset wird angeglichen.
 *
 * HR_max = 180 (Frank's persoenlicher Maximalpuls)
 * HR_rest = 65 (Default — Frank's typischer Ruhepuls)
 */
private fun estimateVo2Max(w: de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity): String {
    val avgHr = w.avgHeartRate ?: return "—"
    val distance = w.distanceMeters ?: return "—"
    val duration = w.durationSeconds ?: return "—"
    if (duration < 60 || distance < 100) return "—"
    if (avgHr < 80 || avgHr > 220) return "—"
    val frankMaxHr = 180
    val restingHr = 65
    if (frankMaxHr <= restingHr) return "—"
    val durationMin = duration / 60.0
    val speedMperMin = distance / durationMin
    val vo2Workout = 0.2 * speedMperMin + 3.5
    val hrReserveFraction = (avgHr - restingHr).toDouble() / (frankMaxHr - restingHr).toDouble()
    if (hrReserveFraction <= 0.1) return "—"
    val vo2MaxAcsm = vo2Workout / hrReserveFraction
    val vo2Max = vo2MaxAcsm + 2.0
    if (vo2Max < 20 || vo2Max > 80) return "—"
    return "≈ ${formatVo2(vo2Max)}"
}

/** VO2Max mit deutschem Komma-Format — Frank-Wunsch 2026-05-09: "40,8" statt "41". */
private fun formatVo2(v: Double): String =
    "%.1f".format(v).replace(".", ",")

/**
 * Frank-Wunsch 2026-05-16: Quellenabhaengige Beschriftung des Workout-Chips
 * und des Default-Titels. Polar ist ab jetzt die alleinige Workout-Quelle.
 *
 * Frank-Update 2026-05-16 (Iteration 2): Frank verlangt dass die alten
 * Eintraege NICHT mehr als "T-Rex 3" angezeigt werden — sie sind aus seiner
 * Sicht ohnehin obsolet (werden beim naechsten erfolgreichen Polar-Bulk-
 * Import geloescht). Bis dahin sollen sie neutral "Polar" zeigen, da Frank
 * seine Trainings konzeptionell als Polar-Trainings sieht (sein H10 ist
 * ja immer dran, auch wenn die Aufzeichnung historisch ueber Zepp lief).
 *
 * - source startsWith "polar"   → "Polar"
 * - source == "health_connect"  → "Health Connect"
 * - source enthaelt "huami"     → "Polar" (frueher: "T-Rex 3", aufgehoben 2026-05-16)
 * - source == null oder leer    → "Polar"
 * - Sonstiges                   → "Polar"
 */
private fun sourceLabel(source: String?): String {
    if (source.isNullOrBlank()) return "Polar"
    val lc = source.lowercase()
    return when {
        lc.startsWith("polar") -> "Polar"
        lc == "health_connect" -> "Health Connect"
        else -> "Polar"
    }
}

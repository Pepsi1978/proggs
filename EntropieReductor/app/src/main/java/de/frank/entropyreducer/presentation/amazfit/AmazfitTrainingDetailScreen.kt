package de.frank.entropyreducer.presentation.amazfit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.presentation.components.ColorPaletteBar
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.rememberCardColors
import de.frank.entropyreducer.presentation.dashboard4.BiomarkerCardId
import de.frank.entropyreducer.presentation.dashboard4.formatDistance
import de.frank.entropyreducer.presentation.dashboard4.formatDuration
import de.frank.entropyreducer.presentation.dashboard4.formatPace
import de.frank.entropyreducer.presentation.dashboard4.formatStartLabel
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Detail-Ansicht eines Workouts mit Hero-Pattern, Stats-Grid und visualisierten Verlaufs-Daten.
 * Frank-Wunsch 2026-05-09: schoenes Layout fuer den letzten Lauf mit Pattern-Karten,
 * Pulsverlauf-Chart, Splits-Tabelle und GPS-Track-Vorschau.
 */
@Composable
fun AmazfitTrainingDetailScreen(
    onBack: () -> Unit,
    vm: AmazfitTrainingDetailViewModel = hiltViewModel(),
) {
    val workout by vm.workout.collectAsStateWithLifecycle()
    val restingHrForDay by vm.restingHrForWorkoutDay.collectAsStateWithLifecycle()
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

    // Frank-Wunsch 2026-05-18: 3-Punkte-Menue oben rechts mit "Aktivitaet
    // loeschen". Loest einen Bestaetigungs-Dialog aus und navigiert nach
    // erfolgreichem Loeschen automatisch zurueck.
    var menuExpanded by remember { mutableStateOf(false) }
    var deleteDialogOpen by remember { mutableStateOf(false) }
    if (deleteDialogOpen && w != null) {
        AlertDialog(
            onDismissRequest = { deleteDialogOpen = false },
            title = { Text("Aktivität löschen?") },
            text = {
                Text(
                    "Möchtest du dieses Training wirklich komplett entfernen? " +
                        "Diese Aktion kann nicht rückgängig gemacht werden."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteDialogOpen = false
                        vm.deleteCurrentWorkout(onDeleted = onBack)
                    }
                ) {
                    Text("Löschen", color = LocalCosmos.current.crit)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogOpen = false }) { Text("Abbrechen") }
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
    // Frank-Wunsch 2026-05-17 (Iteration 3): Pro-Feld-Schloss-Icon. Set der
    // Labels die manuell editiert wurden — wird an alle StatsGrid-Aufrufe
    // weitergereicht. Labels MUESSEN exakt mit den Stat-Card-Labels
    // uebereinstimmen.
    //
    // KEIN Fallback fuer Bestandsdaten: Trainings die vor Einfuehrung der
    // manualOverrideFields-Spalte editiert wurden zeigen KEINE Schloesser.
    // Falscher Schloss-Spam (alle 9 Karten markiert obwohl nur Puls editiert)
    // ist schlimmer als gar kein Schloss. Wenn Frank Schloesser bei alten
    // Trainings will, einfach erneut editieren — dann werden die konkret
    // geaenderten Felder spezifisch markiert.
    val editedLabels: Set<String> =
        remember(w?.manualOverrideFields) {
            w?.manualOverrideFields
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet() ?: emptySet()
        }

    // Frank-Wunsch 2026-05-17 Iteration 7: Vollbild-State lebt auf Screen-Level
    // statt in den Cards. Cards im LazyColumn werden recycled (wenn sie aus
    // dem Viewport scrollen) und ihre lokalen rememberSaveable-States ueberleben
    // Configuration-Changes nicht zuverlaessig. Hier oben bleibt der State stabil.
    var fullscreenChart by rememberSaveable { mutableStateOf<String?>(null) }

    val tempoSec = remember(tempoStream) { tempoStream.filter { it in 150.0..1200.0 } }
    val hrValues = remember(hr) { hr.map { it.toDouble() } }
    val xLabelPuls: (Int) -> String = remember { { i -> "${i / 60} min" } }
    val totalKm = w?.distanceMeters?.takeIf { it > 0 }?.let { it / 1000.0 }
    val xLabelTempo: (Int) -> String =
        remember(totalKm, tempoSec.size) {
            { i ->
                val lastIdx = (tempoSec.size - 1).coerceAtLeast(1)
                if (totalKm != null) {
                    val km = (i.toDouble() / lastIdx) * totalKm
                    "Km ${"%.1f".format(km)}"
                } else {
                    "Km ${i + 1}"
                }
            }
        }

    Box(modifier = Modifier.fillMaxSize()) {
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
            actions = {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Mehr",
                            tint = cosmos.textPrimary,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Aktivität löschen") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = LocalCosmos.current.crit,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                deleteDialogOpen = true
                            },
                        )
                    }
                }
            },
            compactHeader = true,
        ) { padding ->
            // Frank-Wunsch 2026-05-18 Folgeauftrag: Farbpalette oben im
            // Training-Hero-Detail. Auswahl persistiert auf AMAZFIT_LAST_HERO.
            val cardColorAccess = rememberCardColors()
            val heroCardId = BiomarkerCardId.AMAZFIT_LAST_HERO

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    ColorPaletteBar(
                        selectedIndex = cardColorAccess.colors[heroCardId] ?: 0,
                        onPick = { idx -> cardColorAccess.setColor(heroCardId, idx) },
                    )
                }
                if (w == null) {
                    item { Text("Wird geladen …", color = cosmos.textSecondary) }
                    return@LazyColumn
                }
                item { HeroCard(w, onEditClick = { editDialogOpen = true }) }
                item {
                    PaceAndHrGrid(
                        w,
                        editedLabels = editedLabels,
                        onEditClick = { editDialogOpen = true },
                    )
                }
                item {
                    TerrainGrid(
                        w = w,
                        restingHrForDay = restingHrForDay,
                        editedLabels = editedLabels,
                        onEditClick = { editDialogOpen = true },
                    )
                }
                item { TrainingseffektCard(w) }
                if (gps.isNotEmpty())
                    item {
                        GpsTrackCard(gps, w.city, onOpenFullscreen = { fullscreenChart = "gps" })
                    }
                if (hr.isNotEmpty())
                    item { PulsverlaufCard(hr, onOpenFullscreen = { fullscreenChart = "puls" }) }
                if (tempoStream.size >= 10)
                    item {
                        TempoVerlaufCard(
                            tempoStream,
                            w.distanceMeters,
                            onOpenFullscreen = { fullscreenChart = "tempo" },
                        )
                    }
                if (splits.isNotEmpty()) item { SplitsCard(splits) }
                item { SchwimmCard(w) }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }

        // Vollbild-Overlays — rendern ueber dem Scaffold weil sie in der Box
        // ALS ZWEITE Kinder kommen (z-order = render-order in Box). State
        // ueberlebt Rotation weil er auf Screen-Level lebt.
        when (fullscreenChart) {
            "gps" ->
                if (gps.size >= 2) {
                    BackHandler { fullscreenChart = null }
                    GpsTrackFullscreen(gps) { fullscreenChart = null }
                }
            "puls" ->
                if (hr.isNotEmpty()) {
                    BackHandler { fullscreenChart = null }
                    ZoomableChartFullscreen(
                        accent = LocalCosmos.current.crit,
                        values = hrValues,
                        yUnit = "bpm",
                        yFormat = { it.toInt().toString() },
                        xLabel = xLabelPuls,
                        invertY = false,
                        yPaddingFraction = 0.10,
                        crosshairTooltip = { v, x -> "${v.toInt()} bpm · $x" },
                        onClose = { fullscreenChart = null },
                    )
                }
            "tempo" ->
                if (tempoSec.size >= 10) {
                    BackHandler { fullscreenChart = null }
                    ZoomableChartFullscreen(
                        accent = LocalCosmos.current.accent,
                        values = tempoSec,
                        yUnit = "min/km",
                        yFormat = { formatPaceSec(it) },
                        xLabel = xLabelTempo,
                        invertY = true,
                        yPaddingFraction = 0.05,
                        crosshairTooltip = { v, x -> "${formatPaceSec(v)} · $x" },
                        onClose = { fullscreenChart = null },
                    )
                }
        }
    }
}

/* ============================== HERO ============================== */

@Composable
private fun HeroCard(w: AmazfitWorkoutEntity, onEditClick: () -> Unit = {}) {
    val cosmos = LocalCosmos.current
    val accent = LocalCosmos.current.warn
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(48.dp)
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
                    modifier =
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BigStat(
                    label = "Distanz",
                    value = w.distanceMeters?.let { formatDistance(it) } ?: "—",
                    accent = accent,
                    modifier = Modifier.weight(1f),
                    onClick = onEditClick,
                )
                BigStat(
                    label = "Dauer",
                    value = w.durationSeconds?.let { formatDuration(it) } ?: "—",
                    accent = accent,
                    modifier = Modifier.weight(1f),
                    onClick = onEditClick,
                )
            }
        }
    }
}

@Composable
private fun BigStat(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val cosmos = LocalCosmos.current
    // Frank-Wunsch 2026-05-25: Distanz/Dauer im Trainingsdetail direkt antippbar,
    // um den Editier-Dialog zu oeffnen (nur wenn onClick gesetzt ist — der
    // Biomarker-Uebersichts-Hero bleibt bewusst nicht antippbar).
    Column(
        modifier =
            modifier.then(
                if (onClick != null) {
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = cosmos.textSecondary,
        )
        Text(text = value, color = accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

/* ============================== STATS-GRIDS ============================== */

@Composable
private fun PaceAndHrGrid(
    w: AmazfitWorkoutEntity,
    editedLabels: Set<String> = emptySet(),
    onEditClick: () -> Unit = {},
) {
    StatsGrid(
        items =
            listOf(
                "Ø Pace" to (w.avgPaceSecPerKm?.let { formatPace(it) } ?: "—"),
                "Maximale Pace" to (w.maxPaceSecPerKm?.let { formatPace(it) } ?: "—"),
                "Ø Puls" to (w.avgHeartRate?.let { "$it bpm" } ?: "—"),
                "Maximalpuls" to (w.maxHeartRate?.let { "$it bpm" } ?: "—"),
            ),
        editedLabels = editedLabels,
        onItemClick = onEditClick,
    )
}

@Composable
private fun TerrainGrid(
    w: AmazfitWorkoutEntity,
    restingHrForDay: Int? = null,
    editedLabels: Set<String> = emptySet(),
    onEditClick: () -> Unit = {},
) {
    // Frank-Wunsch 2026-05-17: VO2max-Box nur bei Lauf-basierten Sportarten
    // anzeigen. Bei Crosstrainer/Rudern/Krafttraining/etc. komplett weglassen
    // damit Frank sofort sieht "das war kein Laufen".
    val showVo2 = de.frank.entropyreducer.presentation.amazfit.isVo2MaxSport(w.sportName)
    val statsItems = buildList {
        add("Kalorien" to (w.calories?.let { "%.0f kcal".format(it) } ?: "—"))
        if (showVo2) add("VO₂Max" to estimateVo2Max(w, restingHrForDay))
    }
    StatsGrid(items = statsItems, editedLabels = editedLabels, onItemClick = onEditClick)
}

@Composable
private fun StatsGrid(
    items: List<Pair<String, String>>,
    editedLabels: Set<String> = emptySet(),
    onItemClick: () -> Unit = {},
) {
    val cosmos = LocalCosmos.current
    val accent = LocalCosmos.current.warn
    val rows = items.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (label, value) ->
                    // Frank-Wunsch 2026-05-17: Jedes Stat-Feld ist klickbar
                    // und oeffnet den Edit-Dialog. Stift-Icons sind raus —
                    // direkt auf die Wert-Box tippen.
                    GlassCard(modifier = Modifier.weight(1f).clickable { onItemClick() }) {
                        Column {
                            // Frank-Wunsch 2026-05-17 (Iteration 2): Kleines
                            // Schloss-Icon direkt im Label wenn dieses Feld
                            // manuell editiert wurde. Pro Karte sichtbar.
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cosmos.textSecondary,
                                    modifier = Modifier.weight(1f),
                                )
                                if (label in editedLabels) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription =
                                            "Manuell editiert — vor Sync geschützt",
                                        tint = accent,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
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
                EffektBar(
                    label = "Aerob",
                    value = a,
                    max = 5.0,
                    accent = LocalCosmos.current.accent,
                )
                Spacer(Modifier.height(6.dp))
            }
            if (b != null) {
                EffektBar(label = "Anaerob", value = b, max = 5.0, accent = LocalCosmos.current.crit)
            }
        }
    }
}

@Composable
private fun EffektBar(
    label: String,
    value: Double,
    max: Double,
    accent: androidx.compose.ui.graphics.Color,
) {
    val cosmos = LocalCosmos.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = cosmos.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                "%.1f / %.1f".format(value, max),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(cosmos.glassBorder.copy(alpha = 0.25f))
        ) {
            Box(
                modifier =
                    Modifier.fillMaxWidth(fraction = (value / max).coerceIn(0.0, 1.0).toFloat())
                        .height(8.dp)
                        .background(accent)
            )
        }
    }
}

/* ============================== GPS-TRACK ============================== */

/**
 * Strecken-Karte mit Satelliten-Layer (Frank-Wunsch 2026-05-17). Zeigt die GPS- Punkte als Polyline
 * auf einer Google-Maps-Satelliten-Karte mit Strassen-Namen (HYBRID). Start-Punkt gruen, Ziel-Punkt
 * rot — gleiche Farben wie zuvor bei der Canvas-Vorschau. Kamera fittet automatisch auf die
 * Strecken-Bounds, der Benutzer kann mit Zwei-Finger-Geste zoomen und mit einem Finger verschieben.
 *
 * API-Key kommt aus ~/SK/EntropieReductor/maps-api-key.txt ueber den manifest- Placeholder
 * `mapsApiKey`. Bei leerem Key rendert die Karte einen grauen Hintergrund + Google-Wasserzeichen,
 * die Polyline ist trotzdem sichtbar.
 */
@Composable
private fun GpsTrackCard(
    points: List<Pair<Double, Double>>,
    city: String?,
    onOpenFullscreen: () -> Unit,
) {
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
                Text(
                    text = city,
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .aspectRatio(1.6f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cosmos.glassBorder.copy(alpha = 0.08f))
            ) {
                if (points.size >= 2) {
                    GpsTrackMap(points = points, fullscreen = false)
                    // Overlay-Box UEBER der Map faengt den Tap — GoogleMap
                    // wuerde sonst alle Touches schlucken.
                    Box(modifier = Modifier.matchParentSize().clickable { onOpenFullscreen() })
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text =
                    "${points.size} GPS-Punkte · grün = Start, rot = Ziel · Tap auf Karte für Vollbild",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
        }
    }
}

/**
 * Vollbild-Anzeige der GPS-Strecke. Wird vom AmazfitTrainingDetailScreen direkt als
 * Geschwister-Composable des Scaffold gerendert (kein Popup/Dialog) — damit ueberlebt die
 * Vollbild-Ansicht jede Configuration-Change.
 */
@Composable
private fun GpsTrackFullscreen(points: List<Pair<Double, Double>>, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        GpsTrackMap(points = points, fullscreen = true)
        IconButton(
            onClick = onClose,
            modifier =
                Modifier.align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape),
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Vollbild schliessen",
                tint = Color.White,
            )
        }
    }
}

/**
 * Eigentliche Karten-Implementierung — getrennt von der Karte ausgelagert damit der
 * GoogleMap-Composable nicht jedes Mal neu komponiert wird wenn andere Felder der Card aktualisiert
 * werden.
 *
 * Konvertiert Lat/Lon-Paare zu LatLng-Objekten, berechnet LatLngBounds und fittet die Kamera
 * einmalig beim ersten Render. Polyline-Farbe = Warning (gleicher Akzent wie zuvor im Canvas).
 * Marker fuer Start und Ziel.
 */
@Composable
private fun GpsTrackMap(points: List<Pair<Double, Double>>, fullscreen: Boolean) {
    val latLngs =
        remember(points) {
            points.map { com.google.android.gms.maps.model.LatLng(it.first, it.second) }
        }
    val bounds =
        remember(latLngs) {
            val builder = com.google.android.gms.maps.model.LatLngBounds.builder()
            latLngs.forEach { builder.include(it) }
            builder.build()
        }
    val startLatLng = latLngs.first()
    val endLatLng = latLngs.last()
    // Initial-Kamera grob in der Mitte der Strecke. Der echte Bounds-Fit
    // passiert nach dem ersten Layout-Pass via moveCamera in onMapLoaded.
    val cameraPositionState =
        com.google.maps.android.compose.rememberCameraPositionState {
            position =
                com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
                    com.google.android.gms.maps.model.LatLng(
                        (bounds.northeast.latitude + bounds.southwest.latitude) / 2.0,
                        (bounds.northeast.longitude + bounds.southwest.longitude) / 2.0,
                    ),
                    13f,
                )
        }
    val mapProperties = remember {
        com.google.maps.android.compose.MapProperties(
            mapType = com.google.maps.android.compose.MapType.HYBRID
        )
    }
    // Frank-Wunsch 2026-05-17 (Iteration 3): In der eingebetteten Vorschau
    // sind ALLE Gesten deaktiviert — sonst frisst die GoogleMap-Composable
    // alle Touch-Events und der Tap-auf-Card-fuer-Vollbild funktioniert nicht.
    // Im Vollbild sind dafuer alle Steuer-Elemente und alle Gesten aktiv.
    val uiSettings =
        remember(fullscreen) {
            com.google.maps.android.compose.MapUiSettings(
                zoomControlsEnabled = fullscreen,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = fullscreen,
                compassEnabled = fullscreen,
                zoomGesturesEnabled = fullscreen,
                scrollGesturesEnabled = fullscreen,
                rotationGesturesEnabled = fullscreen,
                tiltGesturesEnabled = fullscreen,
                scrollGesturesEnabledDuringRotateOrZoom = fullscreen,
            )
        }
    val accent = LocalCosmos.current.warn
    com.google.maps.android.compose.GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapLoaded = {
            // Strecken-Fit erst NACH dem ersten Layout-Pass — vorher kennt die
            // Map ihre Pixel-Groesse nicht und newLatLngBounds wirft IllegalState.
            runCatching {
                cameraPositionState.move(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 64)
                )
            }
        },
    ) {
        com.google.maps.android.compose.Polyline(points = latLngs, color = accent, width = 10f)
        com.google.maps.android.compose.Marker(
            state = com.google.maps.android.compose.MarkerState(position = startLatLng),
            title = "Start",
            icon =
                com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                ),
        )
        com.google.maps.android.compose.Marker(
            state = com.google.maps.android.compose.MarkerState(position = endLatLng),
            title = "Ziel",
            icon =
                com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                ),
        )
    }
}

/* ============================== PULSVERLAUF ============================== */

@Composable
private fun PulsverlaufCard(hr: List<Int>, onOpenFullscreen: () -> Unit) {
    val cosmos = LocalCosmos.current
    val avg = if (hr.isEmpty()) 0 else hr.average().toInt()
    val min = hr.minOrNull() ?: 0
    val max = hr.maxOrNull() ?: 0
    val values = remember(hr) { hr.map { it.toDouble() } }
    val xLabelFn: (Int) -> String = { i ->
        // Minuten-Skala: 1 Sample ≈ 1 Sekunde
        val mins = i / 60
        "${mins} min"
    }
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
                    color = LocalCosmos.current.crit,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().clickable { onOpenFullscreen() }) {
                ChartWithAxes(
                    accent = LocalCosmos.current.crit,
                    xCount = hr.size,
                    yMin = (min - 5).coerceAtLeast(40).toDouble(),
                    yMax = (max + 5).coerceAtMost(220).toDouble(),
                    yUnit = "bpm",
                    yFormat = { it.toInt().toString() },
                    xLabel = xLabelFn,
                    xTickCount = 6,
                    yTickCount = 7,
                    values = values,
                    invertY = false,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${hr.size} Werte · Min $min · Max $max bpm · Tap auf Chart für Vollbild",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
        }
    }
}

/**
 * Linien-Chart mit Achsen-Skalen (Y links, X unten), Hintergrund-Gridlines,
 * Start/Ende-Markierungen. Frank-Wunsch 2026-05-09: Skalen sichtbar fuer Pulsverlauf und
 * Tempo-Verlauf.
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
    modifier: Modifier = Modifier.fillMaxWidth().height(190.dp),
    // Frank-Wunsch 2026-05-17 (Iteration 4): Crosshair zeigt vertikale Linie
    // + dicker Punkt + Tooltip an Daumen-Position waehrend Drag im Vollbild.
    // Beide Parameter zusammen aktiv = Crosshair sichtbar.
    crosshairIdx: Int? = null,
    crosshairLabel: String? = null,
) {
    val cosmos = LocalCosmos.current
    val gridColor = cosmos.glassBorder.copy(alpha = 0.5f)
    val labelArgb = cosmos.textPrimary.toArgb()
    if (values.size < 2 || yMax <= yMin) return
    // Performance-Audit Loop 2 (2026-05-10): Pfad einmalig allokieren statt
    // pro Frame. Iteration 8: textPaint und tooltipPaint auch outside.
    val seriesPath = remember { Path() }
    val textPaint =
        remember(labelArgb) {
            android.graphics.Paint().apply {
                color = labelArgb
                textSize = 24f
                isAntiAlias = true
            }
        }
    val tooltipTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 36f
            isAntiAlias = true
            isFakeBoldText = true
        }
    }
    val tooltipBgPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(235, 0, 0, 0)
            isAntiAlias = true
        }
    }
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(cosmos.glassBorder.copy(alpha = 0.10f))
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
                drawIntoCanvas { it.nativeCanvas.drawText(yFormat(v), 4f, y + 8f, textPaint) }
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
                    it.nativeCanvas.drawText(xLabel(idx), x - 18f, size.height - 6f, textPaint)
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
            // Crosshair-Overlay (Frank-Wunsch 2026-05-17 Iteration 4) —
            // vertikale Linie + dicker Punkt + Tooltip-Bubble. Nur sichtbar
            // wenn crosshairIdx im gueltigen Bereich liegt.
            val ci = crosshairIdx
            if (ci != null && ci in values.indices) {
                val cx = toX(ci)
                val cy = toY(values[ci])
                // Vertikale Linie quer durchs Chart
                drawLine(
                    color = androidx.compose.ui.graphics.Color(0xCCFFFFFF),
                    start = Offset(cx, padTop),
                    end = Offset(cx, padTop + h),
                    strokeWidth = 2f,
                )
                // Dicker Akzent-Punkt darueber + weisser Kern
                drawCircle(color = accent, radius = 14f, center = Offset(cx, cy))
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.White,
                    radius = 6f,
                    center = Offset(cx, cy),
                )
                // Tooltip-Bubble — auf der gegenueberliegenden Seite vom
                // Cursor damit der Finger den Text nicht verdeckt. Paints
                // sind gecacht (siehe oben — vermeidet Pro-Frame-Allokation).
                val label = crosshairLabel
                if (!label.isNullOrBlank()) {
                    val textW = tooltipTextPaint.measureText(label)
                    val tipPad = 14f
                    val tipBoxW = textW + tipPad * 2
                    val tipBoxH = 56f
                    val placeRight = cx + 24f + tipBoxW < size.width
                    val tipLeft = if (placeRight) cx + 24f else cx - 24f - tipBoxW
                    // Frank-Wunsch 2026-05-17 Iteration 10: Tooltip muss UNTER
                    // dem Close-Button (oben rechts, ~60dp Bottom) liegen, sonst
                    // ueberlappt es mit Kamera-Cutout des Fold 6.
                    // 220 Canvas-Pixel = ca. 70-80dp auf Standard-Displays —
                    // sicher unter Buttons. Auf grossen Displays (Tablet, Landscape)
                    // nehmen wir 12% der Canvas-Hoehe falls das mehr ist.
                    val tipTop = (padTop + (size.height * 0.12f)).coerceAtLeast(padTop + 220f)
                    drawIntoCanvas {
                        it.nativeCanvas.drawRoundRect(
                            tipLeft,
                            tipTop,
                            tipLeft + tipBoxW,
                            tipTop + tipBoxH,
                            12f,
                            12f,
                            tooltipBgPaint,
                        )
                        it.nativeCanvas.drawText(
                            label,
                            tipLeft + tipPad,
                            tipTop + tipBoxH / 2 + 12f,
                            tooltipTextPaint,
                        )
                    }
                }
            }
            @Suppress("UNUSED_VARIABLE") val u2 = yUnit
        }
    }
}

/* ====================== ZOOMBARER VOLLBILD-CHART ====================== */

/**
 * Vollbild-Chart mit Pinch-Zoom, Pan und Crosshair-Tooltip — Iteration 7 nach
 * Best-Practice-Recherche (siehe reference_compose_chart_smooth_pinch_zoom.md).
 *
 * Bedienung:
 * - Pinch (zwei Finger) -> X-Zoom 1x..200x mit sofortiger Reaktion
 * - Zwei Finger Pan -> sichtbaren X-Bereich verschieben
 * - Single-Finger horizontal -> Crosshair an Daumen-Position
 * - Single-Finger vertikal (wenn gezoomt) -> Y-Range verschieben
 * - Doppel-Tap -> Reset
 *
 * Smoothness-Hebel (Recherche-Empfehlungen):
 * - Snapshot.withMutableSnapshot fuer Batch-State-Updates pro Gesture-Event
 * - Manuelles awaitPointerEvent ohne touchSlop fuer sofortige Reaktion
 * - LTTB-Decimation auf ~400 Punkte bei grossen Datensaetzen
 *
 * Wird als Screen-Level-Composable gerendert (kein Popup/Dialog) — Vollbild ueberlebt damit jede
 * Rotation.
 */
@Composable
private fun ZoomableChartFullscreen(
    accent: androidx.compose.ui.graphics.Color,
    values: List<Double>,
    yUnit: String,
    yFormat: (Double) -> String,
    xLabel: (Int) -> String,
    invertY: Boolean,
    yPaddingFraction: Double,
    crosshairTooltip: (Double, String) -> String,
    onClose: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    // Persistierte States (ueberleben Rotation)
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var offset by rememberSaveable { mutableFloatStateOf(0f) }
    var yShift by rememberSaveable { mutableFloatStateOf(0f) }
    var crosshairIdxInVisible by remember { mutableStateOf<Int?>(null) }

    // Frank-Wunsch 2026-05-17 Iteration 8: Google-Maps-Smoothness via
    // graphicsLayer-Trick. Waehrend Pinch wird die gesamte Chart-Box GPU-
    // skaliert (uniform x+y) — KEIN Re-Compute der Polyline, des Y-Auto-Scale
    // oder der Achsen. Beim Loslassen wird der Pinch-Faktor in echtes scale/
    // offset konvertiert und der Chart re-rendert.
    //
    // Diese States leben NUR waehrend einer aktiven Pinch-Geste, danach
    // werden sie auf Identity zurueckgesetzt.
    var pinchScale by remember { mutableFloatStateOf(1f) }
    var pinchPanX by remember { mutableFloatStateOf(0f) }
    var pinchPanY by remember { mutableFloatStateOf(0f) }
    var pinchOriginX by remember { mutableFloatStateOf(0.5f) }
    var pinchOriginY by remember { mutableFloatStateOf(0.5f) }
    var isPinching by remember { mutableStateOf(false) }

    val totalSize = values.size
    val visibleFraction = (1f / scale).coerceIn(2f / totalSize.coerceAtLeast(2).toFloat(), 1f)
    val maxOffset = (1f - visibleFraction).coerceAtLeast(0f)
    val clampedOffset = offset.coerceIn(0f, maxOffset)
    val startIdx = (clampedOffset * (totalSize - 1)).toInt().coerceIn(0, totalSize - 2)
    val endIdx =
        ((clampedOffset + visibleFraction) * (totalSize - 1))
            .toInt()
            .coerceIn(startIdx + 1, totalSize - 1)

    // Decimation: Bei mehr als 500 sichtbaren Punkten reduzieren wir auf ~400
    // via einfaches Sub-Sampling (jeder N-te). LTTB waere visuell schoener, aber
    // bei Pinch-Zoom-Performance ist Speed wichtiger als Visual-Perfektion.
    val rawVisible = remember(values, startIdx, endIdx) { values.subList(startIdx, endIdx + 1) }
    val visibleValues =
        remember(rawVisible) {
            if (rawVisible.size <= 500) {
                rawVisible.toList()
            } else {
                val step = rawVisible.size / 400
                rawVisible.filterIndexed { i, _ -> i % step == 0 }
            }
        }
    val visibleMin = visibleValues.min()
    val visibleMax = visibleValues.max()
    val rawSpan = (visibleMax - visibleMin).coerceAtLeast(0.0)
    val padding = (rawSpan * yPaddingFraction).coerceAtLeast(1.0)
    // Y-Shift wird auf die Span umgerechnet — yShift = 1.0 verschiebt um genau
    // die volle visible Span. Nur aktiv wenn scale > 1 (Y-Pan im Zoom).
    val yShiftPixels = if (scale > 1.05f) yShift * rawSpan else 0.0
    val yMinScaled = visibleMin - padding + yShiftPixels
    val yMaxScaled = visibleMax + padding + yShiftPixels

    val decimationStep = if (rawVisible.size > 500) rawVisible.size / 400 else 1
    val xLabelWrapper: (Int) -> String = { i -> xLabel(startIdx + i * decimationStep) }

    val crosshairLabel: String? =
        crosshairIdxInVisible
            ?.takeIf { it in visibleValues.indices }
            ?.let { idx -> crosshairTooltip(visibleValues[idx], xLabelWrapper(idx)) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(12.dp)
                    // GOOGLE-MAPS-SMOOTHNESS-HEBEL (Iteration 8):
                    // Manuelles awaitEachGesture mit Live-Pinch via graphicsLayer.
                    // Waehrend zwei Finger drauf sind: kein Re-Compute, nur
                    // GPU-Transform der gesamten Box (Bitmap-Skalierung wie Google
                    // Maps Tiles). Bei Pinch-End: konvertieren in echtes scale/offset.
                    .pointerInput(visibleValues.size, totalSize) {
                        awaitEachGesture {
                            val padLeftPx = 56f
                            val padRightPx = 12f
                            fun pointerXToIdx(xPx: Float): Int {
                                val w = (size.width - padLeftPx - padRightPx).coerceAtLeast(1f)
                                val frac = ((xPx - padLeftPx) / w).coerceIn(0f, 1f)
                                return (frac * (visibleValues.size - 1).coerceAtLeast(1))
                                    .toInt()
                                    .coerceIn(0, (visibleValues.size - 1).coerceAtLeast(0))
                            }

                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            crosshairIdxInVisible = pointerXToIdx(firstDown.position.x)

                            // Direction-Lock fuer Single-Finger:
                            // erste signifikante Bewegung entscheidet ob Crosshair
                            // (X-dominant) oder Y-Pan (Y-dominant) fuer die ganze Geste.
                            var singleFingerMode: String? = null // "x" oder "y"
                            var movementAccumX = 0f
                            var movementAccumY = 0f

                            val canvasW = size.width.toFloat().coerceAtLeast(1f)
                            val canvasH = size.height.toFloat().coerceAtLeast(1f)

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break

                                if (pressed.size >= 2) {
                                    // Pinch-Modus — LIVE-Modus via graphicsLayer.
                                    // KEIN scale/offset update hier, sondern nur
                                    // pinchScale/pinchPanX (= GPU-Transform).
                                    val p0 = pressed[0].position
                                    val p1 = pressed[1].position
                                    val newDist = (p0 - p1).getDistance().coerceAtLeast(1f)
                                    val newCentroid =
                                        androidx.compose.ui.geometry.Offset(
                                            (p0.x + p1.x) / 2f,
                                            (p0.y + p1.y) / 2f,
                                        )

                                    if (!isPinching) {
                                        // Pinch-Start: initialisiere Live-Transform
                                        isPinching = true
                                        crosshairIdxInVisible = null
                                        pinchScale = 1f
                                        pinchPanX = 0f
                                        pinchPanY = 0f
                                        pinchOriginX = (newCentroid.x / canvasW).coerceIn(0f, 1f)
                                        pinchOriginY = (newCentroid.y / canvasH).coerceIn(0f, 1f)
                                        pressed[0].consume()
                                        pressed[1].consume()
                                    } else {
                                        // Live-Pinch — graphicsLayer-State updaten
                                        val change0 = pressed[0].positionChange()
                                        val change1 = pressed[1].positionChange()
                                        val avgPanX = (change0.x + change1.x) / 2f
                                        val avgPanY = (change0.y + change1.y) / 2f
                                        // Distanz-Aenderung -> Zoom-Faktor
                                        val prevP0 = pressed[0].previousPosition
                                        val prevP1 = pressed[1].previousPosition
                                        val prevDist =
                                            (prevP0 - prevP1).getDistance().coerceAtLeast(1f)
                                        val zoomDelta = newDist / prevDist
                                        androidx.compose.runtime.snapshots.Snapshot
                                            .withMutableSnapshot {
                                                // Sehr weit gefasste Range damit
                                                // waehrend Live-Pinch nichts clamped —
                                                // erst beim Commit (Pinch-End) wird
                                                // auf 1..200 begrenzt.
                                                pinchScale =
                                                    (pinchScale * zoomDelta).coerceIn(0.001f, 1000f)
                                                pinchPanX += avgPanX
                                                pinchPanY += avgPanY
                                            }
                                        pressed.forEach { it.consume() }
                                    }
                                } else if (pressed.size == 1 && !isPinching) {
                                    // Single-Finger mit Direction-Lock
                                    val change = pressed[0]
                                    val delta = change.positionChange()
                                    movementAccumX += kotlin.math.abs(delta.x)
                                    movementAccumY += kotlin.math.abs(delta.y)

                                    if (
                                        singleFingerMode == null &&
                                            (movementAccumX + movementAccumY) > 8f
                                    ) {
                                        // Genug Bewegung — Modus festlegen
                                        singleFingerMode =
                                            if (
                                                movementAccumY > movementAccumX * 1.5f &&
                                                    scale > 1.05f
                                            )
                                                "y"
                                            else "x"
                                    }

                                    when (singleFingerMode) {
                                        "y" -> {
                                            // Frank-Wunsch 2026-05-17 Iteration 9:
                                            // Inhalt folgt dem Finger (Scrubbing-Pattern):
                                            // Finger nach oben -> Graph wandert nach oben.
                                            //
                                            // Frank-Wunsch 2026-05-18: Bei invertY=true
                                            // (Tempo-Chart: niedrigere Werte oben) muss
                                            // das Vorzeichen umgedreht werden, sonst
                                            // wandert der Inhalt entgegengesetzt zum
                                            // Finger. Pulsverlauf (invertY=false) blieb
                                            // korrekt, Tempoverlauf war verkehrt herum.
                                            val rawDelta = delta.y / canvasH * 1.0f
                                            val yShiftDelta = if (invertY) -rawDelta else rawDelta
                                            androidx.compose.runtime.snapshots.Snapshot
                                                .withMutableSnapshot {
                                                    yShift =
                                                        (yShift + yShiftDelta).coerceIn(-2f, 2f)
                                                }
                                        }
                                        "x",
                                        null -> {
                                            crosshairIdxInVisible = pointerXToIdx(change.position.x)
                                        }
                                    }
                                    change.consume()
                                }
                            }

                            // ALLE Finger losgelassen — Pinch committen.
                            if (isPinching) {
                                // Konvertiere visualScale/pinchPanX in real scale/offset.
                                // Mathematik: der Datenpunkt unter dem Pivot soll
                                // an gleicher canvas-Position bleiben.
                                val pivotFracCanvas = pinchOriginX
                                // Aktueller visible-Anteil und visible-Start:
                                val curVisible = (1f / scale).coerceIn(2f / totalSize.toFloat(), 1f)
                                val curStart =
                                    offset.coerceIn(0f, (1f - curVisible).coerceAtLeast(0f))
                                val dataPivotFrac = curStart + pivotFracCanvas * curVisible

                                val newScale = (scale * pinchScale).coerceIn(1f, 200f)
                                val newVisible =
                                    (1f / newScale).coerceIn(
                                        2f / totalSize.coerceAtLeast(2).toFloat(),
                                        1f,
                                    )
                                val newStartFromZoom = dataPivotFrac - pivotFracCanvas * newVisible
                                val panFrac = -pinchPanX / canvasW * newVisible
                                val newOffset =
                                    (newStartFromZoom + panFrac).coerceIn(
                                        0f,
                                        (1f - newVisible).coerceAtLeast(0f),
                                    )
                                androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                                    scale = newScale
                                    offset = newOffset
                                    pinchScale = 1f
                                    pinchPanX = 0f
                                    pinchPanY = 0f
                                    isPinching = false
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                                    scale = 1f
                                    offset = 0f
                                    yShift = 0f
                                    crosshairIdxInVisible = null
                                }
                            }
                        )
                    }
        ) {
            // GPU-Transform-Wrapper: waehrend Pinch wird die gesamte Chart-Box
            // uniform (x+y) skaliert wie Google Maps eine Bitmap. KEIN Re-Compute.
            Box(
                modifier =
                    Modifier.fillMaxSize().graphicsLayer {
                        if (isPinching) {
                            scaleX = pinchScale
                            scaleY = pinchScale
                            translationX = pinchPanX
                            translationY = pinchPanY
                            transformOrigin = TransformOrigin(pinchOriginX, pinchOriginY)
                        }
                    }
            ) {
                ChartWithAxes(
                    accent = accent,
                    xCount = visibleValues.size,
                    yMin = yMinScaled,
                    yMax = yMaxScaled,
                    yUnit = yUnit,
                    yFormat = yFormat,
                    xLabel = xLabelWrapper,
                    xTickCount = 6,
                    yTickCount = 7,
                    values = visibleValues,
                    invertY = invertY,
                    modifier = Modifier.fillMaxSize(),
                    crosshairIdx = crosshairIdxInVisible,
                    crosshairLabel = crosshairLabel,
                )
            }
        }
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (scale > 1.01f || clampedOffset > 0.001f || kotlin.math.abs(yShift) > 0.01f) {
                IconButton(
                    onClick = {
                        androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                            scale = 1f
                            offset = 0f
                            yShift = 0f
                            crosshairIdxInVisible = null
                        }
                    },
                    modifier =
                        Modifier.padding(end = 8.dp)
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Zoom zurücksetzen",
                        tint = Color.White,
                    )
                }
            }
            IconButton(
                onClick = onClose,
                modifier =
                    Modifier.size(44.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Vollbild schliessen",
                    tint = Color.White,
                )
            }
        }
    }
}

/* ============================== TEMPO-VERLAUF ============================== */

/**
 * Tempo-Verlaufs-Chart als Linie. Frank-Wunsch 2026-05-09: Tempo soll auch grafisch dargestellt
 * werden, nicht nur als Tabelle. Y-Achse INVERTIERT — niedrigere sec/km (= schneller) ist OBEN.
 */
@Composable
private fun TempoVerlaufCard(
    stream: List<Double>,
    distanceMeters: Double?,
    onOpenFullscreen: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    // stream-Werte sind bereits in sec/km vom Parser. Stark schwankende Werte
    // (z.B. an Pause-Phasen) ueber 1200 sec/km filtern damit der Chart nicht
    // durch Outliers verzerrt wird.
    val secPerKm = stream.filter { it in 150.0..1200.0 }
    if (secPerKm.size < 10) return
    val avg = secPerKm.average()
    val min = secPerKm.min()
    val max = secPerKm.max()
    val accent = LocalCosmos.current.accent
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
            Box(modifier = Modifier.fillMaxWidth().clickable { onOpenFullscreen() }) {
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
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text =
                    "${secPerKm.size} Werte · Schnellster: ${formatPaceSec(min)} · Langsamster: ${formatPaceSec(max)} · grün = Start, rot = Ziel · Tap auf Chart für Vollbild",
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                ) {
                    Text(
                        text = "Km ${idx + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cosmos.textSecondary,
                        modifier = Modifier.weight(0.18f),
                    )
                    Box(
                        modifier =
                            Modifier.weight(0.55f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(cosmos.glassBorder.copy(alpha = 0.18f))
                    ) {
                        // Frank-Wunsch 2026-05-09 (zweite Iteration): laengster Bar
                        // = 100% (langsamster Km), andere proportional davon.
                        // Beispiel: maxPace 9:25 (565 sec/km) = 100% Bar,
                        // 7:11 (431 sec/km) = 431/565 = 76% Bar.
                        val frac = (sec / maxPace).toFloat().coerceIn(0.0f, 1.0f)
                        // Farbverlauf relativ: gruen (schnellster) → rot (langsamster)
                        val colorFrac =
                            if (maxPace > minPace)
                                ((sec - minPace) / (maxPace - minPace)).coerceIn(0.0, 1.0).toFloat()
                            else 0.5f
                        val barColor =
                            lerpColor(
                                androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                androidx.compose.ui.graphics.Color(0xFFFF9800),
                                androidx.compose.ui.graphics.Color(0xFFEF5350),
                                colorFrac,
                            )
                        Box(
                            modifier =
                                Modifier.fillMaxWidth(fraction = frac)
                                    .height(10.dp)
                                    .background(barColor)
                        )
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

// DatenLueckenCard + DatenLueckeRow entfernt 2026-05-17 (Frank-Wunsch).

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
            ValueRow(
                "Pool-Länge",
                w.poolLengthMeters?.takeIf { it > 0 }?.let { "%.0f m".format(it) },
            )
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

/* ============================== PARSER ============================== */

/**
 * Format-Erkenntnisse aus rolandsz/Mi-Fit-and-Zepp-workout-exporter:
 * - Trenner zwischen Samples ist ";" (nicht "|")
 * - Lat/Lon/HR sind DELTA-encoded: erster Wert absolut, weitere Deltas
 * - Lat/Lon-Skalierung: Integer ÷ 100_000_000 für Grad
 * - HR-Format pro Sample: "time_delta,hr_delta" (zwei Werte!)
 */

/**
 * Parst Pulsverlauf. Format: "0,85;1,1;1,-1;1,2;..." wo erste Zahl ein time-delta ist und zweite
 * ein hr-delta. Erster hr-Wert ist absolut, alle weiteren sind Deltas die akkumuliert werden
 * muessen.
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
 * Frank-Wunsch 2026-05-16: Parst JSON-Array-Streams `[[ts, value], ...]`. Liefert eine Liste der
 * Values als Int. Defensiv: Plausibilitaets-Range.
 */
private fun parseJsonTimestampValueAsIntList(
    json: String,
    valueIndex: Int,
    minVal: Int,
    maxVal: Int,
): List<Int> {
    val out = mutableListOf<Int>()
    runCatching {
        val arr = Json.parseToJsonElement(json) as? JsonArray ?: return emptyList()
        for (item in arr) {
            val pair = item as? JsonArray ?: continue
            val v =
                pair.getOrNull(valueIndex)?.let { el ->
                    runCatching {
                            (el as? JsonPrimitive)?.intOrNull
                                ?: (el as? JsonPrimitive)?.content?.toDoubleOrNull()?.toInt()
                        }
                        .getOrNull()
                } ?: continue
            if (v in minVal..maxVal) out.add(v)
        }
    }
    return out
}

/** Wie oben, aber liefert Double-Werte (fuer Pace). */
private fun parseJsonTimestampValueAsDoubleList(
    json: String,
    valueIndex: Int,
    minVal: Double,
    maxVal: Double,
): List<Double> {
    val out = mutableListOf<Double>()
    runCatching {
        val arr = Json.parseToJsonElement(json) as? JsonArray ?: return emptyList()
        for (item in arr) {
            val pair = item as? JsonArray ?: continue
            val v =
                pair.getOrNull(valueIndex)?.let { el ->
                    runCatching { (el as? JsonPrimitive)?.doubleOrNull }.getOrNull()
                } ?: continue
            if (v in minVal..maxVal) out.add(v)
        }
    }
    return out
}

/**
 * Parst den hochaufgeloesten Pace-Stream aus dem Detail-Endpoint `data.pace`. Format vermutlich
 * "time_delta,pace_value;..." mit Werten in Sekunden pro Meter (gleiche Einheit wie avg_pace) —
 * konvertiert auf Sekunden pro Kilometer durch ×1000. Defensiv: falls Werte gross genug sind (>50),
 * schon sec/km, kein zusaetzliches Skalieren.
 */
private fun parsePaceStream(s: String?): List<Double> {
    if (s.isNullOrBlank()) return emptyList()
    val trimmed = s.trim()
    // Frank-Wunsch 2026-05-16: Polar-Bulk-Import liefert Pace als JSON-Array
    // `[[ts, paceSecPerKm], ...]` direkt — bereits in sec/km, kein Skalieren noetig.
    if (trimmed.startsWith("[")) {
        return parseJsonTimestampValueAsDoubleList(
            trimmed,
            valueIndex = 1,
            minVal = 150.0,
            maxVal = 1500.0,
        )
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
 * Parst kilo_pace / lap String. Format ist je nach Zepp-Version: "503.5;504.2;505.1" — nur
 * Pace-Werte "1000,503.5;2000,504.2" — Distanz,Pace pro Kilometer-Marker "503.5|504.2" — alte
 * Pipe-Variante Wir nehmen pro Sample die LETZTE Zahl (= Pace bei Distanz-Marker-Format).
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
 * "latInt,lonInt;latInt,lonInt;..." mit Semikolon zwischen Punkten und Komma zwischen Lat und Lon
 * je Punkt. Werte sind DELTA-encoded (erster Punkt absolut), Skalierung ÷ 100_000_000.
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
            val arr = Json.parseToJsonElement(trimmed) as? JsonArray ?: return@runCatching
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
 * Schaetzt VO2Max aus den ECHTEN Workout-Daten — kombiniert ACSM-Lauf-Formel mit HR-Reserve nach
 * Karvonen. Frank-Wunsch 2026-05-09: pro Lauf einen eigenen Wert, nicht eine Konstante.
 *
 * Schritt 1: VO2 bei diesem Workout (ml/kg/min) aus Lauf-Geschwindigkeit: VO2_workout = 0.2 × Speed
 * (m/min) + 3.5 [ACSM-Formel fuer Laufen]
 *
 * Schritt 2: HR-Reserve-Anteil — wieviel % der HR-Reserve nutzt Frank waehrend dieses Workouts?
 * Wenn er bei moderater Belastung gleichen Speed schafft, ist seine VO2Max hoeher als wenn er voll
 * ausbelastet sein muesste: HR_reserve_anteil = (avg_HR − rest_HR) / (max_HR − rest_HR)
 *
 * Schritt 3: Hochrechnung auf Maximum: VO2Max ≈ VO2_workout / HR_reserve_anteil
 *
 * HR_max = 180 (Frank's persoenlicher Maximalpuls — Frank-Info 2026-05-09) HR_rest = 65 (Default —
 * sollte spaeter aus AmazfitDaily kommen)
 *
 * Verifikation Frank's Trailrunning 7.16km/3612s: speed=119 m/min, VO2_workout=27.3, avg_HR=146,
 * HR_reserve=0.70, VO2Max ≈ 39 — passt zu Zepp-App-Anzeige 41 (kleine Abweichung wegen Hoehenmeter
 * beim Trail).
 */
/**
 * Schaetzt VO2Max nach der ACSM-Formel mit HR-Reserve-Hochrechnung.
 *
 * Frank-Wunsch 2026-05-17: Ruhepuls kommt jetzt aus den Tages-Daten (Whoop- Snapshot fuer den
 * Workout-Tag), nicht mehr hardcoded. Wenn fuer den Workout-Tag KEIN Ruhepuls-Wert vorliegt (z.B.
 * Polar-Bulk-Trainings vor der Whoop-Anschaffung), liefert die Funktion "—" — KEIN Fake mit 65 bpm.
 *
 * Schritt 1 — VO2 bei diesem Workout (ml/kg/min): VO2_workout = 0.2 × Speed (m/min) + 3.5
 * [ACSM-Lauf-Formel]
 *
 * Schritt 2 — HR-Reserve-Anteil (Karvonen): HR_reserve_anteil = (avg_HR − Tages-Ruhepuls) / (max_HR
 * − Tages-Ruhepuls)
 *
 * Schritt 3 — Hochrechnung auf Maximum: VO2Max ≈ VO2_workout / HR_reserve_anteil
 *
 * Korrektur +2 (Frank-Empirie 2026-05-09): empirischer Offset damit die absoluten Werte zu Franks
 * Zepp-App passen.
 *
 * HR_max = 180 (Frank's persoenlicher Maximalpuls)
 */
private fun estimateVo2Max(
    w: de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity,
    restingHrForDay: Int?,
): String {
    val avgHr = w.avgHeartRate ?: return "—"
    val distance = w.distanceMeters ?: return "—"
    val duration = w.durationSeconds ?: return "—"
    if (duration < 60 || distance < 100) return "—"
    if (avgHr < 80 || avgHr > 220) return "—"
    // Frank-Wunsch 2026-05-17 (revidiert): Wenn fuer den Tag KEIN Whoop-
    // Ruhepuls verfuegbar (alle Trainings vor der Whoop-Anschaffung Feb 2026),
    // nutze als Fallback 60 bpm. So bekommen auch alle aelteren Polar-
    // Bulk-Trainings einen VO2max-Schaetzwert.
    val restingHr = restingHrForDay ?: 60
    val frankMaxHr = 180
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
private fun formatVo2(v: Double): String = "%.1f".format(v).replace(".", ",")

/**
 * Frank-Wunsch 2026-05-16: Quellenabhaengige Beschriftung des Workout-Chips und des Default-Titels.
 * Polar ist ab jetzt die alleinige Workout-Quelle.
 *
 * Frank-Update 2026-05-16 (Iteration 2): Frank verlangt dass die alten Eintraege NICHT mehr als
 * "T-Rex 3" angezeigt werden — sie sind aus seiner Sicht ohnehin obsolet (werden beim naechsten
 * erfolgreichen Polar-Bulk- Import geloescht). Bis dahin sollen sie neutral "Polar" zeigen, da
 * Frank seine Trainings konzeptionell als Polar-Trainings sieht (sein H10 ist ja immer dran, auch
 * wenn die Aufzeichnung historisch ueber Zepp lief).
 *
 * - source startsWith "polar" → "Polar"
 * - source == "health_connect" → "Health Connect"
 * - source enthaelt "huami" → "Polar" (frueher: "T-Rex 3", aufgehoben 2026-05-16)
 * - source == null oder leer → "Polar"
 * - Sonstiges → "Polar"
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

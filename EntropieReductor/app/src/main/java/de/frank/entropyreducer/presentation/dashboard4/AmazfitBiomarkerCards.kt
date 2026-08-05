package de.frank.entropyreducer.presentation.dashboard4

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.presentation.amazfit.EditTrainingValuesDialog
import de.frank.entropyreducer.presentation.amazfit.ManualWorkoutOverrides
import de.frank.entropyreducer.presentation.amazfit.findRestingHrForWorkoutDay
import de.frank.entropyreducer.presentation.amazfit.sourceLabel
import de.frank.entropyreducer.presentation.amazfit.isVo2MaxSport
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.LocalCardBackgroundOverride
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Header-Trenner zwischen Whoop-Werten und Amazfit-T-Rex-3-Werten. Frank-Wunsch 2026-05-09:
 * deutliche Quellen-Trennung.
 */
@Composable
internal fun AmazfitSectionHeader() {
    val cosmos = LocalCosmos.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Box(
            modifier =
                Modifier.size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LocalCosmos.current.warn.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.MonitorHeart,
                contentDescription = null,
                tint = LocalCosmos.current.warn,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.size(10.dp))
        Column {
            Text(
                "Polar",
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
 * Sport-Card im Biomarker-Screen. Zeigt die letzten ~5 Trainings als kompakte Liste mit
 * Sportart-Symbol + Distanz + Zeit + Pace + Puls. Tap auf eine Zeile fuehrt zum Detail-Screen,
 * "Alle anzeigen" oeffnet die volle Trainings-Liste.
 */
/**
 * EIGENE Hero-Card fuer den letzten Lauf — Frank-Wunsch 2026-05-09: separate "Blase" oberhalb der
 * Trainings-Liste, nicht IN der Trainings-Card.
 */
@Composable
internal fun AmazfitLastTrainingHeroCard(
    workouts: List<AmazfitWorkoutEntity>,
    snapshots: List<BiomarkerSnapshotEntity>,
    onOpenDetail: (String) -> Unit,
    onSaveOverrides: (String, ManualWorkoutOverrides) -> Unit = { _, _ -> },
) {
    val latest = workouts.firstOrNull() ?: return
    // Performance (Tiefen-Debugging 2026-07-04, Almanach compose §10.5): O(n)-Suche ueber die
    // gesamte Whoop-Historie (inkl. 2 ZonedDateTime-Allokationen pro Snapshot) lief bei JEDER
    // Recomposition — memoiziert auf (snapshots, startMs). Ergebnis identisch.
    val restingHr = remember(snapshots, latest.startMs) {
        findRestingHrForWorkoutDay(snapshots, latest.startMs)
    }
    LetzterLaufHero(
        w = latest,
        restingHrForDay = restingHr,
        onOpenDetail = { onOpenDetail(latest.trackId) },
        onSaveOverrides = { overrides -> onSaveOverrides(latest.trackId, overrides) },
    )
}

@Composable
internal fun AmazfitTrainingsCard(
    workouts: List<AmazfitWorkoutEntity>,
    snapshots: List<BiomarkerSnapshotEntity>,
    onOpenAll: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val cosmos = LocalCosmos.current
    // Letzten ueberspringen — der ist in der separaten Hero-Card.
    val rest = workouts.drop(1).take(5)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LocalCosmos.current.warn.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsRun,
                        contentDescription = null,
                        tint = LocalCosmos.current.warn,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    text = "Frühere Trainings (Polar)",
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (workouts.isNotEmpty()) {
                    Text(
                        text = "Alle anzeigen ▸",
                        color = LocalCosmos.current.warn,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onOpenAll() },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (workouts.isEmpty()) {
                Text(
                    text =
                        "Noch keine Trainings — verbinde dich in den Einstellungen unter API mit deinem Polar-Konto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            } else if (rest.isEmpty()) {
                Text(
                    text = "Bisher nur das eine Training oben.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            } else {
                rest.forEachIndexed { i, w ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    // Performance (Tiefen-Debugging 2026-07-04): RestingHr-Suche memoiziert —
                    // lief vorher pro Row bei jeder Recomposition ueber die ganze Whoop-Historie.
                    val restingHr = remember(snapshots, w.startMs) {
                        findRestingHrForWorkoutDay(snapshots, w.startMs)
                    }
                    AmazfitWorkoutRow(
                        workout = w,
                        onClick = { onOpenDetail(w.trackId) },
                        restingHrForDay = restingHr,
                    )
                }
                val remaining = workouts.size - 1 - rest.size
                if (remaining > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "+ $remaining weitere Trainings",
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable { onOpenAll() }
                                .padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * Hero-Pattern für den letzten Lauf — Frank-Wunsch 2026-05-09: "schoenes Fenster mit allen Werten
 * in Patterns". Sportart-Icon + Datum + T-Rex-3-Badge oben, dann Distanz und Dauer GROSS, darunter
 * ein 2x2-Pattern mit Pace, Puls, Kalorien, Höhenmeter.
 */
@Composable
private fun LetzterLaufHero(
    w: AmazfitWorkoutEntity,
    restingHrForDay: Int?,
    onOpenDetail: () -> Unit,
    onSaveOverrides: (ManualWorkoutOverrides) -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = LocalCosmos.current.warn
    val heroShape = RoundedCornerShape(14.dp)
    // Frank-Wunsch 2026-05-17: Edit-Dialog auch in der Hero-Card (oben neben
    // dem Polar-Badge). Damit kann Frank das oberste Training direkt bearbeiten
    // ohne den Detail-Screen zu oeffnen.
    var editOpen by remember { mutableStateOf(false) }
    if (editOpen) {
        EditTrainingValuesDialog(
            workout = w,
            onDismiss = { editOpen = false },
            onSave = { overrides ->
                onSaveOverrides(overrides)
                editOpen = false
            },
        )
    }
    // Frank-Wunsch 2026-05-13: Heller Rahmen wie bei allen anderen Pattern (GlassCard-Border).
    // Frank-Wunsch 2026-05-18 (Folgeauftrag 2): Wenn Frank ueber die Farbpalette
    // im Training-Detail eine Farbe ausgewaehlt hat, soll diese den hellgelben
    // Standard-Hintergrund ersetzen. LocalCardBackgroundOverride wird im
    // BiomarkerScreen pro Karte gesetzt — null = Standard (hellgelb bleibt).
    val heroBackground = LocalCardBackgroundOverride.current ?: accent.copy(alpha = 0.10f)
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .clip(heroShape)
                .background(heroBackground)
                .border(BorderStroke(1.dp, cosmos.glassBorder), heroShape)
                .clickable { onOpenDetail() }
                .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(40.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = w.sportName ?: "Sport",
                            style = MaterialTheme.typography.titleMedium,
                            color = cosmos.textPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        // Wetter direkt neben dem Titel (Frank-Wunsch): Temperatur + Wetter-Icon.
                        weatherLabel(w)?.let { wLabel ->
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = wLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = cosmos.textSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Text(
                        text = formatStartLabel(w.startMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
                Box(
                    modifier =
                        Modifier.clip(RoundedCornerShape(6.dp))
                            .background(accent.copy(alpha = 0.22f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    // Frank-Entscheidung 2026-08-05: echte Quelle statt pauschal "Polar" — hier
                    // stand vorher auch auf Trainings "Polar", die von Oura oder Whoop stammten.
                    Text(
                        text = sourceLabel(w.source),
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MiniStat(
                    label = "Ø Pace",
                    value = w.avgPaceSecPerKm?.let { formatPace(it) } ?: "—",
                    modifier = Modifier.weight(1f),
                    onClick = { editOpen = true },
                )
                MiniStat(
                    label = "Ø Puls",
                    value = w.avgHeartRate?.let { "$it bpm" } ?: "—",
                    modifier = Modifier.weight(1f),
                    onClick = { editOpen = true },
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MiniStat(
                    label = "Kalorien",
                    value = w.calories?.let { "%.0f kcal".format(it) } ?: "—",
                    modifier = Modifier.weight(1f),
                    onClick = { editOpen = true },
                )
                MiniStat(
                    label = "VO₂Max",
                    value = formatHeroVo2Max(w, restingHrForDay),
                    modifier = Modifier.weight(1f),
                    onClick = { editOpen = true },
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
private fun MiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val cosmos = LocalCosmos.current
    // Frank-Wunsch 2026-05-17: Stift raus, stattdessen ist die ganze Mini-Stat-
    // Flaeche selbst klickbar (oeffnet Edit-Dialog).
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(cosmos.glassBorder.copy(alpha = 0.15f))
                .then(clickModifier)
                .padding(horizontal = 10.dp, vertical = 8.dp)
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
 * Eine Zeile in der Trainings-Liste. Sportart links, Datum/Zeit Mitte oben, Distanz und Pace
 * darunter, Pulswerte rechts.
 */
@Composable
internal fun AmazfitWorkoutRow(
    workout: AmazfitWorkoutEntity,
    onClick: () -> Unit,
    restingHrForDay: Int? = null,
) {
    val cosmos = LocalCosmos.current
    Row(
        modifier =
            Modifier.fillMaxWidth()
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
                // Wetter pro Training (Frank-Wunsch): stuendliche Temperatur + Wetter-Icon
                // am GPS-Ort/zur Trainingszeit — direkt neben den Trainings-Metriken.
                weatherLabel(workout)?.let { wLabel ->
                    Text(
                        text = "  ·  $wLabel",
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        // Frank-Wunsch 2026-05-09: in der Trainings-Liste statt Max-HR den
        // VO2Max-Wert anzeigen — das ist der aussagekraeftigere Trainings-
        // Indikator pro Lauf.
        //
        // Frank-Wunsch 2026-05-17: VO₂max-Zeile NUR bei Lauf-basierten Sportarten
        // anzeigen. Bei Crosstrainer/Rudern/Krafttraining/etc. komplett weglassen
        // damit Frank auf einen Blick sieht "das war kein Laufen".
        val showVo2 = isVo2MaxSport(workout.sportName)
        val vo2 = if (showVo2) formatHeroVo2Max(workout, restingHrForDay) else null
        Column(horizontalAlignment = Alignment.End) {
            workout.avgHeartRate?.let {
                Text(
                    text = "Ø $it bpm",
                    color = LocalCosmos.current.crit,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (vo2 != null) {
                Text(
                    text = "VO₂max $vo2",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

// Performance-Audit Loop 3 (2026-05-10): top-level Formatter (DateTimeFormatter
// ist thread-safe per java.time-API-Garantie). Vorher 2x Allokation pro
// formatStartLabel-Aufruf — bei 5 Workout-Zeilen × Recompositions = 10/s.
private val WORKOUT_TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val WORKOUT_DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE dd.MM. · HH:mm", Locale.GERMANY)

internal fun formatStartLabel(startMs: Long): String {
    val zone = ZoneId.systemDefault()
    val zdt = Instant.ofEpochMilli(startMs).atZone(zone)
    val today = java.time.LocalDate.now(zone)
    val date = zdt.toLocalDate()
    val time = zdt.format(WORKOUT_TIME_FMT)
    return when (date) {
        today -> "Heute · $time"
        today.minusDays(1) -> "Gestern · $time"
        today.minusDays(2) -> "Vorgestern · $time"
        else -> zdt.format(WORKOUT_DATE_FMT)
    }
}

internal fun formatDistance(meters: Double): String =
    if (meters >= 1000.0) "%.2f km".format(Locale.GERMANY, meters / 1000.0)
    else "${meters.toInt()} m"

internal fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/**
 * Wetter-Icon (Emoji) zur Wetterlage — Frank-Wunsch: ein Zeichen (Sonne/Wolke/…) pro Training.
 * Die Woerter kommen aus [de.frank.entropyreducer.data.repository.WeatherRepository.wmoToCondition].
 */
internal fun weatherEmoji(condition: String?): String =
    when (condition) {
        "sonnig" -> "☀️"
        "heiter" -> "🌤️"
        "bewölkt" -> "☁️"
        "wechselhaft" -> "⛅"
        "regnerisch" -> "🌧️"
        "schneeig" -> "🌨️"
        "neblig" -> "🌫️"
        "gewittrig" -> "⛈️"
        "windig" -> "💨"
        "schwül" -> "🥵"
        else -> "🌡️"
    }

/**
 * Kompaktes Wetter-Label "18°C ☀️" fuer die Trainings-Anzeige — null wenn fuer das Training
 * (noch) keine Temperatur recherchiert wurde (kein GPS / API-Fehler / Abruf ausstehend).
 */
internal fun weatherLabel(w: AmazfitWorkoutEntity): String? {
    val temp = w.weatherTempCelsius ?: return null
    return "$temp°C ${weatherEmoji(w.weatherCondition)}"
}

/**
 * VO2Max-Berechnung fuer Hero-MiniStat und Trainings-Liste — zentrale Formel damit Hero,
 * Detail-Screen und Liste denselben Wert zeigen. Reine ACSM-Formel + HR-Reserve + Frank's
 * empirische +2 Korrektur.
 */
/**
 * Frank-Wunsch 2026-05-17: Ruhepuls kommt jetzt aus den Tages-Daten (Whoop- Snapshot fuer den
 * Workout-Tag). Wenn fuer den Tag KEIN Ruhepuls-Wert vorliegt → "—" statt mit Hardcoded-65 zu
 * raten.
 */
internal fun formatHeroVo2Max(w: AmazfitWorkoutEntity, restingHrForDay: Int?): String {
    val v = computeVo2MaxOrNull(w, restingHrForDay) ?: return "—"
    return "%.1f".format(v).replace(".", ",")
}

/**
 * Frank-Wunsch 2026-05-18: Reine Double-Variante der VO2max-Berechnung — wird gebraucht damit der
 * Biomarker-VO2max-Mini-Pattern und der zugehoerige Detail- Screen die letzten Trainings als
 * Verlauf zeichnen koennen. Liefert den Wert in ml/(kg·min) oder null wenn das Training nicht
 * VO2max-faehig ist (Sportart nicht Laufen/Trail/Walk, fehlende Distanz/Dauer/Puls, oder Wert
 * ausserhalb physiologisch plausibler Spanne 20-80).
 *
 * Pflicht-Vorpruefung mit [isVo2MaxSport] — Krafttraining, Yoga, Crosstrainer etc. liefern keinen
 * sinnvollen Wert.
 */
internal fun computeVo2MaxOrNull(w: AmazfitWorkoutEntity, restingHrForDay: Int?): Double? {
    if (!isVo2MaxSport(w.sportName)) return null
    val avgHr = w.avgHeartRate ?: return null
    val distance = w.distanceMeters ?: return null
    val duration = w.durationSeconds ?: return null
    if (duration < 60 || distance < 100 || avgHr < 80 || avgHr > 220) return null
    val restingHr = restingHrForDay ?: 60
    val frankMaxHr = 180
    if (frankMaxHr <= restingHr) return null
    val durationMin = duration / 60.0
    val speedMperMin = distance / durationMin
    val vo2Workout = 0.2 * speedMperMin + 3.5
    val hrReserve = (avgHr - restingHr).toDouble() / (frankMaxHr - restingHr).toDouble()
    if (hrReserve <= 0.1) return null
    val vo2Max = vo2Workout / hrReserve + 2.0
    if (vo2Max < 20 || vo2Max > 80) return null
    return vo2Max
}

internal fun formatPace(secPerKm: Double): String {
    if (secPerKm <= 0) return ""
    val total = secPerKm.toInt()
    val m = total / 60
    val s = total % 60
    return "%d:%02d min/km".format(m, s)
}

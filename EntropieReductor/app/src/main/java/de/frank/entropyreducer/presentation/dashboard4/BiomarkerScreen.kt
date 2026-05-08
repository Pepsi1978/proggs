package de.frank.entropyreducer.presentation.dashboard4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.presentation.ThemeViewModel
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.components.StatusBar
import de.frank.entropyreducer.presentation.components.ThemeToggleIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import de.frank.entropyreducer.presentation.components.charts.HrvLineChart
import de.frank.entropyreducer.presentation.components.charts.InteractiveLineChart
import de.frank.entropyreducer.presentation.components.charts.RecoveryRing
import de.frank.entropyreducer.presentation.components.charts.SleepStagesBar
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.navigation.Routes
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Dashboard 4 — Biomarker. Spec §13.
 * Recovery-Ring + Schluesselwerte (HRV/RHR/Schlaf/Sleep-Performance) + HRV-Verlauf
 * + Schlafstadien gestern + Strain. Wenn Whoop nicht verbunden ist, zeigen wir
 * einen Empty-State mit Verweis auf Settings.
 */
@Composable
fun BiomarkerHostScreen(
    onOpenSettings: () -> Unit,
    onSwitchTab: (String) -> Unit,
    onOpenMetricDetail: (String) -> Unit = {},
    vm: BiomarkerViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val cosmos = LocalCosmos.current
    val themeVm: ThemeViewModel = hiltViewModel()
    val themeMode by themeVm.themeMode.collectAsState()

    CosmosScaffold(
        title = "Biomarker",
        actions = {
            ThemeToggleIcon(current = themeMode, onCycle = themeVm::cycle)
            IconButton(onClick = vm::refreshNow) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Whoop-Sync starten",
                    tint = cosmos.textPrimary,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Einstellungen",
                    tint = cosmos.textPrimary,
                )
            }
        },
        bottomBar = {
            CosmosBottomBar(
                currentTab = Routes.BIOMARKER,
                micState = MicState.IDLE,
                onTabSelected = onSwitchTab,
                onMicClick = { onSwitchTab(Routes.TASKS) },
            )
        },
    ) { padding ->
        // Frank-Wunsch 2026-05-09: alle Verlaufs-Charts zeigen nur die letzten 70 Tage.
        // Aeltere Daten bleiben in der DB erhalten und sind im Detail-Screen sichtbar
        // (state.history bleibt komplett, nur die Chart-Cards filtern hier).
        // Performance: Filter nur neu berechnen wenn sich state.history aendert —
        // nicht bei jedem unrelated state-Update (z.B. lastWhoopSyncMs).
        val historyLast70 = androidx.compose.runtime.remember(state.history) {
            val seventyDaysAgoMs = System.currentTimeMillis() - 70L * 24 * 60 * 60 * 1000
            state.history.filter { it.capturedAt >= seventyDaysAgoMs }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // Frank-Wunsch 2026-05-09: kleine Info-Zeile direkt unter dem
                // Header die zeigt wann zuletzt erfolgreich mit Whoop synchronisiert
                // wurde. Hilft Frank zu sehen ob die Daten frisch sind oder ob der
                // Sync klemmt — ohne in die Settings gehen zu muessen.
                Text(
                    text = "Zuletzt synchronisiert: ${formatRelativeSyncTime(state.lastWhoopSyncMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                StatusBar(
                    percent = state.statusBreakdown?.total ?: 0,
                    breakdown = state.statusBreakdown,
                )
            }
            item { DateSelectorBar(state, vm) }
            item { GesamterholungCard(state) }
            item { KeyValueGrid(state, onOpenMetricDetail) }

            // History-Charts — alle Whoop-Werte mit VOLLSTAENDIGER Historie und
            // interaktivem Chart (Y-Achse, X-Achse, Tap auf Punkt zeigt Wert).
            // Frank-Wunsch 2026-05-08: nicht mehr 30-Tage-Slice sondern alles,
            // klickbar für Detail-Screen.
            item {
                MetricHistoryCard(
                    title = "HRV-Verlauf",
                    accent = CosmosColors.AccentPrimary,
                    points = historyLast70.mapNotNull { snap ->
                        snap.hrvMs?.let { snap.capturedAt to it }
                    },
                    unit = "ms",
                    onClick = { onOpenMetricDetail(MetricKey.HRV) },
                )
            }
            item {
                MetricHistoryCard(
                    title = "Resting Heart Rate",
                    accent = CosmosColors.Critical,
                    points = historyLast70.mapNotNull { snap ->
                        snap.restingHeartRate?.toDouble()?.let { snap.capturedAt to it }
                    },
                    unit = "bpm",
                    onClick = { onOpenMetricDetail(MetricKey.RHR) },
                    lowerIsBetter = true,
                )
            }
            item {
                MetricHistoryCard(
                    title = "Schlaf-Performance",
                    accent = CosmosColors.Success,
                    points = historyLast70.mapNotNull { snap ->
                        snap.sleepPerformance?.toDouble()?.let { snap.capturedAt to it }
                    },
                    unit = "%",
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_PERF) },
                )
            }
            item {
                MetricHistoryCard(
                    title = "Schlafdauer",
                    accent = CosmosColors.AccentSecondary,
                    points = historyLast70.mapNotNull { snap ->
                        snap.sleepTotalMinutes?.toDouble()?.let { snap.capturedAt to it }
                    },
                    unit = "min",
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_TOTAL) },
                )
            }
            item {
                // Schlafstadien-Card ist klickbar — führt zur Liste aller Schlaf-Werte
                // im Detail-Screen. Frank-Wunsch 2026-05-08: "wenn ich auf Schlaf drücke
                // soll was passieren". Wir oeffnen das SLEEP_TOTAL Detail mit allen
                // Werten + 4 weitere Tap-Tipps für REM/Deep/Light/Awake-Detail-Screens.
                GlassCard(modifier = Modifier.fillMaxWidth().clickable { onOpenMetricDetail(MetricKey.SLEEP_TOTAL) }) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Schlafstadien gestern Nacht",
                                style = MaterialTheme.typography.titleMedium,
                                color = cosmos.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "Details ▸",
                                color = CosmosColors.AccentSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        SleepStagesBar(
                            remMinutes = (state.selectedSnapshot ?: state.latest)?.sleepRemMinutes,
                            deepMinutes = (state.selectedSnapshot ?: state.latest)?.sleepDeepMinutes,
                            lightMinutes = (state.selectedSnapshot ?: state.latest)?.sleepLightMinutes,
                            awakeMinutes = (state.selectedSnapshot ?: state.latest)?.sleepAwakeMinutes,
                        )
                        Spacer(Modifier.height(8.dp))
                        // Mini-Schnellzugriff auf 4 Sleep-Stage-Detail-Screens
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SleepStageChip("REM", CosmosColors.AccentSecondary) { onOpenMetricDetail(MetricKey.SLEEP_REM) }
                            SleepStageChip("Tief", CosmosColors.AccentPrimary) { onOpenMetricDetail(MetricKey.SLEEP_DEEP) }
                            SleepStageChip("Leicht", CosmosColors.Warning) { onOpenMetricDetail(MetricKey.SLEEP_LIGHT) }
                            SleepStageChip("Wach", CosmosColors.Critical) { onOpenMetricDetail(MetricKey.SLEEP_AWAKE) }
                        }
                    }
                }
            }
            item {
                MetricHistoryCard(
                    title = "Strain",
                    accent = CosmosColors.Warning,
                    points = historyLast70.mapNotNull { snap ->
                        snap.dayStrain?.let { snap.capturedAt to it }
                    },
                    unit = "",
                    onClick = { onOpenMetricDetail(MetricKey.STRAIN) },
                )
            }
            item {
                MetricHistoryCard(
                    title = "Tagesumsatz",
                    accent = CosmosColors.AccentPrimary,
                    points = historyLast70.mapNotNull { snap ->
                        // Frank-Wunsch 2026-05-09: Whoop liefert Tagesumsatz in
                        // Kilojoule, wir zeigen ihn aber in Kilokalorien an
                        // (1 kcal = 4.184 kJ). Umrechnung passiert nur an der UI,
                        // die DB-Daten bleiben in Whoop's Original-Einheit.
                        snap.dayKilojoules?.let { snap.capturedAt to (it / 4.184) }
                    },
                    unit = "kcal",
                    onClick = { onOpenMetricDetail(MetricKey.KILOJOULES) },
                )
            }
            // Phase 11 — neue Whoop-Felder (Frank-Wunsch 2026-05-08).
            item {
                MetricHistoryCard(
                    title = "Atemfrequenz",
                    accent = CosmosColors.AccentPrimary,
                    points = historyLast70.mapNotNull { snap ->
                        snap.respiratoryRate?.let { snap.capturedAt to it }
                    },
                    unit = "Atemzüge/min",
                    onClick = { onOpenMetricDetail(MetricKey.RESPIRATORY) },
                )
            }
            item {
                MetricHistoryCard(
                    title = "Schlafeffizienz",
                    accent = CosmosColors.Success,
                    points = historyLast70.mapNotNull { snap ->
                        snap.sleepEfficiencyPercent?.toDouble()?.let { snap.capturedAt to it }
                    },
                    unit = "%",
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_EFFICIENCY) },
                )
            }
            item {
                MetricHistoryCard(
                    title = "Schlafregelmäßigkeit",
                    accent = CosmosColors.Success,
                    points = historyLast70.mapNotNull { snap ->
                        snap.sleepConsistencyPercent?.toDouble()?.let { snap.capturedAt to it }
                    },
                    unit = "%",
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_CONSISTENCY) },
                )
            }
            item {
                MetricHistoryCard(
                    lowerIsBetter = true,
                    title = "Schlafdefizit",
                    accent = CosmosColors.Warning,
                    points = historyLast70.mapNotNull { snap ->
                        snap.sleepDebtMinutes?.toDouble()?.let { snap.capturedAt to it }
                    },
                    unit = "min",
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_DEBT) },
                )
            }
            item {
                MetricHistoryCard(
                    title = "Sauerstoffsättigung",
                    accent = CosmosColors.Success,
                    points = historyLast70.mapNotNull { snap ->
                        snap.spo2Percent?.let { snap.capturedAt to it }
                    },
                    unit = "%",
                    onClick = { onOpenMetricDetail(MetricKey.SPO2) },
                )
            }
            item {
                MetricHistoryCard(
                    title = "Hauttemperatur",
                    accent = CosmosColors.Warning,
                    points = historyLast70.mapNotNull { snap ->
                        snap.skinTempCelsius?.let { snap.capturedAt to it }
                    },
                    unit = "°C",
                    onClick = { onOpenMetricDetail(MetricKey.SKIN_TEMP) },
                    lowerIsBetter = true,
                )
            }
            item {
                MetricHistoryCard(
                    title = "Durchschnittliche Herzfrequenz",
                    accent = CosmosColors.Critical,
                    points = historyLast70.mapNotNull { snap ->
                        snap.averageHeartRate?.toDouble()?.let { snap.capturedAt to it }
                    },
                    unit = "bpm",
                    onClick = { onOpenMetricDetail(MetricKey.AVG_HR) },
                    lowerIsBetter = true,
                )
            }
            // Korrelations-Card: zeigt Pearson-Korrelation HRV ↔ Schlafdauer
            // über die volle Historie.
            item { CorrelationCard(state) }
            if (state.latest == null) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                "Noch keine Biomarker",
                                style = MaterialTheme.typography.titleMedium,
                                color = cosmos.textPrimary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Verbinde dein Whoop-Armband in den Einstellungen, um Recovery, " +
                                    "HRV und Schlafdaten in den Status einzubeziehen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = cosmos.textSecondary,
                            )
                        }
                    }
                }
            }
            state.message?.let { msg ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun KeyValueGrid(state: BiomarkerUiState, onOpenDetail: (String) -> Unit) {
    // Mini-Cards zeigen jetzt den AUSGEWAEHLTEN Tag (Frank-Wunsch 2026-05-08:
    // zwischen Heute / gestern / vorgestern wechseln). Fallback auf latest
    // wenn für den ausgewaehlten Tag kein Snapshot existiert.
    val latest = state.selectedSnapshot ?: state.latest
    val history = state.history30Days
    // 30-Tage-Mittel pro Metrik (ohne den heutigen Wert) — Basis für Trend-Pfeil + Delta.
    val avgHrv = history.mapNotNull { it.hrvMs }.takeIf { it.isNotEmpty() }?.average()
    val avgRhr = history.mapNotNull { it.restingHeartRate }.takeIf { it.isNotEmpty() }?.average()
    val avgSleep = history.mapNotNull { it.sleepTotalMinutes }.takeIf { it.isNotEmpty() }?.average()
    val avgSleepPerf = history.mapNotNull { it.sleepPerformance }.takeIf { it.isNotEmpty() }?.average()

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "HRV",
            value = latest?.hrvMs?.let { "${"%.0f".format(it)} ms" } ?: "—",
            delta = formatDelta(latest?.hrvMs, avgHrv, "ms"),
            deltaPositive = (latest?.hrvMs ?: 0.0) > (avgHrv ?: 0.0),
            footnote = "vs. 30-Tage-Mittel",
            onClick = { onOpenDetail(MetricKey.HRV) },
        )
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "Herzfrequenz",
            value = latest?.restingHeartRate?.let { "$it bpm" } ?: "—",
            delta = formatDelta(latest?.restingHeartRate?.toDouble(), avgRhr, "bpm"),
            // Bei RHR ist NIEDRIGER besser — Pfeil-Logik invertiert.
            deltaPositive = (latest?.restingHeartRate?.toDouble() ?: 0.0) < (avgRhr ?: 0.0),
            footnote = "vs. 30-Tage-Mittel",
            onClick = { onOpenDetail(MetricKey.RHR) },
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val sleepMin = latest?.sleepTotalMinutes ?: 0
        val sleepLabel = if (sleepMin == 0) {
            "—"
        } else {
            "${sleepMin / 60} h ${(sleepMin % 60).toString().padStart(2, '0')} min"
        }
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "Schlaf",
            value = sleepLabel,
            delta = formatDelta(latest?.sleepTotalMinutes?.toDouble(), avgSleep, "min", asMinutes = true),
            deltaPositive = (latest?.sleepTotalMinutes?.toDouble() ?: 0.0) > (avgSleep ?: 0.0),
            footnote = "vs. 30-Tage-Mittel",
            onClick = { onOpenDetail(MetricKey.SLEEP_TOTAL) },
        )
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "Performance",
            value = latest?.sleepPerformance?.let { "$it %" } ?: "—",
            delta = formatDelta(latest?.sleepPerformance?.toDouble(), avgSleepPerf, "%"),
            deltaPositive = (latest?.sleepPerformance?.toDouble() ?: 0.0) > (avgSleepPerf ?: 0.0),
            footnote = "vs. 30-Tage-Mittel",
            onClick = { onOpenDetail(MetricKey.SLEEP_PERF) },
        )
    }
}

/** Zentrale Konstanten für Metrik-IDs — werden in Routes + Detail-Screen genutzt. */
internal object MetricKey {
    const val HRV = "hrv"
    const val RHR = "rhr"
    const val SLEEP_PERF = "sleep_perf"
    const val SLEEP_TOTAL = "sleep_total"
    const val STRAIN = "strain"
    const val KILOJOULES = "kilojoules"
    const val SLEEP_REM = "sleep_rem"
    const val SLEEP_DEEP = "sleep_deep"
    const val SLEEP_LIGHT = "sleep_light"
    const val SLEEP_AWAKE = "sleep_awake"
    const val SLEEP_DISTURBANCES = "sleep_disturbances"
    const val RECOVERY = "recovery"
    // Phase 11 — neue Whoop-Felder (Frank-Wunsch 2026-05-08)
    const val RESPIRATORY = "respiratory"
    const val SLEEP_CONSISTENCY = "sleep_consistency"
    const val SLEEP_EFFICIENCY = "sleep_efficiency"
    const val SLEEP_NEED = "sleep_need"
    const val SLEEP_DEBT = "sleep_debt"
    const val SPO2 = "spo2"
    const val SKIN_TEMP = "skin_temp"
    const val AVG_HR = "avg_hr"
    const val MAX_HR = "max_hr"
}

/**
 * Formatiert die Differenz zwischen aktuellem Wert und 30-Tage-Mittel als
 * "+X" / "-X" inkl. Einheit. Bei null-Werten leerer String.
 */
private fun formatDelta(current: Double?, avg: Double?, unit: String, asMinutes: Boolean = false): String {
    if (current == null || avg == null) return ""
    val diff = current - avg
    val sign = if (diff >= 0) "+" else ""
    return if (asMinutes) {
        val minutes = kotlin.math.abs(diff).toInt()
        val h = minutes / 60
        val m = minutes % 60
        val signActual = if (diff >= 0) "+" else "-"
        if (h > 0) "$signActual${h} h ${m.toString().padStart(2, '0')} $unit"
        else "$signActual${m} $unit"
    } else {
        "$sign${"%.0f".format(diff)} $unit"
    }
}

@Composable
private fun MetricMiniCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    delta: String,
    deltaPositive: Boolean,
    footnote: String,
    onClick: (() -> Unit)? = null,
) {
    val cosmos = LocalCosmos.current
    val deltaColor = if (deltaPositive) CosmosColors.Success else CosmosColors.Critical
    val cardModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    GlassCard(modifier = cardModifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = cosmos.textSecondary)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = cosmos.textPrimary)
            if (delta.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(delta, color = deltaColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(2.dp))
            Text(footnote, color = cosmos.textSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Klein-Pille für Sleep-Stage-Schnellzugriff im Schlaf-Card. Tap navigiert zum
 * jeweiligen Stage-Detail-Screen mit allen Werten als Liste + Verlaufschart.
 */
@Composable
private fun SleepStageChip(label: String, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.18f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = accent,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Tag-Selektor-Bar (Frank-Wunsch 2026-05-08): Pfeil-links / Datum / Pfeil-rechts
 * + "Heute"-Button wenn der ausgewaehlte Tag nicht heute ist. Erlaubt Frank
 * zwischen Heute / gestern / vorgestern / beliebigem Tag zu wechseln.
 */
@Composable
private fun DateSelectorBar(state: BiomarkerUiState, vm: BiomarkerViewModel) {
    val cosmos = LocalCosmos.current
    val today = java.time.LocalDate.now()
    val selDate = state.selectedDate
    val label = when (selDate) {
        today -> "Heute"
        today.minusDays(1) -> "Gestern"
        today.minusDays(2) -> "Vorgestern"
        else -> selDate.format(
            java.time.format.DateTimeFormatter.ofPattern("EEE dd.MM.yyyy", java.util.Locale.GERMANY),
        )
    }
    val isToday = selDate == today
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.shiftDay(-1) }) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "Vortag",
                    tint = cosmos.textPrimary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    color = cosmos.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (state.selectedSnapshot == null && !isToday) {
                    Text(
                        text = "Kein Whoop-Datensatz an diesem Tag",
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            IconButton(
                onClick = { vm.shiftDay(1) },
                enabled = !isToday,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Folgetag",
                    tint = if (isToday) cosmos.textSecondary.copy(alpha = 0.4f) else cosmos.textPrimary,
                )
            }
            if (!isToday) {
                androidx.compose.material3.TextButton(onClick = vm::goToToday) {
                    Text("Heute", color = CosmosColors.AccentPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Generische History-Chart-Card mit interaktivem Linien-Chart (Y-Achse,
 * X-Achse, Tap-zu-Tooltip). Tap auf die ganze Card oeffnet den Detail-Screen
 * mit voller Zahlen-Liste (Frank-Wunsch 2026-05-08).
 */
@Composable
private fun MetricHistoryCard(
    title: String,
    accent: androidx.compose.ui.graphics.Color,
    points: List<Pair<Long, Double>>,
    unit: String,
    onClick: () -> Unit,
    /** True bei Metriken wo niedriger besser ist (RHR, Schlafdefizit, Avg-HR).
     *  Trendlinien-Farbe wird dann semantisch gefaerbt: fallend = gruen. */
    lowerIsBetter: Boolean = false,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Details ▸",
                    color = accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (points.isEmpty()) {
                Text(
                    text = "Noch keine Daten — sync dein Whoop-Armband.",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                InteractiveLineChart(
                    points = points,
                    accent = accent,
                    unit = unit,
                    lowerIsBetter = lowerIsBetter,
                )
            }
        }
    }
}

/**
 * Korrelations-Card: berechnet Pearson-Korrelation zwischen HRV und Schlafdauer
 * über die letzten 30 Tage. Frank-Wunsch (Soll-Bild 15/25): "zeigt ob mehr Schlaf
 * mit höherer HRV einhergeht".
 */
@Composable
private fun CorrelationCard(state: BiomarkerUiState) {
    val cosmos = LocalCosmos.current
    // Nutzt VOLLE Historie damit die Korrelation mehr Datenpunkte hat.
    val pairs = state.history.mapNotNull { snap ->
        val hrv = snap.hrvMs ?: return@mapNotNull null
        val sleep = snap.sleepTotalMinutes ?: return@mapNotNull null
        hrv to sleep.toDouble()
    }
    val r = if (pairs.size >= 3) pearson(pairs) else null
    val (label, color) = when {
        r == null -> "Nicht genug Daten" to cosmos.textSecondary
        r >= 0.5 -> "Starke positive Korrelation" to CosmosColors.Success
        r >= 0.2 -> "Schwache positive Korrelation" to CosmosColors.AccentPrimary
        r >= -0.2 -> "Keine klare Korrelation" to cosmos.textSecondary
        r >= -0.5 -> "Schwache negative Korrelation" to CosmosColors.Warning
        else -> "Starke negative Korrelation" to CosmosColors.Critical
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "HRV ↔ Schlafdauer",
                style = MaterialTheme.typography.titleMedium,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (r != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Pearson r = ${"%.2f".format(r)} (n=${pairs.size})",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Höhere Werte deuten an: mehr Schlaf -> höhere HRV. " +
                    "Negative Werte heißen: mehr Schlaf -> niedrigere HRV (selten).",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun pearson(pairs: List<Pair<Double, Double>>): Double {
    val n = pairs.size
    val sumX = pairs.sumOf { it.first }
    val sumY = pairs.sumOf { it.second }
    val sumXY = pairs.sumOf { it.first * it.second }
    val sumX2 = pairs.sumOf { it.first * it.first }
    val sumY2 = pairs.sumOf { it.second * it.second }
    val numerator = n * sumXY - sumX * sumY
    val denomSq = (n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY)
    if (denomSq <= 0) return 0.0
    return numerator / kotlin.math.sqrt(denomSq)
}

/**
 * Gesamterholung-Card im Soll-Design (Bild 15/25).
 * Layout: links Title + Status-Sub-Text + Erlaeuterung; rechts großer Recovery-Ring.
 */
@Composable
private fun GesamterholungCard(state: BiomarkerUiState) {
    val cosmos = LocalCosmos.current
    // Recovery vom AUSGEWAEHLTEN Tag, sonst latest.
    val score = (state.selectedSnapshot ?: state.latest)?.recoveryScore
    val statusLabel = when {
        score == null -> "Noch keine Daten"
        score >= 75 -> "Dein Körper ist im Hoch."
        score >= 50 -> "Dein Körper ist im Gleichgewicht."
        score >= 25 -> "Dein Körper braucht heute Schonung."
        else -> "Dein Körper ist erschoepft."
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Gesamterholung",
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Erholung basiert auf mehreren Biomarkern und Trends.",
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier.width(120.dp).height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                RecoveryRing(score = score)
            }
        }
    }
}

/**
 * Formatiert den letzten erfolgreichen Sync-Zeitstempel als kurze deutsche
 * Relativzeit. Frank will auf einen Blick sehen ob die Whoop-Daten frisch sind.
 *
 * 0L = noch nie gesynced. < 1 Min = "gerade eben". < 60 Min = "vor X Minuten".
 * Sonst absolute Zeit oder Datum, je nachdem ob heute oder frueher.
 */
private fun formatRelativeSyncTime(syncMs: Long): String {
    if (syncMs <= 0L) return "noch nie"
    val now = System.currentTimeMillis()
    val diffSec = (now - syncMs) / 1_000L
    return when {
        diffSec < 60 -> "gerade eben"
        diffSec < 3600 -> "vor ${diffSec / 60} Minuten"
        else -> {
            val syncInstant = java.time.Instant.ofEpochMilli(syncMs)
                .atZone(java.time.ZoneId.systemDefault())
            val nowInstant = java.time.Instant.ofEpochMilli(now)
                .atZone(java.time.ZoneId.systemDefault())
            val sameDay = syncInstant.toLocalDate() == nowInstant.toLocalDate()
            val timeFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            if (sameDay) {
                "heute ${syncInstant.format(timeFmt)}"
            } else {
                val dateFmt = java.time.format.DateTimeFormatter.ofPattern("d.M. HH:mm")
                syncInstant.format(dateFmt)
            }
        }
    }
}

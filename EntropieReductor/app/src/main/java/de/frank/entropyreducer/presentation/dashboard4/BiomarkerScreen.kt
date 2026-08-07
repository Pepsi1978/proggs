package de.frank.entropyreducer.presentation.dashboard4

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.IsolatedThemeToggleIcon
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.components.charts.InteractiveLineChart
import de.frank.entropyreducer.presentation.components.charts.RecoveryRing
import de.frank.entropyreducer.presentation.components.charts.SleepStageColors
import de.frank.entropyreducer.presentation.components.charts.SleepStagesBar
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.navigation.Routes
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

/**
 * Dashboard 4 — Biomarker. Spec §13. Recovery-Ring + Schluesselwerte
 * (HRV/RHR/Schlaf/Sleep-Performance) + HRV-Verlauf
 * + Schlafstadien gestern + Strain. Wenn Whoop nicht verbunden ist, zeigen wir einen Empty-State
 *   mit Verweis auf Settings.
 */
@Composable
fun BiomarkerHostScreen(
    onOpenSettings: () -> Unit,
    onSwitchTab: (String) -> Unit,
    onOpenMetricDetail: (String) -> Unit = {},
    onOpenTrainingDetail: (String) -> Unit = {},
    onOpenAllTrainings: () -> Unit = {},
    onOpenOuraDetail: (String) -> Unit = {},
    onOpenHealthConnectDetail: (String) -> Unit = {},
    onOpenSubArea: (parentTab: String, index: Int) -> Unit = { _, _ -> },
    showBottomBar: Boolean = true,
    vm: BiomarkerViewModel = hiltViewModel(),
) {
    // Frank-Wunsch 2026-06-19 (Sync-Etappe 1.1): Bei jedem Sichtbarwerden des Biomarker-Tabs die
    // teuren Fitness-APIs (Whoop/Oura/Health Connect/Kalender) aktualisieren — und NUR dann
    // (nicht bei jedem App-Start). ON_RESUME statt LaunchedEffect(Unit), weil der Tab via
    // saveState/restoreState wiederhergestellt wird (tabSwitch in AppNavGraph) — ein einmaliger
    // Effekt liefe nur beim allerersten Betreten. Der API-Sync ist im ForegroundSyncManager gegen
    // parallele Doppellaeufe abgesichert (apiMutex) und setzt zugleich den 8h-Timer neu.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        vm.onBiomarkerOpened()
    }
    // Performance-Audit E2 (2026-05-10): collectAsStateWithLifecycle statt
    // collectAsState — Flow-Subscription pausiert wenn die UI im Hintergrund ist
    // (Lifecycle < STARTED). Spart CPU/Akku bei Tab-Wechseln und Sperrbildschirm.
    val state by vm.state.collectAsStateWithLifecycle()
    val cardOrder by vm.cardOrder.collectAsStateWithLifecycle()
    // Frank-Wunsch 2026-05-22 Phase 2: einheitliche Mic-Aktion mit Rosé-Akzent.
    var micActionsOpen by remember { mutableStateOf(false) }
    val tasksVm: de.frank.entropyreducer.presentation.dashboard1.TasksViewModel = hiltViewModel()
    // Frank-Wunsch 2026-05-18: per Karte gewaehlte Hintergrundfarbe.
    val cardColorMap by vm.cardColors.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    // themeVm + themeMode wurden hier oben gelesen — bei jedem Theme-Toggle
    // rekomponierte der ganze Screen. Jetzt liest IsolatedThemeToggleIcon den
    // Flow SELBST → nur das Toggle-Icon rekomponiert.

    // Health-Connect-Daten (Frank-Wunsch 2026-05-10): separater State + Launcher
    // fuer den Permission-Dialog. Permission gilt fuer Weight + BodyFat +
    // LeanBodyMass — alle drei werden in einem Klick angefordert. Nach
    // erfolgreichem Grant lesen wir alle Werte neu.
    val weightState by vm.weight.collectAsStateWithLifecycle()
    val weightPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                androidx.health.connect.client.PermissionController
                    .createRequestPermissionResultContract()
        ) { _ ->
            vm.refreshWeight()
        }
    // Performance 2026-05-23: remember(...) gibt der Lambda eine stabile Identitaet
    // ueber Recompositions hinweg. Vorher wurde sie bei jeder Screen-Recomposition
    // neu allokiert und brach damit die Skippability von BiomarkerCardForId — alle
    // sichtbaren Karten recomponierten mit. Verhalten unveraendert (gleicher Aufruf).
    val onRequestWeightPermission: () -> Unit =
        remember(weightPermissionLauncher, vm) {
            {
                // Frank-Wunsch 2026-05-10 (dritte Iteration): ALLE Health-Connect-READ-
                // Permissions in einem Rutsch anfordern, damit zukuenftige Plugins ohne
                // erneuten Permission-Dialog auskommen. Die Liste kommt direkt aus dem
                // HealthConnectManager — eine zentrale Stelle, kein Inline-Boilerplate.
                weightPermissionLauncher.launch(vm.allHealthConnectPermissions())
            }
        }
    // Frank-Wunsch 2026-05-10: Tap auf eine Health-Connect-Mini-Karte triggert
    // einen Refresh des letzten Werts. Wenn Permission fehlt, soll stattdessen
    // der Permission-Dialog kommen — die Karten-Logik unten entscheidet je nach
    // weightState welcher der beiden Handler greift.
    val onRefreshHealthConnect: () -> Unit = remember(vm) { { vm.refreshWeight() } }
    // Performance 2026-05-23: stabile Identitaet statt Inline-Allokation pro Item
    // (vorher direkt im LazyGrid-Item erzeugt → brach Card-Skippability).
    val onSaveWorkoutOverrides:
        (String, de.frank.entropyreducer.presentation.amazfit.ManualWorkoutOverrides) -> Unit =
        remember(vm) { { trackId, overrides -> vm.applyWorkoutOverrides(trackId, overrides) } }

    CosmosScaffold(
        title = "Biomarker",
        showBottomBar = showBottomBar,
        actions = {
            IsolatedThemeToggleIcon()
            IconButton(onClick = vm::refreshNow) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Whoop-Sync starten",
                    tint = cosmos.textPrimary,
                )
            }
            // Frank-Wunsch 2026-05-10 (zweite Iteration): direkter Zugang zu
            // Health Connect aus dem Biomarker-Header — auch wenn alle Mini-
            // Karten "Tippen" zeigen (= Permissions noch nicht vollstaendig
            // erteilt). Frank kann hier in HC navigieren und einzelne Permissions
            // togglen / die fehlende 'Vergangene Daten'-Permission aktivieren.
            IconButton(onClick = vm::openHealthConnectPermissionsEditor) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Health Connect öffnen",
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
                onMicClick = { micActionsOpen = !micActionsOpen },
                onSubAreaSelected = onOpenSubArea,
            )
        },
        // Frank-Wunsch 2026-05-09 (analog zum Aufgaben-Screen, fuenfte Praezisierung):
        // Der "Zuletzt synchronisiert"-Zeitstempel soll genauso nah am Titel
        // "Biomarker" sitzen wie die Backup-Statuszeile am Titel "Entropie Reduktor".
        // compactHeader=true reduziert die Material-3-TopAppBar-Hoehe von 64dp
        // (Default) auf 44dp und damit die Luft unter dem Titel von ~18dp auf ~8dp.
        compactHeader = true,
    ) { padding ->
        // Performance-Audit E1 (2026-05-10): historyLast70-Berechnung jetzt im VM
        // (siehe buildChartData in BiomarkerViewModel). Chart-Cards lesen die Maps
        // aus state.chartData — keine lokale 70-Tage-Filter-Berechnung mehr.

        // Frank-Wunsch 2026-05-10: Drag & Drop fuer alle Daten-Karten.
        // Lokale Liste fuer sofortiges UI-Feedback waehrend des Ziehens. Wird bei jedem
        // Reorder upgedatet und gleichzeitig in den DataStore persistiert.
        // Frank-Vorgabe 2026-05-10: ausgeblendete Karten (HIDDEN_CARD_IDS) werden
        // hier ausgefiltert — sie tauchen nicht mehr im LazyGrid auf, auch wenn
        // sie noch in der persistierten Reihenfolge stehen. Damit verschwinden
        // OURA_ACTIVITY und OURA_SLEEP_DETAIL automatisch ueberall.
        var localOrder by
            remember(cardOrder) {
                mutableStateOf(cardOrder.filterNot { it in BiomarkerCardId.HIDDEN_CARD_IDS })
            }

        // 2-Spalten-Grid: Mini-Karten (HRV, Ruhepuls, Schlaf, Performance) belegen
        // je 1 Spalte, alle anderen die volle Breite. Frank-Wunsch 2026-05-10:
        // Mini-Karten sollen sich auch UNTEREINANDER tauschen lassen (z.B. HRV ↔ Performance).
        val lazyGridState = rememberLazyGridState()
        val reorderState =
            rememberReorderableLazyGridState(lazyGridState) { from, to ->
                // Reorder anhand der String-Keys, nicht der LazyGrid-Indizes — damit
                // Header-Items mit eigenen String-Keys (hdr_*) zwischen den verschiebbaren
                // Karten ignoriert werden koennen (die sind nicht in localOrder).
                val fromKey = from.key as? String
                val toKey = to.key as? String
                if (fromKey != null && toKey != null && fromKey != toKey) {
                    val fromIdx = localOrder.indexOf(fromKey)
                    val toIdx = localOrder.indexOf(toKey)
                    if (fromIdx >= 0 && toIdx >= 0) {
                        localOrder =
                            localOrder.toMutableList().apply { add(toIdx, removeAt(fromIdx)) }
                        vm.saveCardOrder(localOrder)
                    }
                }
            }

        // Frank-Wunsch 2026-05-22 Phase 2: Box-Wrapper damit MicCaptureActions
        // ueber dem Grid als Overlay schweben kann (Alignment.BottomCenter).
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(padding)
        ) {
            LazyVerticalGrid(
                state = lazyGridState,
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                // Frank-Wunsch 2026-05-09: top auf 0 damit der Sync-Zeitstempel direkt
                // an die jetzt kompakte TopAppBar anschliesst (~8dp natuerliche Luft
                // bleiben durch das vertikale Zentrieren des Titels in der TopAppBar).
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 0.dp,
                        bottom = 16.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item("hdr_sync", span = { GridItemSpan(2) }) {
                    // Frank-Wunsch 2026-05-09: kleine Info-Zeile direkt unter dem
                    // Header die zeigt wann zuletzt erfolgreich mit Whoop synchronisiert
                    // wurde. Hilft Frank zu sehen ob die Daten frisch sind oder ob der
                    // Sync klemmt — ohne in die Settings gehen zu muessen.
                    Text(
                        // Frank-Wunsch 2026-05-10: Header zeigt das ALTESTE der vier
                        // Sync-Zeitstempel — also den Zeitpunkt zu dem wirklich ALLE
                        // Quellen aktuell waren. Wenn eine Quelle noch nie gesynced
                        // wurde (0L), wird sie ignoriert; wenn alle 0 sind, "noch nie".
                        text =
                            "Zuletzt synchronisiert: ${formatRelativeSyncTime(
                        listOfNotNull(
                            state.lastWhoopSyncMs.takeIf { it > 0L },
                            state.lastOuraSyncMs.takeIf { it > 0L },
                            state.lastAmazfitSyncMs.takeIf { it > 0L },
                            state.lastHealthConnectSyncMs.takeIf { it > 0L },
                        ).minOrNull() ?: 0L
                    )}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cosmos.textSecondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // Frank-Wunsch 2026-05-18: "Zustand jetzt"-Balken nur im Aufgaben-
                // und Analyse-Tab sichtbar. Im Biomarker-Bereich entfernt damit
                // der Fokus auf die Biomarker selbst liegt.
                item("hdr_date", span = { GridItemSpan(2) }) { DateSelectorBar(state, vm) }

                // ============ VERSCHIEBBARE KARTEN (Frank-Wunsch 2026-05-10) ============
                // Drag & Drop fuer alle Daten-Karten — Reihenfolge wird im DataStore
                // (BiomarkerCardOrderRepository) persistiert. Neue Karten in spaeteren
                // App-Versionen werden automatisch ans Ende angehaengt. Frank kann mit
                // dem Drag-Handle-Symbol oben rechts auf jeder Karte die Reihenfolge frei
                // anpassen — Long-Press auf das Symbol startet das Verschieben.
                // Default-Reihenfolge bei Erstinstallation (BiomarkerCardId.DEFAULT_ORDER):
                //   HRV → Ruhepuls (Herzfrequenz)
                //   Atemfrequenz → SpO2 → Hauttemperatur → Hauttemperatur-Delta (Koerper)
                //   Schlaf-Performance → Schlafdauer → Schlafphasen → Erholsamer Schlaf
                //     → Schlafeffizienz → Schlafregelmaessigkeit → Schlafdefizit (Schlaf)
                //   Tagesumsatz → Belastung → Workouts (Aktivitaet)
                //   Korrelation HRV ↔ Schlafdauer (Analyse)
                //   Amazfit-Hero + Amazfit-Trainings (T-Rex 3)
                items(
                    items = localOrder,
                    key = { it },
                    // Performance-Audit (Frank-Wunsch 2026-05-18): contentType
                    // ermoeglicht Item-Recycling pro Typ. LazyVerticalGrid kann
                    // Mini-Cards untereinander und volle Cards untereinander
                    // wiederverwenden statt jedes Card-Layout neu zu inflaten.
                    // Bei 25+ Cards spuerbar weniger Ruckeln beim Scrollen.
                    contentType = { id ->
                        if (id in BiomarkerCardId.MINI_CARD_IDS) "mini" else "wide"
                    },
                    span = { id ->
                        if (id in BiomarkerCardId.MINI_CARD_IDS) GridItemSpan(1)
                        else GridItemSpan(2)
                    },
                ) { id ->
                    ReorderableItem(reorderState, key = id) { _ ->
                        // Frank-Wunsch 2026-05-10: KEIN sichtbares Drag-Handle mehr — die
                        // ganze Karte ist long-press-draggable. Tap auf die Karte oeffnet
                        // weiterhin den Detail-Screen (Compose unterscheidet Tap vs. Long-Press).
                        //
                        // Frank-Wunsch 2026-05-18: individuelle Hintergrundfarbe pro
                        // Karte via CompositionLocal — alle GlassCards in dieser
                        // Card lesen automatisch den Override.
                        val cardOverride =
                            de.frank.entropyreducer.presentation.components
                                .cardColorOverrideForIndex(
                                    de.frank.entropyreducer.data.repository.cardColorIndexForTheme(
                                        cardColorMap, id, cosmos.isDark
                                    ),
                                    cosmos.isDark,
                                )
                        androidx.compose.runtime.CompositionLocalProvider(
                            de.frank.entropyreducer.presentation.components
                                .LocalCardBackgroundOverride provides cardOverride
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().longPressDraggableHandle()) {
                                BiomarkerCardForId(
                                    id = id,
                                    state = state,
                                    onOpenMetricDetail = onOpenMetricDetail,
                                    onOpenTrainingDetail = onOpenTrainingDetail,
                                    onOpenAllTrainings = onOpenAllTrainings,
                                    onOpenOuraDetail = onOpenOuraDetail,
                                    weightState = weightState,
                                    onRequestWeightPermission = onRequestWeightPermission,
                                    onOpenHealthConnectDetail = onOpenHealthConnectDetail,
                                    onSaveWorkoutOverrides = onSaveWorkoutOverrides,
                                )
                            }
                        }
                    }
                }
                // ============ ENDE VERSCHIEBBARE KARTEN ============
                // Alter, fest-verdrahteter Block ist jetzt in BiomarkerCardForId verlagert.
                // Falls Build fehlschlaegt: Block weiter unten ist auskommentiert.
                /*
                // ============ Herzfrequenz-Block (LEGACY — siehe BiomarkerCardForId) ============
                item {
                    MetricHistoryCard(
                        title = "HRV-Verlauf",
                        accent = LocalCosmos.current.accent,
                        points = state.chartData.pointsLast70["hrv"] ?: emptyList(),
                        fullHistoryPoints = state.chartData.fullPoints["hrv"] ?: emptyList(),
                        unit = "ms",
                        onClick = { onOpenMetricDetail(MetricKey.HRV) },
                    )
                }
                item {
                    MetricHistoryCard(
                        title = "Ruhepuls",
                        accent = LocalCosmos.current.crit,
                        points = state.chartData.pointsLast70["rhr"] ?: emptyList(),
                        fullHistoryPoints = state.chartData.fullPoints["rhr"] ?: emptyList(),
                        unit = "bpm",
                        onClick = { onOpenMetricDetail(MetricKey.RHR) },
                        lowerIsBetter = true,
                    )
                }

                // ============ Körper-Block (Atmung, Sauerstoff, Hauttemperatur) ============
                item {
                    // Frank-Wunsch 2026-05-09: Atemfrequenz folgt der Whoop-Doktrin —
                    // niedrigere Atemfrequenz im Schlaf = entspannter = besser.
                    MetricHistoryCard(
                        title = "Atemfrequenz",
                        accent = LocalCosmos.current.accent,
                        points = state.chartData.pointsLast70["respiratory"] ?: emptyList(),
                        fullHistoryPoints = state.chartData.fullPoints["respiratory"] ?: emptyList(),
                        unit = "/min",
                        onClick = { onOpenMetricDetail(MetricKey.RESPIRATORY) },
                        lowerIsBetter = true,
                    )
                }
                item {
                    MetricHistoryCard(
                        title = "Sauerstoffsättigung",
                        accent = LocalCosmos.current.ok,
                        points = state.chartData.pointsLast70["spo2"] ?: emptyList(),
                        fullHistoryPoints = state.chartData.fullPoints["spo2"] ?: emptyList(),
                        unit = "%",
                        onClick = { onOpenMetricDetail(MetricKey.SPO2) },
                    )
                }
                item {
                    MetricHistoryCard(
                        title = "Hauttemperatur",
                        accent = LocalCosmos.current.warn,
                        points = state.chartData.pointsLast70["skin_temp"] ?: emptyList(),
                        fullHistoryPoints = state.chartData.fullPoints["skin_temp"] ?: emptyList(),
                        unit = "°C",
                        onClick = { onOpenMetricDetail(MetricKey.SKIN_TEMP) },
                        lowerIsBetter = true,
                    )
                }
                // Eigenberechnung — Hauttemperatur-Abweichung gegenueber 30-Tage-Baseline.
                item {
                    SkinTempDeltaCard(
                        currentValue = (state.selectedSnapshot ?: state.latest)?.skinTempCelsius,
                        delta = state.skinTempDelta,
                        onClick = { onOpenMetricDetail(MetricKey.SKIN_TEMP) },
                    )
                }

                // ============ Schlaf-Block (alles untereinander) ============
                item {
                    MetricHistoryCard(
                        title = "Schlaf-Performance",
                        accent = LocalCosmos.current.ok,
                        points = state.chartData.pointsLast70["sleep_perf"] ?: emptyList(),
                        fullHistoryPoints = state.chartData.fullPoints["sleep_perf"] ?: emptyList(),
                        unit = "%",
                        onClick = { onOpenMetricDetail(MetricKey.SLEEP_PERF) },
                    )
                }
                item {
                    // Schlafdauer in Stunden statt Minuten (Frank-Wunsch 2026-05-09).
                    MetricHistoryCard(
                        title = "Schlafdauer",
                        accent = LocalCosmos.current.accentForscher,
                        points = state.chartData.pointsLast70["sleep_total"] ?: emptyList(),
                        fullHistoryPoints = state.chartData.fullPoints["sleep_total"] ?: emptyList(),
                        unit = "min",
                        onClick = { onOpenMetricDetail(MetricKey.SLEEP_TOTAL) },
                        valueFormatter = SLEEP_HOUR_FORMAT,
                    )
                }
                item {
                    // Schlafphasen-Card mit Stage-Bar + 4 Stage-Chips.
                    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onOpenMetricDetail(MetricKey.SLEEP_TOTAL) }) {
                        Column {
                            Text(
                                text = "Schlafphasen",
                                style = MaterialTheme.typography.titleMedium,
                                color = cosmos.textPrimary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            SleepStagesBar(
                                remMinutes = (state.selectedSnapshot ?: state.latest)?.sleepRemMinutes,
                                deepMinutes = (state.selectedSnapshot ?: state.latest)?.sleepDeepMinutes,
                                lightMinutes = (state.selectedSnapshot ?: state.latest)?.sleepLightMinutes,
                                awakeMinutes = (state.selectedSnapshot ?: state.latest)?.sleepAwakeMinutes,
                            )
                            Spacer(Modifier.height(8.dp))
                            // Frank-Wunsch 2026-05-13: Reihenfolge + Farben 1:1 vom Bar uebernehmen:
                            // Tief → REM → Leicht → Wach.
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SleepStageChip("Tief", SleepStageColors.Deep) { onOpenMetricDetail(MetricKey.SLEEP_DEEP) }
                                SleepStageChip("REM", SleepStageColors.Rem) { onOpenMetricDetail(MetricKey.SLEEP_REM) }
                                SleepStageChip("Leicht", SleepStageColors.Light) { onOpenMetricDetail(MetricKey.SLEEP_LIGHT) }
                                SleepStageChip("Wach", SleepStageColors.Awake) { onOpenMetricDetail(MetricKey.SLEEP_AWAKE) }
                            }
                        }
                    }
                }
                // Eigenberechnung — Erholsamer Schlaf % aus REM + Tiefschlaf.
                item {
                    RestorativeSleepCard(
                        percent = state.restorativeSleepPercent,
                        avgPercent = state.history30Days.mapNotNull { snap ->
                            val total = snap.sleepTotalMinutes ?: return@mapNotNull null
                            val rem = snap.sleepRemMinutes ?: return@mapNotNull null
                            val deep = snap.sleepDeepMinutes ?: return@mapNotNull null
                            if (total > 0) (rem + deep).toDouble() / total * 100.0 else null
                        }.takeIf { it.isNotEmpty() }?.average(),
                        onClick = { onOpenMetricDetail(MetricKey.SLEEP_RESTORATIVE) },
                    )
                }
                item {
                    MetricHistoryCard(
                        title = "Schlafeffizienz",
                        accent = LocalCosmos.current.ok,
                        points = state.chartData.pointsLast70["sleep_efficiency"] ?: emptyList(),
                        fullHistoryPoints = state.chartData.fullPoints["sleep_efficiency"] ?: emptyList(),
                        unit = "%",
                        onClick = { onOpenMetricDetail(MetricKey.SLEEP_EFFICIENCY) },
                    )
                }
                item {
                    MetricHistoryCard(
                        title = "Schlafregelmäßigkeit",
                        accent = LocalCosmos.current.ok,
                        points = state.chartData.pointsLast70["sleep_consistency"] ?: emptyList(),
                        fullHistoryPoints = state.chartData.fullPoints["sleep_consistency"] ?: emptyList(),
                        unit = "%",
                        onClick = { onOpenMetricDetail(MetricKey.SLEEP_CONSISTENCY) },
                    )
                }
                item {
                    MetricHistoryCard(
                        lowerIsBetter = true,
                        title = "Schlafdefizit",
                        accent = LocalCosmos.current.warn,
                        points = state.chartData.pointsLast70["sleep_debt"] ?: emptyList(),
                        fullHistoryPoints = state.chartData.fullPoints["sleep_debt"] ?: emptyList(),
                        unit = "min",
                        onClick = { onOpenMetricDetail(MetricKey.SLEEP_DEBT) },
                    )
                }

                // ============ Aktivitaet-Block (Tagesumsatz, Belastung, Workouts) ============
                item {
                    // Tagesumsatz-Card schliesst den heutigen Tag aus weil der Wert sich
                    // ueber den Tag aufbaut. Andere Metriken (Schlaf, HRV) sind morgens
                    // schon final — die brauchen keinen Filter.
                    val todayStartMs = java.time.LocalDate.now()
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                    MetricHistoryCard(
                        title = "Tagesumsatz",
                        accent = LocalCosmos.current.accent,
                        points = historyLast70
                            .filter { it.capturedAt < todayStartMs }
                            .mapNotNull { snap ->
                                // Whoop liefert kJ, Anzeige in kcal (Faktor 4.184).
                                snap.dayKilojoules?.let { snap.capturedAt to (it / 4.184) }
                            },
                        fullHistoryPoints = state.history
                            .filter { it.capturedAt < todayStartMs }
                            .mapNotNull { snap ->
                                snap.dayKilojoules?.let { snap.capturedAt to (it / 4.184) }
                            },
                        unit = "kcal",
                        onClick = { onOpenMetricDetail(MetricKey.KILOJOULES) },
                    )
                }
                item {
                    // Belastung steht direkt UEBER der Workout-Card — Frank-Wunsch
                    // 2026-05-09: thematische Naehe.
                    MetricHistoryCard(
                        title = "Belastung",
                        accent = LocalCosmos.current.warn,
                        points = state.chartData.pointsLast70["strain"] ?: emptyList(),
                        fullHistoryPoints = state.chartData.fullPoints["strain"] ?: emptyList(),
                        unit = "",
                        onClick = { onOpenMetricDetail(MetricKey.STRAIN) },
                    )
                }
                // Frank-Wunsch 2026-05-11: WorkoutsForDayCard (Whoop-Workouts an diesem
                // Tag) ist im Biomarker-Screen redundant — die Trainings stehen schon im
                // Amazfit-Hero-Bereich. Komplett entfernt aus Default-Layout.
                // Korrelations-Card: zeigt Pearson-Korrelation HRV ↔ Schlafdauer
                // über die volle Historie.
                item { CorrelationCard(state) }
                // T-Rex-3-Daily-Cards (PAI, BioCharge, Hauttemperatur) ENTFERNT
                // 2026-05-09 (Frank-Befund): Diese Werte sind in der Zepp-Cloud-API
                // nicht zugaenglich — die Endpoint-Probes lieferten alle 404. Plus
                // Recherche bestaetigt: Hauttemperatur und Atemfrequenz sind nur
                // on-device-Sensorwerte, keine Cloud-Synchronisation.
                // Sport-Bereich — Frank-Wunsch 2026-05-09: HERO-Card fuer letzten
                // Lauf separat oberhalb der Trainings-Liste, nicht eingebettet.
                item {
                    AmazfitLastTrainingHeroCard(
                        workouts = state.amazfitWorkouts,
                        onOpenDetail = onOpenTrainingDetail,
                    )
                }
                item {
                    AmazfitTrainingsCard(
                        workouts = state.amazfitWorkouts,
                        onOpenAll = onOpenAllTrainings,
                        onOpenDetail = onOpenTrainingDetail,
                    )
                }
                */
                // ============ ENDE LEGACY-BLOCK ============

                if (state.latest == null) {
                    item("ft_empty", span = { GridItemSpan(2) }) {
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
                // Frank-Wunsch 2026-05-17: Permanenter Sync-Status-Footer ganz unten.
                // Zeigt "wird synchronisiert" mit Spinner solange isRefreshing, sonst
                // das letzte Sync-Ergebnis mit Datum/Uhrzeit + Anzahl pro Quelle.
                state.message?.let { msg ->
                    item("ft_msg", span = { GridItemSpan(2) }) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.isRefreshing) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.height(16.dp).width(16.dp),
                                        strokeWidth = 2.dp,
                                        color = LocalCosmos.current.accent,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                }
                                Text(
                                    msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        if (state.isRefreshing) cosmos.textPrimary
                                        else cosmos.textSecondary,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                item("ft_spacer", span = { GridItemSpan(2) }) { Spacer(Modifier.height(80.dp)) }
            }
            // BottomBar ist immer die orange Hauptleiste; Mic-Aktionen folgen derselben Farbe.
            val micAccent = LocalCosmos.current.accent
            de.frank.entropyreducer.presentation.components.MicCaptureActions(
                visible = micActionsOpen,
                accent = micAccent,
                onTextCommit = { text, source -> tasksVm.processCapturedText(text, source) },
                onClose = { micActionsOpen = false },
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
            )
        } // close Box
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
    val avgSleepPerf =
        history.mapNotNull { it.sleepPerformance }.takeIf { it.isNotEmpty() }?.average()

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "HRV",
            value = latest?.hrvMs?.let { "${"%.1f".format(it)} ms" } ?: "—",
            delta = formatDelta(latest?.hrvMs, avgHrv, "ms"),
            deltaPositive = (latest?.hrvMs ?: 0.0) > (avgHrv ?: 0.0),
            footnote = "vs. 30-Tage-Mittel",
            onClick = { onOpenDetail(MetricKey.HRV) },
        )
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "Ruhepuls",
            value = latest?.restingHeartRate?.let { "$it bpm" } ?: "—",
            delta = formatDelta(latest?.restingHeartRate?.toDouble(), avgRhr, "bpm"),
            // Bei Ruhepuls ist NIEDRIGER besser — Pfeil-Logik invertiert.
            deltaPositive = (latest?.restingHeartRate?.toDouble() ?: 0.0) < (avgRhr ?: 0.0),
            footnote = "vs. 30-Tage-Mittel",
            onClick = { onOpenDetail(MetricKey.RHR) },
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Frank-Wunsch 2026-05-16: reine Schlafzeit OHNE Wachzeit anzeigen
        // (= Tief + REM + Leicht). Whoop liefert sleepTotalMinutes als Time-in-Bed
        // inklusive Wachzeit — ziehen wir hier aktiv ab.
        val effectiveSleepMin = effectiveSleepMinutes(latest)
        val sleepLabel =
            if (effectiveSleepMin <= 0) {
                "—"
            } else {
                "${effectiveSleepMin / 60} h ${(effectiveSleepMin % 60).toString().padStart(2, '0')} min"
            }
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "Schlaf",
            value = sleepLabel,
            delta =
                formatDelta(
                    effectiveSleepMin.toDouble().takeIf { it > 0.0 },
                    avgSleep,
                    "min",
                    asMinutes = true,
                ),
            deltaPositive = (effectiveSleepMin.toDouble()) > (avgSleep ?: 0.0),
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
    const val MAX_HR = "max_hr"
    // Frank-Wunsch 2026-05-09 — Eigenberechnungen aus Whoop-Rohdaten:
    const val SLEEP_RESTORATIVE = "sleep_restorative"
    const val SKIN_TEMP_DELTA = "skin_temp_delta"
    const val SLEEP_CYCLES = "sleep_cycles"
    // Frank-Wunsch 2026-05-18: VO2max aus VO2-faehigen Workouts (Laufen,
    // Trail, Walk). Wert wird im BiomarkerViewModel berechnet und in
    // chartData["vo2max"] abgelegt.
    const val VO2MAX = "vo2max"
}

/**
 * Formatiert die Differenz zwischen aktuellem Wert und 30-Tage-Mittel als "+X" / "-X" inkl.
 * Einheit. Bei null-Werten leerer String.
 */
private fun formatDelta(
    current: Double?,
    avg: Double?,
    unit: String,
    asMinutes: Boolean = false,
): String {
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
    valueColor: androidx.compose.ui.graphics.Color? = null,
    /**
     * Optionaler Suffix der NEBEN dem Label steht (z.B. Datum des letzten Werts). Frank-Wunsch
     * 2026-05-10: "Gewicht 14.01." statt Datum unten in Footnote. Wird subtil in
     * textSecondary-Farbe und kleiner Schrift gerendert.
     */
    labelSuffix: String? = null,
) {
    val cosmos = LocalCosmos.current
    val deltaColor = if (deltaPositive) LocalCosmos.current.ok else LocalCosmos.current.crit
    val cardModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    GlassCard(modifier = cardModifier) {
        Column {
            if (labelSuffix.isNullOrBlank()) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = cosmos.textSecondary,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = cosmos.textSecondary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = labelSuffix,
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor ?: cosmos.textPrimary,
            )
            if (delta.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    delta,
                    color = deltaColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (footnote.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    footnote,
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/**
 * Klein-Pille für Sleep-Stage-Schnellzugriff im Schlaf-Card. Tap navigiert zum jeweiligen
 * Stage-Detail-Screen mit allen Werten als Liste + Verlaufschart.
 */
@Composable
private fun SleepStageChip(
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier.clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.18f))
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
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
 * + "Heute"-Button wenn der ausgewaehlte Tag nicht heute ist. Erlaubt Frank zwischen Heute /
 *   gestern / vorgestern / beliebigem Tag zu wechseln.
 */
@Composable
private fun DateSelectorBar(state: BiomarkerUiState, vm: BiomarkerViewModel) {
    val cosmos = LocalCosmos.current
    val today = java.time.LocalDate.now()
    val selDate = state.selectedDate
    val label =
        when (selDate) {
            today -> "Heute"
            today.minusDays(1) -> "Gestern"
            today.minusDays(2) -> "Vorgestern"
            // Performance-Audit Loop 2 (2026-05-10): Top-level Formatter (DATE_SELECTOR_FMT)
            // statt Allokation pro Recomposition.
            else -> selDate.format(DATE_SELECTOR_FMT)
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
            IconButton(onClick = { vm.shiftDay(1) }, enabled = !isToday) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Folgetag",
                    tint =
                        if (isToday) cosmos.textSecondary.copy(alpha = 0.4f) else cosmos.textPrimary,
                )
            }
            if (!isToday) {
                androidx.compose.material3.TextButton(onClick = vm::goToToday) {
                    Text(
                        "Heute",
                        color = LocalCosmos.current.accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * Generische History-Chart-Card mit interaktivem Linien-Chart (Y-Achse, X-Achse, Tap-zu-Tooltip).
 * Tap auf die ganze Card oeffnet den Detail-Screen mit voller Zahlen-Liste (Frank-Wunsch
 * 2026-05-08).
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun MetricHistoryCard(
    title: String,
    accent: androidx.compose.ui.graphics.Color,
    points: List<Pair<Long, Double>>,
    unit: String,
    onClick: () -> Unit,
    /**
     * True bei Metriken wo niedriger besser ist (RHR, Schlafdefizit, Avg-HR). Trendlinien-Farbe
     * wird dann semantisch gefaerbt: fallend = gruen.
     */
    lowerIsBetter: Boolean = false,
    /**
     * Frank-Wunsch 2026-05-09: bei Schlafdauer die Y-Achse + Tooltip + Header-Wert in Stunden statt
     * Minuten formatieren. Wenn null: Standard formatY + unit.
     */
    valueFormatter: ((Double) -> String)? = null,
    /**
     * Frank-Wunsch 2026-05-09: Durchschnitt soll ueber ALLE jemals gespeicherten Werte (volle
     * Historie seit 25.02.2026) berechnet werden, nicht nur die letzten 70 Tage die im Chart
     * sichtbar sind. Wenn null: fallback auf points.
     */
    fullHistoryPoints: List<Pair<Long, Double>>? = null,
) {
    val cosmos = LocalCosmos.current
    // Frank-Wunsch 2026-05-09: aktuellen Wert (letzter Datenpunkt) prominent oben
    // im Card-Header zwischen Titel und "Details ▸" anzeigen damit man den Tageswert
    // sofort sieht ohne den Chart antippen zu muessen.
    val latestValue = points.lastOrNull()?.second
    val latestLabel = latestValue?.let { v ->
        valueFormatter?.invoke(v)
            ?: (formatLatestForCard(v) + if (unit.isNotBlank()) " $unit" else "")
    }
    // Frank-Wunsch 2026-05-09: unter jedem Chart der Durchschnitt aller Werte + die
    // Abweichung des aktuellen Werts vom Durchschnitt. Plus = gruen, Minus = rot.
    // Frank-Praezisierung 2026-05-09: Durchschnitt MUSS ueber die volle Historie
    // berechnet werden — nicht nur ueber das Chart-Fenster.
    // Performance-Audit Loop 3 (2026-05-10): avg in remember(...) — vorher pro
    // Recomposition O(N) Scan ueber die volle Historie (bis 365 Datenpunkte).
    val avgSource = fullHistoryPoints ?: points
    val avg =
        remember(avgSource) { avgSource.map { it.second }.takeIf { it.isNotEmpty() }?.average() }
    val diff = if (latestValue != null && avg != null) latestValue - avg else null
    val avgLabel = avg?.let { v ->
        valueFormatter?.invoke(v)
            ?: (formatLatestForCard(v) + if (unit.isNotBlank()) " $unit" else "")
    }
    val diffLabel = diff?.let { d ->
        val sign = if (d >= 0) "+" else "−"
        val absValue = kotlin.math.abs(d)
        val formatted =
            valueFormatter?.invoke(absValue)
                ?: (formatLatestForCard(absValue) + if (unit.isNotBlank()) " $unit" else "")
        "$sign$formatted"
    }
    // Frank-Praezisierung 2026-05-09: Farbe respektiert lowerIsBetter — bei Ruhepuls,
    // Schlafdefizit, Atemfrequenz, Hauttemperatur ist ein NIEDRIGERER Wert besser
    // (also negative Abweichung = gruen). Bei HRV, Schlaf-Performance, SpO2 etc.
    // ist hoeher besser. Bei diff == 0 neutrale Farbe (kein Auf/Ab).
    val isImprovement: Boolean? =
        when {
            diff == null || diff == 0.0 -> null
            lowerIsBetter -> diff < 0
            else -> diff > 0
        }
    val diffColor =
        when (isImprovement) {
            null -> cosmos.textSecondary
            true -> LocalCosmos.current.ok
            false -> LocalCosmos.current.crit
        }
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (latestLabel != null) {
                    Text(
                        text = latestLabel,
                        color = accent,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (points.isEmpty()) {
                Text(
                    text = "Noch keine Daten — sync dein Whoop-Armband.",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                // Frank-Wunsch 2026-05-09 (Abend): Tap auf den Chart oeffnet die
                // Detail-Seite — kein Tooltip-Modus mehr, kein "Details ▸"-Hinweis
                // im Header noetig. Der ganze Graph ist clickable.
                InteractiveLineChart(
                    points = points,
                    accent = accent,
                    unit = unit,
                    lowerIsBetter = lowerIsBetter,
                    valueFormatter = valueFormatter,
                    onClick = onClick,
                )
                if (avgLabel != null && diffLabel != null) {
                    Spacer(Modifier.height(12.dp))
                    // Frank-Praezisierung 2026-05-09 (vierte Iteration):
                    // - Durchschnitt + Abweichung GARANTIERT nebeneinander (kein FlowRow-Umbruch)
                    // - EINHEITLICHE Hintergrundfarbe fuer alle Charts: AccentPrimary getoent
                    //   (gleiche Farbe wie sie bei HRV-Card schon ist)
                    // - EINHEITLICHE Schriftfarben fuer alle Charts: 'Durchschnitt:' und
                    //   der Wert in AccentPrimary; 'Abweichung:' in textSecondary; nur
                    //   der Diff-Wert ist farbig (gruen positiv / rot negativ)
                    // - Diff-Farbe respektiert lowerIsBetter pro Metrik (siehe oben)
                    // - 'Durchschnitt' linksbuendig, 'Abweichung' rechtsbuendig
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(LocalCosmos.current.accent.copy(alpha = 0.10f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Linke Haelfte — Durchschnitt linksbuendig
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Durchschnitt: ",
                                    color = LocalCosmos.current.accent,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = avgLabel,
                                    color = LocalCosmos.current.accent,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            // Rechte Haelfte — Abweichung rechtsbuendig
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Text(
                                    text = "Abweichung: ",
                                    color = cosmos.textSecondary,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = diffLabel,
                                    color = diffColor,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Format-Helper fuer den Header-Wert oben in der Card — kompakt, ohne Zwangsdezimalstelle. */
private fun formatLatestForCard(value: Double): String {
    val abs = kotlin.math.abs(value)
    return when {
        abs >= 100 -> "%.0f".format(value)
        abs >= 10 -> "%.1f".format(value)
        else -> "%.1f".format(value)
    }
}

/** Schlafstunden-Formatter — wandelt Minuten zu "8h 33min" Anzeige. */
private val SLEEP_HOUR_FORMAT: (Double) -> String = { mins ->
    val totalMin = mins.toInt().coerceAtLeast(0)
    val h = totalMin / 60
    val m = totalMin % 60
    "${h}h ${m.toString().padStart(2, '0')}m"
}

/**
 * Gesamterholung-Card im Soll-Design (Bild 15/25). Layout: links Title + Status-Sub-Text +
 * Erlaeuterung; rechts großer Recovery-Ring.
 */
@Composable
private fun GesamterholungCard(state: BiomarkerUiState, onOpenDetail: (String) -> Unit) {
    val cosmos = LocalCosmos.current
    // Recovery vom AUSGEWAEHLTEN Tag, sonst latest.
    val score = (state.selectedSnapshot ?: state.latest)?.recoveryScore
    val statusLabel =
        when {
            score == null -> "Noch keine Daten"
            score >= 75 -> "Dein Körper ist im Hoch."
            score >= 50 -> "Dein Körper ist im Gleichgewicht."
            score >= 25 -> "Dein Körper braucht heute Schonung."
            else -> "Dein Körper ist erschoepft."
        }
    // Frank-Wunsch 2026-05-10: Delta vs. 30-Tage-Mittel direkt unter dem Status,
    // damit Frank sofort sieht ob die heutige Erholung ueber oder unter dem
    // persoenlichen Schnitt liegt — gleicher Stil wie bei den Mini-Karten.
    // Performance-Audit Loop 3 (2026-05-10): mapNotNull+average in remember.
    val avgRecovery =
        remember(state.history30Days) {
            state.history30Days
                .mapNotNull { it.recoveryScore }
                .takeIf { it.isNotEmpty() }
                ?.average()
        }
    val deltaText = formatDelta(score?.toDouble(), avgRecovery, "")
    val deltaPositive = (score?.toDouble() ?: 0.0) > (avgRecovery ?: 0.0)
    val deltaColor = if (deltaPositive) LocalCosmos.current.ok else LocalCosmos.current.crit
    // Frank-Wunsch 2026-05-09 (Abend): Tap auf den Recovery-Ring oeffnet die
    // Recovery-Detail-Seite — wie alle anderen Charts im Biomarker-Bereich.
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onOpenDetail(MetricKey.RECOVERY) }) {
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
                if (deltaText.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = deltaText.trim(),
                        color = deltaColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    // Frank-Vorgabe 2026-05-10: hinter 'vs. 30-Tage-Mittel'
                    // den konkreten Mittelwert anzeigen — sonst weiss Frank nicht
                    // gegen welchen Vergleichswert das Delta gerechnet ist.
                    val avgLabel =
                        avgRecovery?.let { "vs. 30-Tage-Mittel: ${it.toInt()}" }
                            ?: "vs. 30-Tage-Mittel"
                    Text(
                        text = avgLabel,
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
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
 * Formatiert den letzten erfolgreichen Sync-Zeitstempel als kurze deutsche Relativzeit. Frank will
 * auf einen Blick sehen ob die Whoop-Daten frisch sind.
 *
 * 0L = noch nie gesynced. < 1 Min = "gerade eben". < 60 Min = "vor X Minuten". Sonst absolute Zeit
 * oder Datum, je nachdem ob heute oder frueher.
 */
/**
 * Erholsamer-Schlaf-Karte — Eigenberechnung aus REM + Tiefschlaf in % vom Gesamtschlaf. Whoop's
 * Restorative-Sleep-Feature ist nicht ueber die API abrufbar, aber die Formel liefert sehr nahe
 * Werte (Frank-Recherche 2026-05-09).
 *
 * Optisch: grosser Prozent-Wert, Sub-Zeile mit den beiden Komponenten REM/Tief, Begruendungstext
 * darunter. Tap fuehrt zum Detail-Screen mit Verlauf.
 */
@Composable
private fun RestorativeSleepCard(percent: Double?, avgPercent: Double?, onClick: () -> Unit) {
    // Frank-Wunsch 2026-05-10: 1:1 wie die anderen Mini-Karten (HRV, Schlaf,
    // Performance, Herzfrequenz) — gleiche MetricMiniCard mit Label + Wert +
    // Delta vs. 30-Tage-Mittel + Footnote. Hoeherer Restorative-Anteil ist
    // besser, daher deltaPositive bei percent > avgPercent.
    MetricMiniCard(
        modifier = Modifier.fillMaxWidth(),
        label = "Erholsamer Schlaf",
        value = percent?.let { "${"%.0f".format(it)} %" } ?: "—",
        delta = formatDelta(percent, avgPercent, "%"),
        deltaPositive = (percent ?: 0.0) > (avgPercent ?: 0.0),
        footnote = "vs. 30-Tage-Mittel",
        onClick = onClick,
    )
}

/**
 * Hauttemperatur-Delta-Karte — zeigt die Abweichung des aktuellen Wertes vom 30-Tage-Schnitt.
 * Grosse Werte oder rote Faerbung deuten auf Erkrankung, Stress, Zyklus-Effekt oder Aufenthalt in
 * warmer/kalter Umgebung hin.
 *
 * Wenn die Baseline noch nicht stabil (< 7 Werte): freundlicher Hinweis.
 */
@Composable
private fun SkinTempDeltaCard(currentValue: Double?, delta: Double?, onClick: () -> Unit) {
    // Frank-Wunsch 2026-05-11: gedreht. Die ABWEICHUNG vom 30-Tage-Mittel ist
    // jetzt der grosse, farbige Hauptwert (rot bei steigender Temperatur,
    // gruen bei fallender — niedriger ist besser bei Hauttemperatur). Die
    // absolute Temperatur ist nur noch klein als Zusatzzeile in der Footnote.
    val cosmos = LocalCosmos.current
    val sign =
        when {
            delta == null -> ""
            delta >= 0 -> "+"
            else -> "−"
        }
    val absDelta = delta?.let { kotlin.math.abs(it) } ?: 0.0
    val bigDeltaText = if (delta != null) "$sign${"%.2f".format(absDelta)} °C" else "—"
    val bigDeltaColor =
        when {
            delta == null -> cosmos.textPrimary
            delta < 0.0 -> LocalCosmos.current.ok
            delta > 0.0 -> LocalCosmos.current.crit
            else -> cosmos.textPrimary
        }
    // Frank-Wunsch 2026-05-11: "vs. 30-Tage-Mittel" gehoert auf eine eigene Zeile —
    // der Strich zwischen "aktuell" und "vs." sah aus als wuerde der Text gewaltsam
    // umbrechen.
    val tempSuffix =
        currentValue?.let { "${"%.2f".format(it)} °C aktuell\nvs. 30-Tage-Mittel" }
            ?: "vs. 30-Tage-Mittel"
    MetricMiniCard(
        modifier = Modifier.fillMaxWidth(),
        label = "Hauttemperatur",
        value = bigDeltaText,
        delta = "",
        deltaPositive = (delta ?: 0.0) < 0.0,
        footnote = tempSuffix,
        onClick = onClick,
        valueColor = bigDeltaColor,
    )
}

// Performance-Audit Loop 2 (2026-05-10): top-level DateTimeFormatter statt
// Allokation pro formatRelativeSyncTime-Aufruf. DateTimeFormatter ist thread-safe.
private val SYNC_TIME_FMT: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
private val SYNC_DATE_FMT: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("d.M. HH:mm")
private val DATE_SELECTOR_FMT: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("EEE dd.MM.yyyy", java.util.Locale.GERMANY)

private fun formatRelativeSyncTime(syncMs: Long): String {
    if (syncMs <= 0L) return "noch nie"
    val now = System.currentTimeMillis()
    val diffSec = (now - syncMs) / 1_000L
    return when {
        diffSec < 60 -> "gerade eben"
        diffSec < 3600 -> "vor ${diffSec / 60} Minuten"
        else -> {
            val syncInstant =
                java.time.Instant.ofEpochMilli(syncMs).atZone(java.time.ZoneId.systemDefault())
            val nowInstant =
                java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault())
            val sameDay = syncInstant.toLocalDate() == nowInstant.toLocalDate()
            if (sameDay) {
                "heute ${syncInstant.format(SYNC_TIME_FMT)}"
            } else {
                syncInstant.format(SYNC_DATE_FMT)
            }
        }
    }
}

/**
 * Verschiebbare Biomarker-Karte fuer eine gegebene Card-ID.
 *
 * Frank-Wunsch 2026-05-10: Drag & Drop fuer alle Daten-Karten. Diese Composable ist die Bruecke
 * zwischen der Card-ID (String aus [BiomarkerCardId]) und der konkreten UI. Nutzt die bereits
 * vorhandenen privaten Composables (MetricHistoryCard, GesamterholungCard, KeyValueGrid,
 * SkinTempDeltaCard, RestorativeSleepCard, CorrelationCard, AmazfitLastTrainingHeroCard,
 * AmazfitTrainingsCard, WorkoutsForDayCard) und entscheidet anhand der ID welche Card gerendert
 * wird.
 *
 * Wird umschlossen von einer Column damit Multi-Element-Cards (z.B. KeyValueGrid mit zwei Rows +
 * Spacer) korrekt vertikal anordnen — sonst wuerde der aeussere Box im LazyColumn-Item die Children
 * stapeln statt untereinander.
 *
 * Wenn eine ID nicht bekannt ist (Schutz vor Datenmuell aus dem DataStore): stille leere Box. Das
 * Repository filtert solche IDs eigentlich raus, aber Defense-in-Depth.
 */
@Composable
private fun BiomarkerCardForId(
    id: String,
    state: BiomarkerUiState,
    onOpenMetricDetail: (String) -> Unit,
    onOpenTrainingDetail: (String) -> Unit,
    onOpenAllTrainings: () -> Unit,
    onOpenOuraDetail: (String) -> Unit = {},
    weightState: WeightState = WeightState(),
    onRequestWeightPermission: () -> Unit = {},
    onOpenHealthConnectDetail: (String) -> Unit = {},
    onSaveWorkoutOverrides:
        (String, de.frank.entropyreducer.presentation.amazfit.ManualWorkoutOverrides) -> Unit =
        { _, _ ->
        },
) {
    val cosmos = LocalCosmos.current
    Column(modifier = Modifier.fillMaxWidth()) {
        when (id) {
            BiomarkerCardId.GESAMTERHOLUNG -> GesamterholungCard(state, onOpenMetricDetail)
            BiomarkerCardId.RECOVERY_GRAPH ->
                RecoveryGraphCard(
                    selectedSnapshot = state.selectedSnapshot ?: state.latest,
                    history = state.history30Days,
                    // Frank-Wunsch 2026-08-07: Tap oeffnet den Detail-Screen — 1:1 wie beim
                    // HRV-Verlauf. Vorher fehlte der onClick, dadurch passierte beim Tippen nichts.
                    onClick = { onOpenMetricDetail(MetricKey.RECOVERY) },
                )
            BiomarkerCardId.MINI_WEIGHT ->
                MiniWeightCard(
                    weight = weightState,
                    onRequestPermission = onRequestWeightPermission,
                    onClick = { onOpenHealthConnectDetail(HealthConnectMetricKey.WEIGHT) },
                )
            BiomarkerCardId.MINI_BODY_FAT ->
                MiniBodyFatCard(
                    weight = weightState,
                    onRequestPermission = onRequestWeightPermission,
                    onClick = { onOpenHealthConnectDetail(HealthConnectMetricKey.BODY_FAT) },
                )
            BiomarkerCardId.MINI_LEAN_BODY_MASS ->
                MiniLeanBodyMassCard(
                    weight = weightState,
                    onRequestPermission = onRequestWeightPermission,
                    onClick = { onOpenHealthConnectDetail(HealthConnectMetricKey.LEAN_BODY_MASS) },
                )
            BiomarkerCardId.MINI_BODY_WATER ->
                MiniBodyWaterCard(
                    weight = weightState,
                    onRequestPermission = onRequestWeightPermission,
                    onClick = { onOpenHealthConnectDetail(HealthConnectMetricKey.BODY_WATER) },
                )
            BiomarkerCardId.MINI_BONE_MASS ->
                MiniBoneMassCard(
                    weight = weightState,
                    onRequestPermission = onRequestWeightPermission,
                    onClick = { onOpenHealthConnectDetail(HealthConnectMetricKey.BONE_MASS) },
                )
            BiomarkerCardId.MINI_MUSCLE_MASS ->
                MiniMuscleMassCard(
                    weight = weightState,
                    onRequestPermission = onRequestWeightPermission,
                    onClick = { onOpenHealthConnectDetail(HealthConnectMetricKey.MUSCLE_MASS) },
                )

            // ============ Mini-Cards (Frank-Wunsch 2026-05-10) ============
            // Vier eigenstaendige Mini-Karten, die im 2-Spalten-Grid liegen und
            // unabhaengig voneinander verschoben werden koennen. Frueher waren sie
            // ein festes 2x2-Grid (KEY_VALUE_GRID).
            // Frank-Wunsch 2026-06-21: MINI_HRV ist jetzt eine volle Breite-Karte
            // im Erholungsverlauf-Pattern — nicht mehr halb so gross.
            BiomarkerCardId.MINI_HRV ->
                HrvGraphCard(
                    selectedSnapshot = state.selectedSnapshot ?: state.latest,
                    history = state.history,
                    onClick = { onOpenMetricDetail(MetricKey.HRV) },
                )
            BiomarkerCardId.MINI_RHR -> MiniRhrCard(state, onOpenMetricDetail)
            BiomarkerCardId.MINI_VO2MAX -> MiniVo2MaxCard(state, onOpenMetricDetail)
            // Frank-Wunsch 2026-08-07: Schlaf ist jetzt eine volle Breite-Karte im
            // Erholungsverlauf-Pattern (Balken-Graph, 30-Tage-Schnitt, Ampel) statt der
            // halben Mini-Karte. Tap oeffnet unveraendert den Schlafdauer-Detail-Screen.
            BiomarkerCardId.MINI_SLEEP_TOTAL ->
                SleepTotalGraphCard(
                    selectedSnapshot = state.selectedSnapshot ?: state.latest,
                    history = state.history,
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_TOTAL) },
                )
            // Frank-Wunsch 2026-08-07: Performance ist jetzt eine volle Breite-Karte im
            // Erholungsverlauf-Pattern (Balken-Graph, 30-Tage-Schnitt, Ampel) statt der
            // halben Mini-Karte. Tap oeffnet unveraendert den Performance-Detail-Screen.
            BiomarkerCardId.MINI_SLEEP_PERFORMANCE ->
                SleepPerformanceGraphCard(
                    selectedSnapshot = state.selectedSnapshot ?: state.latest,
                    history = state.history,
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_PERF) },
                )

            BiomarkerCardId.HRV ->
                MetricHistoryCard(
                    title = "HRV-Verlauf",
                    accent = LocalCosmos.current.accent,
                    points = state.chartData.pointsLast70["hrv"] ?: emptyList(),
                    fullHistoryPoints = state.chartData.fullPoints["hrv"] ?: emptyList(),
                    unit = "ms",
                    onClick = { onOpenMetricDetail(MetricKey.HRV) },
                )

            BiomarkerCardId.RHR ->
                MetricHistoryCard(
                    title = "Ruhepuls",
                    accent = LocalCosmos.current.crit,
                    points = state.chartData.pointsLast70["rhr"] ?: emptyList(),
                    fullHistoryPoints = state.chartData.fullPoints["rhr"] ?: emptyList(),
                    unit = "bpm",
                    onClick = { onOpenMetricDetail(MetricKey.RHR) },
                    lowerIsBetter = true,
                )

            BiomarkerCardId.RESPIRATORY ->
                MetricHistoryCard(
                    title = "Atemfrequenz",
                    accent = LocalCosmos.current.accent,
                    points = state.chartData.pointsLast70["respiratory"] ?: emptyList(),
                    fullHistoryPoints = state.chartData.fullPoints["respiratory"] ?: emptyList(),
                    unit = "/min",
                    onClick = { onOpenMetricDetail(MetricKey.RESPIRATORY) },
                    lowerIsBetter = true,
                )

            BiomarkerCardId.SPO2 ->
                MetricHistoryCard(
                    title = "Sauerstoffsättigung",
                    accent = LocalCosmos.current.ok,
                    points = state.chartData.pointsLast70["spo2"] ?: emptyList(),
                    fullHistoryPoints = state.chartData.fullPoints["spo2"] ?: emptyList(),
                    unit = "%",
                    onClick = { onOpenMetricDetail(MetricKey.SPO2) },
                )

            BiomarkerCardId.SKIN_TEMP ->
                MetricHistoryCard(
                    title = "Hauttemperatur",
                    accent = LocalCosmos.current.warn,
                    points = state.chartData.pointsLast70["skin_temp"] ?: emptyList(),
                    fullHistoryPoints = state.chartData.fullPoints["skin_temp"] ?: emptyList(),
                    unit = "°C",
                    onClick = { onOpenMetricDetail(MetricKey.SKIN_TEMP) },
                    lowerIsBetter = true,
                )

            BiomarkerCardId.SKIN_TEMP_DELTA ->
                SkinTempDeltaCard(
                    currentValue = (state.selectedSnapshot ?: state.latest)?.skinTempCelsius,
                    delta = state.skinTempDelta,
                    onClick = { onOpenMetricDetail(MetricKey.SKIN_TEMP) },
                )

            BiomarkerCardId.SLEEP_PERFORMANCE ->
                MetricHistoryCard(
                    title = "Schlaf-Performance",
                    accent = LocalCosmos.current.ok,
                    points = state.chartData.pointsLast70["sleep_perf"] ?: emptyList(),
                    fullHistoryPoints = state.chartData.fullPoints["sleep_perf"] ?: emptyList(),
                    unit = "%",
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_PERF) },
                )

            BiomarkerCardId.SLEEP_TOTAL ->
                MetricHistoryCard(
                    title = "Schlafdauer",
                    accent = LocalCosmos.current.accentForscher,
                    points = state.chartData.pointsLast70["sleep_total"] ?: emptyList(),
                    fullHistoryPoints = state.chartData.fullPoints["sleep_total"] ?: emptyList(),
                    unit = "min",
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_TOTAL) },
                    valueFormatter = SLEEP_HOUR_FORMAT,
                )

            BiomarkerCardId.SLEEP_STAGES -> {
                // Schlafphasen-Card mit Stage-Bar + 4 Stage-Chips.
                GlassCard(
                    modifier =
                        Modifier.fillMaxWidth().clickable {
                            onOpenMetricDetail(MetricKey.SLEEP_TOTAL)
                        }
                ) {
                    Column {
                        Text(
                            text = "Schlafphasen",
                            style = MaterialTheme.typography.titleMedium,
                            color = cosmos.textPrimary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        SleepStagesBar(
                            remMinutes = (state.selectedSnapshot ?: state.latest)?.sleepRemMinutes,
                            deepMinutes =
                                (state.selectedSnapshot ?: state.latest)?.sleepDeepMinutes,
                            lightMinutes =
                                (state.selectedSnapshot ?: state.latest)?.sleepLightMinutes,
                            awakeMinutes =
                                (state.selectedSnapshot ?: state.latest)?.sleepAwakeMinutes,
                        )
                        Spacer(Modifier.height(8.dp))
                        // Frank-Wunsch 2026-05-13: Reihenfolge + Farben 1:1 vom Bar uebernehmen:
                        // Tief → REM → Leicht → Wach.
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SleepStageChip("Tief", SleepStageColors.Deep) {
                                onOpenMetricDetail(MetricKey.SLEEP_DEEP)
                            }
                            SleepStageChip("REM", SleepStageColors.Rem) {
                                onOpenMetricDetail(MetricKey.SLEEP_REM)
                            }
                            SleepStageChip("Leicht", SleepStageColors.Light) {
                                onOpenMetricDetail(MetricKey.SLEEP_LIGHT)
                            }
                            SleepStageChip("Wach", SleepStageColors.Awake) {
                                onOpenMetricDetail(MetricKey.SLEEP_AWAKE)
                            }
                        }
                    }
                }
            }

            // Frank-Wunsch 2026-08-07: Tap auf die Verlaufs-Pattern oeffnet den jeweiligen
            // Detail-Screen — 1:1 wie beim HRV-Verlauf. Vorher fehlte ueberall der onClick,
            // dadurch passierte beim Tippen nichts.
            BiomarkerCardId.SLEEP_DEEP_GRAPH ->
                DeepSleepGraphCard(
                    selectedSnapshot = state.selectedSnapshot ?: state.latest,
                    history = state.history,
                    precomputed = state.deepSleepDerived,
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_DEEP) },
                )

            BiomarkerCardId.SLEEP_REM_GRAPH ->
                RemSleepGraphCard(
                    selectedSnapshot = state.selectedSnapshot ?: state.latest,
                    history = state.history,
                    precomputed = state.remSleepDerived,
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_REM) },
                )

            BiomarkerCardId.SLEEP_WAKE_GRAPH ->
                WakeTimeGraphCard(
                    selectedSnapshot = state.selectedSnapshot ?: state.latest,
                    history = state.history,
                    precomputed = state.wakeTimeDerived,
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_AWAKE) },
                )

            BiomarkerCardId.SLEEP_RESTORATIVE_GRAPH ->
                RestorativeSleepGraphCard(
                    selectedSnapshot = state.selectedSnapshot ?: state.latest,
                    history = state.history,
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_RESTORATIVE) },
                )

            BiomarkerCardId.SLEEP_RESTORATIVE ->
                RestorativeSleepCard(
                    // Performance-Audit E1 (2026-05-10): 30d-Avg jetzt im VM vorberechnet.
                    percent = state.restorativeSleepPercent,
                    avgPercent = state.chartData.restorativeSleepAvg30dPercent,
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_RESTORATIVE) },
                )

            BiomarkerCardId.SLEEP_EFFICIENCY ->
                MetricHistoryCard(
                    title = "Schlafeffizienz",
                    accent = LocalCosmos.current.ok,
                    points = state.chartData.pointsLast70["sleep_efficiency"] ?: emptyList(),
                    fullHistoryPoints =
                        state.chartData.fullPoints["sleep_efficiency"] ?: emptyList(),
                    unit = "%",
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_EFFICIENCY) },
                )

            BiomarkerCardId.SLEEP_CONSISTENCY ->
                MetricHistoryCard(
                    title = "Schlafregelmäßigkeit",
                    accent = LocalCosmos.current.ok,
                    points = state.chartData.pointsLast70["sleep_consistency"] ?: emptyList(),
                    fullHistoryPoints =
                        state.chartData.fullPoints["sleep_consistency"] ?: emptyList(),
                    unit = "%",
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_CONSISTENCY) },
                )

            BiomarkerCardId.SLEEP_DEBT ->
                MetricHistoryCard(
                    lowerIsBetter = true,
                    title = "Schlafdefizit",
                    accent = LocalCosmos.current.warn,
                    points = state.chartData.pointsLast70["sleep_debt"] ?: emptyList(),
                    fullHistoryPoints = state.chartData.fullPoints["sleep_debt"] ?: emptyList(),
                    unit = "min",
                    onClick = { onOpenMetricDetail(MetricKey.SLEEP_DEBT) },
                )

            BiomarkerCardId.KILOJOULES ->
                MetricHistoryCard(
                    // Performance-Audit E1 (2026-05-10): Today-Filter + kJ→kcal-Umrechnung
                    // jetzt im VM vorberechnet (buildChartData). Tagesstart wird einmal pro
                    // combine-Emission ermittelt — der lokale remember{} entfaellt damit.
                    title = "Tagesumsatz",
                    accent = LocalCosmos.current.accent,
                    points = state.chartData.pointsLast70["kilojoules"] ?: emptyList(),
                    fullHistoryPoints = state.chartData.fullPoints["kilojoules"] ?: emptyList(),
                    unit = "kcal",
                    onClick = { onOpenMetricDetail(MetricKey.KILOJOULES) },
                )

            BiomarkerCardId.STRAIN ->
                MetricHistoryCard(
                    title = "Belastung",
                    accent = LocalCosmos.current.warn,
                    points = state.chartData.pointsLast70["strain"] ?: emptyList(),
                    fullHistoryPoints = state.chartData.fullPoints["strain"] ?: emptyList(),
                    unit = "",
                    onClick = { onOpenMetricDetail(MetricKey.STRAIN) },
                )

            BiomarkerCardId.WORKOUTS_FOR_DAY ->
                WorkoutsForDayCard(workouts = state.workoutsForSelectedDay)

            // Frank-Wunsch 2026-06-21: HRV ↔ Schlafdauer-Korrelation entfernt.
            // Die ID wird via HIDDEN_CARD_IDS ausgefiltert, aber wir behalten
            // den Branch als stille No-Op fuer Backward-Compat.
            BiomarkerCardId.CORRELATION -> { }

            BiomarkerCardId.AMAZFIT_LAST_HERO ->
                AmazfitLastTrainingHeroCard(
                    workouts = state.amazfitWorkouts,
                    snapshots = state.history,
                    onOpenDetail = onOpenTrainingDetail,
                    onSaveOverrides = onSaveWorkoutOverrides,
                )

            BiomarkerCardId.AMAZFIT_TRAININGS ->
                AmazfitTrainingsCard(
                    workouts = state.amazfitWorkouts,
                    snapshots = state.history,
                    onOpenAll = onOpenAllTrainings,
                    onOpenDetail = onOpenTrainingDetail,
                )

            // Oura-Ring-Karten (Frank-Wunsch 2026-05-10, Etappe D mit Historie).
            BiomarkerCardId.OURA_READINESS ->
                OuraReadinessCard(
                    readiness = state.ouraReadinessForSelectedDay,
                    history = state.ouraReadinessHistory,
                    selectedDate = state.selectedDate,
                    onClick = { onOpenOuraDetail(OuraMetricKey.READINESS) },
                )

            BiomarkerCardId.OURA_SLEEP_SCORE ->
                OuraSleepScoreCard(
                    sleep = state.ouraSleepForSelectedDay,
                    history = state.ouraSleepHistory,
                    selectedDate = state.selectedDate,
                    onClick = { onOpenOuraDetail(OuraMetricKey.SLEEP_SCORE) },
                )

            BiomarkerCardId.OURA_ACTIVITY ->
                OuraActivityCard(
                    activity = state.ouraActivityForSelectedDay,
                    history = state.ouraActivityHistory,
                    onClick = { onOpenOuraDetail(OuraMetricKey.ACTIVITY) },
                )

            BiomarkerCardId.OURA_RESILIENCE ->
                OuraResilienceCard(
                    resilience = state.ouraResilienceForSelectedDay,
                    history = state.ouraResilienceHistory,
                    onClick = { onOpenOuraDetail(OuraMetricKey.RESILIENCE) },
                )

            // Sleep-Detail bleibt im when fuer Backward-Compat — ist aber NICHT
            // mehr in DEFAULT_ORDER (Frank vertraut nur Whoop fuer Schlafphasen).
            BiomarkerCardId.OURA_SLEEP_DETAIL ->
                OuraSleepDetailCard(
                    sleepDetails = state.ouraSleepDetailsForSelectedDay,
                    onClick = {},
                )

            else -> {
                // Unbekannte Card-ID — stille leere Box. Defense-in-Depth gegen
                // Datenmuell aus dem DataStore (Repository filtert eigentlich schon).
            }
        }
    }
}

/**
 * Mini-Karten (Frank-Wunsch 2026-05-10) — die drei kleinen 1-Spalten-Karten oben im
 * Biomarker-Screen mit Ruhepuls, Schlaf und Schlaf-Performance. Frueher waren sie als festes
 * 2x2-Grid (KeyValueGrid) zusammengebaut, jetzt sind es eigenstaendige verschiebbare Items im
 * LazyVerticalGrid. HRV ist seit 2026-06-21 eine volle Breite-Karte im Erholungsverlauf-Pattern
 * und gehoert daher nicht mehr hierher.
 *
 * Die Logik (Wert + 30-Tage-Mittel + Delta + Footnote) ist identisch zur bisherigen
 * KeyValueGrid-Implementierung — nur eben pro Karte einzeln.
 */
@Composable
private fun MiniRhrCard(state: BiomarkerUiState, onOpenDetail: (String) -> Unit) {
    val latest = state.selectedSnapshot ?: state.latest
    val avgRhr =
        remember(state.history30Days) {
            state.history30Days
                .mapNotNull { it.restingHeartRate }
                .takeIf { it.isNotEmpty() }
                ?.average()
        }
    MetricMiniCard(
        modifier = Modifier.fillMaxWidth(),
        label = "Ruhepuls",
        value = latest?.restingHeartRate?.let { "$it bpm" } ?: "—",
        delta = formatDelta(latest?.restingHeartRate?.toDouble(), avgRhr, "bpm"),
        // Bei Ruhepuls ist NIEDRIGER besser — Pfeil-Logik invertiert.
        deltaPositive = (latest?.restingHeartRate?.toDouble() ?: 0.0) < (avgRhr ?: 0.0),
        footnote = "vs. 30-Tage-Mittel",
        onClick = { onOpenDetail(MetricKey.RHR) },
    )
}

/**
 * Frank-Wunsch 2026-05-16: reine Netto-Schlafzeit (= Tief + REM + Leicht) statt Time-in-Bed inkl.
 * Wachzeit. Whoop liefert `sleepTotalMinutes` als Zeit im Bett — Wachzeit aktiv abziehen damit der
 * Schlafwert das ist was Frank erwartet.
 */
private fun effectiveSleepMinutes(snap: BiomarkerSnapshotEntity?): Int {
    if (snap == null) return 0
    val total = snap.sleepTotalMinutes ?: return 0
    val awake = snap.sleepAwakeMinutes ?: 0
    return (total - awake).coerceAtLeast(0)
}

/**
 * Frank-Wunsch 2026-05-18: VO2max-Mini-Karte. Zeigt den letzten verfuegbaren VO2max-Wert aus den
 * juengsten VO2-faehigen Workouts (Laufen/Trail/Walk), formatiert mit einer Nachkommastelle. Delta
 * ist die Abweichung vom 90-Tage-Mittel (Frank-Wunsch: ausnahmsweise 90 statt 30 Tage, weil
 * VO2max-Werte langsamer schwanken als HRV/RHR).
 *
 * Klick oeffnet die Detail-Ansicht (MetricKey.VO2MAX). Die Detail-Page nutzt den vorberechneten
 * Cache aus state.chartData.fullPoints["vo2max"].
 */
@Composable
private fun MiniVo2MaxCard(state: BiomarkerUiState, onOpenDetail: (String) -> Unit) {
    // Aggregate aus dem chartData-Cache lesen — keine eigene Berechnung pro
    // Recomposition. Die Liste ist nach Workout-Start aufsteigend sortiert,
    // also ist der letzte Eintrag der juengste VO2max-Wert.
    val full = state.chartData.fullPoints["vo2max"] ?: emptyList()
    val latest = full.lastOrNull()?.second
    val ninetyDaysAgoMs = remember { System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000 }
    val avg90 =
        remember(full) {
            full
                .filter { it.first >= ninetyDaysAgoMs }
                .map { it.second }
                .takeIf { it.isNotEmpty() }
                ?.average()
        }
    val deltaText =
        if (latest != null && avg90 != null) {
            val diff = latest - avg90
            val sign = if (diff >= 0) "+" else ""
            "${sign}${"%.1f".format(diff).replace('.', ',')}"
        } else {
            ""
        }
    MetricMiniCard(
        modifier = Modifier.fillMaxWidth(),
        label = "VO2max",
        value = latest?.let { "${"%.1f".format(it).replace('.', ',')}" } ?: "—",
        delta = deltaText,
        deltaPositive = (latest ?: 0.0) >= (avg90 ?: 0.0),
        footnote = "vs. 90-Tage-Mittel",
        onClick = { onOpenDetail(MetricKey.VO2MAX) },
    )
}

/**
 * Gewichts-Mini-Karte (Frank-Wunsch 2026-05-10) — liest aus Health Connect, das von der Zepp-App
 * mit den Daten der Amazfit Smart Scale befuellt wird.
 *
 * Drei Anzeigemodi:
 * 1. HC nicht verfuegbar -> Hinweis "Health Connect nicht installiert"
 * 2. Permission nicht erteilt -> Tap fordert Permission an
 * 3. Permission erteilt + Daten -> Wert + Delta + Footnote (Standard-Mini-Karte)
 *
 * Hauttemperatur-Logik analog: Bei Gewicht ist NIEDRIGER nicht zwingend besser, aber Frank moechte
 * typisch (z.B. Diaet-Phase) Trend nach unten als positiv sehen — daher deltaPositive = (latestKg <
 * avg30dKg).
 */
@Composable
private fun MiniWeightCard(
    weight: WeightState,
    onRequestPermission: () -> Unit,
    onClick: () -> Unit,
) {
    HealthConnectMiniCard(
        weight = weight,
        label = "Gewicht",
        latest = weight.latestKg,
        avg = weight.avg30dKg,
        history = weight.history30d,
        unit = "kg",
        valueFormatter = { "${"%.1f".format(it)} kg" },
        deltaPositive = (weight.latestKg ?: 0.0) < (weight.avg30dKg ?: 0.0),
        onRequestPermission = onRequestPermission,
        onClick = onClick,
    )
}

@Composable
private fun MiniBodyFatCard(
    weight: WeightState,
    onRequestPermission: () -> Unit,
    onClick: () -> Unit,
) {
    HealthConnectMiniCard(
        weight = weight,
        label = "Körperfett",
        latest = weight.latestBodyFatPercent,
        avg = weight.avg30dBodyFatPercent,
        history = weight.bodyFatHistory30d,
        unit = "%",
        valueFormatter = { "${"%.1f".format(it)} %" },
        deltaPositive = (weight.latestBodyFatPercent ?: 0.0) < (weight.avg30dBodyFatPercent ?: 0.0),
        onRequestPermission = onRequestPermission,
        onClick = onClick,
    )
}

@Composable
private fun MiniLeanBodyMassCard(
    weight: WeightState,
    onRequestPermission: () -> Unit,
    onClick: () -> Unit,
) {
    HealthConnectMiniCard(
        weight = weight,
        label = "Magermasse",
        latest = weight.latestLeanBodyMassKg,
        avg = weight.avg30dLeanBodyMassKg,
        history = weight.leanBodyMassHistory30d,
        unit = "kg",
        valueFormatter = { "${"%.1f".format(it)} kg" },
        deltaPositive = (weight.latestLeanBodyMassKg ?: 0.0) > (weight.avg30dLeanBodyMassKg ?: 0.0),
        onRequestPermission = onRequestPermission,
        onClick = onClick,
    )
}

@Composable
private fun MiniBodyWaterCard(
    weight: WeightState,
    onRequestPermission: () -> Unit,
    onClick: () -> Unit,
) {
    HealthConnectMiniCard(
        weight = weight,
        label = "Wasser",
        latest = weight.latestBodyWaterMassKg,
        avg = weight.avg30dBodyWaterMassKg,
        history = weight.bodyWaterMassHistory30d,
        unit = "kg",
        valueFormatter = { "${"%.1f".format(it)} kg" },
        // Bei Wasser ist hoeher = besser (Hydration). Frank-Vorgabe analog Magermasse.
        deltaPositive =
            (weight.latestBodyWaterMassKg ?: 0.0) > (weight.avg30dBodyWaterMassKg ?: 0.0),
        onRequestPermission = onRequestPermission,
        onClick = onClick,
    )
}

@Composable
private fun MiniBoneMassCard(
    weight: WeightState,
    onRequestPermission: () -> Unit,
    onClick: () -> Unit,
) {
    HealthConnectMiniCard(
        weight = weight,
        label = "Knochen",
        latest = weight.latestBoneMassKg,
        avg = weight.avg30dBoneMassKg,
        history = weight.boneMassHistory30d,
        unit = "kg",
        valueFormatter = { "${"%.1f".format(it)} kg" },
        // Bei Knochenmasse ist hoeher = besser (stabile Knochendichte).
        deltaPositive = (weight.latestBoneMassKg ?: 0.0) > (weight.avg30dBoneMassKg ?: 0.0),
        onRequestPermission = onRequestPermission,
        onClick = onClick,
    )
}

/**
 * Muskelmasse-Karte (Frank-Wunsch 2026-05-10): Health Connect hat keinen direkten
 * Muscle-Mass-Datentyp. Wir naehern an: Muskelmasse ≈ Magermasse - Knochenmasse. (Magermasse =
 * Muskeln + Wasser + Knochen + Organe → minus Knochen = Muskel + Wasser + Organe.) Wenn
 * Knochenmasse fehlt, fallback auf reine Magermasse.
 *
 * History-Berechnung erfolgt punktweise zwischen den Werten gleicher Zeitstempel.
 */
@Composable
private fun MiniMuscleMassCard(
    weight: WeightState,
    onRequestPermission: () -> Unit,
    onClick: () -> Unit,
) {
    val latest = computeMuscleMass(weight.latestLeanBodyMassKg, weight.latestBoneMassKg)
    val avg = computeMuscleMass(weight.avg30dLeanBodyMassKg, weight.avg30dBoneMassKg)
    val history = computeMuscleMassHistory(weight.leanBodyMassHistory30d, weight.boneMassHistory30d)
    HealthConnectMiniCard(
        weight = weight,
        label = "Muskel",
        latest = latest,
        avg = avg,
        history = history,
        unit = "kg",
        valueFormatter = { "${"%.1f".format(it)} kg" },
        deltaPositive = (latest ?: 0.0) > (avg ?: 0.0),
        onRequestPermission = onRequestPermission,
        onClick = onClick,
    )
}

private fun computeMuscleMass(leanKg: Double?, boneKg: Double?): Double? {
    if (leanKg == null) return null
    return if (boneKg != null) leanKg - boneKg else leanKg
}

private fun computeMuscleMassHistory(
    leanHistory: List<Pair<Long, Double>>,
    boneHistory: List<Pair<Long, Double>>,
): List<Pair<Long, Double>> {
    if (leanHistory.isEmpty()) return emptyList()
    // Zu jedem LeanBodyMass-Zeitstempel den naechsten Bone-Wert finden (gleicher
    // Tag oder fallback). Wenn kein Bone-Wert da ist, nutzen wir lean direkt.
    val boneByMs = boneHistory.associate { it.first to it.second }
    return leanHistory.map { (ts, lean) ->
        val bone =
            boneByMs[ts] ?: boneHistory.minByOrNull { kotlin.math.abs(it.first - ts) }?.second
        ts to (if (bone != null) lean - bone else lean)
    }
}

/**
 * Gemeinsame Implementierung der Health-Connect-Mini-Karten (Gewicht, Koerperfett, Magermasse,
 * Wasser, Knochen). Frank-Wunsch 2026-05-10 (zweite Iteration):
 * - Tap im "Wert vorhanden"-Zustand oeffnet Detail-Screen mit History (analog zu
 *   Readiness/Schlaf-Score)
 * - Footnote zeigt Datum des letzten Werts ("vom 14.01.2026 · vs. 30-Tage-Mittel")
 * - Bei fehlender Permission: Tap startet Permission-Dialog
 * - Bei keinen Daten: Tap oeffnet trotzdem den Detail-Screen, dort kann Refresh ausgeloest werden
 */
@Composable
private fun HealthConnectMiniCard(
    weight: WeightState,
    label: String,
    latest: Double?,
    avg: Double?,
    history: List<Pair<Long, Double>>,
    unit: String,
    valueFormatter: (Double) -> String,
    deltaPositive: Boolean,
    onRequestPermission: () -> Unit,
    onClick: () -> Unit,
) {
    when {
        !weight.healthConnectAvailable -> {
            MetricMiniCard(
                modifier = Modifier.fillMaxWidth(),
                label = label,
                value = "—",
                delta = "",
                deltaPositive = false,
                footnote = "Health Connect nicht verfuegbar",
            )
        }
        !weight.permissionGranted -> {
            MetricMiniCard(
                modifier = Modifier.fillMaxWidth(),
                label = label,
                value = "Tippen",
                delta = "",
                deltaPositive = false,
                footnote = "Erlaubnis erforderlich",
                onClick = onRequestPermission,
            )
        }
        weight.isLoading -> {
            MetricMiniCard(
                modifier = Modifier.fillMaxWidth(),
                label = label,
                value = "…",
                delta = "",
                deltaPositive = false,
                footnote = "lade aus Health Connect …",
            )
        }
        latest == null -> {
            MetricMiniCard(
                modifier = Modifier.fillMaxWidth(),
                label = label,
                value = "—",
                delta = "",
                deltaPositive = false,
                footnote = "Tippen für Details + Aktualisieren",
                onClick = onClick,
            )
        }
        else -> {
            val deltaText =
                if (avg != null) {
                    val diff = latest - avg
                    val sign = if (diff >= 0) "+" else "−"
                    "$sign${"%.1f".format(kotlin.math.abs(diff))} $unit"
                } else {
                    ""
                }
            // Frank-Wunsch 2026-05-10 (zweite Iteration): Datum NEBEN dem Label,
            // nicht in der Footnote. So sieht Frank auf einen Blick wie alt der
            // Wert ist ohne nach unten gucken zu muessen.
            val lastTs = history.maxByOrNull { it.first }?.first
            val labelSuffix = lastTs?.let { formatMiniDate(it) }
            MetricMiniCard(
                modifier = Modifier.fillMaxWidth(),
                label = label,
                value = valueFormatter(latest),
                delta = deltaText,
                deltaPositive = deltaPositive,
                footnote = "vs. 30-Tage-Mittel",
                onClick = onClick,
                labelSuffix = labelSuffix,
            )
        }
    }
}

// Performance-Audit Loop 9 (2026-05-10): ThreadLocal SimpleDateFormat statt
// Allokation pro Aufruf in jedem Mini-Card-Format.
private val MINI_DATE_FMT: ThreadLocal<java.text.SimpleDateFormat> = ThreadLocal.withInitial {
    java.text.SimpleDateFormat("dd.MM.", java.util.Locale.GERMAN)
}

private fun formatMiniDate(ms: Long): String = MINI_DATE_FMT.get()!!.format(java.util.Date(ms))

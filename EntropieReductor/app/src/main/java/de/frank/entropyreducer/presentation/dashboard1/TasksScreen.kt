package de.frank.entropyreducer.presentation.dashboard1

// Glance-Import entfernt 2026-05-11 — Widget ist jetzt klassischer
// AppWidgetProvider, Updates ueber WidgetUpdater.updateAll
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.remote.drive.SyncStatus
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.presentation.ThemeViewModel
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.EntropyCategoryPill
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.ThemeToggleIcon
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.priorityRampColor
import de.frank.entropyreducer.presentation.recurring.RecurringTemplatesViewModel
import de.frank.entropyreducer.presentation.recurring.TemplateAsTaskCard
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.color
import de.frank.entropyreducer.presentation.theme.label
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Dashboard 1 — Aufgaben (Spec §10, Referenzbild 11/21). */
@Composable
fun TasksScreen(
    onOpenSettings: () -> Unit,
    onSwitchTab: (String) -> Unit,
    currentTab: String,
    onOpenSubArea: (parentTab: String, index: Int) -> Unit = { _, _ -> },
    onOpenEntryDetail: (entryId: String) -> Unit = {},
    // Frank-Wunsch 2026-06-01: Klick auf eine Loop-Karte oeffnet den Loop-Detail-Screen.
    onOpenLoopDetail: (templateId: String) -> Unit = {},
    showBottomBar: Boolean = true,
    vm: TasksViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    val snackbar = remember { SnackbarHostState() }
    // Frank-Wunsch 2026-05-24: Der Loop-Bereich (wiederkehrende Aufgaben) ist jetzt ein
    // Akkordeon-Dropdown im Aufgaben-Reiter (zwischen SPAETER und ERLEDIGT) statt eines
    // eigenen Sub-Screens. Vorlagen + ihre Verknuepfung (Checkbox an = Aufgabe erscheint
    // im Reiter) kommen unveraendert aus dem RecurringTemplatesViewModel.
    val recurringVm: RecurringTemplatesViewModel = hiltViewModel()
    val recurringTemplates by recurringVm.templates.collectAsStateWithLifecycle()
    val kiTaskSuggestVm: KiTaskSuggestViewModel = hiltViewModel()
    val kiTaskSuggestions by kiTaskSuggestVm.suggestions.collectAsStateWithLifecycle()
    val kiTaskSuggestState by kiTaskSuggestVm.state.collectAsStateWithLifecycle()
    val kiTaskAcceptingId by kiTaskSuggestVm.acceptingId.collectAsStateWithLifecycle()
    val kiTaskError by kiTaskSuggestVm.error.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Lokaler State fuer den Bucket-Picker — speichert nur die Entry-ID, der
    // tatsaechliche Eintrag wird aus dem aktuellen State frisch nachgelesen damit
    // die Anzeige immer den neusten manualBucket/timeBucket-Stand zeigt.
    var bucketPickerEntryId by remember { mutableStateOf<String?>(null) }
    // Frank-Wunsch 2026-05-31: Welche Aufgabe nach einem Widget-Tap auf die Prio-Perle
    // ihren Schieberegler automatisch geoeffnet bekommt (null = keiner). Wird von der
    // Karte selbst wieder geleert, sobald sie den Regler aufgeklappt hat.
    var prioPickerEntryId by remember { mutableStateOf<String?>(null) }
    // Frank-Wunsch 2026-05-31: Gleiches fuer LOOP-Vorlagen — welche Vorlage nach einem
    // Widget-Tap auf ihre Prio- bzw. Tag-Perle den Schieberegler / das Tag-Menue
    // automatisch geoeffnet bekommt.
    var prioPickerTemplateId by remember { mutableStateOf<String?>(null) }
    var bucketPickerTemplateId by remember { mutableStateOf<String?>(null) }
    // LazyListState am Top damit beide LaunchedEffects (Bucket-Picker + Scroll)
    // darauf zugreifen koennen. Wird unten an die Haupt-LazyColumn uebergeben.
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Frank-Wunsch 2026-05-24: Aufgabenbloecke (Heute/Morgen/Freiblock/Spaeter/Loop/
    // Erledigt) sind jetzt aufklappbare Akkordeon-Dropdowns. Es ist immer nur EIN
    // Block offen — Klick auf einen Header oeffnet ihn und schliesst den vorher
    // offenen automatisch; ein erneuter Klick klappt ihn wieder zu. Standard: HEUTE.
    // Der Schluessel ist der Sektions-Name (bucket.name bzw. SECTION_LOOP/SECTION_ERLEDIGT).
    var expandedSection by rememberSaveable { mutableStateOf<String?>(TimeBucket.HEUTE.name) }

    // Frank-Wunsch 2026-05-22 (Sprint 6 Iteration): Mic-Tap oeffnet jetzt die
    // einheitliche MicCaptureActions ueber der BottomBar — zwei runde Buttons
    // "Schreiben" und "Aufnehmen" in Orange (1:1 wie der Entropie-Reiter).
    // Kein eckiges BottomSheet mehr. Die Permission wird erst beim Klick auf
    // "Aufnehmen" innerhalb der Actions geprueft.
    var micActionsOpen by remember { mutableStateOf(false) }

    // Frank-Wunsch 2026-05-31: Nach dem Einsprechen einer Aufgabe erscheint ein
    // Review-Fenster mit dem transkribierten Text. Hier wird das aktuelle
    // Transkript gehalten; null = kein Fenster offen.
    var reviewTranscript by remember { mutableStateOf<String?>(null) }

    // Frank-Wunsch 2026-05-11: Widget muss frisch werden wenn sich Aufgaben
    // aendern (Bucket umsortiert, Karte erledigt, neue Karte). Ohne diesen
    // Trigger wuerde die Liste bis zu 30 Min veraltet bleiben.
    //
    // Frank-Wunsch 2026-05-11 (dritte Iteration, Performance-Fix):
    //  - debounce von 600ms auf 1500ms erhoeht: bei schnellem Editieren oder
    //    Bucket-Umsortieren sammelt das mehr Updates und ruft updateAll nur
    //    einmal pro Burst. Glance.updateAll rendert ALLE Widget-Instanzen
    //    neu (laedt alle aktiven Eintraege aus Room, baut RemoteViews) — das
    //    ist teuer und ruckelt die App wenn das Widget installiert ist.
    //  - applicationContext statt LocalContext.current: Context ist langlebig,
    //    haengt nicht am Activity-Lifecycle und vermeidet potentielle Recompose-
    //    Trigger wenn LocalContext sich aendert.
    val appCtx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    @OptIn(FlowPreview::class)
    LaunchedEffect(Unit) {
        androidx.compose.runtime
            .snapshotFlow {
                // Stable signature: nur Bucket-Zuordnung + Reihenfolge der IDs.
                // Tags/Description-Aenderungen triggern kein Update — die sehen
                // im Widget eh nicht anders aus solange Layout stabil bleibt.
                state.entriesByBucket.entries.joinToString("|") { (bucket, list) ->
                    "$bucket=${list.joinToString(",") { it.id + ":" + it.manualBucket?.name.orEmpty() }}"
                }
            }
            .distinctUntilChanged()
            .debounce(1500)
            .collect {
                runCatching {
                    de.frank.entropyreducer.presentation.widget.WidgetUpdater.updateAll(appCtx)
                }
            }
    }

    // Frank-Wunsch 2026-05-11: Widget-Tap auf eine Aufgabe oder die KI/Manuell-
    // Pille schickt einen Deep-Link an WidgetDeepLinkBus. Wir reagieren hier:
    //  - ACTION_RESCHEDULE → BucketPickerSheet fuer die Task oeffnen
    //  - ACTION_FOCUS      → Tasks-Tab ist schon offen (NavGraph default); kein
    //    weiterer Schritt noetig — Frank scrollt zur Karte (LazyColumn-Scroll
    //    auf Karten-Ebene koennte spaeter ergaenzt werden).
    // Nach Verarbeitung wird der Bus geleert, damit ein Configuration-Change
    // (Theme-Wechsel) den Tap nicht ein zweites Mal triggert.
    val widgetDeepLink by
        de.frank.entropyreducer.presentation.widget.WidgetDeepLinkBus.events
            .collectAsStateWithLifecycle()
    // Bugfix 2026-05-11: Konsolidierter Widget-Tap-Handler. Vorher zwei
    // LaunchedEffects (BucketPicker + Scroll), die race-conditions hatten —
    // einer hat den Bus gecleart bevor der andere reagieren konnte. Jetzt
    // ein einziger Effect der ALLES macht: warten bis Tasks geladen sind,
    // scrollen, bucketPicker oeffnen, dann Bus clearen.
    //
    // Key (widgetDeepLink, state.entriesByBucket) feuert auch wenn die Tasks
    // erst spaeter nachladen — z.B. beim App-Start aus dem Widget heraus.
    LaunchedEffect(widgetDeepLink, state.entriesByBucket) {
        val link = widgetDeepLink ?: return@LaunchedEffect
        val isTaskAction =
            link.action == de.frank.entropyreducer.presentation.widget.WidgetIntents.ACTION_FOCUS ||
                link.action ==
                    de.frank.entropyreducer.presentation.widget.WidgetIntents.ACTION_RESCHEDULE ||
                link.action ==
                    de.frank.entropyreducer.presentation.widget.WidgetIntents.ACTION_SET_PRIORITY
        if (!isTaskAction) return@LaunchedEffect

        // Warten bis Tasks geladen sind, sonst kann computeTaskLocation
        // den Block nicht finden und der Scroll waere zu briefing.
        val tasksLoaded =
            state.entriesByBucket.values.any { it.isNotEmpty() } ||
                state.resolvedEntries.isNotEmpty()
        if (!tasksLoaded) return@LaunchedEffect

        // 1) Scroll zur Aufgabe (beide Aktionen — FOCUS und RESCHEDULE). Mit dem
        // Akkordeon (Frank-Wunsch 2026-05-24) muss der Block der Aufgabe zuerst
        // aufgeklappt werden, sonst ist die Karte nicht gerendert.
        val location = computeTaskLocation(state = state, taskId = link.taskId)
        if (location != null) {
            expandedSection = location.first
            runCatching { listState.animateScrollToItem(location.second, scrollOffset = -120) }
        }

        // 2) Bei RESCHEDULE zusaetzlich den Bucket-Picker oeffnen
        if (
            link.action ==
                de.frank.entropyreducer.presentation.widget.WidgetIntents.ACTION_RESCHEDULE
        ) {
            bucketPickerEntryId = link.taskId
        }

        // 2b) Bei SET_PRIORITY den Schieberegler genau dieser Aufgabe direkt aufklappen.
        // Die Karte (EntropyEntryCard) liest prioPickerEntryId und oeffnet ihren Slider,
        // sobald sie nach dem Scroll gerendert ist — und leert die Markierung wieder.
        if (
            link.action ==
                de.frank.entropyreducer.presentation.widget.WidgetIntents.ACTION_SET_PRIORITY
        ) {
            prioPickerEntryId = link.taskId
        }

        // 3) Bus clearen — Link wurde konsumiert
        de.frank.entropyreducer.presentation.widget.WidgetDeepLinkBus.clear()
    }

    // Frank-Wunsch 2026-05-31: Widget-Tap auf die Prio-/Tag-Perle einer LOOP-Vorlage.
    // Eigener Handler, weil Loop-Vorlagen keine normalen Aufgaben sind: Loop-Block
    // aufklappen und genau diese Vorlage markieren — ihre Karte oeffnet dann selbst
    // den Schieberegler (autoOpenPrioSlider) bzw. das Tag-Menue (autoOpenBucketMenu).
    LaunchedEffect(widgetDeepLink, recurringTemplates) {
        val link = widgetDeepLink ?: return@LaunchedEffect
        val isLoopAction =
            link.action ==
                de.frank.entropyreducer.presentation.widget.WidgetIntents
                    .ACTION_SET_LOOP_PRIORITY ||
                link.action ==
                    de.frank.entropyreducer.presentation.widget.WidgetIntents.ACTION_SET_LOOP_BUCKET
        if (!isLoopAction) return@LaunchedEffect
        // Warten bis die Vorlagen geladen sind (z.B. App-Start direkt aus dem Widget).
        if (recurringTemplates.none { it.id == link.taskId }) return@LaunchedEffect
        expandedSection = SECTION_LOOP
        when (link.action) {
            de.frank.entropyreducer.presentation.widget.WidgetIntents.ACTION_SET_LOOP_PRIORITY ->
                prioPickerTemplateId = link.taskId
            de.frank.entropyreducer.presentation.widget.WidgetIntents.ACTION_SET_LOOP_BUCKET ->
                bucketPickerTemplateId = link.taskId
        }
        de.frank.entropyreducer.presentation.widget.WidgetDeepLinkBus.clear()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            vm.dismissError()
        }
    }

    val themeVm: ThemeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val themeMode by themeVm.themeMode.collectAsStateWithLifecycle()

    // Frank-Wunsch 2026-05-24: Mit dem Akkordeon ist die Header-Position eindeutig.
    // Nach dem Aufklappen eines Ziel-Blocks sind alle anderen Bloecke zugeklappt
    // (= je 1 Header-Item). Der Header liegt damit bei: 1 (Briefing) + Position des
    // Buckets in ALL_TIME_BUCKETS. Erledigt liegt nach allen Buckets + Loop.
    fun bucketHeaderIndex(target: de.frank.entropyreducer.domain.model.TimeBucket): Int =
        1 + ALL_TIME_BUCKETS.indexOf(target)

    CosmosScaffold(
        title = "Aufgaben",
        titleEndContent = {},
        showBottomBar = showBottomBar,
        actions = {
            // Refresh-Button (Frank-Wunsch 2026-05-22): aktualisiert den
            // gesamten Aufgabenreiter — Rollover, Bucket-Balance, Auto-Archiv
            // und neue Bewertung aller offenen Aufgaben mit aktuellen
            // Nachtraegen + Zeitanpassungen.
            IconButton(
                onClick = {
                    vm.refreshAll()
                    scope.launch { snackbar.showSnackbar("Aufgaben werden aktualisiert …") }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Aufgaben aktualisieren",
                    tint = cosmos.textPrimary,
                )
            }
            ThemeToggleIcon(current = themeMode, onCycle = themeVm::cycle)
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
                currentTab = currentTab,
                micState = state.micState,
                onTabSelected = onSwitchTab,
                onMicClick = { micActionsOpen = !micActionsOpen },
                onSubAreaSelected = onOpenSubArea,
            )
        },
        // Frank-Wunsch 2026-05-09 (vierte Praezisierung): Backup-Statuszeile
        // soll direkt unter dem Titel "Entropie Reduktor" sitzen ohne unsichtbaren
        // Spacer dazwischen. Die ~18dp "unsichtbare Luft" unter dem vertikal
        // zentrierten Titel kamen aus der Material-3-TopAppBar (64dp Default-
        // Hoehe). compactHeader=true reduziert die Hoehe auf 44dp -> Luft
        // schrumpft auf ~8dp. Andere Screens bleiben unangetastet.
        compactHeader = true,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Backup-Statuszeile DIREKT unter dem Titel "Entropie Reduktor"
                // — Frank-Wunsch 2026-05-09 (praezisiert): kommt ueber die
                // StatusBar, ist die allererste Zeile unter dem Titel. Funktionen
                // identisch (Cloud-Icon + Status-Label). Wird nur angezeigt
                // wenn Drive-Backup aktiviert ist.
                if (state.driveBackupEnabled) {
                    // Frank-Wunsch 2026-05-09 (dritte Praezisierung): Zeile MUSS
                    // direkt am Boden des Titels kleben — kein Spacer davor, kein
                    // Spacer danach, vertikales Padding in der Row selbst auf 0
                    // reduziert (siehe BackupStatusBadge).
                    BackupStatusBadge(state.syncStatus, state.lastBackupAtMs)
                }
                // Frank-Wunsch 2026-05-23: StatusBar ("Zustand jetzt"-Balken) ist
                // ausschliesslich im Analyse-Tab. Hier raus, damit der sichtbare
                // Bereich fuer Aufgaben nach oben wandert.
                Spacer(Modifier.height(8.dp))

                // Re-Score-Banner: laeuft eine Re-Bewertung aller offenen Aufgaben
                // mit der aktuellen priorityScore-Doktrin? Frank-Wunsch 2026-05-09:
                // beim ersten Start nach Doktrin-Update sollen die Aufgaben EINMAL
                // automatisch neu bewertet werden, damit die farbige Prio-Zahl
                // auch bei alten Eintraegen die richtige Farbe trifft. Der Banner
                // zeigt Fortschritt (X von Y) — kein Spinner-Modal, damit Frank
                // weiter mit der App arbeiten kann waehrend es laeuft.
                state.rescoreProgress?.let { rp ->
                    RescoreBanner(rp)
                    Spacer(Modifier.height(8.dp))
                }

                state.processingMessage?.let {
                    GlassCard(
                        modifier =
                            Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cosmos.textPrimary,
                        )
                    }
                }

                // PERFORMANCE 2026-05-09: derivedStateOf cached den all-empty Check —
                // wird sonst bei jedem State-Update neu berechnet.
                val isEmpty by
                    remember(state.entriesByBucket, state.resolvedEntries) {
                        derivedStateOf {
                            state.entriesByBucket.values.all { it.isEmpty() } &&
                                state.resolvedEntries.isEmpty()
                        }
                    }
                // Frank-Wunsch 2026-05-11: Scroll zur Widget-getappten Aufgabe
                // wird jetzt im konsolidierten LaunchedEffect oben gehandhabt
                // (listState ist am Top der TasksScreen-Funktion deklariert).

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "briefing", contentType = "briefing") {
                        // Frank-Wunsch 2026-05-23: BriefingPanel enthaelt jetzt
                        // auch die KI-Frage des Moments als zweites Dropdown —
                        // keine separate KiQuestionCard mehr in der LazyColumn.
                        de.frank.entropyreducer.presentation.briefing.BriefingPanel()
                    }
                    // Frank-Wunsch 2026-05-23 (Folge-Iteration): Kategorie-
                    // Filterleiste ("Koerperlich/Mental/Zeitlich") ist hier raus —
                    // stoert die Konzentration auf die Aufgaben. Filter-State
                    // im ViewModel bleibt unveraendert (zeigt also alle Eintraege).

                    if (isEmpty) {
                        item(key = "empty", contentType = "empty") { EmptyState() }
                    } else {
                        // Aktive Eintraege gruppiert nach Time-Bucket. Frank-Wunsch
                        // 2026-05-09: HEUTE-Limit wird im ViewModel via
                        // autoBalanceBuckets() durchgesetzt — die DB enthaelt also
                        // schon nur max 5 in HEUTE. Restliche Eintraege wurden auf
                        // MORGEN/FREIBLOCK/SPAETER verteilt. Wir zeigen alle Buckets
                        // sortiert nach priorityScore desc damit Frank ALLE Aufgaben
                        // sieht.
                        // PERFORMANCE 2026-05-09: Sortierung+Filter laufen jetzt im
                        // ViewModel (TasksViewModel.kt), nicht mehr hier — die Lists
                        // sind beim Eintreffen schon sortiert und gefiltert.
                        // Frank-Wunsch 2026-05-23: HEUTE wird nicht mehr automatisch
                        // nachgefuellt. Sind alle HEUTE-Aufgaben erledigt (HEUTE leer)
                        // und liegen in aelteren Bereichen noch offene Aufgaben, zeigen
                        // wir im HEUTE-Bereich einen Button der 5 neue nachholt.
                        val hasPullableTasks =
                            state.entriesByBucket.any { (b, l) ->
                                b != de.frank.entropyreducer.domain.model.TimeBucket.HEUTE &&
                                    l.isNotEmpty()
                            }
                        // Frank-Wunsch 2026-05-24: jeder Bucket ist ein aufklappbares
                        // Akkordeon. Der Header wird IMMER gezeigt (auch wenn leer), die
                        // Eintraege nur wenn der Block aufgeklappt ist. Es ist immer nur
                        // ein Block gleichzeitig offen (gesteuert ueber expandedSection).
                        ALL_TIME_BUCKETS.forEach { bucket ->
                            val list = state.entriesByBucket[bucket].orEmpty()
                            val sectionExpanded = expandedSection == bucket.name
                            item(key = "header-${bucket.name}", contentType = "bucket-header") {
                                BucketHeader(
                                    bucket = bucket,
                                    count = list.size,
                                    expanded = sectionExpanded,
                                    onToggle = {
                                        expandedSection = if (sectionExpanded) null else bucket.name
                                    },
                                )
                            }
                            if (sectionExpanded) {
                                if (list.isNotEmpty()) {
                                    items(
                                        items = list,
                                        key = { it.id },
                                        contentType = { "entry" },
                                    ) { entry ->
                                        // PERFORMANCE 2026-05-09: Lambdas mit remember(entry.id)
                                        // stabilisieren, damit EntropyEntryCard skippable bleibt
                                        // (zusammen mit @Immutable auf der Entity). Ohne diese
                                        // Stabilisierung erzeugt jede Recomposition neue Lambda-
                                        // Instanzen → alle sichtbaren Karten recomposen → Jank.
                                        val onClick =
                                            remember(entry.id) { { onOpenEntryDetail(entry.id) } }
                                        val onResolve =
                                            remember(entry.id, entry.title) {
                                                {
                                                    vm.markEntryResolved(entry.id)
                                                    scope.launch {
                                                        val result =
                                                            snackbar.showSnackbar(
                                                                message =
                                                                    "Eintrag erledigt: ${entry.title}",
                                                                actionLabel = "Rückgängig",
                                                                duration =
                                                                    androidx.compose.material3
                                                                        .SnackbarDuration
                                                                        .Short,
                                                            )
                                                        if (
                                                            result ==
                                                                androidx.compose.material3
                                                                    .SnackbarResult
                                                                    .ActionPerformed
                                                        ) {
                                                            vm.reopenEntry(entry.id)
                                                        }
                                                    }
                                                    Unit
                                                }
                                            }
                                        val onPickBucket =
                                            remember(entry.id) {
                                                { bucketPickerEntryId = entry.id }
                                            }
                                        val onSetPrio =
                                            remember(entry.id) {
                                                { score: Double ->
                                                    vm.setManualPriority(entry.id, score)
                                                }
                                            }
                                        EntropyEntryCard(
                                            entry = entry,
                                            onClick = onClick,
                                            onResolve = onResolve,
                                            onPickBucket = onPickBucket,
                                            onSetManualPriority = onSetPrio,
                                            autoOpenPrioSlider = prioPickerEntryId == entry.id,
                                            onPrioSliderConsumed = { prioPickerEntryId = null },
                                        )
                                    }
                                } else if (
                                    bucket ==
                                        de.frank.entropyreducer.domain.model.TimeBucket.HEUTE &&
                                        hasPullableTasks
                                ) {
                                    // HEUTE ist leer (alles erledigt) — Nachlade-Button statt
                                    // automatischer Nachfuellung (Frank-Wunsch 2026-05-23).
                                    item(key = "refill-heute", contentType = "refill-heute") {
                                        RefillHeuteButton(onClick = { vm.refillHeute() })
                                    }
                                } else {
                                    item(
                                        key = "empty-${bucket.name}",
                                        contentType = "bucket-empty",
                                    ) {
                                        EmptyBucketHint()
                                    }
                                }
                            }
                        }
                        // Loop-Block (Frank-Wunsch 2026-05-24): wiederkehrende Aufgaben
                        // 1:1 wie im fruehern Loop-Reiter, jetzt als Akkordeon zwischen
                        // SPAETER und ERLEDIGT. Checkbox an = Aufgabe erscheint im Reiter
                        // (Verknuepfung lebt im RecurringTemplatesViewModel, unveraendert).
                        run {
                            val loopExpanded = expandedSection == SECTION_LOOP
                            val loopAccent = cosmos.accentTasks
                            item(key = "header-loop", contentType = "loop-header") {
                                AccordionHeaderRow(
                                    label = "Loop",
                                    icon = Icons.Outlined.Repeat,
                                    accent = loopAccent,
                                    count = recurringTemplates.size,
                                    expanded = loopExpanded,
                                    onToggle = {
                                        expandedSection = if (loopExpanded) null else SECTION_LOOP
                                    },
                                )
                            }
                            if (loopExpanded) {
                                if (recurringTemplates.isNotEmpty()) {
                                    items(
                                        items = recurringTemplates,
                                        key = { "loop-${it.id}" },
                                        contentType = { "loop-entry" },
                                    ) { template ->
                                        // Frische Lambdas (1:1 wie im Loop-Reiter) damit
                                        // toggleActive immer die aktuelle isActive-Version
                                        // der Vorlage liest.
                                        TemplateAsTaskCard(
                                            template = template,
                                            onToggleActive = { recurringVm.toggleActive(template) },
                                            onDelete = { recurringVm.delete(template) },
                                            onSetPriority = { score ->
                                                recurringVm.setPriority(template, score)
                                            },
                                            onSetTargetBucket = { bucket ->
                                                recurringVm.setTargetBucket(template, bucket)
                                            },
                                            onSetInterval = { days ->
                                                recurringVm.setIntervalDays(template, days)
                                            },
                                            onOpenDetail = { onOpenLoopDetail(template.id) },
                                            autoOpenPrioSlider =
                                                prioPickerTemplateId == template.id,
                                            onPrioSliderConsumed = { prioPickerTemplateId = null },
                                            autoOpenBucketMenu =
                                                bucketPickerTemplateId == template.id,
                                            onBucketMenuConsumed = { bucketPickerTemplateId = null },
                                        )
                                    }
                                } else {
                                    item(key = "empty-loop", contentType = "bucket-empty") {
                                        EmptyBucketHint()
                                    }
                                }
                            }
                        }
                        // Erledigt-Sektion am Ende — Header immer sichtbar, Inhalt nur
                        // wenn der Block aufgeklappt ist (Frank-Wunsch 2026-05-24).
                        run {
                            val resolved = state.resolvedEntries
                            val resolvedExpanded = expandedSection == SECTION_ERLEDIGT
                            item(key = "resolved-header", contentType = "resolved-header") {
                                ResolvedHeader(
                                    count = resolved.size,
                                    expanded = resolvedExpanded,
                                    onToggle = {
                                        expandedSection =
                                            if (resolvedExpanded) null else SECTION_ERLEDIGT
                                    },
                                )
                            }
                            if (resolvedExpanded) {
                                if (resolved.isNotEmpty()) {
                                    items(
                                        items = resolved,
                                        key = { "resolved-${it.id}" },
                                        contentType = { "entry" },
                                    ) { entry ->
                                        val onClick =
                                            remember(entry.id) { { onOpenEntryDetail(entry.id) } }
                                        val onResolve =
                                            remember(entry.id) { { vm.reopenEntry(entry.id) } }
                                        EntropyEntryCard(
                                            entry = entry,
                                            onClick = onClick,
                                            onResolve = onResolve,
                                        )
                                    }
                                } else {
                                    item(key = "empty-resolved", contentType = "bucket-empty") {
                                        EmptyBucketHint()
                                    }
                                }
                            }
                        }
                        // KI-Vorschlaege-Sektion (Frank-Wunsch 2026-06-18):
                        // Aufgaben die aus Ideen per Gemini generiert werden.
                        // Gelber Akkordeon-Header, Flag-Button zum Uebernehmen.
                        run {
                            val kiExpanded = expandedSection == SECTION_KI_VORSCHLAEGE
                            val kiAccent = Color(0xFFFBBF24)
                            item(key = "header-ki-vorschlaege", contentType = "ki-vorschlaege-header") {
                                AccordionHeaderRow(
                                    label = "Aufgabenvorschläge",
                                    icon = Icons.Outlined.AutoAwesome,
                                    accent = kiAccent,
                                    count = kiTaskSuggestions.size,
                                    expanded = kiExpanded,
                                    onToggle = {
                                        expandedSection = if (kiExpanded) null else SECTION_KI_VORSCHLAEGE
                                    },
                                )
                            }
                            if (kiExpanded) {
                                // Generieren-Button
                                item(key = "ki-gen-button", contentType = "ki-gen-button") {
                                    KiSuggestGenerateButton(
                                        state = kiTaskSuggestState,
                                        onClick = { kiTaskSuggestVm.generateSuggestions() },
                                        onReset = { kiTaskSuggestVm.resetProcessedIdeas() },
                                    )
                                }
                                // Fehler-Anzeige
                                kiTaskError?.let { err ->
                                    item(key = "ki-error", contentType = "ki-error") {
                                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = err,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = LocalCosmos.current.crit,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                IconButton(onClick = { kiTaskSuggestVm.dismissError() }) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Close,
                                                        contentDescription = "Fehler schliessen",
                                                        tint = LocalCosmos.current.crit,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (kiTaskSuggestions.isNotEmpty()) {
                                    itemsIndexed(
                                        items = kiTaskSuggestions,
                                        key = { _, sug -> "ki-sug-${sug.id}" },
                                        contentType = { _, _ -> "ki-suggestion" },
                                    ) { index, suggestion ->
                                        KiSuggestionCard(
                                            index = index + 1,
                                            suggestion = suggestion,
                                            isAccepting = kiTaskAcceptingId == suggestion.id,
                                            onAccept = { kiTaskSuggestVm.acceptSuggestion(suggestion) },
                                            onDelete = { kiTaskSuggestVm.deleteSuggestion(suggestion.id) },
                                        )
                                    }
                                } else if (kiTaskSuggestState != KiTaskSuggestState.LOADING) {
                                    item(key = "empty-ki-vorschlaege", contentType = "bucket-empty") {
                                        EmptyBucketHint()
                                    }
                                }
                            }
                        }
                    }
                    item(key = "bottom-spacer", contentType = "spacer") {
                        Spacer(Modifier.height(120.dp)) // Platz für Bottom-Nav
                    }
                }
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp),
            ) {
                Snackbar(it)
            }
            // Frank-Wunsch 2026-05-22: einheitliche Mic-Aktion mit BottomBar-Farbe.
            // Switcher offen → Cyan (Uebersichts-Mic-Farbe), sonst Orange (Aufgaben-Sub).
            val switcher =
                de.frank.entropyreducer.presentation.navigation.LocalBottomBarSwitcher.current
            val micAccent =
                if (switcher.showSwitcher) LocalCosmos.current.accent
                else LocalCosmos.current.accentTasks
            de.frank.entropyreducer.presentation.components.MicCaptureActions(
                visible = micActionsOpen,
                accent = micAccent,
                onTextCommit = { text, source -> vm.processCapturedText(text, source) },
                onClose = { micActionsOpen = false },
                // Frank-Wunsch 2026-05-31: eingesprochene Aufgaben erst im
                // Review-Fenster pruefen/verbessern/benennen, dann speichern.
                onReviewTranscript = { reviewTranscript = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            // Review-Fenster nach der Transkription (Frank-Wunsch 2026-05-31).
            reviewTranscript?.let { transcript ->
                TaskCaptureReviewDialog(
                    initialTranscript = transcript,
                    accent = micAccent,
                    onImprove = { text -> vm.improveTranscript(text) },
                    onSave = { text, manualTitle ->
                        vm.processCapturedText(
                            text,
                            de.frank.entropyreducer.domain.model.EntrySource.NUTZER_MIC,
                            manualTitle,
                        )
                        reviewTranscript = null
                    },
                    onDismiss = { reviewTranscript = null },
                )
            }
        }
    }

    // Detail wird jetzt als Vollbild-Screen ueber den NavGraph aufgerufen
    // (Frank-Wunsch 2026-05-20). Klick auf eine Eintrag-Card navigiert direkt
    // — kein Bottom-Sheet mehr, kein detailEntry-State im ViewModel noetig.

    // Proaktiver Forscher: nach dem Erledigen eines Eintrags fragt die App
    // direkt wie der Eintrag geloest wurde. Das Insight-Board lernt daraus
    // (Frank-Wunsch 2026-05-08).
    state.pendingMethodFor?.let { entry ->
        MethodPromptDialog(
            entry = entry,
            onSubmit = { notes -> vm.submitMethod(notes) },
            onDismiss = { vm.dismissMethodPrompt() },
        )
    }

    // Bucket-Picker-Sheet (Frank-Wunsch 2026-05-09): aktiv wenn der Pill-Button
    // unten rechts in einer Card geklickt wurde. Eintrag wird live aus dem State
    // gezogen, damit Aenderungen ohne Sheet-Neuoeffnen sichtbar sind.
    bucketPickerEntryId?.let { id ->
        val allActive: List<EntropyEntryEntity> =
            state.entriesByBucket.values.flatten() + state.resolvedEntries
        val entry = allActive.firstOrNull { it.id == id }
        if (entry != null) {
            BucketPickerSheet(
                entry = entry,
                onPick = { bucket -> vm.setManualBucket(id, bucket) },
                onClearManual = { vm.clearManualBucket(id) },
                onClose = { bucketPickerEntryId = null },
            )
        } else if (allActive.isNotEmpty()) {
            // Bugfix 2026-05-11: Eintrag nur reseten wenn Tasks SCHON geladen
            // sind UND der Eintrag wirklich weg ist. Beim Start via Widget ist
            // allActive zunaechst leer (Tasks laden noch) — wir warten bis sie
            // da sind, statt das Sheet vorzeitig zu schliessen.
            bucketPickerEntryId = null
        }
    }
}

@Composable
private fun MethodPromptDialog(
    entry: EntropyEntryEntity,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val notesState = remember(entry.id) { androidx.compose.runtime.mutableStateOf("") }
    val notes = notesState.value
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor =
            if (cosmos.isDark) CosmosColors.BgDarkAccent else CosmosColors.BgLightAccent,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sparkle-Icon-Kreis links — visueller Anker fuer "Forscher fragt"
                Box(
                    modifier =
                        Modifier.size(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(LocalCosmos.current.accentForscher.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⚛", color = LocalCosmos.current.accentForscher, fontSize = 22.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wie hast du das gelöst?",
                        color = cosmos.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "\"${entry.title}\"",
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text =
                        "Der Forscher merkt sich deine Methode und legt sie als " +
                            "bestätigte Vorgehensweise im Insight-Board ab.",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                Box {
                    androidx.compose.material3.OutlinedTextField(
                        value = notes,
                        onValueChange = { notesState.value = it },
                        placeholder = {
                            Text(
                                "z.B. Früh schlafen + 20 min Spaziergang",
                                color = cosmos.textSecondary,
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors =
                            androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = cosmos.textPrimary,
                                unfocusedTextColor = cosmos.textPrimary,
                                focusedBorderColor = LocalCosmos.current.accent,
                                unfocusedBorderColor = cosmos.glassBorder,
                            ),
                    )
                    // Mic-Button rechts unten — Whisper Large V3 Turbo (Frank-Wunsch
                    // 2026-05-08, ersetzt System-SpeechRecognizer).
                    de.frank.entropyreducer.presentation.components.WhisperMicButton(
                        onTranscript = { transcript ->
                            notesState.value =
                                if (notesState.value.isBlank()) transcript
                                else "${notesState.value} $transcript"
                        },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    onSubmit(notes)
                    notesState.value = ""
                },
                enabled = notes.isNotBlank(),
            ) {
                Text(
                    "Speichern",
                    color = LocalCosmos.current.accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Überspringen", color = cosmos.textSecondary)
            }
        },
    )
}

/**
 * Detail-Bottom-Sheet (Bild 12/22). Zeigt Eintrag im Detail mit Icon-Kreis, Title, Beschreibung,
 * Schweregrad-Hinweis, 4 Status-Buttons (Offen / In Arbeit / Reduziert / Archiviert), Tags,
 * KI-Begruendung + KI-Notizen, sowie ein "Löschen"-Button. Aus dem Sheet kann der Status direkt
 * umgestellt werden.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun EntryDetailSheet(
    entry: EntropyEntryEntity,
    onClose: () -> Unit,
    onSetStatus: (EntryStatus) -> Unit,
    onDelete: () -> Unit,
    onAddFollowup: (String) -> Unit,
) {
    val cosmos = LocalCosmos.current
    val sheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // windowInsets = WindowInsets(0): Sheet uebernimmt die Insets selbst nicht — die
    // Column unten kompensiert mit eigenem Bottom-Padding. So bleibt der ganze Sheet
    // bis zum unteren Bildschirmrand sichtbar (kein doppeltes Inset-Padding).
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = bottomInset + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header: Title + X
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Eintrag im Detail",
                    style = MaterialTheme.typography.titleLarge,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.Close,
                        contentDescription = "Schließen",
                        tint = cosmos.textSecondary,
                    )
                }
            }
            // Hero-Card mit Icon-Kreis + Title + Score
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        CategoryIconCircle(category = entry.category, tint = entry.category.color())
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            EntropyCategoryPill(entry.category)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = cosmos.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = entry.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cosmos.textSecondary,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${entry.priorityScore.toInt()}",
                                color = priorityColor(entry.priorityScore),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Prio",
                                color = cosmos.textSecondary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Schweregrad: ${severityLabel(entry.severity)} (${entry.severity}/10)",
                        style = MaterialTheme.typography.labelMedium,
                        color = cosmos.textSecondary,
                    )
                    Spacer(Modifier.height(6.dp))
                    SeverityRainbowBar(severity = entry.severity)
                }
            }
            // Status-Section: 4 Buttons
            Text(
                "Status",
                style = MaterialTheme.typography.titleSmall,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusButton(
                    "Offen",
                    EntryStatus.OFFEN,
                    entry.status,
                    onSetStatus,
                    modifier = Modifier.weight(1f),
                )
                StatusButton(
                    "In Arbeit",
                    EntryStatus.IN_ARBEIT,
                    entry.status,
                    onSetStatus,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusButton(
                    "Reduziert",
                    EntryStatus.REDUZIERT,
                    entry.status,
                    onSetStatus,
                    modifier = Modifier.weight(1f),
                )
                StatusButton(
                    "Archiviert",
                    EntryStatus.ARCHIVIERT,
                    entry.status,
                    onSetStatus,
                    modifier = Modifier.weight(1f),
                )
            }
            // Nachtrag-per-Sprache (Frank-Wunsch 2026-05-08): Mic-Button startet
            // System-SpeechRecognizer, Transkript wird an die Beschreibung
            // angehaengt und der Eintrag durch ProcessEntryUseCase neu bewertet
            // (Prio + Bucket + Dauer landen automatisch an der richtigen Stelle).
            FollowupMicButton(onTranscript = onAddFollowup)
            // Tags
            if (entry.tags.isNotEmpty()) {
                Text(
                    "Tags",
                    style = MaterialTheme.typography.titleSmall,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.tags.forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = cosmos.textSecondary,
                            modifier =
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(cosmos.glassBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            // KI-Begruendung
            if (entry.priorityReason.isNotBlank() || !entry.aiNotes.isNullOrBlank()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            "KI-Begruendung",
                            style = MaterialTheme.typography.titleSmall,
                            color = LocalCosmos.current.accentForscher,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = entry.priorityReason.ifBlank { "(keine Begruendung)" },
                            style = MaterialTheme.typography.bodySmall,
                            color = cosmos.textPrimary,
                        )
                        if (!entry.aiNotes.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "KI-Notizen",
                                style = MaterialTheme.typography.titleSmall,
                                color = LocalCosmos.current.accentForscher,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = entry.aiNotes!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = cosmos.textPrimary,
                            )
                        }
                    }
                }
            }
            // Löschen-Button — gefuellt rot, klar sichtbar, fixe Hoehe damit Text
            // immer lesbar ist; horizontales Default-Padding entfernt damit Icon+Text
            // bei jeder Sheet-Breite mittig sitzen.
            androidx.compose.material3.Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors =
                    androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = LocalCosmos.current.crit,
                        contentColor = androidx.compose.ui.graphics.Color.White,
                    ),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 10.dp,
                    ),
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Löschen",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FollowupMicButton(onTranscript: (String) -> Unit) {
    val cosmos = LocalCosmos.current
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LocalCosmos.current.accentForscher.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Whisper Large V3 Turbo statt System-SpeechRecognizer (Frank-Wunsch 2026-05-08).
        de.frank.entropyreducer.presentation.components.WhisperMicButton(
            onTranscript = onTranscript,
            size = 40.dp,
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "Nachtrag einsprechen",
                color = cosmos.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Whisper Large V3 Turbo — KI bewertet die Aufgabe mit Nachtrag neu.",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun StatusButton(
    label: String,
    status: EntryStatus,
    current: EntryStatus,
    onClick: (EntryStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    val selected = status == current
    val accent = if (selected) LocalCosmos.current.accent else cosmos.textSecondary
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selected) LocalCosmos.current.accent.copy(alpha = 0.18f) else cosmos.glassBg
                )
                .clickable { onClick(status) }
                .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun severityLabel(severity: Int): String =
    when {
        severity <= 3 -> "Niedrig"
        severity <= 6 -> "Mittel"
        severity <= 8 -> "Hoch"
        else -> "Sehr hoch"
    }

/**
 * Mappt einen priorityScore (0.0-100.0) auf eine Farbe fuer die grosse Prio-Zahl auf der
 * Aufgabenkarte. Skala (Frank-Wunsch 2026-05-10): 80-100 -> Rot (sehr wichtig) 60-80 -> Orange
 * 40-60 -> Gelb 20-40 -> Gruen 0-20 -> Blau (geringste Prio — kuehlste Farbe) Achtung: Bewusst
 * andersherum als die Severity-Bar (dort ist Rot schlecht).
 */
private fun priorityColor(score: Double): Color =
    when {
        score >= 80.0 -> CosmosColors.PriorityRed
        score >= 60.0 -> CosmosColors.PriorityOrange
        score >= 40.0 -> CosmosColors.PriorityYellow
        score >= 20.0 -> CosmosColors.PriorityGreen
        else -> CosmosColors.PriorityBlue
    }

@Composable
private fun CategoryFilterRow(
    active: Set<EntropyCategory>,
    onToggle: (EntropyCategory) -> Unit,
    onClearAll: () -> Unit,
) {
    // values() allociert ein neues Array bei jedem Aufruf — einmal cachen.
    val categories = remember { EntropyCategory.values().toList() }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            val isAll = active.isEmpty()
            CategoryFilterChip(
                label = "Alle",
                icon = Icons.Outlined.GridView,
                tint = LocalCosmos.current.accent,
                selected = isAll,
                onClick = onClearAll,
            )
        }
        items(items = categories, key = { it.name }) { cat ->
            val on = cat in active
            CategoryFilterChip(
                label = cat.label(),
                icon = iconForCategory(cat),
                tint = cat.color(),
                selected = on,
                onClick = { onToggle(cat) },
            )
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    // PERFORMANCE 2026-05-09: clip() entfernt — background mit Shape uebernimmt das.
    val pillShape = remember { RoundedCornerShape(50) }
    val bg = if (selected) tint.copy(alpha = 0.20f) else cosmos.glassBg
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.background(bg, pillShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) tint else cosmos.textSecondary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) tint else cosmos.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun iconForCategory(
    category: EntropyCategory
): androidx.compose.ui.graphics.vector.ImageVector =
    when (category) {
        EntropyCategory.KOERPERLICH -> Icons.Outlined.Bolt
        EntropyCategory.MENTAL -> Icons.Outlined.Psychology
        EntropyCategory.ZEITLICH -> Icons.Outlined.AccessTime
        EntropyCategory.EMOTIONAL -> Icons.Outlined.FavoriteBorder
        EntropyCategory.GESUNDHEITLICH -> Icons.Outlined.MedicalServices
        EntropyCategory.UMGEBUNG -> Icons.Outlined.Home
        EntropyCategory.SONSTIGES -> Icons.Outlined.MoreHoriz
    }

/**
 * "Neue Aufgaben hinzufügen?"-Button (Frank-Wunsch 2026-05-23). Erscheint im HEUTE-Bereich erst
 * wenn alle HEUTE-Aufgaben erledigt sind — statt automatischer Nachfuellung. Hellgelber
 * Hintergrund, ruft vm.refillHeute() auf das 5 neue Aufgaben aus den aelteren Bereichen nach HEUTE
 * holt.
 */
@Composable
private fun RefillHeuteButton(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFEF3C7)) // hellgelb (amber-100)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = Color(0xFF92400E), // dunkles Bernstein fuer Kontrast auf Hellgelb
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Neue Aufgaben hinzufügen?",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF92400E),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BucketHeader(bucket: TimeBucket, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    val label =
        when (bucket) {
            TimeBucket.HEUTE -> "HEUTE"
            TimeBucket.MORGEN -> "MORGEN"
            TimeBucket.FREIBLOCK -> "FREIBLOCK"
            TimeBucket.SPAETER -> "SPÄTER"
        }
    AccordionHeaderRow(
        label = label,
        icon = bucketIcon(bucket),
        accent = bucketAccent(bucket),
        count = count,
        expanded = expanded,
        onToggle = onToggle,
    )
}

@Composable
private fun ResolvedHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    AccordionHeaderRow(
        label = "ERLEDIGT",
        icon = Icons.Outlined.CheckCircle,
        accent = LocalCosmos.current.ok,
        count = count,
        expanded = expanded,
        onToggle = onToggle,
    )
}

/**
 * Gemeinsamer Akkordeon-Header fuer die Aufgabenbloecke (Frank-Wunsch 2026-05-24). Klickbare Zeile
 * mit Icon-Pille, Label, Count-Pille und einem Chevron das beim Aufklappen sanft um 180° dreht. Im
 * aufgeklappten Zustand bekommt die Zeile eine zarte Akzent-Toenung, damit der aktive Block sofort
 * erkennbar ist.
 */
@Composable
private fun AccordionHeaderRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val chevronRotation by
        animateFloatAsState(
            targetValue = if (expanded) 180f else 0f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            label = "accordion-chevron",
        )
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (expanded) accent.copy(alpha = 0.12f) else Color.Transparent)
                .clickable(onClick = onToggle)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier =
                Modifier.clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Zuklappen" else "Aufklappen",
            tint = accent,
            modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = chevronRotation },
        )
    }
}

/** Dezenter Hinweis fuer einen aufgeklappten, aber leeren Aufgabenblock. */
@Composable
private fun EmptyBucketHint() {
    val cosmos = LocalCosmos.current
    Text(
        text = "Keine Aufgaben",
        style = MaterialTheme.typography.bodySmall,
        color = cosmos.textSecondary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

private fun bucketIcon(bucket: TimeBucket): androidx.compose.ui.graphics.vector.ImageVector =
    when (bucket) {
        TimeBucket.HEUTE -> Icons.Outlined.Today
        TimeBucket.MORGEN -> Icons.Outlined.Event
        TimeBucket.FREIBLOCK -> Icons.Outlined.DateRange
        TimeBucket.SPAETER -> Icons.Outlined.HourglassEmpty
    }

@Composable
private fun bucketAccent(bucket: TimeBucket): Color =
    when (bucket) {
        TimeBucket.HEUTE -> LocalCosmos.current.accent
        TimeBucket.MORGEN -> LocalCosmos.current.accentForscher
        TimeBucket.FREIBLOCK -> CosmosColors.CatHealth
        TimeBucket.SPAETER -> LocalCosmos.current.textSecondary
    }

/**
 * Liefert die ganz leichte Hintergrund-Toenung der Aufgabenkarte je nach Bucket (Frank-Wunsch
 * 2026-05-10, zweite Iteration). GlassCard rendert die Farbe als Linear-Gradient von oben-links
 * (transparent) nach unten-rechts (voller Tint). Frank wollte das Orange/Gelb/Gruen/Blau dezenter —
 * daher hier zusaetzlich der Endwert-Alpha um ~25% reduziert (light: 0.18→0.14, dark: 0.12→0.10).
 * Zusammen mit dem Verlauf wirkt das Orange jetzt sehr zurueckhaltend und nur in der unteren
 * rechten Card-Ecke schwach erkennbar.
 * - HEUTE = Orange-Stich
 * - MORGEN = Gelb-Stich
 * - FREIBLOCK = Gruen-Stich
 * - SPAETER = Blau-Stich
 */
/**
 * Frank-Wunsch 2026-05-22 (vierte Iteration): Karten-Hintergrund folgt jetzt der
 * priorityScore-Farbe statt dem Time-Bucket. So sieht Frank auf einen Blick wie wichtig eine
 * Aufgabe ist, ohne die Prio-Zahl rechts ablesen zu muessen.
 *
 * Skala identisch zu priorityColor() in dieser Datei (Konsistenz mit der grossen Prio-Zahl rechts
 * auf der Karte):
 * - 80-100 Rot (sehr hohe Prio)
 * - 60-80 Orange
 * - 40-60 Gelb
 * - 20-40 Gruen
 * - 0-20 Blau (geringste Prio)
 *
 * Alpha leicht hoeher als der frueher Bucket-Tint (light: 0.18, dark: 0.14), damit der
 * Farbunterschied zwischen 35/55/85 deutlich sichtbar ist — Frank will den Stich wirklich erkennen
 * koennen.
 */
private fun priorityCardTint(score: Double, isDark: Boolean): Color {
    val base =
        when {
            score >= 80.0 -> CosmosColors.PriorityRed
            score >= 60.0 -> CosmosColors.PriorityOrange
            score >= 40.0 -> CosmosColors.PriorityYellow
            score >= 20.0 -> CosmosColors.PriorityGreen
            else -> CosmosColors.PriorityBlue
        }
    return base.copy(alpha = if (isDark) 0.14f else 0.18f)
}

// Prioritaets-Farbrampe (kontinuierlich, 5%-Schritte, drei Familien Gruen→Gelb→Rot)
// ausgelagert nach presentation/PriorityRamp.kt — SINGLE SOURCE. App-Karte UND
// Home-Screen-Widget nutzen dieselbe Funktion, damit die Farben bit-identisch sind
// (Frank-Wunsch 2026-05-31). priorityRampColor() kommt jetzt via Import oben.

/**
 * Frank-Wunsch 2026-06-01: Erledigungs-Zeitpunkt menschenlesbar (Tag + Uhrzeit) — z.B. "01.06.2026
 * um 12:38". Quelle ist resolvedAt, das jeder Erledigen-Pfad setzt.
 */
private fun formatResolvedAt(ms: Long): String {
    val dt = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
    return "%02d.%02d.%d um %02d:%02d"
        .format(dt.dayOfMonth, dt.monthValue, dt.year, dt.hour, dt.minute)
}

@Composable
private fun EntropyEntryCard(
    entry: EntropyEntryEntity,
    onClick: () -> Unit = {},
    onResolve: () -> Unit = {},
    onSeverityHint: () -> Unit = {},
    onPickBucket: () -> Unit = {},
    onSetManualPriority: (Double) -> Unit = {},
    autoOpenPrioSlider: Boolean = false,
    onPrioSliderConsumed: () -> Unit = {},
) {
    val cosmos = LocalCosmos.current
    val isResolved = entry.status == EntryStatus.REDUZIERT || entry.status == EntryStatus.ARCHIVIERT
    // PERFORMANCE 2026-05-09: graphicsLayer statt Modifier.alpha — bei alpha = 1f
    // wird der Layer komplett uebersprungen (compositingStrategy = ModulateAlpha
    // sorgt dafuer dass nur die Pixel-Alpha moduliert wird, ohne separate
    // Off-Screen-Buffer-Allocation). Vorher: Modifier.alpha erzeugte auch bei
    // alpha = 1f gelegentlich einen Layer.
    val cardAlpha = if (isResolved) 0.55f else 1f
    // Frank-Wunsch 2026-05-31: manuelle Prioritaet per Schieberegler. Solange der
    // Regler aktiv ist, faerbt sich die Kachel LIVE nach dem aktuellen Reglerwert.
    // Effektive Prioritaet = Live-Regler ?: manueller Wert ?: KI-Wert.
    var sliderActive by remember(entry.id) { mutableStateOf(false) }
    var liveSlider by remember(entry.id) { mutableStateOf<Float?>(null) }
    // Frank-Wunsch 2026-05-31: Tap auf die Prio-Perle im Widget oeffnet die App und
    // klappt direkt den Schieberegler GENAU dieser Aufgabe auf (Verknuepfung zur
    // manuellen Prioritaet, analog zum Bucket-Picker). autoOpenPrioSlider wird vom
    // TasksScreen gesetzt, sobald der Widget-Deep-Link diese Karte meint; danach wird
    // die Markierung via onPrioSliderConsumed sofort wieder geleert.
    LaunchedEffect(autoOpenPrioSlider) {
        if (autoOpenPrioSlider) {
            sliderActive = true
            onPrioSliderConsumed()
        }
    }
    val effectivePriority =
        liveSlider?.toDouble() ?: entry.manualPriorityScore ?: entry.priorityScore

    // Frank-Wunsch 2026-05-31: Karten-Hintergrund ist ein kraeftiger horizontaler
    // Verlauf in der Prioritaetsfarbe — links dezent, rechts die volle Farbe.
    // Die Farbe allein (Dunkelrot=hoch … Hellgruen=niedrig, kein Blau) macht die
    // Prioritaet sofort sichtbar; eine Prio-Zahl ist nicht mehr noetig.
    val ramp = priorityRampColor(effectivePriority)
    val priorityBrush =
        remember(ramp) { Brush.horizontalGradient(colors = listOf(ramp.copy(alpha = 0.20f), ramp)) }
    GlassCard(
        modifier =
            Modifier.fillMaxWidth().clickable(onClick = onClick).graphicsLayer {
                alpha = cardAlpha
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
        tintBrush = priorityBrush,
    ) {
        Column {
            // Frank-Wunsch 2026-05-31: radikal vereinfacht — ganz vorne das Haekchen
            // (hakt direkt ab, kein Dialog), dann der Titel in EINER Zeile (von der KI
            // auf max. 3 Woerter zusammengefasst). Keine Beschreibung, keine Prio-Zahl,
            // kein Schweregrad-Balken, keine Tags/Bereiche, kein Empfohlen/Zeitaufwand.
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Haekchen: leeres Quadrat wenn offen, ausgefuelltes Haekchen wenn erledigt.
                // Frank-Wunsch 2026-05-31: offenes Haekchenfeld hat eine weisse Fuellflaeche
                // (gleiche Farbe wie die KI-/Prio-Perlen = cosmos.glassBg) und eine graue
                // Umrandung — damit klar als antippbares Feld erkennbar.
                val checkBg =
                    if (isResolved) LocalCosmos.current.ok.copy(alpha = 0.85f) else cosmos.glassBg
                val checkBorder = if (isResolved) LocalCosmos.current.ok else cosmos.textSecondary
                Box(
                    modifier =
                        Modifier.size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(checkBg)
                            .border(
                                androidx.compose.foundation.BorderStroke(2.dp, checkBorder),
                                RoundedCornerShape(8.dp),
                            )
                            .clickable(onClick = onResolve),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isResolved) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Erledigt",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            // Frank-Wunsch 2026-06-01: Bei erledigten Aufgaben sichtbar anzeigen WANN genau
            // erledigt wurde (Tag + Uhrzeit aus resolvedAt). Eingerueckt unter den Titel
            // (40dp = Haekchen 28dp + 12dp Abstand).
            if (isResolved) {
                entry.resolvedAt?.let { resolvedMs ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Erledigt am ${formatResolvedAt(resolvedMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                        modifier = Modifier.padding(start = 40.dp),
                    )
                }
            }

            // Untere Zeile: Priorität-Perle + KI/manuell-Perle (rechts).
            Spacer(Modifier.height(8.dp))
            val prioLabel =
                when {
                    liveSlider != null -> "Priorität ${Math.round(liveSlider!!)}"
                    entry.manualPriorityScore != null ->
                        "Priorität ${entry.manualPriorityScore!!.toInt()}"
                    else -> "Priorität KI"
                }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                PriorityPearl(label = prioLabel, onClick = { sliderActive = !sliderActive })
                Spacer(Modifier.width(8.dp))
                BucketPickerButton(
                    isManual = entry.manualBucket != null,
                    bucket = entry.timeBucket,
                    onClick = onPickBucket,
                )
            }
            // Schieberegler — nur wenn die Priorität-Perle angetippt wurde. In 5%-
            // Schritten (steps=19 → 0,5,…,100). Beim Schieben aendert sich Kachel-
            // Farbe + Wert live; beim Loslassen wird gespeichert und der Regler
            // klappt wieder zu.
            if (sliderActive) {
                Spacer(Modifier.height(8.dp))
                val sliderPos =
                    liveSlider ?: (entry.manualPriorityScore ?: entry.priorityScore).toFloat()
                androidx.compose.material3.Slider(
                    value = sliderPos.coerceIn(0f, 100f),
                    onValueChange = { liveSlider = it },
                    onValueChangeFinished = {
                        liveSlider?.let { onSetManualPriority(it.toDouble()) }
                        sliderActive = false
                    },
                    valueRange = 0f..100f,
                    steps = 19,
                    colors =
                        androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = ramp,
                            activeTrackColor = ramp,
                        ),
                )
            }
        }
    }
}

/**
 * Kleine Perle (gleicher Stil wie die KI/manuell-Perle) die die Prioritaet zeigt: "Priorität KI"
 * wenn die KI bestimmt, sonst "Priorität <Wert>" bei manuellem Wert. Klick oeffnet den
 * Schieberegler. Die Perle selbst aendert ihre Farbe NICHT — nur die Kachel dahinter faerbt sich
 * (Frank-Wunsch 2026-05-31).
 */
@Composable
private fun PriorityPearl(label: String, onClick: () -> Unit) {
    val cosmos = LocalCosmos.current
    val pillShape = remember { RoundedCornerShape(50) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.background(cosmos.glassBg, pillShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = cosmos.textSecondary,
        )
    }
}

/**
 * Kleiner Button unten rechts in der Card der das Bucket-Auswahl-Sheet oeffnet (Frank-Wunsch
 * 2026-05-09). Zeigt das Bucket-Icon mit aktiver Farbe wenn Frank den Bucket manuell zugewiesen hat
 * (manualBucket != null), sonst nur dezenter Outline-Style — die KI hat entschieden.
 */
@Composable
private fun BucketPickerButton(isManual: Boolean, bucket: TimeBucket, onClick: () -> Unit) {
    val cosmos = LocalCosmos.current
    // Frank-Wunsch 2026-05-31 (Folge-Korrektur): Beide Pillen (KI + manuell) sind
    // jetzt KOMPLETT gleich — gleicher heller Perle-Hintergrund (cosmos.glassBg) UND
    // gleiche Text-/Icon-Farbe (cosmos.textSecondary). Es unterscheidet nur noch das
    // Wort ("KI" vs. "manuell"); kein Hellblau/Blau mehr, das auf den farbigen
    // Kacheln schlecht lesbar war.
    val bg = cosmos.glassBg
    val tint = cosmos.textSecondary
    // PERFORMANCE 2026-05-09: clip() entfernt — background(color, shape) clippt
    // visuell (zeichnet abgerundete Form), Inhalte sind kurz und passen rein.
    val pillShape = remember { RoundedCornerShape(50) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.background(bg, pillShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = bucketIcon(bucket),
            contentDescription = "Bucket ändern",
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (isManual) "manuell" else "KI",
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (isManual) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * Bottom-Sheet zur manuellen Bucket-Zuordnung (Frank-Wunsch 2026-05-09). Zeigt vier
 * Bucket-Optionen + "KI bestimmt" als Reset. Aktive Auswahl wird in der Bucket-Akzent-Farbe
 * hervorgehoben.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun BucketPickerSheet(
    entry: EntropyEntryEntity,
    onPick: (TimeBucket) -> Unit,
    onClearManual: () -> Unit,
    onClose: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val sheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = bottomInset + 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Wann erledigen?",
                style = MaterialTheme.typography.titleLarge,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "\"${entry.title}\"",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            // Vier Bucket-Optionen
            ALL_TIME_BUCKETS.forEach { bucket ->
                val isActive =
                    entry.manualBucket == bucket ||
                        (entry.manualBucket == null && entry.timeBucket == bucket)
                BucketOptionRow(
                    bucket = bucket,
                    label = bucketLabelLong(bucket),
                    description = bucketDescription(bucket),
                    isActive = isActive,
                    isManual = entry.manualBucket == bucket,
                    onClick = {
                        onPick(bucket)
                        onClose()
                    },
                )
            }
            // Reset auf KI
            if (entry.manualBucket != null) {
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        onClearManual()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = LocalCosmos.current.accentForscher,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "KI entscheiden lassen",
                        color = LocalCosmos.current.accentForscher,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BucketOptionRow(
    bucket: TimeBucket,
    label: String,
    description: String,
    isActive: Boolean,
    isManual: Boolean,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = bucketAccent(bucket)
    val bg = if (isActive) accent.copy(alpha = 0.18f) else cosmos.glassBg
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(
            modifier =
                Modifier.size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = bucketIcon(bucket),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
        }
        if (isActive) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "aktiv",
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun bucketLabelLong(bucket: TimeBucket): String =
    when (bucket) {
        TimeBucket.HEUTE -> "Heute"
        TimeBucket.MORGEN -> "Morgen"
        TimeBucket.FREIBLOCK -> "Freiblock"
        TimeBucket.SPAETER -> "Später"
    }

private fun bucketDescription(bucket: TimeBucket): String =
    when (bucket) {
        TimeBucket.HEUTE -> "wird nicht automatisch nachgefüllt — neue Aufgaben erst per Button"
        TimeBucket.MORGEN -> "rückt morgen automatisch in Heute"
        TimeBucket.FREIBLOCK -> "nächster freier Schichtblock"
        TimeBucket.SPAETER -> "kein Datum — Sammelbecken"
    }

/** Farbiger Kreis mit Material-Icon basierend auf der Entropie-Kategorie. */
@Composable
internal fun CategoryIconCircle(
    category: de.frank.entropyreducer.domain.model.EntropyCategory,
    tint: androidx.compose.ui.graphics.Color,
) {
    val icon =
        when (category) {
            de.frank.entropyreducer.domain.model.EntropyCategory.KOERPERLICH -> Icons.Outlined.Bolt
            de.frank.entropyreducer.domain.model.EntropyCategory.MENTAL -> Icons.Outlined.Psychology
            de.frank.entropyreducer.domain.model.EntropyCategory.ZEITLICH ->
                Icons.Outlined.AccessTime
            de.frank.entropyreducer.domain.model.EntropyCategory.EMOTIONAL ->
                Icons.Outlined.FavoriteBorder
            de.frank.entropyreducer.domain.model.EntropyCategory.GESUNDHEITLICH ->
                Icons.Outlined.MedicalServices
            de.frank.entropyreducer.domain.model.EntropyCategory.UMGEBUNG -> Icons.Outlined.Home
            de.frank.entropyreducer.domain.model.EntropyCategory.SONSTIGES ->
                Icons.Outlined.MoreHoriz
        }
    // PERFORMANCE 2026-05-09: clip() entfernt — background(color, CircleShape)
    // zeichnet den Kreis direkt, das Icon ist innerhalb der size(44.dp) und
    // hat selbst nur 22.dp, kein Overflow moeglich.
    val circleShape = remember { RoundedCornerShape(50) }
    Box(
        modifier = Modifier.size(44.dp).background(tint.copy(alpha = 0.15f), circleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * 5-Segment-Severity-Bar im Regenbogen-Stil (Soll-Design). Die Segmente sind gleich groß, der
 * "ausgefuellte" Anteil ergibt sich aus severity/10. Nicht gefuellte Segmente sind ausgegraut,
 * gefuellte zeigen ihre Status-Farbe.
 */
@Composable
internal fun SeverityRainbowBar(severity: Int) {
    val cosmos = LocalCosmos.current
    val sev = severity.coerceIn(1, 10)
    // PERFORMANCE 2026-05-09: Komplette Neuimplementierung mit Canvas — vorher
    // 5 einzelne Box-Composables mit clip+background, was 5 zusaetzliche
    // GraphicsLayer pro Karte erzeugte. Bei 10 sichtbaren Karten = 50 Layer
    // nur fuer die Severity-Skala. Canvas zeichnet alle 5 Segmente in EINEM
    // Layer mit drawRoundRect — kein clip, kein Layer-Compositing.
    val palette = remember {
        listOf(
            CosmosColors.StatusGreen,
            CosmosColors.StatusLightGreen,
            CosmosColors.StatusYellow,
            CosmosColors.StatusOrange,
            CosmosColors.StatusRed,
        )
    }
    val emptyColor = cosmos.glassBg
    Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
        val segmentCount = 5
        val gap = 3.dp.toPx()
        val totalGap = gap * (segmentCount - 1)
        val segmentWidth = (size.width - totalGap) / segmentCount
        val cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        for (i in 0 until segmentCount) {
            val filled = sev >= (i + 1) * 2
            val x = i * (segmentWidth + gap)
            drawRoundRect(
                color = if (filled) palette[i] else emptyColor,
                topLeft = Offset(x, 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = cornerRadius,
            )
        }
    }
}

@Composable
private fun EntryMetaRow(entry: EntropyEntryEntity, modifier: Modifier = Modifier) {
    val cosmos = LocalCosmos.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Bucket-Time-Label (TimeBucket)
        val bucketLabel =
            when (entry.timeBucket) {
                de.frank.entropyreducer.domain.model.TimeBucket.HEUTE -> "heute"
                de.frank.entropyreducer.domain.model.TimeBucket.MORGEN -> "morgen"
                de.frank.entropyreducer.domain.model.TimeBucket.FREIBLOCK -> "Freiblock"
                de.frank.entropyreducer.domain.model.TimeBucket.SPAETER -> "später"
            }
        val durationHint =
            entry.estimatedDurationMinutes?.let {
                when {
                    it < 60 -> "$it min"
                    it < 24 * 60 -> "${it / 60} h"
                    else -> "${it / (24 * 60)} d"
                }
            }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = cosmos.textSecondary,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = if (durationHint != null) "$bucketLabel, $durationHint" else bucketLabel,
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        // Empfohlen-Badge (immer wenn priorityScore > 70)
        // PERFORMANCE 2026-05-09: clip() entfernt — background mit Shape clippt visuell.
        if (entry.priorityScore > 70) {
            val pillShape = remember { RoundedCornerShape(50) }
            Text(
                text = "Empfohlen",
                color = LocalCosmos.current.ok,
                style = MaterialTheme.typography.labelSmall,
                modifier =
                    Modifier.background(LocalCosmos.current.ok.copy(alpha = 0.15f), pillShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        // Wearable-Indikator (wenn ein Biomarker-Snapshot verlinkt ist)
        if (entry.biomarkerSnapshotId != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.MonitorHeart,
                    contentDescription = "Wearable-Bezug",
                    tint = LocalCosmos.current.accentForscher,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "Wearable",
                    color = LocalCosmos.current.accentForscher,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val cosmos = LocalCosmos.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Mic,
            contentDescription = null,
            tint = LocalCosmos.current.accent,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tippe auf das Mikrofon und sprich aus, was deine Energie kostet.",
            style = MaterialTheme.typography.titleMedium,
            color = cosmos.textPrimary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text =
                "Die KI ordnet es ein, priorisiert es und plant es in deinen Schichtkalender ein.",
            style = MaterialTheme.typography.bodyMedium,
            color = cosmos.textSecondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

/**
 * Kleine, dezente Statuszeile direkt unter dem Titel "Entropie Reduktor". Zeigt visuell ob
 * Drive-Backup gerade laeuft, fertig ist oder fehlgeschlagen ist — und seit wann. Frank-Wunsch
 * 2026-05-09: er will nach jedem neuen Eintrag sofort sehen "okay, das ist im Backup".
 */
@Composable
private fun BackupStatusBadge(syncStatus: SyncStatus, lastBackupAtMs: Long) {
    val cosmos = LocalCosmos.current
    val (icon, tint, label) =
        when (syncStatus) {
            SyncStatus.Idle ->
                Triple(
                    Icons.Outlined.CloudDone,
                    LocalCosmos.current.ok,
                    if (lastBackupAtMs > 0L) "Backup: ${formatBackupTime(lastBackupAtMs)}"
                    else "Backup eingerichtet",
                )
            SyncStatus.Pending ->
                Triple(
                    Icons.Outlined.CloudSync,
                    LocalCosmos.current.accentForscher,
                    "Aenderung erfasst — Backup startet gleich",
                )
            SyncStatus.Running ->
                Triple(Icons.Outlined.CloudSync, LocalCosmos.current.accent, "Backup laeuft …")
            is SyncStatus.Synced ->
                Triple(
                    Icons.Outlined.CloudDone,
                    LocalCosmos.current.ok,
                    "Im Backup gesichert: ${formatBackupTime(syncStatus.atEpochMs)}",
                )
            is SyncStatus.Failed ->
                Triple(Icons.Outlined.CloudOff, LocalCosmos.current.crit, "Backup fehlgeschlagen")
        }
    Row(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary)
    }
}

/**
 * Banner direkt unter dem Titel "Entropie Reduktor" wenn gerade alle offenen Aufgaben mit der
 * aktualisierten priorityScore-Doktrin neu bewertet werden (Frank-Wunsch 2026-05-09 — neue
 * 5-Farben-Skala basiert auf Entropie- Reduktion). Zeigt Fortschritt "X von Y", einen schmalen
 * Balken und am Ende "Fertig: X von Y neu bewertet" fuer 3 Sekunden bevor der Banner verschwindet.
 */
@Composable
private fun RescoreBanner(progress: RescoreProgress) {
    val cosmos = LocalCosmos.current
    val isFinished = progress.done + progress.failed >= progress.total
    val accent = if (isFinished) LocalCosmos.current.ok else LocalCosmos.current.accent
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            val label =
                when {
                    isFinished && progress.failed == 0 ->
                        "Aufgaben mit neuer Skala neu bewertet (${progress.done} von ${progress.total})"
                    isFinished && progress.failed > 0 ->
                        "Neu bewertet: ${progress.done} von ${progress.total} — ${progress.failed} fehlgeschlagen"
                    else ->
                        "Aufgaben werden mit neuer Skala neu bewertet … ${progress.done} von ${progress.total}"
                }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
        }
        Spacer(Modifier.height(4.dp))
        if (!isFinished && progress.total > 0) {
            LinearProgressIndicator(
                progress = {
                    (progress.done + progress.failed).toFloat() / progress.total.toFloat()
                },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = accent,
                trackColor = cosmos.glassBg,
            )
        }
    }
}

// ==================== KI-Vorschlaege Composables ====================

@Composable
private fun KiSuggestGenerateButton(
    state: KiTaskSuggestState,
    onClick: () -> Unit,
    onReset: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val isLoading = state == KiTaskSuggestState.LOADING
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val longPressJob = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            longPressJob.value = scope.launch {
                                kotlinx.coroutines.delay(2000L)
                                try {
                                    @Suppress("DEPRECATION")
                                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(500, 255))
                                } catch (_: Exception) { }
                                onReset()
                            }
                            tryAwaitRelease()
                            longPressJob.value?.cancel()
                            longPressJob.value = null
                        },
                        onTap = { onClick() },
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFBBF24),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (isLoading) "Generiere Vorschläge …" else "Aufgaben aus Ideen generieren",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isLoading) cosmos.textSecondary else cosmos.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFFFBBF24),
                )
            }
        }
    }
}

@Composable
private fun KiSuggestionCard(
    index: Int,
    suggestion: KiTaskSuggestion,
    isAccepting: Boolean,
    onAccept: () -> Unit,
    onDelete: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accentBlue = Color(0xFF60A5FA)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Nummerierter Kreis
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suggestion.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (suggestion.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = suggestion.description,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = cosmos.textSecondary,
                            lineHeight = 22.sp,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onAccept,
                        enabled = !isAccepting,
                    ) {
                        if (isAccepting) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = accentBlue,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = "Als Aufgabe übernehmen",
                                tint = accentBlue,
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Vorschlag löschen",
                            tint = Color(0xFFEF4444),
                        )
                    }
                }
            }
            // Quelle: "Erstellt aus Ideen" in der Reiterfarbe
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Erstellt aus Ideen",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = accentBlue,
            )
        }
    }
}

// Performance-Audit Loop 8 (2026-05-10): Top-level Liste statt .values()-Array
// pro Recomposition. TimeBucket wird in zwei verschiedenen Tab-Reihen iteriert.
private val ALL_TIME_BUCKETS: List<TimeBucket> = TimeBucket.entries.toList()

// Akkordeon-Sektions-Schluessel fuer Bloecke ohne TimeBucket (Frank-Wunsch 2026-05-24).
// Loop liegt zwischen SPAETER und ERLEDIGT (Frank-Wunsch 2026-05-24, Aufgabe 3).
private const val SECTION_LOOP = "LOOP"
private const val SECTION_ERLEDIGT = "ERLEDIGT"
private const val SECTION_KI_VORSCHLAEGE = "KI_VORSCHLAEGE"

// Performance-Audit Loop 5 (2026-05-10): SimpleDateFormat ist nicht thread-safe,
// aber teuer. ThreadLocal pro Pattern statt Allokation pro Aufruf.
private val BACKUP_TIME_TODAY_FMT: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
    SimpleDateFormat("HH:mm", Locale.GERMAN)
}
private val BACKUP_TIME_OLDER_FMT: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
    SimpleDateFormat("dd.MM. HH:mm", Locale.GERMAN)
}

/** Formatiert einen Epoch-ms-Zeitstempel in "HH:mm" (heute) oder "dd.MM. HH:mm" (sonst). */
private fun formatBackupTime(epochMs: Long): String {
    val now = System.currentTimeMillis()
    val today0 =
        Date(now).run {
            // 0:00 Lokalzeit
            java.util.Calendar.getInstance()
                .apply {
                    timeInMillis = now
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                .timeInMillis
        }
    val fmt =
        if (epochMs >= today0) BACKUP_TIME_TODAY_FMT.get()!! else BACKUP_TIME_OLDER_FMT.get()!!
    return fmt.format(Date(epochMs))
}

/**
 * Findet den Aufgabenblock der die Aufgabe enthaelt und berechnet ihren LazyColumn- Index
 * (Frank-Wunsch 2026-05-24, Akkordeon). Annahme: GENAU dieser Block ist aufgeklappt, alle anderen
 * Bloecke sind zugeklappt (= je 1 Header-Item) — der Aufrufer klappt den Block vorher auf
 * (expandedSection = bucketName). 0: briefing 1 + Position des Buckets in ALL_TIME_BUCKETS: dessen
 * Header danach: die Eintraege des aufgeklappten Blocks Liefert (Sektions-Schluessel, Item-Index)
 * oder null wenn die Aufgabe in keinem aktiven Bucket liegt.
 */
private fun computeTaskLocation(state: TasksUiState, taskId: String): Pair<String, Int>? {
    val isEmpty =
        state.entriesByBucket.values.all { it.isEmpty() } && state.resolvedEntries.isEmpty()
    if (isEmpty) return null
    for ((bucketPos, bucket) in ALL_TIME_BUCKETS.withIndex()) {
        val list = state.entriesByBucket[bucket].orEmpty()
        val pos = list.indexOfFirst { it.id == taskId }
        if (pos >= 0) {
            // briefing(1) + Header der vorherigen (zugeklappten) Bloecke (bucketPos)
            // + eigener Header(1) + Position in der Liste.
            return bucket.name to (1 + bucketPos + 1 + pos)
        }
    }
    return null
}

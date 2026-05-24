package de.frank.entropyreducer.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.data.settings.ThemeMode
import de.frank.entropyreducer.domain.usecase.SyncEntriesUseCase
import de.frank.entropyreducer.presentation.launch.LaunchScreen
import de.frank.entropyreducer.presentation.navigation.AppNavGraph
import de.frank.entropyreducer.presentation.theme.EntropieReductorTheme
import de.frank.entropyreducer.presentation.widget.WidgetDeepLink
import de.frank.entropyreducer.presentation.widget.WidgetDeepLinkBus
import de.frank.entropyreducer.presentation.widget.WidgetIntents
import de.frank.entropyreducer.workers.BackgroundScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Einzige Activity der App. Compose uebernimmt das gesamte Routing.
 * Beim ersten Start: Default-Prompts vorinstallieren, Theme aus AppSettings beobachten.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Frank-Wunsch 2026-05-11: Wenn die App vom Home-Screen-Widget aus
        // gestartet wurde, traegt das Intent eine Task-ID + Aktion. Wir
        // schreiben das in den WidgetDeepLinkBus — TasksScreen reagiert.
        handleWidgetIntent(intent)
        // Frank-Wunsch 2026-05-11 (dritte Iteration): Bei Widget-Tap soll der
        // LaunchScreen UEBERSPRUNGEN werden — Frank will direkt die Aufgabe
        // sehen, nicht erst auf "Start" tippen muessen. Initialer started-Wert
        // dafuer abgeleitet aus dem Intent.
        val widgetFastTrack = hasWidgetExtras(intent)
        setContent {
            // BootstrapViewModel: init {} legt beim ersten Start die Default-Prompts an.
            hiltViewModel<BootstrapViewModel>()
            // StartupViewModel: zieht beim App-Start einmalig den letzten Drive-Stand
            // und wirft ggf. einen Sync hinterher, damit Geraete-Wechsel sauber laufen.
            hiltViewModel<StartupViewModel>()

            // ThemeViewModel beobachtet die Theme-Einstellung — der ganze Compose-Tree
            // wird automatisch hell/dunkel gerendert wenn der Toggle umgeschaltet wird.
            val themeVm: ThemeViewModel = hiltViewModel()
            val themeMode by themeVm.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            EntropieReductorTheme(darkTheme = effectiveDark) {
                // Falls die App schon lief und nur via singleTop neu hereinkommt,
                // wird onNewIntent gefeuert — der Bus wird dort befuellt. Hier
                // braucht es nichts weiter.

                // Frank-Wunsch 2026-05-09: Beim App-Start zuerst ein
                // LaunchScreen mit dem Variant-Label (Debugversion oder
                // Performance Version) — Frank kann so beide parallel
                // installierten Builds sofort unterscheiden. rememberSaveable
                // ueberlebt Configuration-Changes wie Theme-Wechsel oder
                // Foldable-Klappung; nur ein echter App-Neustart bringt den
                // LaunchScreen zurueck.
                var started by rememberSaveable { mutableStateOf(widgetFastTrack) }
                // Wenn die App offen war und ein Widget-Tap onNewIntent triggert,
                // setzt der Bus-Collector started=true und der LaunchScreen
                // verschwindet sofort. Auch fuer den ersten Start mit Widget-
                // Intent (widgetFastTrack ist dann schon true).
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    de.frank.entropyreducer.presentation.widget.WidgetDeepLinkBus
                        .events.collect { link ->
                            if (link != null) started = true
                        }
                }
                if (started) {
                    AppNavGraph()
                } else {
                    LaunchScreen(onStart = { started = true })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Activity ist launchMode=standard (Default), aber Widget-Action-Intents
        // setzen FLAG_ACTIVITY_SINGLE_TOP — wenn die App schon laeuft landet der
        // Tap also hier statt onCreate. Bus befuellen und fertig.
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun hasWidgetExtras(intent: Intent?): Boolean {
        if (intent == null) return false
        return intent.hasExtra(WidgetIntents.EXTRA_ACTION)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        intent ?: return
        val taskId = intent.getStringExtra(WidgetIntents.EXTRA_TASK_ID) ?: return
        val action = intent.getStringExtra(WidgetIntents.EXTRA_ACTION)
            ?: WidgetIntents.ACTION_FOCUS
        WidgetDeepLinkBus.emit(WidgetDeepLink(taskId = taskId, action = action))
        // Extras NICHT aus dem Intent loeschen — wenn Activity-Recreate
        // passiert (z.B. Theme-Wechsel waehrend des Widget-Taps), wird
        // handleWidgetIntent noch einmal aufgerufen; der Bus deduppliziert
        // ueber den consumer-side clear() in TasksScreen.
    }
}

/**
 * Initialisiert beim ersten App-Start die drei Default-Prompts (Spec §6.4).
 */
@HiltViewModel
class BootstrapViewModel @Inject constructor(
    private val prompts: PromptRepository,
) : ViewModel() {
    init {
        viewModelScope.launch {
            if (prompts.count() == 0) {
                val now = System.currentTimeMillis()
                prompts.upsert(
                    SavedPromptEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Körper zuerst",
                        content = "Priorisiere Einträge der Kategorie KOERPERLICH grundsaetzlich am höchsten, weil koerperliche Verfassung Voraussetzung für jede andere Reduktion ist. Wenn ein koerperlicher Eintrag offen ist, darf kein anderer einen höheren Score bekommen.",
                        isActive = true,
                        createdAt = now, updatedAt = now,
                    )
                )
                prompts.upsert(
                    SavedPromptEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Schichtdienst-Logik",
                        content = "Frank arbeitet im Schichtsystem (4 Tagdienste, 4 frei, 4 Nachtdienste, 4 frei). Berücksichtige bei zeitlichen Aufgaben, ob ein Eintrag in einem Frei-Block schneller erledigt werden kann. Aufgaben, die einen Frei-Block brauchen, werden im Dienst niedriger priorisiert.",
                        isActive = true,
                        createdAt = now + 1, updatedAt = now + 1,
                    )
                )
                prompts.upsert(
                    SavedPromptEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Schnelle Siege bevorzugen",
                        content = "Wenn ein Eintrag in unter 10 Minuten erledigbar ist, gib ihm einen Bonus von +15 auf den Priority Score, weil schnelle Siege psychische Entropie sofort senken.",
                        isActive = true,
                        createdAt = now + 2, updatedAt = now + 2,
                    )
                )
            }
        }
    }
}

/**
 * Beobachtet den Theme-Modus aus den Settings und liefert ihn als StateFlow
 * an die Compose-Wurzel.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val settings: AppSettings,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settings.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun cycle(next: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(next) }
    }
}

/**
 * Wird beim ersten Compose der App-Wurzel erzeugt — laedt einmalig das letzte
 * Drive-Backup nach (falls verbunden), damit Geraete-Wechsel automatisch
 * synchronisiert werden. Nutzt einen Mutex auf Android-Ebene (statisch),
 * damit ein Activity-Recreate bei Theme-Wechsel nicht zwei Restores triggert.
 *
 * Performance-Warmup (Frank-Wunsch 2026-05-09): Beim Start werden die wichtigsten
 * Repositories einmal angefasst, damit Room die Datenbank initialisiert und die
 * ersten Queries gecacht sind. Tab-Switches sind dadurch sofort responsive — der
 * erste DB-Hit fuer jede Query passiert hier im Hintergrund, nicht beim Tab-Klick.
 */
@HiltViewModel
class StartupViewModel @Inject constructor(
    private val secrets: EncryptedSecretsStore,
    private val syncEntries: SyncEntriesUseCase,
    private val coordinator: SyncCoordinator,
    private val scheduler: BackgroundScheduler,
    private val oauth: OAuthService,
    private val entries: de.frank.entropyreducer.data.repository.EntryRepository,
    private val memories: de.frank.entropyreducer.data.repository.MemoryRepository,
    private val balanceBuckets: de.frank.entropyreducer.domain.usecase.BalanceBucketsUseCase,
    // Frank-Wunsch 2026-05-23: Beim App-Start sollen ALLE Tab-Bereiche schon im
    // Arbeitsspeicher liegen — auch die Biomarker-Datenquellen. Diese Repos werden
    // hier beim Start einmal angefasst, damit Room die Tabellen laedt und die Daten
    // im Speicher gecacht sind, bevor Frank den Biomarker-Tab oeffnet.
    private val whoop: de.frank.entropyreducer.data.repository.WhoopRepository,
    private val oura: de.frank.entropyreducer.data.repository.OuraRepository,
    private val amazfit: de.frank.entropyreducer.data.repository.AmazfitRepository,
    // Frank-Wunsch 2026-05-23: Kalender-Sync laeuft jetzt direkt beim frischen Start
    // (nach dem Drive-Backup), nicht mehr ueber den naechtlichen Worker.
    private val calendar: de.frank.entropyreducer.data.repository.CalendarRepository,
    // Frank-Wunsch 2026-05-23: HealthConnect-Werte beim frischen Start in die DB cachen,
    // damit der Biomarker-Tab sie ohne eigenen Live-Read anzeigen kann.
    private val healthConnectRepository:
        de.frank.entropyreducer.data.repository.HealthConnectRepository,
    // Frank-Bugfix 2026-05-23: fuer den "Zuletzt synchronisiert"-Footer beim frischen Start.
    private val appSettings: de.frank.entropyreducer.data.settings.AppSettings,
    // Frank-Wunsch 2026-05-24: Tagebuch-Bruecke — Eintraege aus BestJournal Frank spiegeln.
    private val journalMirror: de.frank.entropyreducer.data.repository.JournalMirrorRepository,
) : ViewModel() {
    init {
        if (!startupRanThisProcess) {
            startupRanThisProcess = true
            // Frank-Wunsch 2026-05-24: Tagebuch-Bruecke. Beim frischen App-Start die
            // Eintraege aus BestJournal Frank spiegeln (read-only, volles Abbild).
            // Eigener Launch-Block (Dispatchers.IO), laeuft parallel zu den anderen
            // Start-Tasks und blockiert die UI nicht.
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { journalMirror.sync() }
                    .onFailure {
                        android.util.Log.w("StartupViewModel", "Journal-Sync fehlgeschlagen", it)
                    }
            }
            // Performance-Warmup: parallel zur Drive-Restore-Logik laeuft das
            // Repository-Warming. So sind beim ersten Tab-Klick alle DB-Queries
            // schon gecacht (Frank-Wunsch 2026-05-09 Performance).
            //
            // Performance-Fix Loop 2.3: Beide launch-Bloecke explizit auf
            // Dispatchers.IO. Vorher lief das auf viewModelScope-Default
            // (Main.immediate). Innen werden EncryptedSharedPreferences gelesen
            // (secrets.driveBackupEnabled, .driveAccountEmail, .calendarAccountEmail,
            // oauth.loadWhoopAuthState), Room-Flows gesammelt (.first()) und
            // mehrere WorkManager.enqueueUniquePeriodicWork-Setups gemacht — bei
            // einem kalten App-Start ist das in Summe 50-200 ms Main-Thread-Last,
            // sichtbar als Time-To-First-Frame-Verzoegerung.
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    // Eine .first() pro Repository = ein DB-Query, danach hat Room
                    // die Tabellen geladen und Connection-Pool ist warm.
                    entries.getActive().first()
                    memories.getActive().first()
                    // Balance-Buckets gleich beim Start ausfuehren — damit beim ersten
                    // Oeffnen des Aufgaben-Tabs schon die korrekte 5/5/10/Rest-Verteilung
                    // steht und nicht erst durch das ViewModel-Init geladen werden muss.
                    balanceBuckets()
                }
            }
            // Biomarker-Bereich beim Start vorladen (Frank-Wunsch 2026-05-23): Whoop-,
            // Oura- und Amazfit-Daten einmal lesen, damit Room die Tabellen in den
            // Speicher laedt und der Biomarker-Tab beim ersten Oeffnen sofort Daten
            // hat — kein Nachladen beim Reinscrollen. Eigener Launch-Block, damit es
            // parallel zum Aufgaben-Warmup laeuft (Dispatchers.IO, off-main).
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    whoop.observeAll().first()
                    whoop.observeWorkouts().first()
                    oura.observeReadiness().first()
                    oura.observeDailySleep().first()
                    oura.observeActivity().first()
                    oura.observeResilience().first()
                    oura.observeSleepDetails().first()
                    amazfit.observeAllDaily().first()
                    amazfit.observeAllWorkouts().first()
                }
            }
            viewModelScope.launch(Dispatchers.IO) {
                // Frank-Wunsch 2026-05-23: ZENTRALER Start-Sync-Ablauf. Alle Daten-APIs
                // synchronisieren NUR beim frischen App-Start (hier, einmal pro Prozess) —
                // nicht mehr beim Zurueckholen aus dem Hintergrund und nicht beim Oeffnen
                // des Biomarker-Tabs.
                //
                // Reihenfolge bewusst: ZUERST Google Drive komplett (1. Restore holt die
                // gesicherte Historie zurueck, 2. Backup sichert neue lokale Aenderungen) —
                // beides wird ABGEWARTET. ERST DANN die einzelnen Daten-APIs. So ist die
                // Historie wiederhergestellt/gesichert bevor neue Daten oben drauf kommen.
                if (secrets.driveBackupEnabled && secrets.driveAccountEmail != null) {
                    runCatching { syncEntries.restoreFromDrive() }
                    // syncNowAndWait() WARTET bis das komplette Backup (Haupt + Workouts +
                    // Health) durch ist — anders als das fruehere fire-and-forget requestSync().
                    runCatching { coordinator.syncNowAndWait() }
                }

                // Frank-Wunsch 2026-05-23: Reihenfolge nach dem Drive-Backup bewusst gewaehlt —
                // 1. Health Connect: lokal, schnell, kein Netz/Rate-Limit -> Gewicht sofort da.
                // 2. + 3. Whoop und Oura: die zentralen Biomarker (Erholung, Schlaf).
                // 4. Strava: oft rate-limitiert, darum nicht blockierend vorne.
                // 5. Kalender: laedt am meisten (Jahre im Voraus), am wenigsten zeitkritisch.
                // Jeweils nur wenn verbunden; Strava prueft die Auth intern selbst. Die Zaehler
                // werden fuer den Footer gesammelt.
                // Performance 2026-05-23 (Vorschlag 3, vom Benutzer freigegeben): Die einzelnen
                // Daten-APIs sind voneinander unabhaengig (verschiedene Hosts, je eigenes
                // Rate-Limit, schreiben in verschiedene Tabellen) und liefen bisher streng
                // nacheinander. Sie laufen jetzt PARALLEL (async/coroutineScope) — der Start-Sync
                // dauert damit nur noch so lange wie der LANGSAMSTE Einzel-Sync statt deren Summe.
                //
                // Bewusst NICHT veraendert: Das Drive-Restore+Backup oben bleibt davor UND
                // blockierend — die Historie muss wiederhergestellt/gesichert sein BEVOR neue
                // Daten dazukommen (das war der einzige Reihenfolge-Zwang). Strava bringt seinen
                // eigenen Rate-Limit-Cooldown mit und ist daher gefahrlos parallel. Jeder Block
                // behaelt seine eigene Fehlerbehandlung (runCatching/getOrDefault) — ein
                // Einzel-Fehler reisst die anderen nicht mit.
                var hcCount = 0
                var whoopCount = 0
                var ouraCount = 0
                var stravaCount = 0
                coroutineScope {
                    val hcDeferred =
                        async { runCatching { healthConnectRepository.syncToCache() }.getOrDefault(0) }
                    val whoopDeferred =
                        async {
                            if (oauth.loadWhoopAuthState().isAuthorized) {
                                whoop.syncLastDays(365).getOrDefault(0)
                            } else {
                                0
                            }
                        }
                    val ouraDeferred =
                        async {
                            if (oura.isTokenConfigured()) {
                                oura.syncLastDays(365).getOrNull()?.values?.sum() ?: 0
                            } else {
                                0
                            }
                        }
                    val stravaDeferred =
                        async {
                            runCatching { amazfit.mergeFromStrava(days = 30) }
                                .getOrDefault(-1)
                                .coerceAtLeast(0)
                        }
                    val calendarDeferred =
                        async {
                            if (secrets.calendarAccountEmail != null) {
                                runCatching { calendar.syncDefaultWindow() }
                            }
                        }
                    hcCount = hcDeferred.await()
                    whoopCount = whoopDeferred.await()
                    ouraCount = ouraDeferred.await()
                    stravaCount = stravaDeferred.await()
                    calendarDeferred.await()
                }

                // Frank-Bugfix 2026-05-23: Footer-Zeitstempel setzen — genau wie der manuelle
                // Aktualisieren-Knopf (refreshNow). Behebt die Regression aus #987, wo der
                // frische Start die Syncs zwar ausfuehrte, aber den "Zuletzt synchronisiert"-
                // Footer nicht aktualisierte (er blieb auf dem alten Stand stehen).
                val nowZ = java.time.ZonedDateTime.now()
                val footerTs =
                    "%02d.%02d. %02d:%02d".format(
                        nowZ.dayOfMonth,
                        nowZ.monthValue,
                        nowZ.hour,
                        nowZ.minute,
                    )
                val footer =
                    "✓ $footerTs · Whoop $whoopCount · Strava $stravaCount · " +
                        "Oura $ouraCount · Health Connect $hcCount"
                runCatching { appSettings.setLastRefreshFooter(footer, System.currentTimeMillis()) }

                // Hintergrund-Jobs aufsetzen. ensureNightlyJobs() bestellt die alten
                // naechtlichen Whoop-/Kalender-Jobs jetzt AB (Frank-Wunsch 2026-05-23:
                // kein automatischer Hintergrund-Sync mehr). Die KI-Worker (Briefing,
                // Codex, Review, Trigger) bleiben unveraendert.
                scheduler.ensureNightlyJobs()
                scheduler.ensureKiQuestionJob()
                scheduler.ensureCodexJob()
                scheduler.ensureDailyBriefingJob()
                scheduler.ensureReviewJobs()
                scheduler.ensureCorrelationAndTriggerJobs()
                scheduler.runKiQuestionCheckNow()
            }
        }
    }

    companion object {
        @Volatile
        private var startupRanThisProcess: Boolean = false
    }
}

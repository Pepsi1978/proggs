package de.frank.entropyreducer.presentation

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
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
import de.frank.entropyreducer.data.tts.TtsUsageBackup
import de.frank.entropyreducer.domain.usecase.SyncEntriesUseCase
import de.frank.entropyreducer.di.ApplicationScope
import de.frank.entropyreducer.presentation.launch.LaunchScreen
import de.frank.entropyreducer.presentation.navigation.AppNavGraph
import de.frank.entropyreducer.presentation.theme.EntropieReductorTheme
import de.frank.entropyreducer.presentation.widget.WidgetDeepLink
import de.frank.entropyreducer.presentation.widget.WidgetDeepLinkBus
import de.frank.entropyreducer.presentation.widget.WidgetIntents
import de.frank.entropyreducer.workers.BackgroundScheduler
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Einzige Activity der App. Compose uebernimmt das gesamte Routing.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var usageBackup: TtsUsageBackup

    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var secrets: EncryptedSecretsStore

    @Inject
    lateinit var secondBrainVpnConnector: de.frank.entropyreducer.data.remote.brain.SecondBrainVpnConnector

    @Inject
    lateinit var secondBrainIdeaConnector: de.frank.entropyreducer.data.remote.brain.SecondBrainIdeaConnector

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                lifecycleScope.launch(Dispatchers.IO) { secondBrainVpnConnector.connect() }
            } else {
                secondBrainVpnConnector.reportConsentDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Frank-Wunsch 2026-07-03: TTS-Zaehlerstand aus dem Drive-Backup wiederherstellen (hebt den
        // lokalen Wert an, wenn der Drive-Wert groesser ist) — ueberlebt eine Neuinstallation.
        // Fehler werden in restoreOnStart() geschluckt; blockiert den App-Start nie.
        lifecycleScope.launch { usageBackup.restoreOnStart() }
        enableEdgeToEdge()
        handleWidgetIntent(intent)
        val widgetFastTrack = hasWidgetExtras(intent)
        setContent {
            hiltViewModel<BootstrapViewModel>()
            hiltViewModel<StartupViewModel>()

            val themeVm: ThemeViewModel = hiltViewModel()
            val themeMode by themeVm.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            EntropieReductorTheme(darkTheme = effectiveDark) {
                var started by rememberSaveable { mutableStateOf(widgetFastTrack) }
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    WidgetDeepLinkBus.events.collect { link ->
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
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        maybeStartSecondBrainTunnel()
        // Etappe 2 (Brain->App, Frank-Wunsch 2026-07-04): im Second Brain neu angelegte Ideen in die
        // App holen. Mit Retry (der WireGuard-Tunnel braucht nach dem Vordergrund-Wechsel kurz);
        // prueft intern Key + aktivierten Ideen-Schalter.
        secondBrainIdeaConnector.pullFromBrain(lifecycleScope)
        // Fix 2026-07-04: Ausstehende Ideen-Uploads aktiv nachschieben. Eine Idee, deren erster
        // Upload unterwegs (ohne Netz) fehlschlug, wird so beim naechsten App-Vordergrund mit
        // Internet automatisch hochgeladen — statt am Flow-Observer liegen zu bleiben (mit Retry
        // gegen den nach dem Vordergrund-Wechsel noch nicht ganz oben stehenden WireGuard-Tunnel).
        secondBrainIdeaConnector.retryPendingUploads(lifecycleScope)
    }

    override fun onStop() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                secondBrainIdeaConnector.retryPendingUploadsNow()
            } finally {
                // Fix 2026-07-14 (#12): lifecycle.currentState ist main-thread-only → auf dem
                // Main-Dispatcher lesen (unmittelbar vor disconnect(), damit die Race klein bleibt).
                val stillBackground = withContext(Dispatchers.Main) {
                    !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
                }
                if (stillBackground) {
                    secondBrainVpnConnector.disconnect()
                }
            }
        }
        super.onStop()
    }

    private fun maybeStartSecondBrainTunnel() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Frank-Wunsch 2026-07-04: Tunnel kommt — genau wie in CortexAndroid — allein durch eine
            // vorhandene WireGuard-Config hoch. Der "Second Brain aktiviert"-Toggle ist KEINE
            // Vorbedingung mehr; die Existenz der Config signalisiert den Verbindungswunsch (Cortex
            // prueft in onStart ebenso nur, ob eine Config vorhanden ist).
            val hasConfig = !secrets.secondBrainWireGuardConfig.isNullOrBlank()
            if (!hasConfig) return@launch
            if (!secondBrainVpnConnector.loadSavedConfig()) return@launch
            val permissionIntent = VpnService.prepare(this@MainActivity)
            if (permissionIntent != null) {
                launch(Dispatchers.Main) { vpnPermissionLauncher.launch(permissionIntent) }
            } else {
                secondBrainVpnConnector.connect()
            }
        }
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
    }
}

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
                        name = "Koerper zuerst",
                        content = "Priorisiere Eintraege der Kategorie KOERPERLICH grundsaetzlich am hoechsten.",
                        isActive = true,
                        createdAt = now, updatedAt = now,
                    )
                )
                prompts.upsert(
                    SavedPromptEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Schichtdienst-Logik",
                        content = "Frank arbeitet im Schichtsystem (4 Tagdienste, 4 frei, 4 Nachtdienste, 4 frei).",
                        isActive = true,
                        createdAt = now + 1, updatedAt = now + 1,
                    )
                )
                prompts.upsert(
                    SavedPromptEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Schnelle Siege bevorzugen",
                        content = "Wenn ein Eintrag in unter 10 Minuten erledigbar ist, gib ihm einen Bonus von +15 auf den Priority Score.",
                        isActive = true,
                        createdAt = now + 2, updatedAt = now + 2,
                    )
                )
            }
        }
    }
}

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
 * Drive-Backup nach und warmt Repositories auf.
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
    private val whoop: de.frank.entropyreducer.data.repository.WhoopRepository,
    private val oura: de.frank.entropyreducer.data.repository.OuraRepository,
    private val amazfit: de.frank.entropyreducer.data.repository.AmazfitRepository,
    private val calendar: de.frank.entropyreducer.data.repository.CalendarRepository,
    private val healthConnectRepository: de.frank.entropyreducer.data.repository.HealthConnectRepository,
    private val appSettings: de.frank.entropyreducer.data.settings.AppSettings,
    private val journalMirror: de.frank.entropyreducer.data.repository.JournalMirrorRepository,
    private val foregroundSync: de.frank.entropyreducer.domain.usecase.ForegroundSyncManager,
    private val secondBrainIdeaConnector: de.frank.entropyreducer.data.remote.brain.SecondBrainIdeaConnector,
) : ViewModel() {
    init {
        secondBrainIdeaConnector.start(viewModelScope)
        if (!startupRanThisProcess) {
            startupRanThisProcess = true
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { journalMirror.sync() }
                    .onFailure {
                        de.frank.entropyreducer.data.diagnostics.Diag.w(
                            de.frank.entropyreducer.data.diagnostics.DiagnosticArea.APP,
                            "StartupViewModel", "Journal-Sync fehlgeschlagen", it
                        )
                    }
            }
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    entries.getActive().first()
                    memories.getActive().first()
                    balanceBuckets()
                }
            }
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
                // Kaltstart: Drive sofort (1:1-Multi-Device-Sync), Fitness-APIs nur nach 8h.
                runCatching { foregroundSync.syncForeground() }
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

package de.frank.entropyreducer.presentation

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
import de.frank.entropyreducer.workers.BackgroundScheduler
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
                // Frank-Wunsch 2026-05-09: Beim App-Start zuerst ein
                // LaunchScreen mit dem Variant-Label (Debugversion oder
                // Performance Version) — Frank kann so beide parallel
                // installierten Builds sofort unterscheiden. rememberSaveable
                // ueberlebt Configuration-Changes wie Theme-Wechsel oder
                // Foldable-Klappung; nur ein echter App-Neustart bringt den
                // LaunchScreen zurueck.
                var started by rememberSaveable { mutableStateOf(false) }
                if (started) {
                    AppNavGraph()
                } else {
                    LaunchScreen(onStart = { started = true })
                }
            }
        }
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
) : ViewModel() {
    init {
        if (!startupRanThisProcess) {
            startupRanThisProcess = true
            // Performance-Warmup: parallel zur Drive-Restore-Logik laeuft das
            // Repository-Warming. So sind beim ersten Tab-Klick alle DB-Queries
            // schon gecacht (Frank-Wunsch 2026-05-09 Performance).
            viewModelScope.launch {
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
            viewModelScope.launch {
                if (secrets.driveBackupEnabled && secrets.driveAccountEmail != null) {
                    runCatching { syncEntries.restoreFromDrive() }
                    // Nach dem Restore noch ein Backup, damit lokale Aenderungen,
                    // die ggf. waehrend Offline-Phase entstanden, hochgeladen werden.
                    coordinator.requestSync()
                }
                // Stufe 2: Hintergrund-Sync-Plaene aufsetzen — auch wenn keine Tokens
                // existieren. Die Worker laufen leer, machen aber kein Schaden.
                scheduler.ensureNightlyJobs()
                scheduler.ensureKiQuestionJob()
                scheduler.ensureCodexJob()
                scheduler.ensureDailyBriefingJob()
                scheduler.ensureReviewJobs()
                scheduler.ensureCorrelationAndTriggerJobs()
                if (secrets.calendarAccountEmail != null) {
                    scheduler.runCalendarSyncNow()
                }
                if (oauth.loadWhoopAuthState().isAuthorized) {
                    scheduler.runWhoopSyncNow()
                }
                scheduler.runKiQuestionCheckNow()
            }
        }
    }

    companion object {
        @Volatile
        private var startupRanThisProcess: Boolean = false
    }
}

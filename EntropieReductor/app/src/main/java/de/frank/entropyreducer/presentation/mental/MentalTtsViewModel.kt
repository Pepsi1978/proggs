package de.frank.entropyreducer.presentation.mental

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.tts.TtsUsage
import de.frank.entropyreducer.data.tts.TtsUsageStore
import de.frank.entropyreducer.di.ApplicationScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Vorlese-System fuer das Mentalboard (Frank-Wunsch 2026-06-16).
 *
 * Muster (Anker-Schema): Satz 1 ist der "Anker". Vorgelesen wird paarweise — vor JEDEM Folgesatz
 * zuerst der Anker (so oft wie [MentalTtsUiState.ankerCount]), danach der Folgesatz (so oft wie
 * [MentalTtsUiState.folgeCount]). Beispiel 4 Saetze, anker=2, folge=1:
 *   1,1,2 · 1,1,3 · 1,1,4
 * Zwischen JEDEM gesprochenen Satz liegen [PAUSE_MS] (9 Sekunden). Die Reihenfolge ergibt sich
 * direkt aus der aktuellen Sortierung des Mentalboards — sortiert Frank um, aendert sich die
 * Vorlese-Reihenfolge automatisch mit (die Sequenz wird bei jedem Start frisch gebildet).
 *
 * Der Durchgang wird über den dauerhaften Play/Pause/Stop-Player im Mentalboard gesteuert und endet
 * nach der gewählten Sequenz oder spätestens an der globalen Sicherheitsgrenze.
 *
 * Gewohnheiten mitlesen: Ist [MentalTtsUiState.includeHabits]
 * aktiv, werden die Gewohnheiten NACH dem Mentalblock angehaengt — in der Vorleseweise des
 * Gewohnheit-Reiters (jeder Satz [gewohnheitRepeat]-mal, aus dem gemeinsamen gewohnheit_tts_settings-
 * Store). Der Mental-Loop wiederholt dann Mentalblock UND Gewohnheiten gemeinsam.
 *
 * Kosten/Netz: Jede Sequenzposition wird frisch ueber Google-TTS synthetisiert. Dadurch bekommt
 * auch jede Wiederholung desselben Satzes eine eigene Betonung statt exakt dieselbe MP3-Datei.
 * Beim Stop wird der Sequenz-Cache wieder aufgeraeumt.
 *
 * Threading (Bug-Almanach media3 T1/L1): Der MediaPlayer wird ausschliesslich aus
 * withContext(Dispatchers.Main) bedient; Freigabe deterministisch in [stop]/[onCleared].
 */

/* Eigener kleiner DataStore nur fuer die Vorlese-Einstellungen (anker/folge/Gewohnheiten). Bewusst
 * getrennt vom "mental_board"-Store (Saetze) — andere Datei, kein Konflikt. */
internal val Context.mentalTtsStore by preferencesDataStore(name = "mental_tts_settings")
private val KEY_ANKER = intPreferencesKey("anker_count")
private val KEY_FOLGE = intPreferencesKey("folge_count")
private val KEY_INCLUDE_HABITS = booleanPreferencesKey("include_habits")
private val KEY_MENTAL_PAUSE_SECONDS = intPreferencesKey("pause_seconds")
private val KEY_RANDOM_PLAYBACK = booleanPreferencesKey("random_playback")

data class MentalTtsUiState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    /** Wie oft der Anker (Satz 1) vor jedem Folgesatz vorgelesen wird (1..10). */
    val ankerCount: Int = 1,
    /** Wie oft jeder Folgesatz vorgelesen wird (1..10). */
    val folgeCount: Int = 1,
    /** Gewohnheiten am Ende mitlesen? */
    val includeHabits: Boolean = false,
    /** Zufällige Satzreihenfolge: jeden Satzblock einmal pro Durchlauf, Wiederholungen bleiben direkt zusammen. */
    val randomPlayback: Boolean = false,
    /** Pause zwischen zwei gesprochenen Mental-Saetzen (1..30 Sekunden). */
    val pauseSeconds: Int = 9,
    /** Globale Sicherheitsgrenze fuer automatischen Vorlese-Stop (15..120 Minuten). */
    val autoStopMinutes: Int = AppSettings.DEFAULT_TTS_AUTO_STOP_MINUTES,
    val autoStopEndsAtWallClockMs: Long? = null,
    val autoStopRemainingMs: Long? = null,
    val error: String? = null,
)

/** Interne Momentaufnahme der vier persistierten Vorlese-Einstellungen. */
private data class MentalSettings(
    val anker: Int,
    val folge: Int,
    val includeHabits: Boolean,
    val randomPlayback: Boolean,
    val pauseSeconds: Int,
)

@HiltViewModel
class MentalTtsViewModel
@Inject
    constructor(
        application: Application,
        private val appSettings: AppSettings,
        private val playback: MentalTtsPlaybackController,
        usageStore: TtsUsageStore,
        @ApplicationScope private val applicationScope: CoroutineScope,
    ) : AndroidViewModel(application) {

    /** Live-Monatsverbrauch des TTS-Kontingents (Anzeige unter dem letzten Satz im Mentalboard). */
    val usage: StateFlow<TtsUsage> = usageStore.usage

    private companion object {
        const val TAG = "MentalTts"
        const val DEFAULT_PAUSE_SECONDS = 9
        const val RANGE_MIN = 1
        const val RANGE_MAX = 10
        const val PAUSE_RANGE_MIN = 1
        const val PAUSE_RANGE_MAX = 30
    }

    private val ctx: Context
        get() = getApplication()

    private val settingsFlow: Flow<MentalSettings> =
        ctx.mentalTtsStore.data.map { p ->
            MentalSettings(
                anker = (p[KEY_ANKER] ?: 1).coerceIn(RANGE_MIN, RANGE_MAX),
                folge = (p[KEY_FOLGE] ?: 1).coerceIn(RANGE_MIN, RANGE_MAX),
                includeHabits = p[KEY_INCLUDE_HABITS] ?: false,
                randomPlayback = p[KEY_RANDOM_PLAYBACK] ?: false,
                pauseSeconds = (p[KEY_MENTAL_PAUSE_SECONDS] ?: DEFAULT_PAUSE_SECONDS)
                    .coerceIn(PAUSE_RANGE_MIN, PAUSE_RANGE_MAX),
            )
        }

    private val playbackState =
        combine(
            playback.isPlayingFlow,
            playback.isPausedFlow,
            playback.autoStopEndsAtWallClockMsFlow,
            playback.autoStopRemainingMsFlow,
            playback.playbackLabelFlow,
        ) { playing, paused, autoStopEndsAt, autoStopRemainingMs, label ->
            // Der obere Mental-Player UND die obere Zeitleiste reagieren NUR auf den echten
            // Mental-Lauf — nicht auf den Spezial- oder reinen Gewohnheits-Lauf (Frank-Wunsch
            // 2026-07-15: der untere Spezial-Player ist voellig unabhaengig von den oberen Mentals).
            val isMental = playing && label == "Mental"
            PlaybackState(
                isPlaying = isMental,
                isPaused = isMental && paused,
                autoStopEndsAtWallClockMs = if (isMental) autoStopEndsAt else null,
                autoStopRemainingMs = if (isMental) autoStopRemainingMs else null,
            )
        }

    val uiState: StateFlow<MentalTtsUiState> =
        combine(
            settingsFlow,
            appSettings.ttsAutoStopMinutesFlow,
            playbackState,
            playback.errorFlow,
        ) { s, autoStopMinutes, playbackState, err ->
                MentalTtsUiState(
                    isPlaying = playbackState.isPlaying,
                    isPaused = playbackState.isPaused,
                    ankerCount = s.anker,
                    folgeCount = s.folge,
                    includeHabits = s.includeHabits,
                    randomPlayback = s.randomPlayback,
                    pauseSeconds = s.pauseSeconds,
                    autoStopMinutes = autoStopMinutes,
                    autoStopEndsAtWallClockMs = playbackState.autoStopEndsAtWallClockMs,
                    autoStopRemainingMs = playbackState.autoStopRemainingMs,
                    error = err,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MentalTtsUiState(),
            )

    private data class PlaybackState(
        val isPlaying: Boolean,
        val isPaused: Boolean,
        val autoStopEndsAtWallClockMs: Long?,
        val autoStopRemainingMs: Long?,
    )

    /* ----------------------------- Einstellungen ----------------------------- */

    fun setAnkerCount(n: Int) {
        viewModelScope.launch {
            ctx.mentalTtsStore.edit { it[KEY_ANKER] = n.coerceIn(RANGE_MIN, RANGE_MAX) }
        }
    }

    fun setFolgeCount(n: Int) {
        viewModelScope.launch {
            ctx.mentalTtsStore.edit { it[KEY_FOLGE] = n.coerceIn(RANGE_MIN, RANGE_MAX) }
        }
    }

    /** Gewohnheiten am Ende mitlesen. */
    fun setIncludeHabits(enabled: Boolean) {
        viewModelScope.launch { ctx.mentalTtsStore.edit { it[KEY_INCLUDE_HABITS] = enabled } }
    }

    fun setRandomPlayback(enabled: Boolean) {
        viewModelScope.launch { ctx.mentalTtsStore.edit { it[KEY_RANDOM_PLAYBACK] = enabled } }
    }

    fun setPauseSeconds(seconds: Int) {
        viewModelScope.launch {
            ctx.mentalTtsStore.edit {
                it[KEY_MENTAL_PAUSE_SECONDS] = seconds.coerceIn(PAUSE_RANGE_MIN, PAUSE_RANGE_MAX)
            }
        }
    }

    fun setAutoStopMinutes(minutes: Int) {
        // Dieser globale Wert muss Navigation weg vom Settings-Screen überleben.
        applicationScope.launch { appSettings.setTtsAutoStopMinutes(minutes) }
    }

    fun dismissError() {
        playback.dismissError()
    }

    /* ----------------------------- Steuerung ----------------------------- */

    /** Startet die aktuelle Mental-Liste; aktivierte Gewohnheiten werden am Ende mitgelesen. */
    fun play(mentals: List<Mental>, gewohnheiten: List<Mental> = emptyList()) {
        playback.startMentalPlayback(mentals, gewohnheiten)
    }

    fun stop() {
        playback.stop()
    }

    fun pause() {
        playback.pause()
    }

    fun resume() {
        playback.resume()
    }

    override fun onCleared() {
        super.onCleared()
        // Playback lebt im ApplicationScope weiter. Stop nur per Lautsprecher oder Timeout.
    }
}

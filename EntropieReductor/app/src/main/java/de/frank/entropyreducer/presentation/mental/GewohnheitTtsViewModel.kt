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
 * Vorlese-System fuer den Gewohnheitsreiter (einfacher als MentalTtsViewModel):
 * - Jeder Satz wird [repeatCount]-mal vorgelesen.
 * - 9 Sekunden Pause zwischen jedem gesprochenen Satz.
 * - Jede Wiederholung wird frisch ueber TTS synthetisiert, damit sie nie exakt gleich klingt.
 * - Endlosschleife bis zur globalen Sicherheitsgrenze aus den Vorlesen-Einstellungen.
 * - Nur die Gewohnheiten UEBER dem Separator werden vorgelesen.
 */

// internal (nicht private): das Mental-Vorlesen mit aktivem "G"-Haekchen liest die
// "wie-oft"-Zahl der Gewohnheiten aus DIESEM Store (gleicher DataStore, keine zweite Instanz
// auf dieselbe Datei — das wuerde crashen). Frank-Wunsch 2026-07-03.
internal val Context.gewohnheitTtsStore by preferencesDataStore(name = "gewohnheit_tts_settings")
internal val KEY_REPEAT = intPreferencesKey("repeat_count")
internal val KEY_GEWOHNHEIT_PAUSE_SECONDS = intPreferencesKey("pause_seconds")
private val KEY_LOOP = booleanPreferencesKey("loop_enabled")

data class GewohnheitTtsUiState(
    val isPlaying: Boolean = false,
    /** Wie oft jeder Satz vorgelesen wird (1..10). */
    val repeatCount: Int = 1,
    /** Endlosschleife aktiv? */
    val loop: Boolean = false,
    /** Pause zwischen zwei gesprochenen Gewohnheits-Saetzen (1..30 Sekunden). */
    val pauseSeconds: Int = 9,
    /** Globale Sicherheitsgrenze fuer automatischen Vorlese-Stop (15..120 Minuten). */
    val autoStopMinutes: Int = AppSettings.DEFAULT_TTS_AUTO_STOP_MINUTES,
    val error: String? = null,
)

private data class GewohnheitTtsSettings(
    val repeatCount: Int,
    val loop: Boolean,
    val pauseSeconds: Int,
)

@HiltViewModel
class GewohnheitTtsViewModel
@Inject
    constructor(
        application: Application,
        private val appSettings: AppSettings,
        private val playback: MentalTtsPlaybackController,
        @ApplicationScope private val applicationScope: CoroutineScope,
    ) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "GewohnheitTts"
        const val DEFAULT_PAUSE_SECONDS = 9
        const val RANGE_MIN = 1
        const val RANGE_MAX = 10
        const val PAUSE_RANGE_MIN = 1
        const val PAUSE_RANGE_MAX = 30
    }

    private val ctx: Context
        get() = getApplication()

    private val settingsFlow: Flow<GewohnheitTtsSettings> =
        ctx.gewohnheitTtsStore.data.map { p ->
            GewohnheitTtsSettings(
                repeatCount = (p[KEY_REPEAT] ?: 1).coerceIn(RANGE_MIN, RANGE_MAX),
                loop = p[KEY_LOOP] ?: false,
                pauseSeconds = (p[KEY_GEWOHNHEIT_PAUSE_SECONDS] ?: DEFAULT_PAUSE_SECONDS)
                    .coerceIn(PAUSE_RANGE_MIN, PAUSE_RANGE_MAX),
            )
        }

    val uiState: StateFlow<GewohnheitTtsUiState> =
        combine(settingsFlow, appSettings.ttsAutoStopMinutesFlow, playback.isPlayingFlow, playback.errorFlow) { settings, autoStopMinutes, playing, err ->
                GewohnheitTtsUiState(
                    isPlaying = playing,
                    repeatCount = settings.repeatCount,
                    loop = settings.loop,
                    pauseSeconds = settings.pauseSeconds,
                    autoStopMinutes = autoStopMinutes,
                    error = err,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = GewohnheitTtsUiState(),
            )

    fun setRepeatCount(n: Int) {
        viewModelScope.launch {
            ctx.gewohnheitTtsStore.edit { it[KEY_REPEAT] = n.coerceIn(RANGE_MIN, RANGE_MAX) }
        }
    }

    fun setLoop(enabled: Boolean) {
        viewModelScope.launch { ctx.gewohnheitTtsStore.edit { it[KEY_LOOP] = enabled } }
    }

    fun setPauseSeconds(seconds: Int) {
        viewModelScope.launch {
            ctx.gewohnheitTtsStore.edit {
                it[KEY_GEWOHNHEIT_PAUSE_SECONDS] = seconds.coerceIn(PAUSE_RANGE_MIN, PAUSE_RANGE_MAX)
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

    fun togglePlayback(gewohnheiten: List<Mental>) {
        playback.toggleGewohnheitPlayback(gewohnheiten)
    }

    fun stop() {
        playback.stop()
    }

    override fun onCleared() {
        super.onCleared()
        // Playback lebt im ApplicationScope weiter. Stop nur per Lautsprecher oder Timeout.
    }
}

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Vorlese-System fuer die SPEZIELLEN Mental-Saetze (Frank-Wunsch 2026-07-15).
 *
 * Eigener, vom normalen Mentalboard unabhaengiger Durchlauf mit eigenem Player (Play/Pause/Stop),
 * eigener Pause zwischen den Saetzen, eigener Abschaltzeit und optionaler Zufallsreihenfolge. Nur
 * AKTIVIERTE Saetze werden vorgelesen; jeder Satz wird pro Durchlauf frisch synthetisiert, damit
 * keine zwei Wiederholungen exakt gleich betont klingen. Der Lauf laeuft in Endlosschleife bis zum
 * Stop-Button oder bis die eingestellte Abschaltzeit erreicht ist.
 *
 * Teilt sich den Singleton-[MentalTtsPlaybackController] mit dem normalen Mental-/Gewohnheits-Lauf
 * (es laeuft immer nur EIN Durchlauf gleichzeitig — ein MediaPlayer, ein WakeLock). Startet der
 * Nutzer den Spezial-Lauf, waehrend etwas anderes laeuft, wird der andere Lauf zuerst gestoppt.
 */

/* Eigener kleiner DataStore nur fuer die Spezial-Vorlese-Einstellungen. Bewusst getrennt vom
 * "mental_tts_settings"-Store — andere Datei, kein Konflikt. */
private val Context.specialMentalTtsStore by preferencesDataStore(name = "special_mental_tts_settings")
private val KEY_SPECIAL_PAUSE_SECONDS = intPreferencesKey("pause_seconds")
private val KEY_SPECIAL_RANDOM_PLAYBACK = booleanPreferencesKey("random_playback")
private val KEY_SPECIAL_AUTO_STOP_MINUTES = intPreferencesKey("auto_stop_minutes")

data class SpecialMentalTtsUiState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    /** Pause zwischen zwei gesprochenen speziellen Mental-Saetzen (1..30 Sekunden). */
    val pauseSeconds: Int = 9,
    /** Zufaellige Reihenfolge der speziellen Mental-Saetze. */
    val randomPlayback: Boolean = false,
    /** Abschaltzeit nur fuer die speziellen Mentals (15..120 Minuten). */
    val autoStopMinutes: Int = AppSettings.DEFAULT_TTS_AUTO_STOP_MINUTES,
    val autoStopRemainingMs: Long? = null,
    val error: String? = null,
)

private data class SpecialSettings(
    val pauseSeconds: Int,
    val randomPlayback: Boolean,
    val autoStopMinutes: Int,
)

@HiltViewModel
class SpecialMentalTtsViewModel
@Inject
    constructor(
        application: Application,
        private val playback: MentalTtsPlaybackController,
        @ApplicationScope private val applicationScope: CoroutineScope,
    ) : AndroidViewModel(application) {

    private companion object {
        const val DEFAULT_PAUSE_SECONDS = 9
        const val PAUSE_RANGE_MIN = 1
        const val PAUSE_RANGE_MAX = 30
        const val AUTO_STOP_MIN = 15
        const val AUTO_STOP_MAX = 120
        const val LABEL = "Spezial"
    }

    private val ctx: Context
        get() = getApplication()

    private val settingsFlow: Flow<SpecialSettings> =
        ctx.specialMentalTtsStore.data.map { p ->
            SpecialSettings(
                pauseSeconds = (p[KEY_SPECIAL_PAUSE_SECONDS] ?: DEFAULT_PAUSE_SECONDS)
                    .coerceIn(PAUSE_RANGE_MIN, PAUSE_RANGE_MAX),
                randomPlayback = p[KEY_SPECIAL_RANDOM_PLAYBACK] ?: false,
                autoStopMinutes = (p[KEY_SPECIAL_AUTO_STOP_MINUTES] ?: AppSettings.DEFAULT_TTS_AUTO_STOP_MINUTES)
                    .coerceIn(AUTO_STOP_MIN, AUTO_STOP_MAX),
            )
        }

    val uiState: StateFlow<SpecialMentalTtsUiState> =
        combine(
            settingsFlow,
            playback.isPlayingFlow,
            playback.isPausedFlow,
            playback.playbackLabelFlow,
            combine(playback.autoStopRemainingMsFlow, playback.errorFlow) { rem, err -> rem to err },
        ) { s, playing, paused, label, remErr ->
            // isPlaying/isPaused nur, wenn WIRKLICH der Spezial-Lauf aktiv ist (nicht Mental/Gewohnheit).
            val isSpecial = playing && label == LABEL
            SpecialMentalTtsUiState(
                isPlaying = isSpecial,
                isPaused = isSpecial && paused,
                pauseSeconds = s.pauseSeconds,
                randomPlayback = s.randomPlayback,
                autoStopMinutes = s.autoStopMinutes,
                autoStopRemainingMs = if (isSpecial) remErr.first else null,
                error = remErr.second,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SpecialMentalTtsUiState(),
            )

    /* ----------------------------- Einstellungen ----------------------------- */

    fun setPauseSeconds(seconds: Int) {
        viewModelScope.launch {
            ctx.specialMentalTtsStore.edit {
                it[KEY_SPECIAL_PAUSE_SECONDS] = seconds.coerceIn(PAUSE_RANGE_MIN, PAUSE_RANGE_MAX)
            }
        }
    }

    fun setRandomPlayback(enabled: Boolean) {
        viewModelScope.launch {
            ctx.specialMentalTtsStore.edit { it[KEY_SPECIAL_RANDOM_PLAYBACK] = enabled }
        }
    }

    fun setAutoStopMinutes(minutes: Int) {
        viewModelScope.launch {
            ctx.specialMentalTtsStore.edit {
                it[KEY_SPECIAL_AUTO_STOP_MINUTES] = minutes.coerceIn(AUTO_STOP_MIN, AUTO_STOP_MAX)
            }
        }
    }

    fun dismissError() {
        playback.dismissError()
    }

    /* ----------------------------- Steuerung ----------------------------- */

    /** Play: startet den Spezial-Lauf mit den uebergebenen (aktivierten) Saetzen — oder setzt fort. */
    fun play(specials: List<SpecialMental>) {
        // Lauf lebt im ApplicationScope; auch das Einlesen der Einstellungen darf Navigation ueberleben.
        applicationScope.launch {
            val s = settingsFlow.first()
            playback.startSpecialPlayback(
                specialTexts = specials.map { it.text },
                pauseSeconds = s.pauseSeconds,
                randomPlayback = s.randomPlayback,
                autoStopMinutes = s.autoStopMinutes,
            )
        }
    }

    fun pause() {
        playback.pause()
    }

    fun stop() {
        playback.stop()
    }

    override fun onCleared() {
        super.onCleared()
        // Playback lebt im ApplicationScope weiter. Stop nur per Stop-Button oder Timeout.
    }
}

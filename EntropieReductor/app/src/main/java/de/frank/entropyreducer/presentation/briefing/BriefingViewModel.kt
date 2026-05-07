package de.frank.entropyreducer.presentation.briefing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.tts.TtsPlayer
import de.frank.entropyreducer.workers.BackgroundScheduler
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI-Zustand fuer das Briefing-Panel auf Dashboard 1 (Tagesbriefing) und
 * Detail-Screens fuer Wochen-/Monatsrueckblick.
 */
data class BriefingUiState(
    val dailyText: String = "",
    val dailyDate: String = "",
    val dailyAtMs: Long = 0L,
    val weeklyText: String = "",
    val weeklyAtMs: Long = 0L,
    val monthlyText: String = "",
    val monthlyAtMs: Long = 0L,
    val playing: PlayingKind = PlayingKind.NONE,
    val loading: PlayingKind = PlayingKind.NONE,
    val errorMessage: String? = null,
)

enum class PlayingKind { NONE, DAILY, WEEKLY, MONTHLY }

/**
 * ViewModel fuer das Briefing-Panel — kombiniert die drei Cache-Flows aus
 * AppSettings, kapselt TTS-Wiedergabe und triggert manuelle Generierung
 * ueber den BackgroundScheduler.
 */
@HiltViewModel
class BriefingViewModel @Inject constructor(
    private val settings: AppSettings,
    private val tts: TtsPlayer,
    private val scheduler: BackgroundScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(BriefingUiState())
    val state: StateFlow<BriefingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settings.dailyBriefingTextFlow,
                settings.dailyBriefingDateFlow,
                settings.dailyBriefingGeneratedAtMsFlow,
            ) { text, date, at ->
                Triple(text, date, at)
            }.collect { (text, date, at) ->
                _state.update {
                    it.copy(dailyText = text, dailyDate = date, dailyAtMs = at)
                }
            }
        }
        viewModelScope.launch {
            combine(
                settings.lastWeeklyReviewTextFlow,
                settings.lastWeeklyReviewAtMsFlow,
            ) { t, at -> t to at }
                .collect { (t, at) ->
                    _state.update { it.copy(weeklyText = t, weeklyAtMs = at) }
                }
        }
        viewModelScope.launch {
            combine(
                settings.lastMonthlyReviewTextFlow,
                settings.lastMonthlyReviewAtMsFlow,
            ) { t, at -> t to at }
                .collect { (t, at) ->
                    _state.update { it.copy(monthlyText = t, monthlyAtMs = at) }
                }
        }
    }

    /** Setzt den Text vor — zwingt TTS-Stop wenn gerade gesprochen wird. */
    fun speak(kind: PlayingKind) {
        val text = when (kind) {
            PlayingKind.DAILY -> _state.value.dailyText
            PlayingKind.WEEKLY -> _state.value.weeklyText
            PlayingKind.MONTHLY -> _state.value.monthlyText
            PlayingKind.NONE -> ""
        }
        if (text.isBlank()) return
        if (_state.value.playing == kind || _state.value.loading == kind) {
            stop()
            return
        }
        _state.update { it.copy(loading = kind, errorMessage = null) }
        viewModelScope.launch {
            tts.speak(
                text = text,
                onPlaybackStart = { _state.update { it.copy(loading = PlayingKind.NONE, playing = kind) } },
                onComplete = { _state.update { it.copy(playing = PlayingKind.NONE) } },
                onError = { e ->
                    _state.update {
                        it.copy(
                            loading = PlayingKind.NONE,
                            playing = PlayingKind.NONE,
                            errorMessage = e.message,
                        )
                    }
                },
            )
        }
    }

    fun stop() {
        tts.stop()
        _state.update { it.copy(playing = PlayingKind.NONE, loading = PlayingKind.NONE) }
    }

    fun regenerateDaily() = scheduler.runDailyBriefingNow()
    fun regenerateWeekly() = scheduler.runWeeklyReviewNow()
    fun regenerateMonthly() = scheduler.runMonthlyReviewNow()

    override fun onCleared() {
        tts.stop()
        super.onCleared()
    }
}

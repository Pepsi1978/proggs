package de.frank.entropyreducer.presentation.briefing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.tts.TtsPlayer
import de.frank.entropyreducer.workers.BackgroundScheduler
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI-Zustand für das Briefing-Panel auf Dashboard 1 (Tagesbriefing) und
 * Detail-Screens für Wochen-/Monatsrueckblick.
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
 * ViewModel für das Briefing-Panel — kombiniert die drei Cache-Flows aus
 * AppSettings, kapselt TTS-Wiedergabe und triggert manuelle Generierung
 * über den BackgroundScheduler.
 */
@HiltViewModel
class BriefingViewModel @Inject constructor(
    private val settings: AppSettings,
    private val tts: TtsPlayer,
    private val scheduler: BackgroundScheduler,
    private val process: de.frank.entropyreducer.domain.usecase.ProcessEntryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(BriefingUiState())
    val state: StateFlow<BriefingUiState> = _state.asStateFlow()

    /**
     * Job des aktuell laufenden TTS-Aufrufs. Wird gecancelled bevor ein neuer
     * speak() startet — sonst koennte der alte Callback den State des neuen
     * Aufrufs ueberschreiben (Race Condition zwischen Tabs).
     */
    private var speakJob: Job? = null

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

    /**
     * Startet TTS-Wiedergabe für den gewaehlten Briefing-Typ. Toggle-Verhalten:
     * Wenn für DEN GLEICHEN Tab schon laeuft → stop() (Pause).
     * Wenn ein ANDERER Tab gerade laeuft → vorher stoppen, dann neu starten.
     *
     * Race-Schutz: Vorheriger speakJob wird IMMER gecancelled, bevor ein neuer
     * Coroutine-Job startet. So können alte Callbacks den State des neuen
     * Aufrufs nicht mehr ueberschreiben.
     */
    fun speak(kind: PlayingKind) {
        val text = when (kind) {
            PlayingKind.DAILY -> _state.value.dailyText
            PlayingKind.WEEKLY -> _state.value.weeklyText
            PlayingKind.MONTHLY -> _state.value.monthlyText
            PlayingKind.NONE -> ""
        }
        if (text.isBlank()) return
        // Toggle: gleicher Tab erneut getippt → stoppen.
        if (_state.value.playing == kind || _state.value.loading == kind) {
            stop()
            return
        }
        // Anderer Tab → laufenden Job cancellen, Player stoppen, dann neu starten.
        speakJob?.cancel()
        tts.stop()
        _state.update { it.copy(loading = kind, playing = PlayingKind.NONE, errorMessage = null) }
        speakJob = viewModelScope.launch {
            tts.speak(
                text = text,
                onPlaybackStart = {
                    // Nur uebernehmen wenn dieser Job noch der aktuelle ist —
                    // sonst koennte ein veralteter Callback den State zerschiessen.
                    if (_state.value.loading == kind) {
                        _state.update { it.copy(loading = PlayingKind.NONE, playing = kind) }
                    }
                },
                onComplete = {
                    if (_state.value.playing == kind) {
                        _state.update { it.copy(playing = PlayingKind.NONE) }
                    }
                },
                onError = { e ->
                    if (_state.value.loading == kind || _state.value.playing == kind) {
                        _state.update {
                            it.copy(
                                loading = PlayingKind.NONE,
                                playing = PlayingKind.NONE,
                                errorMessage = e.message,
                            )
                        }
                    }
                },
            )
        }
    }

    fun stop() {
        speakJob?.cancel()
        speakJob = null
        tts.stop()
        _state.update { it.copy(playing = PlayingKind.NONE, loading = PlayingKind.NONE) }
    }

    fun regenerateDaily() = scheduler.runDailyBriefingNow()
    fun regenerateWeekly() = scheduler.runWeeklyReviewNow()
    fun regenerateMonthly() = scheduler.runMonthlyReviewNow()

    /**
     * Briefing-Antwort-Feature (Frank-Wunsch 2026-05-08): Frank kann auf das
     * Briefing antworten — entweder per Text oder per Sprache. Die Antwort wird
     * durch ProcessEntryUseCase als neuer Eintrag mit Tag "Briefing-Antwort"
     * verarbeitet — die KI kann Beobachtungen daraus extrahieren oder neue
     * Aufgaben anlegen, je nach Inhalt der Antwort.
     */
    fun submitBriefingResponse(kind: PlayingKind, response: String) {
        if (response.isBlank()) return
        viewModelScope.launch {
            val prefix = when (kind) {
                PlayingKind.DAILY -> "Briefing-Antwort (Tag)"
                PlayingKind.WEEKLY -> "Briefing-Antwort (Woche)"
                PlayingKind.MONTHLY -> "Briefing-Antwort (Monat)"
                PlayingKind.NONE -> "Briefing-Antwort"
            }
            process(
                rawTranscript = "$prefix: ${response.trim()}",
                source = de.frank.entropyreducer.domain.model.EntrySource.NUTZER_TEXT,
            )
        }
    }

    override fun onCleared() {
        tts.stop()
        super.onCleared()
    }
}

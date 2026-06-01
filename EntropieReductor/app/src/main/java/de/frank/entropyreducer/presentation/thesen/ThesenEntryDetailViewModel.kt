package de.frank.entropyreducer.presentation.thesen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.domain.tts.TtsPlayer
import de.frank.entropyreducer.domain.tts.TtsResult
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThesenTtsState {
    IDLE,
    LOADING,
    SPEAKING,
}

data class ThesenEntryDetailUiState(
    val entry: ThesenEntry? = null,
    val ttsState: ThesenTtsState = ThesenTtsState.IDLE,
    val error: String? = null,
    val isDeleted: Boolean = false,
)

/**
 * ViewModel für den Vollbild-Detail-Screen eines Thesen-Eintrags ("Entropie"-Sub-Bereich,
 * Frank-Wunsch 2026-05-20). 1:1 Pendant zu EntryDetailViewModel im Aufgaben-Bereich — Layout, TTS
 * und Nachträge funktionieren gleich, nur das Datenmodell ist DataStore-basiert (kein Room).
 */
@HiltViewModel
class ThesenEntryDetailViewModel
@Inject
constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val ttsPlayer: TtsPlayer,
) : AndroidViewModel(application) {

    private val entryId: String = savedStateHandle.get<String>("entryId") ?: ""

    private val ctx: android.content.Context
        get() = getApplication()

    private val entryFlow =
        if (entryId.isBlank()) flowOf<ThesenEntry?>(null) else thesenEntryFlow(ctx, entryId)

    private val ttsStateFlow = MutableStateFlow(ThesenTtsState.IDLE)
    private val errorFlow = MutableStateFlow<String?>(null)
    private val deletedFlow = MutableStateFlow(false)

    val uiState: StateFlow<ThesenEntryDetailUiState> =
        combine(entryFlow, ttsStateFlow, errorFlow, deletedFlow) { entry, tts, err, del ->
                ThesenEntryDetailUiState(
                    entry = entry,
                    ttsState = tts,
                    error = err,
                    isDeleted = del,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ThesenEntryDetailUiState(),
            )

    private var ttsJob: Job? = null

    /** Inline-Edit des Haupt-Textes. */
    fun updateText(newText: String) {
        if (entryId.isBlank()) return
        viewModelScope.launch { updateThesenEntry(ctx, entryId, text = newText) }
    }

    /** Inline-Edit des Titels. */
    fun updateTitle(newTitle: String) {
        if (entryId.isBlank()) return
        viewModelScope.launch { updateThesenEntry(ctx, entryId, title = newTitle) }
    }

    /** Speichert die KI-Bullet-Point-Zusammenfassung. */
    fun updateSummary(newSummary: String) {
        if (entryId.isBlank()) return
        viewModelScope.launch { updateThesenEntry(ctx, entryId, summary = newSummary) }
    }

    /** Speichert einen neuen Nachtrag. */
    fun addFollowup(text: String) {
        val clean = text.trim()
        if (clean.isBlank() || entryId.isBlank()) return
        viewModelScope.launch {
            addThesenFollowup(
                ctx,
                entryId,
                ThesenFollowup(
                    id = UUID.randomUUID().toString(),
                    createdAtMs = System.currentTimeMillis(),
                    text = clean,
                ),
            )
        }
    }

    fun updateFollowup(followupId: String, newText: String) {
        if (entryId.isBlank()) return
        viewModelScope.launch { updateThesenFollowup(ctx, entryId, followupId, newText) }
    }

    fun deleteFollowup(followupId: String) {
        if (entryId.isBlank()) return
        viewModelScope.launch { deleteThesenFollowup(ctx, entryId, followupId) }
    }

    /**
     * Frank-Wunsch 2026-05-23: KI-Nachbearbeitung des Eintrags-Texts via Gemini.
     * Speichert das Ergebnis als improvedText, isImproved=true. Der Original-Text
     * bleibt in `text` erhalten — die UI bietet einen Tab zwischen beiden.
     */
    fun setImprovedText(improved: String) {
        if (entryId.isBlank()) return
        viewModelScope.launch {
            updateThesenEntry(
                ctx,
                entryId,
                improvedText = improved,
                isImproved = true,
            )
        }
    }

    /**
     * Frank-Wunsch 2026-05-23: KI-Verbesserung eines einzelnen Nachtrags. Der originale
     * Nachtragstext bleibt erhalten — improvedText wird daneben gespeichert.
     */
    fun setFollowupImproved(followupId: String, improved: String) {
        if (entryId.isBlank()) return
        viewModelScope.launch {
            setThesenFollowupImproved(ctx, entryId, followupId, improved)
        }
    }

    fun deleteEntry() {
        if (entryId.isBlank()) return
        viewModelScope.launch {
            stopTts()
            deleteThesenEntry(ctx, entryId)
            deletedFlow.value = true
        }
    }

    /**
     * Spricht Titel + Text + alle Nachträge der Reihe nach vor.
     *
     * @param showImproved Welche Variante vorgelesen wird — entspricht dem aktuell
     *        im Detail-Screen angewählten Tab: true = KI-verbesserte Fassung
     *        (Fallback auf Original, wo es keine verbesserte gibt), false = Original.
     *        So liest der Lautsprecher exakt das vor, was gerade sichtbar ist.
     */
    fun speakAll(showImproved: Boolean) {
        val state = uiState.value
        val entry = state.entry ?: return
        if (state.ttsState != ThesenTtsState.IDLE) {
            stopTts()
            return
        }
        val parts = buildList {
            if (entry.title.isNotBlank()) add(entry.title)
            val mainImproved = entry.improvedText
            val mainText =
                if (showImproved && !mainImproved.isNullOrBlank()) mainImproved else entry.text
            if (mainText.isNotBlank()) add(mainText)
            entry.followups.forEachIndexed { index, f ->
                add("Nachtrag ${germanOrdinal(index + 1)}")
                val fImproved = f.improvedText
                add(if (showImproved && !fImproved.isNullOrBlank()) fImproved else f.text)
            }
        }
        val spokenText = parts.joinToString(separator = ". ").take(4500)

        ttsStateFlow.value = ThesenTtsState.LOADING
        ttsJob = viewModelScope.launch {
            val result =
                ttsPlayer.speak(
                    text = spokenText,
                    onPlaybackStart = { ttsStateFlow.value = ThesenTtsState.SPEAKING },
                    onComplete = { ttsStateFlow.value = ThesenTtsState.IDLE },
                    onError = {
                        ttsStateFlow.value = ThesenTtsState.IDLE
                        errorFlow.value = "Vorlesen fehlgeschlagen: ${it.message}"
                    },
                )
            if (result is TtsResult.Error) {
                ttsStateFlow.value = ThesenTtsState.IDLE
                errorFlow.value = result.message
            }
        }
    }

    fun stopTts() {
        ttsPlayer.stop()
        ttsJob?.cancel()
        ttsJob = null
        ttsStateFlow.value = ThesenTtsState.IDLE
    }

    fun dismissError() {
        errorFlow.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTts()
    }

    private fun germanOrdinal(n: Int): String =
        when (n) {
            1 -> "Eins"
            2 -> "Zwei"
            3 -> "Drei"
            4 -> "Vier"
            5 -> "Fünf"
            6 -> "Sechs"
            7 -> "Sieben"
            8 -> "Acht"
            9 -> "Neun"
            10 -> "Zehn"
            else -> "$n"
        }
}

package de.frank.entropyreducer.presentation.ideen

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

enum class IdeenTtsState {
    IDLE,
    LOADING,
    SPEAKING,
}

data class IdeenEntryDetailUiState(
    val entry: IdeenEntry? = null,
    val ttsState: IdeenTtsState = IdeenTtsState.IDLE,
    val error: String? = null,
    val isDeleted: Boolean = false,
)

/**
 * ViewModel für den Vollbild-Detail-Screen eines Ideen-Eintrags ("Entropie"-Sub-Bereich,
 * Frank-Wunsch 2026-05-20). 1:1 Pendant zu EntryDetailViewModel im Aufgaben-Bereich — Layout, TTS
 * und Nachträge funktionieren gleich, nur das Datenmodell ist DataStore-basiert (kein Room).
 */
@HiltViewModel
class IdeenEntryDetailViewModel
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
        if (entryId.isBlank()) flowOf<IdeenEntry?>(null) else ideenEntryFlow(ctx, entryId)

    private val ttsStateFlow = MutableStateFlow(IdeenTtsState.IDLE)
    private val errorFlow = MutableStateFlow<String?>(null)
    private val deletedFlow = MutableStateFlow(false)

    val uiState: StateFlow<IdeenEntryDetailUiState> =
        combine(entryFlow, ttsStateFlow, errorFlow, deletedFlow) { entry, tts, err, del ->
                IdeenEntryDetailUiState(
                    entry = entry,
                    ttsState = tts,
                    error = err,
                    isDeleted = del,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = IdeenEntryDetailUiState(),
            )

    private var ttsJob: Job? = null

    /** Inline-Edit des Haupt-Textes. */
    fun updateText(newText: String) {
        if (entryId.isBlank()) return
        viewModelScope.launch { updateIdeenEntry(ctx, entryId, text = newText) }
    }

    /** Inline-Edit des Titels. */
    fun updateTitle(newTitle: String) {
        if (entryId.isBlank()) return
        viewModelScope.launch { updateIdeenEntry(ctx, entryId, title = newTitle) }
    }

    /** Speichert die KI-Bullet-Point-Zusammenfassung. */
    fun updateSummary(newSummary: String) {
        if (entryId.isBlank()) return
        viewModelScope.launch { updateIdeenEntry(ctx, entryId, summary = newSummary) }
    }

    /** Speichert einen neuen Nachtrag. */
    fun addFollowup(text: String) {
        val clean = text.trim()
        if (clean.isBlank() || entryId.isBlank()) return
        viewModelScope.launch {
            addIdeenFollowup(
                ctx,
                entryId,
                IdeenFollowup(
                    id = UUID.randomUUID().toString(),
                    createdAtMs = System.currentTimeMillis(),
                    text = clean,
                ),
            )
        }
    }

    fun updateFollowup(followupId: String, newText: String) {
        if (entryId.isBlank()) return
        viewModelScope.launch { updateIdeenFollowup(ctx, entryId, followupId, newText) }
    }

    fun deleteFollowup(followupId: String) {
        if (entryId.isBlank()) return
        viewModelScope.launch { deleteIdeenFollowup(ctx, entryId, followupId) }
    }

    /**
     * Frank-Wunsch 2026-05-23: KI-Nachbearbeitung des Eintrags-Texts via Gemini.
     * Speichert das Ergebnis als improvedText, isImproved=true. Der Original-Text
     * bleibt in `text` erhalten — die UI bietet einen Tab zwischen beiden.
     */
    fun setImprovedText(improved: String) {
        if (entryId.isBlank()) return
        viewModelScope.launch {
            updateIdeenEntry(
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
            setIdeenFollowupImproved(ctx, entryId, followupId, improved)
        }
    }

    fun deleteEntry() {
        if (entryId.isBlank()) return
        viewModelScope.launch {
            stopTts()
            deleteIdeenEntry(ctx, entryId)
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
        if (state.ttsState != IdeenTtsState.IDLE) {
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

        ttsStateFlow.value = IdeenTtsState.LOADING
        ttsJob = viewModelScope.launch {
            val result =
                ttsPlayer.speak(
                    text = spokenText,
                    onPlaybackStart = { ttsStateFlow.value = IdeenTtsState.SPEAKING },
                    onComplete = { ttsStateFlow.value = IdeenTtsState.IDLE },
                    onError = {
                        ttsStateFlow.value = IdeenTtsState.IDLE
                        errorFlow.value = "Vorlesen fehlgeschlagen: ${it.message}"
                    },
                )
            if (result is TtsResult.Error) {
                ttsStateFlow.value = IdeenTtsState.IDLE
                errorFlow.value = result.message
            }
        }
    }

    fun stopTts() {
        ttsPlayer.stop()
        ttsJob?.cancel()
        ttsJob = null
        ttsStateFlow.value = IdeenTtsState.IDLE
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

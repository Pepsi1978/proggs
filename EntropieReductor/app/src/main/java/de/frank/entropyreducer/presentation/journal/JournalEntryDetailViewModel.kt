package de.frank.entropyreducer.presentation.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDao
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorEntryEntity
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorFollowupEntity
import de.frank.entropyreducer.domain.tts.TtsPlayer
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class JournalTtsState {
    IDLE,
    LOADING,
    SPEAKING,
}

data class JournalDetailState(
    val entry: JournalMirrorEntryEntity? = null,
    val followups: List<JournalMirrorFollowupEntity> = emptyList(),
    val loaded: Boolean = false,
    val ttsState: JournalTtsState = JournalTtsState.IDLE,
)

@HiltViewModel
class JournalEntryDetailViewModel
@Inject
constructor(
    savedState: SavedStateHandle,
    private val dao: JournalMirrorDao,
    private val tts: TtsPlayer,
) : ViewModel() {

    private val sourceId: Long = savedState.get<String>("sourceId")?.toLongOrNull() ?: -1L
    private val _state = MutableStateFlow(JournalDetailState())
    val state: StateFlow<JournalDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val entry = dao.getEntry(sourceId)
            val followups = if (entry != null) dao.followupsFor(sourceId) else emptyList()
            _state.value = JournalDetailState(entry = entry, followups = followups, loaded = true)
        }
    }

    /** Liest Eintrag (Titel + Text) + alle Nachtraege am Stueck hintereinander vor. */
    fun speak() {
        val s = _state.value
        if (s.ttsState != JournalTtsState.IDLE) {
            // Bereits am Vorlesen/Laden -> stoppen (Toggle).
            stopSpeaking()
            return
        }
        val entry = s.entry ?: return
        val sb = StringBuilder()
        entry.title?.takeIf { it.isNotBlank() }?.let { sb.append(it).append(". ") }
        sb.append(entry.displayText)
        s.followups.forEach { f ->
            val txt = if (f.isImproved && !f.improvedText.isNullOrBlank()) f.improvedText else f.text
            sb.append(". Nachtrag: ").append(txt)
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(ttsState = JournalTtsState.LOADING)
            tts.speak(
                // Google TTS verarbeitet ~5000 Zeichen pro Request.
                text = sb.toString().take(4800),
                onPlaybackStart = {
                    _state.value = _state.value.copy(ttsState = JournalTtsState.SPEAKING)
                },
                onComplete = { _state.value = _state.value.copy(ttsState = JournalTtsState.IDLE) },
                onError = { _state.value = _state.value.copy(ttsState = JournalTtsState.IDLE) },
            )
        }
    }

    fun stopSpeaking() {
        tts.stop()
        _state.value = _state.value.copy(ttsState = JournalTtsState.IDLE)
    }

    override fun onCleared() {
        tts.stop()
        super.onCleared()
    }
}

package de.frank.cortex.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.frank.cortex.audio.MicRecorder
import de.frank.cortex.data.model.*
import de.frank.cortex.network.ApiClient
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.vpn.TunnelState
import de.frank.cortex.vpn.WireGuardManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val action: String? = null,
    val category: String? = null,
    val title: String? = null,
    val recallHits: Int? = null,
    val options: List<ChatOption>? = null,
    val stored: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val categories: List<CategoryInfo> = emptyList(),
    val selectedCategory: String? = null,
    val titleOverride: String = "",
    val ttsEnabled: Boolean = true,
    val sessionId: String = "android-${UUID.randomUUID()}",
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false
)

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val micRecorder = MicRecorder()

    init {
        // Kategorien vom Server laden, SOBALD das VPN verbunden ist. Vorher (beim App-Start) ist
        // der Tunnel noch aus, der Aufruf liefe ins Leere — deshalb blieb das Dropdown leer.
        viewModelScope.launch {
            WireGuardManager.state.collect { st ->
                if (st == TunnelState.CONNECTED) loadCategories()
            }
        }
    }

    fun toggleRecording() {
        if (micRecorder.isRecording()) {
            // Stoppen + transkribieren
            _uiState.update { it.copy(isRecording = false, isTranscribing = true) }
            viewModelScope.launch {
                try {
                    val wavBytes = withContext(Dispatchers.IO) { micRecorder.stop() }
                    if (wavBytes == null || wavBytes.isEmpty()) {
                        _uiState.update { it.copy(isTranscribing = false, error = "Keine Aufnahme") }
                        return@launch
                    }
                    CortexLog.info("ChatVM", "toggleRecording", "Aufnahme gestoppt", mapOf("wav_bytes" to wavBytes.size))

                    // Stille-Halluzination filtern
                    val result = ApiClient.groqTranscribe(wavBytes)
                    val filteredText = filterSilence(result)
                    if (filteredText.isNotBlank()) {
                        _uiState.update { it.copy(isTranscribing = false) }
                        // Text wird über Callback ans UI zurückgegeben
                        _transcribedText.tryEmit(filteredText)
                    } else {
                        _uiState.update { it.copy(isTranscribing = false) }
                        CortexLog.info("ChatVM", "toggleRecording", "Stille erkannt — nichts eingefügt")
                    }
                } catch (e: Exception) {
                    CortexLog.error("ChatVM", "toggleRecording", "Transkription fehlgeschlagen: ${e.message}")
                    _uiState.update { it.copy(isTranscribing = false, error = "Transkription fehlgeschlagen: ${e.message}") }
                }
            }
        } else {
            // Aufnahme starten
            _uiState.update { it.copy(isRecording = true) }
            micRecorder.start(viewModelScope)
            CortexLog.checkpoint(
                step = "stt_start",
                intent = "Mikrofon-Aufnahme starten",
                expected = "recording",
                actual = "recording",
                ok = true
            )
        }
    }

    // Flow für transkribierten Text → UI setzt ihn ins Eingabefeld
    private val _transcribedText = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val transcribedText: SharedFlow<String> = _transcribedText

    private fun filterSilence(result: de.frank.cortex.data.model.GroqTranscriptionResponse): String {
        val segments = result.segments
        if (segments.isNullOrEmpty()) return result.text.trim()

        // Segmente mit hoher Stille-Wahrscheinlichkeit herausfiltern
        val significantSegments = segments.filter { seg ->
            val noSpeech = seg.no_speech_prob ?: 0.0
            val avgLogprob = seg.avg_logprob ?: 0.0
            noSpeech < 0.6 && avgLogprob > -1.0
        }
        return if (significantSegments.isEmpty()) "" else result.text.trim()
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                if (WireGuardManager.state.value != TunnelState.CONNECTED) return@launch
                val response = ApiClient.agentApi().getCategories()
                _uiState.update { it.copy(categories = response.categories) }
                CortexLog.info("ChatVM", "loadCategories", "Kategorien geladen", mapOf("count" to response.categories.size))
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "loadCategories", "Fehler: ${e.message}")
            }
        }
    }

    fun createCategory(name: String) {
        viewModelScope.launch {
            try {
                if (WireGuardManager.state.value != TunnelState.CONNECTED) return@launch
                ApiClient.agentApi().createCategory(CreateCategoryRequest(name))
                loadCategories()
                _uiState.update { it.copy(selectedCategory = name) }
                CortexLog.info("ChatVM", "createCategory", "Kategorie erstellt: $name")
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "createCategory", "Fehler: ${e.message}")
                _uiState.update { it.copy(error = "Kategorie konnte nicht erstellt werden: ${e.message}") }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        if (text.length > 500000) {
            _uiState.update { it.copy(error = "Text zu lang (max. 500.000 Zeichen)") }
            return
        }

        val state = _uiState.value

        // User-Nachricht hinzufügen
        val userMsg = ChatMessage(text = text, isUser = true)
        _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                if (WireGuardManager.state.value != TunnelState.CONNECTED) {
                    _uiState.update { it.copy(isLoading = false, error = "VPN nicht aktiv") }
                    return@launch
                }

                val request = ChatRequest(
                    text = text,
                    session_id = state.sessionId,
                    category = state.selectedCategory,
                    title = state.titleOverride.ifBlank { null }
                )

                CortexLog.checkpoint(
                    step = "chat_send",
                    intent = "Nachricht an Agent senden",
                    expected = "reply mit action",
                    actual = "warte…",
                    ok = false,
                    ctx = mapOf("text_length" to text.length)
                )

                val response = ApiClient.agentApi().chat(request)

                val agentMsg = ChatMessage(
                    text = response.reply,
                    isUser = false,
                    action = response.action,
                    category = response.category,
                    title = response.title,
                    recallHits = response.recall_hits,
                    options = response.options,
                    stored = response.stored
                )

                _uiState.update {
                    it.copy(
                        messages = it.messages + agentMsg,
                        isLoading = false,
                        titleOverride = if (response.action == "store") "" else it.titleOverride
                    )
                }

                CortexLog.checkpoint(
                    step = "chat_send",
                    intent = "Nachricht an Agent senden",
                    expected = "reply mit action",
                    actual = "action=${response.action}",
                    ok = true,
                    ctx = mapOf("stored" to response.stored, "action" to response.action)
                )

            } catch (e: Exception) {
                CortexLog.error("ChatVM", "sendMessage", "Fehler: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Fehler: ${e.message}"
                    )
                }
            }
        }
    }

    fun sendOption(option: ChatOption) {
        sendMessage(option.send)
    }

    fun clearText() {
        // Wird im UI gehandhabt
    }

    fun updateTitleOverride(title: String) {
        _uiState.update { it.copy(titleOverride = title) }
    }

    fun updateSelectedCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun toggleTts() {
        _uiState.update { it.copy(ttsEnabled = !it.ttsEnabled) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

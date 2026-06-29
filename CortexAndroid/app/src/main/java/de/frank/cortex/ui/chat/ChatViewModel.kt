package de.frank.cortex.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.frank.cortex.audio.MicRecorder
import de.frank.cortex.audio.PcmPlayer
import de.frank.cortex.data.model.*
import de.frank.cortex.data.SettingsStore
import de.frank.cortex.network.ApiClient
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.vpn.TunnelState
import de.frank.cortex.vpn.WireGuardManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
    val isTranscribing: Boolean = false,
    val isSpeaking: Boolean = false,
    val isImproving: Boolean = false,
    val isGeneratingTitle: Boolean = false,
    val contextMode: String = SettingsStore.CONTEXT_MODE_AUTO
)

object ChatCommands {
    private val _newChat = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val newChat: SharedFlow<Unit> = _newChat

    fun requestNewChat() {
        _newChat.tryEmit(Unit)
    }
}

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val micRecorder = MicRecorder()
    private val pcmPlayer = PcmPlayer()
    private var speakJob: Job? = null

    init {
        // Vorlese-Schalter aus den Einstellungen uebernehmen, damit Chat-Icon + Auto-Vorlesen
        // denselben Stand zeigen wie der "Antwort vorlesen"-Schalter in den Einstellungen.
        _uiState.update { it.copy(ttsEnabled = SettingsStore.ttsEnabled) }

        // Kategorien vom Server laden, SOBALD das VPN verbunden ist. Vorher (beim App-Start) ist
        // der Tunnel noch aus, der Aufruf liefe ins Leere — deshalb blieb das Dropdown leer.
        viewModelScope.launch {
            WireGuardManager.state.collect { st ->
                if (st == TunnelState.CONNECTED) loadCategories()
            }
        }
    }

    fun toggleRecording() {
        CortexLog.info("ChatVM", "toggleRecording", "toggleRecording aufgerufen, isRecording=${micRecorder.isRecording()}")
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
            val started = micRecorder.start(viewModelScope)
            if (!started) {
                CortexLog.error("ChatVM", "toggleRecording", "AudioRecord konnte nicht initialisiert werden")
                _uiState.update { it.copy(isRecording = false, error = "Mikrofon nicht verfügbar") }
                return
            }
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

    private val _improvedText = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val improvedText: SharedFlow<String> = _improvedText

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
                    title = state.titleOverride.ifBlank { null },
                    context_mode = state.contextMode,
                    context_prompt = if (state.contextMode == SettingsStore.CONTEXT_MODE_AUTO) null
                    else SettingsStore.contextPrompt(state.contextMode)
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

                // Auto-TTS: Antwort vorlesen falls aktiviert
                // Auto-Vorlesen folgt dem Schalter aus den Einstellungen (Single Source of Truth).
                if (SettingsStore.ttsEnabled && response.reply.isNotBlank()) {
                    speakResponse(response.reply)
                }

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

    fun startNewChat() {
        stopSpeaking()
        if (micRecorder.isRecording()) {
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) { micRecorder.stop() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    CortexLog.warn("ChatVM", "startNewChat", "Aufnahme-Stopp fehlgeschlagen: ${e.message}")
                }
            }
        }
        _uiState.update {
            it.copy(
                messages = emptyList(),
                isLoading = false,
                error = null,
                titleOverride = "",
                sessionId = "android-${UUID.randomUUID()}",
                isRecording = false,
                isTranscribing = false,
                isImproving = false,
                isGeneratingTitle = false
            )
        }
        CortexLog.info("ChatVM", "startNewChat", "Neuer Chat gestartet")
    }

    fun improveText(text: String) {
        val original = text.trim()
        if (original.isBlank()) {
            _uiState.update { it.copy(error = "Kein Text zum Verbessern") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isImproving = true, error = null) }
            try {
                CortexLog.checkpoint(
                    step = "gemini_improve",
                    intent = "Eingabetext sprachlich verbessern",
                    expected = "verbesserter Text ersetzt Eingabefeld",
                    actual = "warte…",
                    ok = false,
                    ctx = mapOf("input_len" to original.length)
                )
                val improved = withContext(Dispatchers.IO) { ApiClient.geminiImprove(original) }.trim()
                if (improved.isBlank()) {
                    throw IllegalStateException("Gemini hat leeren Text zurückgegeben")
                }
                _improvedText.emit(improved)
                _uiState.update { it.copy(isImproving = false) }
                CortexLog.checkpoint(
                    step = "gemini_improve",
                    intent = "Eingabetext sprachlich verbessern",
                    expected = "verbesserter Text ersetzt Eingabefeld",
                    actual = "output_len=${improved.length}",
                    ok = true,
                    ctx = mapOf("input_len" to original.length, "output_len" to improved.length)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "improveText", "Gemini-Verbesserung fehlgeschlagen: ${e.message}")
                _uiState.update {
                    it.copy(
                        isImproving = false,
                        error = "Gemini konnte den Text nicht verbessern: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearText() {
        // Wird im UI gehandhabt
    }

    fun updateTitleOverride(title: String) {
        _uiState.update { it.copy(titleOverride = title) }
    }

    fun generateTitleFromText(text: String) {
        val source = text.trim()
        if (source.isBlank()) {
            _uiState.update { it.copy(error = "Kein Text für einen Titel") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingTitle = true, error = null) }
            try {
                val title = withContext(Dispatchers.IO) { ApiClient.geminiTitle(source) }
                _uiState.update { it.copy(titleOverride = title.take(200), isGeneratingTitle = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "generateTitleFromText", "Titelgenerierung fehlgeschlagen: ${e.message}")
                _uiState.update {
                    it.copy(isGeneratingTitle = false, error = "Gemini konnte keinen Titel erzeugen: ${e.message}")
                }
            }
        }
    }

    fun updateSelectedCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateContextMode(mode: String) {
        _uiState.update { it.copy(contextMode = mode) }
    }

    fun toggleTts() {
        // Einmal auf den Lautsprecher tippen = laufendes Vorlesen SOFORT abbrechen + an/aus schalten.
        val nowEnabled = !_uiState.value.ttsEnabled
        stopSpeaking()
        _uiState.update { it.copy(ttsEnabled = nowEnabled) }
        SettingsStore.ttsEnabled = nowEnabled
    }

    /** Bricht laufendes Vorlesen sofort ab (Lautsprecher-Knopf). */
    fun stopSpeaking() {
        speakJob?.cancel()
        speakJob = null
        pcmPlayer.stop()
        _uiState.update { it.copy(isSpeaking = false) }
    }

    override fun onCleared() {
        super.onCleared()
        speakJob?.cancel()
        pcmPlayer.stop()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun speakResponse(text: String) {
        speakJob?.cancel()
        speakJob = viewModelScope.launch {
            _uiState.update { it.copy(isSpeaking = true) }
            try {
                val voice = SettingsStore.ttsVoice
                val chunks = chunkText(text, 360)
                val ttsStartedAt = System.currentTimeMillis()
                CortexLog.info("ChatVM", "speakResponse", "TTS Pipeline startet",
                    mapOf("len" to text.length, "chunks" to chunks.size, "voice" to voice))

                coroutineScope {
                    var nextAudio = async(Dispatchers.IO) { ApiClient.geminiTts(chunks.first(), voice) }
                    chunks.forEachIndexed { index, chunk ->
                        val pcm = nextAudio.await()
                        if (index == 0) {
                            CortexLog.info("ChatVM", "speakResponse", "TTS erster Ton bereit",
                                mapOf("elapsed_ms" to (System.currentTimeMillis() - ttsStartedAt), "chunk_len" to chunk.length))
                        }
                        nextAudio = if (index < chunks.lastIndex) {
                            async(Dispatchers.IO) { ApiClient.geminiTts(chunks[index + 1], voice) }
                        } else {
                            async(Dispatchers.IO) { ByteArray(0) }
                        }
                        pcmPlayer.playAndAwait(pcm, SettingsStore.ttsRate)
                    }
                }
                CortexLog.info("ChatVM", "speakResponse", "TTS abgeschlossen",
                    mapOf("elapsed_ms" to (System.currentTimeMillis() - ttsStartedAt), "chunks" to chunks.size))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "speakResponse", "TTS fehlgeschlagen: ${e.message}")
            } finally {
                pcmPlayer.stop()
                _uiState.update { it.copy(isSpeaking = false) }
            }
        }
    }

    private fun chunkText(text: String, maxLen: Int): List<String> {
        val chunks = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.isNotEmpty()) {
            if (remaining.length <= maxLen) {
                chunks.add(remaining)
                break
            }
            // Suche Satzgrenze (., !, ?) innerhalb der ersten maxLen Zeichen
            val searchEnd = minOf(maxLen, remaining.length) - 1
            val lastSentence = remaining.take(maxLen).indexOfLast { it in ".!?" }
            val splitAt = if (lastSentence > maxLen / 2) lastSentence + 1 else searchEnd
            // Falls kein Satzzeichen, suche Leerzeichen
            val finalSplit = if (splitAt > maxLen / 2) splitAt
            else remaining.take(maxLen).indexOfLast { it == ' ' }.let {
                if (it > 0) it + 1 else maxLen
            }
            chunks.add(remaining.take(finalSplit).trim())
            remaining = remaining.drop(finalSplit).trim()
        }
        return chunks
    }
}

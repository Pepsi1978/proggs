package de.frank.cortex.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.frank.cortex.audio.MicRecorder
import de.frank.cortex.audio.PcmPlayer
import de.frank.cortex.data.ChatSessionStore
import de.frank.cortex.data.ChatSessionSummary
import de.frank.cortex.data.model.*
import de.frank.cortex.data.SettingsStore
import de.frank.cortex.data.StoredChatMessage
import de.frank.cortex.network.ApiClient
import de.frank.cortex.network.GeminiRateLimitException
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.vpn.TunnelState
import de.frank.cortex.vpn.WireGuardManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
    val sessions: List<ChatSessionSummary> = emptyList(),
    val isSessionsPanelOpen: Boolean = false,
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val isSpeaking: Boolean = false,
    val speakingMessageId: String? = null,
    val isImproving: Boolean = false,
    val isGeneratingTitle: Boolean = false,
    val contextMode: String = SettingsStore.CONTEXT_MODE_AUTO,
    val responseSize: String = SettingsStore.RESPONSE_SIZE_MEDIUM
)

object ChatCommands {
    private val _newChat = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val newChat: SharedFlow<Unit> = _newChat

    private val _openSessions = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openSessions: SharedFlow<Unit> = _openSessions

    fun requestNewChat() {
        _newChat.tryEmit(Unit)
    }

    fun requestOpenSessions() {
        _openSessions.tryEmit(Unit)
    }
}

class ChatViewModel : ViewModel() {

    private companion object {
        // 2026-07-01: TTS_FIRST_TARGET_CHARS war bei 220 (Bugfix #47325) bestaetigt "super" schnell.
        // Ein spaeterer Umbau erhoehte ihn auf 520 - das machte den Start wieder spuerbar langsamer.
        // Zurueckgesetzt auf den bewaehrten Wert samt der dazugehoerigen (grosszuegigeren) Merge-Regel.
        const val TTS_FIRST_TARGET_CHARS = 220
        // Frank-Feedback 2026-07-01: 720 erzeugte eine hoerbare Luecke zwischen 1. und 2. Absatz
        // (2. Chunk brauchte zu lange zum Synthetisieren). Auf 500 gesenkt - kuerzere Folgechunks
        // sind schneller fertig synthetisiert, bevor der vorherige Chunk zu Ende gespielt ist.
        const val TTS_TARGET_CHARS = 500
        const val TTS_MAX_CHARS = 950
        const val TTS_PREFETCH_AHEAD = 2
        const val TTS_RETRY_ATTEMPTS = 2
        const val TTS_RETRY_SPLIT_MIN_CHARS = 260
        // Default-Backoff wenn Gemini 429 ohne Retry-After-Header meldet.
        const val TTS_RATE_LIMIT_DEFAULT_BACKOFF_MS = 2000L
        const val TTS_RATE_LIMIT_MAX_BACKOFF_MS = 8000L
        // Ein rate-limitierter Chunk wird GEDULDIG wiederholt (nie sofort verworfen), aber nur
        // bis zu dieser Gesamt-Wartezeit. Kommt selbst danach kein Ton durch, ist das Kontingent
        // hoechstwahrscheinlich erschoepft (nicht nur ein kurzer Burst) - dann NICHT minutenlang
        // still weiter-warten, sondern abbrechen und dem Nutzer klar sagen, was los ist.
        // Frank-Vorfall 2026-07-01: bei erschoepftem Free-Tier-Kontingent bekam JEDER Request 429,
        // auch der erste - die alte 120s-Geduld liess die App still haengen (wirkte "kaputt").
        const val TTS_RATE_LIMIT_GIVEUP_MS = 25_000L
        // Nur so viele TTS-Anfragen GLEICHZEITIG - verhindert den 429-Burst von vornherein.
        // 1 reicht: eine Chunk-Wiedergabe dauert laenger als die Synthese des naechsten
        // Chunks, die serielle Synthese bleibt der Wiedergabe also voraus (keine Luecken)
        // und trifft die Rate-Limit-Wand deutlich seltener.
        const val TTS_MAX_CONCURRENT_SYNTH = 1
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val micRecorder = MicRecorder()
    private val pcmPlayer = PcmPlayer()
    private var speakJob: Job? = null
    private var speechGeneration = 0

    // Geteilter Rate-Limit-Zaun: sobald EIN Chunk ein 429 von Gemini-TTS bekommt, warten ALLE
    // gerade laufenden/naechsten Chunk-Synthesen bis zu diesem Zeitpunkt - statt weiter parallel
    // gegen die Bremse zu rennen. Root Cause fuer "Lautsprecher tut bei grossen/neuen Nachrichten
    // teilweise nichts": viele parallele Chunks trafen die Rate-Limit-Wand gleichzeitig, und die
    // (spaeter hinzugefuegte) Retry-Logik hat durch rekursives Aufsplitten sogar noch mehr
    // Anfragen erzeugt statt zu bremsen.
    @Volatile private var ttsRateLimitedUntil: Long = 0L

    // Wird auf true gesetzt, sobald ein Chunk das Rate-Limit-Zeitlimit erschoepft hat (Kontingent
    // wahrscheinlich aufgebraucht). speakResponse liest das, um die Session sauber abzubrechen und
    // dem Nutzer eine klare Meldung zu zeigen, statt still weiter zu warten.
    @Volatile private var ttsRateLimitExhausted = false

    // Begrenzt die GLEICHZEITIGEN Gemini-TTS-Anfragen (siehe TTS_MAX_CONCURRENT_SYNTH).
    private val ttsSemaphore = Semaphore(TTS_MAX_CONCURRENT_SYNTH)

    init {
        // Vorlese-Schalter aus den Einstellungen uebernehmen, damit Chat-Icon + Auto-Vorlesen
        // denselben Stand zeigen wie der "Antwort vorlesen"-Schalter in den Einstellungen.
        _uiState.update { it.copy(ttsEnabled = SettingsStore.ttsEnabled, responseSize = SettingsStore.responseSize) }
        refreshSessions()

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
        val sessionId = state.sessionId

        // User-Nachricht hinzufügen
        val userMsg = ChatMessage(text = text, isUser = true)
        _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true, error = null) }

        viewModelScope.launch {
            val sendStartedAt = System.currentTimeMillis()
            updateSessionsAfterPersist(sessionId, userMsg)
            try {
                if (WireGuardManager.state.value != TunnelState.CONNECTED) {
                    _uiState.update { it.copy(isLoading = false, error = "VPN nicht aktiv") }
                    return@launch
                }

                val request = ChatRequest(
                    text = text,
                    session_id = sessionId,
                    category = state.selectedCategory,
                    title = state.titleOverride.ifBlank { null },
                    context_mode = state.contextMode,
                    context_prompt = buildContextPrompt(state.contextMode, state.responseSize)
                )

                CortexLog.checkpoint(
                    step = "chat_send",
                    intent = "Nachricht an Agent senden",
                    expected = "reply mit action",
                    actual = "warte…",
                    ok = false,
                    ctx = mapOf("text_length" to text.length)
                )

                val agentCallStartedAt = System.currentTimeMillis()
                val response = ApiClient.agentApi().chat(request)
                val agentElapsedMs = System.currentTimeMillis() - agentCallStartedAt

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
                updateSessionsAfterPersist(sessionId, agentMsg)

                CortexLog.checkpoint(
                    step = "chat_send",
                    intent = "Nachricht an Agent senden",
                    expected = "reply mit action",
                    actual = "action=${response.action}",
                    ok = true,
                    ctx = mapOf("stored" to response.stored, "action" to response.action)
                )
                CortexLog.info(
                    "ChatVM",
                    "sendMessage",
                    "Agent-Antwort erhalten",
                    mapOf(
                        "agent_elapsed_ms" to agentElapsedMs,
                        "total_elapsed_ms" to (System.currentTimeMillis() - sendStartedAt),
                        "request_chars" to text.length,
                        "reply_chars" to response.reply.length,
                        "recall_hits" to response.recall_hits
                    )
                )

                // Auto-Vorlesen ist global; die Bubble-ID bleibt der manuellen Replay-Funktion vorbehalten.
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
                isSessionsPanelOpen = false,
                isRecording = false,
                isTranscribing = false,
                isImproving = false,
                isGeneratingTitle = false
            )
        }
        CortexLog.info("ChatVM", "startNewChat", "Neuer Chat gestartet")
        refreshSessions()
    }

    fun openSessionsPanel() {
        _uiState.update { it.copy(isSessionsPanelOpen = true) }
        refreshSessions()
    }

    fun closeSessionsPanel() {
        _uiState.update { it.copy(isSessionsPanelOpen = false) }
    }

    fun selectSession(sessionId: String) {
        stopSpeaking()
        viewModelScope.launch {
            try {
                val messages = withContext(Dispatchers.IO) {
                    ChatSessionStore.loadMessages(sessionId).map { it.toChatMessage() }
                }
                val sessions = withContext(Dispatchers.IO) { ChatSessionStore.listSessions() }
                _uiState.update {
                    it.copy(
                        sessionId = sessionId,
                        messages = messages,
                        sessions = sessions,
                        isSessionsPanelOpen = false,
                        isLoading = false,
                        error = null,
                        titleOverride = "",
                        isRecording = false,
                        isTranscribing = false,
                        isImproving = false,
                        isGeneratingTitle = false
                    )
                }
                CortexLog.info("ChatVM", "selectSession", "Session geöffnet", mapOf("session_id" to sessionId, "messages" to messages.size))
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "selectSession", "Session konnte nicht geladen werden: ${e.message}")
                _uiState.update { it.copy(error = "Session konnte nicht geladen werden: ${e.message}") }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        stopSpeaking()
        viewModelScope.launch {
            try {
                val sessions = withContext(Dispatchers.IO) {
                    ChatSessionStore.deleteSession(sessionId)
                    ChatSessionStore.listSessions()
                }
                _uiState.update { state ->
                    if (state.sessionId == sessionId) {
                        state.copy(
                            sessionId = "android-${UUID.randomUUID()}",
                            messages = emptyList(),
                            sessions = sessions,
                            isLoading = false,
                            error = null,
                            titleOverride = "",
                            isRecording = false,
                            isTranscribing = false,
                            isImproving = false,
                            isGeneratingTitle = false
                        )
                    } else {
                        state.copy(sessions = sessions, error = null)
                    }
                }
                CortexLog.info("ChatVM", "deleteSession", "Session gelöscht", mapOf("session_id" to sessionId))
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "deleteSession", "Session konnte nicht gelöscht werden: ${e.message}")
                _uiState.update { it.copy(error = "Session konnte nicht gelöscht werden: ${e.message}") }
            }
        }
    }

    private fun refreshSessions() {
        viewModelScope.launch {
            val sessions = withContext(Dispatchers.IO) {
                try {
                    ChatSessionStore.listSessions()
                } catch (e: Exception) {
                    CortexLog.error("ChatVM", "refreshSessions", "Session-Liste konnte nicht geladen werden: ${e.message}")
                    emptyList()
                }
            }
            _uiState.update { it.copy(sessions = sessions) }
        }
    }

    private suspend fun updateSessionsAfterPersist(sessionId: String, message: ChatMessage) {
        val sessions = withContext(Dispatchers.IO) {
            try {
                ChatSessionStore.saveMessage(sessionId, message.toStoredMessage())
                ChatSessionStore.listSessions()
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "persistSession", "Nachricht konnte nicht gespeichert werden: ${e.message}")
                emptyList()
            }
        }
        _uiState.update { it.copy(sessions = sessions) }
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

    fun updateResponseSize(size: String) {
        val normalized = when (size) {
            SettingsStore.RESPONSE_SIZE_SHORT,
            SettingsStore.RESPONSE_SIZE_MEDIUM,
            SettingsStore.RESPONSE_SIZE_XL -> size
            else -> SettingsStore.RESPONSE_SIZE_MEDIUM
        }
        SettingsStore.responseSize = normalized
        _uiState.update { it.copy(responseSize = normalized) }
    }

    fun toggleMessageSpeech(messageId: String, text: String) {
        val spokenText = text.trim()
        if (spokenText.isBlank()) {
            CortexLog.warn("ChatVM", "toggleMessageSpeech", "Blasen-TTS ohne Text ignoriert", mapOf("message_id" to messageId))
            return
        }
        if (_uiState.value.isSpeaking && _uiState.value.speakingMessageId == messageId) {
            CortexLog.info("ChatVM", "toggleMessageSpeech", "Blasen-TTS stoppt", mapOf("message_id" to messageId))
            stopSpeaking()
        } else {
            CortexLog.info("ChatVM", "toggleMessageSpeech", "Blasen-TTS startet", mapOf("message_id" to messageId, "message_len" to spokenText.length))
            speakResponse(spokenText, messageId)
        }
    }

    fun prepareMessageShare(messageId: String) {
        val message = _uiState.value.messages.firstOrNull { it.id == messageId && !it.isUser } ?: return
        CortexLog.info("ChatVM", "prepareMessageShare", "Teilen-Platzhalter gedrückt", mapOf("message_len" to message.text.length))
        _uiState.update { it.copy(error = "Teilen wird als Nächstes verknüpft.") }
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
        speechGeneration++
        speakJob?.cancel()
        speakJob = null
        pcmPlayer.stop()
        _uiState.update { it.copy(isSpeaking = false, speakingMessageId = null) }
    }

    override fun onCleared() {
        super.onCleared()
        speakJob?.cancel()
        pcmPlayer.stop()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun speakResponse(text: String, messageId: String? = null) {
        speechGeneration++
        val generation = speechGeneration
        speakJob?.cancel()
        speakJob = viewModelScope.launch {
            _uiState.update { it.copy(isSpeaking = true, speakingMessageId = messageId) }
            // Frischer Session-Start: alten Rate-Limit-Zaun/Flag zuruecksetzen, sonst wuerde eine
            // vorige (erschoepfte) Session diese sofort faelschlich bremsen/abbrechen.
            ttsRateLimitedUntil = 0L
            ttsRateLimitExhausted = false
            try {
                val voice = SettingsStore.ttsVoice
                val rate = SettingsStore.ttsRate
                val chunks = chunkText(text)
                if (chunks.isEmpty()) return@launch
                val ttsStartedAt = System.currentTimeMillis()
                CortexLog.info("ChatVM", "speakResponse", "TTS Pipeline startet",
                    mapOf("len" to text.length, "chunks" to chunks.size, "voice" to voice, "rate" to rate))

                coroutineScope {
                    val pending = mutableMapOf<Int, Deferred<ByteArray?>>()

                    // AudioTrack-Aufbau laeuft PARALLEL zur ersten TTS-Netzwerkanfrage statt
                    // erst danach - die paar ms AudioTrack-Setup sind so "kostenlos", weil sie
                    // sich mit dem viel laengeren Netzwerk-Roundtrip ueberlappen.
                    val playerReady = async(Dispatchers.IO) { pcmPlayer.start(rate) }

                    fun enqueue(index: Int) {
                        if (index in chunks.indices && pending[index] == null) {
                            pending[index] = async(Dispatchers.IO) { synthesizeTtsChunk(chunks[index], voice, index) }
                        }
                    }

                    repeat(minOf(TTS_PREFETCH_AHEAD + 1, chunks.size)) { enqueue(it) }
                    var firstAudioLogged = false

                    chunks.forEachIndexed { index, chunk ->
                        val pcm = pending.remove(index)?.await()
                        enqueue(index + TTS_PREFETCH_AHEAD + 1)
                        if (speechGeneration != generation) return@coroutineScope

                        if (pcm == null || pcm.isEmpty()) {
                            if (ttsRateLimitExhausted) {
                                // Rate-Limit erschoepft: NICHT still weiter-warten, sondern die ganze
                                // Session sauber beenden und dem Nutzer klar sagen, was los ist.
                                val msg = if (firstAudioLogged)
                                    "Gemini-Vorlese-Limit erreicht — die restlichen Absätze konnten nicht vorgelesen werden. Bitte später erneut."
                                else
                                    "Vorlesen momentan nicht möglich — das Gemini-Vorlese-Limit ist erreicht. Bitte später erneut versuchen."
                                CortexLog.warn("ChatVM", "speakResponse", "TTS wegen Rate-Limit gestoppt",
                                    mapOf("index" to index, "any_audio" to firstAudioLogged))
                                _uiState.update { it.copy(error = msg) }
                                return@coroutineScope
                            }
                            // Echter Inhalts-/Format-Fehler bei genau diesem Chunk -> nur diesen
                            // ueberspringen, der Rest wird normal weiter vorgelesen.
                            CortexLog.warn("ChatVM", "speakResponse", "TTS-Chunk übersprungen",
                                mapOf("index" to index, "chunk_len" to chunk.length))
                            return@forEachIndexed
                        }

                        playerReady.await() // no-op sobald der Track laengst steht
                        if (!firstAudioLogged) {
                            CortexLog.info("ChatVM", "speakResponse", "TTS erster Ton bereit",
                                mapOf("elapsed_ms" to (System.currentTimeMillis() - ttsStartedAt), "chunk_len" to chunk.length))
                            firstAudioLogged = true
                        }
                        pcmPlayer.writeAndAwait(pcm)
                    }
                }
                CortexLog.info("ChatVM", "speakResponse", "TTS abgeschlossen",
                    mapOf("elapsed_ms" to (System.currentTimeMillis() - ttsStartedAt), "chunks" to chunks.size))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "speakResponse", "TTS fehlgeschlagen: ${e.message}")
            } finally {
                if (speechGeneration == generation) {
                    pcmPlayer.stop()
                    _uiState.update { it.copy(isSpeaking = false, speakingMessageId = null) }
                }
            }
        }
    }

    private fun buildContextPrompt(contextMode: String, responseSize: String): String? {
        val modePrompt = if (contextMode == SettingsStore.CONTEXT_MODE_AUTO) "" else SettingsStore.contextPrompt(contextMode)
        val sizePrompt = SettingsStore.responseSizePrompt(responseSize)
        return listOf(modePrompt, sizePrompt).filter { it.isNotBlank() }.joinToString("\n\n").ifBlank { null }
    }

    private suspend fun synthesizeTtsChunk(chunk: String, voice: String, index: Int): ByteArray? {
        var lastError: String? = null
        var contentAttempts = 0
        var rateLimitRetries = 0
        var rateLimitWaitedMs = 0L

        while (true) {
            // Geteilter Rate-Limit-Zaun: falls GERADE ein anderer Chunk ein 429 ausgeloest hat,
            // erst abwarten statt sofort erneut gegen dieselbe Bremse zu laufen.
            val gateWaitMs = ttsRateLimitedUntil - System.currentTimeMillis()
            if (gateWaitMs > 0) delay(gateWaitMs)
            try {
                // Semaphore begrenzt die gleichzeitigen Anfragen -> weniger 429 von vornherein.
                return ttsSemaphore.withPermit { ApiClient.geminiTts(chunk, voice) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GeminiRateLimitException) {
                // Ein 429 ist zunaechst VORUEBERGEHEND: den Chunk NICHT sofort verwerfen (sonst
                // fehlen ganze Absaetze - Bug vom 2026-07-01), sondern mit wachsendem Backoff
                // geduldig erneut versuchen. ABER nur bis TTS_RATE_LIMIT_GIVEUP_MS - kommt selbst
                // dann kein Ton, ist das Kontingent erschoepft; dann Flag setzen + aufgeben, damit
                // speakResponse dem Nutzer klar meldet "Limit erreicht" statt still zu haengen.
                lastError = e.message
                val escalated = TTS_RATE_LIMIT_DEFAULT_BACKOFF_MS shl minOf(rateLimitRetries, 2)
                val backoff = (e.retryAfterMs ?: escalated)
                    .coerceIn(TTS_RATE_LIMIT_DEFAULT_BACKOFF_MS, TTS_RATE_LIMIT_MAX_BACKOFF_MS)
                ttsRateLimitedUntil = maxOf(ttsRateLimitedUntil, System.currentTimeMillis() + backoff)
                rateLimitRetries++
                rateLimitWaitedMs += backoff
                CortexLog.warn("ChatVM", "synthesizeTtsChunk", "TTS-Chunk rate-limited, warte ${backoff}ms",
                    mapOf("index" to index, "retry" to rateLimitRetries, "total_wait_ms" to rateLimitWaitedMs, "chunk_len" to chunk.length))
                if (rateLimitWaitedMs >= TTS_RATE_LIMIT_GIVEUP_MS) {
                    CortexLog.error("ChatVM", "synthesizeTtsChunk", "TTS-Chunk nach ${rateLimitWaitedMs}ms Rate-Limit aufgegeben - Kontingent erschoepft?",
                        mapOf("index" to index, "chunk_len" to chunk.length))
                    ttsRateLimitExhausted = true
                    return null
                }
                delay(backoff)
            } catch (e: Exception) {
                lastError = e.message
                contentAttempts++
                CortexLog.warn("ChatVM", "synthesizeTtsChunk", "TTS-Chunk fehlgeschlagen: ${e.message}",
                    mapOf("index" to index, "attempt" to contentAttempts, "chunk_len" to chunk.length))
                if (contentAttempts >= TTS_RETRY_ATTEMPTS) break
                delay(300L * contentAttempts)
            }
        }

        // Hierher kommt man NUR ueber den content-Fehler-`break` (der Rate-Limit-Pfad kehrt oben
        // entweder mit Audio zurueck oder laeuft weiter) - also nur bei echten Inhalts-/Format-
        // Fehlern. Ein Rate-Limit wird NICHT rekursiv aufgesplittet, das wuerde die Anfragenzahl
        // genau dann erhoehen, wenn Gemini schon drosselt.
        if (chunk.length > TTS_RETRY_SPLIT_MIN_CHARS) {
            val retryMax = maxOf(TTS_RETRY_SPLIT_MIN_CHARS, chunk.length / 2)
            val pieces = splitLongTextAtWords(chunk, retryMax)
            if (pieces.size > 1) {
                CortexLog.warn("ChatVM", "synthesizeTtsChunk", "TTS-Chunk wird kleiner erneut versucht",
                    mapOf("index" to index, "pieces" to pieces.size, "chunk_len" to chunk.length, "last_error" to (lastError ?: "unbekannt")))
                val pcms = mutableListOf<ByteArray>()
                for ((pieceIndex, piece) in pieces.withIndex()) {
                    val pcm = synthesizeTtsChunk(piece, voice, index * 100 + pieceIndex) ?: return null
                    pcms += pcm
                }
                return concatPcm(pcms)
            }
        }
        return null
    }

    private fun chunkText(text: String): List<String> {
        val normalized = text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("[ \t]+"), " ")
            .trim()
        if (normalized.isBlank()) return emptyList()

        val units = normalized
            .split(Regex("\n{2,}|\n"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .flatMap { splitParagraphForTts(it) }

        val chunks = mutableListOf<String>()
        var current = ""

        fun flush() {
            if (current.isNotBlank()) {
                chunks.add(current.trim())
                current = ""
            }
        }

        // Bewaehrte, grosszuegige Merge-Regel aus Bugfix #47325: ein Absatz darf ueber "target"
        // hinauswachsen, SOLANGE der bisherige Chunk noch klein ist (< target/2) - das haelt
        // natuerliche Absaetze zusammen statt sie stur bei "target" Zeichen zu kappen.
        units.forEach { unit ->
            val target = if (chunks.isEmpty()) TTS_FIRST_TARGET_CHARS else TTS_TARGET_CHARS
            val candidate = if (current.isBlank()) unit else "$current\n\n$unit"
            if (candidate.length <= target || (current.length < target / 2 && candidate.length <= TTS_MAX_CHARS)) {
                current = candidate
            } else {
                flush()
                current = unit
            }
        }
        flush()
        val first = chunks.firstOrNull()
        return if (first != null && first.length > TTS_FIRST_TARGET_CHARS) {
            splitLongTextAtWords(first, TTS_FIRST_TARGET_CHARS) + chunks.drop(1)
        } else {
            chunks
        }
    }

    private fun splitParagraphForTts(paragraph: String): List<String> {
        if (paragraph.length <= TTS_MAX_CHARS) return listOf(paragraph)
        val sentences = paragraph.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        val result = mutableListOf<String>()
        var current = ""

        fun flush() {
            if (current.isNotBlank()) {
                result.add(current.trim())
                current = ""
            }
        }

        sentences.forEach { sentence ->
            if (sentence.length > TTS_MAX_CHARS) {
                flush()
                result += splitLongTextAtWords(sentence, TTS_MAX_CHARS)
            } else {
                val candidate = if (current.isBlank()) sentence else "$current $sentence"
                if (candidate.length <= TTS_MAX_CHARS) current = candidate else {
                    flush()
                    current = sentence
                }
            }
        }
        flush()
        return result
    }

    private fun splitLongTextAtWords(text: String, maxLen: Int): List<String> {
        val result = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.length > maxLen) {
            val splitAt = remaining.take(maxLen).indexOfLast { it == ' ' }.let { if (it > maxLen / 2) it else maxLen }
            result.add(remaining.take(splitAt).trim())
            remaining = remaining.drop(splitAt).trim()
        }
        if (remaining.isNotBlank()) result.add(remaining)
        return result
    }

    private fun concatPcm(parts: List<ByteArray>): ByteArray {
        val totalBytes = parts.sumOf { it.size }
        val merged = ByteArray(totalBytes)
        var offset = 0
        parts.forEach { part ->
            part.copyInto(merged, destinationOffset = offset)
            offset += part.size
        }
        return merged
    }
}

private fun ChatMessage.toStoredMessage(): StoredChatMessage = StoredChatMessage(
    id = id,
    text = text,
    isUser = isUser,
    action = action,
    category = category,
    title = title,
    recallHits = recallHits,
    stored = stored,
    timestamp = timestamp
)

private fun StoredChatMessage.toChatMessage(): ChatMessage = ChatMessage(
    id = id,
    text = text,
    isUser = isUser,
    action = action,
    category = category,
    title = title,
    recallHits = recallHits,
    options = null,
    stored = stored,
    timestamp = timestamp
)

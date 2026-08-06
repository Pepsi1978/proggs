package de.frank.cortex.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.frank.cortex.audio.MicRecorder
import de.frank.cortex.audio.PcmPlayer
import de.frank.cortex.audio.SpeechAnalyzer
import de.frank.cortex.audio.WhisperHallucinationFilter
import de.frank.cortex.data.ChatSessionStore
import de.frank.cortex.data.ChatSessionSummary
import de.frank.cortex.data.model.*
import de.frank.cortex.data.SettingsStore
import de.frank.cortex.data.StoredChatMessage
import de.frank.cortex.data.UserContextPrompt
import de.frank.cortex.network.ApiClient
import de.frank.cortex.network.ChatStreamException
import de.frank.cortex.network.GeminiRateLimitException
import de.frank.cortex.network.GeminiTtsAccessException
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.vpn.TunnelState
import de.frank.cortex.vpn.WireGuardManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
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

// @Immutable: alle Felder sind val und werden nach der Konstruktion nie mutiert (auch die
// options-Liste nicht) — das garantiert Compose, dass ChatBubble bei unveraenderter Message
// geskippt werden darf, obwohl List<> formal instabil ist.
@androidx.compose.runtime.Immutable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val action: String? = null,
    val category: String? = null,
    val title: String? = null,
    val categories: List<String> = emptyList(),
    val docId: String? = null,
    val memoryText: String? = null,
    val storedText: String? = null,
    val memoryEditable: Boolean = false,
    val recallHits: Int? = null,
    val options: List<ChatOption>? = null,
    val stored: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val responseTimeMs: Long? = null,
    val sourceUsage: List<String>? = null
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
    val contextMode: String = SettingsStore.contextMode,
    val contextModeRevision: Int = SettingsStore.contextModeRevision,
    val responseSize: String = SettingsStore.RESPONSE_SIZE_AUTO,
    val userContextPrompts: List<UserContextPrompt> = emptyList(),
    val improvingContextPromptId: String? = null
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
        // 2026-07-02: Das Zeichen-Aufbau-System (erster Chunk 350, Folgechunks 650) wurde auf
        // Franks Wunsch ENTFERNT — jeder komplette Absatz ist jetzt ein eigener Vorlese-Chunk
        // (siehe chunkText). TTS_MAX_CHARS bleibt als reine API-Sicherheitsgrenze bestehen
        // (nur UEBERLANGE Absaetze werden an Satzgrenzen geteilt, sonst lehnt die TTS-API ab).
        const val TTS_MAX_CHARS = 1000
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

    // Whisper-Anti-Halluzinations-Kette (bugs/desktop/groq-transkription.md §2, 4 Schichten wie TVO):
    // Schicht 1 (Sprachgehalt-Vorfilter) hier vor dem Senden, Schicht 2-4 im Filter.
    private val speechAnalyzer = SpeechAnalyzer()
    private val whisperFilter = WhisperHallucinationFilter()

    private val pcmPlayer = PcmPlayer()
    private val mp3Player = de.frank.cortex.audio.Mp3Player(de.frank.cortex.CortexApp.appContext)
    private val edgeSynth = de.frank.cortex.audio.EdgeTtsSynthesizer()
    private val qwenSynth = de.frank.cortex.tts.QwenTtsSynthesizer()
    // speechGeneration/speakJob werden von Main (stopSpeaking/toggleTts) UND vom IO-Thread
    // (handleDelta -> startStreamingSpeech) mutiert: AtomicInteger + Lock statt plain Int,
    // sonst konnte ein verlorenes Inkrement den Abbruch-Vergleich aushebeln (gestopptes
    // Vorlesen sprach weiter bzw. zwei Pipelines ueberlappten).
    private val speechLock = Any()
    private var speakJob: Job? = null
    private val speechGeneration = java.util.concurrent.atomic.AtomicInteger(0)
    private var lastTitleSource: String? = null
    private var titleAttempt = 0

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

    // Wird gesetzt, wenn Cloud-TTS Zugriff verweigert (403/401 - z.B. API nicht freigeschaltet).
    // Dann keine sinnlosen Retries, sondern klare Meldung an den Nutzer.
    @Volatile private var ttsAccessDenied = false

    // Wortlaut des letzten Fehlers der eigenen Stimme (fehlender Schluessel, abgelehnte Kennung).
    // Alibaba-Fehler betreffen IMMER die ganze Session, nicht nur einen Absatz — deshalb bricht
    // die Pipeline damit ab und zeigt die Meldung, statt jeden Absatz still zu ueberspringen.
    @Volatile private var qwenErrorMessage: String? = null

    // Begrenzt die GLEICHZEITIGEN Gemini-TTS-Anfragen (siehe TTS_MAX_CONCURRENT_SYNTH).
    private val ttsSemaphore = Semaphore(TTS_MAX_CONCURRENT_SYNTH)

    init {
        // Vorlese-Zustand aus dem Store uebernehmen — der Lautsprecher-Knopf im Gespraech ist der
        // EINZIGE Vorlesen-Schalter (der Einstellungen-Schalter wurde 2026-07-02 entfernt).
        _uiState.update {
            it.copy(
                ttsEnabled = SettingsStore.ttsEnabled,
                responseSize = SettingsStore.responseSize,
                userContextPrompts = SettingsStore.userContextPrompts()
            )
        }
        refreshSessions()

        // Kategorien vom Server laden, SOBALD das VPN verbunden ist. Vorher (beim App-Start) ist
        // der Tunnel noch aus, der Aufruf liefe ins Leere — deshalb blieb das Dropdown leer.
        viewModelScope.launch {
            WireGuardManager.state.collect { st ->
                if (st == TunnelState.CONNECTED) {
                    loadCategories()
                    syncSizePromptsWithServer()
                    // Gemerkte Speicher-Auftraege (Offline-Outbox) automatisch nachsenden.
                    flushOutbox()
                    flushPendingSessionEnds()
                }
            }
        }
    }

    // Modus- und A/S/M/XL-Prompts zentral halten: Server = Quelle der Wahrheit,
    // damit das Dashboard DIESELBEN Texte nutzt. Einmalig pro App-Lauf.
    @Volatile private var sizePromptsSynced = false
    private var userContextPromptSyncJob: Job? = null

    private fun UserContextPrompt.toAgentPrompt() = AgentUserContextPrompt(
        id = id,
        text = text,
        enabled = enabled,
        original_text = originalText
    )

    private fun AgentUserContextPrompt.toLocalPrompt() = UserContextPrompt(
        id = id,
        text = text,
        enabled = enabled,
        originalText = original_text
    )

    private fun syncUserContextPromptsToServer(prompts: List<UserContextPrompt>, debounceMs: Long = 600L) {
        if (WireGuardManager.state.value != TunnelState.CONNECTED) {
            // Offline-Aenderung vormerken: beim naechsten Connect schiebt syncSizePromptsWithServer
            // die LOKALEN Prompts zum Server, statt sie vom (alten) Server-Stand ueberschreiben
            // zu lassen — vorher ging jeder Offline-Edit beim naechsten Start still verloren.
            SettingsStore.pendingUserContextPromptSync = true
            return
        }
        userContextPromptSyncJob?.cancel()
        userContextPromptSyncJob = viewModelScope.launch(Dispatchers.IO) {
            if (debounceMs > 0) delay(debounceMs)
            try {
                ApiClient.agentApi().updateConfig(
                    AgentConfigRequest(user_context_prompts = prompts.map { it.toAgentPrompt() })
                )
                SettingsStore.pendingUserContextPromptSync = false
                CortexLog.info("ChatVM", "syncUserContextPrompts", "Kontext-Prompts zum Server synchronisiert",
                    mapOf("count" to prompts.size))
            } catch (e: CancellationException) {
                // Scope stirbt mitten im Sync (App-Ende): vormerken, sonst gewinnt beim
                // naechsten Start der alte Server-Stand und die Aenderung ist weg.
                SettingsStore.pendingUserContextPromptSync = true
                throw e
            } catch (e: Exception) {
                SettingsStore.pendingUserContextPromptSync = true
                CortexLog.warn("ChatVM", "syncUserContextPrompts", "Kontext-Prompt-Sync fehlgeschlagen (wird nachgeholt): ${e.message}")
            }
        }
    }

    private fun syncSizePromptsWithServer() {
        if (sizePromptsSynced) return
        sizePromptsSynced = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cfg = ApiClient.agentApi().getConfig()
                // Den App-Start-Abruf gleich als /config-Zwischenspeicher nutzen: die Einstellungen
                // zeigen die Agenten-Modelle dann beim Oeffnen sofort (Performance 2026-07-12).
                ApiClient.cacheAgentConfig(cfg)
                // Offline getaetigte lokale Aenderungen haben VORRANG: erst hochschieben, dann
                // gilt wieder "Server gewinnt". Ohne diese Reihenfolge ueberschrieb der alte
                // Server-Stand jede Offline-Aenderung still und endgueltig.
                val pushEditablePrompts = SettingsStore.pendingEditablePromptSync
                if (pushEditablePrompts) {
                    ApiClient.agentApi().updateConfig(
                        AgentConfigRequest(
                            size_prompt_auto = SettingsStore.responseSizePrompt("auto"),
                            size_prompt_s = SettingsStore.responseSizePrompt("s"),
                            size_prompt_m = SettingsStore.responseSizePrompt("m"),
                            size_prompt_xl = SettingsStore.responseSizePrompt("xl"),
                            context_prompt_save = SettingsStore.contextPrompt(SettingsStore.CONTEXT_MODE_SAVE),
                            context_prompt_search = SettingsStore.contextPrompt(SettingsStore.CONTEXT_MODE_SEARCH)
                        )
                    )
                    SettingsStore.pendingEditablePromptSync = false
                    CortexLog.info("ChatVM", "syncSizePrompts", "Offline geaenderte Modus-/Groessen-Prompts zum Server nachgesendet")
                }
                if (!pushEditablePrompts && (cfg.size_prompts_custom || cfg.context_prompts_custom)) {
                    // Server hat gepflegte Prompts -> lokal uebernehmen (Server gewinnt).
                    cfg.size_prompts.forEach { (k, v) ->
                        if (k in setOf("auto", "s", "m", "xl") && v.isNotBlank()) SettingsStore.setResponseSizePrompt(k, v)
                    }
                    cfg.context_prompts.forEach { (k, v) ->
                        if (k in setOf(SettingsStore.CONTEXT_MODE_SAVE, SettingsStore.CONTEXT_MODE_SEARCH) && v.isNotBlank()) {
                            SettingsStore.setContextPrompt(k, v)
                        }
                    }
                    CortexLog.info("ChatVM", "syncSizePrompts", "Zentrale Modus- und A/S/M/XL-Prompts vom Server uebernommen")
                }
                val pushUserPrompts = SettingsStore.pendingUserContextPromptSync
                if (pushUserPrompts) {
                    val localPrompts = SettingsStore.userContextPrompts()
                    ApiClient.agentApi().updateConfig(
                        AgentConfigRequest(user_context_prompts = localPrompts.map { it.toAgentPrompt() })
                    )
                    SettingsStore.pendingUserContextPromptSync = false
                    CortexLog.info("ChatVM", "syncSizePrompts", "Offline geaenderte Kontext-Prompts zum Server nachgesendet",
                        mapOf("count" to localPrompts.size))
                }
                if (!pushUserPrompts && cfg.user_context_prompts_custom) {
                    val serverPrompts = cfg.user_context_prompts.map { it.toLocalPrompt() }
                    SettingsStore.setUserContextPrompts(serverPrompts)
                    _uiState.update { it.copy(userContextPrompts = serverPrompts) }
                    CortexLog.info("ChatVM", "syncSizePrompts", "Zentrale Kontext-Prompts vom Server uebernommen",
                        mapOf("count" to serverPrompts.size))
                }
                if (!cfg.size_prompts_custom || !cfg.context_prompts_custom) {
                    // Server noch ohne eigene Prompts -> Franks lokal gepflegte Handy-Prompts EINMALIG hochladen (Seed).
                    ApiClient.agentApi().updateConfig(
                        de.frank.cortex.data.model.AgentConfigRequest(
                            size_prompt_auto = if (!cfg.size_prompts_custom) SettingsStore.responseSizePrompt("auto") else null,
                            size_prompt_s = if (!cfg.size_prompts_custom) SettingsStore.responseSizePrompt("s") else null,
                            size_prompt_m = if (!cfg.size_prompts_custom) SettingsStore.responseSizePrompt("m") else null,
                            size_prompt_xl = if (!cfg.size_prompts_custom) SettingsStore.responseSizePrompt("xl") else null,
                            context_prompt_save = if (!cfg.context_prompts_custom) SettingsStore.contextPrompt(SettingsStore.CONTEXT_MODE_SAVE) else null,
                            context_prompt_search = if (!cfg.context_prompts_custom) SettingsStore.contextPrompt(SettingsStore.CONTEXT_MODE_SEARCH) else null
                        )
                    )
                    CortexLog.info("ChatVM", "syncSizePrompts", "Fehlende Handy-Prompts als zentrale Quelle zum Server geladen (Seed)")
                }
                val localUserPrompts = SettingsStore.userContextPrompts()
                if (!cfg.user_context_prompts_custom && localUserPrompts.isNotEmpty()) {
                    ApiClient.agentApi().updateConfig(
                        AgentConfigRequest(user_context_prompts = localUserPrompts.map { it.toAgentPrompt() })
                    )
                    CortexLog.info("ChatVM", "syncSizePrompts", "Lokale Kontext-Prompts als zentrale Quelle zum Server geladen",
                        mapOf("count" to localUserPrompts.size))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                sizePromptsSynced = false   // naechster VPN-Connect versucht es erneut
                CortexLog.warn("ChatVM", "syncSizePrompts", "Prompt-Sync fehlgeschlagen: ${e.message}")
            }
        }
    }

    fun toggleRecording() {
        CortexLog.info("ChatVM", "toggleRecording", "toggleRecording aufgerufen, isRecording=${micRecorder.isRecording()}")
        // Waehrend die Transkription des letzten Stopps laeuft, weitere Tipps ignorieren:
        // ein schneller Doppel-Tipp startete sonst sofort eine NEUE ungewollte Aufnahme
        // (der Recorder war schon gestoppt -> Start-Zweig), waehrend die erste noch lief.
        if (_uiState.value.isTranscribing) return
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

                    // Anti-Halluzinations-Kette (4 Schichten, bugs/desktop/groq-transkription.md §2).
                    // Schicht 1: Sprachgehalt-Vorfilter — reine Stille gar nicht erst an Groq senden
                    // (Whisper halluziniert Floskeln bei Stille MIT hoher Confidence). analysis == null
                    // (Decode-Fehler) -> NICHT filtern, trotzdem senden (funktionserhaltend).
                    val analysis = withContext(Dispatchers.IO) { speechAnalyzer.analyze(wavBytes) }
                    if (analysis != null && analysis.voicedMs < SpeechAnalyzer.MIN_SPEECH_MS) {
                        _uiState.update { it.copy(isTranscribing = false) }
                        CortexLog.info(
                            "ChatVM", "toggleRecording",
                            "Schicht 1: Aufnahme ohne Sprachgehalt (${analysis.voicedMs} ms laut " +
                                "< ${SpeechAnalyzer.MIN_SPEECH_MS} ms) — nicht an Groq gesendet"
                        )
                        return@launch
                    }

                    // Schicht 2-4: Confidence-Gate + Audio-Abgleich + Floskel-Blocklist.
                    val result = ApiClient.groqTranscribe(wavBytes)
                    val filteredText = whisperFilter.filter(result, analysis)
                    if (filteredText.isNotBlank()) {
                        _uiState.update { it.copy(isTranscribing = false) }
                        // Text wird über Callback ans UI zurückgegeben
                        _transcribedText.tryEmit(filteredText)
                    } else {
                        _uiState.update { it.copy(isTranscribing = false) }
                        CortexLog.info("ChatVM", "toggleRecording", "Stille/Halluzination erkannt — nichts eingefügt")
                    }
                } catch (e: CancellationException) {
                    throw e
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

    fun loadCategories() {
        viewModelScope.launch {
            try {
                if (WireGuardManager.state.value != TunnelState.CONNECTED) return@launch
                val response = ApiClient.agentApi().getCategories()
                _uiState.update { it.copy(categories = response.categories) }
                CortexLog.info("ChatVM", "loadCategories", "Kategorien geladen", mapOf("count" to response.categories.size))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "loadCategories", "Fehler: ${e.message}")
            }
        }
    }

    fun createCategory(name: String) {
        viewModelScope.launch {
            try {
                if (WireGuardManager.state.value != TunnelState.CONNECTED) {
                    // Sichtbares Feedback statt stillem Abbruch: der Nutzer glaubte sonst,
                    // die Kategorie sei angelegt worden.
                    _uiState.update { it.copy(error = "VPN nicht aktiv — Kategorie konnte nicht angelegt werden") }
                    return@launch
                }
                val response = ApiClient.agentApi().createCategory(CreateCategoryRequest(name))
                // Kanonischen key aus der Antwort uebernehmen (Server normalisiert evtl.), damit der
                // ausgewaehlte Kategorie-String exakt dem entspricht, den by-category filtert.
                val key = response.key?.takeIf { it.isNotBlank() } ?: name
                loadCategories()
                _uiState.update { it.copy(selectedCategory = key) }
                CortexLog.info("ChatVM", "createCategory", "Kategorie erstellt: $key")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "createCategory", "Fehler: ${e.message}")
                _uiState.update { it.copy(error = "Kategorie konnte nicht erstellt werden: ${e.message}") }
            }
        }
    }

    /** @return true, wenn die Nachricht angenommen wurde (Eingabefeld darf geleert werden);
     *  false bei Ablehnung (Entwurf im Eingabefeld behalten). */
    fun sendMessage(text: String): Boolean {
        if (text.isBlank()) return true
        // Server-Sicherheitsdeckel statt hartkodierter 500k (Fallback 500_000, wenn /config-Cache leer).
        val maxChars = ApiClient.cachedAgentConfig()?.limits?.agent?.chat_text_max_chars ?: 500_000
        if (text.length > maxChars) {
            _uiState.update { it.copy(error = "Text zu lang (max. ${String.format(java.util.Locale.GERMANY, "%,d", maxChars)} Zeichen)") }
            return false
        }

        val state = _uiState.value
        // Offline UND Nicht-Outbox-Modus (z.B. Suche/Auto/Smalltalk): der Auftrag kann weder gesendet
        // noch — wie Speicher-/Regel-Auftraege — gemerkt werden. Daher NICHT als gesendete Nachricht
        // darstellen/persistieren und den Entwurf im Eingabefeld behalten (analog zur Laengen-Ablehnung).
        val offlineNonOutbox = WireGuardManager.state.value != TunnelState.CONNECTED &&
            state.contextMode !in setOf(SettingsStore.CONTEXT_MODE_SAVE, SettingsStore.CONTEXT_MODE_RULE)
        if (offlineNonOutbox) {
            _uiState.update { it.copy(error = "VPN nicht aktiv") }
            return false
        }

        val sessionId = state.sessionId
        val isFirstMessage = state.messages.isEmpty()

        // User-Nachricht hinzufügen
        val userMsg = ChatMessage(text = text, isUser = true)
        _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true, error = null) }

        viewModelScope.launch {
            val sendStartedAt = System.currentTimeMillis()
            updateSessionsAfterPersist(sessionId, userMsg)
            // Sessions-Titel = KI-Zusammenfassung der INTENTION der ersten Frage (Frank-Wunsch
            // 2026-07-02), im Hintergrund via gemini-3.1-flash-lite. Fallback: Roh-Text bleibt.
            if (isFirstMessage) generateSessionIntentTitle(sessionId, text)
            // Vor dem try deklariert, damit die Abbruch-Pfade unten die TEIL-Antwort noch
            // persistieren koennen (Frank-Wunsch 2026-07-02: halbe Streaming-Antworten duerfen
            // nach Session-Wechsel nicht verloren gehen).
            var streamedMsgId: String? = null
            val rawAccum = StringBuilder()
            var firstResponseElapsedMs: Long? = null
            try {
                if (WireGuardManager.state.value != TunnelState.CONNECTED) {
                    // Mutierende Speicher- und Regelauftraege gehen ohne VPN nicht verloren,
                    // sondern werden beim naechsten Tunnel-Aufbau im urspruenglichen Modus gesendet.
                    if (state.contextMode in setOf(SettingsStore.CONTEXT_MODE_SAVE, SettingsStore.CONTEXT_MODE_RULE)) {
                        // ERST den One-Shot-Modus zuruecksetzen (erhoeht die globale Revision),
                        // DANN den Auftrag mit der frischen Revision einreihen. Umgekehrt trug
                        // das Item immer die schon ueberholte Revision und flushOutbox verwarf
                        // jeden offline gemerkten Regel-Auftrag als "veraltet" — stiller
                        // Datenverlust trotz gegenteiliger Zusage in der Chat-Notiz.
                        resetOneShotContextMode(state.contextMode, state.contextModeRevision)
                        val item = de.frank.cortex.data.OutboxItem(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            text = text,
                            category = state.selectedCategory,
                            title = state.titleOverride.take(200).ifBlank { null },
                            contextMode = state.contextMode,
                            contextModeRevision = SettingsStore.contextModeRevision,
                            responseSize = state.responseSize,
                            requestId = UUID.randomUUID().toString(),
                            createdAt = System.currentTimeMillis()
                        )
                        val queued = withContext(Dispatchers.IO) {
                            try {
                                ChatSessionStore.enqueueOutbox(item)
                                true
                            } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
                                CortexLog.error("ChatVM", "outbox", "Einreihen fehlgeschlagen: ${e.message}")
                                false
                            }
                        }
                        if (queued) {
                            val queuedKind = if (state.contextMode == SettingsStore.CONTEXT_MODE_RULE) "Regel-Auftrag" else "Speicher-Auftrag"
                            CortexLog.info("ChatVM", "outbox", "$queuedKind gemerkt (kein VPN)",
                                mapOf("chars" to text.length, "session_id" to sessionId, "mode" to state.contextMode))
                            val notice = ChatMessage(
                                text = "Kein VPN — dein $queuedKind ist gemerkt und wird automatisch gesendet, sobald der Tunnel steht.",
                                isUser = false,
                                action = "outbox"
                            )
                            _uiState.update {
                                if (it.sessionId != sessionId) it.copy(isLoading = false)
                                else it.copy(isLoading = false, messages = it.messages + notice)
                            }
                            // Notiz auch persistieren: nach Session-Wechsel/Neustart stand die
                            // eigene Nachricht sonst ohne jede Erklaerung in der Historie.
                            // (Modus-Reset passierte bereits VOR dem Einreihen, siehe oben.)
                            updateSessionsAfterPersist(sessionId, notice)
                        } else {
                            // Einreihen fehlgeschlagen: den zuvor (fuer die frische Revision) zurueck-
                            // gesetzten One-Shot-Modus wiederherstellen, damit Franks Speicher-/Regel-
                            // Wahl nicht still verloren geht und der naechste Versuch wieder im richtigen
                            // Modus laeuft (die Reset-vor-Einreihen-Reihenfolge bleibt bewusst erhalten).
                            updateContextMode(state.contextMode)
                            _uiState.update { it.copy(isLoading = false, error = "VPN nicht aktiv — merken fehlgeschlagen") }
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "VPN nicht aktiv") }
                    }
                    return@launch
                }

                val request = ChatRequest(
                    text = text,
                    session_id = sessionId,
                    category = state.selectedCategory,
                    // Server validiert title mit max. 200 Zeichen (422 bei mehr).
                    title = state.titleOverride.take(200).ifBlank { null },
                    context_mode = state.contextMode,
                    context_mode_revision = state.contextModeRevision,
                    context_prompt = buildContextPrompt(state.contextMode, state.responseSize),
                    response_size = state.responseSize,
                    // EIN Key pro Nutzer-Intent: Stream und /chat-Fallback nutzen dasselbe
                    // request-Objekt -> der Server dedupliziert (keine Doppel-Verarbeitung).
                    request_id = UUID.randomUUID().toString()
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

                // === Streaming (Frank-Wunsch 2026-07-02) ===
                // Die Antwort waechst live in der Bubble; das Vorlesen startet, sobald der ERSTE
                // vollstaendige Absatz da ist (nie bei Einzelwoertern — sonst bricht TTS zu frueh ab).
                // Schlaegt das Streaming fehl BEVOR etwas ankam -> klassischer /chat als Fallback.
                var spokenUpTo = 0
                var ttsChannel: Channel<String>? = null
                val ttsWanted = SettingsStore.ttsEnabled

                // Vorwaerts-Cursor fuer die Absatzgrenzen-Suche: lastIndexOf ueber den GANZEN
                // Puffer war bei Antworten ohne "\n\n" (lange Bullet-Listen) O(n) pro Delta und
                // damit O(n^2) pro Antwort — jetzt wird jede Pufferstelle nur einmal gescannt.
                var boundaryScanFrom = 0

                fun handleDelta(delta: String) {
                    // Eine NEUE "\n\n"-Grenze kann nur entstehen, wenn dieses Delta ein '\n'
                    // enthaelt oder direkt an ein '\n' anschliesst.
                    val boundaryPossible = ttsWanted &&
                        (delta.indexOf('\n') >= 0 ||
                            (rawAccum.isNotEmpty() && rawAccum[rawAccum.length - 1] == '\n'))
                    rawAccum.append(delta)
                    val current = rawAccum.toString()
                    if (firstResponseElapsedMs == null && delta.isNotBlank()) {
                        // Freeze the end-to-end latency when the first visible answer text arrives.
                        firstResponseElapsedMs = System.currentTimeMillis() - sendStartedAt
                    }
                    val msgId = streamedMsgId
                    if (msgId == null) {
                        val m = ChatMessage(
                            text = current,
                            isUser = false,
                            responseTimeMs = firstResponseElapsedMs
                        )
                        streamedMsgId = m.id
                        // Nur in die UI haengen, wenn Frank noch in DIESER Session ist — sonst
                        // erschien die Antwort der alten Frage mitten im neuen/fremden Chat.
                        _uiState.update {
                            if (it.sessionId != sessionId) it
                            else it.copy(messages = it.messages + m, isLoading = false)
                        }
                    } else {
                        _uiState.update { st ->
                            when {
                                st.sessionId != sessionId -> st
                                // Kehrt Frank WAEHREND des Streams in die Ursprungs-Session
                                // zurueck, fehlt die Bubble (selectSession laedt aus dem Store,
                                // wo die Teil-Antwort noch nicht liegt) — dann wieder anhaengen
                                // statt ins Leere zu mappen.
                                st.messages.none { it.id == msgId } -> st.copy(
                                    messages = st.messages + ChatMessage(
                                        id = msgId,
                                        text = current,
                                        isUser = false,
                                        responseTimeMs = firstResponseElapsedMs
                                    )
                                )
                                else -> st.copy(messages = st.messages.map {
                                    if (it.id == msgId) it.copy(
                                        text = current,
                                        responseTimeMs = firstResponseElapsedMs
                                    ) else it
                                })
                            }
                        }
                    }
                    if (boundaryPossible) {
                        var boundary = -1
                        var idx = rawAccum.indexOf("\n\n", maxOf(spokenUpTo, boundaryScanFrom))
                        while (idx >= 0) {
                            boundary = idx
                            idx = rawAccum.indexOf("\n\n", idx + 2)
                        }
                        // Ein '\n' am Pufferende kann mit dem naechsten Delta noch eine Grenze bilden.
                        boundaryScanFrom = maxOf(0, rawAccum.length - 1)
                        if (boundary > spokenUpTo) {
                            val ready = rawAccum.substring(spokenUpTo, boundary).trim()
                            spokenUpTo = boundary + 2
                            // LIVE pruefen (nicht nur den Sende-Beginn-Snapshot ttsWanted):
                            // schaltet Frank das Vorlesen aus, BEVOR der erste Absatz fertig
                            // ist, darf hier keine neue Vorlese-Pipeline mehr starten.
                            if (ready.isNotBlank() && SettingsStore.ttsEnabled) {
                                val ch = ttsChannel ?: startStreamingSpeech().also { ttsChannel = it }
                                ch.trySend(localTtsClean(ready))
                            }
                        }
                    }
                }

                val response = try {
                    ApiClient.chatStream(request, ::handleDelta)
                } catch (e: CancellationException) {
                    ttsChannel?.close()
                    throw e
                } catch (e: Exception) {
                    ttsChannel?.close()
                    val streamFailure = e as? ChatStreamException
                    val safeFallback = rawAccum.isEmpty() &&
                        streamFailure?.deltaReceived != true && streamFailure?.doneReceived != true &&
                        !request.request_id.isNullOrBlank()
                    if (safeFallback) {
                        // Noch nichts empfangen -> gefahrloser Fallback auf den bisherigen Weg.
                        CortexLog.warn("ChatVM", "sendMessage", "Streaming fehlgeschlagen -> klassischer /chat: ${e.message}",
                            mapOf("request_id" to request.request_id.orEmpty().take(12),
                                "headers" to (streamFailure?.headersReceived ?: false),
                                "events" to (streamFailure?.anyEventReceived ?: false),
                                "same_request_id" to true))
                        ttsChannel = null
                        // Fallback-Client mit 600s-Dach: /chat sendet bis zur fertigen Antwort
                        // KEIN Byte — lange Server-Laeufe sprengten das 120s-readTimeout. Vorher
                        // tote VPN-/Mobilfunk-Routen aus dem gemeinsamen OkHttp-Pool entfernen.
                        ApiClient.resetAgentConnections("chat_stream_fallback")
                        ApiClient.agentChatFallbackApi().chat(request)
                    } else {
                        throw e
                    }
                }
                val agentElapsedMs = System.currentTimeMillis() - agentCallStartedAt

                // Rest-Text nach dem letzten vollstaendigen Absatz vorlesen + Pipeline abschliessen.
                ttsChannel?.let { ch ->
                    val rest = rawAccum.substring(spokenUpTo.coerceAtMost(rawAccum.length)).trim()
                    if (rest.isNotBlank()) ch.trySend(localTtsClean(rest))
                    ch.close()
                }

                val existingId = streamedMsgId
                val agentMsg = response.toChatMessage(
                    id = existingId ?: UUID.randomUUID().toString(),
                    responseTimeMs = firstResponseElapsedMs ?: (System.currentTimeMillis() - sendStartedAt)
                )

                _uiState.update { st ->
                    // Session-Guard: Antwort nur in die UI der URSPRUNGS-Session haengen —
                    // persistiert wird sie unten ohnehin korrekt dorthin. Fehlt die Streaming-
                    // Bubble (Session zwischenzeitlich verlassen und wieder geoeffnet), wird
                    // die finale Antwort ANGEHAENGT statt ins Leere gemappt.
                    if (st.sessionId != sessionId) st
                    else st.copy(
                        messages = if (existingId != null && st.messages.any { it.id == existingId })
                            st.messages.map { if (it.id == existingId) agentMsg else it }
                        else
                            st.messages + agentMsg,
                        isLoading = false,
                        titleOverride = if (response.action == "store") "" else st.titleOverride
                    )
                }
                resetOneShotContextMode(state.contextMode, state.contextModeRevision)
                updateSessionsAfterPersist(sessionId, agentMsg)

                // Sichtbare Meldung, wenn das Server-Kontextfenster (40 Nachrichten) erstmals
                // ueberschritten wurde (Frank-Wunsch 2026-07-02) — kommt pro Session genau einmal.
                if (response.context_limit_reached) {
                    val notice = ChatMessage(
                        text = "⚠️ Kontextgrenze erreicht — ältere Nachrichten dieses Gesprächs fließen ab jetzt nicht mehr in die Antworten ein.",
                        isUser = false,
                        action = "context_limit"
                    )
                    _uiState.update {
                        if (it.sessionId != sessionId) it
                        else it.copy(messages = it.messages + notice)
                    }
                    updateSessionsAfterPersist(sessionId, notice)
                }

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

                // Auto-Vorlesen: Wurde schon waehrend des Streams absatzweise vorgelesen (ttsChannel),
                // NICHT erneut starten. Sonst (kurze Antwort ohne Absatz-Grenze / klassischer Fallback)
                // wie bisher die komplette Antwort vorlesen.
                if (SettingsStore.ttsEnabled && response.reply.isNotBlank() && ttsChannel == null) {
                    speakResponse(response.reply)
                }

            } catch (e: CancellationException) {
                // App/ViewModel wird beendet: die bereits gestreamte TEIL-Antwort trotzdem sichern.
                persistPartialStreamedReply(
                    sessionId,
                    streamedMsgId,
                    rawAccum,
                    firstResponseElapsedMs
                )
                throw e
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "sendMessage", "Fehler: ${e.message}")
                // Halbe Streaming-Antwort NICHT verlieren: was schon ankam, wird als normale
                // Nachricht in der Session-Historie gespeichert (Frank-Wunsch 2026-07-02).
                // Bei Erfolg ueberschreibt die finale Antwort denselben Datensatz (gleiche id,
                // CONFLICT_REPLACE) — hier bleibt eben der Teilstand erhalten.
                persistPartialStreamedReply(
                    sessionId,
                    streamedMsgId,
                    rawAccum,
                    firstResponseElapsedMs
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Fehler: ${e.message}"
                    )
                }
            }
        }
        return true
    }

    // Schutz gegen parallele Flush-Laeufe (mehrere CONNECTED-Events kurz nacheinander).
    @Volatile private var outboxFlushing = false
    @Volatile private var sessionEndFlushing = false

    /** Sendet gemerkte Speicher-Auftraege (Offline-Outbox) nach, sobald der Tunnel steht.
     *  Sequenziell + idempotent (request_id aus der Einreihung — der Server dedupliziert,
     *  selbst wenn ein Flush-Versuch nach dem Senden abbricht und wiederholt wird). */
    private fun flushOutbox() {
        if (outboxFlushing) return
        outboxFlushing = true
        viewModelScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    try {
                        ChatSessionStore.listOutbox()
                    } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
                        CortexLog.error("ChatVM", "outboxFlush", "Outbox nicht lesbar: ${e.message}")
                        emptyList()
                    }
                }
                if (items.isEmpty()) return@launch
                CortexLog.info("ChatVM", "outboxFlush", "Sende gemerkte Speicher-Auftraege", mapOf("count" to items.size))
                for (item in items) {
                    if (WireGuardManager.state.value != TunnelState.CONNECTED) break
                    // BEWUSST KEINE Revisions-Pruefung mehr: Der globale Revisionszaehler steigt
                    // bei JEDEM Moduswechsel (auch dem Auto-Reset nach dem Einreihen) — der
                    // fruehere Gleichheits-Vergleich verwarf damit praktisch jeden gemerkten
                    // Regel-Auftrag als "veraltet" (stiller Datenverlust trotz Chat-Zusage).
                    // Ein eingereihtes Item IST ein bestaetigter Sende-Wunsch; die Bindung von
                    // Regel-BESTAETIGUNGEN an den unveraenderten R-Modus macht der Server selbst
                    // (context_mode_revision im Request bleibt dafuer erhalten).
                    try {
                        // Fallback-Client (600s-Dach): auch Outbox-Auftraege koennen serverseitig
                        // lange laufen — das 120s-readTimeout des Standard-Clients riss sonst ab.
                        val response = ApiClient.agentChatFallbackApi().chat(
                            ChatRequest(
                                text = item.text,
                                session_id = item.sessionId,
                                category = item.category,
                                title = item.title,
                                context_mode = item.contextMode,
                                context_mode_revision = item.contextModeRevision,
                                context_prompt = buildContextPrompt(item.contextMode, item.responseSize),
                                response_size = item.responseSize,
                                request_id = item.requestId,
                                memory_edit = item.memoryEdit,
                                memory_doc_id = item.memoryDocId,
                                memory_categories = item.memoryCategories
                            )
                        )
                        if (response.action == "error") {
                            // Server hat den Auftrag verarbeitet und ABGELEHNT: erneutes Senden
                            // schluege deterministisch wieder fehl — die Poison-Message blockierte
                            // frueher fuer immer ALLE nachfolgenden Auftraege (break ohne Verwerfen).
                            withContext(Dispatchers.IO) { ChatSessionStore.deleteOutboxItem(item.id) }
                            val failNotice = ChatMessage(
                                text = "❌ Gemerkter Auftrag wurde vom Server abgelehnt und verworfen: ${response.reply.ifBlank { "unbekannter Grund" }}",
                                isUser = false,
                                action = "outbox_error"
                            )
                            updateSessionsAfterPersist(item.sessionId, failNotice)
                            if (_uiState.value.sessionId == item.sessionId) {
                                _uiState.update { it.copy(messages = it.messages + failNotice) }
                            }
                            CortexLog.warn("ChatVM", "outboxFlush", "Auftrag vom Server abgelehnt — verworfen",
                                mapOf("session_id" to item.sessionId))
                            continue
                        }
                        val agentMsg = response.toChatMessage()
                        updateSessionsAfterPersist(item.sessionId, agentMsg)
                        // Ist die betroffene Session gerade offen: Antwort auch live anzeigen.
                        if (_uiState.value.sessionId == item.sessionId) {
                            _uiState.update { it.copy(messages = it.messages + agentMsg) }
                        }
                        withContext(Dispatchers.IO) { ChatSessionStore.deleteOutboxItem(item.id) }
                        CortexLog.info("ChatVM", "outboxFlush", "Speicher-Auftrag nachgesendet",
                            mapOf("action" to response.action, "stored" to response.stored))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Rest bleibt in der Outbox — naechster Tunnel-Aufbau versucht es erneut.
                        CortexLog.warn("ChatVM", "outboxFlush", "Nachsenden fehlgeschlagen (Rest bleibt gemerkt): ${e.message}")
                        break
                    }
                }
            } finally {
                outboxFlushing = false
            }
        }
    }

    /** Holt Plus-/Neue-Session-Abschlüsse nach, die ohne VPN oder bei Serverfehler gemerkt wurden. */
    private fun flushPendingSessionEnds() {
        if (sessionEndFlushing) return
        sessionEndFlushing = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = try {
                    ChatSessionStore.listPendingSessionEnds()
                } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
                    CortexLog.error("ChatVM", "sessionEndFlush", "Session-Ende-Outbox nicht lesbar: ${e.message}")
                    emptyList()
                }
                if (items.isEmpty()) return@launch
                CortexLog.info("ChatVM", "sessionEndFlush", "Sende gemerkte Session-Enden", mapOf("count" to items.size))
                for (item in items) {
                    if (WireGuardManager.state.value != TunnelState.CONNECTED) break
                    try {
                        val response = ApiClient.agentApi().endSession(EndSessionRequest(session_id = item.sessionId))
                        if (!response.ok) throw Exception(response.message ?: "Server meldet ok=false")
                        ChatSessionStore.deletePendingSessionEnd(item.sessionId)
                        CortexLog.info("ChatVM", "sessionEndFlush", "Gemerkte Server-Session beendet",
                            mapOf("session_id" to item.sessionId))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        CortexLog.warn("ChatVM", "sessionEndFlush", "Session-Ende-Nachholung fehlgeschlagen (Rest bleibt gemerkt): ${e.message}",
                            mapOf("session_id" to item.sessionId))
                        break
                    }
                }
            } finally {
                sessionEndFlushing = false
            }
        }
    }

    private fun enqueuePendingSessionEnd(sessionId: String, reason: String) {
        try {
            ChatSessionStore.enqueueSessionEnd(sessionId)
            CortexLog.warn("ChatVM", "sessionEnd", "Server-Session-Ende gemerkt: $reason",
                mapOf("session_id" to sessionId))
        } catch (e: Exception) {
            CortexLog.error("ChatVM", "sessionEnd", "Session-Ende konnte nicht gemerkt werden: ${e.message}",
                mapOf("session_id" to sessionId))
            _uiState.update { it.copy(error = "Altes Gespräch fällt auf den 10-Minuten-Fallback zurück: ${e.message}") }
        }
    }

    /** Sichert eine mittendrin abgebrochene Streaming-Antwort in der Session-Historie.
     *  NonCancellable: laeuft auch dann zu Ende, wenn die aufrufende Coroutine gerade
     *  gecancelt wird (z.B. App-Ende) — genau dann ist das Sichern am wichtigsten. */
    private suspend fun persistPartialStreamedReply(
        sessionId: String,
        messageId: String?,
        rawAccum: StringBuilder,
        responseTimeMs: Long?
    ) {
        if (messageId == null) return
        val partial = rawAccum.toString().trim()
        if (partial.isBlank()) return
        withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
            try {
                ChatSessionStore.saveMessage(
                    sessionId,
                    StoredChatMessage(
                        id = messageId,
                        text = partial,
                        isUser = false,
                        action = null,
                        category = null,
                        title = null,
                        recallHits = null,
                        stored = false,
                        timestamp = System.currentTimeMillis(),
                        responseTimeMs = responseTimeMs
                    )
                )
                CortexLog.info("ChatVM", "persistPartial", "Abgebrochene Streaming-Antwort gesichert",
                    mapOf("session_id" to sessionId, "chars" to partial.length))
            } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
                CortexLog.warn("ChatVM", "persistPartial", "Teil-Antwort konnte nicht gesichert werden: ${e.message}")
            }
        }
        refreshSessions()
    }

    fun sendOption(option: ChatOption) {
        sendMessage(option.send)
    }

    fun addUserContextPrompt(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(error = "Prompt ist leer") }
            return
        }
        // UI-State ist die Quelle (nicht der Store): waehrend ein Tipp-Debounce laeuft, ist der
        // Store veraltet — ein Lesen von dort haette den gerade getippten Text verworfen.
        val updated = _uiState.value.userContextPrompts + UserContextPrompt(
            id = UUID.randomUUID().toString(),
            text = trimmed,
            enabled = true,
            originalText = trimmed
        )
        SettingsStore.setUserContextPrompts(updated)
        _uiState.update { it.copy(userContextPrompts = updated) }
        syncUserContextPromptsToServer(updated, debounceMs = 0L)
        CortexLog.checkpoint(
            step = "context_prompt_add",
            intent = "Zusatz-Prompt speichern und bei Chat-Anfragen mitsenden",
            expected = "prompt_saved",
            actual = "prompt_saved",
            ok = true,
            ctx = mapOf("prompts" to updated.size, "chars" to trimmed.length)
        )
    }

    private var promptPersistJob: Job? = null

    fun updateUserContextPrompt(id: String, text: String) {
        // Pro TASTENDRUCK nur den UI-State aktualisieren; die JSON-Serialisierung der ganzen
        // Liste in die SharedPreferences (+ Server-Sync) laeuft entprellt hinterher — vorher
        // ruckelte die Eingabe bei mehreren langen Prompts spuerbar.
        val updated = _uiState.value.userContextPrompts.map {
            if (it.id == id) it.copy(text = text) else it
        }
        _uiState.update { it.copy(userContextPrompts = updated) }
        promptPersistJob?.cancel()
        promptPersistJob = viewModelScope.launch {
            delay(400)
            val current = _uiState.value.userContextPrompts
            SettingsStore.setUserContextPrompts(current)
            syncUserContextPromptsToServer(current, debounceMs = 0L)
        }
    }

    fun toggleUserContextPrompt(id: String, enabled: Boolean) {
        val updated = _uiState.value.userContextPrompts.map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
        SettingsStore.setUserContextPrompts(updated)
        _uiState.update { it.copy(userContextPrompts = updated) }
        syncUserContextPromptsToServer(updated, debounceMs = 0L)
    }

    fun improveUserContextPrompt(id: String) {
        val prompt = _uiState.value.userContextPrompts.firstOrNull { it.id == id } ?: return
        val original = prompt.text.trim()
        if (original.isBlank()) {
            _uiState.update { it.copy(error = "Prompt ist leer") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(improvingContextPromptId = id, error = null) }
            try {
                CortexLog.checkpoint(
                    step = "context_prompt_gemini_improve",
                    intent = "Kontext-Prompt als Zusatzanweisung fuer Agenten verbessern",
                    expected = "verbesserter Prompt ersetzt Prompt-Karte",
                    actual = "warte…",
                    ok = false,
                    ctx = mapOf("prompt_id" to id, "input_len" to original.length)
                )
                val extra = """
                    Zusatzaufgabe: Dieser Text ist kein normaler Chat-Text, sondern eine zusätzliche Anweisung,
                    die später zusammen mit einer eigentlichen Nutzerfrage an einen Agenten gesendet wird.
                    Optimiere ihn deshalb so, dass der Agent ihn eindeutig als dauerhaft geltende Zusatzanweisung
                    zur folgenden Frage versteht. Beginne den verbesserten Text mit: "Zusätzliche Anweisung an den Agenten:"
                    und formuliere danach die Anweisung klar, vollständig, direkt und handlungsorientiert.
                """.trimIndent()
                val improved = withContext(Dispatchers.IO) { ApiClient.geminiImprove(original, extra) }.trim()
                if (improved.isBlank()) throw IllegalStateException("Gemini hat leeren Text zurückgegeben")
                val updated = _uiState.value.userContextPrompts.map {
                    if (it.id == id) it.copy(text = improved, originalText = it.originalText ?: original) else it
                }
                SettingsStore.setUserContextPrompts(updated)
                _uiState.update { it.copy(userContextPrompts = updated, improvingContextPromptId = null) }
                syncUserContextPromptsToServer(updated, debounceMs = 0L)
                CortexLog.checkpoint(
                    step = "context_prompt_gemini_improve",
                    intent = "Kontext-Prompt als Zusatzanweisung fuer Agenten verbessern",
                    expected = "verbesserter Prompt ersetzt Prompt-Karte",
                    actual = "output_len=${improved.length}",
                    ok = true,
                    ctx = mapOf("prompt_id" to id, "input_len" to original.length, "output_len" to improved.length)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "improveUserContextPrompt", "Gemini-Kontext-Prompt-Verbesserung fehlgeschlagen: ${e.message}")
                _uiState.update {
                    it.copy(
                        improvingContextPromptId = null,
                        error = "Gemini konnte den Prompt nicht verbessern: ${e.message}"
                    )
                }
            }
        }
    }

    fun restoreUserContextPromptOriginal(id: String) {
        val prompts = _uiState.value.userContextPrompts
        val prompt = prompts.firstOrNull { it.id == id } ?: return
        val original = prompt.originalText?.trim().orEmpty()
        if (original.isBlank()) {
            _uiState.update { it.copy(error = "Kein Original für diesen Prompt gespeichert") }
            return
        }
        val updated = prompts.map { if (it.id == id) it.copy(text = original) else it }
        SettingsStore.setUserContextPrompts(updated)
        _uiState.update { it.copy(userContextPrompts = updated) }
        syncUserContextPromptsToServer(updated, debounceMs = 0L)
    }

    fun deleteUserContextPrompt(id: String) {
        val updated = _uiState.value.userContextPrompts.filterNot { it.id == id }
        SettingsStore.setUserContextPrompts(updated)
        _uiState.update { it.copy(userContextPrompts = updated) }
        syncUserContextPromptsToServer(updated, debounceMs = 0L)
    }

    fun saveEditedStoreConfirmation(source: ChatMessage, editedText: String) {
        val text = editedText.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(error = "Kein Text zum Speichern") }
            return
        }

        val state = _uiState.value
        val sessionId = state.sessionId
        val category = source.category ?: state.selectedCategory
        val title = source.title ?: state.titleOverride.ifBlank { null }
        val categories = source.categories.ifEmpty { listOfNotNull(category) }
        val userMsg = ChatMessage(
            text = text,
            isUser = true,
            action = "store_edit",
            category = category,
            title = title,
            categories = categories,
            docId = source.docId,
            memoryText = text
        )
        _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true, error = null) }

        viewModelScope.launch {
            updateSessionsAfterPersist(sessionId, userMsg)
            try {
                if (WireGuardManager.state.value != TunnelState.CONNECTED) {
                    val item = de.frank.cortex.data.OutboxItem(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        text = text,
                        category = category,
                        title = title,
                        contextMode = SettingsStore.CONTEXT_MODE_SAVE,
                            contextModeRevision = state.contextModeRevision,
                            responseSize = state.responseSize,
                            requestId = UUID.randomUUID().toString(),
                            memoryEdit = true,
                            memoryDocId = source.docId,
                            memoryCategories = categories,
                            createdAt = System.currentTimeMillis()
                    )
                    val queued = withContext(Dispatchers.IO) {
                        try {
                            ChatSessionStore.enqueueOutbox(item)
                            true
                        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
                            CortexLog.error("ChatVM", "saveEditedStore", "Einreihen fehlgeschlagen: ${e.message}")
                            false
                        }
                    }
                    if (queued) {
                        val notice = ChatMessage(
                            text = "📮 Kein VPN — dein editierter Speicher-Auftrag ist gemerkt und wird automatisch gesendet, sobald der Tunnel steht.",
                            isUser = false,
                            action = "outbox"
                        )
                        _uiState.update { it.copy(isLoading = false, messages = it.messages + notice) }
                        updateSessionsAfterPersist(sessionId, notice)
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "VPN nicht aktiv — merken fehlgeschlagen") }
                    }
                    return@launch
                }

                val response = withContext(Dispatchers.IO) {
                    ApiClient.agentChatFallbackApi().chat(
                        ChatRequest(
                            text = text,
                            session_id = sessionId,
                            category = category,
                            title = title,
                            context_mode = SettingsStore.CONTEXT_MODE_SAVE,
                            context_mode_revision = state.contextModeRevision,
                            context_prompt = buildContextPrompt(SettingsStore.CONTEXT_MODE_SAVE, state.responseSize),
                            response_size = state.responseSize,
                            request_id = UUID.randomUUID().toString(),
                            memory_edit = true,
                            memory_doc_id = source.docId,
                            memory_categories = categories
                        )
                    )
                }
                val agentMsg = response.toChatMessage()
                _uiState.update { st ->
                    if (st.sessionId != sessionId) st.copy(isLoading = false)
                    else st.copy(
                        messages = st.messages + agentMsg,
                        isLoading = false,
                        titleOverride = if (response.action == "store") "" else st.titleOverride
                    )
                }
                updateSessionsAfterPersist(sessionId, agentMsg)
                CortexLog.info("ChatVM", "saveEditedStore", "Editierter Speicher-Auftrag gesendet",
                    mapOf("stored" to response.stored, "action" to response.action, "chars" to text.length))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "saveEditedStore", "Fehler: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Speichern fehlgeschlagen: ${e.message}") }
            }
        }
    }

    fun startNewChat() {
        stopSpeaking()
        val oldState = _uiState.value
        val oldSessionId = oldState.sessionId
        val shouldEndServerSession = oldState.messages.isNotEmpty()
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
                selectedCategory = null,
                sessionId = "android-${UUID.randomUUID()}",
                isSessionsPanelOpen = false,
                isRecording = false,
                isTranscribing = false,
                isImproving = false,
                isGeneratingTitle = false
            )
        }
        CortexLog.info("ChatVM", "startNewChat", "Neuer Chat lokal sofort gestartet",
            mapOf("old_session_id" to oldSessionId, "server_end" to shouldEndServerSession))
        refreshSessions()
        if (!shouldEndServerSession) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (WireGuardManager.state.value != TunnelState.CONNECTED) {
                    enqueuePendingSessionEnd(oldSessionId, "VPN nicht verbunden")
                    return@launch
                }
                val response = ApiClient.agentApi().endSession(EndSessionRequest(session_id = oldSessionId))
                if (!response.ok) throw Exception(response.message ?: "Server meldet ok=false")
                CortexLog.info("ChatVM", "startNewChat", "Alte Server-Session nach Plus-Klick beendet",
                    mapOf("session_id" to oldSessionId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                enqueuePendingSessionEnd(oldSessionId, "Sofortiges Beenden fehlgeschlagen: ${e.message}")
                _uiState.update { it.copy(error = "Altes Gespräch wird beim nächsten VPN-Start gesichert: ${e.message}") }
            }
        }
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
                        selectedCategory = null,
                        isRecording = false,
                        isTranscribing = false,
                        isImproving = false,
                        isGeneratingTitle = false
                    )
                }
                CortexLog.info("ChatVM", "selectSession", "Session geöffnet", mapOf("session_id" to sessionId, "messages" to messages.size))
            } catch (e: CancellationException) {
                throw e
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
                            selectedCategory = null,
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "deleteSession", "Session konnte nicht gelöscht werden: ${e.message}")
                _uiState.update { it.copy(error = "Session konnte nicht gelöscht werden: ${e.message}") }
            }
        }
    }

    /** Erzeugt im Hintergrund den Intentions-Titel der Session (1-3 Zeilen) und speichert ihn.
     *  Fehler sind unkritisch — dann bleibt der bisherige Anfangstext-Titel (funktionserhaltend). */
    private fun generateSessionIntentTitle(sessionId: String, firstMessage: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val title = ApiClient.geminiSessionTitle(firstMessage)
                if (title.isNotBlank()) {
                    ChatSessionStore.updateSessionTitle(sessionId, title)
                    refreshSessions()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.warn("ChatVM", "sessionTitle", "Intentions-Titel fehlgeschlagen (Roh-Titel bleibt): ${e.message}")
            }
        }
    }

    private fun refreshSessions() {
        viewModelScope.launch {
            // null = Laden fehlgeschlagen -> die bisher angezeigte Liste NICHT wegwerfen
            // (vorher wurde sie faelschlich durch emptyList() geleert).
            val sessions = withContext(Dispatchers.IO) {
                try {
                    ChatSessionStore.listSessions()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    CortexLog.error("ChatVM", "refreshSessions", "Session-Liste konnte nicht geladen werden: ${e.message}")
                    null
                }
            }
            if (sessions != null) _uiState.update { it.copy(sessions = sessions) }
        }
    }

    private suspend fun updateSessionsAfterPersist(sessionId: String, message: ChatMessage) {
        // null = Speichern/Laden fehlgeschlagen -> bisherige Sessions-Liste behalten,
        // statt sie (wie frueher) durch eine leere Liste zu ersetzen.
        val sessions = withContext(Dispatchers.IO) {
            try {
                ChatSessionStore.saveMessage(sessionId, message.toStoredMessage())
                ChatSessionStore.listSessions()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "persistSession", "Nachricht konnte nicht gespeichert werden: ${e.message}")
                null
            }
        }
        if (sessions != null) _uiState.update { it.copy(sessions = sessions) }
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
        titleAttempt = if (lastTitleSource == source) titleAttempt + 1 else 1
        lastTitleSource = source
        val attempt = titleAttempt
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingTitle = true, error = null) }
            try {
                val title = withContext(Dispatchers.IO) { ApiClient.geminiTitle(source, attempt) }
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
        val normalized = when (mode) {
            SettingsStore.CONTEXT_MODE_AUTO,
            SettingsStore.CONTEXT_MODE_SMALLTALK,
            SettingsStore.CONTEXT_MODE_SAVE,
            SettingsStore.CONTEXT_MODE_RULE,
            SettingsStore.CONTEXT_MODE_SEARCH -> mode
            else -> SettingsStore.CONTEXT_MODE_AUTO
        }
        _uiState.update {
            if (it.contextMode == normalized) it
            else it.copy(
                contextMode = normalized,
                contextModeRevision = SettingsStore.advanceContextModeRevision(normalized)
            )
        }
    }

    private fun resetOneShotContextMode(requestMode: String, requestRevision: Int) {
        if (requestMode !in setOf(
                SettingsStore.CONTEXT_MODE_SAVE,
                SettingsStore.CONTEXT_MODE_RULE,
                SettingsStore.CONTEXT_MODE_SEARCH
            )
        ) return

        val current = _uiState.value
        if (current.contextMode == requestMode && current.contextModeRevision == requestRevision) {
            updateContextMode(SettingsStore.CONTEXT_MODE_AUTO)
        }
    }

    fun updateResponseSize(size: String) {
        val normalized = when (size) {
            SettingsStore.RESPONSE_SIZE_AUTO,
            SettingsStore.RESPONSE_SIZE_SHORT,
            SettingsStore.RESPONSE_SIZE_MEDIUM,
            SettingsStore.RESPONSE_SIZE_XL -> size
            else -> SettingsStore.RESPONSE_SIZE_AUTO
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
        synchronized(speechLock) {
            speechGeneration.incrementAndGet()
            speakJob?.cancel()
            speakJob = null
        }
        pcmPlayer.stop()
        mp3Player.stop()
        _uiState.update { it.copy(isSpeaking = false, speakingMessageId = null) }
    }

    override fun onCleared() {
        super.onCleared()
        // Ausstehende Prompt-Persistenz noch sichern (Debounce-Job stirbt gleich mit dem Scope)
        // und den Server-Sync vormerken — sonst ueberschrieb der alte Server-Stand die letzte
        // lokale Aenderung beim naechsten Start still.
        if (promptPersistJob?.isActive == true) {
            promptPersistJob?.cancel()
            SettingsStore.setUserContextPrompts(_uiState.value.userContextPrompts)
            SettingsStore.pendingUserContextPromptSync = true
        }
        synchronized(speechLock) {
            speakJob?.cancel()
            speakJob = null
        }
        pcmPlayer.stop()
        mp3Player.stop()
        // Laufende Aufnahme freigeben: sonst blieb das Mikrofon (inkl. gruenem Privacy-
        // Indikator) nach dem App-Ende bis zum Prozess-Tod belegt.
        micRecorder.release()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun speakResponse(text: String, messageId: String? = null) {
        val spokenText = ChatSpeechSanitizer.clean(text)
        if (spokenText.isBlank()) return
        val generation: Int
        synchronized(speechLock) {
            generation = speechGeneration.incrementAndGet()
            speakJob?.cancel()
            speakJob = viewModelScope.launch {
            _uiState.update { it.copy(isSpeaking = true, speakingMessageId = messageId) }
            // BEIDE Player defensiv stoppen: die alte Generation raeumt wegen des Generation-
            // Guards im finally nichts auf — lief sie auf dem JEWEILS ANDEREN Motor (Provider-
            // Wechsel Chirp<->Edge), stoppte ihn sonst niemand und ein AudioTrack blieb
            // ungereleased im PLAYING-Zustand haengen.
            pcmPlayer.stop()
            mp3Player.stop()
            // Frischer Session-Start: alten Rate-Limit-Zaun/Flag zuruecksetzen, sonst wuerde eine
            // vorige (erschoepfte) Session diese sofort faelschlich bremsen/abbrechen.
            ttsRateLimitedUntil = 0L
            ttsRateLimitExhausted = false
            ttsAccessDenied = false
            qwenErrorMessage = null
            // Vorlese-Motor waehlen: Edge (Microsoft, MP3), die eigene geklonte Stimme (Alibaba,
            // WAV) oder Chirp 3: HD (Google, PCM). Nur Chirp liefert rohes PCM fuer den
            // durchgehenden AudioTrack — Edge und die eigene Stimme kommen als fertige Datei.
            val provider = SettingsStore.ttsProvider
            val isEdge = provider == SettingsStore.TTS_PROVIDER_EDGE
            val isQwen = provider == SettingsStore.TTS_PROVIDER_QWEN
            val isFileAudio = isEdge || isQwen
            try {
                val voice = when {
                    isQwen -> SettingsStore.qwenTtsVoiceId
                    isEdge -> SettingsStore.edgeTtsVoice
                    else -> SettingsStore.ttsVoice
                }
                val rate = SettingsStore.ttsRate
                val chunks = chunkText(spokenText)
                if (chunks.isEmpty()) return@launch
                val ttsStartedAt = System.currentTimeMillis()
                CortexLog.info("ChatVM", "speakResponse", "TTS Pipeline startet",
                    mapOf("len" to spokenText.length, "chunks" to chunks.size, "voice" to voice, "rate" to rate, "engine" to ttsEngineName(isEdge, isQwen)))

                coroutineScope {
                    val pending = mutableMapOf<Int, Deferred<ByteArray?>>()

                    // Chirp: durchgehenden AudioTrack PARALLEL zur ersten Netzanfrage aufbauen.
                    // Edge/eigene Stimme: kein Vorab-Player noetig (MediaPlayer baut pro Haeppchen auf).
                    val playerReady = if (isFileAudio) null else async(Dispatchers.IO) { pcmPlayer.start(rate) }

                    fun enqueue(index: Int) {
                        if (index in chunks.indices && pending[index] == null) {
                            pending[index] = async(Dispatchers.IO) {
                                val started = System.currentTimeMillis()
                                val audio = when {
                                    isQwen -> synthesizeQwenChunk(chunks[index], voice, index)
                                    isEdge -> synthesizeEdgeChunk(chunks[index], voice, index)
                                    else -> synthesizeTtsChunk(chunks[index], voice, index)
                                }
                                CortexLog.info("ChatVM", "ttsSynth", "TTS-Chunk synthetisiert",
                                    mapOf("index" to index, "chunk_len" to chunks[index].length,
                                        "audio_bytes" to (audio?.size ?: 0),
                                        "elapsed_ms" to (System.currentTimeMillis() - started)))
                                audio
                            }
                        }
                    }

                    repeat(minOf(TTS_PREFETCH_AHEAD + 1, chunks.size)) { enqueue(it) }
                    var firstAudioLogged = false

                    // Beim vorzeitigen Verlassen laufende Prefetch-Synthesen SOFORT abbrechen —
                    // coroutineScope wartet sonst auf sie (Rate-Limit-Retries bis zu 25 s), und
                    // der Lautsprecher bliebe solange faelschlich auf "spricht".
                    fun cancelPending() {
                        pending.values.forEach { it.cancel() }
                        pending.clear()
                    }

                    chunks.forEachIndexed { index, chunk ->
                        val audio = pending.remove(index)?.await()
                        enqueue(index + TTS_PREFETCH_AHEAD + 1)
                        if (speechGeneration.get() != generation) {
                            cancelPending()
                            return@coroutineScope
                        }

                        if (audio == null || audio.isEmpty()) {
                            val qwenReason = qwenErrorMessage
                            if (qwenReason != null) {
                                // Alibaba-Fehler gelten fuer die ganze Session (Schluessel/Stimme),
                                // nicht nur fuer diesen Absatz -> klar melden und abbrechen.
                                CortexLog.warn("ChatVM", "speakResponse", "Eigene Stimme gestoppt: $qwenReason",
                                    mapOf("index" to index))
                                _uiState.update { it.copy(error = "Vorlesen mit deiner Stimme nicht möglich: $qwenReason") }
                                cancelPending()
                                return@coroutineScope
                            }
                            if (ttsAccessDenied) {
                                // Cloud-TTS-API nicht freigeschaltet o.ae. -> klar melden statt still.
                                CortexLog.warn("ChatVM", "speakResponse", "TTS wegen Zugriffsfehler gestoppt",
                                    mapOf("index" to index))
                                _uiState.update { it.copy(error = "Vorlesen nicht möglich: Cloud Text-to-Speech API ist für deinen Schlüssel nicht freigeschaltet. Bitte im Google-Cloud-Projekt die \"Cloud Text-to-Speech API\" aktivieren.") }
                                cancelPending()
                                return@coroutineScope
                            }
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
                                cancelPending()
                                return@coroutineScope
                            }
                            // Echter Inhalts-/Format-Fehler bei genau diesem Chunk -> nur diesen
                            // ueberspringen, der Rest wird normal weiter vorgelesen.
                            CortexLog.warn("ChatVM", "speakResponse", "TTS-Chunk übersprungen",
                                mapOf("index" to index, "chunk_len" to chunk.length))
                            return@forEachIndexed
                        }

                        if (!firstAudioLogged) {
                            CortexLog.info("ChatVM", "speakResponse", "TTS erster Ton bereit",
                                mapOf("elapsed_ms" to (System.currentTimeMillis() - ttsStartedAt), "chunk_len" to chunk.length))
                            firstAudioLogged = true
                        }
                        val playStarted = System.currentTimeMillis()
                        if (isFileAudio) {
                            mp3Player.playAndAwait(audio, rate)
                        } else {
                            playerReady?.await() // no-op sobald der Track laengst steht
                            pcmPlayer.writeAndAwait(audio)
                        }
                        CortexLog.info("ChatVM", "ttsPlay", "TTS-Chunk abgespielt",
                            mapOf("index" to index, "audio_bytes" to audio.size,
                                "elapsed_ms" to (System.currentTimeMillis() - playStarted)))
                    }
                }
                CortexLog.info("ChatVM", "speakResponse", "TTS abgeschlossen",
                    mapOf("elapsed_ms" to (System.currentTimeMillis() - ttsStartedAt), "chunks" to chunks.size))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "speakResponse", "TTS fehlgeschlagen: ${e.message}")
            } finally {
                if (speechGeneration.get() == generation) {
                    pcmPlayer.stop()
                    mp3Player.stop()
                    _uiState.update { it.copy(isSpeaking = false, speakingMessageId = null) }
                }
            }
            }
        }
    }

    /**
     * Streaming-Vorlese-Pipeline (Frank-Vorgabe 2026-07-02): konsumiert FERTIGE Absaetze aus einem
     * Channel — das Vorlesen beginnt also fruehestens mit dem ersten vollstaendigen Absatz, nie mit
     * Einzelwoertern (sonst haelt das TTS-Modell den Text faelschlich fuer komplett und bricht ab).
     * Synthese laeuft dem Abspielen ueber einen kleinen Audio-Puffer voraus (Prefetch wie speakResponse).
     */
    private fun startStreamingSpeech(): Channel<String> {
        val textChannel = Channel<String>(Channel.UNLIMITED)
        val generation: Int
        synchronized(speechLock) {
            generation = speechGeneration.incrementAndGet()
            speakJob?.cancel()
            speakJob = viewModelScope.launch {
            _uiState.update { it.copy(isSpeaking = true, speakingMessageId = null) }
            // Defensiv beide Player stoppen (Provider-Wechsel-Leck, siehe speakResponse).
            pcmPlayer.stop()
            mp3Player.stop()
            ttsRateLimitedUntil = 0L
            ttsRateLimitExhausted = false
            ttsAccessDenied = false
            qwenErrorMessage = null
            val provider = SettingsStore.ttsProvider
            val isEdge = provider == SettingsStore.TTS_PROVIDER_EDGE
            val isQwen = provider == SettingsStore.TTS_PROVIDER_QWEN
            val isFileAudio = isEdge || isQwen
            try {
                val voice = when {
                    isQwen -> SettingsStore.qwenTtsVoiceId
                    isEdge -> SettingsStore.edgeTtsVoice
                    else -> SettingsStore.ttsVoice
                }
                val rate = SettingsStore.ttsRate
                CortexLog.info("ChatVM", "streamSpeech", "Streaming-TTS-Pipeline startet",
                    mapOf("voice" to voice, "rate" to rate, "engine" to ttsEngineName(isEdge, isQwen)))
                coroutineScope {
                    val playerReady = if (isFileAudio) null else async(Dispatchers.IO) { pcmPlayer.start(rate) }
                    val audioChannel = Channel<ByteArray?>(TTS_PREFETCH_AHEAD)
                    val synthJob = launch(Dispatchers.IO) {
                        var index = 0
                        try {
                            for (para in textChannel) {
                                for (chunk in chunkText(para)) {
                                    val synthStarted = System.currentTimeMillis()
                                    val audio = when {
                                        isQwen -> synthesizeQwenChunk(chunk, voice, index)
                                        isEdge -> synthesizeEdgeChunk(chunk, voice, index)
                                        else -> synthesizeTtsChunk(chunk, voice, index)
                                    }
                                    CortexLog.info("ChatVM", "streamTtsSynth", "Streaming-TTS-Chunk synthetisiert",
                                        mapOf("index" to index, "chunk_len" to chunk.length,
                                            "audio_bytes" to (audio?.size ?: 0),
                                            "elapsed_ms" to (System.currentTimeMillis() - synthStarted),
                                            "prefetch_limit" to TTS_PREFETCH_AHEAD))
                                    audioChannel.send(audio)
                                    index++
                                }
                            }
                        } finally {
                            audioChannel.close()
                        }
                    }
                    var firstAudioLogged = false
                    var playIndex = 0
                    for (audio in audioChannel) {
                        val currentIndex = playIndex++
                        if (speechGeneration.get() != generation) break
                        if (audio == null || audio.isEmpty()) {
                            // Als plain if statt ?.let: 'break' darf keine Lambda-Grenze ueberspringen.
                            val qwenReason = qwenErrorMessage
                            if (qwenReason != null) {
                                _uiState.update { it.copy(error = "Vorlesen mit deiner Stimme nicht möglich: $qwenReason") }
                                break
                            }
                            if (ttsAccessDenied) {
                                _uiState.update { it.copy(error = "Vorlesen nicht möglich: Cloud Text-to-Speech API ist für deinen Schlüssel nicht freigeschaltet.") }
                                break
                            }
                            if (ttsRateLimitExhausted) {
                                _uiState.update { it.copy(error = "Vorlese-Limit erreicht — die restlichen Absätze konnten nicht vorgelesen werden.") }
                                break
                            }
                            continue   // einzelnen kaputten Chunk ueberspringen, Rest weiter vorlesen
                        }
                        if (!firstAudioLogged) {
                            CortexLog.info("ChatVM", "streamSpeech", "Erster Ton (erster fertiger Absatz)")
                            firstAudioLogged = true
                        }
                        val playStarted = System.currentTimeMillis()
                        if (isFileAudio) mp3Player.playAndAwait(audio, rate)
                        else { playerReady?.await(); pcmPlayer.writeAndAwait(audio) }
                        CortexLog.info("ChatVM", "streamTtsPlay", "Streaming-TTS-Chunk abgespielt",
                            mapOf("index" to currentIndex, "audio_bytes" to audio.size,
                                "elapsed_ms" to (System.currentTimeMillis() - playStarted)))
                    }
                    synthJob.cancel()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("ChatVM", "streamSpeech", "Streaming-TTS fehlgeschlagen: ${e.message}")
            } finally {
                textChannel.close()
                if (speechGeneration.get() == generation) {
                    pcmPlayer.stop()
                    mp3Player.stop()
                    _uiState.update { it.copy(isSpeaking = false, speakingMessageId = null) }
                }
            }
            }
        }
        return textChannel
    }

    private fun localTtsClean(s: String): String = ChatSpeechSanitizer.clean(s)

    private fun buildContextPrompt(contextMode: String, responseSize: String): String? {
        val modePrompt = if (contextMode == SettingsStore.CONTEXT_MODE_AUTO) "" else SettingsStore.contextPrompt(contextMode)
        val sizePrompt = SettingsStore.responseSizePrompt(responseSize)
        val activeUserPrompts = SettingsStore.userContextPrompts()
            .filter { it.enabled && it.text.isNotBlank() }
            .mapIndexed { index, prompt -> "Zusatz-Prompt ${index + 1}:\n${prompt.text.trim()}" }
            .joinToString("\n\n")
        val combined = listOf(modePrompt, sizePrompt, activeUserPrompts)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
            .ifBlank { return null }
        // Server lehnt context_prompt > limits.agent.context_prompt_max_chars HART mit 422 ab
        // (bewusst keine serverseitige Kuerzung) — ohne lokalen Deckel machten zwei bis drei
        // laengere aktive Zusatz-Prompts JEDE Chat-Nachricht kaputt ("HTTP 422").
        // KEIN kuenstlicher Mindestwert: der Server akzeptiert auch kleinere konfigurierte
        // Limits — ein Client-Floor ueber dem Server-Limit erzeugte genau die 422er, die die
        // Kappung verhindern soll.
        val maxChars = (ApiClient.cachedAgentConfig()?.limits?.agent?.context_prompt_max_chars ?: 4000)
            .coerceAtLeast(0)
        if (combined.length > maxChars) {
            CortexLog.warn("ChatVM", "buildContextPrompt",
                "Kontext-Prompt auf $maxChars Zeichen gekappt (war ${combined.length})")
        }
        return combined.take(maxChars)
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
            } catch (e: GeminiTtsAccessException) {
                // Zugriff verweigert (API nicht freigeschaltet / Key ohne Rechte): Retry waere
                // zwecklos. Flag setzen, damit speakResponse eine klare Meldung zeigt + abbricht.
                CortexLog.error("ChatVM", "synthesizeTtsChunk", "Cloud-TTS-Zugriff verweigert: ${e.message}",
                    mapOf("index" to index, "chunk_len" to chunk.length))
                ttsAccessDenied = true
                return null
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

    /**
     * Synthetisiert einen Chunk mit Edge TTS (Microsoft) und gibt die MP3-Bytes zurueck.
     * Edge ist kostenlos und kennt kein 429/Key-Problem - nur einfache Wiederholung bei
     * WebSocket-Fehlern; bleibt es dabei, wird der Chunk uebersprungen (null).
     */
    private suspend fun synthesizeEdgeChunk(chunk: String, voice: String, index: Int): ByteArray? {
        repeat(TTS_RETRY_ATTEMPTS) { attempt ->
            try {
                val mp3 = edgeSynth.synthesize(chunk, voice)
                if (mp3.isNotEmpty()) return mp3
                CortexLog.warn("ChatVM", "synthesizeEdgeChunk", "Edge lieferte leeres Audio",
                    mapOf("index" to index, "attempt" to (attempt + 1), "chunk_len" to chunk.length))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.warn("ChatVM", "synthesizeEdgeChunk", "Edge-Chunk fehlgeschlagen: ${e.message}",
                    mapOf("index" to index, "attempt" to (attempt + 1), "chunk_len" to chunk.length))
            }
            if (attempt < TTS_RETRY_ATTEMPTS - 1) delay(300L * (attempt + 1))
        }
        return null
    }

    /**
     * Synthetisiert einen Absatz mit der eigenen, bei Alibaba geklonten Stimme.
     *
     * KEIN Wiederholungsversuch bei Konfigurationsfehlern: ein fehlender Schluessel oder eine
     * abgewiesene Stimm-Kennung wird durch Wiederholen nicht besser — der Grund wird gemerkt,
     * damit die Pipeline die Session sauber beendet und ihn anzeigt.
     */
    private suspend fun synthesizeQwenChunk(chunk: String, voice: String, index: Int): ByteArray? {
        try {
            val wav = qwenSynth.synthesize(chunk, voice, SettingsStore.qwenTtsApiKey)
            if (wav.isNotEmpty()) return wav
            qwenErrorMessage = "Alibaba hat keinen Ton geliefert."
        } catch (e: CancellationException) {
            throw e
        } catch (e: de.frank.cortex.tts.QwenTtsException) {
            CortexLog.warn("ChatVM", "synthesizeQwenChunk", "Eigene Stimme fehlgeschlagen: ${e.message}",
                mapOf("index" to index, "chunk_len" to chunk.length))
            qwenErrorMessage = e.message ?: "Unbekannter Fehler."
        } catch (e: Exception) {
            CortexLog.warn("ChatVM", "synthesizeQwenChunk", "Eigene Stimme fehlgeschlagen: ${e.message}",
                mapOf("index" to index, "chunk_len" to chunk.length))
            qwenErrorMessage = "Die Verbindung zu Alibaba ist fehlgeschlagen."
        }
        return null
    }

    private fun ttsEngineName(isEdge: Boolean, isQwen: Boolean): String = when {
        isQwen -> "qwen"
        isEdge -> "edge"
        else -> "chirp"
    }

    private val whitespaceRunRegex = Regex("[ \t]+")
    private val paragraphSplitRegex = Regex("\n{2,}")
    private val sentenceSplitRegex = Regex("(?<=[.!?])\\s+")

    private fun chunkText(text: String): List<String> {
        val normalized = text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(whitespaceRunRegex, " ")
            .trim()
        if (normalized.isBlank()) return emptyList()

        // Frank-Regel 2026-07-02: IMMER komplette Absaetze als Vorlese-Einheiten — jeder Absatz
        // (durch Leerzeile getrennt) wird einzeln synthetisiert und vorgelesen. Das fruehere
        // Zeichen-Aufbau-System (erster Chunk max 350, Folge-Chunks ~650, Deckel 1000, Absaetze
        // wurden zusammengelegt oder mitten drin geteilt) ist bewusst ENTFERNT. TTS_MAX_CHARS
        // bleibt nur als API-Sicherheitsgrenze: einzig UEBERLANGE Absaetze werden an
        // Satzgrenzen geteilt (sonst lehnt die TTS-API die Anfrage ab).
        return normalized
            .split(paragraphSplitRegex)
            .map { it.replace("\n", " ").trim() }
            .filter { it.isNotBlank() }
            .flatMap { splitParagraphForTts(it) }
    }

    private fun splitParagraphForTts(paragraph: String): List<String> {
        if (paragraph.length <= TTS_MAX_CHARS) return listOf(paragraph)
        val sentences = paragraph.split(sentenceSplitRegex).filter { it.isNotBlank() }
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

private fun ChatResponse.toChatMessage(
    id: String = UUID.randomUUID().toString(),
    responseTimeMs: Long? = null
): ChatMessage = ChatMessage(
    id = id,
    text = ChatSpeechSanitizer.clean(reply),
    isUser = false,
    action = action,
    category = category,
    title = title,
    categories = categories ?: listOfNotNull(category),
    docId = doc_id,
    memoryText = memory_text,
    storedText = stored_text,
    memoryEditable = memory_editable,
    recallHits = recall_hits,
    options = options,
    stored = stored,
    responseTimeMs = responseTimeMs,
    sourceUsage = source_usage
)

private fun ChatMessage.toStoredMessage(): StoredChatMessage = StoredChatMessage(
    id = id,
    text = if (isUser) text else ChatSpeechSanitizer.clean(text),
    isUser = isUser,
    action = action,
    category = category,
    title = title,
    categories = categories,
    docId = docId,
    memoryText = memoryText,
    storedText = storedText,
    memoryEditable = memoryEditable,
    recallHits = recallHits,
    options = options.orEmpty(),
    stored = stored,
    timestamp = timestamp,
    responseTimeMs = responseTimeMs,
    sourceUsage = sourceUsage
)

private fun StoredChatMessage.toChatMessage(): ChatMessage = ChatMessage(
    id = id,
    text = if (isUser) text else ChatSpeechSanitizer.clean(text),
    isUser = isUser,
    action = action,
    category = category,
    title = title,
    categories = categories,
    docId = docId,
    memoryText = memoryText,
    storedText = storedText,
    memoryEditable = memoryEditable,
    recallHits = recallHits,
    options = options.takeIf { it.isNotEmpty() },
    stored = stored,
    timestamp = timestamp,
    responseTimeMs = responseTimeMs,
    sourceUsage = sourceUsage
)

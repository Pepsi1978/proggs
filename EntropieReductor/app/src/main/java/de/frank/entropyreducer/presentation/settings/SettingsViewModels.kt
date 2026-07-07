package de.frank.entropyreducer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.data.remote.drive.GoogleSignInHelper
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.remote.drive.SyncStatus
import de.frank.entropyreducer.data.remote.brain.SecondBrainIdeaConnector
import de.frank.entropyreducer.data.remote.tts.GoogleTtsVoice
import de.frank.entropyreducer.data.remote.tts.GoogleTtsVoices
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.MemoryRepository
import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.domain.model.MemorySource
import de.frank.entropyreducer.domain.tts.TtsPlayer
import de.frank.entropyreducer.domain.tts.TtsResult
import de.frank.entropyreducer.domain.usecase.SyncEntriesUseCase
import de.frank.entropyreducer.domain.usecase.TestApiKeyUseCase
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/* ----------------- API Keys ----------------- */
data class ApiKeysUiState(
    val groqKey: String = "",
    val geminiKey: String = "",
    val ttsKey: String = "",
    val groqSaved: Boolean = false,
    val geminiSaved: Boolean = false,
    val ttsSaved: Boolean = false,
    val groqStatus: ConnectionStatus = ConnectionStatus.UNKNOWN,
    val geminiStatus: ConnectionStatus = ConnectionStatus.UNKNOWN,
    val ttsStatus: ConnectionStatus = ConnectionStatus.UNKNOWN,
    val ttsVoiceName: String = GoogleTtsVoices.DEFAULT_VOICE_NAME,
    val ttsVoices: List<GoogleTtsVoice> = GoogleTtsVoices.ALL,
    val ttsSpeakState: TtsSpeakState = TtsSpeakState.IDLE,
    val ttsLastError: String? = null,
    val secondBrainKey: String = "",
    val secondBrainWireGuardConfig: String = "",
    val secondBrainSaved: Boolean = false,
    val secondBrainWireGuardSaved: Boolean = false,
    val secondBrainEnabled: Boolean = false,
    val secondBrainStatus: ConnectionStatus = ConnectionStatus.UNKNOWN,
    val secondBrainMessage: String = "Noch nicht synchronisiert.",
    val secondBrainSyncing: Boolean = false,
)

enum class ConnectionStatus {
    UNKNOWN,
    OK,
    FAIL,
    TESTING,
}

enum class TtsSpeakState {
    IDLE,
    LOADING,
    SPEAKING,
    ERROR,
}

@HiltViewModel
class ApiKeysViewModel
@Inject
constructor(
    private val secrets: EncryptedSecretsStore,
    private val testApi: TestApiKeyUseCase,
    private val settings: AppSettings,
    private val ttsPlayer: TtsPlayer,
    private val secondBrainIdeaConnector: SecondBrainIdeaConnector,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            ApiKeysUiState(
                groqKey = secrets.groqApiKey.orEmpty(),
                geminiKey = secrets.geminiApiKey.orEmpty(),
                ttsKey = secrets.googleTtsApiKey.orEmpty(),
                secondBrainKey = secrets.secondBrainApiKey.orEmpty(),
                secondBrainWireGuardConfig = secrets.secondBrainWireGuardConfig.orEmpty(),
                groqSaved = !secrets.groqApiKey.isNullOrBlank(),
                geminiSaved = !secrets.geminiApiKey.isNullOrBlank(),
                ttsSaved = !secrets.googleTtsApiKey.isNullOrBlank(),
                secondBrainSaved = !secrets.secondBrainApiKey.isNullOrBlank(),
                secondBrainWireGuardSaved = !secrets.secondBrainWireGuardConfig.isNullOrBlank(),
            )
        )
    val state: StateFlow<ApiKeysUiState> = _state.asStateFlow()

    init {
        // Voice-Auswahl aus AppSettings laden — leerer Wert -> Default-Stimme.
        viewModelScope.launch {
            settings.ttsVoiceFlow.collect { stored ->
                val effective = stored.ifBlank { GoogleTtsVoices.DEFAULT_VOICE_NAME }
                _state.update { it.copy(ttsVoiceName = effective) }
            }
        }
        viewModelScope.launch {
            settings.secondBrainIdeasConnectorEnabledFlow.collect { enabled ->
                _state.update { it.copy(secondBrainEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            secondBrainIdeaConnector.state.collect { sync ->
                _state.update {
                    it.copy(
                        secondBrainMessage = sync.lastMessage,
                        secondBrainSyncing = sync.syncing,
                    )
                }
            }
        }
    }

    fun setGroq(value: String) {
        _state.update { it.copy(groqKey = value) }
    }

    fun setGemini(value: String) {
        _state.update { it.copy(geminiKey = value) }
    }

    fun setTts(value: String) {
        _state.update { it.copy(ttsKey = value) }
    }

    fun setSecondBrain(value: String) {
        _state.update { it.copy(secondBrainKey = value) }
    }

    fun setSecondBrainWireGuardConfig(value: String) {
        _state.update { it.copy(secondBrainWireGuardConfig = value) }
    }

    fun saveGroq() {
        secrets.groqApiKey = state.value.groqKey.trim().ifBlank { null }
        _state.update { it.copy(groqSaved = !state.value.groqKey.isBlank()) }
    }

    fun saveGemini() {
        secrets.geminiApiKey = state.value.geminiKey.trim().ifBlank { null }
        _state.update { it.copy(geminiSaved = !state.value.geminiKey.isBlank()) }
    }

    fun saveTts() {
        secrets.googleTtsApiKey = state.value.ttsKey.trim().ifBlank { null }
        _state.update { it.copy(ttsSaved = !state.value.ttsKey.isBlank()) }
    }

    fun saveSecondBrain() {
        secrets.secondBrainApiKey = state.value.secondBrainKey.trim().ifBlank { null }
        _state.update { it.copy(secondBrainSaved = state.value.secondBrainKey.isNotBlank()) }
    }

    fun saveSecondBrainWireGuardConfig() {
        secrets.secondBrainWireGuardConfig = state.value.secondBrainWireGuardConfig.trim().ifBlank { null }
        _state.update { it.copy(secondBrainWireGuardSaved = state.value.secondBrainWireGuardConfig.isNotBlank()) }
    }

    fun testGemini() {
        viewModelScope.launch {
            _state.update { it.copy(geminiStatus = ConnectionStatus.TESTING) }
            val result = testApi.testGemini(state.value.geminiKey.trim())
            _state.update {
                it.copy(
                    geminiStatus =
                        if (result.isSuccess) ConnectionStatus.OK else ConnectionStatus.FAIL
                )
            }
        }
    }

    /** Echter Groq-Test via GET /openai/v1/models. */
    fun testGroq() {
        viewModelScope.launch {
            _state.update { it.copy(groqStatus = ConnectionStatus.TESTING) }
            val result = testApi.testGroq(state.value.groqKey.trim())
            _state.update {
                it.copy(
                    groqStatus =
                        if (result.isSuccess) ConnectionStatus.OK else ConnectionStatus.FAIL
                )
            }
        }
    }

    /** Echter TTS-Test via GET /v1/voices?key={key}. */
    fun testTts() {
        viewModelScope.launch {
            _state.update { it.copy(ttsStatus = ConnectionStatus.TESTING) }
            val result = testApi.testTts(state.value.ttsKey.trim())
            _state.update {
                it.copy(
                    ttsStatus = if (result.isSuccess) ConnectionStatus.OK else ConnectionStatus.FAIL
                )
            }
        }
    }

    fun testSecondBrain() {
        viewModelScope.launch {
            _state.update { it.copy(secondBrainStatus = ConnectionStatus.TESTING) }
            val ok = secondBrainIdeaConnector.testConnection(state.value.secondBrainKey)
            _state.update { it.copy(secondBrainStatus = if (ok) ConnectionStatus.OK else ConnectionStatus.FAIL) }
        }
    }

    fun setSecondBrainEnabled(value: Boolean) {
        viewModelScope.launch { settings.setSecondBrainIdeasConnectorEnabled(value) }
    }

    fun syncSecondBrainIdeasNow() {
        secondBrainIdeaConnector.syncAllNow(viewModelScope)
    }

    /** Speichert die ausgewaehlte Chirp-3-HD-Stimme und aktualisiert das UI sofort. */
    fun setTtsVoice(voiceName: String) {
        _state.update { it.copy(ttsVoiceName = voiceName) }
        viewModelScope.launch { settings.setTtsVoice(voiceName) }
    }

    /**
     * Stoppt die Wiedergabe sofort. UI kehrt in IDLE zurück. Für X-Button im Voice-Picker, falls
     * eine Probe noch laeuft.
     */
    fun stopTtsPreview() {
        ttsPlayer.stop()
        _state.update { it.copy(ttsSpeakState = TtsSpeakState.IDLE) }
    }

    /**
     * Sprich-Test: synthesiziert einen kurzen Beispiel-Text mit der aktuell ausgewaehlten Stimme.
     * UI-Spinner -> IDLE -> SPEAKING -> IDLE / ERROR.
     */
    fun testSpeak() {
        if (
            _state.value.ttsSpeakState == TtsSpeakState.LOADING ||
                _state.value.ttsSpeakState == TtsSpeakState.SPEAKING
        )
            return
        _state.update { it.copy(ttsSpeakState = TtsSpeakState.LOADING, ttsLastError = null) }
        viewModelScope.launch {
            val result =
                ttsPlayer.speak(
                    text = SAMPLE_PREVIEW,
                    voiceNameOverride = state.value.ttsVoiceName,
                    onPlaybackStart = {
                        _state.update { it.copy(ttsSpeakState = TtsSpeakState.SPEAKING) }
                    },
                    onComplete = { _state.update { it.copy(ttsSpeakState = TtsSpeakState.IDLE) } },
                    onError = { e ->
                        _state.update {
                            it.copy(ttsSpeakState = TtsSpeakState.ERROR, ttsLastError = e.message)
                        }
                    },
                )
            if (result is TtsResult.Error) {
                _state.update {
                    it.copy(ttsSpeakState = TtsSpeakState.ERROR, ttsLastError = result.message)
                }
            }
        }
    }

    /**
     * Lifecycle-Cleanup: Wenn der Benutzer die API-Schluessel-Seite verlaesst waehrend eine
     * Sprich-Probe noch laeuft, würde der MediaPlayer im Hintergrund weiter abspielen.
     * ViewModel.onCleared stoppt das.
     */
    override fun onCleared() {
        ttsPlayer.stop()
        super.onCleared()
    }

    private companion object {
        const val SAMPLE_PREVIEW =
            "Hallo Frank. So klingt diese Stimme. " +
                "Sie liest dir später Tagesbriefings, Wochenrückblicke und " +
                "Genie-Antworten vor."
    }
}

/* ----------------- Models ----------------- */
data class ModelsUiState(
    val whisperModel: String = AppSettings.DEFAULT_WHISPER,
    val geminiModel: String = AppSettings.DEFAULT_GEMINI,
    val transcriptionLanguage: String = "de",
    val ttsVoice: String = "",
)

@HiltViewModel
class ModelsViewModel @Inject constructor(private val settings: AppSettings) : ViewModel() {
    val state: StateFlow<ModelsUiState> =
        kotlinx.coroutines.flow
            .combine(
                settings.whisperModelFlow,
                settings.geminiModelFlow,
                settings.transcriptionLanguageFlow,
                settings.ttsVoiceFlow,
            ) { w, g, l, v ->
                ModelsUiState(w, g, l, v)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModelsUiState())

    fun setWhisper(v: String) = viewModelScope.launch { settings.setWhisperModel(v) }

    fun setGemini(v: String) = viewModelScope.launch { settings.setGeminiModel(v) }

    fun setLanguage(v: String) = viewModelScope.launch { settings.setTranscriptionLanguage(v) }

    fun setTtsVoice(v: String) = viewModelScope.launch { settings.setTtsVoice(v) }
}

/** State der KI-gestützten Profil-Destillation. */
enum class DistillState {
    IDLE,
    RUNNING,
    DONE,
}

/* ----------------- Profile ----------------- */
@HiltViewModel
class ProfileViewModel
@Inject
constructor(
    private val settings: AppSettings,
    private val memoryRepo: MemoryRepository,
    private val gemini: de.frank.entropyreducer.data.remote.GeminiApi,
    private val secrets: EncryptedSecretsStore,
    // Frank-Bugfix 2026-05-22: Profil-Edit triggert sofort Drive-Sync —
    // sonst geht der frisch editierte Profil-Text bei Reinstall verloren.
    private val syncCoordinator:
        dagger.Lazy<de.frank.entropyreducer.data.remote.drive.SyncCoordinator>,
) : ViewModel() {
    val profileText: StateFlow<String> =
        settings.profileTextFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _distillState = MutableStateFlow(DistillState.IDLE)
    val distillState: StateFlow<DistillState> = _distillState.asStateFlow()

    private val _distillError = MutableStateFlow<String?>(null)
    val distillError: StateFlow<String?> = _distillError.asStateFlow()

    fun dismissDistillError() {
        _distillError.value = null
    }

    fun save(text: String) = viewModelScope.launch {
        settings.setProfileText(text)
        syncCoordinator.get().requestSync("Einstellungen: Profiltext geaendert")
    }

    /**
     * Extrahiert alle Entropie-relevanten Informationen aus dem Profiltext via Gemini (Frank-Wunsch
     * 2026-05-20). Speichert sie als einzelne Memory-Einträge (source = AUS_PROFIL).
     *
     * Vorher: simple Absatz-Heuristik mit max 8 Einträgen. Jetzt: Gemini bekommt einen System-
     * Prompt der explizit nach Entropie-Themen sucht (Stress, Schlaf, Konflikte, Suchtmuster,
     * Routinen, etc.) und so viele Einträge zurückgibt, wie es relevante Punkte findet.
     */
    fun distillToMemory(text: String) = viewModelScope.launch {
        runDistill(text, replaceExisting = false)
    }

    /**
     * "Aus Profil neu generieren" (MemoryScreen). Loescht erst alle bestehenden AUS_PROFIL-Memories
     * und ruft dann die Destillation erneut. Manuelle und KI-Vorschlag- Einträge bleiben
     * unangetastet.
     */
    fun regenerateFromProfile() = viewModelScope.launch {
        val text = settings.profileTextFlow.first()
        if (text.isBlank()) {
            _distillError.value = "Profil ist leer — nichts zu übernehmen."
            return@launch
        }
        runDistill(text, replaceExisting = true)
    }

    private suspend fun runDistill(text: String, replaceExisting: Boolean) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            _distillError.value = "Profil ist leer — nichts zu übernehmen."
            return
        }
        val apiKey = secrets.geminiApiKey
        if (apiKey.isNullOrBlank()) {
            _distillError.value = "Kein Gemini-API-Schlüssel hinterlegt"
            return
        }

        _distillState.value = DistillState.RUNNING
        _distillError.value = null

        runCatching {
                val model = settings.geminiModelFlow.first()
                val response =
                    gemini.generateContent(
                        model = model,
                        apiKey = apiKey,
                        request =
                            de.frank.entropyreducer.data.remote.GeminiRequest(
                                systemInstruction =
                                    de.frank.entropyreducer.data.remote.GeminiContent(
                                        parts =
                                            listOf(
                                                de.frank.entropyreducer.data.remote.GeminiPart(
                                                    PROFILE_DISTILL_PROMPT
                                                )
                                            )
                                    ),
                                contents =
                                    listOf(
                                        de.frank.entropyreducer.data.remote.GeminiContent(
                                            role = "user",
                                            parts =
                                                listOf(
                                                    de.frank.entropyreducer.data.remote.GeminiPart(
                                                        trimmed
                                                    )
                                                ),
                                        )
                                    ),
                                generationConfig =
                                    de.frank.entropyreducer.data.remote.GeminiGenerationConfig(
                                        temperature = 0.3,
                                        responseMimeType = "application/json",
                                        maxOutputTokens = 4096,
                                    ),
                            ),
                    )
                response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.let(::parseEntries)
                    .orEmpty()
            }
            .onSuccess { entries ->
                if (entries.isEmpty()) {
                    _distillState.value = DistillState.IDLE
                    _distillError.value = "Gemini hat keine relevanten Einträge gefunden"
                    return@onSuccess
                }
                if (replaceExisting) {
                    memoryRepo.deleteBySource(MemorySource.AUS_PROFIL)
                }
                val now = System.currentTimeMillis()
                entries.forEachIndexed { idx, content ->
                    memoryRepo.upsert(
                        MemoryEntryEntity(
                            id = UUID.randomUUID().toString(),
                            content = content,
                            source = MemorySource.AUS_PROFIL,
                            isActive = true,
                            confidence = 80,
                            createdAt = now + idx,
                            updatedAt = now + idx,
                        )
                    )
                }
                _distillState.value = DistillState.DONE
            }
            .onFailure { ex ->
                _distillState.value = DistillState.IDLE
                _distillError.value = ex.message ?: "Profil-Analyse fehlgeschlagen"
            }
    }

    private fun parseEntries(raw: String): List<String> {
        return runCatching {
                val cleaned =
                    raw.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                val array =
                    runCatching { org.json.JSONArray(cleaned) }.getOrNull()
                        ?: runCatching {
                                val obj = org.json.JSONObject(cleaned)
                                obj.optJSONArray("entries")
                                    ?: obj.optJSONArray("memories")
                                    ?: obj.optJSONArray("items")
                            }
                            .getOrNull()
                        ?: return@runCatching emptyList()
                buildList(array.length()) {
                    for (i in 0 until array.length()) {
                        val entry =
                            when (val item = array.opt(i)) {
                                is String -> item.trim()
                                is org.json.JSONObject ->
                                    item
                                        .optString("content")
                                        .ifBlank { item.optString("text") }
                                        .ifBlank { item.optString("memory") }
                                        .trim()
                                else -> ""
                            }
                        if (entry.isNotBlank() && entry.length in 8..500) add(entry)
                    }
                }
            }
            .getOrDefault(emptyList())
    }

    private companion object {
        const val PROFILE_DISTILL_PROMPT =
            """
Du analysierst einen persönlichen Profiltext und extrahierst ALLE Informationen, die für die
Reduktion persönlicher Entropie relevant sind.

Persönliche Entropie umfasst (nicht abschließend):
- Stress-Auslöser und Stress-Muster
- Schlafqualität und Schlafhygiene
- Beziehungs-Konflikte und Kommunikationsmuster
- Sucht-Verhalten und Gewohnheiten (Koffein, Alkohol, Nikotin, Bildschirmzeit, Zucker)
- Körperliche Beschwerden, Energie-Level, Sport-/Bewegungsroutinen
- Mentale Muster: Grübeln, Aufschieben, Perfektionismus, Selbstkritik
- Emotionale Trigger und Reaktionsmuster
- Tages-/Wochenroutinen, Rituale, Strukturen
- Belastende Lebensereignisse und ungelöste Themen
- Werte, Ziele, Sinn-Themen — soweit sie Entropie-Reduktion betreffen
- Diagnosen, Medikamente, Therapieerfahrungen
- Familiäre/biografische Hintergründe die heute Entropie verursachen
- Arbeitsumfeld, finanzielle Belastungen

Pflicht-Regeln:
- Gib ALLE relevanten Informationen zurück — nicht "die 5 wichtigsten", sondern wirklich alles.
- Jede Information als EIGENER String — atomic, in sich verständlich.
- Sprache: Deutsch mit echten Umlauten (ä ö ü ß).
- Dritte Person ODER neutral formuliert — KEIN "Ich".
- Jeder Eintrag 8-400 Zeichen, ein vollständiger Satz oder kurzer Absatz.
- KEINE Doppelungen, KEINE Banalitäten, KEINE Schlüsse.
- Wenn der Text nichts Entropie-Relevantes enthält: leeres Array.

Output: EIN reines JSON-Array von Strings. NICHTS sonst — kein Markdown, keine Einleitung.

Beispiel-Output:
[
  "Schläft unregelmäßig zwischen 23 und 2 Uhr, wacht oft 1-2x pro Nacht auf.",
  "Trinkt 4-5 Tassen Kaffee pro Tag, oft auch nach 16 Uhr.",
  "Hat Konflikt mit Schwester wegen Erbsache seit 2 Jahren ungeklärt.",
  "Verfällt unter Stress in Perfektionismus und arbeitet bis 22 Uhr.",
  "Hat ADHS-Diagnose, nimmt aktuell kein Medikament.",
  "Joggt 2x pro Woche, hat aber Knieprobleme seit Herbst."
]
"""
    }
}

/* ----------------- Prompts ----------------- */
@HiltViewModel
class PromptsViewModel
@Inject
constructor(
    private val repo: PromptRepository,
    private val permissionRepo:
        de.frank.entropyreducer.data.repository.PromptToolPermissionRepository,
    private val toolRegistry: de.frank.entropyreducer.domain.agentic.ToolRegistry,
    private val triggerRepo:
        de.frank.entropyreducer.data.repository.PromptTriggerRepository,
) : ViewModel() {

    // Auto-Trigger (Etappe 11)
    fun triggersForPrompt(
        promptId: String
    ): kotlinx.coroutines.flow.Flow<
        List<de.frank.entropyreducer.data.local.entities.PromptTriggerEntity>
    > = triggerRepo.getForPrompt(promptId)

    fun addCronTrigger(promptId: String, cronExpression: String) =
        viewModelScope.launch {
            val nextAt =
                de.frank.entropyreducer.domain.agentic.trigger.SimpleCronParser.nextFireAt(
                    cronExpression
                )
            triggerRepo.upsert(
                de.frank.entropyreducer.data.local.entities.PromptTriggerEntity(
                    id = UUID.randomUUID().toString(),
                    promptId = promptId,
                    triggerType = de.frank.entropyreducer.domain.model.TriggerType.CRON,
                    cronExpression = cronExpression,
                    isActive = true,
                    nextScheduledAt = nextAt,
                )
            )
        }

    fun deleteTrigger(
        trigger: de.frank.entropyreducer.data.local.entities.PromptTriggerEntity
    ) = viewModelScope.launch { triggerRepo.delete(trigger) }

    fun setTriggerActive(
        trigger: de.frank.entropyreducer.data.local.entities.PromptTriggerEntity,
        active: Boolean,
    ) = viewModelScope.launch { triggerRepo.setActive(trigger.id, active) }

    /**
     * Chain-Trigger: dieser Prompt soll nach Erfolg eines anderen Prompts laufen.
     * (Etappe 12)
     */
    fun addChainTrigger(promptId: String, chainAfterPromptId: String) =
        viewModelScope.launch {
            triggerRepo.upsert(
                de.frank.entropyreducer.data.local.entities.PromptTriggerEntity(
                    id = UUID.randomUUID().toString(),
                    promptId = promptId,
                    triggerType = de.frank.entropyreducer.domain.model.TriggerType.CHAIN,
                    chainAfterPromptId = chainAfterPromptId,
                    isActive = true,
                )
            )
        }
    val prompts: StateFlow<List<SavedPromptEntity>> =
        repo.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Alle bekannten Write-Tools (fuer den Permission-Editor). Read-Tools brauchen keine Eintraege. */
    val writeTools: List<de.frank.entropyreducer.domain.agentic.AgenticTool>
        get() = toolRegistry.writeTools

    /**
     * Permissions des aktuell editierten Prompts als Flow — wird vom Dialog abonniert.
     * Mapped auf eine Map<toolName, Pair<granted, trustMode>> fuer einfacheren UI-Zugriff.
     */
    fun permissionsForPrompt(
        promptId: String
    ): kotlinx.coroutines.flow.Flow<
        Map<String, de.frank.entropyreducer.presentation.agentic.ToolPermissionState>
    > =
        permissionRepo
            .getForPrompt(promptId)
            .map { list ->
                list.associate { p ->
                    p.toolName to
                        de.frank.entropyreducer.presentation.agentic.ToolPermissionState(
                            p.granted,
                            p.trustMode,
                        )
                }
            }

    fun setToolPermission(
        promptId: String,
        toolName: String,
        granted: Boolean,
        trustMode: Boolean,
    ) = viewModelScope.launch {
        permissionRepo.upsert(
            de.frank.entropyreducer.data.local.entities.PromptToolPermissionEntity(
                id = "${promptId}_${toolName}",
                promptId = promptId,
                toolName = toolName,
                granted = granted,
                trustMode = trustMode,
            )
        )
    }

    fun save(entity: SavedPromptEntity) = viewModelScope.launch { repo.upsert(entity) }

    fun toggle(entity: SavedPromptEntity) = viewModelScope.launch {
        repo.upsert(
            entity.copy(isActive = !entity.isActive, updatedAt = System.currentTimeMillis())
        )
    }

    fun delete(entity: SavedPromptEntity) = viewModelScope.launch {
        // Direktive 3 Loop-2-Fix (war LOOP-2-2-Bug): Beim Prompt-Delete auch
        // orphaned Chain-Trigger raeumen die diesen Prompt als chainAfterPromptId
        // referenzieren. Permissions + eigene Trigger werden via FK CASCADE
        // automatisch geloescht, aber chainAfterPromptId ist KEIN FK (weak
        // reference) und braucht expliziten Cleanup.
        triggerRepo.deleteOrphanedChainTriggers(entity.id)
        repo.delete(entity)
    }

    fun create(
        name: String,
        content: String,
        category: de.frank.entropyreducer.domain.model.PromptCategory =
            de.frank.entropyreducer.domain.model.PromptCategory.AUFGABEN,
        model: String = "gemini-3.1-flash-lite",
        trustModeDefault: Boolean = false,
    ) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        repo.upsert(
            SavedPromptEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                content = content,
                isActive = true,
                createdAt = now,
                updatedAt = now,
                category = category,
                model = model,
                trustModeDefault = trustModeDefault,
            )
        )
    }

    // Agentic-AI (Frank-Wunsch 2026-05-21): schmale Updates fuer Modell + Trust
    fun updateModel(promptId: String, model: String) =
        viewModelScope.launch { repo.updateModel(promptId, model) }

    fun updateTrustMode(promptId: String, trust: Boolean) =
        viewModelScope.launch { repo.updateTrustMode(promptId, trust) }

    /**
     * Installiert mitgelieferte Beispiel-Vorlagen (Frank-Wunsch 2026-05-21).
     * Pro Vorlage: wenn ein Prompt mit dem Namen schon existiert → skip.
     * Sonst: erstellen + write-Tools (falls in Vorlage definiert) freischalten
     * (Trust-Modus bleibt aus — Frank entscheidet bewusst).
     *
     * Liefert die Anzahl der tatsaechlich eingefuegten Vorlagen ueber den
     * Result-Callback fuer UI-Feedback.
     */
    fun installTemplates(onResult: (installed: Int, skipped: Int) -> Unit) =
        viewModelScope.launch {
            val existingNames = repo.getAll().first().map { it.name }.toSet()
            var installed = 0
            var skipped = 0
            for (tpl in de.frank.entropyreducer.domain.agentic.templates
                .AgenticPromptTemplates.ALL) {
                if (existingNames.contains(tpl.name)) {
                    skipped++
                    continue
                }
                val now = System.currentTimeMillis()
                val newId = UUID.randomUUID().toString()
                repo.upsert(
                    SavedPromptEntity(
                        id = newId,
                        name = tpl.name,
                        content = tpl.content,
                        isActive = true,
                        createdAt = now,
                        updatedAt = now,
                        category = tpl.category,
                        model = tpl.model,
                        tokenLimitPerDay = null,
                        trustModeDefault = false,
                    )
                )
                // Write-Tool-Permissions setzen (granted=true, trust=false)
                tpl.writeToolsToGrant.forEach { toolName ->
                    permissionRepo.upsert(
                        de.frank.entropyreducer.data.local.entities
                            .PromptToolPermissionEntity(
                                id = "${newId}_${toolName}",
                                promptId = newId,
                                toolName = toolName,
                                granted = true,
                                trustMode = false,
                            )
                    )
                }
                installed++
            }
            onResult(installed, skipped)
        }
}

/* ----------------- Memory ----------------- */
@HiltViewModel
class MemoryViewModel @Inject constructor(private val repo: MemoryRepository) : ViewModel() {
    val memories: StateFlow<List<MemoryEntryEntity>> =
        repo.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(entity: MemoryEntryEntity) = viewModelScope.launch {
        repo.upsert(
            entity.copy(isActive = !entity.isActive, updatedAt = System.currentTimeMillis())
        )
    }

    fun delete(entity: MemoryEntryEntity) = viewModelScope.launch { repo.delete(entity) }

    fun update(entity: MemoryEntryEntity) = viewModelScope.launch { repo.upsert(entity) }

    fun add(content: String) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        repo.upsert(
            MemoryEntryEntity(
                id = UUID.randomUUID().toString(),
                content = content,
                source = MemorySource.MANUELL,
                isActive = true,
                confidence = 100,
                createdAt = now,
                updatedAt = now,
            )
        )
    }
}

/* ----------------- Export ----------------- */

data class ExportUiState(
    val driveAccountEmail: String? = null,
    val driveBackupEnabled: Boolean = false,
    val lastBackupAtMs: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val driveStatusMessage: String? = null,
    val restoreInProgress: Boolean = false,
)

@HiltViewModel
class ExportViewModel
@Inject
constructor(
    private val entries: EntryRepository,
    private val memories: MemoryRepository,
    private val secrets: EncryptedSecretsStore,
    val signInHelper: GoogleSignInHelper,
    private val coordinator: SyncCoordinator,
    private val syncEntries: SyncEntriesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(loadInitial())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    init {
        // Coordinator-Status in unseren UI-Status spiegeln.
        viewModelScope.launch {
            coordinator.status.collect { status -> _state.update { it.copy(syncStatus = status) } }
        }
    }

    private fun loadInitial(): ExportUiState =
        ExportUiState(
            driveAccountEmail = secrets.driveAccountEmail,
            driveBackupEnabled = secrets.driveBackupEnabled,
            lastBackupAtMs = secrets.driveLastBackupEpochMs,
        )

    fun onSignInSuccess(account: GoogleSignInAccount) {
        val email =
            account.email
                ?: run {
                    _state.update {
                        it.copy(driveStatusMessage = "Konto ohne E-Mail-Adresse — abgewiesen.")
                    }
                    return
                }
        secrets.driveAccountEmail = email
        secrets.driveBackupEnabled = true
        _state.update {
            it.copy(
                driveAccountEmail = email,
                driveBackupEnabled = true,
                driveStatusMessage = "Mit $email verbunden. Prüfe Drive auf bestehenden Stand …",
            )
        }

        viewModelScope.launch {
            // KRITISCH: Auf neuem Geraet darf der erste Sign-In NICHT direkt
            // ein Backup mit dem leeren lokalen Stand hochladen — sonst wird
            // der vorhandene Drive-Backup-Stand durch 0 Eintraege ueberschrieben.
            // Stattdessen prüfen wir zuerst ob auf Drive bereits ein Backup
            // liegt: ja → restore, nein → erstes Backup hochladen.
            val hasBackup = runCatching { syncEntries.hasRemoteBackup() }.getOrDefault(false)
            if (hasBackup) {
                _state.update {
                    it.copy(driveStatusMessage = "Backup gefunden — wird heruntergeladen …")
                }
                syncEntries
                    .restoreFromDrive()
                    .onSuccess { result ->
                        val text =
                            when (result) {
                                is SyncEntriesUseCase.RestoreOutcome.NoBackup ->
                                    "Kein Backup gefunden — frisches Backup wird angelegt."
                                is SyncEntriesUseCase.RestoreOutcome.Merged ->
                                    "Wiederhergestellt: ${result.inserted} neu, ${result.updated} aktualisiert."
                            }
                        _state.update { it.copy(driveStatusMessage = text) }
                        if (result is SyncEntriesUseCase.RestoreOutcome.NoBackup) {
                            coordinator.requestImmediate()
                        }
                    }
                    .onFailure { ex ->
                        _state.update {
                            it.copy(driveStatusMessage = "Restore fehlgeschlagen: ${ex.message}")
                        }
                    }
            } else {
                _state.update {
                    it.copy(
                        driveStatusMessage = "Kein bestehender Backup-Stand. Erstes Backup laeuft …"
                    )
                }
                coordinator.requestImmediate()
            }
        }
    }

    fun onSignInError(message: String) {
        _state.update { it.copy(driveStatusMessage = "Sign-In fehlgeschlagen: $message") }
    }

    fun toggleBackupEnabled(enabled: Boolean) {
        secrets.driveBackupEnabled = enabled
        _state.update { it.copy(driveBackupEnabled = enabled) }
        if (enabled && secrets.driveAccountEmail != null) {
            coordinator.requestImmediate()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signInHelper.signOut()
            secrets.driveAccountEmail = null
            secrets.driveBackupEnabled = false
            _state.update {
                it.copy(
                    driveAccountEmail = null,
                    driveBackupEnabled = false,
                    driveStatusMessage =
                        "Vom Drive-Konto getrennt. Bestehendes Backup bleibt auf Drive.",
                )
            }
        }
    }

    fun backupNow() = syncEntries.backupNow()

    fun restoreNow() {
        viewModelScope.launch {
            _state.update { it.copy(restoreInProgress = true) }
            val result = syncEntries.restoreFromDrive()
            _state.update { st ->
                val msg =
                    result.fold(
                        onSuccess = { outcome ->
                            when (outcome) {
                                is SyncEntriesUseCase.RestoreOutcome.NoBackup ->
                                    "Kein Backup auf Drive gefunden."
                                is SyncEntriesUseCase.RestoreOutcome.Merged ->
                                    "Restore: ${outcome.inserted} neu, ${outcome.updated} aktualisiert."
                            }
                        },
                        onFailure = { ex -> "Restore fehlgeschlagen: ${ex.message}" },
                    )
                st.copy(restoreInProgress = false, driveStatusMessage = msg)
            }
        }
    }

    fun clearStatusMessage() {
        _state.update { it.copy(driveStatusMessage = null) }
    }

    fun deleteAllEntries() = viewModelScope.launch { entries.deleteAll() }

    fun deleteAllMemories() = viewModelScope.launch { memories.deleteAll() }
}

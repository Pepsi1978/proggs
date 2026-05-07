package de.frank.entropyreducer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.data.remote.drive.GoogleSignInHelper
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.remote.drive.SyncStatus
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.MemoryRepository
import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.remote.tts.GoogleTtsVoice
import de.frank.entropyreducer.data.remote.tts.GoogleTtsVoices
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.domain.model.MemorySource
import de.frank.entropyreducer.domain.tts.TtsPlayer
import de.frank.entropyreducer.domain.tts.TtsResult
import de.frank.entropyreducer.domain.usecase.SyncEntriesUseCase
import de.frank.entropyreducer.domain.usecase.TestApiKeyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

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
)
enum class ConnectionStatus { UNKNOWN, OK, FAIL, TESTING }
enum class TtsSpeakState { IDLE, LOADING, SPEAKING, ERROR }

@HiltViewModel
class ApiKeysViewModel @Inject constructor(
    private val secrets: EncryptedSecretsStore,
    private val testApi: TestApiKeyUseCase,
    private val settings: AppSettings,
    private val ttsPlayer: TtsPlayer,
) : ViewModel() {
    private val _state = MutableStateFlow(
        ApiKeysUiState(
            groqKey = secrets.groqApiKey.orEmpty(),
            geminiKey = secrets.geminiApiKey.orEmpty(),
            ttsKey = secrets.googleTtsApiKey.orEmpty(),
            groqSaved = !secrets.groqApiKey.isNullOrBlank(),
            geminiSaved = !secrets.geminiApiKey.isNullOrBlank(),
            ttsSaved = !secrets.googleTtsApiKey.isNullOrBlank(),
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
    }

    fun setGroq(value: String) { _state.update { it.copy(groqKey = value) } }
    fun setGemini(value: String) { _state.update { it.copy(geminiKey = value) } }
    fun setTts(value: String) { _state.update { it.copy(ttsKey = value) } }

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

    fun testGemini() {
        viewModelScope.launch {
            _state.update { it.copy(geminiStatus = ConnectionStatus.TESTING) }
            val result = testApi.testGemini(state.value.geminiKey.trim())
            _state.update {
                it.copy(geminiStatus = if (result.isSuccess) ConnectionStatus.OK else ConnectionStatus.FAIL)
            }
        }
    }

    /** Echter Groq-Test via GET /openai/v1/models. */
    fun testGroq() {
        viewModelScope.launch {
            _state.update { it.copy(groqStatus = ConnectionStatus.TESTING) }
            val result = testApi.testGroq(state.value.groqKey.trim())
            _state.update {
                it.copy(groqStatus = if (result.isSuccess) ConnectionStatus.OK else ConnectionStatus.FAIL)
            }
        }
    }

    /** Echter TTS-Test via GET /v1/voices?key={key}. */
    fun testTts() {
        viewModelScope.launch {
            _state.update { it.copy(ttsStatus = ConnectionStatus.TESTING) }
            val result = testApi.testTts(state.value.ttsKey.trim())
            _state.update {
                it.copy(ttsStatus = if (result.isSuccess) ConnectionStatus.OK else ConnectionStatus.FAIL)
            }
        }
    }

    /** Speichert die ausgewaehlte Chirp-3-HD-Stimme und aktualisiert das UI sofort. */
    fun setTtsVoice(voiceName: String) {
        _state.update { it.copy(ttsVoiceName = voiceName) }
        viewModelScope.launch { settings.setTtsVoice(voiceName) }
    }

    /**
     * Stoppt die Wiedergabe sofort. UI kehrt in IDLE zurueck. Fuer X-Button im
     * Voice-Picker, falls eine Probe noch laeuft.
     */
    fun stopTtsPreview() {
        ttsPlayer.stop()
        _state.update { it.copy(ttsSpeakState = TtsSpeakState.IDLE) }
    }

    /**
     * Sprich-Test: synthesiziert einen kurzen Beispiel-Text mit der aktuell
     * ausgewaehlten Stimme. UI-Spinner -> IDLE -> SPEAKING -> IDLE / ERROR.
     */
    fun testSpeak() {
        if (_state.value.ttsSpeakState == TtsSpeakState.LOADING ||
            _state.value.ttsSpeakState == TtsSpeakState.SPEAKING
        ) return
        _state.update { it.copy(ttsSpeakState = TtsSpeakState.LOADING, ttsLastError = null) }
        viewModelScope.launch {
            val result = ttsPlayer.speak(
                text = SAMPLE_PREVIEW,
                voiceNameOverride = state.value.ttsVoiceName,
                onPlaybackStart = {
                    _state.update { it.copy(ttsSpeakState = TtsSpeakState.SPEAKING) }
                },
                onComplete = {
                    _state.update { it.copy(ttsSpeakState = TtsSpeakState.IDLE) }
                },
                onError = { e ->
                    _state.update {
                        it.copy(
                            ttsSpeakState = TtsSpeakState.ERROR,
                            ttsLastError = e.message,
                        )
                    }
                },
            )
            if (result is TtsResult.Error) {
                _state.update {
                    it.copy(
                        ttsSpeakState = TtsSpeakState.ERROR,
                        ttsLastError = result.message,
                    )
                }
            }
        }
    }

    /**
     * Lifecycle-Cleanup: Wenn der Benutzer die API-Schluessel-Seite verlaesst
     * waehrend eine Sprich-Probe noch laeuft, wuerde der MediaPlayer im
     * Hintergrund weiter abspielen. ViewModel.onCleared stoppt das.
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
class ModelsViewModel @Inject constructor(
    private val settings: AppSettings,
) : ViewModel() {
    val state: StateFlow<ModelsUiState> = kotlinx.coroutines.flow.combine(
        settings.whisperModelFlow,
        settings.geminiModelFlow,
        settings.transcriptionLanguageFlow,
        settings.ttsVoiceFlow,
    ) { w, g, l, v -> ModelsUiState(w, g, l, v) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModelsUiState())

    fun setWhisper(v: String) = viewModelScope.launch { settings.setWhisperModel(v) }
    fun setGemini(v: String) = viewModelScope.launch { settings.setGeminiModel(v) }
    fun setLanguage(v: String) = viewModelScope.launch { settings.setTranscriptionLanguage(v) }
    fun setTtsVoice(v: String) = viewModelScope.launch { settings.setTtsVoice(v) }
}

/* ----------------- Profile ----------------- */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settings: AppSettings,
    private val memoryRepo: MemoryRepository,
) : ViewModel() {
    val profileText: StateFlow<String> =
        settings.profileTextFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun save(text: String) = viewModelScope.launch { settings.setProfileText(text) }

    /** Stub fuer "Aus Profil ins Gedächtnis übernehmen" — voller KI-Aufruf in Stufe 4. */
    fun distillToMemory(text: String) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        // Stufe 1: einfaches Heuristik — pro Absatz ein Memory-Eintrag, max 8.
        val paragraphs = text.split("\n\n", "\r\n\r\n").map { it.trim() }.filter { it.length in 20..400 }.take(8)
        paragraphs.forEachIndexed { idx, p ->
            memoryRepo.upsert(
                MemoryEntryEntity(
                    id = UUID.randomUUID().toString(),
                    content = p,
                    source = MemorySource.AUS_PROFIL,
                    isActive = true,
                    confidence = 80,
                    createdAt = now + idx,
                    updatedAt = now + idx,
                )
            )
        }
    }
}

/* ----------------- Prompts ----------------- */
@HiltViewModel
class PromptsViewModel @Inject constructor(
    private val repo: PromptRepository,
) : ViewModel() {
    val prompts: StateFlow<List<SavedPromptEntity>> =
        repo.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(entity: SavedPromptEntity) = viewModelScope.launch { repo.upsert(entity) }
    fun toggle(entity: SavedPromptEntity) = viewModelScope.launch {
        repo.upsert(entity.copy(isActive = !entity.isActive, updatedAt = System.currentTimeMillis()))
    }
    fun delete(entity: SavedPromptEntity) = viewModelScope.launch { repo.delete(entity) }
    fun create(name: String, content: String) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        repo.upsert(
            SavedPromptEntity(
                id = UUID.randomUUID().toString(),
                name = name, content = content, isActive = true,
                createdAt = now, updatedAt = now,
            )
        )
    }
}

/* ----------------- Memory ----------------- */
@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val repo: MemoryRepository,
) : ViewModel() {
    val memories: StateFlow<List<MemoryEntryEntity>> =
        repo.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(entity: MemoryEntryEntity) = viewModelScope.launch {
        repo.upsert(entity.copy(isActive = !entity.isActive, updatedAt = System.currentTimeMillis()))
    }
    fun delete(entity: MemoryEntryEntity) = viewModelScope.launch { repo.delete(entity) }
    fun update(entity: MemoryEntryEntity) = viewModelScope.launch { repo.upsert(entity) }
    fun add(content: String) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        repo.upsert(
            MemoryEntryEntity(
                id = UUID.randomUUID().toString(),
                content = content, source = MemorySource.MANUELL,
                isActive = true, confidence = 100,
                createdAt = now, updatedAt = now,
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
class ExportViewModel @Inject constructor(
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
            coordinator.status.collect { status ->
                _state.update { it.copy(syncStatus = status) }
            }
        }
    }

    private fun loadInitial(): ExportUiState = ExportUiState(
        driveAccountEmail = secrets.driveAccountEmail,
        driveBackupEnabled = secrets.driveBackupEnabled,
        lastBackupAtMs = secrets.driveLastBackupEpochMs,
    )

    fun onSignInSuccess(account: GoogleSignInAccount) {
        val email = account.email ?: run {
            _state.update { it.copy(driveStatusMessage = "Konto ohne E-Mail-Adresse — abgewiesen.") }
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
            // Stattdessen pruefen wir zuerst ob auf Drive bereits ein Backup
            // liegt: ja → restore, nein → erstes Backup hochladen.
            val hasBackup = runCatching { syncEntries.hasRemoteBackup() }.getOrDefault(false)
            if (hasBackup) {
                _state.update { it.copy(driveStatusMessage = "Backup gefunden — wird heruntergeladen …") }
                syncEntries.restoreFromDrive()
                    .onSuccess { result ->
                        val text = when (result) {
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
                    it.copy(driveStatusMessage = "Kein bestehender Backup-Stand. Erstes Backup laeuft …")
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
                    driveStatusMessage = "Vom Drive-Konto getrennt. Bestehendes Backup bleibt auf Drive.",
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
                val msg = result.fold(
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

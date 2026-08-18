package de.frank.denknotiz.ui

import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.frank.denknotiz.AppContainer
import de.frank.denknotiz.ai.DeviceCode
import de.frank.denknotiz.audio.GroqTranscriber
import de.frank.denknotiz.audio.MicRecorder
import de.frank.denknotiz.data.AppTheme
import de.frank.denknotiz.data.BackupPayload
import de.frank.denknotiz.data.CodexModel
import de.frank.denknotiz.data.DenknotizRepository
import de.frank.denknotiz.data.ReasoningEffort
import de.frank.denknotiz.data.SettingsSnapshot
import de.frank.denknotiz.data.TtsProvider
import de.frank.denknotiz.data.local.EntryEntity
import de.frank.denknotiz.data.local.EvaluationSnapshotEntity
import de.frank.denknotiz.data.local.SessionBundle
import de.frank.denknotiz.data.local.SessionEntity
import de.frank.denknotiz.domain.AnalysisProfiles
import de.frank.denknotiz.tts.QwenVoice
import de.frank.denknotiz.tts.QwenVoiceManager
import de.frank.denknotiz.tts.SpeechController
import de.frank.denknotiz.tts.SpeechState
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

enum class AppSection { WORKBENCH, SETTINGS }

data class InteractionState(
    val section: AppSection = AppSection.WORKBENCH,
    val selectedSessionId: String? = null,
    val draft: String = "",
    val undoDraft: String? = null,
    val focusQuestion: String = "",
    val webEnabled: Boolean = false,
    val evaluating: Boolean = false,
    val improving: Boolean = false,
    val recording: Boolean = false,
    val enrollingVoice: Boolean = false,
    val transcribing: Boolean = false,
    val rejectedAudioPath: String? = null,
    val message: String? = null,
    val deviceCode: DeviceCode? = null,
    val connectingCodex: Boolean = false,
    val qwenVoices: List<QwenVoice> = emptyList(),
    val loadingVoices: Boolean = false,
)

data class DenknotizUiState(
    val sessions: List<SessionEntity> = emptyList(),
    val bundle: SessionBundle? = null,
    val settings: SettingsSnapshot = SettingsSnapshot(),
    val interaction: InteractionState = InteractionState(),
    val speech: SpeechState = SpeechState(),
    val codexConnected: Boolean = false,
    val codexEmail: String? = null,
)

class DenknotizViewModel(
    private val container: AppContainer,
    private val mic: MicRecorder,
    private val speechController: SpeechController,
    private val cacheDir: File,
) : ViewModel() {
    private val repository: DenknotizRepository = container.repository
    private val interaction = MutableStateFlow(InteractionState())
    private var recordingTimeout: Job? = null
    private var retainedAudio: ByteArray? = null
    private var previewPlayer: MediaPlayer? = null
    private val voices = QwenVoiceManager()

    private val bundle = interaction.flatMapLatest { state ->
        state.selectedSessionId?.let(repository::observeBundle) ?: flowOf(null)
    }

    val uiState = combine(
        repository.sessions,
        bundle,
        container.settings.state,
        interaction,
        speechController.state,
    ) { sessions, selectedBundle, settings, currentInteraction, speech ->
        DenknotizUiState(sessions, selectedBundle, settings, currentInteraction, speech,
            container.codex.isConnected, container.codex.email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DenknotizUiState())

    init {
        viewModelScope.launch {
            repository.sessions.collect { sessions ->
                val selected = interaction.value.selectedSessionId
                if (selected == null || sessions.none { it.id == selected }) {
                    val id = sessions.firstOrNull { !it.archived }?.id ?: repository.createSession()
                    interaction.value = interaction.value.copy(selectedSessionId = id)
                }
            }
        }
    }

    fun selectSection(section: AppSection) = update { copy(section = section) }
    fun selectSession(id: String) = update { copy(selectedSessionId = id, section = AppSection.WORKBENCH) }
    fun setDraft(value: String) = update { copy(draft = value) }
    fun setFocus(value: String) = update { copy(focusQuestion = value) }
    fun setWeb(value: Boolean) = update { copy(webEnabled = value) }
    fun consumeMessage() = update { copy(message = null) }

    fun newSession() = launchAction { selectSession(repository.createSession()) }

    fun renameSession(session: SessionEntity, title: String) = launchAction {
        repository.updateSession(session.copy(title = title.trim().ifBlank { session.title }, titleManual = true))
    }

    fun togglePin(session: SessionEntity) = launchAction { repository.updateSession(session.copy(pinned = !session.pinned)) }
    fun toggleArchive(session: SessionEntity) = launchAction { repository.updateSession(session.copy(archived = !session.archived)) }
    fun deleteSession(session: SessionEntity) = launchAction { repository.deleteSession(session.id) }

    fun sendDraft() {
        val text = interaction.value.draft.trim()
        val sessionId = interaction.value.selectedSessionId ?: return
        if (text.isBlank()) return
        update { copy(draft = "", undoDraft = null) }
        launchAction {
            val entry = repository.addNote(sessionId, text)
            val settings = container.settings.state.value
            if (container.codex.isConnected || settings.codexToken.isNotBlank()) {
                runCatching { container.codex.title(text, settings.model, settings.codexToken) }
                    .getOrNull()?.let { repository.setGeneratedTitles(sessionId, entry.id, it) }
            }
        }
    }

    fun editNote(entry: EntryEntity, text: String) = launchAction { repository.editNote(entry.id, text) }
    fun editNoteTitle(entry: EntryEntity, title: String) = launchAction { repository.editNoteTitle(entry.id, title) }
    fun duplicateNote(entry: EntryEntity) = launchAction { repository.duplicateNote(entry.id) }
    fun improveNote(entry: EntryEntity) = launchAction {
        val settings = container.settings.state.value
        val improved = container.codex.improve(entry.text, settings.model, settings.reasoning, settings.codexToken)
        repository.improveNote(entry.id, improved)
    }
    fun restoreNote(entry: EntryEntity) = launchAction { repository.restoreNote(entry.id) }
    fun deleteEntry(entry: EntryEntity) = launchAction { repository.deleteEntry(entry.id) }
    fun responseAsNote(entry: EntryEntity) = launchAction { repository.responseAsNote(entry.id) }

    fun improveDraft() {
        val current = interaction.value.draft.trim()
        if (current.isBlank() || interaction.value.improving) return
        update { copy(improving = true) }
        launchAction(onFinally = { update { copy(improving = false) } }) {
            val settings = container.settings.state.value
            val improved = container.codex.improve(current, settings.model, settings.reasoning, settings.codexToken)
            update { copy(draft = improved, undoDraft = current) }
        }
    }

    fun undoImprovement() {
        val old = interaction.value.undoDraft ?: return
        update { copy(draft = old, undoDraft = null) }
    }

    fun evaluate() {
        val state = interaction.value
        val sessionId = state.selectedSessionId ?: return
        if (state.evaluating) return
        launchAction {
            val settings = container.settings.state.value
            val snapshot = repository.createSnapshot(sessionId, state.focusQuestion, settings.profileId, state.webEnabled,
                settings.model.apiId, settings.reasoning.apiValue, DenknotizRepository.MODEL_CHUNK_CHARS)
            runEvaluation(snapshot)
        }
    }

    fun retry(snapshotId: String) = launchAction { runEvaluation(repository.beginRetry(snapshotId)) }

    private suspend fun runEvaluation(snapshot: EvaluationSnapshotEntity) {
        update { copy(evaluating = true) }
        try {
            val chunks = repository.snapshotInput(snapshot)
            val profile = AnalysisProfiles.firstOrNull { it.id == snapshot.profileId } ?: AnalysisProfiles.first()
            val model = CodexModel.entries.firstOrNull { it.apiId == snapshot.model } ?: CodexModel.TERRA
            val reasoning = ReasoningEffort.entries.firstOrNull { it.apiValue == snapshot.reasoning } ?: ReasoningEffort.MEDIUM
            val result = container.codex.evaluate(chunks, snapshot.focusQuestion, profile.instruction, model, reasoning,
                snapshot.webEnabled, container.settings.state.value.codexToken)
            val citations = JSONArray(result.sources.map { source ->
                org.json.JSONObject().put("title", source.title).put("url", source.url)
            }).toString()
            repository.completeSnapshot(snapshot, result.text, citations)
        } catch (cancelled: CancellationException) {
            repository.markSnapshotFailed(snapshot, "Abgebrochen")
            throw cancelled
        } catch (error: Exception) {
            repository.markSnapshotFailed(snapshot, error.message ?: "Auswertung fehlgeschlagen")
            throw error
        } finally {
            update { copy(evaluating = false) }
        }
    }

    fun startRecording(): Boolean {
        if (interaction.value.recording || interaction.value.transcribing) return false
        val started = mic.start(viewModelScope)
        if (started) {
            update { copy(recording = true) }
            recordingTimeout = viewModelScope.launch { delay(10 * 60_000L); stopRecording() }
        }
        return started
    }

    fun stopRecording() {
        if (!interaction.value.recording) return
        recordingTimeout?.cancel(); recordingTimeout = null
        update { copy(recording = false, transcribing = true) }
        viewModelScope.launch {
            try {
                val wav = mic.stop() ?: return@launch
                transcribe(wav)
            } catch (error: Exception) {
                message(error.message ?: "Transkription fehlgeschlagen")
            } finally {
                update { copy(transcribing = false) }
            }
        }
    }

    fun startVoiceEnrollmentRecording(): Boolean {
        if (interaction.value.recording || interaction.value.enrollingVoice || interaction.value.transcribing) return false
        val started = mic.start(viewModelScope, MicRecorder.CLONING_SAMPLE_RATE)
        if (started) {
            update { copy(enrollingVoice = true) }
            recordingTimeout = viewModelScope.launch { delay(10 * 60_000L); stopVoiceEnrollmentRecording("Stimme") }
        }
        return started
    }

    fun stopVoiceEnrollmentRecording(name: String) {
        if (!interaction.value.enrollingVoice) return
        recordingTimeout?.cancel(); recordingTimeout = null
        update { copy(enrollingVoice = false, loadingVoices = true) }
        viewModelScope.launch {
            try {
                val wav = mic.stop() ?: error("Die Stimmaufnahme ist leer.")
                val id = voices.enroll(container.settings.state.value.qwenKey, name, wav)
                saveSettings { copy(qwenVoiceId = id, qwenVoiceNames = qwenVoiceNames + (id to name.trim())) }
                val availableVoices = voices.list(container.settings.state.value.qwenKey)
                update { copy(qwenVoices = availableVoices, message = "Stimme wurde angelegt.") }
            } catch (error: Exception) {
                message(error.message ?: "Stimme konnte nicht angelegt werden.")
            } finally {
                update { copy(loadingVoices = false) }
            }
        }
    }

    fun retryRejectedAudio() {
        val wav = retainedAudio ?: return
        update { copy(transcribing = true) }
        viewModelScope.launch {
            try { transcribe(wav) } catch (error: Exception) { message(error.message ?: "Transkription fehlgeschlagen") }
            finally { update { copy(transcribing = false) } }
        }
    }

    private suspend fun transcribe(wav: ByteArray) {
        val result = GroqTranscriber(container.settings.state.value.groqKey).transcribe(wav)
        if (result.text.isBlank()) {
            retainedAudio = wav
            val file = File(cacheDir, "gefilterte_aufnahme.wav").apply { writeBytes(wav) }
            update { copy(rejectedAudioPath = file.absolutePath, message = "Die Aufnahme wurde vollständig gefiltert und zur Prüfung behalten.") }
        } else {
            deleteRejectedAudio()
            update { copy(draft = listOf(draft.trim(), result.text.trim()).filter(String::isNotBlank).joinToString("\n\n")) }
        }
    }

    fun playRejectedAudio() {
        val path = interaction.value.rejectedAudioPath ?: return
        previewPlayer?.release()
        previewPlayer = MediaPlayer().apply { setDataSource(path); prepare(); start() }
    }

    fun deleteRejectedAudio() {
        previewPlayer?.release(); previewPlayer = null
        interaction.value.rejectedAudioPath?.let { File(it).delete() }
        retainedAudio = null
        update { copy(rejectedAudioPath = null) }
    }

    fun read(text: String) = speechController.play(text)
    fun speechToggle() = speechController.pauseResume()
    fun speechStop() = speechController.stop()
    fun speechPrevious() = speechController.previous()
    fun speechNext() = speechController.next()

    fun connectCodex() {
        if (interaction.value.connectingCodex) return
        update { copy(connectingCodex = true, deviceCode = null) }
        launchAction(onFinally = { update { copy(connectingCodex = false) } }) {
            container.codex.connect { code -> update { copy(deviceCode = code) } }
            update { copy(deviceCode = null, message = "Codex ist verbunden.") }
        }
    }

    fun disconnectCodex() { container.codex.disconnect(); update { copy(message = "Codex wurde getrennt.") } }

    fun setTheme(value: AppTheme) = saveSettings { copy(theme = value) }
    fun setModel(value: CodexModel) = saveSettings { copy(model = value) }
    fun setReasoning(value: ReasoningEffort) = saveSettings { copy(reasoning = value) }
    fun setProfile(value: String) = saveSettings { copy(profileId = value) }
    fun setTtsProvider(value: TtsProvider) = saveSettings { copy(ttsProvider = value) }
    fun setSpeechRate(value: Float) = saveSettings { copy(speechRate = value) }
    fun setReducedMotion(value: Boolean) = saveSettings { copy(reducedMotion = value) }
    fun setKeys(groq: String, google: String, qwen: String, codex: String) = saveSettings {
        copy(groqKey = groq, googleKey = google, qwenKey = qwen, codexToken = codex)
    }
    fun setVoices(chirp: String, edge: String, qwen: String) = saveSettings {
        copy(chirpVoice = chirp, edgeVoice = edge, qwenVoiceId = qwen)
    }

    fun loadQwenVoices() {
        update { copy(loadingVoices = true) }
        launchAction(onFinally = { update { copy(loadingVoices = false) } }) {
            val availableVoices = voices.list(container.settings.state.value.qwenKey)
            update { copy(qwenVoices = availableVoices) }
        }
    }

    fun deleteQwenVoice(voice: QwenVoice) = launchAction {
        voices.delete(container.settings.state.value.qwenKey, voice.id)
        if (container.settings.state.value.qwenVoiceId == voice.id) saveSettings { copy(qwenVoiceId = "") }
        loadQwenVoices()
    }

    fun renameQwenVoice(voice: QwenVoice, name: String) = saveSettings {
        copy(qwenVoiceNames = qwenVoiceNames + (voice.id to name.trim().ifBlank { voice.name }))
    }

    fun enrollVoice(name: String, wav: ByteArray) = launchAction {
        val id = voices.enroll(container.settings.state.value.qwenKey, name, wav)
        saveSettings { copy(qwenVoiceId = id, qwenVoiceNames = qwenVoiceNames + (id to name.trim())) }
        loadQwenVoices()
    }

    fun export(uri: Uri) = launchAction {
        container.backup.write(uri, repository.backup())
        message("Sicherung wurde geschrieben. Zugangsdaten waren nicht enthalten.")
    }

    fun import(uri: Uri) = launchAction {
        val count = repository.merge(container.backup.read(uri))
        message("$count neue Datensätze wurden zusammengeführt; gleiche IDs blieben unverändert.")
    }

    private fun saveSettings(transform: SettingsSnapshot.() -> SettingsSnapshot) = container.settings.update { it.transform() }
    private fun update(transform: InteractionState.() -> InteractionState) { interaction.value = interaction.value.transform() }
    private fun message(value: String) = update { copy(message = value) }
    private fun launchAction(onFinally: () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message(error.message ?: "Aktion fehlgeschlagen") }
            finally { onFinally() }
        }
    }

    override fun onCleared() {
        mic.release(); previewPlayer?.release(); speechController.stop(); super.onCleared()
    }

    class Factory(
        private val container: AppContainer,
        private val mic: MicRecorder,
        private val speechController: SpeechController,
        private val cacheDir: File,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DenknotizViewModel(container, mic, speechController, cacheDir) as T
    }
}

package de.frank.entropyreducer.presentation.dashboard3

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.audio.AudioRecorder
import de.frank.entropyreducer.data.audio.RecordingService
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.ScientistMessageEntity
import de.frank.entropyreducer.data.local.entities.ScientistSessionEntity
import de.frank.entropyreducer.data.repository.HypothesisRepository
import de.frank.entropyreducer.data.repository.ScientistRepository
import de.frank.entropyreducer.domain.usecase.ScientistChatUseCase
import de.frank.entropyreducer.domain.usecase.TranscribeAudioUseCase
import de.frank.entropyreducer.presentation.components.MicState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ScientistUiState(
    val sessions: List<ScientistSessionEntity> = emptyList(),
    val currentSessionId: String? = null,
    val messages: List<ScientistMessageEntity> = emptyList(),
    val hypothesesByMessageId: Map<String, List<HypothesisEntity>> = emptyMap(),
    val draftText: String = "",
    val isThinking: Boolean = false,
    val micState: MicState = MicState.IDLE,
    val processingMessage: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class ScientistViewModel @Inject constructor(
    application: Application,
    private val scientist: ScientistRepository,
    private val hypotheses: HypothesisRepository,
    private val chat: ScientistChatUseCase,
    private val recorder: AudioRecorder,
    private val transcribe: TranscribeAudioUseCase,
) : AndroidViewModel(application) {

    private val currentSessionFlow = MutableStateFlow<String?>(null)
    private val draftFlow = MutableStateFlow("")
    private val uiOnlyFlow = MutableStateFlow(UiOnly())

    private data class UiOnly(
        val isThinking: Boolean = false,
        val micState: MicState = MicState.IDLE,
        val processingMessage: String? = null,
        val errorMessage: String? = null,
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val messagesFlow = currentSessionFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else scientist.observeMessages(id)
    }

    val state: StateFlow<ScientistUiState> = combine(
        scientist.observeActiveSessions(),
        currentSessionFlow,
        messagesFlow,
        hypotheses.observeAll(),
        combine(draftFlow, uiOnlyFlow) { draft, ui -> draft to ui },
    ) { sessions, currentId, msgs, allHyps, (draft, ui) ->
        val hypById = allHyps.associateBy { it.id }
        val byMsg = msgs.associate { msg ->
            msg.id to msg.attachedHypothesisIds.mapNotNull { hypById[it] }
        }
        ScientistUiState(
            sessions = sessions,
            currentSessionId = currentId,
            messages = msgs,
            hypothesesByMessageId = byMsg,
            draftText = draft,
            isThinking = ui.isThinking,
            micState = ui.micState,
            processingMessage = ui.processingMessage,
            errorMessage = ui.errorMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScientistUiState())

    init {
        // Initial: erste verfuegbare Session waehlen oder eine neue starten.
        viewModelScope.launch {
            val sessions = scientist.observeActiveSessions().first()
            currentSessionFlow.value = sessions.firstOrNull()?.id ?: createSessionInternal()
        }
    }

    fun selectSession(id: String) {
        currentSessionFlow.value = id
    }

    fun newSession() {
        viewModelScope.launch {
            currentSessionFlow.value = createSessionInternal()
            // Kickoff in der frischen Session ausloesen.
            triggerAi(userText = null)
        }
    }

    fun archiveCurrent() {
        val id = currentSessionFlow.value ?: return
        viewModelScope.launch {
            scientist.getSession(id)?.let { scientist.archiveSession(it) }
            // Naechste aktive Session waehlen
            val next = scientist.observeActiveSessions().first()
                .firstOrNull { it.id != id }
            currentSessionFlow.value = next?.id ?: createSessionInternal()
        }
    }

    fun setDraft(text: String) {
        draftFlow.value = text
    }

    fun send() {
        val text = draftFlow.value.trim()
        if (text.isBlank() || uiOnlyFlow.value.isThinking) return
        draftFlow.value = ""
        triggerAi(text)
    }

    /** Kickoff der KI-Antwort nach dem ersten Oeffnen einer leeren Session. */
    fun maybeKickoff() {
        val id = currentSessionFlow.value ?: return
        viewModelScope.launch {
            val msgs = scientist.observeMessages(id).first()
            if (msgs.isEmpty() && !uiOnlyFlow.value.isThinking) triggerAi(null)
        }
    }

    fun onMicClick() {
        val app = getApplication<Application>()
        when (uiOnlyFlow.value.micState) {
            MicState.IDLE -> {
                runCatching {
                    recorder.start()
                    RecordingService.start(app)
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        micState = MicState.RECORDING,
                        processingMessage = "Aufnahme laeuft …",
                    )
                }.onFailure { ex ->
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        micState = MicState.IDLE,
                        errorMessage = ex.message ?: "Aufnahme konnte nicht gestartet werden",
                    )
                }
            }
            MicState.RECORDING -> {
                val file = recorder.stop()
                RecordingService.stop(app)
                if (file == null || !file.exists() || file.length() == 0L) {
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        micState = MicState.IDLE,
                        processingMessage = null,
                    )
                    return
                }
                uiOnlyFlow.value = uiOnlyFlow.value.copy(
                    micState = MicState.PROCESSING,
                    processingMessage = "Transkribiere …",
                )
                viewModelScope.launch {
                    transcribe(file).onSuccess { transcript ->
                        uiOnlyFlow.value = uiOnlyFlow.value.copy(
                            micState = MicState.IDLE,
                            processingMessage = null,
                        )
                        if (transcript.isNotBlank()) {
                            draftFlow.value = (draftFlow.value + " " + transcript).trim()
                        }
                    }.onFailure { ex ->
                        uiOnlyFlow.value = uiOnlyFlow.value.copy(
                            micState = MicState.IDLE,
                            processingMessage = null,
                            errorMessage = ex.message ?: "Transkription fehlgeschlagen",
                        )
                    }
                }
            }
            MicState.PROCESSING -> Unit
        }
    }

    fun dismissError() {
        uiOnlyFlow.value = uiOnlyFlow.value.copy(errorMessage = null)
    }

    /** Markiert eine vorgeschlagene Hypothese als AKTIV mit den gewaehlten Daten. */
    fun startHypothesis(hypothesis: HypothesisEntity, plannedStartMs: Long) {
        viewModelScope.launch {
            val durationMs = hypothesis.plannedEndDate - hypothesis.plannedStartDate
            hypotheses.update(
                hypothesis.copy(
                    status = de.frank.entropyreducer.domain.model.HypothesisStatus.AKTIV,
                    plannedStartDate = plannedStartMs,
                    plannedEndDate = plannedStartMs + durationMs.coerceAtLeast(24L * 60 * 60 * 1000),
                    actualStartDate = plannedStartMs,
                ),
            )
        }
    }

    private fun triggerAi(userText: String?) {
        val id = currentSessionFlow.value ?: return
        viewModelScope.launch {
            uiOnlyFlow.value = uiOnlyFlow.value.copy(isThinking = true)
            chat(sessionId = id, userText = userText).onFailure { ex ->
                uiOnlyFlow.value = uiOnlyFlow.value.copy(
                    isThinking = false,
                    errorMessage = ex.message ?: "Antwort vom Wissenschaftler fehlgeschlagen",
                )
            }.onSuccess {
                uiOnlyFlow.value = uiOnlyFlow.value.copy(isThinking = false)
            }
        }
    }

    private suspend fun createSessionInternal(): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        scientist.upsertSession(
            ScientistSessionEntity(
                id = id,
                title = "Neue Session",
                createdAt = now,
                lastActiveAt = now,
                isArchived = false,
            ),
        )
        return id
    }

    override fun onCleared() {
        super.onCleared()
        if (recorder.isRecording()) recorder.discard()
    }
}

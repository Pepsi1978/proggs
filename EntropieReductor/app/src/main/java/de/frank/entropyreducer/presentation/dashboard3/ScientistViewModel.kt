package de.frank.entropyreducer.presentation.dashboard3

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.audio.AudioRecorder
import de.frank.entropyreducer.data.audio.RecordingService
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.HypothesisMessageEntity
import de.frank.entropyreducer.data.local.entities.ScientistMessageEntity
import de.frank.entropyreducer.data.local.entities.ScientistSessionEntity
import de.frank.entropyreducer.data.repository.HypothesisMessageRepository
import de.frank.entropyreducer.data.repository.HypothesisRepository
import de.frank.entropyreducer.data.repository.MemoryRepository
import de.frank.entropyreducer.data.repository.ScientistQuestionRepository
import de.frank.entropyreducer.data.repository.ScientistRepository
import de.frank.entropyreducer.domain.kiquestion.GenerateScientistQuestionUseCase
import de.frank.entropyreducer.domain.kiquestion.KiQuestion
import de.frank.entropyreducer.domain.model.MemorySource
import de.frank.entropyreducer.domain.usecase.HypothesisChatUseCase
import de.frank.entropyreducer.domain.usecase.ScientistChatUseCase
import de.frank.entropyreducer.domain.usecase.TranscribeAudioUseCase
import de.frank.entropyreducer.presentation.components.MicState
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@androidx.compose.runtime.Immutable
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
    // Frank-Wunsch 2026-05-09: nach erfolgreicher Whisper-Transkription enthaelt
    // dieses Feld den aufgenommenen Text. Solange != null zeigt der Screen den
    // TranscriptEditDialog — Frank kann editieren oder absenden.
    val pendingTranscript: String? = null,
    // Inline-Hypothesen-Chat (Sheet pro Hypothese)
    val activeHypothesisDetailId: String? = null,
    val hypothesisDetail: HypothesisEntity? = null,
    val hypothesisMessages: List<HypothesisMessageEntity> = emptyList(),
    val hypothesisDraftText: String = "",
    val hypothesisIsThinking: Boolean = false,
    val hypothesisMicState: MicState = MicState.IDLE,
    val hypothesisProcessingMessage: String? = null,
    // Loesch-Dialog
    val pendingDeleteHypothesisId: String? = null,
    // Proaktive Forscher-Frage
    val scientistQuestion: KiQuestion? = null,
)

@HiltViewModel
class ScientistViewModel @Inject constructor(
    application: Application,
    private val scientist: ScientistRepository,
    private val hypotheses: HypothesisRepository,
    private val hypothesisMessages: HypothesisMessageRepository,
    private val chat: ScientistChatUseCase,
    private val hypothesisChat: HypothesisChatUseCase,
    private val recorder: AudioRecorder,
    private val transcribe: TranscribeAudioUseCase,
    private val biomarkerDao: de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao,
    private val scientistQuestions: ScientistQuestionRepository,
    private val generateScientistQuestion: GenerateScientistQuestionUseCase,
    private val memories: MemoryRepository,
    private val scientistDatabase: de.frank.entropyreducer.data.local.ScientistDatabase,
) : AndroidViewModel(application) {

    private val currentSessionFlow = MutableStateFlow<String?>(null)
    private val draftFlow = MutableStateFlow("")
    private val uiOnlyFlow = MutableStateFlow(UiOnly())
    private val hypothesisDetailIdFlow = MutableStateFlow<String?>(null)
    private val hypothesisDraftFlow = MutableStateFlow("")
    private val hypothesisUiFlow = MutableStateFlow(HypothesisUi())
    private val pendingDeleteFlow = MutableStateFlow<String?>(null)

    private data class UiOnly(
        val isThinking: Boolean = false,
        val micState: MicState = MicState.IDLE,
        val processingMessage: String? = null,
        val errorMessage: String? = null,
        val pendingTranscript: String? = null,
    )

    private data class HypothesisUi(
        val isThinking: Boolean = false,
        val micState: MicState = MicState.IDLE,
        val processingMessage: String? = null,
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val messagesFlow = currentSessionFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else scientist.observeMessages(id)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val hypothesisMessagesFlow = hypothesisDetailIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else hypothesisMessages.observeForHypothesis(id)
    }

    private data class HypothesisBlock(
        val detailId: String?,
        val messages: List<HypothesisMessageEntity>,
        val draftText: String,
        val ui: HypothesisUi,
    )

    private data class GlobalChatBlock(
        val draft: String,
        val ui: UiOnly,
        val pendingDeleteId: String?,
        val hypothesisBlock: HypothesisBlock,
        val scientistQuestion: KiQuestion?,
    )

    private val hypothesisBlockFlow: kotlinx.coroutines.flow.Flow<HypothesisBlock> = combine(
        hypothesisDetailIdFlow,
        hypothesisMessagesFlow,
        hypothesisDraftFlow,
        hypothesisUiFlow,
    ) { detailId, messages, draft, ui ->
        HypothesisBlock(detailId, messages, draft, ui)
    }

    private val globalChatBlockFlow: kotlinx.coroutines.flow.Flow<GlobalChatBlock> = combine(
        draftFlow,
        uiOnlyFlow,
        pendingDeleteFlow,
        hypothesisBlockFlow,
        scientistQuestions.currentQuestion,
    ) { draft, ui, pendingDel, hBlock, sciQ ->
        GlobalChatBlock(draft, ui, pendingDel, hBlock, sciQ)
    }

    val state: StateFlow<ScientistUiState> = combine(
        scientist.observeActiveSessions(),
        currentSessionFlow,
        messagesFlow,
        hypotheses.observeAll(),
        globalChatBlockFlow,
    ) { sessions, currentId, msgs, allHyps, block ->
        val hypById = allHyps.associateBy { it.id }
        val byMsg = msgs.associate { msg ->
            msg.id to msg.attachedHypothesisIds.mapNotNull { hypById[it] }
        }
        ScientistUiState(
            sessions = sessions,
            currentSessionId = currentId,
            messages = msgs,
            hypothesesByMessageId = byMsg,
            draftText = block.draft,
            isThinking = block.ui.isThinking,
            micState = block.ui.micState,
            processingMessage = block.ui.processingMessage,
            errorMessage = block.ui.errorMessage,
            pendingTranscript = block.ui.pendingTranscript,
            activeHypothesisDetailId = block.hypothesisBlock.detailId,
            hypothesisDetail = block.hypothesisBlock.detailId?.let { hypById[it] },
            hypothesisMessages = block.hypothesisBlock.messages,
            hypothesisDraftText = block.hypothesisBlock.draftText,
            hypothesisIsThinking = block.hypothesisBlock.ui.isThinking,
            hypothesisMicState = block.hypothesisBlock.ui.micState,
            hypothesisProcessingMessage = block.hypothesisBlock.ui.processingMessage,
            pendingDeleteHypothesisId = block.pendingDeleteId,
            scientistQuestion = block.scientistQuestion,
        )
    }
        // Performance-Fix Loop 2.4: combine-Block macht hypById.associateBy +
        // msgs.associate { ... mapNotNull { hypById[it] } }. Bei jedem Update
        // einer der 5 Source-Flows (auch draftFlow → globalChatBlockFlow!)
        // wurde das auf Main durchgerechnet. Bei einem Chat mit 50 Messages und
        // 100 Hypothesen sind das pro Tastendruck 5000 Hashtable-Lookups +
        // Map-Allokation — sichtbar als Tipp-Verzoegerung im Eingabefeld.
        // .flowOn(Default) verlagert die Berechnung off-main, ScientistUiState
        // (@Immutable) wird thread-safe an Main propagiert.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), ScientistUiState())

    init {
        viewModelScope.launch {
            val sessions = scientist.observeActiveSessions().first()
            currentSessionFlow.value = sessions.firstOrNull()?.id ?: createSessionInternal()
        }
        // Beim Oeffnen des Forscher-Bereichs: pruefen, ob eine Forscher-Frage geladen werden soll.
        viewModelScope.launch {
            val current = scientistQuestions.currentQuestion.first()
            val dismissedUntil = scientistQuestions.dismissedUntilMs.first()
            if (current == null && System.currentTimeMillis() >= dismissedUntil) {
                refreshScientistQuestion()
            }
        }
    }

    fun selectSession(id: String) {
        currentSessionFlow.value = id
    }

    fun newSession() {
        viewModelScope.launch {
            currentSessionFlow.value = createSessionInternal()
            triggerAi(userText = null)
        }
    }

    fun archiveCurrent() {
        val id = currentSessionFlow.value ?: return
        viewModelScope.launch {
            scientist.getSession(id)?.let { scientist.archiveSession(it) }
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
                        // Frank-Wunsch 2026-05-09: Statt das Transkript in einen
                        // Draft-Text-Feld zu haengen, oeffnet sich ein Edit-Dialog.
                        // pendingTranscript != null triggert im UI das Pop-up.
                        if (transcript.isNotBlank()) {
                            uiOnlyFlow.value = uiOnlyFlow.value.copy(
                                micState = MicState.IDLE,
                                processingMessage = null,
                                pendingTranscript = transcript.trim(),
                            )
                        } else {
                            uiOnlyFlow.value = uiOnlyFlow.value.copy(
                                micState = MicState.IDLE,
                                processingMessage = null,
                            )
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

    // ====== Pending Transcript (TranscriptEditDialog) ======

    fun editPendingTranscript(text: String) {
        uiOnlyFlow.value = uiOnlyFlow.value.copy(pendingTranscript = text)
    }

    fun sendPendingTranscript() {
        val text = uiOnlyFlow.value.pendingTranscript?.trim().orEmpty()
        if (text.isBlank() || uiOnlyFlow.value.isThinking) return
        uiOnlyFlow.value = uiOnlyFlow.value.copy(pendingTranscript = null)
        triggerAi(text)
    }

    fun dismissPendingTranscript() {
        uiOnlyFlow.value = uiOnlyFlow.value.copy(pendingTranscript = null)
    }

    fun startHypothesis(hypothesis: HypothesisEntity, plannedStartMs: Long) {
        viewModelScope.launch {
            val durationMs = hypothesis.plannedEndDate - hypothesis.plannedStartDate
            val beforeId = hypothesis.biomarkerBeforeId ?: biomarkerDao.getLatest().first()?.id
            hypotheses.update(
                hypothesis.copy(
                    status = de.frank.entropyreducer.domain.model.HypothesisStatus.AKTIV,
                    plannedStartDate = plannedStartMs,
                    plannedEndDate = plannedStartMs + durationMs.coerceAtLeast(24L * 60 * 60 * 1000),
                    actualStartDate = plannedStartMs,
                    biomarkerBeforeId = beforeId,
                ),
            )
        }
    }

    // ====== Inline-Hypothesen-Chat ======

    fun openHypothesisDetail(hypothesisId: String) {
        hypothesisDetailIdFlow.value = hypothesisId
        // Beim Oeffnen einer leeren Diskussion eroeffnet der Wissenschaftler von selbst.
        viewModelScope.launch {
            val existing = hypothesisMessages.getForHypothesis(hypothesisId)
            if (existing.isEmpty() && !hypothesisUiFlow.value.isThinking) {
                triggerHypothesisAi(hypothesisId, userText = null)
            }
        }
    }

    fun closeHypothesisDetail() {
        hypothesisDetailIdFlow.value = null
        hypothesisDraftFlow.value = ""
        // Aufnahme abbrechen falls noch laeuft, sonst koennte die Aufnahme im Hintergrund weiterlaufen.
        if (recorder.isRecording() && hypothesisUiFlow.value.micState == MicState.RECORDING) {
            runCatching {
                recorder.discard()
                RecordingService.stop(getApplication())
            }
            hypothesisUiFlow.value = hypothesisUiFlow.value.copy(
                micState = MicState.IDLE,
                processingMessage = null,
            )
        }
    }

    fun setHypothesisDraft(text: String) {
        hypothesisDraftFlow.value = text
    }

    fun sendInHypothesisChat() {
        val id = hypothesisDetailIdFlow.value ?: return
        val text = hypothesisDraftFlow.value.trim()
        if (text.isBlank() || hypothesisUiFlow.value.isThinking) return
        hypothesisDraftFlow.value = ""
        triggerHypothesisAi(id, text)
    }

    fun onHypothesisMicClick() {
        val app = getApplication<Application>()
        // Globale Mic-Aufnahme darf nicht parallel laufen.
        if (uiOnlyFlow.value.micState != MicState.IDLE) return
        when (hypothesisUiFlow.value.micState) {
            MicState.IDLE -> {
                runCatching {
                    recorder.start()
                    RecordingService.start(app)
                    hypothesisUiFlow.value = hypothesisUiFlow.value.copy(
                        micState = MicState.RECORDING,
                        processingMessage = "Aufnahme laeuft …",
                    )
                }.onFailure { ex ->
                    hypothesisUiFlow.value = hypothesisUiFlow.value.copy(
                        micState = MicState.IDLE,
                    )
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        errorMessage = ex.message ?: "Aufnahme konnte nicht gestartet werden",
                    )
                }
            }
            MicState.RECORDING -> {
                val file = recorder.stop()
                RecordingService.stop(app)
                if (file == null || !file.exists() || file.length() == 0L) {
                    hypothesisUiFlow.value = hypothesisUiFlow.value.copy(
                        micState = MicState.IDLE,
                        processingMessage = null,
                    )
                    return
                }
                hypothesisUiFlow.value = hypothesisUiFlow.value.copy(
                    micState = MicState.PROCESSING,
                    processingMessage = "Transkribiere …",
                )
                viewModelScope.launch {
                    transcribe(file).onSuccess { transcript ->
                        hypothesisUiFlow.value = hypothesisUiFlow.value.copy(
                            micState = MicState.IDLE,
                            processingMessage = null,
                        )
                        if (transcript.isNotBlank()) {
                            hypothesisDraftFlow.value = (hypothesisDraftFlow.value + " " + transcript).trim()
                        }
                    }.onFailure { ex ->
                        hypothesisUiFlow.value = hypothesisUiFlow.value.copy(
                            micState = MicState.IDLE,
                            processingMessage = null,
                        )
                        uiOnlyFlow.value = uiOnlyFlow.value.copy(
                            errorMessage = ex.message ?: "Transkription fehlgeschlagen",
                        )
                    }
                }
            }
            MicState.PROCESSING -> Unit
        }
    }

    private fun triggerHypothesisAi(hypothesisId: String, userText: String?) {
        viewModelScope.launch {
            hypothesisUiFlow.value = hypothesisUiFlow.value.copy(isThinking = true)
            hypothesisChat(hypothesisId = hypothesisId, userText = userText).onFailure { ex ->
                hypothesisUiFlow.value = hypothesisUiFlow.value.copy(isThinking = false)
                uiOnlyFlow.value = uiOnlyFlow.value.copy(
                    errorMessage = ex.message ?: "Antwort vom Wissenschaftler fehlgeschlagen",
                )
            }.onSuccess {
                hypothesisUiFlow.value = hypothesisUiFlow.value.copy(isThinking = false)
            }
        }
    }

    // ====== Hypothese loeschen ======

    fun requestDeleteHypothesis(hypothesisId: String) {
        pendingDeleteFlow.value = hypothesisId
    }

    fun dismissDeleteConfirmation() {
        pendingDeleteFlow.value = null
    }

    fun confirmDeleteHypothesis() {
        val id = pendingDeleteFlow.value ?: return
        viewModelScope.launch {
            val h = hypotheses.get(id)
            if (h != null) {
                // Wenn dies die gerade offene Hypothese ist: Sheet schliessen.
                if (hypothesisDetailIdFlow.value == id) {
                    hypothesisDetailIdFlow.value = null
                    hypothesisDraftFlow.value = ""
                }
                // Frank-Wunsch 2026-05-09: Beim Loeschen einer Hypothese soll der GANZE
                // Wissenschaftler-Eintrag (Einleitungstext + Hypothesen-Karte) verschwinden,
                // nicht nur die Karte. Der Text in der Bubble bezieht sich genau auf diese
                // Hypothese und ist ohne sie sinnlos.
                //
                // Strategie:
                // - Alle ScientistMessages durchgehen, die diese Hypothese-ID in
                //   attachedHypothesisIds tragen.
                // - Wenn die Hypothese die EINZIGE in der Message ist: ganze Message loeschen.
                // - Wenn mehrere drin sind: nur die ID rauswerfen, Message bleibt erhalten
                //   (Frank's Aussage zielt auf den haeufigen Fall 1-Hypothese-pro-Message;
                //   beim seltenen Mehrfach-Fall ist Message-Erhalt sicherer).
                // Performance-Audit Loop 4 (2026-05-10): atomare Transaktion ueber
                // alle Message-Mutationen + Hypothesen-Delete. Vorher konnte ein
                // Process-Kill mid-loop orphaned Message-Referenzen hinterlassen.
                runCatching {
                    scientistDatabase.withTransaction {
                        val all = scientist.getAllMessages()
                        all.filter { it.attachedHypothesisIds.contains(id) }.forEach { msg ->
                            if (msg.attachedHypothesisIds.size <= 1) {
                                scientist.deleteMessageById(msg.id)
                            } else {
                                scientist.updateMessage(
                                    msg.copy(attachedHypothesisIds = msg.attachedHypothesisIds - id),
                                )
                            }
                        }
                        hypotheses.delete(h)
                    }
                }
            }
            pendingDeleteFlow.value = null
        }
    }

    // ====== Proaktive Forscher-Frage ======

    fun refreshScientistQuestion() {
        viewModelScope.launch {
            val previousText = scientistQuestions.currentQuestion.first()?.text
            scientistQuestions.setCurrent(null)
            try {
                val q = generateScientistQuestion(avoidPreviousText = previousText)
                scientistQuestions.setCurrent(q)
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                Diag.e(DiagnosticArea.AGENTIC, "ScientistViewModel", "refreshScientistQuestion failed", t)
            }
        }
    }

    fun snoozeScientistQuestion() {
        viewModelScope.launch { scientistQuestions.snoozeFor24Hours() }
    }

    /**
     * Antwort auf die Forscher-Frage verarbeiten:
     * 1) Frage + Antwort als MANUELL-Memory speichern (isActive=true, confidence=70).
     * 2) Antwort zusaetzlich durch ProcessEntryUseCase laufen lassen — falls Frank
     *    in der Antwort eine konkrete neue Aufgabe nennt, soll die in den Eintraegen
     *    landen (Dedup gegen bestehende Eintraege).
     * 3) Frage entfernen, dann naechste Frage anfordern.
     */
    fun submitScientistQuestionAnswer(answer: String) {
        if (answer.isBlank()) return
        viewModelScope.launch {
            val currentQuestion = scientistQuestions.currentQuestion.first()
            val questionText = currentQuestion?.text.orEmpty()
            scientistQuestions.setCurrent(null)
            try {
                memories.upsert(
                    de.frank.entropyreducer.data.local.entities.MemoryEntryEntity(
                        id = UUID.randomUUID().toString(),
                        content = if (questionText.isNotBlank()) {
                            "Forscher-Frage des Moments — Frage: \"$questionText\" — Antwort von Frank: \"${answer.trim()}\""
                        } else {
                            "Frank's Antwort auf Forscher-Frage: \"${answer.trim()}\""
                        },
                        source = MemorySource.MANUELL,
                        isActive = true,
                        confidence = 70,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                Diag.e(DiagnosticArea.AGENTIC, "ScientistViewModel", "Memory speichern fehlgeschlagen", t)
            }
            refreshScientistQuestion()
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

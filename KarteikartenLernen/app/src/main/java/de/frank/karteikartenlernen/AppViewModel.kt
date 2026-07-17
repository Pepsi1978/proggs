package de.frank.karteikartenlernen

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.karteikartenlernen.audio.ProceduralSoundPlayer
import de.frank.karteikartenlernen.audio.SoundEffect
import de.frank.karteikartenlernen.audio.WavAudioRecorder
import de.frank.karteikartenlernen.auth.CodexAuthManager
import de.frank.karteikartenlernen.auth.AuthErrorKind
import de.frank.karteikartenlernen.auth.CodexAuthException
import de.frank.karteikartenlernen.auth.ExistingSessionContext
import de.frank.karteikartenlernen.auth.GeneratedCard
import de.frank.karteikartenlernen.auth.GeneratedResearch
import de.frank.karteikartenlernen.data.AppDatabase
import de.frank.karteikartenlernen.data.FlashcardEntity
import de.frank.karteikartenlernen.data.ResearchEntity
import de.frank.karteikartenlernen.data.SessionEntity
import de.frank.karteikartenlernen.data.SettingsStore
import de.frank.karteikartenlernen.model.AppSettings
import de.frank.karteikartenlernen.model.AppTab
import de.frank.karteikartenlernen.model.AppUiState
import de.frank.karteikartenlernen.model.CardStatus
import de.frank.karteikartenlernen.model.CrossSuggestion
import de.frank.karteikartenlernen.model.Flashcard
import de.frank.karteikartenlernen.model.GenerationPhase
import de.frank.karteikartenlernen.model.LearningState
import de.frank.karteikartenlernen.model.MicState
import de.frank.karteikartenlernen.model.SAMPLE_ANSWER
import de.frank.karteikartenlernen.model.SAMPLE_IMPROVED
import de.frank.karteikartenlernen.model.SAMPLE_RAW
import de.frank.karteikartenlernen.model.StudySession
import de.frank.karteikartenlernen.model.sampleSessions
import de.frank.karteikartenlernen.model.sampleCards
import de.frank.karteikartenlernen.model.advanceLearningQueue
import de.frank.karteikartenlernen.transcription.GroqTranscriber
import de.frank.karteikartenlernen.text.GeminiTextImprover
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun appendTranscription(existing: String, transcript: String): String {
    val addition = transcript.trim()
    if (addition.isEmpty()) return existing
    if (existing.isEmpty() || existing.last().isWhitespace()) return existing + addition
    return "$existing $addition"
}

internal fun buildCrossSuggestions(
    generatedCards: List<GeneratedCard>,
    persistedCardIds: List<Long>,
    sessions: List<ExistingSessionContext>,
): List<CrossSuggestion> {
    val cardIdsBySession = linkedMapOf<String, MutableList<Long>>()
    generatedCards.zip(persistedCardIds).forEach { (card, cardId) ->
        card.targetSessionIds.forEach { sessionId ->
            cardIdsBySession.getOrPut(sessionId) { mutableListOf() }.add(cardId)
        }
    }
    return sessions.mapNotNull { session ->
        cardIdsBySession[session.id]
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?.let { cardIds -> CrossSuggestion(session.id, session.title, cardIds) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = SettingsStore(application)
    private val dao = AppDatabase.get(application).studyDao()
    private val auth = CodexAuthManager(application)
    private val sounds = ProceduralSoundPlayer()
    private val audioRecorder = WavAudioRecorder(application)
    private val groqTranscriber = GroqTranscriber(BuildConfig.GROQ_API_KEY, BuildConfig.GROQ_TRANSCRIPTION_MODEL)
    private val textImprover = GeminiTextImprover(BuildConfig.GEMINI_API_KEY, BuildConfig.GEMINI_MODEL)
    private val _uiState = MutableStateFlow(
        AppUiState(connectedEmail = auth.email),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var recordingTimerJob: Job? = null
    private var recordingStart: Deferred<Unit>? = null
    private var transcriptionJob: Job? = null
    private var generationJob: Job? = null
    private var improveJob: Job? = null
    private var loginJob: Job? = null
    private val activeSessionId = MutableStateFlow("hrv")

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { value -> _uiState.update { it.copy(settings = value, model = value.model, reasoning = value.reasoning) } }
        }
        viewModelScope.launch {
            if (dao.sessionCount() == 0) {
                sampleSessions.forEach {
                    dao.upsertSession(SessionEntity(it.id, it.title, System.currentTimeMillis() - sampleSessions.indexOf(it) * 86_400_000L, it.count, it.known, it.date, it.accent))
                }
                sampleSessions.forEach { session ->
                    val researchId = dao.insertResearch(ResearchEntity(sessionId = session.id, question = SAMPLE_RAW, answer = SAMPLE_ANSWER, createdAt = System.currentTimeMillis()))
                    dao.insertCards(sampleCards.map {
                        FlashcardEntity(sessionId = session.id, researchId = researchId, question = it.question, answer = it.answer, explanation = it.explanation, status = it.status.name)
                    })
                }
            }
        }
        viewModelScope.launch {
            dao.observeSessions().collect { rows ->
                _uiState.update { state -> state.copy(sessions = rows.map { StudySession(it.id, it.title, it.count, it.known, it.dateLabel, it.accent) }) }
            }
        }
        viewModelScope.launch {
            activeSessionId.flatMapLatest(dao::observeCards).collect { rows ->
                _uiState.update { state -> state.copy(cards = rows.map { Flashcard(it.id, it.question, it.answer, it.explanation, CardStatus.valueOf(it.status)) }) }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        if (_uiState.value.mic != MicState.IDLE) return
        if (tab == AppTab.LEARN) startLearning(_uiState.value.cards) else _uiState.update { it.copy(tab = tab) }
    }

    fun updateInput(value: String) {
        _uiState.update { state ->
            val versions = if (state.versions.firstOrNull().isNullOrEmpty()) listOf(value) else state.versions
            state.copy(input = value, versions = versions)
        }
    }

    fun startRecording() {
        if (_uiState.value.mic != MicState.IDLE || !groqTranscriber.isConfigured) {
            if (!groqTranscriber.isConfigured) {
                _uiState.update { it.copy(message = "Groq Whisper Large V3 Turbo ist in diesem Build nicht konfiguriert.") }
            }
            return
        }
        _uiState.update { it.copy(mic = MicState.RECORDING, recordingSeconds = 0, authError = null) }
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(recordingSeconds = it.recordingSeconds + 1) }
            }
        }
        val start = viewModelScope.async { audioRecorder.start() }
        recordingStart = start
        viewModelScope.launch {
            runCatching { start.await() }.onFailure { error ->
                if (recordingStart === start && _uiState.value.mic == MicState.RECORDING) {
                    recordingTimerJob?.cancel()
                    _uiState.update { it.copy(mic = MicState.IDLE, message = error.message ?: "Das Mikrofon konnte nicht gestartet werden.") }
                }
            }
        }
    }

    fun stopRecording() {
        if (_uiState.value.mic != MicState.RECORDING) return
        recordingTimerJob?.cancel()
        _uiState.update { it.copy(mic = MicState.TRANSCRIBING) }
        val start = recordingStart
        transcriptionJob?.cancel()
        transcriptionJob = viewModelScope.launch {
            var audioFile: java.io.File? = null
            try {
                start?.await() ?: error("Die Aufnahme wurde nicht gestartet.")
                audioFile = audioRecorder.stop()
                val transcript = groqTranscriber.transcribe(audioFile)
                transcriptionFinished(transcript)
                sounds.play(SoundEffect.TRANSITION, _uiState.value.settings)
            } catch (cancelled: CancellationException) {
                audioRecorder.cancel()
                throw cancelled
            } catch (error: Exception) {
                audioRecorder.cancel()
                _uiState.update {
                    it.copy(
                        mic = MicState.IDLE,
                        message = error.message ?: "Transkription fehlgeschlagen. Deine vorhandene Eingabe wurde nicht verändert.",
                    )
                }
            } finally {
                audioFile?.delete()
                if (recordingStart === start) recordingStart = null
            }
        }
    }

    fun transcriptionFinished(text: String?) {
        recordingTimerJob?.cancel()
        val result = text?.takeIf(String::isNotBlank)
        _uiState.update { state ->
            if (result == null) state.copy(mic = MicState.IDLE, message = "Groq hat keinen Text erkannt. Deine vorhandene Eingabe wurde nicht verändert.")
            else {
                val merged = appendTranscription(state.input, result)
                val base = state.versions.take(state.versionIndex + 1)
                val versions = if (base.lastOrNull() == merged) base else base + merged
                state.copy(mic = MicState.IDLE, input = merged, versions = versions, versionIndex = versions.lastIndex, message = null)
            }
        }
    }

    fun recordingPermissionDenied() {
        _uiState.update { it.copy(mic = MicState.IDLE, message = "Mikrofonzugriff wurde nicht erlaubt.") }
    }

    fun improve() {
        val state = _uiState.value
        if (state.improving || state.input.isBlank() || state.mic != MicState.IDLE || state.generationPhase != null) return
        val source = state.input
        _uiState.update { it.copy(improving = true, message = null) }
        improveJob?.cancel()
        improveJob = viewModelScope.launch {
            try {
                val improved = textImprover.improve(source)
                _uiState.update { current ->
                    if (current.input != source) current.copy(improving = false)
                    else {
                        val base = current.versions.take(current.versionIndex + 1)
                        val versions = if (base.lastOrNull() == improved) base else base + improved
                        current.copy(input = improved, versions = versions, versionIndex = versions.lastIndex, improving = false)
                    }
                }
                sounds.play(SoundEffect.FLIP, _uiState.value.settings)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(improving = false, message = error.message ?: "Gemini konnte den Text nicht verbessern. Das Original blieb unverändert.") }
            }
        }
    }

    fun undo() {
        _uiState.update {
            if (it.versionIndex <= 0) it else it.copy(
                versionIndex = it.versionIndex - 1,
                input = it.versions[it.versionIndex - 1],
            )
        }
    }

    fun send() {
        val state = _uiState.value
        if (state.input.isBlank() || state.generationPhase != null || state.mic != MicState.IDLE || state.improving) return
        if (!auth.isConnected) {
            _uiState.update { it.copy(showOAuth = true) }
            return
        }
        _uiState.update { it.copy(answer = "", generationPhase = GenerationPhase.ANSWER, authError = null) }
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val sessionContexts = dao.sessionContexts().map { row ->
                ExistingSessionContext(row.id, row.title, row.question, row.answerExcerpt)
            }
            val result = runCatching {
                auth.generateResearch(modelId(state.model), state.reasoning, state.input, sessionContexts)
            }.getOrElse { error ->
                if (error is CodexAuthException && error.kind == AuthErrorKind.REAUTH) {
                    auth.logout()
                    _uiState.update { it.copy(answer = null, generationPhase = null, connectedEmail = null, showOAuth = true, authError = error.message) }
                } else {
                    _uiState.update { it.copy(answer = null, generationPhase = null, authError = error.message ?: "OpenAI-Anfrage fehlgeschlagen") }
                }
                return@launch
            }
            _uiState.update { it.copy(answer = result.answer, generationPhase = GenerationPhase.CARDS) }
            val (sessionId, persistedCardIds) = persistResearch(state.input, result)
            val crossSuggestions = buildCrossSuggestions(result.cards, persistedCardIds, sessionContexts)
            activeSessionId.value = sessionId
            delay(1700)
            _uiState.update { current ->
                current.copy(
                    generationPhase = GenerationPhase.DONE,
                    savedSessionTitle = result.title,
                    savedCardCount = result.cards.size,
                    crossSuggestions = crossSuggestions,
                )
            }
            delay(700)
            if (crossSuggestions.isNotEmpty()) _uiState.update { it.copy(showCrossSheet = true) }
        }
    }

    private suspend fun persistResearch(question: String, result: GeneratedResearch): Pair<String, List<Long>> {
        val sessionId = "research-${System.currentTimeMillis()}"
        val createdAt = System.currentTimeMillis()
        val cardIds = dao.insertResearchBundle(
            session = SessionEntity(sessionId, result.title, createdAt, result.cards.size, 0, "Heute", 0),
            research = ResearchEntity(sessionId = sessionId, question = question, answer = result.answer, createdAt = createdAt),
            cards = result.cards.map {
            FlashcardEntity(
                sessionId = sessionId,
                researchId = 0,
                question = it.question,
                answer = it.answer,
                explanation = it.explanation,
                status = CardStatus.NEW.name,
            )
        },
        )
        return sessionId to cardIds
    }

    fun resetResearch() {
        generationJob?.cancel()
        generationJob = null
        _uiState.update {
            it.copy(input = "", versions = listOf(""), versionIndex = 0, answer = null, generationPhase = null, showCrossSheet = false)
        }
    }

    fun showModelSheet(show: Boolean) = _uiState.update { it.copy(showModelSheet = show) }
    fun showOAuth(show: Boolean) {
        if (!show) {
            auth.cancelLogin()
            loginJob?.cancel()
        }
        _uiState.update {
            it.copy(
                showOAuth = show,
                authBusy = if (show) it.authBusy else false,
                authError = null,
                authUserCode = if (show) it.authUserCode else null,
                authVerificationUri = if (show) it.authVerificationUri else null,
            )
        }
    }

    fun chooseModel(model: String) = updateSettings { it.copy(model = model) }
    fun chooseReasoning(reasoning: String) = updateSettings { it.copy(reasoning = reasoning) }

    fun login(activity: ComponentActivity) {
        _uiState.update { it.copy(authBusy = true, authError = null, authUserCode = null, authVerificationUri = null) }
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            runCatching {
                auth.login(activity) { info ->
                    _uiState.update {
                        it.copy(authUserCode = info.userCode, authVerificationUri = info.verificationUri)
                    }
                }
            }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            authBusy = false,
                            showOAuth = false,
                            connectedEmail = result.email ?: "ChatGPT-Konto",
                            authUserCode = null,
                            authVerificationUri = null,
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(
                            authBusy = false,
                            authError = error.message ?: "Anmeldung fehlgeschlagen",
                            authUserCode = null,
                            authVerificationUri = null,
                        )
                    }
                }
        }
    }

    fun logout() {
        auth.logout()
        _uiState.update { it.copy(connectedEmail = null) }
    }

    fun updateSearch(value: String) = _uiState.update { it.copy(search = value) }
    fun openSession(session: StudySession) {
        activeSessionId.value = session.id
        _uiState.update { it.copy(selectedSession = session, detailResearchTab = false, detailQuestion = "", detailAnswer = "") }
        viewModelScope.launch {
            val research = dao.latestResearch(session.id)
            _uiState.update { current ->
                if (current.selectedSession?.id != session.id) current
                else current.copy(
                    detailQuestion = research?.question.orEmpty(),
                    detailAnswer = research?.answer.orEmpty(),
                )
            }
        }
    }
    fun closeSession() = _uiState.update { it.copy(selectedSession = null) }
    fun setDetailResearch(value: Boolean) = _uiState.update { it.copy(detailResearchTab = value) }

    fun deleteCard(id: Long) {
        _uiState.update { it.copy(cards = it.cards.filterNot { card -> card.id == id }) }
        viewModelScope.launch { dao.deleteCard(id) }
    }

    fun resetLearningStatus() {
        _uiState.update { state -> state.copy(cards = state.cards.map { it.copy(status = CardStatus.NEW) }) }
        val sessionId = _uiState.value.selectedSession?.id ?: activeSessionId.value
        viewModelScope.launch { dao.resetSession(sessionId); dao.clearKnown(sessionId) }
    }

    fun startLearning(deck: List<Flashcard>) {
        if (deck.isEmpty()) return
        sounds.play(SoundEffect.TRANSITION, _uiState.value.settings)
        val session = _uiState.value.selectedSession ?: _uiState.value.sessions.firstOrNull { it.id == activeSessionId.value }
        _uiState.update { it.copy(learning = LearningState(sessionId = activeSessionId.value, title = session?.title ?: "Karteikarten", deck = deck, queue = deck.indices.toList())) }
        viewModelScope.launch {
            delay(420)
            _uiState.update { it.copy(learning = it.learning?.copy(entering = false)) }
        }
    }

    fun closeLearning() = _uiState.update { it.copy(learning = null) }

    fun flipCard() {
        val learning = _uiState.value.learning ?: return
        if (learning.flipped) return
        sounds.play(SoundEffect.FLIP, _uiState.value.settings)
        _uiState.update { it.copy(learning = learning.copy(flipped = true)) }
    }

    fun rateCard(known: Boolean) {
        val learning = _uiState.value.learning ?: return
        if (!learning.flipped || learning.rating != null) return
        sounds.play(if (known) SoundEffect.KNOWN else SoundEffect.UNKNOWN, _uiState.value.settings)
        val card = learning.deck[learning.queue[learning.position]]
        viewModelScope.launch {
            dao.updateCardStatus(card.id, if (known) CardStatus.KNOWN.name else CardStatus.UNKNOWN.name)
            dao.insertLearningResult(de.frank.karteikartenlernen.data.LearningResultEntity(flashcardId = card.id, known = known, reviewedAt = System.currentTimeMillis()))
            if (known && card.status != CardStatus.KNOWN) dao.incrementKnown(learning.sessionId)
        }
        _uiState.update { it.copy(learning = learning.copy(rating = known)) }
        viewModelScope.launch {
            delay(if (known) 520 else 560)
            _uiState.update { state ->
                val current = state.learning ?: return@update state
                val advance = advanceLearningQueue(current.queue, current.position, known)
                if (advance.done) {
                    sounds.play(SoundEffect.DONE, state.settings)
                    state.copy(learning = current.copy(
                        queue = advance.queue,
                        position = advance.nextPosition,
                        known = current.known + if (known) 1 else 0,
                        repeated = current.repeated + if (known) 0 else 1,
                        done = true,
                        rating = null,
                    ))
                } else {
                    state.copy(learning = current.copy(
                        queue = advance.queue,
                        position = advance.nextPosition,
                        known = current.known + if (known) 1 else 0,
                        repeated = current.repeated + if (known) 0 else 1,
                        flipped = false,
                        entering = true,
                        rating = null,
                    ))
                }
            }
            delay(380)
            _uiState.update { it.copy(learning = it.learning?.copy(entering = false)) }
        }
    }

    fun restartLearning() {
        _uiState.value.learning?.deck?.let(::startLearning)
    }

    fun decideCross(index: Int, accepted: Boolean) {
        val suggestion = _uiState.value.crossSuggestions.getOrNull(index) ?: return
        if (suggestion.accepted != null) return
        _uiState.update { state ->
            state.copy(crossSuggestions = state.crossSuggestions.mapIndexed { i, item ->
                if (i == index) item.copy(accepted = accepted) else item
            })
        }
        if (accepted) viewModelScope.launch {
            try {
                val copied = dao.copyCardsToSession(suggestion.cardIds, suggestion.sessionId)
                if (copied == 0) {
                    _uiState.update { state ->
                        state.copy(
                            crossSuggestions = state.crossSuggestions.mapIndexed { i, item ->
                                if (i == index) item.copy(accepted = null) else item
                            },
                            message = "Die passenden Karten wurden nicht mehr gefunden. Bitte erzeuge die Recherche erneut.",
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { state ->
                    state.copy(
                        crossSuggestions = state.crossSuggestions.mapIndexed { i, item ->
                            if (i == index) item.copy(accepted = null) else item
                        },
                        message = error.message ?: "Die Karten konnten nicht zur Session hinzugefügt werden.",
                    )
                }
            }
        }
    }

    fun closeCross() = _uiState.update { it.copy(showCrossSheet = false) }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val value = transform(_uiState.value.settings)
        _uiState.update { it.copy(settings = value) }
        viewModelScope.launch { settingsStore.save(value) }
    }

    fun testSound(effect: SoundEffect) = sounds.play(effect, _uiState.value.settings)

    override fun onCleared() {
        recordingTimerJob?.cancel()
        transcriptionJob?.cancel()
        audioRecorder.shutdown()
        groqTranscriber.shutdown()
        improveJob?.cancel()
        textImprover.shutdown()
        super.onCleared()
    }

    private fun modelId(label: String): String = when (label) {
        "GPT 5.6 Soul" -> "gpt-5.6-sol"
        "GPT 5.6 Luna" -> "gpt-5.6-luna"
        else -> "gpt-5.6-terra"
    }
}

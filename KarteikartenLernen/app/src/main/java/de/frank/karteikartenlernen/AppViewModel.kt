package de.frank.karteikartenlernen

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.karteikartenlernen.audio.ProceduralSoundPlayer
import de.frank.karteikartenlernen.audio.SoundEffect
import de.frank.karteikartenlernen.auth.CodexAuthManager
import de.frank.karteikartenlernen.auth.AuthErrorKind
import de.frank.karteikartenlernen.auth.CodexAuthException
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = SettingsStore(application)
    private val dao = AppDatabase.get(application).studyDao()
    private val auth = CodexAuthManager(application)
    private val sounds = ProceduralSoundPlayer()
    private val _uiState = MutableStateFlow(
        AppUiState(connectedEmail = auth.email),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var recordingJob: Job? = null
    private var generationJob: Job? = null
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
        if (tab == AppTab.LEARN) startLearning(_uiState.value.cards) else _uiState.update { it.copy(tab = tab) }
    }

    fun updateInput(value: String) {
        _uiState.update { state ->
            val versions = if (state.versions.firstOrNull().isNullOrEmpty()) listOf(value) else state.versions
            state.copy(input = value, versions = versions)
        }
    }

    fun startRecording() {
        _uiState.update { it.copy(mic = MicState.RECORDING, recordingSeconds = 0, authError = null) }
        recordingJob?.cancel()
        recordingJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(recordingSeconds = it.recordingSeconds + 1) }
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        sounds.play(SoundEffect.TRANSITION, _uiState.value.settings)
        _uiState.update { it.copy(mic = MicState.TRANSCRIBING) }
    }

    fun transcriptionFinished(text: String?) {
        recordingJob?.cancel()
        val result = text?.takeIf(String::isNotBlank)
        _uiState.update {
            if (result == null) it.copy(mic = MicState.IDLE, message = "Die Spracherkennung hat keinen Text geliefert. Bitte erneut versuchen oder tippen.")
            else it.copy(mic = MicState.IDLE, input = result, versions = listOf(result), versionIndex = 0, message = null)
        }
    }

    fun improve() {
        val state = _uiState.value
        if (state.improving || state.input.isBlank()) return
        _uiState.update { it.copy(improving = true) }
        viewModelScope.launch {
            delay(1100)
            _uiState.update {
                val clean = it.input.trim().replaceFirstChar { first -> first.uppercase() }.let { text -> if (text.endsWith('?') || text.endsWith('.') || text.endsWith('!')) text else "$text?" }
                val versions = if (it.versions.lastOrNull() == clean) it.versions else it.versions + clean
                it.copy(input = clean, versions = versions, versionIndex = versions.lastIndex, improving = false)
            }
            sounds.play(SoundEffect.FLIP, _uiState.value.settings)
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
        if (state.input.isBlank() || state.generationPhase != null) return
        if (!auth.isConnected) {
            _uiState.update { it.copy(showOAuth = true) }
            return
        }
        _uiState.update { it.copy(answer = "", generationPhase = GenerationPhase.ANSWER, authError = null) }
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val result = runCatching {
                auth.generateResearch(modelId(state.model), state.reasoning, state.input, state.settings.cardsPerResearch)
            }.getOrElse { error ->
                if (error is CodexAuthException && error.kind == AuthErrorKind.REAUTH) {
                    auth.logout()
                    _uiState.update { it.copy(answer = null, generationPhase = null, connectedEmail = null, showOAuth = true, authError = error.message) }
                } else {
                    _uiState.update { it.copy(answer = null, generationPhase = null, authError = error.message ?: "OpenAI-Anfrage fehlgeschlagen") }
                }
                return@launch
            }
            val words = result.answer.split(' ')
            for (end in 2..words.size step 2) {
                _uiState.update { it.copy(answer = words.take(end).joinToString(" ")) }
                delay(38)
            }
            _uiState.update { it.copy(answer = result.answer, generationPhase = GenerationPhase.CARDS) }
            val sessionId = persistResearch(state.input, result)
            activeSessionId.value = sessionId
            delay(1700)
            _uiState.update { current ->
                current.copy(
                    generationPhase = GenerationPhase.DONE,
                    savedSessionTitle = result.title,
                    savedCardCount = result.cards.size,
                    crossSuggestions = current.sessions.filterNot { it.id == sessionId }.take(2).map { CrossSuggestion(it.id, it.title, result.cards.size.coerceAtMost(6)) },
                )
            }
            delay(700)
            _uiState.update { it.copy(showCrossSheet = true) }
        }
    }

    private suspend fun persistResearch(question: String, result: GeneratedResearch): String {
        val sessionId = "research-${System.currentTimeMillis()}"
        dao.upsertSession(SessionEntity(sessionId, result.title, System.currentTimeMillis(), result.cards.size, 0, "Heute", 0))
        val researchId = dao.insertResearch(ResearchEntity(sessionId = sessionId, question = question, answer = result.answer, createdAt = System.currentTimeMillis()))
        dao.insertCards(result.cards.map {
            FlashcardEntity(
                sessionId = sessionId,
                researchId = researchId,
                question = it.question,
                answer = it.answer,
                explanation = it.explanation,
                status = CardStatus.NEW.name,
            )
        })
        return sessionId
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
        _uiState.update { it.copy(showOAuth = show, authBusy = if (show) it.authBusy else false, authError = null) }
    }

    fun chooseModel(model: String) = updateSettings { it.copy(model = model) }
    fun chooseReasoning(reasoning: String) = updateSettings { it.copy(reasoning = reasoning) }

    fun login(activity: Activity) {
        _uiState.update { it.copy(authBusy = true, authError = null) }
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            runCatching { auth.login(activity) }
                .onSuccess { result ->
                    _uiState.update { it.copy(authBusy = false, showOAuth = false, connectedEmail = result.email ?: "ChatGPT-Konto") }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update { it.copy(authBusy = false, authError = error.message ?: "Anmeldung fehlgeschlagen") }
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
        _uiState.update { it.copy(selectedSession = session, detailResearchTab = false) }
        viewModelScope.launch {
            dao.latestResearch(session.id)?.let { research ->
                _uiState.update { it.copy(detailQuestion = research.question, detailAnswer = research.answer) }
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
        if (accepted) viewModelScope.launch {
            val cards = _uiState.value.cards.take(suggestion.count).map {
                FlashcardEntity(sessionId = suggestion.sessionId, researchId = 0, question = it.question, answer = it.answer, explanation = it.explanation, status = CardStatus.NEW.name)
            }
            dao.insertCards(cards)
            dao.addCardCount(suggestion.sessionId, cards.size)
        }
        _uiState.update { state ->
            state.copy(crossSuggestions = state.crossSuggestions.mapIndexed { i, item ->
                if (i == index) item.copy(accepted = accepted) else item
            })
        }
    }

    fun closeCross() = _uiState.update { it.copy(showCrossSheet = false) }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val value = transform(_uiState.value.settings)
        _uiState.update { it.copy(settings = value) }
        viewModelScope.launch { settingsStore.save(value) }
    }

    fun testSound(effect: SoundEffect) = sounds.play(effect, _uiState.value.settings)

    private fun modelId(label: String): String = when (label) {
        "GPT 5.6 Soul" -> "gpt-5.6-soul"
        "GPT 5.6 Luna" -> "gpt-5.6-luna"
        else -> "gpt-5.6-terra"
    }
}

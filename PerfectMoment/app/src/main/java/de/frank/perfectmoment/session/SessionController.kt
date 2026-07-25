package de.frank.perfectmoment.session

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import de.frank.perfectmoment.auth.CodexAuthManager
import de.frank.perfectmoment.auth.CodexModel
import de.frank.perfectmoment.auth.CodexQuestionRequest
import de.frank.perfectmoment.auth.IncrementalQuestionValidator
import de.frank.perfectmoment.auth.QuestionResponseValidator
import de.frank.perfectmoment.auth.QuestionPerspective
import de.frank.perfectmoment.auth.ReasoningEffort
import de.frank.perfectmoment.data.local.SessionEntity
import de.frank.perfectmoment.data.repository.ContentRepository
import de.frank.perfectmoment.data.repository.SessionRepository
import de.frank.perfectmoment.data.settings.SecureSettings
import de.frank.perfectmoment.tts.TtsCatalog
import de.frank.perfectmoment.tts.TtsManager
import de.frank.perfectmoment.tts.TtsProvider
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class SessionRuntime(
    val topic: String,
    val sessionId: Long? = null,
    val config: SessionConfig,
    val replay: Boolean = false,
    val generating: Boolean = false,
)

class SessionController(
    context: Context,
    private val settings: SecureSettings,
    private val contentRepository: ContentRepository,
    private val sessionRepository: SessionRepository,
    private val codexAuthManager: CodexAuthManager,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ttsManager = TtsManager(appContext)
    private val operationMutex = Mutex()
    private val engineGeneration = AtomicLong(0)
    private var engine: SessionEngine? = null
    private var stateJob: Job? = null

    private val _runtime = MutableStateFlow<SessionRuntime?>(null)
    val runtime: StateFlow<SessionRuntime?> = _runtime.asStateFlow()

    private val _state = MutableStateFlow<SessionState?>(null)
    val state: StateFlow<SessionState?> = _state.asStateFlow()

    suspend fun startNewSession(
        topic: String,
        introContext: String,
        entranceQuestion: String = "",
    ) = operationMutex.withLock {
        releaseEngine()
        val config = currentConfig()
        _state.value = null
        val sessionId = sessionRepository.createSession(
            newSessionEntity(topic, config, introContext, entranceQuestion),
        )
        val runtime = SessionRuntime(
            topic = topic,
            sessionId = sessionId,
            config = config,
            generating = true,
        )
        _runtime.value = runtime

        try {
            startForegroundSessionService()
            val requestBase = newQuestionRequest(topic, introContext, entranceQuestion)
            val refillPort = newRefillPort(requestBase, entranceQuestion)
            val persistencePort = newPersistencePort(sessionId)
            val streamedValidator = IncrementalQuestionValidator(excludedQuestions = listOf(entranceQuestion))
            val streamedQuestions = mutableListOf<Question>()
            val rawInitial = codexAuthManager.generateQuestions(requestBase) { rawQuestion ->
                streamedValidator.accept(rawQuestion)?.let { parsed ->
                    val question = Question(emoji = parsed.emoji, text = parsed.text)
                    sessionRepository.appendQuestions(sessionId, listOf(question))
                    withContext(Dispatchers.Main.immediate) {
                        if (_runtime.value?.sessionId != sessionId) return@withContext
                        streamedQuestions += question
                        val activeEngine = engine
                        if (activeEngine == null) {
                            createEngine(
                                runtime = runtime,
                                questions = listOf(question),
                                refillPort = refillPort,
                                persistencePort = persistencePort,
                                initialGenerationInFlight = true,
                            )
                        } else {
                            activeEngine.appendQuestions(listOf(question))
                        }
                    }
                }
            }
            val validatedInitial = QuestionResponseValidator.validate(
                rawInitial,
                excludedQuestions = listOf(entranceQuestion),
            )
                .map { Question(emoji = it.emoji, text = it.text) }
            val streamedTexts = streamedQuestions
                .mapTo(mutableSetOf()) { QuestionResponseValidator.normalizeQuestion(it.text) }
            val missingQuestions = validatedInitial.filter {
                QuestionResponseValidator.normalizeQuestion(it.text) !in streamedTexts
            }
            if (missingQuestions.isNotEmpty()) {
                sessionRepository.appendQuestions(sessionId, missingQuestions)
                withContext(Dispatchers.Main.immediate) {
                    if (_runtime.value?.sessionId != sessionId) return@withContext
                    val activeEngine = engine
                    if (activeEngine == null) {
                        createEngine(
                            runtime = runtime.copy(generating = false),
                            questions = missingQuestions,
                            refillPort = refillPort,
                            persistencePort = persistencePort,
                        )
                    } else {
                        activeEngine.appendQuestions(missingQuestions)
                    }
                }
            }
            withContext(Dispatchers.Main.immediate) {
                if (_runtime.value?.sessionId != sessionId) return@withContext
                engine?.completeInitialGeneration()
                _runtime.value = _runtime.value?.copy(generating = false)
            }
        } catch (error: Throwable) {
            if (error !is kotlinx.coroutines.CancellationException &&
                _runtime.value?.sessionId == sessionId &&
                _state.value?.questions?.isNotEmpty() == true
            ) {
                engine?.completeInitialGeneration()
                _runtime.value = _runtime.value?.copy(generating = false)
                return@withLock
            }
            withContext(NonCancellable) {
                releaseEngine()
                appContext.stopService(Intent(appContext, SessionForegroundService::class.java))
                try {
                    sessionRepository.deleteSession(sessionId)
                } catch (cleanupError: Throwable) {
                    error.addSuppressed(cleanupError)
                }
            }
            throw error
        }
    }

    suspend fun replaySession(sourceSessionId: Long, shuffle: Boolean) = operationMutex.withLock {
        releaseEngine()
        val source = requireNotNull(sessionRepository.getSession(sourceSessionId)) {
            "Die gespeicherte Sitzung wurde nicht gefunden."
        }
        val config = currentConfig()
        val perspectiveQuestions = codexAuthManager.rewriteQuestionsForPerspective(
            questions = source.questions.map { it.text },
            perspective = QuestionPerspective.fromId(settings.questionPerspective),
            model = CodexModel.fromLabel(settings.model),
            reasoningEffort = ReasoningEffort.fromLabel(settings.reasoning),
        )
        val questions = source.questions.zip(perspectiveQuestions) { entity, text ->
            Question(id = entity.id, emoji = entity.emoji, text = text)
        }.let { if (shuffle) it.shuffled() else it }
        createEngine(
            runtime = SessionRuntime(source.session.topic, sourceSessionId, config, replay = true),
            questions = questions,
            refillPort = null,
            persistencePort = QuestionPersistencePort { },
        )
        startForegroundSessionService()
    }

    suspend fun resumeSession(sourceSessionId: Long) = operationMutex.withLock {
        releaseEngine()
        val source = requireNotNull(sessionRepository.getSession(sourceSessionId)) {
            "Die gespeicherte Sitzung wurde nicht gefunden."
        }
        val questionIndex = requireNotNull(source.session.resumeQuestionIndex) {
            "Für diese Sitzung ist kein Fortsetzungspunkt gespeichert."
        }
        val config = SessionConfig.fromSeconds(
            pauseRepSeconds = source.session.pauseRep,
            pauseNextSeconds = source.session.pauseNext,
            repsPerQuestion = source.session.reps,
            durationMinutes = source.session.durationMin,
        )
        val entranceQuestion = source.session.entranceQuestion
        val requestBase = newQuestionRequest(
            topic = source.session.topic,
            introContext = source.session.introContext,
            entranceQuestion = entranceQuestion,
        )
        createEngine(
            runtime = SessionRuntime(source.session.topic, sourceSessionId, config),
            questions = source.questions.map { Question(it.id, it.emoji, it.text) },
            refillPort = newRefillPort(requestBase, entranceQuestion),
            persistencePort = newPersistencePort(sourceSessionId),
            checkpoint = SessionCheckpoint(
                currentIndex = questionIndex,
                currentRep = source.session.resumeRepetition ?: 1,
                remainingMs = source.session.resumeRemainingMs ?: config.durationMs,
            ),
        )
        startForegroundSessionService()
    }

    fun toggleSpeaker() {
        val intent = Intent(appContext, SessionForegroundService::class.java)
            .setAction(SessionForegroundService.ACTION_TOGGLE)
        ContextCompat.startForegroundService(appContext, intent)
    }

    fun togglePause() {
        engine?.togglePause()
    }

    internal fun setSpeakerOn(enabled: Boolean) {
        engine?.setSpeakerOn(enabled)
    }

    fun stopAndClear() {
        val sessionId = _runtime.value?.sessionId
        engine?.stopSession()
        releaseEngine()
        appContext.stopService(Intent(appContext, SessionForegroundService::class.java))
        if (sessionId != null) scope.launch { sessionRepository.clearProgress(sessionId) }
    }

    suspend fun saveAndClear() = operationMutex.withLock {
        val runtime = requireNotNull(_runtime.value)
        val sessionId = requireNotNull(runtime.sessionId)
        val state = requireNotNull(_state.value)
        sessionRepository.saveProgress(sessionId, state, runtime.config)
        releaseEngine()
        appContext.stopService(Intent(appContext, SessionForegroundService::class.java))
    }

    fun dismissEnded() {
        if (_state.value?.phase == Phase.ENDED) {
            releaseEngine()
            appContext.stopService(Intent(appContext, SessionForegroundService::class.java))
        }
    }

    private fun createEngine(
        runtime: SessionRuntime,
        questions: List<Question>,
        refillPort: QuestionRefillPort?,
        persistencePort: QuestionPersistencePort,
        initialGenerationInFlight: Boolean = false,
        checkpoint: SessionCheckpoint? = null,
    ) {
        val generation = engineGeneration.incrementAndGet()
        val created = SessionEngine(
            initialQuestions = questions,
            config = runtime.config,
            ttsPort = TtsAdapter(ttsManager),
            refillPort = refillPort,
            persistencePort = persistencePort,
            coroutineScope = scope,
            dispatcher = Dispatchers.Main.immediate,
            replay = runtime.replay,
            initialGenerationInFlight = initialGenerationInFlight,
            checkpoint = checkpoint,
        )
        engine = created
        _runtime.value = runtime.copy(generating = initialGenerationInFlight)
        _state.value = created.state.value
        stateJob = scope.launch {
            created.state.collect { next ->
                if (generation != engineGeneration.get()) return@collect
                _state.value = next
                if (next.phase == Phase.ENDED) {
                    appContext.stopService(Intent(appContext, SessionForegroundService::class.java))
                    runtime.sessionId?.let { sessionId ->
                        scope.launch { sessionRepository.clearProgress(sessionId) }
                    }
                }
            }
        }
        created.start()
    }

    private fun startForegroundSessionService() {
        ContextCompat.startForegroundService(
            appContext,
            Intent(appContext, SessionForegroundService::class.java)
                .setAction(SessionForegroundService.ACTION_START),
        )
    }

    private suspend fun newQuestionRequest(
        topic: String,
        introContext: String,
        entranceQuestion: String,
    ): CodexQuestionRequest {
        val skill = contentRepository.getSkill(settings.activeSkillId)
            ?: throw IllegalStateException("Der aktive Skill wurde nicht gefunden.")
        return CodexQuestionRequest(
            topic = topic,
            introContext = introContext,
            entranceQuestion = entranceQuestion,
            skillText = skill.text,
            operatingModeText = settings.operatingModeText,
            perspective = QuestionPerspective.fromId(settings.questionPerspective),
            model = CodexModel.fromLabel(settings.model),
            reasoningEffort = ReasoningEffort.fromLabel(settings.reasoning),
        )
    }

    private fun newRefillPort(
        requestBase: CodexQuestionRequest,
        entranceQuestion: String,
    ) = QuestionRefillPort { existing ->
        val raw = codexAuthManager.generateQuestions(
            requestBase.copy(previousQuestions = existing.map(Question::text)),
        )
        QuestionResponseValidator.validate(
            raw,
            existing.map(Question::text),
            listOf(entranceQuestion),
        )
            .map { "${it.emoji} ${it.text}" }
    }

    private fun newPersistencePort(sessionId: Long) = QuestionPersistencePort { questions ->
        sessionRepository.appendQuestions(sessionId, questions)
    }

    private fun newSessionEntity(
        topic: String,
        config: SessionConfig,
        introContext: String,
        entranceQuestion: String,
    ): SessionEntity {
        val provider = TtsProvider.entries.firstOrNull { it.id == settings.ttsProvider }
            ?: TtsCatalog.DEFAULT_PROVIDER
        val voice = if (provider == TtsProvider.EDGE) {
            settings.edgeTtsVoice
        } else {
            settings.googleTtsVoice
        }
        return SessionEntity(
            topic = topic,
            startedAt = System.currentTimeMillis(),
            durationMin = (config.durationMs / 60_000L).toInt(),
            voiceName = voice,
            providerId = provider.id,
            pauseRep = (config.pauseRepMs / 1_000L).toInt(),
            pauseNext = (config.pauseNextMs / 1_000L).toInt(),
            reps = config.repsPerQuestion,
            introContext = introContext,
            entranceQuestion = entranceQuestion,
        )
    }

    private fun currentConfig() = SessionConfig.fromSeconds(
        pauseRepSeconds = settings.pauseRepSeconds,
        pauseNextSeconds = settings.pauseNextSeconds,
        repsPerQuestion = settings.repsPerQuestion,
        durationMinutes = settings.sessionDurationMin,
    )

    private fun releaseEngine() {
        engineGeneration.incrementAndGet()
        stateJob?.cancel()
        stateJob = null
        engine?.close()
        engine = null
        _state.value = null
        _runtime.value = null
    }

    private class TtsAdapter(private val manager: TtsManager) : SessionTtsPort {
        override fun speak(text: String, listener: SessionTtsPort.Listener) {
            manager.speak(
                text = text,
                onStart = listener::onStart,
                onComplete = listener::onComplete,
                onError = listener::onError,
            )
        }

        override fun stop() = manager.stop()
        override fun pause(): Boolean = manager.pause()
        override fun resume(): Boolean = manager.resume()
    }
}

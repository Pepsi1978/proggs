package de.frank.perfectmoment.session

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
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
import kotlinx.coroutines.CancellationException
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

/** Wie viele bereits gestellte Fragen der Anfrage als Wiederholungsschutz beigelegt werden. */
private const val RECENT_QUESTION_CONTEXT = 60

private const val TAG = "PmSession"

data class SessionRuntime(
    val topic: String,
    val sessionId: Long? = null,
    val config: SessionConfig,
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
                streamedValidator.accept(rawQuestion).forEach { parsed ->
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

    /** Spielt den vollständigen Bestand erneut ab und lädt 30 Fragen vor dessen Ende nach. */
    suspend fun replaySession(sourceSessionId: Long, shuffle: Boolean) = operationMutex.withLock {
        releaseEngine()
        val source = requireNotNull(sessionRepository.getSession(sourceSessionId)) {
            "Die gespeicherte Sitzung wurde nicht gefunden."
        }
        val config = currentConfig().copy(durationMs = 0L)
        val questions = source.questions
            .map { Question(id = it.id, emoji = it.emoji, text = it.text) }
            .let { if (shuffle) it.shuffled() else it }
        val entranceQuestion = source.session.entranceQuestion
        val requestBase = newQuestionRequest(
            topic = source.session.topic,
            introContext = source.session.introContext,
            entranceQuestion = entranceQuestion,
        )
        sessionRepository.markPlayed(sourceSessionId)
        createEngine(
            runtime = SessionRuntime(source.session.topic, sourceSessionId, config),
            questions = questions,
            refillPort = newRefillPort(requestBase, entranceQuestion),
            persistencePort = newPersistencePort(sourceSessionId),
            voice = SessionVoice.of(source.session),
            refillWhenStockIsLow = true,
        )
        startForegroundSessionService()
    }

    /**
     * Setzt eine Sitzung genau am gespeicherten Punkt fort. Bereits vorgelesene Fragen bleiben
     * hinter dem Fortsetzungspunkt liegen und kommen nie ein zweites Mal.
     *
     * @param shuffle mischt ausschliesslich die noch offenen Fragen. Jede davon kommt genau
     *        einmal an die Reihe, danach laeuft die Sitzung mit Nachschub weiter.
     */
    suspend fun resumeSession(
        sourceSessionId: Long,
        useChangedSettings: Boolean,
        shuffle: Boolean,
    ) = operationMutex.withLock {
        releaseEngine()
        val source = requireNotNull(sessionRepository.getSession(sourceSessionId)) {
            "Die gespeicherte Sitzung wurde nicht gefunden."
        }
        val questionIndex = requireNotNull(source.session.resumeQuestionIndex) {
            "Für diese Sitzung ist kein Fortsetzungspunkt gespeichert."
        }
        val config = if (useChangedSettings) {
            currentConfig()
        } else {
            SessionConfig.fromSeconds(
                pauseRepSeconds = source.session.pauseRep,
                pauseNextSeconds = source.session.pauseNext,
                repsPerQuestion = source.session.reps,
                durationMinutes = source.session.durationMin,
            )
        }
        val entranceQuestion = source.session.entranceQuestion
        val requestBase = newQuestionRequest(
            topic = source.session.topic,
            introContext = source.session.introContext,
            entranceQuestion = entranceQuestion,
        )
        val stored = source.questions.map { Question(it.id, it.emoji, it.text) }
        sessionRepository.markPlayed(sourceSessionId)
        createEngine(
            runtime = SessionRuntime(source.session.topic, sourceSessionId, config),
            questions = if (shuffle) shuffleUpcoming(stored, questionIndex) else stored,
            refillPort = newRefillPort(requestBase, entranceQuestion),
            persistencePort = newPersistencePort(sourceSessionId),
            checkpoint = SessionCheckpoint(
                currentIndex = questionIndex,
                // Beim Mischen steht am Fortsetzungspunkt eine andere Frage — die faengt bei
                // ihrer ersten Wiederholung an, nicht mitten in der der abgebrochenen Frage.
                currentRep = if (shuffle) 1 else source.session.resumeRepetition ?: 1,
                remainingMs = if (useChangedSettings) {
                    config.initialRemainingMs
                } else {
                    source.session.resumeRemainingMs ?: config.initialRemainingMs
                },
            ),
            voice = SessionVoice.of(source.session),
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
        voice: SessionVoice? = null,
        refillWhenStockIsLow: Boolean = false,
    ) {
        val generation = engineGeneration.incrementAndGet()
        val created = SessionEngine(
            initialQuestions = questions,
            config = runtime.config,
            ttsPort = TtsAdapter(ttsManager, voice),
            refillPort = refillPort,
            persistencePort = persistencePort,
            coroutineScope = scope,
            dispatcher = Dispatchers.Main.immediate,
            initialGenerationInFlight = initialGenerationInFlight,
            checkpoint = checkpoint,
            refillWhenStockIsLow = refillWhenStockIsLow,
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

    /**
     * Lädt den nächsten Fragenblock nach und reicht jede Frage einzeln weiter, sobald sie fertig
     * ist — genau wie beim Sitzungsstart. Ohne diesen Strom müsste die Sitzung auf den ganzen
     * Block warten, was bei hoher Denkstufe Minuten dauert.
     *
     * Ein unvollständiger Block wird nicht verworfen: Was bereits angekommen ist, bleibt gültig.
     * Sonst würde eine einzige fehlende oder doppelte Frage den gesamten Nachschub kosten.
     */
    private fun newRefillPort(
        requestBase: CodexQuestionRequest,
        entranceQuestion: String,
    ) = QuestionRefillPort { existing, onQuestion ->
        val validator = IncrementalQuestionValidator(
            previousQuestions = existing.map(Question::text),
            excludedQuestions = listOf(entranceQuestion),
        )
        val delivered = mutableListOf<Question>()
        val startedAt = SystemClock.elapsedRealtime()
        val deliver: suspend (String) -> Unit = { rawQuestion ->
            validator.accept(rawQuestion).forEach { parsed ->
                val question = Question(emoji = parsed.emoji, text = parsed.text)
                delivered += question
                onQuestion(question)
                Log.i(TAG, "Nachschub: Frage ${delivered.size} nach ${seconds(startedAt)}s")
            }
        }
        Log.i(
            TAG,
            "Nachschub angefordert: Modell=${requestBase.model.apiId}, " +
                "Denkstufe=${requestBase.reasoningEffort.apiValue}, Vorrat=${existing.size}",
        )
        try {
            codexAuthManager.generateQuestions(
                requestBase.copy(previousQuestions = recentQuestionTexts(existing)),
                deliver,
            ).forEach { deliver(it) }
            Log.i(TAG, "Nachschub fertig: ${delivered.size} neue Fragen nach ${seconds(startedAt)}s")
        } catch (cancelled: CancellationException) {
            Log.w(TAG, "Nachschub abgebrochen nach ${seconds(startedAt)}s, ${delivered.size} Fragen angekommen")
            throw cancelled
        } catch (error: Throwable) {
            Log.w(TAG, "Nachschub fehlgeschlagen nach ${seconds(startedAt)}s, ${delivered.size} Fragen angekommen", error)
            if (delivered.isEmpty()) throw error
        }
        delivered
    }

    private fun seconds(startedAt: Long) = (SystemClock.elapsedRealtime() - startedAt) / 1_000L

    /**
     * Nur die zuletzt gestellten Fragen gehen als „nicht wiederholen“ mit in die Anfrage. Ohne
     * Grenze wüchse sie in einer Endlos-Sitzung mit jedem Block weiter, bis der Nachschub daran
     * scheitert.
     */
    private fun recentQuestionTexts(existing: List<Question>): List<String> =
        existing.takeLast(RECENT_QUESTION_CONTEXT).map(Question::text)

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

    /**
     * [voice] is the voice a single history entry was given. It is null for a fresh session,
     * which then follows the settings as before.
     */
    private class TtsAdapter(
        private val manager: TtsManager,
        private val voice: SessionVoice? = null,
    ) : SessionTtsPort {
        override fun speak(text: String, listener: SessionTtsPort.Listener, varied: Boolean) {
            manager.speak(
                text = text,
                onStart = listener::onStart,
                onComplete = listener::onComplete,
                onError = listener::onError,
                varied = varied,
                providerOverride = voice?.provider,
                voiceOverride = voice?.voiceId,
            )
        }

        override fun stop() = manager.stop()
        override fun pause(): Boolean = manager.pause()
        override fun resume(): Boolean = manager.resume()
    }
}

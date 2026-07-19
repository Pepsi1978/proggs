package de.frank.perfectmoment.session

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import de.frank.perfectmoment.auth.CodexAuthManager
import de.frank.perfectmoment.auth.CodexModel
import de.frank.perfectmoment.auth.CodexQuestionRequest
import de.frank.perfectmoment.auth.QuestionResponseValidator
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

    suspend fun startNewSession(topic: String, introContext: String) = operationMutex.withLock {
        releaseEngine()
        val config = currentConfig()
        _state.value = null
        val sessionId = sessionRepository.createSession(newSessionEntity(topic, config))
        val runtime = SessionRuntime(
            topic = topic,
            sessionId = sessionId,
            config = config,
            generating = true,
        )
        _runtime.value = runtime

        try {
            startForegroundSessionService()
            val skill = contentRepository.getSkill(settings.activeSkillId)
                ?: throw IllegalStateException("Der aktive Skill wurde nicht gefunden.")
            val requestBase = CodexQuestionRequest(
                topic = topic,
                introContext = introContext,
                skillText = skill.text,
                operatingModeText = settings.operatingModeText,
                model = CodexModel.fromLabel(settings.model),
                reasoningEffort = ReasoningEffort.fromLabel(settings.reasoning),
            )
            val initial = QuestionResponseValidator.validate(
                codexAuthManager.generateQuestions(requestBase),
            ).map { Question(emoji = it.emoji, text = it.text) }
            sessionRepository.appendQuestions(sessionId, initial)

            val refillPort = QuestionRefillPort { existing ->
                val raw = codexAuthManager.generateQuestions(
                    requestBase.copy(previousQuestions = existing.map(Question::text)),
                )
                QuestionResponseValidator.validate(raw, existing.map(Question::text))
                    .map { "${it.emoji} ${it.text}" }
            }
            createEngine(
                runtime = runtime,
                questions = initial,
                refillPort = refillPort,
                persistencePort = QuestionPersistencePort { questions ->
                    sessionRepository.appendQuestions(sessionId, questions)
                },
            )
        } catch (error: Throwable) {
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
        val questions = source.questions.map {
            Question(id = it.id, emoji = it.emoji, text = it.text)
        }.let { if (shuffle) it.shuffled() else it }
        val sessionId = sessionRepository.createSession(newSessionEntity(source.session.topic, config))
        sessionRepository.appendQuestions(sessionId, questions)
        createEngine(
            runtime = SessionRuntime(source.session.topic, sessionId, config, replay = true),
            questions = questions,
            refillPort = null,
            persistencePort = QuestionPersistencePort { },
        )
        startForegroundSessionService()
    }

    fun toggleSpeaker() {
        val intent = Intent(appContext, SessionForegroundService::class.java)
            .setAction(SessionForegroundService.ACTION_TOGGLE)
        ContextCompat.startForegroundService(appContext, intent)
    }

    internal fun setSpeakerOn(enabled: Boolean) {
        engine?.setSpeakerOn(enabled)
    }

    fun stopAndClear() {
        engine?.stopSession()
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
        )
        engine = created
        _runtime.value = runtime.copy(generating = false)
        _state.value = created.state.value
        stateJob = scope.launch {
            created.state.collect { next ->
                if (generation != engineGeneration.get()) return@collect
                _state.value = next
                if (next.phase == Phase.ENDED) {
                    appContext.stopService(Intent(appContext, SessionForegroundService::class.java))
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

    private fun newSessionEntity(topic: String, config: SessionConfig): SessionEntity {
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
    }
}

package de.frank.perfectmoment.session

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SessionEngine(
    initialQuestions: List<Question>,
    private val config: SessionConfig,
    private val ttsPort: SessionTtsPort,
    private val refillPort: QuestionRefillPort? = null,
    private val persistencePort: QuestionPersistencePort = QuestionPersistencePort { },
    coroutineScope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1),
    private val clock: SessionClock = MonotonicSessionClock,
    private var initialGenerationInFlight: Boolean = false,
    private val checkpoint: SessionCheckpoint? = null,
    private val refillRetryMs: Long = REFILL_RETRY_MS,
    private val offlineThresholdMs: Long = OFFLINE_THRESHOLD_MS,
) : Closeable {
    private val engineJob = SupervisorJob(coroutineScope.coroutineContext[Job])
    private val scope = CoroutineScope(coroutineScope.coroutineContext + engineJob + dispatcher)
    private val started = AtomicBoolean(false)
    private val _state = MutableStateFlow(
        SessionState(
            questions = initialQuestions,
            currentIndex = checkpoint?.currentIndex?.coerceIn(0, initialQuestions.lastIndex.coerceAtLeast(0)) ?: 0,
            currentRep = checkpoint?.currentRep?.coerceIn(1, config.repsPerQuestion) ?: 1,
            phase = if (checkpoint != null && initialQuestions.isNotEmpty()) Phase.SPEAKING else Phase.IDLE_MUTED,
            speakerOn = checkpoint != null && initialQuestions.isNotEmpty(),
            paused = checkpoint != null && initialQuestions.isNotEmpty(),
            remainingMs = if (config.isEndless) {
                checkpoint?.remainingMs
                    ?.takeIf { it > 1L }
                    ?.coerceAtMost(config.initialRemainingMs)
                    ?: config.initialRemainingMs
            } else {
                checkpoint?.remainingMs?.coerceIn(1L, config.durationMs) ?: config.durationMs
            },
        ),
    )
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var deadlineMs = 0L
    private var timerExpired = false
    private var timerJob: Job? = null
    private var cadenceJob: Job? = null
    private var cadenceDeadlineMs = 0L
    private var cadenceContinuation: (() -> Unit)? = null
    private var refillJob: Job? = null
    private var refillRetryJob: Job? = null
    private var offlineMonitorJob: Job? = null
    private var speechGeneration = 0L
    private var pausedAudioAtPosition = false
    private var pausedCadenceRemainingMs = 0L
    private var pausedCadenceContinuation: (() -> Unit)? = null
    private var ttsErrorsForCurrentQuestion = 0
    private var offlineNoticePending = false
    private var offlineNoticeInProgress = false
    private var offlineNoticeDelivered = false
    private val refillTriggers = mutableSetOf<Int>()
    private val networkFailures = mutableMapOf<NetworkOperation, Long>()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            val restored = checkpoint != null && _state.value.questions.isNotEmpty()
            deadlineMs = clock.nowMillis() + _state.value.remainingMs
            _state.value = _state.value.copy(
                phase = when {
                    _state.value.questions.isEmpty() -> Phase.WAITING_NETWORK
                    restored -> Phase.SPEAKING
                    else -> Phase.IDLE_MUTED
                },
                speakerOn = restored,
                paused = restored,
            )
            if (!_state.value.paused) startTimer()
            if (_state.value.questions.isEmpty() && !initialGenerationInFlight) {
                requestRefill()
            } else if (restored) {
                refillIfResumedStockIsLow()
            }
        }
    }

    fun appendQuestions(questions: List<Question>) {
        if (questions.isEmpty()) return
        scope.launch {
            val current = _state.value
            val wasWaitingAtEnd = current.phase == Phase.WAITING_NETWORK &&
                current.currentIndex >= current.questions.size
            _state.value = current.copy(questions = current.questions + questions)
            if (wasWaitingAtEnd && !current.paused) {
                if (current.speakerOn) {
                    afterOfflineNotice(::speakCurrentQuestion)
                } else {
                    _state.value = _state.value.copy(phase = Phase.IDLE_MUTED)
                }
            }
        }
    }

    fun completeInitialGeneration() {
        scope.launch {
            initialGenerationInFlight = false
            if (_state.value.currentIndex >= _state.value.questions.size) handleQuestionsExhausted()
        }
    }

    fun setSpeakerOn(enabled: Boolean) {
        scope.launch { setSpeakerOnInternal(enabled) }
    }

    fun togglePause() {
        scope.launch {
            if (_state.value.paused) resumeSessionInternal() else pauseSessionInternal()
        }
    }

    fun toggleSpeaker() {
        scope.launch { setSpeakerOnInternal(!_state.value.speakerOn) }
    }

    private fun setSpeakerOnInternal(enabled: Boolean) {
        val current = _state.value
        if (current.phase == Phase.ENDED || current.speakerOn == enabled) return
        if (current.paused) {
            if (!enabled && pausedAudioAtPosition) {
                speechGeneration++
                pausedAudioAtPosition = false
                ttsPort.stop()
            }
            _state.value = current.copy(speakerOn = enabled)
            return
        }
        if (!enabled && timerExpired) {
            endSession()
            return
        }

        speechGeneration++
        cadenceJob?.cancel()
        cadenceJob = null
        cadenceDeadlineMs = 0L
        cadenceContinuation = null
        if (!enabled) {
            offlineNoticeInProgress = false
            ttsPort.stop()
            _state.value = current.copy(
                speakerOn = false,
                phase = Phase.IDLE_MUTED,
            )
            return
        }

        _state.value = current.copy(speakerOn = true)
        if (_state.value.currentIndex < _state.value.questions.size) {
            speakCurrentQuestion()
        } else {
            handleQuestionsExhausted()
        }
    }

    fun stopSession() {
        scope.launch { endSession() }
    }

    private fun pauseSessionInternal() {
        val current = _state.value
        if (current.phase == Phase.ENDED || current.paused) return

        val remaining = max(0L, deadlineMs - clock.nowMillis())
        timerJob?.cancel()
        timerJob = null
        when (current.phase) {
            Phase.SPEAKING -> {
                pausedAudioAtPosition = ttsPort.pause()
                if (!pausedAudioAtPosition) {
                    speechGeneration++
                    ttsPort.stop()
                }
            }
            Phase.PAUSE_REP, Phase.PAUSE_NEXT -> {
                pausedCadenceRemainingMs = max(0L, cadenceDeadlineMs - clock.nowMillis())
                pausedCadenceContinuation = cadenceContinuation
                cadenceJob?.cancel()
                cadenceJob = null
            }
            Phase.WAITING_NETWORK -> if (offlineNoticeInProgress) {
                pausedAudioAtPosition = ttsPort.pause()
                if (!pausedAudioAtPosition) {
                    speechGeneration++
                    ttsPort.stop()
                    offlineNoticeInProgress = false
                    offlineNoticePending = true
                }
            }
            else -> Unit
        }
        _state.value = current.copy(paused = true, remainingMs = remaining)
    }

    private fun resumeSessionInternal() {
        val current = _state.value
        if (!current.paused || current.phase == Phase.ENDED) return
        if (!config.isEndless && current.remainingMs <= 0L) {
            endSession()
            return
        }

        timerExpired = false
        deadlineMs = clock.nowMillis() + current.remainingMs
        _state.value = current.copy(paused = false)
        startTimer()
        when (current.phase) {
            Phase.SPEAKING -> {
                if (!current.speakerOn) {
                    _state.value = _state.value.copy(phase = Phase.IDLE_MUTED)
                } else if (!pausedAudioAtPosition || !ttsPort.resume()) {
                    pausedAudioAtPosition = false
                    speakCurrentQuestion()
                } else {
                    pausedAudioAtPosition = false
                }
            }
            Phase.PAUSE_REP, Phase.PAUSE_NEXT -> {
                val next = pausedCadenceContinuation
                val remaining = pausedCadenceRemainingMs
                pausedCadenceContinuation = null
                pausedCadenceRemainingMs = 0L
                if (next != null) schedulePause(current.phase, remaining, next)
            }
            Phase.WAITING_NETWORK -> when {
                pausedAudioAtPosition && ttsPort.resume() -> pausedAudioAtPosition = false
                current.speakerOn && current.currentIndex < current.questions.size -> {
                    pausedAudioAtPosition = false
                    afterOfflineNotice(::speakCurrentQuestion)
                }
            }
            else -> Unit
        }
    }

    override fun close() {
        ttsPort.stop()
        scope.cancel()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(TIMER_TICK_MS)
                val remaining = max(0L, deadlineMs - clock.nowMillis())
                _state.value = _state.value.copy(remainingMs = remaining)
                if (remaining == 0L) {
                    if (config.isEndless) {
                        deadlineMs = clock.nowMillis() + config.initialRemainingMs
                        _state.value = _state.value.copy(remainingMs = config.initialRemainingMs)
                        continue
                    }
                    timerExpired = true
                    if (_state.value.phase != Phase.SPEAKING) endSession()
                    return@launch
                }
            }
        }
    }

    private fun speakCurrentQuestion() {
        val current = _state.value
        if (!current.speakerOn || current.paused || current.phase == Phase.ENDED) return
        if (timerExpired) {
            endSession()
            return
        }
        if (current.currentIndex >= current.questions.size) {
            handleQuestionsExhausted()
            return
        }

        cadenceJob?.cancel()
        cadenceJob = null
        val token = ++speechGeneration
        _state.value = current.copy(phase = Phase.SPEAKING)
        triggerRefillIfNeeded(current.currentIndex)
        val text = current.questions[current.currentIndex].text

        try {
            ttsPort.speak(
                text,
                object : SessionTtsPort.Listener {
                    override fun onComplete() {
                        scope.launch { handleTtsComplete(token) }
                    }

                    override fun onError(error: Throwable?) {
                        scope.launch { handleTtsError(token) }
                    }
                },
                varied = true,
            )
        } catch (error: Exception) {
            scope.launch { handleTtsError(token) }
        }
    }

    private fun handleTtsComplete(token: Long) {
        if (token != speechGeneration || _state.value.paused || _state.value.phase != Phase.SPEAKING) return
        markNetworkSuccess(NetworkOperation.TTS)
        if (timerExpired) {
            endSession()
            return
        }

        afterOfflineNotice {
            val current = _state.value
            if (current.currentRep < config.repsPerQuestion) {
                schedulePause(Phase.PAUSE_REP, config.pauseRepMs) {
                    _state.value = _state.value.copy(currentRep = _state.value.currentRep + 1)
                    speakCurrentQuestion()
                }
            } else {
                advanceToNextQuestion()
            }
        }
    }

    private fun handleTtsError(token: Long) {
        if (token != speechGeneration || _state.value.paused || _state.value.phase != Phase.SPEAKING) return
        markNetworkFailure(NetworkOperation.TTS)
        if (timerExpired) {
            endSession()
            return
        }

        ttsErrorsForCurrentQuestion++
        if (ttsErrorsForCurrentQuestion < MAX_TTS_ERRORS_PER_QUESTION) {
            schedulePause(Phase.PAUSE_REP, config.pauseRepMs) {
                afterOfflineNotice(::speakCurrentQuestion)
            }
        } else {
            advanceToNextQuestion()
        }
    }

    private fun advanceToNextQuestion() {
        val nextIndex = _state.value.currentIndex + 1
        if (nextIndex >= _state.value.questions.size) {
            _state.value = _state.value.copy(
                currentIndex = nextIndex,
                currentRep = 1,
                phase = Phase.WAITING_NETWORK,
            )
            ttsErrorsForCurrentQuestion = 0
            requestRefill()
            return
        }

        schedulePause(Phase.PAUSE_NEXT, config.pauseNextMs) {
            _state.value = _state.value.copy(currentIndex = nextIndex, currentRep = 1)
            ttsErrorsForCurrentQuestion = 0
            afterOfflineNotice(::speakCurrentQuestion)
        }
    }

    private fun schedulePause(phase: Phase, durationMs: Long, next: () -> Unit) {
        if (timerExpired) {
            endSession()
            return
        }
        _state.value = _state.value.copy(phase = phase)
        cadenceJob?.cancel()
        cadenceDeadlineMs = clock.nowMillis() + durationMs
        cadenceContinuation = next
        cadenceJob = scope.launch {
            delay(durationMs)
            cadenceJob = null
            cadenceDeadlineMs = 0L
            cadenceContinuation = null
            if (_state.value.speakerOn && _state.value.phase != Phase.ENDED) next()
        }
    }

    /**
     * Beim Fortsetzen liegt der Index-Trigger des laufenden Blocks meist schon hinter dem
     * Fortsetzungspunkt. Reicht der gespeicherte Vorrat nicht mehr für den üblichen Vorlauf,
     * wird der Nachschub sofort angestossen — die Sitzung endet dann nicht am letzten
     * gespeicherten Fragen, sondern läuft wie eine frisch gestartete weiter.
     */
    private fun refillIfResumedStockIsLow() {
        val current = _state.value
        if (current.questions.size - current.currentIndex > RESUME_REFILL_THRESHOLD) return
        requestRefill()
    }

    private fun triggerRefillIfNeeded(index: Int) {
        if (index < FIRST_REFILL_INDEX || (index - FIRST_REFILL_INDEX) % BLOCK_SIZE != 0) return
        if (refillTriggers.add(index)) requestRefill()
    }

    private fun requestRefill() {
        if (initialGenerationInFlight || refillPort == null || _state.value.refillInFlight ||
            _state.value.phase == Phase.ENDED
        ) {
            return
        }
        refillRetryJob?.cancel()
        refillRetryJob = null
        _state.value = _state.value.copy(refillInFlight = true)
        refillJob = scope.launch {
            try {
                val parsed = refillPort.requestQuestions(_state.value.questions)
                    .map(EmojiParser::parse)
                    .filter { it.text.isNotBlank() }
                    .map { Question(emoji = it.emoji, text = it.text) }
                if (parsed.isEmpty()) {
                    refillFailed()
                    return@launch
                }

                persistencePort.persistQuestions(parsed)
                _state.value = _state.value.copy(
                    questions = _state.value.questions + parsed,
                    refillInFlight = false,
                )
                markNetworkSuccess(NetworkOperation.REFILL)
                if (_state.value.speakerOn && !_state.value.paused &&
                    _state.value.currentIndex < _state.value.questions.size &&
                    _state.value.phase == Phase.WAITING_NETWORK && !offlineNoticeInProgress
                ) {
                    afterOfflineNotice(::speakCurrentQuestion)
                } else if (!_state.value.speakerOn && _state.value.phase == Phase.WAITING_NETWORK) {
                    _state.value = _state.value.copy(phase = Phase.IDLE_MUTED)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                refillFailed()
            }
        }
    }

    private fun refillFailed() {
        _state.value = _state.value.copy(refillInFlight = false)
        markNetworkFailure(NetworkOperation.REFILL)
        scheduleRefillRetry()
    }

    private fun scheduleRefillRetry() {
        if (refillPort == null || refillRetryJob?.isActive == true || _state.value.phase == Phase.ENDED) return
        refillRetryJob = scope.launch {
            delay(refillRetryMs)
            refillRetryJob = null
            requestRefill()
        }
    }

    /**
     * Am Listenende endet die Sitzung nie von selbst — sie wartet auf Nachschub. Das gilt für
     * jede Sitzung, auch für gespeicherte aus dem Verlauf.
     */
    private fun handleQuestionsExhausted() {
        _state.value = _state.value.copy(phase = Phase.WAITING_NETWORK)
        if (!initialGenerationInFlight) requestRefill()
    }

    private fun markNetworkFailure(operation: NetworkOperation) {
        networkFailures.putIfAbsent(operation, clock.nowMillis())
        scheduleOfflineMonitor()
    }

    private fun markNetworkSuccess(operation: NetworkOperation) {
        networkFailures.remove(operation)
        val earliestFailure = networkFailures.values.minOrNull()
        val stillOffline = earliestFailure != null &&
            clock.nowMillis() - earliestFailure >= offlineThresholdMs
        _state.value = _state.value.copy(offline = stillOffline)
        if (earliestFailure == null) {
            offlineNoticePending = false
            offlineNoticeDelivered = false
        }
        scheduleOfflineMonitor()
    }

    private fun scheduleOfflineMonitor() {
        offlineMonitorJob?.cancel()
        val earliestFailure = networkFailures.values.minOrNull()
        if (earliestFailure == null) {
            offlineMonitorJob = null
            return
        }
        offlineMonitorJob = scope.launch {
            delay(max(0L, offlineThresholdMs - (clock.nowMillis() - earliestFailure)))
            val stillEarliest = networkFailures.values.minOrNull() ?: return@launch
            if (clock.nowMillis() - stillEarliest >= offlineThresholdMs) {
                _state.value = _state.value.copy(offline = true)
                val shouldAnnounce = !offlineNoticeDelivered
                if (shouldAnnounce) {
                    offlineNoticeDelivered = true
                    offlineNoticePending = true
                }
                if (shouldAnnounce && _state.value.speakerOn && !_state.value.paused &&
                    _state.value.phase == Phase.WAITING_NETWORK &&
                    !offlineNoticeInProgress
                ) {
                    afterOfflineNotice {
                        if (_state.value.currentIndex < _state.value.questions.size) {
                            speakCurrentQuestion()
                        } else {
                            _state.value = _state.value.copy(phase = Phase.WAITING_NETWORK)
                        }
                    }
                }
            }
        }
    }

    private fun afterOfflineNotice(next: () -> Unit) {
        if (!offlineNoticePending || !_state.value.speakerOn || timerExpired) {
            next()
            return
        }
        offlineNoticePending = false
        offlineNoticeInProgress = true
        val token = ++speechGeneration
        _state.value = _state.value.copy(phase = Phase.WAITING_NETWORK)
        try {
            ttsPort.speak(
                OFFLINE_MESSAGE,
                object : SessionTtsPort.Listener {
                    override fun onComplete() {
                        scope.launch {
                            if (token == speechGeneration && _state.value.phase != Phase.ENDED) {
                                offlineNoticeInProgress = false
                                next()
                            }
                        }
                    }

                    override fun onError(error: Throwable?) {
                        scope.launch {
                            if (token == speechGeneration && _state.value.phase != Phase.ENDED) {
                                offlineNoticeInProgress = false
                                next()
                            }
                        }
                    }
                },
            )
        } catch (_: Exception) {
            offlineNoticeInProgress = false
            next()
        }
    }

    private fun endSession() {
        if (_state.value.phase == Phase.ENDED) return
        speechGeneration++
        offlineNoticeInProgress = false
        _state.value = _state.value.copy(
            phase = Phase.ENDED,
            speakerOn = false,
            paused = false,
            remainingMs = max(0L, _state.value.remainingMs),
            refillInFlight = false,
        )
        ttsPort.stop()
        cadenceJob?.cancel()
        cadenceDeadlineMs = 0L
        cadenceContinuation = null
        pausedCadenceContinuation = null
        refillJob?.cancel()
        refillRetryJob?.cancel()
        offlineMonitorJob?.cancel()
        timerJob?.cancel()
    }

    private enum class NetworkOperation {
        TTS,
        REFILL,
    }

    companion object {
        const val OFFLINE_MESSAGE = "Keine Verbindung. Die Sitzung wartet."
        const val REFILL_RETRY_MS = 15_000L
        const val OFFLINE_THRESHOLD_MS = 120_000L
        private const val TIMER_TICK_MS = 100L
        private const val FIRST_REFILL_INDEX = 19
        private const val BLOCK_SIZE = 30
        private const val RESUME_REFILL_THRESHOLD = BLOCK_SIZE - FIRST_REFILL_INDEX
        private const val MAX_TTS_ERRORS_PER_QUESTION = 3
    }
}

package de.frank.entropyreducer.presentation.mental

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.tts.TtsPlaybackService
import de.frank.entropyreducer.di.ApplicationScope
import de.frank.entropyreducer.domain.tts.TtsPlayer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class MentalTtsPlaybackController
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val ttsPlayer: TtsPlayer,
    private val appSettings: AppSettings,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private companion object {
        const val TAG = "MentalTtsPlayback"
        const val DEFAULT_PAUSE_SECONDS = 9
        const val RANGE_MIN = 1
        const val RANGE_MAX = 10
        const val PAUSE_RANGE_MIN = 1
        const val PAUSE_RANGE_MAX = 30

        val KEY_ANKER = intPreferencesKey("anker_count")
        val KEY_FOLGE = intPreferencesKey("folge_count")
        val KEY_INCLUDE_HABITS = booleanPreferencesKey("include_habits")
        val KEY_MENTAL_PAUSE_SECONDS = intPreferencesKey("pause_seconds")
        val KEY_RANDOM_PLAYBACK = booleanPreferencesKey("random_playback")
        val KEY_GEWOHNHEIT_LOOP = booleanPreferencesKey("loop_enabled")
    }

    private val lock = Any()
    private var playbackJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var playbackLabel: String? = null
    private var autoStopCountdown: PausableCountdown? = null
    private var countdownTickerJob: Job? = null

    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow

    private val _isPausedFlow = MutableStateFlow(false)
    val isPausedFlow: StateFlow<Boolean> = _isPausedFlow

    private val _errorFlow = MutableStateFlow<String?>(null)
    val errorFlow: StateFlow<String?> = _errorFlow

    private val _autoStopEndsAtWallClockMsFlow = MutableStateFlow<Long?>(null)
    val autoStopEndsAtWallClockMsFlow: StateFlow<Long?> = _autoStopEndsAtWallClockMsFlow

    private val _autoStopRemainingMsFlow = MutableStateFlow<Long?>(null)
    val autoStopRemainingMsFlow: StateFlow<Long?> = _autoStopRemainingMsFlow

    // Frank-Wunsch 2026-07-15: welcher Lauf gerade aktiv ist ("Mental"/"Gewohnheit"/"Spezial"/null).
    // So kann der Spezial-Player unterscheiden, ob gerade SEIN Lauf laeuft (Play/Pause/Stop richtig).
    private val _playbackLabelFlow = MutableStateFlow<String?>(null)
    val playbackLabelFlow: StateFlow<String?> = _playbackLabelFlow

    fun dismissError() {
        _errorFlow.value = null
    }

    fun startMentalPlayback(mentals: List<Mental>, gewohnheiten: List<Mental>) {
        if (_isPlayingFlow.value && _playbackLabelFlow.value == "Mental") {
            if (_isPausedFlow.value) resume()
            return
        }
        if (_isPlayingFlow.value) stop()
        val mentalTexts = mentals.map { it.text.trim() }.filter { it.isNotEmpty() }
        if (mentalTexts.isEmpty()) {
            _errorFlow.value = "Keine aktivierten Mentals zum Vorlesen vorhanden."
            return
        }

        startPlayback("Mental") {
            val settings = mentalSettings()
            val autoStopMinutes = appSettings.ttsAutoStopMinutesFlow.first()
            val autoStopMs = markAutoStopDeadline("Mental", autoStopMinutes)
            val gewohnheitTexts =
                if (settings.includeHabits) {
                    gewohnheiten.map { it.text.trim() }.filter { it.isNotEmpty() }
                } else {
                    emptyList()
                }
            val gewohnheitSettings =
                if (settings.includeHabits && gewohnheitTexts.isNotEmpty()) {
                    gewohnheitSettings()
                } else {
                    GewohnheitSettings(repeatCount = 1, loop = false, pauseSeconds = DEFAULT_PAUSE_SECONDS)
                }
            runMentalSequence(
                mentalTexts = mentalTexts,
                anker = settings.anker,
                folge = settings.folge,
                gewohnheitTexts = gewohnheitTexts,
                gewohnheitRepeat = gewohnheitSettings.repeatCount,
                mentalPauseMs = settings.pauseSeconds * 1_000L,
                gewohnheitPauseMs = gewohnheitSettings.pauseSeconds * 1_000L,
                randomPlayback = settings.randomPlayback,
                autoStopMs = autoStopMs,
            )
        }
    }

    /**
     * Spezieller Vorlese-Lauf (Frank-Wunsch 2026-07-15). Play-Button:
     * - Laeuft bereits DER Spezial-Lauf und ist pausiert -> fortsetzen (resume).
     * - Laeuft bereits der Spezial-Lauf und spielt -> nichts tun.
     * - Laeuft ein ANDERER Lauf (Mental/Gewohnheit) -> zuerst stoppen, dann Spezial starten.
     * - Sonst -> Spezial starten.
     *
     * Liest nur die uebergebenen (aktivierten) Saetze, jeder Satz einmal pro Durchlauf, in
     * Endlosschleife bis Stop oder Abschaltzeit. Jeder Satz wird frisch synthetisiert
     * (forceFresh=true), damit keine zwei Wiederholungen exakt gleich betont klingen.
     */
    fun startSpecialPlayback(
        specialTexts: List<String>,
        pauseSeconds: Int,
        randomPlayback: Boolean,
        autoStopMinutes: Int,
    ) {
        if (_isPlayingFlow.value && _playbackLabelFlow.value == "Spezial") {
            if (_isPausedFlow.value) resume()
            return
        }
        if (_isPlayingFlow.value) stop()
        val texts = specialTexts.map { it.trim() }.filter { it.isNotEmpty() }
        if (texts.isEmpty()) {
            _errorFlow.value = "Keine aktivierten speziellen Mentals zum Vorlesen vorhanden."
            return
        }
        startPlayback("Spezial") {
            val autoStopMs = markAutoStopDeadline("Spezial", autoStopMinutes)
            runSpecialSequence(
                texts = texts,
                pauseMs = pauseSeconds * 1_000L,
                randomPlayback = randomPlayback,
                autoStopMs = autoStopMs,
            )
        }
    }

    fun toggleGewohnheitPlayback(gewohnheiten: List<Mental>) {
        if (_isPlayingFlow.value) {
            stop()
            return
        }
        val texts = gewohnheiten.map { it.text.trim() }.filter { it.isNotEmpty() }
        if (texts.isEmpty()) {
            _errorFlow.value = "Keine Gewohnheiten zum Vorlesen vorhanden."
            return
        }

        startPlayback("Gewohnheit") {
            val settings = gewohnheitSettings()
            val randomPlayback = mentalSettings().randomPlayback
            val autoStopMinutes = appSettings.ttsAutoStopMinutesFlow.first()
            val autoStopMs = markAutoStopDeadline("Gewohnheit", autoStopMinutes)
            runGewohnheitSequence(
                texts = texts,
                repeat = settings.repeatCount,
                loop = settings.loop,
                pauseMs = settings.pauseSeconds * 1_000L,
                randomPlayback = randomPlayback,
                autoStopMs = autoStopMs,
            )
        }
    }

    fun stop() {
        val job = synchronized(lock) {
            val current = playbackJob
            playbackJob = null
            current
        }
        job?.cancel()
        ttsPlayer.stop()
        ttsPlayer.clearSequenceCache()
        stopBackgroundPlaybackProtection()
        _autoStopEndsAtWallClockMsFlow.value = null
        _autoStopRemainingMsFlow.value = null
        _isPausedFlow.value = false
        _isPlayingFlow.value = false
        _playbackLabelFlow.value = null
        synchronized(lock) {
            countdownTickerJob?.cancel()
            countdownTickerJob = null
            autoStopCountdown = null
            playbackLabel = null
        }
    }

    fun pause() {
        if (!_isPlayingFlow.value || _isPausedFlow.value) return
        synchronized(lock) { autoStopCountdown?.pause() }
        _isPausedFlow.value = true
        _autoStopEndsAtWallClockMsFlow.value = null
        _autoStopRemainingMsFlow.value = synchronized(lock) { autoStopCountdown?.remainingMs() }
        ttsPlayer.pause()
        stopBackgroundPlaybackProtection()
        Diag.d(DiagnosticArea.GOOGLE_TTS, TAG, "Vorlesen pausiert; Auto-Stop-Zeit eingefroren")
    }

    fun resume() {
        if (!_isPlayingFlow.value || !_isPausedFlow.value) return
        val (label, remainingMs) = synchronized(lock) {
            val countdown = autoStopCountdown ?: return
            countdown.resume()
            (playbackLabel ?: "Mental") to countdown.remainingMs()
        }
        _autoStopEndsAtWallClockMsFlow.value = System.currentTimeMillis() + remainingMs
        _autoStopRemainingMsFlow.value = remainingMs
        _isPausedFlow.value = false
        runCatching { TtsPlaybackService.start(context, label) }
            .onFailure { Diag.e(DiagnosticArea.GOOGLE_TTS, TAG, "$label-Foreground-Service konnte nicht fortgesetzt werden: ${it.message}", it) }
        startBackgroundPlaybackProtection(label, remainingMs)
        ttsPlayer.resume()
        Diag.d(DiagnosticArea.GOOGLE_TTS, TAG, "Vorlesen fortgesetzt; verbleibende Auto-Stop-Zeit=${remainingMs / 1_000}s")
    }

    private fun markAutoStopDeadline(label: String, autoStopMinutes: Int): Long {
        val autoStopMs = autoStopMinutes * 60 * 1_000L
        val endsAt = System.currentTimeMillis() + autoStopMs
        _autoStopEndsAtWallClockMsFlow.value = endsAt
        val endTime = SimpleDateFormat("HH:mm:ss", Locale.GERMANY).format(Date(endsAt))
        Diag.d(
            DiagnosticArea.GOOGLE_TTS,
            TAG,
            "$label-Vorlesen gestartet: Auto-Stop=$autoStopMinutes Minuten, aktiv bis $endTime Uhr",
        )
        startBackgroundPlaybackProtection(label, autoStopMs)
        synchronized(lock) {
            playbackLabel = label
            autoStopCountdown = PausableCountdown(autoStopMs)
        }
        _autoStopRemainingMsFlow.value = autoStopMs
        startCountdownTicker()
        return autoStopMs
    }

    private fun startCountdownTicker() {
        synchronized(lock) {
            countdownTickerJob?.cancel()
            countdownTickerJob = applicationScope.launch {
                while (_isPlayingFlow.value) {
                    _autoStopRemainingMsFlow.value =
                        synchronized(lock) { autoStopCountdown?.remainingMs() }
                    delay(1_000L)
                }
            }
        }
    }

    private fun startPlayback(label: String, block: suspend () -> Unit) {
        _errorFlow.value = null
        _isPlayingFlow.value = true
        _playbackLabelFlow.value = label
        runCatching { TtsPlaybackService.start(context, label) }
            .onFailure { Diag.e(DiagnosticArea.GOOGLE_TTS, TAG, "$label-Foreground-Service konnte nicht starten: ${it.message}", it) }
        var launchedJob: Job? = null
        val job =
            applicationScope.launch(start = CoroutineStart.LAZY) {
                try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val msg = e.message ?: "Vorlesen fehlgeschlagen"
                    _errorFlow.value = msg
                    Diag.e(DiagnosticArea.GOOGLE_TTS, TAG, "$label-Vorlesen fehlgeschlagen: $msg", e)
                } finally {
                    val isCurrent = synchronized(lock) {
                        if (playbackJob === launchedJob) {
                            playbackJob = null
                            true
                        } else {
                            false
                        }
                    }
                    if (isCurrent) {
                        withContext(Dispatchers.Main) { ttsPlayer.stop() }
                        ttsPlayer.clearSequenceCache()
                        stopBackgroundPlaybackProtection()
                        _autoStopEndsAtWallClockMsFlow.value = null
                        _autoStopRemainingMsFlow.value = null
                        _isPausedFlow.value = false
                        _isPlayingFlow.value = false
                        _playbackLabelFlow.value = null
                        synchronized(lock) {
                            countdownTickerJob?.cancel()
                            countdownTickerJob = null
                            autoStopCountdown = null
                            playbackLabel = null
                        }
                    }
                }
            }
        launchedJob = job
        synchronized(lock) { playbackJob = job }
        job.start()
    }

    private fun startBackgroundPlaybackProtection(label: String, autoStopMs: Long) {
        runCatching {
            val pm = context.getSystemService(PowerManager::class.java)
            val timeoutMs = autoStopMs + 60_000L
            val lock = synchronized(lock) {
                wakeLock?.let { if (it.isHeld) it.release() }
                pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EntropyReducer:TtsPlayback")
                    .apply { setReferenceCounted(false) }
                    .also { wakeLock = it }
            }
            lock.acquire(timeoutMs)
            Diag.d(DiagnosticArea.GOOGLE_TTS, TAG, "$label-WakeLock aktiv fuer ${timeoutMs / 60_000} Minuten")
        }.onFailure {
            Diag.e(DiagnosticArea.GOOGLE_TTS, TAG, "$label-WakeLock konnte nicht aktiviert werden: ${it.message}", it)
        }
    }

    private fun stopBackgroundPlaybackProtection() {
        runCatching { TtsPlaybackService.stop(context) }
            .onFailure { Diag.e(DiagnosticArea.GOOGLE_TTS, TAG, "TTS-Foreground-Service konnte nicht gestoppt werden: ${it.message}", it) }
        runCatching {
            synchronized(lock) {
                wakeLock?.let { if (it.isHeld) it.release() }
                wakeLock = null
            }
        }.onFailure {
            Diag.e(DiagnosticArea.GOOGLE_TTS, TAG, "TTS-WakeLock konnte nicht freigegeben werden: ${it.message}", it)
        }
    }

    private suspend fun mentalSettings(): MentalSettings =
        context.mentalTtsStore.data
            .map { p ->
                MentalSettings(
                    anker = (p[KEY_ANKER] ?: 1).coerceIn(RANGE_MIN, RANGE_MAX),
                    folge = (p[KEY_FOLGE] ?: 1).coerceIn(RANGE_MIN, RANGE_MAX),
                    includeHabits = p[KEY_INCLUDE_HABITS] ?: false,
                    randomPlayback = p[KEY_RANDOM_PLAYBACK] ?: false,
                    pauseSeconds =
                        (p[KEY_MENTAL_PAUSE_SECONDS] ?: DEFAULT_PAUSE_SECONDS)
                            .coerceIn(PAUSE_RANGE_MIN, PAUSE_RANGE_MAX),
                )
            }
            .first()

    private suspend fun gewohnheitSettings(): GewohnheitSettings =
        context.gewohnheitTtsStore.data
            .map { p ->
                GewohnheitSettings(
                    repeatCount = (p[KEY_REPEAT] ?: 1).coerceIn(RANGE_MIN, RANGE_MAX),
                    loop = p[KEY_GEWOHNHEIT_LOOP] ?: false,
                    pauseSeconds =
                        (p[KEY_GEWOHNHEIT_PAUSE_SECONDS] ?: DEFAULT_PAUSE_SECONDS)
                            .coerceIn(PAUSE_RANGE_MIN, PAUSE_RANGE_MAX),
                )
            }
            .first()

    private suspend fun runMentalSequence(
        mentalTexts: List<String>,
        anker: Int,
        folge: Int,
        gewohnheitTexts: List<String>,
        gewohnheitRepeat: Int,
        mentalPauseMs: Long,
        gewohnheitPauseMs: Long,
        randomPlayback: Boolean,
        autoStopMs: Long,
    ) {
        val mentalBlocks = buildMentalBlocks(mentalTexts, anker, folge, mentalPauseMs)
        val gewohnheitBlocks = buildHabitBlocks(gewohnheitTexts, gewohnheitRepeat, gewohnheitPauseMs)
        val orderedMentalSequence = buildOrderedMentalSteps(mentalTexts, anker, folge, mentalPauseMs)
        val orderedGewohnheitSequence = gewohnheitBlocks.flatten()
        val orderedSequence = orderedMentalSequence + orderedGewohnheitSequence
        if (orderedSequence.isEmpty()) return
        Diag.d(
            DiagnosticArea.GOOGLE_TTS,
            TAG,
            "Sequenz gebildet: mental=${orderedMentalSequence.size} + gewohnheit=${orderedGewohnheitSequence.size} " +
                "(mentals=${mentalTexts.size} anker=$anker folge=$folge random=$randomPlayback, " +
                "mentalPauseMs=$mentalPauseMs gewohnheiten=${gewohnheitTexts.size} " +
                "repeat=$gewohnheitRepeat gewohnheitPauseMs=$gewohnheitPauseMs)",
        )

        do {
            val sequence = if (randomPlayback) (mentalBlocks + gewohnheitBlocks).shuffled().flatten() else orderedSequence
            for ((index, step) in sequence.withIndex()) {
                currentCoroutineContext().ensureActive()
                awaitResumed()
                if (autoStopReached()) {
                    Diag.d(DiagnosticArea.GOOGLE_TTS, TAG, "${autoStopMs / 60_000}-Minuten-Grenze erreicht - automatischer Stop")
                    return
                }
                val file = ttsPlayer.synthesizeToCache(step.text, forceFresh = true)
                awaitResumed()
                if (autoStopReached()) return
                withContext(Dispatchers.Main) { ttsPlayer.playCachedFileAwait(file) }
                val anotherStepFollows = index != sequence.lastIndex || shouldRepeatMentalPlayback(autoStopReached())
                if (anotherStepFollows) delayWhileActive(step.pauseMs)
            }
        } while (shouldRepeatMentalPlayback(autoStopReached()))
    }

    private suspend fun runGewohnheitSequence(
        texts: List<String>,
        repeat: Int,
        loop: Boolean,
        pauseMs: Long,
        randomPlayback: Boolean,
        autoStopMs: Long,
    ) {
        val blocks = buildHabitBlocks(texts, repeat, pauseMs)
        val orderedSequence = blocks.flatten()
        if (orderedSequence.isEmpty()) return
        Diag.d(
            DiagnosticArea.GOOGLE_TTS,
            TAG,
            "Sequenz gebildet: ${orderedSequence.size} Sätze (sätze=${texts.size} repeat=$repeat loop=$loop random=$randomPlayback pauseMs=$pauseMs)",
        )

        do {
            val sequence = if (randomPlayback) blocks.shuffled().flatten() else orderedSequence
            for ((index, step) in sequence.withIndex()) {
                currentCoroutineContext().ensureActive()
                awaitResumed()
                if (autoStopReached()) {
                    Diag.d(DiagnosticArea.GOOGLE_TTS, TAG, "${autoStopMs / 60_000}-Minuten-Grenze erreicht - automatischer Stop")
                    return
                }
                val file = ttsPlayer.synthesizeToCache(step.text, forceFresh = true)
                awaitResumed()
                if (autoStopReached()) return
                withContext(Dispatchers.Main) { ttsPlayer.playCachedFileAwait(file) }
                val isLastOfRun = index == sequence.lastIndex
                if (!isLastOfRun || loop) delayWhileActive(step.pauseMs)
            }
        } while (loop && !autoStopReached())
    }

    /**
     * Spezieller Mental-Lauf (Frank-Wunsch 2026-07-15): jeder aktivierte Satz einmal pro Durchlauf,
     * Endlosschleife bis Stop oder Abschaltzeit. Jeder Satz wird frisch ueber TTS synthetisiert
     * (forceFresh), damit keine zwei Wiederholungen exakt gleich betont klingen.
     */
    private suspend fun runSpecialSequence(
        texts: List<String>,
        pauseMs: Long,
        randomPlayback: Boolean,
        autoStopMs: Long,
    ) {
        val blocks = texts.map { text -> listOf(SpokenStep(text, pauseMs)) }
        val orderedSequence = blocks.flatten()
        if (orderedSequence.isEmpty()) return
        Diag.d(
            DiagnosticArea.GOOGLE_TTS,
            TAG,
            "Spezial-Sequenz gebildet: ${orderedSequence.size} Sätze (random=$randomPlayback pauseMs=$pauseMs autoStopMs=$autoStopMs)",
        )

        do {
            val sequence = if (randomPlayback) blocks.shuffled().flatten() else orderedSequence
            for (step in sequence) {
                currentCoroutineContext().ensureActive()
                awaitResumed()
                if (autoStopReached()) {
                    Diag.d(DiagnosticArea.GOOGLE_TTS, TAG, "${autoStopMs / 60_000}-Minuten-Grenze erreicht - automatischer Stop (Spezial)")
                    return
                }
                val file = ttsPlayer.synthesizeToCache(step.text, forceFresh = true)
                awaitResumed()
                if (autoStopReached()) return
                withContext(Dispatchers.Main) { ttsPlayer.playCachedFileAwait(file) }
                delayWhileActive(step.pauseMs)
            }
        } while (!autoStopReached())
    }

    private suspend fun awaitResumed() {
        _isPausedFlow.first { paused -> !paused }
        currentCoroutineContext().ensureActive()
    }

    private suspend fun delayWhileActive(durationMs: Long) {
        var remainingMs = durationMs
        while (remainingMs > 0L) {
            awaitResumed()
            val sliceMs = minOf(remainingMs, 100L)
            val startedAt = SystemClock.elapsedRealtime()
            delay(sliceMs)
            if (!_isPausedFlow.value) {
                remainingMs -= (SystemClock.elapsedRealtime() - startedAt).coerceAtMost(sliceMs)
            }
        }
    }

    private fun autoStopReached(): Boolean = synchronized(lock) { autoStopCountdown?.isExpired() == true }

    private fun buildOrderedMentalSteps(texts: List<String>, anker: Int, folge: Int, pauseMs: Long): List<SpokenStep> {
        if (texts.isEmpty()) return emptyList()
        val first = texts.first()
        if (texts.size == 1) return List(anker) { SpokenStep(first, pauseMs) }
        return buildList {
            for (i in 1 until texts.size) {
                repeat(anker) { add(SpokenStep(first, pauseMs)) }
                repeat(folge) { add(SpokenStep(texts[i], pauseMs)) }
            }
        }
    }

    private fun buildMentalBlocks(texts: List<String>, anker: Int, folge: Int, pauseMs: Long): List<List<SpokenStep>> {
        if (texts.isEmpty()) return emptyList()
        val first = texts.first()
        if (texts.size == 1) return listOf(List(anker) { SpokenStep(first, pauseMs) })
        return texts.drop(1).map { followText ->
            buildList {
                repeat(anker) { add(SpokenStep(first, pauseMs)) }
                repeat(folge) { add(SpokenStep(followText, pauseMs)) }
            }
        }
    }

    private fun buildHabitBlocks(texts: List<String>, times: Int, pauseMs: Long): List<List<SpokenStep>> =
        texts.map { text -> List(times) { SpokenStep(text, pauseMs) } }

    private data class MentalSettings(
        val anker: Int,
        val folge: Int,
        val includeHabits: Boolean,
        val randomPlayback: Boolean,
        val pauseSeconds: Int,
    )

    private data class GewohnheitSettings(
        val repeatCount: Int,
        val loop: Boolean,
        val pauseSeconds: Int,
    )

    private data class SpokenStep(val text: String, val pauseMs: Long)
}

internal fun shouldRepeatMentalPlayback(autoStopReached: Boolean): Boolean = !autoStopReached

internal class PausableCountdown(
    private val totalMs: Long,
    private val nowMs: () -> Long = SystemClock::elapsedRealtime,
) {
    private var remainingMs = totalMs.coerceAtLeast(0L)
    private var activeSinceMs: Long? = nowMs()

    @Synchronized
    fun pause() {
        updateRemaining()
        activeSinceMs = null
    }

    @Synchronized
    fun resume() {
        if (remainingMs > 0L && activeSinceMs == null) activeSinceMs = nowMs()
    }

    @Synchronized
    fun remainingMs(): Long {
        updateRemaining()
        return remainingMs
    }

    @Synchronized
    fun isExpired(): Boolean = remainingMs() <= 0L

    private fun updateRemaining() {
        val activeSince = activeSinceMs ?: return
        val now = nowMs()
        remainingMs = (remainingMs - (now - activeSince).coerceAtLeast(0L)).coerceAtLeast(0L)
        activeSinceMs = now
    }
}

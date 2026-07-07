package de.frank.entropyreducer.presentation.mental

import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.di.ApplicationScope
import de.frank.entropyreducer.domain.tts.TtsPlayer
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
        val KEY_MENTAL_LOOP = booleanPreferencesKey("loop_enabled")
        val KEY_INCLUDE_HABITS = booleanPreferencesKey("include_habits")
        val KEY_MENTAL_PAUSE_SECONDS = intPreferencesKey("pause_seconds")
        val KEY_RANDOM_PLAYBACK = booleanPreferencesKey("random_playback")
        val KEY_GEWOHNHEIT_LOOP = booleanPreferencesKey("loop_enabled")
    }

    private val lock = Any()
    private var playbackJob: Job? = null

    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow

    private val _errorFlow = MutableStateFlow<String?>(null)
    val errorFlow: StateFlow<String?> = _errorFlow

    fun dismissError() {
        _errorFlow.value = null
    }

    fun toggleMentalPlayback(mentals: List<Mental>, gewohnheiten: List<Mental>) {
        if (_isPlayingFlow.value) {
            stop()
            return
        }
        val mentalTexts = mentals.map { it.text.trim() }.filter { it.isNotEmpty() }
        if (mentalTexts.isEmpty()) {
            _errorFlow.value = "Keine Mentals zum Vorlesen vorhanden."
            return
        }

        startPlayback("Mental") {
            val settings = mentalSettings()
            val autoStopMinutes = appSettings.ttsAutoStopMinutesFlow.first()
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
                loop = settings.loop,
                gewohnheitTexts = gewohnheitTexts,
                gewohnheitRepeat = gewohnheitSettings.repeatCount,
                mentalPauseMs = settings.pauseSeconds * 1_000L,
                gewohnheitPauseMs = gewohnheitSettings.pauseSeconds * 1_000L,
                randomPlayback = settings.randomPlayback,
                autoStopMs = autoStopMinutes * 60 * 1_000L,
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
            runGewohnheitSequence(
                texts = texts,
                repeat = settings.repeatCount,
                loop = settings.loop,
                pauseMs = settings.pauseSeconds * 1_000L,
                randomPlayback = randomPlayback,
                autoStopMs = autoStopMinutes * 60 * 1_000L,
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
        _isPlayingFlow.value = false
    }

    private fun startPlayback(label: String, block: suspend () -> Unit) {
        _errorFlow.value = null
        _isPlayingFlow.value = true
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
                        _isPlayingFlow.value = false
                    }
                }
            }
        launchedJob = job
        synchronized(lock) { playbackJob = job }
        job.start()
    }

    private suspend fun mentalSettings(): MentalSettings =
        context.mentalTtsStore.data
            .map { p ->
                MentalSettings(
                    anker = (p[KEY_ANKER] ?: 1).coerceIn(RANGE_MIN, RANGE_MAX),
                    folge = (p[KEY_FOLGE] ?: 1).coerceIn(RANGE_MIN, RANGE_MAX),
                    loop = p[KEY_MENTAL_LOOP] ?: false,
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
        loop: Boolean,
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
                "(mentals=${mentalTexts.size} anker=$anker folge=$folge loop=$loop random=$randomPlayback, " +
                "mentalPauseMs=$mentalPauseMs gewohnheiten=${gewohnheitTexts.size} " +
                "repeat=$gewohnheitRepeat gewohnheitPauseMs=$gewohnheitPauseMs)",
        )

        val deadline = SystemClock.elapsedRealtime() + autoStopMs
        do {
            val sequence = if (randomPlayback) (mentalBlocks + gewohnheitBlocks).shuffled().flatten() else orderedSequence
            for ((index, step) in sequence.withIndex()) {
                currentCoroutineContext().ensureActive()
                if (SystemClock.elapsedRealtime() >= deadline) {
                    Diag.d(DiagnosticArea.GOOGLE_TTS, TAG, "${autoStopMs / 60_000}-Minuten-Grenze erreicht - automatischer Stop")
                    return
                }
                val file = ttsPlayer.synthesizeToCache(step.text, forceFresh = true)
                withContext(Dispatchers.Main) { ttsPlayer.playCachedFileAwait(file) }
                val isLastOfRun = index == sequence.lastIndex
                if (!isLastOfRun || loop) delay(step.pauseMs)
            }
        } while (loop && SystemClock.elapsedRealtime() < deadline)
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

        val deadline = SystemClock.elapsedRealtime() + autoStopMs
        do {
            val sequence = if (randomPlayback) blocks.shuffled().flatten() else orderedSequence
            for ((index, step) in sequence.withIndex()) {
                currentCoroutineContext().ensureActive()
                if (SystemClock.elapsedRealtime() >= deadline) {
                    Diag.d(DiagnosticArea.GOOGLE_TTS, TAG, "${autoStopMs / 60_000}-Minuten-Grenze erreicht - automatischer Stop")
                    return
                }
                val file = ttsPlayer.synthesizeToCache(step.text, forceFresh = true)
                withContext(Dispatchers.Main) { ttsPlayer.playCachedFileAwait(file) }
                val isLastOfRun = index == sequence.lastIndex
                if (!isLastOfRun || loop) delay(step.pauseMs)
            }
        } while (loop && SystemClock.elapsedRealtime() < deadline)
    }

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
        val loop: Boolean,
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

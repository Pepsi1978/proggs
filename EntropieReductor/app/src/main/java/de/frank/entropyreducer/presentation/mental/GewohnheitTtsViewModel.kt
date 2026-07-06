package de.frank.entropyreducer.presentation.mental

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.domain.tts.TtsPlayer
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Vorlese-System fuer den Gewohnheitsreiter (einfacher als MentalTtsViewModel):
 * - Jeder Satz wird [repeatCount]-mal vorgelesen.
 * - 9 Sekunden Pause zwischen jedem gesprochenen Satz.
 * - Jede Wiederholung wird frisch ueber TTS synthetisiert, damit sie nie exakt gleich klingt.
 * - Endlosschleife bis 15 Minuten (Sicherheits-Auto-Stop).
 * - Nur die Gewohnheiten UEBER dem Separator werden vorgelesen.
 */

// internal (nicht private): das Mental-Vorlesen mit aktivem "G"-Haekchen liest die
// "wie-oft"-Zahl der Gewohnheiten aus DIESEM Store (gleicher DataStore, keine zweite Instanz
// auf dieselbe Datei — das wuerde crashen). Frank-Wunsch 2026-07-03.
internal val Context.gewohnheitTtsStore by preferencesDataStore(name = "gewohnheit_tts_settings")
internal val KEY_REPEAT = intPreferencesKey("repeat_count")
internal val KEY_GEWOHNHEIT_PAUSE_SECONDS = intPreferencesKey("pause_seconds")
private val KEY_LOOP = booleanPreferencesKey("loop_enabled")

data class GewohnheitTtsUiState(
    val isPlaying: Boolean = false,
    /** Wie oft jeder Satz vorgelesen wird (1..10). */
    val repeatCount: Int = 1,
    /** Endlosschleife aktiv? */
    val loop: Boolean = false,
    /** Pause zwischen zwei gesprochenen Gewohnheits-Saetzen (1..30 Sekunden). */
    val pauseSeconds: Int = 9,
    val error: String? = null,
)

private data class GewohnheitTtsSettings(
    val repeatCount: Int,
    val loop: Boolean,
    val pauseSeconds: Int,
)

@HiltViewModel
class GewohnheitTtsViewModel
@Inject
constructor(
    application: Application,
    private val ttsPlayer: TtsPlayer,
) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "GewohnheitTts"
        const val DEFAULT_PAUSE_SECONDS = 9
        const val MAX_DURATION_MS = 15 * 60 * 1_000L
        const val RANGE_MIN = 1
        const val RANGE_MAX = 10
        const val PAUSE_RANGE_MIN = 1
        const val PAUSE_RANGE_MAX = 30
    }

    private val ctx: Context
        get() = getApplication()

    private val settingsFlow: Flow<GewohnheitTtsSettings> =
        ctx.gewohnheitTtsStore.data.map { p ->
            GewohnheitTtsSettings(
                repeatCount = (p[KEY_REPEAT] ?: 1).coerceIn(RANGE_MIN, RANGE_MAX),
                loop = p[KEY_LOOP] ?: false,
                pauseSeconds = (p[KEY_GEWOHNHEIT_PAUSE_SECONDS] ?: DEFAULT_PAUSE_SECONDS)
                    .coerceIn(PAUSE_RANGE_MIN, PAUSE_RANGE_MAX),
            )
        }

    private val isPlayingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GewohnheitTtsUiState> =
        combine(settingsFlow, isPlayingFlow, errorFlow) { settings, playing, err ->
                GewohnheitTtsUiState(
                    isPlaying = playing,
                    repeatCount = settings.repeatCount,
                    loop = settings.loop,
                    pauseSeconds = settings.pauseSeconds,
                    error = err,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = GewohnheitTtsUiState(),
            )

    private var ttsJob: Job? = null

    fun setRepeatCount(n: Int) {
        viewModelScope.launch {
            ctx.gewohnheitTtsStore.edit { it[KEY_REPEAT] = n.coerceIn(RANGE_MIN, RANGE_MAX) }
        }
    }

    fun setLoop(enabled: Boolean) {
        viewModelScope.launch { ctx.gewohnheitTtsStore.edit { it[KEY_LOOP] = enabled } }
    }

    fun setPauseSeconds(seconds: Int) {
        viewModelScope.launch {
            ctx.gewohnheitTtsStore.edit {
                it[KEY_GEWOHNHEIT_PAUSE_SECONDS] = seconds.coerceIn(PAUSE_RANGE_MIN, PAUSE_RANGE_MAX)
            }
        }
    }

    fun dismissError() {
        errorFlow.value = null
    }

    fun togglePlayback(gewohnheiten: List<Mental>) {
        if (isPlayingFlow.value) {
            stop()
            return
        }
        val texts = gewohnheiten.map { it.text.trim() }.filter { it.isNotEmpty() }
        if (texts.isEmpty()) {
            errorFlow.value = "Keine Gewohnheiten zum Vorlesen vorhanden."
            return
        }
        isPlayingFlow.value = true
        ttsJob =
            viewModelScope.launch {
                val settings = settingsFlow.first()
                try {
                    runSequence(texts, settings.repeatCount, settings.loop, settings.pauseSeconds * 1_000L)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val msg = e.message ?: "Vorlesen fehlgeschlagen"
                    errorFlow.value = msg
                    Diag.e(DiagnosticArea.GOOGLE_TTS, TAG, "Gewohnheit-Vorlesen fehlgeschlagen: $msg", e)
                } finally {
                    ttsPlayer.stop()
                    ttsPlayer.clearSequenceCache()
                    isPlayingFlow.value = false
                }
            }
    }

    fun stop() {
        ttsJob?.cancel()
        ttsJob = null
        ttsPlayer.stop()
        isPlayingFlow.value = false
    }

    private suspend fun runSequence(texts: List<String>, repeat: Int, loop: Boolean, pauseMs: Long) {
        val sequence = buildSequence(texts, repeat)
        if (sequence.isEmpty()) return
        Diag.d(
            DiagnosticArea.GOOGLE_TTS,
            TAG,
            "Sequenz gebildet: ${sequence.size} Saetze (saetze=${texts.size} " +
                "repeat=$repeat loop=$loop pauseMs=$pauseMs)",
        )

        val deadline = SystemClock.elapsedRealtime() + MAX_DURATION_MS
        do {
            for ((index, text) in sequence.withIndex()) {
                currentCoroutineContext().ensureActive()
                if (SystemClock.elapsedRealtime() >= deadline) {
                    Diag.d(
                        DiagnosticArea.GOOGLE_TTS,
                        TAG,
                        "15-Minuten-Grenze erreicht — automatischer Stop",
                    )
                    return
                }
                val file = ttsPlayer.synthesizeToCache(text, forceFresh = true)
                withContext(Dispatchers.Main) { ttsPlayer.playCachedFileAwait(file) }
                val isLastOfRun = index == sequence.lastIndex
                if (!isLastOfRun || loop) {
                    delay(pauseMs)
                }
            }
        } while (loop && SystemClock.elapsedRealtime() < deadline)
    }

    private fun buildSequence(texts: List<String>, repeat: Int): List<String> {
        return buildList {
            for (text in texts) {
                repeat(repeat) { add(text) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsJob?.cancel()
        ttsJob = null
        ttsPlayer.stop()
        ttsPlayer.clearSequenceCache()
    }
}

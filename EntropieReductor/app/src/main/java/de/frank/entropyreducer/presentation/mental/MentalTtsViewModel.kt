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
import java.io.File
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
 * Vorlese-System fuer das Mentalboard (Frank-Wunsch 2026-06-16).
 *
 * Muster (Anker-Schema): Satz 1 ist der "Anker". Vorgelesen wird paarweise — vor JEDEM Folgesatz
 * zuerst der Anker (so oft wie [MentalTtsUiState.ankerCount]), danach der Folgesatz (so oft wie
 * [MentalTtsUiState.folgeCount]). Beispiel 4 Saetze, anker=2, folge=1:
 *   1,1,2 · 1,1,3 · 1,1,4
 * Zwischen JEDEM gesprochenen Satz liegen [PAUSE_MS] (2 Sekunden). Die Reihenfolge ergibt sich
 * direkt aus der aktuellen Sortierung des Mentalboards — sortiert Frank um, aendert sich die
 * Vorlese-Reihenfolge automatisch mit (die Sequenz wird bei jedem Start frisch gebildet).
 *
 * Endlosschleife (gruenes Haekchen): nach einem kompletten Durchgang beginnt die Sequenz von vorne,
 * bis Frank den Lautsprecher erneut drueckt ODER die Sicherheitsgrenze [MAX_DURATION_MS] (15 Minuten)
 * erreicht ist — dann stoppt die App automatisch, damit nicht aus Versehen endlos vorgelesen wird.
 *
 * Kosten/Netz: Folgesaetze werden pro Lauf gecacht. Der Anker (Satz 1) wird dagegen bei JEDEM
 * Auftreten frisch ueber Google-TTS synthetisiert, damit er nicht immer mit exakt derselben
 * Betonung aus derselben MP3-Datei kommt. Beim Stop wird der Sequenz-Cache wieder aufgeraeumt.
 *
 * Threading (Bug-Almanach media3 T1/L1): Der MediaPlayer wird ausschliesslich aus
 * withContext(Dispatchers.Main) bedient; Freigabe deterministisch in [stop]/[onCleared].
 */

/* Eigener kleiner DataStore nur fuer die drei Vorlese-Einstellungen (anker/folge/loop). Bewusst
 * getrennt vom "mental_board"-Store (Saetze) — andere Datei, kein Konflikt. */
private val Context.mentalTtsStore by preferencesDataStore(name = "mental_tts_settings")
private val KEY_ANKER = intPreferencesKey("anker_count")
private val KEY_FOLGE = intPreferencesKey("folge_count")
private val KEY_LOOP = booleanPreferencesKey("loop_enabled")

data class MentalTtsUiState(
    val isPlaying: Boolean = false,
    /** Wie oft der Anker (Satz 1) vor jedem Folgesatz vorgelesen wird (1..10). */
    val ankerCount: Int = 1,
    /** Wie oft jeder Folgesatz vorgelesen wird (1..10). */
    val folgeCount: Int = 1,
    /** Endlosschleife aktiv? */
    val loop: Boolean = false,
    val error: String? = null,
)

private data class SpeechItem(
    val text: String,
    val freshEveryTime: Boolean,
)

@HiltViewModel
class MentalTtsViewModel
@Inject
constructor(
    application: Application,
    private val ttsPlayer: TtsPlayer,
) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "MentalTts"
        const val PAUSE_MS = 2_000L
        const val MAX_DURATION_MS = 15 * 60 * 1_000L // 15 Minuten Sicherheits-Auto-Stop
        const val RANGE_MIN = 1
        const val RANGE_MAX = 10
    }

    private val ctx: Context
        get() = getApplication()

    private val settingsFlow: Flow<Triple<Int, Int, Boolean>> =
        ctx.mentalTtsStore.data.map { p ->
            Triple(
                (p[KEY_ANKER] ?: 1).coerceIn(RANGE_MIN, RANGE_MAX),
                (p[KEY_FOLGE] ?: 1).coerceIn(RANGE_MIN, RANGE_MAX),
                p[KEY_LOOP] ?: false,
            )
        }

    private val isPlayingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MentalTtsUiState> =
        combine(settingsFlow, isPlayingFlow, errorFlow) { (anker, folge, loop), playing, err ->
                MentalTtsUiState(
                    isPlaying = playing,
                    ankerCount = anker,
                    folgeCount = folge,
                    loop = loop,
                    error = err,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MentalTtsUiState(),
            )

    private var ttsJob: Job? = null

    /* ----------------------------- Einstellungen ----------------------------- */

    fun setAnkerCount(n: Int) {
        viewModelScope.launch {
            ctx.mentalTtsStore.edit { it[KEY_ANKER] = n.coerceIn(RANGE_MIN, RANGE_MAX) }
        }
    }

    fun setFolgeCount(n: Int) {
        viewModelScope.launch {
            ctx.mentalTtsStore.edit { it[KEY_FOLGE] = n.coerceIn(RANGE_MIN, RANGE_MAX) }
        }
    }

    fun setLoop(enabled: Boolean) {
        viewModelScope.launch { ctx.mentalTtsStore.edit { it[KEY_LOOP] = enabled } }
    }

    fun dismissError() {
        errorFlow.value = null
    }

    /* ----------------------------- Steuerung ----------------------------- */

    /**
     * Lautsprecher-Druck: laeuft gerade etwas, wird gestoppt (Toggle); sonst startet das Vorlesen
     * der aktuellen Mental-Liste (in der uebergebenen Reihenfolge).
     */
    fun togglePlayback(mentals: List<Mental>) {
        if (isPlayingFlow.value) {
            stop()
            return
        }
        val texts = mentals.map { it.text.trim() }.filter { it.isNotEmpty() }
        if (texts.isEmpty()) {
            errorFlow.value = "Keine Mentals zum Vorlesen vorhanden."
            return
        }
        isPlayingFlow.value = true
        ttsJob =
            viewModelScope.launch {
                val (anker, folge, loop) = settingsFlow.first()
                try {
                    runSequence(texts, anker, folge, loop)
                } catch (e: CancellationException) {
                    throw e // Cancellation NIE verschlucken (Bug-Almanach kotlin §2.1)
                } catch (e: Exception) {
                    val msg = e.message ?: "Vorlesen fehlgeschlagen"
                    errorFlow.value = msg
                    Diag.e(DiagnosticArea.GOOGLE_TTS, TAG, "Mental-Vorlesen fehlgeschlagen: $msg", e)
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

    /* ----------------------------- Vorlese-Schleife ----------------------------- */

    private suspend fun runSequence(texts: List<String>, anker: Int, folge: Int, loop: Boolean) {
        val sequence = buildSequence(texts, anker, folge)
        if (sequence.isEmpty()) return
        Diag.d(
            DiagnosticArea.GOOGLE_TTS,
            TAG,
            "Sequenz gebildet: ${sequence.size} Saetze (saetze=${texts.size} " +
                "anker=$anker folge=$folge loop=$loop)",
        )

        // 1) Prefetch: Folgesaetze nur einmal synthetisieren. Der Anker bleibt bewusst draussen,
        //    weil er pro Auftreten frisch an Google-TTS geschickt werden soll.
        val fileCache = HashMap<String, File>()
        for (text in sequence.filterNot { it.freshEveryTime }.map { it.text }.distinct()) {
            currentCoroutineContext().ensureActive()
            fileCache[text] = ttsPlayer.synthesizeToCache(text)
        }
        Diag.d(
            DiagnosticArea.GOOGLE_TTS,
            TAG,
            "Prefetch fertig: ${fileCache.size} Folgesaetze synthetisiert/gecacht; " +
                "Anker wird pro Auftreten frisch synthetisiert",
        )

        // 2) Wiedergabe mit frischer Anker-Synthese, Loop + 15-Min-Limit.
        val deadline = SystemClock.elapsedRealtime() + MAX_DURATION_MS
        do {
            for ((index, item) in sequence.withIndex()) {
                currentCoroutineContext().ensureActive()
                if (SystemClock.elapsedRealtime() >= deadline) {
                    Diag.d(
                        DiagnosticArea.GOOGLE_TTS,
                        TAG,
                        "15-Minuten-Grenze erreicht — automatischer Stop",
                    )
                    return
                }
                val file =
                    if (item.freshEveryTime) {
                        ttsPlayer.synthesizeToCache(item.text, forceFresh = true)
                    } else {
                        fileCache.getValue(item.text)
                    }
                withContext(Dispatchers.Main) { ttsPlayer.playCachedFileAwait(file) }
                // 2 Sekunden Pause zwischen jedem Satz — auch ueber Schleifen-Grenzen hinweg,
                // nur nach dem allerletzten Satz eines NICHT-Endlos-Laufs nicht.
                val isLastOfRun = index == sequence.lastIndex
                if (!isLastOfRun || loop) {
                    delay(PAUSE_MS)
                }
            }
        } while (loop && SystemClock.elapsedRealtime() < deadline)
    }

    /**
     * Baut die flache Vorlese-Sequenz aus den Saetzen. Bei nur einem Satz wird dieser eine Satz
     * [anker]-mal vorgelesen (es gibt keinen Folgesatz).
     */
    private fun buildSequence(texts: List<String>, anker: Int, folge: Int): List<SpeechItem> {
        if (texts.isEmpty()) return emptyList()
        val first = texts.first()
        if (texts.size == 1) return List(anker) { SpeechItem(first, freshEveryTime = true) }
        return buildList {
            for (i in 1 until texts.size) {
                repeat(anker) { add(SpeechItem(first, freshEveryTime = true)) }
                repeat(folge) { add(SpeechItem(texts[i], freshEveryTime = false)) }
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

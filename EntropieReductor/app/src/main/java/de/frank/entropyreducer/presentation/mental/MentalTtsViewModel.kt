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
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.tts.TtsUsage
import de.frank.entropyreducer.data.tts.TtsUsageStore
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
 * Vorlese-System fuer das Mentalboard (Frank-Wunsch 2026-06-16).
 *
 * Muster (Anker-Schema): Satz 1 ist der "Anker". Vorgelesen wird paarweise — vor JEDEM Folgesatz
 * zuerst der Anker (so oft wie [MentalTtsUiState.ankerCount]), danach der Folgesatz (so oft wie
 * [MentalTtsUiState.folgeCount]). Beispiel 4 Saetze, anker=2, folge=1:
 *   1,1,2 · 1,1,3 · 1,1,4
 * Zwischen JEDEM gesprochenen Satz liegen [PAUSE_MS] (9 Sekunden). Die Reihenfolge ergibt sich
 * direkt aus der aktuellen Sortierung des Mentalboards — sortiert Frank um, aendert sich die
 * Vorlese-Reihenfolge automatisch mit (die Sequenz wird bei jedem Start frisch gebildet).
 *
 * Endlosschleife (gruenes Haekchen): nach einem kompletten Durchgang beginnt die Sequenz von vorne,
 * bis Frank den Lautsprecher erneut drueckt ODER die globale Sicherheitsgrenze aus den Vorlesen-
 * Einstellungen erreicht ist — dann stoppt die App automatisch, damit nicht aus Versehen endlos
 * vorgelesen wird.
 *
 * Gewohnheiten mitlesen ("G"-Haekchen, Frank-Wunsch 2026-07-03): Ist [MentalTtsUiState.includeHabits]
 * aktiv, werden die Gewohnheiten NACH dem Mentalblock angehaengt — in der Vorleseweise des
 * Gewohnheit-Reiters (jeder Satz [gewohnheitRepeat]-mal, aus dem gemeinsamen gewohnheit_tts_settings-
 * Store). Der Mental-Loop wiederholt dann Mentalblock UND Gewohnheiten gemeinsam.
 *
 * Kosten/Netz: Jede Sequenzposition wird frisch ueber Google-TTS synthetisiert. Dadurch bekommt
 * auch jede Wiederholung desselben Satzes eine eigene Betonung statt exakt dieselbe MP3-Datei.
 * Beim Stop wird der Sequenz-Cache wieder aufgeraeumt.
 *
 * Threading (Bug-Almanach media3 T1/L1): Der MediaPlayer wird ausschliesslich aus
 * withContext(Dispatchers.Main) bedient; Freigabe deterministisch in [stop]/[onCleared].
 */

/* Eigener kleiner DataStore nur fuer die Vorlese-Einstellungen (anker/folge/loop/G). Bewusst
 * getrennt vom "mental_board"-Store (Saetze) — andere Datei, kein Konflikt. */
private val Context.mentalTtsStore by preferencesDataStore(name = "mental_tts_settings")
private val KEY_ANKER = intPreferencesKey("anker_count")
private val KEY_FOLGE = intPreferencesKey("folge_count")
private val KEY_LOOP = booleanPreferencesKey("loop_enabled")
private val KEY_INCLUDE_HABITS = booleanPreferencesKey("include_habits")
private val KEY_MENTAL_PAUSE_SECONDS = intPreferencesKey("pause_seconds")

data class MentalTtsUiState(
    val isPlaying: Boolean = false,
    /** Wie oft der Anker (Satz 1) vor jedem Folgesatz vorgelesen wird (1..10). */
    val ankerCount: Int = 1,
    /** Wie oft jeder Folgesatz vorgelesen wird (1..10). */
    val folgeCount: Int = 1,
    /** Endlosschleife aktiv? */
    val loop: Boolean = false,
    /** "G"-Haekchen: Gewohnheiten am Ende mitlesen? */
    val includeHabits: Boolean = false,
    /** Pause zwischen zwei gesprochenen Mental-Saetzen (1..30 Sekunden). */
    val pauseSeconds: Int = 9,
    /** Globale Sicherheitsgrenze fuer automatischen Vorlese-Stop (15..120 Minuten). */
    val autoStopMinutes: Int = AppSettings.DEFAULT_TTS_AUTO_STOP_MINUTES,
    val error: String? = null,
)

/** Interne Momentaufnahme der vier persistierten Vorlese-Einstellungen. */
private data class MentalSettings(
    val anker: Int,
    val folge: Int,
    val loop: Boolean,
    val includeHabits: Boolean,
    val pauseSeconds: Int,
)

@HiltViewModel
class MentalTtsViewModel
@Inject
    constructor(
        application: Application,
        private val ttsPlayer: TtsPlayer,
        private val appSettings: AppSettings,
        usageStore: TtsUsageStore,
    ) : AndroidViewModel(application) {

    /** Live-Monatsverbrauch des TTS-Kontingents (Anzeige unter dem letzten Satz im Mentalboard). */
    val usage: StateFlow<TtsUsage> = usageStore.usage

    private companion object {
        const val TAG = "MentalTts"
        const val DEFAULT_PAUSE_SECONDS = 9
        const val RANGE_MIN = 1
        const val RANGE_MAX = 10
        const val PAUSE_RANGE_MIN = 1
        const val PAUSE_RANGE_MAX = 30
    }

    private val ctx: Context
        get() = getApplication()

    private val settingsFlow: Flow<MentalSettings> =
        ctx.mentalTtsStore.data.map { p ->
            MentalSettings(
                anker = (p[KEY_ANKER] ?: 1).coerceIn(RANGE_MIN, RANGE_MAX),
                folge = (p[KEY_FOLGE] ?: 1).coerceIn(RANGE_MIN, RANGE_MAX),
                loop = p[KEY_LOOP] ?: false,
                includeHabits = p[KEY_INCLUDE_HABITS] ?: false,
                pauseSeconds = (p[KEY_MENTAL_PAUSE_SECONDS] ?: DEFAULT_PAUSE_SECONDS)
                    .coerceIn(PAUSE_RANGE_MIN, PAUSE_RANGE_MAX),
            )
        }

    private val isPlayingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MentalTtsUiState> =
        combine(settingsFlow, appSettings.ttsAutoStopMinutesFlow, isPlayingFlow, errorFlow) { s, autoStopMinutes, playing, err ->
                MentalTtsUiState(
                    isPlaying = playing,
                    ankerCount = s.anker,
                    folgeCount = s.folge,
                    loop = s.loop,
                    includeHabits = s.includeHabits,
                    pauseSeconds = s.pauseSeconds,
                    autoStopMinutes = autoStopMinutes,
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

    /** "G"-Haekchen: Gewohnheiten am Ende mitlesen. */
    fun setIncludeHabits(enabled: Boolean) {
        viewModelScope.launch { ctx.mentalTtsStore.edit { it[KEY_INCLUDE_HABITS] = enabled } }
    }

    fun setPauseSeconds(seconds: Int) {
        viewModelScope.launch {
            ctx.mentalTtsStore.edit {
                it[KEY_MENTAL_PAUSE_SECONDS] = seconds.coerceIn(PAUSE_RANGE_MIN, PAUSE_RANGE_MAX)
            }
        }
    }

    fun setAutoStopMinutes(minutes: Int) {
        viewModelScope.launch { appSettings.setTtsAutoStopMinutes(minutes) }
    }

    fun dismissError() {
        errorFlow.value = null
    }

    /* ----------------------------- Steuerung ----------------------------- */

    /**
     * Lautsprecher-Druck: laeuft gerade etwas, wird gestoppt (Toggle); sonst startet das Vorlesen
     * der aktuellen Mental-Liste (in der uebergebenen Reihenfolge). Ist das "G"-Haekchen aktiv,
     * werden die uebergebenen [gewohnheiten] am Ende mitgelesen.
     */
    fun togglePlayback(mentals: List<Mental>, gewohnheiten: List<Mental> = emptyList()) {
        if (isPlayingFlow.value) {
            stop()
            return
        }
        val mentalTexts = mentals.map { it.text.trim() }.filter { it.isNotEmpty() }
        if (mentalTexts.isEmpty()) {
            errorFlow.value = "Keine Mentals zum Vorlesen vorhanden."
            return
        }
        isPlayingFlow.value = true
        ttsJob =
            viewModelScope.launch {
                val s = settingsFlow.first()
                val autoStopMinutes = appSettings.ttsAutoStopMinutesFlow.first()
                // Gewohnheiten nur anhaengen, wenn das "G"-Haekchen aktiv ist. Ihre "wie-oft pro
                // Satz"-Zahl kommt aus dem Gewohnheit-Reiter (gemeinsamer DataStore) — die
                // Gesamt-Wiederholung steuert allein der Mental-Loop.
                val gewohnheitTexts =
                    if (s.includeHabits) {
                        gewohnheiten.map { it.text.trim() }.filter { it.isNotEmpty() }
                    } else {
                        emptyList()
                    }
                val gewohnheitSettings =
                    if (s.includeHabits && gewohnheitTexts.isNotEmpty()) {
                        ctx.gewohnheitTtsStore.data
                            .map {
                                Pair(
                                    (it[KEY_REPEAT] ?: 1).coerceIn(RANGE_MIN, RANGE_MAX),
                                    (it[KEY_GEWOHNHEIT_PAUSE_SECONDS] ?: DEFAULT_PAUSE_SECONDS)
                                        .coerceIn(PAUSE_RANGE_MIN, PAUSE_RANGE_MAX),
                                )
                            }
                            .first()
                    } else {
                        Pair(1, DEFAULT_PAUSE_SECONDS)
                    }
                try {
                    runSequence(
                        mentalTexts = mentalTexts,
                        anker = s.anker,
                        folge = s.folge,
                        loop = s.loop,
                        gewohnheitTexts = gewohnheitTexts,
                        gewohnheitRepeat = gewohnheitSettings.first,
                        mentalPauseMs = s.pauseSeconds * 1_000L,
                        gewohnheitPauseMs = gewohnheitSettings.second * 1_000L,
                        autoStopMs = autoStopMinutes * 60 * 1_000L,
                    )
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

    private suspend fun runSequence(
        mentalTexts: List<String>,
        anker: Int,
        folge: Int,
        loop: Boolean,
        gewohnheitTexts: List<String>,
        gewohnheitRepeat: Int,
        mentalPauseMs: Long,
        gewohnheitPauseMs: Long,
        autoStopMs: Long,
    ) {
        // Mentalblock (Anker-Schema) + optional Gewohnheiten am Ende (jeder Satz gewohnheitRepeat-mal).
        val mentalSeq = buildSequence(mentalTexts, anker, folge).map { SpokenStep(it, mentalPauseMs) }
        val gewohnheitSeq = buildHabitSequence(gewohnheitTexts, gewohnheitRepeat).map { SpokenStep(it, gewohnheitPauseMs) }
        val sequence = mentalSeq + gewohnheitSeq
        if (sequence.isEmpty()) return
        Diag.d(
            DiagnosticArea.GOOGLE_TTS,
            TAG,
                "Sequenz gebildet: mental=${mentalSeq.size} + gewohnheit=${gewohnheitSeq.size} " +
                "(mentals=${mentalTexts.size} anker=$anker folge=$folge loop=$loop, " +
                "mentalPauseMs=$mentalPauseMs gewohnheiten=${gewohnheitTexts.size} " +
                "repeat=$gewohnheitRepeat gewohnheitPauseMs=$gewohnheitPauseMs)",
        )

        // Wiedergabe mit frischer Synthese pro Satzvorkommen, Loop + einstellbarem Sicherheitslimit.
        val deadline = SystemClock.elapsedRealtime() + autoStopMs
        do {
            for ((index, step) in sequence.withIndex()) {
                currentCoroutineContext().ensureActive()
                if (SystemClock.elapsedRealtime() >= deadline) {
                    Diag.d(
                        DiagnosticArea.GOOGLE_TTS,
                        TAG,
                        "${autoStopMs / 60_000}-Minuten-Grenze erreicht — automatischer Stop",
                    )
                    return
                }
                val file = ttsPlayer.synthesizeToCache(step.text, forceFresh = true)
                withContext(Dispatchers.Main) { ttsPlayer.playCachedFileAwait(file) }
                val isLastOfRun = index == sequence.lastIndex
                if (!isLastOfRun || loop) {
                    delay(step.pauseMs)
                }
            }
        } while (loop && SystemClock.elapsedRealtime() < deadline)
    }

    private data class SpokenStep(val text: String, val pauseMs: Long)

    /**
     * Baut die flache Mental-Vorlese-Sequenz aus den Saetzen. Bei nur einem Satz wird dieser eine Satz
     * [anker]-mal vorgelesen (es gibt keinen Folgesatz).
     */
    private fun buildSequence(texts: List<String>, anker: Int, folge: Int): List<String> {
        if (texts.isEmpty()) return emptyList()
        val first = texts.first()
        if (texts.size == 1) return List(anker) { first }
        return buildList {
            for (i in 1 until texts.size) {
                repeat(anker) { add(first) }
                repeat(folge) { add(texts[i]) }
            }
        }
    }

    /** Baut die Gewohnheit-Sequenz: jeder Satz [times]-mal hintereinander (Gewohnheit-Reiter-Weise). */
    private fun buildHabitSequence(texts: List<String>, times: Int): List<String> {
        if (texts.isEmpty()) return emptyList()
        return buildList {
            for (text in texts) {
                repeat(times) { add(text) }
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

package de.frank.entropyreducer.presentation.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDao
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorEntryEntity
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorFollowupEntity
import de.frank.entropyreducer.domain.tts.TtsPlayer
import de.frank.entropyreducer.domain.tts.TtsResult
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Sicher unter dem Google-TTS-Limit (~5000 Zeichen/Anfrage). Laengere Abschnitte
// werden an Absatz-/Satzgrenzen in mehrere Stuecke geteilt und sequentiell gelesen.
private const val MAX_TTS_CHARS = 3500

enum class JournalTtsState {
    IDLE,
    LOADING,
    SPEAKING,
}

data class JournalDetailState(
    val entry: JournalMirrorEntryEntity? = null,
    val followups: List<JournalMirrorFollowupEntity> = emptyList(),
    val loaded: Boolean = false,
    val ttsState: JournalTtsState = JournalTtsState.IDLE,
)

@HiltViewModel
class JournalEntryDetailViewModel
@Inject
constructor(
    savedState: SavedStateHandle,
    private val dao: JournalMirrorDao,
    private val tts: TtsPlayer,
) : ViewModel() {

    private val sourceId: Long = savedState.get<String>("sourceId")?.toLongOrNull() ?: -1L
    private val _state = MutableStateFlow(JournalDetailState())
    val state: StateFlow<JournalDetailState> = _state.asStateFlow()

    // Job der laufenden Vorlese-Synthese, damit Stop auch WAEHREND des Ladens (vor der
    // Wiedergabe) zuverlaessig greift und keine verspaetete Wiedergabe mehr startet.
    private var speakJob: Job? = null

    init {
        viewModelScope.launch {
            val entry = dao.getEntry(sourceId)
            val followups = if (entry != null) dao.followupsFor(sourceId) else emptyList()
            _state.value = JournalDetailState(entry = entry, followups = followups, loaded = true)
        }
    }

    /**
     * Liest Eintrag (Titel + Text) + alle Nachtraege abschnittsweise vor: ein Abschnitt
     * wird gesprochen, nach dessen Ende automatisch der naechste — so wie BestJournal Frank
     * es bei langen Rueckblicken macht (Muster aus TtsManager). Kein Abschneiden mehr; sehr
     * lange Texte werden an Absatz- und Satzgrenzen in TTS-vertraegliche Stuecke geteilt.
     * Erneutes Tippen oder Stop bricht die Kette sauber ab (Job-Cancel + tts.stop()).
     */
    fun speak(showImproved: Boolean) {
        val s = _state.value
        if (s.ttsState != JournalTtsState.IDLE) {
            stopSpeaking()
            return
        }
        val entry = s.entry ?: return
        val chunks = buildSpeechChunks(entry, s.followups, showImproved)
        if (chunks.isEmpty()) return

        speakJob?.cancel()
        speakJob =
            viewModelScope.launch {
                _state.value = _state.value.copy(ttsState = JournalTtsState.LOADING)
                var started = false
                for (chunk in chunks) {
                    if (!isActive) break
                    val done = CompletableDeferred<Unit>()
                    val result =
                        tts.speak(
                            text = chunk,
                            onPlaybackStart = {
                                if (!started) {
                                    started = true
                                    _state.value =
                                        _state.value.copy(ttsState = JournalTtsState.SPEAKING)
                                }
                            },
                            // Nach jedem Abschnitt geht es zum naechsten weiter.
                            onComplete = { if (!done.isCompleted) done.complete(Unit) },
                            // Bei Fehler diesen Abschnitt ueberspringen (Kette nicht abreissen).
                            onError = { if (!done.isCompleted) done.complete(Unit) },
                        )
                    // Setup-Fehler (z.B. kein API-Key) -> ganze Kette beenden.
                    if (result is TtsResult.Error) break
                    done.await()
                }
                if (isActive) _state.value = _state.value.copy(ttsState = JournalTtsState.IDLE)
            }
    }

    /**
     * Baut die geordnete Abschnittsliste (Titel+Text als erster Block, dann jeder Nachtrag)
     * und teilt zu lange Bloecke an Absatz-/Satzgrenzen in TTS-vertraegliche Stuecke.
     */
    private fun buildSpeechChunks(
        entry: JournalMirrorEntryEntity,
        followups: List<JournalMirrorFollowupEntity>,
        showImproved: Boolean,
    ): List<String> {
        val sections = buildList {
            val head = buildString {
                entry.title?.takeIf { it.isNotBlank() }?.let { append(it).append(". ") }
                // Vorlesen == Anzeige: dieselbe Variante wie der angewählte Tab.
                val mainImproved = entry.improvedText
                append(
                    if (showImproved && !mainImproved.isNullOrBlank()) mainImproved
                    else entry.displayText
                )
            }
            add(head)
            followups.forEachIndexed { i, f ->
                val fImproved = f.improvedText
                val txt = if (showImproved && !fImproved.isNullOrBlank()) fImproved else f.text
                add("Nachtrag ${germanOrdinal(i + 1)}. $txt")
            }
        }
        return sections.flatMap { splitForTts(it) }.filter { it.isNotBlank() }
    }

    /** Teilt einen Abschnitt in Stuecke <= MAX_TTS_CHARS an Absatz- dann Satzgrenzen. */
    private fun splitForTts(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.length <= MAX_TTS_CHARS) return listOf(trimmed)
        val out = mutableListOf<String>()
        val buffer = StringBuilder()
        // Erst an Absaetzen (Leerzeilen), dann an Satzenden splitten.
        val paragraphs = trimmed.split(Regex("(\\r?\\n){2,}"))
        for (para in paragraphs) {
            val sentences = para.split(Regex("(?<=[.!?])\\s+"))
            for (sentence in sentences) {
                val piece = sentence.trim()
                if (piece.isEmpty()) continue
                when {
                    piece.length > MAX_TTS_CHARS -> {
                        // Einzelner uebergrosser Satz: hart in MAX-Stuecke schneiden.
                        if (buffer.isNotBlank()) {
                            out.add(buffer.toString().trim())
                            buffer.clear()
                        }
                        piece.chunked(MAX_TTS_CHARS).forEach { out.add(it) }
                    }
                    buffer.length + piece.length + 1 > MAX_TTS_CHARS -> {
                        out.add(buffer.toString().trim())
                        buffer.clear()
                        buffer.append(piece)
                    }
                    else -> {
                        if (buffer.isNotEmpty()) buffer.append(' ')
                        buffer.append(piece)
                    }
                }
            }
        }
        if (buffer.isNotBlank()) out.add(buffer.toString().trim())
        return out
    }

    private fun germanOrdinal(n: Int): String =
        when (n) {
            1 -> "Eins"
            2 -> "Zwei"
            3 -> "Drei"
            4 -> "Vier"
            5 -> "Fünf"
            6 -> "Sechs"
            7 -> "Sieben"
            8 -> "Acht"
            9 -> "Neun"
            10 -> "Zehn"
            else -> "$n"
        }

    fun stopSpeaking() {
        speakJob?.cancel()
        speakJob = null
        tts.stop()
        _state.value = _state.value.copy(ttsState = JournalTtsState.IDLE)
    }

    override fun onCleared() {
        speakJob?.cancel()
        tts.stop()
        super.onCleared()
    }
}

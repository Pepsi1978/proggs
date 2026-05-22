package de.frank.entropyreducer.presentation.dashboard1.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.dao.EntropyEntryFollowupDao
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryFollowupEntity
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.tts.TtsPlayer
import de.frank.entropyreducer.domain.usecase.ProcessEntryUseCase
import de.frank.entropyreducer.domain.tts.TtsResult
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Status der Vorlesefunktion. Wird in der UI als visueller Hinweis genutzt (Stop-Icon statt
 * Lautsprecher) und um die Mediaplayer-Sitzung sauber zu stoppen wenn der Screen verlassen wird.
 */
enum class TtsState {
    IDLE,
    LOADING,
    SPEAKING,
}

/**
 * UI-State des Detail-Screens. Wird aus drei Quellen kombiniert:
 * 1. EntryRepository (Flow auf den Eintrag — reagiert auf alle Update-Operationen der
 *    TasksViewModel/Repository, z.B. wenn ein Nachtrag den Eintrag neu bewertet)
 * 2. EntropyEntryFollowupDao (Flow auf die Nachträge dieses Eintrags)
 * 3. Lokale UI-States (TTS, Fehler, Verarbeitungs-Indikatoren)
 */
data class EntryDetailUiState(
    val entry: EntropyEntryEntity? = null,
    val followups: List<EntropyEntryFollowupEntity> = emptyList(),
    val ttsState: TtsState = TtsState.IDLE,
    val isAddingFollowup: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false,
)

/**
 * ViewModel für den neuen Vollbild-Detail-Screen einer Entropie-Aufgabe (Frank-Wunsch 2026-05-20).
 * Die Funktionalität ist 1:1 inspiriert von BestJournalFrank's EntryDetailScreen:
 * - Zusammenfassung (hier: Title + Description als Hero-Karte)
 * - Status-Buttons
 * - Nachträge als eigene Karten mit eigener Liste in DB
 * - Vorlesefunktion (TTS) über Title + Description + alle Nachträge
 * - Löschen
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class EntryDetailViewModel
@Inject
constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val entries: EntryRepository,
    private val followupDao: EntropyEntryFollowupDao,
    private val ttsPlayer: TtsPlayer,
    private val process: ProcessEntryUseCase,
) : AndroidViewModel(application) {

    /**
     * Die entryId wird über NavArgument injiziert. Wenn null oder leer, zeigt der Screen eine leere
     * Hülle — passiert nur bei kaputtem Deep-Link.
     */
    private val entryId: String = savedStateHandle.get<String>("entryId") ?: ""

    private val entryIdFlow = MutableStateFlow(entryId)

    /**
     * Reaktiver Flow auf den Eintrag. Wenn der Eintrag aus der DB verschwindet (z.B. nach Löschen),
     * liefert der Flow null — die UI navigiert dann zurück.
     */
    private val entryFlow: kotlinx.coroutines.flow.Flow<EntropyEntryEntity?> =
        entryIdFlow.flatMapLatest { id ->
            if (id.isBlank()) flowOf<EntropyEntryEntity?>(null)
            else
                kotlinx.coroutines.flow.flow<EntropyEntryEntity?> {
                    // Re-fetchen bei jeder externen Modifikation. Wir pollen nicht —
                    // stattdessen reicht ein Single-Read pro Trigger; spätere Updates
                    // kommen über reloadTrigger.
                    emit(entries.get(id))
                    reloadTrigger.collect { emit(entries.get(id)) }
                }
        }

    /** Trigger der ein Re-Read des Eintrags auslöst (nach setStatus etc.). */
    private val reloadTrigger = MutableStateFlow(0L)

    private val followupsFlow = entryIdFlow.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else followupDao.observeByEntryId(id)
    }

    private val ttsStateFlow = MutableStateFlow(TtsState.IDLE)
    private val addingFollowupFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)
    private val deletedFlow = MutableStateFlow(false)

    val uiState: StateFlow<EntryDetailUiState> =
        combine(
                entryFlow,
                followupsFlow,
                ttsStateFlow,
                addingFollowupFlow,
                combine(errorFlow, deletedFlow) { e, d -> e to d },
            ) { entry, followups, tts, adding, errorDeleted ->
                val (error, deleted) = errorDeleted
                EntryDetailUiState(
                    entry = entry,
                    followups = followups,
                    ttsState = tts,
                    isAddingFollowup = adding,
                    error = error,
                    isDeleted = deleted,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = EntryDetailUiState(),
            )

    private var ttsJob: Job? = null

    /** Setzt den Eintrags-Status (OFFEN/IN_ARBEIT/REDUZIERT/ARCHIVIERT). */
    fun setStatus(status: EntryStatus) {
        viewModelScope.launch {
            val current = entries.get(entryId) ?: return@launch
            val now = System.currentTimeMillis()
            entries.update(
                current.copy(
                    status = status,
                    resolvedAt =
                        if (status == EntryStatus.REDUZIERT || status == EntryStatus.ARCHIVIERT) now
                        else null,
                    updatedAt = now,
                )
            )
            reloadTrigger.value = now
        }
    }

    /** Löscht den Eintrag (Nachträge fliegen per ForeignKey CASCADE automatisch mit). */
    fun deleteEntry() {
        viewModelScope.launch {
            val current = entries.get(entryId) ?: return@launch
            entries.delete(current)
            stopTts()
            deletedFlow.value = true
        }
    }

    /**
     * Speichert einen neuen Nachtrag. Der Whisper-Mic-Button liefert den Transkript-String; wir
     * bauen ein FollowupEntity und legen es ab.
     *
     * Frank-Wunsch 2026-05-22: Direkt nach dem Speichern wird der Eintrag neu bewertet —
     * der Nachtrag fliesst in priorityScore + priorityReason ein. So muss Frank nicht
     * extra eine "neu bewerten"-Aktion triggern.
     */
    fun addFollowup(transcript: String) {
        val text = transcript.trim()
        if (text.isBlank() || entryId.isBlank()) return
        viewModelScope.launch {
            addingFollowupFlow.value = true
            try {
                val now = System.currentTimeMillis()
                followupDao.upsert(
                    EntropyEntryFollowupEntity(
                        id = UUID.randomUUID().toString(),
                        entryId = entryId,
                        rawText = text,
                        improvedText = null,
                        isImproved = false,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
                rescoreWithCurrentFollowups()
            } catch (e: Exception) {
                errorFlow.value = "Nachtrag konnte nicht gespeichert werden: ${e.message}"
            } finally {
                addingFollowupFlow.value = false
            }
        }
    }

    /**
     * Sammelt alle Followups des aktuellen Eintrags (chronologisch) und triggert die
     * Gemini-Neubewertung mit Nachtrags-Kontext. Bei Fehler nur leise loggen — der
     * Nachtrag selbst ist ja schon gespeichert und der Eintrag bleibt funktional.
     */
    private suspend fun rescoreWithCurrentFollowups() {
        val current = entries.get(entryId) ?: return
        val allFollowups = followupDao.getByEntryId(entryId)
        val followupTexts = allFollowups
            .sortedBy { it.createdAt }
            .map { it.improvedText?.takeIf { _ -> it.isImproved } ?: it.rawText }
            .filter { it.isNotBlank() }
        process.rescoreExisting(current, followupTexts)
            .onSuccess { reloadTrigger.value = System.currentTimeMillis() }
            .onFailure { ex ->
                android.util.Log.w(
                    "EntryDetailVM",
                    "Rescore nach Nachtrag fehlgeschlagen: ${ex.message}",
                )
            }
    }

    /**
     * Setzt die geschaetzte Zeitspanne (in Minuten) fuer den Eintrag. null = unbestimmt
     * (Default). Wird vom Briefing genutzt um zu pruefen ob die Aufgaben des Tages
     * ueberhaupt in die verfuegbare Zeit passen.
     *
     * Frank-Wunsch 2026-05-22 (zweite Iteration): durationManuallySet wird mitgesetzt,
     * damit nachfolgende Rescores den manuellen Wert NICHT mehr ueberschreiben.
     * null = Loeschen → Flag zurueck auf false → KI darf wieder schaetzen.
     */
    fun setEstimatedDuration(minutes: Int?) {
        viewModelScope.launch {
            val current = entries.get(entryId) ?: return@launch
            entries.update(
                current.copy(
                    estimatedDurationMinutes = minutes,
                    durationManuallySet = minutes != null,
                    updatedAt = System.currentTimeMillis(),
                )
            )
            reloadTrigger.value = System.currentTimeMillis()
        }
    }

    /**
     * Frank-Wunsch 2026-05-22 (dritte Iteration): Frist setzen oder loeschen. null
     * = keine Frist mehr. Nach jeder Aenderung wird der Eintrag neu bewertet, weil
     * die Frist direkt in die Prio-Berechnung einfliesst (kurze Restzeit = hoehere
     * Prio, < 24h = mindestens 95).
     */
    fun setDueDate(epochMs: Long?) {
        viewModelScope.launch {
            val current = entries.get(entryId) ?: return@launch
            entries.update(
                current.copy(
                    dueAtMs = epochMs,
                    updatedAt = System.currentTimeMillis(),
                )
            )
            reloadTrigger.value = System.currentTimeMillis()
            // Frist-Aenderung soll sofort in die Prio einfliessen — Followups
            // mit anhaengen falls vorhanden.
            rescoreWithCurrentFollowups()
        }
    }

    /** Aktualisiert den editierten Text eines Nachtrags (Inline-Edit). */
    fun updateFollowupText(id: String, newText: String) {
        viewModelScope.launch {
            val current = followupDao.getById(id) ?: return@launch
            followupDao.update(
                current.copy(rawText = newText, updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun deleteFollowup(id: String) {
        viewModelScope.launch { followupDao.deleteById(id) }
    }

    /**
     * Spricht den Eintrag plus alle Nachträge der Reihe nach vor. Der TtsPlayer arbeitet
     * single-shot pro speak() — wir konkatenieren also alles vorher zu einem zusammenhängenden Text
     * mit Pausen-Markierungen ("...").
     */
    fun speakAll() {
        val state = uiState.value
        val entry = state.entry ?: return
        // Wenn gerade läuft → stoppen statt neu starten.
        if (state.ttsState != TtsState.IDLE) {
            stopTts()
            return
        }
        val parts = buildList {
            add(entry.title)
            if (entry.description.isNotBlank()) add(entry.description)
            state.followups.forEachIndexed { index, followup ->
                add("Nachtrag ${germanOrdinal(index + 1)}")
                add(followup.improvedText?.takeIf { followup.isImproved } ?: followup.rawText)
            }
        }
        val spokenText = parts.joinToString(separator = ". ").take(4500)

        ttsStateFlow.value = TtsState.LOADING
        ttsJob = viewModelScope.launch {
            val result =
                ttsPlayer.speak(
                    text = spokenText,
                    onPlaybackStart = { ttsStateFlow.value = TtsState.SPEAKING },
                    onComplete = { ttsStateFlow.value = TtsState.IDLE },
                    onError = {
                        ttsStateFlow.value = TtsState.IDLE
                        errorFlow.value = "Vorlesen fehlgeschlagen: ${it.message}"
                    },
                )
            if (result is TtsResult.Error) {
                ttsStateFlow.value = TtsState.IDLE
                errorFlow.value = result.message
            }
        }
    }

    fun stopTts() {
        ttsPlayer.stop()
        ttsJob?.cancel()
        ttsJob = null
        ttsStateFlow.value = TtsState.IDLE
    }

    fun dismissError() {
        errorFlow.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTts()
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
}

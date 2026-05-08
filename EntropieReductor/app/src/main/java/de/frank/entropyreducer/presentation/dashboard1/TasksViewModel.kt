package de.frank.entropyreducer.presentation.dashboard1

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.audio.AudioRecorder
import de.frank.entropyreducer.data.audio.RecordingService
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.KiQuestionRepository
import de.frank.entropyreducer.domain.kiquestion.KiQuestion
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.domain.status.StatusBreakdown
import de.frank.entropyreducer.domain.status.StatusObserver
import de.frank.entropyreducer.domain.usecase.CalculateBucketsUseCase
import de.frank.entropyreducer.domain.usecase.ProcessEntryUseCase
import de.frank.entropyreducer.domain.usecase.TranscribeAudioUseCase
import de.frank.entropyreducer.presentation.components.MicState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State der Aufgaben-Ansicht.
 */
data class TasksUiState(
    val entriesByBucket: Map<TimeBucket, List<EntropyEntryEntity>> = emptyMap(),
    val activeCategories: Set<EntropyCategory> = emptySet(),
    val statusPercent: Int = 50,
    val statusBreakdown: StatusBreakdown? = null,
    val openCount: Int = 0,
    val micState: MicState = MicState.IDLE,
    val processingMessage: String? = null,
    val errorMessage: String? = null,
    val recentlyCreatedId: String? = null,
    val kiQuestion: KiQuestion? = null,
    /** Eintrag der aktuell im Detail-Bottom-Sheet angezeigt wird (null = geschlossen). */
    val detailEntry: EntropyEntryEntity? = null,
    /** Erledigt-Bucket — die letzten REDUZIERT/ARCHIVIERT-Eintraege, sortiert nach
     *  resolvedAt absteigend. Werden separat unter den aktiven Bucket-Sektionen
     *  als ausgegrauter Block angezeigt. */
    val resolvedEntries: List<EntropyEntryEntity> = emptyList(),
    /** Proaktiver Forscher: Eintrag für den gerade nach der Loesungsmethode
     *  gefragt wird. null = kein Dialog sichtbar (Frank-Wunsch 2026-05-08). */
    val pendingMethodFor: EntropyEntryEntity? = null,
)

private data class UiOnlyState(
    val micState: MicState = MicState.IDLE,
    val processingMessage: String? = null,
    val errorMessage: String? = null,
    val recentlyCreatedId: String? = null,
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    application: Application,
    private val entries: EntryRepository,
    private val recorder: AudioRecorder,
    private val transcribe: TranscribeAudioUseCase,
    private val process: ProcessEntryUseCase,
    private val statusObserver: StatusObserver,
    private val kiQuestions: KiQuestionRepository,
    private val generateKiQuestion: de.frank.entropyreducer.domain.kiquestion.GenerateKiQuestionUseCase,
    @Suppress("unused") private val bucketingUseCase: CalculateBucketsUseCase,
) : AndroidViewModel(application) {

    private val activeCategoriesFlow = MutableStateFlow<Set<EntropyCategory>>(emptySet())
    private val uiOnlyFlow = MutableStateFlow(UiOnlyState())
    private val detailEntryIdFlow = MutableStateFlow<String?>(null)
    private val pendingMethodForFlow = MutableStateFlow<EntropyEntryEntity?>(null)

    val state: StateFlow<TasksUiState> = combine(
        entries.getActive(),
        activeCategoriesFlow,
        uiOnlyFlow,
        combine(statusObserver.observe(), kiQuestions.currentQuestion) { b, q -> b to q },
        combine(detailEntryIdFlow, pendingMethodForFlow) { d, p -> d to p },
    ) { list, cats, ui, (breakdown, question), (detailId, pendingMethod) ->
        val filtered = if (cats.isEmpty()) list else list.filter { it.category in cats }
        // Aktive Eintraege (OFFEN + IN_ARBEIT) → Bucket-Gruppierung
        val activeList = filtered.filter { it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT }
        val grouped = activeList.groupBy { it.timeBucket }
        // Erledigt-Bucket (REDUZIERT) — sortiert nach resolvedAt absteigend (neueste zuerst).
        val resolvedList = filtered
            .filter { it.status == EntryStatus.REDUZIERT }
            .sortedByDescending { it.resolvedAt ?: it.updatedAt }
            .take(20)
        val openCount = list.count { it.status == EntryStatus.OFFEN }
        val detail = detailId?.let { id -> list.firstOrNull { it.id == id } }
        TasksUiState(
            entriesByBucket = grouped,
            activeCategories = cats,
            statusPercent = breakdown.total,
            statusBreakdown = breakdown,
            openCount = openCount,
            micState = ui.micState,
            processingMessage = ui.processingMessage,
            errorMessage = ui.errorMessage,
            recentlyCreatedId = ui.recentlyCreatedId,
            kiQuestion = question,
            detailEntry = detail,
            resolvedEntries = resolvedList,
            pendingMethodFor = pendingMethod,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    init {
        // KI-Frage des Moments dynamisch generieren beim ViewModel-Start
        // (Frank-Wunsch 2026-05-08: keine vordefinierten Standardfragen,
        // immer durch Gemini aus dem aktuellen Kontext gebildet).
        refreshKiQuestion()
    }

    /** Generiert die KI-Frage neu durch Gemini-API mit allen offenen Eintraegen
     *  als Kontext. Fallback auf statisch wenn kein API-Key vorhanden.
     *  Frank-Wunsch 2026-05-08: bei manuellem Refresh soll eine ANDERE Frage
     *  als die letzte entstehen — die letzte Frage wird als "vermeide diese
     *  wiederholungen"-Hint mitgeschickt, plus Frage wird sofort genullt damit
     *  der alte Text nicht stehen bleibt waehrend die neue Frage generiert wird. */
    fun refreshKiQuestion() {
        viewModelScope.launch {
            val previousText = kiQuestions.currentQuestion.first()?.text
            kiQuestions.setCurrent(null)
            try {
                val question = generateKiQuestion(previousText)
                kiQuestions.setCurrent(question)
            } catch (t: Throwable) {
                android.util.Log.e("TasksViewModel", "refreshKiQuestion failed", t)
            }
        }
    }

    fun openEntryDetail(id: String) { detailEntryIdFlow.value = id }
    fun closeEntryDetail() { detailEntryIdFlow.value = null }

    /** Loescht einen Eintrag (nicht nur archivieren — komplett weg). Aus dem Detail-Sheet. */
    fun deleteEntry(id: String) {
        viewModelScope.launch {
            val entry = entries.get(id) ?: return@launch
            entries.delete(entry)
            detailEntryIdFlow.value = null
        }
    }

    /** Status setzen aus dem Detail-Sheet (4-Buttons: OFFEN / IN_ARBEIT / REDUZIERT / ARCHIVIERT). */
    fun setEntryStatus(id: String, status: EntryStatus) {
        viewModelScope.launch {
            val entry = entries.get(id) ?: return@launch
            val now = System.currentTimeMillis()
            val updated = entry.copy(
                status = status,
                resolvedAt = if (status == EntryStatus.REDUZIERT || status == EntryStatus.ARCHIVIERT) now else null,
                updatedAt = now,
            )
            entries.update(updated)
            // Proaktiver Forscher (Frank-Wunsch 2026-05-08): wenn der Eintrag
            // gerade auf REDUZIERT geht, fragen wir wie er geloest wurde —
            // damit das InsightBoard "bestaetigte Methoden" lernen kann.
            if (status == EntryStatus.REDUZIERT && entry.status != EntryStatus.REDUZIERT) {
                pendingMethodForFlow.value = updated
            }
        }
    }

    fun snoozeKiQuestion() = viewModelScope.launch { kiQuestions.snoozeFor24Hours() }

    /**
     * Antwort auf die KI-Frage des Moments verarbeiten (Frank-Wunsch 2026-05-08).
     * 1) Antwort durch ProcessEntryUseCase als neuen Eintrag werten.
     * 2) Dedup-Check: wenn ein bestehender offener Eintrag mit aehnlichem Titel
     *    existiert, wird der neue Eintrag wieder geloescht und stattdessen der
     *    bestehende mit aiNotes ergaenzt + priorityScore erhoeht (Frank's
     *    "Laufeinheit"-Beispiel: keine Doppel-Eintraege wenn die Antwort eine
     *    schon priorisierte Aufgabe nennt).
     * 3) Frage wird aus dem Repository entfernt — sie ist beantwortet, soll
     *    nicht stehen bleiben. Beim naechsten refresh kommt eine neue Frage.
     */
    fun submitKiQuestionAnswer(answer: String) {
        if (answer.isBlank()) return
        viewModelScope.launch {
            kiQuestions.setCurrent(null)
            uiOnlyFlow.value = uiOnlyFlow.value.copy(processingMessage = "Antwort wird verarbeitet …")
            val newResult = process(answer.trim(), EntrySource.NUTZER_TEXT)
            newResult.onSuccess { newEntry ->
                val current = entries.getActive().first()
                val similar = current.firstOrNull { it.id != newEntry.id && titlesAreSimilar(it.title, newEntry.title) }
                if (similar != null) {
                    val notes = (similar.aiNotes?.takeIf { it.isNotBlank() }?.let { "$it\n\n" } ?: "") +
                        "Frank's Antwort auf KI-Frage: ${answer.trim()}"
                    entries.update(
                        similar.copy(
                            aiNotes = notes,
                            priorityScore = (similar.priorityScore + 5.0).coerceAtMost(100.0),
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                    entries.delete(newEntry)
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        processingMessage = null,
                        recentlyCreatedId = similar.id,
                    )
                } else {
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        processingMessage = null,
                        recentlyCreatedId = newEntry.id,
                    )
                }
                // Naechste Frage dynamisch generieren (basiert auf neuem Stand).
                refreshKiQuestion()
            }.onFailure { ex ->
                uiOnlyFlow.value = uiOnlyFlow.value.copy(
                    processingMessage = null,
                    errorMessage = ex.message ?: "Antwort konnte nicht verarbeitet werden",
                )
            }
        }
    }

    /** Heuristischer Dedup: wenn der Title eines bestehenden Eintrags eine
     *  signifikante Substring-Ueberlappung mit dem neuen Title hat (>=60% der
     *  kuerzeren Variante), gelten sie als gleich. Faengt "Laufeinheit im Freien"
     *  vs "Laufen draussen" nicht ab — dafuer braeuchte es einen Embedding-Match.
     *  Fuer den haeufigen Fall "exact answer" reicht dieser einfache Check. */
    private fun titlesAreSimilar(a: String, b: String): Boolean {
        val na = a.lowercase().trim()
        val nb = b.lowercase().trim()
        if (na == nb) return true
        if (na.isBlank() || nb.isBlank()) return false
        val shorter = if (na.length < nb.length) na else nb
        val longer = if (na.length < nb.length) nb else na
        return longer.contains(shorter) && shorter.length >= 4
    }

    /**
     * Markiert einen Eintrag als REDUZIERT (= erledigt) — wird vom Haken-Button
     * auf der Eintrag-Card aufgerufen. resolvedAt wird auf jetzt gesetzt damit
     * die Analyse "heute reduziert" zaehlen kann.
     */
    fun markEntryResolved(entryId: String) {
        viewModelScope.launch {
            val entry = entries.get(entryId) ?: return@launch
            val now = System.currentTimeMillis()
            val updated = entry.copy(
                status = EntryStatus.REDUZIERT,
                resolvedAt = now,
                updatedAt = now,
            )
            entries.update(updated)
            // Proaktiver Forscher: nach dem Haken-Tap fragen wie geloest.
            // Nur ausloesen wenn der Eintrag VORHER nicht schon REDUZIERT war
            // (Doppel-Tap auf Haken sollte keinen zweiten Dialog triggern).
            if (entry.status != EntryStatus.REDUZIERT) {
                pendingMethodForFlow.value = updated
            }
        }
    }

    /**
     * Speichert die vom Forscher abgefragte Loesungsmethode in den ai_notes des
     * Eintrags. Format: 'Methode: <text>' an den vorhandenen aiNotes angehaengt.
     * Damit kann die KI im Briefing/Review/Insight-Board diese Methode
     * wiederfinden und als "bestaetigte Methode" einordnen.
     */
    fun submitMethod(notes: String) {
        val current = pendingMethodForFlow.value ?: return
        if (notes.isBlank()) {
            pendingMethodForFlow.value = null
            return
        }
        viewModelScope.launch {
            val existing = current.aiNotes?.takeIf { it.isNotBlank() }
            val combined = if (existing == null) {
                "Methode: ${notes.trim()}"
            } else {
                "$existing\n\nMethode: ${notes.trim()}"
            }
            entries.update(
                current.copy(
                    aiNotes = combined,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            pendingMethodForFlow.value = null
        }
    }

    /**
     * Aufgaben-Nachtrag (Frank-Wunsch 2026-05-08): wenn Frank im Detail-Sheet
     * einen Nachtrag einspricht, wird der Eintrag durch ProcessEntryUseCase
     * neu bewertet — Title + Beschreibung + Tags + priorityScore + timeBucket
     * werden auf Basis von "Originaltext PLUS Nachtrag" neu generiert. So landet
     * die Aufgabe automatisch an der richtigen Prio-Stelle (auch wenn der
     * Nachtrag z.B. eine viel laengere Dauer impliziert).
     */
    fun addFollowupAndReprocess(entryId: String, followup: String) {
        if (followup.isBlank()) return
        viewModelScope.launch {
            val current = entries.get(entryId) ?: return@launch
            val combined = if (current.description.isNotBlank()) {
                "${current.description}\n\nNachtrag: ${followup.trim()}"
            } else {
                "Nachtrag: ${followup.trim()}"
            }
            uiOnlyFlow.value = uiOnlyFlow.value.copy(processingMessage = "Bewerte mit Nachtrag neu …")
            process("${current.title}. $combined", EntrySource.NUTZER_TEXT)
                .onSuccess {
                    // Den NEUEN Eintrag aus process() wieder mergen mit dem alten:
                    // wir wollen den alten id behalten, aber die neuen Felder uebernehmen.
                    // Pragmatisch: den alten Eintrag mit Nachtrag-text + neuen
                    // priority/category/bucket aktualisieren, neuen Eintrag wieder
                    // loeschen damit kein Doppel.
                    val newEntry = it
                    entries.update(
                        current.copy(
                            description = combined,
                            priorityScore = newEntry.priorityScore,
                            timeBucket = newEntry.timeBucket,
                            severity = newEntry.severity,
                            estimatedDurationMinutes = newEntry.estimatedDurationMinutes,
                            category = newEntry.category,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                    entries.delete(newEntry)
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(processingMessage = null)
                }
                .onFailure { ex ->
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        processingMessage = null,
                        errorMessage = ex.message ?: "Nachtrag-Bewertung fehlgeschlagen",
                    )
                }
        }
    }

    fun dismissMethodPrompt() {
        pendingMethodForFlow.value = null
    }

    /**
     * Setzt einen erledigten Eintrag wieder auf OFFEN — für Undo-Snackbar oder
     * wenn der Benutzer sich vertippt hat.
     */
    fun reopenEntry(entryId: String) {
        viewModelScope.launch {
            val entry = entries.get(entryId) ?: return@launch
            entries.update(
                entry.copy(
                    status = EntryStatus.OFFEN,
                    resolvedAt = null,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun toggleCategory(cat: EntropyCategory) {
        val current = activeCategoriesFlow.value.toMutableSet()
        if (cat in current) current.remove(cat) else current.add(cat)
        activeCategoriesFlow.value = current
    }

    fun clearCategoryFilter() { activeCategoriesFlow.value = emptySet() }

    fun onMicClick() {
        val app = getApplication<Application>()
        when (uiOnlyFlow.value.micState) {
            MicState.IDLE -> {
                runCatching {
                    recorder.start()
                    RecordingService.start(app)
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        micState = MicState.RECORDING,
                        processingMessage = "Aufnahme laeuft …",
                    )
                }.onFailure { ex ->
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        micState = MicState.IDLE,
                        errorMessage = ex.message ?: "Aufnahme konnte nicht gestartet werden",
                    )
                }
            }
            MicState.RECORDING -> {
                val file = recorder.stop()
                RecordingService.stop(app)
                if (file == null || !file.exists() || file.length() == 0L) {
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(micState = MicState.IDLE, processingMessage = null)
                    return
                }
                uiOnlyFlow.value = uiOnlyFlow.value.copy(
                    micState = MicState.PROCESSING,
                    processingMessage = "Transkribiere …",
                )
                viewModelScope.launch {
                    transcribe(file).onSuccess { transcript ->
                        if (transcript.isBlank()) {
                            uiOnlyFlow.value = uiOnlyFlow.value.copy(
                                micState = MicState.IDLE, processingMessage = null,
                            )
                            return@onSuccess
                        }
                        uiOnlyFlow.value = uiOnlyFlow.value.copy(processingMessage = "Verarbeite …")
                        process(transcript, EntrySource.NUTZER_MIC)
                            .onSuccess { e ->
                                uiOnlyFlow.value = uiOnlyFlow.value.copy(
                                    micState = MicState.IDLE,
                                    processingMessage = null,
                                    recentlyCreatedId = e.id,
                                )
                            }
                            .onFailure { ex ->
                                uiOnlyFlow.value = uiOnlyFlow.value.copy(
                                    micState = MicState.IDLE,
                                    processingMessage = null,
                                    errorMessage = ex.message ?: "KI-Verarbeitung fehlgeschlagen",
                                )
                            }
                    }.onFailure { ex ->
                        uiOnlyFlow.value = uiOnlyFlow.value.copy(
                            micState = MicState.IDLE,
                            processingMessage = null,
                            errorMessage = ex.message ?: "Transkription fehlgeschlagen",
                        )
                    }
                }
            }
            MicState.PROCESSING -> Unit
        }
    }

    fun dismissError() {
        uiOnlyFlow.value = uiOnlyFlow.value.copy(errorMessage = null)
    }

    fun changeStatus(entry: EntropyEntryEntity, newStatus: EntryStatus) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            entries.update(
                entry.copy(
                    status = newStatus,
                    updatedAt = now,
                    resolvedAt = if (newStatus == EntryStatus.REDUZIERT) now else entry.resolvedAt,
                )
            )
        }
    }

    fun delete(entry: EntropyEntryEntity) {
        viewModelScope.launch { entries.delete(entry) }
    }

    override fun onCleared() {
        super.onCleared()
        if (recorder.isRecording()) recorder.discard()
    }
}

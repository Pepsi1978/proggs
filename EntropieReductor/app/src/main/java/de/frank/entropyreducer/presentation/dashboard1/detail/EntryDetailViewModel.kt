package de.frank.entropyreducer.presentation.dashboard1.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.EntropyEntryFollowupDao
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryFollowupEntity
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.RecurringTemplateRepository
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.tts.TtsPlayer
import de.frank.entropyreducer.domain.tts.TtsResult
import de.frank.entropyreducer.domain.usecase.ProcessEntryUseCase
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
    // Frank-Wunsch 2026-05-22: aus einer Aufgabe direkt eine wiederkehrende Vorlage erzeugen.
    private val recurringRepo: RecurringTemplateRepository,
) : AndroidViewModel(application) {

    /**
     * Frank-Wunsch 2026-05-22: Event-Flow fuer die "Vorlage angelegt"-Snackbar
     * nach Konvertierung in eine wiederkehrende Aufgabe. true = Erfolg, false = Fehler.
     */
    val templateCreated: kotlinx.coroutines.flow.MutableSharedFlow<Boolean> =
        kotlinx.coroutines.flow.MutableSharedFlow(replay = 0, extraBufferCapacity = 1)

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

    /**
     * Frank-Wunsch 2026-05-22: Erzeugt aus dem aktuellen Eintrag eine wiederkehrende
     * Aufgaben-Vorlage mit Default-RRULE "FREQ=DAILY" um 08:00. Frank kann sie
     * danach im Loop-Reiter editieren (Frequenz aendern, Wochentage waehlen etc.).
     * Felder Titel/Beschreibung/Kategorie/Prio/Schwere/Dauer werden 1:1 uebernommen.
     */
    fun convertToRecurringTemplate() {
        viewModelScope.launch {
            try {
                val current = entries.get(entryId) ?: run {
                    templateCreated.tryEmit(false)
                    return@launch
                }
                val now = System.currentTimeMillis()
                val template = RecurringTemplateEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    title = current.title,
                    description = current.description.ifBlank { null },
                    category = current.category,
                    priorityScore = current.priorityScore.toInt().coerceIn(0, 100),
                    severity = current.severity,
                    estimatedDurationMinutes = current.estimatedDurationMinutes,
                    rrule = "FREQ=DAILY",
                    timeOfDayMinutes = 480,
                    untilEpochMs = null,
                    nextOccurrenceAt = null,
                    // Loop-Vorlagen erzeugen keine Aufgaben mehr automatisch. Die konvertierte
                    // Aufgabe bleibt die eine manuell vorhandene Aufgabe; weitere Kopien entstehen
                    // nur noch durch den Hinzufügen-Button im Loop-Bereich.
                    lastGeneratedAt = now,
                    occurrenceCount = 1,
                    isActive = false,
                    createdAt = now,
                    updatedAt = now,
                )
                recurringRepo.upsert(template)

                // Frank-Wunsch 2026-06-02 (Bugfix): Die konvertierte Aufgabe WIRD die heutige
                // Loop-Instanz dieser Vorlage — sie bekommt die deterministische rec-ID
                // ("rec-<templateId>-<mitternacht>") und source=RECURRING_TEMPLATE. Dadurch findet
                // die Loop-Bearbeitung (setPriority/setTitle/setTargetBucket im
                // RecurringTemplatesViewModel filtern genau auf dieses ID-Praefix + source) die
                // Aufgabe wieder, und eine im Loop-Block gesetzte Prioritaet schlaegt sofort auf
                // die Aufgabe im "Heute"-Reiter durch. Vorher blieb die Original-Aufgabe eine
                // unverknuepfte Normal-Aufgabe, sodass die Loop-Prioritaet sie nie erreichte.
                // Die Template-Prioritaet gilt als manuelle Prioritaet der Instanz (Vorrang vor
                // KI) — identisch zu regulaer generierten Loop-Instanzen (Doktrin 2026-06-01).
                val midnight = java.util.Calendar.getInstance().apply {
                    timeInMillis = now
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                val recId = "rec-${template.id}-$midnight"
                val templatePrio = template.priorityScore.toDouble()
                val recInstance = current.copy(
                    id = recId,
                    source = EntrySource.RECURRING_TEMPLATE,
                    priorityScore = templatePrio,
                    manualPriorityScore = templatePrio,
                    // Frank-Regel 2026-06-19: wird zur Loop-Instanz -> Template-getriebene Prio,
                    // kein Instanz-Marker (sonst wuerde die Loop-Pflege sie nicht mehr pflegen).
                    manualPriorityScoreSetAt = null,
                    updatedAt = now,
                )
                // Reihenfolge zwingend (FK entropy_entry_followups.entryId -> entropy_entries.id,
                // ON DELETE CASCADE): 1) neue Instanz anlegen, 2) Nachtraege auf die neue ID
                // umhaengen, 3) ERST DANN die Original loeschen. So gehen keine Nachtraege
                // verloren — CASCADE entfernt nur die alten Rows mit der Original-ID, die
                // umgehaengten haben bereits die neue entryId und bleiben erhalten.
                entries.upsert(recInstance)
                for (followup in followupDao.getByEntryId(current.id)) {
                    followupDao.upsert(
                        followup.copy(id = java.util.UUID.randomUUID().toString(), entryId = recId),
                    )
                }
                entries.delete(current)

                // Der Detail-Screen zeigte die Original-ID, die jetzt nicht mehr existiert (die
                // Aufgabe lebt als Loop-Instanz mit neuer ID weiter). Ueber den bestehenden
                // isDeleted -> onBack-Pfad sauber zurueck zur Liste, statt einen toten Eintrag
                // anzuzeigen.
                deletedFlow.value = true
                templateCreated.tryEmit(true)
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                templateCreated.tryEmit(false)
            }
        }
    }

    /**
     * Frank-Wunsch 2026-05-31: manueller Titel-Edit. Tippt Frank den Titel oben im
     * Detail an und aendert ihn, wird der neue Titel gespeichert und erscheint
     * dank der gemeinsamen DB-Quelle automatisch auch in der Aufgabenuebersicht.
     * Leere Eingaben werden ignoriert (alter Titel bleibt).
     */
    fun updateTitle(newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val current = entries.get(entryId) ?: return@launch
            if (current.title == clean) return@launch
            entries.update(current.copy(title = clean, updatedAt = System.currentTimeMillis()))
            reloadTrigger.value = System.currentTimeMillis()
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
                // Sync-Etappe 1.5: Nachtraege laufen am Repository vorbei (eigener DAO) -> expliziter
                // Backup-Trigger, damit der Nachtrag auch ohne Gemini-Rescore gesichert wird.
                entries.triggerBackup("Aufgaben-Nachtrag: hinzugefuegt")
                rescoreWithCurrentFollowups()
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
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
                Diag.w(DiagnosticArea.APP, 
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
     * Frank-Wunsch 2026-05-22 (dritte Iteration) + 2026-05-23 (Bugfix):
     * Frist setzen oder loeschen. null = keine Frist mehr.
     *
     * Zwei-Stufen-Anpassung damit die Prio IMMER sofort sichtbar wird:
     *  1. Lokaler Deadline-Floor wird unmittelbar berechnet und gespeichert —
     *     funktioniert auch ohne Internet/Gemini-Key. Frank sieht den neuen
     *     Wert sofort in der Liste.
     *  2. Optionaler Gemini-Rescore obendrauf (kann ueberschreiben). Faellt
     *     er aus (kein Key/Netz), bleibt der lokale Floor-Wert stehen.
     */
    fun setDueDate(epochMs: Long?) {
        viewModelScope.launch {
            val current = entries.get(entryId) ?: return@launch
            val recomputedScore =
                de.frank.entropyreducer.domain.usecase.computeDeadlineFloor(
                    baseScore = current.priorityScore,
                    dueAtMs = epochMs,
                )
            entries.update(
                current.copy(
                    dueAtMs = epochMs,
                    priorityScore = recomputedScore,
                    updatedAt = System.currentTimeMillis(),
                )
            )
            reloadTrigger.value = System.currentTimeMillis()
            // Optionale Verfeinerung durch Gemini — laeuft asynchron weiter.
            // Falls Gemini ausfaellt, bleibt der lokale Floor-Wert stehen.
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
            entries.triggerBackup("Aufgaben-Nachtrag: bearbeitet")
        }
    }

    fun deleteFollowup(id: String) {
        viewModelScope.launch {
            followupDao.deleteById(id)
            // Sync-Etappe 1.5: Tombstone, damit die Nachtrag-Loeschung auf andere Geraete propagiert.
            de.frank.entropyreducer.data.markDeleted(
                getApplication(),
                de.frank.entropyreducer.data.TombstoneType.FOLLOWUP,
                id,
            )
            entries.triggerBackup("Aufgaben-Nachtrag: geloescht")
        }
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

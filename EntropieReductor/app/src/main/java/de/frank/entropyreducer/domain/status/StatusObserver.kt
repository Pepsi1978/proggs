package de.frank.entropyreducer.domain.status

import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Liefert einen Live-Flow des aktuellen Status-Balken-Zustands.
 *
 * Spec §4.1 — Wert wird alle 5 Minuten neu berechnet UND sofort bei jeder Eintrags-
 * oder Biomarker-/Calendar-Änderung. Der Ticker triggert ein neues Read-Window
 * (today, 30-Tage-Bereich), damit der Status um Mitternacht ohne App-Restart
 * den richtigen Schichtcode-Tag liest.
 *
 * Performance-Fix Loop 5.1: Vorher returnte observe() bei jedem Aufruf einen
 * neuen Cold-Flow. Bei mehreren Subscribern (AnalysisViewModel + TasksViewModel
 * + ggf. weitere) wurden ALLE 5 Min die 4 Room-Queries pro Subscriber neu
 * ausgefuehrt — also bis zu 4×N redundante DB-Reads pro Tick. Mit shareIn
 * (WhileSubscribed(5s), replay=1) teilen sich alle Subscriber EINEN
 * ticker+combine-Stream; neue Subscriber bekommen sofort den letzten
 * Berechnungs-Snapshot. Kein API-Eingriff fuer Aufrufer (observe() bleibt
 * `Flow<StatusBreakdown>`). Bonus: alle ViewModels sehen jetzt
 * KONSISTENT denselben StatusBreakdown — vorher konnten A und B durch
 * versetzte Tick-Zyklen kurzzeitig unterschiedliche Werte sehen.
 */
@Singleton
class StatusObserver @Inject constructor(
    private val entries: EntryRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val calendarDao: CalendarDayDao,
    private val useCase: CalculateStatusUseCase,
    @ApplicationScope private val scope: CoroutineScope,
) {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val sharedFlow: SharedFlow<StatusBreakdown> =
        ticker(intervalMs = 5L * 60 * 1000)
            .flatMapLatest { _ ->
                val now = System.currentTimeMillis()
                val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000
                val today = java.time.LocalDate
                    .now(java.time.ZoneId.systemDefault())
                    .toString()

                combine(
                    entries.getActive(),
                    biomarkerDao.getLatest(),
                    biomarkerDao.getRange(thirtyDaysAgo, now),
                    calendarDao.getDay(today),
                ) { entryList, latest, history, day ->
                    useCase.calculate(
                        entries = entryList,
                        latestSnapshot = latest,
                        recentSnapshots = history,
                        todayCalendar = day,
                    )
                }
            }
            .shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                replay = 1,
            )

    fun observe(): Flow<StatusBreakdown> = sharedFlow

    /** Emittiert sofort beim Start, dann alle [intervalMs] Millisekunden. */
    private fun ticker(intervalMs: Long): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(intervalMs)
        }
    }
}

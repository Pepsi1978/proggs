package de.frank.entropyreducer.domain.status

import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.repository.EntryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Liefert einen Live-Flow des aktuellen Status-Balken-Zustands.
 *
 * Spec §4.1 — Wert wird alle 5 Minuten neu berechnet UND sofort bei jeder Eintrags-
 * oder Biomarker-/Calendar-Aenderung. Der Ticker triggert ein neues Read-Window
 * (today, 30-Tage-Bereich), damit der Status um Mitternacht ohne App-Restart
 * den richtigen Schichtcode-Tag liest.
 */
@Singleton
class StatusObserver @Inject constructor(
    private val entries: EntryRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val calendarDao: CalendarDayDao,
    private val useCase: CalculateStatusUseCase,
) {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observe(): Flow<StatusBreakdown> = ticker(intervalMs = 5L * 60 * 1000)
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

    /** Emittiert sofort beim Start, dann alle [intervalMs] Millisekunden. */
    private fun ticker(intervalMs: Long): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(intervalMs)
        }
    }
}

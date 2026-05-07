package de.frank.entropyreducer.domain.status

import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.repository.EntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Liefert einen Live-Flow des aktuellen Status-Balken-Zustands. Kombiniert die drei
 * Datenquellen (Eintraege, Biomarker, Kalender heute) und delegiert die Mathematik
 * an den CalculateStatusUseCase.
 *
 * Eingebettet in jedes Dashboard-ViewModel via Hilt.
 */
@Singleton
class StatusObserver @Inject constructor(
    private val entries: EntryRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val calendarDao: CalendarDayDao,
    private val useCase: CalculateStatusUseCase,
) {

    fun observe(): Flow<StatusBreakdown> {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000
        val today = LocalDate.now(ZoneId.systemDefault()).toString()

        return combine(
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
}

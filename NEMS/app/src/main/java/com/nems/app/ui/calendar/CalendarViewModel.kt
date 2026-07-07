package com.nems.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nems.app.data.local.SeedDataProvider
import com.nems.app.data.local.dao.DailyCompletionStat
import com.nems.app.data.repository.SupplementRepository
import com.nems.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: SupplementRepository,
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    val completionStats: StateFlow<Map<String, DailyCompletionStat>> = _currentMonth
        .flatMapLatest { month ->
            val start = month.atDay(1)
            val end = month.atEndOfMonth()
            repository.getCompletionStatsForRange(start, end)
        }
        .map { stats -> stats.associateBy { it.date } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            repository.insertSeedSections(SeedDataProvider.stackSections)
            repository.insertSeedSupplements(SeedDataProvider.supplements)
            // Generate today's entries so the calendar shows data immediately
            val supps = repository.getAllActiveSuplements().first()
            val sects = repository.getAllSections().first()
            repository.generateDailyEntries(LocalDate.now(), supps, sects)
        }
    }

    fun navigateMonth(offset: Int) {
        _currentMonth.value = _currentMonth.value.plusMonths(offset.toLong())
    }

    fun goToToday() {
        _currentMonth.value = YearMonth.now()
    }

    fun getCompletionForDate(date: LocalDate, statsMap: Map<String, DailyCompletionStat>): Float {
        val stat = statsMap[date.format(DateUtils.isoDateFormatter)] ?: return -1f
        return if (stat.total == 0) -1f else stat.takenCount.toFloat() / stat.total
    }
}

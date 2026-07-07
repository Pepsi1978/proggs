package com.nems.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nems.app.data.local.dao.DailyCompletionStat
import com.nems.app.data.local.dao.MissedSupplement
import com.nems.app.data.repository.SupplementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    repository: SupplementRepository,
) : ViewModel() {

    private val thirtyDaysAgo = LocalDate.now().minusDays(30)
    private val sevenDaysAgo = LocalDate.now().minusDays(7)
    private val today = LocalDate.now()

    val weeklyStats: StateFlow<List<DailyCompletionStat>> = repository
        .getCompletionStatsForRange(sevenDaysAgo, today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyStats: StateFlow<List<DailyCompletionStat>> = repository
        .getCompletionStatsForRange(thirtyDaysAgo, today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostMissed: StateFlow<List<MissedSupplement>> = repository
        .getMostMissed(thirtyDaysAgo, today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentStreak: StateFlow<Int> = repository
        .getDailyCompletionStats()
        .map { stats ->
            var streak = 0
            val sorted = stats.sortedByDescending { it.date }
            for (stat in sorted) {
                if (stat.total > 0 && stat.takenCount == stat.total) {
                    streak++
                } else {
                    break
                }
            }
            streak
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}

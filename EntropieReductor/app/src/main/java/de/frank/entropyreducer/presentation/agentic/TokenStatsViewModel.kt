package de.frank.entropyreducer.presentation.agentic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.repository.TokenUsageRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel fuer den TokenStats-Screen (Frank-Wunsch 2026-05-21).
 *
 * Stellt drei Bloecke an Daten bereit:
 *  - heutige Tokens pro Prompt
 *  - 30-Tage-Historie pro Prompt (fuer Mini-Bars)
 *  - das pro-Prompt-Token-Limit (editierbar, null = kein Limit)
 *
 * Alle Werte werden als StateFlow exponiert und vom UI per collectAsState
 * konsumiert. Schreibt das Limit via PromptRepository.updateTokenLimit zurueck.
 */
@HiltViewModel
class TokenStatsViewModel
@Inject
constructor(
    private val promptRepo: PromptRepository,
    private val tokenUsageRepo: TokenUsageRepository,
) : ViewModel() {

    /**
     * Heutige Token-Stats pro Prompt — kombiniert die Prompt-Liste mit dem
     * Tages-Aggregat. Sortiert nach heutigem Verbrauch absteigend.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val rows: StateFlow<List<TokenStatsRow>> =
        promptRepo
            .getAll()
            .flatMapLatest { prompts ->
                if (prompts.isEmpty()) {
                    flowOf(emptyList<TokenStatsRow>())
                } else {
                    val today = tokenUsageRepo.today()
                    tokenUsageRepo.getAllForDay(today).combine(flowOf(prompts)) { todayUsage, ps ->
                        ps.map { prompt ->
                            val usage = todayUsage.firstOrNull { it.promptId == prompt.id }
                            TokenStatsRow(
                                prompt = prompt,
                                todayTotal = usage?.tokensTotal ?: 0,
                                todayInput = usage?.tokensInput ?: 0,
                                todayOutput = usage?.tokensOutput ?: 0,
                                todayRunCount = usage?.runCount ?: 0,
                            )
                        }
                            .sortedByDescending { it.todayTotal }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    /**
     * Geladene 30-Tage-Historien pro Prompt — lazy nachgeladen wenn der UI
     * eine Prompt-Karte expandiert (cache-Verhalten ueber eine Mutable-Map).
     */
    private val _historyCache =
        kotlinx.coroutines.flow.MutableStateFlow<Map<String, List<HistoryBar>>>(emptyMap())
    val historyCache: StateFlow<Map<String, List<HistoryBar>>> = _historyCache

    fun loadHistory(promptId: String, days: Long = 30) {
        viewModelScope.launch {
            val from = tokenUsageRepo.daysAgo(days)
            val rawList = tokenUsageRepo.getHistoryForPrompt(promptId, days).first()
            val byDay = rawList.associateBy { it.day }
            val maxValue = (rawList.maxOfOrNull { it.tokensTotal } ?: 0).coerceAtLeast(1)
            val bars =
                buildList {
                    for (offset in (days - 1) downTo 0) {
                        val day = tokenUsageRepo.daysAgo(offset)
                        val entry = byDay[day]
                        add(
                            HistoryBar(
                                day = day,
                                tokensTotal = entry?.tokensTotal ?: 0,
                                runCount = entry?.runCount ?: 0,
                                relative = (entry?.tokensTotal ?: 0) / maxValue.toFloat(),
                            )
                        )
                    }
                }
            _historyCache.value = _historyCache.value + (promptId to bars)
        }
    }

    fun setTokenLimit(promptId: String, limit: Int?) {
        viewModelScope.launch {
            val cleaned = limit?.takeIf { it > 0 }
            promptRepo.updateTokenLimit(promptId, cleaned)
        }
    }

    fun recomputeStats() {
        // Frank-Wunsch 2026-05-21: "Token-Stats neu berechnen"-Knopf gegen Drift.
        viewModelScope.launch { tokenUsageRepo.recomputeAll() }
    }
}

/**
 * Eine Zeile fuer den TokenStats-Screen: ein Prompt mit Heute-Verbrauch
 * und Konfigurations-Zustand.
 */
data class TokenStatsRow(
    val prompt: SavedPromptEntity,
    val todayTotal: Int,
    val todayInput: Int,
    val todayOutput: Int,
    val todayRunCount: Int,
) {
    val hasLimit: Boolean = prompt.tokenLimitPerDay != null
    val limitReached: Boolean = prompt.tokenLimitPerDay?.let { todayTotal >= it } == true
    val limitProgressFraction: Float =
        prompt.tokenLimitPerDay?.let { todayTotal.toFloat() / it.coerceAtLeast(1).toFloat() } ?: 0f
}

/**
 * Eine Tages-Bar im Balkendiagramm. relative ist 0.0..1.0 fuer die Skalierung
 * gegen das groesste Tagesvolumen in der Historie.
 */
data class HistoryBar(
    val day: String,
    val tokensTotal: Int,
    val runCount: Int,
    val relative: Float,
)

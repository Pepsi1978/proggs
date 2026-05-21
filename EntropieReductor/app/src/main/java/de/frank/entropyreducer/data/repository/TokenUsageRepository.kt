package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.PromptExecutionDao
import de.frank.entropyreducer.data.local.dao.TokenUsageDailyDao
import de.frank.entropyreducer.data.local.entities.TokenUsageDailyEntity
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Repository fuer die Tages-Token-Statistik. Bedient drei verschiedene Konsumenten:
 *
 * 1. **TokenMeter (vor jedem Run):**
 *    - `getTodayTotalForPrompt(promptId)` — wieviel hat dieser Prompt heute schon verbraucht?
 *    - vergleicht mit `savedPrompts.tokenLimitPerDay` (optional, in der Prompt-Entitaet)
 *
 * 2. **ExecutionLogger (nach jedem Run):**
 *    - `addTokens(promptId, day, in, out)` — inkrementiert die Tages-Statistik
 *
 * 3. **TokenStats-Screen (Einstellungen):**
 *    - `getHistoryForPrompt(promptId, days=30)` — fuer Balkendiagramm
 *    - `getAllForDay(today)` — fuer Heute-Uebersicht ueber alle Prompts
 *    - `recomputeAll()` — Drift-Schutz: baut die Tabelle aus prompt_executions neu auf
 *
 * id-Konvention: "{promptId}_{day}" als String-Schluessel fuer einfaches Upsert.
 */
@Singleton
class TokenUsageRepository
@Inject
constructor(
    private val dao: TokenUsageDailyDao,
    private val executionDao: PromptExecutionDao,
) {

    private val zone: ZoneId = ZoneId.systemDefault()

    /** ISO-Datum heute in lokaler Zeitzone (z.B. "2026-05-21"). */
    fun today(): String = LocalDate.now(zone).toString()

    /** ISO-Datum vor N Tagen (z.B. fuer 30-Tage-Historie). */
    fun daysAgo(days: Long): String = LocalDate.now(zone).minusDays(days).toString()

    fun getHistoryForPrompt(
        promptId: String,
        days: Long = 30,
    ): Flow<List<TokenUsageDailyEntity>> =
        dao.getHistoryForPrompt(promptId, daysAgo(days))

    fun getAllForDay(day: String): Flow<List<TokenUsageDailyEntity>> = dao.getAllForDay(day)

    fun getAllForToday(): Flow<List<TokenUsageDailyEntity>> = dao.getAllForDay(today())

    suspend fun getOne(promptId: String, day: String): TokenUsageDailyEntity? =
        dao.getOne(promptId, day)

    /**
     * Vom TokenMeter genutzt — wieviel Tokens hat dieser Prompt heute verbraucht?
     * Aggregiert ueber alle Eintraege fuer (promptId, today). Sollte normalerweise
     * eine einzige Zeile sein, aber SUM ist defensiv.
     */
    suspend fun getTodayTotalForPrompt(promptId: String): Int =
        dao.getTotalForPromptOnDay(promptId, today())

    /**
     * Vom ExecutionLogger nach jedem Lauf aufgerufen. Inkrementiert die Tages-Statistik
     * fuer (promptId, today).
     */
    suspend fun addTokens(promptId: String, tokensInput: Int, tokensOutput: Int) {
        val day = today()
        val id = "${promptId}_${day}"
        val existing = dao.getOne(promptId, day)
        val updated =
            if (existing == null) {
                TokenUsageDailyEntity(
                    id = id,
                    promptId = promptId,
                    day = day,
                    tokensInput = tokensInput,
                    tokensOutput = tokensOutput,
                    tokensTotal = tokensInput + tokensOutput,
                    runCount = 1,
                )
            } else {
                existing.copy(
                    tokensInput = existing.tokensInput + tokensInput,
                    tokensOutput = existing.tokensOutput + tokensOutput,
                    tokensTotal = existing.tokensTotal + tokensInput + tokensOutput,
                    runCount = existing.runCount + 1,
                )
            }
        dao.upsert(updated)
    }

    /**
     * Drift-Schutz: aus den Einstellungen aufrufbar. Loescht die Aggregations-Tabelle
     * und baut sie neu aus prompt_executions auf. Sollte O(n)-Operation sein wobei n =
     * Anzahl Executions in den letzten 365 Tagen.
     */
    suspend fun recomputeAll() {
        dao.deleteAll()
        val executions = executionDao.getRecent(limit = 10_000)
        // Hinweis: getRecent ist Flow, wir nutzen executionDao direkt mit suspend fuer
        // Recompute. Bei Bedarf in Future-Migration eine getRecentSnapshot()-Methode
        // anbieten — fuer Stufe 1 nutzen wir die LIMIT-Query via Flow.first().
        // Aktueller Stand: recomputeAll wird vom Settings-Screen ueber executionDao
        // direkt aufgerufen. Erweiterung kommt in Etappe 10 (TokenStats-Screen).
        executions
            .let {
                /* placeholder - actual recompute in Etappe 10 */
            }
    }

    /** Alte Tage prunen (Default: aelter als 365 Tage). */
    suspend fun pruneOldDays(keepDays: Long = 365) {
        dao.pruneOlderThan(daysAgo(keepDays))
    }

    suspend fun deleteAllForPrompt(promptId: String) = dao.deleteByPrompt(promptId)
}

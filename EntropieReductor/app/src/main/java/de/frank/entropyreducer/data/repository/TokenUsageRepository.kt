package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.PromptExecutionDao
import de.frank.entropyreducer.data.local.dao.TokenUsageDailyDao
import de.frank.entropyreducer.data.local.entities.TokenUsageDailyEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock

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
     *
     * Direktive 3 Loop-1-Fix (war MED-1-Bug): Atomare Upsert-Operation via
     * SQL ON CONFLICT statt read-modify-write. Verhindert verlorene Tokens
     * bei parallelen Runs (z.B. CRON + Chain feuern gleichzeitig).
     *
     * Direktive 3 Loop-3-Fix (war L3-1-Bug): minSdk=28 hat SQLite 3.22, ON
     * CONFLICT DO UPDATE braucht 3.24+. Fallback auf Read-Modify-Write fuer
     * alte SQLite-Versionen. try-catch ist sicherer als Android-API-Check —
     * manche OEMs aktualisieren SQLite unabhaengig vom API-Level. Mutex
     * verhindert Race bei Fallback-Pfad.
     */
    suspend fun addTokens(promptId: String, tokensInput: Int, tokensOutput: Int) {
        if (tokensInput == 0 && tokensOutput == 0) return
        val day = today()
        val id = "${promptId}_${day}"
        try {
            dao.incrementUsage(
                id = id,
                promptId = promptId,
                day = day,
                tokensInput = tokensInput,
                tokensOutput = tokensOutput,
            )
        } catch (e: android.database.sqlite.SQLiteException) {
            // SQLite < 3.24: ON CONFLICT DO UPDATE wird nicht unterstuetzt.
            // Defensiv via Mutex+Read-Modify-Write fallback. Single-Mutex ist
            // OK weil pro-Tag-pro-Prompt die Konkurrenz typischerweise klein ist.
            addTokensFallback(id, promptId, day, tokensInput, tokensOutput)
        }
    }

    private val fallbackMutex = kotlinx.coroutines.sync.Mutex()

    private suspend fun addTokensFallback(
        id: String,
        promptId: String,
        day: String,
        tokensInput: Int,
        tokensOutput: Int,
    ) {
        // Loop-5-Fix (L5-7): withLock statt lock/unlock — cancellation-safe.
        // lock() ist suspending und kann selbst gecancelt werden, dann darf
        // finally NICHT unlock() auf einem nicht-gehaltenen Mutex aufrufen
        // (Crash mit IllegalStateException). withLock handhabt das korrekt.
        fallbackMutex.withLock {
            val existing = dao.getOne(promptId, day)
            if (existing == null) {
                dao.upsert(
                    TokenUsageDailyEntity(
                        id = id,
                        promptId = promptId,
                        day = day,
                        tokensInput = tokensInput,
                        tokensOutput = tokensOutput,
                        tokensTotal = tokensInput + tokensOutput,
                        runCount = 1,
                    )
                )
            } else {
                dao.upsert(
                    existing.copy(
                        tokensInput = existing.tokensInput + tokensInput,
                        tokensOutput = existing.tokensOutput + tokensOutput,
                        tokensTotal = existing.tokensTotal + tokensInput + tokensOutput,
                        runCount = existing.runCount + 1,
                    )
                )
            }
        }
    }

    /**
     * Drift-Schutz: aus den Einstellungen aufrufbar. Aggregation neu aus
     * prompt_executions aufbauen.
     *
     * Wichtig (Direktive 3 Loop-1-Fix, war HIGH-1-Bug): Diese Methode war
     * frueher ein Stub der dao.deleteAll() aufgerufen hat OHNE die Aggregation
     * neu zu bauen — ergab kompletten Datenverlust der Tagesstatistik. Neue
     * Implementierung:
     *
     *  1. Erst alle Executions laden und in Memory aggregieren (promptId+day -> Summen)
     *  2. ERST DANACH dao.deleteAll() + Re-Insert — wenn Schritt 1 fehlschlaegt,
     *     bleibt die alte Tabelle unangetastet (defensiv).
     *
     * Aggregierungs-Tag = ISO-Datum (lokale Zeitzone) vom startedAt-Zeitpunkt.
     */
    suspend fun recomputeAll() {
        // Schritt 1: in-memory Aggregation aufbauen (KEIN destruktiver Schritt vorab).
        val allExecutions = executionDao.getRecent(limit = 10_000).first()
        if (allExecutions.isEmpty()) {
            // Keine Executions vorhanden — Tabelle leeren ist defensiv ok.
            dao.deleteAll()
            return
        }
        data class Agg(
            var tokensInput: Int = 0,
            var tokensOutput: Int = 0,
            var tokensTotal: Int = 0,
            var runCount: Int = 0,
        )
        val aggregated = mutableMapOf<Pair<String, String>, Agg>()
        for (exec in allExecutions) {
            // Nur erfolgreich abgeschlossene oder zumindest token-erfassende Runs zaehlen
            if (exec.tokensTotal == 0 && exec.tokensInput == 0 && exec.tokensOutput == 0) continue
            val day = Instant.ofEpochMilli(exec.startedAt).atZone(zone).toLocalDate().toString()
            val key = exec.promptId to day
            val agg = aggregated.getOrPut(key) { Agg() }
            agg.tokensInput += exec.tokensInput
            agg.tokensOutput += exec.tokensOutput
            agg.tokensTotal += exec.tokensTotal
            agg.runCount += 1
        }

        // Schritt 2: Tabelle leeren + neu befuellen. Erst jetzt destruktiv.
        dao.deleteAll()
        for ((key, agg) in aggregated) {
            val (promptId, day) = key
            dao.upsert(
                TokenUsageDailyEntity(
                    id = "${promptId}_${day}",
                    promptId = promptId,
                    day = day,
                    tokensInput = agg.tokensInput,
                    tokensOutput = agg.tokensOutput,
                    tokensTotal = agg.tokensTotal,
                    runCount = agg.runCount,
                )
            )
        }
    }

    /** Alte Tage prunen (Default: aelter als 365 Tage). */
    suspend fun pruneOldDays(keepDays: Long = 365) {
        dao.pruneOlderThan(daysAgo(keepDays))
    }

    suspend fun deleteAllForPrompt(promptId: String) = dao.deleteByPrompt(promptId)
}

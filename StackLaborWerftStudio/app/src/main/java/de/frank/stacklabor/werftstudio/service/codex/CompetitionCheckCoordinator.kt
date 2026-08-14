package de.frank.stacklabor.werftstudio.service.codex

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CompetitionCheckResult(
    val stackId: String,
    val checkedSupplementIds: Set<String>,
    val noticesBySupplementId: Map<String, String>,
)

interface CompetitionChecker {
    suspend fun check(stackId: String, addedSupplementIds: Set<String>): CompetitionCheckResult
}

interface CompetitionNoticeStore {
    suspend fun markPending(stackId: String, supplementIds: Set<String>)
    suspend fun saveOpenNotices(result: CompetitionCheckResult)
    suspend fun saveUnavailableNotice(stackId: String, supplementIds: Set<String>, message: String)
}

class CompetitionCheckCoordinator(
    private val scope: CoroutineScope,
    private val checker: CompetitionChecker,
    private val noticeStore: CompetitionNoticeStore,
    private val debounceMillis: Long = 3_000L,
) {
    private val locks = ConcurrentHashMap<String, Any>()
    private val pendingIds = ConcurrentHashMap<String, MutableSet<String>>()
    private val jobs = ConcurrentHashMap<String, Job>()

    fun supplementsAdded(stackId: String, supplementIds: Set<String>) {
        require(stackId.isNotBlank())
        if (supplementIds.isEmpty()) return
        val lock = locks.getOrPut(stackId) { Any() }
        synchronized(lock) {
            pendingIds.getOrPut(stackId) { linkedSetOf() }.addAll(supplementIds)
            jobs.remove(stackId)?.cancel()
            jobs[stackId] = scope.launch {
                val snapshot = synchronized(lock) { pendingIds[stackId].orEmpty().toSet() }
                noticeStore.markPending(stackId, snapshot)
                delay(debounceMillis)
                try {
                    val current = synchronized(lock) { pendingIds[stackId].orEmpty().toSet() }
                    noticeStore.saveOpenNotices(checker.check(stackId, current))
                    synchronized(lock) {
                        pendingIds.remove(stackId)
                        jobs.remove(stackId)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    val current = synchronized(lock) { pendingIds[stackId].orEmpty().toSet() }
                    noticeStore.saveUnavailableNotice(
                        stackId,
                        current,
                        "Konkurrenzprüfung nicht möglich — beim nächsten Auswerten",
                    )
                    synchronized(lock) {
                        pendingIds.remove(stackId)
                        jobs.remove(stackId)
                    }
                }
            }
        }
    }

    fun cancel(stackId: String) {
        synchronized(locks.getOrPut(stackId) { Any() }) {
            jobs.remove(stackId)?.cancel()
            pendingIds.remove(stackId)
        }
    }
}

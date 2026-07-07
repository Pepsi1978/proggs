package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import javax.inject.Inject

/**
 * Wird vom CalendarSyncWorker nach jedem Calendar-Sync aufgerufen. Frueher hat
 * dieser UseCase die Bucket-Heuristik aus CalculateBucketsUseCase angewendet —
 * das hat aber Frank's strikte 5/5/10/unbegrenzt-Spezifikation gebrochen
 * (alle 30-min-Eintraege ohne Kalenderdaten landeten in HEUTE).
 *
 * Frank-Wunsch 2026-05-09: Die Bucket-Verteilung ist STRIKT — 5/5/10/unbegrenzt
 * nach priorityScore. Es gibt nur eine autoritative Quelle dafuer:
 * BalanceBucketsUseCase. RebucketEntriesUseCase delegiert vollstaendig dorthin
 * damit der Calendar-Worker die Verteilung nicht mehr zerstoeren kann.
 */
class RebucketEntriesUseCase @Inject constructor(
    private val balance: BalanceBucketsUseCase,
) {

    suspend operator fun invoke(): Int {
        val updated = balance()
        Diag.i(DiagnosticArea.TASKS, TAG, "Rebucketing: $updated Eintraege durch BalanceBucketsUseCase aktualisiert")
        return updated
    }

    companion object { private const val TAG = "RebucketEntriesUseCase" }
}

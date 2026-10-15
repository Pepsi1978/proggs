package de.frank.entropyreducer.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.repository.WhoopRepository
import de.frank.entropyreducer.domain.usecase.ForegroundSyncManager
import de.frank.entropyreducer.domain.usecase.WhoopAbrufPlan
import kotlinx.coroutines.CancellationException

/**
 * Synchronisiert Whoop-Biomarker im Hintergrund.
 * Spec §15.4 — bei App-Start (wenn letzter Sync > 30 Min.) + nightly 04:30.
 */
@HiltWorker
class WhoopSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: WhoopRepository,
    private val sync: ForegroundSyncManager,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Frank-Wunsch 2026-05-08: vollstaendige Historie (letztes Jahr) statt
        // nur 30 Tage — die KI kann mit mehr Daten besser Trends erkennen und
        // die Korrelations-Card profitiert direkt von der laengeren Historie.
        val outcome = repo.syncLastDays(days = 365)
        if (outcome.isSuccess) {
            // 2026-10-15: Dieser Abruf (direkt nach neuer Anmeldung) laeuft am zentralen Abgleich vorbei.
            // Bericht und Fusszeile sofort neu schreiben, sonst bleibt "Anmeldung abgelaufen" bis zum naechsten
            // Hintergrundlauf stehen. Ohne Whoop: der Abruf ist ja gerade erst gelaufen.
            try {
                sync.syncApisNow("Nach Whoop-Abruf", WhoopAbrufPlan.Anlass.OHNE_WHOOP)
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (_: Exception) {
            }
        }
        return outcome.fold(
            onSuccess = { Result.success() },
            onFailure = { retryOrFailure() },
        )
    }

    companion object {
        const val UNIQUE_NAME_PERIODIC = "whoop-sync-periodic"
        const val UNIQUE_NAME_ONESHOT = "whoop-sync-oneshot"
    }
}

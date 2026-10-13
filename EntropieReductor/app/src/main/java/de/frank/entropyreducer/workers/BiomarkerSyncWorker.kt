package de.frank.entropyreducer.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.domain.usecase.ForegroundSyncManager
import kotlinx.coroutines.CancellationException

/**
 * Gleicht alle Biomarker-Quellen (Whoop, Oura, Waage, Health Connect, Kalender) im Hintergrund ab,
 * auch wenn die App nicht geöffnet ist (Frank-Wunsch 2026-10-13: "Werte sollen immer aktuell sein").
 *
 * Nutzt denselben Abgleich wie der Biomarker-Reiter und Jarvis ([ForegroundSyncManager.syncApisNow]),
 * also dieselbe Sperre, dieselbe Fehlerbewertung je Quelle und dieselbe Fußzeile.
 *
 * Gibt immer success zurück: Der Lauf wiederholt sich ohnehin alle 30 Minuten, und ein failure
 * würde nur die Fehlerzählung von WorkManager hochtreiben. Was schiefging, steht je Quelle im
 * gespeicherten Abgleich-Stand und im Diagnose-Protokoll.
 */
@HiltWorker
class BiomarkerSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val sync: ForegroundSyncManager,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        try {
            sync.syncApisNow("Hintergrund")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Diag.w(DiagnosticArea.APP, "BiomarkerSyncWorker", "Hintergrund-Abgleich fehlgeschlagen", error)
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME_PERIODIC = "biomarker-sync-periodic"
    }
}

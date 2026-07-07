package de.frank.entropyreducer.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.domain.usecase.GenieCodexSynthesizer

/**
 * Sonntags 19:00 (lokale Zeit) — siehe BackgroundScheduler.ensureCodexJob().
 * Auch via runCodexNow() einmalig ausloesbar.
 */
@HiltWorker
class GenieCodexWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val synth: GenieCodexSynthesizer,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return synth().fold(
            onSuccess = {
                Diag.i(DiagnosticArea.AGENTIC, TAG, "Codex-Synthese OK ($it)")
                Result.success()
            },
            onFailure = {
                Diag.e(DiagnosticArea.AGENTIC, TAG, "Codex-Synthese fehlgeschlagen", it)
                Result.retry()
            },
        )
    }

    companion object {
        private const val TAG = "GenieCodexWorker"
        const val UNIQUE_NAME_PERIODIC = "genie-codex-periodic"
        const val UNIQUE_NAME_ONESHOT = "genie-codex-oneshot"
    }
}

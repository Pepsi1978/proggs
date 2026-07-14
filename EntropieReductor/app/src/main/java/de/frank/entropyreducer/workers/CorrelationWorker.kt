package de.frank.entropyreducer.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.domain.usecase.DetectCorrelationsUseCase

/**
 * Korrelations-Engine Worker (Spec §16.1) — täglich 03:30.
 * Berechnet Cohen's-d-Beobachtungen zwischen Supplements und Biomarker und
 * loggt sie. Echte Trigger-Vorschlaege erzeugt der KiTriggerWorker später
 * aus den gesammelten Beobachtungen.
 */
@HiltWorker
class CorrelationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val detector: DetectCorrelationsUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val observations = detector()
        Diag.i(DiagnosticArea.BIOMARKER, TAG, "Korrelations-Lauf fertig: ${observations.size} Beobachtungen.")
        observations.forEach { obs ->
            Diag.i(DiagnosticArea.BIOMARKER, 
                TAG,
                "Beobachtung: ${obs.stackType} -> ${obs.metric}, d=${"%.2f".format(obs.effectSize)}, " +
                    "n=${obs.nWith}/${obs.nWithout}, mean=${"%.1f".format(obs.meanWith)}/${"%.1f".format(obs.meanWithout)}",
            )
        }
        Result.success()
    } catch (cancellation: kotlinx.coroutines.CancellationException) {
        throw cancellation
    } catch (t: Throwable) {
        Diag.e(DiagnosticArea.BIOMARKER, TAG, "CorrelationWorker fehlgeschlagen", t)
        retryOrFailure()
    }

    companion object {
        private const val TAG = "CorrelationWorker"
        const val UNIQUE_NAME_PERIODIC = "correlation-periodic"
    }
}

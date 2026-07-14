package de.frank.entropyreducer.workers

import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker

internal fun CoroutineWorker.retryOrFailure(maxAttempts: Int = 3): ListenableWorker.Result =
    if (runAttemptCount + 1 < maxAttempts) {
        ListenableWorker.Result.retry()
    } else {
        ListenableWorker.Result.failure()
    }

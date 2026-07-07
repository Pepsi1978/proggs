package de.frank.entropyreducer.util

import kotlinx.coroutines.CancellationException

/**
 * Wie [kotlin.runCatching], aber kooperiert mit Structured Concurrency:
 * [CancellationException] wird durchgereicht statt als [Result.failure]
 * eingefangen.
 *
 * Hintergrund: `kotlin.runCatching` faengt jedes [Throwable], inkl.
 * [CancellationException]. Wird eine Coroutine gecancelt (z.B. weil ein
 * neuer Worker mit `ExistingWorkPolicy.REPLACE` enqueued wurde, oder weil
 * `viewModelScope` abbricht), schluckt das Original-runCatching die
 * Cancellation. Folge: irrefuehrende `E/...fehlgeschlagen`-Logs und
 * `Result.retry()`-Schleifen im WorkManager.
 *
 * Quelle: Roman Elizarov (Kotlin coroutines lead),
 * "Exceptions in coroutines" (2020-09-24,
 * https://elizarov.medium.com/exceptions-in-kotlin-coroutines-fa19d80f7c4a):
 * "If your code uses runCatching ... it must not catch CancellationException".
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (c: CancellationException) {
    throw c
} catch (e: Throwable) {
    Result.failure(e)
}

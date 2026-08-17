package de.frank.perfectmoment.network

import java.util.concurrent.RejectedExecutionException
import java.util.logging.Logger
import okhttp3.OkHttpClient

internal fun OkHttpClient.shutdownSafely(logger: Logger) {
    dispatcher.cancelAll()
    val executor = dispatcher.executorService
    if (executor.isShutdown) return

    try {
        executor.execute {
            try {
                connectionPool.evictAll()
            } catch (error: Exception) {
                logger.warning("OkHttp connection cleanup failed: ${error.message}")
            } finally {
                executor.shutdown()
            }
        }
    } catch (_: RejectedExecutionException) {
        // A concurrent shutdown already owns cleanup.
    }
}

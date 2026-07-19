package de.frank.perfectmoment.network

import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test

class OkHttpShutdownTest {
    @Test
    fun repeatedShutdownTerminatesExecutor() {
        val client = OkHttpClient()
        val logger = Logger.getLogger("OkHttpShutdownTest")

        client.shutdownSafely(logger)
        client.shutdownSafely(logger)

        assertTrue(client.dispatcher.executorService.awaitTermination(2, TimeUnit.SECONDS))
    }
}

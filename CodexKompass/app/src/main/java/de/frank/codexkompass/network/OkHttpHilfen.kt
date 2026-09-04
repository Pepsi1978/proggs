package de.frank.codexkompass.network

import de.frank.codexkompass.observability.KompassLog
import java.io.IOException
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response

/**
 * Wartet auf die Antwort, ohne einen Thread zu blockieren, und bricht den Aufruf ab, wenn die
 * Coroutine abgebrochen wird.
 *
 * Wichtig ist die Unterscheidung in [onFailure]: Ein vom Benutzer abgebrochener Aufruf ist
 * kein Netzwerkfehler. Würde er als solcher gemeldet, erschiene beim Stoppen des Vorlesens
 * jedes Mal eine Fehlermeldung — und ein Wiederholungsversuch liefe sogar noch einmal los.
 */
suspend fun Call.awaitAntwort(): Response = suspendCancellableCoroutine { fortsetzung ->
    fortsetzung.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!fortsetzung.isActive) return
            if (call.isCanceled()) fortsetzung.cancel(e) else fortsetzung.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (fortsetzung.isActive) {
                fortsetzung.resume(response) { _, abgebrochen, _ -> abgebrochen.close() }
            } else {
                response.close()
            }
        }
    })
}

/**
 * Räumt einen OkHttp-Client vollständig ab.
 *
 * Ohne das Herunterfahren des Verbindungspools und des Dispatchers halten dessen Threads den
 * Prozess am Leben, obwohl die App längst nichts mehr tut.
 */
fun OkHttpClient.beendeSanft(modul: String) {
    runCatching { dispatcher.executorService.shutdown() }
        .onFailure { KompassLog.warn(modul, "beendeSanft", "Dispatcher nicht beendbar", mapOf("grund" to it.message)) }
    runCatching { connectionPool.evictAll() }
        .onFailure { KompassLog.warn(modul, "beendeSanft", "Verbindungspool nicht leerbar", mapOf("grund" to it.message)) }
    runCatching { cache?.close() }
}

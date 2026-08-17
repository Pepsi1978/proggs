package de.frank.experimente.tts

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

/**
 * Eine Anfrage abwarten, ohne den Faden zu blockieren — und sie beim Abbruch wirklich
 * abbrechen. Genau daran hing es: wird das Vorlesen gestoppt, muss auch die Synthese des
 * schon vorbereiteten nächsten Absatzes sofort enden, nicht erst nach dem Zeitablauf.
 */
internal suspend fun Call.warteAufAntwort(): Response =
    suspendCancellableCoroutine { fortsetzung ->
        fortsetzung.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!fortsetzung.isCancelled) fortsetzung.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                fortsetzung.resume(response)
            }
        })
    }

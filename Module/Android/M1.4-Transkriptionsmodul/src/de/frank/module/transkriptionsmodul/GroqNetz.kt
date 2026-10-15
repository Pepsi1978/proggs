// ──────────────────────────────────────────────────────────────────────
// Modul M1.4 — Transkriptionsmodul · Stand v1
// Quelle: Module/Android/M1.4-Transkriptionsmodul/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.4 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.transkriptionsmodul

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Ein einziger, langlebiger Netz-Client für Groq. Jede Aufnahme bekam früher einen eigenen und
 * zahlte damit jedes Mal DNS, TCP und TLS neu. Der gemeinsame Pool hält die Verbindung einige
 * Minuten offen — im Gesprächsmodus spart das jede Runde.
 *
 * Nie herunterfahren: Er lebt so lange wie die App.
 */
object GroqNetz {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    @Volatile private var zuletztGewaermt = 0L

    /**
     * Öffnet die Verbindung zu Groq schon beim Aufnahmestart, damit sie beim Hochladen bereitsteht.
     * Die Antwort (ohne Schlüssel: 401) ist egal, es geht nur um den offenen Kanal. Läuft im
     * Hintergrund des Clients und blockiert den Aufrufer nie.
     */
    fun vorwaermen() {
        val jetzt = System.currentTimeMillis()
        // Der Pool hält Verbindungen fünf Minuten — öfter anzuklopfen bringt nichts.
        if (jetzt - zuletztGewaermt < WAERMEN_ABSTAND_MS) return
        zuletztGewaermt = jetzt
        runCatching {
            client.newCall(Request.Builder().url(WAERM_URL).head().build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    zuletztGewaermt = 0L
                    Protokoll.aktiv.netz("Groq vorwärmen gescheitert", e.javaClass.simpleName)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.close()
                }
            })
        }
    }

    private const val WAERM_URL = "https://api.groq.com/openai/v1/models"
    private const val WAERMEN_ABSTAND_MS = 4 * 60_000L
}

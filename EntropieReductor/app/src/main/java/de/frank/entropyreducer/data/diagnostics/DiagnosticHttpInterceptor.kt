package de.frank.entropyreducer.data.diagnostics

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp-Interceptor der HTTP-Fehler der KI-/TTS-APIs zentral ins Diagnose-Log schreibt
 * (Frank-Wunsch 2026-05-23). Diese APIs werden aus 20+ UseCases heraus aufgerufen — ein
 * Interceptor ist die EINE Stelle die jeden Call sieht, statt jeden Aufrufer einzeln zu
 * instrumentieren.
 *
 * Bewusst NUR fuer die KI-/TTS-Hosts: Whoop, Oura, Kalender, Drive und Health Connect
 * haben klare Repository-Sync-Methoden und loggen dort selbst (sonst doppelte Eintraege).
 *
 * Sicherheit: Es werden NUR Methode, Pfad und HTTP-Code geloggt — niemals Header (API-Keys!)
 * oder Bodies. Die IOException wird weitergereicht (nicht verschluckt) — der Aufrufer behaelt
 * seine eigene Fehlerbehandlung (Direktive #3, funktionserhaltend).
 */
@Singleton
class DiagnosticHttpInterceptor
@Inject
constructor(
    private val diagnostics: DiagnosticLogger,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val area = areaForHost(request.url.host) ?: return chain.proceed(request)

        val response =
            try {
                chain.proceed(request)
            } catch (e: IOException) {
                diagnostics.error(
                    area,
                    "Netzwerkfehler bei ${request.method} ${request.url.encodedPath}",
                    e,
                )
                throw e
            }

        if (!response.isSuccessful) {
            val code = response.code
            val path = request.url.encodedPath
            when {
                code == 429 ->
                    diagnostics.warn(area, "Rate-Limit erreicht (HTTP 429) bei $path")
                code == 401 || code == 403 ->
                    diagnostics.error(
                        area,
                        "Zugriff verweigert (HTTP $code) — API-Schluessel pruefen. $path",
                    )
                code in 500..599 ->
                    diagnostics.error(area, "Serverfehler (HTTP $code) bei $path")
                else -> diagnostics.warn(area, "HTTP $code bei ${request.method} $path")
            }
        }
        return response
    }

    private fun areaForHost(host: String): DiagnosticArea? =
        when {
            host.contains("groq.com") -> DiagnosticArea.GROQ
            host.contains("generativelanguage") -> DiagnosticArea.GEMINI
            host.contains("texttospeech") -> DiagnosticArea.GOOGLE_TTS
            host.contains("anthropic.com") -> DiagnosticArea.CLAUDE
            host == "10.8.0.1" -> DiagnosticArea.SECOND_BRAIN
            else -> null
        }
}

package de.frank.jarvis.tunnel

import android.util.Log
import com.ngrok.Forwarder
import com.ngrok.Session
import java.net.URL
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TunnelStufe { AUS, KEIN_TOKEN, VERBINDET, ONLINE, FEHLER }

data class TunnelZustand(
    val stufe: TunnelStufe = TunnelStufe.AUS,
    /** Öffentliche Adresse des Tunnels (https://…), solange er steht. */
    val adresse: String = "",
    val meldung: String = "",
    /** Dauer des letzten Lebenszeichens in Millisekunden; -1 = noch keines. */
    val pingMs: Long = -1,
)

/**
 * Der Tunnel vom Internet direkt in die App: ngrok vergibt eine öffentliche HTTPS-Adresse und leitet jede
 * Anfrage an den MCP-Server auf diesem Handy weiter. Die Verbindung baut das Handy selbst nach außen auf —
 * es braucht keinen eigenen Server und keinen offenen Port.
 */
object Tunnel {
    private const val TAG = "JarvisTunnel"
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _zustand = MutableStateFlow(TunnelZustand())
    val zustand: StateFlow<TunnelZustand> = _zustand.asStateFlow()

    private var lauf: Job? = null
    private var sitzung: Session? = null
    private var weiterleitung: Forwarder.Endpoint? = null

    @Synchronized
    fun starte(token: String, festeAdresse: String, port: Int) {
        stoppe()
        if (token.isBlank()) {
            _zustand.value = TunnelZustand(TunnelStufe.KEIN_TOKEN, meldung = "Tunnel-Schlüssel fehlt")
            return
        }
        lauf = bereich.launch {
            var pause = 3_000L
            while (isActive) {
                _zustand.value = TunnelZustand(TunnelStufe.VERBINDET, meldung = "Tunnel wird aufgebaut …")
                try {
                    com.ngrok.NgrokStart.lade()
                    val neu = Session.withAuthtoken(token)
                        .metadata("Jarvis")
                        .heartbeatInterval(Duration.ofSeconds(20))
                        .heartbeatTolerance(Duration.ofSeconds(15))
                        .heartbeatHandler(object : Session.HeartbeatHandler {
                            override fun heartbeat(durationMs: Long) {
                                val z = _zustand.value
                                if (z.adresse.isNotEmpty()) _zustand.value = z.copy(stufe = TunnelStufe.ONLINE, meldung = "Online", pingMs = durationMs)
                            }

                            override fun timeout() {
                                // ngrok verbindet von selbst neu; bis dahin ehrlich anzeigen.
                                _zustand.value = _zustand.value.copy(stufe = TunnelStufe.VERBINDET, meldung = "Verbindung unterbrochen, verbinde neu …")
                            }
                        })
                        .connect()
                    val bauplan = neu.httpEndpoint().apply { if (festeAdresse.isNotBlank()) domain(festeAdresse) }
                    val ziel = neu.forwardHttp(bauplan, URL("http://127.0.0.1:$port"))
                    synchronized(this@Tunnel) { sitzung = neu; weiterleitung = ziel }
                    _zustand.value = TunnelZustand(TunnelStufe.ONLINE, adresse = ziel.url.trimEnd('/'), meldung = "Online")
                    Log.i(TAG, "Tunnel steht: ${ziel.url}")
                    // Blockiert, bis die Weiterleitung endet (Abbruch, Kontoproblem, Sitzung geschlossen).
                    ziel.join()
                    pause = 3_000L
                } catch (e: Throwable) {
                    if (!isActive) break
                    Log.w(TAG, "Tunnel fehlgeschlagen", e)
                    _zustand.value = TunnelZustand(TunnelStufe.FEHLER, meldung = deute(e))
                }
                schliesse()
                if (!isActive) break
                delay(pause)
                pause = (pause * 2).coerceAtMost(60_000L)
            }
        }
    }

    @Synchronized
    fun stoppe() {
        lauf?.cancel()
        lauf = null
        schliesse()
        _zustand.value = TunnelZustand(TunnelStufe.AUS, meldung = "Aus")
    }

    @Synchronized
    private fun schliesse() {
        val w = weiterleitung
        val s = sitzung
        weiterleitung = null
        sitzung = null
        // Schließen kann kurz blockieren: nicht auf dem aufrufenden Faden.
        if (w != null || s != null) bereich.launch {
            runCatching { w?.close() }
            runCatching { s?.close() }
        }
    }

    /** Macht aus den englischen ngrok-Fehlern einen Satz, mit dem man etwas anfangen kann. */
    private fun deute(e: Throwable): String {
        val text = generateSequence(e) { it.cause }.mapNotNull { it.message }.joinToString(" | ")
        val klein = text.lowercase()
        return when {
            e is UnsatisfiedLinkError || e is NoClassDefFoundError -> "Die Tunnel-Bibliothek ließ sich auf diesem Gerät nicht laden."
            "authtoken" in klein || "authentication failed" in klein || "err_ngrok_105" in klein || "err_ngrok_107" in klein ->
                "Der Tunnel-Schlüssel wird nicht angenommen. Bitte den Authtoken aus dem ngrok-Konto neu einfügen."
            "err_ngrok_108" in klein || "simultaneous" in klein -> "Mit diesem ngrok-Konto läuft schon ein anderer Tunnel. Bitte den anderen beenden."
            "domain" in klein || "err_ngrok_3" in klein -> "Die eingetragene feste Adresse gehört nicht zu diesem ngrok-Konto. Bitte prüfen oder leer lassen."
            "dns" in klein || "resolve" in klein || "network" in klein || "connect" in klein || "timed out" in klein -> "Kein Internet oder ngrok nicht erreichbar. Ich versuche es weiter."
            else -> "Tunnel-Fehler: ${text.take(180).ifEmpty { e.javaClass.simpleName }}"
        }
    }
}

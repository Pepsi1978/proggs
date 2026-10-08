package de.frank.jarvis.tunnel

import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

enum class TunnelStufe { AUS, NICHT_EINGERICHTET, VERBINDET, ONLINE, FEHLER }

data class TunnelZustand(
    val stufe: TunnelStufe = TunnelStufe.AUS,
    val meldung: String = "",
)

/**
 * Die Verbindung nach draußen: Jarvis baut von sich aus eine WebSocket zum eigenen Server auf
 * (Jarvis-Relay, siehe `Jarvis/server`). ChatGPT spricht die öffentliche Adresse des Servers an,
 * der Server reicht jeden Aufruf über diese Verbindung ans Handy weiter und die Antwort zurück.
 * Das Handy braucht dafür keinen offenen Port; bricht die Verbindung ab, wird sie neu aufgebaut.
 */
object Tunnel {
    private const val TAG = "JarvisTunnel"
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** Werkzeugaufrufe laufen nebeneinander, damit ein langer Auftrag kurze Aufrufe nicht aufhält. */
    private val arbeiter = Executors.newFixedThreadPool(4)
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private val _zustand = MutableStateFlow(TunnelZustand())
    val zustand: StateFlow<TunnelZustand> = _zustand.asStateFlow()

    private var lauf: Job? = null
    private var socket: WebSocket? = null
    private var aktuell: Triple<String, String, String>? = null
    /** Weckt die Warteschleife sofort, z. B. wenn das Netz zurück ist. */
    private val anstoss = Channel<Unit>(Channel.CONFLATED)

    /** [verarbeite] bekommt den Rumpf eines MCP-Aufrufs und liefert die Antwort (null = nichts zu antworten). */
    @Synchronized
    fun starte(host: String, token: String, geheimnis: String, verarbeite: (String) -> String?) {
        // Unveränderte Daten und die Schleife läuft: nichts tun. Sonst risse jedes Öffnen der App die Verbindung kurz ab.
        val daten = Triple(host, token, geheimnis)
        if (daten == aktuell && lauf?.isActive == true) return
        stoppe()
        aktuell = daten
        if (host.isBlank() || token.isBlank()) {
            _zustand.value = TunnelZustand(TunnelStufe.NICHT_EINGERICHTET, if (host.isBlank()) "Server nicht eingerichtet" else "Server-Schlüssel fehlt (Einstellungen)")
            return
        }
        lauf = bereich.launch {
            var pause = 2_000L
            while (isActive) {
                _zustand.value = TunnelZustand(TunnelStufe.VERBINDET, "Verbinde mit dem Server …")
                val ende = Channel<String>(Channel.CONFLATED)
                val anfrage = Request.Builder()
                    .url("wss://$host/geraet/ws")
                    .header("X-Jarvis-Token", token)
                    .header("X-Jarvis-Geheimnis", geheimnis)
                    .build()
                val ws = client.newWebSocket(anfrage, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        pause = 2_000L
                        _zustand.value = TunnelZustand(TunnelStufe.ONLINE, "Online")
                        Log.i(TAG, "Verbunden mit $host")
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        arbeiter.execute {
                            val nachricht = runCatching { JSONObject(text) }.getOrNull() ?: return@execute
                            val antwort = runCatching { verarbeite(nachricht.optString("rumpf")) }
                                .getOrElse { """{"jsonrpc":"2.0","id":null,"error":{"code":-32603,"message":"Interner Fehler in Jarvis"}}""" }
                            webSocket.send(JSONObject().put("id", nachricht.optString("id")).put("antwort", antwort ?: JSONObject.NULL).toString())
                        }
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(1000, null) }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { ende.trySend("") }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.w(TAG, "Verbindung verloren: ${t.message}")
                        ende.trySend(deute(t, response))
                    }
                })
                synchronized(this@Tunnel) { socket = ws }
                val grund = try { ende.receive() } finally { ws.cancel() }
                if (!isActive) break
                _zustand.value = if (grund.isEmpty()) TunnelZustand(TunnelStufe.VERBINDET, "Verbinde neu …") else TunnelZustand(TunnelStufe.FEHLER, grund)
                // Warten — oder sofort weiter, wenn das Netz zurückgemeldet wird.
                withTimeoutOrNull(pause) { anstoss.receive() }
                pause = (pause * 2).coerceAtMost(30_000L)
            }
        }
    }

    /** Sofort neu versuchen, statt die Wartezeit abzusitzen (das Netz ist wieder da). */
    fun jetztVerbinden() { anstoss.trySend(Unit) }

    @Synchronized
    fun stoppe() {
        lauf?.cancel()
        lauf = null
        socket?.cancel()
        socket = null
        aktuell = null
        _zustand.value = TunnelZustand(TunnelStufe.AUS, "Aus")
    }

    private fun deute(fehler: Throwable, antwort: Response?): String = when {
        antwort?.code == 404 -> "Der Server lehnt dieses Handy ab (Server-Schlüssel passt nicht)."
        antwort != null -> "Der Server antwortet mit Fehler ${antwort.code}."
        fehler is java.net.UnknownHostException -> "Kein Internet. Ich versuche es weiter."
        fehler is javax.net.ssl.SSLException -> "Sichere Verbindung zum Server gescheitert. Ich versuche es weiter."
        else -> "Server nicht erreichbar. Ich versuche es weiter."
    }
}

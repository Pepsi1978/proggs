package de.frank.jarvis.tunnel

import android.util.Log
import de.frank.jarvis.observability.Logbuch
import java.util.concurrent.ConcurrentLinkedQueue
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
    /** So lange wartet der Server höchstens auf eine Antwort (ZEITLIMIT_S im Relay). */
    private const val NACHSENDEN_MS = 75_000L
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /**
     * Werkzeugaufrufe laufen nebeneinander, damit ein langer Auftrag kurze Aufrufe nicht aufhält. Ohne feste
     * Obergrenze: Vier lange Aufrufe (Bild, Ablage) dürfen einen kurzen nicht in die Warteschlange drängen.
     * Unbenutzte Fäden gibt der Pool nach einer Minute wieder frei.
     */
    private val arbeiter = Executors.newCachedThreadPool()
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
    /** Das Netz, über das die Verbindung gerade läuft (Kennung von Android); 0 = verloren, null = noch unbekannt. */
    private var netz: Long? = null
    /** Die Verbindung, die absichtlich abgerissen wurde (Netzwechsel): ihr Ende ist kein Fehler, nur ein Neuaufbau. */
    @Volatile private var abgebrochen: WebSocket? = null
    /**
     * Die Verbindung, deren Handshake fertig ist (onOpen). Nur über sie gehen Antworten raus: OkHttp nimmt vor onOpen
     * jede Nachricht an und verwirft sie, wenn der Aufbau scheitert.
     */
    @Volatile private var offen: WebSocket? = null
    /**
     * Antworten, die nicht abgingen, weil die Verbindung gerade neu aufgebaut wird. Sie gehen über die nächste
     * Verbindung raus; der Server ordnet sie über die id zu, egal über welche Verbindung sie kommen.
     */
    private val ausstehend = ConcurrentLinkedQueue<Pair<Long, String>>()

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
                        // Unter derselben Sperre wie das Einreihen in sende(): Keine Antwort fällt zwischen beide.
                        synchronized(this@Tunnel) { offen = webSocket; nachsenden(webSocket) }
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        arbeiter.execute {
                            val nachricht = runCatching { JSONObject(text) }.getOrNull() ?: return@execute
                            // Ein Signal des Servers (zum Beispiel „tagebuch“: neuer Eintrag in Drive) ist kein Aufruf und bekommt keine Antwort.
                            if (nachricht.has("signal")) { runCatching { beiSignal?.invoke(nachricht.optString("signal")) }; return@execute }
                            val antwort = runCatching { verarbeite(nachricht.optString("rumpf")) }
                                .getOrElse { Logbuch.fehler("Tunnel: Aufruf verarbeiten", it); """{"jsonrpc":"2.0","id":null,"error":{"code":-32603,"message":"Interner Fehler in Jarvis"}}""" }
                            // Über die aktuelle Verbindung antworten: Riss die alte während des Aufrufs ab, käme die Antwort sonst nie an.
                            sende(JSONObject().put("id", nachricht.optString("id")).put("antwort", antwort ?: JSONObject.NULL).toString())
                        }
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(1000, null) }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        synchronized(this@Tunnel) { if (offen === webSocket) offen = null }
                        ende.trySend("")
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.w(TAG, "Verbindung verloren: ${t.message}")
                        synchronized(this@Tunnel) { if (offen === webSocket) offen = null }
                        ende.trySend(if (webSocket === abgebrochen) "" else deute(t, response))
                    }
                })
                synchronized(this@Tunnel) { socket = ws }
                // Nachgesendet wird nur in onOpen: Vor dem Handshake nähme OkHttp die Antworten an und verlöre sie, wenn der Aufbau scheitert.
                val grund = try { ende.receive() } finally {
                    ws.cancel()
                    // Eine tote Verbindung gilt nicht mehr als aktuell: Ein Netzwechsel in der Pause bricht dann nichts Totes ab.
                    synchronized(this@Tunnel) { if (socket === ws) socket = null; if (offen === ws) offen = null }
                }
                if (!isActive) break
                _zustand.value = if (grund.isEmpty()) TunnelZustand(TunnelStufe.VERBINDET, "Verbinde neu …") else TunnelZustand(TunnelStufe.FEHLER, grund)
                // Warten — oder sofort weiter, wenn das Netz zurückgemeldet wird.
                withTimeoutOrNull(pause) { anstoss.receive() }
                pause = (pause * 2).coerceAtMost(30_000L)
            }
        }
    }

    /** Empfänger für Signale des Servers (Art, z. B. „tagebuch“); setzt der Dienst. */
    @Volatile var beiSignal: ((String) -> Unit)? = null

    /** Sofort neu versuchen, statt die Wartezeit abzusitzen (das Netz ist wieder da). */
    fun jetztVerbinden() { anstoss.trySend(Unit) }

    /**
     * Android meldet ein (neues) Standardnetz. Wechselt es (WLAN ↔ Mobilfunk), hängt die alte Verbindung noch am
     * alten Netz und fiele erst nach dem nächsten ausbleibenden Ping auf (bis ~40 s). Deshalb sofort abreißen und
     * über das neue Netz neu verbinden; laufende Antworten gehen dabei über die neue Verbindung raus.
     * Der erste Aufruf nach dem Anmelden meldet nur das bestehende Netz: dann bleibt alles, wie es ist.
     */
    fun netzGewechselt(kennung: Long) {
        val alt = synchronized(this) { netz.also { netz = kennung } }
        if (alt == null || alt == kennung) return
        if (alt != 0L) {
            synchronized(this) { abgebrochen = socket; socket?.cancel() }
            Logbuch.netz("Netzwechsel", "Verbindung wird über das neue Netz neu aufgebaut")
        }
        anstoss.trySend(Unit)
    }

    /** Das Standardnetz ist weg: Die Verbindung darauf ist tot, nicht erst auf den Ping warten. */
    fun netzVerloren(kennung: Long) {
        synchronized(this) {
            if (netz != kennung) return
            netz = 0L
            abgebrochen = socket
            socket?.cancel()
        }
        Logbuch.netz("Netz verloren", "Verbindung wird neu aufgebaut, sobald wieder ein Netz da ist")
    }

    /**
     * Vergisst das zuletzt bekannte Netz. Der Dienst ruft das vor dem Anmelden des Netz-Rückrufs auf: Dessen erste Meldung
     * gilt dann als Bestandsaufnahme und reißt keine frische Verbindung ab, auch wenn das Netz inzwischen ein anderes ist.
     */
    fun netzVergessen() = synchronized(this) { netz = null; abgebrochen = null }

    /** Schickt eine Antwort über die offene Verbindung; gibt es keine oder nimmt sie nichts an, wartet sie auf die nächste. */
    private fun sende(json: String) {
        val zurueck = synchronized(this) {
            val o = offen
            if (o == null || !o.send(json)) { ausstehend.add(System.currentTimeMillis() to json); true } else false
        }
        if (zurueck) Logbuch.netz("Antwort zurückgehalten", "Verbindung wird neu aufgebaut, sie geht danach raus")
    }

    /** Zurückgehaltene Antworten über [ws] nachsenden. Ältere als das Zeitlimit des Servers wartet dort niemand mehr ab. */
    private fun nachsenden(ws: WebSocket) {
        while (true) {
            val (zeit, json) = ausstehend.poll() ?: break
            if (System.currentTimeMillis() - zeit > NACHSENDEN_MS) continue
            if (!ws.send(json)) { ausstehend.add(zeit to json); break }
            Logbuch.netz("Antwort nachgereicht", "nach ${(System.currentTimeMillis() - zeit) / 1000} s")
        }
    }

    @Synchronized
    fun stoppe() {
        lauf?.cancel()
        lauf = null
        socket?.cancel()
        socket = null
        offen = null
        abgebrochen = null
        aktuell = null
        ausstehend.clear()
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

package de.frank.cortex.vpn

import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import de.frank.cortex.data.SettingsStore
import de.frank.cortex.network.ApiClient
import de.frank.cortex.observability.CortexLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.StringReader

enum class TunnelState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

object WireGuardManager {

    private val _state = MutableStateFlow(TunnelState.DISCONNECTED)
    val state: StateFlow<TunnelState> = _state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var backend: GoBackend? = null
    private var tunnel: Tunnel? = null
    private var config: Config? = null
    // @Volatile: wird von IO-Threads geschrieben und vom Main-Thread (UI) gelesen.
    @Volatile private var lastError: String? = null
    // Verhindert PARALLELE connect-Läufe (Doppel-Tap auf die Pill im CONNECTING-Zustand,
    // VPN-Toggle + onStart-Trigger): zwei konkurrierende backend.setState(UP) racen sonst
    // auf _state/lastError und der zweite kann den ersten faelschlich auf ERROR setzen.
    private val connectInFlight = java.util.concurrent.atomic.AtomicBoolean(false)
    private val configReconnectInFlight = java.util.concurrent.atomic.AtomicBoolean(false)
    // disconnect() waehrend eines laufenden Connect-Versuchs (z.B. Tipp auf "Verbinde…" = Abbruch
    // oder App geht in den Hintergrund): ohne dieses Signal fuehrte der Retry nach dem delay(500)
    // den Aufbau trotzdem zu Ende und ueberschrieb den gewollten Disconnect (Tunnel blieb an).
    @Volatile private var abortConnect = false

    private val tunnelImpl = object : Tunnel {
        override fun getName(): String = "cortex"
        override fun onStateChange(newState: Tunnel.State) {
            val mappedState = when (newState) {
                Tunnel.State.UP -> TunnelState.CONNECTED
                Tunnel.State.DOWN -> TunnelState.DISCONNECTED
                Tunnel.State.TOGGLE -> _state.value
            }
            if (mappedState != _state.value) {
                ApiClient.resetAgentConnections("wireguard_${newState.name.lowercase()}")
            }
            _state.value = mappedState
            CortexLog.info("WireGuard", "onStateChange", "State: $newState")
        }
    }

    fun init(context: android.content.Context) {
        try {
            backend = GoBackend(context)
            tunnel = tunnelImpl
            CortexLog.info("WireGuard", "init", "GoBackend initialisiert")
        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
            CortexLog.error("WireGuard", "init", "GoBackend-Initialisierung fehlgeschlagen: ${e.message}")
            _state.value = TunnelState.ERROR
            lastError = e.message
        }
    }

    /** Reine VALIDIERUNG ohne Seiteneffekt — ersetzt NICHT die aktive Konfiguration.
     *  (parseConfig uebernahm frueher bei jedem parsebaren Tipp-Zwischenstand im Settings-
     *  Editor sofort die aktive Tunnel-Config.) */
    fun validateConfig(configText: String): Boolean = try {
        Config.parse(BufferedReader(StringReader(configText)))
        true
    } catch (_: Exception) {   // no-cancellation-rethrow (kein suspend im try)
        false
    }

    /** Entfernt die aktive Konfiguration (z.B. wenn Frank die Config in den Settings loescht). */
    fun clearConfig() {
        config = null
        CortexLog.info("WireGuard", "clearConfig", "Konfiguration entfernt")
    }

    fun parseConfig(configText: String): Boolean {
        return try {
            val reader = BufferedReader(StringReader(configText))
            config = Config.parse(reader)
            CortexLog.info("WireGuard", "parseConfig", "Konfiguration geparst")
            true
        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
            CortexLog.error("WireGuard", "parseConfig", "Parse-Fehler: ${e.message}")
            lastError = "Konfigurationsfehler: ${e.message}"
            false
        }
    }

    fun loadSavedConfig(): Boolean {
        val saved = SettingsStore.wgConfig
        if (saved.isBlank()) {
            CortexLog.warn("WireGuard", "loadSavedConfig", "Keine gespeicherte Konfiguration")
            return false
        }
        return parseConfig(saved)
    }

    /** Uebernimmt eine bereits geparste neue Config auch in einen laufenden Tunnel. */
    fun reconnectAfterConfigChange() {
        if (_state.value != TunnelState.CONNECTED && _state.value != TunnelState.CONNECTING) return
        if (!configReconnectInFlight.compareAndSet(false, true)) return

        scope.launch {
            try {
                if (!disconnect()) return@launch
                // Ein paralleler alter Connect muss seinen Abbruch erst im finally quittieren,
                // bevor der neue Aufbau die frisch geparste Config sicher erfassen kann.
                while (connectInFlight.get()) delay(25)
                connectInternal()
            } finally {
                configReconnectInFlight.set(false)
            }
        }
    }

    /**
     * Verbindet den Tunnel. Laeuft IMMER im Hintergrund-Thread (Dispatchers.IO), weil
     * GoBackend.setState einen gebundenen VpnService aufbaut — auf dem Main-Thread kann die
     * Service-Bindung beim ERSTEN Aufruf nicht fertig werden (war die Ursache des
     * Fehler/Verbunden-Wackelns). Plus 1 Wiederholversuch fuer den Service-Bind-Race.
     * VORAUSSETZUNG: Die Android-VPN-Erlaubnis ist erteilt (MainActivity ruft VpnService.prepare).
     */
    fun connect() {
        // Ohne erteilte Android-VPN-Erlaubnis wuerde setState(UP) nur kryptisch scheitern.
        // Der Consent-Dialog kann nur aus der MainActivity gestartet werden (VpnService.prepare
        // + Activity-Result) — Aufrufer ohne Activity (z.B. "VPN aktivieren"-Overlay) bekommen
        // hier eine KLARE Meldung statt "Verbindungsfehler: unbekannt".
        try {
            if (android.net.VpnService.prepare(de.frank.cortex.CortexApp.appContext) != null) {
                lastError = "VPN-Berechtigung fehlt — bitte über die VPN-Pille oben verbinden und den Android-Dialog bestätigen."
                _state.value = TunnelState.ERROR
                CortexLog.warn("WireGuard", "connect", "VPN-Erlaubnis fehlt — Consent-Dialog noetig (nur ueber MainActivity moeglich)")
                return
            }
        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
            CortexLog.warn("WireGuard", "connect", "VPN-Consent-Pruefung fehlgeschlagen: ${e.message}")
        }
        scope.launch {
            connectInternal()
        }
    }

    private suspend fun connectInternal(): Boolean = withContext(Dispatchers.IO) {
        val currentConfig = config
        val currentBackend = backend
        val currentTunnel = tunnel

        if (currentConfig == null) {
            CortexLog.error("WireGuard", "connect", "Keine Konfiguration geladen")
            lastError = "Keine Konfiguration geladen"
            _state.value = TunnelState.ERROR
            return@withContext false
        }
        if (currentBackend == null || currentTunnel == null) {
            CortexLog.error("WireGuard", "connect", "Backend nicht initialisiert")
            lastError = "Backend nicht initialisiert"
            _state.value = TunnelState.ERROR
            return@withContext false
        }
        if (_state.value == TunnelState.CONNECTED) return@withContext true
        // Bereits ein Connect unterwegs? Dann NICHT parallel ein zweites setState(UP) starten —
        // aber einen evtl. gesetzten Abbruch-Wunsch ZURUECKNEHMEN: der laufende Versuch fuehrt
        // den Aufbau dann zu Ende. Sonst verpuffte ein "Verbinden"-Tipp im 0,5-2s-Auslauffenster
        // nach einem Abbruch komplett lautlos.
        if (!connectInFlight.compareAndSet(false, true)) {
            if (abortConnect) {
                abortConnect = false
                _state.value = TunnelState.CONNECTING
                CortexLog.info("WireGuard", "connect", "Neuer Verbinden-Wunsch uebernimmt den noch laufenden Versuch")
                return@withContext false
            }
            // Der laufende Versuch koennte GENAU JETZT final fehlschlagen und den Flag im finally
            // wieder freigeben — dann darf dieser Tipp den Aufbau doch noch uebernehmen, statt lautlos
            // zu verpuffen (der In-Flight-Versuch wuerde sonst gleich mit ERROR ueberschreiben). Kurze
            // CAS-Retry-Schleife, solange der Zustand bereits ERROR/DISCONNECTED ist.
            var acquired = false
            var attempts = 0
            while (attempts < 10) {
                val st = _state.value
                if (st != TunnelState.ERROR && st != TunnelState.DISCONNECTED) break
                if (connectInFlight.compareAndSet(false, true)) { acquired = true; break }
                delay(25)
                attempts++
            }
            if (!acquired) return@withContext false
        }

        try {
            abortConnect = false
            _state.value = TunnelState.CONNECTING
            CortexLog.checkpoint(
                step = "vpn_connect",
                intent = "Tunnel starten",
                expected = "connected",
                actual = "connecting",
                ok = false
            )

            var lastEx: Exception? = null
            repeat(2) { attempt ->
                if (abortConnect) {
                    CortexLog.info("WireGuard", "connect", "Verbindungsaufbau auf Wunsch abgebrochen")
                    _state.value = TunnelState.DISCONNECTED
                    return@withContext false
                }
                try {
                    currentBackend.setState(currentTunnel, Tunnel.State.UP, currentConfig)
                    if (abortConnect) {
                        // Disconnect kam GENAU waehrend des Aufbaus: sofort wieder abbauen,
                        // statt den gewollten Trenn-Zustand zu ueberschreiben.
                        try { currentBackend.setState(currentTunnel, Tunnel.State.DOWN, null) } catch (_: Exception) {}
                        _state.value = TunnelState.DISCONNECTED
                        CortexLog.info("WireGuard", "connect", "Aufbau nach Disconnect-Wunsch sofort wieder getrennt")
                        return@withContext false
                    }
                    _state.value = TunnelState.CONNECTED
                    lastError = null
                    CortexLog.checkpoint(
                        step = "vpn_connect",
                        intent = "Tunnel starten",
                        expected = "connected",
                        actual = "connected",
                        ok = true
                    )
                    CortexLog.info("WireGuard", "connect", "Tunnel verbunden (Versuch ${attempt + 1})")
                    return@withContext true
                } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
                    lastEx = e
                    CortexLog.warn("WireGuard", "connect", "Versuch ${attempt + 1} fehlgeschlagen: ${e.message}")
                    // Nur ZWISCHEN den Versuchen warten — nach dem letzten verzoegerte das
                    // delay die Fehleranzeige grundlos um 500 ms.
                    if (attempt == 0) delay(500)
                }
            }

            val e = lastEx
            CortexLog.error("WireGuard", "connect", "Verbindungsfehler: ${e?.message}")
            // lastError VOR dem State publizieren: UI-Collector reagieren auf ERROR und rufen
            // sofort getLastError() — vorher lasen sie den alten/null-Fehlertext.
            lastError = when {
                e?.message?.contains("VPN_NOT_AUTHORIZED") == true ->
                    "VPN-Berechtigung erforderlich — bitte Android-Dialog bestätigen."
                e?.message?.contains("VPN", ignoreCase = true) == true ->
                    "Ein anderes VPN ist aktiv — bitte erst trennen."
                else -> "Verbindungsfehler: ${e?.message ?: "unbekannt"}"
            }
            _state.value = TunnelState.ERROR
            CortexLog.checkpoint(
                step = "vpn_connect",
                intent = "Tunnel starten",
                expected = "connected",
                actual = "error",
                ok = false
            )
            false
        } finally {
            connectInFlight.set(false)
        }
    }

    suspend fun disconnect(): Boolean = withContext(Dispatchers.IO) {
        // Laufenden Connect-Versuch (Retry-Fenster) mit abbrechen.
        abortConnect = true
        val currentBackend = backend
        val currentTunnel = tunnel

        if (currentBackend == null || currentTunnel == null) {
            _state.value = TunnelState.DISCONNECTED
            return@withContext true
        }

        try {
            currentBackend.setState(currentTunnel, Tunnel.State.DOWN, null)
            _state.value = TunnelState.DISCONNECTED
            lastError = null
            CortexLog.info("WireGuard", "disconnect", "Tunnel getrennt")
            true
        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
            CortexLog.error("WireGuard", "disconnect", "Trenn-Fehler: ${e.message}")
            lastError = e.message
            _state.value = TunnelState.ERROR
            false
        }
    }

    /** Wird von MainActivity aufgerufen, wenn der Benutzer den Android-VPN-Dialog ablehnt. */
    fun reportConsentDenied() {
        lastError = "VPN-Berechtigung abgelehnt \u2014 zum Verbinden im Android-Dialog auf \u201EZulassen\u201C tippen."
        _state.value = TunnelState.ERROR
        CortexLog.warn("WireGuard", "consent", "VPN-Berechtigung abgelehnt")
    }

    fun getLastError(): String? = lastError
    fun hasConfig(): Boolean = SettingsStore.wgConfig.isNotBlank()
}

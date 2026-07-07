package de.frank.cortex.vpn

import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import de.frank.cortex.data.SettingsStore
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
    private var lastError: String? = null

    private val tunnelImpl = object : Tunnel {
        override fun getName(): String = "cortex"
        override fun onStateChange(newState: Tunnel.State) {
            _state.value = when (newState) {
                Tunnel.State.UP -> TunnelState.CONNECTED
                Tunnel.State.DOWN -> TunnelState.DISCONNECTED
                Tunnel.State.TOGGLE -> _state.value
            }
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

    /**
     * Verbindet den Tunnel. Laeuft IMMER im Hintergrund-Thread (Dispatchers.IO), weil
     * GoBackend.setState einen gebundenen VpnService aufbaut — auf dem Main-Thread kann die
     * Service-Bindung beim ERSTEN Aufruf nicht fertig werden (war die Ursache des
     * Fehler/Verbunden-Wackelns). Plus 1 Wiederholversuch fuer den Service-Bind-Race.
     * VORAUSSETZUNG: Die Android-VPN-Erlaubnis ist erteilt (MainActivity ruft VpnService.prepare).
     */
    fun connect() {
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
            try {
                currentBackend.setState(currentTunnel, Tunnel.State.UP, currentConfig)
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
                delay(500)
            }
        }

        val e = lastEx
        CortexLog.error("WireGuard", "connect", "Verbindungsfehler: ${e?.message}")
        _state.value = TunnelState.ERROR
        lastError = when {
            e?.message?.contains("VPN_NOT_AUTHORIZED") == true ->
                "VPN-Berechtigung erforderlich — bitte Android-Dialog bestätigen."
            e?.message?.contains("VPN", ignoreCase = true) == true ->
                "Ein anderes VPN ist aktiv — bitte erst trennen."
            else -> "Verbindungsfehler: ${e?.message ?: "unbekannt"}"
        }
        CortexLog.checkpoint(
            step = "vpn_connect",
            intent = "Tunnel starten",
            expected = "connected",
            actual = "error",
            ok = false
        )
        false
    }

    suspend fun disconnect(): Boolean = withContext(Dispatchers.IO) {
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
            _state.value = TunnelState.ERROR
            lastError = e.message
            false
        }
    }

    /** Wird von MainActivity aufgerufen, wenn der Benutzer den Android-VPN-Dialog ablehnt. */
    fun reportConsentDenied() {
        _state.value = TunnelState.ERROR
        lastError = "VPN-Berechtigung abgelehnt \u2014 zum Verbinden im Android-Dialog auf \u201EZulassen\u201C tippen."
        CortexLog.warn("WireGuard", "consent", "VPN-Berechtigung abgelehnt")
    }

    fun getLastError(): String? = lastError
    fun hasConfig(): Boolean = SettingsStore.wgConfig.isNotBlank()
}

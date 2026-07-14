package de.frank.entropyreducer.data.remote.brain

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import java.io.BufferedReader
import java.io.StringReader
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SecondBrainVpnState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

@Singleton
class SecondBrainVpnConnector @Inject constructor(
    @ApplicationContext context: Context,
    private val secrets: EncryptedSecretsStore,
) {
    @Volatile var state: SecondBrainVpnState = SecondBrainVpnState.DISCONNECTED
        private set

    @Volatile private var backend: GoBackend? = null
    @Volatile private var config: Config? = null
    @Volatile private var lastError: String? = null
    // Fix 2026-07-14 (#10a): Hash der aktuell geparsten bzw. der aktiv verbundenen Config —
    // damit connect() eine geaenderte Config erkennt und den Tunnel neu aufbaut.
    @Volatile private var configHash: String? = null
    @Volatile private var connectedConfigHash: String? = null
    private val stateMutex = Mutex()

    private val tunnel = object : Tunnel {
        override fun getName(): String = "entropie-second-brain"

        override fun onStateChange(newState: Tunnel.State) {
            state = when (newState) {
                Tunnel.State.UP -> SecondBrainVpnState.CONNECTED
                Tunnel.State.DOWN -> SecondBrainVpnState.DISCONNECTED
                Tunnel.State.TOGGLE -> state
            }
            Diag.i(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=vpnStateChange expected=state_update actual=$newState ok=true",
            )
        }
    }

    init {
        try {
            backend = GoBackend(context)
            Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=vpnInit expected=backend_ready actual=backend_ready ok=true")
        } catch (e: Exception) {
            state = SecondBrainVpnState.ERROR
            lastError = e.message
            Diag.e(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=vpnInit expected=backend_ready actual=error ok=false message=${e.message ?: e::class.java.simpleName}",
                e,
            )
        }
    }

    fun hasConfig(): Boolean = !secrets.secondBrainWireGuardConfig.isNullOrBlank()

    fun loadSavedConfig(): Boolean {
        val raw = secrets.secondBrainWireGuardConfig.orEmpty()
        if (raw.isBlank()) {
            Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=vpnLoadConfig expected=config_present actual=missing ok=false")
            return false
        }
        return parseConfig(raw)
    }

    fun parseConfig(configText: String): Boolean {
        return try {
            config = Config.parse(BufferedReader(StringReader(configText)))
            configHash = sha256(configText)
            lastError = null
            Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=vpnParseConfig expected=parse_ok actual=parse_ok ok=true")
            true
        } catch (e: Exception) {
            // Fix 2026-07-14 (#10b): State nur auf ERROR setzen, wenn KEIN Tunnel laeuft — sonst
            // wuerde ein fehlgeschlagener Parse einen aktiv verbundenen Tunnel faelschlich als
            // Fehler markieren.
            if (state != SecondBrainVpnState.CONNECTED) {
                state = SecondBrainVpnState.ERROR
            }
            lastError = "WireGuard-Konfiguration ungültig: ${e.message}"
            Diag.e(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=vpnParseConfig expected=parse_ok actual=error ok=false message=${e.message ?: e::class.java.simpleName}",
                e,
            )
            false
        }
    }

    suspend fun connect(): Boolean = stateMutex.withLock { withContext(Dispatchers.IO) {
        val activeBackend = backend
        val activeConfig = config ?: run {
            if (!loadSavedConfig()) return@withContext false
            config
        }
        if (activeBackend == null || activeConfig == null) {
            state = SecondBrainVpnState.ERROR
            lastError = "WireGuard-Backend oder Konfiguration fehlt."
            Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=vpnConnect expected=ready actual=missing_backend_or_config ok=false")
            return@withContext false
        }
        if (state == SecondBrainVpnState.CONNECTED) {
            // Fix 2026-07-14 (#10a): Bei unveraenderter Config sofort zurueck; bei geaenderter
            // Config den bestehenden Tunnel abbauen und mit der neuen Config neu aufbauen.
            if (configHash != null && configHash == connectedConfigHash) return@withContext true
            Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=vpnReconnectConfigChanged expected=reapply_config actual=tunnel_restart ok=true")
            runCatching { activeBackend.setState(tunnel, Tunnel.State.DOWN, null) }
        }

        state = SecondBrainVpnState.CONNECTING
        Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=vpnConnect expected=connected actual=connecting ok=false")
        try {
            activeBackend.setState(tunnel, Tunnel.State.UP, activeConfig)
            state = SecondBrainVpnState.CONNECTED
            connectedConfigHash = configHash
            lastError = null
            Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=vpnConnect expected=connected actual=connected ok=true")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state = SecondBrainVpnState.ERROR
            lastError = e.message
            Diag.e(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=vpnConnect expected=connected actual=error ok=false message=${e.message ?: e::class.java.simpleName}",
                e,
            )
            false
        }
    } }

    suspend fun disconnect(): Boolean = stateMutex.withLock { withContext(Dispatchers.IO) {
        val activeBackend = backend ?: run {
            state = SecondBrainVpnState.DISCONNECTED
            return@withContext true
        }
        try {
            activeBackend.setState(tunnel, Tunnel.State.DOWN, null)
            state = SecondBrainVpnState.DISCONNECTED
            connectedConfigHash = null
            lastError = null
            Diag.i(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=vpnDisconnect expected=disconnected actual=disconnected ok=true")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state = SecondBrainVpnState.ERROR
            lastError = e.message
            Diag.e(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=vpnDisconnect expected=disconnected actual=error ok=false message=${e.message ?: e::class.java.simpleName}",
                e,
            )
            false
        }
    } }

    fun reportConsentDenied() {
        state = SecondBrainVpnState.ERROR
        lastError = "VPN-Berechtigung abgelehnt."
        Diag.w(DiagnosticArea.SECOND_BRAIN, TAG, "CHECKPOINT step=vpnConsent expected=granted actual=denied ok=false")
    }

    fun getLastError(): String? = lastError

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAG = "SecondBrainVpn"
    }
}

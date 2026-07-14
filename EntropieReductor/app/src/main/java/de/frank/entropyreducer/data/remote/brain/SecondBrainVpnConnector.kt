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
import java.util.concurrent.atomic.AtomicInteger
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
    // Fix Loop-2 (#7): Zahl der selbst ausgeloesten Teardown-DOWNs (Reconnect mit geaenderter
    // Config), die der onStateChange-Callback ignorieren soll — verhindert, dass ein verzoegerter
    // DOWN-Callback einen bereits neu gesetzten CONNECTED-Zustand faelschlich ueberschreibt.
    private val pendingSelfTeardowns = AtomicInteger(0)

    private val tunnel = object : Tunnel {
        override fun getName(): String = "entropie-second-brain"

        override fun onStateChange(newState: Tunnel.State) {
            var ignoredSelfTeardown = false
            when (newState) {
                Tunnel.State.UP -> state = SecondBrainVpnState.CONNECTED
                Tunnel.State.DOWN ->
                    // Fix Loop-2 (#7): Beim Reconnect mit geaenderter Config bauen wir den alten
                    // Tunnel per setState(DOWN) ab. Der dadurch verzoegert eintreffende
                    // DOWN-Callback darf den danach neu gesetzten CONNECTED-Zustand NICHT
                    // ueberschreiben — daher einen vorgemerkten Selbst-Teardown konsumieren und
                    // diesen DOWN ignorieren.
                    if (consumeSelfTeardown()) {
                        ignoredSelfTeardown = true
                    } else {
                        state = SecondBrainVpnState.DISCONNECTED
                    }
                Tunnel.State.TOGGLE -> Unit // Zustand unveraendert lassen
            }
            Diag.i(
                DiagnosticArea.SECOND_BRAIN,
                TAG,
                "CHECKPOINT step=vpnStateChange expected=state_update actual=$newState${if (ignoredSelfTeardown) "_ignored_self_teardown" else ""} ok=true",
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
            // Fix Loop-3 (#8): Ein aus einem frueheren, fehlgeschlagenen Reconnect-Versuch eventuell
            // geleaktes Teardown-Guthaben (setState(DOWN) hat geworfen oder war ein No-op → es kam
            // KEIN DOWN-Callback) zuruecksetzen, damit es diesen Zyklus nicht vergiftet und ein
            // spaeteres legitimes externes DOWN nicht faelschlich verschluckt wird.
            pendingSelfTeardowns.set(0)
            // Fix Loop-2 (#7): den durch diesen Teardown ausgeloesten DOWN-Callback vormerken,
            // damit er den gleich neu gesetzten CONNECTED-Zustand nicht faelschlich ueberschreibt.
            // Fix Loop-3 (#8): Increment nur als tatsaechlich erfolgt zaehlen, wenn setState(DOWN)
            // NICHT wirft — andernfalls kommt kein Callback und der Zaehler wuerde dauerhaft leaken;
            // daher im Fehlerfall (geclampt gegen Unterlauf < 0) wieder dekrementieren.
            pendingSelfTeardowns.incrementAndGet()
            runCatching { activeBackend.setState(tunnel, Tunnel.State.DOWN, null) }
                .onFailure { pendingSelfTeardowns.updateAndGet { current -> if (current > 0) current - 1 else 0 } }
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

    /** Konsumiert einen vorgemerkten Selbst-Teardown-DOWN (Fix Loop-2 #7); true = ignorieren. */
    private fun consumeSelfTeardown(): Boolean =
        pendingSelfTeardowns.getAndUpdate { current -> if (current > 0) current - 1 else 0 } > 0

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAG = "SecondBrainVpn"
    }
}

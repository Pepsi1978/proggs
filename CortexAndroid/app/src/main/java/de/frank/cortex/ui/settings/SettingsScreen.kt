package de.frank.cortex.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import de.frank.cortex.BuildConfig
import de.frank.cortex.audio.PcmPlayer
import de.frank.cortex.data.SettingsStore
import de.frank.cortex.data.TtsUsage
import de.frank.cortex.data.TtsUsageStore
import de.frank.cortex.data.model.*
import de.frank.cortex.network.ApiClient
import de.frank.cortex.network.CODEX_GPT56_MODELS
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.ui.theme.*
import de.frank.cortex.vpn.WireGuardManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun SettingsScreen(
    isDark: Boolean = true,
    themeMode: String = "dark",
    onSetThemeMode: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var serverHost by remember { mutableStateOf(SettingsStore.serverHost) }
    var agentPort by remember { mutableStateOf(SettingsStore.agentPort) }
    var brainPort by remember { mutableStateOf(SettingsStore.brainPort) }
    var dashboardPort by remember { mutableStateOf(SettingsStore.dashboardPort) }
    var sbApiKey by remember { mutableStateOf(SettingsStore.sbApiKey) }
    var groqApiKey by remember { mutableStateOf(SettingsStore.groqApiKey) }
    var geminiApiKey by remember { mutableStateOf(SettingsStore.geminiApiKey) }
    var googleTtsApiKey by remember { mutableStateOf(SettingsStore.googleTtsApiKey) }
    var ttsProvider by remember { mutableStateOf(SettingsStore.ttsProvider) }
    var ttsVoice by remember { mutableStateOf(SettingsStore.ttsVoice.removePrefix("de-DE-Chirp3-HD-")) }
    var edgeTtsVoice by remember { mutableStateOf(SettingsStore.edgeTtsVoice) }
    val ttsUsage by TtsUsageStore.usage.collectAsState()
    LaunchedEffect(Unit) { TtsUsageStore.refreshMonth() }
    var recordingToneEnabled by remember { mutableStateOf(SettingsStore.recordingToneEnabled) }
    var recordingToneVolume by remember { mutableStateOf(SettingsStore.recordingToneVolume) }
    var biometricLockEnabled by remember { mutableStateOf(SettingsStore.biometricLockEnabled) }
    var wgConfig by remember { mutableStateOf(SettingsStore.wgConfig) }
    val screenScope = rememberCoroutineScope()
    var agentModelOptions by remember {
        mutableStateOf(listOf("gemini-3.1-flash-lite", "gemini-3.5-flash", "gemini-3-flash-preview", "gemini-2.5-flash", "minimax/minimax-m3") + CODEX_GPT56_MODELS)
    }
    var hauptModel by remember { mutableStateOf(agentModelOptions.first()) }
    var speicherModel by remember { mutableStateOf(agentModelOptions.first()) }
    var abfrageModel by remember { mutableStateOf(agentModelOptions.first()) }
    var reasoningOptions by remember { mutableStateOf(listOf("none", "minimal", "low", "medium", "high", "xhigh")) }
    var hauptReasoning by remember { mutableStateOf("medium") }
    var speicherReasoning by remember { mutableStateOf("medium") }
    var abfrageReasoning by remember { mutableStateOf("medium") }
    // Router (Schritt 1 = Einordnung der Nachricht): eigenes Modell/Thinking, ROUTER_AUTO = wie Hauptagent.
    var routerModel by remember { mutableStateOf(ROUTER_AUTO) }
    var routerReasoning by remember { mutableStateOf(ROUTER_AUTO) }
    var tavilyEnabled by remember { mutableStateOf(true) }
    var memoryWebInfluence by remember { mutableStateOf("normal") }
    var memoryWebInfluenceSaving by remember { mutableStateOf(false) }
    var memoryWebInfluenceStatus by remember { mutableStateOf("Server-Stand wird geladen") }
    var modelPriceLabels by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var agentModelsLoading by remember { mutableStateOf(false) }
    var agentModelsSaving by remember { mutableStateOf(false) }
    var websearchTestRunning by remember { mutableStateOf(false) }
    var agentModelStatus by remember { mutableStateOf("") }
    var websearchTestStatus by remember { mutableStateOf("Schalter-Test prüft ohne echte Websuche, ob Tavily-Aus korrekt auf GPT-Websuche fällt.") }
    var runtimeLimits by remember { mutableStateOf(RuntimeLimits()) }
    var limitDrafts by remember { mutableStateOf(limitsToDrafts(RuntimeLimits())) }
    var limitsSaving by remember { mutableStateOf(false) }
    var limitsStatus by remember { mutableStateOf("Server-Stand wird geladen") }
    var codexConnected by remember { mutableStateOf(false) }
    // rememberSaveable: laufender Auth-Zwischenstand (Code!) und Prompt-Auswahl duerfen eine
    // Rotation/Theme-Aenderung nicht verlieren — der grosse Eingabe-Code war sonst einfach weg.
    var codexStatus by rememberSaveable { mutableStateOf("Server-Codex-Status wird geladen") }
    var codexUserCode by rememberSaveable { mutableStateOf("") }
    var codexAuthId by rememberSaveable { mutableStateOf("") }
    var codexConnecting by remember { mutableStateOf(false) }
    var selectedContextPromptMode by rememberSaveable { mutableStateOf(SettingsStore.RESPONSE_SIZE_AUTO) }
    // Zaehlt lokale Bearbeitungen an Modellen/Schaltern/Limits: eine LANGSAM eintreffende
    // GET-/config-Antwort darf danach getaetigte Eingaben nicht mehr zuruecksetzen.
    var configEditGeneration by remember { mutableIntStateOf(0) }
    // true, sobald irgendein echter Server-Stand (Cache oder frisch) angewendet wurde. Ohne
    // diesen Zustand sendeten die Speichern-Buttons App-DEFAULTS als vermeintlichen Ist-Stand
    // und ueberschrieben Franks echte Server-Konfiguration (Modelle/Limits).
    var configLoaded by remember { mutableStateOf(false) }
    fun loadEditablePrompt(key: String): String = when (key) {
        SettingsStore.RESPONSE_SIZE_AUTO,
        SettingsStore.RESPONSE_SIZE_SHORT,
        SettingsStore.RESPONSE_SIZE_MEDIUM,
        SettingsStore.RESPONSE_SIZE_XL -> SettingsStore.responseSizePrompt(key)
        else -> SettingsStore.contextPrompt(key)
    }
    fun saveEditablePrompt(key: String, prompt: String) {
        when (key) {
            SettingsStore.RESPONSE_SIZE_AUTO,
            SettingsStore.RESPONSE_SIZE_SHORT,
            SettingsStore.RESPONSE_SIZE_MEDIUM,
            SettingsStore.RESPONSE_SIZE_XL -> {
                SettingsStore.setResponseSizePrompt(key, prompt)
                // Zentral zum Server syncen: Dashboard nutzt DENSELBEN Prompt.
                // NonCancellable: der PUT muss auch dann zu Ende laufen, wenn Frank sofort den
                // Tab wechselt (screenScope wird dabei gecancelt). Schlaegt er fehl, wird die
                // Aenderung per pendingEditablePromptSync beim naechsten Connect NACHGESENDET —
                // vorher setzte "Server gewinnt" den Prompt beim naechsten Start still zurueck.
                screenScope.launch {
                    try {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                            ApiClient.agentApi().updateConfig(
                                de.frank.cortex.data.model.AgentConfigRequest(
                                    size_prompt_auto = if (key == SettingsStore.RESPONSE_SIZE_AUTO) prompt else null,
                                    size_prompt_s = if (key == SettingsStore.RESPONSE_SIZE_SHORT) prompt else null,
                                    size_prompt_m = if (key == SettingsStore.RESPONSE_SIZE_MEDIUM) prompt else null,
                                    size_prompt_xl = if (key == SettingsStore.RESPONSE_SIZE_XL) prompt else null
                                )
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        SettingsStore.pendingEditablePromptSync = true
                        CortexLog.warn("Settings", "syncSizePrompt", "Server-Sync fehlgeschlagen (lokal gespeichert, wird nachgeholt): ${e.message}")
                    }
                }
            }
            else -> {
                SettingsStore.setContextPrompt(key, prompt)
                screenScope.launch {
                    try {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                            ApiClient.agentApi().updateConfig(
                                de.frank.cortex.data.model.AgentConfigRequest(
                                    context_prompt_save = if (key == SettingsStore.CONTEXT_MODE_SAVE) prompt else null,
                                    context_prompt_search = if (key == SettingsStore.CONTEXT_MODE_SEARCH) prompt else null
                                )
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        SettingsStore.pendingEditablePromptSync = true
                        CortexLog.warn("Settings", "syncContextPrompt", "Server-Sync fehlgeschlagen (lokal gespeichert, wird nachgeholt): ${e.message}")
                    }
                }
            }
        }
    }
    var contextPromptDraft by rememberSaveable { mutableStateOf(loadEditablePrompt(selectedContextPromptMode)) }
    var contextPromptEditing by rememberSaveable { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // use{}: Stream sicher schliessen (vorher blieb er offen — Ressourcen-Leak).
            val text = try {
                context.contentResolver.openInputStream(it)?.use { s -> s.bufferedReader().readText() }
            } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
                CortexLog.warn("Settings", "wgImport", "Config-Datei nicht lesbar: ${e.message}")
                null
            }
            if (text != null && WireGuardManager.parseConfig(text)) {
                SettingsStore.wgConfig = text
                wgConfig = text
                WireGuardManager.reconnectAfterConfigChange()
            }
        }
    }

    // Wendet eine /config-Antwort auf alle Einstellungs-States an — fuer den lokalen
    // Zwischenspeicher (fromCache=true) und den frischen Server-Stand (Performance 2026-07-12).
    // fromCache: der Cache darf lokale PROMPTS nicht persistieren — er kann aelter sein als
    // eine offline getaetigte lokale Aenderung (die sonst still ueberschrieben wurde).
    fun applyServerConfig(config: de.frank.cortex.data.model.AgentConfigResponse, fromCache: Boolean = false) {
        if (config.available.isNotEmpty()) agentModelOptions = config.available
        if (config.reasoning_available.isNotEmpty()) reasoningOptions = config.reasoning_available
        modelPriceLabels = config.model_prices.mapValues { (_, p) ->
            if (p.input != null && p.output != null)
                "\$${p.input} Input · \$${p.output} Output"
            else "über Abo (nicht pro Token)"
        }
        val models = config.models
        val reasoning = config.reasoning
        hauptModel = models["haupt"] ?: config.model ?: agentModelOptions.first()
        speicherModel = models["speicher"] ?: config.model ?: hauptModel
        abfrageModel = models["abfrage"] ?: config.model ?: hauptModel
        hauptReasoning = reasoning["haupt"] ?: "medium"
        speicherReasoning = reasoning["speicher"] ?: "medium"
        abfrageReasoning = reasoning["abfrage"] ?: "medium"
        routerModel = config.router_model.ifBlank { ROUTER_AUTO }
        routerReasoning = config.router_reasoning.ifBlank { ROUTER_AUTO }
        tavilyEnabled = config.tavily_enabled
        memoryWebInfluence = config.memory_web_influence.takeIf { it in setOf("light", "normal", "strong") } ?: "normal"
        // Prompts nur vom FRISCHEN Server-Stand uebernehmen und nur, wenn keine lokale
        // Offline-Aenderung auf ihren Upload wartet (pendingEditablePromptSync).
        val promptsPullAllowed = !fromCache && !SettingsStore.pendingEditablePromptSync
        if (promptsPullAllowed && config.size_prompts_custom) {
            config.size_prompts.forEach { (k, v) ->
                if (k in setOf("auto", "s", "m", "xl") && v.isNotBlank()) SettingsStore.setResponseSizePrompt(k, v)
            }
        }
        if (promptsPullAllowed && config.context_prompts_custom) {
            config.context_prompts.forEach { (k, v) ->
                if (k in setOf(SettingsStore.CONTEXT_MODE_SAVE, SettingsStore.CONTEXT_MODE_SEARCH) && v.isNotBlank()) {
                    SettingsStore.setContextPrompt(k, v)
                }
            }
        }
        // NIE den Entwurf ueberschreiben, waehrend Frank gerade darin tippt.
        if (!contextPromptEditing) {
            contextPromptDraft = loadEditablePrompt(selectedContextPromptMode)
        }
        runtimeLimits = config.limits
        limitDrafts = limitsToDrafts(config.limits)
        codexConnected = config.codex?.connected == true
        codexStatus = if (codexConnected) "Server verbunden — GPT/Codex-Modelle sind auswählbar" else "Server nicht verbunden"
        configLoaded = true
    }

    LaunchedEffect(Unit) {
        // 1) Zwischengespeicherten Server-Stand SOFORT anzeigen — die Modelle stehen ohne
        //    Wartezeit da (keine Eieruhr); der frische Stand ersetzt sie gleich still.
        val cached = ApiClient.cachedAgentConfig()
        if (cached != null) {
            applyServerConfig(cached, fromCache = true)
            agentModelStatus = "Wird mit dem Server abgeglichen…"
            memoryWebInfluenceStatus = "Wird mit dem Server abgeglichen…"
            limitsStatus = "Wird mit dem Server abgeglichen…"
        } else {
            agentModelsLoading = true
        }
        // 2) Frischen Server-Stand holen und still uebernehmen — aber NUR, wenn Frank in der
        //    Zwischenzeit nichts lokal geaendert hat (sonst setzte die langsame Antwort seine
        //    gerade getaetigte Auswahl kommentarlos zurueck).
        val editGenerationBeforeGet = configEditGeneration
        try {
            val config = ApiClient.agentApi().getConfig()
            ApiClient.cacheAgentConfig(config)
            if (configEditGeneration == editGenerationBeforeGet) {
                applyServerConfig(config)
                agentModelStatus = "Aktueller Server-Stand geladen"
                memoryWebInfluenceStatus = "Aktueller Server-Stand geladen"
                limitsStatus = "Aktuelle Limits geladen"
            } else {
                // Ehrlicher Status: die Antwort wurde wegen einer lokalen Bearbeitung verworfen —
                // "geladen" zu behaupten (und configLoaded=false zu lassen) hinterliess sonst
                // dauerhaft ausgegraute Speichern-Buttons bei gegenteiliger Statuszeile.
                CortexLog.info("Settings", "loadAgentModels", "GET /config verworfen — lokale Bearbeitung war schneller")
                val hint = "Lokale Änderung aktiv — Server-Stand nicht übernommen (Einstellungen erneut öffnen)"
                agentModelStatus = hint
                memoryWebInfluenceStatus = hint
                limitsStatus = hint
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CortexLog.warn("Settings", "loadAgentModels", "Agent-Modelle nicht geladen: ${e.message}")
            if (cached != null) {
                agentModelStatus = "Zwischengespeicherter Stand — Server gerade nicht erreichbar"
                memoryWebInfluenceStatus = "Zwischengespeicherter Stand — Server gerade nicht erreichbar"
                limitsStatus = "Zwischengespeicherte Limits — Server gerade nicht erreichbar"
            } else {
                agentModelStatus = "Server-Stand nicht erreichbar"
                memoryWebInfluenceStatus = "Server-Stand nicht erreichbar"
                limitsStatus = "Limits nicht erreichbar"
            }
        } finally {
            agentModelsLoading = false
        }
    }

    // Nach Activity-Recreation (Rotation/Theme) ueberlebt der Auth-Code per rememberSaveable,
    // aber die Poll-Schleife (screenScope) ist tot — der Code wuerde gross angezeigt, waehrend
    // NIEMAND mehr pollt und die Verbindung nie zustande kommen kann. Ehrlich aufraeumen.
    LaunchedEffect(Unit) {
        if (codexUserCode.isNotBlank() && !codexConnecting) {
            codexUserCode = ""
            codexAuthId = ""
            codexStatus = "Verbindungsvorgang unterbrochen — bitte erneut auf Verbinden tippen"
        }
    }

    fun requestSecretReveal(onUnlocked: () -> Unit) {
        if (!SettingsStore.biometricLockEnabled) {
            onUnlocked()
            return
        }
        val activity = context as? FragmentActivity
        if (activity == null) {
            Toast.makeText(context, "Biometrie nicht verfügbar", Toast.LENGTH_SHORT).show()
            return
        }
        val available = BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (available != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(context, "Biometrie nicht verfügbar", Toast.LENGTH_SHORT).show()
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Schlüssel anzeigen")
            .setSubtitle("API-Schlüssel separat entsperren")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(info)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // === VERBINDUNG & VPN ===
        SectionHeader("VERBINDUNG & VPN")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column {
                // WG Config row
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.VpnKey, null, tint = Iris, modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WireGuard-Konfiguration", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (wgConfig.isNotBlank()) "Konfiguration vorhanden" else "Keine Konfiguration",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.5.sp,
                            color = if (wgConfig.isNotBlank()) Mint else Rose
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (isDark) DarkChip else LightChip,
                        border = BorderStroke(1.dp, if (isDark) DarkChipBorder else LightChipBorder),
                        modifier = Modifier.clickable { filePicker.launch("*/*") }
                    ) {
                        Row(
                            modifier = Modifier.height(32.dp).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(16.dp))
                            Text("Import", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                // Config textarea
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = if (isDark) DarkField else LightField,
                    border = BorderStroke(1.dp, if (isDark) DarkFieldBorder else LightFieldBorder),
                    modifier = Modifier.padding(12.dp).fillMaxWidth()
                ) {
                    BasicTextField(
                        value = wgConfig,
                        onValueChange = {
                            wgConfig = it
                            when {
                                // Loeschen explizit erlauben: vorher blieb die alte Config
                                // persistiert (und nach Neustart AKTIV), obwohl der Editor
                                // leer aussah — Entfernen war unmoeglich.
                                it.isBlank() -> {
                                    SettingsStore.wgConfig = ""
                                    WireGuardManager.clearConfig()
                                }
                                // Erst VALIDIEREN (ohne Seiteneffekt), dann uebernehmen:
                                // parseConfig ersetzte sonst bei jedem parsebaren Tipp-
                                // Zwischenstand sofort die AKTIVE Tunnel-Konfiguration.
                                WireGuardManager.validateConfig(it) -> {
                                    SettingsStore.wgConfig = it
                                    WireGuardManager.parseConfig(it)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(10.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = JetBrainsMono,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { inner ->
                            if (wgConfig.isEmpty()) {
                                Text("\u2026oder Konfiguration hier einfügen",
                                    fontFamily = JetBrainsMono, fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inner()
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                // Server-Host
                Row(
                    modifier = Modifier.padding(13.dp, 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Server-Host", fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(96.dp))
                    // Nur GUELTIGE Werte persistieren: ein leerer Host oder Whitespace erzeugte
                    // eine unbrauchbare Basis-URL — Retrofit warf dann bei JEDEM Aufruf eine
                    // IllegalArgumentException und Chat/Dashboard/Sync waren komplett tot.
                    SettingsTextField(serverHost, { input ->
                        serverHost = input
                        // Nur Hostname/IP-Zeichen zulassen: "http://host" oder "host:port" bauten
                        // sonst weiterhin eine unbrauchbare Basis-URL (IllegalArgumentException
                        // bei jedem Request), obwohl die Validierung genau das verhindern soll.
                        input.trim().takeIf { it.isNotEmpty() && it.matches(Regex("[A-Za-z0-9.\\-]+")) }
                            ?.let { SettingsStore.serverHost = it }
                    }, Modifier.weight(1f))
                }

                // Ports row (nur Ziffern, 1-65535 — ungueltige Zwischenstaende bleiben lokal)
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsTextField(agentPort, { input ->
                        agentPort = input.filter { it.isDigit() }.take(5)
                        agentPort.toIntOrNull()?.takeIf { it in 1..65535 }
                            ?.let { SettingsStore.agentPort = it.toString() }
                    }, Modifier.weight(1f), center = true, hint = "Agent")
                    SettingsTextField(brainPort, { input ->
                        brainPort = input.filter { it.isDigit() }.take(5)
                        brainPort.toIntOrNull()?.takeIf { it in 1..65535 }
                            ?.let { SettingsStore.brainPort = it.toString() }
                    }, Modifier.weight(1f), center = true, hint = "Brain")
                    SettingsTextField(dashboardPort, { input ->
                        dashboardPort = input.filter { it.isDigit() }.take(5)
                        dashboardPort.toIntOrNull()?.takeIf { it in 1..65535 }
                            ?.let { SettingsStore.dashboardPort = it.toString() }
                    }, Modifier.weight(1f), center = true, hint = "Dashboard")
                }
            }
        }

        // === SCHLÜSSEL ===
        SectionHeader("SCHLÜSSEL")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column {
                SecretRow("Server-Schlüssel (SB_API_KEY)", sbApiKey,
                    { sbApiKey = it; SettingsStore.sbApiKey = it }, isDark,
                    divider = true,
                    onRevealRequest = ::requestSecretReveal)
                SecretRow("Groq-Schlüssel (Spracheingabe)", groqApiKey,
                    { groqApiKey = it; SettingsStore.groqApiKey = it }, isDark,
                    divider = true,
                    onRevealRequest = ::requestSecretReveal)
                SecretRow("Gemini-Schlüssel (Verbessern + TTS-Fallback)", geminiApiKey,
                    { geminiApiKey = it; SettingsStore.geminiApiKey = it }, isDark,
                    divider = true,
                    onRevealRequest = ::requestSecretReveal)
                SecretRow("Google TTS-Schlüssel (Chirp 3: HD Vorlesen)", googleTtsApiKey,
                    { googleTtsApiKey = it; SettingsStore.googleTtsApiKey = it }, isDark,
                    divider = true,
                    onRevealRequest = ::requestSecretReveal)
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Login, null, tint = Orange, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("OpenAI Codex / ChatGPT", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(codexStatus, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (codexConnecting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Orange)
                    }
                    if (codexUserCode.isNotBlank()) {
                        Text(
                            codexUserCode,
                            fontFamily = JetBrainsMono,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = Orange
                        )
                        Text("Diesen Code auf der geöffneten OpenAI-Seite eingeben. Die Tokens werden auf dem Server gespeichert.", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (codexConnected) {
                        Text(
                            "Jetzt unten im Abschnitt KI bei Hauptagent/Speicheragent/Abfrageagent ein GPT/Codex-Modell wählen und speichern.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                screenScope.launch {
                                    codexConnecting = true
                                    try {
                                        val start = ApiClient.agentApi().startCodexAuth()
                                        codexAuthId = start.auth_id
                                        codexUserCode = start.user_code
                                        codexStatus = "Browser öffnen und Code eingeben"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(start.verification_uri)))
                                        // interval VOR der Division absichern (interval=0 wuerde crashen) und
                                        // JEDE Poll-Runde einzeln fehlertolerant machen: der Browser-Wechsel
                                        // trennt das VPN (App im Hintergrund) — ein einzelner fehlgeschlagener
                                        // Poll darf den ganzen Auth-Vorgang nicht mehr abbrechen; nach der
                                        // Rueckkehr in die App verbindet das VPN neu und der Poll greift wieder.
                                        val pollInterval = start.interval.coerceAtLeast(3)
                                        // Poll-Schleife liefert nur das ERGEBNIS; der getConfig-
                                        // Abgleich passiert danach in einem EIGENEN try: ein
                                        // transienter /config-Fehler nach dem Erfolg lief sonst
                                        // in den Poll-catch, pollte weiter (Server hat die
                                        // auth_id nach Erfolg entfernt) und meldete garantiert
                                        // "Code abgelaufen" — obwohl die Verbindung stand.
                                        var outcome = "timeout"
                                        var round = 0
                                        val maxRounds = (start.expires_in / pollInterval).coerceAtMost(180)
                                        while (round < maxRounds && outcome == "timeout") {
                                            round++
                                            delay((pollInterval * 1000).toLong())
                                            try {
                                                val poll = ApiClient.agentApi().pollCodexAuth(de.frank.cortex.data.model.CodexAuthPollRequest(codexAuthId))
                                                when (poll.status) {
                                                    "connected" -> outcome = "connected"
                                                    "expired" -> outcome = "expired"
                                                }
                                            } catch (e: CancellationException) {
                                                throw e
                                            } catch (e: Exception) {
                                                CortexLog.warn("Settings", "codexAuth", "Poll-Runde fehlgeschlagen (weiter bis Ablauf): ${e.message}")
                                            }
                                        }
                                        if (outcome == "connected") {
                                            codexConnected = true
                                            codexUserCode = ""
                                            codexStatus = "Server verbunden — GPT/Codex-Modelle sind auswählbar"
                                            try {
                                                val config = ApiClient.agentApi().getConfig()
                                                if (config.available.isNotEmpty()) agentModelOptions = config.available
                                                ApiClient.cacheAgentConfig(config)   // Cache bleibt synchron zum neuen Codex-Stand
                                            } catch (e: CancellationException) {
                                                throw e
                                            } catch (e: Exception) {
                                                CortexLog.warn("Settings", "codexAuth", "Modell-Abgleich nach Verbindung fehlgeschlagen (nur Anzeige betroffen): ${e.message}")
                                            }
                                            codexConnecting = false
                                            return@launch
                                        }
                                        codexUserCode = ""
                                        codexStatus = "Code abgelaufen"
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        val message = codexAuthErrorMessage(e)
                                        CortexLog.error("Settings", "codexAuth", "Codex-Verbindung fehlgeschlagen: $message")
                                        codexStatus = message
                                    } finally {
                                        codexConnecting = false
                                    }
                                }
                            },
                            enabled = !codexConnecting,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Orange),
                            modifier = Modifier.weight(1f)
                        ) { Text("Verbinden", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                        Button(
                            onClick = {
                                screenScope.launch {
                                    try {
                                        ApiClient.agentApi().disconnectCodex()
                                        codexConnected = false
                                        codexUserCode = ""
                                        codexStatus = "Server nicht verbunden"
                                        // Eigenes try: schlaegt nur der Config-Refresh fehl, ist das
                                        // Trennen trotzdem gelungen — der Toast "Trennen fehlgeschlagen"
                                        // waere falsch (und der Cache wird beim naechsten GET korrigiert).
                                        try {
                                            val config = ApiClient.agentApi().getConfig()
                                            if (config.available.isNotEmpty()) agentModelOptions = config.available
                                            ApiClient.cacheAgentConfig(config)   // Cache bleibt synchron zum getrennten Stand
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            CortexLog.warn("Settings", "codexDisconnect", "Config-Refresh nach Trennen fehlgeschlagen: ${e.message}")
                                        }
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Trennen fehlgeschlagen", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("Trennen", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }

        // === SPRACHE & STIMME ===
        SectionHeader("SPRACHE & STIMME")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column {
                // Der fruehere "Antworten vorlesen"-Schalter wurde bewusst ENTFERNT (Frank-Wunsch
                // 2026-07-02): Das Vorlesen wird ausschliesslich ueber den Lautsprecher-Knopf im
                // Gespraech gesteuert (an = immer vorlesen, aus = nie) — EIN Schalter statt zwei.

                // Vorlese-Motor (TTS-Provider) — Chirp 3: HD (Google) vs. Edge (Microsoft)
                val isEdgeSel = ttsProvider == SettingsStore.TTS_PROVIDER_EDGE
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Vorlese-Motor", fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TtsEngineChip(
                            title = "Chirp 3: HD",
                            subtitle = "Google · viel gratis",
                            selected = !isEdgeSel,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        ) {
                            ttsProvider = SettingsStore.TTS_PROVIDER_CHIRP
                            SettingsStore.ttsProvider = SettingsStore.TTS_PROVIDER_CHIRP
                        }
                        TtsEngineChip(
                            title = "Edge",
                            subtitle = "Microsoft · kostenlos",
                            selected = isEdgeSel,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        ) {
                            ttsProvider = SettingsStore.TTS_PROVIDER_EDGE
                            SettingsStore.ttsProvider = SettingsStore.TTS_PROVIDER_EDGE
                        }
                    }
                    Text(
                        if (isEdgeSel) "Edge-TTS von Microsoft — kostenlos, natürliche Stimmen (Seraphina, Florian …)."
                        else "Chirp 3: HD von Google Cloud — beste Qualität, gratis bis 1 Mio. Zeichen/Monat.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TtsUsageIndicator(ttsUsage)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 14.dp))

                // Voice selector (engine-aware: Chirp-Stimmen ODER Edge-Stimmen)
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.RecordVoiceOver, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp))
                    Text(if (isEdgeSel) "Edge-Stimme" else "Chirp-Stimme", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    var showVoiceDialog by remember { mutableStateOf(false) }
                    var showEdgeVoiceDialog by remember { mutableStateOf(false) }
                    val pcmPlayer = remember { PcmPlayer() }
                    val edgeSynth = remember { de.frank.cortex.audio.EdgeTtsSynthesizer() }
                    val edgeMp3Player = remember { de.frank.cortex.audio.Mp3Player(context) }
                    val scope = rememberCoroutineScope()
                    // Vorherigen Test-Job CANCELN, nicht nur den Player stoppen: steckte Test A
                    // noch in der Synthese, unterbrach sein verspaetetes playAndAwait sonst die
                    // laengst laufende Wiedergabe von Test B (falsche Stimme erklang zuletzt).
                    var voiceTestJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (isDark) DarkField else LightField,
                        border = BorderStroke(1.dp, if (isDark) DarkFieldBorder else LightFieldBorder),
                        modifier = Modifier.clickable { if (isEdgeSel) showEdgeVoiceDialog = true else showVoiceDialog = true }
                    ) {
                        Text(
                            if (isEdgeSel) edgeVoiceLabelFor(edgeTtsVoice) else voiceLabelFor(ttsVoice),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (showVoiceDialog) {
                        VoicePickerDialog(
                            currentVoice = ttsVoice,
                            onSelect = { voice ->
                                ttsVoice = voice
                                SettingsStore.ttsVoice = voice.removePrefix("de-DE-Chirp3-HD-")
                                showVoiceDialog = false
                            },
                            onDismiss = { showVoiceDialog = false },
                            onTestVoice = { voice ->
                                voiceTestJob?.cancel()
                                voiceTestJob = scope.launch {
                                    try {
                                        pcmPlayer.stop()
                                        val label = voiceLabelFor(voice)
                                        val pcm = ApiClient.geminiTts(
                                            "Hallo, ich bin $label. Willkommen bei Cortex, deinem zweiten Gehirn.",
                                            voice
                                        )
                                        pcmPlayer.playAndAwait(pcm, SettingsStore.ttsRate)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        CortexLog.error("Settings", "testVoice", "TTS-Test fehlgeschlagen: ${e.message}")
                                        Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                    if (showEdgeVoiceDialog) {
                        EdgeVoicePickerDialog(
                            currentVoice = edgeTtsVoice,
                            onSelect = { voiceId ->
                                edgeTtsVoice = voiceId
                                SettingsStore.edgeTtsVoice = voiceId
                                showEdgeVoiceDialog = false
                            },
                            onDismiss = { showEdgeVoiceDialog = false },
                            onTestVoice = { voiceId ->
                                voiceTestJob?.cancel()
                                voiceTestJob = scope.launch {
                                    try {
                                        edgeMp3Player.stop()
                                        val short = edgeVoiceShortFor(voiceId)
                                        val mp3 = edgeSynth.synthesize(
                                            "Hallo, ich bin $short. Willkommen bei Cortex, deinem zweiten Gehirn.",
                                            voiceId
                                        )
                                        edgeMp3Player.playAndAwait(mp3, SettingsStore.ttsRate)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        CortexLog.error("Settings", "testEdgeVoice", "Edge-TTS-Test fehlgeschlagen: ${e.message}")
                                        Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 14.dp))

                // Sprechtempo (Design: 0.7–1.4, Schritt 0.05, Standard 1.0)
                Column(modifier = Modifier.padding(14.dp)) {
                    var ttsRate by remember { mutableStateOf(SettingsStore.ttsRate) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sprechtempo", fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(
                            String.format(java.util.Locale.US, "%.2f×", ttsRate),
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = ttsRate,
                        onValueChange = { ttsRate = it },
                        onValueChangeFinished = { SettingsStore.ttsRate = ttsRate },
                        valueRange = 0.7f..1.4f,
                        steps = 13,
                        colors = SliderDefaults.colors(thumbColor = Orange, activeTrackColor = Orange)
                    )
                }
            }
        }

        // === TÖNE ===
        SectionHeader("TÖNE")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.GraphicEq, null, tint = Orange, modifier = Modifier.size(21.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Aufnahmeton", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Signal beim Start der Aufnahme", fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (recordingToneEnabled) Orange else if (isDark) DarkFieldBorder else LightFieldBorder,
                        modifier = Modifier
                            .width(46.dp)
                            .height(27.dp)
                            .clickable {
                                recordingToneEnabled = !recordingToneEnabled
                                SettingsStore.recordingToneEnabled = recordingToneEnabled
                            }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(3.dp),
                            contentAlignment = if (recordingToneEnabled) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(21.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.background)
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 14.dp))
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lautstärke", fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(
                            // Math.round statt toInt: 0.45f*100 = 44.999… zeigte sonst "44%".
                            "${Math.round(recordingToneVolume * 100f)}%",
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = recordingToneVolume,
                        onValueChange = { recordingToneVolume = it },
                        onValueChangeFinished = { SettingsStore.recordingToneVolume = recordingToneVolume },
                        valueRange = 0f..1f,
                        steps = 19,
                        enabled = recordingToneEnabled,
                        colors = SliderDefaults.colors(thumbColor = Orange, activeTrackColor = Orange)
                    )
                }
            }
        }

        // === SICHERHEIT ===
        SectionHeader("SICHERHEIT")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Fingerprint, null, tint = Iris, modifier = Modifier.size(22.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Fingerabdruck-Schutz", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Beim Öffnen der App entsperren", fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (biometricLockEnabled) Iris else if (isDark) DarkFieldBorder else LightFieldBorder,
                    modifier = Modifier
                        .width(46.dp)
                        .height(27.dp)
                        .clickable {
                            biometricLockEnabled = !biometricLockEnabled
                            SettingsStore.biometricLockEnabled = biometricLockEnabled
                            // Recents-Thumbnail-Schutz sofort mitschalten (sonst erst ab dem
                            // naechsten App-Start): FLAG_SECURE verhindert, dass die Task-
                            // Uebersicht den entsperrten Inhalt zeigt.
                            (context as? FragmentActivity)?.window?.let { win ->
                                if (biometricLockEnabled) {
                                    win.setFlags(
                                        android.view.WindowManager.LayoutParams.FLAG_SECURE,
                                        android.view.WindowManager.LayoutParams.FLAG_SECURE
                                    )
                                } else {
                                    win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                                }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(3.dp),
                        contentAlignment = if (biometricLockEnabled) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(21.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }
                }
            }
        }

        // === KI ===
        SectionHeader("KI")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Psychology, null, tint = Orange, modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Agent-Modelle", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Synchron mit Bibliothekar-Agent im Dashboard", fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (agentModelsLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Orange)
                    }
                }

                AgentModelDropdown("Hauptagent", hauptModel, agentModelOptions, isDark, priceLabels = modelPriceLabels) { hauptModel = it; configEditGeneration++ }
                if (modelSupportsReasoning(hauptModel)) {
                    AgentModelDropdown("Thinking", hauptReasoning, reasoningOptions, isDark) { hauptReasoning = it; configEditGeneration++ }
                }
                // Router (Schritt 1): darf ein eigenes (schnelles) Modell + eigenes Thinking nutzen;
                // "auto (wie Hauptagent)" = exakt bisheriges Verhalten. Die Antwort (Schritt 2)
                // formuliert immer der Hauptagent.
                AgentModelDropdown(
                    "Router", routerModel, listOf(ROUTER_AUTO) + agentModelOptions, isDark,
                    priceLabels = modelPriceLabels + (ROUTER_AUTO to "übernimmt Modell + Thinking vom Hauptagenten")
                ) { routerModel = it; configEditGeneration++ }
                val effectiveRouterModel = if (routerModel == ROUTER_AUTO) hauptModel else routerModel
                if (routerModel != ROUTER_AUTO && modelSupportsReasoning(effectiveRouterModel)) {
                    AgentModelDropdown("Thinking", routerReasoning, listOf(ROUTER_AUTO) + reasoningOptions, isDark) { routerReasoning = it; configEditGeneration++ }
                }
                AgentModelDropdown("Speicheragent", speicherModel, agentModelOptions, isDark, priceLabels = modelPriceLabels) { speicherModel = it; configEditGeneration++ }
                if (modelSupportsReasoning(speicherModel)) {
                    AgentModelDropdown("Thinking", speicherReasoning, reasoningOptions, isDark) { speicherReasoning = it; configEditGeneration++ }
                }
                AgentModelDropdown("Abfrageagent", abfrageModel, agentModelOptions, isDark, priceLabels = modelPriceLabels) { abfrageModel = it; configEditGeneration++ }
                if (modelSupportsReasoning(abfrageModel)) {
                    AgentModelDropdown("Thinking", abfrageReasoning, reasoningOptions, isDark) { abfrageReasoning = it; configEditGeneration++ }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.TravelExplore, null, tint = Orange, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Websearch-Modell Tavily", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (tavilyEnabled) "Tavily wird für Internetfragen genutzt" else "Tavily ist aus; GPT-5.5 kann eigene Websuche nutzen",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(if (tavilyEnabled) "An" else "Aus", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (tavilyEnabled) Orange else MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(
                        checked = tavilyEnabled,
                        onCheckedChange = { tavilyEnabled = it; configEditGeneration++ },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Orange)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        websearchTestStatus,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            screenScope.launch {
                                websearchTestRunning = true
                                websearchTestStatus = "Websuche-Test läuft..."
                                try {
                                    val result = ApiClient.agentApi().websearchToggleSelftest()
                                    val current = if (result.current?.tavily_enabled == true) "Tavily an" else "Tavily aus"
                                    websearchTestStatus = if (result.ok) {
                                        "Websuche-Test bestanden: ${result.passed}/${result.total} · aktuell: $current"
                                    } else {
                                        "Websuche-Test fehlgeschlagen: ${result.passed}/${result.total}"
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    CortexLog.error("Settings", "websearchToggleSelftest", "Websuche-Test fehlgeschlagen: ${e.message}")
                                    websearchTestStatus = "Websuche-Test nicht erreichbar"
                                    Toast.makeText(context, "Websuche-Test nicht erreichbar", Toast.LENGTH_SHORT).show()
                                } finally {
                                    websearchTestRunning = false
                                }
                            }
                        },
                        enabled = !websearchTestRunning,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Orange.copy(alpha = 0.6f))
                    ) {
                        if (websearchTestRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Orange)
                        } else {
                            Text("Websuche testen", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Orange)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        agentModelStatus,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            screenScope.launch {
                                agentModelsSaving = true
                                try {
                                    // NonCancellable: sofortiger Tab-Wechsel nach "Speichern" cancelt den
                                    // screenScope — der PUT muss trotzdem zu Ende laufen (vorher kamen die
                                    // Modelle nie auf dem Server an, ohne jedes Feedback).
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                                        ApiClient.agentApi().updateConfig(
                                            de.frank.cortex.data.model.AgentConfigRequest(
                                                haupt_model = hauptModel,
                                                speicher_model = speicherModel,
                                                abfrage_model = abfrageModel,
                                                haupt_reasoning = hauptReasoning,
                                                speicher_reasoning = speicherReasoning,
                                                abfrage_reasoning = abfrageReasoning,
                                                router_model = if (routerModel == ROUTER_AUTO) "auto" else routerModel,
                                                router_reasoning = if (routerReasoning == ROUTER_AUTO) "auto" else routerReasoning,
                                                tavily_enabled = tavilyEnabled
                                            )
                                        )
                                        // /config-Cache konsistent halten: sonst zeigte der naechste
                                        // Oeffnen-Vorgang (bzw. offline dauerhaft) die ALTEN Modelle.
                                        try {
                                            ApiClient.cacheAgentConfig(ApiClient.agentApi().getConfig())
                                        } catch (inner: Exception) {
                                            CortexLog.warn("Settings", "saveAgentModels", "Cache-Refresh nach Speichern fehlgeschlagen: ${inner.message}")
                                        }
                                    }
                                    agentModelStatus = "Modelle gespeichert"
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    CortexLog.error("Settings", "saveAgentModels", "Speichern fehlgeschlagen: ${e.message}")
                                    agentModelStatus = "Speichern fehlgeschlagen"
                                    Toast.makeText(context, "Modelle konnten nicht gespeichert werden", Toast.LENGTH_SHORT).show()
                                } finally {
                                    agentModelsSaving = false
                                }
                            }
                        },
                        // configLoaded: ohne je geladenen Server-Stand wuerden hier App-Defaults
                        // als vermeintlicher Ist-Stand gesendet und Franks Konfiguration ersetzt.
                        enabled = !agentModelsSaving && configLoaded,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Orange)
                    ) {
                        if (agentModelsSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Speichern", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // === QDRANT-/QUADRANT-LIMITS ===
        SectionHeader("QDRANT-LIMITS")
        LimitSettingsCard(
            isDark = isDark,
            drafts = limitDrafts,
            status = limitsStatus,
            saving = limitsSaving,
            saveEnabled = configLoaded,
            brainAvailable = runtimeLimits.brain != null,
            onDraftChange = { key, value ->
                configEditGeneration++
                limitDrafts = limitDrafts + (key to value.filter { it.isDigit() }.take(7))
            },
            onSave = {
                screenScope.launch {
                    limitsSaving = true
                    try {
                        val next = draftsToLimits(runtimeLimits, limitDrafts)
                        val response = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                            val r = ApiClient.agentApi().updateConfig(
                                AgentConfigRequest(limits = next)
                            )
                            // /config-Cache aktualisieren: buildContextPrompt kappt gegen den
                            // GECACHTEN Wert — ein gesenktes context_prompt_max_chars griffe
                            // sonst erst beim naechsten Cache-Refresh (422er bis dahin).
                            try {
                                ApiClient.cacheAgentConfig(ApiClient.agentApi().getConfig())
                            } catch (inner: Exception) {
                                CortexLog.warn("Settings", "saveLimits", "Cache-Refresh nach Speichern fehlgeschlagen: ${inner.message}")
                            }
                            r
                        }
                        runtimeLimits = response.limits
                        limitDrafts = limitsToDrafts(response.limits)
                        limitsStatus = "Limits gespeichert und sofort aktiv"
                        Toast.makeText(context, "Qdrant-Limits gespeichert", Toast.LENGTH_SHORT).show()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        CortexLog.error("Settings", "saveLimits", "Limit-Speichern fehlgeschlagen: ${e.message}")
                        limitsStatus = "Speichern fehlgeschlagen"
                        Toast.makeText(context, "Limits konnten nicht gespeichert werden", Toast.LENGTH_SHORT).show()
                    } finally {
                        limitsSaving = false
                    }
                }
            }
        )

        // === KONTEXT ===
        SectionHeader("KONTEXT")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Tune, null, tint = Orange, modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modus-Prompts", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Zusatzauftrag für Modi und Antwortlängen A/S/M/XL", fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                ContextPromptModeButtons(
                    selectedMode = selectedContextPromptMode,
                    isDark = isDark,
                    onSelect = { mode ->
                        selectedContextPromptMode = mode
                        contextPromptDraft = loadEditablePrompt(mode)
                        contextPromptEditing = false
                    }
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) DarkField else LightField,
                    border = BorderStroke(1.dp, if (contextPromptEditing) Orange.copy(alpha = 0.55f) else if (isDark) DarkFieldBorder else LightFieldBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BasicTextField(
                        value = contextPromptDraft,
                        onValueChange = { if (contextPromptEditing) contextPromptDraft = it.take(4000) },
                        readOnly = !contextPromptEditing,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(12.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (contextPromptEditing) {
                        TextButton(onClick = {
                            contextPromptDraft = loadEditablePrompt(selectedContextPromptMode)
                            contextPromptEditing = false
                        }) { Text("Abbrechen") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                saveEditablePrompt(selectedContextPromptMode, contextPromptDraft)
                                contextPromptEditing = false
                                configEditGeneration++   // spaete GET-Antwort darf den gespeicherten Prompt nicht zuruecksetzen
                                Toast.makeText(context, "Prompt gespeichert", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Orange)
                        ) { Text("Speichern", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    } else {
                        Button(
                            onClick = { contextPromptEditing = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Orange)
                        ) { Text("Bearbeiten", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }

        // === GEDÄCHTNIS-EINFLUSS BEI WEBSUCHEN ===
        SectionHeader("GEDÄCHTNIS + WEB")
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, Iris.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Iris.copy(alpha = if (isDark) 0.18f else 0.10f),
                                MaterialTheme.colorScheme.surface,
                                Orange.copy(alpha = if (isDark) 0.08f else 0.05f)
                            )
                        )
                    )
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Icon(Icons.Default.Psychology, null, tint = Iris, modifier = Modifier.size(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gedächtnis-Einfluss bei Websuchen", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Wie stark passende persönliche Notizen mit aktuellen Webfakten verbunden werden. Suchtreffer und Qdrant-Limits bleiben gleich.",
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (memoryWebInfluenceSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Iris)
                    }
                }

                val influenceOptions = listOf(
                    Triple("light", "Leicht", "Webfakten führen; Notizen bleiben sparsam, aber erkennbar."),
                    Triple("normal", "Normal", "Notizen werden sichtbar an mehreren passenden Stellen verbunden."),
                    Triple("strong", "Stark", "Persönliche Einträge bilden die zentrale, ausführlichere Linse.")
                )
                influenceOptions.forEach { (id, label, description) ->
                    val active = memoryWebInfluence == id
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = if (active) Iris.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                        border = BorderStroke(1.dp, if (active) Iris else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !memoryWebInfluenceSaving) {
                                if (!active) {
                                    val previous = memoryWebInfluence
                                    memoryWebInfluence = id
                                    configEditGeneration++   // spaete GET-Antwort darf die Wahl nicht zuruecksetzen
                                    memoryWebInfluenceSaving = true
                                    screenScope.launch {
                                        try {
                                            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                                                val r = ApiClient.agentApi().updateConfig(
                                                    AgentConfigRequest(memory_web_influence = id)
                                                )
                                                // Cache konsistent halten (wie bei Modellen/Limits):
                                                // sonst zeigte das naechste Oeffnen der Einstellungen
                                                // wieder den ALTEN Einfluss-Wert aus dem Zwischenspeicher.
                                                try {
                                                    ApiClient.cacheAgentConfig(ApiClient.agentApi().getConfig())
                                                } catch (inner: Exception) {
                                                    CortexLog.warn("Settings", "saveMemoryWebInfluence", "Cache-Refresh nach Speichern fehlgeschlagen: ${inner.message}")
                                                }
                                                r
                                            }
                                            memoryWebInfluence = response.memory_web_influence.takeIf {
                                                it in setOf("light", "normal", "strong")
                                            } ?: "normal"
                                            memoryWebInfluenceStatus = "Gespeichert und sofort aktiv"
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            memoryWebInfluence = previous
                                            memoryWebInfluenceStatus = "Speichern fehlgeschlagen"
                                            CortexLog.error("Settings", "saveMemoryWebInfluence", "Speichern fehlgeschlagen: ${e.message}")
                                            Toast.makeText(context, "Gedächtnis-Einfluss konnte nicht gespeichert werden", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            memoryWebInfluenceSaving = false
                                        }
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            RadioButton(selected = active, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Iris))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (active) Iris else MaterialTheme.colorScheme.onSurface)
                                Text(description, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Text(memoryWebInfluenceStatus, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // === DARSTELLUNG ===
        SectionHeader("DARSTELLUNG")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.padding(13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(13.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data class ThemeOpt(val id: String, val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
                val opts = listOf(
                    ThemeOpt("dark", "Dunkel", Icons.Default.DarkMode),
                    ThemeOpt("light", "Hell", Icons.Default.LightMode),
                    ThemeOpt("system", "System", Icons.Default.PhoneAndroid)
                )
                opts.forEach { opt ->
                    val active = themeMode == opt.id
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) Iris.copy(alpha = 0.14f)
                        else if (isDark) DarkField else LightField,
                        border = BorderStroke(1.dp,
                            if (active) Iris else if (isDark) DarkFieldBorder else LightFieldBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (!active) onSetThemeMode(opt.id)
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                opt.icon,
                                null, modifier = Modifier.size(22.dp),
                                tint = if (active) Iris else MaterialTheme.colorScheme.onSurface
                            )
                            Text(opt.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = if (active) Iris else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // === ÜBER ===
        SectionHeader("ÜBER")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Iris.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = "Cortex Speicher",
                        tint = Iris,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cortex", fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp)
                    Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_BUMPED_AT})",
                        fontFamily = JetBrainsMono, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private data class LimitItem(
    val key: String,
    val label: String,
    val unit: String,
    val summary: String,
    val explanation: String
)

private val runtimeLimitItems = listOf(
    LimitItem(
        "agent.recall_normal_max",
        "Normale Gedächtnisfrage",
        "Treffer",
        "Wie viele Einträge der Agent bei normalen Fragen höchstens aus dem Gehirn holt.",
        "Bei einer normalen Frage sucht Cortex zuerst in Qdrant nach passenden Einträgen. Dieser Wert ist die Obergrenze für die besten Dokumente, die danach an den Leseagenten gehen. Größer heißt: Cortex schaut breiter und findet eher Randtreffer, wird aber langsamer und gibt dem Leseagenten mehr Rauschen. Kleiner heißt: schneller und fokussierter, aber ein seltener passender Eintrag kann fehlen. Nach der Mehrfachsuche werden die zusammengeführten Treffer wieder auf diesen Wert begrenzt."
    ),
    LimitItem(
        "agent.recall_full_limit",
        "Expliziter Vollabruf",
        "Treffer · 0 = alle",
        "Nur für Fragen wie „Zeig mir alles über ...“ oder „komplette Kategorie“.",
        "Normale Fragen nutzen dieses Limit nicht. Es greift nur, wenn du bewusst alles sehen willst. 0 bedeutet: keine Trefferbegrenzung, also alle passenden Einträge. Größer als 0 begrenzt auch solche Vollfragen. Kleiner macht Vollabrufe schneller, kann aber bei echten Alles-Fragen unvollständig werden."
    ),
    LimitItem(
        "agent.multi_query_variants",
        "Mehrfachsuche-Varianten",
        "Varianten",
        "Zusätzliche Suchformulierungen neben deiner Originalfrage.",
        "Cortex sucht immer mit deiner Originalfrage. Dieser Wert sagt, wie viele alternative Suchformulierungen der Router zusätzlich erzeugen darf. 2 bedeutet: Originalfrage plus 2 Varianten, also maximal 3 Suchläufe. Größer findet mehr Formulierungen und Synonyme, kostet aber mehr Zeit und kann mehr Rauschen erzeugen. 0 heißt: nur deine Originalfrage."
    ),
    LimitItem(
        "agent.entity_docs_limit",
        "Entity-Vollabruf",
        "Dokumente · 0 = alle",
        "Wie viele verknüpfte Dokumente bei „Alles über X“ gelesen werden dürfen.",
        "Das Entity-Register ist keine normale Top-N-Suche: Wenn Cortex eine Entität erkennt, kann es alle damit verknüpften Einträge holen. Dieser Wert begrenzt diese Menge; 0 bedeutet vollständig. Größer als 0 macht Entity-Fragen schneller und kleiner, kann aber bei großen Themen unvollständig werden."
    ),
    LimitItem(
        "agent.entity_extract_chars",
        "Entity-Extraktionsfenster",
        "Zeichen",
        "Wie viel Text der Speicheragent je Eintrag zur Entity-Erkennung sieht.",
        "Beim Speichern kann Cortex Personen, Projekte, Geräte oder Themen als Entitäten erkennen. Dieses Fenster sagt, wie viel vom Eintrag für diese Extraktion betrachtet wird. Der Volltext wird trotzdem 1:1 gespeichert. Größer verknüpft lange Einträge besser, kostet aber mehr Modellkontext beim Speichern."
    ),
    LimitItem(
        "agent.lese_snippet_chars",
        "Leseagent-Schnipsel",
        "Zeichen je Treffer",
        "Wie viel Text der Leseagent pro Treffer zum Auswählen sieht.",
        "Der Leseagent liest nicht sofort jeden Volltext. Er bekommt pro Treffer nur einen relevanten Ausschnitt und entscheidet daraus, welche Einträge wirklich zur Frage passen. Größer gibt ihm mehr Kontext und hilft bei langen Einträgen. Kleiner ist schneller und spart Kontext, kann aber wichtige Stellen außerhalb des Schnipsels übersehen."
    ),
    LimitItem(
        "agent.answer_snippet_limit",
        "Antwort-Schnipsel",
        "Treffer",
        "Wie viele der besten Gedächtnistreffer das Sofort-Profil an den Antwort-Agenten gibt.",
        "Nach der Qdrant-Suche reicht das Profil „Sofort Web + Gedächtnis“ nur diese Anzahl der bestbewerteten Treffer als Text-Schnipsel an den Hauptagenten weiter. Größer gibt der Antwort eine breitere Wissensgrundlage, benötigt aber mehr Kontext und kann zusätzliches Rauschen einbringen. Kleiner antwortet fokussierter und schneller, kann aber einen weiter hinten liegenden relevanten Treffer auslassen. Die Zeichenlimits je Schnipsel und für den gesamten Antwort-Kontext gelten zusätzlich."
    ),
    LimitItem(
        "agent.source_chip_limit",
        "Anklickbare Quellen-Chips",
        "Quellen",
        "Wie viele gelesene Gedächtniseinträge im Web-Dashboard anklickbar angezeigt werden.",
        "Dieses Limit steuert nur die Quellen-Chips unter Antworten im Web-Dashboard; der Android-Chat zeigt diese Chips derzeit nicht. Es verändert weder die Anzahl der Qdrant-Suchtreffer noch die Anzahl der Antwort-Schnipsel. Größer macht mehr gelesene Treffer direkt nachprüfbar, benötigt aber mehr Platz unter der Antwort. Cortex zeigt nie mehr Chips als Antwort-Schnipsel gelesen wurden oder passende Einträge vorhanden sind."
    ),
    LimitItem(
        "agent.answer_hit_chars",
        "Antwort je Eintrag",
        "Zeichen je Eintrag",
        "Wie viel Volltext der Antwort-Agent pro ausgewähltem Eintrag bekommt.",
        "Nachdem der Leseagent passende Einträge gewählt hat, bekommt der Antwort-Agent pro Eintrag höchstens diesen Textumfang. Größer erlaubt genauere Antworten aus langen Dokumenten. Kleiner schützt vor Kontextüberlauf, kann aber Details abschneiden. Dieser Wert kann nie sinnvoll größer sein als das Gesamtbudget."
    ),
    LimitItem(
        "agent.answer_total_chars",
        "Antwort-Kontext gesamt",
        "Zeichen gesamt",
        "Gesamtes Trefferbudget für die finale Antwort.",
        "Das ist der Deckel für alle ausgewählten Einträge zusammen. Beispiel: Wenn 10 Einträge ausgewählt werden, teilt Cortex dieses Budget auf sie auf. Größer gibt dem Antwort-Agenten mehr Material, wird aber langsamer und teurer. Kleiner hält Antworten stabiler und verhindert Hänger, kann aber Zusammenhänge verkürzen."
    ),
    LimitItem(
        "agent.answer_max_tokens",
        "Antwort-Max-Tokens",
        "Tokens",
        "Maximale Länge, die das Modell als Antwort erzeugen darf.",
        "Selbst wenn viel Kontext gelesen wurde, kann die Antwort am Ausgabelimit enden. Dieser Wert steuert die maximale Antwortlänge des Modells. Größer erlaubt ausführlichere Gesamtantworten, kleinere Werte halten Antworten kürzer und günstiger."
    ),
    LimitItem(
        "agent.recall_working_cache_threshold",
        "Arbeitscache ab",
        "Treffer",
        "Ab wie vielen Treffern Cortex blockweise verdichtet statt alles direkt zu lesen.",
        "Der Arbeitscache ist die Sicherheitsbrücke für große Vollabrufe: Treffer werden in Blöcke geteilt, zwischengespeichert und verdichtet, statt alle Volltexte in ein einziges Kontextfenster zu drücken. Niedriger ist sicherer, höher bleibt bei mittleren Mengen direkter."
    ),
    LimitItem(
        "agent.full_category_batch_chars",
        "Kategorie-Batch",
        "Zeichen je Block",
        "Blockgröße für sequenziell gelesene Kategorien.",
        "Kategoriefragen laufen nicht als normale semantische Top-N-Suche, sondern lesen die Kategorie Eintrag für Eintrag. Dieser Wert sagt, wie groß ein Verdichtungsblock sein darf. Größer erhält mehr Zusammenhang, kleiner ist stabiler."
    ),
    LimitItem(
        "agent.dedup_candidates",
        "Duplikat-Prüfung",
        "Kandidaten",
        "Wie viele ähnliche bestehende Einträge vor dem Speichern geprüft werden.",
        "Beim Speichern sucht Cortex nach ähnlichen vorhandenen Einträgen, damit nicht unnötig Dubletten entstehen. Höher erkennt mehr Ähnlichkeiten, niedriger macht Speichern schneller. Dieses Limit wirkt nur im Speicherpfad, nicht auf normale Antworten."
    ),
    LimitItem(
        "agent.history_max",
        "Gesprächsverlauf",
        "Nachrichten",
        "Wie viele alte Chat-Nachrichten der Agent zusätzlich mitnimmt.",
        "Dieser Wert ist getrennt von der Gedächtnissuche. Er sagt nur, wie viel vom laufenden Gespräch als Kontext an das Modell geht. Größer hilft, wenn du dich auf frühere Chatstellen beziehst. Kleiner spart Kontext und reduziert Verwirrung durch alte Nebenthemen. 0 bedeutet: kein alter Verlauf."
    ),
    LimitItem(
        "agent.history_compress_at",
        "Verlauf komprimieren ab",
        "Nachrichten · 0 = aus",
        "Ab wie vielen Chat-Nachrichten Cortex den Verlauf zusammenfasst.",
        "Zählt deine Fragen und Cortex-Antworten zusammen. Ist die Schwelle erreicht, fasst der Hauptagent den bisherigen Verlauf kumulativ zusammen, statt alte Nachrichten nur hart abzuschneiden. 0 schaltet diese Komprimierung aus; dann gilt allein das Gesprächsverlauf-Limit."
    ),
    LimitItem(
        "agent.context_prompt_max_chars",
        "Zusatzprompt",
        "Zeichen",
        "Maximale Länge des Zusatzauftrags aus der App.",
        "Der Zusatzprompt enthält zum Beispiel Modus- und Antwortlängen-Anweisungen. Größer erlaubt ausführlichere Spezialanweisungen. Kleiner schützt davor, dass sehr lange Zusatztexte den eigentlichen Auftrag verdrängen. Wenn der App-Text länger ist, lehnt der Server ihn laut ab, statt ihn still zu kürzen."
    ),
    LimitItem(
        "agent.chat_text_max_chars",
        "Chat-Nachricht",
        "Zeichen",
        "Sicherheitsdeckel für eine einzelne normale Chat-Nachricht.",
        "Sehr lange Texte sind erlaubt, aber nicht unbegrenzt. Dieser Wert schützt Server und Modell vor Speicherproblemen. Größer erlaubt riesige Pastes. Kleiner schützt stärker vor Hängern. Wenn der Text zu lang ist, kommt ein klarer Fehler; Cortex schneidet den Text nicht heimlich ab."
    ),
    LimitItem(
        "brain.search_overfetch_factor",
        "Vektor-Overfetch",
        "Faktor",
        "Wie viele rohe Qdrant-Punkte intern pro gewünschtem Treffer geholt werden.",
        "Qdrant sucht auf Chunk-Ebene. Ein Dokument kann mehrere Chunks haben, deshalb holt die Brain-API intern mehr Rohpunkte und dedupliziert danach auf Dokumente. Faktor 4 bei 50 Treffern bedeutet: bis zu 200 rohe Vektor-Punkte. Größer verbessert die Chance, nach dem Deduplizieren genug gute Dokumente zu behalten. Kleiner ist schneller, kann aber passende Dokumente verlieren."
    ),
    LimitItem(
        "brain.search_date_overfetch_factor",
        "Datums-Overfetch",
        "Faktor",
        "Reserve für Zeitfragen, falls nach Datum nachgefiltert werden muss.",
        "Wenn Qdrant oder der Client einen Datumsfilter nicht nativ anwenden kann, holt Cortex mehr Kandidaten und filtert danach in Python. Dieser Faktor verhindert, dass passende Datumstreffer vorher verdrängt werden. Er wirkt nur auf Suchfragen mit Datum oder Zeitraum."
    ),
    LimitItem(
        "brain.bm25_candidate_factor",
        "BM25-Kandidaten",
        "Faktor",
        "Zusätzliche Stichworttreffer für exakte Namen, Zahlen und Begriffe.",
        "Neben der semantischen Vektorsuche läuft BM25 als Stichwortsuche. Das hilft bei exakten Wörtern wie Namen, Orten, Medikamenten, Versionsnummern oder Fehlercodes. Faktor 4 bei 50 Treffern bedeutet: bis zu 200 BM25-Kandidaten. Größer findet mehr exakte Worttreffer. Kleiner ist schneller, kann aber seltene Begriffe übersehen."
    ),
    LimitItem(
        "brain.bm25_min_candidates",
        "BM25-Mindestkandidaten",
        "Kandidaten",
        "Mindestzahl für BM25, auch wenn das Trefferlimit klein ist.",
        "Wenn du nur wenige Treffer anforderst, sorgt dieser Wert dafür, dass die Stichwortsuche trotzdem nicht zu schmal sucht. Größer ist sicherer für exakte Begriffe, kostet aber etwas mehr Rechenzeit. Kleiner ist schneller, kann aber bei kleinen Limits exakte Worttreffer verlieren."
    ),
    LimitItem(
        "brain.chunk_chars",
        "Suchstück-Größe",
        "Zeichen",
        "Wie lang neue Suchstücke beim Speichern werden.",
        "Das Gehirn speichert den Volltext 1:1. Für die Suche wird der Text zusätzlich in überlappende Suchstücke zerlegt. Größer heißt: jedes Suchstück enthält mehr Zusammenhang. Kleiner heißt: genauere Trefferstellen und mehr Chunks. Wichtig: Diese Einstellung gilt für neue oder neu gespeicherte Einträge; bestehende Einträge behalten ihre alten Suchstücke, bis sie neu gespeichert oder neu eingebettet werden."
    ),
    LimitItem(
        "brain.chunk_overlap",
        "Suchstück-Überlappung",
        "Zeichen",
        "Wie stark sich benachbarte Suchstücke überschneiden.",
        "Überlappung verhindert, dass ein wichtiger Satz genau an einer Chunk-Grenze zerschnitten wird. Größer ist sicherer für zusammenhängende Sätze, erzeugt aber mehr doppelte Suchfläche. Kleiner spart Speicher und Embedding-Aufwand, kann aber Grenzfälle verschlechtern. Der Wert muss kleiner als die Suchstück-Größe sein; der Server sichert das ab."
    ),
    LimitItem(
        "brain.recall_boost_promille",
        "Recall-Boost (Promille)",
        "Promille",
        "Zusätzlicher Recall-Spielraum für die Vektorsuche in Promille.",
        "Ein kleiner Aufschlag in Promille, der die Trefferabdeckung (Recall) der Vektorsuche leicht erhöht, indem etwas breiter gesucht wird. Größer findet mehr Grenzfälle, kann aber unschärfere Treffer zulassen; kleiner bleibt präziser. 0 lässt das Verhalten unverändert."
    )
)

private fun limitsToDrafts(limits: RuntimeLimits): Map<String, String> = buildMap {
    put("agent.dedup_candidates", limits.agent.dedup_candidates.toString())
    put("agent.history_max", limits.agent.history_max.toString())
    put("agent.history_compress_at", limits.agent.history_compress_at.toString())
    put("agent.recall_full_limit", limits.agent.recall_full_limit.toString())
    put("agent.multi_query_variants", limits.agent.multi_query_variants.toString())
    put("agent.entity_extract_chars", limits.agent.entity_extract_chars.toString())
    put("agent.entity_docs_limit", limits.agent.entity_docs_limit.toString())
    put("agent.lese_snippet_chars", limits.agent.lese_snippet_chars.toString())
    put("agent.answer_snippet_limit", limits.agent.answer_snippet_limit.toString())
    put("agent.source_chip_limit", limits.agent.source_chip_limit.toString())
    put("agent.answer_hit_chars", limits.agent.answer_hit_chars.toString())
    put("agent.answer_total_chars", limits.agent.answer_total_chars.toString())
    put("agent.answer_max_tokens", limits.agent.answer_max_tokens.toString())
    put("agent.recall_working_cache_threshold", limits.agent.recall_working_cache_threshold.toString())
    put("agent.recall_normal_max", limits.agent.recall_normal_max.toString())
    put("agent.full_category_batch_chars", limits.agent.full_category_batch_chars.toString())
    put("agent.context_prompt_max_chars", limits.agent.context_prompt_max_chars.toString())
    put("agent.chat_text_max_chars", limits.agent.chat_text_max_chars.toString())
    // brain == null: brain-api war beim /config-Abruf nicht erreichbar — dann KEINE Drafts
    // aus App-Defaults erfinden (sie wuerden beim Speichern Franks echte Server-Limits ersetzen).
    limits.brain?.let { brain ->
        put("brain.chunk_chars", brain.chunk_chars.toString())
        put("brain.chunk_overlap", brain.chunk_overlap.toString())
        put("brain.search_overfetch_factor", brain.search_overfetch_factor.toString())
        put("brain.search_date_overfetch_factor", brain.search_date_overfetch_factor.toString())
        put("brain.bm25_candidate_factor", brain.bm25_candidate_factor.toString())
        put("brain.bm25_min_candidates", brain.bm25_min_candidates.toString())
        put("brain.recall_boost_promille", (brain.recall_boost_promille ?: 0).toString())
    }
}

private fun limitInt(drafts: Map<String, String>, key: String, fallback: Int): Int =
    drafts[key]?.toIntOrNull() ?: fallback

private fun draftsToLimits(current: RuntimeLimits, drafts: Map<String, String>): RuntimeLimits = RuntimeLimits(
    agent = AgentRuntimeLimits(
        dedup_candidates = limitInt(drafts, "agent.dedup_candidates", current.agent.dedup_candidates),
        history_max = limitInt(drafts, "agent.history_max", current.agent.history_max),
        history_compress_at = limitInt(drafts, "agent.history_compress_at", current.agent.history_compress_at),
        recall_full_limit = limitInt(drafts, "agent.recall_full_limit", current.agent.recall_full_limit),
        multi_query_variants = limitInt(drafts, "agent.multi_query_variants", current.agent.multi_query_variants),
        entity_extract_chars = limitInt(drafts, "agent.entity_extract_chars", current.agent.entity_extract_chars),
        entity_docs_limit = limitInt(drafts, "agent.entity_docs_limit", current.agent.entity_docs_limit),
        lese_snippet_chars = limitInt(drafts, "agent.lese_snippet_chars", current.agent.lese_snippet_chars),
        answer_snippet_limit = limitInt(drafts, "agent.answer_snippet_limit", current.agent.answer_snippet_limit),
        source_chip_limit = limitInt(drafts, "agent.source_chip_limit", current.agent.source_chip_limit),
        answer_hit_chars = limitInt(drafts, "agent.answer_hit_chars", current.agent.answer_hit_chars),
        answer_total_chars = limitInt(drafts, "agent.answer_total_chars", current.agent.answer_total_chars),
        answer_max_tokens = limitInt(drafts, "agent.answer_max_tokens", current.agent.answer_max_tokens),
        recall_working_cache_threshold = limitInt(drafts, "agent.recall_working_cache_threshold", current.agent.recall_working_cache_threshold),
        recall_normal_max = limitInt(drafts, "agent.recall_normal_max", current.agent.recall_normal_max),
        full_category_batch_chars = limitInt(drafts, "agent.full_category_batch_chars", current.agent.full_category_batch_chars),
        context_prompt_max_chars = limitInt(drafts, "agent.context_prompt_max_chars", current.agent.context_prompt_max_chars),
        chat_text_max_chars = limitInt(drafts, "agent.chat_text_max_chars", current.agent.chat_text_max_chars)
    ),
    // Kein bekannter Brain-Stand -> auch KEINE Brain-Limits mitsenden (Moshi laesst null-Felder
    // weg, der Server aendert dann nichts an den Brain-Limits).
    brain = current.brain?.let { brain ->
        BrainRuntimeLimits(
            chunk_chars = limitInt(drafts, "brain.chunk_chars", brain.chunk_chars),
            chunk_overlap = limitInt(drafts, "brain.chunk_overlap", brain.chunk_overlap),
            search_overfetch_factor = limitInt(drafts, "brain.search_overfetch_factor", brain.search_overfetch_factor),
            search_date_overfetch_factor = limitInt(drafts, "brain.search_date_overfetch_factor", brain.search_date_overfetch_factor),
            bm25_candidate_factor = limitInt(drafts, "brain.bm25_candidate_factor", brain.bm25_candidate_factor),
            bm25_min_candidates = limitInt(drafts, "brain.bm25_min_candidates", brain.bm25_min_candidates),
            recall_boost_promille = limitInt(drafts, "brain.recall_boost_promille", brain.recall_boost_promille ?: 0)
        )
    }
)

@Composable
private fun LimitSettingsCard(
    isDark: Boolean,
    drafts: Map<String, String>,
    status: String,
    saving: Boolean,
    saveEnabled: Boolean = true,
    brainAvailable: Boolean = true,
    onDraftChange: (String, String) -> Unit,
    onSave: () -> Unit
) {
    var expandedKey by remember { mutableStateOf<String?>(null) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Memory, null, tint = Orange, modifier = Modifier.size(22.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Qdrant Limits", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Steuert, wie breit Cortex im Gehirn sucht und wie viel Kontext Agenten bekommen.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                "Speichern wirkt serverseitig: normale Fragen nutzen diese Limits sofort. Limit 0 bedeutet nur beim expliziten Vollabruf „alle Treffer“; normale Fragen bleiben geschützt.",
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            runtimeLimitItems.forEachIndexed { index, item ->
                LimitRow(
                    item = item,
                    value = drafts[item.key].orEmpty(),
                    isDark = isDark,
                    expanded = expandedKey == item.key,
                    // Brain-Felder sperren, solange kein echter Brain-Server-Stand vorliegt:
                    // Eingaben wuerden beim Speichern verworfen (brain=null wird nicht gesendet),
                    // die App meldete aber trotzdem Erfolg — stiller Eingabeverlust.
                    enabled = brainAvailable || !item.key.startsWith("brain."),
                    onValueChange = { onDraftChange(item.key, it) },
                    onToggle = { expandedKey = if (expandedKey == item.key) null else item.key }
                )
                if (index < runtimeLimitItems.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(status, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Button(
                    onClick = onSave,
                    enabled = !saving && saveEnabled,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Speichern", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LimitRow(
    item: LimitItem,
    value: String,
    isDark: Boolean,
    expanded: Boolean,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
    onToggle: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (enabled) item.summary else "Brain-API nicht erreichbar — Wert gesperrt, bis der Server-Stand geladen ist.",
                    fontSize = 11.sp, lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) DarkField else LightField,
                border = BorderStroke(1.dp, if (isDark) DarkFieldBorder else LightFieldBorder),
                modifier = Modifier.width(98.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    readOnly = !enabled,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = JetBrainsMono,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(item.unit, fontFamily = JetBrainsMono, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            TextButton(onClick = onToggle) {
                Text(if (expanded) "Erklärung ausblenden" else "Erklärung", fontSize = 12.sp)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, modifier = Modifier.size(17.dp))
            }
        }
        if (expanded) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) DarkField else LightField,
                border = BorderStroke(1.dp, if (isDark) DarkFieldBorder else LightFieldBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    item.explanation,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ContextPromptModeButtons(
    selectedMode: String,
    isDark: Boolean,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val modeItems = listOf(
            PromptButtonItem(SettingsStore.CONTEXT_MODE_SAVE, "Speichern", { Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp)) }),
            PromptButtonItem(SettingsStore.CONTEXT_MODE_SEARCH, "Suchen", { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) })
        )
        val sizeItems = listOf(
            PromptButtonItem(SettingsStore.RESPONSE_SIZE_AUTO, "A", { Text("A", fontSize = 14.sp, fontWeight = FontWeight.Bold) }),
            PromptButtonItem(SettingsStore.RESPONSE_SIZE_SHORT, "S", { Text("S", fontSize = 14.sp, fontWeight = FontWeight.Bold) }),
            PromptButtonItem(SettingsStore.RESPONSE_SIZE_MEDIUM, "M", { Text("M", fontSize = 14.sp, fontWeight = FontWeight.Bold) }),
            PromptButtonItem(SettingsStore.RESPONSE_SIZE_XL, "XL", { Text("XL", fontSize = 14.sp, fontWeight = FontWeight.Bold) })
        )
        PromptButtonRow(modeItems, selectedMode, isDark, onSelect)
        PromptButtonRow(sizeItems, selectedMode, isDark, onSelect)
    }
}

private data class PromptButtonItem(
    val key: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
private fun PromptButtonRow(
    items: List<PromptButtonItem>,
    selectedMode: String,
    isDark: Boolean,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val active = selectedMode == item.key
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (active) Orange.copy(alpha = 0.18f) else if (isDark) DarkField else LightField,
                border = BorderStroke(1.dp, if (active) Orange.copy(alpha = 0.55f) else if (isDark) DarkFieldBorder else LightFieldBorder),
                modifier = Modifier.weight(1f).clickable { onSelect(item.key) }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    CompositionLocalProvider(LocalContentColor provides if (active) Orange else MaterialTheme.colorScheme.onSurfaceVariant) {
                        item.icon()
                    }
                    Text(item.label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// Router-"auto": Schritt 1 (Einordnung) folgt Modell + Thinking des Hauptagenten.
private const val ROUTER_AUTO = "auto (wie Hauptagent)"

private fun isCodexModel(model: String): Boolean {
    val m = model.lowercase()
    return m.startsWith("gpt-") || m.startsWith("codex/") || m.startsWith("openai-codex/")
}

private fun isGeminiModel(model: String): Boolean = model.lowercase().startsWith("gemini")

// Thinking/Reasoning-Stufen einstellbar: Codex/GPT UND Gemini (thinking_budget aus derselben Stufe).
// minimax/OpenCode denkt nativ -> keine Auswahl.
private fun modelSupportsReasoning(model: String): Boolean = isCodexModel(model) || isGeminiModel(model)

private fun codexAuthErrorMessage(e: Exception): String {
    val http = e as? HttpException
    if (http != null) {
        val raw = http.response()?.errorBody()?.string().orEmpty()
        val detail = Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)
        val suffix = detail?.replace("\\n", " ")?.takeIf { it.isNotBlank() } ?: "Serverfehler"
        return "Verbindung fehlgeschlagen (HTTP ${http.code()}): $suffix"
    }
    return "Verbindung fehlgeschlagen: ${e.message ?: e::class.java.simpleName}"
}

@Composable
private fun AgentModelDropdown(
    label: String,
    value: String,
    options: List<String>,
    isDark: Boolean,
    priceLabels: Map<String, String>? = null,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(label, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) DarkField else LightField,
                border = BorderStroke(1.dp, if (isDark) DarkFieldBorder else LightFieldBorder),
                modifier = Modifier.fillMaxWidth().clickable { open = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        value,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                shape = RoundedCornerShape(14.dp),
                containerColor = if (isDark) DarkSurface else Color.White,
                border = BorderStroke(1.dp, if (isDark) DarkBorder else LightBorder)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(option, fontFamily = JetBrainsMono, fontSize = 12.5.sp)
                                if (priceLabels != null) {
                                    // Preis direkt im Dropdown unter dem Modellnamen (Frank-Wunsch):
                                    // schon BEIM Auswaehlen sehen, wie teuer das Modell ist.
                                    Text(
                                        priceLabels[option] ?: "über Abo (nicht pro Token)",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelect(option)
                            open = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = JetBrainsMono,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    center: Boolean = false,
    hint: String = ""
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (isDark) DarkField else LightField,
        border = BorderStroke(1.dp, if (isDark) DarkFieldBorder else LightFieldBorder),
        modifier = modifier
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.padding(8.dp, 10.dp),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = JetBrainsMono,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            decorationBox = { inner ->
                if (value.isEmpty() && hint.isNotEmpty()) {
                    Text(hint, fontFamily = JetBrainsMono, fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                inner()
            }
        )
    }
}

@Composable
private fun SecretRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isDark: Boolean,
    divider: Boolean,
    onRevealRequest: ((() -> Unit) -> Unit)
) {
    var visible by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.padding(13.dp, 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                BasicTextField(
                    value = if (visible) value else "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022",
                    onValueChange = onValueChange,
                    readOnly = !visible,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    singleLine = true
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isDark) DarkField else LightField)
                    .border(1.dp, if (isDark) DarkFieldBorder else LightFieldBorder, RoundedCornerShape(9.dp))
                    .clickable {
                        if (visible) {
                            visible = false
                        } else {
                            onRevealRequest { visible = true }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

private fun voiceLabelFor(apiName: String): String {
    // Alte Values mit Prefix (z.B. "de-DE-Chirp3-HD-Kore") → stripped
    val stripped = apiName.removePrefix("de-DE-Chirp3-HD-")
    return chirpVoices.firstOrNull { it.name == stripped }?.label ?: stripped
}

// --- Edge TTS (Microsoft) — dieselben 6 Stimmen wie in BestJournalFrank ---
private data class EdgeVoiceEntry(
    val id: String,
    val short: String,
    val gender: Gender,
    val description: String
)

private val edgeVoices = listOf(
    EdgeVoiceEntry("de-DE-SeraphinaMultilingualNeural", "Seraphina", Gender.FEMALE, "weiblich · Standard"),
    EdgeVoiceEntry("de-DE-FlorianMultilingualNeural", "Florian", Gender.MALE, "männlich"),
    EdgeVoiceEntry("de-DE-KatjaNeural", "Katja", Gender.FEMALE, "weiblich, warm"),
    EdgeVoiceEntry("de-DE-KillianNeural", "Killian", Gender.MALE, "männlich, warm"),
    EdgeVoiceEntry("de-DE-ConradNeural", "Conrad", Gender.MALE, "männlich, klar"),
    EdgeVoiceEntry("de-DE-AmalaNeural", "Amala", Gender.FEMALE, "weiblich, jung"),
)

private fun edgeVoiceLabelFor(id: String): String = edgeVoices.firstOrNull { it.id == id }?.short ?: id
private fun edgeVoiceShortFor(id: String): String = edgeVoices.firstOrNull { it.id == id }?.short ?: "die Stimme"

@Composable
private fun TtsUsageIndicator(usage: TtsUsage) {
    val used = usage.charsThisMonth.coerceAtMost(usage.limit)
    val fraction = if (usage.limit > 0L) used.toFloat() / usage.limit.toFloat() else 0f
    val usedText = String.format(java.util.Locale.GERMANY, "%,d", usage.charsThisMonth)
    val limitText = String.format(java.util.Locale.GERMANY, "%,d", usage.limit)

    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            "Chirp 3 HD: $usedText von $limitText Zeichen · ${usage.percentFree} % frei",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp)
                    .clip(RoundedCornerShape(3.dp)).background(Orange)
            )
        }
    }
}

@Composable
private fun TtsEngineChip(
    title: String,
    subtitle: String,
    selected: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) Orange.copy(alpha = 0.16f) else if (isDark) DarkField else LightField
    val border = if (selected) Orange.copy(alpha = 0.55f) else if (isDark) DarkFieldBorder else LightFieldBorder
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = if (selected) Orange else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EdgeVoicePickerDialog(
    currentVoice: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onTestVoice: (String) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edge-Stimme wählen", fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        },
        text = {
            Column {
                Text("Edge TTS · Microsoft (kostenlos)",
                    fontFamily = JetBrainsMono, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(min = 150.dp, max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(edgeVoices) { voice ->
                        val accent = if (voice.gender == Gender.FEMALE) Color(0xFFF2698E) else Color(0xFF5AB0F2)
                        val isSelected = currentVoice == voice.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) accent.copy(alpha = 0.14f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) accent.copy(alpha = 0.40f) else Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(shape = RoundedCornerShape(8.dp), color = accent.copy(alpha = 0.15f)) {
                                    Icon(Icons.Default.RecordVoiceOver, null, tint = accent,
                                        modifier = Modifier.size(22.dp).padding(4.dp))
                                }
                                Column(modifier = Modifier.weight(1f).clickable { onSelect(voice.id) }) {
                                    Text(voice.short, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface)
                                    Text(voice.description, fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onTestVoice(voice.id) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, "Testen",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null, tint = accent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fertig") }
        },
        containerColor = if (isDark) DarkRaised else LightSurface,
        shape = RoundedCornerShape(22.dp)
    )
}

private data class VoiceEntry(
    val name: String,
    val label: String,
    val gender: Gender,
    val description: String
)

private enum class Gender { FEMALE, MALE }

private val chirpVoices = listOf(
    // Weiblich (14)
    VoiceEntry("Achernar", "Achernar", Gender.FEMALE, "weiblich"),
    VoiceEntry("Aoede", "Aoede", Gender.FEMALE, "weiblich"),
    VoiceEntry("Autonoe", "Autonoe", Gender.FEMALE, "weiblich"),
    VoiceEntry("Callirrhoe", "Callirrhoe", Gender.FEMALE, "weiblich"),
    VoiceEntry("Despina", "Despina", Gender.FEMALE, "weiblich"),
    VoiceEntry("Erinome", "Erinome", Gender.FEMALE, "weiblich"),
    VoiceEntry("Gacrux", "Gacrux", Gender.FEMALE, "weiblich"),
    VoiceEntry("Kore", "Kore", Gender.FEMALE, "weiblich · Standard"),
    VoiceEntry("Laomedeia", "Laomedeia", Gender.FEMALE, "weiblich"),
    VoiceEntry("Leda", "Leda", Gender.FEMALE, "weiblich"),
    VoiceEntry("Pulcherrima", "Pulcherrima", Gender.FEMALE, "weiblich"),
    VoiceEntry("Sulafat", "Sulafat", Gender.FEMALE, "weiblich"),
    VoiceEntry("Vindemiatrix", "Vindemiatrix", Gender.FEMALE, "weiblich"),
    VoiceEntry("Zephyr", "Zephyr", Gender.FEMALE, "weiblich"),
    // Männlich (16)
    VoiceEntry("Achird", "Achird", Gender.MALE, "männlich"),
    VoiceEntry("Algenib", "Algenib", Gender.MALE, "männlich"),
    VoiceEntry("Algieba", "Algieba", Gender.MALE, "männlich"),
    VoiceEntry("Alnilam", "Alnilam", Gender.MALE, "männlich"),
    VoiceEntry("Charon", "Charon", Gender.MALE, "männlich"),
    VoiceEntry("Enceladus", "Enceladus", Gender.MALE, "männlich"),
    VoiceEntry("Fenrir", "Fenrir", Gender.MALE, "männlich"),
    VoiceEntry("Iapetus", "Iapetus", Gender.MALE, "männlich"),
    VoiceEntry("Orus", "Orus", Gender.MALE, "männlich"),
    VoiceEntry("Puck", "Puck", Gender.MALE, "männlich"),
    VoiceEntry("Rasalgethi", "Rasalgethi", Gender.MALE, "männlich"),
    VoiceEntry("Sadachbia", "Sadachbia", Gender.MALE, "männlich"),
    VoiceEntry("Sadaltager", "Sadaltager", Gender.MALE, "männlich"),
    VoiceEntry("Schedar", "Schedar", Gender.MALE, "männlich"),
    VoiceEntry("Umbriel", "Umbriel", Gender.MALE, "männlich"),
    VoiceEntry("Zubenelgenubi", "Zubenelgenubi", Gender.MALE, "männlich"),
)

@Composable
private fun VoicePickerDialog(
    currentVoice: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onTestVoice: (String) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Stimme wählen", fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        },
        text = {
            Column {
                Text("Chirp 3: HD · Google Cloud TTS",
                    fontFamily = JetBrainsMono, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(min = 200.dp, max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val femaleVoices = chirpVoices.filter { it.gender == Gender.FEMALE }
                    val maleVoices = chirpVoices.filter { it.gender == Gender.MALE }

                    item {
                        Text("WEIBLICH", fontFamily = JetBrainsMono, fontSize = 9.5.sp,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp))
                    }

                    items(femaleVoices) { voice ->
                        VoiceRow(
                            voice = voice,
                            isSelected = currentVoice == voice.name,
                            accentColor = Color(0xFFF2698E),
                            onSelect = { onSelect(voice.name) },
                            onTest = { onTestVoice(voice.name) }
                        )
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("MÄNNLICH", fontFamily = JetBrainsMono, fontSize = 9.5.sp,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp))
                    }

                    items(maleVoices) { voice ->
                        VoiceRow(
                            voice = voice,
                            isSelected = currentVoice == voice.name,
                            accentColor = Color(0xFF5AB0F2),
                            onSelect = { onSelect(voice.name) },
                            onTest = { onTestVoice(voice.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fertig") }
        },
        containerColor = if (isDark) DarkRaised else LightSurface,
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun VoiceRow(
    voice: VoiceEntry,
    isSelected: Boolean,
    accentColor: Color,
    onSelect: () -> Unit,
    onTest: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    val bg = if (isSelected) accentColor.copy(alpha = 0.14f) else Color.Transparent
    val border = if (isSelected) accentColor.copy(alpha = 0.40f) else Color.Transparent

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Gender icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Icon(
                    if (voice.gender == Gender.FEMALE) Icons.Default.Face else Icons.Default.Face,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp).padding(4.dp)
                )
            }

            // Name — klickbar für Auswahl
            Column(
                modifier = Modifier.weight(1f).clickable { onSelect() }
            ) {
                Text(
                    voice.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    voice.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Selection indicator
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null,
                    tint = accentColor, modifier = Modifier.size(18.dp))
            }

            // Test button
            IconButton(
                onClick = { onTest() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.PlayArrow, "Vorschau",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

private class LazyColumnScope {
    // Stub — importiert über existierende Datei
}

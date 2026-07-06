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
import de.frank.cortex.data.model.*
import de.frank.cortex.network.ApiClient
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
    var recordingToneEnabled by remember { mutableStateOf(SettingsStore.recordingToneEnabled) }
    var recordingToneVolume by remember { mutableStateOf(SettingsStore.recordingToneVolume) }
    var biometricLockEnabled by remember { mutableStateOf(SettingsStore.biometricLockEnabled) }
    var wgConfig by remember { mutableStateOf(SettingsStore.wgConfig) }
    val screenScope = rememberCoroutineScope()
    var agentModelOptions by remember {
        mutableStateOf(listOf("gemini-3.1-flash-lite", "gemini-3.5-flash", "gemini-3-flash-preview", "gemini-2.5-flash", "minimax/minimax-m3"))
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
    var modelPriceLabels by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var agentModelsLoading by remember { mutableStateOf(false) }
    var agentModelsSaving by remember { mutableStateOf(false) }
    var agentModelStatus by remember { mutableStateOf("") }
    var runtimeLimits by remember { mutableStateOf(RuntimeLimits()) }
    var limitDrafts by remember { mutableStateOf(limitsToDrafts(RuntimeLimits())) }
    var limitsSaving by remember { mutableStateOf(false) }
    var limitsStatus by remember { mutableStateOf("Server-Stand wird geladen") }
    var codexConnected by remember { mutableStateOf(false) }
    var codexStatus by remember { mutableStateOf("Server-Codex-Status wird geladen") }
    var codexUserCode by remember { mutableStateOf("") }
    var codexAuthId by remember { mutableStateOf("") }
    var codexConnecting by remember { mutableStateOf(false) }
    var selectedContextPromptMode by remember { mutableStateOf(SettingsStore.RESPONSE_SIZE_AUTO) }
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
                // Best effort — offline bleibt der lokale Stand, Sync holt es beim
                // naechsten App-Start nach (ChatViewModel.syncSizePromptsWithServer).
                screenScope.launch {
                    try {
                        ApiClient.agentApi().updateConfig(
                            de.frank.cortex.data.model.AgentConfigRequest(
                                size_prompt_auto = if (key == SettingsStore.RESPONSE_SIZE_AUTO) prompt else null,
                                size_prompt_s = if (key == SettingsStore.RESPONSE_SIZE_SHORT) prompt else null,
                                size_prompt_m = if (key == SettingsStore.RESPONSE_SIZE_MEDIUM) prompt else null,
                                size_prompt_xl = if (key == SettingsStore.RESPONSE_SIZE_XL) prompt else null
                            )
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        CortexLog.warn("Settings", "syncSizePrompt", "Server-Sync fehlgeschlagen (lokal gespeichert): ${e.message}")
                    }
                }
            }
            else -> {
                SettingsStore.setContextPrompt(key, prompt)
                screenScope.launch {
                    try {
                        ApiClient.agentApi().updateConfig(
                            de.frank.cortex.data.model.AgentConfigRequest(
                                context_prompt_save = if (key == SettingsStore.CONTEXT_MODE_SAVE) prompt else null,
                                context_prompt_search = if (key == SettingsStore.CONTEXT_MODE_SEARCH) prompt else null
                            )
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        CortexLog.warn("Settings", "syncContextPrompt", "Server-Sync fehlgeschlagen (lokal gespeichert): ${e.message}")
                    }
                }
            }
        }
    }
    var contextPromptDraft by remember { mutableStateOf(loadEditablePrompt(selectedContextPromptMode)) }
    var contextPromptEditing by remember { mutableStateOf(false) }

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
            }
        }
    }

    LaunchedEffect(Unit) {
        agentModelsLoading = true
        try {
            val config = ApiClient.agentApi().getConfig()
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
            if (config.size_prompts_custom) {
                config.size_prompts.forEach { (k, v) ->
                    if (k in setOf("auto", "s", "m", "xl") && v.isNotBlank()) SettingsStore.setResponseSizePrompt(k, v)
                }
            }
            if (config.context_prompts_custom) {
                config.context_prompts.forEach { (k, v) ->
                    if (k in setOf(SettingsStore.CONTEXT_MODE_SAVE, SettingsStore.CONTEXT_MODE_SEARCH) && v.isNotBlank()) {
                        SettingsStore.setContextPrompt(k, v)
                    }
                }
            }
            contextPromptDraft = loadEditablePrompt(selectedContextPromptMode)
            runtimeLimits = config.limits
            limitDrafts = limitsToDrafts(config.limits)
            limitsStatus = "Aktuelle Limits geladen"
            codexConnected = config.codex?.connected == true
            codexStatus = if (codexConnected) "Server verbunden — GPT/Codex-Modelle sind auswählbar" else "Server nicht verbunden"
            agentModelStatus = "Aktueller Server-Stand geladen"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CortexLog.warn("Settings", "loadAgentModels", "Agent-Modelle nicht geladen: ${e.message}")
            agentModelStatus = "Server-Stand nicht erreichbar"
            limitsStatus = "Limits nicht erreichbar"
        } finally {
            agentModelsLoading = false
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
                            if (WireGuardManager.parseConfig(it)) SettingsStore.wgConfig = it
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
                    SettingsTextField(serverHost, { serverHost = it; SettingsStore.serverHost = it },
                        Modifier.weight(1f))
                }

                // Ports row
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsTextField(agentPort, { agentPort = it; SettingsStore.agentPort = it },
                        Modifier.weight(1f), center = true, hint = "Agent")
                    SettingsTextField(brainPort, { brainPort = it; SettingsStore.brainPort = it },
                        Modifier.weight(1f), center = true, hint = "Brain")
                    SettingsTextField(dashboardPort, { dashboardPort = it; SettingsStore.dashboardPort = it },
                        Modifier.weight(1f), center = true, hint = "Dashboard")
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
                                        repeat((start.expires_in / start.interval).coerceAtMost(180)) {
                                            delay((start.interval.coerceAtLeast(3) * 1000).toLong())
                                            val poll = ApiClient.agentApi().pollCodexAuth(de.frank.cortex.data.model.CodexAuthPollRequest(codexAuthId))
                                            if (poll.status == "connected") {
                                                codexConnected = true
                                                codexUserCode = ""
                                                codexStatus = "Server verbunden — GPT/Codex-Modelle sind auswählbar"
                                                val config = ApiClient.agentApi().getConfig()
                                                if (config.available.isNotEmpty()) agentModelOptions = config.available
                                                codexConnecting = false
                                                return@launch
                                            }
                                            if (poll.status == "expired") {
                                                codexUserCode = ""
                                                codexStatus = "Code abgelaufen"
                                                codexConnecting = false
                                                return@launch
                                            }
                                        }
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
                                        val config = ApiClient.agentApi().getConfig()
                                        if (config.available.isNotEmpty()) agentModelOptions = config.available
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
                                scope.launch {
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
                                scope.launch {
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
                            "${(recordingToneVolume * 100).toInt()}%",
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

                AgentModelDropdown("Hauptagent", hauptModel, agentModelOptions, isDark, priceLabels = modelPriceLabels) { hauptModel = it }
                if (modelSupportsReasoning(hauptModel)) {
                    AgentModelDropdown("Thinking", hauptReasoning, reasoningOptions, isDark) { hauptReasoning = it }
                }
                // Router (Schritt 1): darf ein eigenes (schnelles) Modell + eigenes Thinking nutzen;
                // "auto (wie Hauptagent)" = exakt bisheriges Verhalten. Die Antwort (Schritt 2)
                // formuliert immer der Hauptagent.
                AgentModelDropdown(
                    "Router", routerModel, listOf(ROUTER_AUTO) + agentModelOptions, isDark,
                    priceLabels = modelPriceLabels + (ROUTER_AUTO to "übernimmt Modell + Thinking vom Hauptagenten")
                ) { routerModel = it }
                val effectiveRouterModel = if (routerModel == ROUTER_AUTO) hauptModel else routerModel
                if (routerModel != ROUTER_AUTO && modelSupportsReasoning(effectiveRouterModel)) {
                    AgentModelDropdown("Thinking", routerReasoning, listOf(ROUTER_AUTO) + reasoningOptions, isDark) { routerReasoning = it }
                }
                AgentModelDropdown("Speicheragent", speicherModel, agentModelOptions, isDark, priceLabels = modelPriceLabels) { speicherModel = it }
                if (modelSupportsReasoning(speicherModel)) {
                    AgentModelDropdown("Thinking", speicherReasoning, reasoningOptions, isDark) { speicherReasoning = it }
                }
                AgentModelDropdown("Abfrageagent", abfrageModel, agentModelOptions, isDark, priceLabels = modelPriceLabels) { abfrageModel = it }
                if (modelSupportsReasoning(abfrageModel)) {
                    AgentModelDropdown("Thinking", abfrageReasoning, reasoningOptions, isDark) { abfrageReasoning = it }
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
                        onCheckedChange = { tavilyEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Orange)
                    )
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
                        enabled = !agentModelsSaving,
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
            onDraftChange = { key, value -> limitDrafts = limitDrafts + (key to value.filter { it.isDigit() }.take(7)) },
            onSave = {
                screenScope.launch {
                    limitsSaving = true
                    try {
                        val next = draftsToLimits(runtimeLimits, limitDrafts)
                        val response = ApiClient.agentApi().updateConfig(
                            AgentConfigRequest(limits = next)
                        )
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
        "agent.lese_snippet_chars",
        "Leseagent-Schnipsel",
        "Zeichen je Treffer",
        "Wie viel Text der Leseagent pro Treffer zum Auswählen sieht.",
        "Der Leseagent liest nicht sofort jeden Volltext. Er bekommt pro Treffer nur einen relevanten Ausschnitt und entscheidet daraus, welche Einträge wirklich zur Frage passen. Größer gibt ihm mehr Kontext und hilft bei langen Einträgen. Kleiner ist schneller und spart Kontext, kann aber wichtige Stellen außerhalb des Schnipsels übersehen."
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
        "agent.history_max",
        "Gesprächsverlauf",
        "Nachrichten",
        "Wie viele alte Chat-Nachrichten der Agent zusätzlich mitnimmt.",
        "Dieser Wert ist getrennt von der Gedächtnissuche. Er sagt nur, wie viel vom laufenden Gespräch als Kontext an das Modell geht. Größer hilft, wenn du dich auf frühere Chatstellen beziehst. Kleiner spart Kontext und reduziert Verwirrung durch alte Nebenthemen. 0 bedeutet: kein alter Verlauf."
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
    )
)

private fun limitsToDrafts(limits: RuntimeLimits): Map<String, String> = mapOf(
    "agent.history_max" to limits.agent.history_max.toString(),
    "agent.recall_full_limit" to limits.agent.recall_full_limit.toString(),
    "agent.multi_query_variants" to limits.agent.multi_query_variants.toString(),
    "agent.lese_snippet_chars" to limits.agent.lese_snippet_chars.toString(),
    "agent.answer_hit_chars" to limits.agent.answer_hit_chars.toString(),
    "agent.answer_total_chars" to limits.agent.answer_total_chars.toString(),
    "agent.recall_normal_max" to limits.agent.recall_normal_max.toString(),
    "agent.context_prompt_max_chars" to limits.agent.context_prompt_max_chars.toString(),
    "agent.chat_text_max_chars" to limits.agent.chat_text_max_chars.toString(),
    "brain.chunk_chars" to limits.brain.chunk_chars.toString(),
    "brain.chunk_overlap" to limits.brain.chunk_overlap.toString(),
    "brain.search_overfetch_factor" to limits.brain.search_overfetch_factor.toString(),
    "brain.bm25_candidate_factor" to limits.brain.bm25_candidate_factor.toString(),
    "brain.bm25_min_candidates" to limits.brain.bm25_min_candidates.toString()
)

private fun limitInt(drafts: Map<String, String>, key: String, fallback: Int): Int =
    drafts[key]?.toIntOrNull() ?: fallback

private fun draftsToLimits(current: RuntimeLimits, drafts: Map<String, String>): RuntimeLimits = RuntimeLimits(
    agent = AgentRuntimeLimits(
        history_max = limitInt(drafts, "agent.history_max", current.agent.history_max),
        recall_full_limit = limitInt(drafts, "agent.recall_full_limit", current.agent.recall_full_limit),
        multi_query_variants = limitInt(drafts, "agent.multi_query_variants", current.agent.multi_query_variants),
        lese_snippet_chars = limitInt(drafts, "agent.lese_snippet_chars", current.agent.lese_snippet_chars),
        answer_hit_chars = limitInt(drafts, "agent.answer_hit_chars", current.agent.answer_hit_chars),
        answer_total_chars = limitInt(drafts, "agent.answer_total_chars", current.agent.answer_total_chars),
        recall_normal_max = limitInt(drafts, "agent.recall_normal_max", current.agent.recall_normal_max),
        context_prompt_max_chars = limitInt(drafts, "agent.context_prompt_max_chars", current.agent.context_prompt_max_chars),
        chat_text_max_chars = limitInt(drafts, "agent.chat_text_max_chars", current.agent.chat_text_max_chars)
    ),
    brain = BrainRuntimeLimits(
        chunk_chars = limitInt(drafts, "brain.chunk_chars", current.brain.chunk_chars),
        chunk_overlap = limitInt(drafts, "brain.chunk_overlap", current.brain.chunk_overlap),
        search_overfetch_factor = limitInt(drafts, "brain.search_overfetch_factor", current.brain.search_overfetch_factor),
        bm25_candidate_factor = limitInt(drafts, "brain.bm25_candidate_factor", current.brain.bm25_candidate_factor),
        bm25_min_candidates = limitInt(drafts, "brain.bm25_min_candidates", current.brain.bm25_min_candidates)
    )
)

@Composable
private fun LimitSettingsCard(
    isDark: Boolean,
    drafts: Map<String, String>,
    status: String,
    saving: Boolean,
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
                    enabled = !saving,
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
                Text(item.summary, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

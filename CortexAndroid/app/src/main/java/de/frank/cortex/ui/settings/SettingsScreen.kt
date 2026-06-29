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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
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
import de.frank.cortex.network.ApiClient
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.ui.theme.*
import de.frank.cortex.vpn.WireGuardManager
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
    var ttsEnabled by remember { mutableStateOf(SettingsStore.ttsEnabled) }
    var ttsVoice by remember { mutableStateOf(SettingsStore.ttsVoice.removePrefix("de-DE-Chirp3-HD-")) }
    var recordingToneEnabled by remember { mutableStateOf(SettingsStore.recordingToneEnabled) }
    var recordingToneVolume by remember { mutableStateOf(SettingsStore.recordingToneVolume) }
    var biometricLockEnabled by remember { mutableStateOf(SettingsStore.biometricLockEnabled) }
    var wgConfig by remember { mutableStateOf(SettingsStore.wgConfig) }
    val screenScope = rememberCoroutineScope()
    var agentModelOptions by remember {
        mutableStateOf(listOf("gemini-3.1-flash-lite", "gemini-2.5-flash", "minimax/minimax-m3"))
    }
    var hauptModel by remember { mutableStateOf(agentModelOptions.first()) }
    var speicherModel by remember { mutableStateOf(agentModelOptions.first()) }
    var abfrageModel by remember { mutableStateOf(agentModelOptions.first()) }
    var reasoningOptions by remember { mutableStateOf(listOf("none", "minimal", "low", "medium", "high", "xhigh")) }
    var hauptReasoning by remember { mutableStateOf("medium") }
    var speicherReasoning by remember { mutableStateOf("medium") }
    var abfrageReasoning by remember { mutableStateOf("medium") }
    var agentModelsLoading by remember { mutableStateOf(false) }
    var agentModelsSaving by remember { mutableStateOf(false) }
    var agentModelStatus by remember { mutableStateOf("") }
    var codexConnected by remember { mutableStateOf(false) }
    var codexStatus by remember { mutableStateOf("Server-Codex-Status wird geladen") }
    var codexUserCode by remember { mutableStateOf("") }
    var codexAuthId by remember { mutableStateOf("") }
    var codexConnecting by remember { mutableStateOf(false) }
    var selectedContextPromptMode by remember { mutableStateOf(SettingsStore.CONTEXT_MODE_SMALLTALK) }
    var contextPromptDraft by remember { mutableStateOf(SettingsStore.contextPrompt(selectedContextPromptMode)) }
    var contextPromptEditing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val text = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
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
            val models = config.models
            val reasoning = config.reasoning
            hauptModel = models["haupt"] ?: config.model ?: agentModelOptions.first()
            speicherModel = models["speicher"] ?: config.model ?: hauptModel
            abfrageModel = models["abfrage"] ?: config.model ?: hauptModel
            hauptReasoning = reasoning["haupt"] ?: "medium"
            speicherReasoning = reasoning["speicher"] ?: "medium"
            abfrageReasoning = reasoning["abfrage"] ?: "medium"
            codexConnected = config.codex?.connected == true
            codexStatus = if (codexConnected) "Server verbunden — GPT/Codex-Modelle sind auswählbar" else "Server nicht verbunden"
            agentModelStatus = "Aktueller Server-Stand geladen"
        } catch (e: Exception) {
            CortexLog.warn("Settings", "loadAgentModels", "Agent-Modelle nicht geladen: ${e.message}")
            agentModelStatus = "Server-Stand nicht erreichbar"
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
                SecretRow("Gemini-Schlüssel (Vorlesen + Verbessern)", geminiApiKey,
                    { geminiApiKey = it; SettingsStore.geminiApiKey = it }, isDark,
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
                // TTS toggle
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (ttsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        null, tint = Orange, modifier = Modifier.size(21.dp)
                    )
                    Text("Antworten vorlesen", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    // iOS-style toggle
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (ttsEnabled) Orange else if (isDark) DarkFieldBorder else LightFieldBorder,
                        modifier = Modifier
                            .width(46.dp)
                            .height(27.dp)
                            .clickable { ttsEnabled = !ttsEnabled; SettingsStore.ttsEnabled = ttsEnabled }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(3.dp),
                            contentAlignment = if (ttsEnabled) Alignment.CenterEnd else Alignment.CenterStart
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

                // Voice selector
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.RecordVoiceOver, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp))
                    Text("Stimme", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    var showVoiceDialog by remember { mutableStateOf(false) }
                    val pcmPlayer = remember { PcmPlayer() }
                    val scope = rememberCoroutineScope()

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (isDark) DarkField else LightField,
                        border = BorderStroke(1.dp, if (isDark) DarkFieldBorder else LightFieldBorder),
                        modifier = Modifier.clickable { showVoiceDialog = true }
                    ) {
                        Text(
                            voiceLabelFor(ttsVoice),
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
                                    } catch (e: Exception) {
                                        CortexLog.error("Settings", "testVoice", "TTS-Test fehlgeschlagen: ${e.message}")
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

                AgentModelDropdown("Hauptagent", hauptModel, agentModelOptions, isDark) { hauptModel = it }
                if (isCodexModel(hauptModel)) {
                    AgentModelDropdown("Thinking", hauptReasoning, reasoningOptions, isDark) { hauptReasoning = it }
                }
                AgentModelDropdown("Speicheragent", speicherModel, agentModelOptions, isDark) { speicherModel = it }
                if (isCodexModel(speicherModel)) {
                    AgentModelDropdown("Thinking", speicherReasoning, reasoningOptions, isDark) { speicherReasoning = it }
                }
                AgentModelDropdown("Abfrageagent", abfrageModel, agentModelOptions, isDark) { abfrageModel = it }
                if (isCodexModel(abfrageModel)) {
                    AgentModelDropdown("Thinking", abfrageReasoning, reasoningOptions, isDark) { abfrageReasoning = it }
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
                                            abfrage_reasoning = abfrageReasoning
                                        )
                                    )
                                    agentModelStatus = "Modelle gespeichert"
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
                        Text("Zusatzauftrag für Smalltalk, Speichern und Suchen", fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                ContextPromptModeButtons(
                    selectedMode = selectedContextPromptMode,
                    isDark = isDark,
                    onSelect = { mode ->
                        selectedContextPromptMode = mode
                        contextPromptDraft = SettingsStore.contextPrompt(mode)
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
                            contextPromptDraft = SettingsStore.contextPrompt(selectedContextPromptMode)
                            contextPromptEditing = false
                        }) { Text("Abbrechen") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                SettingsStore.setContextPrompt(selectedContextPromptMode, contextPromptDraft)
                                contextPromptEditing = false
                                Toast.makeText(context, "Kontext-Prompt gespeichert", Toast.LENGTH_SHORT).show()
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

        // Speichern button
        Button(
            onClick = { /* save all */ },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Iris)
        ) {
            Text("Speichern", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ContextPromptModeButtons(
    selectedMode: String,
    isDark: Boolean,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val items = listOf(
            Triple(SettingsStore.CONTEXT_MODE_SMALLTALK, Icons.Default.Forum, "Smalltalk"),
            Triple(SettingsStore.CONTEXT_MODE_SAVE, Icons.Default.Save, "Speichern"),
            Triple(SettingsStore.CONTEXT_MODE_SEARCH, Icons.Default.Search, "Suchen")
        )
        items.forEach { (mode, icon, label) ->
            val active = selectedMode == mode
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (active) Orange.copy(alpha = 0.18f) else if (isDark) DarkField else LightField,
                border = BorderStroke(1.dp, if (active) Orange.copy(alpha = 0.55f) else if (isDark) DarkFieldBorder else LightFieldBorder),
                modifier = Modifier.weight(1f).clickable { onSelect(mode) }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(icon, null, tint = if (active) Orange else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun isCodexModel(model: String): Boolean {
    val m = model.lowercase()
    return m.startsWith("gpt-") || m.startsWith("codex/") || m.startsWith("openai-codex/")
}

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
                        text = { Text(option, fontFamily = JetBrainsMono, fontSize = 12.5.sp) },
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
                Text("Chirp 3D · Gemini TTS",
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

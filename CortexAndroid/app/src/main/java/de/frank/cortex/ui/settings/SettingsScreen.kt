package de.frank.cortex.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.cortex.BuildConfig
import de.frank.cortex.data.SettingsStore
import de.frank.cortex.ui.theme.*
import de.frank.cortex.vpn.WireGuardManager

@Composable
fun SettingsScreen(
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
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
    var ttsVoice by remember { mutableStateOf(SettingsStore.ttsVoice) }
    var wgConfig by remember { mutableStateOf(SettingsStore.wgConfig) }

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
                    divider = true)
                SecretRow("Groq-Schlüssel (Spracheingabe)", groqApiKey,
                    { groqApiKey = it; SettingsStore.groqApiKey = it }, isDark,
                    divider = true)
                SecretRow("Gemini-Schlüssel (Vorlesen + Verbessern)", geminiApiKey,
                    { geminiApiKey = it; SettingsStore.geminiApiKey = it }, isDark,
                    divider = false)
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
                    val voices = listOf("Aoede", "Charon", "Fenrir", "Kore", "Puck")
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (isDark) DarkField else LightField,
                            border = BorderStroke(1.dp, if (isDark) DarkFieldBorder else LightFieldBorder),
                            modifier = Modifier.clickable { expanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ttsVoice, fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            voices.forEach { voice ->
                                DropdownMenuItem(
                                    text = { Text(voice) },
                                    onClick = { ttsVoice = voice; SettingsStore.ttsVoice = voice; expanded = false }
                                )
                            }
                        }
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
                data class ThemeOpt(val id: String, val name: String, val icon: String)
                val opts = listOf(
                    ThemeOpt("dark", "Dunkel", "dark_mode"),
                    ThemeOpt("light", "Hell", "light_mode")
                )
                opts.forEach { opt ->
                    val active = (opt.id == "dark" && isDark) || (opt.id == "light" && !isDark)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) Iris.copy(alpha = 0.14f)
                        else if (isDark) DarkField else LightField,
                        border = BorderStroke(1.dp,
                            if (active) Iris else if (isDark) DarkFieldBorder else LightFieldBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                // Nur toggeln wenn der aktive Modus nicht schon gewählt ist
                                if (!active) onToggleTheme()
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (opt.id == "dark") Icons.Default.DarkMode else Icons.Default.LightMode,
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
                    Text("\uD83E\uDDE0", fontSize = 21.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cortex", fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp)
                    Text("Version ${BuildConfig.VERSION_NAME} \u00B7 privat",
                        fontFamily = JetBrainsMono, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = Mint.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp), tint = Mint)
                        Text("lokal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Mint)
                    }
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
    divider: Boolean
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
                    .clickable { visible = !visible },
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

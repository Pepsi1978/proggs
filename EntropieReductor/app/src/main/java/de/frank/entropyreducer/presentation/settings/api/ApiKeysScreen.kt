package de.frank.entropyreducer.presentation.settings.api

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.remote.tts.GoogleTtsVoice
import de.frank.entropyreducer.data.remote.tts.VoiceGender
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.settings.drive.DriveBackupSection
import de.frank.entropyreducer.presentation.settings.ApiKeysUiState
import de.frank.entropyreducer.presentation.settings.ApiKeysViewModel
import de.frank.entropyreducer.presentation.settings.ConnectionStatus
import de.frank.entropyreducer.presentation.settings.TtsSpeakState
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

@Composable
fun ApiKeysScreen(
    onBack: () -> Unit,
    vm: ApiKeysViewModel = hiltViewModel(),
    oauthVm: OAuthViewModel = hiltViewModel(),
    ouraVm: OuraApiViewModel = hiltViewModel(),
    healthConnectVm: HealthConnectApiViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val oauthState by oauthVm.state.collectAsStateWithLifecycle()
    val ouraState by ouraVm.state.collectAsStateWithLifecycle()
    val healthConnectState by healthConnectVm.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    CosmosScaffold(
        title = "API-Schlüssel",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Verwalte deine API-Schlüssel und Verbindungen. Deine Daten werden verschluesselt gespeichert und niemals ohne deine Zustimmung weitergegeben.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textSecondary,
                )
            }
            // Frank-Wunsch 2026-05-23: "Alles neu laden"-Knopf ganz oben. Laedt die komplette
            // Historie aller Quellen neu (umgeht den inkrementellen Sync) — nur auf manuellen Druck.
            item {
                ReloadAllDataCard(
                    onReload = oauthVm::reloadAllData,
                    message = oauthState.message,
                )
            }
            // Frank-Wunsch 2026-05-10: Google-Drive-Backup gehoert thematisch zu allen
            // Login/API-relevanten Bereichen — nicht unter Datenexport. Daher ganz oben
            // direkt nach dem Intro-Text. Google Calendar gleich darunter (zweiter
            // Frank-Wunsch 2026-05-10) — beide Google-Logins beieinander.
            item { DriveBackupSection() }
            item { GoogleCalendarOAuthCard(oauthVm, oauthState) }
            item {
                ApiKeyCard(
                    title = "Groq API Key",
                    subtitle = "Für schnelle KI-Antworten über das Groq LLM API.",
                    accent = LocalCosmos.current.crit,
                    value = state.groqKey,
                    onValueChange = vm::setGroq,
                    onSave = vm::saveGroq,
                    onTest = vm::testGroq,
                    status = state.groqStatus,
                )
            }
            item {
                ApiKeyCard(
                    title = "Gemini API Key",
                    subtitle = "Für fortschrittliche KI-Modelle von Google Gemini.",
                    accent = LocalCosmos.current.accentForscher,
                    value = state.geminiKey,
                    onValueChange = vm::setGemini,
                    onSave = vm::saveGemini,
                    onTest = vm::testGemini,
                    status = state.geminiStatus,
                )
            }
            item {
                ApiKeyCard(
                    title = "Google Cloud TTS API Key",
                    subtitle = "Für hochwertige Text-zu-Sprache Ausgaben (Chirp 3 HD).",
                    accent = LocalCosmos.current.accent,
                    value = state.ttsKey,
                    onValueChange = vm::setTts,
                    onSave = vm::saveTts,
                    onTest = vm::testTts,
                    status = state.ttsStatus,
                )
            }
            item {
                SecondBrainConnectorCard(
                    state = state,
                    onValueChange = vm::setSecondBrain,
                    onSave = vm::saveSecondBrain,
                    onTest = vm::testSecondBrain,
                    onWireGuardConfigChange = vm::setSecondBrainWireGuardConfig,
                    onSaveWireGuardConfig = vm::saveSecondBrainWireGuardConfig,
                )
            }
            item {
                TtsVoiceCard(
                    state = state,
                    onSelectVoice = vm::setTtsVoice,
                    onTestSpeak = vm::testSpeak,
                    onStop = vm::stopTtsPreview,
                )
            }
            item { PolarOAuthCard(oauthVm, oauthState) }
            item { WhoopOAuthCard(oauthVm, oauthState) }
            item { OuraApiCard(ouraVm, ouraState) }
            item { HealthConnectApiCard(healthConnectVm, healthConnectState) }
            item {
                Text(
                    text = "Deine API-Schlüssel werden mit AES-256-GCM auf deinem Gerät verschluesselt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SecondBrainConnectorCard(
    state: ApiKeysUiState,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onWireGuardConfigChange: (String) -> Unit,
    onSaveWireGuardConfig: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Link, null, tint = cosmos.accentTasksSub, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text("Second-Brain-Verbindung", style = MaterialTheme.typography.titleMedium, color = cosmos.accentTasksSub)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Verbindung zum Second Brain (Schlüssel + WireGuard). Welche Inhalte synchronisiert " +
                    "werden (z. B. Ideen), stellst du im Bereich „Second Brain“ ein.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            ApiKeyCard(
                title = "Second Brain API Key",
                subtitle = "Privater Bearer-Key für http://10.8.0.1:8000 über WireGuard.",
                accent = cosmos.accentTasksSub,
                value = state.secondBrainKey,
                onValueChange = onValueChange,
                onSave = onSave,
                onTest = onTest,
                status = state.secondBrainStatus,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.secondBrainWireGuardConfig,
                onValueChange = onWireGuardConfigChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                placeholder = { Text("WireGuard-Konfiguration aus Cortex einfügen", color = cosmos.textSecondary) },
                minLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = cosmos.textPrimary,
                    unfocusedTextColor = cosmos.textPrimary,
                    cursorColor = cosmos.accentTasksSub,
                    focusedIndicatorColor = cosmos.accentTasksSub,
                    unfocusedIndicatorColor = cosmos.glassBorder,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSaveWireGuardConfig,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = cosmos.accentTasksSub),
            ) {
                Text(if (state.secondBrainWireGuardSaved) "WireGuard-Konfig aktualisieren" else "WireGuard-Konfig speichern")
            }
        }
    }
}

/**
 * Frank-Wunsch 2026-05-23: "Alles neu laden"-Knopf ganz oben in den API-Einstellungen.
 * Loest einen vollstaendigen (nicht-inkrementellen) Sync aller Datenquellen aus — nuetzlich
 * z.B. nach einem Geraetewechsel. Passiert NUR auf manuellen Druck.
 */
@Composable
private fun ReloadAllDataCard(onReload: () -> Unit, message: String?) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                "Alles neu laden",
                style = MaterialTheme.typography.titleMedium,
                color = LocalCosmos.current.accent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Holt die komplette Historie von Whoop, Oura und Health Connect neu — " +
                    "statt nur der neuesten Daten. Sinnvoll z.B. nach einem Geraetewechsel. " +
                    "Laeuft nur, wenn du diesen Knopf drueckst.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.Button(
                onClick = onReload,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Alle Daten neu laden")
            }
            if (!message.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun ApiKeyCard(
    title: String,
    subtitle: String,
    accent: Color,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    status: ConnectionStatus,
) {
    val cosmos = LocalCosmos.current
    var hidden by remember { mutableStateOf(true) }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = accent)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("API Key", color = cosmos.textSecondary) },
                visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { hidden = !hidden }) {
                        Icon(
                            imageVector = if (hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (hidden) "Anzeigen" else "Verbergen",
                            tint = cosmos.textSecondary,
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = cosmos.textPrimary,
                    unfocusedTextColor = cosmos.textPrimary,
                    cursorColor = accent,
                    focusedIndicatorColor = accent,
                    unfocusedIndicatorColor = cosmos.glassBorder,
                ),
            )
            Spacer(Modifier.height(8.dp))
            when (status) {
                ConnectionStatus.OK -> ConnectionLabel(
                    label = "Verbindung erfolgreich",
                    color = LocalCosmos.current.ok,
                    icon = Icons.Outlined.CheckCircle,
                )
                ConnectionStatus.FAIL -> ConnectionLabel(
                    label = "Ungueltiger API-Schlüssel oder fehlende Berechtigungen",
                    color = LocalCosmos.current.crit,
                    icon = Icons.Outlined.ErrorOutline,
                )
                ConnectionStatus.TESTING -> ConnectionLabel(
                    label = "Teste Verbindung …",
                    color = cosmos.textSecondary,
                    icon = Icons.Outlined.HourglassBottom,
                )
                ConnectionStatus.UNKNOWN -> Unit
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onTest, modifier = Modifier.weight(1f)) {
                    Text("Verbindung testen")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                ) {
                    Text("Speichern")
                }
            }
        }
    }
}

@Composable
private fun ConnectionLabel(label: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun WhoopOAuthCard(vm: OAuthViewModel, state: OAuthUiState) {
    val cosmos = LocalCosmos.current
    var secretHidden by remember { mutableStateOf(true) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        result.data?.let { data -> vm.onWhoopAuthResult(data) }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                "Whoop OAuth",
                style = MaterialTheme.typography.titleMedium,
                color = LocalCosmos.current.accentForscher,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Verbinde deinen Whoop-Account für Recovery, HRV und Schlafdaten. " +
                    "Registriere dein Developer-App in app.whoop.com mit folgender Redirect-URI:",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                state.whoopRedirectUri,
                style = MaterialTheme.typography.bodySmall,
                color = LocalCosmos.current.accent,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.whoopClientId,
                onValueChange = vm::setWhoopClientId,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Client ID", color = cosmos.textSecondary) },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.whoopClientSecret,
                onValueChange = vm::setWhoopClientSecret,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Client Secret", color = cosmos.textSecondary) },
                visualTransformation = if (secretHidden) PasswordVisualTransformation() else VisualTransformation.None,
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { secretHidden = !secretHidden }) {
                        Icon(
                            imageVector = if (secretHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (secretHidden) "Anzeigen" else "Verbergen",
                            tint = cosmos.textSecondary,
                        )
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = vm::saveWhoopCredentials,
                    modifier = Modifier.weight(1f),
                ) { Text("Speichern") }
                if (state.whoopConnected) {
                    Button(
                        onClick = vm::disconnectWhoop,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalCosmos.current.crit),
                    ) {
                        Icon(Icons.Outlined.LinkOff, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Trennen")
                    }
                } else {
                    Button(
                        onClick = {
                            vm.buildWhoopAuthIntent()?.let { launcher.launch(it) }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = LocalCosmos.current.accentForscher),
                    ) {
                        Icon(Icons.Outlined.Link, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Verbinden")
                    }
                }
            }
            if (state.whoopConnected) {
                Spacer(Modifier.height(8.dp))
                ConnectionLabel("Verbunden", LocalCosmos.current.ok, Icons.Outlined.CheckCircle)
            }
        }
    }
}

/**
 * Polar-Historie-Card (Frank-Wunsch 2026-05-16): reiner ZIP-Bulk-Import der
 * Polar-Trainings-Historie. Keine Live-API-Anbindung — Live-Trainings kommen
 * ueber Health Connect (siehe HealthConnectManager).
 */
@Composable
private fun PolarOAuthCard(vm: OAuthViewModel, state: OAuthUiState) {
    val cosmos = LocalCosmos.current
    // Frank-Wunsch 2026-05-17: Nur noch ZIP-Bulk-Import — keine OAuth-Verbindung
    // mehr. Polar AccessLink V3/V4/Flow-Web sind komplett entfernt. Live-Trainings
    // kommen ueber Health Connect, Historie ueber den ZIP-Bulk-Import.
    val bulkImportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { vm.startPolarBulkImport(it) }
    }
    val polarRed = Color(0xFFE6014C)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                "Polar-Historie (ZIP-Import)",
                style = MaterialTheme.typography.titleMedium,
                color = polarRed,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Hast du eine Polar-Export-ZIP per Mail erhalten (von " +
                    "flow.polar.com/data/export-data)? Hier kannst du ALLE " +
                    "Trainings deiner Polar-Historie auf einmal importieren. " +
                    "Bei 10 Jahren Trainings dauert der Vorgang ca. 30-60 Sek. " +
                    "Live-Daten kommen ueber Health Connect.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { bulkImportPicker.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Polar-Export-ZIP auswaehlen")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Powered by Polar — Daten nur fuer persoenliche Analyse.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
        }
    }
}

@Composable
private fun GoogleCalendarOAuthCard(vm: OAuthViewModel, state: OAuthUiState) {
    val cosmos = LocalCosmos.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (val signInResult = vm.calendarSignIn.accountFromResult(result.data)) {
            is de.frank.entropyreducer.data.remote.calendar.CalendarSignInHelper.SignInResult.Success ->
                vm.onCalendarSignInSuccess(signInResult.account)
            is de.frank.entropyreducer.data.remote.calendar.CalendarSignInHelper.SignInResult.Error ->
                vm.onCalendarSignInError(signInResult.message)
        }
    }

    val connected = state.calendarAccountEmail != null

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                "Google Calendar",
                style = MaterialTheme.typography.titleMedium,
                color = LocalCosmos.current.accent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Verbinde deinen Google-Kalender — die App liest die Schichtcodes als " +
                    "Ganztagestermine ('Tag 1', 'Nacht 2 X', 'U' für Urlaub, etc.). " +
                    "Login laeuft über Google Sign-In; keine Client-ID-Eingabe nötig.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            if (connected) {
                Text(
                    "Verbunden mit ${state.calendarAccountEmail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalCosmos.current.ok,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = vm::disconnectGoogleCalendar,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LocalCosmos.current.crit),
                ) {
                    Icon(Icons.Outlined.LinkOff, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Trennen")
                }
            } else {
                Button(
                    onClick = { launcher.launch(vm.calendarSignIn.signInIntent()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LocalCosmos.current.accent),
                ) {
                    Icon(Icons.Outlined.Link, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Mit Google Calendar verbinden")
                }
            }
            state.message?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(msg, style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary)
            }
        }
    }
}

/* ----------------- TTS Voice-Auswahl ----------------- */

@Composable
private fun TtsVoiceCard(
    state: ApiKeysUiState,
    onSelectVoice: (String) -> Unit,
    onTestSpeak: () -> Unit,
    onStop: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    var showPicker by remember { mutableStateOf(false) }
    val current = state.ttsVoices.firstOrNull { it.name == state.ttsVoiceName }
    val displayName = current?.displayName ?: state.ttsVoiceName

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.RecordVoiceOver,
                    null,
                    tint = LocalCosmos.current.accent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Stimme",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalCosmos.current.accent,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Wähle eine Chirp-3-HD-Stimme. Sie liest Tagesbriefings, Wochenrückblicke und Genie-Antworten.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(12.dp))

            // Aktuelle Auswahl, klickbar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = cosmos.glassBorder.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable(enabled = state.ttsSaved) { showPicker = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (state.ttsSaved) displayName else "Erst API-Schlüssel speichern",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.ttsSaved) cosmos.textPrimary else cosmos.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Ändern",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.ttsSaved) LocalCosmos.current.accent else cosmos.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Sprich-Test-Button mit Loading-/Speaking-/Error-State
            when (state.ttsSpeakState) {
                TtsSpeakState.IDLE -> {
                    OutlinedButton(
                        onClick = onTestSpeak,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.ttsSaved,
                    ) {
                        Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Stimme anhören")
                    }
                }
                TtsSpeakState.LOADING -> {
                    OutlinedButton(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = LocalCosmos.current.accent,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Lade Audio …")
                    }
                }
                TtsSpeakState.SPEAKING -> {
                    Button(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalCosmos.current.accent,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Icon(Icons.Outlined.Stop, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Stoppen")
                    }
                }
                TtsSpeakState.ERROR -> {
                    OutlinedButton(
                        onClick = onTestSpeak,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = LocalCosmos.current.crit,
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("Erneut versuchen")
                    }
                }
            }

            state.ttsLastError?.let { msg ->
                Spacer(Modifier.height(6.dp))
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalCosmos.current.crit,
                )
            }
        }
    }

    if (showPicker) {
        VoicePickerDialog(
            voices = state.ttsVoices,
            selected = state.ttsVoiceName,
            onSelect = { name ->
                onSelectVoice(name)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun VoicePickerDialog(
    voices: List<GoogleTtsVoice>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            OutlinedButton(onClick = onDismiss) { Text("Abbrechen") }
        },
        title = {
            Text("Chirp-3-HD-Stimme wählen", color = cosmos.textPrimary)
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(voices, key = { it.name }) { voice ->
                    VoiceRow(
                        voice = voice,
                        isSelected = voice.name == selected,
                        onClick = { onSelect(voice.name) },
                    )
                }
            }
        },
    )
}

@Composable
private fun VoiceRow(
    voice: GoogleTtsVoice,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = when (voice.gender) {
        VoiceGender.FEMALE -> LocalCosmos.current.accent
        VoiceGender.MALE -> LocalCosmos.current.accentForscher
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = if (isSelected) accent.copy(alpha = 0.18f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.RecordVoiceOver,
            null,
            tint = accent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            voice.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = cosmos.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                Icons.Outlined.CheckCircle,
                null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun formatRelativeTime(epochMs: Long): String {
    val now = System.currentTimeMillis()
    val diffSec = (now - epochMs) / 1_000L
    return when {
        diffSec < 60 -> "gerade eben"
        diffSec < 3600 -> "vor ${diffSec / 60} Minuten"
        diffSec < 86400 -> "vor ${diffSec / 3600} Stunden"
        else -> "vor ${diffSec / 86400} Tagen"
    }
}

/* ----------------- Oura Ring (Personal Access Token) ----------------- */

@Composable
private fun OuraApiCard(vm: OuraApiViewModel, state: OuraApiUiState) {
    val cosmos = LocalCosmos.current
    var hidden by remember { mutableStateOf(true) }
    val accent = LocalCosmos.current.accent
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                "Oura Ring",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Verbinde deinen Oura Ring fuer Readiness-Score, Schlafqualitaet, " +
                    "Aktivitaet, Resilienz und detaillierte Schlafphasen. Erstelle einen " +
                    "Personal Access Token unter cloud.ouraring.com/personal-access-tokens " +
                    "und fuege ihn hier ein. Der Token wird verschluesselt auf dem Geraet " +
                    "gespeichert.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.tokenInput,
                onValueChange = vm::setToken,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Personal Access Token", color = cosmos.textSecondary) },
                visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                singleLine = true,
                enabled = !state.busy,
                trailingIcon = {
                    IconButton(onClick = { hidden = !hidden }) {
                        Icon(
                            imageVector = if (hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (hidden) "Anzeigen" else "Verbergen",
                            tint = cosmos.textSecondary,
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = cosmos.textPrimary,
                    unfocusedTextColor = cosmos.textPrimary,
                    cursorColor = accent,
                    focusedIndicatorColor = accent,
                    unfocusedIndicatorColor = cosmos.glassBorder,
                ),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.tokenSaved) {
                    OutlinedButton(
                        onClick = vm::runSync,
                        modifier = Modifier.weight(1f),
                        enabled = !state.busy,
                    ) {
                        if (state.busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = accent,
                            )
                            Spacer(Modifier.size(8.dp))
                        }
                        Text("Sync starten")
                    }
                    Button(
                        onClick = vm::disconnect,
                        modifier = Modifier.weight(1f),
                        enabled = !state.busy,
                        colors = ButtonDefaults.buttonColors(containerColor = LocalCosmos.current.crit),
                    ) {
                        Icon(Icons.Outlined.LinkOff, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Trennen")
                    }
                } else {
                    Button(
                        onClick = vm::saveAndConnect,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.Black,
                        ),
                    ) {
                        if (state.busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.Black,
                            )
                            Spacer(Modifier.size(8.dp))
                        } else {
                            Icon(Icons.Outlined.Link, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                        }
                        Text("Verbinden")
                    }
                }
            }
            if (state.tokenSaved) {
                Spacer(Modifier.height(8.dp))
                ConnectionLabel(
                    label = "Verbunden",
                    color = LocalCosmos.current.ok,
                    icon = Icons.Outlined.CheckCircle,
                )
                if (state.lastSyncMs > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Letzter Sync: " + formatRelativeTime(state.lastSyncMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
            }
            state.message?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            }
        }
    }
}

/* ----------------- Health Connect (Gewicht, Koerperfett, Magermasse) ----------------- */

@Composable
private fun HealthConnectApiCard(
    vm: HealthConnectApiViewModel,
    state: HealthConnectApiUiState,
) {
    val cosmos = LocalCosmos.current
    val accent = LocalCosmos.current.ok
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.health.connect.client.PermissionController
            .createRequestPermissionResultContract(),
    ) { granted ->
        vm.refresh()
        val needed = vm.requiredPermissions()
        val allOk = granted.containsAll(needed)
        vm.setMessage(
            if (allOk) "Alle Berechtigungen erteilt — Werte werden beim naechsten Refresh geladen."
            else "Nur ${granted.intersect(needed).size} von ${needed.size} Berechtigungen erteilt.",
        )
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                "Health Connect",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Liest dein Gewicht, Koerperfett und die Magermasse aus Health Connect — " +
                    "die Zepp-App synchronisiert die Werte deiner Amazfit Smart Scale dort hin. " +
                    "In Zepp musst du dazu unter Profil → 3rd Party Account Linking → Health " +
                    "Connect die Schreibrechte einschalten.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            if (!state.available) {
                Text(
                    text = "Health Connect ist auf diesem Geraet nicht verfuegbar.",
                    color = LocalCosmos.current.crit,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                val statusText = if (state.allGranted) "Alle Berechtigungen erteilt"
                else "${state.grantedCount} von ${state.totalCount} Berechtigungen erteilt"
                val statusColor = if (state.allGranted) accent else LocalCosmos.current.warn
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.Button(
                    onClick = { launcher.launch(vm.requiredPermissions()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = accent,
                    ),
                ) {
                    Text(
                        text = if (state.allGranted) "Berechtigungen erneut bestaetigen"
                        else "Berechtigungen erteilen",
                    )
                }
            }
            state.message?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            }
        }
    }
}

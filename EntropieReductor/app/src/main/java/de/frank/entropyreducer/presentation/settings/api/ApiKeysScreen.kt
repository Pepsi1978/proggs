package de.frank.entropyreducer.presentation.settings.api

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.settings.ApiKeysViewModel
import de.frank.entropyreducer.presentation.settings.ConnectionStatus
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

@Composable
fun ApiKeysScreen(
    onBack: () -> Unit,
    vm: ApiKeysViewModel = hiltViewModel(),
    oauthVm: OAuthViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val oauthState by oauthVm.state.collectAsState()
    val cosmos = LocalCosmos.current
    CosmosScaffold(
        title = "API-Schluessel",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurueck", tint = cosmos.textPrimary)
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
                    text = "Verwalte deine API-Schluessel und Verbindungen. Deine Daten werden verschluesselt gespeichert und niemals ohne deine Zustimmung weitergegeben.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textSecondary,
                )
            }
            item {
                ApiKeyCard(
                    title = "Groq API Key",
                    subtitle = "Fuer schnelle KI-Antworten ueber das Groq LLM API.",
                    accent = CosmosColors.Critical,
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
                    subtitle = "Fuer fortschrittliche KI-Modelle von Google Gemini.",
                    accent = CosmosColors.AccentSecondary,
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
                    subtitle = "Fuer hochwertige Text-zu-Sprache Ausgaben (Chirp 3 HD).",
                    accent = CosmosColors.AccentPrimary,
                    value = state.ttsKey,
                    onValueChange = vm::setTts,
                    onSave = vm::saveTts,
                    onTest = vm::testTts,
                    status = state.ttsStatus,
                )
            }
            item { WhoopOAuthCard(oauthVm, oauthState) }
            item { GoogleCalendarOAuthCard(oauthVm, oauthState) }
            item {
                Text(
                    text = "Deine API-Schluessel werden mit AES-256-GCM auf deinem Geraet verschluesselt.",
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
                    color = CosmosColors.Success,
                    icon = Icons.Outlined.CheckCircle,
                )
                ConnectionStatus.FAIL -> ConnectionLabel(
                    label = "Ungueltiger API-Schluessel oder fehlende Berechtigungen",
                    color = CosmosColors.Critical,
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
                color = CosmosColors.AccentSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Verbinde deinen Whoop-Account fuer Recovery, HRV und Schlafdaten. " +
                    "Registriere dein Developer-App in app.whoop.com mit folgender Redirect-URI:",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                state.whoopRedirectUri,
                style = MaterialTheme.typography.bodySmall,
                color = CosmosColors.AccentPrimary,
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
                        colors = ButtonDefaults.buttonColors(containerColor = CosmosColors.Critical),
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
                        colors = ButtonDefaults.buttonColors(containerColor = CosmosColors.AccentSecondary),
                    ) {
                        Icon(Icons.Outlined.Link, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Verbinden")
                    }
                }
            }
            if (state.whoopConnected) {
                Spacer(Modifier.height(8.dp))
                ConnectionLabel("Verbunden", CosmosColors.Success, Icons.Outlined.CheckCircle)
            }
        }
    }
}

@Composable
private fun GoogleCalendarOAuthCard(vm: OAuthViewModel, state: OAuthUiState) {
    val cosmos = LocalCosmos.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        result.data?.let { data -> vm.onGoogleAuthResult(data, state.googleClientId) }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                "Google Calendar OAuth",
                style = MaterialTheme.typography.titleMedium,
                color = CosmosColors.AccentPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Verbinde deinen Google-Kalender — die App liest die Schichtcodes als " +
                    "Ganztagestermine. Trage die OAuth-Client-ID aus der Cloud Console ein " +
                    "und stelle die folgende Redirect-URI dort bereit:",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                OAuthService.GOOGLE_REDIRECT_URI,
                style = MaterialTheme.typography.bodySmall,
                color = CosmosColors.AccentPrimary,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.googleClientId,
                onValueChange = vm::setGoogleClientId,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Google OAuth Client ID", color = cosmos.textSecondary) },
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            if (state.googleCalendarConnected) {
                Button(
                    onClick = vm::disconnectGoogleCalendar,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmosColors.Critical),
                ) {
                    Icon(Icons.Outlined.LinkOff, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Trennen")
                }
            } else {
                Button(
                    onClick = {
                        vm.buildGoogleAuthIntent(state.googleClientId)?.let { launcher.launch(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmosColors.AccentPrimary),
                ) {
                    Icon(Icons.Outlined.Link, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Mit Google Calendar verbinden")
                }
            }
            if (state.googleCalendarConnected) {
                Spacer(Modifier.height(8.dp))
                ConnectionLabel("Verbunden — Schichtcodes werden alle 24h gesynct", CosmosColors.Success, Icons.Outlined.CheckCircle)
            }
            state.message?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(msg, style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary)
            }
        }
    }
}

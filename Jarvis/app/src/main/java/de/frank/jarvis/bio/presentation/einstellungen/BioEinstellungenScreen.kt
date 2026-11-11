package de.frank.jarvis.bio.presentation.einstellungen

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.frank.jarvis.bio.BioModul
import de.frank.jarvis.bio.presentation.components.CosmosScaffold
import de.frank.jarvis.bio.presentation.components.GlassCard
import de.frank.jarvis.bio.presentation.theme.LocalCosmos
import de.frank.jarvis.data.Geraet

/**
 * Einstellungen des Biomarker-Bereichs: die Biomarker-Karten aus „API-Schlüssel“ von Entropie Reductor (Alles neu laden,
 * Polar-ZIP, Whoop, Oura, Health Connect). Auf dem Tablet nur ein Hinweis: Zugänge und Abruf laufen auf dem Handy.
 */
@Composable
fun BioEinstellungenScreen(onBack: () -> Unit) {
    val cosmos = LocalCosmos.current
    CosmosScaffold(
        title = "Biomarker-Einstellungen",
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary) }
        },
        compactHeader = true,
    ) { padding ->
        if (Geraet.istTablet) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Zugänge und Abruf laufen auf dem Handy. Das Tablet zeigt nur den Spiegel der Biodaten des Handys.",
                        color = cosmos.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                BioFachEinstellungen(Modifier.fillMaxWidth())
            }
            return@CosmosScaffold
        }
        ZugangsKarten(Modifier.fillMaxSize().padding(padding))
    }
}

/**
 * Die Biomarker-Einstellungen als Reiter „Biomarker“ in Jarvis' Einstellungen (Franks Vorgabe vom 11.11.2026: alle Einstellungen
 * gehören in den Einstellungsbereich). Ohne eigene Scrollliste, weil der Einstellungsbildschirm selbst scrollt.
 */
@Composable
fun BioEinstellungenInhalt(modifier: Modifier = Modifier) {
    de.frank.jarvis.bio.presentation.theme.BioThema {
        val cosmos = LocalCosmos.current
        if (Geraet.istTablet) {
            Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Zugänge und Abruf laufen auf dem Handy. Das Tablet zeigt nur den Spiegel der Biodaten des Handys.", color = cosmos.textPrimary, style = MaterialTheme.typography.bodyMedium)
                }
                BioFachEinstellungen(Modifier.fillMaxWidth())
            }
        } else {
            ZugangsKarten(modifier, alsListe = false)
        }
    }
}

@Composable
private fun ZugangsKarten(modifier: Modifier, alsListe: Boolean = true) {
    val app = LocalContext.current.applicationContext
    BioModul.einrichten(app)
    val oauthVm: OAuthViewModel = viewModel(key = "bio-oauth") {
        OAuthViewModel(app, BioModul.oauth, BioModul.geheim, BioModul.trainings, BioModul.einstellungen)
    }
    val ouraVm: OuraApiViewModel = viewModel(key = "bio-oura-api") { OuraApiViewModel(BioModul.oura, BioModul.geheim) }
    val hcVm: HealthConnectApiViewModel = viewModel(key = "bio-hc-api") { HealthConnectApiViewModel(BioModul.healthConnect) }
    // Die ViewModels leben so lange wie Jarvis' Fenster: beim Betreten der Seite den lokalen Stand neu lesen (kein Abruf).
    androidx.compose.runtime.LaunchedEffect(Unit) { oauthVm.pruefeVerbindung(); hcVm.refresh() }
    val oauthState by oauthVm.state.collectAsStateWithLifecycle()
    val ouraState by ouraVm.state.collectAsStateWithLifecycle()
    val hcState by hcVm.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    // Als eigene Seite mit eigener Scrollleiste, im Einstellungsbildschirm ohne (der scrollt selbst).
    val basis = if (alsListe) modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp) else modifier
    Column(basis, verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val breite = Modifier.widthIn(max = 720.dp).fillMaxWidth()
        Text(
            "Zugänge zu Whoop, Oura, Health Connect und der Polar-Historie. Die Schlüssel liegen verschlüsselt auf diesem Gerät und gehen nie an den Server oder an ein Modell.",
            style = MaterialTheme.typography.bodyMedium,
            color = cosmos.textSecondary,
            modifier = breite,
        )
        WhoopOAuthCard(breite, oauthVm, oauthState)
        OuraApiCard(breite, ouraVm, ouraState)
        HealthConnectApiCard(breite, hcVm, hcState)
        BioFachEinstellungen(breite)
        PolarOAuthCard(breite)
        ReloadAllDataCard(breite, onReload = oauthVm::reloadAllData, message = oauthState.message)
        if (alsListe) Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ReloadAllDataCard(modifier: Modifier, onReload: () -> Unit, message: String?) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = modifier) {
        Column {
            Text("Alles neu laden", style = MaterialTheme.typography.titleMedium, color = cosmos.accent)
            Spacer(Modifier.height(4.dp))
            Text(
                "Holt die komplette Historie von Whoop, Oura, Waage und Trainings neu — statt nur der neuesten Daten. " +
                    "Sinnvoll z. B. nach einem Gerätewechsel. Läuft nur, wenn du diesen Knopf drückst.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onReload, modifier = Modifier.fillMaxWidth()) { Text("Alle Daten neu laden") }
            if (!message.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary)
            }
        }
    }
}

@Composable
private fun ConnectionLabel(label: String, color: Color, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun WhoopOAuthCard(modifier: Modifier, vm: OAuthViewModel, state: OAuthUiState) {
    val cosmos = LocalCosmos.current
    var secretHidden by remember { mutableStateOf(true) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.let { data -> vm.onWhoopAuthResult(data) }
    }
    GlassCard(modifier = modifier) {
        Column {
            Text("Whoop OAuth", style = MaterialTheme.typography.titleMedium, color = cosmos.accentForscher)
            Spacer(Modifier.height(4.dp))
            Text(
                "Verbinde deinen Whoop-Account für Recovery, HRV und Schlafdaten. Registriere deine Developer-App in " +
                    "app.whoop.com mit folgender Redirect-URI:",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(state.whoopRedirectUri, style = MaterialTheme.typography.bodySmall, color = cosmos.accent)
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
                            if (secretHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            if (secretHidden) "Anzeigen" else "Verbergen",
                            tint = cosmos.textSecondary,
                        )
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::saveWhoopCredentials, modifier = Modifier.weight(1f)) { Text("Speichern") }
                if (state.whoopConnected) {
                    Button(
                        onClick = vm::disconnectWhoop,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = cosmos.crit),
                    ) {
                        Icon(Icons.Outlined.LinkOff, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Trennen")
                    }
                } else {
                    Button(
                        onClick = { vm.buildWhoopAuthIntent()?.let { launcher.launch(it) } },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = cosmos.accentForscher),
                    ) {
                        Icon(Icons.Outlined.Link, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Verbinden")
                    }
                }
            }
            if (state.whoopConnected) {
                Spacer(Modifier.height(8.dp))
                ConnectionLabel("Verbunden", cosmos.ok, Icons.Outlined.CheckCircle)
            }
        }
    }
}

/** Polar-Historie: reiner ZIP-Import (Live-Trainings kommen über Health Connect). */
@Composable
private fun PolarOAuthCard(modifier: Modifier) {
    val cosmos = LocalCosmos.current
    val context = LocalContext.current
    val stand by BioPolarImport.stand.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { BioPolarImport.starte(context, it) }
    }
    val polarRed = Color(0xFFE6014C)
    GlassCard(modifier = modifier) {
        Column {
            Text("Polar-Historie (ZIP-Import)", style = MaterialTheme.typography.titleMedium, color = polarRed)
            Spacer(Modifier.height(4.dp))
            Text(
                "Hast du eine Polar-Export-ZIP per Mail erhalten (von flow.polar.com/data/export-data)? Hier kannst du alle " +
                    "Trainings deiner Polar-Historie auf einmal importieren. Bei 10 Jahren Trainings dauert das ca. 30–60 Sekunden; " +
                    "bitte Jarvis so lange offen lassen. Live-Daten kommen über Health Connect.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { picker.launch("*/*") }, modifier = Modifier.fillMaxWidth(), enabled = !BioPolarImport.laeuft) {
                Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Polar-Export-ZIP auswählen")
            }
            stand?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = cosmos.textPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Text("Powered by Polar — Daten nur für persönliche Analyse.", style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary)
        }
    }
}

private fun formatRelativeTime(epochMs: Long): String {
    val diffSec = (System.currentTimeMillis() - epochMs) / 1_000L
    return when {
        diffSec < 60 -> "gerade eben"
        diffSec < 3600 -> "vor ${diffSec / 60} Minuten"
        diffSec < 86400 -> "vor ${diffSec / 3600} Stunden"
        else -> "vor ${diffSec / 86400} Tagen"
    }
}

@Composable
private fun OuraApiCard(modifier: Modifier, vm: OuraApiViewModel, state: OuraApiUiState) {
    val cosmos = LocalCosmos.current
    var hidden by remember { mutableStateOf(true) }
    val accent = cosmos.accent
    GlassCard(modifier = modifier) {
        Column {
            Text("Oura Ring", style = MaterialTheme.typography.titleMedium, color = accent)
            Spacer(Modifier.height(4.dp))
            Text(
                "Verbinde deinen Oura Ring für Readiness-Score, Schlafqualität, Aktivität, Resilienz und detaillierte Schlafphasen. " +
                    "Erstelle einen Personal Access Token unter cloud.ouraring.com/personal-access-tokens und füge ihn hier ein. " +
                    "Der Token wird verschlüsselt auf dem Gerät gespeichert.",
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
                            if (hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            if (hidden) "Anzeigen" else "Verbergen",
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
                    OutlinedButton(onClick = vm::runSync, modifier = Modifier.weight(1f), enabled = !state.busy) {
                        if (state.busy) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = accent)
                            Spacer(Modifier.size(8.dp))
                        }
                        Text("Sync starten")
                    }
                    Button(
                        onClick = vm::disconnect,
                        modifier = Modifier.weight(1f),
                        enabled = !state.busy,
                        colors = ButtonDefaults.buttonColors(containerColor = cosmos.crit),
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
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = cosmos.farben.aufPrimaer),
                    ) {
                        if (state.busy) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = cosmos.farben.aufPrimaer)
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
                ConnectionLabel("Verbunden", cosmos.ok, Icons.Outlined.CheckCircle)
                if (state.lastSyncMs > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text("Letzter Sync: " + formatRelativeTime(state.lastSyncMs), style = MaterialTheme.typography.labelSmall, color = cosmos.textSecondary)
                }
            }
            state.message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary)
            }
        }
    }
}

@Composable
private fun HealthConnectApiCard(modifier: Modifier, vm: HealthConnectApiViewModel, state: HealthConnectApiUiState) {
    val cosmos = LocalCosmos.current
    val accent = cosmos.ok
    val launcher = rememberLauncherForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        vm.refresh()
        val needed = vm.requiredPermissions()
        vm.setMessage(
            if (granted.containsAll(needed)) "Alle Berechtigungen erteilt — Werte kommen beim nächsten Abruf."
            else "Nur ${granted.intersect(needed).size} von ${needed.size} Berechtigungen erteilt.",
        )
    }
    GlassCard(modifier = modifier) {
        Column {
            Text("Health Connect", style = MaterialTheme.typography.titleMedium, color = accent)
            Spacer(Modifier.height(4.dp))
            Text(
                "Liest Trainings und Körperwerte aus Health Connect. Die Zepp-App synchronisiert die Werte deiner Amazfit Smart " +
                    "Scale dort hin; dazu in Zepp unter Profil → 3rd Party Account Linking → Health Connect die Schreibrechte einschalten.",
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            if (!state.available) {
                Text("Health Connect ist auf diesem Gerät nicht verfügbar.", color = cosmos.crit, style = MaterialTheme.typography.bodySmall)
            } else {
                val statusText = if (state.allGranted) "Alle Berechtigungen erteilt" else "${state.grantedCount} von ${state.totalCount} Berechtigungen erteilt"
                Text(
                    statusText,
                    color = if (state.allGranted) accent else cosmos.warn,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { launcher.launch(vm.requiredPermissions()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                ) {
                    Text(if (state.allGranted) "Berechtigungen erneut bestätigen" else "Berechtigungen erteilen")
                }
            }
            state.message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary)
            }
        }
    }
}

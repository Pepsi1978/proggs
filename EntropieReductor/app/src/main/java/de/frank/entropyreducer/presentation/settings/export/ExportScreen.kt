package de.frank.entropyreducer.presentation.settings.export

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.remote.drive.SyncStatus
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.settings.ExportViewModel
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportScreen(onBack: () -> Unit, vm: ExportViewModel = hiltViewModel()) {
    val cosmos = LocalCosmos.current
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var confirmDeleteEntries by remember { mutableStateOf(false) }
    var confirmDeleteMemories by remember { mutableStateOf(false) }

    // Sign-In-Launcher (ActivityResultContract).
    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (val signInResult = vm.signInHelper.accountFromResult(result.data)) {
            is de.frank.entropyreducer.data.remote.drive.GoogleSignInHelper.SignInResult.Success ->
                vm.onSignInSuccess(signInResult.account)
            is de.frank.entropyreducer.data.remote.drive.GoogleSignInHelper.SignInResult.Error ->
                vm.onSignInError("Code ${signInResult.statusCode} (${signInResult.message})")
        }
    }

    LaunchedEffect(state.driveStatusMessage) {
        state.driveStatusMessage?.let {
            snackbar.showSnackbar(it)
            vm.clearStatusMessage()
        }
    }

    CosmosScaffold(
        title = "Datenexport / Datenschutz",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DriveBackupCard(state, vm, signInLauncher = { signInLauncher.launch(vm.signInHelper.signInIntent()) })

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Datenexport", style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { /* Stufe 4 — JSON-Export */ },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Alle Daten als JSON exportieren (Stufe 4)") }
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Löschen", style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { confirmDeleteEntries = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CosmosColors.Critical, contentColor = Color.White),
                        ) { Text("Alle Einträge löschen") }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { confirmDeleteMemories = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CosmosColors.Critical, contentColor = Color.White),
                        ) { Text("Alle Memory-Einträge löschen") }
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Alle Daten werden ausschliesslich lokal auf deinem Geraet gespeichert. Bei aktivem Drive-Backup wird zusaetzlich eine JSON-Kopie deiner Einträge im appDataFolder deines Google-Kontos abgelegt — für dich nicht im normalen Drive sichtbar, nur diese App kann sie lesen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cosmos.textSecondary,
                    )
                }
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            ) { Snackbar(it) }
        }
    }

    if (confirmDeleteEntries) {
        ConfirmDialog(
            title = "Alle Einträge löschen?",
            text = "Diese Aktion kann nicht rückgängig gemacht werden. Bei aktivem Drive-Backup wird auch der Cloud-Stand entsprechend angepasst.",
            onConfirm = { vm.deleteAllEntries(); confirmDeleteEntries = false },
            onCancel = { confirmDeleteEntries = false },
        )
    }
    if (confirmDeleteMemories) {
        ConfirmDialog(
            title = "Alle Memory-Einträge löschen?",
            text = "Diese Aktion kann nicht rückgängig gemacht werden.",
            onConfirm = { vm.deleteAllMemories(); confirmDeleteMemories = false },
            onCancel = { confirmDeleteMemories = false },
        )
    }
}

@Composable
private fun DriveBackupCard(
    state: de.frank.entropyreducer.presentation.settings.ExportUiState,
    vm: ExportViewModel,
    signInLauncher: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when {
                    state.syncStatus is SyncStatus.Failed -> Icons.Outlined.CloudOff
                    state.syncStatus is SyncStatus.Synced -> Icons.Outlined.CloudDone
                    else -> Icons.Outlined.CloudSync
                }
                val tint = when {
                    state.syncStatus is SyncStatus.Failed -> CosmosColors.Critical
                    state.syncStatus is SyncStatus.Synced -> CosmosColors.Success
                    state.syncStatus is SyncStatus.Running -> CosmosColors.AccentSecondary
                    else -> CosmosColors.AccentPrimary
                }
                Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Google Drive Backup", style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary)
                    Spacer(Modifier.size(2.dp))
                    val email = state.driveAccountEmail
                    Text(
                        text = email ?: "Noch kein Google-Konto verbunden",
                        style = MaterialTheme.typography.bodySmall,
                        color = cosmos.textSecondary,
                    )
                }
                if (state.syncStatus is SyncStatus.Running || state.restoreInProgress) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = CosmosColors.AccentPrimary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Status-Zeile
            val statusLabel = when (val s = state.syncStatus) {
                SyncStatus.Idle -> if (state.lastBackupAtMs > 0) "Letzter Upload: ${formatTimestamp(state.lastBackupAtMs)}" else "Backup ist eingerichtet"
                SyncStatus.Pending -> "Änderung erfasst — Upload startet gleich"
                SyncStatus.Running -> "Backup laeuft …"
                is SyncStatus.Synced -> "Synchronisiert um ${formatTimestamp(s.atEpochMs)}"
                is SyncStatus.Failed -> "Letzter Upload fehlgeschlagen: ${s.reason}"
            }
            Text(statusLabel, style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary)
            Spacer(Modifier.height(12.dp))

            if (state.driveAccountEmail == null) {
                Button(
                    onClick = signInLauncher,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmosColors.AccentPrimary,
                        contentColor = Color.Black,
                    ),
                ) { Text("Mit Google verbinden") }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Auto-Backup bei jeder Änderung",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cosmos.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.driveBackupEnabled,
                        onCheckedChange = vm::toggleBackupEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = CosmosColors.AccentPrimary),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { vm.backupNow() },
                        modifier = Modifier.weight(1f),
                        enabled = state.driveBackupEnabled && state.syncStatus !is SyncStatus.Running,
                    ) { Text("Jetzt sichern") }
                    OutlinedButton(
                        onClick = { vm.restoreNow() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.restoreInProgress,
                    ) { Text("Wiederherstellen") }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { vm.signOut() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Vom Konto trennen", color = CosmosColors.Critical) }
            }
        }
    }
}

private fun formatTimestamp(ms: Long): String {
    if (ms == 0L) return "—"
    val format = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMAN)
    return format.format(Date(ms))
}

@Composable
private fun ConfirmDialog(title: String, text: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CosmosColors.Critical, contentColor = Color.White),
            ) { Text("Löschen") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Abbrechen") } },
        title = { Text(title) },
        text = { Text(text) },
    )
}

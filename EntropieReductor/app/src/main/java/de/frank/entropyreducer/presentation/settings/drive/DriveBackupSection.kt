package de.frank.entropyreducer.presentation.settings.drive

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.remote.drive.GoogleSignInHelper
import de.frank.entropyreducer.data.remote.drive.SyncStatus
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.settings.ExportViewModel
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Self-contained Drive-Backup-Bereich (Frank-Wunsch 2026-05-10): Login,
 * Auto-Backup-Toggle, Manuelles Sichern + Wiederherstellen, Konto-Trennen.
 *
 * Frueher lag das in der Datenexport-Sektion — gehoert aber thematisch zu allen
 * Login/API-relevanten Bereichen, daher jetzt ganz oben im API-Schluessel-Screen.
 *
 * Nutzt den bestehenden [ExportViewModel] (Hilt liefert pro Screen einen eigenen
 * Scope, das ist OK — der Drive-Backup-State wird ohnehin aus
 * EncryptedSecretsStore + SyncCoordinator gelesen, also Screen-uebergreifend
 * konsistent).
 *
 * Status-Meldungen (Sign-In erfolgreich, Restore fertig, Konto getrennt) erscheinen
 * als kleines inline-Banner unter der Card und verschwinden nach 4 Sekunden
 * automatisch.
 */
@Composable
fun DriveBackupSection(
    modifier: Modifier = Modifier,
    vm: ExportViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (val signInResult = vm.signInHelper.accountFromResult(result.data)) {
            is GoogleSignInHelper.SignInResult.Success ->
                vm.onSignInSuccess(signInResult.account)
            is GoogleSignInHelper.SignInResult.Error ->
                vm.onSignInError("Code ${signInResult.statusCode} (${signInResult.message})")
        }
    }

    // Status-Meldung nach 4 Sekunden automatisch ausblenden — sonst bleibt sie
    // sichtbar bis der User auf einen anderen Screen wechselt.
    LaunchedEffect(state.driveStatusMessage) {
        if (state.driveStatusMessage != null) {
            delay(4000)
            vm.clearStatusMessage()
        }
    }

    Column(modifier = modifier) {
        DriveBackupCard(
            state = state,
            vm = vm,
            signInLauncher = { signInLauncher.launch(vm.signInHelper.signInIntent()) },
        )
        AnimatedVisibility(visible = state.driveStatusMessage != null) {
            val msg = state.driveStatusMessage
            if (msg != null) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    StatusBanner(message = msg, onDismiss = vm::clearStatusMessage)
                }
            }
        }
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
                    state.syncStatus is SyncStatus.Failed -> LocalCosmos.current.crit
                    state.syncStatus is SyncStatus.Synced -> LocalCosmos.current.ok
                    state.syncStatus is SyncStatus.Running -> LocalCosmos.current.accentForscher
                    else -> LocalCosmos.current.accent
                }
                Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Google Drive Backup",
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                    )
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
                        color = LocalCosmos.current.accent,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            val statusLabel = when (val s = state.syncStatus) {
                SyncStatus.Idle -> if (state.lastBackupAtMs > 0) {
                    "Letzter Upload: ${formatTimestamp(state.lastBackupAtMs)}"
                } else {
                    "Backup ist eingerichtet"
                }
                SyncStatus.Pending -> "Änderung erfasst — Upload startet gleich"
                SyncStatus.Running -> "Backup läuft …"
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
                        containerColor = LocalCosmos.current.accent,
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
                        colors = SwitchDefaults.colors(checkedThumbColor = LocalCosmos.current.accent),
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
                ) { Text("Vom Konto trennen", color = LocalCosmos.current.crit) }
            }
        }
    }
}

@Composable
private fun StatusBanner(message: String, onDismiss: () -> Unit) {
    val cosmos = LocalCosmos.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LocalCosmos.current.accent.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = cosmos.textPrimary,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) {
            Text("OK", color = LocalCosmos.current.accent, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// Performance-Audit Loop 9 (2026-05-10): ThreadLocal SimpleDateFormat.
private val DRIVE_BACKUP_FMT: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
    SimpleDateFormat("dd.MM. HH:mm", Locale.GERMAN)
}

private fun formatTimestamp(ms: Long): String {
    if (ms == 0L) return "—"
    return DRIVE_BACKUP_FMT.get()!!.format(Date(ms))
}

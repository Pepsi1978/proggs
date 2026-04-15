package com.nems.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nems.app.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    onNavigateToAuth: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Consent launcher: when Drive needs permission, launch the consent flow then retry sync
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        viewModel.syncNow()
    }
    LaunchedEffect(uiState.consentIntent) {
        uiState.consentIntent?.let { intent ->
            consentLauncher.launch(intent)
            viewModel.clearConsentIntent()
        }
    }

    // Logout confirmation dialog
    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showLogoutDialog(false) },
            title = {
                Text(
                    text = "Abmelden",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = "Moechtest du dich wirklich abmelden? Lokale Daten bleiben erhalten.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.signOut() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Abmelden")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showLogoutDialog(false) }) {
                    Text("Abbrechen")
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Screen title
        Text(
            text = "Einstellungen",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Konto ──────────────────────────────────────────────────────────────
        SettingsSectionCard(
            icon = Icons.Rounded.Person,
            iconTint = MaterialTheme.colorScheme.primary,
            title = "Konto",
        ) {
            if (uiState.userProfile != null) {
                val profile = uiState.userProfile!!

                // User info row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = profile.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.showLogoutDialog(true) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                            ),
                        ),
                    ) {
                        Text("Abmelden")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Last sync timestamp
                uiState.lastSyncTimestamp?.let { ts ->
                    val formatted = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN).format(Date(ts))
                    Text(
                        text = "Letzte Synchronisation: $formatted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Sync message (auto-dismiss via dismissSyncMessage)
                uiState.syncMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Action buttons: backup + restore
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { viewModel.syncNow() },
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (uiState.isSyncing) "Laeuft..." else "Sichern",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.restoreFromBackup() },
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = !uiState.isSyncing).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            ),
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Wiederherstellen",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            } else {
                // Not signed in
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nicht angemeldet",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Sicherung und Wiederherstellung mit Google Drive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sync message for sign-in errors
                uiState.syncMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = { viewModel.signIn(context) },
                        enabled = !uiState.isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (uiState.isSyncing) "Anmeldung..." else "Mit Google anmelden",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        // ── Design ─────────────────────────────────────────────────────────────
        SettingsSectionCard(
            icon = Icons.Rounded.Palette,
            iconTint = MaterialTheme.colorScheme.tertiary,
            title = "Design",
        ) {
            Column(modifier = Modifier.selectableGroup()) {
                ThemeModeOption(
                    label = "Dunkelmodus",
                    selected = uiState.themeMode == ThemeMode.DARK,
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                )
                ThemeModeOption(
                    label = "Hellmodus",
                    selected = uiState.themeMode == ThemeMode.LIGHT,
                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                )
                ThemeModeOption(
                    label = "System",
                    selected = uiState.themeMode == ThemeMode.SYSTEM,
                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                )
            }
        }

        // ── App-Info ───────────────────────────────────────────────────────────
        SettingsSectionCard(
            icon = Icons.Rounded.Info,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            title = "App-Info",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppInfoRow(label = "App", value = "NEMS")
                AppInfoRow(label = "Version", value = "1.0.0")
                AppInfoRow(label = "Beschreibung", value = "Nahrungsergaenzungsmittel-Tracker")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Private helpers ────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = iconTint,
            )
        }
        content()
    }
}

@Composable
private fun ThemeModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun AppInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

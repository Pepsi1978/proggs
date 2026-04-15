package com.nems.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nems.app.ui.theme.GlassBorder
import com.nems.app.ui.theme.GlassWhite
import com.nems.app.ui.theme.NeonCyan
import com.nems.app.ui.theme.NeonPurple
import com.nems.app.ui.theme.StatusRed
import com.nems.app.ui.theme.TextSecondary
import com.nems.app.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    onNavigateToAuth: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsState()

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

        Spacer(modifier = Modifier.height(8.dp))

        // Google Account section
        SettingsSectionCard(
            icon = Icons.Default.AccountCircle,
            iconTint = NeonCyan,
            title = "Google-Konto",
        ) {
            Text(
                text = "Synchronisiere deine Daten mit Google Drive und greife von mehreren Geraeten darauf zu.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onNavigateToAuth,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF0D0D1A),
                    ),
                ) {
                    Text(
                        text = "Mit Google anmelden",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Design section
        SettingsSectionCard(
            icon = Icons.Default.Palette,
            iconTint = NeonPurple,
            title = "Design",
        ) {
            Column(modifier = Modifier.selectableGroup()) {
                ThemeModeOption(
                    label = "Dunkel",
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                )
                ThemeModeOption(
                    label = "Hell",
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                )
                ThemeModeOption(
                    label = "System",
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                )
            }
        }

        // Data section
        SettingsSectionCard(
            icon = Icons.Default.Storage,
            iconTint = NeonCyan,
            title = "Daten",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: Export CSV */ },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeonCyan,
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(NeonCyan.copy(alpha = 0.5f)),
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp),
                        )
                        Text("Als CSV exportieren")
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: Delete all data with confirmation dialog */ },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StatusRed,
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(StatusRed.copy(alpha = 0.5f)),
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp),
                        )
                        Text("Alle Daten loeschen")
                    }
                }
            }
        }

        // App info section
        SettingsSectionCard(
            icon = Icons.Default.Info,
            iconTint = TextSecondary,
            title = "App-Info",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Version",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Text(
                    text = "1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Paket",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Text(
                    text = "com.nems.app",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

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
            .background(GlassWhite)
            .border(
                width = 1.dp,
                color = GlassBorder,
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
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 0.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = iconTint,
                modifier = Modifier.padding(start = 8.dp),
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
                selectedColor = NeonCyan,
                unselectedColor = TextSecondary,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else TextSecondary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

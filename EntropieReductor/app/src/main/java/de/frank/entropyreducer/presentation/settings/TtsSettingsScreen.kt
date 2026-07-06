package de.frank.entropyreducer.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.mental.GewohnheitTtsViewModel
import de.frank.entropyreducer.presentation.mental.MentalTtsViewModel
import de.frank.entropyreducer.presentation.theme.LocalCosmos

@Composable
fun TtsSettingsScreen(
    onBack: () -> Unit,
    mentalTtsViewModel: MentalTtsViewModel = hiltViewModel(),
    gewohnheitTtsViewModel: GewohnheitTtsViewModel = hiltViewModel(),
) {
    val cosmos = LocalCosmos.current
    val mentalState by mentalTtsViewModel.uiState.collectAsStateWithLifecycle()
    val gewohnheitState by gewohnheitTtsViewModel.uiState.collectAsStateWithLifecycle()

    CosmosScaffold(
        title = "Vorlesen",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
        showBottomBar = false,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(if (cosmos.isDark) Color(0xFF12100D) else Color(0xFFFAF7F3))
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Pausen zwischen vorgelesenen Sätzen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textSecondary,
                )
            }
            item {
                PauseSettingCard(
                    icon = Icons.Outlined.Psychology,
                    accent = cosmos.accentTasksSub,
                    title = "Mental",
                    subtitle = "Pause zwischen zwei Mental-Sätzen beim Vorlesen.",
                    valueSeconds = mentalState.pauseSeconds,
                    onValueSelected = mentalTtsViewModel::setPauseSeconds,
                )
            }
            item {
                PauseSettingCard(
                    icon = Icons.Outlined.Flag,
                    accent = cosmos.accentTasksSub,
                    title = "Gewohnheit",
                    subtitle = "Pause zwischen zwei Gewohnheits-Sätzen beim Vorlesen.",
                    valueSeconds = gewohnheitState.pauseSeconds,
                    onValueSelected = gewohnheitTtsViewModel::setPauseSeconds,
                )
            }
        }
    }
}

@Composable
private fun PauseSettingCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    valueSeconds: Int,
    onValueSelected: (Int) -> Unit,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary)
                Spacer(Modifier.size(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary)
            }
            PauseDropdown(
                valueSeconds = valueSeconds,
                accent = accent,
                onValueSelected = onValueSelected,
            )
        }
    }
}

@Composable
private fun PauseDropdown(
    valueSeconds: Int,
    accent: Color,
    onValueSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.12f))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Outlined.VolumeUp, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(4.dp))
            Text(
                text = "${valueSeconds}s",
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            Icon(Icons.Outlined.ArrowDropDown, null, tint = accent, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                text = "Pause auswählen",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
            (1..30).forEach { seconds ->
                DropdownMenuItem(
                    text = { Text("$seconds Sekunde${if (seconds == 1) "" else "n"}") },
                    onClick = {
                        onValueSelected(seconds)
                        expanded = false
                    },
                    trailingIcon = {
                        if (seconds == valueSeconds) {
                            Icon(Icons.Outlined.Check, null, tint = accent)
                        }
                    },
                )
            }
        }
    }
}

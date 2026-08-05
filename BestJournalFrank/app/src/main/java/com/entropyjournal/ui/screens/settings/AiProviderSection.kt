package com.entropyjournal.ui.screens.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entropyjournal.data.remote.codex.CodexModel
import com.entropyjournal.data.remote.codex.ReasoningEffort
import com.entropyjournal.util.Constants

/**
 * Die Auswahl, welche KI die gesamte Auswertung uebernimmt.
 *
 * Links Gemini mit API-Schluessel, rechts ChatGPT/Codex mit Anmeldung ueber den Browser. Der
 * gewaehlte Anbieter gilt fuer Dashboard, Rueckblicke, Textverbesserung, Zusammenfassungen und
 * Symbolerkennung gleichermassen.
 */
@Composable
fun AiProviderSection(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    val context = LocalContext.current
    val usesCodex = uiState.aiProvider == Constants.AI_PROVIDER_CODEX

    Column {
        Text(
            "KI-Modell",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Für Textverbesserung, Dashboard-Analyse und Rückblicke",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProviderChip(
                label = "Gemini",
                selected = !usesCodex,
                modifier = Modifier.weight(1f),
            ) { viewModel.updateAiProvider(Constants.AI_PROVIDER_GEMINI) }
            ProviderChip(
                label = "ChatGPT Codex",
                selected = usesCodex,
                modifier = Modifier.weight(1f),
            ) { viewModel.updateAiProvider(Constants.AI_PROVIDER_CODEX) }
        }
        Spacer(modifier = Modifier.height(14.dp))

        if (usesCodex) {
            CodexSettings(viewModel = viewModel, uiState = uiState, context = context)
        } else {
            GeminiModelDropdown(viewModel = viewModel, uiState = uiState)
        }
    }
}

@Composable
private fun ProviderChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            (if (selected) "◉ " else "○ ") + label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GeminiModelDropdown(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    var expanded by remember { mutableStateOf(false) }
    val selected = Constants.GEMINI_FLASH_MODELS.find { it.id == uiState.selectedModel }
        ?: Constants.GEMINI_FLASH_MODELS.first()

    DropdownField(
        value = "${selected.displayName}   ${selected.price}",
        expanded = expanded,
        onToggle = { expanded = !expanded },
    )
    if (expanded) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Constants.GEMINI_FLASH_MODELS.forEach { model ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateSelectedModel(model.id)
                            expanded = false
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        model.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (model.id == uiState.selectedModel)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Input ${model.price.substringBefore("/")} • " +
                            "Output ${model.price.substringAfter("/")}  pro 1M Token",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CodexSettings(
    viewModel: SettingsViewModel,
    uiState: SettingsUiState,
    context: android.content.Context,
) {
    var modelExpanded by remember { mutableStateOf(false) }
    var effortExpanded by remember { mutableStateOf(false) }
    val selectedModel = CodexModel.fromId(uiState.codexModel)
    val selectedEffort = ReasoningEffort.fromValue(uiState.codexEffort)

    // Anmeldung
    if (uiState.codexConnected) {
        Text(
            "Angemeldet" + (uiState.codexEmail?.let { " als $it" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { viewModel.logoutFromCodex() }) { Text("Abmelden") }
    } else if (uiState.codexLoggingIn) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                if (uiState.codexUserCode == null) "Gerätecode wird geholt …"
                else "Warte auf die Bestätigung im Browser …",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        uiState.codexUserCode?.let { code ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Diesen Code im Browser eingeben:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    code,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(onClick = { viewModel.cancelCodexLogin() }) { Text("Abbrechen") }
    } else {
        Text(
            "Melde dich mit deinem ChatGPT-Konto an. Die App zeigt einen Code und öffnet die " +
                "Bestätigungsseite im Browser — dort den Code eingeben, fertig.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                (context as? ComponentActivity)?.let { viewModel.loginToCodex(it) }
            },
        ) { Text("Bei ChatGPT anmelden") }
    }

    uiState.codexMessage?.let { note ->
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        "Modell",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(6.dp))
    DropdownField(
        value = selectedModel.label,
        expanded = modelExpanded,
        onToggle = { modelExpanded = !modelExpanded },
    )
    if (modelExpanded) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            CodexModel.entries.forEach { model ->
                Text(
                    model.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (model == selectedModel) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateCodexModel(model.apiId)
                            modelExpanded = false
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    Text(
        "Denk-Aufwand (Thinking)",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(6.dp))
    DropdownField(
        value = selectedEffort.label,
        expanded = effortExpanded,
        onToggle = { effortExpanded = !effortExpanded },
    )
    if (effortExpanded) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            ReasoningEffort.entries.forEach { effort ->
                Text(
                    effort.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (effort == selectedEffort) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateCodexEffort(effort.apiValue)
                            effortExpanded = false
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun DropdownField(value: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

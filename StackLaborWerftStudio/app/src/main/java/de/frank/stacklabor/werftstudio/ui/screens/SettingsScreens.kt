package de.frank.stacklabor.werftstudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.stacklabor.werftstudio.ui.components.GlassHeader
import de.frank.stacklabor.werftstudio.ui.components.PrimaryAction
import de.frank.stacklabor.werftstudio.ui.components.WerftScreen
import de.frank.stacklabor.werftstudio.ui.model.Appearance
import de.frank.stacklabor.werftstudio.ui.model.CodexLoginState
import de.frank.stacklabor.werftstudio.ui.model.StackLaborCallbacks
import de.frank.stacklabor.werftstudio.ui.model.StackLaborEvent
import de.frank.stacklabor.werftstudio.ui.model.StackLaborUiState
import de.frank.stacklabor.werftstudio.ui.navigation.Origin
import de.frank.stacklabor.werftstudio.ui.navigation.StackLaborRoute
import de.frank.stacklabor.werftstudio.ui.theme.StackLaborTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: StackLaborUiState, callbacks: StackLaborCallbacks) {
    var seedWarning by rememberSaveable { mutableStateOf(false) }
    var selectionId by rememberSaveable { mutableStateOf<String?>(null) }
    WerftScreen {
        Column(Modifier.fillMaxSize()) {
            GlassHeader("Einstellungen", onBack = callbacks.onBack)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SettingsGroup("Vorlesen") {
                        SettingsItem("Anbieter", state.ttsProviderLabel) { selectionId = "tts-provider" }
                        SettingsDivider()
                        SettingsItem("Stimme", state.ttsVoiceLabel) { selectionId = "tts-voice" }
                        SettingsDivider()
                        SettingsItem("Tempo", state.ttsSpeedLabel) { selectionId = "tts-speed" }
                        SettingsDivider()
                        SettingsItem("Pause zwischen Absätzen", state.ttsPauseLabel) { selectionId = "tts-pause" }
                        SettingsDivider()
                        SettingsItem("Automatische Abschaltung", state.ttsTimeoutLabel) { selectionId = "tts-timeout" }
                        SettingsDivider()
                        SettingsItem("Verbrauch", state.ttsUsageLabel) { callbacks.onEvent(StackLaborEvent.SelectSetting("tts-usage")) }
                    }
                }
                item {
                    SettingsGroup("Codex") {
                        SettingsItem("Konto", state.codexAccountLabel) { callbacks.onNavigate(StackLaborRoute.CodexLogin(Origin.Settings)) }
                        SettingsDivider()
                        SettingsItem("Modell", state.codexModelLabel) { selectionId = "codex-model" }
                        SettingsDivider()
                        SettingsItem("Denkstufe", state.codexReasoningLabel) { selectionId = "codex-reasoning" }
                    }
                }
                item {
                    SettingsGroup("Daten") {
                        SettingsItem("Exportieren", "JSON") { callbacks.onEvent(StackLaborEvent.ExportData) }
                        SettingsDivider()
                        SettingsItem("Importieren", "JSON") { callbacks.onEvent(StackLaborEvent.ImportData) }
                        SettingsDivider()
                        SettingsItem("Startbestand einlesen", "72 Einträge") { seedWarning = true }
                        SettingsDivider()
                        SettingsItem("Letzte Sicherung wiederherstellen") { callbacks.onEvent(StackLaborEvent.RestoreBackup) }
                        DesignWarning("Startbestand ersetzt alle vorhandenen Stacks und kann nicht rückgängig gemacht werden.")
                    }
                }
                item {
                    SettingsGroup("Darstellung") {
                        SettingsItem(
                            "Hell/Dunkel",
                            trailing = {
                                Switch(
                                    checked = state.appearance == Appearance.Dark,
                                    onCheckedChange = { callbacks.onEvent(StackLaborEvent.ToggleAppearance) },
                                )
                            },
                            onClick = { callbacks.onEvent(StackLaborEvent.ToggleAppearance) },
                        )
                        SettingsDivider()
                        SettingsItem(
                            "Bewegung reduzieren",
                            trailing = {
                                Switch(
                                    checked = state.reducedMotion,
                                    onCheckedChange = { callbacks.onEvent(StackLaborEvent.SelectSetting("reduced-motion")) },
                                )
                            },
                            onClick = { callbacks.onEvent(StackLaborEvent.SelectSetting("reduced-motion")) },
                        )
                    }
                }
                item {
                    Text(
                        "StackLabor Werft Studio ${state.versionName}\nStand ${state.versionBumpedAt}",
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = StackLaborTheme.colors.textMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        if (seedWarning) {
            AlertDialog(
                onDismissRequest = { seedWarning = false },
                text = { Text("Der Startbestand überschreibt alle vorhandenen Stacks. Fortfahren?") },
                confirmButton = {
                    TextButton(onClick = { seedWarning = false; callbacks.onEvent(StackLaborEvent.ImportSeedData) }) { Text("Fortfahren") }
                },
                dismissButton = { TextButton(onClick = { seedWarning = false }) { Text("Abbrechen") } },
            )
        }
        settingSelection(state, selectionId)?.let { selection ->
            ModalBottomSheet(
                onDismissRequest = { selectionId = null },
                containerColor = StackLaborTheme.colors.surface,
            ) {
                Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
                    Text(selection.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = StackLaborTheme.colors.elevated,
                        border = BorderStroke(1.dp, StackLaborTheme.colors.border),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Aktuelle Auswahl", style = MaterialTheme.typography.labelMedium, color = StackLaborTheme.colors.textMuted)
                            Text(selection.value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(selection.hint, style = MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted)
                    Spacer(Modifier.height(16.dp))
                    PrimaryAction(
                        "Nächste Auswahl",
                        onClick = {
                            callbacks.onEvent(StackLaborEvent.SelectSetting(selection.id))
                            selectionId = null
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    )
                }
            }
        }
    }
}

private data class SettingSelection(val id: String, val title: String, val value: String, val hint: String)

private fun settingSelection(state: StackLaborUiState, id: String?): SettingSelection? = when (id) {
    "tts-provider" -> SettingSelection(id, "TTS-Anbieter", state.ttsProviderLabel, "Wechselt zwischen den eingerichteten Sprachdiensten.")
    "tts-voice" -> SettingSelection(id, "Stimme", state.ttsVoiceLabel, "Zeigt die nächste Stimme des aktiven Anbieters an.")
    "tts-speed" -> SettingSelection(id, "Sprechtempo", state.ttsSpeedLabel, "Schaltet das Tempo in 0,1-Schritten weiter.")
    "tts-pause" -> SettingSelection(id, "Absatzpause", state.ttsPauseLabel, "Wechselt zwischen kurzer, mittlerer und langer Pause.")
    "tts-timeout" -> SettingSelection(id, "Automatische Abschaltung", state.ttsTimeoutLabel, "Wechselt zwischen 15, 30 und 60 Minuten.")
    "codex-model" -> SettingSelection(id, "Codex-Modell", state.codexModelLabel, "Wechselt zum nächsten verfügbaren Codex-Modell.")
    "codex-reasoning" -> SettingSelection(id, "Denkstufe", state.codexReasoningLabel, "Wechselt zur nächsten Denkstufe.")
    else -> null
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = StackLaborTheme.colors.textMuted,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = StackLaborTheme.colors.surface,
            border = BorderStroke(1.dp, StackLaborTheme.colors.border),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItem(
    label: String,
    value: String = "",
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(value, style = MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted, maxLines = 1)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = StackLaborTheme.colors.border.copy(alpha = 0.55f))
}

@Composable
private fun DesignWarning(text: String) {
    Row(
        Modifier.fillMaxWidth().background(StackLaborTheme.colors.yellow.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(StackLaborTheme.colors.yellow, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.yellowText)
    }
}

@Composable
fun CodexLoginScreen(state: StackLaborUiState, callbacks: StackLaborCallbacks) {
    WerftScreen(goldDark = true) {
        Column(Modifier.fillMaxSize()) {
            GlassHeader("Codex-Anmeldung", onBack = callbacks.onBack)
            Column(
                Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    Modifier.widthIn(max = 388.dp).fillMaxWidth().height(96.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = StackLaborTheme.colors.surface,
                    border = BorderStroke(1.dp, StackLaborTheme.colors.accent.copy(alpha = 0.7f)),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("GERÄTECODE", style = MaterialTheme.typography.labelSmall, color = StackLaborTheme.colors.textMuted)
                        Text(
                            state.codexDeviceCode.ifBlank { "…" },
                            fontSize = 32.sp,
                            lineHeight = 38.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StackLaborTheme.colors.textStrong,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Öffne diese Seite und gib den Code ein:", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Text(state.codexVerificationUrl, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.accent, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                PrimaryAction(
                    "Seite öffnen",
                    { callbacks.onEvent(StackLaborEvent.OpenCodexPage) },
                    Modifier.widthIn(max = 388.dp).fillMaxWidth().height(52.dp),
                )
                Spacer(Modifier.height(16.dp))
                LoginStatus(state.codexLoginState, callbacks)
            }
        }
    }
}

@Composable
private fun LoginStatus(status: CodexLoginState, callbacks: StackLaborCallbacks) {
    val (title, detail) = when (status) {
        CodexLoginState.Waiting -> "Warte auf Anmeldung …" to "Die App prüft die Bestätigung automatisch."
        CodexLoginState.Success -> "Anmeldung bestätigt" to "Codex ist jetzt für Auswertungen bereit."
        CodexLoginState.Expired -> "Code abgelaufen" to "Fordere einen neuen Geräte-Code an."
        CodexLoginState.Denied -> "Anmeldung verweigert" to "Der Vorgang wurde im Browser abgelehnt."
        CodexLoginState.NetworkError -> "Keine Verbindung" to "Die Anmeldung konnte nicht geprüft werden."
    }
    Row(
        Modifier.widthIn(max = 388.dp).fillMaxWidth().height(40.dp)
            .background(StackLaborTheme.colors.elevated, RoundedCornerShape(10.dp)).padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(if (status == CodexLoginState.Success) StackLaborTheme.colors.green else StackLaborTheme.colors.accent, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = StackLaborTheme.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (status == CodexLoginState.Expired || status == CodexLoginState.NetworkError) {
            TextButton(onClick = { callbacks.onEvent(StackLaborEvent.RetryCodexLogin) }, modifier = Modifier.height(40.dp)) { Text("Erneut") }
        } else {
            Spacer(Modifier.width(10.dp))
        }
    }
}

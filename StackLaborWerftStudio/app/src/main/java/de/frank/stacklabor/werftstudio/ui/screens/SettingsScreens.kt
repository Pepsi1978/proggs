package de.frank.stacklabor.werftstudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val colors = StackLaborTheme.colors
    WerftScreen {
        Column(Modifier.fillMaxSize()) {
            GlassHeader("Einstellungen", onBack = callbacks.onBack, framedBack = true)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    SettingsGroup("Vorlesen", Icons.AutoMirrored.Filled.VolumeUp) {
                        SettingsItem("Anbieter", state.ttsProviderLabel, selected = true) { selectionId = "tts-provider" }
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
                    SettingsGroup("Codex", Icons.Default.AutoAwesome) {
                        SettingsItem("Konto", state.codexAccountLabel) { callbacks.onNavigate(StackLaborRoute.CodexLogin(Origin.Settings)) }
                        SettingsDivider()
                        SettingsItem("Modell", state.codexModelLabel) { selectionId = "codex-model" }
                        SettingsDivider()
                        SettingsItem("Denkstufe", state.codexReasoningLabel) { selectionId = "codex-reasoning" }
                    }
                }
                item {
                    SettingsGroup("Daten", Icons.Default.Storage) {
                        SettingsItem("Exportieren", "JSON") { callbacks.onEvent(StackLaborEvent.ExportData) }
                        SettingsDivider()
                        SettingsItem("Importieren", "JSON") { callbacks.onEvent(StackLaborEvent.ImportData) }
                        SettingsDivider()
                        SettingsItem("Startbestand einlesen", "72 Einträge", iconTint = StackLaborTheme.colors.red) { seedWarning = true }
                        SettingsDivider()
                        SettingsItem("Letzte Sicherung wiederherstellen") { callbacks.onEvent(StackLaborEvent.RestoreBackup) }
                    }
                }
                item {
                    SettingsGroup("Darstellung", Icons.Default.LightMode) {
                        SettingsItem(
                            "Hell/Dunkel",
                            trailing = {
                                SettingsToggle(state.appearance == Appearance.Dark)
                            },
                            onClick = { callbacks.onEvent(StackLaborEvent.ToggleAppearance) },
                        )
                        SettingsDivider()
                        SettingsItem(
                            "Bewegung reduzieren",
                            trailing = {
                                SettingsToggle(state.reducedMotion)
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
            Dialog(onDismissRequest = { seedWarning = false }) {
                Surface(
                    Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(18.dp)).drawBehind {
                        drawRect(colors.red, size = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx()))
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = StackLaborTheme.colors.surface,
                    border = BorderStroke(1.dp, StackLaborTheme.colors.border),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Der Startbestand überschreibt alle vorhandenen Stacks. Fortfahren?", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { seedWarning = false }, modifier = Modifier.weight(1f)) { Text("Abbrechen") }
                            PrimaryAction(
                                "Fortfahren",
                                { seedWarning = false; callbacks.onEvent(StackLaborEvent.ImportSeedData) },
                                Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        settingSelection(state, selectionId)?.let { selection ->
            Dialog(
                onDismissRequest = { selectionId = null },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.BottomCenter) {
                    Surface(
                        Modifier.fillMaxWidth().heightIn(max = 640.dp).shadow(18.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = StackLaborTheme.colors.surface.copy(alpha = 0.96f),
                        border = BorderStroke(1.dp, StackLaborTheme.colors.border),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text(selection.title, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(12.dp))
                            selection.options.forEach { option ->
                                val selected = option == selection.value
                                Row(
                                    Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) StackLaborTheme.colors.accent.copy(alpha = 0.1f) else StackLaborTheme.colors.surface)
                                        .border(1.dp, if (selected) StackLaborTheme.colors.accent else StackLaborTheme.colors.border, RoundedCornerShape(12.dp))
                                        .clickable {
                                            callbacks.onEvent(StackLaborEvent.SelectSettingValue(selection.id, option))
                                            selectionId = null
                                        }.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        option,
                                        Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        color = if (selected) StackLaborTheme.colors.accent else StackLaborTheme.colors.textStrong,
                                    )
                                    if (selected) Icon(Icons.Default.Check, null, Modifier.size(20.dp), tint = StackLaborTheme.colors.accent)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            PrimaryAction(
                                "Fertig",
                                onClick = { selectionId = null },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class SettingSelection(val id: String, val title: String, val value: String, val options: List<String>)

private fun settingSelection(state: StackLaborUiState, id: String?): SettingSelection? = when (id) {
    "tts-provider" -> SettingSelection(id, "Anbieter", state.ttsProviderLabel, listOf("Microsoft Edge", "Google Cloud", "Meine Stimme"))
    "tts-voice" -> SettingSelection(
        id,
        "Stimme",
        state.ttsVoiceLabel,
        when (state.ttsProviderLabel) {
            "Google Cloud" -> listOf("Kore", "Charon")
            "Meine Stimme" -> listOf("Meine Stimme")
            else -> listOf("Seraphina", "Katja", "Conrad")
        },
    )
    "tts-speed" -> SettingSelection(id, "Tempo", state.ttsSpeedLabel, listOf("0,8×", "1,0×", "1,2×"))
    "tts-pause" -> SettingSelection(id, "Pause zwischen Absätzen", state.ttsPauseLabel, listOf("Kurz", "Mittel", "Lang"))
    "tts-timeout" -> SettingSelection(id, "Automatische Abschaltung", state.ttsTimeoutLabel, listOf("15 Min.", "30 Min.", "60 Min."))
    "codex-model" -> SettingSelection(id, "Modell", state.codexModelLabel, listOf("Sol", "Terra", "Luna"))
    "codex-reasoning" -> SettingSelection(id, "Denkstufe", state.codexReasoningLabel, listOf("Niedrig", "Mittel", "Hoch", "Sehr hoch", "Maximal"))
    else -> null
}

private val LocalSettingsIcon = staticCompositionLocalOf<ImageVector> { Icons.AutoMirrored.Filled.VolumeUp }

@Composable
private fun SettingsGroup(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val colors = StackLaborTheme.colors
    Column(
        Modifier.fillMaxWidth().shadow(16.dp, shape, ambientColor = androidx.compose.ui.graphics.Color(0x295A3508), spotColor = androidx.compose.ui.graphics.Color(0x245A3508))
            .clip(shape).background(StackLaborTheme.colors.surface).border(1.dp, StackLaborTheme.colors.border, shape),
    ) {
        Row(
            Modifier.fillMaxWidth().height(44.dp).background(colors.elevated).drawBehind {
                drawLine(colors.border, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(4.dp).height(16.dp).background(StackLaborTheme.colors.accent, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
        }
        CompositionLocalProvider(LocalSettingsIcon provides icon) {
            Column(Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
private fun SettingsItem(
    label: String,
    value: String = "",
    trailing: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    iconTint: Color = StackLaborTheme.colors.accent,
    onClick: () -> Unit,
) {
    val colors = StackLaborTheme.colors
    Row(
        Modifier.fillMaxWidth().height(64.dp)
            .shadow(16.dp, RoundedCornerShape(12.dp), ambientColor = androidx.compose.ui.graphics.Color(0x295A3508), spotColor = androidx.compose.ui.graphics.Color(0x245A3508))
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) Modifier.background(StackLaborTheme.colors.accent.copy(alpha = 0.08f))
                else Modifier.background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(StackLaborTheme.colors.elevated.copy(alpha = 0.48f), StackLaborTheme.colors.surface))),
            )
            .border(1.dp, if (selected) StackLaborTheme.colors.accent else StackLaborTheme.colors.border, RoundedCornerShape(12.dp))
            .drawBehind {
                if (selected) drawRect(colors.accent, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
            }.clickable(onClick = onClick).padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(48.dp).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            Box(
                Modifier.size(36.dp).background(StackLaborTheme.colors.accent.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LocalSettingsIcon.current, null, Modifier.size(22.dp), tint = iconTint)
            }
        }
        Text(
            label,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = if (selected) StackLaborTheme.colors.accent else StackLaborTheme.colors.textStrong,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(value, Modifier.widthIn(max = 148.dp), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = if (selected) StackLaborTheme.colors.accent else StackLaborTheme.colors.textMuted, maxLines = 1)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(24.dp), tint = StackLaborTheme.colors.textMuted)
        }
    }
}

@Composable
private fun SettingsDivider() {
    Spacer(Modifier.height(0.dp))
}

@Composable
private fun SettingsToggle(checked: Boolean) {
    val x by androidx.compose.animation.core.animateDpAsState(
        if (checked) 22.dp else 2.dp,
        androidx.compose.animation.core.tween(if (StackLaborTheme.motionEnabled) 180 else 0),
        label = "settingsToggle",
    )
    Box(
        Modifier.width(44.dp).height(24.dp).clip(RoundedCornerShape(12.dp))
            .background(if (checked) StackLaborTheme.colors.accent else StackLaborTheme.colors.disabled),
    ) {
        Box(
            Modifier.offset { IntOffset(x.roundToPx(), 2.dp.roundToPx()) }.size(20.dp)
                .background(StackLaborTheme.colors.surface, RoundedCornerShape(10.dp)),
        )
    }
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
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    Modifier.widthIn(max = 388.dp).fillMaxWidth().height(96.dp)
                        .shadow(16.dp, RoundedCornerShape(12.dp), ambientColor = androidx.compose.ui.graphics.Color(0x295A3508), spotColor = androidx.compose.ui.graphics.Color(0x245A3508)),
                    shape = RoundedCornerShape(12.dp),
                    color = StackLaborTheme.colors.surface,
                    border = BorderStroke(1.dp, StackLaborTheme.colors.accent.copy(alpha = 0.4f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            state.codexDeviceCode.ifBlank { "…" },
                            fontSize = 40.sp,
                            lineHeight = 48.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StackLaborTheme.colors.textStrong,
                            maxLines = 1,
                        )
                    }
                }
                Column(Modifier.widthIn(max = 388.dp).fillMaxWidth().height(44.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Öffne diese Seite und gib den Code ein:", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Text(state.codexVerificationUrl, style = MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.accent, textAlign = TextAlign.Center)
                }
                PrimaryAction(
                    "Seite öffnen",
                    { callbacks.onEvent(StackLaborEvent.OpenCodexPage) },
                    Modifier.widthIn(max = 388.dp).fillMaxWidth().height(52.dp),
                )
                LoginStatus(state.codexLoginState, callbacks)
            }
        }
    }
}

@Composable
private fun LoginStatus(status: CodexLoginState, callbacks: StackLaborCallbacks) {
    val title = when (status) {
        CodexLoginState.Waiting -> "Warte auf Anmeldung …" to "Die App prüft die Bestätigung automatisch."
        CodexLoginState.Success -> "Anmeldung bestätigt" to "Codex ist jetzt für Auswertungen bereit."
        CodexLoginState.Expired -> "Code abgelaufen" to "Fordere einen neuen Geräte-Code an."
        CodexLoginState.Denied -> "Anmeldung verweigert" to "Der Vorgang wurde im Browser abgelehnt."
        CodexLoginState.NetworkError -> "Keine Verbindung" to "Die Anmeldung konnte nicht geprüft werden."
    }.first
    Row(
        Modifier.widthIn(max = 388.dp).fillMaxWidth().height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(8.dp).background(if (status == CodexLoginState.Success) StackLaborTheme.colors.green else StackLaborTheme.colors.accent, RoundedCornerShape(4.dp)))
        }
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        if (status == CodexLoginState.Expired || status == CodexLoginState.NetworkError) {
            TextButton(onClick = { callbacks.onEvent(StackLaborEvent.RetryCodexLogin) }, modifier = Modifier.height(40.dp)) { Text("Erneut") }
        }
    }
}

package de.frank.stacklabor.werftstudio.ui.screens

import de.frank.stacklabor.werftstudio.service.codex.CodexModel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import de.frank.stacklabor.werftstudio.service.speech.GroqTranscriber
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import de.frank.stacklabor.werftstudio.SPEED_STEPS
import de.frank.stacklabor.werftstudio.data.preferences.TtsAnbieter
import de.frank.stacklabor.werftstudio.label
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
import de.frank.stacklabor.werftstudio.ui.components.RaisedPanel
import de.frank.stacklabor.werftstudio.ui.theme.bevel
import de.frank.stacklabor.werftstudio.ui.theme.darkenBy
import de.frank.stacklabor.werftstudio.ui.theme.depthShadow
import de.frank.stacklabor.werftstudio.ui.theme.lightenBy
import de.frank.stacklabor.werftstudio.ui.theme.metalRim
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(state: StackLaborUiState, callbacks: StackLaborCallbacks) {
    var seedWarning by rememberSaveable { mutableStateOf(false) }
    var selectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var keyEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    var vocabularyOpen by rememberSaveable { mutableStateOf(false) }
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
                        if (state.ttsProviderIsGemini) {
                            SettingsItem("Welcher Schlüssel", state.geminiKeyChoiceLabel) { selectionId = "gemini-key-choice" }
                            SettingsDivider()
                            SettingsItem(
                                "Rückfall auf Chirp 3 HD",
                                if (state.geminiChirpFallback) "Eingeschaltet" else "Ausgeschaltet",
                                trailing = { SettingsToggle(state.geminiChirpFallback) },
                                onClick = { callbacks.onEvent(StackLaborEvent.SelectSetting("gemini-chirp-fallback")) },
                            )
                            Text(
                                "Scheitert der kostenlose Schlüssel (etwa Kontingent erschöpft oder ein HTTP-Fehler), liest StackLabor " +
                                    "mit derselben Stimme über Google Chirp 3 HD weiter statt mit dem bezahlten Schlüssel.",
                                Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = StackLaborTheme.colors.textMuted,
                            )
                            SettingsDivider()
                        }
                        if (state.ttsKeyHint.isNotEmpty()) {
                            Text(
                                state.ttsKeyHint,
                                Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = StackLaborTheme.colors.red,
                            )
                            SettingsDivider()
                        }
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
                    SettingsGroup("API-Schlüssel", Icons.Default.Key) {
                        SettingsItem("Alibaba (eigene Stimme)", state.qwenApiKeyLabel) { keyEntryId = "qwen-api-key" }
                        SettingsDivider()
                        SettingsItem("Alibaba-Stimmen-ID", state.qwenVoiceIdLabel) { keyEntryId = "qwen-voice-id" }
                        SettingsDivider()
                        SettingsItem("Google Chirp 3 HD", state.googleApiKeyLabel) { keyEntryId = "google-api-key" }
                        SettingsDivider()
                        SettingsItem("Gemini-Stimme: kostenlos", state.geminiFreeKeyLabel) { keyEntryId = "gemini-key-free" }
                        SettingsDivider()
                        SettingsItem("Gemini-Stimme: bezahlt", state.geminiPaidKeyLabel) { keyEntryId = "gemini-key-paid" }
                        SettingsDivider()
                        SettingsItem("Azure-Speech (KEY 1)", state.azureKeyLabel) { keyEntryId = "azure-key" }
                        SettingsDivider()
                        SettingsItem("Azure-Region", state.azureRegionValue) { keyEntryId = "azure-region" }
                        SettingsDivider()
                        SettingsItem("Groq Whisper", state.groqApiKeyLabel) { keyEntryId = "groq-api-key" }
                        SettingsDivider()
                        SettingsItem(
                            "Eigene Stimmen aus dem Konto laden",
                            if (state.clonedVoicesLoading) "Lädt …" else state.clonedVoicesHint,
                        ) { callbacks.onEvent(StackLaborEvent.LoadClonedVoices) }
                    }
                }
                item {
                    SettingsGroup("Spracheingabe", Icons.Default.Mic) {
                        SettingsItem("Whisper-Modell", state.whisperModelLabel) { selectionId = "whisper-model" }
                        SettingsDivider()
                        SettingsItem(
                            "Persönliches Wörterbuch",
                            if (state.vocabularyEnabled) "Eingeschaltet" else "Ausgeschaltet",
                            trailing = { SettingsToggle(state.vocabularyEnabled) },
                            onClick = { callbacks.onEvent(StackLaborEvent.SelectSetting("vocabulary-enabled")) },
                        )
                        SettingsDivider()
                        SettingsItem(
                            "Begriffe",
                            "${GroqTranscriber.begriffe(state.vocabularyText).size} Begriffe",
                        ) { vocabularyOpen = true }
                        SettingsDivider()
                        Text(
                            "Whisper übernimmt aus dem Wörterbuch die Schreibweise von Namen und Fachbegriffen. " +
                                "Large V3 ist genauer, muss bei Groq aber freigeschaltet sein — sonst nimmt die App automatisch Turbo.",
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = StackLaborTheme.colors.textMuted,
                        )
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
                    SettingsGroup("Sicherheit", Icons.Default.Fingerprint) {
                        SettingsItem(
                            "Fingerabdruck",
                            if (state.fingerprintEnabled) "Eingeschaltet" else "Ausgeschaltet",
                            trailing = { SettingsToggle(state.fingerprintEnabled) },
                            onClick = { callbacks.onEvent(StackLaborEvent.ToggleFingerprint) },
                        )
                        SettingsDivider()
                        Text(
                            "Ist der Fingerabdruck eingeschaltet, öffnet sich die App nur nach einer Prüfung — " +
                                "und auch das Schloss eines Stacks springt erst nach einer Prüfung um.",
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = StackLaborTheme.colors.textMuted,
                        )
                    }
                }
                item {
                    Text(
                        "StackLabor ${state.versionName}\nStand ${state.versionBumpedAt}",
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
                    Modifier.fillMaxWidth()
                        .depthShadow(RoundedCornerShape(18.dp), 28.dp, strength = 1.5f)
                        .drawBehind {
                            drawRect(colors.red, size = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx()))
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = StackLaborTheme.colors.surface,
                    border = BorderStroke(1.dp, metalRim(0.85f)),
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
                        Modifier.fillMaxWidth().heightIn(max = 640.dp)
                            .depthShadow(RoundedCornerShape(20.dp), 30.dp, strength = 1.5f),
                        shape = RoundedCornerShape(20.dp),
                        color = StackLaborTheme.colors.surface.copy(alpha = 0.97f),
                        border = BorderStroke(1.dp, metalRim(0.9f)),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text(selection.title, style = MaterialTheme.typography.titleLarge)
                            if (selection.hint.isNotEmpty()) {
                                Text(selection.hint, style = MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted)
                            }
                            if (selection.options.isEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Keine Stimme verfügbar. Bitte zuerst den Schlüssel hinterlegen und die Stimmen laden.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StackLaborTheme.colors.textMuted,
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            // Eine eigene Spalte, damit auch die 30 Google-Stimmen scrollen können.
                            LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                            items(selection.options) { option ->
                                val selected = option == selection.value
                                Row(
                                    Modifier.fillMaxWidth().height(52.dp)
                                        .depthShadow(RoundedCornerShape(16.dp), if (selected) 10.dp else 4.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (selected) {
                                                Brush.verticalGradient(
                                                    listOf(
                                                        StackLaborTheme.colors.accent.copy(alpha = 0.22f),
                                                        StackLaborTheme.colors.accent.copy(alpha = 0.06f),
                                                    ),
                                                )
                                            } else {
                                                Brush.verticalGradient(
                                                    listOf(StackLaborTheme.colors.surface.lightenBy(0.25f), StackLaborTheme.colors.surface),
                                                )
                                            },
                                        )
                                        .border(1.dp, if (selected) metalRim(1f) else metalRim(0.4f), RoundedCornerShape(16.dp))
                                        .combinedClickable(
                                            onClick = {
                                                callbacks.onEvent(StackLaborEvent.SelectSettingValue(selection.id, option))
                                                selectionId = null
                                            },
                                            onLongClick = if (selection.favorites != null) {
                                                { callbacks.onEvent(StackLaborEvent.SelectSettingValue("tts-voice-favorite", option)) }
                                            } else {
                                                null
                                            },
                                            onLongClickLabel = if (selection.favorites != null) "Favorit an/aus" else null,
                                        ).padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val favorite = selection.favorites?.contains(option) == true
                                    if (selection.favorites != null) {
                                        IconButton(
                                            onClick = { callbacks.onEvent(StackLaborEvent.SelectSettingValue("tts-voice-favorite", option)) },
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                            Icon(
                                                if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                                                if (favorite) "Favorit entfernen" else "Als Favorit markieren",
                                                Modifier.size(20.dp),
                                                tint = if (favorite) Color(0xFFFFC107) else StackLaborTheme.colors.textMuted,
                                            )
                                        }
                                    }
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
                            }
                            Spacer(Modifier.height(8.dp))
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
        if (vocabularyOpen) {
            VocabularyDialog(
                current = state.vocabularyText,
                onDismiss = { vocabularyOpen = false },
                onSave = { value ->
                    callbacks.onEvent(StackLaborEvent.SaveVocabulary(value))
                    vocabularyOpen = false
                },
            )
        }
        keyEntry(state, keyEntryId)?.let { entry ->
            ApiKeyDialog(
                entry = entry,
                onDismiss = { keyEntryId = null },
                onSave = { value ->
                    callbacks.onEvent(StackLaborEvent.SaveApiKey(entry.id, value))
                    keyEntryId = null
                },
            )
        }
    }
}

private data class ApiKeyEntry(
    val id: String,
    val title: String,
    val current: String,
    val hint: String,
    /** false = kein Geheimnis (etwa die Region): Feld von Anfang an sichtbar. */
    val secret: Boolean = true,
)

private fun keyEntry(state: StackLaborUiState, id: String?): ApiKeyEntry? = when (id) {
    "qwen-api-key" -> ApiKeyEntry(
        id,
        "Alibaba-Schlüssel",
        state.qwenApiKeyValue,
        "Der DashScope-Schlüssel des Model-Studio-Kontos, in dem deine eigene Stimme liegt. " +
            "Er bleibt nur auf diesem Gerät gespeichert.",
    )
    "qwen-voice-id" -> ApiKeyEntry(
        id,
        "Alibaba-Stimmen-ID",
        state.qwenVoiceIdValue,
        "Die Kennung deiner geklonten Stimme (beginnt mit qwen-tts-vc-). Alternativ unter " +
            "„Eigene Stimmen aus dem Konto laden“ auswählen.",
    )
    "google-api-key" -> ApiKeyEntry(
        id,
        "Google-Schlüssel",
        state.googleApiKeyValue,
        "Der Google-Cloud-Schlüssel mit freigeschalteter Text-to-Speech-API (Chirp 3 HD). " +
            "Er bleibt nur auf diesem Gerät gespeichert.",
    )
    "gemini-key-free" -> ApiKeyEntry(
        id,
        "Gemini-Stimme: Schlüssel kostenlos (Projekt ohne Abrechnung)",
        state.geminiFreeKeyValue,
        "Der Schlüssel eines AI-Studio-Projekts ohne Abrechnung für Gemini 3.8 Flash TTS und Flash-Lite TTS. " +
            "Er bleibt nur auf diesem Gerät gespeichert.",
    )
    "gemini-key-paid" -> ApiKeyEntry(
        id,
        "Gemini-Stimme: Schlüssel bezahlt (Projekt mit Abrechnung)",
        state.geminiPaidKeyValue,
        "Der Schlüssel eines AI-Studio-Projekts mit Abrechnung. Wird je nach Schlüsselwahl genutzt, wenn der kostenlose scheitert.",
    )
    "azure-key" -> ApiKeyEntry(
        id,
        "Azure-Speech-Schlüssel (KEY 1)",
        state.azureKeyValue,
        "KEY 1 deiner Azure-Speech-Ressource (Stufe F0 kostenlos oder S0; HD-Stimmen nur mit S0).",
    )
    "azure-region" -> ApiKeyEntry(
        id,
        "Azure-Region (z. B. westeurope)",
        state.azureRegionValue,
        "Die Region deiner Speech-Ressource. Der Endpunkt aus dem Azure-Portal geht auch.",
        secret = false,
    )
    "groq-api-key" -> ApiKeyEntry(
        id,
        "Groq-API-Schlüssel",
        state.groqApiKeyValue,
        "Der Groq-Schlüssel für die deutsche Spracheingabe mit Whisper (Modell unter „Spracheingabe“ wählbar).",
    )
    else -> null
}

/** Eingabefeld für einen API-Schlüssel — verdeckt, mit Augen-Schalter zum Nachsehen. */
@Composable
private fun ApiKeyDialog(entry: ApiKeyEntry, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by rememberSaveable(entry.id) { mutableStateOf(entry.current) }
    var visible by rememberSaveable(entry.id) { mutableStateOf(!entry.secret) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            Modifier.fillMaxWidth().depthShadow(RoundedCornerShape(18.dp), 28.dp, strength = 1.5f),
            shape = RoundedCornerShape(18.dp),
            color = StackLaborTheme.colors.surface,
            border = BorderStroke(1.dp, metalRim(0.9f)),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(entry.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(entry.hint, style = MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted)
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(StackLaborTheme.colors.background)
                        .border(1.dp, metalRim(0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = StackLaborTheme.colors.textStrong),
                        cursorBrush = SolidColor(StackLaborTheme.colors.accent),
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        decorationBox = { inner ->
                            Box {
                                if (value.isEmpty()) {
                                    Text(
                                        if (entry.secret) "Schlüssel einfügen" else "westeurope",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = StackLaborTheme.colors.textMuted,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            if (visible) "Schlüssel verbergen" else "Schlüssel anzeigen",
                            Modifier.size(22.dp),
                            tint = StackLaborTheme.colors.textMuted,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Abbrechen") }
                    PrimaryAction("Speichern", { onSave(value) }, Modifier.weight(1f))
                }
            }
        }
    }
}

/** Mehrzeiliges Feld für die Begriffe des persönlichen Wörterbuchs. */
@Composable
private fun VocabularyDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf(current) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            Modifier.fillMaxWidth().depthShadow(RoundedCornerShape(18.dp), 28.dp, strength = 1.5f),
            shape = RoundedCornerShape(18.dp),
            color = StackLaborTheme.colors.surface,
            border = BorderStroke(1.dp, metalRim(0.9f)),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Persönliches Wörterbuch", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Begriffe (durch Komma oder Zeilenumbruch getrennt)",
                    style = MaterialTheme.typography.bodySmall,
                    color = StackLaborTheme.colors.textMuted,
                )
                Spacer(Modifier.height(14.dp))
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 320.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(StackLaborTheme.colors.background)
                        .border(1.dp, metalRim(0.6f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = StackLaborTheme.colors.textStrong),
                    cursorBrush = SolidColor(StackLaborTheme.colors.accent),
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Abbrechen") }
                    PrimaryAction("Speichern", { onSave(value) }, Modifier.weight(1f))
                }
            }
        }
    }
}

private data class SettingSelection(
    val id: String,
    val title: String,
    val value: String,
    val options: List<String>,
    val hint: String = "",
    /** Nicht null = Stimmliste: Favoriten bekommen einen gelben Stern, langes Drücken schaltet sie um. */
    val favorites: Set<String>? = null,
)

private fun settingSelection(state: StackLaborUiState, id: String?): SettingSelection? = when (id) {
    "tts-provider" -> SettingSelection(id, "Anbieter", state.ttsProviderLabel, TtsAnbieter.entries.map { it.label() })
    "tts-voice" -> SettingSelection(
        id,
        "Stimme",
        state.ttsVoiceLabel,
        state.ttsVoiceOptions,
        hint = (if (state.ttsProviderLabel.startsWith("Meine Stimme")) state.clonedVoicesHint + "\n" else "") +
            "Langes Drücken oder Stern: Favorit an/aus — Favoriten stehen oben.",
        favorites = state.ttsFavoriteVoiceOptions,
    )
    "gemini-key-choice" -> SettingSelection(
        id,
        "Welcher Schlüssel",
        state.geminiKeyChoiceLabel,
        de.frank.stacklabor.werftstudio.service.tts.GeminiKeyChoice.CHOICES.map { it.second },
    )
    "tts-speed" -> SettingSelection(
        id,
        "Tempo",
        state.ttsSpeedLabel,
        SPEED_STEPS.map { String.format(java.util.Locale.GERMANY, "%.2f×", it) },
    )
    "tts-pause" -> SettingSelection(id, "Pause zwischen Absätzen", state.ttsPauseLabel, listOf("Kurz", "Mittel", "Lang"))
    "tts-timeout" -> SettingSelection(id, "Automatische Abschaltung", state.ttsTimeoutLabel, listOf("15 Min.", "30 Min.", "60 Min."))
    "codex-model" -> SettingSelection(id, "Modell", state.codexModelLabel, CodexModel.entries.map { it.label })
    "whisper-model" -> SettingSelection(
        id,
        "Whisper-Modell",
        state.whisperModelLabel,
        GroqTranscriber.MODELLE.map { it.second },
        hint = "Gilt ab der nächsten Aufnahme.",
    )
    "codex-reasoning" -> SettingSelection(id, "Denkstufe", state.codexReasoningLabel,
        (CodexModel.entries.firstOrNull { it.label == state.codexModelLabel } ?: CodexModel.TERRA)
            .reasoningEfforts.map { it.label })
    else -> null
}

private val LocalSettingsIcon = staticCompositionLocalOf<ImageVector> { Icons.AutoMirrored.Filled.VolumeUp }

@Composable
private fun SettingsGroup(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val colors = StackLaborTheme.colors
    Column(
        Modifier.fillMaxWidth().depthShadow(shape, 18.dp)
            .clip(shape).background(StackLaborTheme.colors.surface).border(1.dp, metalRim(0.8f), shape),
    ) {
        Row(
            Modifier.fillMaxWidth().height(44.dp)
                .background(Brush.verticalGradient(listOf(colors.elevated.lightenBy(0.12f), colors.elevated.darkenBy(0.05f))))
                .bevel(0.8f)
                .drawBehind {
                    drawLine(colors.accent.copy(alpha = 0.55f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.width(4.dp).height(16.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.accent.lightenBy(0.35f), colors.accent, colors.accent.darkenBy(0.25f)),
                        ),
                        RoundedCornerShape(2.dp),
                    ),
            )
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
    RaisedPanel(
        Modifier.fillMaxWidth().height(64.dp),
        rimAlpha = if (selected) 1f else 0.6f,
        background = if (selected) {
            Brush.verticalGradient(listOf(colors.accent.copy(alpha = 0.18f), colors.accent.copy(alpha = 0.05f)))
        } else null,
        onClick = onClick,
    ) {
    Row(
        Modifier.fillMaxSize()
            .drawBehind {
                if (selected) drawRect(colors.accent, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
            }.padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(48.dp).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            Box(
                Modifier.depthShadow(RoundedCornerShape(18.dp), 6.dp)
                    .size(36.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.accent.copy(alpha = 0.24f), colors.accent.copy(alpha = 0.08f)),
                        ),
                        RoundedCornerShape(18.dp),
                    )
                    .border(1.dp, metalRim(0.45f), RoundedCornerShape(18.dp)),
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
}

@Composable
private fun SettingsDivider() {
    Spacer(Modifier.height(0.dp))
}

@Composable
private fun SettingsToggle(checked: Boolean) {
    val colors = StackLaborTheme.colors
    val x by androidx.compose.animation.core.animateDpAsState(
        if (checked) 22.dp else 2.dp,
        androidx.compose.animation.core.tween(if (StackLaborTheme.motionEnabled) 180 else 0),
        label = "settingsToggle",
    )
    Box(
        // Sunken track with a raised knob riding in it.
        Modifier.width(44.dp).height(24.dp).clip(RoundedCornerShape(16.dp))
            .background(
                if (checked) {
                    Brush.verticalGradient(listOf(colors.accent.darkenBy(0.25f), colors.accent))
                } else {
                    Brush.verticalGradient(listOf(colors.disabled.darkenBy(0.18f), colors.disabled))
                },
            )
            .border(1.dp, Color.Black.copy(alpha = 0.22f), RoundedCornerShape(16.dp)),
    ) {
        Box(
            Modifier.offset { IntOffset(x.roundToPx(), 2.dp.roundToPx()) }
                .depthShadow(RoundedCornerShape(10.dp), 6.dp)
                .size(20.dp)
                .background(
                    Brush.verticalGradient(listOf(colors.surface.lightenBy(0.5f), colors.surface)),
                    RoundedCornerShape(10.dp),
                ),
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
                        .shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = androidx.compose.ui.graphics.Color(0x291E2A4A), spotColor = androidx.compose.ui.graphics.Color(0x241E2A4A)),
                    shape = RoundedCornerShape(16.dp),
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

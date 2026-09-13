@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.frank.wecker

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.genialeideen.BuildConfig
import de.frank.genialeideen.audio.VoiceSampleScript
import de.frank.genialeideen.auth.CodexModel
import de.frank.genialeideen.auth.ReasoningEffort
import de.frank.genialeideen.tts.*
import de.frank.genialeideen.ui.*
import de.frank.genialeideen.ui.theme.*

@Composable
fun SettingsPage(vm: WeckerViewModel, activity: ComponentActivity) {
    val revision by vm.settingsRevision.collectAsStateWithLifecycle()
    val settings = vm.settings
    val recording by vm.recording.collectAsStateWithLifecycle()
    val voices by vm.clonedVoices.collectAsStateWithLifecycle()
    val sample by vm.hasVoiceSample.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val code by vm.loginCode.collectAsStateWithLifecycle()
    var voiceName by rememberSaveable { mutableStateOf("") }
    var removeVoice by remember { mutableStateOf<ClonedVoice?>(null) }
    var copySettings by remember { mutableStateOf(false) }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var showVoices by rememberSaveable { mutableStateOf(false) }
    var provider by remember(revision) { mutableStateOf(settings.ttsProvider) }
    var selected by remember(revision, provider) { mutableStateOf(when (provider) {
        TtsProvider.GOOGLE_CLOUD.id -> settings.googleTtsVoice
        TtsProvider.QWEN.id -> settings.qwenStandardVoice
        TtsProvider.QWEN_CLONE.id -> settings.qwenTtsVoiceId
        else -> settings.edgeTtsVoice
    }) }
    var rate by remember(revision) { mutableFloatStateOf(settings.ttsSpeechRate) }
    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) vm.startRecording(true) else vm.message.value = "Für deine Stimmprobe wird die Mikrofonberechtigung benötigt."
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.settingsRevision.value++ }
    fun launch(action: String, packageUri: Boolean = false) {
        runCatching { activity.startActivity(Intent(action).apply { if (packageUri) data = Uri.parse("package:${activity.packageName}") }) }
            .onFailure { vm.message.value = "Diese Einstellungsseite ist auf dem Gerät nicht verfügbar. Öffne die Android-App-Einstellungen." }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Section("Zuverlässig wecken") {
            Text("Für das Wecken werden genaue Alarme und der Android-Weckkanal verwendet. Auch bei gesperrtem Bildschirm und ohne Internet.")
            readiness(activity).forEach { (name, ready) -> Text("${if (ready) "✓" else "○"} $name", color = if (ready) Semantisch.erfolg else Semantisch.warnung) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StillerKnopf("Genaue Alarme", { if (Build.VERSION.SDK_INT >= 31) launch(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, true) })
                StillerKnopf("Benachrichtigungen", {
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                        notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else activity.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName))
                })
                StillerKnopf("Vollbild-Wecker", { if (Build.VERSION.SDK_INT >= 34) launch(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, true) })
                StillerKnopf("Nicht-stören-Zugriff", { launch(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS) })
                StillerKnopf("Wecker in Nicht stören", { launch("android.settings.ZEN_MODE_SETTINGS") })
                StillerKnopf("Akku-Einstellungen", { launch(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, true) })
            }
            Text("Wichtig: In Androids Nicht-stören-Modus müssen Wecker zugelassen sein. Eine App kann einen vom System vollständig gesperrten Weckkanal nicht zuverlässig übergehen. Erlaube Wecker in allen verwendeten Modi und Routinen.", style = MaterialTheme.typography.bodySmall)
            Text("Unter Akku die App bei Bedarf auf „Uneingeschränkt“ stellen. Nach einem erzwungenen Stopp muss sie wieder geöffnet werden. Ein ausgeschaltetes Telefon kann nicht wecken.", style = MaterialTheme.typography.bodySmall)
        }
        Section("Verbindung zu Geniale Ideen") {
            Text("Die offenen Ideen werden lokal übernommen. Beide Apps müssen mit demselben Schlüssel signiert sein.", style = MaterialTheme.typography.bodySmall)
            GoldKnopf("Spracheinstellungen übernehmen", { copySettings = true })
            Text("Übernimmt die dort gewählte Stimme, das Tempo sowie Google-, Alibaba- und Groq-Schlüssel. Danach kannst du die Stimme im Wecker unabhängig auswählen.", style = MaterialTheme.typography.bodySmall)
            StillerKnopf("Offene Ideen aktualisieren", vm::syncIdeas)
        }
        Section("Vorlesen · Stimmen & Tempo") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TtsProvider.entries.forEach { item ->
                    FilterChip(provider == item.id, {
                        settings.ttsProvider = item.id; provider = item.id; vm.settingsChanged()
                    }, { Text(item.label) })
                }
            }
            when (provider) {
                TtsProvider.GOOGLE_CLOUD.id -> Text("Google Cloud Text-to-Speech · Chirp 3 HD. Benötigt einen dafür freigeschalteten Google-Schlüssel; ein reiner Gemini-API-Schlüssel reicht nicht automatisch.", style = MaterialTheme.typography.bodySmall)
                TtsProvider.QWEN.id, TtsProvider.QWEN_CLONE.id -> Text("Alibaba Model Studio · DashScope International. Eigene Stimmen verwenden dasselbe Klonmodell wie Geniale Ideen.", style = MaterialTheme.typography.bodySmall)
                else -> Text("Microsoft Edge · kein eigener Schlüssel nötig. Für die Audio-Vorbereitung wird Internet benötigt.", style = MaterialTheme.typography.bodySmall)
            }
            val catalog = when (provider) {
                TtsProvider.GOOGLE_CLOUD.id -> TtsCatalog.googleVoices
                TtsProvider.QWEN.id -> TtsCatalog.qwenVoices
                TtsProvider.QWEN_CLONE.id -> emptyList()
                else -> TtsCatalog.edgeVoices
            }
            val available = if (provider == TtsProvider.QWEN_CLONE.id) voices.map {
                it.id to (settings.qwenVoiceNames[it.id] ?: it.name)
            } else catalog.map { it.id to "${it.name} · ${if (it.gender == VoiceGender.FEMALE) "weiblich" else "männlich"}" }
            GoldKnopf(available.find { it.first == selected }?.second ?: "Stimme auswählen", { showVoices = !showVoices }, Modifier.fillMaxWidth())
            if (showVoices) {
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Stimmen suchen") }, singleLine = true)
            Toggle("Nur Favoriten anzeigen", onlyFavorites) { onlyFavorites = it }
            available.filter { (!onlyFavorites || it.first in settings.favoriteTtsVoices) && it.second.contains(search, ignoreCase = true) }.forEach { (id, label) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected == id, {
                        selected = id
                        when (provider) {
                            TtsProvider.GOOGLE_CLOUD.id -> settings.googleTtsVoice = id
                            TtsProvider.QWEN.id -> settings.qwenStandardVoice = id
                            TtsProvider.QWEN_CLONE.id -> settings.qwenTtsVoiceId = id
                            else -> settings.edgeTtsVoice = id
                        }
                        vm.settingsChanged()
                        showVoices = false
                    })
                    Text(label, Modifier.weight(1f))
                    StillerKnopf(if (id in settings.favoriteTtsVoices) "★" else "☆", {
                        settings.favoriteTtsVoices = if (id in settings.favoriteTtsVoices) settings.favoriteTtsVoices - id else settings.favoriteTtsVoices + id
                        vm.settingsRevision.value++
                    })
                }
            }
            }
            if (provider == TtsProvider.QWEN_CLONE.id) GoldKnopf("Meine Stimmen laden", vm::loadVoices)
            Text("Sprechtempo: ${"%.2f".format(rate)}×")
            Slider(rate, { rate = it }, valueRange = .5f..2f, onValueChangeFinished = { settings.ttsSpeechRate = rate; vm.settingsChanged() })
            var german by remember(revision) { mutableStateOf(settings.immerDeutschVorlesen) }
            Toggle("Deutsche Aussprache beibehalten", german) { german = it; settings.immerDeutschVorlesen = it; vm.settingsChanged() }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldKnopf("Stimme anhören", vm::previewVoice, aktiviert = busy.isBlank())
                StillerKnopf("Stoppen", vm::stopPreview)
            }
            Text("Nach Änderungen werden aktive Wecker neu vorbereitet. Bereits fertiges Audio bleibt bis zum erfolgreichen Abschluss verfügbar.", style = MaterialTheme.typography.bodySmall)
        }
        Section("Sprachschlüssel", collapsible = true, summary = "Google · Alibaba · Groq") {
                SecretField("Google / Chirp-3-HD-Schlüssel", settings.googleTtsApiKey) { settings.googleTtsApiKey = it; vm.settingsChanged() }
                SecretField("Alibaba / DashScope-Schlüssel", settings.qwenTtsApiKey) { settings.qwenTtsApiKey = it; vm.settingsChanged() }
                SecretField("Groq-Schlüssel", settings.groqApiKey) { settings.groqApiKey = it; vm.settingsChanged() }
            Text("Die Schlüssel werden mit Android Keystore verschlüsselt gespeichert.", style = MaterialTheme.typography.bodySmall)
        }
        Section("Meine Stimme aufnehmen", collapsible = true, summary = "Eigene Stimmen erstellen und verwalten") {
            Text(VoiceSampleScript.script(VoiceSampleScript.fallback).joinToString("\n\n"), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(voiceName, { voiceName = it }, Modifier.fillMaxWidth(), label = { Text("Name deiner Stimme") }, singleLine = true)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldKnopf(if (recording) "■ Aufnahme beenden" else "● Stimmprobe aufnehmen", {
                    if (recording) vm.stopRecording()
                    else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.startRecording(true)
                    else microphone.launch(Manifest.permission.RECORD_AUDIO)
                }, aktiviert = busy.isBlank())
                GoldKnopf("Stimme erstellen", { vm.createVoice(voiceName) }, aktiviert = sample && !recording && busy.isBlank())
                StillerKnopf("Stimmen aktualisieren", vm::loadVoices)
            }
            voices.forEach { voice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(settings.qwenVoiceNames[voice.id] ?: voice.name, Modifier.weight(1f))
                    StillerKnopf("Löschen", { removeVoice = voice })
                }
            }
            Text("Beim Erstellen wird die Stimmprobe an dein Alibaba-Konto übertragen. Löschen entfernt die Stimme aus diesem Konto und betrifft auch ihre Verwendung in Geniale Ideen.", style = MaterialTheme.typography.bodySmall)
        }
        Section("Spracheingabe", collapsible = true, summary = "Groq · Whisper Large V3 Turbo") {
            Text("Groq · Whisper Large V3 Turbo")
            key(revision) {
                SettingToggle("Stille vorab herausfiltern", settings.filterStilleVorabAn) { settings.filterStilleVorabAn = it }
                SettingToggle("Segmentmetriken prüfen", settings.filterSegmentmetrikenAn) { settings.filterSegmentmetrikenAn = it }
                SettingToggle("Zeitstempel prüfen", settings.filterZeitstempelAn) { settings.filterZeitstempelAn = it }
                SettingToggle("Erfundene Floskeln herausfiltern", settings.filterFloskelnAn) { settings.filterFloskelnAn = it }
            }
            Text("Die Spracheingabe benötigt Internet. Der fertige Wecker benötigt es nicht.", style = MaterialTheme.typography.bodySmall)
        }
        Section("KI-Textverbesserung", collapsible = true, summary = if (vm.auth.isConnected) "ChatGPT verbunden" else "ChatGPT-Anmeldung einrichten") {
            Text(if (vm.auth.isConnected) "ChatGPT verbunden: ${vm.auth.email.orEmpty()}" else "ChatGPT noch nicht verbunden")
            if (vm.auth.isConnected) StillerKnopf("Abmelden", { vm.auth.logout(); vm.settingsRevision.value++ })
            else GoldKnopf("Mit ChatGPT anmelden", { vm.login(activity) }, aktiviert = busy.isBlank())
            Choice("Modell", settings.model, CodexModel.entries.map { it.apiId to it.label }) { settings.model = it; vm.settingsRevision.value++ }
            Choice("Denktiefe", settings.reasoning, CodexModel.fromLabel(settings.model).supportedEfforts.map { it.apiValue to it.label }) { settings.reasoning = it; vm.settingsRevision.value++ }
            code?.let { info ->
                Text("Anmeldecode: ${info.userCode}", fontFamily = IdeenSchriftFest)
                val clipboard = LocalClipboardManager.current
                GoldKnopf("Code kopieren und Anmeldung öffnen", {
                    clipboard.setText(AnnotatedString(info.userCode))
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.verificationUri)))
                })
            }
        }
        Section("Genialer Wecker") {
            Text("Version ${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_BUMPED_AT}")
            Text("Design, 3D-Knöpfe, Animationen und Sprachbausteine aus Geniale Ideen. Weckdienst, Offline-Vorbereitung und Foto-Aufgaben für diese App entwickelt.", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
    }
    if (copySettings) Confirm("Spracheinstellungen übernehmen?", "Die Sprachschlüssel, Stimme und das Tempo im Wecker werden durch die Werte aus Geniale Ideen ersetzt.", {
        copySettings = false; vm.importSettings()
    }, { copySettings = false })
    removeVoice?.let { voice -> Confirm("Stimme aus Alibaba löschen?", "Diese Stimme wird auch für andere Apps unbrauchbar. Vorbereitete Wecker-Audiodateien bleiben erhalten.", {
        removeVoice = null; vm.removeVoice(voice.id)
    }, { removeVoice = null }) }
}

@Composable
private fun SecretField(label: String, initial: String, save: (String) -> Unit) {
    var text by remember(label, initial) { mutableStateOf(initial) }
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StillerKnopf(if (visible) "Verbergen" else "Anzeigen", { visible = !visible })
        GoldKnopf("Speichern", { save(text.trim()) })
    }
}

@Composable
private fun SettingToggle(label: String, initial: Boolean, save: (Boolean) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    Toggle(label, value) { value = it; save(it) }
}

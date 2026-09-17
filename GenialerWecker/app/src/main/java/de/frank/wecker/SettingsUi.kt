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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    val voicesLoading by vm.voicesLoading.collectAsStateWithLifecycle()
    val voiceError by vm.voiceLoadError.collectAsStateWithLifecycle()
    val sample by vm.hasVoiceSample.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val code by vm.loginCode.collectAsStateWithLifecycle()
    val permissions = rememberReadiness()
    val ideas by vm.ideas.collectAsStateWithLifecycle()
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
    LaunchedEffect(Unit) { vm.loadVoices() }
    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) vm.startRecording(true) else vm.message.value = "Für deine Stimmprobe wird die Mikrofonberechtigung benötigt."
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.settingsRevision.value++ }
    fun launch(action: String, packageUri: Boolean = false, extraPackage: Boolean = false) {
        runCatching { activity.startActivity(Intent(action).apply {
            if (packageUri) data = Uri.parse("package:${activity.packageName}")
            if (extraPackage) putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        }) }
            .onFailure { vm.message.value = "Diese Einstellungsseite ist auf dem Gerät nicht verfügbar. Öffne die Android-App-Einstellungen." }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val missing = permissions.count { !it.second }
        // Every requirement sits next to the one button that fixes it; nothing to search for.
        val fix: Map<String, () -> Unit> = mapOf(
            "Genaue Weckzeiten" to { if (Build.VERSION.SDK_INT >= 31) launch(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, true) },
            "Benachrichtigungen" to {
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                else launch(Settings.ACTION_APP_NOTIFICATION_SETTINGS, extraPackage = true)
            },
            "Vollbild-Wecker" to { if (Build.VERSION.SDK_INT >= 34) launch(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, true) },
            "Wecker bei Nicht stören" to {
                if (!activity.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted) launch(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                else launch("android.settings.ZEN_MODE_SETTINGS")
            },
            "Akku uneingeschränkt" to {
                if (activity.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(activity.packageName))
                    launch(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, true)
                else launch(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, true)
            },
        )
        Section("Weckbereitschaft") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (missing == 0) Icons.Default.VerifiedUser else Icons.Default.NotificationsActive, null,
                    tint = if (missing == 0) Semantisch.erfolg else Semantisch.warnung)
                Text(if (missing == 0) "Alles bereit: Der Wecker klingelt auch gesperrt und ohne Internet."
                    else "$missing ${if (missing == 1) "Freigabe fehlt" else "Freigaben fehlen"}. Tippe jeweils auf „Erlauben“.",
                    Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyMedium)
            }
            permissions.forEach { (name, ready) ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (ready) "✓" else "○", color = if (ready) Semantisch.erfolg else Semantisch.warnung)
                    Text(name, Modifier.weight(1f).padding(start = 10.dp), color = if (ready) LocalGold.current.textPrimaer else Semantisch.warnung)
                    if (ready) Text("erteilt", style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
                    else StillerKnopf("Erlauben", { fix[name]?.invoke() }, Modifier.semantics { contentDescription = "$name erlauben" }, hervorgehoben = true)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StillerKnopf("Nicht-stören-Modi öffnen", { launch("android.settings.ZEN_MODE_SETTINGS") })
                StillerKnopf("App-Info öffnen", { launch(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, true) })
            }
            Text("Erlaube Wecker in allen verwendeten Nicht-stören-Modi und Routinen. Nach „Stopp erzwingen“ die App einmal öffnen. Ein ausgeschaltetes Telefon kann nicht wecken.", style = MaterialTheme.typography.bodySmall)
        }
        Section("Vorlesen · Stimmen & Tempo", collapsible = true, summary = "${when (provider) {
            TtsProvider.GOOGLE_CLOUD.id -> "Google"; TtsProvider.QWEN_CLONE.id -> "Meine Stimmen"; else -> "Edge"
        }} · Tempo ${"%.2f".format(rate)}× · gilt für alle Wecker ohne eigene Stimme") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(TtsProvider.QWEN_CLONE, TtsProvider.GOOGLE_CLOUD, TtsProvider.EDGE).forEach { item ->
                    FilterChip(provider == item.id, {
                        settings.ttsProvider = item.id; provider = item.id; vm.settingsChanged()
                    }, { Text(if (item == TtsProvider.QWEN_CLONE) "Meine Stimmen" else item.label) })
                }
            }
            when (provider) {
                TtsProvider.GOOGLE_CLOUD.id -> Text("Google Cloud Text-to-Speech · Chirp 3 HD. Benötigt einen dafür freigeschalteten Google-Schlüssel; ein reiner Gemini-API-Schlüssel reicht nicht automatisch.", style = MaterialTheme.typography.bodySmall)
                TtsProvider.QWEN.id, TtsProvider.QWEN_CLONE.id -> Text("Alibaba Model Studio · DashScope International. Eigene Stimmen verwenden dasselbe Klonmodell wie Geniale Ideen.", style = MaterialTheme.typography.bodySmall)
                else -> Text("Microsoft Edge · kein eigener Schlüssel nötig. Für die Audio-Vorbereitung wird Internet benötigt.", style = MaterialTheme.typography.bodySmall)
            }
            val catalog = when (provider) {
                TtsProvider.GOOGLE_CLOUD.id -> TtsCatalog.googleVoices
                TtsProvider.QWEN_CLONE.id -> emptyList()
                else -> TtsCatalog.edgeVoices
            }
            val available = if (provider == TtsProvider.QWEN_CLONE.id) voices.map {
                it.id to (settings.qwenVoiceNames[it.id] ?: it.name)
            } else catalog.map { it.id to "${it.name} · ${if (it.gender == VoiceGender.FEMALE) "weiblich" else "männlich"}" }
            GoldKnopf(available.find { it.first == selected }?.second ?: settings.qwenVoiceNames[selected] ?: "Stimme auswählen", { showVoices = !showVoices }, Modifier.fillMaxWidth())
            if (showVoices) {
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Stimmen suchen") }, singleLine = true)
            Toggle("Nur Favoriten anzeigen", onlyFavorites) { onlyFavorites = it }
            available.filter { (!onlyFavorites || it.first in settings.favoriteTtsVoices) && it.second.contains(search, ignoreCase = true) }.forEach { (id, label) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected == id, {
                        selected = id
                        when (provider) {
                            TtsProvider.GOOGLE_CLOUD.id -> settings.googleTtsVoice = id
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
            if (provider == TtsProvider.QWEN_CLONE.id) {
                if (voicesLoading) Text("Deine hochgeladenen Stimmen werden geladen …")
                if (voiceError.isNotBlank()) Text(voiceError, color = Semantisch.warnung)
                Text("${voices.size} eigene Stimmen", style = MaterialTheme.typography.bodySmall)
                GoldKnopf("Meine Stimmen aktualisieren", { vm.loadVoices(force = true) }, aktiviert = !voicesLoading)
            }
            Text("Sprechtempo: ${"%.2f".format(rate)}×")
            Slider(rate, { rate = it }, valueRange = .5f..2f, onValueChangeFinished = { settings.ttsSpeechRate = rate; vm.settingsChanged() })
            var german by remember(revision) { mutableStateOf(settings.immerDeutschVorlesen) }
            Toggle("Deutsche Aussprache beibehalten", german) { german = it; settings.immerDeutschVorlesen = it; vm.settingsChanged() }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldKnopf("Stimme anhören", vm::previewVoice, aktiviert = busy.isBlank())
                StillerKnopf("Stoppen", vm::stopPreview)
            }
            Text("Beim Speichern werden sechs Varianten derselben Stimme erzeugt, mit behutsamen Tempo-Unterschieden. Beim Wecken läuft Variante 1 bis 6, dann wieder 1. Absätze werden vorgeladen. Bereits fertiges Audio bleibt bis zum erfolgreichen Abschluss verfügbar.", style = MaterialTheme.typography.bodySmall)
        }
        val ideasAt by vm.ideasAt.collectAsStateWithLifecycle()
        Section("Geniale Ideen · Offene Ideen", collapsible = true,
            summary = "${ideas.size} offene ${if (ideas.size == 1) "Idee" else "Ideen"} · ${if (ideasAt > 0) "Stand ${formatAt(ideasAt)}" else "noch nicht abgeglichen"}") {
            // Opening the section syncs, like opening the former ideas page did.
            LaunchedEffect(Unit) { vm.syncIdeas() }
            Text("Nur offene Ideen, in derselben Reihenfolge. Die Originale bleiben in Geniale Ideen. Dein Wecker erhält eine lokale Lesekopie. Beide Apps müssen mit demselben Schlüssel signiert sein.", style = MaterialTheme.typography.bodySmall)
            GoldKnopf("Jetzt abgleichen", vm::syncIdeas)
            if (ideasAt > 0) Text("Stand: ${formatAt(ideasAt)}", style = MaterialTheme.typography.bodySmall)
            if (ideas.isEmpty()) Leerzustand("✧", "Noch keine offenen Ideen", "Öffne Geniale Ideen und lege dort eine offene Idee an. Beide Apps benötigen den aktuellen Stand.")
            ideas.forEachIndexed { index, idea ->
                if (index > 0) HorizontalDivider(color = LocalGold.current.textGedaempft.copy(alpha = .3f))
                Text(idea.title, style = MaterialTheme.typography.titleSmall, color = LocalGold.current.textPrimaer)
                Text(idea.text, style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider(color = LocalGold.current.primaer.copy(alpha = .4f))
            GoldKnopf("Spracheinstellungen übernehmen", { copySettings = true })
            Text("Übernimmt die dort gewählte Stimme, das Tempo sowie Google-, Alibaba- und Groq-Schlüssel. Danach kannst du die Stimme im Wecker unabhängig auswählen.", style = MaterialTheme.typography.bodySmall)
        }
        Section("Sprachschlüssel", collapsible = true, summary = listOf("Google" to settings.googleTtsApiKey, "Alibaba" to settings.qwenTtsApiKey, "Groq" to settings.groqApiKey)
            .joinToString(" · ") { (name, key) -> "$name ${if (key.isBlank()) "fehlt" else "✓"}" }) {
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
                }, aktiviert = recording || busy.isBlank())
                GoldKnopf("Stimme erstellen", { vm.createVoice(voiceName) }, aktiviert = sample && !recording && busy.isBlank())
                StillerKnopf("Stimmen aktualisieren", { vm.loadVoices(force = true) })
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

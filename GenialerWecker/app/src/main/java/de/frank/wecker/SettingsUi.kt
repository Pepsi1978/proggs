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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
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
import kotlin.math.roundToInt
import de.frank.genialeideen.BuildConfig
import de.frank.genialeideen.audio.VoiceSampleScript
import de.frank.genialeideen.auth.CodexModel
import de.frank.genialeideen.auth.ReasoningEffort
import de.frank.genialeideen.tts.*
import de.frank.genialeideen.ui.*
import de.frank.genialeideen.ui.theme.*
import de.frank.wecker.design.Design
import de.frank.wecker.design.LocalGestalt

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
    // Eine Ebene über der zuklappbaren Karte, damit bloßes Zuklappen die Eingabe nicht verwirft. Der Schlüssel
    // bleibt im Arbeitsspeicher dieser Seite – nichts davon geht in SavedState, auf die Platte, ins Log oder ins
    // ViewModel. Der Schlüsselwert selbst ist der Erinnerungsschlüssel: nur ein tatsächlich geänderter
    // gespeicherter Wert setzt genau seinen Entwurf zurück, die beiden anderen bleiben unberührt.
    var googleEntwurf by remember(settings.googleTtsApiKey) { mutableStateOf(settings.googleTtsApiKey) }
    var qwenEntwurf by remember(settings.qwenTtsApiKey) { mutableStateOf(settings.qwenTtsApiKey) }
    var groqEntwurf by remember(settings.groqApiKey) { mutableStateOf(settings.groqApiKey) }
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding()) {
      DesignBlatt {
       Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    tint = if (missing == 0) LocalSemantisch.current.erfolg else LocalSemantisch.current.warnung)
                Text(if (missing == 0) "Alles bereit: Der Wecker klingelt auch gesperrt und ohne Internet."
                    else "$missing ${if (missing == 1) "Freigabe fehlt" else "Freigaben fehlen"}. Tippe jeweils auf „Erlauben“.",
                    Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyMedium)
            }
            permissions.forEach { (name, ready) ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (ready) "✓" else "○", color = if (ready) LocalSemantisch.current.erfolg else LocalSemantisch.current.warnung)
                    Text(name, Modifier.weight(1f).padding(start = 10.dp), color = if (ready) LocalGold.current.textPrimaer else LocalSemantisch.current.warnung)
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
        BenachrichtigungenKarte(vm, activity)
        var ausrichtung by remember(revision) { mutableStateOf(settings.ausrichtung) }
        var design by remember(revision) { mutableStateOf(settings.design) }
        Section("Darstellung", collapsible = true, initiallyExpanded = false,
            summary = "${Design.von(design).anzeige} · Ausrichtung: ${Ausrichtung.optionen.find { it.first == ausrichtung }?.second ?: "Automatisch"}") {
            // Eigene Achse: das Design gilt unabhängig von Hell/Dunkel und von der Ausrichtung.
            Choice("Design", design, Design.entries.map { it.id to it.anzeige }) {
                design = it
                settings.design = it
                vm.settingsRevision.value++
            }
            Text(Design.von(design).beschreibung, style = MaterialTheme.typography.bodySmall)
            Text("Gilt für Weckerliste, Editor, Einstellungen und den Weckbildschirm. Hell/Dunkel und Ausrichtung wählst du weiterhin getrennt.",
                style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
            HorizontalDivider(color = LocalGold.current.rahmen)
            Choice("Ausrichtung", ausrichtung, Ausrichtung.optionen) {
                ausrichtung = it
                settings.ausrichtung = it
                Ausrichtung.anwenden(activity, it)
                vm.settingsRevision.value++
            }
            Text("Gilt für die Weckerliste und den Weckbildschirm. „Automatisch“ überlässt die Wahl wie bisher dem Gerät. " +
                "Android kann die Ausrichtung in geteilten Fenstern oder auf großen Displays vorgeben.",
                style = MaterialTheme.typography.bodySmall)
        }
        Section("Vorlesen · Stimmen & Tempo", collapsible = true, summary = "${when (provider) {
            TtsProvider.GOOGLE_CLOUD.id -> "Google"; TtsProvider.QWEN_CLONE.id -> "Meine Stimmen"; else -> "Edge"
        }} · Tempo ${"%.2f".format(rate)}× · gilt für alle Wecker ohne eigene Stimme") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(TtsProvider.QWEN_CLONE, TtsProvider.GOOGLE_CLOUD, TtsProvider.EDGE).forEach { item ->
                    Chip3D(provider == item.id, {
                        settings.ttsProvider = item.id; provider = item.id; vm.settingsChanged()
                    }, if (item == TtsProvider.QWEN_CLONE) "Meine Stimmen" else item.label)
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
            Eingabefeld(search, { search = it }, "Stimmen suchen", Modifier.fillMaxWidth())
            Toggle("Nur Favoriten anzeigen", onlyFavorites) { onlyFavorites = it }
            // Einmal gefiltert: dieselbe Liste entscheidet über Einträge und Leerzustand. Der normalisierte
            // Suchtext gilt für Filter, Meldung und Rücksetzknopf gleichermaßen – reine Leerzeichen filtern
            // dadurch gar nicht erst. Das Eingabefeld zeigt weiterhin die rohe Eingabe.
            val suchtext = search.trim()
            val gefiltert = available.filter { (!onlyFavorites || it.first in settings.favoriteTtsVoices) && it.second.contains(suchtext, ignoreCase = true) }
            // Eine von vornherein leere Liste (eigene Stimmen noch nicht geladen) ist kein Filterproblem.
            // Dafür sprechen weiter unten die Lade-, Fehler- und „0 eigene Stimmen“-Zeilen, hier bleibt es still.
            if (available.isNotEmpty() && gefiltert.isEmpty()) {
                val suchbegriff = suchtext.isNotEmpty()
                Text(when {
                    suchbegriff && onlyFavorites -> "Keine deiner Favoriten passt zu „$suchtext“."
                    suchbegriff -> "Keine Stimme passt zu „$suchtext“."
                    else -> "Für diesen Anbieter ist noch keine Stimme als Favorit markiert."
                }, style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Jeder Knopf führt garantiert zu einer gefüllten Liste: bei beiden Filtern wird auch beides gelöst.
                    when {
                        suchbegriff && onlyFavorites -> StillerKnopf("Filter zurücksetzen", { search = ""; onlyFavorites = false }, hervorgehoben = true)
                        suchbegriff -> StillerKnopf("Suche löschen", { search = "" }, hervorgehoben = true)
                        else -> StillerKnopf("Favoritenfilter ausschalten", { onlyFavorites = false }, hervorgehoben = true)
                    }
                }
            }
            gefiltert.forEach { (id, label) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Auswahlpunkt und Name sind eine gemeinsame Fläche von mindestens 48 dp; der Favoritenstern
                    // liegt bewusst daneben und wählt die Stimme deshalb nicht aus.
                    Row(Modifier.weight(1f).heightIn(min = 48.dp).selectable(selected == id, role = androidx.compose.ui.semantics.Role.RadioButton) {
                        selected = id
                        when (provider) {
                            TtsProvider.GOOGLE_CLOUD.id -> settings.googleTtsVoice = id
                            TtsProvider.QWEN_CLONE.id -> settings.qwenTtsVoiceId = id
                            else -> settings.edgeTtsVoice = id
                        }
                        vm.settingsChanged()
                        showVoices = false
                    }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected == id, null)
                        Text(label, Modifier.padding(start = 8.dp))
                    }
                    val favorit = id in settings.favoriteTtsVoices
                    StillerKnopf(if (favorit) "★" else "☆", {
                        settings.favoriteTtsVoices = if (favorit) settings.favoriteTtsVoices - id else settings.favoriteTtsVoices + id
                        vm.settingsRevision.value++
                    }, Modifier.semantics {
                        contentDescription = if (favorit) "$label aus den Favoriten entfernen" else "$label zu den Favoriten hinzufügen"
                    })
                }
            }
            }
            if (provider == TtsProvider.QWEN_CLONE.id) {
                if (voicesLoading) Text("Deine hochgeladenen Stimmen werden geladen …")
                if (voiceError.isNotBlank()) Text(voiceError, color = LocalSemantisch.current.warnung)
                Text("${voices.size} eigene Stimmen", style = MaterialTheme.typography.bodySmall)
                GoldKnopf("Meine Stimmen aktualisieren", { vm.loadVoices(force = true) }, aktiviert = !voicesLoading)
            }
            Text("Sprechtempo: ${"%.2f".format(rate)}×")
            Regler3D(rate, { rate = it }, bereich = .5f..2f,
                aufAenderungFertig = { settings.ttsSpeechRate = rate; vm.settingsChanged() })
            var german by remember(revision) { mutableStateOf(settings.immerDeutschVorlesen) }
            Toggle("Deutsche Aussprache beibehalten", german) { german = it; settings.immerDeutschVorlesen = it; vm.settingsChanged() }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Ohne Wecker: der globale Standard, genau wie bisher.
                GoldKnopf("Stimme anhören", { vm.previewVoice() }, aktiviert = busy.isBlank())
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
        // Die Zusammenfassung spricht ausschließlich über gespeicherte Schlüssel; ein Entwurf wird als offen
        // benannt und nie als vorhandener Schlüssel dargestellt.
        val offeneSchluessel = googleEntwurf.trim() != settings.googleTtsApiKey.trim() ||
            qwenEntwurf.trim() != settings.qwenTtsApiKey.trim() || groqEntwurf.trim() != settings.groqApiKey.trim()
        Section("Sprachschlüssel", collapsible = true, summary = listOf("Google" to settings.googleTtsApiKey, "Alibaba" to settings.qwenTtsApiKey, "Groq" to settings.groqApiKey)
            .joinToString(" · ") { (name, key) -> "$name ${if (key.isBlank()) "fehlt" else "✓"}" } +
            if (offeneSchluessel) " · ungespeicherte Änderung" else "") {
                SecretField("Google / Chirp-3-HD-Schlüssel", settings.googleTtsApiKey, googleEntwurf, { googleEntwurf = it }) { settings.googleTtsApiKey = it; vm.settingsChanged() }
                SecretField("Alibaba / DashScope-Schlüssel", settings.qwenTtsApiKey, qwenEntwurf, { qwenEntwurf = it }) { settings.qwenTtsApiKey = it; vm.settingsChanged() }
                SecretField("Groq-Schlüssel", settings.groqApiKey, groqEntwurf, { groqEntwurf = it }) { settings.groqApiKey = it; vm.settingsChanged() }
            Text("Die Schlüssel werden mit Android Keystore verschlüsselt gespeichert.", style = MaterialTheme.typography.bodySmall)
        }
        Section("Meine Stimme aufnehmen", collapsible = true, summary = "Eigene Stimmen erstellen und verwalten") {
            Text(VoiceSampleScript.script(VoiceSampleScript.fallback).joinToString("\n\n"), style = MaterialTheme.typography.bodySmall)
            Eingabefeld(voiceName, { voiceName = it }, "Name deiner Stimme", Modifier.fillMaxWidth())
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
      }
    }
    if (copySettings) Confirm("Spracheinstellungen übernehmen?", "Die Sprachschlüssel, Stimme und das Tempo im Wecker werden durch die Werte aus Geniale Ideen ersetzt.", "Übernehmen", {
        copySettings = false; vm.importSettings()
    }, { copySettings = false })
    removeVoice?.let { voice -> Confirm("Stimme aus Alibaba löschen?", "Diese Stimme wird auch für andere Apps unbrauchbar. Vorbereitete Wecker-Audiodateien bleiben erhalten.", "Stimme löschen", {
        removeVoice = null; vm.removeVoice(voice.id)
    }, { removeVoice = null }) }
}

/**
 * Zeigt den Entwurf, den [SettingsPage] hält, und legt ihn erst auf ausdrückliches Speichern ab –
 * nie im SavedState, nie im Log, kein automatisches Speichern bei Fokusverlust. Verglichen wird der
 * normalisierte Wert, weil auch getrimmt gespeichert wird.
 */
@Composable
private fun SecretField(label: String, gespeichert: String, text: String, onText: (String) -> Unit, save: (String) -> Unit) {
    // Bewusst nur hier gemerkt: beim Zuklappen der Karte ist der Schlüssel wieder maskiert.
    var visible by remember { mutableStateOf(false) }
    val tastatur = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val geaendert = text.trim() != gespeichert.trim()
    fun speichern() { save(text.trim()); tastatur?.hide() }
    Eingabefeld(text, onText, label, Modifier.fillMaxWidth(),
        sichtWandlung = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        tastaturOptionen = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
        tastaturAktionen = androidx.compose.foundation.text.KeyboardActions(onDone = { if (geaendert) speichern() else tastatur?.hide() }))
    // Umbrechend: auf schmaler Breite und mit großer Schrift bleiben beide Knöpfe sichtbar.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StillerKnopf(if (visible) "Verbergen" else "Anzeigen", { visible = !visible })
        // Nur eine echte Änderung lässt sich speichern; ein unverändertes (auch leeres) Feld bleibt still.
        GoldKnopf("Speichern", { speichern() }, aktiviert = geaendert)
    }
    // Eigene Zeile, damit der Hinweis auch bei großer Schrift vollständig lesbar bleibt.
    if (geaendert) Text("Noch nicht gespeichert", style = MaterialTheme.typography.bodySmall, color = LocalSemantisch.current.warnung)
}

@Composable
private fun SettingToggle(label: String, initial: Boolean, save: (Boolean) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    Toggle(label, value) { value = it; save(it) }
}

/** One switch per notification type; for now only the sleep reminder. No master switch. */
@Composable
private fun BenachrichtigungenKarte(vm: WeckerViewModel, activity: ComponentActivity) {
    val context = activity
    // Explicit refresh instead of polling: resume (e.g. back from system settings), permission result and switch.
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    val on = remember(refresh) { SchlafErinnerung.enabled(context) }
    val blocked = remember(refresh) { SchlafErinnerung.blockiert(context) }
    val inexact = remember(refresh) { SchlafErinnerung.exaktFehlt(context) }
    val problems = remember(refresh, alarms) { SchlafErinnerung.statusFehler(context) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        SchlafErinnerung.syncAll(context); refresh++
    }
    fun open(intent: Intent) {
        try { activity.startActivity(intent) }
        catch (e: Exception) { vm.message.value = "Die Android-Einstellung konnte nicht geöffnet werden (${e.javaClass.simpleName})." }
    }
    Section("Benachrichtigungen", collapsible = true, initiallyExpanded = false,
        summary = "Schlafenszeit-Erinnerung ${if (on) SchlafErinnerung.leadMinutes(context).let { if (it == 0) "zur Schlafenszeit" else "$it Min. vorher" } else "aus"}" +
            "${if (on && blocked != null) " · gesperrt" else ""}",
        error = if (on && blocked != null) "Die Erinnerung ist eingeschaltet, wird von Android aber nicht angezeigt." else null) {
        Toggle("Schlafenszeit-Erinnerung", on) { checked ->
            // The switch shows the stored state only; a failed write keeps the old state and says so.
            val stored = SchlafErinnerung.setEnabled(context, checked)
            if (!stored) vm.message.value = "Die Einstellung konnte nicht gespeichert werden. Der bisherige Stand gilt weiter."
            refresh++
            if (stored && checked && Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        Text("Vor der berechneten Schlafenszeit, nur für aktive Wecker mit Schlafdauer.", style = MaterialTheme.typography.bodySmall)
        if (on) {
            // Beim Ziehen nur die Anzeige; gespeichert und neu geplant wird erst am Ende der Geste.
            var vorlauf by remember(refresh) { mutableIntStateOf(SchlafErinnerung.leadMinutes(context)) }
            Text(if (vorlauf == 0) "Vorlauf: zur Schlafenszeit" else "Vorlauf: $vorlauf Min. vorher",
                style = MaterialTheme.typography.titleMedium, color = LocalGold.current.primaer)
            // Die Neuplanung hängt weiterhin ausschließlich am Ende der Geste, nicht an jeder
            // Bewegung — sonst würde bei jedem Pixel neu terminiert.
            Regler3D(vorlauf.toFloat(), { vorlauf = it.roundToInt() },
                Modifier.semantics {
                    contentDescription = "Vorlauf der Schlafenszeit-Erinnerung"
                    stateDescription = if (vorlauf == 0) "zur Schlafenszeit" else "$vorlauf Minuten vorher"
                },
                bereich = 0f..SchlafPlan.LEAD_MAX_MINUTES.toFloat(), stufen = SchlafPlan.LEAD_MAX_MINUTES - 1,
                aufAenderungFertig = {
                    if (!SchlafErinnerung.setLeadMinutes(context, vorlauf)) {
                        vm.message.value = "Der Vorlauf konnte nicht gespeichert werden. Der bisherige Stand gilt weiter."
                        vorlauf = SchlafErinnerung.leadMinutes(context)
                    }
                    refresh++
                })
            Text("0 Minuten erinnert genau zur Schlafenszeit. Ausgeschaltet wird die Erinnerung allein über den Schalter.",
                style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
            when (blocked) {
                "app" -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Benachrichtigungen der App sind ausgeschaltet.", Modifier.weight(1f), color = LocalSemantisch.current.warnung, style = MaterialTheme.typography.bodySmall)
                    StillerKnopf("Erlauben", {
                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName))
                    }, hervorgehoben = true)
                }
                "kanal" -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Der Kanal „Schlafenszeit-Erinnerung“ ist gesperrt.", Modifier.weight(1f), color = LocalSemantisch.current.warnung, style = MaterialTheme.typography.bodySmall)
                    StillerKnopf("Kanal öffnen", {
                        open(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, SchlafErinnerung.CHANNEL))
                    }, hervorgehoben = true)
                }
                else -> if (alarms.none { it.enabled && it.sleepMinutes > 0 })
                    Text("Noch kein aktiver Wecker mit Schlafdauer.", style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
            }
            if (inexact) Text("Ohne Freigabe für genaue Zeiten kann die Erinnerung einige Minuten verspätet kommen.",
                style = MaterialTheme.typography.bodySmall, color = LocalSemantisch.current.warnung)
        }
        problems.forEach { Text(it, color = LocalSemantisch.current.warnung, style = MaterialTheme.typography.bodySmall) }
    }
}

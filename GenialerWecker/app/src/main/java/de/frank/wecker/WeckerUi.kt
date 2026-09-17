@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.frank.wecker

import android.Manifest
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import de.frank.genialeideen.BuildConfig
import de.frank.genialeideen.audio.VoiceSampleScript
import de.frank.genialeideen.auth.CodexModel
import de.frank.genialeideen.auth.ReasoningEffort
import de.frank.genialeideen.tts.*
import de.frank.genialeideen.ui.*
import de.frank.genialeideen.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun WeckerApp(vm: WeckerViewModel, activity: ComponentActivity) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val audioBusy by vm.audioBusy.collectAsStateWithLifecycle()
    val recording by vm.recording.collectAsStateWithLifecycle()
    val view = LocalView.current
    SideEffect {
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = theme != "dark"
            isAppearanceLightNavigationBars = theme != "dark"
        }
    }
    var page by rememberSaveable { mutableStateOf(if (draft != null) "edit" else "alarms") }
    var delete by remember { mutableStateOf<Alarm?>(null) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) vm.save { page = "alarms" } else vm.message.value = "Erlaube Benachrichtigungen, damit der Wecker auf dem Sperrbildschirm bedienbar ist."
    }
    GenialeIdeenTheme(theme) {
        val gold = LocalGold.current
        BackHandler(page != "alarms") { vm.stopPreview(); page = "alarms" }
        Box(Modifier.fillMaxSize().background(gold.hintergrund)) {
            BewegterHintergrund()
            Column(Modifier.fillMaxSize().imePadding()) {
                IdeenKopfleiste(
                    titel = when (page) { "edit" -> if (vm.isNewDraft) "Neuer Wecker" else "Wecker bearbeiten"; "settings" -> "Einstellungen"; "ideas" -> "Offene Ideen"; else -> "Genialer Wecker" },
                    themeWahl = theme,
                    aufThemeTipp = { vm.settings.theme = if (theme == "dark") "light" else "dark" },
                    aufEinstellungen = if (page == "settings") null else ({ page = "settings" }),
                    voran = if (page != "alarms") ({ StillerKnopf("‹", { vm.stopPreview(); page = "alarms" }); Spacer(Modifier.width(8.dp)) }) else null,
                )
                if (busy.isNotBlank() || audioBusy.isNotBlank()) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(busy.ifBlank { audioBusy }, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.bodySmall)
                    StillerKnopf("Abbrechen", vm::cancelAction)
                }
                if (message.isNotBlank()) GoldKarte(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        StillerKnopf("OK", { vm.message.value = "" })
                    }
                }
                AnimatedContent(page, Modifier.weight(1f), label = "Bildschirmwechsel") { current ->
                    when (current) {
                        "edit" -> draft?.let { alarm -> Column(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f)) { key(alarm.id) { AlarmEditor(vm, alarm, activity) } }
                            Box(Modifier.fillMaxWidth().background(gold.flaeche.copy(alpha = .85f)).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                                GoldKnopf("Wecker speichern", {
                                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else vm.save { page = "alarms" }
                                }, Modifier.fillMaxWidth(), aktiviert = busy.isBlank() && !recording, hauptKnopf = true)
                            }
                        } } ?: Box(Modifier.fillMaxSize())
                        "settings" -> SettingsPage(vm, activity)
                        "ideas" -> IdeasPage(vm)
                        else -> AlarmList(alarms, vm,
                            onNew = { vm.newAlarm(); page = "edit" },
                            onEdit = { vm.edit(it); page = "edit" }, onDelete = { delete = it },
                            onSettings = { page = "settings" }, onIdeas = { page = "ideas" })
                    }
                }
            }
        }
        delete?.let { alarm -> Confirm("Wecker löschen?", "„${alarm.name}“ wird entfernt.", {
            vm.delete(alarm); delete = null
        }, { delete = null }) }
    }
}

@Composable
private fun AlarmList(alarms: List<Alarm>, vm: WeckerViewModel, onNew: () -> Unit, onEdit: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit, onSettings: () -> Unit, onIdeas: () -> Unit) {
    val gold = LocalGold.current
    // Nur für diese Listenansicht merken: neue Wecker und eine neu geöffnete Liste sind kompakt.
    var expandedIds by remember { mutableStateOf(emptySet<String>()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val next = alarms.flatMap { alarm -> listOfNotNull(alarm.nextAt.takeIf { alarm.enabled && it > now }, alarm.snoozeUntil.takeIf { it > now }) }.minOrNull()
    BoxWithConstraints {
        LazyVerticalGrid(columns = GridCells.Fixed(if (maxWidth >= 680.dp) 2 else 1),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GoldKarte(erhoeht = true) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DEIN TAG BEGINNT MIT EINER IDEE", style = MaterialTheme.typography.labelSmall, color = gold.textGedaempft, letterSpacing = 2.sp)
                        Text(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")),
                            fontFamily = IdeenSchriftBetont, fontSize = 72.sp, color = gold.primaer)
                        Text(if (next == null) "Zeit für einen guten Morgen." else "Nächster Wecker: ${formatAt(next)}", color = gold.textGedaempft)
                        // Always reserve the line so toggling a switch never changes the card height.
                        Text(if (next != null) "In ${remaining(next - now)}" else " ", style = MaterialTheme.typography.bodySmall, color = gold.primaer)
                        Spacer(Modifier.height(18.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GoldKnopf("＋ Wecker", onNew, hauptKnopf = true)
                            StillerKnopf("Offene Ideen", onIdeas)
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { ReadinessCard(onSettings, hideWhenReady = true) }
            if (alarms.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Leerzustand("☀", "Ein Morgen nach deinen Wünschen", "Musik, Gedanken und Erinnerungen – in deiner Reihenfolge. Lege deinen ersten Wecker an.")
            }
            items(alarms, key = { it.id }) { alarm ->
                val expanded = alarm.id in expandedIds
                val toggleDetails = {
                    expandedIds = if (expanded) expandedIds - alarm.id else expandedIds + alarm.id
                }
                GoldKarte(Modifier.fillMaxWidth().animateItem().clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    enabled = !expanded, onClickLabel = "Wecker bearbeiten",
                    onClick = { onEdit(alarm) })) {
                    Column(Modifier.padding(18.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Switch(alarm.enabled, { vm.toggle(alarm, it) }, Modifier.semantics {
                                contentDescription = "Wecker aktivieren: ${alarm.name}"
                            }, colors = SchalterFarben())
                            Column(Modifier.weight(1f).clickable(
                                interactionSource = remember { MutableInteractionSource() }, indication = null,
                                onClickLabel = "Wecker bearbeiten", onClick = { onEdit(alarm) })) {
                                Text(alarm.timeLabel, fontFamily = IdeenSchriftBetont, fontSize = 44.sp, color = if (alarm.enabled) gold.primaer else gold.textGedaempft)
                                Text(alarm.name, style = MaterialTheme.typography.titleMedium,
                                    maxLines = if (expanded) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = toggleDetails, modifier = Modifier.semantics {
                                stateDescription = if (expanded) "Aufgeklappt" else "Zugeklappt"
                            }) {
                                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Weckerdetails ${if (expanded) "zuklappen" else "aufklappen"}: ${alarm.name}",
                                    tint = gold.primaer)
                            }
                        }
                        if (expanded) {
                            Text(scheduleLabel(alarm), color = gold.textGedaempft, style = MaterialTheme.typography.bodySmall)
                            Text(alarm.steps.joinToString(" → ") { it.title }, color = gold.primaer, style = MaterialTheme.typography.bodySmall)
                            if (alarm.snoozeUntil > 0) Text("Schlummert bis ${formatAt(alarm.snoozeUntil)}", color = gold.primaer)
                            if (alarm.enabled && alarm.nextAt > 0) Text(formatAt(alarm.nextAt), style = MaterialTheme.typography.bodySmall)
                            Text("Lautstärke ${alarm.volume} %${if (alarm.photoRequired) " · Foto-Aufgabe" else ""}", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                            if (alarm.needsSpeech) {
                                val status = when {
                                    alarm.preparationError.isNotBlank() -> "Vorbereitung offen: ${alarm.preparationError}"
                                    alarm.preparedAt == 0L -> "Sprachausgabe noch nicht offline bereit"
                                    else -> "${alarm.voiceVariants.size.takeIf { it > 0 } ?: 1} Stimmvarianten offline bereit · ${formatAt(alarm.preparedAt)}"
                                }
                                Text(status, style = MaterialTheme.typography.bodySmall,
                                    color = if (alarm.preparationError.isNotBlank() || alarm.preparedAt == 0L) Semantisch.warnung else Semantisch.erfolg)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StillerKnopf("Bearbeiten", { onEdit(alarm) }, hervorgehoben = true)
                                StillerKnopf("Testwecken", { vm.test(alarm) })
                                if (alarm.needsSpeech) StillerKnopf("Audio vorbereiten", { vm.prepare(alarm) })
                                StillerKnopf("Duplizieren", { onEdit(alarm.copy(id = UUID.randomUUID().toString(), name = "${alarm.name} – Kopie", enabled = false, nextAt = 0, snoozeUntil = 0)) })
                                if (alarm.enabled && alarm.repeats) StillerKnopf("Nächstes auslassen", { vm.skip(alarm) })
                                StillerKnopf("Löschen", { onDelete(alarm) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Section(title: String, collapsible: Boolean = false, summary: String = "", content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable(title) { mutableStateOf(!collapsible) }
    GoldKarte(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().then(if (collapsible) Modifier.clickable { expanded = !expanded } else Modifier), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = LocalGold.current.primaer)
                    if (!expanded && summary.isNotBlank()) Text(summary, style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
                }
                if (collapsible) StillerKnopf(if (expanded) "⌃" else "⌄", { expanded = !expanded })
            }
            if (expanded) content()
        }
    }
}

@Composable
private fun AlarmEditor(vm: WeckerViewModel, alarm: Alarm, activity: ComponentActivity) {
    val recording by vm.recording.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val music = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importMusic) }
    val ringtone = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        @Suppress("DEPRECATION")
        val uri = result.data?.getParcelableExtra<Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        uri?.let { vm.importMusic(it) }
    }
    var photoPath by rememberSaveable { mutableStateOf("") }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoPath.isNotBlank()) vm.referencePhoto(File(photoPath))
    }
    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) vm.startRecording() else vm.message.value = "Für das Diktat wird die Mikrofonberechtigung benötigt."
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Section("Deine Weckzeit") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(alarm.timeLabel, Modifier.weight(1f), fontSize = 52.sp, fontFamily = IdeenSchriftBetont, color = LocalGold.current.primaer)
                GoldKnopf("Uhrzeit", {
                    TimePickerDialog(activity, { _, hour, minute -> vm.change(alarm.copy(hour = hour, minute = minute)) }, alarm.hour, alarm.minute, true).show()
                })
            }
            OutlinedTextField(alarm.name, { vm.change(alarm.copy(name = it)) }, Modifier.fillMaxWidth(), label = { Text("Name des Weckers") }, singleLine = true)
            RepeatEditor(alarm, activity, vm::change)
            Text("Beim Speichern wird dieser Wecker automatisch aktiviert.", style = MaterialTheme.typography.bodySmall)
        }
        Section("Dein Weckablauf", collapsible = true, summary = alarm.steps.joinToString(" → ") { it.title }) {
            Text("Wähle die Bausteine und ihre Reihenfolge. Der gesamte Ablauf wiederholt sich bis zum Stoppen; Songs laufen vollständig durch.", style = MaterialTheme.typography.bodySmall)
            Step.entries.forEach { step -> Toggle(step.title, step in alarm.steps) { checked ->
                vm.change(alarm.copy(steps = if (checked) alarm.steps + step else alarm.steps - step))
            } }
            alarm.steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}. ${step.title}", Modifier.weight(1f))
                    if (index > 0) StillerKnopf("↑", {
                        val list = alarm.steps.toMutableList(); java.util.Collections.swap(list, index, index - 1); vm.change(alarm.copy(steps = list))
                    })
                    Spacer(Modifier.width(8.dp))
                    if (index < alarm.steps.lastIndex) StillerKnopf("↓", {
                        val list = alarm.steps.toMutableList(); java.util.Collections.swap(list, index, index + 1); vm.change(alarm.copy(steps = list))
                    })
                }
            }
            if (Step.IDEAS in alarm.steps) Text("Die offenen Ideen werden in ihrer Reihenfolge aus Geniale Ideen gelesen. Bei bestehender Verbindung bereitet die App Änderungen automatisch vor.", style = MaterialTheme.typography.bodySmall)
        }
        if (alarm.needsSpeech) AlarmSpeechEditor(vm, alarm)
        if (Step.MUSIC in alarm.steps || Step.TONE in alarm.steps) Section("Musik & Klingelzeichen", collapsible = true, summary = alarm.musicName) {
            if (Step.TONE in alarm.steps) Choice("Klingelzeichen vor dem Text", alarm.cue, Tones.names.toList()) { vm.change(alarm.copy(cue = it)) }
            Text(alarm.musicName, style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldKnopf("MP3 / Audio wählen", { music.launch(arrayOf("audio/*")) })
                StillerKnopf("Geräte-Wecktöne", {
                    ringtone.launch(Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM)
                        .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false))
                })
            }
            Tones.names.forEach { (id, title) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(alarm.music.isBlank() && alarm.tone == id, { vm.change(alarm.copy(tone = id, music = "", musicName = title)) })
                    Text(title, Modifier.weight(1f))
                    StillerKnopf("Anhören", { vm.playPreview(Tones.file(vm.store.files, id)) })
                }
            }
            StillerKnopf("Vorschau stoppen", vm::stopPreview)
            Text("Das Erinnerungszeichen dauert 2 Sekunden, die anderen eingebauten Signale 6 Sekunden. Der Musikschritt spielt die ganze Datei ab. Alle eingebauten Signale wurden eigens für diese App erzeugt.", style = MaterialTheme.typography.bodySmall)
        }
        if (Step.TEXT in alarm.steps) Section("Deine Erinnerung") {
            OutlinedTextField(alarm.text, { vm.change(alarm.copy(text = it)) }, Modifier.fillMaxWidth().heightIn(min = 160.dp), label = { Text("Text, der vorgelesen werden soll") })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldKnopf(if (recording) "■ Diktat abschließen" else "● Diktieren", {
                    if (recording) vm.stopRecording()
                    else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.startRecording()
                    else microphone.launch(Manifest.permission.RECORD_AUDIO)
                }, aktiviert = busy.isBlank())
                StillerKnopf("Text verbessern", vm::improve)
                if (alarm.originalText.isNotBlank()) StillerKnopf("Original zurückholen", { vm.change(alarm.copy(text = alarm.originalText, originalText = "")) })
            }
            Text("Groq · Whisper Large V3 Turbo. Die KI-Textverbesserung nutzt dieselbe ChatGPT-Anmeldung und Modellauswahl wie Geniale Ideen.", style = MaterialTheme.typography.bodySmall)
        }
        Section("Lautstärke & Schlummern", collapsible = true, summary = "${alarm.volume} % Lautstärke · ${alarm.snoozeMinutes} Min. schlummern") {
            ValueSlider("Wecklautstärke", alarm.volume, 1..100, "%") { vm.change(alarm.copy(volume = it)) }
            Text("Diese Lautstärke gilt beim Wecken unabhängig von der bisherigen Lautstärke. Android setzt sie auf die nächste unterstützte Lautstärkestufe. Danach wird der vorherige Wert wiederhergestellt.", style = MaterialTheme.typography.bodySmall)
            ValueSlider("Sanftes Anschwellen", alarm.fadeSeconds, 0..120, "Sek.") { vm.change(alarm.copy(fadeSeconds = it)) }
            Toggle("Vibrieren", alarm.vibrate) { vm.change(alarm.copy(vibrate = it)) }
            ValueSlider("Schlummerdauer", alarm.snoozeMinutes, 1..60, "Min.") { vm.change(alarm.copy(snoozeMinutes = it)) }
            ValueSlider("Erlaubte Schlummerpausen", alarm.snoozeLimit, 0..20, "") { vm.change(alarm.copy(snoozeLimit = it)) }
        }
        Section("Aufstehen zum Ausschalten", collapsible = true, summary = if (alarm.photoRequired) "Foto-Aufgabe aktiv" else "Normaler Stoppknopf") {
            Toggle("Foto-Aufgabe aktivieren", alarm.photoRequired) { vm.change(alarm.copy(photoRequired = it)) }
            if (alarm.photoRequired) {
                Text("Zum Stoppen muss ein neues Kamerafoto die ausgewählten Bedingungen erfüllen. Die Prüfung läuft vollständig auf dem Gerät. Mehrere Bedingungen müssen gemeinsam erfüllt sein.", style = MaterialTheme.typography.bodySmall)
                GoldKnopf("Referenzfoto aufnehmen", {
                    val file = newPhoto(activity)
                    photoPath = file.absolutePath
                    photo.launch(FileProvider.getUriForFile(activity, "${activity.packageName}.photos", file))
                })
                if (alarm.reference.isNotBlank()) {
                    PhotoPreview(File(alarm.reference))
                    StillerKnopf("Referenz entfernen", { vm.change(alarm.copy(reference = "")) })
                    ValueSlider("Benötigte Motiv-Ähnlichkeit", alarm.photoTolerance, 55..95, "%") { vm.change(alarm.copy(photoTolerance = it)) }
                    Text("Dasselbe Motiv aus ähnlicher Perspektive fotografieren. Der lokale Bildvergleich bewertet Struktur und Farben.", style = MaterialTheme.typography.bodySmall)
                }
                ValueSlider("Mindesthelligkeit im Foto", alarm.minBrightness, 0..90, "%") { vm.change(alarm.copy(minBrightness = it)) }
                Choice("Vorherrschende Farbe", alarm.color, PhotoCheck.colors.toList()) { vm.change(alarm.copy(color = it)) }
                if (alarm.color != "none") ValueSlider("Erforderlicher Farbanteil", alarm.colorPercent, 5..90, "%") { vm.change(alarm.copy(colorPercent = it)) }
                Text("Schlummern bleibt entsprechend deinem Limit möglich. Der normale Stoppknopf wird durch die Foto-Aufgabe ersetzt.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("Dein Entwurf wird automatisch gespeichert. Beim Speichern werden sechs Stimmvarianten vorbereitet. Beim Wecken folgen sie offline aufeinander.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AlarmSpeechEditor(vm: WeckerViewModel, alarm: Alarm) {
    val revision by vm.settingsRevision.collectAsStateWithLifecycle()
    val voices by vm.clonedVoices.collectAsStateWithLifecycle()
    val loading by vm.voicesLoading.collectAsStateWithLifecycle()
    val error by vm.voiceLoadError.collectAsStateWithLifecycle()
    val defaults = remember(revision) { de.frank.genialeideen.speech.SyntheseStimme(vm.settings) }
    val effective = alarm.resolveVoice(defaults)
    val available = voices.map {
        "${TtsProvider.QWEN_CLONE.id}|${it.id}" to "${vm.settings.qwenVoiceNames[it.id] ?: it.name} · Meine Stimmen"
    } + TtsCatalog.googleVoices.map {
        "${TtsProvider.GOOGLE_CLOUD.id}|${it.id}" to "${it.name} · Google"
    } + TtsCatalog.edgeVoices.map {
        "${TtsProvider.EDGE.id}|${it.id}" to "${it.name} · Edge"
    }
    val defaultId = when (defaults.ttsProvider) {
        TtsProvider.GOOGLE_CLOUD.id -> defaults.googleTtsVoice
        TtsProvider.QWEN_CLONE.id -> defaults.qwenTtsVoiceId
        TtsProvider.QWEN.id -> defaults.qwenStandardVoice
        else -> defaults.edgeTtsVoice
    }
    val defaultLabel = available.find { it.first == "${defaults.ttsProvider}|$defaultId" }?.second
        ?: vm.settings.qwenVoiceNames[defaultId] ?: defaultId.ifBlank { "Noch keine Stimme eingerichtet" }
    val selected = if (alarm.voiceProvider.isBlank()) "" else "${alarm.voiceProvider}|${alarm.voiceId}"
    val options = listOf("" to "Standard aus Einstellungen · $defaultLabel") + available +
        if (selected.isNotBlank() && available.none { it.first == selected })
            listOf(selected to "${vm.settings.qwenVoiceNames[alarm.voiceId] ?: alarm.voiceId} · gespeicherte Auswahl")
        else emptyList()
    LaunchedEffect(Unit) { vm.loadVoices() }
    Section("Stimme & Sprechgeschwindigkeit") {
        Choice("Stimme für diesen Wecker", selected, options) { chosen ->
            vm.change(alarm.copy(voiceProvider = chosen.substringBefore('|'), voiceId = chosen.substringAfter('|', "")))
        }
        Text("Sprechgeschwindigkeit: ${"%.2f".format(effective.ttsSpeechRate)}×" +
            if (alarm.speechRate == null) " · Standard aus Einstellungen" else " · nur dieser Wecker")
        Slider(effective.ttsSpeechRate, { vm.change(alarm.copy(speechRate = it)) }, valueRange = .5f..2f)
        if (alarm.speechRate != null) StillerKnopf("Standard-Sprechgeschwindigkeit verwenden", {
            vm.change(alarm.copy(speechRate = null))
        })
        Text("Ohne eigene Auswahl gelten Stimme und Sprechgeschwindigkeit aus den Einstellungen. Jede Änderung hier gilt nur für diesen Wecker.", style = MaterialTheme.typography.bodySmall)
        if (loading) Text("Deine hochgeladenen Stimmen werden geladen …", style = MaterialTheme.typography.bodySmall)
        if (error.isNotBlank() && vm.settings.qwenTtsApiKey.isNotBlank()) {
            Text(error, color = Semantisch.warnung, style = MaterialTheme.typography.bodySmall)
            StillerKnopf("Meine Stimmen erneut laden", { vm.loadVoices(force = true) })
        }
    }
}

@Composable
fun Toggle(label: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(value, change, colors = SchalterFarben())
    }
}

@Composable
fun ValueSlider(label: String, value: Int, range: IntRange, unit: String, change: (Int) -> Unit) {
    Text("$label: $value $unit", style = MaterialTheme.typography.bodyMedium)
    Slider(value.toFloat(), { change(it.roundToInt()) }, valueRange = range.first.toFloat()..range.last.toFloat())
}

@Composable
fun Choice(label: String, selected: String, options: List<Pair<String, String>>, choose: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Text(label, style = MaterialTheme.typography.labelLarge)
    GoldKnopf(options.find { it.first == selected }?.second ?: "Auswählen", { open = true }, Modifier.fillMaxWidth())
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(label) },
        text = { LazyColumn { items(options, key = { it.first }) { option ->
            Row(Modifier.fillMaxWidth().clickable { choose(option.first); open = false }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(option.first == selected, null); Text(option.second, Modifier.padding(start = 8.dp))
            }
        } } }, confirmButton = { StillerKnopf("Schließen", { open = false }) })
}

@Composable
fun PhotoPreview(file: File) {
    var bitmap by remember(file.absolutePath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(file.absolutePath) {
        bitmap = withContext(Dispatchers.IO) { runCatching { PhotoCheck.bitmap(file) }.getOrNull() }
    }
    bitmap?.let { Image(it.asImageBitmap(), "Gespeichertes Referenzmotiv", Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(16.dp))) }
}

@Composable
fun Confirm(title: String, text: String, yes: () -> Unit, no: () -> Unit) {
    AlertDialog(onDismissRequest = no, title = { Text(title) }, text = { Text(text) },
        confirmButton = { GoldKnopf("Bestätigen", yes) }, dismissButton = { StillerKnopf("Abbrechen", no) })
}

fun newPhoto(context: Context): File = File(context.cacheDir, "photos").apply { mkdirs() }.let { File(it, "${UUID.randomUUID()}.jpg") }
fun formatAt(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, dd.MM. · HH:mm", java.util.Locale.GERMAN))
fun dayLabel(days: Set<Int>): String = if (days.isEmpty()) "Einmalig" else if (days.size == 7) "Jeden Tag" else days.sorted().joinToString(" · ") { listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")[it - 1] }
fun scheduleLabel(alarm: Alarm): String = when {
    alarm.intervalDays > 0 -> "Alle ${alarm.intervalDays} Tage · ab ${dateLabel(alarm.startDate)}"
    alarm.startDate.isNotBlank() -> "Einmalig am ${dateLabel(alarm.startDate)}"
    else -> dayLabel(alarm.days)
}
private fun dateLabel(date: String) = java.time.LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

@Composable
private fun RepeatEditor(alarm: Alarm, activity: ComponentActivity, change: (Alarm) -> Unit) {
    val today = java.time.LocalDate.now()
    val mode = when { alarm.intervalDays > 0 -> "interval"; alarm.startDate.isNotBlank() -> "date"; alarm.days.isNotEmpty() -> "weekdays"; else -> "once" }
    Choice("Wann soll er wecken?", mode, listOf("once" to "Einmalig", "weekdays" to "An Wochentagen", "date" to "In X Tagen / an einem Datum", "interval" to "Schichtwecker · alle X Tage")) { chosen ->
        change(when (chosen) {
            "weekdays" -> alarm.copy(days = setOf(1, 2, 3, 4, 5), startDate = "", intervalDays = 0)
            "date" -> alarm.copy(days = emptySet(), startDate = today.plusDays(1).toString(), intervalDays = 0)
            "interval" -> alarm.copy(days = emptySet(), startDate = today.plusDays(1).toString(), intervalDays = 35)
            else -> alarm.copy(days = emptySet(), startDate = "", intervalDays = 0)
        })
    }
    if (mode == "weekdays") FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEachIndexed { index, name ->
            val day = index + 1
            FilterChip(day in alarm.days, { change(alarm.copy(days = if (day in alarm.days) alarm.days - day else alarm.days + day)) }, { Text(name) })
        }
    }
    if (mode == "date" || mode == "interval") {
        if (mode == "interval") {
            ValueSlider("Alle wie viele Tage?", alarm.intervalDays, 1..60, "Tage") { change(alarm.copy(intervalDays = it)) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 5, 8, 35).forEach { days -> FilterChip(alarm.intervalDays == days, { change(alarm.copy(intervalDays = days)) }, { Text("$days Tage") }) }
            }
        }
        val ahead = java.time.temporal.ChronoUnit.DAYS.between(today, java.time.LocalDate.parse(alarm.startDate)).toInt()
        Text(if (mode == "interval") "Erster Schichtalarm: ${dateLabel(alarm.startDate)}" else "Am ${dateLabel(alarm.startDate)} um ${alarm.timeLabel}")
        ValueSlider("Schnellwahl: in", ahead.coerceIn(0, 60), 0..60, "Tagen") {
            change(alarm.copy(startDate = today.plusDays(it.toLong()).toString()))
        }
        StillerKnopf("Datum im Kalender wählen", {
            val date = java.time.LocalDate.parse(alarm.startDate)
            android.app.DatePickerDialog(activity, { _, year, month, day ->
                change(alarm.copy(startDate = java.time.LocalDate.of(year, month + 1, day).toString()))
            }, date.year, date.monthValue - 1, date.dayOfMonth).show()
        })
        if (mode == "interval") Text("Der Rhythmus bleibt am Startdatum verankert. Schlummern oder das Auslassen eines Termins verschiebt deine Schichtfolge nicht.", style = MaterialTheme.typography.bodySmall)
    }
    val next = runCatching { AlarmTime.next(alarm) }.getOrNull()
    Text(next?.let { "Nächster Termin: ${formatAt(it)}" } ?: "Bitte einen zukünftigen Termin wählen.", style = MaterialTheme.typography.bodySmall, color = LocalGold.current.primaer)
}
private fun remaining(ms: Long): String { val minutes = (ms / 60_000).coerceAtLeast(0); return "${minutes / 60} Std. ${minutes % 60} Min." }

@Composable
private fun IdeasPage(vm: WeckerViewModel) {
    val ideas by vm.ideas.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.syncIdeas() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Section("Die Brücke zu Geniale Ideen") {
            Text("Nur offene Ideen, in derselben Reihenfolge. Die Originale bleiben in Geniale Ideen. Dein Wecker erhält eine lokale Lesekopie.")
            GoldKnopf("Jetzt abgleichen", vm::syncIdeas)
            vm.store.prefs.getLong("ideasAt", 0).takeIf { it > 0 }?.let { Text("Stand: ${formatAt(it)}", style = MaterialTheme.typography.bodySmall) }
        } }
        items(ideas, key = { it.id }) { idea -> Section(idea.title) { Text(idea.text) } }
        if (ideas.isEmpty()) item { Leerzustand("✧", "Noch keine offenen Ideen", "Öffne Geniale Ideen und lege dort eine offene Idee an. Beide Apps benötigen den aktuellen Stand.") }
    }
}

@Composable
fun ReadinessCard(onSettings: () -> Unit, hideWhenReady: Boolean = false) {
    val state = rememberReadiness()
    if (hideWhenReady && state.all { it.second }) return
    GoldKarte {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (state.all { it.second }) Icons.Default.VerifiedUser else Icons.Default.NotificationsActive,
                null, tint = if (state.all { it.second }) Semantisch.erfolg else Semantisch.warnung)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(if (state.all { it.second }) "Weckberechtigungen bereit" else "Wecken einrichten", style = MaterialTheme.typography.titleSmall)
                Text(state.filterNot { it.second }.joinToString(" · ") { it.first }.ifBlank { "Genaue Zeit · Sperrbildschirm · Nicht stören" }, style = MaterialTheme.typography.bodySmall)
            }
            StillerKnopf("Prüfen", onSettings)
        }
    }
}

@Composable
fun rememberReadiness(): List<Pair<String, Boolean>> {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { revision++; delay(2000) }
        }
    }
    return remember(revision) { readiness(context) }
}

fun readiness(context: Context): List<Pair<String, Boolean>> {
    val manager = context.getSystemService(NotificationManager::class.java)
    val policy = runCatching { if (Build.VERSION.SDK_INT >= 30) manager.consolidatedNotificationPolicy else manager.notificationPolicy }.getOrNull()
    val allowsAlarms = Build.VERSION.SDK_INT < 28 || (policy != null && (policy.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS) != 0)
    return listOf("Genaue Weckzeiten" to AlarmScheduler(context).allowed(),
        "Benachrichtigungen" to manager.areNotificationsEnabled(),
        "Vollbild-Wecker" to (Build.VERSION.SDK_INT < 34 || manager.canUseFullScreenIntent()),
        "Wecker bei Nicht stören" to (manager.isNotificationPolicyAccessGranted && allowsAlarms && manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_NONE))
}

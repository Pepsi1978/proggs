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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
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
        // A refusal must never block saving: the alarm still rings, only the lock-screen controls are missing.
        vm.save(notificationsDenied = !allowed) { page = "alarms" }
    }
    var leaveEditor by remember { mutableStateOf(false) }
    var settingsFrom by rememberSaveable { mutableStateOf("alarms") }
    var pendingOpen by remember { mutableStateOf<(() -> Unit)?>(null) }
    /** Opening another draft while unsaved changes exist asks first instead of overwriting them. */
    fun openDraft(open: () -> Unit) { if (draft != null && vm.draftChanged()) pendingOpen = open else { open(); page = "edit" } }
    fun back() {
        vm.stopPreview()
        if (page == "edit" && draft != null && vm.draftChanged()) leaveEditor = true
        else if (page == "settings" && settingsFrom == "edit" && draft != null) page = "edit"
        else { if (page == "edit") vm.closeEditor(); page = "alarms" }
    }
    GenialeIdeenTheme(theme) {
        val gold = LocalGold.current
        BackHandler(page != "alarms") { back() }
        Box(Modifier.fillMaxSize().background(gold.hintergrund)) {
            SichtbarerHintergrund()
            Column(Modifier.fillMaxSize().imePadding()) {
                IdeenKopfleiste(
                    titel = when (page) { "edit" -> if (vm.isNewDraft) "Neuer Wecker" else "Wecker bearbeiten"; "settings" -> "Einstellungen"; else -> "Genialer Wecker" },
                    themeWahl = theme,
                    aufThemeTipp = { vm.settings.theme = if (theme == "dark") "light" else "dark" },
                    aufEinstellungen = if (page == "settings") null else ({ vm.stopPreview(); settingsFrom = page; page = "settings" }),
                    voran = if (page != "alarms") ({ StillerKnopf("‹", { back() }, Modifier.semantics { contentDescription = "Zurück zur Weckerliste" }); Spacer(Modifier.width(8.dp)) }) else null,
                )
                if (busy.isNotBlank() || audioBusy.isNotBlank()) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(busy.ifBlank { audioBusy }, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.bodySmall)
                    StillerKnopf("Abbrechen", vm::cancelAction)
                }
                AufnahmeLeiste(vm)
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
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GoldKnopf("Wecker speichern", {
                                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else vm.save { page = "alarms" }
                                    }, Modifier.fillMaxWidth(), aktiviert = busy.isBlank() && !recording, hauptKnopf = true)
                                    SpeicherVorschau(alarm)
                                }
                            }
                        } } ?: Box(Modifier.fillMaxSize())
                        "settings" -> SettingsPage(vm, activity)
                        else -> AlarmList(alarms, vm,
                            onNew = { openDraft { vm.newAlarm() } },
                            onEdit = { alarm -> if (draft?.id == alarm.id) page = "edit" else openDraft { vm.edit(alarm) } }, onDelete = { delete = it },
                            onSettings = { settingsFrom = "alarms"; page = "settings" },
                            openDraft = draft?.takeIf { vm.draftChanged() }, onResumeDraft = { page = "edit" })
                    }
                }
            }
        }
        if (leaveEditor) AlertDialog(onDismissRequest = { leaveEditor = false },
            title = { Text("Änderungen speichern?") },
            text = { Text("Du hast diesen Wecker geändert, aber noch nicht gespeichert.") },
            confirmButton = { GoldKnopf("Speichern", { leaveEditor = false; vm.save { page = "alarms" } }) },
            dismissButton = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StillerKnopf("Verwerfen", { leaveEditor = false; vm.closeEditor(); page = "alarms" })
                StillerKnopf("Weiter bearbeiten", { leaveEditor = false })
            } })
        pendingOpen?.let { open -> AlertDialog(onDismissRequest = { pendingOpen = null },
            title = { Text("Ungespeicherten Entwurf verwerfen?") },
            text = { Text("„${draft?.name.orEmpty()}“ hat noch ungespeicherte Änderungen.") },
            confirmButton = { GoldKnopf("Entwurf fortsetzen", { pendingOpen = null; page = "edit" }) },
            dismissButton = { StillerKnopf("Verwerfen", { pendingOpen = null; vm.closeEditor(); open(); page = "edit" }) }) }
        delete?.let { alarm -> Confirm("Wecker löschen?", "„${alarm.name}“ wird entfernt.", {
            vm.delete(alarm); delete = null
        }, { delete = null }) }
    }
}

@Composable
private fun AlarmList(alarms: List<Alarm>, vm: WeckerViewModel, onNew: () -> Unit, onEdit: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit, onSettings: () -> Unit, openDraft: Alarm?, onResumeDraft: () -> Unit) {
    val gold = LocalGold.current
    // Nur für diese Listenansicht merken: neue Wecker und eine neu geöffnete Liste sind kompakt.
    var expandedIds by remember { mutableStateOf(emptySet<String>()) }
    val issues by vm.store.issues.collectAsState()
    val now = rememberNow(60_000)
    val nextRegular = alarms.mapNotNull { alarm -> alarm.nextAt.takeIf { alarm.enabled && it > now } }.minOrNull()
    val nextSnooze = alarms.mapNotNull { alarm -> alarm.snoozeUntil.takeIf { it > now } }.minOrNull()
    val next = listOfNotNull(nextRegular, nextSnooze).minOrNull()
    val nextIsSnooze = next != null && next == nextSnooze
    BoxWithConstraints {
        val ringSize = if (maxWidth < 360.dp) 88.dp else 104.dp
        LazyVerticalGrid(columns = GridCells.Fixed(if (maxWidth >= 680.dp) 2 else 1),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GoldKarte(erhoeht = true) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            RestzeitRing(now, next, nextIsSnooze, Modifier.size(ringSize))
                            // No maxLines: with large system fonts the lines wrap instead of being cut off.
                            Column(Modifier.weight(1f)) {
                                // The current time appears exactly once.
                                Text(formatClock(now), fontFamily = IdeenSchriftBetont, fontSize = 56.sp, color = gold.primaer)
                                if (next == null) {
                                    Text("Kein Wecker aktiv", style = MaterialTheme.typography.titleMedium, color = gold.textPrimaer)
                                    Text("Schalte einen Wecker ein oder lege einen neuen an.", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                                } else {
                                    TerminZeile(now, next, nextIsSnooze)
                                    Text("in ${remainingLong(next - now)}", style = MaterialTheme.typography.bodyMedium, color = gold.primaer)
                                }
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GoldKnopf("＋ Wecker", onNew, hauptKnopf = true)
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { ReadinessCard(onSettings, hideWhenReady = true) }
            if (openDraft != null) item(span = { GridItemSpan(maxLineSpan) }) {
                GoldKarte {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, null, tint = gold.primaer)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("Ungespeicherter Entwurf", style = MaterialTheme.typography.titleSmall)
                            Text("${openDraft.timeLabel} · ${openDraft.name}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        StillerKnopf("Weiter bearbeiten", onResumeDraft, hervorgehoben = true)
                    }
                }
            }
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
                            Switch(alarm.enabled, { on ->
                                // A one-off date that has passed cannot ring: open the editor to pick a new date instead of failing.
                                val latest = vm.store.get(alarm.id) ?: alarm
                                if (on && latest.isExpiredOnce()) onEdit(latest) else vm.toggle(alarm, on)
                            }, Modifier.semantics {
                                contentDescription = "Wecker aktivieren: ${alarm.name}"
                            }, colors = SchalterFarben())
                            Column(Modifier.weight(1f).clickable(
                                interactionSource = remember { MutableInteractionSource() }, indication = null,
                                onClickLabel = "Wecker bearbeiten", onClick = { onEdit(alarm) })) {
                                val reduced = LocalBewegungReduziert.current
                                val timeColor by androidx.compose.animation.animateColorAsState(if (alarm.enabled) gold.primaer else gold.textGedaempft,
                                    if (reduced) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(250), label = "weckzeitFarbe")
                                Text(alarm.timeLabel, fontFamily = IdeenSchriftBetont, fontSize = 44.sp, color = timeColor)
                                Text(alarm.name, style = MaterialTheme.typography.titleMedium,
                                    maxLines = if (expanded) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis)
                            }
                            KlappKnopf(expanded, toggleDetails,
                                beschreibung = "Weckerdetails ${if (expanded) "zuklappen" else "aufklappen"}: ${alarm.name}")
                        }
                        // Reliability warnings stay visible even on collapsed cards.
                        issues[alarm.id]?.let { StatusZeile(Icons.Default.Warning, it, Semantisch.warnung) }
                        if (alarm.snoozeUntil > now) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Snooze, null, tint = gold.primaer, modifier = Modifier.size(20.dp))
                            Text("Schlummert bis ${formatClock(alarm.snoozeUntil)}", Modifier.weight(1f).padding(horizontal = 8.dp), color = gold.primaer)
                            StillerKnopf("Schlummern beenden", { vm.endSnooze(alarm) }, hervorgehoben = true)
                        }
                        // One compact line tells when it rings, without opening the card.
                        if (alarm.enabled && alarm.nextAt > 0 && alarm.sleepMinutes > 0) SchlafZeile(Schlaf.hinweis(alarm.nextAt, alarm.sleepMinutes, now))
                        if (!expanded) Text(if (alarm.enabled && alarm.nextAt > 0) "${scheduleLabel(alarm)} · ${formatAt(alarm.nextAt)}" else "${scheduleLabel(alarm)} · ausgeschaltet",
                            color = gold.textGedaempft, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (expanded) {
                            Text(scheduleLabel(alarm), color = gold.textGedaempft, style = MaterialTheme.typography.bodySmall)
                            Text(alarm.steps.joinToString(" → ") { it.title }, color = gold.primaer, style = MaterialTheme.typography.bodySmall)
                            val skipped = alarm.enabled && alarm.repeats && alarm.nextAt > 0 &&
                                runCatching { alarm.nextAt > AlarmTime.next(alarm, Instant.ofEpochMilli(now)) }.getOrDefault(false)
                            if (alarm.enabled && alarm.nextAt > 0) StatusZeile(Icons.Default.Alarm, "Nächster Termin: ${formatAt(alarm.nextAt)}", gold.textPrimaer)
                            if (skipped) StatusZeile(Icons.Default.SkipNext, "Ein Termin wird ausgelassen", gold.primaer)
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
                                if (alarm.enabled && alarm.repeats) {
                                    if (skipped) StillerKnopf("Auslassen rückgängig", { vm.unskip(alarm) })
                                    else StillerKnopf("Nächsten Termin auslassen", { vm.skip(alarm) })
                                }
                                if (alarm.needsSpeech) StillerKnopf("Audio vorbereiten", { vm.prepare(alarm) })
                                StillerKnopf("Duplizieren", { onEdit(alarm.copy(id = UUID.randomUUID().toString(), name = "${alarm.name} – Kopie", enabled = false, nextAt = 0, snoozeUntil = 0, snoozes = 0)) })
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
fun Section(title: String, collapsible: Boolean = false, summary: String = "", error: String? = null,
    initiallyExpanded: Boolean = !collapsible, content: @Composable ColumnScope.() -> Unit) {
    // Only visibility is remembered here; all values live in the draft in the ViewModel.
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded || !collapsible) }
    GoldKarte(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).then(if (collapsible) Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button,
                onClickLabel = if (expanded) "$title zuklappen" else "$title aufklappen") { expanded = !expanded }
                .semantics { stateDescription = if (expanded) "Aufgeklappt" else "Zugeklappt" } else Modifier),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = LocalGold.current.primaer)
                    if (!expanded && summary.isNotBlank()) Text(summary, style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
                    // A missing required input is shown in its own card, also while collapsed.
                    if (error != null) Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Semantisch.warnung, modifier = Modifier.size(18.dp))
                        Text(error, Modifier.padding(start = 6.dp), color = Semantisch.warnung, style = MaterialTheme.typography.bodySmall)
                    }
                }
                // The header row already announces this action; the button itself stays silent for TalkBack.
                if (collapsible) KlappKnopf(expanded, { expanded = !expanded }, beschreibung = null, modifier = Modifier.padding(start = 8.dp))
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
        // Derived from this draft only, so it disappears once corrected and never carries over to another draft.
        val minute = rememberNow(60_000)
        OffenesDiktatKarte(vm, alarm)
        if (remember(alarm, minute) { alarm.isExpiredOnce() }) GoldKarte(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EventBusy, null, tint = Semantisch.warnung)
                Text("Dieses Datum liegt in der Vergangenheit. Wähle einen neuen Termin – beim Speichern wird der Wecker eingeschaltet.",
                    Modifier.padding(start = 12.dp), color = Semantisch.warnung, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Section("Deine Weckzeit", collapsible = true, initiallyExpanded = true,
            summary = listOfNotNull(alarm.timeLabel, alarm.name.ifBlank { null }, scheduleLabel(alarm),
                if (alarm.sleepMinutes > 0) "Schlafdauer ${Schlaf.dauer(alarm.sleepMinutes)}" else null).joinToString(" · ")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val pickTime = { TimePickerDialog(activity, { _, hour, minute -> vm.change(alarm.copy(hour = hour, minute = minute)) }, alarm.hour, alarm.minute, true).show() }
                Text(alarm.timeLabel, Modifier.weight(1f).clickable(onClickLabel = "Uhrzeit ändern", onClick = pickTime), fontSize = 52.sp, fontFamily = IdeenSchriftBetont, color = LocalGold.current.primaer)
                GoldKnopf("Uhrzeit ändern", pickTime)
            }
            OutlinedTextField(alarm.name, { vm.change(alarm.copy(name = it)) }, Modifier.fillMaxWidth(), label = { Text("Name des Weckers") }, singleLine = true)
            RepeatEditor(alarm, activity, vm::change)
            SchlafdauerEingabe(alarm, vm::change)
        }
        Section("Dein Weckablauf", collapsible = true, summary = alarm.steps.joinToString(" → ") { it.title }.ifBlank { "Kein Schritt gewählt" },
            error = if (alarm.steps.isEmpty()) "Wähle mindestens einen Weckschritt." else null) {
            Text("Der gesamte Ablauf wiederholt sich bis zum Stoppen; Songs laufen vollständig durch.", style = MaterialTheme.typography.bodySmall)
            Text("Bausteine auswählen", style = MaterialTheme.typography.titleSmall, color = LocalGold.current.primaer)
            Step.entries.forEach { step -> Toggle(step.title, step in alarm.steps) { checked ->
                vm.change(alarm.copy(steps = if (checked) alarm.steps + step else alarm.steps - step))
            } }
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = LocalGold.current.primaer.copy(alpha = .4f))
            Text("Reihenfolge beim Wecken (verschieben per Drag-and-drop)", style = MaterialTheme.typography.titleSmall, color = LocalGold.current.primaer)
            if (alarm.steps.isEmpty()) Text("Noch kein Baustein ausgewählt.", style = MaterialTheme.typography.bodySmall)
            else de.frank.module.draganddrop.WeckReihenfolgeListe(alarm, vm::change)
            if (Step.IDEAS in alarm.steps) Text("Die offenen Ideen werden in ihrer Reihenfolge aus Geniale Ideen gelesen. Bei bestehender Verbindung bereitet die App Änderungen automatisch vor. Ansehen und abgleichen: Einstellungen → Geniale Ideen.", style = MaterialTheme.typography.bodySmall)
        }
        if (alarm.needsSpeech) AlarmSpeechEditor(vm, alarm)
        if (Step.MUSIC in alarm.steps || Step.TONE in alarm.steps) Section("Musik & Klingelzeichen", collapsible = true, summary = listOfNotNull(
            if (Step.TONE in alarm.steps) "Klingelzeichen: ${Tones.names[alarm.cue] ?: alarm.cue}" else null,
            if (Step.MUSIC in alarm.steps) "Musik: ${alarm.musicName}" else null).joinToString(" · ")) {
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
                    Row(Modifier.weight(1f).heightIn(min = 48.dp).selectable(alarm.music.isBlank() && alarm.tone == id, role = androidx.compose.ui.semantics.Role.RadioButton) {
                        vm.change(alarm.copy(tone = id, music = "", musicName = title)) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(alarm.music.isBlank() && alarm.tone == id, null)
                        Text(title, Modifier.padding(start = 8.dp))
                    }
                    StillerKnopf("Anhören", { vm.playTone(id) })
                }
            }
            StillerKnopf("Vorschau stoppen", vm::stopPreview)
            Text("Das Erinnerungszeichen dauert 2 Sekunden, die anderen eingebauten Signale 6 Sekunden. Der Musikschritt spielt die ganze Datei ab. Alle eingebauten Signale wurden eigens für diese App erzeugt.", style = MaterialTheme.typography.bodySmall)
        }
        if (Step.TEXT in alarm.steps) Section("Deine Erinnerung", collapsible = true, initiallyExpanded = alarm.text.isBlank(),
            summary = alarm.text.trim().replace('\n', ' ').let { if (it.length > 50) "„${it.take(50)}…“" else if (it.isNotBlank()) "„$it“" else "" },
            error = if (alarm.text.isBlank()) "Der Erinnerungstext fehlt." else null) {
            OutlinedTextField(alarm.text, { vm.change(alarm.copy(text = it)) }, Modifier.fillMaxWidth().heightIn(min = 160.dp), label = { Text("Text, der vorgelesen werden soll") })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldKnopf(if (recording) "■ Diktat abschließen" else "● Diktieren", {
                    if (recording) vm.stopRecording()
                    else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.startRecording()
                    else microphone.launch(Manifest.permission.RECORD_AUDIO)
                // Stop is always possible; start only while nothing else runs.
                }, aktiviert = recording || busy.isBlank())
                StillerKnopf("Text verbessern", vm::improve)
                if (alarm.originalText.isNotBlank()) StillerKnopf("Original zurückholen", { vm.change(alarm.copy(text = alarm.originalText, originalText = "")) })
            }
            Text("Groq · Whisper Large V3 Turbo. Die KI-Textverbesserung nutzt dieselbe ChatGPT-Anmeldung und Modellauswahl wie Geniale Ideen.", style = MaterialTheme.typography.bodySmall)
        }
        Section("Lautstärke & Schlummern", collapsible = true, summary = listOf(
            "${alarm.volume} % Lautstärke",
            if (alarm.fadeSeconds > 0) "Anschwellen ${alarm.fadeSeconds} Sek." else "ohne Anschwellen",
            if (alarm.vibrate) "Vibration an" else "Vibration aus",
            if (alarm.snoozeLimit == 0) "Schlummern deaktiviert" else "Schlummern ${alarm.snoozeMinutes} Min., bis ${alarm.snoozeLimit}×",
        ).joinToString(" · ")) {
            ValueSlider("Wecklautstärke", alarm.volume, 1..100, "%") { vm.change(alarm.copy(volume = it)) }
            Text("Diese Lautstärke gilt beim Wecken unabhängig von der bisherigen Lautstärke. Android setzt sie auf die nächste unterstützte Lautstärkestufe. Danach wird der vorherige Wert wiederhergestellt.", style = MaterialTheme.typography.bodySmall)
            ValueSlider("Sanftes Anschwellen", alarm.fadeSeconds, 0..120, "Sek.") { vm.change(alarm.copy(fadeSeconds = it)) }
            Toggle("Vibrieren", alarm.vibrate) { vm.change(alarm.copy(vibrate = it)) }
            ValueSlider("Schlummerdauer", alarm.snoozeMinutes, 1..60, "Min.") { vm.change(alarm.copy(snoozeMinutes = it)) }
            ValueSlider("Erlaubte Schlummerpausen", alarm.snoozeLimit, 0..20, "") { vm.change(alarm.copy(snoozeLimit = it)) }
        }
        Section("Aufstehen zum Ausschalten", collapsible = true,
            summary = if (!alarm.photoRequired) "Normaler Stoppknopf" else listOfNotNull(
                if (alarm.reference.isNotBlank()) "Referenzmotiv ab ${alarm.photoTolerance} % Ähnlichkeit" else null,
                if (alarm.minBrightness > 0) "Helligkeit ab ${alarm.minBrightness} %" else null,
                if (alarm.color != "none") "${PhotoCheck.colors[alarm.color]} ab ${alarm.colorPercent} %" else null,
            ).joinToString(" · ", prefix = "Foto-Aufgabe: ").removeSuffix("Foto-Aufgabe: ").ifBlank { "Foto-Aufgabe ohne Bedingung" },
            error = if (alarm.photoRequired && alarm.reference.isBlank() && alarm.color == "none" && alarm.minBrightness == 0)
                "Lege ein Referenzfoto, eine Mindesthelligkeit oder eine Farbe fest." else null) {
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
    Section("Stimme & Sprechgeschwindigkeit", collapsible = true, summary = listOf(
        options.find { it.first == selected }?.second ?: "Stimme wählen",
        "Tempo ${"%.2f".format(effective.ttsSpeechRate)}× ${if (alarm.speechRate == null) "(Standard)" else "(nur dieser Wecker)"}",
    ).joinToString(" · ")) {
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

/** The former raised StillerKnopf (⌃/⌄) as fold button; small body, touch target extended to 48 dp. */
@Composable
fun KlappKnopf(expanded: Boolean, onToggle: () -> Unit, beschreibung: String?, modifier: Modifier = Modifier) {
    StillerKnopf(if (expanded) "⌃" else "⌄", onToggle, modifier.minimumInteractiveComponentSize().then(
        if (beschreibung == null) Modifier.clearAndSetSemantics {}
        else Modifier.semantics { contentDescription = beschreibung; stateDescription = if (expanded) "Aufgeklappt" else "Zugeklappt" }))
}

@Composable
fun Toggle(label: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = change),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(value, null, colors = SchalterFarben())
    }
}

@Composable
fun ValueSlider(label: String, value: Int, range: IntRange, unit: String, change: (Int) -> Unit) {
    Text("$label: $value $unit", style = MaterialTheme.typography.bodyMedium)
    Slider(value.toFloat(), { change(it.roundToInt()) }, Modifier.semantics { contentDescription = label; stateDescription = "$value $unit" },
        valueRange = range.first.toFloat()..range.last.toFloat())
}

@Composable
fun Choice(label: String, selected: String, options: List<Pair<String, String>>, choose: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Text(label, style = MaterialTheme.typography.labelLarge)
    GoldKnopf(options.find { it.first == selected }?.second ?: "Auswählen", { open = true }, Modifier.fillMaxWidth())
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(label) },
        text = { LazyColumn { items(options, key = { it.first }) { option ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(option.first == selected, role = androidx.compose.ui.semantics.Role.RadioButton) { choose(option.first); open = false }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
fun dayLabel(days: Set<Int>): String = if (days.isEmpty()) "Einmalig" else if (days.size == 7) "Täglich" else if (days == setOf(1, 2, 3, 4, 5)) "Mo–Fr" else days.sorted().joinToString(" · ") { listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")[it - 1] }
fun scheduleLabel(alarm: Alarm): String = when {
    alarm.intervalDays > 0 -> "Alle ${alarm.intervalDays} Tage · ab ${dateLabel(alarm.startDate)}"
    alarm.startDate.isNotBlank() -> "Einmalig am ${dateLabel(alarm.startDate)}"
    else -> dayLabel(alarm.days)
}
private fun dateLabel(date: String) = java.time.LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

@Composable
private fun RepeatEditor(alarm: Alarm, activity: ComponentActivity, change: (Alarm) -> Unit) {
    val today = java.time.LocalDate.now()
    val mode = when { alarm.intervalDays > 0 -> "interval"; alarm.startDate.isNotBlank() -> "date"; alarm.days.size == 7 -> "daily"; alarm.days.isNotEmpty() -> "weekdays"; else -> "once" }
    fun changeMode(chosen: String) {
        // Tapping the active mode again changes nothing, so a chosen date or individual weekdays are never reset.
        if (chosen == mode) return
        change(when (chosen) {
            "daily" -> alarm.copy(days = (1..7).toSet(), startDate = "", intervalDays = 0)
            "weekdays" -> alarm.copy(days = setOf(1, 2, 3, 4, 5), startDate = "", intervalDays = 0)
            "date" -> alarm.copy(days = emptySet(), startDate = today.plusDays(1).toString(), intervalDays = 0)
            "interval" -> alarm.copy(days = emptySet(), startDate = today.plusDays(1).toString(), intervalDays = 35)
            else -> alarm.copy(days = emptySet(), startDate = "", intervalDays = 0)
        })
    }
    Text("Wann soll er wecken?", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("once" to "Einmalig", "daily" to "Täglich", "weekdays" to "An Wochentagen", "date" to "An einem Datum", "interval" to "Schicht · alle X Tage").forEach { (id, label) ->
            FilterChip(mode == id, { changeMode(id) }, { Text(label) })
        }
    }
    if (mode == "weekdays") {
        val names = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEachIndexed { index, name ->
                val day = index + 1
                // Removing the last day keeps the weekday mode instead of silently turning into a one-off alarm.
                FilterChip(day in alarm.days, { if (day !in alarm.days || alarm.days.size > 1) change(alarm.copy(days = if (day in alarm.days) alarm.days - day else alarm.days + day)) },
                    { Text(name) }, Modifier.semantics { contentDescription = names[index] })
            }
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
}
/** Like [remaining], but switches to days beyond 24 hours. */
fun remainingLong(ms: Long): String {
    val minutes = ZeitRing.ceilMinutes(ms)
    val days = minutes / (24 * 60)
    return if (days == 0L) remaining(ms) else "$days ${if (days == 1L) "Tag" else "Tagen"} ${minutes / 60 % 24} Std."
}

/** Symbol plus text, so a status is never conveyed by color alone. */
@Composable
private fun StatusZeile(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, Modifier.padding(start = 8.dp), color = color, style = MaterialTheme.typography.bodySmall)
    }
}

/** Marker exactly as drawn on the ring: now is hollow, the alarm is filled. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.ringMarker(at: androidx.compose.ui.geometry.Offset, hollow: Boolean,
    color: androidx.compose.ui.graphics.Color, surface: androidx.compose.ui.graphics.Color) {
    if (hollow) {
        drawCircle(surface, 4.5f.dp.toPx(), at)
        drawCircle(color, 4.5f.dp.toPx(), at, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
    } else drawCircle(color, 5.5f.dp.toPx(), at)
}

@Composable
private fun LegendenMarker(hollow: Boolean, color: androidx.compose.ui.graphics.Color) {
    val surface = LocalGold.current.flaecheErhoeht
    androidx.compose.foundation.Canvas(Modifier.size(14.dp)) { ringMarker(center, hollow, color, surface) }
}

/** The alarm term with the same filled marker as on the ring; the weekday and date are added when it is not today. */
@Composable
private fun TerminZeile(now: Long, target: Long, snooze: Boolean) {
    val gold = LocalGold.current
    val accent = if (snooze) Semantisch.info else gold.primaer
    val zone = ZoneId.systemDefault()
    val sameDay = Instant.ofEpochMilli(now).atZone(zone).toLocalDate() == Instant.ofEpochMilli(target).atZone(zone).toLocalDate()
    // Not today: the concrete date, e.g. "Fr, 18.09. · 09:00".
    val targetText = if (sameDay) formatClock(target) else formatAt(target)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LegendenMarker(hollow = false, color = accent)
        Text("${if (snooze) "Schlummern bis" else "Wecker"} $targetText", style = MaterialTheme.typography.bodyMedium, color = gold.textPrimaer)
    }
}

/**
 * Normale 12-Stunden-Uhr: 12 oben, 3 rechts, 6 unten, 9 links. Bogen im Uhrzeigersinn von jetzt (hohler Punkt)
 * bis zum Wecker (gefüllter Punkt), nur wenn eindeutig (siehe [ZeitRing]); sonst kurze Kennzeichnung in der Mitte.
 * Für TalkBack stumm, der Text daneben ist maßgeblich.
 */
@Composable
private fun RestzeitRing(now: Long, target: Long?, snooze: Boolean, modifier: Modifier = Modifier) {
    val gold = LocalGold.current
    val accent = if (snooze) Semantisch.info else gold.primaer
    val geometry = target?.let { ZeitRing.berechne(now, it) }
    val nowAngle = ZeitRing.angle(now, ZoneId.systemDefault())
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val numberStyle = MaterialTheme.typography.labelSmall.copy(color = gold.textGedaempft, fontSize = 10.sp)
    val centerStyle = MaterialTheme.typography.labelMedium.copy(color = accent, fontWeight = FontWeight.SemiBold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    val surface = gold.flaecheErhoeht
    androidx.compose.foundation.Canvas(modifier) {
        val stroke = 5.dp.toPx()
        val radius = size.minDimension / 2f - stroke
        val topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius)
        val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        fun point(angleDeg: Float, r: Float = radius) = Math.toRadians(angleDeg.toDouble()).let {
            center + androidx.compose.ui.geometry.Offset((r * Math.cos(it)).toFloat(), (r * Math.sin(it)).toFloat())
        }
        drawCircle(gold.textGedaempft.copy(alpha = .35f), radius, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        repeat(12) { hour ->
            val a = hour * 30f - 90f
            val long = hour % 3 == 0
            drawLine(gold.textGedaempft.copy(alpha = if (long) .8f else .45f), point(a, radius - (if (long) 7 else 4).dp.toPx()), point(a, radius), 1.5f.dp.toPx())
        }
        listOf(0 to "12", 3 to "3", 6 to "6", 9 to "9").forEach { (hour, label) ->
            val layout = measurer.measure(label, numberStyle)
            val at = point(hour * 30f - 90f, radius - 15.dp.toPx())
            drawText(layout, topLeft = at - androidx.compose.ui.geometry.Offset(layout.size.width / 2f, layout.size.height / 2f))
        }
        val sweep = geometry?.sweep
        if (geometry != null && sweep != null) drawArc(accent, geometry.nowAngle, sweep, useCenter = false, topLeft = topLeft, size = arcSize,
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        geometry?.centerLabel?.let { label ->
            // Fit inside the space the 12/3/6/9 numbers leave free: shrink, then try two lines, never paint over them.
            val numberRadius = radius - 15.dp.toPx()
            val maxWidth = 2 * (numberRadius - measurer.measure("3", numberStyle).size.width / 2f - 2.dp.toPx())
            val maxHeight = 2 * (numberRadius - measurer.measure("12", numberStyle).size.height / 2f - 2.dp.toPx())
            val twoLines = when {
                label == "Umstellung" -> "Um-\nstellung"
                ' ' in label -> label.replaceFirst(' ', '\n')
                else -> label
            }
            val layout = listOf(11, 10, 9, 8).asSequence().flatMap { size -> sequenceOf(label, twoLines).map { it to size } }
                .map { (text, size) -> measurer.measure(text, centerStyle.copy(fontSize = size.sp)) }
                .firstOrNull { it.size.width <= maxWidth && it.size.height <= maxHeight }
                ?: measurer.measure(twoLines, centerStyle.copy(fontSize = 8.sp),
                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = maxWidth.toInt().coerceAtLeast(1)))
            drawText(layout, topLeft = center - androidx.compose.ui.geometry.Offset(layout.size.width / 2f, layout.size.height / 2f))
        }
        if (geometry != null) ringMarker(point(geometry.targetAngle), hollow = false, color = accent, surface = surface)
        ringMarker(point(nowAngle), hollow = true, color = gold.textPrimaer, surface = surface)
    }
}

/**
 * Global recording bar below the header, on every page and independent of collapsed sections: duration, the real
 * input level as a simple bar, and a 48 dp stop button. After stopping it shows the processing status instead.
 */
@Composable
private fun AufnahmeLeiste(vm: WeckerViewModel) {
    val session by vm.recordingSession.collectAsStateWithLifecycle()
    val status by vm.recordingStatus.collectAsStateWithLifecycle()
    val open by vm.openDictation.collectAsStateWithLifecycle()
    val gold = LocalGold.current
    val current = session
    when {
        // Processing status wins: while stopping, the bar no longer claims an active recording.
        status.isNotBlank() -> Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(status, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.bodySmall)
            if (status == "Diktat wird transkribiert …") StillerKnopf("Abbrechen", vm::cancelDictation)
        }
        current != null -> Row(Modifier.fillMaxWidth().background(gold.flaeche.copy(alpha = .9f)).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            val animate = !LocalBewegungReduziert.current && rememberResumed()
            val pulse = if (animate) rememberInfiniteTransition(label = "aufnahmePuls").animateFloat(.45f, 1f,
                infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "aufnahmePunkt").value else 1f
            Box(Modifier.size(12.dp).graphicsLayer { alpha = pulse }.background(Semantisch.fehler, androidx.compose.foundation.shape.CircleShape))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                val seconds = ((rememberNow(1000) - current.startedAt) / 1000).coerceAtLeast(0)
                val label = if (current.kind == RecordingKind.DIKTAT) "Diktat für „${current.draftName}“" else "Stimmprobe"
                Text("$label · ${seconds / 60}:${"%02d".format(seconds % 60)}", style = MaterialTheme.typography.bodySmall, color = gold.textPrimaer)
                // Real input level, collected only while resumed.
                val level by vm.recordingLevel.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)
                Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(gold.textGedaempft.copy(alpha = .25f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(level.coerceIn(0f, 1f)).background(gold.primaer))
                }
            }
            Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                StillerKnopf("■ Stoppen", vm::stopRecording, Modifier.semantics { contentDescription = "Aufnahme stoppen" }, hervorgehoben = true)
            }
        }
        open != null -> Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Mic, null, tint = gold.primaer, modifier = Modifier.size(20.dp))
            Text("Offenes Diktat für „${open?.draftName.orEmpty()}“${if ((open?.missing ?: 0) > 0) " (unvollständig)" else ""} – im Wecker einfügen oder verwerfen.", Modifier.weight(1f).padding(horizontal = 10.dp),
                style = MaterialTheme.typography.bodySmall)
            StillerKnopf("Verwerfen", vm::discardOpenDictation)
        }
    }
}

/** Offers a waiting dictation only in the editor of its original alarm, for a conscious insert. */
@Composable
private fun OffenesDiktatKarte(vm: WeckerViewModel, alarm: Alarm) {
    val open by vm.openDictation.collectAsStateWithLifecycle()
    val dictation = open?.takeIf { it.draftId == alarm.id } ?: return
    GoldKarte(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Offenes Diktat", style = MaterialTheme.typography.titleSmall, color = LocalGold.current.primaer)
            Text("„${dictation.text.take(160)}${if (dictation.text.length > 160) "…" else ""}“", style = MaterialTheme.typography.bodySmall)
            // Never shown as complete when parts were not transcribed.
            if (dictation.missing > 0) StatusZeile(Icons.Default.Warning, "Unvollständig: ${dictation.missing} Abschnitte fehlen.", Semantisch.warnung)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldKnopf("In „Deine Erinnerung“ einfügen", vm::insertOpenDictation)
                StillerKnopf("Verwerfen", vm::discardOpenDictation)
            }
        }
    }
}

/** Bedtime line with a moon symbol, visually separate from the ring and its legend. */
@Composable
private fun SchlafZeile(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Bedtime, null, tint = LocalGold.current.textGedaempft, modifier = Modifier.size(18.dp))
        Text(text, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textPrimaer)
    }
}

/** Optional sleep duration per alarm: Aus/7/8/9 chips plus a 30-minute stepper from 30 minutes to 24 hours. */
@Composable
private fun SchlafdauerEingabe(alarm: Alarm, change: (Alarm) -> Unit) {
    val sleep = alarm.sleepMinutes
    Text("Gewünschte Schlafdauer", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(sleep == 0, { if (sleep != 0) change(alarm.copy(sleepMinutes = 0)) }, { Text("Aus") })
        listOf(7, 8, 9).forEach { hours ->
            FilterChip(sleep == hours * 60, { if (sleep != hours * 60) change(alarm.copy(sleepMinutes = hours * 60)) }, { Text("$hours Std.") })
        }
    }
    if (sleep > 0) Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton({ change(alarm.copy(sleepMinutes = (sleep - Schlaf.STEP).coerceAtLeast(Schlaf.MIN))) }, enabled = sleep > Schlaf.MIN) {
            Icon(Icons.Default.Remove, "Schlafdauer um 30 Minuten verringern", tint = LocalGold.current.primaer)
        }
        Text(Schlaf.dauer(sleep), Modifier.widthIn(min = 72.dp).semantics { contentDescription = "Schlafdauer ${Schlaf.dauer(sleep)}" },
            style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton({ change(alarm.copy(sleepMinutes = (sleep + Schlaf.STEP).coerceAtMost(Schlaf.MAX))) }, enabled = sleep < Schlaf.MAX) {
            Icon(Icons.Default.Add, "Schlafdauer um 30 Minuten erhöhen", tint = LocalGold.current.primaer)
        }
    }
    Text(if (sleep == 0) "Ohne Angabe wird keine Schlafenszeit angezeigt." else "Zeigt die ungefähre Schlafenszeit vor dem nächsten Termin. Kein Tracking, keine Erinnerung.",
        style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
}

/** What saving will do: switched on, and the term it will then ring. Not a claim that it is already planned. */
@Composable
private fun SpeicherVorschau(alarm: Alarm) {
    val minute = rememberNow(60_000)
    val next = remember(alarm, minute) { runCatching { AlarmTime.next(alarm, Instant.ofEpochMilli(minute)) }.getOrNull() }
    if (next != null) Text("Nach dem Speichern eingeschaltet · klingelt dann ${formatAt(next)} (in ${remainingLong(next - minute)})",
        Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textPrimaer,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    if (next != null && alarm.sleepMinutes > 0) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        SchlafZeile("Nach dem Speichern: " + Schlaf.hinweis(next, alarm.sleepMinutes, minute))
    }
    if (next == null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Warning, null, tint = Semantisch.warnung, modifier = Modifier.size(18.dp))
        Text("Nach dem Speichern gäbe es keinen zukünftigen Termin – ändere Datum oder Uhrzeit.", Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.bodySmall, color = Semantisch.warnung)
    }
}

fun remaining(ms: Long): String { val minutes = ZeitRing.ceilMinutes(ms); return "${minutes / 60} Std. ${minutes % 60} Min." }


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
                Text(state.filterNot { it.second }.joinToString(" · ") { "Fehlt: ${it.first}" }.ifBlank { "Genaue Zeit · Sperrbildschirm · Nicht stören · Akku" }, style = MaterialTheme.typography.bodySmall)
            }
            StillerKnopf(if (state.all { it.second }) "Prüfen" else "Beheben", onSettings, hervorgehoben = !state.all { it.second })
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
        "Wecker bei Nicht stören" to (manager.isNotificationPolicyAccessGranted && allowsAlarms && manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_NONE),
        // Samsung puts optimized apps to sleep; unrestricted battery keeps restore and preparation alive.
        "Akku uneingeschränkt" to context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName))
}

@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.frank.wecker

import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import android.Manifest
import android.app.NotificationManager
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
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import kotlinx.coroutines.launch
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.layout
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
import de.frank.wecker.design.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.style.TextAlign
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.unit.Density
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable

@Composable
fun WeckerApp(vm: WeckerViewModel, activity: ComponentActivity) {
    AutomatischeWeckbereitschaft(activity)
    val themeRoh by vm.theme.collectAsStateWithLifecycle()
    // „system“ folgt der Sonne; alles Weitere sieht nur noch hell oder dunkel.
    val theme = wirksamesTheme(themeRoh)
    // Eigene Achse neben Hell/Dunkel und Ausrichtung; lifecycle-bewusst gesammelt.
    val designId by vm.settings.designFlow.collectAsStateWithLifecycle()
    // Kartendeckkraft je Design; der Regler in den Einstellungen wirkt sofort, und beim
    // Umschalten des Designs gilt dessen eigener Wert.
    val deckkraftJeDesign by vm.settings.kartenDeckkraftFlow.collectAsStateWithLifecycle()
    val design = Design.von(designId)
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
    // Sanfter Wechsel von Modus und Design: ein Standbild der bisherigen Ansicht liegt kurz obenauf
    // und blendet aus, während darunter schon alles im neuen Aussehen steht.
    val ansicht = androidx.compose.ui.graphics.rememberGraphicsLayer()
    var standbild by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val standbildDeckung = remember { androidx.compose.animation.core.Animatable(0f) }
    val bildScope = rememberCoroutineScope()
    val mitUeberblendung: (() -> Unit) -> Unit = { aenderung ->
        bildScope.launch {
            standbild = runCatching { ansicht.toImageBitmap() }.getOrNull()
            standbildDeckung.snapTo(1f)
            aenderung()
            standbildDeckung.animateTo(0f, androidx.compose.animation.core.tween(420))
            standbild = null
        }
    }
    WeckerTheme(theme, design, kartenDeckkraftProzent = deckkraftJeDesign[design.id] ?: 100) {
        val gold = LocalGold.current
        BackHandler(page != "alarms") { back() }
        Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()
            .drawWithContent { ansicht.record { this@drawWithContent.drawContent() }; drawLayer(ansicht) }
            .background(gold.hintergrund)) {
            LocalGestalt.current.Hintergrund(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize().imePadding()) {
                DesignKopfleiste(
                    titel = when (page) { "edit" -> if (vm.isNewDraft) "Neuer Wecker" else "Wecker bearbeiten"; "settings" -> "Einstellungen"; else -> "Genialer Wecker" },
                    themeWahl = themeRoh,
                    // Reihum: hell → dunkel → automatisch → hell.
                    aufThemeTipp = { mitUeberblendung { vm.settings.theme = when (themeRoh) { "light" -> "dark"; "dark" -> "system"; else -> "light" } } },
                    // Schaltet reihum durch die vier Designs.
                    aufDesignTipp = { mitUeberblendung {
                        val alle = Design.entries
                        vm.settings.design = alle[(alle.indexOf(design) + 1) % alle.size].id
                        vm.settingsRevision.value++
                    } },
                    aufEinstellungen = if (page == "settings") null else ({ vm.stopPreview(); settingsFrom = page; page = "settings" }),
                    // Derselbe runde Kopfknopf wie rechts — der frühere höhere „‹“-Knopf machte die
                    // Kopfleiste im Editor höher als auf der Liste, und die oberste Linie sprang.
                    voran = if (page != "alarms") ({
                        KopfKnopf(beschreibung = "Zurück zur Weckerliste", aufTipp = { back() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = gold.primaer, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                    }) else null,
                )
                AufnahmeLeiste(vm)
                // Der Bildschirmwechsel hatte bisher die Vorgabe-Überblendung: beide Seiten lagen
                // kurz übereinander und wuchsen dabei. Jetzt trägt die Richtung die Bedeutung —
                // tiefer hinein schiebt von rechts, zurück nach links — und beide Seiten bewegen
                // sich nur so weit, dass die Richtung erkennbar wird. Bei reduzierter Bewegung
                // wird hart umgeschaltet, ohne jede Verschiebung.
                val tiefe = { seite: String -> when (seite) { "alarms" -> 0; "edit" -> 1; else -> 2 } }
                // Außerhalb des Übergangs lesen: der transitionSpec-Block ist kein Composable.
                val wechselReduziert = LocalBewegungReduziert.current
                // Fortschritt und Meldungen schweben über dem Inhalt, statt ihn nach unten zu schieben —
                // das Einfügen einer Zeile oben ließ die ganze Seite bei jedem Knopfdruck ruckeln.
                Box(Modifier.weight(1f)) {
                AnimatedContent(page, Modifier.fillMaxSize(), label = "Bildschirmwechsel",
                    transitionSpec = {
                        if (wechselReduziert) {
                            (androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None)
                        } else {
                            // Weich statt schiebend: Die neue Seite blendet ein und wächst dabei kaum
                            // merklich heran, die alte blendet aus. Ohne Seitwärtsbewegung fällt ein
                            // schwerer erster Aufbau (Editor) nicht mehr als Sprung auf.
                            val vorwaerts = tiefe(targetState) >= tiefe(initialState)
                            val weich = androidx.compose.animation.core.FastOutSlowInEasing
                            (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(320, delayMillis = 40, easing = weich)) +
                                androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(360, easing = weich),
                                    initialScale = if (vorwaerts) 0.97f else 1.02f))
                                .togetherWith(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200, easing = weich)))
                        }
                    }) { current ->
                    // Je Seitenaufbau eine neue Folge: Karten und Abschnitte steigen nacheinander auf.
                    val auftritte = remember(current) { AuftrittsFolge() }
                    CompositionLocalProvider(LocalAuftrittsFolge provides auftritte) {
                    when (current) {
                        "edit" -> draft?.let { alarm -> Column(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f)) { key(alarm.id) { AlarmEditor(vm, alarm, activity) } }
                            Box(
                                Modifier.fillMaxWidth()
                                    .background(gold.flaeche.copy(alpha = .94f))
                                    .drawBehind {
                                        drawLine(
                                            gold.primaer.copy(alpha = .35f),
                                            Offset(0f, 0f),
                                            Offset(size.width, 0f),
                                            1.dp.toPx(),
                                        )
                                    }
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpeicherVorschau(alarm)
                                    GoldKnopf("Wecker speichern", {
                                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else vm.save { page = "alarms" }
                                    }, Modifier.fillMaxWidth(), aktiviert = busy.isBlank() && !recording, hauptKnopf = true)
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
                Column(Modifier.fillMaxWidth().align(Alignment.TopCenter).zIndex(2f)) {
                    androidx.compose.animation.AnimatedVisibility(busy.isNotBlank() || audioBusy.isNotBlank(),
                        enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
                        val form = RoundedCornerShape(16.dp)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                            .shadow(6.dp, form).clip(form).background(gold.flaecheErhoeht).border(1.dp, gold.rahmen, form)
                            .padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = gold.primaer)
                            Text(busy.ifBlank { audioBusy }, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.bodySmall)
                            StillerKnopf("Abbrechen", vm::cancelAction)
                        }
                    }
                // Bestätigungen stehen 3 Sekunden gut sichtbar da und verschwinden dann von selbst —
                // nichts mehr wegklicken. Ein Tipp schließt sie früher.
                // Fehler bleiben stehen, bis man sie antippt — nur Bestätigungen verschwinden von selbst.
                val istFehler = Regex("nicht|fehl|kein |abgebrochen|leer|warte|zuerst", RegexOption.IGNORE_CASE).containsMatchIn(message)
                LaunchedEffect(message) {
                    if (message.isNotBlank() && !istFehler) { delay(3_000); if (vm.message.value == message) vm.message.value = "" }
                }
                androidx.compose.animation.AnimatedVisibility(message.isNotBlank(),
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = .96f),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = .96f)) {
                    var zuletzt by remember { mutableStateOf(message) }
                    if (message.isNotBlank()) zuletzt = message
                    val form = RoundedCornerShape(LocalDesignTokens.current.karteRadius.coerceAtMost(20.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                        .shadow(8.dp, form, ambientColor = gold.primaer, spotColor = gold.primaer)
                        .clip(form).background(gold.flaecheErhoeht)
                        .border(2.dp, if (istFehler) LocalSemantisch.current.warnung else gold.primaer, form)
                        .clickable(onClickLabel = "Meldung schließen") { vm.message.value = "" }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (istFehler) Icons.Default.Warning else Icons.Default.CheckCircle, null,
                            tint = if (istFehler) LocalSemantisch.current.warnung else gold.primaer, modifier = Modifier.size(22.dp))
                        Text(zuletzt, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold, color = gold.textPrimaer)
                    }
                }
                }
                }
            }
        }
        standbild?.let { bild ->
            Image(bild, null, Modifier.fillMaxSize().graphicsLayer { alpha = standbildDeckung.value },
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds)
        }
        }
        if (leaveEditor) DesignTextDialog(
            titel = "Änderungen speichern?",
            text = "Du hast diesen Wecker geändert, aber noch nicht gespeichert.",
            aufSchliessen = { leaveEditor = false },
            bestaetigung = { GoldKnopf("Speichern", { leaveEditor = false; vm.save { page = "alarms" } }) },
            abbruch = {
                StillerKnopf("Verwerfen", { leaveEditor = false; vm.closeEditor(); page = "alarms" })
                StillerKnopf("Weiter bearbeiten", { leaveEditor = false })
            },
        )
        pendingOpen?.let { open -> DesignTextDialog(
            titel = "Ungespeicherten Entwurf verwerfen?",
            text = "„${draft?.name.orEmpty()}“ hat noch ungespeicherte Änderungen.",
            aufSchliessen = { pendingOpen = null },
            bestaetigung = { GoldKnopf("Entwurf fortsetzen", { pendingOpen = null; page = "edit" }) },
            abbruch = { StillerKnopf("Verwerfen", { pendingOpen = null; vm.closeEditor(); open(); page = "edit" }) },
        ) }
        delete?.let { alarm -> Confirm("Wecker löschen?", "„${alarm.name}“ wird entfernt.", "Wecker löschen", {
            vm.delete(alarm); delete = null
        }, { delete = null }) }
    }
}

@Composable
private fun AlarmList(gespeichert: List<Alarm>, vm: WeckerViewModel, onNew: () -> Unit, onEdit: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit, onSettings: () -> Unit, openDraft: Alarm?, onResumeDraft: () -> Unit) {
    val gold = LocalGold.current
    // Nur für diese Listenansicht merken: neue Wecker und eine neu geöffnete Liste sind kompakt.
    // Saveable, damit das Auf- und Zuklappen des Geräts (Activity-Neuerstellung) den Stand nicht verwirft;
    // beim Verlassen der Liste wird der Eintrag verworfen, die Liste beginnt also weiterhin zugeklappt.
    var expandedIds by rememberSaveable(saver = androidx.compose.runtime.saveable.listSaver<MutableState<Set<String>>, String>(
        save = { it.value.toList() }, restore = { mutableStateOf(it.toSet()) })) { mutableStateOf(emptySet<String>()) }
    val issues by vm.store.issues.collectAsState()
    val now = rememberNow(60_000)
    // Nur die Anzeige ist nach Uhrzeit sortiert (früh bis spät); gespeichert bleibt die Reihenfolge unverändert.
    // Mit jeder Minute neu, damit Sonnenwecker mit wandernder Uhrzeit an der richtigen Stelle bleiben.
    val alarms = remember(gespeichert, now) { Alarm.nachUhrzeit(gespeichert) }
    // Der Sprung nach dem Speichern sucht die Karte in der aktuellen Liste, nicht in der beim Start des Effekts.
    val aktuelleListe by androidx.compose.runtime.rememberUpdatedState(alarms)
    val nextRegular = alarms.mapNotNull { alarm -> alarm.nextAt.takeIf { alarm.enabled && it > now } }.minOrNull()
    val nextSnooze = alarms.mapNotNull { alarm -> alarm.snoozeUntil.takeIf { it > now } }.minOrNull()
    val next = listOfNotNull(nextRegular, nextSnooze).minOrNull()
    val nextIsSnooze = next != null && next == nextSnooze
    // Derselbe Wecker wie bisher, nur als Objekt statt nur mit Namen — dadurch kann der Hero ihn
    // direkt öffnen. Berechnung und Vorrang des Schlummerns bleiben unberührt.
    val nextAlarm = next?.let { target ->
        if (nextIsSnooze) alarms.firstOrNull { it.snoozeUntil == target }
        else alarms.firstOrNull { it.enabled && it.nextAt == target }
    }
    val nextName = nextAlarm?.name?.takeIf { it.isNotBlank() }
    // Einmal abfragen und an Hero und Bereitschaftskarte weitergeben: Zwei Aufrufer hätten zwei
    // Zwei-Sekunden-Schleifen über NotificationManager, PowerManager und AlarmScheduler bedeutet.
    val bereitschaft = rememberReadiness()
    // Nur sinnvoll, solange der nächste Termin tatsächlich ein Schlummern ist.
    val aufSchlummernBeenden: () -> Unit = { nextAlarm?.takeIf { nextIsSnooze }?.let { vm.endSnooze(it) } }
    val gridState = rememberLazyGridState()
    val saved by vm.lastSaved.collectAsStateWithLifecycle()
    val resumed = rememberResumed()
    val reducedMotion = LocalBewegungReduziert.current
    val glow = remember { androidx.compose.animation.core.Animatable(0f) }
    var glowFor by remember { mutableStateOf<SavedEvent?>(null) }
    LaunchedEffect(saved, resumed, reducedMotion) {
        val event = saved ?: return@LaunchedEffect
        if (!resumed) return@LaunchedEffect
        val alarmIndex = aktuelleListe.indexOfFirst { it.id == event.id }
        if (alarmIndex >= 0) {
            // Der Kopf ist seit der Fixierung KEIN Listeneintrag mehr — früher stand hier deshalb
            // eine 2 (Kopf + Bereitschaft). Jetzt zählt nur noch die Bereitschaftskarte, die
            // immer ein Eintrag bleibt (sie blendet nur ihren Inhalt aus), plus der Entwurf.
            // Die Leerzustandskarte gibt es nur ohne Wecker und spielt hier deshalb keine Rolle.
            val index = VORLAUF_EINTRAEGE + (if (openDraft != null) 1 else 0) + alarmIndex
            val visible = gridState.layoutInfo.visibleItemsInfo.any { it.key == event.id }
            // Never fight the user: no scroll while they scroll, and their gesture may interrupt ours.
            if (!visible && !gridState.isScrollInProgress) try {
                if (reducedMotion) gridState.scrollToItem(index) else gridState.animateScrollToItem(index)
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (!currentCoroutineContext().isActive) throw e
            }
            glowFor = event
            if (reducedMotion) { glow.snapTo(.6f); delay(2000); glow.snapTo(0f) }
            else { glow.snapTo(0f); glow.animateTo(.6f, androidx.compose.animation.core.tween(300)); glow.animateTo(0f, androidx.compose.animation.core.tween(600)) }
            if (glowFor == event) glowFor = null
        }
        vm.consumeSaved(event.generation)
    }
    BoxWithConstraints {
        val breite = maxWidth
        // Morgenruhe bleibt einspaltig, auch auf dem großen Display: die Tagesachse ist ein
        // durchgehender Strang und darf nicht in zwei Spalten zerfallen. Zwischen den Stationen
        // entfällt der Abstand, damit die Linie nicht unterbrochen wird.
        val achsenDesign = LocalDesignTokens.current.design == Design.MORGENRUHE
        // Der Kopf scrollt nicht mehr mit. Weil er dadurch dauerhaft Platz kostet, ist er
        // kompakter als die frühere Scrollkarte und schrumpft bei wenig Höhe weiter — im
        // Querformat darf er die Liste nicht verdrängen, sondern gibt selbst nach.
        // Die Schwelle für WEIT lag bei 640 dp und war damit zu niedrig: Ein gewöhnliches
        // Hochformat-Telefon hat rund 750 dp und bekam deshalb immer die größte Fassung. Beim
        // Traumraum belegte der feststehende Kopf dadurch nachgerechnete 39 % der Höhe — für
        // einen Block, der nie wegscrollt, zu viel. WEIT gilt jetzt erst ab echtem Tablettmaß;
        // Telefon und Foldable, auch aufgeklappt, liegen darunter und bekommen die mittlere
        // Fassung mit kleinerem Motiv und kleinerer Uhr (beim Traumraum rund 32 %).
        // Die Schriftskalierung geht mit in die Rechnung: Bei doppelt großer Systemschrift wächst
        // jede Zeile mit, der verfügbare Platz aber nicht. Ohne diesen Teiler bliebe der Hero auf
        // der großen Stufe stehen und fräße die halbe Seite.
        val schriftFaktor = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val nutzHoehe = maxHeight / schriftFaktor
        val stufe = when {
            nutzHoehe < 400.dp -> KopfStufe.SCHMAL
            nutzHoehe < 820.dp -> KopfStufe.MITTEL
            else -> KopfStufe.WEIT
        }
        val aktivAnzahl = alarms.count { it.enabled }
        val heroDaten = HeroDaten(
            now = now, next = next, nextIsSnooze = nextIsSnooze, nextAlarm = nextAlarm,
            nextName = nextName, bereit = bereitschaft.all { it.second },
            offen = bereitschaft.count { !it.second }, stufe = stufe, breite = breite,
            anzahl = alarms.size, aktivAnzahl = aktivAnzahl,
        )
        Column(Modifier.fillMaxSize()) {
            // Der feststehende Hero. Er sitzt außerhalb des Rasters, damit die Weckerliste
            // darunter scrollt, ohne ihn mitzunehmen. `zIndex` hebt ihn über das Raster: Sonst
            // zeichnet die Liste als späteres Geschwister über seinen Schatten, und die Karten
            // würden auf ihm liegen statt unter ihm durchzugleiten.
            Box(
                Modifier.fillMaxWidth().zIndex(1f)
                    // Traumraum rückt minimal näher an die Kopfleiste.
                    // Morgenruhe führt die Tagesachse selbst bis an die Liste, daher dort kein Abstand.
                    .padding(start = 16.dp, end = 16.dp, top = seitenAbstandOben(),
                        bottom = if (LocalDesignTokens.current.design == Design.MORGENRUHE) 0.dp else 14.dp),
            ) {
                when (LocalDesignTokens.current.design) {
                    Design.TRAUMRAUM -> TraumraumHero(heroDaten, onNew, onEdit, onSettings, aufSchlummernBeenden)
                    Design.ORBIT -> OrbitHero(heroDaten, onNew, onEdit, onSettings, aufSchlummernBeenden)
                    Design.MORGENRUHE -> MorgenruheHero(heroDaten, onNew, onEdit, onSettings, aufSchlummernBeenden)
                    else -> SchlichtHero(heroDaten, onNew, onEdit, onSettings, aufSchlummernBeenden)
                }
            }
            // Immer untereinander, auch aufgeklappt — in allen Designs gleich.
            LazyVerticalGrid(columns = GridCells.Fixed(1), state = gridState,
                contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 32.dp),
                verticalArrangement = Arrangement.spacedBy(if (achsenDesign) 0.dp else 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Bleibt immer ein Eintrag, auch wenn sie nichts zeigt — sonst verschöbe sich der
                // Sprungindex nach dem Speichern, sobald alle Freigaben erteilt sind.
                item(span = { GridItemSpan(maxLineSpan) }) { ReadinessCard(onSettings, hideWhenReady = true, zustand = bereitschaft) }
                if (openDraft != null) item(span = { GridItemSpan(maxLineSpan) }) {
                    LocalGestalt.current.Flaeche(Modifier, erhoeht = false) {
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
                    LeererStart(now, onNew)
                }
                items(alarms, key = { it.id }) { alarm ->
                    WeckerKarte(
                        alarm = alarm, vm = vm, now = now, hinweis = issues[alarm.id],
                        expanded = alarm.id in expandedIds,
                        istNaechster = nextAlarm?.id == alarm.id,
                        aufKlappen = { expandedIds = if (alarm.id in expandedIds) expandedIds - alarm.id else expandedIds + alarm.id },
                        onEdit = onEdit, onDelete = onDelete,
                        glowAlpha = if (glowFor?.id == alarm.id) glow.value else 0f,
                        resumed = resumed, reducedMotion = reducedMotion,
                        modifier = Modifier.animateItem().auftritt(),
                    )
                }
            }
        }
    }
}

/** Wie viel Platz der feststehende Hero bekommt. */
enum class KopfStufe { SCHMAL, MITTEL, WEIT }

/**
 * Wie viele Einträge vor dem ersten Wecker im Raster stehen. Aktuell genau die
 * Bereitschaftskarte; der Entwurf kommt im Aufrufer dazu. Diese Zahl und der Rasteraufbau
 * gehören zusammen — wer oben einen Eintrag ergänzt, muss sie hier mitziehen.
 */
private const val VORLAUF_EINTRAEGE = 1

/**
 * Alles, was die vier Hero-Bereiche zeigen — einmal berechnet, dann nur noch angeordnet.
 *
 * Vorher reichte ein `terminBlock`-Lambda eine fertige Textfolge durch und zwang damit allen
 * Designs dieselbe Reihenfolge auf. Mit den Rohdaten setzt jedes Design seine eigene Hierarchie:
 * Morgenruhe führt mit dem Datum, Orbit mit einem Instrumentenkopf, Schlicht mit der Uhr.
 */
@Immutable
data class HeroDaten(
    val now: Long,
    val next: Long?,
    val nextIsSnooze: Boolean,
    val nextAlarm: Alarm?,
    val nextName: String?,
    val bereit: Boolean,
    val offen: Int,
    val stufe: KopfStufe,
    val breite: androidx.compose.ui.unit.Dp,
    /** Wie viele Wecker es überhaupt gibt – für den Leerzustand „noch keiner“ gegenüber „alle aus“. */
    val anzahl: Int = 0,
    /** Wie viele Wecker aktuell eingeschaltet sind – auf einen Blick im Hero sichtbar. */
    val aktivAnzahl: Int = 0,
) {
    val hatTermin: Boolean get() = next != null
    val aktivText: String get() = when {
        anzahl == 0 -> "Kein Wecker"
        aktivAnzahl == 0 -> "0 von $anzahl aktiv"
        aktivAnzahl == anzahl -> if (anzahl == 1) "1 Wecker aktiv" else "Alle $anzahl aktiv"
        else -> "$aktivAnzahl von $anzahl aktiv"
    }
    /** Leerzustand in derselben Dreizeilen-Form wie ein Termin: kurz, jede Zeile passt in eine Zeile. */
    val leerTitel: String get() = "Kein Wecker aktiv"
    val leerGrund: String get() = when (anzahl) { 0 -> "Noch keiner angelegt"; 1 -> "Dein Wecker ist aus"; else -> "Alle $anzahl Wecker sind aus" }
    val leerHilfe: String get() = if (anzahl == 0) "Tippe auf „Neuer Wecker“" else "Unten einfach einschalten"
    val schmal: Boolean get() = stufe == KopfStufe.SCHMAL
    val weit: Boolean get() = stufe == KopfStufe.WEIT
    /** Die Restzeit als Anteil eines Tages — für lineare Anzeigen. Über 24 Stunden ist voll. */
    fun tagesAnteil(): Float =
        next?.let { ((it - now) / 86_400_000f).coerceIn(0f, 1f) } ?: 0f
}

/** Die Schriftgröße der großen Hero-Uhr je Platzstufe. */
private fun uhrGroesse(stufe: KopfStufe): androidx.compose.ui.unit.TextUnit = when (stufe) {
    KopfStufe.SCHMAL -> 34.sp
    KopfStufe.MITTEL -> 52.sp
    KopfStufe.WEIT -> 56.sp
}

/**
 * Die größte Schriftgröße, mit der die Uhrzeit in [maxBreite] noch vollständig passt.
 *
 * `softWrap = false` schneidet stillschweigend ab, wenn der Platz nicht reicht — und genau das
 * geschah bei schmalen Geräten: Die Textspalte war rechnerisch 138 dp breit, die Uhr brauchte bei
 * 52 sp und hochgesetzter Systemschrift deutlich mehr. Statt zu raten wird hier gemessen, wie es
 * der Weckbildschirm für seine große Uhr schon tut. Gemessen wird mit dem breitesten Ziffernpaar,
 * damit die Größe über den Tag hinweg stabil bleibt und die Zahl nicht bei jedem Minutenwechsel
 * springt.
 */
@Composable
private fun passendeUhrGroesse(
    maxBreite: androidx.compose.ui.unit.Dp,
    basis: androidx.compose.ui.unit.TextUnit,
    familie: androidx.compose.ui.text.font.FontFamily,
    gewicht: FontWeight?,
): androidx.compose.ui.unit.TextUnit {
    val messer = androidx.compose.ui.text.rememberTextMeasurer()
    val dichte = LocalDensity.current
    return remember(messer, maxBreite, basis, familie, gewicht, dichte.density, dichte.fontScale) {
        val grenze = with(dichte) { maxBreite.toPx() }
        if (grenze <= 0f) return@remember basis
        val stil = androidx.compose.ui.text.TextStyle(fontFamily = familie, fontWeight = gewicht)
        fun breiteBei(sp: Float) = messer.measure(
            "00:00", stil.copy(fontSize = sp.sp), maxLines = 1, softWrap = false,
        ).size.width.toFloat()
        val gewuenscht = basis.value
        if (breiteBei(gewuenscht) <= grenze) return@remember basis
        // Halbierung zwischen einer sicher passenden Untergrenze und der zu großen Wunschgröße.
        var passt = 22f
        var zuGross = gewuenscht
        if (breiteBei(passt) > grenze) return@remember passt.sp
        repeat(7) {
            val mitte = (passt + zuGross) / 2f
            if (breiteBei(mitte) <= grenze) passt = mitte else zuGross = mitte
        }
        passt.sp
    }
}

/**
 * Die Uhr wächst mit der Systemschrift nur begrenzt mit: Bei doppelter Schrift würde aus 52 sp
 * eine 120 dp hohe Zeile und der feststehende Hero verschlänge die halbe Seite. Termin, Name und
 * Restzeit skalieren dagegen voll mit — sie muss man lesen können.
 */
@Composable
private fun GedeckelteSchrift(inhalt: @Composable () -> Unit) {
    val dichte = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(dichte.density, dichte.fontScale.coerceAtMost(1.2f)),
        content = inhalt,
    )
}

/** Das Datum als ruhige Kopfzeile — heute stand nirgends, welcher Tag überhaupt ist. */
private fun datumsZeile(now: Long): String = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", java.util.Locale.GERMAN))

/** Kurzfassung für enge Anordnungen. */
private fun datumKurz(now: Long): String = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("EE dd.MM.", java.util.Locale.GERMAN))

/**
 * Der Bereitschaftshinweis als eine Zeile. Er beantwortet die Frage, die bei einem Wecker über
 * allem steht: Wird er überhaupt klingeln? Die ausführlichen Angaben bleiben in der
 * Bereitschaftskarte, die ohnehin nur erscheint, wenn etwas fehlt.
 */
@Composable
private fun BereitZeile(
    daten: HeroDaten, aufEinstellungen: () -> Unit,
    farbeBereit: androidx.compose.ui.graphics.Color, farbeOffen: androidx.compose.ui.graphics.Color,
    stil: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelMedium,
) {
    val text = if (daten.bereit) "Weckbereit"
    else "${daten.offen} ${if (daten.offen == 1) "Freigabe fehlt" else "Freigaben fehlen"}"
    Row(
        Modifier
            .then(if (daten.bereit) Modifier else Modifier.clickable(
                onClickLabel = "Weckbereitschaft einrichten", onClick = aufEinstellungen))
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (daten.bereit) Icons.Default.VerifiedUser else Icons.Default.NotificationsActive,
            null, tint = if (daten.bereit) farbeBereit else farbeOffen,
            modifier = Modifier.size(15.dp),
        )
        // Ohne Kürzungszeichen und ohne Breitenvorgabe schnitt „2 Freigaben fehlen" bei
        // vergrößerter Systemschrift still ab — man sah nicht einmal, dass etwas fehlt.
        Text(text, Modifier.weight(1f, fill = false).padding(start = 6.dp), style = stil,
            color = if (daten.bereit) farbeBereit else farbeOffen,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Der gemeinsame Terminblock: Termin, Name und Restzeit als **eine** antippbare Gruppe, die den
 * nächsten Wecker öffnet. Das ist die naheliegendste Handlung nach dem Blick auf die Uhr; bisher
 * musste man ihn in der Liste suchen. Die Zeilenzahl ist in jedem Zustand gleich, damit der Hero
 * nicht springt, wenn ein Wecker aus- oder eingeschaltet wird.
 */
@Composable
private fun TerminGruppe(
    daten: HeroDaten, aufOeffnen: () -> Unit,
    textFarbe: androidx.compose.ui.graphics.Color,
    gedaempft: androidx.compose.ui.graphics.Color,
    fuehrung: androidx.compose.ui.graphics.Color,
    zeigePfeil: Boolean = true,
    ausrichtung: Alignment.Horizontal = Alignment.Start,
    punkt: Boolean = true,
) {
    val oeffenbar = daten.nextAlarm != null
    Column(
        Modifier.fillMaxWidth()
            .then(if (oeffenbar) Modifier.clickable(
                onClickLabel = "Nächsten Wecker öffnen", onClick = aufOeffnen) else Modifier),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalAlignment = ausrichtung,
    ) {
        if (daten.next == null) {
            // Dieselben drei Zeilen wie bei einem Termin (Titel, Name, Restzeit): gleiche Höhe, kein Umbruch.
            Text(daten.leerTitel, style = MaterialTheme.typography.bodyMedium, color = gedaempft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(daten.leerGrund, style = MaterialTheme.typography.titleSmall, color = textFarbe, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(daten.leerHilfe, style = MaterialTheme.typography.bodyMedium, color = fuehrung, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else {
            TerminZeile(daten.now, daten.next, daten.nextIsSnooze, punkt)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(daten.nextName ?: "Wecker", Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleSmall, color = textFarbe,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (zeigePfeil && oeffenbar) Icon(
                    Icons.Default.ChevronRight, null, tint = gedaempft,
                    modifier = Modifier.padding(start = 2.dp).size(16.dp),
                )
            }
            Text("in ${remainingLong(daten.next - daten.now)}",
                style = MaterialTheme.typography.bodyMedium, color = fuehrung,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Die Aktionszeile: die Hauptaktion als beschrifteter Knopf statt als isoliertes Pluszeichen,
 * daneben genau eine zweitrangige Handlung, die zum Zustand passt — beim Schlummern das Beenden,
 * sonst das Öffnen des nächsten Weckers.
 */
@Composable
private fun HeroAktionen(
    daten: HeroDaten, aufNeu: () -> Unit, aufOeffnen: () -> Unit, aufSchlummernBeenden: () -> Unit,
    knopfText: String = "Neuer Wecker",
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GoldKnopf(knopfText, aufNeu, hauptKnopf = true, beschreibung = "Neuen Wecker anlegen",
            symbol = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) })
        when {
            daten.nextIsSnooze -> StillerKnopf("Schlummern beenden", aufSchlummernBeenden, hervorgehoben = true)
            // Gleich groß wie der Hauptknopf: derselbe Körper, nur nicht als Hauptaktion betont.
            daten.nextAlarm != null -> GoldKnopf("Öffnen", aufOeffnen, beschreibung = "Nächsten Wecker öffnen")
        }
    }
}

/**
 * **Schlicht — die Uhr im Glasbett.**
 *
 * Der bisherige Kopf war eine enge Zeile aus Ring, Uhr, Text und einem nackten Pluszeichen und
 * damit kleiner als eine geschlossene Weckerkarte. Jetzt ist er das, was er sein soll: das
 * Zentrum der Seite. Der Ring sitzt in einer vertieften Mulde, die Karte trägt den plastischen
 * Goldverlauf mit Lichtkante und einem statischen Glanzbogen, und sie schwebt als einziges
 * Element über der Liste.
 *
 * Die Goldfarben selbst sind unverändert — es ändern sich nur Anordnung, Tiefe und Licht.
 */
@Composable
private fun SchlichtHero(
    daten: HeroDaten, aufNeu: () -> Unit, aufOeffnen: (Alarm) -> Unit,
    aufEinstellungen: () -> Unit, aufSchlummernBeenden: () -> Unit,
) {
    val gold = LocalGold.current
    val semantisch = LocalSemantisch.current
    val oeffnen = { daten.nextAlarm?.let(aufOeffnen); Unit }
    if (daten.schmal) {
        SchmalerHero(daten, aufNeu, oeffnen, aufSchlummernBeenden)
        return
    }
    // Gerechnet wird mit der **echten** Innenbreite, nicht mit der Fensterbreite: Von dieser
    // gehen außen 2 × 16 dp und innerhalb der Karte noch einmal 2 × 16 dp ab. Vorher floss die
    // volle Fensterbreite in die Ringgröße ein — bei 360 dp Gerätebreite blieben der Textspalte
    // dadurch 138 statt der zugesicherten 186 dp, und die Uhr wurde abgeschnitten.
    val innen = daten.breite - HERO_AUSSEN * 2 - KARTE_INNEN * 2
    // Die Aktionen stehen jetzt immer unter dem Ganzen und über die volle Breite. Dadurch muss
    // die Textspalte nur noch Termin, Name und Restzeit tragen, und der Ring darf größer bleiben,
    // statt bei schmalen Geräten auf Abzeichengröße zu schrumpfen.
    val ringRoh = innen - SPALTEN_ABSTAND - textMindest() - RINGBETT_RAND
    // Unter 96 dp wäre der Ring kein Instrument mehr, sondern ein Abzeichen. Auf sehr schmalen
    // Geräten entfällt er deshalb ganz und die Uhr bekommt die volle Breite — die Anordnung
    // wechselt, statt beide Teile unlesbar zu quetschen.
    val zeigtRing = ringRoh >= 96.dp
    val ring = ringRoh.coerceIn(96.dp, if (daten.weit) 144.dp else 128.dp)
    val textBreite = if (zeigtRing) innen - SPALTEN_ABSTAND - (ring + RINGBETT_RAND) else innen
    HeroKarte {
        Column(Modifier.fillMaxWidth().padding(KARTE_INNEN), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Kompakte obere Statuszeile: links aktive Wecker, rechts Weckbereitschaft — dadurch
            // behalten die Hauptaktionen unten die volle Breite und stapeln sich nicht mehr unnötig.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                HeroStatusPille(
                    text = daten.aktivText,
                    aktiv = daten.aktivAnzahl > 0,
                    farbe = gold.primaer,
                    textFarbe = gold.textPrimaer,
                )
                Spacer(Modifier.width(8.dp))
                BereitZeile(daten, aufEinstellungen, semantisch.erfolg, semantisch.warnung)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SPALTEN_ABSTAND),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Vertiefte 3D-Mulde mit warmem Messing-Lichthof hinter dem 3D-Wecker.
                val klingeln = rememberGelegentlichesKlingeln()
                val deckkraft = LocalKartenDeckkraft.current
                if (zeigtRing) Box(
                    Modifier.size(ring + RINGBETT_RAND)
                        .drawBehind {
                            if (deckkraft > 0f) {
                                val r = size.minDimension / 2f
                                drawCircle(
                                    Brush.radialGradient(
                                        listOf(
                                            gold.primaer.copy(alpha = (if (gold.istDunkel) 0.18f else 0.14f) * deckkraft),
                                            androidx.compose.ui.graphics.Color.Transparent,
                                        ),
                                        center = center,
                                        radius = r * 1.06f,
                                    ),
                                    radius = r * 1.06f,
                                )
                                drawCircle(
                                    Brush.radialGradient(
                                        listOf(
                                            androidx.compose.ui.graphics.Color.Black.copy(alpha = (if (gold.istDunkel) 0.34f else 0.10f) * deckkraft),
                                            androidx.compose.ui.graphics.Color.Transparent,
                                        ),
                                        center = center + Offset(0f, 3.dp.toPx()),
                                        radius = r * 0.96f,
                                    ),
                                    radius = r * 0.96f,
                                )
                            }
                        }
                        .graphicsLayer {
                            rotationZ = klingeln.value
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.85f)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Wecker3D(Modifier.matchParentSize(), gehaeuse = ring + 14.dp)
                    RestzeitRing(daten.now, daten.next, daten.nextIsSnooze, Modifier.size(ring), zifferblatt = true)
                    EdleZeiger(Modifier.size(ring))
                    GlasKuppel(Modifier.size(ring))
                }
                // Rechtsbündig: Datum, Uhr und Termin enden an derselben Kante wie „Weckbereit“.
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.End) {
                    // Oben die Uhr, darunter Datum und Termin in derselben Schriftgröße, ohne Punkt davor.
                    GedeckelteSchrift {
                        // Die Größe wird gegen die tatsächlich verbleibende Spaltenbreite gemessen.
                        val groesse = passendeUhrGroesse(textBreite, uhrGroesse(daten.stufe),
                            zahlSchrift(), zahlGewicht())
                        // Die Ziffern rollen beim Minutenwechsel, und kurz nach dem Lichtlauf über
                        // das Glas zieht ein heller Goldglanz durch die Zahlen selbst.
                        RollendeUhr(
                            formatClock(daten.now),
                            androidx.compose.ui.text.TextStyle(fontFamily = zahlSchrift(), fontWeight = zahlGewicht(),
                                fontSize = groesse, color = gold.primaer),
                            Modifier.glanzLauf(staerke = if (gold.istDunkel) 0.75f else 0.55f,
                                farbe = if (gold.istDunkel) androidx.compose.ui.graphics.Color(0xFFFFF3D1) else gold.akzentWarm,
                                nurSchrift = true, versatzMs = 350L),
                            ueberschrift = true,
                        )
                    }
                    // Auf ~384 dp (S24) passte „Sonntag, 27. September“ nicht in eine Zeile: umbrechen statt kürzen.
                    Text(datumsZeile(daten.now),
                        style = MaterialTheme.typography.bodyMedium, color = gold.textGedaempft,
                        textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    TerminGruppe(daten, oeffnen, gold.textPrimaer, gold.textGedaempft, gold.primaer,
                        ausrichtung = Alignment.End, punkt = false)
                }
            }
            // Volle Breite für die Hauptaktionen: „Neuer Wecker“ und „Öffnen“ sitzen harmonisch nebeneinander.
            HeroAktionen(daten, aufNeu, oeffnen, aufSchlummernBeenden)
        }
    }
}

/** Kompakte Status-Pille für aktive Wecker im oberen Designbereich. */
@Composable
private fun HeroStatusPille(
    text: String,
    aktiv: Boolean,
    farbe: androidx.compose.ui.graphics.Color,
    textFarbe: androidx.compose.ui.graphics.Color,
    schriftFamilie: androidx.compose.ui.text.font.FontFamily? = null,
) {
    val form = RoundedCornerShape(LocalDesignTokens.current.chipRadius)
    Row(
        Modifier.clip(form)
            .background(farbe.copy(alpha = if (aktiv) 0.16f else 0.08f))
            .border(1.dp, farbe.copy(alpha = if (aktiv) 0.42f else 0.22f), form)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp).background(if (aktiv) farbe else textFarbe.copy(alpha = 0.4f), CircleShape))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = schriftFamilie ?: MaterialTheme.typography.labelSmall.fontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (aktiv) farbe else textFarbe.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Außenabstand der Hero-Box links und rechts. */
private val HERO_AUSSEN = 16.dp
/** Innenabstand innerhalb der Hero-Karte. */
private val KARTE_INNEN = 16.dp
/** Abstand zwischen Ring-/Motivspalte und Textspalte. */
private val SPALTEN_ABSTAND = 14.dp
/** Der Rand des vertieften Ringbetts rund um den Ring. */
private val RINGBETT_RAND = 16.dp
/**
 * Was die Textspalte mindestens braucht, damit Termin, Name und Restzeit lesbar bleiben.
 * Die längste Zeile ist der Termin („● Wecker 07:00 · morgen"); die Aktionen stehen inzwischen
 * darunter und zählen hier nicht mehr mit.
 */
private val TEXT_MINDEST = 150.dp

/**
 * Die Mindestbreite wächst mit der Systemschrift: Bei großer Schrift braucht dieselbe Zeile
 * mehr Platz, der Bildschirm wird aber nicht breiter. Gedeckelt bei Faktor 1,5, damit der Ring
 * nicht schon bei mäßig vergrößerter Schrift ganz verschwindet.
 */
@Composable
private fun textMindest(): androidx.compose.ui.unit.Dp =
    TEXT_MINDEST * LocalDensity.current.fontScale.coerceIn(1f, 1.5f)

/**
 * Schlichts Heroträger: geschliffenes 3D-Glasgehäuse mit mehrschichtiger Tiefe, oberer
 * Gold-Lichtkante, sanftem Reflex und edler Fase — voll steuerbar über die Kartendeckkraft.
 */
@Composable
private fun HeroKarte(inhalt: @Composable () -> Unit) {
    val gold = LocalGold.current
    val form = RoundedCornerShape(LocalDesignTokens.current.karteRadius)
    val flaeche = gold.heroGrund
    val deckkraft = LocalKartenDeckkraft.current
    Box(
        Modifier.fillMaxWidth()
            .deckSchatten(gold.primaer, Hoehe.schwebendeLeiste, form, deckkraft)
            .clip(form)
            .background(
                Brush.verticalGradient(
                    listOf(
                        flaeche.heller(if (gold.istDunkel) 0.11f else 0.05f),
                        flaeche,
                        flaeche.dunkler(if (gold.istDunkel) 0.16f else 0.07f),
                    ).map { it.copy(alpha = it.alpha * deckkraft) },
                ),
            )
            .drawBehind {
                if (deckkraft > 0f) {
                    // Subtiles oberes Lichtband für skulpturale 3D-Glaskrone.
                    drawRect(
                        Brush.verticalGradient(
                            listOf(
                                gold.primaer.copy(alpha = (if (gold.istDunkel) 0.12f else 0.09f) * deckkraft),
                                androidx.compose.ui.graphics.Color.Transparent,
                            ),
                            startY = 0f,
                            endY = size.height * 0.34f,
                        ),
                    )
                }
            }
            .glanzBogen(deckung = (if (gold.istDunkel) 0.09f else 0.18f) * deckkraft)
            .heroFase3D(
                form = form,
                kante = gold.primaer.copy(alpha = if (gold.istDunkel) 0.34f else 0.42f),
                licht = if (gold.istDunkel) gold.primaer.heller(0.35f) else androidx.compose.ui.graphics.Color.White,
                lichtAlpha = if (gold.istDunkel) 0.42f else 0.72f,
                schattenAlpha = if (gold.istDunkel) 0.55f else 0.24f,
                deckkraft = deckkraft,
            )
            .glanzLauf(staerke = (if (gold.istDunkel) 0.10f else 0.42f) * deckkraft),
    ) {
        // Die Funkelsterne gehören zur Karte, nicht zum Inhalt: Sie verblassen mit ihr.
        HeroDeko(Modifier.matchParentSize().graphicsLayer { alpha = deckkraft })
        inhalt()
    }
}

/**
 * Die flache Fassung für wenig Höhe — Querformat und sehr große Systemschrift. Sie zeigt
 * dieselben Angaben in einer Zeile, statt Inhalte abzuschneiden oder den Hero die ganze
 * Seite füllen zu lassen.
 */
@Composable
private fun SchmalerHero(
    daten: HeroDaten, aufNeu: () -> Unit, aufOeffnen: () -> Unit, aufSchlummernBeenden: () -> Unit,
) {
    val gold = LocalGold.current
    LocalGestalt.current.Flaeche(Modifier.fillMaxWidth(), erhoeht = true) {
        // Vorher standen Uhr, Terminspalte und die Aktionsreihe in **einer** Zeile, wobei nur die
        // Terminspalte gewichtet war. Uhr und die lange Aktion „Schlummern beenden" nahmen sich
        // die Breite zuerst; auf 320 bis 360 dp blieb vom Termin fast nichts übrig. Jetzt stehen
        // die Aktionen unter der Zeile, und der Termin behält die Breite, die er braucht.
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GedeckelteSchrift {
                    // Auch hier gegen den echten Platz gemessen: Die Uhr darf die Terminspalte
                    // nicht auffressen, sondern gibt selbst nach.
                    val groesse = passendeUhrGroesse(
                        daten.breite - HERO_AUSSEN * 2 - 28.dp - textMindest(),
                        uhrGroesse(daten.stufe), zahlSchrift(), zahlGewicht(),
                    )
                    RollendeUhr(formatClock(daten.now),
                        androidx.compose.ui.text.TextStyle(fontFamily = zahlSchrift(), fontWeight = zahlGewicht(),
                            fontSize = groesse, color = gold.primaer))
                }
                Column(
                    Modifier.weight(1f).then(
                        if (daten.nextAlarm != null) Modifier.clickable(
                            onClickLabel = "Nächsten Wecker öffnen", onClick = aufOeffnen) else Modifier),
                ) {
                    Text(
                        if (daten.next == null) "${daten.leerGrund} · ${daten.leerHilfe}"
                        else "${terminAnzeige(daten.now, daten.next).einzeilig}${daten.nextName?.let { " · $it" } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium, color = gold.textPrimaer,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (daten.next != null) Text("in ${remainingLong(daten.next - daten.now)}",
                        style = MaterialTheme.typography.bodySmall, color = gold.primaer,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            // Eigene Zeile für die Handlungen. „Schlummern beenden" ist bewusst ausgeschrieben —
            // die Handlung entfernt den Schlummertermin und plant regulär neu, sie weckt nicht
            // sofort. Eine Kurzform wie „Wecken" wäre irreführend und könnte den Schlummeralarm
            // kosten. Bei sehr schmalen Geräten bricht die Reihe um, statt zu drängeln.
            // Bewusst eine Row mit Gewicht statt einer FlowRow: Diese Fassung greift auch
            // hochkant bei sehr großer Systemschrift, und dort ist „Schlummern beenden" breiter
            // als die ganze Karte. In der FlowRow liefe der Knopftext über, weil er ohne
            // Breitenvorgabe misst. Mit `weight(1f, fill = false)` bekommt er eine Obergrenze
            // und bricht innerhalb des Knopfes auf zwei Zeilen um.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GoldKnopf("Neuer Wecker", aufNeu, hauptKnopf = true, beschreibung = "Neuen Wecker anlegen",
                    symbol = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) })
                if (daten.nextIsSnooze) StillerKnopf("Schlummern beenden", aufSchlummernBeenden,
                    Modifier.weight(1f, fill = false), hervorgehoben = true)
            }
        }
    }
}

/**
 * **Morgenruhe — das Tagesblatt am Anfang der Achse.**
 *
 * Morgenruhe ordnet den Tag an einer senkrechten Achse; die Weckerkarten sind Stationen darauf.
 * Der Hero ist folgerichtig der Kopf dieser Achse: Er führt mit dem Datum, nicht mit der Uhrzeit,
 * und die Linie der Stationen beginnt sichtbar unter ihm.
 *
 * Er trägt bewusst keinen Schatten und keinen Verlauf — das ist Schlichts Sprache. Die Abgrenzung
 * macht eine eigene, tiefer gesetzte Leinenfläche mit klarer Kante; das einzige Ornament ist ein
 * kurzer Messingstrich.
 */
@Composable
private fun MorgenruheHero(
    daten: HeroDaten, aufNeu: () -> Unit, aufOeffnen: (Alarm) -> Unit,
    aufEinstellungen: () -> Unit, aufSchlummernBeenden: () -> Unit,
) {
    val gold = LocalGold.current
    val oeffnen = { daten.nextAlarm?.let(aufOeffnen); Unit }
    if (daten.schmal) { SchmalerHero(daten, aufNeu, oeffnen, aufSchlummernBeenden); return }
    val form = RoundedCornerShape(LocalDesignTokens.current.karteRadius)
    // Wie bei Schlicht mit der echten Innenbreite gerechnet.
    val innen = daten.breite - HERO_AUSSEN * 2 - KARTE_INNEN * 2
    // Seit 1.1.114 steht die Schlafszene als breite Bühne über der ganzen Karte, statt als kleines
    // Quadrat an den rechten Rand gedrückt zu sein. Ihre Höhe folgt der Breite (rund 2,5 : 1) und ist
    // gedeckelt, damit der feststehende Kopf nicht zu viel Höhe frisst; Datum und Uhr teilen sich
    // dafür darunter eine Zeile.
    // Zusätzlich an der Fensterhöhe gedeckelt (höchstens 17 Prozent, bei großer Schrift weniger):
    // In niedrigen Fenstern — quer oder geteilter Bildschirm knapp über der SCHMAL-Grenze — fällt die
    // Bühne unter ihre Mindesthöhe weg, statt der Weckerliste den Platz zu nehmen.
    val fensterHoehe = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp /
        LocalDensity.current.fontScale.coerceAtLeast(1f)
    // Seit 1.1.118 deutlich höher (vorher 0,40 × Breite, höchstens 136/170 dp und 17 % der Fenster-
    // höhe): Das Zimmer war bei voller Breite nur ein flacher Streifen, Möbel und Person kaum zu
    // erkennen, weil die Höhe den Maßstab der Szene bestimmt. Der Platz kommt aus den Zeilen unter
    // der Uhr, die jetzt rechts neben Datum und Uhr stehen.
    val buehne = ((innen + KARTE_INNEN) * 0.50f)
        .coerceIn(0.dp, if (daten.weit) 240.dp else 210.dp)
        .coerceAtMost(fensterHoehe * 0.26f)
    val zeigtMotiv = buehne >= 96.dp
    // Seit 1.1.117 ist die Bühne ein geschlossenes Zimmer mit Decke, Wänden und Boden bis an den Rand:
    // Es bekommt deshalb eigene runde Ecken und eine feine Kante, statt eckig in der Karte zu stehen.
    val zimmerForm = RoundedCornerShape((LocalDesignTokens.current.karteRadius * 0.7f).coerceAtMost(18.dp))
    // Datum und Uhr bekommen knapp die Hälfte, der rechte Block den Rest — er trägt Knöpfe.
    val linksBreite = (innen - SPALTEN_ABSTAND) * 0.46f
    val rechtsBreite = innen - SPALTEN_ABSTAND - linksBreite
    // Einstellbare Kartendeckkraft: Fläche, Licht, Kante und Schatten werden gemeinsam durchsichtig.
    val deckkraft = LocalKartenDeckkraft.current
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()
            .deckSchatten(gold.akzentWarm, Hoehe.karteErhoeht, form, deckkraft)
            .clip(form).background(Brush.verticalGradient(listOf(gold.heroGrund.heller(if (gold.istDunkel) 0.08f else 0.04f), gold.heroGrund, gold.heroGrund.dunkler(0.07f)).map { it.deck(deckkraft) }))
            .glanzBogen(deckung = (if (gold.istDunkel) 0.07f else 0.19f) * deckkraft)
            // Weiches Morgenlicht hinter dem Bett — ein warmer Schein oben rechts.
            .drawBehind {
                drawRect(Brush.radialGradient(listOf(gold.akzentWarm.copy(alpha = .24f * deckkraft), androidx.compose.ui.graphics.Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.15f), radius = size.maxDimension * 0.6f))
            }
            .heroFase3D(
                form = form,
                kante = gold.heroKante,
                licht = androidx.compose.ui.graphics.Color.White,
                lichtAlpha = if (gold.istDunkel) 0.24f else 0.65f,
                schattenAlpha = if (gold.istDunkel) 0.45f else 0.22f,
                deckkraft = deckkraft,
            )
            // Morgenlicht, das alle paar Sekunden über die Karte streicht.
            .glanzLauf(staerke = (if (gold.istDunkel) 0.10f else 0.45f) * deckkraft)) {
            HeroDeko(Modifier.matchParentSize().graphicsLayer { alpha = deckkraft })
            Column(Modifier.fillMaxWidth()) {
                // Die Bühne: das Schlafzimmer in einem plastischen 3D-Fensterrahmen mit Tiefenschatten innen.
                if (zeigtMotiv) Box(
                    Modifier.fillMaxWidth().height(buehne).padding(start = 10.dp, end = 10.dp, top = 10.dp)
                        .clip(zimmerForm),
                ) {
                    LocalGestalt.current.Motiv(Modifier.matchParentSize())
                    // Innerer 3D-Rahmenschatten oben & seitlich plus feine helle Fensterbank-Kante unten.
                    Box(
                        Modifier.matchParentSize()
                            .drawBehind {
                                drawRect(
                                    Brush.verticalGradient(
                                        listOf(
                                            androidx.compose.ui.graphics.Color.Black.copy(alpha = if (gold.istDunkel) 0.36f else 0.16f),
                                            androidx.compose.ui.graphics.Color.Transparent,
                                        ),
                                        startY = 0f,
                                        endY = 18.dp.toPx(),
                                    ),
                                )
                                drawRect(
                                    Brush.verticalGradient(
                                        listOf(
                                            androidx.compose.ui.graphics.Color.Transparent,
                                            androidx.compose.ui.graphics.Color.Black.copy(alpha = if (gold.istDunkel) 0.26f else 0.10f),
                                        ),
                                        startY = size.height - 14.dp.toPx(),
                                        endY = size.height,
                                    ),
                                )
                            }
                            .border(
                                1.5f.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        androidx.compose.ui.graphics.Color.Black.copy(alpha = if (gold.istDunkel) 0.45f else 0.20f),
                                        gold.heroKante.copy(alpha = 0.55f),
                                        androidx.compose.ui.graphics.Color.White.copy(alpha = if (gold.istDunkel) 0.18f else 0.65f),
                                    ),
                                ),
                                zimmerForm,
                            ),
                    )
                }
                // Kompakte Statuszeile über Datum und Uhr: links aktive Wecker, rechts Weckbereitschaft —
                // hält den Hero kompakt, sodass alle Weckerkarten darunter mehr Raum bekommen.
                Row(
                    Modifier.fillMaxWidth().padding(start = KARTE_INNEN, end = KARTE_INNEN, top = if (zeigtMotiv) 8.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (daten.anzahl > 0) HeroStatusPille(
                        text = daten.aktivText,
                        aktiv = daten.aktivAnzahl > 0,
                        farbe = gold.primaer,
                        textFarbe = gold.heroSchrift,
                    ) else Spacer(Modifier.width(1.dp))
                    Spacer(Modifier.width(8.dp))
                    BereitZeile(daten, aufEinstellungen, LocalSemantisch.current.erfolg, LocalSemantisch.current.warnung)
                }
                Row(Modifier.fillMaxWidth().padding(start = KARTE_INNEN, end = KARTE_INNEN, bottom = 12.dp, top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(SPALTEN_ABSTAND),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(linksBreite), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        // Das einzige Ornament des Designs: ein kurzer Messingstrich als Tagesmarke.
                        Box(Modifier.width(40.dp).height(2.dp).background(gold.akzentWarm))
                        Text(datumsZeile(daten.now), style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium, color = gold.heroFuehrung,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        GedeckelteSchrift {
                            val groesse = passendeUhrGroesse(linksBreite, uhrGroesse(daten.stufe),
                                zahlSchrift(), zahlGewicht())
                            // Rollende Ziffern, durch die alle paar Sekunden ein Glanz zieht.
                            RollendeUhr(
                                formatClock(daten.now),
                                androidx.compose.ui.text.TextStyle(fontFamily = zahlSchrift(), fontWeight = zahlGewicht(),
                                    fontSize = groesse, color = gold.heroSchrift),
                                Modifier.glanzLauf(staerke = 0.6f, farbe = schriftGlanzFarbe(), nurSchrift = true, versatzMs = 350L),
                                ueberschrift = true,
                            )
                        }
                    }
                    MorgenruheRechts(daten, rechtsBreite, oeffnen, aufNeu, aufSchlummernBeenden,
                        Modifier.weight(1f))
                }
            }
        }
        // Der Anschluss an die Tagesachse: Der Punkt sitzt genau dort, wo `Station` in der Liste
        // ihre Linie zeichnet — 13 dp von der Spaltenkante. Ohne ihn schwebte die erste Station.
        // 26 dp = die bisherigen 12 dp plus die 14 dp Hero-Abstand, den Morgenruhe nicht mehr trägt.
        Row(Modifier.fillMaxWidth().height(26.dp)) {
            Box(Modifier.width(26.dp), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(gold.rahmen))
            }
        }
    }
}

/**
 * **Traumraum — die Kuppel als Objekt, die Perle als Brücke.**
 *
 * Aufbau und Überlappung bleiben; sie tragen den Charakter. Behoben ist, was den Hero unsichtbar
 * machte: Der radiale Schein lag bisher auf dem **Seitenhintergrund** und beleuchtete damit Seite
 * und Kuppel gleichermaßen — gemessene 1,07:1 im hellen Modus, auf dem Gerät nicht zu erkennen.
 * Jetzt liegt das Licht **in** der Kuppel, die Kuppel hat eine eigene warme Fläche mit sichtbarer
 * Kante, und die Perle sitzt auf der normalen Fläche statt auf derselben Farbe wie die Kuppel —
 * erst dadurch ist die Überlappung überhaupt zu sehen.
 */
@Composable
private fun TraumraumHero(
    daten: HeroDaten, aufNeu: () -> Unit, aufOeffnen: (Alarm) -> Unit,
    aufEinstellungen: () -> Unit, aufSchlummernBeenden: () -> Unit,
) {
    val gold = LocalGold.current
    val semantisch = LocalSemantisch.current
    val oeffnen = { daten.nextAlarm?.let(aufOeffnen); Unit }
    if (daten.schmal) { SchmalerHero(daten, aufNeu, oeffnen, aufSchlummernBeenden); return }
    val radius = LocalDesignTokens.current.karteRadius
    // Vollständig plastische 3D-Kuppel ohne abgeschnittene 90-Grad-Ecken oben.
    val kuppelForm = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = radius, bottomEnd = radius)
    val kuppelDeckkraft = LocalKartenDeckkraft.current
    val motiv = if (daten.weit) 84.dp else 64.dp
    val versatz = 26.dp
    // Auch hier gegen die echte Restbreite gemessen: innen abzüglich der beidseitigen Motivfreiräume.
    val uhrPlatz = daten.breite - HERO_AUSSEN * 2 - 40.dp - motiv * 1.1f
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth()
                .then(if (kuppelDeckkraft > 0f) Modifier.shadow(20.dp, kuppelForm,
                    ambientColor = gold.primaer.copy(alpha = .50f * kuppelDeckkraft * kuppelDeckkraft),
                    spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = .65f * kuppelDeckkraft * kuppelDeckkraft)) else Modifier)
                .clip(kuppelForm)
                .background(Brush.verticalGradient(listOf(gold.heroGrund.heller(if (gold.istDunkel) 0.09f else 0.12f), gold.heroGrund, gold.heroGrundUnten.dunkler(0.15f)).map { it.deck(kuppelDeckkraft) }))
                .drawBehind {
                    if (kuppelDeckkraft > 0f) {
                        // Warmer Glut-Horizont im unteren Kuppelbogen für räumliche 3D-Wölbung.
                        drawRect(
                            Brush.radialGradient(
                                listOf(
                                    gold.primaer.copy(alpha = (if (gold.istDunkel) 0.24f else 0.16f) * kuppelDeckkraft),
                                    androidx.compose.ui.graphics.Color.Transparent,
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.96f),
                                radius = size.width * 0.72f,
                            ),
                        )
                    }
                }
                .glanzBogen(deckung = (if (gold.istDunkel) 0.09f else 0.26f) * kuppelDeckkraft)
                .heroFase3D(
                    form = kuppelForm,
                    kante = gold.heroKante,
                    licht = if (gold.istDunkel) gold.akzentWarm else androidx.compose.ui.graphics.Color.White,
                    lichtAlpha = if (gold.istDunkel) 0.42f else 0.72f,
                    schattenAlpha = if (gold.istDunkel) 0.52f else 0.26f,
                    deckkraft = kuppelDeckkraft,
                )
                // Ein warmes Streiflicht zieht alle paar Sekunden durch die Kuppel.
                .glanzLauf(staerke = (if (gold.istDunkel) 0.12f else 0.4f) * kuppelDeckkraft,
                    farbe = if (gold.istDunkel) gold.akzentWarm else androidx.compose.ui.graphics.Color.White)
                .padding(top = 12.dp, bottom = versatz + 14.dp, start = 18.dp, end = 18.dp),
        ) {
            // Nachthimmel in der Kuppel: Sterne in zwei Tönen und ein heller Mond.
            Sternenhimmel(Modifier.matchParentSize().graphicsLayer { alpha = kuppelDeckkraft })
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    HeroStatusPille(
                        text = daten.aktivText,
                        aktiv = daten.aktivAnzahl > 0,
                        farbe = gold.primaer,
                        textFarbe = gold.heroSchrift,
                    )
                    Spacer(Modifier.width(8.dp))
                    BereitZeile(daten, aufEinstellungen, semantisch.erfolg, semantisch.warnung)
                }
                Box(Modifier.fillMaxWidth()) {
                    LocalGestalt.current.Motiv(Modifier.align(Alignment.CenterStart).size(motiv))
                    Column(Modifier.fillMaxWidth().padding(horizontal = motiv * 0.55f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(datumsZeile(daten.now), style = MaterialTheme.typography.labelLarge,
                            color = gold.akzentWarm, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        GedeckelteSchrift {
                            val groesse = passendeUhrGroesse(uhrPlatz, uhrGroesse(daten.stufe),
                                zahlSchrift(), zahlGewicht())
                            // Rollende Ziffern, durch die alle paar Sekunden ein Glutglanz zieht.
                            RollendeUhr(
                                formatClock(daten.now),
                                androidx.compose.ui.text.TextStyle(fontFamily = zahlSchrift(), fontWeight = zahlGewicht(),
                                    fontSize = groesse, color = gold.heroFuehrung),
                                Modifier.glanzLauf(staerke = 0.65f, farbe = schriftGlanzFarbe(), nurSchrift = true, versatzMs = 350L),
                                ueberschrift = true,
                            )
                        }
                    }
                }
            }
        }
        // Die Perle überlappt die Kuppel. Der Versatz wird aus der belegten Höhe herausgerechnet,
        // damit darunter keine tote Fläche entsteht.
        // Die Aktion sitzt **in** der Perle, nicht daneben. Vorher stand sie als runder Knopf
        // rechts außerhalb auf dem Seitenhintergrund, während die Kuppel darüber endete — sie
        // wirkte herausgefallen, und rechts neben der Perle blieb eine leere Kuppelecke stehen.
        // Jetzt überlappt eine einzige geschlossene Fläche die Kuppel.
        Box(
            Modifier.fillMaxWidth()
                .layout { messbar, grenzen ->
                    val platz = messbar.measure(grenzen)
                    val hub = versatz.roundToPx()
                    layout(platz.width, (platz.height - hub).coerceAtLeast(0)) { platz.place(0, -hub) }
                }
                .padding(horizontal = 18.dp),
        ) {
            Perle {
                // Auf schmalen Geräten steht die Aktion unter dem Termin statt daneben; die
                // Schwelle rechnet mit der echten Innenbreite der Perle.
                val innen = daten.breite - HERO_AUSSEN * 2 - 36.dp - 32.dp
                val nebeneinander = innen >= textMindest() + 60.dp && !daten.nextIsSnooze
                if (nebeneinander) Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.weight(1f)) {
                        TraumTermin(daten, oeffnen)
                    }
                    GoldKnopf("＋", aufNeu, hauptKnopf = true, beschreibung = "Neuen Wecker anlegen")
                } else {
                    TraumTermin(daten, oeffnen)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        GoldKnopf("Neuer Wecker", aufNeu, hauptKnopf = true,
                            beschreibung = "Neuen Wecker anlegen",
                            symbol = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) })
                        if (daten.nextIsSnooze) StillerKnopf("Schlummern beenden",
                            aufSchlummernBeenden, hervorgehoben = true)
                    }
                }
            }
        }
    }
}

/**
 * **Orbit — das Hauptinstrument.**
 *
 * Orbit ist eine Instrumententafel, und der Hero ist ihr Hauptinstrument: Kopfstreifen mit
 * Statusleuchte, Messwerk in der Mitte, Fußstreifen mit Aktion und Bereitschaftsanzeige.
 * Statt eines Rings — den dieses Design nie hatte — zeigt eine lineare Skala die Restzeit als
 * Anteil eines Tages. Sie rechnet nur mit dem bereits bekannten Termin, es kommt keine neue
 * Weckmechanik dazu.
 *
 * Kein Schatten, kein Verlauf, kein Schein: Hier arbeiten ausschließlich Linien und Kanten.
 */
@Composable
private fun OrbitHero(
    daten: HeroDaten, aufNeu: () -> Unit, aufOeffnen: (Alarm) -> Unit,
    aufEinstellungen: () -> Unit, aufSchlummernBeenden: () -> Unit,
) {
    val gold = LocalGold.current
    val semantisch = LocalSemantisch.current
    val oeffnen = { daten.nextAlarm?.let(aufOeffnen); Unit }
    if (daten.schmal) { SchmalerHero(daten, aufNeu, oeffnen, aufSchlummernBeenden); return }
    val form = RoundedCornerShape(LocalDesignTokens.current.karteRadius)
    // Auch hier gibt die echte Innenbreite die Motivgröße vor: Auf sehr schmalen Abdeckbildschirmen
    // ginge sonst der Datenspalte der Platz aus. Orbits Innenabstand ist 12 dp, nicht 16 dp.
    val innen = daten.breite - HERO_AUSSEN * 2 - 24.dp
    // Der Text beginnt nach gut einem Drittel; die Sonne und die inneren Bahnen bleiben frei.
    val textStart = (innen * 0.36f).coerceAtMost((innen - textMindest()).coerceAtLeast(12.dp))
    val uhrPlatz = innen + 12.dp - textStart
    val statusFarbe = when {
        daten.nextIsSnooze -> semantisch.info
        daten.hatTermin -> gold.akzentWarm
        else -> gold.heroSchriftGedaempft
    }
    val statusText = when {
        daten.nextIsSnooze -> "SCHLUMMERT"
        daten.hatTermin -> if (daten.anzahl > 0) "${daten.aktivAnzahl}/${daten.anzahl} AKTIV" else "AKTIV"
        else -> "KEIN TERMIN"
    }
    // Einstellbare Kartendeckkraft: Fläche, Leuchten, Ring und Schatten werden gemeinsam durchsichtig.
    val deckkraft = LocalKartenDeckkraft.current
    Box(Modifier.fillMaxWidth()
        .deckSchatten(gold.primaer, Hoehe.karteErhoeht, form, deckkraft)
        .clip(form).background(Brush.verticalGradient(listOf(gold.heroGrund.heller(0.07f), gold.heroGrund, gold.heroGrund.dunkler(0.11f)).map { it.deck(deckkraft) }))
        .glanzBogen(deckung = (if (gold.istDunkel) 0.09f else 0.18f) * deckkraft)
        // Kühles Instrumentenleuchten hinter dem Motiv und gefräste Innen-Nut.
        .drawBehind {
            drawRect(Brush.radialGradient(listOf(gold.primaer.copy(alpha = .18f * deckkraft), androidx.compose.ui.graphics.Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.5f), radius = size.maxDimension * 0.5f))
        }
        .then(if (deckkraft > 0f) Modifier.nut(form, alpha = 0.24f * deckkraft) else Modifier)
        // Präzise lineare Metall-Fase statt sweepGradient (gleichmäßige Kantenkontraste in Hell & Dunkel).
        .heroFase3D(
            form = form,
            kante = gold.heroKante,
            licht = gold.primaer.heller(0.35f),
            lichtAlpha = if (gold.istDunkel) 0.45f else 0.55f,
            schattenAlpha = if (gold.istDunkel) 0.55f else 0.28f,
            deckkraft = deckkraft,
            staerke = 1.5f.dp,
        )
        // Ein Abtaststrahl im Signalgrün zieht alle paar Sekunden über das Modul.
        .glanzLauf(staerke = (if (gold.istDunkel) 0.12f else 0.3f) * deckkraft, farbe = gold.primaer)) {
        HeroDeko(Modifier.matchParentSize().graphicsLayer { alpha = deckkraft })
        Column(Modifier.fillMaxWidth()) {
            // Kopfstreifen: links der Signalbalken, rechts die Statusleuchte.
            Row(
                Modifier.fillMaxWidth().padding(start = 0.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(3.dp).height(14.dp).background(gold.akzentWarm))
                Text("NÄCHSTER TERMIN", Modifier.padding(start = 9.dp).weight(1f),
                    fontFamily = IdeenSchriftFest, style = MaterialTheme.typography.labelSmall,
                    color = gold.heroSchriftGedaempft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(datumKurz(daten.now).uppercase(java.util.Locale.GERMAN),
                    fontFamily = IdeenSchriftFest, style = MaterialTheme.typography.labelSmall,
                    color = gold.heroSchriftGedaempft, maxLines = 1)
                Box(Modifier.padding(start = 10.dp, end = 5.dp).size(7.dp).background(statusFarbe, CircleShape))
                Text(statusText, fontFamily = IdeenSchriftFest,
                    style = MaterialTheme.typography.labelSmall, color = statusFarbe, maxLines = 1)
            }
            HorizontalDivider(color = gold.heroKante)
            // Links dreht sich groß das Sonnensystem im Hintergrund, rechts schützt ein sanfter
            // Leseverlauf die Instrumentendaten vor den äußeren Planetenbahnen.
            Box(Modifier.fillMaxWidth()) {
                Sonnensystem(Modifier.matchParentSize().clipToBounds())
                Box(
                    Modifier.matchParentSize().drawBehind {
                        if (deckkraft > 0f) {
                            drawRect(
                                Brush.horizontalGradient(
                                    listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        gold.heroGrund.copy(alpha = 0.55f * deckkraft),
                                        gold.heroGrund.copy(alpha = 0.80f * deckkraft),
                                    ),
                                    startX = size.width * 0.40f,
                                    endX = size.width,
                                ),
                            )
                        }
                    },
                )
                Column(Modifier.fillMaxWidth().padding(start = textStart, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.End) {
                    // Die Skala ist genau so breit wie die Uhrzeit darüber. Vorher lief sie über die
                    // ganze Textspalte und ragte links ins Sonnensystem; jetzt bleibt es frei sichtbar.
                    var uhrBreite by remember { mutableIntStateOf(0) }
                    GedeckelteSchrift {
                        val groesse = passendeUhrGroesse(uhrPlatz, uhrGroesse(daten.stufe),
                            IdeenSchriftFest, FontWeight.SemiBold)
                        // Rollende Ziffern wie auf einer Anzeigetafel, dazu ein Signalglanz.
                        RollendeUhr(
                            formatClock(daten.now),
                            androidx.compose.ui.text.TextStyle(fontFamily = IdeenSchriftFest, fontWeight = FontWeight.SemiBold,
                                fontSize = groesse, color = gold.heroFuehrung),
                            Modifier.onSizeChanged { uhrBreite = it.width }
                                .glanzLauf(staerke = 0.7f, farbe = schriftGlanzFarbe(), nurSchrift = true, versatzMs = 350L),
                            ueberschrift = true,
                        )
                    }
                    RestzeitSkala(daten, gold.heroFuehrung, if (daten.nextIsSnooze) semantisch.info else gold.akzentWarm,
                        gold.heroKante,
                        Modifier.width(with(LocalDensity.current) { uhrBreite.toDp() }))
                    OrbitZeile("TERMIN", if (daten.next == null) "—"
                        else terminAnzeige(daten.now, daten.next).einzeilig, gold, oeffnen, daten.nextAlarm != null)
                    OrbitZeile("NAME", daten.nextName ?: "—", gold, oeffnen, daten.nextAlarm != null)
                    OrbitZeile("REST", daten.next?.let { remainingLong(it - daten.now) } ?: "—",
                        gold, oeffnen, daten.nextAlarm != null, wertFarbe = gold.heroFuehrung)
                }
            }
            HorizontalDivider(color = gold.heroKante)
            // Vorher standen hier drei ungewichtete Teile nebeneinander, getrennt durch einen
            // Spacer mit Gewicht. Der kann sich nur bis null zusammenziehen — sobald die lange
            // Aktion „SCHLUMMERN BEENDEN" dazukam, wurde die Bereitschaftsanzeige rechts
            // hinausgedrängt. Eine FlowRow bricht stattdessen sauber um.
            // Vorher standen hier drei ungewichtete Teile nebeneinander, getrennt durch einen
            // Spacer mit Gewicht. Der kann sich nur bis null zusammenziehen — sobald die lange
            // Aktion „SCHLUMMERN BEENDEN" dazukam, wurde die Bereitschaft rechts hinausgedrängt.
            // Nachgerechnet braucht diese Zeile mit Schlummern und fehlenden Freigaben rund
            // 436 dp; selbst das Fold bietet nur 419 dp. Die Aufteilung hängt deshalb am
            // Zustand, nicht an der Gerätebreite: Die Hauptaktion und die Bereitschaft teilen
            // sich immer eine Zeile, die lange Schlummeraktion bekommt ihre eigene darunter.
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Dieselbe Aktionszeile wie in den anderen Designs: „Neuer Wecker“ und „Öffnen“ gleich groß,
                // beim Schlummern statt „Öffnen“ das Beenden.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { HeroAktionen(daten, aufNeu, oeffnen, aufSchlummernBeenden) }
                    BereitZeile(daten, aufEinstellungen, semantisch.erfolg, semantisch.warnung,
                        stil = MaterialTheme.typography.labelSmall.copy(fontFamily = IdeenSchriftFest))
                }
            }
        }
    }
}

/** Eine Messwertzeile der Instrumententafel: Beschriftung links, Wert rechts. */
@Composable
private fun OrbitZeile(
    name: String, wert: String, gold: de.frank.genialeideen.ui.theme.GoldPalette,
    aufOeffnen: () -> Unit, klickbar: Boolean,
    wertFarbe: androidx.compose.ui.graphics.Color = gold.heroSchrift,
) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (klickbar) Modifier.clickable(onClickLabel = "Nächsten Wecker öffnen", onClick = aufOeffnen) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        // Rechtsbündig: Beschriftung direkt vor dem Wert, beide an der rechten Kante.
        Text(name, Modifier.padding(end = 8.dp), fontFamily = IdeenSchriftFest,
            style = MaterialTheme.typography.labelSmall, color = gold.heroSchriftGedaempft, maxLines = 1)
        Text(wert, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodySmall,
            color = wertFarbe, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
    }
}

/**
 * Die lineare Restzeitskala: gefüllter Anteil bis zum nächsten Termin, bezogen auf 24 Stunden,
 * mit Teilstrichen alle sechs Stunden. Reine Arithmetik auf dem bereits bekannten Termin.
 * Für TalkBack stumm — die Zeile „REST" daneben ist maßgeblich.
 */
@Composable
private fun RestzeitSkala(
    daten: HeroDaten, spurFarbe: androidx.compose.ui.graphics.Color,
    fuellFarbe: androidx.compose.ui.graphics.Color, kante: androidx.compose.ui.graphics.Color,
    /** Die Breite gibt der Aufrufer vor; im Orbit-Hero ist das die Breite der Uhrzeit. */
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val anteil = daten.tagesAnteil()
    Canvas(modifier.height(4.dp).clearAndSetSemantics { }) {
        drawRect(kante, size = size)
        if (anteil > 0f) drawRect(fuellFarbe, size = androidx.compose.ui.geometry.Size(size.width * anteil, size.height))
        // Teilstriche alle sechs Stunden — sie geben der Skala erst ihren Maßstab.
        for (i in 1..3) {
            val x = size.width * (i / 4f)
            drawRect(spurFarbe.copy(alpha = .55f), topLeft = Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(1.dp.toPx(), size.height))
        }
    }
}

/**
 * Eine Weckerkarte. Zugeklappt hat sie in jeder Ansicht dieselbe Höhe — das war vorher nicht so:
 * Die Datumszeile erschien nur ab übermorgen, und Schlaf- sowie Warnhinweise kamen als zusätzliche
 * Zeilen dazu. Auf dem Gerät standen dadurch Karten mit rund 320 und mit rund 394 Pixeln Höhe
 * nebeneinander.
 *
 * Jetzt gibt es feste Fächer, die immer besetzt sind:
 *  1. Kopfzeile — Schalter, Datum, Uhrzeit, Name, Klappknopf
 *  2. Planzeile — Zeitplan und nächster Termin
 *  3. Hinweisfach — genau eine Zeile, auch wenn es nichts zu melden gibt
 *
 * Nichts wird dabei weggelassen: Gibt es mehrere Hinweise, führt der wichtigere, und der andere
 * bleibt als Symbol am Zeilenende sichtbar. Vollständig stehen alle Hinweise in den Details.
 * Der einzige bewusste Ausreißer ist ein laufendes Schlummern: Es bekommt seine eigene Zeile mit
 * der Aktion „Schlummern beenden", weil diese Aktion erreichbar bleiben muss. Das betrifft immer
 * nur den einen Wecker, der gerade schlummert, und hebt ihn zu Recht aus der Reihe heraus.
 *
 * Aufgeklappt darf die Karte wie bisher mit ihrem Inhalt wachsen.
 */
@Composable
private fun WeckerKarte(
    alarm: Alarm, vm: WeckerViewModel, now: Long, hinweis: String?, expanded: Boolean,
    istNaechster: Boolean,
    aufKlappen: () -> Unit, onEdit: (Alarm) -> Unit, onDelete: (Alarm) -> Unit,
    glowAlpha: Float, resumed: Boolean, reducedMotion: Boolean,
    // `animateItem` lebt im LazyItemScope; die Karte selbst kennt ihn nicht mehr, seit sie eine
    // eigene Composable ist. Der Aufrufer reicht den Modifier deshalb herein.
    modifier: Modifier = Modifier,
) {
    val gold = LocalGold.current
    val semantisch = LocalSemantisch.current
    val entwurf = LocalDesignTokens.current.design
    val deckkraft = LocalKartenDeckkraft.current
    // Morgenruhe reiht die Wecker als Stationen an einer Tagesachse auf.
    val achse = entwurf == Design.MORGENRUHE
    // Orbit setzt zugeklappte Wecker als dichte Instrumentenzeile: knappes Innenmaß und enge
    // Abstände. Innerhalb von Orbit sind die Karten dadurch untereinander wieder gleich hoch.
    val dicht = entwurf == Design.ORBIT && !expanded

    val schlummerBis = alarm.snoozeUntil.takeIf { it > now }
    val schlafHinweis = if (alarm.enabled && alarm.nextAt > 0 && alarm.sleepMinutes > 0)
        Schlaf.hinweis(alarm.nextAt, alarm.sleepMinutes, now) else null
    val musikInfo = remember(alarm.music, alarm.musicName, alarm.musicQuelle, alarm.tone, alarm.steps, resumed) {
        if (Step.MUSIC in alarm.steps) MusikAnzeige.von(alarm) else null
    }
    val klangWarnung = musikInfo?.takeIf { it.ersatz }?.let { "MP3 „${it.fehlenderName}“ fehlt – Ersatzton aktiv" }
    val wirksameWarnung = hinweis ?: klangWarnung
    val kurzMeta = remember(alarm, musikInfo) {
        buildList {
            if (musikInfo != null && !musikInfo.ersatz) {
                add(if (musikInfo.art == MusikArt.MP3) "MP3: ${musikInfo.titel}" else musikInfo.titel)
            } else if (Step.TONE in alarm.steps) {
                add(Tones.names[alarm.cue] ?: "Klingelzeichen")
            }
            add("${alarm.volume} %")
            if (Step.TEXT in alarm.steps) add("Text")
            if (Step.IDEAS in alarm.steps) add("Ideen")
            if (Step.TASKS in alarm.steps) add("Aufgaben")
            if (alarm.photoRequired) add("Foto")
        }.joinToString(" · ")
    }

    val kartenForm = RoundedCornerShape(LocalDesignTokens.current.karteRadius)
    Station(achse, alarm.enabled, modifier) {
        // Immer Ebene.KARTE mit inListe = true (ein Schatten, keine Körnung im Scroll-Raster);
        // der nächste Wecker hebt sich über den goldenen Akzentrahmen und das „Nächster“-Abzeichen ab.
        LocalGestalt.current.Flaeche(
            Modifier.fillMaxWidth()
                .then(
                    when {
                        glowAlpha > 0f -> Modifier.border(2.dp, gold.primaer.copy(alpha = glowAlpha), kartenForm)
                        istNaechster && deckkraft > 0f -> Modifier.border(
                            1.5f.dp,
                            gold.primaer.copy(alpha = (if (gold.istDunkel) 0.72f else 0.78f) * deckkraft),
                            kartenForm,
                        )
                        else -> Modifier
                    },
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    enabled = !expanded, onClickLabel = "Wecker bearbeiten",
                    onClick = { onEdit(alarm) },
                ),
            ebene = Ebene.KARTE,
            inListe = true,
        ) {
            // No animateContentSize here: the detail block animates its own size, never two size animations at once.
            Column(
                Modifier.padding(horizontal = if (dicht) 10.dp else 16.dp, vertical = if (dicht) 8.dp else 11.dp),
                verticalArrangement = Arrangement.spacedBy(if (dicht) 3.dp else 6.dp),
            ) {
                // Name über dem Schalter, in allen Designs gleich: Name in einer Blase, bei nächstem
                // Wecker ein klares „Nächster“-Abzeichen, dann „ · “ und die Wiederholung.
                Row(
                    Modifier.fillMaxWidth().clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                        onClickLabel = "Wecker bearbeiten", onClick = { onEdit(alarm) },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val zeilenStil = if (dicht) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium
                    val planStil = zeilenStil.copy(fontWeight = FontWeight.Normal, fontSize = zeilenStil.fontSize * 0.85f)
                    val namensForm = RoundedCornerShape(LocalDesignTokens.current.chipRadius)
                    Text(alarm.name, Modifier.weight(1f, fill = false)
                        .clip(namensForm)
                        .background(
                            if (alarm.enabled) gold.primaer.copy(alpha = .17f) else gold.textGedaempft.copy(alpha = .10f),
                            namensForm,
                        )
                        .border(
                            1.dp,
                            if (alarm.enabled) gold.primaer.copy(alpha = .42f) else gold.textGedaempft.copy(alpha = .24f),
                            namensForm,
                        )
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                        style = zeilenStil, color = if (alarm.enabled) gold.textPrimaer else gold.textGedaempft,
                        maxLines = if (expanded) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis)
                    if (istNaechster) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (schlummerBis != null) "Schlummert" else "Nächster",
                            Modifier.clip(namensForm)
                                .background(if (schlummerBis != null) semantisch.info else gold.primaer)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (schlummerBis != null) androidx.compose.ui.graphics.Color.White else gold.aufPrimaer,
                            maxLines = 1,
                        )
                    }
                    Text(" · " + (if (alarm.enabled) kurzPlan(alarm) else "${kurzPlan(alarm)} · aus"),
                        style = planStil, color = gold.textGedaempft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // --- Fach 1: Kopfzeile ---
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (dicht) 8.dp else 12.dp)) {
                    Schalter3D(alarm.enabled, { on ->
                        // A one-off date that has passed cannot ring: open the editor to pick a new date instead of failing.
                        val latest = vm.store.get(alarm.id) ?: alarm
                        if (on && latest.isExpiredOnce()) onEdit(latest) else vm.toggle(alarm, on)
                    }, Modifier.semantics {
                        contentDescription = "Wecker aktivieren: ${alarm.name}"
                    })
                    Box(Modifier.weight(1f).clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                        onClickLabel = "Wecker bearbeiten", onClick = { onEdit(alarm) })) {
                        val timeColor by androidx.compose.animation.animateColorAsState(
                            if (alarm.enabled) gold.primaer else gold.textGedaempft,
                            if (reducedMotion) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(250),
                            label = "weckzeitFarbe")
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            WeckzeitUeberschrift(alarm, now, timeColor, platzHalten = !expanded)
                            if (alarm.days.isNotEmpty() && !dicht) {
                                WochentagPunkte(alarm.days, alarm.enabled, gold.primaer, gold.textGedaempft)
                            }
                        }
                    }
                    KlappKnopf(expanded, aufKlappen,
                        beschreibung = "Weckerdetails ${if (expanded) "zuklappen" else "aufklappen"}: ${alarm.name}")
                }

                // Without RESUMED or with reduced motion the details switch instantly.
                val detailEnter = if (reducedMotion || !resumed) androidx.compose.animation.EnterTransition.None
                    else androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(200)) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200))
                val detailExit = if (reducedMotion || !resumed) androidx.compose.animation.ExitTransition.None
                    else androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(200)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))

                // --- Fach 2: Kompakte Info- bzw. Hinweiszeile im zugeklappten Zustand ---
                AnimatedVisibility(!expanded && !dicht, enter = detailEnter, exit = detailExit) {
                    HinweisFach(schlummerBis = schlummerBis, warnung = wirksameWarnung, schlaf = schlafHinweis,
                        meta = kurzMeta, hatSonne = alarm.sunMode.isNotBlank(),
                        warnFarbe = semantisch.warnung, akzentFarbe = gold.primaer,
                        ruhigFarbe = gold.textPrimaer, leerFarbe = gold.textGedaempft)
                }
                if (dicht) HinweisFach(schlummerBis = schlummerBis, warnung = wirksameWarnung, schlaf = schlafHinweis,
                    meta = kurzMeta, hatSonne = alarm.sunMode.isNotBlank(),
                    warnFarbe = semantisch.warnung, akzentFarbe = gold.primaer,
                    ruhigFarbe = gold.textPrimaer, leerFarbe = gold.textGedaempft)

                // --- Details ---
                AnimatedVisibility(expanded, enter = detailEnter, exit = detailExit) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(scheduleLabel(alarm), color = gold.textGedaempft, style = MaterialTheme.typography.bodySmall)
                    Text(alarm.steps.joinToString(" → ") { it.title }, color = gold.primaer, style = MaterialTheme.typography.bodySmall)
                    schlummerBis?.let { bis -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Snooze, null, tint = gold.primaer, modifier = Modifier.size(20.dp))
                        Text("Schlummert bis ${formatClock(bis)}", Modifier.weight(1f).padding(horizontal = 8.dp),
                            color = gold.primaer)
                        StillerKnopf("Schlummern beenden", { vm.endSnooze(alarm) }, hervorgehoben = true)
                    } }
                    wirksameWarnung?.let { StatusZeile(Icons.Default.Warning, it, semantisch.warnung) }
                    schlafHinweis?.let { SchlafZeile(it) }
                    // Skip mark when present; entries without a mark keep the former nextAt comparison.
                    val skipped = AlarmTime.isSkipping(alarm, Instant.ofEpochMilli(now))
                    if (alarm.enabled && alarm.nextAt > 0) StatusZeile(Icons.Default.Alarm, "Nächster Termin: ${terminAnzeige(now, alarm.nextAt).einzeilig}", gold.textPrimaer)
                    if (skipped) StatusZeile(Icons.Default.SkipNext, "Ein Termin wird ausgelassen", gold.primaer)
                    val tonText = musikInfo?.let { if (it.ersatz) "Ersatzton (${it.titel})" else "${it.quelle}: ${it.titel}" }
                    Text(listOfNotNull(tonText, "Lautstärke ${alarm.volume} %", if (alarm.photoRequired) "Foto-Aufgabe" else null).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                    if (alarm.needsSpeech) {
                        val status = when {
                            alarm.preparationError.isNotBlank() -> "Vorbereitung offen: ${alarm.preparationError}"
                            alarm.preparedAt == 0L -> "Sprachausgabe noch nicht offline bereit"
                            else -> "${alarm.voiceVariants.size.takeIf { it > 0 } ?: 1} Stimmvarianten offline bereit · ${formatAt(alarm.preparedAt)}"
                        }
                        Text(status, style = MaterialTheme.typography.bodySmall,
                            color = if (alarm.preparationError.isNotBlank() || alarm.preparedAt == 0L) semantisch.warnung else semantisch.erfolg)
                    }
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StillerKnopf("Bearbeiten", { onEdit(alarm) }, hervorgehoben = true)
                        StillerKnopf("Testwecken", { vm.test(alarm) })
                        if (alarm.enabled && alarm.repeats && skipped) StillerKnopf("Auslassen rückgängig", { vm.unskip(alarm) })
                        if (alarm.needsSpeech) StillerKnopf("Audio vorbereiten", { vm.prepare(alarm) })
                        StillerKnopf("Duplizieren", { onEdit(alarm.copy(id = UUID.randomUUID().toString(), name = "${alarm.name} – Kopie", enabled = false, nextAt = 0, snoozeUntil = 0, snoozes = 0, skippedThrough = "")) })
                        StillerKnopf("Löschen", { onDelete(alarm) })
                    }
                } }
            }
        }
    }
}

@Composable
private fun HinweisFach(
    schlummerBis: Long?, warnung: String?, schlaf: String?,
    meta: String, hatSonne: Boolean,
    warnFarbe: androidx.compose.ui.graphics.Color,
    akzentFarbe: androidx.compose.ui.graphics.Color,
    ruhigFarbe: androidx.compose.ui.graphics.Color,
    leerFarbe: androidx.compose.ui.graphics.Color,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            schlummerBis != null -> {
                Icon(Icons.Default.Snooze, null, tint = akzentFarbe, modifier = Modifier.size(16.dp))
                Text("Schlummert bis ${formatClock(schlummerBis)}", Modifier.weight(1f).padding(start = 7.dp),
                    color = akzentFarbe, style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                NebenMarker(warnung != null, Icons.Default.Warning, "Außerdem ein Warnhinweis, Details aufklappen", warnFarbe)
                NebenMarker(schlaf != null, Icons.Default.Bedtime, "Außerdem ein Schlafhinweis, Details aufklappen", leerFarbe)
            }
            warnung != null -> {
                Icon(Icons.Default.Warning, null, tint = warnFarbe, modifier = Modifier.size(16.dp))
                Text(warnung, Modifier.weight(1f).padding(start = 7.dp), color = warnFarbe,
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                NebenMarker(schlaf != null, Icons.Default.Bedtime, "Außerdem ein Schlafhinweis, Details aufklappen", leerFarbe)
            }
            schlaf != null -> {
                Icon(Icons.Default.Bedtime, null, tint = leerFarbe, modifier = Modifier.size(16.dp))
                Text(schlaf, Modifier.weight(1f).padding(start = 7.dp), color = ruhigFarbe,
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            else -> {
                Icon(
                    if (hatSonne) Icons.Default.WbTwilight else Icons.Default.MusicNote,
                    null,
                    tint = leerFarbe.copy(alpha = 0.85f),
                    modifier = Modifier.size(15.dp),
                )
                Text(meta, Modifier.weight(1f).padding(start = 6.dp),
                    color = leerFarbe, style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Ein nachrangiger Hinweis als Symbol am Zeilenende — sichtbar, ohne die Zeile zu sprengen. */
@Composable
private fun NebenMarker(
    zeigen: Boolean, symbol: androidx.compose.ui.graphics.vector.ImageVector,
    beschreibung: String, farbe: androidx.compose.ui.graphics.Color,
) {
    if (zeigen) Icon(symbol, beschreibung, tint = farbe, modifier = Modifier.padding(start = 6.dp).size(16.dp))
}

/**
 * Traumraum: eine geschwungene Kuppel trägt die Uhrzeit und das Kissenmotiv, darunter überlappt die
 * Perle mit dem tatsächlichen nächsten Termin. Inhalt und Rückrufe sind dieselben wie in jedem
 * anderen Design — nur die Anordnung gehört zum Entwurf.
 */
/**
 * Traumraum legt Editor und Einstellungen als großes Blatt an: ein oben gerundeter Bogen mit Griff,
 * der unter der Kopfzeile ansetzt. Der Inhalt bleibt derselbe und scrollt weiter vollständig; es ist
 * bewusst kein echtes Modal, das einen Entwurf verwerfen könnte.
 */
@Composable
fun DesignBlatt(inhalt: @Composable ColumnScope.() -> Unit) {
    val gold = LocalGold.current
    val traum = LocalDesignTokens.current.design == Design.TRAUMRAUM
    // Rundum abgerundet: Die Blase beginnt oben rund und endet unten ebenso rund.
    val form = RoundedCornerShape(38.dp)
    // Genau eine Aufrufstelle für Column und für den Inhalt: nur der Modifier und der Griff hängen
    // am Design. Ein Wechsel zu oder von Traumraum lässt damit die Zusammensetzung stehen, statt den
    // Unterbaum zu verwerfen — aufgeklappte Karten bleiben offen.
    // Das Blatt trug bisher nur Farbe und einen Strich. Damit blieben Traumraums Editor und
    // Einstellungen die einzigen großen Flächen der App ganz ohne Tiefe — ausgerechnet dort, wo
    // man sich am längsten aufhält. Jetzt bekommt es dieselbe Schichtung wie jede andere Fläche:
    // deckende Grundfarbe, Tiefenverlauf, gerichteter Reflex, Kante zuletzt.
    val material = LocalMaterial.current
    // Das Blatt ist die große Karte unter Editor und Einstellungen und folgt deshalb der Kartendeckkraft.
    val deckkraft = LocalKartenDeckkraft.current
    Column(
        Modifier.fillMaxWidth().then(
            if (traum) Modifier
                // Breiter: Das Blatt nutzt 8 dp des Seitenrands mit, damit der Inhalt nicht gequetscht wirkt.
                .layout { messbar, grenzen ->
                    val extra = 8.dp.roundToPx()
                    val breite = grenzen.maxWidth + extra * 2
                    val platz = messbar.measure(grenzen.copy(minWidth = breite, maxWidth = breite))
                    layout(grenzen.maxWidth, platz.height) { platz.place(-extra, 0) }
                }
                .padding(bottom = 12.dp)
                .clip(form)
                .background(gold.flaeche.deck(deckkraft))
                .tiefenVerlauf(material.tiefenOben * deckkraft, material.tiefenUnten * deckkraft)
                .gerichteterReflex(material.reflexFarbe, material.reflexAlpha * deckkraft,
                    material.reflexWinkelGrad, material.reflexLaenge)
                .border(1.dp, materialKante(material.kanteLichtFarbe, material.kanteLichtAlpha * deckkraft,
                    material.kanteSchattenAlpha * deckkraft), form)
                .padding(start = 0.dp, end = 0.dp, top = 10.dp, bottom = 22.dp)
            else Modifier,
        ),
    ) {
        if (traum) Box(Modifier.fillMaxWidth().padding(bottom = 14.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(46.dp).height(5.dp).clip(RoundedCornerShape(50)).background(gold.rahmen))
        }
        inhalt()
    }
}

/** Kompakte 7-Tage-Punktzeile (M D M D F S S) auf Weckerkarten — Wochentage auf einen Blick erkennbar. */
@Composable
private fun WochentagPunkte(
    tage: Set<Int>,
    aktiv: Boolean,
    akzent: androidx.compose.ui.graphics.Color,
    gedaempft: androidx.compose.ui.graphics.Color,
) {
    val kuerzel = listOf("M", "D", "M", "D", "F", "S", "S")
    Row(
        Modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        kuerzel.forEachIndexed { i, buchstabe ->
            val drin = (i + 1) in tage
            val punktFarbe = when {
                !drin -> gedaempft.copy(alpha = 0.22f)
                aktiv -> akzent
                else -> gedaempft.copy(alpha = 0.65f)
            }
            Box(
                Modifier.size(18.dp)
                    .clip(CircleShape)
                    .background(if (drin) punktFarbe.copy(alpha = if (aktiv) 0.22f else 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
                    .border(
                        width = if (drin) 1.dp else 0.7f.dp,
                        color = punktFarbe,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    buchstabe,
                    fontSize = 9.sp,
                    lineHeight = 9.sp,
                    fontWeight = if (drin) FontWeight.Bold else FontWeight.Normal,
                    color = if (drin) (if (aktiv) akzent else gedaempft) else gedaempft.copy(alpha = 0.42f),
                )
            }
        }
    }
}

/**
 * Traumraum fasst Termin, Name und Restzeit in eine eigene runde Fläche — die „Perle" des Entwurfs.
 * Alle anderen Designs geben denselben Inhalt unverändert weiter.
 */
@Composable
private fun Perle(inhalt: @Composable ColumnScope.() -> Unit) {
    val gold = LocalGold.current
    val form = RoundedCornerShape(28.dp)
    val deckkraft = LocalKartenDeckkraft.current
    Box(Modifier.fillMaxWidth()
        .deckSchatten(gold.primaer, Hoehe.schwebendeLeiste, form, deckkraft)
        .clip(form)
        .background(Brush.verticalGradient(listOf(gold.flaecheErhoeht.heller(0.08f), gold.flaecheErhoeht, gold.flaecheErhoeht.dunkler(0.10f)).map { it.deck(deckkraft) }))
        .glanzBogen(deckung = (if (gold.istDunkel) 0.09f else 0.22f) * deckkraft)
        .heroFase3D(
            form = form,
            kante = gold.rahmen,
            licht = if (gold.istDunkel) gold.akzentWarm else androidx.compose.ui.graphics.Color.White,
            lichtAlpha = if (gold.istDunkel) 0.34f else 0.65f,
            schattenAlpha = if (gold.istDunkel) 0.45f else 0.22f,
            deckkraft = deckkraft,
        )
        .padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = inhalt)
    }
}

/**
 * Eine Station an der Tagesachse: nur Morgenruhe zeichnet Linie und Punkt, alle anderen Designs
 * geben den Inhalt unverändert weiter. Die Karte selbst bleibt in jedem Fall dieselbe.
 */
/**
 * @param modifier gehört an die **äußere** Wurzel dieses Listeneintrags. Lazy-Layouts lesen die
 *   ParentData am Wurzelknoten des Items; lag `animateItem()` weiter innen an der Karte, sah das
 *   Raster ihn bei Morgenruhe gar nicht — dort fehlten deshalb Ein-, Ausblend- und
 *   Verschiebeanimationen, die die anderen drei Designs hatten.
 */
@Composable
private fun Station(anAchse: Boolean, aktiv: Boolean, modifier: Modifier = Modifier,
    inhalt: @Composable () -> Unit) {
    if (!anAchse) { Box(modifier) { inhalt() }; return }
    val gold = LocalGold.current
    // Die Linie läuft über die volle Höhe jeder Station und damit ohne Unterbrechung durch die Liste;
    // der Punkt sitzt auf Höhe der Weckzeit. Der eigene Abstand liegt innerhalb der Station.
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Max), verticalAlignment = Alignment.Top) {
        Box(Modifier.width(26.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(gold.rahmen))
            Box(Modifier.padding(top = 30.dp).size(13.dp)
                .background(if (aktiv) gold.primaer else gold.hintergrund, androidx.compose.foundation.shape.CircleShape)
                .border(2.dp, if (aktiv) gold.primaer else gold.rahmen, androidx.compose.foundation.shape.CircleShape))
        }
        // Der Abstand liegt innerhalb der Station; die Linie daneben zeichnet ungebrochen weiter,
        // so kleben die Karten trotz spacedBy(0) nicht aneinander.
        Box(Modifier.weight(1f).padding(start = 10.dp, top = 10.dp, bottom = 10.dp)) { inhalt() }
    }
}

/**
 * Kopfzeile je Design. Schlicht, Morgenruhe und Traumraum behalten die vertraute Leiste;
 * Orbit setzt eine kantige Instrumentenzeile mit fester Schrift darüber. Titel, Zurück-Knopf,
 * Hell/Dunkel-Umschalter und das Zahnrad bleiben in allen Designs an derselben Stelle.
 */
/**
 * Die Schrift der großen Zahlen je Design: Orbit setzt sie fest wie auf einer Instrumententafel,
 * Morgenruhe und Traumraum nehmen die ruhige Grotesk ihrer Entwürfe, Schlicht bleibt bei der
 * bisherigen Serifenschrift.
 */
@Composable
fun zahlSchrift() = when (LocalDesignTokens.current.design) {
    Design.ORBIT -> IdeenSchriftFest
    Design.MORGENRUHE, Design.TRAUMRAUM -> IdeenSchrift
    else -> IdeenSchriftBetont
}

/**
 * Das Gewicht der großen Zahlen. Schlicht bekommt bewusst `null` — dort bleibt die Serifenschrift
 * unverändert ohne eigene Gewichtsangabe. Traumraum setzt kräftig, Orbit halbfett, Morgenruhe leicht.
 */
@Composable
fun zahlGewicht(): FontWeight? = when (LocalDesignTokens.current.design) {
    Design.TRAUMRAUM -> FontWeight.Bold
    Design.ORBIT -> FontWeight.SemiBold
    Design.MORGENRUHE -> FontWeight.Light
    else -> null
}

@Composable
fun Section(
    title: String,
    collapsible: Boolean = false,
    summary: String = "",
    error: String? = null,
    initiallyExpanded: Boolean = !collapsible,
    status: String? = null,
    statusFarbe: androidx.compose.ui.graphics.Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Only visibility is remembered here; all values live in the draft in the ViewModel.
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded || !collapsible) }
    var groesseAnimieren by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(450); groesseAnimieren = true }
    val sanftWachsen = if (groesseAnimieren) Modifier.animateContentSize() else Modifier
    val gold = LocalGold.current
    val entwurf = LocalDesignTokens.current.design
    val haptik = LocalHapticFeedback.current
    val umschalten = {
        haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        expanded = !expanded
    }

    val kopfModifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .then(if (collapsible) Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button,
            onClickLabel = if (expanded) "$title zuklappen" else "$title aufklappen",
            onClick = umschalten)
            .semantics { stateDescription = if (expanded) "Aufgeklappt" else "Zugeklappt" } else Modifier)
    val beschriftung = @Composable { titelStil: androidx.compose.ui.text.TextStyle, titelFarbe: androidx.compose.ui.graphics.Color ->
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, Modifier.weight(1f, fill = false), style = titelStil, color = titelFarbe, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!status.isNullOrBlank()) {
                    val pilleFarbe = statusFarbe ?: gold.primaer
                    val pilleForm = RoundedCornerShape(LocalDesignTokens.current.chipRadius)
                    Text(
                        status,
                        Modifier.clip(pilleForm)
                            .background(pilleFarbe.copy(alpha = 0.16f))
                            .border(1.dp, pilleFarbe.copy(alpha = 0.42f), pilleForm)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = pilleFarbe,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // Morgenruhe zeigt die Zusammenfassung in seiner Fläche, nicht unter dem Titel.
            if (!expanded && summary.isNotBlank() && entwurf != Design.MORGENRUHE)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
            // A missing required input is shown in its own card, also while collapsed.
            if (error != null) Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = LocalSemantisch.current.warnung, modifier = Modifier.size(18.dp))
                Text(error, Modifier.padding(start = 6.dp), color = LocalSemantisch.current.warnung, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    when (entwurf) {
        Design.MORGENRUHE -> LocalGestalt.current.Flaeche(Modifier.fillMaxWidth().auftritt(), erhoeht = false) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp).then(sanftWachsen),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(kopfModifier.heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        beschriftung(MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), gold.textPrimaer)
                        if (!expanded && summary.isNotBlank()) Text(summary,
                            style = MaterialTheme.typography.bodyMedium, color = gold.textGedaempft)
                    }
                    if (collapsible) KlappKnopf(expanded, { expanded = !expanded }, beschreibung = null, modifier = Modifier.padding(start = 12.dp))
                }
                if (expanded) content()
            }
        }
        Design.ORBIT -> LocalGestalt.current.Flaeche(Modifier.fillMaxWidth().auftritt(), erhoeht = false) {
            Column(Modifier.then(sanftWachsen)) {
                Row(kopfModifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                        beschriftung(MaterialTheme.typography.labelLarge.copy(fontFamily = IdeenSchriftFest, letterSpacing = 1.5.sp), gold.primaer)
                    }
                    if (collapsible) {
                        VerticalDivider(Modifier.padding(vertical = 3.dp), color = gold.rahmen)
                        Box(Modifier.fillMaxHeight().padding(top = 3.dp, bottom = 3.dp, end = 3.dp).width(49.dp)
                            .background(gold.primaer.copy(alpha = .06f), RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                            .clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null,
                                tint = gold.primaer, modifier = Modifier.size(26.dp))
                        }
                    }
                }
                if (expanded) {
                    HorizontalDivider(color = gold.rahmen)
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
                }
            }
        }
        Design.TRAUMRAUM -> {
            val material = LocalMaterial.current
            val abschnittForm = RoundedCornerShape(LocalDesignTokens.current.karteRadius)
            Column(Modifier.fillMaxWidth().auftritt().padding(horizontal = 8.dp).then(sanftWachsen),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(kopfModifier.padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        beschriftung(MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), gold.textPrimaer)
                    }
                    if (collapsible) KlappKnopf(expanded, { expanded = !expanded }, beschreibung = null, modifier = Modifier.padding(start = 8.dp))
                }
                val d = LocalKartenDeckkraft.current
                val schatten = (material.schattenFarbe ?: androidx.compose.ui.graphics.Color.Black).deck(d * d)
                if (expanded) Box(
                    Modifier.fillMaxWidth()
                        .then(if (d > 0f) Modifier.shadow(3.dp, abschnittForm, ambientColor = schatten, spotColor = schatten) else Modifier)
                        .clip(abschnittForm)
                        .background(gold.flaecheErhoeht.deck(d))
                        .tiefenVerlauf(material.tiefenOben * d, material.tiefenUnten * d)
                        .border(1.dp, materialKante(material.kanteLichtFarbe, material.kanteLichtAlpha * .8f * d, material.kanteSchattenAlpha * .6f * d), abschnittForm)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
                }
            }
        }
        else -> LocalGestalt.current.Flaeche(Modifier.fillMaxWidth().auftritt(), erhoeht = false) {
            Column(Modifier.padding(18.dp).then(sanftWachsen), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(kopfModifier, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { beschriftung(MaterialTheme.typography.titleMedium, gold.primaer) }
                    if (collapsible) KlappKnopf(expanded, { expanded = !expanded }, beschreibung = null, modifier = Modifier.padding(start = 8.dp))
                }
                if (expanded) content()
            }
        }
    }
}

@Composable
private fun AlarmEditor(vm: WeckerViewModel, alarm: Alarm, activity: ComponentActivity) {
    val gold = LocalGold.current
    val semantisch = LocalSemantisch.current
    val haptikEditor = LocalHapticFeedback.current
    val reduziert = LocalBewegungReduziert.current
    val recording by vm.recording.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val resumed = rememberResumed()
    val musikInfo = remember(alarm.music, alarm.musicName, alarm.musicQuelle, alarm.tone, resumed) { MusikAnzeige.von(alarm) }
    var musikModusName by rememberSaveable(alarm.id) { mutableStateOf(musikInfo.art.name) }
    val musikModus = runCatching { MusikArt.valueOf(musikModusName) }.getOrDefault(MusikArt.EINGEBAUT)
    var mp3AbbruchHinweis by rememberSaveable(alarm.id) { mutableStateOf("") }
    // Sobald eine echte Datei oder ein Geräte-Weckton gewählt wurde, folgt der aktive Reiter automatisch.
    LaunchedEffect(alarm.music, alarm.musicQuelle) {
        if (alarm.music.isNotBlank()) {
            musikModusName = if (alarm.musicQuelle == "geraet") MusikArt.GERAET.name else MusikArt.MP3.name
            mp3AbbruchHinweis = ""
        }
    }
    val music = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            mp3AbbruchHinweis = ""
            musikModusName = MusikArt.MP3.name
            vm.importMusic(uri, "datei")
        } else {
            // Ruhiger Inline-Hinweis direkt an der Auswahlstelle statt stehendem Fehlerbanner oben.
            mp3AbbruchHinweis = "Auswahl abgebrochen – bisheriger Weckton bleibt aktiv."
        }
    }
    val ringtone = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        @Suppress("DEPRECATION")
        val uri = result.data?.getParcelableExtra<Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (uri != null) {
            mp3AbbruchHinweis = ""
            musikModusName = MusikArt.GERAET.name
            vm.importMusic(uri, "geraet")
        } else {
            mp3AbbruchHinweis = "Auswahl abgebrochen – bisheriger Weckton bleibt aktiv."
        }
    }
    var photoPath by rememberSaveable { mutableStateOf("") }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoPath.isNotBlank()) vm.referencePhoto(File(photoPath))
    }
    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) vm.startRecording() else vm.message.value = "Für das Diktat wird die Mikrofonberechtigung benötigt."
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = seitenAbstandOben(), bottom = 16.dp).navigationBarsPadding()) {
      DesignBlatt {
       Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Der Name steht immer ganz oben, auch wenn der Abschnitt darunter zugeklappt ist; nur die Beschriftung ist fett.
        Eingabefeld(alarm.name, { vm.change(alarm.copy(name = it)) }, "Name des Weckers", Modifier.fillMaxWidth(), beschriftungFett = true)
        val minute = rememberNow(60_000)
        OffenesDiktatKarte(vm, alarm)
        if (remember(alarm, minute) { alarm.isExpiredOnce() }) LocalGestalt.current.Flaeche(Modifier.fillMaxWidth(), erhoeht = false) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EventBusy, null, tint = semantisch.warnung)
                Text("Dieses Datum liegt in der Vergangenheit. Wähle einen neuen Termin – beim Speichern wird der Wecker eingeschaltet.",
                    Modifier.padding(start = 12.dp), color = semantisch.warnung, style = MaterialTheme.typography.bodyMedium)
            }
        }
        // Kept outside the section: its content is removed while collapsed, the remembered date must survive that.
        var gemerktesDatum by rememberSaveable(alarm.id) { mutableStateOf(alarm.startDate) }
        val naechsterTermin = remember(alarm, minute) {
            runCatching { AlarmTime.next(alarm, Instant.ofEpochMilli(minute)) }.getOrNull()
        }
        Section(
            title = "Weckzeit & Wiederholung",
            collapsible = true,
            initiallyExpanded = true,
            status = alarm.timeLabel,
            summary = listOfNotNull(alarm.timeLabel, scheduleLabel(alarm),
                if (alarm.sleepMinutes > 0) "Schlafdauer ${Schlaf.dauer(alarm.sleepMinutes)}" else null).joinToString(" · "),
        ) {
            var zeitWahlOffen by rememberSaveable(alarm.id) { mutableStateOf(false) }
            if (zeitWahlOffen) ZeitWahlDialog(
                stunde = (alarm.sonnenMinute()?.takeIf { alarm.sunMode.isNotBlank() } ?: (alarm.hour * 60 + alarm.minute)) / 60,
                minute = (alarm.sonnenMinute()?.takeIf { alarm.sunMode.isNotBlank() } ?: (alarm.hour * 60 + alarm.minute)) % 60,
                aufAbbruch = { zeitWahlOffen = false },
                aufWahl = { stunde, minuteWahl -> zeitWahlOffen = false; vm.change(alarm.copy(hour = stunde, minute = minuteWahl, sunMode = "")) },
            )
            val pickTime = {
                haptikEditor.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                zeitWahlOffen = true
            }
            fun verschiebeMinuten(delta: Int) {
                val gesamt = ((alarm.hour * 60 + alarm.minute + delta) % 1440 + 1440) % 1440
                vm.change(alarm.copy(hour = gesamt / 60, minute = gesamt % 60, sunMode = ""))
            }
            // Plastische 3D-Zeitbühne: große, direkt antippbare Uhrzeit mit Live-Restzeit und ±15-Minuten-Schnelltasten.
            val zeitForm = RoundedCornerShape(LocalDesignTokens.current.karteRadius.coerceAtMost(22.dp))
            Box(
                Modifier.fillMaxWidth()
                    .clip(zeitForm)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                gold.flaecheErhoeht.heller(if (gold.istDunkel) 0.08f else 0.03f),
                                gold.flaecheErhoeht,
                                gold.flaecheErhoeht.dunkler(if (gold.istDunkel) 0.12f else 0.05f),
                            ),
                        ),
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                gold.primaer.copy(alpha = if (gold.istDunkel) 0.45f else 0.50f),
                                gold.rahmen.copy(alpha = 0.45f),
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = if (gold.istDunkel) 0.40f else 0.12f),
                            ),
                        ),
                        zeitForm,
                    )
                    .clickable(onClickLabel = "Uhrzeit ändern", onClick = pickTime)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            if (alarm.sunMode.isNotBlank()) Icons.Default.WbTwilight else Icons.Default.Schedule,
                            null,
                            tint = gold.primaer,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            when {
                                alarm.sunMode.isNotBlank() -> "Sonnenaufgangswecker · tippen für feste Zeit"
                                naechsterTermin != null -> "Klingelt ${terminAnzeige(minute, naechsterTermin).einzeilig} · in ${remainingLong(naechsterTermin - minute)}"
                                else -> "Tippen zum Einstellen der Uhrzeit"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = gold.textGedaempft,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                    GedeckelteSchrift {
                        Text(
                            alarm.timeLabel,
                            fontSize = 56.sp,
                            fontFamily = zahlSchrift(),
                            fontWeight = zahlGewicht(),
                            color = gold.primaer,
                            maxLines = 1,
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (alarm.sunMode.isBlank()) {
                            StillerKnopf("−15 Min.", { verschiebeMinuten(-15) })
                        }
                        GoldKnopf(if (alarm.sunMode.isBlank()) "Uhrzeit ändern" else "Feste Uhrzeit", pickTime)
                        if (alarm.sunMode.isBlank()) {
                            StillerKnopf("+15 Min.", { verschiebeMinuten(15) })
                        }
                    }
                }
            }
            SonnenWahl(alarm, vm::change)
            HorizontalDivider(Modifier.padding(vertical = 2.dp), color = gold.rahmen)
            // Wiederholung direkt nach der Uhrzeit — die zweithäufigste Einstellung ohne Scrollen erreichbar.
            RepeatEditor(alarm, activity, gemerktesDatum, { gemerktesDatum = it }, vm::change)
            HorizontalDivider(Modifier.padding(vertical = 2.dp), color = gold.rahmen)
            // Schlafdauer kompakt darunter, damit sie nicht vor die Wiederholung drängt.
            SchlafdauerEingabe(alarm, vm::change)
        }
        val ablaufFehler = when {
            alarm.steps.isEmpty() -> "Wähle mindestens einen Weckschritt."
            Step.TEXT in alarm.steps && alarm.text.isBlank() -> "Der Erinnerungstext fehlt."
            else -> null
        }
        val ablaufStatus = when {
            Step.MUSIC in alarm.steps && musikInfo.ersatz -> "MP3 fehlt"
            Step.MUSIC in alarm.steps -> MusikAnzeige.kurz(alarm)
            alarm.steps.isNotEmpty() -> "${alarm.steps.size} ${if (alarm.steps.size == 1) "Schritt" else "Schritte"}"
            else -> "Kein Schritt"
        }
        Section(
            title = "Weckton, MP3 & Ablauf",
            collapsible = true,
            initiallyExpanded = true,
            status = ablaufStatus,
            statusFarbe = if (Step.MUSIC in alarm.steps && musikInfo.ersatz) semantisch.warnung else gold.primaer,
            summary = listOfNotNull(
                if (Step.MUSIC in alarm.steps) MusikAnzeige.kurz(alarm) else null,
                alarm.steps.joinToString(" → ") { it.title }.ifBlank { "Kein Schritt gewählt" },
            ).distinct().joinToString(" · "),
            error = ablaufFehler,
        ) {
            Text(
                "Aktiviere die gewünschten Bausteine – alle Einstellungen dazu öffnen sich direkt an Ort und Stelle. Songs laufen vollständig durch.",
                style = MaterialTheme.typography.bodySmall,
                color = gold.textGedaempft,
            )
            var aufgabenOffen by rememberSaveable(alarm.id) { mutableStateOf(false) }
            var ideenOffen by rememberSaveable(alarm.id) { mutableStateOf(false) }
            val ideen by vm.ideas.collectAsStateWithLifecycle()
            val ideenDeaktiviert by vm.ideasDisabled.collectAsStateWithLifecycle()
            val aktiveIdeenAnzahl = remember(ideen, ideenDeaktiviert) { ideen.count { it.id !in ideenDeaktiviert } }
            val schrittAnzeigeReihenfolge = listOf(Step.MUSIC, Step.TONE, Step.TEXT, Step.IDEAS, Step.TASKS)
            schrittAnzeigeReihenfolge.forEach { step ->
                val aktiv = step in alarm.steps
                val untertitel = when (step) {
                    Step.MUSIC -> when {
                        !aktiv -> "Eingebauter Weckton, eigene MP3-Datei oder Geräte-Ton"
                        musikInfo.ersatz -> "Datei fehlt – Ersatzton aktiv"
                        musikInfo.art == MusikArt.MP3 -> "MP3: ${musikInfo.titel}"
                        musikInfo.art == MusikArt.GERAET -> "Gerät: ${musikInfo.titel}"
                        else -> "Weckton: ${musikInfo.titel}"
                    }
                    Step.TONE -> if (!aktiv) "Kurzes Signal vor dem Vorlesen oder der Musik"
                        else Tones.names[alarm.cue] ?: alarm.cue
                    Step.TEXT -> when {
                        !aktiv -> "Persönliche Nachricht wird vorgelesen"
                        alarm.text.isBlank() -> "Text fehlt noch"
                        else -> alarm.text.trim().replace('\n', ' ').take(48)
                    }
                    Step.IDEAS -> if (!aktiv) "Offene Ideen aus Geniale Ideen vorlesen"
                        else "$aktiveIdeenAnzahl aktive ${if (aktiveIdeenAnzahl == 1) "Idee" else "Ideen"}" +
                            if (alarm.ideasRepeats > 1) " · ${alarm.ideasRepeats}×" else ""
                    Step.TASKS -> if (!aktiv) "Offene Aufgaben für den Wecktag vorlesen"
                        else "Offene Aufgaben für ${aufgabenTag(TasksBridge.klingeltag(alarm, ausPlan = true))}" +
                            if (alarm.tasksRepeats > 1) " · ${alarm.tasksRepeats}×" else ""
                }
                val symbol = when (step) {
                    Step.MUSIC -> Icons.Default.MusicNote
                    Step.TONE -> Icons.Default.NotificationsActive
                    Step.TEXT -> Icons.Default.RecordVoiceOver
                    Step.IDEAS -> Icons.Default.Lightbulb
                    Step.TASKS -> Icons.Default.CheckCircle
                }
                val fehlerZeile = (step == Step.TEXT && aktiv && alarm.text.isBlank()) ||
                    (step == Step.MUSIC && aktiv && musikInfo.ersatz)
                BausteinKarte(
                    titel = step.title,
                    untertitel = untertitel,
                    symbol = symbol,
                    aktiv = aktiv,
                    warnung = fehlerZeile,
                    aufWechsel = { checked ->
                        vm.change(alarm.copy(steps = if (checked) alarm.steps + step else alarm.steps - step))
                        if (checked && step == Step.TASKS) { vm.loadTasks(alarm); aufgabenOffen = true }
                        if (checked && step == Step.IDEAS) { vm.syncIdeasStill() }
                    },
                ) {
                    when (step) {
                        Step.MUSIC -> {
                            // 3-Wege-Wahl direkt im Schritt „MP3 / Weckton“: Eingebauter Ton · MP3-Datei · Geräte-Ton
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Chip3D(musikModus == MusikArt.EINGEBAUT, {
                                    musikModusName = MusikArt.EINGEBAUT.name
                                    mp3AbbruchHinweis = ""
                                    if (alarm.music.isNotBlank()) {
                                        val tonName = Tones.names[alarm.tone] ?: Tones.names.getValue("classic")
                                        vm.change(alarm.copy(music = "", musicName = tonName, musicQuelle = ""))
                                    }
                                }, "Eingebauter Ton")
                                Chip3D(musikModus == MusikArt.MP3, {
                                    musikModusName = MusikArt.MP3.name
                                    mp3AbbruchHinweis = ""
                                }, "MP3-Datei")
                                Chip3D(musikModus == MusikArt.GERAET, {
                                    musikModusName = MusikArt.GERAET.name
                                    mp3AbbruchHinweis = ""
                                }, "Geräte-Weckton")
                            }
                            when (musikModus) {
                                MusikArt.EINGEBAUT -> {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Tones.names.forEach { (id, name) ->
                                            Chip3D(alarm.music.isBlank() && alarm.tone == id, {
                                                vm.change(alarm.copy(tone = id, music = "", musicName = name, musicQuelle = ""))
                                            }, name)
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AnhoerKnopf(vm, "weckton:${alarm.tone}") { vm.playTone(alarm.tone, alarm.volume, alarm.fadeSeconds) }
                                        Text(
                                            "Läuft direkt offline auf dem Gerät.",
                                            Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = gold.textGedaempft,
                                        )
                                    }
                                }
                                MusikArt.MP3 -> {
                                    val hatGueltigeMp3 = musikInfo.art == MusikArt.MP3 && !musikInfo.ersatz
                                    val mp3Fehlt = musikInfo.art == MusikArt.MP3 && musikInfo.ersatz
                                    when {
                                        hatGueltigeMp3 -> {
                                            val dateiForm = RoundedCornerShape(LocalDesignTokens.current.chipRadius.coerceAtMost(14.dp))
                                            Row(
                                                Modifier.fillMaxWidth()
                                                    .clip(dateiForm)
                                                    .background(gold.primaer.copy(alpha = 0.12f))
                                                    .border(1.dp, gold.primaer.copy(alpha = 0.38f), dateiForm)
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                Icon(Icons.Default.AudioFile, null, tint = gold.primaer, modifier = Modifier.size(22.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(musikInfo.titel, style = MaterialTheme.typography.titleSmall, color = gold.textPrimaer, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                    Text("Gespeicherte MP3-Datei · offline bereit", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                                                }
                                            }
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                GoldKnopf("MP3-Datei wechseln", { music.launch(arrayOf("audio/*")) },
                                                    symbol = { Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp)) })
                                                AnhoerKnopf(vm, "musik:mp3") { vm.playMusic(alarm.music, alarm.volume, alarm.fadeSeconds) }
                                            }
                                        }
                                        mp3Fehlt -> {
                                            val warnForm = RoundedCornerShape(LocalDesignTokens.current.chipRadius.coerceAtMost(14.dp))
                                            Column(
                                                Modifier.fillMaxWidth()
                                                    .clip(warnForm)
                                                    .background(semantisch.warnung.copy(alpha = 0.12f))
                                                    .border(1.dp, semantisch.warnung.copy(alpha = 0.45f), warnForm)
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Icon(Icons.Default.Warning, null, tint = semantisch.warnung, modifier = Modifier.size(20.dp))
                                                    Text(
                                                        "„${musikInfo.fehlenderName}“ ist nicht mehr verfügbar. Beim Wecken erklingt als Ersatz der klassische Weckton.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = semantisch.warnung,
                                                    )
                                                }
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    GoldKnopf("MP3-Datei auswählen", { music.launch(arrayOf("audio/*")) },
                                                        symbol = { Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp)) })
                                                    StillerKnopf("Eingebauten Ton nutzen", {
                                                        musikModusName = MusikArt.EINGEBAUT.name
                                                        val tonName = Tones.names[alarm.tone] ?: Tones.names.getValue("classic")
                                                        vm.change(alarm.copy(music = "", musicName = tonName, musicQuelle = ""))
                                                    })
                                                }
                                            }
                                        }
                                        else -> {
                                            // Unmittelbar an derselben Stelle: Aktion „MP3-Datei auswählen“!
                                            GoldKnopf(
                                                "MP3-Datei auswählen",
                                                { music.launch(arrayOf("audio/*")) },
                                                Modifier.fillMaxWidth(),
                                                symbol = { Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp)) },
                                            )
                                            Text(
                                                if (alarm.music.isBlank()) "Noch keine MP3-Datei gewählt – bis zur Auswahl erklingt „${Tones.names[alarm.tone] ?: "Klassischer Wecker"}“."
                                                else "Aktuell noch aktiv: ${musikInfo.titel} (${musikInfo.quelle}). Wähle eine MP3-Datei zum Wechseln.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = gold.textGedaempft,
                                            )
                                        }
                                    }
                                    if (mp3AbbruchHinweis.isNotBlank()) {
                                        StatusZeile(Icons.Default.Info, mp3AbbruchHinweis, gold.textGedaempft)
                                    }
                                }
                                MusikArt.GERAET -> {
                                    val hatGueltigenGeraetTon = musikInfo.art == MusikArt.GERAET && !musikInfo.ersatz
                                    val geraetFehlt = musikInfo.art == MusikArt.GERAET && musikInfo.ersatz
                                    val oeffneRingtone = {
                                        ringtone.launch(Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER)
                                            .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM)
                                            .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false))
                                    }
                                    when {
                                        hatGueltigenGeraetTon -> {
                                            val dateiForm = RoundedCornerShape(LocalDesignTokens.current.chipRadius.coerceAtMost(14.dp))
                                            Row(
                                                Modifier.fillMaxWidth()
                                                    .clip(dateiForm)
                                                    .background(gold.primaer.copy(alpha = 0.12f))
                                                    .border(1.dp, gold.primaer.copy(alpha = 0.38f), dateiForm)
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                Icon(Icons.Default.LibraryMusic, null, tint = gold.primaer, modifier = Modifier.size(22.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(musikInfo.titel, style = MaterialTheme.typography.titleSmall, color = gold.textPrimaer, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                    Text("Geräte-Weckton · lokal gesichert", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                                                }
                                            }
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                GoldKnopf("Geräte-Weckton wechseln", oeffneRingtone)
                                                AnhoerKnopf(vm, "musik:geraet") { vm.playMusic(alarm.music, alarm.volume, alarm.fadeSeconds) }
                                            }
                                        }
                                        geraetFehlt -> {
                                            val warnForm = RoundedCornerShape(LocalDesignTokens.current.chipRadius.coerceAtMost(14.dp))
                                            Column(
                                                Modifier.fillMaxWidth()
                                                    .clip(warnForm)
                                                    .background(semantisch.warnung.copy(alpha = 0.12f))
                                                    .border(1.dp, semantisch.warnung.copy(alpha = 0.45f), warnForm)
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                StatusZeile(Icons.Default.Warning, "„${musikInfo.fehlenderName}“ ist nicht mehr verfügbar. Beim Wecken erklingt als Ersatz der klassische Weckton.", semantisch.warnung)
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    GoldKnopf("Geräte-Weckton auswählen", oeffneRingtone)
                                                    StillerKnopf("Eingebauten Ton nutzen", {
                                                        musikModusName = MusikArt.EINGEBAUT.name
                                                        val tonName = Tones.names[alarm.tone] ?: Tones.names.getValue("classic")
                                                        vm.change(alarm.copy(music = "", musicName = tonName, musicQuelle = ""))
                                                    })
                                                }
                                            }
                                        }
                                        else -> {
                                            GoldKnopf("Geräte-Weckton auswählen", oeffneRingtone, Modifier.fillMaxWidth())
                                            Text("Wähle einen der installierten Wecktöne deines Handys.", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                                        }
                                    }
                                    if (mp3AbbruchHinweis.isNotBlank()) {
                                        StatusZeile(Icons.Default.Info, mp3AbbruchHinweis, gold.textGedaempft)
                                    }
                                }
                            }
                        }
                        Step.TONE -> {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Tones.names.forEach { (id, title) ->
                                    Chip3D(alarm.cue == id, { vm.change(alarm.copy(cue = id)) }, title)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AnhoerKnopf(vm, "ton:${alarm.cue}") { vm.playTone(alarm.cue, alarm.volume, alarm.fadeSeconds) }
                                Text("Kurzes Zeichen vor Sprache oder Musik.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                            }
                        }
                        Step.TEXT -> {
                            if (alarm.text.isBlank()) StatusZeile(Icons.Default.Warning, "Der Erinnerungstext fehlt.", semantisch.warnung)
                            Eingabefeld(alarm.text, { vm.change(alarm.copy(text = it)) }, "Text, der vorgelesen werden soll",
                                Modifier.fillMaxWidth().heightIn(min = 132.dp), einzeilig = false)
                            val diktatRevision by vm.settingsRevision.collectAsStateWithLifecycle()
                            val parakeet = remember(diktatRevision) { de.frank.module.transkriptionsmodul.ParakeetUeberJarvis.gewaehlt(activity) }
                            val lokal = remember(diktatRevision) { vm.settings.diktatWeg == "lokal" } && !parakeet
                            if (lokal) {
                                DiktatUndVorlesen(sprache = DiktatSprache.DE, anfuegen = { diktiert ->
                                    vm.draft.value?.takeIf { it.id == alarm.id }?.let { aktuell ->
                                        vm.change(aktuell.copy(text = listOf(aktuell.text.trimEnd(), diktiert.trim()).filter(String::isNotBlank).joinToString("\n")))
                                    }
                                }) { AnhoerKnopfText(vm, alarm) }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StillerKnopf("Text verbessern", vm::improve)
                                    if (alarm.originalText.isNotBlank()) StillerKnopf("Original zurückholen", { vm.change(alarm.copy(text = alarm.originalText, originalText = "")) })
                                }
                                Text("Whisper auf dem Handy – ohne Internet. Die KI-Textverbesserung nutzt dieselbe ChatGPT-Anmeldung wie Geniale Ideen.", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                            } else {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GoldKnopf(if (recording) "■ Diktat abschließen" else "● Diktieren", {
                                        if (recording) vm.stopRecording()
                                        else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.startRecording()
                                        else microphone.launch(Manifest.permission.RECORD_AUDIO)
                                    }, aktiviert = recording || busy.isBlank())
                                    AnhoerKnopfText(vm, alarm)
                                    StillerKnopf("Text verbessern", vm::improve)
                                    if (alarm.originalText.isNotBlank()) StillerKnopf("Original zurückholen", { vm.change(alarm.copy(text = alarm.originalText, originalText = "")) })
                                }
                                Text((if (parakeet) "Parakeet über Jarvis (offline)" else "Groq · Whisper ${de.frank.genialeideen.audio.GroqTranscriber.modellName(vm.settings.groqModell)}") + ". KI-Textverbesserung über ChatGPT.", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                            }
                        }
                        Step.IDEAS -> {
                            WiederholenZeile(alarm.ideasRepeats) { vm.change(alarm.copy(ideasRepeats = it)) }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StillerKnopf("Ideen ansehen & wählen ($aktiveIdeenAnzahl)", { vm.syncIdeasStill(); ideenOffen = true }, hervorgehoben = true)
                            }
                            Text("Liest deine aktiven offenen Ideen aus Geniale Ideen in ihrer Reihenfolge vor.", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                            if (ideenOffen) DesignDialog(
                                titel = "Offene Ideen (${ideen.size})",
                                aufSchliessen = { ideenOffen = false },
                                bestaetigung = { StillerKnopf("Schließen", { ideenOffen = false }) },
                            ) {
                                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (ideen.isEmpty()) {
                                        Text("Noch keine offenen Ideen geladen. Öffne Geniale Ideen oder tippe in den Einstellungen auf „Jetzt abgleichen“.",
                                            style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                                    } else {
                                        ideen.forEach { idea ->
                                            val aktivIdee = idea.id !in ideenDeaktiviert
                                            Row(
                                                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                                    .toggleable(aktivIdee, interactionSource = null, indication = null, role = androidx.compose.ui.semantics.Role.Checkbox) {
                                                        haptikEditor.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        vm.setIdeaActive(idea.id, it)
                                                    },
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Checkbox(aktivIdee, null, colors = CheckboxDefaults.colors(
                                                    checkedColor = gold.primaer, uncheckedColor = gold.textGedaempft, checkmarkColor = gold.aufPrimaer,
                                                ))
                                                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                                    Text(idea.title, style = MaterialTheme.typography.titleSmall,
                                                        color = if (aktivIdee) gold.textPrimaer else gold.textGedaempft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    if (idea.text.isNotBlank()) Text(idea.text, style = MaterialTheme.typography.bodySmall,
                                                        color = gold.textGedaempft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Step.TASKS -> {
                            AufgabenBereich(vm, alarm, aufgabenOffen) { aufgabenOffen = it }
                        }
                    }
                }
            }
            if (alarm.steps.size > 1) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = gold.rahmen)
                Text("Reihenfolge beim Wecken (ziehen zum Sortieren)", style = MaterialTheme.typography.titleSmall, color = gold.primaer)
                de.frank.module.draganddrop.WeckReihenfolgeListe(alarm, vm::change)
            }
        }
        if (alarm.needsSpeech) AlarmSpeechEditor(vm, alarm)
        Section(
            title = "Lautstärke & Schlummern",
            collapsible = true,
            initiallyExpanded = true,
            status = "${alarm.volume} %",
            summary = listOfNotNull(
                "${alarm.volume} % Lautstärke",
                if (alarm.vibrate) "Vibration" else "ohne Vibration",
                if (alarm.snoozeLimit == 0) "Schlummern aus" else "Schlummern ${alarm.snoozeMinutes} Min., bis ${alarm.snoozeLimit}×",
            ).joinToString(" · "),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Wecklautstärke", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = gold.primaer)
                Text("${alarm.volume} %", style = MaterialTheme.typography.titleMedium, fontFamily = zahlSchrift(), fontWeight = FontWeight.Bold, color = gold.primaer)
            }
            Regler3D(
                wert = alarm.volume.toFloat(),
                aufAenderung = { v -> val i = v.roundToInt(); vm.change(alarm.copy(volume = i)); vm.vorschauLautstaerke(i) },
                modifier = Modifier.semantics {
                    contentDescription = "Wecklautstärke"
                    stateDescription = "${alarm.volume} Prozent"
                },
                bereich = 1f..100f,
                stufen = 98,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(40 to "Leise · 40 %", 70 to "Normal · 70 %", 100 to "Laut · 100 %").forEach { (wert, label) ->
                    Chip3D(alarm.volume == wert, { vm.change(alarm.copy(volume = wert)); vm.vorschauLautstaerke(wert) }, label)
                }
            }
            Toggle("Vibrieren", alarm.vibrate) { vm.change(alarm.copy(vibrate = it)) }
            HorizontalDivider(Modifier.padding(vertical = 2.dp), color = gold.rahmen)
            Text("Schlummern", style = MaterialTheme.typography.titleSmall, color = gold.primaer)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip3D(alarm.snoozeLimit == 0, { vm.change(alarm.copy(snoozeLimit = 0)) }, "Aus")
                listOf(5, 10, 15).forEach { min ->
                    Chip3D(alarm.snoozeLimit > 0 && alarm.snoozeMinutes == min, {
                        vm.change(alarm.copy(snoozeMinutes = min, snoozeLimit = alarm.snoozeLimit.coerceAtLeast(3)))
                    }, "$min Min.")
                }
            }
            var feinOffen by rememberSaveable(alarm.id) { mutableStateOf(alarm.fadeSeconds > 0 || (alarm.snoozeLimit > 0 && alarm.snoozeMinutes !in listOf(5, 10, 15))) }
            val haptikFein = LocalHapticFeedback.current
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button) {
                        haptikFein.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        feinOffen = !feinOffen
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Feineinstellungen · Anschwellen ${alarm.fadeSeconds} Sek., max. ${alarm.snoozeLimit}×",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = gold.textGedaempft,
                )
                Icon(
                    if (feinOffen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null,
                    tint = gold.primaer,
                    modifier = Modifier.size(20.dp),
                )
            }
            AnimatedVisibility(
                feinOffen,
                enter = if (reduziert) androidx.compose.animation.EnterTransition.None else androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = if (reduziert) androidx.compose.animation.ExitTransition.None else androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ValueSlider("Sanftes Anschwellen", alarm.fadeSeconds, 0..120, "Sek.") { vm.change(alarm.copy(fadeSeconds = it)); vm.vorschauAnschwellen(it) }
                    ValueSlider("Schlummerdauer", alarm.snoozeMinutes, 1..60, "Min.") { vm.change(alarm.copy(snoozeMinutes = it)) }
                    ValueSlider("Erlaubte Schlummerpausen", alarm.snoozeLimit, 0..20, "") { vm.change(alarm.copy(snoozeLimit = it)) }
                    Text("Die Wecklautstärke gilt beim Klingeln unabhängig von der bisherigen Gerätelautstärke und wird danach wiederhergestellt.", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                }
            }
        }
        Section(
            title = "Wecker ausschalten",
            collapsible = true,
            initiallyExpanded = alarm.photoRequired,
            status = if (alarm.photoRequired) "Foto-Aufgabe" else "Normal",
            summary = if (!alarm.photoRequired) "Normaler Stoppknopf" else listOfNotNull(
                if (alarm.reference.isNotBlank()) "Referenzmotiv ab ${alarm.photoTolerance} % Ähnlichkeit" else null,
                if (alarm.minBrightness > 0) "Helligkeit ab ${alarm.minBrightness} %" else null,
                if (alarm.color != "none") "${PhotoCheck.colors[alarm.color]} ab ${alarm.colorPercent} %" else null,
            ).joinToString(" · ", prefix = "Foto-Aufgabe: ").removeSuffix("Foto-Aufgabe: ").ifBlank { "Foto-Aufgabe ohne Bedingung" },
            error = if (alarm.photoRequired && alarm.reference.isBlank() && alarm.color == "none" && alarm.minBrightness == 0)
                "Lege ein Referenzfoto, eine Mindesthelligkeit oder eine Farbe fest." else null,
        ) {
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
        Text(modifier = Modifier.padding(horizontal = 14.dp), text = "Dein Entwurf wird automatisch gesichert. Beim Speichern werden sechs Stimmvarianten offline vorbereitet.", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
        Spacer(Modifier.height(12.dp))
       }
      }
    }
}

/**
 * Modulare 3D-Bausteinkarte im Editor: zeigt Symbol, Titel, kompakte Statuszeile und Schalter.
 * Beim Einschalten klappen alle zugehörigen Einstellungen unmittelbar an derselben Stelle auf.
 */
@Composable
private fun BausteinKarte(
    titel: String,
    untertitel: String,
    symbol: androidx.compose.ui.graphics.vector.ImageVector,
    aktiv: Boolean,
    warnung: Boolean,
    aufWechsel: (Boolean) -> Unit,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val gold = LocalGold.current
    val semantisch = LocalSemantisch.current
    val haptik = LocalHapticFeedback.current
    val form = RoundedCornerShape(LocalDesignTokens.current.chipRadius.coerceAtMost(18.dp))
    val akzent = if (warnung) semantisch.warnung else gold.primaer
    val kanteFarbe = when {
        warnung -> semantisch.warnung.copy(alpha = 0.65f)
        aktiv -> gold.primaer.copy(alpha = 0.45f)
        else -> gold.rahmen.copy(alpha = 0.65f)
    }
    Column(
        Modifier.fillMaxWidth()
            .clip(form)
            .background(if (aktiv) gold.flaecheErhoeht.heller(if (gold.istDunkel) 0.04f else 0.01f) else gold.eingabefeld.copy(alpha = 0.65f))
            .border(1.dp, kanteFarbe, form)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .toggleable(
                    value = aktiv,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = androidx.compose.ui.semantics.Role.Switch,
                    onValueChange = {
                        haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        aufWechsel(it)
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(34.dp)
                    .clip(RoundedCornerShape(LocalDesignTokens.current.chipRadius.coerceAtMost(10.dp)))
                    .background(if (aktiv) akzent.copy(alpha = 0.18f) else gold.textGedaempft.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(symbol, null, tint = if (aktiv) akzent else gold.textGedaempft, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    titel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (aktiv) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (aktiv) gold.textPrimaer else gold.textGedaempft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    untertitel,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (warnung) semantisch.warnung else gold.textGedaempft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Schalter3D(aktiv, null)
        }
        val reduziertBaustein = LocalBewegungReduziert.current
        AnimatedVisibility(
            aktiv,
            enter = if (reduziertBaustein) androidx.compose.animation.EnterTransition.None else androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = if (reduziertBaustein) androidx.compose.animation.ExitTransition.None else androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider(color = gold.rahmen.copy(alpha = 0.65f))
                inhalt()
            }
        }
    }
}

@Composable
private fun AlarmSpeechEditor(vm: WeckerViewModel, alarm: Alarm) {
    val revision by vm.settingsRevision.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val recording by vm.recording.collectAsStateWithLifecycle()
    val voices by vm.clonedVoices.collectAsStateWithLifecycle()
    val loading by vm.voicesLoading.collectAsStateWithLifecycle()
    val error by vm.voiceLoadError.collectAsStateWithLifecycle()
    val defaults = remember(revision) { de.frank.genialeideen.speech.SyntheseStimme(vm.settings) }
    val effective = alarm.resolveVoice(defaults)
    val available = stimmAuswahl(vm, voices)
    AlarmSpeechEditorRest(vm, alarm, defaults, effective, available, busy, recording, loading, error)
}

/** Alle wählbaren Stimmen als „anbieter|stimme“ → Anzeige, Favoriten mit Stern oben; geteilt von Wecker-Editor und Schlafenszeit-Ansage. */
fun stimmAuswahl(vm: WeckerViewModel, voices: List<de.frank.genialeideen.tts.ClonedVoice>): List<Pair<String, String>> {
    val availableRoh = voices.map {
        "${TtsProvider.QWEN_CLONE.id}|${it.id}" to "${vm.settings.qwenVoiceNames[it.id] ?: it.name} · Meine Stimmen"
    } + ModellKatalog.STIMMEN.map {
        "${TtsProvider.MODELL.id}|${it.id}" to "${ModellKatalog.anzeige(it)} · Supertonic (Handy)"
    } + TtsCatalog.googleVoices.map {
        "${TtsProvider.GOOGLE_CLOUD.id}|${it.id}" to "${it.name} · Google · ${geschlecht(it)}"
    } + TtsCatalog.geminiFlashVoices.map {
        "${TtsProvider.GEMINI_FLASH.id}|${it.id}" to "${it.name} · Gemini 3.8 Flash · ${geschlecht(it)}"
    } + TtsCatalog.geminiLiteVoices.map {
        "${TtsProvider.GEMINI_LITE.id}|${it.id}" to "${it.name} · Gemini 3.8 Flash-Lite · ${geschlecht(it)}"
    } + TtsCatalog.edgeVoices.map {
        "${TtsProvider.EDGE.id}|${it.id}" to "${it.name} · Edge · ${geschlecht(it)}"
    } + TtsCatalog.azureVoices.map {
        "${TtsProvider.AZURE.id}|${it.id}" to "${it.name} · Azure · ${geschlecht(it)}"
    }
    // Favoriten (in den Einstellungen per Stern oder langem Drücken markiert) stehen oben, mit Stern.
    val favoriten = vm.settings.favoriteTtsVoices
    return availableRoh.sortedByDescending { it.first.substringAfter('|') in favoriten }
        .map { (id, name) -> if (id.substringAfter('|') in favoriten) id to "★ $name" else id to name }
}

@Composable
private fun AlarmSpeechEditorRest(vm: WeckerViewModel, alarm: Alarm, defaults: de.frank.genialeideen.speech.SyntheseStimme,
    effective: de.frank.genialeideen.speech.SyntheseStimme, available: List<Pair<String, String>>,
    busy: String, recording: Boolean, loading: Boolean, error: String) {
    val defaultId = when (defaults.ttsProvider) {
        TtsProvider.GOOGLE_CLOUD.id -> defaults.googleTtsVoice
        TtsProvider.QWEN_CLONE.id -> defaults.qwenTtsVoiceId
        TtsProvider.QWEN.id -> defaults.qwenStandardVoice
        TtsProvider.MODELL.id -> defaults.modellStimme
        TtsProvider.GEMINI_FLASH.id -> defaults.geminiFlashTtsVoice
        TtsProvider.GEMINI_LITE.id -> defaults.geminiLiteTtsVoice
        TtsProvider.AZURE.id -> defaults.azureTtsVoice
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
        Regler3D(effective.ttsSpeechRate, { vm.change(alarm.copy(speechRate = it)) }, bereich = .5f..2f)
        if (alarm.speechRate != null) StillerKnopf("Standard-Sprechgeschwindigkeit verwenden", {
            vm.change(alarm.copy(speechRate = null))
        })
        // Genau diese Stimme mit genau diesem Tempo, ohne die globalen Einstellungen anzufassen.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GoldKnopf("Stimme anhören", { vm.previewVoice(alarm) }, aktiviert = busy.isBlank() && !recording)
            // Stoppen bleibt immer möglich, auch während etwas anderes läuft.
            StillerKnopf("Stoppen", vm::stopPreview)
        }
        Text("Ohne eigene Auswahl gelten Stimme und Sprechgeschwindigkeit aus den Einstellungen. Jede Änderung hier gilt nur für diesen Wecker.", style = MaterialTheme.typography.bodySmall)
        if (loading) Text("Deine hochgeladenen Stimmen werden geladen …", style = MaterialTheme.typography.bodySmall)
        if (error.isNotBlank() && vm.settings.qwenTtsApiKey.isNotBlank()) {
            Text(error, color = LocalSemantisch.current.warnung, style = MaterialTheme.typography.bodySmall)
            StillerKnopf("Meine Stimmen erneut laden", { vm.loadVoices(force = true) })
        }
    }
}

/** The former raised StillerKnopf (⌃/⌄) as fold button; small body, touch target extended to 48 dp. */
@Composable
fun KlappKnopf(expanded: Boolean, onToggle: () -> Unit, beschreibung: String?, modifier: Modifier = Modifier) {
    StillerKnopf(if (expanded) "⌃" else "⌄", onToggle, modifier.minimumInteractiveComponentSize().size(44.dp).then(
        if (beschreibung == null) Modifier.clearAndSetSemantics {}
        else Modifier.semantics { contentDescription = beschreibung; stateDescription = if (expanded) "Aufgeklappt" else "Zugeklappt" }))
}

/**
 * Wiederholen-Schalter für einen Vorlese-Block: Minus/Plus von 0 bis 6. 0 und 1 lesen den Block einmal,
 * ab 2 wird er so oft am Stück gelesen, jedes Mal mit einer anderen der sechs Sprachvarianten.
 */
@Composable
fun WiederholenZeile(wert: Int, aendern: (Int) -> Unit) {
    val gold = LocalGold.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Wiederholen")
            Text(if (wert <= 1) "Einmal vorlesen, dann der nächste Schritt" else "$wert× am Stück vorlesen, $wert verschiedene Varianten",
                style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
        }
        GoldKnopf("−", { aendern((wert - 1).coerceAtLeast(0)) }, aktiviert = wert > 0, beschreibung = "Weniger Wiederholungen")
        Text("$wert", Modifier.widthIn(min = 36.dp).padding(horizontal = 8.dp), fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
            color = gold.primaer, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        GoldKnopf("+", { aendern((wert + 1).coerceAtMost(Alarm.MAX_REPEATS)) }, aktiviert = wert < Alarm.MAX_REPEATS, beschreibung = "Mehr Wiederholungen")
    }
}

/** Aufgaben-Baustein im Editor: Tag, Knopf zum Ansehen, Wiederholen-Schalter und das kleine Fenster. */
@Composable
private fun AufgabenBereich(vm: WeckerViewModel, alarm: Alarm, offen: Boolean, setzeOffen: (Boolean) -> Unit) {
    val gold = LocalGold.current
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val tasksDay by vm.tasksDay.collectAsStateWithLifecycle()
    val loading by vm.tasksLoading.collectAsStateWithLifecycle()
    val error by vm.tasksError.collectAsStateWithLifecycle()
    // Aus dem Entwurf gerechnet: der gespeicherte nextAt zeigt beim Bearbeiten noch auf den alten Termin.
    val tag = remember(alarm) { TasksBridge.klingeltag(alarm, ausPlan = true) }
    val tagText = remember(tag) { aufgabenTag(tag) }
    Text("Aufgaben", style = MaterialTheme.typography.titleSmall, color = gold.primaer)
    Text("Vorgelesen werden die offenen Aufgaben aus der App „Aufgaben“ für $tagText ab der Weckzeit " +
        "(frühere Termine fallen weg, Aufgaben ohne Uhrzeit bleiben): zuerst die Uhrzeit, dann der Titel, " +
        "zwischen zwei Aufgaben 2 Sekunden Pause, nach dem ganzen Block 3 Sekunden. Änderungen in der Aufgaben-App werden automatisch neu vorbereitet.",
        style = MaterialTheme.typography.bodySmall)
    StillerKnopf("Aufgaben für $tagText ansehen", { vm.loadTasks(alarm); setzeOffen(true) })
    WiederholenZeile(alarm.tasksRepeats) { vm.change(alarm.copy(tasksRepeats = it)) }
    if (offen) DesignDialog(
        titel = "Aufgaben · $tagText",
        aufSchliessen = { setzeOffen(false) },
        bestaetigung = { StillerKnopf("Schließen", { setzeOffen(false) }) },
    ) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                loading -> Text("Aufgaben werden gelesen …", color = gold.textGedaempft)
                error.isNotBlank() -> Text(error, color = LocalSemantisch.current.warnung)
                tasksDay != tag -> Text("Noch nicht gelesen.", color = gold.textGedaempft)
                tasks.isEmpty() -> Text("Für $tagText sind ab der Weckzeit keine offenen Aufgaben eingetragen.", color = gold.textPrimaer)
                else -> tasks.forEach { aufgabe ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(aufgabe.zeit.ifBlank { "–" }, Modifier.width(64.dp), fontFamily = zahlSchrift(), fontWeight = FontWeight.SemiBold, color = gold.primaer)
                        Text(aufgabe.titel, Modifier.weight(1f), color = gold.textPrimaer)
                    }
                }
            }
        }
    }
}

/** „morgen, Di 30.09.“ bzw. „heute, …“ oder nur das Datum. */
private fun aufgabenTag(tag: java.time.LocalDate): String {
    val heute = java.time.LocalDate.now()
    val datum = tag.format(DateTimeFormatter.ofPattern("EE dd.MM.", java.util.Locale.GERMAN))
    return when (tag) {
        heute -> "heute, $datum"
        heute.plusDays(1) -> "morgen, $datum"
        else -> datum
    }
}

@Composable
fun Toggle(label: String, value: Boolean, change: (Boolean) -> Unit) {
    val haptik = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = { neu ->
            haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            change(neu)
        }),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Schalter3D(value, null)
    }
}

@Composable
fun ValueSlider(label: String, value: Int, range: IntRange, unit: String, change: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("$label: $value${if (unit.isBlank()) "" else " $unit"}")
        // Reglerbahn als vertiefte Rille, Knauf erhaben — dasselbe Material wie überall sonst.
        Regler3D(
            wert = value.toFloat(),
            aufAenderung = { change(it.roundToInt()) },
            // Die Ansage für TalkBack bleibt wie bisher: Name des Reglers und sein aktueller
            // Wert samt Einheit. Ohne sie hieße der Regler nur „Schieberegler".
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = "$value${if (unit.isBlank()) "" else " $unit"}"
            },
            bereich = range.first.toFloat()..range.last.toFloat(),
            stufen = (range.last - range.first - 1).coerceAtLeast(0),
        )
    }
}

/** Stimmen im Editor genauso benennen wie in den Einstellungen. */
fun geschlecht(stimme: TtsVoice): String = if (stimme.gender == VoiceGender.FEMALE) "weiblich" else "männlich"

/** Ab wie vielen Einträgen die Auswahl ein Suchfeld bekommt; kürzere Listen bleiben unverändert. */
private const val CHOICE_SUCHE_AB = 12

@Composable
fun Choice(label: String, selected: String, options: List<Pair<String, String>>, choose: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Text(label, style = MaterialTheme.typography.labelLarge)
    GoldKnopf(options.find { it.first == selected }?.second ?: "Auswählen", { open = true }, Modifier.fillMaxWidth())
    if (open) {
        // Der Zustand lebt nur, solange der Dialog offen ist: jedes Öffnen beginnt ohne Suchbegriff
        // und bei der aktuellen Auswahl.
        var suche by rememberSaveable { mutableStateOf("") }
        val suchbar = options.size > CHOICE_SUCHE_AB
        val gezeigt = if (!suchbar || suche.isBlank()) options
            else options.filter { it.second.contains(suche.trim(), ignoreCase = true) }
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0))
        // Nach einem eingegebenen Suchbegriff steht das erste Ergebnis oben. Der leere Anfangszustand
        // löst nichts aus, damit die Startposition auf der aktuellen Auswahl erhalten bleibt.
        if (suchbar) LaunchedEffect(suche) { if (suche.isNotBlank()) listState.scrollToItem(0) }
        val haptikWahl = LocalHapticFeedback.current
        val eintraege: LazyListScope.() -> Unit = {
            items(gezeigt, key = { it.first }) { option ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(option.first == selected, interactionSource = null, indication = null, role = androidx.compose.ui.semantics.Role.RadioButton) {
                    haptikWahl.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    choose(option.first); open = false
                }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(option.first == selected, null); Text(option.second, Modifier.padding(start = 8.dp))
                }
            }
        }
        DesignDialog(
            titel = label,
            aufSchliessen = { open = false },
            bestaetigung = { StillerKnopf("Schließen", { open = false }) },
            inhalt = {
                // Kurze Listen behalten genau den bisherigen Aufbau ohne Höhenbegrenzung.
                if (!suchbar) LazyColumn(state = listState, content = eintraege)
                else {
                    Eingabefeld(suche, { suche = it }, "Suchen", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    if (gezeigt.isEmpty()) Text("Kein Eintrag passt zu „${suche.trim()}“. Ändere den Suchbegriff oder leere das Feld, um wieder alle ${options.size} Einträge zu sehen.",
                        style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
                    // Begrenzt nach oben, weicht auf kleinen Schirmen aber zurück, statt den Dialog zu sprengen.
                    else LazyColumn(Modifier.heightIn(max = 320.dp).weight(1f, fill = false), state = listState, content = eintraege)
                }
            },
        )
    }
}

@Composable
fun PhotoPreview(file: File) {
    var bitmap by remember(file.absolutePath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(file.absolutePath) {
        bitmap = withContext(Dispatchers.IO) { runCatching { PhotoCheck.bitmap(file) }.getOrNull() }
    }
    bitmap?.let { Image(it.asImageBitmap(), "Gespeichertes Referenzmotiv", Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(16.dp))) }
}

/** [aktion] benennt die Folge auf dem Knopf selbst – ohne Vorgabe, damit keine Rückfrage beim generischen „Bestätigen“ bleibt. */
@Composable
fun Confirm(title: String, text: String, aktion: String, yes: () -> Unit, no: () -> Unit) {
    DesignTextDialog(titel = title, text = text, aufSchliessen = no,
        bestaetigung = { GoldKnopf(aktion, yes) }, abbruch = { StillerKnopf("Abbrechen", no) })
}

fun newPhoto(context: Context): File = File(context.cacheDir, "photos").apply { mkdirs() }.let { File(it, "${UUID.randomUUID()}.jpg") }
/**
 * Ein tatsächlicher Termin, zerlegt für die Anzeige: heute nur die Uhrzeit, morgen die Uhrzeit mit
 * Zusatz, ab übermorgen das lokale Kalenderdatum zuerst und die Uhrzeit danach, bei einem anderen
 * Jahr mit Jahreszahl. Entschieden wird nach Kalendertagen, nicht nach Stundenabstand.
 * Reine Darstellung — Planung, AlarmTime und Scheduler bleiben unberührt.
 */
data class TerminAnzeige(val datum: String?, val uhrzeit: String, val zusatz: String?) {
    /** Eine Zeile wie im Kopfbereich: 18:30 · 07:00 · morgen · So, 20.09. · 07:00 */
    val einzeilig: String get() = listOfNotNull(datum, uhrzeit, zusatz).joinToString(" · ")
}

fun terminAnzeige(now: Long, target: Long, zone: ZoneId = ZoneId.systemDefault()): TerminAnzeige {
    val heute = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val ziel = Instant.ofEpochMilli(target).atZone(zone)
    val tag = ziel.toLocalDate()
    // Aus derselben Zone wie das Datum, damit Uhrzeit und Tag nie auseinanderfallen.
    val uhrzeit = ziel.format(DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.GERMAN))
    return when {
        tag == heute -> TerminAnzeige(null, uhrzeit, null)
        tag == heute.plusDays(1) -> TerminAnzeige(null, uhrzeit, "morgen")
        else -> TerminAnzeige(ziel.format(DateTimeFormatter.ofPattern(
            if (tag.year != heute.year) "EEE, dd.MM.yyyy" else "EEE, dd.MM.", java.util.Locale.GERMAN)),
            uhrzeit, null)
    }
}

fun formatAt(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, dd.MM. · HH:mm", java.util.Locale.GERMAN))
fun dayLabel(days: Set<Int>): String = if (days.isEmpty()) "Einmalig" else if (days.size == 7) "Täglich" else if (days == setOf(1, 2, 3, 4, 5)) "Mo–Fr" else days.sorted().joinToString(" · ") { listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")[it - 1] }
fun scheduleLabel(alarm: Alarm): String = rhythmusLabel(alarm) +
    if (alarm.sunMode.isBlank()) "" else " · ☀ ${SonnenWecker.bezeichnung(alarm)}"

private fun rhythmusLabel(alarm: Alarm): String = when {
    alarm.repeatUnit == Alarm.MONTHLY -> {
        val day = java.time.LocalDate.parse(alarm.startDate).dayOfMonth
        (if (alarm.repeatEvery <= 1) "Monatlich am $day." else "Alle ${alarm.repeatEvery} Monate am $day.") +
            (if (day > 28) " · sonst Monatsletzter" else "") + " · ab ${dateLabel(alarm.startDate)}"
    }
    alarm.repeatUnit == Alarm.YEARLY -> {
        val date = java.time.LocalDate.parse(alarm.startDate)
        (if (alarm.repeatEvery <= 1) "Jährlich am " else "Alle ${alarm.repeatEvery} Jahre am ") +
            "%02d.%02d.".format(date.dayOfMonth, date.monthValue) +
            (if (date.monthValue == 2 && date.dayOfMonth == 29) " · sonst 28.02." else "") + " · ab ${dateLabel(alarm.startDate)}"
    }
    alarm.intervalDays > 0 -> "Alle ${alarm.intervalDays} Tage · ab ${dateLabel(alarm.startDate)}"
    alarm.startDate.isNotBlank() -> "Einmalig am ${dateLabel(alarm.startDate)}"
    else -> dayLabel(alarm.days)
}
private fun dateLabel(date: String) = java.time.LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

@Composable
private fun RepeatEditor(alarm: Alarm, activity: ComponentActivity, gemerktesDatum: String, merke: (String) -> Unit, change: (Alarm) -> Unit) {
    val today = java.time.LocalDate.now()
    val mode = when {
        alarm.repeatUnit == Alarm.MONTHLY -> "month"
        alarm.repeatUnit == Alarm.YEARLY -> "year"
        alarm.intervalDays > 0 -> "interval"
        alarm.days.size == 7 -> "daily"
        alarm.days.isNotEmpty() -> "weekdays"
        else -> "once"
    }
    // The current date wins; only when the alarm carries none (daily and weekdays clear it) the remembered one applies.
    val anchor = alarm.startDate.ifBlank { gemerktesDatum }.ifBlank { today.plusDays(1).toString() }
    fun changeMode(chosen: String) {
        // Tapping the active mode again changes nothing, so date, weekdays or interval are never reset.
        if (chosen == mode) return
        // Saved synchronously with the switch, not through an effect, so the date cannot be lost in between.
        if (alarm.startDate.isNotBlank()) merke(alarm.startDate)
        change(when (chosen) {
            "daily" -> alarm.copy(days = (1..7).toSet(), startDate = "", intervalDays = 0, repeatUnit = "", repeatEvery = 0)
            "weekdays" -> alarm.copy(days = alarm.days.takeIf { it.isNotEmpty() && it.size < 7 } ?: setOf(1, 2, 3, 4, 5),
                startDate = "", intervalDays = 0, repeatUnit = "", repeatEvery = 0)
            "interval" -> alarm.copy(days = emptySet(), startDate = anchor, intervalDays = alarm.intervalDays.takeIf { it > 0 } ?: 35, repeatUnit = "", repeatEvery = 0)
            "month" -> alarm.copy(days = emptySet(), startDate = anchor, intervalDays = 0, repeatUnit = Alarm.MONTHLY, repeatEvery = alarm.repeatEvery.coerceIn(1, 12))
            "year" -> alarm.copy(days = emptySet(), startDate = anchor, intervalDays = 0, repeatUnit = Alarm.YEARLY, repeatEvery = alarm.repeatEvery.coerceIn(1, 5))
            else -> alarm.copy(days = emptySet(), intervalDays = 0, repeatUnit = "", repeatEvery = 0)
        })
    }
    Text("Wann soll er wecken?", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("once" to "Einmalig", "daily" to "Täglich", "weekdays" to "An Wochentagen",
            "interval" to "Alle X Tage", "month" to "Monatlich", "year" to "Jährlich").forEach { (id, label) ->
            Chip3D(mode == id, { changeMode(id) }, label)
        }
    }
    if (mode == "weekdays") {
        val names = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
        val kuerzel = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        val haptikTag = LocalHapticFeedback.current
        val gold = LocalGold.current
        val tokens = LocalDesignTokens.current
        val schriftFaktor = LocalDensity.current.fontScale
        if (schriftFaktor <= 1.25f) {
            // Alle 7 Tage gleichmäßig in einer einzigen klaren Reihe ohne vereinzeltes „So“ in Zeile 2.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                kuerzel.forEachIndexed { index, name ->
                    val day = index + 1
                    val gewaehlt = day in alarm.days
                    val tagForm = RoundedCornerShape(tokens.chipRadius)
                    Box(
                        Modifier.weight(1f)
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = gewaehlt,
                                role = androidx.compose.ui.semantics.Role.Checkbox,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    haptikTag.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (!gewaehlt || alarm.days.size > 1) {
                                        change(alarm.copy(days = if (gewaehlt) alarm.days - day else alarm.days + day))
                                    }
                                },
                            )
                            .semantics { contentDescription = names[index] }
                            .then(
                                if (gewaehlt) Modifier.shadow(4.dp, tagForm, ambientColor = gold.primaer, spotColor = gold.primaer)
                                else Modifier.shadow(1.5f.dp, tagForm, ambientColor = androidx.compose.ui.graphics.Color.Black, spotColor = androidx.compose.ui.graphics.Color.Black),
                            )
                            .clip(tagForm)
                            .background(if (gewaehlt) gold.primaer else gold.flaecheErhoeht)
                            .border(
                                1.dp,
                                if (gewaehlt) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f) else gold.rahmen,
                                tagForm,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (gewaehlt) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (gewaehlt) gold.aufPrimaer else gold.textPrimaer,
                            maxLines = 1,
                        )
                    }
                }
            }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                kuerzel.forEachIndexed { index, name ->
                    val day = index + 1
                    Chip3D(day in alarm.days, { if (day !in alarm.days || alarm.days.size > 1) change(alarm.copy(days = if (day in alarm.days) alarm.days - day else alarm.days + day)) },
                        name, Modifier.semantics { contentDescription = names[index] })
                }
            }
        }
    }
    if (mode == "once") {
        // One-off with optional date: without a date the alarm rings at the next matching time.
        Toggle("An einem bestimmten Datum", alarm.startDate.isNotBlank()) { dated ->
            if (!dated && alarm.startDate.isNotBlank()) merke(alarm.startDate)
            change(alarm.copy(startDate = if (dated) anchor else ""))
        }
        if (alarm.startDate.isBlank()) Text("Ohne Datum weckt er beim nächsten Erreichen der Uhrzeit.", style = MaterialTheme.typography.bodySmall)
    }
    if (alarm.startDate.isNotBlank()) {
        // Kalendertage, damit die Zeitumstellung die Entfernung nicht verfälscht; als Long, weil der Abstand
        // beliebig groß sein darf und erst nach der Bereichsprüfung in den Schieberegler passt.
        val ahead = java.time.temporal.ChronoUnit.DAYS.between(today, java.time.LocalDate.parse(alarm.startDate))
        val tage = { anzahl: Long -> if (anzahl == 1L) "1 Tag" else "$anzahl Tage" }
        // Auch hier stand bisher der helle Plattformdialog. Der Ersatz übernimmt die Semantik
        // unverändert: Er schreibt erst beim Bestätigen, und nur das Startdatum.
        // Ebenfalls saveable, aus demselben Grund wie bei der Uhrzeitwahl.
        var datumWahlOffen by rememberSaveable(alarm.id) { mutableStateOf(false) }
        // Knopf und gewähltes Datum stehen in einer Zeile, direkt unter der Auswahl.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StillerKnopf(if (mode == "once") "Datum wählen" else "Startdatum", { datumWahlOffen = true }, hervorgehoben = true)
            Text(if (mode == "once") "${dateLabel(alarm.startDate)} um ${alarm.timeLabel}" else "ab ${dateLabel(alarm.startDate)}",
                Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = LocalGold.current.primaer)
        }
        if (datumWahlOffen) DatumWahlDialog(
            datum = runCatching { java.time.LocalDate.parse(alarm.startDate) }.getOrDefault(today),
            fruehestes = null,
            aufAbbruch = { datumWahlOffen = false },
            aufWahl = { gewaehlt -> datumWahlOffen = false; change(alarm.copy(startDate = gewaehlt.toString())) },
        )
        // Die Schnellwahl erscheint nur, wenn sie den tatsächlichen Abstand zeigen kann. Sonst würde sie einen
        // geklemmten Wert behaupten – und eine Berührung würde einen bestehenden Rhythmus neu verankern.
        if (ahead in 0L..60L) ValueSlider("Schnellwahl: in", ahead.toInt(), 0..60, "Tagen") {
            change(alarm.copy(startDate = today.plusDays(it.toLong()).toString()))
        }
        // Ein einmaliger Termin in der Vergangenheit hat oben bereits seine eigene Warnkarte; nicht doppelt melden.
        else if (!(mode == "once" && ahead < 0L)) Text(
            if (ahead > 60L) "Das Startdatum liegt ${tage(ahead)} voraus – weiter als die Schnellwahl reicht. Ändere es über den Kalender."
            else "Das Startdatum liegt ${tage(-ahead)} zurück. Daran bleibt der Rhythmus verankert. Bei Bedarf kannst du es über den Kalender ändern.",
            style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
        if (mode == "interval") Text("Der Rhythmus bleibt am Startdatum verankert. Schlummern oder das Auslassen eines Termins verschiebt deine Schichtfolge nicht.", style = MaterialTheme.typography.bodySmall)
    }
    if (mode == "interval") {
        // Existing values above 60 days stay untouched; the slider grows instead of cutting them down.
        ValueSlider("Alle wie viele Tage?", alarm.intervalDays, 1..maxOf(60, alarm.intervalDays), "Tage") { change(alarm.copy(intervalDays = it)) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(4, 5, 8, 35).forEach { days -> Chip3D(alarm.intervalDays == days, { change(alarm.copy(intervalDays = days)) }, "$days Tage") }
        }
    }
    if (mode == "month") {
        ValueSlider("Alle wie viele Monate?", alarm.repeatEvery.coerceIn(1, 12), 1..12, "Monate") { change(alarm.copy(repeatEvery = it)) }
        val day = java.time.LocalDate.parse(anchor).dayOfMonth
        val takt = if (alarm.repeatEvery > 1) "jeden ${alarm.repeatEvery}. Monat" else "jeden Monat"
        Text("Ab ${dateLabel(anchor)} $takt am $day. um ${alarm.timeLabel}." +
            if (day > 28) " Fehlt der Tag in einem Monat, weckt er am letzten Tag; danach wieder am $day." else "",
            style = MaterialTheme.typography.bodySmall)
    }
    if (mode == "year") {
        ValueSlider("Alle wie viele Jahre?", alarm.repeatEvery.coerceIn(1, 5), 1..5, "Jahre") { change(alarm.copy(repeatEvery = it)) }
        val date = java.time.LocalDate.parse(anchor)
        val taktJahr = if (alarm.repeatEvery > 1) "jedes ${alarm.repeatEvery}. Jahr" else "jedes Jahr"
        Text("Ab ${dateLabel(anchor)} $taktJahr am %02d.%02d. um ${alarm.timeLabel}.".format(date.dayOfMonth, date.monthValue) +
            if (date.monthValue == 2 && date.dayOfMonth == 29) " Ohne 29.02. weckt er am 28.02., im Schaltjahr wieder am 29.02." else "",
            style = MaterialTheme.typography.bodySmall)
    }
    // Dauer: wie lange die Wiederholung läuft. Vorgabe „endlos“; ein Tipp öffnet den Kalender für das Enddatum.
    if (mode != "once") {
        var endeWahlOffen by rememberSaveable(alarm.id) { mutableStateOf(false) }
        if (endeWahlOffen) DatumWahlDialog(
            datum = runCatching { java.time.LocalDate.parse(alarm.endDate) }.getOrNull()
                ?: runCatching { java.time.LocalDate.parse(alarm.startDate).plusMonths(12) }.getOrDefault(today.plusMonths(12)),
            fruehestes = runCatching { java.time.LocalDate.parse(alarm.startDate) }.getOrNull()?.takeIf { it.isAfter(today) } ?: today,
            aufAbbruch = { endeWahlOffen = false },
            aufWahl = { gewaehlt -> endeWahlOffen = false; change(alarm.copy(endDate = gewaehlt.toString())) },
        )
        HorizontalDivider(Modifier.padding(vertical = 2.dp), color = LocalGold.current.rahmen)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StillerKnopf("Dauer", { endeWahlOffen = true }, hervorgehoben = true)
            Text(if (alarm.endDate.isBlank()) "Dauer: endlos" else "Dauer: bis ${dateLabel(alarm.endDate)}",
                Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = LocalGold.current.primaer)
            if (alarm.endDate.isNotBlank()) StillerKnopf("Endlos", { change(alarm.copy(endDate = "")) })
        }
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

/**
 * Große Überschrift einer Weckerkarte. Ein AKTIVER Wecker zeigt den tatsächlich geplanten nächsten
 * Termin aus Alarm.nextAt — bei einem Monats-, Jahres- oder Intervallplan liegt der Tage entfernt,
 * und dann gehört das Datum davor. Ein ausgeschalteter Wecker zeigt weiterhin seine konfigurierte
 * Weckzeit, denn genau die bearbeitet der Editor.
 *
 * Datum und Uhrzeit dürfen umbrechen: auf schmalen Anzeigen und bei großer Systemschrift wird lieber
 * zweizeilig gesetzt als verkleinert oder abgeschnitten.
 */
/**
 * @param platzHalten hält die Datumszeile frei, auch wenn kein Datum angezeigt wird. Die fachliche
 *   Regel, WANN ein Datum erscheint, bleibt davon unberührt: Sie steht weiterhin allein in
 *   [terminAnzeige] — heute und morgen führt die Uhrzeit, ab übermorgen das Datum. Reserviert wird
 *   nur der Platz, damit zugeklappte Karten nicht unterschiedlich hoch werden.
 */
@Composable
private fun WeckzeitUeberschrift(alarm: Alarm, now: Long, farbe: androidx.compose.ui.graphics.Color,
    platzHalten: Boolean = false) {
    // Kurz, eindeutig und ohne Abschneiden auch auf schmalen Displays: „heute“, „morgen“ oder „Sa, 28.11.“
    val zusatz = remember(alarm.enabled, alarm.nextAt, now) {
        if (!alarm.enabled || alarm.nextAt <= 0) null else {
            val zone = ZoneId.systemDefault()
            val tag = Instant.ofEpochMilli(alarm.nextAt).atZone(zone).toLocalDate()
            val heute = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            when {
                tag == heute -> "heute"
                tag == heute.plusDays(1) -> "morgen"
                tag.year == heute.year -> tag.format(DateTimeFormatter.ofPattern("EE, dd.MM.", java.util.Locale.GERMAN))
                else -> tag.format(DateTimeFormatter.ofPattern("dd.MM.yy", java.util.Locale.GERMAN))
            }
        }
    }
    val orbit = LocalDesignTokens.current.design == Design.ORBIT
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        Text(alarm.timeLabel, fontFamily = zahlSchrift(), fontWeight = zahlGewicht(),
            fontSize = if (orbit) 30.sp else 36.sp, color = farbe, maxLines = 1, softWrap = false)
        if (alarm.sunMode.isNotBlank()) {
            Icon(Icons.Default.WbTwilight, "Sonnenaufgangswecker", tint = farbe,
                modifier = Modifier.padding(bottom = if (orbit) 6.dp else 8.dp).size(16.dp))
        }
        zusatz?.let {
            Text(it, Modifier.weight(1f, fill = false).padding(bottom = if (orbit) 5.dp else 6.dp),
                fontFamily = if (orbit) IdeenSchriftFest else null,
                style = if (orbit) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = farbe.copy(alpha = 0.88f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The alarm term with the same filled marker as on the ring; the weekday and date are added when it is not today. */
@Composable
private fun TerminZeile(now: Long, target: Long, snooze: Boolean, punkt: Boolean = true) {
    val gold = LocalGold.current
    val accent = if (snooze) LocalSemantisch.current.info else gold.primaer
    val targetText = terminAnzeige(now, target).einzeilig
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (punkt) LegendenMarker(hollow = false, color = accent)
        // Genau eine Zeile: Ohne diese Begrenzung brach „Wecker Mo, 22.09. · 06:30" um, und der
        // Hero war je nach Termin unterschiedlich hoch — er sprang beim Wechsel von „morgen"
        // auf ein Wochentagsdatum.
        Text("${if (snooze) "Schlummern bis" else "Wecker"} $targetText",
            style = MaterialTheme.typography.bodyMedium, color = gold.textPrimaer,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Normale 12-Stunden-Uhr: 12 oben, 3 rechts, 6 unten, 9 links. Bogen im Uhrzeigersinn von jetzt (hohler Punkt)
 * bis zum Wecker (gefüllter Punkt), nur wenn eindeutig (siehe [ZeitRing]); sonst kurze Kennzeichnung in der Mitte.
 * Für TalkBack stumm, der Text daneben ist maßgeblich.
 */
@Composable
private fun RestzeitRing(now: Long, target: Long?, snooze: Boolean, modifier: Modifier = Modifier, zifferblatt: Boolean = false) {
    val gold = LocalGold.current
    val accent = if (snooze) LocalSemantisch.current.info else gold.primaer
    // Das Zifferblatt des 3D-Weckers ist auch im Dunkelmodus hell (Elfenbein) – darauf braucht es dunkle Tinte.
    val hellesBlatt = zifferblatt && gold.istDunkel
    val tinteLeicht = if (hellesBlatt) ZIFFER_TINTE_LEICHT else gold.textGedaempft
    val tinteStark = if (hellesBlatt) ZIFFER_TINTE_STARK else gold.textPrimaer
    val geometry = target?.let { ZeitRing.berechne(now, it) }
    val nowAngle = ZeitRing.angle(now, ZoneId.systemDefault())
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val numberStyle = MaterialTheme.typography.labelSmall.copy(color = tinteLeicht, fontSize = 10.sp)
    val centerStyle = MaterialTheme.typography.labelMedium.copy(color = accent, fontWeight = FontWeight.SemiBold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    val surface = if (hellesBlatt) ZIFFER_ELFENBEIN else gold.flaecheErhoeht
    androidx.compose.foundation.Canvas(modifier) {
        val stroke = 5.dp.toPx()
        val radius = size.minDimension / 2f - stroke
        val topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius)
        val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        fun point(angleDeg: Float, r: Float = radius) = Math.toRadians(angleDeg.toDouble()).let {
            center + androidx.compose.ui.geometry.Offset((r * Math.cos(it)).toFloat(), (r * Math.sin(it)).toFloat())
        }
        // Im 3D-Wecker läuft der Schlafbogen mit Abstand innerhalb der Teilung; die Zahlen rücken dafür nach innen.
        val bogenRadius = if (zifferblatt) radius - 10.dp.toPx() else radius
        val zahlenRadius = radius - (if (zifferblatt) 21 else 15).dp.toPx()
        if (zifferblatt) {
            // Edles Zifferblatt: Sonnenschliff, der das Licht fächert, eine Guilloché-Rosette in der
            // Mitte und eine Minutenschiene aus zwei feinen Kreisen mit Teilung dazwischen.
            val schliff = (0..24).map { i ->
                if (i % 2 == 0) androidx.compose.ui.graphics.Color.White.copy(alpha = if (hellesBlatt) .16f else .22f)
                else tinteStark.copy(alpha = .05f)
            }
            drawCircle(Brush.sweepGradient(schliff, center), radius)
            val rosette = radius * .34f
            repeat(7) { i ->
                drawCircle(tinteLeicht.copy(alpha = .10f), rosette * (i + 1) / 7f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(.6f.dp.toPx()))
            }
            val schiene = radius - 3.dp.toPx()
            drawCircle(tinteLeicht.copy(alpha = .55f), radius, style = androidx.compose.ui.graphics.drawscope.Stroke(.8f.dp.toPx()))
            drawCircle(tinteLeicht.copy(alpha = .40f), schiene, style = androidx.compose.ui.graphics.drawscope.Stroke(.6f.dp.toPx()))
            repeat(60) { minute ->
                if (minute % 5 != 0) drawLine(tinteLeicht.copy(alpha = .55f), point(minute * 6f - 90f, schiene),
                    point(minute * 6f - 90f, radius), .7f.dp.toPx())
            }
            // Aufgesetzte Stundenindizes aus poliertem Messing: dunkle Kante, helle Fläche, feiner Glanzstrich.
            val index = if (gold.istDunkel) androidx.compose.ui.graphics.lerp(gold.primaer, androidx.compose.ui.graphics.Color(0xFF5E4B2A), .35f)
                else gold.primaer
            repeat(12) { hour ->
                if (hour % 3 != 0) {
                    val a = hour * 30f - 90f
                    val innenPunkt = point(a, radius - 8.dp.toPx())
                    val aussenPunkt = point(a, radius - 1.5f.dp.toPx())
                    drawLine(tinteStark.copy(alpha = .45f), innenPunkt + androidx.compose.ui.geometry.Offset(.5f.dp.toPx(), .8f.dp.toPx()),
                        aussenPunkt + androidx.compose.ui.geometry.Offset(.5f.dp.toPx(), .8f.dp.toPx()), 2.6f.dp.toPx())
                    drawLine(Brush.linearGradient(listOf(index.heller(.45f), index.dunkler(.25f)), innenPunkt, aussenPunkt),
                        innenPunkt, aussenPunkt, 2.2f.dp.toPx())
                    drawLine(androidx.compose.ui.graphics.Color.White.copy(alpha = .55f), innenPunkt, aussenPunkt, .5f.dp.toPx())
                }
            }
            // Römische Ziffern in Schlichts Serifenschrift; unter der XII ein kleiner Messingstern.
            val roemisch = numberStyle.copy(color = tinteStark.copy(alpha = .88f), fontFamily = IdeenSchriftBetont,
                fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = .3.sp)
            listOf(0 to "XII", 3 to "III", 6 to "VI", 9 to "IX").forEach { (hour, label) ->
                val layout = measurer.measure(label, roemisch)
                val at = point(hour * 30f - 90f, zahlenRadius)
                drawText(layout, topLeft = at - androidx.compose.ui.geometry.Offset(layout.size.width / 2f, layout.size.height / 2f))
            }
            val stern = center + androidx.compose.ui.geometry.Offset(0f, -zahlenRadius * .56f)
            val s = 2.6f.dp.toPx()
            val sternPfad = androidx.compose.ui.graphics.Path().apply {
                moveTo(stern.x, stern.y - s); lineTo(stern.x + s * .28f, stern.y - s * .28f)
                lineTo(stern.x + s, stern.y); lineTo(stern.x + s * .28f, stern.y + s * .28f)
                lineTo(stern.x, stern.y + s); lineTo(stern.x - s * .28f, stern.y + s * .28f)
                lineTo(stern.x - s, stern.y); lineTo(stern.x - s * .28f, stern.y - s * .28f); close()
            }
            drawPath(sternPfad, Brush.linearGradient(listOf(index.heller(.4f), index.dunkler(.2f)),
                stern - androidx.compose.ui.geometry.Offset(s, s), stern + androidx.compose.ui.geometry.Offset(s, s)))
        } else {
            drawCircle(tinteLeicht.copy(alpha = .35f), radius, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            repeat(12) { hour ->
                val a = hour * 30f - 90f
                val long = hour % 3 == 0
                drawLine(tinteLeicht.copy(alpha = if (long) .8f else .45f), point(a, radius - (if (long) 7 else 4).dp.toPx()), point(a, radius), 1.5f.dp.toPx())
            }
            listOf(0 to "12", 3 to "3", 6 to "6", 9 to "9").forEach { (hour, label) ->
                val layout = measurer.measure(label, numberStyle)
                val at = point(hour * 30f - 90f, zahlenRadius)
                drawText(layout, topLeft = at - androidx.compose.ui.geometry.Offset(layout.size.width / 2f, layout.size.height / 2f))
            }
        }
        val sweep = geometry?.sweep
        if (geometry != null && sweep != null) {
            if (zifferblatt) {
                // Im 3D-Wecker erscheint die Schlafzeit erst, wenn ein aktiver Wecker höchstens 12 Stunden entfernt
                // ist: ein feiner oranger Bogen mit Abstand zum Gehäuserand und einer zarten Spur als Führung.
                val farben = if (snooze) listOf(accent, accent) else listOf(ORANGE_A, ORANGE_B)
                val start = geometry.nowAngle
                val verlauf = Brush.sweepGradient(
                    *(0..20).map { i -> ((start + sweep * i / 20f + 360f) % 360f) / 360f to lerpFarbe(farben[0], farben[1], i / 20f) }
                        .sortedBy { it.first }.toTypedArray(), center = center)
                val bogenLinks = androidx.compose.ui.geometry.Offset(center.x - bogenRadius, center.y - bogenRadius)
                val bogenGroesse = androidx.compose.ui.geometry.Size(bogenRadius * 2, bogenRadius * 2)
                drawCircle(farben[1].copy(alpha = .12f), bogenRadius, style = androidx.compose.ui.graphics.drawscope.Stroke(2.2f.dp.toPx()))
                drawArc(verlauf, start, sweep, useCenter = false, topLeft = bogenLinks, size = bogenGroesse,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.2f.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
            } else drawArc(accent, geometry.nowAngle, sweep, useCenter = false, topLeft = topLeft, size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        // Im 3D-Wecker bleibt das Blatt ohne Bogen leer: kein Datumsfenster, keine Punkte (Wunsch 28.09.2026).
        // Im 3D-Wecker bleibt das Blatt ohne Bogen leer: kein Datumsfenster, keine Punkte (Wunsch 28.09.2026).
        if (!zifferblatt) geometry?.centerLabel?.let { label ->
            // Fit inside the space the 12/3/6/9 numbers leave free: shrink, then try two lines, never paint over them.
            val numberRadius = zahlenRadius
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
        // Die Zeiger zeichnet [EdleZeiger] als eigene Ebene darüber, mit sekundengenauem Takt.
        if (!zifferblatt) {
            if (geometry != null) ringMarker(point(geometry.targetAngle, bogenRadius), hollow = false, color = accent, surface = surface)
            ringMarker(point(nowAngle, bogenRadius), hollow = true, color = tinteStark, surface = surface)
        }
    }
}

/**
 * Schlichts Wecker als plastisches Objekt: Bodenschatten, Metallfüße, gewölbte Glocken mit Glanzpunkt,
 * Hammer, ein Gehäuse mit umlaufendem Metallglanz und Fase, darin das leicht gewölbte Zifferblatt.
 * Alles statisch gezeichnet (keine Dauerbewegung im feststehenden Hero). [gehaeuse] = Außendurchmesser.
 */
@Composable
private fun Wecker3D(modifier: Modifier, gehaeuse: androidx.compose.ui.unit.Dp) {
    val gold = LocalGold.current
    val dunkel = gold.istDunkel
    // Im Dunkeln gedecktes, dunkleres Messing statt grellem Gold; das Blatt bleibt hell wie bei einer echten Uhr.
    val metall = if (dunkel) androidx.compose.ui.graphics.lerp(gold.primaer, androidx.compose.ui.graphics.Color(0xFF5E4B2A), .5f) else gold.primaer
    val blatt = if (dunkel) ZIFFER_ELFENBEIN else gold.flaecheErhoeht
    androidx.compose.foundation.Canvas(modifier) {
        val r = size.minDimension / 2f
        val k = gehaeuse.toPx() / 2f
        val rund = androidx.compose.ui.graphics.StrokeCap.Round
        fun punkt(winkel: Float, abstand: Float) = Math.toRadians(winkel.toDouble()).let {
            center + androidx.compose.ui.geometry.Offset((abstand * Math.cos(it)).toFloat(), (abstand * Math.sin(it)).toFloat())
        }
        fun o(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(x, y)
        val schwarz = androidx.compose.ui.graphics.Color.Black
        val weiss = androidx.compose.ui.graphics.Color.White
        // Weicher Bodenschatten unter den Füßen.
        drawOval(Brush.radialGradient(listOf(schwarz.copy(alpha = if (dunkel) .5f else .22f), schwarz.copy(alpha = 0f)),
            center = o(center.x, center.y + r * 1.04f), radius = r * .8f),
            topLeft = o(center.x - r * .8f, center.y + r * .96f), size = androidx.compose.ui.geometry.Size(r * 1.6f, r * .17f))
        // Füße: vom Gehäuse schräg nach außen, zum Boden hin dunkler.
        listOf(122f, 58f).forEach { w ->
            val a = punkt(w, k * .8f); val b = punkt(w, r * 1.06f)
            drawLine(Brush.linearGradient(listOf(metall.heller(.25f), metall.dunkler(.4f)), a, b), a, b, 7.dp.toPx(), rund)
            drawCircle(Brush.radialGradient(listOf(metall.heller(.3f), metall.dunkler(.45f)), center = b - o(2f, 2f), radius = 5.dp.toPx()), 4.5f.dp.toPx(), b)
        }
        // Hammer mitten zwischen den Glocken.
        val hammerOben = punkt(-90f, r * 1.02f)
        drawLine(metall.dunkler(.3f), punkt(-90f, k * .9f), hammerOben, 3.dp.toPx(), rund)
        drawCircle(Brush.radialGradient(listOf(metall.heller(.5f), metall.dunkler(.3f)), center = hammerOben - o(2f, 2f), radius = 6.dp.toPx()), 5.dp.toPx(), hammerOben)
        // Glocken: Kugelschattierung mit Licht von oben links, Schlagschatten auf dem Gehäuse, Glanzpunkt.
        val g = r * .34f
        listOf(-130f, -50f).forEach { w ->
            val m = punkt(w, r * .86f)
            drawCircle(schwarz.copy(alpha = if (dunkel) .35f else .16f), g, m + o(g * .1f, g * .16f))
            drawCircle(Brush.radialGradient(listOf(metall.heller(.6f), metall.heller(.12f), metall.dunkler(.38f)),
                center = m - o(g * .35f, g * .42f), radius = g * 1.45f), g, m)
            drawCircle(metall.dunkler(.45f).copy(alpha = .6f), g, m, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f.dp.toPx()))
            val glanz = m - o(g * .36f, g * .44f)
            drawCircle(Brush.radialGradient(listOf(weiss.copy(alpha = .8f), weiss.copy(alpha = 0f)), center = glanz, radius = g * .42f), g * .42f, glanz)
        }
        // Gehäuse: Schlagschatten, umlaufender Metallglanz, Fase nach innen.
        drawCircle(schwarz.copy(alpha = if (dunkel) .45f else .2f), k, center + o(0f, k * .07f))
        drawCircle(Brush.sweepGradient(listOf(metall.heller(.5f), metall.heller(.05f), metall.dunkler(.35f), metall.dunkler(.1f),
            metall.heller(.35f), metall.heller(.55f), metall.heller(.5f)), center), k)
        drawCircle(Brush.linearGradient(listOf(metall.dunkler(.4f), metall.heller(.45f)), start = center - o(k, k), end = center + o(k, k)), k * .925f)
        // Zifferblatt, leicht gewölbt, oben mit feiner Innenschattenkante.
        drawCircle(Brush.radialGradient(listOf(blatt.heller(.08f), blatt, blatt.dunkler(.08f)),
            center = center - o(k * .2f, k * .25f), radius = k * 1.1f), k * .885f)
        drawCircle(Brush.verticalGradient(listOf(schwarz.copy(alpha = if (dunkel) .24f else .16f), schwarz.copy(alpha = 0f)),
            startY = center.y - k * .885f, endY = center.y - k * .5f), k * .885f)
    }
}

/** Das Deckglas über dem Zifferblatt: ein weicher Lichtfleck oben links und eine feine Lichtkante. */
@Composable
private fun GlasKuppel(modifier: Modifier) {
    val dunkel = LocalGold.current.istDunkel
    androidx.compose.foundation.Canvas(modifier) {
        val k = size.minDimension / 2f
        val weiss = androidx.compose.ui.graphics.Color.White
        val kreis = androidx.compose.ui.graphics.Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(center, k))
        }
        clipPath(kreis) {
            drawOval(Brush.verticalGradient(listOf(weiss.copy(alpha = if (dunkel) .12f else .32f), weiss.copy(alpha = 0f)),
                startY = center.y - k, endY = center.y - k * .2f),
                topLeft = androidx.compose.ui.geometry.Offset(center.x - k * .78f, center.y - k * .98f),
                size = androidx.compose.ui.geometry.Size(k * 1.3f, k * .8f))
        }
        drawArc(weiss.copy(alpha = if (dunkel) .25f else .6f), 195f, 75f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(center.x - k * .96f, center.y - k * .96f),
            size = androidx.compose.ui.geometry.Size(k * 1.92f, k * 1.92f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

/**
 * Die Zeiger des 3D-Weckers: Stunde und Minute als facettierte Dauphine-Zeiger aus poliertem Messing mit
 * Mittelgrat und Schattenwurf aufs Blatt, dazu ein schlanker karminroter Sekundenzeiger mit Gegengewicht-Ring.
 * Eigener Sekundentakt (nur solange der Bildschirm vorne ist); Stunde und Minute laufen stufenlos mit.
 * Der Sekundenzeiger springt wie ein feines Uhrwerk mit kurzem Nachfedern, bei reduzierter Bewegung ohne.
 * Gleiche Geometrie wie [RestzeitRing]: gleiche Größe übergeben.
 */
@Composable
private fun EdleZeiger(modifier: Modifier) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val jetzt = rememberNow(1000)
    val zeit = remember(jetzt) { Instant.ofEpochMilli(jetzt).atZone(ZoneId.systemDefault()).toLocalTime() }
    val sekunde = zeit.second
    // Fortlaufender Winkel, damit der Sprung von 59 auf 0 vorwärts läuft statt einmal rückwärts herum.
    val sekundenWinkel = remember { androidx.compose.animation.core.Animatable(sekunde * 6f) }
    LaunchedEffect(jetzt / 1000) {
        var schritt = sekunde * 6f - sekundenWinkel.value % 360f
        if (schritt < 0f) schritt += 360f
        val ziel = sekundenWinkel.value + schritt
        // Mehr als ein Schritt (z. B. nach der Rückkehr in die App): direkt hin, ohne Umlauf.
        if (reduziert || schritt > 12f) sekundenWinkel.snapTo(ziel)
        else sekundenWinkel.animateTo(ziel, androidx.compose.animation.core.spring(dampingRatio = .5f, stiffness = 1100f))
    }
    val minutenWinkel = (zeit.minute + zeit.second / 60f) * 6f
    val stundenWinkel = ((zeit.hour % 12) + zeit.minute / 60f + zeit.second / 3600f) * 30f
    val metall = if (gold.istDunkel) androidx.compose.ui.graphics.lerp(gold.primaer, androidx.compose.ui.graphics.Color(0xFF5E4B2A), .35f)
        else gold.primaer
    val tinte = if (gold.istDunkel) ZIFFER_TINTE_STARK else gold.textPrimaer
    androidx.compose.foundation.Canvas(modifier) {
        val radius = size.minDimension / 2f - 5.dp.toPx()
        val schatten = androidx.compose.ui.graphics.Color.Black.copy(alpha = if (gold.istDunkel) .30f else .22f)
        val versatz = androidx.compose.ui.geometry.Offset(.9f.dp.toPx(), 1.7f.dp.toPx())

        // Dauphine-Zeiger nach oben gezeichnet; die linke Facette fängt das Licht, die rechte liegt im Schatten.
        fun dauphine(winkel: Float, laenge: Float, breite: Float) {
            val c = center
            val schulter = c.y - laenge * .16f
            val spitze = androidx.compose.ui.geometry.Offset(c.x, c.y - laenge)
            val heck = androidx.compose.ui.geometry.Offset(c.x, c.y + laenge * .14f)
            val links = androidx.compose.ui.graphics.Path().apply {
                moveTo(spitze.x, spitze.y); lineTo(c.x - breite, schulter); lineTo(heck.x, heck.y); close()
            }
            val rechts = androidx.compose.ui.graphics.Path().apply {
                moveTo(spitze.x, spitze.y); lineTo(c.x + breite, schulter); lineTo(heck.x, heck.y); close()
            }
            val umriss = androidx.compose.ui.graphics.Path().apply {
                moveTo(spitze.x, spitze.y); lineTo(c.x + breite, schulter); lineTo(heck.x, heck.y)
                lineTo(c.x - breite, schulter); close()
            }
            translate(versatz.x, versatz.y) { rotate(winkel, c) { drawPath(umriss, schatten) } }
            rotate(winkel, c) {
                drawPath(links, Brush.linearGradient(listOf(metall.heller(.62f), metall.heller(.18f)),
                    spitze, androidx.compose.ui.geometry.Offset(c.x - breite, schulter)))
                drawPath(rechts, Brush.linearGradient(listOf(metall.dunkler(.12f), metall.dunkler(.48f)),
                    spitze, androidx.compose.ui.geometry.Offset(c.x + breite, schulter)))
                drawPath(umriss, tinte.copy(alpha = .55f), style = androidx.compose.ui.graphics.drawscope.Stroke(.6f.dp.toPx(),
                    join = androidx.compose.ui.graphics.StrokeJoin.Round))
                // Feiner Mittelgrat, der das Licht wie poliertes Metall bricht.
                drawLine(androidx.compose.ui.graphics.Color.White.copy(alpha = .45f), spitze,
                    androidx.compose.ui.geometry.Offset(c.x, c.y - laenge * .08f), .45f.dp.toPx())
            }
        }

        // Minute endet vor dem Schlafbogen, die Stunde ist deutlich kürzer – auch bei kleinen Uhren.
        val minutenLaenge = maxOf(radius - 17.dp.toPx(), radius * .7f)
        dauphine(stundenWinkel, minutenLaenge * .68f, (radius * .075f).coerceAtLeast(2.6f.dp.toPx()))
        dauphine(minutenWinkel, minutenLaenge, (radius * .058f).coerceAtLeast(2.dp.toPx()))

        // Sekundenzeiger: schlanke Nadel bis in die Minutenschiene, hinten ein Ring als Gegengewicht.
        val spitze = radius - 2.dp.toPx()
        val ringMitte = radius * .2f
        val ringRadius = 2.4f.dp.toPx()
        fun sekundenNadel(farbe: androidx.compose.ui.graphics.Color) {
            val c = center
            val rund = androidx.compose.ui.graphics.StrokeCap.Round
            drawLine(farbe, androidx.compose.ui.geometry.Offset(c.x, c.y + ringMitte - ringRadius),
                androidx.compose.ui.geometry.Offset(c.x, c.y - spitze), 1.dp.toPx(), rund)
            drawCircle(farbe, ringRadius, androidx.compose.ui.geometry.Offset(c.x, c.y + ringMitte),
                style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            drawLine(farbe, androidx.compose.ui.geometry.Offset(c.x, c.y + ringMitte + ringRadius),
                androidx.compose.ui.geometry.Offset(c.x, c.y + ringMitte + ringRadius + 2.dp.toPx()), 1.6f.dp.toPx(), rund)
        }
        translate(versatz.x * 1.3f, versatz.y * 1.3f) { rotate(sekundenWinkel.value, center) { sekundenNadel(schatten) } }
        rotate(sekundenWinkel.value, center) { sekundenNadel(SEKUNDE_KARMIN) }

        // Mittelachse: Messingkappe mit Glanz, darauf die karminrote Sekundennabe und ein polierter Stift.
        val kappe = (radius * .085f).coerceAtLeast(3.5f.dp.toPx())
        drawCircle(schatten, kappe, center + versatz)
        drawCircle(Brush.radialGradient(listOf(metall.heller(.6f), metall, metall.dunkler(.4f)),
            center = center - androidx.compose.ui.geometry.Offset(kappe * .35f, kappe * .35f), radius = kappe * 1.4f), kappe)
        drawCircle(tinte.copy(alpha = .5f), kappe, style = androidx.compose.ui.graphics.drawscope.Stroke(.5f.dp.toPx()))
        drawCircle(SEKUNDE_KARMIN, kappe * .55f)
        drawCircle(Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color.White.copy(alpha = .9f), metall.heller(.3f)),
            center = center - androidx.compose.ui.geometry.Offset(kappe * .1f, kappe * .1f), radius = kappe * .3f), kappe * .25f)
    }
}

/**
 * Stummes Klingeln im Ruhezustand: Nach zufällig 7,5 bis 13 Sekunden wackelt der 3D-Wecker gut eine Sekunde lang
 * wie beim Wecken (zwei kurze Stöße, dazwischen eine Pause), ganz ohne Ton. Läuft nur, solange der
 * Bildschirm vorne ist; bei reduzierter Bewegung steht er still. Liefert den Drehwinkel in Grad.
 */
@Composable
private fun rememberGelegentlichesKlingeln(): State<Float> {
    val winkel = remember { androidx.compose.animation.core.Animatable(0f) }
    val aktiv = rememberResumed() && !LocalBewegungReduziert.current
    LaunchedEffect(aktiv) {
        if (!aktiv) { winkel.snapTo(0f); return@LaunchedEffect }
        while (true) {
            kotlinx.coroutines.delay(7_500L + kotlin.random.Random.nextLong(5_501L))
            winkel.animateTo(0f, androidx.compose.animation.core.keyframes {
                durationMillis = 1300
                0f at 0; 5f at 60; -5f at 120; 5f at 180; -5f at 240; 4f at 300; -4f at 360; 2f at 420; 0f at 480
                0f at 760; 4f at 820; -4f at 880; 3f at 940; -3f at 1000; 1.5f at 1060; 0f at 1120; 0f at 1300
            })
        }
    }
    return winkel.asState()
}

/** Sekundenzeiger: tiefes Karminrot – klassisch, hebt sich von Messing und vom nachtblauen Schlafbogen ab. */
private val SEKUNDE_KARMIN = androidx.compose.ui.graphics.Color(0xFFA8322B)

/** Schlafbogen im 3D-Wecker: warmes Orange, hebt sich vom Messing und vom Elfenbeinblatt ab. */
private val ORANGE_A = androidx.compose.ui.graphics.Color(0xFFF29A38)
private val ORANGE_B = androidx.compose.ui.graphics.Color(0xFFE2701C)
/** Zifferblatt im Dunkelmodus: gedämpftes Elfenbein mit dunkler Tinte – eine Uhr hat nie ein schwarzes Blatt. */
private val ZIFFER_ELFENBEIN = androidx.compose.ui.graphics.Color(0xFFD9D0BB)
private val ZIFFER_TINTE_LEICHT = androidx.compose.ui.graphics.Color(0xFF6A604D)
private val ZIFFER_TINTE_STARK = androidx.compose.ui.graphics.Color(0xFF2A2418)
private fun lerpFarbe(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color, t: Float) =
    androidx.compose.ui.graphics.lerp(a, b, t)

/** Der 3D-Wecker als freistehende Figur mit laufender Uhrzeit – z. B. im Leerzustand. [groesse] = Gesamtbreite. */
@Composable
fun Wecker3DFigur(now: Long, groesse: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val ring = groesse * 0.8f
    Box(modifier.size(groesse), contentAlignment = Alignment.Center) {
        Wecker3D(Modifier.size(ring + 16.dp), gehaeuse = ring + 14.dp)
        RestzeitRing(now, null, false, Modifier.size(ring), zifferblatt = true)
        EdleZeiger(Modifier.size(ring))
        GlasKuppel(Modifier.size(ring))
    }
}

/** Erster Start ohne Wecker: ein schwebender 3D-Wecker, eine klare Zeile, ein Knopf. */
@Composable
private fun LeererStart(now: Long, aufNeu: () -> Unit) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val schweben = if (reduziert) 0f else {
        val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "schweben")
        t.animateFloat(-4f, 4f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2600),
            androidx.compose.animation.core.RepeatMode.Reverse), label = "hub").value
    }
    LocalGestalt.current.Flaeche(Modifier.fillMaxWidth(), erhoeht = true) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val klingeln = rememberGelegentlichesKlingeln()
            Wecker3DFigur(now, 150.dp, Modifier.graphicsLayer {
                translationY = schweben * density
                rotationZ = klingeln.value
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.85f)
            })
            Spacer(Modifier.height(6.dp))
            Text("Dein Morgen, deine Reihenfolge", style = MaterialTheme.typography.titleLarge, color = gold.textPrimaer,
                textAlign = TextAlign.Center)
            Text("Weckton, Musik und ein Text, den dir eine natürliche Stimme vorliest.",
                style = MaterialTheme.typography.bodyMedium, color = gold.textGedaempft, textAlign = TextAlign.Center)
            GoldKnopf("Ersten Wecker anlegen", aufNeu, hauptKnopf = true, symbol = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) })
        }
    }
}

/** Deterministischer Sternenhimmel mit Mond für Traumraums Kuppel — keine Animation, kein Zufall pro Bild. */
@Composable
private fun Sternenhimmel(modifier: Modifier) {
    val gold = LocalGold.current
    val stern = if (gold.istDunkel) androidx.compose.ui.graphics.Color(0xFFFFF4E0) else gold.akzentWarm
    val glut = gold.primaer
    val mond = if (gold.istDunkel) androidx.compose.ui.graphics.Color(0xFFFFE9B8) else androidx.compose.ui.graphics.Color(0xFFFFF6DC)
    val himmel = gold.heroGrund
    // Die Sterne leben: Sie gehen auf, funkeln, verlöschen; Satelliten und Sternschnuppen ziehen vorbei.
    de.frank.wecker.design.FunkelHimmel(modifier, stern = stern, akzent = glut, dichte = 3.2f, satelliten = true)
    androidx.compose.foundation.Canvas(modifier) {
        // Mondsichel rechts unterhalb der Statuszeile, damit sie nie hinter „Weckbereit“ liegt.
        val m = androidx.compose.ui.geometry.Offset(size.width - 34.dp.toPx(), 52.dp.toPx())
        val mr = 13.dp.toPx()
        drawCircle(Brush.radialGradient(listOf(mond.copy(alpha = .35f), androidx.compose.ui.graphics.Color.Transparent), center = m, radius = mr * 2.6f), mr * 2.6f, m)
        drawCircle(mond, mr, m)
        drawCircle(himmel, mr * 0.86f, m + androidx.compose.ui.geometry.Offset(mr * 0.45f, -mr * 0.25f))
    }
}

/**
 * Morgenruhes rechter Block neben Datum und Uhr: Termin oder Leerstatus und die Wecker-Aktionen,
 * rechtsbündig gestapelt. Früher standen sie als eigene Zeilen unter der Uhr. Die Weckbereitschaft
 * steht seit 1.1.119 links über dem Messingstrich; „Öffnen“ entfällt, der Termin selbst ist antippbar.
 * In schmalen Spalten (zugeklappt, geteilter Bildschirm) brechen Texte auf zwei Zeilen und die
 * Knöpfe untereinander um, statt abgeschnitten zu werden. [spalte] ist die Breite dieses Blocks.
 */
@Composable
private fun MorgenruheRechts(
    daten: HeroDaten, spalte: androidx.compose.ui.unit.Dp, aufOeffnen: () -> Unit, aufNeu: () -> Unit,
    aufSchlummernBeenden: () -> Unit, modifier: Modifier = Modifier,
) {
    val gold = LocalGold.current
    val semantisch = LocalSemantisch.current
    val rechts = androidx.compose.ui.text.style.TextAlign.End
    val oeffenbar = daten.nextAlarm != null
    // „Wecker anlegen“ samt Plus braucht rund 160 dp (mit der Systemschrift mehr); darunter genügt „Neu“.
    val knopfText = if (spalte < 160.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.5f)) "Neu" else "Wecker anlegen"
    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(
            Modifier.then(if (oeffenbar) Modifier.clickable(
                onClickLabel = "Nächsten Wecker öffnen", onClick = aufOeffnen) else Modifier),
            horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (daten.next == null) {
                Text(daten.leerGrund, style = MaterialTheme.typography.titleSmall, color = gold.heroSchrift,
                    textAlign = rechts, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (daten.anzahl == 0) "Tippe auf „$knopfText“" else daten.leerHilfe,
                    style = MaterialTheme.typography.bodySmall, color = gold.heroFuehrung,
                    textAlign = rechts, maxLines = 2, overflow = TextOverflow.Ellipsis)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LegendenMarker(hollow = false, color = if (daten.nextIsSnooze) semantisch.info else gold.primaer)
                    Text("${if (daten.nextIsSnooze) "Schlummern bis" else "Wecker"} ${terminAnzeige(daten.now, daten.next).einzeilig}",
                        Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium,
                        color = gold.heroSchrift, textAlign = rechts, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(daten.nextName ?: "Wecker", style = MaterialTheme.typography.titleSmall, color = gold.heroSchrift,
                    textAlign = rechts, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("noch ${remainingLong(daten.next - daten.now)}", style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = gold.heroFuehrung,
                    textAlign = rechts, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GoldKnopf(knopfText, aufNeu, hauptKnopf = true, beschreibung = "Neuen Wecker anlegen",
                symbol = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) })
            if (daten.nextIsSnooze) StillerKnopf("Schlummern beenden", aufSchlummernBeenden, hervorgehoben = true)
        }
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
            Box(Modifier.size(12.dp).graphicsLayer { alpha = pulse }.background(LocalSemantisch.current.fehler, androidx.compose.foundation.shape.CircleShape))
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
        // Text on top with full width, buttons below and wrapping: readable on narrow screens and with large fonts.
        open != null -> Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val waiting = open ?: return@Column
            val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, null, tint = gold.primaer, modifier = Modifier.size(20.dp))
                Text("Offenes Diktat für „${waiting.draftName}“${if (waiting.createdAt > 0) " vom ${formatAt(waiting.createdAt)}" else ""}" +
                    "${if (waiting.missing > 0) " (unvollständig)" else ""} – im Wecker einfügen, kopieren oder verwerfen.", Modifier.weight(1f).padding(start = 10.dp),
                    style = MaterialTheme.typography.bodySmall)
            }
            // Copy is always reachable, also for dictations whose draft no longer exists.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StillerKnopf("Text kopieren", { clipboard.setText(androidx.compose.ui.text.AnnotatedString(waiting.text)); vm.message.value = "Diktattext kopiert." })
                StillerKnopf("Verwerfen", vm::discardOpenDictation)
            }
        }
    }
}

/** Offers a waiting dictation only in the editor of its original alarm, for a conscious insert. */
@Composable
private fun OffenesDiktatKarte(vm: WeckerViewModel, alarm: Alarm) {
    val open by vm.openDictation.collectAsStateWithLifecycle()
    val dictation = open?.takeIf { it.draftId == alarm.id } ?: return
    LocalGestalt.current.Flaeche(Modifier.fillMaxWidth(), erhoeht = false) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Offenes Diktat", style = MaterialTheme.typography.titleSmall, color = LocalGold.current.primaer)
            Text("„${dictation.text.take(160)}${if (dictation.text.length > 160) "…" else ""}“", style = MaterialTheme.typography.bodySmall)
            // Never shown as complete when parts were not transcribed.
            if (dictation.missing > 0) StatusZeile(Icons.Default.Warning, "Unvollständig: ${dictation.missing} Abschnitte fehlen.", LocalSemantisch.current.warnung)
            val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldKnopf("In „Deine Erinnerung“ einfügen", vm::insertOpenDictation)
                StillerKnopf("Text kopieren", { clipboard.setText(androidx.compose.ui.text.AnnotatedString(dictation.text)); vm.message.value = "Diktattext kopiert." })
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
        Chip3D(sleep == 0, { if (sleep != 0) change(alarm.copy(sleepMinutes = 0)) }, "Aus")
        listOf(7, 8, 9).forEach { hours ->
            Chip3D(sleep == hours * 60, { if (sleep != hours * 60) change(alarm.copy(sleepMinutes = hours * 60)) }, "$hours Std.")
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
    Text(if (sleep == 0) "Ohne Angabe wird keine Schlafenszeit angezeigt." else "Zeigt die ungefähre Schlafenszeit. Kein Tracking.",
        style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
    if (sleep > 0) SchlafErinnerungProWecker(alarm, change)
}

/** Anzeigename eines Sonnenzeitpunkts im Editor; die Wecklogik und ihre Bezeichnungen bleiben unberührt. */
private fun sonnenTitel(e: SonnenWecker.Ereignis): String = when (e) {
    SonnenWecker.Ereignis.DAEMMERUNG -> "Beginn der Morgendämmerung"
    SonnenWecker.Ereignis.AUFGANG -> "Sonnenaufgang (Empfehlung)"
    SonnenWecker.Ereignis.TAGHELL -> "Heller Morgen"
}

private fun versatzKurz(v: Int): String = when {
    v == 0 -> "genau"
    v < 0 -> "${-v} Min. früher"
    else -> "$v Min. später"
}

/**
 * Sonnenaufgangswecker: Schalter, Wahl des Sonnenzeitpunkts in einem deckenden Fenster, Zeitleiste zur
 * Feinabstimmung und die nächsten berechneten Weckzeiten. Die Berechnung selbst bleibt in [SonnenWecker].
 */
@Composable
private fun SonnenWahl(alarm: Alarm, change: (Alarm) -> Unit) {
    val gold = LocalGold.current
    val an = alarm.sunMode.isNotBlank()
    // Schalter direkt rechts neben der Beschriftung statt am Zeilenende.
    Row(Modifier.heightIn(min = 48.dp).toggleable(an, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = { ein ->
            // Ein neuer Sonnenaufgangswecker startet genau zum Zeitpunkt, ohne Abweichung.
            if (ein) change(alarm.copy(sunMode = SonnenWecker.STANDARD.id, sunOffsetMinutes = 0))
            // Beim Ausschalten bleibt die zuletzt berechnete Sonnen-Uhrzeit als feste Weckzeit stehen, statt still auf die alte zu springen.
            else { val m = alarm.sonnenMinute() ?: (alarm.hour * 60 + alarm.minute)
                change(alarm.copy(sunMode = "", sunOffsetMinutes = 0, hour = m / 60, minute = m % 60)) }
        }), verticalAlignment = Alignment.CenterVertically) {
        Text("Sonnenaufgangswecker"); Spacer(Modifier.width(16.dp)); Schalter3D(an, null)
    }
    if (!an) return
    val ereignis = SonnenWecker.Ereignis.von(alarm.sunMode) ?: SonnenWecker.STANDARD
    var versatz by remember(alarm.id, alarm.sunOffsetMinutes) { mutableIntStateOf(alarm.sunOffsetMinutes) }
    var wahlOffen by rememberSaveable(alarm.id) { mutableStateOf(false) }
    GoldKnopf("${sonnenTitel(ereignis)} · ${versatzKurz(versatz)}", { wahlOffen = true }, Modifier.fillMaxWidth())
    if (wahlOffen) androidx.compose.ui.window.Dialog(onDismissRequest = { wahlOffen = false }) {
        // Eigene, vollständig deckende Fläche ohne Überschrift: nur die drei Sonnenzeitpunkte.
        LocalGestalt.current.Flaeche(Modifier.fillMaxWidth().background(gold.flaecheErhoeht, RoundedCornerShape(LocalDesignTokens.current.karteRadius)), erhoeht = true) {
            Column(Modifier.fillMaxWidth().background(gold.flaecheErhoeht).padding(vertical = 8.dp)) {
                SonnenWecker.Ereignis.entries.forEach { e ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(e == ereignis, interactionSource = null, indication = null,
                        role = androidx.compose.ui.semantics.Role.RadioButton) { change(alarm.copy(sunMode = e.id)); wahlOffen = false }
                        .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(e == ereignis, null); Text(sonnenTitel(e), Modifier.padding(start = 12.dp), color = gold.textPrimaer)
                    }
                }
            }
        }
    }
    fun setze(v: Int) { versatz = v.coerceIn(SonnenWecker.VERSATZ_MIN, SonnenWecker.VERSATZ_MAX); change(alarm.copy(sunOffsetMinutes = versatz)) }
    // Ablesen und Feinabstimmung minutengenau: Die Zeitleiste rastet in 5-Minuten-Schritten, − und + verschieben um je eine Minute.
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton({ setze(versatz - 1) }, enabled = versatz > SonnenWecker.VERSATZ_MIN) { Icon(Icons.Default.Remove, "Eine Minute früher", tint = gold.primaer) }
        Text(if (versatz == 0) "Manuell: Genau zum Zeitpunkt" else "Manuell: ${versatzKurz(versatz)}",
            Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelLarge, color = gold.primaer)
        IconButton({ setze(versatz + 1) }, enabled = versatz < SonnenWecker.VERSATZ_MAX) { Icon(Icons.Default.Add, "Eine Minute später", tint = gold.primaer) }
    }
    Regler3D(versatz.toFloat(), { versatz = (it / 5f).roundToInt() * 5 },
        Modifier.semantics { contentDescription = "Abweichung zum Sonnenzeitpunkt"; stateDescription = if (versatz == 0) "genau zum Zeitpunkt" else versatzKurz(versatz) },
        bereich = SonnenWecker.VERSATZ_MIN.toFloat()..SonnenWecker.VERSATZ_MAX.toFloat(),
        stufen = (SonnenWecker.VERSATZ_MAX - SonnenWecker.VERSATZ_MIN) / 5 - 1,
        aufAenderungFertig = { change(alarm.copy(sunOffsetMinutes = versatz)) })
    Row(Modifier.fillMaxWidth()) {
        Text("Früher", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
        Text("Später", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
    }
    val context = LocalContext.current
    var ortStand by remember { mutableIntStateOf(0) }
    val freigabe = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        SonnenOrt.aktualisiere(context); SonnenOrt.neuPlanen(context); ortStand++
    }
    LaunchedEffect(Unit) { SonnenOrt.aktualisiere(context); ortStand++ }
    val heute = remember(rememberNow(60_000) / 3_600_000) { java.time.LocalDate.now() }
    val vorschau = remember(alarm.sunMode, versatz, heute, ortStand) { SonnenWecker.vorschau(alarm.copy(sunOffsetMinutes = versatz), heute.plusDays(1)) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        vorschau.forEach { (tag, uhr) ->
            Row(Modifier.fillMaxWidth()) {
                Text(tag.format(DateTimeFormatter.ofPattern("EEEE, dd.MM.", java.util.Locale.GERMAN)), Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium, color = gold.textPrimaer)
                Text(uhr, style = MaterialTheme.typography.bodyMedium, fontFamily = zahlSchrift(), color = gold.primaer)
            }
        }
    }
    val zone = remember(heute, ZoneId.systemDefault()) { ZoneId.systemDefault().let { it.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.GERMAN).ifBlank { it.id } } }
    Text("Berechnet für deine Zeitzone: $zone", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
    // Ohne Freigabe rechnet der Wecker mit einem geschätzten Ort; nur dann gibt es den Knopf dafür.
    if (!remember(ortStand) { SonnenOrt.erlaubt(context) })
        StillerKnopf("Ungefähren Standort erlauben", { freigabe.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION) }, hervorgehoben = true)
    Text("Wiederholung, Schlafdauer und Schlafzeiterinnerung richten sich automatisch nach der jeweils berechneten Weckzeit.",
        style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
}

/**
 * Schlafenszeit-Erinnerung direkt am Wecker: ohne eigene Wahl folgt sie Schalter und Vorlauf der globalen
 * Einstellungen; wer hier schaltet oder zieht, legt es für diesen Wecker fest.
 */
@Composable
private fun SchlafErinnerungProWecker(alarm: Alarm, change: (Alarm) -> Unit) {
    val context = LocalContext.current
    val globalAn = remember { SchlafErinnerung.enabled(context) }
    val globalVorlauf = remember { SchlafErinnerung.leadMinutes(context) }
    val an = SchlafPlan.an(alarm, globalAn)
    var vorlauf by remember(alarm.id, alarm.sleepLeadMinutes) { mutableIntStateOf(SchlafPlan.vorlauf(alarm, globalVorlauf)) }
    Toggle("Schlafenszeit-Erinnerung", an) { change(alarm.copy(sleepReminder = it, sleepLeadMinutes = alarm.sleepLeadMinutes ?: vorlauf)) }
    if (!an) return
    val vorlaufText = if (vorlauf == 0) "zur Schlafenszeit" else "$vorlauf Min. vor dem Schlafengehen"
    Text("Erinnerung $vorlaufText", style = MaterialTheme.typography.labelLarge, color = LocalGold.current.primaer)
    Regler3D(vorlauf.toFloat(), { vorlauf = it.roundToInt() },
        Modifier.semantics {
            contentDescription = "Vorlauf der Schlafenszeit-Erinnerung für diesen Wecker"
            stateDescription = if (vorlauf == 0) "zur Schlafenszeit" else "$vorlauf Minuten vorher"
        },
        bereich = 0f..SchlafPlan.LEAD_MAX_MINUTES.toFloat(), stufen = SchlafPlan.LEAD_MAX_MINUTES - 1,
        aufAenderungFertig = { change(alarm.copy(sleepReminder = true, sleepLeadMinutes = vorlauf)) })
    // Uhrzeit aus der nächsten Weckzeit dieses Entwurfs; ohne berechenbaren Termin bleibt es beim Vorlauf.
    val minute = rememberNow(60_000)
    val erinnerung = remember(alarm, vorlauf, minute / 60_000) {
        runCatching {
            val weck = AlarmTime.next(alarm).takeIf { it > 0 } ?: return@runCatching null
            val bett = Schlaf.bedtime(weck, alarm.sleepMinutes)
            fun uhr(t: Long) = Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
            uhr(bett - SchlafPlan.leadMs(vorlauf)) to uhr(bett)
        }.getOrNull()
    }
    Text(when {
        erinnerung == null -> "Die Erinnerung kommt $vorlaufText."
        vorlauf == 0 -> "Um ${erinnerung.second} Uhr kommt die Schlaferinnerung – genau zur Schlafenszeit."
        else -> "Um ${erinnerung.first} Uhr kommt die Schlaferinnerung – $vorlauf Minuten vor der Schlafenszeit um ${erinnerung.second} Uhr."
    }, style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
    // Genau der Text, der vorgelesen wird (bei mehreren Weckern zur selben Schlafenszeit werden sie zusammengefasst).
    val ansage = remember(alarm, vorlauf, minute / 60_000) { SchlafAnsage.vorschau(alarm, vorlauf) }
    if (ansage != null) Text("Ansage: „${ansage.removePrefix("Schlafenszeit-Erinnerung. ")}“",
        style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textPrimaer)
}

/** What saving will do: switched on, and the term it will then ring. Not a claim that it is already planned. */
@Composable
private fun SpeicherVorschau(alarm: Alarm) {
    val minute = rememberNow(60_000)
    // Dieselbe reine Prüfung, die AlarmScheduler.save() als erstes ausführt: Was dort scheitern würde,
    // darf hier nicht als Erfolg angekündigt werden. Keine zweite Regel, kein Eingriff ins Speichern.
    val fehler = remember(alarm) {
        runCatching { alarm.validate() }.exceptionOrNull()
            ?.let { it.message?.takeIf { text -> text.isNotBlank() } ?: "Diese Eingaben ergeben noch keinen gültigen Wecker." }
    }
    val next = remember(alarm, minute) { runCatching { AlarmTime.next(alarm, Instant.ofEpochMilli(minute)) }.getOrNull() }
    @Composable fun Warnung(text: String) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Warning, null, tint = LocalSemantisch.current.warnung, modifier = Modifier.size(18.dp))
        Text(text, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodySmall, color = LocalSemantisch.current.warnung)
    }
    when {
        // Die Fehlermeldung kommt sonst erst nach dem Tippen auf „Wecker speichern“.
        fehler != null -> Warnung(fehler)
        next != null -> {
            Text("Nach dem Speichern eingeschaltet · klingelt dann ${terminAnzeige(minute, next).einzeilig} (in ${remainingLong(next - minute)})",
                Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textPrimaer,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (alarm.sleepMinutes > 0) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SchlafZeile("Nach dem Speichern: " + Schlaf.hinweis(next, alarm.sleepMinutes, minute))
            }
        }
        else -> Warnung("Nach dem Speichern gäbe es keinen zukünftigen Termin – ändere Datum oder Uhrzeit.")
    }
}

fun remaining(ms: Long): String { val minutes = ZeitRing.ceilMinutes(ms); return "${minutes / 60} Std. ${minutes % 60} Min." }


@Composable
fun ReadinessCard(onSettings: () -> Unit, hideWhenReady: Boolean = false,
    /** Vorgegebener Zustand; ohne Vorgabe fragt die Karte selbst. Die Weckerliste reicht ihn
     *  herein, damit dort nicht zwei Zwei-Sekunden-Abfragen nebeneinander laufen. */
    zustand: List<Pair<String, Boolean>>? = null) {
    val state = zustand ?: rememberReadiness()
    if (hideWhenReady && state.all { it.second }) return
    LocalGestalt.current.Flaeche(Modifier, erhoeht = false) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (state.all { it.second }) Icons.Default.VerifiedUser else Icons.Default.NotificationsActive,
                null, tint = if (state.all { it.second }) LocalSemantisch.current.erfolg else LocalSemantisch.current.warnung)
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
    // Wecker kommen in jedem Nicht-stören-Modus ab Werk durch (USAGE_ALARM). Rot wird die Zeile nur, wenn
    // der gerade aktive Modus sie wirklich stummschaltet — ein Zugriffsrecht braucht dafür niemand.
    val filter = manager.currentInterruptionFilter
    val allowsAlarms = Build.VERSION.SDK_INT < 28 || policy == null || (policy.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS) != 0
    val dndLaesstWeckerDurch = filter != NotificationManager.INTERRUPTION_FILTER_NONE &&
        (filter != NotificationManager.INTERRUPTION_FILTER_PRIORITY || allowsAlarms)
    return listOf("Genaue Weckzeiten" to AlarmScheduler(context).allowed(),
        "Benachrichtigungen" to manager.areNotificationsEnabled(),
        "Vollbild-Wecker" to (Build.VERSION.SDK_INT < 34 || manager.canUseFullScreenIntent()),
        // Entsperrt erscheint der Wecker nur mit diesem Recht als Vollbild statt als kleine Meldung oben.
        "Vollbild auch entsperrt" to android.provider.Settings.canDrawOverlays(context),
        "Wecker bei Nicht stören" to dndLaesstWeckerDurch,
        // Samsung puts optimized apps to sleep; unrestricted battery keeps restore and preparation alive.
        "Akku uneingeschränkt" to context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName))
}


/**
 * Traumraums Terminangaben als kleine Liste mit Punkten: „Nächster Wecker 04:01 · morgen“,
 * „Name: Frühschicht“, „Klingelt in 17 Std. 3 Min.“ — gleich hoch in jedem Zustand.
 */
@Composable
private fun TraumTermin(daten: HeroDaten, aufOeffnen: () -> Unit) {
    val gold = LocalGold.current
    val akzent = if (daten.nextIsSnooze) LocalSemantisch.current.info else gold.primaer
    @Composable fun Zeile(text: String, farbe: androidx.compose.ui.graphics.Color, stil: androidx.compose.ui.text.TextStyle, zeilen: Int = 1) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(akzent))
            Text(text, Modifier.padding(start = 8.dp), style = stil, color = farbe, minLines = zeilen, maxLines = zeilen, overflow = TextOverflow.Ellipsis)
        }
    }
    Column(Modifier.fillMaxWidth().then(if (daten.nextAlarm != null) Modifier.clickable(
        onClickLabel = "Nächsten Wecker öffnen", onClick = aufOeffnen) else Modifier),
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (daten.next == null) {
            Zeile(daten.leerTitel, gold.textPrimaer, MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Zeile(daten.leerGrund, gold.textPrimaer, MaterialTheme.typography.bodyMedium)
            Zeile(daten.leerHilfe, akzent, MaterialTheme.typography.bodyMedium)
        } else {
            val termin = terminAnzeige(daten.now, daten.next).einzeilig
            Zeile("${if (daten.nextIsSnooze) "Schlummern bis" else "Nächster Wecker"} $termin", gold.textPrimaer,
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Zeile("Name: ${daten.nextName ?: "Wecker"}", gold.textPrimaer, MaterialTheme.typography.bodyMedium)
            Zeile("Klingelt in ${remainingLong(daten.next - daten.now)}", akzent, MaterialTheme.typography.bodyMedium)
        }
    }
}


/**
 * Die einzige Stelle, die die Wahl auflöst: „system“ bedeutet Automatik nach der Sonne in Neuenhagen,
 * nicht nach dem Android-System. Reine Rechnung ohne Speicherzugriff, also auch vor dem ersten Entsperren sicher.
 */
@Composable
fun wirksamesTheme(wahl: String): String {
    // Automatisch folgt der Sonne: jede Minute prüfen, nur solange sichtbar.
    var sonneDunkel by remember(wahl) { mutableStateOf(Sonnenstand.istDunkel()) }
    val lebenszyklus = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lebenszyklus, wahl) {
        if (wahl == "light" || wahl == "dark") return@LaunchedEffect
        lebenszyklus.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                sonneDunkel = Sonnenstand.istDunkel()
                delay(60_000L)
            }
        }
    }
    return if (wahl == "system") (if (sonneDunkel) "dark" else "light") else wahl
}


/** „Anhören“ wird während der Vorschau zu „Stopp“; ein zweiter Tipp beendet sie. */
@Composable
fun AnhoerKnopf(vm: WeckerViewModel, schluessel: String, abspielen: () -> Unit) {
    val laeuft by vm.vorschau.collectAsStateWithLifecycle()
    val aktiv = laeuft == schluessel
    StillerKnopf(if (aktiv) "■ Stopp" else "Anhören", {
        if (aktiv) vm.stopPreview() else { abspielen(); vm.vorschau.value = schluessel }
    }, hervorgehoben = aktiv)
}


@Composable
private fun AnhoerKnopfText(vm: WeckerViewModel, alarm: Alarm) {
    val laeuft by vm.vorschau.collectAsStateWithLifecycle()
    val aktiv = laeuft == "text:${alarm.id}"
    StillerKnopf(if (aktiv) "■ Test stoppen" else "Test vorlesen", {
        if (aktiv) vm.stopPreview()
        else if (alarm.text.isBlank()) vm.message.value = "Gib zuerst einen Text ein."
        else { vm.previewVoice(alarm, alarm.text); vm.vorschau.value = "text:${alarm.id}" }
    }, hervorgehoben = aktiv)
}


/**
 * Die Schmuckebene hinter den Heros, je Design eigen: Schlicht goldenes Funkeln, Morgenruhe eine
 * aufgehende Sonne mit Strahlen, Orbit Umlaufbahnen mit Planeten. Traumraum hat seinen Sternenhimmel.
 * Deterministisch und ohne Animation — der Hero steht dauerhaft auf dem Schirm.
 */
@Composable
private fun HeroDeko(modifier: Modifier) {
    val gold = LocalGold.current
    val design = LocalDesignTokens.current.design
    androidx.compose.foundation.Canvas(modifier) {
        val zufall = java.util.Random(7)
        when (design) {
            // Feste Plätze statt Zufall: nur in den freien Zonen — zwischen Zifferblatt und
            // rechtsbündigem Text und unten zwischen „Öffnen“ und „Weckbereit“, nie unter Schrift.
            Design.SCHLICHT -> listOf(
                Triple(0.43f, 0.10f, 3.2f), Triple(0.48f, 0.28f, 2.0f), Triple(0.41f, 0.46f, 2.6f), Triple(0.47f, 0.62f, 1.8f),
                Triple(0.53f, 0.83f, 2.4f), Triple(0.60f, 0.93f, 1.6f), Triple(0.66f, 0.80f, 2.8f), Triple(0.72f, 0.91f, 1.8f),
            ).forEach { (fx, fy, groesse) ->
                val x = size.width * fx
                val y = size.height * fy
                val r = groesse.dp.toPx()
                val a = if (gold.istDunkel) 0.45f else 0.35f
                val pfad = androidx.compose.ui.graphics.Path().apply {
                    moveTo(x, y - r * 2); quadraticTo(x, y, x + r * 2, y); quadraticTo(x, y, x, y + r * 2)
                    quadraticTo(x, y, x - r * 2, y); quadraticTo(x, y, x, y - r * 2); close()
                }
                drawPath(pfad, gold.primaer.copy(alpha = a))
                drawCircle(androidx.compose.ui.graphics.Color.White.copy(alpha = a * 0.8f), r * 0.35f, androidx.compose.ui.geometry.Offset(x, y))
            }
            // Morgenruhe: keine feste Sonne mehr, die Sonne geht in der Schlafszene selbst auf.
            Design.ORBIT -> {
                // Die Bahnen und Planeten zeichnet jetzt das animierte Sonnensystem; hier bleiben nur Sterne.
                repeat(18) {
                    drawCircle(gold.heroSchrift.copy(alpha = .10f + zufall.nextFloat() * .2f), (0.6f + zufall.nextFloat()).dp.toPx(),
                        androidx.compose.ui.geometry.Offset(size.width * zufall.nextFloat(), size.height * zufall.nextFloat()))
                }
            }
            else -> {}
        }
    }
}


/** Nur die Art der Wiederholung — Uhrzeit und Datum stehen schon neben der großen Zahl. */
fun kurzPlan(alarm: Alarm): String = when {
    alarm.repeatUnit == Alarm.MONTHLY -> if (alarm.repeatEvery <= 1) "Monatlich" else "Alle ${alarm.repeatEvery} Monate"
    alarm.repeatUnit == Alarm.YEARLY -> if (alarm.repeatEvery <= 1) "Jährlich" else "Alle ${alarm.repeatEvery} Jahre"
    alarm.intervalDays > 0 -> "Alle ${alarm.intervalDays} Tage"
    alarm.startDate.isNotBlank() -> "Einmalig"
    else -> dayLabel(alarm.days)
}


/**
 * Der Abstand zwischen Kopfleiste und der ersten Fläche — auf Liste, Editor und Einstellungen
 * **derselbe**, damit die oberste Linie beim Seitenwechsel nicht springt.
 */
@Composable
fun seitenAbstandOben(): androidx.compose.ui.unit.Dp =
    if (LocalDesignTokens.current.design == Design.TRAUMRAUM) 4.dp else 12.dp

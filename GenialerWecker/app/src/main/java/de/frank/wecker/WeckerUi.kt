@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package de.frank.wecker

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable

@Composable
fun WeckerApp(vm: WeckerViewModel, activity: ComponentActivity) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    // Eigene Achse neben Hell/Dunkel und Ausrichtung; lifecycle-bewusst gesammelt.
    val designId by vm.settings.designFlow.collectAsStateWithLifecycle()
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
    WeckerTheme(theme, design) {
        val gold = LocalGold.current
        BackHandler(page != "alarms") { back() }
        Box(Modifier.fillMaxSize().background(gold.hintergrund)) {
            LocalGestalt.current.Hintergrund(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize().imePadding()) {
                DesignKopfleiste(
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
                if (message.isNotBlank()) LocalGestalt.current.Flaeche(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), erhoeht = false) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        StillerKnopf("OK", { vm.message.value = "" })
                    }
                }
                // Der Bildschirmwechsel hatte bisher die Vorgabe-Überblendung: beide Seiten lagen
                // kurz übereinander und wuchsen dabei. Jetzt trägt die Richtung die Bedeutung —
                // tiefer hinein schiebt von rechts, zurück nach links — und beide Seiten bewegen
                // sich nur so weit, dass die Richtung erkennbar wird. Bei reduzierter Bewegung
                // wird hart umgeschaltet, ohne jede Verschiebung.
                val tiefe = { seite: String -> when (seite) { "alarms" -> 0; "edit" -> 1; else -> 2 } }
                // Außerhalb des Übergangs lesen: der transitionSpec-Block ist kein Composable.
                val wechselReduziert = LocalBewegungReduziert.current
                AnimatedContent(page, Modifier.weight(1f), label = "Bildschirmwechsel",
                    transitionSpec = {
                        if (wechselReduziert) {
                            (androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None)
                        } else {
                            val vorwaerts = tiefe(targetState) >= tiefe(initialState)
                            val weg = if (vorwaerts) 56 else -56
                            val dauer = Motion.ZUSTAND_MS
                            (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(dauer)) +
                                androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(dauer)) { weg })
                                .togetherWith(
                                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(dauer / 2)) +
                                        androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(dauer)) { -weg })
                        }
                    }) { current ->
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
private fun AlarmList(alarms: List<Alarm>, vm: WeckerViewModel, onNew: () -> Unit, onEdit: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit, onSettings: () -> Unit, openDraft: Alarm?, onResumeDraft: () -> Unit) {
    val gold = LocalGold.current
    // Nur für diese Listenansicht merken: neue Wecker und eine neu geöffnete Liste sind kompakt.
    // Saveable, damit das Auf- und Zuklappen des Geräts (Activity-Neuerstellung) den Stand nicht verwirft;
    // beim Verlassen der Liste wird der Eintrag verworfen, die Liste beginnt also weiterhin zugeklappt.
    var expandedIds by rememberSaveable(saver = androidx.compose.runtime.saveable.listSaver<MutableState<Set<String>>, String>(
        save = { it.value.toList() }, restore = { mutableStateOf(it.toSet()) })) { mutableStateOf(emptySet<String>()) }
    val issues by vm.store.issues.collectAsState()
    val now = rememberNow(60_000)
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
        val alarmIndex = alarms.indexOfFirst { it.id == event.id }
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
        val heroDaten = HeroDaten(
            now = now, next = next, nextIsSnooze = nextIsSnooze, nextAlarm = nextAlarm,
            nextName = nextName, bereit = bereitschaft.all { it.second },
            offen = bereitschaft.count { !it.second }, stufe = stufe, breite = breite,
        )
        Column(Modifier.fillMaxSize()) {
            // Der feststehende Hero. Er sitzt außerhalb des Rasters, damit die Weckerliste
            // darunter scrollt, ohne ihn mitzunehmen. `zIndex` hebt ihn über das Raster: Sonst
            // zeichnet die Liste als späteres Geschwister über seinen Schatten, und die Karten
            // würden auf ihm liegen statt unter ihm durchzugleiten.
            Box(
                Modifier.fillMaxWidth().zIndex(1f)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
            ) {
                when (LocalDesignTokens.current.design) {
                    Design.TRAUMRAUM -> TraumraumHero(heroDaten, onNew, onEdit, aufSchlummernBeenden)
                    Design.ORBIT -> OrbitHero(heroDaten, onNew, onEdit, onSettings, aufSchlummernBeenden)
                    Design.MORGENRUHE -> MorgenruheHero(heroDaten, onNew, onEdit, onSettings, aufSchlummernBeenden)
                    else -> SchlichtHero(heroDaten, onNew, onEdit, onSettings, aufSchlummernBeenden)
                }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(if (breite >= 680.dp && !achsenDesign) 2 else 1), state = gridState,
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
                    Leerzustand("☀", "Ein Morgen nach deinen Wünschen", "Musik, Gedanken und Erinnerungen – in deiner Reihenfolge. Lege deinen ersten Wecker an.")
                }
                items(alarms, key = { it.id }) { alarm ->
                    WeckerKarte(
                        alarm = alarm, vm = vm, now = now, hinweis = issues[alarm.id],
                        expanded = alarm.id in expandedIds,
                        aufKlappen = { expandedIds = if (alarm.id in expandedIds) expandedIds - alarm.id else expandedIds + alarm.id },
                        onEdit = onEdit, onDelete = onDelete,
                        glowAlpha = if (glowFor?.id == alarm.id) glow.value else 0f,
                        resumed = resumed, reducedMotion = reducedMotion,
                        modifier = Modifier.animateItem(),
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
) {
    val hatTermin: Boolean get() = next != null
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
) {
    val oeffenbar = daten.nextAlarm != null
    Column(
        Modifier.fillMaxWidth()
            .then(if (oeffenbar) Modifier.clickable(
                onClickLabel = "Nächsten Wecker öffnen", onClick = aufOeffnen) else Modifier),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        if (daten.next == null) {
            Text("Kein Wecker aktiv", style = MaterialTheme.typography.bodyMedium,
                color = textFarbe, maxLines = 1)
            Text("Lege einen an oder schalte einen ein", style = MaterialTheme.typography.titleSmall,
                color = gedaempft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // Hält die dritte Zeile frei, damit der Hero in jedem Zustand gleich hoch bleibt.
            Text(" ", Modifier.clearAndSetSemantics { },
                style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        } else {
            TerminZeile(daten.now, daten.next, daten.nextIsSnooze)
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
            daten.nextAlarm != null -> StillerKnopf("Öffnen", aufOeffnen)
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
    val ring = ringRoh.coerceIn(96.dp, if (daten.weit) 148.dp else 132.dp)
    val textBreite = if (zeigtRing) innen - SPALTEN_ABSTAND - (ring + RINGBETT_RAND) else innen
    HeroKarte {
        Column(Modifier.fillMaxWidth().padding(KARTE_INNEN), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SPALTEN_ABSTAND),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Der Ring liegt in einer leicht vertieften Mulde — dieselbe Sprache wie ein nicht
                // gedrückter Knopf, nur auf ein Instrument angewendet. Statisch, kein Dauerleuchten.
                if (zeigtRing) Box(
                    Modifier.size(ring + RINGBETT_RAND)
                        .background(gold.flaeche.dunkler(0.05f), CircleShape)
                        .border(1.dp, lichtKante(gedrueckt = true, staerke = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    RestzeitRing(daten.now, daten.next, daten.nextIsSnooze, Modifier.size(ring))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(datumsZeile(daten.now), Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium, color = gold.textGedaempft,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    GedeckelteSchrift {
                        // Die Größe wird gegen die tatsächlich verbleibende Spaltenbreite gemessen.
                        val groesse = passendeUhrGroesse(textBreite, uhrGroesse(daten.stufe),
                            zahlSchrift(), zahlGewicht())
                        Text(formatClock(daten.now), Modifier.semantics { heading() },
                            fontFamily = zahlSchrift(), fontWeight = zahlGewicht(),
                            fontSize = groesse, color = gold.primaer, maxLines = 1, softWrap = false)
                    }
                    TerminGruppe(daten, oeffnen, gold.textPrimaer, gold.textGedaempft, gold.primaer)
                }
            }
            // Über die volle Breite: Hier ist Platz für den beschrifteten Hauptknopf und die
            // zweitrangige Handlung, auch wenn deren Text länger ist.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    HeroAktionen(daten, aufNeu, oeffnen, aufSchlummernBeenden)
                }
                BereitZeile(daten, aufEinstellungen, semantisch.erfolg, semantisch.warnung)
            }
        }
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
 * Schlichts Heroträger: der gewohnte Goldkörper, aber deutlich erhoben und mit einem statischen
 * Glanzbogen im oberen Drittel — die Glassignatur, die dem alten Kopf fehlte. Bewusst ohne jede
 * Endlosbewegung: Der Hero scrollt nie weg und würde sonst dauerhaft Bilder kosten.
 */
@Composable
private fun HeroKarte(inhalt: @Composable () -> Unit) {
    val gold = LocalGold.current
    val form = RoundedCornerShape(LocalDesignTokens.current.karteRadius)
    val flaeche = gold.heroGrund
    Box(
        Modifier.fillMaxWidth()
            .tiefenSchatten(gold.primaer, Hoehe.schwebendeLeiste, form)
            .clip(form)
            .background(
                Brush.verticalGradient(
                    listOf(
                        flaeche.heller(if (gold.istDunkel) 0.08f else 0.03f),
                        flaeche,
                        flaeche.dunkler(if (gold.istDunkel) 0.12f else 0.05f),
                    ),
                ),
            )
            .glanzBogen(deckung = if (gold.istDunkel) 0.06f else 0.14f)
            .border(1.dp, lichtKante(staerke = if (gold.istDunkel) 0.16f else 0.55f), form),
    ) { inhalt() }
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
                    Text(formatClock(daten.now), fontFamily = zahlSchrift(), fontWeight = zahlGewicht(),
                        fontSize = groesse, color = gold.primaer, maxLines = 1, softWrap = false)
                }
                Column(
                    Modifier.weight(1f).then(
                        if (daten.nextAlarm != null) Modifier.clickable(
                            onClickLabel = "Nächsten Wecker öffnen", onClick = aufOeffnen) else Modifier),
                ) {
                    Text(
                        if (daten.next == null) "Kein Wecker aktiv"
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
    val semantisch = LocalSemantisch.current
    val oeffnen = { daten.nextAlarm?.let(aufOeffnen); Unit }
    if (daten.schmal) { SchmalerHero(daten, aufNeu, oeffnen, aufSchlummernBeenden); return }
    val form = RoundedCornerShape(LocalDesignTokens.current.karteRadius)
    // Wie bei Schlicht mit der echten Innenbreite gerechnet. Der Fehler war hier ein anderer:
    // Motiv und Aktionsknopf standen in derselben rechten Spalte, die damit so breit wurde wie
    // der Knopf — der Textspalte blieben bei 360 dp Gerätebreite nur 132 dp. Jetzt trägt die
    // rechte Spalte allein das Motiv, die Aktionen stehen darunter über die volle Breite.
    val innen = daten.breite - HERO_AUSSEN * 2 - KARTE_INNEN * 2
    val motiv = (innen - SPALTEN_ABSTAND - textMindest())
        .coerceIn(0.dp, if (daten.weit) 128.dp else 104.dp)
    val zeigtMotiv = motiv >= 72.dp
    val textBreite = innen - if (zeigtMotiv) SPALTEN_ABSTAND + motiv else 0.dp
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().clip(form).background(gold.heroGrund).border(1.dp, gold.heroKante, form)) {
            Column(Modifier.fillMaxWidth().padding(KARTE_INNEN), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SPALTEN_ABSTAND)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        // Das einzige Ornament des Designs: ein kurzer Messingstrich als Tagesmarke.
                        Box(Modifier.width(40.dp).height(2.dp).background(gold.akzentWarm))
                        Text(datumsZeile(daten.now), style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium, color = gold.heroFuehrung,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        GedeckelteSchrift {
                            val groesse = passendeUhrGroesse(textBreite, uhrGroesse(daten.stufe),
                                zahlSchrift(), zahlGewicht())
                            Text(formatClock(daten.now), Modifier.semantics { heading() },
                                fontFamily = zahlSchrift(), fontWeight = zahlGewicht(),
                                fontSize = groesse, color = gold.heroSchrift,
                                maxLines = 1, softWrap = false)
                        }
                        TerminGruppe(daten, oeffnen, gold.heroSchrift, gold.heroSchriftGedaempft, gold.heroFuehrung)
                    }
                    // Nur das Motiv steht rechts — und nur, wenn dafür wirklich Platz ist.
                    if (zeigtMotiv) LocalGestalt.current.Motiv(Modifier.size(motiv))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        HeroAktionen(daten, aufNeu, oeffnen, aufSchlummernBeenden, knopfText = "Wecker anlegen")
                    }
                    BereitZeile(daten, aufEinstellungen, semantisch.erfolg, semantisch.warnung)
                }
            }
        }
        // Der Anschluss an die Tagesachse: Der Punkt sitzt genau dort, wo `Station` in der Liste
        // ihre Linie zeichnet — 13 dp von der Spaltenkante. Ohne ihn schwebte die erste Station.
        Row(Modifier.fillMaxWidth().height(12.dp)) {
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
    daten: HeroDaten, aufNeu: () -> Unit, aufOeffnen: (Alarm) -> Unit, aufSchlummernBeenden: () -> Unit,
) {
    val gold = LocalGold.current
    val oeffnen = { daten.nextAlarm?.let(aufOeffnen); Unit }
    if (daten.schmal) { SchmalerHero(daten, aufNeu, oeffnen, aufSchlummernBeenden); return }
    val radius = LocalDesignTokens.current.karteRadius
    val kuppelForm = RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
    val motiv = if (daten.weit) 96.dp else 72.dp
    val versatz = 26.dp
    // Auch hier gegen die echte Restbreite gemessen: innen abzüglich Motiv und Abstand.
    val uhrPlatz = daten.breite - HERO_AUSSEN * 2 - 40.dp - motiv - 14.dp
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth()
                // Eine einzige farbige Umgebungsschicht statt eines Kontaktschattens: weicher
                // Schein von oben, kein plastischer Körper — das ist Traumraums Sprache.
                .shadow(16.dp, kuppelForm, ambientColor = gold.primaer.copy(alpha = .35f),
                    spotColor = androidx.compose.ui.graphics.Color.Transparent)
                .clip(kuppelForm)
                .background(Brush.verticalGradient(listOf(gold.heroGrund, gold.heroGrundUnten)))
                .border(1.dp, gold.heroKante, kuppelForm)
                .padding(top = 14.dp, bottom = versatz + 14.dp, start = 20.dp, end = 20.dp),
        ) {
            // Das Licht sitzt jetzt in der Kuppel und ist von ihr beschnitten.
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(datumsZeile(daten.now), style = MaterialTheme.typography.labelMedium,
                    color = gold.akzentWarm, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LocalGestalt.current.Motiv(Modifier.size(motiv))
                    GedeckelteSchrift {
                        val groesse = passendeUhrGroesse(uhrPlatz, uhrGroesse(daten.stufe),
                            zahlSchrift(), zahlGewicht())
                        Text(formatClock(daten.now), Modifier.semantics { heading() },
                            fontFamily = zahlSchrift(), fontWeight = zahlGewicht(),
                            fontSize = groesse, color = gold.heroFuehrung,
                            maxLines = 1, softWrap = false)
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
                        TerminGruppe(daten, oeffnen, gold.textPrimaer, gold.textGedaempft, gold.primaer)
                    }
                    GoldKnopf("＋", aufNeu, hauptKnopf = true, beschreibung = "Neuen Wecker anlegen")
                } else {
                    TerminGruppe(daten, oeffnen, gold.textPrimaer, gold.textGedaempft, gold.primaer)
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
    val motiv = (innen - 12.dp - textMindest()).coerceIn(0.dp, if (daten.weit) 108.dp else 92.dp)
    val zeigtMotiv = motiv >= 64.dp
    val uhrPlatz = innen - if (zeigtMotiv) motiv + 12.dp else 0.dp
    val statusFarbe = when {
        daten.nextIsSnooze -> semantisch.info
        daten.hatTermin -> gold.akzentWarm
        else -> gold.heroSchriftGedaempft
    }
    val statusText = when {
        daten.nextIsSnooze -> "SCHLUMMERT"
        daten.hatTermin -> "AKTIV"
        else -> "KEIN TERMIN"
    }
    Box(Modifier.fillMaxWidth().clip(form).background(gold.heroGrund).border(1.dp, gold.heroKante, form)) {
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
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (zeigtMotiv) LocalGestalt.current.Motiv(Modifier.size(motiv))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    GedeckelteSchrift {
                        val groesse = passendeUhrGroesse(uhrPlatz, uhrGroesse(daten.stufe),
                            IdeenSchriftFest, FontWeight.SemiBold)
                        Text(formatClock(daten.now), Modifier.semantics { heading() },
                            fontFamily = IdeenSchriftFest, fontWeight = FontWeight.SemiBold,
                            fontSize = groesse, color = gold.heroFuehrung,
                            maxLines = 1, softWrap = false)
                    }
                    RestzeitSkala(daten, gold.heroFuehrung, if (daten.nextIsSnooze) semantisch.info else gold.akzentWarm,
                        gold.heroKante)
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GoldKnopf("WECKER", aufNeu, hauptKnopf = true, beschreibung = "Neuen Wecker anlegen",
                        symbol = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) })
                    Spacer(Modifier.weight(1f))
                    BereitZeile(daten, aufEinstellungen, semantisch.erfolg, semantisch.warnung,
                        stil = MaterialTheme.typography.labelSmall.copy(fontFamily = IdeenSchriftFest))
                }
                if (daten.nextIsSnooze) Box(Modifier.fillMaxWidth()) {
                    StillerKnopf("SCHLUMMERN BEENDEN", aufSchlummernBeenden, hervorgehoben = true)
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
    ) {
        Text(name, Modifier.width(60.dp), fontFamily = IdeenSchriftFest,
            style = MaterialTheme.typography.labelSmall, color = gold.heroSchriftGedaempft, maxLines = 1)
        Text(wert, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
            color = wertFarbe, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
) {
    val anteil = daten.tagesAnteil()
    Canvas(Modifier.fillMaxWidth().height(4.dp).clearAndSetSemantics { }) {
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
    aufKlappen: () -> Unit, onEdit: (Alarm) -> Unit, onDelete: (Alarm) -> Unit,
    glowAlpha: Float, resumed: Boolean, reducedMotion: Boolean,
    // `animateItem` lebt im LazyItemScope; die Karte selbst kennt ihn nicht mehr, seit sie eine
    // eigene Composable ist. Der Aufrufer reicht den Modifier deshalb herein.
    modifier: Modifier = Modifier,
) {
    val gold = LocalGold.current
    val semantisch = LocalSemantisch.current
    val entwurf = LocalDesignTokens.current.design
    // Morgenruhe reiht die Wecker als Stationen an einer Tagesachse auf.
    val achse = entwurf == Design.MORGENRUHE
    // Orbit setzt zugeklappte Wecker als dichte Instrumentenzeile: knappes Innenmaß und enge
    // Abstände. Innerhalb von Orbit sind die Karten dadurch untereinander wieder gleich hoch.
    val dicht = entwurf == Design.ORBIT && !expanded

    val schlummerBis = alarm.snoozeUntil.takeIf { it > now }
    val schlafHinweis = if (alarm.enabled && alarm.nextAt > 0 && alarm.sleepMinutes > 0)
        Schlaf.hinweis(alarm.nextAt, alarm.sleepMinutes, now) else null

    Station(achse, alarm.enabled, modifier) {
        LocalGestalt.current.Flaeche(
            Modifier.fillMaxWidth()
                .then(if (glowAlpha > 0f) Modifier.border(2.dp, gold.primaer.copy(alpha = glowAlpha),
                    RoundedCornerShape(LocalDesignTokens.current.karteRadius)) else Modifier)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    enabled = !expanded, onClickLabel = "Wecker bearbeiten",
                    onClick = { onEdit(alarm) },
                ),
            erhoeht = false,
        ) {
            // No animateContentSize here: the detail block animates its own size, never two size animations at once.
            Column(
                Modifier.padding(horizontal = if (dicht) 10.dp else 18.dp, vertical = if (dicht) 8.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (dicht) 3.dp else 8.dp),
            ) {
                // --- Fach 1: Kopfzeile ---
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (dicht) 8.dp else 12.dp)) {
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
                        val timeColor by androidx.compose.animation.animateColorAsState(
                            if (alarm.enabled) gold.primaer else gold.textGedaempft,
                            if (reducedMotion) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(250),
                            label = "weckzeitFarbe")
                        WeckzeitUeberschrift(alarm, now, timeColor, platzHalten = !expanded)
                        // Row statt FlowRow: Eine FlowRow bricht je nach Länge von Name und Zeitplan
                        // mal auf eine, mal auf zwei Zeilen um — und genau dadurch wurden zugeklappte
                        // Orbit-Karten unterschiedlich hoch. Jetzt teilen sich beide fest eine Zeile,
                        // der Name bekommt den Vorrang und der Zeitplan seinen eigenen Mindestanteil.
                        if (dicht) Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(alarm.name, Modifier.weight(1f, fill = false),
                                style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(scheduleLabel(alarm), Modifier.weight(1f, fill = false), fontFamily = IdeenSchriftFest,
                                style = MaterialTheme.typography.labelSmall, color = gold.textGedaempft,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else Text(alarm.name, style = MaterialTheme.typography.titleMedium,
                            maxLines = if (expanded) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis)
                    }
                    KlappKnopf(expanded, aufKlappen,
                        beschreibung = "Weckerdetails ${if (expanded) "zuklappen" else "aufklappen"}: ${alarm.name}")
                }

                // Without RESUMED or with reduced motion the details switch instantly.
                val detailEnter = if (reducedMotion || !resumed) androidx.compose.animation.EnterTransition.None
                    else androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(200)) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200))
                val detailExit = if (reducedMotion || !resumed) androidx.compose.animation.ExitTransition.None
                    else androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(200)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))

                // --- Fach 2 und 3: Planzeile und Hinweisfach, beide nur im zugeklappten Zustand ---
                AnimatedVisibility(!expanded && !dicht, enter = detailEnter, exit = detailExit) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (alarm.enabled && alarm.nextAt > 0) "${scheduleLabel(alarm)} · ${terminAnzeige(now, alarm.nextAt).einzeilig}"
                            else "${scheduleLabel(alarm)} · ausgeschaltet",
                            color = gold.textGedaempft, style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        HinweisFach(schlummerBis = schlummerBis, warnung = hinweis, schlaf = schlafHinweis,
                            warnFarbe = semantisch.warnung, akzentFarbe = gold.primaer,
                            ruhigFarbe = gold.textPrimaer, leerFarbe = gold.textGedaempft)
                    }
                }
                // Orbit zeigt zugeklappt keine Planzeile (sie steht bereits in der Kopfzeile),
                // behält aber das Hinweisfach — ein Warnhinweis darf nirgends verschwinden.
                if (dicht) HinweisFach(schlummerBis = schlummerBis, warnung = hinweis, schlaf = schlafHinweis,
                    warnFarbe = semantisch.warnung, akzentFarbe = gold.primaer,
                    ruhigFarbe = gold.textPrimaer, leerFarbe = gold.textGedaempft)

                // --- Details ---
                AnimatedVisibility(expanded, enter = detailEnter, exit = detailExit) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(scheduleLabel(alarm), color = gold.textGedaempft, style = MaterialTheme.typography.bodySmall)
                    Text(alarm.steps.joinToString(" → ") { it.title }, color = gold.primaer, style = MaterialTheme.typography.bodySmall)
                    // Hier stehen alle Hinweise vollständig, auch die, die oben nur als Symbol geführt
                    // wurden — und hier liegt die Aktion zum Beenden des Schlummerns. Sie hatte früher
                    // eine eigene Zeile auf der zugeklappten Karte und machte damit genau den einen
                    // Wecker höher als alle anderen; gleiche geschlossene Höhen gehen vor.
                    schlummerBis?.let { bis -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Snooze, null, tint = gold.primaer, modifier = Modifier.size(20.dp))
                        Text("Schlummert bis ${formatClock(bis)}", Modifier.weight(1f).padding(horizontal = 8.dp),
                            color = gold.primaer)
                        StillerKnopf("Schlummern beenden", { vm.endSnooze(alarm) }, hervorgehoben = true)
                    } }
                    hinweis?.let { StatusZeile(Icons.Default.Warning, it, semantisch.warnung) }
                    schlafHinweis?.let { SchlafZeile(it) }
                    // Skip mark when present; entries without a mark keep the former nextAt comparison.
                    val skipped = AlarmTime.isSkipping(alarm, Instant.ofEpochMilli(now))
                    if (alarm.enabled && alarm.nextAt > 0) StatusZeile(Icons.Default.Alarm, "Nächster Termin: ${terminAnzeige(now, alarm.nextAt).einzeilig}", gold.textPrimaer)
                    if (skipped) StatusZeile(Icons.Default.SkipNext, "Ein Termin wird ausgelassen", gold.primaer)
                    Text("Lautstärke ${alarm.volume} %${if (alarm.photoRequired) " · Foto-Aufgabe" else ""}", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                    if (alarm.needsSpeech) {
                        val status = when {
                            alarm.preparationError.isNotBlank() -> "Vorbereitung offen: ${alarm.preparationError}"
                            alarm.preparedAt == 0L -> "Sprachausgabe noch nicht offline bereit"
                            else -> "${alarm.voiceVariants.size.takeIf { it > 0 } ?: 1} Stimmvarianten offline bereit · ${formatAt(alarm.preparedAt)}"
                        }
                        Text(status, style = MaterialTheme.typography.bodySmall,
                            color = if (alarm.preparationError.isNotBlank() || alarm.preparedAt == 0L) semantisch.warnung else semantisch.erfolg)
                    }
                    // Frequent actions first.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StillerKnopf("Bearbeiten", { onEdit(alarm) }, hervorgehoben = true)
                        StillerKnopf("Testwecken", { vm.test(alarm) })
                        if (alarm.enabled && alarm.repeats) {
                            if (skipped) StillerKnopf("Auslassen rückgängig", { vm.unskip(alarm) })
                            else StillerKnopf("Nächsten Termin auslassen", { vm.skip(alarm) })
                        }
                    }
                    // Rare actions second.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (alarm.needsSpeech) StillerKnopf("Audio vorbereiten", { vm.prepare(alarm) })
                        StillerKnopf("Duplizieren", { onEdit(alarm.copy(id = UUID.randomUUID().toString(), name = "${alarm.name} – Kopie", enabled = false, nextAt = 0, snoozeUntil = 0, snoozes = 0, skippedThrough = "")) })
                    }
                    // Delete separated by a divider and its own end-aligned row, without a fixed width.
                    HorizontalDivider(color = LocalGestalt.current.trennfarbe())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        StillerKnopf("Löschen", { onDelete(alarm) })
                    }
                } }
            }
        }
    }
}

/**
 * Das Hinweisfach einer zugeklappten Karte: genau eine Zeile, immer vorhanden.
 *
 * Es führt nach fester Rangfolge — laufendes Schlummern, dann Zuverlässigkeitswarnung, dann
 * Schlafhinweis. Was nicht führt, geht nicht verloren, sondern bleibt als Symbol am Zeilenende
 * stehen und steht vollständig in den Details; dort liegt auch die Aktion „Schlummern beenden".
 *
 * Genau eine Zeile ist der Punkt: Vorher hatte ein schlummernder Wecker eine eigene Zeile mit
 * Knopf und war dadurch als einziger höher als alle anderen. Jetzt ist jede zugeklappte Karte
 * desselben Designs bei gleicher Fenster- und Schriftkonfiguration gleich hoch. Die Mindesthöhe
 * entspricht dem Symbol; sie begrenzt nichts nach oben, große Systemschrift lässt die Zeile also
 * wachsen, statt Text abzuschneiden.
 */
@Composable
private fun HinweisFach(
    schlummerBis: Long?, warnung: String?, schlaf: String?,
    warnFarbe: androidx.compose.ui.graphics.Color,
    akzentFarbe: androidx.compose.ui.graphics.Color,
    ruhigFarbe: androidx.compose.ui.graphics.Color,
    leerFarbe: androidx.compose.ui.graphics.Color,
) {
    // Die Mindesthöhe entspricht dem Symbol: Ohne sie wäre eine Zeile mit Symbol rund 9 Pixel
    // höher als der reine Textplatzhalter — gemessen auf dem Gerät, bevor diese Zeile stand.
    Row(Modifier.fillMaxWidth().heightIn(min = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            schlummerBis != null -> {
                Icon(Icons.Default.Snooze, null, tint = akzentFarbe, modifier = Modifier.size(18.dp))
                Text("Schlummert bis ${formatClock(schlummerBis)}", Modifier.weight(1f).padding(start = 8.dp),
                    color = akzentFarbe, style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                NebenMarker(warnung != null, Icons.Default.Warning, "Außerdem ein Warnhinweis, Details aufklappen", warnFarbe)
                NebenMarker(schlaf != null, Icons.Default.Bedtime, "Außerdem ein Schlafhinweis, Details aufklappen", leerFarbe)
            }
            warnung != null -> {
                Icon(Icons.Default.Warning, null, tint = warnFarbe, modifier = Modifier.size(18.dp))
                Text(warnung, Modifier.weight(1f).padding(start = 8.dp), color = warnFarbe,
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                NebenMarker(schlaf != null, Icons.Default.Bedtime, "Außerdem ein Schlafhinweis, Details aufklappen", leerFarbe)
            }
            schlaf != null -> {
                Icon(Icons.Default.Bedtime, null, tint = leerFarbe, modifier = Modifier.size(18.dp))
                Text(schlaf, Modifier.weight(1f).padding(start = 8.dp), color = ruhigFarbe,
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // Platzhalter: ein geschütztes Leerzeichen belegt genau eine Zeile in derselben
            // Typografie und wächst deshalb mit der Systemschrift mit. Für TalkBack stumm.
            else -> Text(" ", Modifier.weight(1f).clearAndSetSemantics { },
                style = MaterialTheme.typography.bodySmall, maxLines = 1)
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
    val form = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)
    // Genau eine Aufrufstelle für Column und für den Inhalt: nur der Modifier und der Griff hängen
    // am Design. Ein Wechsel zu oder von Traumraum lässt damit die Zusammensetzung stehen, statt den
    // Unterbaum zu verwerfen — aufgeklappte Karten bleiben offen.
    // Das Blatt trug bisher nur Farbe und einen Strich. Damit blieben Traumraums Editor und
    // Einstellungen die einzigen großen Flächen der App ganz ohne Tiefe — ausgerechnet dort, wo
    // man sich am längsten aufhält. Jetzt bekommt es dieselbe Schichtung wie jede andere Fläche:
    // deckende Grundfarbe, Tiefenverlauf, gerichteter Reflex, Kante zuletzt.
    val material = LocalMaterial.current
    Column(
        Modifier.fillMaxWidth().then(
            if (traum) Modifier.padding(top = 10.dp)
                .clip(form)
                .background(gold.flaeche)
                .tiefenVerlauf(material.tiefenOben, material.tiefenUnten)
                .gerichteterReflex(material.reflexFarbe, material.reflexAlpha,
                    material.reflexWinkelGrad, material.reflexLaenge)
                .border(1.dp, materialKante(material.kanteLichtFarbe, material.kanteLichtAlpha,
                    material.kanteSchattenAlpha), form)
                .padding(horizontal = 4.dp, vertical = 10.dp)
            else Modifier,
        ),
    ) {
        if (traum) Box(Modifier.fillMaxWidth().padding(bottom = 14.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(46.dp).height(5.dp).clip(RoundedCornerShape(50)).background(gold.rahmen))
        }
        inhalt()
    }
}

@Composable
private fun TraumraumKopf(now: Long, terminBlock: @Composable ColumnScope.() -> Unit, onNew: () -> Unit,
    stufe: KopfStufe) {
    val gold = LocalGold.current
    // In der schmalsten Stufe wäre die Kuppel mit Motiv und überlappender Perle höher als der
    // verbleibende Listenraum. Dann tritt eine flache Fassung an ihre Stelle: gleiche Farben,
    // gleiche Rundung, gleicher Inhalt — nur ohne Motiv, ohne Überlappung und einzeilig.
    if (stufe == KopfStufe.SCHMAL) {
        LocalGestalt.current.Flaeche(Modifier.fillMaxWidth(), erhoeht = true) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(formatClock(now), fontFamily = zahlSchrift(), fontWeight = zahlGewicht(),
                    fontSize = uhrGroesse(stufe), color = gold.primaer)
                Column(Modifier.weight(1f)) { terminBlock() }
                GoldKnopf("＋", onNew, hauptKnopf = true, beschreibung = "Neuen Wecker anlegen")
            }
        }
        return
    }
    val weit = stufe == KopfStufe.WEIT
    // Ein Radius für das ganze Design statt vier: Die Kuppel hatte 46 dp, die Kopfleiste darüber
    // 28 dp, Karten 32 dp. Zwei konvexe Unterkanten mit verschiedenen Radien direkt übereinander
    // sahen unbeabsichtigt aus. Jetzt gilt überall der Kartenradius.
    val kuppelRadius = LocalDesignTokens.current.karteRadius
    val kuppelForm = RoundedCornerShape(bottomStart = kuppelRadius, bottomEnd = kuppelRadius)
    Column(Modifier.fillMaxWidth()) {
        // Der untere Innenabstand muss größer sein als die Überlappung — sonst stößt die Perle
        // ohne Luft an die Uhr. Bei gleichen Werten waren es exakt 0 dp Abstand.
        Box(Modifier.fillMaxWidth().clip(kuppelForm).background(gold.flaecheErhoeht)
            .padding(top = if (weit) 18.dp else 14.dp, bottom = if (weit) 46.dp else 40.dp, start = 20.dp, end = 20.dp)) {
            // Motiv und Uhr stehen nebeneinander, nicht übereinander. Übereinander summierten sich
            // Motivhöhe und Zeilenhöhe, und der feststehende Kopf belegte auf dem Gerät gemessene
            // 51 % der Bildschirmhöhe — für einen Block, der nie mehr wegscrollt, deutlich zu viel.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                LocalGestalt.current.Motiv(Modifier.size(if (weit) 88.dp else 64.dp))
                Spacer(Modifier.width(14.dp))
                Text(formatClock(now), fontFamily = zahlSchrift(), fontWeight = zahlGewicht(),
                    fontSize = uhrGroesse(stufe), color = gold.primaer)
            }
        }
        // Überlappend: die Perle sitzt optisch in der Kuppel. `offset` allein verschiebt nur die
        // Zeichnung und lässt unten genau so viel Luft stehen, wie es oben überlappt — gemessene
        // 73 Pixel tote Fläche zwischen Kopf und erster Karte. Deshalb wird die Überlappung hier
        // aus der belegten Höhe herausgerechnet, statt sie nur optisch zu verschieben.
        val versatz = 28.dp
        Column(
            Modifier.fillMaxWidth()
                .layout { messbar, grenzen ->
                    val platz = messbar.measure(grenzen)
                    val hub = versatz.roundToPx()
                    layout(platz.width, (platz.height - hub).coerceAtLeast(0)) { platz.place(0, -hub) }
                }
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Die Aktion sitzt neben der Perle statt in einer eigenen Reihe darunter. Die eigene
            // Reihe war der teuerste Teil des feststehenden Kopfes; jedes andere Design setzt die
            // Aktion ebenfalls in die Zeile.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { Perle { terminBlock() } }
                GoldKnopf("＋", onNew, hauptKnopf = true, beschreibung = "Neuen Wecker anlegen")
            }
        }
    }
}

/**
 * Orbit: das Kopfmodul einer Instrumententafel — das freigestellte Motiv links, der Datenblock
 * rechts, darunter die Aktionszeile. Gleiche Werte wie überall, nur in fester Schrift auf einer Achse.
 */
@Composable
private fun OrbitKopf(now: Long, next: Long?, nextIsSnooze: Boolean, ringSize: androidx.compose.ui.unit.Dp,
    terminBlock: @Composable ColumnScope.() -> Unit, onNew: () -> Unit,
    stufe: KopfStufe) {
    val gold = LocalGold.current
    val weit = stufe == KopfStufe.WEIT
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Die Statuszeile des Moduls gehört zur Instrumentenlogik und bleibt, solange Platz ist.
        // „GENIALER WECKER" stand hier früher ein zweites Mal — die Kopfleiste darüber zeigt
        // denselben Titel bereits. Übrig bleibt die Angabe, die nur dieses Modul liefert.
        if (stufe != KopfStufe.SCHMAL) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(if (next == null) "KEIN TERMIN" else "AKTIV", fontFamily = IdeenSchriftFest,
                style = MaterialTheme.typography.labelSmall, color = gold.akzentWarm)
        }
        LocalGestalt.current.Flaeche(Modifier.fillMaxWidth(), erhoeht = false) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = if (weit) 14.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // Orbit zeigt an dieser Stelle sein freigestelltes Motiv statt des Zwölfstundenrings;
                // Uhrzeit und der nächste Termin daneben bleiben unverändert.
                if (stufe != KopfStufe.SCHMAL) LocalGestalt.current.Motiv(Modifier.size(ringSize))
                Column(Modifier.weight(1f)) {
                    Text(formatClock(now), fontFamily = IdeenSchriftFest, fontWeight = FontWeight.SemiBold,
                        fontSize = uhrGroesse(stufe), color = gold.primaer)
                    if (weit) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = gold.rahmen)
                    terminBlock()
                }
                // Eng wandert die Hauptaktion in die Modulzeile, statt eine eigene Reihe zu belegen.
                if (!weit) GoldKnopf("＋", onNew, hauptKnopf = true, beschreibung = "Neuen Wecker anlegen")
            }
        }
        // Nur noch die eine Hauptaktion. „EINSTELLUNGEN" stand hier doppelt: Das Zahnrad in der
        // Kopfleiste führt an dieselbe Stelle. Eine Reihe mit einem Knopf braucht keine FlowRow.
        if (weit) GoldKnopf("＋ WECKER", onNew, hauptKnopf = true)
    }
}

/**
 * Traumraum fasst Termin, Name und Restzeit in eine eigene runde Fläche — die „Perle" des Entwurfs.
 * Alle anderen Designs geben denselben Inhalt unverändert weiter.
 */
@Composable
private fun Perle(inhalt: @Composable ColumnScope.() -> Unit) {
    val gold = LocalGold.current
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(gold.flaecheErhoeht)
        .border(1.dp, gold.rahmen, RoundedCornerShape(28.dp)).padding(16.dp)) {
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
fun Section(title: String, collapsible: Boolean = false, summary: String = "", error: String? = null,
    initiallyExpanded: Boolean = !collapsible, content: @Composable ColumnScope.() -> Unit) {
    // Only visibility is remembered here; all values live in the draft in the ViewModel.
    // Der Zustand liegt genau einmal hier, unabhängig vom Design; die Designs ordnen nur anders an.
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded || !collapsible) }
    val gold = LocalGold.current
    val entwurf = LocalDesignTokens.current.design

    // Gemeinsame Bausteine — in jedem Design derselbe Inhalt und dieselben Rückrufe.
    val kopfModifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .then(if (collapsible) Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button,
            onClickLabel = if (expanded) "$title zuklappen" else "$title aufklappen") { expanded = !expanded }
            .semantics { stateDescription = if (expanded) "Aufgeklappt" else "Zugeklappt" } else Modifier)
    val beschriftung = @Composable { titelStil: androidx.compose.ui.text.TextStyle, titelFarbe: androidx.compose.ui.graphics.Color ->
        Column(Modifier.fillMaxWidth()) {
            Text(title, style = titelStil, color = titelFarbe)
            if (!expanded && summary.isNotBlank()) Text(summary, style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
            // A missing required input is shown in its own card, also while collapsed.
            if (error != null) Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = LocalSemantisch.current.warnung, modifier = Modifier.size(18.dp))
                Text(error, Modifier.padding(start = 6.dp), color = LocalSemantisch.current.warnung, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    when (entwurf) {
        // Morgenruhe: der Gruppentitel steht ruhig über der flachen Karte, nicht in ihr.
        Design.MORGENRUHE -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(kopfModifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { beschriftung(MaterialTheme.typography.titleSmall, gold.textGedaempft) }
                if (collapsible) KlappKnopf(expanded, { expanded = !expanded }, beschreibung = null, modifier = Modifier.padding(start = 8.dp))
            }
            if (expanded) LocalGestalt.current.Flaeche(Modifier.fillMaxWidth(), erhoeht = false) {
                Column(Modifier.padding(18.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
            }
        }
        // Orbit: technische Modulkopfzeile mit fester Schrift und Trennlinie im kantigen Modul.
        Design.ORBIT -> LocalGestalt.current.Flaeche(Modifier.fillMaxWidth(), erhoeht = false) {
            Column(Modifier.animateContentSize()) {
                Row(kopfModifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        beschriftung(MaterialTheme.typography.labelLarge.copy(fontFamily = IdeenSchriftFest, letterSpacing = 1.5.sp), gold.primaer)
                    }
                    if (collapsible) KlappKnopf(expanded, { expanded = !expanded }, beschreibung = null, modifier = Modifier.padding(start = 8.dp))
                }
                if (expanded) {
                    HorizontalDivider(color = gold.rahmen)
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
                }
            }
        }
        // Traumraum: leichte Abschnittstrennung innerhalb des bereits vorhandenen großen Blatts.
        // Traumraum behält seine offene Gliederung auf dem Blatt — keine Karte in der Karte.
        // Statt einer bloßen Trennlinie bekommt der Abschnitt aber eine leicht abgesetzte
        // Vertiefung, sobald er offen ist: Dadurch liegt der Inhalt sichtbar *im* Blatt.
        Design.TRAUMRAUM -> {
            val material = LocalMaterial.current
            val abschnittForm = RoundedCornerShape(LocalDesignTokens.current.karteRadius)
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp).animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(kopfModifier, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        beschriftung(MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), gold.textPrimaer)
                    }
                    if (collapsible) KlappKnopf(expanded, { expanded = !expanded }, beschreibung = null, modifier = Modifier.padding(start = 8.dp))
                }
                if (expanded) Box(
                    Modifier.fillMaxWidth()
                        .clip(abschnittForm)
                        .background(gold.flaecheErhoeht)
                        .innenSchatten(abschnittForm, material.innenSchattenAlpha * 0.6f, tiefe = 4.dp)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
                }
                HorizontalDivider(color = gold.rahmen.copy(alpha = .6f))
            }
        }
        // Schlicht: unverändert der bisherige Aufbau aus Kopfzeile und Inhalt in einer Karte.
        else -> LocalGestalt.current.Flaeche(Modifier.fillMaxWidth(), erhoeht = false) {
            Column(Modifier.padding(18.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(kopfModifier, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { beschriftung(MaterialTheme.typography.titleMedium, gold.primaer) }
                    // The header row already announces this action; the button itself stays silent for TalkBack.
                    if (collapsible) KlappKnopf(expanded, { expanded = !expanded }, beschreibung = null, modifier = Modifier.padding(start = 8.dp))
                }
                if (expanded) content()
            }
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding()) {
      DesignBlatt {
       Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Derived from this draft only, so it disappears once corrected and never carries over to another draft.
        val minute = rememberNow(60_000)
        OffenesDiktatKarte(vm, alarm)
        if (remember(alarm, minute) { alarm.isExpiredOnce() }) LocalGestalt.current.Flaeche(Modifier.fillMaxWidth(), erhoeht = false) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EventBusy, null, tint = LocalSemantisch.current.warnung)
                Text("Dieses Datum liegt in der Vergangenheit. Wähle einen neuen Termin – beim Speichern wird der Wecker eingeschaltet.",
                    Modifier.padding(start = 12.dp), color = LocalSemantisch.current.warnung, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Section("Deine Weckzeit", collapsible = true, initiallyExpanded = true,
            summary = listOfNotNull(alarm.timeLabel, alarm.name.ifBlank { null },
                if (alarm.sleepMinutes > 0) "Schlafdauer ${Schlaf.dauer(alarm.sleepMinutes)}" else null).joinToString(" · ")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Früher öffnete hier der Plattformdialog `android.app.TimePickerDialog`. Der zieht sein
                // Aussehen aus `styles.xml` (Theme.Material.Light) und erschien deshalb in jedem Design
                // und auch im Dunkelmodus als weißer Systemdialog — ausgerechnet bei der wichtigsten
                // Handlung der App. Jetzt kommt die Wahl aus der eigenen Farbwelt.
                // Saveable und an die Wecker-Kennung gebunden: Dreht sich das Gerät oder klappt das
                // Foldable auf, wird die Activity neu erstellt. Mit `remember` verschwand der Dialog
                // dabei samt angefangener Auswahl — der innere Picker-Zustand nützt nichts, wenn
                // schon das Öffnen vergessen ist. Der Schlüssel verhindert, dass der Dialog beim
                // Wechsel auf einen anderen Wecker offen bleibt.
                var zeitWahlOffen by rememberSaveable(alarm.id) { mutableStateOf(false) }
                if (zeitWahlOffen) ZeitWahlDialog(
                    stunde = alarm.hour, minute = alarm.minute,
                    aufAbbruch = { zeitWahlOffen = false },
                    aufWahl = { stunde, minute -> zeitWahlOffen = false; vm.change(alarm.copy(hour = stunde, minute = minute)) },
                )
                val pickTime = { zeitWahlOffen = true }
                Text(alarm.timeLabel, Modifier.weight(1f).clickable(onClickLabel = "Uhrzeit ändern", onClick = pickTime), fontSize = 52.sp, fontFamily = zahlSchrift(), fontWeight = zahlGewicht(), color = LocalGold.current.primaer)
                GoldKnopf("Uhrzeit ändern", pickTime)
            }
            Eingabefeld(alarm.name, { vm.change(alarm.copy(name = it)) }, "Name des Weckers", Modifier.fillMaxWidth())
            SchlafdauerEingabe(alarm, vm::change)
        }
        // Kept outside the section: its content is removed while collapsed, the remembered date must survive that.
        var gemerktesDatum by rememberSaveable(alarm.id) { mutableStateOf(alarm.startDate) }
        Section("Wiederholung", collapsible = true, summary = scheduleLabel(alarm)) {
            RepeatEditor(alarm, activity, gemerktesDatum, { gemerktesDatum = it }, vm::change)
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
    val available = voices.map {
        "${TtsProvider.QWEN_CLONE.id}|${it.id}" to "${vm.settings.qwenVoiceNames[it.id] ?: it.name} · Meine Stimmen"
    } + TtsCatalog.googleVoices.map {
        "${TtsProvider.GOOGLE_CLOUD.id}|${it.id}" to "${it.name} · Google · ${geschlecht(it)}"
    } + TtsCatalog.edgeVoices.map {
        "${TtsProvider.EDGE.id}|${it.id}" to "${it.name} · Edge · ${geschlecht(it)}"
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
    StillerKnopf(if (expanded) "⌃" else "⌄", onToggle, modifier.minimumInteractiveComponentSize().then(
        if (beschreibung == null) Modifier.clearAndSetSemantics {}
        else Modifier.semantics { contentDescription = beschreibung; stateDescription = if (expanded) "Aufgeklappt" else "Zugeklappt" }))
}

@Composable
fun Toggle(label: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = change),
        verticalAlignment = Alignment.CenterVertically) {
        // Der eigene Schalter statt des Material-Schalters: Seine Bahn liegt vertieft, der Knauf
        // erhaben. Die Rolle sitzt bereits am umschließenden `toggleable`, deshalb hier ohne
        // eigenen Rückruf — sonst meldete TalkBack zwei Schalter.
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
        val eintraege: LazyListScope.() -> Unit = {
            items(gezeigt, key = { it.first }) { option ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(option.first == selected, role = androidx.compose.ui.semantics.Role.RadioButton) { choose(option.first); open = false }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
fun scheduleLabel(alarm: Alarm): String = when {
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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEachIndexed { index, name ->
                val day = index + 1
                // Removing the last day keeps the weekday mode instead of silently turning into a one-off alarm.
                FilterChip(day in alarm.days, { if (day !in alarm.days || alarm.days.size > 1) change(alarm.copy(days = if (day in alarm.days) alarm.days - day else alarm.days + day)) },
                    { Text(name) }, Modifier.semantics { contentDescription = names[index] })
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
    if (alarm.startDate.isNotBlank()) {
        // Kalendertage, damit die Zeitumstellung die Entfernung nicht verfälscht; als Long, weil der Abstand
        // beliebig groß sein darf und erst nach der Bereichsprüfung in den Schieberegler passt.
        val ahead = java.time.temporal.ChronoUnit.DAYS.between(today, java.time.LocalDate.parse(alarm.startDate))
        val tage = { anzahl: Long -> if (anzahl == 1L) "1 Tag" else "$anzahl Tage" }
        Text(if (mode == "once") "Am ${dateLabel(alarm.startDate)} um ${alarm.timeLabel}" else "Startdatum: ${dateLabel(alarm.startDate)}")
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
        // Auch hier stand bisher der helle Plattformdialog. Der Ersatz übernimmt die Semantik
        // unverändert: Er schreibt erst beim Bestätigen, und nur das Startdatum.
        // Ebenfalls saveable, aus demselben Grund wie bei der Uhrzeitwahl.
        var datumWahlOffen by rememberSaveable(alarm.id) { mutableStateOf(false) }
        if (datumWahlOffen) DatumWahlDialog(
            datum = runCatching { java.time.LocalDate.parse(alarm.startDate) }.getOrDefault(today),
            fruehestes = null,
            aufAbbruch = { datumWahlOffen = false },
            aufWahl = { gewaehlt -> datumWahlOffen = false; change(alarm.copy(startDate = gewaehlt.toString())) },
        )
        StillerKnopf("Datum im Kalender wählen", { datumWahlOffen = true })
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
    val anzeige = remember(alarm.enabled, alarm.nextAt, alarm.timeLabel, now) {
        if (alarm.enabled && alarm.nextAt > 0) terminAnzeige(now, alarm.nextAt) else null
    }
    // Orbit setzt Zeit und Termin als dichte Zeile auf eine Achse; die anderen Designs stellen die
    // Uhrzeit groß heraus und das Datum darüber.
    if (LocalDesignTokens.current.design == Design.ORBIT) {
        // Dieselbe Reihenfolge wie in jedem Design: ab übermorgen steht das Datum vorn, heute und
        // morgen die Uhrzeit. Orbit setzt es nur kompakter und in fester Schrift.
        // Row statt FlowRow, aus demselben Grund wie in der Kartenzeile: Der Umbruch hing von der
        // Textlänge ab und machte Karten unterschiedlich hoch. Alle drei Teile bleiben auf einer
        // Zeile; wird es eng, kürzt der Zusatz zuerst, die Uhrzeit nie.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val datumText = anzeige?.datum
            if (datumText != null) {
                Text(datumText, Modifier.weight(1f, fill = false), fontFamily = IdeenSchriftFest,
                    style = MaterialTheme.typography.labelMedium, color = farbe,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else if (platzHalten) {
                Text(" ", Modifier.clearAndSetSemantics { }, fontFamily = IdeenSchriftFest,
                    style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            Text(anzeige?.uhrzeit ?: alarm.timeLabel, fontFamily = zahlSchrift(), fontWeight = zahlGewicht(),
                fontSize = 30.sp, color = farbe, maxLines = 1)
            anzeige?.zusatz?.let { zusatz ->
                Text(zusatz, Modifier.weight(1f, fill = false), fontFamily = IdeenSchriftFest,
                    style = MaterialTheme.typography.labelMedium, color = farbe,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        return
    }
    val datumText = anzeige?.datum
    if (datumText != null) {
        Text(datumText, style = MaterialTheme.typography.titleMedium, color = farbe,
            maxLines = if (platzHalten) 1 else 2, overflow = TextOverflow.Ellipsis)
    } else if (platzHalten) {
        // Unsichtbarer Platzhalter derselben Typografie: eine Zeile, die mit der Systemschrift
        // mitwächst. Ohne ihn wären Karten mit Datum rund 74 Pixel höher als die ohne.
        Text(" ", Modifier.clearAndSetSemantics { }, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
    // Auch hier eine feste Zeile: Uhrzeit und Zusatz („morgen") dürfen nicht je nach Namenslänge
    // mal neben-, mal untereinander stehen. Der Zusatz sitzt auf der Grundlinie der großen Zahl.
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        Text(anzeige?.uhrzeit ?: alarm.timeLabel, fontFamily = zahlSchrift(), fontWeight = zahlGewicht(),
            fontSize = 44.sp, color = farbe, maxLines = 1)
        anzeige?.zusatz?.let { zusatz ->
            Text(zusatz, Modifier.weight(1f, fill = false).padding(bottom = 8.dp),
                style = MaterialTheme.typography.titleMedium, color = farbe,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The alarm term with the same filled marker as on the ring; the weekday and date are added when it is not today. */
@Composable
private fun TerminZeile(now: Long, target: Long, snooze: Boolean) {
    val gold = LocalGold.current
    val accent = if (snooze) LocalSemantisch.current.info else gold.primaer
    val targetText = terminAnzeige(now, target).einzeilig
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LegendenMarker(hollow = false, color = accent)
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
private fun RestzeitRing(now: Long, target: Long?, snooze: Boolean, modifier: Modifier = Modifier) {
    val gold = LocalGold.current
    val accent = if (snooze) LocalSemantisch.current.info else gold.primaer
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
    Text(if (sleep == 0) "Ohne Angabe wird keine Schlafenszeit angezeigt." else "Zeigt die ungefähre Schlafenszeit. Die Erinnerung kommt mit dem in den Einstellungen gewählten Vorlauf, wenn sie dort eingeschaltet ist. Kein Tracking.",
        style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
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
    val allowsAlarms = Build.VERSION.SDK_INT < 28 || (policy != null && (policy.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS) != 0)
    return listOf("Genaue Weckzeiten" to AlarmScheduler(context).allowed(),
        "Benachrichtigungen" to manager.areNotificationsEnabled(),
        "Vollbild-Wecker" to (Build.VERSION.SDK_INT < 34 || manager.canUseFullScreenIntent()),
        "Wecker bei Nicht stören" to (manager.isNotificationPolicyAccessGranted && allowsAlarms && manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_NONE),
        // Samsung puts optimized apps to sleep; unrestricted battery keeps restore and preparation alive.
        "Akku uneingeschränkt" to context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName))
}

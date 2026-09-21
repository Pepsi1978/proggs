package de.frank.wecker

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.genialeideen.data.settings.SecureSettings
import de.frank.genialeideen.ui.*
import de.frank.genialeideen.ui.theme.*
import de.frank.wecker.design.Design
import de.frank.wecker.design.LocalGestalt
import de.frank.wecker.design.WeckerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.animation.togetherWith

/** A tapped action waiting for the service's confirmation. [afterSeq] ignores results that existed before the tap. */
private data class PendingAction(val id: String, val ringId: Long, val action: String, val afterSeq: Long)

class AlarmActivity : ComponentActivity() {
    /** Erst nach der ersten Entsperrung geöffnet; davor gibt es keine verschlüsselten Einstellungen. */
    private var settings: SecureSettings? = null

    /**
     * Die aktuelle Wahl, jedes Mal frisch gelesen. Ist der verschlüsselte Speicher nicht zu öffnen, gilt
     * dunkel – nicht der Vorgabewert „light": ein weckender Bildschirm mitten in der Nacht darf nicht
     * wegen eines Lesefehlers hell aufblenden. Eine ausdrücklich gewählte helle Oberfläche bleibt hell.
     */
    private fun leseTheme(): String {
        if (!getSystemService(android.os.UserManager::class.java).isUserUnlocked) return "dark"
        val offen = settings ?: runCatching { SecureSettings(this) }.getOrNull()
        if (offen == null) {
            android.util.Log.w("WeckerAlarmUi", "Einstellungen nicht zu öffnen; Weckbildschirm bleibt dunkel")
            return "dark"
        }
        if (!offen.verfuegbar) {
            // Nicht behalten: eine einmal leer gebliebene Instanz hielte den Fehlschlag dauerhaft fest und
            // kein späteres Resume käme je an die echte Wahl. Verwerfen, beim nächsten Mal neu versuchen.
            runCatching { offen.close() }
            settings = null
            android.util.Log.w("WeckerAlarmUi", "Verschlüsselte Einstellungen nicht lesbar; Weckbildschirm bleibt dunkel")
            return "dark"
        }
        settings = offen
        return runCatching { offen.theme }.getOrDefault("dark")
    }

    /** Rückfall ist Schlicht — die gewohnte Optik, wenn die Einstellungen nicht lesbar sind. */
    private fun leseDesign(): Design {
        val offen = settings ?: return Design.SCHLICHT
        return runCatching { if (offen.verfuegbar) Design.von(offen.design) else Design.SCHLICHT }
            .getOrDefault(Design.SCHLICHT)
    }

    /** Hier ist „automatisch" der richtige Rückfall: er entspricht dem Verhalten ohne Einstellung. */
    private fun leseAusrichtung(): String {
        val offen = settings ?: return Ausrichtung.AUTOMATISCH
        return runCatching { if (offen.verfuegbar) offen.ausrichtung else Ausrichtung.AUTOMATISCH }
            .getOrDefault(Ausrichtung.AUTOMATISCH)
    }

    override fun onDestroy() {
        // Der eigene Wrapper meldet seinen Listener wieder ab; die Instanz des ViewModels bleibt unberührt.
        runCatching { settings?.close() }
        settings = null
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val start = leseTheme()
        // Ausdrückliche, durchsichtige Ränder: der Vorgabewert setzt auf älteren Versionen einen hellen
        // Scrim hinter die Navigationsleiste, der nicht zur gewählten Oberfläche passen muss.
        val rand = if (start == "dark") androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = rand, navigationBarStyle = rand)
        hideStatusBar()
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            val state by AlarmService.state.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            var photoPath by rememberSaveable { mutableStateOf("") }
            var photoRingId by rememberSaveable { mutableLongStateOf(0L) }
            var message by rememberSaveable { mutableStateOf("") }
            /** The ring whose photo is being checked; an old check can never change the state of a newer ring. */
            var checkingRing by remember { mutableStateOf<Long?>(null) }
            /** Ring a local message belongs to; it is shown only for that ring. */
            var messageRing by rememberSaveable { mutableLongStateOf(0L) }
            var seenAlarm by rememberSaveable { mutableStateOf(false) }
            var pending by remember { mutableStateOf<PendingAction?>(null) }
            var feedback by remember { mutableStateOf<RingResult?>(null) }
            var shownAlarm by remember { mutableStateOf<Alarm?>(null) }
            // Wiggle triggers carry their ring, so a new ring never replays an old failure.
            var snoozeWiggle by remember { mutableStateOf<Pair<Long, Int>?>(null) }
            var endWiggle by remember { mutableStateOf<Pair<Long, Int>?>(null) }
            fun say(text: String, ring: Long) { message = text; messageRing = ring }

            /** Sends immediately; success is shown only once the service confirms this exact action. */
            fun send(action: String, id: String, ring: Long, commandName: String = action) {
                if (pending != null || feedback != null) return
                pending = PendingAction(id, ring, action, AlarmService.result.value?.seq ?: 0)
                command(commandName, id, ring)
            }
            val photo = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                val path = photoPath
                val ring = photoRingId
                if (success && path.isNotBlank()) scope.launch {
                    checkingRing = ring
                    // True only while the ring the photo was taken for is still the live one.
                    fun stillSameRing() = AlarmService.state.value.let { it.alarm != null && it.ringId == ring }
                    try {
                        val alarm = AlarmService.state.value.alarm
                        // A photo taken for an earlier ring is discarded, never applied to the current one.
                        if (alarm == null || !stillSameRing()) { say("Das Foto gehört zu einem früheren Klingeln und wurde verworfen.", AlarmService.state.value.ringId); return@launch }
                        val result = withContext(Dispatchers.IO) { PhotoCheck.check(File(path), alarm) }
                        // Re-check after the slow check: a new ring must never see or receive this old result.
                        if (!stillSameRing()) return@launch
                        say(result.message, ring)
                        // The photo success is confirmed only by the service after PHOTO_OK.
                        if (result.accepted) send("STOP", alarm.id, ring, "PHOTO_OK")
                    } catch (e: Exception) { if (stillSameRing()) say(e.message ?: "Das Foto konnte nicht geprüft werden.", ring) }
                    // Release only this check's own ring.
                    finally { if (checkingRing == ring) checkingRing = null; File(path).delete() }
                } else say("Fotoaufnahme abgebrochen. Der Wecker läuft weiter.", ring)
            }
            LaunchedEffect(Unit) {
                AlarmService.result.collect { result ->
                    val waiting = pending ?: return@collect
                    if (result == null || result.seq <= waiting.afterSeq || result.id != waiting.id || result.ringId != waiting.ringId || result.action != waiting.action) return@collect
                    pending = null
                    if (result.ok) feedback = result
                    else if (result.action == "SNOOZE") snoozeWiggle = result.ringId to ((snoozeWiggle?.takeIf { it.first == result.ringId }?.second ?: 0) + 1)
                    else endWiggle = result.ringId to ((endWiggle?.takeIf { it.first == result.ringId }?.second ?: 0) + 1)
                }
            }
            // A confirmation that never arrives must not lock the buttons.
            LaunchedEffect(pending) { if (pending != null) { delay(4000); pending = null } }
            var lastRing by rememberSaveable { mutableLongStateOf(0L) }
            LaunchedEffect(state.alarm, state.ringId) {
                val alarm = state.alarm ?: return@LaunchedEffect
                seenAlarm = true
                shownAlarm = alarm
                if (state.ringId != lastRing) {
                    // A new ring starts clean, even for the same alarm id: no feedback, waiting action, wiggle or message carries over.
                    lastRing = state.ringId
                    feedback = null; pending = null
                    snoozeWiggle = null; endWiggle = null
                    message = ""
                }
            }
            LaunchedEffect(feedback, state.ringId, state.alarm == null, pending) {
                val done = feedback
                if (done != null) {
                    delay(350)
                    val now = AlarmService.state.value
                    if (now.alarm == null || now.ringId == done.ringId) finish()
                } else if (state.alarm == null && seenAlarm && pending == null) finish()
            }
            // Kein Flow, sondern ein ausdrückliches neues Lesen bei jedem ON_RESUME: diese Activity ist
            // singleTask und wird für ein weiteres Klingeln über onNewIntent wiederverwendet, eine einmalige
            // Momentaufnahme bekäme eine zwischenzeitliche Änderung nie mit. Ein eigener Wrapper hat eigene
            // Listenerlisten und erhielte Änderungen aus dem ViewModel nicht zuverlässig als Ereignis.
            var theme by remember { mutableStateOf(leseTheme()) }
            // Die gewählte Ausrichtung gilt auch hier; Android kann sie auf großen Displays übergehen.
            var ausrichtung by remember { mutableStateOf(leseAusrichtung()) }
            var design by remember { mutableStateOf(leseDesign()) }
            val darstellung = LocalLifecycleOwner.current.lifecycle
            DisposableEffect(darstellung) {
                val beobachter = androidx.lifecycle.LifecycleEventObserver { _, ereignis ->
                    if (ereignis == Lifecycle.Event.ON_RESUME) { theme = leseTheme(); ausrichtung = leseAusrichtung(); design = leseDesign() }
                }
                darstellung.addObserver(beobachter)
                onDispose { darstellung.removeObserver(beobachter) }
            }
            val konfiguration = androidx.compose.ui.platform.LocalConfiguration.current
            LaunchedEffect(ausrichtung, konfiguration.smallestScreenWidthDp) { Ausrichtung.anwenden(this@AlarmActivity, ausrichtung) }
            val wirksam = wirksamesTheme(theme)
            SideEffect {
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = wirksam != "dark"
                    isAppearanceLightNavigationBars = wirksam != "dark"
                }
            }
            WeckerTheme(wirksam, design, ausrichtung) {
                val gold = LocalGold.current
                val alarm = state.alarm ?: shownAlarm.takeIf { feedback != null }
                // The ring whose UI is shown; the confirmed ring stays displayed while the service shuts down.
                val displayRing = if (state.alarm != null) state.ringId else feedback?.ringId ?: 0L
                // Everything shown is filtered by ring, so stale state is never visible, not even for one frame after a new ring.
                val shownFeedback = feedback?.takeIf { it.ringId == displayRing }
                val shownMessage = message.takeIf { messageRing == displayRing }.orEmpty()
                val checking = checkingRing != null && checkingRing == displayRing
                BackHandler {
                    if (state.test) command("TEST_END", state.alarm?.id, state.ringId)
                    else say("Der Wecker läuft weiter. Nutze Schlummern oder erfülle die Stopp-Aufgabe.", state.ringId)
                }
                val onEnd: () -> Unit = end@{
                    val current = alarm ?: return@end
                    if (pending != null || feedback != null) return@end
                    if (current.photoRequired) {
                        val file = newPhoto(this@AlarmActivity)
                        photoPath = file.absolutePath; photoRingId = state.ringId
                        photo.launch(FileProvider.getUriForFile(this@AlarmActivity, "$packageName.photos", file))
                    } else send("STOP", current.id, state.ringId)
                }
                Box(Modifier.fillMaxSize().background(gold.hintergrund)) {
                    LocalGestalt.current.Hintergrund(Modifier.fillMaxSize())
                    // Keyed by ring: animation state (scale, fade, ring exit) can never pass over to a new ring.
                    key(displayRing) {
                        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
                            // Low landscape keeps the horizontal bottom bar instead of an overfilled side column.
                            val wide = maxWidth >= 600.dp && maxWidth > maxHeight && maxHeight >= 480.dp
                            val screenHeight = maxHeight
                            val content = @Composable { modifier: Modifier ->
                                BoxWithConstraints(modifier) {
                                    val contentWidth = maxWidth - 48.dp
                                    val contentHeight = maxHeight
                                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        // Nur der dekorative Kopf unterscheidet sich; alles darunter ist in jedem
                                        // Design derselbe Code. Die Uhr zeigt immer die echte aktuelle Zeit.
                                        WeckKopf(alarm, contentWidth, contentHeight, shownFeedback != null)
                                        if (alarm != null) WeckSchritte(alarm, state.step, state.variation)
                                        if (shownFeedback == null && state.message.isNotBlank()) Text(state.message, color = LocalSemantisch.current.warnung)
                                        if (shownFeedback == null && shownMessage.isNotBlank()) Text(shownMessage)
                                        if (alarm != null && alarm.photoRequired && shownFeedback == null) {
                                            Section("Deine Foto-Aufgabe") {
                                                if (alarm.reference.isNotBlank()) Box(Modifier.heightIn(max = minOf(240.dp, contentHeight * 0.3f))) { PhotoPreview(File(alarm.reference)) }
                                                if (alarm.minBrightness > 0) Text("Mindestens ${alarm.minBrightness} % Helligkeit")
                                                if (alarm.color != "none") Text("${PhotoCheck.colors[alarm.color]}: mindestens ${alarm.colorPercent} % des Bildes")
                                                Text("Beenden öffnet die Kamera. Der Wecker stoppt erst, wenn das Foto passt.", style = MaterialTheme.typography.bodySmall, color = gold.textGedaempft)
                                            }
                                        }
                                    }
                                }
                            }
                            val actions = @Composable { modifier: Modifier, vertical: Boolean ->
                                Column(modifier.background(gold.flaeche.copy(alpha = .9f)).drawBehind {
                                    val line = 1.dp.toPx()
                                    if (vertical) drawLine(gold.primaer.copy(alpha = .5f), Offset(0f, 0f), Offset(0f, size.height), line)
                                    else drawLine(gold.primaer.copy(alpha = .5f), Offset(0f, 0f), Offset(size.width, 0f), line)
                                }.padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp, if (vertical) Alignment.CenterVertically else Alignment.Top)) {
                                    ErgebnisZeile(shownFeedback)
                                    // Identical for test and real alarms; the photo task can never be bypassed here.
                                    if (alarm != null) WeckTasten(alarm, checking, shownFeedback?.action, snoozeWiggle?.takeIf { it.first == displayRing },
                                        endWiggle?.takeIf { it.first == displayRing }, vertical, screenHeight,
                                        onSnooze = { send("SNOOZE", alarm.id, state.ringId) }, onEnd = onEnd)
                                    else StillerKnopf("Zurück zur App", { finish() })
                                    // The only visible difference between a test and a real alarm.
                                    if (state.test) Text("(Test)", style = MaterialTheme.typography.labelSmall, color = gold.textGedaempft)
                                }
                            }
                            if (wide) Row(Modifier.fillMaxSize()) {
                                content(Modifier.weight(1f).fillMaxHeight())
                                actions(Modifier.width(260.dp).fillMaxHeight(), true)
                            } else Column(Modifier.fillMaxSize()) {
                                content(Modifier.weight(1f).fillMaxWidth())
                                actions(Modifier.fillMaxWidth(), false)
                            }
                        }
                    }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        // Opened from the fallback notification: a visible activity may start the alarm service again.
        if (AlarmService.state.value.alarm == null && AlarmStore.get(this).ringing().isNotEmpty()) runCatching {
            androidx.core.content.ContextCompat.startForegroundService(this, Intent(this, AlarmService::class.java).setAction("RING"))
        }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusBar()
    }
    private fun command(action: String, id: String?, ring: Long) {
        startService(Intent(this, AlarmService::class.java).setAction(action).putExtra("id", id).putExtra("ring", ring))
    }
}

/** Kleinste Größe, die für die große Uhr überhaupt noch versucht wird. */
private const val KLEINSTE_UHR = 8f

/**
 * Ein Textstil für die große Uhr. Zurückgegeben wird **nur eine Größe, die gemessen in [maxBreite]
 * gepasst hat** — ein Abbruch der Suche gilt ausdrücklich nicht als Treffer.
 *
 * Gemessen und gezeichnet wird **derselbe** Stil: er entsteht aus dem geerbten [LocalTextStyle] und
 * bekommt Schrift, Gewicht und Farbe aufgesetzt. Damit misst niemand mit anderen Werten, als später
 * auf dem Bildschirm stehen — auch Zeichenabstand und sonstige geerbte Merkmale gehen ein.
 *
 * Die breiteste Ziffer wird ermittelt statt behauptet: alle zehn werden einmal gemessen, die
 * breiteste bildet das Muster „XX:XX". Passt schon [hoechstens], bleibt es dabei. Sonst wird
 * zwischen einer **nachweislich passenden** Unterseite und der zu großen Oberseite halbiert; das
 * Ergebnis ist immer die zuletzt bestätigte Unterseite.
 *
 * Dokumentierter Sonderfall: Ist die Breite unbekannt oder so klein, dass selbst [KLEINSTE_UHR]
 * nicht hineinpasst, wird diese Größe zurückgegeben — **ohne** Zusicherung, dass sie passt. Dann ist
 * schlicht kein Platz vorhanden; die Uhr bricht wegen `softWrap = false` trotzdem nicht um.
 *
 * Gerechnet wird in einem [remember], dessen Schlüssel nur an Breite, Höchstgröße, Stil und Dichte
 * hängen — nicht an der Uhrzeit. Keine Schleife über Frames, kein zusätzlicher Zustand.
 */
@Composable
private fun uhrStil(
    maxBreite: androidx.compose.ui.unit.Dp,
    hoechstens: Float,
    familie: androidx.compose.ui.text.font.FontFamily,
    gewicht: androidx.compose.ui.text.font.FontWeight?,
    farbe: androidx.compose.ui.graphics.Color,
): androidx.compose.ui.text.TextStyle {
    val messer = androidx.compose.ui.text.rememberTextMeasurer()
    val dichte = androidx.compose.ui.platform.LocalDensity.current
    val basis = LocalTextStyle.current.merge(
        androidx.compose.ui.text.TextStyle(fontFamily = familie, fontWeight = gewicht, color = farbe),
    )
    return remember(messer, maxBreite, hoechstens, basis, dichte.density, dichte.fontScale) {
        val grenze = with(dichte) { maxBreite.toPx() }
        // Sonderfall ohne bekannte Breite: nichts zu messen, es gilt die Höchstgröße.
        if (grenze <= 0f) return@remember basis.copy(fontSize = hoechstens.sp)
        fun breiteVon(text: String, sp: Float) =
            messer.measure(text, basis.copy(fontSize = sp.sp)).size.width.toFloat()
        val breiteste = (0..9).maxByOrNull { breiteVon(it.toString(), hoechstens) } ?: 0
        val muster = "$breiteste$breiteste:$breiteste$breiteste"
        // Reicht der Platz schon für die Höchstgröße, bleibt die gewohnte Darstellung unverändert.
        if (breiteVon(muster, hoechstens) <= grenze) return@remember basis.copy(fontSize = hoechstens.sp)
        // Sonderfall zu wenig Platz: auch die kleinste Größe passt nicht. Sie wird ausdrücklich
        // ohne Zusicherung zurückgegeben — ein Abbruch der Suche gilt nicht als Treffer.
        if (breiteVon(muster, KLEINSTE_UHR) > grenze) return@remember basis.copy(fontSize = KLEINSTE_UHR.sp)
        // Begrenzte Halbierung zwischen bestätigter Unterseite und zu großer Oberseite.
        var passt = KLEINSTE_UHR
        var zuGross = hoechstens
        repeat(8) {
            val mitte = (passt + zuGross) / 2f
            if (breiteVon(muster, mitte) <= grenze) passt = mitte else zuGross = mitte
        }
        // `passt` wurde in jedem Fall gemessen und hat gepasst.
        basis.copy(fontSize = passt.sp)
    }
}

/**
 * Der dekorative Kopf des Weckbildschirms je Design — und nur er. Schritte, Meldungen, Foto-Aufgabe,
 * Tasten und die gesamte Zustandslogik liegen außerhalb und sind in allen Designs identisch.
 *
 * Schlicht behält exakt den bisherigen Aufbau aus Gruß, Pulsring mit Uhr und Namen. Traumraum setzt
 * eine geschwungene Kuppel mit kräftiger zentrierter Uhr, Morgenruhe eine ruhige Gruppe ohne Ring,
 * Orbit eine Instrumentenleiste mit fester Schrift.
 */
@Composable
private fun WeckKopf(alarm: Alarm?, contentWidth: androidx.compose.ui.unit.Dp,
    contentHeight: androidx.compose.ui.unit.Dp, verlaesst: Boolean) {
    val gold = LocalGold.current
    val entwurf = de.frank.wecker.design.LocalDesignTokens.current.design
    // Show the live clock, not the alarm time, so the user always sees the current time.
    val now = rememberNow()
    val name = alarm?.name ?: "Der Wecker wird geöffnet …"
    when (entwurf) {
        de.frank.wecker.design.Design.TRAUMRAUM -> {
            val form = androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp)
            Column(Modifier.fillMaxWidth().clip(form).background(gold.flaecheErhoeht).padding(vertical = 30.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GUTEN MORGEN", color = gold.primaer, letterSpacing = 3.sp, style = MaterialTheme.typography.labelMedium)
                // Genau der Stil, mit dem gemessen wurde — dadurch passt die Uhrzeit nachweislich.
                Text(formatClock(now), maxLines = 1, softWrap = false,
                    style = uhrStil(contentWidth - 40.dp, 88f, zahlSchrift(),
                        androidx.compose.ui.text.font.FontWeight.Bold, gold.primaer))
                Text(name, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        de.frank.wecker.design.Design.MORGENRUHE -> {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("GUTEN MORGEN", color = gold.primaer, letterSpacing = 3.sp, style = MaterialTheme.typography.labelMedium)
                Text(formatClock(now), maxLines = 1, softWrap = false,
                    style = uhrStil(contentWidth, 76f, zahlSchrift(),
                        androidx.compose.ui.text.font.FontWeight.Light, gold.primaer))
                Text(name, style = MaterialTheme.typography.headlineSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        de.frank.wecker.design.Design.ORBIT -> {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("WECKER AKTIV", fontFamily = IdeenSchriftFest, color = gold.akzentWarm,
                        style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
                    Text(alarm?.timeLabel.orEmpty(), fontFamily = IdeenSchriftFest, color = gold.textGedaempft,
                        style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
                }
                HorizontalDivider(color = gold.rahmen)
                Text(formatClock(now), maxLines = 1, softWrap = false,
                    style = uhrStil(contentWidth, 80f, zahlSchrift(),
                        androidx.compose.ui.text.font.FontWeight.SemiBold, gold.primaer))
                Text(name, style = MaterialTheme.typography.titleLarge)
            }
        }
        else -> {
            Text("GUTEN MORGEN", color = gold.primaer, letterSpacing = 3.sp)
            // Raw size first: below 140 dp the ring is left out instead of being clamped up.
            val factor = if (alarm?.photoRequired == true) 0.30f else 0.45f
            val raw = minOf(contentWidth * 0.8f, contentHeight * factor)
            val ring = if (raw < 140.dp) null else raw.coerceAtMost(300.dp)
            val clockSize = ring?.let { (it.value * 0.24f).coerceIn(44f, 76f) } ?: 56f
            if (ring != null) Box(Modifier.size(ring), contentAlignment = Alignment.Center) {
                WeckPuls(ringing = alarm != null, leaving = verlaesst, modifier = Modifier.matchParentSize())
                Text(formatClock(now), fontFamily = zahlSchrift(), fontSize = clockSize.sp, color = gold.primaer)
            } else Text(formatClock(now), fontFamily = zahlSchrift(), fontSize = clockSize.sp, color = gold.primaer)
            Text(name, style = MaterialTheme.typography.headlineMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/**
 * Ruhiger Pulsring mit langsam wanderndem Lichtbogen hinter der Uhrzeit. Bewegt sich nur, solange der
 * Bildschirm sichtbar und fortgesetzt ist; bei ausgeschalteten Systemanimationen bleibt alles statisch.
 */
@Composable
private fun WeckPuls(ringing: Boolean, leaving: Boolean, modifier: Modifier = Modifier) {
    val gold = LocalGold.current.primaer
    val reduced = LocalBewegungReduziert.current
    val animate = ringing && !leaving && !reduced && rememberResumed()
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val offset = elapsed
        val start = withFrameMillis { it }
        while (true) withFrameMillis { elapsed = offset + it - start }
    }
    val exit by animateFloatAsState(if (leaving) 1f else 0f, weich(300), label = "ringAusblenden")
    Canvas(modifier) {
        val t = elapsed
        val fade = 1f - exit
        val base = size.minDimension / 2f * 0.8f
        withTransform({ scale(1f - 0.15f * exit, 1f - 0.15f * exit, center) }) {
            val breathe = if (animate) (kotlin.math.sin(t / 4500.0 * 2 * Math.PI).toFloat() + 1f) / 2f else .5f
            drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = (.14f + .10f * breathe) * fade), Color.Transparent), center, base * 1.25f), base * 1.25f)
            drawCircle(gold.copy(alpha = .30f * fade), base, style = Stroke(2.dp.toPx()))
            if (animate) {
                val p = (t % 4500L) / 4500f
                drawCircle(gold.copy(alpha = .45f * (1f - p) * fade), base * (0.92f + 0.18f * p), style = Stroke(3.dp.toPx()))
            }
            val start = if (animate) (t % 16000L) / 16000f * 360f else -90f
            val segments = 14
            val topLeft = Offset(center.x - base, center.y - base)
            repeat(segments) { i ->
                drawArc(gold.copy(alpha = (i + 1f) / segments * .9f * fade), start + i * 5f, 5.5f, useCenter = false,
                    topLeft = topLeft, size = Size(base * 2, base * 2),
                    style = Stroke(5.dp.toPx(), cap = if (i == segments - 1) StrokeCap.Round else StrokeCap.Butt))
            }
        }
    }
}

/** Animation spec that is instant when system animations are off or the screen is not resumed. */
@Composable
private fun <T> weich(durationMs: Int): androidx.compose.animation.core.FiniteAnimationSpec<T> =
    if (LocalBewegungReduziert.current || !rememberResumed()) snap() else tween(durationMs)

/** Die Schritte dieses Weckers als Chips; der gerade laufende ist hervorgehoben und wechselt weich. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun WeckSchritte(alarm: Alarm, step: String, variation: Int) {
    val gold = LocalGold.current
    // Schlicht behält exakt die bisherige 50-%-Pille; nur die anderen Designs setzen ihre Kante.
    val tokens = de.frank.wecker.design.LocalDesignTokens.current
    val shape = if (tokens.design == de.frank.wecker.design.Design.SCHLICHT)
        androidx.compose.foundation.shape.RoundedCornerShape(50)
    else androidx.compose.foundation.shape.RoundedCornerShape(tokens.chipRadius)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        alarm.steps.forEach { item ->
            val active = item.title == step
            val fill by androidx.compose.animation.animateColorAsState(if (active) gold.primaer.copy(alpha = .22f) else Color.Transparent, weich(250), label = "schrittFlaeche")
            val edge by androidx.compose.animation.animateColorAsState(if (active) gold.primaer else gold.textGedaempft.copy(alpha = .5f), weich(250), label = "schrittRand")
            Box(Modifier.clip(shape).background(fill).border(1.dp, edge, shape).padding(horizontal = 12.dp, vertical = 6.dp)
                .semantics { selected = active }) {
                Text(item.title, color = if (active) gold.textPrimaer else gold.textGedaempft, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    // Steps outside the alarm's own list (start, emergency tone) stay readable as text.
    if (step.isNotBlank() && alarm.steps.none { it.title == step }) Text(step, color = gold.textGedaempft)
    if (alarm.voiceVariants.isNotEmpty()) {
        val spec = weich<Float>(200)
        androidx.compose.animation.AnimatedContent(variation, transitionSpec = {
            // No SizeTransform: with snap fades there must be no size spring either.
            (androidx.compose.animation.fadeIn(spec) togetherWith androidx.compose.animation.fadeOut(spec)).using(null)
        }, label = "stimmvariante") { value ->
            Text("Stimmvariante $value von ${alarm.voiceVariants.size}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Runde Alarmtaste: Traumraum bleibt kreisrund, Orbit und Morgenruhe setzen ihre eigene Kante.
 * Mindestmaße, Haptik und Verhalten sind in allen Designs gleich.
 */
@Composable
private fun tasteRundForm(): androidx.compose.ui.graphics.Shape {
    val tokens = de.frank.wecker.design.LocalDesignTokens.current
    // Schlicht und Traumraum bleiben kreisrund; die kantigen Designs setzen ihren Radius.
    return if (tokens.design == de.frank.wecker.design.Design.SCHLICHT || tokens.tasteRadius >= 100.dp)
        androidx.compose.foundation.shape.CircleShape
    else androidx.compose.foundation.shape.RoundedCornerShape(tokens.tasteRadius)
}

/**
 * Fortschritt 0..1 einer einmaligen Bestätigungsanimation. Läuft nur bei RESUMED und aktiven Systemanimationen;
 * geht RESUMED verloren, springt sie auf den Endzustand und wird nach der Wiederaufnahme nicht erneut abgespielt.
 * Der Zustand lebt innerhalb von key(displayRing) und beginnt daher für jedes Klingeln neu.
 */
@Composable
private fun einmalFortschritt(active: Boolean, durationMs: Int): Float {
    val animate = !LocalBewegungReduziert.current && rememberResumed()
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(active, animate) {
        when {
            !active -> { played = false; progress.snapTo(0f) }
            animate && !played -> {
                played = true
                progress.snapTo(0f)
                progress.animateTo(1f, tween(durationMs, easing = androidx.compose.animation.core.LinearEasing))
            }
            else -> { played = true; progress.snapTo(1f) }
        }
    }
    return progress.value
}

/** Gleitet erst nach der Bestätigung des Dienstes ein; ohne Animation (reduziert oder nicht RESUMED) steht sie sofort. */
@Composable
private fun ErgebnisZeile(result: RingResult?) {
    val progress = einmalFortschritt(result != null, 250)
    val done = result ?: return
    Text(if (done.action == "SNOOZE") "Schlummert bis ${formatClock(done.snoozeUntil)}" else "Wecker beendet",
        Modifier.graphicsLayer { alpha = progress; translationY = (1f - progress) * 24.dp.toPx() },
        style = MaterialTheme.typography.titleLarge, color = LocalGold.current.primaer,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}

/**
 * Zwei beschriftete Aktionen, ohne Scrollen erreichbar. Runde Tasten ab 96 dp; darunter kompakte rechteckige
 * Tasten mit umbrechender Beschriftung, bei wenig Breite untereinander.
 */
@Composable
private fun WeckTasten(alarm: Alarm, checking: Boolean, confirmed: String?, snoozeWiggle: Any?, endWiggle: Any?,
    vertical: Boolean, screenHeight: androidx.compose.ui.unit.Dp, onSnooze: () -> Unit, onEnd: () -> Unit) {
    val gold = LocalGold.current
    val snoozeLeft = (alarm.snoozeLimit - alarm.snoozes).coerceAtLeast(0)
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val gap = if (screenHeight < 480.dp) 12.dp else 24.dp
        val byHeight = if (vertical) (screenHeight - gap - 160.dp) / 2 else screenHeight * 0.22f
        val byWidth = if (vertical) maxWidth else (maxWidth - gap) / 2
        val entwurf = de.frank.wecker.design.LocalDesignTokens.current.design
        val diameter = minOf(176.dp, byHeight, byWidth)
        // Jedes Design ordnet anders an, ohne die Mindestmaße zu unterschreiten:
        // Morgenruhe stapelt mit dem Beenden-Knopf oben, Orbit stellt zwei gleich große
        // Module nebeneinander, Traumraum setzt zwei Kreise. Schlicht bleibt beim Platzverhalten.
        val round = when (entwurf) {
            de.frank.wecker.design.Design.MORGENRUHE -> false
            de.frank.wecker.design.Design.TRAUMRAUM -> diameter >= 96.dp
            de.frank.wecker.design.Design.ORBIT -> false
            else -> diameter >= 96.dp
        }
        val stacked = when (entwurf) {
            de.frank.wecker.design.Design.MORGENRUHE -> true
            de.frank.wecker.design.Design.ORBIT -> (maxWidth - gap) / 2 < 140.dp
            else -> vertical || (!round && (maxWidth - gap) / 2 < 140.dp)
        }
        val snoozeDone = confirmed == "SNOOZE"
        val endDone = confirmed == "STOP"
        val snooze = @Composable { modifier: Modifier ->
            Taste(round, diameter, if (snoozeDone) Icons.Default.Check else Icons.Default.Snooze, if (snoozeDone) "Schlummert" else "Schlummern",
                LocalSemantisch.current.info, enabled = snoozeLeft > 0 || snoozeDone, main = false, confirmed = snoozeDone, faded = confirmed != null && !snoozeDone,
                info = if (snoozeLeft > 0) "${alarm.snoozeMinutes} Min. · noch $snoozeLeft" else "Keine Schlummerpause mehr",
                infoColor = if (snoozeLeft > 0) gold.textGedaempft else LocalSemantisch.current.warnung, onClick = onSnooze,
                modifier = modifier.wackelnBeiFehler(snoozeWiggle))
        }
        val end = @Composable { modifier: Modifier ->
            Taste(round, diameter, when { endDone -> Icons.Default.Check; alarm.photoRequired -> Icons.Default.PhotoCamera; else -> Icons.Default.AlarmOff },
                when { endDone -> "Beendet"; checking -> "Prüfe …"; else -> "Beenden" }, gold.primaer, enabled = !checking || endDone, main = true,
                confirmed = endDone, faded = confirmed != null && !endDone,
                info = if (alarm.photoRequired) "Mit Foto-Aufgabe" else "", infoColor = gold.textGedaempft, onClick = onEnd,
                modifier = modifier.wackelnBeiFehler(endWiggle))
        }
        when {
            round && !stacked -> Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Top) { snooze(Modifier); end(Modifier) }
            round -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) { end(Modifier); snooze(Modifier) }
            !stacked -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Top) {
                snooze(Modifier.weight(1f)); end(Modifier.weight(1f))
            }
            else -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) { end(Modifier.fillMaxWidth()); snooze(Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun Taste(round: Boolean, diameter: androidx.compose.ui.unit.Dp, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String,
    color: Color, enabled: Boolean, main: Boolean, confirmed: Boolean, faded: Boolean, info: String,
    infoColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val onColor = if (color.luminance() > 0.5f) Color(0xFF1B1B1B) else Color.White
    // Only after the service confirmed: a short pop on the confirmed button, the other one fades out.
    val pop = einmalFortschritt(confirmed, 250)
    val fade = einmalFortschritt(faded, 250)
    val scale = 1f + .08f * kotlin.math.sin(Math.PI * pop).toFloat()
    val spec = weich<Float>(150)
    val content = @Composable {
        androidx.compose.animation.AnimatedContent(icon to label, transitionSpec = {
            (androidx.compose.animation.fadeIn(spec) togetherWith androidx.compose.animation.fadeOut(spec)).using(null)
        }, label = "tasteInhalt") { (shownIcon, shownLabel) ->
            if (round) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(shownIcon, contentDescription = null, tint = onColor, modifier = Modifier.size(diameter * 0.3f))
                Text(shownLabel, color = onColor, fontSize = if (diameter < 110.dp) 13.sp else 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 1, softWrap = false)
            } else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(shownIcon, contentDescription = null, tint = onColor, modifier = Modifier.size(24.dp))
                Text(shownLabel, color = onColor, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
    Column((if (round) modifier.width(diameter) else modifier).graphicsLayer { scaleX = scale; scaleY = scale; alpha = 1f - .65f * fade },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Knopf3D gives the haptic pulse; a faded button is really disabled, so TalkBack and haptics match what can be used.
        Knopf3D(onClick, if (round) Modifier.size(diameter) else Modifier.fillMaxWidth().heightIn(min = 56.dp),
            grundfarbe = color, form = if (round) tasteRundForm() else androidx.compose.foundation.shape.RoundedCornerShape(
                if (de.frank.wecker.design.LocalDesignTokens.current.design == de.frank.wecker.design.Design.SCHLICHT) 16.dp
                else de.frank.wecker.design.LocalDesignTokens.current.tasteRadius),
            aktiviert = enabled && !faded, hauptKnopf = main, innenAbstandWaagerecht = if (round) 8.dp else 12.dp,
            innenAbstandSenkrecht = 8.dp, beschreibung = label) {
            if (round) {
                // Circles hold at most 1.2× font scale (>= 96 dp keeps "Schlummern" whole); rectangles wrap instead.
                val density = androidx.compose.ui.platform.LocalDensity.current
                CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                    androidx.compose.ui.unit.Density(density.density, density.fontScale.coerceAtMost(1.2f))) { content() }
            } else content()
        }
        if (info.isNotBlank()) Text(info, color = infoColor, style = if (round) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

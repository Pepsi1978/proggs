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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            // Im Direct Boot stehen verschlüsselte Einstellungen noch nicht zur Verfügung.
            val theme = remember {
                if (getSystemService(android.os.UserManager::class.java).isUserUnlocked)
                    SecureSettings(this).use { it.theme } else "dark"
            }
            SideEffect {
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = theme != "dark"
                    isAppearanceLightNavigationBars = theme != "dark"
                }
            }
            GenialeIdeenTheme(theme) {
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
                    SichtbarerHintergrund()
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
                                        Text("GUTEN MORGEN", color = gold.primaer, letterSpacing = 3.sp)
                                        // Raw size first: below 140 dp the ring is left out instead of being clamped up.
                                        val factor = if (alarm?.photoRequired == true) 0.30f else 0.45f
                                        val raw = minOf(contentWidth * 0.8f, contentHeight * factor)
                                        val ring = if (raw < 140.dp) null else raw.coerceAtMost(300.dp)
                                        // Show the live clock, not the alarm time, so the user always sees the current time.
                                        val now = rememberNow()
                                        val clockSize = ring?.let { (it.value * 0.24f).coerceIn(44f, 76f) } ?: 56f
                                        if (ring != null) Box(Modifier.size(ring), contentAlignment = Alignment.Center) {
                                            WeckPuls(ringing = alarm != null, leaving = shownFeedback != null, modifier = Modifier.matchParentSize())
                                            Text(formatClock(now), fontFamily = IdeenSchriftBetont, fontSize = clockSize.sp, color = gold.primaer)
                                        } else Text(formatClock(now), fontFamily = IdeenSchriftBetont, fontSize = clockSize.sp, color = gold.primaer)
                                        Text(alarm?.name ?: "Der Wecker wird geöffnet …", style = MaterialTheme.typography.headlineMedium,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        if (alarm != null) WeckSchritte(alarm, state.step, state.variation)
                                        if (shownFeedback == null && state.message.isNotBlank()) Text(state.message, color = Semantisch.warnung)
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
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
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
        val diameter = minOf(176.dp, byHeight, byWidth)
        val round = diameter >= 96.dp
        val stacked = vertical || (!round && (maxWidth - gap) / 2 < 140.dp)
        val snoozeDone = confirmed == "SNOOZE"
        val endDone = confirmed == "STOP"
        val snooze = @Composable { modifier: Modifier ->
            Taste(round, diameter, if (snoozeDone) Icons.Default.Check else Icons.Default.Snooze, if (snoozeDone) "Schlummert" else "Schlummern",
                Semantisch.info, enabled = snoozeLeft > 0 || snoozeDone, main = false, confirmed = snoozeDone, faded = confirmed != null && !snoozeDone,
                info = if (snoozeLeft > 0) "${alarm.snoozeMinutes} Min. · noch $snoozeLeft" else "Keine Schlummerpause mehr",
                infoColor = if (snoozeLeft > 0) gold.textGedaempft else Semantisch.warnung, onClick = onSnooze,
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
            grundfarbe = color, form = if (round) androidx.compose.foundation.shape.CircleShape else androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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

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
            var checking by remember { mutableStateOf(false) }
            var seenAlarm by rememberSaveable { mutableStateOf(false) }
            var pending by remember { mutableStateOf<PendingAction?>(null) }
            var feedback by remember { mutableStateOf<RingResult?>(null) }
            var shownAlarm by remember { mutableStateOf<Alarm?>(null) }
            var snoozeWiggle by remember { mutableStateOf<Int?>(null) }
            var endWiggle by remember { mutableStateOf<Int?>(null) }

            /** Sends immediately; success is shown only once the service confirms this exact action. */
            fun send(action: String, id: String, ring: Long, commandName: String = action) {
                if (pending != null || feedback != null) return
                pending = PendingAction(id, ring, action, AlarmService.result.value?.seq ?: 0)
                command(commandName, id, ring)
            }
            val photo = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success && photoPath.isNotBlank()) scope.launch {
                    checking = true
                    try {
                        val ringing = AlarmService.state.value
                        val alarm = ringing.alarm ?: return@launch
                        // A photo taken for an earlier ring is discarded, never applied to the current one.
                        if (ringing.ringId != photoRingId) { message = "Das Foto gehört zu einem früheren Klingeln und wurde verworfen."; return@launch }
                        val result = withContext(Dispatchers.IO) { PhotoCheck.check(File(photoPath), alarm) }
                        message = result.message
                        // The photo success is confirmed only by the service after PHOTO_OK.
                        if (result.accepted) send("STOP", alarm.id, photoRingId, "PHOTO_OK")
                    } catch (e: Exception) { message = e.message ?: "Das Foto konnte nicht geprüft werden." }
                    finally { checking = false; File(photoPath).delete() }
                } else message = "Fotoaufnahme abgebrochen. Der Wecker läuft weiter."
            }
            LaunchedEffect(Unit) {
                AlarmService.result.collect { result ->
                    val waiting = pending ?: return@collect
                    if (result == null || result.seq <= waiting.afterSeq || result.id != waiting.id || result.ringId != waiting.ringId || result.action != waiting.action) return@collect
                    pending = null
                    if (result.ok) feedback = result
                    else if (result.action == "SNOOZE") snoozeWiggle = (snoozeWiggle ?: 0) + 1
                    else endWiggle = (endWiggle ?: 0) + 1
                }
            }
            // A confirmation that never arrives must not lock the buttons.
            LaunchedEffect(pending) { if (pending != null) { delay(4000); pending = null } }
            LaunchedEffect(state.alarm, state.ringId) {
                val alarm = state.alarm ?: return@LaunchedEffect
                seenAlarm = true
                shownAlarm = alarm
                // A new ring has priority, even for the same alarm id: drop feedback or waiting actions of another ring.
                if (feedback != null && feedback?.ringId != state.ringId) feedback = null
                if (pending != null && pending?.ringId != state.ringId) pending = null
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
                val alarm = state.alarm ?: shownAlarm.takeIf { feedback != null }
                BackHandler {
                    if (state.test) command("TEST_END", state.alarm?.id, state.ringId)
                    else message = "Der Wecker läuft weiter. Nutze Schlummern oder erfülle die Stopp-Aufgabe."
                }
                Box(Modifier.fillMaxSize().background(LocalGold.current.hintergrund)) {
                    SichtbarerHintergrund()
                    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Spacer(Modifier.height(8.dp))
                        Text("GUTEN MORGEN", color = LocalGold.current.primaer, letterSpacing = 3.sp)
                        // Show the live clock, not the alarm time, so the user always sees the current time.
                        val now = rememberNow()
                        Box(Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                            WeckPuls(ringing = alarm != null, leaving = feedback != null, modifier = Modifier.matchParentSize())
                            Text(formatClock(now), fontFamily = IdeenSchriftBetont, fontSize = 76.sp, color = LocalGold.current.primaer)
                        }
                        Text(alarm?.name ?: "Der Wecker wird geöffnet …", style = MaterialTheme.typography.headlineMedium)
                        feedback?.let { done ->
                            Text(if (done.action == "SNOOZE") "Schlummert bis ${formatClock(done.snoozeUntil)}" else "Wecker beendet",
                                style = MaterialTheme.typography.titleLarge, color = LocalGold.current.primaer)
                        } ?: Text(state.step, color = LocalGold.current.textGedaempft)
                        if (feedback == null && alarm?.voiceVariants?.isNotEmpty() == true) Text("Stimmvariante ${state.variation} von ${alarm.voiceVariants.size}", style = MaterialTheme.typography.bodySmall)
                        if (feedback == null && state.message.isNotBlank()) Text(state.message, color = Semantisch.warnung)
                        if (feedback == null && message.isNotBlank()) Text(message)
                        if (alarm != null) {
                            if (alarm.photoRequired && feedback == null) {
                                Section("Deine Foto-Aufgabe") {
                                    if (alarm.reference.isNotBlank()) PhotoPreview(File(alarm.reference))
                                    if (alarm.minBrightness > 0) Text("Mindestens ${alarm.minBrightness} % Helligkeit")
                                    if (alarm.color != "none") Text("${PhotoCheck.colors[alarm.color]}: mindestens ${alarm.colorPercent} % des Bildes")
                                    Text("Beenden öffnet die Kamera. Der Wecker stoppt erst, wenn das Foto passt.", style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
                                }
                            }
                            // Identical for test and real alarms; the photo task can never be bypassed here.
                            WeckTasten(alarm, checking, feedback?.action, snoozeWiggle, endWiggle,
                                onSnooze = { send("SNOOZE", alarm.id, state.ringId) }, onEnd = {
                                    if (pending != null || feedback != null) return@WeckTasten
                                    if (alarm.photoRequired) {
                                        val file = newPhoto(this@AlarmActivity)
                                        photoPath = file.absolutePath; photoRingId = state.ringId
                                        photo.launch(FileProvider.getUriForFile(this@AlarmActivity, "$packageName.photos", file))
                                    } else send("STOP", alarm.id, state.ringId)
                                })
                        } else StillerKnopf("Zurück zur App", { finish() })
                        // The only visible difference between a test and a real alarm.
                        if (state.test) Text("(Test)", style = MaterialTheme.typography.labelSmall, color = LocalGold.current.textGedaempft)
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
    val exit by animateFloatAsState(if (leaving) 1f else 0f, if (reduced) snap() else tween(300), label = "ringAusblenden")
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

/** Zwei große runde Tasten, nachts leicht zu treffen. Nebeneinander, auf schmalen Displays untereinander. */
@Composable
private fun WeckTasten(alarm: Alarm, checking: Boolean, confirmed: String?, snoozeWiggle: Int?, endWiggle: Int?,
    onSnooze: () -> Unit, onEnd: () -> Unit) {
    val gold = LocalGold.current
    val snoozeLeft = (alarm.snoozeLimit - alarm.snoozes).coerceAtLeast(0)
    val gap = 28.dp
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
        val sideBySide = maxWidth >= 120.dp * 2 + gap
        val diameter = if (sideBySide) ((maxWidth - gap) / 2).coerceAtMost(176.dp) else maxWidth.coerceAtMost(176.dp)
        val snooze = @Composable {
            val done = confirmed == "SNOOZE"
            RundeTaste(diameter, if (done) Icons.Default.Check else Icons.Default.Snooze, if (done) "Schlummert" else "Schlummern",
                Semantisch.info, enabled = snoozeLeft > 0 || done, main = false,
                info = if (snoozeLeft > 0) "${alarm.snoozeMinutes} Min. · noch $snoozeLeft" else "Keine Schlummerpause mehr",
                infoColor = if (snoozeLeft > 0) gold.textGedaempft else Semantisch.warnung, onClick = onSnooze,
                modifier = Modifier.wackelnBeiFehler(snoozeWiggle))
        }
        val end = @Composable {
            val done = confirmed == "STOP"
            RundeTaste(diameter, when { done -> Icons.Default.Check; alarm.photoRequired -> Icons.Default.PhotoCamera; else -> Icons.Default.AlarmOff },
                when { done -> "Beendet"; checking -> "Prüfe …"; else -> "Beenden" }, gold.primaer, enabled = !checking, main = true,
                info = if (alarm.photoRequired) "Mit Foto-Aufgabe" else " ", infoColor = gold.textGedaempft, onClick = onEnd,
                modifier = Modifier.wackelnBeiFehler(endWiggle))
        }
        if (sideBySide) Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Top) { snooze(); end() }
        else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) { end(); snooze() }
    }
}

@Composable
private fun RundeTaste(diameter: androidx.compose.ui.unit.Dp, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String,
    color: Color, enabled: Boolean, main: Boolean, info: String,
    infoColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val onColor = if (color.luminance() > 0.5f) Color(0xFF1B1B1B) else Color.White
    Column(modifier.width(diameter), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Knopf3D already gives the haptic pulse; no second impulse here.
        Knopf3D(onClick, Modifier.size(diameter), grundfarbe = color, form = androidx.compose.foundation.shape.CircleShape,
            aktiviert = enabled, hauptKnopf = main, innenAbstandWaagerecht = 8.dp, innenAbstandSenkrecht = 8.dp, beschreibung = label) {
            // Cap font scaling inside the circle so large system fonts never clip the label; the info line below scales fully.
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, density.fontScale.coerceAtMost(1.2f))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(icon, contentDescription = null, tint = onColor, modifier = Modifier.size(diameter * 0.3f))
                    Text(label, color = onColor, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        maxLines = 1, softWrap = false)
                }
            }
        }
        Text(info, color = infoColor, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

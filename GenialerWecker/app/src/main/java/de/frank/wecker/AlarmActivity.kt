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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.genialeideen.data.settings.SecureSettings
import de.frank.genialeideen.ui.*
import de.frank.genialeideen.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
            var photoAlarmId by rememberSaveable { mutableStateOf("") }
            var message by rememberSaveable { mutableStateOf("") }
            var checking by remember { mutableStateOf(false) }
            var seenAlarm by rememberSaveable { mutableStateOf(false) }
            val photo = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success && photoPath.isNotBlank()) scope.launch {
                    checking = true
                    try {
                        val alarm = AlarmService.state.value.alarm
                        if (alarm == null || alarm.id != photoAlarmId) return@launch
                        val result = withContext(Dispatchers.IO) { PhotoCheck.check(File(photoPath), alarm) }
                        message = result.message
                        if (result.accepted) command("PHOTO_OK", alarm.id)
                    } catch (e: Exception) { message = e.message ?: "Das Foto konnte nicht geprüft werden." }
                    finally { checking = false; File(photoPath).delete() }
                } else message = "Fotoaufnahme abgebrochen. Der Wecker läuft weiter."
            }
            LaunchedEffect(state.alarm?.id) {
                if (state.alarm != null) seenAlarm = true
                else if (seenAlarm) finish()
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
                BackHandler {
                    if (state.test) command("TEST_END")
                    else message = "Der Wecker läuft weiter. Nutze Schlummern oder erfülle die Stopp-Aufgabe."
                }
                Box(Modifier.fillMaxSize().background(LocalGold.current.hintergrund)) {
                    BewegterHintergrund()
                    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Spacer(Modifier.height(24.dp))
                        Text("GUTEN MORGEN", color = LocalGold.current.primaer, letterSpacing = 3.sp)
                        val alarm = state.alarm
                        // Show the live clock, not the alarm time, so the user always sees the current time.
                        var now by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
                        LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); kotlinx.coroutines.delay(1000) } }
                        Text(java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), fontFamily = IdeenSchriftBetont, fontSize = 76.sp, color = LocalGold.current.primaer)
                        Text(alarm?.name ?: "Der Wecker wird geöffnet …", style = MaterialTheme.typography.headlineMedium)
                        Text(state.step, color = LocalGold.current.textGedaempft)
                        if (alarm?.voiceVariants?.isNotEmpty() == true) Text("Stimmvariante ${state.variation} von ${alarm.voiceVariants.size}", style = MaterialTheme.typography.bodySmall)
                        if (state.message.isNotBlank()) Text(state.message, color = Semantisch.warnung)
                        if (message.isNotBlank()) Text(message)
                        if (alarm != null) {
                            if (alarm.photoRequired) {
                                Section("Deine Foto-Aufgabe") {
                                    if (alarm.reference.isNotBlank()) PhotoPreview(File(alarm.reference))
                                    if (alarm.minBrightness > 0) Text("Mindestens ${alarm.minBrightness} % Helligkeit")
                                    if (alarm.color != "none") Text("${PhotoCheck.colors[alarm.color]}: mindestens ${alarm.colorPercent} % des Bildes")
                                    Text("Beenden öffnet die Kamera. Der Wecker stoppt erst, wenn das Foto passt.", style = MaterialTheme.typography.bodySmall, color = LocalGold.current.textGedaempft)
                                }
                            }
                            // Identical for test and real alarms; the photo task can never be bypassed here.
                            WeckTasten(alarm, checking, onSnooze = { command("SNOOZE") }, onEnd = {
                                if (alarm.photoRequired) {
                                    val file = newPhoto(this@AlarmActivity)
                                    photoPath = file.absolutePath; photoAlarmId = alarm.id
                                    photo.launch(FileProvider.getUriForFile(this@AlarmActivity, "$packageName.photos", file))
                                } else command("STOP")
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
    private fun command(action: String, id: String? = null) {
        startService(Intent(this, AlarmService::class.java).setAction(action).putExtra("id", id))
    }
}

/** Zwei große runde Tasten, nachts leicht zu treffen. Nebeneinander, auf schmalen Displays untereinander. */
@Composable
private fun WeckTasten(alarm: Alarm, checking: Boolean, onSnooze: () -> Unit, onEnd: () -> Unit) {
    val gold = LocalGold.current
    val snoozeLeft = (alarm.snoozeLimit - alarm.snoozes).coerceAtLeast(0)
    val gap = 28.dp
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
        val sideBySide = maxWidth >= 120.dp * 2 + gap
        val diameter = if (sideBySide) ((maxWidth - gap) / 2).coerceAtMost(176.dp) else maxWidth.coerceAtMost(176.dp)
        val snooze = @Composable {
            RundeTaste(diameter, Icons.Default.Snooze, "Schlummern", Semantisch.info, enabled = snoozeLeft > 0, main = false,
                info = if (snoozeLeft > 0) "${alarm.snoozeMinutes} Min. · noch $snoozeLeft" else "Keine Schlummerpause mehr",
                infoColor = if (snoozeLeft > 0) gold.textGedaempft else Semantisch.warnung, onClick = onSnooze)
        }
        val end = @Composable {
            RundeTaste(diameter, if (alarm.photoRequired) Icons.Default.PhotoCamera else Icons.Default.AlarmOff,
                if (checking) "Prüfe …" else "Beenden", gold.primaer, enabled = !checking, main = true,
                info = if (alarm.photoRequired) "Mit Foto-Aufgabe" else " ", infoColor = gold.textGedaempft, onClick = onEnd)
        }
        if (sideBySide) Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Top) { snooze(); end() }
        else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) { end(); snooze() }
    }
}

@Composable
private fun RundeTaste(diameter: androidx.compose.ui.unit.Dp, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String,
    color: androidx.compose.ui.graphics.Color, enabled: Boolean, main: Boolean, info: String,
    infoColor: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val onColor = if (color.luminance() > 0.5f) androidx.compose.ui.graphics.Color(0xFF1B1B1B) else androidx.compose.ui.graphics.Color.White
    Column(Modifier.width(diameter), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Knopf3D(onClick, Modifier.size(diameter), grundfarbe = color, form = androidx.compose.foundation.shape.CircleShape,
            aktiviert = enabled, hauptKnopf = main, innenAbstandWaagerecht = 8.dp, innenAbstandSenkrecht = 8.dp, beschreibung = label) {
            // Cap font scaling inside the circle so large system fonts never clip the label; the info line below scales fully.
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
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

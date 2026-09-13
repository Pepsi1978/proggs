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
                BackHandler { message = "Der Wecker läuft weiter. Nutze Schlummern oder erfülle die Stopp-Aufgabe." }
                Box(Modifier.fillMaxSize().background(LocalGold.current.hintergrund)) {
                    BewegterHintergrund()
                    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Spacer(Modifier.height(24.dp))
                        Text(if (state.test) "TESTWECKEN" else "GUTEN MORGEN", color = LocalGold.current.primaer, letterSpacing = 3.sp)
                        val alarm = state.alarm
                        Text(alarm?.timeLabel ?: "Wecker", fontFamily = IdeenSchriftBetont, fontSize = 76.sp, color = LocalGold.current.primaer)
                        Text(alarm?.name ?: "Der Wecker wird geöffnet …", style = MaterialTheme.typography.headlineMedium)
                        Text(state.step, color = LocalGold.current.textGedaempft)
                        if (state.message.isNotBlank()) Text(state.message, color = Semantisch.warnung)
                        if (message.isNotBlank()) Text(message)
                        if (alarm != null) {
                            if (alarm.snoozeLimit > alarm.snoozes && !state.test) GoldKnopf(
                                "Schlummern · ${alarm.snoozeMinutes} Min. (${alarm.snoozeLimit - alarm.snoozes} übrig)",
                                { command("SNOOZE") }, Modifier.fillMaxWidth())
                            if (alarm.photoRequired && !state.test) {
                                Section("Deine Foto-Aufgabe") {
                                    if (alarm.reference.isNotBlank()) PhotoPreview(File(alarm.reference))
                                    if (alarm.minBrightness > 0) Text("Mindestens ${alarm.minBrightness} % Helligkeit")
                                    if (alarm.color != "none") Text("${PhotoCheck.colors[alarm.color]}: mindestens ${alarm.colorPercent} % des Bildes")
                                    GoldKnopf(if (checking) "Foto wird geprüft …" else "Foto aufnehmen & prüfen", {
                                        val file = newPhoto(this@AlarmActivity)
                                        photoPath = file.absolutePath; photoAlarmId = alarm.id
                                        photo.launch(FileProvider.getUriForFile(this@AlarmActivity, "$packageName.photos", file))
                                    }, Modifier.fillMaxWidth(), aktiviert = !checking, hauptKnopf = true)
                                }
                            } else GoldKnopf(if (state.test) "Test beenden" else "Wecker stoppen", { command("STOP") }, Modifier.fillMaxWidth(), hauptKnopf = true)
                        } else StillerKnopf("Zurück zur App", { finish() })
                    }
                }
            }
        }
    }
    private fun command(action: String, id: String? = null) {
        startService(Intent(this, AlarmService::class.java).setAction(action).putExtra("id", id))
    }
}

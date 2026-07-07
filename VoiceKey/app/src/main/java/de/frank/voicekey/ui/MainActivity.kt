package de.frank.voicekey.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.voicekey.obs.Obs
import de.frank.voicekey.service.BootReceiver
import de.frank.voicekey.ui.theme.VoiceKeyTheme

/**
 * Compose-Host: NUR die Einstellungs-Oberflaeche (das Lauschen macht der WakeWordService). Zeigt
 * beim ersten Start den Berechtigungs-Assistenten, danach den Hauptbildschirm.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: WakeWordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Vom BootReceiver: Tipp auf die Notification soll den Dienst direkt wieder starten.
        val autostart = intent?.getBooleanExtra(BootReceiver.EXTRA_AUTOSTART, false) == true
        if (autostart) {
            Obs.i("MainActivity", "onCreate", "Autostart nach Boot angefordert")
            viewModel.setServiceEnabled(true)
        }

        setContent { VoiceKeyTheme { VoiceKeyApp(viewModel) } }
    }

    override fun onResume() {
        super.onResume()
        // Freigaben koennen sich in den System-Settings geaendert haben.
        viewModel.refreshPermissions(this)
    }
}

@Composable
private fun VoiceKeyApp(viewModel: WakeWordViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val micLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.refreshPermissions(context)
        }
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.refreshPermissions(context)
        }

    // null = automatisch (Assistent solange Freigaben fehlen), sonst explizite Nutzer-Wahl.
    var setupVisible by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val showSetup = setupVisible ?: !state.permissions.allGranted

    Scaffold { innerPadding ->
        // Compose-Kurzcheck #13: innerPadding IMMER anwenden, sonst liegt Content unter der
        // Statusbar.
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(innerPadding)) {
            if (showSetup) {
                BackHandler { setupVisible = false }
                SetupScreen(
                    permissions = state.permissions,
                    onBack = { setupVisible = false },
                    onRequestMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onRequestNotifications = {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onRequestOverlay = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            )
                        )
                    },
                    onRequestBattery = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}"),
                            )
                        )
                    },
                    onRequestAssistKill = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                )
            } else {
                SettingsScreen(
                    state = state,
                    onToggleService = viewModel::setServiceEnabled,
                    onTestTrigger = { viewModel.testTrigger(context) },
                    onEndAssistant = { viewModel.endAssistant(context) },
                    onToggleFavorit = viewModel::toggleFavorit,
                    onAddWord = viewModel::addWord,
                    onUpdateWord = viewModel::updateWord,
                    onRemoveWord = viewModel::removeWord,
                    onRetryModels = viewModel::ensureModels,
                    onOpenSetup = { setupVisible = true },
                )
            }
        }
    }
}

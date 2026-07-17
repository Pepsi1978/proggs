package de.frank.fisetinbegleiter

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import de.frank.fisetinbegleiter.ui.FisetinApp
import de.frank.fisetinbegleiter.ui.theme.FisetinTheme

class MainActivity : ComponentActivity() {
    private var externalDestination by mutableStateOf<String?>(null)
    private var exportText: String = ""

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(exportText) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalDestination = intent.getStringExtra(EXTRA_DESTINATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            FisetinTheme {
                FisetinApp(
                    externalDestination = externalDestination,
                    onDestinationConsumed = { externalDestination = null },
                    onExport = { text ->
                        exportText = text
                        createDocument.launch("fisetin-protokoll.txt")
                    },
                    onOpenExactAlarmSettings = {
                        openSettings((application as FisetinApplication).alarmScheduler.exactAlarmSettingsIntent())
                    },
                    onOpenBatterySettings = {
                        openSettings((application as FisetinApplication).alarmScheduler.batterySettingsIntent())
                    },
                    showFirstStartNotice = !getPreferences(MODE_PRIVATE).getBoolean(KEY_NOTICE_SEEN, false),
                    onNoticeConfirmed = {
                        getPreferences(MODE_PRIVATE).edit { putBoolean(KEY_NOTICE_SEEN, true) }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        externalDestination = intent.getStringExtra(EXTRA_DESTINATION)
    }

    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            Log.w("FisetinBegleiter", "Herstellerspezifische Einstellungsseite nicht verfügbar", error)
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        private const val KEY_NOTICE_SEEN = "safety_notice_seen"
    }
}

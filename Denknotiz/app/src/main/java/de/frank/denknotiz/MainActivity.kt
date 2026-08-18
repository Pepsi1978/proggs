package de.frank.denknotiz

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import de.frank.denknotiz.audio.MicRecorder
import de.frank.denknotiz.tts.SpeechController
import de.frank.denknotiz.ui.DenknotizApp
import de.frank.denknotiz.ui.DenknotizViewModel

class MainActivity : ComponentActivity() {
    private var afterMicrophonePermission: (() -> Unit)? = null
    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) afterMicrophonePermission?.invoke()
        afterMicrophonePermission = null
    }
    private val notificationsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val createBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::export)
    }
    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::import)
    }
    private val viewModel: DenknotizViewModel by viewModels {
        val app = application as DenknotizApplication
        DenknotizViewModel.Factory(app.container, MicRecorder(applicationContext), SpeechController(applicationContext), cacheDir)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DenknotizApp(
                viewModel = viewModel,
                requestMicrophone = ::withMicrophonePermission,
                requestNotifications = {
                    if (android.os.Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                createBackup = { createBackup.launch("denknotiz-sicherung.json") },
                openBackup = { openBackup.launch(arrayOf("application/json", "text/json", "text/plain")) },
            )
        }
    }

    private fun withMicrophonePermission(action: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) action()
        else { afterMicrophonePermission = action; microphonePermission.launch(Manifest.permission.RECORD_AUDIO) }
    }
}

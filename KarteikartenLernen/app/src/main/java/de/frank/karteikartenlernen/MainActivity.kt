package de.frank.karteikartenlernen

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import de.frank.karteikartenlernen.audio.EdgeTtsPlayer
import de.frank.karteikartenlernen.model.MicState
import de.frank.karteikartenlernen.ui.KarteikartenApp

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private lateinit var edgeTtsPlayer: EdgeTtsPlayer
    private var pendingSpeechStart = false

    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && pendingSpeechStart) viewModel.startRecording()
        else if (!granted && pendingSpeechStart) viewModel.recordingPermissionDenied()
        pendingSpeechStart = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        edgeTtsPlayer = EdgeTtsPlayer(this)
        setContent {
            val state by viewModel.uiState.collectAsState()
            KarteikartenApp(
                state = state,
                viewModel = viewModel,
                onMicClick = {
                    if (state.mic == MicState.RECORDING) {
                        viewModel.stopRecording()
                    } else if (state.mic == MicState.IDLE) requestSpeechStart()
                },
                onSpeak = { text ->
                    edgeTtsPlayer.speak(
                        text = text,
                        voice = state.settings.voice,
                        speechRate = state.settings.speechRate,
                    )
                },
                onSpeakWithContinuation = { text, continuation ->
                    edgeTtsPlayer.speakWithPreparedContinuation(
                        text = text,
                        continuationText = continuation,
                        voice = state.settings.voice,
                        speechRate = state.settings.speechRate,
                    )
                },
                onContinueSpeaking = edgeTtsPlayer::continuePreparedSpeech,
                onStopSpeaking = {
                    edgeTtsPlayer.stop()
                },
                onLogin = { viewModel.login(this) },
                onOpenAuthPage = {
                    state.authVerificationUri?.let { uri ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                    }
                },
                onDarkChanged = { dark ->
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                },
            )
        }
    }

    private fun requestSpeechStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startRecording()
        } else {
            pendingSpeechStart = true
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onDestroy() {
        if (::edgeTtsPlayer.isInitialized) edgeTtsPlayer.shutdown()
        super.onDestroy()
    }
}

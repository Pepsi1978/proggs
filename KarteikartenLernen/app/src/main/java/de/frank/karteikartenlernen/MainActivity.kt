package de.frank.karteikartenlernen

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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

class MainActivity : ComponentActivity(), RecognitionListener {
    private val viewModel: AppViewModel by viewModels()
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var edgeTtsPlayer: EdgeTtsPlayer
    private var pendingSpeechStart = false

    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && pendingSpeechStart) startSpeechRecognition()
        pendingSpeechStart = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        speechRecognizer = if (SpeechRecognizer.isRecognitionAvailable(this)) {
            SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this) }
        } else null
        edgeTtsPlayer = EdgeTtsPlayer(this)
        setContent {
            val state by viewModel.uiState.collectAsState()
            KarteikartenApp(
                state = state,
                viewModel = viewModel,
                onMicClick = {
                    if (state.mic == MicState.RECORDING) {
                        viewModel.stopRecording()
                        speechRecognizer?.stopListening()
                    } else requestSpeechStart()
                },
                onSpeak = { text ->
                    edgeTtsPlayer.speak(
                        text = text,
                        voice = state.settings.voice,
                        speechRate = state.settings.speechRate,
                    )
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
            startSpeechRecognition()
        } else {
            pendingSpeechStart = true
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startSpeechRecognition() {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            viewModel.startRecording()
            viewModel.stopRecording()
            viewModel.transcriptionFinished(null)
            return
        }
        viewModel.startRecording()
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            },
        )
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        if (::edgeTtsPlayer.isInitialized) edgeTtsPlayer.shutdown()
        super.onDestroy()
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() { viewModel.stopRecording() }
    override fun onError(error: Int) { viewModel.transcriptionFinished(null) }
    override fun onPartialResults(partialResults: Bundle?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        viewModel.transcriptionFinished(text)
    }
}

package de.frank.entropyreducer.presentation.components

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.audio.AudioRecorder
import de.frank.entropyreducer.data.audio.RecordingService
import de.frank.entropyreducer.domain.usecase.TranscribeAudioUseCase
import de.frank.entropyreducer.presentation.theme.CosmosColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Wiederverwendbarer ViewModel fuer Whisper-Voice-Capture (Frank-Wunsch
 * 2026-05-08: "Alle Antwort-Bereiche sollen Whisper Large V3 Turbo nutzen,
 * nicht das schlechtere System-Transkriptionsmodell"). Wird von [WhisperMicButton]
 * konsumiert — jede UI-Stelle die Sprach-Eingabe braucht (Method-Prompt,
 * Aufgaben-Nachtrag, Briefing-Antwort) kann einen Mic-Button mit eigenem
 * VM einbinden.
 *
 * State-Flow: IDLE → tap → RECORDING → tap-stop → PROCESSING → IDLE +
 * Transkript-Callback.
 */
enum class VoiceCaptureState { IDLE, RECORDING, PROCESSING }

@HiltViewModel
class VoiceCaptureViewModel @Inject constructor(
    application: Application,
    private val recorder: AudioRecorder,
    private val transcribe: TranscribeAudioUseCase,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(VoiceCaptureState.IDLE)
    val state: StateFlow<VoiceCaptureState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var pendingCallback: ((String) -> Unit)? = null

    fun toggle(onResult: (String) -> Unit) {
        when (_state.value) {
            VoiceCaptureState.IDLE -> startRecording(onResult)
            VoiceCaptureState.RECORDING -> stopAndTranscribe()
            VoiceCaptureState.PROCESSING -> Unit
        }
    }

    private fun startRecording(onResult: (String) -> Unit) {
        val app = getApplication<Application>()
        runCatching {
            recorder.start()
            RecordingService.start(app)
            pendingCallback = onResult
            _state.value = VoiceCaptureState.RECORDING
            _error.value = null
        }.onFailure { ex ->
            _error.value = ex.message ?: "Aufnahme konnte nicht gestartet werden"
            _state.value = VoiceCaptureState.IDLE
        }
    }

    private fun stopAndTranscribe() {
        val app = getApplication<Application>()
        val file = recorder.stop()
        RecordingService.stop(app)
        if (file == null || !file.exists() || file.length() == 0L) {
            _state.value = VoiceCaptureState.IDLE
            pendingCallback = null
            return
        }
        _state.value = VoiceCaptureState.PROCESSING
        viewModelScope.launch {
            transcribe(file)
                .onSuccess { transcript ->
                    pendingCallback?.invoke(transcript.trim())
                    pendingCallback = null
                    _state.value = VoiceCaptureState.IDLE
                }
                .onFailure { ex ->
                    _error.value = ex.message ?: "Transkription fehlgeschlagen"
                    pendingCallback = null
                    _state.value = VoiceCaptureState.IDLE
                }
        }
    }

    fun clearError() { _error.value = null }

    override fun onCleared() {
        if (recorder.isRecording()) recorder.discard()
        super.onCleared()
    }
}

/**
 * Mic-Button der Whisper Large V3 Turbo (ueber TranscribeAudioUseCase) nutzt.
 * Visuell: cyan-gefuellter Kreis im IDLE, rot-pulsierend im RECORDING,
 * Loading-Spinner im PROCESSING.
 *
 * @param onTranscript Callback mit dem fertig transkribierten Text.
 */
@Composable
fun WhisperMicButton(
    onTranscript: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    vm: VoiceCaptureViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val (bg, icon, tint) = when (state) {
        VoiceCaptureState.IDLE -> Triple(
            CosmosColors.AccentPrimary,
            Icons.Outlined.Mic,
            Color.White,
        )
        VoiceCaptureState.RECORDING -> Triple(
            CosmosColors.Critical,
            Icons.Outlined.Stop,
            Color.White,
        )
        VoiceCaptureState.PROCESSING -> Triple(
            CosmosColors.AccentSecondary,
            Icons.Outlined.Mic,
            Color.White,
        )
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(enabled = state != VoiceCaptureState.PROCESSING) {
                vm.toggle(onTranscript)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (state == VoiceCaptureState.PROCESSING) {
            CircularProgressIndicator(
                modifier = Modifier.size(size * 0.55f),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = when (state) {
                    VoiceCaptureState.RECORDING -> "Aufnahme stoppen"
                    else -> "Per Sprache aufnehmen (Whisper Large V3 Turbo)"
                },
                tint = tint,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}

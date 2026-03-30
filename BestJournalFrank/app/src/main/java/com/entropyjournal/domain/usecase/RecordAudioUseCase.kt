package com.entropyjournal.domain.usecase

import com.entropyjournal.util.AudioRecorderHelper
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject

class RecordAudioUseCase @Inject constructor(
    private val audioRecorderHelper: AudioRecorderHelper
) {
    val amplitude: StateFlow<Float> = audioRecorderHelper.amplitude
    val durationSeconds: StateFlow<Int> = audioRecorderHelper.durationSeconds

    suspend fun startRecording(outputFile: File) {
        audioRecorderHelper.startRecording(outputFile)
    }

    fun stopRecording() {
        audioRecorderHelper.stopRecording()
    }

    fun isRecording(): Boolean = audioRecorderHelper.isCurrentlyRecording()
}

package com.bestjournal.app.data.repository

import com.bestjournal.app.data.local.whisper.LocalWhisperTranscriber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionRepository @Inject constructor(
    private val localWhisper: LocalWhisperTranscriber
) {
    suspend fun transcribeAudio(audioFile: File): Result<String> {
        // Always use local offline Whisper via sherpa-onnx
        return localWhisper.transcribe(audioFile)
    }
}

package com.entropyjournal.domain.usecase

import com.entropyjournal.data.repository.TranscriptionRepository
import java.io.File
import javax.inject.Inject

class TranscribeAudioUseCase @Inject constructor(
    private val transcriptionRepository: TranscriptionRepository
) {
    suspend operator fun invoke(audioFile: File): Result<String> {
        return transcriptionRepository.transcribeAudio(audioFile)
    }
}

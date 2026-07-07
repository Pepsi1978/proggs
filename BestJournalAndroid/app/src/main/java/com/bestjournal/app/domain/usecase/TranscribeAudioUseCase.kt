package com.bestjournal.app.domain.usecase

import com.bestjournal.app.data.repository.TranscriptionRepository
import com.bestjournal.app.data.repository.TranscriptionResult
import java.io.File
import javax.inject.Inject

class TranscribeAudioUseCase @Inject constructor(
    private val transcriptionRepository: TranscriptionRepository
) {
    suspend operator fun invoke(audioFile: File): Result<TranscriptionResult> {
        return transcriptionRepository.transcribeAudio(audioFile)
    }
}

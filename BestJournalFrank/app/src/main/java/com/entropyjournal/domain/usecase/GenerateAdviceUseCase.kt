package com.entropyjournal.domain.usecase

import com.entropyjournal.data.repository.AdviceRepository
import com.entropyjournal.domain.model.AdviceBlock
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GenerateAdviceUseCase @Inject constructor(
    private val adviceRepository: AdviceRepository
) {
    operator fun invoke(): Flow<List<AdviceBlock>> {
        return adviceRepository.getAllAdviceBlocks()
    }
}

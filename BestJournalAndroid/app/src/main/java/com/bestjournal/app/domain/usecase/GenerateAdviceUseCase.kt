package com.bestjournal.app.domain.usecase

import com.bestjournal.app.data.repository.AdviceRepository
import com.bestjournal.app.domain.model.AdviceBlock
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GenerateAdviceUseCase @Inject constructor(
    private val adviceRepository: AdviceRepository
) {
    operator fun invoke(): Flow<List<AdviceBlock>> {
        return adviceRepository.getAllAdviceBlocks()
    }
}

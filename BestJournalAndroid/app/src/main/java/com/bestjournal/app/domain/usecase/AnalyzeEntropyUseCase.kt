package com.bestjournal.app.domain.usecase

import com.bestjournal.app.data.repository.AdviceRepository
import com.bestjournal.app.data.repository.JournalRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class AnalyzeEntropyUseCase
@Inject
constructor(
    private val journalRepository: JournalRepository,
    private val adviceRepository: AdviceRepository,
) {
    suspend operator fun invoke(
        freshAnalysis: Boolean = false,
        modelName: String = com.bestjournal.app.data.remote.ai.FirebaseAiService.MODEL_FLASH,
    ): Result<Unit> {
        val entries = journalRepository.getAllEntries().first()
        if (entries.isEmpty()) return Result.failure(Exception("Keine Tagebucheinträge vorhanden"))

        val total = entries.size
        val allText =
            entries
                .mapIndexed { index, entry ->
                    "=== EINTRAG ${index + 1} von $total (${com.bestjournal.app.util.DateTimeFormatter.formatFull(entry.timestamp)}) ===\n${entry.displayText}"
                }
                .joinToString("\n\n")

        return adviceRepository.analyzeEntropy(allText, entries.size, freshAnalysis, modelName)
    }
}

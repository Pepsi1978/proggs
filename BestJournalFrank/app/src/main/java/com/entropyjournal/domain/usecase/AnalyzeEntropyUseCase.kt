package com.entropyjournal.domain.usecase

import com.entropyjournal.data.repository.AdviceRepository
import com.entropyjournal.data.repository.JournalRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AnalyzeEntropyUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
    private val adviceRepository: AdviceRepository
) {
    suspend operator fun invoke(freshAnalysis: Boolean = false): Result<Unit> {
        val entries = journalRepository.getAllEntries().first()
        if (entries.isEmpty()) return Result.failure(Exception("Keine Tagebucheinträge vorhanden"))

        val total = entries.size
        val allText = entries.mapIndexed { index, entry ->
            "=== EINTRAG ${index + 1} von $total (${com.entropyjournal.util.DateTimeFormatter.formatFull(entry.timestamp)}) ===\n${entry.displayText}"
        }.joinToString("\n\n")

        return adviceRepository.analyzeEntropy(allText, entries.size, freshAnalysis)
    }
}

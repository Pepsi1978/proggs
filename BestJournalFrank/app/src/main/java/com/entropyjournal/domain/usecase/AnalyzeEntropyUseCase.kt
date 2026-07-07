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
        val allEntries = journalRepository.getAllEntries().first()
        if (allEntries.isEmpty()) return Result.failure(Exception("Keine Tagebucheinträge vorhanden"))

        // Limit to last 40 entries to keep API calls fast and cost-effective
        val entries = allEntries.takeLast(40)

        val total = entries.size
        val allText = entries.mapIndexed { index, entry ->
            "=== EINTRAG ${index + 1} von $total (${com.entropyjournal.util.DateTimeFormatter.formatFull(entry.timestamp)}) ===\n${entry.displayText}"
        }.joinToString("\n\n")

        return adviceRepository.analyzeEntropy(allText, entries.size, freshAnalysis)
    }
}

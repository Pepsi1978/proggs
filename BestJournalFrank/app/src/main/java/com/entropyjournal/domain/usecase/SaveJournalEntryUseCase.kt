package com.entropyjournal.domain.usecase

import com.entropyjournal.data.repository.JournalRepository
import com.entropyjournal.domain.model.JournalEntry
import javax.inject.Inject

class SaveJournalEntryUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    suspend operator fun invoke(entry: JournalEntry): Long {
        return journalRepository.saveEntry(entry)
    }
}

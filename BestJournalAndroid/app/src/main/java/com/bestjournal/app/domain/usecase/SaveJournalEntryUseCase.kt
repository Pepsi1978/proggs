package com.bestjournal.app.domain.usecase

import com.bestjournal.app.data.repository.JournalRepository
import com.bestjournal.app.domain.model.JournalEntry
import javax.inject.Inject

class SaveJournalEntryUseCase @Inject constructor(
    private val journalRepository: JournalRepository
) {
    suspend operator fun invoke(entry: JournalEntry): Long {
        return journalRepository.saveEntry(entry)
    }
}

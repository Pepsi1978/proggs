package de.frank.entropyreducer.presentation.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDao
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorEntryEntity
import de.frank.entropyreducer.data.prefs.JournalSyncMeta
import de.frank.entropyreducer.data.prefs.JournalSyncStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class JournalViewModel
@Inject
constructor(dao: JournalMirrorDao, syncMeta: JournalSyncMeta) : ViewModel() {

    val entries: StateFlow<List<JournalMirrorEntryEntity>> =
        dao.observeEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val syncStatus: StateFlow<JournalSyncStatus> =
        syncMeta.status.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            JournalSyncStatus(0L, 0),
        )
}

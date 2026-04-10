package com.bestjournal.app.domain.usecase

import com.bestjournal.app.ui.screens.journal.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Singleton that holds download/upload progress visible across all ViewModels. */
object SyncProgressHolder {
    private val _status = MutableStateFlow(SyncStatus.IDLE)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _downloadCurrent = MutableStateFlow(0)
    val downloadCurrent: StateFlow<Int> = _downloadCurrent.asStateFlow()

    private val _downloadTotal = MutableStateFlow(0)
    val downloadTotal: StateFlow<Int> = _downloadTotal.asStateFlow()

    fun setUploading(current: Int = 0, total: Int = 0) {
        _status.value = SyncStatus.UPLOADING
        _downloadCurrent.value = current
        _downloadTotal.value = total
    }

    fun setDownloading(current: Int, total: Int) {
        _status.value = SyncStatus.DOWNLOADING
        _downloadCurrent.value = current
        _downloadTotal.value = total
    }

    fun setSynced() {
        _status.value = SyncStatus.SYNCED
        _downloadCurrent.value = 0
        _downloadTotal.value = 0
    }

    fun setError() {
        _status.value = SyncStatus.ERROR
        _downloadCurrent.value = 0
        _downloadTotal.value = 0
    }

    fun setIdle() {
        _status.value = SyncStatus.IDLE
        _downloadCurrent.value = 0
        _downloadTotal.value = 0
    }
}

package com.bestjournal.app.ui.screens.retrospective

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestjournal.app.data.local.dao.EntryPhotoDao
import com.bestjournal.app.data.local.entity.EntryPhotoEntity
import com.bestjournal.app.data.repository.RetrospectiveRepository
import com.bestjournal.app.domain.usecase.GenerateRetrospectiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RetrospectiveViewModel
@Inject
constructor(
    private val repository: RetrospectiveRepository,
    private val generateUseCase: GenerateRetrospectiveUseCase,
    private val entryPhotoDao: EntryPhotoDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    val weeklySummaries =
        repository
            .getWeeklySummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlySummaries =
        repository
            .getMonthlySummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val yearlySummaries =
        repository
            .getYearlySummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isProfileSwitch = MutableStateFlow(false)
    val isProfileSwitch: StateFlow<Boolean> = _isProfileSwitch.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentPhotos = MutableStateFlow<List<EntryPhotoEntity>>(emptyList())
    val currentPhotos: StateFlow<List<EntryPhotoEntity>> = _currentPhotos.asStateFlow()

    fun loadPhotosForPeriod(startDate: Long, endDate: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allPhotos = entryPhotoDao.getPhotosForDateRange(startDate, endDate)
                // Filter out photos whose files no longer exist on disk
                val existing = allPhotos.filter { java.io.File(it.filePath).exists() }
                Log.d(
                    "RetroVM",
                    "Photos for period: ${allPhotos.size} found, ${existing.size} exist on disk",
                )
                _currentPhotos.value = existing
            } catch (e: Exception) {
                Log.e("RetroVM", "Failed to load photos: ${e.message}")
                _currentPhotos.value = emptyList()
            }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Wait for any active restore/sync to finish before generating reviews.
                // Initial delay gives SettingsVM time to start downloadMissingPhotos()
                // and set SyncProgressHolder to DOWNLOADING before we check.
                kotlinx.coroutines.delay(3000)
                val syncStatus = com.bestjournal.app.domain.usecase.SyncProgressHolder.status
                if (
                    syncStatus.value ==
                        com.bestjournal.app.ui.screens.journal.SyncStatus.DOWNLOADING ||
                        syncStatus.value ==
                            com.bestjournal.app.ui.screens.journal.SyncStatus.UPLOADING
                ) {
                    Log.d("RetroVM", "Sync active (${syncStatus.value}), waiting for completion...")
                    syncStatus.first { status ->
                        status != com.bestjournal.app.ui.screens.journal.SyncStatus.DOWNLOADING &&
                            status != com.bestjournal.app.ui.screens.journal.SyncStatus.UPLOADING
                    }
                    // Extra delay to let DB writes settle after restore
                    kotlinx.coroutines.delay(2000)
                    Log.d("RetroVM", "Sync finished, proceeding with review generation")
                } else {
                    Log.d("RetroVM", "No active sync (${syncStatus.value}), proceeding")
                }

                // One-time cleanup via local flag file (not backed up to Drive)
                val flagFile = java.io.File(context.filesDir, ".retro_cleaned_v3")
                if (!flagFile.exists()) {
                    repository.deleteAll()
                    flagFile.createNewFile()
                    Log.d("RetroVM", "One-time cleanup: cleared old retrospective data")
                }

                _isGenerating.value = true
                val count = generateUseCase.generateMissing()
                if (count > 0) {
                    Log.d("RetroVM", "Generated $count new reviews")
                }
            } catch (e: Exception) {
                Log.e("RetroVM", "Review generation failed: ${e.message}", e)
                _errorMessage.value = e.message
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private var regenerationJob: kotlinx.coroutines.Job? = null

    fun regenerateAll() {
        // Cancel any running regeneration to prevent race conditions
        regenerationJob?.cancel()
        regenerationJob =
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    _isProfileSwitch.value = true
                    _isGenerating.value = true
                    _errorMessage.value = null
                    // Delete old and regenerate — if generation fails, error is shown
                    // and user can retry (which will attempt to fill the gaps)
                    repository.deleteAll()
                    Log.d("RetroVM", "Deleted all retrospectives for profile-change regeneration")
                    val count = generateUseCase.generateMissing()
                    if (count == 0) {
                        Log.w("RetroVM", "Regeneration produced 0 reviews — AI may have failed")
                    } else {
                        Log.d("RetroVM", "Regenerated $count reviews with new profile style")
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e // Don't catch cancellation
                } catch (e: Exception) {
                    Log.e("RetroVM", "Regeneration failed: ${e.message}", e)
                    _errorMessage.value = e.message
                } finally {
                    _isGenerating.value = false
                    _isProfileSwitch.value = false
                }
            }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun retryGeneration() {
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isGenerating.value = true
                val count = generateUseCase.generateMissing()
                Log.d("RetroVM", "Retry generated $count reviews")
            } catch (e: Exception) {
                Log.e("RetroVM", "Retry failed: ${e.message}", e)
                _errorMessage.value = e.message
            } finally {
                _isGenerating.value = false
            }
        }
    }
}

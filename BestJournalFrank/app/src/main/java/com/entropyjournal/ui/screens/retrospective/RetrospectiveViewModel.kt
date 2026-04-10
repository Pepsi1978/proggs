package com.entropyjournal.ui.screens.retrospective

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entropyjournal.data.local.dao.EntryPhotoDao
import com.entropyjournal.data.local.entity.EntryPhotoEntity
import com.entropyjournal.data.repository.RetrospectiveRepository
import com.entropyjournal.domain.usecase.GenerateRetrospectiveUseCase
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

    private val _isGenerating = MutableStateFlow(true)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isProfileSwitch = MutableStateFlow(false)
    val isProfileSwitch: StateFlow<Boolean> = _isProfileSwitch.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentPhotos = MutableStateFlow<List<EntryPhotoEntity>>(emptyList())
    val currentPhotos: StateFlow<List<EntryPhotoEntity>> = _currentPhotos.asStateFlow()

    private suspend fun awaitSyncComplete() {
        // Only wait if user is signed in — no account means no sync can happen
        val prefs =
            try {
                val masterKey =
                    androidx.security.crypto.MasterKeys.getOrCreate(
                        androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
                    )
                androidx.security.crypto.EncryptedSharedPreferences.create(
                    com.entropyjournal.util.Constants.ENCRYPTED_PREFS_NAME,
                    masterKey,
                    context,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
                        .AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
                        .AES256_GCM,
                )
            } catch (_: Exception) {
                return
            }
        val hasAccount =
            prefs.getString(com.entropyjournal.util.Constants.PREF_GOOGLE_ACCOUNT_EMAIL, null) !=
                null
        if (!hasAccount) {
            Log.d("RetroVM", "No Google account, skipping sync wait")
            return
        }

        kotlinx.coroutines.delay(3000)
        val syncStatus = com.entropyjournal.domain.usecase.SyncProgressHolder.status
        val currentStatus = syncStatus.value
        if (
            currentStatus == com.entropyjournal.ui.screens.journal.SyncStatus.DOWNLOADING ||
                currentStatus == com.entropyjournal.ui.screens.journal.SyncStatus.UPLOADING
        ) {
            Log.d("RetroVM", "Sync active ($currentStatus), waiting for completion...")
            syncStatus.first { status ->
                status != com.entropyjournal.ui.screens.journal.SyncStatus.DOWNLOADING &&
                    status != com.entropyjournal.ui.screens.journal.SyncStatus.UPLOADING
            }
            kotlinx.coroutines.delay(2000)
            Log.d("RetroVM", "Sync finished, proceeding")
        } else {
            Log.d("RetroVM", "No active sync ($currentStatus), proceeding")
        }
    }

    fun loadPhotosForPeriod(startDate: Long, endDate: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allPhotos = entryPhotoDao.getPhotosForDateRange(startDate, endDate)
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
                awaitSyncComplete()

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
        regenerationJob?.cancel()
        regenerationJob =
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    _isProfileSwitch.value = true
                    _isGenerating.value = true
                    _errorMessage.value = null
                    awaitSyncComplete()
                    repository.deleteAll()
                    Log.d("RetroVM", "Deleted all retrospectives for profile-change regeneration")
                    val count = generateUseCase.generateMissing()
                    if (count == 0) {
                        Log.w("RetroVM", "Regeneration produced 0 reviews — AI may have failed")
                    } else {
                        Log.d("RetroVM", "Regenerated $count reviews with new profile style")
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
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
                awaitSyncComplete()
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

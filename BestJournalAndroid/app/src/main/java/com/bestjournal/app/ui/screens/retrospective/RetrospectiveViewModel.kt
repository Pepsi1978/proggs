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

    private val _isWaitingForRestore = MutableStateFlow(false)
    val isWaitingForRestore: StateFlow<Boolean> = _isWaitingForRestore.asStateFlow()

    private val _isProfileSwitch = MutableStateFlow(false)
    val isProfileSwitch: StateFlow<Boolean> = _isProfileSwitch.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentPhotos = MutableStateFlow<List<EntryPhotoEntity>>(emptyList())
    val currentPhotos: StateFlow<List<EntryPhotoEntity>> = _currentPhotos.asStateFlow()

    /**
     * Waits until ALL photos referenced in DB actually exist on disk. After a restore, photos are
     * downloaded in the background — we must wait for that to complete before generating
     * retrospectives, otherwise they'll be missing photos.
     */
    private suspend fun awaitSyncComplete() {
        _isWaitingForRestore.value = true
        try {
            awaitSyncCompleteInternal()
        } finally {
            _isWaitingForRestore.value = false
        }
    }

    private suspend fun awaitSyncCompleteInternal() {
        // Only wait if user is signed in — no account means no sync can happen
        val prefs =
            try {
                val masterKey =
                    androidx.security.crypto.MasterKeys.getOrCreate(
                        androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
                    )
                androidx.security.crypto.EncryptedSharedPreferences.create(
                    com.bestjournal.app.util.Constants.ENCRYPTED_PREFS_NAME,
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
        // Step 1: Wait for PREF_RESTORE_PENDING flag to clear.
        // This flag is set BEFORE restore starts and cleared AFTER all photos are downloaded.
        // It survives app restarts (SharedPreferences), so the new instance sees it.
        val maxWaitMs = 10L * 60 * 1000 // 10 minutes max
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < maxWaitMs) {
            val restorePending =
                prefs.getBoolean(com.bestjournal.app.util.Constants.PREF_RESTORE_PENDING, false)
            if (!restorePending) break
            Log.d("RetroVM", "Restore in progress, waiting...")
            kotlinx.coroutines.delay(3000)
        }

        // Step 2: Also verify all photos are actually on disk (belt-and-suspenders)
        val hasAccount =
            prefs.getString(com.bestjournal.app.util.Constants.PREF_GOOGLE_ACCOUNT_EMAIL, null) !=
                null
        if (!hasAccount) {
            Log.d("RetroVM", "No Google account, skipping photo check")
            return
        }

        val dbFile = context.getDatabasePath("entropy_journal_db")
        if (!dbFile.exists()) return

        var lastMissing = -1
        while (System.currentTimeMillis() - start < maxWaitMs) {
            val missing = countMissingPhotos(dbFile)
            if (missing == 0) {
                if (lastMissing > 0) {
                    Log.d("RetroVM", "All photos on disk, proceeding")
                }
                break
            }
            if (missing != lastMissing) {
                Log.d("RetroVM", "Waiting for $missing photos to download...")
                lastMissing = missing
            }
            kotlinx.coroutines.delay(3000)
        }

        if (System.currentTimeMillis() - start >= maxWaitMs) {
            Log.w("RetroVM", "Timeout waiting for restore, proceeding anyway")
        }
    }

    private fun countMissingPhotos(dbFile: java.io.File): Int {
        return try {
            val db =
                android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.path,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
                )
            val cursor = db.rawQuery("SELECT filePath FROM entry_photos", null)
            var missing = 0
            while (cursor.moveToNext()) {
                val path = cursor.getString(0)
                if (!java.io.File(path).exists()) missing++
            }
            cursor.close()
            db.close()
            missing
        } catch (_: Exception) {
            0
        }
    }

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
        // Cancel any running regeneration to prevent race conditions
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

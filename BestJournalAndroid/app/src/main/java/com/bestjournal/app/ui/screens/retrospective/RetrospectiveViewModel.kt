package com.bestjournal.app.ui.screens.retrospective

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.billing.SubscriptionState
import com.bestjournal.app.data.local.dao.EntryPhotoDao
import com.bestjournal.app.data.local.entity.EntryPhotoEntity
import com.bestjournal.app.data.repository.RetrospectiveRepository
import com.bestjournal.app.domain.usecase.GenerateRetrospectiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
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
    val billingManager: BillingManager,
    private val profileChangeBus: com.bestjournal.app.util.ProfileChangeBus,
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

    private val _lockedWeeks =
        MutableStateFlow<List<GenerateRetrospectiveUseCase.LockedWeekRange>>(emptyList())
    val lockedWeeks: StateFlow<List<GenerateRetrospectiveUseCase.LockedWeekRange>> =
        _lockedWeeks.asStateFlow()

    val subscriptionState: StateFlow<SubscriptionState> = billingManager.subscriptionState

    private fun isPremium(): Boolean =
        billingManager.subscriptionState.value is SubscriptionState.Subscribed

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
                com.bestjournal.app.util.EncryptedPrefsProvider.get(context)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("RetroVM", "Failed to init encrypted prefs", e)
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
            try {
                db.rawQuery("SELECT filePath FROM entry_photos", null).use { cursor ->
                    var missing = 0
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(0)
                        if (!java.io.File(path).exists()) missing++
                    }
                    missing
                }
            } finally {
                runCatching { db.close() }
            }
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

    // Single source of truth for ANY running retrospective work (init, regenerate,
    // retry). Every new launch waits for the previous one to fully terminate
    // (cancelAndJoin) before it calls deleteAll() + generateMissing(). Without this
    // serialization, a second trigger arriving while the first is still running in
    // parallel async-tasks would leave stale inserts racing the fresh deleteAll(),
    // producing duplicate weekly entries (e.g. same week appearing multiple times).
    private var currentJob: kotlinx.coroutines.Job? = null

    init {
        currentJob =
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    awaitSyncComplete()

                    // One-time cleanup via local flag file (not backed up to Drive)
                    val flagFile = java.io.File(context.filesDir, ".retro_cleaned_v3")
                    if (!flagFile.exists()) {
                        repository.deleteAll()
                        if (!flagFile.createNewFile()) {
                            Log.w(
                                "RetroVM",
                                "Flag file already exists or could not be created: ${flagFile.absolutePath}",
                            )
                        }
                        Log.d("RetroVM", "One-time cleanup: cleared old retrospective data")
                    }

                    // Profile-change trigger: if the user switched dashboard scenario since last visit,
                    // wipe all retros and regenerate them with the new profile style.
                    // Uses the same EncryptedSharedPreferences store that SettingsScreen writes to.
                    val encPrefs =
                        try {
                            com.bestjournal.app.util.EncryptedPrefsProvider.get(context)
                        } catch (_: Exception) {
                            null
                        }
                    if (
                        encPrefs?.getBoolean(
                            com.bestjournal.app.util.Constants.PREF_RETRO_NEEDS_REGEN,
                            false,
                        ) == true
                    ) {
                        encPrefs
                            .edit()
                            .remove(com.bestjournal.app.util.Constants.PREF_RETRO_NEEDS_REGEN)
                            .apply()
                        repository.deleteAll()
                        Log.d("RetroVM", "Profile change detected — regenerating all reviews")
                    }

                    val premium = isPremium()
                    _isGenerating.value = true
                    val count = generateUseCase.generateMissing(isPremium = premium)
                    if (count > 0) {
                        Log.d("RetroVM", "Generated $count new reviews (premium=$premium)")
                    }
                    // Load locked week placeholders for free users
                    if (!premium) {
                        _lockedWeeks.value = generateUseCase.getLockedWeekRanges()
                        Log.d("RetroVM", "Locked weeks: ${_lockedWeeks.value.size}")
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("RetroVM", "Review generation failed: ${e.message}", e)
                    _errorMessage.value = e.message
                } finally {
                    _isGenerating.value = false
                }
            }

        // React to profile-change events from SettingsScreen
        viewModelScope.launch {
            var previousVersion = profileChangeBus.version.value
            profileChangeBus.version.collect { newVersion ->
                if (newVersion > previousVersion) {
                    previousVersion = newVersion
                    Log.d("RetroVM", "Profile change bus fired (v=$newVersion) — regenerating")
                    regenerateAll()
                }
            }
        }

        // React to subscription state changes: regenerate when user upgrades
        viewModelScope.launch {
            billingManager.subscriptionState.collect { state ->
                if (state is SubscriptionState.Subscribed && _lockedWeeks.value.isNotEmpty()) {
                    Log.d("RetroVM", "User upgraded to premium — generating locked reviews")
                    _lockedWeeks.value = emptyList()
                    val previous = currentJob
                    currentJob =
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                previous?.cancelAndJoin()
                            } catch (_: kotlinx.coroutines.CancellationException) {
                                // Expected
                            }
                            try {
                                _isGenerating.value = true
                                val count = generateUseCase.generateMissing(isPremium = true)
                                Log.d("RetroVM", "Post-upgrade generated $count reviews")
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Log.e("RetroVM", "Post-upgrade generation failed: ${e.message}", e)
                            } finally {
                                _isGenerating.value = false
                            }
                        }
                }
            }
        }
    }

    /**
     * Returns a localized "Letzte Aktualisierung am <timestamp>" text for the newest
     * retrospective summary, or null if no summaries exist yet. Reuses the exact string
     * resource and format used by the Dashboard so both screens stay visually consistent
     * and we reuse the existing 26-language translations.
     */
    fun getLastUpdatedText(): String? {
        val newest =
            (weeklySummaries.value + monthlySummaries.value + yearlySummaries.value)
                .maxByOrNull { it.createdAt }
                ?: return null
        val locale = java.util.Locale.getDefault()
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMHHmm")
        val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern, locale)
        val formatted =
            java.time.ZonedDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(newest.createdAt),
                    java.time.ZoneId.systemDefault(),
                )
                .format(formatter)
        return context.getString(
            com.bestjournal.app.R.string.dashboard_last_updated,
            formatted,
        )
    }

    /**
     * Called whenever the user re-enters the Retrospective screen. Checks the profile-change flag
     * and triggers a full regeneration if the user switched dashboard scenarios since last visit.
     */
    fun checkProfileChangeAndRegenerate() {
        viewModelScope.launch(Dispatchers.IO) {
            val encPrefs =
                try {
                    com.bestjournal.app.util.EncryptedPrefsProvider.get(context)
                } catch (_: Exception) {
                    return@launch
                }
            val needsRegen =
                encPrefs.getBoolean(
                    com.bestjournal.app.util.Constants.PREF_RETRO_NEEDS_REGEN,
                    false,
                )
            if (!needsRegen) return@launch
            encPrefs
                .edit()
                .remove(com.bestjournal.app.util.Constants.PREF_RETRO_NEEDS_REGEN)
                .apply()
            Log.d("RetroVM", "Profile change detected on tab enter — regenerating")
            regenerateAll()
        }
    }

    fun regenerateAll() {
        val previous = currentJob
        currentJob =
            viewModelScope.launch(Dispatchers.IO) {
                // Wait for the previous generation to FULLY terminate (all parallel
                // async-tasks inside generateMissing*() returned) before wiping the
                // DB. Plain cancel() is non-blocking and would let stale inserts
                // land AFTER deleteAll() → duplicate weeks.
                try {
                    previous?.cancelAndJoin()
                } catch (_: kotlinx.coroutines.CancellationException) {
                    // Expected when the previous job was already cancelling
                }
                try {
                    _isProfileSwitch.value = true
                    _isGenerating.value = true
                    _errorMessage.value = null
                    awaitSyncComplete()
                    repository.deleteAll()
                    Log.d("RetroVM", "Deleted all retrospectives for profile-change regeneration")
                    val premium = isPremium()
                    val count = generateUseCase.generateMissing(isPremium = premium)
                    if (count == 0) {
                        Log.w("RetroVM", "Regeneration produced 0 reviews — AI may have failed")
                    } else {
                        Log.d("RetroVM", "Regenerated $count reviews with new profile style")
                    }
                    // Recalculate locked weeks (or clear for premium)
                    _lockedWeeks.value =
                        if (!premium) generateUseCase.getLockedWeekRanges() else emptyList()
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
        val previous = currentJob
        currentJob =
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    previous?.cancelAndJoin()
                } catch (_: kotlinx.coroutines.CancellationException) {
                    // Expected
                }
                try {
                    _isGenerating.value = true
                    awaitSyncComplete()
                    val premium = isPremium()
                    val count = generateUseCase.generateMissing(isPremium = premium)
                    Log.d("RetroVM", "Retry generated $count reviews")
                    // Recalculate locked weeks (or clear for premium)
                    _lockedWeeks.value =
                        if (!premium) generateUseCase.getLockedWeekRanges() else emptyList()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("RetroVM", "Retry failed: ${e.message}", e)
                    _errorMessage.value = e.message
                } finally {
                    _isGenerating.value = false
                }
            }
    }
}

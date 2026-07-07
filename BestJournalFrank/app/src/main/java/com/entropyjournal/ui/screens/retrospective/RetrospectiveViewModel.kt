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
    private val encryptedPrefs: android.content.SharedPreferences,
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
     * Returns "Letzte Aktualisierung am <timestamp>" for the newest retrospective summary,
     * or null if no summaries exist yet. Uses the same format as the Dashboard so both screens
     * stay visually consistent. Ported from BestJournalAndroid.
     */
    fun getLastUpdatedText(): String? {
        val newest =
            (weeklySummaries.value + monthlySummaries.value + yearlySummaries.value)
                .maxByOrNull { it.createdAt }
                ?: return null
        val sdf = java.text.SimpleDateFormat("dd.MM. 'um' HH:mm", java.util.Locale.GERMAN)
        return "Letzte Aktualisierung am ${sdf.format(java.util.Date(newest.createdAt))}"
    }

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
        // Step 1: Wait for PREF_RESTORE_PENDING flag to clear.
        // This flag is set BEFORE restore starts and cleared AFTER all photos are downloaded.
        // It survives app restarts (SharedPreferences), so the new instance sees it.
        val maxWaitMs = 10L * 60 * 1000 // 10 minutes max
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < maxWaitMs) {
            val restorePending =
                prefs.getBoolean(com.entropyjournal.util.Constants.PREF_RESTORE_PENDING, false)
            if (!restorePending) break
            Log.d("RetroVM", "Restore in progress, waiting...")
            kotlinx.coroutines.delay(3000)
        }

        // Step 2: Also verify all photos are actually on disk (belt-and-suspenders)
        val hasAccount =
            prefs.getString(com.entropyjournal.util.Constants.PREF_GOOGLE_ACCOUNT_EMAIL, null) !=
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

    // Track the last dashboard scenario + verbose-timestamp so we can regenerate
    // all retros whenever the user switches profile or toggles "Längere Version"
    // in Einstellungen. Initialized lazily after the first generation so we don't
    // trigger a regeneration on app start.
    private var lastScenario: Int = encryptedPrefs.getInt(com.entropyjournal.util.Constants.PREF_DASHBOARD_SCENARIO, 0)
    private var lastVerboseChangedAt: Long =
        encryptedPrefs.getLong(com.entropyjournal.util.Constants.PREF_VERBOSE_DASHBOARD_CHANGED_AT, 0L)
    // Track last custom-prompt save timestamp so a new "Individuelle Analyse" prompt
    // also regenerates every retrospective (weekly/monthly/yearly), not only the dashboard.
    private var lastCustomPromptSavedAt: Long =
        encryptedPrefs.getLong("custom_prompt_saved_at", 0L)

    // Single source of truth for ANY running retrospective work (init, regenerate,
    // retry). Every new launch waits for the previous one to fully terminate
    // (cancelAndJoin) before it calls deleteAll() + generateMissing(). Without this
    // serialization, a second trigger arriving while the first is still running in
    // parallel async-tasks would leave stale inserts racing the fresh deleteAll(),
    // producing duplicate weekly entries (e.g. "Woche 3" appearing multiple times).
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
                        flagFile.createNewFile()
                        Log.d("RetroVM", "One-time cleanup: cleared old retrospective data")
                    }

                    _isGenerating.value = true

                    // Signature-Check: vergleicht die aktuelle Profil+Variant-Kombi
                    // mit der zuletzt erfolgreich generierten. Stimmt es nicht, dann
                    // wurden waehrend die Rueckblicke geschlossen waren das Profil oder
                    // der Verbose-Schalter geaendert — der Polling-Loop hat das nicht
                    // mitbekommen, weil der ViewModel zwischenzeitlich nicht aktiv war.
                    // In diesem Fall: ALLE Rueckblicke loeschen und mit der neuen
                    // Signatur neu generieren — sonst sieht der User alte Texte aus
                    // dem vorherigen Profil oder die neuen ZUSAETZLICH zu den alten.
                    val currentSig = generateUseCase.currentSignature()
                    val storedSig =
                        encryptedPrefs.getString(
                            com.entropyjournal.util.Constants.PREF_RETRO_LAST_GENERATED_SIGNATURE,
                            null,
                        )
                    val signatureChanged = storedSig != null && storedSig != currentSig

                    if (signatureChanged) {
                        Log.d(
                            "RetroVM",
                            "Signature changed since last generation (stored=$storedSig, current=$currentSig) — regenerating all retrospectives",
                        )
                        _isProfileSwitch.value = true
                        try {
                            repository.deleteAll()
                            val count = generateUseCase.generateMissing()
                            generateUseCase.persistCurrentSignature()
                            // lastScenario/lastVerboseChangedAt auf den aktuellen Stand
                            // setzen, damit der Polling-Loop nicht direkt nochmal
                            // dasselbe macht.
                            lastScenario =
                                encryptedPrefs.getInt(
                                    com.entropyjournal.util.Constants.PREF_DASHBOARD_SCENARIO,
                                    0,
                                )
                            lastVerboseChangedAt =
                                encryptedPrefs.getLong(
                                    com.entropyjournal.util.Constants.PREF_VERBOSE_DASHBOARD_CHANGED_AT,
                                    0L,
                                )
                            lastCustomPromptSavedAt =
                                encryptedPrefs.getLong("custom_prompt_saved_at", 0L)
                            Log.d(
                                "RetroVM",
                                "Regenerated $count reviews after signature mismatch",
                            )
                        } finally {
                            _isProfileSwitch.value = false
                        }
                    } else {
                        val count = generateUseCase.generateMissing()
                        if (count > 0) {
                            Log.d("RetroVM", "Generated $count new reviews")
                        }
                        // Signature beim allerersten Start persistieren (storedSig == null).
                        if (storedSig == null) {
                            generateUseCase.persistCurrentSignature()
                        }
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

        // Watch for profile switches and verbose-toggle changes while on any
        // screen. When either flips we regenerate ALL existing retrospectives
        // so the user sees the new prompt style immediately, not only for new
        // future weeks/months/years.
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val currentScenario = encryptedPrefs.getInt(com.entropyjournal.util.Constants.PREF_DASHBOARD_SCENARIO, 0)
                    val currentVerboseChangedAt = encryptedPrefs.getLong(com.entropyjournal.util.Constants.PREF_VERBOSE_DASHBOARD_CHANGED_AT, 0L)
                    val currentCustomPromptSavedAt = encryptedPrefs.getLong("custom_prompt_saved_at", 0L)
                    val scenarioChanged = currentScenario != lastScenario
                    val verboseFlipped = currentVerboseChangedAt > lastVerboseChangedAt && currentVerboseChangedAt > 0L
                    val customPromptChanged =
                        currentCustomPromptSavedAt > lastCustomPromptSavedAt &&
                            currentCustomPromptSavedAt > 0L
                    if (scenarioChanged || verboseFlipped || customPromptChanged) {
                        lastScenario = currentScenario
                        lastVerboseChangedAt = currentVerboseChangedAt
                        lastCustomPromptSavedAt = currentCustomPromptSavedAt
                        Log.d(
                            "RetroVM",
                            "Trigger regenerateAll (scenario=$currentScenario verboseFlipped=$verboseFlipped customPromptChanged=$customPromptChanged)",
                        )
                        regenerateAll()
                    }
                } catch (e: Exception) {
                    Log.e("RetroVM", "Watcher error: ${e.message}")
                }
                kotlinx.coroutines.delay(500)
            }
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
                    val count = generateUseCase.generateMissing()
                    // Signatur des neuen Profil/Variant-Zustands persistieren,
                    // damit der naechste ViewModel-Start nicht erneut regeneriert.
                    generateUseCase.persistCurrentSignature()
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
                    val count = generateUseCase.generateMissing()
                    Log.d("RetroVM", "Retry generated $count reviews")
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

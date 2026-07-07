package com.bestjournal.app.ui.screens.consent

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.bestjournal.app.util.Constants
import com.bestjournal.app.util.PrivacyGateHelper
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Granular consent view model for the ConsentScreen.
 *
 * Per EDSA Guideline 03/2023 (Dark Patterns) and BGH Planet49, consent must be:
 *  - Specific per processing purpose (granular toggles, not a single accept-all)
 *  - Freely given (accept and reject paths equally prominent)
 *  - Informed (clear description per toggle)
 *  - Revocable as easily as granted (Art. 7 Abs. 3 DSGVO — mirrored in Settings)
 *  - Provable (timestamp + policy version persisted for audit trail)
 *
 * Toggle truth sources:
 *  - Analytics / Drive-Backup → SharedPreferences keys in [Constants]
 *  - Groq / Gemini / Edge TTS → [PrivacyGateHelper] keys (one source of truth; the
 *    per-use PrivacyGateDialog automatically skips when already consented here).
 *
 * All toggles default to OFF. Essential processing (local storage, crash logs for
 * data-loss prevention) is not gated and has no toggle.
 */
@HiltViewModel
class ConsentViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SharedPreferences,
    private val firebaseAnalytics: FirebaseAnalytics,
) : ViewModel() {

    private val _analyticsEnabled = MutableStateFlow(false)
    val analyticsEnabled: StateFlow<Boolean> = _analyticsEnabled.asStateFlow()

    private val _groqEnabled = MutableStateFlow(false)
    val groqEnabled: StateFlow<Boolean> = _groqEnabled.asStateFlow()

    private val _geminiEnabled = MutableStateFlow(false)
    val geminiEnabled: StateFlow<Boolean> = _geminiEnabled.asStateFlow()

    private val _ttsEnabled = MutableStateFlow(false)
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    private val _driveBackupEnabled = MutableStateFlow(false)
    val driveBackupEnabled: StateFlow<Boolean> = _driveBackupEnabled.asStateFlow()

    private val _doNotSellEnabled = MutableStateFlow(false)
    val doNotSellEnabled: StateFlow<Boolean> = _doNotSellEnabled.asStateFlow()

    fun setAnalytics(enabled: Boolean) {
        _analyticsEnabled.value = enabled
    }

    fun setGroq(enabled: Boolean) {
        _groqEnabled.value = enabled
    }

    fun setGemini(enabled: Boolean) {
        _geminiEnabled.value = enabled
    }

    fun setTts(enabled: Boolean) {
        _ttsEnabled.value = enabled
    }

    fun setDriveBackup(enabled: Boolean) {
        _driveBackupEnabled.value = enabled
    }

    /**
     * "Do Not Sell My Personal Information" (CCPA/CPRA 2026). When on, all optional
     * toggles flip off. This is the opt-out master switch for California users.
     */
    fun setDoNotSell(enabled: Boolean) {
        _doNotSellEnabled.value = enabled
        if (enabled) {
            _analyticsEnabled.value = false
            _groqEnabled.value = false
            _geminiEnabled.value = false
            _ttsEnabled.value = false
            _driveBackupEnabled.value = false
        }
    }

    /** One-tap accept: flip every optional toggle to on, then persist. */
    fun acceptAll() {
        _analyticsEnabled.value = true
        _groqEnabled.value = true
        _geminiEnabled.value = true
        _ttsEnabled.value = true
        _driveBackupEnabled.value = true
        _doNotSellEnabled.value = false
        persist()
    }

    /** One-tap reject: keep every optional toggle off, persist, continue. */
    fun acceptMinimumOnly() {
        _analyticsEnabled.value = false
        _groqEnabled.value = false
        _geminiEnabled.value = false
        _ttsEnabled.value = false
        _driveBackupEnabled.value = false
        persist()
    }

    /** Persist the current toggle state. */
    fun saveSelection() {
        persist()
    }

    private fun persist() {
        prefs.edit {
            putBoolean(Constants.PREF_CONSENT_SHOWN, true)
            putString(Constants.PREF_CONSENT_POLICY_VERSION, Constants.CURRENT_POLICY_VERSION)
            putLong(Constants.PREF_CONSENT_TIMESTAMP, System.currentTimeMillis())
            putBoolean(Constants.PREF_ANALYTICS_ENABLED, _analyticsEnabled.value)
            putBoolean(Constants.PREF_DRIVE_BACKUP_ENABLED, _driveBackupEnabled.value)
            putBoolean(Constants.PREF_DO_NOT_SELL, _doNotSellEnabled.value)
        }
        // Firebase Analytics is process-global: apply immediately so no event is
        // queued before the next app start.
        firebaseAnalytics.setAnalyticsCollectionEnabled(_analyticsEnabled.value)

        // Bridge the three cloud-AI toggles into PrivacyGateHelper so the per-use
        // AlertDialog (PrivacyGateDialog) knows they are pre-consented.
        PrivacyGateHelper.setConsent(
            context,
            PrivacyGateHelper.CloudService.Groq,
            _groqEnabled.value,
        )
        PrivacyGateHelper.setConsent(
            context,
            PrivacyGateHelper.CloudService.Gemini,
            _geminiEnabled.value,
        )
        PrivacyGateHelper.setConsent(
            context,
            PrivacyGateHelper.CloudService.EdgeTts,
            _ttsEnabled.value,
        )

        // Mirror the TTS consent into Settings → Töne & Haptik → Stimmen so a user
        // who ticked "Vorlesen" in the consent sheet finds the switch already on
        // with a locale-matching default voice pre-selected. Opposite direction:
        // if the consent toggle is off we also turn the settings switch off so
        // the two views stay in sync.
        try {
            val encPrefs = com.bestjournal.app.util.EncryptedPrefsProvider.get(context)
            val ttsOn = _ttsEnabled.value
            encPrefs.edit()
                .putBoolean(Constants.PREF_TTS_ENABLED, ttsOn)
                .apply()
            if (ttsOn &&
                encPrefs.getString(Constants.PREF_EDGE_TTS_VOICE, null).isNullOrBlank()
            ) {
                val defaultVoice = com.bestjournal.app.util.TtsVoiceRegistry
                    .getLocaleVoices()
                    .defaultVoiceId
                encPrefs.edit()
                    .putString(Constants.PREF_EDGE_TTS_VOICE, defaultVoice)
                    .apply()
            }
        } catch (_: Exception) {
            // EncryptedPrefsProvider very rarely fails on first-install edge cases;
            // silent fallback — the user can still flip the toggle manually later.
        }
    }
}

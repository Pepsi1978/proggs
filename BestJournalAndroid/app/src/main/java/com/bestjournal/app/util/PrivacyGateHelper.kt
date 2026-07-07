package com.bestjournal.app.util

import android.content.Context
import androidx.core.content.edit

/**
 * Persists per-service USA-transfer consents separately from the one-time
 * ConsentScreen decision. Each cloud service (Groq, Gemini, Edge TTS) is
 * gated by a first-use AlertDialog that the user must actively accept.
 *
 * Rationale: The pre-onboarding ConsentScreen obtains a global consent, but
 * EDSA Guideline 03/2023 and BGH Planet49 require specific, informed, and
 * granular consent before a concrete third-country transfer happens. This
 * helper bridges that gap without breaking the existing onboarding flow.
 *
 * All keys default to `false` until the user accepts once. A user can revoke
 * granularly in Settings → Privacy (not implemented here, pref keys exposed).
 */
object PrivacyGateHelper {

    private const val PREFS_NAME = "privacy_gate"
    const val KEY_GROQ_CONSENTED = "groq_usa_transfer_consented"
    const val KEY_GEMINI_CONSENTED = "gemini_usa_transfer_consented"
    const val KEY_EDGE_TTS_CONSENTED = "edge_tts_usa_transfer_consented"

    enum class CloudService(val prefKey: String) {
        Groq(KEY_GROQ_CONSENTED),
        Gemini(KEY_GEMINI_CONSENTED),
        EdgeTts(KEY_EDGE_TTS_CONSENTED),
    }

    fun hasConsented(context: Context, service: CloudService): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(service.prefKey, false)

    fun setConsent(context: Context, service: CloudService, consented: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(service.prefKey, consented) }
    }

    /** Revoke all per-service consents (called on account deletion). */
    fun revokeAll(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { clear() }
    }
}

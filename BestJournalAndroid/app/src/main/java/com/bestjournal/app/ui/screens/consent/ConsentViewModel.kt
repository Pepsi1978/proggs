package com.bestjournal.app.ui.screens.consent

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.bestjournal.app.util.Constants
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConsentViewModel
@Inject
constructor(
    private val prefs: SharedPreferences,
    private val firebaseAnalytics: FirebaseAnalytics,
) : ViewModel() {

    /**
     * User tapped "Loslegen" — full accept incl. anonymous analytics.
     * Persists both flags and activates Firebase Analytics collection immediately.
     */
    fun acceptAll() {
        prefs.edit {
            putBoolean(Constants.PREF_CONSENT_SHOWN, true)
            putBoolean(Constants.PREF_ANALYTICS_ENABLED, true)
        }
        firebaseAnalytics.setAnalyticsCollectionEnabled(true)
    }

    /**
     * User tapped "Statistiken deaktivieren" — proceeds without analytics.
     * Marks consent as shown (so we don't reprompt) but keeps analytics off.
     */
    fun acceptWithoutAnalytics() {
        prefs.edit {
            putBoolean(Constants.PREF_CONSENT_SHOWN, true)
            putBoolean(Constants.PREF_ANALYTICS_ENABLED, false)
        }
        firebaseAnalytics.setAnalyticsCollectionEnabled(false)
    }
}

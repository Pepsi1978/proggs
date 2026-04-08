package com.bestjournal.app.ui.screens.splash

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.bestjournal.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(private val prefs: SharedPreferences) : ViewModel() {

    fun isOnboardingCompleted(): Boolean =
        prefs.getBoolean(Constants.PREF_ONBOARDING_COMPLETED, false)
}

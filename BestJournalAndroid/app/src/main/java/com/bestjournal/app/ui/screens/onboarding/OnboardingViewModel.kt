package com.bestjournal.app.ui.screens.onboarding

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import com.bestjournal.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: SharedPreferences
) : ViewModel() {

    val isCompleted: Boolean
        get() = prefs.getBoolean(Constants.PREF_ONBOARDING_COMPLETED, false)

    private val _selectedGoals = MutableStateFlow(emptySet<String>())
    val selectedGoals: StateFlow<Set<String>> = _selectedGoals.asStateFlow()

    fun toggleGoal(goal: String) {
        _selectedGoals.value = if (goal in _selectedGoals.value) {
            _selectedGoals.value - goal
        } else {
            _selectedGoals.value + goal
        }
    }

    fun saveGoals() {
        val goals = _selectedGoals.value.joinToString(",")
        prefs.edit().putString(Constants.PREF_ONBOARDING_GOALS, goals).apply()
        trackEvent("onboarding_goals_selected", mapOf("goals" to goals))
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean(Constants.PREF_ONBOARDING_COMPLETED, true).apply()
        trackEvent("onboarding_completed")
    }

    fun trackEvent(name: String, params: Map<String, String> = emptyMap()) {
        val paramStr = if (params.isNotEmpty()) {
            " (${params.entries.joinToString(", ") { "${it.key}=${it.value}" }})"
        } else ""
        Log.d("OnboardingAnalytics", "Event: $name$paramStr")
        // TODO: Replace with Firebase Analytics when SDK is added
        // firebaseAnalytics.logEvent(name) { params.forEach { (k, v) -> param(k, v) } }
    }
}

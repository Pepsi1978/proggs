package com.bestjournal.app.ui.screens.onboarding

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.bestjournal.app.util.AnalyticsTracker
import com.bestjournal.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: SharedPreferences,
    val analyticsTracker: AnalyticsTracker,
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
        analyticsTracker.trackOnboardingGoalsSelected(goals)
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean(Constants.PREF_ONBOARDING_COMPLETED, true).apply()
        analyticsTracker.trackOnboardingCompleted()
    }
}

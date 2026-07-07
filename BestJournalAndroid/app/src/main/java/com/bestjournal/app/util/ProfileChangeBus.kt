package com.bestjournal.app.util

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide singleton bus that signals when the user has switched the dashboard scenario / profile.
 * SettingsScreen posts via [notifyProfileChanged]; RetrospectiveViewModel collects [version] and
 * regenerates all retros whenever the counter increments.
 *
 * Counter is used instead of a bool so every change is a distinct StateFlow emission.
 */
@Singleton
class ProfileChangeBus @Inject constructor() {
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version.asStateFlow()

    fun notifyProfileChanged() {
        _version.value = _version.value + 1
    }
}

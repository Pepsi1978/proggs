package de.frank.entropyreducer.presentation.widget

import android.content.Context
import android.content.res.Configuration
import de.frank.entropyreducer.data.settings.ThemeMode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object WidgetSystemThemeRefreshCoordinator {
    private val mutex = Mutex()
    private var lastRefreshedNightMode: Int? = null

    suspend fun refreshIfNeeded(context: Context, mode: ThemeMode): Boolean {
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val shouldRefresh = mutex.withLock {
            if (!shouldRefreshWidgetForSystemTheme(mode)) {
                lastRefreshedNightMode = null
                false
            } else if (!shouldRefreshWidgetForNightMode(mode, lastRefreshedNightMode, nightMode)) {
                false
            } else {
                lastRefreshedNightMode = nightMode
                true
            }
        }
        if (!shouldRefresh) return false

        return try {
            WidgetUpdater.updateAll(context)
            true
        } catch (error: Throwable) {
            mutex.withLock {
                if (lastRefreshedNightMode == nightMode) {
                    lastRefreshedNightMode = null
                }
            }
            throw error
        }
    }
}

internal fun shouldRefreshWidgetForSystemTheme(mode: ThemeMode): Boolean =
    mode == ThemeMode.SYSTEM

internal fun shouldRefreshWidgetForNightMode(
    mode: ThemeMode,
    lastRefreshedNightMode: Int?,
    currentNightMode: Int,
): Boolean =
    shouldRefreshWidgetForSystemTheme(mode) && lastRefreshedNightMode != currentNightMode

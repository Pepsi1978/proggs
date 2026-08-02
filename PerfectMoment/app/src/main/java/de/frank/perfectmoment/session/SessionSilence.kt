package de.frank.perfectmoment.session

import android.app.NotificationManager
import android.content.Context
import java.util.logging.Logger

/**
 * Keeps other apps' notification sounds out of a running session so that only the spoken
 * questions are audible. Alarms still ring, and the previous do-not-disturb state is restored
 * once the session ends.
 */
class SessionSilence(context: Context) {
    private val appContext = context.applicationContext
    private var previousFilter: Int? = null

    fun activate() {
        val manager = notificationManager() ?: return
        if (!manager.isNotificationPolicyAccessGranted) {
            logger.warning("Do-not-disturb access is missing, notification sounds stay audible")
            return
        }
        val current = manager.currentInterruptionFilter
        // A stricter filter the user picked themselves must never be loosened.
        if (current == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
            current == NotificationManager.INTERRUPTION_FILTER_NONE
        ) {
            return
        }
        try {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
            previousFilter = current
        } catch (error: SecurityException) {
            logger.warning("Do-not-disturb could not be enabled: ${error.message}")
        }
    }

    fun restore() {
        val previous = previousFilter ?: return
        previousFilter = null
        val manager = notificationManager() ?: return
        if (!manager.isNotificationPolicyAccessGranted) return
        // Whatever the user changed in the meantime stays untouched.
        if (manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALARMS) return
        try {
            manager.setInterruptionFilter(
                if (previous == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                } else {
                    previous
                },
            )
        } catch (error: SecurityException) {
            logger.warning("Do-not-disturb could not be restored: ${error.message}")
        }
    }

    private fun notificationManager(): NotificationManager? =
        appContext.getSystemService(NotificationManager::class.java)

    companion object {
        fun isAllowed(context: Context): Boolean =
            context.getSystemService(NotificationManager::class.java)
                ?.isNotificationPolicyAccessGranted == true

        private val logger = Logger.getLogger(SessionSilence::class.java.name)
    }
}

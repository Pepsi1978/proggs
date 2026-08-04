package de.frank.perfectmoment.session

import android.app.NotificationManager
import android.content.Context
import java.util.logging.Logger

/**
 * Keeps other apps' notification sounds out of a running session so that only the spoken
 * questions are audible. Alarms still ring, and the previous do-not-disturb state is restored
 * once the session ends.
 *
 * What was switched is written to disk, not just held in memory: a process that is killed — by
 * swiping the app away, by the system, or by a crash — never reaches `onDestroy`, and a note that
 * only lived in memory would leave do-not-disturb switched on forever. Because the note survives,
 * [restore] can undo it later from a fresh process, which is why it also runs at app start.
 */
class SessionSilence(context: Context) {
    private val appContext = context.applicationContext
    private val store = appContext.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)

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
            // Committed before anything else can go wrong, so the note is on disk even if the
            // process dies in the very next moment.
            store.edit().putInt(KEY_PREVIOUS_FILTER, current).commit()
        } catch (error: SecurityException) {
            logger.warning("Do-not-disturb could not be enabled: ${error.message}")
        }
    }

    /**
     * Undoes what [activate] switched. Does nothing when this app did not switch anything, so it
     * is safe to call at any time — including at app start, where it heals a session whose
     * process was killed before it could clean up.
     */
    fun restore() {
        val previous = store.getInt(KEY_PREVIOUS_FILTER, NOTHING_STORED)
        if (previous == NOTHING_STORED) return
        store.edit().remove(KEY_PREVIOUS_FILTER).commit()
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

        internal const val STORE_NAME = "session_silence"
        internal const val KEY_PREVIOUS_FILTER = "previous_interruption_filter"
        internal const val NOTHING_STORED = Int.MIN_VALUE
        private val logger = Logger.getLogger(SessionSilence::class.java.name)
    }
}

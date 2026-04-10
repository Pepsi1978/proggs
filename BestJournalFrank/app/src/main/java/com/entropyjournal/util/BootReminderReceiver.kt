package com.entropyjournal.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Re-schedules all alarms after device reboot, timezone change, or time adjustment. AlarmManager
 * alarms are stored as UTC millis — when the timezone changes, we must re-calculate and re-set them
 * so they fire at the correct LOCAL time.
 */
class BootReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val validActions =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
            )
        if (intent.action !in validActions) return

        Log.d("BootReminderReceiver", "Re-scheduling alarms (reason: ${intent.action})")

        try {
            val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val prefs =
                EncryptedSharedPreferences.create(
                    Constants.ENCRYPTED_PREFS_NAME,
                    masterKey,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )

            val manager = DailyReminderManager(context, prefs)
            manager.rescheduleIfEnabled()
            manager.ensureWeeklyReviewScheduled()
            manager.ensureMonthlyReviewScheduled()
            manager.ensureYearlyReviewScheduled()

            if (manager.isReminderEnabled()) {
                Log.d(
                    "BootReminderReceiver",
                    "Reminder re-scheduled for ${manager.getReminderHour()}:${"%02d".format(manager.getReminderMinute())}",
                )
            }
            if (manager.isWeeklyReviewEnabled()) {
                Log.d("BootReminderReceiver", "Weekly review re-scheduled")
            }
            if (manager.isMonthlyReviewEnabled()) {
                Log.d("BootReminderReceiver", "Monthly review re-scheduled")
            }
            if (manager.isYearlyReviewEnabled()) {
                Log.d("BootReminderReceiver", "Yearly review re-scheduled")
            }
        } catch (e: Exception) {
            Log.e("BootReminderReceiver", "Failed to re-schedule reminder", e)
        }
    }
}

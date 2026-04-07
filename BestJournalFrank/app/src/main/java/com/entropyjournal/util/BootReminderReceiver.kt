package com.entropyjournal.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class BootReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("BootReminderReceiver", "Device rebooted, checking reminder state")

        try {
            val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val prefs = EncryptedSharedPreferences.create(
                Constants.ENCRYPTED_PREFS_NAME,
                masterKey,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

            val manager = DailyReminderManager(context, prefs)
            manager.rescheduleIfEnabled()
            manager.ensureWeeklyReviewScheduled()

            if (manager.isReminderEnabled()) {
                Log.d("BootReminderReceiver",
                    "Reminder re-scheduled for ${manager.getReminderHour()}:${"%02d".format(manager.getReminderMinute())}")
            }
            if (manager.isWeeklyReviewEnabled()) {
                Log.d("BootReminderReceiver", "Weekly review re-scheduled")
            }
        } catch (e: Exception) {
            Log.e("BootReminderReceiver", "Failed to re-schedule reminder", e)
        }
    }
}

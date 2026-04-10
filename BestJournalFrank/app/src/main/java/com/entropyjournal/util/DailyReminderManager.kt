package com.entropyjournal.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import java.util.Calendar

class DailyReminderManager(private val context: Context, private val prefs: SharedPreferences) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(hour: Int, minute: Int) {
        prefs
            .edit()
            .putBoolean(Constants.PREF_REMINDER_ENABLED, true)
            .putInt(Constants.PREF_REMINDER_HOUR, hour)
            .putInt(Constants.PREF_REMINDER_MINUTE, minute)
            .apply()

        try {
            val calendar =
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                createPendingIntent(),
            )
        } catch (e: Exception) {
            android.util.Log.e("ReminderManager", "Failed to schedule daily reminder", e)
        }
    }

    fun cancelReminder() {
        prefs.edit().putBoolean(Constants.PREF_REMINDER_ENABLED, false).apply()
        alarmManager.cancel(createPendingIntent())
    }

    fun isReminderEnabled(): Boolean = prefs.getBoolean(Constants.PREF_REMINDER_ENABLED, false)

    fun getReminderHour(): Int = prefs.getInt(Constants.PREF_REMINDER_HOUR, 20)

    fun getReminderMinute(): Int = prefs.getInt(Constants.PREF_REMINDER_MINUTE, 0)

    fun rescheduleIfEnabled() {
        if (isReminderEnabled()) {
            scheduleReminder(getReminderHour(), getReminderMinute())
        }
    }

    fun scheduleWeeklyReview(
        dayOfWeek: Int = getWeeklyReviewDay(),
        hour: Int = getWeeklyReviewHour(),
        minute: Int = getWeeklyReviewMinute(),
        fromUserToggle: Boolean = true,
    ) {
        if (fromUserToggle) {
            prefs
                .edit()
                .putBoolean(Constants.PREF_WEEKLY_REVIEW_ENABLED, true)
                .putInt(Constants.PREF_WEEKLY_REVIEW_DAY, dayOfWeek)
                .putInt(Constants.PREF_WEEKLY_REVIEW_HOUR, hour)
                .putInt(Constants.PREF_WEEKLY_REVIEW_MINUTE, minute)
                .apply()
        }

        try {
            val calendar =
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, dayOfWeek)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(Calendar.getInstance())) {
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7,
                createWeeklyReviewPendingIntent(),
            )
        } catch (e: Exception) {
            android.util.Log.e("ReminderManager", "Failed to schedule weekly review", e)
        }
    }

    fun cancelWeeklyReview() {
        prefs.edit().putBoolean(Constants.PREF_WEEKLY_REVIEW_ENABLED, false).apply()
        alarmManager.cancel(createWeeklyReviewPendingIntent())
    }

    fun isWeeklyReviewEnabled(): Boolean =
        prefs.getBoolean(Constants.PREF_WEEKLY_REVIEW_ENABLED, true)

    fun getWeeklyReviewDay(): Int = prefs.getInt(Constants.PREF_WEEKLY_REVIEW_DAY, Calendar.SUNDAY)

    fun getWeeklyReviewHour(): Int = prefs.getInt(Constants.PREF_WEEKLY_REVIEW_HOUR, 15)

    fun getWeeklyReviewMinute(): Int = prefs.getInt(Constants.PREF_WEEKLY_REVIEW_MINUTE, 0)

    fun ensureWeeklyReviewScheduled() {
        if (isWeeklyReviewEnabled()) {
            scheduleWeeklyReview(fromUserToggle = false)
        }
    }

    // ── Monthly Review ──────────────────────────────────────────────────

    fun scheduleMonthlyReview(fromUserToggle: Boolean = true) {
        if (fromUserToggle) {
            prefs.edit().putBoolean(Constants.PREF_MONTHLY_REVIEW_ENABLED, true).apply()
        }
        try {
            val target =
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 15)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (!after(Calendar.getInstance())) {
                        add(Calendar.MONTH, 1)
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    }
                }
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                target.timeInMillis,
                createMonthlyReviewPendingIntent(),
            )
        } catch (e: Exception) {
            android.util.Log.e("ReminderManager", "Failed to schedule monthly review", e)
        }
    }

    fun cancelMonthlyReview() {
        prefs.edit().putBoolean(Constants.PREF_MONTHLY_REVIEW_ENABLED, false).apply()
        try {
            alarmManager.cancel(createMonthlyReviewPendingIntent())
        } catch (_: Exception) {}
    }

    fun isMonthlyReviewEnabled(): Boolean =
        prefs.getBoolean(Constants.PREF_MONTHLY_REVIEW_ENABLED, true)

    fun ensureMonthlyReviewScheduled() {
        if (isMonthlyReviewEnabled()) scheduleMonthlyReview(fromUserToggle = false)
    }

    // ── Yearly Review ───────────────────────────────────────────────────

    fun scheduleYearlyReview(fromUserToggle: Boolean = true) {
        if (fromUserToggle) {
            prefs.edit().putBoolean(Constants.PREF_YEARLY_REVIEW_ENABLED, true).apply()
        }
        try {
            val target =
                Calendar.getInstance().apply {
                    set(Calendar.MONTH, Calendar.DECEMBER)
                    set(Calendar.DAY_OF_MONTH, 31)
                    set(Calendar.HOUR_OF_DAY, 15)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (!after(Calendar.getInstance())) {
                        add(Calendar.YEAR, 1)
                    }
                }
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                target.timeInMillis,
                createYearlyReviewPendingIntent(),
            )
        } catch (e: Exception) {
            android.util.Log.e("ReminderManager", "Failed to schedule yearly review", e)
        }
    }

    fun cancelYearlyReview() {
        prefs.edit().putBoolean(Constants.PREF_YEARLY_REVIEW_ENABLED, false).apply()
        try {
            alarmManager.cancel(createYearlyReviewPendingIntent())
        } catch (_: Exception) {}
    }

    fun isYearlyReviewEnabled(): Boolean =
        prefs.getBoolean(Constants.PREF_YEARLY_REVIEW_ENABLED, true)

    fun ensureYearlyReviewScheduled() {
        if (isYearlyReviewEnabled()) scheduleYearlyReview(fromUserToggle = false)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createWeeklyReviewPendingIntent(): PendingIntent {
        val intent = Intent(context, WeeklyReviewReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            WEEKLY_REVIEW_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createMonthlyReviewPendingIntent(): PendingIntent {
        val intent = Intent(context, MonthlyReviewReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            MONTHLY_REVIEW_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createYearlyReviewPendingIntent(): PendingIntent {
        val intent = Intent(context, YearlyReviewReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            YEARLY_REVIEW_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val REQUEST_CODE = 1001
        private const val WEEKLY_REVIEW_REQUEST_CODE = 1002
        private const val MONTHLY_REVIEW_REQUEST_CODE = 1003
        private const val YEARLY_REVIEW_REQUEST_CODE = 1004
    }
}

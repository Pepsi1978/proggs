package de.frank.fisetinbegleiter.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import de.frank.fisetinbegleiter.data.CureDayEntity
import de.frank.fisetinbegleiter.data.CureEntity
import de.frank.fisetinbegleiter.data.ProtocolTemplateEntity
import java.time.LocalDate
import java.time.ZoneId

enum class AlarmType {
    MEAL_WARNING,
    SPERMIDIN_OPEN,
    SPERMIDIN_REMINDER,
    BLOCK_ENDED,
    NEXT_DAY,
    CURE_ENDED,
}

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        alarmManager.canScheduleExactAlarms()

    fun exactAlarmSettingsIntent(): Intent {
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun batterySettingsIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun scheduleDay(cure: CureEntity, day: CureDayEntity, protocol: ProtocolTemplateEntity, t0: Long) {
        if (!protocol.remindersEnabled) return
        schedule(day.id, AlarmType.MEAL_WARNING, t0 + protocol.mealWarningMinutes * 60_000L)
        schedule(day.id, AlarmType.SPERMIDIN_OPEN, t0 + protocol.spermidinStartMinutes * 60_000L)
        schedule(day.id, AlarmType.SPERMIDIN_REMINDER, t0 + protocol.spermidinReminderMinutes * 60_000L)
        schedule(day.id, AlarmType.BLOCK_ENDED, t0 + protocol.antioxidantBlockMinutes * 60_000L)

        if (day.dayNumber < cure.plannedDurationDays) {
            val nextDate = LocalDate.ofEpochDay(day.plannedEpochDay).plusDays(1)
            val nextStart = nextDate.atStartOfDay()
                .plusMinutes(cure.preferredStartMinuteOfDay.toLong())
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            schedule(day.id, AlarmType.NEXT_DAY, nextStart, day.dayNumber + 1)
        }
    }

    fun cancelMeal(dayId: Long) = cancel(dayId, AlarmType.MEAL_WARNING)

    fun cancelSpermidin(dayId: Long) {
        cancel(dayId, AlarmType.SPERMIDIN_OPEN)
        cancel(dayId, AlarmType.SPERMIDIN_REMINDER)
    }

    private fun schedule(dayId: Long, type: AlarmType, triggerAt: Long, targetDay: Int = 0) {
        if (triggerAt <= System.currentTimeMillis()) return
        val pendingIntent = alarmPendingIntent(dayId, type, targetDay, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun cancel(dayId: Long, type: AlarmType) {
        val pendingIntent = alarmPendingIntent(dayId, type, 0, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun alarmPendingIntent(
        dayId: Long,
        type: AlarmType,
        targetDay: Int,
        flag: Int,
    ): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_DAY_ID, dayId)
            putExtra(AlarmReceiver.EXTRA_TYPE, type.name)
            putExtra(AlarmReceiver.EXTRA_TARGET_DAY, targetDay)
        }
        val requestCode = ((dayId % 100_000) * 10 + type.ordinal).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flag or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

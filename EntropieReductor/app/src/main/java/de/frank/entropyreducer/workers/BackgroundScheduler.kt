package de.frank.entropyreducer.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plant Periodische und einmalige Hintergrund-Sync-Jobs (Calendar + Whoop).
 * Spec §15.4 + §15.5 — beide nightly um 04:30 (ausserhalb beider Schichten),
 * plus on-demand nach Sign-In oder bei Settings-Aktion.
 */
@Singleton
class BackgroundScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val wm = WorkManager.getInstance(context)

    /** Plant Calendar + Whoop nightly um 04:30 lokaler Zeit. */
    fun ensureNightlyJobs() {
        val targetMinutes = 4 * 60 + 30 // 04:30
        val initialDelayMinutes = minutesUntil(targetMinutes)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        wm.enqueueUniquePeriodicWork(
            CalendarSyncWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<CalendarSyncWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build(),
        )
        wm.enqueueUniquePeriodicWork(
            WhoopSyncWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<WhoopSyncWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build(),
        )
    }

    /** Plant die KI-Frage-des-Moments-Pruefung alle 30 Minuten. */
    fun ensureKiQuestionJob() {
        wm.enqueueUniquePeriodicWork(
            KiQuestionWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<KiQuestionWorker>(30, TimeUnit.MINUTES)
                .setInitialDelay(2, TimeUnit.MINUTES)
                .build(),
        )
    }

    fun runKiQuestionCheckNow() {
        wm.enqueueUniqueWork(
            KiQuestionWorker.UNIQUE_NAME_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<KiQuestionWorker>().build(),
        )
    }

    /** Stoesst einen einmaligen Calendar-Sync an (z.B. nach Sign-In). */
    fun runCalendarSyncNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        wm.enqueueUniqueWork(
            CalendarSyncWorker.UNIQUE_NAME_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CalendarSyncWorker>()
                .setConstraints(constraints).build(),
        )
    }

    fun runWhoopSyncNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        wm.enqueueUniqueWork(
            WhoopSyncWorker.UNIQUE_NAME_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WhoopSyncWorker>()
                .setConstraints(constraints).build(),
        )
    }

    /** Bricht den Calendar-Sync ab — z.B. wenn der Nutzer sich abmeldet. */
    fun cancelCalendarSync() {
        wm.cancelUniqueWork(CalendarSyncWorker.UNIQUE_NAME_PERIODIC)
        wm.cancelUniqueWork(CalendarSyncWorker.UNIQUE_NAME_ONESHOT)
    }

    fun cancelWhoopSync() {
        wm.cancelUniqueWork(WhoopSyncWorker.UNIQUE_NAME_PERIODIC)
        wm.cancelUniqueWork(WhoopSyncWorker.UNIQUE_NAME_ONESHOT)
    }

    /** Berechnet die Anzahl Minuten bis zum naechsten Vorkommen von [targetMinutes] (Tagesminuten). */
    private fun minutesUntil(targetMinutes: Int): Long {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val target = now.toLocalDate().atTime(LocalTime.of(targetMinutes / 60, targetMinutes % 60))
        val resolved = if (target.isBefore(now)) target.plusDays(1) else target
        val nowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val resolvedMillis = resolved.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return ((resolvedMillis - nowMillis) / 60_000L).coerceAtLeast(1L)
    }
}

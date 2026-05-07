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
import java.time.DayOfWeek
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

    /** Plant die Genie-Codex-Synthese sonntags 19:00 lokaler Zeit (Spec §16.5). */
    fun ensureCodexJob() {
        val initialDelayMinutes = minutesUntilNextWeekday(DayOfWeek.SUNDAY, 19 * 60)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        wm.enqueueUniquePeriodicWork(
            GenieCodexWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<GenieCodexWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build(),
        )
    }

    /** Stoesst eine Codex-Synthese sofort an (Manueller "Jetzt aktualisieren"-Button). */
    fun runCodexNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        wm.enqueueUniqueWork(
            GenieCodexWorker.UNIQUE_NAME_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<GenieCodexWorker>().setConstraints(constraints).build(),
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

    /**
     * Plant das Tagesbriefing als Polling-Worker alle 90 Minuten. Worker selbst
     * entscheidet anhand `dailyBriefingDate`, ob heute schon eines gebaut wurde —
     * so deckt eine einzelne Schedule-Konfiguration alle Schichtfenster ab.
     */
    fun ensureDailyBriefingJob() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        wm.enqueueUniquePeriodicWork(
            DailyBriefingWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<DailyBriefingWorker>(90, TimeUnit.MINUTES)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build(),
        )
    }

    fun runDailyBriefingNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        wm.enqueueUniqueWork(
            DailyBriefingWorker.UNIQUE_NAME_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DailyBriefingWorker>().setConstraints(constraints).build(),
        )
    }

    /** Plant Wochenrueckblick (sonntags 19:00) + Monatsrueckblick (1. des Monats 19:00). */
    fun ensureReviewJobs() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        // Wochenrueckblick — Sonntag 19:00
        val weeklyDelay = minutesUntilNextWeekday(DayOfWeek.SUNDAY, 19 * 60)
        wm.enqueueUniquePeriodicWork(
            WeeklyReviewWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<WeeklyReviewWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(weeklyDelay, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build(),
        )
        // Monatsrueckblick — alle 24h pruefen, Worker entscheidet selbst (1. des Monats)
        wm.enqueueUniquePeriodicWork(
            MonthlyReviewWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<MonthlyReviewWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(minutesUntil(19 * 60), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build(),
        )
    }

    fun runWeeklyReviewNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        wm.enqueueUniqueWork(
            WeeklyReviewWorker.UNIQUE_NAME_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WeeklyReviewWorker>().setConstraints(constraints).build(),
        )
    }

    fun runMonthlyReviewNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        wm.enqueueUniqueWork(
            MonthlyReviewWorker.UNIQUE_NAME_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MonthlyReviewWorker>().setConstraints(constraints).build(),
        )
    }

    /**
     * Plant die Korrelations-Engine (taeglich 03:30) und die KI-Trigger-Engine
     * (Mittwoch + Sonntag 11:00). Spec §16.1 + §16.2.
     */
    fun ensureCorrelationAndTriggerJobs() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        wm.enqueueUniquePeriodicWork(
            CorrelationWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<CorrelationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(minutesUntil(3 * 60 + 30), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build(),
        )
        // KI-Trigger-Engine: Worker laeuft taeglich 11:00, prueft selbst Mi/So.
        wm.enqueueUniquePeriodicWork(
            KiTriggerWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<KiTriggerWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(minutesUntil(11 * 60), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build(),
        )
        // Trigger-Polling — alle 15 Min Bedingungen aktiver Trigger pruefen.
        wm.enqueueUniquePeriodicWork(
            TriggerPollingWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<TriggerPollingWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(7, TimeUnit.MINUTES)
                .build(),
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

    /** Anzahl Minuten bis zum naechsten Vorkommen des angegebenen Wochentags + Tagesminute. */
    private fun minutesUntilNextWeekday(dayOfWeek: DayOfWeek, targetMinutes: Int): Long {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val targetTime = LocalTime.of(targetMinutes / 60, targetMinutes % 60)
        var date = now.toLocalDate()
        while (date.dayOfWeek != dayOfWeek) date = date.plusDays(1)
        var target = date.atTime(targetTime)
        if (!target.isAfter(now)) target = target.plusWeeks(1)
        val nowMs = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val targetMs = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return ((targetMs - nowMs) / 60_000L).coerceAtLeast(1L)
    }
}

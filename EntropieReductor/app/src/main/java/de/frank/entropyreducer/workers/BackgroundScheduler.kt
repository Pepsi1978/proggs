package de.frank.entropyreducer.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.di.ApplicationScope
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Plant Periodische und einmalige Hintergrund-Sync-Jobs (Calendar + Whoop).
 * Spec §15.4 + §15.5 — beide nightly um 04:30 (ausserhalb beider Schichten),
 * plus on-demand nach Sign-In oder bei Settings-Aktion.
 */
@Singleton
class BackgroundScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettings: AppSettings,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    private val wm = WorkManager.getInstance(context)

    /** Plant Calendar + Whoop nightly um 04:30 lokaler Zeit. */
    fun ensureNightlyJobs() {
        // Frank-Wunsch 2026-05-23: KEINE naechtlichen Hintergrund-Syncs mehr fuer Whoop und
        // Kalender. Beide synchronisieren jetzt NUR noch beim frischen App-Start (zentral im
        // StartupViewModel) und beim manuellen Aktualisieren-Knopf. Daten-Syncs sollen nur
        // laufen wenn die App wirklich geoeffnet ist.
        //
        // Die frueher angelegten periodischen 24h-Jobs werden hier AKTIV ABBESTELLT —
        // WorkManager speichert periodische Jobs persistent, sie wuerden sonst auf Geraeten
        // mit alter App-Version (oder nach diesem Update) weiterlaufen.
        wm.cancelUniqueWork(CalendarSyncWorker.UNIQUE_NAME_PERIODIC)
        wm.cancelUniqueWork(WhoopSyncWorker.UNIQUE_NAME_PERIODIC)
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

    /** Plant die KI-Frage-des-Moments-Pruefung alle 30 Minuten.
     *  Performance-Audit Loop 1 (2026-05-10): BatteryNotLow-Constraint hinzu —
     *  KI-Fragen sind nicht zeitkritisch, sollten bei <15% Akku pausieren. */
    fun ensureKiQuestionJob() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        wm.enqueueUniquePeriodicWork(
            KiQuestionWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<KiQuestionWorker>(30, TimeUnit.MINUTES)
                .setInitialDelay(2, TimeUnit.MINUTES)
                .setConstraints(constraints)
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
        // Performance-Audit Loop 1 (2026-05-10): BatteryNotLow ergaenzt —
        // Briefing kann 90 min warten bis Akku >15% ist (Ladegeraet).
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
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
            OneTimeWorkRequestBuilder<DailyBriefingWorker>()
                .setInputData(androidx.work.workDataOf(DailyBriefingWorker.KEY_FORCE to true))
                .setConstraints(constraints)
                .build(),
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
        // Monatsrueckblick — alle 24h prüfen, Worker entscheidet selbst (1. des Monats)
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
            OneTimeWorkRequestBuilder<WeeklyReviewWorker>()
                .setInputData(androidx.work.workDataOf(WeeklyReviewWorker.KEY_FORCE to true))
                .setConstraints(constraints)
                .build(),
        )
    }

    fun runMonthlyReviewNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        wm.enqueueUniqueWork(
            MonthlyReviewWorker.UNIQUE_NAME_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MonthlyReviewWorker>()
                .setInputData(androidx.work.workDataOf(MonthlyReviewWorker.KEY_FORCE to true))
                .setConstraints(constraints)
                .build(),
        )
    }

    /**
     * Stoesst die KI-Trigger-Engine sofort an (manueller "Trigger jetzt erzeugen"-Button).
     * Setzt `force=true` damit der Worker den Wochentags-Check (Mi/So) ueberspringt —
     * sonst würde ein manueller Aufruf an einem Donnerstag ohne Effekt durchlaufen.
     */
    fun runKiTriggerNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        wm.enqueueUniqueWork(
            KiTriggerWorker.UNIQUE_NAME_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<KiTriggerWorker>()
                .setInputData(androidx.work.workDataOf(KiTriggerWorker.KEY_FORCE to true))
                .setConstraints(constraints)
                .build(),
        )
    }

    /**
     * Plant die Korrelations-Engine (täglich 03:30) und die KI-Trigger-Engine
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
        // KI-Trigger-Engine: Worker laeuft täglich 11:00, prueft selbst Mi/So.
        wm.enqueueUniquePeriodicWork(
            KiTriggerWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<KiTriggerWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(minutesUntil(11 * 60), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build(),
        )
        // Trigger-Polling — alle 15 Min Bedingungen aktiver Trigger prüfen.
        // Performance-Audit Loop 1 (2026-05-10): BatteryNotLow ergaenzt —
        // Trigger-Polling ist nicht zeitkritisch und nur lokal (kein Netz),
        // bei <15% Akku duerfen Vorschlaege spaeter erscheinen.
        val triggerPollingConstraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        wm.enqueueUniquePeriodicWork(
            TriggerPollingWorker.UNIQUE_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<TriggerPollingWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(7, TimeUnit.MINUTES)
                .setConstraints(triggerPollingConstraints)
                .build(),
        )
    }

    /** Stoesst einen einmaligen Calendar-Sync an (z.B. nach Sign-In oder App-Start).
     *  KEEP statt REPLACE: wenn StartupViewModel und ProcessLifecycleObserver beide
     *  beim ersten App-Start triggern, gewinnt der erste — der zweite wird ohne
     *  Cancellation ignoriert. WorkManager 10-Min-Stop-Timeout fangt haengende
     *  Syncs ohnehin ab. */
    fun runCalendarSyncNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        wm.enqueueUniqueWork(
            CalendarSyncWorker.UNIQUE_NAME_ONESHOT,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<CalendarSyncWorker>()
                .setConstraints(constraints).build(),
        )
    }

    /** Stoesst einen einmaligen Whoop-Sync an. KEEP statt REPLACE — siehe
     *  runCalendarSyncNow. Verhindert die Doppel-Trigger-Race zwischen
     *  StartupViewModel.init und ProcessLifecycleObserver.onStart, die in
     *  Loop-1-Logcat zu 'Worker A startet → cancelled → Worker B startet'
     *  gefuehrt hat (Token-Refresh + 3 API-Calls verschwendet). */
    fun runWhoopSyncNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        wm.enqueueUniqueWork(
            WhoopSyncWorker.UNIQUE_NAME_ONESHOT,
            ExistingWorkPolicy.KEEP,
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

    // runAmazfitSyncNow + cancelAmazfitSync entfernt 2026-05-17 (Frank-Wunsch):
    // Zepp-Cloud-API komplett raus. Workouts kommen ueber Health-Connect-Sync
    // (BiomarkerViewModel.refreshNow), Daily-Werte ueber Health Connect.

    // runPolarSyncNow + cancelPolarSync entfernt 2026-05-17 (Frank-Wunsch).
    // Polar-Live-API gibt es nicht mehr — nur noch Polar-ZIP-Bulk-Import
    // (siehe runPolarBulkImport / cancelPolarBulkImport unten).

    /**
     * Bricht alle laufenden oder enqueued Polar-Bulk-Imports ab. Wichtig
     * vor einem neuen Start, weil WorkManager Worker auch ueber App-Updates
     * persistiert — ein alter Worker mit gescheiterter Drive-URI koennte
     * sonst gleichzeitig mit dem neuen file://-URI laufen und Fehler-
     * Notifications produzieren.
     */
    fun cancelPolarBulkImport() {
        wm.cancelUniqueWork(PolarBulkImportWorker.UNIQUE_NAME_ONESHOT)
    }

    /**
     * Stoesst den Polar-Bulk-Import an (Frank-Wunsch 2026-05-16).
     * zipUri ist die content:// URI die der File-Picker liefert.
     * REPLACE: ein laufender Import wird ersetzt, kein Doppel-Import.
     */
    fun runPolarBulkImport(zipUri: android.net.Uri) {
        // KEIN Netzwerk-Constraint — die ZIP liegt schon lokal auf dem Geraet.
        // Nur Sicherheit: bei niedrigem Akku abwarten waere unschoen, weil
        // der User aktiv "Importieren" gedrueckt hat. Also OHNE Battery-Check.
        val request = OneTimeWorkRequestBuilder<PolarBulkImportWorker>()
            .setInputData(
                androidx.work.workDataOf(
                    PolarBulkImportWorker.KEY_ZIP_URI to zipUri.toString(),
                ),
            )
            // Foreground-Worker brauchen Expedited-Hint um sofort zu starten
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        wm.enqueueUniqueWork(
            PolarBulkImportWorker.UNIQUE_NAME_ONESHOT,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    companion object {
        private const val TAG = "BackgroundScheduler"
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

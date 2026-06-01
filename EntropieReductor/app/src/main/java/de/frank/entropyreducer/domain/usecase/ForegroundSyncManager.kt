package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.repository.AmazfitRepository
import de.frank.entropyreducer.data.repository.CalendarRepository
import de.frank.entropyreducer.data.repository.HealthConnectRepository
import de.frank.entropyreducer.data.repository.OuraRepository
import de.frank.entropyreducer.data.repository.WhoopRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex

/**
 * Zentraler Daten-Sync (Frank-Wunsch 2026-06-01). Eine EINZIGE Quelle der Wahrheit fuer
 * den kompletten Datenabgleich aller Quellen — vorher lag die Logik inline im
 * StartupViewModel und konnte deshalb NUR beim frischen App-Start laufen.
 *
 * Root Cause des Sync-Bugs: Der Start-Sync lief ausschliesslich in StartupViewModel.init
 * (einmal pro Prozess). Lag die App ueber Nacht nur im Hintergrund/Arbeitsspeicher, wurde
 * beim Zurueckholen WEDER ein neuer Prozess gestartet (StartupViewModel.init feuert nicht)
 * NOCH ein Lifecycle-Sync ausgeloest — der fruehere ProcessLifecycle-Observer war am
 * 2026-05-23 entfernt worden, weil er bei JEDEM Foreground-Wechsel synchronisierte
 * (Datenverbrauch/Rate-Limit). Folge: "Zuletzt synchronisiert" blieb auf dem Stand vom
 * Vorabend, obwohl laengst neue Biomarker-Daten vorlagen.
 *
 * Loesung: [syncIfStale] wird vom ProcessLifecycle-ON_START-Observer bei jedem
 * Foreground-Wechsel aufgerufen, synchronisiert aber NUR wenn seit dem letzten Sync
 * mindestens das uebergebene Intervall (Standard 8 h) vergangen ist. Damit ist der
 * Spam-Fall (haeufiges Wechseln) ausgeschlossen und der Ueber-Nacht-Fall abgedeckt.
 */
@Singleton
class ForegroundSyncManager
@Inject
constructor(
    private val secrets: EncryptedSecretsStore,
    private val syncEntries: SyncEntriesUseCase,
    private val coordinator: SyncCoordinator,
    private val oauth: OAuthService,
    private val whoop: WhoopRepository,
    private val oura: OuraRepository,
    private val amazfit: AmazfitRepository,
    private val calendar: CalendarRepository,
    private val healthConnectRepository: HealthConnectRepository,
    private val appSettings: AppSettings,
) {
    // Verhindert dass zwei Sync-Laeufe gleichzeitig starten (z.B. frischer Start +
    // Foreground-Event kurz hintereinander). Wer den Lock nicht bekommt, ueberspringt.
    private val mutex = Mutex()

    /**
     * Synchronisiert nur dann, wenn seit dem letzten erfolgreichen Sync mindestens
     * [minIntervalMs] vergangen sind. Wird beim Foreground-Wechsel aufgerufen.
     * Wenn noch nie gesynced wurde (Zeitstempel 0), wird synchronisiert.
     */
    suspend fun syncIfStale(minIntervalMs: Long) {
        val lastMs = appSettings.lastRefreshFooterAtMsFlow.first()
        val now = System.currentTimeMillis()
        if (lastMs > 0L && now - lastMs < minIntervalMs) {
            // Noch frisch genug — kein Sync. Spart Datenverbrauch und Rate-Limit.
            return
        }
        syncAllSources()
    }

    /**
     * Voller Datenabgleich aller Quellen. 1:1 der Ablauf der frueher inline im
     * StartupViewModel lag — wird jetzt von dort UND vom Foreground-Observer genutzt.
     *
     * Reihenfolge bewusst: ZUERST Google Drive komplett (Restore holt die Historie
     * zurueck, dann Backup sichert neue lokale Aenderungen) — beides wird abgewartet.
     * ERST DANACH die einzelnen Daten-APIs (parallel, je eigene Fehlerbehandlung).
     * Am Ende wird der "Zuletzt synchronisiert"-Footer gestempelt.
     *
     * Bei parallelem Aufruf (Lock belegt) kehrt die Methode sofort zurueck.
     */
    suspend fun syncAllSources() {
        if (!mutex.tryLock()) return
        try {
            if (secrets.driveBackupEnabled && secrets.driveAccountEmail != null) {
                runCatching { syncEntries.restoreFromDrive() }
                runCatching { coordinator.syncNowAndWait() }
            }

            var hcCount = 0
            var whoopCount = 0
            var ouraCount = 0
            var stravaCount = 0
            coroutineScope {
                val hcDeferred =
                    async { runCatching { healthConnectRepository.syncToCache() }.getOrDefault(0) }
                val whoopDeferred =
                    async {
                        if (oauth.loadWhoopAuthState().isAuthorized) {
                            whoop.syncLastDays(365).getOrDefault(0)
                        } else {
                            0
                        }
                    }
                val ouraDeferred =
                    async {
                        if (oura.isTokenConfigured()) {
                            oura.syncLastDays(365).getOrNull()?.values?.sum() ?: 0
                        } else {
                            0
                        }
                    }
                val stravaDeferred =
                    async {
                        runCatching { amazfit.mergeFromStrava(days = 30) }
                            .getOrDefault(-1)
                            .coerceAtLeast(0)
                    }
                val calendarDeferred =
                    async {
                        if (secrets.calendarAccountEmail != null) {
                            runCatching { calendar.syncDefaultWindow() }
                        }
                    }
                hcCount = hcDeferred.await()
                whoopCount = whoopDeferred.await()
                ouraCount = ouraDeferred.await()
                stravaCount = stravaDeferred.await()
                calendarDeferred.await()
            }

            // "Zuletzt synchronisiert"-Footer setzen (gleiches Format wie der manuelle
            // Aktualisieren-Knopf). Der Zeitstempel dient gleichzeitig als Grundlage
            // fuer den 8h-Throttle in syncIfStale().
            val nowZ = java.time.ZonedDateTime.now()
            val footerTs =
                "%02d.%02d. %02d:%02d".format(
                    nowZ.dayOfMonth,
                    nowZ.monthValue,
                    nowZ.hour,
                    nowZ.minute,
                )
            val footer =
                "✓ $footerTs · Whoop $whoopCount · Strava $stravaCount · " +
                    "Oura $ouraCount · Health Connect $hcCount"
            runCatching { appSettings.setLastRefreshFooter(footer, System.currentTimeMillis()) }
        } finally {
            mutex.unlock()
        }
    }

    companion object {
        /** Standard-Throttle fuer den Foreground-Sync: 8 Stunden (Frank-Wunsch 2026-06-01). */
        const val FOREGROUND_SYNC_MIN_INTERVAL_MS = 8L * 60L * 60L * 1000L
    }
}

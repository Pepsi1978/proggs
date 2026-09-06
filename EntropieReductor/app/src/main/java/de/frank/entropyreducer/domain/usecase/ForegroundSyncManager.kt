package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.repository.AmazfitRepository
import de.frank.entropyreducer.data.repository.CalendarRepository
import de.frank.entropyreducer.data.repository.ZeppBodyRepository
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
import kotlinx.coroutines.CancellationException

/**
 * Zentraler Daten-Sync. Trennt bewusst ZWEI Sync-Arten mit unterschiedlichem Takt
 * (Frank-Wunsch 2026-06-19, Sync-Synchronitaets-Umbau Etappe 1.1):
 *
 * 1. **Drive-Backup (Restore + Upload)** — klein (JSON), traegt die 1:1-Multi-Device-
 *    Synchronitaet (Aufgaben, Gewohnheiten, Texte ...). Laeuft bei JEDEM App-Start und
 *    JEDEM Foreground-Wechsel SOFORT (kein Throttle). Siehe [syncDriveNow].
 *
 * 2. **Fitness-APIs (Whoop/Oura/Health Connect/Kalender)** — teuer (Datenverbrauch +
 *    Rate-Limit). Laeuft NUR (a) automatisch nach 8 h ODER (b) wenn Frank den Biomarker-Tab
 *    oeffnet ([syncApisNow]). Jede vollstaendige API-Aktualisierung setzt den 8-h-Timer neu.
 *    So zahlt Frank den teuren API-Verkehr nur, wenn er die Biomarker-Werte wirklich anschaut.
 *
 * Vorgeschichte: Frueher lief BEIDES gemeinsam und nur alle 8 h (`syncIfStale`/`syncAllSources`).
 * Dadurch konnte eine Aenderung auf Geraet A nicht "3 Minuten spaeter" auf Geraet B ankommen —
 * der Restore lief schlicht nicht. Der 8-h-Throttle bleibt jetzt NUR fuer die teuren APIs.
 *
 * Zwei getrennte Mutexe: Ein laufender (langsamer) API-Sync darf den schnellen Drive-Sync
 * NICHT blockieren und umgekehrt — ein Biomarker-Tab-Klick soll sofort die APIs ziehen koennen,
 * auch wenn gerade ein Drive-Sync laeuft.
 *
 * Hinweis (bugs/android/kotlin.md §2.1): die `runCatching`-Bloecke um die einzelnen Sync-Calls
 * sind das bewusst erhaltene Bestandsverhalten (frueher `syncAllSources`). Die Haupt-Aufrufer
 * laufen im app-weiten Scope (nie gecancelt); die fachlichen Repos behandeln Cancellation selbst.
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
    private val zeppBodyRepository: ZeppBodyRepository,
    private val appSettings: AppSettings,
) {
    private val driveMutex = Mutex()
    private val apiMutex = Mutex()

    /**
     * Vollabgleich beim App-Start / Foreground-Wechsel: Drive IMMER (sofort), Fitness-APIs nur
     * wenn der 8-h-Timer abgelaufen ist. Reihenfolge: erst Drive (Restore holt fremde
     * Geraete-Aenderungen + sichert den eigenen Stand), dann ggf. die APIs.
     */
    suspend fun syncForeground(minIntervalMs: Long = FOREGROUND_SYNC_MIN_INTERVAL_MS) {
        syncDriveNow()
        syncApisIfStale(minIntervalMs)
    }

    /**
     * Drive-Restore + Backup. Laeuft bei JEDEM Start/Foreground sofort (kein Throttle).
     * Restore VOR Upload (Frank-Wunsch 2026-06-02): zuerst fremde Geraete-Aenderungen holen,
     * dann den eigenen Stand hochladen. Loop-Vorlagen erzeugen keine Aufgaben mehr automatisch.
     * Bei parallelem Aufruf (Lock belegt) kehrt die Methode sofort zurueck.
     */
    suspend fun syncDriveNow() {
        if (!secrets.driveBackupEnabled || secrets.driveAccountEmail == null) return
        if (!driveMutex.tryLock()) {
            Diag.i(DiagnosticArea.DRIVE_BACKUP, "ForegroundSync", "Drive-Sync laeuft bereits -> uebersprungen")
            return
        }
        try {
            Diag.i(DiagnosticArea.DRIVE_BACKUP, "ForegroundSync", "Drive-Sync START (Restore -> Heilung -> Upload)")
            val restoreResult = syncEntries.restoreFromDrive()
            restoreResult.exceptionOrNull()?.let { error ->
                if (error is CancellationException) throw error
                Diag.w(DiagnosticArea.DRIVE_BACKUP, "ForegroundSync", "Drive-Restore fehlgeschlagen; Upload zum Schutz des Remote-Stands abgebrochen", error)
                return
            }
            val uploadResult = coordinator.syncNowAndWait()
            uploadResult.exceptionOrNull()?.let { error ->
                if (error is CancellationException) throw error
                Diag.w(DiagnosticArea.DRIVE_BACKUP, "ForegroundSync", "Drive-Upload fehlgeschlagen", error)
                return
            }
            Diag.i(DiagnosticArea.DRIVE_BACKUP, "ForegroundSync", "Drive-Sync FERTIG")
        } finally {
            driveMutex.unlock()
        }
    }

    /**
     * Fitness-API-Sync nur, wenn seit der letzten API-Aktualisierung mindestens [minIntervalMs]
     * vergangen sind (Standard 8 h). Wird beim Start/Foreground aufgerufen. Wenn noch nie
     * gesynced wurde (Timer 0), wird synchronisiert.
     */
    suspend fun syncApisIfStale(minIntervalMs: Long) {
        val lastMs = appSettings.lastRefreshFooterAtMsFlow.first()
        val now = System.currentTimeMillis()
        if (lastMs > 0L && now - lastMs < minIntervalMs) {
            Diag.i(
                DiagnosticArea.APP,
                "ForegroundSync",
                "API-Sync uebersprungen (noch frisch: ${(now - lastMs) / 60000} min < ${minIntervalMs / 60000} min)",
            )
            return
        }
        syncApisNow("8h-Automatik")
    }

    /**
     * Voller Fitness-API-Abgleich (Whoop/Oura/Health Connect/Kalender). Wird (a) von
     * [syncApisIfStale] nach 8 h ODER (b) beim Klick auf den Biomarker-Tab aufgerufen
     * ([reason] = "Biomarker-Tab"). Stempelt am Ende den "Zuletzt synchronisiert"-Footer —
     * dieser Zeitstempel ist gleichzeitig der 8-h-Timer (jede API-Aktualisierung setzt ihn neu).
     * Bei parallelem Aufruf (Lock belegt) kehrt die Methode sofort zurueck.
     */
    suspend fun syncApisNow(reason: String = "Biomarker-Tab") {
        if (!apiMutex.tryLock()) {
            Diag.i(DiagnosticArea.APP, "ForegroundSync", "API-Sync laeuft bereits ($reason) -> uebersprungen")
            return
        }
        try {
            Diag.i(DiagnosticArea.APP, "ForegroundSync", "API-Sync START ($reason)")
            val results = coroutineScope {
                val bodyDeferred =
                    async { runCatchingCancellable { zeppBodyRepository.syncToCache() } }
                val whoopDeferred =
                    async {
                        if (oauth.loadWhoopAuthState().isAuthorized) {
                            whoop.syncLastDays(365)
                        } else {
                            Result.success(0)
                        }
                    }
                val ouraDeferred =
                    async {
                        if (oura.isTokenConfigured()) {
                            oura.syncLastDays(365).map { it.values.sum() }
                        } else {
                            Result.success(0)
                        }
                    }
                val trainingDeferred =
                    async {
                        // Frank-Wunsch 2026-07-03: Trainings kommen jetzt aus Health Connect (Strava raus).
                        runCatchingCancellable { amazfit.mergeFromHealthConnect(days = 30) }
                    }
                val calendarDeferred =
                    async {
                        if (secrets.calendarAccountEmail != null) {
                            calendar.syncDefaultWindow().map { 0 }
                        } else {
                            Result.success(0)
                        }
                    }
                listOf(
                    bodyDeferred.await(),
                    whoopDeferred.await(),
                    ouraDeferred.await(),
                    trainingDeferred.await(),
                    calendarDeferred.await(),
                )
            }
            val firstFailure = results.firstNotNullOfOrNull { it.exceptionOrNull() }
            if (firstFailure != null) {
                if (firstFailure is CancellationException) throw firstFailure
                Diag.w(DiagnosticArea.APP, "ForegroundSync", "API-Sync unvollständig; 8h-Timer bleibt unverändert", firstFailure)
                return
            }
            val (bodyCount, whoopCount, ouraCount, trainingCount) = results.take(4).map { it.getOrThrow() }

            // "Zuletzt synchronisiert"-Footer setzen (gleiches Format wie der manuelle
            // Aktualisieren-Knopf). Der Zeitstempel dient gleichzeitig als Grundlage
            // fuer den 8h-Throttle in syncApisIfStale().
            val nowZ = java.time.ZonedDateTime.now()
            val footerTs =
                "%02d.%02d. %02d:%02d".format(
                    nowZ.dayOfMonth,
                    nowZ.monthValue,
                    nowZ.hour,
                    nowZ.minute,
                )
            val footer =
                "✓ $footerTs · Whoop $whoopCount · Training $trainingCount · " +
                    "Oura $ouraCount · Zepp $bodyCount"
            appSettings.setLastRefreshFooter(footer, System.currentTimeMillis())
            Diag.i(
                DiagnosticArea.APP,
                "ForegroundSync",
                "API-Sync FERTIG ($reason): Whoop=$whoopCount Training=$trainingCount Oura=$ouraCount Zepp=$bodyCount -> 8h-Timer neu gesetzt",
            )
        } finally {
            apiMutex.unlock()
        }
    }

    companion object {
        /** Standard-Throttle fuer den Fitness-API-Sync: 8 Stunden (Frank-Wunsch 2026-06-01). */
        const val FOREGROUND_SYNC_MIN_INTERVAL_MS = 8L * 60L * 60L * 1000L
    }

    private suspend inline fun <T> runCatchingCancellable(crossinline block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.failure(error)
        }
}

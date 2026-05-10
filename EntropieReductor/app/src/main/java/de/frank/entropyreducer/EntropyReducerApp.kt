package de.frank.entropyreducer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import de.frank.entropyreducer.data.local.InitialDataMigrator
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.health.HealthConnectManager
import de.frank.entropyreducer.data.repository.AmazfitRepository
import de.frank.entropyreducer.data.repository.OuraRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.di.ApplicationScope
import de.frank.entropyreducer.workers.BackgroundScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Anwendungs-Wurzel: Hilt-Setup + WorkManager-Hilt-Integration.
 * Alle weiteren Initialisierungen erfolgen lazy via Hilt-Module.
 *
 * Frank-Wunsch 2026-05-09: Bei JEDEM App-Start (auch nach App-im-Hintergrund-
 * zurueckholen) soll der Whoop-Sync laufen. MainActivity.init feuerte nur einmal
 * pro Process-Lifetime — wenn Frank die App nur kurz wechselt und zurueckholt,
 * wird der Init NICHT nochmal durchlaufen. Loesung: ProcessLifecycleOwner,
 * sein ON_START-Event feuert bei jedem Foreground-Wechsel der App.
 */
@HiltAndroidApp
class EntropyReducerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var scheduler: BackgroundScheduler

    @Inject
    lateinit var oauth: OAuthService

    @Inject
    lateinit var dataMigrator: InitialDataMigrator

    @Inject
    lateinit var amazfitRepository: AmazfitRepository

    @Inject
    lateinit var ouraRepository: OuraRepository

    @Inject
    lateinit var healthConnect: HealthConnectManager

    @Inject
    lateinit var appSettings: AppSettings

    // Performance-Fix Loop 2.1: ApplicationScope (SupervisorJob + Dispatchers.IO,
    // siehe AppScopeModule.kt) wird benutzt um den ProcessLifecycleOwner ON_START-
    // Callback aus dem Main-Thread herauszuholen — der Whoop-AuthState-Read trifft
    // EncryptedSharedPreferences (Hardware-Keystore-Roundtrip + Disk-I/O), das war
    // auf Main beim App-Foreground-Wechsel sichtbar.
    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    /**
     * Frank-Wunsch 2026-05-09 (Abend): Datenrettung aus alter AppDatabase v9 in
     * ScientistDatabase v2 — Stufe 1 muss VOR Hilt-Init laufen, damit die alte
     * DB-Datei noch unangetastet ist wenn wir sie lesen. Die geretteten Daten
     * werden zwischengespeichert und in onCreate() nach super.onCreate() via
     * Hilt-DAO geschrieben.
     */
    private var rescuedData: InitialDataMigrator.RescuedData? = null

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        // Stufe 1 (Pre-Hilt): alte Insights und Memories aus AppDatabase v9 retten
        // BEVOR Room sie destructive resettet. Liest direkt mit SQLiteDatabase
        // OPEN_READONLY, kein Hilt-Zugriff.
        rescuedData = InitialDataMigrator.readOldDataPreHilt(this)

        super.onCreate()

        // Stufe 2 (Post-Hilt): geretete Daten via DAO in ScientistDatabase schreiben.
        // ScientistDatabase wird beim ersten DAO-Zugriff von Room geoeffnet,
        // dabei laeuft MIGRATION_1_2 und legt die neuen Tabellen an.
        dataMigrator.writeRescuedData(rescuedData)
        rescuedData = null

        // Frank-Wunsch 2026-05-10: T-Rex-3-Sport-Mapping korrigieren. Bestehende
        // Workouts mit Code 7 (Trailrunning), 12 (Crosstrainer) oder 52 (Krafttraining)
        // wurden frueher faelschlich als "Laufen" gespeichert weil die T-Rex 3 fuer
        // alle Sportarten den gleichen source="run.NNN.huami.com" sendet. Migration
        // ist idempotent — bei wiederholten Starts kein Effekt mehr.
        applicationScope.launch {
            runCatching { amazfitRepository.applyFrankSportOverrides() }
                .onFailure { android.util.Log.w("EntropyReducerApp", "Sport-Override-Migration fehlgeschlagen", it) }
        }

        // Bei jedem App-Foreground-Wechsel Whoop-Sync triggern wenn verbunden.
        // Whoop-Rate-Limit ist 60 req/min — selbst 50 Foreground-Wechsel/Tag sind
        // unkritisch, der Worker macht nur 3 paginierte API-Calls pro Sync.
        //
        // Performance-Fix Loop 2.1: oauth.loadWhoopAuthState() liest aus
        // EncryptedSharedPreferences (Hardware-Keystore-Roundtrip + Disk-I/O) und
        // parsed JSON — bisher synchron auf Main bei jedem onStart. Bei Frank's
        // Foldable mit haeufigen Foreground-Wechseln war das ein sichtbarer
        // Stutter im Resume-Pfad. applicationScope laeuft auf Dispatchers.IO,
        // scheduler.runWhoopSyncNow() ruft thread-safe WorkManager.enqueueUniqueWork
        // (REPLACE-Policy stellt sicher dass paralleler ON_START keine Doppel-
        // Worker erzeugt).
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                applicationScope.launch {
                    if (oauth.loadWhoopAuthState().isAuthorized) {
                        scheduler.runWhoopSyncNow()
                    }
                }
                // Frank-Wunsch 2026-05-10: bei jedem App-Start auch Amazfit
                // und Oura synchronisieren. Amazfit laeuft ueber den Worker
                // (kennt den Token-Status selbst). Oura nutzt den Repository
                // direkt — wenn kein Token gespeichert ist, wirft syncLastDays
                // eine IllegalStateException die wir hier still verschlucken.
                applicationScope.launch {
                    runCatching { scheduler.runAmazfitSyncNow() }
                }
                applicationScope.launch {
                    if (ouraRepository.isTokenConfigured()) {
                        // Folgesync zieht 7 Tage zurueck — reicht um neue Werte
                        // zu holen und ist schnell. Der initiale 365-Tage-Pull
                        // passiert nur beim ersten Token-Speichern.
                        runCatching { ouraRepository.syncLastDays(7) }
                            .onFailure { android.util.Log.w("EntropyReducerApp", "Oura-Foreground-Sync fehlgeschlagen", it) }
                    }
                }
                // Frank-Wunsch 2026-05-10: bei jedem App-Start auch Health
                // Connect refreshen — Smart-Scale-Werte koennen sich aendern
                // ohne dass die App im Vordergrund war. Schreibt nach
                // erfolgreichem Read den Sync-Zeitstempel in AppSettings.
                //
                // Performance-Audit Loop 1 (2026-05-10): Cache-Window 4h —
                // Foldable-User loest onStart bei jedem Aufklappen aus (20+/Tag).
                // Gewichtsdaten aendern sich nicht 20x am Tag, jeder HC-Read ist
                // ein Binder-IPC zum HealthData-Prozess. Cache spart 90% der IPC-
                // Roundtrips ohne Funktionsverlust.
                applicationScope.launch {
                    runCatching {
                        val lastSync = appSettings.lastHealthConnectSyncMsFlow.first()
                        val now = System.currentTimeMillis()
                        val staleThresholdMs = 4 * 60 * 60 * 1000L // 4h
                        if (now - lastSync < staleThresholdMs) {
                            // Frische genug — Read ueberspringen.
                            return@runCatching
                        }
                        if (healthConnect.isAvailable() && healthConnect.hasWeightReadPermission()) {
                            healthConnect.readLatestWeightKg()
                            appSettings.setLastHealthConnectSync(now)
                        }
                    }.onFailure { android.util.Log.w("EntropyReducerApp", "HealthConnect-Foreground-Sync fehlgeschlagen", it) }
                }
            }
        })
    }
}

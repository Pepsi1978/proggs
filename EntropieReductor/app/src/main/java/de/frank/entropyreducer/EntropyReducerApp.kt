package de.frank.entropyreducer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import de.frank.entropyreducer.data.health.HealthConnectManager
import de.frank.entropyreducer.data.local.InitialDataMigrator
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.repository.AmazfitRepository
import de.frank.entropyreducer.data.repository.OuraRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.di.ApplicationScope
import de.frank.entropyreducer.workers.BackgroundScheduler
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Anwendungs-Wurzel: Hilt-Setup + WorkManager-Hilt-Integration. Alle weiteren Initialisierungen
 * erfolgen lazy via Hilt-Module.
 *
 * Frank-Wunsch 2026-05-09: Bei JEDEM App-Start (auch nach App-im-Hintergrund- zurueckholen) soll
 * der Whoop-Sync laufen. MainActivity.init feuerte nur einmal pro Process-Lifetime — wenn Frank die
 * App nur kurz wechselt und zurueckholt, wird der Init NICHT nochmal durchlaufen. Loesung:
 * ProcessLifecycleOwner, sein ON_START-Event feuert bei jedem Foreground-Wechsel der App.
 */
@HiltAndroidApp
class EntropyReducerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var scheduler: BackgroundScheduler

    @Inject lateinit var oauth: OAuthService

    @Inject lateinit var secrets: de.frank.entropyreducer.data.settings.EncryptedSecretsStore

    @Inject lateinit var dataMigrator: InitialDataMigrator

    @Inject lateinit var amazfitRepository: AmazfitRepository

    @Inject lateinit var ouraRepository: OuraRepository

    @Inject lateinit var healthConnect: HealthConnectManager

    @Inject lateinit var appSettings: AppSettings

    // Performance-Fix Loop 2.1: ApplicationScope (SupervisorJob + Dispatchers.IO,
    // siehe AppScopeModule.kt) wird benutzt um den ProcessLifecycleOwner ON_START-
    // Callback aus dem Main-Thread herauszuholen — der Whoop-AuthState-Read trifft
    // EncryptedSharedPreferences (Hardware-Keystore-Roundtrip + Disk-I/O), das war
    // auf Main beim App-Foreground-Wechsel sichtbar.
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    /**
     * Frank-Wunsch 2026-05-09 (Abend): Datenrettung aus alter AppDatabase v9 in ScientistDatabase
     * v2 — Stufe 1 muss VOR Hilt-Init laufen, damit die alte DB-Datei noch unangetastet ist wenn
     * wir sie lesen. Die geretteten Daten werden zwischengespeichert und in onCreate() nach
     * super.onCreate() via Hilt-DAO geschrieben.
     */
    private var rescuedData: InitialDataMigrator.RescuedData? = null

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()

    override fun onCreate() {
        // Frank-Wunsch 2026-05-13: App laeuft IMMER in Berliner Zeitzone (Europe/Berlin),
        // unabhaengig von der Geraete-Zeitzone oder ob das Geraet auf Reisen ist. Das
        // muss VOR super.onCreate() passieren damit alle Hilt-/Room-/WorkManager-
        // Initialisierungen bereits mit der korrekten Zeitzone laufen. Damit picken
        // ALLE Aufrufe von ZoneId.systemDefault() / TimeZone.getDefault() / LocalDate.now()
        // automatisch Europe/Berlin — wir muessen nicht 55 Call-Sites einzeln aendern.
        // Dokumentiert in [AppTime] (siehe util/AppTime.kt).
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Berlin"))

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
                .onFailure {
                    android.util.Log.w(
                        "EntropyReducerApp",
                        "Sport-Override-Migration fehlgeschlagen",
                        it,
                    )
                }
        }

        // Frank-Wunsch 2026-05-16: Einmalige Workout-Cleanup-Migration vor der
        // Polar-Integration. Loescht alle bestehenden Workouts und triggert einen
        // Sync damit das Drive-Backup mit dem leeren Stand ueberschrieben wird.
        // 5-Sekunden-Delay damit etwaige Drive-Restore-Operationen am App-Start
        // zuerst abgeschlossen sind und ihre Workouts ueberhaupt sichtbar werden,
        // bevor die Migration sie raeumt. Idempotent via workoutCleanupV1-Flag.
        applicationScope.launch {
            kotlinx.coroutines.delay(5000L)
            runCatching {
                if (!appSettings.isWorkoutCleanupV1Done()) {
                    amazfitRepository.cleanupAllWorkoutsForMigration()
                    appSettings.setWorkoutCleanupV1Done(true)
                    android.util.Log.i(
                        "EntropyReducerApp",
                        "Workout-Cleanup-Migration v1 abgeschlossen — Backup wird mit leerem Stand ueberschrieben",
                    )
                }
            }.onFailure {
                android.util.Log.w(
                    "EntropyReducerApp",
                    "Workout-Cleanup-Migration fehlgeschlagen",
                    it,
                )
            }
        }

        // Frank-Wunsch 2026-05-17: Sport-Rename V1. Einmalige Umbenennung der
        // Polar-Codes 18 (Indoor-Rudern) und entspr. Rudergeraet zu Crosstrainer.
        // Frank's Polar zeichnet Crosstrainer als Indoor-Rudern auf — diese
        // Migration korrigiert alle bisherigen Workouts. Source-Maps fuer
        // zukuenftige Imports sind bereits angepasst.
        applicationScope.launch {
            kotlinx.coroutines.delay(6000L)
            runCatching {
                if (!appSettings.isSportRenameV1Done()) {
                    val renamedA = amazfitRepository.renameSportName("Indoor-Rudern", "Crosstrainer")
                    val renamedB = amazfitRepository.renameSportName("Rudergeraet", "Crosstrainer")
                    appSettings.setSportRenameV1Done(true)
                    android.util.Log.i(
                        "EntropyReducerApp",
                        "Sport-Rename-V1 fertig: $renamedA Indoor-Rudern + $renamedB Rudergeraet -> Crosstrainer",
                    )
                }
                if (!appSettings.isSportRenameV2Done()) {
                    val renamed = amazfitRepository.renameSportName(
                        "Funktionelles Training",
                        "Trailrunning",
                    )
                    appSettings.setSportRenameV2Done(true)
                    android.util.Log.i(
                        "EntropyReducerApp",
                        "Sport-Rename-V2 fertig: $renamed Funktionelles Training -> Trailrunning",
                    )
                }
            }.onFailure {
                android.util.Log.w("EntropyReducerApp", "Sport-Rename fehlgeschlagen", it)
            }
        }

        // Frank-Wunsch 2026-05-17: V2-Cleanup vor der Strava-only-Phase.
        // Behaelt nur Trainings der letzten ~2 Jahre bis 30.03.2026 17:25 Berlin —
        // alles davor (Uralt-Polar-Daten) UND alles danach (Polar-Duplikate vom
        // 17.05., 14.05., 09.05., 08.05., 01.05.) wird in einer SQL-Operation
        // entfernt. Danach soll Strava sauber neu reinkommen.
        //
        // 7-Sekunden-Delay: 2 Sekunden nach V1, damit V1-Cleanup + Drive-Refresh
        // zuerst fertig sind und V2 nur die danach noch da-bleibenden Trainings
        // anguckt. Idempotent via workoutCleanupV2-Flag.
        applicationScope.launch {
            kotlinx.coroutines.delay(7000L)
            runCatching {
                if (!appSettings.isWorkoutCleanupV2Done()) {
                    val zone = java.time.ZoneId.of("Europe/Berlin")
                    val newerThanMs = java.time.LocalDateTime.of(2026, 3, 30, 17, 26, 0)
                        .atZone(zone).toInstant().toEpochMilli()
                    val olderThanMs = System.currentTimeMillis() -
                        2L * 365L * 24L * 60L * 60L * 1000L
                    val deleted = amazfitRepository
                        .cleanupWorkoutsKeepRange(olderThanMs, newerThanMs)
                    appSettings.setWorkoutCleanupV2Done(true)
                    android.util.Log.i(
                        "EntropyReducerApp",
                        "Workout-Cleanup-V2 abgeschlossen — $deleted Trainings geloescht " +
                            "(Fenster: $olderThanMs .. $newerThanMs)",
                    )
                }
            }.onFailure {
                android.util.Log.w(
                    "EntropyReducerApp",
                    "Workout-Cleanup-V2 fehlgeschlagen",
                    it,
                )
            }
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
        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        applicationScope.launch {
                            if (oauth.loadWhoopAuthState().isAuthorized) {
                                scheduler.runWhoopSyncNow()
                            }
                        }
                        // Frank-Wunsch 2026-05-16: Polar bei jedem App-Foreground.
                        // Frank-Wunsch 2026-05-17: nur wenn `disablePolarSync == false`.
                        // Default ist Polar AUS — Strava alleinige Trainings-Quelle.
                        // Code bleibt erhalten damit Polar jederzeit wieder anschaltbar ist.
                        applicationScope.launch {
                            val polarDisabled = appSettings.isPolarSyncDisabled()
                            if (!polarDisabled &&
                                oauth.loadPolarAuthState().isAuthorized &&
                                secrets.polarUserId > 0L) {
                                scheduler.runPolarSyncNow()
                            }
                        }
                        // Frank-Wunsch 2026-05-11: Amazfit/Zepp wird NICHT mehr automatisch
                        // beim App-Start synchronisiert. Grund: jeder Re-Login zur Zepp-
                        // Cloud invalidiert den Token der Zepp-App auf dem Handy — Frank
                        // wurde dort staendig rausgeworfen. Zepp-Sync laeuft jetzt nur
                        // noch ueber den manuellen "Aktualisieren"-Knopf im Biomarker-
                        // Bildschirm (BiomarkerViewModel.refreshNow). Andere APIs (Whoop,
                        // Oura, Health Connect) sind nicht betroffen — die haben das
                        // Single-Token-Problem nicht.
                        applicationScope.launch {
                            if (ouraRepository.isTokenConfigured()) {
                                // Folgesync zieht 7 Tage zurueck — reicht um neue Werte
                                // zu holen und ist schnell. Der initiale 365-Tage-Pull
                                // passiert nur beim ersten Token-Speichern.
                                runCatching { ouraRepository.syncLastDays(7) }
                                    .onFailure {
                                        android.util.Log.w(
                                            "EntropyReducerApp",
                                            "Oura-Foreground-Sync fehlgeschlagen",
                                            it,
                                        )
                                    }
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
                                    if (
                                        healthConnect.isAvailable() &&
                                            healthConnect.hasWeightReadPermission()
                                    ) {
                                        healthConnect.readLatestWeightKg()
                                        appSettings.setLastHealthConnectSync(now)
                                    }
                                }
                                .onFailure {
                                    android.util.Log.w(
                                        "EntropyReducerApp",
                                        "HealthConnect-Foreground-Sync fehlgeschlagen",
                                        it,
                                    )
                                }
                        }
                    }
                }
            )
    }
}

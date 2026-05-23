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

    /** Sprint 2 (Frank-Wunsch 2026-05-22): wiederkehrende Aufgaben beim App-Start erzeugen. */
    @Inject
    lateinit var generateRecurringInstances:
        de.frank.entropyreducer.domain.usecase.GenerateRecurringInstancesUseCase

    @Inject lateinit var ouraRepository: OuraRepository

    @Inject lateinit var healthConnect: HealthConnectManager

    @Inject lateinit var appSettings: AppSettings

    @Inject
    lateinit var agenticTriggerScheduler:
        de.frank.entropyreducer.domain.agentic.trigger.TriggerScheduler

    @Inject
    lateinit var promptExecutionRepo:
        de.frank.entropyreducer.data.repository.PromptExecutionRepository

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

        // Frank-Wunsch 2026-05-21: Agentic-AI-Trigger-Worker als PeriodicWork
        // registrieren — pruefen alle 15 Minuten ob ein CRON-Trigger faellig ist.
        // KEEP-Policy stellt sicher dass der bestehende Worker nicht neu gestartet
        // wird wenn er schon laeuft.
        agenticTriggerScheduler.scheduleOrUpdate()

        // Direktive 3 Loop-2-Fix (war LOOP-2-1-Bug): nach App-Crash haengende
        // prompt_executions-Eintraege mit Status RUNNING auf INTERRUPTED setzen.
        // Sonst zeigt der HistoryScreen sie ewig als "laufend" an obwohl die App
        // schon laengst neu gestartet wurde.
        applicationScope.launch {
            runCatching {
                    promptExecutionRepo.markRunningAsInterrupted(System.currentTimeMillis())
                }
                .onFailure {
                    android.util.Log.w(
                        "EntropyReducerApp",
                        "markRunningAsInterrupted fehlgeschlagen",
                        it,
                    )
                }
        }

        // Frank-Bugfix 2026-05-22 (#949): NICHT mehr blind RRULE-basiert
        // generieren beim App-Start — das erzeugte Duplikate, weil bei jedem
        // Start eine neue Instanz mit anderem occurrenceMs kam. Stattdessen
        // wird jetzt cleanup+ensure aufgerufen: max 1 offene Aufgabe pro aktiver
        // Vorlage, 0 bei inaktiver.
        applicationScope.launch {
            runCatching { generateRecurringInstances.cleanupAndEnsureSingle() }
                .onFailure {
                    android.util.Log.w(
                        "EntropyReducerApp",
                        "RecurringCleanup fehlgeschlagen",
                        it,
                    )
                }
        }

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
        // Idempotent via workoutCleanupV1-Flag.
        //
        // Frank-Bugfix 2026-05-22: Auf einem frisch installierten Geraet (z.B. nach
        // ADB-Uninstall) ist die lokale DB leer UND das Flag noch nicht gesetzt.
        // Frueher (5-Sekunden-Delay) lief die Migration BEVOR der Benutzer Zeit
        // hatte Drive zu verbinden — und setzte das Flag auf true. Der spaetere
        // Restore wurde dann wegen des Flags ausgebremst und Frank's Trainings
        // kamen NICHT zurueck.
        //
        // Neue Logik:
        //  1) Lokale Tabelle nicht leer → Migration laeuft normal (wie frueher).
        //  2) Lokale Tabelle leer + Flag nicht gesetzt → wahrscheinlich frische
        //     Installation. Migration UEBERSPRINGEN — wenn Frank spaeter Drive
        //     verbindet, kommt der korrekte Stand zurueck. Wenn er nichts
        //     verbindet bleibt die Tabelle leer (genauso wie nach Migration).
        //  3) Delay 60s statt 5s als zusaetzliche Defense-in-Depth.
        applicationScope.launch {
            kotlinx.coroutines.delay(60_000L)
            runCatching {
                    if (!appSettings.isWorkoutCleanupV1Done()) {
                        val localCount = amazfitRepository.workoutCount()
                        if (localCount == 0) {
                            android.util.Log.i(
                                "EntropyReducerApp",
                                "Workout-Cleanup-Migration v1 uebersprungen — lokale DB leer, evtl. frische Installation; warte auf Drive-Restore",
                            )
                        } else {
                            amazfitRepository.cleanupAllWorkoutsForMigration()
                            appSettings.setWorkoutCleanupV1Done(true)
                            android.util.Log.i(
                                "EntropyReducerApp",
                                "Workout-Cleanup-Migration v1 abgeschlossen ($localCount Eintraege geraeumt) — Backup wird mit leerem Stand ueberschrieben",
                            )
                        }
                    }
                }
                .onFailure {
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
                        val renamedA =
                            amazfitRepository.renameSportName("Indoor-Rudern", "Crosstrainer")
                        val renamedB =
                            amazfitRepository.renameSportName("Rudergeraet", "Crosstrainer")
                        appSettings.setSportRenameV1Done(true)
                        android.util.Log.i(
                            "EntropyReducerApp",
                            "Sport-Rename-V1 fertig: $renamedA Indoor-Rudern + $renamedB Rudergeraet -> Crosstrainer",
                        )
                    }
                    if (!appSettings.isSportRenameV2Done()) {
                        val renamed =
                            amazfitRepository.renameSportName(
                                "Funktionelles Training",
                                "Trailrunning",
                            )
                        appSettings.setSportRenameV2Done(true)
                        android.util.Log.i(
                            "EntropyReducerApp",
                            "Sport-Rename-V2 fertig: $renamed Funktionelles Training -> Trailrunning",
                        )
                    }
                }
                .onFailure {
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
                        val newerThanMs =
                            java.time.LocalDateTime.of(2026, 3, 30, 17, 26, 0)
                                .atZone(zone)
                                .toInstant()
                                .toEpochMilli()
                        val olderThanMs =
                            System.currentTimeMillis() - 2L * 365L * 24L * 60L * 60L * 1000L
                        val deleted =
                            amazfitRepository.cleanupWorkoutsKeepRange(olderThanMs, newerThanMs)
                        appSettings.setWorkoutCleanupV2Done(true)
                        android.util.Log.i(
                            "EntropyReducerApp",
                            "Workout-Cleanup-V2 abgeschlossen — $deleted Trainings geloescht " +
                                "(Fenster: $olderThanMs .. $newerThanMs)",
                        )
                    }
                }
                .onFailure {
                    android.util.Log.w("EntropyReducerApp", "Workout-Cleanup-V2 fehlgeschlagen", it)
                }
        }

        // Frank-Wunsch 2026-05-19, geaendert 2026-05-23: Permanente 1-Jahres-Retention
        // fuer Trainings (vorher 2 Jahre). Polar-Bulk-Import brachte 957 Workouts
        // (zurueck bis 2018), die App wurde dadurch langsam. Im Gegensatz zu V2-Cleanup
        // oben (einmalige Migration mit Fixdatum) laeuft pruneOldTrainings() bei JEDEM
        // Start gegen die aktuelle Uhrzeit minus TRAINING_RETENTION_DAYS (365) — damit
        // ist das Fenster rollend und Re-Imports koennen die Liste nicht mehr aufblaehen.
        // Trimmt auch die bereits vorhandenen Eintraege beim naechsten Start aufs letzte Jahr.
        // 8-Sekunden-Delay: nach V2-Cleanup (7s), damit V2 zuerst sein Fixdatum-
        // Fenster anwendet und V3 nur noch nachsortiert was V2 stehen liess.
        applicationScope.launch {
            kotlinx.coroutines.delay(8000L)
            runCatching {
                    val deleted = amazfitRepository.pruneOldTrainings()
                    android.util.Log.i(
                        "EntropyReducerApp",
                        "Trainings-Retention (1 Jahr): $deleted Trainings entfernt",
                    )
                }
                .onFailure {
                    android.util.Log.w(
                        "EntropyReducerApp",
                        "Trainings-Retention fehlgeschlagen",
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
                        // Frank-Wunsch 2026-05-17: Strava bei jedem App-Foreground
                        // sync — neue Workouts der letzten 30 Tage werden geholt.
                        // Manuelle Edits sind durch manualOverridesMs geschuetzt
                        // (siehe AmazfitRepository.mergeFromStrava). Strava-API
                        // hat 100 Requests/15min Limit — bei 20 App-Wechseln/Tag
                        // ist das unkritisch.
                        applicationScope.launch {
                            runCatching { amazfitRepository.mergeFromStrava(days = 30) }
                                .onFailure {
                                    android.util.Log.w(
                                        "EntropyReducerApp",
                                        "Strava-Foreground-Sync fehlgeschlagen",
                                        it,
                                    )
                                }
                        }
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

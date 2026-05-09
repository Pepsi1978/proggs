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
import de.frank.entropyreducer.workers.BackgroundScheduler
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

        // Bei jedem App-Foreground-Wechsel Whoop-Sync triggern wenn verbunden.
        // Whoop-Rate-Limit ist 60 req/min — selbst 50 Foreground-Wechsel/Tag sind
        // unkritisch, der Worker macht nur 3 paginierte API-Calls pro Sync.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (oauth.loadWhoopAuthState().isAuthorized) {
                    scheduler.runWhoopSyncNow()
                }
            }
        })
    }
}

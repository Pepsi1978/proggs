package de.frank.entropyreducer

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Anwendungs-Wurzel: Hilt-Setup + WorkManager-Hilt-Integration.
 * Alle weiteren Initialisierungen erfolgen lazy via Hilt-Module.
 */
@HiltAndroidApp
class EntropyReducerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}

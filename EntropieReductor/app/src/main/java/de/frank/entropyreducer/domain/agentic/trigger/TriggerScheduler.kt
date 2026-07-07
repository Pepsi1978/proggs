package de.frank.entropyreducer.domain.agentic.trigger

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registriert den AgenticTriggerWorker beim App-Start (Frank-Wunsch 2026-05-21).
 *
 * Aufruf in EntropyReducerApp.onCreate():
 *   triggerScheduler.scheduleOrUpdate()
 *
 * WorkManager-Konfiguration:
 *  - PeriodicWorkRequest mit 15 Min Intervall (Android-Minimum)
 *  - KeepExistingWork: bestehender Worker wird nicht neu gestartet, nur
 *    sicher registriert
 *  - Constraint: Netzwerk verbunden (sonst kein Gemini-API moeglich, der
 *    Run wuerde sofort failen)
 *  - Battery-Optimization: wenn Frank's Geraet in Doze-Mode ist, kann der
 *    Worker verzoegert werden. Das ist OK — die naechste Wachphase holt
 *    die Trigger nach.
 *
 * Deaktivieren: triggerScheduler.cancel() — z.B. wenn Frank Auto-Trigger
 * komplett aus haben will (Aktion fehlt aktuell in der UI, kommt spaeter).
 */
@Singleton
class TriggerScheduler
@Inject
constructor(@ApplicationContext private val context: Context) {

    fun scheduleOrUpdate() {
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val request =
            PeriodicWorkRequestBuilder<AgenticTriggerWorker>(
                15L,
                TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                AgenticTriggerWorker.UNIQUE_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }

    fun cancel() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(AgenticTriggerWorker.UNIQUE_NAME_PERIODIC)
    }
}

package de.frank.entropyreducer.data.remote.drive

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Frank-Bugfix 2026-05-22: Helper damit file-basierte Stores (Tagebuch,
 * Thesen — beide nutzen Jetpack DataStore und haben keine Hilt-DI) trotzdem
 * den Drive-Backup-Sync triggern koennen.
 *
 * Holt die SyncCoordinator-Singleton aus dem Application-Hilt-Graph via
 * EntryPointAccessors und ruft requestSync(). Wenn Drive-Backup deaktiviert
 * oder kein Konto verbunden ist, ist requestSync() bereits ein No-Op —
 * runCatching schluckt jeden Fehler damit der eigentliche Schreibvorgang
 * nicht durch einen fehlerhaften Backup-Trigger geblockt wird.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncCoordinatorEntryPoint {
    fun syncCoordinator(): SyncCoordinator
}

fun triggerDriveBackup(context: Context, reason: String = "DataStore-Aenderung") {
    runCatching {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SyncCoordinatorEntryPoint::class.java,
        )
            .syncCoordinator()
            .requestSync(reason)
    }
}

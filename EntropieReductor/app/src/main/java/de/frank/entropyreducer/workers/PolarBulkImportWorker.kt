package de.frank.entropyreducer.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.frank.entropyreducer.data.local.AppDatabase
import de.frank.entropyreducer.data.local.dao.AmazfitWorkoutDao
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.remote.polar.PolarBulkImporter
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One-Shot-Worker fuer den Polar-Bulk-Import (Frank-Wunsch 2026-05-16).
 *
 * Importiert die per E-Mail erhaltene Polar-Flow-Export-ZIP in die App.
 * Streamt die ZIP via PolarBulkImporter, batched die Entities in 50er-
 * Bloecken in die DB via withTransaction (deutlich schneller als pro
 * Eintrag), zeigt waehrend des Imports eine Foreground-Notification mit
 * Fortschritt.
 *
 * Warum Foreground-Worker:
 *  - Bei 3000 Trainings dauert der Import 30-60 Sekunden
 *  - Android killt Background-Worker aggressiv wenn der Bildschirm ausgeht
 *  - Mit Foreground-Notification haelt das System den Worker am Leben
 *
 * Input-Daten (via workData):
 *  - KEY_ZIP_URI: String — content://-URI zur ZIP-Datei (vom File-Picker)
 *
 * Direktive 3 (Resilient Bugfixing):
 *  - Wenn die ZIP-Datei nicht gelesen werden kann: Result.failure (kein Retry,
 *    der User muss die Datei neu auswaehlen)
 *  - Wenn einzelne Trainings nicht geparst werden koennen: weiterlaufen, am
 *    Ende den Zaehler "uebersprungen" in der Notification anzeigen
 */
@HiltWorker
class PolarBulkImportWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val importer: PolarBulkImporter,
    private val workoutDao: AmazfitWorkoutDao,
    private val appDatabase: AppDatabase,
    private val syncCoordinator: SyncCoordinator,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val zipUriString = inputData.getString(KEY_ZIP_URI)
        Log.i(TAG, "Polar-Bulk-Worker: doWork gestartet — zipUriString=$zipUriString")
        if (zipUriString.isNullOrBlank()) {
            Log.e(TAG, "Polar-Bulk-Import: kein zipUri uebergeben")
            return Result.failure()
        }
        val zipUri = Uri.parse(zipUriString)
        Log.i(TAG, "Polar-Bulk-Worker: URI scheme=${zipUri.scheme} path=${zipUri.path}")

        // Frank-Bugfix 2026-05-16 (4. Iteration): Wir akzeptieren NUR noch
        // file://-URIs. Wenn aus irgendeinem Grund eine alte content://-URI
        // ankommt (z.B. enqueued Worker vor #774), brechen wir SOFORT ab
        // mit klarer Fehlermeldung — kein Versuch mehr, Drive zu lesen.
        if (zipUri.scheme != "file") {
            val msg = "Polar-Bulk-Worker erhielt ungueltige URI-Quelle '${zipUri.scheme}://' — erwartet wird file://. Bitte ZIP-Datei nochmal auswaehlen."
            Log.e(TAG, msg)
            showFailureNotification("Quelle nicht unterstuetzt. Bitte erneut ZIP auswaehlen.")
            return Result.failure()
        }

        setForeground(buildForegroundInfo("Polar-Historie wird gelesen…"))

        // Cache-Datei fuer spaeteres Aufraeumen merken (nur wenn file:// und
        // im App-Cache liegt — fremde Dateien loeschen wir natuerlich nicht)
        val cacheFileToCleanup: File? = if (zipUri.scheme == "file") {
            val path = zipUri.path
            if (path != null && path.contains("/polar-bulk-import/")) File(path) else null
        } else null

        return try {
            var totalSkipped = 0
            val allEntities = importer.import(zipUri) { progress ->
                val message = if (progress.finished) {
                    "Fertig — ${progress.entitiesParsed} Trainings importiert (${progress.skipped} uebersprungen)"
                } else {
                    "${progress.entitiesParsed} Trainings importiert (${progress.skipped} uebersprungen)"
                }
                setForegroundAsync(buildForegroundInfo(message))
                totalSkipped = progress.skipped
            }

            // Frank-Wunsch 2026-05-16: vor dem Schreiben der Polar-Daten alle
            // alten Zepp/HC/T-Rex-3-Trainings loeschen. Damit nur noch Polar-
            // Daten in der Liste sichtbar sind — kein "T-Rex 3"-Label mehr.
            val deletedOld = workoutDao.deleteNonPolarWorkouts()
            Log.i(TAG, "Polar-Bulk-Import: $deletedOld alte non-Polar-Trainings geloescht")

            // Polar-Trainings atomar in die DB — REPLACE-Strategie.
            appDatabase.withTransaction {
                workoutDao.upsertAll(allEntities)
            }
            Log.i(TAG, "Polar-Bulk-Import erfolgreich: ${allEntities.size} Trainings in DB geschrieben, $totalSkipped uebersprungen")

            // Cache aufraeumen (nur unsere eigene Datei)
            cacheFileToCleanup?.let { runCatching { it.delete() } }

            syncCoordinator.requestSync()
            showCompletionNotification(allEntities.size, totalSkipped)
            Result.success()
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.e(TAG, "Polar-Bulk-Import fehlgeschlagen", t)
            cacheFileToCleanup?.let { runCatching { it.delete() } }
            showFailureNotification(t.message ?: t.javaClass.simpleName)
            Result.failure()
        }
    }

    private fun buildForegroundInfo(progressText: String): ForegroundInfo {
        ensureChannel()
        val notification: Notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Polar-Historie wird importiert")
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)  // unbestimmter Fortschritt — wir wissen die Gesamtzahl erst am Ende
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showCompletionNotification(imported: Int, skipped: Int) {
        ensureChannel()
        val text = if (skipped > 0) {
            "$imported Trainings importiert, $skipped uebersprungen"
        } else {
            "$imported Trainings erfolgreich importiert"
        }
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Polar-Historie importiert")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showFailureNotification(reason: String) {
        ensureChannel()
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Polar-Import fehlgeschlagen")
            .setContentText(reason.take(120))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID + 2, notification)
    }

    private fun ensureChannel() {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Polar-Import",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Fortschritt und Ergebnis des Polar-Historien-Imports"
            }
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "PolarBulkImportWorker"
        private const val CHANNEL_ID = "polar_bulk_import"
        private const val NOTIFICATION_ID = 73101
        const val KEY_ZIP_URI = "zip_uri"
        const val UNIQUE_NAME_ONESHOT = "polar-bulk-import"
    }
}

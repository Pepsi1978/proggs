package de.frank.updatestation

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Prüft im eingestellten Takt (Standard 30 Minuten), ob in einem Update-Ordner eine neuere Version liegt. */
class PruefWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (Quellen.aktuelle(applicationContext) == null) return Result.success()
        return try {
            val liste = Pruefer.pruefe(applicationContext)
            Benachrichtigungen.meldeNeue(applicationContext, liste)
            Result.success()
        } catch (e: AnmeldungNoetig) {
            Log.w(TAG, "Hintergrundprüfung: Anmeldung nötig")
            Benachrichtigungen.anmeldung(applicationContext)
            Result.success()
        } catch (e: IOException) {
            Log.w(TAG, "Hintergrundprüfung: Netzwerk/Ordner-Fehler, neuer Versuch folgt", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Hintergrundprüfung fehlgeschlagen", e)
            Result.success()
        }
    }

    companion object {
        /**
         * Plant die periodische Prüfung mit dem eingestellten Intervall. UPDATE statt KEEP: ein
         * geändertes Intervall greift sofort, ohne den laufenden Auftrag abzubrechen.
         */
        fun plane(context: Context) {
            val minuten = Einstellungen(context).intervallMinuten.toLong()
            val anfrage = PeriodicWorkRequestBuilder<PruefWorker>(minuten, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("update-pruefung", ExistingPeriodicWorkPolicy.UPDATE, anfrage)
            Log.i(TAG, "Automatische Prüfung geplant: alle $minuten Minuten")
        }

        /** Hängt eine APK noch in der Synchronisierung, in 5 Minuten erneut prüfen statt 30 Minuten warten. */
        fun planeNachpruefung(context: Context) {
            val anfrage = OneTimeWorkRequestBuilder<PruefWorker>()
                .setInitialDelay(5, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("update-nachpruefung", ExistingWorkPolicy.REPLACE, anfrage)
            Log.i(TAG, "Nachprüfung in 5 Minuten geplant (APK noch nicht synchronisiert)")
        }
    }
}

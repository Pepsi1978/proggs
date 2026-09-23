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
import kotlinx.coroutines.CancellationException
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Netzwerk-, Ordner- und sonstige Fehler: höchstens MAX_VERSUCHE je Auftrag, dann einmal melden.
            val versuch = runAttemptCount + 1
            Log.w(TAG, "Hintergrundprüfung fehlgeschlagen (Versuch $versuch/$MAX_VERSUCHE)", e)
            Diagnose.ereignis(applicationContext, Phase.WORKER, "FEHLER", "klasse" to Diagnose.klasse(e), "versuch" to versuch)
            if (versuch < MAX_VERSUCHE) return Result.retry()
            val einst = Einstellungen(applicationContext)
            einst.pruefFehler = true
            if (!einst.pruefFehlerGemeldet && Benachrichtigungen.warnung(
                    applicationContext, 44, "Update-Prüfung scheitert wiederholt",
                    "Die automatische Prüfung ist mehrmals fehlgeschlagen. Details unter Einstellungen → Diagnose.",
                )
            ) {
                einst.pruefFehlerGemeldet = true
            }
            Diagnose.ereignis(applicationContext, Phase.WORKER, "AUFGEGEBEN", "versuche" to versuch)
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

        /**
         * Hängt ein Projektordner noch in der Synchronisierung, in 5 Minuten erneut prüfen statt auf
         * den regulären Takt zu warten. Die Begrenzung auf [MAX_NACHPRUEFUNGEN] Versuche pro
         * Projekt und Lage übernimmt [Einstellungen.zaehleNachpruefungen]; danach prüft wieder
         * nur der reguläre Takt (z. B. bei dauerhaft liegengebliebener APK).
         */
        fun planeNachpruefung(context: Context, projekte: Set<String>) {
            val anfrage = OneTimeWorkRequestBuilder<PruefWorker>()
                .setInitialDelay(5, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("update-nachpruefung", ExistingWorkPolicy.REPLACE, anfrage)
            Log.i(TAG, "Nachprüfung in 5 Minuten geplant (Synchronisations-Zwischenstand: ${projekte.sorted()})")
        }

        const val MAX_NACHPRUEFUNGEN = 6
        private const val MAX_VERSUCHE = 3
    }
}

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
            // Eigene Nachprüfung erkennen: sie darf die nächste nur anhängen, nie sich selbst ersetzen.
            val liste = Pruefer.pruefe(applicationContext, ausNachpruefung = TAG_NACHPRUEFUNG in tags)
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
         * Plant die periodische Prüfung mit dem eingestellten Intervall. Läuft bei jedem Prozessstart
         * (auch wenn WorkManager den Prozess nur für einen Worker startet): UPDATE nur, wenn sich das
         * Intervall gegenüber dem zuletzt geplanten wirklich geändert hat – das übernimmt die neue
         * Spezifikation, ohne den laufenden Worker zu stoppen oder den Takt neu zu starten. Sonst KEEP.
         */
        fun plane(context: Context) {
            val einst = Einstellungen(context)
            val minuten = einst.intervallMinuten
            val geaendert = einst.geplantesIntervall != minuten
            val anfrage = PeriodicWorkRequestBuilder<PruefWorker>(minuten.toLong(), TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "update-pruefung",
                if (geaendert) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
                anfrage,
            )
            if (geaendert) {
                einst.geplantesIntervall = minuten
                Log.i(TAG, "Automatische Prüfung neu geplant: alle $minuten Minuten (UPDATE)")
            }
        }

        /**
         * Hängt ein Projektordner noch in der Synchronisierung, in 5 Minuten erneut prüfen statt auf
         * den regulären Takt zu warten. Die Begrenzung (Versuche pro Projekt und Lage plus echte
         * Mindestzeit) übernimmt [Einstellungen.zaehleNachpruefungen].
         *
         * - Aus der laufenden Nachprüfung selbst ([ausNachpruefung]): APPEND_OR_REPLACE – die nächste
         *   wird hinter die laufende gehängt; REPLACE würde den laufenden Worker abbrechen.
         * - Aus App oder periodischer Prüfung: KEEP – eine ausstehende oder laufende Nachprüfung wird
         *   weder verschoben noch abgebrochen; ihr Scan deckt ohnehin alle Projekte ab.
         */
        fun planeNachpruefung(context: Context, projekte: Set<String>, ausNachpruefung: Boolean) {
            val anfrage = OneTimeWorkRequestBuilder<PruefWorker>()
                .setInitialDelay(5, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .addTag(TAG_NACHPRUEFUNG)
                .build()
            val policy = if (ausNachpruefung) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP
            WorkManager.getInstance(context).enqueueUniqueWork("update-nachpruefung", policy, anfrage)
            Log.i(TAG, "Nachprüfung in 5 Minuten angefordert ($policy, Synchronisations-Zwischenstand: ${projekte.sorted()})")
        }

        const val TAG_NACHPRUEFUNG = "nachpruefung"

        const val MAX_NACHPRUEFUNGEN = 6
        private const val MAX_VERSUCHE = 3
    }
}

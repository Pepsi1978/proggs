package de.frank.updatestation

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Echtzeit-Signal von Jarvis: Der Server meldet eine neu veröffentlichte APK (apk-update am PC), Jarvis reicht es
 * über seine ohnehin offene Serververbindung hierher weiter. Kein eigenes Abfragen im Hintergrund nötig.
 * Geschützt durch Jarvis' Signatur-Erlaubnis BRUECKE.
 */
class SignalEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AKTION) return
        val paket = intent.getStringExtra("paket")
        val vc = intent.getLongExtra("versionCode", 0L)
        Log.i(TAG, "Update-Signal: ${intent.getStringExtra("projekt")} $paket vc $vc")
        Diagnose.ereignis(context, Phase.SYNC, "SIGNAL", "paket" to paket, "vc" to vc)
        PruefWorker.planeSignal(context.applicationContext, paket, vc)
    }

    companion object {
        const val AKTION = "de.frank.updatestation.UPDATE_SIGNAL"
    }
}

/** Prüft im eingestellten Takt (Standard 30 Minuten), ob in einem Update-Ordner eine neuere Version liegt. */
class PruefWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (Quellen.aktuelle(applicationContext) == null) return Result.success()
        return try {
            val app = applicationContext
            // Aus der offenen App angestoßen: nur vorladen, die Liste hat die App gerade selbst geprüft.
            val nurVorladen = inputData.getBoolean(EINGABE_NUR_VORLADEN, false)
            // Eigene Nachprüfung erkennen: sie darf die nächste nur anhängen, nie sich selbst ersetzen.
            val liste = if (nurVorladen) Pruefer.bewerte(app, Einstellungen(app).funde)
                else Pruefer.pruefe(app, ausNachpruefung = TAG_NACHPRUEFUNG in tags)
            // Inzwischen anders installiert (adb, andere Sitzung): alte „bereit“-Meldung wegräumen.
            liste.filter { it.status != Status.UPDATE }.forEach { Benachrichtigungen.entferneUpdate(app, it.paket) }
            val fertig = Vorladen.lade(app, liste)
            if (fertig.isNotEmpty()) {
                Pruefer.bewerteGespeichert(app)
                if (!nurVorladen) {
                    Benachrichtigungen.meldeBereit(app, fertig)
                    // Nicht bei „Nicht stören“ (Schlafenszeit): dann bleibt es bei der stillen Benachrichtigung.
                    val nm = app.getSystemService(NotificationManager::class.java)
                    if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) Vorladen.sageAn(app, fertig)
                }
            }
            if (!nurVorladen) {
                Benachrichtigungen.meldeNeue(app, liste)
                fasseSignalNach(liste)
            }
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

    /**
     * Kam dieser Lauf von einem Echtzeit-Signal (neue APK am PC veröffentlicht), ist Drive für Desktop oft noch beim
     * Hochladen. Bis die gemeldete Version vorgeladen (oder sonst erledigt) ist, in kurzen Abständen erneut prüfen.
     */
    private fun fasseSignalNach(liste: List<AppEintrag>) {
        val paket = inputData.getString(EINGABE_PAKET)?.takeIf { it.isNotBlank() } ?: return
        val vc = inputData.getLong(EINGABE_VC, 0L).takeIf { it > 0 } ?: return
        val versuch = inputData.getInt(EINGABE_VERSUCH, 0)
        val e = liste.firstOrNull { it.paket == paket }
        val erledigt = e != null && e.fund.manifest.versionCode >= vc &&
            (e.status != Status.UPDATE || !Einstellungen(applicationContext).vorladen ||
                Vorladen.vorhanden(applicationContext, e.fund.manifest) != null)
        if (erledigt || versuch >= SIGNAL_ABSTAENDE_S.size) return
        planeSignal(applicationContext, paket, vc, versuch + 1, ExistingWorkPolicy.APPEND_OR_REPLACE)
        Log.i(TAG, "Signal $paket vc $vc noch nicht da: Versuch ${versuch + 1} in ${SIGNAL_ABSTAENDE_S[versuch]} s")
    }

    companion object {
        private const val EINGABE_NUR_VORLADEN = "nurVorladen"
        private const val EINGABE_PAKET = "paket"
        private const val EINGABE_VC = "versionCode"
        private const val EINGABE_VERSUCH = "versuch"
        private val SIGNAL_ABSTAENDE_S = longArrayOf(30, 60, 120, 240, 480)

        /**
         * Echtzeit-Signal: sofort prüfen und vorladen. Ein neues Signal ersetzt ein noch wartendes (REPLACE);
         * das Nachfassen aus dem laufenden Lauf hängt sich dahinter (APPEND_OR_REPLACE), statt ihn abzubrechen.
         */
        fun planeSignal(context: Context, paket: String?, versionCode: Long, versuch: Int = 0,
                        policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE) {
            val anfrage = OneTimeWorkRequestBuilder<PruefWorker>()
                .setInitialDelay(if (versuch == 0) 0 else SIGNAL_ABSTAENDE_S[versuch - 1], TimeUnit.SECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(EINGABE_PAKET to paket.orEmpty(), EINGABE_VC to versionCode, EINGABE_VERSUCH to versuch))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("update-signal", policy, anfrage)
        }

        /** Aus der offenen App: gefundene Updates im Hintergrund vorladen (ohne Benachrichtigung und Ansage). */
        fun planeVorladen(context: Context) {
            val anfrage = OneTimeWorkRequestBuilder<PruefWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(EINGABE_NUR_VORLADEN to true))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("update-vorladen", ExistingWorkPolicy.KEEP, anfrage)
        }

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

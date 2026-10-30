package de.frank.updatestation

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf

/**
 * Verschobene Installation: Wer gerade mit Jarvis arbeitet, startet dessen Update erst nach ein paar Minuten.
 * Ein Wecker löst sie auch aus, wenn UpdateStation längst im Hintergrund oder beendet ist.
 */
object Aufschub {
    const val PAKET_JARVIS = "de.frank.jarvis"
    private const val EXTRA_PAKET = "paket"

    fun plane(context: Context, paket: String, minuten: Int) {
        val um = System.currentTimeMillis() + minuten * 60_000L
        val wecker = context.getSystemService(AlarmManager::class.java)
        val pi = weckerIntent(context, paket)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || wecker.canScheduleExactAlarms()) {
            wecker.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, um, pi)
        } else {
            wecker.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, um, pi)
        }
        ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Geplant(um))
        Diagnose.ereignis(context, Phase.INSTALLATION, "VERSCHOBEN", "paket" to paket, "minuten" to minuten)
    }

    /** „Sofort“ nach einer schon verschobenen Installation: Der Wecker darf nicht noch einmal auslösen. */
    fun abbrechen(context: Context, paket: String) {
        context.getSystemService(AlarmManager::class.java).cancel(weckerIntent(context, paket))
    }

    private fun weckerIntent(context: Context, paket: String): PendingIntent {
        val intent = Intent(context, AufschubEmpfaenger::class.java).putExtra(EXTRA_PAKET, paket)
        return PendingIntent.getBroadcast(context, paket.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    class AufschubEmpfaenger : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val paket = intent.getStringExtra(EXTRA_PAKET) ?: return
            val anfrage = OneTimeWorkRequestBuilder<AufschubWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(workDataOf(EXTRA_PAKET to paket))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("aufschub-$paket", ExistingWorkPolicy.KEEP, anfrage)
        }
    }

    /** Lädt und übergibt im Worker, damit Android den Prozess bis zur Übergabe am Leben lässt. */
    class AufschubWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val app = applicationContext
            val paket = inputData.getString(EXTRA_PAKET) ?: return Result.success()
            Pruefer.bewerteGespeichert(app)
            ZustandsSpeicher.setzeInstallation(paket, null)
            val eintrag = ZustandsSpeicher.zustand.value.eintraege
                .firstOrNull { it.paket == paket && (it.status == Status.UPDATE || it.status == Status.NICHT_INSTALLIERT) }
            if (eintrag == null) {
                Log.i(TAG, "$paket: verschobene Installation nicht mehr nötig")
                return Result.success()
            }
            if (!app.packageManager.canRequestPackageInstalls()) {
                val text = "Bitte zuerst „Unbekannte Apps installieren“ erlauben."
                ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Fehler(text, eintrag.fund.manifest.versionCode))
                Benachrichtigungen.fehler(app, paket, eintrag.label, text)
                return Result.success()
            }
            Installierer.starte(app, eintrag).await()
            return Result.success()
        }

        /** Nur unter Android 12 nötig: Dort läuft eilige Arbeit als Vordergrunddienst. */
        override suspend fun getForegroundInfo(): ForegroundInfo =
            ForegroundInfo(FOREGROUND_ID, Benachrichtigungen.verschobeneInstallation(applicationContext))
    }

    private const val FOREGROUND_ID = 45
}
